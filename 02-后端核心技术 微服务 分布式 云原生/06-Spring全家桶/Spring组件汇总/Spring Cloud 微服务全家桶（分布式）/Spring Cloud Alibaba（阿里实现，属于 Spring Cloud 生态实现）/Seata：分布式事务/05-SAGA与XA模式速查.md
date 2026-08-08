# 05 Seata SAGA 与 XA 模式速查

> 长事务的两端解：**SAGA 状态机编排（补偿链）适合超长流程**；**XA 数据库原生 2PC 适合强一致金融核心**——本篇覆盖两种模式的状态机定义、注解式、XA 数据源代理与适用边界。

---

## 📚 目录

1. [SAGA 模式定位](#1-saga-模式定位)
2. [SAGA 核心模型：正向事务 + 补偿链](#2-saga-核心模型正向事务--补偿链)
3. [状态机引擎（JSON 定义）](#3-状态机引擎json-定义)
4. [注解式 SAGA](#4-注解式-saga)
5. [XA 模式：数据库原生 2PC](#5-xa-模式数据库原生-2pc)
6. [SAGA 与 XA 的边界](#6-saga-与-xa-的边界)
7. [面试高频问题](#7-面试高频问题)

---

## 1. SAGA 模式定位

**SAGA 把长事务拆成一系列本地短事务，每个短事务配一个补偿操作**——任一步失败，从失败点**反向**执行补偿（类似"撤销操作栈"）。Seata 提供**状态机引擎**（JSON 编排）与**注解式**（`@SagaTransactional` + `@Compensable`）两种实现。

| 对比项 | AT/TCC | SAGA |
|--------|--------|------|
| 定位 | 短事务（秒级） | **长事务（分钟级/小时级）** |
| 结构 | 单个事务 + 全局锁/预留 | **无锁，补偿链串行推进** |
| 隔离性 | 有全局锁（AT） | **无隔离保证**（脏读可能） |
| 适用 | 下单/扣减 | 订单履约全流程、预订、跨机构调用 |
| 复杂度的位置 | 写三方法 | 设计补偿链（正向 N 个 + 反向 N-1 个） |

> 🎯 一句话选型：**"业务要跑几十秒甚至几分钟、经过 5+ 个服务、中途还有人工环节？"**——锁不住（全局锁早超时），补偿又写不动（TCC 三方法 × N 服务）——**SAGA 的正向短事务 + 反向补偿链是唯一可行解**。

## 2. SAGA 核心模型：正向事务 + 补偿链

```text
正向执行（T1→T4 全部成功）：
    T1 下单 → T2 扣款 → T3 出库 → T4 发货     → 完成

失败回滚（T3 失败）：
    T1 下单 → T2 扣款 → [T3 出库 失败]
        └─ C2 退钱 → C1 取消订单               → 反向补偿

特点：
    ├─ 每个 T 是独立本地事务（自己提交，无全局锁）
    ├─ 补偿 C 是业务真实代码（如"调退款接口"）
    └─ 中间状态对他人可见（无隔离）——需业务接受
```

| 元素 | 说明 |
|------|------|
| 正向事务 T | 每步独立提交的本地事务（或远程调用） |
| 补偿 C | 与 T 配对的反向操作（**必须可逆**，如退款/取消单） |
| 状态机 | 编排 T/C 顺序、分支条件、重试策略、超时 |
| 异常处理 | 补偿失败 → 人工干预（SAGA 无法自动完成） |

> ⚠️ **SAGA 铁律：补偿操作必须幂等**——补偿可能被重试多次；且**正向与补偿之间不能完全逆运算**时（如发了货），补偿只能做到"能接受"的程度（运费不退等业务约定）。

## 3. 状态机引擎（JSON 定义）

Seata SAGA 状态机（seata-saga 模块）：JSON 定义状态流转，Server/客户端引擎执行。

```json
{
  "name": "orderFulfillment",
  "startState": "createOrder",
  "states": {
    "createOrder": {
      "type": "ServiceTask",
      "serviceName": "orderService",
      "serviceMethod": "create",
      "next": "deductBalance",
      "CompensateState": "cancelOrder"          // 失败时的补偿
    },
    "deductBalance": {
      "type": "ServiceTask",
      "serviceName": "accountService",
      "serviceMethod": "deduct",
      "next": "shipGoods",
      "CompensateState": "refundBalance"
    },
    "shipGoods": {
      "type": "ServiceTask",
      "serviceName": "logisticsService",
      "serviceMethod": "ship",
      "end": true,
      "CompensateState": "cancelShip"
    },
    "cancelOrder": { "type": "ServiceTask", "serviceName": "orderService", "serviceMethod": "cancel", "end": true },
    "refundBalance": { "type": "ServiceTask", "serviceName": "accountService", "serviceMethod": "refund", "next": "cancelOrder" },
    "cancelShip": { "type": "ServiceTask", "serviceName": "logisticsService", "serviceMethod": "cancelShip", "next": "refundBalance" }
  }
}
```

```java
// 发起 SAGA 流程（通过 SagaTemplate / 状态机引擎）
@GlobalTransactional
public void fulfill(OrderDTO dto) {
    // 状态机引擎按 JSON 定义执行，失败自动走补偿链
    sagaTemplate.execute("orderFulfillment", dto);
}
```

| 状态类型 | 说明 |
|---------|------|
| ServiceTask | 调服务方法（正向或补偿） |
| ScriptTask/Choice/ForkJoin | 脚本、分支、并行编排 |
| 重试/超时 | 状态可配 retry 与 timeout，失败转补偿 |

> 💡 状态机的优势：**流程可视化、可编排分支/并行/超时**，适合固定业务流；代价是 JSON 维护成本（业务变流程要改状态机）。

## 4. 注解式 SAGA

轻量场景可用注解（不写 JSON，直接标注补偿方法）：

```java
@Service
public class OrderSagaService {

    /** 正向操作 */
    @SagaTransactional(rollbackFor = Exception.class)
    public boolean createOrder(OrderDTO dto) {
        orderMapper.insert(dto);
        accountService.deduct(dto.getUserId(), 100);   // 失败时走补偿
        return true;
    }

    /** 补偿操作：成功返回 true 才真正执行 */
    @Compensable(compensateMethod = "compensateCreateOrder")
    public void createOrderForCompensate(OrderDTO dto) {
        // 实际业务（与上面 createOrder 共用）
    }

    public void compensateCreateOrder(OrderDTO dto) {
        orderMapper.deleteByOrderId(dto.getOrderId()); // 反向操作
        accountService.refund(dto.getUserId(), 100);
    }
}
```

> ⚠️ 注解式限制：`@Compensable` 的补偿方法**必须与正向方法同参数签名**；正向方法需手动声明补偿映射。**流程复杂时 JSON 状态机远优于注解式**。

## 5. XA 模式：数据库原生 2PC

### 5.1 原理

XA 基于数据库原生 2PC 协议：**一阶段各库执行 SQL 但不提交（Prepare），二阶段全部准备成功才提交，任一失败全部回滚**——Seata 只做协调（TC），事务性由数据库保证。

```text
XA 流程：
    TM 开启全局事务（XID）→ 各 RM 用 XA 连接执行 SQL
        ① 一阶段：各库 PREPARE（数据锁定在库内）
        ② 二阶段：全部 OK → 各库 COMMIT；任一失败 → 各库 ROLLBACK
```

### 5.2 配置与代码

```xml
<dependency>
    <groupId>io.seata</groupId>
    <artifactId>seata-rm-datasource</artifactId>
    <version>2.6.0</version>
</dependency>
```

```java
// XA 数据源代理（替代默认 DataSourceProxy）
@Bean
public DataSource dataSource() {
    XADataSource xaDs = new MysqlXADataSource();   // 需 XA 驱动
    xaDs.setUrl(jdbcUrl); xaDs.setUser(user); xaDs.setPassword(pwd);
    return new DataSourceProxyXA(xaDs);            // ★ Seata 的 XA 代理
}

// 业务侧与 AT 相同：@GlobalTransactional 即可（分支类型由数据源自动识别）
@GlobalTransactional(rollbackFor = Exception.class)
public void transfer(TransferDTO dto) { ... }
```

### 5.3 XA 的代价与适用

| 维度 | XA | AT |
|------|-----|-----|
| 一致性 | **强一致（严格 ACID）** | 最终一致 |
| 性能 | **低**：一阶段 Prepare 后**锁保持到二阶段**，连接全程占用 | 高（一阶段即提交） |
| 侵入性 | 无（协议标准） | 无（自动） |
| 数据库要求 | 必须支持 XA（MySQL/PostgreSQL/Oracle 都支持） | 任意 SQL 库 |
| 适用 | 金融核心/强一致小并发 | 默认首选 |

> 🎯 面试必答：**"XA 和 Seata AT 区别？"**——XA 的"不提交"由**数据库协议**保证（Prepare 后锁不释放），强一致但连接占用久、并发上不去；AT 一阶段就提交（释放锁），用 undo_log + 全局锁做**补偿式**最终一致，性能高但隔离弱——**强一致选 XA，性能与可用性选 AT**。

## 6. SAGA 与 XA 的边界

| 场景 | SAGA | XA |
|------|:---:|:---:|
| 超长流程（分钟级） | ✅ | ❌（锁不住） |
| 跨机构/第三方接口（不可回滚） | ✅（补偿=退款） | ❌（非 XA 协议） |
| 强一致金融核心（秒级） | ❌ | ✅ |
| 高并发 | ✅（无锁） | ❌（锁瓶颈） |
| 隔离性要求 | ❌（无隔离） | ✅（库级隔离） |

| 对比项 | SAGA | XA |
|--------|------|-----|
| 一致性 | 最终一致 | 强一致 |
| 锁 | 无锁 | Prepare 后持锁到二阶段 |
| 开发量 | 补偿链/状态机（高） | 零（协议） |
| 数据库 | 任意 | 必须支持 XA |
| 典型场景 | 订单履约、预订、跨机构 | 银行核心、数据迁移 |

> ⚠️ **SAGA 补偿失败的兜底**：补偿链中断 → **必须告警 + 人工介入**（补偿状态机支持断点续跑，但语义无法自动补全）。

## 7. 面试高频问题

| 问题 | 一句话答案 |
|------|-----------|
| SAGA 适合什么？ | 长事务、多服务、无强一致要求（履约/预订） |
| SAGA 和 TCC 的区别？ | TCC 是单事务的三阶段；SAGA 是事务链的正向+补偿编排 |
| SAGA 的隔离性？ | 无隔离保证（中间状态可见），业务需接受 |
| XA 为什么慢？ | Prepare 后库锁保持到二阶段，连接占用久 |
| XA 和 Seata 什么关系？ | Seata 做协调（TC），一致性由数据库 XA 协议保证 |
| XA 需要什么前置？ | 数据库支持 XA 协议 + 驱动（MysqlXADataSource 等） |

---

**下一模块**：[06-模式选型与对比](06-模式选型与对比.md)　**返回总览**：[00-Seata知识体系总览](00-Seata知识体系总览.md)

**【参考来源】**：[Seata SAGA 模式官方文档](https://seata.incubator.apache.org/zh-cn/docs/v1.0/user/mode/saga/)、[Seata XA 模式官方文档](https://seata.incubator.apache.org/zh-cn/docs/v1.0/user/mode/xa/)、[Seata 四大模式与适用场景（阿里云开发者）](https://developer.aliyun.com/article/1739881)
