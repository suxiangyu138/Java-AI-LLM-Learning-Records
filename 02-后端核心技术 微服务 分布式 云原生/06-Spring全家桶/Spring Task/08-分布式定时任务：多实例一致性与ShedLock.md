# 08 分布式定时任务：多实例一致性与 ShedLock

> 单实例的 @Scheduled 在微服务化后必然重复执行——ShedLock 用一把"分布式锁"让任务在集群中只跑一次；本模块讲透多实例问题的本质、ShedLock 机制、Quartz/XXL-Job 选型与锁设计细节

---

## 📚 目录

1. [问题本质：多实例下 @Scheduled 会执行几次](#1-问题本质多实例下-scheduled-会执行几次)
2. [ShedLock：分布式调度锁](#2-shedlock分布式调度锁)
3. [锁的实现存储与过期设计](#3-锁的实现存储与过期设计)
4. [ShedLock 的适用边界与局限](#4-shedlock-的适用边界与局限)
5. [Quartz 与 XXL-Job 对比选型](#5-quartz-与-xxl-job-对比选型)
6. [与 Redis 分布式锁的异同](#6-与-redis-分布式锁的异同)
7. [分布式任务架构设计](#7-分布式任务架构设计)

---

## 1. 问题本质：多实例下 @Scheduled 会执行几次

```text
K8s 部署 3 个副本，每个副本都跑着同一个 @Scheduled 任务：

副本A ──┐
副本B ──┼── 同时触发 → 任务执行 3 次！
副本C ──┘

后果：
  数据同步 → 3 份重复数据
  发通知 → 用户收到 3 条
  扣费/清点 → 业务错误
```

**为什么必须解决：** @Scheduled 是**实例本地**的调度器——它不知道也不关心其它实例。集群化部署后，"每实例一个调度器"必然产生重复执行。

| 解决思路 | 说明 |
|---------|------|
| 只让一个实例跑任务（如 K8s leader 选举） | 单点、故障转移复杂 |
| 任务幂等（重复执行无害） | 治标不治本，浪费资源 |
| **分布式锁（ShedLock 方案）** | 锁住执行权：谁抢到谁跑 ✅ 主流 |

> 🎯 **核心要点**：分布式任务的第一问题是"**只跑一次**"——ShedLock 通过"抢锁"把执行权交给一个实例，其余实例跳过本轮。这是最轻量、对代码侵入最小的方案。

## 2. ShedLock：分布式调度锁

### 2.1 原理

```text
每个任务执行前：尝试获取分布式锁（如 Redis SETNX / DB 行锁）
  获取成功 → 执行任务
  获取失败 → 跳过本轮（日志记录）
执行后/超时：释放锁
```

**注意**：ShedLock 不是调度器——它不负责"何时触发"，只负责"触发后谁执行"。调度仍由各实例的 @Scheduled 完成（每个实例都触发，但只有持锁者执行）。

### 2.2 集成（Redis 实现）

```xml
<dependency>
    <groupId>net.javacrumbs.shedlock</groupId>
    <artifactId>shedlock-spring</artifactId>
    <version>5.x</version>          <!-- 以官方最新为准 -->
</dependency>
<dependency>
    <groupId>net.javacrumbs.shedlock</groupId>
    <artifactId>shedlock-provider-redis-spring</artifactId>
    <version>5.x</version>
</dependency>
```

```java
@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "10m")      // 全局默认锁最长时间
public class SchedulerConfig {

    @Bean
    public LockProvider lockProvider(RedisConnectionFactory factory) {
        return new RedisLockProvider(factory);
    }
}

// 任务上锁
@Component
public class DistributedTask {

    @Scheduled(fixedDelay = 60_000)
    @SchedulerLock(name = "syncOrders",                    // 锁名（跨实例唯一标识）
            lockAtMostFor = "10m",                         // 锁最长时间（防死锁）
            lockAtLeastFor = "5s")                         // 锁最短时间（防并发触发窗口）
    public void syncOrders() { ... }
}
```

### 2.3 两个锁时间参数（核心）

| 参数 | 语义 | 设置原则 |
|------|------|---------|
| `lockAtMostFor` | 锁**最长**持有时间（超时自动释放） | 任务最大执行时长 × 1.5（防任务崩溃锁不释放） |
| `lockAtLeastFor` | 锁**最短**持有时间 | 任务正常执行时长的 1-2 倍（防调度抖动导致双跑） |

```text
为什么需要 lockAtLeastFor：
  实例A 执行完（耗时 100ms）→ 释放锁
  实例B 恰好同一毫秒抢锁成功 → 双跑！
  lockAtLeastFor=5s → A 的锁至少持有 5 秒 → B 抢不到 → 防抖
```

> ⚠️ **常见坑**：只配 lockAtMostFor 不配 lockAtLeastFor——短任务在调度时钟漂移下仍有极小概率双跑；`lockAtMostFor` 设得比任务执行时长还短 = 任务未结束锁已释放 = 双跑。

## 3. 锁的实现存储与过期设计

| Provider | 存储 | 适用 |
|---------|------|------|
| `RedisLockProvider` | Redis（SET NX + 过期） | **最常见**（Redis 已有） |
| `JdbcTemplateLockProvider` | DB 表（shedlock 表） | 无 Redis 场景 |
| `ZooKeeperLockProvider` | ZK 临时节点 | 已有 ZK 体系 |

**Redis 锁的要点（对应 Spring Data Redis 体系 04 篇）：**

```text
加锁：SET lock:task:shedlock_xxx <uuid> NX PX lockAtMostFor
      （原子命令——绝不可 SETNX 后再 EXPIRE 两步式）
续期：长任务场景看门狗续期（ShedLock 5.x 支持）
释放：比对持有者再 DEL（防误删他人锁）
```

> 🎯 **要点**：ShedLock 的 Redis 锁与"分布式锁三件套"（原子加锁/续期/比对释放）完全同源——理解了 Spring Data Redis 的 Lua 锁，就理解了 ShedLock 内部。

## 4. ShedLock 的适用边界与局限

| 能力 | ShedLock |
|------|:---:|
| 防止重复执行 | ✅ |
| 任务分片（每实例跑一部分） | ❌ |
| 任务路由（指定实例执行） | ❌ |
| 调度中心统一管理 | ❌ |
| 失败补偿/重试编排 | ❌（自建） |
| 动态 cron | ❌（配合 03 篇方案） |

| 适用 | 不适用 |
|------|--------|
| 轻量任务少（<50 个）、无调度中心 | 任务量大、需要管理台 |
| 已有 Redis/DB 基础设施 | 需要分片、路由、监控面板 |
| 团队熟悉 Spring 生态 | 需要企业级调度平台 |

> 💡 **判断标准**：ShedLock 是"给 @Scheduled 补一个锁"，不是"分布式任务平台"。任务多了、管理需求多了，升级到调度平台（下节）。

## 5. Quartz 与 XXL-Job 对比选型

| 维度 | ShedLock + @Scheduled | Quartz | XXL-Job |
|------|:---:|:---:|:---:|
| 定位 | 轻量锁 | 全功能调度框架 | 分布式任务平台 |
| 部署 | 应用内 | 应用内（可集群） | 独立调度中心 |
| 管理台 | ❌ | 有（需插件） | ✅ 完善 |
| 分片 | ❌ | 手动实现 | ✅ 内置 |
| 故障转移 | 抢锁自然转移 | 支持 | ✅ 内置 |
| 动态 cron | 配合 03 篇 | ✅（JobDetail 更新） | ✅ 界面配置 |
| 学习成本 | 低 | 中 | 中 |
| 场景 | 简单任务、快速接入 | 复杂调度依赖（日历/ misfire 策略） | 任务多、要管理台、大团队 |

```text
选型口诀：
  任务少、想轻量 → ShedLock
  调度复杂（日历、错过策略、持久化 job） → Quartz
  任务多、要管理台/分片/告警 → XXL-Job
```

> ⚠️ **Quartz 注意**：与 Spring 的 `@Scheduled` 是两套体系——Quartz 自己管调度（JobStore 持久化），接入 Spring 需要 `SchedulerFactoryBean` 适配；**不要把两者混用于同一任务**。

## 6. 与 Redis 分布式锁的异同

| 维度 | 业务分布式锁 | ShedLock 任务锁 |
|------|:---:|:---:|
| 目的 | 保护临界区（业务操作互斥） | 防任务重复执行 |
| 持有时间 | 短（业务操作时长） | 与任务周期同量级 |
| 释放方式 | 业务 finally 释放 | 任务结束/超时自动释放 |
| 锁粒度 | 业务 key（如订单号） | 任务名（全局唯一） |
| 实现 | 同一套 Redis 锁原语 | 同一套原语 + 调度语义包装 |

**本质**：两者共用"SET NX + 过期 + 比对释放"的锁原语，只是**用途与粒度不同**——理解了一个，另一个就是换了个场景。

## 7. 分布式任务架构设计

### 7.1 分层架构（生产标准）

```text
┌──────────────────────────────────────────────┐
│ 调度层：每实例 @Scheduled（触发时机各自算）         │
│   + ShedLock 锁（执行权全局唯一）                │
├──────────────────────────────────────────────┤
│ 执行层：任务逻辑（幂等 + 可观测）                 │
│   + 执行记录表 + 指标 + 告警（07 篇）             │
├──────────────────────────────────────────────┤
│ 管理层：动态 cron（03 篇）+ 手动触发 + 监控面板    │
└──────────────────────────────────────────────┘
```

### 7.2 设计检查清单

- [ ] 每个任务有唯一锁名（`@SchedulerLock(name=...)`）；
- [ ] `lockAtMostFor` > 任务最大执行时长（防锁早释放）；
- [ ] `lockAtLeastFor` 防调度抖动双跑；
- [ ] 任务幂等（重跑无害）——锁是兜底不是依赖；
- [ ] 任务崩溃后锁能自动过期（lockAtMostFor 兜底）；
- [ ] 锁存储高可用（Redis 主从/哨兵，见 Spring Data Redis 体系 09 篇）；
- [ ] 心跳监控：任务不执行的实例要有"跳过"日志（区分"没触发"与"被锁跳过"）。

> 🎯 **核心要点**：分布式任务的一致性答案 = **每实例触发 + 全局抢锁**（ShedLock）。三个关键设计：锁名唯一、lockAtMostFor 兜底防死锁、lockAtLeastFor 防抖动双跑；任务本体必须幂等（锁只是概率防御，幂等是确定性防御）。任务规模上去后，选型路径 ShedLock → Quartz → XXL-Job 由"调度复杂度与管理需求"驱动。

---

**上一模块**：[07-定时任务的异常处理与可观测](07-定时任务的异常处理与可观测.md)　**下一模块**：[09-生产实践与面试题](09-生产实践与面试题.md)
