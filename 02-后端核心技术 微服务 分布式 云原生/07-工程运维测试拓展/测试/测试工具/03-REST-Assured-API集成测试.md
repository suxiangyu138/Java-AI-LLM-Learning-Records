# REST Assured API 集成测试
> REST Assured 是一款基于 Java 的 DSL（领域特定语言）库，专为 RESTful API 的自动化测试而设计。它提供了一套简洁的 Given-When-Then 风格 API，使得接口测试代码可读性极高，是与 Spring Boot 等 Java 后端框架配合进行集成测试的首选工具。

## 目录

1. [REST Assured 概述与依赖](#1-rest-assured-概述与依赖)
2. [Given-When-Then 模式](#2-given-when-then-模式)
3. [GET 请求](#3-get-请求)
4. [POST / PUT / PATCH / DELETE 请求](#4-post--put--patch--delete-请求)
5. [响应验证](#5-响应验证)
6. [JSON 响应提取 (JsonPath)](#6-json-响应提取-jsonpath)
7. [XML 响应提取 (XmlPath)](#7-xml-响应提取-xmlpath)
8. [认证机制](#8-认证机制)
9. [请求/响应日志](#9-请求响应日志)
10. [请求规范与响应规范复用](#10-请求规范与响应规范复用)
11. [Hamcrest 匹配器](#11-hamcrest-匹配器)
12. [JSON Schema 验证](#12-json-schema-验证)
13. [Spring Boot 集成测试](#13-spring-boot-集成测试)
14. [完整示例：User CRUD API 测试](#14-完整示例user-crud-api-测试)
15. [REST Assured vs MockMvc vs TestRestTemplate](#15-rest-assured-vs-mockmvc-vs-testresttemplate)

---

## 1. REST Assured 概述与依赖

### 1.1 什么是 REST Assured

> REST Assured 由 Johan Haleby 创建，是 Java 生态中最流行的 REST API 测试框架。它借鉴了 HTTP 的语义（Given 准备、When 执行、Then 验证），将 API 测试代码组织成流畅的链式调用，使得测试脚本既是代码也是可读的测试文档。

### 1.2 核心特性

| 特性 | 描述 |
|------|------|
| **Given-When-Then DSL** | 语义化的链式 API，代码即文档 |
| **多协议支持** | HTTP / HTTPS（包括 RESTful API 和 SOAP） |
| **多种认证** | Basic Auth、OAuth2、Form Auth、Digest Auth |
| **JSON/XML 原生支持** | 内建 JsonPath 和 XmlPath 解析引擎 |
| **Schema 验证** | 支持 JSON Schema Draft 4/7 验证 |
| **Hamcrest 集成** | 丰富的匹配器语法，断言简洁强大 |
| **日志支持** | 请求/响应全链路日志，便于调试 |
| **Spring Boot 集成** | @SpringBootTest + 随机端口无缝集成 |
| **DTO 序列化** | 自动将 Java 对象序列化为 JSON/XML 请求体 |
| **静态导入友好** | 静态方法设计，测试代码极其简洁 |

### 1.3 Maven / Gradle 依赖

#### Maven

```xml
<!-- REST Assured 核心 -->
<dependency>
    <groupId>io.rest-assured</groupId>
    <artifactId>rest-assured</artifactId>
    <version>5.4.0</version>
    <scope>test</scope>
</dependency>

<!-- JSON Schema 验证 -->
<dependency>
    <groupId>io.rest-assured</groupId>
    <artifactId>json-schema-validator</artifactId>
    <version>5.4.0</version>
    <scope>test</scope>
</dependency>

<!-- XML 路径支持（酌情添加） -->
<dependency>
    <groupId>io.rest-assured</groupId>
    <artifactId>xml-path</artifactId>
    <version>5.4.0</version>
    <scope>test</scope>
</dependency>

<!-- Spring Boot 测试集成 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

#### Gradle

```groovy
dependencies {
    testImplementation 'io.rest-assured:rest-assured:5.4.0'
    testImplementation 'io.rest-assured:json-schema-validator:5.4.0'
    testImplementation 'io.rest-assured:xml-path:5.4.0'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}
```

### 1.4 静态导入（推荐）

```java
// 核心 API 静态导入
import static io.restassured.RestAssured.*;
import static io.restassured.matcher.RestAssuredMatchers.*;
import static org.hamcrest.Matchers.*;

// 使用后，测试代码可简化为：
given()
    .param("key", "value")
.when()
    .get("/api/resource")
.then()
    .statusCode(200)
    .body("data", hasSize(10));
```

---

## 2. Given-When-Then 模式

### 2.1 基本结构

REST Assured 遵循 BDD（Behavior-Driven Development）风格的 Given-When-Then 模式：

```java
given()
    // 前置条件：请求配置、参数、头信息、认证等
    .header("Content-Type", "application/json")
    .param("page", 1)
    .auth().oauth2(accessToken)
.when()
    // 执行动作：发送 HTTP 请求
    .get("/api/users")
.then()
    // 验证结果：状态码、响应体、头信息等
    .statusCode(200)
    .body("code", equalTo(0))
    .body("data.total", greaterThan(0));
```

| 阶段 | 英文 | 含义 | 常见操作 |
|------|------|------|----------|
| **准备** | Given | 配置测试前置条件 | 参数、头信息、认证、Cookie、请求体 |
| **执行** | When | 发送 HTTP 请求 | GET、POST、PUT、DELETE、PATCH |
| **验证** | Then | 验证响应结果 | 状态码、响应体、头信息、响应时间 |

### 2.2 链式调用的本质

REST Assured 的 API 设计基于 RequestSpecBuilder -> RequestSpecification -> Response -> ResponseSpecification 的构建器模式。

### 2.3 提取与验证分离

REST Assured 支持从响应中提取数据，供后续测试或断言使用：

```java
// 提取单个值
String token = given()
    .body(loginRequest)
.when()
    .post("/api/auth/login")
.then()
    .statusCode(200)
    .extract()
    .path("data.token");

// 提取整个响应对象
Response response = given()
    .queryParam("page", 1)
.when()
    .get("/api/users")
.then()
    .extract()
    .response();

// 提取为 Java 对象
UserDTO user = given()
    .pathParam("id", 1001)
.when()
    .get("/api/users/{id}")
.then()
    .extract()
    .as(UserDTO.class);

// 提取为 List
List<UserDTO> users = given()
.when()
    .get("/api/users")
.then()
    .extract()
    .jsonPath()
    .getList("data", UserDTO.class);
```

> 💡 设计原则：Given-When-Then 模式使测试用例天然成为可执行的需求文档。团队评审时，非技术人员也能理解测试意图。

---

## 3. GET 请求

### 3.1 基础 GET 请求

```java
// 最简单形式
when()
    .get("https://api.example.com/users")
.then()
    .statusCode(200);

// 使用静态 baseURI 配置（推荐）
@BeforeAll
static void setup() {
    baseURI = "https://api.example.com";
    basePath = "/api";
}

@Test
void testGetUsers() {
    when()
        .get("/users")
    .then()
        .statusCode(200)
        .body("code", equalTo(0));
}
```

### 3.2 Path 参数

```java
// 方式一：直接在路径中使用占位符
given()
    .pathParam("userId", 1001)
.when()
    .get("/users/{userId}")
.then()
    .statusCode(200);

// 方式二：多个 Path 参数
given()
    .pathParam("orgId", 50)
    .pathParam("userId", 1001)
.when()
    .get("/orgs/{orgId}/users/{userId}")
.then()
    .statusCode(200);

// 方式三：Map 批量设置
Map<String, Object> pathParams = new HashMap<>();
pathParams.put("orgId", 50);
pathParams.put("userId", 1001);

given()
    .pathParams(pathParams)
.when()
    .get("/orgs/{orgId}/users/{userId}")
.then()
    .statusCode(200);
```

### 3.3 Query 参数

```java
// 单个参数
given()
    .queryParam("page", 1)
    .queryParam("size", 20)
.when()
    .get("/users")
.then()
    .statusCode(200);

// 使用 params() 批量设置
given()
    .params("page", 1, "size", 20, "sort", "name,asc")
.when()
    .get("/users")
.then()
    .statusCode(200);

// Map 参数
Map<String, Object> queryParams = new HashMap<>();
queryParams.put("page", 1);
queryParams.put("size", 20);

given()
    .queryParams(queryParams)
.when()
    .get("/users")
.then()
    .statusCode(200);
```

### 3.4 Header 和 Cookie

```java
// 自定义 Header
given()
    .header("X-Request-ID", UUID.randomUUID().toString())
    .header("X-Client-Version", "1.2.3")
.when()
    .get("/users")
.then()
    .statusCode(200);

// Cookie
given()
    .cookie("sessionId", "abc123xyz")
.when()
    .get("/users/profile")
.then()
    .statusCode(200);
```

### 3.5 完整的 GET 测试用例

```java
@Test
void testGetUserById_ShouldReturnUser_WhenUserExists() {
    given()
        .pathParam("id", 1001)
        .header("Accept", "application/json")
    .when()
        .get("/users/{id}")
    .then()
        .statusCode(200)
        .header("Content-Type", containsString("application/json"))
        .body("code", equalTo(0))
        .body("data.id", equalTo(1001))
        .body("data.name", notNullValue())
        .time(lessThan(2000L));
}

@Test
void testGetUserById_ShouldReturn404_WhenUserNotExists() {
    given()
        .pathParam("id", 99999)
    .when()
        .get("/users/{id}")
    .then()
        .statusCode(404)
        .body("code", equalTo(40401))
        .body("message", containsString("not found"));
}
```

---

## 4. POST / PUT / PATCH / DELETE 请求

### 4.1 POST 请求

#### 使用 JSON 字符串请求体

```java
@Test
void testCreateUser_WithJsonString() {
    String requestBody = """
        {
            "name": "Alice",
            "email": "alice@example.com",
            "password": "SecurePass123!",
            "role": "admin"
        }
        """;

    given()
        .header("Content-Type", "application/json")
        .body(requestBody)
    .when()
        .post("/users")
    .then()
        .statusCode(201)
        .body("code", equalTo(0))
        .body("data.id", notNullValue())
        .body("data.name", equalTo("Alice"))
        .header("Location", notNullValue());
}
```

#### 使用 Java 对象自动序列化

```java
// 1. 定义 DTO
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateUserRequest {
    private String name;
    private String email;
    private String password;
    private String role;
}

// 2. 测试方法
@Test
void testCreateUser_WithJavaObject() {
    CreateUserRequest request = new CreateUserRequest(
        "Bob", "bob@example.com", "P@ssw0rd", "editor"
    );

    ApiResponse<UserDTO> response = given()
        .contentType(ContentType.JSON)
        .body(request)
    .when()
        .post("/users")
    .then()
        .statusCode(201)
        .extract()
        .as(new TypeRef<ApiResponse<UserDTO>>() {});

    assertThat(response.getData().getId()).isNotNull();
    assertThat(response.getData().getName()).isEqualTo("Bob");
}
```

> 💡 序列化机制：REST Assured 默认使用 Jackson（若在 classpath 上），其次支持 Gson。通过 body() 传入 Java 对象时，会自动调用 ObjectMapper 序列化为 JSON 字符串。

#### Form 参数

```java
@Test
void testLogin_WithFormParams() {
    given()
        .contentType("application/x-www-form-urlencoded")
        .formParam("username", "admin")
        .formParam("password", "password123")
    .when()
        .post("/auth/login")
    .then()
        .statusCode(200)
        .body("code", equalTo(0))
        .body("data.token", notNullValue());
}
```

#### Multipart 文件上传

```java
@Test
void testUploadFile() {
    given()
        .multiPart("file", new File("src/test/resources/test-upload.pdf"))
        .multiPart("description", "Test file upload")
    .when()
        .post("/files/upload")
    .then()
        .statusCode(201)
        .body("code", equalTo(0))
        .body("data.fileId", notNullValue());
}

@Test
void testUploadMultipleFiles() {
    given()
        .multiPart("files", new File("src/test/resources/file1.pdf"))
        .multiPart("files", new File("src/test/resources/file2.pdf"))
    .when()
        .post("/files/batch-upload")
    .then()
        .statusCode(200);
}

@Test
void testUploadFileWithByteArray() {
    byte[] fileContent = Files.readAllBytes(Paths.get("src/test/resources/image.png"));
    given()
        .multiPart("file", "image.png", fileContent, "image/png")
    .when()
        .post("/files/upload")
    .then()
        .statusCode(201);
}
```

### 4.2 PUT 请求（全量更新）

```java
@Test
void testUpdateUser_ShouldSucceed() {
    String updateBody = """
        {
            "name": "Alice Updated",
            "email": "alice.new@example.com",
            "role": "editor"
        }
        """;

    given()
        .pathParam("id", 1001)
        .contentType(ContentType.JSON)
        .body(updateBody)
    .when()
        .put("/users/{id}")
    .then()
        .statusCode(200)
        .body("code", equalTo(0))
        .body("data.name", equalTo("Alice Updated"));
}
```

### 4.3 PATCH 请求（部分更新）

```java
@Test
void testPartialUpdateUser() {
    String patchBody = """
        { "email": "alice.changed@example.com" }
        """;

    given()
        .pathParam("id", 1001)
        .contentType(ContentType.JSON)
        .body(patchBody)
    .when()
        .patch("/users/{id}")
    .then()
        .statusCode(200)
        .body("data.email", equalTo("alice.changed@example.com"));
}
```

### 4.4 DELETE 请求

```java
@Test
void testDeleteUser_ShouldSucceed() {
    given()
        .pathParam("id", 1001)
    .when()
        .delete("/users/{id}")
    .then()
        .statusCode(200)
        .body("code", equalTo(0));

    // 验证已删除
    given()
        .pathParam("id", 1001)
    .when()
        .get("/users/{id}")
    .then()
        .statusCode(404);
}

@Test
void testBatchDeleteUsers() {
    List<Integer> userIds = List.of(2001, 2002, 2003);

    given()
        .contentType(ContentType.JSON)
        .body(Map.of("ids", userIds))
    .when()
        .delete("/users/batch")
    .then()
        .statusCode(200)
        .body("data.deletedCount", equalTo(3));
}


---

## 5. 响应验证

### 5.1 状态码验证

```java
// 精确匹配
.then().statusCode(200);

// 使用 Hamcrest 匹配器
.then().statusCode(allOf(greaterThanOrEqualTo(200), lessThan(300)));

// 验证错误状态码
.then().statusCode(400);
.then().statusCode(401);
.then().statusCode(403);
.then().statusCode(404);
.then().statusCode(500);
```

### 5.2 Header 验证

```java
// 单个 Header 验证
.then()
    .header("Content-Type", "application/json")
    .header("X-Request-ID", notNullValue());

// Header 值包含特定字符串
.then()
    .header("Content-Type", containsString("json"));

// 多个 Header 值
.then()
    .header("Set-Cookie", allOf(
        containsString("sessionId"),
        containsString("HttpOnly"),
        containsString("Secure")
    ));
```

### 5.3 Cookie 验证

```java
.then().cookie("sessionId");
.then().cookie("sessionId", "abc123xyz");
.then()
    .detailedCookie("sessionId")
        .value(notNullValue())
        .path("/")
        .httpOnly(true)
        .secured(true);
```

### 5.4 Content-Type 验证

```java
.then().contentType(ContentType.JSON);
.then().contentType(ContentType.XML);
```

### 5.5 响应时间验证

```java
.then().time(lessThan(2000L));
.then().time(both(greaterThan(100L)).and(lessThan(1000L)));

long responseTime = given()
    .when().get("/users").time();
```

### 5.6 响应体完整验证

```java
@Test
void testFullResponseValidation() {
    given()
        .queryParam("page", 1)
        .queryParam("size", 10)
    .when()
        .get("/users")
    .then()
        .statusCode(200)
        .header("Content-Type", containsString("json"))
        .time(lessThan(3000L))
        .body("code", equalTo(0))
        .body("data", hasSize(10))
        .body("data[0].id", notNullValue())
        .body("data[0].name", not(emptyString()));
}
```

---

## 6. JSON 响应提取 (JsonPath)

### 6.1 JsonPath 概述

REST Assured 内建了强大的 JsonPath 实现，支持从 JSON 响应中提取复杂路径的数据。

```java
String json = "{\"store\":{\"books\":[{\"title\":\"Book1\",\"price\":10},{\"title\":\"Book2\",\"price\":20}]}}";
JsonPath jsonPath = new JsonPath(json);

String title = jsonPath.getString("store.books[0].title");
int price = jsonPath.getInt("store.books[0].price");
List<String> allTitles = jsonPath.getList("store.books.title");
```

### 6.2 根元素提取

```java
// 假设响应: {"code":0, "message":"success", "data":{...}}
.then()
    .body("code", equalTo(0))
    .body("message", equalTo("success"));

int code = response.path("code");
```

### 6.3 嵌套对象提取

```java
// 假设响应: {"data": {"user": {"id": 1001, "name": "Alice", "profile": {"age": 28}}}}
.then()
    .body("data.user.id", equalTo(1001))
    .body("data.user.name", equalTo("Alice"))
    .body("data.user.profile.age", equalTo(28));

// 提取嵌套对象
User user = response.jsonPath().getObject("data.user", User.class);
```

### 6.4 数组元素提取

```java
// 假设响应: {"data": [{"id": 1, "name": "Alice"}, {"id": 2, "name": "Bob"}]}

.then().body("data.size()", equalTo(2));
.then().body("data[0].name", equalTo("Alice"));
.then().body("data[-1].name", equalTo("Bob"));

List<Integer> ids = response.jsonPath().getList("data.id");
List<User> users = response.jsonPath().getList("data", User.class);
```

### 6.5 条件过滤

```java
.then().body("data.find { it.role == 'admin' }.name", equalTo("Alice"));
.then().body("data.findAll { it.role == 'admin' }.name", hasItems("Alice", "Diana"));
.then().body("data.findAll { it.id > 1 && it.role == 'admin' }.size()", equalTo(1));
```

### 6.6 高级 JsonPath

```java
int minId = response.jsonPath().getInt("data.min { it.id }.id");
List<String> sortedNames = response.jsonPath().getList("data.sort { it.name }.name");
boolean hasAdmin = response.jsonPath().getBoolean("data.any { it.role == 'admin' }");
```

---

## 7. XML 响应提取 (XmlPath)

### 7.1 XmlPath 基础

```java
// 假设 XML 响应:
// <response>
//     <code>0</code>
//     <data><user><id>1001</id><name>Alice</name></user></data>
// </response>

.then()
    .body("response.code", equalTo("0"))
    .body("response.data.user.name", equalTo("Alice"));

// XML 属性: <user id="1001" name="Alice" />
.then()
    .body("user.@id", equalTo("1001"));
```

### 7.2 XML Namespace 处理

```java
XmlPath xmlPath = new XmlPath(xmlResponse)
    .namespace("soap", "http://schemas.xmlsoap.org/soap/envelope/")
    .namespace("ns2", "http://service.example.com");

String userId = xmlPath.getString("soap:Envelope.soap:Body.ns2:getUserResponse.user.id");
```

> \u{1f4a1} REST API 优先使用 JSON：现代 REST API 绝大多数使用 JSON 格式。XML 路径提取主要用于遗留系统或 SOAP 服务的测试。



---

## 8. 认证机制

### 8.1 Basic Auth

```java
// HTTP Basic 认证
given()
    .auth()
    .basic("username", "password")
.when()
    .get("/api/protected/resource")
.then()
    .statusCode(200);

// Preemptive Basic Auth（不等待 401 质询，直接发送）
given()
    .auth()
    .preemptive()
    .basic("username", "password")
.when()
    .get("/api/protected/resource")
.then()
    .statusCode(200);
```

| Basic Auth 类型 | 说明 | 使用场景 |
|-----------------|------|----------|
| .auth().basic() | 标准 Basic Auth，先发请求，收到 401 后再带认证头重试 | 标准 HTTP 流程 |
| .auth().preemptive().basic() | 预先发送认证头，不等待质询 | 避免额外往返 |

### 8.2 OAuth2

```java
// OAuth2 Bearer Token
given()
    .auth()
    .oauth2("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
.when()
    .get("/api/users")
.then()
    .statusCode(200);

// 从认证服务获取 Token 后使用
@Test
void testWithDynamicOAuth2Token() {
    String accessToken = given()
        .contentType("application/x-www-form-urlencoded")
        .formParam("grant_type", "client_credentials")
        .formParam("client_id", "my-client")
        .formParam("client_secret", "my-secret")
    .when()
        .post("https://auth.example.com/oauth/token")
    .then()
        .statusCode(200)
        .extract()
        .path("access_token");

    given()
        .auth().oauth2(accessToken)
    .when()
        .get("/api/users")
    .then()
        .statusCode(200);
}

// 使用 Authorization Header 手动设置
given()
    .header("Authorization", "Bearer " + accessToken)
.when()
    .get("/api/users")
.then()
    .statusCode(200);
```

### 8.3 Form Auth

```java
given()
    .auth()
    .form("admin", "password123",
        new FormAuthConfig("/login", "username", "password"))
.when()
    .get("/api/protected/resource")
.then()
    .statusCode(200);
```

### 8.4 API Key 认证

```java
// API Key 在 Header 中
given()
    .header("X-API-Key", "api-key-12345")
.when()
    .get("/api/protected/data")
.then()
    .statusCode(200);

// API Key 在 Query Param 中
given()
    .queryParam("api_key", "api-key-12345")
.when()
    .get("/api/protected/data")
.then()
    .statusCode(200);
```

---

## 9. 请求/响应日志

### 9.1 日志方法总览

| 日志方法 | 作用 | 适用阶段 |
|----------|------|----------|
| log().all() | 记录请求/响应的所有信息 | 调试阶段 |
| log().method() | 仅记录 HTTP 方法 | 简洁日志 |
| log().path() | 仅记录请求路径 | 简洁日志 |
| log().params() | 仅记录请求参数 | 参数敏感时 |
| log().body() | 仅记录请求/响应体 | 体数据敏感时 |
| log().ifValidationFails() | 仅当验证失败时记录 | 生产测试推荐 |
| log().status() | 仅记录状态码 | 最简洁 |

### 9.2 请求日志

```java
given()
    .log().all()
    .header("Content-Type", "application/json")
    .body("{\"name\":\"Alice\"}")
.when()
    .post("/users");
```

### 9.3 响应日志

```java
when()
    .get("/users/1001")
.then()
    .log().all()
    .statusCode(200);
```

### 9.4 条件日志（推荐）

```java
// 仅在验证失败时记录日志
given()
    .body(loginRequest)
.when()
    .post("/auth/login")
.then()
    .log().ifValidationFails()
    .statusCode(200)
    .body("code", equalTo(0));

// 仅在错误状态码时记录
given()
.when()
    .get("/users/99999")
.then()
    .log().ifStatusCodeIsEqualTo(404)
    .statusCode(404);
```

### 9.5 日志配置覆盖

```java
// 全局配置默认日志策略
@BeforeAll
static void setup() {
    RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
}

RestAssured.config = RestAssured.config()
    .logConfig(new LogConfig()
        .enableLoggingOfRequestAndResponseIfValidationFails()
        .enablePrettyPrinting(true)
        .blacklistHeader("Authorization")
        .blacklistHeader("Cookie")
    );
```

---

## 10. 请求规范与响应规范复用

### 10.1 RequestSpecification（请求规范）

```java
public class BaseApiTest {

    protected static RequestSpecification requestSpec;

    @BeforeAll
    static void setupRequestSpec() {
        requestSpec = new RequestSpecBuilder()
            .setBaseUri("http://localhost")
            .setPort(8080)
            .setBasePath("/api")
            .setContentType(ContentType.JSON)
            .setAccept(ContentType.JSON)
            .addHeader("X-Client-Name", "rest-assured-test")
            .addQueryParam("locale", "zh_CN")
            .setRelaxedHTTPSValidation()
            .build();
    }

    @Test
    void testGetUsers() {
        given()
            .spec(requestSpec)
            .queryParam("page", 1)
        .when()
            .get("/users")
        .then()
            .statusCode(200);
    }
}
```

#### 请求规范常用配置

| 配置方法 | 说明 | 示例 |
|----------|------|------|
| setBaseUri() | 设置基础 URI | http://localhost |
| setPort() | 设置端口 | 8080 |
| setBasePath() | 设置基础路径 | /api/v2 |
| setContentType() | 默认 Content-Type | ContentType.JSON |
| setAccept() | 默认 Accept | ContentType.JSON |
| addHeader() | 添加请求头 | X-Request-Id |
| addQueryParam() | 添加公共 Query 参数 | locale=zh_CN |
| setAuth() | 设置认证 | oauth2(token) |
| setRelaxedHTTPSValidation() | 跳过 SSL 证书验证 | 测试环境使用 |

### 10.2 ResponseSpecification（响应规范）

```java
public class BaseApiTest {

    protected static ResponseSpecification successSpec;

    @BeforeAll
    static void setupResponseSpec() {
        successSpec = new ResponseSpecBuilder()
            .expectStatusCode(200)
            .expectContentType(ContentType.JSON)
            .expectBody("code", equalTo(0))
            .expectBody("message", equalTo("success"))
            .expectResponseTime(lessThan(3000L))
            .build();

        ResponseSpecification notFoundSpec = new ResponseSpecBuilder()
            .expectStatusCode(404)
            .expectContentType(ContentType.JSON)
            .expectBody("code", equalTo(40401))
            .build();
    }

    @Test
    void testGetUsers() {
        given()
            .spec(requestSpec)
        .when()
            .get("/users")
        .then()
            .spec(successSpec);
    }
}
```

> \u{1f389} 复用最佳实践：将公共配置提取到基类或工具类中，每个测试只关注自身的业务验证逻辑。遵循 DRY 原则，大幅减少测试代码量。

---

## 11. Hamcrest 匹配器

### 11.1 常用匹配器速查

```java
import static org.hamcrest.Matchers.*;
```

| 分类 | 匹配器 | 含义 | 示例 |
|------|--------|------|------|
| **相等性** | equalTo() | 等于 | equalTo("Alice") |
| | is() | 是（语法糖） | is("Alice") |
| | not() | 非 | not(equalTo("Bob")) |
| | nullValue() | null 值 | nullValue() |
| | notNullValue() | 非 null | notNullValue() |
| **字符串** | containsString() | 包含子串 | containsString("@") |
| | startsWith() | 以...开头 | startsWith("user_") |
| | endsWith() | 以...结尾 | endsWith(".com") |
| | matchesPattern() | 正则匹配 | matchesPattern("\\d+") |
| **数值** | greaterThan() | 大于 | greaterThan(0) |
| | lessThan() | 小于 | lessThan(100) |
| **集合** | hasSize() | 集合大小 | hasSize(10) |
| | hasItem() | 包含元素 | hasItem("admin") |
| | hasItems() | 包含多个元素 | hasItems("admin", "editor") |
| | containsInAnyOrder() | 包含（无序） | containsInAnyOrder("B", "A") |
| **逻辑** | allOf() | 全部满足（AND） | allOf(greaterThan(0), lessThan(100)) |
| | anyOf() | 满足其一（OR） | anyOf(is("A"), is("B")) |

### 11.2 在 REST Assured 中使用

```java
@Test
void testHamcrestMatchers() {
    given()
        .queryParam("page", 1)
    .when()
        .get("/users")
    .then()
        .statusCode(200)
        .body("message", is("success"))
        .body("code", equalTo(0))
        .body("data.total", greaterThan(0))
        .body("data.records", hasSize(20))
        .body("data.records.name", hasItems("Alice", "Bob"))
        .body("data.error", nullValue())
        .body("data.records[0].id",
            allOf(greaterThan(0), lessThan(10000)))
        .time(lessThan(2000L));
}
```



---

## 12. JSON Schema 验证

### 12.1 什么是 JSON Schema

JSON Schema 是一种声明式语言，用于描述 JSON 数据的结构和约束。通过 JSON Schema 验证，可以确保 API 响应始终符合约定的格式。

### 12.2 Schema 文件定义

```json
{
    "$schema": "https://json-schema.org/draft-07/schema#",
    "title": "User List Response",
    "type": "object",
    "required": ["code", "message", "data"],
    "properties": {
        "code": { "type": "integer", "minimum": 0, "maximum": 99999 },
        "message": { "type": "string" },
        "data": {
            "type": "object",
            "required": ["total", "records"],
            "properties": {
                "total": { "type": "integer", "minimum": 0 },
                "records": {
                    "type": "array",
                    "items": { "$ref": "#/definitions/User" }
                }
            }
        }
    },
    "definitions": {
        "User": {
            "type": "object",
            "required": ["id", "name", "email"],
            "properties": {
                "id": { "type": "integer", "minimum": 1 },
                "name": { "type": "string", "minLength": 1 },
                "email": { "type": "string", "format": "email" },
                "role": { "type": "string", "enum": ["admin", "editor", "viewer"] }
            }
        }
    }
}
```

### 12.3 Schema 验证

```java
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;

// 从 classpath 加载 Schema
@Test
void testResponseMatchesSchema() {
    given()
        .queryParam("page", 1)
        .queryParam("size", 20)
    .when()
        .get("/users")
    .then()
        .statusCode(200)
        .body(matchesJsonSchemaInClasspath("schemas/user-list-response.json"));
}

// 从文件系统加载 Schema
@Test
void testWithExternalSchema() {
    given()
        .get("/users")
    .then()
        .body(matchesJsonSchema(
            new File("src/test/resources/schemas/user-list.json")));
}
```

---

## 13. Spring Boot 集成测试

### 13.1 基础集成配置

REST Assured 与 Spring Boot 的集成测试非常自然，通过 @SpringBootTest 和随机端口即可启动完整的应用上下文。

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserApiIntegrationTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setup() {
        RestAssured.port = port;
        RestAssured.baseURI = "http://localhost";
        RestAssured.basePath = "/api";
    }

    @Test
    void contextLoads() {
        // 验证 Spring 上下文可以正常加载
    }
}
```

### 13.2 使用 RequestSpecification 优化

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class BaseIntegrationTest {

    @LocalServerPort
    protected int port;

    protected RequestSpecification requestSpec;
    protected ResponseSpecification responseSpec;

    @BeforeEach
    void setupBase() {
        requestSpec = new RequestSpecBuilder()
            .setBaseUri("http://localhost")
            .setPort(port)
            .setBasePath("/api")
            .setContentType(ContentType.JSON)
            .setAccept(ContentType.JSON)
            .build();

        responseSpec = new ResponseSpecBuilder()
            .expectStatusCode(200)
            .expectContentType(ContentType.JSON)
            .expectBody("code", equalTo(0))
            .build();
    }
}
```

### 13.3 带数据库的集成测试

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class UserRepositoryIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.save(new User(null, "Alice", "alice@test.com", "admin"));
        userRepository.save(new User(null, "Bob", "bob@test.com", "editor"));
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    @Test
    void testGetUsers_ShouldReturnAllUsers() {
        given()
            .spec(requestSpec)
        .when()
            .get("/users")
        .then()
            .spec(responseSpec)
            .body("data.total", equalTo(2))
            .body("data.records.name", hasItems("Alice", "Bob"));
    }

    @Test
    void testCreateUser_ShouldPersistToDatabase() {
        int userId = given()
            .spec(requestSpec)
            .body(new CreateUserRequest("Diana", "diana@test.com", "pass123", "editor"))
        .when()
            .post("/users")
        .then()
            .statusCode(201)
            .extract()
            .path("data.id");

        assertThat(userRepository.findById((long) userId)).isPresent();
    }
}
```

### 13.4 使用 @Sql 注解管理测试数据

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql(scripts = "/sql/init-users.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/cleanup-users.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class UserApiSqlIntegrationTest extends BaseIntegrationTest {

    @Test
    void testGetUsers_WithSqlInitData() {
        given()
            .spec(requestSpec)
        .when()
            .get("/users")
        .then()
            .spec(responseSpec)
            .body("data.records", hasSize(5));
    }
}
```

### 13.5 带安全认证的集成测试

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SecuredApiIntegrationTest extends BaseIntegrationTest {

    private String adminToken;

    @BeforeEach
    void setUp() {
        adminToken = given()
            .spec(requestSpec)
            .body(new LoginRequest("admin", "admin123"))
        .when()
            .post("/auth/login")
        .then()
            .statusCode(200)
            .extract()
            .path("data.token");
    }

    @Test
    void testSecuredEndpoint_WithValidToken() {
        given()
            .spec(requestSpec)
            .auth().oauth2(adminToken)
        .when()
            .get("/admin/users")
        .then()
            .spec(responseSpec);
    }

    @Test
    void testSecuredEndpoint_WithoutToken_ShouldReturn401() {
        given()
            .spec(requestSpec)
        .when()
            .get("/admin/users")
        .then()
            .statusCode(401);
    }

    @Test
    void testRoleBasedAccess() {
        String editorToken = given()
            .spec(requestSpec)
            .body(new LoginRequest("editor", "editor123"))
        .when()
            .post("/auth/login")
        .then()
            .extract()
            .path("data.token");

        given()
            .spec(requestSpec)
            .auth().oauth2(editorToken)
        .when()
            .get("/admin/users")
        .then()
            .statusCode(403);
    }
}
```




---

## 14. 完整示例：User CRUD API 测试

### 14.1 被测 API 说明

假设有一个标准的 User CRUD REST API：

| 方法 | 路径 | 描述 | 成功响应 |
|------|------|------|----------|
| GET | /api/users | 获取用户列表 | 200: 分页列表 |
| GET | /api/users/{id} | 获取单个用户 | 200: 用户详情 |
| POST | /api/users | 创建用户 | 201: 创建的用户 |
| PUT | /api/users/{id} | 全量更新用户 | 200: 更新后的用户 |
| PATCH | /api/users/{id} | 部分更新用户 | 200: 更新后的用户 |
| DELETE | /api/users/{id} | 删除用户 | 200: 成功消息 |

### 14.2 完整测试代码

```java
package com.example.api.user;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.common.mapper.TypeRef;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.module.jsv.JsonSchemaValidator;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("User CRUD API \u96c6\u6210\u6d4b\u8bd5")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserApiFullIntegrationTest {

    @LocalServerPort
    private int port;

    private RequestSpecification requestSpec;
    private ResponseSpecification successSpec;
    private ResponseSpecification createdSpec;
    private ResponseSpecification notFoundSpec;

    private static int createdUserId;

    @BeforeEach
    void setup() {
        requestSpec = new RequestSpecBuilder()
            .setBaseUri("http://localhost")
            .setPort(port)
            .setBasePath("/api")
            .setContentType(ContentType.JSON)
            .setAccept(ContentType.JSON)
            .build();

        successSpec = new ResponseSpecBuilder()
            .expectStatusCode(200)
            .expectContentType(ContentType.JSON)
            .expectBody("code", equalTo(0))
            .expectBody("message", equalTo("success"))
            .expectResponseTime(lessThan(5000L))
            .build();

        createdSpec = new ResponseSpecBuilder()
            .expectStatusCode(201)
            .expectContentType(ContentType.JSON)
            .expectBody("code", equalTo(0))
            .build();

        notFoundSpec = new ResponseSpecBuilder()
            .expectStatusCode(404)
            .expectContentType(ContentType.JSON)
            .expectBody("code", equalTo(40401))
            .build();
    }

    @Nested
    @DisplayName("GET /api/users - \u83b7\u53d6\u7528\u6237\u5217\u8868")
    class GetUserList {

        @Test
        @DisplayName("\u5e94\u8fd4\u56de\u5206\u9875\u7528\u6237\u5217\u8868")
        void testGetUsers_ShouldReturnPaginatedList() {
            given()
                .spec(requestSpec)
                .queryParam("page", 1)
                .queryParam("size", 10)
            .when()
                .get("/users")
            .then()
                .spec(successSpec)
                .body("data.total", greaterThanOrEqualTo(0))
                .body("data.records", notNullValue());
        }

        @Test
        @DisplayName("\u9ed8\u8ba4\u53c2\u6570\u5e94\u8fd4\u56de\u7b2c\u4e00\u9875")
        void testGetUsers_WithDefaultParams() {
            given()
                .spec(requestSpec)
            .when()
                .get("/users")
            .then()
                .spec(successSpec)
                .body("data.page", equalTo(1))
                .body("data.size", equalTo(20));
        }

        @Test
        @DisplayName("\u8d1f\u6570\u5206\u9875\u53c2\u6570\u5e94\u8fd4\u56de 400")
        void testGetUsers_WithInvalidPagination() {
            given()
                .spec(requestSpec)
                .queryParam("page", -1)
            .when()
                .get("/users")
            .then()
                .statusCode(400);
        }
    }

    @Nested
    @DisplayName("GET /api/users/{id} - \u83b7\u53d6\u5355\u4e2a\u7528\u6237")
    class GetUserById {

        @Test
        @DisplayName("\u5e94\u8fd4\u56de\u6307\u5b9a\u7528\u6237")
        void testGetUserById_ShouldReturnUser() {
            given()
                .spec(requestSpec)
                .pathParam("id", 1)
            .when()
                .get("/users/{id}")
            .then()
                .spec(successSpec)
                .body("data.id", equalTo(1))
                .body("data.name", not(emptyString()))
                .body("data.email", containsString("@"));
        }

        @Test
        @DisplayName("\u4e0d\u5b58\u5728\u7684\u7528\u6237\u5e94\u8fd4\u56de 404")
        void testGetUserById_NotFound() {
            given()
                .spec(requestSpec)
                .pathParam("id", 99999)
            .when()
                .get("/users/{id}")
            .then()
                .spec(notFoundSpec)
                .body("message", containsString("not found"));
        }
    }

    // ... \u66f4\u591a\u6d4b\u8bd5\u65b9\u6cd5\u8bf7\u53c2\u89c1\u4e0a\u6587\u5404\u8282
}
```


### 14.3 测试执行结果示例

```
User CRUD API 集成测试
├── GET /api/users - 获取用户列表
│   ├── [OK] 应返回分页用户列表
│   ├── [OK] 默认参数应返回第一页
│   └── [OK] 负数分页参数应返回 400
├── GET /api/users/{id} - 获取单个用户
│   ├── [OK] 应返回指定用户
│   └── [OK] 不存在的用户应返回 404
├── POST /api/users - 创建用户
│   ├── [OK] 应成功创建用户
│   ├── [OK] 缺少必填字段应返回 400
│   └── [OK] 重复邮箱应返回 409
├── PUT /api/users/{id} - 更新用户
│   └── [OK] 应成功全量更新用户
├── PATCH /api/users/{id} - 部分更新
│   └── [OK] 应成功部分更新用户邮箱
├── DELETE /api/users/{id} - 删除用户
│   ├── [OK] 应成功删除用户
│   └── [OK] 重复删除应返回 404
└── Schema 验证
    └── [OK] 用户列表响应应匹配 JSON Schema
总计：15 测试通过，0 测试失败
```

---

## 15. REST Assured vs MockMvc vs TestRestTemplate

### 15.1 对比总表

| 特性 | REST Assured | Spring MockMvc | TestRestTemplate |
|------|-------------|----------------|-------------------|
| **定位** | 通用 REST API 测试框架 | Spring MVC 控制器测试 | Spring Boot HTTP 客户端测试 |
| **是否启动服务器** | 是（集成测试） | 否（模拟 Servlet 容器） | 是（集成测试） |
| **协议层** | 真实 HTTP | 模拟 HTTP | 真实 HTTP |
| **DSL 风格** | Given-When-Then | 流畅 API | 方法调用 |
| **代码可读性** | 极高 | 高 | 中 |
| **JSON/XML 解析** | 内建 JsonPath/XmlPath | 需配合 JsonPath | 需配合 JsonPath |
| **认证支持** | 全面（Basic/OAuth2/Form/Digest） | 有限（需手动模拟 SecurityContext） | 有限 |
| **Schema 验证** | 内建支持 | 需额外依赖 | 需额外依赖 |
| **文件上传测试** | 简单内建 | 中等（MockMultipartFile） | 复杂 |
| **响应时间断言** | 内建 .time() | 无 | 无 |
| **日志调试** | 详细过滤器控制 | 有限 | 有限 |
| **学习曲线** | 中 | 低 | 低 |
| **与 Spring 集成度** | 高 | 最高 | 高 |
| **端到端验证能力** | 强（真实网络栈） | 弱 | 中 |

### 15.2 场景选型建议

| 测试场景 | 推荐工具 | 理由 |
|----------|----------|------|
| **纯 Controller 层逻辑验证** | MockMvc | 最快、最轻量、不启动完整容器 |
| **完整 REST API 集成测试** | REST Assured | Given-When-Then 语义、JSON Schema、丰富的验证能力 |
| **测试 RestTemplate 调用** | TestRestTemplate | 与 RestTemplate 天然集成 |
| **涉及 Spring Security 的测试** | REST Assured | OAuth2 等认证支持更完善 |
| **微服务间 API 契约测试** | REST Assured | 真实 HTTP 能暴露网络层问题 |
| **快速验证 API 功能** | TestRestTemplate | 最简单的 API，适合快速脚本 |
| **全链路端到端测试** | REST Assured | 真实 HTTP、负载均衡、网络超时等都能覆盖 |
| **CI/CD 管道中的 API 测试** | REST Assured | CLI/IDE/CI 三方运行一致 |

### 15.3 三种方式代码对比

```java
// ========== REST Assured ==========
@Test
void testWithRestAssured() {
    given()
        .contentType(ContentType.JSON)
        .body(new CreateUserRequest("Alice", "alice@test.com", "pass", "admin"))
    .when()
        .post("/api/users")
    .then()
        .statusCode(201)
        .body("code", equalTo(0))
        .body("data.id", notNullValue())
        .time(lessThan(3000L));
}

// ========== MockMvc ==========
@Autowired
private MockMvc mockMvc;

@Test
void testWithMockMvc() throws Exception {
    String requestBody = objectMapper.writeValueAsString(
        new CreateUserRequest("Alice", "alice@test.com", "pass", "admin"));

    mockMvc.perform(post("/api/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.id").isNumber());
    // 注意：MockMvc 无法验证响应时间、真实 HTTP 行为
}

// ========== TestRestTemplate ==========
@Autowired
private TestRestTemplate restTemplate;

@Test
void testWithTestRestTemplate() {
    CreateUserRequest request = new CreateUserRequest(
        "Alice", "alice@test.com", "pass", "admin");

    ResponseEntity<ApiResponse> response = restTemplate.postForEntity(
        "/api/users", request, ApiResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody().getCode()).isEqualTo(0);
    assertThat(response.getBody().getData().getId()).isNotNull();
}
```

### 15.4 REST Assured 核心优势总结

1. **Given-When-Then 语义 -- 测试即文档**
   - 非技术人员也能理解测试意图
   - 代码组织结构与 BDD 流程一致

2. **丰富的验证能力**
   - 响应体：JsonPath + Hamcrest 匹配器
   - 响应结构：JSON Schema 验证
   - 响应时间：精确到毫秒级的断言
   - 响应头/Cookie/状态码全覆盖

3. **真实 HTTP 请求**
   - 能发现网络层、序列化、连接池等问题
   - 与生产环境行为一致
   - 支持完整的 HTTP 协议特性

4. **完善的认证支持**
   - OAuth2 / Basic / Digest / Form Auth 一应俱全
   - 无需手动组装 Authorization Header

5. **强大的提取能力**
   - 提取响应数据供后续请求使用
   - 支持复杂 JSON Path 表达式
   - 可完整提取为 Java 对象

6. **Spring Boot 无缝集成**
   - @SpringBootTest + 随机端口
   - 完整的依赖注入支持
   - 与 @Sql、@AutoConfigureTestDatabase 等完美配合

> **项目选型建议**：对于 Java 后端项目的 API 集成测试层，**强烈推荐 REST Assured**。它在代码可读性、验证丰富度、认证支持和 Spring Boot 集成方面都表现卓越。MockMvc 更适合 Controller 层的单元测试，TestRestTemplate 则适合简单的 HTTP 调用场景。三者在项目中可以共存，各司其职。

---

> 本文档系统梳理了 REST Assured 在 Java 后端 API 集成测试中的完整知识体系，从基础语法到高级特性、从单接口测试到完整的 CRUD 测试套件，再到与 Spring Boot 的深度集成。掌握这些内容，可以有效构建高质量、高可靠性的 API 测试体系。
