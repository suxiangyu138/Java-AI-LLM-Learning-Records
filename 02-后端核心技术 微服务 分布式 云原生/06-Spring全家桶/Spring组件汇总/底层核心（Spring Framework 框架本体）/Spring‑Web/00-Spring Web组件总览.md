# 00 Spring Web 组件总览

> 组件卡片：spring-web 是什么、版本现状、能做什么、与深度体系如何衔接——MVC 与 WebFlux 共用的 HTTP 基础设施

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

**spring-web 是 Spring 的 HTTP 与 Web 基础设施层**——HTTP 消息模型（HttpHeaders/HttpMessage/MediaType）、消息转换器（JSON/XML 等）、HTTP 客户端（RestClient/WebClient/RestTemplate）、Web 上下文（request/session 作用域）与 Servlet 集成过滤器；**Spring MVC 与 Spring WebFlux 都构建在它之上**。

```text
核心心智模型：
  spring-web = 协议层 + 传输层 + 上下文层
    ├── http          HTTP 语义模型：Message/Headers/Status/MediaType
    ├── http.converter 消息转换：对象 ↔ 字节（JSON/XML/Form/...）
    ├── http.client   HTTP 客户端：RestClient（同步）/ WebClient（响应式）
    ├── web.context   Web 容器上下文：WebApplicationContext/作用域
    └── web           Servlet 集成：过滤器/CORS/多部分解析

  一句话：把"HTTP 协议"变成"可编程的对象模型"，MVC/WebFlux 只管业务映射。
```

| 维度 | 说明 |
|------|------|
| 所属 | Spring Framework 底层核心模块（spring-web） |
| 版本线 | 随 Spring Framework 7.x（2025-11 起） |
| 语言要求 | Java 17+（Servlet 6.x / Jakarta EE 11 可选） |
| 定位 | HTTP 协议层与传输层（MVC/WebFlux 的公共地基） |

### 1.1 定位辨析：web / webmvc / webflux / starter

| 术语 | 模块 | 定位 | 是否包含服务器 |
|------|------|------|--------------|
| spring-web | Framework 底层模块 | HTTP 协议/传输/上下文 | 否（纯协议层） |
| spring-webmvc | Framework 模块 | Servlet 栈控制器层 | 依赖容器（Tomcat 等） |
| spring-webflux | Framework 模块 | 响应式栈控制器层 | 依赖 Netty 等 |
| starter-web | Boot 启动器 | web + webmvc + Tomcat 自动配置 | 是（内嵌 Tomcat） |
| starter-restclient | Boot 启动器 | 纯客户端（无服务器） | 否 |

> 💡 面试陷阱题："spring-boot-starter-web 是模块吗？"——不是，它是 Boot 的**依赖聚合器**（web + webmvc + Tomcat + 自动配置）；模块坐标只有 `org.springframework:spring-web`。

## 2. 版本现状（2026-08）

| 版本 | 说明 |
|------|------|
| Spring Framework 7.0（2025-11） | 当前主线（Boot 4.0 配套） |
| Spring Framework 7.1（2026-05） | 最新维护线（Boot 4.1 配套） |
| Spring Framework 6.2.x | 存量主线（Boot 3.x 配套） |

**7.x web 关键变化：**

