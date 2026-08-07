# 04 数据绑定与 HTTP 消息速查

> HttpHeaders/RequestEntity/ResponseEntity、UriComponentsBuilder、MediaType——HTTP 消息的对象模型速查

---

## 📚 目录

1. [HTTP 消息四件套](#1-http-消息四件套)
2. [HttpHeaders 速查](#2-httpheaders-速查)
3. [UriComponentsBuilder：URI 构建](#3-uricomponentsbuilderuri-构建)
4. [MediaType 与 MimeType](#4-mediatype-与-mimetype)
5. [请求/响应实体：RequestEntity 与 ResponseEntity](#5-请求响应实体requestentity-与-responseentity)

---

## 1. HTTP 消息四件套

| 类型 | 内容 | 用途 |
|------|------|------|
| `HttpMessage` | 头部（HttpHeaders） + 体（字节） | 抽象基类 |
| `HttpHeaders` | 头字段集合（不区分大小写） | 构建/读取头 |
| `HttpEntity<T>` | 头 + 任意类型体 | 通用实体（模板时代常用） |
| `RequestEntity<T>` | HttpEntity + 方法 + URI | 客户端请求表达 |
| `ResponseEntity<T>` | HttpEntity + 状态码 | 服务端响应/客户端接收 |

```java
// 服务端返回（MVC 控制器）
@GetMapping("/orders/{id}")
public ResponseEntity<Order> get(@PathVariable Long id) {
    return ResponseEntity.ok()
            .header("X-Trace-Id", traceId)
            .body(orderService.findById(id));
}
```

> 🎯 **核心要点**：`ResponseEntity` 是"状态码 + 头 + 体"三合一的响应表达——**要自定义状态码/响应头时用它**；纯业务数据直接返回对象即可（MVC 自动 200 + 转换器）。

### 1.1 四件套选型场景

| 场景 | 用哪个 | 原因 |
|------|--------|------|
| 控制器返回业务数据 | 直接返回对象 | MVC 自动 200 + 转换器 |
| 需要自定义状态码/头 | `ResponseEntity<T>` | 状态码 + 头 + 体一体 |
| 客户端构建完整请求 | `RequestEntity<T>` | 方法 + URI + 头 + 体 |
| 只传头 + 体（无方法语义） | `HttpEntity<T>` | RestTemplate 老 API 常见 |
| 只想读/写头 | `HttpHeaders` | 轻量，无体 |
| 转换器 SPI 内部 | `HttpInputMessage` / `HttpOutputMessage` | 读写字节流 |

```java
// HttpEntity 用法（模板时代常见，现多用 RequestEntity）
HttpEntity<PaymentReq> entity = HttpEntity
        .ok(req)                                        // 快捷状态
        .header("X-Idempotency-Key", key);

// 消息体系是转换器的输入输出契约
HttpInputMessage input;     // read() 的入参：头 + 字节流
HttpOutputMessage output;   // write() 的出参：头 + 字节流
```

> 💡 面试小点：`HttpMessage` 只有"头 + 体"两个概念；`RequestEntity` 在它之上加"方法 + URI"，`ResponseEntity` 加"状态码"——**继承链就是 HTTP 语义的累加**。

### 1.2 HttpMessage 体系源码级结构

```text
HttpMessage（接口）
├── HttpHeaders headers   头字段集合（大小写不敏感，多值有序）
└── 抽象子类
    ├── HttpEntity<T>     头 + 泛型体（体由转换器读写）
    │   ├── RequestEntity<T>   + HttpMethod + URI
    │   └── ResponseEntity<T>  + HttpStatusCode
    └── 底层消息（转换器 SPI 用）
        ├── HttpInputMessage   接口：read() 的入参
        ├── HttpOutputMessage  接口：write() 的出参
        └── ServletServerHttpRequest/Response（容器适配实现）

关键设计：头与体分离、泛型体、状态码独立——
"HTTP 语义的每一部分都可单独操作"
```

| 源码细节 | 说明 |
|----------|------|
| 状态码 | 6.x 起用 `HttpStatusCode` 接口（数字 + 语义） |
| 泛型体 | `HttpEntity<T>` 的 T 由转换器按 ResolvableType 解析 |
| 不可变性 | RequestEntity/ResponseEntity 构造后不可改（复制构造） |
| 便捷静态方法 | `ResponseEntity.ok()/created()/noContent()` 等链式入口 |

> 💡 面试加分：`ResponseEntity` 的泛型 T 与转换器的 `GenericHttpMessageConverter` 是配套设计——**泛型信息从实体一路传到转换器**（ResolvableType），List/Page 等复杂类型才能精确转换。

## 2. HttpHeaders 速查

```java
HttpHeaders headers = new HttpHeaders();
headers.setContentType(MediaType.APPLICATION_JSON);
headers.setAccept(List.of(MediaType.APPLICATION_JSON));
headers.setBearerAuth(token);                 // Authorization: Bearer xxx
headers.set("X-Custom", "value");
headers.setContentLength(1024);
headers.setCacheControl(CacheControl.noCache());

// 读取（不区分大小写）
String auth = headers.getFirst(HttpHeaders.AUTHORIZATION);
List<String> setCookie = headers.get(HttpHeaders.SET_COOKIE);
```

| 便捷方法 | 含义 |
|---------|------|
| `setContentType` / `setAccept` | 内容类型/接受类型 |
| `setBearerAuth` | Bearer token |
| `setBasicAuth` | Basic 认证 |
| `setContentLength` / `setContentDisposition` | 长度/附件（下载名） |
| `getFirst` / `get` / `containsKey` | 读取（大小写不敏感） |

> 💡 7.x 修复：`HttpHeaders` 与 `WebSocketHttpHeaders` 的互操作（跨模块头字段一致）在 7.0.x 已收敛——头字段读写行为全栈统一。

### 2.1 HttpHeaders 完整方法速查

| 分类 | 方法 | 说明 |
|------|------|------|
| 内容 | `setContentType` / `setContentLength` | 请求/响应体元信息 |
| 协商 | `setAccept` / `setAcceptLanguage` | 声明可接受的类型/语言 |
| 认证 | `setBearerAuth` / `setBasicAuth` / `setAuthorization` | 认证头 |
| 缓存 | `setCacheControl` / `setExpires` / `setLastModified` | HTTP 缓存 |
| 条件请求 | `setIfNoneMatch` / `setIfModifiedSince` / `setETag` | 304 协商 |
| 会话 | `setCookie` / `getCookies` / `setLocation` | Cookie / 重定向 |
| 范围 | `setRange` / `setAcceptRanges` | 断点续传（206） |
| 读取 | `getFirst` / `get` / `containsKey` / `getOrDefault` | 大小写不敏感 |

```java
// 条件请求示例（缓存协商）
headers.setETag("\"abc123\"");                        // ETag 必须带引号
headers.setCacheControl(CacheControl.maxAge(60));     // Cache-Control: max-age=60
headers.setLastModified(System.currentTimeMillis());

// 读取：大小写不敏感 + 多值
String etag = headers.getFirst(HttpHeaders.IF_NONE_MATCH);
List<String> cookies = headers.get(HttpHeaders.SET_COOKIE);   // 多值头
```

> ⚠️ **ETag 引号坑**：`setETag` 期望的是带引号的 `"abc123"`——不带引号会被服务端比较逻辑误判；`set-cookie` 是**多值语义**（追加用 `add`，`set` 会覆盖）。

### 2.2 头字段的安全与透传实践

| 实践 | 说明 |
|------|------|
| 敏感头只透传内网 | Authorization/Cookie 不应透传到第三方（换签名/内部 token） |
| traceId 全链路透传 | 请求进 → 生成 traceId → 响应头带回 → 客户端续传 |
| 头大小限制 | 容器默认限制请求头大小（Tomcat maxHttpHeaderSize）——超长头报 400 |
| 伪造来源头 | X-Forwarded-For 可伪造——信任前必须经可信代理剥离 |
| 响应头安全 | `X-Content-Type-Options: nosniff`、CSP 等由安全组件统一加 |

```java
// traceId 透传示例（客户端）
restClient.requestInterceptor((request, body, execution) -> {
    request.getHeaders().set("X-Trace-Id", TraceContext.currentId());
    return execution.execute(request, body);
});
```

> 💡 头字段是"跨服务契约"的一部分——**接口文档里要列全自定义头**（traceId/幂等键/版本号），否则排查问题靠猜。

### 2.3 缓存控制深入

| 头 | 语义 | 典型值 |
|----|------|--------|
| `Cache-Control` | 缓存策略（7.x 用 `CacheControl` 构建器） | `max-age=60, no-cache, no-store` |
| `ETag` / `If-None-Match` | 内容指纹协商 | 服务端算哈希，客户端回传 |
| `Last-Modified` / `If-Modified-Since` | 时间戳协商 | 秒级精度 |
| `Expires` | 旧式过期时间（被 Cache-Control 取代） | 少用 |
| `Vary` | 缓存键依据（按哪个头区分缓存） | `Accept-Encoding` |

```java
// 服务端缓存协商示例（MVC）
@GetMapping("/report")
public ResponseEntity<String> report(HttpServletRequest req) {
    String etag = "\"" + content.hashCode() + "\"";
    if (etag.equals(req.getHeader(HttpHeaders.IF_NONE_MATCH))) {
        return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();   // 304
    }
    return ResponseEntity.ok().eTag(etag).body(content);
}
```

> ⚠️ **缓存与安全**：`Cache-Control: no-store` 用于敏感数据（token/个人信息）——只 `no-cache` 仍可能落盘；304 协商的前提是请求携带 `If-None-Match`，客户端（如 RestClient）默认不自动携带，需显式设置。

## 3. UriComponentsBuilder：URI 构建

```java
// ① 链式构建
URI uri = UriComponentsBuilder.fromPath("/orders/{id}")
        .queryParam("status", "PAID")
        .queryParam("from", "2026-01-01")
        .buildAndExpand(1001L)              // 路径变量替换
        .toUri();                            // → /orders/1001?status=PAID&from=...

// ② 完整 URL（含 base + path + 编码）
URI full = UriComponentsBuilder.fromUriString("https://api.example.com")
        .path("/v1/orders")
        .queryParam("page", 1)
        .encode()
        .build().toUri();

// ③ RestClient 内联
client.get().uri(b -> b.path("/orders/{id}")
        .queryParam("fields", "id,amount")
        .build(1001L));
```

| 能力 | 说明 |
|------|------|
| 路径变量 | `{id}` + `buildAndExpand(...)` |
| 查询参数 | `queryParam(name, value...)`（多值自动逗号/重复） |
| 编码 | `.encode()` 防注入（URL 编码） |
| 模板 | `UriTemplate` / `fromUriString` |
| 服务端 | `ServletUriComponentsBuilder`（基于当前请求重建） |

> ⚠️ **安全要点**：拼接用户输入进 URI 必须走 `queryParam` + `.encode()`——**字符串拼 URL 是注入漏洞**（`?redirect=` 场景尤甚）；路径变量用 `buildAndExpand` 自动编码。

### 3.1 URI 编码模式与 ServletUriComponentsBuilder

**两种编码模式**（`encode()` 的参数，7.x 常见面试点）：

| 模式 | 时机 | 行为 |
|------|------|------|
| `encode()`（默认） | 构建时整体编码 | 模板结构与变量统一编码，安全第一 |
| `encode(EncodingMode.VALUES_ONLY)` | 仅值编码 | 保留模板结构，只编码变量值 |

```java
// VALUES_ONLY 典型场景：路径模板可信，只有变量来自用户
URI uri = UriComponentsBuilder
        .fromPath("/search/{keyword}")
        .queryParam("q", userInput)
        .encode(EncodingMode.VALUES_ONLY)   // 只编码变量值
        .buildAndExpand(keyword)
        .toUri();

// ServletUriComponentsBuilder：基于当前请求重建（服务端重定向常用）
URI next = ServletUriComponentsBuilder.fromCurrentRequest()
        .replaceQueryParam("page", page + 1)
        .build().toUri();                    // 保留协议/host/path，替换查询参数
```

**字符串拼接反例**（经典安全题）：

```java
// ❌ 危险：用户输入直接进 URL，& 可篡改其他参数
String url = "https://api.com/q?keyword=" + userInput;

// ✅ 正确：queryParam 自动编码
URI url = UriComponentsBuilder.fromUriString("https://api.com/q")
        .queryParam("keyword", userInput)
        .build().toUri();
```

> 🎯 **一句话**：`UriComponentsBuilder` 的职责是"结构 + 编码"两件事——**结构你负责（path/queryParam），编码它负责（encode）**；任何时候不要让用户输入以裸字符串形态进 URL。

### 3.2 UriTemplate 与路径匹配

| 能力 | 说明 |
|------|------|
| `UriTemplate` | 独立的模板解析类：`{id}`、`{name:regex}`（正则约束） |
| `expand(...)` | 变量填充（编码由调用方决定） |
| `match(...)` | 反向匹配：解析已存在的 URL 得到变量值 |
| 服务端 `@PathVariable` | 由 HandlerMapping 按模板匹配（webmvc 层） |

```java
// UriTemplate：模板 + 正则约束
UriTemplate tpl = new UriTemplate("/orders/{id:\\d+}");   // id 必须纯数字
Map<String, String> vars = tpl.match("/orders/1001");     // {id=1001}
String uri = tpl.expand(1001).toASCIIString();            // /orders/1001

// 路径变量与查询参数混用
URI uri = UriComponentsBuilder
        .fromPath("/v1/orders/{id}")
        .queryParam("fields", "id,status")
        .queryParam("verbose", true)       // 布尔值自动转 "true"
        .buildAndExpand(orderId)
        .toUri();
```

> 💡 服务端 `@GetMapping("/orders/{id}")` 的 `{id}` 与客户端 `UriComponentsBuilder` 的 `{id}` 是**同一套模板语法**——只是"匹配"（服务端）与"填充"（客户端）两个方向。

## 4. MediaType 与 MimeType

```java
// 常用常量
MediaType.APPLICATION_JSON       // application/json
MediaType.APPLICATION_XML
MediaType.APPLICATION_FORM_URLENCODED
MediaType.MULTIPART_FORM_DATA
MediaType.TEXT_PLAIN / TEXT_HTML

// 解析与比较
MediaType mt = MediaType.parseMediaType("application/json;charset=UTF-8");
mt.isCompatibleWith(MediaType.APPLICATION_JSON);   // true（含参数忽略比较）
mt.includes(MediaType.parseMediaType("application/*"));  // 通配包含
```

| 方法 | 语义 |
|------|------|
| `isCompatibleWith` | 类型兼容（忽略参数/通配） |
| `includes` | 包含关系（`application/*` 包含 `application/json`） |
| `getCharset` / `getParameters` | 参数读取 |
| `sortBySpecificity` | 媒体类型特异度排序（协商用） |

> 💡 面试小点：MimeType 是通用 MIME（无参数语义），MediaType 是 HTTP 专用（带 charset 等参数）——转换器匹配用 MediaType。

### 4.1 MediaType 细节与解析异常

| 要点 | 说明 |
|------|------|
| 通配符 | `application/*`、`*/*`、`text/*;q=0.5` 均合法 |
| 参数 | `application/json;charset=UTF-8` —— charset 是唯一"标准"参数 |
| 大小写 | 类型/子类型不区分大小写，参数值区分 |
| 比较 | `isCompatibleWith` 忽略参数；`includes` 检查包含关系 |
| 排序 | `sortBySpecificity` / `sortByQualityValue` 用于协商 |

```java
// 解析异常处理
try {
    MediaType.parseMediaType("application/json; charset=UTF-8");
} catch (InvalidMediaTypeException e) {
    // 非法字符/未闭合引号等解析失败
    log.warn("非法媒体类型: {}", raw);
}

// 协商判断（客户端常见）
boolean canNegotiate = acceptTypes.stream()
        .anyMatch(a -> a.isCompatibleWith(producedType));
```

> ⚠️ **charset 陷阱**：`MediaType.APPLICATION_JSON` 常量不带 charset；`application/json;charset=UTF-8` 与它 `isCompatibleWith` 为 true，但 `equals` 为 false——**用 equals 比较媒体类型是常见 bug 源头**，转换器匹配用的是兼容比较。

### 4.2 MediaType 生产实战

| 场景 | 写法 | 注意 |
|------|------|------|
| 响应 JSON | `MediaType.APPLICATION_JSON` | 常量即 `application/json` |
| SSE 流 | `MediaType.TEXT_EVENT_STREAM` | 控制器 produces 用它 |
| 文件下载 | `MediaType.APPLICATION_OCTET_STREAM` + ContentDisposition | 附件名要编码 |
| 表单提交 | `MediaType.APPLICATION_FORM_URLENCODED` | MultiValueMap 载体 |
| 图片 | `MediaType.IMAGE_PNG` 等 | 静态资源由容器处理 |
| 自定义 vendor 类型 | `application/vnd.api+json`（JSON:API 风格） | 版本化/平台化 API 常见 |

```java
// 文件下载的完整表达（附件名中文编码）
return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .header(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment()
                        .filename("对账单.csv", StandardCharsets.UTF_8)  // RFC 5987 编码
                        .build().toString())
        .body(bytes);
```

> ⚠️ 中文文件名必须走 `ContentDisposition.filename(name, UTF_8)`——裸拼接会乱码；浏览器解析附件名有跨浏览器差异，统一用 RFC 5987 编码最稳。

### 4.3 MediaType 与内容协商实战

| 协商环节 | 谁定 | 示例 |
|----------|------|------|
| 客户端期望 | `Accept` 头（RestClient `.accept(...)`） | `application/json;q=0.9, application/xml;q=0.5` |
| 服务端能力 | `produces` 注解声明 | `@GetMapping(produces = "application/json")` |
| 交集计算 | `isCompatibleWith` + q 值排序 | 无交集 → 406 |
| 响应定型 | 选中转换器 + `Content-Type` 头 | `application/json` |

```java
// 客户端协商实战：同接口两种格式
ResponseEntity<String> xml = restClient.get()
        .uri("/v1/orders/{id}", id)
        .accept(MediaType.APPLICATION_XML)         // 声明要 XML
        .retrieve().toEntity(String.class);         // 拿原始 XML 文本

ResponseEntity<Order> json = restClient.get()
        .uri("/v1/orders/{id}", id)
        .accept(MediaType.APPLICATION_JSON)         // 声明要 JSON
        .retrieve().toEntity(Order.class);          // 直接绑定对象
```

> 💡 生产约定：**接口只对外提供一种主流格式**（JSON），XML/其他格式按需开放——协商能力存在，但"多格式契约"维护成本高；服务端用 `produces` 锁定格式可减少 406 类问题的排查面。

## 5. 请求/响应实体：RequestEntity 与 ResponseEntity

```java
// 客户端：RequestEntity 表达完整请求
RequestEntity<PaymentReq> request = RequestEntity
        .post(URI.create("https://api.example.com/v1/charge"))
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .header("X-Idempotency-Key", key)
        .body(req);

// 服务端：ResponseEntity 表达完整响应
@PostMapping("/v1/charge")
public ResponseEntity<PaymentResult> charge(@RequestBody PaymentReq req) {
    PaymentResult result = payService.charge(req);
    return ResponseEntity
            .created(URI.create("/v1/charges/" + result.id()))   // 201 + Location
            .body(result);
}
```

| 状态码场景 | 写法 |
|-----------|------|
| 200 + 数据 | `ResponseEntity.ok(data)` |
| 201 创建 | `ResponseEntity.created(location).body(data)` |
| 204 无内容 | `ResponseEntity.noContent().build()` |
| 404 | `ResponseEntity.notFound().build()` |
| 202 异步接受 | `ResponseEntity.accepted().build()` |

> 🎯 **核心要点**：RequestEntity/ResponseEntity 是"完整 HTTP 语义"的载体——客户端用它表达方法+URI+头+体，服务端用它表达状态码+头+体；RestClient 的 `.retrieve().toEntity(Type)` 返回的就是 ResponseEntity。

### 5.1 状态码场景扩展与面试追问

| 状态码 | 写法 | 场景 |
|--------|------|------|
| 200 | `ResponseEntity.ok(data)` | 常规 |
| 201 | `created(location)` | 资源创建（应带 Location） |
| 202 | `accepted()` | 异步任务受理 |
| 204 | `noContent()` | 删除成功、空更新 |
| 206 | `status(PARTIAL_CONTENT)` + Range 头 | 断点续传 |
| 301/302 | `status(...).location(uri)` | 重定向 |
| 400 | `badRequest().body(ErrorDto)` | 参数错误（带错误体） |
| 401/403 | `status(UNAUTHORIZED)` | 未认证/未授权 |
| 404 | `notFound()` | 资源不存在 |
| 409 | `status(CONFLICT)` | 版本冲突/唯一键冲突 |
| 422 | `status(UNPROCESSABLE_ENTITY)` | 业务校验失败 |
| 500 | `status(INTERNAL_SERVER_ERROR)` | 兜底（一般交异常处理器） |

```java
// 带错误体的 400 响应（前后端契约一致的关键）
return ResponseEntity.badRequest()
        .contentType(MediaType.APPLICATION_JSON)
        .body(new ErrorResponse("E1001", "参数非法", List.of(fieldErrors)));
```

| 面试追问 | 回答要点 |
|----------|---------|
| ResponseEntity 与 @ResponseBody 区别 | ResponseEntity 自带状态码/头；@ResponseBody 只写体（默认 200） |
| @ResponseStatus 与 ResponseEntity 谁优先 | 异常处理器中 @ResponseStatus 生效；方法返回 ResponseEntity 时以其为准 |
| Location 头有什么用 | 客户端按 201 + Location 发 GET 获取新资源（HATEOAS 雏形） |
| 为什么 204 不能有 body | HTTP 规范：204 响应体必须为空，MVC 会丢弃 |
| 错误响应怎么统一 | 全局 @ExceptionHandler + 统一 ErrorResponse 结构（见 MVC 系列） |
| 为什么 201 要带 Location | 资源创建后客户端可立即定位新资源（规范要求） |
| UriComponentsBuilder 线程安全吗 | 是——构建器不持有可变状态，可安全共享 |
| MediaType 与 Content-Type 头什么关系 | 头是字符串形式，MediaType 是其对象化——setContentType 自动序列化 |
| 一个响应能带多个 Content-Type 吗 | 不能——单值头；Accept 才可能多值 |
| UriComponentsBuilder 与 UriTemplate 什么关系 | builder 是"构建器"，template 是"模板解析器"——builder 内部用模板语法表达路径变量 |
| HttpHeaders 为什么大小写不敏感 | HTTP 规范要求头名大小写不敏感——内部统一小写键存储 |
| ResponseEntity 泛型体与转换器怎么协作 | 泛型信息（ResolvableType）从实体传给转换器——List/Page 类型因此可精确反序列化 |
| 文件下载为什么用 octet-stream | 让浏览器走"下载"而非"内联打开"——配合 Content-Disposition 附件语义 |

### 5.2 与数据绑定的联动（HTTP 消息 ↔ 参数绑定）

```text
一条请求的数据流（MVC）：
  请求行/头 → RequestEntity（客户端侧表达）
  → DispatcherServlet 解析 → 参数绑定（@PathVariable/@RequestParam/@RequestBody）
  → 控制器返回值 → ResponseEntity 包装（状态码/头/体）
  → HttpMessageConverter 序列化 → 响应

  数据绑定（04 篇）↔ 消息模型（本篇）的分界：
  绑定 = 请求数据 → 方法参数（webmvc 的 HandlerMethodArgumentResolver）
  消息 = HTTP 结构表达（头/体/状态码）（spring-web 的 HttpMessage 体系）
```

| 结合点 | 说明 |
|--------|------|
| @RequestBody 读取时 | 转换器读 HttpInputMessage → 对象 |
| 返回值写回时 | 对象 → 转换器写 HttpOutputMessage |
| RestClient 收发时 | 客户端用同一套消息模型 |

> 🎯 **本模块一句话**：四件套（HttpMessage/HttpHeaders/RequestEntity/ResponseEntity）是 HTTP 的"对象化表达"——配 UriComponentsBuilder 建 URL、MediaType 定类型、ResponseEntity 定状态码，一次 HTTP 交互的所有结构都有了。

### 5.3 常见坑速查

| 现象 | 根因 | 解法 |
|------|------|------|
| URL 参数中文乱码 | 未 encode 或双重编码 | 统一 `queryParam` + `encode()`，不要手工拼接 |
| 响应头不生效 | 在写体之后 set 头 | 头必须在写体前设置（转换器 write 时头已定型） |
| 附件名乱码 | 文件名未 RFC 5987 编码 | `ContentDisposition.filename(name, UTF_8)` |
| 状态码正确但体为空 | 204/304 语义无体 | 按规范确认；要体换 200 |
| ETag 不匹配 | 引号缺失/值不一致 | `setETag("\"...\"")` 带引号 |
| 重定向丢失头 | 302 后客户端未携带 | 手动处理重定向或用 `followRedirects` 配置 |
| 日期头解析失败 | 非标准格式 | 用 `setDate`/`getFirstDate`（自动 RFC 1123） |
| 多值头被覆盖 | 用了 `set` 而非 `add` | `set-cookie` 等用 `add` 追加 |
| 请求头大小超限 | 容器限制 | 调 `server.tomcat.max-http-header-size`（先查是否真需要大请求头） |
| HttpHeaders 为空对象 | 从 request 拿了新实例 | 注入/传递现有实例；新实例需复制构造 |
| URL 中特殊字符被二次编码 | 已编码值再次 encode | 区分"模板变量需要编码"与"值已编码"——用原始值走 VALUES_ONLY |
| 大响应体内存暴涨 | 对象绑定整读 | 流式消费（exchange 读 InputStream 或 WebClient bodyToFlux） |
| 绑定失败吞异常 | 转换器异常未转 4xx | 抛 `HttpMessageNotReadableException` 交异常处理器转 400 |

> ⚠️ **写体前设置头** 是最隐蔽的坑：`HttpOutputMessage` 一旦开始写流，头集合就被"冻结"——先 setContentType/ContentDisposition 再写字节，顺序反了静默丢失。

### 5.4 快速记忆卡

```text
消息四件套：HttpMessage（头+体）→ HttpEntity（泛型体）
           → RequestEntity（+方法+URI）/ ResponseEntity（+状态码）
头：HttpHeaders（大小写不敏感、多值有序、set/add 语义区分）
URL：UriComponentsBuilder（结构+编码两职责，VALUES_ONLY 精细化）
类型：MediaType（兼容比较 isCompatibleWith，勿用 equals）
状态码：ResponseEntity 便捷方法（ok/created/accepted/noContent/notFound）
```

---

**下一模块**：[05-Web 上下文与作用域速查](05-Web上下文与作用域速查.md)　**返回总览**：[00-Spring Web组件总览](00-Spring Web组件总览.md)
