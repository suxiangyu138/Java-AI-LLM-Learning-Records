# 04 ItemReader 体系

> ItemReader 是数据流的源头：文件、数据库、消息队列、函数适配器四大类，配上游标/分页、组合与 peek 机制——本章给出 6.0 时代的完整 Reader 选型地图与配置示例

---

## 📚 目录

1. [ItemReader 契约与设计约束](#1-itemreader-契约与设计约束)
2. [文件类 Reader](#2-文件类-reader)
3. [数据库类 Reader](#3-数据库类-reader)
4. [消息与特殊数据源 Reader](#4-消息与特殊数据源-reader)
5. [组合、Peek 与函数适配器](#5-组合peek-与函数适配器)
6. [游标 vs 分页深度对比](#6-游标-vs-分页深度对比)
7. [Reader 选型决策表](#7-reader-选型决策表)

---

## 1. ItemReader 契约与设计约束

```java
public interface ItemReader<T> {
    @Nullable
    T read() throws Exception;
}
```

三条铁律：

1. **返回 null = 数据耗尽**，Step 结束本次循环（不是"读到空数据"）；
2. **read() 抛异常** 走容错策略（模块 06），不会结束 Step；
3. **6.0 装配约束**：ItemReader 必须**构造器注入**必需依赖（默认构造 + setter 的旧风格已禁止）——官方实现全部通过 Builder 或构造器创建。

Thread-Safety 维度：**普通 Step 中 reader 每次只被单线程调用**；但多线程 Step（模块 07）中同一 reader 实例会被并发调用，必须线程安全（官方 reader 在此场景需配合 `SynchronizedItemStreamReader` 等包装）。

## 2. 文件类 Reader

### 2.1 FlatFileItemReader —— 最常用的平面文件读取器

配置要素：Resource + 行映射（LineMapper）= 分词器（Tokenizer）+ 字段映射器（FieldSetMapper）。

```java
@Bean
@StepScope
FlatFileItemReader<Order> orderReader(
        @Value("#{jobParameters['input.file']}") Resource resource,
        OrderFieldSetMapper mapper) {
    return new FlatFileItemReaderBuilder<Order>()
            .name("orderReader")
            .resource(resource)
            .linesToSkip(1)                       // 跳过表头
            .delimited()                          // 分隔符模式
            .delimiter(",")
            .names("id", "customerId", "amount")  // 列名（FieldSet 按名取）
            .fieldSetMapper(mapper)
            .maxItemCount(10_000_000)             // 硬上限保护
            .encoding("UTF-8")
            .build();
}
```

| 分词器 | 适用 | 关键配置 |
|--------|------|---------|
| DelimitedLineTokenizer | CSV/TSV | delimiter、quotes（`"` 引号内转义）、names |
| FixedLengthTokenizer | 固定宽度文件（银行对账等） | 列宽数组 `setColumns(new Range[]{new Range(1,10), ...})` |
| JsonLineTokenizer | 单行 JSON | 与 JsonItemReader 功能重叠，6.0 优先用 JsonItemReader |

FieldSetMapper 写法：

```java
public class OrderFieldSetMapper implements FieldSetMapper<Order> {
    @Override
    public Order mapFieldSet(FieldSet fs) {
        return Order.of(fs.readLong("id"), fs.readString("customerId"), fs.readBigDecimal("amount"));
    }
}
```

内置映射器：`BeanWrapperFieldSetMapper`（按 names 反射注入）、`RecordFieldSetMapper`（记录类型）。⚠️ 6.0.4 修复：`RecordFieldSetMapper` 对可空类型曾拒绝 null 值（JSpecify 空安全细化后的问题）。

### 2.2 多文件与 JSON/XML

```java
// 多个文件顺序读取（配合通配符资源）
MultiResourceItemReader<Order> multiReader = new MultiResourceItemReaderBuilder<Order>()
        .name("multiOrderReader")
        .resources(resources)               // new PathMatchingResourcePatternResolver().getResources("s3://bucket/orders-*.csv")
        .delegate(orderReader)              // 每个文件一个 FlatFileItemReader
        .build();

// JSON（6.0 支持 Jackson 3.x；5.x 为 Jackson2）
JsonItemReader<Order> jsonReader = new JsonItemReaderBuilder<Order>()
        .name("jsonOrderReader")
        .resource(resource)
        .jsonObjectReader(new JacksonJsonObjectReader<>(Order.class))
        .build();

// XML
StaxEventItemReader<Order> xmlReader = new StaxEventItemReaderBuilder<Order>()
        .name("xmlOrderReader")
        .resource(resource)
        .addFragmentRootElements("order")
        .unmarshaller(jaxbUnmarshaller)
        .build();
```

> ⚠️ 6.0 注意：`JsonItemWriter`/`JsonItemReader` 走 **Jackson 3.x**；5.x 的 `Jackson2*` 系列在 6.0 未保留双版本 API，迁移需直接替换（Jackson 2 支持整体废弃，6.0 还修复了 `appendAllowed` 时 JsonFileItemWriter 破坏 JSON 数组结构的问题）。

## 3. 数据库类 Reader

### 3.1 JDBC：游标 vs 分页

```java
// 游标式：ResultSet 逐行滚动，内存恒定 O(1)
JdbcCursorItemReader<Order> cursorReader = new JdbcCursorItemReaderBuilder<Order>()
        .name("orderCursorReader")
        .dataSource(dataSource)
        .sql("SELECT id, customer_id, amount FROM t_order WHERE status = 'NEW'")
        .rowMapper((rs, i) -> Order.of(rs.getLong("id"), rs.getString("customer_id"), rs.getBigDecimal("amount")))
        .fetchSize(500)                          // 驱动批量取行（Oracle/PostgreSQL 必须设置）
        .build();

// 分页式：LIMIT/OFFSET 分页查询，每次读一页
JdbcPagingItemReader<Order> pagingReader = new JdbcPagingItemReaderBuilder<Order>()
        .name("orderPagingReader")
        .dataSource(dataSource)
        .selectClause("SELECT id, customer_id, amount")
        .fromClause("FROM t_order")
        .whereClause("WHERE status = 'NEW'")
        .sortKeys(Map.of("id", Order.ASCENDING))  // 分页必须排序
        .pageSize(1000)
        .build();
```

5.2 起支持 **`dataRowMapper(TargetType.class)`**：直接映射 Java record / Kotlin data class（与 `beanRowMapper` 并列），省去手写 rowMapper：

```java
JdbcCursorItemReader<OrderRecord> reader = new JdbcCursorItemReaderBuilder<OrderRecord>()
        .name("r")
        .dataSource(ds)
        .sql("SELECT * FROM t_order")
        .dataRowMapper(OrderRecord.class)   // 5.2+ / 6.0
        .build();
```

### 3.2 JPA / Hibernate / Repository

```java
// JPA 分页
JpaPagingItemReader<Order> jpaReader = new JpaPagingItemReaderBuilder<Order>()
        .name("orderJpaReader")
        .entityManagerFactory(emf)
        .queryString("select o from Order o where o.status = 'NEW'")
        .pageSize(1000)
        .build();

// JPA 游标（流式）
JpaCursorItemReader<Order> jpaCursorReader = new JpaCursorItemReaderBuilder<Order>()
        .name("orderJpaCursor")
        .entityManagerFactory(emf)
        .queryString("select o from Order o where o.status = 'NEW'")
        .build();
```

5.2 起 JPA 游标/分页 builder 支持 **query hints**（fetch size、timeout 等直接传入）。

```java
// Spring Data Repository 驱动（内存开销大，小数据量可用）
RepositoryItemReader<Order> repoReader = new RepositoryItemReaderBuilder<Order>()
        .repository(orderRepository)
        .methodName("findByStatus")
        .arguments("NEW")
        .pageSize(100)
        .build();
```

### 3.3 已移除的数据库 Reader

> ⚠️ 6.0 移除了已废弃的 **MongoItemReader / Neo4jItemReader**（及其 Writer）——MongoDB 作为批处理数据源时，推荐自定义 reader（基于 Spring Data Mongo 流式查询）或第三方实现。

## 4. 消息与特殊数据源 Reader

| Reader | 数据源 | 关键语义 |
|--------|--------|---------|
| KafkaItemReader | Kafka topic | 按分区/偏移消费；`setSaveState(true)` 时消费位点可随 ExecutionContext 重启续读 |
| JmsItemReader | JMS 队列 | 每次 read 阻塞收一条消息（配合 Tasklet 自循环 + CONTINUABLE） |
| AmqpItemReader | RabbitMQ | 同上，基于 Spring AMQP |
| MongoItemReader | MongoDB | 6.0 已移除（见上），可用自定义实现 |

Kafka 消费示例（批处理与流式消费的典型结合点）：

```java
KafkaItemReader<String, Order> kafkaReader = new KafkaItemReaderBuilder<String, Order>()
        .name("orderKafkaReader")
        .topic("order-events")
        .partitions(0, 1, 2)
        .consumerProperties(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092",
                ConsumerConfig.GROUP_ID_CONFIG, "batch-orders"))
        .saveState(true)                    // 位点持久化，支持重启续读
        .build();
```

## 5. 组合、Peek 与函数适配器

### 5.1 组合与 Peek

| 组件 | 作用 |
|------|------|
| CompositeItemReader（5.2 新增） | **顺序**从多个同构数据源读取（如先读文件再读表）；6.0.4 修复了多次 Job 执行间迭代器不复位的问题 |
| SingleItemPeekableItemReader | 包装一个 reader，支持"偷看下一条"（用于去重相邻行等场景） |
| ListItemReader / IteratorItemReader / QueueItemReader | 内存集合/队列数据源（测试与单元场景） |
| BlockingQueueItemReader（5.2 新增） | 从有界阻塞队列读取，供生产者-消费者模型使用（模块 07） |

### 5.2 函数适配器（5.2 新增 `org.springframework.batch.item.function` 包）

把 java.util.function 直接适配为批处理工件，告别匿名内部类：

```java
SupplierItemReader<String> supplierReader = new SupplierItemReader<>(() -> nextLine());  // Supplier → reader
```

同包还有 `ConsumerItemWriter`（Consumer → writer）、`PredicateFilteringItemProcessor`（Predicate → 过滤型 processor）、`FunctionItemProcessor`（Function → 转换型 processor）。

### 5.3 包装旧服务

```java
// 把已有 DAO/Service 的"取一批"方法包装成 reader
ItemReaderAdapter<Order> adapter = new ItemReaderAdapterBuilder<Order>()
        .targetObject(orderService)
        .targetMethod("nextOrder")     // 方法返回 null 表示取完
        .build();
```

## 6. 游标 vs 分页深度对比

| 维度 | 游标式（Cursor） | 分页式（Paging） |
|------|----------------|-----------------|
| 实现原理 | 保持 ResultSet 打开，逐行滚动 | 每页一条 SQL（LIMIT/OFFSET） |
| 内存占用 | 恒定（一次一行） | 一页在内存（pageSize） |
| 数据库连接 | 整个 Step 持有一个连接（长事务风险） | 每页短连接，可配连接池 |
| 排序要求 | 无 | **必须排序键**（分页稳定性） |
| 断点续读 | 需自定义记录游标位置 | 页码存 ExecutionContext 天然可续 |
| 多线程 Step | 几乎不可用（连接共享冲突） | 可用（每线程独立页查询） |
| 适用数据库 | 全部 | 大表 + 分页优化良好的库（MySQL OFFSET 大偏移有坑） |
| 典型选择 | Oracle/PostgreSQL 大表游标 | MySQL 中等表、多线程 Step |

> 💡 大表分页注意：`OFFSET` 深翻页性能差，可改用"游标分页"（`WHERE id > ? ORDER BY id LIMIT n`），用 ExecutionContext 记录 last id。

## 7. Reader 选型决策表

| 场景 | 首选 Reader | 备选 |
|------|------------|------|
| CSV/TSV 导入 | FlatFileItemReader + DelimitedLineTokenizer | MultiResourceItemReader（多文件） |
| 固定宽度对账文件 | FlatFileItemReader + FixedLengthTokenizer | — |
| JSON 文件 | JsonItemReader（Jackson 3） | JsonLineTokenizer |
| XML 文件 | StaxEventItemReader | — |
| 关系库大表（内存敏感） | JdbcCursorItemReader（+fetchSize） | JpaCursorItemReader |
| 关系库中表 + 需续读 | JdbcPagingItemReader | JpaPagingItemReader |
| 记录类型直接映射 | dataRowMapper（5.2+/6.0） | BeanWrapperFieldSetMapper |
| Kafka 事件离线归集 | KafkaItemReader（saveState=true） | — |
| 多源顺序读 | CompositeItemReader（5.2+） | MultiResourceItemReader |
| 生产-消费流水线 | BlockingQueueItemReader（5.2+） | QueueItemReader |

---

> 🎯 **核心要点**：ItemReader 家族按"文件/DB/消息/函数"四类记忆；数据库读的核心决策是**游标（省内存、长连接）vs 分页（可续读、可并发）**；5.2 起的函数适配器与 dataRowMapper 大幅减少样板代码；6.0 移除 Mongo/Neo4j reader 并全面转向 Jackson 3，是版本迁移的雷区。

**下一模块**：[05-ItemProcessor与ItemWriter体系](05-ItemProcessor与ItemWriter体系.md)　|　**返回总览**：[00-Spring Batch知识体系总览](00-Spring Batch知识体系总览.md)
