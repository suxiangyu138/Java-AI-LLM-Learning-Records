# 00 Spring Cloud CircuitBreaker 知识体系总览

> 组件卡片：Spring Cloud CircuitBreaker 是什么、版本现状、能做什么、与对照体系如何衔接——**熔断降级的官方抽象层 + 三套实现（Resilience4J / Spring Retry / Framework Retry）**，专治"下游雪崩时把故障传染给上游"；5.x 主线。对照体系见 [Sentinel：流量控制熔断降级](../Spring Cloud Alibaba（阿里实现，属于 Spring Cloud 生态实现）/Sentinel：流量控制熔断降级/00-Sentinel知识体系总览.md) 与 [后端分布式常用名词通俗解释（熔断降级雪崩篇）](../../../../../09-Web开发全流程/后端 分布式常用名词通俗解释/)

---

## 📚 目录

1. [组件一句话定位](#1-组件一句话定位)
2. [版本现状（2026-08）](#2-版本现状2026-08)
3. [能力地图](#3-能力地图)
4. [与对照体系的映射](#4-与对照体系的映射)
5. [快速上手 3 步](#5-快速上手-3-步)
6. [速查导航](#6-速查导航)
7. [学习路线推荐](#7-学习路线推荐)
8. [核心概念速查](#8-核心概念速查)

---

## 1. 组件一句话定位

**Spring Cloud CircuitBreaker 是 Spring Cloud 官方的熔断降级抽象层（SPI）**——上层定义统一的 `CircuitBreakerFactory` 编程 API，下层可插拔三套实现（**Resilience4J 默认主流 / Spring Retry 维护模式 / Framework Retry 5.0 新增**）；解决"服务调用下游失败时，如何快速失败 + 优雅降级，防止故障级联放大成雪崩"。

```text
核心心智模型：
    你的业务代码（调用下游 HTTP / RPC）
        │  cbFactory.create("orderService").run(调用, 降级函数)
        ▼
    Spring Cloud CircuitBreaker 抽象层（spring-cloud-commons，SPI）
        ├── Resilience4J 实现   ← 默认主流：熔断+超时+隔离+限流+重试全家桶
        ├── Spring Retry 实现   ← 维护模式（等 Spring Retry 停产后移除）
        └── Framework Retry 实现 ← 5.0 新增：基于 Spring Framework 7 原生 retry
        ▼
    下游（order-service）：状态机 CLOSED → OPEN → HALF_OPEN
        故障率超阈值 → OPEN（快速失败，直接走 fallback，不再打下游）
        恢复期放行试探 → HALF_OPEN → 成功关闸 / 失败重新开闸
```

| 维度 | 说明 |
|------|------|
| 所属 | Spring Cloud 官方组件（抽象层在 spring-cloud-commons，实现在 spring-cloud-circuitbreaker） |
| 版本线 | **5.x 主线（5.0.x 为当前主流，2025.1/Oakwood，Boot 4 + Framework 7）**；3.3.x 供 Boot 3.5 |
| 默认实现 | **Resilience4J（2.3.0）**：熔断/超时/隔离/限流/重试全套 |
| 编程方式 | 编程式 `CircuitBreakerFactory.run()` + 实现方注解（Resilience4J 的 @CircuitBreaker 等） |
| 定位 | 只做"调用保护"：快速失败 + 降级 + 熔断（不做注册发现、不做网关） |
| 适用生态 | **官方 Spring Cloud 体系**；SCA（Nacos/Sentinel）生态用 Sentinel 实现 |

### 1.1 CircuitBreaker 解决什么问题

**① 下游故障时的雪崩传染**：order-service 调 payment-service，payment 变慢/挂掉 → 所有线程阻塞在等待响应 → order-service 线程池耗尽 → 上游 user-service 也堆满 → **雪崩**。熔断器在下游故障率超阈值后**快速失败**（不等待超时），把故障隔离在下游（[03 篇](03-熔断原理：状态机与滑动窗口.md)）。

**② 优雅降级**：熔断/超时后不是报 500，而是返回 fallback（缓存数据、兜底提示、默认值）——**用户体验保底**（[05 篇](05-注解开发与降级速查.md)）。

| 维度 | 没有熔断器 | 有熔断器 |
|------|-----------|---------|
| 下游 100% 失败 | 每次请求等满超时（如 30s×N 并发） | **故障率达标立刻快速失败（毫秒级）** |
| 线程资源 | 全部阻塞等下游，线程池耗尽 | 熔断期间零下游调用，线程释放 |
| 恢复 | 下游好了也不知道，继续扛 | **半开试探**：成功一次就恢复流量 |
| 用户体验 | 超时/5xx 一片 | fallback 兜底（缓存/默认值） |

> 🎯 判断标准一句话：**"应用里有没有调用远程服务的入口？"**——有，就该配熔断器；RPC 时代（Feign/WebClient/RestTemplate 调微服务）这是标配，不是可选项（[08 篇](08-生产实践与选型避坑.md) 接入清单）。

### 1.2 适用与不适用场景

| 场景 | 适合？ | 说明 |
|------|:---:|------|
| 微服务间 HTTP/RPC 调用保护 | ✅ | 主战场（Feign/Gateway/WebClient/RestTemplate） |
| 下游慢调用保护（超时） | ✅ | TimeLimiter 超时 + 熔断联动 |
| 数据库/缓存故障兜底 | ✅ | 降级到缓存/默认值 |
| 高并发下游瞬时打爆 | ⚠️ | 熔断是"事后保护"，限流是"事前控制"（配 Sentinel 或 Resilience4J RateLimiter） |
| 服务注册发现 | ❌ | 那是 Nacos/Eureka 的职责 |
| 网关路由 | ❌ | Gateway 有自己的 CircuitBreaker 过滤器（基于本组件） |
| 替代消息队列削峰 | ❌ | 削峰是 MQ/限流的领域 |

> ⚠️ **最大认知误区**：把熔断器当"限流器"用——**熔断是保护下游（下游挂了别打死它、也别让自己等死），限流是保护自己（流量太大拒掉一部分）**；两者互补不互替（[08 篇](08-生产实践与选型避坑.md) 选型表）。

### 1.3 与熔断降级方案对照

| 方案 | 实现 | 特性 | 生态 |
|------|------|------|------|
| **SCCB + Resilience4J** | 状态机+滑动窗口+信号量/线程池隔离 | 全能力（熔断/超时/隔离/限流/重试），可插拔 | **官方 Cloud 生态** |
| **SCCB + Framework Retry** | Framework 7 原生 retry 状态机 | 轻量零依赖，仅阻塞式，5.0 新增 | 官方 Cloud 生态（Boot 4） |
| **SCA Sentinel** | 滑动窗口+令牌桶+熔断 | 控制台可视化、规则持久化、限流更强 | **SCA 生态（阿里）** |
| Hystrix（已废弃） | 线程池/信号量隔离 | 2018 停止维护，SCCB 前身 | 遗留系统 |
| Spring Retry（维护模式） | RetryTemplate 状态机 | 只有重试+熔断，无隔离限流 | 官方 Cloud 生态 |

> 🎯 面试必答：**"Hystrix 为什么被弃用，替代方案是什么？"**——Hystrix 2018 年停止维护，Spring Cloud 官方用 **SCCB 抽象层替代**（Netflix 全家桶集体退出），主流实现为 Resilience4J：**更轻量（单 JAR、无线程池隔离默认信号量）、可函数式组合（装饰器模式）、对响应式原生支持**；国内团队则常迁移到 SCA 生态的 **Sentinel**（控制台+规则持久化更完善）（[08 篇](08-生产实践与选型避坑.md) vs Sentinel 对比）。

## 2. 版本现状（2026-08）

| 版本线 | 状态 | 关键点 |
|--------|------|--------|
| **5.x（5.0.x 主流）** | **当前主线** | Spring Cloud **2025.1.x（Oakwood）**、**Boot 4.0/4.1**、Framework 7、Resilience4J 2.3.0、**新增 Framework Retry 实现**、**跳过 4.0.x 直接对齐 5.0** |
| 3.3.x | 存量（EOL） | Spring Cloud 2025.0.x（Northfields）、Boot 3.5；**OSS 支持已于 2026-06-30 结束** |
| 3.2.x | 存量（EOL） | Spring Cloud 2024.0.x（Moorgate）、Boot 3.4 |
| 3.1.x | 存量 | Spring Cloud 2023.0.x（Leyton）、Boot 3.2/3.3 |
| 3.0.x | 老存量 | Spring Cloud 2022.0.x（Kilburn）、Boot 3.0/3.1 |
| 2.x | 老存量 | Spring Cloud 2020.0/2021.0、Boot 2.4-2.7 |
| 1.x | 废弃 | Hoxton 时代（Hystrix 兼容） |

> ⚠️ **版本策略（2026 起）**：**新项目 Boot 4.x + Cloud 2025.1.x + SCCB 5.0.x**；存量 Boot 3.5 用 3.3.x（**OSS 已 EOL，建议尽快升级**）；**SCCB 大版本跟着 Cloud release train 走，不单独钉版本**（[01 篇](01-模块清单与版本矩阵.md) 矩阵）。

### 2.1 5.0 关键变化（3.3.x → 5.0.x 迁移要点）

| 变化 | 说明 |
|------|------|
| 版本号跳跃 | **跳过 4.0.x 直接对齐 5.0.x**（2025.1 全组件统一 5.0） |
| **新增 Framework Retry 实现** | `spring-cloud-starter-circuitbreaker-framework-retry`，基于 **Spring Framework 7 原生 resilience**（RetryTemplate/@Retryable/@ConcurrencyLimit），仅阻塞式（[07 篇](07-Framework Retry与Spring Retry对比.md)） |
| Spring Retry 模块维护模式 | 不再加新特性，**Spring Retry 停产后移除**；已有代码不受影响 |
| Resilience4J 升级 | 2.2.x → **2.3.0**（虚拟线程 pinning 修复、无锁滑动窗口、自定义 Clock；独立库已出 2.4.0，支持 Boot 4/Cloud 5） |
| Boot 4 底座 | jakarta 全系、Spring Framework 7、AOT/CRaC 对齐 |
| Hystrix 彻底移除 | 1.x 时代遗留，5.x 无任何 Hystrix 代码 |

### 2.2 关键演进时间线

| 版本 | 里程碑 |
|------|--------|
| 1.x（2019-2020） | 从 Hystrix 抽象出来的 SPI 诞生（CircuitBreakerFactory + Hystrix 实现） |
| 2.0（2020.12） | Resilience4J 转正为默认实现，Hystrix 降级可选 |
| 3.0（2022.12） | 对齐 Boot 3/Jakarta；新增 Spring Retry 实现 |
| 3.2/3.3（2024-2025） | Moorgate/Northfields 维护线（Boot 3.4/3.5） |
| 5.0（2025.11） | **Oakwood 大版本**：跳过 4.x、Framework Retry 实现、Resilience4J 2.3.0、Spring Retry 维护模式 |

## 3. 能力地图

| 能力域 | 能力点 | 对应机制 |
|--------|--------|---------|
| 熔断 | 三态状态机（CLOSED/OPEN/HALF_OPEN） | Resilience4J CircuitBreaker |
| 熔断 | 滑动窗口统计（次数/时间） | COUNT_BASED 环形缓冲 / TIME_BASED 分桶 |
| 快速失败 | 熔断期直接走 fallback 不打下游 | OPEN 状态 tryAcquirePermission |
| 降级 | run() fallback 函数 / fallbackMethod 注解 | CircuitBreakerFactory API / Resilience4J 注解 |
| 超时 | 调用超时上限 | TimeLimiter（cancelRunningFuture 可中断） |
| 隔离 | 并发限制，防线程池耗尽 | Bulkhead（Semaphore 默认 / ThreadPool） |
| 限流 | 单位时间请求数上限 | RateLimiter（令牌桶） |
| 重试 | 失败自动重试 | Retry（最大次数/间隔/退避） |
| 响应式 | WebFlux/WebClient 全链路保护 | reactor-resilience4j starter（Mono/Flux） |
| 集成 | Feign 调用自动包熔断 | spring-cloud-openfeign `feign.circuitbreaker.enabled=true` |
| 集成 | 网关路由级熔断 | CircuitBreakerGatewayFilterFactory（Gateway 组件） |
| 可观测 | 状态事件/指标/健康检查 | EventPublisher + resilience4j-micrometer + actuator health |
| 配置 | 实例/分组/默认三级配置 | resilience4j.* yaml 属性 + Customizer Bean |

### 3.1 能力边界：CircuitBreaker 不做什么

| 能力边界 | 谁来做 | 说明 |
|---------|--------|------|
| 注册发现 | Nacos / Eureka / Consul | 熔断器只保护"调用"，不感知服务地址 |
| 配置中心 | Nacos / Spring Cloud Config | 熔断规则不动态下发（改配置需重启，除非接配置中心） |
| 网关路由 | Spring Cloud Gateway | Gateway 是"入口保护"，熔断器是"出口保护" |
| 全局限流/流量治理 | Sentinel（SCA）/ Gateway | SCCB 只有单机能力，无控制台/集群流控 |
| 消息可靠投递 | MQ | 与熔断无关 |

> ⚠️ **常见归因错误**：下游恢复但熔断器还开着，业务说"熔断器坏了"——**排查路径："健康检查/日志看状态机 → waitDurationInOpenState 是否合理 → 半开试探是否成功 → 是否手动重置"**，大部分"异常"是参数语义没理解（[08 篇](08-生产实践与选型避坑.md) 坑位表）。

## 4. 与对照体系的映射

| 速查文档 | 对照/深度体系 |
|---------|-------------|
| 03-熔断原理 | [Sentinel：熔断降级](../Spring Cloud Alibaba（阿里实现，属于 Spring Cloud 生态实现）/Sentinel：流量控制熔断降级/04-熔断降级速查.md)（滑动窗口实现对比）、[后端分布式常用名词通俗解释](../../../../../09-Web开发全流程/后端 分布式常用名词通俗解释/)（雪崩/熔断/降级白话） |
| 04-弹性能力 | [JUC 高并发编程](../../../../../01-底层根基-Java核心底座/02-JUC高并发编程/)（线程池/信号量原理）、[虚拟线程](../../../../../01-底层根基-Java核心底座/02-JUC高并发编程/虚拟线程/)（2.3.0 pinning 修复背景） |
| 05-注解与集成 | [Spring Cloud OpenFeign](../Spring Cloud OpenFeign/)（feign.circuitbreaker 集成）、[Spring Cloud Gateway](../Spring Cloud Gateway/)（熔断过滤器） |
| 08-选型 | [Sentinel 总览](../Spring Cloud Alibaba（阿里实现，属于 Spring Cloud 生态实现）/Sentinel：流量控制熔断降级/00-Sentinel知识体系总览.md)（vs Sentinel 全维度） |
| 08-可观测 | [Spring Cloud Bus 系列](../Spring Cloud Bus/00-Spring Cloud Bus知识体系总览.md)（同为官方组件抽象层写法，可对照） |

> 💡 分工约定：**本系列回答"熔断器怎么配、状态机怎么转、fallback 怎么写、和 Sentinel 怎么选"**；通用分布式概念（雪崩/降级/幂等）在 [后端分布式常用名词通俗解释](../../../../../09-Web开发全流程/后端 分布式常用名词通俗解释/) 对照理解。

## 5. 快速上手 3 步

**① 加依赖**（以默认实现 Resilience4J 为例）：

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-circuitbreaker-resilience4j</artifactId>
    <!-- 版本由 spring-cloud-dependencies BOM（2025.1.x）统一管理，不写 version -->
</dependency>
```

**② 编程式包裹调用**（注入 `CircuitBreakerFactory`）：

```java
@RestController
public class OrderController {
    private final CircuitBreakerFactory cbFactory;      // spring-cloud-commons 自动装配
    private final RestTemplate restTemplate;

    public OrderController(CircuitBreakerFactory cbFactory, RestTemplate restTemplate) {
        this.cbFactory = cbFactory;
        this.restTemplate = restTemplate;
    }

    @GetMapping("/order/{id}")
    public String getOrder(@PathVariable Long id) {
        return cbFactory.create("orderService")          // ★ 熔断器实例名（yaml 按它配规则）
                .run(                                    // 受保护调用
                    () -> restTemplate.getForObject("http://order-service/orders/" + id, String.class),
                    throwable -> "降级：订单服务暂不可用");   // ★ 熔断/异常时返回的兜底
    }
}
```

**③ 配熔断阈值**（application.yml）：

```yaml
resilience4j:
  circuitbreaker:
    instances:
      orderService:
        sliding-window-size: 50          # 统计最近 50 次调用
        failure-rate-threshold: 50       # 失败率 ≥ 50% 熔断
        minimum-number-of-calls: 10      # 至少统计 10 次才计算
        wait-duration-in-open-state: 30s # 熔断后 30s 进入半开试探
  timelimiter:
    instances:
      orderService:
        timeout-duration: 3s             # 调用超时 3s
```

> 🎯 跑通即及格：**① 正常调用返回真实数据；② 停掉下游 → 连续调用触发失败率 → 日志出现 `CircuitBreaker 'orderService' recorded a state transition from CLOSED to OPEN`；③ 熔断后立即返回降级文案（不超时）**——三件事都通，主链路打通（[02 篇](02-快速开始与编程式API速查.md)）。

### 5.1 三步验证与排障起点

| 步骤 | 做法 | 预期 |
|------|------|------|
| ① 正常链路 | 起下游，调接口 | 返回真实数据（有调用日志） |
| ② 触发熔断 | 停下游，连打 10+ 次 | 状态机 CLOSED→OPEN，日志可见 state transition |
| ③ 熔断生效 | OPEN 期间再调 | **立即**返回 fallback（毫秒级，不等超时） |

> 💡 排障起点：**先确认熔断器"名字"和 yaml 里的实例名一致（create("orderService") ↔ instances.orderService）**——名字对不上 = 全走默认配置，这是 80% "熔断不生效"的原因（[08 篇](08-生产实践与选型避坑.md) 排错表）。

## 6. 速查导航

| 文档 | 内容 |
|------|------|
| [01-模块清单与版本矩阵](01-模块清单与版本矩阵.md) | artifact 坐标（4 套 starter）、包结构、**版本矩阵（SCCB↔Cloud↔Boot↔Resilience4J）**、跳过 4.0.x 说明 |
| [02-快速开始与编程式API速查](02-快速开始与编程式API速查.md) | 依赖/配置、CircuitBreakerFactory 编程式 API、run/fallback、响应式 API、Customizer |
| [03-熔断原理：状态机与滑动窗口](03-熔断原理：状态机与滑动窗口.md) | 三态转换图、COUNT/TIME 滑动窗口、参数语义、事件模型、vs Hystrix |
| [04-弹性能力全景：隔离超时重试限流](04-弹性能力全景：隔离超时重试限流.md) | Bulkhead/TimeLimiter/RateLimiter/Retry、组合顺序、线程池隔离对比 |
| [05-注解开发与降级速查](05-注解开发与降级速查.md) | Resilience4J 五注解、fallbackMethod 规则、Feign/Gateway/HttpService 集成 |
| [06-配置属性速查](06-配置属性速查.md) | resilience4j.* 全属性表、三级优先级、metrics/health |
| [07-Framework Retry与Spring Retry对比](07-Framework Retry与Spring Retry对比.md) | 5.0 新实现、Framework 7 原生 resilience、三实现选型 |
| [08-生产实践与选型避坑](08-生产实践与选型避坑.md) | 参数调优基线、监控告警、坑位表、vs Sentinel、升级路径 |

### 6.1 阅读顺序建议

- **第一次接触**：02（编程式 API）→ 03（原理）→ 跑通熔断；
- **项目实战**：02 → 06（属性）→ 08 调优与监控；
- **面试冲刺**：03 原理（状态机/滑动窗口）→ 05 fallback 规则 → 08 vs Sentinel；
- **Boot 4 新特性**：07 Framework Retry（5.0 新增）。

### 6.2 前置知识建议

| 前置主题 | 所在文档 | 与本系列的关系 |
|---------|---------|--------------|
| 雪崩/熔断/降级概念 | [后端分布式常用名词通俗解释](../../../../../09-Web开发全流程/后端 分布式常用名词通俗解释/) | 白话理解本组件的目标 |
| 信号量/线程池 | [JUC 高并发编程](../../../../../01-底层根基-Java核心底座/02-JUC高并发编程/) | Bulkhead 隔离的实现基础 |
| Feign 调用 | [Spring Cloud OpenFeign](../Spring Cloud OpenFeign/) | 最常见的包裹对象 |
| Gateway 路由 | [Spring Cloud Gateway](../Spring Cloud Gateway/) | 网关级熔断过滤器 |

## 7. 学习路线推荐

| 路线 | 适用 | 顺序 |
|------|------|------|
| 入门速查 | 会用 Feign/WebClient | 00 总览 → 02 编程式 API → 跑通熔断 |
| 项目实践 | 上生产 | 02 → 06 属性 → 08 调优/监控/坑位 |
| 面试冲刺 | 全考点 | 03 原理 → 05 fallback → 08 vs Sentinel |
| 架构选型 | 负责人 | 07 三实现对比 → 08 vs Sentinel 选型 → 03 原理 |

## 8. 核心概念速查

| 术语 | 一句话解释 |
|------|-----------|
| 熔断（Circuit Breaker） | 下游故障率超阈值后快速失败，防止雪崩 |
| CLOSED / OPEN / HALF_OPEN | 三态：正常放行 / 熔断快速失败 / 半开试探恢复 |
| 滑动窗口 | 统计窗口：COUNT_BASED 次数环 / TIME_BASED 时间桶 |
| failure-rate-threshold | 熔断触发失败率阈值（默认 50%） |
| minimum-number-of-calls | 触发计算最少调用数（默认 100） |
| wait-duration-in-open-state | OPEN 停留时长，之后转 HALF_OPEN（默认 60s） |
| fallback | 降级函数：熔断/异常时返回的兜底结果 |
| CircuitBreakerFactory | spring-cloud-commons 的统一工厂 API（create → run） |
| TimeLimiter | 超时限制（默认 1s，可中断被挂起线程） |
| Bulkhead | 舱壁隔离：信号量（默认）/ 线程池，防线程耗尽 |
| RateLimiter | 令牌桶限流 |
| Resilience4J | 默认实现：Java 容错库（熔断/超时/隔离/限流/重试） |
| Framework Retry | 5.0 新增实现：基于 Spring Framework 7 原生 retry |
| Sentinel | SCA 生态熔断限流组件（本系列的对照系） |

---

**下一模块**：[01-模块清单与版本矩阵](01-模块清单与版本矩阵.md)　**返回总览**：本页

**【参考来源】**：[Spring Cloud CircuitBreaker 5.0.2 官方参考文档](https://docs.spring.io/spring-cloud-circuitbreaker/reference/)、[Spring Cloud 2025.1.0（Oakwood）发布公告](https://spring.io/blog/2025/11/25/spring-cloud-2025-1-0-aka-oakwood-has-been-released/)、[Spring Cloud release train EOL（endoflife.date）](https://endoflife.date/spring-cloud)、[spring-cloud-starter-circuitbreaker-resilience4j 5.0.2 POM（Maven Central）](https://repo1.maven.org/maven2/org/springframework/cloud/spring-cloud-starter-circuitbreaker-resilience4j/5.0.2/spring-cloud-starter-circuitbreaker-resilience4j-5.0.2.pom)
