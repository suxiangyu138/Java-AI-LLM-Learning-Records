# SpringMVC 常用注解速查

> **定位**：注解简化 Web 层开发——无需手动配置 Servlet，仅需注解即可完成请求映射、参数绑定、结果返回。

---

## 1. 核心注解速查

### 1.1 控制器标识

| 注解 | 说明 |
|------|------|
| `@Controller` | 标识为控制器（返回视图） |
| `@RestController` | `@Controller` + `@ResponseBody`（返回 JSON） |

### 1.2 请求映射

| 注解 | 说明 |
|------|------|
| `@RequestMapping` | 通用映射（可指定 method/params） |
| `@GetMapping` | GET 请求（查询） |
| `@PostMapping` | POST 请求（新增） |
| `@PutMapping` | PUT 请求（修改） |
| `@DeleteMapping` | DELETE 请求（删除） |

### 1.3 参数绑定

| 注解 | 来源 | 示例 |
|------|------|------|
| `@RequestParam` | URL 查询参数 `?name=张三` | `@RequestParam String name` |
| `@PathVariable` | URL 路径 `/user/{id}` | `@PathVariable Integer id` |
| `@RequestBody` | POST 请求体 JSON | `@RequestBody User user` |
| `@ModelAttribute` | 表单提交 | `@ModelAttribute User user` |
| `@RequestHeader` | 请求头 | `@RequestHeader("Token") String token` |

### 1.4 响应处理

| 注解 | 说明 |
|------|------|
| `@ResponseBody` | 返回 JSON 数据 |
| `@ExceptionHandler` | 异常处理（配合 `@RestControllerAdvice`） |

---

## 2. 实战代码

### 2.1 RESTful CRUD

```java
@RestController
@RequestMapping("/user")
public class UserController {

    @GetMapping("/{id}")
    public User getUser(@PathVariable Integer id) {
        return new User(id, "张三", 20, "男");
    }

    @PostMapping
    public String addUser(@RequestBody User user) {
        return "新增成功：" + user.getName();
    }

    @PutMapping
    public String updateUser(@RequestBody User user) {
        return "修改成功：" + user.getName();
    }

    @DeleteMapping("/{id}")
    public String deleteUser(@PathVariable Integer id) {
        return "删除成功：" + id;
    }
}
```

### 2.2 全局异常处理

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(Exception.class)
    public String handle(Exception e) {
        return "请求异常：" + e.getMessage();
    }
}
```

---

## 3. 避坑要点

| 问题 | 解决 |
|------|------|
| 注解导包错误 | 所有注解在 `org.springframework.web.bind.annotation` 包 |
| `@ResponseBody` 用错场景 | 返回视图不能用，只能 JSON |
| 参数绑定为 null | 前端参数名必须与 `@RequestParam`/实体类属性名一致 |
| `@PathVariable` 与 URL 占位符不匹配 | 名称必须一致 |
| 405 Method Not Allowed | HTTP 方法与 `@GetMapping`/`@PostMapping` 不匹配 |
| Controller 未被扫描 | 检查 `<context:component-scan>` 包路径 |
