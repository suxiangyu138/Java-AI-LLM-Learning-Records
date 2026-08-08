# 07-Spring Cloud 源码导读
> 微服务四组件源码入口：Nacos 注册/订阅、OpenFeign 动态代理、Gateway 路由定位、Stream Binder 绑定——每个只读"入口到结论"一条链

## 📚 目录
1. [阅读策略：四组件各读一条链](#1-阅读策略四组件各读一条链)
2. [Nacos 客户端源码](#2-nacos-客户端源码)
3. [OpenFeign 源码](#3-openfeign-源码)
4. [Spring Cloud Gateway 源码](#4-spring-cloud-gateway-源码)
5. [Spring Cloud Stream 源码](#5-spring-cloud-stream-源码)
6. [四个"入口类"速查表](#6-四个入口类速查表)
7. [核心要点](#7-核心要点)
8. [参考来源](#8-参考来源)

## 1. 阅读策略：四组件各读一条链

| 组件 | 只读一条链 | 输出 |
|------|-----------|------|
| Nacos | "应用启动 → 注册成功 → 收到服务变更" | 注册/心跳/订阅三张时序 |
| OpenFeign | "注入 FeignClient → 调用 → 发 HTTP" | 代理生成与调用链 |
| Gateway | "请求进入 → 路由命中 → 过滤器链 → 转发" | 定位与过滤链 |
| Stream | "定义函数 → 绑定创建 → 消费消息" | 绑定生命周期 |

> 💡 Spring Cloud 源码体量巨大，**全读不现实**。策略 = 每组件一条完整主链 + 入口类速查表（第 6 节），遇到问题再按表打点。

## 2. Nacos 客户端源码

### 2.1 注册链路（以 Nacos 3.x 客户端为例）

```text
应用启动
 → NacosServiceRegistryAutoConfiguration（自动配置）
 → NacosServiceRegistry.register（Spring Cloud Commons 接口实现）
      └─ nacosClient.registerInstance（NacosNamingService）
           └─ 发送注册请求 → 心跳线程（BeatReactor）定时续约（5s）
                 └─ 实例状态维护（临时实例由客户端心跳维持）
```

| 环节 | 关键类 | 说明 |
|------|--------|------|
| 注册 | `NacosServiceRegistry` | Commons `ServiceRegistry` 实现 |
| 客户端 | `NacosNamingService` | 注册/查询/订阅核心 |
| 心跳 | `BeatReactor` | 临时实例续约（默认 5s） |
| 订阅 | `NacosNamingService.subscribe` → `NamingClientProxy` | 服务变更推送 |

### 2.2 服务发现与订阅链路

```text
@LoadBalanced RestTemplate / OpenFeign
 → DiscoveryClient.getInstances（NacosDiscoveryClient）
 → NacosNamingService.selectInstances（查询健康实例）
 → 订阅机制：subscribe 注册监听 → 服务变更 → 回调更新本地缓存
```

> 🎯 **核心要点**：Nacos 客户端 = **注册（注册+心跳） + 发现（查询+订阅）**两条链。临时实例"不在即删"由心跳维护——这是 AP 语义的客户端证据。

## 3. OpenFeign 源码

### 3.1 代理生成链路

```text
@EnableFeignClients → FeignClientsRegistrar（ImportBeanDefinitionRegistrar）
 → 扫描 @FeignClient 接口
 → 每个接口注册 FeignClientFactoryBean（FactoryBean）
      └─ getObject() → Feign.builder() → ReflectiveFeign
           → 创建 JDK 动态代理（InvocationHandler = FeignInvocationHandler）
```

| 环节 | 关键类 | 说明 |
|------|--------|------|
| 扫描 | `FeignClientsRegistrar` | 与 [06](06-数据访问源码导读.md) 的 Registrar 套路一致 |
| 工厂 | `FeignClientFactoryBean` | 按接口配置构建 Feign 实例 |
| 代理 | `FeignInvocationHandler` | 方法分发到 MethodHandler |
| 方法映射 | `MethodHandler`（`SynchronousMethodHandler`） | 请求模板 → 执行 |

### 3.2 调用链路

```text
orderClient.getOrder(id)
 → FeignInvocationHandler.invoke
 → SynchronousMethodHandler.invoke
      ├─ 请求模板组装（方法参数 → RequestTemplate）
      ├─ Client.execute（HTTP 客户端：JDK/OkHttp/Apache）
      └─ 结果解码（Decoder → 返回类型）
```

| 面试锚点 | 源码事实 |
|----------|---------|
| 超时/重试 | `Request.Options`（连接/读取超时）+ `Retryer`（默认不重试 5xx） |
| 熔断集成 | 5.0 起官方降级为兼容适配器，官方方向是 Spring HTTP Interface（`@HttpExchange`） |
| 与 LoadBalancer | `FeignBlockingLoadBalancerClient`（负载均衡的 Client 装饰器） |

## 4. Spring Cloud Gateway 源码

### 4.1 路由定位链路

```text
HTTP 请求 → GatewayHandlerMapping（WebFlux HandlerMapping 实现）
 → 遍历 RouteLocator 收集的路由
 → Predicate 匹配（Path/Header/Method 谓词）
 → 命中 → GatewayWebHandler 执行
      └─ GatewayFilterChain（过滤器责任链）
           ├─ 全局过滤器（GlobalFilter：负载均衡/限流/日志）
           └─ 路由过滤器（GatewayFilter：改写/重试/熔断）
 → 转发（NettyRoutingFilter 发送目标服务）
```

| 环节 | 关键类 | 说明 |
|------|--------|------|
| 路由收集 | `RouteLocator`（`RouteDefinitionRouteLocator`） | 从配置/Bean 构建路由 |
| 匹配 | `GatewayHandlerMapping` + Predicate | 谓词链 |
| 执行 | `GatewayWebHandler` | 过滤器链驱动 |
| 转发 | `NettyRoutingFilter` | WebFlux 异步转发 |

### 4.2 过滤器链源码要点

```text
GatewayFilterChain = 组合模式（DefaultGatewayFilterChain）
 → 逐个执行（GlobalFilter 与 GatewayFilter 合并排序）
 → 顺序：路由级 → 全局级（Order 决定）
```

> 💡 自写过滤器看两个接口：`GlobalFilter`（全局，`filter(exchange, chain)`）+ `Ordered`（排序）；`GatewayFilterFactory`（配置化生成）。

## 5. Spring Cloud Stream 源码

### 5.1 绑定生命周期链路

```text
应用启动
 → StreamFunctionAutoConfiguration（函数注册）
 → 解析 spring.cloud.function.definition
 → 为函数生成绑定（function-in-0 / function-out-0）
 → BinderFactory.getBinder（kafka/rabbit）
 → KafkaMessageChannelBinder.bindConsumer / bindProducer
      ├─ 校验/创建 Topic
      └─ 启动消费者容器（spring-kafka）或生产者
 → 消息流转：函数 ↔ MessageChannel ↔ Binder
```

| 环节 | 关键类 | 说明 |
|------|--------|------|
| 函数注册 | `FunctionInitializer` | 函数 Bean → 绑定目标 |
| 绑定抽象 | `Binding` / `Bindable` | 生命周期管理 |
| Binder SPI | `Binder<T, C, P>` | 中间件适配层 |
| Kafka 实现 | `KafkaMessageChannelBinder` | 容器/事务/DLQ |

### 5.2 消费链路

```text
Kafka 消息 → ConcurrentMessageListenerContainer
 → 反序列化 → MessageChannel（input）
 → 函数消费（Consumer<...>）→ 处理结果 → output 通道 → 生产
```

> 🎯 **核心要点**：Stream 源码 = **绑定层（Binder SPI）+ 编程层（函数式模型）**两层的拼接。业务只面对函数，Binder 负责把 MessageChannel 翻译成中间件资源（详见仓库「Spring Cloud Stream」体系）。

## 6. 四个"入口类"速查表

| 组件 | 入口类 | 关键方法 | 断点场景 |
|------|--------|---------|---------|
| Nacos | `NacosServiceRegistry` / `NacosNamingService` | `register` / `subscribe` | 注册与订阅 |
| OpenFeign | `FeignClientFactoryBean` / `SynchronousMethodHandler` | `getObject` / `invoke` | 代理与调用 |
| Gateway | `GatewayHandlerMapping` / `GatewayWebHandler` | `getHandler` / `handle` | 路由与过滤链 |
| Stream | `FunctionInitializer` / `KafkaMessageChannelBinder` | `initialize` / `bindConsumer` | 绑定创建 |

## 7. 核心要点

> 🎯 **核心要点**：
> - 每组件只读一条主链：注册/订阅（Nacos）、代理/调用（Feign）、定位/过滤（Gateway）、绑定/消费（Stream）；
> - 四组件都复用同一套路：自动配置发现 → Registrar/FactoryBean 注册 → 代理或处理器 → 执行链；
> - 断点入口记"入口类 + 关键方法"两个词即可，不要背代码；
> - 与数据访问（[06](06-数据访问源码导读.md)）同构：接口式/声明式组件源码都可按四部曲读。

## 8. 参考来源

- [Spring Cloud Commons（ServiceRegistry/DiscoveryClient 接口）](https://docs.spring.io/spring-cloud-commons/reference/)
- [Spring Cloud OpenFeign GitHub](https://github.com/spring-cloud/spring-cloud-openfeign)
- [Spring Cloud Gateway Reference](https://docs.spring.io/spring-cloud-gateway/reference/)
- [Spring Cloud Stream Reference](https://docs.spring.io/spring-cloud-stream/reference/)
- [Nacos Java SDK 源码（nacos-client）](https://github.com/alibaba/nacos)

---

**下一模块**：[08-动态代理与字节码增强深潜](08-动态代理与字节码增强深潜.md)　/　**返回总览**：[00-总览](00-高级进阶与源码学习总览.md)
