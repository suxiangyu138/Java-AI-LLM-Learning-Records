# 01 - Jackson 核心注解与配置

> 🎯 Jackson 是 Spring Boot 默认的 JSON 序列化库 — 掌握驼峰下划线转换、日期格式化、字段忽略、枚举映射，80% 的 JSON 问题迎刃而解

---

## 目录

1. [核心注解速查](#1-核心注解速查)
2. [命名策略：驼峰 vs 下划线](#2-命名策略驼峰-vs-下划线)
3. [日期格式化](#3-日期格式化)
4. [字段忽略与条件序列化](#4-字段忽略与条件序列化)
5. [枚举序列化](#5-枚举序列化)

---

## 1. 核心注解速查

| 注解 | 作用 | 标注位置 |
|------|------|:---:|
| `@JsonProperty` | 指定 JSON 字段名 | 字段/getter |
| `@JsonFormat` | 日期/数字格式化 | 字段 |
| `@JsonIgnore` | 忽略字段 | 字段 |
| `@JsonInclude` | 条件序列化（null/empty） | 类/字段 |
| `@JsonAlias` | 反序列化别名（多字段名兼容） | 字段 |
| `@JsonPropertyOrder` | 指定序列化顺序 | 类 |
| `@JsonUnwrapped` | 扁平化嵌套对象 | 字段 |

---

## 2. 命名策略：驼峰 vs 下划线

```java
// 场景：Java 驼峰 ↔ 前端下划线

@Data
public class User {
    @JsonProperty("user_name")    // Java: userName → JSON: user_name
    private String userName;

    @JsonProperty("created_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;
}

// 反序列化兼容多个字段名（旧接口下划线、新接口驼峰）
@Data
public class UserV2 {
    @JsonProperty("userName")
    @JsonAlias({"user_name", "username"})  // 前端传任意一种都接受
    private String userName;
}
```

```java
// 全局配置：驼峰 → 下划线（一劳永逸）
@Configuration
public class JacksonConfig {
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer customizer() {
        return builder -> {
            builder.propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
            builder.simpleDateFormat("yyyy-MM-dd HH:mm:ss");
            builder.timeZone("GMT+8");
        };
    }
}
```

```yaml
# 或 application.yml
spring:
  jackson:
    property-naming-strategy: SNAKE_CASE   # 全局下划线
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: GMT+8
```

---

## 3. 日期格式化

```java
@Data
public class Order {
    // Date 类型
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    // LocalDateTime（Java 8+）
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    // LocalDate
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate orderDate;

    // 时间戳格式（毫秒数）
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    private Long timestamp;
}
```

```xml
<!-- ⚠️ LocalDateTime 需要额外依赖 -->
<dependency>
    <groupId>com.fasterxml.jackson.datatype</groupId>
    <artifactId>jackson-datatype-jsr310</artifactId>
</dependency>
```

---

## 4. 字段忽略与条件序列化

```java
@Data
public class User {
    private Long id;
    private String name;

    @JsonIgnore          // 永不出现在 JSON 中
    private String password;

    @JsonIgnore          // 永不出现在 JSON 中
    private String salt;
}

// ⭐ 条件序列化（比 @JsonIgnore 更灵活）
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)   // null 值不序列化
public class Result<T> {
    private int code;
    private String msg;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private T data;             // data 为 null 时不出现在 JSON 中

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<String> errors;  // 空列表不序列化
}
```

| `@JsonInclude` 值 | 条件 |
|-------------------|------|
| `NON_NULL` | 不为 null |
| `NON_EMPTY` | 不为 null 且不为空（String/Collection/Array） |
| `NON_DEFAULT` | 不为默认值 |
| `ALWAYS` | 总是序列化（默认） |

---

## 5. 枚举序列化

```java
// 方式1：默认 — 序列化为 name()
// → JSON: "MALE"
public enum Gender { MALE, FEMALE }

// 方式2：序列化为 code
@Data
public class User {
    @JsonValue          // ← 标注枚举的 getter，序列化时用 code
    private Gender gender;
}

// 方式3：自定义序列化（最灵活）
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum OrderStatus {
    PENDING(0, "待支付"),
    PAID(1, "已支付"),
    CANCELLED(2, "已取消");

    private final int code;
    private final String desc;

    // JSON 输出: { "code": 0, "desc": "待支付" }
}

// 方式4：接受 code 反序列化
@JsonCreator
public static OrderStatus fromCode(int code) {
    for (OrderStatus s : values()) {
        if (s.code == code) return s;
    }
    throw new IllegalArgumentException("未知状态: " + code);
}
```

> 🎯 **最常用组合**：`application.yml` 全局下划线 + `@JsonFormat` 日期 + `@JsonIgnore` 排密码/盐。枚举用 `@JsonValue` 或 `Shape.OBJECT` 而非默认 name()。
