# Python 爬虫必做项目 面试问答清单
> 🎯 基于项目实战清单，涵盖面试高频问题与完美解答方案，帮助你在 Python 爬虫岗位面试中脱颖而出。

## 目录
1. [基础概念与核心原理](#1-基础概念与核心原理)
2. [项目实战深度问答](#2-项目实战深度问答)
3. [进阶与系统设计](#3-进阶与系统设计)
4. [场景题与故障排查](#4-场景题与故障排查)

---

## 1. 基础概念与核心原理
> 💡 面试官在这一环节考察你的基本功是否扎实

### Q1：请对比 requests、aiohttp、Scrapy 三种请求方案的异同与适用场景
**面试官意图：** 考察你对 HTTP 请求层的理解深度，以及根据场景选择合适工具的能力。

**完美解答：**

三种方案分别对应同步阻塞、异步非阻塞、工程化框架三个层级：

| 对比维度 | requests | aiohttp | Scrapy |
|----------|----------|---------|--------|
| 模式 | 同步阻塞 | 异步非阻塞（asyncio） | 异步 + 事件驱动 |
| 并发能力 | 单线程串行 | 高并发协程 | 中间件 + Twisted 异步 |
| 上手难度 | 简单 | 中等 | 较复杂 |
| 扩展性 | 无框架支持 | 无框架支持 | 完整的 Spider/Pipeline/Middleware 体系 |
| 反爬对抗 | 手动配置 | 手动配置 | 内置中间件支持 |
| 数据持久化 | 手动实现 | 手动实现 | Item Pipeline 开箱即用 |
| 适用场景 | 小型项目、单页爬取 | 大规模并发请求 | 工程化、全链路爬虫 |

**核心结论：**
- **requests** 适合入门、调试、单次请求、小规模静态页面抓取
- **aiohttp** 适合 IO 密集型的大规模并发场景，如每天抓取数十万 URL
- **Scrapy** 适合完整的工程化爬虫项目，自带调度、去重、管道、限速等能力

**核心代码对比：**

```python
# requests — 同步
import requests
resp = requests.get('https://example.com', headers={'User-Agent': 'Mozilla/5.0'})
print(resp.text)

# aiohttp — 异步
import aiohttp, asyncio
async def fetch(url):
    async with aiohttp.ClientSession() as session:
        async with session.get(url) as resp:
            return await resp.text()

# Scrapy — 框架
import scrapy
class MySpider(scrapy.Spider):
    name = 'example'
    start_urls = ['https://example.com']
    def parse(self, response):
        yield {'title': response.css('h1::text').get()}
```

**延伸追问应对：** 如果追问"Scrapy 的请求调度机制"，回答——Scrapy 使用优先级队列（默认 LIFO 栈 + 可配置优先级），通过 Scheduler 组件管理待爬队列，支持去重过滤（用 RFPDupeFilter 对请求做指纹去重），并通过 DOWNLOAD_DELAY 和 CONCURRENT_REQUESTS 控制请求频率。

---

### Q2：对比 BeautifulSoup、lxml、正则表达式三种 HTML 解析方案
**面试官意图：** 考察数据解析层的基本功。

**完美解答：**

| 对比维度 | BeautifulSoup | lxml (XPath) | 正则表达式 |
|----------|---------------|--------------|-----------|
| 解析方式 | DOM 树 + CSS Selector | XPath 路径表达式 | 模式匹配 |
| 容错性 | 强（对破损 HTML 友好） | 中 | 弱 |
| 语法复杂度 | 简单 | 中等 | 较复杂 |
| 性能 | 中 | 快 | 快 |
| 定位能力 | CSS 类/ID/标签 | 父子/兄弟/属性 | 无结构感知 |
| 学习成本 | 低 | 中 | 中高 |

**最佳实践：**
- **BeautifulSoup** 优先用于快速开发、调试阶段、HTML 结构不规则时
- **lxml + XPath** 用于结构清晰、需要精确定位的页面（如表格、列表）
- **正则表达式** 仅在从字符串中提取特定模式时使用（如提取邮箱、电话号码），不应作为 HTML 解析首选

```python
# BeautifulSoup — CSS Selector
from bs4 import BeautifulSoup
soup = BeautifulSoup(html, 'html.parser')
titles = soup.select('div.article h2.title')

# lxml — XPath
from lxml import etree
tree = etree.HTML(html)
titles = tree.xpath('//div[@class="article"]//h2[@class="title"]/text()')

# 正则表达式 — 提取特定模式
import re
emails = re.findall(r'[\w.+-]+@[\w-]+\.[\w.-]+', html)
```

> 💡 **面试加分：** 主动说出"XPath 和 CSS Selector 的选择原则"——XPath 适合面向路径的精确查找（如多层嵌套、条件筛选），CSS Selector 适合面向样式的快速定位（如类名、ID）。实际项目中经常两者混用。

**延伸追问应对：** 如果问"HTML 解析和 API 接口数据如何选择"，回答——优先检查页面是否预置 JSON 数据（通过 Network 面板查看 XHR/Fetch 请求），如果能直接调用 API，性能远优于 HTML 解析。API 无法返回所需数据时再选择 HTML 解析方案。

---

### Q3：Selenium、Playwright、requests 三种动态页面方案如何选择？
**面试官意图：** 考察你对 JS 渲染机制的理解和动态页面抓取方案选型能力。

**完美解答：**

| 对比维度 | Selenium | Playwright | requests + API 逆向 |
|----------|----------|------------|-------------------|
| 底层原理 | WebDriver 协议控制浏览器 | CDP (Chrome DevTools Protocol) | 模拟 HTTP 请求 |
| 性能 | 慢（启动完整浏览器） | 中等（可复用上下文） | 极快 |
| 无头模式 | 支持 | 原生支持 | 不涉及 |
| 等待机制 | 隐式/显式/强制等待 | auto-wait + 断言 API | 需手动处理 |
| 反爬识别 | 易被检测 | 指纹更难识别 | 容易被封 IP |
| 学习曲线 | 中等 | 中等 | 低 |
| 生态成熟度 | 高（文档丰富） | 中（发展快） | 极高 |

**选型原则：**
- ✅ **优先选择 requests 逆向**：页面数据通过 API 接口返回时，直接模拟接口调用
- ✅ **选择 Playwright**：需要高性能动态渲染、反爬检测严苛时（Playwright 默认指纹更难识别）
- ✅ **选择 Selenium**：博客教程学习、企业内部管理系统（不需要过多反爬）
- ✅ **选择 Selenium**：项目已有 Selenium 体系代码，维护成本优先

```python
# Selenium
from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC

driver = webdriver.Chrome()
driver.get('https://example.com')
element = WebDriverWait(driver, 10).until(
    EC.presence_of_element_located((By.CLASS_NAME, 'content'))
)

# Playwright
from playwright.sync_api import sync_playwright
with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    page = browser.new_page()
    page.goto('https://example.com')
    content = page.wait_for_selector('.content').text_content()
```

> ⚠️ **注意：** Selenium 和 Playwright 启动浏览器本身占用 ~300MB 内存，每多一个并发实例内存翻倍。单机并发通常控制在 2-4 个浏览器实例。

---

## 2. 项目实战深度问答
> 💡 面试官会深挖你的项目细节

### Q4：请描述一个完整的静态网页单页爬取项目，以豆瓣电影为例
**面试官意图：** 考察你从分析目标到落盘的全流程能力和代码质量意识。

**完美解答：**

**为什么选择这个项目：** 豆瓣电影页面结构清晰、HTML 标签语义化好、无复杂 JS 渲染，是最适合新手入门的实战项目。同时豆瓣有基本的反爬策略（UA 检测、请求频率限制），可以练习基础反爬。

**具体实现步骤：**

1. **目标分析：** 打开豆瓣电影 Top250 页面，检查 HTML 结构，定位电影标题、评分、评价人数等字段所在标签
2. **请求构造：** 设置合理的 User-Agent 和 Accept 头，避免被识别为爬虫
3. **页面解析：** 使用 BeautifulSoup 的 CSS Selector 或 XPath 定位数据节点
4. **数据清洗：** 去除多余空格、空行，统一日期/评分格式
5. **数据存储：** 写入 CSV 文件，指定 UTF-8 编码

```python
import requests
from bs4 import BeautifulSoup
import csv
import time

headers = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
    'Accept': 'text/html,application/xhtml+xml',
    'Accept-Language': 'zh-CN,zh;q=0.9',
}

def fetch_movies():
    url = 'https://movie.douban.com/top250'
    resp = requests.get(url, headers=headers, timeout=10)
    resp.encoding = 'utf-8'
    soup = BeautifulSoup(resp.text, 'html.parser')

    movies = []
    for item in soup.select('div.item'):
        title = item.select_one('span.title').text
        rating = item.select_one('span.rating_num').text
        quote = item.select_one('span.inq')
        quote_text = quote.text.strip() if quote else ''

        movies.append({
            'title': title,
            'rating': rating,
            'quote': quote_text,
        })
    return movies

def save_to_csv(movies):
    with open('movies.csv', 'w', newline='', encoding='utf-8-sig') as f:
        writer = csv.DictWriter(f, fieldnames=['title', 'rating', 'quote'])
        writer.writeheader()
        writer.writerows(movies)

if __name__ == '__main__':
    movies = fetch_movies()
    save_to_csv(movies)
    print(f'共抓取 {len(movies)} 部电影')
```

**遇到的挑战与解决：**
- **问题：** 豆瓣对高频请求返回 418 状态码（被封）
- **解决：** 添加请求间隔 `time.sleep(1)`，同时伪装完整的 HTTP 请求头
- **效果：** 请求成功率从 60% 提升至 99% 以上

**延伸追问应对：** 如果问"为什么用 `utf-8-sig` 编码"，回答——`utf-8-sig` 会在文件开头写入 BOM（Byte Order Mark），Excel 打开 CSV 文件时能正确识别中文编码，否则会出现乱码。

---

### Q5：分页批量爬取如何实现断点续爬和异常恢复？
**面试官意图：** 考察数据规模的工程化处理能力和异常容灾意识。

**完美解答：**

分页爬取的核心三个要点：URL 规律识别、异常容错、断点续爬。

**1. 分页 URL 规律分析**

豆瓣 Top250 的分页规律是 `?start=0`、`?start=25`……`?start=225`，偏移量每页 +25。通用的分页模式有：

| 分页类型 | URL 示例 | 规律 |
|----------|----------|------|
| 查询参数 | `?page=1` / `?offset=0` | 数字递增 |
| RESTful | `/api/articles/1` | 路径数字递增 |
| 游标分页 | `?cursor=abc123` | 上次请求的最后一条记录的 ID |
| 无限滚动 | 无显式分页参数 | XHR 带 `page_size`+`last_id` |

**2. 异常容错**

```python
import time
from functools import wraps

def retry(max_retries=3, delay=1):
    def decorator(func):
        @wraps(func)
        def wrapper(*args, **kwargs):
            for attempt in range(max_retries):
                try:
                    return func(*args, **kwargs)
                except Exception as e:
                    if attempt == max_retries - 1:
                        raise
                    time.sleep(delay * (attempt + 1))  # 指数退避
            return None
        return wrapper
    return decorator

@retry(max_retries=3, delay=2)
def fetch_page(url):
    resp = requests.get(url, headers=headers, timeout=10)
    resp.raise_for_status()
    return resp.text
```

**3. 断点续爬设计**

核心思想：记录已完成的页码，下次启动时跳过。

```python
import json
import os

class CrawlerCheckpoint:
    def __init__(self, filepath='checkpoint.json'):
        self.filepath = filepath
        self.completed_pages = self._load()

    def _load(self):
        if os.path.exists(self.filepath):
            with open(self.filepath, 'r') as f:
                return set(json.load(f))
        return set()

    def mark_done(self, page):
        self.completed_pages.add(page)
        with open(self.filepath, 'w') as f:
            json.dump(list(self.completed_pages), f)

    def is_done(self, page):
        return page in self.completed_pages

# 使用示例
checkpoint = CrawlerCheckpoint()
for page in range(1, 11):
    if checkpoint.is_done(page):
        continue
    data = fetch_page(f'https://example.com?page={page}')
    save_data(data)
    checkpoint.mark_done(page)
```

**效果：** 即使爬虫中途崩溃或被强制中断，重新启动后自动跳过已完成的页面，无需人工干预。

---

### Q6：Selenium 爬取动态页面时如何处理无限滚动、懒加载和反爬？
**面试官意图：** 考察动态渲染页面爬取的核心技能和反爬实战经验。

**完美解答：**

**1. 无限滚动处理**

```python
from selenium import webdriver
from selenium.webdriver.common.by import By
import time

def scroll_to_load(driver, max_scrolls=20, scroll_pause=2):
    last_height = driver.execute_script('return document.body.scrollHeight')
    scrolls = 0

    while scrolls < max_scrolls:
        driver.execute_script('window.scrollTo(0, document.body.scrollHeight)')
        time.sleep(scroll_pause)

        new_height = driver.execute_script('return document.body.scrollHeight')
        if new_height == last_height:
            break  # 没有新内容加载
        last_height = new_height
        scrolls += 1
```

**2. 显式等待（替代 time.sleep）**

```python
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.common.by import By

# ✅ 正确做法：等待元素出现
element = WebDriverWait(driver, 10).until(
    EC.presence_of_element_located((By.CLASS_NAME, 'item'))
)

# ❌ 错误做法：固定等待
# time.sleep(5)  # 不可控，浪费性能
```

**3. 反爬对抗策略**

| 反爬手段 | 应对方案 |
|----------|----------|
| WebDriver 特征检测 | 添加 `options.add_experimental_option('excludeSwitches', ['enable-automation'])` |
| navigator.webdriver 检测 | 使用 CDP 命令行覆盖 `driver.execute_cdp_cmd('Page.addScriptToEvaluateOnNewDocument', ...)` |
| UA 检测 | 设置完整的 User-Agent，模拟真实浏览器 |
| 无头浏览器检测 | 使用 Playwright 替代 Selenium（指纹更难识别） |

```python
# Selenium 反检测配置
from selenium.webdriver.chrome.options import Options

options = Options()
options.add_argument('--disable-blink-features=AutomationControlled')
options.add_experimental_option('excludeSwitches', ['enable-automation'])
options.add_experimental_option('useAutomationExtension', False)

driver = webdriver.Chrome(options=options)
driver.execute_cdp_cmd('Page.addScriptToEvaluateOnNewDocument', {
    'source': 'Object.defineProperty(navigator, "webdriver", {get: () => undefined})'
})
```

**4. 无头浏览器配置**

```python
options.add_argument('--headless=new')         # Chrome 112+ 新版无头模式
options.add_argument('--no-sandbox')
options.add_argument('--disable-dev-shm-usage')
options.add_argument('--window-size=1920,1080')
```

> 💡 **面试加分：** 主动提到"无头浏览器和有头浏览器在反爬检测上的区别"——有些网站检测 `navigator.userAgent` 中是否包含 `HeadlessChrome` 关键字，新版 Chrome 的无头模式已与有头模式共享 UA，但屏幕分辨率、WebGL 渲染器等指纹仍有差异。

**延伸追问应对：** 如果问"无限滚动抓取如何避免重复数据"，回答——在每次滚动后对已抓取的元素 ID 做去重集合（Set）校验，新元素才加入数据列表。同时可以记录当前页面的 DOM 快照指纹，避免重复加载同一区域。

---

### Q7：Scrapy 框架的核心架构和组件是如何协同工作的？
**面试官意图：** 考察 Scrapy 框架的工程化理解深度。

**完美解答：**

**Scrapy 架构图的核心流程：**

```
           ┌──────────────┐
           │   Spider     │  ← 用户编写的爬虫逻辑
           └──────┬───────┘
                  │ 产生 Request
                  ▼
        ┌─────────────────┐
        │   Engine (引擎) │  ← 核心调度中心
        └──────┬──────────┘
               │ 传递 Request
               ▼
        ┌───────────────────┐
        │   Scheduler (调度器) │  ← 管理请求队列 + 去重
        └──────┬────────────┘
               │ 取出 Request
               ▼
        ┌───────────────────┐
        │ Downloader (下载器) │  ← 发送 HTTP 请求
        └──────┬────────────┘
               │ 返回 Response
               ▼
        ┌─────────────────┐
        │   Engine        │  → Spider.parse() 处理
        └──────┬──────────┘
               │ 产生 Item / Request
               ▼
        ┌────────────────┐
        │ Item Pipeline  │  ← 数据清洗、去重、持久化
        └────────────────┘
```

**六大组件详解：**

| 组件 | 职责 | 核心扩展点 |
|------|------|-----------|
| Spider | 定义爬取逻辑和数据解析规则 | `parse()` 方法、`start_requests()` |
| Engine | 控制数据流在各组件间流转 | 不直接扩展 |
| Scheduler | 管理待爬请求队列 + 去重 | 可替换为 Scrapy-Redis |
| Downloader | 发送 HTTP/HTTPS 请求 | Downloader Middleware |
| Item Pipeline | 数据清洗、验证、存储 | 多个 Pipeline 类串联 |
| Middleware | 在请求/响应流转时做增强 | Spider Middleware, Downloader Middleware |

**实战配置清单：**

```python
# settings.py 核心配置
ROBOTSTXT_OBEY = False           # 根据需求决定是否遵守 robots.txt
CONCURRENT_REQUESTS = 16         # 并发请求数
DOWNLOAD_DELAY = 1                # 下载延迟（秒）
COOKIES_ENABLED = False           # 禁用 Cookie（某些场景需要启用）
DEFAULT_REQUEST_HEADERS = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
    'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
}
ITEM_PIPELINES = {
    'myproject.pipelines.DuplicatesPipeline': 300,
    'myproject.pipelines.MongoDBPipeline': 400,
}
DOWNLOADER_MIDDLEWARES = {
    'myproject.middlewares.ProxyMiddleware': 100,
    'scrapy.downloadermiddlewares.useragent.UserAgentMiddleware': None,
}
```

**中间件典型用法（随机 User-Agent + 代理）：**

```python
# middlewares.py
import random
from scrapy import signals

class RandomUserAgentMiddleware:
    USER_AGENTS = [
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
        'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36',
        'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36',
    ]

    def process_request(self, request, spider):
        request.headers['User-Agent'] = random.choice(self.USER_AGENTS)
```

> 🎯 **总结：** Scrapy 最大的价值不在于某个单独的功能，而在于把请求调度、并发控制、去重、中间件链、数据管道全部集成在一个工程化的框架里，开发者只关注 Spider 和 Pipeline 的编写。

---

## 3. 进阶与系统设计
> 💡 拉开差距的环节

### Q8：如何设计一个高可用的代理 IP 池系统？
**面试官意图：** 考察系统设计能力和反爬对抗的工程化思维。

**完美解答：**

代理 IP 池的核心挑战：代理可用性低（时效性短）、质量参差不齐（速度/匿名级别）、多线程取用时的竞争问题。

**1. 系统架构**

```
                ┌──────────────────────┐
                │  代理源（免费+付费）   │
                │  西刺/快代理/付费API   │
                └──────┬───────────────┘
                       ▼
                ┌──────────────────────┐
                │  Proxy Fetcher (采集)  │
                │  定时轮询各代理源       │
                └──────┬───────────────┘
                       ▼
                ┌──────────────────────┐
                │  Proxy Validator (校验)│
                │  ping + 目标网站可用性  │
                └──────┬───────────────┘
                       ▼
                ┌──────────────────────┐
                │  Proxy Pool (存储+筛选)│ → Redis Sorted Set
                │  按速度/分数排序       │   (score = 响应时间)
                └──────┬───────────────┘
                       ▼
                ┌──────────────────────┐
                │  Proxy API (对外接口)  │
                │  GET /random          │
                │  GET /count           │
                └──────────────────────┘
```

**2. 数据结构设计（Redis Sorted Set）**

```python
# 存储格式
# Key: proxy_pool
# Member: ip:port
# Score: 响应时间（毫秒，越小越快）

# 获取最快代理
ZRANGE proxy_pool 0 0 WITHSCORES

# 获取随机代理
ZRANDMEMBER proxy_pool

# 代理失败降权
ZINCRBY proxy_pool 1000 "192.168.1.1:8080"

# 代理彻底不可用则移除
ZREM proxy_pool "192.168.1.1:8080"
```

**3. 质量打分机制**

```python
class ProxyScorer:
    def __init__(self):
        self.weight = {
            'response_time': 0.4,    # 响应时间权重
            'success_rate': 0.3,     # 成功率权重
            'anonymity': 0.2,        # 匿名级别权重
            'stability': 0.1,        # 稳定性权重
        }

    def score(self, proxy_info):
        rt_score = self._normalize_time(proxy_info['response_time'])
        sr_score = proxy_info['success_count'] / max(proxy_info['total_count'], 1)
        anon_score = {'透明': 0.3, '匿名': 0.7, '高匿': 1.0}.get(proxy_info['anonymity'], 0.5)
        stable_score = 1.0 if proxy_info['response_time_std'] < 500 else 0.5

        return (rt_score * self.weight['response_time'] +
                sr_score * self.weight['success_rate'] +
                anon_score * self.weight['anonymity'] +
                stable_score * self.weight['stability'])
```

**4. 使用策略**

```python
import random
import redis

class ProxyManager:
    def __init__(self):
        self.redis = redis.Redis()
        self.pool_key = 'proxy_pool'

    def get_random(self):
        """获取随机代理"""
        proxy = self.redis.srandmember(self.pool_key)
        return proxy.decode() if proxy else None

    def get_fastest(self, count=1):
        """获取最快的前 N 个代理"""
        proxies = self.redis.zrange(self.pool_key, 0, count - 1)
        return [p.decode() for p in proxies]

    def report_failure(self, proxy):
        """报告代理失败（降权）"""
        self.redis.zincrby(self.pool_key, 5000, proxy)

    def report_success(self, proxy):
        """报告代理成功（加分）"""
        current = self.redis.zscore(self.pool_key, proxy)
        if current and current > 100:
            self.redis.zincrby(self.pool_key, -100, proxy)
```

> 💡 **面试加分：** 主动提到"代理池维护的开销"——免费代理平均存活时间只有 5-15 分钟，需要每 3 分钟做一次全量校验。付费代理存活时间更长（小时级），但成本按流量计费。实际项目建议混合使用：付费代理做核心请求池，免费代理做补充。

---

### Q9：请设计一个基于 Scrapy-Redis 的分布式爬虫系统
**面试官意图：** 考察分布式爬虫架构的设计能力和对 Scrapy-Redis 的理解。

**完美解答：**

**1. 为什么需要分布式？**

| 指标 | 单机 Scrapy | 分布式 Scrapy-Redis |
|------|-------------|-------------------|
| 最大并发 | ~16-32 请求 | 数百至数千请求 |
| 存储总请求数 | 内存受限 | Redis 可存千万级 URL |
| 去重持久化 | 进程内（重启丢失） | Redis 持久化去重集合 |
| 高可用 | 单点故障 | 多 Worker 容错 |
| 水平扩展 | 不支持 | 加机器即可 |

**2. 核心架构**

```
                          ┌────────────────────┐
                          │    Redis 数据库      │
                          │ ┌────────────────┐  │
                          │ │ Request 队列    │  │ ← Spider 将 Request 推入
                          │ │ (List)         │  │
                          │ ├────────────────┤  │
                          │ │ 去重指纹集合     │  │ ← RFPDupeFilter
                          │ │ (Set)          │  │
                          │ ├────────────────┤  │
                          │ │ Item 汇总队列   │  │ ← Spider 产出的数据
                          │ │ (List)         │  │
                          │ └────────────────┘  │
                          └─────────┬───────────┘
                                    │
            ┌───────────────────────┼──────────────────────┐
            │                       │                      │
      ┌─────▼─────┐          ┌─────▼─────┐          ┌─────▼─────┐
      │ Worker 1  │          │ Worker 2  │          │ Worker N  │
      │ Scrapy    │          │ Scrapy    │    ...    │ Scrapy    │
      │ Spider    │          │ Spider    │          │ Spider    │
      │ Pipeline  │          │ Pipeline  │          │ Pipeline  │
      └───────────┘          └───────────┘          └───────────┘
```

**3. 关键配置**

```python
# settings.py — 分布式配置
# 使用 Scrapy-Redis 调度器替换默认调度器
SCHEDULER = "scrapy_redis.scheduler.Scheduler"
DUPEFILTER_CLASS = "scrapy_redis.dupefilter.RFPDupeFilter"

# Redis 连接配置
REDIS_URL = 'redis://192.168.1.100:6379'

# 调度器配置
SCHEDULER_PERSIST = True           # 爬取结束后保留 Redis 中的请求队列
SCHEDULER_FLUSH_ON_START = False   # 启动时不清空队列（支持断点续爬）
SCHEDULER_IDLE_BEFORE_CLOSE = 10   # 空闲后等待秒数再关闭

# 去重配置
DUPEFILTER_DEBUG = True

# Item 存储
ITEM_PIPELINES = {
    'scrapy_redis.pipelines.RedisPipeline': 300,  # 将 Item 写入 Redis
}
```

**4. Spider 编写差异**

```python
from scrapy_redis.spiders import RedisSpider

class MyDistributedSpider(RedisSpider):
    name = 'myspider'
    # 不再使用 start_urls，改为从 Redis 读取起始 URL
    redis_key = 'myspider:start_urls'

    def parse(self, response):
        # 同普通 Spider
        yield {'url': response.url, 'title': response.css('h1::text').get()}
```

**使用方法：**
```bash
# 1. 在所有 Worker 上启动爬虫
scrapy crawl myspider

# 2. 向 Redis 推送种子 URL
redis-cli LPUSH myspider:start_urls "https://example.com/page1"
redis-cli LPUSH myspider:start_urls "https://example.com/page2"

# 3. 多个 Worker 自动从 Redis 队列中领取 Request 执行
```

> ⚠️ **注意：** Redis 在分布式爬虫中是单点瓶颈。如果 Redis 宕机，整个爬虫系统不可用。生产环境需要对 Redis 做主从 + Sentinel 高可用方案。

---

### Q10：如何设计一个高并发异步爬虫来提升数据采集效率？
**面试官意图：** 考察你对异步编程的理解深度和性能优化实战经验。

**完美解答：**

**1. 同步 vs 异步的吞吐量对比**

| 方案 | 100 个请求耗时 | CPU 利用率 | 代码复杂度 |
|------|---------------|-----------|-----------|
| requests 串行 | ~50s (每个 0.5s) | 低 | 低 |
| 多线程 ThreadPool | ~2-5s | 中 | 中 |
| asyncio + aiohttp | ~0.5-1s | 高 | 中高 |
| 多进程 + aiohttp | ~0.3-0.8s | 极高 | 高 |

**2. asyncio 并发爬虫核心实现**

```python
import asyncio
import aiohttp
import csv
from asyncio import Semaphore

class AsyncCrawler:
    def __init__(self, urls, concurrency=50, output='data.csv'):
        self.urls = urls
        self.semaphore = Semaphore(concurrency)  # 信号量控制并发
        self.session = None
        self.results = []
        self.output = output

    async def fetch(self, url):
        """单个请求，带信号量控制"""
        async with self.semaphore:
            for attempt in range(3):  # 重试 3 次
                try:
                    async with self.session.get(url, timeout=aiohttp.ClientTimeout(total=10)) as resp:
                        if resp.status == 200:
                            html = await resp.text()
                            return {'url': url, 'html': html, 'status': resp.status}
                        elif resp.status == 429:
                            await asyncio.sleep(2 ** attempt)  # 指数退避
                        else:
                            break
                except (aiohttp.ClientError, asyncio.TimeoutError) as e:
                    if attempt == 2:
                        return {'url': url, 'error': str(e)}
                    await asyncio.sleep(1)
        return {'url': url, 'error': 'failed'}

    async def run(self):
        """批量任务调度"""
        connector = aiohttp.TCPConnector(limit=100, limit_per_host=10)
        timeout = aiohttp.ClientTimeout(total=30)
        async with aiohttp.ClientSession(connector=connector, timeout=timeout) as session:
            self.session = session
            tasks = [self.fetch(url) for url in self.urls]
            results = await asyncio.gather(*tasks, return_exceptions=True)
            self.results = [r for r in results if isinstance(r, dict)]

    def save(self):
        with open(self.output, 'w', newline='', encoding='utf-8') as f:
            writer = csv.DictWriter(f, fieldnames=['url', 'status', 'error'])
            writer.writeheader()
            writer.writerows(self.results)

# 使用
urls = [f'https://example.com/page/{i}' for i in range(1000)]
crawler = AsyncCrawler(urls, concurrency=50)
asyncio.run(crawler.run())
crawler.save()
```

**3. 性能优化参数调优**

| 参数 | 推荐值 | 说明 |
|------|--------|------|
| `TCPConnector(limit)` | 100-200 | 连接池总大小 |
| `TCPConnector(limit_per_host)` | 10-20 | 单个主机并发上限，避免被封 |
| `Semaphore` 并发数 | 30-100 | 控制同时进行的请求数 |
| `aiohttp.ClientTimeout(total)` | 10-30s | 请求超时时间，避免阻塞 |
| `retry` 重试次数 | 2-3 | 网络抖动容错 |

**延伸追问应对：** 如果问"asyncio 和多线程的本质区别"，回答——asyncio 是**协作式并发**（主动让出控制权 `await`），切换开销在微秒级；多线程是**抢占式并发**（操作系统调度），切换开销在毫秒级且存在 GIL 限制。IO 密集型任务 asyncio 远优于多线程。

---

## 4. 场景题与故障排查
> 💡 考察实际解决问题的能力

### Q11：某天你运行的爬虫突然全部返回 403/418 状态码，如何处理？
**面试官意图：** 考察面对反爬升级时的排查思路和应急处理能力。

**完美解答：**

这是爬虫面试中最经典的故障场景。以下是标准排查流程：

**第一步：确认故障范围（5 分钟）**
```bash
# 1. 测试目标网站是否可用
curl -I https://target.com -A "Mozilla/5.0"

# 2. 比对当前爬虫的请求和浏览器正常访问的差异
# 使用浏览器 DevTools → Network → 查看正常请求的 Headers
```

**第二步：按优先级逐一排查**

| 排查项 | 解决方案 | 修复耗时 |
|--------|----------|----------|
| User-Agent 过期或被加入黑名单 | 更换为最新 Chrome UA | 5 分钟 |
| Cookie/Session 未携带 | 添加 Session 维持 | 10 分钟 |
| IP 被临时或永久封禁 | 切换代理 IP | 10 分钟 |
| 请求频率过高触发了限流 | 增加 `DOWNLOAD_DELAY` 或限速 | 5 分钟 |
| 网站新增了 JS Challenge（如 Cloudflare） | 升级为 Playwright/Selenium | 1-2 小时 |
| 网站新增了验证码（CAPTCHA） | 接入打码平台或识别模型 | 2-4 小时 |

**第三步：应急处理脚本**

```python
import random

class AntiBlockStrategy:
    """反封锁应急策略包"""

    @staticmethod
    def rotate_ua():
        uas = [
            'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36',
            'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36',
            'Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:126.0) Gecko/20100101 Firefox/126.0',
        ]
        return random.choice(uas)

    @staticmethod
    def random_delay(min_s=1, max_s=3):
        time.sleep(random.uniform(min_s, max_s))

    @classmethod
    def enhanced_headers(cls):
        return {
            'User-Agent': cls.rotate_ua(),
            'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8',
            'Accept-Language': 'zh-CN,zh;q=0.9,en;q=0.8',
            'Accept-Encoding': 'gzip, deflate, br',
            'Connection': 'keep-alive',
            'Upgrade-Insecure-Requests': '1',
            'Sec-Fetch-Dest': 'document',
            'Sec-Fetch-Mode': 'navigate',
            'Sec-Fetch-Site': 'none',
            'Sec-Fetch-User': '?1',
        }
```

**第四步：长期方案**

```python
# 限速器 — 适配目标网站的速率限制
class RateLimiter:
    def __init__(self, calls_per_minute=30):
        self.min_interval = 60.0 / calls_per_minute
        self.last_call_time = 0

    def wait(self):
        now = time.time()
        elapsed = now - self.last_call_time
        if elapsed < self.min_interval:
            time.sleep(self.min_interval - elapsed)
        self.last_call_time = time.time()
```

> 🎯 **面试加分：** 主动总结"反爬对抗不是一次性工作，而是持续博弈的过程"。当一种反爬手段被破解后，网站会升级更复杂的检测机制。成熟的爬虫项目需要有"监控报警 → 人工介入 → 策略升级"的闭环流程。

---

### Q12：爬虫抓取的数据质量差（字段缺失/乱码/重复），如何排查与修复？
**面试官意图：** 考察数据质量意识和问题定位能力。

**完美解答：**

**1. 常见数据质量问题速查表**

| 问题表现 | 可能原因 | 解决方案 |
|----------|----------|----------|
| 中文乱码 | 页面编码与解析编码不一致 | 检查 `resp.encoding` 或通过 `chardet` 自动检测编码 |
| 字段缺失（None） | CSS Selector/XPath 定位失效 | 检查是否有 iframe 或动态加载 |
| 数据重复 | 分页逻辑未去重或页面包含重复内容 | 使用 URL 指纹去重 + 业务主键去重 |
| 数值格式不一致 | 不同页面使用不同格式 | 统一清洗正则：`re.sub(r'\s+', '', val)` |
| 空行/空白字符 | HTML 中包含大量排版空格 | `str.strip()` + 过滤空字符串 |

**2. 乱码排查与修复**

```python
import chardet

# 方案一：通过 Content-Type 获取编码
resp = requests.get(url)
if resp.encoding and resp.encoding.lower() != 'utf-8':
    resp.encoding = 'utf-8'  # 强制指定

# 方案二：自动检测编码
detected = chardet.detect(resp.content)
resp.encoding = detected['encoding']

# 方案三：从 HTML meta 标签读取编码
import re
match = re.search(r'<meta.*?charset=["\']?([\w-]+)', resp.text, re.IGNORECASE)
if match:
    resp.encoding = match.group(1)
```

**3. 多层级去重策略**

```python
class MultiLevelDeduplicator:
    def __init__(self):
        self.url_fingerprints = set()   # URL 级别
        self.content_fingerprints = set()  # 内容级别
        self.business_keys = set()      # 业务主键级别

    def is_duplicate(self, item):
        # 第一层：URL 去重
        if item.get('url') in self.url_fingerprints:
            return True

        # 第二层：内容指纹去重（MD5）
        import hashlib
        content = f"{item['title']}{item['content'][:100]}"
        fp = hashlib.md5(content.encode()).hexdigest()
        if fp in self.content_fingerprints:
            return True

        # 第三层：业务主键去重
        biz_key = f"{item['source']}:{item['article_id']}"
        if biz_key in self.business_keys:
            return True

        # 全部通过后记录
        self.url_fingerprints.add(item.get('url'))
        self.content_fingerprints.add(fp)
        self.business_keys.add(biz_key)
        return False
```

**4. 数据质量监控（实用方案）**

```python
class DataQualityMonitor:
    """数据质量监控器"""
    def __init__(self):
        self.stats = {
            'total': 0,
            'missing_title': 0,
            'missing_content': 0,
            'encoding_errors': 0,
            'duplicates': 0,
        }

    def validate(self, item):
        self.stats['total'] += 1

        issues = []
        if not item.get('title'):
            self.stats['missing_title'] += 1
            issues.append('missing_title')

        if not item.get('content') or len(item['content'].strip()) < 10:
            self.stats['missing_content'] += 1
            issues.append('missing_content')

        return {
            'is_valid': len(issues) == 0,
            'issues': issues
        }

    def report(self):
        missing_rate = (self.stats['missing_title'] + self.stats['missing_content']) / max(self.stats['total'], 1)
        print(f"总计: {self.stats['total']}, 缺失率: {missing_rate:.1%}")
        if missing_rate > 0.1:  # 缺失率超过 10% 告警
            print(f"⚠️ 数据质量告警：字段缺失率 {missing_rate:.1%}，请检查解析逻辑")
```

> 💡 **核心原则：** 数据清洗的目标不是让每一条数据都完美，而是通过监控 + 告警 + 自动化修复的闭环，将异常率控制在可接受范围（通常 < 5%）。更重要的是记录异常数据的原始上下文（保存原始 HTML），便于事后修复。

---

## 💎 面试加分金句
- **"爬虫的本质是程序化模拟浏览器行为，关键在于理解 HTTP 协议和浏览器的渲染机制。"** — 展现你从协议层面理解爬虫，而非只会调库。
- **"反爬对抗不是一次性工作，而是持续博弈的过程。成熟的爬虫系统需要'监控报警 → 策略升级 → 灰度验证'的闭环。"** — 展现工程化思维。
- **"爬虫不仅仅要关注抓取成功率，更要关注数据质量。一条干净的数据胜过一千条肮脏的数据。"** — 展现质量意识。
- **"单机爬虫永远存在上限，分布式爬虫的设计核心在于'请求队列 + 去重'的集中化，而不是 Worker 本身。"** — 展现架构思维。
- **"合规是一切爬虫业务的前提。只抓取公开数据，严格遵守 robots.txt，控制合理频率，不用于商业竞争。"** — 展现职业操守和合规意识。

## 📋 高频追问清单

| 追问方向 | 应对策略 |
|----------|----------|
| requests 的 Session 和 Cookie 机制 | 说明 Session 对象自动管理 Cookie，配合 `requests.Session()` 实现登录态保持 |
| XPath `//` 和 `/` 的区别 | `//` 从任意位置匹配（全文档搜索），`/` 从当前节点直接子节点匹配 |
| CSS Selector 基本语法 | 掌握 `#id`、`.class`、`tag`、`div > p`、`div + p`、`[attr=value]` 核心语法 |
| Selenium 等待机制 | 区分隐式等待（`implicitly_wait`）、显式等待（`WebDriverWait`）、强制等待（`time.sleep`） |
| aiohttp 的 `ClientSession` 复用 | 复用 Session 保持连接池和 Cookie，避免每个请求新建 Session |
| Scrapy 的 Middleware 优先级 | 数字越小越靠近 Engine，请求时数字小的先处理，响应时数字大的先处理 |
| Scrapy-Redis 去重原理 | 使用 `RFPDupeFilter` 对 `(method, url, body, headers)` 计算 SHA1 指纹，存入 Redis Set |
| 异步 vs 多线程性能对比 | asyncio 协程切换开销 ~1μs，多线程切换 ~10μs + GIL 限制 |
| 代理 IP 匿名级别 | 透明代理（传递真实 IP）、匿名代理（隐藏真实 IP 但标记为代理）、高匿代理（完全伪装） |
| robots.txt 合规 | `User-agent: *` 控制所有爬虫，`Disallow: /private/` 禁止爬取路径 |

## 🔗 关联知识点
- [Python 基础 — requests 库核心用法](https://docs.python-requests.org/)
- [BeautifulSoup 官方文档](https://www.crummy.com/software/BeautifulSoup/bs4/doc/)
- [Scrapy 官方教程](https://docs.scrapy.org/en/latest/)
- [Playwright Python 文档](https://playwright.dev/python/)
- [Scrapy-Redis 分布式爬虫](https://github.com/rmax/scrapy-redis)
- [aiohttp 异步 HTTP 文档](https://docs.aiohttp.org/)
