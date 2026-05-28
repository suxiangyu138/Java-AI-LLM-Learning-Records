# MySQL 事务（Java 后端企业级实战版）

> **文档定位**：Java 后端企业级技术文档 | MySQL 事务机制  
> **前置基础**：MySQL 多表操作、视图、用户与权限体系  
> **衔接章节**：MySQL 多表操作 → **本章** → MySQL 索引

---

## 一、核心概念

### 1.1 什么是事务（Java 后端视角）

事务（Transaction）是 MySQL 中一组 **不可分割的 SQL 执行单元**，核心目标是保证数据一致性。它将一组 SQL 语句封装为一个原子操作，**要么全部执行成功（COMMIT），要么全部执行失败（ROLLBACK）**，不会出现"部分成功"的中间状态。

### 1.2 必须使用事务的 3 种场景

| 场景类型 | 典型业务 | 事务必要性 |
|----------|----------|------------|
| **多表关联操作** | 创建订单：新增 `t_order` + 新增 `t_order_detail` | 两操作必须同时成功，否则出现"有订单无详情" |
| **批量操作** | 批量删除用户：删除 `t_user` + 删除 `t_user_role` 关联数据 | 确保所有用户及关联数据完整删除，避免数据冗余 |
| **敏感操作** | 用户充值/商品出库：扣减余额 + 扣减库存 | 扣款和库存扣减必须同时成功，避免财务异常 |

### 1.3 关键前提

**事务仅对 InnoDB 存储引擎有效**。MyISAM 引擎不支持事务，企业级开发中所有核心业务表必须指定 `ENGINE=InnoDB`，否则事务操作静默失效。

---

## 二、底层原理

### 2.1 ACID 四大特性

事务的四大特性（ACID）是事务可靠性的核心保障，也是 Java 后端面试高频考点。

#### 2.1.1 原子性（Atomicity）

- **定义**：事务中的所有 SQL 操作，要么全部执行成功，要么全部执行失败，是一个不可分割的整体
- **实现机制**：通过 Undo Log（回滚日志）实现，执行 SQL 前先记录 Undo Log，回滚时根据 Undo Log 恢复数据
- **实战场景**：创建订单时执行 2 条 SQL，若详情插入失败，原子性保证订单主表的操作被回滚撤销

#### 2.1.2 一致性（Consistency）

- **定义**：事务执行前后，数据库始终处于"合法状态"，符合所有业务规则和完整性约束（主键唯一、非空、外键）
- **实现机制**：由原子性、隔离性、持久性共同保证，是 ACID 的最终目标
- **实战场景**：用户充值场景，若事务失败，余额和库存必须回到操作前状态

#### 2.1.3 隔离性（Isolation）

- **定义**：多个事务并发执行时相互隔离，一个事务的执行不影响其他事务
- **实现机制**：通过锁机制（行锁、间隙锁）和 MVCC（多版本并发控制）实现
- **实战场景**：两个用户同时抢购同一商品，隔离性保证不会出现"库存扣减为负数"

#### 2.1.4 持久性（Durability）

- **定义**：事务一旦提交（COMMIT），数据永久写入磁盘，即使数据库崩溃/重启也不会丢失
- **实现机制**：通过 Redo Log（重做日志）实现，COMMIT 前先将 Redo Log 刷盘
- **实战场景**：用户下单成功（事务提交），即使 MySQL 崩溃，重启后订单数据依然存在

### 2.2 事务的自动提交机制

MySQL 默认开启 **自动提交（`autocommit=1`）**，每条 SQL 语句自动作为一个独立事务执行后立即提交。Java 后端开发中多表操作必须关闭自动提交，手动控制事务边界：

```sql
-- 查看自动提交状态（1=开启，0=关闭）
SELECT @@autocommit;

-- 关闭自动提交（仅当前会话有效）
SET autocommit = 0;

-- 恢复自动提交
SET autocommit = 1;
```

> Spring/MyBatis 框架会自动管理事务：关闭自动提交 → 执行业务逻辑 → 正常则 COMMIT / 异常则 ROLLBACK。

---

## 三、代码实现

### 3.1 事务基本操作

```sql
-- 1. 开启事务（两种语法等价）
START TRANSACTION;
-- 或 BEGIN;

-- 2. 执行一组 SQL 操作（创建订单示例）
INSERT INTO t_order (order_no, user_id, total_price, order_status)
VALUES ('20241001003', 1, 6999.00, 0);

INSERT INTO t_order_detail (order_id, goods_id, goods_name, goods_price, buy_num)
VALUES (3, 2, '华为Mate60', 5999.00, 1);

-- 3. 提交事务（所有 SQL 执行成功，数据永久写入）
COMMIT;

-- 4. 回滚事务（任意一条 SQL 失败时执行，撤销所有操作）
-- ROLLBACK;
```

### 3.2 事务回滚实战

