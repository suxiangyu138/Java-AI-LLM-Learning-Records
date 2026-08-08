# MySQL 写入与去重
> 增删改查、批量写入、三种去重策略（UNIQUE/INSERT IGNORE/ON DUPLICATE）——"数据进库"的完整姿势

## 📚 目录
1. [增删改查四件套](#1-增删改查四件套)
2. [批量写入：executemany](#2-批量写入executemany)
3. [去重策略一：UNIQUE + 先查后插](#3-去重策略一unique--先查后插)
4. [去重策略二：INSERT IGNORE](#4-去重策略二insert-ignore)
5. [去重策略三：ON DUPLICATE KEY（upsert）](#5-去重策略三on-duplicate-keyupsert)
6. [写入规范与性能](#6-写入规范与性能)

## 1. 增删改查四件套

```python
# ═══ 增（INSERT）═══
cur.execute(
    "INSERT INTO news (title, url, date) VALUES (%s, %s, %s)",
    ("标题一", "https://example.com/1", "2026-08-08"),
)
conn.commit()
print(cur.lastrowid)              # 新插入的自增 ID

# ═══ 查（SELECT）═══
cur.execute("SELECT * FROM news WHERE date >= %s ORDER BY publish_time DESC LIMIT 20", ("2026-08-01",))
rows = cur.fetchall()             # 列表[dict, ...]
row = cur.fetchone()              # 单条

# ═══ 改（UPDATE）═══
cur.execute(
    "UPDATE news SET status = %s WHERE url = %s",
    (1, "https://example.com/1"),
)
conn.commit()
print(cur.rowcount)               # 影响行数（0 = 没匹配到）

# ═══ 删（DELETE）═══
cur.execute("DELETE FROM news WHERE date < %s", ("2026-01-01",))
conn.commit()
```

| 操作 | 注意 |
|------|------|
| INSERT | `cur.lastrowid` 拿自增 ID |
| SELECT | `fetchall`/`fetchone`；大量数据用 `fetchmany(1000)` 分批 |
| UPDATE | 看 `rowcount` 确认真的更新了 |
| DELETE | 加 WHERE（**忘 WHERE = 全表清空**——先 SELECT 数一遍） |

> ⚠️ **DELETE 安全三连**：① 先 SELECT 确认范围；② 用事务（`BEGIN`→`DELETE`→验证→`COMMIT`）；③ 重要表禁用裸 DELETE（用 status 标记替代，见 [04-去重与增量策略](04-去重与增量策略.md)）。

## 2. 批量写入：executemany

```python
# ═══ 批量插入（爬虫一天的数据一次写入）═══
items = [
    {"title": f"新闻{i}", "url": f"https://example.com/{i}", "date": "2026-08-08"}
    for i in range(1000)
]

cur.executemany(
    "INSERT INTO news (title, url, date) VALUES (%s, %s, %s)",
    [(it["title"], it["url"], it["date"]) for it in items],
)
conn.commit()
# 1000 条一条 SQL 执行——比循环 1000 次快 10 倍+

# ═══ 分批批量（超大列表防内存/包过大）═══
BATCH = 500
for i in range(0, len(items), BATCH):
    batch = items[i:i + BATCH]
    cur.executemany("INSERT INTO news (title, url, date) VALUES (%s, %s, %s)",
                    [(it["title"], it["url"], it["date"]) for it in batch])
    conn.commit()
```

| 批量要点 | 说明 |
|---------|------|
| `executemany` | 一次执行多条（性能关键） |
| 分批 | 每批 500-1000 条（防单包过大） |
| 事务边界 | 每批 commit（失败只回滚该批） |
| 与去重配合 | 见 §3-5（批量也能去重） |

> 🎯 性能认知：**"循环单条插入是爬虫写库的头号性能浪费"**——`executemany` 一条 SQL 写 1000 条；配合去重策略（§5 的 upsert 批量版），爬取+入库全程无重复无浪费。

## 3. 去重策略一：UNIQUE + 先查后插

```sql
-- 前提：表有 UNIQUE 约束（01 §3 的 uk_url）
CREATE TABLE ... (
    url VARCHAR(1024) NOT NULL,
    UNIQUE KEY uk_url (url(255))
);
```

```python
# ═══ 先查后插（简单直观）═══
def insert_if_new(cur, title, url):
    cur.execute("SELECT id FROM news WHERE url = %s", (url,))
    if cur.fetchone():
        return False              # 已存在，跳过
    cur.execute("INSERT INTO news (title, url) VALUES (%s, %s)", (title, url))
    return True

# 缺点：查一次 + 插一次 = 两次 SQL（慢）；并发下仍有竞态
```

| 策略 | 原理 | 优点 | 缺点 |
|------|------|------|------|
| UNIQUE + 先查后插 | 查重后插入 | 直观 | 2 次 SQL；并发竞态 |
| **INSERT IGNORE** | 撞唯一键静默跳过 | 1 次 SQL；天然防并发 | 无反馈（跳过不报错） |
| **ON DUPLICATE KEY** | 撞唯一键则更新 | 幂等 upsert | 需处理"更新了什么" |

> 🎯 结论：**生产优先 INSERT IGNORE / ON DUPLICATE KEY**（一次 SQL + 并发安全）；"先查后插"留给低频/逻辑简单场景。

## 4. 去重策略二：INSERT IGNORE

```python
# ═══ INSERT IGNORE：唯一键冲突 → 静默跳过 ═══
cur.execute(
    "INSERT IGNORE INTO news (title, url, date) VALUES (%s, %s, %s)",
    ("重复标题", "https://example.com/1", "2026-08-08"),
)
conn.commit()
# rowcount == 0 → 该 url 已存在（被忽略）
# rowcount == 1 → 新插入成功

# ═══ 批量版（去重 + 高性能合一）═══
cur.executemany(
    "INSERT IGNORE INTO news (title, url, date) VALUES (%s, %s, %s)",
    [(it["title"], it["url"], it["date"]) for it in items],
)
conn.commit()
inserted = cur.rowcount          # 实际插入条数（被跳过的不算）
print(f"共 {len(items)} 条，新插入 {inserted} 条，跳过 {len(items)-inserted} 条")
```

| INSERT IGNORE 要点 | 说明 |
|-------------------|------|
| 触发条件 | 撞**任何**唯一键/主键 |
| 反馈 | `rowcount` = 实际插入数 |
| 批量 | executemany + IGNORE 完美配合 |
| 统计 | `len - rowcount` = 重复数（采集报告用） |

> 🎯 面试点：**"INSERT IGNORE 是爬虫入库去重的标准姿势"**——一条 SQL 完成"去重 + 插入 + 计数"，批量场景快且天然防并发重复。

## 5. 去重策略三：ON DUPLICATE KEY（upsert）

```python
# ═══ upsert：唯一键冲突 → 更新指定字段（幂等）═══
cur.execute(
    """INSERT INTO news (title, url, views, status)
       VALUES (%s, %s, %s, %s)
       ON DUPLICATE KEY UPDATE
           views = VALUES(views),        # 更新浏览量（新值覆盖）
           status = VALUES(status)""",
    ("新闻", "https://example.com/1", 1234, 1),
)
conn.commit()

# 场景：
#   增量更新（浏览量/价格变化 → 覆盖更新而非新增）
#   幂等重跑（同一批数据跑两次 → 结果一致）

# ═══ 批量 upsert ═══
cur.executemany(
    """INSERT INTO news (title, url, views) VALUES (%s, %s, %s)
       ON DUPLICATE KEY UPDATE views = VALUES(views)""",
    [(it["title"], it["url"], it["views"]) for it in items],
)
conn.commit()
```

| 策略 | 唯一键冲突时 | 适用 |
|------|-------------|------|
| INSERT IGNORE | 跳过 | 只增不改（新闻列表） |
| **ON DUPLICATE KEY** | 更新 | **增量更新（价格/浏览量/状态）** |

> 🎯 选型：**"列表数据（只进不出）→ INSERT IGNORE；更新型数据（价格/状态会变）→ ON DUPLICATE KEY"**——与 [04-去重与增量策略](04-去重与增量策略.md) 的增量设计直接配合。

## 6. 写入规范与性能

```text
入库规范自查：
  □ 所有 SQL 参数化（%s 占位符，永不拼接）
  □ 批量用 executemany（循环单条是反模式）
  □ 去重用 INSERT IGNORE / ON DUPLICATE KEY（先查后插只限低频）
  □ 每批 commit（失败回滚粒度可控）
  □ 表有 UNIQUE 去重键 + 查询索引
  □ 大字段（正文）与查询字段分离（必要时分表）
  □ 写入失败记录日志（失败清单，见阶段 2 §6）
  □ 入库前数据已清洗（阶段 2 §5 的 clean_text/to_int）

性能基线：
  单机爬虫：executemany 批量 ≈ 5000-10000 条/秒（简单表）
  瓶颈通常在网络/解析，不在写库
```

> 🎯 本阶段入库目标：**"爬多快，存多快，不重复"**——批量 + 去重 + 参数化三件套齐了，数据库层就不再是爬虫瓶颈。下一步把"去重 + 增量"工程化（[04-去重与增量策略](04-去重与增量策略.md)）。

---

**下一模块**：[03-MongoDB存储](03-MongoDB存储.md) / **返回总览**：[00-阶段3数据存储总览](00-阶段3数据存储总览.md)
