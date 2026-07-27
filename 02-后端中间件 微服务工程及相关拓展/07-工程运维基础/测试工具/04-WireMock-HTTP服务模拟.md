# WireMock HTTP 服务模拟

> 基于 HTTP 的 API 模拟工具，用于在测试中模拟外部 REST/SOAP 服务，解决第三方依赖不稳定、限流、环境不可用等集成测试痛点。

---

## 目录

1. [WireMock 概述](#1-wiremock-概述)
2. [安装与启动](#2-安装与启动)
3. [Stubbing 基础](#3-stubbing-基础)
4. [响应定义](#4-响应定义)
5. [请求匹配](#5-请求匹配)
6. [优先级系统](#6-优先级系统)
7. [状态行为（Scenarios）](#7-状态行为scenarios)
8. [响应模板（Response Templating）](#8-响应模板response-templating)
9. [Recording 与 Playback](#9-recording-与-playback)
10. [验证（Verification）](#10-验证verification)
11. [故障模拟](#11-故障模拟)
12. [WireMock + Spring Boot 集成](#12-wiremock--spring-boot-集成)
13. [完整示例：OrderService 调用外部 PaymentService](#13-完整示例orderservice-调用外部-paymentservice)
14. [WireMock vs Mockito vs Hoverfly](#14-wiremock-vs-mockito-vs-hoverfly)

---

## 1. WireMock 概述

### 1.1 什么是 WireMock

WireMock 是一个开源的 HTTP 模拟库/服务，专门用于在测试中模拟外部 HTTP API。它作为一个轻量级 HTTP 服务器运行，接收请求并返回预定义的响应，使开发者无需依赖真实的外部服务即可完成集成测试。

### 1.2 为什么需要 HTTP Mock

| 场景 | 问题 | WireMock 方案 |
|------|------|---------------|
| **第三方 API 依赖** | 支付网关、短信服务在测试环境不可用或不稳定 | 模拟标准的 HTTP 响应 |
| **限流与配额** | 外部 API 有调用次数限制或按调用付费 | 本地模拟，零成本调用 |
| **网络延迟** | 依赖外网，测试执行缓慢 | 本地回环，毫秒级响应 |
| **异常场景** | 超时、500、网络故障难以复现 | 可精确控制故障注入 |
| **测试隔离** | 多个测试共享同一个外部 API 导致互相干扰 | 每个测试拥有独立的 Mock 端口 |

### 1.3 核心能力

```
WireMock 能力矩阵

┌─ Stubbing ─────────────────────────────────┐
│  预设 HTTP 请求的匹配规则与响应模板          │
│  GET / POST / PUT / DELETE / PATCH / HEAD  │
└────────────────────────────────────────────┘

┌─ Request Matching ─────────────────────────┐
│  URL / Headers / Body / Query Params        │
│  JSON / XML / Regex / EqualToJson          │
└────────────────────────────────────────────┘

┌─ Verification ─────────────────────────────┐
│  验证实际请求是否如预期发出                  │
│  次数检查 / 请求内容检查 / 调用顺序检查      │
└────────────────────────────────────────────┘

┌─ Recording / Proxy ────────────────────────┐
│  录制真实 API 请求-响应                     │
│  录制后回放，无需真实依赖                   │
└────────────────────────────────────────────┘

┌─ Fault Injection ─────────────────────────┐
│  连接断开 / 空响应 / 格式错误 / 延迟注入    │
└────────────────────────────────────────────┘
```

> 💡 **适用场景判断**：如果被测代码通过 HTTP 调用外部服务，且你不希望在测试中真正调用该服务，就是 WireMock 的用武之地。

---

## 2. 安装与启动

### 2.1 独立 JAR（Standalone）

```bash
# 下载 standalone JAR
wget https://repo1.maven.org/maven2/org/wiremock/wiremock-standalone/3.9.1/wiremock-standalone-3.9.1.jar

# 启动（默认端口 8080）
java -jar wiremock-standalone-3.9.1.jar

# 自定义端口和目录
java -jar wiremock-standalone-3.9.1.jar --port 9090 --root-dir ./mock-data
```

启动后访问 `http://localhost:8080/__admin/` 可查看 WireMock Admin API。

### 2.2 Docker 部署

```bash
# 启动 WireMock 容器
docker run -it --rm \
  -p 8080:8080 \
  -v $PWD/wiremock:/home/wiremock \
  --name wiremock \
  wiremock/wiremock:3.9.1

# 指定端口
docker run -it --rm -p 9090:9090 wiremock/wiremock:3.9.1 --port 9090
```

> 💡 使用 Docker 时，将 stubbing 配置文件放在挂载目录的 `mappings/` 和 `__files/` 目录下，WireMock 自动加载。

### 2.3 JVM 库模式（WireMockServer）

```xml
<dependency>
    <groupId>org.wiremock</groupId>
    <artifactId>wiremock</artifactId>
    <version>3.9.1</version>
    <scope>test</scope>
</dependency>
```

```java
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.extension.RegisterExtension;

class WireMockBasicTest {

    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
        .options(wireMockConfig().port(8089))
        .build();

    @Test
    void simpleStub() {
        // 给 WireMock 配置 Stub
        stubFor(get("/api/hello")
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("Hello, WireMock!")));

        // 发送真实 HTTP 请求（模拟被测代码）
        String result = new RestTemplate().getForObject(
            "http://localhost:8089/api/hello", String.class);

        assertEquals("Hello, WireMock!", result);
    }
}
```

### 2.4 Maven 插件方式

```xml
<plugin>
    <groupId>org.wiremock</groupId>
    <artifactId>wiremock-maven-plugin</artifactId>
    <version>2.0.0</version>
    <executions>
        <execution>
            <phase>pre-integration-test</phase>
            <goals>
                <goal>run</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <port>8089</port>
        <dir>${project.basedir}/src/test/resources/wiremock</dir>
    </configuration>
</plugin>
```

---

## 3. Stubbing 基础

### 3.1 基本语法

```java
import static com.github.tomakehurst.wiremock.client.WireMock.*;

// 完整链式语法
stubFor(get(urlEqualTo("/api/users/1"))
    .willReturn(aResponse()
        .withStatus(200)
        .withHeader("Content-Type", "application/json")
        .withBody("{\"id\":1, \"name\":\"张三\"}")));
```

### 3.2 支持的所有 HTTP 方法

```java
stubFor(get(urlEqualTo("/resource"))    ...);
stubFor(post(urlEqualTo("/resource"))   ...);
stubFor(put(urlEqualTo("/resource"))    ...);
stubFor(delete(urlEqualTo("/resource")) ...);
stubFor(patch(urlEqualTo("/resource"))  ...);
stubFor(head(urlEqualTo("/resource"))   ...);
stubFor(options(urlEqualTo("/resource"))...);
```

### 3.3 URL 匹配模式

```java
// 精确匹配（推荐用于固定路径）
stubFor(get(urlEqualTo("/api/users/1"))

// 路径匹配（忽略查询参数）
stubFor(get(urlPathEqualTo("/api/users"))

// 正则匹配
stubFor(get(urlMatching("/api/users/[0-9]+"))

// 路径模式匹配
stubFor(get(urlPathMatching("/api/(users|orders)/.*"))
```

> ⚠️ `urlEqualTo` 会匹配完整 URL 包括查询参数，`urlPathEqualTo` 只匹配路径部分。

### 3.4 Header 匹配

```java
stubFor(get(urlEqualTo("/api/orders"))
    .withHeader("Authorization", equalTo("Bearer token-123"))
    .withHeader("Accept", containing("application/json"))
    .withHeader("X-Request-Id", matching("[a-f0-9-]+"))
    .willReturn(aResponse().withStatus(200)));
```

### 3.5 Request Body 匹配

```java
// 精确 Body 匹配
stubFor(post(urlEqualTo("/api/orders"))
    .withRequestBody(equalTo("{\"productId\":1}"))

// 包含匹配
stubFor(post(urlEqualTo("/api/orders"))
    .withRequestBody(containing("productId"))

// JSON 精确匹配
stubFor(post(urlEqualTo("/api/orders"))
    .withRequestBody(equalToJson("""
        {"productId": 1, "quantity": 2}
        """))

// JSON 路径匹配（忽略特定字段）
stubFor(post(urlEqualTo("/api/orders"))
    .withRequestBody(equalToJson("""
        {"productId": 1, "quantity": 2}
        """, true, true))  // true=ignoreArrayOrder, true=ignoreExtraFields

// XML 匹配
stubFor(post(urlEqualTo("/api/orders"))
    .withRequestBody(equalToXml("<order><productId>1</productId></order>"))

// 正则匹配 Body
stubFor(post(urlEqualTo("/api/orders"))
    .withRequestBody(matching(".*productId.*"))
```

### 3.6 Query Parameter 匹配

```java
stubFor(get(urlPathEqualTo("/api/users"))
    .withQueryParam("page", equalTo("1"))
    .withQueryParam("size", equalTo("20"))
    .withQueryParam("sort", matching("(asc|desc)"))
    .willReturn(aResponse().withStatus(200)));
```

---

## 4. 响应定义

### 4.1 响应 DSL 详解

```java
aResponse()
    // HTTP 状态码
    .withStatus(200)                    // 200 OK
    .withStatus(201)                    // 201 Created
    .withStatus(400)                    // 400 Bad Request
    .withStatus(401)                    // 401 Unauthorized
    .withStatus(404)                    // 404 Not Found
    .withStatus(500)                    // 500 Internal Server Error

    // 响应头
    .withHeader("Content-Type", "application/json")
    .withHeader("X-Correlation-Id", "abc-123")

    // 响应体
    .withBody("{\"status\":\"ok\"}")                        // 直接字符串
    .withBodyFile("response/order-123.json")                // 从 __files/ 目录读取

    // JSON 响应体（Java 对象转 JSON）
    .withBody(objectMapper.writeValueAsString(orderResponse))

    // Base64 编码的二进制响应
    .withBase64Body("bG9yZW0gaXBzdW0=")

    // 延迟注入
    .withFixedDelay(3000)               // 固定延迟 3 秒
    .withLogNormalRandomDelay(90, 0.1)  // 随机延迟（对数正态分布）

    // 响应转换器
    .withTransformers("response-template")  // 使用模板引擎
```

### 4.2 JSON 响应示例

```java
stubFor(get(urlEqualTo("/api/users/42"))
    .willReturn(aResponse()
        .withStatus(200)
        .withHeader("Content-Type", "application/json")
        .withBody("""
            {
                "id": 42,
                "name": "张三",
                "email": "zhangsan@example.com",
                "role": "ADMIN",
                "createdAt": "2026-07-20T10:30:00Z"
            }
            """)));
```

### 4.3 从文件读取响应体

在 `src/test/resources/wiremock/__files/` 目录下创建文件：

```json
// __files/response/order-123.json
{
    "orderId": "ORD-123",
    "status": "PAID",
    "amount": 99.99,
    "currency": "CNY"
}
```

```java
stubFor(get(urlEqualTo("/api/orders/ORD-123"))
    .willReturn(aResponse()
        .withStatus(200)
        .withHeader("Content-Type", "application/json")
        .withBodyFile("response/order-123.json")));
```

> 💡 `withBodyFile` 默认从 `__files/` 目录查找文件，可通过 `--root-dir` 或 `wireMockConfig().usingFilesUnderDirectory()` 自定义根目录。

### 4.4 延迟模拟

```java
// 固定延迟（模拟慢响应）
stubFor(get(urlEqualTo("/api/slow"))
    .willReturn(aResponse()
        .withStatus(200)
        .withFixedDelay(5000)));  // 5 秒延迟

// 随机延迟（更真实地模拟网络波动）
stubFor(get(urlEqualTo("/api/unstable"))
    .willReturn(aResponse()
        .withStatus(200)
        .withLogNormalRandomDelay(90, 0.1)));  // median=90ms, sigma=0.1

// 均匀分布延迟
stubFor(get(urlEqualTo("/api/variable"))
    .willReturn(aResponse()
        .withUniformRandomDelay(100, 500)));  // 100ms ~ 500ms 随机
```

### 4.5 Chunked 编码响应

```java
stubFor(get(urlEqualTo("/api/stream"))
    .willReturn(aResponse()
        .withStatus(200)
        .withHeader("Content-Type", "text/plain")
        .withChunkedBody("这是一段流式响应内容...", 50)));  // 每 chunk 50 字符
```

---

## 5. 请求匹配

### 5.1 匹配策略总览

WireMock 的匹配器（Request Matcher）体系非常丰富，可以精确控制哪些请求触发哪些响应：

| 匹配器 | 匹配目标 | 示例 |
|--------|----------|------|
| `urlEqualTo` | 完整 URL（含查询参数） | `urlEqualTo("/api/users?page=1")` |
| `urlPathEqualTo` | 仅路径 | `urlPathEqualTo("/api/users")` |
| `urlMatching` | URL 正则匹配 | `urlMatching("/api/users/\\d+")` |
| `urlPathMatching` | 路径正则匹配 | `urlPathMatching("/api/(users\|orders)")` |
| `equalTo` | 精确字符串匹配 | `equalTo("Bearer token-abc")` |
| `containing` | 包含匹配 | `containing("productId")` |
| `matching` | 正则匹配 | `matching("[a-z]+@[a-z]+\\.com")` |
| `notMatching` | 正则不匹配 | `notMatching("admin")` |
| `equalToJson` | JSON 精确匹配 | `equalToJson("{\"id\":1}")` |
| `matchingJsonPath` | JSON Path 匹配 | `matchingJsonPath("$.status")` |
| `equalToXml` | XML 精确匹配 | `equalToXml("<root/>")` |
| `matchingXPath` | XPath 匹配 | `matchingXPath("//order/status")` |
| `absent` | 不存在/未提供 | `absent()` |

### 5.2 JSON 匹配深度分析

```java
// 严格 JSON 匹配（顺序、额外字段都要检查）
stubFor(post(urlEqualTo("/api/orders"))
    .withRequestBody(equalToJson("""
        {"productId": 1, "quantity": 2, "coupon": null}
        """)));

// 宽松 JSON 匹配（忽略数组顺序）
stubFor(post(urlEqualTo("/api/orders"))
    .withRequestBody(equalToJson("""
        {"productId": 1, "quantity": 2}
        """, true, false)));  // ignoreArrayOrder=true, ignoreExtraFields=false

// 完全宽松（忽略数组顺序和额外字段）
stubFor(post(urlEqualTo("/api/orders"))
    .withRequestBody(equalToJson("""
        {"productId": 1}
        """, true, true)));  // 只校验 productId 字段存在且值正确

// JSON Path 匹配（灵活提取并校验）
stubFor(post(urlEqualTo("/api/orders"))
    .withRequestBody(matchingJsonPath("$.items[?(@.price > 100)]"))
    .willReturn(aResponse().withStatus(200)));

stubFor(post(urlEqualTo("/api/orders"))
    .withRequestBody(matchingJsonPath("$.userId", equalTo("123")))
    .willReturn(aResponse().withStatus(200)));
```

### 5.3 XML 匹配

```java
// XML 精确匹配（忽略空白）
stubFor(post(urlEqualTo("/api/soap"))
    .withRequestBody(equalToXml("""
        <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
            <soap:Body>
                <getUser>
                    <id>1</id>
                </getUser>
            </soap:Body>
        </soap:Envelope>
        """))
    .willReturn(aResponse().withStatus(200)));

// XPath 匹配
stubFor(post(urlEqualTo("/api/soap"))
    .withRequestBody(matchingXPath("//getUser/id/text()", equalTo("1")))
    .willReturn(aResponse().withStatus(200)));

// XPath 存在性匹配
stubFor(post(urlEqualTo("/api/soap"))
    .withRequestBody(matchingXPath("//getUser[id=1]"))
    .willReturn(aResponse().withStatus(200)));
```

### 5.4 多条件组合匹配

```java
// AND 条件：所有条件必须同时满足
stubFor(post(urlEqualTo("/api/payments"))
    .withHeader("Authorization", containing("Bearer"))
    .withHeader("X-Idempotency-Key", notMatching(""))
    .withRequestBody(matchingJsonPath("$.amount"))
    .withQueryParam("currency", equalTo("CNY"))
    .willReturn(aResponse().withStatus(200)));

// OR 条件：使用自定义匹配器
stubFor(requestMatching(request -> {
    String method = request.getMethod().value();
    String path = request.getUrl();
    return method.equals("GET") && path.startsWith("/api/v2/");
}).willReturn(aResponse().withStatus(200)));
```

---

## 6. 优先级系统

当多个 Stub 都可以匹配同一个请求时，WireMock 会根据优先级选择最合适的 Stub。

### 6.1 优先级规则

```java
// 最高优先级（priority = 1）：精确匹配特定用户
stubFor(get(urlEqualTo("/api/users/1"))
    .atPriority(1)
    .willReturn(aResponse()
        .withStatus(200)
        .withBody("{\"id\":1,\"name\":\"管理员\"}")));

// 中等优先级（priority = 3）：匹配所有用户请求
stubFor(get(urlMatching("/api/users/[0-9]+"))
    .atPriority(3)
    .willReturn(aResponse()
        .withStatus(200)
        .withBody("{\"name\":\"普通用户\"}")));

// 最低优先级（priority = 10）：兜底，匹配 /api/users 下的所有请求
stubFor(get(urlPathMatching("/api/users/.*"))
    .atPriority(10)
    .willReturn(aResponse()
        .withStatus(404)
        .withBody("{\"error\":\"User not found\"}")));
```

### 6.2 优先级行为说明

- **数字越小，优先级越高**（1 为最高，10 为默认最低值）
- 当 WireMock 收到请求时，按优先级从高到低依次尝试匹配
- 第一个匹配到请求的 Stub 会返回响应，后续 Stub 被忽略
- 不设置 `atPriority()` 时默认优先级为 5

---

## 7. 状态行为（Scenarios）

Scenarios 允许 Stub 根据调用次数改变行为，模拟多步骤交互流程。

### 7.1 基本用法

```java
// 场景：模拟订单状态变更流程

// Step 1: 订单刚创建
stubFor(post(urlEqualTo("/api/payments"))
    .inScenario("Order Payment")
    .whenScenarioStateIs(Scenario.STARTED)
    .willReturn(aResponse()
        .withStatus(200)
        .withBody("{\"status\":\"PENDING\"}"))
    .willSetStateTo("PAID"));

// Step 2: 支付已确认
stubFor(get(urlEqualTo("/api/payments/123"))
    .inScenario("Order Payment")
    .whenScenarioStateIs("PAID")
    .willReturn(aResponse()
        .withStatus(200)
        .withBody("{\"status\":\"COMPLETED\", \"paidAt\":\"2026-07-20T10:30:00Z\"}"))
    .willSetStateTo("REFUNDED"));

// Step 3: 已退款
stubFor(post(urlEqualTo("/api/payments/123/refund"))
    .inScenario("Order Payment")
    .whenScenarioStateIs("REFUNDED")
    .willReturn(aResponse()
        .withStatus(400)
        .withBody("{\"error\":\"Already refunded\"}")));
```

### 7.2 状态转换图

```
STARTED ──[POST /payments]──▶ PAID ──[GET /payments/123]──▶ REFUNDED
                                                               │
                                              [POST /refund]───▶ 400 Error
```

### 7.3 重置场景

```java
// 测试中手动重置场景状态
WireMock.resetScenario("Order Payment");

// 重置所有场景
WireMock.resetAllScenarios();
```

### 7.4 不设初始状态

```java
// 不设置 whenScenarioStateIs 的 Stub 在任何状态下都匹配
// 可用于进入场景前的准备工作
stubFor(post(urlEqualTo("/api/auth/token"))
    .inScenario("Authenticated Flow")
    .willReturn(aResponse()
        .withStatus(200)
        .withBody("{\"token\":\"jwt-token-123\"}"))
    .willSetStateTo("AUTHENTICATED"));
```

---

## 8. 响应模板（Response Templating）

WireMock 内置 Handlebars 模板引擎，可以在响应体中动态引用请求数据。

### 8.1 启用模板

```java
// 方式一：使用 withTransformers
stubFor(get(urlPathMatching("/api/users/([0-9]+)"))
    .willReturn(aResponse()
        .withTransformers("response-template")
        .withHeader("Content-Type", "application/json")
        .withBody("""
            {
                "userId": "{{request.pathSegments.[2]}}",
                "queryName": "{{request.query.name}}",
                "timestamp": "{{now}}",
                "randomId": "{{randomValue length=32 type=ALPHANUMERIC}}"
            }
            """)));

// 方式二：全局配置时自动启用
WireMockServer wm = new WireMockServer(wireMockConfig()
    .extensions(new ResponseTemplateTransformer(true)));  // true=全局启用
```

### 8.2 Handlebars 模板语法

| 模板表达式 | 说明 | 示例值 |
|-----------|------|--------|
| `{{request.path}}` | 完整请求路径 | `/api/users/42` |
| `{{request.pathSegments.[0]}}` | 路径段 | `api` |
| `{{request.pathSegments.[2]}}` | 取第 N 段（1-indexed） | `42` |
| `{{request.query.key}}` | 查询参数值 | `张三` |
| `{{request.headers.Authorization}}` | 请求头 | `Bearer xxx` |
| `{{request.body}}` | 请求体 | `{"id":1}` |
| `{{jsonPath request.body '$.userId'}}` | 从请求体 JSON 提取 | `123` |
| `{{request.method}}` | HTTP 方法 | `GET` |
| `{{request.scheme}}` | 协议 | `http` |
| `{{request.host}}` | Host | `localhost` |
| `{{request.port}}` | 端口 | `8089` |
| `{{now}}` | 当前时间（ISO 格式） | `2026-07-20T10:30:00Z` |
| `{{now format='yyyy-MM-dd'}}` | 格式化日期 | `2026-07-20` |
| `{{now offset='3 days' format='yyyy-MM-dd'}}` | 偏移日期 | `2026-07-23` |
| `{{randomValue length=16 type=ALPHANUMERIC}}` | 随机字符串 | `aB3xK9mP2qR7vW1n` |
| `{{randomValue type=UUID}}` | 随机 UUID | `a1b2c3d4-...` |
| `{{pickRandom 'A' 'B' 'C'}}` | 随机选择 | `B` |
| `{{parseNumber request.query.page}}` | 字符串转数字 | `1` |

### 8.3 条件模板

```json
{
    "status": "{{#ifeq request.query.type 'vip'}}PREMIUM{{else}}STANDARD{{/ifeq}}",
    "message": "{{#if request.headers.X-Debug}}调试模式已开启{{/if}}",
    "items": [
        {{#each (jsonPath request.body '$.items')}}
        {
            "id": {{this.id}},
            "processed": true
        }{{#unless @last}},{{/unless}}
        {{/each}}
    ]
}
```

### 8.4 模板实战示例

```java
stubFor(post(urlEqualTo("/api/echo"))
    .willReturn(aResponse()
        .withTransformers("response-template")
        .withHeader("Content-Type", "application/json")
        .withBody("""
            {
                "method": "{{request.method}}",
                "path": "{{request.path}}",
                "queryParams": {
                    "page": "{{request.query.page}}",
                    "filter": "{{request.query.filter}}"
                },
                "headers": {
                    "content-type": "{{request.headers.Content-Type}}",
                    "user-agent": "{{request.headers.User-Agent}}"
                },
                "bodyEcho": {{request.body}},
                "timestamp": "{{now}}",
                "requestId": "{{randomValue type=UUID}}"
            }
            """)));
```

---

## 9. Recording 与 Playback

WireMock 可以作为一个反向代理（Proxy），录制真实 API 的请求-响应，后续在测试中直接回放，无需再次调用真实服务。

### 9.1 录制模式启动

```bash
# 以代理模式启动（--proxy-all 指定目标地址）
java -jar wiremock-standalone-3.9.1.jar \
  --proxy-all "https://api.real-service.com" \
  --record-mappings \
  --root-dir ./recorded-mappings
```

### 9.2 编程方式录制

```java
// 配置录制
WireMock wm = new WireMock("localhost", 8089);

// 开始录制（代理所有 /api 请求到 https://api.real-service.com）
wm.startRecording(wm.recordSpec()
    .forTarget("https://api.real-service.com")
    .onlyRequestsMatching(getRequestedFor(urlPathMatching("/api/.*")))
    .captureHeader("Content-Type")
    .captureHeader("Authorization")
    .extractBodyViaHttp(RecordingSpec.BodyExtractionMethod.MODIFY_HEADERS)
    .recordThroughProxy(proxyConfig -> proxyConfig
        .withTrustStore("truststore.jks", "password")));

// 发送请求到 WireMock (localhost:8089)，WireMock 会转发到真实服务并记录
restTemplate.getForObject("http://localhost:8089/api/users/1", String.class);

// 停止录制
wm.stopRecording();

// 录制的 Stub 会自动保存到 mappings/ 目录
```

### 9.3 使用录制的映射文件

录制完成后，WireMock 会在 `mappings/` 目录生成 JSON 文件：

```json
// mappings/api_users_1-xxxx.json
{
    "request": {
        "method": "GET",
        "urlPath": "/api/users/1"
    },
    "response": {
        "status": 200,
        "jsonBody": {
            "id": 1,
            "name": "张三",
            "email": "zhangsan@example.com"
        },
        "headers": {
            "Content-Type": "application/json"
        }
    },
    "uuid": "a1b2c3d4-...",
    "persistent": true
}
```

### 9.4 快照录制（Snapshots）

```java
// 动态快照：对特定请求生成 Stub
WireMock.takeSnapshotRecording();

// 或通过 Admin API
// POST /__admin/recordings/snapshot
// Body: {"targetBaseUrl": "https://api.real-service.com"}
```

> 💡 **使用建议**：录制功能适合快速生成 Stub，但录制的响应数据可能包含敏感信息或过期数据。录制结果应作为起点，再手动调整和精简。

---

## 10. 验证（Verification）

WireMock 可以验证被测系统是否按预期发出了 HTTP 请求。

### 10.1 基本验证

```java
stubFor(post(urlEqualTo("/api/payments"))
    .willReturn(aResponse().withStatus(200)));

// 被测代码执行...
orderService.processPayment(order);

// 验证请求已被发送（至少一次）
verify(postRequestedFor(urlEqualTo("/api/payments")));

// 验证精确调用次数
verify(1, postRequestedFor(urlEqualTo("/api/payments")));

// 验证未被调用
verify(0, postRequestedFor(urlEqualTo("/api/payments/cancel")));
```

### 10.2 带请求内容的验证

```java
// 验证请求体内容
verify(postRequestedFor(urlEqualTo("/api/payments"))
    .withRequestBody(matchingJsonPath("$.amount", equalTo("99.99")))
    .withRequestBody(matchingJsonPath("$.currency", equalTo("CNY"))));

// 验证请求头
verify(getRequestedFor(urlPathEqualTo("/api/orders"))
    .withHeader("Authorization", containing("Bearer "))
    .withHeader("X-Idempotency-Key", notMatching("")));

// 验证查询参数
verify(getRequestedFor(urlPathEqualTo("/api/users"))
    .withQueryParam("page", equalTo("1"))
    .withQueryParam("size", equalTo("20")));
```

### 10.3 高级验证

```java
// 验证一段时间内没有匹配的请求
verify(0, getRequestedFor(urlPathMatching("/api/internal/.*")));

// 验证请求顺序（需要 WireMock 3.x+）
// 可以通过设置每个 Stub 的 ID 后配合验证
stubFor(get(urlEqualTo("/api/auth/token"))
    .withId("auth-step-1")
    .willReturn(aResponse().withStatus(200)));
stubFor(get(urlEqualTo("/api/data"))
    .withId("auth-step-2")
    .willReturn(aResponse().withStatus(200)));

// 查找未匹配的请求（记录没有匹配到任何 Stub 的请求）
List<LoggedRequest> unmatched = findAll(unmatchedRequests());
assertTrue(unmatched.isEmpty());
```

### 10.4 验证超时与精确时间

```java
// 查找最近 5 秒内的请求
List<LoggedRequest> recentRequests = findAll(
    getRequestedFor(urlPathEqualTo("/api/orders"))
        .withClock().before(Clock.offset(Clock.systemDefaultZone(), Duration.ofSeconds(5))));
```

---

## 11. 故障模拟

WireMock 支持几种内置的故障注入方式，用于测试客户端对异常情况的处理能力。

### 11.1 网络故障

```java
// 连接重置（Connection Reset by Peer）
stubFor(get(urlEqualTo("/api/unstable"))
    .willReturn(aResponse()
        .withFault(Fault.CONNECTION_RESET_BY_PEER)));

// 空响应（连接正常建立但立即关闭，不返回任何数据）
stubFor(get(urlEqualTo("/api/empty"))
    .willReturn(aResponse()
        .withFault(Fault.EMPTY_RESPONSE)));

// 格式错误的响应块（chunked 编码异常）
stubFor(get(urlEqualTo("/api/malformed"))
    .willReturn(aResponse()
        .withFault(Fault.MALFORMED_RESPONSE_CHUNK)));

// 随机数据（返回无法解析的垃圾数据）
stubFor(get(urlEqualTo("/api/garbage"))
    .willReturn(aResponse()
        .withFault(Fault.RANDOM_DATA_THEN_CLOSE)));
```

### 11.2 故障类型详解

| 故障类型 | 行为 | 客户端表现 |
|----------|------|------------|
| `CONNECTION_RESET_BY_PEER` | TCP 连接被强制重置 | `SocketException: Connection reset` |
| `EMPTY_RESPONSE` | 连接建立后直接关闭，无任何响应数据 | `SocketException: Unexpected end of file` |
| `MALFORMED_RESPONSE_CHUNK` | 返回无效的分块编码数据 | `IOException: Invalid chunk header` |
| `RANDOM_DATA_THEN_CLOSE` | 返回随机二进制数据后关闭 | 解析异常 |

### 11.3 超时模拟组合

```java
// 200ms 延迟 + 连接重置（模拟服务过载后崩溃）
stubFor(get(urlEqualTo("/api/cascading-failure"))
    .willReturn(aResponse()
        .withFixedDelay(200)
        .withFault(Fault.CONNECTION_RESET_BY_PEER)));

// 随机延迟 + 偶尔 500
stubFor(get(urlEqualTo("/api/unreliable"))
    .willReturn(aResponse()
        .withUniformRandomDelay(100, 2000)
        .withStatus(503)
        .withBody("{\"error\":\"Service temporarily unavailable\"}")));
```

---

## 12. WireMock + Spring Boot 集成

### 12.1 使用 WireMockExtension（推荐方式）

```xml
<dependency>
    <groupId>org.wiremock</groupId>
    <artifactId>wiremock-spring-boot</artifactId>
    <version>3.9.1</version>
    <scope>test</scope>
</dependency>
```

```java
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentServiceIntegrationTest {

    // 启动 WireMock 在随机端口
    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
        .options(wireMockConfig().dynamicPort())
        .build();

    @Autowired
    private PaymentService paymentService;

    @Test
    void processPayment_Success() {
        // Given：配置 WireMock 模拟支付网关响应
        String paymentGatewayUrl = "http://localhost:" + wireMock.getPort();
        System.setProperty("payment.gateway.url", paymentGatewayUrl);

        wireMock.stubFor(post(urlEqualTo("/gateway/charge"))
            .withRequestBody(matchingJsonPath("$.amount"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {"transactionId": "TXN-001", "status": "SUCCESS"}
                    """)));

        // When
        PaymentResult result = paymentService.charge(new BigDecimal("99.99"));

        // Then
        assertEquals("TXN-001", result.getTransactionId());
        assertEquals(Status.SUCCESS, result.getStatus());

        wireMock.verify(postRequestedFor(urlEqualTo("/gateway/charge")));
    }
}
```

### 12.2 动态属性注入

```java
@SpringBootTest
class OrderServiceWireMockTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
        .options(wireMockConfig().dynamicPort())
        .build();

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("payment.gateway.url", () ->
            "http://localhost:" + wireMock.getPort() + "/gateway");
        registry.add("sms.provider.url", () ->
            "http://localhost:" + wireMock.getPort() + "/sms");
    }

    @Autowired
    private OrderService orderService;

    // 测试方法...
}
```

### 12.3 Spring Cloud Contract WireMock（备选方案）

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-contract-wiremock</artifactId>
    <scope>test</scope>
</dependency>
```

```java
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;

@SpringBootTest
@AutoConfigureWireMock(port = 0, stubs = "classpath:/mappings/")
class PaymentServiceContractTest {

    @Autowired
    private PaymentService paymentService;

    @Value("${wiremock.server.port}")
    private int wireMockPort;

    @Test
    void testWithAutoConfigure() {
        // @AutoConfigureWireMock 自动管理 WireMockServer
        // Stub 文件从 classpath:/mappings/ 加载
        // 可在测试中动态添加 stub
        stubFor(get(urlEqualTo("/api/health"))
            .willReturn(aResponse().withStatus(200)));
    }
}
```

### 12.4 在 application.yml 中配置外部服务地址

```yaml
# src/main/resources/application.yml
payment:
  gateway:
    url: https://real-payment-gateway.com/api

# src/test/resources/application.yml
payment:
  gateway:
    url: http://localhost:${wiremock.server.port}/api
```

---

## 13. 完整示例：OrderService 调用外部 PaymentService

### 13.1 被测代码

```java
// PaymentGateway.java（外部服务 Feign/RestTemplate 封装）
public class PaymentGateway {

    private final RestTemplate restTemplate;
    private final String gatewayUrl;

    public PaymentGateway(RestTemplate restTemplate, String gatewayUrl) {
        this.restTemplate = restTemplate;
        this.gatewayUrl = gatewayUrl;
    }

    public PaymentResponse charge(PaymentRequest request) {
        return restTemplate.postForObject(
            gatewayUrl + "/charge",
            request,
            PaymentResponse.class);
    }

    public PaymentResponse refund(String transactionId) {
        return restTemplate.postForObject(
            gatewayUrl + "/refund/" + transactionId,
            null,
            PaymentResponse.class);
    }

    public PaymentStatus getStatus(String transactionId) {
        return restTemplate.getForObject(
            gatewayUrl + "/status/" + transactionId,
            PaymentStatus.class);
    }
}

// OrderService.java（业务逻辑）
@Service
public class OrderService {

    private final PaymentGateway paymentGateway;
    private final OrderRepository orderRepository;
    private final NotificationService notificationService;

    public OrderService(PaymentGateway paymentGateway,
                        OrderRepository orderRepository,
                        NotificationService notificationService) {
        this.paymentGateway = paymentGateway;
        this.orderRepository = orderRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public OrderResult placeOrder(OrderRequest request) {
        // Step 1: 创建订单
        Order order = new Order(request.getUserId(), request.getItems());
        order = orderRepository.save(order);

        // Step 2: 发起支付
        PaymentRequest paymentReq = new PaymentRequest(
            order.getId(), order.getTotalAmount(), "CNY");
        PaymentResponse paymentResp = paymentGateway.charge(paymentReq);

        if ("SUCCESS".equals(paymentResp.getStatus())) {
            order.markAsPaid(paymentResp.getTransactionId());
            orderRepository.save(order);

            // Step 3: 发送通知
            notificationService.sendOrderConfirmation(order);

            return OrderResult.success(order.getId(), paymentResp.getTransactionId());
        } else {
            order.markAsFailed(paymentResp.getErrorMsg());
            orderRepository.save(order);
            return OrderResult.failure(paymentResp.getErrorMsg());
        }
    }

    @Transactional
    public RefundResult refundOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));

        PaymentResponse resp = paymentGateway.refund(order.getTransactionId());

        if ("SUCCESS".equals(resp.getStatus())) {
            order.markAsRefunded();
            orderRepository.save(order);
            return RefundResult.success();
        }
        return RefundResult.failure(resp.getErrorMsg());
    }
}
```

### 13.2 集成测试

```java
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class OrderServiceWireMockTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
        .options(wireMockConfig().dynamicPort())
        .build();

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("payment.gateway.url",
            () -> "http://localhost:" + wireMock.getPort());
    }

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
    }

    // ─────────────── 用例 1：正常支付成功 ───────────────

    @Test
    @DisplayName("下单-支付成功-应返回成功结果")
    void placeOrder_WithValidPayment_ShouldSucceed() {
        // Given：模拟支付网关返回成功
        wireMock.stubFor(post(urlPathEqualTo("/charge"))
            .withRequestBody(matchingJsonPath("$.amount"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "transactionId": "TXN-20260720-001",
                        "status": "SUCCESS",
                        "paidAt": "2026-07-20T10:30:00Z"
                    }
                    """)));

        // When
        OrderResult result = orderService.placeOrder(
            new OrderRequest(1L, List.of(new OrderItem(100L, 2))));

        // Then
        assertTrue(result.isSuccess());
        assertEquals("TXN-20260720-001", result.getTransactionId());

        // 验证 WireMock 被正确调用
        wireMock.verify(postRequestedFor(urlPathEqualTo("/charge"))
            .withRequestBody(matchingJsonPath("$.currency", equalTo("CNY"))));

        // 验证数据库状态
        Order savedOrder = orderRepository.findById(result.getOrderId()).orElseThrow();
        assertEquals(OrderStatus.PAID, savedOrder.getStatus());
    }

    // ─────────────── 用例 2：第三方支付失败 ───────────────

    @Test
    @DisplayName("下单-支付网关返回失败-订单应标记为失败")
    void placeOrder_WithPaymentFailure_ShouldMarkOrderFailed() {
        // Given：模拟支付失败
        wireMock.stubFor(post(urlPathEqualTo("/charge"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "status": "FAILED",
                        "errorMsg": "余额不足"
                    }
                    """)));

        // When
        OrderResult result = orderService.placeOrder(
            new OrderRequest(1L, List.of(new OrderItem(100L, 2))));

        // Then
        assertFalse(result.isSuccess());
        assertEquals("余额不足", result.getErrorMsg());
    }

    // ─────────────── 用例 3：支付网关超时 ───────────────

    @Test
    @DisplayName("下单-支付网关超时-应抛出异常")
    void placeOrder_WithGatewayTimeout_ShouldThrowException() {
        // Given：模拟超时
        wireMock.stubFor(post(urlPathEqualTo("/charge"))
            .willReturn(aResponse()
                .withStatus(200)
                .withFixedDelay(5000)));  // 5秒超时

        // 假设 RestTemplate 配置了 3 秒超时
        assertThrows(ResourceAccessException.class, () -> {
            orderService.placeOrder(new OrderRequest(1L, List.of(new OrderItem(100L, 2))));
        });
    }

    // ─────────────── 用例 4：支付网关 500 ───────────────

    @Test
    @DisplayName("下单-支付网关返回500-应处理异常")
    void placeOrder_WithGateway500_ShouldHandleError() {
        // Given：模拟 500
        wireMock.stubFor(post(urlPathEqualTo("/charge"))
            .willReturn(aResponse()
                .withStatus(500)
                .withBody("{\"error\":\"Internal Server Error\"}")));

        assertThrows(HttpServerErrorException.class, () -> {
            orderService.placeOrder(new OrderRequest(1L, List.of(new OrderItem(100L, 2))));
        });
    }

    // ─────────────── 用例 5：退款成功 ───────────────

    @Test
    @DisplayName("退款-支付网关退款成功-订单应标记为已退款")
    void refundOrder_WithSuccessfulRefund_ShouldMarkRefunded() {
        // Given：准备一个已支付的订单
        Order paidOrder = orderRepository.save(
            new Order(1L, List.of(new OrderItem(100L, 2))));
        paidOrder.markAsPaid("TXN-ORIG-001");
        orderRepository.save(paidOrder);

        // 模拟退款响应
        wireMock.stubFor(post(urlPathMatching("/refund/.*"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"status\":\"SUCCESS\"}")));

        // When
        RefundResult result = orderService.refundOrder(paidOrder.getId());

        // Then
        assertTrue(result.isSuccess());
        Order updated = orderRepository.findById(paidOrder.getId()).orElseThrow();
        assertEquals(OrderStatus.REFUNDED, updated.getStatus());

        wireMock.verify(postRequestedFor(urlPathMatching("/refund/.*")));
    }

    // ─────────────── 用例 6：退款重复调用 ───────────────

    @Test
    @DisplayName("退款-已退款的订单再次退款-应拒绝")
    void refundOrder_AlreadyRefunded_ShouldReject() {
        // Given
        Order refundedOrder = orderRepository.save(
            new Order(1L, List.of(new OrderItem(100L, 2))));
        refundedOrder.markAsRefunded();
        orderRepository.save(refundedOrder);

        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            orderService.refundOrder(refundedOrder.getId());
        });
    }
}
```

### 13.3 测试结果分析

| 测试用例 | 验证目标 | WireMock 能力 |
|----------|----------|---------------|
| 支付成功 | 正常业务路径 | Stubbing + JSON Body 匹配 |
| 支付失败 | 第三方返回失败状态 | 自定义响应体 |
| 支付超时 | 客户端超时处理 | `withFixedDelay` |
| 网关 500 | 服务端错误处理 | `withStatus(500)` |
| 退款成功 | 退款业务流程 | URL 正则匹配 |
| 重复退款 | 幂等性/状态控制 | 无 WireMock，纯业务逻辑验证 |

---

## 14. WireMock vs Mockito vs Hoverfly

### 14.1 对比总览

| 维度 | WireMock | Mockito | Hoverfly |
|------|----------|---------|----------|
| **类型** | HTTP 服务模拟 | Java Mock 框架 | HTTP 代理模拟 |
| **层级** | HTTP 网络层 | 方法调用层（JVM 内） | HTTP 网络层（独立代理） |
| **模拟对象** | 外部 HTTP API | Java 接口/类 | 外部 HTTP API |
| **运行方式** | 独立 HTTP 服务器 | 内嵌于 JVM | 独立代理进程 |
| **语言绑定** | 多语言（Java, .NET, Python…） | Java 专用 | 多语言（HTTP 接口） |
| **性能** | 中（网络 IO 开销） | 极快（方法调用） | 中（网络 IO 开销） |
| **录制/回放** | 内置支持 | 不支持 | 内置支持 |
| **故障注入** | 网络级故障（连接重置等） | 异常抛出 | 网络级故障 + 延迟 |
| **延迟模拟** | 固定/随机/对数正态 | 需自定义 | 内置延迟配置 |
| **Templating** | Handlebars 模板引擎 | 无 | 无 |
| **Scenarios** | 内置状态机支持 | 无 | 支持（有限） |
| **管理接口** | REST Admin API | 无 | REST API |
| **SSL/TLS** | 内置支持 | 无 | 内置 MITM |

### 14.2 选择建议

```
┌────────────────────────────────────────────────────────────┐
│  你模拟的是什么？                                           │
│                                                            │
│  HTTP 调用外部 API（REST/SOAP/GraphQL）                     │
│      ├── 需要录制/回放真实响应 → WireMock（或 Hoverfly）    │
│      ├── 需要精确的故障注入   → WireMock                   │
│      └── 需要动态响应模板     → WireMock                   │
│                                                            │
│  Java 内部方法调用（Service → Repository）                   │
│      └── Mockito                                            │
│                                                            │
│  需要拦截整个 JVM 的出站 HTTP 流量                           │
│      └── Hoverfly（透明代理模式）                           │
└────────────────────────────────────────────────────────────┘
```

### 14.3 何时组合使用

```java
// Mockito + WireMock 是最佳组合
// Mockito 模拟内部依赖，WireMock 模拟外部 HTTP API
@SpringBootTest
class OrderServiceFullTest {

    // WireMock 模拟外部支付网关
    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
        .options(wireMockConfig().dynamicPort())
        .build();

    @MockBean  // Mockito 模拟消息队列（内部依赖）
    private NotificationService notificationService;

    @Autowired
    private OrderService orderService;

    @Test
    void placeOrder_ShouldCallPaymentAndNotification() {
        // WireMock: 模拟外部 HTTP API
        wireMock.stubFor(post(urlPathEqualTo("/charge"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("{\"status\":\"SUCCESS\"}")));

        // Mockito: 模拟内部消息发送
        doNothing().when(notificationService)
            .sendOrderConfirmation(any(Order.class));

        // 执行测试
        OrderResult result = orderService.placeOrder(...);

        // 验证外部调用
        wireMock.verify(postRequestedFor(urlPathEqualTo("/charge")));

        // 验证内部调用
        verify(notificationService).sendOrderConfirmation(any(Order.class));
    }
}
```

> 🎯 **最佳实践总结**：对于涉及外部 HTTP API 的集成测试，WireMock 是首选方案。Mockito 用于模拟内部依赖。在微服务架构中，每个微服务的测试通常同时使用 Mockito（模拟本服务内部依赖）和 WireMock（模拟其他微服务）。Hoverfly 适合需要透明的全局代理拦截场景，但在精细控制和 Java 生态集成方面不如 WireMock 成熟。

### 14.4 性能参考

| 方式 | 单测试耗时 | 启动时间 | 适用规模 |
|------|-----------|----------|----------|
| Mockito（方法模拟） | ~1-5ms | 0s | 单元测试 |
| WireMock（内嵌JVM） | ~5-20ms | ~500ms | 集成测试 |
| WireMock（独立进程） | ~5-20ms | ~2s | 集成测试/契约测试 |
| Hoverfly（独立代理） | ~10-30ms | ~3s | 集成测试 |
| 真实 HTTP 调用 | 100ms+ | 依赖网络 | E2E 测试 |

---

> ⚠️ **注意事项**：
> 1. WireMock 的 Stub 默认会在 `mappings/` 目录下持久化，使用 `@WireMockExtension` 的 `@BeforeEach` 中建议调用 `WireMock.reset()` 避免 Stub 污染。
> 2. WireMock 只模拟 HTTP 协议的交互，不能模拟 HTTPS 证书链验证等安全细节（除非配置 SSL）。
> 3. 使用录制功能时注意处理敏感数据（API Key、密码等），录制后应审查并清理。
> 4. 对于高并发测试，WireMock 的默认线程池可能成为瓶颈，可通过 `wireMockConfig().containerThreads(16)` 调整。

> 🎯 **WireMock 核心优势**：在真实的 HTTP 协议层面模拟外部服务，同时提供精确的请求匹配、丰富的响应定义和强大的验证能力，是 Java 微服务集成测试中模拟外部 HTTP API 的事实标准工具。
