# Jackson 核心实战

> Jackson 是 Java JSON 处理的"事实标准"——Spring Boot 默认引擎，功能最全、生态最大、注解最多、安全最稳

## 📚 目录

1. [ObjectMapper 核心配置](#1)
2. [Jackson 注解完全指南](#2)
3. [泛型与复杂类型处理](#3)
4. [流式 API (Streaming API)](#4)
5. [树模型 (Tree Model)](#5)
6. [扩展模块](#6)
7. [Jackson 2.18/2.19 新特性](#7)

---

## 1. ObjectMapper 核心配置 {#1}

### 1.1 基础用法

```xml
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.18.2</version>
</dependency>
```

```java
import com.fasterxml.jackson.databind.*;

// ── ObjectMapper 是线程安全的，必须全局单例复用 ──
ObjectMapper mapper = new ObjectMapper();

// POJO → JSON (序列化)
User user = new User("Alice", 25);
String json = mapper.writeValueAsString(user);
// {"name":"Alice","age":25}

// JSON → POJO (反序列化)
User parsed = mapper.readValue("{\"name\":\"Bob\",\"age\":30}", User.class);

// 文件读写
mapper.writeValue(new File("output.json"), user);
User fromFile = mapper.readValue(new File("input.json"), User.class);

// JSON → Map/List
Map<String, Object> map = mapper.readValue(json, Map.class);
List<User> list = mapper.readValue(jsonArray, new TypeReference<List<User>>() {});
```

### 1.2 序列化特性配置

```java
ObjectMapper mapper = new ObjectMapper();

// ── 常用序列化配置 ──
mapper.configure(SerializationFeature.INDENT_OUTPUT, true);           // 美化输出
mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false); // 日期用 ISO 格式
mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);    // 空 Bean 不报错
mapper.configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true); // 枚举写 name()

// ── 属性包含策略 ──
mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);       // 不序列化 null
mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);      // null + 空字符串/集合
mapper.setSerializationInclusion(JsonInclude.Include.NON_DEFAULT);    // null + 默认值
```

### 1.3 反序列化特性配置

```java
// ── 常用反序列化配置 ──
mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);  // 忽略未知属性
mapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true);  // null 赋值给基本类型报错
mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true); // 单值转数组
mapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
mapper.configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true); // 未知枚举→null

// ── 大小写不敏感 ──
mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
```

### 1.4 命名策略

```java
// ── 全局命名策略 ──
mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);     // snake_case
mapper.setPropertyNamingStrategy(PropertyNamingStrategies.UPPER_CAMEL_CASE); // PascalCase
mapper.setPropertyNamingStrategy(PropertyNamingStrategries.LOWER_CASE);     // 全小写

// ── Date 格式化 ──
mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
// 或使用 Java 8 time 模块（推荐）
mapper.registerModule(new JavaTimeModule());
mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

// ── 时区 ──
mapper.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
```

### 1.5 生产级 ObjectMapper 配置模板

```java
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
            // ── 模块注册 ──
            .registerModule(new JavaTimeModule())
            .registerModule(new Jdk8Module())
            .registerModule(new ParameterNamesModule())

            // ── 序列化 ──
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)

            // ── 反序列化 ──
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)

            // ── 其他 ──
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
            .setPropertyNamingStrategy(PropertyNamingStrategries.LOWER_CAMEL_CASE)

            // ── 安全配置 ──
            .disable(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE)
            .deactivateDefaultTyping();  // 禁用多态类型推断（安全）
    }
}
```

> 🎯 **核心要点**：`ObjectMapper` 创建成本极高（初始化序列化器缓存、内省 Bean），**必须全局单例**。Spring Boot 已自动配置，需要定制化时注入并修改默认配置而非新建实例。

---

## 2. Jackson 注解完全指南 {#2}

### 2.1 核心注解速查表

| 注解 | 作用于 | 用途 | 示例 |
|------|:---:|------|------|
| `@JsonProperty` | 字段/方法 | 重命名属性 | `@JsonProperty("user_name")` |
| `@JsonAlias` | 字段 | 反序列化时别名 | `@JsonAlias({"name","n"})` |
| `@JsonIgnore` | 字段/方法 | 忽略序列化和反序列化 | `@JsonIgnore` |
| `@JsonIgnoreProperties` | 类 | 忽略指定属性 | `@JsonIgnoreProperties({"password"})` |
| `@JsonFormat` | 字段 | 日期/数字格式化 | `@JsonFormat(pattern="yyyy-MM-dd")` |
| `@JsonInclude` | 类/字段 | 条件排除 | `@JsonInclude(NON_NULL)` |
| `@JsonUnwrapped` | 字段 | 扁平化嵌套对象 | `@JsonUnwrapped Address address` |
| `@JsonView` | 字段/方法 | 视图级过滤 | `@JsonView(Views.Public.class)` |
| `@JsonCreator` | 构造器/工厂 | 反序列化创建方式 | `@JsonCreator/mode=DELEGATING` |
| `@JsonValue` | 方法 | 序列化时仅输出该值 | 枚举序列化 |

### 2.2 实战示例

```java
import com.fasterxml.jackson.annotation.*;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)   // 类级：忽略未知 JSON 属性
public class User {

    @JsonProperty("user_id")                   // JSON 中字段名为 user_id
    private Long id;

    @JsonAlias({"name", "username", "n"})     // 反序列化时接受多种别名
    private String name;

    @JsonIgnore                               // 密码永不输出
    private String password;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;

    @JsonInclude(JsonInclude.Include.NON_NULL) // 为 null 时不输出
    private String email;

    @JsonUnwrapped                             // 扁平化嵌套对象
    private Address address;

    // ── 构造器注入（不可变对象）──
    @JsonCreator
    public User(
            @JsonProperty("user_id") Long id,
            @JsonProperty("name") String name) {
        this.id = id;
        this.name = name;
    }
}
```

### 2.3 多态类型处理

```java
// ── 基类注解 ──
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,          // 使用属性名标识类型
    include = JsonTypeInfo.As.PROPERTY,  // 作为 JSON 属性
    property = "type"                    // 类型字段名
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = Cat.class, name = "cat"),
    @JsonSubTypes.Type(value = Dog.class, name = "dog")
})
public abstract class Animal {
    private String name;
}

public class Cat extends Animal {
    private boolean indoor;
}

public class Dog extends Animal {
    private String breed;
}
```

```json
// 序列化输出
{"type": "cat", "name": "Whiskers", "indoor": true}
{"type": "dog", "name": "Rex", "breed": "Golden Retriever"}
```

### 2.4 自定义序列化器

```java
// ── 敏感信息脱敏 ──
public class SensitiveSerializer extends JsonSerializer<String> {
    @Override
    public void serialize(String value, JsonGenerator gen,
                          SerializerProvider provider) throws IOException {
        if (value == null) {
            gen.writeNull();
        } else if (value.length() <= 2) {
            gen.writeString(value);
        } else {
            gen.writeString(value.charAt(0) + "***" +
                           value.charAt(value.length() - 1));
        }
    }
}

// 使用
public class User {
    @JsonSerialize(using = SensitiveSerializer.class)
    private String phone;   // "13812345678" → "1***8"
}

// ── 金额格式化 ──
public class MoneySerializer extends JsonSerializer<BigDecimal> {
    @Override
    public void serialize(BigDecimal value, JsonGenerator gen,
                          SerializerProvider provider) throws IOException {
        gen.writeString(value.setScale(2, RoundingMode.HALF_UP).toString());
    }
}
```

### 2.5 @JsonFilter 动态过滤

```java
// 定义过滤器
@JsonFilter("userFilter")
public class User {
    public Long id;
    public String name;
    public String email;
    public String password;
}

// 使用过滤器
SimpleFilterProvider filters = new SimpleFilterProvider()
    .addFilter("userFilter",
        SimpleBeanPropertyFilter.filterOutAllExcept("id", "name"));

String json = new ObjectMapper()
    .setFilterProvider(filters)
    .writerWithDefaultPrettyPrinter()
    .writeValueAsString(user);
// 仅输出 {"id": 1, "name": "Alice"}
```

---

## 3. 泛型与复杂类型处理 {#3}

### 3.1 TypeReference —— 解决泛型擦除

```java
// ❌ 错误：泛型在运行时被擦除
List<User> list = mapper.readValue(json, List.class);
// list 实际是 List<Map>，每个元素是 LinkedHashMap，不是 User！

// ✅ 正确：使用 TypeReference
List<User> list = mapper.readValue(json, new TypeReference<List<User>>() {});

// ✅ Map 类型
Map<String, List<User>> map = mapper.readValue(json,
    new TypeReference<Map<String, List<User>>>() {});

// ✅ 嵌套泛型
Result<PageInfo<User>> result = mapper.readValue(json,
    new TypeReference<Result<PageInfo<User>>>() {});
```

### 3.2 JavaType —— 动态构建类型

```java
// TypeReference 必须是静态类型（编译期已知），运行时动态类型用 JavaType

TypeFactory tf = mapper.getTypeFactory();

// 动态构建 List<User>
JavaType listType = tf.constructCollectionType(List.class, User.class);
List<User> list = mapper.readValue(json, listType);

// 动态构建 Map<String, User>
JavaType mapType = tf.constructMapType(Map.class, String.class, User.class);
Map<String, User> map = mapper.readValue(json, mapType);

// 复杂嵌套：List<Map<String, List<User>>>
JavaType innerList = tf.constructCollectionType(List.class, User.class);
JavaType innerMap = tf.constructMapType(Map.class, String.class,
    innerList.getRawClass());  // List.class
JavaType outerList = tf.constructCollectionType(List.class, innerMap);
```

### 3.3 Jackson 类型转换

```java
// ObjectMapper.convertValue() —— 在 Java 对象间转换，不走 JSON 中间格式
ObjectMapper mapper = new ObjectMapper();

// Map → POJO
Map<String, Object> map = Map.of("name", "Alice", "age", 25);
User user = mapper.convertValue(map, User.class);

// POJO → Map
Map<String, Object> userMap = mapper.convertValue(user,
    new TypeReference<Map<String, Object>>() {});

// 不同 POJO 间转换
UserDTO dto = mapper.convertValue(userEntity, UserDTO.class);
```

---

## 4. 流式 API (Streaming API) {#4}

### 4.1 适用场景

流式 API 适用于**超大 JSON**（GB 级），逐 token 解析，内存占用极小：

```java
import com.fasterxml.jackson.core.*;

// ── 解析超大 JSON 数组 ──
// [ {...}, {...}, ... ]  ← 可能有百万个元素
JsonFactory factory = new JsonFactory();

try (JsonParser parser = factory.createParser(new File("huge.json"))) {
    // 确保是数组开头
    if (parser.nextToken() != JsonToken.START_ARRAY) {
        throw new IllegalStateException("Expected JSON array");
    }

    while (parser.nextToken() != JsonToken.END_ARRAY) {
        // 每个元素按 POJO 解析
        User user = mapper.readValue(parser, User.class);
        processUser(user);   // 处理完即可 GC，无需全量加载
    }
}
```

### 4.2 流式写入

```java
// ── 流式生成 JSON，避免内存中构建完整对象 ──
try (JsonGenerator gen = factory.createGenerator(new FileWriter("output.json"))) {
    gen.writeStartArray();                       // [

    for (User user : fetchUsersFromDB()) {       // 从 DB 流式读取
        gen.writeStartObject();                  //   {
        gen.writeNumberField("id", user.getId());
        gen.writeStringField("name", user.getName());
        gen.writeEndObject();                    //   }
    }

    gen.writeEndArray();                         // ]
}
```

### 4.3 JsonParser 直接读取指定字段

```java
// 场景：只关心 JSON 中的几个字段，无需解析整个对象
try (JsonParser parser = factory.createParser(json)) {
    while (parser.nextToken() != JsonToken.END_OBJECT) {
        String fieldName = parser.currentName();
        parser.nextToken();  // 移动到值

        switch (fieldName) {
            case "id" -> userId = parser.getLongValue();
            case "name" -> userName = parser.getText();
            default -> parser.skipChildren();  // 跳过不关心的字段
        }
    }
}
```

> 🎯 **核心要点**：处理超大 JSON 时的铁律——**永远不要将整个 JSON 加载到 String 中**，直接用 `InputStream`/`Reader` + 流式 API，内存可控。

---

## 5. 树模型 (Tree Model) {#5}

### 5.1 基本操作

树模型适合结构动态变化的 JSON，不需要预定义 Java 类：

```java
// JSON → JsonNode
String json = """
    {"name":"Alice","age":25,"address":{"city":"NYC"}}
    """;

JsonNode root = mapper.readTree(json);

// ── 读取值 ──
String name = root.get("name").asText();           // "Alice"
int age = root.get("age").asInt();                 // 25
String city = root.at("/address/city").asText();   // JSONPath: "NYC"

// ── 安全读取（字段可能不存在）──
String email = root.has("email")
    ? root.get("email").asText()
    : "unknown";

// ── 数组遍历 ──
JsonNode tagsNode = root.get("tags");
if (tagsNode.isArray()) {
    for (JsonNode tag : tagsNode) {
        System.out.println(tag.asText());
    }
}
```

### 5.2 动态构建和修改

```java
// ── 创建 JsonNode ──
ObjectNode userNode = mapper.createObjectNode();
userNode.put("name", "Alice");
userNode.put("age", 25);
userNode.putNull("email");

ObjectNode addressNode = mapper.createObjectNode();
addressNode.put("city", "New York");
addressNode.put("zip", "10001");
userNode.set("address", addressNode);

ArrayNode tagsNode = userNode.putArray("tags");
tagsNode.add("java");
tagsNode.add("spring");

// 输出
System.out.println(mapper.writerWithDefaultPrettyPrinter()
    .writeValueAsString(userNode));

// ── 修改现有 JSON ──
((ObjectNode) root).put("age", 26);                  // 更新字段
((ObjectNode) root).remove("email");                 // 删除字段
((ObjectNode) root).set("email",                    // 替换值
    new TextNode("alice@example.com"));
```

### 5.4 JsonNode 类型判断

```java
JsonNode node = root.get("value");

if (node.isNull())           { /* null */ }
else if (node.isTextual())   { node.asText();    }
else if (node.isInt())       { node.asInt();     }
else if (node.isLong())      { node.asLong();    }
else if (node.isDouble())    { node.asDouble();  }
else if (node.isBoolean())   { node.asBoolean(); }
else if (node.isArray())     { /* 数组处理 */     }
else if (node.isObject())    { /* 对象处理 */     }
```

### 5.5 POJO vs Tree Model vs Streaming 选型

| 场景 | 推荐方式 | 理由 |
|------|---------|------|
| 结构固定、有 Java 类 | **POJO 绑定** | 类型安全、代码清晰 |
| 结构动态、无固定类 | **Tree Model** | 灵活访问，JSONPath 支持 |
| 超大文件、只读部分字段 | **Streaming API** | 内存极低 |
| 需要部分解析 + 部分动态 | 混合使用 | 流式读取 + 按需 POJO/Tree |

---

## 6. 扩展模块 {#6}

### 6.1 Java 8 时间模块（必备）

```xml
<dependency>
    <groupId>com.fasterxml.jackson.datatype</groupId>
    <artifactId>jackson-datatype-jsr310</artifactId>
</dependency>
```

```java
mapper.registerModule(new JavaTimeModule());
mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

// 现在 LocalDateTime、ZonedDateTime、Instant 等都可正常序列化
// LocalDateTime → "2026-07-30T10:30:00"
// Instant       → "2026-07-30T02:30:00Z"
```

### 6.2 JDK 8 模块

```xml
<dependency>
    <groupId>com.fasterxml.jackson.datatype</groupId>
    <artifactId>jackson-datatype-jdk8</artifactId>
</dependency>
```

```java
mapper.registerModule(new Jdk8Module());
// 支持：Optional<T>、OptionalInt、OptionalLong、OptionalDouble
// Optional.of("value") → "value"
// Optional.empty()    → null
```

### 6.3 多格式模块

```xml
<!-- JSON → Jackson 原生支持，无需额外依赖 -->

<!-- XML -->
<dependency>
    <groupId>com.fasterxml.jackson.dataformat</groupId>
    <artifactId>jackson-dataformat-xml</artifactId>
</dependency>

<!-- YAML -->
<dependency>
    <groupId>com.fasterxml.jackson.dataformat</groupId>
    <artifactId>jackson-dataformat-yaml</artifactId>
</dependency>

<!-- CSV -->
<dependency>
    <groupId>com.fasterxml.jackson.dataformat</groupId>
    <artifactId>jackson-dataformat-csv</artifactId>
</dependency>

<!-- Protobuf -->
<dependency>
    <groupId>com.fasterxml.jackson.dataformat</groupId>
    <artifactId>jackson-dataformat-protobuf</artifactId>
</dependency>
```

```java
// 使用统一的 ObjectMapper 即可处理多格式
// XML: 使用 XmlMapper（继承自 ObjectMapper）
XmlMapper xmlMapper = new XmlMapper();
User user = xmlMapper.readValue(xmlString, User.class);

// YAML
YAMLMapper yamlMapper = new YAMLMapper();

// CSV
CsvMapper csvMapper = new CsvMapper();
CsvSchema schema = csvMapper.schemaFor(User.class).withHeader();
List<User> users = csvMapper.readerFor(User.class)
    .with(schema)
    .readValues(csvString)
    .readAll();
```

---

## 7. Jackson 2.18/2.19 新特性 {#7}

### 7.1 零拷贝数值解析（2.18 里程碑特性）

Jackson 2.18 重构了浮点数解析，避免不必要的字符串分配：

```java
// 启用快速数值解析
ObjectMapper mapper = JsonMapper.builder()
    .enable(StreamReadFeature.USE_FAST_DOUBLE_PARSER)
    .enable(StreamReadFeature.USE_FAST_BIG_NUMBER_PARSER)
    .build();

// 性能提升：
// ── 1 亿次 double 解析：内存从 ~6 GiB 降到 <100 MiB
// ── 吞吐量提升约 30%
```

### 7.2 锁优化

```java
// 2.18 将内部缓存从 synchronized 改为 ReentrantLock
// ── InternCache、ThreadLocalBufferManager 并发性能更高
// ── 多线程场景下 ObjectMapper 吞吐量进一步提升
```

### 7.3 Multi-Release JAR（2.19）

```java
// 2.19 支持 Multi-Release JAR
// ── 同一个 JAR 可针对不同 Java 版本提供不同实现
// ── JDK 17+ 用户自动获得 Record 等新特性的优化路径
// ── 最低仍兼容 Java 8
```

### 7.4 版本选择建议

| JDK 版本 | 推荐 Jackson 版本 | 说明 |
|---------|:---:|------|
| Java 8 | 2.18.x | 兼容性最好 |
| Java 11-17 | 2.18.x / 2.19.x | 2.19 优化更佳 |
| Java 21+ | 2.19.x | Multi-Release JAR 获得最优路径 |

> 🎯 **核心要点**：Jackson 2.18 是 2024-2025 年最重要的升级——浮点解析零拷贝优化让处理数值密集型 JSON 的性能和内存表现均有质的飞跃。处于 2.17 及以下版本的项目应优先升级。

---

**返回总览：** [00-JSON 知识体系总览](./00-JSON知识体系总览.md) | **下一模块：** [03-Gson 与 Fastjson2](./03-Gson与Fastjson2.md)
