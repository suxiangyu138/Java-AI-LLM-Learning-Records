# Spring MVC 与 RESTful API 开发

> **"The web never sleeps, and neither should your API."**  
> 版本: Spring Framework 6.x / Spring Boot 3.x / JDK 17+  
> 适用: 企业级 REST API 开发

---

## Table of Contents

1. [Spring MVC 架构与核心概念](#1-spring-mvc-架构与核心概念)
2. [DispatcherServlet 请求处理流程](#2-dispatcherservlet-请求处理流程)
3. [Controller 配置详解](#3-controller-配置详解)
4. [请求参数绑定](#4-请求参数绑定)
5. [响应处理与内容协商](#5-响应处理与内容协商)
6. [RESTful API 设计规范](#6-restful-api-设计规范)
7. [Bean Validation 参数校验](#7-bean-validation-参数校验)
8. [统一响应与全局异常处理](#8-统一响应与全局异常处理)
9. [拦截器 (Interceptor)](#9-拦截器-interceptor)
10. [跨域 (CORS)](#10-跨域-cors)
11. [文件上传与下载](#11-文件上传与下载)
12. [异步请求处理](#12-异步请求处理)
13. [REST 客户端](#13-rest-客户端)
14. [安全最佳实践](#14-安全最佳实践)
15. [测试 Spring MVC](#15-测试-spring-mvc)
16. [面试题精选](#16-面试题精选)

---

## 1. Spring MVC 架构与核心概念

### 1.1 MVC 设计模式

```
┌─────────────────────────────────────────────────────────────────────┐
│                       MVC 设计模式                                   │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  用户请求                                                           │
│     │                                                              │
│     ▼                                                              │
│  ┌──────────┐     ┌──────────┐     ┌──────────┐                   │
│  │ Controller │────▶  Model   │────▶   View    │                   │
│  │ (处理器)   │     │ (数据)   │     │ (展现)    │                   │
│  │  Servlet   │     │  POJO   │     │  JSP/HTML│                   │
│  └──────────┘     └──────────┘     └──────────┘                   │
│       │                                  │                        │
│       │          ┌──────────┐            │                        │
│       └──────────│ 客户端    │◀───────────┘                        │
│                  │ 浏览器    │   响应                               │
│                  └──────────┘                                     │
│                                                                     │
│  传统 Web 应用: Controller 返回 ModelAndView → ViewResolver → JSP  │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### 1.2 Spring MVC 架构 (前后端分离)

```
┌─────────────────────────────────────────────────────────────────────┐
│               Spring MVC RESTful 架构 (前后端分离)                   │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌──────────┐     ┌──────────────────────────────────────────────┐ │
│  │  Client   │     │           Spring MVC 前端控制器              │ │
│  │  Browser  │     │                                              │ │
│  │  Postman  │     │  ┌────────────────┐                         │ │
│  │  Mobile   │─────│─▶│DispatcherServlet│                         │ │
│  │  Vue/React│     │  └───────┬────────┘                         │ │
│  └──────────┘     │          │                                    │ │
│                   │          ▼                                    │ │
│       JSON        │  ┌───────────────┐                            │ │
│◀──────────────────│──│  HttpMessage  │                            │ │
│                   │  │  Converter    │                            │ │
│                   │  └───────┬───────┘                            │ │
│                   │          │                                    │ │
│                   │          ▼                                    │ │
│                   │  ┌────────────────┐                           │ │
│                   │  │   Controller   │ → Service → Repository    │ │
│                   │  │  @RestController│                          │ │
│                   │  └────────────────┘                           │ │
│                   └──────────────────────────────────────────────┘ │
│                                                                     │
│  前后端分离: 不返回视图，只返回 JSON/XML 数据                         │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### 1.3 Spring MVC 核心组件

| 组件 | 说明 | 接口/类 |
|------|------|---------|
| **DispatcherServlet** | 前端控制器，所有请求的入口 | `org.springframework.web.servlet.DispatcherServlet` |
| **HandlerMapping** | 将请求映射到处理器 | `RequestMappingHandlerMapping` (注解驱动) |
| **HandlerAdapter** | 调用处理器的适配器 | `RequestMappingHandlerAdapter` |
| **HandlerInterceptor** | 拦截器，在处理器前后执行 | `HandlerInterceptor` |
| **Controller** | 实际的请求处理器 | `@Controller`, `@RestController` |
| **HttpMessageConverter** | 请求/响应消息转换 (JSON/XML) | `MappingJackson2HttpMessageConverter` |
| **ViewResolver** | 视图解析器 (传统 MVC) | `InternalResourceViewResolver` |
| **ExceptionResolver** | 异常解析器 | `HandlerExceptionResolver` |

---

## 2. DispatcherServlet 请求处理流程

### 2.1 完整流程图

```
┌─────────────────────────────────────────────────────────────────────────┐
│              DispatcherServlet 请求处理流程 (Step-by-Step)              │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  [1] HTTP Request                                                       │
│       │                                                                │
│       ▼                                                                │
│  [2] DispatcherServlet (前端控制器)                                     │
│       │  doService() → doDispatch()                                    │
│       │                                                                │
│       ▼                                                                │
│  [3] HandlerMapping (处理器映射)                                        │
│       │  查找匹配的 HandlerExecutionChain                               │
│       │  (Controller Method + Interceptors)                            │
│       │  RequestMappingHandlerMapping 根据 @RequestMapping 匹配        │
│       │                                                                │
│       ▼                                                                │
│  [4] HandlerExecutionChain                                              │
│       │  ● 匹配的 Controller Method                                     │
│       │  ● 匹配的 HandlerInterceptor 列表                              │
│       │                                                                │
│       ▼                                                                │
│  [5] HandlerAdapter (处理器适配器)                                      │
│       │  调用拦截器 preHandle() 方法                                    │
│       │  RequestMappingHandlerAdapter 处理方法参数绑定                  │
│       │                                                                │
│       ▼                                                                │
│  [6] 参数解析 (ArgumentResolver)                                        │
│       │  解析 @PathVariable, @RequestParam, @RequestBody 等             │
│       │  转换请求参数为 Java 对象                                       │
│       │  调用 HttpMessageConverter 转换 @RequestBody                    │
│       │                                                                │
│       ▼                                                                │
│  [7] 调用 Controller 方法                                               │
│       │  执行 @Valid 参数校验                                           │
│       │  执行业务逻辑 → 返回结果                                        │
│       │                                                                │
│       ▼                                                                │
│  [8] 返回值处理 (ReturnValueHandler)                                    │
│       │  处理 @ResponseBody, ResponseEntity 等                        │
│       │  调用 HttpMessageConverter 序列化响应                          │
│       │                                                                │
│       ▼                                                                │
│  [9] 调用拦截器 postHandle() 方法                                       │
│       │                                                                │
│       ▼                                                                │
│  [10] 发送 HTTP Response                                                │
│       │  调用拦截器 afterCompletion() 方法                              │
│       │                                                                │
│  [11] 异常时: HandlerExceptionResolver                                  │
│       │  处理 Controller 抛出的异常                                     │
│       │  返回错误响应                                                   │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### 2.2 源码级流程分析

```java
// DispatcherServlet.doDispatch() 核心源码分析 (Spring 6.x 简化版)
protected void doDispatch(HttpServletRequest request, HttpServletResponse response) {
    HttpServletRequest processedRequest = request;
    HandlerExecutionChain mappedHandler = null;
    boolean multipartRequestParsed = false;

    try {
        ModelAndView mv = null;
        Exception dispatchException = null;

        try {
            // 1. 检查是否为文件上传请求
            processedRequest = checkMultipart(request);
            multipartRequestParsed = (processedRequest != request);

            // 2. 通过 HandlerMapping 获取执行链
            //    根据 URL、请求方法、参数等匹配 Controller 方法
            mappedHandler = getHandler(processedRequest);
            if (mappedHandler == null) {
                noHandlerFound(processedRequest, response);
                return;
            }

            // 3. 获取 HandlerAdapter (适配器模式)
            //    不同的处理器类型使用不同的适配器
            HandlerAdapter ha = getHandlerAdapter(mappedHandler.getHandler());

            // 4. 执行拦截器 preHandle
            if (!mappedHandler.applyPreHandle(processedRequest, response)) {
                return;
            }

            // 5. 核心：调用 Controller 方法
            //    ha.handle() → RequestMappingHandlerAdapter.invokeHandlerMethod()
            //    → 参数解析 → 方法调用 → 返回值处理
            mv = ha.handle(processedRequest, response, mappedHandler.getHandler());

            // 6. 如果 Controller 标注了 @ResponseBody，此时已经写入了响应
            //    mv 可能为 null

            // 7. 执行拦截器 postHandle
            mappedHandler.applyPostHandle(processedRequest, response, mv);

        } catch (Exception ex) {
            dispatchException = ex;
        } catch (Throwable err) {
            dispatchException = new NestedServletException("Handler dispatch failed", err);
        }

        // 8. 处理结果 (正常或异常)
        processDispatchResult(processedRequest, response, mappedHandler, mv, dispatchException);

    } catch (Exception ex) {
        // 9. 异常时触发拦截器 afterCompletion
        triggerAfterCompletion(processedRequest, response, mappedHandler, ex);
    }
}
```

### 2.3 HandlerMapping 匹配过程

```java
// ========== 请求匹配过程 ==========
// 请求: GET /api/v1/users/123?page=1
//
// 从所有 @RequestMapping 中匹配最优的处理器方法

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    
    // 精确匹配: /api/v1/users
    @GetMapping
    public Result<List<User>> list(@RequestParam int page) { ... }
    
    // 路径变量匹配: /api/v1/users/123
    @GetMapping("/{id}")
    public Result<User> getById(@PathVariable Long id) { ... }
    
    // 复杂匹配: /api/v1/users/search?keyword=xxx
    @GetMapping("/search")
    public Result<List<User>> search(@RequestParam String keyword) { ... }
}

// 匹配优先级（从高到低）:
// 1. 精确路径: /api/v1/users (GET)
// 2. 路径变量: /api/v1/users/{id}
// 3. 通配符: /api/v1/users/**
// 4. 最佳匹配原则: 越精确的路径优先级越高
```

---

## 3. Controller 配置详解

### 3.1 @Controller vs @RestController

```java
// ========== @Controller ==========
// 传统 MVC 控制器，返回视图名称
// 需要配合 @ResponseBody 才能返回 JSON

@Controller  // 返回 ModelAndView (视图渲染)
@RequestMapping("/page")
public class PageController {
    
    @GetMapping("/index")
    public String index(Model model) {
        model.addAttribute("message", "Hello Thymeleaf");
        return "index";  // 视图名称 → ViewResolver 解析
    }
    
    @GetMapping("/data")
    @ResponseBody  // 强制返回 JSON，不走视图解析
    public Map<String, Object> data() {
        return Map.of("key", "value");  // 直接 JSON 响应
    }
}

// ========== @RestController ==========
// = @Controller + @ResponseBody (组合注解)
// 所有方法默认返回 JSON，无需单独加 @ResponseBody
// 推荐用于 RESTful API

@RestController  // 所有方法返回 JSON
@RequestMapping("/api/v1/users")
public class UserController {
    
    @GetMapping
    public Result<List<User>> list() {
        // 自动 JSON 序列化
        return Result.success(userService.findAll());
    }
    
    @PostMapping
    public Result<User> create(@RequestBody @Valid UserCreateRequest request) {
        return Result.success(userService.create(request));
    }
}
```

### 3.2 核心请求映射注解

```java
// ========== @RequestMapping 完整参数 ==========
@RestController
@RequestMapping("/api/v1")
public class UserController {
    
    // 1. 基本 URL 映射
    @GetMapping("/users")
    public Result<List<User>> list() { ... }
    
    // 2. 路径变量
    @GetMapping("/users/{id}")
    public Result<User> getById(@PathVariable Long id) { ... }
    
    // 3. 多路径映射
    @GetMapping({"/users", "/members"})
    public Result<List<User>> listBoth() { ... }
    
    // 4. 限定请求参数
    @GetMapping(value = "/users", params = "role=admin")
    public Result<List<User>> listAdmins() { ... }
    
    // 5. 限定请求头
    @GetMapping(value = "/users", headers = "X-API-Version=2")
    public Result<List<User>> listV2() { ... }
    
    // 6. 限定 Content-Type (consumes)
    @PostMapping(value = "/users", consumes = "application/json")
    public Result<User> createJson(@RequestBody User user) { ... }
    
    // 7. 限定 Accept (produces)
    @GetMapping(value = "/users/{id}", produces = "application/json;charset=UTF-8")
    public Result<User> getByIdJson(@PathVariable Long id) { ... }
    
    // 8. HTTP 方法简写注解
    @GetMapping    // = @RequestMapping(method = GET)
    @PostMapping   // = @RequestMapping(method = POST)
    @PutMapping    // = @RequestMapping(method = PUT)
    @DeleteMapping // = @RequestMapping(method = DELETE)
    @PatchMapping  // = @RequestMapping(method = PATCH)
}

// ========== URL 模式匹配规则 ==========
// 精确匹配:   /users/profile          → 最高优先级
// 路径变量:   /users/{id}             → 次高
// 正则路径:   /users/{id:[0-9]+}      → 只匹配数字
// 通配符:     /users/**               → 匹配 /users, /users/123, /users/123/posts
// 后缀匹配:   /users/*                → 匹配 /users/123, 不匹配 /users/123/posts

// ========== Matrix 变量 (矩阵变量) ==========
// URL: /users/42;q=5;fields=name,email
@RestController
@RequestMapping("/users/{id}")
public class UserController {
    
    @GetMapping
    public User getById(
            @PathVariable Long id,
            @MatrixVariable(name = "q", required = false) Integer queryVersion,
            @MatrixVariable(name = "fields", required = false) List<String> fields) {
        // ...
    }
}
```

### 3.3 RESTful URL 设计规范

```java
// ========== RESTful URL 最佳实践 ==========
// 资源名称使用复数名词，不要使用动词

@RestController
@RequestMapping("/api/v1")
public class BestPracticeController {
    
    // GET /api/v1/users — 列表查询
    @GetMapping("/users")
    public Result<PageResult<User>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) { ... }
    
    // GET /api/v1/users/{id} — 单个查询
    @GetMapping("/users/{id}")
    public Result<User> getById(@PathVariable Long id) { ... }
    
    // POST /api/v1/users — 创建
    @PostMapping("/users")
    public Result<User> create(@RequestBody @Valid UserCreateRequest request) { ... }
    
    // PUT /api/v1/users/{id} — 全量更新
    @PutMapping("/users/{id}")
    public Result<User> update(@PathVariable Long id, 
                              @RequestBody @Valid UserUpdateRequest request) { ... }
    
    // PATCH /api/v1/users/{id} — 部分更新
    @PatchMapping("/users/{id}")
    public Result<User> patch(@PathVariable Long id,
                             @RequestBody Map<String, Object> fields) { ... }
    
    // DELETE /api/v1/users/{id} — 删除
    @DeleteMapping("/users/{id}")
    public Result<Void> delete(@PathVariable Long id) { ... }
    
    // 子资源: GET /api/v1/users/{id}/orders
    @GetMapping("/users/{userId}/orders")
    public Result<List<Order>> getUserOrders(@PathVariable Long userId) { ... }
    
    // 带操作: POST /api/v1/users/{id}/activate (操作是动词)
    @PostMapping("/users/{id}/activate")
    public Result<Void> activate(@PathVariable Long id) { ... }
}
```

---

## 4. 请求参数绑定

### 4.1 参数绑定注解总览

```java
// ========== 参数绑定注解一览 ==========
@RestController
@RequestMapping("/api")
public class ParamBindingController {
    
    // 1. @PathVariable — URL 路径变量
    // URL: GET /api/users/123
    @GetMapping("/users/{id}")
    public User getById(@PathVariable Long id) { ... }
    
    // 带名称指定
    @GetMapping("/users/{userId}/orders/{orderId}")
    public Order getOrder(
            @PathVariable("userId") Long userId,
            @PathVariable("orderId") Long orderId) { ... }
    
    // 2. @RequestParam — 查询参数
    // URL: GET /api/users?page=1&size=20&keyword=john
    @GetMapping("/users")
    public Result<List<User>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword) { ... }
    
    // Map 接收所有查询参数
    @GetMapping("/params")
    public Map<String, String> allParams(@RequestParam Map<String, String> params) { ... }
    
    // 3. @RequestBody — 请求体绑定 (JSON/XML)
    // POST /api/users  Content-Type: application/json
    @PostMapping("/users")
    public User create(@RequestBody @Valid UserCreateRequest request) { ... }
    
    // 可以接收原始 Map
    @PostMapping("/raw")
    public Map<String, Object> rawBody(@RequestBody Map<String, Object> body) { ... }
    
    // 4. @RequestHeader — 请求头
    @GetMapping("/users/{id}")
    public User getById(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token,
            @RequestHeader(name = "X-Request-Id", required = false) String requestId,
            @RequestHeader("User-Agent") String userAgent) { ... }
    
    // 接收所有请求头
    @GetMapping("/headers")
    public Map<String, String> allHeaders(
            @RequestHeader Map<String, String> headers) { ... }
    
    // 5. @CookieValue — Cookie 值
    @GetMapping("/me")
    public User currentUser(
            @CookieValue(name = "SESSION_ID", required = false) String sessionId) { ... }
    
    // 6. @RequestAttribute — 请求属性 (由拦截器或过滤器设置)
    @GetMapping("/protected")
    public Result<?> protectedResource(
            @RequestAttribute("currentUser") User currentUser) { ... }
    
    // 7. @ModelAttribute — 模型属性绑定 (表单提交)
    // POST /api/users/form  Content-Type: application/x-www-form-urlencoded
    @PostMapping("/users/form")
    public User createFromForm(@ModelAttribute @Valid UserCreateRequest request) { ... }
    
    // 8. @RequestPart — 文件上传 + JSON 混合
    @PostMapping(value = "/users/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<?> importUsers(
            @RequestPart("file") MultipartFile file,
            @RequestPart("metadata") @Valid ImportMetadata metadata) { ... }
}
```

### 4.2 @RequestBody 与 HttpMessageConverter

```java
// ========== HttpMessageConverter 工作流程 ==========
// 请求: POST /api/users
// Content-Type: application/json
// Body: {"name":"John","email":"john@example.com","age":25}
//
// 1. DispatcherServlet 接收到请求
// 2. HandlerAdapter 找到匹配的方法
// 3. 发现参数有 @RequestBody 注解
// 4. RequestResponseBodyMethodProcessor 处理
// 5. 根据 Content-Type 找到匹配的 HttpMessageConverter
//    application/json → MappingJackson2HttpMessageConverter
// 6. 将 JSON 反序列化为 UserCreateRequest 对象
// 7. 执行 @Valid 校验
// 8. 将校验后的对象传入 Controller 方法

// ========== 默认注册的 HttpMessageConverter ==========
// 在 Spring Boot 中自动配置（WebMvcAutoConfiguration）:
// 
// 序号  | 转换器                            | 支持的媒体类型
// ------|-----------------------------------|------------------
// 1     | MappingJackson2HttpMessageConverter| application/json, application/*+json
// 2     | MappingJackson2XmlHttpMessageConverter | application/xml, text/xml
// 3     | StringHttpMessageConverter         | text/plain, */*
// 4     | ByteArrayHttpMessageConverter      | application/octet-stream
// 5     | ResourceHttpMessageConverter       | */*
// 6     | FormHttpMessageConverter           | application/x-www-form-urlencoded

// ========== 自定义 HttpMessageConverter ==========
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        // 完全替换默认转换器（不推荐，会丢失默认配置）
    }
    
    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        // 扩展默认转换器（推荐）
        // 添加自定义的 CSV 转换器
        converters.add(new CsvHttpMessageConverter<>());
    }
}

// 自定义 CSV MessageConverter 示例
public class CsvHttpMessageConverter<T> extends AbstractHttpMessageConverter<T> {
    
    public CsvHttpMessageConverter() {
        super(new MediaType("text", "csv"));
    }
    
    @Override
    protected boolean supports(Class<?> clazz) {
        // 支持所有类（简化示例）
        return true;
    }
    
    @Override
    protected T readInternal(Class<? extends T> clazz, HttpInputMessage inputMessage) 
            throws IOException, HttpMessageNotReadableException {
        // CSV → Java 对象
        String body = new String(inputMessage.getBody().readAllBytes());
        // 解析 CSV 并转换为对象...
        return null;
    }
    
    @Override
    protected void writeInternal(T t, HttpOutputMessage outputMessage) 
            throws IOException, HttpMessageNotWritableException {
        // Java 对象 → CSV
        OutputStream out = outputMessage.getBody();
        // 转换逻辑...
        out.write("name,email\n".getBytes());
    }
}
```

### 4.3 参数绑定高级技巧

```java
// ========== 1. 简单类型自动转换 ==========
// Spring 支持多种类型的自动转换：
// String → int, long, boolean, double, BigDecimal, LocalDate, LocalDateTime, UUID, ...

@GetMapping("/convert")
public void typeConversion(
        @RequestParam int intValue,             // "123" → 123
        @RequestParam long longValue,           // "1234567890" → 1234567890L
        @RequestParam boolean boolValue,        // "true" → true
        @RequestParam BigDecimal bigDecimal,    // "123.45" → BigDecimal
        @RequestParam LocalDate date,           // "2024-01-15" → LocalDate
        @RequestParam LocalDateTime dateTime,   // "2024-01-15T10:30:00" → LocalDateTime
        @RequestParam UUID uuid) { ... }        // "550e8400-e29b-41d4-a716-446655440000" → UUID

// ========== 2. 枚举类型自动绑定 ==========
public enum UserStatus {
    ACTIVE, INACTIVE, BANNED
}

@GetMapping("/users")
public Result<List<User>> listByStatus(
        @RequestParam UserStatus status) {  // "active" → UserStatus.ACTIVE
    // 默认按名称匹配，大小写不敏感
    // 或者使用 @JsonValue/@JsonCreator 自定义序列化
}

// ========== 3. @InitBinder 自定义绑定 ==========
@RestController
@RequestMapping("/users")
public class UserController {
    
    @InitBinder  // 在当前 Controller 中生效
    public void initBinder(WebDataBinder binder) {
        // 注册自定义属性编辑器
        binder.registerCustomEditor(LocalDate.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                setValue(LocalDate.parse(text, DateTimeFormatter.ofPattern("yyyy/MM/dd")));
            }
        });
        
        // 设置允许的字段（白名单，防止恶意绑定）
        binder.setAllowedFields("name", "email", "age");
        // binder.setDisallowedFields("role", "password");  // 黑名单方式
    }
    
    @PostMapping
    public User create(User user) {  // @ModelAttribute 绑定
        return user;
    }
}

// ========== 4. @JsonCreator — 自定义 JSON 反序列化 ==========
public enum OrderStatus {
    PENDING("pending"),
    PAID("paid"),
    SHIPPED("shipped"),
    COMPLETED("completed"),
    CANCELLED("cancelled");
    
    private final String code;
    
    OrderStatus(String code) { this.code = code; }
    
    @JsonCreator  // JSON 反序列化时调用此方法
    public static OrderStatus fromCode(String code) {
        for (OrderStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown status: " + code);
    }
    
    @JsonValue  // JSON 序列化时使用此值
    public String getCode() { return code; }
}

// 此时: {"status": "paid"} → OrderStatus.PAID
```

---

## 5. 响应处理与内容协商

### 5.1 @ResponseBody 与 ResponseEntity

```java
// ========== 1. @ResponseBody ==========
// 将返回值直接写入 HTTP 响应体
// 通过 HttpMessageConverter 序列化
// 自动设置 Content-Type

@RestController
public class ResponseController {
    
    @GetMapping("/string")
    public String plainText() {
        return "Hello World";  // text/plain;charset=UTF-8
    }
    
    @GetMapping("/object")
    public User user() {
        return new User("John", "john@example.com");  // application/json
    }
    
    @GetMapping("/map")
    public Map<String, Object> map() {
        return Map.of("success", true, "data", "some data");  // application/json
    }
}

// ========== 2. ResponseEntity<T> (完全控制 HTTP 响应) ==========
@RestController
@RequestMapping("/api/v1")
public class ResponseEntityController {
    
    // 自定义状态码
    @PostMapping("/users")
    public ResponseEntity<User> create(@RequestBody User user) {
        User created = userService.create(user);
        return ResponseEntity
                .status(HttpStatus.CREATED)  // 201 Created
                .header("Location", "/api/v1/users/" + created.getId())
                .body(created);
    }
    
    // 自定义响应头
    @GetMapping("/download")
    public ResponseEntity<Resource> download() {
        Resource file = new FileSystemResource("/path/to/file.pdf");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                    "attachment; filename=\"report.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(file.contentLength())
                .body(file);
    }
    
    // 条件响应
    @GetMapping("/users/{id}")
    public ResponseEntity<User> getById(@PathVariable Long id) {
        return userService.findById(id)
                .map(ResponseEntity::ok)                 // 200 OK
                .orElse(ResponseEntity.notFound().build());  // 404 Not Found
    }
    
    // 禁用缓存
    @GetMapping("/live-data")
    public ResponseEntity<Data> liveData() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .body(dataService.getLiveData());
    }
    
    // ETag 支持
    @GetMapping("/etag-data")
    public ResponseEntity<Data> etagData(@RequestHeader("If-None-Match") String ifNoneMatch) {
        Data data = dataService.getData();
        String etag = calculateEtag(data);
        
        if (etag.equals(ifNoneMatch)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();  // 304
        }
        
        return ResponseEntity.ok()
                .eTag(etag)
                .body(data);
    }
}

// ========== 3. ResponseStatusException ==========
// Spring 5+ 提供的便捷异常

@GetMapping("/users/{id}")
public User getById(@PathVariable Long id) {
    User user = userService.findById(id);
    if (user == null) {
        throw new ResponseStatusException(
            HttpStatus.NOT_FOUND, 
            "User not found with id: " + id
        );
    }
    return user;
}

// 带原因和异常
throw new ResponseStatusException(
    HttpStatus.BAD_REQUEST,
    "Invalid request",
    new IllegalArgumentException("id must be positive")
);
```

### 5.2 Content Negotiation (内容协商)

```java
// ========== 内容协商配置 ==========
// 内容协商允许客户端通过 Accept 头或 URL 后缀选择响应格式
// Spring Boot 默认配置支持 JSON 和 XML

@Configuration
public class ContentNegotiationConfig implements WebMvcConfigurer {
    
    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer
            // 1. 基于 Accept 头 (默认)
            .favorParameter(false)
            // 2. 基于 URL 参数 (?format=json)
            .favorParameter(true)
            .parameterName("format")
            // 3. 基于 URL 后缀 (.json, .xml)
            .favorPathExtension(true)
            // 忽略 accept 头
            .ignoreAcceptHeader(false)
            // 默认媒体类型
            .defaultContentType(MediaType.APPLICATION_JSON)
            // 注册媒体类型
            .mediaType("json", MediaType.APPLICATION_JSON)
            .mediaType("xml", MediaType.APPLICATION_XML);
    }
    
    // 需要额外依赖 Jackson XML 支持
    // <dependency>
    //     <groupId>com.fasterxml.jackson.dataformat</groupId>
    //     <artifactId>jackson-dataformat-xml</artifactId>
    // </dependency>
}

// ========== 使用示例 ==========
// 客户端可以选择返回格式:
// GET /api/users/123 → Accept: application/json (默认返回 JSON)
// GET /api/users/123.json → 返回 JSON
// GET /api/users/123.xml → 返回 XML (需要 Jackson XML)
// GET /api/users/123?format=xml → 返回 XML

// ========== 在 Controller 中控制 produces/consumes ==========
@RestController
@RequestMapping("/api/users")
public class ContentController {
    
    // 仅接收 JSON，返回 JSON
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, 
                 produces = MediaType.APPLICATION_JSON_VALUE)
    public User create(@RequestBody User user) { ... }
    
    // 同时支持 JSON 和 XML
    @GetMapping(value = "/{id}", 
                produces = {MediaType.APPLICATION_JSON_VALUE, 
                           MediaType.APPLICATION_XML_VALUE})
    public User getById(@PathVariable Long id) { ... }
}
```

### 5.3 Jackson 序列化定制

```java
// ========== 常用 Jackson 注解 ==========
public class User {
    
    @JsonIgnore  // 序列化/反序列化时忽略此字段
    private String password;
    
    @JsonProperty("user_name")  // 自定义 JSON 属性名
    private String userName;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime createTime;
    
    @JsonInclude(JsonInclude.Include.NON_NULL)  // null 不序列化
    private String optionalField;
    
    @JsonSerialize(using = CustomSerializer.class)  // 自定义序列化
    @JsonDeserialize(using = CustomDeserializer.class)  // 自定义反序列化
    private BigDecimal amount;
    
    @JsonView(View.Summary.class)  // 视图控制
    private Long id;
    
    @JsonView(View.Detail.class)
    private String email;
}

// ========== Jackson 全局配置 ==========
@Configuration
public class JacksonConfig {
    
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> {
            builder.serializationInclusion(JsonInclude.Include.NON_NULL)
                   .timeZone(TimeZone.getTimeZone("Asia/Shanghai"))
                   .dateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"))
                   .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                   .featuresToEnable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                   .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        };
    }
}

// ========== 日期序列化配置 ==========
// application.yml:
// spring:
//   jackson:
//     date-format: yyyy-MM-dd HH:mm:ss
//     time-zone: Asia/Shanghai
//     default-property-inclusion: non_null
//     serialization:
//       write-dates-as-timestamps: false
//       fail-on-empty-beans: false
//     deserialization:
//       fail-on-unknown-properties: false
```

---

## 6. RESTful API 设计规范

### 6.1 RESTful 设计原则

```
┌─────────────────────────────────────────────────────────────────────┐
│                  RESTful API 设计规范总结                            │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  1. 使用名词复数作为资源名称                                        │
│     ✅ /users, /orders, /products                                   │
│     ❌ /getUser, /userList, /findAll                                │
│                                                                     │
│  2. HTTP 方法对应 CRUD 操作                                         │
│     GET     → 查询资源                                              │
│     POST    → 创建资源                                              │
│     PUT     → 全量更新资源                                          │
│     PATCH   → 部分更新资源                                          │
│     DELETE  → 删除资源                                              │
│                                                                     │
│  3. 使用子资源表达资源关系                                          │
│     ✅ /users/{id}/orders                                           │
│     ❌ /ordersByUser?userId={id}                                    │
│                                                                     │
│  4. 分页、过滤、排序使用查询参数                                    │
│     ✅ /users?page=1&size=20&sort=name,asc&role=admin              │
│     ❌ /users/page/1/size/20                                       │
│                                                                     │
│  5. 版本控制使用前缀或 Accept 头                                    │
│     ✅ /api/v1/users                                                │
│     ✅ Accept: application/vnd.example.v2+json                     │
│     ❌ /users-v1, /v1.0/users                                       │
│                                                                     │
│  6. 统一响应格式                                                    │
│     ✅ {"code":200,"message":"success","data":{...}}               │
│     ❌ 每个接口返回格式不同                                          │
│                                                                     │
│  7. 使用标准 HTTP 状态码                                            │
│     200 OK, 201 Created, 204 No Content                            │
│     400 Bad Request, 401 Unauthorized, 403 Forbidden               │
│     404 Not Found, 409 Conflict, 422 Unprocessable Entity          │
│     500 Internal Server Error                                       │
│                                                                     │
│  8. 错误信息提供清晰的问题描述                                      │
│     ✅ {"code":1001,"message":"User not found","detail":"id=999"}  │
│     ❌ "Error occurred"                                             │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### 6.2 HTTP 状态码使用指南

```java
// ========== HTTP 状态码详细使用指南 ==========

// 2xx 成功
ResponseEntity.status(HttpStatus.OK).body(data);                      // 200 GET 成功
ResponseEntity.status(HttpStatus.CREATED).body(created);              // 201 POST 创建成功
ResponseEntity.status(HttpStatus.ACCEPTED).body(taskId);              // 202 异步任务已接受
ResponseEntity.status(HttpStatus.NO_CONTENT).build();                 // 204 DELETE 成功, 无内容

// 3xx 重定向
ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY)
    .location(URI.create("/api/v2/users")).build();                   // 301 资源已永久移动
ResponseEntity.status(HttpStatus.FOUND)
    .location(URI.create("/login")).build();                          // 302 临时重定向
ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();               // 304 缓存未修改

// 4xx 客户端错误
ResponseEntity.status(HttpStatus.BAD_REQUEST).build();                // 400 请求参数错误
ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();               // 401 未认证
ResponseEntity.status(HttpStatus.FORBIDDEN).build();                  // 403 无权限
ResponseEntity.status(HttpStatus.NOT_FOUND).build();                  // 404 资源不存在
ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();         // 405 请求方法不支持
ResponseEntity.status(HttpStatus.CONFLICT).build();                   // 409 资源冲突
ResponseEntity.status(HttpStatus.GONE).build();                       // 410 资源已永久删除
ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(errors);  // 422 校验失败
ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();          // 429 请求频率限制

// 5xx 服务端错误
ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();      // 500 服务器内部错误
ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();        // 503 服务不可用
ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).build();            // 504 网关超时
```

### 6.3 API 版本控制策略

```java
// ========== 三种主流的版本控制策略 ==========

// 1. URL 路径版本（最常用）
@RestController
@RequestMapping("/api/v1/users")  // /api/v1/users
public class UserControllerV1 { ... }

@RestController
@RequestMapping("/api/v2/users")  // /api/v2/users
public class UserControllerV2 { ... }

// 2. 请求头版本（隐藏版本信息）
@RestController
@RequestMapping("/api/users")
public class VersionedController {
    
    @GetMapping
    @ApiVersion("1")  // Accept: application/vnd.example.v1+json
    public Result<List<UserV1>> listV1() { ... }
    
    @GetMapping
    @ApiVersion("2")  // Accept: application/vnd.example.v2+json
    public Result<List<UserV2>> listV2() { ... }
}

// 3. 参数版本（简单直接）
@GetMapping("/api/users?version=2")

// 实践中推荐 URL 路径版本，清晰明了
// 版本号使用整数 (v1, v2, v3...) 不要使用语义化版本 (v1.0.1)
```

---

## 7. Bean Validation 参数校验

### 7.1 JSR-380 (Bean Validation 2.0) 内置约束

```java
// ========== JSR-380 内置校验注解 ==========
public class UserCreateRequest {
    
    // ─── Null 检查 ───
    @Null        // 必须为 null (更新操作可禁止传 ID)
    private Long id;
    
    @NotNull     // 不能为 null (但可以是 "")
    private String name;
    
    @NotEmpty    // 不能为 null, 且长度 > 0 (适用于 String, Collection, Map, Array)
    private String title;
    
    @NotBlank    // 不能为 null, trim() 后长度 > 0 (仅适用于 String)
    private String username;
    
    // ─── 数值检查 ───
    @Min(0)
    @Max(150)
    private int age;
    
    @Positive            // > 0
    @PositiveOrZero      // >= 0
    private BigDecimal price;
    
    @Negative            // < 0
    @NegativeOrZero      // <= 0
    private BigDecimal discount;
    
    @DecimalMin("0.01")   // 字符串形式比较 (适用于 BigDecimal)
    @DecimalMax("999999.99")
    private BigDecimal amount;
    
    // ─── 字符串检查 ───
    @Size(min = 2, max = 50)          // 字符串长度 / 集合大小
    private String displayName;
    
    @Email(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")
    private String email;
    
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
    
    // ─── 其他 ───
    @Past              // 必须是过去的时间
    @PastOrPresent     // 过去或现在
    private LocalDate birthday;
    
    @Future            // 必须是未来的时间
    @FutureOrPresent   // 未来或现在
    private LocalDateTime expireTime;
    
    @AssertTrue        // 必须为 true (业务约束)
    private boolean agreedToTerms;
    
    @AssertFalse       // 必须为 false
    private boolean disabled;
}

// ========== 请求体校验 ==========
@RestController
@RequestMapping("/api/v1/users")
@Validated  // 开启方法级别校验
public class UserController {
    
    // 对请求体进行校验
    @PostMapping
    public Result<User> create(
            @Valid @RequestBody UserCreateRequest request) {  // @Valid 触发校验
        // 如果校验失败，抛出 MethodArgumentNotValidException
        return Result.success(userService.create(request));
    }
    
    // 对查询参数进行校验（需要 @Validated 在类级别）
    @GetMapping("/search")
    public Result<List<User>> search(
            @RequestParam @NotBlank String keyword,
            @RequestParam @Min(1) @Max(100) int limit) { ... }
    
    // 对路径变量进行校验
    @GetMapping("/{id}")
    public Result<User> getById(
            @PathVariable @Positive Long id) { ... }
}
```

### 7.2 @Valid vs @Validated

```java
// ========== @Valid (JSR-380) vs @Validated (Spring) ==========
public class UserCreateRequest {
    
    @NotNull
    @Valid  // @Valid 级联校验 — 会校验 Address 中的约束
    private Address address;
    
    @Valid
    private List<@Email String> emails;  // 容器元素校验
    
    @NotEmpty
    @Valid
    private List<OrderItem> items;  // 列表中每个元素都校验
    
    // Inner class
    public static class Address {
        @NotBlank
        private String province;
        
        @NotBlank
        private String city;
        
        @NotBlank
        private String detail;
    }
}

// ========== @Validated 的分组校验功能 ==========
// @Validated 是 Spring 的扩展，支持分组校验
// @Valid 不支持分组

// 定义分组接口
public interface CreateGroup {}
public interface UpdateGroup {}

public class UserRequest {
    
    @Null(groups = CreateGroup.class)  // 创建时不能有 ID
    @NotNull(groups = UpdateGroup.class)  // 更新时必须有 ID
    private Long id;
    
    @NotBlank(groups = {CreateGroup.class, UpdateGroup.class})
    private String name;
    
    @Email(groups = CreateGroup.class)  // 创建时必须验证
    private String email;
    
    @NotBlank(groups = UpdateGroup.class)  // 更新时必须验证
    private String phone;
}

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    
    @PostMapping
    public Result<User> create(
            @Validated(CreateGroup.class) @RequestBody UserRequest request) {
        // 只校验 CreateGroup 组
        return Result.success(userService.create(request));
    }
    
    @PutMapping("/{id}")
    public Result<User> update(
            @Validated(UpdateGroup.class) @RequestBody UserRequest request) {
        // 只校验 UpdateGroup 组
        return Result.success(userService.update(request));
    }
}
```

### 7.3 自定义校验注解

```java
// ========== 自定义校验注解 ==========

// 1. 创建注解
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PhoneValidator.class)  // 关联验证器
@Documented
public @interface ValidPhone {
    
    String message() default "手机号格式不正确";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
    
    // 允许指定多个
    String country() default "CN";
}

// 2. 实现验证器
public class PhoneValidator implements ConstraintValidator<ValidPhone, String> {
    
    private static final Map<String, String> PHONE_PATTERNS = Map.of(
        "CN", "^1[3-9]\\d{9}$",
        "US", "^\\+?1[2-9]\\d{9}$",
        "JP", "^\\+?81[7-9]0\\d{8}$"
    );
    
    private String country;
    
    @Override
    public void initialize(ValidPhone annotation) {
        this.country = annotation.country();
    }
    
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;  // @NotNull 处理空值
        }
        
        String pattern = PHONE_PATTERNS.get(country);
        if (pattern == null) {
            return false;
        }
        
        return value.matches(pattern);
    }
}

// 3. 使用自定义注解
public class UserCreateRequest {
    
    @ValidPhone(country = "CN", message = "请输入正确的中国大陆手机号")
    private String phone;
}

// ========== 字段间关联校验 (类级别) ==========
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PasswordMatchValidator.class)
public @interface PasswordMatch {
    String message() default "两次输入的密码不一致";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

public class PasswordMatchValidator 
        implements ConstraintValidator<PasswordMatch, PasswordChangeRequest> {
    
    @Override
    public boolean isValid(PasswordChangeRequest request, 
                          ConstraintValidatorContext context) {
        if (request.getNewPassword() == null || request.getConfirmPassword() == null) {
            return true;
        }
        return request.getNewPassword().equals(request.getConfirmPassword());
    }
}

@PasswordMatch
public class PasswordChangeRequest {
    @NotBlank
    @Size(min = 6, max = 20)
    private String newPassword;
    
    @NotBlank
    private String confirmPassword;
}
```

### 7.4 编程式校验

```java
// ========== 编程式校验（非 Controller 层） ==========
// 在 Service 层手动触发校验

@Service
public class UserService {
    
    @Autowired
    private Validator validator;  // JSR-380 Validator
    
    public User create(UserCreateRequest request) {
        // 手动校验
        Set<ConstraintViolation<UserCreateRequest>> violations = 
            validator.validate(request);
        
        if (!violations.isEmpty()) {
            // 收集所有错误信息
            String message = violations.stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining(", "));
            
            throw new ValidationException("校验失败: " + message);
        }
        
        return save(request);
    }
    
    // 指定分组
    public void validateWithGroup(UserRequest request, Class<?> group) {
        Set<ConstraintViolation<UserRequest>> violations = 
            validator.validate(request, group);
        // ...
    }
}

// ========== 校验结果响应 ==========
// 默认错误响应格式:
// {
//   "timestamp": "2024-01-15T10:30:00",
//   "status": 400,
//   "error": "Bad Request",
//   "path": "/api/v1/users",
//   "errors": [
//     {
//       "field": "email",
//       "message": "必须是一个合法的邮件地址",
//       "rejectedValue": "invalid-email"
//     },
//     {
//       "field": "name",
//       "message": "不能为空",
//       "rejectedValue": null
//     }
//   ]
// }
```

---

## 8. 统一响应与全局异常处理

### 8.1 统一响应体 (Result Pattern)

```java
// ========== 统一响应体 ==========
// 企业级 API 的标准响应封装
// 确保所有接口返回一致的响应格式

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Result<T> {
    
    private int code;         // 业务状态码 (非 HTTP 状态码)
    private String message;   // 提示消息
    private T data;           // 响应数据
    private Long timestamp;   // 时间戳
    
    // 成功响应
    public static <T> Result<T> success() {
        return new Result<>(200, "success", null, System.currentTimeMillis());
    }
    
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "success", data, System.currentTimeMillis());
    }
    
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(200, message, data, System.currentTimeMillis());
    }
    
    // 失败响应
    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message, null, System.currentTimeMillis());
    }
    
    public static <T> Result<T> error(ErrorCode errorCode) {
        return new Result<>(errorCode.getCode(), errorCode.getMessage(), 
            null, System.currentTimeMillis());
    }
    
    // 带数据的分页响应
    public static <T> Result<PageResult<T>> page(Page<T> page) {
        PageResult<T> pageResult = new PageResult<>();
        pageResult.setList(page.getContent());
        pageResult.setTotal(page.getTotalElements());
        pageResult.setPage(page.getNumber() + 1);
        pageResult.setSize(page.getSize());
        return success(pageResult);
    }
}

// ========== 分页响应包装 ==========
@Data
public class PageResult<T> {
    private List<T> list;
    private long total;
    private int page;
    private int size;
    
    public int getTotalPages() {
        return size == 0 ? 0 : (int) Math.ceil((double) total / size);
    }
    
    public boolean hasMore() {
        return page * size < total;
    }
}

// ========== 业务错误码枚举 ==========
public interface ErrorCode {
    int getCode();
    String getMessage();
}

@Getter
@AllArgsConstructor
public enum GlobalErrorCode implements ErrorCode {
    
    // ─── 通用错误码 (1xxx) ───
    SUCCESS(200, "success"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或令牌已过期"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),
    TOO_MANY_REQUESTS(429, "请求频率过高"),
    INTERNAL_ERROR(500, "服务器内部错误"),
    SERVICE_UNAVAILABLE(503, "服务暂不可用"),
    
    // ─── 业务错误码 (2xxx) ───
    USER_NOT_FOUND(2001, "用户不存在"),
    USER_ALREADY_EXISTS(2002, "用户已存在"),
    USER_PASSWORD_ERROR(2003, "密码错误"),
    USER_LOCKED(2004, "账号已被锁定"),
    
    ORDER_NOT_FOUND(2101, "订单不存在"),
    ORDER_STATUS_INVALID(2102, "订单状态不正确"),
    ORDER_AMOUNT_INVALID(2103, "订单金额异常"),
    
    // ─── 校验错误码 (3xxx) ───
    VALIDATION_ERROR(3001, "参数校验失败"),
    DATA_CONFLICT(3002, "数据冲突"),
    FILE_TOO_LARGE(3003, "文件过大");
    
    private final int code;
    private final String message;
}
```

### 8.2 全局异常处理

```java
// ========== 全局异常处理 ==========
// 使用 @RestControllerAdvice 统一处理异常
// 确保所有异常都返回统一的 Result 格式

@RestControllerAdvice  // = @ControllerAdvice + @ResponseBody
public class GlobalExceptionHandler {
    
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    // ========== 1. 参数校验异常 ==========
    // @Valid 校验失败
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("参数校验失败: {}", message);
        return Result.error(GlobalErrorCode.VALIDATION_ERROR.getCode(), message);
    }
    
    // @Validated 参数校验失败 (查询参数, 路径变量)
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolation(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining(", "));
        return Result.error(GlobalErrorCode.VALIDATION_ERROR.getCode(), message);
    }
    
    // ========== 2. HTTP 消息转换异常 ==========
    // @RequestBody JSON 格式错误
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        return Result.error(GlobalErrorCode.BAD_REQUEST.getCode(), 
            "请求体格式错误，请检查 JSON 格式");
    }
    
    // ========== 3. 类型转换异常 ==========
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<Void> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        String message = String.format("参数 '%s' 格式不正确，期望类型: %s",
            e.getName(), e.getRequiredType().getSimpleName());
        return Result.error(GlobalErrorCode.BAD_REQUEST.getCode(), message);
    }
    
    // ========== 4. 缺少必要参数 ==========
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Void> handleMissingParam(MissingServletRequestParameterException e) {
        String message = String.format("缺少必要参数: %s (%s)", 
            e.getParameterName(), e.getParameterType());
        return Result.error(GlobalErrorCode.BAD_REQUEST.getCode(), message);
    }
    
    // ========== 5. HTTP 方法不支持 ==========
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return Result.error(GlobalErrorCode.METHOD_NOT_ALLOWED.getCode(),
            "不支持的请求方法: " + e.getMethod());
    }
    
    // ========== 6. 404 资源不存在 ==========
    @ExceptionHandler(NoHandlerFoundException.class)
    public Result<Void> handleNoHandler(NoHandlerFoundException e) {
        return Result.error(GlobalErrorCode.NOT_FOUND.getCode(),
            "接口不存在: " + e.getRequestURL());
    }
    
    // ========== 7. 业务异常 ==========
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusiness(BusinessException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }
    
    // ========== 8. 访问权限异常 ==========
    @ExceptionHandler(AccessDeniedException.class)
    public Result<Void> handleAccessDenied(AccessDeniedException e) {
        return Result.error(GlobalErrorCode.FORBIDDEN);
    }
    
    @ExceptionHandler(AuthenticationException.class)
    public Result<Void> handleAuth(AuthenticationException e) {
        return Result.error(GlobalErrorCode.UNAUTHORIZED);
    }
    
    // ========== 9. 文件上传异常 ==========
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<Void> handleMaxUpload(MaxUploadSizeExceededException e) {
        return Result.error(GlobalErrorCode.FILE_TOO_LARGE);
    }
    
    // ========== 10. 兜底异常 (最后一道防线) ==========
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e, HttpServletRequest request) {
        log.error("未预期的异常: url={}", request.getRequestURI(), e);
        return Result.error(GlobalErrorCode.INTERNAL_ERROR.getCode(), 
            "服务器繁忙，请稍后重试");
    }
}
```

### 8.3 业务异常设计

```java
// ========== 业务异常体系 ==========
// 自定义的业务异常，携带错误码和参数

@Getter
public class BusinessException extends RuntimeException {
    
    private final int code;
    private final Object[] args;
    
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.args = null;
    }
    
    public BusinessException(ErrorCode errorCode, Object... args) {
        super(String.format(errorCode.getMessage(), args));
        this.code = errorCode.getCode();
        this.args = args;
    }
    
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
        this.args = null;
    }
    
    // 更丰富的业务异常
    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.code = errorCode.getCode();
        this.args = null;
    }
}

// ========== 使用示例 ==========
@Service
public class UserService {
    
    public User getUserById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new BusinessException(
                GlobalErrorCode.USER_NOT_FOUND, id));  // code=2001, message="用户不存在: 123"
    }
    
    public User createUser(UserCreateRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(
                GlobalErrorCode.USER_ALREADY_EXISTS, request.getUsername());
        }
        return userRepository.save(request.toEntity());
    }
    
    @Transactional
    public void deleteUser(Long id) {
        User user = getUserById(id);
        if (user.hasActiveOrders()) {
            throw new BusinessException(
                GlobalErrorCode.ORDER_STATUS_INVALID,
                "用户存在未完成的订单，无法删除");
        }
        userRepository.delete(user);
    }
}

// ========== 访问者 IP 异常 ==========
// 在 Controller 中使用
@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    
    @PostMapping
    public Result<User> create(@Valid @RequestBody UserCreateRequest request) {
        return Result.success(userService.create(request));
    }
    
    @GetMapping("/{id}")
    public Result<User> getById(@PathVariable @Positive Long id) {
        return Result.success(userService.getUserById(id));
    }
}
```

---

## 9. 拦截器 (Interceptor)

### 9.1 HandlerInterceptor 接口

```java
// ========== HandlerInterceptor 三个方法 ==========
public interface HandlerInterceptor {
    
    // 1. preHandle — 处理器执行前
    //    返回 true: 继续执行
    //    返回 false: 中断请求（不会执行处理器和 postHandle）
    default boolean preHandle(HttpServletRequest request, 
                             HttpServletResponse response, 
                             Object handler) throws Exception {
        return true;
    }
    
    // 2. postHandle — 处理器执行后，视图渲染前
    //    可以修改 ModelAndView (传统 MVC)
    //    REST API 中通常不需要
    default void postHandle(HttpServletRequest request, 
                           HttpServletResponse response, 
                           Object handler, 
                           @Nullable ModelAndView modelAndView) throws Exception {
    }
    
    // 3. afterCompletion — 请求完成后 (视图渲染后)
    //    无论如何都会执行（包括异常）
    //    适合资源清理
    default void afterCompletion(HttpServletRequest request, 
                                HttpServletResponse response, 
                                Object handler, 
                                @Nullable Exception ex) throws Exception {
    }
}
```

### 9.2 常用拦截器实现

```java
// ========== 1. 日志拦截器 ==========
@Component
public class LogInterceptor implements HandlerInterceptor {
    
    private static final Logger log = LoggerFactory.getLogger(LogInterceptor.class);
    private static final String START_TIME = "requestStartTime";
    
    @Override
    public boolean preHandle(HttpServletRequest request, 
                            HttpServletResponse response, 
                            Object handler) {
        // 记录请求开始时间
        request.setAttribute(START_TIME, System.currentTimeMillis());
        
        // 记录请求信息
        log.info("=> [{}] {} | {} | Params: {}",
            request.getMethod(),
            request.getRequestURI(),
            request.getRemoteAddr(),
            getParamsString(request));
        
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, 
                               HttpServletResponse response, 
                               Object handler, 
                               Exception ex) {
        Long startTime = (Long) request.getAttribute(START_TIME);
        long duration = System.currentTimeMillis() - startTime;
        
        log.info("<= [{}] {} | Status: {} | Duration: {}ms",
            request.getMethod(),
            request.getRequestURI(),
            response.getStatus(),
            duration);
        
        // 慢请求告警
        if (duration > 3000) {
            log.warn("慢请求: [{}] {} | {}ms", 
                request.getMethod(), request.getRequestURI(), duration);
        }
    }
    
    private String getParamsString(HttpServletRequest request) {
        Map<String, String[]> paramMap = request.getParameterMap();
        if (paramMap.isEmpty()) return "{}";
        
        return paramMap.entrySet().stream()
                .map(e -> e.getKey() + "=" + Arrays.toString(e.getValue()))
                .collect(Collectors.joining("&"));
    }
}

// ========== 2. 认证拦截器 ==========
@Component
public class AuthenticationInterceptor implements HandlerInterceptor {
    
    @Autowired
    private TokenService tokenService;
    
    @Override
    public boolean preHandle(HttpServletRequest request, 
                            HttpServletResponse response, 
                            Object handler) throws Exception {
        // 检查是否是静态资源或公开接口
        if (isPublicEndpoint(request)) {
            return true;
        }
        
        // 从请求头获取 Token
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                JsonUtils.toJson(Result.error(GlobalErrorCode.UNAUTHORIZED)));
            return false;
        }
        
        try {
            // 解析 Token
            String jwt = token.substring(7);  // 去掉 "Bearer "
            Long userId = tokenService.parseToken(jwt);
            
            // 将用户信息放入请求上下文
            request.setAttribute("currentUserId", userId);
            return true;
        } catch (Exception e) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                JsonUtils.toJson(Result.error(GlobalErrorCode.UNAUTHORIZED)));
            return false;
        }
    }
    
    private boolean isPublicEndpoint(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        
        // 公开接口列表
        return path.contains("/api/v1/auth/login") ||
               path.contains("/api/v1/auth/register") ||
               path.contains("/api/v1/public") ||
               path.equals("/error") ||
               request.getMethod().equals("OPTIONS");  // CORS 预检请求
    }
}

// ========== 3. 频率限制拦截器 ==========
@Component
public class RateLimitInterceptor implements HandlerInterceptor {
    
    @Autowired
    private RedisTemplate<String, Integer> redisTemplate;
    
    @Override
    public boolean preHandle(HttpServletRequest request, 
                            HttpServletResponse response, 
                            Object handler) throws Exception {
        // 获取客户端 IP
        String clientIp = getClientIp(request);
        String key = "rate_limit:" + clientIp;
        
        // 滑动窗口计数
        Long count = redisTemplate.opsForValue().increment(key);
        if (count == 1) {
            redisTemplate.expire(key, Duration.ofSeconds(60));  // 60 秒过期
        }
        
        int limit = 100;  // 每分钟 100 次
        if (count > limit) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                JsonUtils.toJson(Result.error(GlobalErrorCode.TOO_MANY_REQUESTS)));
            return false;
        }
        
        return true;
    }
    
    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
```

### 9.3 注册拦截器

```java
// ========== 注册拦截器 ==========
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    
    @Autowired
    private LogInterceptor logInterceptor;
    
    @Autowired
    private AuthenticationInterceptor authInterceptor;
    
    @Autowired
    private RateLimitInterceptor rateLimitInterceptor;
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 1. 日志拦截器 — 记录所有请求
        registry.addInterceptor(logInterceptor)
                .addPathPatterns("/**")       // 拦截所有路径
                .order(1);                     // 执行顺序 (越小越先执行)
        
        // 2. 认证拦截器 — 拦截需要认证的 API
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/v1/**")                     // 拦截 API 请求
                .excludePathPatterns(
                    "/api/v1/auth/login",                          // 登录不拦截
                    "/api/v1/auth/register",                       // 注册不拦截
                    "/api/v1/public/**",                           // 公开接口
                    "/swagger-resources/**",                        // Swagger
                    "/v3/api-docs/**",
                    "/favicon.ico"
                )
                .order(2);
        
        // 3. 频率限制拦截器 — 对 API 限流
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/**")
                .order(3);
    }
}
```

### 9.4 Interceptor vs Filter vs AOP

```java
// ========== 三者对比 ==========
// 
// 特性          | Filter                    | Interceptor              | AOP
// --------------|---------------------------|--------------------------|-------------------------
// 规范          | Servlet 规范              | Spring 框架              | Spring AOP
// 作用范围      | 所有 Web 请求             | Spring MVC 请求          | Bean 方法
// 访问 Controller | 不能                    | 可以                     | 可以
// 访问 HttpServletRequest | 可以            | 可以                     | 不能 (可通过参数)
// 执行顺序      | 最先                      | Filter 之后              | Interceptor 之后
// 适用场景      | 字符编码, CORS, 安全      | 认证, 日志, 权限         | 事务, 缓存, 审计
// 
// 执行顺序链:
// Request → Filter → DispatcherServlet → Interceptor.preHandle 
//   → Controller → Interceptor.postHandle → ViewResolver 
//   → Interceptor.afterCompletion → Response

// ========== Filter 示例 (与 Interceptor 对比) ==========
@Component  // 或 @WebFilter
@Order(1)
public class CorsFilter implements Filter {
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, 
                        FilterChain chain) throws IOException, ServletException {
        HttpServletResponse res = (HttpServletResponse) response;
        
        // 设置 CORS 头 — Filter 适合做这种底层处理
        res.setHeader("Access-Control-Allow-Origin", "*");
        res.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        res.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type");
        
        chain.doFilter(request, response);
    }
}

// ========== 何时使用哪个？ ==========
// Filter:
//   字符编码 (CharacterEncodingFilter)
//   跨域 (CorsFilter)
//   安全检查 (Spring Security 的 DelegatingFilterProxy)
//   请求包装 (XSS 过滤)
//   ✅ 适用于 Servlet 层面的通用处理
// 
// Interceptor:
//   认证/授权检查
//   请求日志 (获取 Controller 方法信息)
//   频率限制
//   上下文设置 (设置 ThreadLocal)
//   ✅ 适用于 Controller 层面的处理
// 
// AOP:
//   事务管理 (@Transactional)
//   缓存 (@Cacheable)
//   审计日志
//   ✅ 适用于 Service 层面的横切关注点
```

---

## 10. 跨域 (CORS)

### 10.1 CORS 原理

```java
// ========== CORS (Cross-Origin Resource Sharing) ==========
// 浏览器安全策略：默认禁止跨域请求
// CORS 通过 HTTP 头控制跨域访问

// 简单请求 (GET/POST with simple headers):
//   → 浏览器直接发送请求
//   → 检查响应头的 Access-Control-Allow-Origin

// 预检请求 (PUT/DELETE/PATCH, custom headers, application/json):
//   → 浏览器先发送 OPTIONS 请求 (preflight)
//   → 服务器返回允许的策略
//   → 浏览器再发送实际请求

// 预检请求 OPTIONS:
// Origin: http://localhost:3000
// Access-Control-Request-Method: POST
// Access-Control-Request-Headers: Authorization, Content-Type

// 响应头:
// Access-Control-Allow-Origin: http://localhost:3000
// Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS
// Access-Control-Allow-Headers: Authorization, Content-Type
// Access-Control-Max-Age: 3600
// Access-Control-Allow-Credentials: true
```

### 10.2 @CrossOrigin 注解

```java
// ========== @CrossOrigin 注解 ==========
// 可以在类级别或方法级别配置 CORS

// 1. Controller 类级别 — 所有方法生效
@RestController
@RequestMapping("/api/v1/users")
@CrossOrigin(origins = "http://localhost:3000", maxAge = 3600)
public class UserController { ... }

// 2. 方法级别 — 仅该方法生效
@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    
    @GetMapping("/{id}")
    @CrossOrigin(
        origins = {"http://localhost:3000", "https://example.com"},
        methods = {RequestMethod.GET, RequestMethod.POST},
        allowedHeaders = "*",
        allowCredentials = "true",
        maxAge = 3600
    )
    public User getById(@PathVariable Long id) { ... }
}

// 3. 允许所有来源 (开发环境)
@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")  // 任何来源都可以访问
public class DevController { ... }
```

### 10.3 全局 CORS 配置

```java
// ========== 全局 CORS 配置 (推荐) ==========
// 可以配置多个允许的来源、自定义路径模式

@Configuration
public class CorsConfig implements WebMvcConfigurer {
    
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                // 允许的来源
                .allowedOrigins(
                    "http://localhost:3000",     // 本地开发
                    "https://admin.example.com",  // 管理后台
                    "https://www.example.com"     // 前端站点
                )
                // 或者允许所有来源 (不推荐生产)
                // .allowedOriginPatterns("*")
                
                // 允许的 HTTP 方法
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                
                // 允许的请求头
                .allowedHeaders("Authorization", "Content-Type", "X-Request-ID")
                
                // 暴露给客户端的响应头
                .exposedHeaders("X-Total-Count", "X-Request-ID")
                
                // 是否允许携带凭证 (Cookie, Authorization 头)
                .allowCredentials(true)
                
                // 预检请求缓存时间 (秒)
                .maxAge(3600);
        
        // 针对不同路径配置不同规则
        registry.addMapping("/public/**")
                .allowedOrigins("*")
                .allowedMethods("GET")
                .maxAge(3600);
    }
}
```

---

## 11. 文件上传与下载

### 11.1 文件上传配置

```yaml
# ========== application.yml 文件上传配置 ==========
spring:
  servlet:
    multipart:
      enabled: true               # 启用文件上传
      max-file-size: 10MB         # 单个文件大小
      max-request-size: 100MB     # 总请求大小 (多文件)
      file-size-threshold: 2KB    # 文件大小阈值 (超过后写入磁盘)
      location: /tmp/uploads      # 临时存储目录
      resolve-lazily: false       # 是否延迟解析
```

### 11.2 文件上传 Controller

```java
// ========== 文件上传 ==========
@RestController
@RequestMapping("/api/v1/files")
public class FileController {
    
    @Autowired
    private FileService fileService;
    
    // 1. 单文件上传
    @PostMapping("/upload")
    public Result<FileInfo> upload(
            @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error(GlobalErrorCode.BAD_REQUEST.getCode(), "文件不能为空");
        }
        FileInfo fileInfo = fileService.store(file);
        return Result.success(fileInfo);
    }
    
    // 2. 多文件上传
    @PostMapping("/uploads")
    public Result<List<FileInfo>> uploadMultiple(
            @RequestParam("files") List<MultipartFile> files) {
        List<FileInfo> result = files.stream()
                .filter(f -> !f.isEmpty())
                .map(fileService::store)
                .collect(Collectors.toList());
        return Result.success(result);
    }
    
    // 3. 文件 + JSON 混合上传
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<?> importWithMetadata(
            @RequestParam("file") MultipartFile file,
            @RequestPart("metadata") @Valid ImportMetadata metadata) {
        // 同时接收文件和 JSON 元数据
        FileInfo fileInfo = fileService.store(file);
        importService.processImport(fileInfo, metadata);
        return Result.success("导入任务已提交");
    }
    
    // 4. 文件下载
    @GetMapping("/download/{fileId}")
    public ResponseEntity<Resource> download(@PathVariable String fileId) throws IOException {
        FileInfo fileInfo = fileService.getFileInfo(fileId);
        Resource resource = fileService.loadAsResource(fileId);
        
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(fileInfo.getContentType()))
                .contentLength(fileInfo.getSize())
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                    "attachment; filename=\"" + URLEncoder.encode(
                        fileInfo.getOriginalName(), StandardCharsets.UTF_8) + "\"")
                .body(resource);
    }
    
    // 5. 在线预览 (图片/PDF)
    @GetMapping("/preview/{fileId}")
    public ResponseEntity<Resource> preview(@PathVariable String fileId) throws IOException {
        FileInfo fileInfo = fileService.getFileInfo(fileId);
        Resource resource = fileService.loadAsResource(fileId);
        
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(fileInfo.getContentType()))
                .contentLength(fileInfo.getSize())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")  // inline = 在线预览
                .body(resource);
    }
}
```

### 11.3 文件存储服务

```java
// ========== 文件存储服务 (本地存储示例) ==========
@Service
public class FileService {
    
    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;
    
    // 存储文件
    public FileInfo store(MultipartFile file) {
        // 1. 验证文件
        validateFile(file);
        
        // 2. 生成唯一文件名
        String originalName = file.getOriginalFilename();
        String extension = getExtension(originalName);
        String storedName = UUID.randomUUID().toString() + extension;
        
        // 3. 创建日期目录 (按日期分目录)
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String dir = Paths.get(uploadDir, datePath).toString();
        File directory = new File(dir);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        
        // 4. 保存文件
        Path filePath = Paths.get(dir, storedName);
        try {
            file.transferTo(filePath);
        } catch (IOException e) {
            throw new BusinessException(GlobalErrorCode.INTERNAL_ERROR, "文件存储失败");
        }
        
        // 5. 返回文件信息
        FileInfo fileInfo = new FileInfo();
        fileInfo.setOriginalName(originalName);
        fileInfo.setStoredName(storedName);
        fileInfo.setPath(datePath + "/" + storedName);
        fileInfo.setSize(file.getSize());
        fileInfo.setContentType(file.getContentType());
        fileInfo.setExtension(extension);
        
        // 6. 保存文件记录到数据库
        return fileRepository.save(fileInfo);
    }
    
    // 加载文件
    public Resource loadAsResource(String fileId) {
        FileInfo fileInfo = getFileInfo(fileId);
        Path filePath = Paths.get(uploadDir, fileInfo.getPath());
        
        Resource resource = new UrlResource(filePath.toUri());
        if (!resource.exists() || !resource.isReadable()) {
            throw new BusinessException(GlobalErrorCode.NOT_FOUND, "文件不存在或不可读");
        }
        return resource;
    }
    
    private void validateFile(MultipartFile file) {
        // 文件大小已在配置中限制
        
        // 文件类型白名单
        String contentType = file.getContentType();
        if (contentType != null && !ALLOWED_TYPES.contains(contentType)) {
            throw new BusinessException(GlobalErrorCode.BAD_REQUEST, 
                "不支持的文件类型: " + contentType);
        }
        
        // 文件名安全检查
        String name = file.getOriginalFilename();
        if (name != null && (name.contains("..") || name.contains("/"))) {
            throw new BusinessException(GlobalErrorCode.BAD_REQUEST, "非法的文件名");
        }
    }
    
    private static final Set<String> ALLOWED_TYPES = Set.of(
        "image/jpeg", "image/png", "image/gif", "image/webp",
        "application/pdf",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );
    
    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }
}
```

---

## 12. 异步请求处理

### 12.1 DeferredResult — 异步响应

```java
// ========== DeferredResult ==========
// 将请求处理延迟到另一个线程
// 适用于长轮询、推送场景

@RestController
@RequestMapping("/api/async")
public class AsyncController {
    
    @Autowired
    private TaskQueue taskQueue;
    
    // 异步处理 — Servlet 线程立即释放
    @GetMapping("/process/{taskId}")
    public DeferredResult<Result<String>> process(@PathVariable String taskId) {
        // 设置超时时间 (毫秒)
        DeferredResult<Result<String>> result = new DeferredResult<>(30000L);
        
        // 超时回调
        result.onTimeout(() -> {
            result.setErrorResult(Result.error(500, "处理超时"));
        });
        
        // 完成回调
        result.onCompletion(() -> {
            System.out.println("Task " + taskId + " completed");
        });
        
        // 提交到异步线程处理
        taskQueue.submit(taskId, result);
        
        return result;  // 返回 DeferredResult，不阻塞当前线程
    }
}

// 异步任务处理器
@Component
public class TaskQueue {
    
    private final ExecutorService executor = Executors.newFixedThreadPool(10);
    
    public void submit(String taskId, DeferredResult<Result<String>> result) {
        executor.submit(() -> {
            try {
                // 模拟耗时处理
                Thread.sleep(5000);
                String data = "Processed: " + taskId;
                result.setResult(Result.success(data));
            } catch (Exception e) {
                result.setErrorResult(Result.error(500, e.getMessage()));
            }
        });
    }
}
```

### 12.2 SSE (Server-Sent Events)

```java
// ========== SSE — 服务端推送 ==========
// 适用于实时通知、消息推送、进度更新

@RestController
@RequestMapping("/api/sse")
public class SseController {
    
    @Autowired
    private NotificationService notificationService;
    
    // 建立 SSE 连接
    @GetMapping(value = "/subscribe/{userId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@PathVariable Long userId) {
        // 超时时间 (毫秒) — Long.MAX_VALUE 表示永不过期
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        
        // 注册到通知服务
        notificationService.register(userId, emitter);
        
        // 超时回调
        emitter.onTimeout(() -> notificationService.unregister(userId, emitter));
        
        // 错误回调
        emitter.onError(e -> notificationService.unregister(userId, emitter));
        
        // 完成回调
        emitter.onCompletion(() -> notificationService.unregister(userId, emitter));
        
        return emitter;
    }
    
    // 发送通知到所有连接
    @PostMapping("/notify")
    public Result<Void> notify(@RequestBody Notification notification) {
        notificationService.broadcast(notification);
        return Result.success();
    }
}

// ========== 通知服务 ==========
@Component
public class NotificationService {
    
    private final Map<Long, List<SseEmitter>> userEmitters = new ConcurrentHashMap<>();
    
    public void register(Long userId, SseEmitter emitter) {
        userEmitters.computeIfAbsent(userId, k -> 
            Collections.synchronizedList(new ArrayList<>()))
            .add(emitter);
    }
    
    public void unregister(Long userId, SseEmitter emitter) {
        List<SseEmitter> emitters = userEmitters.get(userId);
        if (emitters != null) {
            emitters.remove(emitter);
        }
    }
    
    public void sendToUser(Long userId, Object data) {
        List<SseEmitter> emitters = userEmitters.get(userId);
        if (emitters == null) return;
        
        Iterator<SseEmitter> iterator = emitters.iterator();
        while (iterator.hasNext()) {
            SseEmitter emitter = iterator.next();
            try {
                emitter.send(SseEmitter.event()
                    .name("notification")
                    .data(data));
            } catch (IOException e) {
                emitter.completeWithError(e);
                iterator.remove();
            }
        }
    }
    
    public void broadcast(Object data) {
        userEmitters.forEach((userId, emitters) -> {
            sendToUser(userId, data);
        });
    }
}

// 前端 JavaScript 消费者:
// const eventSource = new EventSource('/api/sse/subscribe/123');
// eventSource.addEventListener('notification', (event) => {
//     const data = JSON.parse(event.data);
//     console.log('收到通知:', data);
// });
// eventSource.onerror = (err) => console.error('SSE error:', err);
```

---

## 13. REST 客户端

### 13.1 RestTemplate (Spring 5 及之前)

```java
// ========== RestTemplate 配置 ==========
// 传统同步 HTTP 客户端 (Spring Boot 3.x 中标记为 @Deprecated)
// 推荐替换为 WebClient 或 RestClient

@Configuration
public class RestTemplateConfig {
    
    @Bean
    @Deprecated  // Spring Boot 3.2+ 标记为 deprecated
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(10))
                .additionalMessageConverters(new MappingJackson2HttpMessageConverter())
                .defaultHeader("User-Agent", "MyApp/1.0")
                .build();
    }
}

// ========== 使用示例 ==========
@Service
public class UserApiClient {
    
    @Autowired
    private RestTemplate restTemplate;
    
    private static final String BASE_URL = "https://api.example.com";
    
    // GET 请求
    public List<User> getUsers() {
        ResponseEntity<List<User>> response = restTemplate.exchange(
            BASE_URL + "/users",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<User>>() {}
        );
        return response.getBody();
    }
    
    // GET 带参数
    public User getUserById(Long id) {
        return restTemplate.getForObject(
            BASE_URL + "/users/{id}",
            User.class,
            id  // 替换 {id}
        );
    }
    
    // POST 请求
    public User createUser(UserCreateRequest request) {
        ResponseEntity<User> response = restTemplate.postForEntity(
            BASE_URL + "/users",
            request,
            User.class
        );
        return response.getBody();
    }
    
    // 带 Header 的请求
    public User getUserWithAuth(Long id, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        HttpEntity<?> entity = new HttpEntity<>(headers);
        
        ResponseEntity<User> response = restTemplate.exchange(
            BASE_URL + "/users/{id}",
            HttpMethod.GET,
            entity,
            User.class,
            id
        );
        return response.getBody();
    }
}
```

### 13.2 WebClient (Spring 5+ 响应式)

```java
// ========== WebClient 配置 ==========
// 响应式 HTTP 客户端，支持同步和异步调用
// 推荐替代 RestTemplate

@Configuration
public class WebClientConfig {
    
    @Bean
    public WebClient webClient(WebClient.Builder builder) {
        return builder
                .baseUrl("https://api.example.com")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("User-Agent", "MyApp/1.0")
                .defaultCookie("session", "xxx")
                .codecs(config -> config
                    .defaultCodecs()
                    .maxInMemorySize(16 * 1024 * 1024))  // 16MB
                .build();
    }
}

// ========== WebClient 调用示例 ==========
@Service
public class WebClientUserService {
    
    @Autowired
    private WebClient webClient;
    
    // 同步阻塞 (类似 RestTemplate)
    public List<User> getUsersBlocking() {
        return webClient.get()
                .uri("/users")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<User>>() {})
                .block();  // 阻塞等待
    }
    
    // 异步 (响应式)
    public Mono<User> getUserById(Long id) {
        return webClient.get()
                .uri("/users/{id}", id)
                .retrieve()
                .bodyToMono(User.class);
    }
    
    // POST 异步
    public Mono<User> createUser(UserCreateRequest request) {
        return webClient.post()
                .uri("/users")
                .body(Mono.just(request), UserCreateRequest.class)
                .retrieve()
                .bodyToMono(User.class);
    }
    
    // 错误处理
    public Mono<User> getUserSafe(Long id) {
        return webClient.get()
                .uri("/users/{id}", id)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response ->
                    Mono.error(new BusinessException(
                        GlobalErrorCode.BAD_REQUEST, "Client error")))
                .onStatus(HttpStatus::is5xxServerError, response ->
                    Mono.error(new BusinessException(
                        GlobalErrorCode.SERVICE_UNAVAILABLE, "Server error")))
                .bodyToMono(User.class);
    }
    
    // 带认证
    public Mono<User> getUserWithAuth(Long id, String token) {
        return webClient.get()
                .uri("/users/{id}", id)
                .headers(headers -> headers.setBearerAuth(token))
                .retrieve()
                .bodyToMono(User.class);
    }
}
```

### 13.3 RestClient (Spring Boot 3.2+)

```java
// ========== RestClient ==========
// Spring 6.1 / Spring Boot 3.2 引入的新同步 HTTP 客户端
// 结合了 RestTemplate 的简单和 WebClient 的 fluent API

@Configuration
public class RestClientConfig {
    
    @Bean
    public RestClient restClient(RestClient.Builder builder) {
        return builder
                .baseUrl("https://api.example.com")
                .defaultHeader("User-Agent", "MyApp/1.0")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .requestInterceptor((request, body, execution) -> {
                    // 请求拦截器 — 日志、认证等
                    System.out.println("Request: " + request.getURI());
                    return execution.execute(request, body);
                })
                .build();
    }
}

// ========== RestClient 使用示例 ==========
@Service
public class RestClientUserService {
    
    @Autowired
    private RestClient restClient;
    
    // GET 请求
    public List<User> getUsers() {
        return restClient.get()
                .uri("/users")
                .retrieve()
                .body(new ParameterizedTypeReference<List<User>>() {});
    }
    
    // GET 带路径变量
    public User getUserById(Long id) {
        return restClient.get()
                .uri("/users/{id}", id)
                .retrieve()
                .body(User.class);
    }
    
    // POST 请求
    public User createUser(UserCreateRequest request) {
        return restClient.post()
                .uri("/users")
                .body(request)
                .retrieve()
                .body(User.class);
    }
    
    // 自定义 Headers
    public User getUserWithAuth(Long id, String token) {
        return restClient.get()
                .uri("/users/{id}", id)
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .body(User.class);
    }
    
    // 检查响应状态
    public Optional<User> findUser(Long id) {
        return restClient.get()
                .uri("/users/{id}", id)
                .exchange((request, response) -> {
                    if (response.getStatusCode().is4xxClientError()) {
                        return Optional.empty();
                    }
                    User user = response.bodyTo(User.class);
                    return Optional.ofNullable(user);
                });
    }
}
```

---

## 14. 安全最佳实践

### 14.1 安全清单

```java
// ========== REST API 安全清单 ==========

// 1. SQL 注入防护
// ✅ 使用参数绑定 (PreparedStatement)
@Query("SELECT u FROM User u WHERE u.email = :email")
Optional<User> findByEmail(@Param("email") String email);
// ❌ 不要拼接 SQL
// String sql = "SELECT * FROM users WHERE email = '" + email + "'";

// 2. XSS 防护
// ✅ JSON 序列化时自动转义 (Jackson 默认处理)
// ✅ 输出编码防止 XSS

// 3. 参数校验白名单
@InitBinder
public void initBinder(WebDataBinder binder) {
    // 只允许这些字段绑定
    binder.setAllowedFields("name", "email", "password", "phone");
    // 禁止绑定 role, admin 等敏感字段
    binder.setDisallowedFields("role", "admin", "permissions");
}

// 4. 防止暴力破解 — 请求频率限制
// 已在上文的 RateLimitInterceptor 中实现

// 5. 敏感信息脱敏
public class UserResponse {
    
    @JsonIgnore  // 密码不返回
    private String password;
    
    @JsonSerialize(using = PhoneMaskSerializer.class)
    private String phone;  // 138****1234
}

public class PhoneMaskSerializer extends JsonSerializer<String> {
    @Override
    public void serialize(String value, JsonGenerator gen, 
                         SerializerProvider provider) throws IOException {
        if (value != null && value.length() == 11) {
            gen.writeString(value.substring(0, 3) + "****" + value.substring(7));
        } else {
            gen.writeString(value);
        }
    }
}

// 6. HTTPS 强制
// 在 Nginx 层或 Spring Boot 配置中强制 HTTPS
// application.yml:
// server.ssl.enabled: true
// server.ssl.key-store: classpath:keystore.p12

// 7. 安全响应头
@Configuration
public class SecurityHeadersConfig implements WebMvcConfigurer {
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    Object handler) {
                response.setHeader("X-Content-Type-Options", "nosniff");
                response.setHeader("X-Frame-Options", "DENY");
                response.setHeader("X-XSS-Protection", "1; mode=block");
                response.setHeader("Cache-Control", "no-store");
                response.setHeader("Strict-Transport-Security", 
                    "max-age=31536000; includeSubDomains");
                return true;
            }
        });
    }
}

// 8. 日志安全 — 不打印敏感信息
public class SensitiveDataConverter extends Converter<String, String> {
    
    private static final Set<String> SENSITIVE_KEYS = Set.of(
        "password", "token", "secret", "authorization", "creditCard"
    );
    
    @Override
    public String convert(String source) {
        // 对敏感数据进行脱敏
        // 实现略
        return source;
    }
}
```

---

## 15. 测试 Spring MVC

### 15.1 MockMvc 基础

```java
// ========== MockMvc 测试 ==========
// MockMvc 不启动完整的 HTTP 服务器
// 在 Servlet 容器模拟环境中测试 Controller

// 方式 1: 自动配置 (推荐)
@SpringBootTest
@AutoConfigureMockMvc  // 自动配置 MockMvc
class UserControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean  // 替换 UserService bean 为 Mock
    private UserService userService;
    
    @Test
    void shouldReturnUser_whenGetById() throws Exception {
        // 准备
        User mockUser = new User(1L, "John", "john@example.com");
        when(userService.findById(1L)).thenReturn(mockUser);
        
        // 执行 & 验证
        mockMvc.perform(get("/api/v1/users/{id}", 1L)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("John"))
                .andExpect(jsonPath("$.data.email").value("john@example.com"));
    }
    
    @Test
    void shouldReturn404_whenUserNotFound() throws Exception {
        when(userService.findById(999L)).thenThrow(
            new BusinessException(GlobalErrorCode.USER_NOT_FOUND));
        
        mockMvc.perform(get("/api/v1/users/{id}", 999L))
                .andExpect(status().isOk())  // 全局异常处理返回 200
                .andExpect(jsonPath("$.code").value(2001))  // 业务错误码
                .andExpect(jsonPath("$.message").value("用户不存在"));
    }
}

// 方式 2: 只测试 Web 层 (Slice Test)
@WebMvcTest(UserController.class)  // 只加载 UserController
class UserControllerWebMvcTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private UserService userService;
    // 不会加载 Service, Repository 等
    // 测试更快，更聚焦
}

// ========== 常用请求方法 ==========
mockMvc.perform(get("/api/users"))                         // GET
mockMvc.perform(post("/api/users"))                        // POST
mockMvc.perform(put("/api/users/{id}", 1))                 // PUT
mockMvc.perform(delete("/api/users/{id}", 1))              // DELETE
mockMvc.perform(patch("/api/users/{id}", 1))               // PATCH

// ========== 设置请求属性 ==========
mockMvc.perform(get("/api/users")
    .param("page", "1")                       // 查询参数
    .param("size", "20")
    .header("Authorization", "Bearer token")   // 请求头
    .cookie("SESSION", "abc123")               // Cookie
    .contentType(MediaType.APPLICATION_JSON)   // Content-Type
    .accept(MediaType.APPLICATION_JSON)        // Accept
    .content("{\"name\":\"John\"}")             // 请求体
    .requestAttr("attr", "value")              // 请求属性
    .sessionAttr("user", new User())           // Session 属性
    .locale(Locale.CHINA)                      // Locale
);

// ========== 常用结果匹配器 ==========
mockMvc.perform(get("/api/users"))
    // 状态码
    .andExpect(status().isOk())                            // 200
    .andExpect(status().isCreated())                       // 201
    .andExpect(status().isBadRequest())                    // 400
    .andExpect(status().isNotFound())                      // 404
    
    // JSON 路径 (JSON Path)
    .andExpect(jsonPath("$.code").value(200))
    .andExpect(jsonPath("$.data").isArray())
    .andExpect(jsonPath("$.data[0].name").value("John"))
    .andExpect(jsonPath("$.data.length()").value(3))
    .andExpect(jsonPath("$.data[?(@.age > 18)]").exists())
    
    // 响应头
    .andExpect(header().string("Content-Type", "application/json"))
    .andExpect(header().exists("X-Request-ID"))
    
    // 响应内容
    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
    .andExpect(content().string(containsString("success")))
    
    // 转发/重定向
    .andExpect(forwardedUrl("/error"))
    .andExpect(redirectedUrl("/login"));
```

### 15.2 @WebMvcTest 切面测试

```java
// ========== @WebMvcTest ==========
// 只加载 Web 层的 Bean（Controller, ControllerAdvice, Filter, Interceptor 等）
// 不会加载 Service, Repository, JPA 等
// 需要手动 Mock 依赖

@WebMvcTest(UserController.class)  // 只测试 UserController
class UserControllerSliceTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;  // Jackson ObjectMapper
    
    @MockBean
    private UserService userService;
    
    @MockBean
    private AuthenticationInterceptor authInterceptor;  // Mock 拦截器
    
    private User mockUser;
    
    @BeforeEach
    void setUp() {
        mockUser = new User(1L, "John", "john@example.com");
        
        // 让认证拦截器放行
        when(authInterceptor.preHandle(any(), any(), any())).thenReturn(true);
    }
    
    @Test
    void shouldCreateUser() throws Exception {
        UserCreateRequest request = new UserCreateRequest();
        request.setName("John");
        request.setEmail("john@example.com");
        request.setPassword("password123");
        
        when(userService.create(any())).thenReturn(mockUser);
        
        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("John"));
        
        verify(userService).create(any());
    }
    
    @Test
    void shouldFailValidation_whenNameBlank() throws Exception {
        UserCreateRequest request = new UserCreateRequest();
        request.setName("");  // 空名称 — 校验失败
        request.setEmail("john@example.com");
        request.setPassword("password123");
        
        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(3001))  // 校验错误码
                .andExpect(jsonPath("$.message").value(containsString("name")));
        
        verify(userService, never()).create(any());
    }
}
```

### 15.3 集成测试

```java
// ========== 集成测试 ==========
// 启动完整上下文，测试真实 HTTP 请求
// 使用 TestRestTemplate 或 WebTestClient

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
// RANDOM_PORT: 启动真实服务器，随机端口
class UserControllerIntegrationTest {
    
    @LocalServerPort
    private int port;
    
    @Autowired
    private TestRestTemplate restTemplate;  // 自动配置，处理跳转
    
    @Autowired
    private UserRepository userRepository;
    
    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }
    
    @Test
    void shouldCreateAndRetrieveUser() {
        // 1. 创建用户
        UserCreateRequest request = new UserCreateRequest();
        request.setName("Jane");
        request.setEmail("jane@example.com");
        request.setPassword("password");
        
        ResponseEntity<Result> createResponse = restTemplate.postForEntity(
            "/api/v1/users",
            request,
            Result.class
        );
        
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(createResponse.getBody().getCode()).isEqualTo(200);
        
        // 2. 查询用户
        ResponseEntity<Result> getResponse = restTemplate.getForEntity(
            "/api/v1/users/{id}",
            Result.class,
            1
        );
        
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        // ...
    }
}

// ========== 使用 Testcontainers ==========
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers  // 使用 Docker 容器运行测试数据库
class FullIntegrationTest {
    
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    void fullUserFlow() {
        // 完整用户流程测试
        // 1. 创建 → 2. 查询 → 3. 更新 → 4. 删除 → 5. 验证删除
    }
}
```

---

## 16. 面试题精选

### 16.1 基础面试题

**Q1: 描述从 HTTP 请求到响应的完整流程。**

A: 
1. 客户端发送 HTTP 请求
2. DispatcherServlet 接收请求，调用 doDispatch()
3. HandlerMapping 根据 URL/方法找到匹配的 Controller 方法和拦截器
4. HandlerAdapter 调用拦截器 preHandle()
5. 参数解析器解析请求参数（@PathVariable, @RequestParam, @RequestBody 等）
6. 执行 Controller 方法，@Valid 校验参数
7. 返回值处理器处理返回值（@ResponseBody → JSON 序列化）
8. 调用拦截器 postHandle() 和 afterCompletion()
9. 发送 HTTP 响应给客户端

**Q2: @Controller 和 @RestController 的区别？**

A: @RestController = @Controller + @ResponseBody。@Controller 用于传统 MVC（返回视图），@RestController 用于 RESTful API（直接返回 JSON/XML）。

### 16.2 进阶面试题

**Q3: 如何实现全局异常处理？**

A: 使用 @RestControllerAdvice + @ExceptionHandler。定义一个全局异常处理器类，为不同类型的异常定义处理方法，统一返回 Result 格式。

**Q4: 如何处理参数校验错误？**

A: 请求体用 @Valid + @RequestBody，参数校验失败抛 MethodArgumentNotValidException。查询参数和路径变量用 @Validated + 校验注解，抛 ConstraintViolationException。在全局异常处理器中捕获并返回友好的错误信息。

**Q5: Interceptor 和 Filter 有什么区别？**

| 方面 | Filter | Interceptor |
|------|--------|-------------|
| 规范 | Servlet | Spring MVC |
| 作用范围 | 所有 Servlet 请求 | 仅 Spring MVC 请求 |
| 访问 Controller 信息 | 不能 | 可以 (handler 参数) |
| 执行顺序 | 在 Interceptor 之前 | 在 Filter 之后 |

### 16.3 高级面试题

**Q6: Spring MVC 如何处理 @RequestBody JSON 到 Java 对象的转换？**

A: 通过 HttpMessageConverter。RequestMappingHandlerAdapter 根据请求的 Content-Type 找到匹配的转换器（如 MappingJackson2HttpMessageConverter 处理 application/json），调用 read() 方法将 JSON 反序列化为 Java 对象，然后执行校验。

**Q7: 如何设计一个可扩展的 API 版本控制方案？**

A: 推荐 URL 路径版本 (/api/v1/...)。实现方式：自定义注解 @ApiVersion，自定义 RequestMappingHandlerMapping 根据版本号选择正确的方法。或者简单地在不同 Controller 类中使用不同的 @RequestMapping。

**Q8: 如何实现接口频率限制？**

A: 
- 方案 1: Interceptor + Redis (滑动窗口)
- 方案 2: Spring Cloud Gateway 的 RequestRateLimiter
- 方案 3: 第三方 API 网关 (Kong, Nginx)
- 方案 4: Bucket4j 库 (令牌桶算法)

---

> **Spring MVC 是构建 RESTful API 的核心武器。掌握请求处理流程、参数绑定、校验、异常处理和拦截器，是成为合格 Java 后端工程师的必备技能。不仅要会用，更要理解每个注解背后的工作机制。**
