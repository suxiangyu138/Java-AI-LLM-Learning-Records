# 11 - String与字符串处理
> 定位：深入理解Java String的不可变性、字符串常量池机制、StringBuilder/StringBuffer的性能差异，掌握字符串高效处理技巧

## 目录
1. [String的不可变性](#1-string的不可变性)
2. [字符串常量池（String Pool）](#2-字符串常量池string-pool)
3. [String的创建方式对比](#3-string的创建方式对比)
4. [intern()方法深入](#4-intern方法深入)
5. [String常用方法大全](#5-string常用方法大全)
6. [StringBuilder vs StringBuffer](#6-stringbuilder-vs-stringbuffer)
7. [字符串拼接原理与性能](#7-字符串拼接原理与性能)
8. [字符串编码与字符集](#8-字符串编码与字符集)
9. [Java 13+ Text Blocks（文本块）](#9-java-13-text-blocks文本块)
10. [String.format与MessageFormat](#10-stringformat与messageformat)
11. [字符串处理最佳实践](#11-字符串处理最佳实践)
12. [面试高频考点](#12-面试高频考点)

---

## 1. String的不可变性

### 1.1 不可变性的含义与实现

String 是不可变对象——一旦创建，值不可改变。所有"修改"操作都返回新 String 对象。

```java
String original = "Hello";
String modified = original.concat(" World");
System.out.println(original);   // "Hello"（不变）
System.out.println(modified);   // "Hello World"（新对象）
```

底层实现：

```java
// JDK 8：final char[]
public final class String { private final char value[]; private int hash; }
// JDK 9+：优化为 byte[] + coder（LATIN1/UTF16），英文节省 50% 内存
public final class String { private final byte[] value; private final byte coder; }
```

设计要点：`final` 类、`private final` 数组、不暴露 setter、构造器不泄露 `this`。

### 1.2 不可变性的好处与代价

| 好处 | 说明 |
|------|------|
| 线程安全 | 无需同步，天然安全 |
| 字符串池共享 | 多个引用指向同一对象 |
| 哈希缓存 | hashCode 只计算一次，HashMap Key 性能好 |
| 安全性 | 类路径、网络参数不可篡改 |

> ⚠️ 代价：大量操作创建临时对象，增加 GC 压力。循环中必须用 StringBuilder。

```java
// 错误
String s = ""; for (int i = 0; i < 10000; i++) { s += i; }  // O(n^2)
// 正确
StringBuilder sb = new StringBuilder(100000);
for (int i = 0; i < 10000; i++) { sb.append(i); }
```

---

## 2. 字符串常量池（String Pool）

### 2.1 基本概念

String Pool 是 JVM 为字符串字面量提供的缓存机制。

```java
String s1 = "Hello";
String s2 = "Hello";
System.out.println(s1 == s2);       // true（同一池对象）
String s3 = new String("Hello");
System.out.println(s1 == s3);       // false（池 vs 堆）
```

### 2.2 String Pool 演进（JDK 6 vs JDK 7+）

```
JDK 6:
+---------------------------+
| 方法区 (PermGen)          |
| String Pool（存实际对象） |
+---------------------------+
| 堆 (Heap)                 |
+---------------------------+

JDK 7+:
+---------------------------+
| 堆 (Heap)                 |
| String Pool（存引用）     |
| String 对象本身在堆中     |
+---------------------------+
```

| 对比 | JDK 6 | JDK 7+ |
|------|-------|--------|
| 位置 | 方法区（PermGen） | 堆中 |
| 存储 | 实际对象 | 引用 |
| GC | 不回收 | 可回收 |
| OOM | 常见（大量 intern） | 极少 |

### 2.3 常量折叠

```java
String s1 = "Hello" + " " + "World";   // 编译为 "Hello World"
String s2 = "Hello World";
System.out.println(s1 == s2);  // true

final String f = "Hello";
String s3 = f + " World";              // final 也折叠
System.out.println(s2 == s3);  // true

String v = "Hello";
String s4 = v + " World";              // 运行时拼接
System.out.println(s2 == s4);  // false
```

---

## 3. String的创建方式对比

| 方式 | 代码 | 对象数 | 入池 |
|------|------|--------|------|
| 字面量 | `"abc"` | 0 或 1 | 是 |
| new | `new String("abc")` | 1 或 2 | 否 |
| char[] | `new String(chars)` | 1 | 否 |

```java
String s1 = "abc", s2 = "abc";
System.out.println(s1 == s2); // true

String s3 = new String("abc");
System.out.println(s1 == s3); // false
```

`new String("abc")` 对象个数分析：
- 首次出现 `"abc"` 字面量：池中创建 1 个 + 堆上创建 1 个 = **2 个**
- 池中已有 `"abc"`：只在堆上创建 1 个

---

## 4. intern()方法深入

### 4.1 基本原理

```java
String s1 = new String("Hello");
String s2 = s1.intern();  // 入池，返回池中引用
String s3 = "Hello";
System.out.println(s2 == s3); // true
System.out.println(s1 == s3); // false
```

### 4.2 JDK 7+ 行为差异

```java
// JDK 6：intern 在 PermGen 复制对象
// JDK 7+：intern 在堆池中存引用

String s1 = new String("a") + new String("b");  // "ab"（堆对象）
s1.intern();                                      // JDK 7+：存引用
String s2 = "ab";
System.out.println(s1 == s2);  // true（JDK 7+）/ false（JDK 6）

// 顺序反转
String s3 = "cd";                                  // 先入池
String s4 = new String("c") + new String("d");
s4.intern();                                      // 池中已有 → 返回池对象
System.out.println(s3 == s4);  // false
```

> 💡 业务代码很少需要手动 intern()。大量重复运行时字符串（如 JSON key）才考虑，但需监控池大小。

---

## 5. String常用方法大全

| 类别 | 方法 |
|------|------|
| 长度/判空 | `length()` `isEmpty()` `isBlank()`(JDK 11+) |
| 字符 | `charAt(int)` `toCharArray()` |
| 比较 | `equals()` `equalsIgnoreCase()` `compareTo()` |
| 查找 | `indexOf()` `lastIndexOf()` `contains()` `startsWith()` `endsWith()` |
| 截取 | `substring(int, int)`（左闭右开） |
| 替换 | `replace()`（普通文本） `replaceAll(regex)` `replaceFirst(regex)` |
| 空白 | `trim()`（ASCII） `strip()`(JDK 11+, Unicode) |
| 分割 | `split(regex)` `split(regex, limit)` `join(delimiter, ...)` |
| 转换 | `valueOf(type)` `getBytes()` `toCharArray()` |
| 格式化 | `format(pattern, args)` `formatted(args)`(JDK 15+) |

```java
// 重要细节

// replace vs replaceAll
"a1b2c3".replaceAll("\\d", "-");   // "a-b-c-"（replaceAll 是正则）
"$5.00".replace("$", "USD");       // "USD5.00"（replace 普通文本，无需转义）

// trim vs strip（JDK 11+）
"　abc".trim();                   // "　abc"（trim 不能去全角空格！）
"　abc".strip();                  // "abc"

// split 正则转义
"192.168.1.1".split("\\.");       // ✅ 必须转义 . （它是正则通配符）
// split limit 控制次数
"a,b,c,d".split(",", 3);          // [a, b, c,d]

// join
String.join(" | ", "A", "B", "C"); // "A | B | C"

// isBlank（JDK 11+）
"  ".isBlank();                   // true
"".isEmpty();                     // true
```

---

## 6. StringBuilder vs StringBuffer

### 6.1 三兄弟对比

| 特性 | String | StringBuilder | StringBuffer |
|------|--------|---------------|--------------|
| 可变性 | 不可变 | 可变 | 可变 |
| 线程安全 | 安全（不可变） | 不安全 | 安全（synchronized） |
| 性能 | 最慢 | 最快 | 稍慢 |
| 推荐场景 | 固定字符串 | 单线程大量拼接 | 多线程大量拼接 |

### 6.2 核心 API

```java
StringBuilder sb = new StringBuilder(1024);          // 指定初始容量
sb.append("Hello").append(" ").append("World");      // 链式调用
sb.insert(5, " Java");                                // 插入
sb.delete(5, 10);                                     // 删除 [5,10)
sb.replace(6, 11, "Java");                            // 替换
sb.reverse();                                         // 反转
String result = sb.toString();

StringBuilder cap = new StringBuilder();
System.out.println(cap.capacity());   // 16（默认容量）
```

### 6.3 扩容机制

```
初始容量：16
扩容系数：new = old * 2 + 2
最大容量：Integer.MAX_VALUE - 8

最佳实践：预估字符串长度，指定初始容量避免数组复制
```

### 6.4 性能对比（100,000 次拼接）

| 方式 | 相对耗时 |
|------|---------|
| String + | ~100x（O(n^2)） |
| StringBuilder | 1x |
| StringBuffer | ~1.7x |

---

## 7. 字符串拼接原理与性能

### 7.1 `+` 运算符底层

```java
// 纯字面量 → 常量折叠：编译为 "Hello World"
String s = "Hello" + " " + "World";

// 含变量 → 编译为 StringBuilder
String c = a + " " + b;
// 等效：new StringBuilder().append(a).append(" ").append(b).toString()
```

> ⚠️ 循环中的 `+` 每次新建 StringBuilder 和 String 对象，不会复用，性能 O(n^2)。

```java
// 反编译后等价于：
String s = "";
for (int i = 0; i < 100; i++) {
    String tmp = new StringBuilder().append(s).append(i).toString();
    s = tmp;  // 100 次循环 = 100 个 StringBuilder + 100 个 String 临时对象
}
```

### 7.2 各种拼接方式选择

| 方式 | 性能 | 推荐场景 |
|------|------|---------|
| `str1 + str2` | 差 | 少量固定拼接 |
| `StringBuilder.append()` | 最优 | 大量拼接（单线程） |
| `StringBuffer.append()` | 良 | 大量拼接（多线程） |
| `String.join()` | 良 | 分隔符合并 |
| `Collectors.joining()` | 良 | Stream 处理 |

---

## 8. 字符串编码与字符集

### 8.1 不同编码字节数

```java
String str = "Java你好";
"UTF-8".length:    9     // 英文 1B + 中文 3B
"UTF-16".length:   12    // 含 BOM
"GBK".length:      7     // 英文 1B + 中文 2B
"字符数":            6
```

### 8.2 常用字符集

| 字符集 | 英文 | 中文 | 推荐场景 |
|-------|------|------|---------|
| UTF-8 | 1 B | 3 B | 通用标准（首选） |
| UTF-16 | 2-4 B | 2 B | Java 内部编码 |
| GBK | 1 B | 2 B | 遗留系统 |
| ISO-8859-1 | 1 B | 不可表示 | 纯英文 |

### 8.3 JDK 9 优化

```
JDK 9+：byte[] + coder（LATIN1=0 / UTF16=1）
- 纯 Latin-1 字符：每字符 1 byte，节省 50% 内存
- 含中文：UTF16 编码，每字符 2 bytes
```

> 💡 **最佳实践**：统一使用 UTF-8，避免编码问题。

---

## 9. Java 13+ Text Blocks（文本块）

JDK 15 正式推出，使用 `"""` 包裹多行字符串，无需转义和拼接。

```java
// 传统 JSON
String json = "{\n  \"name\": \"Java\"\n}";

// Text Block
String jsonTB = """
                {
                    "name": "Java"
                }
                """;

// SQL
String query = """
               SELECT u.id, u.name
               FROM users u
               WHERE u.status = 'ACTIVE'
               """;

// HTML + formatted()
String html = """
              <h1>Hello, %s!</h1>
              """.formatted("Java");
```

关键规则：
- 自动去除公共前导空白
- 保留换行符
- `\` 取消换行（拼接长行）
- `\s` 保留末尾空格

| 对比 | 传统 String | Text Block |
|------|------------|------------|
| 多行 | `\n` 拼接 | 天然支持 |
| 引号 | `\"` 转义 | 无需转义 |
| 可读性 | 差 | 好 |

---

## 10. String.format与MessageFormat

### 10.1 String.format()

```java
// 基本语法：%[index$][width][.precision]conversion
String.format("整数=%d, 浮点=%.2f, 字符串=%s", 100, 3.1415, "Hi");
// "整数=100, 浮点=3.14, 字符串=Hi"

// 宽度与对齐
String.format("|%10s|%-10s|", "right", "left");  // |     right|left      |

// 数字格式化
String.format("%,d", 1000000);     // "1,000,000"
String.format("%08d", 123);        // "00000123"
String.format("%+d", 100);         // "+100"

// printf 直接输出
System.out.printf("Hello %s!%n", "Java");
```

### 10.2 MessageFormat（多语言模板）

```java
import java.text.MessageFormat;

// 占位符 {index}
MessageFormat.format("用户 {0} 创建了 {1} 条记录。", "张三", 42);

// 类型样式：{index,type,style}
MessageFormat.format("日期: {0,date,full}, 金额: {1,number,currency}",
    new java.util.Date(), 12345.67);

// 复数：ChoiceFormat
String p = "文件{0,choice,0#没有文件|1#一个文件|1<{0}个文件}";
MessageFormat.format(p, 0);  // "没有文件"
MessageFormat.format(p, 5);  // "5个文件"
```

> **选型**：技术格式化用 `String.format()`，i18n 多语言模板用 `MessageFormat`。

---

## 11. 字符串处理最佳实践

### 11.1 比较与判空

```java
// 常量放前面避免 NPE
if ("ACTIVE".equals(input)) { }     // input=null → false
// if (input.equals("ACTIVE"))      // input=null → NPE

// 判空白
if (s != null && !s.isBlank()) { }  // JDK 11+
if (s != null && !s.isEmpty()) { }  // JDK 8
```

### 11.2 敏感信息处理

```java
// 用 char[] 而非 String，用后立即清空
char[] password = {'s', 'e', 'c'};
// 使用...
java.util.Arrays.fill(password, '\0');
// String 不可变，GC 前一直驻留内存，可被 dump 读取
```

### 11.3 构建决策树

```
少量固定拼接 → + 运算符
循环内大量拼接 → StringBuilder（预估容量）
多线程拼接 → StringBuffer
分隔符合并 → String.join() / StringJoiner / Collectors.joining()
技术格式化 → String.format() / formatted()
多语言模板 → MessageFormat
```

### 11.4 其他要点

```java
// 正则转义："." 需 "\\." 
"192.168.1.1".split("\\.");

// replace vs replaceAll（前者是普通文本，后者是正则）
str.replace("$", "USD");              // 普通文本
str.replaceAll("\\$", "USD");        // 需转义

// 预估 StringBuilder 容量
new StringBuilder(list.size() * 20);

// 统一 UTF-8 编码
```

---

## 12. 面试高频考点

### 12.1 核心问题

| 问题 | 要点 |
|------|------|
| String 为什么不可变？ | `final` 类 + `final` char[] + 不暴露修改方法 |
| `new String("abc")` 创建几个对象？ | 1 或 2（取决于池中是否有 "abc"） |
| String Pool JDK 6 vs 7+？ | PermGen → 堆；存对象 → 存引用 |
| `"a"+"b"+"c"` 创建几个对象？ | 1 个（常量折叠） |
| 循环中为什么不用 `+`？ | 每次新建 StringBuilder + String，O(n^2) |
| hashcode 为什么用 31？ | 素数减少碰撞，`31*i = (i<<5)-i` 计算快 |
| JDK 9 优化了什么？ | char[] → byte[] + coder，节省内存 |

### 12.2 经典代码题

```java
// 1. 池 vs 堆
String s1 = "abc", s2 = "abc", s3 = new String("abc");
System.out.println(s1 == s2);   // true
System.out.println(s1 == s3);   // false

// 2. 常量折叠
String s1 = "hello";
String s2 = "he" + "llo";               // 常量折叠
System.out.println(s1 == s2);           // true

String s3 = "he";
String s4 = s3 + "llo";                 // 运行时
System.out.println(s1 == s4);           // false

final String s5 = "he";
String s6 = s5 + "llo";
System.out.println(s1 == s6);           // true（final 也折叠）

// 3. intern（JDK 7+）
String s1 = new String("a") + new String("b");
s1.intern();
String s2 = "ab";
System.out.println(s1 == s2);           // true（JDK 7+）

// 4. StringBuilder 扩容
StringBuilder sb = new StringBuilder();  // 容量 16
sb.append("1234567890123456");           // 长度 16，刚好
System.out.println(sb.capacity());       // 16
sb.append("1");                          // 触发扩容
System.out.println(sb.capacity());       // 34 (16*2+2)
```

> 🎯 **一句话总结**：String 不可变是安全的基础，String Pool 是性能的保障，StringBuilder 是高效的武器，编码统一是兼容的前提。

---

**关联内容：**
- [01-Java基础语法与面向对象核心](./01-Java基础语法与面向对象核心.md) — equals/hashCode、值传递
- [03-常用API与集合框架](./03-常用API与集合框架.md) — 集合框架、Stream API