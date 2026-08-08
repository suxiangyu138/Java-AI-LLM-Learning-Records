# 04 核心原理：Supplier 链与响应式实现

> 原理篇：**一次"服务名调用"的完整内部旅程——上下文创建、Supplier 链组装、实例选择、地址替换**——理解 LoadBalancer 的响应式核心与上下文隔离机制；面试核心区，对照 [Gateway 系列](../Spring Cloud Gateway/06-核心原理：WebFlux 处理链路.md) 的 lb:// 链路。

---

## 📚 目录

1. [调用旅程总览](#1-调用旅程总览)
2. [上下文模型：per-service 隔离](#2-上下文模型per-service-隔离)
3. [Supplier 链：组合与执行顺序](#3-supplier-链组合与执行顺序)
4. [实例选择：ReactiveLoadBalancer 内部](#4-实例选择reactiveloadbalancer-内部)
5. [地址替换：请求变换与执行](#5-地址替换请求变换与执行)
6. [响应式实现：为什么是 Mono/Flux](#6-响应式实现为什么是-monoflux)
7. [vs Ribbon：原理对比](#7-vs-ribbon原理对比)

---

## 1. 调用旅程总览

```text
一次 http://payment-service/api/hello 的完整旅程（RestTemplate + @LoadBalanced）：

调用方
  │
  ▼
① LoadBalancerInterceptor（拦截器）
  │    识别服务名 URL → 交给 BlockingLoadBalancerClient
  ▼
② LoadBalancerClientFactory.getLazyProvider("payment-service", ...)
  │    ★ 按服务名取该服务的 LB 上下文（子上下文）
  ▼
③ ServiceInstanceListSupplier 链（响应式）
  │    DiscoveryClient 拉列表 → Caffeine 缓存 → 过滤（策略）→ Flux 输出
  ▼
④ ReactiveLoadBalancer.choose()（如 RoundRobinLoadBalancer）
  │    从候选列表选一个实例（原子计数/权重/随机...）
  ▼
⑤ 地址替换：http://payment-service → http://192.168.1.10:8082
  │    LoadBalancerClientRequestTransformer 可改写请求
  ▼
⑥ 真实 HTTP 调用（RestTemplate/Netty）
  ▼
下游实例
```

> 🎯 一句话本质：**LoadBalancer = "服务名 → 实例"的解析器**——拦截器负责发现服务名，Supplier 链负责拿候选列表，LoadBalancer 负责选一个，最后把 URL 里的服务名替换成真实地址。

## 2. 上下文模型：per-service 隔离

### 2.1 子上下文机制

```text
LoadBalancer 为每个服务创建独立的子上下文（Child Context）：
  └── 调用 order-service → 创建 order-service 的 LB 上下文（含它自己的 Supplier 链/策略 Bean）
  └── 调用 payment-service → 独立上下文（自己的链/策略）

为什么隔离：
  ├── 每个服务的策略可独立配置（A 服务轮询、B 服务权重）
  ├── 实例列表缓存独立（互不干扰）
  └── 服务级配置（spring.cloud.loadbalancer.clients.<id>.*）有专属作用域
```

### 2.2 懒加载与预加载

```yaml
spring:
  cloud:
    loadbalancer:
      eager-load:
        clients: payment-service,order-service   # ★ 预加载（默认懒加载）
```

```text
懒加载（默认）：第一次调用该服务才创建上下文（首次调用慢几十 ms）
预加载：启动时创建指定服务的上下文（首次调用无冷启动）
```

> 💡 首调延迟敏感（网关/入口服务）用 eager-load 预加载，避免"第一个请求慢"。

## 3. Supplier 链：组合与执行顺序

### 3.1 链式模型

```text
ServiceInstanceListSupplier 是响应式数据流（Flux<ServiceInstance>）：
  [DiscoveryClient] → [Caching] → [ZonePreference] → [Weighted] → 输出候选列表
     原始列表            缓存         过滤区域            加权

每个 supplier：拿上游的 Flux → 变换 → 传给下游（装饰器模式）
```

```java
// builder 生成链
@ServiceInstanceListSupplier.builder()
        .withDiscoveryClient()          // 源：注册中心
        .withCaching()                  // 装饰：缓存
        .withZonePreference()           // 装饰：区域过滤
        .withWeighted()                 // 装饰：权重
        .build(context);
```

### 3.2 放置规则（官方明确）

| 规则 | 说明 |
|------|------|
| 缓存/健康检查必须紧跟"网络获取"supplier 后 | 缓存的是"注册中心原始列表"，过滤型 supplier 不参与缓存 |
| 过滤型 supplier（权重/区域/提示）在缓存之后 | 每次请求都过滤（缓存列表 + 实时过滤） |
| Caching 与 HealthCheck 二选一 | 两者都有"缓存"语义，双包是重复（[05 篇](05-缓存与健康检查.md)） |

> 🎯 面试必答：**"为什么要 Supplier 链而不是一个接口？"**——**单一职责 + 自由组合**：发现、缓存、过滤、选择各管一段，策略差异 = 链的组合差异——新增策略只需新增一个 supplier（开闭原则），Ribbon 的 Rule 体系则难扩展。

## 4. 实例选择：ReactiveLoadBalancer 内部

### 4.1 接口与实现

```java
public interface ReactiveLoadBalancer<T> {
    Mono<Response<T>> choose(Request request);   // ★ 核心方法：选一个
}
// 实现：RoundRobinLoadBalancer / RandomLoadBalancer
// Response<T> 里封装：getServer()（选中的实例）/ 空响应（无实例）
```

### 4.2 轮询实现细节

```text
RoundRobinLoadBalancer：
  ├── AtomicInteger 计数器 + 列表大小取模（nextIndex = counter.getAndIncrement() % size）
  ├── 列表为空 → 返回空 Response（调用方抛 No instances available）
  ├── 并发安全（原子计数）
  └── 响应式：整个选择在 Mono 里（非阻塞）
```

### 4.3 实例元数据（metadata）的作用

```text
ServiceInstance 携带 metadata（注册时写入）：
  ├── weight → WeightedSupplier 读取
  ├── zone → ZonePreference 读取
  ├── hint → HintSupplier 读取
  ├── api-version → ApiVersionSupplier 读取
  ├── secure（是否 https）→ 请求变换时升级协议
  └── 策略差异的本质 = 读 metadata 的方式不同
```

> 💡 面试加分：**"策略之间为什么能任意组合？"——所有策略都是"读 metadata + 过滤/加权"**——metadata 是策略的输入协议，组合因此正交。

## 5. 地址替换：请求变换与执行

### 5.1 完整替换流程

```text
原始：http://payment-service/api/hello
  │
  ├── BlockingLoadBalancerClient 拿到选中的 ServiceInstance（ip:8082）
  ├── 构造 ServiceRequestWrapper：URI 替换 → http://192.168.1.10:8082/api/hello
  ├── LoadBalancerRequestTransformer 链（可选改写）
  └── 执行请求
```

```java
// ServiceRequestWrapper 是核心：包装原始请求、覆盖 URI
class ServiceRequestWrapper extends HttpRequestWrapper {
    ServiceInstance instance;
    @Override public URI getURI() {
        return new URI(instance.getScheme(), null, instance.getHost(),
                       instance.getPort(), super.getURI().getPath(), ...);
    }
}
```

### 5.2 协议升级（secure 语义）

```text
选中实例 metadata.secure=true → 请求自动升级为 https
  ├── 注册中心标记安全实例（如启用 TLS 的下游）
  └── 混合协议场景（部分服务 https）自动适配
```

## 6. 响应式实现：为什么是 Mono/Flux

### 6.1 核心 API 全响应式

```text
ServiceInstanceListSupplier → Flux<ServiceInstance>（列表是数据流）
ReactiveLoadBalancer.choose() → Mono<Response>（选择是异步）
  ├── 注册中心查询可以是异步（DiscoveryClient 响应式）
  ├── 健康检查是持续流（Flux.interval 周期探测）
  └── 缓存是响应式缓存（Caffeine 异步封装）
```

### 6.2 阻塞与响应式的统一

| 场景 | 使用 | 内部 |
|------|------|------|
| RestTemplate/RestClient | BlockingLoadBalancerClient | 阻塞包装（block()） |
| WebClient | ReactorLoadBalancerExchangeFilterFunction | 原生响应式 |
| Feign | FeignBlockingLoadBalancerClient | 阻塞包装 |
| 直接调用 | ReactiveLoadBalancer.choose() | 原生 |

> 🎯 面试必答：**"为什么 LoadBalancer 是响应式的？"**——**注册中心的查询与健康检查天然是"流"**（实例会变、健康会变），响应式 API（Flux/Mono）让"列表→过滤→选择"全链路非阻塞；阻塞客户端（RestTemplate/Feign）通过内部 block() 适配——**一个响应式核心，多种客户端形态**。

## 7. vs Ribbon：原理对比

| 维度 | LoadBalancer | Ribbon（废弃） |
|------|-------------|----------------|
| 核心模型 | **Supplier 链（组合式）** | Rule + ILoadBalancer（继承式） |
| 选择 API | ReactiveLoadBalancer.choose() | ServerList / chooseServer() |
| 列表获取 | DiscoveryClientSupplier | ServerList 接口 |
| 缓存 | **内置 Caffeine（TTL 35s）** | 无内置（靠注册中心客户端缓存） |
| 响应式 | ✅ 原生 | ❌ |
| 健康检查 | ✅ 内置 supplier | ⚠️ 依赖注册中心 |
| 配置 | spring.cloud.loadbalancer.* | ribbon.*（已移除） |
| 维护 | ✅ 官方活跃 | 2018 停止 |
| 扩展 | 新增 supplier（开闭原则） | 继承 Rule（重） |

> 🎯 面试必答：**"LoadBalancer 和 Ribbon 原理上什么区别？"**——**三差**：**① 模型**（Ribbon 继承式 Rule，LoadBalancer 组合式 Supplier 链——新策略 = 新 supplier 而非改基类）；**② 响应式**（LoadBalancer 全链路 Mono/Flux，Ribbon 同步阻塞）；**③ 缓存**（LoadBalancer 内置 Caffeine 35s 缓存防注册中心压力，Ribbon 无）——所以 Ribbon 被官方放弃不是"功能缺失"，是**架构模型过时**。

---

**下一模块**：[05-缓存与健康检查](05-缓存与健康检查.md)　**返回总览**：[00-Spring Cloud LoadBalancer知识体系总览](00-Spring Cloud LoadBalancer知识体系总览.md)

**【参考来源】**：[Spring Cloud LoadBalancer 5.0.2 官方文档（Spring Cloud Commons）](https://docs.spring.io/spring-cloud-commons/reference/spring-cloud-commons/loadbalancer.html)
