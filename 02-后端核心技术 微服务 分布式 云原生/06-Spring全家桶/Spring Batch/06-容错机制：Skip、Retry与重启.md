# 06 容错机制：Skip、Retry 与重启

> 批处理与普通接口开发最大的分野在于"遇到坏数据怎么办"——Skip 跳过、Retry 重试、重启续跑，三者构成 Spring Batch 的容错三角；6.0 将 retry 内核从 Spring Retry 换成 Spring Framework 7 内置 Retry

---

## 📚 目录

1. [容错三角与失败分类](#1-容错三角与失败分类)
2. [Skip：跳过坏数据](#2-skip跳过坏数据)
3. [Retry：重试可恢复错误](#3-retry重试可恢复错误)
4. [重启：续跑而非重跑](#4-重启续跑而非重跑)
5. [事务与回滚语义](#5-事务与回滚语义)
6. [容错组合与完整示例](#6-容错组合与完整示例)

---

## 1. 容错三角与失败分类

| 失败类型 | 典型例子 | 对策 |
|---------|---------|------|
| 坏数据（永久失败） | 字段缺失、格式非法、外键不存在 | **Skip**（跳过并计数） |
| 瞬时故障（可恢复） | 连接超时、锁等待、限流 429 | **Retry**（退避重试） |
| 进程级失败 | 宕机、OOM、人工停止 | **重启/续跑**（元数据断点） |

> 💡 决策口诀：**数据问题 skip，资源问题 retry，进程问题 restart**。把"数据库临时不可用"配成 skip 是生产事故的开端——它会静默丢数据。

## 2. Skip：跳过坏数据

### 2.1 配置

```java
return new StepBuilder("import", jobRepository)
        .<Order, Order>chunk(500)
        .transactionManager(txManager)
        .reader(reader)
        .processor(processor)
        .writer(writer)
        .faultTolerant()                              // 开启容错能力
        .skip(InvalidDataException.class)             // 跳过指定异常
        .skip(ValidationException.class)
        .noSkip(FatalDataException.class)             // 排除（即使前面配了父类）
        .skipLimit(100)                               // 全 Step 跳过上限
        .build();
```

### 2.2 三类 Skip 的语义差异（高频考点）

| 类别 | 触发点 | 计数 | 数据去向 | 说明 |
|------|--------|------|---------|------|
| 读跳过 ReadSkip | `read()` 抛异常 | READ_SKIP_COUNT | 该行被丢弃 | 文件损坏行、反序列化失败 |
| 处理跳过 ProcessSkip | `process()` 抛异常 | PROCESS_SKIP_COUNT | **该条不写入**，但**已读的其余条目正常处理** | 校验失败、转换失败 |
| 写跳过 WriteSkip | `write()` 抛异常 | WRITE_SKIP_COUNT | **整个 chunk 的已写部分回滚**，重放时只跳过坏的那条 | 唯一键冲突等——**回滚代价最大** |

写跳过的内部机制（经典实现）：write 失败 → 回滚整个 chunk → 框架对 chunk 内每条数据逐个重写、定位坏数据并跳过 → 其余条目重写成功。因此 **WRITE_SKIP 的 CPU/IO 成本是普通写的好几倍**，应尽力避免触发。

### 2.3 SkipPolicy 与监听器

6.0 起 **skip 完全基于 `SkipPolicy` 接口**（旧代码路径移除）：

```java
public class OrderSkipPolicy implements SkipPolicy {
    @Override
    public boolean shouldSkip(Throwable t, int skipCount) {
        return t instanceof InvalidDataException && skipCount < 100;
    }
}
// 装配：.skipPolicy(new OrderSkipPolicy())  // 与 .skip/.skipLimit 二选一，更精细

// 审计：每次跳过回调
public class OrderSkipListener implements SkipListener<Order, Order> {
    @Override
    public void onSkipInRead(Throwable t) { log.warn("skip read: {}", t.getMessage()); }
    @Override
    public void onSkipInProcess(Order item, Throwable t) { /* 坏数据归档 */ }
    @Override
    public void onSkipInWrite(Order item, Throwable t) { /* 写入失败清单 */ }
}
```

> 💡 生产要求：Skip 必须配监听器落审计表——"跳过了什么、为什么跳"是数据质量报告的输入；skipLimit 上限内还有"超过上限怎么办"：**超限即 Step 失败**，可重启后用不同策略继续。

## 3. Retry：重试可恢复错误

### 3.1 6.0 内核变更

| 版本 | Retry 内核 |
|------|-----------|
| 5.x 及以前 | **Spring Retry**（spring-retry 库，RetryTemplate 机制） |
| 6.0 | **Spring Framework 7 内置 Retry**（并入框架核心，同样的 RetryTemplate 语义） |

对开发者影响：API 形态基本一致（retry-limit、backOffPolicy、RetryListener 概念不变），但依赖从 spring-retry 变为 Spring Framework 内置；旧的 `FaultTolerantStepBuilder`/`SimpleStepBuilder` 已废弃。

### 3.2 配置

```java
return new StepBuilder("import", jobRepository)
        .<Order, Order>chunk(500)
        .transactionManager(txManager)
        .reader(reader).processor(processor).writer(writer)
        .faultTolerant()
        .retry(TransientDataException.class)   // 只对瞬时异常重试
        .retryLimit(3)
        .backOffPolicy(new ExponentialBackOffPolicyBuilder()
                .initialInterval(1_000).multiplier(2).maxInterval(60_000).build())
        .build();
```

| 组件 | 作用 |
|------|------|
| `.retry(异常类)` | 声明可重试异常（按类型匹配，可多个） |
| `.retryLimit(n)` | 每条数据最大重试次数 |
| BackOffPolicy | 重试间隔策略：`FixedBackOffPolicy`（固定）、`ExponentialBackOffPolicy`（指数退避，默认 multiplier 2） |
| `.noRetry(...)` | 排除子类 |
| RetryListener | `onError(retryCount)` 回调：重试次数告警、降级开关 |

> ⚠️ 重试与事务的耦合：Retry 发生在 **chunk 事务内**——重试期间 DB 连接被事务占用，长退避会拉长事务。实践：backoff 上限别超过事务超时；连接池紧张时优先"外部重试"（Job 级重启）而非 Step 内长退避。

## 4. 重启：续跑而非重跑

### 4.1 重启语义

- 失败（FAILED/STOPPED）的 JobExecution，用**相同参数**再次启动 = 重启同一 Instance：框架跳过已 COMMITTED 的 chunk，从断点继续（借助 ExecutionContext）；
- **已提交的块不会重放** → 写入侧必须保证"重放幂等"（模块 05 的 ItemKeyGenerator / ON DUPLICATE KEY）；
- 用不同参数启动 = 新 Instance = 全新执行。

### 4.2 重启控制 API

| API | 语义 |
|-----|------|
| `.startLimit(n)` | Step 最多尝试 n 次（默认 Integer.MAX）；超限 Step 标记 START_LIMIT_EXCEEDED |
| `.allowStartIfComplete(true)` | 即使上次 COMPLETED 也允许重跑（默认 false——已完成 Step 直接跳过） |
| `.preventRestart()` / Job 级 | 禁止作业重启（BATCH_JOB_EXECUTION 不落 RESTARTABLE 记录） |
| `jobParameters.getBoolean("restart")` | 常用外部开关：同参数 + restart=false 时把旧执行置 ABANDONED 再重跑 |

```java
new StepBuilder("import", jobRepository)
        .<Order, Order>chunk(500)
        .startLimit(3)                    // 最多重试 3 次
        .allowStartIfComplete(false)      // 完成过就不再跑
        ...
```

### 4.3 JobOperator 侧的重启（模块 08 展开）

6.0 的 `JobOperator.restart(executionId)` 用上次失败执行的参数重启；`JobOperator.recover(executionId)`（**6.0 新增**）把 FAILED 执行标记为 ABANDONED 以便重新启动——此前只能手工改库。

> ⚠️ 6.0 重启兼容警告（再强调）：**5.x 遗留的 FAILED 实例不能在 6.0 直接重启**（JobParameters 序列化格式变化）——迁移前先 restart 到成功或 recover 掉。

## 5. 事务与回滚语义

### 5.1 回滚边界

```text
chunk 内任一环节异常（未配置容错）：
  read → process → write 全部回滚，StepExecution 置 FAILED

配置 skip 后：
  读异常 → 跳过该行（无事务影响）
  处理异常 → 跳过该条（本 chunk 其余条目正常提交）
  写异常 → 整个 chunk 回滚 → 逐条重放定位坏数据 → 坏数据跳过，其余重写提交
```

### 5.2 非事务资源的回滚陷阱

| 资源 | 事务性 | 风险与对策 |
|------|--------|-----------|
| 数据库（JDBC/JPA） | ✅ 参与 chunk 事务 | 无 |
| 文件写（FlatFileItemWriter） | ❌ | 回滚后文件已写半截 → 用 `shouldDeleteIfEmpty` + 重启覆盖模式，或先写临时文件再原子 rename |
| Kafka/JMS 发送 | ❌（除非配置 broker 事务） | 已发送消息不可回收 → 消费端幂等 + 业务键去重 |
| 外部 API 调用 | ❌ | 幂等接口 + 失败重放安全设计 |

> 🎯 结论：**Skip/Retry 只保证"数据库侧"的强一致，非事务写入必须自带幂等**——这是批处理生产事故的头号来源。

## 6. 容错组合与完整示例

**场景**：导入订单文件，坏行跳过（≤100），数据库锁等待重试（3 次退避），临时文件故障重试后仍失败则 Step 失败、由运维重启续跑。

```java
@Bean
Step importStep(JobRepository jobRepository, PlatformTransactionManager txManager,
                ItemReader<Order> reader, ItemProcessor<Order, Order> processor,
                ItemWriter<Order> writer, OrderSkipListener skipListener) {
    return new StepBuilder("import", jobRepository)
            .<Order, Order>chunk(500)
            .transactionManager(txManager)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .faultTolerant()
            // 数据类异常：跳过
            .skip(InvalidDataException.class)
            .skipLimit(100)
            // 瞬时类异常：退避重试 3 次
            .retry(CannotAcquireLockException.class)
            .retryLimit(3)
            .backOffPolicy(new ExponentialBackOffPolicyBuilder()
                    .initialInterval(1_000).multiplier(2.0).maxInterval(30_000).build())
            // 审计
            .listener(skipListener)
            .build();
}
```

**配套生产策略**：Job 启动入口做"上次 FAILED 则 restart 模式"判断；Step 上加 `startLimit(3)` 防重试风暴；重试耗尽后 Job FAILED → 人工/告警介入 → 修复后同参数重启续跑。

---

> 🎯 **核心要点**：容错三角的选型口诀"数据 skip、资源 retry、进程 restart"；三类 skip 中写跳过代价最大（整块回滚 + 逐条重放）；6.0 的 retry 内核已迁至 Spring Framework 7；重启续跑依赖元数据 + ExecutionContext，但"数据库之外的世界没有回滚"，幂等设计是容错的另一半。

**下一模块**：[07-并行与分布式：多线程、分区与远程执行](07-并行与分布式：多线程、分区与远程执行.md)　|　**返回总览**：[00-Spring Batch知识体系总览](00-Spring Batch知识体系总览.md)