- **`HttpMessageConverters` 统一配置 API（7.0）**：框架级集中配置消息转换器（对齐响应式侧 CodecConfigurer）——`RestClient.builder().configureMessageConverters(...)` / `RestTemplate` 同款 / `WebMvcConfigurer` 同款；旨在**取代 Boot 自身的 HttpMessageConverters 类型**；
- **消息转换器改名与重构**：`JacksonJsonHttpMessageConverter`（取代 Jackson2 命名）、`JacksonXmlHttpMessageConverter`、`JacksonCborHttpMessageConverter`（CBOR 默认装配）；`KotlinSerializationJsonHttpMessageConverter` 默认**只编码 @Serializable 类型**（可用 Predicate 定制）；
- **RestTemplate 官宣弃用**（2025-09 官方博客）：7.0 宣布弃用意图 → 计划 7.1 正式标记 `@Deprecated` → 8.0 移除（OSS 支持至 2029+）；迁移 RestClient（同一基础设施，可包一层旧 RestTemplate 过渡）；
- **API 版本化（7.0）**：客户端侧 `ApiVersionInserter`（头/媒体类型/路径/查询参数注入版本），`defaultVersion(...)` + `apiVersionInserter(...)` 配置；
- **HTTP 接口组（7.0）**：`@ImportHttpServices(group = "...", types = {...})` 批量声明共享同一 RestClient 的 HTTP 接口客户端；
- **`RestTestClient`（7.0）**：统一断言 API 的测试客户端（支持真实服务器集成测试与 MockMvc 式 mock 测试）；
- **7.1**：`PreFlightRequestFilter`（CORS 预检专用过滤器）；`HttpHeaders`/`WebSocketHttpHeaders` 互操作修复；
- **Boot 4 新 starter**：`spring-boot-starter-restclient` / `spring-boot-starter-webclient` 明确客户端意图。

> ⚠️ **要点**：7.0 的 web 变化是"HTTP 客户端与转换器现代化"——RestClient 是唯一同步客户端的未来；存量 RestTemplate 无需急迁（支持期 2029+），但新代码一律 RestClient。

### 2.1 版本窗口与升级影响面

| 版本 | 起始时间 | Java 基线 | Boot 配套 | 支持状态 |
|------|---------|-----------|-----------|---------|
| 7.0.x | 2025-11 | Java 17+ | Boot 4.0.x | 当前主线（维护中） |
| 7.1.x | 2026-05 | Java 17+ | Boot 4.1.x | 最新维护线 |
| 6.2.x | 2023-11 | Java 17+ | Boot 3.4/3.5 | 存量主线（OSS 支持中） |

**从 6.2 升级到 7.x 的代码影响面**（按改动频率排序）：

| 影响点 | 6.x 写法 | 7.x 写法 | 影响 |
|--------|---------|---------|------|
| JSON 转换器类名 | `MappingJackson2HttpMessageConverter` | `JacksonJsonHttpMessageConverter` | 编译期，改名即可 |
| 转换器配置 | `extendMessageConverters(List<...>)` | `configureMessageConverters(HttpMessageConverters)` | 编译期，改方法签名 |
| 客户端主推 | RestTemplate | RestClient | 运行期不变，新代码迁移 |
| API 版本化 | 手动加头 | `ApiVersionInserter` 自动注入 | 新能力，可选 |
| CORS 预检 | CorsFilter 通用处理 | `PreFlightRequestFilter`（7.1） | 新能力，可选 |

> ⚠️ **升级节奏建议**：① 6.x → 7.x 先跑编译，按上表改名清单逐项处理；② 转换器配置 API 迁移属于"行为等价"重构——先迁移再改行为；③ RestTemplate 存量代码维持原样，与 RestClient 并行运行验证行为一致后逐步替换。

### 2.2 7.x 新能力一览

| 新能力 | 版本 | 定位 | 是否默认 |
|--------|------|------|---------|
| `HttpMessageConverters` 统一配置 API | 7.0 | 框架级转换器配置（对齐 CodecConfigurer） | 配置点，非开关 |
| 转换器改名（JacksonJson 等） | 7.0 | 命名现代化 | 是（新类名） |
| `ApiVersionInserter` | 7.0 | 客户端 API 版本注入 | 需显式配置 |
| `@ImportHttpServices` 接口组 | 7.0 | 批量声明 HTTP 接口客户端 | 需显式声明 |
| `RestTestClient` | 7.0 | 统一客户端测试 API | 测试时使用 |
| RestTemplate 弃用声明 | 7.0 → 7.1 | 同步客户端收敛到 RestClient | 7.1 标记 @Deprecated |
| `PreFlightRequestFilter` | 7.1 | CORS 预检独立处理 | 需显式注册 |
| `spring-boot-starter-restclient/webclient` | Boot 4 | 客户端意图明确的 starter | Boot 4 提供 |

