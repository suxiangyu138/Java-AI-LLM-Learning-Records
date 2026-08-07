# Spring Batch 知识体系总览

> 从领域模型、JobRepository 元数据、Chunk/Tasklet 执行模型，到 Skip/Retry 容错、分区与远程执行、可观测性——Spring Batch 6.0（2025-11 发布，截至 2026-08 最新 6.0.4）是 Java 生态做**离线大数据量批处理**的事实标准，本体系覆盖 Spring Boot 4.0 + Spring Framework 7.0 时代的最新 API 与最佳实践

---

## 📚 目录

1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [为什么必须系统学透 Spring Batch](#3-为什么必须系统学透-spring-batch)
4. [核心概念速查](#4-核心概念速查)
5. [与周边知识的关系](#5-与周边知识的关系)
6. [学习路线推荐](#6-学习路线推荐)
7. [快速自测 10 题](#7-快速自测-10-题)
8. [版本窗口与参考来源](#8-版本窗口与参考来源)

---

## 1. 知识体系导图

```text
Spring Batch 知识体系（Spring Batch 6.0.x / Spring Boot 4.0.x）
│
├── 01 批处理核心模型与领域对象
│   ├── Job / Step / JobInstance / JobExecution / StepExecution
│   ├── JobParameters（6.0 改为 record + Set）与 ExecutionContext
│   ├── 状态机：BatchStatus / ExitStatus / FlowExecutionStatus
│   └── 6.0 领域模型不可变重构（long 主键、无孤儿实体）
│
├── 02 基础设施与 JobRepository 元数据
│   ├── spring-boot-starter-batch 与 Boot 4 自动配置
│   ├── JDBC 元数据：6 张 BATCH_* 表 + 序列 + 乐观锁
│   ├── MongoDB / Resourceless 两种新 JobRepository
│   ├── @EnableJdbcJobRepository / @EnableMongoJobRepository
│   └── 6.0 序列改名与数据库迁移脚本
│
├── 03 Job 与 Step 构建：Tasklet 与 Chunk 模型
│   ├── JobBuilder(name, jobRepository) / StepBuilder 新 API
│   ├── Tasklet 与 Chunk 两种执行模型（ChunkOrientedStep 转正）
│   ├── Flow / Decision / 条件流转
│   ├── 监听器体系（core.listener）
│   └── JobParametersIncrementer（6.0 行为变更）
│
├── 04 ItemReader 体系
│   ├── 文件类：FlatFile / JSON / XML / MultiResource
│   ├── 数据库类：JDBC 游标与分页 / JPA / Hibernate
│   ├── 消息类：Kafka / JMS / AMQP
│   └── 组合与函数适配器：CompositeItemReader / SupplierItemReader
│
├── 05 ItemProcessor 与 ItemWriter 体系
│   ├── 转换 / 校验 / 过滤 / 组合 / 分类处理器
│   ├── JDBC / JPA / 文件 / JSON / Kafka / JMS 写入器
│   ├── 提交边界：commit-interval 与事务
│   └── ItemKeyGenerator 幂等去重
│
├── 06 容错机制：Skip、Retry 与重启
│   ├── SkipPolicy 与跳过的三类计数（读/写/处理）
│   ├── 6.0 基于 Spring Framework 7 Retry（不再用 Spring Retry）
│   ├── 事务边界与回滚语义
│   └── 重启机制与幂等设计（startLimit / allowStartIfComplete）
│
├── 07 并行与分布式：多线程、分区与远程执行
│   ├── 扩展阶梯：多线程 Step → 本地分区 → 远程分区/远程 Chunking
│   ├── 6.0 生产者-消费者并发模型与背压
│   ├── 远程分区：MessageChannelPartitionHandler + 共享 JobRepository
│   ├── 远程 Chunking：RemoteChunkingManager/Worker
│   └── 虚拟线程（JDK 21+）
│
├── 08 调度、运维与可观测性
│   ├── JobLauncher / JobOperator / CommandLineJobOperator
│   ├── 调度集成：Spring Task / Quartz / ShedLock
│   ├── stop / recover / 优雅停机（SIGTERM）
│   └── Micrometer 指标 + JFR 事件 + JobExplorer 监控
│
├── 09 测试与生产实践
│   ├── spring-boot-starter-batch-test / JobOperatorTestUtils
│   ├── 黄金实践与性能调优
│   ├── 故障排查手册
│   └── 5.x → 6.0 迁移清单
│
└── 10 面试题与避坑指南
```

## 2. 模块导航

| 序号 | 模块 | 核心内容 | 适合人群 |
|:---:|------|---------|---------|
| 01 | 批处理核心模型与领域对象 | 领域对象、参数、上下文、状态机、6.0 不可变重构 | 所有人（地基） |
| 02 | 基础设施与 JobRepository 元数据 | Starter、元数据表、三种 JobRepository、初始化策略 | 所有人 |
| 03 | Job 与 Step 构建 | 构建器 API、Tasklet/Chunk、流程控制、监听器 | 所有人 |
| 04 | ItemReader 体系 | 各类读数据源与组合方式 | 开发必读 |
| 05 | ItemProcessor 与 ItemWriter 体系 | 处理模式、写数据源、提交边界 | 开发必读 |
| 06 | 容错机制 | Skip/Retry/重启/幂等/事务 | 进阶 |
| 07 | 并行与分布式 | 多线程、分区、远程执行、背压 | 进阶+架构 |
| 08 | 调度、运维与可观测性 | 启动方式、调度集成、监控 | 运维+开发 |
| 09 | 测试与生产实践 | 测试框架、黄金实践、迁移、故障排查 | 生产落地 |
| 10 | 面试题与避坑指南 | 高频面试题与避坑清单 | 面试冲刺 |

## 3. 为什么必须系统学透 Spring Batch

**Spring Batch 是 Java 后端"离线批量处理"场景的唯一主流答案**，与消息队列（在线异步）、Spring Task（轻量定时）形成互补：

- **版本窗口（2026-08）**：Spring Batch **6.0.4**（2026-06-10 发布，配套 Spring Boot 4.0.7）为当前主线；6.0.0 GA 于 2025-11-19 发布，是配合 Spring Framework 7.0 / Spring Data 4.0 的大版本重构。5.2.x 已停止开源维护（5.2.6 为收官版），官方明确引导升级 6.0.x。
- **6.0 是一次"推倒式"重构**：领域模型不可变、`JobParameters` 改为 record、`JobRepository` 默认无资源（无需数据库即可跑 Hello World）、retry 从 Spring Retry 迁移到 Spring Framework 7 内置 Retry、包结构与命名大范围调整（`SimpleJobOperator` → `TaskExecutorJobOperator` 等）。**面试与生产迁移都要以 6.0 API 为准，旧博客中的 4.x/5.x 写法大多已废弃或移除。**
- **生产价值**：Chunk 化的事务边界、声明式 Skip/Retry、标准元数据表（可监控可审计）、重启续跑、分区与远程执行，是自研循环处理代码难以企及的工程完备度。
- **生态位置**：Spring Batch Integration 与 Spring Integration 7.0 打通远程分区/远程 Chunking/SEDA；与 Spring Data 4.0 打通 MongoDB 仓库；Micrometer + JFR 提供开箱可观测性。

## 4. 核心概念速查

| 概念 | 一句话解释 | 关键 API / 位置 |
|------|-----------|----------------|
| Job | 一次批处理任务的完整定义 | `JobBuilder("name", jobRepository)` |
| Step | Job 内的最小执行单元（Tasklet 或 Chunk） | `StepBuilder("name", jobRepository)` |
| JobInstance | 业务含义上的"同一次任务"（Job 名 + 参数唯一标识） | `BATCH_JOB_INSTANCE` |
| JobExecution | 一次实际运行记录（可重跑多次） | `BATCH_JOB_EXECUTION` |
| StepExecution | 每次 JobExecution 中每个 Step 的运行记录 | `BATCH_STEP_EXECUTION` |
| JobParameters | 启动参数（6.0 为 record，`JobParameters` 存 `Set<JobParameter>`） | `core.job.parameters` 包 |
| ExecutionContext | 步骤/作业间的键值状态存储（支持续跑） | `core.execution` 包 |
| Chunk | 一批 item 作为一个事务单元（commit-interval） | `.chunk(10).transactionManager(tx)` |
| Tasklet | 单块执行逻辑（无循环读写的场景） | 实现 `Tasklet.execute()` |
| Skip | 跳过失败 item 继续处理（读/写/处理三类） | `SkipPolicy` + `.faultTolerant()` |
| Retry | 失败后重试（6.0 基于 SF7 Retry） | `.retry()` / `.backOffPolicy()` |
| PartitionStep | 分区并行（本地/远程 worker 各跑一个 step） | `Partitioner` + `PartitionHandler` |
| JobRepository | 元数据持久化接口（6.0 起 `extends JobExplorer`） | JDBC / MongoDB / Resourceless 三实现 |
| JobOperator | 启动/停止/重启/恢复的运维入口（6.0 起 `extends JobLauncher`） | `TaskExecutorJobOperator` / `CommandLineJobOperator` |

## 5. 与周边知识的关系

| 邻近知识 | 与 Spring Batch 的关系 |
|---------|----------------------|
| Spring Task | 定时触发调度——Batch 管"批处理执行"，Spring Task 管"什么时候触发"（cron 中启动 JobLauncher） |
| Quartz / ShedLock | 更复杂的调度/分布式锁；多实例部署 Batch 时保证单点触发 |
| Spring Integration | 远程分区、远程 Chunking、SEDA 通道的基础设施（spring-batch-integration） |
| Spring Data JPA / MongoDB | JPA/Hibernate/Mongo item reader/writer；5.2 起支持 Mongo 版 JobRepository |
| Spring Framework 7 Retry | 6.0 起 retry 语义改用 Spring Framework 内置 Retry（替代 Spring Retry） |
| Micrometer / JFR | 批处理指标（`spring.batch.job` / `spring.batch.step`）与飞行记录事件 |
| 消息队列（Kafka/RabbitMQ） | 在线异步与离线批处理的互补；Kafka 亦是 item 读写的数据源之一 |
| 云原生（K8s/GraalVM） | Boot 4 下 Batch 支持 GraalVM Native Image；远程执行可跨 JVM 部署 |

## 6. 学习路线推荐

**路线 A：快速上手（开发岗）** —— 01 → 03 → 04 → 05 → 09（测试部分）
学完即可写出"文件读入 → 处理 → 入库"的完整 Job，并通过 spring-batch-test 验证。

**路线 B：进阶（架构/资深）** —— 02 → 06 → 07 → 08
掌握元数据设计、容错语义、分区与远程执行、可观测性，能设计生产级批处理平台。

**路线 C：面试冲刺** —— 10 → 01 → 06
先刷面试题建立全局框架，再回补领域模型与容错细节，答出 6.0 版本差异是加分项。

> 💡 所有模块均以 **Spring Batch 6.0.x + Spring Boot 4.0.x** 为准；涉及版本差异处标注 5.x 旧写法，便于迁移对照。

## 7. 快速自测 10 题

1. Job、JobInstance、JobExecution 三者的区别与联系？（提示：实例是"业务身份"，执行是"运行记录"）
2. Chunk 模型的提交边界是什么？一个 10000 条数据的 Job 配 `chunk(1000)` 会提交几次事务？
3. ItemReader 返回 null 意味着什么？
4. ItemProcessor 返回 null 意味着什么？
5. Skip 与 Retry 分别在什么场景下用？读、写、处理三类 skip 有何不同？
6. 为什么多线程 Step 会失去重启能力？
7. 本地分区与远程 Chunking 的本质区别？（提示：数据过网 vs 只传分片范围）
8. Spring Batch 6.0 中 `JobRepository` 与 `JobExplorer`、`JobOperator` 与 `JobLauncher` 的关系？
9. 元数据表 `BATCH_JOB_INSTANCE` 的唯一性由什么决定？重启如何识别"同一次任务"？
10. 6.0 中 retry 的底层依赖是什么？5.x 时代呢？

## 8. 版本窗口与参考来源

> ⚠️ **版本窗口**：本文档体系编写于 2026-08，以 Spring Batch **6.0.4** / Spring Boot **4.0.x** / Spring Framework **7.0** 为准；5.2.x 为最后一个社区版系列，5.1.x 仅存商业维护。内容经 2026-08 网络检索核实。

- Spring Batch 6.0.4 / 5.2.6 发布公告（2026-06-10）：https://spring.io/blog/2026/06/10/spring-batch-6-0-4-and-5-2-6-available-now
- Spring Batch 6.0 新特性官方文档：https://docs.spring.io/spring-batch/reference/6.0-SNAPSHOT/whatsnew.html
- Spring Batch 6.0.0 GA 发布公告（2025-11-19）：https://spring.io/blog/2025/11/19/spring-batch-6-0-0-ga
- Spring Batch 6.0 迁移指南（GitHub Wiki）：https://github.com/spring-projects/spring-batch/wiki/Spring-Batch-6.0-Migration-Guide
- Spring Batch 5.2 新特性：https://docs.spring.io/spring-batch/reference/5.2/whatsnew.html
- Spring Batch 元数据 Schema 附录：https://docs.spring.io/spring-batch/reference/schema-appendix.html
- Spring Batch Observability：https://docs.spring.io/spring-batch/reference/spring-batch-observability.html
- Spring Batch 6 外部执行（远程分区/Chunking）文档：https://docs.spring.io/spring-batch/reference/6.0-SNAPSHOT/spring-batch-integration/externalizing-execution.html
- Spring Batch 6.0 发布历史：https://github.com/spring-projects/spring-batch/releases/tag/v6.0.4

---

> 🎯 **核心要点**：Spring Batch 6.0 是配合 Spring Boot 4 / Spring Framework 7 的重构大版本——默认无库运行、JobParameters 变 record、retry 换内核、API 大改名。学本体系的关键是**用 6.0 视角**理解"Chunk 事务边界 + 声明式容错 + 元数据驱动运维"这三大设计支柱。

**下一模块**：[01-批处理核心模型与领域对象](01-批处理核心模型与领域对象.md)
