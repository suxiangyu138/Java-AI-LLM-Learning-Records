# SpringMVC 类型转换、数据格式化与数据校验

> **三功能协同**：类型转换（String→指定类型）→ 数据格式化（规范格式）→ 数据校验（合法性检查）。

---

## 1. 类型转换

### 1.1 内置转换器（自动生效）

| 前端字符串 | 后端类型 | 示例 |
|-----------|----------|------|
| `"100"` | Integer/Long/Double | `@RequestParam Integer id` |
| `"true"` | Boolean | `@RequestParam Boolean enabled` |
| `"1,2,3"` | Integer[] | `@RequestParam Integer[] ids` |

### 1.2 自定义转换器

```java
// ① 定义枚举
public enum GenderEnum {
    MALE("男"), FEMALE("女");
    public static GenderEnum getByGender(String s) { ... }
}

// ② 实现 Converter 接口
public class StringToGenderConverter implements Converter<String, GenderEnum> {
    @Override
    public GenderEnum convert(String source) {
        return GenderEnum.getByGender(source);
    }
}
```

```xml
<!-- ③ 注册转换器 -->
<bean id="conversionService"
    class="org.springframework.context.support.ConversionServiceFactoryBean">
    <property name="converters">
        <set>
            <bean class="com.example.converter.StringToGenderConverter"/>
        </set>
    </property>
</bean>
<mvc:annotation-driven conversion-service="conversionService"/>
```

---

## 2. 数据格式化

| 注解 | 作用 | 示例 |
|------|------|------|
| `@DateTimeFormat` | 日期格式 | `@DateTimeFormat(pattern = "yyyy-MM-dd")` |
| `@NumberFormat` | 数字格式 | `@NumberFormat(pattern = "#,###")` |

```java
public class User {
    @NumberFormat(pattern = "#,###")
    private Integer age;      // 显示为 12,345

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date birthday;    // 接收 2024-10-01

    @NumberFormat(pattern = "#.##")
    private Double salary;    // 保留 2 位小数
}
```

---

## 3. 数据校验

### 3.1 依赖

```xml
<dependency>
    <groupId>org.hibernate.validator</groupId>
    <artifactId>hibernate-validator</artifactId>
    <version>6.1.7.Final</version>
</dependency>
```

### 3.2 常用校验注解

| 注解 | 作用 | 示例 |
|------|------|------|
| `@NotNull` | 不能为 null（引用类型） | `@NotNull(message = "年龄不能为空")` |
| `@NotBlank` | 字符串不能为空/全空格 | `@NotBlank(message = "姓名不能为空")` |
| `@Min` / `@Max` | 数字最小值/最大值 | `@Min(1) @Max(150)` |
| `@Size` | 字符串长度 | `@Size(min=1, max=50)` |
| `@Pattern` | 正则匹配 | `@Pattern(regexp = "^1[3-9]\\d{9}$")` |
| `@Email` | 邮箱格式 | `@Email(message = "邮箱格式错误")` |

### 3.3 实体类 + Controller

```java
public class User {
    @NotBlank(message = "姓名不能为空")
    private String name;

    @NotNull(message = "年龄不能为空")
    @Min(value = 1, message = "年龄不能小于1")
    @Max(value = 150, message = "年龄不能大于150")
    private Integer age;

    @Email(message = "邮箱格式错误")
    private String email;
}
```

```java
@PostMapping("/add")
public String addUser(@Valid @ModelAttribute User user,
        BindingResult result, Model model) {
    if (result.hasErrors()) {
        model.addAttribute("errors", result.getAllErrors());
        return "addUser";  // 返回表单页显示错误
    }
    // 校验通过 → 执行业务
    return "success";
}
```

### 3.4 JSP 展示错误

```jsp
<form:form modelAttribute="user" action="/user/add" method="post">
    姓名：<form:input path="name"/>
    <form:errors path="name" cssClass="error"/>
</form:form>
```

---

## 4. 三功能协同顺序

```text
前端字符串参数
→ ① 类型转换（String → Integer/Date/...）
→ ② 数据格式化（Date 格式化、数字千分位）
→ ③ 数据校验（@Valid + BindingResult）
→ 通过 → 业务层
→ 失败 → 返回错误信息
```

## 5. 避坑要点

| 问题 | 解决 |
|------|------|
| 类型转换 400 | 前端格式与后端类型匹配（"abc" 不能转 Integer） |
| 格式化注解无效 | 开启 `<mvc:annotation-driven/>` |
| 校验注解无效 | 引入 hibernate-validator + 参数前加 `@Valid` |
| `BindingResult` 异常 | 必须紧跟在 `@Valid` 参数后 |
| 自定义转换器未生效 | 注册 `conversionService` + `<mvc:annotation-driven>` 指定 |
