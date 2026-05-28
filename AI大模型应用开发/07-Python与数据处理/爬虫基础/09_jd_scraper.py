"""
模块 09：京东首页数据爬虫（升级版）。

覆盖知识点：
    - 【新增】Playwright 浏览器自动化（处理 JS 动态渲染）
    - 【新增】日志系统替代 print
    - 【新增】三级降级策略（Playwright → requests → 模拟数据）
    - 浏览器环境模拟（Headers / Cookies / Session）
    - 反爬虫策略整合（UA 池 / 延迟 / 重试）
    - HTML 解析（导航分类 / 商品信息提取）
    - API 接口抓取（JSONP 分类接口）
    - Cookie 持久化（减少重复验证）
    - 数据持久化（CSV + JSON）
"""

from __future__ import annotations

import csv
import json
import logging
import random
import time
from dataclasses import dataclass, field, asdict
from datetime import datetime
from pathlib import Path
from typing import Any

import requests
from bs4 import BeautifulSoup, Tag

# ----------------------------------------------------------------
# Playwright 可选依赖检测
# ----------------------------------------------------------------
try:
    from playwright.sync_api import sync_playwright, TimeoutError as PlaywrightTimeout
    HAS_PLAYWRIGHT = True
except ImportError:
    HAS_PLAYWRIGHT = False

# ============================================================
# 日志配置
# ============================================================
logger = logging.getLogger(__name__)


def setup_logging(level: int = logging.INFO) -> None:
    """配置日志输出格式。"""
    logging.basicConfig(
        level=level,
        format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
        datefmt="%H:%M:%S",
    )


# ============================================================
# User-Agent 池
# ============================================================
USER_AGENTS = [
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36",
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36 Edg/130.0.0.0",
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36",
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:133.0) Gecko/20100101 Firefox/133.0",
]


def random_ua() -> str:
    """返回随机 User-Agent。"""
    return random.choice(USER_AGENTS)


# ============================================================
# 数据模型
# ============================================================
@dataclass
class Category:
    """商品分类。"""

    name: str
    url: str
    level: str = ""  # 一级 / 二级


@dataclass
class ProductCard:
    """首页商品卡片。"""

    title: str
    price: str
    img_url: str
    product_url: str
    promotion: str = ""

    @property
    def is_valid(self) -> bool:
        """基本校验：必须有标题。"""
        return bool(self.title)


@dataclass
class JdHomePageData:
    """京东首页完整数据结构。"""

    categories: list[Category] = field(default_factory=list)
    hot_products: list[ProductCard] = field(default_factory=list)
    promotions: list[str] = field(default_factory=list)
    extracted_at: str = field(default_factory=lambda: datetime.now().isoformat())

    @property
    def is_valid(self) -> bool:
        """至少有一个分类或商品才算有效数据。"""
        return bool(self.categories) or bool(self.hot_products)

    @property
    def summary(self) -> str:
        """返回数据摘要（用于日志输出）。"""
        return (f"分类={len(self.categories)}, "
                f"商品={len(self.hot_products)}, "
                f"促销={len(self.promotions)}")


