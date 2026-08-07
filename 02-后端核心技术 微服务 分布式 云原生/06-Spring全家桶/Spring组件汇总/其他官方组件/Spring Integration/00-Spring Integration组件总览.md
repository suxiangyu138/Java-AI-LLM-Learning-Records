# 00 Spring Integration 组件总览

> 组件卡片：Spring Integration 是什么、版本现状、能做什么、与深度体系如何衔接——本系列是"组件汇总"速查型文档；Spring Integration 是 Spring 生态的"企业集成模式（EIP）"实现与消息驱动基础设施，暂无独立深度体系，深挖见官方参考文档

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

**Spring Integration 是 Spring 生态的"企业集成模式（EIP）"实现**——用消息（Message）在通道（Channel）上串联转换器、过滤器、路由器、拆分器、聚合器等组件，实现系统间的解耦集成，也是 Spring Batch 远程执行、事件驱动流水线的底层基础设施。

```text
核心心智模型：
  Message（消息 = headers + payload）
    ↓ 发送到
  MessageChannel（通道）
    ├── SubscribableChannel（广播：Direct/PublishSubscribe/Executor）
    └── PollableChannel（可轮询：Queue）
    ↓ 组件端点（Endpoint）串接
  Transformer → Filter → Router → Splitter → Aggregator → ServiceActivator
    （全部可用 Java DSL 的 IntegrationFlow 一行式声明）
```

> 🎯 **一句话**：Spring Integration = "消息中间件无关的 EIP 实现"——把集成逻辑写成流程图（DSL），底层可切换内存/文件/JDBC/Kafka/AMQP/JMS 等任意通道。

## 2. 版本现状（2026-08）

| 版本线 | 最新 | 发布时间 | 配套 | 状态 |
|--------|------|---------|------|------|
| **7.1.x** | 7.1.0 | 2026-06 上旬 | Spring Framework 7.0.7 / Boot 4.0.x | **当前主线** |
| 7.0.x | 7.0.x | 2025-11 起 | SF 7.0 / Boot 4.0 | 首个 7.x 线 |
| 6.4.x | 6.4.x | — | SF 6.2 / Boot 3.x | 存量维护线 |

7.1.0 亮点（2026-06）：

- **CloudEvents 支持**（7.1.0-M2 引入）：`FromCloudEventTransformer` 与 `CloudEvents` 工具类，云事件规范开箱即用；
- **gRPC DSL**（7.1.0-M2 引入）：inbound/outbound gateway 的 DSL 构建器；
- **消息选择器**：基于 header 值匹配模式的消息选择（MessageSelector）；
- `@CrossOrigin` 对齐 Spring MVC：禁 `allowCredentials`、引入 `originPatterns`；
- `IntegrationPatternType` 新增 `message_store` 模式；`ExpressionEvaluatingMessageProcessor` 改进；
- 大量修复：防止文件写出输出目录（安全）、Redis/JDBC LockRegistry 竞态、gRPC inbound 错误传播等。

> 💡 版本线对应：SI 7.x = Boot 4 / SF 7；SI 6.x = Boot 3 / SF 6——与 Boot 大版本强绑定，禁止跨线混搭。

## 3. 能力地图

| 能力域 | 能力 | 代表组件 |
|--------|------|---------|
| 消息模型 | Message / MessageChannel / 选择器 | MessageBuilder、DirectChannel、QueueChannel、PublishSubscribeChannel |
| EIP 模式 | 转换/过滤/路由/拆分/聚合/桥接/增强 | Transformer、Filter、Router、Splitter、Aggregator、Bridge |
| Java DSL | 流程图式声明 | IntegrationFlow.from(...).transform().filter().handle() |
| 消息网关 | 接口代理 | @MessagingGateway |
| 通道适配器 | 30+ 外部系统适配 | file/jdbc/jpa/redis/mongo/kafka/amqp/jms/mqtt/http/webflux/ws/mail/sftp/ftp/rsocket… |
| 事务 | 消息流事务绑定 | TransactionSynchronizationFactory、@Transactional 端点 |
| 聚合 | 相关器/释放策略/消息存储 | CorrelationStrategy、ReleaseStrategy、MessageGroupStore |
| 锁与分布式 | 跨实例互斥 | JdbcLockRegistry、RedisLockRegistry |
| 错误处理 | errorChannel / 错误网关 | ErrorMessage、errorChannel（全局默认） |
| 可观测 | Micrometer 观测 | spring.integration.* 指标、Observation 集成 |
| 批量桥接 | 与 Spring Batch 联动 | 远程分区/远程 Chunking/SEDA 通道 |
| 响应式 | 反应式集成流 | FluxMessageChannel、Kotlin DSL、Reactive 网关 |

