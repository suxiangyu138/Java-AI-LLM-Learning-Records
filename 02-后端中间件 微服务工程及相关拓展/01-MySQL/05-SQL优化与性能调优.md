# 05 - SQL 优化与性能调优
> 定位：EXPLAIN 执行计划全列解析、慢查询日志与分析、索引失效场景、大分页/JOIN/COUNT 优化、速查表

## 1. SQL 优化

### 1.1 EXPLAIN 分析执行计划

EXPLAIN 是 SQL 优化最重要的工具，它展示了 MySQL 如何执行一条 SQL 语句。

```sql
EXPLAIN SELECT e.emp_name, d.dept_name
FROM employee e
LEFT JOIN department d ON e.dept_id = d.dept_id
WHERE e.salary > 5000;
```

各列详解：

#### type（访问类型，重点）

type 列表示 MySQL 找到目标行的方式，从好到差排序：

```
system > const > eq_ref > ref > range > index > ALL
```

| type | 含义 | 说明 |
|------|------|------|
| **system** | 表只有一行（系统表） | 极少见 |
| **const** | 主键或唯一索引等值匹配 | `WHERE id=1`，最多返回一行 |
| **eq_ref** | 主键或唯一索引被 JOIN 使用 | 通常出现在多表 JOIN 时，被驱动表按主键/唯一键匹配 |
| **ref** | 普通索引等值匹配 | `WHERE name='张三'`，可能返回多行 |
| **range** | 索引范围扫描 | `>`、`<`、`BETWEEN`、`IN`、`LIKE 'abc%'` |
| **index** | 扫描整个索引树 | 比 ALL 好一点，因为只读索引不读数据 |
| **ALL** | 全表扫描 | **性能最差，应极力避免** |

**优化目标**：至少达到 `range` 级别，力争 `ref` 或 `const`。

#### key（实际使用的索引）

MySQL 可能在一个表上用多个索引，`key` 列显示实际选择的索引。如果为 NULL，表示没有使用索引。

#### key_len（索引使用的字节数）

key_len 表示 MySQL 在索引中使用的字节数，可以推导出使用了联合索引的哪些列。

```
对于索引 (a INT, b VARCHAR(20) NOT NULL, c CHAR(10))：
- a 全部使用：key_len = 4（INT 占 4 字节）
- a 和 b 使用：key_len = 4 + 20*3（UTF8 下 VARCHAR(N) 最多 N*3 字节）+ 2（变长字段长度前缀）= 66
- a、b、c 全部使用：key_len = 4 + 60 + 2 + 10*3 = 96
```

#### rows（估算扫描行数）

rows 是 MySQL 估算的需要扫描的行数，不是精确值，但数量级参考意义很大。优化目标就是让 rows 尽可能小。

#### Extra（额外信息，重要）

| Extra | 含义 | 好坏 |
|-------|------|------|
| **Using index** | 覆盖索引，无需回表 | **好** |
| **Using index condition** | 使用了索引下推（ICP） | **较好** |
| **Using where** | 在 Server 层做了条件过滤 | 中性 |
| **Using filesort** | 需要额外的排序操作（数据量小时在内存排，大时在磁盘排） | **差** |
| **Using temporary** | 使用了临时表（常见于 GROUP BY、DISTINCT） | **差** |
| **Using index for group-by** | 使用了松散索引扫描做 GROUP BY | 好 |

---

### 1.2 慢查询日志

#### 开启与配置

```sql
-- 查看慢查询状态
SHOW VARIABLES LIKE 'slow_query%';
SHOW VARIABLES LIKE 'long_query_time';

-- 开启慢查询日志（MySQL 8.0 默认关闭）
SET GLOBAL slow_query_log = ON;
SET GLOBAL long_query_time = 1;  -- 单位秒，设置为 1 或更小
SET GLOBAL log_queries_not_using_indexes = ON;  -- 记录没有使用索引的查询
```

#### 分析工具

```bash
# mysqldumpslow 是最常用的慢查询分析工具
# 按平均查询时间排序，取前 10 条
mysqldumpslow -s t -t 10 /var/log/mysql/slow-query.log

# -s c: 按执行次数排序
# -s l: 按锁定时间排序
# -s r: 按返回行数排序
# -t N: 取前 N 条
```

---

### 1.3 常见优化策略

#### 避免 SELECT *

```sql
-- 差：查询所有列，无法使用覆盖索引，可能传输大量不必要的数据
SELECT * FROM employee WHERE name = '张三';

-- 好：只查询需要的列，且可以被索引覆盖
SELECT id, name FROM employee WHERE name = '张三';
```

#### 避免索引失效

以下操作会导致索引失效（或无法完全使用索引）：

```sql
-- 1. 在索引列上使用函数
WHERE DATE(create_time) = '2024-01-01'  -- 应改为 create_time >= '2024-01-01 00:00:00' AND create_time < '2024-01-02 00:00:00'

-- 2. 隐式类型转换（name 是 VARCHAR）
WHERE name = 123  -- 应改为 name = '123'

-- 3. 索引列参与计算
WHERE salary * 12 > 100000  -- 应改为 salary > 100000/12

-- 4. OR 条件（两侧不一定都是索引列）
WHERE name = '张三' OR age = 25  -- 如果 age 没有索引，则 name 索引也不生效

-- 5. 使用 != 或 <>
WHERE status != 'ACTIVE'  -- 这种范围太大，MySQL 通常放弃索引走全表

-- 6. LIKE 以通配符开头
WHERE name LIKE '%张%'  -- 右边通配符可以：LIKE '张%'

-- 7. NOT IN
WHERE id NOT IN (1, 2, 3)  -- 可考虑使用 NOT EXISTS 替代
```

