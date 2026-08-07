# 00 Spring Batch 组件总览

> 组件卡片：Spring Batch 是什么、版本现状、能做什么、与深度体系如何衔接——本系列是"组件汇总"速查型文档，深挖见 [Spring Batch 深度知识体系](../../../Spring Batch/00-Spring Batch知识体系总览.md)（11 篇完整教程）

---

## 📚 目录

1. [组件一句话定位](#1-组件一句话定位)
2. [版本现状（2026-08）](#2-版本现状2026-08)
3. [能力地图](#3-能力地图)
4. [与深度体系的映射](#4-与深度体系的映射)
5. [快速上手 3 步](#5-快速上手-3-步)
6. [速查导航](#6-速查导航)
7. [学习路线推荐](#7-学习路线推荐)
8. [核心概念速查](#8-核心概念速查)

---

## 1. 组件一句话定位

**Spring Batch 是 Java 生态"离线大数据量批处理"的事实标准**——以 Chunk 事务边界、声明式 Skip/Retry、元数据驱动的重启续跑为核心，处理日终结算、数据迁移、报表汇总等场景。

```text
核心心智模型：
  Job（作业定义）
   └── Step（执行单元）
        ├── Tasklet：一段逻辑（初始化/清理）
        └── Chunk：读 N 条 → 处理 → 写 → 提交一次事务
              （ItemReader → ItemProcessor → ItemWriter）
  JobRepository（元数据账本：BATCH_* 表，驱动重启/审计/监控）
```

> 🎯 **一句话**：Spring Batch 不解决"分布式计算"（那是 Spark/Flink 的事），它解决"单 JVM 内把大数据量批处理做对"——可重启、可审计、可容错。

## 2. 版本现状（2026-08）

| 版本线 | 最新 | 发布 | 配套 | 状态 |
|--------|------|------|------|------|
| **6.0.x** | 6.0.4 | 2026-06-10 | Spring Boot 4.0.7 / Spring Framework 7.0 | **当前主线（推荐）** |
| 5.2.x | 5.2.6 | 2026-06-10 | Boot 3.5.15 / SF 6.2 | 已停止开源维护（收官版） |
| 5.1.x | 5.1.11 | — | — | 仅商业维护 |

6.0 是配合 SF 7 / Boot 4 的重构大版本，关键变化：

- **默认无库运行**：ResourcelessJobRepository + ResourcelessTransactionManager，Hello World 零配置；
- **API 重构**：`new JobBuilder(name, jobRepository)` 构造注入（工厂类移除）、JobParameters 变 record、`JobRepository extends JobExplorer`、`JobOperator extends JobLauncher`；
- **容错换核**：retry 从 Spring Retry 迁到 Spring Framework 7 内置 Retry；skip 完全基于 SkipPolicy；
- **并发重写**：生产者-消费者 + 有界队列背压（throttleLimit 移除）；
- **运维升级**：CommandLineJobOperator 替代 CommandLineJobRunner、JobOperator.recover()、JFR 可观测事件；
- ⚠️ **迁移警告**：5.x 遗留的 FAILED 实例无法在 6.0 续跑（JobParameters 序列化格式变化）；`BATCH_JOB_SEQ` 改名 `BATCH_JOB_INSTANCE_SEQ`。

> 💡 写代码/面试一律以 6.0 API 为准——旧博客里的 JobBuilderFactory、`.chunk(n, tx)`、spring-retry 写法均已废弃或移除。

## 3. 能力地图

| 能力域 | 能力 | 对应组件/API |
|--------|------|-------------|
| 执行模型 | Tasklet / Chunk 双模型 | Tasklet、StepBuilder.chunk() |
| 数据读写 | 文件/DB/消息/函数四类 reader、writer 全家桶 | FlatFileItemReader、JdbcBatchItemWriter、KafkaItemReader… |
| 容错 | Skip / Retry / 重启续跑 / 幂等 | SkipPolicy、SF7 Retry、startLimit、ItemKeyGenerator |
| 并行 | 多线程 Step / 本地分区 / 远程分区 / 远程 Chunking | Partitioner、MessageChannelPartitionHandler、RemoteChunking*Builder |
| 元数据 | JDBC / MongoDB / Resourceless 三仓库 | @EnableJdbcJobRepository、@EnableMongoJobRepository |
| 运维 | 启动/停止/恢复/放弃 | TaskExecutorJobOperator、CommandLineJobOperator |
| 可观测 | Micrometer 指标 / JFR 事件 | spring.batch.job/step 指标、ObservationRegistry |
| 测试 | 端到端 Job 测试 | spring-boot-starter-batch-test、JobOperatorTestUtils |
| 集成 | 远程执行消息通道 | spring-batch-integration（Spring Integration 7.x） |

## 4. 与深度体系的映射

| 本卡片模块 | 深度体系对应 | 衔接说明 |
|-----------|-------------|---------|
| 00-04 全卡 | [00-Spring Batch知识体系总览](../../../Spring Batch/00-Spring Batch知识体系总览.md) | 11 篇完整教程（总览/领域模型/元数据/构建/读写/容错/并行/运维/测试/面试） |
| 01 模块清单 | [02-基础设施与JobRepository元数据](../../../Spring Batch/02-基础设施与JobRepository元数据.md) | 依赖与仓库细节 |
| 02 核心类 | [01-批处理核心模型与领域对象](../../../Spring Batch/01-批处理核心模型与领域对象.md) | 领域对象与状态机 |
| 04 集成地图 | [07-并行与分布式：多线程、分区与远程执行](../../../Spring Batch/07-并行与分布式：多线程、分区与远程执行.md) | 与 Spring Integration 的远程执行协作 |

## 5. 快速上手 3 步

```text
① 引入依赖：spring-boot-starter-batch（Boot 4 管理版本，无需 @EnableBatchProcessing）
② 定义 Job + Step（Chunk 或 Tasklet），JobBuilder/StepBuilder 构造器注入 JobRepository
③ 启动：spring.batch.job.enabled=true（默认）或手动 JobLauncher/JobOperator
   生产请显式配置 JDBC/Mongo 仓库（默认 Resourceless 无重启能力）
```

```java
// 最小可运行 Job（6.0，无需数据库）
@Configuration(proxyBeanMethods = false)
class HelloJobConfig {
    @Bean
    Step step1(JobRepository jobRepository) {
        return new StepBuilder("step1", jobRepository)
                .tasklet((contribution, ctx) -> RepeatStatus.FINISHED)
                .build();
    }
    @Bean
    Job helloJob(JobRepository jobRepository, Step step1) {
        return new JobBuilder("helloJob", jobRepository).start(step1).build();
    }
}
```

## 6. 速查导航

| 卡片 | 内容 | 何时翻 |
|------|------|--------|
| [01-模块清单](01-模块清单.md) | artifact 与 Starter 对照 | 加依赖、解 NoSuchMethodError |
| [02-核心类与注解速查](02-核心类与注解速查.md) | 领域对象/构建器/读写接口/容错 API | 写 Job 时随手查 |
| [03-配置属性速查](03-配置属性速查.md) | spring.batch.* 属性字典 | 写 application.yml 时 |
| [04-集成地图与常见问题](04-集成地图与常见问题.md) | 生态集成 + 高频坑 + 排障流程 | 集成/排障 |

## 7. 学习路线推荐

```text
快速开发：00 → 02（核心类）→ 深度体系 03/04/05（构建+读写）→ 09 测试
架构进阶：00 → 深度体系 06（容错）→ 07（并行分布式）→ 08（运维可观测）
面试冲刺：00 版本现状 → 深度体系 10（面试题与避坑）→ 01（领域模型）
```

## 8. 核心概念速查

| 概念 | 一句话 |
|------|--------|
| Job / Step | 作业定义 / 最小执行单元 |
| JobInstance / JobExecution | 业务身份（参数哈希） / 一次运行记录 |
| Chunk | N 条一个事务（commit-interval） |
| JobRepository | 元数据账本，驱动重启/审计 |
| Skip / Retry | 跳过坏数据 / 重试瞬时故障 |
| PartitionStep | 分片并行（本地/远程） |
| JobOperator | 启动/停止/重启/恢复的运维入口 |
| JobParametersIncrementer | 每次启动自动生成新参数（新实例） |

---

> 🎯 **核心要点**：Spring Batch 6.0 是"无库起步、构造注入、SF7 容错内核"的新时代；本卡片的深度版本在 `../Spring Batch/` 深度体系，生产落地前务必阅读其中的迁移清单与故障排查手册。

**下一模块**：[01-模块清单](01-模块清单.md)
