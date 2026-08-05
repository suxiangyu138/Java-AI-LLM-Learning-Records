# 05 - URL 解析编程实战：Java 与 JavaScript

> 把 URL 相关的 Java/JS API 一次性讲透——`URI` vs `URL` 的陷阱、`UriComponentsBuilder` 最佳实践、OkHttp `HttpUrl`、WHATWG `URL`/`URLSearchParams`、Node.js 端 URL 处理，以及跨语言常见踩坑对照

---

## 📚 目录

1. [Java 端：URI vs URL 的核心差异](#1-java-端uri-vs-url-的核心差异)
2. [URL.equals() 的 DNS 陷阱](#2-urlequals-的-dns-陷阱)
3. [UriComponentsBuilder 实战大全](#3-uricomponentsbuilder-实战大全)
4. [OkHttp HttpUrl 详解](#4-okhttp-httpurl-详解)
5. [Spring 工具集：UriUtils / ServletUriComponentsBuilder](#5-spring-工具集uriutils--servleturicomponentsbuilder)
6. [JavaScript 端：WHATWG URL API](#6-javascript-端whatwg-url-api)
7. [URLSearchParams 完全指南](#7-urlsearchparams-完全指南)
8. [Node.js 端 URL 处理](#8-nodejs-端-url-处理)
9. [跨语言对照速查表](#9-跨语言对照速查表)

---

## 1. Java 端：URI vs URL 的核心差异

### 1.1 概念对照

| 维度 | `java.net.URI` | `java.net.URL` |
|------|---------------|----------------|
| 定位 | 纯**语法**标识符，只管字符串合法性 | 语法 + **协议处理器**，要有办法访问 |
| 构造时行为 | **纯字符串校验**，不联网 | 某些操作会触发 DNS / 连接 |
| 支持 Scheme | 任意合法 scheme | 只支持内置协议处理器（http/https/file/jar/ftp...） |
| `equals` / `hashCode` | 不联网，纯字符串比较 | **会做 DNS 解析 + IP 比对** ❌ |
| 线程安全 | ✅ 不可变对象 | ⚠️ `equals`/`hashCode` 有同步锁 |
| Java 20+ | — | 所有 String 构造器标记 `@Deprecated` |
| 推荐场景 | **解析、拼接、校验、归一化** | 真正要打开连接时 |

```java
// 第一原则：优先用 URI，需要网络 I/O 时才转 URL
URI  uri = URI.create("https://api.example.com/v1/users/42?name=tom");  // ✅
URL  url = uri.toURL();                                                   // ✅
```

### 1.2 URI 三大构造方式

```java
// ① URI.create() —— 解析已编码/已合法的字符串（最常用）
URI u1 = URI.create("https://a.com/path?q=1#f");
//   内部调 new URI(str)，遇到非法字符抛 IllegalArgumentException
//   不会自动编码

// ② new URI(str) —— 与 create() 行为相同，但抛 URISyntaxException（checked）
try {
    URI u2 = new URI("https://a.com/path");
} catch (URISyntaxException e) {
    // 处理
}

// ③ new URI(scheme, authority, path, query, fragment) —— 多参构造，自动编码
URI u3 = new URI("https", "a.com", "/my path", "kw=a b", "frag ment");
System.out.println(u3.toASCIIString());
// https://a.com/my%20path?kw=a%20b#frag%20ment
// ↑ 自动编码了空格等非法字符
```

> ⚠️ **多参构造的自动编码范围有限**：只编码非法字符（空格、中文等），**不编码保留字符**（`&`、`=`、`?`、`#` 等）。所以 Query 值里含 `&` 时仍需先手工编码。

### 1.3 URI 常用方法速查

```java
URI u = URI.create("https://user:pwd@api.example.com:8443/v1/users/42?page=2&size=10#profile");

u.getScheme();        // "https"
u.getAuthority();     // "user:pwd@api.example.com:8443"
u.getUserInfo();      // "user:pwd"
u.getHost();          // "api.example.com"
u.getPort();          // 8443
u.getPath();          // "/v1/users/42"           ← 已解码
u.getRawPath();       // "/v1/users/42"           ← 原始
u.getQuery();         // "page=2&size=10"         ← 已解码
u.getRawQuery();      // "page=2&size=10"         ← 原始
u.getFragment();      // "profile"               ← 已解码
u.getRawFragment();   // "profile"               ← 原始

// 常用组合
u.isAbsolute();       // true（有 scheme）
u.isOpaque();         // false（有 hierarchy）
u.normalize();        // 移除 . 和 ..
u.resolve("next");    // 相对路径解析
u.relativize(u2);     // 计算相对差
u.compareTo(u2);      // 字典序比较（不联网）
```

### 1.4 raw vs decoded 的生死抉择

```java
URI u = URI.create("https://a.com/%E4%B8%AD%E6%96%87/a%2Fb%3Fc?kw=%E4%B8%AD");

// ❌ 解码版 —— 丢失原始结构
u.getPath();        // "/中文/a/b?c" → %2F 变成了真 /，%3F 变成了真 ?
                     // 层级信息完全丢失！

// ✅ 原始版 —— 保真
u.getRawPath();     // "/%E4%B8%AD%E6%96%87/a%2Fb%3Fc"

// ❌ 用 getPath() 做安全校验
if (u.getPath().startsWith("/safe/")) { ... }
// 攻击者：https://a.com/%2e%2e%2fetc%2Fpasswd
// u.getPath() → "/../etc/passwd" → 绕过前缀检查

// ✅ 用 getRawPath() 做安全校验
if (!u.getRawPath().startsWith("/safe/")) { reject; }
```

> 🎯 **铁律**：**做安全校验、路径拼接、签名计算时，一律用 `getRaw*` 系列方法**。`get*` 系列只有在最终展示给用户或用做业务参数值时使用。

---

## 2. URL.equals() 的 DNS 陷阱

这是 Java 后端开发**最著名、代价最大**的 API 设计缺陷之一。

### 2.1 问题根源

```java
// java.net.URL.equals() 的 JDK 源码逻辑：
// 1. 比较两个 URL 的 IP 地址是否相同 → 需要 DNS 解析
// 2. 比较端口是否相同
// 3. 比较路径是否相同

URL u1 = new URL("https://google.com/a");
URL u2 = new URL("https://google.com/a");

// ❌ 每次比较都可能发起 DNS 查询！
boolean same = u1.equals(u2);  // 慢 + 不确定（DNS 轮询返回不同 IP → false）
```

### 2.2 三个致命后果

| 问题 | 说明 | 实际影响 |
|------|------|---------|
| **性能灾难** | `equals()`/`hashCode()` 触发 DNS 查询 | 把 URL 当 Map Key 时，每次 `get` 都是网络 I/O |
| **不确定结果** | DNS 轮询 / CDN 返回不同 IP → 相同逻辑 URL 被判不相等 | `Set<URL>` 里出现重复项 |
| **阻塞风险** | DNS 不可达时 `equals()` 卡住几十秒 | 线程池被打满 |

```java
// ❌ 经典事故场景：URL 当缓存 Key
Map<URL, CachedPage> cache = new HashMap<>();
// 每次 cache.get(key) 都可能触发 DNS 解析，把缓存变成了网络 IO 放大器

// ✅ 正确做法：用 URI 或 String 当 Key
Map<String, CachedPage> cache = new HashMap<>();
cache.put(uri.toASCIIString(), page);
```

### 2.3 正确替代

```java
// ✅ 比大小 / 去重 / Map Key → 用 URI（纯字符串比较）
URI uri1 = URI.create("https://a.com/p");
URI uri2 = URI.create("https://a.com/p");
boolean same = uri1.equals(uri2);             // true，纯字符串比较，不联网
int hash = uri1.hashCode();                   // 只依赖字符串，稳定

// ✅ 需要比较"是否指向同一资源" → 先归一化再比较 String
String norm1 = UrlNormalizer.canonicalize("HTTPS://A.COM:443/p/");
String norm2 = UrlNormalizer.canonicalize("https://a.com/p");
boolean same = norm1.equals(norm2);           // true
```

> ⚠️ **`URI.equals()` 的注意事项**：它对大小写、编码形态**敏感**。`URI.create("https://A.COM/p")` 与 `URI.create("https://a.com/p")` **不相等**。做"指向同一资源"判断前须先归一化。

---

## 3. UriComponentsBuilder 实战大全

Spring 提供的 `UriComponentsBuilder` 是 Java 生态中处理 URL 的最佳工具——按组件编码、支持 URI Template、链式 API。

### 3.1 基础用法

```java
import org.springframework.web.util.UriComponentsBuilder;

// ① 从字符串构建
URI uri = UriComponentsBuilder
        .fromUriString("https://api.example.com")
        .path("/v1/users/{userId}/orders")
        .queryParam("status", "PAID")
        .queryParam("page", 2)
        .queryParam("tag", "hot", "new")       // 重复 key：tag=hot&tag=new
        .fragment("summary")
        .buildAndExpand(Map.of("userId", 42))
        .encode()                               // ← 统一编码
        .toUri();

System.out.println(uri);
// https://api.example.com/v1/users/42/orders?status=PAID&page=2&tag=hot&tag=new#summary
```

### 3.2 编码控制

```java
// ① .encode() —— 整体编码（默认行为）
UriComponentsBuilder.fromUriString("https://a.com")
    .path("/users/张 三")                  // 含中文 + 空格
    .queryParam("kw", "A&B")               // 值含 &
    .encode()
    .toUriString();
// → https://a.com/users/%E5%BC%A0%20%E4%B8%89?kw=A%26B

// ② .build().encode() —— 先用模板变量值替换，再编码
UriComponentsBuilder.fromUriString("https://a.com")
    .path("/users/{name}")
    .buildAndExpand("张 三")
    .encode()
    .toUriString();
// → https://a.com/users/%E5%BC%A0%20%E4%B8%89

// ③ .build(true) —— 变量值已编码（不再二次编码）
UriComponentsBuilder.fromUriString("https://a.com")
    .path("/users/{name}")
    .buildAndExpand(URLEncoder.encode("张 三", UTF_8))   // 变量值已编码
    .encode()                                           // ❌ 双重编码！变成 %25E4...
    .toUriString();

// ✅ 正确写法
UriComponentsBuilder.fromUriString("https://a.com")
    .path("/users/{name}")
    .build(Map.of("name", URLEncoder.encode("张 三", UTF_8)))  // build() 带 Map 不编码
    .toUriString();
```

### 3.3 从 Servlet 请求构建

```java
@GetMapping("/users")
public Page<UserVO> list(HttpServletRequest request, UriComponentsBuilder builder) {

    // ① 从当前请求构建（保留 scheme/host/port/path/query）
    URI nextPage = ServletUriComponentsBuilder.fromRequest(request)
            .replaceQueryParam("page", nextPageNum)
            .build()
            .toUri();

    // ② 从当前上下文构建（用作 Location 头）
    URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(newUser.getId())
            .toUri();

    // ③ 从 Controller 映射构建（不受实际请求干扰）
    URI link = MvcUriComponentsBuilder.fromMethodName(UserController.class, "get", 42L)
            .build()
            .toUri();
    // → http://localhost:8080/api/v1/users/42
}
```

### 3.4 批量替换 Query 参数

```java
// 场景：翻页 / 过滤时只改一两个参数，保留其他参数

UriComponentsBuilder builder = ServletUriComponentsBuilder.fromRequest(request);

// 替换单个参数
builder.replaceQueryParam("page", "3");

// 替换多个参数
MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
params.add("page", "3");
params.add("size", "50");
builder.replaceQueryParams(params);    // 一次性全替换

// 删除参数
builder.replaceQueryParam("token", (Object[]) null);   // 删掉 token

// 只保留白名单参数（安全）
String[] ALLOWED_PARAMS = {"page", "size", "sort", "kw"};
Set<String> allowSet = Set.of(ALLOWED_PARAMS);
builder.replaceQueryParams(null);   // 先清空
request.getParameterMap().forEach((k, v) -> {
    if (allowSet.contains(k)) builder.queryParam(k, (Object[]) v);
});
```

### 3.5 三个常见坑

```java
// 坑 1：fromHttpUrl vs fromUriString
UriComponentsBuilder.fromHttpUrl("https://A.COM/p");   // scheme + host 自动转小写
UriComponentsBuilder.fromUriString("https://A.COM/p");  // 原样保留大小写

// 坑 2：queryParam 传 null 的问题
builder.queryParam("kw", null);   // → ?kw  （值为空，连 = 都没有）
builder.queryParam("kw", "");     // → ?kw=  （空串）
builder.queryParam("kw", "a", null, "b");  // → kw=a&kw&kw=b

// 坑 3：buildAndExpand 遇到未赋值的变量
UriComponentsBuilder.fromUriString("https://a.com/{a}/{b}")
    .buildAndExpand(Map.of("a", "x"));    // ❌ IllegalArgumentException: b not provided
// 解决：确保所有 {name} 都有值，或用 build() 不展开
```

---

## 4. OkHttp HttpUrl 详解

没有 Spring 环境时，OkHttp 的 `HttpUrl` 是最好的替代——自动编码、不可变、比 `URI` 更适合 HTTP 场景。

### 4.1 基础用法

```java
import okhttp3.HttpUrl;

// ① 解析
HttpUrl url = HttpUrl.parse("https://api.example.com/v1/users?page=1");
//  若非法返回 null（不抛异常）

// ② 构建
HttpUrl url = new HttpUrl.Builder()
        .scheme("https")
        .host("api.example.com")
        .addPathSegment("v1")
        .addPathSegment("users")
        .addQueryParameter("page", "2")
        .addQueryParameter("kw", "A&B=C")     // 自动编码
        .build();

System.out.println(url);
// https://api.example.com/v1/users?page=2&kw=A%26B%3DC
```

### 4.2 路径操作

```java
HttpUrl base = HttpUrl.parse("https://a.com/api/v1/");

// addPathSegment —— 自动编码，编码 / 防止层级混淆
base.newBuilder()
    .addPathSegment("my file.txt")      // → /api/v1/my%20file.txt
    .addPathSegment("a/b")              // → /api/v1/my%20file.txt/a%2Fb  ✅ / 被编码
    .build();

// addPathSegments —— 每段独立编码
base.newBuilder()
    .addPathSegments("a/b/c")           // → /api/v1/a/b/c  （按 / 拆成 3 段）
    .build();

// encodedPath —— 已编码路径直接设（不做二次编码）
base.newBuilder()
    .encodedPath("/api/v1/my%20file")   // 原样使用
    .build();
```

### 4.3 Query 参数操作

```java
HttpUrl url = HttpUrl.parse("https://a.com/search?q=a&q=b&page=1");

// 读取
url.queryParameter("q");               // "a"（取首个）
url.queryParameterValues("q");         // ["a", "b"]（取全部）
url.querySize();                       // 3
url.queryParameterName(0);             // "q"
url.queryParameterValue(0);            // "a"

// 修改
HttpUrl modified = url.newBuilder()
        .setQueryParameter("page", "2")       // 替换同名参数
        .removeAllQueryParameters("q")         // 删除所有 q 参数
        .addQueryParameter("sort", "desc")
        .build();
```

### 4.4 resolve —— 相对 URL 解析

```java
HttpUrl base = HttpUrl.parse("https://a.com/docs/guide/intro.html");

base.resolve("/api/users");                // https://a.com/api/users
base.resolve("next.html");                 // https://a.com/docs/guide/next.html
base.resolve("../index.html");              // https://a.com/docs/index.html
base.resolve("//cdn.com/lib.js");          // https://cdn.com/lib.js
base.resolve("?page=2");                   // https://a.com/docs/guide/intro.html?page=2
```

### 4.5 HttpUrl vs URI 选择指南

| 场景 | 推荐 | 原因 |
|------|------|------|
| Spring 项目 | `UriComponentsBuilder` | Spring 原生，融入 MVC |
| HTTP 客户端拼接 URL | `HttpUrl` | 自动编码、`resolve`、拓扑链接 |
| 纯 URL 语法校验 | `URI.create()` | 足够轻量 |
| 做 Map Key | `String`（归一化后） | 避免 `URI` 的大小写/编码差异 |
| 自定义 Scheme | `URI` | `HttpUrl` 只认 `http`/`https` |

---

## 5. Spring 工具集：UriUtils / ServletUriComponentsBuilder

### 5.1 UriUtils —— 按位置的编码

```java
import org.springframework.web.util.UriUtils;

String raw = "my file/测试.txt";

// 不同位置的编码
UriUtils.encodePath(raw, UTF_8);           // "my%20file/%E6%B5%8B%E8%AF%95.txt"  ← / 不编码
UriUtils.encodePathSegment(raw, UTF_8);    // "my%20file%2F%E6%B5%8B%E8%AF%95.txt" ← / 也编码
UriUtils.encodeQuery(raw, UTF_8);          // "my+file%2F%E6%B5%8B%E8%AF%95.txt"   ← 空格变+
UriUtils.encodeQueryParam(raw, UTF_8);     // "my%20file%2F%E6%B5%8B%E8%AF%95.txt" ← 空格变%20
UriUtils.encodeFragment(raw, UTF_8);       // "my%20file/%E6%B5%8B%E8%AF%95.txt"
UriUtils.encodeHost(raw, UTF_8);           // 用于 IDN

// 解码
UriUtils.decode("my%20file.txt", UTF_8);   // "my file.txt"
```

| 方法 | 空格 | `/` | `+` | 用途 |
|------|:---:|:---:|:---:|------|
| `encodePath` | `%20` | 不编码 | `%2B` | 全路径 |
| `encodePathSegment` | `%20` | `%2F` | `%2B` | **单段路径**（推荐） |
| `encodeQuery` | `+` ⚠️ | `%2F` | `%2B` | 整条 Query（废弃） |
| `encodeQueryParam` | `%20` | `%2F` | `%2B` | **单个参数值**（推荐） |
| `encodeFragment` | `%20` | 不编码 | `%2B` | Fragment |

> 🎯 **推荐组合**：Path 段用 `encodePathSegment`，Query 值用 `encodeQueryParam`。不要用 `encodeQuery`（它按 `application/x-www-form-urlencoded` 把空格编成 `+`）。

### 5.2 UriTemplate —— URI 模板变量

```java
import org.springframework.web.util.UriTemplate;

UriTemplate tpl = new UriTemplate("https://a.com/users/{id}/orders/{orderId}{?status,page}");

// 展开
URI uri = tpl.expand(Map.of(
    "id", "42",
    "orderId", "1001",
    "status", "PAID",
    "page", "2"
));
// → https://a.com/users/42/orders/1001?status=PAID&page=2

// 匹配（反向）
Map<String, String> vars = tpl.match("https://a.com/users/42/orders/1001?status=PAID");
// → {id=42, orderId=1001, status=PAID}
```

**URI Template 语法速查（RFC 6570）**：

| 表达式 | 示例 | 展开结果 |
|--------|------|---------|
| `{var}` | `{id}` | `42` |
| `{/var}` | `{/seg}` | `/a/b`（加前缀 `/`） |
| `{?var}` | `{?kw,page}` | `?kw=java&page=1`（加前缀 `?`，未赋值省略） |
| `{&var}` | `{&kw}` | `&kw=java`（加前缀 `&`） |
| `{#var}` | `{#section}` | `#intro`（加前缀 `#`） |

```java
// OpenAPI / Swagger 中的典型用法
@GetMapping("/users/{id}")
// 在 OpenAPI 注解中：
// @Operation(operationId = "getUser")
// path = "/users/{id}"   ← {id} 就是 URI Template 变量
```

---

## 6. JavaScript 端：WHATWG URL API

### 6.1 `new URL()` —— 浏览器与 Node.js 统一

```javascript
// ① 绝对 URL
const u = new URL("https://user:pwd@api.example.com:8443/v1/users/42?page=2&size=10#profile");

u.protocol;    // "https:"          ← 注意包含冒号
u.username;    // "user"
u.password;    // "pwd"
u.hostname;    // "api.example.com"  ← 纯主机名，不含端口
u.host;        // "api.example.com:8443"
u.port;        // "8443"
u.pathname;    // "/v1/users/42"
u.search;      // "?page=2&size=10"
u.hash;        // "#profile"
u.origin;      // "https://api.example.com:8443"
u.href;        // 完整 URL 字符串

// ② 相对 URL（必须提供 base）
const rel = new URL("../api/v2.html", "https://a.com/docs/guide/intro.html");
rel.href;   // "https://a.com/docs/api/v2.html"

const rel2 = new URL("//cdn.com/lib.js", "https://a.com/");
rel2.href;  // "https://cdn.com/lib.js"
```

### 6.2 WHATWG URL 与 RFC 3986 的行为差异

WHATWG URL Standard 比 RFC 3986 **更宽容**：

```javascript
// ① 自动处理非法字符（而 Java URI.create() 会抛异常）
new URL("https://a.com/my file.txt").href;
// "https://a.com/my%20file.txt"       ← 自动编码空格

new URL("https://a.com/path\\back").href;
// "https://a.com/path/back"           ← 自动把反斜杠转成正斜杠 ✅

// ② 自动 trim + 清理
new URL(" https://a.com/p  ").href;
// "https://a.com/p"

// ③ Tab / 换行 → 自动去除
new URL("https://a.com/\na\tb").href;
// "https://a.com/ab"

// ④ Host 转小写
new URL("HTTPS://API.Example.COM/Path").href;
// "https://api.example.com/Path"      ← host 转小写，path 保留
```

```java
// Java 侧对比：同样的输入直接抛异常
URI.create("https://a.com/my file.txt");
// ❌ URISyntaxException: Illegal character in path

URI.create(" https://a.com/p ");
// ❌ URISyntaxException: Illegal character in scheme
```

> 🎯 **解析器差异攻击根源**：同一字符串，WHATWG 能解析出 `host=A.com`，RFC 3986 解析出 `host=B.com` 或直接抛异常。安全过滤时必须考虑两端解析结果可能不同（详见 06 篇）。

### 6.3 修改 URL 组件

```javascript
const u = new URL("https://a.com/v1/users/42?page=1");

// 修改路径
u.pathname = "/v2/orders";
u.href;  // "https://a.com/v2/orders?page=1"

// 修改单个查询参数
u.searchParams.set("page", "3");
u.href;  // "https://a.com/v2/orders?page=3"

// 添加参数
u.searchParams.append("tag", "new");
u.searchParams.append("tag", "hot");

// 删除参数
u.searchParams.delete("tag");

// 排序（用于规范化/签名）
u.searchParams.sort();
```

### 6.4 浏览器端从当前页面构建

```javascript
// 从当前页面
const u = new URL(window.location.href);
const u2 = new URL("/api/users", window.location.origin);

// 获取当前页面的单个 query 参数
const page = new URLSearchParams(window.location.search).get("page");
```

---

## 7. URLSearchParams 完全指南

### 7.1 构造

```javascript
// ① 从字符串
const sp = new URLSearchParams("page=1&kw=a+b&tags=AI&tags=URL");

// ② 从对象
const sp2 = new URLSearchParams({ page: "1", kw: "a b", tags: ["AI", "URL"] });
sp2.toString();  // "page=1&kw=a+b&tags=AI%2CURL"   ← 注意：数组默认逗号拼接

// ③ 迭代构造
const sp3 = new URLSearchParams();
sp3.append("page", "1");
sp3.append("tags", "AI");
sp3.append("tags", "URL");     // 支持重复 key

// ④ 附在 URL 上
const u = new URL("https://a.com/search");
u.searchParams.set("page", "1");        // 等价于 sp3
```

### 7.2 CRUD

```javascript
const sp = new URLSearchParams("page=1&page=2&kw=hello");

// 读
sp.get("page");          // "1"        ← 取第一个
sp.getAll("page");       // ["1","2"]  ← 取全部
sp.has("kw");            // true
sp.has("xxx");           // false

// 改
sp.set("page", "3");     // 覆盖所有 key=page 的 → "page=3&kw=hello"
sp.append("page", "4");  // 追加                  → "page=3&kw=hello&page=4"
sp.delete("kw");         // 删除
sp.sort();               // 字典排序

// 遍历
for (const [key, value] of sp) {
    console.log(key, value);
}
// page, 3
// page, 4

// keys / values / entries 都返回 Iterator
[...sp.keys()];         // ["page", "page"]
[...sp.values()];       // ["3", "4"]
[...sp.entries()];      // [["page","3"], ["page","4"]]
```

### 7.3 序列化差异与坑

```javascript
const sp = new URLSearchParams({ name: "张三", kw: "a+b" });

// toString() —— 用 + 表示空格（表单编码）
sp.toString();
// "name=%E5%BC%A0%E4%B8%89&kw=a%2Bb"
//  ↑ 中文正确编码    ↑ + 被编码为 %2B（正确），空格编码成 + ← 看中文编码那里

// 实际上空格：
const sp2 = new URLSearchParams({ name: "a b" });
sp2.toString();  // "name=a+b"   ← 空格变成 +
```

> ⚠️ **`+` 的二义性**：`URLSearchParams.toString()` 把空格编成 `+`。若后端严格按 RFC 3986 解析（如 Go `net/url` 的 `QueryUnescape`），`+` 不会被解成空格，导致值变成 `"a+b"` 而非 `"a b"`。跨语言通信时建议后端统一用 RFC 3986 规则。

### 7.4 实际案例：前端分页封装

```javascript
class Pagination {
    static build(url, { page, size, sort, filters }) {
        const u = new URL(url);
        if (page !== undefined)  u.searchParams.set("page", page);
        if (size !== undefined)  u.searchParams.set("size", size);
        if (sort) u.searchParams.set("sort", sort);
        if (filters) {
            Object.entries(filters).forEach(([k, v]) => {
                if (v != null && v !== "") u.searchParams.set(k, v);
            });
        }
        return u.toString();
    }

    static parse(searchString) {
        const sp = new URLSearchParams(searchString);
        return {
            page:  parseInt(sp.get("page")) || 0,
            size:  parseInt(sp.get("size")) || 20,
            sort:  sp.get("sort"),
            kw:    sp.get("kw"),
        };
    }
}
```

---

## 8. Node.js 端 URL 处理

### 8.1 两种 API 对比

Node.js 提供了两套 URL API：

| API | 引入 | 符合标准 | 推荐 |
|-----|------|:---:|:---:|
| **WHATWG URL** | 全局可用（Node 10+） | WHATWG URL Standard | ✅ **推荐** |
| **Legacy `url` 模块** | `require('url')` | Node.js 自有 | ❌ 仅维护老代码 |

```javascript
// ✅ 推荐：WHATWG URL（Node 10+ 全局可用，无需 require）
const u = new URL("https://a.com/p?q=1");
u.hostname;   // "a.com"
u.searchParams.get("q");  // "1"

// ❌ 旧式（仍可用但不推荐新代码使用）
const url = require("url");
const parsed = url.parse("https://a.com/p?q=1", true);
parsed.query.q;  // "1"
```

### 8.2 Node.js 独有的 `url` 模块方法

```javascript
const url = require("url");

// ① url.resolve —— 注意已废弃，但老代码大量使用
url.resolve("https://a.com/docs/guide/", "intro.html");
// "https://a.com/docs/guide/intro.html"

// ✅ 替代：使用 WHATWG URL
new URL("intro.html", "https://a.com/docs/guide/").href;

// ② url.format —— URL 对象 → 字符串
url.format({
    protocol: "https",
    hostname: "a.com",
    pathname: "/p",
    query: { q: "1" }
});
// "https://a.com/p?q=1"

// ③ url.domainToASCII / domainToUnicode
url.domainToASCII("例子.测试");        // "xn--fsqu00a.xn--0zwm56d"
url.domainToUnicode("xn--fsqu00a.xn--0zwm56d");  // "例子.测试"

// ④ url.fileURLToPath / pathToFileURL
url.fileURLToPath("file:///home/user/file.txt");  // "/home/user/file.txt"
url.pathToFileURL("/home/user/file.txt").href;    // "file:///home/user/file.txt"
```

### 8.3 Express / Koa 中取 URL 组件

```javascript
// Express
app.get("/api/users/:id", (req, res) => {
    req.protocol;           // "https"
    req.hostname;           // "api.example.com"
    req.originalUrl;        // "/api/users/42?page=1"   ← 含 query
    req.path;               // "/api/users/42"
    req.query;              // { page: "1" }             ← 已解析的对象
    req.params.id;          // "42"

    // 拼装完整 URL
    const fullUrl = `${req.protocol}://${req.get("host")}${req.originalUrl}`;
    const parsed  = new URL(fullUrl);
});

// Koa
app.use(async (ctx) => {
    ctx.protocol;           // "https"
    ctx.host;               // "api.example.com"
    ctx.path;               // "/api/users/42"
    ctx.query;              // { page: "1" }
    ctx.querystring;        // "page=1"
    ctx.URL;                // WHATWG URL 对象
    ctx.URL.searchParams.get("page");  // "1"
});
```

---

## 9. 跨语言对照速查表

### 9.1 解析 URL 各组件

| 操作 | Java (`URI`) | JS (WHATWG) | Go (`net/url`) | Python (`urllib`) |
|------|-------------|-------------|----------------|-------------------|
| 解析 | `URI.create(s)` | `new URL(s)` | `url.Parse(s)` | `urlparse(s)` |
| Scheme | `getScheme()` | `.protocol` | `.Scheme` | `.scheme` |
| Host | `getHost()` | `.hostname` | `.Hostname()` | `.hostname` |
| Port | `getPort()` | `.port` | `.Port()` | `.port` |
| Path | `getRawPath()` | `.pathname` | `.Path` | `.path` |
| Query | `getRawQuery()` | `.search` | `.RawQuery` | `.query` |
| Fragment | `getRawFragment()` | `.hash` | `.Fragment` | `.fragment` |

### 9.2 编码

| 操作 | Java | JS |
|------|------|----|
| 编码 Query 值 | `UriUtils.encodeQueryParam(s, UTF_8)` | `encodeURIComponent(s)` |
| 编码 Path 段 | `UriUtils.encodePathSegment(s, UTF_8)` | `encodeURIComponent(s)` |
| 编码整条 URL | — | `encodeURI(s)` ⚠️ |
| 解码 | `UriUtils.decode(s, UTF_8)` | `decodeURIComponent(s)` |

### 9.3 拼接 URL

| 操作 | Java | JS |
|------|------|----|
| 拼 Query | `UriComponentsBuilder...queryParam(k, v)` | `u.searchParams.set(k, v)` |
| 拼 Path | `UriComponentsBuilder...pathSegment(s)` | `u.pathname += "/" + encodeURIComponent(s)` |
| 相对解析 | `base.resolve(s)` | `new URL(s, base)` |
| 最终输出 | `.toUriString()` / `.toASCIIString()` | `.href` / `.toString()` |

### 9.4 各语言独有陷阱

| 语言 | 陷阱 | 说明 |
|------|------|------|
| Java | `URL.equals()` 做 DNS 查询 | 用 `URI` 代替 |
| Java | `URLEncoder` 是表单编码器 | Path 用 `UriUtils.encodePathSegment` |
| Java | `URL` 构造器 Java 20+ 弃用 | 用 `URI.create(s).toURL()` |
| JS | `encodeURI` 不编码 `& = / ? #` | 编码值一律用 `encodeURIComponent` |
| JS | `encodeURIComponent` 不编码 `!'()*` | 签名场景需补编码 |
| JS | `btoa` 不支持中文 | 先用 `TextEncoder` 转 UTF-8 |
| Go | `url.Parse` 不验证 scheme | 需手动检查 |
| Python | `urlparse` 对不同 scheme 解析规则不同 | 注意 `;params` 部分 |

---

> 🎯 **本篇核心要点**
> 1. Java 优先用 `URI` 而非 `URL`——`URL.equals()` 会做 DNS 解析
> 2. Spring 项目字符串拼 URL 的唯一正解是 `UriComponentsBuilder`
> 3. 无 Spring 时用 OkHttp `HttpUrl`，自动编码 + 不可变 + 比 `URI.create()` 更友好
> 4. 安全校验一律用 `getRawPath()` / `getRawQuery()`，解码后的值会丢失层级 + 被绕过
> 5. JS 编码值用 `encodeURIComponent`；拼接 Query 优先用 `URLSearchParams`
> 6. 跨语言通信时统一编码规则，`+` vs `%20` 是跨语言最常见的不一致点

---

**上一模块**：[04-URL路由与后端开发实战.md](04-URL路由与后端开发实战.md) / **下一模块**：[06-URL安全与常见漏洞防护.md](06-URL安全与常见漏洞防护.md)
