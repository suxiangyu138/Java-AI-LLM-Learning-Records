# 00 Project Reactor 组件总览

> 组件卡片：Project Reactor 是什么、版本现状、能做什么、与深度体系如何衔接——本系列是"组件汇总"速查型文档；Reactor 是 JVM 响应式流的参考实现（Mono/Flux），暂无独立深度体系，深挖见官方参考文档

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

**Project Reactor 是 JVM 响应式流（Reactive Streams）的参考实现**——以 `Mono`（0/1 个元素）与 `Flux`（0..N 个元素）两个发布者为核心，用操作符链描述"异步数据流"，配合背压与调度器实现非阻塞、可组合、高并发的数据处理。

```text
核心心智模型：
  发布者（Mono / Flux）
    │ 操作符链（不触发不执行：冷/惰性）
    │   map → flatMap → filter → zip → retry …
    ▼
  订阅（subscribe()）才真正执行
    │ 背压：下游 request(n) 反推上游生产
    │ 调度器：parallel / boundedElastic / single / immediate
    ▼
  终止：onComplete / onError（错误处理操作符兜底）
```

> 🎯 **一句话**：Reactor = "异步流式编程的 Java 母语"——WebFlux、R2DBC、RSocket、Spring Cloud Gateway 的底层都是它；理解 Mono/Flux 就理解了 Spring 响应式生态的一切。

## 2. 版本现状（2026-08）

| 组件 | 最新版本 | 发布时间 | 说明 |
|------|---------|---------|------|
| reactor-core | **3.8.6**（3.8.5 于 2026-05） | 2026 上半年 | 当前主线（2025.0.x Release Train） |
| reactor-netty | 2.x（随 2025.0.x） | — | WebFlux/RSocket 默认传输 |
| reactor-test | 3.8.x | — | StepVerifier 测试工具 |
| reactor-kafka / reactor-pool / reactor-adapter / reactor-extra | 各自 1.x/3.x | — | 按需引入 |

版本要点（2025.0.x 发布火车）：

- 3.8.5（2026-05）：升级 ByteBuddy 1.18.8、Micrometer 1.16.5、Micrometer Tracing 1.6.5；
- 3.8.4：新增"订阅模式（subscription patterns）"文档章节；
- **Spring Boot 4 配套 Reactor 3.8.x**（Spring Shell 4.0.3 依赖升级即 Reactor 3.8.6）；
- 与 Micrometer 观测 / Micrometer Tracing 深度集成（`Mono.deferContextual` + 观测 API）。

> 💡 版本线：Reactor 3.8.x ↔ Boot 4 / SF 7；Reactor 3.7.x ↔ Boot 3.x——版本由 Boot BOM 管理，一般无需手工指定。

## 3. 能力地图

| 能力域 | 能力 | 代表 API |
|--------|------|---------|
| 数据流 | Mono（0/1）/ Flux（0..N）双发布者 | `Mono.just`、`Flux.range`、`fromIterable` |
| 操作符 | 变换/组合/过滤/窗口/条件 100+ | map、flatMap、concatMap、zip、filter、buffer、window |
| 调度 | 线程切换与执行模型 | Schedulers.parallel/boundedElastic/single/immediate |
| 背压 | 需求驱动生产 | request(n)、onBackpressureBuffer/Drop/Latest、limitRate |
| 错误处理 | 声明式兜底 | onErrorReturn、onErrorResume、onErrorMap、retry、retryWhen |
| 响应式上下文 | 贯穿流的上下文传播 | `deferContextual`、ContextView |
| 组合 | 异步编排 | zip、merge、concat、switchMap、flatMapSequential |
| 取消 | 资源释放 | Disposable、doOnCancel、finally |
| 观测 | 指标/追踪 | Micrometer 观测、Tracing（3.8.x 升级） |
| 测试 | 虚拟时间/断言 | StepVerifier、StepVerifier.withVirtualTime |
| 调试 | 组装栈追踪 | `Hooks.onOperatorDebug()`（调试代理） |

## 4. 与深度体系的映射

