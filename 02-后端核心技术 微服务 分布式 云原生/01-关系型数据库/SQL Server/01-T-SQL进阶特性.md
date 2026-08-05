# 01 - T-SQL 进阶特性

> 定位：SQL Server 独有的 T-SQL 能力——CTE/窗口函数/TOP-OFFSET/PIVOT-UNPIVOT/MERGE/临时表——企业开发高频 SQL 技能

## 📚 目录

1. [CTE 与窗口函数](#1-cte-与窗口函数)
2. [TOP 与 OFFSET-FETCH](#2-top-与-offset-fetch)
3. [PIVOT 与 UNPIVOT](#3-pivot-与-unpivot)
4. [MERGE 语句](#4-merge-语句)
5. [临时表与表变量](#5-临时表与表变量)
6. [SQL Server 2025 T-SQL 新特性](#6-sql-server-2025-t-sql-新特性)

---

## 1. CTE 与窗口函数

### 1.1 递归 CTE

```sql
-- 递归 CTE：组织架构/分类树
WITH deptTree AS (
    -- 锚点：根
    SELECT id, name, parent_id, 0 AS level
    FROM departments WHERE parent_id IS NULL
    UNION ALL
    -- 递归
    SELECT d.id, d.name, d.parent_id, t.level + 1
    FROM departments d
    JOIN deptTree t ON d.parent_id = t.id
)
SELECT * FROM deptTree ORDER BY level, id;
-- ⚠️ 最大递归深度默认 100（OPTION(MAXRECURSION n) 调整）
```

### 1.2 窗口函数

```sql
-- SQL Server 窗口函数（与 PG 类似）
SELECT name, dept_id, salary,
    ROW_NUMBER() OVER (PARTITION BY dept_id ORDER BY salary DESC) AS rn,
    RANK() OVER (PARTITION BY dept_id ORDER BY salary DESC) AS rk,
    LAG(salary) OVER (PARTITION BY dept_id ORDER BY salary) AS prev,
    SUM(salary) OVER (ORDER BY salary ROWS UNBOUNDED PRECEDING) AS cum
FROM employees;

-- ⚠️ SQL Server 特有：ROW_NUMBER() 是分页的经典写法
```

### 1.3 企业高频场景

```sql
-- ① 分页经典写法（ROW_NUMBER 嵌套，2012 前无 OFFSET）
SELECT * FROM (
    SELECT *, ROW_NUMBER() OVER (ORDER BY id) AS rn
    FROM orders
) t WHERE rn BETWEEN 21 AND 30;          -- 第 3 页（每页 10 条）

-- ② 分组 Top1（最新一条）
SELECT * FROM (
    SELECT *, ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY created_at DESC) AS rn
    FROM login_logs
) t WHERE rn = 1;

-- ③ 环比/同比（LAG 是报表标配）
SELECT month,
       amount,
       LAG(amount) OVER (ORDER BY month) AS prev_month,   -- 上月
       amount - LAG(amount) OVER (ORDER BY month) AS diff, -- 差值
       LAG(amount, 12) OVER (ORDER BY month) AS last_year  -- 去年同期
FROM monthly_sales;

-- ④ 去除重复（ROW_NUMBER = 1 保留一条）
DELETE FROM t FROM (
    SELECT ROW_NUMBER() OVER (PARTITION BY user_id, product_id ORDER BY id) AS rn
    FROM orders) t WHERE t.rn > 1;
```

> 💡 **经验**：SQL Server 2012+ 分页首选 OFFSET-FETCH；ROW_NUMBER 分页在"需要多级排序/去重组合"时仍有用武之地。窗口函数在 SQL Server 中支持 `ROWS/RANGE` 帧（与 PG 一致）。

---

## 2. TOP 与 OFFSET-FETCH

### 2.1 TOP（SQL Server 特有）

```sql
-- TOP：限制返回行数（比 LIMIT 更早出现）
SELECT TOP 10 * FROM orders ORDER BY amount DESC;

-- TOP 带 PERCENT（百分比）
SELECT TOP 10 PERCENT * FROM products ORDER BY price DESC;

-- TOP 带 TIES（含并列）
SELECT TOP 3 WITH TIES name, salary
FROM employees ORDER BY salary DESC;
-- 若第 3 名与第 4 名同薪 → 返回 4 行
```

### 2.2 OFFSET-FETCH（SQL Server 2012+）

```sql
-- 标准分页（推荐替代 TOP）
SELECT * FROM orders
ORDER BY id
OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY;  -- 第 3 页

-- ⚠️ OFFSET-FETCH 必须配合 ORDER BY（与 MySQL LIMIT 不同）
```

> 🎯 **要点**：新项目使用 OFFSET-FETCH（SQL 标准）；TOP 是 SQL Server 遗留语法，了解即可。`TOP WITH TIES` 是处理"并列排名"的独特语法。

### 2.3 SELECT INTO（表复制利器）

```sql
-- SELECT INTO：一条语句建表 + 复制数据（免 CREATE TABLE）
SELECT id, name, amount INTO orders_backup
FROM orders WHERE amount > 100;

-- ⚠️ 与 CREATE TABLE AS（PG）等价
-- 注意：不复制约束/索引/默认值，仅结构与数据
-- 场景：备份快照、分库分表迁移、ETL 中间表
```

| 分页方式 | 语法 | 适用 |
|---------|------|------|
| TOP | `SELECT TOP n` | 取前 N（无排序） |
| ROW_NUMBER | 嵌套查询 | 2012 前分页 |
| OFFSET-FETCH | 标准分页 | ✅ 新项目首选 |

---

## 3. PIVOT 与 UNPIVOT

### 3.1 PIVOT（行转列）

```sql
-- 原始数据：每行一个产品一个月销量
-- product | month | sales
-- PIVOT → 每行一个产品、12 列月销量
SELECT * FROM (
    SELECT product, month, sales FROM monthly_sales
) src
PIVOT (
    SUM(sales) FOR month IN ([Jan],[Feb],[Mar],[Apr],[May],[Jun])
) AS pvt;
```

### 3.2 UNPIVOT（列转行）

```sql
-- 反向：月列 → 行
SELECT product, month, sales
FROM quarterly_report
UNPIVOT (
    sales FOR month IN (Jan, Feb, Mar, Apr, May, Jun)
) AS unpvt;
```

> 🎯 **要点**：PIVOT/UNPIVOT 是 SQL Server 独有语法（MySQL 无、PG 需 crosstab 扩展）。财务报表/交叉报表场景常用。

### 3.3 动态列 PIVOT（列名不确定时）

```sql
-- 场景：月份不确定（报表按月动态扩展列）
DECLARE @cols NVARCHAR(MAX), @sql NVARCHAR(MAX);

-- ① 动态拼列名
SELECT @cols = STRING_AGG(QUOTENAME(month), ',')
FROM (SELECT DISTINCT month FROM monthly_sales) m;

-- ② 动态 SQL 执行 PIVOT
SET @sql = N'
    SELECT * FROM (
        SELECT product, month, sales FROM monthly_sales
    ) src
    PIVOT (SUM(sales) FOR month IN (' + @cols + ')) AS pvt';
EXEC sp_executesql @sql;

-- ⚠️ 注意：动态 SQL 有注入风险，列名必须 QUOTENAME 处理
```

---

## 4. MERGE 语句

```sql
-- MERGE = INSERT + UPDATE + DELETE 一体化（UPSERT）
MERGE INTO target_table AS t
USING source_table AS s
ON t.id = s.id
WHEN MATCHED AND s.status = 'delete' THEN
    DELETE
WHEN MATCHED THEN
    UPDATE SET t.name = s.name, t.amount = s.amount
WHEN NOT MATCHED THEN
    INSERT (id, name, amount) VALUES (s.id, s.name, s.amount);

-- 场景：ETL 数据同步、批量更新/插入
-- ⚠️ MERGE 是原子操作（单一语句，免手动判断）
```

> 🎯 **要点**：MERGE = 数据同步利器（ETL 中的"增量合并"）——一条语句处理增删改，避免分别写三个操作。MySQL 的 `ON DUPLICATE KEY` 和 PG 的 `ON CONFLICT` 只能做插入更新。

### 4.1 MERGE OUTPUT 与性能注意

```sql
-- ① OUTPUT 返回受影响行（审计/日志必备）
MERGE INTO target_table AS t
USING source_table AS s ON t.id = s.id
WHEN MATCHED THEN UPDATE SET t.name = s.name
WHEN NOT MATCHED THEN INSERT (id, name) VALUES (s.id, s.name)
OUTPUT $action, inserted.id, deleted.id;   -- ⚠️ inserted/deleted 伪表

-- ② 并发安全：目标表加 HOLDLOCK 防死锁
MERGE INTO target_table WITH (HOLDLOCK) AS t ...
-- ⚠️ 不加锁时：并发 MERGE 可能死锁或丢更新（常见坑）

-- ③ 性能注意：
--    source 侧先筛选好（避免全表扫描比对）
--    WHEN MATCHED 不带 UPDATE 子句可写 WHEN MATCHED THEN UPDATE SET (列不变)
--    大表同步分批（每批 1 万行），避免长事务 + 日志暴涨
```

```
⚠️ 面试必答：
"MERGE 的坑：目标表要 HOLDLOCK 防并发死锁；
 OUTPUT 拿变更行做审计；
 source 一定要先过滤；
 大表分批，别一条 MERGE 吞 100 万行。"
```

---

## 5. 临时表与表变量

### 5.1 临时表（#table）

```sql
-- ① 本地临时表（#）：当前会话可见
CREATE TABLE #temp_users (
    id INT, name NVARCHAR(50)
);
INSERT INTO #temp_users SELECT id, name FROM users WHERE status = 'active';
-- 会话结束自动删除

-- ② 全局临时表（##）：所有会话可见
CREATE TABLE ##global_temp (id INT);

-- ⚠️ 临时表有统计信息 → 大数据量优于表变量
```

### 5.2 表变量（@table）

```sql
-- 表变量：批处理/存储过程内（不产生事务日志）
DECLARE @temp TABLE (id INT, name NVARCHAR(50));
INSERT INTO @temp SELECT id, name FROM users WHERE id < 100;
-- ⚠️ 无统计信息 → 少量数据（< 100 行）性能好
```

| 维度 | 临时表 | 表变量 |
|------|:---:|:---:|
| 作用域 | 会话/全局 | 批处理 |
| 事务日志 | 有 | 无 |
| 统计信息 | ✅ | ❌ |
| 索引 | 可建 | 有限 |
| 适用 | 大数据量 | < 100 行 |

```
⚠️ 面试必答：
"临时表有统计信息（大数据量优）、
 表变量无日志（少量数据快）——
 100 行分界线是选型经验。"
```

### 5.3 临时表 vs 表变量 vs CTE 选型

| 维度 | CTE | 临时表 #t | 表变量 @t |
|------|:---:|:---:|:---:|
| 生命周期 | 单语句 | 会话 | 批处理 |
| 统计信息 | ❌ | ✅ | ❌（2019 起有基数估计优化） |
| 事务日志 | 无 | 有 | 无 |
| 索引 | ❌ | ✅ 可建 | ✅ 主键可建 |
| 大数据量 | 反复引用差 | ✅ 推荐 | 差 |
| 存储过程复用 | ✅ 内联 | ✅ 多次引用 | ✅ |

```
选型口诀：
  单次引用小结果 → CTE
  大数据量/多次引用 → 临时表（#）
  轻量小集合传参 → 表变量（@）
  场景切换存储过程间传递数据 → 临时表
```

```sql
-- 表变量性能陷阱（SQL Server 2019+ 可加内联优化提示）
DECLARE @t TABLE (id INT PRIMARY KEY, name NVARCHAR(50));
-- 大数据量用临时表替代，或给表变量建索引提示：
-- OPTION (RECOMPILE) 让优化器看到实际行数
SELECT * FROM @t OPTION (RECOMPILE);
```

---

## 6. SQL Server 2025 T-SQL 新特性

> 💡 SQL Server 2025（2025-11 发布）T-SQL 增强——AI 与开发者体验是主线，面试 2026 加分项。

### 6.1 原生 VECTOR 类型与向量检索

```sql
-- ⚠️ 原生向量类型 + DiskANN 索引（对标 pgvector）
CREATE TABLE documents (
    id INT PRIMARY KEY,
    content NVARCHAR(MAX),
    embedding VECTOR(1536)               -- 原生类型（默认 float32）
);
CREATE VECTOR INDEX idx_doc_embed ON documents (embedding);

-- 向量距离查询（VECTOR_DISTANCE，默认余弦）
SELECT TOP 5 content,
       VECTOR_DISTANCE(embedding, @query_vector) AS dist
FROM documents
ORDER BY dist;
```

### 6.2 原生 JSON 类型与正则函数

```sql
-- ① 原生 JSON 数据类型（不再是 NVARCHAR 模拟）
CREATE TABLE events (
    id INT PRIMARY KEY,
    payload JSON,                        -- ⚠️ 原生类型 + 可索引
    ts DATETIME2
);
CREATE INDEX idx_json ON events (payload);   -- 原生 JSON 索引

-- ② 原生正则表达式（2025 GA，2MB 输入上限）
SELECT * FROM users
WHERE REGEXP_LIKE(email, '^[a-z0-9._%+-]+@[a-z0-9.-]+\.[a-z]{2,}$');

SELECT REGEXP_SUBSTR(phone, '[0-9]{11}') AS number FROM users;
```

### 6.3 AI 集成与模糊匹配

```sql
-- ① 外部模型调用（T-SQL 直连 LLM，免中间件）
CREATE EXTERNAL MODEL openai_gpt
  WITH (SOURCE = 'OpenAI', MODEL = 'gpt-4o-mini');
EXEC sp_invoke_external_rest_endpoint
  @url = 'https://api.openai.com/v1/chat/completions', ...

-- ② 模糊字符串匹配（数据清洗/去重）
SELECT * FROM customers
WHERE FUZZY_MATCH(name, '张山') > 0.8;     -- 近似匹配（含纠错）
```

| SQL Server 2025 特性 | 一句话 | 对标 |
|---------------------|--------|------|
| VECTOR + DiskANN | 原生向量检索 | pgvector |
| 原生 JSON 类型 | 文档式查询 + 索引 | PG JSONB / MySQL JSON |
| 正则函数 | 原生 REGEXP_* 七函数 | MySQL/PG |
| sp_invoke_external_rest_endpoint | T-SQL 调 LLM/API | PG plpgsql + http |
| FUZZY_MATCH | 模糊匹配 | PG pg_trgm |

---

> 🎯 **核心要点**：T-SQL 独有 = **TOP WITH TIES**（并列处理）+ **OFFSET-FETCH**（现代分页）+ **PIVOT/UNPIVOT**（行列转换）+ **MERGE**（增删改同步 + OUTPUT + HOLDLOCK）+ **临时表/表变量**（数据中间存储）+ **2025 原生 VECTOR/JSON/正则**。这些是 T-SQL 相对标准 SQL 的扩展——SQL Server 企业开发的日常能力。

---

**返回总览**：[00-SQLServer总览与核心概念](00-SQLServer总览与核心概念.md) | **下一篇**：[02-SQLServer索引与性能](02-SQLServer索引与性能.md)
