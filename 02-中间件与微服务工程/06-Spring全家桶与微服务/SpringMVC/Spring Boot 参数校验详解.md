# Spring Boot 参数校验详解

## 基础依赖

Spring Boot 自带 `spring-boot-starter-validation`（2.3+ 需单独引入）：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

## 常用校验注解

| 注解 | 说明 | 示例 |
|------|------|------|
| `@NotNull` | 不能为 null | 适用于所有类型 |
| `@NotEmpty` | 不能为 null 且不能为空（长度>0） | 字符串、集合、数组 |
| `@NotBlank` | 不能为 null 且 trim 后长度>0 | 仅字符串 |
| `@Size(min, max)` | 长度范围 | 字符串、集合 |
| `@Min` / `@Max` | 数值最小/最大值 | 数字 |
| `@Email` | 邮箱格式 | 字符串 |
| `@Pattern` | 正则匹配 | 字符串 |
| `@Positive` / `@Negative` | 正数/负数 | 数字 |
| `@Past` / `@Future` | 过去/未来日期 | 日期类型 |

## 使用方式

### 请求体校验（@RequestBody）

```java
@Data
public class UserCreateDTO {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 20, message = "用户名长度2-20位")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, message = "密码至少6位")
    private String password;

    @Email(message = "邮箱格式不正确")
    private String email;

    @NotNull(message = "年龄不能为空")
    @Min(value = 0, message = "年龄不能为负数")
    @Max(value = 150, message = "年龄不能超过150")
    private Integer age;
}

@RestController
public class UserController {
    @PostMapping("/users")
    public Result<?> create(@Valid @RequestBody UserCreateDTO dto) {
        // @Valid 触发校验，失败抛 MethodArgumentNotValidException
        userService.create(dto);
        return Result.ok();
    }
}
```

### 路径参数 + 查询参数校验

需要在 Controller 类上加 `@Validated`：

```java
@RestController
@Validated
public class UserController {
    @GetMapping("/users/{id}")
    public Result<User> getById(
            @PathVariable @Min(1) Long id) {
        return Result.ok(userService.getById(id));
    }

    @GetMapping("/users")
    public Result<Page<User>> list(
            @RequestParam @Min(1) Integer page,
            @RequestParam @Min(1) @Max(100) Integer size) {
        return Result.ok(userService.page(page, size));
    }
}
```

失败抛 `ConstraintViolationException`。

## 分组校验

不同场景用不同校验规则：

```java
public class UserCreateDTO {
    @NotBlank(groups = {Create.class, Update.class})
    private String username;

    @NotBlank(groups = Create.class)  // 仅创建时校验
    @Size(min = 6, groups = Create.class)
    private String password;

    // 分组标记接口
    public interface Create {}
    public interface Update {}
}

// Controller 中指定校验分组
@PostMapping
public Result<?> create(@Validated(UserCreateDTO.Create.class) @RequestBody UserCreateDTO dto) { }
```

## 自定义校验注解

```java
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PhoneValidator.class)
public @interface Phone {
    String message() default "手机号格式不正确";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

public class PhoneValidator implements ConstraintValidator<Phone, String> {
    private static final Pattern PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value != null && PATTERN.matcher(value).matches();
    }
}
```

## 全局异常处理配合

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handle(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return Result.fail(400, msg);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Result<?> handle(ConstraintViolationException ex) {
        return Result.fail(400, ex.getMessage());
    }
}
```

## 实战要点

- 不要用 entity 类直接接收请求参数，应该用 DTO 放校验注解
- 前端校验提升体验，后端校验保证安全，两层都要做
- service 层方法也可以加 `@Validated`，但业务上通常靠逻辑判断而非注解校验
- message 不要返回"系统错误"这种废话，给调用方有用的提示
