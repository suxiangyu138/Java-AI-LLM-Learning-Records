# JSON 最佳实践与安全

> JSON 反序列化是 Java 安全最危险的"入口"之一——一行 `readValue()` 可能触发 RCE。性能和安全，两手都要硬

## 📚 目录

1. [性能优化策略](#1)
2. [安全防护全景](#2)
3. [JSON Schema 校验体系](#3)
4. [常见问题排查手册](#4)
5. [工程化规范清单](#5)

---

## 1. 性能优化策略 {#1}

### 1.1 ObjectMapper 单例复用

这是最重要的性能规则——ObjectMapper 创建成本极高：

```java
// ❌ 错误 —— 每次请求新建（极慢！）
@RestController
public class BadController {
    @GetMapping("/bad")
    public User bad() throws Exception {
        return new ObjectMapper().readValue(json, User.class);  // 每次创建！
    }
}

// ✅ 正确 —— 注入全局单例
@RestController
public class GoodController {
    @Autowired
    private ObjectMapper mapper;     // Spring Boot 自动配置的单例

    @GetMapping("/good")
    public User good() throws Exception {
        return mapper.readValue(json, User.class);
    }
}
```

| 方式 | 操作耗时 | 说明 |
|------|:---:|------|
| 每次 new ObjectMapper | ~50-200ms | 初始化序列化器缓存、Bean 内省 |
| 全局单例 ObjectMapper | <1ms | 缓存预热完成 |

### 1.2 流式 API —— 处理大 JSON

```java
/**
 * 三种模式的性能与内存对比
 * 场景：处理 100MB JSON 文件
 */

// ❌ 方式1：全量加载到 String —— 内存爆炸
String json = Files.readString(Path.of("huge.json"));   // 100MB+ 内存
User[] users = mapper.readValue(json, User[].class);     // 再 ×2~3

// ⚠️ 方式2：数据绑定 —— 内存中构建完整数组
try (InputStream is = new FileInputStream("huge.json")) {
    User[] users = mapper.readValue(is, User[].class);  // 比 String 好，但仍需全部加载
}

// ✅ 方式3：流式 API —— 恒量内存
try (JsonParser parser = mapper.getFactory()
        .createParser(new File("huge.json"))) {

    if (parser.nextToken() != JsonToken.START_ARRAY) {
        throw new IllegalStateException("Expected array");
    }

    int count = 0;
    while (parser.nextToken() != JsonToken.END_ARRAY) {
        User user = mapper.readValue(parser, User.class);
        processUser(user);
        count++;
    }
    log.info("处理完成: {} 条", count);
}
```

### 1.3 Jackson 2.18 零拷贝数值优化

```java
// 启用快速数值解析（Jackson 2.18+）
ObjectMapper mapper = JsonMapper.builder()
    .enable(StreamReadFeature.USE_FAST_DOUBLE_PARSER)       // 零拷贝 double
    .enable(StreamReadFeature.USE_FAST_BIG_NUMBER_PARSER)    // 零拷贝 BigInteger
    .build();

// 效果：
// ── 1 亿次 double 解析：内存从 6 GiB → <100 MiB
// ── 吞吐量提升 ~30%
// ── 对数值密集型 JSON（IoT 数据、金融行情）效果显著
```

### 1.4 序列化预热

```java
// ── 应用启动时预加载序列化器，避免首次请求延迟 ──
@Component
public class JacksonWarmUp implements ApplicationRunner {

    @Autowired
    private ObjectMapper mapper;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // 预热常用类型
        User dummy = new User();
        mapper.writeValueAsString(dummy);                  // 预热序列化器
        mapper.readValue("{}", User.class);                // 预热反序列化器
        mapper.readValue("[]", new TypeReference<List<User>>() {});

        log.info("Jackson 预热完成");
    }
}
```

### 1.5 性能优化清单

| 优化项 | 效果 | 难度 |
|------|:---:|:---:|
| ObjectMapper 单例 | 🟢 关键 | 低 |
| 流式 API 处理大文件 | 🟢 关键 | 中 |
| 启用 USE_FAST_DOUBLE_PARSER (2.18+) | 🟢 显著 | 低 |
| 预热序列化器 | 🟡 有用 | 低 |
| JsonInclude.NON_NULL 减少输出 | 🟡 有用 | 低 |
| 避免深层嵌套 (>5 层) | 🟡 有用 | 设计层 |
| 用 JMH 基准测试验证 | 🟡 推荐 | 中 |
| GraalVM Native Image (AOT) | 🟢 显著 | 高 |

---

## 2. 安全防护全景 {#2}

### 2.1 JSON 反序列化为何危险？

```
攻击链：
  恶意 JSON 输入
  → 触发"有趣"类的构造器/setter/getter
  → 执行 Runtime.exec() / JNDI 注入
  → 远程代码执行 (RCE)
  → 服务器沦陷

根源：多态反序列化机制（@type / AutoType / enableDefaultTyping）
  让攻击者控制要实例化的类，进而控制程序行为
```

### 2.2 Jackson 安全配置

```java
// ═══════════════════════════════════════
// 🔴 绝对禁止的配置
// ═══════════════════════════════════════

// ❌ 不要启用全局多态类型（极危险！）
mapper.enableDefaultTyping();                          // 永不使用！
mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);

// ❌ 不要对不受信任的输入使用这些
mapper.enable(DeserializationFeature
    .FAIL_ON_INVALID_SUBTYPE);                        // 关闭类型检查


// ═══════════════════════════════════════
// ✅ 推荐的安全配置
// ═══════════════════════════════════════
ObjectMapper mapper = JsonMapper.builder()
    // 禁用默认类型推断
    .deactivateDefaultTyping()

    // 限制输入大小（防止 DoS）
    .streamReadConstraints(
        StreamReadConstraints.builder()
            .maxStringLength(20_000_000)    // 最大字符串 20MB
            .maxNumberLength(1000)           // 最大数字长度
            .maxNestingDepth(1000)           // 最大嵌套深度
            .build()
    )
    .build();
```

### 2.3 Fastjson 安全配置（回顾）

```java
// ── Fastjson2 安全基线 ──

// 1. 使用最新版本 Fastjson2（不是 1.x！）
// <dependency>
//     <groupId>com.alibaba.fastjson2</groupId>
//     <artifactId>fastjson2</artifactId>
//     <version>2.0.53</version>
// </dependency>

// 2. 生产环境必须启用 SafeMode
// JVM 参数：-Dfastjson2.parser.safeMode=true
// 或代码配置：
// Fastjson2 默认已安全，不需要额外配置

// 3. 如确需 AutoType，使用严格白名单
// 配置文件 fastjson2.properties：
// fastjson2.autoType.accept=com.example.model.*
// fastjson2.autoType.deny=java.*,javax.*,com.sun.*

// 4. 输入大小限制
// 应用层自行校验
if (json.length() > MAX_JSON_SIZE) {
    throw new IllegalArgumentException("JSON too large");
}
```

### 2.4 Gson 安全说明

```java
// Gson 不支持多态反序列化（@type 机制），
// 因此天然免疫大部分 JSON 反序列化 RCE。
// 但仍有以下注意：

// 1. 避免 GsonBuilder 中注册不受信任的类型适配器
Gson gson = new GsonBuilder()
    .registerTypeAdapter(Object.class, dangerousAdapter)  // ⚠️ 小心
    .create();

// 2. 输入大小限制（自行实现）
```

### 2.5 纵深防御策略

```text
                         ┌─────────────────────┐
                         │   客户端             │
                         └──────────┬──────────┘
                                    │ HTTPS
                         ┌──────────▼──────────┐
层级1: API 网关           │  WAF + 速率限制      │
                         │  请求大小限制         │
                         └──────────┬──────────┘
                                    │
                         ┌──────────▼──────────┐
层级2: 应用校验           │  JSON Schema 校验    │
                         │  字段白名单           │
                         │  输入 sanitization    │
                         └──────────┬──────────┘
                                    │
                         ┌──────────▼──────────┐
层级3: JSON 库安全        │  SafeMode / 禁用多态  │
                         │  StreamReadConstraints│
                         └──────────┬──────────┘
                                    │
                         ┌──────────▼──────────┐
层级4: 运行时沙箱          │  SecurityManager     │
                         │  JVM 安全策略         │
                         │  容器/网络隔离         │
                         └─────────────────────┘
```

> 🎯 **核心要点**：安全不能只靠 JSON 库——纵深防御才是正道。即使 Jackson/Gson 安全记录良好，也要在网关层限制请求大小、在应用层做 Schema 校验、在运行时层做容器隔离。

### 2.6 依赖安全扫描

```xml
<!-- OWASP Dependency Check Maven Plugin -->
<plugin>
    <groupId>org.owasp</groupId>
    <artifactId>dependency-check-maven</artifactId>
    <version>10.0.3</version>
    <configuration>
        <failBuildOnCVSS>7</failBuildOnCVSS>  <!-- CVSS >= 7 构建失败 -->
    </configuration>
    <executions>
        <execution>
            <goals><goal>check</goal></goals>
        </execution>
    </executions>
</plugin>
```

```bash
# 定期运行
mvn dependency-check:check

# 或在 CI 中集成
# GitHub Actions: dependency-check-action
```

---

## 3. JSON Schema 校验体系 {#3}

### 3.1 API 契约式校验

```xml
<dependency>
    <groupId>com.networknt</groupId>
    <artifactId>json-schema-validator</artifactId>
    <version>1.5.0</version>
</dependency>
```

### 3.2 Spring Boot 集成 Schema 校验

```java
// ── 自定义注解 ──
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidJson {
    String schemaPath();    // classpath 下的 Schema 文件路径
}

// ── HandlerMethodArgumentResolver 实现 ──
public class JsonSchemaValidatingResolver
        implements HandlerMethodArgumentResolver {

    private final ObjectMapper mapper;
    private final JsonSchemaFactory schemaFactory;

    @Override
    public Object resolveArgument(MethodParameter param,
            ModelAndViewContainer mav, NativeWebRequest req,
            WebDataBinderFactory binderFactory) throws Exception {

        ValidJson annotation = param.getParameterAnnotation(ValidJson.class);

        // 1. 读取 JSON 请求体
        String body = req.getNativeRequest(HttpServletRequest.class)
            .getReader().lines().collect(Collectors.joining());

        // 2. 加载 Schema
        JsonSchema schema = schemaFactory.getSchema(
            getClass().getResourceAsStream(annotation.schemaPath()));

        // 3. 校验
        Set<ValidationMessage> errors = schema.validate(
            mapper.readTree(body));

        if (!errors.isEmpty()) {
            throw new JsonSchemaValidationException(errors);
        }

        // 4. 通过校验 → 正常反序列化
        return mapper.readValue(body, param.getParameterType());
    }
}
```

```java
// ── 使用 ──
@PostMapping("/users")
public ApiResponse<User> createUser(
        @ValidJson(schemaPath = "/schemas/create-user.schema.json")
        @RequestBody String rawJson) {
    // 自定义解析器已校验，这里直接处理
}
```

### 3.3 Schema 版本化策略

```
resources/schemas/
├── v1/
│   ├── user.schema.json          # v1 版本
│   └── create-order.schema.json
├── v2/
│   ├── user.schema.json          # v2 版本（新增字段）
│   └── create-order.schema.json
└── latest/                        # 符号链接 → 当前版本
```

```java
// API 版本化 + Schema 版本化 = 数据契约演进
@PostMapping("/api/v1/users")
public void createV1(@ValidJson(schemaPath = "/schemas/v1/user.schema.json")
        @RequestBody String json) { }

@PostMapping("/api/v2/users")
public void createV2(@ValidJson(schemaPath = "/schemas/v2/user.schema.json")
        @RequestBody String json) { }
```

---

## 4. 常见问题排查手册 {#4}

### 4.1 序列化无限循环 (StackOverflowError)

```java
// ── 问题：双向引用导致无穷递归 ──
public class Order {
    private List<OrderItem> items;
}
public class OrderItem {
    private Order order;         // ← 回到 Order！
}

// ── 解决方案 ──
// 方案1：@JsonIgnore 切断循环
public class OrderItem {
    @JsonIgnore
    private Order order;
}

// 方案2：@JsonBackReference / @JsonManagedReference
public class Order {
    @JsonManagedReference                          // "我方"
    private List<OrderItem> items;
}
public class OrderItem {
    @JsonBackReference                             // "对方"（不序列化）
    private Order order;
}

// 方案3：@JsonIdentityInfo（通过 ID 引用）
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class,
                  property = "id")
public class Order {
    private Long id;
    private List<OrderItem> items;
}
```

### 4.2 泛型擦除 —— 反序列化得到 LinkedHashMap

```java
// ── 症状 ──
List<User> users = mapper.readValue(json, List.class);
// users.get(0).getClass() → LinkedHashMap，不是 User！

// ── 原因 ──
// Java 泛型在运行时擦除，Jackson 不知道 List 里装什么

// ── 解决方案 ──
List<User> users = mapper.readValue(json,
    new TypeReference<List<User>>() {});   // ✅ 正确
```

### 4.3 日期格式不一致

```java
// ── 症状 ──
// 序列化得到 [2026,7,30,10,30,0]，不是 "2026-07-30 10:30:00"
// 或收到 "2026-07-30T10:30:00"，反序列化报错

// ── 原因 ──
// 未注册 JavaTimeModule 或未禁用 WRITE_DATES_AS_TIMESTAMPS

// ── 解决方案 ──
mapper.registerModule(new JavaTimeModule());
mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

// 或字段级声明
@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
private LocalDateTime createdAt;
```

### 4.4 未知属性处理

```java
// ── 症状 ──
// UnrecognizedPropertyException: Unrecognized field "newField"

// ── 策略 ──
// 开发环境：严格模式（尽早发现字段不匹配）
spring.jackson.deserialization.FAIL_ON_UNKNOWN_PROPERTIES=true

// 生产环境：宽松模式（向前兼容）
spring.jackson.deserialization.FAIL_ON_UNKNOWN_PROPERTIES=false

// 或类级声明
@JsonIgnoreProperties(ignoreUnknown = true)
public class User { ... }
```

### 4.5 布尔/数字类型不匹配

```java
// ── 症状 ──
// JSON 中 "isVip": 1，但 Java 是 boolean isVip
// MismatchedInputException: Cannot deserialize value of type `boolean`

// ── 解决方案 ──
// 方案1：自定义反序列化器
public class NumericBooleanDeserializer
        extends JsonDeserializer<Boolean> {
    @Override
    public Boolean deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException {
        String text = p.getText();
        return "1".equals(text) || "true".equalsIgnoreCase(text);
    }
}

// 方案2：在 DTO 中用 int 接收后转换
```

### 4.6 排查工具速查

| 问题 | 排查思路 |
|------|---------|
| 序列化结果不对 | 检查 `@JsonProperty` 命名、`@JsonIgnore`、`JsonInclude` |
| 反序列化失败 | 打印原始 JSON + 目标类型，检查泛型是否用了 TypeReference |
| 性能瓶颈 | JMH 基准测试，启用 `new ObjectMapper()` 计数 |
| 内存溢出 | 检查是否将整个 JSON 读成 String，改用流式 API |
| 字段为 null | 检查字段名大小写、命名策略、`@JsonProperty` 映射 |

---

## 5. 工程化规范清单 {#5}

### 5.1 团队 JSON 规范

```markdown
## 团队 JSON 开发规范

### 1. 库选型
- ✅ 统一使用 Jackson（Spring Boot 项目）
- ✅ 版本在父 POM `<dependencyManagement>` 中统一管理
- ❌ 禁止多个 JSON 库混用
- 🚫 禁止使用 Fastjson 1.x

### 2. 配置规范
- [ ] ObjectMapper 启用 JavaTimeModule
- [ ] 禁用 WRITE_DATES_AS_TIMESTAMPS
- [ ] 生产环境 FAIL_ON_UNKNOWN_PROPERTIES = false
- [ ] 启用 NON_NULL 属性包含策略

### 3. API 设计
- [ ] 统一响应格式 {code, message, data, timestamp}
- [ ] 金额字段使用字符串类型
- [ ] 日期使用 ISO 8601 格式
- [ ] 分页接口包含 total, page, size, hasNext

### 4. 安全
- [ ] 禁用 enableDefaultTyping
- [ ] 请求体大小限制（如 10MB）
- [ ] JSON Schema 校验关键接口
- [ ] CI 中集成依赖安全扫描

### 5. 代码规范
- [ ] DTO/VO 使用 @Data + Jackson 注解
- [ ] 敏感字段使用 @JsonIgnore
- [ ] 泛型反序列化使用 TypeReference
- [ ] 不要每次 new ObjectMapper()
```

### 5.2 POM 依赖管理

```xml
<!-- 父 POM 中锁定 Jackson 版本 -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.fasterxml.jackson</groupId>
            <artifactId>jackson-bom</artifactId>
            <version>2.18.2</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<!-- 子模块不需要指定版本 -->
<dependencies>
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
    </dependency>
    <dependency>
        <groupId>com.fasterxml.jackson.datatype</groupId>
        <artifactId>jackson-datatype-jsr310</artifactId>
    </dependency>
</dependencies>
```

### 5.3 CI/CD 校验脚本

```bash
#!/bin/bash
# json-standards-check.sh —— CI 中校验 JSON 相关规范

echo "🔍 检查 JSON 安全配置..."

# 1. 检查是否误用了 Fastjson 1.x（排除 fastjson2）
if grep -r "com.alibaba:fastjson:" pom.xml */pom.xml 2>/dev/null |
   grep -v "fastjson2" | grep -v "compatible"; then
    echo "❌ 项目中存在 Fastjson 1.x 依赖，请迁移到 Fastjson2！"
    exit 1
fi

# 2. 检查是否禁用了 enableDefaultTyping
if grep -r "enableDefaultTyping\|enableDefaultTyping" src/ 2>/dev/null; then
    echo "❌ 检测到 enableDefaultTyping，可能存在安全风险！"
    exit 1
fi

# 3. 检查 ObjectMapper 单例
if grep -r "new ObjectMapper()" src/ 2>/dev/null; then
    echo "⚠️  检测到 new ObjectMapper()，建议注入单例"
fi

echo "✅ JSON 安全检查通过"
```

> 🎯 **核心要点**：JSON 开发不是"能跑就行"——安全配置是底线，性能优化是加分项，Schema 校验是进化方向。在团队中建立规范、在 CI 中自动化检查，才能长治久安。

---

**返回总览：** [00-JSON 知识体系总览](./00-JSON知识体系总览.md)
