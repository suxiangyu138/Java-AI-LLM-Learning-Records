# MySQL 面试宝典
> 基于课程大纲全面覆盖MySQL面试高频考点，从基础语法到InnoDB底层原理，从SQL优化到分库分表运维架构

## 目录
1. [一、基础概念速答](#一基础概念速答)
2. [二、深度原理剖析](#二深度原理剖析)
3. [三、实战场景题](#三实战场景题)
4. [四、手写代码题](#四手写代码题)
5. [五、系统设计题](#五系统设计题)
6. [六、常见坑点与最佳实践](#六常见坑点与最佳实践)
7. [七、面试回答模板](#七面试回答模板)
8. [八、快速查漏补缺Checklist](#八快速查漏补缺checklist)

---

## 一、基础概念速答

### 1. SQL的四大分类是什么？
> DDL（数据定义语言）、DML（数据操作语言）、DQL（数据查询语言）、DCL（数据控制语言）。

- **DDL**：用来定义数据库对象，如创建数据库、表、索引等。关键字包括 `CREATE`、`ALTER`、`DROP`、`TRUNCATE`。
- **DML**：用来对表中的数据进行增删改。关键字包括 `INSERT`、`UPDATE`、`DELETE`。
- **DQL**：用来查询表中的数据。核心关键字是 `SELECT`，常配合 `FROM`、`WHERE`、`GROUP BY`、`HAVING`、`ORDER BY`、`LIMIT` 使用。
- **DCL**：用来管理数据库用户和权限。关键字包括 `GRANT`、`REVOKE`、`CREATE USER`。

> 💡 面试中常问 `TRUNCATE` 属于 DDL 而非 DML，因为它会隐式提交事务且无法回滚。

### 2. DQL 语句的执行顺序是什么？
> SQL 的执行顺序与书写顺序不同，理解执行顺序对优化查询至关重要。

```sql
-- 书写顺序：
SELECT ... FROM ... WHERE ... GROUP BY ... HAVING ... ORDER BY ... LIMIT ...
-- 实际执行顺序：
FROM -> WHERE -> GROUP BY -> HAVING -> SELECT -> ORDER BY -> LIMIT
```

1. **FROM**：确定数据来源表
2. **WHERE**：筛选原始数据行
3. **GROUP BY**：对数据进行分组
4. **HAVING**：对分组后的数据进行过滤
5. **SELECT**：确定最终返回的列
6. **ORDER BY**：对结果排序
7. **LIMIT**：分页截取

> 🎯 理解执行顺序有助于优化：`WHERE` 阶段过滤越多，后续处理的数据量越小。

### 3. MySQL 的常用聚合函数有哪些？
> 聚合函数作用于一组数据并返回单个值，常与 `GROUP BY` 搭配使用。

| 函数 | 作用 | 注意事项 |
|------|------|---------|
| `COUNT()` | 统计行数 | `COUNT(*)` 包括 null 行，`COUNT(列名)` 排除 null |
| `SUM()` | 求和 | 仅适用于数值类型 |
| `AVG()` | 求平均值 | 仅适用于数值类型 |
| `MAX()` | 求最大值 | 可用于数值、字符串、日期 |
| `MIN()` | 求最小值 | 可用于数值、字符串、日期 |

> 💡 `COUNT(*)` 在 InnoDB 中需要扫描全表（没有 MyISAM 那样的独立行数计数器）。

### 4. 内连接、外连接和自连接的区别？
> 多表查询的三种核心连接方式。

- **内连接（INNER JOIN）**：返回两个表中满足连接条件的交集部分。`SELECT * FROM A INNER JOIN B ON A.id = B.aid`
- **左外连接（LEFT JOIN）**：返回左表全部行 + 右表匹配行，右表无匹配则补 NULL。`SELECT * FROM A LEFT JOIN B ON A.id = B.aid`
- **右外连接（RIGHT JOIN）**：返回右表全部行 + 左表匹配行，左表无匹配则补 NULL。`SELECT * FROM A RIGHT JOIN B ON A.id = B.aid`
- **自连接**：同一张表自己连接自己，必须使用表别名。常用于查询层级结构（如员工-经理关系）。
- **联合查询（UNION）**：将多个 SELECT 的结果合并，`UNION` 去重，`UNION ALL` 不去重。要求各 SELECT 的列数和类型一致。

### 5. 什么是子查询？有哪些类型？
> 子查询是嵌套在另一个 SQL 语句中的查询，可分为标量、列、行、表子查询四种。

- **标量子查询**：返回单个值（一行一列），可用在 `WHERE` 比较运算符后。`SELECT * FROM emp WHERE salary > (SELECT AVG(salary) FROM emp)`
- **列子查询**：返回一列多行，搭配 `IN`、`ANY`、`ALL` 使用。`SELECT * FROM emp WHERE dept_id IN (SELECT id FROM dept WHERE name LIKE '%技术%')`
- **行子查询**：返回一行多列。`SELECT * FROM emp WHERE (salary, job) = (SELECT salary, job FROM emp WHERE name = '张三')`
- **表子查询**：返回多行多列，作为临时表用在 `FROM` 子句中。`SELECT * FROM (SELECT * FROM emp WHERE salary > 5000) t`

> 💡 子查询性能通常不如 JOIN，但也更易读。面试中常问子查询与 JOIN 的优劣对比。

### 6. 事务的 ACID 特性是什么？
> 事务是一组不可分割的操作单元，ACID 是其四大核心特性。

| 特性 | 含义 | 实现机制 |
|------|------|---------|
| **A**tomicity（原子性） | 事务要么全部成功，要么全部回滚 | undo log 记录回滚信息 |
| **C**onsistency（一致性） | 事务前后数据完整性约束不被破坏 | 应用层 + 数据库约束共同保证 |
| **I**solation（隔离性） | 并发事务之间相互隔离 | MVCC + 锁机制 |
| **D**urability（持久性） | 事务提交后数据永久保存 | redo log 保证崩溃恢复 |

> 🎯 面试常问：InnoDB 如何通过 redo log + undo log 保证 A 和 D，通过 MVCC 保证 I，最终实现 C。

### 7. MySQL 的四种事务隔离级别？
> 隔离级别从低到高依次是读未提交、读已提交、可重复读、串行化。

| 隔离级别 | 脏读 | 不可重复读 | 幻读 |
|---------|:---:|:--------:|:---:|
| READ UNCOMMITTED | 可能 | 可能 | 可能 |
| READ COMMITTED（Oracle默认） | 避免 | 可能 | 可能 |
| REPEATABLE READ（MySQL默认） | 避免 | 避免 | 可能（InnoDB 通过 gap lock 解决） |
| SERIALIZABLE | 避免 | 避免 | 避免 |

> ⚠️ MySQL InnoDB 在 RR 级别下通过 MVCC + Next-Key Lock 解决了幻读问题，但实际上架级别要求特别严格时仍需 SERIALIZABLE。

### 8. InnoDB 和 MyISAM 的区别？
> 两个最常用的存储引擎，5.5 后 InnoDB 成为默认引擎。

| 对比维度 | InnoDB | MyISAM |
|---------|--------|--------|
| 事务支持 | 支持（ACID） | 不支持 |
| 锁粒度 | 行级锁 | 表级锁 |
| 外键 | 支持 | 不支持 |
| 聚簇索引 | 是（数据即索引） | 否（索引和数据分离） |
| 全文索引 | 5.6 后支持 | 支持 |
| COUNT(*) 速度 | 慢（需扫描） | 快（独立计数器） |
| 崩溃恢复 | 支持（redo log） | 不支持 |
| 适用场景 | 事务型、高并发写 | 只读、OLAP 报表 |

> 💡 面试题："为什么不建议使用 MyISAM？" 核心原因：不支持事务 + 表级锁并发低 + 无崩溃恢复能力。

### 9. MySQL 索引的分类有哪些？
> 按数据结构、物理存储、字段特性等多个维度分类。

- **按数据结构**：B+Tree 索引（默认）、Hash 索引（Memory 引擎支持）、R-Tree（空间索引）、Full-Text（全文索引）
- **按物理存储**：聚簇索引（主键索引，数据即索引）、二级索引（非聚簇索引，叶子存主键值）
- **按字段特性**：主键索引、唯一索引、普通索引、全文索引、复合索引（联合索引）
- **按列数**：单列索引、联合索引

> 🎯 面试常问：InnoDB 为什么必须要有主键？因为聚簇索引依赖主键组织数据。

### 10. 最左前缀法则是什么？
> 联合索引的生效规则——查询从索引的最左列开始，不跳过索引中的列。

```sql
-- 创建联合索引 (a, b, c)
CREATE INDEX idx_a_b_c ON table (a, b, c);

-- 生效的场景：
WHERE a = 1                    -- 用到 a
WHERE a = 1 AND b = 2          -- 用到 a, b
WHERE a = 1 AND b = 2 AND c = 3 -- 用到 a, b, c
WHERE a = 1 AND c = 3          -- 用到 a（c 不生效，因为跳过了 b）

-- 不生效的场景：
WHERE b = 2                    -- 未从最左列开始
WHERE c = 3                    -- 未从最左列开始
```

> ⚠️ MySQL 优化器会做优化，不保证完全遵循书写顺序，但"跳列"一定会导致后续列索引失效。

### 11. 常见的索引失效场景有哪些？
> 索引不是建了就一定生效，以下场景会导致索引失效。

| 场景 | 说明 | 示例 |
|------|------|------|
| 违反最左前缀 | 未从联合索引第一列开始 | `WHERE b = 1` |
| 范围查询右边列失效 | >、<、BETWEEN 右侧的列 | `WHERE a > 1 AND b = 2`（b 失效） |
| 对索引列使用函数 | 函数运算使索引无效 | `WHERE LOWER(name) = 'abc'` |
| 对索引列进行运算 | 算术运算使索引无效 | `WHERE age + 1 = 20` |
| 使用 OR 连接 | OR 两边不全是索引列 | `WHERE id = 1 OR name = 'a'`（name 无索引） |
| LIKE 以 % 开头 | 无法确定匹配前缀 | `WHERE name LIKE '%abc'` |
| 字符串不加引号 | 隐式类型转换 | `WHERE name = 123` |
| 数据分布不均 | 优化器认为全表扫描更快 | 如性别字段（区分度低） |

> 💡 判断索引是否生效：使用 `EXPLAIN` 查看 `key` 字段和 `Extra` 字段。

### 12. 什么是回表查询？什么是覆盖索引？
> 回表和覆盖索引是索引查询中的核心概念，直接影响查询性能。

- **回表查询**：二级索引的叶子节点只存储主键值，当查询的列不在索引中时，需要根据主键回到聚簇索引中再查一次完整行数据，这个过程称为回表。
- **覆盖索引**：索引中包含了查询所需要的一切字段，无需回表。`Extra` 字段显示 `Using index`。

```sql
-- 创建联合索引 (id, name, age)
CREATE INDEX idx_id_name_age ON user (id, name, age);

-- 覆盖索引（不需要回表）
SELECT id, name, age FROM user WHERE id = 1;
-- Extra: Using index

-- 需要回表（查询了 address 字段，不在索引中）
SELECT id, name, address FROM user WHERE id = 1;
-- Extra: NULL（需要回表）
```

> 🎯 覆盖索引是 SQL 优化的核心手段之一，能显著减少磁盘 IO。

### 13. 什么是前缀索引？
> 对字符串类型的列只索引前 N 个字符，以节省索引空间。

```sql
-- 创建前缀索引（取 name 前 5 个字符）
CREATE INDEX idx_name_prefix ON user (name(5));
```

- **优点**：大幅减少索引占用的磁盘空间，提高索引维护效率
- **缺点**：选择性降低（区分度不如全列索引），且无法使用覆盖索引
- **选择依据**：计算不同前缀长度的选择性，`COUNT(DISTINCT LEFT(name, N)) / COUNT(*)` 越接近 1 越好

> 💡 通常前缀长度选到区分度接近完整列即可，一般 5~10 个字符。

### 14. 聚集索引和非聚集索引的区别？
> 区别在于索引中数据的物理存储方式。

- **聚集索引（Clustered Index）**：数据行的物理顺序与索引顺序相同，叶子节点存储完整行数据。InnoDB 主键索引就是聚集索引，一张表只有一个聚集索引。
- **非聚集索引（Non-Clustered Index）**：叶子节点不存储完整数据，而是存储主键值或行指针。InnoDB 的二级索引都是非聚集索引。一张表可以有多个非聚集索引。

> 🎯 面试常问：InnoDB 中为什么建议使用自增主键？因为自增主键插入时页分裂少，而 UUID 主键会导致频繁的页分裂和索引碎片。

### 15. 什么是 MVCC？
> MVCC（Multi-Version Concurrency Control，多版本并发控制）是 InnoDB 实现高并发事务的核心机制。

- **核心思想**：通过维护数据的多个版本，让读操作不阻塞写操作，写操作也不阻塞读操作
- **依赖组件**：隐藏字段（`DB_TRX_ID`、`DB_ROLL_PTR`）、undo log 版本链、ReadView（一致性视图）
- **工作流程**：每行数据有多个历史版本，通过 undo log 串联成版本链。事务执行时生成 ReadView，根据可见性规则判断哪个版本可见
- **可见性规则**：ReadView 中的 `m_ids`（活跃事务列表）决定某版本是否对当前事务可见

> 💡 InnoDB 在 RR 隔离级别下，事务首次执行 SELECT 时生成 ReadView，整个事务期间复用该 ReadView，从而解决了不可重复读。

### 16. 什么是 Next-Key Lock？
> Next-Key Lock 是 InnoDB 行锁与间隙锁的组合，用于解决幻读问题。

- **组成**：行锁（Record Lock）+ 间隙锁（Gap Lock），锁定一个左开右闭区间 `(a, b]`
- **作用**：防止其他事务在锁定的间隙中插入新数据，从而防止幻读
- **触发条件**：在 RR 隔离级别下，使用唯一索引的等值查询且记录不存在时，退化为间隙锁；范围查询时使用 Next-Key Lock

```sql
-- 假设表中有 id: 1, 5, 10
-- 执行以下查询：
SELECT * FROM table WHERE id = 3 FOR UPDATE;
-- 锁定区间 (1, 5]，防止插入 id=3 的新行

SELECT * FROM table WHERE id > 5 FOR UPDATE;
-- 锁定 (5, +∞)
```

> ⚠️ Next-Key Lock 是 RR 级别解决幻读的关键，但也是死锁的常见原因。

### 17. redo log 和 undo log 的作用？
> 两种日志分别保证事务的持久性和原子性。

| 日志 | 作用 | 类型 | 存储位置 | 内容 |
|------|------|------|---------|------|
| **redo log** | 保证持久性（Durability），崩溃恢复 | 物理日志 | 磁盘循环写 | 记录页的修改操作 |
| **undo log** | 保证原子性（Atomicity），事务回滚 | 逻辑日志 | undo 表空间 | 记录数据修改前的版本 |

- **redo log 写入流程**：事务提交时先写 redo log buffer -> 刷入 redo log file（WAL 机制）-> 脏页刷新到磁盘
- **undo log 版本链**：每条记录的修改都会生成 undo log，通过 `DB_ROLL_PTR` 串联形成版本链，用于事务回滚和 MVCC 快照读

> 💡 WAL（Write-Ahead Logging）机制：先写日志再写磁盘，是 MySQL 高性能写入的关键。

### 18. MySQL 的主从复制原理？
> 主库将变更写入 binlog，从库通过 IO 线程和 SQL 线程同步数据。

```
主库 -> binlog -> IO 线程拉取 -> relay log -> SQL 线程回放 -> 从库
```

- **步骤**：
  1. 主库事务提交时将数据变更写入 binlog
  2. 从库通过 IO 线程连接主库，拉取 binlog 写入本地的 relay log
  3. 从库的 SQL 线程读取 relay log 并回放，完成数据同步
- **同步方式**：异步复制（默认）、半同步复制、全同步复制
- **复制类型**：基于语句的复制（SBR）、基于行的复制（RBR）、混合模式（MBR）

> 🎯 面试常问：主从延迟的原因及解决方案。常见原因包括：从库单线程回放慢、大事务、主库写入压力过大。

### 19. 分库分表的拆分方式有哪些？
> 解决单库单表数据量过大问题时的两种拆分策略。

- **垂直拆分**：
  - **垂直分库**：按业务模块拆分到不同数据库（如用户库、订单库、商品库）
  - **垂直分表**：将宽表拆成多张窄表，高频字段放主表，低频/大字段放扩展表
- **水平拆分**：
  - **水平分库**：同一张表按分片键拆分到多个数据库实例
  - **水平分表**：同一张表按分片键拆分到同一库的多张表

```text
分片算法常见：
- 范围分片：按 ID 范围分区（1-1000w, 1001w-2000w）
- 取模分片：ID % 节点数，均匀分布
- 一致性 Hash：减少扩缩容时的数据迁移量
- 枚举分片：按地区、类型等枚举值分片
- 时间分片：按年/月/日分片
```

> ⚠️ 分库分表后带来的问题：跨库 JOIN、分布式事务、全局主键、跨分片排序分页。

### 20. 读写分离的实现方式？
> 主库处理写操作，从库处理读操作，分摊压力。

- **实现方式**：通过 MyCat、ShardingSphere 等中间件或在应用层通过 AbstractRoutingDataSource 动态切换数据源
- **一主一从**：最简单的架构，主库写，从库读
- **双主双从**：两个主库互为主备，各自挂载从库，适合高可用场景
- **注意事项**：主从延迟可能导致读不到刚写入的数据，可通过强制读主库或延迟读解决

---

## 二、深度原理剖析

### 1. InnoDB 的 Buffer Pool 是如何工作的？
> Buffer Pool 是 InnoDB 在内存中的缓存区域，用于缓存表数据和索引数据，极大减少磁盘 IO。

- **组成**：缓存数据页、索引页、undo 页、插入缓冲、自适应哈希索引、锁信息等
- **管理方式**：基于 LRU 算法，但采用改进的 LRU 策略（内存冷热分离），将 LRU 链表分为 young 区和 old 区
- **预读机制**：InnoDB 会预测即将访问的数据页并提前加载到 Buffer Pool 中
- **脏页刷新**：修改过的数据页称为脏页，通过后台线程异步刷盘，由 `innodb_io_capacity` 参数控制刷盘速度

> 💡 Buffer Pool 的大小直接影响 MySQL 性能，建议设置为物理内存的 60%-80%。

### 2. B+Tree 和 B-Tree 的区别？为什么 InnoDB 选择 B+Tree？
> B+Tree 是 B-Tree 的变体，是 InnoDB 索引的底层数据结构。

| 对比项 | B-Tree | B+Tree |
|--------|--------|--------|
| 数据存储 | 非叶子节点也存数据 | 仅叶子节点存数据 |
| 叶子节点结构 | 独立 | 双向链表连接 |
| 查询效率 | 不稳定（可能中间找到） | 稳定（必须到叶子） |
| 范围查询 | 需要中序遍历 | 链表直接遍历，高效 |
| 磁盘 IO 次数 | 较高（每层都可能 IO） | 较低（非叶子存指针更多） |

- **选择原因**：
  1. 非叶子节点不存数据，可容纳更多 key，降低树高（通常 3-4 层），减少磁盘 IO
  2. 叶子节点双向链表，范围查询和排序效率极高
  3. 所有查询都要到叶子节点，查询时间稳定

> 🎯 面试高频追问："为什么 B+Tree 比红黑树更适合做数据库索引？" 因为红黑树是二叉树，树高更高，磁盘寻道次数更多。

### 3. MVCC 的可见性判断规则详述？
> MVCC 通过 ReadView 判断版本链中哪个版本对当前事务可见。

**ReadView 包含四个核心字段**：
- `m_ids`：生成 ReadView 时当前活跃的事务 ID 集合
- `min_trx_id`：`m_ids` 中的最小值
- `max_trx_id`：预分配给下一个事务的 ID
- `creator_trx_id`：创建该 ReadView 的事务 ID

**可见性判断规则**（按 `DB_TRX_ID` 与 ReadView 比较）：

| 条件 | 结论 | 说明 |
|------|:---:|------|
| `trx_id == creator_trx_id` | 可见 | 当前事务修改的版本 |
| `trx_id < min_trx_id` | 可见 | 该版本在 ReadView 生成前已提交 |
| `trx_id >= max_trx_id` | 不可见 | 该版本在 ReadView 生成后启动的事务修改的 |
| `trx_id in m_ids` | 不可见 | 该版本由活跃事务修改，未提交 |
| `trx_id not in m_ids` | 可见 | 该版本事务已提交 |

> 💡 RR 级别下，ReadView 在事务第一次 SELECT 时生成，整个事务期间不变；RC 级别下，每条 SELECT 都生成新的 ReadView。

### 4. InnoDB 行锁有哪些类型？什么情况下会升级为表锁？
> InnoDB 的行锁类型包括行锁、间隙锁、Next-Key 锁。

| 锁类型 | 描述 | 场景 |
|--------|------|------|
| Record Lock | 锁定单条记录 | 唯一索引等值匹配到记录 |
| Gap Lock | 锁定记录之间的间隙 | 唯一索引等值匹配不到记录，或范围查询 |
| Next-Key Lock | Record Lock + Gap Lock，左开右闭 | RR 级别下的默认行锁策略 |
| Insert Intention Lock | 插入意向锁，Gap Lock 的一种 | 向某个间隙插入数据时 |

**行锁升级为表锁的场景**：
- 如果 `WHERE` 条件没有使用索引，InnoDB 无法确定哪些行需要锁定，会从行锁升级为表锁
- 当访问的表数据量非常大且索引选择性差时，MySQL 可能认为表锁比大量行锁更高效
- DDL 操作（`ALTER TABLE` 等）需要表级锁

> ⚠️ 这就是为什么 `UPDATE` 操作一定要用索引条件，否则整个表被锁住，并发性能骤降。

### 5. SQL 优化的一般思路和步骤？
> 定位慢查询、分析原因、针对性优化的系统化方法。

**优化步骤**：
1. **定位慢查询**：开启慢查询日志（`slow_query_log`），设置 `long_query_time` 阈值
2. **分析执行计划**：使用 `EXPLAIN` 分析 SQL，关注 `type`、`key`、`rows`、`Extra` 字段
3. **索引优化**：检查是否全表扫描、是否索引失效、是否可创建覆盖索引
4. **SQL 重写**：拆分复杂 SQL、避免函数运算、优化 JOIN 和子查询
5. **表结构优化**：字段类型优化、拆分大表、冗余字段减少 JOIN
6. **参数调优**：Buffer Pool、连接数、刷盘策略等参数调整

```sql
-- 常用分析命令
EXPLAIN SELECT * FROM user WHERE name = '张三';
SHOW PROFILES;          -- 查看 SQL 执行时间分布
SHOW PROFILE;           -- 查看某条 SQL 各阶段的耗时
```

> 🎯 `EXPLAIN` 的 type 字段排序（从好到差）：`const` > `eq_ref` > `ref` > `range` > `index` > `ALL`。

### 6. ORDER BY 和 GROUP BY 的优化原则？
> 排序和分组操作如果不当会引起严重的性能问题。

**ORDER BY 优化**：
- **Using filesort**：在内存或磁盘中排序，性能差。通过索引排序可避免
- **Using index**：利用 B+Tree 天然有序性，Extra 显示此值为最优
- **优化原则**：`ORDER BY` 的字段顺序要与联合索引的列顺序一致，且排序方向相同（都是 ASC 或 DESC）

```sql
-- 索引 (a, b, c)
ORDER BY a, b, c;       -- Using index
ORDER BY a DESC, b DESC; -- Using index（方向一致）
ORDER BY a, b DESC, c;   -- 可能 filesort（方向不一致）
```

**GROUP BY 优化**：
- `GROUP BY` 本质是先分组后排序，默认也会排序
- 如果不需要排序，可加 `ORDER BY NULL` 避免排序开销
- 尽量使用覆盖索引，避免回表查询

### 7. COUNT(\*) 的优化方法？
> InnoDB 没有像 MyISAM 那样缓存行数，`COUNT(*)` 需要逐行扫描。

- **COUNT(\*) vs COUNT(1) vs COUNT(列名)**：
  - `COUNT(*)` 和 `COUNT(1)` 性能相同，都是统计行数
  - `COUNT(列名)` 统计非空行数，效率低于前两者
- **优化方案**：
  1. **使用近似值**：`SHOW TABLE STATUS LIKE 'table_name'` 的 `Rows` 字段（不精确）
  2. **Redis 缓存计数**：在 Redis 中维护计数器，增删数据时更新
  3. **计数表**：单独一张表记录行数，利用事务保证一致性
  4. **使用覆盖索引**：`COUNT(*)` 会选最小的二级索引扫描，因为二级索引比聚簇索引小

> 💡 InnoDB 会优先选择最小的二级索引进行 `COUNT(*)` 扫描，而不是主键索引。

### 8. LIMIT 深度分页如何优化？
> 传统 `LIMIT M, N` 在 M 很大时性能极差，因为 MySQL 需要扫描前 M+N 行再丢弃前 M 行。

```sql
-- 传统方式（性能差，offset 越大越慢）
SELECT * FROM user LIMIT 100000, 10;

-- 优化方式1：子查询利用覆盖索引
SELECT * FROM user 
WHERE id > (SELECT id FROM user ORDER BY id LIMIT 100000, 1) 
LIMIT 10;

-- 优化方式2：JOIN 方式（利用覆盖索引）
SELECT * FROM user u
INNER JOIN (SELECT id FROM user ORDER BY id LIMIT 100000, 10) AS tmp ON u.id = tmp.id;
```

- **核心思路**：利用覆盖索引快速定位主键，再通过主键回表获取完整数据
- **适用条件**：主键必须有序且连续

### 9. MySQL 的日志系统是如何工作的？
> MySQL 的日志系统包括 binlog（Server 层）、redo log（InnoDB 层）、undo log。

**写入流程**：
```
执行 UPDATE user SET name='new' WHERE id=1;
  -> 先从 Buffer Pool 中读取 id=1 的数据页
  -> 写入 undo log（记录旧值用于回滚）
  -> 在 Buffer Pool 中更新数据页（标记为脏页）
  -> 写入 redo log prepare 状态（两阶段提交的第一阶段）
  -> 写入 binlog（Server 层记录）
  -> 写入 redo log commit 状态（两阶段提交的第二阶段）
  -> 后台线程将脏页刷入磁盘
```

- **两阶段提交**：保证 redo log 和 binlog 的一致性，防止主从复制数据不一致
- **binlog 三种格式**：STATEMENT（记录 SQL）、ROW（记录行变更）、MIXED（混合模式）

> 🎯 面试常问：两阶段提交的目的是解决 redo log 和 binlog 的一致性问题。如果先写 redo log 后写 binlog，崩溃时 binlog 会少记录，导致从库数据不一致。

### 10. 什么是自适应哈希索引（Adaptive Hash Index）？
> InnoDB 根据访问模式自动为热点数据页建立哈希索引，用于加速等值查询。

- **特点**：
  - 完全自动，DBA 无法干预
  - 基于 Buffer Pool 中频繁访问的数据页建立
  - 只支持等值查询（`=` 和 `IN`），不支持范围查询
  - 通过 `innodb_adaptive_hash_index` 参数控制开启/关闭
- **工作原理**：InnoDB 监控索引页的访问模式，如果发现某个索引页频繁以等值查询方式访问，就会在内存中创建一张哈希表
- **潜在问题**：在高并发场景下，AHI 的全局锁（btr_search_latch）可能成为瓶颈

---

## 三、实战场景题

### 1. （阿里）线上一条 SQL 突然变慢，如何排查？
> 诊断步骤涉及多个层面的分析。

**排查步骤**：
1. **确认现象**：是否全表扫描？是否锁等待？是否资源争用？
2. **查看慢查询日志**：确认 SQL 是否被记录，执行时间
3. **使用 EXPLAIN 分析**：查看 type、key、rows、Extra 字段
4. **检查索引状态**：`SHOW INDEX FROM table` 查看索引是否失效
5. **查看锁等待**：`SHOW ENGINE INNODB STATUS` 或 `performance_schema` 检查锁等待
6. **查看系统资源**：CPU、IO、内存是否异常
7. **检查表结构变更**：是否最近有 DDL 操作导致统计信息过期

> 💡 常见原因：索引失效、统计信息过期导致优化器选错索引、Buffer Pool 不足、锁等待。

### 2. （字节）如何设计一个高效的订单号生成器？
> 利用 Redis INCR 或雪花算法生成全局唯一订单号。

**方案一：Redis INCR 生成**

```sql
-- 订单号格式：日期 + 自增序列
-- Redis 命令：INCR order:20260722
-- 从 Redis 获取后拼成字符串 "20260722000001"

-- 对应 MySQL 中的全局唯一 ID
CREATE TABLE `order_id_gen` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `stub` CHAR(1) NOT NULL DEFAULT '',
  PRIMARY KEY (`id`),
  UNIQUE KEY `stub` (`stub`)
) ENGINE=InnoDB;

-- 通过 replace into 获取全局唯一 ID
REPLACE INTO order_id_gen (stub) VALUES ('');
SELECT LAST_INSERT_ID();
```

**方案二：雪花算法（Snowflake）**
- 64 位 long：1位符号位 + 41位时间戳 + 10位机器ID + 12位序列号
- 单机每秒可生成 4096 个 ID，分布式无中心节点
- 时钟回拨问题需要做兜底处理

### 3. （美团）有一个大表需要添加索引，如何避免影响线上业务？
> 大表加索引不能直接 DDL，需要低峰期操作或使用在线 DDL 工具。

**解决方案**：
1. **使用 Online DDL**：MySQL 5.6+ 支持，语法 `ALTER TABLE t ADD INDEX idx_name (col) ALGORITHM=INPLACE, LOCK=NONE`
2. **使用 pt-online-schema-change（Percona Toolkit）**：创建临时表 -> 同步数据 -> 替换表
3. **低峰期执行**：凌晨业务低峰期操作
4. **分片分批**：如果是分库分表场景，逐库逐表操作
5. **备库先加**：先给从库加索引，切换后再给原主库加

```sql
-- 最安全的在线 DDL 语法
ALTER TABLE large_table ADD INDEX idx_col (col) ALGORITHM=INPLACE, LOCK=NONE;
```

> ⚠️ `ALGORITHM=COPY` 和 `LOCK=SHARED` 会锁表，影响线上业务。

### 4. （阿里）如何解决 MySQL 主从延迟问题？
> 主从延迟在高并发写入场景下非常常见，需要多维度解决。

**原因分析**：
- 从库单线程 SQL 回放速度跟不上主库写入速度
- 大事务（如一次 DELETE 大量数据）
- 主库 DDL 操作（表结构变更）
- 从库硬件配置低于主库
- 网络延迟

**解决方案**：
1. **并行复制**：MySQL 5.7 开启 slave_parallel_workers，8.0 支持 Writeset 并行回放
2. **大事务拆分**：将批量操作拆分成多个小事务
3. **强制读主库**：对一致性要求高的场景强制走主库
4. **使用缓存**：Redis 缓存热点数据，减少从库读取压力
5. **半同步复制**：确保至少一个从库同步完成后再提交（`rpl_semi_sync_master_wait_for_slave_count=1`）
6. **提升从库配置**：从库内存、磁盘不低于主库

### 5. （腾讯）索引设计时，如何选择复合索引的字段顺序？
> 字段顺序遵循"区分度优先、查询条件优先"的原则。

**核心原则**：
1. **区分度高的列在前**：筛选掉更多数据的列放前面。`GROUP BY` 和 `ORDER BY` 的列优先
2. **等值条件在前，范围条件在后**：`WHERE a=1 AND b>10` 创建 `(a, b)`，等值列 a 在最左
3. **高频查询的列在前**：最常用的查询条件放最左
4. **排序字段考虑进去**：`ORDER BY` 字段可以包含在联合索引中避免 filesort

```sql
-- 假设高频查询：
SELECT * FROM user WHERE status = 1 AND create_time > '2025-01-01' ORDER BY id;

-- 索引设计：(status, create_time, id)
-- status 等值查询在前
-- create_time 范围查询在中间
-- id 用于排序
-- 合理
```

### 6. 如何高效地批量插入大量数据？
> 批量插入的优化策略。

| 策略 | 说明 | 示例 |
|------|------|------|
| 合并 INSERT | 一条语句插多行 | `INSERT INTO t VALUES (1),(2),(3)` |
| 手动提交事务 | 避免频繁提交 | 每 1000-10000 条提交一次 |
| 主键顺序插入 | B+Tree 无需页分裂 | 自增主键天然有序 |
| 关闭唯一校验 | 临时关闭不必要的约束 | `SET UNIQUE_CHECKS=0` |
| 使用 LOAD DATA | 直接导入文件 | `LOAD DATA LOCAL INFILE 'data.csv' INTO TABLE t` |
| 批量写入工具 | MySQLdump | 导入时使用 `--extended-insert` |

```sql
-- MySQL 批量插入（事务控制）
START TRANSACTION;
INSERT INTO t (id, name) VALUES (1, 'a'), (2, 'b'), (3, 'c'), (4, 'd'), ...
COMMIT;
```

### 7. （美团）乐观锁和悲观锁在秒杀场景中的应用？
> 秒杀场景是典型的高并发写场景，锁策略选择至关重要。

**悲观锁**：
```sql
-- SELECT ... FOR UPDATE 加行锁
BEGIN;
SELECT stock FROM product WHERE id = 1 FOR UPDATE;
-- 检查库存 -> 扣减
UPDATE product SET stock = stock - 1 WHERE id = 1;
COMMIT;
```
- **优点**：强一致性，不会超卖
- **缺点**：并发能力差，容易死锁

**乐观锁（版本号机制）**：
```sql
-- 使用版本号
UPDATE product SET stock = stock - 1, version = version + 1 
WHERE id = 1 AND version = #{oldVersion} AND stock > 0;
```
- **优点**：无锁竞争，并发高
- **缺点**：冲突频繁时重试成本高，适用于"读多写少"的场景

**混合方案**：使用 Redis 做库存扣减前置校验 + MySQL 做最终落盘，Redis 用 Lua 脚本保证原子性。

> 🎯 秒杀场景的最佳实践是"前端限流 + Redis 预扣 + MQ 异步下单 + MySQL 最终一致性"。

### 8. （阿里）如何处理大分页问题（百万级偏移量）？
> 传统 LIMIT 百万级偏移量会扫描并丢弃大量数据，需特殊优化。

**常规方案**：
```sql
-- 优化前（OFFSET 极大时极慢）
SELECT * FROM orders ORDER BY id LIMIT 1000000, 20;

-- 优化方案1：延迟关联（先走覆盖索引，再 JOIN 回表）
SELECT o.* FROM orders o
INNER JOIN (SELECT id FROM orders ORDER BY id LIMIT 1000000, 20) tmp ON o.id = tmp.id;

-- 优化方案2：使用范围查询代替 LIMIT（必须有有序主键）
SELECT * FROM orders WHERE id > 1000000 ORDER BY id LIMIT 20;
```

**核心思路**：利用覆盖索引只扫描主键，再通过主键回表获取完整数据。

### 9. update 操作导致行锁升级为表锁的场景？
> 行锁升级为表锁是 InnoDB 并发性能骤降的常见原因。

**主要原因**：
- `UPDATE` 的 `WHERE` 条件未使用索引（或者索引失效）
- 表关联更新时关联字段没有索引
- 锁定的行数超过某个阈值（如超过表行数的一半），MySQL 优化器认为表锁更优

```sql
-- 行锁（name 有索引）
UPDATE user SET status = 1 WHERE name = '张三';

-- 表锁（name 无索引或索引失效，所有行被锁）
UPDATE user SET status = 1 WHERE name LIKE '%三';
```

> 💡 预防措施：确保 `UPDATE` 和 `DELETE` 的 `WHERE` 条件命中索引，在 WHERE 字段上建立合理索引。

### 10. varchar 和 char 如何选择？
> 两种字符串类型的选择取决于字段数据的长度特征。

| 类型 | 特点 | 适用场景 |
|------|------|---------|
| CHAR(N) | 定长，不足补空格，最大 255 字符 | 固定长度字段：身份证号、手机号、MD5 值 |
| VARCHAR(N) | 变长，额外 1-2 字节记录长度 | 可变长度字段：用户名、地址、备注 |

- **CHAR 优势**：存储空间固定，读写效率略高，无碎片问题
- **VARCHAR 优势**：节省存储空间（对于长度变化大的字段）
- **选择建议**：
  - 长度固定或变化很小，用 CHAR
  - 长度变化大或超过 255 字符，用 VARCHAR
  - 频繁更新的字段，CHAR 减少页分裂

---

## 四、手写代码题

### 1. 创建员工表并添加索引
```sql
CREATE TABLE employee (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    emp_no VARCHAR(20) NOT NULL COMMENT '员工编号',
    name VARCHAR(50) NOT NULL COMMENT '姓名',
    dept_id BIGINT NOT NULL COMMENT '部门ID',
    salary DECIMAL(10,2) NOT NULL COMMENT '薪资',
    hire_date DATE NOT NULL COMMENT '入职日期',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态 1在职 0离职',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_dept_id (dept_id),
    INDEX idx_salary (salary),
    INDEX idx_hire_date (hire_date),
    UNIQUE INDEX idx_emp_no (emp_no),
    INDEX idx_dept_salary (dept_id, salary)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工表';
```

### 2. 查出每个部门薪资最高的员工
```sql
-- 方法一：子查询 + GROUP BY
SELECT e.* FROM employee e
INNER JOIN (
    SELECT dept_id, MAX(salary) AS max_salary
    FROM employee
    WHERE status = 1
    GROUP BY dept_id
) t ON e.dept_id = t.dept_id AND e.salary = t.max_salary;

-- 方法二：窗口函数（MySQL 8.0+）
SELECT * FROM (
    SELECT *,
           RANK() OVER (PARTITION BY dept_id ORDER BY salary DESC) AS rk
    FROM employee WHERE status = 1
) t WHERE rk = 1;
```

### 3. 统计用户最近 30 天的每日活跃数
```sql
SELECT 
    DATE(login_time) AS login_date,
    COUNT(DISTINCT user_id) AS active_users
FROM user_login_log
WHERE login_time >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)
GROUP BY DATE(login_time)
ORDER BY login_date;
```

### 4. 删除重复数据，保留 ID 最小的一条
```sql
-- 假设 email 字段重复
DELETE FROM user 
WHERE id NOT IN (
    SELECT * FROM (
        SELECT MIN(id) FROM user GROUP BY email
    ) AS tmp  -- 避免 MySQL 子查询的歧义
);

-- 方法二：多表删除
DELETE u1 FROM user u1
INNER JOIN user u2 
WHERE u1.email = u2.email AND u1.id > u2.id;
```

### 5. 使用存储过程实现批量插入测试数据
```sql
DELIMITER $$

CREATE PROCEDURE batch_insert(IN total INT)
BEGIN
    DECLARE i INT DEFAULT 1;
    START TRANSACTION;
    WHILE i <= total DO
        INSERT INTO employee (emp_no, name, dept_id, salary, hire_date)
        VALUES (
            CONCAT('EMP', LPAD(i, 6, '0')),
            CONCAT('user_', i),
            FLOOR(1 + RAND() * 5),
            ROUND(5000 + RAND() * 15000, 2),
            DATE_ADD('2020-01-01', INTERVAL FLOOR(RAND() * 2000) DAY)
        );
        IF i % 1000 = 0 THEN
            COMMIT;
            START TRANSACTION;
        END IF;
        SET i = i + 1;
    END WHILE;
    COMMIT;
END$$

DELIMITER ;

CALL batch_insert(100000);
```

### 6. 利用窗口函数计算累计销售额
```sql
-- 计算每个销售员每天的累计销售额
SELECT 
    sales_person,
    sale_date,
    amount,
    SUM(amount) OVER (PARTITION BY sales_person ORDER BY sale_date 
                      ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS cumulative_amount
FROM sales
ORDER BY sales_person, sale_date;
```

### 7. 查出连续登录 3 天及以上的用户
```sql
WITH login_days AS (
    SELECT 
        user_id,
        login_date,
        DATE_SUB(login_date, INTERVAL ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY login_date) DAY) AS grp
    FROM (
        SELECT DISTINCT user_id, DATE(login_time) AS login_date 
        FROM user_login_log
    ) t1
)
SELECT DISTINCT user_id
FROM login_days
GROUP BY user_id, grp
HAVING COUNT(*) >= 3;
```

### 8. 利用触发器实现日志记录
```sql
-- 创建员工更新日志表
CREATE TABLE employee_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    emp_id BIGINT NOT NULL,
    old_salary DECIMAL(10,2),
    new_salary DECIMAL(10,2),
    operator VARCHAR(50),
    operate_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 创建触发器
DELIMITER $$
CREATE TRIGGER trg_employee_update
AFTER UPDATE ON employee
FOR EACH ROW
BEGIN
    IF OLD.salary != NEW.salary THEN
        INSERT INTO employee_log(emp_id, old_salary, new_salary, operator)
        VALUES (NEW.id, OLD.salary, NEW.salary, USER());
    END IF;
END$$
DELIMITER ;
```

---

## 五、系统设计题

### 1. 设计一个支持高并发秒杀系统的数据库架构
> 秒杀系统的核心是"削峰填谷"，数据库层重点解决超卖和性能问题。

**架构设计**：
1. **前端限流**：限流按钮点击、验证码、单用户限购
2. **Redis 预扣库存**：利用 Redis INCR 和 Lua 脚本提前扣减库存，避免直接冲击数据库
3. **MQ 异步下单**：扣减成功后将订单数据发送到消息队列，异步落库
4. **MySQL 库存最终扣减**：消费 MQ 消息，CAS 乐观锁更新库存

```sql
-- 库存扣减 SQL（乐观锁）
UPDATE seckill_stock 
SET stock = stock - 1, version = version + 1 
WHERE product_id = ? AND version = ? AND stock > 0;
```

5. **热点数据隔离**：热度高的商品单独分库或使用缓存
6. **订单表分库分表**：按用户 ID 分片，避免单表过大

### 2. 设计一个电商平台的数据库分库分表方案
> 针对电商业务的特点设计分片方案。

**业务分析**：
- 核心表：用户、商品、订单、库存、支付
- 数据量预估：订单表增长最快，用户表次之

**分片方案**：
- **用户表**：按 user_id 取模分 64 库
- **订单表**：按 buyer_id 取模分 64 库 1024 表（不影响按买家查询），同时按 order_id 建立全局索引
- **商品表**：按 category_id 范围分片或枚举分片
- **库存表**：按 product_id 取模分片

**配套组件**：
- 分布式主键生成器（雪花算法）
- 分库分表中间件（ShardingSphere）
- 分布式事务（Seata AT 模式或 TCC 模式）
- 跨分片查询（汇总中间层）

> 💡 分库分表的核心原则：根据业务查询模式选择分片键，尽量保证单条查询只落一个分片。

### 3. 设计一个读写分离 + 主从切换的高可用方案
> 保障数据库高可用，故障时自动切换。

**架构组件**：
1. **一主两从**：主库负责写，两个从库负责读，从库可互为主备
2. **VIP + Keepalived**：主库挂载虚拟 IP，故障时 VIP 漂移到新主库
3. **中间件层**：ShardingSphere 或 MyCat 读写分离，自动切换
4. **半同步复制**：保证至少一个从库有最新数据
5. **延迟监控**：监控主从延迟，延迟超过阈值自动报警并切到主库读

**故障切换流程**：
```
主库宕机 -> 哨兵/监控发现 -> 从库升级为新主库 -> 
VIP 漂移到新主库 -> 中间件切换数据源 -> 修复旧主库加入从库
```

> 💡 关键指标：RTO 控制在 30 秒内，RPO 控制在 1 秒内。

### 4. 如何在线迁移 MySQL 数据到新集群？
> 在线迁移涉及数据全量同步和增量同步两个阶段。

**步骤**：
1. **搭建新集群**：部署新 MySQL 集群
2. **全量同步**：使用 `mysqldump` 或 `xtrabackup` 全量备份恢复
   - `mysqldump --single-transaction --master-data=2 -A > backup.sql`
3. **增量同步**：建立主从关系，从旧主库同步 binlog 到新集群
4. **数据校验**：使用 `pt-table-checksum` 校验数据一致性
5. **灰度切换**：部分只读流量切到新集群验证
6. **全量切换**：写入流量切换到新集群
7. **回滚预案**：如果新集群有问题，DNS 或 VIP 切回旧集群

### 5. 设计一个通用的慢查询治理平台
> 系统化治理慢查询，保障数据库性能。

**核心功能模块**：
1. **慢查询采集**：开启慢查询日志，实时采集并入库分析
2. **执行计划分析**：自动 `EXPLAIN` 分析，给出索引建议
3. **索引推荐**：基于查询模式推荐创建联合索引
4. **SQL 审核**：上线前自动审核 SQL，拦截大表无索引查询
5. **锁分析**：分析死锁日志，定位锁冲突
6. **容量预测**：基于数据增长趋势预测数据库容量

```sql
-- 慢查询监控 SQL
SELECT * FROM mysql.slow_log ORDER BY query_time DESC LIMIT 10;
SELECT DIGEST_TEXT, SUM_ROWS_EXAMINED, SUM_ROWS_SENT, COUNT_STAR 
FROM performance_schema.events_statements_summary_by_digest
WHERE SUM_ROWS_EXAMINED / SUM_ROWS_SENT > 1000
ORDER BY SUM_ROWS_EXAMINED DESC;
```

---

## 六、常见坑点与最佳实践

### 常见坑点

| 坑点 | 原因 | 解决方案 |
|------|------|---------|
| 隐式类型转换导致索引失效 | 字符串列传入数字值，MySQL 做隐式转换 | 代码中始终保证类型匹配 |
| OR 条件导致全表扫描 | OR 两侧只要有一列无索引就全表扫描 | 用 UNION ALL 替代 OR，或确保所有 OR 条件列都有索引 |
| IN 子查询性能差 | IN 子查询对每一行都执行一次 | 用 JOIN 或 EXISTS 替代（取决于数据量） |
| NOT IN 导致全表扫描 | NOT IN 无法使用索引 | 用 NOT EXISTS 或 LEFT JOIN IS NULL 替代 |
| WHERE 对索引列使用函数 | 函数运算使索引失效 | 创建表达式索引（MySQL 8.0）或在代码层计算好值传入 |
| 大事务导致主从延迟 | 一个大事务生成大量 binlog，从库回放慢 | 大事务分批处理，单事务控制在 1 秒内 |
| 行锁升级为表锁 | 未命中索引的 UPDATE/DELETE | 确保 WHERE 条件使用索引 |
| 分页越深越慢 | OFFSET 需要扫描大量无用行 | 使用延迟关联或范围查询替代 LIMIT OFFSET |
| 自增主键不连续 | 事务回滚、INSERT ON DUPLICATE KEY UPDATE | 业务不依赖 ID 连续性 |

### 最佳实践

| 实践 | 说明 |
|------|------|
| 字段拒绝 NULL | 使用 NOT NULL + DEFAULT，NULL 列难以优化查询且占用额外空间 |
| 最小数据类型原则 | 能用 INT 不用 BIGINT，能用 VARCHAR(50) 不用 VARCHAR(500) |
| 索引不是越多越好 | 索引会降低写入性能，单表建议不超过 5 个索引 |
| 使用覆盖索引查询 | 避免回表，`SELECT *` 改成只查必要字段 |
| 避免大事务 | 单事务操作行数控制在 10000 以内 |
| 用 INNER JOIN 代替子查询 | MySQL 对 JOIN 优化更好（IN 子查询除外） |
| 合理设置 Buffer Pool | 设为物理内存的 70% |
| 定期更新统计信息 | `ANALYZE TABLE t` 让优化器做出正确选择 |
| 使用连接池 | 减少频繁创建/销毁连接的开销 |
| 字段尽量用数字类型 | 数字类型比字符串类型查询更快 |

---

## 七、面试回答模板

### 模板1：谈谈你对 MySQL 索引的理解
> 面试官期望：数据结构 + 实现原理 + 适用场景 + 优化实践

**回答结构**：
1. **索引的定义**：索引是帮助 MySQL 高效获取数据的排好序的数据结构
2. **底层数据结构**：InnoDB 使用 B+Tree，非叶子节点只存 key，叶子节点存完整记录并形成双向链表。优势是树高低（3-4层）、范围查询快
3. **索引分类**：聚簇索引（主键，数据即索引）和二级索引（叶子存主键值），覆盖索引可避免回表
4. **使用规则**：最左前缀法则、索引失效场景（函数、隐式转换、%LIKE）
5. **优化实践**：Explain 分析 type 和 Extra 字段、联合索引设计（区分度高的放前面）、避免 SELECT\*

### 模板2：InnoDB 如何实现事务的隔离性？
> 面试官期望：MVCC + 锁机制完整讲解

**回答结构**：
1. **隔离性的目标**：使并发事务互不干扰
2. **MVCC 机制**：每行记录有隐藏字段（DB_TRX_ID、DB_ROLL_PTR），结合 undo log 版本链和 ReadView 实现快照读。RR 级别下 ReadView 复用
3. **锁机制**：当前读使用 Record Lock、Gap Lock、Next-Key Lock 防止幻读
4. **两者的配合**：普通 SELECT 用 MVCC 快照读（无锁），UPDATE/DELETE/SELECT FOR UPDATE 用当前读（加锁）
5. **最终保障**：四种隔离级别通过 MVCC 和锁的不同组合实现

### 模板3：如何处理缓存一致性问题？
> 面试官期望：MySQL + Redis 联动场景

**回答结构**：
1. **问题背景**：缓存和数据库两套存储系统，数据可能不一致
2. **常见方案**：Cache Aside Pattern（先更新 DB 后删缓存）、延迟双删
3. **最终一致性方案**：通过 Canal 监听 binlog 变更，异步同步到 Redis
4. **强一致性方案**：使用分布式锁或直接读取数据库
5. **实际实践**：业务允许短暂不一致（秒级），采用延迟双删 + TTL 兜底

### 模板4：MySQL 慢查询如何排查和优化？
> 面试官期望：系统化排查思路

**回答结构**：
1. **定位**：开启慢查询日志 -> 设置 long_query_time=1 -> 分析慢查询日志文件
2. **分析**：EXPLAIN 分析执行计划 -> 关注 type（ALL 要优化）、key（是否用索引）、rows（扫描行数）、Extra（filesort/temporary）
3. **优化方向**：是否可加索引？是否索引失效？是否可改 SQL？是否可拆大事务？
4. **表层面**：是否可分区？是否可分表？字段类型是否合理？
5. **架构层面**：是否可加缓存？是否可读写分离？

### 模板5：分库分表后带来的问题和解决方案？
> 面试官期望：对分布式数据库问题的全面认知

**回答结构**：
1. **解决的问题**：单表数据量过大、单库连接数不足、单机磁盘空间不够
2. **引入的问题**：跨库 JOIN 困难、分布式事务、全局主键、跨分片排序分页、数据迁移扩缩容困难
3. **解决方案**：
   - 跨库 JOIN -> 应用层组装或宽表冗余
   - 分布式事务 -> Seata TCC 或 MQ 最终一致性
   - 全局主键 -> 雪花算法
   - 跨分片分页 -> 汇总排序取交集
   - 扩缩容 -> 一致性 Hash 减少迁移量
4. **最佳实践**：能不拆分就不拆分，先用读写分离 + 缓存顶住，真的不行再拆

---

## 八、快速查漏补缺 Checklist

- [ ] SQL 四大分类（DDL/DML/DQL/DCL）及关键字
- [ ] DQL 执行顺序：FROM > WHERE > GROUP BY > HAVING > SELECT > ORDER BY > LIMIT
- [ ] 内连接、外连接、自连接、联合查询的区别语法
- [ ] 子查询四种类型：标量、列、行、表
- [ ] 事务 ACID 特性及实现（redo log/undo log/MVCC）
- [ ] 四种隔离级别及解决的问题
- [ ] InnoDB vs MyISAM 对比
- [ ] B+Tree 结构及为什么选 B+Tree
- [ ] 聚簇索引 vs 二级索引 vs 覆盖索引 vs 回表
- [ ] 最左前缀法则和索引失效场景（至少说出 6 种）
- [ ] 前缀索引选择和区分度计算
- [ ] EXPLAIN 各字段含义（type、key、rows、Extra、key_len）
- [ ] SQL 优化：ORDER BY、GROUP BY、COUNT、LIMIT、INSERT 优化
- [ ] 行锁升级为表锁的场景
- [ ] 行锁类型：Record Lock、Gap Lock、Next-Key Lock、Insert Intention Lock
- [ ] MVCC 原理：隐藏字段、undo log 版本链、ReadView
- [ ] redo log 和 undo log 的作用和写入流程
- [ ] 两阶段提交（redo log prepare -> binlog -> redo log commit）
- [ ] Buffer Pool 工作原理
- [ ] 自适应哈希索引
- [ ] 主从复制原理和延迟解决方案
- [ ] 分库分表拆分方式和分片算法
- [ ] 读写分离实现方式
- [ ] 大事务、大分页、大表 DDL 的优化方法
- [ ] 慢查询排查步骤
- [ ] 乐观锁和悲观锁在秒杀场景的对比