# ============================================================
# 京东爬虫
# ============================================================
class JdScraper:
    """
    京东首页爬虫（升级版）。

    抓取策略（三级降级）：
        1. Playwright 浏览器渲染 → 获取完整的 JS 渲染 HTML
        2. requests 降级 → 获取静态 HTML + JSONP API 数据
        3. 模拟数据 → MockJdDataProvider 保证演示流程可走通

    注意事项：
        - 请遵守 robots.txt 并控制请求频率，仅用于学习
        - 京东首页结构会频繁变更，选择器可能需要定期更新
        - 生产环境建议使用专业的反检测方案
    """

    HOME_URL = "https://www.jd.com"
    CATEGORY_API = "https://dc.3.cn/category/get"
    TIMEOUT = 15

    def __init__(
        self,
        use_playwright: bool = True,
        headless: bool = True,
        output_dir: str = "jd_output",
        min_delay: float = 1.0,
        max_delay: float = 3.0,
    ) -> None:
        """
        Args:
            use_playwright: 是否尝试使用 Playwright（未安装时自动降级）。
            headless: Playwright 是否无头模式。
            output_dir: 输出目录。
            min_delay: 请求间隔最小值（秒）。
            max_delay: 请求间隔最大值（秒）。
        """
        self.use_playwright = use_playwright and HAS_PLAYWRIGHT
        self.headless = headless
        self.output = Path(output_dir)
        self.output.mkdir(exist_ok=True)
        self._min_delay = min_delay
        self._max_delay = max_delay
        self._cookie_path = self.output / "cookies.json"

        self.session = requests.Session()
        self._configure_session()

        if self.use_playwright:
            logger.info("Playwright 可用，将使用浏览器渲染模式")
        else:
            logger.info("Playwright 不可用，将使用 requests + 降级模式")

    # ----------------------------------------------------------
    # 会话管理
    # ----------------------------------------------------------
    def _configure_session(self) -> None:
        """配置会话 —— 模拟真实浏览器。"""
        self.session.headers.update({
            "User-Agent": random_ua(),
            "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
            "Accept-Language": "zh-CN,zh;q=0.9,en;q=0.8",
            "Accept-Encoding": "gzip, deflate, br",
            "Cache-Control": "no-cache",
            "Connection": "keep-alive",
            "Referer": "https://www.jd.com/",
        })

    def _refresh_ua(self) -> None:
        """随机更换 User-Agent。"""
        self.session.headers["User-Agent"] = random_ua()

    def _polite_delay(self) -> None:
        """请求间隔延迟，模拟人类浏览节奏。"""
        time.sleep(random.uniform(self._min_delay, self._max_delay))

    def _safe_get(self, url: str, **kwargs: Any) -> requests.Response | None:
        """带退避重试的 GET 请求。"""
        for attempt in range(3):
            try:
                resp = self.session.get(url, timeout=self.TIMEOUT, **kwargs)
                if resp.status_code < 500:
                    return resp
                logger.warning("HTTP %d (重试 %d/3)", resp.status_code, attempt + 1)
            except requests.RequestException as e:
                logger.warning("%s (重试 %d/3)", e, attempt + 1)
            time.sleep(2 ** attempt + random.uniform(0, 1))
        return None

    # ----------------------------------------------------------
    # Cookie 持久化
    # ----------------------------------------------------------
    def _load_cookies(self) -> dict[str, str]:
        """从磁盘加载持久化 Cookie。"""
        if self._cookie_path.exists():
            try:
                raw = self._cookie_path.read_text(encoding="utf-8")
                cookies: dict[str, str] = json.loads(raw)
                for name, value in cookies.items():
                    self.session.cookies.set(name, value)
                logger.info("已加载 %d 个持久化 Cookie", len(cookies))
                return cookies
            except (json.JSONDecodeError, OSError) as e:
                logger.warning("Cookie 加载失败: %s", e)
        return {}

    def _save_cookies_from_session(self) -> None:
        """将当前 Session Cookie 保存到磁盘。"""
        try:
            cookies = {c.name: c.value for c in self.session.cookies}
            self._cookie_path.write_text(
                json.dumps(cookies, ensure_ascii=False, indent=2),
                encoding="utf-8",
            )
            logger.info("已保存 %d 个 Cookie", len(cookies))
        except OSError as e:
            logger.warning("Cookie 保存失败: %s", e)

    # ----------------------------------------------------------
    # 策略 1: Playwright 浏览器渲染
    # ----------------------------------------------------------
    def _fetch_with_playwright(self) -> str | None:
        """
        使用 Playwright 获取京东首页的完全渲染 HTML。

        核心流程：
            1. 启动 Chromium（无头/有头模式）
            2. 加载持久化 Cookie 减少验证码
            3. 拦截图片/字体资源加速加载
            4. 导航并等待关键 DOM 出现
            5. 滚页触发懒加载内容
            6. 截屏留存用于调试
            7. 持久化 Cookie 供下次使用

        Returns:
            渲染完成的 HTML 字符串，或 None（失败时）。
        """
        if not HAS_PLAYWRIGHT:
            logger.warning("Playwright 未安装，请执行: pip install playwright && playwright install chromium")
            return None

        with sync_playwright() as p:
            browser = p.chromium.launch(
                headless=self.headless,
                args=[
                    "--disable-blink-features=AutomationControlled",
                    "--no-sandbox",
                ],
            )
            context = browser.new_context(
                viewport={"width": 1920, "height": 1080},
                user_agent=random_ua(),
                locale="zh-CN",
                timezone_id="Asia/Shanghai",
            )

            # 尝试加载持久化 Cookie
            cookie_file = self.output / "cookies_playwright.json"
            if cookie_file.exists():
                try:
                    with open(cookie_file, "r", encoding="utf-8") as f:
                        cookies = json.load(f)
                    context.add_cookies(cookies)
                    logger.info("已加载 %d 个 Playwright Cookie", len(cookies))
                except Exception as e:
                    logger.warning("Playwright Cookie 加载失败: %s", e)

            page = context.new_page()

            # 拦截图片和字体请求，加速页面加载
            page.route(
                "**/*.{png,jpg,jpeg,gif,svg,woff,woff2,ttf}",
                lambda route: route.abort(),
            )

            try:
                logger.info("Playwright 正在导航到 %s ...", self.HOME_URL)
                page.goto(self.HOME_URL, wait_until="networkidle", timeout=30000)

                # 等待核心分类导航出现（逐一尝试多个可能的选择器）
                nav_selectors = [
                    ".cate_menu_item",
                    "#J_cate",
                    ".JS_navCtn",
                    "#nav-2014",
                ]
                for sel in nav_selectors:
                    try:
                        page.wait_for_selector(sel, timeout=8000)
                        logger.info("分类导航选择器 '%s' 匹配成功", sel)
                        break
                    except PlaywrightTimeout:
                        continue
                else:
                    logger.warning("所有分类导航选择器均未匹配，页面结构可能已变更")

                # 截屏用于调试
                screenshot_path = self.output / "jd_screenshot.png"
                page.screenshot(path=str(screenshot_path))
                logger.info("首页截屏已保存: %s", screenshot_path)

                # 滚动页面触发懒加载
                page.evaluate("window.scrollTo(0, document.body.scrollHeight)")
                page.wait_for_timeout(2000)
                page.evaluate("window.scrollTo(0, 0)")
                page.wait_for_timeout(500)

                # 获取渲染后的完整 HTML
                html = page.content()
                logger.info("Playwright 获取 HTML 成功，大小: %d 字节", len(html))

                # 持久化 Cookie 供下次使用
                try:
                    cookies_list = context.cookies()
                    with open(cookie_file, "w", encoding="utf-8") as f:
                        json.dump(cookies_list, f, ensure_ascii=False, indent=2)
                    logger.info("Playwright Cookie 已持久化")
                except Exception as e:
                    logger.warning("Playwright Cookie 保存失败: %s", e)

                return html

            except PlaywrightTimeout as e:
                logger.error("Playwright 页面加载超时: %s", e)
                return None
            except Exception as e:
                logger.error("Playwright 异常: %s", e)
                return None
            finally:
                browser.close()

    # ----------------------------------------------------------
    # 策略 2: requests 降级模式
    # ----------------------------------------------------------
    def _fetch_with_requests(self) -> JdHomePageData:
        """
        使用 requests 获取京东首页静态数据 + API 数据。

        这是降级路径，只能获取服务端渲染的部分内容。
        """
        data = JdHomePageData()
        self._polite_delay()
        self._refresh_ua()

        logger.info("正在请求京东首页（requests 降级）...")
        resp = self._safe_get(self.HOME_URL)

        if resp is None:
            logger.error("首页请求失败")
            return data

        logger.info("首页响应: HTTP %d, 大小 %d 字节", resp.status_code, len(resp.content))
        soup = BeautifulSoup(resp.content, "lxml")

        data.categories = self._parse_categories(soup)
        data.hot_products = self._parse_product_cards(soup)
        data.promotions = self._parse_promotions(soup)

        logger.info("requests 解析: %s", data.summary)
        return data

    def _append_api_categories(self, data: JdHomePageData) -> None:
        """通过 JSONP API 获取分类并合并到 data 中（去重）。"""
        api_cats = self.fetch_categories_via_api()
        existing_names = {c.name.strip() for c in data.categories}
        for c in api_cats:
            if c.name.strip() not in existing_names:
                data.categories.append(c)
        if api_cats:
            logger.info("API 补充后总计 %d 个分类", len(data.categories))

    # ----------------------------------------------------------
    # 统一入口（三级降级调度）
    # ----------------------------------------------------------
    def fetch_homepage(self) -> JdHomePageData:
        """
        抓取京东首页并解析结构化数据。

        降级链路：Playwright → requests + API → 模拟数据

        Returns:
            JdHomePageData（绝不会为空，三层保护）。
        """
        # ----- 策略 1: Playwright 浏览器渲染 -----
        if self.use_playwright:
            logger.info("--- 策略 1: Playwright 浏览器渲染 ---")
            html = self._fetch_with_playwright()
            if html:
                soup = BeautifulSoup(html, "lxml")
                data = JdHomePageData()
                data.categories = self._parse_categories(soup)
                data.hot_products = self._parse_product_cards(soup)
                data.promotions = self._parse_promotions(soup)
                logger.info("Playwright 解析完成: %s", data.summary)

                if not data.categories:
                    logger.info("Playwright 未提取到分类，尝试 API 补充")
                    self._append_api_categories(data)
                return data
            else:
                logger.warning("Playwright 获取失败，降级到 requests 模式")

        # ----- 策略 2: requests 降级 -----
        logger.info("--- 策略 2: requests + API 模式 ---")
        data = self._fetch_with_requests()
        self._append_api_categories(data)
        if data.is_valid:
            logger.info("requests 模式解析完成: %s", data.summary)
            return data

        # ----- 策略 3: 模拟数据 -----
        logger.info("--- 策略 3: 模拟数据回退 ---")
        logger.warning("所有抓取方式均未获取到有效数据，使用模拟数据用于演示")
        return MockJdDataProvider.generate()

    # ----------------------------------------------------------
    # HTML 解析器
    # ----------------------------------------------------------

    # 注意：以下 CSS 选择器列表是尽力而为的覆盖。
    # 京东前端 DOM 结构会频繁变更，如果提取不到数据，
    # 请打开浏览器开发者工具检查当前页面的实际 class 名称并更新选择器。

    def _parse_categories(self, soup: BeautifulSoup) -> list[Category]:
        """解析首页分类导航。"""
        categories: list[Category] = []

        selectors = [
            ".cate_menu_item",                     # 经典左侧导航
            ".JS_navCtn .cate_menu_item",          # 导航容器
            "#categorys .cate_menu_item",          # 分类区域
            ".navitems li",                        # 顶部导航
            ".menu-drop .item",                    # 下拉菜单
            "#J_cate .cate_menu_item",             # 新版分类侧栏
            ".cate-menu-list a",                   # 新版分类列表
        ]

        for selector in selectors:
            items = soup.select(selector)
            if items:
                for item in items:
                    a_tag = item.find("a", href=True)
                    if not a_tag:
                        a_tag = item.select_one("a[href]")
                    if a_tag:
                        name = a_tag.get_text(strip=True)
                        href = a_tag.get("href", "")
                        if name and len(name) < 20:
                            categories.append(Category(
                                name=name,
                                url=href if href.startswith("http") else f"https:{href}",
                                level="一级",
                            ))
                if categories:
                    break

        return categories

    def _parse_product_cards(self, soup: BeautifulSoup) -> list[ProductCard]:
        """解析首页商品卡片。"""
        products: list[ProductCard] = []

        selectors = [
            ".goods-list-v1 .gl-item",             # 经典商品列表
            ".goods-list .gl-item",                # 通用商品列表
            "[class*='goods-item']",               # 商品项模糊匹配
            ".J_goodsList .gl-item",               # 推荐商品列表
            ".seckill-goods-item",                 # 秒杀商品
            "#J_goodsList li",                     # 新版商品列表
            ".plist .gl-item",                     # 另一个常见容器
            ".tab-content .gl-item",               # Tab 内容区商品
        ]

        for selector in selectors:
            items = soup.select(selector)
            if items:
                for item in items[:30]:
                    product = self._extract_product(item)
                    if product and product.is_valid:
                        products.append(product)
                if products:
                    break

        return products

    def _extract_product(self, item: Tag) -> ProductCard | None:
        """从单个商品 DOM 中提取 ProductCard。"""
        title = ""
        price = ""
        img_url = ""
        product_url = ""
        promotion = ""

        # 标题
        for cls in (".p-name", ".goods-name", ".title", "a[title]", ".p-name em"):
            tag = item.select_one(cls)
            if tag:
                title = tag.get_text(strip=True) or tag.get("title", "")
                if title:
                    break

        # 价格
        for cls in (".p-price", ".goods-price", ".price", "[class*='price']"):
            tag = item.select_one(cls)
            if tag:
                price = tag.get_text(strip=True)
                if price and "¥" in price:
                    break

        # 图片（支持懒加载属性）
        for cls in ("img", ".p-img img", ".goods-img img"):
            tag = item.select_one(cls)
            if tag:
                img_url = (
                    tag.get("src", "") or
                    tag.get("data-lazy-img", "") or
                    tag.get("data-src", "") or
                    tag.get("data-original", "")
                )
                if img_url:
                    break

        # 商品链接
        for cls in ("a[href]", ".p-name a", ".goods-name a"):
            tag = item.select_one(cls)
            if tag:
                href = tag.get("href", "")
                product_url = href if href.startswith("http") else f"https:{href}"
                break

        # 促销标签
        promo_tag = item.select_one(
            ".p-icons, .goods-icons, [class*='promotion'], [class*='tag']"
        )
        if promo_tag:
            promotion = promo_tag.get_text(strip=True)

        if not title:
            return None

        return ProductCard(title, price, img_url, product_url, promotion)

    def _parse_promotions(self, soup: BeautifulSoup) -> list[str]:
        """解析首页促销活动文案。"""
        promos: list[str] = []

        for selector in (
            ".promo-banner",
            ".activity-text",
            "[class*='promotion']",
            ".floor-banner",
            ".J_actBanner",
            ".swiper-slide",
            ".slide-text",
        ):
            for tag in soup.select(selector):
                text = tag.get_text(strip=True)
                if text and len(text) < 100:
                    promos.append(text)

        return promos[:20]

    # ----------------------------------------------------------
    # API 接口抓取
    # ----------------------------------------------------------
    def fetch_categories_via_api(self) -> list[Category]:
        """
        通过京东 JSONP 接口获取分类数据。

        接口: https://dc.3.cn/category/get
        返回格式: JSONP callback，需要去包装
        """
        logger.info("正在通过 API 获取分类...")
        self._refresh_ua()
        self._polite_delay()

        resp = self._safe_get(self.CATEGORY_API)
        if resp is None or not resp.text:
            logger.error("分类 API 请求失败")
            return []

        categories: list[Category] = []
        try:
            text = resp.text
            # 去除 JSONP 回调包装: jQueryXXXX([...])
            json_str = text[text.index("(") + 1:text.rindex(")")]
            data = json.loads(json_str)
            for item in data:
                cat = item.get("s", {})
                name = cat.get("n", "")
                url = cat.get("u", "")
                if name:
                    categories.append(Category(name=name, url=url, level="一级"))
                    # 解析二级分类
                    subs = cat.get("s", [])
                    for sub in subs:
                        sub_name = sub.get("n", "")
                        sub_url = sub.get("u", "")
                        if sub_name:
                            categories.append(Category(
                                name=f"  └ {sub_name}",
                                url=sub_url,
                                level="二级",
                            ))
            logger.info("API 获取到 %d 个分类", len(categories))
        except (json.JSONDecodeError, ValueError, IndexError) as e:
            logger.error("分类数据解析失败: %s", e)

        return categories

    # ----------------------------------------------------------
    # 数据持久化
    # ----------------------------------------------------------
    def save_to_json(self, data: JdHomePageData, filename: str = "jd_homepage.json") -> Path:
        """保存为 JSON 文件。"""
        filepath = self.output / filename
        filepath.write_text(
            json.dumps(asdict(data), ensure_ascii=False, indent=2),
            encoding="utf-8",
        )
        logger.info("JSON 已保存: %s", filepath)
        return filepath

    def save_to_csv(self, data: JdHomePageData) -> tuple[Path, Path]:
        """保存分类和商品为 CSV 文件。"""
        # 分类 CSV
        cat_path = self.output / "categories.csv"
        with open(cat_path, "w", newline="", encoding="utf-8-sig") as f:
            writer = csv.writer(f)
            writer.writerow(["分类名称", "链接", "层级"])
            for cat in data.categories:
                writer.writerow([cat.name, cat.url, cat.level])
        logger.info("分类 CSV: %s (%d 行)", cat_path, len(data.categories))

        # 商品 CSV
        prod_path = self.output / "products.csv"
        with open(prod_path, "w", newline="", encoding="utf-8-sig") as f:
            writer = csv.writer(f)
            writer.writerow(["标题", "价格", "图片URL", "商品链接", "促销信息"])
            for p in data.hot_products:
                writer.writerow([p.title, p.price, p.img_url, p.product_url, p.promotion])
        logger.info("商品 CSV: %s (%d 行)", prod_path, len(data.hot_products))

        return cat_path, prod_path