> 💡 新能力使用优先级：**改名的转换器**（编译期必处理）> **RestClient**（新代码必用）> **ApiVersionInserter/接口组**（多版本服务建议）> **PreFlightRequestFilter**（CORS 复杂场景可选）。

## 3. 能力地图

| 能力域 | 能力点 | 关键类/API |
|--------|--------|-----------|
| HTTP 模型 | 请求/响应对象 | `HttpMessage`、`HttpHeaders`、`ResponseEntity`、`MediaType` |
| 消息转换 | 对象 ↔ 字节 | `HttpMessageConverter`、`JacksonJsonHttpMessageConverter`（7.0 改名） |
| 同步客户端 | HTTP 客户端 | `RestClient`（✅ 推荐）、`RestTemplate`（7.0 弃用） |
| 响应式客户端 | 非阻塞 HTTP | `WebClient`（WebFlux 系列） |
| API 版本化 | 客户端版本注入 | `ApiVersionInserter`（7.0） |
| HTTP 接口 | 声明式远程调用 | `@HttpExchange`、`@ImportHttpServices`（7.0 组） |
| Web 上下文 | Web 容器集成 | `WebApplicationContext`、request/session 作用域 |
| 请求作用域 | 线程/请求绑定 | `RequestContextHolder`、`RequestContextFilter` |
| Servlet 集成 | 过滤器与 CORS | `CorsFilter`、`PreFlightRequestFilter`（7.1） |
| 多部分 | 文件上传解析 | `MultipartHttpMessageConverter`、`MultipartFile` |
| 测试 | 客户端测试 | `RestTestClient`（7.0） |

### 3.1 能力边界：什么归 spring-web，什么不归

| 归 spring-web | 不归 spring-web（在上下层） |
|--------------|---------------------------|
| HTTP 消息模型（HttpMessage/Headers/MediaType） | 控制器映射/参数绑定（spring-webmvc） |
| 消息转换器（对象 ↔ 字节） | 业务异常 → 状态码映射（webmvc 的 @ExceptionHandler） |
| HTTP 客户端（RestClient/WebClient 传输层） | 视图渲染（Thymeleaf/JSP 等） |
| 请求/会话作用域与 Web 上下文 | 安全过滤器链（Spring Security） |
| Servlet 过滤器（编码/CORS/请求上下文） | 响应式调度器/背压（Reactor，webflux 消费） |
| WebAsyncManager 异步抽象 | 声明式接口代理语义（web 的 HttpServiceProxyFactory 也在此） |

> 🎯 **判断口诀**：凡"HTTP 协议语义"归 spring-web；凡"业务应用语义"（映射、权限、渲染）归上层——面试被问"某个能力在哪个模块"按此判断。

### 3.2 阻塞 vs 响应式：传输层的取舍

| 维度 | 同步（RestClient + 虚拟线程） | 响应式（WebClient + Reactor） |
|------|------------------------------|-------------------------------|
| 代码形态 | 顺序书写，与业务代码一致 | Mono/Flux 管道，学习成本高 |
| 并发能力 | 虚拟线程承载（KB 级栈） | 事件循环复用线程（不占线程数） |
| 流式/背压 | 不支持（规划中） | 原生支持（SSE、大流） |
| 调试 | 栈可读，断点直观 | 堆栈深、调试难 |
| 阻塞库 | JDBC 等直接可用 | 需全链路响应式客户端 |
| 适用 | 常规服务间调用、CRUD | 网关、实时流、管道编排 |

> 💡 **官方立场（2025-09 博客）**：虚拟线程成熟后，大多数 HTTP 客户端场景回到同步栈；响应式保留给"需要背压/流式/极端连接复用"的窄场景——**先同步后响应式，别反着来**。

### 3.3 生产实践要点

