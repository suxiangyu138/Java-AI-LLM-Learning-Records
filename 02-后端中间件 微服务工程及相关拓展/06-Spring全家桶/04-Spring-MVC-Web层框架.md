# 04-Spring MVC Web层框架
> 🎯 Java Web开发的标准框架 — 掌握DispatcherServlet核心调度流程、RESTful接口设计、拦截器/过滤器、全局异常处理，是SpringBoot Web开发的直接基础

---

## 目录
1. [本章总览](#1-本章总览)
2. [Spring MVC核心架构与执行流程](#2-spring-mvc核心架构与执行流程)
3. [请求映射与参数绑定](#3-请求映射与参数绑定)
4. [响应处理与数据转换](#4-响应处理与数据转换)
5. [拦截器与过滤器](#5-拦截器与过滤器)
6. [全局异常处理](#6-全局异常处理)
7. [RESTful API设计规范](#7-restful-api设计规范)
8. [文件上传与跨域CORS](#8-文件上传与跨域cors)
9. [高频踩坑与误区](#9-高频踩坑与误区)
10. [随堂基础练习](#10-随堂基础练习)
11. [章节综合实操案例](#11-章节综合实操案例)
12. [分层综合习题](#12-分层综合习题)
13. [本章复盘速记清单](#13-本章复盘速记清单)
14. [精通拓展补充-P2](#14-精通拓展补充-p2)

---

## 1. 本章总览

### 1.1 知识定位
- **归属**：Spring全家桶核心组件 → 层级2 P0核心必学
- **前置依赖**：IoC容器 + AOP + JavaWeb基础（Servlet、HTTP协议）
- **SpringBoot关系**：SpringBoot内嵌Spring MVC，自动配置DispatcherServlet

### 1.2 三层学习目标

| 级别 | 目标 |
|------|------|
| **基础** | 使用`@RestController`、`@GetMapping`等注解完成CRUD接口 |
| **熟练** | 掌握拦截器、全局异常处理、参数校验，规范开发RESTful接口 |
| **精通** | 吃透DispatcherServlet源码流程、自定义参数解析器、返回值处理器 |

---

## 2. Spring MVC核心架构与执行流程

### 2.1 核心组件

| 组件 | 作用 |
|------|------|
| **DispatcherServlet** | 前端控制器，统一调度入口，所有请求的中央处理器 |
| **HandlerMapping** | 根据请求URL找到对应的Handler（Controller方法） |
| **HandlerAdapter** | 调用Handler，适配不同类型的Controller |
| **HandlerInterceptor** | 拦截器，在Handler执行前后进行拦截处理 |
| **ViewResolver** | 视图解析器，将逻辑视图名解析为具体View |
| **HandlerExceptionResolver** | 异常解析器，处理Controller中的异常 |

### 2.2 完整请求处理流程

```
┌─────────┐     ┌──────────────────┐     ┌───────────────┐
│ 客户端   │ ──→ │ DispatcherServlet│ ──→ │ HandlerMapping │
└─────────┘     └──────────────────┘     └───────┬───────┘
                     ↑  前端控制器                   │ 找到Handler
                     │                              ↓
            ┌────────┴────────┐           ┌─────────────────┐
            │   返回响应给客户端  │           │ HandlerExecutionChain│
            └────────┬────────┘           │ = Handler + 拦截器链  │
                     ↑                    └────────┬────────┘
                     │                             │
            ┌────────┴────────┐                    ↓
            │  ViewResolver   │           ┌─────────────────┐
            │  (或HttpMessage │ ←──────── │ HandlerAdapter  │
            │   Converter)    │           │  调用Handler     │
            └─────────────────┘           └─────────────────┘
```

**详细步骤**：
1. 请求到达 DispatcherServlet
2. DispatcherServlet 调用 HandlerMapping 找到对应的 Handler + 拦截器链
3. 执行拦截器的 `preHandle()` 方法
4. DispatcherServlet 调用 HandlerAdapter 执行 Handler（Controller方法）
5. Handler 执行业务逻辑，返回 ModelAndView 或 @ResponseBody 数据
6. 执行拦截器的 `postHandle()` 方法
7. 视图解析 / 消息转换（JSON序列化）
8. 执行拦截器的 `afterCompletion()` 方法
9. 响应返回给客户端

### 2.3 DispatcherServlet源码核心流程

```java
// DispatcherServlet.doDispatch() 核心流程（简化）
protected void doDispatch(HttpServletRequest request, HttpServletResponse response) {
    HttpServletRequest processedRequest = request;
    HandlerExecutionChain mappedHandler = null;
    
    try {
        // 1. 根据请求找到Handler
        mappedHandler = getHandler(processedRequest);
        
        // 2. 根据Handler找到HandlerAdapter
        HandlerAdapter ha = getHandlerAdapter(mappedHandler.getHandler());
        
        // 3. 执行拦截器的preHandle
        if (!mappedHandler.applyPreHandle(processedRequest, response)) {
            return; // preHandle返回false则终止
        }
        
        // 4. 通过适配器执行Handler（Controller方法）
        ModelAndView mv = ha.handle(processedRequest, response, mappedHandler.getHandler());
        
        // 5. 执行拦截器的postHandle
        mappedHandler.applyPostHandle(processedRequest, response, mv);
        
        // 6. 视图解析和渲染
        processDispatchResult(processedRequest, response, mappedHandler, mv);
    } catch (Exception ex) {
        // 7. 异常处理 → HandlerExceptionResolver
        processDispatchException(processedRequest, response, mappedHandler, ex);
    } finally {
        // 8. 执行拦截器的afterCompletion
        mappedHandler.triggerAfterCompletion(processedRequest, response, null);
    }
}
```

---

## 3. 请求映射与参数绑定

### 3.1 请求映射注解

| 注解 | 说明 | 示例 |
|------|------|------|
| `@RequestMapping` | 通用映射，可指定method/params/headers等 | `@RequestMapping(value="/users", method=GET)` |
| `@GetMapping` | GET请求 | `@GetMapping("/users/{id}")` |
| `@PostMapping` | POST请求 | `@PostMapping("/users")` |
| `@PutMapping` | PUT请求 | `@PutMapping("/users/{id}")` |
| `@DeleteMapping` | DELETE请求 | `@DeleteMapping("/users/{id}")` |
| `@PatchMapping` | PATCH请求（部分更新） | `@PatchMapping("/users/{id}")` |

### 3.2 参数绑定注解完整示例

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    // === 路径参数 ===
    @GetMapping("/{id}")
    public User getUser(@PathVariable("id") Long userId) {
        return userService.findById(userId);
    }

    // === 查询参数 ===
    @GetMapping
    public Page<User> listUsers(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return userService.findByPage(page, size, keyword);
    }

    // === JSON请求体 ===
    @PostMapping
    public User createUser(@RequestBody @Valid UserCreateDTO dto) {
        return userService.create(dto);
    }

    // === 请求头 ===
    @GetMapping("/me")
    public User currentUser(@RequestHeader("Authorization") String token) {
        return userService.getByToken(token);
    }

    // === Cookie ===
    @GetMapping("/session")
    public String sessionInfo(@CookieValue(value = "JSESSIONID", required = false) String sessionId) {
        return sessionId;
    }

    // === 组合参数（自动封装为对象）===
    @GetMapping("/search")
    public List<User> search(UserQuery query) {
        // query的字段自动从请求参数中绑定（如 ?name=张三&age=20）
        return userService.search(query);
    }
}
```

### 3.3 @RequestParam vs @PathVariable vs @RequestBody

| 注解 | 数据来源 | 适用场景 | 请求示例 |
|------|----------|----------|----------|
| `@RequestParam` | URL查询参数 或 form-data | 查询、过滤、分页 | `?page=1&size=10` |
| `@PathVariable` | URL路径中 | 资源标识 | `/users/123` |
| `@RequestBody` | HTTP请求体（JSON/XML） | 创建、更新资源 | `POST /users` Body: `{"name":"张三"}` |

---

## 4. 响应处理与数据转换

### 4.1 @ResponseBody vs @RestController

```java
// @Controller + @ResponseBody = @RestController
@RestController  // 相当于 @Controller + @ResponseBody（所有方法返回JSON）
public class UserController { ... }

@Controller
public class PageController {
    
    @GetMapping("/index")
    public String index() {
        return "index"; // 返回视图名（通过ViewResolver解析到index.html）
    }
    
    @ResponseBody
    @GetMapping("/api/status")
    public Map<String, Object> status() {
        return Map.of("status", "ok"); // 返回JSON
    }
}
```

### 4.2 统一返回结果封装

```java
// 统一响应体
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponse<T> {
    private int code;
    private String message;
    private T data;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "success", data);
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}

@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping("/{id}")
    public ApiResponse<User> getUser(@PathVariable Long id) {
        User user = userService.findById(id);
        return ApiResponse.success(user);
    }
}
// 响应JSON: {"code":200,"message":"success","data":{"id":1,"name":"张三"}}
```

### 4.3 日期格式化

```java
// 方案1：@JsonFormat（Jackson序列化/反序列化）
@Data
public class UserDTO {
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}

// 方案2：@DateTimeFormat（Spring MVC参数绑定）
@GetMapping("/users")
public List<User> listByDate(
    @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
    return userService.findByDate(date);
}

// 方案3：全局配置（application.yml）
spring:
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: GMT+8
```

---

## 5. 拦截器与过滤器

### 5.1 过滤器 Filter vs 拦截器 HandlerInterceptor

| 维度 | 过滤器 Filter | 拦截器 HandlerInterceptor |
|------|-------------|--------------------------|
| **规范** | Servlet规范（javax.servlet / jakarta.servlet） | Spring MVC框架特有 |
| **容器** | Servlet容器（Tomcat）管理 | Spring IOC容器管理 |
| **拦截范围** | 所有进入容器的请求 | 进入DispatcherServlet后的请求 |
| **能获取Bean** | ❌ 不能直接注入Spring Bean | ✅ 可以注入Spring Bean |
| **执行顺序** | Filter先执行 → 再经过拦截器链 | 在Filter之后执行 |
| **适用场景** | 编码设置、CORS、安全过滤、XSS防护 | 登录校验、权限控制、日志记录 |

### 5.2 拦截器实现

```java
@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtils jwtUtils;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, 
                             Object handler) {
        // 放行OPTIONS预检请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        
        String token = request.getHeader("Authorization");
        if (token == null || !jwtUtils.validate(token)) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"未登录\"}");
            return false; // 拦截请求
        }
        
        // 将用户信息存入request Attribute，后续可通过RequestContextHolder获取
        request.setAttribute("currentUser", jwtUtils.parse(token));
        return true; // 放行请求
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response,
                           Object handler, ModelAndView modelAndView) {
        // Controller执行后、视图渲染前
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // 整个请求结束后（视图渲染完成），进行资源清理
    }
}

// 注册拦截器
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")            // 拦截的路径
                .excludePathPatterns("/api/auth/**");  // 排除的路径（登录接口）
    }
}
```

### 5.3 过滤器实现

```java
@WebFilter(urlPatterns = "/*")
public class RequestLogFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpReq = (HttpServletRequest) request;
        
        long start = System.currentTimeMillis();
        log.info("→ {} {}", httpReq.getMethod(), httpReq.getRequestURI());
        
        chain.doFilter(request, response); // 必须调用，否则请求不会继续
        
        long cost = System.currentTimeMillis() - start;
        log.info("← {} {} [{}ms]", httpReq.getMethod(), httpReq.getRequestURI(), cost);
    }
}

// SpringBoot启动类上需加 @ServletComponentScan
@SpringBootApplication
@ServletComponentScan // 扫描@WebFilter/@WebServlet/@WebListener
public class Application { ... }
```

---

## 6. 全局异常处理

### 6.1 @ControllerAdvice + @ExceptionHandler

```java
@RestControllerAdvice  // = @ControllerAdvice + @ResponseBody
@Slf4j
public class GlobalExceptionHandler {

    // 处理业务异常
    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusiness(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return ApiResponse.error(e.getCode(), e.getMessage());
    }

    // 处理参数校验异常
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleValid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ApiResponse.error(400, msg);
    }

    // 处理404
    @ExceptionHandler(NoHandlerFoundException.class)
    public ApiResponse<Void> handle404(NoHandlerFoundException e) {
        return ApiResponse.error(404, "接口不存在: " + e.getRequestURL());
    }

    // 兜底：处理所有未捕获异常
    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleAll(Exception e) {
        log.error("系统异常", e);
        return ApiResponse.error(500, "服务器内部错误");
    }
}
```

### 6.2 异常处理优先级

```
@ExceptionHandler方法级别（精确匹配） 
    → Controller内@ExceptionHandler 
    → @ControllerAdvice全局@ExceptionHandler 
    → Spring默认的BasicErrorController
```

---

## 7. RESTful API设计规范

### 7.1 URL设计

| 操作 | HTTP方法 | URL | 说明 |
|------|----------|-----|------|
| 查询列表 | GET | `/api/users` | 集合资源 |
| 查询单个 | GET | `/api/users/{id}` | 单个资源 |
| 创建 | POST | `/api/users` | 新增资源 |
| 全量更新 | PUT | `/api/users/{id}` | 替换整个资源 |
| 部分更新 | PATCH | `/api/users/{id}` | 更新部分字段 |
| 删除 | DELETE | `/api/users/{id}` | 删除资源 |
| 子资源 | GET | `/api/users/{id}/orders` | 关联资源 |

### 7.2 状态码规范

| 状态码 | 含义 | 使用场景 |
|--------|------|----------|
| `200` | OK | GET/PUT/PATCH成功 |
| `201` | Created | POST创建成功 |
| `204` | No Content | DELETE成功（无响应体） |
| `400` | Bad Request | 参数校验失败 |
| `401` | Unauthorized | 未认证（未登录） |
| `403` | Forbidden | 已认证但无权限 |
| `404` | Not Found | 资源不存在 |
| `409` | Conflict | 资源冲突（如重复创建） |
| `500` | Internal Server Error | 服务器内部错误 |

---

## 8. 文件上传与跨域CORS

### 8.1 文件上传

```java
@RestController
@RequestMapping("/api/files")
public class FileController {

    @PostMapping("/upload")
    public ApiResponse<String> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ApiResponse.error(400, "文件不能为空");
        }
        
        String originalName = file.getOriginalFilename();
        String ext = originalName.substring(originalName.lastIndexOf("."));
        String newName = UUID.randomUUID().toString() + ext;
        
        Path targetPath = Path.of("D:/uploads", newName);
        Files.createDirectories(targetPath.getParent());
        file.transferTo(targetPath);
        
        return ApiResponse.success("/files/" + newName);
    }

    @PostMapping("/batch-upload")
    public ApiResponse<List<String>> uploadBatch(@RequestParam("files") MultipartFile[] files) {
        // 多文件上传
    }
}

