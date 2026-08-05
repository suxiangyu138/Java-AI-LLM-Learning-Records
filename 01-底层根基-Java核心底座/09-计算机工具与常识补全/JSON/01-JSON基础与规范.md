# JSON 基础与规范

> JSON 是现代 Web 的"通用数据语言"——轻量、可读、语言无关，是所有 Java 开发者必会的基础技能

## 📚 目录

1. [JSON 语法规范 (RFC 8259)](#1)
2. [JSON 数据类型详解](#2)
3. [JSON 数据结构设计](#3)
4. [JSONPath —— JSON 的 XPath](#4)
5. [JSON Schema 入门](#5)
6. [JSON vs 其他数据格式](#6)

---

## 1. JSON 语法规范 (RFC 8259) {#1}

### 1.1 JSON 是什么？

JSON（JavaScript Object Notation）是一种轻量级的数据交换格式，由 **RFC 8259**（2017）和 **ECMA-404** 标准定义。它是目前 Web API、配置文件、消息队列中最主流的数据格式。

```json
{
  "name": "John",
  "age": 30,
  "isAdmin": false,
  "address": {
    "city": "New York",
    "zip": "10001"
  },
  "tags": ["java", "spring", "json"],
  "metadata": null
}
```

### 1.2 基本语法规则

| 规则 | 说明 | 正确 ✅ | 错误 ❌ |
|------|------|---------|---------|
| **键必须双引号** | key 用 `"` 包裹 | `"name": "Alice"` | `name: "Alice"` |
| **字符串双引号** | 值用 `"` 而非 `'` | `"hello"` | `'hello'` |
| **不支持注释** | JSON 原生不支持注释 | 用 `"_comment"` 字段 | `// 注释` |
| **无尾随逗号** | 最后一个元素后不加逗号 | `[1, 2, 3]` | `[1, 2, 3,]` |
| **UTF-8 编码** | RFC 要求 UTF-8 | `\u4e2d\u6587` | GBK 编码 |
| **数字格式** | 不区分整数/浮点数 | `42`, `3.14`, `-1.5e10` | `0x2A`, `NaN`, `Infinity` |

### 1.3 转义字符

```json
{
  "path": "C:\\Users\\John\\Documents",
  "quote": "He said \"Hello\"",
  "multiline": "Line1\nLine2\tTabbed",
  "unicode": "\u4e2d\u6587"
}
```

| 转义 | 含义 | 转义 | 含义 |
|------|------|------|------|
| `\"` | 双引号 | `\n` | 换行 |
| `\\` | 反斜杠 | `\r` | 回车 |
| `\/` | 正斜杠 | `\t` | 制表符 |
| `\b` | 退格 | `\f` | 换页 |
| `\uXXXX` | Unicode | | |

### 1.4 JSON 验证工具

```bash
# 命令行验证
echo '{"name":"test"}' | python -m json.tool
echo '{"name":"test"}' | jq .

# Java 验证
# new ObjectMapper().readTree(jsonString);
```

---

## 2. JSON 数据类型详解 {#2}

### 2.1 六大数据类型

```
JSON 值
├── Object   → {"key": "value", ...}   无序键值对集合
├── Array    → [value, value, ...]     有序值列表
├── String   → "Hello World"           Unicode 字符串
├── Number   → 42 / 3.14 / -1.5e10    整数或浮点数
├── Boolean  → true / false
└── null     → null
```

### 2.2 Number 类型陷阱

```json
{
  "integer": 9007199254740993,      // ⚠️ 超出 JS/部分库的安全整数范围
  "float": 0.1 + 0.2,               // ⚠️ 浮点精度问题：不传 0.3，传字符串
  "bigDecimal": "123456789.987654321", // ✅ 大数、精确小数用字符串
  "scientific": 1.5e10,              // 1.5 × 10¹⁰ = 15000000000
  "leadingZero": 42                  // ❌ 不允许前导零（如 042）
}
```

> 🎯 **核心要点**：涉及金额、大整数时，JSON 中优先用**字符串**传递，避免 Number 精度丢失。Jackson 可用 `@JsonFormat(shape = Shape.STRING)` 将 `BigDecimal`/`Long` 序列化为字符串。

### 2.3 null vs 缺失字段

```json
// 三种情况的语义差异
{
  "email": null,       // 显式 null：字段存在但值为空
  "age": 0             // 零值：与 null 语义完全不同
}
// "phone" 字段缺失   // 隐式缺失：可能表示"未提供"或"不适用"
```

| 情况 | Jackson 默认行为 | 语义 |
|------|:---:|------|
| 字段缺失 | 不设置（字段默认值） | 未提供 |
| `"email": null` | 设置为 null | 显式清空 |
| `"age": 0` | 设置为 0 | 零值 |

---

## 3. JSON 数据结构设计 {#3}

### 3.1 扁平 vs 嵌套

```json
// ❌ 过度嵌套 —— 难以解析和扩展
{
  "user": {
    "profile": {
      "personal": {
        "name": "Alice"
      }
    }
  }
}

// ✅ 适度扁平 —— 命名体现层级
{
  "userId": 1,
  "userName": "Alice",
  "profileAvatar": "https://cdn.example.com/avatar.jpg",
  "profileBio": "Java Developer"
}
```

### 3.2 命名规范

| 风格 | 示例 | 主流场景 |
|------|------|---------|
| **camelCase** | `firstName`, `createdAt` | Java (Jackson 默认), JavaScript |
| **snake_case** | `first_name`, `created_at` | Python, Ruby, PostgreSQL |
| **PascalCase** | `FirstName`, `CreatedAt` | C#, .NET |
| **kebab-case** | `first-name`, `created-at` | HTTP Headers, CSS（JSON 键不支持） |

```java
// Jackson 自动转换命名风格
ObjectMapper mapper = new ObjectMapper();
mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

// Java POJO 使用 camelCase，JSON 输出 snake_case
public class User {
    private String firstName;  // → "first_name"
}
```

### 3.3 常见设计反模式

| 反模式 | 问题 | 改进 |
|--------|------|------|
| **深层嵌套 > 4 层** | 解析困难，维护噩梦 | 拆分嵌套对象，用引用替代 |
| **超大数组** | 内存溢出，序列化慢 | 分页返回，流式处理 |
| **动态类型字段** | `"value": "100"` 有时是字符串有时是数字 | 统一类型，或用多态字段 |
| **冗余数据** | 每个元素包含相同的 `createdBy` | 抽取到外层公共字段 |
| **二进制数据 Base64** | 膨胀 33%，浪费带宽 | 用 multipart 或独立 URL |
| **枚举用数字** | `"status": 1` 无自描述性 | 用字符串 `"status": "ACTIVE"` |

### 3.4 标准 API 响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "users": [
      {"id": 1, "name": "Alice"},
      {"id": 2, "name": "Bob"}
    ],
    "pagination": {
      "page": 1,
      "size": 20,
      "total": 150,
      "hasNext": true
    }
  },
  "timestamp": "2026-07-30T10:30:00Z",
  "requestId": "req-abc-123"
}
```

---

## 4. JSONPath —— JSON 的 XPath {#4}

### 4.1 基本表达式

JSONPath 是 JSON 的查询语言，类比 XML 的 XPath：

```java
// Maven 依赖
// <dependency>
//     <groupId>com.jayway.jsonpath</groupId>
//     <artifactId>json-path</artifactId>
//     <version>2.9.0</version>
// </dependency>

import com.jayway.jsonpath.*;

String json = """
    {
      "store": {
        "books": [
          {"title": "Java 实战", "price": 99.0, "category": "tech"},
          {"title": "Spring 揭秘", "price": 79.0, "category": "tech"},
          {"title": "三体", "price": 45.0, "category": "fiction"}
        ]
      }
    }
    """;

// 基础查询
List<String> titles = JsonPath.read(json, "$.store.books[*].title");
// ["Java 实战", "Spring 揭秘", "三体"]

// 过滤表达式
List<String> cheapBooks = JsonPath.read(json,
    "$.store.books[?(@.price < 50)].title");
// ["三体"]

// 获取第一本书
Map<String, Object> first = JsonPath.read(json, "$.store.books[0]");

// 价格总和
Double total = JsonPath.read(json, "$.store.books[*].price.sum()");
```

### 4.2 JSONPath 语法速查

| 表达式 | 含义 | 示例 |
|--------|------|------|
| `$` | 根节点 | `$` |
| `.key` | 子节点 | `$.name` |
| `..key` | 递归搜索 | `$..title` |
| `[n]` | 数组索引 | `$[0]`, `$[-1]` |
| `[*]` | 所有元素 | `$.books[*]` |
| `[start:end]` | 切片 | `$[0:3]` |
| `[?(@.key > 10)]` | 过滤器 | `[?(@.price < 50)]` |
| `@` | 当前元素 | 仅在过滤器中使用 |

### 4.3 高级用法

```java
// 配置与缓存
Configuration conf = Configuration.builder()
    .options(Option.AS_PATH_LIST, Option.SUPPRESS_EXCEPTIONS)
    .build();

DocumentContext ctx = JsonPath.using(conf).parse(json);

// 修改 JSON
ctx.set("$.store.books[0].price", 129.0);
ctx.add("$.store.books", Map.of("title", "新书", "price", 59.0));
ctx.delete("$.store.books[2]");

// 提取路径
List<String> paths = ctx.read("$..price");
```

---

## 5. JSON Schema 入门 {#5}

### 5.1 Schema 是什么？

JSON Schema 是一份"契约"——定义 JSON 数据应该长什么样（字段、类型、约束），用于自动化验证。

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "$id": "https://example.com/user.schema.json",
  "title": "User",
  "type": "object",
  "properties": {
    "id": {
      "type": "integer",
      "minimum": 1
    },
    "name": {
      "type": "string",
      "minLength": 1,
      "maxLength": 100
    },
    "email": {
      "type": "string",
      "format": "email"
    },
    "age": {
      "type": "integer",
      "minimum": 0,
      "maximum": 150
    },
    "role": {
      "type": "string",
      "enum": ["ADMIN", "USER", "GUEST"]
    },
    "tags": {
      "type": "array",
      "items": { "type": "string" },
      "uniqueItems": true
    }
  },
  "required": ["id", "name", "email"],
  "additionalProperties": false
}
```

### 5.2 Java 中使用 JSON Schema 校验

```xml
<dependency>
    <groupId>com.networknt</groupId>
    <artifactId>json-schema-validator</artifactId>
    <version>1.4.0</version>
</dependency>
```

```java
import com.networknt.schema.*;

// 加载 Schema
JsonSchemaFactory factory = JsonSchemaFactory.getInstance(
    SpecVersion.VersionFlag.V202012);
JsonSchema schema = factory.getSchema(
    UserValidator.class.getResourceAsStream("/user-schema.json"));

// 校验数据
String userJson = """
    {"id": 1, "name": "Alice", "email": "alice@example.com"}
    """;

Set<ValidationMessage> errors = schema.validate(
    new ObjectMapper().readTree(userJson));

if (errors.isEmpty()) {
    System.out.println("✅ 校验通过");
} else {
    errors.forEach(e ->
        System.out.printf("❌ %s: %s%n", e.getPath(), e.getMessage()));
}
```

### 5.3 Schema 关键字速查

| 关键字 | 用途 | 示例 |
|--------|------|------|
| `type` | 数据类型 | `"string"`, `"integer"`, `"array"` |
| `properties` | 对象属性定义 | `{"name": {...}}` |
| `required` | 必填字段 | `["id", "name"]` |
| `enum` | 枚举值 | `["A", "B", "C"]` |
| `minimum/maximum` | 数值范围 | `"minimum": 0` |
| `minLength/maxLength` | 字符串长度 | `"maxLength": 100` |
| `pattern` | 正则匹配 | `"^[a-z]+$"` |
| `format` | 格式约束 | `"email"`, `"date-time"`, `"uri"` |
| `items` | 数组元素约束 | `{"type": "string"}` |
| `minItems/maxItems` | 数组长度 | `"maxItems": 100` |
| `additionalProperties` | 是否允许额外属性 | `false` |
| `oneOf/anyOf/allOf` | 组合 Schema | `"oneOf": [...]` |

---

## 6. JSON vs 其他数据格式 {#6}

### 6.1 综合对比

| 维度 | JSON | XML | YAML | Protobuf |
|------|:---:|:---:|:---:|:---:|
| **可读性** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ❌ 二进制 |
| **数据体积** | 中等 | 大（标签冗余） | 中等 | 极小（压缩） |
| **解析速度** | 快 | 慢 | 中等 | 极快 |
| **Schema 支持** | JSON Schema | XSD (成熟) | 弱 | .proto (强) |
| **注释** | ❌ 不支持 | ✅ | ✅ | ✅ |
| **多态** | ❌ 需自行实现 | ✅ xsi:type | ✅ | ✅ |
| **Java 生态** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐ |
| **适合场景** | Web API、配置 | 企业系统集成 | 配置文件、DevOps | 高性能 RPC |

### 6.2 格式选型决策

```
场景选型决策：
│
├── REST API / Web Service → JSON（无争议首选）
│
├── 微服务间 RPC 通信 → Protobuf（性能优先）或 JSON（调试友好）
│
├── 配置文件 (人工编写) → YAML（注释 + 可读）> JSON > XML
│
├── 企业系统集成 (SOAP/EDI) → XML（业界标准，XSD 校验完善）
│
├── 消息队列 / 事件流 → JSON（通用）或 Avro/Protobuf（Schema Registry）
│
└── 前端构建工具配置 → JSON (package.json) / YAML (CI/CD)
```

> 🎯 **核心要点**：JSON 是通用数据交换的"通用语"——你不需要在所有场景都用 JSON，但你必须能读写、校验、安全处理 JSON。

---

**返回总览：** [00-JSON 知识体系总览](./00-JSON知识体系总览.md) | **下一模块：** [02-Jackson 核心实战](./02-Jackson核心实战.md)
