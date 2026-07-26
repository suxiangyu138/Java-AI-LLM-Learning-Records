# 03 - 分布式事务：从 2PC 到 Saga

> 🎯 分布式事务是微服务架构最头疼的问题 — 从强一致的 2PC 到最终一致的 TCC/Saga，理解每种方案的原理、代价和适用场景，是做微服务架构的必修课

---

## 目录

1. [问题背景](#1-问题背景)
2. [刚性事务：2PC / 3PC / XA](#2-刚性事务2pc--3pc--xa)
3. [柔性事务：TCC](#3-柔性事务tcc)
4. [柔性事务：Saga](#4-柔性事务saga)
5. [可靠消息最终一致性](#5-可靠消息最终一致性)
6. [Seata 框架实战](#6-seata-框架实战)
7. [方案对比与选型](#7-方案对比与选型)

---

## 1. 问题背景

```
单机事务（ACID）：
  BEGIN TX;
  UPDATE account SET balance = balance - 100 WHERE id = 1;
  UPDATE account SET balance = balance + 100 WHERE id = 2;
  COMMIT;
  → 一个数据库，一个事务，ACID 全部满足

分布式事务的挑战：
  订单服务 → 写订单库
  库存服务 → 写库存库     ← 两个独立数据库！
  账户服务 → 写账户库
  → 如何保证三者同时成功或同时失败？
```

| 单机 (ACID) | 分布式 |
|-------------|--------|
| 一个数据库 | 多个数据库（甚至多个机房） |
| 本地事务保证原子性 | 网络不可靠，无法简单原子提交 |
| 锁机制保证隔离性 | 分布式锁代价高 |

---

## 2. 刚性事务：2PC / 3PC / XA

### 2.1 2PC（两阶段提交）

```
Phase 1 — Prepare（准备阶段）：
  协调者 → 所有参与者："准备提交事务 X，你能提交吗？"
  参与者 → 执行事务，锁住资源，但不提交
          → 回复 YES（可提交）或 NO（有问题）

Phase 2 — Commit/Rollback（提交阶段）：
  全部 YES → 协调者发 COMMIT → 参与者提交释放锁
  任一 NO 或超时 → 协调者发 ROLLBACK → 参与者回滚释放锁
```

| 优点 | 缺点 |
|------|------|
| 原理简单，强一致性 | **同步阻塞**：Phase 1 到 Phase 2 之间锁资源不释放 |
| 数据库原生支持（XA） | **单点故障**：协调者宕机，参与者锁着资源傻等 |
| — | **数据不一致**：Phase 2 部分节点收到 COMMIT，部分未收到 |

### 2.2 3PC（三阶段提交）

```
2PC 的改进版：
  CanCommit → 询问是否可以提交（轻量检查，不锁资源）
  PreCommit → 执行事务 + 锁资源（增加超时机制，参与者超时自动提交）
  DoCommit  → 提交

改进：降低了阻塞概率（引入超时机制）
代价：多一轮消息，性能更差
```

### 2.3 XA 协议

> X/Open 的分布式事务标准 — 数据库实现 2PC 的接口规范。

```java
// MySQL XA 事务示例
XA START 'tx1';
UPDATE orders SET status='paid' WHERE id=1001;
XA END 'tx1';
XA PREPARE 'tx1';     // Phase 1
XA COMMIT 'tx1';      // Phase 2
```

> ⚠️ 2PC/3PC/XA 适用于**单体应用跨库**场景，不适合高并发微服务。

---

## 3. 柔性事务：TCC

> 💡 TCC（Try-Confirm-Cancel）是业务层的两阶段提交，无锁高性能。

### 3.1 三个阶段

| 阶段 | 操作 | 示例（扣库存） |
|------|------|---------------|
| **Try** | 预留资源、检查 | 冻结 1 个库存（库存数-1，冻结数+1） |
| **Confirm** | 确认执行 | 扣减冻结库存（冻结数-1） |
| **Cancel** | 取消回滚 | 解冻库存（库存数+1，冻结数-1） |

```java
// TCC 接口示例
public interface InventoryTcc {
    @RequestMapping("/inventory/try")
    boolean tryDeduct(@RequestBody DeductRequest req);    // 冻结

    @RequestMapping("/inventory/confirm")
    boolean confirmDeduct(@RequestBody DeductRequest req); // 确认

    @RequestMapping("/inventory/cancel")
    boolean cancelDeduct(@RequestBody DeductRequest req);  // 取消
}
```

| 优点 | 缺点 |
|------|------|
| 无锁，高性能 | 业务侵入大（每个接口需实现正反操作） |
| 隔离性强（冻结资源） | 开发成本高 |
| 适合高并发核心链路 | Confirm/Cancel 需幂等 |

---

## 4. 柔性事务：Saga

> 💡 Saga 将长事务拆分为多个本地事务，每个本地事务有对应的补偿操作。

### 4.1 两种协调模式

| 模式 | 说明 | 优点 | 缺点 |
|------|------|------|------|
| **编排（Choreography）** | 事件驱动，每个服务自行决定下一步 | 松耦合 | 流程难追踪 |
| **协同（Orchestration）** | 中心化的 Saga 协调器指挥各服务 | 易追踪、逻辑集中 | 协调器单点 |

```text
Saga 示例：下单流程
  1. 订单服务：创建订单（状态=PENDING）
  2. 库存服务：扣减库存           ← 补偿：恢复库存
  3. 账户服务：扣减余额           ← 补偿：退款
  4. 订单服务：订单状态→CONFIRMED

如果步骤 3 失败：
  → 账户服务无需补偿（未扣款成功）
  → 库存服务执行补偿：恢复库存
  → 订单服务执行补偿：订单状态→CANCELLED
```

### 4.2 Saga 的挑战

| 挑战 | 解决方案 |
|------|----------|
| 补偿失败 | 重试 + 人工介入 |
| 隔离性缺失 | 业务层容忍（如超卖后人工退款） |
| 循环依赖 | 设计时避免，Saga 编排器检测 |

---

## 5. 可靠消息最终一致性

> 💡 RocketMQ 事务消息的核心思路：本地事务 + 消息表 + 定时检查。

```text
流程：
1. 生产者发"半消息"（Half Message）到 MQ → MQ 存下但不可见
2. 生产者执行本地事务
3. 成功 → 向 MQ 发送 Commit → MQ 将半消息标记为可消费
   失败 → 向 MQ 发送 Rollback → MQ 删除半消息
4. 如果生产者挂了未 Confirm → MQ 定期回查生产者本地事务状态
5. 消费者消费消息 → 执行本地事务 → 成功 ACK / 失败重试

RocketMQ 事务消息 = 半消息 + 回查机制
```

```java
// RocketMQ 事务消息
TransactionMQProducer producer = new TransactionMQProducer("tx_group");
producer.setTransactionListener(new TransactionListener() {
    @Override
    public LocalTransactionState executeLocalTransaction(Message msg, Object arg) {
        // 执行本地事务
        try { orderService.createOrder(arg); return COMMIT_MESSAGE; }
        catch (Exception e) { return ROLLBACK_MESSAGE; }
    }

    @Override
    public LocalTransactionState checkLocalTransaction(MessageExt msg) {
        // MQ 回查本地事务状态
        return orderService.isOrderExist(msg.getKeys()) ? COMMIT_MESSAGE : ROLLBACK_MESSAGE;
    }
});
```

---

## 6. Seata 框架实战

> Seata（阿里开源）提供 AT / TCC / Saga / XA 四种模式。

### 6.1 AT 模式（⭐ 最常用，自动补偿）

```text
原理：
1. 拦截业务 SQL → 解析 → 生成"前镜像"（修改前数据）
2. 执行业务 SQL
3. 生成"后镜像"（修改后数据）→ 写入 Undo Log
4. 全局提交 → 异步删除 Undo Log
5. 全局回滚 → 根据 Undo Log 回放前镜像
```

```yaml
# Seata AT 模式配置
seata:
  tx-service-group: my_tx_group
  service:
    vgroup-mapping:
      my_tx_group: default
  data-source-proxy-mode: AT    # 自动代理数据源
```

### 6.2 四种模式对比

| 模式 | 一致性 | 性能 | 业务侵入 | 适用场景 |
|------|:---:|:---:|:---:|------|
| **AT** | 弱一致 | ⭐⭐⭐ | 无（自动） | 通用场景，⭐ 首选 |
| **TCC** | 强一致 | ⭐⭐ | 高（三接口） | 核心支付链路 |
| **Saga** | 最终一致 | ⭐⭐⭐ | 中（补偿接口） | 长事务、老系统改造 |
| **XA** | 强一致 | ⭐ | 无 | 单体跨库 |

---

## 7. 方案对比与选型

| 方案 | 一致性 | 性能 | 复杂度 | 锁定资源 | 适用场景 |
|------|:---:|:---:|:---:|:---:|------|
| 2PC/XA | 强 | ⭐ | 低 | ✅ 锁 | 单体应用跨库 |
| TCC | 强 | ⭐⭐⭐ | 高 | ❌ 无锁 | 高并发核心链路 |
| Saga | 最终 | ⭐⭐⭐ | 中 | ❌ 无锁 | 长事务/微服务 |
| 可靠消息 | 最终 | ⭐⭐⭐ | 中 | ❌ 无锁 | 异步解耦场景 |
| AT (Seata) | 弱 | ⭐⭐ | 低 | ✅ 全局锁 | 通用 CRUD 微服务 |

### 选型决策树

```
是否需要强一致性？
├── YES → 能否接受锁和性能损失？
│   ├── YES → 2PC/XA（跨库事务）
│   └── NO  → TCC（业务层补偿）
└── NO  → 是否是长事务？
    ├── YES → Saga
    └── NO  → 异步解耦？
        ├── YES → 可靠消息最终一致
        └── NO  → Seata AT（无侵入）
```

> 🎯 **最佳实践**：90% 的场景用 Seata AT 模式（无侵入）；核心支付链路用 TCC（强隔离）；跨部门长流程用 Saga（松耦合）。
