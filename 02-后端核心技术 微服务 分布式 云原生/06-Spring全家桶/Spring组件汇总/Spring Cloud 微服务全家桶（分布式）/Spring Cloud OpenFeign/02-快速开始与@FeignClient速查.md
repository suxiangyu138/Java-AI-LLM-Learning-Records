# 02 快速开始与 @FeignClient 速查

> 实操篇：**@FeignClient 全属性（name/url/contextId/configuration/fallback...）、接口定义规范、@EnableFeignClients 扫描规则、三步验证**——Feign 的一切都围绕"接口注解"展开。

---

## 📚 目录

1. [环境准备与依赖](#1-环境准备与依赖)
2. [@EnableFeignClients：扫描与开关](#2-enablefeignclients扫描与开关)
3. [@FeignClient 全属性速查](#3-feignclient-全属性速查)
4. [接口定义规范](#4-接口定义规范)
5. [上下文隔离：contextId 的使用场景](#5-上下文隔离contextid-的使用场景)
6. [三步验证与排障](#6-三步验证与排障)

---

## 1. 环境准备与依赖

### 1.1 版本组合（2026-08 主线）

| 项 | 版本 |
|----|------|
| Spring Boot | **4.0.x/4.1.x** |
| Spring Cloud | **2025.1.x（Oakwood）** |
| Spring Cloud OpenFeign | **5.0.x**（BOM 锁定） |

### 1.2 依赖

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>
<!-- ★ 默认 Client 依赖（官方要求显式加） -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-loadbalancer</artifactId>
</dependency>
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
</dependency>
```

> ⚠️ 缺 loadbalancer 的表现：启动可能正常，**调用时服务名解析失败**（`UnknownHostException: payment-service`）——先查依赖树（[08 篇](08-生产实践与选型避坑.md) 坑位表）。

## 2. @EnableFeignClients：扫描与开关

```java
@SpringBootApplication
@EnableFeignClients                            // ★ 默认扫描启动类所在包及其子包
public class OrderApplication { }

// 精确指定扫描包（Feign 接口不在启动类子包时）
@EnableFeignClients(basePackages = "com.example.api.client")

// 显式指定接口类
@EnableFeignClients(clients = {PaymentClient.class, UserClient.class})
```

| 用法 | 场景 |
|------|------|
| 默认（无参） | Feign 接口在启动类子包下（最常见） |
| basePackages | 接口集中在独立包（如 api.client 包） |
| clients | 少量接口显式列出（精确控制） |

> ⚠️ **最常见的坑**：Feign 接口不在扫描范围 → 注入报 `No qualifying bean`——先确认 @EnableFeignClients 扫描范围与接口包路径。

## 3. @FeignClient 全属性速查

### 3.1 属性总表

| 属性 | 必填 | 说明 | 示例 |
|------|:---:|------|------|
| **name** | ✅（5.x 起） | 客户端名 = 服务名（LB 用它选实例）；支持占位符 | `name = "payment-service"` |
| value | — | name 的别名（二选一） | `value = "payment-service"` |
| **url** | ❌ | 固定地址（**指定后不走 LB**） | `url = "http://localhost:8082"` |
| **contextId** | 条件 | 同名/同 url 多客户端时的区分标识 | `contextId = "paymentV1"` |
| **configuration** | ❌ | 自定义配置类（覆盖默认 Feign 配置） | `configuration = FooConfig.class` |
| fallback | ❌ | 降级实现类（需是 Bean） | `fallback = PaymentFallback.class` |
| fallbackFactory | ❌ | 降级工厂（可拿异常） | `fallbackFactory = PaymentFallbackFactory.class` |
| qualifiers | ❌ | Bean 别名（默认全限定接口名） | `qualifiers = "paymentClient"` |
| primary | ❌ | false 关闭默认 @Primary | `primary = false`（配合 fallback） |
| path | ❌ | 统一路径前缀 | `path = "/api"` |

### 3.2 核心属性详解

```java
// ① 标准用法（服务名 + LB + 熔断）
@FeignClient(name = "payment-service", fallbackFactory = PaymentFallbackFactory.class)
public interface PaymentClient { ... }

// ② 固定地址（不走注册中心/LB）
@FeignClient(name = "external-api", url = "https://api.thirdparty.com")
public interface ExternalApiClient { ... }

// ③ 占位符（配置化）
@FeignClient(name = "${feign.payment.name:payment-service}")
public interface PaymentClient { ... }

// ④ 前缀路径
@FeignClient(name = "payment-service", path = "/api")
public interface PaymentClient {
    @GetMapping("/hello")      // 实际请求：/api/hello
    String hello();
}
```

> ⚠️ **name 必填注意**：5.x 起**用 url 也必须写 name**（官方：name is now required even when using url）——早期版本可只写 url，现在必须双写。

## 4. 接口定义规范

### 4.1 方法注解（SpringMvcContract 契约）

```java
@FeignClient(name = "payment-service")
public interface PaymentClient {
    // ① GET + 路径变量
    @GetMapping("/payments/{id}")
    Payment getPayment(@PathVariable("id") Long id);

    // ② POST + JSON 体
    @PostMapping("/payments")
    Payment createPayment(@RequestBody PaymentRequest request);

    // ③ 查询参数
    @GetMapping("/payments")
    List<Payment> listPayments(@RequestParam("status") String status);

    // ④ 请求头
    @GetMapping("/payments/history")
    List<Payment> history(@RequestHeader("X-User-Id") String userId);
}
```

### 4.2 规范清单

| 规范 | 说明 |
|------|------|
| 注解语义复用 | @GetMapping/@PostMapping/@PathVariable/@RequestBody/@RequestParam 全部可用（与 Controller 相同） |
| **返回类型与泛型** | 支持复杂泛型（List<Payment>），解码器自动处理 |
| **继承** | 接口可继承（父接口定义共享方法） |
| 方法重载 | 不支持同签名重载（代理映射冲突） |
| 参数注解必写 | 多个参数时每个都要标 @PathVariable/@RequestParam（否则绑定失败） |
| DTO 独立性 | 接口返回 DTO 与 Controller DTO 可共用（跨服务共享 DTO 是常见实践） |

> ⚠️ **接口与 Controller 的边界**：Feign 接口是"调用契约"——**别把业务逻辑写进接口实现**（Feign 接口没有实现类，只有代理）；降级逻辑放 fallback 类。

## 5. 上下文隔离：contextId 的使用场景

### 5.1 问题场景

```java
// 同名客户端两个配置（一个要走熔断、一个不走）
@FeignClient(name = "payment-service", configuration = AConfig.class, contextId = "paymentA")
public interface PaymentClientA { ... }

@FeignClient(name = "payment-service", configuration = BConfig.class, contextId = "paymentB")
public interface PaymentClientB { ... }

// 场景：老接口超时 1s、新接口超时 5s——同一服务两个客户端配置
```

### 5.2 不写 contextId 的问题

```text
两个 @FeignClient(name="payment-service") 但 configuration 不同：
  ├── 上下文/Bean 名冲突（都是 payment-service 上下文）
  ├── 启动报 BeanDefinitionOverrideException 或配置互相污染
  └── contextId 唯一 → 各自独立上下文与配置
```

> 💡 判断标准：**同一服务需要多套 Feign 配置（不同超时/不同拦截器）时用 contextId 区分**；大多数项目一个服务一个客户端，用不到。

## 6. 三步验证与排障

### 6.1 验证清单

| 步骤 | 做法 | 预期结果 |
|------|------|---------|
| ① 代理生成 | 启动 + 注入 | PaymentClient Bean 是 JDK 动态代理（类名带 $$） |
| ② 调用成功 | 调接口 | 返回下游数据（服务名解析 + LB 选实例） |
| ③ 熔断降级 | 停下游 + 开 circuitbreaker | fallback 生效（返回降级值） |
| ④ 日志 | 开 DEBUG 调一次 | 请求 URL/耗时可见（loggerLevel=full） |

### 6.2 排障起点

| 现象 | 大概率原因 | 处理 |
|------|-----------|------|
| 注入报 No qualifying bean | @EnableFeignClients 没加 / 扫描范围不对 | 检查启动类注解与接口包路径 |
| UnknownHostException: 服务名 | 缺 loadbalancer 依赖 / 没写 name | 加 starter-loadbalancer；检查 name |
| 接口方法参数绑定错 | 多参数没标注解 | 每个参数标 @PathVariable/@RequestParam |
| fallback 不生效 | 没开 circuitbreaker / fallback 不是 Bean | feign.circuitbreaker.enabled=true（[04 篇](04-熔断降级集成.md)） |
| 超时/慢 | 超时未配（默认值） | connectTimeout/readTimeout 显式配（[03 篇](03-超时重试与HTTP客户端.md)） |

> 💡 排障第一招：**启动日志看 Feign 接口注册**（`Registering Feign client: payment-service` 或类似）→ 再看调用日志（开 DEBUG）——"注册没注册、请求发没发"两步定位（[08 篇](08-生产实践与选型避坑.md) 也有）。

---

**下一模块**：[03-超时重试与HTTP客户端](03-超时重试与HTTP客户端.md)　**返回总览**：[00-Spring Cloud OpenFeign知识体系总览](00-Spring Cloud OpenFeign知识体系总览.md)

**【参考来源】**：[Spring Cloud OpenFeign 5.0.2（Declarative REST Client / @FeignClient）](https://docs.spring.io/spring-cloud-openfeign/reference/spring-cloud-openfeign.html)