#### 大分页优化（延迟关联）

```sql
-- 差：LIMIT 100000, 20 需要读取 100020 行数据，再丢弃前 100000 行
SELECT * FROM employee ORDER BY id LIMIT 100000, 20;

-- 好：延迟关联，先通过覆盖索引快速定位主键，再回表
SELECT e.*
FROM employee e
INNER JOIN (
    SELECT id FROM employee ORDER BY id LIMIT 100000, 20
) tmp ON e.id = tmp.id;
```

另一种优化方式是基于游标的分页（**推荐**）：

```sql
-- 差：传统分页，越往后越慢
SELECT * FROM employee ORDER BY id LIMIT 100000, 20;

-- 好：游标分页，记住上一页最后一条的 id
SELECT * FROM employee WHERE id > 100000 ORDER BY id LIMIT 20;
```

#### JOIN 小表驱动大表

在 JOIN 中，MySQL 通常选择小表作为驱动表（先查），大表作为被驱动表（后查），被驱动表走索引。

```sql
-- 假设 employee 10 万行，department 20 行
-- MySQL 会自动选择 department 作为驱动表，employee 作为被驱动表
-- employee.dept_id 上有索引时，被驱动表查询为 ref 级别
SELECT * FROM employee e JOIN department d ON e.dept_id = d.dept_id;
```

**优化建议**：确保被驱动表的 JOIN 列有索引。如果 `WHERE` 条件能大幅缩小驱动表的数据量，也可以显式用 STRAIGHT_JOIN 强制指定驱动顺序。

---

### 1.4 COUNT 查询优化

```sql
-- COUNT(*) vs COUNT(1) vs COUNT(列)

-- MySQL 对 COUNT(*) 做了特殊优化，选择最短的二级索引来计数
-- 结果：COUNT(*) = COUNT(1) > COUNT(列)
-- COUNT(列) 需要判断列是否为 NULL，因此更慢

-- 极差的写法：COUNT(列) 且列没有索引
SELECT COUNT(name) FROM employee;  -- 需要全表扫描

-- 好的写法
SELECT COUNT(*) FROM employee;  -- 选最小的二级索引扫描

---

## 2. SQL 优化速查表


| 问题/场景 | 优化方案 | 说明 |
|-----------|----------|------|
| 查询慢，type=ALL | 添加合适的索引 | 优先排查 WHERE 条件的列 |
| 排序慢，Using filesort | 在 ORDER BY 列上加索引 | 索引天然有序，避免额外排序 |
| 分组慢，Using temporary | 在 GROUP BY 列上加索引 | 松散索引扫描避免临时表 |
| 分页深，越翻越慢 | 游标分页或延迟关联 | 避免大 OFFSET |
| JOIN 慢 | 被驱动表 JOIN 列加索引 | 小表驱动大表 |
| 慢查询日志发现大量重复 SQL | 合并 SQL，避免 N+1 问题 | 批量查询代替循环逐条查询 |
| OR 导致索引失效 | 改为 UNION ALL 或用 IN | 或考虑给所有 OR 列建索引 |
| LIKE '%xxx%' 不走索引 | 考虑全文索引（FULLTEXT） | 或搜索引擎（ES）代替 |
| COUNT(*) 慢 | 确保有二级索引 | COUNT(*) 会自动选最小的二级索引 |
| 写操作慢 | 检查索引是否过多 | 写操作需要维护所有索引 |

---

## 3. 慢查询排查

### 9.1 慢查询日志

```sql
-- 开启慢查询日志
SET GLOBAL slow_query_log = ON;
SET GLOBAL long_query_time = 1;       -- 超过 1 秒记录
SET GLOBAL slow_query_log_file = '/var/log/mysql/slow.log';

-- 查看慢查询配置
SHOW VARIABLES LIKE 'slow_query%';
SHOW VARIABLES LIKE 'long_query_time';
```

### 9.2 常用排查命令

```sql
-- 查看当前连接状态
SHOW PROCESSLIST;

-- 查看 InnoDB 状态（含锁信息）
SHOW ENGINE INNODB STATUS;

-- 查看 SQL 执行耗时
SET profiling = 1;
SELECT * FROM t_order WHERE amount > 1000;
SHOW PROFILES;
SHOW PROFILE FOR QUERY 1;

-- EXPLAIN 分析执行计划（核心）
EXPLAIN FORMAT=JSON SELECT * FROM t_order WHERE amount > 1000;
```

### 9.3 优化流程

```
定位慢查询 → EXPLAIN 分析 → 检查索引 → 改写 SQL → 验证效果
     ↓                                                     ↓
  开启慢查询日志                                     EXPLAIN 确认
     ↓                                                     ↓
  SHOW PROCESSLIST                                  type/key/rows 改善
```

---

**返回总览**：[00-MySQL总览与技术术语](00-MySQL总览与技术术语.md) | **上一篇**：[04-索引原理与设计](04-索引原理与设计.md) | **下一篇**：[06-MySQL基础操作速查](06-MySQL基础操作速查.md)
