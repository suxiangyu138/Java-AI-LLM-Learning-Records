# 04 - Scrapy 框架实战

> 🎯 Scrapy 是 Python 最强大的爬虫框架——内置异步引擎、自动去重、中间件、Pipeline，一个项目搞定从请求到存储的全流程，适合中大型爬虫项目

---

## 目录

1. [Scrapy 架构](#1-scrapy-架构)
2. [第一个 Spider](#2-第一个-spider)
3. [Item Pipeline](#3-item-pipeline)
4. [Middleware 中间件](#4-middleware-中间件)
5. [生产配置](#5-生产配置)

---

## 1. Scrapy 架构

```
┌────────────────────────────────────────────────────────┐
│                    Scrapy Engine                        │
│              (控制数据流、触发事件)                       │
└──────┬──────────┬───────────┬───────────┬──────────────┘
       │          │           │           │
  ┌────▼──┐  ┌───▼────┐  ┌───▼────┐  ┌───▼────┐
  │Spiders│  │Scheduler│  │Downloader│  │Item    │
  │(你的代码)│  │(URL队列) │  │(异步请求)│  │Pipeline│
  └───────┘  └───┬─────┘  └───┬─────┘  └───┬────┘
                 │             │             │
            ┌────▼─────────────▼─────────────▼────┐
            │         Middleware 中间件             │
            │  Spider Middleware  /  Downloader MW │
            └──────────────────────────────────────┘
```

| 组件 | 职责 | 你写什么 |
|------|------|---------|
| **Engine** | 核心调度 | 无需修改 |
| **Spiders** | 解析响应 + 提取数据 | ⭐ 主要写这里 |
| **Scheduler** | URL 去重 + 调度队列 | 配置即可 |
| **Downloader** | 异步下载 | 配置 Middleware |
| **Item Pipeline** | 清洗 + 存储 | ⭐ 数据后处理 |
| **Middleware** | 请求/响应拦截 | Headers/代理/IP |

## 2. 第一个 Spider

### 2.1 创建项目

```bash
# 创建项目
scrapy startproject myproject
cd myproject

# 生成 Spider
scrapy genspider quotes quotes.toscrape.com

# 项目结构
myproject/
├── scrapy.cfg
└── myproject/
    ├── spiders/
    │   └── quotes.py          ← 你的爬虫代码
    ├── items.py               ← 数据模型
    ├── pipelines.py           ← 数据处理管道
    ├── middlewares.py         ← 中间件
    └── settings.py            ← 全局配置
```

### 2.2 Spider 代码

```python
# spiders/quotes.py
import scrapy

class QuotesSpider(scrapy.Spider):
    name = "quotes"                         # 爬虫唯一标识
    allowed_domains = ["quotes.toscrape.com"]
    start_urls = ["https://quotes.toscrape.com"]

    def parse(self, response):
        """默认回调：解析列表页"""
        for quote in response.css("div.quote"):
            yield {
                "text": quote.css("span.text::text").get(),
                "author": quote.css("small.author::text").get(),
                "tags": quote.css("a.tag::text").getall(),
            }

        # 翻页：提取"下一页"链接
        next_page = response.css("li.next a::attr(href)").get()
        if next_page:
            yield response.follow(next_page, callback=self.parse)
```

```bash
# 运行
scrapy crawl quotes -o quotes.json
# 输出 JSON 文件
```

### 2.3 数据提取速查

```python
# CSS 选择器（Scrapy 扩展语法）
response.css("div.title::text").get()         # 取第一个文本
response.css("div.title::text").getall()      # 取所有文本（列表）
response.css("a::attr(href)").get()           # 取属性
response.css("a::attr(href)").getall()        # 取所有属性

# XPath 选择器
response.xpath("//h1/text()").get()
response.xpath("//a/@href").getall()

# 混合使用
response.css("div.product").xpath("./h3/text()").get()
```

## 3. Item Pipeline

### 3.1 定义 Item

```python
# items.py
import scrapy

class ProductItem(scrapy.Item):
    name = scrapy.Field()
    price = scrapy.Field()
    url = scrapy.Field()
    category = scrapy.Field()
    crawl_time = scrapy.Field()
```

### 3.2 Pipeline 数据清洗

```python
# pipelines.py
import re
from datetime import datetime

class CleanPipeline:
    """数据清洗 Pipeline"""

    def process_item(self, item, spider):
        # 价格标准化
        if item.get("price"):
            price_str = item["price"].replace("¥", "").replace(",", "").strip()
            try:
                item["price"] = float(price_str)
            except ValueError:
                item["price"] = None

        # 名称去空格
        if item.get("name"):
            item["name"] = item["name"].strip()

        # 添加爬取时间
        item["crawl_time"] = datetime.now().isoformat()
        return item


class MySQLPipeline:
    """MySQL 存储 Pipeline"""

    def open_spider(self, spider):
        import pymysql
        self.conn = pymysql.connect(
            host="localhost", user="root",
            password="", database="crawler_db"
        )
        self.cursor = self.conn.cursor()

    def process_item(self, item, spider):
        sql = """
        INSERT INTO products (name, price, url, crawl_time)
        VALUES (%s, %s, %s, %s)
        ON DUPLICATE KEY UPDATE price=%s, crawl_time=%s
        """
        self.cursor.execute(sql, (
            item["name"], item["price"], item["url"],
            item["crawl_time"], item["price"], item["crawl_time"]
        ))
        self.conn.commit()
        return item

    def close_spider(self, spider):
        self.cursor.close()
        self.conn.close()
```

```python
# settings.py — 启用 Pipeline（按数字顺序执行）
ITEM_PIPELINES = {
    "myproject.pipelines.CleanPipeline": 100,     # 先清洗
    "myproject.pipelines.MySQLPipeline": 200,     # 后存储
}
```

## 4. Middleware 中间件

### 4.1 User-Agent 轮换

```python
# middlewares.py
import random

class RotateUserAgentMiddleware:
    """随机 UA 中间件"""

    USER_AGENTS = [
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/125.0.0.0",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 14.5) Safari/605.1",
        "Mozilla/5.0 (X11; Linux x86_64) Firefox/126.0",
    ]

    def process_request(self, request, spider):
        request.headers["User-Agent"] = random.choice(self.USER_AGENTS)
```

### 4.2 代理中间件

```python
class ProxyMiddleware:
    """代理 IP 中间件"""

    def __init__(self):
        self.proxies = [
            "http://proxy1:8080",
            "http://proxy2:8080",
        ]

    def process_request(self, request, spider):
        request.meta["proxy"] = random.choice(self.proxies)

    def process_exception(self, request, exception, spider):
        # 代理失效 → 换一个重试
        request.meta["proxy"] = random.choice(self.proxies)
        return request  # 返回 request 触发重试
```

```python
# settings.py
DOWNLOADER_MIDDLEWARES = {
    "myproject.middlewares.RotateUserAgentMiddleware": 543,
    "myproject.middlewares.ProxyMiddleware": 544,
}
```

## 5. 生产配置

```python
# settings.py 生产配置

# 并发控制
CONCURRENT_REQUESTS = 16            # 全局并发数
CONCURRENT_REQUESTS_PER_DOMAIN = 8  # 单域名并发数
CONCURRENT_REQUESTS_PER_IP = 4      # 单 IP 并发数

# 下载延迟（反爬核心）
DOWNLOAD_DELAY = 1                  # 请求间隔（秒）
RANDOMIZE_DOWNLOAD_DELAY = True     # 随机 +/- 50%

# 重试
RETRY_ENABLED = True
RETRY_TIMES = 3
RETRY_HTTP_CODES = [429, 500, 502, 503, 504]

# 自动限速（推荐开启）
AUTOTHROTTLE_ENABLED = True
AUTOTHROTTLE_START_DELAY = 1       # 起始延迟
AUTOTHROTTLE_MAX_DELAY = 60        # 最大延迟
AUTOTHROTTLE_TARGET_CONCURRENCY = 2.0  # 目标并发

# 缓存
HTTPCACHE_ENABLED = True            # 开发时开启，避免重复请求
HTTPCACHE_EXPIRATION_SECS = 86400   # 缓存 24 小时

# Feed 导出
FEEDS = {
    "output/%(name)s_%(time)s.json": {
        "format": "json",
        "encoding": "utf-8",
        "indent": 2,
    }
}
```

## 核心要点回顾

- Scrapy 架构 = Engine + Spider + Scheduler + Downloader + Pipeline
- Spider 的核心方法：`parse()` → yield dict/Item → 走 Pipeline
- Pipeline 按数字顺序执行：先清洗 → 再存储
- Middleware 两大类：Spider Middleware（处理 Item）+ Downloader Middleware（处理 Request/Response）
- 生产配置：并发 8-16 + 延迟 1-3s + 自动限速 + 缓存 + 重试

## 参考资料

1. Scrapy 官方文档 — docs.scrapy.org
2. Scrapy 中文文档 — scrapy-chs.readthedocs.io
