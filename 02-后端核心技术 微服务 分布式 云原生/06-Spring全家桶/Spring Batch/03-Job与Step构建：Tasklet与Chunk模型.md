# 03 Job 与 Step 构建：Tasklet 与 Chunk 模型

> 构建 API 从"工厂 + 字符串名"进化为"构造器注入 JobRepository"，Chunk 执行模型在 6.0 迎来重写版（ChunkOrientedStep 转正）——本章讲清两种执行模型、流程分支控制与监听器体系，并给出一个完整的端到端示例

---

## 📚 目录

1. [构建器 API 演进与 6.0 新写法](#1-构建器-api-演进与-60-新写法)
2. [Step 的三种类型](#2-step-的三种类型)
3. [Tasklet 模型](#3-tasklet-模型)
4. [Chunk 模型与执行细节](#4-chunk-模型与执行细节)
5. [Flow、Decision 与条件分支](#5-flowdecision-与条件分支)
6. [监听器体系](#6-监听器体系)
7. [JobParametersIncrementer](#7-jobparametersincrementer)
8. [完整示例：CSV 导入 Job](#8-完整示例csv-导入-job)

---

## 1. 构建器 API 演进与 6.0 新写法

| 版本 | 典型写法 | 问题 |
|------|---------|------|
| 4.x | `JobBuilderFactory.get("job")...` / `StepBuilderFactory.get("step")...` | 工厂隐藏依赖 |
| 5.x | `new JobBuilder("job").repository(jobRepository)` / `.repository()` 显式声明 | 依赖仍可后置，易漏配 |
| **6.0** | `new JobBuilder("job", jobRepository)` / `new StepBuilder("step", jobRepository)` | **构造器强制注入**，`JobBuilderFactory`/`StepBuilderFactory` 已移除 |

```java
// 6.0 标准写法：JobRepository 通过构造器传入
@Bean
Step step1(JobRepository jobRepository) {
    return new StepBuilder("step1", jobRepository)
            .tasklet(...)
            .build();
}

@Bean
Job job1(JobRepository jobRepository, Step step1) {
    return new JobBuilder("job1", jobRepository)
            .start(step1)
            .build();
}
```

> ⚠️ 迁移注意：5.x 中"先 new Builder 再 `.repository(jobRepository)`"的写法在 6.0 中已不再支持——JobBuilder/StepBuilder 的**单参构造器与所有已废弃 API 均被移除**，必须走双参构造器。

## 2. Step 的三种类型

| Step 类型 | 构建方式 | 适用场景 |
|-----------|---------|---------|
| TaskletStep | `.tasklet(tasklet)` | 单块逻辑：初始化、清理、文件搬运、复杂非线性处理 |
| Chunk 步 | `.<I,O>chunk(n).reader().processor().writer()` | 数据流式处理（绝大多数批处理） |
| PartitionStep | `.partitioner()` + `.partitionHandler()` | 并行扩展（模块 07） |
| FlowStep / JobStep | `.flow(flow)` / `.job(job)` | 复用已有 Flow / 嵌套执行其他 Job（6.0 中 JobStep 改用 JobOperator 启动） |

此外 6.0 引入 **StoppableStep** 接口：任何 Step 类型都能响应外部停止信号（此前只有 Tasklet 步支持），为"优雅停机"（模块 08）打底。

## 3. Tasklet 模型

Tasklet 是"一次 execute 调用完成一段逻辑"的执行单元，返回 `RepeatStatus` 控制重复：

```java
public interface Tasklet {
    RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception;
}
```

- `RepeatStatus.FINISHED`：本步骤完成，Step 结束；
- `RepeatStatus.CONTINUABLE`：**框架会再次调用 execute()**（适合"分批消费队列直到取空"的自循环模式）。

典型用法：

```java
@Bean
Step cleanupStep(JobRepository jobRepository) {
    return new StepBuilder("cleanup", jobRepository)
            .tasklet((contribution, chunkContext) -> {
                // 清理过期临时文件 / 初始化目录 / 发送通知
                FileUtils.forceDelete(new File("/tmp/staging"));
                return RepeatStatus.FINISHED;
            })
            .build();
}
```

> 💡 分界经验：**有"数据流 + 逐条处理"结构的用 Chunk，一次性动作用 Tasklet**。把整个业务写进一个 Tasklet 是新手最常见的错误——既失去事务边界，也失去重启粒度。

## 4. Chunk 模型与执行细节

### 4.1 6.0 的 Chunk 重写

- 5.1 中实验性的 **ChunkOrientedStep 在 6.0 转正**，取代 `ChunkOrientedTasklet` / `TaskletStep` 成为 Chunk 步的标准实现；旧实现保留（已废弃）供 v6 整代过渡；
- retry 从 Spring Retry 换为 **Spring Framework 7 内置 Retry**（模块 06）；
- 并发模型从"并行迭代"换为**生产者-消费者 + 有界队列**（模块 07）。

### 4.2 构建与事务

```java
@Bean
Step importStep(JobRepository jobRepository,
                PlatformTransactionManager transactionManager,
                ItemReader<Order> reader,
                ItemProcessor<Order, Order> processor,
                ItemWriter<Order> writer) {
    return new StepBuilder("import", jobRepository)
            .<Order, Order>chunk(1000)              // commit-interval = 1000
            .transactionManager(transactionManager) // 6.0 推荐拆分写法
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .build();
}
```

版本差异：5.x/6.0 早期写法 `.chunk(1000, transactionManager)` 的双参形式已在 6.0 废弃（迁移指南建议改 `.chunk(1000).transactionManager(tx)` 或新 `ChunkOrientedStepBuilder`）。

### 4.3 Chunk 执行循环与事务边界

```text
while (StepExecution 未结束) {
    读 chunkSize 条（read 到 null 表示数据耗尽）
    → 逐条 processor 处理（null 返回 = 过滤）
    → 一次性 writer 写入
    → 提交事务（commit）
    → 把 StepExecution/计数器/上下文持久化到元数据
}
任何一步抛异常 → 回滚当前块 → 按容错策略（模块 06）处理
```

三个必须记住的语义：

1. **一个 chunk = 一个事务**：commit-interval=1000 时，10 万条数据 = 100 次提交，失败只回滚最后一次未提交的块；
2. **commit 前先写元数据**：重启续跑的正确性来自"元数据与业务数据几乎同步落盘"（模块 01）；
3. **commit-interval 的选择**：过大 → 内存压力 + 回滚成本高；过小 → 事务开销大。经验值：JDBC 批量写 500~2000，重处理逻辑 100~500，文件写 1000~5000，需实测。

### 4.4 @StepScope 与延迟绑定

`.reader()` 等方法默认在 Step 执行时才解析 Bean（Step scope），允许把 JobParameters 注入 reader 属性：

```java
@Bean
@StepScope
FlatFileItemReader<Order> orderReader(
        @Value("#{jobParameters['input.file']}") Resource file) {
    return new FlatFileItemReaderBuilder<Order>()
            .resource(file)
            // ...
            .build();
}
```

> ⚠️ 6.0.4 修复：`@StepScope` + 本地 chunking（模块 07）组合下曾触发 `ScopeNotActiveException`，升级到 6.0.4 及以上即可规避。

## 5. Flow、Decision 与条件分支

### 5.1 顺序执行

```java
new JobBuilder("job", jobRepository)
        .start(step1)      // 顺序执行
        .next(step2)
        .next(step3)
        .build();
```

### 5.2 条件分支（ExitStatus 驱动）

```java
new JobBuilder("job", jobRepository)
        .start(step1)
        .on("FAILED").to(alertStep)     // 按 ExitCode 分支
        .on("*").to(step2)              // 通配：其余情况
        .end()                          // 结束 Flow
        .build();

// 多分支 + 决策器
new JobBuilder("job", jobRepository)
        .start(step1)
        .on("FAILED").fail()            // 标记 Job 失败
        .on("COMPLETED").to(decider)    // 先走到决策器
        .from(decider).on("A").to(stepA)
        .from(decider).on("B").to(stepB)
        .end()
        .build();
```

`JobExecutionDecider` 示例：

```java
public class AmountDecider implements JobExecutionDecider {
    @Override
    public FlowExecutionStatus decide(JobExecution jobExecution, StepExecution stepExecution) {
        return new FlowExecutionStatus(shouldProcessA() ? "A" : "B");
    }
}
```

分支判断用的是 **ExitStatus（退出码）而非 BatchStatus**——自定义 ExitCode 即可定义自己的分支语言：

```java
stepExecution.setExitStatus(new ExitStatus("HAS_ERROR_ROW")); // 6.0 中 ExitStatus 不可变，通过 Step 监听器设置后由框架传播
```

### 5.3 Flow 组合与状态流式语法

| API | 语义 |
|-----|------|
| `.on("FAILED").to(step2)` | 匹配失败则跳转 |
| `.from(step1).on("X").to(stepY)` | 从已有节点续写分支 |
| `.stopAndRestart(step)` | 停止并标记从某步重启 |
| `.end()` / `.end("CODE")` | 正常结束（或带自定义退出码结束） |
| `.fail()` / `.stop()` | 标记失败 / 停止 |
| `.next(flow)` | 嵌入子 Flow |

> 🎯 核心辨析：`.on()` 匹配的是 **ExitStatus 的 code**；同一 Step 的 BatchStatus 是 COMPLETED 时也可能因为自定义 ExitCode 走到"失败"分支——**别用 BatchStatus 猜分支走向**。

## 6. 监听器体系

6.0 把监听器统一归入 `org.springframework.batch.core.listener` 包（5.x 分散在各处）。按执行粒度分四层：

| 监听器 | 回调时机 | 典型用途 |
|--------|---------|---------|
| JobExecutionListener | beforeJob / afterJob | 作业前后清理、通知、全局参数校验 |
| StepExecutionListener | beforeStep / afterStep | 初始化 / 收尾、记录进度到 ExecutionContext |
| ChunkListener | beforeChunk / afterChunk / afterChunkError | 块级监控、事务边界埋点 |
| ItemReadListener / ItemProcessListener / ItemWriteListener | 每个 item 前后 + onReadError 等 | 逐条审计、错误采样 |

容错类监听器（模块 06）：`SkipListener`（onSkipInRead/Write/Process）、`RetryListener`（onError/open/close）。

```java
// 用 StepListener 组合注册多个监听器
new StepBuilder("step", jobRepository)
        .tasklet(tasklet)
        .listener(new JobExecutionListener() {
            @Override public void beforeJob(JobExecution jobExecution) { /* ... */ }
            @Override public void afterJob(JobExecution jobExecution) { /* ... */ }
        })
        .build();
```

> ⚠️ 注意：Job 级别的 `JobExecutionListener` 直接注册在 Job 上（`new JobBuilder(...).listener(...)`）；Step 级别的注册在 StepBuilder 上。6.0 中旧的 listener 支持类（如 `ItemListenerSupport` 系列）已被移除，统一用接口 + default 方法或 lambda。

## 7. JobParametersIncrementer

用于"每次启动自动生成下一组参数"（如按天跑批时自动 +1 天），保证每次启动都是**新实例**：

```java
new JobBuilder("dailyReport", jobRepository)
        .incrementer(new RunIdIncrementer())   // 每次启动参数里 run.id +1
        .start(step1)
        .build();
```

| 内置实现 | 行为 |
|---------|------|
| RunIdIncrementer | 维护 `run.id` 参数自增 |
| SimpleJobParametersIncrementer | 维护 `timestamp` 参数（当前时间） |
| 自定义 | 实现 `increment(JobParameters) → JobParameters` |

> ⚠️ **6.0 行为变更**：Job 配置了 Incrementer 后，**启动时用户显式传入的参数会被忽略并打印 warning**——框架完全以 incrementer 计算出的参数为准（避免与实例身份判定冲突）。5.x 中"传入参数覆盖 incrementer"的行为不再成立。

## 8. 完整示例：CSV 导入 Job

一个"读取订单 CSV → 过滤金额为负 → 批量写入数据库"的 6.0 完整 Job：

```java
@Configuration(proxyBeanMethods = false)
class OrderImportJobConfig {

    @Bean
    Job importJob(JobRepository jobRepository, Step importStep,
                  Step notificationStep) {
        return new JobBuilder("orderImport", jobRepository)
                .start(importStep)
                .on("FAILED").to(notificationStep)
                .on("*").end()
                .end()
                .listener(new JobExecutionListener() {
                    @Override
                    public void afterJob(JobExecution jobExecution) {
                        log.info("job done: {}, status={}",
                                jobExecution.getJobInstance().getJobName(),
                                jobExecution.getExitStatus());
                    }
                })
                .build();
    }

    @Bean
    Step importStep(JobRepository jobRepository,
                    PlatformTransactionManager txManager,
                    @Qualifier("orderReader") ItemReader<Order> reader,
                    @Qualifier("orderWriter") ItemWriter<Order> writer) {
        return new StepBuilder("import", jobRepository)
                .<Order, Order>chunk(500)
                .transactionManager(txManager)
                .reader(reader)
                .processor(new FunctionItemProcessor<Order, Order>(order -> {
                    if (order.getAmount().signum() < 0) {
                        return null;              // 负金额订单被过滤（FILTER_COUNT+1）
                    }
                    return order;
                }))
                .writer(writer)
                .build();
    }

    @Bean
    @StepScope
    FlatFileItemReader<Order> orderReader(
            @Value("#{jobParameters['input.file']}") Resource file,
            OrderRowMapper rowMapper) {
        return new FlatFileItemReaderBuilder<Order>()
                .name("orderReader")
                .resource(file)
                .linesToSkip(1)
                .delimited()
                .names("id", "customerId", "amount")
                .fieldSetMapper(rowMapper)
                .build();
    }

    @Bean
    Step notificationStep(JobRepository jobRepository) {
        return new StepBuilder("notify", jobRepository)
                .tasklet((c, ctx) -> {
                    log.warn("order import failed, notify ops");
                    return RepeatStatus.FINISHED;
                })
                .build();
    }
}
```

> 💡 注释中的执行语义：`processor` 返回 null 的条目被过滤（FILTER_COUNT）；chunk(500) 每 500 条一个事务；`@StepScope` 让 reader 在步骤执行时才绑定 `jobParameters['input.file']`。

---

> 🎯 **核心要点**：6.0 构建 API 是"构造器注入 JobRepository"——JobBuilder/StepBuilder 双参构造，工厂类已成历史；Chunk 步以新 ChunkOrientedStep 实现，`chunk(N)` 与 `transactionManager()` 分离写法；条件分支用 ExitStatus 驱动 `.on("...")`；监听器四层粒度 + Skip/Retry 监听器构成完整横切能力。

**下一模块**：[04-ItemReader体系](04-ItemReader体系.md)　|　**返回总览**：[00-Spring Batch知识体系总览](00-Spring Batch知识体系总览.md)
