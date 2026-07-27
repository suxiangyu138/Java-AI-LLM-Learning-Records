# 07 - Record 与密封类

> 🎯 Record 和密封类是 Java 14-17 最重要的类型系统升级——Record 消灭了 DTO 的样板代码，密封类让类型层次可控。这两个特性在 AI 数据处理和 API 设计中极其实用

---

## 目录

1. [Record：不可变数据载体](#1-record不可变数据载体)
2. [密封类：可控的类型层次](#2-密封类可控的类型层次)
3. [Record Pattern 解构](#3-record-pattern-解构)

---

## 1. Record：不可变数据载体

### 1.1 消灭样板代码

```java
// Java 8: 60 行样板代码
public class UserDTO {
    private final Long id;
    private final String name;
    private final String email;

    public UserDTO(Long id, String name, String email) {
        this.id = id; this.name = name; this.email = email;
    }
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    @Override public boolean equals(Object o) { /* ... */ }
    @Override public int hashCode() { /* ... */ }
    @Override public String toString() { /* ... */ }
}

// Java 16+: 1 行！
public record UserDTO(Long id, String name, String email) {}
// 自动生成：构造器、getter、equals、hashCode、toString
```

### 1.2 Record 的特性与限制

```java
// 自定义构造器（参数校验）
public record UserDTO(Long id, String name, String email) {
    public UserDTO {
        if (id == null) throw new IllegalArgumentException("id required");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("name required");
    }

    // 派生方法
    public String displayName() {
        return name + " <" + email + ">";
    }
}

// Record 的限制：
// ❌ 不能继承其他类（隐式继承 java.lang.Record）
// ❌ 不能被继承（final）
// ❌ 字段都是 final（不可变）
// ✅ 可以实现接口
public record UserSummary(Long id, String name) implements Serializable {}
```

## 2. 密封类：可控的类型层次

```java
// 密封类 = 限定谁能继承我
public sealed class Result<T>
    permits Success<T>, Failure<T>, Loading<T> {
}
// 只能被 Success、Failure、Loading 继承

public record Success<T>(T data) extends Result<T> {}
public record Failure<T>(String error) extends Result<T> {}
public record Loading<T>() extends Result<T> {}

// 使用时：编译器知道所有可能的子类 → 保证 switch 穷举
String message = switch (result) {
    case Success<T>(var data) -> "成功: " + data;
    case Failure<T>(var error) -> "失败: " + error;
    case Loading<T>() -> "加载中...";
    // 编译器强制要求覆盖所有 3 种情况！
};
```

### 实际应用场景

```java
// AI API 响应类型
public sealed interface ChatResponse
    permits TextResponse, ToolCallResponse, ErrorResponse {}

public record TextResponse(String content) implements ChatResponse {}
public record ToolCallResponse(String toolName, Map<String,Object> args) implements ChatResponse {}
public record ErrorResponse(int code, String message) implements ChatResponse {}

// 处理时编译器保证穷举
String result = switch (response) {
    case TextResponse(var text) -> text;
    case ToolCallResponse(var name, var args) -> "Calling " + name + "...";
    case ErrorResponse(var code, var msg) -> "Error " + code + ": " + msg;
};
```

## 3. Record Pattern 解构

```java
// Java 21: 在 instanceof 和 switch 中解构 Record

// instanceof 解构
if (obj instanceof UserDTO(Long id, String name, String email)) {
    System.out.println(name + " <" + email + ">");
    // 直接拿到 id、name、email，无需强转！
}

// switch 嵌套模式
Object shape = new Circle(new Point(0, 0), 5);
String desc = switch (shape) {
    case Circle(Point(var x, var y), var radius) ->
        "圆心(" + x + "," + y + "), 半径" + radius;
    case Rectangle(Point(var x, var y), var w, var h) ->
        "矩形(" + x + "," + y + ") " + w + "×" + h;
    default -> "未知形状";
};
```

## 核心要点回顾

- Record = 不可变 DTO 的一行定义（消灭 60 行样板代码）
- 密封类 = 受控的类型继承（编译器保证 switch 穷举）
- Record Pattern = instanceof/switch 中直接解构（Java 21）
- AI 开发高频场景：API 响应类型（成功/失败/加载）→ 密封接口
- 数据传输对象（DTO/VO）→ 全部用 Record

## 参考资料

1. JEP 395: Records
2. JEP 409: Sealed Classes
3. JEP 440: Record Patterns
