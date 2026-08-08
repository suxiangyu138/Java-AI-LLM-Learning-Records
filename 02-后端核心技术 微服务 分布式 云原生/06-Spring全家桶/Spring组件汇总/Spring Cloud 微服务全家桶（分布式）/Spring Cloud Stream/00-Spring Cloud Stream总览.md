# Spring Cloud Stream 总览
> 基于 Spring Cloud Function 的统一消息驱动微服务框架，用一套 API 屏蔽 Kafka / RabbitMQ / Kinesis 等消息中间件的差异

## 📚 目录
1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [学习路线推荐](#3-学习路线推荐)
4. [核心概念速查](#4-核心概念速查)
5. [版本窗口说明](#5-版本窗口说明)
6. [参考来源](#6-参考来源)

## 1. 知识体系导图

```text
Spring Cloud Stream（5.0.2 / Spring Cloud 2025.1 Oakwood / Boot 4.0）
│
├─ 核心模型层 ──────────────────────────────
│   ├─ Destination（目的地：Topic / Exchange 逻辑名）
│   ├─ Binding（绑定：应用与目的地之间的管道）
│   ├─ Group（消费组：实例间负载均衡，组内竞争）
│   ├─ Partition（分区：有序性 + 可扩展性）
│   ├─ Binder 抽象（SPI：Kafka / RabbitMQ / Kinesis / GCP / Azure ...）
│   └─ 多 Binder（一套应用同时对接多种中间件）
│
├─ 编程模型层（函数式，3.0 起推荐 / 4.0 起唯一）──
│   ├─ Supplier<T>   = Source  数据源（轮询 / 定时 / 响应式）
│   ├─ Function<I,O> = Processor 处理器
│   ├─ Consumer<I>   = Sink    数据汇
│   ├─ 绑定命名规则：<函数名>-in-0 / -out-0
│   ├─ 函数定义与组合：definition / | 管道
│   └─ StreamBridge（REST 等外部来源注入流）
│
├─ Binder 实现层 ───────────────────────────
│   ├─ Kafka Binder（分区 / 批量 / 事务 / 响应式 / Kafka Streams）
│   ├─ RabbitMQ Binder（Exchange 拓扑 / 延迟 / 批量 / 死信）
│   └─ 其他：Kinesis / Google PubSub / Azure Event Hubs / Solace / Artemis
│
├─ 可靠性与错误处理 ────────────────────────
│   ├─ 重试（binder 级 RetryTemplate / 容器级 ErrorHandler）
│   ├─ 死信 DLQ（Kafka: error.<dest>.<group> / Rabbit: <dest>.<group>.dlq）
│   └─ 错误头（x-exception-message / x-original-topic ...）
│
└─ 工程化 ──────────────────────────────────
    ├─ 测试（Test Binder / InputDestination / OutputDestination）
    ├─ 监控（binder 指标 / Kafka 消费者组 offset）
    └─ 生产避坑（KIP 兼容 / 重试与 rebalance 冲突 / 多 binder 冲突）
```

## 2. 模块导航

| 序号 | 模块 | 核心内容 | 适合人群 |
|:---:|------|---------|---------|
| 00 | 总览 | 体系导图、路线、核心概念速查 | 所有人 |
| 01 | [模块清单与版本矩阵](01-模块清单与版本矩阵.md) | 全部 artifacts、Boot/Cloud/Stream 版本对应表 | 选型、升级 |
| 02 | [核心模型：Binder 抽象与目的地绑定](02-核心模型：Binder抽象与目的地绑定.md) | Destination/Binding/Group/Partition/Binder SPI | 入门必读 |
| 03 | [函数式编程模型与绑定生成](03-函数式编程模型与绑定生成.md) | Supplier/Function/Consumer、命名规则、组合、批量 | 入门必读 |
| 04 | [快速开始：Kafka 与 RabbitMQ 实战](04-快速开始：Kafka与RabbitMQ实战.md) | 双 Binder 完整 Demo + 运行验证 | 上手实操 |
| 05 | [绑定配置属性全解](05-绑定配置属性全解.md) | bindings.producer/consumer 全属性、内容类型、转换 | 进阶 |
| 06 | [Kafka Binder 深入](06-Kafka Binder深入.md) | 分区消费组、批量、事务、响应式、Kafka Streams | 进阶 |
| 07 | [RabbitMQ Binder 深入](07-RabbitMQ Binder深入.md) | Exchange/Queue 拓扑、延迟、批量、死信 | 进阶 |
| 08 | [消息可靠性与重试死信](08-消息可靠性与重试死信.md) | 重试两策略、DLQ 全解、错误头、修复回放 | 重点 |
| 09 | [StreamBridge 与事件路由](09-StreamBridge与事件路由.md) | 动态目的地、事件路由、MessageRoutingCallback、REST 集成 | 进阶 |
| 10 | [生产实践与选型避坑](10-生产实践与选型避坑.md) | 测试、监控、性能、对比选型、避坑清单、面试 | 收尾 |

## 3. 学习路线推荐

| 路线 | 人群 | 路径 |
|------|------|------|
| 入门路线（1~2 天） | 有 Spring Boot + Kafka/Rabbit 基础的开发 | 00 → 01 → 02 → 03 → 04 → 08（重试死信必读） |
| 进阶路线（3~5 天） | 要落地生产的工程师 | 入门路线 + 05 → 06/07 → 09 → 10 |
| 面试冲刺路线（半天） | 备战面试 | 00 核心概念速查 → 02（概念辨析）→ 03（函数式模型）→ 08（可靠性）→ 10（面试题） |

## 4. 核心概念速查

| 概念 | 一句话解释 | 关键配置前缀 |
|------|-----------|-------------|
| Binder | 桥接应用与消息中间件的 SPI 实现，绑定器决定"连谁、怎么连" | `spring.cloud.stream.binder` |
| Destination | 目的地逻辑名（Kafka 的 Topic、Rabbit 的 Exchange），由 Binder 映射到物理资源 | `bindings.<b>.destination` |
| Binding | 应用与目的地之间的连接，分 input（消费）与 output（生产） | `bindings.<b>` |
| Group | 消费组：同组实例竞争消费，实现负载均衡与水平扩展 | `bindings.<b>.group` |
| Partition | 分区：保证同 key 消息有序，输出与输入分区一一映射 | `bindings.<b>.producer.partitionKeyExpression` |
| 函数式模型 | 用 `Supplier`/`Function`/`Consumer` 声明消息源/处理器/汇，替代已删除的 `@EnableBinding`/`@StreamListener` | `spring.cloud.function.definition` |
| StreamBridge | 程序化向输出目的地发送消息（REST 入口注入流） | `spring.cloud.stream.source` |
| DLQ | 死信队列/主题：重试耗尽后的归宿，含异常元信息头 | Kafka: `kafka.bindings.<b>.consumer.enable-dlq` |
| Test Binder | 测试专用 Binder，不连真实中间件，直接断言消息 | `spring-cloud-stream-test-binder` |

## 5. 版本窗口说明

| 项 | 本文档基准版本 | 说明 |
|----|---------------|------|
| Spring Cloud Stream | 5.0.2 | 最新稳定版，Spring Cloud 2025.1.x（Oakwood）发布列车成员 |
| Spring Cloud | 2025.1.x（Oakwood） | 5.0.0 起随此列车发布；5.0.1 起兼容 Boot 4.0.1+ |
| Spring Boot | 4.0.x | 5.0.x 基线；4.2.x 旧列车对应 Boot 3.x |
| 编程模型 | 函数式（唯一） | 4.0 起注解模型（`@EnableBinding`/`@StreamListener`）已被彻底移除 |
| 兼容边界 | 4.2.x（Boot 3.x） | 存量 Boot 3 项目迁移时关注 4.0 Migration guide 差异点 |

> ⚠️ **时效性**：本文档按 2026-08 检索到的官方资料编写。版本号、默认值与属性清单请以 [Spring Cloud Stream 官方参考文档](https://docs.spring.io/spring-cloud-stream/reference/) 为准；升级前务必查阅对应版本的 [Migration guide](https://github.com/spring-cloud/spring-cloud-stream/wiki)。

## 6. 参考来源

- [Spring Cloud Stream Reference（5.0.x）](https://docs.spring.io/spring-cloud-stream/reference/)
- [Producing and Consuming Messages（函数式模型）](https://docs.spring.io/spring-cloud-stream/reference/spring-cloud-stream/producing-and-consuming-messages.html)
- [Kafka Binder Reference（Retry/DLQ）](https://docs.spring.io/spring-cloud-stream/reference/4.2/kafka/kafka-binder/retry-dlq.adoc)
- [RabbitMQ Binder Reference](https://docs.spring.io/spring-cloud-stream/reference/4.3/rabbit/rabbit_overview.html)
- [Spring Cloud 2025.1.1 (Oakwood) 发布博客](https://spring.io/blog/2026/01/29/spring-cloud-2025-1-1-aka-oakwood-has-been-released)
- [spring-cloud-stream GitHub Releases](https://github.com/spring-cloud/spring-cloud-stream/releases)
- [4.0 Migration guide](https://github.com/spring-cloud/spring-cloud-stream/wiki/4.0-Migration-guide)

---

**下一模块**：[01-模块清单与版本矩阵](01-模块清单与版本矩阵.md)
