# 02 - PostgreSQL 索引原理与设计

> 定位：PG 最独特的优势——六种索引类型（B-tree/GIN/GiST/BRIN/Hash/向量）、部分索引、PG 18 skip scan、索引设计原则

## 📚 目录

1. [六种索引类型](#1-六种索引类型)
2. [B-tree 与 Skip Scan（PG 18）](#2-b-tree-与-skip-scanpg-18)
3. [GIN 倒排索引](#3-gin-倒排索引)
4. [GiST 与 BRIN](#4-gist-与-brin)
5. [部分索引与表达式索引](#5-部分索引与表达式索引)
6. [索引设计进阶](#6-索引设计进阶)

---

## 1. 六种索引类型

| 索引 | 适用 | MySQL 对应 |
|------|------|:---:|
| **B-tree** | 通用（排序/范围/=） | ✅ 有 |
| **GIN** | 全文/数组/JSONB | ❌ 无 |
| **GiST** | 几何/全文自定义 | ❌ 无 |
| **BRIN** | 超大表的顺序列 | ❌ 无 |
| Hash | 等值 | ✅ 有 |
| **向量** | pgvector ANN | ❌ 无 |

```
⚠️ 面试必答：
"PG 有六种索引——B-tree 通用、
 GIN 倒排（全文/数组）、GiST 通用搜索、
 BRIN 块级（超大表）、Hash 等值、向量 ANN；
 这是 PG 相对 MySQL 最大的索引优势。"
```

---

## 2. B-tree 与 Skip Scan（PG 18）

### 2.1 B-tree 基础

```sql
-- 普通 B-tree（与 MySQL 类似，叶子链表 + 优化器更智能）
CREATE INDEX idx_name ON users(name);
CREATE INDEX idx_composite ON orders(user_id, status, created_at);

-- 支持排序（DESC NULLS LAST 等）
CREATE INDEX idx_salary_desc ON employees(salary DESC NULLS LAST);
```

### 2.2 B-tree 内部结构（源码级）

```
PG B-tree 物理结构（页 = 8KB）：
  Meta Page（第 0 页）→ 记录根页位置
    ↓
  Root Page（根）→ 指向内部页
    ↓
  Internal Page（内部页）→ 键 + 子页指针（高扇出，一页可存数百键）
    ↓
  Leaf Page（叶页）→ 排序的键 + TID（页号, 行偏移），叶页双向链表

关键点：
  ① 索引页与数据页同尺寸（8KB），节点高扇出 → 树高通常 3-4 层
  ② 叶子页双向链表 → 范围扫描无需回溯父节点
  ③ 索引条目 = (键, TID)——TID 指向堆表行（ctid）
  ④ 排序规则由 opclass 决定（如 int4_ops/text_ops）
```

```sql
-- 用 pageinspect 看索引页内部（调优/诊断神器）
CREATE EXTENSION pageinspect;
SELECT * FROM bt_page_stats('idx_name', 1);   -- 看第 1 页统计
SELECT * FROM bt_page_items('idx_name', 1);   -- 看页内条目
```

### 2.3 扫描方式对比（EXPLAIN 里的访问路径）

| 扫描方式 | 原理 | 何时出现 | 性能 |
|---------|------|---------|:---:|
| Seq Scan | 全表顺序扫 | 无索引/低选择性 | 慢（小表最快） |
| Index Scan | 走索引定位行（回表） | 高选择性 | ✅ 快 |
| Index Only Scan | 索引含所需列（不回表） | 覆盖索引 | ✅✅ 最快 |
| Bitmap Heap Scan | 索引找候选页再批量读 | 中等选择性 | 大范围最优 |

```sql
-- Index Only Scan 场景：索引包含查询所需全部列
CREATE INDEX idx_users_name ON users(name);
EXPLAIN SELECT name FROM users WHERE name = '张三';
-- 如果只查 name（索引列）→ Index Only Scan 无需回表

-- Bitmap 场景：过滤后命中 10% 行
EXPLAIN SELECT * FROM orders
WHERE status = 'PAID' AND amount > 1000;
```

> 💡 **EXPLAIN 三看**：看访问方式（Seq/Index/Bitmap）、看预估行数偏差（rows vs actual）、看排序/哈希（Sort/Hash Join 代价）。

### 2.4 Skip Scan（PG 18 新特性）

```sql
-- ⚠️ PG 18 Skip Scan：联合索引不依赖前导列也能用
CREATE INDEX idx_status_user ON orders(status, user_id);

-- PG 17 前：WHERE user_id=1 → 无法用 idx_status_user（status 不在 WHERE）
-- PG 18：WHERE user_id=1 → 可用！优化器跳过 status 列扫描

-- AWS 实测：21× 性能提升（73ms → 3.4ms）
-- 前提：前导列基数低（如 status 只有几个值）
```

> 🎯 **要点**：PG 18 Skip Scan 是**最令 DBA 兴奋的特性**——不需要为每个查询建新索引。与 MySQL 的索引下推（ICP）互补但不同：ICP 是过滤优化、Skip Scan 是访问路径优化。

---

## 3. GIN 倒排索引

### 3.1 适用场景

```sql
-- ① 全文搜索（tsvector）
CREATE INDEX idx_fts ON articles USING GIN(to_tsvector('english', content));

-- PG 支持多种语言分词器（中文需 zhparser 扩展或 pg_trgm）

-- ② 数组包含（@> 操作符）
CREATE INDEX idx_tags ON products USING GIN(tags);

-- 查询：WHERE tags @> ARRAY['electronics', 'sale']

-- ③ JSONB 键值检索
CREATE INDEX idx_json ON events USING GIN(data jsonb_path_ops);
```

### 3.2 GIN 内部原理（Pending List）

```
GIN 写入优化（fastupdate）：
  新条目先进 Pending List（内存 + 未合并段，不立即更新主索引）
  累积到 gin_pending_list_limit（默认 4MB）→ 批量合并进主索引树

好处：写入快（避免高频随机写）
代价：查询时要先扫 Pending List 再查主索引（慢一点）

⚠️ 注意事项：
  ① gin_pending_list_limit 可调（写多调大、读多调小）
  ② 维护后建议：VACUUM 会自动触发合并
  ③ 崩溃恢复时 Pending List 会重建（无需担心数据丢失）
```

```sql
-- 查看 GIN 索引状态
SELECT * FROM gin_metapage_info('idx_tags');
```

### 3.3 GIN vs B-tree

| 维度 | B-tree | GIN |
|------|:---:|:---:|
| 场景 | 单值 | 多值（数组/JSON/全文） |
| 原理 | 排序树 | 倒排索引（值 → 文档列表） |
| 写入 | 快 | 慢（需维护倒排表） |
| 查询 | 等值/范围 | 包含/匹配 |

```
⚠️ 面试必答：
"GIN = 倒排索引——数组包含、JSON 检索、
 全文搜索的标配；
 写入慢但查询快（适合读多写少场景）。"
```

---

## 4. GiST 与 BRIN

### 4.1 GiST

```sql
-- GiST = 通用搜索树（可自定义的平衡树，R-tree 思想）
-- ① 几何索引（PostGIS 底层）
CREATE INDEX idx_geom ON locations USING GiST(geom);

-- ② 全文自定义距离
-- ③ 范围类型（Range）高效重叠查询
-- ④ 时态约束（PG 18 EXCLUDE USING gist）
```

```
GiST 原理（源码级）：
  内部节点存「边界框」（bounding box）而非精确值
  查询时先判断边界框相交 → 剪枝（不相关的子树直接跳过）
  → 空间/范围查询从 O(n) 降到 O(log n)

  与 B-tree 区别：
    B-tree = 精确值有序 → 等值/范围
    GiST  = 近似边界框 → 相交/包含/最近邻
```

### 4.2 BRIN

```sql
-- BRIN = 块范围索引：每块存 min/max
-- ⚠️ 超大表（>10 亿行）且数据物理有序时效率极高
CREATE INDEX idx_brin ON events USING BRIN(created_at)
  WITH (pages_per_range = 32);    -- 每 32 页一个统计块

-- 索引大小：B-tree(100GB) → BRIN(<1MB)
-- 查询走 BRIN 先定位候选块，再块内全扫

-- 适用：流水表（日志/事件/时序）且顺序写入
```

```
BRIN 关键参数：
  pages_per_range = 128（默认）→ 每 128 页（1MB）记一个 min/max
  值越小 → 索引越大、过滤越精确；值越大 → 更省空间、过滤越粗

⚠️ 失效场景：数据乱序写入（随机时间戳）→ 每个范围的 min/max
  迅速覆盖全表范围 → BRIN 失去过滤能力（全表扫）
  对策：按时间分区 或 定期 CLUSTER 排序，保证物理有序
```

| 场景 | B-tree | BRIN |
|------|:---:|:---:|
| 索引大小 | 大（GB） | **极小（MB）** |
| 查询精度 | 准 | 粗（需二次过滤） |
| 写入开销 | 有 | **极小** |
| 适用 | 通用 | 超大有序表 |

> 🎯 **要点**：BRIN 是 PG 独有的"超轻量索引"——用 < 1MB 索引 1TB 级别的流水表。MySQL 无此能力（分区表 + 前缀索引近似但不同）。

---

## 5. 部分索引与表达式索引

### 5.1 部分索引

```sql
-- 只索引满足条件的行（减少索引大小）
CREATE INDEX idx_active_users ON users(email)
    WHERE status = 'active';          -- ⚠️ 只索引活跃用户

-- 场景：90% 查询只查活跃用户 → 索引体积减半
```

### 5.2 表达式索引

```sql
-- 索引计算结果而不只是列值
CREATE INDEX idx_lower_email ON users(lower(email));

-- 查询：WHERE lower(email) = 'a@b.com'  → 走索引
-- ⚠️ MySQL 8.0+ 函数索引是类似能力
```

### 5.3 索引设计六原则

```
✅ 等值查询列建 B-tree（默认）
✅ 数组/JSON/全文用 GIN
✅ 超大顺序表用 BRIN
✅ 热数据子集用部分索引
✅ 函数/表达式查询用表达式索引
✅ 联合索引高区分度在前（与 MySQL 一致）
⚠️ 不要过度索引（写入变慢、空间浪费）
```

---

## 6. 索引设计进阶

### 6.1 覆盖索引（INCLUDE）与索引命中检查

```sql
-- ① INCLUDE：附加非键列（仅存值不参与排序，减少回表）
CREATE INDEX idx_users_email ON users(email) INCLUDE (name, status);
-- 查询 WHERE email='x' SELECT name,status → Index Only Scan 免回表

-- ② 对比：普通组合索引 vs INCLUDE
--   组合索引 (email, name) → 列参与排序（占更多空间、对前导列顺序敏感）
--   INCLUDE (name)      → 仅存值（适合"只查不排"的列）

-- ③ 检查索引使用率（慢查询 + 未命中索引一目了然）
SELECT schemaname, relname, indexrelname,
       idx_scan, idx_tup_read, idx_tup_fetch
FROM pg_stat_user_indexes
ORDER BY idx_scan ASC LIMIT 10;    -- 从未被扫过的索引 = 候选删除
```

### 6.2 组合索引列顺序（与 MySQL 一致 + Skip Scan 补充）

```
组合索引 (a, b, c)：
  ✅ 能命中：WHERE a=… / WHERE a=… AND b=… / a,b,c 任意前缀
  ❌ 不能命中：WHERE b=… / WHERE c=…（PG 18 前）
  ✅ PG 18 Skip Scan：WHERE b=… 且 a 基数低时也能用！

顺序规则：
  ① 等值条件列在前（等值 > 范围）
  ② 区分度高的在前（一般经验，但结合①）
  ③ 范围列放最后（否则后续列无法用于等值定位）
```

### 6.3 索引失效与调优速查

| 场景 | 问题 | 对策 |
|------|------|------|
| `WHERE lower(name)='x'` | 函数包裹列 | 建表达式索引 `lower(name)` |
| 大表写入慢 | 索引过多 | 用 `pg_stat_user_indexes` 找冷索引删掉 |
| 索引膨胀 | 大量 UPDATE/DELETE | `REINDEX INDEX CONCURRENTLY` |
| `LIKE '%abc'` | 前导通配符 | 表达式索引 `reverse(name)` + `LIKE 'cba%'` |
| GIN 写入慢 | pending list 满 | 调大 `gin_pending_list_limit` |

> 🎯 **要点**：覆盖索引（INCLUDE）是 PG 12+ 的回表终结者；配合 `pg_stat_user_indexes` 定期清冷索引，是索引治理的标准动作。

---

> 🎯 **核心要点**：PG 索引 = **六类型**（B-tree/GIN/GiST/BRIN/Hash/向量）+ **PG 18 Skip Scan**（不依赖前导列）+ **部分索引/表达式索引** + **INCLUDE 覆盖索引**。PG 索引多样性的核心价值——**不同场景用最匹配的索引类型**（全文 GIN、大表 BRIN、几何 GiST），底层是 8KB 页 + 高扇出 B-tree 结构。

---

**返回总览**：[00-PostgreSQL总览与核心概念](00-PostgreSQL总览与核心概念.md) | **上一篇**：[01-PostgreSQL SQL进阶与独有特性](01-PostgreSQLSQL进阶与独有特性.md) | **下一篇**：[03-PostgreSQL MVCC与并发控制](03-PostgreSQLMVCC与并发控制.md)
