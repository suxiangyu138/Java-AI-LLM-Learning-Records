# MySQL 面试问答清单
> 🎯 基于项目实战清单，涵盖面试高频问题与完美解答方案，帮助你在面试中脱颖而出。

## 目录
1. [基础概念与核心原理](#1-基础概念与核心原理)
2. [项目实战深度问答](#2-项目实战深度问答)
3. [进阶与系统设计](#3-进阶与系统设计)
4. [场景题与故障排查](#4-场景题与故障排查)

---

## 1. 基础概念与核心原理

> 💡 面试官在这一环节考察你的基本功是否扎实

### Q1：MySQL 的 ACID 特性分别是怎么实现的？事务隔离级别有哪些？
**面试官意图：** 考察对事务核心概念的理解深度，了解你不仅知道"是什么"，还知道"怎么实现的"。

**完美解答：**

**ACID 的实现机制：**

| 特性 | 含义 | 实现机制 |
|------|------|---------|
| **A 原子性（Atomicity）** | 事务要么全部成功，要么全部回滚 | **undo log**：记录修改前的数据，回滚时恢复到修改前 |
| **C 一致性（Consistency）** | 事务前后数据满足所有约束规则 | 由原子性、隔离性、持久性共同保证 + 业务层约束 |
| **I 隔离性（Isolation）** | 并发事务互不干扰 | **锁机制** + **MVCC（多版本并发控制）** |
| **D 持久性（Durability）** | 事务提交后数据永久保存 | **redo log**：先写日志再写磁盘（WAL 技术） |

**四大隔离级别（从低到高）：**

| 级别 | 脏读 | 不可重复读 | 幻读 | 实现方式 |
|------|:---:|:---------:|:---:|---------|
| **READ UNCOMMITTED** | 可能 | 可能 | 可能 | 不加任何锁 |
| **READ COMMITTED** | 否 | 可能 | 可能 | 使用快照读 + 记录锁 |
| **REPEATABLE READ**（MySQL 默认） | 否 | 否 | 可能（InnoDB 可以避免） | MVCC + Next-Key Lock |
| **SERIALIZABLE** | 否 | 否 | 否 | 所有读都加锁 |

> ⚠️ **特别说明**：MySQL 的 RR 级别通过 Next-Key Lock 解决了幻读问题，而其他数据库的 RR 级别不能解决幻读。这是 InnoDB 的重要优势。

**延伸追问应对：** 如果问 MVCC 原理，需说明三个隐藏字段（DB_TRX_ID、DB_ROLL_PTR、DB_ROW_ID）和 undo log 版本链、ReadView 的可见性判断规则。

---

### Q2：MySQL 的索引底层为什么使用 B+ 树而不是 B 树、红黑树或哈希表？
**面试官意图：** 考察对索引数据结构的理解深度，看你是否真正理解 B+ 树的优势。

**完美解答：**

| 数据结构 | 为什么不选？/ 优势对比 |
|----------|---------------------|
| **哈希表** | 不支持范围查询（`>`、`<`、`BETWEEN`），不支持排序，不支持最左前缀匹配 |
| **红黑树** | 树高随数据量快速增长（log2N），MySQL 千万级数据需要 20+ 层 IO，太慢 |
| **B 树** | 节点中既存数据也存指针，单节点能存的关键字少，树高较高 |
| **B+ 树** | ✅ **最优选择** |

**B+ 树的三大核心优势：**

1. **IO 次数少且稳定**：非叶子节点只存关键值，每个节点可以存上千个关键字，树高通常只有 2-4 层（以主键索引为例，16KB 的页每次 IO 可以加载大量数据），查询 10 条和 1000 万条数据的时间几乎一样。

2. **范围查询高效**：叶子节点通过双向链表串联，一次范围查询找到起始位置后，沿着链表顺序扫描即可，不需要像 B 树那样反复回溯。

3. **数据存储集中**：所有数据只在叶子节点存储，非叶子节点只做路由，缓存利用率更高。

> 💡 **举例**：假设 InnoDB 页大小 16KB，每个 key 8 字节，指针 6 字节，一个非叶子节点可存约 16KB/14B ≈ 1170 个关键字。高度为 3 的 B+ 树可存约 1170 × 1170 × 16 = 2100 万条记录，查询只需要 3 次 IO。

---

### Q3：SQL 执行计划中的 type 字段代表什么？从好到差怎么排序？
**面试官意图：** 考察对 EXPLAIN 执行计划的实际分析能力，这是 SQL 优化的基础。

**完美解答：**

`EXPLAIN` 中的 type 字段表示访问类型，从好到差排序如下：

| 等级 | 类型 | 含义 | 说明 |
|:----:|------|------|------|
| ⭐ | **system** | 表中只有一行数据 | 系统表，几乎遇不到 |
| ⭐ | **const** | 使用主键或唯一索引等值查询 | 只需读取 1 行，速度最快 |
| ⭐ | **eq_ref** | 联表查询时使用主键或唯一索引关联 | 驱动表返回 1 条，被驱动表匹配 1 条 |
| ⭐ | **ref** | 使用普通索引等值查询 | 可能返回多条 |
| ⭐ | **range** | 索引范围扫描 | `>`、`<`、`BETWEEN`、`IN`、`LIKE` |
| ⭐ | **index** | 全索引扫描 | 扫描整个索引树，比全表扫描好一点 |
| ❌ | **ALL** | 全表扫描 | **必须避免**，数据量大时性能灾难 |

**优化目标**：至少达到 `range`，最好达到 `ref`、`eq_ref` 或 `const`。

```sql
-- 示例：看看你的 SQL 是什么级别
EXPLAIN SELECT e.name, d.dept_name 
FROM employees e 
LEFT JOIN departments d ON e.dept_id = d.id 
WHERE e.salary > 10000;
```

> 💡 **判断标准**：出现 `ALL` 或 `index` 时一定要警惕。对于百万级数据量的表，`ALL` 意味着扫描数百万行，通常需要加索引优化。

---

## 2. 项目实战深度问答

> 💡 面试官会深挖你的项目细节，验证你"真的做过"而不是"背过"

### Q4：以电商订单系统为例，你是如何进行数据库设计的？聊聊表结构设计的思路
**面试官意图：** 考察真实的业务表设计能力，从需求分析到字段设计的完整思路。

**完美解答：**

**第一步：业务需求梳理**

电商订单系统的核心实体和关系：
- **用户** ↔ **订单**：一对多
- **订单** ↔ **订单项**：一对多
- **商品** ↔ **订单项**：多对多（通过订单项建立关系）

**第二步：核心表结构**

```sql
-- 1. 用户表
CREATE TABLE `user` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
  `username` VARCHAR(32) NOT NULL COMMENT '用户名',
  `phone` VARCHAR(11) DEFAULT NULL COMMENT '手机号',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `is_deleted` TINYINT DEFAULT 0 COMMENT '软删除标记',
  INDEX idx_phone (`phone`) USING BTREE
) COMMENT '用户表';

-- 2. 订单表（核心，设计时重点考虑查询性能）
CREATE TABLE `order` (
  `id` BIGINT AUTO_INCREMENT COMMENT '自增主键',
  `order_no` VARCHAR(32) NOT NULL COMMENT '订单编号（业务主键）',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `total_amount` DECIMAL(12,2) NOT NULL COMMENT '订单总金额',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态：0待付款/1待发货/2已发货/3已完成/4已取消',
  `pay_time` DATETIME DEFAULT NULL COMMENT '支付时间',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY uk_order_no (`order_no`),
  KEY idx_user_id (`user_id`),
  KEY idx_status_create_time (`status`, `create_time`)
) COMMENT '订单表';

-- 3. 订单项表
CREATE TABLE `order_item` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `order_id` BIGINT NOT NULL,
  `product_id` BIGINT NOT NULL,
  `product_name` VARCHAR(128) NOT NULL COMMENT '快照名称',
  `product_price` DECIMAL(10,2) NOT NULL COMMENT '快照价格',
  `quantity` INT NOT NULL DEFAULT 1,
  KEY idx_order_id (`order_id`)
) COMMENT '订单项表';
```

**第三步：设计亮点说明**

| 设计点 | 为什么这么做 |
|--------|-------------|
| **使用 order_no 作为业务主键 + 唯一索引** | 自增主键对内，order_no 对外（分布式唯一） |
| **金额使用 DECIMAL 而不是 FLOAT** | 避免精度丢失 |
| **增加商品快照字段（product_name/product_price）** | 即使商品后续修改，订单中的价格和名称也不变 |
| **联合索引 idx_status_create_time** | 状态查询和按时间排序的常见场景，避免回表 |
| **软删除字段** | 重要数据不物理删除，保留历史 |

> 🎯 **面试中展示这个设计能证明：** 你不仅有建表能力，还有业务兜底意识（快照）、有性能优化意识（联合索引）、有数据安全意识（软删除、DECIMAL 精度）。

---

### Q5：有一张 500 万数据的订单表，某条订单查询很慢，你怎么调优？
**面试官意图：** 考察 SQL 优化的实际排查能力和方法论。

**完美解答：**

**标准排查流程：**

**第一步：定位慢 SQL**
```sql
-- 开启慢查询日志
SET GLOBAL slow_query_log = ON;
SET GLOBAL long_query_time = 1;  -- 超过 1 秒的 SQL 记录

-- 查看慢查询日志
-- Linux: /var/lib/mysql/{hostname}-slow.log
```

**第二步：分析执行计划**
```sql
EXPLAIN SELECT * FROM order 
WHERE user_id = 12345 
ORDER BY create_time DESC 
LIMIT 10;
```

**第三步：常见问题及优化方案**

| 问题 | 表现 | 优化方案 |
|------|------|---------|
| **索引缺失** | type = ALL 全表扫描 | 根据 WHERE 条件建索引，如 `create index idx_user_id on order(user_id)` |
| **索引未覆盖** | Extra 出现 `Using filesort` | 建联合索引 `idx_user_id_create_time(user_id, create_time)` 覆盖排序 |
| **索引失效** | 建了索引但 type = ALL | 检查是否违反最左前缀、使用了函数或隐式类型转换 |
| **深度分页** | `LIMIT 100000, 10` 越来越慢 | 改用子查询优化或游标分页 |
| **数据量大** | 全表 500 万数据 | 考虑分库分表或历史归档 |

**第四步：实际优化案例**
```sql
-- 优化前（深度分页，越往后越慢）
SELECT * FROM order WHERE user_id = 12345 ORDER BY create_time DESC LIMIT 100000, 10;

-- 优化方案1：游标分页（记住上次最后一条的 ID）
SELECT * FROM order 
WHERE user_id = 12345 AND id < 100000  -- 客户端传最后一条的 ID
ORDER BY create_time DESC LIMIT 10;

-- 优化方案2：子查询优化
SELECT * FROM order 
WHERE id IN (
    SELECT id FROM (SELECT id FROM order 
                    WHERE user_id = 12345 
                    ORDER BY create_time DESC LIMIT 100000, 10) AS tmp
);
```

> 💡 **核心原则**：优化 SQL 的三大方向——**加合适的索引**、**减少扫描行数**、**减少回表次数**。按这个思路排查，90% 的慢查询都能解决。

---

### Q6：你在项目中如何使用事务保证数据一致性？遇到过什么坑？
**面试官意图：** 考察事务在实际业务中的应用经验，而不仅仅是理论。

**完美解答：**

**以图书借阅系统为例：**
```java
@Service
public class BorrowService {
    
    @Autowired
    private BookMapper bookMapper;
    @Autowired
    private BorrowRecordMapper borrowRecordMapper;
    
    @Transactional(rollbackFor = Exception.class)
    public Result borrowBook(Long userId, Long bookId) {
        // 1. 查询图书库存
        Book book = bookMapper.selectByIdForUpdate(bookId); // 悲观锁
        
        // 2. 库存校验
        if (book.getStock() <= 0) {
            return Result.fail("库存不足");
        }
        
        // 3. 扣减库存
        book.setStock(book.getStock() - 1);
        bookMapper.updateById(book);
        
        // 4. 创建借阅记录
        BorrowRecord record = new BorrowRecord();
        record.setUserId(userId);
        record.setBookId(bookId);
        record.setBorrowTime(new Date());
        borrowRecordMapper.insert(record);
        
        // 5. 事务方法执行完自动提交，任一异常全部回滚
        return Result.success();
    }
}
```

**实际中遇到过的坑：**

| 坑 | 原因 | 解决方案 |
|----|------|---------|
| **事务不生效** | `@Transactional` 加到同一个类中的方法调用 | 事务方法必须在不同的类中调用，或自己注入自己 |
| **事务回滚异常** | 默认只回滚 RuntimeException | 添加 `rollbackFor = Exception.class` |
| **事务超时** | 默认超时 30 秒，业务执行慢时抛异常 | `@Transactional(timeout = 60)` 或优化业务逻辑 |
| **大事务问题** | 事务中做了大量操作，长时间占用连接 | 拆分事务，非核心逻辑移到事务外 |
| **事务传播失控** | 内部方法使用了 REQUIRES_NEW 导致事务独立提交 | 根据业务场景选择合适的传播行为 |

> ⚠️ **经典坑**：`@Transactional` 在同一个类中的方法调用 A()→B()，B 有 `@Transactional` 也不生效，因为 AOP 代理捕获不到内部调用。解决方法是将方法拆分到不同 Service 或注入自身 Bean。

---

## 3. 进阶与系统设计

> 💡 这是拉开差距的环节，考察架构思维

### Q7：MySQL 的锁机制是怎样的？行锁、表锁、间隙锁分别什么时候生效？
**面试官意图：** 考察对锁机制的深入理解，判断你是否能解决高并发下的数据竞争问题。

**完美解答：**

**MySQL 的锁分类体系：**

```
MySQL 锁
├── 全局锁：FLUSH TABLES WITH READ LOCK（全库只读）
├── 表级锁
│   ├── 表锁（LOCK TABLES ... READ/WRITE）
│   ├── 元数据锁（MDL，自动加，保护表结构）
│   └── 意向锁（IS/IX，表示事务要加行锁的意图）
└── 行级锁（InnoDB）
    ├── 记录锁（Record Lock）：锁住索引上的某一行
    ├── 间隙锁（Gap Lock）：锁住两个索引之间的间隙
    └── Next-Key Lock：记录锁 + 间隙锁（解决幻读）
```

**锁生效的条件：**

| 场景 | SQL 示例 | 加的锁 | 说明 |
|------|---------|--------|------|
| 主键等值查询命中 | `SELECT * FROM t WHERE id = 10 FOR UPDATE` | 行锁（记录锁） | 只锁定 `id=10` 这行 |
| 普通索引等值查询命中 | `SELECT * FROM t WHERE name = 'Bob' FOR UPDATE` | 行锁 + 间隙锁 | 锁定该索引项前后的间隙 |
| 范围查询 | `SELECT * FROM t WHERE id > 10 FOR UPDATE` | Next-Key Lock | 锁住 10 之后的所有记录和间隙 |
| 查询无结果 | `SELECT * FROM t WHERE id = 100 FOR UPDATE`（100 不存在） | 间隙锁 | 防止幻读 |
| 不使用索引 | `UPDATE t SET name='A' WHERE name LIKE '%B%'` | 表锁（行锁升级） | 索引失效导致行锁升级为表锁 |

**死锁的排查与解决：**
```sql
-- 查看当前锁等待
SHOW ENGINE INNODB STATUS;
-- 查看正在运行的事务
SELECT * FROM information_schema.INNODB_TRX;
-- 查看锁等待
SELECT * FROM information_schema.INNODB_LOCK_WAITS;
```

> ⚠️ **避免死锁的铁律**：多个线程加锁的顺序必须一致。比如线程 A 先锁 user 再锁 order，线程 B 也必须先锁 user 再锁 order，不要反过来。

---

### Q8：如何设计分库分表方案？你的路由策略是什么？
**面试官意图：** 考察大数据量场景下的架构设计能力。

**完美解答：**

**分库分表的时机：** 单表超过 500 万行或数据量超过 200GB，且查询性能已无法通过索引优化解决。

**核心策略对比：**

| 策略 | 说明 | 优点 | 缺点 |
|------|------|------|------|
| **垂直分库** | 按业务拆分，订单库、用户库、商品库分开 | 业务隔离、独立扩展 | 跨库事务麻烦 |
| **垂直分表** | 大表拆分，热字段一张表，冷字段一张表 | 减少单行数据量 | 需要联表查询 |
| **水平分表** | 同一张表按 ID 哈希拆到多张表 | 单表数据量可控 | 跨表查询、排序困难 |
| **水平分库** | 按 ID 哈希拆分到多个数据库实例 | 线性扩展、性能好 | 分布式事务、聚合查询复杂 |

**路由策略选择：**

| 策略 | 算法 | 适用场景 |
|------|------|---------|
| **范围路由** | user_id 1-10000 → 库1，10001-20000 → 库2 | 数据增长可预测，容易扩容 |
| **哈希路由** | `user_id % 库数量` → 目标库 | 数据分布均匀，但扩容困难 |
| **一致性哈希** | 将 hash 值分布到环上 | 扩容时影响范围最小 |

**实战方案（以订单表为例）：**
```java
// 按 user_id 分库 + 按 order_id 分表
// 分库：user_id % 4 → 4 个库
// 分表：order_id % 64 → 每库 64 张表，共计 256 张表

public class ShardingStrategy {
    
    public String getDataSourceKey(Long userId) {
        int dbIndex = (int) (userId % 4);
        return "sharding_db_" + dbIndex;
    }
    
    public String getTableName(Long orderId) {
        int tableIndex = (int) (orderId % 64);
        return "order_" + tableIndex;
    }
    
    public static void main(String[] args) {
        // 推荐使用 ShardingSphere-JDBC 配置
        // 无需手动路由，配置绑定表即可
    }
}
```

> 💡 **重要提醒**：分库分表是最后的方案。先考虑：索引优化 → 读写分离 → 缓存层 → 归档历史数据。90% 的场景在前几步就能解决，不必一上来就分库分表。

---

## 4. 场景题与故障排查

> 💡 考察实际解决问题的能力

### Q9：慢查询日志发现某 SQL 执行了 10 秒，你怎么排查？
**面试官意图：** 考察定位和解决 SQL 性能问题的完整思路。

**完美解答：**

**标准排查过程：**

```sql
-- Step 1: 拿到慢 SQL
SELECT * FROM order_item oi 
LEFT JOIN product p ON oi.product_id = p.id 
WHERE oi.create_time > '2024-01-01' 
ORDER BY oi.create_time DESC 
LIMIT 100;
```

```sql
-- Step 2: EXPLAIN 分析
EXPLAIN SELECT * FROM order_item oi 
LEFT JOIN product p ON oi.product_id = p.id 
WHERE oi.create_time > '2024-01-01' 
ORDER BY oi.create_time DESC 
LIMIT 100;
```

**Step 3: 分析结果**

| 发现的问题 | 检查点 |
|-----------|--------|
| `type: ALL` | 全表扫描，索引缺失 |
| `rows: 5000000` | 扫描了 500 万行 |
| `Extra: Using filesort` | 文件排序，性能差 |
| `Extra: Using where` | 没有用到索引 |

**Step 4: 针对性优化**

```sql
-- 方案1：建联合索引覆盖查询和排序
CREATE INDEX idx_create_time ON order_item(create_time);

-- 方案2：如果查询范围太大，加上限制条件减少数据量
-- 不要一次查半年数据，可以按月分页

-- 方案3：如果必须查大范围数据，改用游标模式
-- 前端使用"加载更多"，传最后一条 create_time
SELECT * FROM order_item 
WHERE create_time > '2024-01-01' AND create_time < '2024-02-01' 
ORDER BY create_time DESC LIMIT 100;
```

**Step 5: 验证优化效果**
```sql
EXPLAIN SELECT * FROM order_item 
WHERE create_time > '2024-01-01' AND create_time < '2024-02-01' 
ORDER BY create_time DESC LIMIT 100;
-- 期望看到：type: range, rows: 大幅减少, Extra: Using index condition
```

---

### Q10：线上发现大量死锁怎么办？分析一下死锁的常见原因
**面试官意图：** 考察对死锁的排查和解决能力。

**完美解答：**

**死锁排查命令：**
```sql
-- 1. 查看最近一次死锁信息
SHOW ENGINE INNODB STATUS\G;
-- 找到 "LATEST DETECTED DEADLOCK" 部分

-- 2. 查看当前线程的事务信息
SELECT * FROM information_schema.INNODB_TRX\G;

-- 3. 查看锁等待情况
SELECT * FROM information_schema.INNODB_LOCK_WAITS;
```

**常见的死锁场景和解决方案：**

**场景1：加锁顺序不一致**
```sql
-- 事务A：先锁id=1，再锁id=2
UPDATE t SET name='A' WHERE id = 1;
UPDATE t SET name='B' WHERE id = 2;

-- 事务B：先锁id=2，再锁id=1（造成死锁）
UPDATE t SET name='C' WHERE id = 2;
UPDATE t SET name='D' WHERE id = 1;
```
> ✅ **解决**：所有事务按相同顺序加锁，如统一从小到大。

**场景2：间隙锁冲突**
```sql
-- 事务A：在范围查询中加了间隙锁
SELECT * FROM t WHERE id BETWEEN 10 AND 20 FOR UPDATE;

-- 事务B：尝试插入 id=15 的记录 → 等待事务A释放间隙锁
INSERT INTO t(id) VALUES(15);

-- 事务A：又尝试插入 id=15 → 等待事务B释放插入意向锁 → 死锁
INSERT INTO t(id) VALUES(15);
```
> ✅ **解决**：使用普通索引代替唯一索引可以减少间隙锁；或者降低隔离级别到 RC。

**场景3：批量更新锁升级**
```sql
-- 事务A：更新大量数据，某些行加了行锁，某些行升级为表锁
UPDATE t SET status=1 WHERE create_time < '2023-01-01';
```
> ✅ **解决**：分批更新，每次只更新少量行；确保`WHERE`条件走索引。

> 💡 **实用建议**：MySQL 会自动检测死锁并回滚其中一个事务（通常回滚事务量小的那个），所以大多数死锁对业务影响有限。但频繁死锁一定要排查代码逻辑。

---

### Q11：如果数据库 CPU 突然飙升到 100%，你怎么办？
**面试官意图：** 考察应急处置能力和排查思路。

**完美解答：**

**应急处置四步法：**

**第一步：快速止血**
```bash
# 查看当前正在执行的 SQL
SHOW FULL PROCESSLIST;

# 看到大量 SELECT 慢查询：杀死慢查询
KILL thread_id;

# 或者批量 kill（慎用）
SELECT CONCAT('KILL ', id, ';') FROM information_schema.PROCESSLIST WHERE COMMAND = 'Query' AND TIME > 30;
```

**第二步：排查根因**

| 可能原因 | 检查方式 |
|----------|---------|
| **慢 SQL 并发** | `SHOW FULL PROCESSLIST` 查看是否有大量耗时长的 SQL |
| **索引失效** | `EXPLAIN` 分析是否有全表扫描 |
| **大量写操作** | 查看 `Innodb_rows_inserted/updated/deleted` 状态 |
| **锁等待** | `SHOW ENGINE INNODB STATUS` 查看是否有锁冲突 |
| **服务器资源不足** | 查看内存、IO、带宽使用情况 |

**第三步：常见解决方案**

| 场景 | 解决 |
|------|------|
| 慢 SQL 导致 | KILL 后，分析慢 SQL，加索引或改写 |
| 大量并发查询 | 加 Redis 缓存层，减少 DB 查询 |
| 锁等待严重 | 检查事务未提交的情况，调优加锁顺序 |
| 分页查询导致 | 改成游标分页，限制单次查询范围 |
| 全表扫描 | 检查 WHERE 条件，建合适的索引 |

**第四步：长期治理**
```bash
# 监控慢查询
SET GLOBAL slow_query_log = ON;
SET GLOBAL long_query_time = 1;

# 配置最大连接数（别等 OOM 了才配置）
SET GLOBAL max_connections = 200;

# 配置超时时间（避免长时间占用连接）
SET GLOBAL wait_timeout = 600;
SET GLOBAL interactive_timeout = 600;
```

> 💡 **核心原则**：紧急状态下先止血（KILL 慢查询），保证系统可用，然后再分析根因。如果频繁 CPU 飙升，建议加慢查询监控和告警机制。

---

### Q12：主从复制的延迟问题怎么解决？
**面试官意图：** 考察对 MySQL 主从架构的理解和实际应用中的延迟治理能力。

**完美解答：**

**主从延迟的常见原因：**

| 原因 | 说明 |
|------|------|
| **主库写入压力大** | 主库 TPS 过高，从库来不及执行 binlog |
| **从库配置低** | 主从机器配置不一致，从库写入慢 |
| **大事务** | 一个大事务的 binlog 需要从库完整执行完 |
| **单线程复制** | 默认情况下从库是单线程串行重放 binlog |
| **从库查询压力大** | 从库既要承担读请求又要同步数据 |

**解决方案：**

**1. 架构层面**
```bash
# 修改从库配置，启用并行复制（MySQL 5.7+）
# slave_parallel_workers = 4
# slave_parallel_type = LOGICAL_CLOCK
```

**2. 代码层面——强制主库读**
```java
// 对一致性要求高的场景，强制读主库
@Transactional(readOnly = true)
public Order getOrderByNo(String orderNo) {
    // 使用主库数据源
    return orderMapper.selectByOrderNo(orderNo);
}

// 或者使用 Hint 机制强制路由到主库
```

**3. 业务层面——缓存做缓冲**
```java
// 下单后立刻写入 Redis，查询时优先查缓存
public Order getLatestOrder(Long orderId) {
    Order order = redisTemplate.opsForValue().get("order:" + orderId);
    if (order != null) return order;
    
    order = orderMapper.selectByPrimaryKey(orderId); // 可能从从库读不到最新
    if (order != null) {
        redisTemplate.opsForValue().set("order:" + orderId, order, 5, TimeUnit.SECONDS);
    }
    return order;
}
```

**4. 终极方案——GTID + 半同步复制**
```bash
# 开启半同步复制：主库等待至少一个从库确认再返回
INSTALL PLUGIN rpl_semi_sync_master SONAME 'semisync_master.so';
INSTALL PLUGIN rpl_semi_sync_slave SONAME 'semisync_slave.so';
SET GLOBAL rpl_semi_sync_master_enabled = 1;
SET GLOBAL rpl_semi_sync_slave_enabled = 1;
```

> 💡 **面试加分**：能说出 5.7 版本的并行复制机制（基于组提交的 binlog 并行回放）和半同步复制，说明你不仅有理论还有实战经验。

---

## 💎 面试加分金句

- "设计表结构时，我始终坚持每个表都有主键、高频查询字段必加索引、金额用 DECIMAL、重要数据用软删除、关联字段名一致这五大原则。"
- "B+ 树的优势不仅在于树高低，更在于叶子节点的双向链表设计让范围查询从 O(N) 变成了磁盘顺序 IO，这是 MySQL 处理大量数据查询的底层保障。"
- "索引不是万能的——冗余索引会增加写入负担，索引失效常发生在函数操作、隐式类型转换和 LIKE 前导通配符这三种情况。"
- "分库分表是架构上最后的武器，我通常会先用缓存 + 读写分离 + 数据归档三板斧解决 90% 的性能问题。"
- "事务隔离级别的选择是一种工程权衡——RC 能避免间隙锁冲突提升并发，但你必须接受不可重复读；RR 更安全但锁开销更大。"

## 📋 高频追问清单

| 追问方向 | 应对策略 |
|----------|----------|
| 自增主键和 UUID 主键怎么选？ | 自增主键 B+ 树插入连续，不产生页分裂；UUID 离散插入导致页分裂频繁 |
| 什么是回表查询？怎么避免？ | 通过非聚簇索引找到主键后再查聚簇索引。建覆盖索引（包括所有查询字段）可避免 |
| MySQL 的行数统计为什么不准确？ | InnoDB 用 MVCC，每一行有多个版本，无法精确统计。MyISAM 才精确计数 |
| UNION 和 UNION ALL 的区别？ | UNION 去重排序，UNION ALL 只做合并；能确定无重复时用 UNION ALL |
| varchar(100) 和 varchar(500) 有区别吗？ | MySQL 在内存中按最大长度分配，大的 varchar 会占用更多内存排序 |
| 什么是索引下推？ | 在索引遍历过程中，直接通过索引过滤 WHERE 条件，减少回表次数 |
| MySQL 为什么要有 Buffer Pool？ | 减少磁盘 IO，热点数据在内存中直接查询 |
| GROUP BY 一定要和聚合函数一起用吗？ | GROUP BY 的本质是分组，聚合函数是放在 SELECT 后的 |

## 🔗 关联知识点

- [Redis必做项目清单-面试问答](#) — MySQL + Redis 缓存一致性方案
- [RabbitMQ必做项目清单-面试问答](#) — 本地消息表保证最终一致性
- [Docker必做项目清单-面试问答](#) — MySQL 容器化主从复制部署
