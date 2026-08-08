# MySQL 连接与表设计
> pymysql 连接、表结构与字段类型、连接管理——"爬虫数据的家"怎么布置

## 📚 目录
1. [pymysql 定位与安装](#1-pymysql-定位与安装)
2. [连接与基础操作](#2-连接与基础操作)
3. [爬虫场景的表设计](#3-爬虫场景的表设计)
4. [字段类型选型](#4-字段类型选型)
5. [连接管理：上下文与池](#5-连接管理上下文与池)

## 1. pymysql 定位与安装

| 事实 | 说明 |
|------|------|
| 定位 | Python 连接 MySQL 的纯 Python 驱动 |
| 安装 | `pip install pymysql`（或 uv pip install） |
| 替代 | mysql-connector-python（官方）、SQLAlchemy（ORM，后续） |
| 本阶段 | 手写 SQL 理解原理（ORM 思路见 [Spring Data 体系](../../../../02-后端核心技术%20微服务%20分布式%20云原生/06-Spring全家桶/Spring组件汇总/Spring%20Data%20系列【数据访问层】/Spring%20Data%20MongoDB/00-Spring%20Data%20MongoDB组件总览.md)，Java 侧另讲） |

> 🎯 学习定位：**先手写 SQL 懂原理，再上 ORM**——爬虫脚本用 pymysql 完全够（轻量无依赖）；大规模工程再用 SQLAlchemy（ORM 思路见 [Spring Data 体系](../../../../02-后端核心技术%20微服务%20分布式%20云原生/06-Spring全家桶/Spring组件汇总/Spring%20Data%20系列【数据访问层】/Spring%20Data%20MongoDB/00-Spring%20Data%20MongoDB组件总览.md)）。

## 2. 连接与基础操作

```python
import pymysql

# ═══ 连接 ═══
conn = pymysql.connect(
    host="localhost",          # 或 Docker 容器 IP
    port=3306,
    user="root",
    password="yourpassword",
    database="crawler_db",
    charset="utf8mb4",         # ⚠️ 必须 utf8mb4（支持中文+emoji）
    cursorclass=pymysql.cursors.DictCursor,   # 返回字典（推荐）
)
cur = conn.cursor()

# ═══ 建库建表 ═══
cur.execute("CREATE DATABASE IF NOT EXISTS crawler_db DEFAULT CHARSET utf8mb4")
conn.commit()

# ═══ 基础写入与查询 ═══
cur.execute(
    "INSERT INTO news (title, url, date) VALUES (%s, %s, %s)",
    ("标题", "https://example.com/1", "2026-08-08"),
)
conn.commit()

cur.execute("SELECT * FROM news WHERE title = %s", ("标题",))
row = cur.fetchone()           # 单条（字典）
rows = cur.fetchall()          # 全部（列表）

conn.close()                   # ⚠️ 用完必关
```

| 连接要点 | 说明 |
|---------|------|
| `charset="utf8mb4"` | **必须**（utf8 不支持 emoji/生僻字） |
| `DictCursor` | 结果按列名字典返回（比元组好用） |
| `%s` 占位符 | **防注入铁律**（阶段 2 §7 已埋点） |
| commit | 写操作必须提交（DML 自动开启事务） |
| close | 用完关闭（见 §5 上下文管理） |

> ⚠️ 新手三坑：**① 忘 charset=utf8mb4**（中文存进去乱码/报错）；**② 忘 commit**（数据"写进去了"但回滚了）；**③ 忘 close**（连接泄漏——长时间爬虫必炸）。

## 3. 爬虫场景的表设计

```sql
-- ═══ 标准爬虫表设计（直接可抄）═══
CREATE TABLE IF NOT EXISTS news (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,   -- 自增主键
    title       VARCHAR(512) NOT NULL,               -- 标题（长度留够）
    url         VARCHAR(1024) NOT NULL,              -- 链接
    content     MEDIUMTEXT,                          -- 正文（大字段）
    author      VARCHAR(128) DEFAULT '',             -- 作者
    publish_time DATETIME,                           -- 发布时间
    crawl_time  DATETIME DEFAULT CURRENT_TIMESTAMP,  -- 抓取时间（审计）
    status      TINYINT DEFAULT 0,                   -- 状态（0待处理/1已处理）
    UNIQUE KEY uk_url (url(255)),                    -- ⚠️ 去重约束（url 前缀索引）
    KEY idx_time (publish_time)                      -- 查询索引
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

| 设计要点 | 说明 |
|---------|------|
| `UNIQUE KEY uk_url` | **数据库级去重**（url 重复插入失败） |
| url 前缀索引 (255) | url 太长不能全字段唯一（前缀即可） |
| 冗余抓取时间 | 审计"什么时候抓的"（数据治理必需） |
| status 字段 | 抓取状态机（待处理/已处理——增量策略用，见 04） |
| InnoDB + utf8mb4 | 生产标配（事务 + 全字符集） |

> 🎯 爬虫表设计核心：**"业务字段 + 去重键 + 状态字段 + 时间审计"四件套**——去重键让数据库管"不重复"，状态字段让增量策略有据可依（04 章）。

## 4. 字段类型选型

| 数据 | 推荐类型 | 说明 |
|------|---------|------|
| ID/数字 | BIGINT / INT | 别用 VARCHAR 存数字 |
| 标题/短文本 | VARCHAR(255-512) | 长度按业务留余量 |
| 正文/长文本 | MEDIUMTEXT / LONGTEXT | 4MB/4GB（爬虫正文够用） |
| URL | VARCHAR(1024) | 长 URL 用前缀索引 |
| 日期时间 | DATETIME | 存标准格式（清洗成 `%Y-%m-%d`） |
| 状态/枚举 | TINYINT | 0/1/2 数字状态（别存字符串） |
| 价格 | DECIMAL(10,2) | ⚠️ 别用 FLOAT（精度） |
| 标签列表 | JSON 类型 | MySQL 5.7+ 原生 JSON |

> ⚠️ 类型选型三原则：**① 数字用数字类型**（VARCHAR 存数字 = 排序/计算全废）；**② 金额用 DECIMAL**（FLOAT 精度丢失是财务事故）；**③ 枚举用数字**（TINYINT 而非字符串）。

## 5. 连接管理：上下文与池

```python
# ═══ 方式一：上下文管理器（推荐，自动关连接）═══
import pymysql
from contextlib import closing

with closing(pymysql.connect(
        host="localhost", user="root", password="xxx",
        database="crawler_db", charset="utf8mb4",
        cursorclass=pymysql.cursors.DictCursor)) as conn:
    with conn.cursor() as cur:
        cur.execute("SELECT COUNT(*) AS c FROM news")
        print(cur.fetchone())
    # conn 自动关闭（with 块退出）

# ═══ 方式二：DBUtils 连接池（长时间/多线程爬虫）═══
# pip install DBUtils
from dbutils.pooled_db import PooledDB

pool = PooledDB(
    creator=pymysql,
    maxconnections=10,             # 池大小
    host="localhost", user="root", password="xxx",
    database="crawler_db", charset="utf8mb4",
    cursorclass=pymysql.cursors.DictCursor,
)

conn = pool.connection()           # 从池取（用完归还，不真关）
try:
    with conn.cursor() as cur:
        cur.execute("SELECT 1")
finally:
    conn.close()                   # 归还连接池
```

| 连接方案 | 场景 |
|---------|------|
| 单次脚本 | 直接连接 + with 关闭 |
| **长时间爬虫** | **连接池**（DBUtils）——防"连一次断一次" |
| 多线程爬虫 | 连接池（每线程取连接，用完归还） |
| 大型工程 | SQLAlchemy 连接池（后续） |

> 🎯 连接管理认知：**"数据库连接是稀缺资源"**——每次爬取都新建连接 = 握手开销 + 连接数爆表；连接池复用是生产标配。单机爬虫 DBUtils 池 10 连接足够（对应[数据库连接池体系](../../../../02-后端核心技术%20微服务%20分布式%20云原生/01-关系型数据库/数据库连接池/00-数据库连接池知识体系总览.md)）。

---

**下一模块**：[02-MySQL写入与去重](02-MySQL写入与去重.md) / **返回总览**：[00-阶段3数据存储总览](00-阶段3数据存储总览.md)