# ============================================================
# 模拟数据提供者（降级兜底）
# ============================================================
class MockJdDataProvider:
    """
    模拟京东首页数据提供者。

    当 Playwright 不可用且 requests 也无法获取有效数据时，
    返回合理结构的模拟数据，保证演示流程可以完整走通。

    模拟数据标题带【模拟】前缀，便于区分。
    """

    MOCK_CATEGORIES: list[tuple[str, str]] = [
        ("手机", "https://list.jd.com/list.html?cat=9987,653,655"),
        ("电脑办公", "https://list.jd.com/list.html?cat=670,671,672"),
        ("家用电器", "https://list.jd.com/list.html?cat=737,738,739"),
        ("食品生鲜", "https://list.jd.com/list.html?cat=1320,1583,1590"),
        ("美妆护肤", "https://list.jd.com/list.html?cat=1316,1381,1382"),
        ("母婴玩具", "https://list.jd.com/list.html?cat=1319,1523,1524"),
        ("图书文娱", "https://list.jd.com/list.html?cat=1713,3258,3297"),
        ("家居家装", "https://list.jd.com/list.html?cat=1620,1621,1622"),
        ("运动户外", "https://list.jd.com/list.html?cat=1318,1462,1463"),
        ("鞋靴箱包", "https://list.jd.com/list.html?cat=11729,11730,11731"),
    ]

    MOCK_PRODUCTS: list[ProductCard] = [
        ProductCard(
            title="【模拟】Apple iPhone 16 Pro Max 256GB 原色钛金属",
            price="¥9,999",
            img_url="https://img.example.com/iphone16.jpg",
            product_url="https://item.jd.com/100000000001.html",
            promotion="京东百亿补贴",
        ),
        ProductCard(
            title="【模拟】华为 Mate 70 Pro+ 16GB+512GB 皓月银",
            price="¥8,999",
            img_url="https://img.example.com/mate70.jpg",
            product_url="https://item.jd.com/100000000002.html",
            promotion="以旧换新最高补贴2000元",
        ),
        ProductCard(
            title="【模拟】联想拯救者 Y9000P 2025 i9-14900HX RTX4070",
            price="¥10,499",
            img_url="https://img.example.com/legion.jpg",
            product_url="https://item.jd.com/100000000003.html",
            promotion="12期免息",
        ),
        ProductCard(
            title="【模拟】戴森 V16 Detect Absolute 无线吸尘器",
            price="¥5,990",
            img_url="https://img.example.com/dyson.jpg",
            product_url="https://item.jd.com/100000000004.html",
            promotion="满3000减300",
        ),
        ProductCard(
            title="【模拟】茅台 飞天 53度 500ml 酱香型白酒",
            price="¥1,499",
            img_url="https://img.example.com/maotai.jpg",
            product_url="https://item.jd.com/100000000005.html",
            promotion="限时抢购",
        ),
        ProductCard(
            title="【模拟】海尔 全自动滚筒洗衣机 10kg 变频静音",
            price="¥3,299",
            img_url="https://img.example.com/haier.jpg",
            product_url="https://item.jd.com/100000000006.html",
            promotion="以旧换新",
        ),
        ProductCard(
            title="【模拟】耐克 Air Force 1 '07 经典空军一号",
            price="¥899",
            img_url="https://img.example.com/nike.jpg",
            product_url="https://item.jd.com/100000000007.html",
            promotion="",
        ),
        ProductCard(
            title="【模拟】SK-II 神仙水 230ml 护肤精华露",
            price="¥1,370",
            img_url="https://img.example.com/skii.jpg",
            product_url="https://item.jd.com/100000000008.html",
            promotion="买2件享8折",
        ),
    ]

    @classmethod
    def generate(cls) -> JdHomePageData:
        """生成模拟京东首页数据。"""
        categories = [
            Category(name=name, url=url, level="一级")
            for name, url in cls.MOCK_CATEGORIES
        ]
        promotions = [
            "618 年中购物节 · 全场满300减50",
            "京东百亿补贴 · 大牌低至5折",
            "新品首发 · 旗舰手机现货开抢",
            "京东超市 · 好物低至1元",
            "家电以旧换新 · 最高补贴2000元",
        ]
        logger.info("生成模拟数据: %d 分类, %d 商品, %d 促销",
                     len(categories), len(cls.MOCK_PRODUCTS), len(promotions))
        return JdHomePageData(
            categories=categories,
            hot_products=list(cls.MOCK_PRODUCTS),
            promotions=promotions,
        )


