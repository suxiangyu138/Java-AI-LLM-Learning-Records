# 05 - Java 正则 API 深度实践

> **核心摘要**：Java 正则 API = Pattern（编译模式）+ Matcher（执行匹配）双层设计——Pattern 不可变线程安全可复用，Matcher 有状态单次使用。本文覆盖 API 全景、预编译实践、常用方法、标志位、线程安全与 String 便捷方法。

> **前置阅读**：[[01-正则基础：字符与元字符]]、[[02-正则进阶：分组引用与断言]]

---

## 📚 目录

1. [API 架构：Pattern 与 Matcher](#1-api-架构pattern-与-matcher)
2. [Pattern 预编译实践](#2-pattern-预编译实践)
3. [Matcher 核心方法](#3-matcher-核心方法)
4. [标志位大全](#4-标志位大全)
5. [String 便捷方法](#5-string-便捷方法)
6. [线程安全与性能](#6-线程安全与性能)
7. [API 陷阱清单](#7-api-陷阱清单)
8. [核心要点](#8-核心要点)

---

## 1. API 架构：Pattern 与 Matcher

> **背景**：Java 正则采用「编译-执行」分离设计——Pattern 一次编译，Matcher 多次执行。
> **目的**：编译开销一次支付，匹配执行高效复用。
> **适用范围**：所有 Java 正则场景。
> **前提假设**：理解两者的生命周期差异是正确使用的前提。

```text
Pattern 与 Matcher 的分工
┌─────────────────────────────────────────────┐
│ Pattern（编译后的模式）                       │
│ ├── 不可变（immutable）                      │
│ ├── 线程安全（可共享）                       │
│ ├── 一次编译，永久复用                       │
│ └── 工厂：Pattern.compile(regex, flags)      │
├─────────────────────────────────────────────┤
│ Matcher（一次匹配的状态机）                   │
│ ├── 有状态（记录当前位置/分组）               │
│ ├── 非线程安全（每次匹配 new 或 reset）       │
│ ├── 由 Pattern.matcher(input) 创建           │
│ └── 可复用：matcher.reset(newInput)          │
└─────────────────────────────────────────────┘
```

| 维度 | Pattern | Matcher |
|------|:---:|:---:|
| 可变性 | 不可变 | 有状态 |
| 线程安全 | ✅ 可共享 | ❌ 每次独立 |
| 生命周期 | 长（应用级） | 短（单次匹配） |
| 创建开销 | 高（编译） | 低（包装输入） |
| 复用方式 | 静态字段缓存 | reset() 换输入 |

---

## 2. Pattern 预编译实践

### 2.1 为什么不预编译会慢

```text
Pattern.compile 的开销
├── ① 解析正则语法（构建 AST）
├── ② 编译为内部匹配器结构
├── ③ 开销量级：微秒-毫秒级（复杂模式更大）
├── ④ 热循环中重复编译：10 万次 × 1ms = 100 秒浪费！
└── 结论：热路径必须预编译为常量
```

### 2.2 预编译最佳实践

```java
// ✅ 正确：静态常量预编译（一次编译全局复用）
public class RegexConstants {
    public static final Pattern EMAIL =
            Pattern.compile("[\\w.]+@[\\w.]+\\.[a-z]{2,}");
    public static final Pattern PHONE =
            Pattern.compile("1[3-9]\\d{9}");
    public static final Pattern DATE =
            Pattern.compile("(\\d{4})-(\\d{2})-(\\d{2})");
}

// ✅ 使用
if (RegexConstants.EMAIL.matcher(email).matches()) { ... }

// ❌ 错误：每次调用重新编译
public boolean isValidEmail(String email) {
    return Pattern.compile("[\\w.]+@[\\w.]+\\.[a-z]{2,}")  // ❌ 热循环编译
                  .matcher(email).matches();
}
```

### 2.3 动态正则的缓存（用户规则场景）

```java
// 用户/配置提供的动态正则 → 用缓存避免重复编译
public class PatternCache {
    private final Map<String, Pattern> cache = new ConcurrentHashMap<>();

    public Pattern get(String regex) {
        return cache.computeIfAbsent(regex, Pattern::compile);
        // ⚠️ 注意：不可信正则需沙箱化（见 04 篇）
    }
}
```

---

## 3. Matcher 核心方法

### 3.1 方法全景

| 方法 | 语义 | 关键差异 |
|------|------|---------|
| `matches()` | 全输入匹配 | 等价 `^模式$` |
| `find()` | 子串查找（迭代） | 从上次位置继续 |
| `lookingAt()` | 前缀匹配 | 从开头但不要求到结尾 |
| `group()` / `group(n)` | 提取分组 | group(0) = 整体 |
| `start()` / `end()` | 匹配位置 | 用于切割 |
| `replaceAll()` | 全部替换 | 支持 $1 引用 |
| `replaceFirst()` | 首个替换 | 同上 |
| `appendReplacement()` | 流式替换 | 复杂替换场景 |
| `reset()` | 重置匹配器 | 换输入复用 |

### 3.2 matches vs find（最易混淆）

```java
// matches()：整个输入必须完全匹配
Pattern.compile("\\d+").matcher("123").matches();      // ✅ true
Pattern.compile("\\d+").matcher("abc123").matches();  // ❌ false（不是全部）

// find()：只要包含子串就匹配（隐式循环！）
Pattern.compile("\\d+").matcher("abc123").find();     // ✅ true（找到 123）

// lookingAt()：从开头匹配前缀（不要求到结尾）
Pattern.compile("\\d+").matcher("123abc").lookingAt(); // ✅ true

// ⚠️ 业务选择：校验用 matches（全匹配），提取用 find（子串）
```

### 3.3 find 的迭代陷阱（性能与逻辑）

```java
// find() 的迭代语义：从上次匹配结束位置继续
Matcher m = Pattern.compile("\\d+").matcher("a1b22c333");
while (m.find()) {
    System.out.println(m.group() + " @ " + m.start());
    // 1 @ 1
    // 22 @ 3
    // 333 @ 6
}

// ⚠️ 陷阱 1：忘写循环 → 只匹配第一次
// ⚠️ 陷阱 2：find() 隐式 O(N) 扫描 → 配合复杂模式可能 O(N²)
//   （JEP 8260688 指出：单纯占有量词不足以解决）
// ⚠️ 陷阱 3：循环中修改输入 → 位置错乱
```

### 3.4 分组提取的完整模式

```java
public class LogParser {
    private static final Pattern ORDER_LOG = Pattern.compile(
        "(?<ts>\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}) " +
        "(?<level>INFO|WARN|ERROR) " +
        "\\[(?<service>[^]]+)\\] orderId=(?<orderId>\\w+)"
    );

    public record OrderLog(String ts, String level, String service, String orderId) {}

    public Optional<OrderLog> parse(String line) {
        Matcher m = ORDER_LOG.matcher(line);
        if (!m.matches()) return Optional.empty();
        return Optional.of(new OrderLog(
            m.group("ts"), m.group("level"),
            m.group("service"), m.group("orderId")));
    }
}
```

---

## 4. 标志位大全

### 4.1 编译标志

| 标志 | 常量 | 作用 |
|------|:---:|------|
| **DOTALL** | `Pattern.DOTALL` | `.` 匹配换行（跨行匹配） |
| **MULTILINE** | `Pattern.MULTILINE` | `^` `$` 按行匹配 |
| **CASE_INSENSITIVE** | `Pattern.CASE_INSENSITIVE` | 忽略大小写（仅 ASCII，中文不受影响） |
| **UNICODE_CASE** | `Pattern.UNICODE_CASE` | 配合忽略大小写（Unicode 级） |
| **COMMENTS** | `Pattern.COMMENTS` | 忽略空白和 # 注释（可读性） |
| **LITERAL** | `Pattern.LITERAL` | 按字面匹配（不解析元字符） |
| **UNIX_LINES** | `Pattern.UNIX_LINES` | 只认 \n 为行终止符 |
| **CANON_EQ** | `Pattern.CANON_EQ` | 规范等价匹配（少用） |

### 4.2 标志使用示例

```java
// 编译标志（Pattern.compile 第二参数）
Pattern p1 = Pattern.compile("^error", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
Pattern p2 = Pattern.compile("<.*>", Pattern.DOTALL);          // 跨行
Pattern p3 = Pattern.compile("(a) # 注释", Pattern.COMMENTS);  // 可读性

// 内联标志（模式内，局部作用域）
Pattern p4 = Pattern.compile("(?i)error");      // 忽略大小写
Pattern p5 = Pattern.compile("(?i)error(?-i)WARN");  // 局部开关
Pattern p6 = Pattern.compile("(?s)<.*>");       // 内联 DOTALL

// ⚠️ 区别：编译标志全局生效；内联标志可局部控制
```

### 4.3 标志的坑

```text
标志陷阱
├── ① CASE_INSENSITIVE 不覆盖中文大小写（中文无大小写）
├── ② 需要 Unicode 大小写（如德文 ß→SS）：用 UNICODE_CASE
├── ③ MULTILINE 改变 ^ $ 语义：^ 匹配每行行首
├── ④ 想「整个输入首尾」用 \A \z（不受 MULTILINE 影响）
└── ⑤ LITERAL 模式性能极佳（无解析）——纯字面匹配用它
```

---

## 5. String 便捷方法

### 5.1 三个便捷方法

```java
// String 内置正则方法（内部创建 Pattern，热循环注意！）
"abc123".matches("\\w+\\d+");              // 全匹配校验
"a,b;c".split("[,;]");                     // 按正则分割
"123-456".replaceAll("(\\d{3})-(\\d{3})", "$1$2");  // 正则替换
"abc".replaceFirst("a", "x");              // 首个替换

// ⚠️ 性能注意：这三个方法每次调用内部都编译 Pattern！
// 热循环中应改用预编译 Pattern（见第 2 节）
```

### 5.2 replace vs replaceAll（易混）

```java
// replace(CharSequence, CharSequence)：字面替换（不是正则！）
"a.b.c".replace(".", "/");        // → a/b/c（字面点被替换）

// replaceAll(String, String)：正则替换
"a.b.c".replaceAll("\\.", "/");   // → a/b/c（正则需转义）

// ⚠️ 关键区别：想替换字面字符用 replace（免转义、更快）
// ⚠️ 陷阱：replaceAll 的第一个参数是正则——"." 会匹配所有字符！
"a.b.c".replaceAll(".", "/");     // → /////（灾难！每个字符都被替换）
```

### 5.3 split 的细节

```java
// split 的边界行为（易错点）
"a,b,c".split(",");               // [a, b, c]
"a,,c".split(",");                // [a, , c]（空元素保留？）
"a,b,".split(",");                // [a, b]（⚠️ 尾部分隔符被丢弃！）
"a,b,".split(",", -1);            // [a, b, ]（-1 保留所有空元素）

// 正则特殊字符作为分隔符
"a.b.c".split("\\.");             // 正则转义（[a, b, c]）
"a|b|c".split("\\|");             // 竖线转义
"a$b".split("\\$");               // 美元转义
// ⚠️ 简单方案：split(Pattern.quote(".")) 按字面分割
```

---

## 6. 线程安全与性能

### 6.1 线程安全模型

```text
Java 正则线程安全
├── Pattern：不可变 → 线程安全 → 可静态共享 ✅
├── Matcher：有状态 → 非线程安全 → 每次 new/reset ⚠️
├── 错误用法：静态共享 Matcher（多线程并发调用）→ 数据错乱
└── 正确用法：静态共享 Pattern + 每线程独立 Matcher

// ✅ 正确
public class Validator {
    private static final Pattern EMAIL = Pattern.compile("...");  // 共享安全
    public boolean check(String s) {
        return EMAIL.matcher(s).matches();   // Matcher 每次新建（安全）
    }
}

// ❌ 错误
private static Matcher SHARED_MATCHER = ...;   // 多线程并发 → 错乱
```

### 6.2 性能清单

```text
Java 正则性能优化清单（按收益排序）
├── ① 预编译 Pattern（静态常量）——最大收益
├── ② 避免 find() 重复扫描长输入（一次循环收集完）
├── ③ 长输入先用简单检查（length/startsWith）短路
├── ④ 简单校验优先 String 方法（startsWith/contains——无正则开销）
├── ⑤ 危险模式改写（见 03/04 篇）
└── ⑥ 热路径测量：JMH 基准（Pattern 编译 vs 匹配耗时）
```

### 6.3 性能对比示例（JMH 思路）

```java
// 简单校验的替代方案（正则 vs 原生方法）
boolean r1 = "abc123".matches("\\d+");          // 正则：编译+匹配
boolean r2 = Character.isDigit('1');            // 原生：零开销
// 单字符判断用原生 API；复杂模式才用正则
```

---

## 7. API 陷阱清单

| # | 陷阱 | 表现 | 解法 |
|---|------|------|------|
| 1 | **matches 当 find 用** | 部分匹配误判为全匹配 | 明确语义（校验用 matches） |
| 2 | **replaceAll 字面替换** | `.` 匹配一切 → 灾难 | 字面替换用 replace |
| 3 | **split 丢尾部空串** | "a,b," → [a,b] | split(",", -1) |
| 4 | **共享 Matcher 并发** | 数据错乱 | Pattern 共享 + Matcher 独立 |
| 5 | **热循环内编译** | 性能下降 100 倍 | 静态预编译 |
| 6 | **find 死循环** | 零宽匹配无限循环 | 正则避免零宽 + 循环上限 |
| 7 | **group 越界** | 组不存在抛异常 | 先 matches() 再取 group |
| 8 | **内联标志意外生效** | (?i) 影响后续 | 局部关闭 (?-i) |

```java
// 陷阱 6 示例：零宽匹配死循环
Matcher m = Pattern.compile("(?=a)").matcher("aaa");
while (m.find()) { ... }   // ⚠️ 零宽匹配：位置不前进 → 死循环！
// 解法：循环内检查 m.end() == m.start() 时手动前进

// 陷阱 7 示例：group 越界
Matcher m = Pattern.compile("(\\d+)").matcher("123");
// m.group(2)  → 抛 IndexOutOfBoundsException（只有 1 个组）
```

---

## 8. 核心要点

> 🎯 **核心要点**：
> 1. 双层 API：Pattern（不可变线程安全共享）+ Matcher（有状态每次新建）——**静态共享 Pattern 是标准实践**
> 2. 预编译是最大性能收益：热循环重复编译 = 100 倍性能损失
> 3. matches（全匹配校验）vs find（子串提取）vs lookingAt（前缀）语义要分清
> 4. String 便捷方法每次内部编译——热循环用预编译 Pattern；`replace` 字面 vs `replaceAll` 正则要分清
> 5. split 默认丢尾部空串（用 -1 保留）；共享 Matcher 并发是线程安全红线

---

**下一模块**：[06-实战场景与模式库](06-实战场景与模式库.md) | **返回总览**：[00-正则表达式知识体系总览](00-正则表达式知识体系总览.md)