| 实践 | 要点 |
|------|------|
| 超时必配 | RestClient 默认无超时——每个客户端显式配 connect/read 超时 |
| 连接池必配 | 默认每请求新建连接——HttpComponents 池 + 空闲回收 |
| 幂等重试 | 重试只对幂等请求（GET/PUT/DELETE + 幂等键）；POST 重试必须幂等键 |
| 版本化尽早 | 多版本 API 服务从第一天就配 `ApiVersionInserter`，后面补成本高 |
| 监控埋点 | 客户端加拦截器统计耗时/错误率（traceId 透传） |
| 测试用 RestTestClient | 统一 mock/真实服务器测试，替代手写 mock 代码 |

> 🎯 生产四连问：**超时配了吗？池配了吗？重试上限了吗？异常映射了吗？**——四个问题答不上来，客户端代码别上线。

## 4. 与深度体系的映射

| 速查文档 | 深度体系模块 |
|---------|-------------|
| 02-HTTP 客户端速查 | [Spring全家桶-04-Spring-MVC-Web层框架](../../../Spring全家桶/04-Spring-MVC-Web层框架.md) |
| 03-消息转换与内容协商速查 | [SpringMVC-04-响应与消息转换：HttpMessageConverter与Jackson 3](../../../SpringMVC/04-响应与消息转换：HttpMessageConverter与Jackson 3.md) |
| 04-数据绑定与 HTTP 消息速查 | [SpringMVC-03-参数绑定与数据校验](../../../SpringMVC/03-参数绑定与数据校验.md) |
| 05-Web 上下文与作用域速查 | [SpringMVC-01-DispatcherServlet请求处理全链路](../../../SpringMVC/01-DispatcherServlet请求处理全链路.md) |
| 06-过滤链与 CORS 速查 | [SpringMVC-06-拦截器与过滤器](../../../SpringMVC/06-拦截器与过滤器.md) |
| 07-异步与流式速查 | [SpringMVC-07-异步处理：虚拟线程、SSE与流式响应](../../../SpringMVC/07-异步处理：虚拟线程、SSE与流式响应.md) |
| 08-集成地图与常见问题 | [SpringBoot-03-SpringBootWeb开发](../../../SpringBoot/03-SpringBootWeb开发.md) |

> 💡 本系列定位"查得快"——HTTP 层以下（协议/转换/客户端）归 spring-web，HTTP 层以上（控制器/映射/异常）归 MVC 系列；响应式栈（WebClient/WebFlux）见响应式编程组件系列。

## 5. 快速上手 3 步

**① 引入依赖**（Boot 4：`spring-boot-starter-web` 传递引入 spring-web；纯客户端可用 `spring-boot-starter-restclient`）：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

**② 使用 RestClient 调用第三方接口**：

```java
@Service
public class PaymentClient {
    private final RestClient restClient;

    public PaymentClient(RestClient.Builder builder) {
        this.restClient = builder
                .baseUrl("https://api.example.com")
                .defaultHeader("Authorization", "Bearer " + token)
                .build();
    }

    public PaymentResult charge(PaymentReq req) {
        return restClient.post()
                .uri("/v1/charge")
                .contentType(MediaType.APPLICATION_JSON)
                .body(req)
                .retrieve()                       // 发起 + 状态码检查
                .body(PaymentResult.class);       // 自动 JSON 转换
    }
}
```

**③ 声明式 HTTP 接口**（7.0 推荐风格）：

```java
@HttpExchange("/v1/orders")
public interface OrderClient {
    @GetExchange("/{id}")
    Order getById(@PathVariable Long id);
}

@Configuration
class ClientConfig {
    @Bean
    OrderClient orderClient(RestClient.Builder builder) {
        return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(builder.build()))
                .build().createClient(OrderClient.class);
    }
}
```

### 5.1 边界与异常处理

**RestClient 超时与连接池**（默认无池、无超时——生产必须显式配置）：

