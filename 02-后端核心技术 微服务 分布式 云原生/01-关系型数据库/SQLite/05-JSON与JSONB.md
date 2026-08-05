# JSON与JSONB
> json 函数族、JSONB 二进制格式（3.45+）、性能对比与"内部格式"警告：SQLite 的 JSON 处理能力全景。

---

## 📚 目录

1. [JSON 支持概览](#1-json-支持概览)
2. [json 函数族](#2-json-函数族)
3. [JSONB：二进制 JSON](#3-jsonb二进制-json)
4. [性能对比](#4-性能对比)
5. [JSON 与关系模型的结合](#5-json-与关系模型的结合)
6. [工程实践与陷阱](#6-工程实践与陷阱)

---

## 1. JSON 支持概览

### 1.1 版本演进

| 版本 | 里程碑 |
|------|--------|
| 3.9（2015） | json1 扩展（json 函数家族） |
| 3.38（2022） | JSON 操作符 -> 和 ->> |
| 3.45（2024） | **JSONB 二进制格式** |
| 3.51（2025-11） | jsonb_each/jsonb_tree（直接迭代 JSONB） |
| 3.53（2026-04） | json_array_insert + jsonb 对应版 |

```text
JSON 能力定位：
  SQLite 的 JSON 支持是"函数级"而非"类型级"
  —— 没有 JSON 列类型，JSON 存为 TEXT/BLOB
  （对比 PG：json/jsonb 原生类型）

但函数完备：查询/修改/聚合/路径提取全覆盖
```

### 1.2 存储方式

```text
两种存储：
  TEXT：文本 JSON（人类可读，默认）
  BLOB（JSONB）：二进制格式（3.45+，性能优）

声明示例：
  CREATE TABLE doc (id INT, data TEXT);     -- 文本 JSON
  CREATE TABLE doc (id INT, data BLOB);     -- JSONB 存储

写入：
  INSERT ... VALUES (1, json('{"a":1}'));     -- 文本
  INSERT ... VALUES (1, jsonb('{"a":1}'));    -- JSONB

注意：SQLite 无 json 列类型（3.53 仍无）
  → JSONB 只是"约定存 BLOB"，靠函数读写
```

---

## 2. json 函数族

### 2.1 核心函数

| 函数 | 作用 | 示例 |
|------|------|------|
| json(value) | 验证/规范化 JSON | json('{"a":1}') |
| json_extract(j, path) | 路径提取 | json_extract(d, '$.user.name') |
| json_set(j, path, v) | 设置值（可新增） | json_set(d, '$.a', 2) |
| json_insert(j, path, v) | 插入（已存在不改） | json_insert(d, '$.a', 2) |
| json_remove(j, path) | 删除路径 | json_remove(d, '$.a') |
| json_array / json_object | 构造 | json_object('a', 1) |
| json_type / json_valid | 类型/有效性 | json_valid(d) |
| json_each / json_tree | 表值函数（遍历） | SELECT * FROM json_each(d) |
| json_patch(j, patch) | RFC 7396 合并 | json_patch(d, '{"b":2}') |

### 2.2 路径语法与操作符

```text
路径语法（JSONPath 子集）：
  $             根
  $.a.b         属性链
  $[0]          数组下标
  $[0].name     组合
  $[#-1]        倒数第一（3.38+）

操作符（3.38+）：
  ->   返回 JSON 值（带引号/类型）
  ->>  返回文本值（去引号）

示例：
  SELECT data->>'user.name' FROM doc WHERE id = 1;
  -- 等价：json_extract(data, '$.user.name')
  SELECT data->0 FROM doc;  -- 数组首个元素
```

### 2.3 JSON 聚合

```sql
-- 分组聚合为数组/对象
SELECT user_id, json_group_array(amount) AS amounts
FROM orders GROUP BY user_id;

SELECT user_id, json_group_object(date, amount) AS by_date
FROM orders GROUP BY user_id;

-- 更新 JSON 字段（配合 json_set）
UPDATE doc SET data = json_set(data, '$.count',
    COALESCE(json_extract(data, '$.count'), 0) + 1)
WHERE id = 1;
```

---

## 3. JSONB：二进制 JSON

### 3.1 什么是 JSONB

```text
JSONB（3.45+）：JSON 的二进制内部表示
  结构：类型标记 + 长度 + 值（紧凑布局）
  特点：
    ① 更小的存储（去除空白/重复键名结构）
    ② 更快的解析（无需文本解析）
    ③ 支持原地修改（部分更新）

注意（重要警告）：
  SQLite 的 JSONB 是内部格式！
  与 PostgreSQL 的 JSONB 不兼容
  → 不要把 JSONB BLOB 导出到其他数据库/系统
  → 跨系统传输必须转文本（jsonb → json）
```

### 3.2 函数对应

```text
每个 json_* 函数都有 jsonb_* 对应版：
  jsonb('{"a":1}')      文本 → JSONB
  jsonb_extract(jb, $)  从 JSONB 提取
  jsonb_set / jsonb_insert / jsonb_remove
  jsonb_each / jsonb_tree（3.51+，直接迭代 JSONB）
  jsonb_group_array / jsonb_group_object

JSONB 与 TEXT 混用：
  json 函数接受 TEXT 或 BLOB（自动识别）
  jsonb 函数输出 BLOB

链式操作（性能关键）：
  文本 JSON 链式修改：每次都要 解析→修改→序列化
  JSONB 链式修改：原地/半原地（省解析）
```

### 3.3 JSONB 的存储选择

```text
何时用 JSONB：
  ① 频繁读/改 JSON（性能敏感）
  ② 大 JSON 文档（存储省 5-10%）
  ③ 嵌套查询多（解析成本高）

何时用文本：
  ① 需要人类可读/导出（外部系统）
  ② JSON 很少访问（存储不是瓶颈）
  ③ 兼容旧数据（已存 TEXT）

工程默认（2025-2026）：
  内部处理用 JSONB，对外交互转文本
```

---

## 4. 性能对比

### 4.1 官方与实测数据

| 维度 | JSON（TEXT） | JSONB（BLOB） | 提升 |
|------|:---:|:---:|:---:|
| 解析速度 | 基准 | ~3 倍快 | 3× |
| 存储大小 | 基准 | 小 5-10% | 5-10% |
| 链式修改 | 全量解析 | 局部更新 | 数倍 |
| 聚合 | 基准 | 更快 | 明显 |

```text
为什么快：
  文本 JSON：解析 → 建树 → 修改 → 序列化（每次）
  JSONB：二进制布局直接操作（类型标记定位）

为什么小：
  键名去重结构、无空白、紧凑整数编码

实测参考（官方基准/社区）：
  json_extract 大文档：JSONB 快 ~3 倍
  1MB JSON 文档加载：JSONB 明显占优
```

### 4.2 索引 JSON 字段

```sql
-- 表达式索引：加速 JSON 路径查询
CREATE INDEX idx_doc_user ON doc(json_extract(data, '$.user.id'));

-- 部分索引 + JSON 条件
CREATE INDEX idx_doc_active ON doc(json_extract(data, '$.status'))
  WHERE json_extract(data, '$.status') = 'ACTIVE';

-- 查询走索引：
SELECT * FROM doc WHERE json_extract(data, '$.user.id') = 42;
-- EXPLAIN 应显示 SEARCH USING INDEX idx_doc_user

注意：表达式必须与索引定义完全一致（否则不命中）
```

---

## 5. JSON 与关系模型的结合

### 5.1 何时用 JSON vs 关系列

| 场景 | 推荐 | 理由 |
|------|------|------|
| 固定字段 + 强查询 | 关系列 | 类型安全、索引友好 |
| 灵活 Schema（配置/扩展） | JSON | 无需迁移 |
| 嵌套结构（文档） | JSON | 关系化成本高 |
| 混合 | 核心列 + JSON 扩展 | 两全其美 |

```text
混合模式（2025-2026 主流）：
  固定字段：独立列（索引/约束）
  可变字段：JSON 列（扩展点）

例：用户表
  id, name, email（关系列）
  extra（JSON：preferences、tags、扩展属性）

优势：核心查询走索引，扩展字段零迁移
```

### 5.2 查询 JSON 的最佳实践

```sql
-- 提取并过滤（配合表达式索引）
SELECT id, data->>'user.name' AS name
FROM doc
WHERE data->>'user.status' = 'active';

-- JSON 表值函数：把数组展开成行
SELECT doc_id, item.value
FROM doc, json_each(doc.data, '$.items') AS item
WHERE item.value->>'price' > 100;

-- 更新嵌套数组（json_set 路径）
UPDATE doc
SET data = json_set(data, '$.items[0].price', 199)
WHERE id = 1;
```

---

## 6. 工程实践与陷阱

### 6.1 常见陷阱

| 陷阱 | 说明 | 对策 |
|------|------|------|
| JSONB 跨库导出 | 内部格式不兼容 | 转文本再导出 |
| json_set 与 insert 混淆 | set 新增/覆盖，insert 只新增 | 按需选择 |
| 索引表达式不一致 | 查询与索引定义差一个空格 | 完全一致 |
| 无效 JSON 静默 | json() 校验失败返回 NULL | json_valid 检查 |
| 大 JSON 全量更新 | 每改一个字段全量重写 | JSONB + 局部路径 |
| 文本 JSON 链式操作慢 | 反复解析序列化 | 用 jsonb 系列 |

### 6.2 工程清单

```text
① 存储：内部 JSONB（性能），对外文本
② 索引：JSON 路径表达式索引（查询热点）
③ 校验：写入前 json_valid（或应用层校验）
④ 迁移：文本 → JSONB（SELECT jsonb(data) 重建列）
⑤ 跨系统：导出前转 json()（文本）
⑥ 监控：大 JSON 文档的查询延迟

AI 场景（07 模块呼应）：
  JSONB 存 Agent 的记忆/工具参数/上下文
  → 结构灵活 + 查询快（json_extract 索引）
```

> 🎯 **核心要点**：SQLite 的 JSON = 函数级能力 + JSONB 性能选项——没有原生类型，但有完整函数与二进制格式。JSONB（3.45+）是性能关键：快 3 倍、小 5-10%、支持链式操作；但它"仅限 SQLite 内部"，跨系统必须转文本。工程模式：**混合建模（关系列 + JSON 扩展列）+ 表达式索引 + JSONB 存储**。

---

## 7. 小结

| 问题 | 答案 |
|------|------|
| JSON 怎么存？ | TEXT 或 BLOB（JSONB），无原生类型 |
| 核心函数？ | json_extract/set/remove、json_each/tree、-> 操作符 |
| JSONB 是什么？ | 二进制 JSON（3.45+），快 3×、小 5-10% |
| JSONB 兼容性？ | 仅 SQLite 内部格式（勿跨库导出） |
| 怎么索引 JSON？ | 表达式索引（json_extract 路径） |
| 何时用 JSON？ | 灵活 Schema/嵌套文档/扩展属性 |

**下一模块**：[06-FTS5全文检索](06-FTS5全文检索.md)　**返回总览**：[00-SQLite知识体系总览](00-SQLite知识体系总览.md)
