# 06-SQL调优.md -- SQL Performance Tuning

> **从"SQL 能跑就行"到"查询毫秒级响应"——数据库性能优化的系统方法**

---

## 目录

1. [SQL 执行顺序与优化器](#1-sql-执行顺序与优化器)
2. [索引优化深入](#2-索引优化深入)
3. [查询优化技术](#3-查询优化技术)
4. [Schema 优化](#4-schema-优化)
5. [慢查询分析](#5-慢查询分析)
6. [事务优化](#6-事务优化)
7. [连接池优化](#7-连接池优化)
8. [读写分离与分片](#8-读写分离与分片)
9. [真实案例](#9-真实案例)
10. [面试高频题](#10-面试高频题)

---

## 1. SQL 执行顺序与优化器

### 1.1 SQL 逻辑执行顺序

```sql
SELECT                          -- 5. 选择需要的列
    department_id,
    COUNT(*) AS emp_count,
    AVG(salary) AS avg_salary
FROM                            -- 1. 确定数据来源
    employees
WHERE                           -- 2. 行级别过滤
    status = 'ACTIVE'
    AND hire_date > '2020-01-01'
GROUP BY                        -- 3. 分组
    department_id
HAVING                          -- 4. 分组后过滤
    COUNT(*) > 5
ORDER BY                        -- 6. 排序
    avg_salary DESC
LIMIT 10;                       -- 7. 限制返回行数
```

**实际执行顺序（优化器可能重排）：**

```
FROM → ON → JOIN → WHERE → GROUP BY → HAVING →
SELECT → DISTINCT → ORDER BY → LIMIT/OFFSET
```

**理解执行顺序的重要性：**
- `WHERE` 在 `GROUP BY` 之前，所以 WHERE 不能使用聚合函数
- `HAVING` 在 `GROUP BY` 之后，可以过滤聚合结果
- `ORDER BY` 在 `SELECT` 之后，所以可以使用别名
- `LIMIT` 在 `ORDER BY` 之后，排序后再取前 N 条

### 1.2 MySQL 优化器工作流程

```text
SQL Query
    │
    ▼
Parser (解析器) — 语法解析，生成解析树
    │
    ▼
Preprocessor (预处理器) — 语义检查，权限检查
    │
    ▼
Optimizer (优化器) — 核心优化过程
    ├── 逻辑优化: 等价变换、子查询扁平化、视图合并
    ├── 物理优化: 访问路径选择、连接顺序选择
    └── 输出: 执行计划
    │
    ▼
Executor (执行器) — 执行执行计划，调用存储引擎接口
    │
    ▼
Storage Engine (存储引擎) — 数据读写 (InnoDB, MyISAM)
```

**优化器决策的考虑因素：**
- 全表扫描代价 vs 索引扫描代价
- 选择哪个索引（`show index from table` 查看可选索引）
- 多表连接时选择哪个表作为驱动表
- 子查询是否转换为 JOIN
- 是否使用临时表或文件排序

### 1.3 查看执行计划

```sql
-- 查看执行计划
EXPLAIN SELECT * FROM users WHERE email = 'test@example.com';

-- 更详细的执行计划 (MySQL 8.0.18+)
EXPLAIN ANALYZE SELECT * FROM users WHERE email = 'test@example.com';
```

**EXPLAIN 输出字段详解：**

| 字段 | 含义 | 优化方向 |
|------|------|---------|
| `id` | 查询序号，越大越先执行 | - |
| `select_type` | SIMPLE/PRIMARY/SUBQUERY/DERIVED | 避免 DERIVED (子查询作为临时表) |
| `table` | 访问的表 | - |
| `partitions` | 分区匹配情况 | 确认分区裁剪生效 |
| **`type`** | 访问类型 (性能从好到差) | 目标: const/ref/range，避免 ALL |
| `possible_keys` | 可能使用的索引 | 检查是否有合适的索引 |
| **`key`** | 实际选择的索引 | 确认索引被使用 |
| `key_len` | 索引使用的字节数 | 越长说明使用索引列越多 |
| `ref` | 索引匹配的列或常量 | - |
| **`rows`** | 预估扫描行数 | 越小越好，应尽量接近实际行数 |
| `filtered` | 通过 WHERE 后剩余百分比 | 越高越好 |
| **`Extra`** | 额外信息 | Using index (覆盖扫描) 最优 |

**type 列详解（性能从高到低）：**

| type | 含义 | 示例 |
|------|------|------|
| `system` | 表只有一行 (系统表) | 极少见 |
| `const` | 主键或唯一索引等值匹配 | `WHERE id = 1` |
| `eq_ref` | JOIN 时使用主键/唯一索引 | `JOIN ... ON a.id = b.user_id` |
| `ref` | 普通索引等值匹配 | `WHERE email = 'test@test.com'` |
| `range` | 索引范围扫描 | `WHERE age BETWEEN 20 AND 30` |
| `index` | 扫描索引树 (比 ALL 好一点) | `SELECT COUNT(*)` |
| `ALL` | 全表扫描 (需要避免) | `WHERE name LIKE '%keyword%'` |

**EXPLAIN ANALYZE 输出示例：**

```sql
EXPLAIN ANALYZE
SELECT u.name, COUNT(o.id) as order_count
FROM users u
LEFT JOIN orders o ON o.user_id = u.id
WHERE u.created_at > '2024-01-01'
GROUP BY u.id
ORDER BY order_count DESC
LIMIT 10;
```

```text
-> Limit: 10 row(s)  (actual time=15.2..15.3 rows=10 loops=1)
    -> Sort: order_count DESC, limit input to 10 row(s) per chunk
        (actual time=15.2..15.2 rows=10 loops=1)
        -> Stream results  (actual time=0.2..14.8 rows=523 loops=1)
            -> Group aggregate: count(o.id)
                (actual time=0.2..13.9 rows=523 loops=1)
                -> Nested loop left join
                    (actual time=0.1..11.5 rows=1523 loops=1)
                    -> Index range scan on u using idx_created_at
                        (actual time=0.1..2.3 rows=523 loops=1)
                    -> Index lookup on o using idx_user_id
                        (actual time=0.0..0.0 rows=2.9 loops=523)
```

---

## 2. 索引优化深入

### 2.1 索引数据结构

```text
B+Tree 结构:
          [10, 20, 30]           ← 内部节点 (只存索引，不存数据)
         /      |      \
        ▼       ▼       ▼
   [1,5,10] [15,20] [25,30]      ← 内部节点
    /   |     |   \    |   \
   ▼   ▼     ▼   ▼   ▼   ▼
  [1] [5]   [15][20] [25][30]    ← 叶子节点 (存完整行或主键)
   ↓   ↓     ↓   ↓   ↓   ↓
  Page1 Page2 ...              ← 双向链表连接叶子节点
```

**B+Tree 为什么适合索引：**
- 所有数据在叶子节点，查询次数稳定（树高度）
- 叶子节点双向链表，支持范围查询
- 内部节点不存数据，可存储更多索引键，降低树高
- 3-4 层即可支持百万到亿级数据

### 2.2 索引分类

| 类型 | 语法 | 特点 |
|------|------|------|
| 主键索引 | `PRIMARY KEY (id)` | 自动创建，叶子节点存行数据（聚簇） |
| 唯一索引 | `UNIQUE KEY (email)` | 确保唯一性，可为 NULL |
| 普通索引 | `INDEX (name)` | 加速查询，不约束唯一性 |
| 复合索引 | `INDEX (status, created_at)` | 最左前缀匹配，覆盖多个字段 |
| 全文索引 | `FULLTEXT (content)` | 针对大文本的模糊搜索 |
| 空间索引 | `SPATIAL (geo)` | 地理空间数据 |
| 降序索引 (8.0+) | `INDEX (created_at DESC)` | 支持降序排序 |
| 不可见索引 (8.0+) | `ALTER INDEX idx VISIBLE/INVISIBLE` | 安全删除索引 |

### 2.3 复合索引设计原则

**最左前缀原则：**
```sql
-- 创建复合索引
CREATE INDEX idx_status_created_user
ON orders (status, created_at, user_id);

-- 可以使用索引的情况:
WHERE status = 'PAID'                               -- ✓ 第一列
WHERE status = 'PAID' AND created_at > '2024-01-01'  -- ✓ 第一列 + 第二列
WHERE status = 'PAID' AND created_at > '2024-01-01'  -- ✓ 全部
    AND user_id = 123
WHERE created_at > '2024-01-01'                      -- ✗ 跳过了第一列，不能使用索引

-- 范围查询后的列不能继续使用索引:
WHERE status = 'PAID'                               -- 使用索引
    AND created_at > '2024-01-01'                    -- 使用索引（范围）
    AND user_id = 123                                -- 不能使用索引（在范围后）
```

**复合索引设计原则：**

| 原则 | 说明 | 示例 |
|------|------|------|
| 等值条件放前面 | 等值查询的列放在最前面 | `WHERE a=1 AND b>10` → INDEX(a, b) |
| 高基数放前面 | 区分度高的列放前面 | 先放 status（区分度高）再放 created_at |
| 范围条件放最后 | 范围查询的列放在最后 | INDEX(status, created_at) 而不是反着 |
| 覆盖索引优先 | 索引包含所有需要的列 | INDEX(status, amount) 如果只查这两个列 |

### 2.4 索引优化实战

```sql
-- 查看表索引
SHOW INDEX FROM orders;

-- 查看未使用的索引
SELECT
    OBJECT_SCHEMA,
    OBJECT_NAME,
    INDEX_NAME,
    COUNT_STAR,
    COUNT_READ,
    COUNT_WRITE
FROM performance_schema.table_io_waits_summary_by_index_usage
WHERE OBJECT_SCHEMA = 'your_database'
    AND INDEX_NAME IS NOT NULL
    AND COUNT_READ = 0;

-- 查找重复索引
SELECT
    a.TABLE_SCHEMA,
    a.TABLE_NAME,
    a.INDEX_NAME AS index_a,
    b.INDEX_NAME AS index_b,
    a.COLUMN_NAME
FROM information_schema.STATISTICS a
JOIN information_schema.STATISTICS b
    ON a.TABLE_SCHEMA = b.TABLE_SCHEMA
    AND a.TABLE_NAME = b.TABLE_NAME
    AND a.COLUMN_NAME = b.COLUMN_NAME
    AND a.SEQ_IN_INDEX = b.SEQ_IN_INDEX
    AND a.INDEX_NAME < b.INDEX_NAME;
```

### 2.5 索引使用验证

```sql
-- 用 EXPLAIN 验证索引是否被使用
EXPLAIN SELECT * FROM orders WHERE status = 'PAID' AND created_at > '2024-06-01';

-- 强制使用索引 (不推荐，仅用于测试)
SELECT * FROM orders FORCE INDEX(idx_status_created) WHERE status = 'PAID';

-- 忽略索引
SELECT * FROM orders IGNORE INDEX(idx_status_created) WHERE status = 'PAID';

-- 不可见索引 (MySQL 8.0+) — 安全删除索引的方式
ALTER TABLE orders ALTER INDEX idx_status SET INVISIBLE;
-- 观察一段时间，确认没有查询使用它
ALTER TABLE orders ALTER INDEX idx_status VISIBLE;  -- 恢复
DROP INDEX idx_status ON orders;                     -- 确认安全后删除
```

### 2.6 索引监控

```sql
-- 查询索引使用统计
SELECT * FROM performance_schema.table_io_waits_summary_by_index_usage
WHERE TABLE_NAME = 'orders';

-- 查询索引基数 (区分度)
SHOW INDEX FROM orders;
-- Cardinality 字段: 索引的区分度，接近行数说明区分度高

-- 索引大小
SELECT
    database_name,
    table_name,
    index_name,
    stat_value
FROM mysql.innodb_index_stats
WHERE table_name = 'orders'
    AND stat_name = 'size';
```

---

## 3. 查询优化技术

### 3.1 避免 SELECT *

```sql
-- ❌ 坏写法: 查询所有列
SELECT * FROM users WHERE email = 'test@test.com';

-- ✅ 好写法: 只查询需要的列
SELECT id, username, email FROM users WHERE email = 'test@test.com';
```

**SELECT * 的问题：**
1. 返回不需要的数据，增加网络传输
2. 无法使用覆盖索引（需要回表查询）
3. 增加内存消耗和 I/O
4. 表结构变化时应用可能出问题

### 3.2 分页优化

```sql
-- ❌ 深分页问题: OFFSET 越大越慢
SELECT * FROM orders ORDER BY created_at DESC LIMIT 10 OFFSET 100000;
-- 这条 SQL 需要扫描 100010 行，然后丢弃前 100000 行

-- 方式 1: 游标分页 (Cursor-based Pagination) — 推荐
-- 基于上一页的最后一条记录的 ID
SELECT * FROM orders
WHERE created_at < '2024-06-15 10:30:00'  -- 上一页的最后时间
ORDER BY created_at DESC
LIMIT 10;

-- 方式 2: 主键游标
SELECT * FROM orders
WHERE id < 100000  -- 上一页的最小 ID
ORDER BY id DESC
LIMIT 10;

-- 方式 3: 延迟关联 (Deferred Join) — 当游标分页不可用时
SELECT o.* FROM orders o
INNER JOIN (
    SELECT id FROM orders
    ORDER BY created_at DESC
    LIMIT 10 OFFSET 100000  -- 先在索引上定位
) tmp ON o.id = tmp.id;
```

**分页方式对比：**

| 方式 | 性能 | 可用性 | 适用场景 |
|------|------|--------|---------|
| 传统 OFFSET | 深分页极差 | 支持任意跳页 | 管理后台 |
| 游标分页 | 始终 O(1) | 不支持跳页 | 移动端、API |
| 延迟关联 | 大幅改善 | 支持任意跳页 | 任意跳页需求 |

### 3.3 JOIN 优化

```sql
-- 驱动表选择: 小表驱动大表
-- MySQL 会选择数据量小的表作为驱动表

-- 验证 JOIN 效率: 用 STRAIGHT_JOIN 强制指定驱动表
SELECT * FROM
    small_table STRAIGHT_JOIN big_table
    ON small_table.id = big_table.ref_id;

-- 确保 JOIN 列有索引
CREATE INDEX idx_user_id ON orders(user_id);

-- JOIN 时避免类型不一致
-- ❌
SELECT * FROM users u JOIN orders o ON u.id = o.user_id
WHERE u.id = '123';  -- 字符串 vs 数字 → 类型转换

-- ✅
SELECT * FROM users u JOIN orders o ON u.id = o.user_id
WHERE u.id = 123;
```

### 3.4 子查询优化

```sql
-- ❌ 低效: 子查询可能重复执行
SELECT * FROM users
WHERE id IN (
    SELECT user_id FROM orders WHERE amount > 1000
);

-- ✅ 优化: 使用 JOIN
SELECT DISTINCT u.*
FROM users u
INNER JOIN orders o ON u.id = o.user_id
WHERE o.amount > 1000;

-- ❌ 低效: 关联子查询
SELECT * FROM orders o
WHERE amount > (
    SELECT AVG(amount) FROM orders WHERE user_id = o.user_id
);

-- ✅ 优化: 使用派生表
SELECT o.*
FROM orders o
INNER JOIN (
    SELECT user_id, AVG(amount) as avg_amount
    FROM orders
    GROUP BY user_id
) avg_tbl ON o.user_id = avg_tbl.user_id
WHERE o.amount > avg_tbl.avg_amount;
```

**子查询优化原则：**

| 原写法 | 优化写法 | 原因 |
|--------|---------|------|
| `IN (SELECT ...)` | `JOIN` | Semi-join 可能自动优化，但 JOIN 更明确 |
| `NOT IN (SELECT ...)` | `NOT EXISTS` 或 `LEFT JOIN ... IS NULL` | NOT IN 不会使用索引 |
| `EXISTS (SELECT 1 ...)` | `JOIN` 或 `SEMI JOIN` | 现代 MySQL 已经优化 |
| `SELECT (SELECT ...)` | `JOIN` | 关联子查询每行执行一次 |

### 3.5 函数在 WHERE 中的陷阱

```sql
-- ❌ 索引列上使用函数 → 索引失效
SELECT * FROM users
WHERE DATE(created_at) = '2024-06-15';

-- ✅ 改写为条件范围
SELECT * FROM users
WHERE created_at >= '2024-06-15 00:00:00'
    AND created_at < '2024-06-16 00:00:00';

-- ❌ 隐式类型转换 → 索引失效
SELECT * FROM users WHERE phone = 13800138000;

-- ✅ 显式字符串查询
SELECT * FROM users WHERE phone = '13800138000';

-- ❌ LIKE 前缀模糊 → 索引失效
SELECT * FROM users WHERE name LIKE '%keyword%';

-- ✅ LIKE 后缀模糊 → 可以用索引
SELECT * FROM users WHERE name LIKE 'keyword%';
```

### 3.6 OR 优化

```sql
-- ❌ OR 可能导致索引失效
SELECT * FROM orders
WHERE status = 'PAID' OR status = 'SHIPPED';

-- ✅ UNION ALL 替代 OR
SELECT * FROM orders WHERE status = 'PAID'
UNION ALL
SELECT * FROM orders WHERE status = 'SHIPPED';

-- ✅ 或者用 IN (如果索引合理)
SELECT * FROM orders WHERE status IN ('PAID', 'SHIPPED');

-- ❌ OR 跨不同索引列
SELECT * FROM orders
WHERE status = 'PAID' OR amount > 1000;

-- ✅ 拆分为 UNION
SELECT * FROM orders WHERE status = 'PAID'
UNION
SELECT * FROM orders WHERE amount > 1000;
```

### 3.7 IN vs EXISTS

```sql
-- 小表驱动大表时:
-- 子查询表小 → 用 IN
SELECT * FROM orders WHERE user_id IN (
    SELECT id FROM users WHERE status = 'VIP'
);

-- 子查询表大 → 用 EXISTS
SELECT * FROM users WHERE EXISTS (
    SELECT 1 FROM orders WHERE orders.user_id = users.id
);

-- 但现代 MySQL 优化器可能自动优化，最好用 EXPLAIN 验证
```

### 3.8 COUNT(*) 优化

```sql
-- COUNT(*) 不同类型性能
SELECT COUNT(*) FROM users;              -- InnoDB 需要扫描全表或索引
SELECT COUNT(1) FROM users;              -- 和 COUNT(*) 一样
SELECT COUNT(id) FROM users;             -- 如果 id 允许 NULL，结果可能不同

-- 近似行数快速获取
SELECT TABLE_ROWS
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'db' AND TABLE_NAME = 'users';
-- 注意: 这个值是估算的，不精确

-- COUNT 特定条件优化
-- ❌ 扫描全表
SELECT COUNT(*) FROM orders;

-- ✅ 使用覆盖索引 (如果索引包含所有需要判断的列)
SELECT COUNT(*) FROM orders WHERE status = 'PAID';
-- 如果 status 有索引，只需要扫描索引树
```

### 3.9 DISTINCT 和 GROUP BY 优化

```sql
-- DISTINCT 和 GROUP BY 原理相似，不使用索引时都需要临时表

-- ❌ DISTINCT 去重扫描大表
SELECT DISTINCT user_id FROM orders;

-- ✅ 如果 user_id 有索引，GROUP BY 可能更高效
SELECT user_id FROM orders GROUP BY user_id;

-- 优化 GROUP BY: 确保排序列有索引
-- 如果 ORDER BY 和 GROUP BY 的列相同
SELECT status, COUNT(*)
FROM orders
GROUP BY status
ORDER BY status;  -- 如果 status 有索引，避免文件排序

-- 延迟 GROUP BY
-- ❌ 先分组再 JOIN
SELECT u.name, COUNT(o.id)
FROM users u
LEFT JOIN orders o ON u.id = o.user_id
GROUP BY u.id, u.name;

-- ✅ 先聚合再 JOIN
SELECT u.name, t.order_count
FROM users u
LEFT JOIN (
    SELECT user_id, COUNT(*) as order_count
    FROM orders
    GROUP BY user_id
) t ON u.id = t.user_id;
```

---

## 4. Schema 优化

### 4.1 数据类型选择

| 数据 | 推荐类型 | 不推荐 | 原因 |
|------|---------|--------|------|
| 状态 (0/1) | `TINYINT(1)` | `INT` | 节省 3 字节 |
| 年龄/数量 | `INT UNSIGNED` | `BIGINT` | 省 4 字节 |
| 金额 | `DECIMAL(10,2)` | `FLOAT/DOUBLE` | 避免浮点精度问题 |
| 手机号 | `VARCHAR(20)` | `BIGINT` | 方便模糊搜索 |
| 邮箱 | `VARCHAR(100)` | `TEXT` | 索引更长、性能差 |
| 大文本 | `TEXT` / `LONGTEXT` | `VARCHAR(65535)` | 超长 VARCHAR 可能降级 |
| IP 地址 | `INT UNSIGNED` | `VARCHAR(15)` | 4 字节 vs 15 字节 |
| UUID | `BINARY(16)` | `CHAR(36)` | 16 字节 vs 36 字节 |

**更小的通常更快：**
- 磁盘 I/O 更小
- 内存缓存能放更多数据
- CPU 处理更快
- 索引树更矮（单个 key 更小）

### 4.2 范式与反范式

```sql
-- 范式化: 减少冗余
-- orders 表不存用户姓名，通过 user_id 关联
CREATE TABLE orders (
    id BIGINT PRIMARY KEY,
    user_id BIGINT,
    amount DECIMAL(10,2),
    created_at DATETIME
);

CREATE TABLE users (
    id BIGINT PRIMARY KEY,
    name VARCHAR(100),
    email VARCHAR(200)
);

-- 反范式化: 用空间换时间
-- 在 orders 表中冗余存 user_name，避免频繁 JOIN
CREATE TABLE orders (
    id BIGINT PRIMARY KEY,
    user_id BIGINT,
    user_name VARCHAR(100),  -- 冗余字段
    amount DECIMAL(10,2),
    created_at DATETIME
);
```

**范式 vs 反范式选择：**

| 场景 | 推荐 |
|------|------|
| 数据一致性要求高 | 范式化 |
| 写频繁 | 范式化 |
| 读频繁，JOIN 是瓶颈 | 反范式化 |
| 报表/分析查询 | 反范式化 |
| 数据量小 | 范式化 |
| 数据量大且读多写少 | 反范式化 |

### 4.3 垂直分表

```sql
-- 原表: 所有列在一起
CREATE TABLE products (
    id BIGINT PRIMARY KEY,
    name VARCHAR(100),
    price DECIMAL(10,2),
    description TEXT,          -- 大字段，不常查询
    image_data LONGBLOB,      -- 超大字段，很少直接查
    created_at DATETIME,
    updated_at DATETIME
);

-- 拆分成主表和扩展表
CREATE TABLE products (
    id BIGINT PRIMARY KEY,
    name VARCHAR(100),
    price DECIMAL(10,2),
    created_at DATETIME,
    updated_at DATETIME
);

CREATE TABLE product_details (
    product_id BIGINT PRIMARY KEY,
    description TEXT,
    image_data LONGBLOB,
    FOREIGN KEY (product_id) REFERENCES products(id)
);
```

### 4.4 水平分区

```sql
-- MySQL 分区表 (8.0+)
CREATE TABLE orders (
    id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    amount DECIMAL(10,2),
    created_at DATETIME NOT NULL
) PARTITION BY RANGE (YEAR(created_at)) (
    PARTITION p2022 VALUES LESS THAN (2023),
    PARTITION p2023 VALUES LESS THAN (2024),
    PARTITION p2024 VALUES LESS THAN (2025),
    PARTITION p_future VALUES LESS THAN MAXVALUE
);

-- 分区裁剪: 查询时只扫描相关分区
EXPLAIN SELECT * FROM orders WHERE created_at >= '2024-01-01';
-- partitions: p2024,p_future  (不会扫描 p2022,p2023)

-- 分区类型:
-- RANGE: 按范围分区 (最常用)
-- LIST: 按值列表分区
-- HASH: 按哈希分区
-- KEY: 类似 HASH 但由 MySQL 决定
```

---

## 5. 慢查询分析

### 5.1 启用慢查询日志

```sql
-- 查看当前配置
SHOW VARIABLES LIKE 'slow_query%';
SHOW VARIABLES LIKE 'long_query_time';

-- 临时开启 (重启失效)
SET GLOBAL slow_query_log = 'ON';
SET GLOBAL long_query_time = 0.5;       -- 超过 500ms
SET GLOBAL log_queries_not_using_indexes = 'ON';  -- 记录未使用索引的查询
SET GLOBAL log_slow_admin_statements = 'ON';      -- 记录慢管理语句

-- 查看慢查询日志位置
SHOW VARIABLES LIKE 'slow_query_log_file';
```

### 5.2 mysqldumpslow 分析

```bash
# 分析慢查询日志
mysqldumpslow -t 10 /var/log/mysql/slow.log        # 前 10 条
mysqldumpslow -s c -t 10 /var/log/mysql/slow.log   # 按查询次数排序
mysqldumpslow -s t -t 10 /var/log/mysql/slow.log   # 按查询时间排序
mysqldumpslow -s l -t 10 /var/log/mysql/slow.log   # 按锁时间排序
mysqldumpslow -g "SELECT" /var/log/mysql/slow.log  # 过滤特定类型
```

### 5.3 pt-query-digest (Percona Toolkit)

```bash
# 安装 Percona Toolkit
apt-get install percona-toolkit

# 分析慢查询日志
pt-query-digest /var/log/mysql/slow.log

# 从进程列表分析
pt-query-digest --processlist h=localhost

# 从 tcpdump 分析
pt-query-digest --type tcpdump traffic.pcap

# 输出到文件
pt-query-digest /var/log/mysql/slow.log > query_report.txt

# 分析结果示例:
# Profile
# Rank Query ID           Response time    Calls R/Call  V/M
# ==== ================== ================ ===== ======= =====
#    1 0x1234ABCD567890EF  25.3455 38.2%    123  0.2061  0.00 SELECT orders
#    2 0x5678EFAB123490CD  15.2344 22.9%     56  0.2720  0.00 SELECT users
#    3 0x9012CDEF345678AB  10.1234 15.2%     89  0.1137  0.00 UPDATE orders
```

### 5.4 常见慢查询模式

#### 模式 1: 缺少索引

```sql
-- EXPLAIN 结果: type: ALL, rows: 1000000
SELECT * FROM orders WHERE status = 'PENDING';

-- 解决方案: 添加索引
CREATE INDEX idx_status ON orders(status);
```

#### 模式 2: 索引未使用

```sql
-- 有索引但没用
EXPLAIN SELECT * FROM orders WHERE YEAR(created_at) = 2024;
-- type: ALL (函数导致索引失效)

-- 修复: 改为范围查询
SELECT * FROM orders
WHERE created_at >= '2024-01-01'
  AND created_at < '2025-01-01';
```

#### 模式 3: 锁等待

```sql
-- 查询长时间等待锁
SHOW PROCESSLIST;
-- State: Updating, Waiting for table metadata lock

-- 查看当前锁
SELECT * FROM information_schema.INNODB_TRX\G
SELECT * FROM performance_schema.metadata_locks\G
```

#### 模式 4: 大偏移量分页

```sql
-- 扫描太多行
EXPLAIN SELECT * FROM orders LIMIT 10 OFFSET 100000;
-- rows: 100010

-- 解决方案: 游标分页或延迟关联
```

---

## 6. 事务优化

### 6.1 短事务原则

```java
// ❌ 事务中做耗时操作
@Transactional
public void processOrder(Long orderId) {
    Order order = orderRepository.findById(orderId).orElseThrow();

    // 调用外部 API (耗时长、网络不稳定)
    String paymentResult = paymentService.charge(order.getAmount());

    // 发送邮件通知
    emailService.sendOrderConfirmation(order);

    order.setStatus(OrderStatus.PAID);
    orderRepository.save(order);
}

// ✅ 事务只包含数据库操作
@Transactional
public void processOrder(Long orderId) {
    Order order = orderRepository.findById(orderId).orElseThrow();
    order.setStatus(OrderStatus.PAID);
    orderRepository.save(order);
}

// 非事务操作放在事务外
public void processOrderComplete(Long orderId) {
    // 1. 事务内: 只做数据库操作
    processOrder(orderId);

    // 2. 事务外: 调用外部服务
    Order order = orderRepository.findById(orderId).orElseThrow();
    paymentService.chargeAsync(order.getAmount());
    emailService.sendOrderConfirmationAsync(order);
}
```

### 6.2 批量操作

```java
// ❌ 逐条插入 (每条一个事务)
for (Order order : orders) {
    orderRepository.save(order);
}

// ✅ 批量插入 (一个事务)
// 方式 1: JPA — 使用 saveAll
orderRepository.saveAll(orders);

// 方式 2: JDBC batch
String sql = "INSERT INTO orders (user_id, amount, status) VALUES (?, ?, ?)";
jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
    @Override
    public void setValues(PreparedStatement ps, int i) throws SQLException {
        Order order = orders.get(i);
        ps.setLong(1, order.getUserId());
        ps.setBigDecimal(2, order.getAmount());
        ps.setString(3, order.getStatus());
    }

    @Override
    public int getBatchSize() {
        return orders.size();
    }
});

// 方式 3: JDBC URL 配置批量参数
// jdbc:mysql://localhost:3306/db?rewriteBatchedStatements=true
```

**批量操作建议大小：**

| 操作 | 建议批次大小 | 说明 |
|------|-------------|------|
| INSERT | 500-1000 | 太大会导致 undo log 膨胀 |
| UPDATE | 200-500 | 影响行数多，锁范围大 |
| DELETE | 100-200 | 大量删除导致锁和主从延迟 |

### 6.3 锁竞争优化

```sql
-- 查看锁等待
SHOW ENGINE INNODB STATUS\G
SELECT * FROM sys.innodb_lock_waits;

-- 减少行锁范围:
-- 1. 使用索引，减少锁扫描的行数
-- 2. 缩短事务时间
-- 3. 按相同顺序访问资源，避免死锁
-- 4. 考虑使用 READ COMMITTED (MVCC 减少锁)
SET GLOBAL TRANSACTION ISOLATION LEVEL READ COMMITTED;
```

---

## 7. 连接池优化

### 7.1 HikariCP 最佳配置

```yaml
# application.yml
spring:
  datasource:
    hikari:
      # 连接池大小
      maximum-pool-size: 20
      minimum-idle: 5

      # 超时设置
      connection-timeout: 30000       # 获取连接超时 (30s)
      idle-timeout: 600000            # 空闲连接超时 (10min)
      max-lifetime: 1800000           # 连接最大存活时间 (30min)
      keepalive-time: 300000          # 保持连接活跃 (5min)

      # 验证
      connection-test-query: SELECT 1
      validation-timeout: 5000

      # 性能
      leak-detection-threshold: 60000 # 连接泄漏检测 (60s)
      pool-name: UserServicePool
```

**连接池大小计算：**

```text
公式: 池大小 = Tn × (Cm - 1)
  Tn = 最大线程数 (如 Tomcat 200)
  Cm = 单个请求的数据库连接数 (通常 1)

估算: 200 × (1 - 1) + 1 = ... 实际上经验值:
- 20-50 个连接通常足够大多数应用
- 过多连接反而导致 MySQL 上下文切换开销

原则: 连接池不是越大越好！
- 每个连接需要内存 (~10MB)
- 连接数过多 = 并发争抢 = 性能下降
- PostgreSQL: 经验公式 (2 * core_count + 1)
```

### 7.2 连接池监控

```yaml
# 暴露 HikariCP 指标
management:
  metrics:
    tags:
      application: ${spring.application.name}
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
```

```promql
# 监控指标
hikaricp_connections_active      # 活跃连接数
hikaricp_connections_idle        # 空闲连接数
hikaricp_connections_pending     # 等待获取连接的线程数
hikaricp_connections_timeout_total  # 获取连接超时总数
hikaricp_connections_max         # 最大连接数
```

---

## 8. 读写分离与分片

### 8.1 MySQL 主从架构

```yaml
# Spring Boot 配置读写分离
spring:
  datasource:
    # 主库 (写)
    primary:
      jdbc-url: jdbc:mysql://master:3306/db
      username: root
      password: secret
    # 从库 (读)
    replica:
      jdbc-url: jdbc:mysql://replica1:3306/db
      username: root
      password: secret
```

```java
// 使用 ShardingSphere 实现读写分离
@Configuration
public class ShardingConfig {

    @Bean
    public DataSource dataSource() {
        // ShardingSphere 5.x 配置
        YamlShardingSphereDataSourceFactory.createDataSource();
    }
}
```

### 8.2 ShardingSphere-Proxy 配置

```yaml
# config-sharding.yaml
dataSources:
  ds_0:
    dataSourceClassName: com.zaxxer.hikari.HikariDataSource
    driverClassName: com.mysql.cj.jdbc.Driver
    jdbcUrl: jdbc:mysql://master:3306/db
    username: root
    password: secret
  ds_0_slave:
    dataSourceClassName: com.zaxxer.hikari.HikariDataSource
    driverClassName: com.mysql.cj.jdbc.Driver
    jdbcUrl: jdbc:mysql://slave1:3306/db
    username: root
    password: secret

rules:
  - !READWRITE_SPLITTING
    dataSources:
      readwrite_ds:
        writeDataSourceName: ds_0
        readDataSourceNames:
          - ds_0_slave

  - !SHARDING
    tables:
      orders:
        actualDataNodes: ds_0.orders_${0..3}
        tableStrategy:
          standard:
            shardingColumn: id
            shardingAlgorithmName: orders_inline
    shardingAlgorithms:
      orders_inline:
        type: INLINE
        props:
          algorithm-expression: orders_${id % 4}
```

---

## 9. 真实案例

### 案例 1: 深分页从 5s 到 50ms

```sql
-- 优化前: 5.2s
SELECT * FROM orders ORDER BY created_at DESC LIMIT 20 OFFSET 50000;

-- 优化后: 47ms
SELECT o.* FROM orders o
INNER JOIN (
    SELECT id FROM orders
    ORDER BY created_at DESC
    LIMIT 20 OFFSET 50000
) tmp ON o.id = tmp.id;
```

### 案例 2: N+1 查询

```java
// 优化前: N+1 查询 (1 + 1000 次查询)
List<Order> orders = orderRepository.findAll();
for (Order order : orders) {
    User user = userRepository.findById(order.getUserId());  // 循环内查询
}

// 优化后: 2 次查询
List<Order> orders = orderRepository.findAll();
Set<Long> userIds = orders.stream()
    .map(Order::getUserId)
    .collect(Collectors.toSet());
Map<Long, User> userMap = userRepository.findAllById(userIds)
    .stream()
    .collect(Collectors.toMap(User::getId, Function.identity()));
```

### 案例 3: 连接数过大导致服务不可用

```yaml
# 优化前: 100 个连接
spring.datasource.hikari.maximum-pool-size=100

# 优化后: 20 个连接
spring.datasource.hikari.maximum-pool-size=20
```
效果：连接池争用从 80% 降低到 5%，CPU 使用率降低 40%，QPS 提升 2 倍。

### 案例 4: 索引优化对比

| 查询 | 优化前 | 优化后 | 提升 |
|------|--------|--------|------|
| 根据 email 查用户 | `type: ALL, 100ms` | `type: const, 0.5ms` | 200x |
| 订单状态统计 | `type: ALL, 3s` | `type: ref, 20ms` | 150x |
| 日期范围订单分页 | `type: ALL, 5s` | `type: range, 50ms` | 100x |

---

## 10. 面试高频题

### 基础概念类

**Q: MySQL EXPLAIN 中的 type 字段各值含义？**
A: 从好到差: system → const → eq_ref → ref → range → index → ALL。目标是 ref 以上，避免 ALL。

**Q: B+Tree 索引为什么适合范围查询？**
A: B+Tree 的叶子节点通过双向链表连接，因此一旦定位到范围起始点，只需要沿着链表遍历直到结束即可，适合范围查询。

**Q: 什么情况下索引会失效？**
A:
1. 对索引列使用函数: `WHERE DATE(created_at) = '2024-01-01'`
2. 隐式类型转换: `WHERE phone = 13800138000` (phone 是 varchar)
3. LIKE 前缀模糊: `WHERE name LIKE '%keyword'`
4. OR 条件中包含非索引列
5. 复合索引不满足最左前缀

### 实战类

**Q: 有一条慢 SQL，怎么优化？**
A: 系统化排查步骤：
1. `EXPLAIN` 看执行计划，检查 type 和 rows
2. 确认是否使用了合适的索引
3. 检查 SQL 写法是否优化（避免函数、SELECT *）
4. 分析数据分布（选择性、数据量）
5. 考虑 Schema 优化（数据类型、冗余字段、反范式）
6. 最终方案：加索引、改写 SQL、改表结构

**Q: 如何处理深分页问题？**
A:
1. 优先使用游标分页（推荐）
2. 延迟关联（先查主键再 JOIN）
3. 限制跳页数（最多 100 页）
4. 使用搜索引擎 (Elasticsearch)
5. 业务上不允许深度翻页（如只显示前 100 页）

**Q: SELECT * 为什么不好？**
A:
1. 多查了不需要的列，增加网络 I/O 和内存
2. 无法使用覆盖索引，必须回表
3. 表结构变化可能导致应用异常

### 场景类

**Q: 线上突然数据库 CPU 100%，怎么排查？**
A:
1. 紧急: `SHOW PROCESSLIST` 看哪些查询在运行
2. `EXPLAIN` 分析慢查询
3. 检查是否有大量慢查询堆积
4. 检查是否有大事务、锁等待
5. 检查连接池是否被打满
6. 临时止损: KILL 查询、限流、扩容

**Q: 刚上线了某个功能，数据库连接池被打满了，原因和解决方案？**
A:
原因：
- 连接泄漏（未关闭的连接）
- QPS 大增超过连接池上限
- 每个请求持有连接时间过长
解决方案：
- 检查连接是否及时归还
- 缩短事务时间
- 设置 `leak-detection-threshold`
- 临时增大连接池（治标）
- 添加缓存（治本）

**Q: 主从延迟导致业务读到了旧数据，怎么处理？**
A:
1. 写后立即读的场景强制从主库读取
2. 使用缓存（Redis）缓存最新数据
3. 业务层容忍短暂不一致
4. 监控主从延迟，延迟过大时切主
5. 半同步复制减少延迟

---

## 总结检查清单

- [ ] EXPLAIN 分析是否成为日常习惯
- [ ] 所有查询是否都有合适的索引
- [ ] 复合索引是否遵循最左前缀原则
- [ ] 是否有 SELECT * 需要优化
- [ ] 深分页是否使用游标或延迟关联
- [ ] N+1 查询是否已排查
- [ ] 慢查询日志是否已开启
- [ ] 是否有定期分析慢查询的习惯
- [ ] Schema 设计是否合理（数据类型、范式）
- [ ] 事务是否尽可能短
- [ ] 连接池配置是否合理
- [ ] 是否有读写分离方案
- [ ] 是否有容量规划和监控
