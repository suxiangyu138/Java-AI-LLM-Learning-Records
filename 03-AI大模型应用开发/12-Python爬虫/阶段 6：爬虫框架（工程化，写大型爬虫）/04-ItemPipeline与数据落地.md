# 04 Item Pipeline 与数据落地
> 数据的最后一道工序：管道机制 + 校验清洗去重 + MySQL 入库 + 图片媒体下载

## 📚 目录
1. [Pipeline 机制与执行模型](#1-pipeline-机制与执行模型)
2. [数据落库：MySQL 连接生命周期](#2-数据落库mysql-连接生命周期)
3. [去重与增量：Pipeline 层的幂等设计](#3-去重与增量pipeline-层的幂等设计)
4. [媒体下载：ImagesPipeline 与文件管道](#4-媒体下载imagespipeline-与文件管道)
5. [管道组合与顺序设计](#5-管道组合与顺序设计)
6. [常见坑与调试](#6-常见坑与调试)
7. [核心要点](#7-核心要点)

---

## 1. Pipeline 机制与执行模型

### 1.1 基本契约

```python
# pipelines.py
from scrapy.exceptions import DropItem
from scrapy.utils.item import ItemAdapter


class MyPipeline:
    def open_spider(self, spider):        # 爬虫开启时（建连接/建表）
        pass

    def process_item(self, item, spider):  # 每个 Item 经过（核心）
        return item                        # 返回 item 传给下一级
        # 或 raise DropItem("原因")        # 丢弃，不再往下传

    def close_spider(self, spider):       # 爬虫关闭时（关连接/统计）
        pass
```

**三个铁律**：

| 规则 | 说明 |
|------|------|
| 返回值 = 交给下一级 | `return item` 必须写，漏写=静默丢弃 |
| 抛 `DropItem` = 终止本级及后续 | 丢弃时**日志记录原因**，便于排查 |
| 异常会中断整条爬取 | `process_item` 抛非 DropItem 异常 → 该 item 失败但**爬虫继续**（记 ERROR 日志） |

### 1.2 注册与顺序

```python
# settings.py
ITEM_PIPELINES = {
    "quotes.pipelines.ValidationPipeline": 100,   # 数字小 = 先执行
    "quotes.pipelines.CleanPipeline": 200,
    "quotes.pipelines.MysqlPipeline": 300,        # 入库放最后
    "quotes.pipelines.ImagesPipeline": 150,       # 注意 2.14 起官方 ImagesPipeline 有独立启用方式（§4）
}
```

> 🎯 **核心要点**：Pipeline 是**有序的流水线**——校验(100) → 清洗(200) → 入库(300)。顺序编号用 100/200/300 间隔，给将来插入新管道留空间；**任何"对 item 的横切处理"都优先考虑 Pipeline，而不是塞进 Spider**。

## 2. 数据落库：MySQL 连接生命周期

### 2.1 连接管理（open_spider / close_spider 成对）

```python
# pipelines.py —— MySQL 入库管道
import pymysql
from pymysql.cursors import DictCursor


class MysqlPipeline:
    def open_spider(self, spider):
        self.conn = pymysql.connect(
            host=spider.settings.get("MYSQL_HOST", "localhost"),
            port=spider.settings.getint("MYSQL_PORT", 3306),
            user=spider.settings.get("MYSQL_USER"),
            password=spider.settings.get("MYSQL_PASSWORD"),
            database=spider.settings.get("MYSQL_DB"),
            charset="utf8mb4",            # ⚠️ 必配，否则 emoji/生僻字报错
        )
        self.conn.autocommit(True)        # 生产按需关闭改批量提交（§2.3）

    def process_item(self, item, spider):
        adapter = ItemAdapter(item)
        sql = (
            "INSERT INTO quotes (text, author, tags, url, crawled_at) "
            "VALUES (%s, %s, %s, %s, NOW())"
        )
        with self.conn.cursor() as cur:
            cur.execute(sql, (
                adapter["text"], adapter["author"],
                ",".join(adapter.get("tags") or []), adapter["url"],
            ))
        return item

    def close_spider(self, spider):
        if hasattr(self, "conn"):
            self.conn.close()
```

### 2.2 配置外置（安全规范）

```python
# settings.py —— 敏感信息不进代码（08 章部署规范）
MYSQL_HOST = os.environ.get("CRAWLER_MYSQL_HOST", "127.0.0.1")
MYSQL_USER = os.environ.get("CRAWLER_MYSQL_USER", "crawler")
MYSQL_PASSWORD = os.environ.get("CRAWLER_MYSQL_PASSWORD", "")   # 生产必须环境变量
MYSQL_DB = os.environ.get("CRAWLER_MYSQL_DB", "crawler")
```

### 2.3 性能：单条 INSERT vs 批量

| 方式 | 1 万条耗时（量级） | 适用 |
|------|:---:|------|
| 单条 INSERT（autocommit） | 慢 10 倍+ | 小数据/调试 |
| executemany 分批（如 500 条/批） | 快 5-10 倍 | ⭐ 生产标准 |
| LOAD DATA INFILE | 最快 | 超大文件导入 |

```python
class BulkMysqlPipeline:
    BATCH = 500

    def open_spider(self, spider):
        self.buffer = []
        self.conn = ...  # 同上

    def process_item(self, item, spider):
        self.buffer.append((item["text"], item["url"]))
        if len(self.buffer) >= self.BATCH:
            self._flush()
        return item

    def close_spider(self, spider):
        self._flush()                 # ⚠️ 关闭时必 flush 剩余
        self.conn.close()

    def _flush(self):
        if not self.buffer:
            return
        with self.conn.cursor() as cur:
            cur.executemany(
                "INSERT INTO quotes (text, url) VALUES (%s, %s)",
                self.buffer,
            )
        self.buffer.clear()
```

> ⚠️ 批量管道三个坑：(1) `close_spider` 忘了 flush → 最后一批丢失；(2) 中途异常时 buffer 里数据丢 → 用 try/finally 兜底 flush；(3) 高并发下单连接串行执行有瓶颈 → 需要时用**连接池**（DBUtils，阶段 3 已讲）或并发分片。

## 3. 去重与增量：Pipeline 层的幂等设计

**两层去重各有分工**：

| 层 | 位置 | 管什么 | 手段 |
|----|------|--------|------|
| 请求层 | Scheduler 去重器 | 同一 URL 只下载一次 | RFPDupeFilter（01 章 §4） |
| 数据层 | Pipeline 入库 | 同一数据只入库一次（换 URL 同内容/断点重跑） | DB 唯一键 + `INSERT ... ON DUPLICATE KEY UPDATE` / `INSERT IGNORE` |

```python
class UpsertPipeline:
    def process_item(self, item, spider):
        adapter = ItemAdapter(item)
        # 表结构: UNIQUE KEY uk_url (url)
        sql = (
            "INSERT INTO quotes (text, author, url, updated_at) "
            "VALUES (%s, %s, %s, NOW()) "
            "ON DUPLICATE KEY UPDATE text=VALUES(text), author=VALUES(author), "
            "updated_at=NOW()"        # 重复则更新（增量更新语义）
        )
        # 只插入不改：用 INSERT IGNORE INTO ...（重复静默跳过）
        ...
```

**增量爬虫的数据层三件套**（配合 09 章综合项目）：

1. **唯一键**：URL 或内容指纹（MD5，阶段 3 已讲）做 UNIQUE KEY；
2. **INSERT IGNORE / ON DUPLICATE**：重复数据幂等，断点重跑不重复；
3. **crawled_at / updated_at**：时间戳字段支撑增量判断（"今天抓没抓过"）与数据新鲜度监控。

> 🎯 **核心要点**：**爬虫必须幂等**——同一 URL 跑十遍，数据库里还是一份数据。请求层去重防"重复下载"，数据层幂等防"重复入库"，两层都做才算工程级爬虫。

## 4. 媒体下载：ImagesPipeline 与文件管道

### 4.1 官方管道与 2.14 新能力

Scrapy 内置媒体管道（`scrapy.pipelines.images.ImagesPipeline` / `files.FilesPipeline`），自动完成：下载 → 校验 → 去重（指纹）→ 缩略图 → 落盘 → **image_urls 字段转为 images 结果字段**。

- **2.14.0 新特性**：ImagesPipeline 自动按 **EXIF 方向旋转**图片（`ImageOps.exif_transpose`），修正手机/相机照片方向问题。

```python
# ① settings.py
ITEM_PIPELINES = {"scrapy.pipelines.images.ImagesPipeline": 150}
IMAGES_STORE = "data/images"
IMAGES_THUMBS = {"small": (60, 60), "big": (270, 270)}   # 生成缩略图
IMAGES_MIN_WIDTH = 100                                    # 过滤太小的图
IMAGES_MIN_HEIGHT = 100

# ② Spider 里只需给出图片 URL 列表字段
class ProductSpider(scrapy.Spider):
    def parse(self, response):
        yield {
            "image_urls": response.css("div.gallery img::attr(src)").getall(),
            "name": response.css("h1::text").get(),
        }

# ③ 管道处理完自动得到本地路径（默认字段名 images）
# item["images"] = [{"path": "full/xxx.jpg", "url": "...", "checksum": "..."}, ...]
```

### 4.2 自定义媒体管道（改名/改存储）

```python
from scrapy.pipelines.images import ImagesPipeline


class CustomImagesPipeline(ImagesPipeline):
    def file_path(self, request, response=None, info=None, *, item=None):
        # 按业务 ID 组织目录，避免 hash 名难追踪
        return f"products/{item['sku']}/{request.url.split('/')[-1]}"

    def item_completed(self, results, item, info):
        ok, results = zip(*results)          # results = [(success, {path,url,...}), ...]
        item["images"] = [r for s, r in results if s]
        return item
```

> ⚠️ 媒体管道注意：`image_urls` 字段名**必须**与 settings 配置匹配（或用 `FILES_URLS_FIELD` 等自定义字段名）；媒体下载失败只影响单条 item（图片字段为空），**不会**中断爬取——所以下游要容忍 `images` 为空。

## 5. 管道组合与顺序设计

生产项目典型管道编排（编号即顺序）：

```text
100 ValidationPipeline     必填/类型/格式校验（03 章 §6）
150 ImagesPipeline         图片下载（先于入库，item 里需要本地路径）
200 CleanPipeline          清洗：去重列表/标准化日期/trim
300 MysqlPipeline          UPSERT 入库（最后）
400（可选）LogPipeline     抽样记录样本数据，供人工质检
```

```python
ITEM_PIPELINES = {
    "quotes.pipelines.ValidationPipeline": 100,
    "scrapy.pipelines.images.ImagesPipeline": 150,
    "quotes.pipelines.CleanPipeline": 200,
    "quotes.pipelines.MysqlPipeline": 300,
}
```

> 💡 设计原则：**"丢弃优先、加工居中、落地最后"**——校验丢得越早，浪费越少；入库永远最后，保证进入数据库的一定是成品。

## 6. 常见坑与调试

| # | 坑 | 现象 | 解法 |
|:---:|-----|------|------|
| 1 | process_item 忘了 return | 数据全丢但无报错 | 骨架模板先写 `return item` |
| 2 | open_spider 建连接失败 | 爬虫立刻挂 | 连接参数放 settings 环境变量；启动前先 ping |
| 3 | 中文/emoji 报错 | `Incorrect string value` | 表 utf8mb4 + 连接 charset=utf8mb4 |
| 4 | 批量管道丢尾批 | 最后几百条缺失 | close_spider 必 flush（try/finally） |
| 5 | 重复入库 | 断点重跑后数据翻倍 | 唯一键 + INSERT IGNORE/ON DUPLICATE |
| 6 | DropItem 没日志 | 数据莫名少、无法定位 | DropItem 里写原因字符串 |
| 7 | 管道异常影响整次爬取 | 一个 item 异常爬虫报错 | 用 try/except 包住易错段，记 ERROR 不中断 |

> 💡 调试三板斧：(1) `scrapy crawl xxx -L DEBUG` 看 Pipeline 日志；(2) 只跑一页验证管道（`scrapy crawl xxx -a start_url=...` 或临时限制 start_urls）；(3) `item_scraped_count` / `item_dropped_count` 统计对比（08 章）。

## 7. 核心要点

| 序号 | 要点 |
|:---:|------|
| 1 | Pipeline 三铁律：return item / DropItem 终止 / 异常记日志不中断 |
| 2 | 顺序编号 100/200/300 留扩展位；校验最前、入库最后 |
| 3 | 连接在 open_spider 建、close_spider 关；敏感配置走环境变量 |
| 4 | 入库用 executemany 批量（500/批），关闭时必 flush |
| 5 | 幂等两件套：唯一键 + INSERT IGNORE / ON DUPLICATE；时间戳支撑增量 |
| 6 | ImagesPipeline 自动下载/校验/缩略图/去重；2.14 起自动 EXIF 旋转 |
| 7 | 媒体管道失败不中断爬取，下游要容忍空 images |

---

**下一模块**：[05-中间件与下载控制](05-中间件与下载控制.md) / **返回总览**：[00-阶段6爬虫框架总览](00-阶段6爬虫框架总览.md)

## 参考来源

- [Scrapy Item Pipeline 官方文档](https://docs.scrapy.org/en/latest/topics/item-pipeline.html)
- [Scrapy Media Pipeline 官方文档](https://docs.scrapy.org/en/latest/topics/media-pipeline.html)
- [Scrapy 2.14 Release Notes（ImagesPipeline EXIF 旋转）](https://docs.scrapy.org/en/latest/news.html)
- [阶段 3：数据存储（pymysql/去重/连接池前置知识）](../阶段%203：数据存储/00-阶段3数据存储总览.md)