## 4. 与深度体系的映射

| 本卡片模块 | 深度体系对应 | 衔接说明 |
|-----------|-------------|---------|
| 02 核心类 | [Spring Batch 深度体系-07 并行与分布式](../../../Spring Batch/07-并行与分布式：多线程、分区与远程执行.md) | 远程分区/远程 Chunking 全部依赖 SI 通道（本卡片是那些通道的"底层原理"） |
| 04 集成地图 | [消息队列理论与实战](../../../../03-消息队列/消息队列理论与实战/00-消息队列知识体系总览.md) | SI 是"中间件无关"抽象层，MQ 是底层传输 |
| 04 集成地图 | [Spring Batch 深度体系-08 调度、运维与可观测性](../../../Spring Batch/08-调度、运维与可观测性.md) | 事件触发批处理（消息到达 → 启 Job） |

> 💡 Spring Integration 生态位说明：它常与 Spring Batch（离线批）、Spring AMQP/Kafka（在线消息）、Spring Cloud Stream（消息驱动微服务）配合使用——本仓库暂无 SI 独立深度体系，需要深入时以 [官方参考文档](https://docs.spring.io/spring-integration/reference/) 为准。

## 5. 快速上手 3 步

```text
① 引入 spring-boot-starter-integration（Boot 4 管理版本）
② 用 IntegrationFlow DSL 声明一条消息流
③ 通过网关/通道/定时源触发，观察端到端处理
```

```java
// 最小消息流：定时生成 → 转换 → 落日志（Boot 4 / SI 7）
@Configuration(proxyBeanMethods = false)
class BasicFlowConfig {
    @Bean
    IntegrationFlow tickFlow() {
        return IntegrationFlow.from(() -> "tick",
                        spec -> spec.poller(p -> p.fixedDelay(1000)))  // 轮询源
                .transform(String::toUpperCase)                        // 转换
                .handle(message -> log.info("got: {}", message.getPayload())) // 处理
                .get();
    }
}
```

> 💡 运行即得：默认 `errorChannel` 捕获异常；端点自动注册为 Bean；`@MessagingGateway` 可把接口变成发送代理。

## 6. 速查导航

| 卡片 | 内容 | 何时翻 |
|------|------|--------|
| [01-模块清单](01-模块清单.md) | artifact 与 Starter 对照（30+ 适配器模块） | 加依赖、选适配器 |
| [02-核心类与注解速查](02-核心类与注解速查.md) | 消息/通道/EIP 组件/DSL/注解 | 写消息流时随手查 |
| [03-配置属性速查](03-配置属性速查.md) | spring.integration.* 属性字典 | 写 application.yml 时 |
| [04-集成地图与常见问题](04-集成地图与常见问题.md) | 生态集成 + 高频坑 + 排障流程 | 集成/排障 |

## 7. 学习路线推荐

```text
快速上手：00 → 01 → 05（DSL 三步）→ 02 核心类
消息流架构：00 → 02 → 03 → 官方 EIP 参考（路由/聚合/事务）
与批处理联动：00 → 04 集成地图 → Spring Batch 深度体系 07（远程执行）
```

## 8. 核心概念速查

| 概念 | 一句话 |
|------|--------|
| Message | headers + payload 的不可变消息 |
| MessageChannel | 消息通道（Subscribable 广播 / Pollable 队列） |
| 端点 Endpoint | 通道上的处理单元（适配器/网关/服务激活器） |
| IntegrationFlow | Java DSL 声明的消息流（流程图即代码） |
| @MessagingGateway | 接口代理：调方法 = 发消息 |
| Poller | 轮询源/轮询端点（fixedDelay/cron） |
| errorChannel | 全局错误通道（默认已存在） |
| MessageGroupStore | 聚合/分组的持久化存储 |
| LockRegistry | 跨实例互斥锁（JDBC/Redis 实现） |

---

> 🎯 **核心要点**：Spring Integration 是"用消息流实现 EIP 的粘合层"——DSL 是心智入口，通道适配器是扩展点，errorChannel/MessageStore/LockRegistry 是生产三件套；7.1.0 的新鲜点（CloudEvents、gRPC DSL、message_store 模式）值得在方案评审时提及。

**下一模块**：[01-模块清单](01-模块清单.md)