# application.yml
spring:
  servlet:
    multipart:
      max-file-size: 10MB       # 单个文件最大大小
      max-request-size: 100MB   # 总请求最大大小
```

### 8.2 跨域CORS

```java
// 方案1：Controller级别
@RestController
@CrossOrigin(origins = "http://localhost:3000", maxAge = 3600)
@RequestMapping("/api")
public class UserController { ... }

// 方案2：全局配置（推荐）
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:3000")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
```

---

## 9. 高频踩坑与误区

| 序号 | 问题 | 原因 | 解决方案 |
|------|------|------|----------|
| 1 | `@PathVariable`拿不到值 | 路径变量名和方法参数名不一致 | 用`@PathVariable("id")`显式指定 |
| 2 | POST请求参数为null | 使用了`@RequestParam`而非`@RequestBody`接收JSON | JSON请求体用`@RequestBody` |
| 3 | 文件上传报413 | 文件超过大小限制 | 配置`multipart.max-file-size` |
| 4 | CORS预检失败 | OPTIONS请求被拦截或未配置CORS | 拦截器放行OPTIONS + CORS配置 |
| 5 | 日期解析失败400 | 日期字符串格式不匹配 | 检查`@JsonFormat`/`@DateTimeFormat`格式 |
| 6 | 拦截器中注入的Bean为null | 拦截器通过new创建而非@Component | 拦截器加`@Component`，配置中用注入的实例 |
| 7 | 404但路径正确 | `@RestController`但没加`@RequestMapping` | 检查Controller类和方法的路径映射 |

---

## 10. 随堂基础练习

1. 编写一个完整的CRUD Controller（GET/POST/PUT/DELETE），返回统一ApiResponse
2. 使用`@Valid` + `@NotBlank`/`@NotNull`进行参数校验
3. 实现一个登录拦截器，从Header中获取token并校验
4. 使用`@ControllerAdvice`处理`MethodArgumentNotValidException`

---

## 11. 章节综合实操案例

```java
// 完整用户管理Controller（包含CRUD + 分页 + 校验 + 统一返回）
@RestController
@RequestMapping("/api/users")
@Validated
@Slf4j
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping
    public ApiResponse<Long> create(@RequestBody @Valid UserCreateDTO dto) {
        Long id = userService.create(dto);
        return ApiResponse.success(id);
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, 
                                     @RequestBody @Valid UserUpdateDTO dto) {
        userService.update(id, dto);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ApiResponse.success(null);
    }

    @GetMapping("/{id}")
    public ApiResponse<UserVO> getById(@PathVariable Long id) {
        return ApiResponse.success(userService.getUserVO(id));
    }

    @GetMapping
    public ApiResponse<PageResult<UserVO>> list(PageQuery query) {
        return ApiResponse.success(userService.pageQuery(query));
    }
}

