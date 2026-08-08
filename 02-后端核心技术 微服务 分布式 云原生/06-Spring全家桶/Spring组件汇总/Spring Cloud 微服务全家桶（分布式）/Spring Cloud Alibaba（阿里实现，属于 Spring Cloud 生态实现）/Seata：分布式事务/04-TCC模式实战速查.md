# 04 Seata TCC 模式实战速查

> 手工补偿三方法：Try 预留 → Confirm 确认 → Cancel 取消——**高并发扣减/跨非关系库场景的终极方案**；同时讲透空回滚、悬挂、幂等三大经典难题与 TCC-FENCE 防悬挂表。

---

## 📚 目录

1. [TCC 模式定位](#1-tcc-模式定位)
2. [核心概念与执行流程](#2-核心概念与执行流程)
3. [注解式实现（@LocalTCC）](#3-注解式实现localtcc)
4. [三大难题：空回滚、悬挂、幂等](#4-三大难题空回滚悬挂幂等)
5. [TCC-FENCE 防悬挂增强](#5-tcc-fence-防悬挂增强)
6. [适用场景与边界](#6-适用场景与边界)
7. [面试高频问题](#7-面试高频问题)

---

## 1. TCC 模式定位

**TCC（Try-Confirm-Cancel）是把业务逻辑拆成三个可显式调用的方法**：Try 阶段检查 + 预留资源（不真正扣减），Confirm 阶段用预留资源完成真实操作，Cancel 阶段释放预留资源——**一致性由业务代码自己保证，不依赖数据库锁**。

| 对比项 | AT | TCC |
|--------|-----|-----|
| 侵入性 | 无（数据源代理自动） | **高（每个操作写三方法）** |
| 锁 | 全局锁（热点行冲突） | **无全局锁（并发最高）** |
| 一致性 | 最终一致（读未提交） | 业务可控（可设计接近强一致） |
| 数据库依赖 | 依赖事务 + undo_log | **不依赖数据库**（Redis/MongoDB 等也支持） |
| 开发成本 | 低 | 高（三方法 + 三难题处理） |

> 🎯 一句话选型：**AT 是"数据库事务的自动延伸"，TCC 是"业务代码手工编排补偿"**——要极致并发、跨异构存储、精细控制隔离 → TCC（完整选型见 [06 篇](06-模式选型与对比.md)）。

## 2. 核心概念与执行流程

### 2.1 三方法职责

| 方法 | 阶段 | 职责 | 示例（库存扣减） |
|------|------|------|-----------------|
| **Try** | 一阶段 | 检查业务条件 + **预留资源**（冻结/锁定，不真扣） | `UPDATE stock SET frozen=frozen+1 WHERE sku_id=? AND count-frozen>=1` |
| **Confirm** | 二阶段（成功） | 用预留资源完成真实操作（幂等） | `UPDATE stock SET count=count-1, frozen=frozen-1` |
| **Cancel** | 二阶段（失败） | 释放预留资源（幂等） | `UPDATE stock SET frozen=frozen-1` |

### 2.2 执行流程

```text
    TM（发起方）                  TC（Server）               RM（TCC 服务）
        │  begin + Try 调用          │                          │
        │ ──────────────────────────►│──► ① 执行 Try（预留资源）
        │                            │    ② 注册分支（一阶段完成）
        │  全部 Try 成功 → commit     │                          │
        │ ──────────────────────────►│──► ③ Confirm（幂等执行）
        │  任一 Try 失败 → rollback   │                          │
        │ ──────────────────────────►│──► ④ Cancel（幂等执行）
```

> ⚠️ **关键差异（对比 AT）**：AT 的二阶段是"数据自动恢复"，TCC 的二阶段是"**再调一次业务方法**"——Confirm/Cancel 是真实业务代码，由 TC 在二阶段回调执行。

## 3. 注解式实现（@LocalTCC）

Seata 支持注解式 TCC（`@LocalTCC` + `@TwoPhaseBusinessAction`），把三方法写在一个接口里：

```java
@LocalTCC
public interface StockTccService {

    /**
     * Try：预留库存（一阶段）
     * @param actionContext Seata 自动注入的上下文（含 xid/branchId）
     */
    @TwoPhaseBusinessAction(name = "stockTccAction", commitMethod = "confirm", rollbackMethod = "cancel")
    boolean tryDeduct(BusinessActionContext actionContext,
                      @BusinessActionContextParameter(paramName = "skuId") String skuId,
                      @BusinessActionContextParameter(paramName = "count") int count);

    /** Confirm：真正扣减（二阶段，TC 回调，必须幂等） */
    boolean confirm(BusinessActionContext actionContext);

    /** Cancel：释放预留（二阶段，TC 回调，必须幂等） */
    boolean cancel(BusinessActionContext actionContext);
}
```

```java
// 业务侧三方法实现（库存表 + 冻结字段）
public boolean tryDeduct(BusinessActionContext ctx, String skuId, int count) {
    // 幂等检查：本次分支是否已 Try 过
    if (alreadyTried(ctx.getBranchId())) return true;
    int rows = stockMapper.freeze(skuId, count);   // frozen 增加（不扣可用）
    return rows > 0;
}

public boolean confirm(BusinessActionContext ctx) {
    // 幂等：frozen → count 转移（frozen-1, count-1）
    String skuId = ctx.getActionContext("skuId").toString();
    stockMapper.confirmDeduct(skuId, ctx.getActionContext("count"));
    return true;
}

public boolean cancel(BusinessActionContext ctx) {
    // 幂等：释放 frozen
    stockMapper.unfreeze(ctx.getActionContext("skuId"), ctx.getActionContext("count"));
    return true;
}
```

> 💡 `@BusinessActionContextParameter` 标注的参数会被保存进 `BusinessActionContext`，**Confirm/Cancel 阶段用 `ctx.getActionContext(key)` 取回**——所以二阶段不依赖方法参数，只依赖上下文。

## 4. 三大难题：空回滚、悬挂、幂等

TCC 的确认/取消回调**可能迟到、可能重复、可能丢失**，必须业务侧兜底：

### 4.1 空回滚（Cancel 先于 Try）

| 场景 | 问题 |
|------|------|
| Try 网络超时 → TM 判定失败 → TC 发 Cancel；**Try 实际没执行到**（或执行了但未注册分支） | Cancel 找不到预留资源可释放，直接执行会误扣 |

**解法**：Cancel 前判断"该分支的 Try 是否执行过"——**没执行过则跳过（记录标记）**。

### 4.2 悬挂（Try 迟到）

| 场景 | 问题 |
|------|------|
| Try 超时触发 Cancel 后，**迟到的 Try 才执行**——预留资源被冻结却永远不会被 Confirm/Cancel | 资源永久悬挂（泄漏） |

**解法**：Try 前检查"该分支是否已被 Cancel"——**已 Cancel 则拒绝执行 Try**。

### 4.3 幂等（二阶段重复调用）

| 场景 | 问题 |
|------|------|
| TC 与 RM 断连重试、Confirm/Cancel 网络重复 | 重复执行导致多扣/多释放 |

**解法**：Try/Confirm/Cancel 都以 `branchId`（或 xid+branchId）为键做**幂等标记**，已执行直接返回成功。

| 难题 | 场景 | 防法 |
|------|------|------|
| 空回滚 | Cancel 先于 Try | Cancel 检查 Try 标记，无标记跳过 |
| 悬挂 | Try 迟于 Cancel | Try 检查 Cancel 标记，有标记拒绝 |
| 幂等 | 二阶段重复调用 | 三方法都以 branchId 做幂等键 |

## 5. TCC-FENCE 防悬挂增强

Seata 内置 **TCC-FENCE** 解决空回滚/悬挂/幂等：一张 `tcc_fence_log` 表 + 拦截器自动处理。

```sql
-- 建表（官方脚本 seata/script/client/tcc/db/mysql.sql）
CREATE TABLE `tcc_fence_log` (
  `xid` varchar(128) NOT NULL,              -- 全局事务 ID
  `branch_id` bigint NOT NULL,              -- 分支事务 ID
  `action_name` varchar(64) NOT NULL,       -- 动作名（@TwoPhaseBusinessAction name）
  `status` tinyint NOT NULL,                -- 0 尝试中 / 1 已提交 / 2 已回滚 / 3 悬挂
  `gmt_create` datetime NOT NULL,
  `gmt_modified` datetime NOT NULL,
  PRIMARY KEY (`xid`,`branch_id`),
  KEY `idx_gmt_modified` (`gmt_modified`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
```

```yaml
# 开启 TCC-FENCE（默认 true）
seata:
  tcc:
    fence:
      enable: true
      log-table-name: tcc_fence_log
```

| 机制 | 实现 |
|------|------|
| 幂等 | 以 (xid, branch_id) 唯一键插入 fence 记录，重复执行插入失败 → 直接返回成功 |
| 空回滚 | Cancel 时 fence 状态非 Try 完成 → 跳过执行（记悬挂） |
| 悬挂 | Try 时若已有 Cancel 标记（status=2）→ 拒绝执行并记录 status=3 |
| 清理 | 定期清理超时 fence 日志 |

> 🎯 实践建议：**TCC 一律开 FENCE**（默认开）——三难题手工实现容易漏，FENCE 表一行配置全兜住；代价是每分支多一次表写入（性能可接受）。

## 6. 适用场景与边界

| 场景 | 适合？ | 说明 |
|------|:---:|------|
| 高并发热点扣减（秒杀/库存） | ✅ | 无全局锁，并发上限最高 |
| 跨异构存储（MySQL + Redis + Mongo） | ✅ | 不依赖数据库事务 |
| 金融转账强一致 | ✅ | Try 冻结 → Confirm 划转，业务可控 |
| 第三方支付对接（不可回滚只能退款） | ✅ | Cancel = 调退款接口（补偿式） |
| 业务简单、快速迭代 | ❌ | 三方法开发成本高，AT 更划算 |
| 超长事务（>1 分钟） | ❌ | 那是 SAGA 的领域（[05 篇](05-SAGA与XA模式速查.md)） |

> ⚠️ **TCC 最大的隐性成本**：表结构要改造（预留字段 frozen）、三方法要幂等、失败路径要脑内推演——**只在高并发/异构存储/精细隔离三个硬需求下选它**，否则 AT 更合适。

## 7. 面试高频问题

| 问题 | 一句话答案 |
|------|-----------|
| TCC 和 AT 的本质区别？ | AT 自动反向 SQL 恢复数据；TCC 二阶段回调业务写的 Confirm/Cancel |
| Try 阶段为什么是"预留"而不是"扣减"？ | 预留可逆，Confirm 才不可逆——保证 Cancel 一定能释放 |
| 空回滚怎么发生？ | Try 超时但没执行到，Cancel 先到 |
| 悬挂怎么发生？ | Cancel 后迟到的 Try 才执行，资源永久冻结 |
| 幂等怎么保证？ | (xid, branch_id) 唯一键标记（TCC-FENCE） |
| TCC 支持非数据库吗？ | 支持（不依赖 DB 事务，Redis 等也能 TCC） |
| TCC 隔离性怎么控制？ | 业务在 Try 里自行加锁/检查（Seata 不提供全局锁） |

---

**下一模块**：[05-SAGA与XA模式速查](05-SAGA与XA模式速查.md)　**返回总览**：[00-Seata知识体系总览](00-Seata知识体系总览.md)

**【参考来源】**：[Seata TCC 模式官方文档](https://seata.incubator.apache.org/zh-cn/docs/v1.0/user/mode/tcc/)、[Seata 四大模式与适用场景（阿里云开发者）](https://developer.aliyun.com/article/1739881)、[Seata AT、TCC、SAGA、XA 模式选型](https://cloud.tencent.cn/developer/article/2160777)
