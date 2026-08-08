# 01 Scrapy 架构与数据流
> 本阶段认知地基：五组件如何协作 + 一条请求的完整旅程 + 2026 异步现代化

## 📚 目录
1. [Scrapy 是什么：与 requests 脚本的本质差异](#1-scrapy-是什么与-requests-脚本的本质差异)
2. [五大组件与职责](#2-五大组件与职责)
3. [八步数据流：一个请求的完整旅程](#3-八步数据流一个请求的完整旅程)
4. [调度器与去重：并发的心脏](#4-调度器与去重并发的心脏)
5. [异步引擎：Twisted 与 2.14+ 异步现代化](#5-异步引擎twisted-与-214-异步现代化)
6. [settings.py 架构级配置](#6-settingspy-架构级配置)
7. [核心要点](#7-核心要点)

---

## 1. Scrapy 是什么：与 requests 脚本的本质差异

Scrapy 是 Python 最主流的爬虫框架，**2.16.0**（2026-05-19 发布，官方支持 Python 3.14 与 Twisted 26.4.0+；最低要求 Python 3.10）。它不是一个"更快的 requests"，而是**一个完整的爬虫操作系统**：

| 维度 | requests 手写脚本 | Scrapy |
|------|------------------|--------|
| 并发 | 自己写线程池/异步循环 | 内置异步引擎（无感高并发） |
| 请求管理 | 手动维护队列/去重 | 调度器自动管理（优先级/去重/持久化） |
| 数据流 | 循环里逐段代码 | 管道化：Spider → Pipeline 每层可插拔 |
| 横切逻辑 | 每个循环里复制粘贴 | 中间件统一拦截（UA/代理/重试） |
| 异常处理 | try/except 到处散落 | 重试中间件 + errback 统一兜底 |
| 断点续爬 | 自己设计 | Scheduler 持久化开箱即用 |
| 扩展 | 推倒重写 | 加一个 Pipeline / 中间件即可 |
| 部署运维 | 无 | Scrapyd / Docker / Stats 统计 |

> 🎯 **核心要点**：手写脚本解决"**爬一次**"，Scrapy 解决"**十万次、天天爬、坏了能续、能上生产**"。判断要不要上框架：页面 > 3 个、需要并发 > 10、需要定时增量——三个条件任一命中就用框架。

## 2. 五大组件与职责

```text
                    ┌─────────────┐
                    │   Engine    │  引擎：总指挥，调度一切
                    └──────┬──────┘
         ┌─────────┬───────┼───────┬──────────┐
         ▼         ▼       ▼       ▼          ▼
   ┌─────────┐ ┌────────┐ ┌──────────┐ ┌────────────┐ ┌─────────────┐
   │Scheduler│ │Downloader│ │ Spiders  │ │ItemPipeline│ │  Middleware │
   │ 调度器   │ │ 下载器   │ │  爬虫    │ │ 数据管道   │ │  中间件(环)  │
   └─────────┘ └────────┘ └──────────┘ └────────────┘ └─────────────┘
   请求队列+去重 发 HTTP 拿响应 解析→产出请求/Item  逐层清洗入库  请求/响应/Item 拦截
```

| 组件 | 职责 | 类比 | 开发者的工作 |
|------|------|------|------------|
| **Engine** | 总指挥，控制数据流在组件间流转 | 主板/总线 | 不碰 |
| **Scheduler** | 接收请求、去重、按优先级排队、出队 | 任务调度中心 | 配置去重器/优先级 |
| **Downloader** | 异步发 HTTP 请求，拿回 Response | 网络层 | 中间件里管代理/UA |
| **Spiders** | 定义起始 URL、解析响应、产出 **Request** 或 **Item** | 你写的核心业务 | ⭐ 主要工作 |
| **Item Pipeline** | 按序处理 Item：校验/清洗/入库 | 流水线工人 | ⭐ 主要工作 |
| **Middlewares** | 请求下载前/后、Spider 输入/输出拦截 | 安检/过滤器 | 反爬治理主战场 |

> 💡 理解框架的最高效姿势：**只记住一句数据流**——"Engine 从 Scheduler 拿一个 Request → 交 Downloader 下载 → 响应进 Spider 解析 → 解析出的新 Request 进 Scheduler、解析出的 Item 进 Pipeline"。整章下面都是这句话的展开。

## 3. 八步数据流：一个请求的完整旅程

以 `scrapy crawl quotes` 抓取一个列表页为例，一次完整爬取周期：

```text
① Engine 从 Scheduler 取出第一个 Request（quotes.toscrape.com）
② Engine 把 Request 交给 Downloader（先过 下载中间件 的 process_request）
③ Downloader 发起 HTTP 请求，拿到 Response
④ 响应回程：再次穿过下载中间件（process_response）→ Engine
⑤ Engine 把 Response 交给 Spider（先过 Spider 中间件的 process_spider_input）
⑥ Spider.parse() 解析响应：
      └─ 提取到链接 → yield Request → 经 process_spider_output 回到 ①（新周期）
      └─ 提取到数据 → yield Item → 经 process_spider_output → ⑦
⑦ Item 进入 Item Pipeline 逐层处理（每层返回处理后的 item 或 DropItem 丢弃）
⑧ 全部请求耗尽（Scheduler 空 + Downloader 空闲）→ Engine 关闭 Spider，
     输出统计（Stats），写入关闭日志
```

**关键工程含义**（面试必问）：

- **并发不是"同时发 N 个"而是"流水线交织"**：Spider 解析请求 A 的同时，Downloader 在下载请求 B，Scheduler 在排队请求 C——整个系统由**事件循环**驱动，单线程异步但吞吐远超同步循环。
- **yield 是框架的核心契约**：`parse()` 里 `yield Request` / `yield Item`，函数变成**生成器**，Engine 边消费边推进，你不需要手动管理循环。
- **两个队列决定终止条件**：Scheduler 请求队列空 + Downloader 无在途请求 → 爬取结束。死循环（A 页面不断产出生成 B 的请求）会让队列永不枯竭，所以要靠去重器（§4）+ `depth`/`allow` 约束兜底。

> 🎯 **核心要点**：把"一个请求的旅程"按八步讲清楚，等于讲清楚了 Scrapy 的全部——面试 80% 的 Scrapy 题（数据流、组件职责、中间件执行顺序）都从这张图派生。

## 4. 调度器与去重：并发的心脏

### 4.1 调度器组件构成

```text
Scheduler = 内存队列 / 磁盘队列 / 优先级队列
          + 去重器（DupeFilter）
          + 持久化（job 目录，可选）
```

- **队列选择**（`SCHEDULER_QUEUE_CLASS`）：默认 **DownloaderAwarePriorityQueue**（2.14.0 起为默认，取代旧 PriorityQueue）——它按"优先级 × 下载器状态"组织队列，**内存友好、支持请求优先级**，是 2.14 异步现代化的一部分。
- **去重器**（`DUPEFILTER_CLASS`）：默认 `RFPDupeFilter`，对 Request 计算 **指纹**（method + url + body + headers 关键位的 SHA1），已见过的直接丢弃，不发给 Downloader。
- **持久化**（`JOBDIR`）：设置后 Scheduler 状态与去重集合**落盘**，重启 Spider 自动续爬（断点续爬的本地形态；分布式形态见 07 章）。

### 4.2 去重生效的位置

```text
Spider yield Request → Spider 中间件 → Engine → Scheduler
                                              │
                    ┌─────────────────────────┤
                    ▼                         ▼
              去重器判定                入队列等下载
              (重复→直接丢弃)           (按优先级出队)
```

> ⚠️ 去重基于 **Request 指纹**而非 URL 字符串：`?page=1` 与 `?page=1#a`（fragment 不参与指纹）不会误判重复；但**带时间戳/随机参数的 URL 每次指纹都不同**，会绕过去重造成重复下载——这类 URL 应自己清洗后再 yield。

### 4.3 优先级（`Request.priority`）

| 值 | 用途 |
|------|------|
| 默认 0 | 常规页 |
| 正数（如 10） | 详情页优先于列表页（先抓热门内容） |
| 负数（如 -10） | 低优先级批量页，避免挤占队列 |

> 💡 优先级只在 DownloaderAwarePriorityQueue 下真正生效（2.14+ 默认即此）；旧 PriorityQueue 无此能力。列表页持续 yield 详情页请求时，给详情页更高优先级可显著改善"先出列表、详情在后面慢慢补"的延迟。

## 5. 异步引擎：Twisted 与 2.14+ 异步现代化

### 5.1 双事件循环共存

Scrapy 构建在 **Twisted**（事件驱动网络框架）之上。2020 年 2.0 起支持 asyncio 协程回调；2026 年 2.14 后进一步现代化：

```text
Scrapy 2.14 前                Scrapy 2.14+（2026-01-05 发布）
──────────────────            ───────────────────────────
CrawlerProcess (Deferred)     CrawlerProcess + AsyncCrawlerProcess（协程版）
Deferred 回调链                + download_request_async() / download_async()
                              + start_async() / stop_async() / close_async()
                              + open_spider_async() / close_spider_async()
```

- **`start_requests()` 可用 `async def`**：2.13+ 支持异步起始请求（例如先查 Redis/DB 再决定抓哪些 URL）。
- **回调可用 `async def`**：`parse` 内可以 `await`（调用 asyncio 库、Playwright API 等），需要 `TWISTED_REACTOR = "twisted.internet.asyncioreactor.AsyncioSelectorReactor"` 时二者完美共存。
- **2.15.0（2026-04-09）**：实验性支持"**无 Twisted reactor 运行**" + 实验性 **httpx 下载处理器**（Downloader 可选非 Twisted 实现）——方向是逐步摆脱 Deferred 遗留 API，走向纯 asyncio。2.16.0（2026-05-19）官方支持 Python 3.14 与 Twisted 26.4.0+。

### 5.2 对写爬虫的实际影响

| 场景 | 写法 |
|------|------|
| 普通请求/解析 | 不需要知道 reactor，yield Request 即可 |
| 需要 await（如 Playwright 的异步 API） | 回调定义成 `async def parse(self, response): ...` + 配置 asyncio reactor |
| 集成 asyncio 库（aiohttp 测速等） | `from twisted.internet import asyncioreactor; asyncioreactor.install()`（2.14+ 可经 `AsyncCrawlerProcess` 更优雅地混用） |
| 在自己的程序里运行爬虫 | `AsyncCrawlerProcess` / `AsyncCrawlerRunner`（协程版，避免 Deferred 回调地狱） |

> 🎯 **核心要点**：2026 年的 Scrapy 已经是"**Twisted 底座 + asyncio 接口**"的双轨框架——你日常写 Spider 完全无感，但做**动态页集成（06 章）**和**代码内嵌运行爬虫**时必须用到 async API。面试记住时间线：2.14 异步现代化（Async* 系列 API）、2.15 httpx 实验下载器、2.16 Python 3.14 支持。

## 6. settings.py 架构级配置

架构相关的全局配置（每一组在后续章节展开）：

```python
# settings.py —— 架构级配置速览
BOT_NAME = "quotes"                      # 项目名，也是默认 UA 前缀

# ── 并发与延迟（05 章详解） ──
CONCURRENT_REQUESTS = 32                 # 全局并发上限
CONCURRENT_REQUESTS_PER_DOMAIN = 8       # 单域名并发（反爬友好）
DOWNLOAD_DELAY = 0.5                     # 同一域名的请求间隔（秒）
RANDOMIZE_DOWNLOAD_DELAY = True          # 随机 ±50% 防机器特征
AUTOTHROTTLE_ENABLED = True              # 自动限速（推荐）

# ── 调度器（本章） ──
SCHEDULER_QUEUE_CLASS = "scrapy.squeues.DownloaderAwarePriorityQueue"  # 2.14+ 默认
DUPEFILTER_CLASS = "scrapy.dupefilters.RFPDupeFilter"                  # 默认去重器
JOBDIR = "jobdata"                       # 断点续爬目录（生产按实例分开）

# ── 下载与中间件（05 章） ──
DOWNLOAD_TIMEOUT = 15                    # 单请求超时
RETRY_ENABLED = True                     # 失败重试
RETRY_TIMES = 2
DOWNLOADER_MIDDLEWARES = {...}           # 注册下载中间件
SPIDER_MIDDLEWARES = {...}               # 注册 Spider 中间件

# ── 管道（04 章） ──
ITEM_PIPELINES = {...}                   # 注册 Item Pipeline，数值=执行顺序

# ── 日志与统计（08 章） ──
LOG_LEVEL = "INFO"
LOG_FILE = "crawl.log"
STATS_DUMP = True                        # 结束时打印统计
```

> ⚠️ 2.14 起官方将**逐步弃用 Spider 类属性配置**（`name = "x"`、`allowed_domains`、`custom_settings` 之外的下载类属性），统一迁移到 **`custom_settings`** 或 settings.py——旧代码里 `handle_httpstatus_list = [...]` 这类类属性写法，新项目一律放进 `custom_settings`。

## 7. 核心要点

| 序号 | 要点 |
|:---:|------|
| 1 | Scrapy 2.16.0（2026-05-19）为最新稳定版：Python 3.14 / Twisted 26.4+；最低 Python 3.10 |
| 2 | 五大组件：Engine 调度、Scheduler 排队去重、Downloader 下载、Spider 解析产出、Pipeline 处理数据 |
| 3 | 八步数据流一句话：出队 → 下载 → 进 Spider → 解析出 Request/Item → Request 回队列、Item 进管道 |
| 4 | yield 是框架契约：yield Request 产生新任务，yield Item 产出数据 |
| 5 | 默认去重 = Request 指纹（SHA1），动态参数 URL 需自清洗防绕过 |
| 6 | 2.14+ 异步现代化：AsyncCrawlerProcess 系列 API、默认 DownloaderAwarePriorityQueue、async 回调 |
| 7 | 2.15 实验性 httpx 下载器 + 无 reactor 运行；2.16 Python 3.14 官方支持 |
| 8 | 类属性配置逐步迁移到 custom_settings（2.14 弃用政策） |

---

**下一模块**：[02-项目结构与 Spider 开发](02-项目结构与Spider开发.md) / **返回总览**：[00-阶段6爬虫框架总览](00-阶段6爬虫框架总览.md)

## 参考来源

- [Scrapy Architecture 官方文档](https://docs.scrapy.org/en/latest/topics/architecture.html)
- [Scrapy Release Notes](https://docs.scrapy.org/en/latest/news.html)（2.14-2.16 变更详情）
- [Zyte Blog: Scrapy in 2026 — Modern Async Crawling](https://www.zyte.com/blog/scrapy-in-2026-modern-async-crawling/)
- [Scrapy Scheduler 队列与优先级官方文档](https://docs.scrapy.org/en/latest/topics/scheduler.html)
- [主体系 04-Scrapy框架实战.md](../04-Scrapy框架实战.md)（架构速查版）
