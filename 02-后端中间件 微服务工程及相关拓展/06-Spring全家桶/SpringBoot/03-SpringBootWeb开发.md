# 03 - Spring Boot Web 开发

> 定位：Spring MVC 核心、RESTful API、参数绑定、全局异常处理、参数校验、声明式 HTTP 客户端——Web 层实战

## 📚 目录

1. [Spring MVC 核心](#1-spring-mvc-核心)
2. [RESTful 控制器](#2-restful-控制器)
3. [参数绑定全解](#3-参数绑定全解)
4. [参数校验](#4-参数校验)
5. [全局异常处理](#5-全局异常处理)
6. [声明式 HTTP 客户端（Boot 4）](#6-声明式-http-客户端boot-4)

---

## 1. Spring MVC 核心

### 1.1 请求处理链路

```
请求 → DispatcherServlet（前端控制器）
  → HandlerMapping（找 Controller 方法）
  → HandlerAdapter（执行 + 参数绑定）
  → Controller 方法
  → 返回值解析（@ResponseBody → JSON）
  → 响应

⚠️ 面试必答：
"Spring MVC 核心 = DispatcherServlet 前端控制器——
 所有请求先到它，再分发到 Controller；
 HandlerMapping 找方法、HandlerAdapter 执行。"
```

### 1.2 分层结构

```
Controller（接收参数/返回响应）→ Service（业务逻辑）
  → Repository（数据访问）

⚠️ 规范：
  Controller 不写业务逻辑（薄控制器）
  Service 管事务与业务
  分层解耦是 Spring 架构第一原则
```

---

## 2. RESTful 控制器

```java
@RestController                       // = @Controller + @ResponseBody
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {   // ⚠️ 构造器注入
        this.userService = userService;
    }

    @GetMapping                     // 查询列表
    public PageResult<UserVO> list(@RequestParam(defaultValue = "1") int page) {
        return userService.page(page);
    }

    @GetMapping("/{id}")            // 查询单个
    public UserVO get(@PathVariable Long id) {
        return userService.getById(id);
    }

    @PostMapping                    // 创建 → 201
    @ResponseStatus(HttpStatus.CREATED)
    public UserVO create(@Valid @RequestBody UserCreateDTO dto) {
        return userService.create(dto);
    }

    @PutMapping("/{id}")            // 全量更新
    public UserVO update(@PathVariable Long id,
                         @Valid @RequestBody UserUpdateDTO dto) {
        return userService.update(id, dto);
    }

    @DeleteMapping("/{id}")         // 删除
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        userService.delete(id);
    }
}
```

> 🎯 **要点**：RESTful 五方法——GET 查、POST 建（201）、PUT 改、DELETE 删。DTO 接收（防越权字段）、VO 返回（不暴露实体）是规范。

---

## 3. 参数绑定全解

```java
@RestController
@RequestMapping("/demo")
public class ParamController {

    // ① 路径参数
    @GetMapping("/path/{id}")
    public String path(@PathVariable Long id) { }

    // ② 查询参数
    @GetMapping("/query")
    public String query(@RequestParam String name,
                        @RequestParam(defaultValue = "10") int size) { }

    // ③ 表单/JSON 体（对象绑定）
    @PostMapping("/body")
    public String body(@RequestBody UserDTO user) { }

    // ④ 请求头
    @GetMapping("/header")
    public String header(@RequestHeader("X-Token") String token) { }

    // ⑤ Cookie
    @GetMapping("/cookie")
    public String cookie(@CookieValue("JSESSIONID") String sid) { }

    // ⑥ 日期格式化
    @GetMapping("/date")
    public String date(@DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) { }

    // ⑦ 文件上传
    @PostMapping("/upload")
    public String upload(@RequestParam("file") MultipartFile file) {
        file.getOriginalFilename();
        file.getSize();
        // 保存逻辑
    }
}
```

---

## 4. 参数校验

```java
// ① DTO 校验注解（Bean Validation 3.1）
public class UserCreateDTO {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 20)
    private String name;

    @NotNull
    @Min(0) @Max(150)
    private Integer age;

    @Email
    private String email;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式错误")
    private String phone;
}

// ② 控制器启用校验
@PostMapping
public UserVO create(@Valid @RequestBody UserCreateDTO dto) { }

// ③ 分组校验（新增/更新不同规则）
public class UserDTO {
    @Null(groups = Create.class)
    @NotNull(groups = Update.class)
    private Long id;
}

// ⚠️ 校验失败 → MethodArgumentNotValidException → 全局异常处理
```

> 🎯 **要点**：参数校验 = DTO 注解（@Valid）+ 分组校验 + 全局异常统一响应。**DTO 校验是防御注入/越权/非法输入的第一道门**。

---

## 5. 全局异常处理

```java
// ⚠️ 全局异常处理器（统一响应格式）
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 业务异常（可预期）
    @ExceptionHandler(BizException.class)
    public Result<Void> handleBiz(BizException e) {
        return Result.error(e.getCode(), e.getMessage());
    }

    // 参数校验异常
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return Result.error(400, msg);
    }

    // 兜底异常（系统异常）
    @ExceptionHandler(Exception.class)
    public Result<Void> handleOther(Exception e) {
        log.error("系统异常", e);                 // ⚠️ 记录日志
        return Result.error(500, "系统繁忙");      // ⚠️ 不暴露内部细节
    }
}
```

```java
// 统一响应封装
@Data
public class Result<T> {
    private int code;
    private String message;
    private T data;

    public static <T> Result<T> success(T data) { }
    public static <T> Result<T> error(int code, String msg) { }
}
```

> 🎯 **要点**：全局异常三块——业务异常（可预期）、校验异常（参数）、兜底异常（隐藏细节）。**统一响应格式**（code/message/data）是前后端协作的约定。

---

## 6. 声明式 HTTP 客户端（Boot 4）

```java
// ⚠️ Boot 4 新特性：@HttpServiceClient 声明式调用（官方替代 Feign）
// ① 定义接口
@HttpExchange("/api/users")               // 基础路径
public interface UserClient {

    @GetExchange("/{id}")                 // GET
    UserVO getUser(@PathVariable Long id);

    @PostExchange                         // POST
    @HttpServiceMethod
    UserVO createUser(@RequestBody UserCreateDTO dto);

    @PutExchange("/{id}")
    UserVO updateUser(@PathVariable Long id, @RequestBody UserUpdateDTO dto);

    @DeleteExchange("/{id}")
    void deleteUser(@PathVariable Long id);
}

// ② 启用客户端代理
@Configuration
public class HttpClientConfig {
    @Bean
    UserClient userClient(HttpServiceProxyFactory factory) {
        return factory.createClient(UserClient.class);
    }
}

// ③ 注入使用（像调用本地方法）
@Service
public class OrderService {
    private final UserClient userClient;    // 自动注入
    // userClient.getUser(id);  ← HTTP 调用
}
```

> 🎯 **要点**：Boot 4 的 `@HttpServiceClient`（@HttpExchange/@GetExchange 等）让"接口即客户端"——官方声明式 HTTP 方案，替代 RestTemplate/Feign。微服务内部调用的标准答案。

---

> 🎯 **核心要点**：Web 体系 = **DispatcherServlet**（前端控制器分发）+ **RESTful 五方法**（DTO/VO 分层）+ **参数绑定七式**（路径/查询/体/头/Cookie/日期/文件）+ **校验**（@Valid + 分组）+ **全局异常**（业务/校验/兜底三分类）+ **@HttpServiceClient**（Boot 4 声明式调用）。"薄控制器 + DTO 校验 + 统一异常"是企业级 Web 三规范。

---

**返回总览**：[00-SpringBoot总览与核心概念](00-SpringBoot总览与核心概念.md) | **上一篇**：[02-SpringBoot配置体系](02-SpringBoot配置体系.md) | **下一篇**：[04-SpringBoot数据访问](04-SpringBoot数据访问.md)
