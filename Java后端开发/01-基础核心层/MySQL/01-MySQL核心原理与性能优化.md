# MySQL 核心原理与性能优化

> 本文从 SQL 基础进阶、事务与锁机制、索引原理、SQL 优化、架构高可用五大维度，系统性地梳理 MySQL 核心知识体系。全文搭配大量实际 SQL 示例与 EXPLAIN 输出，力求兼顾理论深度与工程实战价值。

---

## 目录

1. [SQL 基础与进阶](#1-sql-基础与进阶)
2. [事务与锁](#2-事务与锁)
3. [索引原理](#3-索引原理)
4. [SQL 优化](#4-sql-优化)
5. [架构与高可用](#5-架构与高可用)
6. [SQL 优化速查表](#6-sql-优化速查表)
7. [面试核心问题](#7-面试核心问题)

---

## 1. SQL 基础与进阶

### 1.1 多表连接

多表连接是关系型数据库最核心的操作之一。MySQL 支持以下几种 JOIN 类型，理解它们的语义是写出正确 SQL 的前提。

#### INNER JOIN（内连接）

内连接返回两个表的交集部分，即只返回两个表中满足连接条件的行。

```sql
-- 查询每位员工及其所属部门（只显示有部门的员工）
SELECT e.emp_name, d.dept_name
FROM employee e
INNER JOIN department d ON e.dept_id = d.dept_id;
```

逻辑等价于用 WHERE 做隐式连接：

```sql
SELECT e.emp_name, d.dept_name
FROM employee e, department d
WHERE e.dept_id = d.dept_id;
```

#### LEFT JOIN（左外连接）

左表全部保留，右表只返回匹配的行，不匹配的地方补 NULL。

```sql
-- 查询所有员工及其部门（没有部门的员工也要显示）
SELECT e.emp_name, d.dept_name
FROM employee e
LEFT JOIN department d ON e.dept_id = d.dept_id;
```

一个高频面试考点：**用 LEFT JOIN 查「在左表但不在右表」的数据**。

```sql
-- 查询没有部门的员工
SELECT e.emp_name
FROM employee e
LEFT JOIN department d ON e.dept_id = d.dept_id
WHERE d.dept_id IS NULL;
```

#### RIGHT JOIN（右外连接）

与 LEFT JOIN 对称，右表全保留，左表匹配。

```sql
SELECT e.emp_name, d.dept_name
FROM employee e
RIGHT JOIN department d ON e.dept_id = d.dept_id;
```

实践中建议统一使用 LEFT JOIN，避免 LEFT/RIGHT 混用增加理解成本。

#### FULL JOIN（全外连接）

MySQL 不直接支持 FULL OUTER JOIN，需要用 UNION 模拟：

```sql
-- 查询所有员工和所有部门（无论是否匹配）
SELECT e.emp_name, d.dept_name
FROM employee e
LEFT JOIN department d ON e.dept_id = d.dept_id
UNION
SELECT e.emp_name, d.dept_name
FROM employee e
RIGHT JOIN department d ON e.dept_id = d.dept_id;
```

UNION 自带去重，如果需要保留重复行可使用 UNION ALL。

#### CROSS JOIN（交叉连接 / 笛卡尔积）

两表所有行组合，结果行数 = 左表行数 × 右表行数。通常需要配合 WHERE 条件使用，否则数据量会爆炸。

```sql
-- 所有员工与所有部门的组合
SELECT e.emp_name, d.dept_name
FROM employee e
CROSS JOIN department d;

-- 等价写法
SELECT e.emp_name, d.dept_name
FROM employee e, department d;
```

**实践建议**：在线上环境务必警惕 CROSS JOIN，一个 10 万行的表和 1 万行的表做笛卡尔积会产生 10 亿条中间结果，直接打爆内存。

---

### 1.2 子查询

子查询是嵌套在另一个 SQL 语句内部的查询，根据返回结果的不同分为三类。

#### 标量子查询（返回单个值）

```sql
-- 查询工资高于平均值的员工
SELECT emp_name, salary
FROM employee
WHERE salary > (SELECT AVG(salary) FROM employee);
```

标量子查询可以出现在 SELECT、WHERE、HAVING 子句中。

#### 行子查询（返回一行多列）

```sql
-- 查询与 '张三' 部门和职位都相同的员工
SELECT emp_name, dept_id, job_title
FROM employee
WHERE (dept_id, job_title) = (
    SELECT dept_id, job_title FROM employee WHERE emp_name = '张三'
);
```

#### 列子查询（返回一列多行）

```sql
-- 查询在销售部的所有员工
SELECT emp_name
FROM employee
WHERE dept_id IN (
    SELECT dept_id FROM department WHERE dept_name = '销售部'
);
```

#### EXISTS vs IN

这是一个面试常考点。EXISTS 和 IN 在语义上都可以判断存在性，但执行逻辑不同。

```sql
-- IN 子查询：先执行子查询，结果集物化后去重，外层逐行匹配
SELECT * FROM employee
WHERE dept_id IN (SELECT dept_id FROM department);

-- EXISTS 子查询：外层逐行遍历，对每一行执行子查询，找到第一个匹配即停止
SELECT * FROM employee e
WHERE EXISTS (
    SELECT 1 FROM department d WHERE d.dept_id = e.dept_id
);
```

- **IN 适合子查询结果集小、外层表大的场景**：因为子查询结果可以缓存。
- **EXISTS 适合子查询结果集大、外层表小的场景**：因为它是外层驱动内层，可以利用内层索引。
- **NOT IN vs NOT EXISTS**：NOT IN 在有 NULL 值时结果集为空（因为 `NULL IN (...) ` 返回 UNKNOWN），而 NOT EXISTS 不受 NULL 影响。因此能用 NOT EXISTS 就不要用 NOT IN。

#### 相关子查询 vs 非相关子查询

- **非相关子查询**：可以独立执行，不依赖外层查询。执行一次即可，性能通常更好。
- **相关子查询**：依赖外层查询的列，外层每一行都要执行一次子查询。性能较差，但表达能力更强。

```sql
-- 相关子查询：查询每个部门中工资最高的员工
SELECT e1.emp_name, e1.salary, e1.dept_id
FROM employee e1
WHERE e1.salary = (
    SELECT MAX(e2.salary) FROM employee e2 WHERE e2.dept_id = e1.dept_id
);
```

---

### 1.3 分组聚合

#### GROUP BY 与聚合函数

GROUP BY 将数据按指定列分组，配合聚合函数（COUNT、SUM、AVG、MAX、MIN）对每组进行计算。

```sql
-- 统计每个部门的员工数和平均工资
SELECT dept_id,
       COUNT(*) AS emp_count,
       AVG(salary) AS avg_salary
FROM employee
GROUP BY dept_id;
```

#### HAVING vs WHERE

这是最容易被混淆的概念。两者的核心区别在于**执行时机**：

- **WHERE** 在 GROUP BY **之前**执行，筛选的是**原始行数据**，不能使用聚合函数。
- **HAVING** 在 GROUP BY **之后**执行，筛选的是**分组后的结果**，可以使用聚合函数。

```sql
-- WHERE: 先过滤工资 >= 5000 的员工，再分组统计
SELECT dept_id, COUNT(*) AS high_salary_count
FROM employee
WHERE salary >= 5000
GROUP BY dept_id;

-- HAVING: 分组后筛选平均工资 > 8000 的部门
SELECT dept_id, AVG(salary) AS avg_salary
FROM employee
GROUP BY dept_id
HAVING AVG(salary) > 8000;
```

#### ROLLUP（小计）

ROLLUP 在 GROUP BY 的基础上增加小计和总计行，在报表场景非常实用。

```sql
-- 按部门和职位分组统计人数，附带小计和总计
SELECT dept_id, job_title, COUNT(*) AS cnt
FROM employee
GROUP BY dept_id, job_title WITH ROLLUP;
```

结果中 `job_title` 为 NULL 的行表示该部门的小计行，`dept_id` 和 `job_title` 都为 NULL 的行表示全表总计。

---

### 1.4 窗口函数（重点）

窗口函数是 MySQL 8.0 引入的杀手级特性，它能在不改变行数的情况下对每行数据执行聚合或排序计算。相比于 GROUP BY 会压缩行数，窗口函数保留所有原始行。

#### 基本语法

```sql
<窗口函数>() OVER (
    PARTITION BY <分组列>
    ORDER BY <排序列>
    [ROWS | RANGE BETWEEN <帧开始> AND <帧结束>]
)
```

#### ROW_NUMBER / RANK / DENSE_RANK

这三兄弟用于组内排序编号，区别在于处理并列值的方式：

```sql
-- 每个部门按工资降序排名
SELECT emp_name, dept_id, salary,
       ROW_NUMBER() OVER (PARTITION BY dept_id ORDER BY salary DESC) AS rn,
       RANK()       OVER (PARTITION BY dept_id ORDER BY salary DESC) AS rk,
       DENSE_RANK() OVER (PARTITION BY dept_id ORDER BY salary DESC) AS dr
FROM employee;
```

| 场景             | ROW_NUMBER | RANK | DENSE_RANK |
| ---------------- | ---------- | ---- | ---------- |
| 工资 100, 100, 90 | 1, 2, 3    | 1, 1, 3 | 1, 1, 2    |
| 是否有间隔        | —          | 有    | 无         |

**经典应用：分组 Top-N**

```sql
-- 每个部门工资前 3 名
SELECT * FROM (
    SELECT *,
           ROW_NUMBER() OVER (PARTITION BY dept_id ORDER BY salary DESC) AS rn
    FROM employee
) t
WHERE rn <= 3;
```

#### LAG / LEAD（前后行访问）

LAG 访问前 N 行，LEAD 访问后 N 行，常用于计算差值或同比环比。

```sql
-- 计算每位员工与其同部门前一名员工的工资差
SELECT emp_name, dept_id, salary,
       LAG(salary, 1) OVER (PARTITION BY dept_id ORDER BY salary) AS prev_salary,
       salary - LAG(salary, 1) OVER (PARTITION BY dept_id ORDER BY salary) AS diff
FROM employee;
```

#### 聚合窗口函数与窗口帧

SUM/AVG/COUNT 等聚合函数配合 OVER 子句可以实现累计计算。

```sql
-- 按日期累计销售额
SELECT sale_date, amount,
       SUM(amount) OVER (ORDER BY sale_date) AS running_total
FROM sales;
```

**ROWS vs RANGE**：

```sql
-- ROWS BETWEEN: 按行数滑动
SUM(amount) OVER (ORDER BY sale_date ROWS BETWEEN 2 PRECEDING AND CURRENT ROW)

-- RANGE BETWEEN: 按值范围滑动（相同值纳入同一帧）
SUM(amount) OVER (ORDER BY sale_date RANGE BETWEEN INTERVAL 7 DAY PRECEDING AND CURRENT ROW)
```

#### 窗口函数的执行顺序

窗口函数在 SQL 执行流程中的位置很特殊：它在 **WHERE、GROUP BY、HAVING 之后**，在 **ORDER BY 之前**执行。这意味着你不能在 WHERE 中直接引用窗口函数。

```sql
-- 错误写法：WHERE 中不能使用窗口函数
SELECT * FROM (
    SELECT *, ROW_NUMBER() OVER (ORDER BY salary DESC) AS rn
    FROM employee
) t
WHERE rn <= 10;  -- 需要嵌套子查询
```

完整的 SQL 执行顺序为：

```
FROM -> JOIN -> WHERE -> GROUP BY -> HAVING -> WINDOW -> SELECT -> DISTINCT -> UNION -> ORDER BY -> LIMIT
```

---

### 1.5 UNION vs UNION ALL

两者都用于合并多个查询结果集，区别在于：

- **UNION**：合并后自动去重，会多一次排序/去重的开销。
- **UNION ALL**：直接合并，保留所有行，性能更好。

```sql
-- 合并两个表的数据（去重）
SELECT name FROM customers
UNION
SELECT name FROM employees;

-- 合并两个表的数据（不去重，性能更优）
SELECT name FROM customers
UNION ALL
SELECT name FROM employees;
```

**实践原则**：明确不需要去重时，一律用 UNION ALL。如果数据本身没有重复，UNION 不仅白消耗性能，还可能导致意外的排序。

---

### 1.6 CTE（公用表表达式）与递归 CTE

CTE 用 WITH 子句定义临时命名结果集，可以看作只在当前 SQL 中存在的临时视图，极大提升复杂查询的可读性。

```sql
-- 基本 CTE
WITH dept_stats AS (
    SELECT dept_id, COUNT(*) AS cnt, AVG(salary) AS avg_sal
    FROM employee
    GROUP BY dept_id
)
SELECT d.dept_name, s.cnt, s.avg_sal
FROM department d
JOIN dept_stats s ON d.dept_id = s.dept_id;
```

#### 递归 CTE（处理树形结构）

递归 CTE 是 MySQL 8.0 的另一大杀器，专门用于处理树状数据（组织架构、分类层级、评论回复等）。

```sql
-- 递归查询所有下级部门（包含根部门）
WITH RECURSIVE dept_tree AS (
    -- 锚点成员：根部门
    SELECT dept_id, dept_name, parent_id, 1 AS level
    FROM department
    WHERE parent_id IS NULL

    UNION ALL

    -- 递归成员：查询子部门
    SELECT d.dept_id, d.dept_name, d.parent_id, t.level + 1
    FROM department d
    JOIN dept_tree t ON d.parent_id = t.dept_id
)
SELECT * FROM dept_tree ORDER BY level, dept_id;
```

递归 CTE 的关键点：
1. **锚点成员**：递归的起点，只执行一次。
2. **递归成员**：引用 CTE 自身，不断迭代直到没有新行产生。
3. 递归深度由 `cte_max_recursion_depth` 参数控制（默认 1000）。

---

## 2. 事务与锁

### 2.1 ACID 属性与实现原理

ACID 是数据库事务的四大特性，理解它们的底层实现是 MySQL 进阶的分水岭。

| 特性 | 含义 | 实现机制 |
|------|------|----------|
| **Atomicity（原子性）** | 事务要么全部成功，要么全部回滚 | **undo log**：记录数据修改前的旧值，回滚时用 undo log 恢复 |
| **Consistency（一致性）** | 事务前后数据完整性约束不被破坏 | 应用层 + 数据库约束 + 原子性/隔离性/持久性共同保证 |
| **Isolation（隔离性）** | 并发事务互不干扰 | **MVCC + 锁** |
| **Durability（持久性）** | 事务提交后数据不会丢失 | **redo log**（WAL 机制）：先写日志再写磁盘 |

#### Undo Log（保证原子性）

当事务修改数据时，InnoDB 会生成 undo log 记录修改前的数据状态。如果事务需要回滚，就从 undo log 中读取旧值恢复。undo log 同时也服务于 MVCC 的快照读。

#### Redo Log（保证持久性）

InnoDB 采用 **WAL（Write-Ahead Logging）** 策略：数据修改首先写入 redo log（顺序写，性能高），再异步刷入磁盘（随机写）。即使数据库崩溃，重启时也能通过 redo log 重放恢复已提交事务的数据。

redo log 由两部分组成：内存中的 **redo log buffer** 和磁盘上的 **redo log file**。通过 `innodb_flush_log_at_trx_commit` 参数控制刷盘策略：

- `=1`：每次事务提交都刷盘（最安全，默认值）。
- `=0`：每秒刷盘一次（性能最好，但可能丢失 1 秒数据）。
- `=2`：每次提交写入 OS cache，每秒刷盘（折中方案）。

---

### 2.2 隔离级别

SQL 标准定义了四种隔离级别，MySQL InnoDB 默认使用 **REPEATABLE READ**。

| 隔离级别 | 脏读 | 不可重复读 | 幻读 |
|-----------|------|------------|------|
| READ UNCOMMITTED | 可能 | 可能 | 可能 |
| READ COMMITTED | 不可能 | 可能 | 可能 |
| REPEATABLE READ（MySQL 默认） | 不可能 | 不可能 | 部分解决 |
| SERIALIZABLE | 不可能 | 不可能 | 不可能 |

```sql
-- 查看当前隔离级别
SELECT @@transaction_isolation;

-- 设置隔离级别
SET SESSION TRANSACTION ISOLATION LEVEL READ COMMITTED;
```

---

### 2.3 并发问题详解

#### 脏读（Dirty Read）

事务 A 读取了事务 B **未提交**的数据，然后事务 B 回滚了，A 读到了不存在的数据。

```
事务 A:         事务 B:
                 UPDATE salary SET amount=10000 WHERE id=1
SELECT amount     -- 读到 10000（未提交的数据）
                 ROLLBACK
```

只在 READ UNCOMMITTED 下发生。

#### 不可重复读（Non-Repeatable Read）

事务 A 内两次读取同一行数据，结果不一样（因为事务 B 在这期间提交了修改）。

```
事务 A:         事务 B:
SELECT amount    -- 读到 5000
                 UPDATE salary SET amount=10000 WHERE id=1
                 COMMIT
SELECT amount    -- 读到 10000（两次结果不一致）
```

在 READ COMMITTED 下可能发生，REPEATABLE READ 通过 MVCC 解决。

#### 幻读（Phantom Read）

事务 A 内两次范围查询，第二次查出了第一次没有的行（因为事务 B 插入了新数据）。

```
事务 A:                  事务 B:
SELECT * FROM employee
WHERE dept_id=10         -- 查到 3 行
                         INSERT INTO employee(emp_id,dept_id) VALUES (100,10)
                         COMMIT
SELECT * FROM employee
WHERE dept_id=10         -- 查到 4 行（多了一行"幻影"行）
```

**MySQL 对幻读的处理**：在 REPEATABLE READ 级别下，MVCC 快照读解决了大部分幻读问题（普通 SELECT 看到的是事务开始时的快照）。但对**当前读**（SELECT ... FOR UPDATE / UPDATE / DELETE），仍然可能产生幻读。InnoDB 通过 **Next-Key Lock** 来在特定场景下防止幻读。

---

### 2.4 MVCC 实现原理

MVCC（Multi-Version Concurrency Control，多版本并发控制）是 InnoDB 实现高并发读的关键技术，它在不加锁的情况下让读操作不阻塞写操作。

#### 隐藏列

InnoDB 的聚簇索引行记录中隐藏着三个关键字段：

| 隐藏列 | 作用 |
|--------|------|
| `DB_TRX_ID` | 最后修改该行的事务 ID |
| `DB_ROLL_PTR` | 指向 undo log 中该行旧版本的指针（回滚指针） |
| `DB_ROW_ID` | 行 ID（隐藏自增列，没有主键时 InnoDB 用它生成聚簇索引） |

#### Undo Log 版本链

每次对记录进行修改，InnoDB 都会生成一个 undo log 旧版本快照，通过 `DB_ROLL_PTR` 将这些版本串联成一个**版本链**。版本链的头是当前最新值，链尾是最早的旧值。

#### ReadView（读视图）

当事务执行快照读（普通 SELECT）时，InnoDB 会生成一个 ReadView，它包含以下关键信息：

- `trx_ids`：生成 ReadView 时**活跃**（未提交）的事务 ID 列表。
- `up_limit_id`（`low_trx_id`）：trx_ids 中的最小值。
- `low_limit_id`（`high_trx_id`）：生成 ReadView 时系统尚未分配的下一个事务 ID。

**判断规则**：沿着 undo log 版本链回溯，对每个版本检查其 `DB_TRX_ID`：

1. 如果 `DB_TRX_ID < up_limit_id`：该版本在 ReadView 生成前已提交，可见。
2. 如果 `DB_TRX_ID >= low_limit_id`：该版本在 ReadView 生成后才创建，不可见。
3. 如果 `up_limit_id <= DB_TRX_ID < low_limit_id`：检查是否在 `trx_ids` 列表中：
   - 在列表中：未提交，不可见。
   - 不在列表中：已提交，可见。

#### 快照读 vs 当前读

- **快照读**（Snapshot Read）：普通的 SELECT 语句，不加锁，通过 MVCC 读取历史版本。
- **当前读**（Current Read）：读取记录的最新版本，且对读取的记录加锁。
  - `SELECT ... FOR UPDATE`（行级排他锁）
  - `SELECT ... LOCK IN SHARE MODE`（行级共享锁）—— MySQL 8.0 起推荐使用 `SELECT ... FOR SHARE`
  - `UPDATE`、`DELETE`、`INSERT`

**RR 下的快照读**：只在事务中第一次 SELECT 时生成 ReadView，后续复用，因此可重复读。
**RC 下的快照读**：每次 SELECT 都生成新的 ReadView，因此不可重复读。

---

### 2.5 锁机制

#### 表级锁

| 锁类型 | 说明 |
|--------|------|
| **表读锁（READ）** | 不阻塞其他读，阻塞写。`LOCK TABLES t READ` |
| **表写锁（WRITE）** | 阻塞其他读和写。`LOCK TABLES t WRITE` |
| **MDL（元数据锁）** | 自动加锁，DML 加 MDL 读锁，DDL 加 MDL 写锁。MDL 写锁会阻塞所有 DML |

#### 行级锁（InnoDB）

InnoDB 的行锁是通过**给索引项加锁**实现的，如果没有索引，退化为表锁（极其危险）。

| 行锁类型 | 说明 |
|----------|------|
| **Record Lock** | 单行记录锁，锁住索引记录本身 |
| **Gap Lock** | 间隙锁，锁住记录之间的间隙（两端开区间），防止幻读。**只在 RR 级别存在** |
| **Next-Key Lock** | Record Lock + Gap Lock 的组合，左开右闭区间。例如对 (10, 20] 加锁，既锁住 20 这条记录，也锁住 10 到 20 之间的间隙 |

**Next-Key Lock 解决幻读**：

假设表中有 id = 10, 20, 30 三条记录。事务 A 执行：

```sql
SELECT * FROM t WHERE id > 10 FOR UPDATE;
```

InnoDB 会加 Next-Key Lock，锁住范围 (10, 20]、(20, 30] 以及 (30, +∞) 的间隙。这样事务 B 无法插入 id=15、id=25 等任何在这个范围内的记录，从而防止幻读。

#### 意向锁（Intention Lock）

意向锁是**表级锁**，用于协调表锁和行锁的关系，由 InnoDB 自动管理：

- **意向共享锁（IS）**：事务准备给某些行加共享锁。
- **意向排他锁（IX）**：事务准备给某些行加排他锁。

当要给表加表锁时，MySQL 只需检查表的意向锁是否冲突，而无需遍历每一行判断是否有行锁，极大提升了效率。

```
表锁兼容矩阵：

             | 表读锁 | 表写锁 | IS | IX
-------------|--------|--------|----|------
表读锁       | 兼容   | 冲突   | 兼容| 冲突
表写锁       | 冲突   | 冲突   | 冲突| 冲突
IS           | 兼容   | 冲突   | 兼容| 兼容
IX           | 冲突   | 冲突   | 兼容| 兼容
```

#### 死锁

死锁是指两个或多个事务互相持有对方需要的锁，形成循环等待。

**示例**：

```
事务 A: UPDATE t SET x=1 WHERE id=1;  -- 持有 id=1 的锁
事务 B: UPDATE t SET x=2 WHERE id=2;  -- 持有 id=2 的锁
事务 A: UPDATE t SET x=3 WHERE id=2;  -- 等待事务 B 释放 id=2
事务 B: UPDATE t SET x=4 WHERE id=1;  -- 等待事务 A 释放 id=1 → DEADLOCK
```

**MySQL 处理死锁的策略**：
1. **等待图检测**（默认）：InnoDB 维护一个等待图（wait-for graph），检测到循环等待时，选择一个开销最小的事务回滚（通常回滚持有锁最少的事务）。
2. **锁等待超时**：通过 `innodb_lock_wait_timeout` 参数设置超时时间（默认 50 秒），超时则回滚。

**预防死锁的建议**：
- 所有事务按相同顺序访问表/行。
- 尽量缩短事务长度，不要在事务中做耗时的外部调用。
- 合理使用索引，减少锁范围。

---

## 3. 索引原理

索引是数据库性能优化的核心武器，理解索引的底层数据结构是优化的基础。

### 3.1 数据结构演进

#### 哈希表

- **优点**：等值查询 O(1) 的极致速度。
- **缺点**：不支持范围查询（`>`、`<`、`BETWEEN`）；不支持排序；Hash 冲突降低性能。
- **应用**：InnoDB 的**自适应哈希索引（Adaptive Hash Index）**，由 InnoDB 自动决定为热数据构建，对用户透明。

#### 二叉搜索树

- 每个节点最多两个子节点，左小右大。
- **缺点**：插入有序数据时退化为链表，查询退化为 O(n)。

#### AVL 树 / 红黑树

- AVL 树：严格平衡，左右子树高度差不超过 1。查询快但插入/删除需要频繁旋转。
- 红黑树：近似平衡，插入/删除旋转次数少。Java 的 TreeMap、HashMap 链表转红黑树用的就是它。
- **共同缺点**：数据量大时树太高，即使 logN 的复杂度，在几千万数据下树高也有 20+ 层。每层一次磁盘 IO，20 次 IO 在 MySQL 场景下不可接受。

#### B 树

B 树（Balance Tree）是多叉平衡树，每个节点可以存储多个 key 和多个子指针：

- 每个节点存储多个 key，降低树高。
- 所有节点都存储数据（data）。
- 范围查询需要中序遍历（不同节点间跳跃）。

#### B+Tree（InnoDB 的最终选择）

B+Tree 是 B 树的变体，也是 InnoDB 索引的底层数据结构：

- **非叶子节点只存 key + 指针**，不存数据，因此一页可以存更多 key，树更矮。
- **叶子节点存数据行**（聚簇索引）或主键值（二级索引）。
- **叶子节点用双向链表连接**，支持高效的范围查询和排序。

---

### 3.2 B+Tree 性能优势的量化分析

以 InnoDB 默认页大小 16KB 为例：

- 假设索引 key 为 8 字节（如 BIGINT），指针占 6 字节，每个非叶子节点可存约 `16KB / (8 + 6) = 1170` 个 key。
- 叶子节点存数据：假设一行数据 1KB，每页可存 16 行。
- **3 层 B+Tree**：
  - 第 1 层（根）：1170 个 key → 指向 1170 个第 2 层节点
  - 第 2 层：1170 × 1170 = 1,368,900 个 key → 指向 1,368,900 个叶子节点
  - 第 3 层（叶子）：1,368,900 × 16 行 ≈ **2190 万行**

**结论**：2000 万行数据，只需要 3 次磁盘 IO 就能找到目标数据。这就是 B+Tree 恐怖的性能优势。

---

### 3.3 InnoDB 索引分类

#### 聚簇索引（Clustered Index）

- InnoDB 表**必须有一个聚簇索引**。
- 优先使用主键作为聚簇索引；没有主键则选第一个 UNIQUE 列；再没有则隐式生成 `DB_ROW_ID` 作为聚簇索引。
- **叶子节点存储完整行数据**。
- 主键值变更会导致行迁移，代价很大。

**为什么建议使用自增主键？**

```sql
CREATE TABLE t (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50)
);
```

自增主键插入时，新数据总是追加到 B+Tree 的右侧，**页分裂**次数最少。如果用 UUID 等随机值做主键，新数据会随机插入到已有页中，频繁触发页分裂（将满页拆分为两页），产生大量碎片，插入性能下降。

#### 二级索引（Secondary Index / 辅助索引）

- 叶子节点存的是**主键值**（不是行数据）。
- 通过二级索引查找数据需要**回表**：先走二级索引找到主键值，再到聚簇索引中查找完整行。

```sql
-- name 列上的二级索引
CREATE INDEX idx_name ON employee(name);

-- 查询过程：
-- 1. 通过 idx_name 找到 name='张三' 对应的主键值 id=100
-- 2. 通过聚簇索引 (id=100) 回表查询完整数据
SELECT * FROM employee WHERE name = '张三';
```

#### 覆盖索引（Covering Index）

如果查询所需的所有列都在二级索引中，就不需要回表，EXPLAIN 的 Extra 列会显示 `Using index`。

```sql
-- 创建联合索引
CREATE INDEX idx_name_salary ON employee(name, salary);

-- 查询的 name 和 salary 都在索引中，无需回表
EXPLAIN SELECT name, salary FROM employee WHERE name = '张三';
-- Extra: Using index
```

**覆盖索引是 SQL 优化的最高境界之一**，应尽量让高频查询被索引覆盖。

#### 联合索引（Composite Index）与最左前缀原则

联合索引按定义字段的顺序构建 B+Tree。例如 `(a, b, c)` 索引，相当于按 a 排序，a 相同按 b 排序，b 相同按 c 排序。

```sql
CREATE INDEX idx_a_b_c ON t(a, b, c);
```

**最左前缀原则**：查询条件必须从索引的最左列开始，才能使用索引。

| 查询条件 | 是否使用索引 |
|----------|-------------|
| `WHERE a=1` | 是 |
| `WHERE a=1 AND b=2` | 是 |
| `WHERE a=1 AND b=2 AND c=3` | 是（完全匹配） |
| `WHERE b=2` | **否**（跳过了 a） |
| `WHERE a=1 AND c=3` | 部分使用索引（a 走索引，c 不能走） |
| `WHERE a=1 AND b>2 AND c=3` | a 和 b 使用索引，c 不能（b 是范围查询，阻断后续） |

#### 索引下推（Index Condition Pushdown, ICP）

MySQL 5.6 引入的优化。在没有 ICP 时，存储引擎根据索引找到行后直接回表，再将行返回给 Server 层，由 Server 层判断其他条件。有了 ICP，**可以在存储引擎层面**先过滤索引中包含的其他条件，减少回表次数。

```sql
-- 联合索引 (name, age)
SELECT * FROM employee WHERE name LIKE '张%' AND age = 25;

-- 没有 ICP：找到 name 以 '张' 开头的所有行，全部回表，再筛选 age=25
-- 有 ICP：在索引遍历时就直接判断 age=25，只对匹配的行回表
```

EXPLAIN 的 Extra 列显示 `Using index condition` 表示使用了 ICP。

---

### 3.4 索引设计原则

#### 选择高选择性的列

选择性 = `COUNT(DISTINCT col) / COUNT(*)`，选择性越高，索引效果越好。性别列只有 '男'/'女'，选择性 2/N ≈ 0，索引基本没用；身份证号每条记录唯一，选择性 = 1，是极好的索引候选。

#### 联合索引按区分度排序

把区分度高的列放在最左边。例如 `(status, user_id)` 中，status 可能只有几个值，user_id 几乎唯一。如果查询经常 `WHERE status=1 AND user_id=123`，应该建 `(user_id, status)` 而不是反过来。

#### 避免冗余索引

```sql
-- idx_a 和 idx_a_b 是冗余的（idx_a_b 可以覆盖 idx_a 的用途）
INDEX idx_a (a),
INDEX idx_a_b (a, b)

-- 正确的做法：只保留 idx_a_b，去掉 idx_a

-- 但 idx_a_b 和 idx_b_a 是不同的（最左前缀不同）
INDEX idx_a_b (a, b),   -- 服务于 WHERE a=? 和 WHERE a=? AND b=?
INDEX idx_b_a (b, a)    -- 服务于 WHERE b=?
```

#### 前缀索引

对于长字符串列（如 VARCHAR(255)），可以只索引前 N 个字符以节省空间。

```sql
-- 索引 email 的前 10 个字符
CREATE INDEX idx_email_prefix ON employee(email(10));
```

选择多大的前缀？目标是让前缀的选择性尽量接近完整列的选择性。

```sql
-- 计算不同前缀长度的选择性
SELECT COUNT(DISTINCT LEFT(email, 5)) / COUNT(*) AS sel5,
       COUNT(DISTINCT LEFT(email, 8)) / COUNT(*) AS sel8,
       COUNT(DISTINCT LEFT(email, 10)) / COUNT(*) AS sel10,
       COUNT(DISTINCT email) / COUNT(*) AS sel_full
FROM employee;
```

---

## 4. SQL 优化

### 4.1 EXPLAIN 分析执行计划

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

### 4.2 慢查询日志

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

### 4.3 常见优化策略

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

### 4.4 COUNT 查询优化

```sql
-- COUNT(*) vs COUNT(1) vs COUNT(列)

-- MySQL 对 COUNT(*) 做了特殊优化，选择最短的二级索引来计数
-- 结果：COUNT(*) = COUNT(1) > COUNT(列)
-- COUNT(列) 需要判断列是否为 NULL，因此更慢

-- 极差的写法：COUNT(列) 且列没有索引
SELECT COUNT(name) FROM employee;  -- 需要全表扫描

-- 好的写法
SELECT COUNT(*) FROM employee;  -- 选最小的二级索引扫描
```

---

## 5. 架构与高可用

### 5.1 主从复制

主从复制是 MySQL 高可用架构的基石，几乎所有的高可用方案（MHA、Orchestrator、Group Replication）都建立在复制之上。

#### 复制原理

```
客户端
  │
  ▼
主库（Master）
  │ 写入 binlog（二进制日志）
  ▼
┌──────────────┐
│  binlog      │
│  (磁盘)       │
└──────┬───────┘
       │  IO 线程读取 binlog 并写入从库 relay log
       ▼
从库（Slave）
┌──────────────┐
│  relay log   │
│  (磁盘)       │
└──────┬───────┘
       │  SQL 线程读取 relay log 并回放
       ▼
   从库数据
```

**三个关键线程**：
1. **主库 Dump 线程**：主库读取 binlog 发送给从库。
2. **从库 IO 线程**：从库接收 binlog 并写入 relay log。
3. **从库 SQL 线程**：读取 relay log 并执行 SQL。

#### Binlog 格式

| 格式 | 说明 | 适用场景 |
|------|------|----------|
| **STATEMENT** | 记录原始 SQL 语句 | 日志量小，但非确定性函数（NOW()、UUID()）可能导致主从不一致 |
| **ROW** | 记录每一行数据的变化 | **最安全，MySQL 8.0 默认**，日志量大但有确定性 |
| **MIXED** | MySQL 自动判断：确定性 SQL 用 STATEMENT，否则用 ROW | 折中方案 |

#### 异步复制 vs 半同步复制

- **异步复制（默认）**：主库提交事务后立即返回客户端，不等待从库确认。性能最好，但主库崩溃后可能有数据丢失。
- **半同步复制**：主库提交事务后，需要等待至少一个从库写入 relay log 后才返回客户端。**需要安装插件 `rpl_semi_sync_master`**。牺牲一点性能换取数据安全。

#### 主从延迟原因与处理

常见原因：

1. **主库高并发写入**：从库 SQL 线程是单线程回放（MySQL 5.6 之前），跟不上主库的写入速度。
   - 解决方案：MySQL 5.7+ 的并行复制（`slave_parallel_workers` 设置多个 SQL 线程）。
2. **从库硬件差于主库**：从库的 CPU、磁盘 IO 能力不足。
3. **大事务**：一个大的 DELETE/UPDATE 操作在主库很快完成，但在从库回放需要大量时间。
   - 解决方案：拆分大事务。
4. **从库也在执行查询**：慢查询可能占用从库资源，影响 SQL 线程回放。

**延迟监控**：

```sql
-- 在从库执行，Seconds_Behind_Master 表示延迟秒数
SHOW SLAVE STATUS\G
```

---

### 5.2 读写分离

#### 基本架构

```
客户端
  │
  ├────→ 写操作 → 主库（Master）
  │
  └────→ 读操作 → 从库1（Slave1）
                  从库2（Slave2）
```

#### 实现方案

1. **应用层**：在代码中配置两个数据源，手动区分读写（如 Spring 的 `AbstractRoutingDataSource`）。
2. **中间件层**：通过 ShardingSphere-JDBC 或 ShardingSphere-Proxy 实现透明读写分离。

#### 主从延迟的处理方案

1. **强制主库读**：对于对一致性要求高的数据（如用户下单后立即查询订单），强制查询主库。
2. **延迟时间标记**：记录数据的写入时间，从库在数据不足 1 秒时等待。
3. **缓存辅助**：写入后先写缓存，从库延迟期间从缓存读取。

---

### 5.3 分库分表

#### 垂直拆分

按业务域拆分数据库：

```
原始：一个数据库包含所有表
  用户表、订单表、商品表、支付记录...

拆分后：
  用户库（user_db）：用户表
  订单库（order_db）：订单表、支付记录表
  商品库（product_db）：商品表
```

**优点**：业务清晰，解耦程度高。
**缺点**：跨库 JOIN 变得困难，需要应用层处理或使用分库中间件。

#### 水平拆分

按某列的值将数据分片到多个表/库中，每个表的结构相同。

```sql
-- 按 user_id 取模分片
-- user_0：user_id % 2 = 0
-- user_1：user_id % 2 = 1

-- 或按范围分片
-- user_2024：2024 年注册的用户
-- user_2025：2025 年注册的用户
```

**分片键的选择**：选择最常用的查询条件作为分片键，否则查询会广播到所有分片。

#### ShardingSphere

- **ShardingSphere-JDBC**：轻量级 Java 框架，以 jar 包形式嵌入应用，增强 JDBC 层。
- **ShardingSphere-Proxy**：独立数据库代理服务，对应用透明。

```yaml
# ShardingSphere 读写分离 + 分库分表配置示例
rules:
  - !READWRITE_SPLITTING
    dataSources:
      readwrite_ds:
        writeDataSourceName: master
        readDataSourceNames:
          - slave1
          - slave2
  - !SHARDING
    tables:
      order:
        actualDataNodes: ds_$->{0..1}.order_$->{0..1}
        tableStrategy:
          standard:
            shardingColumn: order_id
            shardingAlgorithmName: order_inline
        keyGenerateStrategy:
          column: order_id
          keyGeneratorName: snowflake
```

---

## 6. SQL 优化速查表

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
| 死锁 | 事务按相同顺序操作 | 缩短事务长度 |

---

## 7. 面试核心问题

### 基础与索引

1. **B+Tree 相比 B 树和红黑树的优势是什么？**
   - B+Tree 非叶子节点不存数据，一页能存更多 key，同数据量下树更矮 → IO 次数更少。
   - B+Tree 叶子节点有双向链表，范围查询和排序效率高。
   - 红黑树是二叉树，树高约为 log₂N（1000 万行数据高达 24 层），B+Tree 同数据量只需 3-4 层。

2. **聚簇索引和二级索引的区别？回表是什么？**
3. **什么是最左前缀原则？联合索引 (a,b,c) 在哪些条件下能完全使用？**
4. **覆盖索引为什么快？Extra 中 Using index 和 Using index condition 的区别？**
5. **为什么建议使用自增主键而不是 UUID？**

### 事务与锁

6. **MVCC 如何实现可重复读？ReadView 的生成时机和判断规则？**
7. **MySQL 默认隔离级别是什么？为什么选 REPEATABLE READ 而不是 READ COMMITTED？**
   - 历史原因：MySQL 的主从复制基于 binlog，早期某些情况下 RR 比 RC 更安全。
   - 实际上现在 RC 在大多数业务场景已经足够，很多大厂默认使用 RC。
8. **Next-Key Lock 如何解决幻读？快照读和当前读在幻读上的表现差异？**
9. **死锁是怎么产生的？MySQL 如何处理死锁？**
10. **Gap Lock 只在什么隔离级别下存在？为什么？**

### SQL 优化

11. **一条 SQL 执行很慢，排查思路是什么？**
    - 先看是否偶尔慢（刷脏页、等待 MDL 锁）还是一直慢。
    - 一直慢：EXPLAIN 分析 type/key/rows/Extra，检查索引使用情况。
    - 考虑数据量是否过大需要分库分表。
12. **EXPLAIN 中 type 列各个值的含义？至少要达到什么级别？**
13. **count(*)、count(1)、count(列) 的区别？**
14. **如何优化大分页查询？**
15. **一条 SQL 使用了索引还是慢，可能的原因有哪些？**
    - 回表次数太多（索引筛选不够）。
    - 使用了非聚簇索引但 selected 列多（大量回表）。
    - 索引列的区分度太低。
    - 数据量太大，索引也扛不住。

### 架构

16. **主从复制的原理是什么？主从延迟的常见原因和解决方案？**
17. **binlog 的三种格式及其优缺点？**
18. **分库分表的难点？跨分片查询、全局 ID 生成、分布式事务怎么处理？**
19. **什么是半同步复制？解决了什么问题？**

---

> **总结**：MySQL 的学习路径建议为：SQL 基础 → 索引原理 → EXPLAIN 优化 → 事务与锁 → 架构高可用。索引是入门核心，MVCC 和锁是进阶分水岭，主从复制和分库分表是架构能力的体现。坚持结合 EXPLAIN 分析每一条慢查询，日积月累就能建立深厚的优化直觉。
