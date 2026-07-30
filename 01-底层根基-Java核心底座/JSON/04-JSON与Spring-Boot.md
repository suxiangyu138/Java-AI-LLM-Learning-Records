# JSON 与 Spring Boot

> Spring Boot 将 Jackson 无缝丝滑地融入 Web 层——理解自动配置原理，才能游刃有余地定制和排错

## 📚 目录

1. [Spring Boot Jackson 自动配置](#1)
2. [application.yml 配置大全](#2)
3. [自定义 ObjectMapper](#3)
4. [REST API JSON 最佳实践](#4)
5. [全局异常处理](#5)
6. [日期时间处理](#6)
7. [使用 Gson / Fastjson2 替换 Jackson](#7)

---

## 1. Spring Boot Jackson 自动配置 {#1}

### 1.1 JacksonAutoConfiguration 原理

Spring Boot 的 `JacksonAutoConfiguration` 自动装配流程：

```
Spring Boot 启动
│
├── 检测 classpath 是否有 jackson-databind
│   └── 有 → 自动配置 Jackson
│
├── 创建 ObjectMapper Bean
│   ├── 注册模块（SPI 发现：JavaTimeModule, Jdk8Module 等）
│   ├── 应用 application.yml 中的 spring.jackson.* 配置
│   ├── 注入所有 Jackson2ObjectMapperBuilderCustomizer Bean
│   └── 注入所有 Module Bean
│
├── 创建 MappingJackson2HttpMessageConverter
│   └── 使用上述 ObjectMapper
│
└── 注册到 WebMvcConfigurer 的消息转换器列表
```

### 1.2 查看当前 ObjectMapper 配置

```java
@RestController
public class DebugController {

    @Autowired
    private ObjectMapper mapper;

    @GetMapping("/debug/jackson-config")
    public Map<String, Object> debugJackson() {
        return Map.of(
            "dateFormat", String.valueOf(mapper.getDateFormat()),
            "serializationInclusion", mapper.getSerializationConfig()
                .getDefaultPropertyInclusion().getValueInclusion().name(),
            "registeredModuleIds", mapper.getRegisteredModuleIds().stream()
                .map(Object::toString).toList(),
            "deserializationConfig", Map.of(
                "FAIL_ON_UNKNOWN_PROPERTIES",
                    mapper.getDeserializationConfig()
                        .isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            )
        );
    }
}
```

---

## 2. application.yml 配置大全 {#2}

### 2.1 完整配置清单

```yaml
spring:
  jackson:
    # ── 日期格式 ──
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: Asia/Shanghai                    # 时区

    # ── 序列化 ──
    serialization:
      WRITE_DATES_AS_TIMESTAMPS: false          # 日期不用时间戳
      WRITE_ENUMS_USING_TO_STRING: true         # 枚举写 toString()
      WRITE_ENUMS_USING_INDEX: false            # 枚举不写索引
      WRITE_NULL_MAP_VALUES: false              # Map 中 null 值不输出
      WRITE_EMPTY_JSON_ARRAYS: true             # 空数组输出 []
      INDENT_OUTPUT: false                      # 开发环境可设为 true 美化

    # ── 反序列化 ──
    deserialization:
      FAIL_ON_UNKNOWN_PROPERTIES: false         # 忽略未知属性
      FAIL_ON_NULL_FOR_PRIMITIVES: true         # null 不能赋值基本类型
      ACCEPT_SINGLE_VALUE_AS_ARRAY: true        # 单值当数组处理
      ACCEPT_EMPTY_STRING_AS_NULL_OBJECT: true  # "" 当作 null
      READ_UNKNOWN_ENUM_VALUES_AS_NULL: true    # 未知枚举 → null

    # ── 属性包含策略 ──
    default-property-inclusion: non_null        # 全局排除 null

    # ── 解析器 ──
    parser:
      ALLOW_SINGLE_QUOTES: false                # 不允许单引号（严格模式）
      ALLOW_UNQUOTED_FIELD_NAMES: false         # 不允许无引号字段名

    # ── 生成器 ──
    generator:
      WRITE_BIGDECIMAL_AS_PLAIN: true           # BigDecimal 输出普通数字

    # ── 命名策略 ──
    property-naming-strategy: SNAKE_CASE        # Java camelCase → JSON snake_case

    # ── Locale ──
    locale: zh_CN

    # ── Mapper 特性 ──
    mapper:
      ACCEPT_CASE_INSENSITIVE_PROPERTIES: true   # 大小写不敏感
      USE_STD_BEAN_NAMING: false                 # 使用标准 getter 命名

    # ── 可见性（一般不配置）──
    visibility:
      field: ANY                                 # 字段可见性（一般不调）
      getter: NON_PRIVATE
      is-getter: NON_PRIVATE
      setter: NON_PRIVATE
      creator: NON_PRIVATE
```

### 2.2 环境差异化配置

```yaml
# application-dev.yml —— 开发环境
spring:
  jackson:
    serialization:
      INDENT_OUTPUT: true          # 美化输出，方便调试
    deserialization:
      FAIL_ON_UNKNOWN_PROPERTIES: true   # 尽早发现字段不匹配

# application-production.yml —— 生产环境
spring:
  jackson:
    serialization:
      INDENT_OUTPUT: false         # 紧凑输出，节省带宽
    deserialization:
      FAIL_ON_UNKNOWN_PROPERTIES: false  # 兼容性优先
```

---

## 3. 自定义 ObjectMapper {#3}

### 3.1 方式一：Jackson2ObjectMapperBuilderCustomizer（推荐）

```java
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> {
            // ── 特性开关 ──
            builder.featuresToEnable(
                SerializationFeature.WRITE_ENUMS_USING_TO_STRING
            );
            builder.featuresToDisable(
                SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,
                DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
            );

            // ── 模块 ──
            builder.modules(
                new JavaTimeModule(),
                new Jdk8Module()
            );

            // ── 序列化器 ──
            builder.serializerByType(Long.class,
                new ToStringSerializer());  // Long → String

            // ── 日期格式 ──
            builder.dateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
            builder.timeZone("Asia/Shanghai");

            // ── 属性过滤 ──
            builder.serializationInclusion(JsonInclude.Include.NON_NULL);
            builder.propertyNamingStrategy(
                PropertyNamingStrategies.SNAKE_CASE);
        };
    }
}
```

### 3.2 方式二：直接替换 ObjectMapper Bean

```java
@Configuration
public class JacksonConfig {

    @Bean
    @Primary      // 需要 @Primary 覆盖默认的 ObjectMapper
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .registerModule(new Jdk8Module())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }
}
```

> ⚠️ **注意**：替换 ObjectMapper Bean 会绕过 Spring Boot 的 `spring.jackson.*` 配置。推荐用 **方式一**（Customizer），保留 Boot 的自动配置。

### 3.3 方式三：注入 Module Bean

```java
@Configuration
public class JacksonModuleConfig {

    // Spring Boot 自动发现所有 Module Bean 并注册到 ObjectMapper
    @Bean
    public Module customModule() {
        SimpleModule module = new SimpleModule("CustomModule");

        // 注册自定义序列化器
        module.addSerializer(Long.class, new ToStringSerializer());

        // 注册自定义反序列化器
        module.addDeserializer(String.class, new TrimStringDeserializer());

        // 注册 Mixin
        module.setMixInAnnotation(User.class, UserMixin.class);

        return module;
    }
}

// Mixin：不修改原类，通过注解"混入"Jackson 行为
@JsonIgnoreProperties({"password", "internalId"})
abstract class UserMixin {}
```

---

## 4. REST API JSON 最佳实践 {#4}

### 4.1 统一响应格式

```java
// ── 统一响应体 ──
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponse<T> {

    private int code;
    private String message;
    private T data;
    private long timestamp;
    private String requestId;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "success", data,
            System.currentTimeMillis(), MDC.get("requestId"));
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null,
            System.currentTimeMillis(), MDC.get("requestId"));
    }
}
```

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping("/{id}")
    public ApiResponse<User> getUser(@PathVariable Long id) {
        User user = userService.findById(id);
        return ApiResponse.success(user);
    }

    @GetMapping
    public ApiResponse<PageResult<User>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResult<User> result = userService.findPage(page, size);
        return ApiResponse.success(result);
    }
}
```

### 4.2 @JsonView 实现字段级控制

```java
// ── 视图定义 ──
public class Views {
    public interface Public {}      // 公开视图
    public interface Internal extends Public {}  // 内部视图（包含更多字段）
    public interface Admin extends Internal {}   // 管理员视图
}

public class User {
    @JsonView(Views.Public.class)
    private Long id;

    @JsonView(Views.Public.class)
    private String username;

    @JsonView(Views.Internal.class)      // 仅内部视图可见
    private String email;

    @JsonView(Views.Admin.class)         // 仅管理员视图可见
    private BigDecimal salary;

    @JsonView(Views.Admin.class)
    private String phone;
}
```

```java
// ── 控制器中使用 ──
@RestController
public class UserController {

    // 公开接口 → 仅显示 id + username
    @GetMapping("/api/public/users/{id}")
    @JsonView(Views.Public.class)
    public User getPublicUser(@PathVariable Long id) {
        return userService.findById(id);
    }

    // 内部接口 → 显示 id + username + email
    @GetMapping("/api/internal/users/{id}")
    @JsonView(Views.Internal.class)
    public User getInternalUser(@PathVariable Long id) {
        return userService.findById(id);
    }

    // 管理接口 → 显示所有字段
    @GetMapping("/api/admin/users/{id}")
    @JsonView(Views.Admin.class)
    public User getAdminUser(@PathVariable Long id) {
        return userService.findById(id);
    }
}
```

### 4.3 字段过滤最佳实践

| 策略 | 实现方式 | 适用场景 |
|------|---------|---------|
| **静态排除** | `@JsonIgnore` | 永不暴露的字段（密码） |
| **视图控制** | `@JsonView` | 同一对象在不同 API 返回不同字段 |
| **动态过滤** | `@JsonFilter` + `FilterProvider` | 运行时决定过滤哪些字段 |
| **DTO 分层** | 不同 VO/DTO 类 | 字段差异很大，完全不同的对象 |
| **包含策略** | `@JsonInclude(NON_NULL)` | 全局排除 null 值 |

---

## 5. 全局异常处理 {#5}

### 5.1 JSON 反序列化异常处理

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 请求体 JSON 格式错误
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleJsonParseError(
            HttpMessageNotReadableException ex) {

        String message = "JSON 格式错误";
        if (ex.getCause() instanceof InvalidFormatException ife) {
            message = String.format("字段 '%s' 值格式错误: %s",
                ife.getPath().stream()
                    .map(JsonMappingException.Reference::getFieldName)
                    .collect(Collectors.joining(".")),
                ife.getOriginalMessage());
        } else if (ex.getCause() instanceof UnrecognizedPropertyException upe) {
            message = String.format("未知字段: '%s'",
                upe.getPropertyName());
        }

        log.warn("JSON 解析失败: {}", message);
        return ApiResponse.error(400, message);
    }

    /**
     * Jackson 序列化异常（通常不会发生，作为兜底）
     */
    @ExceptionHandler(JsonProcessingException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleJsonProcessing(JsonProcessingException ex) {
        log.error("JSON 处理异常", ex);
        return ApiResponse.error(500, "JSON 处理异常");
    }

    /**
     * 参数校验异常（@Valid + @RequestBody）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<List<FieldError>> handleValidation(
            MethodArgumentNotValidException ex) {

        List<FieldError> errors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(e -> new FieldError(e.getField(), e.getDefaultMessage()))
            .toList();

        return ApiResponse.error(422, errors.toString());
    }
}

@Data
@AllArgsConstructor
class FieldError {
    private String field;
    private String message;
}
```

### 5.2 @Valid + @RequestBody 组合

```java
// ── 请求体校验 ──
@Data
public class CreateUserRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 50, message = "用户名长度 2-50")
    private String username;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @Min(value = 0, message = "年龄不能为负数")
    @Max(value = 150, message = "年龄不能超过 150")
    private int age;
}

@PostMapping
public ApiResponse<User> createUser(
        @Valid @RequestBody CreateUserRequest request) {
    User user = userService.create(request);
    return ApiResponse.success(user);
}
```

---

## 6. 日期时间处理 {#6}

### 6.1 Java 8 Time API 序列化

```java
// jackson-datatype-jsr310 提供以下类型支持：
// LocalDate      → "2026-07-30"
// LocalTime      → "10:30:00"
// LocalDateTime  → "2026-07-30T10:30:00"
// ZonedDateTime  → "2026-07-30T10:30:00+08:00"
// Instant        → "2026-07-30T02:30:00Z"  (UTC)
// Duration       → "3600.000000000"  (秒)
// Period         → "P1Y2M3D"
```

### 6.2 自定义日期格式

```java
// ── 全局配置 ──
@Bean
public Jackson2ObjectMapperBuilderCustomizer dateCustomizer() {
    return builder -> {
        builder.featuresToDisable(
            SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        builder.dateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
    };
}

// ── 字段级覆盖 ──
public class Event {
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant utcTime;

    @JsonFormat(shape = JsonFormat.Shape.NUMBER)  // 输出毫秒时间戳
    private long timestamp;
}
```

### 6.3 时区最佳实践

```yaml
spring:
  jackson:
    time-zone: Asia/Shanghai   # 全局时区
    date-format: yyyy-MM-dd HH:mm:ss
```

```java
// 最佳实践规则：
// 1. 服务内部 → 统一用 UTC (Instant)
// 2. API 响应 → 按客户端时区（或约定为 UTC+8）
// 3. 数据库 → 存储 UTC
// 4. 展示层 → 前端自行转换时区
```

---

## 7. 使用 Gson / Fastjson2 替换 Jackson {#7}

### 7.1 替换为 Gson

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <exclusions>
        <!-- 排除 Jackson -->
        <exclusion>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-json</artifactId>
        </exclusion>
    </exclusions>
</dependency>

<!-- 添加 Gson -->
<dependency>
    <groupId>com.google.code.gson</groupId>
    <artifactId>gson</artifactId>
</dependency>
```

```java
@Configuration
public class GsonConfig {

    @Bean
    public GsonBuilder gsonBuilder() {
        return new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .serializeNulls();
    }

    // Spring Boot 自动配置 HttpMessageConverter 使用 Gson
    // 前提：classpath 中只有 Gson，没有 Jackson
}
```

### 7.2 替换为 Fastjson2

```xml
<!-- 排除 Jackson 同上 -->

<dependency>
    <groupId>com.alibaba.fastjson2</groupId>
    <artifactId>fastjson2-extension-spring6</artifactId>
    <version>2.0.53</version>
</dependency>
```

```java
@Configuration
public class Fastjson2Config {

    @Bean
    public HttpMessageConverters fastjson2Converters() {
        FastJsonHttpMessageConverter converter =
            new FastJsonHttpMessageConverter();

        // 安全配置
        JSON.config(JSONReader.Feature.SupportAutoType, false);

        return new HttpMessageConverters(converter);
    }
}
```

> ⚠️ **警告**：替换默认 Jackson 会增加维护成本——第三方库可能隐式依赖 Jackson。除非有明确的性能 SLA 要求，否则不建议替换。

---

**返回总览：** [00-JSON 知识体系总览](./00-JSON知识体系总览.md) | **下一模块：** [05-JSON 最佳实践与安全](./05-JSON最佳实践与安全.md)