```sql
START TRANSACTION;

-- 新增订单主表（成功）
INSERT INTO t_order (order_no, user_id, total_price, order_status)
VALUES ('20241001004', 1, 7999.00, 0);

-- 新增订单详情（失败：假设 goods_id=100 不存在，外键约束报错）
INSERT INTO t_order_detail (order_id, goods_id, goods_name, goods_price, buy_num)
VALUES (4, 100, '不存在的商品', 1000.00, 1);

-- 第二条 SQL 失败后执行回滚，第一条 SQL 的操作被撤销
ROLLBACK;
-- 结果：t_order 表中不会有 order_no='20241001004' 的订单
```

### 3.3 保存点（SAVEPOINT）—— 部分回滚

复杂事务中可实现部分回滚，适用于多步骤业务场景（订单创建 → 库存扣减 → 日志记录）：

```sql
START TRANSACTION;

-- 步骤 1：新增订单（成功）
INSERT INTO t_order (order_no, user_id, total_price, order_status)
VALUES ('20241001005', 1, 8999.00, 0);
SAVEPOINT order_save;  -- 标记步骤 1 完成

-- 步骤 2：扣减商品库存（成功）
UPDATE t_goods SET stock = stock - 1 WHERE id = 2 AND is_delete = 0;
SAVEPOINT stock_save;  -- 标记步骤 2 完成

-- 步骤 3：记录操作日志（失败，假设表不存在）
INSERT INTO t_operation_log (user_id, operation, create_time)
VALUES (1, '创建订单', NOW());

-- 回滚到 stock_save（仅撤销步骤 3，保留步骤 1、2）
ROLLBACK TO stock_save;

-- 提交事务（确认步骤 1、2）
COMMIT;
```

> 保存点仅在当前事务中有效，事务提交或回滚后自动失效。企业级开发中仅复杂多步骤场景使用。

---

## 四、实战要点

### 4.1 事务隔离级别

Java 后端高并发场景（秒杀、多用户同时下单）中，并发事务会产生数据异常问题。

#### 4.1.1 三种并发问题

| 并发问题 | 定义 | 示例 |
|----------|------|------|
| **脏读** | 读取到另一个事务未提交的数据，若该事务回滚则读到无效数据 | A 充值 100 元（未提交），B 查询看到 100 元，A 回滚后 B 读到脏数据 |
| **不可重复读** | 同一事务内多次读取同一数据，结果不一致（被其他事务修改并提交） | A 查余额 1000，B 扣 500 并提交，A 再查变 500 |
| **幻读** | 同一事务内多次查询同一条件的数据，结果行数不一致（被其他事务新增/删除并提交） | 管理员查 10 个用户，另一个事务新增 1 个并提交，再查变 11 个 |

#### 4.1.2 MySQL 四大隔离级别

| 隔离级别 | 脏读 | 不可重复读 | 幻读 | 企业级使用场景 |
|----------|------|------------|------|----------------|
| **读未提交** (Read Uncommitted) | 否 | 否 | 否 | 极少使用，仅限临时统计等极低一致性场景 |
| **读已提交** (Read Committed) | 是 | 否 | 否 | Oracle 默认级别，适合并发量高、一致性要求基本的场景（普通列表查询） |
| **可重复读** (Repeatable Read) | 是 | 是 | 部分解决 | **MySQL 默认级别**，适合大多数企业级场景（订单、权限管理） |
| **串行化** (Serializable) | 是 | 是 | 是 | 高一致性场景（金融支付、库存扣减），并发性能最差 |

#### 4.1.3 隔离级别设置

```sql
-- 查看全局隔离级别
SELECT @@global.tx_isolation;

-- 查看当前会话隔离级别
SELECT @@tx_isolation;

-- 设置全局隔离级别为可重复读（默认）
SET GLOBAL tx_isolation = 'REPEATABLE-READ';

-- 设置当前会话隔离级别为串行化
SET SESSION tx_isolation = 'SERIALIZABLE';
```

#### 4.1.4 Spring 框架中的隔离级别配置

```java
// 通过 @Transactional 注解设置隔离级别
@Transactional(isolation = Isolation.REPEATABLE_READ)
public void createOrder(OrderDTO orderDTO) {
    // 业务逻辑
}
```

### 4.2 隔离级别选择策略

```
性能：读未提交 > 读已提交 > 可重复读 > 串行化
一致性：串行化 > 可重复读 > 读已提交 > 读未提交

推荐选择：
  普通业务 → 可重复读（MySQL 默认）
  高并发读 → 读已提交
  金融/库存 → 串行化 或 乐观锁/悲观锁
```

---

## 五、避坑总结

### 5.1 事务失效六大陷阱