| 本卡片模块 | 深度体系对应 | 衔接说明 |
|-----------|-------------|---------|
| 02 核心类 | [Spring WebFlux 卡片](../Spring WebFlux/00-Spring WebFlux组件总览.md)（同级） | WebFlux 是 Reactor 最大的消费方，两者配套学习 |
| 04 集成地图 | [Spring Batch 深度体系-07 并行与分布式](../../../Spring Batch/07-并行与分布式：多线程、分区与远程执行.md) | Reactor 3.8.x 是 Batch 生产者-消费者模型的线程技术背景 |
| 04 集成地图 | [消息队列理论与实战](../../../../03-消息队列/消息队列理论与实战/00-消息队列知识体系总览.md) | reactor-kafka 是 Kafka 的响应式客户端 |

> 💡 Reactor 生态位：它是**数据流编程库**（非 Web 框架）——WebFlux/R2DBC/RSocket/Gateway 全部建立其上；本仓库暂无独立深度体系，深入以 [官方参考文档](https://projectreactor.io/docs/core/release/reference/) 为准。

## 5. 快速上手 3 步

```text
① 引入依赖：WebFlux 项目自带 reactor-core；纯流处理加 io.projectreactor:reactor-core
② 写操作符链（惰性：不订阅不执行）
③ subscribe() 触发；测试用 StepVerifier 断言
```

```java
// 最小数据流：变换 + 过滤 + 订阅
Flux.range(1, 10)                       // 发布 1..10
    .map(i -> i * i)                    // 平方
    .filter(n -> n % 2 == 0)            // 保留偶数
    .subscribe(System.out::println);    // 触发执行

// 异步编排（响应式风格）
Mono<User> user = userRepo.findById(id);       // 假想响应式 DAO
Mono<Order> order = orderRepo.findByUserId(id);
Mono.zip(user, order)
    .map(t -> new Profile(t.getT1(), t.getT2()))
    .doOnError(e -> log.error("failed", e))
    .subscribe(profile -> log.info("profile: {}", profile));
```

> 💡 心智起点：**写操作符链像写"数据流的 SQL"——声明什么，不执行什么**；订阅（subscribe）才是执行信号。

## 6. 速查导航

| 卡片 | 内容 | 何时翻 |
|------|------|--------|
| [01-模块清单](01-模块清单.md) | artifact 与 Release Train 对照 | 加依赖、选客户端库 |
| [02-核心类与注解速查](02-核心类与注解速查.md) | Mono/Flux/调度器/操作符分类 | 写响应式代码时随手查 |
| [03-配置属性速查](03-配置属性速查.md) | reactor 调试/网络/codec 属性 | 写 application.yml 时 |
| [04-集成地图与常见问题](04-集成地图与常见问题.md) | 生态集成 + 高频坑 + 排障流程 | 集成/排障 |

## 7. 学习路线推荐

```text
快速上手：00 → 01 → 05（三步）→ 02 核心类
响应式开发：00 → 02（操作符分类）→ 04（WebFlux/R2DBC 集成）
排障进阶：00 → 03（调试配置）→ 04（背压/上下文坑）→ 官方调试指南
```

## 8. 核心概念速查

| 概念 | 一句话 |
|------|--------|
| Mono / Flux | 0/1 个元素 / 0..N 个元素的异步发布者 |
| 操作符 | 流上声明式变换（惰性） |
| 背压 | 下游 request(n) 反推上游生产 |
| Scheduler | 执行线程模型（parallel/boundedElastic/single） |
| 订阅 | subscribe() 触发整条链执行 |
| Context | 跨操作符的只读上下文传播 |
| StepVerifier | 测试发布者（支持虚拟时间） |
| Disposable | 订阅句柄（dispose 取消） |
| Hooks | 全局调试钩子（onOperatorDebug） |

---

> 🎯 **核心要点**：Reactor = Mono/Flux + 操作符 + 背压 + 调度器四大件；"惰性、背压、不可变"是三大心智原则；3.8.x 与 Micrometer/Tracing 的深度集成让响应式可观测成为标配。

**下一模块**：[01-模块清单](01-模块清单.md)
