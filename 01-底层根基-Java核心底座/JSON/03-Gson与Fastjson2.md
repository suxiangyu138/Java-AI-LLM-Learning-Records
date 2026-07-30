# Gson 与 Fastjson2

> Jackson 是标准答案，但 Gson 的简洁和 Fastjson2 的极速，在特定场景下是无法忽视的选择

## 📚 目录

1. [Gson 核心实战](#1)
2. [Gson vs Jackson 选型](#2)
3. [Fastjson2 实战与迁移](#3)
4. [Fastjson2 安全配置（必读）](#4)
5. [三库全景性能对比](#5)

---

## 1. Gson 核心实战 {#1}

### 1.1 快速上手

```xml
<dependency>
    <groupId>com.google.code.gson</groupId>
    <artifactId>gson</artifactId>
    <version>2.12.1</version>
</dependency>
```

```java
import com.google.gson.*;

// ── 基本用法 ──
Gson gson = new Gson();

// 序列化
String json = gson.toJson(new User("Alice", 25));
// {"name":"Alice","age":25}

// 反序列化
User user = gson.fromJson("{\"name\":\"Bob\",\"age\":30}", User.class);

// ── 生产级配置：GsonBuilder ──
Gson gson = new GsonBuilder()
    .setPrettyPrinting()                           // 美化输出
    .setDateFormat("yyyy-MM-dd HH:mm:ss")          // 日期格式
    .disableHtmlEscaping()                         // 不转义 HTML (< > &)
    .serializeNulls()                              // 序列化 null 值
    .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)  // snake_case
    .setExclusionStrategies()                      // 排除策略
    .create();
```

### 1.2 TypeToken —— Gson 的泛型令牌

```java
// ── 泛型集合反序列化 ──
List<User> users = gson.fromJson(jsonArray,
    new TypeToken<List<User>>(){}.getType());

// ── 复杂嵌套泛型 ──
Map<String, List<User>> map = gson.fromJson(json,
    new TypeToken<Map<String, List<User>>>(){}.getType());
```

### 1.3 Gson 注解

```java
public class User {

    @SerializedName("user_id")      // 重命名（等同于 @JsonProperty）
    private Long id;

    @SerializedName(value = "name",
        alternate = {"username", "n"})  // 反序列化别名
    private String name;

    @Expose(serialize = false, deserialize = true)  // 仅反序列化（密码只进不出）
    private String password;

    @Since(2.0)                     // 版本控制：仅 >=2.0 时包含
    private String newField;

    @Until(1.0)                     // 版本控制：仅 <1.0 时包含
    private String deprecatedField;
}

// @Expose 和版本控制需在 GsonBuilder 中显式启用
Gson gson = new GsonBuilder()
    .excludeFieldsWithoutExposeAnnotation()  // 只序列化 @Expose 字段
    .setVersion(2.0)
    .create();
```

### 1.4 自定义序列化器

```java
// ── 自定义序列化器 ──
class UserSerializer implements JsonSerializer<User> {
    @Override
    public JsonElement serialize(User src, Type typeOfSrc,
                                  JsonSerializationContext context) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", src.getId());
        obj.addProperty("displayName", src.getName());
        obj.addProperty("avatar", "https://cdn.example.com/" + src.getId());
        return obj;
    }
}

// 注册
Gson gson = new GsonBuilder()
    .registerTypeAdapter(User.class, new UserSerializer())
    .create();

// ── 自定义反序列化器 ──
class UserDeserializer implements JsonDeserializer<User> {
    @Override
    public User deserialize(JsonElement json, Type typeOfT,
                             JsonDeserializationContext context) {
        JsonObject obj = json.getAsJsonObject();
        return new User(
            obj.get("id").getAsLong(),
            obj.get("name").getAsString()
        );
    }
}
```

### 1.5 Gson 核心类速查

| 类/接口 | 用途 | 类比 Jackson |
|---------|------|-------------|
| `Gson` | 主入口（线程安全） | `ObjectMapper` |
| `GsonBuilder` | 配置构建器 | `ObjectMapper` 的 setter |
| `TypeToken<T>` | 泛型类型令牌 | `TypeReference<T>` |
| `JsonElement` | 树模型基类 | `JsonNode` |
| `JsonObject` | JSON 对象 | `ObjectNode` |
| `JsonArray` | JSON 数组 | `ArrayNode` |
| `JsonPrimitive` | 原始值 | `ValueNode` |
| `JsonNull` | null 值 | `NullNode` |
| `JsonSerializer<T>` | 自定义序列化 | `JsonSerializer<T>` |
| `JsonDeserializer<T>` | 自定义反序列化 | `JsonDeserializer<T>` |

---

## 2. Gson vs Jackson 选型 {#2}

### 2.1 关键差异

| 维度 | Gson | Jackson |
|------|------|---------|
| **包体积** | ~240 KB | ~1.5 MB (core + databind + annotations) |
| **依赖** | 零外部依赖 | 3 个核心 JAR |
| **API 简洁度** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ |
| **注解丰富度** | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| **多格式支持** | ❌ 仅 JSON | ✅ JSON/XML/YAML/CSV |
| **流式 API** | ✅ JsonReader/Writer | ✅ JsonParser/Generator |
| **树模型** | ✅ JsonElement | ✅ JsonNode |
| **多态处理** | ⚠️ 需手动实现 | ✅ @JsonTypeInfo |
| **Spring 集成** | ⚠️ 需手动配置 | ✅ 默认集成 |
| **性能** | ⭐⭐⭐ | ⭐⭐⭐⭐ |

### 2.2 场景选型

| 场景 | 推荐 | 理由 |
|------|:---:|------|
| **Spring Boot 项目** | Jackson | 默认，零配置 |
| **Android 开发** | Gson | 体积小，ProGuard 友好 |
| **简单 JSON 读写** | Gson | API 最简洁 |
| **快速原型 / PoC** | Gson | 零配置即可用 |
| **复杂对象映射** | Jackson | 注解/多态/转换最完善 |
| **处理 GB 级 JSON** | Jackson | 流式 API 更成熟 |
| **需要多格式输出** | Jackson | YAML/XML/CSV 一体 |

---

## 3. Fastjson2 实战与迁移 {#3}

### 3.1 为什么是 Fastjson2？

```
Fastjson 1.x → 已 EOL（停止维护） → ❌ 禁止新项目使用
Fastjson2     → 全新架构、安全重构   → ✅ 高性能场景可选
```

```xml
<!-- 注意：必须是 fastjson2，不是 fastjson！ -->
<dependency>
    <groupId>com.alibaba.fastjson2</groupId>
    <artifactId>fastjson2</artifactId>
    <version>2.0.53</version>
</dependency>
```

### 3.2 基本用法

```java
import com.alibaba.fastjson2.*;

// ── 序列化 ──
String json = JSON.toJSONString(user);
// 美化输出
String pretty = JSON.toJSONString(user, JSONWriter.Feature.PrettyFormat);
// 排除 null
String noNull = JSON.toJSONString(user, JSONWriter.Feature.WriteNulls);
// 按字段顺序
String ordered = JSON.toJSONString(user,
    JSONWriter.Feature.FieldBased,
    JSONWriter.Feature.SortMapEntriesByKeys);

// ── 反序列化 ──
User user = JSON.parseObject(json, User.class);
List<User> users = JSON.parseArray(jsonArray, User.class);

// ── 泛型反序列化 ──
List<User> list = JSON.parseObject(jsonArray,
    new TypeReference<List<User>>(){}.getType());
```

### 3.3 Fastjson2 注解

```java
@JSONType(orders = {"id", "name", "age"})  // 输出顺序
public class User {

    @JSONField(name = "user_id")            // 重命名
    private Long id;

    @JSONField(serialize = false)           // 不序列化
    private String password;

    @JSONField(format = "yyyy-MM-dd")       // 日期格式
    private LocalDate birthday;

    @JSONField(serializeFeatures = JSONWriter.Feature.WriteNulls)
    private String email;                   // 即使全局排除 null，此字段仍输出
}
```

### 3.4 JSONPath 查询

Fastjson2 内置 JSONPath 支持，无需额外依赖：

```java
String json = """
    {"store":{"books":[
        {"title":"Java 实战","price":99.0},
        {"title":"Spring 揭秘","price":79.0}
    ]}}
    """;

// 路径查询
JSONPath path = JSONPath.of("$.store.books[*].title");
List<String> titles = (List<String>) path.eval(JSON.parse(json));
// ["Java 实战", "Spring 揭秘"]

// 过滤查询
JSONPath filterPath = JSONPath.of("$.store.books[?(@.price > 80)].title");
List<String> filtered = (List<String>) filterPath.eval(JSON.parse(json));
// ["Java 实战"]

// 修改
path.set(JSON.parse(json), "Updated Title");
```

### 3.5 Fastjson 1.x → Fastjson2 迁移

```java
// ── 包名变化 ──
// 1.x: com.alibaba.fastjson.JSON
// 2.x: com.alibaba.fastjson2.JSON

// ── 不兼容变更 ──
// ❌ 1.x                                    ✅ 2.x
JSON.DEFAULT_GENERATE_FEATURE              JSONWriter.Feature 枚举
JSON.DEFAULT_PARSER_FEATURE                JSONReader.Feature 枚举
SerializeConfig.getGlobalInstance()        JSON.config()
ParserConfig.getGlobalInstance()           JSON.config()

// ── API 兼容层（1.x 代码可运行在 2.x 上）──
// 引入兼容包
// <dependency>
//     <groupId>com.alibaba</groupId>
//     <artifactId>fastjson</artifactId>
//     <version>2.0.53</version>
//     <classifier>compatible</classifier>
// </dependency>
// 之后 import com.alibaba.fastjson.JSON 直接可使用 2.x 引擎
```

---

## 4. Fastjson2 安全配置（必读） {#4}

### 4.1 Fastjson 1.x 漏洞简史

```
CVE-2017-18349  (CVSS 9.8)  AutoType导致的RCE
CVE-2020-8840   (CVSS 9.8)  JNDI注入绕过
CVE-2022-25845  (CVSS 9.8)  AutoType反序列化RCE
CVE-2025-70974  (CVSS 10.0) 1.2.48前版本，在野利用
CVE-2026-16723  (CVSS 9.0)  1.2.68-1.2.83 零交互RCE，在野利用

结论：Fastjson 1.x 全线不安全，必须升级！
```

### 4.2 Fastjson2 安全配置模板

```java
import com.alibaba.fastjson2.*;

public class SafeFastjsonConfig {

    /**
     * 生产环境安全配置 —— Fastjson2
     * 原则：默认最安全配置，仅在确需时放开限制
     */
    static {
        // ── 1. 启用 SafeMode（禁止 @type / AutoType）──
        // Fastjson2 默认已禁用 AutoType，比 1.x 安全得多
        // 如需确认：
        // JSON.config(JSONReader.Feature.SupportAutoType, false);

        // ── 2. 如果必须使用 AutoType（如多态反序列化），设置严格白名单 ──
        JSON.config(
            // 仅允许白名单中的类
            JSONReader.Feature.SupportAutoType
        );

        // 添加可信类
        JSON.getTypeByName("@type的完整类名");
        // 或通过配置文件
        // fastjson2.autoType.accept=com.example.model.*
        // fastjson2.autoType.deny=java.*,javax.*

        // ── 3. 输入大小限制 ──
        // 限制 JSON 字符串最大长度（防止 DoS）
        // 在 parse 前自行校验: json.length() > MAX_JSON_LENGTH
    }
}
```

### 4.3 反序列化安全对比

| 安全特性 | Jackson | Gson | Fastjson2 | Fastjson 1.x |
|------|:---:|:---:|:---:|:---:|
| **SafeMode（禁用多态）** | ✅ 默认禁用 | ✅ 不支持多态 | ✅ 默认安全 | ❌ 默认不安全 |
| **类型白名单** | ✅ | N/A | ✅ | ⚠️ 绕过历史 |
| **输入大小限制** | ✅ StreamReadConstraints | ❌ | ❌（自行校验） | ❌ |
| **CVE 历史** | 极少 | 极少 | 较少（注重架构安全） | ⚠️ 数十个 RCE |
| **安全建议** | 保持默认 | 保持默认 | 启用 SafeMode | 🚫 立即停用 |

> 🎯 **核心要点**：Fastjson2 从架构层面解决了 1.x 的安全问题——默认不启用 AutoType、白名单优先、无黑名单绕过。但仍建议**显式配置 SafeMode**，此为生产环境底线。

---

## 5. 三库全景性能对比 {#5}

### 5.1 JMH 基准测试数据

基于公开 benchmark（2024-2025 年数据，JDK 21）：

| 场景 | Jackson 2.18 | Fastjson2 2.0 | Gson 2.12 |
|------|:---:|:---:|:---:|
| **简单 POJO 序列化** | 1,189 ops/ms | 1,234 ops/ms 🏆 | 892 ops/ms |
| **复杂对象序列化** | 921 ops/ms 🏆 | 856 ops/ms | 542 ops/ms |
| **大文本反序列化** | 712 ops/ms 🏆 | 647 ops/ms | 498 ops/ms |
| **序列化内存峰值** | 120 MB | 95 MB 🏆 | 180 MB |
| **反序列化内存峰值** | 140 MB | 105 MB 🏆 | 200 MB dividends |

### 5.2 综合评分

```text
            Jackson   Gson   Fastjson2
性能         ★★★★☆   ★★★☆☆   ★★★★★
安全性       ★★★★★   ★★★★★   ★★★★☆ (须配置)
功能丰富度   ★★★★★   ★★★☆☆   ★★★★☆
注解支持     ★★★★★   ★★★☆☆   ★★★★☆
Spring集成   ★★★★★   ★★☆☆☆   ★★★☆☆
文档/社区    ★★★★★   ★★★★☆   ★★★☆☆
包体积       ★★★☆☆   ★★★★★   ★★★☆☆
零依赖       ★☆☆☆☆   ★★★★★   ★☆☆☆☆
```

### 5.3 最终建议

```
你的项目 → 选什么？
│
├── Spring Boot 项目       → Jackson（不要自找麻烦）
├── 全新项目（非Spring）    → Jackson（最稳的选择）
├── Android / 极小体积     → Gson（Google 出品）
├── API 网关 / 高并发      → Fastjson2（性能领先，安全必配置）
├── 遗留 Fastjson 1.x      → 立即迁移 Fastjson2
└── 多库并存              → 不推荐（版本冲突、行为不一致）
```

> 🎯 **核心要点**：**首选 Jackson，辅助 Gson（轻量场景）或 Fastjson2（性能场景）**。三库混用是反模式——除非有明确的、不可替代的场景需求。

---

**返回总览：** [00-JSON 知识体系总览](./00-JSON知识体系总览.md) | **下一模块：** [04-JSON 与 Spring Boot](./04-JSON与Spring-Boot.md)
