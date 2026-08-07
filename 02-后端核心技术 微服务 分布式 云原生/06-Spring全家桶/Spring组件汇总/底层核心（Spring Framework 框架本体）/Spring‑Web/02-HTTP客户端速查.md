# 02 HTTP 客户端速查

> RestClient/WebClient/RestTemplate/JDK HttpClient 四选一、RestClient 全 API、7.0 新能力——"调用第三方接口"的完整答案

---

## 📚 目录

1. [客户端四选一](#1-客户端四选一)
2. [RestClient：全 API 速查](#2-restclient全-api-速查)
3. [retrieve vs exchange](#3-retrieve-vs-exchange)
4. [WebClient：响应式客户端](#4-webclient响应式客户端)
5. [从 RestTemplate 迁移](#5-从-resttemplate-迁移)
6. [7.0 新能力：ApiVersionInserter 与 HTTP 接口](#6-70-新能力apiversioninserter-与-http-接口)

---

## 1. 客户端四选一

| 客户端 | 栈 | 7.x 状态 | 选型 |
|--------|-----|---------|------|
| `RestClient` | 同步（阻塞） | ✅ **官方推荐**（主推） | 常规 HTTP 调用 |
| `WebClient` | 响应式（非阻塞） | ✅ 积极维护 | 响应式 API、流式、高并发编排 |
| `RestTemplate` | 同步 | ⚠️ 7.0 弃用（8.0 移除） | 存量代码，逐步迁移 |
| JDK `HttpClient` | 同步/异步 | 非 Spring 管理 | 零依赖场景 |

```text
官方结论（Spring 博客，2025-09）：
  "大多数场景用 RestClient；需要响应式或流式用 WebClient"
  RestTemplate 7.0 宣布弃用 → 7.1 正式 @Deprecated → 8.0 移除（OSS 支持至 2029+）
  迁移路径：RestClient 与 RestTemplate 共享同一基础设施
  （拦截器/请求工厂/转换器可无缝搬移）
```

> 🎯 **核心要点**：7.x 的 HTTP 客户端只有两个未来——RestClient（同步）与 WebClient（响应式）；RestTemplate 是"历史包袱"，新代码写它=给后人留技术债。

### 1.1 选型决策清单（3 问定案）

| 问题 | 答是 | 答否 |
|------|------|------|
| 调用方链路是同步的还是响应式的？ | WebClient（响应式链路） | RestClient |
| 需要流式/背压/SSE 长连接？ | WebClient | RestClient |
| 代码是存量 RestTemplate？ | 渐进迁移 RestClient（不急） | 新代码直接 RestClient |
| 零 Spring 依赖的脚本/工具？ | JDK HttpClient | 任意 Spring 客户端 |
| 服务端线程内要发起调用？ | RestClient（阻塞安全） | WebClient 的 .block() 有死锁风险 |

> 💡 决策顺序：**先看链路形态（同步/响应式）→ 再看能力需求（流式）→ 最后看存量**——三个问题答完，客户端就定了；答完还犹豫的，选 RestClient（默认答案）。

## 2. RestClient：全 API 速查

```java
// 构建（Boot 4 自动装配 RestClient.Builder，可注入定制）
RestClient client = RestClient.builder()
        .baseUrl("https://api.example.com")
        .defaultHeader("Authorization", "Bearer xxx")
        .defaultUriVariables(Map.of("tenant", "a1"))
        .requestFactory(new JdkClientHttpRequestFactory())   // JDK 22 HttpClient 后端
        .configureMessageConverters(c -> c.registerDefaults()   // 7.0 转换器配置
                .jsonMessageConverter(new JacksonJsonHttpMessageConverter(mapper)))
        .build();
```

| 能力 | 写法 |
|------|------|
| GET | `.get().uri("/orders/{id}", id).retrieve().body(Order.class)` |
| POST | `.post().uri(...).contentType(JSON).body(req).retrieve()` |
| 表单 | `.body(MultiValueMap<String,String>)`（FormHttpMessageConverter） |
| 查询参数 | `.uri(b -> b.path("/orders").queryParam("status","PAID").build())` |
| 响应头 | `.retrieve().toEntity(Order.class)`（ResponseEntity 含头） |
| 无响应体 | `.retrieve().toBodilessEntity()` |
| 错误处理 | `.retrieve().onStatus(HttpStatusCode::is4xxClientError, (req,res)->{...})` |
| 完整控制 | `.exchange((req,res) -> ...)`（绕过转换链） |
| 异步（7.1+） | `RestClient` 仍同步；异步走 WebClient 或 JDK HttpClient |

> 💡 注入姿势：`RestClient.Builder`（Boot 4 自动配置 Bean）→ 各服务各自 build 定制——比全局单例更灵活；7.0 起可用 `configureMessageConverters` 精准换转换器。

### 2.1 超时、连接池与重试（生产必配）

```java
// 生产级配置：连接池 + 三超时 + 空闲回收
@Configuration
public class RestClientConfig {
    @Bean
    RestClient ordersClient(RestClient.Builder builder) {
        PoolingHttpClientConnectionManager cm =
                new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(200);                       // 总连接上限
        cm.setDefaultMaxPerRoute(50);              // 单路由（单域名）上限

        CloseableHttpClient httpClient = HttpClientBuilder.create()
                .setConnectionManager(cm)
                .evictIdleConnections(Duration.ofSeconds(30))   // 回收空闲连接
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setConnectTimeout(3_000)              // 建连超时
                        .setConnectionRequestTimeout(5_000)    // 从池取连接超时
                        .setSocketTimeout(10_000)              // 读响应超时
                        .build())
                .build();

        return builder.baseUrl("https://api.example.com")
                .requestFactory(new HttpComponentsClientHttpRequestFactory(httpClient))
                .build();
    }
}
```

| 配置点 | 默认 | 生产建议 |
|--------|------|---------|
| 连接池 | 无（每请求新建连接） | 池化 + 上限 + 空闲回收 |
| 连接超时 | 无 | 2-3s |
| 读取超时 | 无 | 按接口 SLA（如 10s） |
| 重试 | 不重试 | 幂等请求可重试；POST 必须幂等键 |

> ⚠️ **三个生产大坑**：① 不配超时 → 依赖方慢接口拖死本服务线程；② 不配池 → 每次请求新建 TCP + TLS 握手（开销巨大）；③ POST 盲目重试 → 重复扣款——重试前必须确认幂等键（Idempotency-Key），或用 `RetryTemplate`（见 [Spring-Core-07](../Spring‑Core/07-重试与弹性速查.md)）限定次数与退避。

### 2.2 全流程示例：调用、错误映射、响应头

```java
// 一个完整的服务调用方法：错误映射 + 响应头 + 泛型响应
public Page<Order> listOrders(String status, int page) {
    return restClient.get()
            .uri(b -> b.path("/v1/orders")
                    .queryParam("status", status)
                    .queryParam("page", page)
                    .build())
            .accept(MediaType.APPLICATION_JSON)
            // 状态码 → 领域异常（4xx 带响应体详情）
            .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                String body = new String(res.getBody().readAllBytes(), UTF_8);
                throw new OrderServiceException(res.getStatusCode().value(), body);
            })
            // 5xx 统一包装
            .onStatus(HttpStatusCode::is5xxServerError,
                    (req, res) -> { throw new UpstreamDownException(); })
            .retrieve()
            .body(new ParameterizedTypeReference<Page<Order>>() {});  // 泛型响应
}

// 需要响应头时：toEntity 返回 ResponseEntity
public ResponseEntity<Order> getWithHeaders(Long id) {
    return restClient.get().uri("/v1/orders/{id}", id)
            .retrieve()
            .toEntity(Order.class);   // 头 + 状态码 + 体
}
```

| API 出口 | 返回 | 场景 |
|----------|------|------|
| `.body(Type)` | 反序列化对象 | 常规 |
| `.toEntity(Type)` | ResponseEntity（含头/状态码） | 需要响应头 |
| `.toBodilessEntity()` | 仅状态码/头 | 204/删除类接口 |
| `.exchange(...)` | 自定义处理 | 原始访问 |

> 💡 泛型响应（`Page<Order>`、`List<Order>`）必须用 `ParameterizedTypeReference`——直接 `.body(Page.class)` 会丢失泛型（转换器拿到裸类型报错或类型不符）。

## 3. retrieve vs exchange

| 方式 | 语义 | 适用 |
|------|------|------|
| `.retrieve()` | 声明式：自动状态码检查 + 转换器链 + 错误映射 | ✅ 90% 场景 |
| `.exchange()` | 原始访问：拿到完整 `ClientHttpResponse`（头/流/手动绑定） | 特殊需求（读头、流式、多格式协商） |

```java
// exchange 示例：响应信息在 header（已知限制：转换器链只在有 body 时触发）
String result = client.get().uri("/orders/{id}", id)
        .exchange((request, response) -> {
            String version = response.getHeaders().getFirst("X-Api-Version");
            if (response.getStatusCode().is2xxSuccessful()) {
                return version + ":" + new String(response.getBody().readAllBytes(), UTF_8);
            }
            throw new MyServiceException(response.getStatusCode(), version);
        });
```

> ⚠️ exchange 里必须自己**消费或关闭响应体**（否则连接不释放）；状态码判断也自己做——它是"逃逸舱口"，别默认用。

### 3.1 调用内部机制：拦截器链与响应处理

```text
一次 .retrieve() 调用的内部流程：
  1. 构建 HttpRequest（方法/URI/头）
  2. 请求拦截器链（ClientHttpRequestInterceptor，先注册先执行）
  3. 请求工厂发送（HttpComponents / JDK HttpClient / 其他）
  4. 状态码检查：defaultStatusHandler / onStatus（默认 4xx/5xx 抛 RestClientResponseException）
  5. 响应转换：按 Content-Type 选转换器 → 反序列化为目标类型
  6. 返回（toEntity/toBodilessEntity/body 三出口）
```

| 环节 | 扩展点 | 典型用途 |
|------|--------|---------|
| 请求拦截器 | `ClientHttpRequestInterceptor` | 鉴权头注入、traceId 透传、日志、限流 |
| 请求工厂 | `ClientHttpRequestFactory` | 连接池/超时/HTTP2/代理 |
| 状态码处理 | `onStatus` / `defaultStatusHandler` | 业务错误映射为领域异常 |
| 转换器 | `configureMessageConverters` | 自定义协议格式 |

```java
// 拦截器示例：全链路 traceId 透传
restClient.requestInterceptor((request, body, execution) -> {
    request.getHeaders().set("X-Trace-Id", TraceContext.currentId());
    return execution.execute(request, body);
});
```

> 💡 面试被问"RestClient 怎么实现统一鉴权/日志"——答**拦截器链**：它包裹在请求工厂之外、转换器之前，一次注册全局生效。

## 4. WebClient：响应式客户端

```java
// 响应式客户端（WebFlux 栈标配）
WebClient client = WebClient.builder()
        .baseUrl("https://api.example.com")
        .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
        .build();

// 同步式调用（.block() —— 服务端线程内谨慎使用）
PaymentResult result = client.post()
        .uri("/v1/charge")
        .bodyValue(req)
        .retrieve()                    // ResponseSpec
        .bodyToMono(PaymentResult.class)
        .block(Duration.ofSeconds(5)); // 显式超时

// 纯响应式：返回 Mono/Flux，由调用方订阅
Mono<PaymentResult> mono = client.get()
        .uri("/v1/orders/{id}", id)
        .retrieve().bodyToMono(PaymentResult.class);
```

| 对比 | RestClient | WebClient |
|------|-----------|-----------|
| 模型 | 阻塞（同步返回） | 非阻塞（Mono/Flux） |
| 流式 | 不支持（规划中） | ✅ 原生支持（SSE/大流） |
| 线程 | 平台线程/虚拟线程 | 事件循环（Netty） |
| 混用 | 服务端线程内安全 | 服务端线程内 `.block()` 有死锁风险 |
| 选型 | 常规服务间调用 | 响应式链路、流式、网关 |

> 💡 7.x 大方向：虚拟线程成熟后"阻塞式也扛并发"——**常规同步服务用 RestClient + 虚拟线程**已是 Boot 4 主流姿势（见 [07-异步与流式速查](07-异步与流式速查.md)）。

### 4.1 WebClient 使用边界与 .block() 陷阱

| 场景 | 用法 | 风险 |
|------|------|------|
| 响应式服务内部 | `bodyToMono(...)` 返回给调用方 | 无 |
| 非响应式服务调用 | `.block(Duration)` | 阻塞调用线程——服务端线程内慎用 |
| 流式消费 | `bodyToFlux(...)` + subscribe | 必须处理取消与错误信号 |
| MVC 服务内调用 | 用 RestClient + 虚拟线程替代 | WebClient 在这里没有优势 |

```java
// 危险写法：事件循环线程上 block —— 可能死锁
// .block() 占住事件循环线程等响应，而响应需要该线程调度 → 超时才解开
Mono.delay(Duration.ofSeconds(2))            // 需要事件循环调度
    .block(Duration.ofSeconds(5));            // 在事件循环线程上阻塞

// 正确：全部响应式，由调用方订阅
return service.callRemote().flatMap(x -> ...);
```

**RestClient vs WebClient vs JDK HttpClient 三选一**：

| 维度 | RestClient | WebClient | JDK HttpClient |
|------|-----------|-----------|----------------|
| 生态 | Spring 全面集成（拦截器/转换器） | Spring 全面集成 | 零依赖原生 |
| 学习成本 | 低 | 高（响应式） | 中 |
| 流式 | 不支持 | ✅ | 部分（异步流） |
| 服务发现 | 无（对接 Spring Cloud 组件） | 同 | 无 |
| 适用 | 常规服务间调用 | 响应式链路 | 工具脚本、轻量场景 |

> 🎯 **取舍结论**：RestClient（同步）与 WebClient（响应式）不是"谁更好"，而是"你的链路是什么形态"——链路同步则 RestClient + 虚拟线程；链路响应式则全链 WebClient；**混搭点（同步服务里 .block()）是事故高发区**。

### 4.2 WebClient 高级用法：流式、超时与重试

```java
// 流式消费 SSE（WebClient 强项）
Flux<ServerSentEvent<Order>> events = webClient.get()
        .uri("/orders/stream")
        .retrieve()
        .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<Order>>() {});

events
        .timeout(Duration.ofSeconds(30))                      // 整体超时
        .retryWhen(Retry.backoff(3, Duration.ofMillis(200)))  // 断线退避重试
        .doOnError(e -> log.warn("事件流中断", e))
        .subscribe(event -> handle(event.data()));            // 订阅即消费

// 过滤器：统一日志/鉴权（对应 RestClient 的拦截器）
WebClient client = WebClient.builder()
        .baseUrl("https://api.example.com")
        .filter(ExchangeFilterFunctions.basicAuthentication("user", "pass"))
        .filter((req, next) -> {
            log.info("{} {}", req.method(), req.url());
            return next.exchange(req);
        })
        .build();
```

| WebClient 高级能力 | 说明 |
|--------------------|------|
| ExchangeFilterFunction | 对应 RestClient 拦截器的响应式版 |
| bodyToFlux + 背压 | 消费侧慢则上游减速（Reactor 原生） |
| Retry.backoff | 断线重连（SSE 长连接场景标配） |
| timeout 操作符 | 单次/整体超时控制 |
| 取消 | Disposable.dispose() 停止订阅 |

> ⚠️ 流式消费的常见 bug：**不订阅（subscribe 缺失）管道不执行**——响应式是"声明 + 订阅"两段式；还有订阅后异常未处理（doOnError 缺失）导致静默中断。

## 5. 从 RestTemplate 迁移

```java
// 旧：RestTemplate
// RestTemplate tpl = new RestTemplate();
// PaymentResult r = tpl.postForObject(url, req, PaymentResult.class);

// 新：RestClient（API 一一对应）
RestClient client = RestClient.create(restTemplate);   // ★ 包装旧实例平滑过渡
PaymentResult r = client.post().uri(url).body(req).retrieve().body(PaymentResult.class);
```

| RestTemplate 方法 | RestClient 对应 |
|-------------------|-----------------|
| `getForObject` / `getForEntity` | `.get().uri(...).retrieve().body(...)` / `.toEntity(...)` |
| `postForObject` / `postForEntity` | `.post().uri(...).body(...).retrieve()...` |
| `exchange(url, method, entity, Type)` | `.method(HttpMethod.X)...exchange(...)` 或 `.retrieve()` |
| `setInterceptors` | `.requestInterceptor(...)` / `.defaultRequest(...)` |
| `setErrorHandler` | `.defaultStatusHandler(...)` / `onStatus(...)` |
| `setMessageConverters` | `.configureMessageConverters(...)`（7.0） |

> 🎯 **核心要点**：迁移是"声明式化"——RestTemplate 把配置放在对象上，RestClient 把配置放在请求链上；**同一请求工厂/拦截器/转换器基础设施**保证迁移零行为差异（官方为此设计 `RestClient.create(existingRestTemplate)` 包装器）。

### 5.1 迁移清单与兼容性对照

| 迁移项 | 旧（RestTemplate） | 新（RestClient） | 备注 |
|--------|-------------------|-----------------|------|
| URL 模板变量 | `getForObject(url, T, vars)` | `.uri(url, vars)` | 变量自动编码 |
| 请求头 | HttpHeaders + HttpEntity | `.header(...)` / `.headers(Consumer)` | 链式更直观 |
| 错误映射 | 自定义 `ResponseErrorHandler` | `onStatus` / `defaultStatusHandler` | 声明式 |
| 泛型响应 | `ParameterizedTypeReference` | 同 | 一致 |
| 异步 | `AsyncRestTemplate`（已移除） | WebClient / JDK HttpClient | 无同步等价 |
| 重试 | 自研/无 | 拦截器或重试模板组合 | 无内建重试 |

```java
// 过渡技巧：旧服务先不换 API，内部委托给 RestClient
public class LegacyTemplateFacade {
    private final RestClient restClient;
    public LegacyTemplateFacade(RestClient restClient) { this.restClient = restClient; }

    public <T> T getForObject(String url, Class<T> type, Object... vars) {
        return restClient.get().uri(url, vars).retrieve().body(type);
    }
    // 旧调用方零改动，底层已切 RestClient
}
```

> 💡 迁移是"渐进式"的：先 `RestClient.create(existingRestTemplate)` 包装过渡，再逐个方法换链式写法，最后删包装层——**每个步骤都可单独验证行为一致**。

### 5.2 迁移验收清单（行为等价对照）

| 检查项 | 方法 | 验收标准 |
|--------|------|---------|
| 请求头/URI 一致 | 对比两客户端请求日志 | 头集合、URL 编码结果完全一致 |
| 状态码处理一致 | 用 404/500 mock 响应对比 | 抛出的异常类型与消息一致 |
| 转换一致 | 同一响应体双客户端反序列化 | 对象字段值逐项相等 |
| 超时行为 | 慢响应 mock | 超时时间与异常一致 |
| 重试/拦截器 | 注入计数拦截器 | 触发次数一致 |

```text
迁移节奏建议：
  阶段 1：RestClient.create(existingRestTemplate) 全量包装（零改动上线）
  阶段 2：逐个服务换链式 API（每换一个跑一遍验收清单）
  阶段 3：删除包装层 + 清理 RestTemplate 引用
  预期：每阶段独立上线，风险可控
```

> 🎯 **迁移的最终目标**：不是"代码长一样"，而是"行为可等价验证"——用验收清单替代"感觉没问题"，是生产迁移的底线。

## 6. 7.0 新能力：ApiVersionInserter 与 HTTP 接口

```java
// ① 客户端 API 版本化：自动注入版本（头/媒体类型/路径/查询参数）
RestClient client = RestClient.builder()
        .baseUrl("https://api.example.com")
        .defaultVersion(ApiVersion.V1)              // 客户端声明当前版本
        .apiVersionInserter(new HeaderApiVersionInserter("X-Api-Version"))
        .build();

// ② HTTP 接口（声明式远程调用）：@HttpExchange 接口
@HttpExchange("/v1/orders")
public interface OrderClient {
    @GetExchange("/{id}")
    Order getById(@PathVariable Long id);
}

// ③ 批量声明接口组（7.0）：共享同一 RestClient
@Configuration
@ImportHttpServices(group = "orders", types = {OrderClient.class, OrderEventClient.class})
public class HttpClientsConfig { }
```

| 能力（7.0） | 说明 |
|------------|------|
| `ApiVersionInserter` | 版本注入策略：`HeaderApiVersionInserter` / `MediaTypeApiVersionInserter` / `PathApiVersionInserter` / `QueryParamApiVersionInserter` |
| `@HttpExchange` / `@GetExchange` 等 | 声明式 HTTP 接口（`HttpServiceProxyFactory` 生成代理） |
| `@ImportHttpServices` | 批量注册接口组（7.0） |
| `RestTestClient` | 统一测试客户端（真实服务器 + mock 双模式） |

> 💡 面试加分：HTTP 接口客户端 = "OpenFeign 的 Spring 原生平替"——同声明式代理模型（对比 Spring Cloud OpenFeign 系列：Spring 原生无服务发现、轻量；Feign 集成注册中心/负载均衡）。

### 6.1 ApiVersionInserter 原理与接口组细节

```text
ApiVersionInserter 工作链路（客户端侧）：
  请求构建 → 读取客户端 defaultVersion → 按插入器实现注入版本
  四种实现：
    HeaderApiVersionInserter       → X-Api-Version: v1（头）
    MediaTypeApiVersionInserter    → Accept: application/vnd.api.v1+json（媒体类型）
    PathApiVersionInserter         → /v1/orders/...（路径前缀）
    QueryParamApiVersionInserter   → ?api-version=v1（查询参数）
  服务端配合：MVC 的 ApiVersioningRequestCondition 按版本路由
  （详见 SpringMVC 系列 API 版本化文档）
```

| HTTP 接口组（7.0）要点 | 说明 |
|------------------------|------|
| `@ImportHttpServices` | 批量注册共享同一 RestClient 的接口 |
| 组内接口 | 全部用相同 baseUrl/拦截器/转换器 |
| 与单接口区别 | 单接口用 `HttpServiceProxyFactory.createClient`；组用注解声明 |
| 测试 | `RestTestClient` 支持 mock 与真实服务器双模式 |

| 面试追问 | 回答要点 |
|----------|---------|
| RestClient 与 OpenFeign 区别 | 都是声明式代理；Feign 依赖注册中心/负载均衡，Spring 原生无此绑定、轻量 |
| 版本注入为什么在客户端做 | 服务多版本并行时，客户端显式声明"我要调哪个版本" |
| 什么时候用接口组 | 同一服务方多个接口客户端时，配置共享 |
| 无响应体的调用怎么拿状态码 | `.retrieve().toBodilessEntity()` 或 `.toEntity(Void.class)` |
| 超大响应怎么处理 | 流式走 WebClient bodyToFlux；同步用 exchange 读流 |
| 怎么给 RestClient 加超时 | 请求工厂（HttpComponents/JDK）设置 connect/read 超时（见 2.1） |
| 客户端连不上会抛什么 | `ResourceAccessException`（连接层），可区分 DNS/连接/超时子类型 |
| 声明式接口的方法参数有哪些可用 | @PathVariable/@RequestBody/@RequestHeader/@RequestParam——与控制器注解同族 |
| 接口客户端能复用服务端 DTO 吗 | 能——声明式接口不绑定服务端实现，DTO 是共享契约 |
| onStatus 与 defaultStatusHandler 什么关系 | 前者按条件覆盖后者——default 是全局兜底，onStatus 是精准分支 |
| 表单提交怎么发 | `.body(MultiValueMap<String,String>)`——FormHttpMessageConverter 自动转表单编码 |
| 响应体是 JSON 数组怎么绑定 | `ParameterizedTypeReference<List<Order>>` 或记录数组类型（泛型必须带全） |
| defaultUriVariables 与请求级变量怎么共存 | 默认变量被请求级同名变量覆盖——越具体越优先 |

> 🎯 **本模块一句话**：四选一（RestClient 主推）→ 全 API（链式）→ 迁移（渐进包装）→ 新能力（版本化/接口组）——HTTP 客户端的答案都在这一篇。

### 6.2 RestTestClient 与测试实践（7.0）

```java
// RestTestClient：真实服务器集成测试与 mock 测试双模式
RestTestClient client = RestTestClient.bindTo(restClient);   // 绑定被测客户端

// 模式一：mock 响应（不启服务器）
client.get().uri("/v1/orders/1")
        .exchange().expectStatus().isOk()
        .expectBody(Order.class).isEqualTo(expectedOrder);

// 模式二：绑定真实服务器（Boot 随机端口）
@SpringBootTest(webEnvironment = RANDOM_PORT)
class OrderClientTest {
    @Test
    void getById() {
        restTestClient.get().uri("/v1/orders/{id}", 1)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON);
    }
}
```

**客户端测试要点**：

| 测试类型 | 工具 | 说明 |
|----------|------|------|
| 单元（mock 响应） | RestTestClient mock 模式 / MockRestServiceServer | 不启动服务器 |
| 集成（真实服务器） | RestTestClient 绑定 + @SpringBootTest | 全链路验证 |
| 契约测试 | 契约框架（如 Pact） | 跨团队接口一致性 |

> 💡 与 `MockRestServiceServer` 的关系：6.x 时代的客户端 mock 工具，7.0 的 `RestTestClient` 是官方统一替代（覆盖 mock + 真实双模式）——新测试代码用 RestTestClient。

---

**下一模块**：[03-消息转换与内容协商速查](03-消息转换与内容协商速查.md)　**返回总览**：[00-Spring Web组件总览](00-Spring Web组件总览.md)
