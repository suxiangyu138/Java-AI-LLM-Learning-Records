"""
模块 10：百度首页数据爬虫。

覆盖知识点：
    - requests Session 管理（Cookie 持久化）
    - 请求头伪装（User-Agent / Referer）
    - HTML 解析（BeautifulSoup）
    - 百度热搜榜 / 导航链接 / 新闻标题提取
    - 数据持久化（JSON + CSV）
    - 反爬基础策略（延迟 / 重试 / UA 轮换）
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

# ============================================================
# 日志配置
# ============================================================
logger = logging.getLogger(__name__)


def setup_logging(level: int = logging.INFO) -> None:
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
    return random.choice(USER_AGENTS)


# ============================================================
# 数据模型
# ============================================================
@dataclass
class HotSearch:
    """百度热搜条目。"""

    rank: int
    title: str
    url: str = ""
    hot_score: str = ""


@dataclass
class NavLink:
    """导航链接。"""

    name: str
    url: str


@dataclass
class BaiduHomePageData:
    """百度首页完整数据结构。"""

    hot_searches: list[HotSearch] = field(default_factory=list)
    nav_links: list[NavLink] = field(default_factory=list)
    news_titles: list[str] = field(default_factory=list)
    extracted_at: str = field(default_factory=lambda: datetime.now().isoformat())

    @property
    def summary(self) -> str:
        return (
            f"热搜={len(self.hot_searches)}, "
            f"导航={len(self.nav_links)}, "
            f"新闻={len(self.news_titles)}"
        )


# ============================================================
# 百度爬虫
# ============================================================
class BaiduScraper:
    """
    百度首页爬虫。

    抓取内容：
        1. 百度热搜榜（实时热点）
        2. 顶部导航链接
        3. 首页新闻标题

    注意事项：
        - 请遵守 robots.txt 并控制请求频率，仅用于学习
        - 百度首页结构可能变更，选择器需定期更新
    """

    HOME_URL = "https://www.baidu.com"
    TIMEOUT = 10

    def __init__(
        self,
        output_dir: str = "baidu_output",
        min_delay: float = 1.0,
        max_delay: float = 3.0,
    ) -> None:
        self.output = Path(output_dir)
        self.output.mkdir(exist_ok=True)
        self._min_delay = min_delay
        self._max_delay = max_delay

        self.session = requests.Session()
        self._configure_session()

    # ----------------------------------------------------------
    # 会话配置
    # ----------------------------------------------------------
    def _configure_session(self) -> None:
        self.session.headers.update({
            "User-Agent": random_ua(),
            "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
            "Accept-Language": "zh-CN,zh;q=0.9,en;q=0.8",
            "Accept-Encoding": "gzip, deflate, br",
            "Cache-Control": "no-cache",
            "Connection": "keep-alive",
            "Referer": "https://www.baidu.com/",
        })

    def _refresh_ua(self) -> None:
        self.session.headers["User-Agent"] = random_ua()

    def _polite_delay(self) -> None:
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
    # 核心抓取
    # ----------------------------------------------------------
    def fetch_homepage(self) -> BaiduHomePageData:
        """抓取百度首页并解析结构化数据。"""
        data = BaiduHomePageData()
        self._polite_delay()
        self._refresh_ua()

        logger.info("正在请求百度首页...")
        resp = self._safe_get(self.HOME_URL)

        if resp is None:
            logger.error("首页请求失败")
            return data

        logger.info("首页响应: HTTP %d, 大小 %d 字节", resp.status_code, len(resp.content))

        # 百度首页编码可能不是 utf-8，显式处理
        resp.encoding = resp.apparent_encoding or "utf-8"
        soup = BeautifulSoup(resp.text, "lxml")

        # 解析各部分数据
        data.hot_searches = self._parse_hot_searches(soup)
        data.nav_links = self._parse_nav_links(soup)
        data.news_titles = self._parse_news_titles(soup)

        logger.info("解析完成: %s", data.summary)
        return data

    # ----------------------------------------------------------
    # 热搜榜解析
    # ----------------------------------------------------------
    def _parse_hot_searches(self, soup: BeautifulSoup) -> list[HotSearch]:
        """
        解析百度热搜榜。

        百度首页热搜通常位于 .hotsearch 或 #hotsearch 区域，
        也可能通过 JS 动态渲染。这里覆盖常见的静态选择器。
        """
        hot_list: list[HotSearch] = []

        selectors = [
            ".hotsearch-item",
            ".hot-news-wrapper .title-content",
            "[class*='hotsearch'] li",
            ".s-hotsearch-content li",
            "#hotsearch-content-wrapper a",
            ".hotlist .item",
        ]

        for selector in selectors:
            items = soup.select(selector)
            if items:
                logger.info("热搜选择器 '%s' 匹配到 %d 条", selector, len(items))
                for idx, item in enumerate(items):
                    a_tag = item.find("a") or item
                    if isinstance(a_tag, Tag):
                        title = a_tag.get_text(strip=True) or a_tag.get("title", "")
                        href = a_tag.get("href", "")
                        if title and len(title) > 1:
                            hot_list.append(HotSearch(
                                rank=idx + 1,
                                title=title,
                                url=href if href.startswith("http") else f"https:{href}",
                            ))
                if hot_list:
                    break

        # 备用：从 class 含 hot/trend/search 关键字的 <a> 标签提取
        if not hot_list:
            for a_tag in soup.find_all("a", href=True):
                text = a_tag.get_text(strip=True)
                href = a_tag.get("href", "")
                parent_class = " ".join(a_tag.parent.get("class", []))
                if text and 2 < len(text) < 30:
                    if any(kw in parent_class for kw in ("hot", "trend", "search")):
                        hot_list.append(HotSearch(
                            rank=len(hot_list) + 1,
                            title=text,
                            url=href if href.startswith("http") else f"https:{href}",
                        ))

        return hot_list

    # ----------------------------------------------------------
    # 导航链接解析
    # ----------------------------------------------------------
    def _parse_nav_links(self, soup: BeautifulSoup) -> list[NavLink]:
        """解析百度顶部导航链接（新闻、地图、贴吧、视频、图片等）。"""
        nav_links: list[NavLink] = []
        seen: set[str] = set()

        nav_selectors = [
            "#s-top-left a",    # 顶部左侧导航
            ".s-top-nav a",     # 新版导航
            "#u1 a",            # 顶部右侧（登录、设置等）
            ".mnav a",          # 中部导航
        ]

        for selector in nav_selectors:
            for a_tag in soup.select(selector):
                name = a_tag.get_text(strip=True)
                href = a_tag.get("href", "")
                if name and href and name not in seen:
                    seen.add(name)
                    nav_links.append(NavLink(
                        name=name,
                        url=href if href.startswith("http") else f"https://www.baidu.com{href}",
                    ))

        logger.info("导航链接提取完成: %d 条", len(nav_links))
        return nav_links

    # ----------------------------------------------------------
    # 新闻标题解析
    # ----------------------------------------------------------
    def _parse_news_titles(self, soup: BeautifulSoup) -> list[str]:
        """解析首页展示的新闻标题。"""
        titles: list[str] = []

        selectors = [
            ".news-item a",
            ".news-list a",
            "[class*='news'] a",
            ".hot-news a",
            ".s-news-wrapper a",
        ]

        for selector in selectors:
            for a_tag in soup.select(selector):
                text = a_tag.get_text(strip=True)
                if text and len(text) > 5 and text not in titles:
                    titles.append(text)

        return titles[:20]

    # ----------------------------------------------------------
    # 数据持久化
    # ----------------------------------------------------------
    def save_to_json(self, data: BaiduHomePageData, filename: str = "baidu_homepage.json") -> Path:
        filepath = self.output / filename
        filepath.write_text(
            json.dumps(asdict(data), ensure_ascii=False, indent=2),
            encoding="utf-8",
        )
        logger.info("JSON 已保存: %s", filepath)
        return filepath

    def save_to_csv(self, data: BaiduHomePageData) -> tuple[Path, Path, Path]:
        hot_path = self.output / "hot_searches.csv"
        with open(hot_path, "w", newline="", encoding="utf-8-sig") as f:
            writer = csv.writer(f)
            writer.writerow(["排名", "标题", "链接", "热度"])
            for hs in data.hot_searches:
                writer.writerow([hs.rank, hs.title, hs.url, hs.hot_score])
        logger.info("热搜 CSV: %s (%d 行)", hot_path, len(data.hot_searches))

        nav_path = self.output / "nav_links.csv"
        with open(nav_path, "w", newline="", encoding="utf-8-sig") as f:
            writer = csv.writer(f)
            writer.writerow(["名称", "链接"])
            for nl in data.nav_links:
                writer.writerow([nl.name, nl.url])
        logger.info("导航 CSV: %s (%d 行)", nav_path, len(data.nav_links))

        news_path = self.output / "news_titles.csv"
        with open(news_path, "w", newline="", encoding="utf-8-sig") as f:
            writer = csv.writer(f)
            writer.writerow(["新闻标题"])
            for nt in data.news_titles:
                writer.writerow([nt])
        logger.info("新闻 CSV: %s (%d 行)", news_path, len(data.news_titles))

        return hot_path, nav_path, news_path


# ============================================================
# 演示
# ============================================================
def demo() -> BaiduHomePageData:
    """运行百度首页爬虫演示。"""
    setup_logging()
    logger.info("=" * 60)
    logger.info("模块 10：百度首页数据爬虫")
    logger.info("=" * 60)

    scraper = BaiduScraper(output_dir="baidu_output")

    # 1. 抓取首页
    data = scraper.fetch_homepage()

    # 2. 展示结果
    logger.info("=" * 60)
    logger.info("抓取结果摘要: %s", data.summary)

    if data.hot_searches:
        logger.info("百度热搜榜 (共 %d 条):", len(data.hot_searches))
        for hs in data.hot_searches[:15]:
            logger.info("  %2d. %s", hs.rank, hs.title)
    else:
        logger.info("未提取到热搜数据（可能是页面结构变更或 JS 动态渲染）")

    if data.nav_links:
        logger.info("导航链接 (共 %d 条):", len(data.nav_links))
        for nl in data.nav_links:
            logger.info("  %s -> %s", nl.name, nl.url)
    else:
        logger.info("未提取到导航链接")

    if data.news_titles:
        logger.info("新闻标题 (共 %d 条):", len(data.news_titles))
        for nt in data.news_titles:
            logger.info("  %s", nt)
    else:
        logger.info("未提取到新闻标题")

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
    demo()