| 陷阱 | 错误做法 | 正确做法 |
|------|----------|----------|
| **存储引擎错误** | 表使用 MyISAM 引擎，事务操作无效 | 所有核心业务表使用 InnoDB 引擎 |
| **事务范围过大** | 将日志记录、外部接口调用等无关操作纳入事务 | 事务仅包含必须保证一致性的 SQL，无关操作放在事务提交后 |
| **异常被吞掉** | catch 异常后未重新抛出，Spring 无法感知异常触发回滚 | catch 后重新抛出 `RuntimeException`，或指定 `@Transactional(rollbackFor = Exception.class)` |
| **隔离级别不当** | 高并发秒杀场景使用默认隔离级别，导致库存超卖 | 高一致性场景设串行化，或结合乐观锁/悲观锁 |
| **忘记提交/回滚** | 手动 `START TRANSACTION` 后忘记 `COMMIT`/`ROLLBACK`，锁表阻塞 | 手动事务必须在 try-catch 中处理，finally 中确保关闭 |
| **视图参与事务修改** | 将视图的 INSERT/UPDATE 纳入事务（视图是虚拟表） | 事务仅操作底层表，视图仅用于查询 |

### 5.2 Spring 事务回滚关键规则

```java
// ❌ 错误：异常被捕获后未抛出，事务不会回滚
@Transactional
public void createOrder(OrderDTO dto) {
    try {
        orderMapper.insert(order);
        detailMapper.insert(detail);
    } catch (Exception e) {
        log.error("error", e);  // 异常被吞掉，事务正常提交！
    }
}

// ✅ 正确：重新抛出运行时异常，触发回滚
@Transactional(rollbackFor = Exception.class)
public void createOrder(OrderDTO dto) {
    try {
        orderMapper.insert(order);
        detailMapper.insert(detail);
    } catch (Exception e) {
        log.error("创建订单失败", e);
        throw new RuntimeException("创建订单失败", e);  // 触发回滚
    }
}
```

---

## 六、企业级最佳实践

### 6.1 事务使用原则

| 原则 | 说明 |
|------|------|
| **最小范围原则** | 事务只包裹必须保证原子性的数据库操作，RPC 调用、文件 IO、发消息等放事务外 |
| **尽快提交原则** | 事务执行时间越短越好，减少锁持有时间，提升并发性能 |
| **InnoDB 强制原则** | 核心业务表必须使用 InnoDB 引擎，建表语句必须包含 `ENGINE=InnoDB` |
| **声明式事务优先** | 优先使用 Spring `@Transactional` 注解，避免手动管理事务 |

### 6.2 Spring 事务配置模板

```java
/**
 * 订单服务实现类 —— 事务管理企业级范例。
 */
@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderDetailMapper detailMapper;

    /**
     * 创建订单（多表操作，需事务保证原子性）。
     *
     * @param orderDTO 订单数据传输对象
     * @throws RuntimeException 如果创建失败则触发事务回滚
     */
    @Override
    @Transactional(
        rollbackFor = Exception.class,           // 任何异常都回滚
        isolation = Isolation.REPEATABLE_READ,   // 隔离级别
        timeout = 30                             // 超时 30 秒
    )
    public void createOrder(OrderDTO orderDTO) {
        // 1. 新增订单主表
        Order order = buildOrder(orderDTO);
        orderMapper.insert(order);

        // 2. 获取自增 ID 后新增详情
        OrderDetail detail = buildDetail(orderDTO, order.getId());
        detailMapper.insert(detail);

        // 3. 扣减库存（在事务内）
        goodsMapper.decreaseStock(orderDTO.getGoodsId(), orderDTO.getBuyNum());

        // 4. 发送通知（应在事务外，通过事务事件监听器处理）
        // 不要在此处发送消息或调用 RPC
    }
}
```

### 6.3 高并发场景策略

| 场景 | 推荐方案 | 隔离级别 |
|------|----------|----------|
| 普通业务 CRUD | `@Transactional` 默认 | 可重复读 |
| 高并发读多写少 | 读已提交 + 乐观锁 | 读已提交 |
| 秒杀库存扣减 | 悲观锁（`SELECT ... FOR UPDATE`）或 Redis 预减 | 可重复读 |
| 金融账务 | 串行化 + 分布式事务 | 串行化 |

### 6.4 本章实战练习

1. 使用事务完成"创建订单"操作：新增 `t_order` + `t_order_detail`，确保同时成功或失败
2. 模拟事务回滚：新增订单主表成功，详情故意写错 `goods_id`，执行回滚验证
3. 使用保存点完成复杂事务：新增订单 → 扣减库存 → 记录日志，日志失败时仅回滚日志
4. 查看当前隔离级别，设置会话为读已提交，模拟不可重复读问题
5. 使用事务完成"分配用户角色"：新增用户 + 关联用户与角色，确保两操作同时成功或失败
6. 模拟高并发：设置隔离级别为串行化，避免两个事务同时扣减同一商品库存导致负数

### 6.5 本章小结

- 事务核心价值：保证多 SQL 操作的数据一致性，ACID 是事务可靠性的保障
- 核心操作：`START TRANSACTION` → 执行 SQL → `COMMIT` / `ROLLBACK`；保存点用于复杂场景部分回滚
- 隔离级别解决并发问题：MySQL 默认可重复读，高并发选读已提交，高一致性选串行化
- 避坑核心：使用 InnoDB 引擎、控制事务范围、正确处理异常、设置合适的隔离级别
- 后续章节：MySQL 索引（提升事务执行效率）
