# Spring Task 知识体系总览

> 从 TaskScheduler 调度架构、@Scheduled 执行语义、动态 Trigger，到 @Async 异步任务、线程池调优，再到 Spring Boot 4 的虚拟线程调度变革与分布式一致性——Spring Task 是"在 JVM 里把定时任务和异步任务做对"的完整答案

---

## 📚 目录

1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [为什么必须系统学透 Spring Task](#3-为什么必须系统学透-spring-task)
4. [核心概念速查](#4-核心概念速查)
5. [与周边知识的关系](#5-与周边知识的关系)
6. [学习路线推荐](#6-学习路线推荐)
7. [快速自测 10 题](#7-快速自测-10-题)

---

## 1. 知识体系导图

```text
Spring Task 知识体系
│
├── 01 任务调度架构：TaskScheduler 与 ScheduledTaskRegistrar
│   ├── TaskScheduler 抽象与 ThreadPoolTaskScheduler
│   ├── ScheduledTaskRegistrar：任务的注册中枢
│   ├── @EnableScheduling 的装配机制
│   └── 调度器线程模型：单线程的坑与多线程改造
│
├── 02 @Scheduled 注解全解：表达式与执行语义
│   ├── fixedDelay / fixedRate / cron / 自定义 Trigger
│   ├── 时区、initialDelay、占位符与 SpEL
│   ├── 单线程调度器下的执行语义陷阱
│   ├── cron 表达式六位/七位全解
│   └── 自调用与代理失效问题
│
├── 03 动态定时任务：SchedulingConfigurer 与 Trigger
│   ├── @Scheduled 静态化的本质与局限
│   ├── SchedulingConfigurer + TriggerTask 动态模式
│   ├── cron 动态变更的生效时机
│   └── 动态间隔（非 cron）任务实现
│
├── 04 异步任务：@Async 与线程池
│   ├── @Async 的代理机制与生效条件
│   ├── TaskExecutor 体系与线程池选型
│   ├── 返回值：void / Future / CompletableFuture
│   ├── 异步异常处理与回调
│   └── 事务与 SecurityContext 在异步中的传播
│
├── 05 线程池配置与性能调优
│   ├── spring.task.scheduling / execution 属性全解
│   ├── 池参数设计：核心/最大/队列/拒绝策略
│   ├── 任务提交者的自保护：队列打满怎么办
│   ├── 优雅关闭：await-termination 与超时
│   └── 线程池监控与动态调参
│
├── 06 虚拟线程与 Spring Boot 4 的调度变革
│   ├── 虚拟线程原理：M:N 调度与廉价线程
│   ├── Boot 4 默认：SimpleAsyncTaskScheduler + 虚拟线程
│   ├── pool-size 属性为何被忽略
│   ├── ThreadLocal 传播与 pinning 陷阱
│   └── 何时关闭虚拟线程（spring.threads.virtual.enabled=false）
│
├── 07 定时任务的异常处理与可观测
│   ├── 异常吞掉与任务中断的默认行为
│   ├── errorHandler 与失败告警
│   ├── 任务超时与重叠执行的监控
│   ├── Micrometer 指标与日志规范
│   └── 幂等与重试策略
│
├── 08 分布式定时任务：多实例一致性与 ShedLock
│   ├── 多实例重复执行的经典问题
│   ├── ShedLock：基于锁的分布式调度
│   ├── Quartz / XXL-Job 对比与选型
│   ├── 锁粒度、过期与续期设计
│   └── 与 Redis 分布式锁的异同
│
└── 09 生产实践与面试题
    ├── 黄金实践十二条
    ├── 定时任务故障排查手册
    ├── 面试高频 15 问
    └── 版本特性速查（2026）
```

## 2. 模块导航

| 序号 | 模块 | 核心内容 | 适合人群 |
|:---:|------|----------|---------|
| 01 | 任务调度架构 | TaskScheduler、ScheduledTaskRegistrar、装配机制 | 入门必读 |
| 02 | @Scheduled 注解全解 | 执行语义、cron 表达式、时区、陷阱 | 入门必读 |
| 03 | 动态定时任务 | SchedulingConfigurer、Trigger、动态 cron | 进阶 |
| 04 | 异步任务：@Async 与线程池 | 代理机制、线程池、返回值、异常 | 高频核心 |
| 05 | 线程池配置与性能调优 | 属性全解、池参数、优雅关闭 | 进阶 |
| 06 | 虚拟线程与 Boot 4 变革 | 虚拟线程原理、默认调度器、陷阱 | 高频核心 |
| 07 | 异常处理与可观测 | errorHandler、监控、告警、幂等 | 进阶 |
| 08 | 分布式定时任务 | 多实例一致性、ShedLock、选型 | 高级 |
| 09 | 生产实践与面试题 | 实践十二条、故障排查、面试题 | 冲刺 |

## 3. 为什么必须系统学透 Spring Task

**1）定时任务与异步任务是每个后端系统的"基础设施"**——报表生成、数据同步、缓存刷新、延迟清理、短信通知全部依赖它。写错一行调度配置，轻则任务不执行，重则重复执行导致数据错误。

**2）默认配置就是最大的坑。** 默认单线程调度器（一个任务阻塞全队）、@Scheduled 静态化（改 cron 要重启）、多实例重复执行——三大经典生产事故的根源都在"默认行为没理解"。

**3）Spring Boot 4 的虚拟线程变革改变了答案。** Boot 4.0（2025-11）在 Java 21+ 上默认用虚拟线程执行 @Scheduled/@Async：`SimpleAsyncTaskScheduler` 替代线程池调度器、pool-size 属性被忽略、ThreadLocal 传播靠上下文快照——2026 年面试"定时任务怎么做"的新答案必须包含虚拟线程。

**4）分布式一致性是架构分水岭。** 单实例时代的 @Scheduled 在多实例部署下会重复执行——ShedLock、Quartz、XXL-Job 的选型与锁设计，是微服务化后的必修课。

> 🎯 **核心要点**：Spring Task = 调度（TaskScheduler + @Scheduled）+ 异步（TaskExecutor + @Async）两条主线，共用一套线程池配置体系。学它 = 学"任务怎么被安排、在哪个线程跑、失败了怎么办、多实例怎么不错不乱"。

## 4. 核心概念速查

| 概念 | 一句话本质 | 关键类/注解 |
|------|-----------|------------|
| TaskScheduler | 调度抽象：安排任务在"未来时刻"执行 | `TaskScheduler`/`ThreadPoolTaskScheduler` |
| TaskExecutor | 执行抽象：任务在线程池中的运行 | `TaskExecutor`/`ThreadPoolTaskExecutor` |
| @Scheduled | 声明式定时任务注解 | `@Scheduled(fixedDelay=...)` |
| ScheduledTaskRegistrar | 任务注册中枢（静态注解与动态任务汇合处） | `ScheduledTaskRegistrar` |
| Trigger | 触发策略：决定下一次执行时间 | `CronTrigger`/`PeriodicTrigger` |
| @Async | 声明式异步执行 | `@Async`/`@EnableAsync` |
| 虚拟线程 | JVM 轻量线程（Boot 4 默认调度线程模型） | `Executors.newVirtualThreadPerTaskExecutor()` |
| ShedLock | 分布式调度锁（防多实例重复执行） | `@SchedulerLock` |
| cron | 时间表达式（秒级精度） | `CronTrigger` |

## 5. 与周边知识的关系

```text
                    ┌─────────────────────────────┐
                    │  Spring 全家桶（06-Spring全家桶） │
                    │  AOP / 事务 / 事件驱动          │
                    └──────────────┬──────────────┘
                                   │ @Async 依赖 AOP 代理；@Transactional 在任务中协同
                    ┌──────────────▼──────────────┐
                    │      Spring Task（本体系）     │
                    │  调度 + 异步 + 线程池 + 分布式   │
                    └──────┬───────────────┬───────┘
                           │               │
          ┌────────────────▼───┐   ┌───────▼────────────────┐
          │ 分布式任务调度框架    │   │ 分布式锁 / 配置中心      │
          │ （Quartz / XXL-Job） │   │ （Redis/ShedLock 等）   │
          └────────────────────┘   └────────────────────────┘
```

- **向上承接**：[Spring全家桶-19-分布式定时任务](../Spring全家桶/19-分布式定时任务.md) 有框架级对比；本体系做 Spring Task 自身机制的深度化。
- **横向联动**：分布式锁（[Spring Data Redis](../Spring Data Redis/00-Spring Data Redis知识体系总览.md) 04 篇 Lua 实现）是 ShedLock 的底层；[Spring框架核心-事件驱动](../Spring框架核心/07-事件驱动机制.md) 与 @Async 共用线程池概念。
- **向下延伸**：Quartz 的 JobStore、XXL-Job 的调度中心架构，均以本体系的"调度语义 + 分布式一致性"为理解地基。

## 6. 学习路线推荐

**路线 A（初级 · 快速上手 2 天）**
01 调度架构 → 02 @Scheduled 全解 → 04 @Async 基础。目标：能写出正确的定时任务与异步方法，避开单线程阻塞坑。

**路线 B（中级 · 生产工程师 1 周）**
路线 A + 03 动态任务 → 05 线程池调优 → 07 异常与可观测。目标：能实现配置中心动态 cron、线程池容量设计、任务告警。

**路线 C（高级 · 架构与面试冲刺）**
全套 01-09，重点 06（虚拟线程）、08（分布式一致性）、09（面试题）。目标：能设计多实例任务架构、讲透 Boot 4 调度变革、回答 2026 年面试题。

## 7. 快速自测 10 题

1. 默认调度器有几个线程？为什么两个 @Scheduled 任务会互相阻塞？
2. fixedDelay 与 fixedRate 的区别？哪个在任务超长时会"叠加执行"？
3. 为什么 @Scheduled 的 cron 改了配置要重启才生效？动态方案是什么？
4. @Async 生效的三个条件是什么？同类自调用为什么不生效？
5. 异步方法抛异常会怎样？怎么拿到异步结果或做失败回调？
6. spring.task.scheduling.pool.size 在 Boot 4 + 虚拟线程下还有用吗？
7. 虚拟线程的 pinning 是什么？哪些代码会触发？
8. 定时任务抛异常后，下次还会执行吗？任务"静默死亡"怎么发现？
9. 两台实例部署同一个 @Scheduled 任务，会执行几次？ShedLock 怎么解决？
10. 优雅关闭时，正在执行的定时任务怎么保证不被掐断？

> 💡 答不上的题，对应的模块序号就是你的学习优先级；答案全在本体系文档里。

---

**下一模块**：[01-任务调度架构：TaskScheduler与ScheduledTaskRegistrar](01-任务调度架构：TaskScheduler与ScheduledTaskRegistrar.md)　**返回总览**：本页
