# 05 - Seata 分布式事务内核

> Seata AT 模式的"无侵入"不是魔法——背后是 Undo Log 自动生成反向 SQL、全局锁防脏写、两阶段提交的完整实现。理解内核，才能在生产中安心使用。

---

## 📚 目录

1. [Seata 事务架构深度回顾](#1-seata-事务架构深度回顾)
2. [AT 模式内核：Undo Log 机制](#2-at-模式内核undo-log-机制)
3. [全局锁与写隔离](#3-全局锁与写隔离)
4. [AT 模式完整时序](#4-at-模式完整时序)
5. [TCC 模式深度对比](#5-tcc-模式深度对比)
6. [Saga 模式：长事务场景](#6-saga-模式长事务场景)
7. [四种模式选型决策](#7-四种模式选型决策)

---

## 1. Seata 事务架构深度回顾

### 1.1 三大角色 + 两阶段

```text
┌──────────────────────────────────────────────────────────┐
│                    Seata 分布式事务                        │
│                                                          │
│   TM (Transaction Manager)                               │
│   └── @GlobalTransactional                              │
│   └── 定义全局事务边界                                    │
│   └── 发起 ① 开启全局事务                                 │
│   └── 发起 ② 提交/回滚全局事务                            │
│                                                          │
│   RM (Resource Manager)                                  │
│   └── 每个微服务                                          │
│   └── 管理本地事务 + 分支事务                             │
│   └── 向 TC 注册分支事务                                  │
│   └── AT 模式：自动管理 Undo Log                         │
│                                                          │
│   TC (Transaction Coordinator)                           │
│   └── Seata Server（独立部署）                            │
│   └── 维护全局事务状态                                    │
│   └── 协调两阶段提交                                      │
│   └── 存储：global_table + branch_table + lock_table      │
└──────────────────────────────────────────────────────────┘

两阶段：
  Phase 1: 各 RM 执行本地事务 + 写 Undo Log → 报告 TC "我 OK"
  Phase 2: TC 收集所有 RM 报告
    → 全部 OK → 全局提交（异步删除 Undo Log）
    → 任一失败 → 全局回滚（用 Undo Log 恢复数据）
```

### 1.2 TC 的三张核心表

```sql
-- 1. 全局事务表
CREATE TABLE global_table (
    xid VARCHAR(128) PRIMARY KEY,       -- 全局事务 ID
    transaction_id BIGINT,              -- 事务编号
    status TINYINT,                     -- 1=Begin 2=Committing 3=Rollbacking ...
    application_id VARCHAR(32),
    transaction_service_group VARCHAR(32),
    transaction_name VARCHAR(128),
    timeout INT,
    begin_time DATETIME,
    gmt_create DATETIME,
    gmt_modified DATETIME
);

-- 2. 分支事务表
CREATE TABLE branch_table (
    branch_id BIGINT PRIMARY KEY,        -- 分支事务 ID
    xid VARCHAR(128),                    -- 所属全局事务
    resource_group_id VARCHAR(32),
    resource_id VARCHAR(256),            -- 资源（数据库）
    lock_key VARCHAR(256),               -- 锁定的主键
    branch_type VARCHAR(8),              -- AT / TCC / SAGA / XA
    status TINYINT,
    application_data VARCHAR(2000),      -- 应用数据
    gmt_create DATETIME,
    gmt_modified DATETIME,
    INDEX idx_xid (xid)
);

-- 3. 全局锁表
CREATE TABLE lock_table (
    row_key VARCHAR(128) PRIMARY KEY,    -- 锁 key（资源+表+主键）
    xid VARCHAR(128),                    -- 所属全局事务
    transaction_id BIGINT,
    branch_id BIGINT,
    resource_id VARCHAR(256),
    table_name VARCHAR(32),
    pk VARCHAR(36),                      -- 主键值
    gmt_create DATETIME,
    gmt_modified DATETIME
);
```

---

## 2. AT 模式内核：Undo Log 机制

### 2.1 AT 模式的本质

```text
AT 模式 = 自动挡分布式事务

原理：
  1. 执行原始 SQL 前 → 查询当前数据 → 生成 Undo Log（反向 SQL）
  2. 执行原始 SQL
  3. Phase 1 提交本地事务（写 Undo Log + 业务数据的持久化！）
  4. Phase 2：
     → 全局提交：异步删除 Undo Log
     → 全局回滚：用 Undo Log 执行反向 SQL

关键洞察：
  AT 模式的"本地事务提交"发生在 Phase 1！
  这与 XA 完全不同（XA 是在 Phase 2 才提交）
  → 所以 AT 模式性能高，但隔离性弱（默认读未提交）
```

### 2.2 Undo Log 表结构

```sql
-- Seata 在每个业务库自动创建的 UNDO_LOG 表
CREATE TABLE undo_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    branch_id BIGINT NOT NULL,
    xid VARCHAR(128) NOT NULL,
    context VARCHAR(128),
    rollback_info LONGBLOB NOT NULL,  -- ⭐ 反向 SQL（JSON 格式）
    log_status INT NOT NULL,          -- 状态
    log_created DATETIME NOT NULL,
    log_modified DATETIME NOT NULL,
    INDEX ux_undo_log (xid, branch_id)
);
```

### 2.3 Undo Log 的内容（核心）

```json
// rollback_info 字段的内容示例
{
  "branchId": 123456789,
  "xid": "192.168.1.10:8091:1234567890",
  "undoItems": [
    {
      "tableName": "account",
      "sqlUndoLog": {
        "beforeImage": {
          // 修改前的数据快照
          "rows": [
            {
              "fields": [
                {"name": "id", "keyType": "PRIMARY_KEY", "type": 4, "value": 1001},
                {"name": "balance", "keyType": "", "type": 4, "value": 500}
              ]
            }
          ]
        },
        "afterImage": {
          // 修改后的数据快照
          "rows": [
            {
              "fields": [
                {"name": "id", "keyType": "PRIMARY_KEY", "type": 4, "value": 1001},
                {"name": "balance", "keyType": "", "type": 4, "value": 400}
              ]
            }
          ]
        },
        "sqlType": "UPDATE",
        "tableName": "account"
      }
    }
  ]
}
```

### 2.4 Undo Log 生成流程

```java
// Seata AT 模式 DataSourceProxy 的核心逻辑（简化）
public class DataSourceProxy extends AbstractDataSourceProxy {

    @Override
    public PreparedStatement prepareStatement(String sql) {
        // 1. 解析 SQL → 识别表、操作类型、WHERE 条件
        SQLRecognizer recognizer = SQLRecognizerFactory.create(sql);
        String tableName = recognizer.getTableName();
        String whereClause = recognizer.getWhereClause();

        // 2. 生成 BeforeImage 查询 SQL
        String beforeSql = "SELECT * FROM " + tableName
                         + " WHERE " + whereClause + " FOR UPDATE";

        // 3. Connection 代理 → 执行 BeforeImage SQL
        //    获取修改前的数据快照
        List<Row> beforeImage = executeQuery(beforeSql);

        // 4. 执行原始 SQL（UPDATE/INSERT/DELETE）
        int affected = originalStatement.executeUpdate();

        // 5. 生成 AfterImage 查询 SQL
        String afterSql = "SELECT * FROM " + tableName
                        + " WHERE " + whereClause;
        List<Row> afterImage = executeQuery(afterSql);

        // 6. 构造 Undo Log
        UndoLog undoLog = UndoLogBuilder.build(beforeImage, afterImage, sqlType);

        // 7. 写入 UNDO_LOG 表（与业务 SQL 在同一本地事务中）
        insertUndoLog(xid, branchId, undoLog);

        return affected;
    }
}
```

### 2.5 回滚流程

```text
全局回滚时，TC 通知各 RM 执行回滚

RM 的回滚操作：
  1. 根据 xid + branch_id 查询 UNDO_LOG 表
  2. 解析 rollback_info（beforeImage / afterImage）
  3. 数据校验：
     → 查询当前数据 → 与 afterImage 比对
     → 不一致 → 说明发生了"脏写"（被其他事务修改了）
     → 脏写 → 尝试人工处理或告警
  4. 数据一致 → 执行反向 SQL
     → UPDATE → 用 beforeImage 的字段值更新
     → INSERT → DELETE FROM table WHERE id = ?
     → DELETE → INSERT INTO table (...) VALUES (...)
  5. 删除 UNDO_LOG 记录
```

---

## 3. 全局锁与写隔离

### 3.1 为什么需要全局锁

```text
问题：AT 模式 Phase 1 就提交了本地事务
  → 其他事务可以看到未完成的全局事务的数据

场景：
  全局事务 G1: UPDATE account SET balance = balance - 100 WHERE id = 1
    → Phase 1: 本地事务提交，balance 从 500 → 400

  全局事务 G2: UPDATE account SET balance = balance + 200 WHERE id = 1
    → Phase 1: 本地事务提交，balance 从 400 → 600

  如果 G1 回滚 → balance 恢复为 500
  但 G2 的 +200 是基于 400 的 → 数据不一致！

全局锁解决这个问题：
  G1 在 Phase 1 获取全局锁（id=1）
  G2 尝试获取全局锁 → 被阻塞
  G1 Phase 2 完成 → 释放全局锁
  G2 获取锁 → 读取最新数据 → 执行
```

### 3.2 全局锁的生命周期

```text
① 获取：Phase 1 提交前
  INSERT INTO lock_table (row_key, xid, ...)
  VALUES ('jdbc:mysql://...@@@account@@@1', 'xid1', ...)

② 持有：整个全局事务期间
  Phase 1 → Phase 2 的全过程

③ 释放：Phase 2 完成
  DELETE FROM lock_table WHERE xid = 'xid1'

④ 超时释放：默认 60 秒
  TC 检测到超时 → 自动释放锁
  → 同时回滚超时的全局事务
```

### 3.3 读隔离级别

```yaml
# Seata AT 模式配置
seata:
  at:
    # 读隔离级别
    read-uncommitted: true   # 默认（性能最高）
    # false → SELECT FOR UPDATE（读已提交，性能稍低）
```

```text
read-uncommitted: true（默认）:
  SELECT 不获取全局锁
  → 可能读到其他全局事务未提交的数据
  → 但全局回滚时会校验+补偿
  → 性能好，适合大多数场景

read-uncommitted: false:
  SELECT FOR UPDATE 获取全局锁
  → 读到的一定是已全局提交的数据
  → 性能稍低，适合对一致性要求极高的场景
```

---

## 4. AT 模式完整时序

```text
┌────────┐     ┌────────┐     ┌────────┐
│  TM    │     │  TC    │     │  RM    │
│(Order) │     │(Server)│     │(Account)│
└───┬────┘     └───┬────┘     └───┬────┘
    │              │              │
    │ ① begin(xid)│              │
    │─────────────→│              │
    │              │              │
    │ ② 调用 Account 服务 │       │
    │─────────────────────────────→│
    │              │              │
    │              │ ③ register   │
    │              │←─────────────│
    │              │   branch     │
    │              │              │
    │              │ ④ Phase 1    │
    │              │   commit OK  │
    │              │←─────────────│
    │              │   (含UndoLog)│
    │              │              │
    │ ⑤ 决定提交/回滚              │
    │─────────────→│              │
    │              │              │
    │              │ ⑥ Phase 2    │
    │              │   commit     │
    │              │─────────────→│
    │              │   (删除Undo) │
    │              │              │
    │ ⑦ 返回结果   │              │
    │←─────────────│              │

关键时间窗口：
  Phase 1 到 Phase 2 之间：
    → 本地事务已提交（数据可见）
    → 全局锁持有（其他事务不能写同一行）
    → 预期：< 100ms（正常情况）
    → 最大：< 60s（超时则自动回滚）
```

---

## 5. TCC 模式深度对比

### 5.1 TCC 的本质

```text
TCC = Try → Confirm → Cancel
  每个方法都写三个实现

AT = 自动生成 Undo Log
TCC = 手动编写补偿逻辑

AT 的问题：
  → 依赖数据库的 Undo Log
  → 不能跨资源（Redis、MQ 等）

TCC 的优势：
  → 资源预留（Try 阶段冻结资源，不真正扣减）
  → 跨资源（数据库 + Redis + MQ 都可以）
  → 无全局锁（Try 阶段用业务字段控制）
```

### 5.2 TCC 三接口

```java
// TCC 模式的完整接口定义
public interface AccountTccService {

    /**
     * Try：资源预留（冻结）
     *   → 检查余额是否充足
     *   → 冻结金额（frozen_amount +100, balance 不变）
     */
    @TwoPhaseBusinessAction(name = "deductAccount", commitMethod = "commit", rollbackMethod = "cancel")
    boolean tryDeduct(
        @BusinessActionContextParameter(paramName = "userId") String userId,
        @BusinessActionContextParameter(paramName = "amount") BigDecimal amount);

    /**
     * Confirm：确认执行
     *   → 真正扣减（balance -100, frozen_amount -100）
     */
    boolean commit(BusinessActionContext context);

    /**
     * Cancel：取消回滚
     *   → 解冻金额（frozen_amount -100, balance 不变）
     */
    boolean cancel(BusinessActionContext context);
}
```

### 5.3 AT vs TCC 核心对比

```text
┌──────────────┬──────────────────────┬──────────────────────┐
│    维度       │       AT 模式         │      TCC 模式        │
├──────────────┼──────────────────────┼──────────────────────┤
│ 代码侵入性    │ 无（只改数据源代理）   │ 高（三接口全手写）    │
│ 资源支持      │ 仅数据库              │ 数据库/Redis/MQ/...  │
│ 一致性        │ 最终一致（有脏写窗口） │ 强一致（资源预留）     │
│ 性能          │ ⭐⭐⭐⭐ 高           │ ⭐⭐⭐ 中             │
│ 全局锁        │ ✅ 需要              │ ❌ 业务字段控制      │
│ 适用场景      │ 通用 CRUD            │ 核心资金链路          │
│ 空回滚        │ 不涉及               │ 需处理（Try 未执行但  │
│              │                      │  Cancel 被调用）     │
│ 悬挂          │ 不涉及               │ 需处理（Cancel 后    │
│              │                      │  Try 才到达）        │
└──────────────┴──────────────────────┴──────────────────────┘
```

### 5.4 TCC 的两个坑

```text
坑 1：空回滚（Empty Rollback）
  场景：Try 超时 → TC 调用 Cancel → 但 Try 还没执行到
  解决：Cancel 中判断 Try 是否已执行
    → 建表记录 Try 状态
    → 或通过幂等 key 判断

坑 2：悬挂（Suspension）
  场景：Cancel 先于 Try 到达
   → Cancel 执行了 → Try 才到达 → Try 被执行了
   → 资源被锁但 Cancel 已经过了
  解决：Try 中先检查 Cancel 是否已执行
```

---

## 6. Saga 模式：长事务场景

### 6.1 工作原理

```text
Saga = 每个步骤有正向操作 + 反向补偿

流程：
  Step 1 → Step 2 → Step 3 → ... → 完成
               ↓ 失败
  补偿 Step 1 ← 补偿 Step 2

适用场景：
  ✅ 长事务（几分钟到几小时）
  ✅ 老系统改造（无法加 TCC 接口）
  ✅ 跨组织/跨系统的业务流程

特点：
  ✅ 无全局锁（每步独立提交）
  ⚠️ 隔离性弱（中途数据可见）
  ✅ 可配置重试策略
  ✅ 支持异步/消息驱动的 Saga
```

### 6.2 Saga 状态机

```json
// Seata Saga 状态机配置
{
  "Name": "createOrderSaga",
  "StartState": "DeductBalance",
  "States": {
    "DeductBalance": {
      "Type": "ServiceTask",
      "ServiceName": "accountService.deduct",
      "Input": ["$.[userId]", "$.[amount]"],
      "CompensateState": "CompensateDeductBalance",
      "Next": "CreateOrder"
    },
    "CreateOrder": {
      "Type": "ServiceTask",
      "ServiceName": "orderService.create",
      "Input": ["$.[userId]", "$.[productId]"],
      "CompensateState": "CompensateCreateOrder",
      "Next": "DeductInventory"
    },
    "DeductInventory": {
      "Type": "ServiceTask",
      "ServiceName": "inventoryService.deduct",
      "Input": ["$.[productId]", "$.[quantity]"],
      "CompensateState": "CompensateDeductInventory",
      "Next": "Succeed"
    },
    "CompensateDeductBalance": {
      "Type": "ServiceTask",
      "ServiceName": "accountService.cancelDeduct",
      "Next": "Fail"
    },
    "CompensateCreateOrder": {
      "Type": "ServiceTask",
      "ServiceName": "orderService.cancel",
      "Next": "CompensateDeductBalance"
    },
    "CompensateDeductInventory": {
      "Type": "ServiceTask",
      "ServiceName": "inventoryService.cancelDeduct",
      "Next": "CompensateCreateOrder"
    },
    "Succeed": {"Type": "Succeed"},
    "Fail": {"Type": "Fail"}
  }
}
```

---

## 7. 四种模式选型决策

```text
你的场景是？
│
├── 通用 CRUD 微服务（订单、积分、商品等）
│   └── → AT 模式
│       原因：无侵入、性能高、开发效率最高
│       注意：数据库必须支持 ACID 事务
│
├── 核心资金链路（支付、转账、退款）
│   └── → TCC 模式
│       原因：需强一致性、需预留资源
│       注意：开发工作量大，需处理空回滚和悬挂
│
├── 长业务流程（几分钟到几小时、多步骤）
│   例：出差审批（申请→审批→订机票→订酒店）
│   └── → Saga 模式
│       原因：步骤间可独立提交，不需全局锁
│       注意：隔离性弱，需设计补偿逻辑
│
├── 传统单体跨库（两个本地数据库）
│   └── → XA 模式
│       原因：强协议、数据库原生支持
│       注意：性能最低、不适合高并发
│
└── 混合场景
    → 一个全局事务中不同分支用不同模式
    → Seata 支持混合模式（但非常见实践）
```

---

> 🎯 **核心要点**：AT 模式的"无侵入"背后是 Undo Log 自动生成反向 SQL + DataSource 代理 + FOR UPDATE 全局锁的组合。TCC 强一致性但代码侵入大，Saga 适合长事务。选型关键不是"哪个模式好"，而是"你的业务最受不了什么"——是数据不一致（选 TCC），还是开发太慢（选 AT），还是性能差（选 Saga）。

---

**下一模块**：[06 - 生态整合与生产最佳实践](./06-生态整合与生产最佳实践.md)  
**返回总览**：[00 - 组件体系总览](./00-SpringCloudAlibaba组件体系总览.md)