```java
@Bean
RestClient restClient(RestClient.Builder builder) {
    HttpComponentsClientHttpRequestFactory factory =
            new HttpComponentsClientHttpRequestFactory(
                    HttpClientBuilder.create()
                            .setConnectionManager(new PoolingHttpClientConnectionManager())
                            .evictExpiredConnections()
                            .build());
    factory.setConnectTimeout(Duration.ofSeconds(3));       // 建连超时
    factory.setConnectionRequestTimeout(Duration.ofSeconds(5)); // 从池取连接超时
    factory.setReadTimeout(Duration.ofSeconds(10));         // 读响应体超时
    return builder
            .requestFactory(factory)
            .defaultStatusHandler(HttpStatusCode::isError, (req, res) -> {
                throw new ThirdPartyException(res.getStatusCode());
            })
            .build();
}
```

**典型异常速查**：

| 异常/现象 | 根因 | 处理 |
|-----------|------|------|
| `ResourceAccessException` | 连接失败/超时 | 检查网络、超时配置、代理 |
| 415 Unsupported Media Type | Content-Type 与转换器不匹配 | 两端媒体类型对齐（[03-消息转换与内容协商速查](03-消息转换与内容协商速查.md)） |
| 406 Not Acceptable | Accept 与 produces 无交集 | 检查 Accept 声明 |
| 连接耗尽 | 无池/池太小 | HttpComponents 工厂 + 池配置（如上） |
| 响应体未关闭 | `.exchange()` 未消费 | 必须消费或 close（[02-HTTP 客户端速查](02-HTTP客户端速查.md) 第 3 节） |
| SSL 证书错误 | 证书链/自签名未信任 | 配置 truststore；自签名仅测试环境可跳过校验（生产禁止） |
| 404/5xx 意外冒泡 | 未映射状态码 | `defaultStatusHandler` 统一转领域异常，日志带响应体摘要 |

### 5.2 常用配置项速查（Boot 4）

| 配置项 | 默认 | 说明 |
|--------|------|------|
| `spring.http.client.connect-timeout` | 无 | 客户端建连超时 |
| `spring.http.client.read-timeout` | 无 | 客户端读响应超时 |
| `spring.http.client.factory` | `jdk` | 请求工厂后端：`jdk` / `httpcomponents` |
| `spring.threads.virtual.enabled` | `true` | 虚拟线程开关（Boot 4 默认开） |
| `spring.servlet.multipart.max-file-size` | `1MB` | 单文件上传上限 |
| `spring.servlet.multipart.max-request-size` | `10MB` | 单请求上传上限 |
| `server.forward-headers-strategy` | `none` | 解析代理头（X-Forwarded-*） |

> ⚠️ 纯代码配置优先（@Bean 定制请求工厂），属性只覆盖"无代码场景"——**超时/池这类关键项建议代码显式配置**，属性与代码叠加时以代码为准（属性用于兜底默认值）。

## 6. 速查导航

| 文档 | 内容 |
|------|------|
| [01-模块清单](01-模块清单.md) | spring-web artifact 与包结构、Servlet/响应式双栈 |
| [02-HTTP 客户端速查](02-HTTP客户端速查.md) | RestClient/WebClient/RestTemplate 选型与迁移 |
| [03-消息转换与内容协商速查](03-消息转换与内容协商速查.md) | 转换器全家（7.0 改名）、configureMessageConverters |
| [04-数据绑定与 HTTP 消息速查](04-数据绑定与HTTP消息速查.md) | HttpHeaders/RequestEntity/UriComponentsBuilder |
| [05-Web 上下文与作用域速查](05-Web上下文与作用域速查.md) | WebApplicationContext、request/session 作用域 |
| [06-过滤链与 CORS 速查](06-过滤链与CORS速查.md) | 内建过滤器、CorsFilter、PreFlightRequestFilter（7.1） |
| [07-异步与流式速查](07-异步与流式速查.md) | 虚拟线程、SSE、响应式基础 |
| [08-集成地图与常见问题](08-集成地图与常见问题.md) | 与 MVC/WebFlux/Security/Boot 联动 + 高频坑 |

### 6.1 面试追问点

