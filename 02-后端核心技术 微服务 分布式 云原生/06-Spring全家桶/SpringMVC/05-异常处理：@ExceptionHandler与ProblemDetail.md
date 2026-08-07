# 05 异常处理：@ExceptionHandler 与 ProblemDetail

> 异常处理是接口质量的最后一道防线：HandlerExceptionResolver 解析链、@RestControllerAdvice 全局处理、RFC 7807 ProblemDetail 标准错误体——本模块让"所有异常都有规范响应"

---

## 📚 目录

1. [HandlerExceptionResolver 解析链](#1-handlerexceptionresolver-解析链)
2. [@ExceptionHandler 与 @RestControllerAdvice](#2-exceptionhandler-与-restcontrolleradvice)
3. [RFC 7807 ProblemDetail 标准错误体](#3-rfc-7807-problemdetail-标准错误体)
4. [异常处理优先级与局部覆盖](#4-异常处理优先级与局部覆盖)
5. [业务异常体系设计](#5-业务异常体系设计)
6. [生产级全局异常处理器模板](#6-生产级全局异常处理器模板)

---

## 1. HandlerExceptionResolver 解析链

```text
处理器方法抛异常 →
  HandlerExceptionResolver 链（按顺序尝试）：
    ① ExceptionHandlerExceptionResolver   ← @ExceptionHandler / @RestControllerAdvice
    ② ResponseStatusExceptionResolver     ← @ResponseStatus / ResponseStatusException
    ③ DefaultHandlerExceptionResolver     ← 框架异常 → 标准状态码
  都不处理 → 异常冒泡到 Servlet 容器 → 500 / error 页面
```

| 解析器 | 处理对象 | 结果 |
|--------|---------|------|
| ExceptionHandlerExceptionResolver | @ExceptionHandler 注解方法 | 自定义响应（最常用） |
| ResponseStatusExceptionResolver | @ResponseStatus 注解 / ResponseStatusException | 指定状态码 |
| DefaultHandlerExceptionResolver | 框架标准异常 | 400/404/405/406/415... |

**框架异常的默认映射（DefaultHandlerExceptionResolver）：**

| 异常 | 状态码 |
|------|:---:|
| MethodArgumentNotValidException | 400（校验失败） |
| HttpMessageNotReadableException | 400（JSON 解析失败） |
| NoHandlerFoundException | 404 |
| HttpRequestMethodNotSupportedException | 405 |
| HttpMediaTypeNotAcceptableException | 406 |
| HttpMediaTypeNotSupportedException | 415 |

> 🎯 **核心要点**：默认映射是"兜底"，业务上要用 @RestControllerAdvice 覆盖关键异常（校验、业务、未知）——否则客户端拿到的是框架默认错误体（无业务码、格式不统一）。

## 2. @ExceptionHandler 与 @RestControllerAdvice

```java
// 局部：控制器内（只对该控制器生效）
@RestController
public class UserController {

    @GetMapping("/api/users/{id}")
    public User get(@PathVariable Long id) {
        return userService.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(UserNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "code", 40401, "message", e.getMessage()));
    }
}

// 全局：@RestControllerAdvice（作用于所有控制器）
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handle(UserNotFoundException e) { ... }
}
```

| 注解 | 作用域 | 说明 |
|------|--------|------|
| `@ExceptionHandler` | 单个控制器 / Advice 类 | 声明处理的异常类型 |
| `@RestControllerAdvice` | 全局 | 拦截所有控制器的异常（可 basePackages 限定） |
| `@ControllerAdvice` | 全局 | 返回 ModelAndView（服务端渲染） |

> ⚠️ **@RestControllerAdvice 的隐藏职责**：除了异常处理，它还能挂 `@InitBinder`（全局绑定器）与 `@ModelAttribute`（全局模型）——不要只当"异常类"用。

## 3. RFC 7807 ProblemDetail 标准错误体

**ProblemDetail** 是 RFC 7807（Problem Details for HTTP APIs）的 Spring 实现——错误响应的**标准结构**：

```java
// 控制器内直接返回（简单场景）
@GetMapping("/api/users/{id}")
public User get(@PathVariable Long id) {
    throw new ResponseStatusException(HttpStatus.NOT_FOUND,
            "用户不存在: " + id);
}

// 生产场景：全局 handler 构造标准错误体
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ProblemDetail handleUserNotFound(UserNotFoundException e) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, e.getMessage());
        detail.setTitle("User Not Found");
        detail.setProperty("code", 40401);          // 自定义属性（业务码）
        detail.setProperty("timestamp", Instant.now().toString());
        return detail;
    }
}
```

**标准响应体：**

```json
{
  "type": "about:blank",
  "title": "User Not Found",
  "status": 404,
  "detail": "用户不存在: 1001",
  "instance": "/api/users/1001",
  "code": 40401,
  "timestamp": "2026-08-07T19:30:00Z"
}
```

| 字段 | 语义 |
|------|------|
| `type` | 问题类型的 URI（默认 about:blank） |
| `title` | 人类可读的短标题 |
| `status` | HTTP 状态码 |
| `detail` | 详细描述 |
| `instance` | 发生问题的 URI |
| 自定义属性 | setProperty 扩展（业务码、时间戳、字段错误） |

> 🎯 **要点**：ProblemDetail 是 RFC 7807 标准——**不是自定义 JSON，是行业规范**。用它 = 客户端开箱即用的错误解析能力 + 可扩展业务属性。2026 年面试"错误响应怎么做"，答 ProblemDetail 是标准答案。

## 4. 异常处理优先级与局部覆盖

```text
同一异常被多个 handler 命中时的选择：
  ① 距离"抛出点"最近的 @ExceptionHandler（控制器内 > Advice）
  ② 异常类型最具体的 handler（子类优先于父类）
  ③ 同一 Advice 内声明顺序
```

**优先级实战：**

```java
// 全局兜底：Exception（最泛）
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)        // 业务异常（具体）→ 404
    public ProblemDetail handleNotFound(NotFoundException e) { ... }

    @ExceptionHandler(Exception.class)                // 未知异常（兜底）→ 500
    public ProblemDetail handleUnknown(Exception e) {
        log.error("unhandled exception", e);          // 未知异常必须完整日志
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "系统繁忙，请稍后重试");
        detail.setTitle("Internal Server Error");
        detail.setProperty("code", 50000);
        return detail;
    }
}

// 局部覆盖：某控制器需要特殊处理
@RestController
public class SpecialController {

    @ExceptionHandler(NotFoundException.class)         // 局部优先于全局
    public ResponseEntity<?> handleSpecial(NotFoundException e) {
        return ResponseEntity.status(404).body(Map.of("hint", "special handling"));
    }
}
```

> ⚠️ **500 兜底的两条红线**：①必须完整日志（堆栈）——未知异常的排查全靠它；②对外响应**不得泄露堆栈/内部信息**（安全），统一"系统繁忙"。

## 5. 业务异常体系设计

```java
// 业务异常基类（携带业务码 + 状态码）
public class BizException extends RuntimeException {

    private final int code;          // 业务码（如 40001 参数、40101 未登录、40301 无权限）
    private final HttpStatus status; // HTTP 状态码

    public BizException(int code, HttpStatus status, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }
}

// 常用子类
public class NotFoundException extends BizException {
    public NotFoundException(String message) {
        super(40401, HttpStatus.NOT_FOUND, message);
    }
}
public class UnauthorizedException extends BizException { ... }
public class ForbiddenException extends BizException { ... }

// 使用
public User getById(Long id) {
    return repository.findById(id)
            .orElseThrow(() -> new NotFoundException("用户不存在: " + id));
}

// 全局兜底：所有 BizException 一个 handler
@ExceptionHandler(BizException.class)
public ProblemDetail handleBiz(BizException e) {
    ProblemDetail detail = ProblemDetail.forStatusAndDetail(e.getStatus(), e.getMessage());
    detail.setProperty("code", e.getCode());
    return detail;
}
```

| 设计原则 | 说明 |
|---------|------|
| 业务码与 HTTP 码分离 | HTTP 码给协议、业务码给前端逻辑 |
| 异常语义化 | NotFound/Forbidden/Unauthorized 子类化 |
| 服务层抛、全局层转 | 业务代码只抛异常，不做响应 |
| 拒绝异常即响应 | 不在 service 拼响应体（耦合 HTTP 层） |

> 🎯 **要点**：异常体系 = 基类（码+状态）+ 语义子类 + 全局 handler 翻译。**业务代码抛异常、Advice 翻译成响应**——这个分层让 Controller 保持薄、Service 保持纯粹。

## 6. 生产级全局异常处理器模板

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final Logger log = LoggerFactory.getLogger(getClass());

    // ① 参数校验失败（03 篇联动）
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> errors = e.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(FieldError::getField,
                        fe -> fe.getDefaultMessage() == null ? "invalid" : fe.getDefaultMessage(),
                        (a, b) -> a));
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "参数校验失败");
        detail.setProperty("code", 40001);
        detail.setProperty("errors", errors);       // 逐字段错误
        return detail;
    }

    // ② JSON 解析失败
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadable(HttpMessageNotReadableException e) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "请求体格式错误");
        detail.setProperty("code", 40002);
        return detail;
    }

    // ③ 业务异常（5.5 节）
    @ExceptionHandler(BizException.class)
    public ProblemDetail handleBiz(BizException e) { ... }

    // ④ 未认证/无权限（Security 体系联动）
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleDenied(AccessDeniedException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "无权限访问");
    }

    // ⑤ 未知异常兜底
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnknown(Exception e) {
        log.error("unhandled exception", e);
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "系统繁忙，请稍后重试");
        detail.setProperty("code", 50000);
        return detail;
    }
}
```

**上线前异常层自检：**

- [ ] 校验失败统一格式（逐字段 errors）
- [ ] 业务异常带业务码（40001/40101/40301...）
- [ ] 未知异常完整日志 + 不泄露内部信息
- [ ] 认证/授权异常与 Security 体系联动（401/403 语义正确）
- [ ] 全局与局部覆盖的边界测试（控制器内 handler 生效验证）

> 🎯 **核心要点**：异常处理 = 解析链兜底 + Advice 全局翻译 + ProblemDetail 标准体。四件套：校验异常（字段级错误）、业务异常（业务码）、安全异常（401/403）、未知异常（日志 + 不泄露）——配齐就是企业级错误响应。**业务代码只抛语义异常，响应格式是全局层的事**。

---

**上一模块**：[04-响应与消息转换：HttpMessageConverter与Jackson 3](04-响应与消息转换：HttpMessageConverter与Jackson 3.md)　**下一模块**：[06-拦截器与过滤器](06-拦截器与过滤器.md)
