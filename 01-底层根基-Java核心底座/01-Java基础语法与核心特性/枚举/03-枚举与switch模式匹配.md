# 03 枚举与 switch 模式匹配

> switch 是枚举最常见的消费方式——从 JDK 5 的常量比较，到 JDK 21（JEP 441）的模式匹配与穷尽性检查，枚举 switch 的写法与安全性发生了代际变化

---

## 📚 目录

1. [switch 演进时间线](#1-switch-演进时间线)
2. [传统枚举 switch（JDK 5-13）](#2-传统枚举-switchjdk-5-13)
3. [switch 表达式（JDK 14+）：箭头语法](#3-switch-表达式jdk-14箭头语法)
4. [JDK 21 模式匹配：穷尽性与守卫](#4-jdk-21-模式匹配穷尽性与守卫)
5. [枚举 switch 的最佳实践](#5-枚举-switch-的最佳实践)
6. [JDK 25 预览：基本类型模式](#6-jdk-25-预览基本类型模式)

---

## 1. switch 演进时间线

| 版本 | 变化 | 对枚举的意义 |
|:----:|------|------------|
| JDK 5 | switch 支持枚举 | 枚举分支成为可能（与 int/String 并列） |
| JDK 14 | **switch 表达式**（箭头语法/返回值） | 枚举分支更简洁、可作表达式 |
| JDK 16 | instanceof 模式匹配（JEP 394） | 类型判断 + 绑定 |
| **JDK 21** | **模式匹配 for switch（JEP 441）** | **穷尽性检查、守卫（when）、null 处理** |
| JDK 22 | 未命名模式 `_`（JEP 456） | default 的现代替代 |
| **JDK 25** | 基本类型模式（JEP 507 预览） | switch 支持更多选择器类型 |

> 🎯 **核心要点**：枚举 switch 的演进 = "**能用（5）→ 好用（14）→ 安全（21）**"——JDK 21 起编译器能验证"所有枚举常量都被处理"（穷尽性），漏分支从"运行时静默"变为"编译期报错"。

---

## 2. 传统枚举 switch（JDK 5-13）

```java
// 传统写法：冒号 + break（JDK 5-13 的标准）
public String describe(Day day) {
    String result;
    switch (day) {                     // 选择器：枚举类型
        case MONDAY:                   // case 标签：不带枚举名前缀！
            result = "周一";
            break;
        case FRIDAY:
            result = "周五";
            break;
        default:
            result = "其他";
    }
    return result;
}
```

**传统写法的三个问题**：

| 问题 | 说明 |
|------|------|
| **漏分支静默** | 忘写某常量 → 走 default（或漏 default 返回 null）——运行时才暴露 |
| break 易漏 | 漏 break → 贯穿（fall-through）→ 逻辑错误 |
| 语句而非表达式 | 不能直接赋值（需临时变量） |

**case 语法注意**：**case 后直接写常量名**（`case MONDAY`），不能写 `case Day.MONDAY`（JDK 21 前）；而 JDK 21+ 模式匹配支持限定常量（见第 4 节）。

> 🎯 **核心要点**：传统 switch 的枚举支持"能用但不安全"——**漏分支与 fall-through 两个经典坑**是 JDK 14/21 改进的动机。

---

## 3. switch 表达式（JDK 14+）：箭头语法

```java
// JDK 14+：箭头语法 + 表达式（可直接赋值）
public String describe(Day day) {
    return switch (day) {
        case MONDAY -> "周一";        // 箭头：无贯穿，无需 break
        case FRIDAY -> "周五";
        case SATURDAY, SUNDAY -> "周末";   // 多值合并
        default -> "其他";
    };
}
```

**箭头语法的改进**：

| 改进 | 说明 |
|------|------|
| 无贯穿 | 箭头分支自动结束（fall-through 坑消失） |
| 表达式化 | 直接返回/赋值（`return switch(...)`） |
| 多值合并 | `case A, B ->` 一个分支多常量 |
| 块分支 | `-> { ... yield 值; }` 支持多语句 |

```java
// 块分支 + yield（需要多语句时）
return switch (status) {
    case PENDING -> {
        log.debug("待支付");
        yield "等待用户支付";
    }
    case PAID -> "已支付";
    default -> "未知";
};
```

> 🎯 **核心要点**：JDK 14 让 switch "**从语句变表达式**"——箭头 + 返回值 + 无贯穿，枚举分支的可读性与安全性大幅提升；`yield` 用于块分支返回值。

---

## 4. JDK 21 模式匹配：穷尽性与守卫

**JDK 21（JEP 441）**对枚举 switch 的三个关键升级：

### 4.1 穷尽性检查（Exhaustiveness）

```java
// JDK 21+：枚举 switch 可以省略 default（编译器验证穷尽）
public String describe(Day day) {
    return switch (day) {
        case MONDAY -> "周一";
        case TUESDAY -> "周二";
        case WEDNESDAY -> "周三";
        case THURSDAY -> "周四";
        case FRIDAY -> "周五";
        case SATURDAY -> "周六";
        case SUNDAY -> "周日";
        // 无 default！漏一个常量 → 编译错误
    };
}
// 新增枚举常量 → 所有 switch 编译报错 → 强制处理（安全检查）
```

### 4.2 守卫子句（Guard / when）

```java
// 守卫：条件过滤 + 模式绑定
public String describeWork(Day day) {
    return switch (day) {
        case Day d when d == Day.SATURDAY || d == Day.SUNDAY -> "周末";  // 限定常量 + 守卫
        case Day d -> "工作日: " + d;
    };
}
```

### 4.3 null 友好

```java
// 传统 switch：day 为 null 抛 NPE；JDK 21+：
return switch (day) {
    case null -> "未知";          // 显式 null 分支（不再 NPE）
    case Day.SATURDAY, Day.SUNDAY -> "周末";
    default -> "工作日";
};
```

> ⚠️ **守卫规则（重要）**：**无守卫的 case 会支配（dominate）带守卫的 case**——`case Day d`（无守卫）必须放在 `case Day d when ...`（带守卫）**之后**，否则带守卫分支不可达、编译报错；且 **`when` 守卫不能用于纯常量标签**（如 `case MONDAY when ...` 不合法）。

> 🎯 **核心要点**：JDK 21 的三升级 = "**穷尽（漏分支编译错）+ 守卫（条件分派）+ null（显式处理）**"——**枚举 switch 从"手动安全"变"编译器保证安全"**；守卫排序规则（无守卫在后）是易错点。

---

## 5. 枚举 switch 的最佳实践

**2026 推荐的枚举 switch 写法**：

```java
// ① 穷尽优先：能省略 default 就省略（编译器兜底）
// ② 分支逻辑简单：箭头 + 表达式
public String statusDesc(OrderStatus s) {
    return switch (s) {
        case PENDING -> "待支付";
        case PAID -> "已支付";
        case SHIPPED -> "已发货";
        case COMPLETED -> "已完成";
        case CANCELLED -> "已取消";
    };
}

// ③ 分支逻辑复杂：块 + yield
// ④ 需要兜底（外部数据）：default 显式返回（不要抛异常吞掉）
public OrderStatus fromCode(int code) {
    return switch (code) {
        case 0 -> OrderStatus.PENDING;
        case 1 -> OrderStatus.PAID;
        case 2 -> OrderStatus.SHIPPED;
        default -> throw new IllegalArgumentException("未知状态码: " + code);
    };
}
```

**"什么时候用 switch、什么时候用成员类体"**（与 02 模块呼应）：

```text
行为在枚举内部 → 成员类体/抽象方法（内聚）
行为在调用方 → switch 表达式（穷尽性保证）
外部转换（code ↔ 枚举）→ switch（无 default 或显式 default）
→ 原则：行为归属决定写法——归属枚举用方法，归属调用方用 switch
```

> 🎯 **核心要点**：最佳实践 = "**穷尽优先（省略 default）+ 简单用箭头/复杂用 yield + 外部转换显式兜底**"——**"行为归属决定写法"**是枚举 switch 与成员类体的选择标准。

---

## 6. JDK 25 预览：基本类型模式

**JDK 25（JEP 507，第五次预览）**：switch 支持基本类型作为选择器与模式：

```java
// JDK 25 预览（--enable-preview）：基本类型模式替代部分 default
static String grade(Number n) {
    return switch (n) {
        case int i when i >= 90 -> "A";        // int 模式 + 守卫
        case int i when i >= 75 -> "B";
        case int i when i >= 60 -> "C";
        case double d when d >= 59.5 -> "C (rounded)";
        default -> "D/F";
    };
}
```

**对枚举开发者的意义**：

```text
① 枚举 switch 的选择器可以是"任意类型"（不仅枚举/int/String）
② 模式 + 守卫让"值域分派"统一（数值阈值、类型分派）
③ 仍在预览（需 --enable-preview）——生产暂缓，了解即可

与枚举的协同场景：
  状态码 int → switch 模式（不转枚举也可安全分派）
  但类型安全仍推荐：int → 枚举（fromCode）→ 枚举 switch
```

> 🎯 **核心要点**：JDK 25 预览扩展了 switch 的"选择器类型"——**方向是"switch 成为统一分派引擎"**；生产实践不变：**枚举仍是状态建模首选**（类型安全），基本类型模式是"原始值分派"的补充。

---

**下一模块**：[04-EnumSet与EnumMap](./04-EnumSet与EnumMap.md) / **返回总览**：[00-枚举知识体系总览](./00-枚举知识体系总览.md)
