# 02 基础设施与 JobRepository 元数据

> JobRepository 是批处理的"大脑与账本"：它持久化所有领域对象、驱动重启逻辑。6.0 最大的基础设施变革是——默认不再需要数据库（Resourceless），JDBC 与 MongoDB 双实现并存，配置注解从"一把梭"拆成 `@EnableJdbcJobRepository` / `@EnableMongoJobRepository`

---

## 📚 目录

1. [依赖与 Spring Boot 4 自动配置](#1-依赖与-spring-boot-4-自动配置)
2. [JobRepository 接口与三种实现](#2-jobrepository-接口与三种实现)
3. [JDBC 元数据 Schema 详解](#3-jdbc-元数据-schema-详解)
4. [乐观锁与并发控制](#4-乐观锁与并发控制)
5. [初始化策略与多数据源](#5-初始化策略与多数据源)
6. [6.0 数据库迁移与最佳实践](#6-60-数据库迁移与最佳实践)

---

## 1. 依赖与 Spring Boot 4 自动配置

### 1.1 依赖引入（Spring Boot 4.0.x / Spring Batch 6.0.x）

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-batch</artifactId>
</dependency>
```

配套要点：

- 版本由 Boot 4.0.x BOM 管理，当前（2026-08）为 Spring Batch **6.0.4**；
- 测试依赖：`spring-boot-starter-batch-test`（含 `spring-batch-test` + 测试骨架，见模块 09）；
- 仅用 Resourceless（默认）跑通 Hello World **不需要任何数据库依赖**；使用 JDBC 仓库需自行引入 `spring-boot-starter-jdbc` + 数据库驱动；使用 MongoDB 仓库引入 `spring-boot-starter-data-mongodb`；
- GraalVM Native Image：6.0 + Boot 4 下开箱即用（无需额外反射配置即可编译运行简单 Job）。

### 1.2 Boot 4 自动配置的变化

| 版本 | @EnableBatchProcessing | 数据库 | 默认事务管理器 |
|------|----------------------|--------|---------------|
| 4.x/5.x | **必须**（开启批处理基础设施） | 默认需要（H2/HSQL 内存库或业务库） | 容器中的 PlatformTransactionManager |
| 6.0 | **不再必须**（Boot 4 自动配置默认开启；仅深度定制时显式使用） | **默认无库**（ResourcelessJobRepository） | ResourcelessTransactionManager（未配置时自动回退） |

```java
// 6.0：最小可运行 Job——不需要 @EnableBatchProcessing、不需要数据库
@Configuration(proxyBeanMethods = false)
class HelloJobConfig {

    @Bean
    Step step1(JobRepository jobRepository) {
        return new StepBuilder("step1", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("hello batch");
                    return RepeatStatus.FINISHED;
                })
                .build();
    }

    @Bean
    Job helloJob(JobRepository jobRepository, Step step1) {
        return new JobBuilder("helloJob", jobRepository)
                .start(step1)
                .build();
    }
}
```

> 💡 自动配置细节：Boot 4 在 classpath 有 Batch 时自动装配 `JobRepository`、`JobLauncher` 等。需要自定义时，扩展 `DefaultBatchConfiguration`（或 `JdbcDefaultBatchConfiguration` / `MongoDefaultBatchConfiguration`）替换默认行为——**优先用继承替换，而非 @EnableBatchProcessing 全量接管**。

## 2. JobRepository 接口与三种实现

### 2.1 接口职责（6.0 起 `JobRepository extends JobExplorer`）

```text
JobExplorer（查询视图：只读）
   ├── getJobInstances / getJobExecutions / getStepExecutions
   └── getJobInstanceCount / findJobInstancesByJobName ...
        ▲
JobRepository（读写视图）
   ├── createJobInstance / createJobExecution / createStepExecution
   ├── updateJobExecution / updateStepExecution / updateExecutionContext
   └── isJobInstanceExists / getLastJobExecution ...
```

6.0 将查询接口上提为父接口，组件只需 JobRepository 即可同时获得读写能力，Bean 依赖更少（6.0 简化配置的核心思路）。

### 2.2 三种实现横向对比

| 实现 | 引入时机 | 持久化 | 重启支持 | 线程安全 | 适用场景 |
|------|---------|--------|---------|---------|---------|
| ResourcelessJobRepository | 5.2 引入，6.0 默认 | 无（不落库） | ❌ | ❌（不可多线程） | 一次性 Job（自有 JVM、单线程）、测试、快速原型 |
| JdbcJobRepository（JdbcDefaultBatchConfiguration） | 经典实现 | JDBC 6 张表 | ✅ | ✅ | 生产标准选择（可监控、可重启、可审计） |
| MongoJobRepository（MongoDefaultBatchConfiguration） | 5.2 引入 | MongoDB 集合 | ✅ | ✅ | 已有 Mongo 基础设施、不想维护关系表 |

> ⚠️ **Resourceless 的坑**：不保存元数据意味着 **没有重启能力、没有运行历史、不能跨 JVM 共享状态**（远程分区/远程 Chunking 明确不可用）。它只适合"跑完就结束、失败重来"的场景——生产环境默认必须显式配置 JDBC 或 Mongo 仓库。

### 2.3 显式配置 JDBC 仓库（6.0 新注解）

```java
// 6.0：显式指定 JDBC 仓库（数据源、事务管理器、表前缀等）
@Configuration(proxyBeanMethods = false)
@EnableBatchProcessing(taskExecutorRef = "batchTaskExecutor")
@EnableJdbcJobRepository(dataSourceRef = "batchDataSource",
        transactionManagerRef = "batchTransactionManager",
        tablePrefix = "BATCH_")
class BatchInfraConfig {

    @Bean
    DataSource batchDataSource() {
        return DataSourceBuilder.create().build();
    }
    // ... transactionManager
}
```

对比 5.x：`@EnableBatchProcessing(dataSourceRef = "...")` 把所有数据源配置揉在一个注解里；6.0 把 **数据源相关配置拆到 `@EnableJdbcJobRepository`**，`@EnableBatchProcessing` 只管任务执行器等与存储无关的配置。

## 3. JDBC 元数据 Schema 详解

### 3.1 表与序列总览

```text
6 张业务表：
  BATCH_JOB_INSTANCE          作业实例（业务身份）
  BATCH_JOB_EXECUTION         作业执行（每次运行）
  BATCH_JOB_EXECUTION_PARAMS  作业执行参数（含类型列）
  BATCH_JOB_EXECUTION_CONTEXT 作业级上下文（序列化的 ExecutionContext）
  BATCH_STEP_EXECUTION        步骤执行（计数器 + 状态）
  BATCH_STEP_EXECUTION_CONTEXT 步骤级上下文

3 个序列（MySQL 用同名单表代替）：
  BATCH_JOB_INSTANCE_SEQ      ← 6.0 由 BATCH_JOB_SEQ 改名而来
  BATCH_JOB_EXECUTION_SEQ
  BATCH_STEP_EXECUTION_SEQ
```

> ⚠️ **6.0 迁移关键点**：序列 `BATCH_JOB_SEQ` 改名为 **`BATCH_JOB_INSTANCE_SEQ`**。官方在 `spring-batch-core` 的 `org.springframework.batch.core.migration/6.0` 下提供了各数据库的迁移脚本（注意：DB2 的脚本仅适用于 Informix，DB2 LUW 不支持序列改名，需手工处理）。

### 3.2 核心表结构

**BATCH_JOB_INSTANCE**

```sql
CREATE TABLE BATCH_JOB_INSTANCE (
  JOB_INSTANCE_ID BIGINT PRIMARY KEY,   -- 由序列生成，非数据库自增
  VERSION BIGINT,                        -- 乐观锁
  JOB_NAME VARCHAR(100) NOT NULL,
  JOB_KEY VARCHAR(32) NOT NULL,          -- JobParameters 序列化哈希
  UNIQUE (JOB_NAME, JOB_KEY)             -- 实例唯一性约束
);
```

**BATCH_JOB_EXECUTION**

```sql
CREATE TABLE BATCH_JOB_EXECUTION (
  JOB_EXECUTION_ID BIGINT PRIMARY KEY,
  VERSION BIGINT,
  JOB_INSTANCE_ID BIGINT NOT NULL,
  CREATE_TIME TIMESTAMP NOT NULL,
  START_TIME TIMESTAMP,
  END_TIME TIMESTAMP,
  STATUS VARCHAR(10),                    -- BatchStatus
  EXIT_CODE VARCHAR(20),                 -- ExitStatus code
  EXIT_MESSAGE VARCHAR(2500),            -- ExitStatus description
  LAST_UPDATED TIMESTAMP,
  CONSTRAINT JOB_INSTANCE_EXECUTION_FK FOREIGN KEY (JOB_INSTANCE_ID)
    REFERENCES BATCH_JOB_INSTANCE(JOB_INSTANCE_ID)
);
```

**BATCH_STEP_EXECUTION**（批处理审计字段最全的一张表）

| 列 | 含义 |
|----|------|
| STEP_NAME / JOB_EXECUTION_ID | 归属 |
| STATUS / EXIT_CODE / EXIT_MESSAGE | 状态与退出码 |
| COMMIT_COUNT | 已提交块数 |
| READ_COUNT / FILTER_COUNT / WRITE_COUNT | 读/过滤/写成功计数 |
| READ_SKIP_COUNT / WRITE_SKIP_COUNT / PROCESS_SKIP_COUNT | 三类跳过计数（模块 06） |
| ROLLBACK_COUNT | 回滚次数 |
| VERSION / LAST_UPDATED | 乐观锁 / 更新时间 |

**BATCH_JOB_EXECUTION_PARAMS**

| 列 | 说明 |
|----|------|
| JOB_EXECUTION_ID | 外键 |
| TYPE_CD | 参数类型：STRING / LONG / DOUBLE / DATE（6.0.2 起支持 ZonedDateTime / OffsetDateTime 类型参数） |
| KEY_NAME / STRING_VAL / LONG_VAL / DOUBLE_VAL / DATE_VAL | 键 + 按类型分列存储 |
| IDENTIFYING | 是否参与 JOB_KEY 计算（`false` 的参数不决定实例身份） |

> 💡 工程价值：`IDENTIFYING=false` 的参数（如"运行批次号"）**不参与实例身份判定**——同参数重跑时，这类参数的变化不会产生新实例。这是设计"重跑但换批次号"场景的关键。

### 3.3 主键为什么用序列而非自增

`JOB_INSTANCE_ID` 等主键不是数据库自增列，而是**由独立序列生成、再回填到 Java 对象**。原因：实体创建后框架需要把 ID 设置回内存对象用于后续关联，依赖 JDBC 的数据库生成主键特性（JDBC 3.0 之后才支持）不够通用且各库行为不一。MySQL 无序列，用单列表 + `MySQLMaxValueIncrementer` 模拟。

## 4. 乐观锁与并发控制

三张主表都有 `VERSION` 列，实现**乐观锁**：

- 每次 update 时 `VERSION = VERSION + 1`，并在 WHERE 中带旧版本号；
- 若版本已被其他线程改过，抛出 `OptimisticLockingFailureException`；
- 典型触发场景：两个进程同时操作同一 JobExecution（如：作业还在跑，运维又点了停止；或 JobOperator 与 Web 管理端并发操作）。

```text
并发防护最佳实践：
  1. 同一 Job 同一参数的并发启动 → 由 JOB_KEY 唯一约束拦截（数据库层兜底）
  2. 运行中的 Job 重复启动 → 启动前用 JobExplorer 查询运行中状态，或靠唯一约束报错
  3. stop / restart 并发 → 依赖乐观锁，失败重试或提示"状态已变化"
```

## 5. 初始化策略与多数据源

### 5.1 表结构初始化（Boot 属性）

| 属性 | 取值 | 行为 |
|------|------|------|
| `spring.batch.jdbc.initialize-schema` | `always` | 启动时总是执行建表脚本 |
| | `embedded`（Boot 默认） | 仅当数据源为嵌入式（H2/HSQL/H2 内存）时初始化 |
| | `never` | 不初始化（生产常用：由 DBA 用官方 schema 脚本建表） |
| `spring.batch.jdbc.table-prefix` | 默认 `BATCH_` | 表前缀，多租户/隔离时改动 |
| `spring.batch.jdbc.isolation-level-for-create` | `ISOLATION_DEFAULT` 等 | 创建元数据时的事务隔离级别 |

生产建议：`initialize-schema=never` + 发布流程中显式执行建表/迁移脚本（脚本在 `spring-batch-core` 的 `org.springframework.batch.core` 包下，按数据库命名：`schema-postgresql.sql` / `schema-mysql.sql` / `schema-oracle.sql` 等）。

### 5.2 多数据源（批处理库与业务库分离）

典型架构：批处理元数据用独立 `batchDataSource`，业务读写用 `dataSource`，防止批处理状态写入与业务大事务互相干扰：

```java
// 关键：@EnableJdbcJobRepository 指向批处理专用数据源（dataSourceRef）
//       chunk 业务事务用另一个事务管理器
@EnableJdbcJobRepository(dataSourceRef = "batchDataSource",
        transactionManagerRef = "batchTransactionManager")
```

> 💡 生产最常见的元数据错误就是"业务数据源与批处理数据源混用"：chunk 事务提交把元数据状态也带进同一个事务，重启逻辑与业务回滚相互污染。

## 6. 6.0 数据库迁移与最佳实践

### 6.1 5.x → 6.0 数据库侧清单

1. 执行 6.0 迁移脚本（序列改名 `BATCH_JOB_SEQ → BATCH_JOB_INSTANCE_SEQ`）；
2. **重启或放弃所有 5.x 遗留的 FAILED 实例**（JobParameters 序列化格式变化，v5 失败实例无法用 v6 续跑）；
3. `JobRepositoryFactoryBean` → `JdbcJobRepositoryFactoryBean`（改名）；
4. 包导入变化：`core.explore` → `core.repository.explore`，DAO 类移到 `core.repository.dao.jdbc`；
5. 配置上：`@EnableBatchProcessing(dataSourceRef=...)` → 拆分到 `@EnableJdbcJobRepository(dataSourceRef=...)`；
6. 需要监控指标时显式提供 `ObservationRegistry` Bean（6.0 移除了 Micrometer 全局静态注册表的使用）。

### 6.2 索引与运维建议

官方建议的 WHERE 模式对应的索引：

| 查询模式 | 触发时机 | 建议索引 |
|---------|---------|---------|
| 按 (JOB_NAME, JOB_KEY) 定位实例 | 每次启动 | `BATCH_JOB_INSTANCE(JOB_NAME, JOB_KEY)`（建表脚本自带唯一约束） |
| 按 JOB_INSTANCE_ID 取执行历史 | 每次重启 | `BATCH_JOB_EXECUTION(JOB_INSTANCE_ID)` |
| 按 VERSION 更新 | 每个 chunk 提交 | `BATCH_STEP_EXECUTION(VERSION)` |
| 按 (STEP_NAME, JOB_EXECUTION_ID) | 每次步骤执行前 | `BATCH_STEP_EXECUTION(STEP_NAME, JOB_EXECUTION_ID)` |

> 🎯 **核心要点**：6.0 默认 Resourceless 让入门门槛归零，但生产必须显式选择 JDBC 或 MongoDB 仓库——元数据是重启、审计、监控的根基。JDBC 方案记住"6 表 + 3 序列 + 乐观锁 + IDENTIFYING 参数"，迁移注意序列改名与 v5 失败实例兼容性问题。

**下一模块**：[03-Job与Step构建：Tasklet与Chunk模型](03-Job与Step构建：Tasklet与Chunk模型.md)　|　**返回总览**：[00-Spring Batch知识体系总览](00-Spring Batch知识体系总览.md)