| 追问 | 回答要点 |
|------|---------|
| RestClient 与 RestTemplate 什么关系 | 同一基础设施（拦截器/请求工厂/转换器）；`RestClient.create(restTemplate)` 可包装过渡 |
| 为什么弃用 RestTemplate | URL 字符串拼接、错误处理分散；RestClient 统一 builder + 链式 + 状态码映射 |
| 阻塞与响应式怎么选 | 先虚拟线程 + 同步；有背压/流式需求才响应式 |
| HttpMessageConverters 与 Boot 自身类型什么关系 | 7.0 框架级 API 取代 Boot 自身类型，服务端/客户端统一配置心智 |
| ApiVersionInserter 怎么工作 | 4 种注入策略（头/媒体类型/路径/查询参数），`defaultVersion` 声明当前版本 |
| 一次 RestClient 调用内部几步 | 构建请求 → 拦截器链 → 请求工厂 → 转换器序列化 → 发送 → 反序列化 → 状态码处理 |
| SSE 断连怎么办 | 心跳/retry 字段/超时放大（[07-异步与流式速查](07-异步与流式速查.md)） |
| 虚拟线程下线程池还有意义吗 | 计算密集与外部连接池仍有意义；虚拟线程本身无需池 |
| 415 与 406 怎么区分记忆 | 415 是"请求进来读不了"（Content-Type 无转换器可读）；406 是"响应出不去"（Accept/produces 无交集可写）——一进一出 |
| WebClient 在 MVC 服务里能用吗 | 能，但 `.block()` 只允许在非事件循环线程；常规同步服务优先 RestClient |

> 💡 面试叙事线：**协议层（spring-web）→ 控制器层（webmvc）→ 自动配置（Boot）**——先讲清 spring-web 是公共底座，再展开上层，结构感最强。

## 7. 学习路线推荐

| 路线 | 适用 | 顺序 |
|------|------|------|
| 入门速查 | 认识 HTTP 层 | 00 总览 → 02 HTTP 客户端 → 03 转换器 → 01 模块清单 |
| 项目实践 | 调用第三方/排查协议问题 | 02 客户端 → 03 转换器 → 06 CORS → 08 常见问题 |
| 面试冲刺 | 协议与并发考点 | 02 客户端选型 → 03 转换原理 → 07 虚拟线程 → 08 考点清单 → [SpringMVC-00](../../../SpringMVC/00-SpringMVC知识体系总览.md) |

## 8. 核心概念速查

| 术语 | 一句话解释 |
|------|-----------|
| spring-web | HTTP 协议层与传输层（MVC/WebFlux 公共底座） |
| RestClient | 同步 HTTP 客户端（7.x 主推） |
| WebClient | 响应式 HTTP 客户端 |
| RestTemplate | 旧同步客户端（7.0 弃用，8.0 移除） |
| HttpMessageConverter | 对象 ↔ 字节的转换 SPI |
| HttpMessageConverters（7.0） | 转换器统一配置 API |
| WebApplicationContext | Web 容器上下文（父子层次） |
| ScopedProxyMode | 跨作用域注入的代理模式 |
| ApiVersionInserter（7.0） | 客户端 API 版本注入器 |

---

**下一模块**：[01-模块清单](01-模块清单.md)　**返回总览**：本页

**【参考来源】**：[Spring HTTP 客户端现状（spring.io 官方博客，2025-09）](https://spring.io/blog/2025/09/30/the-state-of-http-clients-in-spring)、[Spring Framework 7.0 Release Notes（GitHub Wiki）](https://github.com/spring-projects/spring-framework/wiki/Spring-Framework-7.0-Release-Notes)、[Spring Framework 7.0.0-M7 发布公告](https://spring.io/blog/2025/07/17/spring-framework-7-0-0-M7-available-now)、[HTTP Message Conversion 官方文档（7.1-SNAPSHOT）](https://docs.spring.io/spring/reference/7.1-SNAPSHOT/web/webmvc/message-converters.html)
