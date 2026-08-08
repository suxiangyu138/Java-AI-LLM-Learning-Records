# 02 快速开始与核心 API 速查

> 实操篇：**四种接入方式（RestTemplate/WebClient/Feign/编程式）、@LoadBalanced 原理、ReactiveLoadBalancer 核心 API、请求变换器**——先跑通"服务名调用"，再理解 API 骨架。

---

## 📚 目录

1. [环境准备与依赖](#1-环境准备与依赖)
2. [接入方式一：@LoadBalanced RestTemplate](#2-接入方式一loadbalanced-resttemplate)
3. [接入方式二：WebClient](#3-接入方式二webclient)
4. [接入方式三：Feign（自动集成）](#4-接入方式三feign自动集成)
5. [接入方式四：编程式 API（ReactiveLoadBalancer）](#5-接入方式四编程式-apireactiveloadbalancer)
6. [BlockingLoadBalancerClient 与请求变换器](#6-blockingloadbalancerclient-与请求变换器)
7. [三步验证与排障](#7-三步验证与排障)

---

## 1. 环境准备与依赖

### 1.1 版本组合（2026-08 主线）

| 项 | 版本 |
|----|------|
| Spring Boot | **4.0.x/4.1.x** |
| Spring Cloud | **2025.1.x（Oakwood）** |
| Spring Cloud LoadBalancer | **5.0.x**（BOM 锁定） |

### 1.2 依赖

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-loadbalancer</artifactId>
</dependency>
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
</dependency>
<!-- 或 Eureka / Consul / Zookeeper discovery starter -->
```

> 💡 Feign/Gateway 项目通常已含 loadbalancer 传递依赖——`mvn dependency:tree | grep loadbalancer` 确认后再加。

## 2. 接入方式一：@LoadBalanced RestTemplate

### 2.1 用法

```java
@Configuration
public class RestClientConfig {
    @Bean
    @LoadBalanced                                  // ★ 唯一关键注解
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}

@Service
public class PaymentCaller {
    @Autowired
    private RestTemplate restTemplate;

    public String hello() {
        return restTemplate.getForObject(
                "http://payment-service/api/hello", String.class);   // ★ 服务名，非 IP
    }
}
```

### 2.2 @LoadBalanced 原理

```text
@LoadBalanced = 标记 RestTemplate 需要 LB 增强
  ├── LoadBalancerAutoConfiguration 检测到标记 → 给该 RestTemplate 注入拦截器
  ├── LoadBalancerInterceptor 拦截请求 → 解析服务名（http://payment-service/...）
  ├── 交给 BlockingLoadBalancerClient → LoadBalancer 选实例 → 替换为 http://ip:port/api/hello
  └── 发起真实调用

多个 RestTemplate：只有标 @LoadBalanced 的支持服务名调用（未标的不处理）
```

> 🎯 面试必答：**"@LoadBalanced 做了什么？"**——**标记 + 拦截器**：Spring 检测到该注解就给 RestTemplate 装配 `LoadBalancerInterceptor`，拦截器把请求 URL 里的**服务名解析成选中的实例地址**（LoadBalancer 选实例 → 替换 host）——没有它，RestTemplate 会把服务名当域名去 DNS 解析（必失败）。

### 2.3 RestClient（RestTemplate 的继任者）

```java
@Bean
@LoadBalanced
public RestClient.Builder restClientBuilder() {
    return RestClient.builder();
}

// 使用（服务名调用同样生效）
restClientBuilder.build().get().uri("http://payment-service/api/hello").retrieve().body(String.class);
```

## 3. 接入方式二：WebClient

### 3.1 用法（响应式）

```java
@Configuration
public class WebClientConfig {
    @Bean
    @LoadBalanced
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}

@Service
public class ReactivePaymentCaller {
    @Autowired
    private WebClient.Builder webClientBuilder;

    public Mono<String> hello() {
        return webClientBuilder.build()
                .get().uri("http://payment-service/api/hello")    // ★ 服务名
                .retrieve().bodyToMono(String.class);
    }
}
```

### 3.2 底层机制

```text
@LoadBalanced WebClient.Builder → 装配 ReactorLoadBalancerExchangeFilterFunction
  ├── ExchangeFilterFunction：WebClient 的过滤器机制
  ├── 请求发出前拦截 → lb 解析服务名 → 选实例 → 替换 URL
  └── 响应式（不阻塞事件循环）
```

## 4. 接入方式三：Feign（自动集成）

```java
@FeignClient(name = "payment-service")        // ★ name = 服务名，LB 自动生效
public interface PaymentClient {
    @GetMapping("/api/hello")
    String hello();
}
```

```text
Feign + LoadBalancer 集成（FeignBlockingLoadBalancerClient）：
  ├── @FeignClient(name=...) → Feign 的 loadbalancer 模式
  ├── 请求发出 → FeignBlockingLoadBalancerClient → LoadBalancer 选实例
  └── 无需任何额外注解（starter-openfeign 自动装配）
```

> 💡 Feign 用户零成本：**@FeignClient 的 name 天然走 LB**——LoadBalancer 对 Feign 是隐式能力（[OpenFeign 系列](../Spring Cloud OpenFeign/)）。

## 5. 接入方式四：编程式 API（ReactiveLoadBalancer）

### 5.1 直接调用

```java
@Component
public class ManualLbCaller {
    @Autowired
    private LoadBalancerClientFactory lbFactory;    // 按服务名拿上下文

    public ServiceInstance pick(String serviceId) {
        ReactiveLoadBalancer<ServiceInstance> lb = lbFactory
                .getLazyProvider(serviceId, ServiceInstanceListSupplier.class)
                .get();                                   // 或直接拿 LoadBalancer
        return lb.choose(Request.createDefault()).block() // 阻塞取（响应式场景用 subscribe）
                .getServer();                             // 选中的实例
    }
}
```

### 5.2 自定义策略选择

```java
@Configuration
public class CustomLbConfig {
    // ★ 自定义 LoadBalancer：替换默认轮询
    @Bean
    ReactorLoadBalancer<ServiceInstance> randomLoadBalancer(
            Environment environment, LoadBalancerClientFactory factory) {
        String serviceId = environment.getProperty(LoadBalancerClientFactory.PROPERTY_NAME);
        return new RandomLoadBalancer(
                factory.getLazyProvider(serviceId, ServiceInstanceListSupplier.class), serviceId);
    }
}
// 应用：@LoadBalancerClient(name = "payment-service", configuration = CustomLbConfig.class)
```

> 💡 编程式场景：**动态选择实例（AB 测试/自研框架）、测试模拟、不走 HTTP 的 RPC**——大部分业务用不到，但理解它 = 理解 LB 的完整调用链。

## 6. BlockingLoadBalancerClient 与请求变换器

### 6.1 BlockingLoadBalancerClient

```text
职责：阻塞式客户端的统一入口（RestTemplate/RestClient 内部用）
  ├── execute(serviceId, request) → 选实例 → 执行请求
  └── 与 ReactiveLoadBalancer 的关系：内部用同一个 Supplier 链，阻塞包装
```

### 6.2 请求变换器（LoadBalancerRequestTransformer）

```java
// 选中实例后、发起请求前，可改写请求（阻塞式）
@Component
public class MyTransformer implements LoadBalancerRequestTransformer {
    @Override
    public HttpRequest transformRequest(HttpRequest request, ServiceInstance instance) {
        // 例如：把实例 ip 加进头，供下游记录
        request.getHeaders().add("X-Backend-Instance", instance.getHost() + ":" + instance.getPort());
        return request;
    }
}
// WebClient 版：LoadBalancerClientRequestTransformer（签名不同）
```

| 场景 | 用法 |
|------|------|
| 透传实例信息 | 变换器加头（调试/链路） |
| 改写路径 | 变换器改 URI |
| 鉴权补充 | 变换器加认证头 |

> 💡 变换器 vs 过滤器：**变换器只作用于 LB 解析后的请求（一次调用），网关过滤器作用于网关整体**——LB 变换器是"调用点定制"。

## 7. 三步验证与排障

### 7.1 验证清单

| 步骤 | 做法 | 预期结果 |
|------|------|---------|
| ① 服务名解析 | 调接口（注册中心 2 实例） | 请求到达实例（各实例日志交替出现） |
| ② 轮询分发 | 连续调 10 次 | 两个实例约 5/5 分配 |
| ③ 故障规避 | 停一个实例，等缓存 TTL | 请求全打存活实例，无报错 |
| ④ 策略生效 | 配 configurations=weighted + 元数据权重 | 请求按权重分布 |

### 7.2 排障起点

| 现象 | 大概率原因 | 处理 |
|------|-----------|------|
| 服务名解析失败（UnknownHostException） | 没加 @LoadBalanced / 没引 LB starter | 检查注解与依赖 |
| 打到已下线实例 | 缓存 TTL 未过 / 注册中心未摘除 | 等 TTL 或清缓存；看注册中心状态 |
| 全部实例都打不到 | 注册中心连接问题 / 服务未注册 | 查注册中心服务列表 |
| 策略没生效 | configurations 写错 / 实例 metadata 缺失 | 核对配置键与 metadata（[03 篇](03-负载均衡策略全解.md)） |
| 轮询不均匀 | 高并发瞬时（随机抖动）或缓存影响 | 长周期观察，不必追求精确 50/50 |

> 💡 排障第一招：**先 curl 注册中心确认实例列表 → 再开 `logging.level.org.springframework.cloud.loadbalancer=DEBUG` 看选择日志**（打印每个实例的挑选过程）（[08 篇](08-生产实践与选型避坑.md) 也有）。

---

**下一模块**：[03-负载均衡策略全解](03-负载均衡策略全解.md)　**返回总览**：[00-Spring Cloud LoadBalancer知识体系总览](00-Spring Cloud LoadBalancer知识体系总览.md)

**【参考来源】**：[Spring Cloud LoadBalancer 5.0.2 官方文档（Spring Cloud Commons）](https://docs.spring.io/spring-cloud-commons/reference/spring-cloud-commons/loadbalancer.html)