# ============================================================
# 演示
# ============================================================
def demo(use_playwright: bool = True, headless: bool = True) -> JdHomePageData:
    """运行京东首页爬虫演示。"""
    setup_logging()
    logger.info("=" * 60)
    logger.info("模块 09：京东首页数据爬虫（升级版）")
    logger.info("=" * 60)

    scraper = JdScraper(
        use_playwright=use_playwright,
        headless=headless,
        output_dir="jd_output",
    )

    # 1. 抓取首页数据
    logger.info("--- 1. 抓取首页 ---")
    data = scraper.fetch_homepage()

    # 2. 展示结果
    logger.info("=" * 60)
    logger.info("抓取结果摘要: %s", data.summary)
    logger.info("=" * 60)

    logger.info("分类导航 (共 %d 个):", len(data.categories))
    for cat in data.categories[:20]:
        logger.info("  [%s] %s", cat.level, cat.name)
    if len(data.categories) > 20:
        logger.info("  ... 还有 %d 个", len(data.categories) - 20)

    logger.info("商品卡片 (共 %d 个):", len(data.hot_products))
    for p in data.hot_products[:10]:
        logger.info("  %s | %s", p.title[:60], p.price)
    if len(data.hot_products) > 10:
        logger.info("  ... 还有 %d 个", len(data.hot_products) - 10)

    if data.promotions:
        logger.info("促销信息 (共 %d 条):", len(data.promotions))
        for promo in data.promotions[:5]:
            logger.info("  %s", promo[:80])

    # 3. 保存数据
    logger.info("--- 保存数据 ---")
    scraper.save_to_json(data)
    scraper.save_to_csv(data)

    # 4. 输出文件列表
    logger.info("输出目录: %s", scraper.output.absolute())
    for f in sorted(scraper.output.iterdir()):
        if f.is_file():
            size_kb = f.stat().st_size / 1024
            logger.info("  %s (%.1f KB)", f.name, size_kb)

    logger.info("演示完成！")
    return data


if __name__ == "__main__":
    import sys
    use_pw = "--no-playwright" not in sys.argv
    demo(use_playwright=use_pw, headless=True)
