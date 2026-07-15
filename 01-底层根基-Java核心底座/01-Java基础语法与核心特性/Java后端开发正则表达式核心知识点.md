# Java 后端开发正则表达式核心知识点

> **定位**：正则表达式（Regex）是 Java 后端处理字符串的核心工具，广泛应用于数据校验、字符串匹配、替换、提取等场景。核心是通过特定语法规则定义"字符串模式"，结合 Java 的 `Pattern` 和 `Matcher` API 高效完成字符串操作。

---

## 目录

1. [基础语法](#1-基础语法)
2. [Java 核心 API](#2-java-核心-api)
3. [实战示例](#3-实战示例)
4. [性能优化与避坑](#4-性能优化与避坑)
5. [常用正则模板](#5-常用正则模板)

---

## 1. 基础语法

> 正则语法由**普通字符**和**元字符**组成，元字符是正则的核心。

### 1.1 普通字符与转义字符

| 类型 | 说明 | 示例 |
|------|------|------|
| 普通字符 | 无特殊含义，直接匹配自身 | `abc` 匹配字符串 `"abc"` |
| 转义字符 | 元字符前加 `\`（Java 中需写 `\\`） | `\\.` 匹配小数点，`\\d` 匹配数字 |

**Java 中双反斜杠规则**：

```text
正则写法       Java 字符串写法       含义
\d        →    "\\d"            匹配数字
\.        →    "\\."            匹配小数点
\w        →    "\\w"            匹配字母/数字/下划线 [a-zA-Z0-9_]
\s        →    "\\s"            匹配空白字符
```

### 1.2 量词（控制匹配次数）

| 量词 | 含义 | 示例 |
|------|------|------|
| `*` | 0 次或多次（贪婪） | `\\d*` 匹配任意长度数字 |
| `+` | 1 次或多次 | `\\S+` 匹配非空字符串 |
| `?` | 0 次或 1 次；加在量词后变非贪婪 | `abc?` 匹配 `"ab"` 或 `"abc"` |
| `{n}` | 精确 n 次 | `\\d{11}` 校验 11 位手机号 |
| `{n,}` | n 次及以上 | `\\w{6,}` 密码至少 6 位 |
| `{n,m}` | n 到 m 次 | `\\w{6,18}` 密码 6-18 位 |

**贪婪 vs 非贪婪**：

| 模式 | 写法 | 行为 | 示例匹配 `"<div>content</div>"` |
|------|------|------|------|
| 贪婪 | `<.*>` | 尽可能多匹配 | `<div>content</div>`（整串） |
| 非贪婪 | `<.*?>` | 尽可能少匹配 | `<div>`（第一个标签） |

### 1.3 字符类

| 写法 | 含义 | 等价写法 |
|------|------|----------|
| `[abc]` | 匹配 a、b、c 中任意一个 | — |
| `[a-z]` | 匹配任意小写字母 | — |
| `[A-Z]` | 匹配任意大写字母 | — |
| `[a-zA-Z]` | 匹配任意字母 | — |
| `[0-9]` | 匹配任意数字 | `\\d` |
| `[a-zA-Z0-9_]` | 字母/数字/下划线 | `\\w` |
| `[^abc]` | **反向匹配**：除 a、b、c 之外的任意字符 | — |
| `[^\\s]` | 非空白字符 | `\\S` |

### 1.4 边界匹配

> ⚠️ **核心原则**：后端校验必须加 `^` 和 `$`，否则会出现部分匹配错误。

| 元字符 | 含义 | 错误示例 | 正确示例 |
|--------|------|----------|----------|
| `^` | 字符串开头 | `\\d{11}` 匹配 `"138001380001"` 的前 11 位 | `^\\d{11}$` 拒绝 12 位数字 |
| `$` | 字符串结尾 | 同上 | 同上 |
| `\\b` | 单词边界 | `java` 匹配 `"javac"` | `\\bjava\\b` 只匹配独立单词 `"java"` |

### 1.5 分组与逻辑运算

| 语法 | 含义 | 示例 |
|------|------|------|
| `( )` | 分组，将多个字符视为整体，可结合量词 | `(abc)+` 匹配 `"abc"` / `"abcabc"` |
| `|` | 或运算，匹配多个规则之一 | `(\\d{11})\|(.+@.+)` 匹配手机号或邮箱 |
| `(\\d{3})-(\\d{8})` | 分组提取 | 匹配 `"010-12345678"`，括号内为子分组 |

### 1.6 常用元字符速查

| 元字符 | 含义 | 元字符 | 含义 |
|--------|------|--------|------|
| `.` | 任意字符（默认不含换行） | `\\d` | 数字 `[0-9]` |
| `\\D` | 非数字 | `\\w` | 字母/数字/下划线 |
| `\\W` | 非单词字符 | `\\s` | 空白字符 |
| `\\S` | 非空白字符 | `\\n` | 换行符 |
| `\\t` | 制表符 | `^` / `$` | 开头 / 结尾 |

---

## 2. Java 核心 API

> 两个核心类：`java.util.regex.Pattern`（线程安全，可复用）和 `java.util.regex.Matcher`（非线程安全，每次创建新实例）。

### 2.1 Pattern 类

```java
// 编译正则（推荐 static final 复用，避免频繁编译）
private static final Pattern PHONE_PATTERN =
    Pattern.compile("^1[3-9]\\d{9}$");

// 获取匹配器
Matcher matcher = PHONE_PATTERN.matcher("13800138000");
```

**匹配模式（编译时可选）**：

| 常量 | 作用 | 适用场景 |
|------|------|----------|
| `Pattern.CASE_INSENSITIVE` | 忽略大小写 | 匹配邮箱时忽略字母大小写 |
| `Pattern.DOTALL` | `.` 匹配换行符 | 匹配多行文本（如日志解析） |
| `Pattern.MULTILINE` | `^`/`$` 匹配每行开头/结尾 | 多行文本逐行匹配 |

### 2.2 Matcher 类

| 方法 | 功能 | 实战场景 |
|------|------|----------|
| `matches()` | 整个字符串是否完全匹配 | 接口参数校验（手机号、邮箱） |
| `find()` | 查找符合正则的子串，可多次调用 | 日志解析（提取所有匹配项） |
| `group()` | 提取匹配到的子串；`group(n)` 提取第 n 个分组 | 提取固定电话区号和号码 |
| `replaceAll(str)` | 替换所有匹配项 | 数据脱敏（手机号中间 4 位） |
| `replaceFirst(str)` | 替换第一个匹配项 | 替换首个不符合规则的内容 |

### 2.3 Pattern vs String 方法

| 方式 | 性能 | 适用场景 |
|------|------|----------|
| `Pattern.compile().matcher()` | ✅ 高效，预编译复用 | 频繁校验、复杂匹配 |
| `String.matches(regex)` | ❌ 每次重新编译 | 一次性简单校验 |
| `String.contains()` / `startsWith()` | ✅ 最高效 | 简单子串判断，无需正则 |

---

## 3. 实战示例

### 3.1 接口参数校验

```java
// 提前编译，static final 复用
private static final Pattern PHONE_PATTERN =
    Pattern.compile("^1[3-9]\\d{9}$");

private static final Pattern EMAIL_PATTERN =
    Pattern.compile("^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$");

// 校验手机号
public boolean checkPhone(String phone) {
    if (phone == null || phone.isEmpty()) {
        return false;           // ⚠️ 先判空，避免空指针
    }
    return PHONE_PATTERN.matcher(phone).matches();
}

// 校验邮箱
public boolean checkEmail(String email) {
    if (email == null || email.isEmpty()) {
        return false;
    }
    return EMAIL_PATTERN.matcher(email).matches();
}
```

### 3.2 数据脱敏

```java
// 手机号脱敏：138****8000
public String desensitizePhone(String phone) {
    if (phone == null || !PHONE_PATTERN.matcher(phone).matches()) {
        return phone;
    }
    return phone.replaceAll("(\\d{3})(\\d{4})(\\d{4})", "$1****$3");
}

// 身份证脱敏：110101****1234
public String desensitizeIdCard(String idCard) {
    Pattern pattern = Pattern.compile("^(\\d{6})(\\d{8})(\\d{4})$");
    Matcher matcher = pattern.matcher(idCard);
    if (matcher.matches()) {
        return matcher.replaceAll("$1********$3");
    }
    return idCard;
}
```

### 3.3 日志解析

```java
// 日志格式：2026-03-20 10:00:00 INFO [http-nio-8080] - GET /api/user/list?page=1
private static final Pattern LOG_PATTERN =
    Pattern.compile("(GET|POST|PUT|DELETE)\\s+(/api/\\S+)");

public void parseLog(String logContent) {
    Matcher matcher = LOG_PATTERN.matcher(logContent);
    while (matcher.find()) {
        String method = matcher.group(1);  // 请求方法
        String path = matcher.group(2);    // 请求路径
        System.out.println(method + " " + path);
    }
}
```

---

## 4. 性能优化与避坑

### 4.1 性能优化

| 技巧 | 做法 | 说明 |
|------|------|------|
| **预编译复用** | `static final Pattern` | 避免每次校验都编译，编译正则耗时高 |
| **慎用贪婪量词** | 必要时用 `*?`、`+?` 非贪婪 | 复杂字符串中贪婪匹配可能导致性能下降 |
| **简化正则** | 拆分复杂正则为多个简单正则 | 如密码复杂度校验拆分为"含大写""含数字"分别判断 |
| **优先 String 方法** | 简单子串判断用 `contains()`、`startsWith()` | 比正则更高效 |

### 4.2 常见避坑

| 问题 | 原因 | 正确做法 |
|------|------|----------|
| **部分匹配误判** | 忘记加 `^` 和 `$` | 校验必须加边界匹配 |
| **Java 转义错误** | 正则 `\d` 写了 `\d` 而非 `\\d` | Java 字符串中 `\` 必须写 `\\` |
| **空指针异常** | 校验前未判空 | `matcher()` 前先判断 `null` 和 `isEmpty()` |
| **过度依赖正则** | 简单操作用正则 | `contains()`、`startsWith()` 更高效 |

---

## 5. 常用正则模板

> 直接复用，适配常见后端校验场景。

| 场景 | 正则表达式 | 说明 |
|------|-----------|------|
| **手机号** | `^1[3-9]\\d{9}$` | 1 开头，第二位 3-9，共 11 位 |
| **邮箱** | `^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$` | 支持多级域名 |
| **密码**（8-18 位，含字母+数字） | `^(?=.*[a-zA-Z])(?=.*\\d)[a-zA-Z\\d]{8,18}$` | 正向预查确保同时包含 |
| **身份证**（18 位） | `^[1-9]\\d{5}(19\\|20)\\d{2}((0[1-9])\\|(1[0-2]))(([0-2][1-9])\\|10\\|20\\|30\\|31)\\d{3}[0-9Xx]$` | 含最后一位 X |
| **IPv4** | `^((25[0-5]\\|2[0-4]\\d\\|[01]?\\d\\d?)\\.){3}(25[0-5]\\|2[0-4]\\d\\|[01]?\\d\\d?)$` | — |

---

> 🎯 **核心总结**：正则 = **语法 + API + 实战**。先掌握元字符、量词、边界匹配，再熟练运用 `Pattern`（预编译复用）和 `Matcher`（matches/find/group/replaceAll），结合参数校验、数据脱敏、日志解析三大场景复用模板，同时规避转义错误、部分匹配、空指针三个高频坑。
