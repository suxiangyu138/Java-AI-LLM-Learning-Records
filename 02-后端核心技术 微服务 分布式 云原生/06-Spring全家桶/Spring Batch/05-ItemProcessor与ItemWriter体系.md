# 05 ItemProcessor 与 ItemWriter 体系

> Processor 负责"变"（转换/校验/过滤），Writer 负责"落"（入库/写文件/发消息）——本章讲清处理器的四种模式、写入器的分类与幂等设计，以及 commit-interval 背后的提交边界语义

---

## 📚 目录

1. [ItemProcessor 契约与四种处理模式](#1-itemprocessor-契约与四种处理模式)
2. [校验器体系](#2-校验器体系)
3. [ItemWriter 契约与提交边界](#3-itemwriter-契约与提交边界)
4. [数据库写入器](#4-数据库写入器)
5. [文件与消息写入器](#5-文件与消息写入器)
6. [组合写入与幂等设计](#6-组合写入与幂等设计)
7. [Writer 选型决策表](#7-writer-选型决策表)

---

## 1. ItemProcessor 契约与四种处理模式

```java
public interface ItemProcessor<I, O> {
    @Nullable
    O process(I item) throws Exception;
}
```

**返回 null = 过滤掉该条数据**（不写入、FILTER_COUNT 加一）；抛异常则进入容错流程（模块 06）。

### 1.1 四种处理模式总览

| 模式 | 代表实现 | 场景 |
|------|---------|------|
| 转换 Transform | FunctionItemProcessor、ItemProcessorAdapter、自定义 | 字段补全、格式转换、类型映射 |
| 过滤 Filter | PredicateFilteringItemProcessor（5.2+）、process 返回 null | 数据质量剔除 |
| 校验 Validate | ValidatingItemProcessor + Validator | 读入后先验后写 |
| 组合/分类 | CompositeItemProcessor、ClassifierCompositeItemProcessor | 多步骤处理链、按类型分流 |

### 1.2 转换与过滤

```java
// 函数式（5.2+，替代匿名类）
FunctionItemProcessor<Order, Order> enrich = new FunctionItemProcessor<>(order -> {
    order.setFullName(order.getFirstName() + " " + order.getLastName());
    return order;
});

// 谓词过滤（5.2+）
PredicateFilteringItemProcessor<Order, Order> onlyVip = new PredicateFilteringItemProcessor<>(Order::isVip);

// 包装已有 Service 方法
ItemProcessorAdapter<Order, Order> adapter = new ItemProcessorAdapterBuilder<Order, Order>()
        .targetObject(orderService)
        .targetMethod("enrich")
        .build();
```

### 1.3 组合与分类

```java
// 顺序执行多个 processor（前一个输出 = 后一个输入）
CompositeItemProcessor<Order, Order> chain = new CompositeItemProcessorBuilder<Order, Order>()
        .delegates(List.of(validate, enrich, normalize))
        .build();

// 按类型/条件路由到不同 processor
ClassifierCompositeItemProcessor<Order, Order> classifier = new ClassifierCompositeItemProcessorBuilder<Order, Order>()
        .classifier(new SubclassClassifier<>(
                new Classifier<Order, ItemProcessor<?, ? extends Order>>() {
                    @Override
                    public ItemProcessor<?, ? extends Order> classify(Order order) {
                        return order.isVip() ? vipProcessor : normalProcessor;
                    }
                }))
        .build();
```

## 2. 校验器体系

`ValidatingItemProcessor` 持有一个 `Validator<T>`，`validate()` 抛 `ValidationException` 时触发容错（可 skip）：

```java
public class OrderValidator implements Validator<Order> {
    @Override
    public void validate(Order order) throws ValidationException {
        if (order.getAmount() == null) {
            throw new ValidationException("amount is null: " + order.getId());
        }
    }
}

// 装配
ValidatingItemProcessor<Order> validator = new ValidatingItemProcessor<>(new OrderValidator());
```

| 内置校验器 | 能力 |
|-----------|------|
| BeanValidatingItemProcessor | 基于 JSR-303 Bean Validation 注解（@NotNull、@Email…） |
| BeanValidatingRecordItemProcessor（5.2+） | 同上的 record 版本 |
| 自定义 | 实现 Validator 接口 |

> 💡 实践顺序：**先校验、后转换、再写**——校验失败的走 skip 计数，避免脏数据入库后再靠数据库约束兜底。

## 3. ItemWriter 契约与提交边界

```java
public interface ItemWriter<T> {
    void write(Chunk<? extends T> chunk) throws Exception;   // 6.0：批量接收 Chunk（5.1+ 为 List）
}
```

提交边界语义（与模块 03 呼应）：

- 一个 chunk（commit-interval 条）调用一次 `write(List)`；
- **write 在事务内执行**：成功后提交、失败整体回滚本块（配合 skip/retry）；
- 数据库写入器通过 `itemSqlParameterSourceProvider`/JPA 持久化上下文在事务中批量执行；
- 非事务型写入器（文件、消息队列）**没有原子性**——文件写一半、Kafka 发了一半，重启需靠幂等（模块 06）。

## 4. 数据库写入器

### 4.1 JdbcBatchItemWriter —— 生产首选

```java
@Bean
JdbcBatchItemWriter<Order> orderWriter(DataSource dataSource) {
    return new JdbcBatchItemWriterBuilder<Order>()
            .dataSource(dataSource)
            .sql("INSERT INTO t_order (id, customer_id, amount, status) VALUES (:id, :customerId, :amount, 'NEW')")
            .beanMapped()                       // 按 bean 属性名绑定命名参数
            .build();
}
```

| 参数绑定方式 | 适用 |
|-------------|------|
| `beanMapped()` | 普通 POJO（属性名 = 参数名） |
| `itemSqlParameterSourceProvider(...)` | 自定义绑定（Map/record/复杂对象） |
| `recordMapped()`（5.2+） | record 类型直接映射 |

> 💡 性能要点：JDBC 批量写入 + `rewriteBatchedStatements=true`（MySQL）可显著提升吞吐；`assertUpdates(true)` 校验影响行数，防静默丢失。

### 4.2 JPA / Hibernate / Repository

```java
JpaItemWriter<Order> jpaWriter = new JpaItemWriterBuilder<Order>()
        .entityManagerFactory(emf)
        .usePersist(true)        // true=persist（新数据），false=merge
        .build();

RepositoryItemWriter<Order> repoWriter = new RepositoryItemWriterBuilder<Order>()
        .repository(orderRepository)
        .methodName("save")
        .build();
```

> ⚠️ 陷阱：`usePersist(false)`（merge）会先查询再更新，大表逐条 merge 极慢；纯插入场景务必 `usePersist(true)`。

## 5. 文件与消息写入器

### 5.1 文件类

```java
// 平面文件（CSV/固定宽）+ 表头表尾
FlatFileItemWriter<Order> writer = new FlatFileItemWriterBuilder<Order>()
        .name("orderFileWriter")
        .resource(new FileSystemResource("output/orders.csv"))
        .delimited().delimiter(",")
        .names("id", "customerId", "amount")
        .headerCallback(w -> w.write("id,customerId,amount"))
        .footerCallback(w -> w.write("total," + counter))
        .shouldDeleteIfEmpty(true)
        .shouldDeleteIfExists(true)         // 覆盖写
        .append(false)                       // 追加模式
        .build();

// JSON 数组写入（6.0 走 Jackson 3）
JsonFileItemWriter<Order> jsonWriter = new JsonFileItemWriterBuilder<Order>()
        .name("orderJsonWriter")
        .resource(resource)
        .jsonObjectMarshaller(jsonMarshaller)
        .build();
```

> ⚠️ 6.0.4 修复：`JsonFileItemWriter` + `appendAllowed(true)` 多次 open/write/close 会破坏 JSON 数组结构（多段 `[...]` 拼接），6.0.4/5.2.6 已修复，但追加 JSON 仍建议先写临时文件再合并。

### 5.2 消息类（无事务原子性）

| 写入器 | 说明 |
|--------|------|
| KafkaItemWriter | 基于 KafkaTemplate；**Kafka 自身事务**可与 Spring 事务整合（`chunk.transactionManager` 配 KafkaTransactionManager） |
| JmsItemWriter / AmqpItemWriter | 基于 JmsTemplate / AmqpTemplate；JMS 可与本地事务同域（XA 或单 broker 事务） |
| ConsumerItemWriter（5.2+） | `Consumer<T>` 适配，如写日志、调外部 API |

消息写入的可靠性结论：**"写 DB + 发消息"不是一个本地事务能覆盖的**——要么用 Kafka/JMS 参与事务（XA 或 Outbox 模式），要么接受"至少一次"投递 + 消费端幂等。

## 6. 组合写入与幂等设计

### 6.1 组合/分类写入

```java
// 同一份数据写多个目标
CompositeItemWriter<Order> composite = new CompositeItemWriterBuilder<Order>()
        .delegates(List.of(dbWriter, archiveWriter, auditWriter))
        .build();

// 按条件路由写入目标（新单走表、退款单走消息）
ClassifierCompositeItemWriter<Order> classifier = new ClassifierCompositeItemWriterBuilder<Order>()
        .classifier(new SubclassClassifier<>(order ->
                order.getStatus().equals("REFUND") ? kafkaWriter : dbWriter))
        .build();
```

### 6.2 幂等：ItemKeyGenerator

写入目标无自然主键（或主键由外部决定）时，用 `ItemKeyGenerator` 生成幂等键，配合"存在即更新/跳过"实现可重启安全：

```java
JdbcBatchItemWriter<Order> idempotentWriter = new JdbcBatchItemWriterBuilder<Order>()
        .dataSource(dataSource)
        .sql("INSERT INTO t_order (... ) VALUES (...) ON DUPLICATE KEY UPDATE amount = VALUES(amount)")
        .itemKeyGenerator(order -> order.getId())   // 唯一性来源
        .build();
```

> 🎯 幂等设计三原则：**写前可查重、写时用唯一键、重跑能覆盖**。批处理重启（模块 06）依赖"已处理部分可重放而不产生重复"，不是"恰好一次"而是"至少一次 + 去重"。

## 7. Writer 选型决策表

| 目标 | 首选 Writer | 注意事项 |
|------|------------|---------|
| 关系库批量插入 | JdbcBatchItemWriter | beanMapped/recordMapped、assertUpdates |
| JPA 实体写入 | JpaItemWriter | usePersist(true) 防慢 merge |
| Spring Data 仓储 | RepositoryItemWriter | 逐条操作，仅小数据量 |
| CSV 导出 | FlatFileItemWriter | header/footer、覆盖/追加策略 |
| JSON 数组导出 | JsonFileItemWriter（Jackson 3） | append 模式注意 6.0.4 修复 |
| Kafka 事件下发 | KafkaItemWriter | 事务一致性选 KafkaTransactionManager |
| JMS/RabbitMQ | JmsItemWriter / AmqpItemWriter | 结合 broker 事务 |
| 多目标同步写 | CompositeItemWriter | 任一 delegate 失败则整体回滚（事务型） |
| 按类分流 | ClassifierCompositeItemWriter | 配合 SubclassClassifier |

---

> 🎯 **核心要点**：Processor 记住"返回 null 即过滤"；Writer 记住"一个 chunk 一次 write、一次事务"；数据库写入用 JdbcBatchItemWriter 拿批量性能，消息写入要正视"无原子性"并用幂等键兜底；6.0 的 Chunk API（write(Chunk)）与 Jackson 3 迁移是版本关注点。

**下一模块**：[06-容错机制：Skip、Retry与重启](06-容错机制：Skip、Retry与重启.md)　|　**返回总览**：[00-Spring Batch知识体系总览](00-Spring Batch知识体系总览.md)
