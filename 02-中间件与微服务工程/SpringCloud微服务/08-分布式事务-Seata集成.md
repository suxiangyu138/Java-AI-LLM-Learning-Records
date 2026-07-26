# 08 - 分布式事务：Seata 集成

> 🎯 Seata 是阿里开源的分布式事务解决方案 — AT 模式无侵入、TCC 模式强隔离、性能与一致性可选的四种模式覆盖所有微服务事务场景

---

## 目录

1. [Seata 架构概述](#1-seata-架构概述)
2. [TC Server 部署](#2-tc-server-部署)
3. [AT 模式实战](#3-at-模式实战)
4. [TCC 模式实战](#4-tcc-模式实战)
5. [AT vs TCC 选型](#5-at-vs-tcc-选型)

---

## 1. Seata 架构概述

```text
Seata 三大角色：

  TM (Transaction Manager) — 事务管理器
    → 定义全局事务边界（@GlobalTransactional）
    → 负责开启/提交/回滚全局事务

  RM (Resource Manager) — 资源管理器
    → 管理分支事务（本地事务）
    → 向 TC 注册分支、报告状态

  TC (Transaction Coordinator) — 事务协调器
    → Seata Server，独立部署
    → 协调全局事务的提交/回滚
```

### 四种模式对比

| 模式 | 一致性 | 性能 | 业务侵入 | 适用 |
|------|:---:|:---:|:---:|------|
| **AT** | 弱 | ⭐⭐⭐ 高 | 无 | ⭐ 通用 CRUD，默认模式 |
| **TCC** | 强 | ⭐⭐ 中 | 高（三接口） | 核心支付链路 |
| **Saga** | 最终 | ⭐⭐⭐ 高 | 中（补偿） | 长事务/老系统 |
| **XA** | 强 | ⭐ 低 | 无 | 单体跨库 |

---

## 2. TC Server 部署

```yaml
# docker-compose.yml — Seata Server
version: '3'
services:
  seata-server:
    image: seataio/seata-server:1.8.0
    ports:
      - "8091:8091"
      - "7091:7091"
    environment:
      - SEATA_PORT=8091
      - STORE_MODE=db                     # 存储模式：file / db / redis
      - SEATA_STORE_DB_URL=jdbc:mysql://mysql:3306/seata
      - SEATA_STORE_DB_USER=root
```

### 客户端配置

```xml
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-seata</artifactId>
</dependency>
```

```yaml
# seata-client.yml（每个微服务都要配）
seata:
  tx-service-group: my_tx_group
  service:
    vgroup-mapping:
      my_tx_group: default
    grouplist:
      default: 127.0.0.1:8091
  data-source-proxy-mode: AT
```

---

## 3. AT 模式实战

> ⭐ AT 模式：自动代理数据源 → 生成前后镜像 → Undo Log 回滚。

```text
AT 模式执行流程：

1. @GlobalTransactional 开启全局事务
2. Service-A 执行业务 SQL
   → Seata 代理数据源拦截 SQL
   → 生成前镜像(BEFORE_IMAGE)：SELECT 当前数据
   → 执行 SQL
   → 生成后镜像(AFTER_IMAGE)：修改后的数据
   → 写入 Undo Log（在同一本地事务内）
   → 向 TC 注册分支事务

3. Service-B 同上

4. 全部成功 → TC 通知所有 RM 删除 Undo Log

5. 任一失败 → TC 通知所有 RM 回滚
   → RM 根据 Undo Log 的前后镜像反向补偿
```

```java
@Service
public class OrderService {

    @Autowired
    private AccountClient accountClient;

    @GlobalTransactional   // ⭐ 只需这一个注解！
    public void createOrder(OrderRequest req) {
        // 1. 本地事务：创建订单
        orderMapper.insert(req.toOrder());

        // 2. 远程调用：扣减余额
        accountClient.deduct(req.getUserId(), req.getAmount());

        // 任何一步抛异常 → 全局回滚
    }
}
```

```sql
-- Seata AT 模式需要在每个业务数据库创建 undo_log 表
CREATE TABLE undo_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    branch_id BIGINT NOT NULL,
    xid VARCHAR(128) NOT NULL,
    context VARCHAR(128),
    rollback_info LONGBLOB,
    log_status INT,
    log_created DATETIME,
    UNIQUE KEY ux_undo_log (xid, branch_id)
);
```

---

## 4. TCC 模式实战

```java
// TCC 接口：每个方法需实现 Try-Confirm-Cancel
public interface InventoryTccAction {

    @TwoPhaseBusinessAction(name = "deductInventory", commitMethod = "commit", rollbackMethod = "cancel")
    boolean prepare(@BusinessActionContextParameter("productId") Long productId,
                    @BusinessActionContextParameter("count") Integer count);

    boolean commit(BusinessActionContext context);

    boolean cancel(BusinessActionContext context);
}

@Service
public class InventoryTccActionImpl implements InventoryTccAction {

    @Override
    public boolean prepare(Long productId, Integer count) {
        // Try: 冻结库存 (stock -= count, frozen += count)
        return inventoryMapper.freeze(productId, count) > 0;
    }

    @Override
    public boolean commit(BusinessActionContext ctx) {
        // Confirm: 扣减冻结库存 (frozen -= count)
        Long productId = (Long) ctx.getActionContext("productId");
        Integer count = (Integer) ctx.getActionContext("count");
        return inventoryMapper.commit(productId, count) > 0;
    }

    @Override
    public boolean cancel(BusinessActionContext ctx) {
        // Cancel: 解冻库存 (stock += count, frozen -= count)
        Long productId = (Long) ctx.getActionContext("productId");
        Integer count = (Integer) ctx.getActionContext("count");
        return inventoryMapper.unfreeze(productId, count) > 0;
    }
}
```

---

## 5. AT vs TCC 选型

| 维度 | AT | TCC |
|------|:---:|:---:|
| 实现成本 | 低（无侵入） | 高（每个操作三接口） |
| 一致性 | 弱（Undo Log 可能失败） | 强（业务层补偿） |
| 性能 | ⭐⭐⭐ | ⭐⭐（多一次 RPC） |
| 全局锁 | ✅ 有，可能冲突 | ❌ 无锁 |
| 适用 | 90% 的 CRUD 场景 | 支付/库存等核心链路 |

> 🎯 **选型原则**：能用 AT 就用 AT（无侵入），AT 搞不定用 TCC（强一致），长事务/老系统用 Saga。AT 虽然简单，但有全局锁 — 高频热点数据慎用。