@Data
public class UserCreateDTO {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 20, message = "用户名长度2-20")
    private String username;

    @NotBlank @Email(message = "邮箱格式错误")
    private String email;

    @NotNull @Min(value = 1) @Max(value = 150)
    private Integer age;
}
```

---

## 12. 分层综合习题

### 基础题
1. 写出`@PathVariable`和`@RequestParam`的区别
2. Spring MVC的DispatcherServlet是什么？简述其工作流程
3. RESTful API中GET、POST、PUT、DELETE分别代表什么操作？

### 进阶应用题
4. 过滤器和拦截器的区别是什么？各适用什么场景？
5. 如何设计一个统一返回结果类？写出关键代码
6. 两个`@ControllerAdvice`都处理`Exception.class`，哪个优先？

### 精通拔高题
7. 画出`DispatcherServlet.doDispatch()`的完整流程图，包括所有分支
8. 自定义一个参数解析器`HandlerMethodArgumentResolver`，从Header中解析当前用户对象
9. Spring MVC如何处理异步请求（`Callable`、`DeferredResult`）？和同步请求的流程有何不同？

---

## 13. 本章复盘速记清单

| 类别 | 要点 |
|------|------|
| **核心流程** | 请求→DispatcherServlet→HandlerMapping→HandlerAdapter→Handler→视图/JSON→响应 |
| **请求注解** | `@PathVariable` `@RequestParam` `@RequestBody` `@RequestHeader` `@CookieValue` |
| **方法映射** | `@GetMapping` `@PostMapping` `@PutMapping` `@DeleteMapping` `@PatchMapping` |
| **拦截器** | `preHandle`→Controller→`postHandle`→视图→`afterCompletion` |
| **过滤器vs拦截器** | 过滤器=Servlet层(不能注入Bean) | 拦截器=Spring层(可注入Bean) |
| **统一异常** | `@RestControllerAdvice` + `@ExceptionHandler` |
| **RESTful** | URL用名词复数、HTTP方法表示操作、正确使用状态码 |

---

## 14. 精通拓展补充-P2

### 14.1 自定义参数解析器

```java
// 从请求中自动解析出当前登录用户
@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
            && parameter.getParameterType().equals(User.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        String token = request.getHeader("Authorization");
        return jwtUtils.parse(token); // 从token解析出User对象
    }
}

