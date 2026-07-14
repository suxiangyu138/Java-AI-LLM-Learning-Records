# 数据库系统概念 —— 事务

> 从 Java 后端开发视角深度剖析事务机制：ACID、隔离级别、MVCC 与分布式事务

---

## 📑 目录

1. [事务基本概念](#1-事务基本概念)
2. [ACID 特性详解](#2-acid-特性详解)
3. [事务状态与生命周期](#3-事务状态与生命周期)
4. [并发事务问题](#4-并发事务问题)
5. [事务隔离级别](#5-事务隔离级别)
6. [事务实现机制](#6-事务实现机制)
7. [Spring 事务管理实践](#7-spring-事务管理实践)
8. [分布式事务](#8-分布式事务)
9. [常见误区与总结](#9-常见误区与总结)

---

## 1. 事务基本概念

> **事务（Transaction）** 是数据库操作的**最小工作单元**，是一系列操作的集合。要么全部执行成功，要么全部不执行。

```
BEGIN TRANSACTION
    ├── 操作1：扣减库存
    ├── 操作2：创建订单
    ├── 操作3：增加积分
    └── COMMIT / ROLLBACK
```

---

## 2. ACID 特性详解

| 特性 | 全称 | 含义 | Java 实践 |
|------|------|------|-----------|
| **A** | Atomicity（原子性） | 操作不可分割 | `@Transactional` 异常自动回滚 |
| **C** | Consistency（一致性） | 事务前后数据完整性不变 | 数据库约束 + 业务校验双保险 |
| **I** | Isolation（隔离性） | 并发事务互不干扰 | MVCC + 锁机制 + 隔离级别控制 |
| **D** | Durability（持久性） | 提交后数据永久生效 | redo log 落盘保障 |

### 2.1 原子性 —— 下单场景

```java
@Transactional(rollbackFor = Exception.class)
public void createOrder(OrderDTO dto) {
    // 以下三个操作必须同时成功或同时失败
    productMapper.decreaseStock(dto.getProductId(), dto.getQuantity());  // 1
    orderMapper.insert(order);                                           // 2
    pointsMapper.addPoints(dto.getUserId(), dto.getPoints());            // 3
}
```

> **实现原理：** undo log 记录回滚信息，`@Transactional` 异常自动触发回滚。

### 2.2 一致性 —— 转账场景

```sql
-- 转账前后总金额不变
UPDATE account SET balance = balance - 100 WHERE id = 1;  -- A 账户 -100
UPDATE account SET balance = balance + 100 WHERE id = 2;  -- B 账户 +100
-- SUM(balance) 保持不变
```

### 2.3 隔离性 —— 并发场景

```
事务A: 读取库存 = 10          事务B: 读取库存 = 10
事务A: 扣减库存 → 9           事务B: 扣减库存 → 9  ← 错误！应该是 8
```

> **解决方案：** 通过 MVCC 和锁机制实现并发隔离。

### 2.4 持久性 —— 崩溃恢复

```
事务 COMMIT → redo log 落盘 → 数据持久化
即使系统崩溃，重启后通过 redo log 恢复已提交数据
```

---

## 3. 事务状态与生命周期

```
                    ┌─────────┐
                    │  活动    │
                    └────┬────┘
                         │
                    ┌────▼────┐
                    │ 部分提交 │
                    └────┬────┘
                         │
              ┌──────────┼──────────┐
              │                     │
         ┌────▼────┐          ┌────▼────┐
         │  失败   │          │  提交   │
         └────┬────┘          └─────────┘
              │
         ┌────▼────┐
         │  中止   │
         └─────────┘
```

| 状态 | 说明 |
|------|------|
| **活动 (Active)** | 事务正在执行中 |
| **部分提交 (Partially Committed)** | 最后一条语句已执行，等待写入磁盘 |
| **失败 (Failed)** | 无法继续正常执行 |
| **中止 (Aborted)** | 事务回滚，数据库恢复到事务开始前状态 |
| **提交 (Committed)** | 事务成功完成，数据永久写入 |

---

## 4. 并发事务问题

| 问题 | 描述 | Java 后端场景 |
|------|------|-------------|
| **脏读 (Dirty Read)** | 读取到未提交事务的数据 | 读到未支付的订单状态，误判为已支付 |
| **不可重复读 (Non-repeatable Read)** | 同一事务内多次读取结果不同 | 查询用户余额两次结果不一致 |
| **幻读 (Phantom Read)** | 同一查询范围出现新增记录 | 统计订单数两次结果不同 |
| **丢失更新 (Lost Update)** | 两个事务同时修改覆盖 | 库存扣减超卖 |

### 4.1 问题示例

```sql
-- 脏读示例
-- 事务A: UPDATE account SET balance = 500 WHERE id = 1;  (未提交)
-- 事务B: SELECT balance FROM account WHERE id = 1;        (读到 500)
-- 事务A: ROLLBACK;  (回滚到 1000)
-- 事务B 读到了不存在的数据！

-- 不可重复读示例
-- 事务A: SELECT balance FROM account WHERE id = 1;  → 1000
-- 事务B: UPDATE account SET balance = 500 WHERE id = 1; COMMIT;
-- 事务A: SELECT balance FROM account WHERE id = 1;  → 500 (不一致！)

-- 幻读示例
-- 事务A: SELECT COUNT(*) FROM order WHERE user_id = 1;  → 5
-- 事务B: INSERT INTO order ...; COMMIT;
-- 事务A: SELECT COUNT(*) FROM order WHERE user_id = 1;  → 6 (多了！)
```

---

## 5. 事务隔离级别

| 隔离级别 | 脏读 | 不可重复读 | 幻读 | 性能 | 适用场景 |
|----------|:----:|:--------:|:----:|:----:|----------|
| **读未提交 (Read Uncommitted)** | ✗ | ✗ | ✗ | 最高 | 几乎不使用 |
| **读已提交 (Read Committed)** | ✓ | ✗ | ✗ | 高 | Oracle 默认，普通查询 |
| **可重复读 (Repeatable Read)** | ✓ | ✓ | ✗* | 中 | **MySQL 默认**，核心业务 |
| **串行化 (Serializable)** | ✓ | ✓ | ✓ | 低 | 金融强一致性场景 |

> \* MySQL InnoDB 的 MVCC + Next-Key Lock 在可重复读级别下也解决了幻读问题。

### 5.1 Spring 中设置隔离级别

```java
@Service
public class OrderService {

    // MySQL 默认：可重复读
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void createOrder(OrderDTO dto) { ... }

    // 金融场景：串行化
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void transfer(Long fromId, Long toId, BigDecimal amount) { ... }

    // 普通查询：读已提交
    @Transactional(isolation = Isolation.READ_COMMITTED, readOnly = true)
    public List<Order> listOrders(Long userId) { ... }
}
```

---

## 6. 事务实现机制

### 6.1 日志系统

| 日志类型 | 作用 | 原理 |
|----------|------|------|
| **redo log** | 保证持久性 | 记录物理修改，崩溃恢复时重做 |
| **undo log** | 保证原子性 | 记录逻辑回滚信息，事务回滚时撤销 |

```
写流程：先写 redo log (WAL) → 再修改数据页 → 事务提交 → redo log 落盘
回滚流程：读取 undo log → 逆向执行 → 恢复到事务前状态
```

### 6.2 MVCC（多版本并发控制）

> MySQL InnoDB 默认实现，**读写不阻塞**，大幅提升并发性能。

```
MVCC 核心组件：
├── 隐藏列：DB_TRX_ID（事务ID）、DB_ROLL_PTR（回滚指针）
├── undo log 版本链：记录历史版本
└── ReadView：快照读判断可见性

读操作 → 快照读（不加锁，读历史版本）
写操作 → 当前读（加锁，读最新版本）
```

### 6.3 封锁机制

| 锁类型 | 符号 | 说明 | 场景 |
|--------|------|------|------|
| **共享锁 (S 锁)** | 读锁 | 多个事务可同时持有，只读 | 查询数据 |
| **排他锁 (X 锁)** | 写锁 | 一个事务独占，可读写 | 修改数据 |
| **意向锁 (IS/IX)** | 表级 | 表示要在行上加 S/X 锁 | 行锁前提 |
| **间隙锁 (Gap Lock)** | 范围 | 锁定索引间隙，防幻读 | 范围查询 |
| **临键锁 (Next-Key Lock)** | 行+间隙 | 行锁 + 间隙锁组合 | InnoDB 默认 |

```sql
-- 排他锁：防止超卖
SELECT stock FROM product WHERE id = 1 FOR UPDATE;
-- 其他事务必须等待此事务提交后才能操作该行
```

---

## 7. Spring 事务管理实践

### 7.1 声明式事务（推荐）

```java
@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private OrderMapper orderMapper;

    @Transactional(
        propagation = Propagation.REQUIRED,      // 传播机制
        isolation = Isolation.REPEATABLE_READ,    // 隔离级别
        timeout = 30,                              // 超时 30s
        rollbackFor = Exception.class              // 异常回滚
    )
    @Override
    public void createOrder(OrderDTO dto) {
        // 业务逻辑
    }
}
```

### 7.2 事务传播机制

| 传播行为 | 说明 | 使用场景 |
|----------|------|----------|
| `REQUIRED` | 有事务则加入，无则新建（**默认**） | 大多数场景 |
| `REQUIRES_NEW` | 总是新建事务，挂起当前事务 | 日志记录（独立提交） |
| `NESTED` | 嵌套事务，可回滚到保存点 | 子操作可独立回滚 |
| `SUPPORTS` | 有事务则加入，无则非事务运行 | 纯查询操作 |
| `MANDATORY` | 必须在事务中运行，否则抛异常 | 强制事务约束 |
| `NOT_SUPPORTED` | 非事务运行，挂起当前事务 | 不需要事务的操作 |
| `NEVER` | 非事务运行，有事务则抛异常 | 禁止事务场景 |

### 7.3 事务失效场景 ⚠️

```java
// ❌ 失效场景 1：非 public 方法
@Transactional
private void innerMethod() { }  // 不生效！

// ❌ 失效场景 2：同类方法调用（this 调用绕过代理）
public void outer() {
    this.inner();  // @Transactional 不生效！
}

// ❌ 失效场景 3：异常被捕获吞掉
@Transactional
public void save() {
    try {
        // ... 数据库操作
    } catch (Exception e) {
        log.error("error", e);  // 吞掉异常，不回滚！
    }
}

// ✅ 正确做法
@Transactional(rollbackFor = Exception.class)
public void save() throws BusinessException {
    // ... 数据库操作
    // 异常向上抛出，触发回滚
}
```

---

## 8. 分布式事务

### 8.1 微服务场景下的分布式事务

```
            ┌─────────────┐
            │  订单服务    │
            └──────┬──────┘
                   │
        ┌──────────┼──────────┐
        │          │          │
   ┌────▼────┐ ┌──▼────┐ ┌───▼────┐
   │ 库存服务 │ │ 支付  │ │ 积分服务│
   └─────────┘ │ 服务  │ └────────┘
               └───────┘
```

### 8.2 解决方案对比

| 方案 | 一致性 | 性能 | 复杂度 | 适用场景 |
|------|:------:|:----:|:------:|----------|
| **2PC (XA)** | 强一致 | 低 | 中 | 传统金融系统 |
| **TCC (Try-Confirm-Cancel)** | 强一致 | 高 | 高 | 高性能金融场景 |
| **SAGA** | 最终一致 | 高 | 中 | 长事务、微服务 |
| **可靠消息** | 最终一致 | 高 | 中 | 电商普通业务 |
| **Seata AT** | 最终一致 | 高 | 低 | 通用微服务场景 |

### 8.3 Seata AT 模式示例

```java
@Service
public class OrderService {

    @GlobalTransactional  // Seata 分布式事务
    @Transactional
    public void createOrderWithPoints(OrderDTO dto) {
        // 订单服务：创建订单
        orderMapper.insert(order);
        // 库存服务：扣减库存 (RPC)
        inventoryClient.decrease(dto.getProductId(), dto.getQuantity());
        // 积分服务：增加积分 (RPC)
        pointsClient.addPoints(dto.getUserId(), dto.getPoints());
    }
}
```

---

## 9. 常见误区与总结

### 9.1 Java 后端事务常见误区

| 误区 | 后果 | 正确做法 |
|------|------|----------|
| 事务传播机制误用 | 嵌套事务回滚异常 | 理解传播行为，按场景选择 |
| 大事务执行 | 锁等待、连接耗尽 | 缩小事务范围，异步化非核心操作 |
| 忽视隔离级别 | 级别过高性能差，过低不安全 | 根据业务需求选择合适的隔离级别 |
| 异常吞没 | 事务不回滚 | 异常向上抛出，设置 `rollbackFor` |
| 分布式事务滥用 | 本地事务用分布式方案 | 能本地解决不要上分布式 |

### 9.2 总结

> **核心认知：** 事务是数据库保障数据安全的核心机制，是业务可靠性的基石。

- ✅ **保障数据一致性** — 解决并发数据错乱，避免业务损失
- ✅ **支撑高并发架构** — MVCC 提升并发性能，适配海量请求
- ✅ **简化业务开发** — 声明式事务降低开发成本
- ✅ **保证系统可靠性** — 崩溃恢复机制，数据不丢失

---

> 📖 **相关阅读：** [数据库-引言](./数据库-引言.md) | [数据库-SQL](./数据库-SQL.md) | [数据库-并发控制](./数据库-并发控制.md) | [数据库-高级事务处理](./数据库-高级事务处理.md) | [数据库-恢复系统](./数据库-恢复系统.md)
