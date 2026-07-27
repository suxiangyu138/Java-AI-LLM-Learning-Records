# 09 - 模式匹配与 Switch 表达式

> 🎯 Java 14-21 的 Switch 进化是语言现代化的重要一步——从只能匹配基本类型到支持模式匹配、Record 解构、穷举检查。AI 应用中处理多种响应类型/事件类型，Switch 模式匹配是最优雅的方案

---

## 目录

1. [Switch 表达式](#1-switch-表达式)
2. [instanceof 模式匹配](#2-instanceof-模式匹配)
3. [Switch 模式匹配](#3-switch-模式匹配)

---

## 1. Switch 表达式

```java
// Java 8: 传统 switch（语句，容易漏 break）
String type;
switch (day) {
    case MONDAY:
    case FRIDAY: type = "工作日"; break;
    case SATURDAY:
    case SUNDAY: type = "周末"; break;
    default: type = "未知";
}

// Java 14+: Switch 表达式（箭头语法，无穿透）
String type = switch (day) {
    case MONDAY, FRIDAY -> "工作日";
    case SATURDAY, SUNDAY -> "周末";
    default -> "未知";
};

// yield: 复杂 case 块返回值
int hours = switch (day) {
    case MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY -> {
        System.out.println("工作日");
        yield 8;   // yield 代替 return（return 是从方法返回）
    }
    case SATURDAY, SUNDAY -> 0;
};
```

## 2. instanceof 模式匹配

```java
// Java 8: 先 instanceof 再强转（冗余）
if (obj instanceof String) {
    String s = (String) obj;
    System.out.println(s.length());
}

// Java 16+: instanceof 模式匹配（自动绑定变量）
if (obj instanceof String s) {
    System.out.println(s.length());  // 直接用 s！
}

// 复合条件
if (obj instanceof String s && s.length() > 5 && !s.isBlank()) {
    System.out.println(s.toUpperCase());
}

// AI 开发实战：处理多种 LLM 响应类型
public String handleResponse(Object response) {
    if (response instanceof String text) {
        return text;
    }
    if (response instanceof ToolCall(String name, Map<String,Object> args)) {
        return executeTool(name, args);
    }
    if (response instanceof ErrorResponse(int code, String msg)) {
        return "Error [" + code + "]: " + msg;
    }
    return "Unknown response type";
}
```

## 3. Switch 模式匹配 (Java 21)

```java
// 处理不同类型（不需要 instanceof 链了！）

// 传统方式
if (obj instanceof Integer i) {
    return "int: " + i;
} else if (obj instanceof String s) {
    return "string: " + s;
} else if (obj instanceof Long l) {
    return "long: " + l;
}
return "unknown";

// Switch 模式匹配
return switch (obj) {
    case Integer i -> "int: " + i;
    case String s  -> "string: " + s;
    case Long l    -> "long: " + l;
    case null      -> "null value";
    default        -> "unknown: " + obj.getClass().getSimpleName();
};

// 带守卫条件 (Guarded Patterns)
return switch (obj) {
    case String s when s.length() > 10 -> "长文本: " + s.substring(0, 10) + "...";
    case String s                      -> "短文本: " + s;
    case Integer i when i > 0          -> "正整数: " + i;
    case Integer i when i < 0          -> "负整数: " + i;
    case Integer i                     -> "零";
    default -> "其他";
};
```

### 穷举检查

```java
// 密封类 + Switch 模式匹配 = 编译器保证穷举 ✅
sealed interface Shape permits Circle, Rectangle, Triangle {}

double area = switch (shape) {
    case Circle(var r)    -> Math.PI * r * r;
    case Rectangle(var w, var h) -> w * h;
    case Triangle(var b, var h)  -> 0.5 * b * h;
    // 如果漏了一个 → 编译错误！ ← 这就是密封类的威力
};
// 注意：这里不需要 default！因为 Shape 只有 3 种子类
```

## 核心要点回顾

- Switch 表达式：箭头语法 + 无穿透 + `yield` 返回值
- instanceof 模式匹配：`obj instanceof String s` → 直接绑定变量
- Switch 模式匹配 (Java 21)：替代 instanceof 链 + Record 解构
- `when` 守卫条件：在不破坏穷举的前提下加额外筛选
- 密封类 + Switch = 编译期穷举检查（安全性 Max）

## 参考资料

1. JEP 441: Pattern Matching for switch
2. JEP 394: Pattern Matching for instanceof
