# 02 - Unicode 深入

> **核心摘要**：Unicode 的码点体系、17 个平面、代理对机制是国际化的底层知识——Java 的 char 是 16 位 UTF-16 单元，处理 Emoji 必须用 code point。本文深入 Unicode 结构、UTF-8/UTF-16 编码细节与编程实践。

> **前置阅读**：[[01-字符编码演进史]]

---

## 📚 目录

1. [码点与平面体系](#1-码点与平面体系)
2. [BMP 与补充平面](#2-bmp-与补充平面)
3. [代理对机制](#3-代理对机制)
4. [UTF-8 编码详解](#4-utf-8-编码详解)
5. [UTF-16 与 Java char](#5-utf-16-与-java-char)
6. [编程中的 code point 实践](#6-编程中的-code-point-实践)
7. [规范化与大小写](#7-规范化与大小写)
8. [核心要点](#8-核心要点)

---

## 1. 码点与平面体系

> **背景**：Unicode 用「码点」给每个字符唯一编号——从 U+0000 到 U+10FFFF。
> **目的**：理解码点体系，才能理解代理对、Emoji 处理等编程问题。
> **适用范围**：所有 Unicode 相关编程。
> **前提假设**：码点（编号）≠ 字节（存储）——中间隔着编码。

```text
码点范围
├── 总容量：U+0000 ~ U+10FFFF（约 111 万）
├── 已分配：14 万+ 字符（2026）
├── 表示法：U+XXXX（4 位十六进制）或 U+XXXXX（5 位）

码点分类（常用区间）
├── U+0000-U+007F：ASCII（128 个）
├── U+0080-U+00FF：Latin-1 补充
├── U+4E00-U+9FFF：CJK 统一汉字（常用区）
├── U+1F600-U+1F64F：Emoji（表情）
└── U+E000-U+F8FF：私有区（厂商自定义）
```

---

## 2. BMP 与补充平面

### 2.1 平面划分

> 🎯 **Unicode 分 17 个平面**——每个平面 65536 个码点：

```text
17 个平面（Plane）
├── 平面 0（U+0000-U+FFFF）：BMP 基本多文种平面
│   ├── 绝大多数常用字符（拉丁/中文/日文/韩文）
│   ├── 私用区、兼容字符
│   └── 可被 UTF-16 单单元表示（2 字节）
├── 平面 1（U+10000-U+1FFFF）：补充多文种平面（SMP）
│   ├── Emoji、古文字、数学符号
│   └── 需 UTF-16 代理对（4 字节）
├── 平面 2：补充表意文字（CJK 扩展 B+）
├── 平面 14：标签字符
├── 平面 15-16：私用区
└── 平面 3-13：未分配（预留）

BMP 的意义
├── ① 大多数编程场景只在 BMP 内（中文/英文/日文）
├── ② UTF-16 单单元 = BMP 内字符
├── ③ Emoji 是「跨出 BMP」的典型（代理对）
```

### 2.2 为什么 Emoji 是特殊问题

```text
Emoji 的特殊性（编程常见坑）
├── ① Emoji 在补充平面（U+1F600+）——BMP 外
├── ② Java char（16 位）无法直接表示 → 需代理对
├── ③ String.length() 对 Emoji 返回 2（不是 1！）
├── ④ String.substring 可能切开 Emoji（乱码/断裂）
├── ⑤ 组合 Emoji（👨👩👧 由多个码点组合）更复杂
└── ⑥ 处理原则：用 code point API（见第 6 节）
```

---

## 3. 代理对机制

### 3.1 什么是代理对

> **背景**：UTF-16 每个单元 16 位——只能表示 BMP（65536 个码点）；BMP 外的字符需要「两个 16 位单元」组合表示，这对单元称为**代理对（Surrogate Pair）**。

```text
代理对机制
├── 高位代理（High Surrogate）：U+D800-U+DBFF
├── 低位代理（Low Surrogate）：U+DC00-U+DFFF
├── 计算：码点 - 0x10000 → 拆成 10 位 + 10 位
│   ├── 高 10 位 + 0xD800 = 高位代理
│   └── 低 10 位 + 0xDC00 = 低位代理
├── 示例：😊 = U+1F60A
│   ├── 0x1F60A - 0x10000 = 0xF60A = 0000111101 1000001010
│   ├── 高位：0xD83D（0000111101 + 0xD800）
│   └── 低位：0xDE0A（1000001010 + 0xDC00）
└── Java 中 "😊" = 😊

代理区保留
├── U+D800-U+DFFF 不是有效字符（只用于代理）
├── 单独出现的高/低位代理 = 非法 Unicode
└── ⚠️ 安全校验：不可信输入需检查孤立代理（潜在攻击向量）
```

### 3.2 代理对的编程含义

```java
// Java 中代理对的实际影响
String emoji = "😊";              // 实际是 2 个 char
emoji.length();                   // 2（不是 1！）
emoji.charAt(0);                  // '\uD83D'（高位代理——单独无意义）
emoji.charAt(1);                  // '\uDE0A'（低位代理）

// 正确处理（code point API）
emoji.codePointCount(0, emoji.length());   // 1（真正的字符数）
emoji.codePointAt(0);                      // 0x1F60A（完整码点）

// 切割安全（不要用 substring 切字符！）
// emoji.substring(0, 1) → 得到孤立代理（乱码！）
```

---

## 4. UTF-8 编码详解

### 4.1 字节模式

```text
UTF-8 变长编码（1-4 字节）
┌──────────────┬─────────────────────────┬──────────┐
│ 码点范围       │ 字节模式                 │ 示例      │
├──────────────┼─────────────────────────┼──────────┤
│ U+0000-007F  │ 0xxxxxxx                │ A → 41   │
│ U+0080-07FF  │ 110xxxxx 10xxxxxx       │ é → C3 A9│
│ U+0800-FFFF  │ 1110xxxx 10xxxxxx 10xxxxxx │ 中 → E4 B8 AD│
│ U+10000-10FFFF│ 11110xxx 10xxxxxx ×3    │ 😊 → F0 9F 98 8A│
└──────────────┴─────────────────────────┴──────────┘

规则
├── ① ASCII（<0x80）：1 字节（完全兼容）
├── ② 多字节：首字节前缀标记长度（110/1110/11110）
├── ③ 后续字节：10 前缀（10xxxxxx）
└── ④ 自同步：从任意字节可判断「是首字节还是续字节」
```

### 4.2 UTF-8 的校验

```java
// Java 校验 UTF-8（2026 标准做法）
byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
// 校验器（Java 17+）
CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
        .onMalformedInput(CodingErrorAction.REPORT);
try {
    decoder.decode(ByteBuffer.wrap(bytes));
    // 合法 UTF-8
} catch (CharacterCodingException e) {
    // 非法 UTF-8（拒绝/清理）
}

// ⚠️ 安全要点：
// ① 非法 UTF-8 字节序列（如过长的编码/孤立代理）可能被用于攻击
// ② 用户输入必须先校验编码再处理（06 篇详述）
// ③ 替换策略：onMalformedInput(REPLACE) 用 U+FFFD 替换
```

---

## 5. UTF-16 与 Java char

### 5.1 Java 的字符模型

```text
Java 的字符模型（关键认知）
├── char = 16 位（一个 UTF-16 单元）
├── String 内部：char[]（Java 9+ Compact Strings 优化为 byte[]）
├── BMP 内字符：1 个 char（如 '中'）
├── BMP 外字符（Emoji）：2 个 char（代理对）
├── ⚠️ char 不等于「一个字符」——char 是「一个 UTF-16 单元」
└── 现代 Java 推荐：用 code point 处理字符（int 表示）

Java 默认编码演进
├── Java 1-17：平台默认（Windows GBK/Linux UTF-8——坑！）
├── Java 18+：默认 UTF-8（JEP 400）✅
└── 遗留系统：-Dfile.encoding=UTF-8 显式指定
```

### 5.2 字符串操作的正确姿势

```java
// ❌ 错误（char 级操作——Emoji 会坏）
String text = "你好😊";
text.length();                    // 4（2+2——😊 算 2 个）
text.substring(0, 3);             // "你好" + 半个😊（乱码！）

// ✅ 正确（code point 级操作）
String text = "你好😊";
text.codePointCount(0, text.length());  // 3（真正的字符数）

// 遍历字符（code point 安全）
text.codePoints().forEach(cp -> System.out.println(
    new String(Character.toChars(cp))));

// 切割（按码点）
int[] cps = text.codePoints().toArray();
String first = new String(cps, 0, 1);   // "你"（安全）

// 判断字符类型（code point 版本）
Character.isLetterOrDigit(cp);
Character.isEmoji(cp);    // Java 21+ 新增
```

---

## 6. 编程中的 code point 实践

### 6.1 常用 code point API（Java）

| 方法 | 功能 |
|------|------|
| `String.codePointCount()` | 真正的字符数 |
| `String.codePointAt(i)` | 位置 i 的码点（处理代理对） |
| `String.codePoints()` | 码点流（遍历） |
| `Character.toChars(cp)` | 码点 → char[]（可能 2 个） |
| `Character.isEmoji(cp)` | 是否 Emoji（Java 21+） |
| `Character.isSupplementary(cp)` | 是否补充平面 |

### 6.2 实际场景

```text
code point 实践场景
├── ① 长度校验：用户输入「字符数」限制（codePointCount 而非 length）
├── ② 文本截断：按字符截断（不切代理对）
├── ③ 正则：\uD83D... 处理 Emoji（用码点范围）
├── ④ 数据库长度：VARCHAR(n) 的 n 是字符还是字节？（MySQL 是字符）
└── ⑤ 脱敏：手机号打码（按字符处理——Emoji 安全）

正则与码点（Java）
├── 匹配 Emoji：\p{IsEmoji}（Java 21+）
├── 匹配中文：\p{IsHan}
├── 匹配任意字母：\p{L}
└── 匹配任意数字：\p{N}（含全角）
```

---

## 7. 规范化与大小写

### 7.1 规范化（Normalization）

```text
为什么需要规范化
├── 同一个「字符」可能有多种码点表示！
├── 例："é" 可以是：
│   ├── 预组合：U+00E9（单个码点）
│   └── 分解：e + 组合重音（U+0065 U+0301，两个码点）
├── 比较/搜索时：两种表示应视为相同

四种规范化形式
├── NFC：规范组合（推荐默认——预组合）
├── NFD：规范分解
├── NFKC：兼容组合（语义等价合并）
└── NFKD：兼容分解

Java 实践
String normalized = Normalizer.normalize(input, Normalizer.Form.NFC);
// 用户输入统一 NFC（比较/存储一致性）
```

### 7.2 大小写与国际化的坑

```text
大小写转换的国际化陷阱
├── ① 土耳其语 i：大写是 İ（不是 I！）——Locale 相关
├── ② 德文 ß：大写是 SS（长度变化！）
├── ③ 希腊语 Σ：词尾是 ς（上下文相关）
├── ④ Java 默认：toUpperCase() 用默认 Locale——可能意外
└── ⑤ 正确处理：显式指定 Locale.ROOT
    String upper = s.toUpperCase(Locale.ROOT);   // 稳定结果
    String lower = s.toLowerCase(Locale.ROOT);

排序（Collation）
├── 不同语言排序规则不同（中文拼音/笔画、德文 ß 位置）
├── Java：Collator.getInstance(locale)
├── 数据库：排序规则（utf8mb4_general_ci/zh 等）
└── ⚠️ 简单 compareTo 是码点序（不是语言序！）
```

---

## 8. 核心要点

> 🎯 **核心要点**：
> 1. Unicode 17 平面：BMP（常见字符）与补充平面（Emoji）——**Emoji 是「跨出 BMP」的典型**
> 2. 代理对：UTF-16 双单元表示 BMP 外字符——**Java char ≠ 一个字符**
> 3. UTF-8 字节模式（1-4 字节 + 自同步）——从任意字节可正确解析
> 4. Java 处理字符用 code point API（codePointCount/codePoints）——**不用 char 级操作**
> 5. 规范化（NFC 默认）解决「同一字符多种码点」——比较/存储一致性
> 6. 大小写转换指定 Locale.ROOT（土耳其 i/德文 ß 陷阱）；排序用 Collator

---

**下一模块**：[03-中文编码标准](03-中文编码标准.md) | **返回总览**：[00-代码与编程语言的国际性知识体系总览](00-代码与编程语言的国际性知识体系总览.md)
