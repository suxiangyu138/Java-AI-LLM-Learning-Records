# Playwright 现代方案
> 2026 新项目首选：安装、自动等待、locator、route 拦截、context 隔离——"浏览器自动化的现代标准"

## 📚 目录
1. [为什么 2026 选 Playwright](#1-为什么-2026-选-playwright)
2. [安装与启动](#2-安装与启动)
3. [自动等待：告别 sleep](#3-自动等待告别-sleep)
4. [locator：现代定位](#4-locator现代定位)
5. [route()：请求拦截（拿接口数据）](#5-route请求拦截拿接口数据)
6. [BrowserContext：会话隔离](#6-browsercontext会话隔离)
7. [Playwright 爬虫模板](#7-playwright-爬虫模板)

## 1. 为什么 2026 选 Playwright

| 维度 | Playwright（2026 首选） | Selenium |
|------|:---:|:---:|
| 版本 | 1.60+ | 4.4x（5.0 重构中） |
| 等待 | **自动等待**（零 sleep） | 手写 WebDriverWait |
| 请求拦截 | **内置 route()** | selenium-wire（已归档） |
| 会话隔离 | **BrowserContext**（轻量多会话） | 每实例一浏览器 |
| 代理 | 每 context 一行配置 | 繁琐 |
| 反检测 | stealth 生态 | undetected-chromedriver |
| 调试 | **Trace Viewer/录屏** | 弱 |
| 生态趋势 | AI Agent 框架默认 | 存量维护 |

> 🎯 2026 结论（检索核实）：**"新爬虫项目渲染方案默认 Playwright"**——自动等待消灭 90% 时序 bug、route() 让"渲染 + 抓接口"合一、context 隔离省资源。Selenium 用于存量代码维护。

## 2. 安装与启动

```bash
# 安装库 + 浏览器（一次性）
pip install playwright
playwright install chromium          # 只装 chromium 够爬虫用

# 其他浏览器（按需）
# playwright install firefox webkit
```

```python
from playwright.sync_api import sync_playwright

# ═══ 启动（同步 API，爬虫够用）═══
with sync_playwright() as p:
    browser = p.chromium.launch(
        headless=True,               # 无头（服务器）
        args=["--no-sandbox"],
    )
    page = browser.new_page()
    page.goto("https://example.com/list", timeout=30000)
    print(page.title())
    browser.close()

# ═══ 页面内容获取 ═══
html = page.content()                # 渲染后 HTML（核心）
print(html[:500])
```

| Playwright 概念 | 说明 |
|----------------|------|
| `Browser` | 浏览器实例（launch 一次） |
| `Context` | 会话隔离容器（§6，相当于独立浏览器档案） |
| `Page` | 一个标签页（操作主体） |
| `sync_playwright` | 同步 API（爬虫用）；async 给高并发 |

> 💡 资源管理：**`browser.close()` 必须调用**（与 Selenium 的 quit 同理）；`with` 块/上下文是规范写法（§7 模板）。

## 3. 自动等待：告别 sleep

```python
from playwright.sync_api import sync_playwright

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    page = browser.new_page()
    page.goto("https://example.com/list")

    # ═══ 自动等待（Playwright 核心优势）═══
    # 默认 30 秒内自动等待元素"可操作"再执行
    page.click("button.load-more")              # 自动等按钮可点
    page.fill("input[name=q]", "爬虫")           # 自动等输入框可用

    # 显式等待（可选）
    page.wait_for_selector("ul.news-list li", timeout=15000)   # 等元素出现
    page.wait_for_load_state("networkidle")     # 等网络空闲（谨慎用）

    # ⚠️ 不再需要：time.sleep / WebDriverWait
    # Playwright 所有操作内建"等可操作"（actionability）

    html = page.content()
    browser.close()
```

| 自动等待 | 说明 |
|---------|------|
| 默认等待 | 所有操作等元素"可操作"（可见/稳定/可点） |
| `wait_for_selector` | 等数据元素出现（爬虫核心） |
| `wait_for_load_state` | 等加载状态（networkidle 慎用——SPA 可能永不 idle） |
| 超时 | 默认 30s，可按需设置 |

> 🎯 与 Selenium 的对比金句：**"Selenium 要你'等'，Playwright 替你'等'"**——`click`/`fill`/`wait_for_selector` 全部自动等待，消灭了 `WebDriverWait` 与 `time.sleep` 的样板代码。

## 4. locator：现代定位

```python
from playwright.sync_api import sync_playwright

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    page = browser.new_page()
    page.goto("https://example.com/list")

    # ═══ locator：CSS/XPath/文本/角色 四种定位 ═══
    titles = page.locator("ul.news-list li a.title")     # CSS（主力）
    items = page.locator("//ul[@class='list']/li")        # XPath
    next_btn = page.get_by_text("下一页")                  # 文本定位
    search = page.get_by_role("searchbox")                # 无障碍角色

    # ═══ locator 操作 ═══
    count = titles.count()                 # 数量
    first = titles.first.text_content()    # 第一个文本
    all_texts = titles.all_text_contents() # 全部文本（列表）
    hrefs = titles.evaluate_all("els => els.map(e => e.href)")  # 提取属性（JS 表达式）

    # ═══ 遍历 ═══
    for i in range(items.count()):
        item = items.nth(i)
        print(item.locator("a").text_content(), item.locator("a").get_attribute("href"))

    browser.close()
```

| locator 能力 | 说明 |
|-------------|------|
| CSS/XPath | 与阶段 2 同语法（技能复用） |
| `get_by_text` | 文本定位（比 XPath text() 更简洁） |
| `count()` / `nth(i)` | 列表遍历 |
| `all_text_contents` | 批量取文本（爬虫高频） |
| `evaluate_all` | **JS 表达式提取**（属性/复杂提取） |

> 🎯 locator 哲学：**"locator 是'可重查'的定位器"**——不像 Selenium 的 find_element 返回"快照"，Playwright locator 每次操作自动重新查找（配合自动等待，元素动态变化也不怕）。

## 5. route()：请求拦截（拿接口数据）

```python
from playwright.sync_api import sync_playwright

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    page = browser.new_page()

    # ═══ 拦截响应：捕获接口数据（动态页最优解之一）═══
    captured = {}

    def on_response(response):
        url = response.url
        if "/api/articles" in url:               # 匹配数据接口
            try:
                captured["articles"] = response.json()   # 直接拿 JSON！
                print(f"捕获接口: {url}")
            except Exception:
                pass

    page.on("response", on_response)             # 注册响应监听

    page.goto("https://example.com/list")
    page.wait_for_timeout(2000)                  # 等接口返回（或用 wait_for_selector）

    # ═══ 直接使用捕获的接口数据（比解析 HTML 更优）═══
    if "articles" in captured:
        for item in captured["articles"]["data"]["list"]:
            print(item["title"])

    browser.close()
```

| route/监听能力 | 说明 |
|---------------|------|
| `page.on("response")` | 监听所有响应（匹配接口） |
| `response.json()` | 直接拿 JSON（结构化！） |
| `page.route()` | 拦截/修改请求（mock/加速） |
| 价值 | **渲染时顺手抓接口 = 直取与渲染合一** |

> 🎯 杀手级场景：**"接口有签名拿不到？让浏览器帮你请求，你监听响应"**——Playwright 渲染页面时接口自动带签名请求，`page.on("response")` 把 JSON 截下来——**绕开签名逆向**（本阶段可用的最优兜底，比解析渲染 HTML 更结构化）。

## 6. BrowserContext：会话隔离

```python
from playwright.sync_api import sync_playwright

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)

    # ═══ 多个隔离会话（一个浏览器进程，N 个 context）═══
    ctx_a = browser.new_context(
        user_agent="Mozilla/5.0 (Windows NT 10.0...) Chrome/126.0.0.0",
        locale="zh-CN",
        timezone_id="Asia/Shanghai",
        proxy={"server": "http://proxy:8080"},    # 每 context 独立代理！
    )
    ctx_b = browser.new_context()                 # 默认环境

    page_a = ctx_a.new_page()                     # 每个 context 一个 page
    page_a.goto("https://example.com")

    # context 隔离：Cookie/存储/UA 互不影响
    # 场景：
    #   多账号会话隔离（每账号一个 context）
    #   多代理出口（每 context 一个 proxy）
    #   多 UA/时区（指纹分散）

    ctx_a.close()                                 # 用完关闭
    browser.close()
```

| Context 场景 | 价值 |
|-------------|------|
| 多账号 | 每账号独立 Cookie（登录态隔离） |
| 多代理 | 每 context 独立出口 IP |
| 指纹分散 | 每 context 不同 UA/时区 |
| 资源省 | 一个浏览器进程跑几十个 context（vs Selenium 每实例一进程） |

> 🎯 与阶段 4 的衔接：**"Context = 会话级别的隔离容器"**——之前学的"会话一致性"（UA/Cookie/IP 对应）在 Playwright 里用 Context 一行配置实现，比 requests 的 Session 更彻底。

## 7. Playwright 爬虫模板

```python
"""Playwright 渲染爬虫标准模板（2026 生产姿势）"""
from playwright.sync_api import sync_playwright


class PlaywrightSpider:
    def __init__(self, headless=True, proxy=None, ua=None):
        self._pw = sync_playwright().start()
        self.browser = self._pw.chromium.launch(headless=headless, args=["--no-sandbox"])
        self.context = self.browser.new_context(
            user_agent=ua or "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                             "AppleWebKit/537.36 Chrome/126.0.0.0 Safari/537.36",
            locale="zh-CN",
            timezone_id="Asia/Shanghai",
            proxy=proxy,                        # 每 context 独立代理
        )
        self.page = self.context.new_page()

    def fetch_rendered(self, url, selector=None, timeout=15000):
        """打开页面 → 等数据元素 → 返回渲染后 HTML + 捕获接口"""
        api_data = {}
        self.page.on("response", lambda r: self._capture_api(r, api_data))
        self.page.goto(url, timeout=30000)
        if selector:
            self.page.wait_for_selector(selector, timeout=timeout)
        return self.page.content(), api_data     # HTML + 顺手抓的接口

    def _capture_api(self, response, store):
        if "api" in response.url and "json" in (response.headers.get("content-type") or ""):
            try:
                store[response.url] = response.json()
            except Exception:
                pass

    def close(self):
        self.context.close()
        self.browser.close()
        self._pw.stop()

    def __enter__(self):
        return self

    def __exit__(self, *args):
        self.close()


# 使用
with PlaywrightSpider(headless=True) as spider:
    html, api = spider.fetch_rendered(
        "https://example.com/list",
        selector="ul.news-list li",             # 等数据出现
    )
    # ① 优先用捕获的接口数据（结构化）
    if api:
        print("接口数据:", list(api.keys()))
    # ② 或解析渲染后 HTML（bs4，阶段 2 技能）
    from bs4 import BeautifulSoup
    soup = BeautifulSoup(html, "lxml")
    print(len(soup.select("ul.news-list li")), "条")
```

> 🎯 模板设计：**"一次打开页面 = HTML + 接口双收获"**——`fetch_rendered` 同时返回渲染 HTML 与捕获的接口 JSON；接口数据优先（结构化）、HTML 兜底（无接口时）。这是 2026 动态页爬虫的"标准答案"。

---

**下一模块**：[06-无头浏览器与反检测](06-无头浏览器与反检测.md) / **返回总览**：[00-阶段5动态网页爬虫总览](00-阶段5动态网页爬虫总览.md)
