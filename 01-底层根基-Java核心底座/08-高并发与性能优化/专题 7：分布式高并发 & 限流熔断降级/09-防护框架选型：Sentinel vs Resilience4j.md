# 09 防护框架选型：Sentinel vs Resilience4j

> 框架选型决定防护体系的上限——2026 年三大候选是 Hystrix（已停更）、Sentinel 1.8.x、Resilience4j 2.4.x；本讲给出对比矩阵与决策树（2026-08 基准）

---

## 📚 目录

1. [2026 生态现状：三足变双雄](#1-2026-生态现状三足变双雄)
2. [Sentinel：阿里系流控熔断一体](#2-sentinel阿里系流控熔断一体)
3. [Resilience4j 2.4.0：函数式六模块](#3-resilience4j-240函数式六模块)
4. [对比矩阵](#4-对比矩阵)
5. [选型决策树与落地](#5-选型决策树与落地)

---

## 1. 2026 生态现状：三足变双雄

2018 年 Netflix 宣布 Hystrix 进入维护模式后，业界经历了"停更替代"的完整周期，2026 年的格局是**双雄并存**：

- **Hystrix**：2018 年起不再更新，仅存量系统使用，新项目无理由选择；其线程池隔离思想已沉淀为业界共识（[08 篇](08-隔离与舱壁：线程池与信号量.md)），由后续框架继承。
- **Sentinel**：阿里开源，1.8.x 稳定线（Dashboard 1.8.6 为广泛使用版本），阿里双 11 生产验证，2026 年在 SCA（Spring Cloud Alibaba）体系内持续集成；2.0 流量治理方向演进中。
- **Resilience4j**：2.4.0（2026 年发布）支持 JDK 21 虚拟线程、Spring Boot 4 / Spring Cloud 5，是 Spring Cloud CircuitBreaker 官方实现，函数式轻量路线。

选型的第一信息源是**官方 release 页**（见[参考来源](#6-参考来源)），版本与发布节奏会变化，本文基准 2026-08。选型还有一个隐性成本维度：**团队熟悉度与存量资产**——已有的规则、Dashboard、监控看板迁移一次的成本往往高于框架本身的差异，除非有硬性诉求（虚拟线程、非 SCA 栈），否则"团队已有的那一套"通常是正确答案。

## 2. Sentinel：阿里系流控熔断一体

Sentinel 的定位是"**流量控制 + 熔断降级 + 系统保护**三合一"，与 SCA 生态深度绑定。核心能力：

- **规则动态化**：限流/熔断/热点规则通过 Dashboard 或 Nacos 实时下发，不重启生效；规则持久化到 Nacos（flow-rules、gw-flow 等）是大促调参的标配（[07 篇](07-降级与兜底预案：分级设计.md) 的动态开关同源）。
- **丰富的流控维度**：QPS/并发线程数限流、热点参数限流、系统自适应保护（CPU 使用率、系统负载、RT 触发自动限流——2026 年自适应限流的工业实现）。WarmUp 预热、匀速排队两种流控效果内建。
- **生态集成**：@SentinelResource 注解、网关（Spring Cloud Gateway/Nginx）集成、Dubbo 集成、集群流控（Token Server 模式做真正的分布式限流）。

代价是**与 SCA 绑定较深**：独立 Spring Boot 项目接入需要自建规则持久化与告警链路，控制台需自部署。

## 3. Resilience4j 2.4.0：函数式六模块

Resilience4j 的定位是"**轻量、函数式、模块化**"，把容错能力拆成六个可独立组合的模块：**circuitbreaker（熔断）、ratelimiter（限流）、bulkhead（舱壁/隔离）、retry（重试）、timelimiter（超时）、cache（缓存防击穿）**。用法是装饰器链：`CircuitBreaker.decorateFunction(fn)` 或注解 `@CircuitBreaker`，按序组合成 `retry → circuitbreaker → timelimiter → bulkhead` 的完整保护链。

2.4.0 的新能力直接回应 2026 年的技术趋势：**虚拟线程支持**（JDK 基线升到 21）、**Spring Boot 4 / Spring Cloud 5 适配**、熔断初始状态配置（可初始化到 OPEN）、HALF_OPEN 直接转 CLOSED 配置、`withFallback()` 增强；2.3.0 起内部统计无锁化（避免虚拟线程 pinning）。限流模块基于令牌桶（RateLimiter），隔离模块提供线程池与信号量两种 Bulkhead。

短板：**无控制台、无动态规则中心**——阈值改配置要发布，规则动态化需自建（配合 Nacos 动态配置可实现）。

使用形态上两者都是"注解优先、编程式兜底"：Resilience4j 的 `@CircuitBreaker(name="pay", fallbackMethod="payFallback")` 与 Sentinel 的 `@SentinelResource(value="pay", fallback="payFallback", blockHandler="payBlock")` 结构几乎同构——区别在语义：fallback 处理业务异常/熔断，blockHandler 处理限流拒绝，Sentinel 把两种兜底显式分开（[07 篇](07-降级与兜底预案：分级设计.md) 有展开）。编程式（装饰器链）则用于动态路由、批量配置等注解表达不了的场景。团队内有 Java 8 存量系统时注意版本约束：Resilience4j 2.x 与 Sentinel 1.8.x 均要求 JDK 8+，但 R4j 2.4.0 的虚拟线程能力需要 JDK 21——迁移时要先确认运行环境。

## 4. 对比矩阵

| 维度 | Hystrix | Sentinel 1.8.x | Resilience4j 2.4.x |
|---|---|---|---|
| 维护状态 | 停更（2018） | 活跃（1.8.x 稳定） | 活跃（2026 持续发布） |
| 能力覆盖 | 熔断+线程池隔离 | 限流+熔断+降级+系统保护 | 熔断+限流+隔离+重试+超时+缓存 |
| 规则动态化 | 无 | 强（Dashboard/Nacos 下发） | 弱（需自建） |
| 控制台 | 无 | 有（Dashboard） | 无 |
| 线程模型 | 线程池隔离为主 | 信号量为主 | 线程池+信号量双支持 |
| 虚拟线程适配 | 无 | 一般 | 好（2.3+ 无锁统计） |
| 生态 | 已退场 | SCA/阿里系 | Spring Cloud CircuitBreaker/独立 |
| 分布式限流 | 无 | 集群流控（Token Server） | 无（需自接 Redis） |

## 5. 选型决策树与落地

选型按团队技术栈走决策树：

1. **团队用 SCA（Spring Cloud Alibaba）** → 直接选 Sentinel：规则下发、控制台、集群流控零成本获得，阿里大促同款实践（[反向海淘案例](10-生产实战与自测.md)）。
2. **标准 Spring Cloud（非 Alibaba）或独立 Boot 项目** → 选 Resilience4j：Spring Cloud CircuitBreaker 5.0（2025.1 Oakwood）把 Resilience4j 抽象为统一 API，虚拟线程/Boot 4 适配领先；规则动态化用 Nacos 动态配置自行实现。
3. **存量 Hystrix** → 迁移到 Resilience4j（API 迁移成本低，装饰器模型更简洁），或 SCA 存量直接迁 Sentinel。
4. **需要真分布式限流（集群总量控制）** → 只有 Sentinel 集群流控或自建 Redis Lua（[05 篇](05-分布式限流：Redis与多级限流.md)）；Resilience4j 需自行接 Redis 实现。

落地顺序建议：先接熔断（最安全、收益最大）→ 再加限流 → 补隔离 → 最后上规则动态化。存量 Hystrix 迁移到 Resilience4j 的成本很低：HystrixCommand 的 run/fallback 对应 R4j 的装饰器 + 降级函数，命令键对应实例名，线程池配置对应 bulkhead 配置——语义一一对应，核心是**阈值参数要按新框架语义重推**（如 Hystrix 的 requestVolumeThreshold 对应 minimumNumberOfCalls），不能照搬数字。

框架落地后还有两件配套：**配置管理**——Resilience4j 的阈值配合 Spring Cloud Config/Nacos 动态配置实现不重启调整（Sentinel 则用 Dashboard/规则持久化）；**监控告警**——熔断状态迁移事件（OPEN/HALF_OPEN）、限流命中率、降级次数必须接入 Prometheus/Grafana（Resilience4j 原生导出 Metrics，Sentinel 有 Dashboard 与 Micrometer 适配），并配置"熔断打开即告警"——防护组件本身的工作状态不可见，等于没有防护。框架只是载体，[10 篇](10-生产实战与自测.md) 的配置基线、监控闭环与演练才是防护体系真正落地的地方。

> 🎯 **核心要点**：2026 年防护框架双雄并存——Sentinel 1.8.x（规则动态化+控制台+集群流控+系统保护，阿里系）与 Resilience4j 2.4.0（函数式六模块+虚拟线程+Spring Cloud CircuitBreaker 5.0 官方实现），Hystrix 已停更仅存量；SCA 团队选 Sentinel，标准 Spring Cloud 选 Resilience4j，真分布式限流只有 Sentinel 集群流控或自建 Redis Lua；框架是载体，配置基线、监控与演练才是落地关键。

---

## 6. 参考来源

1. [Resilience4j v2.4.0 Release（newreleases.io）](https://newreleases.io/project/github/resilience4j/resilience4j/release/v2.4.0) — 2.4.0 版本线与新能力
2. [Sentinel Releases（GitHub）](https://github.com/alibaba/sentinel/releases) — Sentinel 版本线
3. [Circuit Breaker | Resilience4j（DeepWiki）](https://deepwiki.com/resilience4j/resilience4j/2-circuit-breaker) — 熔断器功能与状态机
4. [Circuit Breaker Pattern with Spring Cloud and Resilience4j（TheCodeForge）](https://thecodeforge.io/java/spring-cloud-circuit-breaker/) — Spring Cloud 集成实践

---

**下一模块**：[10 生产实战与自测](10-生产实战与自测.md)

**返回总览**：[00-总览](00-总览.md)
