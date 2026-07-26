# 分布式事务-Seata
> **P1 就业必备** | 分布式事务是微服务架构中最核心的难点之一，Seata 是阿里巴巴开源的分布式事务解决方案，掌握 AT、TCC、Saga、XA 四大模式是架构师必备技能

---

## 目录

1. [本章总览](#1-本章总览)
2. [分层理论讲解](#2-分层理论讲解)
3. [高频踩坑与误区](#3-高频踩坑与误区)
4. [随堂基础练习](#4-随堂基础练习)
5. [章节综合实操案例](#5-章节综合实操案例)
6. [分层综合习题](#6-分层综合习题)
7. [本章复盘速记清单](#7-本章复盘速记清单)
8. [精通拓展补充-P2](#8-精通拓展补充-p2)

---

## 1. 本章总览

### 1.1 学习目标

| 级别 | 目标 | 掌握程度 |
|------|------|----------|
| P1 就业必备 | 理解分布式事务问题、Seata AT 模式原理与使用 | 能独立配置 Seata + SpringBoot，使用 @GlobalTransactional 解决分布式事务 |
| P1 就业必备 | 掌握 TCC / Saga / XA 模式概念与选型 | 能根据业务场景选择合适模式 |
| P2 架构提升 | 理解 Seata Server 源码架构、全局锁机制 | 能排查分布式事务常见问题，优化性能 |

### 1.2 分布式事务问题引入

#### 1.2.1 什么是分布式事务

在单体应用中，一个业务操作通常在一个数据库事务中完成（ACID）。但在微服务架构中，一个业务操作会跨多个服务、多个数据库，传统的本地事务无法保证跨服务的数据一致性。

> **典型场景：电商下单**
>
> 1. OrderService 创建订单（订单库）
> 2. InventoryService 扣减库存（库存库）
> 3. AccountService 扣减余额（账户库）
>
> 任意一步失败，都需要回滚所有已成功的操作。

#### 1.2.2 分布式事务的挑战

| 挑战 | 说明 |
|------|------|
| 网络不确定性 | 服务间调用可能超时、丢包、网络分区 |
| 数据一致性 | 多个数据库独立提交，无法保证原子性 |
| 隔离性 | 全局事务未结束时，中间状态可能被其他事务读到 |
| 性能开销 | 分布式协调带来额外的网络延迟和锁竞争 |
| 回滚困难 | 跨服务的补偿逻辑复杂 |

#### 1.2.3 理论基础：CAP 与 BASE

| 理论 | 核心内容 | 分布式事务选择 |
|------|----------|----------------|
| **CAP** | 一致性 (Consistency)、可用性 (Availability)、分区容错性 (Partition Tolerance) 三者不可兼得 | AP 或 CP 二选一 |
| **BASE** | Basically Available（基本可用）、Soft State（软状态）、Eventually Consistent（最终一致性） | 弱一致性方案的基础 |

> 💡 **Seata 的选择**：Seata AT 模式追求 **CP**（强一致性），通过全局锁保证隔离性；而可靠消息最终一致性方案追求 **AP**（可用性）。

### 1.3 大纲速览

| 模块 | 内容 | 难度 |
|------|------|------|
| 2.1 | 分布式事务基础与 2PC 协议 | ★☆☆ |
| 2.2 | Seata AT 模式完整原理 | ★★☆ |
| 2.3 | Seata TCC 模式 | ★★☆ |
| 2.4 | Seata Saga 模式 | ★★★ |
| 2.5 | Seata XA 模式 | ★★☆ |
| 2.6 | Seata Server 部署 + Nacos | ★★☆ |
| 2.7 | Seata + SpringBoot 集成 | ★★☆ |
| 2.8 | 选型对比与替代方案 | ★★☆ |

---

## 2. 分层理论讲解

---

### 2.1 分布式事务基础与 2PC 协议

#### 2.1.1 本地事务回顾

```sql
-- 单体应用中的本地事务
START TRANSACTION;
UPDATE account SET balance = balance - 100 WHERE user_id = 1;
UPDATE inventory SET stock = stock - 1 WHERE sku_id = 100;
INSERT INTO orders (user_id, amount) VALUES (1, 100);
COMMIT;  -- 要么全部成功，要么全部回滚
```

本地事务依赖数据库自身的 ACID，通过 `redo log`、`undo log` 实现原子性和持久性。

#### 2.1.2 2PC（Two-Phase Commit）协议

2PC 是分布式事务的经典协议，由一个协调者（Coordinator）和多个参与者（Participant）组成。

**第一阶段：准备阶段（Prepare Phase）**

```
协调者                    参与者
   |--- CanCommit? ------>| 
   |<-- Yes / No ---------|
   |--- PreCommit? ------>|  (写 undo/redo log，加锁)
   |<-- Ready / Fail -----|
```

**第二阶段：提交/回滚阶段（Commit/Abort Phase）**

```
协调者                    参与者
   |--- DoCommit -------->|  (资源释放)
   |<-- Commit OK --------|
   OR
   |--- DoAbort --------->|  (根据 undo log 回滚)
   |<-- Rollback OK ------|
```

**2PC 的缺陷**

| 问题 | 说明 |
|------|------|
| **同步阻塞** | 参与者等待协调者指令期间，资源被锁定，性能低 |
| **单点故障** | 协调者宕机，参与者会一直阻塞 |
| **数据不一致** | 第二阶段部分参与者 commit 失败，导致数据不一致 |
| **脑裂** | 网络分区时，不同参与者的决策可能不一致 |

#### 2.1.3 3PC（Three-Phase Commit）

3PC 在 2PC 的基础上增加了 **PreCommit** 阶段和超时机制，减少阻塞时间，但依然无法完美解决数据一致性问题。

> 💡 Seata 的 AT 模式本质上是 **改进版的 2PC**，但通过引入全局锁和 undo_log 解决了传统 2PC 的阻塞问题。

---

### 2.2 Seata AT 模式完整原理

#### 2.2.1 Seata 核心架构

Seata 定义了三种核心角色：

| 角色 | 全称 | 职责 |
|------|------|------|
| **TC** | Transaction Coordinator（事务协调者） | 维护全局事务和分支事务的状态，驱动全局提交或回滚。即 Seata Server |
| **TM** | Transaction Manager（事务管理器） | 定义全局事务的范围，向 TC 发起 `begin`、`commit`、`rollback` 请求 |
| **RM** | Resource Manager（资源管理器） | 管理分支事务的资源，向 TC 注册分支事务，报告分支状态 |

```
                 TM (@GlobalTransactional)
                  |
                  | 1. begin global transaction
                  v
  +-----------+   TC (Seata Server)  +-----------+
  |   RM-A   |<------->|            |<--------->|   RM-B   |
  |(OrderDB) |  2. reg |  3. XID   | 4. reg    |(AccountDB)|
  +----------+         |           |           +-----------+
                       |5. commit/rollback|
                       +-----------------+
```

**核心流程：**

1. TM 向 TC 申请开启全局事务，TC 返回全局唯一的 **XID**（全局事务 ID）
2. XID 通过微服务调用链路传播（Seata 拦截器自动注入到 RPC 请求头）
3. 每个 RM 将本地事务注册为 TC 的分支事务
4. TM 根据业务结果向 TC 请求全局提交或全局回滚
5. TC 驱动所有 RM 执行二阶段提交或回滚

#### 2.2.2 AT 模式完整流程图

```
一阶段（业务阶段）:
┌──────────────────────────────────────────────────────┐
│ TM: @GlobalTransactional                              │
│    1. TC 开启全局事务 → 获取 XID                      │
│    2. XID 通过 Dubbo/Feign 传递到各个服务              │
│                                                       │
│ RM (每个服务中都存在):                                 │
│    3. 解析 SQL，生成 before image（查询执行前数据）    │
│    4. 执行业务 SQL                                    │
│    5. 查询 after image（查询执行后数据）               │
│    6. 生成 undo_log 记录（before + after image）      │
│    7. 向 TC 注册分支事务                              │
│    8. **立即提交本地事务**（释放数据库锁）              │
└──────────────────────────────────────────────────────┘

二阶段-提交（业务成功）:
┌──────────────────────────────────────────────────────┐
│ TC 通知所有 RM 二阶段提交                             │
│ RM: 删除对应的 undo_log 记录                          │
│ 全局事务结束                                         │
└──────────────────────────────────────────────────────┘

二阶段-回滚（业务异常）:
┌──────────────────────────────────────────────────────┐
│ TC 通知所有 RM 二阶段回滚                             │
│ RM:                                                 │
│    1. 根据 XID + BranchID 查询 undo_log              │
│    2. 校验 after image 与当前数据是否一致             │
│       └─ 一致 → 执行回滚 SQL（用 before image 恢复） │
│       └─ 不一致 → 触发 dirty write 告警，人工介入    │
│    3. 回滚完成后删除 undo_log 记录                    │
│ 全局事务结束                                         │
└──────────────────────────────────────────────────────┘
```

#### 2.2.3 关键机制详解

##### (1) 全局锁（Global Lock）

Seata AT 模式通过 **全局锁** 保证隔离性。在本地事务提交前，RM 会向 TC 申请全局锁。

```
事务A（XID=100）: UPDATE product SET stock = stock - 1 WHERE id = 1
    ↓ 申请全局锁 (pk=1, XID=100) → TC 加锁成功
    ↓ 提交本地事务（释放数据库行锁）
    
事务B（XID=200）: UPDATE product SET stock = stock - 1 WHERE id = 1
    ↓ 申请全局锁 (pk=1, XID=200) → TC 发现 pk=1 已被 XID=100 持有 → 等待/失败
```

> 💡 全局锁保证了在全局事务未结束时，其他全局事务不能修改同一条数据，避免了脏写。

##### (2) undo_log 表结构

每个业务数据库都需要创建 `undo_log` 表，用于存储二阶段回滚所需的 before/after image。

```sql
-- Seata AT 模式必须的 undo_log 表
CREATE TABLE IF NOT EXISTS `undo_log`
(
    `id`            BIGINT AUTO_INCREMENT NOT NULL COMMENT '主键',
    `branch_id`     BIGINT       NOT NULL COMMENT '分支事务ID',
    `xid`           VARCHAR(128) NOT NULL COMMENT '全局事务ID',
    `context`       VARCHAR(128) NOT NULL COMMENT '上下文',
    `rollback_info` LONGBLOB     NOT NULL COMMENT '回滚信息（before image + after image 的序列化数据）',
    `log_status`    INT          NOT NULL COMMENT '状态: 0=正常, 1=已清理',
    `log_created`   DATETIME     NOT NULL COMMENT '创建时间',
    `log_modified`  DATETIME     NOT NULL COMMENT '修改时间',
    `ext`           VARCHAR(100) DEFAULT NULL COMMENT '扩展字段',
    PRIMARY KEY (`id`),
    UNIQUE KEY `ux_undo_log` (`xid`, `branch_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='Seata AT 模式 - undo_log 表';
```

##### (3) 全局事务表（Seata Server 侧）

```sql
-- 全局事务表
CREATE TABLE IF NOT EXISTS `global_table`
(
    `xid`                       VARCHAR(128) NOT NULL COMMENT '全局事务ID',
    `transaction_id`            BIGINT       DEFAULT NULL COMMENT '事务ID',
    `status`                    TINYINT      NOT NULL COMMENT '状态: 0=Begin,1=Committing,2=Committed,3=CommitFailed,4=Rollbacking,5=Rollbacked,6=RollbackFailed,7=Timeout,8=AsyncCommitting,9=PhaseTwo_Committed,10=PhaseTwo_Rollbacked',
    `application_id`            VARCHAR(32)  DEFAULT NULL COMMENT '应用ID',
    `transaction_service_group` VARCHAR(32)  DEFAULT NULL COMMENT '事务服务组',
    `transaction_name`          VARCHAR(128) DEFAULT NULL COMMENT '事务名称',
    `timeout`                   INT          DEFAULT NULL COMMENT '超时时间(ms)',
    `begin_time`                BIGINT       DEFAULT NULL COMMENT '开始时间戳',
    `application_data`          VARCHAR(2000) DEFAULT NULL COMMENT '应用数据',
    `gmt_create`                DATETIME      DEFAULT NULL COMMENT '创建时间',
    `gmt_modified`              DATETIME      DEFAULT NULL COMMENT '修改时间',
    PRIMARY KEY (`xid`),
    KEY `idx_status_gmt_modified` (`status`, `gmt_modified`),
    KEY `idx_transaction_id` (`transaction_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='Seata Server - 全局事务表';

-- 分支事务表
CREATE TABLE IF NOT EXISTS `branch_table`
(
    `branch_id`         BIGINT       NOT NULL COMMENT '分支事务ID',
    `xid`               VARCHAR(128) NOT NULL COMMENT '全局事务ID',
    `transaction_id`    BIGINT       DEFAULT NULL COMMENT '事务ID',
    `resource_group_id` VARCHAR(32)  DEFAULT NULL COMMENT '资源组ID',
    `resource_id`       VARCHAR(256) DEFAULT NULL COMMENT '资源ID',
    `branch_type`       VARCHAR(8)   DEFAULT NULL COMMENT '分支类型: AT/TCC/SAGA/XA',
    `status`            TINYINT      DEFAULT NULL COMMENT '状态',
    `client_id`         VARCHAR(64)  DEFAULT NULL COMMENT '客户端ID',
    `application_data`  VARCHAR(2000) DEFAULT NULL COMMENT '应用数据',
    `gmt_create`        DATETIME     DEFAULT NULL COMMENT '创建时间',
    `gmt_modified`      DATETIME     DEFAULT NULL COMMENT '修改时间',
    PRIMARY KEY (`branch_id`),
    KEY `idx_xid` (`xid`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='Seata Server - 分支事务表';

-- 全局锁表
CREATE TABLE IF NOT EXISTS `lock_table`
(
    `row_key`        VARCHAR(128) NOT NULL COMMENT '行锁KEY',
    `xid`            VARCHAR(128) DEFAULT NULL COMMENT '全局事务ID',
    `transaction_id` BIGINT       DEFAULT NULL COMMENT '事务ID',
    `branch_id`      BIGINT       DEFAULT NULL COMMENT '分支事务ID',
    `resource_id`    VARCHAR(256) DEFAULT NULL COMMENT '资源ID',
    `table_name`     VARCHAR(512) DEFAULT NULL COMMENT '表名',
    `pk`             VARCHAR(128) DEFAULT NULL COMMENT '主键值',
    `status`         TINYINT      DEFAULT NULL COMMENT '状态: 0=锁定, 1=已释放',
    `gmt_create`     DATETIME     DEFAULT NULL COMMENT '创建时间',
    `gmt_modified`   DATETIME     DEFAULT NULL COMMENT '修改时间',
    PRIMARY KEY (`row_key`),
    KEY `idx_xid` (`xid`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='Seata Server - 全局锁表';
```

##### (4) Before Image 与 After Image

```
Before Image（执行前）:
{
    "tableName": "account",
    "sqlType": "UPDATE",
    "columns": ["id", "balance"],
    "before": [
        {"id": 1, "balance": 1000}
    ]
}

After Image（执行后）:
{
    "tableName": "account",
    "sqlType": "UPDATE",
    "columns": ["id", "balance"],
    "after": [
        {"id": 1, "balance": 900}
    ]
}

回滚 SQL 生成：
UPDATE account SET balance = 1000 WHERE id = 1   (根据 before image 恢复)
```

> 💡 before image 和 after image 是 Seata 自动生成的，开发者无需手动处理。Seata 通过解析 SQL 的 WHERE 条件，查询影响行的数据快照。

#### 2.2.4 AT 模式执行流程代码级分析

```java
// 一阶段：@GlobalTransactional 注解的方法
@GlobalTransactional(name = "create-order", timeoutMills = 30000, rollbackFor = Exception.class)
public void createOrder(OrderDTO orderDTO) {
    // 1. TM 向 TC 注册全局事务，获取 XID
    // 2. XID 通过 RootContext.bind(xid) 绑定到当前线程
    
    // 3. OrderService 插入订单（本地事务）
    orderDao.insert(order);
    //    ↓ Seata DataSourceProxy 拦截
    //    ├── 生成 before image
    //    ├── 执行 INSERT
    //    ├── 生成 after image
    //    ├── 写入 undo_log
    //    ├── 向 TC 注册分支事务
    //    └── 提交本地事务
    
    // 4. 远程调用 InventoryService（XID 自动传播）
    inventoryService.deductStock(orderDTO);
    //    ↓ 内部的 DataSourceProxy 同样生成 image + undo_log + 注册分支
    
    // 5. 远程调用 AccountService
    accountService.deductBalance(orderDTO);
    
    // 6. 方法正常结束 → TM 向 TC 请求全局提交
    //    TC 通知所有 RM 二阶段提交 → 删除 undo_log
}
```

---

### 2.3 Seata TCC 模式

#### 2.3.1 什么是 TCC

TCC（Try-Confirm-Cancel）是 **补偿型分布式事务方案**，需要业务方自行实现三个接口：

| 阶段 | 说明 | 示例 |
|------|------|------|
| **Try** | 预留资源，做业务检查 | 冻结库存、冻结余额 |
| **Confirm** | 确认执行业务，使用预留资源 | 扣减冻结库存 |
| **Cancel** | 取消业务，释放预留资源 | 解冻库存、解冻余额 |

#### 2.3.2 TCC 执行流程

```
TM (@GlobalTransactional)
    │
    ├── Try 阶段 ──────────────────────────────────
    │   OrderService:  创建"待支付"订单
    │   InventoryService: 冻结库存 (stock_frozen + 1)
    │   AccountService:   冻结余额 (balance_frozen + 100)
    │
    ├── 全部 Try 成功 → Confirm 阶段 ──────────────
    │   OrderService:  订单状态改为"已确认"
    │   InventoryService: 扣减实际库存 (stock - 1, stock_frozen - 1)
    │   AccountService:   扣减实际余额 (balance - 100, balance_frozen - 100)
    │
    └── 任意 Try 失败 → Cancel 阶段 ──────────────
        OrderService:  订单状态改为"已取消"
        InventoryService: 解冻库存 (stock_frozen - 1)
        AccountService:   解冻余额 (balance_frozen - 100)
```

#### 2.3.3 TCC 接口定义示例

```java
/**
 * TCC 接口定义 - 库存服务
 */
@LocalTCC
public interface InventoryTCCService {

    /**
     * Try 阶段：冻结库存
     */
    @TwoPhaseBusinessAction(
        name = "inventoryTccAction",
        commitMethod = "confirm",
        rollbackMethod = "cancel",
        useTCCFence = true  // 启用 TCC 防悬挂
    )
    boolean tryDeductStock(
        @BusinessActionContextParameter(paramName = "productId") Long productId,
        @BusinessActionContextParameter(paramName = "count") Integer count
    );

    /**
     * Confirm 阶段：扣减实际库存
     */
    boolean confirm(@BusinessActionContext context);

    /**
     * Cancel 阶段：释放冻结库存
     */
    boolean cancel(@BusinessActionContext context);
}
```

#### 2.3.4 TCC 实现示例

```java
@Service
@Slf4j
public class InventoryTCCServiceImpl implements InventoryTCCService {

    @Autowired
    private InventoryMapper inventoryMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean tryDeductStock(Long productId, Integer count) {
        log.info("TCC-Try: 冻结库存, productId={}, count={}", productId, count);

        // 业务检查：库存是否充足
        Inventory inventory = inventoryMapper.selectByProductId(productId);
        if (inventory == null || inventory.getStock() < count) {
            throw new BusinessException("库存不足");
        }

        // 预留资源：冻结库存
        inventoryMapper.freezeStock(productId, count);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean confirm(BusinessActionContext context) {
        Long productId = Long.valueOf(context.getActionContext("productId").toString());
        Integer count = Integer.valueOf(context.getActionContext("count").toString());

        log.info("TCC-Confirm: 确认扣减库存, productId={}, count={}", productId, count);

        // 确认扣减：将冻结库存转为实际扣减
        inventoryMapper.confirmDeduct(productId, count);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancel(BusinessActionContext context) {
        Long productId = Long.valueOf(context.getActionContext("productId").toString());
        Integer count = Integer.valueOf(context.getActionContext("count").toString());

        log.info("TCC-Cancel: 释放冻结库存, productId={}, count={}", productId, count);

        // 释放资源：解冻库存
        inventoryMapper.cancelFreeze(productId, count);
        return true;
    }
}
```

```sql
-- TCC 模式需要的库存表结构（需额外增加冻结字段）
CREATE TABLE `inventory_tcc` (
    `id`            BIGINT AUTO_INCREMENT PRIMARY KEY,
    `product_id`    BIGINT    NOT NULL COMMENT '商品ID',
    `stock`         INT       NOT NULL DEFAULT 0 COMMENT '实际可用库存',
    `stock_frozen`  INT       NOT NULL DEFAULT 0 COMMENT '冻结库存（TCC Try 阶段冻结）',
    `version`       INT       NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    `gmt_create`    DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified`  DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='TCC 模式库存表';

-- 更新冻结库存
UPDATE inventory_tcc
SET stock_frozen = stock_frozen + #{count}
WHERE product_id = #{productId}
  AND stock >= #{count}           -- 确保实际库存足够
  AND version = #{version};

-- Confirm：从实际库存中扣减
UPDATE inventory_tcc
SET stock = stock - #{count},
    stock_frozen = stock_frozen - #{count}
WHERE product_id = #{productId};

-- Cancel：解冻
UPDATE inventory_tcc
SET stock_frozen = stock_frozen - #{count}
WHERE product_id = #{productId};
```

#### 2.3.5 TCC 防悬挂与幂等问题

| 问题 | 说明 | 解决方案 |
|------|------|----------|
| **空回滚** | 二阶段 Cancel 执行时，一阶段 Try 还没执行 | TCC fence 记录事务状态 |
| **悬挂** | Try 在 Cancel 之后执行，导致资源被错误占用 | 通过事务状态表判断，拒绝悬挂的 Try |
| **幂等性** | Confirm/Cancel 可能被重复调用 | 使用唯一键 + 幂等表 |
| **业务侵入性** | TCC 要求业务表增加冻结字段 | 必须改造业务表结构 |

> 💡 Seata 从 1.5.0 版本开始支持 `useTCCFence = true`，自动处理空回滚和悬挂问题。Fence 记录存储在 `tcc_fence_log` 表中。

---

### 2.4 Seata Saga 模式

#### 2.4.1 什么是 Saga

Saga 是 **长事务解决方案**，将一个全局事务拆分为多个本地事务，每个本地事务都有对应的 **补偿操作**（Compensation）。Saga 不要求事务资源的强隔离，适用于业务流程长、对一致性要求较低的 **AP 场景**。

#### 2.4.2 Saga 执行模式

**模式一：串行执行 + 反向补偿**

```
OrderService.createOrder()         → 订单创建成功
    ↓                                    ↓（失败）
InventoryService.deductStock()     → 库存扣减成功
    ↓                                    ↓（失败 → 补偿 InventoryService.compensateDeduct()）
AccountService.deductBalance()     → 余额扣减成功
    ↓                                    ↓（失败 → 补偿 AccountService.compensateDeduct()）
                                        ↓
                                    OrderService.compensateCreateOrder()
```

**模式二：并行执行 + 汇总补偿（DAG 模式）**

```
              → InventoryService.deductStock()
             /            \
OrderService →              → 合并结果
             \            /
              → AccountService.deductBalance()
```

#### 2.4.3 Saga 状态机定义

Seata Saga 使用 **状态机 DSL**（JSON/YAML）来定义 Saga 流程：

```json
{
    "Name": "createOrderSaga",
    "Comment": "电商下单 Saga 流程",
    "Version": "1.0",
    "StartState": "CreateOrder",
    "States": {
        "CreateOrder": {
            "Type": "ServiceTask",
            "ServiceName": "orderService",
            "ServiceMethod": "createOrder",
            "CompensateState": "CompensateCreateOrder",
            "Next": "DeductStock"
        },
        "DeductStock": {
            "Type": "ServiceTask",
            "ServiceName": "inventoryService",
            "ServiceMethod": "deductStock",
            "CompensateState": "CompensateDeductStock",
            "Next": "DeductBalance"
        },
        "DeductBalance": {
            "Type": "ServiceTask",
            "ServiceName": "accountService",
            "ServiceMethod": "deductBalance",
            "CompensateState": "CompensateDeductBalance",
            "Next": "End"
        },
        "CompensateCreateOrder": {
            "Type": "ServiceTask",
            "ServiceName": "orderService",
            "ServiceMethod": "compensateCreateOrder"
        },
        "CompensateDeductStock": {
            "Type": "ServiceTask",
            "ServiceName": "inventoryService",
            "ServiceMethod": "compensateDeductStock"
        },
        "CompensateDeductBalance": {
            "Type": "ServiceTask",
            "ServiceName": "accountService",
            "ServiceMethod": "compensateDeductBalance"
        },
        "End": {
            "Type": "Succeed"
        }
    }
}
```

#### 2.4.4 Saga 实现示例（服务端）

```java
@Service
@Slf4j
public class InventorySagaService {

    /**
     * 正向操作：扣减库存
     */
    @Transactional
    public boolean deductStock(long productId, int count) {
        log.info("Saga: 扣减库存 productId={}, count={}", productId, count);
        return inventoryMapper.deductStock(productId, count) > 0;
    }

    /**
     * 补偿操作：恢复库存
     */
    @Transactional
    public boolean compensateDeductStock(long productId, int count) {
        log.info("Saga-补偿: 恢复库存 productId={}, count={}", productId, count);
        return inventoryMapper.addStock(productId, count) > 0;
    }
}
```

#### 2.4.5 Saga 模式的特点

| 特性 | 说明 |
|------|------|
| **无全局锁** | 资源在本地事务提交后就释放，高并发性能好 |
| **AP 倾向** | 最终一致性，中间状态对外可见 |
| **补偿逻辑** | 业务方需要实现反向补偿操作 |
| **状态管理** | 通过状态机 DSL 定义流程，Seata 负责状态持久化 |
| **适合场景** | 长业务流程、跨组织业务、对中间状态不敏感的业务 |

> ⚠️ **Saga 的隔离性问题**：因为没有全局锁，其他事务可能读到中间状态。比如扣款成功但订单还在 Saga 流程中，用户可能看到账户余额已扣但订单未确认。需要通过业务设计（如状态字段「处理中」）来规避。

---

### 2.5 Seata XA 模式

#### 2.5.1 什么是 XA 模式

XA 是 **DTP 模型**（Distributed Transaction Processing）定义的分布式事务规范。Seata XA 模式直接利用数据库对 XA 协议的支持来实现分布式事务。

#### 2.5.2 XA 模式流程

```
一阶段：
TM: @GlobalTransactional
    ├── 通知所有 RM 执行 XA START
    ├── 执行业务 SQL
    └── 通知所有 RM 执行 XA END + XA PREPARE

二阶段（全部 PREPARE 成功 → XA COMMIT）：
    ├── XA COMMIT 'xid'
    └── 释放资源

二阶段（任意 PREPARE 失败 → XA ROLLBACK）：
    ├── XA ROLLBACK 'xid'
    └── 释放资源
```

#### 2.5.3 配置示例

```yaml
# Seata XA 模式配置
seata:
  data-source-proxy-mode: XA  # 使用 XA 模式
  application-id: order-service
  tx-service-group: my_tx_group
  service:
    vgroup-mapping:
      my_tx_group: default
    grouplist:
      default: 127.0.0.1:8091
```

#### 2.5.4 XA 模式的特点

| 特性 | AT 模式 | XA 模式 |
|------|---------|---------|
| **资源锁** | 全局锁（额外机制） | 数据库自身的 XA 锁 |
| **性能** | 较高（本地事务提前提交） | 较低（锁需到二阶段才释放） |
| **代码入侵** | 无侵入 | 无侵入 |
| **数据库支持** | MySQL、PostgreSQL、Oracle 等 | 需支持 XA 的数据库（MySQL 5.7+、Oracle、DB2） |
| **XA 协议** | 不依赖 | 依赖数据库 XA 实现 |

> ⚠️ **MySQL XA 的陷阱**：MySQL 的 XA 实现存在一些已知问题，如 binlog 与 XA 状态不一致导致的 crash recovery 问题，建议在 MySQL 8.0+ 使用。

---

### 2.6 Seata Server 部署

#### 2.6.1 部署方式对比

| 方式 | 说明 | 推荐场景 |
|------|------|----------|
| 直连模式 | Seata Server 直接连接数据库，存储事务状态 | 开发测试环境 |
| Nacos 注册 | Seata Server 注册到 Nacos，客户端通过 Nacos 发现 | 生产环境（推荐） |
| DB 模式 | Seata Server 使用数据库存储全局锁和事务状态 | 生产高可用 |
| 集群模式 | 多个 Seata Server 节点构成集群，通过 Nacos/Redis 同步 | 生产高可用 |

#### 2.6.2 基于 Nacos + DB 模式部署

**Step 1: 下载 Seata Server**

```bash
# 下载 Seata Server (v2.x)
wget https://github.com/apache/incubator-seata/releases/download/v2.1.0/seata-server-2.1.0.zip
unzip seata-server-2.1.0.zip
cd seata-server-2.1.0
```

**Step 2: 配置数据库**

```sql
-- 创建 seata 数据库，用于存储事务状态
CREATE DATABASE IF NOT EXISTS `seata` DEFAULT CHARACTER SET utf8mb4;

-- 使用 seata 数据库
USE `seata`;

-- 全局事务表
CREATE TABLE IF NOT EXISTS `global_table` ( ... );  -- 见上方 SQL

-- 分支事务表
CREATE TABLE IF NOT EXISTS `branch_table` ( ... );  -- 见上方 SQL

-- 全局锁表
CREATE TABLE IF NOT EXISTS `lock_table` ( ... );     -- 见上方 SQL

-- Seata Server 分布式锁表（集群模式需要）
CREATE TABLE IF NOT EXISTS `distributed_lock` (
    `lock_key`   VARCHAR(128) NOT NULL COMMENT '锁键',
    `lock_value` VARCHAR(255) NOT NULL COMMENT '锁值',
    `expire`     BIGINT       DEFAULT NULL COMMENT '过期时间',
    PRIMARY KEY (`lock_key`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='Seata Server 分布式锁';
```

**Step 3: 配置 Seata Server `application.yml`**

```yaml
server:
  port: 7091

seata:
  server:
    service-port: 8091
    max-commit-retry-timeout: -1
    max-rollback-retry-timeout: -1
    recovery:
      committing-retry-period: 1000
      async-committing-retry-period: 1000
      rollbacking-retry-period: 1000
      timeout-retry-period: 1000

  store:
    mode: db                   # 使用数据库存储
    db:
      datasource: druid
      db-type: mysql
      driver-class-name: com.mysql.cj.jdbc.Driver
      url: jdbc:mysql://127.0.0.1:3306/seata?useUnicode=true&rewriteBatchedStatements=true
      user: root
      password: root
      min-conn: 10
      max-conn: 100
      global-table: global_table
      branch-table: branch_table
      lock-table: lock_table
      distributed-lock-table: distributed_lock
      query-limit: 1000
      max-wait: 5000

  transport:
    type: TCP
    server: NIO
    heartbeat: true
    enable-tm-client-batch-send-request: false
    enable-rm-client-batch-send-request: true
    rpc-rmq-consume-thread-pool-size: 1
    rpc-rmq-batch-request-size: 50
    rpc-tm-batch-request-size: 50

  registry:
    type: nacos                # 使用 Nacos 注册中心
    nacos:
      application: seata-server
      server-addr: 127.0.0.1:8848
      group: SEATA_GROUP
      namespace: ""
      cluster: default
      username: nacos
      password: nacos

  config:
    type: nacos                # 使用 Nacos 配置中心
    nacos:
      server-addr: 127.0.0.1:8848
      group: SEATA_GROUP
      namespace: ""
      username: nacos
      password: nacos
      data-id: seataServer.properties
```

**Step 4: 启动 Seata Server**

```bash
# Windows
bin\seata-server.bat

# Linux / macOS
bash bin/seata-server.sh
```

**Step 5: 验证部署**

```bash
# 查看日志
tail -f logs/seata-server.log

# 访问 Nacos 控制台 http://localhost:8848/nacos
# 在服务列表中查看 seata-server 是否已注册
```

#### 2.6.3 Seata 高可用集群部署架构

```
                        ┌──────────────────┐
                        │    Nginx / SLB    │
                        │   (负载均衡)      │
                        └────────┬─────────┘
                                 │
                ┌────────────────┼────────────────┐
                │                │                │
         ┌──────▼──────┐  ┌──────▼──────┐  ┌──────▼──────┐
         │ Seata Server│  │ Seata Server│  │ Seata Server│
         │   Node 1    │  │   Node 2    │  │   Node 3    │
         │ 127.0.0.1   │  │ 127.0.0.2   │  │ 127.0.0.3   │
         │   :8091     │  │   :8091     │  │   :8091     │
         └──────┬──────┘  └──────┬──────┘  └──────┬──────┘
                │                │                │
                └────────────────┼────────────────┘
                                 │
                    ┌────────────▼────────────┐
                    │     MySQL 集群           │
                    │  (global_table /         │
                    │   branch_table /         │
                    │   lock_table)            │
                    └─────────────────────────┘
```

---

### 2.7 Seata + SpringBoot + Nacos 集成

#### 2.7.1 项目依赖

```xml
<!-- Spring Boot Starter -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>2.7.18</version>
</parent>

<properties>
    <spring-cloud-alibaba.version>2021.0.6.0</spring-cloud-alibaba.version>
    <seata.version>2.1.0</seata.version>
</properties>

<dependencies>
    <!-- Seata Starter -->
    <dependency>
        <groupId>com.alibaba.cloud</groupId>
        <artifactId>spring-cloud-starter-alibaba-seata</artifactId>
        <version>${spring-cloud-alibaba.version}</version>
        <exclusions>
            <exclusion>
                <groupId>io.seata</groupId>
                <artifactId>seata-spring-boot-starter</artifactId>
            </exclusion>
        </exclusions>
    </dependency>
    <dependency>
        <groupId>io.seata</groupId>
        <artifactId>seata-spring-boot-starter</artifactId>
        <version>${seata.version}</version>
    </dependency>

    <!-- Nacos 服务发现 -->
    <dependency>
        <groupId>com.alibaba.cloud</groupId>
        <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
    </dependency>

    <!-- Nacos 配置中心 -->
    <dependency>
        <groupId>com.alibaba.cloud</groupId>
        <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
    </dependency>

    <!-- 数据库 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-jdbc</artifactId>
    </dependency>
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-boot-starter</artifactId>
        <version>3.5.3</version>
    </dependency>
    <dependency>
        <groupId>mysql</groupId>
        <artifactId>mysql-connector-java</artifactId>
        <version>8.0.33</version>
    </dependency>

    <!-- OpenFeign (服务间调用) -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-openfeign</artifactId>
    </dependency>

    <!-- Lombok -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

#### 2.7.2 核心配置

```yaml
# application.yml
server:
  port: 8081

spring:
  application:
    name: order-service
  
  datasource:
    url: jdbc:mysql://localhost:3306/order_db?useUnicode=true&characterEncoding=utf8mb4&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver

  cloud:
    nacos:
      discovery:
        server-addr: 127.0.0.1:8848
        username: nacos
        password: nacos
      config:
        server-addr: 127.0.0.1:8848
        file-extension: yaml

# ==================== Seata 核心配置 ====================
seata:
  enabled: true
  application-id: ${spring.application.name}
  tx-service-group: my_tx_group          # 事务服务组
  enable-auto-data-source-proxy: true     # 启用数据源自动代理（AT 模式关键）
  data-source-proxy-mode: AT              # AT 模式（可选: AT, XA）
  
  # 注册中心（从 Nacos 发现 Seata Server）
  registry:
    type: nacos
    nacos:
      application: seata-server
      server-addr: 127.0.0.1:8848
      group: SEATA_GROUP
      namespace: ""
      cluster: default
  
  # 配置中心
  config:
    type: nacos
    nacos:
      server-addr: 127.0.0.1:8848
      namespace: ""
      group: SEATA_GROUP
      data-id: seataServer.properties

  # 全局事务配置
  service:
    vgroup-mapping:
      my_tx_group: default               # 将事务组映射到 Seata Server 集群
    grouplist:
      default: 127.0.0.1:8091
    disable-global-transaction: false
  
  # 客户端配置
  client:
    rm:
      async-commit-buffer-limit: 1000
      report-retry-count: 5
      table-meta-check-enable: false
      sql-parser-type: druid
      lock:
        retry-interval: 10               # 全局锁重试间隔(ms)
        retry-times: 30                  # 全局锁重试次数
        retry-policy-branch-rollback-on-conflict: true
    tm:
      commit-retry-count: 5              # 提交重试次数
      rollback-retry-count: 5            # 回滚重试次数
    undo:
      log-table: undo_log                # undo_log 表名
      data-validation: true              # 回滚前校验 after image
      log-serialization: jackson         # undo_log 序列化方式
      only-care-update-columns: true     # 仅关注 UPDATE 涉及的列
```

#### 2.7.3 Nacos 配置中心（seataServer.properties）

在 Nacos 配置中心创建 `seataServer.properties`，Group = `SEATA_GROUP`：

```properties
# Seata Server 配置
store.mode=db
store.db.datasource=druid
store.db.dbType=mysql
store.db.driverClassName=com.mysql.cj.jdbc.Driver
store.db.url=jdbc:mysql://127.0.0.1:3306/seata?useUnicode=true&rewriteBatchedStatements=true
store.db.user=root
store.db.password=root
store.db.minConn=5
store.db.maxConn=30
store.db.globalTable=global_table
store.db.branchTable=branch_table
store.db.lockTable=lock_table
store.db.queryLimit=100
store.db.maxWait=5000

# 超时配置
timeout.default=60000

# 线程池
transport.threadFactory.bossThreadSize=1
transport.threadFactory.workerThreadSize=8
transport.threadFactory.executorThreadSize=100
transport.threadFactory.sharedBossThreadSize=2
transport.threadFactory.sharedWorkerThreadSize=8

# 序列化
transport.serialization=seata
transport.compressor=none
```

#### 2.7.4 使用 @GlobalTransactional

```java
@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private InventoryFeignClient inventoryFeignClient;
    @Autowired
    private AccountFeignClient accountFeignClient;

    /**
     * 创建订单 - 分布式事务
     *
     * @GlobalTransactional 参数说明:
     *   name            - 全局事务名称（用于监控和日志）
     *   timeoutMills    - 超时时间(ms)，默认 60s
     *   rollbackFor     - 触发回滚的异常类型
     *   noRollbackFor   - 不触发回滚的异常类型
     */
    @Override
    @GlobalTransactional(
        name = "create-order",
        timeoutMills = 60000,
        rollbackFor = Exception.class
    )
    public OrderResult createOrder(CreateOrderRequest request) {
        log.info("========== 开始创建订单，全局 XID = {} ==========", RootContext.getXID());

        // 1. 创建订单（本地事务）
        Order order = new Order();
        order.setUserId(request.getUserId());
        order.setProductId(request.getProductId());
        order.setCount(request.getCount());
        order.setAmount(request.getAmount());
        order.setStatus(OrderStatus.CREATED);
        orderMapper.insert(order);
        log.info("订单创建成功: orderId={}", order.getId());

        // 2. 远程调用库存服务 - 扣减库存
        log.info("调用库存服务扣减库存: productId={}, count={}", request.getProductId(), request.getCount());
        Result<Void> stockResult = inventoryFeignClient.deduct(request.getProductId(), request.getCount());
        if (!stockResult.isSuccess()) {
            throw new BusinessException("库存不足: " + stockResult.getMessage());
        }

        // 3. 远程调用账户服务 - 扣减余额
        log.info("调用账户服务扣减余额: userId={}, amount={}", request.getUserId(), request.getAmount());
        Result<Void> accountResult = accountFeignClient.deduct(request.getUserId(), request.getAmount());
        if (!accountResult.isSuccess()) {
            throw new BusinessException("余额不足: " + accountResult.getMessage());
        }

        // 4. 更新订单状态
        order.setStatus(OrderStatus.SUCCESS);
        orderMapper.updateById(order);

        log.info("========== 下单成功 ==========");
        return OrderResult.success(order.getId());
    }
}
```

#### 2.7.5 XID 传播机制

Seata 通过 **RootContext** 将 XID 绑定到当前线程，并通过微服务调用链路传播。

```java
// === 客户端 A（发起全局事务）===
// @GlobalTransactional 自动执行:
RootContext.bind(xid);  // 绑定 XID 到当前线程

// Feign 请求时，Seata 的 RequestInterceptor 自动将 XID 写入请求头
// 默认 Header 名称: TX_XID
@Bean
public RequestInterceptor requestInterceptor() {
    return template -> {
        String xid = RootContext.getXID();
        if (StringUtils.isNotBlank(xid)) {
            template.header(RootContext.KEY_XID, xid);
        }
    };
}

// === 客户端 B（参与全局事务）===
// Seata 的 Feign 拦截器从请求头读取 XID
// 自动执行: RootContext.bind(xid)
// 后续的本地事务操作都会注册到该全局事务下
```

```java
// 手动传播 XID（非 Feign 场景，如 MQ、REST Template）
// 发送方
String xid = RootContext.getXID();
message.setHeader("TX_XID", xid);

// 接收方
String xid = message.getHeader("TX_XID");
if (StringUtils.isNotBlank(xid)) {
    RootContext.bind(xid);
}
```

---

### 2.8 选型对比与替代方案

#### 2.8.1 Seata 四大模式对比

| 维度 | AT 模式 | TCC 模式 | Saga 模式 | XA 模式 |
|------|---------|----------|-----------|---------|
| **一致性** | 强一致（全局锁） | 强一致（资源预留） | 最终一致 | 强一致（XA 协议） |
| **隔离性** | 全局锁保证 | 资源预留保证 | 无隔离 | 数据库 XA 锁 |
| **代码侵入** | 无侵入 | 高（需实现 Try/Confirm/Cancel） | 高（需正向+补偿） | 无侵入 |
| **性能** | 较高 | 高（锁粒度小） | 最高（无锁） | 较低（锁到二阶段） |
| **回滚** | 自动（undo_log） | 手动（Cancel） | 手动（补偿） | 自动（XA Rollback） |
| **数据库** | MySQL 等通用 | 任何存储 | 任何存储 | 支持 XA 的数据库 |
| **业务改造成本** | 低（仅加 undo_log 表） | 高（改造表结构+接口） | 中（需补偿逻辑） | 低（仅配置模式） |
| **适用场景** | 通用场景，首选 | 短事务、高并发、资源敏感 | 长流程、AP 场景 | 已有 XA 环境 |

#### 2.8.2 场景选型矩阵

| 业务场景 | 推荐模式 | 理由 |
|----------|----------|------|
| 电商下单（库存+余额+订单） | **AT** | 无侵入，通用，自动回滚 |
| 秒杀/抢购（短事务，高并发） | **TCC** | 资源预留，锁粒度小，性能高 |
| 跨月结账/对账（长流程） | **Saga** | 无全局锁，性能好，最终一致 |
| 银行转账（严格要求 ACID） | **XA** 或 AT | 强一致性保障 |
| 积分+优惠券+支付组合 | **AT** 或 TCC | 根据并发量和一致性要求选择 |
| 已有消息队列异步场景 | 可靠消息最终一致性 | 无需改造，天然解耦 |

#### 2.8.3 非 Seata 方案：可靠消息最终一致性

Seata 追求的是 **CP**（强一致），而有些场景更适合 **AP**（最终一致），如通知、积分、日志等。

**本地消息表架构**

```
┌─────────────────────────┐
│    业务服务 (Producer)   │
│                         │
│ 1. 业务操作 + 插入消息表  │  ← 同一个本地事务
│ 2. 异步发送消息到 MQ     │  ← 定时任务扫描消息表
└──────────┬──────────────┘
           │ MQ (RocketMQ / RabbitMQ)
           │
┌──────────▼──────────────┐
│    下游服务 (Consumer)    │
│                         │
│ 3. 消费消息，执行业务     │
│ 4. 确认消费成功 → ack    │  ← 幂等性保证
└─────────────────────────┘
```

**本地消息表实现示例**

```java
/**
 * 本地消息表 - 订单服务
 */
@Service
@Slf4j
public class ReliableMessageService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private MessageRecordMapper messageRecordMapper;
    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    /**
     * 创建订单并发送消息（同一本地事务）
     */
    @Transactional(rollbackFor = Exception.class)
    public Order createOrderAndSendMessage(CreateOrderRequest request) {
        // 1. 业务操作
        Order order = new Order();
        order.setUserId(request.getUserId());
        order.setAmount(request.getAmount());
        orderMapper.insert(order);

        // 2. 插入消息记录表（同一事务！）
        MessageRecord record = new MessageRecord();
        record.setBusinessId(order.getId());
        record.setBusinessType("order_created");
        record.setMessageBody(JSON.toJSONString(request));
        record.setStatus(MessageStatus.PENDING);
        messageRecordMapper.insert(record);

        return order;
    }

    /**
     * 定时任务：扫描未发送的消息并投递
     */
    @Scheduled(fixedDelay = 5000)
    @Transactional(rollbackFor = Exception.class)
    public void retrySendMessage() {
        // 查询待发送消息
        List<MessageRecord> pendingMessages =
            messageRecordMapper.selectListByStatus(MessageStatus.PENDING);

        for (MessageRecord record : pendingMessages) {
            try {
                // 发送到 MQ
                rocketMQTemplate.convertAndSend(
                    "order-topic",
                    record.getMessageBody()
                );
                // 标记已发送
                record.setStatus(MessageStatus.SENT);
                messageRecordMapper.updateById(record);
            } catch (Exception e) {
                log.error("消息发送失败: id={}", record.getId(), e);
                // 下次定时任务重试
            }
        }
    }

    /**
     * 下游幂等处理注解
     */
    @Idempotent(businessKey = "#message.orderId")
    @Transactional(rollbackFor = Exception.class)
    public void handleOrderCreated(OrderCreatedMessage message) {
        // 幂等处理：已处理则跳过
        if (eventProcessed(message.getUniqueId())) {
            return;
        }

        // 执行库存扣减、积分发放等
        inventoryService.deduct(message.getProductId(), message.getCount());

        // 记录已处理
        markEventProcessed(message.getUniqueId());
    }
}
```

```sql
-- 本地消息表
CREATE TABLE `message_record` (
    `id`             BIGINT AUTO_INCREMENT PRIMARY KEY,
    `business_id`    VARCHAR(64)  NOT NULL COMMENT '业务ID',
    `business_type`  VARCHAR(32)  NOT NULL COMMENT '业务类型',
    `message_body`   TEXT         NOT NULL COMMENT '消息体(JSON)',
    `status`         TINYINT      NOT NULL DEFAULT 0 COMMENT '状态: 0=PENDING, 1=SENT, 2=SUCCESS, 3=FAIL',
    `retry_count`    INT          NOT NULL DEFAULT 0 COMMENT '重试次数',
    `max_retry`      INT          NOT NULL DEFAULT 5 COMMENT '最大重试次数',
    `next_retry_time` DATETIME    DEFAULT NULL COMMENT '下次重试时间',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status (`status`, `next_retry_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='本地消息表-可靠消息最终一致性';

-- 幂等表
CREATE TABLE `idempotent_record` (
    `id`             BIGINT AUTO_INCREMENT PRIMARY KEY,
    `unique_key`     VARCHAR(128) NOT NULL COMMENT '唯一键（幂等KEY）',
    `status`         TINYINT      NOT NULL DEFAULT 0 COMMENT '状态',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_unique_key` (`unique_key`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='消息幂等记录表';
```

| 方案 | 一致性 | 可用性 | 复杂度 | 场景 |
|------|--------|--------|--------|------|
| Seata AT | CP 强一致 | 较低（全局锁等待） | 低（无侵入） | 核心交易链路 |
| 本地消息表 | AP 最终一致 | 高 | 中（消息表 + 定时任务） | 非核心、可异步 |

> 💡 **最佳实践**：核心交易链路（下单、支付、退款）使用 Seata AT；非核心通知（积分、短信、日志）使用消息队列最终一致性。

---

## 3. 高频踩坑与误区

### 3.1 @GlobalTransactional 不生效

| 踩坑 | 详情 |
|------|------|
| **错误现象** | 方法抛出异常后，数据没有回滚 |
| **常见原因 1** | `@GlobalTransactional` 和 `@Transactional` 在同一个方法，但传播行为冲突 |
| **常见原因 2** | 方法内部 `try-catch` 捕获了异常，没有抛出 |
| **常见原因 3** | Feign 调用时 XID 没有传递 |

**正确姿势：**

```java
// ❌ 错误：捕获异常没抛出
@GlobalTransactional
public void createOrder(OrderDTO dto) {
    try {
        // 业务逻辑
    } catch (Exception e) {
        log.error("错误", e);
        // 没有重新抛出 → TM 认为成功 → 全局提交！
    }
}

// ✅ 正确：抛出异常
@GlobalTransactional
public void createOrder(OrderDTO dto) {
    // 业务逻辑（异常直接抛出不拦截）
}

// ✅ 或者：自定义回滚异常
@GlobalTransactional(rollbackFor = BusinessException.class)
public void createOrder(OrderDTO dto) {
    try {
        // 业务逻辑
    } catch (Exception e) {
        throw new BusinessException("下单失败", e);  // 转换为 rollbackFor 中的异常
    }
}
```

### 3.2 全局锁超时异常

| 踩坑 | 详情 |
|------|------|
| **错误现象** | `LockConflictException: Global lock acquire failed` |
| **原因** | 多个全局事务并发操作同一行数据，全局锁冲突 |
| **影响** | 性能下降，大量重试，甚至超时回滚 |

**解决方案：**

```yaml
# 方案 1：增大锁重试次数和间隔
seata:
  client:
    rm:
      lock:
        retry-interval: 10     # 默认 10ms
        retry-times: 60        # 默认 30 次，可增大
```

```java
// 方案 2：缩短全局事务范围，减少锁持有时间
// ❌ 不要在一个全局事务中做太多事情
@GlobalTransactional
public void heavyMethod() {
    step1();  // 处理 1000 行数据
    step2();  // 调用 5 个远程服务
    step3();  // 复杂计算
}

// ✅ 拆分为多个全局事务，或缩小范围
public void heavyMethod() {
    step1();   // 独立事务
    @GlobalTransactional
    public void step1() { ... }
    
    step2();   // 独立事务
    @GlobalTransactional
    public void step2() { ... }
}
```

### 3.3 undo_log 脏写校验失败

| 踩坑 | 详情 |
|------|------|
| **错误现象** | `BranchRollbackFailed: Dirty data detected, please check` |
| **原因** | 全局事务回滚时，after image 与当前数据不一致（其他事务修改了数据） |
| **影响** | 自动回滚失败，需要人工介入 |

```
场景示例：
1. 事务 A 执行：UPDATE account SET balance = 1000 WHERE id = 1
   → after image: balance = 1000
   
2. 事务 B（非 Seata 管理）执行：UPDATE account SET balance = 500 WHERE id = 1
   → 绕过了全局锁
   
3. 事务 A 需要回滚：
   校验 after image: 当前 balance = 500 ≠ after image = 1000
   → 脏数据！拒绝自动回滚！
```

**解决方案：**

```yaml
# 方案 1：关闭 dirty data 校验（谨慎使用）
seata:
  client:
    undo:
      data-validation: false    # 关闭校验（直接按 before image 回滚）
```

```java
// 方案 2：所有修改都走 Seata 全局事务
// 方案 3：定期巡检 undo_log，设置告警
// 方案 4：在 Seata 管理之外的表，不要与 Seata 管理的数据混用
```

### 3.4 无法获取 JDBC Connection

| 踩坑 | 详情 |
|------|------|
| **错误现象** | `CannotGetJdbcConnectionException` 或 DataSource 代理异常 |
| **原因 1** | 自己手动创建了 DataSource Bean，覆盖了 Seata 的自动代理 |
| **原因 2** | 多数据源场景下，Seata 只代理了主数据源 |

```java
// ❌ 错误：自定义 DataSource 覆盖了 Seata 代理
@Bean
public DataSource dataSource() {
    return new DruidDataSource();  // Seata 无法代理这个 DataSource
}

// ✅ 正确：使用 Seata 的 DataSourceProxy
@Bean
public DataSource dataSource(DataSourceProperties properties) {
    DruidDataSource ds = new DruidDataSource();
    ds.setUrl(properties.getUrl());
    ds.setUsername(properties.getUsername());
    ds.setPassword(properties.getPassword());
    // 手动包装为 Seata DataSourceProxy
    return new DataSourceProxy(ds);
}

// ✅ 或者：直接让 Seata 自动代理（默认开启）
// 确保没有手动创建 DataSource Bean
seata.enable-auto-data-source-proxy=true
```

### 3.5 Feign 调用超时导致全局事务回滚

| 踩坑 | 详情 |
|------|------|
| **错误现象** | Feign 调用超时，全局事务回滚，但实际上游服务已经提交 |
| **原因** | Feign 超时时间 < 全局事务超时时间，上游已提交但下游超时 |
| **影响** | 数据不一致：上游已提交，下游没执行 |

**解决方案：**

```yaml
# 方案 1：增大 Feign 超时时间
feign:
  client:
    config:
      default:
        connect-timeout: 5000    # 连接超时 5s
        read-timeout: 30000      # 读取超时 30s

# 方案 2：设置全局事务超时 > Feign 超时
@GlobalTransactional(timeoutMills = 60000)
```

### 3.6 常见误区总结

| 误区 | 正确理解 |
|------|----------|
| "AT 模式不需要数据库表" | 需要 `undo_log` 表 |
| "Seata 自动回滚所有操作" | 只回滚 Seata 代理的数据源操作，非代理的操作不会回滚 |
| "TCC 比 AT 性能好" | 不一定，TCC 需要业务改造，AT 有全局锁开销，具体场景具体分析 |
| "Saga 不需要补偿" | Saga 必须提供补偿操作，否则无法回滚 |
| "@GlobalTransactional 会传播到子线程" | 默认不会！子线程需手动传递 XID |
| "Seata 配置改了不需要重启" | 事务配置不支持热加载，需要重启 |
| "多数据源都能自动代理" | 只有配置了 Seata 代理的数据源才受管理 |

---

## 4. 随堂基础练习

### 4.1 概念选择题

**题目 1：** Seata 中负责维护全局事务和分支事务状态的组件是？

A. TM (Transaction Manager)
B. TC (Transaction Coordinator)
C. RM (Resource Manager)
D. XID

> **答案：B** TC 即 Seata Server，负责协调全局事务状态。

---

**题目 2：** Seata AT 模式二阶段回滚时，通过什么来恢复数据？

A. redo log
B. binlog
C. undo_log（before image）
D. 业务方手动补偿

> **答案：C** AT 模式自动记录 before/after image 到 undo_log 表，回滚时根据 before image 生成回滚 SQL。

---

**题目 3：** 以下哪个不是 Seata AT 模式需要的表？

A. undo_log
B. global_table
C. tcc_fence_log
D. lock_table

> **答案：C** `tcc_fence_log` 是 TCC 模式所需的防悬挂表，AT 模式不需要。

---

**题目 4：** 以下哪种分布式事务模式不需要业务方实现补偿逻辑？

A. TCC
B. Saga
C. AT
D. 本地消息表

> **答案：C** AT 模式通过 undo_log 自动生成回滚 SQL，无需业务方编写补偿逻辑。

---

### 4.2 判断题

**题目 5：** Seata AT 模式中，一阶段会先提交本地事务，释放数据库行锁，再申请全局锁。

> **答案：错。** 正确顺序是：先申请全局锁 → 执行 SQL → 生成 image → 注册分支 → 提交本地事务。

---

**题目 6：** Seata AT 模式的全局锁和数据库的行锁是同一个锁。

> **答案：错。** 全局锁是 Seata 在 TC 端维护的逻辑锁，数据库行锁是 InnoDB 的物理锁，两者是独立的。

---

### 4.3 简答题

**题目 7：** 简述 Seata AT 模式二阶段回滚的完整过程。

> **答案要点：**
> 1. TC 检测到全局事务需要回滚
> 2. TC 通知所有 RM 执行分支回滚
> 3. RM 根据 XID + BranchID 查询 undo_log
> 4. RM 校验 after image 与当前数据是否一致
> 5. 一致 → 执行回滚 SQL（before image 恢复）
> 6. 不一致 → 记录脏数据告警，人工介入
> 7. 删除 undo_log 记录

---

**题目 8：** 在电商下单场景中，为什么推荐使用 Seata AT 而不是 TCC？

> **答案要点：**
> - AT 模式无侵入，不需要改造表结构
> - 电商下单的 Try-Confirm-Cancel 语义不自然（库存难"冻结"）
> - AT 自动回滚，开发效率高
> - TCC 适合高并发短事务场景，普通下单并发量不需要 TCC

---

## 5. 章节综合实操案例

### 5.1 案例概述

构建一个完整的 **电商下单系统**，包含三个微服务：

| 服务 | 端口 | 数据库 | 职责 |
|------|------|--------|------|
| **order-service** | 8081 | `order_db` | 创建订单 |
| **inventory-service** | 8082 | `inventory_db` | 扣减库存 |
| **account-service** | 8083 | `account_db` | 扣减余额 |

三个服务通过 Feign 远程调用，使用 **Seata AT 模式** 保证分布式事务一致性。

### 5.2 数据库表结构

```sql
-- ==================== order_db ====================
CREATE DATABASE IF NOT EXISTS `order_db` DEFAULT CHARACTER SET utf8mb4;
USE `order_db`;

-- 订单表
CREATE TABLE `orders` (
    `id`          BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id`     BIGINT       NOT NULL COMMENT '用户ID',
    `product_id`  BIGINT       NOT NULL COMMENT '商品ID',
    `count`       INT          NOT NULL DEFAULT 1 COMMENT '数量',
    `amount`      DECIMAL(10,2) NOT NULL COMMENT '金额',
    `status`      VARCHAR(16)  NOT NULL DEFAULT 'CREATED' COMMENT '状态: CREATED/SUCCESS/CANCELLED',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='订单表';

-- undo_log（Seata AT 模式必须）
CREATE TABLE IF NOT EXISTS `undo_log`
(
    `id`            BIGINT AUTO_INCREMENT NOT NULL,
    `branch_id`     BIGINT       NOT NULL,
    `xid`           VARCHAR(128) NOT NULL,
    `context`       VARCHAR(128) NOT NULL,
    `rollback_info` LONGBLOB     NOT NULL,
    `log_status`    INT          NOT NULL,
    `log_created`   DATETIME     NOT NULL,
    `log_modified`  DATETIME     NOT NULL,
    `ext`           VARCHAR(100) DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `ux_undo_log` (`xid`, `branch_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- ==================== inventory_db ====================
CREATE DATABASE IF NOT EXISTS `inventory_db` DEFAULT CHARACTER SET utf8mb4;
USE `inventory_db`;

-- 库存表
CREATE TABLE `inventory` (
    `id`         BIGINT AUTO_INCREMENT PRIMARY KEY,
    `product_id` BIGINT       NOT NULL COMMENT '商品ID',
    `stock`      INT          NOT NULL DEFAULT 0 COMMENT '库存数量',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_product_id` (`product_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='库存表';

-- 初始化数据
INSERT INTO `inventory` (`product_id`, `stock`) VALUES (1001, 100);

-- undo_log（同上）
CREATE TABLE IF NOT EXISTS `undo_log` ( ... );

-- ==================== account_db ====================
CREATE DATABASE IF NOT EXISTS `account_db` DEFAULT CHARACTER SET utf8mb4;
USE `account_db`;

-- 账户表
CREATE TABLE `account` (
    `id`          BIGINT AUTO_INCREMENT PRIMARY KEY,
    `user_id`     BIGINT       NOT NULL COMMENT '用户ID',
    `balance`     DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '余额',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='账户表';

-- 初始化数据
INSERT INTO `account` (`user_id`, `balance`) VALUES (1, 1000.00);

-- undo_log（同上）
CREATE TABLE IF NOT EXISTS `undo_log` ( ... );
```

### 5.3 order-service 完整代码

#### 5.3.1 核心配置

```yaml
# order-service/src/main/resources/application.yml
server:
  port: 8081

spring:
  application:
    name: order-service
  datasource:
    url: jdbc:mysql://localhost:3306/order_db?useUnicode=true&characterEncoding=utf8mb4&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver
  cloud:
    nacos:
      discovery:
        server-addr: 127.0.0.1:8848

mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
    map-underscore-to-camel-case: true

seata:
  enabled: true
  application-id: order-service
  tx-service-group: my_tx_group
  registry:
    type: nacos
    nacos:
      application: seata-server
      server-addr: 127.0.0.1:8848
      group: SEATA_GROUP
  config:
    type: nacos
    nacos:
      server-addr: 127.0.0.1:8848
      group: SEATA_GROUP
      data-id: seataServer.properties
  service:
    vgroup-mapping:
      my_tx_group: default
    grouplist:
      default: 127.0.0.1:8091
```

#### 5.3.2 启动类

```java
package com.example.order;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@MapperScan("com.example.order.mapper")
public class OrderServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
```

#### 5.3.3 实体类

```java
package com.example.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("orders")
public class Order {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long productId;
    private Integer count;
    private BigDecimal amount;
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
```

#### 5.3.4 Feign 客户端

```java
package com.example.order.feign;

import com.example.order.dto.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "inventory-service", path = "/inventory")
public interface InventoryFeignClient {

    @GetMapping("/deduct")
    Result<Void> deduct(@RequestParam("productId") Long productId,
                        @RequestParam("count") Integer count);
}

@FeignClient(name = "account-service", path = "/account")
public interface AccountFeignClient {

    @GetMapping("/deduct")
    Result<Void> deduct(@RequestParam("userId") Long userId,
                        @RequestParam("amount") BigDecimal amount);
}
```

#### 5.3.5 Service 层

```java
package com.example.order.service;

import com.example.order.dto.CreateOrderRequest;
import com.example.order.dto.OrderResult;
import com.example.order.entity.Order;
import com.example.order.enums.OrderStatus;
import com.example.order.feign.AccountFeignClient;
import com.example.order.feign.InventoryFeignClient;
import com.example.order.mapper.OrderMapper;
import io.seata.core.context.RootContext;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderMapper orderMapper;
    private final InventoryFeignClient inventoryFeignClient;
    private final AccountFeignClient accountFeignClient;

    /**
     * 创建订单（分布式事务）
     *
     * 流程：
     * 1. 创建订单（本地事务，状态=CREATED）
     * 2. 远程调用库存服务扣减库存
     * 3. 远程调用账户服务扣减余额
     * 4. 更新订单状态为 SUCCESS
     *
     * 任意步骤失败 → Seata 自动回滚所有已提交的本地事务
     */
    @GlobalTransactional(name = "create-order", timeoutMills = 60000, rollbackFor = Exception.class)
    @Transactional(rollbackFor = Exception.class)
    public OrderResult createOrder(CreateOrderRequest request) {
        log.info("========== 开始分布式事务，XID = {} ==========", RootContext.getXID());

        // 1. 创建订单
        Order order = new Order();
        order.setUserId(request.getUserId());
        order.setProductId(request.getProductId());
        order.setCount(request.getCount());
        order.setAmount(request.getAmount());
        order.setStatus(OrderStatus.CREATED.name());
        orderMapper.insert(order);
        log.info("订单创建成功: orderId={}", order.getId());

        // 2. 扣减库存（远程 Feign 调用）
        log.info("开始扣减库存: productId={}, count={}", request.getProductId(), request.getCount());
        Result<Void> stockResult = inventoryFeignClient.deduct(
                request.getProductId(), request.getCount());
        if (!stockResult.isSuccess()) {
            throw new RuntimeException("库存扣减失败: " + stockResult.getMessage());
        }
        log.info("库存扣减成功");

        // 3. 扣减余额（远程 Feign 调用）
        log.info("开始扣减余额: userId={}, amount={}", request.getUserId(), request.getAmount());
        Result<Void> accountResult = accountFeignClient.deduct(
                request.getUserId(), request.getAmount());
        if (!accountResult.isSuccess()) {
            throw new RuntimeException("余额扣减失败: " + accountResult.getMessage());
        }
        log.info("余额扣减成功");

        // 4. 更新订单状态
        order.setStatus(OrderStatus.SUCCESS.name());
        orderMapper.updateById(order);

        log.info("========== 下单成功，XID = {} ==========", RootContext.getXID());
        return OrderResult.success(order.getId());
    }
}
```

#### 5.3.6 Controller 层

```java
package com.example.order.controller;

import com.example.order.dto.CreateOrderRequest;
import com.example.order.dto.OrderResult;
import com.example.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/create")
    public OrderResult createOrder(@RequestBody CreateOrderRequest request) {
        return orderService.createOrder(request);
    }
}
```

### 5.4 inventory-service 完整代码

```yaml
# inventory-service/src/main/resources/application.yml
server:
  port: 8082

spring:
  application:
    name: inventory-service
  datasource:
    url: jdbc:mysql://localhost:3306/inventory_db?useUnicode=true&characterEncoding=utf8mb4&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver
  cloud:
    nacos:
      discovery:
        server-addr: 127.0.0.1:8848

seata:
  enabled: true
  application-id: inventory-service
  tx-service-group: my_tx_group
  registry:
    type: nacos
    nacos:
      application: seata-server
      server-addr: 127.0.0.1:8848
      group: SEATA_GROUP
  config:
    type: nacos
    nacos:
      server-addr: 127.0.0.1:8848
      group: SEATA_GROUP
      data-id: seataServer.properties
  service:
    vgroup-mapping:
      my_tx_group: default
    grouplist:
      default: 127.0.0.1:8091
```

```java
package com.example.inventory.service;

import com.example.inventory.dto.Result;
import com.example.inventory.mapper.InventoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryMapper inventoryMapper;

    @Transactional(rollbackFor = Exception.class)
    public Result<Void> deduct(Long productId, Integer count) {
        log.info("扣减库存: productId={}, count={}", productId, count);

        // 查询库存
        Inventory inventory = inventoryMapper.selectByProductId(productId);
        if (inventory == null) {
            return Result.fail("商品不存在");
        }
        if (inventory.getStock() < count) {
            return Result.fail("库存不足, 当前库存: " + inventory.getStock());
        }

        // 扣减库存（Seata 自动生成 undo_log）
        int affected = inventoryMapper.deductStock(productId, count);
        if (affected == 0) {
            return Result.fail("扣减失败");
        }

        log.info("库存扣减成功: productId={}, 剩余库存={}", productId, inventory.getStock() - count);
        return Result.success(null);
    }
}
```

```java
package com.example.inventory.controller;

import com.example.inventory.dto.Result;
import com.example.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/deduct")
    public Result<Void> deduct(@RequestParam Long productId, @RequestParam Integer count) {
        return inventoryService.deduct(productId, count);
    }
}
```

### 5.5 account-service 完整代码

```yaml
# account-service/src/main/resources/application.yml
server:
  port: 8083

spring:
  application:
    name: account-service
  datasource:
    url: jdbc:mysql://localhost:3306/account_db?useUnicode=true&characterEncoding=utf8mb4&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver
  cloud:
    nacos:
      discovery:
        server-addr: 127.0.0.1:8848

seata:
  enabled: true
  application-id: account-service
  tx-service-group: my_tx_group
  registry:
    type: nacos
    nacos:
      application: seata-server
      server-addr: 127.0.0.1:8848
      group: SEATA_GROUP
  config:
    type: nacos
    nacos:
      server-addr: 127.0.0.1:8848
      group: SEATA_GROUP
      data-id: seataServer.properties
  service:
    vgroup-mapping:
      my_tx_group: default
    grouplist:
      default: 127.0.0.1:8091
```

```java
package com.example.account.service;

import com.example.account.dto.Result;
import com.example.account.mapper.AccountMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountMapper accountMapper;

    @Transactional(rollbackFor = Exception.class)
    public Result<Void> deduct(Long userId, BigDecimal amount) {
        log.info("扣减余额: userId={}, amount={}", userId, amount);

        // 查询账户
        Account account = accountMapper.selectByUserId(userId);
        if (account == null) {
            return Result.fail("账户不存在");
        }
        if (account.getBalance().compareTo(amount) < 0) {
            return Result.fail("余额不足, 当前余额: " + account.getBalance());
        }

        // 扣减余额（Seata 自动生成 undo_log）
        int affected = accountMapper.deductBalance(userId, amount);
        if (affected == 0) {
            return Result.fail("扣减失败");
        }

        log.info("余额扣减成功: userId={}, 剩余余额={}", userId, account.getBalance().subtract(amount));
        return Result.success(null);
    }
}
```

```java
package com.example.account.controller;

import com.example.account.dto.Result;
import com.example.account.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;

@RestController
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping("/deduct")
    public Result<Void> deduct(@RequestParam Long userId, @RequestParam BigDecimal amount) {
        return accountService.deduct(userId, amount);
    }
}
```

### 5.6 测试与验证

#### 5.6.1 正常下单测试

```bash
# 请求：创建订单，用户 1 购买商品 1001，数量 1，金额 100
curl -X POST http://localhost:8081/order/create \
  -H "Content-Type: application/json" \
  -d '{"userId": 1, "productId": 1001, "count": 1, "amount": 100}'

# 正常响应：
# {"success": true, "data": {"orderId": 1}}

# 验证：
# 1. order_db.orders 中新增一条状态为 SUCCESS 的记录
# 2. inventory_db.inventory.stock 从 100 减少到 99
# 3. account_db.account.balance 从 1000 减少到 900
# 4. 各库的 undo_log 表有记录（全局事务提交后会被删除）
```

#### 5.6.2 异常回滚测试

```bash
# 请求：余额不足（用户 1 余额 1000，支付 9999）
curl -X POST http://localhost:8081/order/create \
  -H "Content-Type: application/json" \
  -d '{"userId": 1, "productId": 1001, "count": 1, "amount": 9999}'

# 异常响应：
# {"success": false, "message": "余额不足, 当前余额: 1000"}

# 验证回滚：
# 1. order_db.orders 中不应该有新增记录（或状态为 CREATED 的记录已回滚删除）
# 2. inventory_db.inventory.stock 仍然是 100（回滚了扣减）
# 3. account_db.account.balance 仍然是 1000（未扣减）
```

> 🎯 **成功标志**：Seata 的 AT 模式自动完成了二阶段回滚，所有服务的数据都恢复到了执行前的状态。

#### 5.6.3 验证日志输出

```
# Order Service 正常下单日志
SeataAutoConfiguration: createATDataSource   # Seata 代理数据源
GlobalTransaction: begin                     # TM 开始全局事务
  XID: 192.168.1.100:8091:1234567890         # 全局事务 ID
DataSourceManager: registerResource          # RM 注册资源
OrderMapper.insert: INSERT INTO orders...    # 执行 SQL（同时生成 undo_log）
GlobalTransaction: commit                    # TM 请求全局提交
AsyncCommitting: commit branch 1234567891    # TC 异步提交分支

# 异常回滚日志
GlobalTransaction: rollback                  # TM 请求全局回滚
UndoLogManager: undo_log found, start rollback  # 找到 undo_log
DataSourceManager: execute undo SQL          # 执行回滚 SQL（根据 before image）
UndoLogManager: undo_log deleted              # 删除 undo_log 记录
```

---

## 6. 分层综合习题

### 6.1 基础题

**习题 1：** 什么是 XID？它在 Seata 分布式事务中扮演什么角色？

> **参考答案：** XID 是全局唯一的事务 ID，由 TC 生成。它在整个分布式事务链路中传递，用于标识和关联同一全局事务下的所有分支事务。各服务通过 XID 向 TC 注册分支事务。

---

**习题 2：** 简述 Seata AT 模式中 undo_log 的生成时机和删除时机。

> **参考答案：**
> - **生成时机**：一阶段执行业务 SQL 后，Seata 自动记录 before image（执行前快照）和 after image（执行后快照），序列化后存储到 undo_log。
> - **删除时机**：二阶段全局提交（删除 undo_log）或二阶段回滚完成（删除 undo_log）。

---

**习题 3：** Seata 事务的 `@GlobalTransactional` 和 Spring 的 `@Transactional` 可以同时使用吗？

> **参考答案：** 可以同时使用。`@GlobalTransactional` 管理全局事务，`@Transactional` 管理本地数据库事务。通常建议同时使用，确保本地事务的隔离级别和传播行为。注意 `@Transactional` 的 rollbackFor 要与 `@GlobalTransactional` 一致。

---

### 6.2 进阶题

**习题 4：** 在 Seata AT 模式中，如果二阶段回滚时发生脏数据（after image 不匹配），如何处理？

> **参考答案：** 默认情况下，Seata 会抛出 `BranchRollbackFailed` 异常并拒绝自动回滚，防止数据进一步损坏。解决方案：
> 1. 人工介入：检查业务数据，手动修复不一致。
> 2. 关闭脏数据校验：设置 `seata.client.undo.data-validation=false`（不推荐）。
> 3. 根本解决：确保所有对该数据的修改都在 Seata 全局事务的管理下，避免绕过全局锁的操作。

---

**习题 5：** 高并发场景下，Seata AT 模式的全局锁可能成为瓶颈，如何优化？

> 提示方向：
> - 缩短全局事务范围，减少锁持有时间
> - 控制并发量，使用限流
> - 考虑切换为 TCC 模式（锁粒度更小）
> - 对热点数据使用最终一致性方案

---

**习题 6：** 如何将 Seata AT 模式切换为 XA 模式？需要修改哪些配置？

> **参考答案：** 修改两个配置：
> 1. `seata.data-source-proxy-mode=XA`（改为 XA 模式）
> 2. 确保数据库支持 XA 协议（MySQL 5.7+、Oracle 等）
> 不需要 undo_log 表（XA 模式不依赖 Seata 的 undo_log，而是用数据库自身的 XA 事务）。

---

### 6.3 精通题

**习题 7：** 设计一个混合分布式事务方案：订单创建的强一致操作使用 Seata AT，下单后的积分赠送使用可靠消息最终一致性。

> **设计要点：**
> ```
> @GlobalTransactional
> public void createOrder() {
>     1. 创建订单（AT 管理）
>     2. 扣库存（AT 管理）
>     3. 扣余额（AT 管理）
>     4. 发送"下单成功"消息到延迟队列 ← 不在@GlobalTransactional内
> }
> 
> 下游积分服务：
> @RabbitListener
> public void handleOrderCreated() {
>     // 幂等消费
>     grantPoints();
> }
> ```
> - AT 保证核心链路强一致
> - MQ 保证积分异步最终一致
> - 互不影响

---

**习题 8：** 在 Seata TCC 模式中，什么是"空回滚"和"悬挂"？Seata 如何解决？

> **参考答案：**
> - **空回滚**：二阶段 Cancel 执行时，一阶段 Try 还没执行（如超时）。导致 Cancel 做了无意义的操作。
> - **悬挂**：二阶段 Cancel 执行后，一阶段 Try 才到达。导致 Try 错误地占用了资源。
> - **解决方案**：Seata 1.5.0+ 的 `useTCCFence = true` 通过 `tcc_fence_log` 表记录事务状态，Try 执行前检查是否已经 Cancel 过（防止悬挂），Cancel 执行前检查 Try 是否已完成（防止空回滚）。

---

**习题 9：** 假设在 Seata 二阶段提交时，部分 RM 提交成功、部分 RM 提交失败，事务最终处于什么状态？如何修复？

> **参考答案：** 这种情况称为 **"数据不一致的异常状态"**。Seata 会标记事务为 CommitFailed，并持续重试失败的 RM（最多重试 `client.tm.commit-retry-count` 次）。如果重试耗尽仍未成功：
> 1. 记录告警日志
> 2. 人工介入修复
> 3. 可配置 `seata.server.max-commit-retry-timeout=-1` 表示一直重试

---

## 7. 本章复盘速记清单

### 7.1 核心概念速记

| 概念 | 一句话记忆 |
|------|-----------|
| **分布式事务** | 跨服务、跨数据库的事务一致性保障 |
| **CAP 理论** | 一致性、可用性、分区容错性三者不可兼得 |
| **BASE 理论** | 最终一致性——基本可用、软状态、最终一致 |
| **2PC** | 两阶段提交：准备阶段 + 提交/回滚阶段 |
| **TC** | Transaction Coordinator = Seata Server（协调者） |
| **TM** | Transaction Manager = @GlobalTransactional（发起者） |
| **RM** | Resource Manager = 数据源代理（参与者） |
| **XID** | 全局事务 ID，贯穿整个调用链路 |

### 7.2 Seata 四模式速记

| 模式 | 核心思想 | 补偿方式 | 一句话记忆 |
|------|----------|----------|-----------|
| **AT** | 自动生成 undolog | 自动（undo_log） | 自动版 2PC，无侵入 |
| **TCC** | 业务预留资源 | 手动（Cancel） | 改业务表，高性能 |
| **Saga** | 长事务分解 + 补偿 | 手动（补偿） | 无锁，AP 方案 |
| **XA** | 数据库原生 XA 协议 | 自动（XA Rollback） | 标准协议，性能低 |

### 7.3 配置速查

```yaml
# === Seata 必备配置（二选一） ===

# 方式一：直连 Seata Server
seata:
  tx-service-group: my_tx_group
  service:
    vgroup-mapping:
      my_tx_group: default
    grouplist:
      default: 127.0.0.1:8091

# 方式二：通过 Nacos 发现 Seata Server（生产推荐）
seata:
  tx-service-group: my_tx_group
  registry:
    type: nacos
    nacos:
      server-addr: 127.0.0.1:8848
      application: seata-server

# === 模式切换 ===
seata:
  data-source-proxy-mode: AT   # AT 模式（默认）
  # data-source-proxy-mode: XA  # XA 模式

# === 必备注解 ===
@GlobalTransactional(name = "xxx", timeoutMills = 60000, rollbackFor = Exception.class)
```

### 7.4 建表速查

```sql
-- 每个业务数据库都要执行
CREATE TABLE IF NOT EXISTS `undo_log` (
    `id`            BIGINT AUTO_INCREMENT NOT NULL,
    `branch_id`     BIGINT       NOT NULL,
    `xid`           VARCHAR(128) NOT NULL,
    `context`       VARCHAR(128) NOT NULL,
    `rollback_info` LONGBLOB     NOT NULL,
    `log_status`    INT          NOT NULL,
    `log_created`   DATETIME     NOT NULL,
    `log_modified`  DATETIME     NOT NULL,
    `ext`           VARCHAR(100) DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `ux_undo_log` (`xid`, `branch_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
```

### 7.5 常见问题诊断清单

| 问题 | 排查步骤 |
|------|----------|
| @GlobalTransactional 不生效 | 1. 是否被 try-catch 吞掉异常？2. 是否有手动创建 DataSource？3. XID 是否传递？ |
| 全局锁超时 | 1. 是否热点数据并发？2. 全局事务范围是否过大？3. 重试次数/间隔是否合理？ |
| 脏数据校验失败 | 1. 是否有非 Seata 操作修改了数据？2. 是否可关闭校验？3. 是否需人工修复？ |
| Feign 超时 | 1. Feign 超时时间是否小于全局事务超时时间？2. 网络是否稳定？ |
| TC 连接失败 | 1. Seata Server 是否启动？2. Nacos 注册是否成功？3. 网络策略/防火墙？ |

---

## 8. 精通拓展补充-P2

### 8.1 Seata 源码架构概览

#### 8.1.1 核心模块

| 模块 | 作用 |
|------|------|
| **seata-core** | 核心 API：RootContext（XID 管理）、定义 TC/TM/RM 接口 |
| **seata-rm-datasource** | AT 模式核心：DataSourceProxy、ConnectionProxy、StatementProxy、Executor |
| **seata-tcc** | TCC 模式核心：@LocalTCC、@TwoPhaseBusinessAction、TCCResourceManager |
| **seata-saga** | Saga 状态机引擎：状态机定义、执行、补偿 |
| **seata-server** | TC 服务端：事务协调、全局锁管理、会话管理 |

#### 8.1.2 AT 模式核心执行器链

```
StatementProxy.execute()
    ↓
AbstractDMLBaseExecutor.execute()
    ↓
    ├── executeAutoCommitTrue()   -- 自动提交
    │   ├── buildBeforeImage()    -- 查询前镜像
    │   ├── execute SQL           -- 执行 SQL
    │   ├── buildAfterImage()     -- 查询后镜像
    │   ├── prepareUndoLog()      -- 生成 undo_log
    │   └── commit                -- 提交事务
    │
    └── executeAutoCommitFalse()  -- 手动事务（同上，加上锁管理）
```

#### 8.1.3 全局锁管理

```java
// LockManager 关键逻辑（源码简化示意）
public class LockManagerImpl implements LockManager {

    @Override
    public boolean acquireLock(List<LockDO> lockDOs) {
        // 使用 SELECT FOR UPDATE 或分布式锁实现
        // 锁等待策略：retry-interval + retry-times
        for (int i = 0; i < retryTimes; i++) {
            try {
                // 插入 lock_table
                return doAcquireLock(lockDOs);
            } catch (LockConflictException e) {
                // 等待 retry-interval 后重试
                Thread.sleep(retryInterval);
            }
        }
        throw new LockConflictException("Global lock acquire failed");
    }
}
```

### 8.2 Seata 与 Spring Cloud 全家桶集成

#### 8.2.1 Nacos + Seata + Sentinel 三件套

```yaml
# pom.xml 依赖
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
</dependency>
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-seata</artifactId>
</dependency>
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-sentinel</artifactId>
</dependency>
```

#### 8.2.2 Seata + Dubbo 集成

```yaml
# Dubbo + Seata 配置
dubbo:
  application:
    name: order-service
  registry:
    address: nacos://127.0.0.1:8848
  protocol:
    name: dubbo
    port: -1

seata:
  tx-service-group: my_tx_group
```

```java
// Dubbo 服务接口（XID 自动通过 RPC 上下文传播）
public interface OrderService {
    @GlobalTransactional
    Order createOrder(OrderDTO dto);  // Dubbo 协议自动传递 XID
}
```

### 8.3 事务分组与多环境隔离

#### 8.3.1 事务分组配置

```
seata.tx-service-group = my_tx_group

seata.service.vgroup-mapping:
  my_tx_group: default     # 映射到 default 集群

seata.service.grouplist:
  default: 127.0.0.1:8091
```

多环境隔离策略：

```
# 开发环境
seata.tx-service-group = dev_tx_group
→ dev_tx_group: dev

# 测试环境
seata.tx-service-group = test_tx_group
→ test_tx_group: test

# 生产环境
seata.tx-service-group = prod_tx_group
→ prod_tx_group: prod
```

### 8.4 Seata 性能优化建议

| 优化项 | 说明 | 推荐值 |
|--------|------|--------|
| 全局锁重试间隔 | 避免频繁重试导致 TC 负载过高 | 10~50ms |
| 全局锁重试次数 | 根据业务容忍度调整 | 30~100 次 |
| undo_log 清理 | 异步清理，避免主事务受影响 | 默认即可 |
| Seata Server 连接池 | 根据应用数量调整 | 最大 100 |
| TC 异步提交 | 启用异步提交提升一阶段性能 | 默认开启 |
| 批量发送 | 合并分支事务注册请求，减少网络开销 | 开启 |

```yaml
seata:
  transport:
    enable-rm-client-batch-send-request: true   # RM 批量发送
    enable-tm-client-batch-send-request: false  # TM 逐条发送
    rpc-rmq-batch-request-size: 50              # 批量大小
```

### 8.5 监控与运维

#### 8.5.1 Seata Server 监控指标

Seata Server 支持通过 Micrometer + Prometheus 暴露指标：

```yaml
seata:
  metrics:
    enabled: true
    registry-type: compact
    exporter-list: prometheus
    exporter-prometheus-port: 9898
```

```bash
# Prometheus 配置
scrape_configs:
  - job_name: 'seata'
    metrics_path: '/metrics'
    static_configs:
      - targets: ['localhost:9898']
```

**核心监控指标：**

| 指标 | 含义 | 告警阈值 |
|------|------|----------|
| `seata.global.transactions.total` | 全局事务总数 | - |
| `seata.global.transactions.failed` | 全局事务失败数 | > 0 告警 |
| `seata.branch.transactions.total` | 分支事务总数 | - |
| `seata.global.lock.conflicts` | 全局锁冲突次数 | 突增告警 |
| `seata.global.transactions.time` | 全局事务耗时 | > 500ms 告警 |

#### 8.5.2 日志排查关键点

```bash
# 查看 Seata Server 日志
tail -f logs/seata-server.log | grep -E "ERROR|WARN"

# 查看客户端日志（找 XID）
grep "RootContext" app.log
grep "GlobalTransaction" app.log
grep "undo_log" app.log
grep "BranchRollbackFailed" app.log
```

#### 8.5.3 事务超时处理

```sql
-- 查询 Seata 数据库中处于进行中的全局事务
SELECT * FROM global_table WHERE status = 0;

-- 手动清理超时事务（仅在紧急情况下使用）
-- 注意：清理前请确认该事务对应的业务数据状态
DELETE FROM global_table WHERE xid = 'xxx' AND status = 0;
DELETE FROM branch_table WHERE xid = 'xxx';
DELETE FROM lock_table WHERE xid = 'xxx';
```

### 8.6 Seata 版本演进

| 版本 | 里程碑 | 新特性 |
|------|--------|--------|
| 0.5.x | 初始开源 | AT 模式基础实现 |
| 0.9.x | 生产级 | TCC 模式、Saga 状态机 |
| 1.0.0 | GA 正式版 | XA 模式、全局锁优化 |
| 1.4.x | 性能优化 | 批量发送、异步提交 |
| 1.5.x | 功能增强 | TCC Fence（防悬挂）、配置简化 |
| 2.0.0 | 架构重构 | gRPC 协议、云原生适配 |
| 2.1.x | 当前稳定 | k8s 部署支持、GraalVM 兼容 |

> 💡 **版本选择建议**：新项目推荐使用 Seata 2.1.x（最新稳定版），Spring Boot 2.7.x + Spring Cloud Alibaba 2021.x 兼容性最好。

### 8.7 推荐学习资源

| 资源 | 类型 | 链接 |
|------|------|------|
| Seata 官方文档 | 文档 | https://seata.apache.org/ |
| Seata 源码 | GitHub | https://github.com/apache/incubator-seata |
| Seata 样例项目 | 代码 | https://github.com/apache/incubator-seata/tree/develop/sample |
| Spring Cloud Alibaba 文档 | 文档 | https://sca.aliyun.com/ |
| 阿里云 GTS（商业化） | 云服务 | https://www.aliyun.com/product/gts |

---

> 🎯 **本章总结**：分布式事务是微服务架构中最核心的难点之一。Seata AT 模式通过"一阶段提交 + 二阶段回滚（undo_log）+ 全局锁"机制，实现了对业务几乎无侵入的分布式事务解决方案。TCC 模式适用于高并发场景，Saga 模式适用于长流程 AP 场景，XA 模式适用于强一致性要求的标准场景。在实际项目中，应该根据业务场景灵活选择 Seata 的四种模式，或结合本地消息表实现最终一致性方案，**核心链路用 AT，非核心链路用 MQ**。
