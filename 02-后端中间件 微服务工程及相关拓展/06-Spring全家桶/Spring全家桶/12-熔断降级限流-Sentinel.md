# 熔断降级限流 — Sentinel 深度实战
> 阿里巴巴开源的流量控制、熔断降级、系统负载保护中间件，微服务高可用体系的最后一环

---

## 目录

1. [本章总览](#1-本章总览)
2. [分层理论讲解](#2-分层理论讲解)
3. [高频踩坑与误区](#3-高频踩坑与误区)
4. [随堂基础练习](#4-随堂基础练习)
5. [章节综合实操案例](#5-章节综合实操案例)
6. [分层综合习题](#6-分层综合习题)
7. [本章复盘速记清单](#7-本章复盘速记清单)
8. [精通拓展补充-P2](#8-精通拓展补充-p2)

---

## 1. 本章总览

### 1.1 为什么需要 Sentinel

在微服务架构中，服务之间通过 RPC 或 HTTP 进行调用，当某个下游服务出现延迟或异常时，可能引发 **雪崩效应**（Cascading Failure）。传统的解决方案包括：

| 方案 | 原理 | 局限 |
|------|------|------|
| 熔断（Circuit Breaker） | 错误率达到阈值后断开链路 | 需要配合降级策略 |
| 降级（Degradation） | 提供 fallback 兜底逻辑 | 降级策略需要精细设计 |
| 限流（Rate Limiting） | 控制请求速率 | 单机/分布式策略不同 |
| 隔离（Bulkhead） | 线程池/信号量隔离 | 资源开销较大 |

**Sentinel** 将上述能力整合为一套统一的解决方案，提供 **流量控制**、**熔断降级**、**系统自适应保护** 三大核心能力。

### 1.2 Sentinel 核心优势

| 特性 | Sentinel | Hystrix | Resilience4j |
|------|----------|---------|--------------|
| 流量控制 | 丰富的流控模式（QPS/线程数/Warm-up/排队等待） | 基础限流 | 需自行扩展 |
| 熔断降级 | 慢调用比例、异常比例、异常数 | 基于异常比例 | 慢调用/异常/自定义 |
| 系统自适应保护 | Load/RT/CPU 自适应 | 不支持 | 不支持 |
| 热点参数限流 | 原生支持 | 不支持 | 需自行实现 |
| 实时监控 | 控制台 Dashboard | 需集成 Turbine | Prometheus |
| 规则持久化 | 多数据源（Nacos/APOLLO/ZK） | 不支持 | 需自行实现 |
| 动态规则推送 | push/pull 模式 | 不支持 | 需集成 |

> 💡 Sentinel 在限流维度的能力远超 Hystrix 和 Resilience4j，是 Spring Cloud Alibaba 生态的首选稳定性组件。

### 1.3 本章学习路线

```
入门 → 核心概念（资源/规则/降级）
       ↓
初阶 → 流控规则 + 熔断规则 + 控制台搭建
       ↓
中阶 → @SentinelResource + 热点限流 + 系统保护
       ↓
高阶 → 规则持久化 + 自定义异常处理 + Nacos 集成
       ↓
实战 → 商品服务综合案例（完整工程整合）
       ↓
拓展 → 与 Hystrix/Resilience4j 对比 + P2 进阶
```

### 1.4 版本说明

| 组件 | 版本 |
|------|------|
| Spring Boot | 2.7.x |
| Spring Cloud Alibaba | 2021.0.5.0 |
| Sentinel Core | 1.8.6 |
| Sentinel Dashboard | 1.8.6 |
| Nacos Server | 2.2.0 |
| JDK | 1.8+ |

---

## 2. 分层理论讲解

### 2.1 Sentinel 整体架构

#### 2.1.1 架构图概览

```
┌──────────────────────────────────────────────────────────┐
│                    Sentinel Dashboard                    │
│              (流量监控 / 规则管理 / 实时数据)               │
└────────────────────────┬─────────────────────────────────┘
                         │  HTTP API / 心跳上报
                         ▼
┌──────────────────────────────────────────────────────────┐
│                     Sentinel Core                         │
│  ┌──────────┐  ┌──────────┐  ┌───────────────────────┐   │
│  │  Flow    │  │ Degrade  │  │ System Protection     │   │
│  │  Rule    │  │ Rule     │  │ Adaptive Load         │   │
│  └──────────┘  └──────────┘  └───────────────────────┘   │
│  ┌──────────┐  ┌──────────┐  ┌───────────────────────┐   │
│  │ Hotspot  │  │ Authority│  │ Cluster Flow          │   │
│  │  Rule    │  │ Rule     │  │  Rule                 │   │
│  └──────────┘  └──────────┘  └───────────────────────┘   │
│                                                          │
│  ┌────────────────────────────────────────────────┐      │
│  │           Slot Chain (ProcessorSlot)           │      │
│  │  NodeSelector → ClusterBuilder → Statistc →   │      │
│  │  FlowSlot → DegradeSlot → SystemSlot → ...    │      │
│  └────────────────────────────────────────────────┘      │
└──────────────────────────────────────────────────────────┘
                         │
                         ▼
┌──────────────────────────────────────────────────────────┐
│              DataSources (规则来源)                        │
│  ┌──────────┐  ┌──────────┐  ┌───────────────────────┐   │
│  │  Nacos   │  │ Apoolo   │  │   Zookeeper / File    │   │
│  └──────────┘  └──────────┘  └───────────────────────┘   │
└──────────────────────────────────────────────────────────┘
```

#### 2.1.2 核心概念

| 概念 | 说明 |
|------|------|
| **Resource（资源）** | Sentinel 保护的最小单元，由 `SphU.entry()` 定义，可以是任意 Java 代码块 |
| **Rule（规则）** | 围绕资源定义的流量控制规则，包括流控、降级、热点、系统等规则 |
| **Slot（插槽）** | ProcessorSlotChain 中的处理节点，负责执行具体的检查逻辑 |
| **Node（节点）** | 用于统计实时数据的统计节点，分 ClusterNode、DefaultNode 等 |
| **Context（上下文）** | 调用链的上下文，包含入口、调用来源等元数据 |

#### 2.1.3 ProcessorSlotChain 责任链

Sentinel 使用责任链模式处理每一个资源调用：

```text
CtSph.entry(name)
    ↓
ProcessorSlotChain (责任链)
    ↓
    ├── NodeSelectorSlot       — 构建调用链路父子关系
    ├── ClusterBuilderSlot     — 统计集群维度数据
    ├── LogSlot                — 日志记录
    ├── StatisticSlot          — 实时数据统计（滑动窗口）
    ├── AuthoritySlot          — 黑白名单校验
    ├── SystemSlot             — 系统自适应保护
    ├── FlowSlot               — 流量控制
    └── DegradeSlot            — 熔断降级
```

> 💡 每个 Slot 都可以独立扩展，通过 SPI 机制注册，Sentinel 的扩展性极强。

### 2.2 滑动窗口统计原理

#### 2.2.1 时间窗口模型

Sentinel 使用 **滑动窗口（Sliding Window）** 进行实时统计，而非精确的秒级计数器：

```
时间线 →  0s      1s      2s      3s      4s      5s
         ┌──────┐┌──────┐┌──────┐┌──────┐┌──────┐┌──────┐
窗口 1   │  req ││  req ││  req ││      ││      ││      │
         └──────┘└──────┘└──────┘└──────┘└──────┘└──────┘
         │◄──── 窗口长度 1s ────►│
         
第 2s 后：
         ┌──────┐┌──────┐┌──────┐┌──────┐┌──────┐┌──────┐
窗口 2   │  req ││  req ││  req ││  req ││      ││      │
         └──────┘└──────┘└──────┘└──────┘└──────┘└──────┘
                  │◄──── 窗口长度 1s ────►│
```

- **样本桶（SampleBucket）**：每个桶存储 500ms 的统计数据
- **LeapArray**：窗口滑动数组，默认 2 个样本桶覆盖 1 秒
- `WindowWrap<T>` 包裹每个窗口，包含窗口开始时间和统计值

#### 2.2.2 关键源码类

| 类名 | 职责 |
|------|------|
| `LeapArray` | 滑动窗口数组，以时间戳定位窗口槽位 |
| `MetricBucket` | 存储计数指标（通过/拒绝/异常/耗时） |
| `ArrayMetric` | 包装 LeapArray 对外提供指标查询 |
| `StatisticNode` | 统计节点，聚合资源级统计数据 |

### 2.3 流量控制规则（Flow Rule）

#### 2.3.1 FlowRule 属性

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `resource` | String | — | 资源名称 |
| `count` | double | — | 限流阈值 |
| `grade` | int | `RuleConstant.FLOW_GRADE_QPS` | 限流模式（QPS/线程数） |
| `limitApp` | String | `default` | 针对来源 |
| `strategy` | int | `RuleConstant.STRATEGY_DIRECT` | 流控策略 |
| `controlBehavior` | int | `RuleConstant.CONTROL_BEHAVIOR_DEFAULT` | 流控效果 |
| `warmUpPeriodSec` | int | 10 | Warm-up 预热的秒数 |
| `maxQueueingTimeMs` | int | 500 | 排队等待的最大超时时间 |

#### 2.3.2 限流模式（Grade）

| 模式 | 常量 | 说明 |
|------|------|------|
| QPS 限流 | `FLOW_GRADE_QPS` | 每秒请求数限流，最常用 |
| 线程数限流 | `FLOW_GRADE_THREAD` | 并发线程数限流，防止资源过度占用 |

> 💡 QPS 模式适用于接口级别的流量整形，线程数模式适用于防止慢调用耗尽线程池。

#### 2.3.3 流控效果（Control Behavior）

| 效果 | 常量 | 场景 |
|------|------|------|
| **快速失败** | `CONTROL_BEHAVIOR_DEFAULT` | 默认模式，直接抛出 `FlowException` |
| **Warm-Up** | `CONTROL_BEHAVIOR_WARM_UP` | 预热启动，逐步增加阈值，适用于系统冷启动 |
| **排队等待** | `CONTROL_BEHAVIOR_RATE_LIMITER` | 匀速通过请求，削峰填谷，适用于消息处理 |
| **Warm-Up + 排队** | `CONTROL_BEHAVIOR_WARM_UP_RATE_LIMITER` | 预热+匀速排队组合模式 |

##### Warm-Up 图解

```
请求量
  ▲
  │  ┌─────────────── 阈值 1000 QPS
  │  │               /
  │  │              /
  │  │             /
  │  │            /
  │  │           /
  │  │          /
  │  │───────── 冷启动阈值 (1000 / 3 ≈ 333)
  │  │
  │  └──────────────────────────► 时间
  │     ← 预热 10 秒 →
```

##### 排队等待图解

```
请求到达: R1 R2 R3 R4 R5 R6 R7 R8 ... (突发 1000 req/s)
                    ↓
排队队列: [R4] [R5] [R6] [R7] [R8] ... (允许匀速通过)
                    ↓
通过: ---R1---R2---R3---R4---R5---R6--- (间隔 1ms，每秒 1000 req)
```

#### 2.3.4 流控策略（Strategy）

| 策略 | 说明 |
|------|------|
| `STRATEGY_DIRECT` | 直接针对资源本身限流 |
| `STRATEGY_RELATE` | 关联流控：当关联资源达到阈值时限流当前资源 |
| `STRATEGY_CHAIN` | 链路流控：根据调用链路入口限流 |

> 💡 **关联流控** 常用于读写分离场景：写入达到阈值时限制读取流量，保证写入优先。

#### 2.3.5 基于来源的流控

通过 `limitApp` 字段可以针对不同的调用来源设置差异化的限流策略：

```text
resource: "order:create"
limitApp: "service-a"    → service-a 调用该资源时限流 100 QPS
limitApp: "service-b"    → service-b 调用该资源时限流 200 QPS
limitApp: "default"      → 其他来源限流 50 QPS
```

### 2.4 熔断降级规则（Degrade Rule）

#### 2.4.1 DegradeRule 属性

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `resource` | String | — | 资源名称 |
| `grade` | int | — | 熔断策略（慢调用/异常比例/异常数） |
| `count` | double | — | 阈值（慢调用 RT、异常比例、异常数） |
| `timeWindow` | int | — | 熔断时长，单位秒 |
| `minRequestAmount` | int | 5 | 触发熔断的最小请求数 |
| `statIntervalMs` | int | 1000 | 统计时长，单位 ms |
| `slowRatioThreshold` | double | — | 慢调用比例阈值（仅慢调用策略使用） |

#### 2.4.2 熔断策略（Grade）

| 策略 | 常量 | 原理 | 场景 |
|------|------|------|------|
| **慢调用比例** | `DEGRADE_GRADE_RT` | RT > 阈值（ms）的请求占比超限则熔断 | 数据库慢查询、外部 API 超时 |
| **异常比例** | `DEGRADE_GRADE_EXCEPTION_RATIO` | 异常数占总请求比例超限则熔断 | 业务异常频繁的场景 |
| **异常数** | `DEGRADE_GRADE_EXCEPTION_COUNT` | 最近 1 分钟内异常数超限则熔断 | 异常非比例但次数频繁的场景 |

#### 2.4.3 熔断状态机

```
         ┌──────────────────────────────┐
         │                              │
         ▼                              │
    ┌─────────┐   慢调用/异常超阈值   ┌─────────┐
    │  CLOSED  │ ──────────────────►  │  OPEN   │
    │ (正常)   │                     │ (熔断)  │
    └─────────┘                     └─────────┘
         ▲                              │
         │                              │ timeWindow 过后
         │     ┌──────────────┐         │
         │     │  HALF-OPEN   │ ◄───────┘
         │     │ (半开探活)   │
         │     └──────────────┘
         │           │
         └───────────┘
        单次成功则 CLOSED
        再次失败则 OPEN
```

- **CLOSED**：正常工作状态，统计调用数据
- **OPEN**：熔断状态，直接走 fallback，不执行真实逻辑
- **HALF-OPEN**：熔断时间结束后进入半开状态，允许一个探活请求通过
  - 成功 → 恢复 CLOSED
  - 失败 → 再次进入 OPEN

> 💡 半开探活机制防止熔断恢复后流量瞬间涌入导致二次雪崩。

### 2.5 热点参数限流（Param Flow Rule）

#### 2.5.1 原理

热点参数限流针对**带参数的资源调用**，针对**不同的参数值**设置差异化的限流阈值：

```text
resource: "com.example.service.getProductById"
参数：productId

hotParamConfig:
  productId=1001 → QPS 100（爆款商品，限流宽松）
  productId=1002 → QPS 10（普通商品，限流严格）
  productId=1003 → QPS 5（冷门商品）
  default        → QPS 20（默认阈值）
```

#### 2.5.2 ParamFlowRule 属性

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `resource` | String | — | 资源名称 |
| `paramIdx` | int | — | 参数索引（从 0 开始） |
| `grade` | int | `FLOW_GRADE_QPS` | 限流模式，仅支持 QPS |
| `count` | double | — | 限流阈值 |
| `durationInSec` | int | 1 | 统计窗口时长 |
| `controlBehavior` | int | — | 流控效果 |
| `maxQueueingTimeMs` | int | 0 | 排队超时时间 |
| `paramFlowItemList` | List | — | 特定参数值特殊限流配置 |

#### 2.5.3 特定参数配置（ParamFlowItem）

```java
// 针对参数值 1001 限流 100 QPS
ParamFlowItem item = new ParamFlowItem();
item.setObject("1001");
item.setCount(100.0);
item.setClassType(String.class.getName());
```

### 2.6 系统自适应保护（System Rule）

#### 2.6.1 原理

系统规则是**全局规则**，不针对特定资源，而是根据系统维度的指标进行自适应保护：

#### 2.6.2 SystemRule 属性

| 属性 | 类型 | 说明 |
|------|------|------|
| `highestSystemLoad` | double | 系统 Load 阈值（仅 Linux/Unix） |
| `avgRt` | double | 所有入口流量的平均 RT 阈值 |
| `maxThread` | long | 入口流量的并发线程数阈值 |
| `qps` | double | 入口流量的 QPS 阈值 |
| `highestCpuUsage` | double | CPU 使用率阈值（0.0 ~ 1.0） |

#### 2.6.3 保护机制

| 指标 | 触发条件 | 建议阈值 |
|------|----------|----------|
| LOAD | 系统 `System.loadAverage()` > `highestSystemLoad` | 核心数 × 2 ~ 3 |
| CPU | CPU 使用率 > `highestCpuUsage` | 0.8 ~ 0.9 |
| RT | 所有入口平均 RT > `avgRt` | 根据业务确定 |
| 线程数 | 并发线程数 > `maxThread` | Tomcat 线程池大小 |
| QPS | 入口 QPS > `qps` | 根据压测确定 |

> ⚠️ `highestSystemLoad` 在 Windows 下无效，因 Windows 没有 loadAverage 系统调用。

### 2.7 @SentinelResource 注解

#### 2.7.1 注解属性

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `value` | String | — | 资源名称（必填） |
| `entryType` | EntryType | `EntryType.OUT` | 流量类型（IN/OUT） |
| `fallback` | String | — | 降级函数名，返回所有异常（包括业务异常） |
| `blockHandler` | String | — | 流控降级触发的处理函数名 |
| `exceptionsToIgnore` | Class[] | {} | 忽略的异常，不触发 fallback |
| `exceptionsToTrace` | Class[] | {Throwable.class} | 需要 trace 的异常 |

#### 2.7.2 fallback 与 blockHandler 的区别

| 特性 | fallback | blockHandler |
|------|----------|-------------|
| 触发时机 | 业务异常时 | Sentinel 流控/熔断触发时 |
| 异常类型 | `Throwable`（所有异常） | `BlockException`（限流/降级异常） |
| 优先级 | 低 | 高 |
| 是否必填 | 可选 | 可选 |
| 方法签名 | 参数 + `Throwable` | 参数 + `BlockException` |

> 💡 两者可以共存，当 Sentinel 触发限流时走 `blockHandler`，当业务抛出异常时走 `fallback`。

#### 2.7.3 方法签名要求

```java
// 原始方法
@SentinelResource(value = "getUser", fallback = "getUserFallback", blockHandler = "getUserBlockHandler")
public User getUser(Long id) {
    // 业务逻辑
}

// fallback 要求：返回值 + 参数列表 + Throwable
public User getUserFallback(Long id, Throwable t) {
    return new User("default");
}

// blockHandler 要求：返回值 + 参数列表 + BlockException
public User getUserBlockHandler(Long id, BlockException e) {
    return new User("limited");
}
```

> ⚠️ fallback/blockHandler 方法必须定义在同一个类中（若使用 `fallbackClass` 则可以是静态方法）。

### 2.8 自定义异常处理 — UrlBlockHandler

#### 2.8.1 原理

当请求被 Sentinel 拦截时，默认返回 429 Too Many Requests。通过 `UrlBlockHandler` 可以自定义被限流时的 HTTP 响应。

#### 2.8.2 自定义实现

```java
@Component
public class CustomUrlBlockHandler implements UrlBlockHandler {

    @Override
    public void blocked(HttpServletRequest request, HttpServletResponse response, BlockException e)
            throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(429);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 429);
        result.put("message", "请求被限流，请稍后再试");
        result.put("rule", getRuleInfo(e));
        result.put("resource", e.getRule().getResource());
        result.put("timestamp", System.currentTimeMillis());

        response.getWriter().write(JSON.toJSONString(result));
    }

    private String getRuleInfo(BlockException e) {
        if (e instanceof FlowException) {
            return "flow-rule";
        } else if (e instanceof DegradeException) {
            return "degrade-rule";
        } else if (e instanceof ParamFlowException) {
            return "param-flow-rule";
        } else if (e instanceof SystemBlockException) {
            return "system-rule";
        } else if (e instanceof AuthorityException) {
            return "authority-rule";
        }
        return "unknown";
    }
}
```

### 2.9 Sentinel Dashboard 控制台

#### 2.9.1 架构原理

```
                    Sentinel Dashboard
          ┌─────────────────────────────────┐
          │   Web 界面（Vue + Spring MVC）    │
          │   规则管理 / 实时监控 / 日志查询    │
          └──────────┬──────────────────────┘
                     │  transport API
           ┌─────────┴──────────┐
           ▼                    ▼
    ┌──────────────┐    ┌──────────────┐
    │  App Server1  │    │  App Server2  │
    │  Sentinel     │    │  Sentinel     │
    │  Client       │    │  Client       │
    └──────────────┘    └──────────────┘
           │                    │
           ▼                    ▼
    ┌──────────────┐    ┌──────────────┐
    │  Nacos       │    │  Nacos       │
    │  (持久化)    │    │  (持久化)    │
    └──────────────┘    └──────────────┘
```

#### 2.9.2 Dashboard 启动

```bash
# 下载 jar 包
# https://github.com/alibaba/Sentinel/releases

# 启动 Dashboard（默认端口 8080）
java -Dserver.port=8080 \
     -Dcsp.sentinel.dashboard.server=localhost:8080 \
     -jar sentinel-dashboard-1.8.6.jar

# 生产环境建议配置鉴权
java -Dserver.port=8080 \
     -Dsentinel.dashboard.auth.username=sentinel \
     -Dsentinel.dashboard.auth.password=123456 \
     -jar sentinel-dashboard-1.8.6.jar
```

#### 2.9.3 客户端连接配置

```yaml
# application.yml
spring:
  cloud:
    sentinel:
      transport:
        dashboard: localhost:8080  # Dashboard 地址
        port: 8719                  # 客户端与 Dashboard 通信端口
      eager: true                   # 启动时立即注册，不延迟到首次调用
```

### 2.10 规则持久化（Push 到 Nacos）

#### 2.10.1 三种模式对比

| 模式 | 原理 | 优点 | 缺点 |
|------|------|------|------|
| **原始模式** | 内存存储，重启丢失 | 简单 | 不支持持久化 |
| **Pull 模式** | 客户端定期从外部拉取 | 支持持久化 | 实时性差 |
| **Push 模式** | 配置中心推送到客户端 | 实时性高，统一管理 | 需要集成配置中心（推荐） |

#### 2.10.2 Push 模式架构

```
Sentinel Dashboard ──→ Nacos Config ──→ 应用 Server
       │                                     │
       │ 1. 在 Dashboard 配置规则              │ 3. 监听 Nacos 配置变更
       │ 2. Dashboard 将规则写入 Nacos         │ 4. 规则实时生效
       └──────────────────────────────────────┘
```

#### 2.10.3 Nacos 中规则配置格式

```json
// Nacos Data ID: product-service-flow-rules
// Group: SENTINEL_GROUP
// 配置格式 JSON

[
    {
        "resource": "com.example.product.service.ProductService:getProduct",
        "limitApp": "default",
        "grade": 1,
        "count": 100.0,
        "strategy": 0,
        "controlBehavior": 0,
        "clusterMode": false
    },
    {
        "resource": "com.example.product.service.ProductService:createOrder",
        "limitApp": "default",
        "grade": 1,
        "count": 50.0,
        "strategy": 0,
        "controlBehavior": 1,
        "warmUpPeriodSec": 10,
        "clusterMode": false
    }
]
```

#### 2.10.4 各类规则的 Nacos 配置

```text
# 流控规则
Data ID: {appName}-flow-rules
Group: SENTINEL_GROUP

# 降级规则
Data ID: {appName}-degrade-rules
Group: SENTINEL_GROUP

# 热点规则
Data ID: {appName}-param-flow-rules
Group: SENTINEL_GROUP

# 系统规则
Data ID: {appName}-system-rules
Group: SENTINEL_GROUP

# 授权规则
Data ID: {appName}-authority-rules
Group: SENTINEL_GROUP
```

---

## 3. 高频踩坑与误区

### 3.1 @SentinelResource 不生效

**错误现象**：添加了 `@SentinelResource` 注解但不触发限流。

```java
// 错误写法
@SentinelResource(value = "test")
@RequestMapping("/test")
public String test() {
    return "ok";
}
```

**根因分析**：Sentinel 默认基于微服务调用（RestTemplate/Feign）进行埋点，单独的 `@SentinelResource` 注解需要配合 AOP 切面才能生效。

**正确做法**：引入 `sentinel-annotation-aspectj` 依赖并注册切面。

```java
@Configuration
public class SentinelAspectConfig {
    @Bean
    public SentinelResourceAspect sentinelResourceAspect() {
        return new SentinelResourceAspect();
    }
}
```

### 3.2 fallback 方法签名不匹配

**错误现象**：`@SentinelResource` 配置了 fallback，但触发异常时报错 `No such method`。

**根因分析**：fallback 方法签名必须与原始方法一致，且多一个 `Throwable` 参数。

```java
// 原始方法
public String getUser(Long id, String name) { ... }

// 错误写法 - 参数个数不匹配
public String getUserFallback(Throwable t) { ... }

// 错误写法 - 类型顺序不匹配
public String getUserFallback(Throwable t, Long id, String name) { ... }

// 正确写法
public String getUserFallback(Long id, String name, Throwable t) { ... }
```

### 3.3 Dashboard 没有数据

**错误现象**：控制台启动成功，服务也注册了，但监控页面没有数据。

**根因分析**：
1. 未开启 `eager: true`，需要首次请求才触发注册
2. 网络不通（防火墙阻塞 8719 端口）
3. 应用没有配置 `spring.application.name`

**排查步骤**：

```bash
# 1. 检查应用是否注册
curl http://localhost:8719/api?type=metrics

# 2. 检查 Dashboard 日志
tail -f logs/csp/sentinel-record.log

# 3. 确认应用配置
# spring.application.name 必须设置
# spring.cloud.sentinel.transport.dashboard 地址正确
```

### 3.4 规则重启丢失

**错误现象**：Dashboard 中配置的规则，应用重启后全部消失。

**根因分析**：Sentinel 默认将规则存储在**应用内存**中，重启即丢失。

**解决方案**：必须配置规则持久化，推荐 Push 模式集成 Nacos。

### 3.5 流控不精确

**错误现象**：设置了 QPS=100，但实际通过的请求超过 100。

**根因分析**：
1. 滑动窗口的统计窗口为 1 秒，子窗口为 500ms，存在统计粒度误差（约 ±5%）
2. 并发场景下多个线程同时通过

```text
时间线（子窗口 500ms）：
[0ms - 500ms] 通过 60 个请求
[500ms - 1000ms] 通过 55 个请求
总计 115 个请求 > 阈值 100
```

> 💡 如果对精度要求极高，可以缩短窗口时间，或使用精确限流（单机限流 + 本地计数器）。

### 3.6 热点限流参数索引错误

**错误现象**：热点参数限流不生效，配置的参数索引与实际参数位置不匹配。

```java
// 资源方法
@SentinelResource(value = "getProduct")
public Product getProduct(Long categoryId, Long productId) {
    // productId 是第 1 个参数（索引 1）
}

// 错误配置 — 索引为 0，限流的是 categoryId
paramIdx: 0, count: 10

// 正确配置 — 索引为 1，限流的是 productId
paramIdx: 1, count: 10
```

### 3.7 SystemRule 在 Windows 下无效

**错误现象**：配置了最高系统负载 `highestSystemLoad` 但始终不触发。

**根因分析**：`SystemRule` 依赖 `System.loadAverage()` 系统调用，该接口在 Windows 上始终返回 -1。

**解决方案**：Windows 开发环境下使用 CPU 规则替代 Load 规则：

```java
SystemRule rule = new SystemRule();
// rule.setHighestSystemLoad(10.0);  // Windows 无效
rule.setHighestCpuUsage(0.8);         // 使用 CPU 规则
```

### 3.8 Feign 调用未触发 Sentinel

**错误现象**：配置了 Feign 客户端，但 Sentinel 没有对 Feign 调用进行保护。

**根因分析**：Feign 的 Sentinel 集成需要开启 `feign.sentinel.enabled`。

```yaml
# 必须配置
feign:
  sentinel:
    enabled: true

# application.yml 中还需开启
spring:
  cloud:
    sentinel:
      feign:
        enabled: true
```

### 3.9 blockHandler 和 fallback 同时配置时的误区

**错误现象**：业务异常也走了 blockHandler 而非 fallback。

**根因分析**：两者同时配置时，blockHandler 的优先级高于 fallback，但仅当 `BlockException` 触发时才走 blockHandler。业务异常（非 `BlockException`）会走 fallback。需要注意 blockHandler 只处理限流降级异常。

```java
@SentinelResource(
    value = "test",
    blockHandler = "blockHandler",   // FlowException, DegradeException 等
    fallback = "fallback"            // 其他业务异常
)
```

### 3.10 Nacos 数据格式错误导致规则未加载

**错误现象**：Nacos 中配置了规则，但应用没有加载到规则。

**根因分析**：JSON 格式不规范或字段名称与规则属性不匹配。

```json
// 错误写法 — 字段名不对
[
    {
        "resourceName": "test",   // 应为 resource
        "limitQPS": 100           // 应为 count
    }
]

// 正确写法
[
    {
        "resource": "test",
        "grade": 1,
        "count": 100.0,
        "limitApp": "default",
        "strategy": 0,
        "controlBehavior": 0
    }
]
```

---

## 4. 随堂基础练习

### 练习 1：基础流控规则配置

**需求**：为 `/api/order/create` 资源配置流控规则，QPS 限制为 50，使用快速失败策略。

**提示**：

```java
// 方式一：代码方式
FlowRule rule = new FlowRule();
rule.setResource("/api/order/create");
rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
rule.setCount(50.0);
rule.setControlBehavior(RuleConstant.CONTROL_BEHAVIOR_DEFAULT);
FlowRuleManager.loadRules(Collections.singletonList(rule));

// 方式二：配置文件（不持久）
// 仅测试使用
```

**验证**：使用 JMeter 或 curl 模拟并发请求，观察被限流的请求返回 429。

### 练习 2：Warm-Up 预热限流

**需求**：配置一个 Warm-Up 规则，最终阈值 1000 QPS，预热时长 20 秒。

| 属性 | 值 |
|------|-----|
| resource | `/api/product/list` |
| grade | QPS |
| count | 1000 |
| controlBehavior | WARM_UP |
| warmUpPeriodSec | 20 |

**验证**：启动后前 20 秒阈值逐步从 333 升至 1000 QPS。

### 练习 3：熔断降级 — 异常比例

**需求**：当接口错误率超过 50% 时熔断 30 秒。

```java
DegradeRule rule = new DegradeRule();
rule.setResource("payService");
rule.setGrade(RuleConstant.DEGRADE_GRADE_EXCEPTION_RATIO);
rule.setCount(0.5);      // 50% 异常比例
rule.setTimeWindow(30);   // 熔断 30 秒
rule.setMinRequestAmount(10);  // 至少 10 个请求才触发
DegradeRuleManager.loadRules(Collections.singletonList(rule));
```

### 练习 4：热点参数限流

**需求**：对 `getProductById(Long productId)` 方法进行热点限流，默认 QPS=20，商品 1001 限流 QPS=200。

**提示**：使用 `@SentinelResource` + `ParamFlowRule` 组合。

```java
ParamFlowRule rule = new ParamFlowRule("getProductById")
    .setParamIdx(0)
    .setCount(20);

// 特例：商品 1001 放宽限流
ParamFlowItem item = new ParamFlowItem()
    .setObject(String.valueOf(1001L))
    .setClassType("java.lang.Long")
    .setCount(200);
rule.setParamFlowItemList(Collections.singletonList(item));

ParamFlowRuleManager.loadRules(Collections.singletonList(rule));
```

### 练习 5：Dashboard 搭建与连接

**步骤**：

```bash
# 1. 启动 Dashboard
java -jar sentinel-dashboard-1.8.6.jar --server.port=8888

# 2. 应用配置
# application.yml
spring.cloud.sentinel.transport.dashboard=localhost:8888
spring.cloud.sentinel.transport.port=8719
spring.cloud.sentinel.eager=true

# 3. 启动后访问 http://localhost:8888
# 默认账号密码：sentinel / sentinel
# 观察应用是否注册在机器列表中
```

### 练习 6：@SentinelResource 完整用法

**需求**：编写一个服务方法，同时配置 fallback 和 blockHandler，对比两者的触发时机。

```java
@Service
public class OrderService {

    @SentinelResource(
        value = "createOrder",
        fallback = "createOrderFallback",
        blockHandler = "createOrderBlockHandler"
    )
    public Order createOrder(OrderDTO dto) {
        if (dto.getAmount() > 10000) {
            throw new RuntimeException("订单金额超限");
        }
        return new Order(dto);
    }

    // fallback — 业务异常触发
    public Order createOrderFallback(OrderDTO dto, Throwable t) {
        System.out.println("Fallback: " + t.getMessage());
        return new Order(dto.getUserId(), "DEFAULT");
    }

    // blockHandler — 流控/熔断触发
    public Order createOrderBlockHandler(OrderDTO dto, BlockException e) {
        System.out.println("Blocked: " + e.getClass().getSimpleName());
        return new Order(dto.getUserId(), "LIMITED");
    }
}
```

---

## 5. 章节综合实操案例

### 5.1 案例概述

**业务场景**：构建一个商品服务（Product Service），提供商品详情查询、商品搜索、库存扣减三个核心接口。使用 Sentinel 对三个接口进行差异化保护。

**系统架构**：

```
┌─────────────┐     HTTP/REST      ┌─────────────────────────────┐
│  客户端服务  │ ──────────────────► │     商品服务 Product Service    │
│  (模拟调用)  │                    │  ┌─────────────────────────┐  │
└─────────────┘                    │  │   ProductController      │  │
                                   │  ├─────────────────────────┤  │
                                   │  │   ProductService         │  │
                                   │  ├─────────────────────────┤  │
                                   │  │   InventoryService       │  │
                                   │  ├─────────────────────────┤  │
                                   │  │   Sentinel + Nacos       │  │
                                   │  └─────────────────────────┘  │
                                   └─────────────────────────────┘
                                               │
                                               ▼
                                   ┌─────────────────────┐
                                   │     Nacos Config     │
                                   │  (规则持久化)        │
                                   └─────────────────────┘
```

**保护策略**：

| 接口 | 流控策略 | 熔断策略 |
|------|----------|----------|
| 商品详情查询 `/product/{id}` | QPS=200，Warm-Up 10s | 慢调用比例，RT>200ms，熔断 30s |
| 商品搜索 `/product/search` | QPS=100，排队等待 | 异常比例>30%，熔断 60s |
| 库存扣减 `/inventory/deduct` | QPS=50，快速失败 | 异常数>10，熔断 30s |
| 热点参数（商品 ID=1001，爆款） | QPS=500 | — |

### 5.2 项目结构

```
product-service/
├── pom.xml
├── src/main/java/com/example/product/
│   ├── ProductApplication.java
│   ├── config/
│   │   ├── SentinelAspectConfig.java
│   │   ├── SentinelBlockHandlerConfig.java    # UrlBlockHandler
│   │   └── SentinelDataSourceConfig.java      # Nacos 数据源
│   ├── controller/
│   │   ├── ProductController.java
│   │   └── InventoryController.java
│   ├── service/
│   │   ├── ProductService.java
│   │   └── InventoryService.java
│   ├── entity/
│   │   ├── Product.java
│   │   └── Inventory.java
│   ├── exception/
│   │   └── BusinessException.java
│   └── common/
│       └── Result.java
├── src/main/resources/
│   ├── application.yml
│   └── bootstrap.yml
└── sentinel-rules/                   # 规则 JSON 样例
    ├── flow-rules.json
    ├── degrade-rules.json
    └── param-flow-rules.json
```

### 5.3 完整代码实现

#### 5.3.1 pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>2.7.18</version>
    </parent>

    <groupId>com.example</groupId>
    <artifactId>product-service</artifactId>
    <version>1.0.0</version>
    <name>product-service</name>

    <properties>
        <java.version>1.8</java.version>
        <spring-cloud.version>2021.0.5</spring-cloud.version>
        <spring-cloud-alibaba.version>2021.0.5.0</spring-cloud-alibaba.version>
    </properties>

    <dependencies>
        <!-- Spring Boot Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Spring Cloud Alibaba Sentinel -->
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-sentinel</artifactId>
        </dependency>

        <!-- Sentinel 注解 AOP -->
        <dependency>
            <groupId>com.alibaba.csp</groupId>
            <artifactId>sentinel-annotation-aspectj</artifactId>
        </dependency>

        <!-- Sentinel Nacos 数据源 -->
        <dependency>
            <groupId>com.alibaba.csp</groupId>
            <artifactId>sentinel-datasource-nacos</artifactId>
        </dependency>

        <!-- Nacos Config -->
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- FastJSON -->
        <dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>fastjson</artifactId>
            <version>1.2.83</version>
        </dependency>
    </dependencies>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <dependency>
                <groupId>com.alibaba.cloud</groupId>
                <artifactId>spring-cloud-alibaba-dependencies</artifactId>
                <version>${spring-cloud-alibaba.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

#### 5.3.2 主启动类

```java
package com.example.product;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ProductApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductApplication.class, args);
    }
}
```

#### 5.3.3 bootstrap.yml

```yaml
spring:
  application:
    name: product-service
  cloud:
    nacos:
      config:
        server-addr: 127.0.0.1:8848
        file-extension: yaml
        namespace: public
        group: DEFAULT_GROUP
```

#### 5.3.4 application.yml

```yaml
server:
  port: 8081

spring:
  cloud:
    sentinel:
      transport:
        dashboard: localhost:8080
        port: 8719
      eager: true
      # Nacos 数据源配置（规则持久化）
      datasource:
        # 流控规则
        ds1-flow:
          nacos:
            server-addr: ${spring.cloud.nacos.config.server-addr}
            data-id: ${spring.application.name}-flow-rules
            group-id: SENTINEL_GROUP
            data-type: json
            rule-type: flow
        # 降级规则
        ds2-degrade:
          nacos:
            server-addr: ${spring.cloud.nacos.config.server-addr}
            data-id: ${spring.application.name}-degrade-rules
            group-id: SENTINEL_GROUP
            data-type: json
            rule-type: degrade
        # 热点规则
        ds3-param-flow:
          nacos:
            server-addr: ${spring.cloud.nacos.config.server-addr}
            data-id: ${spring.application.name}-param-flow-rules
            group-id: SENTINEL_GROUP
            data-type: json
            rule-type: param-flow
        # 系统规则
        ds4-system:
          nacos:
            server-addr: ${spring.cloud.nacos.config.server-addr}
            data-id: ${spring.application.name}-system-rules
            group-id: SENTINEL_GROUP
            data-type: json
            rule-type: system

feign:
  sentinel:
    enabled: true

logging:
  level:
    com.alibaba.csp.sentinel: INFO
```

#### 5.3.5 实体类

```java
package com.example.product.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    private Long id;
    private String name;
    private String category;
    private BigDecimal price;
    private Integer stock;
    private String description;
}
```

```java
package com.example.product.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Inventory {
    private Long productId;
    private Integer totalStock;
    private Integer reservedStock;
    private Integer availableStock;
}
```

```java
package com.example.product.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {
    private Integer code;
    private String message;
    private T data;
    private Long timestamp;

    public static <T> Result<T> success(T data) {
        return new Result<>(200, "success", data, System.currentTimeMillis());
    }

    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null, System.currentTimeMillis());
    }

    public static <T> Result<T> blocked(String message) {
        return new Result<>(429, message, null, System.currentTimeMillis());
    }
}
```

#### 5.3.6 Sentinel 切面配置

```java
package com.example.product.config;

import com.alibaba.csp.sentinel.annotation.aspectj.SentinelResourceAspect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SentinelAspectConfig {

    @Bean
    public SentinelResourceAspect sentinelResourceAspect() {
        return new SentinelResourceAspect();
    }
}
```

#### 5.3.7 自定义 UrlBlockHandler

```java
package com.example.product.config;

import com.alibaba.csp.sentinel.adapter.spring.webmvc.callback.BlockExceptionHandler;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.authority.AuthorityException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowException;
import com.alibaba.csp.sentinel.slots.system.SystemBlockException;
import com.example.product.common.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 自定义 Sentinel 限流异常处理
 * 替代默认的 429 页面，返回统一 JSON 格式
 */
@Component
public class SentinelBlockHandlerConfig implements BlockExceptionHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       String resourceName,
                       BlockException e) throws IOException {

        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(429);

        String message = buildMessage(e);
        String detail = buildDetail(e);

        // 记录限流日志
        System.err.printf("[Sentinel Blocked] resource=%s, rule=%s, detail=%s%n",
                resourceName, message, detail);

        Result<?> result = Result.blocked(message);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }

    private String buildMessage(BlockException e) {
        if (e instanceof FlowException) {
            return "请求过于频繁，请稍后重试（流控）";
        } else if (e instanceof DegradeException) {
            return "服务暂时不可用，请稍后重试（熔断降级）";
        } else if (e instanceof ParamFlowException) {
            return "热点参数访问频率过高（热点限流）";
        } else if (e instanceof SystemBlockException) {
            return "系统负载过高，请稍后重试（系统保护）";
        } else if (e instanceof AuthorityException) {
            return "无访问权限（授权规则）";
        }
        return "请求被拦截";
    }

    private String buildDetail(BlockException e) {
        if (e.getRule() != null) {
            return String.format("rule=%s, limitApp=%s, count=%s",
                    e.getRule().getClass().getSimpleName(),
                    e.getRule().getLimitApp(),
                    e.getRule().getCount());
        }
        return "unknown";
    }
}
```

#### 5.3.8 Nacos 数据源配置（Java 配置方式）

```java
package com.example.product.config;

import com.alibaba.csp.sentinel.datasource.ReadableDataSource;
import com.alibaba.csp.sentinel.datasource.nacos.NacosDataSource;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRuleManager;
import com.alibaba.csp.sentinel.slots.system.SystemRule;
import com.alibaba.csp.sentinel.slots.system.SystemRuleManager;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * Sentinel 规则 Nacos 数据源配置
 * 通过 Nacos 推送规则到 Sentinel
 */
@Configuration
public class SentinelDataSourceConfig {

    @Value("${spring.cloud.nacos.config.server-addr:127.0.0.1:8848}")
    private String nacosServerAddr;

    private static final String GROUP_ID = "SENTINEL_GROUP";

    @PostConstruct
    public void init() {
        initFlowRules();
        initDegradeRules();
        initParamFlowRules();
        initSystemRules();
    }

    /**
     * 流控规则数据源
     */
    private void initFlowRules() {
        String dataId = "product-service-flow-rules";
        ReadableDataSource<String, List<FlowRule>> flowRuleDataSource =
                new NacosDataSource<>(nacosServerAddr, GROUP_ID, dataId,
                        source -> JSON.parseObject(source, new TypeReference<List<FlowRule>>() {}));
        FlowRuleManager.register2Property(flowRuleDataSource.getProperty());

        System.out.println("[Sentinel] Flow rules data source loaded from Nacos: " + dataId);
    }

    /**
     * 降级规则数据源
     */
    private void initDegradeRules() {
        String dataId = "product-service-degrade-rules";
        ReadableDataSource<String, List<DegradeRule>> degradeRuleDataSource =
                new NacosDataSource<>(nacosServerAddr, GROUP_ID, dataId,
                        source -> JSON.parseObject(source, new TypeReference<List<DegradeRule>>() {}));
        DegradeRuleManager.register2Property(degradeRuleDataSource.getProperty());

        System.out.println("[Sentinel] Degrade rules data source loaded from Nacos: " + dataId);
    }

    /**
     * 热点参数规则数据源
     */
    private void initParamFlowRules() {
        String dataId = "product-service-param-flow-rules";
        ReadableDataSource<String, List<ParamFlowRule>> paramFlowRuleDataSource =
                new NacosDataSource<>(nacosServerAddr, GROUP_ID, dataId,
                        source -> JSON.parseObject(source, new TypeReference<List<ParamFlowRule>>() {}));
        ParamFlowRuleManager.register2Property(paramFlowRuleDataSource.getProperty());

        System.out.println("[Sentinel] Param flow rules data source loaded from Nacos: " + dataId);
    }

    /**
     * 系统规则数据源
     */
    private void initSystemRules() {
        String dataId = "product-service-system-rules";
        ReadableDataSource<String, List<SystemRule>> systemRuleDataSource =
                new NacosDataSource<>(nacosServerAddr, GROUP_ID, dataId,
                        source -> JSON.parseObject(source, new TypeReference<List<SystemRule>>() {}));
        SystemRuleManager.register2Property(systemRuleDataSource.getProperty());

        System.out.println("[Sentinel] System rules data source loaded from Nacos: " + dataId);
    }
}
```

#### 5.3.9 ProductController

```java
package com.example.product.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.example.product.common.Result;
import com.example.product.entity.Product;
import com.example.product.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/product")
public class ProductController {

    @Autowired
    private ProductService productService;

    /**
     * 商品详情查询
     * 流控策略：QPS=200，Warm-Up 10s
     * 熔断策略：慢调用比例，RT>200ms 比例>50%，熔断30s
     */
    @GetMapping("/{id}")
    @SentinelResource(
        value = "com.example.product.service.ProductService:getProduct",
        blockHandler = "getProductBlockHandler",
        fallback = "getProductFallback"
    )
    public Result<Product> getProduct(@PathVariable Long id) {
        // 模拟慢调用场景
        if (id == 999L) {
            try {
                Thread.sleep(500); // 模拟超时
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        Product product = productService.getProduct(id);
        return Result.success(product);
    }

    /**
     * blockHandler — 流控/熔断触发
     */
    public Result<Product> getProductBlockHandler(Long id, BlockException e) {
        System.err.println("[Sentinel Block] getProduct blocked, id=" + id + ", exception=" + e.getClass().getSimpleName());
        return Result.blocked("商品查询被限流，请稍后重试");
    }

    /**
     * fallback — 业务异常触发
     */
    public Result<Product> getProductFallback(Long id, Throwable t) {
        System.err.println("[Sentinel Fallback] getProduct fallback, id=" + id + ", error=" + t.getMessage());
        return Result.error(500, "商品查询异常：" + t.getMessage());
    }

    /**
     * 商品搜索
     * 流控策略：QPS=100，排队等待
     * 熔断策略：异常比例>30%，熔断60s
     */
    @GetMapping("/search")
    @SentinelResource(
        value = "com.example.product.service.ProductService:searchProduct",
        blockHandler = "searchProductBlockHandler",
        fallback = "searchProductFallback"
    )
    public Result<List<Product>> searchProduct(@RequestParam String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new IllegalArgumentException("搜索关键词不能为空");
        }
        List<Product> products = productService.searchProduct(keyword);
        return Result.success(products);
    }

    public Result<List<Product>> searchProductBlockHandler(String keyword, BlockException e) {
        return Result.blocked("商品搜索被限流，请稍后重试");
    }

    public Result<List<Product>> searchProductFallback(String keyword, Throwable t) {
        return Result.error(500, "搜索异常：" + t.getMessage());
    }
}
```

#### 5.3.10 InventoryController

```java
package com.example.product.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.example.product.common.Result;
import com.example.product.service.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/inventory")
public class InventoryController {

    @Autowired
    private InventoryService inventoryService;

    /**
     * 库存扣减
     * 流控策略：QPS=50，快速失败
     * 熔断策略：异常数>10，熔断30s
     */
    @PostMapping("/deduct")
    @SentinelResource(
        value = "com.example.product.service.InventoryService:deductStock",
        blockHandler = "deductStockBlockHandler",
        fallback = "deductStockFallback"
    )
    public Result<String> deductStock(@RequestParam Long productId,
                                      @RequestParam Integer quantity) {
        boolean success = inventoryService.deductStock(productId, quantity);
        if (success) {
            return Result.success("库存扣减成功");
        }
        return Result.error(400, "库存不足");
    }

    public Result<String> deductStockBlockHandler(Long productId, Integer quantity, BlockException e) {
        return Result.blocked("库存扣减被限流，请稍后重试");
    }

    public Result<String> deductStockFallback(Long productId, Integer quantity, Throwable t) {
        return Result.error(500, "库存扣减异常：" + t.getMessage());
    }
}
```

#### 5.3.11 ProductService

```java
package com.example.product.service;

import com.example.product.entity.Product;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final Map<Long, Product> productDB = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // 初始化商品数据
        productDB.put(1001L, new Product(1001L, "iPhone 15 Pro Max", "手机",
                new BigDecimal("9999.00"), 1000, "苹果旗舰手机"));
        productDB.put(1002L, new Product(1002L, "MacBook Air M3", "笔记本",
                new BigDecimal("12999.00"), 500, "苹果轻薄笔记本"));
        productDB.put(1003L, new Product(1003L, "AirPods Pro 2", "耳机",
                new BigDecimal("1999.00"), 2000, "主动降噪耳机"));
        productDB.put(1004L, new Product(1004L, "iPad Air", "平板",
                new BigDecimal("5999.00"), 800, "苹果平板电脑"));
    }

    /**
     * 查询商品详情 — 受 Sentinel 保护
     */
    public Product getProduct(Long id) {
        // 模拟数据库查询延迟
        try {
            Thread.sleep(20);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Product product = productDB.get(id);
        if (product == null) {
            throw new RuntimeException("商品不存在, id=" + id);
        }
        return product;
    }

    /**
     * 搜索商品
     */
    public List<Product> searchProduct(String keyword) {
        // 模拟搜索耗时
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return productDB.values().stream()
                .filter(p -> p.getName().contains(keyword) || p.getCategory().contains(keyword))
                .collect(Collectors.toList());
    }
}
```

#### 5.3.12 InventoryService

```java
package com.example.product.service;

import com.example.product.entity.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class InventoryService {

    @Autowired
    private ProductService productService;

    /**
     * 库存数据（模拟）
     * productId -> availableStock
     */
    private final Map<Long, Integer> inventoryDB = new ConcurrentHashMap<>();
    private final Map<Long, ReentrantLock> stockLocks = new ConcurrentHashMap<>();

    /**
     * 初始化库存
     */
    public void initStock(Long productId, Integer stock) {
        inventoryDB.put(productId, stock);
        stockLocks.put(productId, new ReentrantLock());
    }

    /**
     * 扣减库存
     * @param productId 商品 ID
     * @param quantity 扣减数量
     * @return true=扣减成功, false=库存不足
     */
    public boolean deductStock(Long productId, Integer quantity) {
        // 检查商品是否存在
        Product product = productService.getProduct(productId);

        // 获取库存锁，保证线程安全
        ReentrantLock lock = stockLocks.computeIfAbsent(productId, k -> new ReentrantLock());
        lock.lock();
        try {
            Integer available = inventoryDB.getOrDefault(productId, 0);
            if (available < quantity) {
                return false;
            }
            // 模拟扣减耗时
            Thread.sleep(30);
            inventoryDB.put(productId, available - quantity);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 查询库存
     */
    public Integer getStock(Long productId) {
        return inventoryDB.getOrDefault(productId, 0);
    }
}
```

#### 5.3.13 测试 Controller（压力测试模拟）

```java
package com.example.product.controller;

import com.example.product.common.Result;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 压力测试端点 — 模拟多来源调用
 */
@RestController
@RequestMapping("/benchmark")
public class BenchmarkController {

    private final AtomicInteger counter = new AtomicInteger(0);

    /**
     * 批量压测：模拟多个客户端同时请求商品详情
     */
    @GetMapping("/product/{id}")
    public Result<Map<String, Object>> benchmarkGetProduct(@PathVariable Long id) {
        long start = System.currentTimeMillis();
        int seq = counter.incrementAndGet();

        Map<String, Object> result = new HashMap<>();
        result.put("seq", seq);
        result.put("productId", id);
        result.put("timestamp", start);
        result.put("elapsed", System.currentTimeMillis() - start);

        return Result.success(result);
    }

    /**
     * 重置计数器
     */
    @GetMapping("/reset")
    public Result<String> reset() {
        counter.set(0);
        return Result.success("counter reset");
    }

    /**
     * 查看当前计数
     */
    @GetMapping("/count")
    public Result<Integer> count() {
        return Result.success(counter.get());
    }
}
```

### 5.4 Nacos 规则配置

在 Nacos Config 中创建以下配置：

#### 5.4.1 流控规则 product-service-flow-rules

```json
[
    {
        "resource": "com.example.product.service.ProductService:getProduct",
        "limitApp": "default",
        "grade": 1,
        "count": 200.0,
        "strategy": 0,
        "controlBehavior": 1,
        "warmUpPeriodSec": 10,
        "clusterMode": false
    },
    {
        "resource": "com.example.product.service.ProductService:searchProduct",
        "limitApp": "default",
        "grade": 1,
        "count": 100.0,
        "strategy": 0,
        "controlBehavior": 2,
        "maxQueueingTimeMs": 500,
        "clusterMode": false
    },
    {
        "resource": "com.example.product.service.InventoryService:deductStock",
        "limitApp": "default",
        "grade": 1,
        "count": 50.0,
        "strategy": 0,
        "controlBehavior": 0,
        "clusterMode": false
    }
]
```

#### 5.4.2 降级规则 product-service-degrade-rules

```json
[
    {
        "resource": "com.example.product.service.ProductService:getProduct",
        "grade": 0,
        "count": 200.0,
        "timeWindow": 30,
        "minRequestAmount": 5,
        "statIntervalMs": 1000,
        "slowRatioThreshold": 0.5
    },
    {
        "resource": "com.example.product.service.ProductService:searchProduct",
        "grade": 1,
        "count": 0.3,
        "timeWindow": 60,
        "minRequestAmount": 10,
        "statIntervalMs": 1000
    },
    {
        "resource": "com.example.product.service.InventoryService:deductStock",
        "grade": 2,
        "count": 10.0,
        "timeWindow": 30,
        "minRequestAmount": 5,
        "statIntervalMs": 1000
    }
]
```

#### 5.4.3 热点规则 product-service-param-flow-rules

```json
[
    {
        "resource": "com.example.product.service.ProductService:getProduct",
        "paramIdx": 0,
        "grade": 1,
        "count": 50.0,
        "durationInSec": 1,
        "controlBehavior": 0,
        "maxQueueingTimeMs": 0,
        "paramFlowItemList": [
            {
                "object": "1001",
                "count": 500.0,
                "classType": "java.lang.Long"
            }
        ]
    }
]
```

#### 5.4.4 系统规则 product-service-system-rules

```json
[
    {
        "highestSystemLoad": 10.0,
        "avgRt": 500,
        "maxThread": 200,
        "qps": 10000,
        "highestCpuUsage": 0.8
    }
]
```

### 5.5 启动与验证

#### 5.5.1 启动步骤

```bash
# 1. 启动 Nacos Server
startup.cmd -m standalone

# 2. 启动 Sentinel Dashboard
java -Dserver.port=8080 -jar sentinel-dashboard-1.8.6.jar

# 3. 在 Nacos 中添加上述规则配置

# 4. 启动商品服务
mvn spring-boot:run

# 5. 验证规则加载
# 查看应用日志确认规则已加载：
# [Sentinel] Flow rules data source loaded from Nacos
```

#### 5.5.2 压力测试脚本

```bash
# 使用 curl 模拟并发请求
# 测试流控 — 快速发送大量请求
for i in {1..500}; do
    curl -s "http://localhost:8081/product/1001" &
done
wait

# 测试慢调用熔断
for i in {1..100}; do
    curl -s "http://localhost:8081/product/999" &   # 999 触发慢调用
done
wait

# 观察产物
# - 部分请求返回 429（被限流）
# - 999 请求触发慢调用熔断后，所有请求返回 429
```

#### 5.5.3 JMeter 压测计划

```
Thread Group (100 线程, Loop 100 次)
├── HTTP Request: GET /product/1001
├── Listener: View Results Tree
├── Listener: Summary Report
├── Listener: Aggregate Report
└── Listener: Backend Listener (可选，对接 Dashboard)
```

> 💡 建议使用 JMeter 或 AB（Apache Bench）进行压测验证限流效果。

---

## 6. 分层综合习题

### 6.1 基础习题

**Q1**：Sentinel 的三大核心能力是什么？

<details>
<summary>答案</summary>
流量控制（Flow Control）、熔断降级（Circuit Breaking）、系统自适应保护（System Protection）。
</details>

**Q2**：`@SentinelResource` 注解中的 `fallback` 和 `blockHandler` 有什么区别？

<details>
<summary>答案</summary>
`fallback` 处理业务异常（Throwable），`blockHandler` 处理 Sentinel 限流/熔断异常（BlockException）。blockHandler 优先级高于 fallback。
</details>

**Q3**：Sentinel 的流控效果有哪几种？分别适用什么场景？

<details>
<summary>答案</summary>
1. 快速失败（Default）— 默认，适用于一般限流
2. Warm-Up — 系统冷启动，逐步增加阈值
3. 排队等待（Rate Limiter）— 削峰填谷，适用消息处理
4. Warm-Up + 排队等待 — 同时具备预热和匀速能力
</details>

**Q4**：熔断降级的三种策略是什么？

<details>
<summary>答案</summary>
1. 慢调用比例（SLOW_REQUEST_RATIO）
2. 异常比例（ERROR_RATIO）
3. 异常数（ERROR_COUNT）
</details>

**Q5**：Sentinel 的熔断状态机包含哪些状态？

<details>
<summary>答案</summary>
CLOSED（关闭/正常）→ OPEN（开启/熔断）→ HALF_OPEN（半开/探活）→ CLOSED 或 OPEN。
</details>

### 6.2 进阶习题

**Q6**：Sentinel 滑动窗口统计的原理是什么？如何影响限流的精度？

<details>
<summary>答案</summary>
Sentinel 使用 LeapArray 滑动窗口数组，默认将 1 秒分为 2 个 500ms 的子窗口（SampleBucket）。当请求到达时，根据时间戳定位到对应的子窗口进行计数。由于窗口切换的边界问题，实际精度约为 ±1 个子窗口的时间（±500ms），最大误差约 5%。可通过减少子窗口时间提高精度，但会增加内存开销。
</details>

**Q7**：如何在分布式场景下使用 Sentinel 进行集群限流？

<details>
<summary>答案</summary>
Sentinel 支持 Token Server/Client 模式的集群限流：
1. 选择一个节点作为 Token Server（嵌入或独立部署）
2. 其他节点作为 Token Client，向 Server 申请 Token
3. 支持全局 QPS 阈值和 Namespace 隔离
4. 缺点：引入额外网络开销，Server 单点需高可用

```yaml
# Token Client 配置
sentinel:
  cluster:
    client:
      server-host: localhost
      server-port: 18730
      request-timeout: 20
```
</details>

**Q8**：Sentinel Push 模式规则持久化的完整流程是什么？

<details>
<summary>答案</summary>
1. Dashboard 接收用户配置的规则
2. Dashboard 通过 HTTP API 将规则写入 Nacos（或其他配置中心）
3. Nacos 配置变更后推送通知给所有监听的应用
4. 应用内的 NacosDataSource 监听配置变更
5. NacosDataSource 解析 JSON 并调用 RuleManager.loadRules()
6. 规则实时更新到 Sentinel 内存中
</details>

**Q9**：Sentinel 的 ProcessorSlotChain 责任链中各 Slot 的执行顺序和作用？

<details>
<summary>答案</summary>
```text
NodeSelectorSlot → 构建调用树节点
ClusterBuilderSlot → 集群节点统计
LogSlot → 限流日志打印
StatisticSlot → 滑动窗口指标统计
AuthoritySlot → 黑白名单校验
SystemSlot → 系统自适应保护
FlowSlot → 流量控制规则检查
DegradeSlot → 熔断降级规则检查
```
每个 Slot 有各自职责，按优先级顺序执行，前一个执行失败则抛异常中断后续。
</details>

**Q10**：如何自定义 Sentinel 的异常处理逻辑，使被限流的请求返回统一的业务 JSON？

<details>
<summary>答案</summary>
实现 `BlockExceptionHandler` 接口（Spring Cloud Alibaba）或 `UrlBlockHandler` 接口（原生），配置返回 JSON 格式的响应。参考章节 5.3.7 的完整实现。
</details>

### 6.3 精通习题

**Q11**：设计一个高并发电商系统的 Sentinel 流量治理方案，包含网关、商品、订单、库存服务。请画出架构图并说明各层保护策略。

<details>
<summary>答案</summary>
方案设计如下：

```
API Gateway (Spring Cloud Gateway + Sentinel)
├── 全局流控: 入口 QPS=10000, CPU>80% 触发保护
├── 路由级流控: /product/** QPS=5000, /order/** QPS=3000, /inventory/** QPS=2000

Product Service
├── 热点限流: 爆款商品 ID 单独配置高 QPS 阈值
├── 熔断: 慢调用比例 >30% 熔断 30s
├── 线程数隔离: 查询线程池最大 50

Order Service
├── 排队等待: 下单接口 QPS=1000, 排队等待削峰
├── 熔断: 异常比例 >20% 熔断 60s
├── 降级: 下单失败返回"稍后重试"

Inventory Service
├── 快速失败: 扣减库存 QPS=500
├── 关联限流: 库存查询关联扣减限流
├── 信号量隔离: 最多 20 个并发扣减
```

关键设计点：
1. 网关层拦截大部分恶意流量
2. 服务间 Feign 调用开启 Sentinel 保护
3. 使用 Nacos 统一管理规则
4. 告警：秒级监控 + 飞书/钉钉通知
</details>

**Q12**：Sentinel 的 ProcessorSlotChain 如何通过 SPI 进行扩展？如果需要自定义一个 Slot 实现安全校验，应该怎么做？

<details>
<summary>答案</summary>
扩展步骤：
1. 实现 `ProcessorSlot` 接口
2. 在 `META-INF/services/com.alibaba.csp.sentinel.slotchain.ProcessorSlot` 中注册
3. 通过 `SlotChainBuilder` SPI 接口自定义整体链构建

```java
// 1. 自定义 Slot
public class SecuritySlot extends AbstractLinkedProcessorSlot<DefaultNode> {
    @Override
    public void entry(Context context, ResourceWrapper resourceWrapper,
                      DefaultNode node, int count, boolean prioritized, Object... args)
                      throws Throwable {
        // 安全校验逻辑
        String origin = context.getOrigin();
        if (isBlocked(origin)) {
            throw new AuthorityException(origin);
        }
        fireEntry(context, resourceWrapper, node, count, prioritized, args);
    }

    @Override
    public void exit(Context context, ResourceWrapper resourceWrapper,
                     int count, Object... args) {
        fireExit(context, resourceWrapper, count, args);
    }
}

// 2. SPI 配置 META-INF/services/com.alibaba.csp.sentinel.slotchain.ProcessorSlot
// com.example.sentinel.extension.SecuritySlot
```
</details>

**Q13**：在 Sentinel 中如何实现动态限流阈值？例如根据 Redis 中的实时指标动态调整 QPS。

<details>
<summary>答案</summary>
实现方案：

```java
@Component
public class DynamicFlowRuleManager {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @PostConstruct
    public void startDynamicAdjust() {
        scheduler.scheduleAtFixedRate(this::adjustRules, 0, 5, TimeUnit.SECONDS);
    }

    private void adjustRules() {
        // 从 Redis 获取实时系统指标
        double cpuUsage = Double.parseDouble(
            redisTemplate.opsForValue().get("metrics:cpu:usage"));
        double currentQps = Double.parseDouble(
            redisTemplate.opsForValue().get("metrics:qps:current"));

        // 动态计算阈值
        double newThreshold;
        if (cpuUsage > 0.8) {
            newThreshold = currentQps * 0.8; // 降低阈值
        } else if (cpuUsage < 0.5) {
            newThreshold = currentQps * 1.2; // 提高阈值
        } else {
            return; // 维持不变
        }

        // 更新规则
        List<FlowRule> rules = FlowRuleManager.getRules();
        for (FlowRule rule : rules) {
            if ("getProduct".equals(rule.getResource())) {
                rule.setCount(newThreshold);
            }
        }
        FlowRuleManager.loadRules(rules);

        System.out.printf("[Dynamic] Adjusted threshold to %.2f (CPU=%.2f%%)%n",
                newThreshold, cpuUsage * 100);
    }
}
```
</details>

**Q14**：在微服务架构中，如何避免 Sentinel 规则过多带来性能开销？

<details>
<summary>答案</summary>
1. **资源粒度控制**：避免过细粒度的资源定义，如 `getProduct-{id}`，应使用 `ProductService:getProduct`
2. **缓存热点规则**：热点参数限流使用 LRU 缓存，避免参数组合爆炸
3. **规则合并**：同一资源的多个规则合并，减少检查次数
4. **异步化统计**：Sentinel 的统计默认异步（批量写入），避免高频锁竞争
5. **关闭不必要的 Slot**：通过 SPI 自定义 SlotChain，移除不必要的 Slot
6. **控制规则数量**：单机建议不超过 1000 条规则
7. **采样统计**：压测模式下可选择采样统计而非全量统计
</details>

---

## 7. 本章复盘速记清单

### 7.1 核心概念速记

| 概念 | 一句话记住 |
|------|-----------|
| **资源（Resource）** | 被保护的对象，`SphU.entry("name")` 定义 |
| **规则（Rule）** | 控制逻辑的配置，流控/降级/热点/系统/授权 |
| **Slot 链** | 责任链模式，8 个 Slot 顺序执行检查 |
| **滑动窗口** | LeapArray 数组，500ms 粒度统计 |
| **Context** | 调用链上下文，含入口和来源 |

### 7.2 流控规则速记

```text
┌─ 限流模式: QPS / THREAD
├─ 流控策略: DIRECT / RELATE / CHAIN
├─ 流控效果: DEFAULT(快速失败) / WARM_UP(预热) / RATE_LIMITER(排队)
└─ 来源控制: limitApp 字段
```

### 7.3 熔断规则速记

```text
┌─ 慢调用比例: RT > count → slowRatioThreshold 比例触发
├─ 异常比例:  异常数/总请求 > count 触发
├─ 异常数:    1min 内异常数 > count 触发
├─ 熔断时长:  timeWindow 秒后进入 HALF_OPEN
└─ 最小请求数: minRequestAmount >= 阈值才触发统计
```

### 7.4 @SentinelResource 速记

```text
@SentinelResource(
    value = "资源名",
    fallback = "业务异常兜底",     // 参数 + Throwable
    blockHandler = "限流熔断兜底",  // 参数 + BlockException
    exceptionsToIgnore = {}       // 忽略的异常
)
```

### 7.5 规则持久化速记

```text
原始模式  → 内存，重启丢失 ❌
Pull 模式 → 定时拉取，有延迟 ⚠️
Push 模式 → Nacos/ZK/Apollo 推送，实时生效 ✅
```

### 7.6 常见问题速查

| 问题 | 解决方案 |
|------|----------|
| @SentinelResource 不生效 | 注册 `SentinelResourceAspect` 切面 |
| Dashboard 无数据 | 开启 `eager: true`，检查端口 8719 |
| 规则重启丢失 | 配置 Nacos/APOLLO 持久化 |
| 限流不精确 | 滑动窗口误差 ±5%，可接受 |
| Windows Load 规则无效 | 改用 CPU 规则替代 |
| Feign 未保护 | 配置 `feign.sentinel.enabled: true` |

### 7.7 学习路径总结

```
理解资源概念 → 掌握 4 种规则 → 熟练注解使用
     ↓
搭建 Dashboard → 配置 Nacos 持久化 → Push 模式
     ↓
自定义异常处理 → 压测验证 → 生产调优
     ↓
深入源码：SlotChain + 滑动窗口 → SPI 扩展
```

---

## 8. 精通拓展补充-P2

### 8.1 Sentinel 与 Hystrix / Resilience4j 深度对比

#### 8.1.1 功能矩阵

| 功能维度 | Sentinel 1.8.6 | Hystrix 1.5.x | Resilience4j 2.x |
|----------|---------------|---------------|-------------------|
| **开源方** | Alibaba | Netflix | 社区 |
| **维护状态** | 活跃维护 | 维护模式（Hystrix 已停止迭代） | 活跃维护 |
| **限流模式** | QPS / 线程数 / 热点 / 系统 | 线程池 / 信号量 | 基础限流（RateLimiter） |
| **限流效果** | 快速失败 / Warm-Up / 排队 / 组合 | 无 | 等待 / 快速失败 |
| **熔断策略** | 慢调用 / 异常比例 / 异常数 | 异常比例 | 慢调用 / 异常比例 / 自定义 |
| **熔断状态** | CLOSED → OPEN → HALF_OPEN | CLOSED → OPEN → HALF_OPEN | CLOSED → OPEN → HALF_OPEN |
| **系统保护** | Load / CPU / RT / 线程数 / QPS | 无 | 无 |
| **热点限流** | 原生支持 | 无 | 无 |
| **实时监控** | Dashboard 控制台 | Turbine + Hystrix Dashboard | Micrometer + Prometheus |
| **动态配置** | Nacos / Apollo / ZK | 不支持 | 支持（通过 Spring Cloud） |
| **规则持久化** | 多数据源 | 不支持 | 需自行实现 |
| **适配框架** | Spring Cloud Alibaba | Spring Cloud Netflix | Spring Cloud Circuit Breaker |
| **学习成本** | 中 | 低 | 中 |
| **生产验证** | 双十一大规模验证 | 大规模验证 | 社区验证 |

#### 8.1.2 性能对比

| 指标 | Sentinel | Hystrix | Resilience4j |
|------|----------|---------|--------------|
| 单机 QPS（简单限流） | ~500,000 | ~200,000 | ~400,000 |
| 内存开销（100 条规则） | ~10MB | ~30MB | ~8MB |
| 统计延迟 | 毫秒级（滑动窗口） | 秒级（滚动窗口） | 毫秒级（Ring Buffer） |
| 限流精度 | ±5% | ±10% | ±3% |

#### 8.1.3 选型建议

| 场景 | 推荐 | 理由 |
|------|------|------|
| 阿里云/Spring Cloud Alibaba 生态 | **Sentinel** | 原生集成，支持最丰富 |
| 非阿里云 Spring Cloud 生态 | **Resilience4j** | 轻量，无外部依赖 |
| 已有 Hystrix 的存量项目 | 过渡到 **Resilience4j** | Hystrix 已停止维护 |
| 需要热点参数限流 | **Sentinel** | 唯一原生支持 |
| 需要系统自适应保护 | **Sentinel** | 唯一支持 Load/CPU 保护 |
| 需要 Dashboard 实时监控 | **Sentinel** | 开箱即用控制台 |

> 💡 业界趋势：Hystrix 已进入维护模式，新项目在 Alibaba 生态选 Sentinel，非 Alibaba 生态选 Resilience4j。

### 8.2 Sentinel 源码核心类解读

#### 8.2.1 CtSph — 核心入口

```java
// CtSph 是 Sph 的默认实现，所有资源调用都经过此方法
public class CtSph implements Sph {

    @Override
    public Entry entry(String name, EntryType type, int count, Object... args) throws BlockException {
        // 1. 将资源名包装为 ResourceWrapper
        ResourceWrapper resource = new StringResourceWrapper(name, type);

        // 2. 获取或创建 ProcessorSlotChain
        ProcessorSlotChain chain = lookProcessChain(resource);

        // 3. 创建 Entry 并执行 SlotChain
        Entry entry = new CtEntry(resource, chain, context);
        chain.entry(context, resource, null, count, false, args);

        return entry;
    }

    // 缓存 SlotChain，重复利用
    private volatile Map<ResourceWrapper, ProcessorSlotChain> chainMap = new ConcurrentHashMap<>();
}
```

#### 8.2.2 LeapArray — 滑动窗口

```java
public abstract class LeapArray<T> {

    // 窗口总时长（毫秒）
    protected int windowLengthInMs;
    // 子窗口数量
    protected int sampleCount;
    // 时间间隔（毫秒）
    protected int intervalInMs;

    // 窗口数组
    protected final AtomicReferenceArray<WindowWrap<T>> array;

    /**
     * 根据时间戳获取当前窗口
     * 核心逻辑：取模定位 + 时间对齐
     */
    public WindowWrap<T> currentWindow(long timeMillis) {
        // 1. 计算时间戳对应的桶索引
        int idx = calculateTimeIdx(timeMillis);
        // 2. 计算该桶的开始时间
        long windowStart = calculateWindowStart(timeMillis);

        while (true) {
            WindowWrap<T> old = array.get(idx);
            if (old == null) {
                // 桶为空，创建新桶
                WindowWrap<T> window = new WindowWrap<>(windowLengthInMs, windowStart, newEmptyBucket(timeMillis));
                if (array.compareAndSet(idx, null, window)) {
                    return window;
                }
                Thread.yield();
            } else if (windowStart == old.windowStart()) {
                // 时间对齐，返回当前桶
                return old;
            } else if (windowStart > old.windowStart()) {
                // 桶过期，重置
                if (array.compareAndSet(idx, old, new WindowWrap<>(windowLengthInMs, windowStart, newEmptyBucket(timeMillis)))) {
                    return array.get(idx);
                }
                Thread.yield();
            } else {
                return old;
            }
        }
    }
}
```

#### 8.2.3 FlowSlot — 流控核心

```java
public class FlowSlot extends AbstractLinkedProcessorSlot<DefaultNode> {

    @Override
    public void entry(Context context, ResourceWrapper resourceWrapper,
                      DefaultNode node, int count, boolean prioritized, Object... args)
                      throws Throwable {
        checkFlow(resourceWrapper, context, node, count, prioritized);

        fireEntry(context, resourceWrapper, node, count, prioritized, args);
    }

    void checkFlow(ResourceWrapper resource, Context context, DefaultNode node,
                   int count, boolean prioritized) throws FlowException {
        // 获取该资源的所有流控规则
        List<FlowRule> rules = FlowRuleManager.getRules();
        if (rules == null || rules.isEmpty()) {
            return;
        }

        // 遍历规则，逐个检查
        for (FlowRule rule : rules) {
            if (!rule.isApplicable(resource.getName())) {
                continue;
            }
            // 流量控制器检查
            if (rule.getRater().canPass(node, count, prioritized)) {
                continue;
            }
            // 限流触发
            throw new FlowException(rule.getLimitApp(), rule);
        }
    }
}
```

### 8.3 Sentinel 与 Spring Cloud Gateway 集成

#### 8.3.1 网关级限流

```xml
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-alibaba-sentinel-gateway</artifactId>
</dependency>
```

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: product-route
          uri: lb://product-service
          predicates:
            - Path=/product/**
        - id: order-route
          uri: lb://order-service
          predicates:
            - Path=/order/**
    sentinel:
      filter:
        enabled: true
      scg:
        fallback:
          mode: response
          response-status: 429
          response-body: '{"code":429,"message":"网关限流"}'
```

#### 8.3.2 网关 API 分组流控

```java
@Configuration
public class GatewaySentinelConfig {

    @PostConstruct
    public void initGatewayRules() {
        // 按 API 分组限流
        Set<ApiDefinition> apiDefinitions = new HashSet<>();

        // 商品 API 分组
        ApiDefinition productApi = new ApiDefinition("product_api")
            .setPredicateItems(new HashSet<ApiPredicateItem>() {{
                add(new ApiPathPredicateItem().setPattern("/product/**")
                    .setMatchStrategy(SentinelGatewayConstants.PARAM_MATCH_STRATEGY_PREFIX));
            }});
        apiDefinitions.add(productApi);

        // 订单 API 分组
        ApiDefinition orderApi = new ApiDefinition("order_api")
            .setPredicateItems(new HashSet<ApiPredicateItem>() {{
                add(new ApiPathPredicateItem().setPattern("/order/**")
                    .setMatchStrategy(SentinelGatewayConstants.PARAM_MATCH_STRATEGY_PREFIX));
            }});
        apiDefinitions.add(orderApi);

        GatewayApiDefinitionManager.loadApiDefinitions(apiDefinitions);

        // 为分组配置流控规则
        Set<GatewayFlowRule> gatewayRules = new HashSet<>();
        gatewayRules.add(new GatewayFlowRule("product_api")
            .setCount(5000)
            .setIntervalSec(1));
        gatewayRules.add(new GatewayFlowRule("order_api")
            .setCount(3000)
            .setIntervalSec(1));

        GatewayRuleManager.loadRules(gatewayRules);
    }
}
```

### 8.4 Sentinel 与 OpenFeign 集成

#### 8.4.1 配置

```yaml
# 开启 Feign 的 Sentinel 支持
feign:
  sentinel:
    enabled: true

# 配置 Sentinel 降级后的处理
spring:
  cloud:
    sentinel:
      feign:
        enabled: true
```

#### 8.4.2 Feign 降级实现

```java
// Feign 客户端
@FeignClient(
    name = "product-service",
    path = "/product",
    fallbackFactory = ProductFeignFallbackFactory.class
)
public interface ProductFeignClient {

    @GetMapping("/{id}")
    Result<Product> getProduct(@PathVariable("id") Long id);

    @GetMapping("/search")
    Result<List<Product>> searchProduct(@RequestParam("keyword") String keyword);
}

// 降级工厂
@Component
public class ProductFeignFallbackFactory implements FallbackFactory<ProductFeignClient> {

    @Override
    public ProductFeignClient create(Throwable cause) {
        return new ProductFeignClient() {
            @Override
            public Result<Product> getProduct(Long id) {
                System.err.println("[Feign Fallback] getProduct failed, id=" + id + ", cause=" + cause.getMessage());
                return Result.error(503, "商品服务熔断，请稍后重试");
            }

            @Override
            public Result<List<Product>> searchProduct(String keyword) {
                System.err.println("[Feign Fallback] searchProduct failed, keyword=" + keyword);
                return Result.error(503, "商品搜索服务不可用");
            }
        };
    }
}
```

### 8.5 Sentinel 生产环境最佳实践

#### 8.5.1 规则管理

| 实践 | 说明 |
|------|------|
| **配置中心统一管理** | 使用 Nacos 管理所有规则，禁止直接修改应用本地规则 |
| **规则版本控制** | Nacos 配置支持版本回滚，规则变更可追溯 |
| **灰度发布规则** | 先灰度一台验证，再全量推送 |
| **规则定期评审** | 每季度评审规则配置，移除冗余规则 |
| **规则备份** | 定期导出规则 JSON 到 Git 仓库 |

#### 8.5.2 集群部署配置

```yaml
# 生产环境建议配置
spring:
  cloud:
    sentinel:
      transport:
        dashboard: sentinel-dashboard.prod:8080
        port: 8719
        heartbeat-interval-ms: 5000  # 心跳间隔
      eager: true
      # 日志配置
      log:
        dir: /data/logs/sentinel
        switch-file: /data/logs/sentinel/switch
      # 指标配置
      metric:
        charset: UTF-8
        file-single-size: 52428800    # 50MB 单个文件
        file-total-count: 6           # 保留 6 个文件
```

#### 8.5.3 监控告警

```java
@Component
public class SentinelMetricsCollector {

    private final MeterRegistry meterRegistry;

    public SentinelMetricsCollector(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Scheduled(fixedDelay = 10000)
    public void collectMetrics() {
        // 收集每个资源的通过/拦截数据
        Map<ResourceWrapper, ClusterNode> resourceMap = ClusterBuilderSlot.getClusterNodeMap();
        for (Map.Entry<ResourceWrapper, ClusterNode> entry : resourceMap.entrySet()) {
            String resource = entry.getKey().getName();
            ClusterNode node = entry.getValue();

            // 通过 QPS
            meterRegistry.gauge("sentinel.pass.qps",
                Tags.of("resource", resource),
                node,
                ClusterNode::passQps);

            // 拦截 QPS
            meterRegistry.gauge("sentinel.block.qps",
                Tags.of("resource", resource),
                node,
                ClusterNode::blockQps);

            // 异常数
            meterRegistry.gauge("sentinel.exception.qps",
                Tags.of("resource", resource),
                node,
                ClusterNode::exceptionQps);

            // 平均 RT
            meterRegistry.gauge("sentinel.avg.rt",
                Tags.of("resource", resource),
                node,
                n -> n.avgRt());
        }

        // 收集系统指标
        meterRegistry.gauge("sentinel.system.load",
            Tags.empty(),
            System,
            s -> s.getSystemLoad());
    }
}
```

### 8.6 Sentinel 与阿里云 AHAS

#### 8.6.1 AHAS 简介

AHAS（Application High Availability Service）是阿里云提供的商业化高可用服务，底层基于 Sentinel，提供：

| 功能 | AHAS | 开源 Sentinel |
|------|------|---------------|
| 控制台 | 云上托管，免运维 | 自行部署 |
| 规则存储 | 云上持久化 | 需自建 Nacos/ZK |
| 告警通知 | 短信/电话/钉钉/飞书 | 需自建告警 |
| 集群限流 | 托管 Token Server | 自建 Token Server |
| 流量演练 | 混沌工程集成 | 不支持 |
| 成本 | 付费 | 免费 |

#### 8.6.2 AHAS 接入配置

```yaml
spring:
  cloud:
    sentinel:
      datasource:
        ds-ahas:
          ahas:
            namespace: default
            license: your-ahas-license
      transport:
        dashboard: ahas.aliyuncs.com:443  # AHAS 控制台地址
```

### 8.7 Sentinel 进阶场景

#### 8.7.1 基于调用链路的限流

```java
// 场景：只对来自 "user-service" 的请求限流
@SentinelResource(value = "order:create")
public Order createOrder(OrderDTO dto) {
    // 默认 EntryType.OUT
    // 当 limitApp = "user-service" 时只限制 user-service 来源
}

// 在 Controller 设置调用来源
@GetMapping("/order/create")
public Result<Order> createOrder(@RequestBody OrderDTO dto) {
    // 设置来源上下文
    ContextUtil.enter("myContext", "user-service");
    try {
        Order order = orderService.createOrder(dto);
        return Result.success(order);
    } finally {
        ContextUtil.exit();
    }
}
```

#### 8.7.2 自适应流量控制

```java
/**
 * 自适应流控：根据成功率为基准动态调整阈值
 */
@Component
public class AdaptiveFlowController {

    private static final double MIN_SUCCESS_RATIO = 0.9;

    @Scheduled(fixedDelay = 5000)
    public void adaptiveControl() {
        for (Map.Entry<ResourceWrapper, ClusterNode> entry :
                ClusterBuilderSlot.getClusterNodeMap().entrySet()) {

            String resource = entry.getKey().getName();
            ClusterNode node = entry.getValue();

            long pass = node.passQps();
            long block = node.blockQps();
            long exception = node.exceptionQps();
            long success = pass - exception;

            if (pass == 0) continue;

            double successRatio = (double) success / pass;

            // 如果成功率低于阈值，收紧限流
            if (successRatio < MIN_SUCCESS_RATIO) {
                List<FlowRule> rules = FlowRuleManager.getRules();
                for (FlowRule rule : rules) {
                    if (rule.getResource().equals(resource)) {
                        double newCount = rule.getCount() * 0.9; // 降低 10%
                        rule.setCount(Math.max(newCount, 10));    // 最低阈值 10
                        System.out.printf("[Adaptive] Tighten %s: %.2f -> %.2f (successRatio=%.2f)%n",
                                resource, rule.getCount(), newCount, successRatio);
                    }
                }
                FlowRuleManager.loadRules(rules);
            }
        }
    }
}
```

#### 8.7.3 全链路压测流量标记

```java
/**
 * 链路压测：通过 Sentinel 的 Origin 标记压测流量
 */
@Aspect
@Component
public class StressTestOriginAspect {

    @Around("@annotation(stressTest)")
    public Object setStressOrigin(ProceedingJoinPoint pjp, StressTest stressTest) throws Throwable {
        // 标记流量来源为压测
        ContextUtil.enter("stress_test", "stress_origin");
        try {
            return pjp.proceed();
        } finally {
            ContextUtil.exit();
        }
    }
}

// 压测专用的限流规则
// resource: "order:create", limitApp: "stress_origin", count: 5000.0
// 正常流量：limitApp: "default", count: 1000.0
```

### 8.8 拓展阅读与学习资源

#### 8.8.1 官方资源

| 资源 | 地址 |
|------|------|
| Sentinel 官方文档 | https://sentinelguard.io/zh-cn/docs/introduction.html |
| GitHub 源码 | https://github.com/alibaba/Sentinel |
| Spring Cloud Alibaba 文档 | https://spring-cloud-alibaba-group.github.io/github-pages/ |
| AHAS 产品页 | https://www.aliyun.com/product/ahas |

#### 8.8.2 推荐书籍与文章

- 《Spring Cloud Alibaba 微服务实战》— Sentinel 章节
- 《深入理解 Sentinel》— 源码分析系列
- Sentinel 官方 Wiki — 最佳实践

#### 8.8.3 进阶方向

| 方向 | 内容 |
|------|------|
| **源码分析** | Sentinel 核心源码、SPI 扩展机制、滑动窗口实现 |
| **集群限流** | Token Server/Client 原理、高可用部署 |
| **规则管理平台** | 自建规则管理界面、可视化规则设计器 |
| **混沌工程** | Sentinel + ChaosBlade 注入故障验证 |
| **多语言扩展** | Sentinel Go / Sentinel C++ / Sentinel Node.js |

---

> 🎯 **本章总结**：Sentinel 是微服务体系中最核心的高可用组件。从基础的流控熔断，到热点限流、系统保护，再到规则持久化和 Nacos 集成，构成了完整的服务保护方案。在生产环境中，建议结合压测数据动态调整规则，并通过监控告警体系确保及时发现异常。

> 💡 **最佳实践**：先压测摸清系统水位，再配置规则，最后上线观察。规则配置要"宁严勿松"，随着系统稳定逐步放宽阈值。
