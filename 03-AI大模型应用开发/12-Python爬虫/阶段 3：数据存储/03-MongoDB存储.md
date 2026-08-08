# MongoDB 存储
> pymongo 连接、文档模型、写入与查询、去重索引——"结构多变的数据"用文档库存

## 📚 目录
1. [MongoDB 与 MySQL 的选型](#1-mongodb-与-mysql-的选型)
2. [pymongo 连接与基础操作](#2-pymongo-连接与基础操作)
3. [文档模型：灵活嵌套](#3-文档模型灵活嵌套)
4. [写入与去重](#4-写入与去重)
5. [查询与索引](#5-查询与索引)
6. [爬虫场景实践](#6-爬虫场景实践)

## 1. MongoDB 与 MySQL 的选型

| 维度 | MySQL | MongoDB |
|------|:---:|:---:|
| 模型 | 表 + 行（固定结构） | **文档（JSON 灵活嵌套）** |
| 结构变化 | 改表（ALTER） | **无需改结构**（字段随意） |
| 嵌套数据 | 多表 JOIN | 一个文档直接嵌套 |
| 查询 | SQL | 类 JSON 查询 |
| 爬虫适用 | 结构化表格数据 | **结构多变/嵌套/日志型** |

> 🎯 选型直觉：**"爬到的数据长得像表格 → MySQL；长得像 JSON/嵌套 → MongoDB"**——文章详情（嵌套标签/作者/评论）就比扁平新闻列表更适合 MongoDB。

## 2. pymongo 连接与基础操作

```python
# pip install pymongo
from pymongo import MongoClient

# ═══ 连接（MongoDB 8.x）═══
client = MongoClient("mongodb://localhost:27017/")
db = client["crawler_db"]              # 数据库（不存在自动创建）
col = db["articles"]                   # 集合（类似表）

# ═══ 插入 ═══
article = {
    "title": "标题一",
    "url": "https://example.com/1",
    "content": "正文...",
    "tags": ["AI", "爬虫"],            # 嵌套数组直接存
    "author": {"name": "张三", "id": 42},   # 嵌套对象直接存
    "crawl_time": "2026-08-08 10:00:00",
}
result = col.insert_one(article)       # 单条
print(result.inserted_id)              # 自动 _id

# ═══ 批量插入 ═══
col.insert_many([article1, article2, article3])

# ═══ 查询 ═══
doc = col.find_one({"title": "标题一"})          # 单条（字典）
docs = list(col.find({"tags": "AI"}))           # 列表（转 list！）
count = col.count_documents({"author.id": 42})  # 计数
```

| pymongo 要点 | 说明 |
|-------------|------|
| 惰性连接 | 首次操作才真连（报错在操作时） |
| 集合自动创建 | 插入时自动建（无需 CREATE TABLE） |
| `_id` | 自动生成唯一 ID |
| `find` 返回游标 | **必须 list()/循环**（不是直接列表） |
| 无事务（默认） | 单文档操作原子；多文档用事务（4.0+） |

> ⚠️ 与 MySQL 习惯差异：**① 不需要"建表"**（插入即建集合）；**② 字段随意**（每篇文档可以不同结构）；**③ find 返回游标**（忘 list() 是常见 bug）。

## 3. 文档模型：灵活嵌套

```python
# ═══ MySQL 要多表 JOIN，MongoDB 一个文档搞定 ═══
article = {
    "title": "MongoDB 实战",
    "content": "...",
    "comments": [                          # 评论数组直接嵌套
        {"user": "张三", "text": "好文", "likes": 5},
        {"user": "李四", "text": "学到了", "likes": 2},
    ],
    "stats": {"views": 1234, "likes": 89}, # 统计对象嵌套
    "related": ["文章A", "文章B"],
}

# ═══ 嵌套查询（点号语法）═══
col.find_one({"comments.user": "张三"})        # 嵌套字段查询
col.find_one({"stats.views": {"$gt": 1000}})   # 大于
col.find({"tags": {"$in": ["AI", "Python"]}})  # 包含任一
```

| 嵌套场景 | 文档模型优势 |
|---------|-------------|
| 评论/标签/相关文章 | 一个文档直接存（MySQL 要 3 张表） |
| 结构不稳定的数据 | 字段增减无需改表 |
| 爬虫原始数据 | 直接把解析结果 dict 存进去（零转换） |

> 🎯 爬虫最优场景：**"解析结果 dict 直接 insert"**——阶段 2 的提取函数返回的就是 dict，MongoDB 零转换入库（MySQL 还要列字段对齐）。结构多变/嵌套多的数据这是最优解。

## 4. 写入与去重

```python
# ═══ 唯一索引（对应 MySQL 的 UNIQUE）═══
col.create_index("url", unique=True)     # url 唯一（去重）

# ═══ 幂等写入（撞唯一索引 → 更新）═══
col.update_one(
    {"url": "https://example.com/1"},           # 过滤条件
    {"$set": {"title": "新标题", "views": 999}},  # 更新内容
    upsert=True,                                  # 不存在则插入
)
# upsert=True = MySQL 的 ON DUPLICATE KEY（一次幂等写入）

# ═══ 批量 upsert（去重 + 更新 + 插入）═══
for article in articles:
    col.update_one(
        {"url": article["url"]},
        {"$set": article},                 # 整篇覆盖更新
        upsert=True,
    )
```

| MongoDB 去重 | 对应 MySQL | 说明 |
|-------------|-----------|------|
| `create_index("url", unique=True)` | UNIQUE KEY | 唯一索引 |
| `update_one(..., upsert=True)` | ON DUPLICATE KEY | 幂等写入 |
| 查重后插 | INSERT IGNORE 替代 | 低频场景 |

> 🎯 结论：**`unique 索引 + upsert` 是 MongoDB 版去重标准**——与 MySQL 的"UNIQUE + ON DUPLICATE"完全同构，掌握 MySQL 那套再来就是换 API。

## 5. 查询与索引

```python
# ═══ 常用查询操作符 ═══
col.find({"views": {"$gt": 1000}})               # 大于
col.find({"date": {"$gte": "2026-08-01"}})       # 大于等于
col.find({"title": {"$regex": "爬虫"}})          # 正则（慢，慎用）
col.find({"status": {"$in": [0, 2]}})            # 包含

# ═══ 排序与分页 ═══
docs = list(col.find({"status": 0})
            .sort("crawl_time", -1)              # -1 倒序
            .limit(100))                          # 分页 1
docs2 = list(col.find({"status": 0})
             .sort("crawl_time", -1)
             .skip(100).limit(100))               # 分页 2（大数据用 _id 游标）

# ═══ 索引设计 ═══
col.create_index([("status", 1), ("crawl_time", -1)])   # 复合索引（状态+时间）
# 索引与查询匹配 → 速度百倍提升；不匹配 → 全集合扫描
```

| 索引要点 | 说明 |
|---------|------|
| 唯一索引 | url（去重） |
| 复合索引 | 高频查询条件组合（status + time） |
| 正则 `$regex` | 慢（不走索引）——能用前缀索引不用正则 |
| 分页 | 大数据用 `_id` 游标（skip 越翻越慢） |

> ⚠️ 索引是查询性能的命根子：**"建索引 = 空间换时间，索引与查询不匹配 = 白建"**——按实际查询条件建（`find` 的过滤字段组合就是索引候选）。深挖见 [MongoDB 体系](../../../../02-后端核心技术%20微服务%20分布式%20云原生/02-非关系型数据库/MongoDB/01-MongoDB核心概念与安装配置.md)。

## 6. 爬虫场景实践

```python
# ═══ 场景一：详情页嵌套数据（最适合 MongoDB）═══
def save_article(article: dict):
    """详情数据（含嵌套评论/标签）直接入库"""
    col.update_one(
        {"url": article["url"]},
        {"$set": article},
        upsert=True,
    )

# ═══ 场景二：原始 HTML 存档（审计/重解析）═══
col_html = db["html_archive"]
col_html.insert_one({
    "url": url,
    "html": html_text,              # 原始 HTML 全文（后续可重新解析）
    "crawl_time": time.strftime("%Y-%m-%d %H:%M:%S"),
})

# ═══ 场景三：抓取任务队列（状态机）═══
col_task = db["crawl_tasks"]
col_task.insert_one({"url": url, "status": 0})       # 0 待抓
col_task.update_one({"url": url}, {"$set": {"status": 1}})   # 1 已抓
pending = list(col_task.find({"status": 0}).limit(100))
```

| 实践场景 | 用 MongoDB 的原因 |
|---------|------------------|
| 嵌套详情 | 评论/标签直接嵌套（免 JOIN） |
| HTML 存档 | 大文本 + 结构随意 |
| 任务队列 | 文档即任务（状态字段驱动） |

> 🎯 本阶段 MongoDB 定位：**"文档型数据的天然归宿"**——嵌套结构、原始存档、任务状态这类"JSON 形态"数据，MongoDB 零转换直存。MySQL 与 MongoDB 双持，选型看数据形态（[06-存储选型与生产规范](06-存储选型与生产规范.md)）。

---

**下一模块**：[04-去重与增量策略](04-去重与增量策略.md) / **返回总览**：[00-阶段3数据存储总览](00-阶段3数据存储总览.md)