// 使用
@GetMapping("/me")
public ApiResponse<User> currentUser(@CurrentUser User user) {
    return ApiResponse.success(user); // user由自定义解析器自动注入
}
```

### 14.2 异步请求处理

```java
// Callable：简单异步
@GetMapping("/async")
public Callable<String> async() {
    return () -> {
        Thread.sleep(2000);
        return "async result";
    };
    // Tomcat工作线程立即释放，由TaskExecutor执行，完成后再次分派响应
}

// DeferredResult：跨线程等待
@GetMapping("/long-poll")
public DeferredResult<String> longPoll() {
    DeferredResult<String> result = new DeferredResult<>(30000L); // 30秒超时
    // 在其他线程（如MQ消费者）中设置结果
    deferredResultMap.put(requestId, result);
    return result;
}
```

### 14.3 Spring MVC vs WebFlux

| 维度 | Spring MVC | Spring WebFlux |
|------|-----------|---------------|
| 编程模型 | 同步阻塞（Servlet API） | 异步非阻塞（Reactive Streams） |
| 容器 | Tomcat/Jetty（Servlet容器） | Netty/Undertow（非Servlet容器） |
| 并发模型 | 线程池（每请求一线程） | 事件循环（少量线程处理大量请求） |
| 数据库支持 | JDBC/JPA（成熟） | R2DBC（生态待完善） |
| 适用场景 | 传统CRUD、事务型业务 | 高并发、网关、流式处理 |
