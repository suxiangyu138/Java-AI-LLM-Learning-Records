# Lombok 核心知识点

## 一、概述

Lombok 是一个编译期（Annotation Processor）Java 库，通过注解自动生成 Getter、Setter、Constructor、Builder 等样板代码，大幅减少 Java 类的冗余代码量。

**核心定位：** 消除 Java 样板代码（Boilerplate），让代码更简洁、专注于业务逻辑。

**官网：** https://projectlombok.org

## 二、核心注解

### 2.1 最常用注解

| 注解 | 生成内容 | 适用场景 |
|------|----------|----------|
| `@Data` | Getter + Setter + toString + equals + hashCode + RequiredArgsConstructor | 数据类（DTO/VO/Entity） |
| `@Getter` / `@Setter` | Getter / Setter 方法 | 需要部分控制 |
| `@Builder` | Builder 模式 | 复杂对象构建 |
| `@AllArgsConstructor` | 全参构造器 | 配合 @Builder |
| `@NoArgsConstructor` | 无参构造器 | JPA Entity / 序列化 |
| `@RequiredArgsConstructor` | final/非空字段的构造器 | 依赖注入（替代 @Autowired） |
| `@Slf4j` | SLF4J Logger 字段 `log` | 日志记录 |

### 2.2 进阶注解

| 注解 | 生成内容 | 说明 |
|------|----------|------|
| `@Value` | 不可变类（final 字段 + Getter + 全参构造 + equals/hashCode） | 替代 @Data 的不可变版本 |
| `@EqualsAndHashCode` | equals() + hashCode() | 可指定只比较部分字段 |
| `@ToString` | toString() | 可排除敏感字段 |
| `@With` | 不可变修改方法（返回新对象） | 适合函数式风格 |
| `@SneakyThrows` | 包装受检异常为非受检异常 | 简化 Lambda 异常处理 |
| `@Cleanup` | try-with-resources 自动关闭 | 替代 finally 手动 close |

## 三、快速上手

### 3.1 依赖配置

```xml
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <version>1.18.32</version>
    <scope>provided</scope>
</dependency>
```

**IDEA 配置：** Settings → Build → Compiler → Annotation Processors → ✓ Enable annotation processing  
**插件：** 安装 Lombok 插件（IDEA 2021+ 已内置）

### 3.2 基础示例

```java
// 传统写法（约 60 行）
public class User {
    private Long id;
    private String name;
    private String email;
    
    public User() {}
    public User(Long id, String name, String email) { ... }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    // ... 更多 getter/setter + equals + hashCode + toString
}
```

```java
// Lombok 写法（约 10 行）
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private Long id;
    private String name;
    private String email;
}
```

### 3.3 Builder 模式

```java
@Builder
@Getter
public class Order {
    private String orderId;
    private BigDecimal amount;
    private OrderStatus status;
}

// 使用
Order order = Order.builder()
    .orderId("ORD-001")
    .amount(new BigDecimal("99.99"))
    .status(OrderStatus.PENDING)
    .build();
```

### 3.4 注解组合速查

| 场景 | 推荐注解组合 |
|------|-------------|
| **Entity** | `@Data @NoArgsConstructor @AllArgsConstructor` |
| **不可变 DTO** | `@Value @Builder` |
| **Service** | `@RequiredArgsConstructor @Slf4j` |
| **配置类** | `@ConfigurationProperties @Data` |
| **请求/响应体** | `@Data @NoArgsConstructor @AllArgsConstructor` |

## 四、使用注意事项

### 4.1 慎用场景

| 场景 | 风险 | 建议 |
|------|------|------|
| **JPA Entity 双向关联** | `@Data` 的 `equals/hashCode` 可能触发懒加载 | 只用 `@Getter @Setter` |
| **集合字段** | `@Builder.Default` 是必须的 | `@Builder.Default List<String> items = new ArrayList<>()` |
| **继承层次** | 容易出错 | 子类用 `@EqualsAndHashCode(callSuper = true)` |
| **Jackson 反序列化** | `@NoArgsConstructor` 有时必须添加 | 配合 `@Jacksonized` |

### 4.2 JPA Entity 安全写法

```java
@Entity
@Getter
@Setter
@NoArgsConstructor(access = PROTECTED)
@ToString(exclude = "department")  // 排除懒加载关联
public class User {
    @Id @GeneratedValue
    private Long id;
    
    @ManyToOne(fetch = LAZY)
    private Department department;  // 不用 @Data，手动控制
}
```

## 五、Lombok 的争议

| 优点 | 缺点 |
|------|------|
| 代码量减少 60%+ | 代码生成不透明（IDE 看不到） |
| 修改字段后自动更新 | 新增开发者需安装插件 |
| 降低维护成本 | Java 标准发展（Record 等）可部分替代 |

**Java 14+ Record 替代方案：**

```java
// Record 替代简单 DTO（不可变 + 自动生成构造器/getter/equals/hashCode）
public record UserDTO(Long id, String name, String email) {}
// 但不可继承、不可 Builder，仍需 Lombok 互补
```

## 六、总结

Lombok 让 Java 代码更简洁，核心价值在于 **@Data + @Builder + @Slf4j** 三大注解。虽然 Java 14+ Record 可替代部分 DTO 场景，但 Lombok 在 Builder 模式、Spring 集成、日志等方面的便利性仍然不可替代。注意 JPA Entity 只使用 `@Getter @Setter` 而非 `@Data`。
