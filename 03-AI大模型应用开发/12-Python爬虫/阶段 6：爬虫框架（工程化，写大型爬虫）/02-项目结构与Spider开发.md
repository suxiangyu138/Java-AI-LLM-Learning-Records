# 02 项目结构与 Spider 开发
> 工程化的起点：脚手架目录规范 + 四种 Spider 形态 + 请求回调链的完整写法

## 📚 目录
1. [项目脚手架与目录结构](#1-项目脚手架与目录结构)
2. [Spider 基类：属性、方法、请求契约](#2-spider-基类属性方法请求契约)
3. [Request 全参数与 meta 上下文传递](#3-request-全参数与-meta-上下文传递)
4. [回调链模式：列表页 → 详情页](#4-回调链模式列表页--详情页)
5. [四种 Spider 类型与选型](#5-四种-spider-类型与选型)
6. [动态起始请求：async start_requests](#6-动态起始请求async-start_requests)
7. [Spider 开发常见坑](#7-spider-开发常见坑)
8. [核心要点](#8-核心要点)

---

## 1. 项目脚手架与目录结构

### 1.1 创建项目

```bash
pip install scrapy==2.16.0        # 需 Python 3.10+
scrapy startproject quotes        # 创建项目
cd quotes
scrapy genspider quotes_spider quotes.toscrape.com   # 生成 Spider
scrapy crawl quotes_spider        # 运行
```

### 1.2 目录结构（工程化视角）

```text
quotes/
├── scrapy.cfg                 # 部署配置（指向 settings + 部署目标）
├── quotes/
│   ├── __init__.py
│   ├── items.py               # Item 定义（或迁移到 models.py，见 03 章）
│   ├── middlewares.py         # 中间件工厂模板（05 章）
│   ├── pipelines.py           # Pipeline 模板（04 章）
│   ├── settings.py            # 全局配置（架构级，01 章 §6）
│   ├── spiders/               # ⭐ 爬虫包
│   │   ├── __init__.py
│   │   └── quotes_spider.py
│   └── utils/                 # （自建）工具：指纹/时间/清洗函数
├── item_pipelines/            # （自建）按 Pipeline 分文件，避免 pipelines.py 膨胀
├── middlewares/               # （自建）按中间件分文件
├── data/                      # （自建）输出目录（gitignore）
└── jobdata/                   # （自建）JOBDIR 断点续爬目录（gitignore）
```

> 💡 工程化建议：**Spider 只做"取数据"**（yield Request / Item），解析出的 Item 交给 Pipeline 处理——Spider 里不要写数据库连接、不要写文件、不要写业务校验。这样"换数据源"只需换 Spider，"改存储"只需改 Pipeline。

### 1.3 新建项目的 5 个开箱配置

```python
# settings.py（新项目建议直接开启）
ROBOTSTXT_OBEY = False          # 学习/自有站点可开；正式抓站评估 robots（阶段 4 合规）
AUTOTHROTTLE_ENABLED = True     # 自动限速（05 章详解）
HTTPCACHE_ENABLED = True        # 开发期调试缓存（禁用时可防止反复打对方站点）
LOG_LEVEL = "INFO"
FEEDS = {"data/quotes.jsonl": {"format": "jsonlines", "encoding": "utf-8"}}  # 快速落盘
```

> ⚠️ 2.14 起官方弃用"Spider 类属性承载下载配置"的写法（如 `handle_httpstatus_list = [404]`、`retry_times` 类属性），一律收进 `custom_settings` 或 settings.py——2026 年新代码请按此规范。

## 2. Spider 基类：属性、方法、请求契约

### 2.1 骨架

```python
import scrapy


class QuotesSpider(scrapy.Spider):
    name = "quotes_spider"                 # 唯一标识，scrapy crawl 用它
    allowed_domains = ["quotes.toscrape.com"]   # 越域 URL 会被过滤（有例外见 §7）
    start_urls = ["https://quotes.toscrape.com/"]  # 起始 URL 列表

    def parse(self, response):
        """默认回调：解析起始响应。response = HtmlResponse"""
        for quote in response.css("div.quote"):
            yield {
                "text": quote.css("span.text::text").get(),
                "author": quote.css("small.author::text").get(),
            }
```

### 2.2 生命周期钩子（工程化必用）

| 钩子 | 触发时机 | 典型用途 |
|------|---------|---------|
| `start_requests()` | 爬取开始 | 从 DB/Redis 读种子 URL（替代 start_urls） |
| `parse()` | 每个响应 | 解析数据（可改名后经 callback 指定） |
| `closed(reason)` | 爬取结束 | 关闭连接/汇总状态/发通知 |
| `custom_settings` | 覆盖全局配置 | 单 Spider 独立的并发/管道/中间件 |

```python
class QuotesSpider(scrapy.Spider):
    name = "quotes_spider"
    custom_settings = {
        "CONCURRENT_REQUESTS_PER_DOMAIN": 4,      # 本 Spider 独享
        "ITEM_PIPELINES": {"quotes.pipelines.MysqlPipeline": 300},  # 独立管道
        "AUTOTHROTTLE_ENABLED": False,
    }

    def closed(self, reason):
        self.logger.info(f"爬取结束，原因: {reason}，共 yield {self.stats.get_value('item_scraped_count')} 条")
```

> 🎯 **核心要点**：`custom_settings` 是"一个项目多套抓取策略"的标准手段（同一工程内，A 站 4 并发免限速、B 站 1 并发限速）——比复制项目改 settings.py 干净得多。

## 3. Request 全参数与 meta 上下文传递

### 3.1 Request 参数速查

```python
yield scrapy.Request(
    url="https://quotes.toscrape.com/page/2/",
    callback=self.parse_detail,        # 指定回调（默认是 parse）
    errback=self.on_error,             # 下载失败兜底（网络错误/超时不会进回调！）
    meta={"page": 2, "source": "list"},    # 跨回调传递任意数据
    priority=10,                       # 优先级（01 章 §4.3）
    dont_filter=True,                  # 跳过指纹去重（小心使用！）
    headers={"Referer": "https://quotes.toscrape.com/"},  # 请求头
    cookies={"sessionid": "abc"},      # 请求级 Cookie
    method="POST",                     # 默认 GET
    body='{"key": "value"}',           # POST body
)
```

### 3.2 meta 传递的两种形态

```python
# 形态一：解析详情页时带上列表页的上下文
def parse(self, response):
    for item in response.css("div.quote"):
        yield scrapy.Request(
            url=item.css("span > a::attr(href)").get(),
            callback=self.parse_detail,
            meta={"author": item.css("small.author::text").get()},  # 已拿到的字段
        )

def parse_detail(self, response):
    yield {
        "author": response.meta["author"],          # 从 meta 取回
        "birthday": response.css("...::text").get(),
    }
```

> ⚠️ **meta 的两个坑**：(1) `meta` 里默认会被塞入若干框架字段（`download_timeout`、`redirect_urls` 等），覆盖它们会破坏下载行为，自定义键别用 `download_*` / `redirect_*` / `depth` 等保留名；(2) 请求被**重试**时 meta 原样带到第二次请求，若要区分首次/重试用 `meta.get("download_slot")` 或自定义 `retry_count` 计数。

## 4. 回调链模式：列表页 → 详情页

大型爬虫的标准骨架（列表分页 + 详情逐条），**本阶段第 9 章综合项目基于此模式扩展**：

```python
import scrapy
from urllib.parse import urljoin


class NewsSpider(scrapy.Spider):
    name = "news"
    allowed_domains = ["news.example.com"]
    start_urls = ["https://news.example.com/list/1"]

    def parse(self, response):
        """列表页：提取详情链接 + 下一页链接"""
        # 详情链接（注意：相对路径要先补全，用 response.urljoin 或 response.follow）
        for a in response.css("h2.title a"):
            yield scrapy.Request(
                url=response.urljoin(a.css("::attr(href)").get()),
                callback=self.parse_detail,
                meta={"title": a.css("::text").get().strip()},
            )
        # 下一页：继续 yield 回 parse（自递归翻页）
        next_page = response.css("a.next::attr(href)").get()
        if next_page:
            yield scrapy.Request(response.urljoin(next_page), callback=self.parse)

    def parse_detail(self, response):
        yield {
            "title": response.meta["title"],
            "url": response.url,
            "content": " ".join(response.css("div.article-body p::text").getall()),
            "publish_time": response.css("time::attr(datetime)").get(),
        }

    def on_error(self, failure):
        self.logger.warning("下载失败: %s", failure.request.url)
```

**工程化要点**：

| 要点 | 做法 |
|------|------|
| 相对 URL | 一律 `response.urljoin(...)` 或直接用 `response.follow(url, ...)`（自动拼接） |
| 终止条件 | 下一页链接不存在就停止——**永远写"拿不到下一页就停"的终止分支**，别依赖异常 |
| 深度控制 | 需要限制深度时 `meta={"depth": ...}` 自增判断，或 09 章讲的深度参数 |
| 详情页失败 | 详情页下载失败**不会**进 parse_detail——必须配 `errback` 记录待补抓 URL（增量思想，09 章） |
| 列表与详情并发 | 详情页优先级 `priority=10`，让详情先于后续列表页下载（01 章 §4.3） |

> 🎯 **核心要点**：回调链 = **"yield 下一个请求 + 指定 callback"的接力**。框架保证"响应回来一定进你指定的回调"，但"响应永远回不来"（超时/4xx/5xx）只走 errback——**errback 是大型爬虫可靠性的分水岭**，不写 errback 的爬虫丢数据是必然的。

## 5. 四种 Spider 类型与选型

| 类型 | 适用场景 | 关键点 |
|------|---------|--------|
| `scrapy.Spider` | 一切常规站点 | 手写 parse，最灵活 |
| `CrawlSpider` | 全站/栏目站、链接结构规则化 | 声明式 `Rule`，自动跟链接，**不用写回调链** |
| `XMLFeedSpider` | sitemap、RSS、API 的 XML 返回 | `itertag` 指定节点，`parse_node` 逐个处理 |
| `CSVFeedSpider` | CSV 数据源 | `delimiter`、`parse_row` 逐行处理 |

### CrawlSpider 示例（Rule 自动爬）

```python
import scrapy
from scrapy.spiders import CrawlSpider, Rule
from scrapy.linkextractors import LinkExtractor


class NewsCrawlSpider(CrawlSpider):
    name = "news_crawl"
    allowed_domains = ["news.example.com"]
    start_urls = ["https://news.example.com/"]

    rules = (
        # 列表页：符合 pattern 的链接继续爬，并回调 parse_list
        Rule(LinkExtractor(allow=r"/list/\d+"), callback="parse_list", follow=True),
        # 详情页：抓完即止（不 follow）
        Rule(LinkExtractor(allow=r"/article/\d+"), callback="parse_detail"),
    )

    def parse_list(self, response):
        # 列表页本身需要的信息（如栏目名）
        yield {"url": response.url, "type": "list"}

    def parse_detail(self, response):
        yield {"url": response.url, "title": response.css("h1::text").get()}
```

> ⚠️ CrawlSpider 的 `rules` 与手写回调链不能混用同一套 parse 命名：**CrawlSpider 默认 parse 是禁用的**（框架内部使用），自定义回调绝不能叫 `parse`。另外 `allow` 正则务必限定到栏目/详情前缀，否则会爬进评论区、用户页等无限角落。

> 💡 **选型口诀**：站点结构规则（栏目/详情 URL 模式清晰）→ CrawlSpider；结构不规则或需要大量业务判断 → 手写 Spider；RSS/API → XMLFeedSpider。

## 6. 动态起始请求：async start_requests

2.13+ 支持 `async def start_requests()`（2.14 异步现代化的一部分），适合"起始 URL 来自数据库/Redis/文件"的工程场景：

```python
import scrapy
import asyncio


class DbSeededSpider(scrapy.Spider):
    name = "db_seeded"

    async def start_requests(self):
        """从外部源读种子 URL（异步友好）"""
        # 例：从 Redis 读待抓队列（07 章分布式里很常用）
        urls = await self.load_seed_urls()
        for url in urls:
            yield scrapy.Request(url, callback=self.parse)

    async def load_seed_urls(self):
        # 模拟异步 IO（真实场景：aioredis / 数据库驱动）
        await asyncio.sleep(0.01)
        return ["https://example.com/a", "https://example.com/b"]

    async def parse(self, response):
        # 回调也可以 async（需要时才能 await）
        yield {"url": response.url}
```

> ⚠️ 使用 async 回调时若同时依赖 Playwright 等异步库，需配置 `TWISTED_REACTOR = "twisted.internet.asyncioreactor.AsyncioSelectorReactor"`（06 章详细展开）。

## 7. Spider 开发常见坑

| # | 坑 | 表现 | 解法 |
|:---:|-----|------|------|
| 1 | `allowed_domains` 误配 | 详情页跳到 CDN/外链域名被静默丢弃 | 按"所有会出现的域名"配置；拿不准就留空（仅开发期） |
| 2 | 相对 URL 没拼接 | 请求 404 或错误路径 | `response.follow()` / `response.urljoin()` |
| 3 | 忘写 errback | 详情页静默丢失、item_scraped_count 神秘减少 | 每个需要可靠性的请求配 errback |
| 4 | 解析写死依赖顺序 | 列表字段与详情字段拆分后对不上 | 用 meta 带上下文 + 详情页用 URL 做 join 键 |
| 5 | `dont_filter=True` 滥用 | 无限循环重复抓 | 只在真正需要"重复访问"时用（如轮询类），配深度上限 |
| 6 | 类属性配置（2.14 弃用） | 新版警告 DeprecationWarning | 迁移到 `custom_settings` |
| 7 | 中文乱码 | 页面编码非 UTF-8 时响应乱码 | 指定 `Request` 编码或自定义 Downloader 处理（05 章） |
| 8 | parse 里写业务 | Spider 越来越肿、难测试 | 拆 Pipeline / utils |

## 8. 核心要点

| 序号 | 要点 |
|:---:|------|
| 1 | 项目 = settings 全局配置 + spiders 业务包 + pipelines/middlewares 扩展层 |
| 2 | Spider 只产 Request / Item，yield 是契约；回调链 + errback 是大型爬虫骨架 |
| 3 | meta 跨回调传上下文，避开保留键；重试会原样携带 meta |
| 4 | 相对 URL 用 response.follow / urljoin，终止分支永远显式写 |
| 5 | CrawlSpider 用 Rule 声明式爬全站，回调不能叫 parse |
| 6 | async start_requests / async parse 是 2.13+ 工程化入口 |
| 7 | 2.14 弃用类属性配置 → custom_settings |

---

**下一模块**：[03-选择器与 Item 数据提取](03-选择器与Item数据提取.md) / **返回总览**：[00-阶段6爬虫框架总览](00-阶段6爬虫框架总览.md)

## 参考来源

- [Scrapy 官方教程（Spider 部分）](https://docs.scrapy.org/en/latest/intro/tutorial.html)
- [Scrapy Spiders 官方文档](https://docs.scrapy.org/en/latest/topics/spiders.html)
- [Scrapy Request/Response 官方文档](https://docs.scrapy.org/en/latest/topics/request-response.html)
- [CrawlSpider 官方文档](https://docs.scrapy.org/en/latest/topics/spiders.html#crawlspider)
- [Zyte Blog: Scrapy 2.14 异步现代化行动项](https://www.zyte.com/blog/scrapy-in-2026-modern-async-crawling/)
