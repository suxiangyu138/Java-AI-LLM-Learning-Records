# 04 响应与消息转换：HttpMessageConverter 与 Jackson 3

> 对象怎么变成 JSON、JSON 怎么变回对象——全在 HttpMessageConverter 链。Spring Framework 7 的 Jackson 3 迁移（tools.jackson 包名 + 类名重命名 + hint 机制）是 2026 年升级 Boot 4 后的第一影响面，本模块把转换链与迁移一次讲透

---

## 📚 目录

1. [消息转换器链与工作流程](#1-消息转换器链与工作流程)
2. [Jackson 3 迁移：包名与类名重命名](#2-jackson-3-迁移包名与类名重命名)
3. [SmartHttpMessageConverter 与 hint 机制](#3-smarthttpmessageconverter-与-hint-机制)
4. [Jackson 3 配置定制](#4-jackson-3-配置定制)
5. [内容协商：Accept 与 produces](#5-内容协商accept-与-produces)
6. [JSON 之外的格式：XML / Protobuf](#6-json-之外的格式xml--protobuf)
7. [常见坑与迁移清单](#7-常见坑与迁移清单)

---

## 1. 消息转换器链与工作流程

```java
// HttpMessageConverter 接口（核心契约）
public interface HttpMessageConverter<T> {
    boolean canRead(Class<?> clazz, MediaType mediaType);
    boolean canWrite(Class<?> clazz, MediaType mediaType);
    T read(Class<? extends T> clazz, HttpInputMessage inputMessage);
    void write(T t, MediaType contentType, HttpOutputMessage outputMessage);
}
```

**写入流程（@ResponseBody 场景）：**

```text
返回 User 对象
  → 内容协商：Accept: application/json → 候选媒体类型
  → 遍历转换器链（MappingJackson2JsonHttpMessageConverter 等）
  → canWrite(User, application/json) == true → write()
  → Jackson 序列化 → 响应体
```

| 内置转换器（按注册顺序） | 格式 |
|------------------------|------|
| `JacksonJsonHttpMessageConverter`（原 MappingJackson2...） | JSON（7.x 默认 Jackson 3） |
| `ByteArrayHttpMessageConverter` | 字节数组 |
| `StringHttpMessageConverter` | 纯文本 |
| `FormHttpMessageConverter` | 表单 |
| `MappingJackson2XmlHttpMessageConverter` → `JacksonXmlHttpMessageConverter` | XML（引入 jackson-xml） |
| Protobuf 等 | 按依赖注册 |

> 🎯 **核心要点**：转换器链 = 按"类型 + 媒体类型"匹配的适配器列表。**每个格式（JSON/XML/字节）是一个转换器**，自定义格式就是写一个转换器注册进链。

## 2. Jackson 3 迁移：包名与类名重命名

### 2.1 包名变化（影响所有 import）

```text
Jackson 2：com.fasterxml.jackson.databind.ObjectMapper
Jackson 3：tools.jackson.databind.JsonMapper
```

| 组件 | Jackson 2 | Jackson 3（7.x 默认） |
|------|-----------|---------------------|
| 包 | `com.fasterxml.jackson` | `tools.jackson`（注解仍留 `com.fasterxml.jackson.annotation`） |
| 主类 | `ObjectMapper` | `JsonMapper`（+ YamlMapper/XmlMapper 等） |
| 构建器 | `Jackson2ObjectMapperBuilder`（已无） | `JsonMapper.builder()` |
| 默认行为 | 属性按声明序 | **属性按字母序**（可配 SORT_PROPERTIES_ALPHABETICALLY） |
| 斜杠转义 | 不转义 | **默认转义 `\/`**（影响 ProblemDetail，可配 ESCAPE_FORWARD_SLASHES） |
| 日期 | 时间戳或字符串 | **默认 ISO-8601 字符串**（无需再配 WRITE_DATES_AS_TIMESTAMPS=false） |
| 模块发现 | 手动注册 | `findModules(ClassLoader)` 自动注册 |

### 2.2 Spring MVC 类名重命名（7.0）

| 旧（Jackson 2 时代） | 新（7.0） |
|---------------------|----------|
| `MappingJackson2HttpMessageConverter` | `JacksonJsonHttpMessageConverter` |
| `MappingJackson2SmileHttpMessageConverter` | `JacksonSmileHttpMessageConverter` |
| `MappingJackson2CborHttpMessageConverter` | `JacksonCborHttpMessageConverter` |
| `MappingJackson2XmlHttpMessageConverter` | `JacksonXmlHttpMessageConverter` |
| `MappingJackson2YamlHttpMessageConverter` | `JacksonYamlHttpMessageConverter` |
| `MappingJackson2JsonView` | `JacksonJsonView` |
| `AbstractJackson2HttpMessageConverter` | `AbstractJacksonHttpMessageConverter` |
| `Jackson2ObjectMapperBuilder` | 无对应（用 Jackson 原生 builder） |

> ⚠️ **迁移提醒**：7.0 默认支持 Jackson 3（类路径有 `tools.jackson` 时），Jackson 2 作为 fallback 仍可用但 deprecated（7.1 计划禁用自动探测、7.2 移除）。**新代码一律用 Jackson 3 类名**；升级后先跑接口回归（字段顺序、斜杠转义、日期格式三类差异最容易漏）。

## 3. SmartHttpMessageConverter 与 hint 机制

7.0 用 `SmartHttpMessageConverter`（支持 hints）取代旧的 `GenericHttpMessageConverter` 包装方案：

```text
旧方案：MappingJacksonValue / MappingJacksonInputMessage 包装对象传附加信息
新方案：转换时携带 Hints（@JsonView / FilterProvider 等意图）→ 转换器自行解读
```

```java
// @JsonView 场景（7.0 通过 hint 生效）
public class UserViews {
    public interface Basic { }        // 基础视图
    public interface Detail extends Basic { }   // 详情视图（含基础）
}

public class User {
    @JsonView(UserViews.Basic.class)
    private Long id;
    @JsonView(UserViews.Basic.class)
    private String name;
    @JsonView(UserViews.Detail.class)
    private String email;      // 敏感字段：只在 Detail 视图输出
}

// 控制器：指定视图
@RestController
public class UserController {
    @GetMapping("/api/users/{id}")
    @JsonView(UserViews.Basic.class)        // 基础信息（脱敏）
    public User get(@PathVariable Long id) { ... }

    @GetMapping("/api/users/{id}/detail")
    @JsonView(UserViews.Detail.class)       // 完整信息
    public User detail(@PathVariable Long id) { ... }
}
```

| 能力 | 说明 |
|------|------|
| `@JsonView` | 按视图选择性序列化字段（脱敏/分层输出） |
| `FilterProvider` | 动态属性过滤 |
| `RequestBodyAdvice.determineReadHints` | 读方向 hint 定制（7.0 新钩子） |
| `ResponseBodyAdvice.determineWriteHints` | 写方向 hint 定制（7.0 新钩子） |

> 💡 **实践**：@JsonView 是"同实体多视图输出"的标准答案（列表精简、详情完整、脱敏字段隔离），比"写两个 DTO"省一半代码。

## 4. Jackson 3 配置定制

```yaml
spring:
  jackson:
    default-property-inclusion: non_null     # null 字段不输出
    time-zone: Asia/Shanghai
    deserialization:
      fail-on-unknown-properties: false      # 未知字段容忍
    serialization:
      write-dates-as-timestamps: false       # 7.x 默认已是 ISO-8601
```

```java
// Boot 4：JsonMapperBuilderCustomizer（对标旧 Jackson2ObjectMapperBuilderCustomizer）
@Bean
JsonMapperBuilderCustomizer jsonCustomizer() {
    return builder -> builder
            .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, false)  // 恢复声明序
            .configure(JsonWriteFeature.ESCAPE_FORWARD_SLASHES, false)       // 关闭斜杠转义
            .enable(SerializationFeature.INDENT_OUTPUT);
}
```

| 常见需求 | 配置 |
|---------|------|
| null 不输出 | `default-property-inclusion: non_null` |
| 未知字段容忍 | `fail-on-unknown-properties: false` |
| 日期格式 | Jackson 3 默认 ISO-8601（LocalDateTime → `2026-08-07T19:30:00`） |
| 驼峰/下划线互转 | `property-naming-strategy: SNAKE_CASE` |
| 字段顺序恢复声明序 | `SORT_PROPERTIES_ALPHABETICALLY=false` |

> ⚠️ **日期注意**：Jackson 3 默认 ISO-8601 字符串输出——**依赖时间戳格式（epoch）的旧客户端在升级后解析会失败**；LocalDateTime 无时区语义，跨时区接口用 `@JsonFormat(pattern, timezone)` 显式声明。

## 5. 内容协商：Accept 与 produces

```text
请求 Accept: application/json;q=0.9, application/xml;q=0.8
  → ContentNegotiationManager 解析
  → 与候选转换器（produces 声明 + 注册转换器）交集
  → 无交集 → 406 Not Acceptable
```

| 配置 | 行为 |
|------|------|
| `produces`（02 篇） | 方法级候选声明（第一优先级） |
| 转换器注册表 | 全局可用格式 |
| 参数协商（`?format=json`） | 可配置开启（默认关闭） |
| 默认回退 | 无 Accept 时按 produces/首注册转换器 |

> 💡 **实战**：只做 JSON 的 API 直接所有方法显式 `produces=JSON`——内容协商变成"纯 JSON"，行为完全确定，406 只在"客户端要了不该要的"时出现。

## 6. JSON 之外的格式：XML / Protobuf

```xml
<!-- XML：引入 jackson-xml（Boot 版本管理） -->
<dependency>
    <groupId>com.fasterxml.jackson.dataformat</groupId>
    <artifactId>jackson-dataformat-xml</artifactId>
</dependency>
```

```java
// 同一接口返回 JSON 或 XML（Accept 驱动）
@GetMapping(value = "/api/report", produces = {
        MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
public Report report() { ... }
```

| 格式 | 转换器 | 场景 |
|------|--------|------|
| JSON | JacksonJsonHttpMessageConverter | **默认主流** |
| XML | JacksonXmlHttpMessageConverter | 老系统/第三方对接 |
| Protobuf | ProtobufHttpMessageConverter | 高性能二进制（内部服务） |
| YAML | JacksonYamlHttpMessageConverter | 配置类接口 |

> 🎯 **要点**：多格式支持 = 依赖 + 转换器自动注册 + produces 声明，三步即可；**二进制格式（Protobuf）在内部高性能链路上是 JSON 的强替代**。

## 7. 常见坑与迁移清单

| 坑 | 现象 | 处理 |
|----|------|------|
| 升级后 JSON 字段顺序乱 | 字母序（Jackson 3 默认） | 配 SORT_PROPERTIES_ALPHABETICALLY=false |
| URL 含斜杠变 `\/` | 转义默认开启 | 配 ESCAPE_FORWARD_SLASHES=false |
| 日期变字符串 | ISO-8601 默认 | 客户端升级或 @JsonFormat 定制 |
| 循环引用 | 序列化栈溢出 | `@JsonIgnoreProperties`/@JsonManagedReference/视图 |
| LocalDateTime 序列化失败 | 缺 jsr310 模块（旧版） | 7.x 已内置 |
| 接口返回类型是 Object/Map | 丢失类型信息 | 用具体类型/泛型 |
| 自定义 ObjectMapper 被覆盖 | Bean 冲突 | 用 JsonMapperBuilderCustomizer 而非新 Bean |

**迁移检查清单（Boot 3→4）：**

- [ ] import 更新：`com.fasterxml.jackson` → `tools.jackson`（注解除外）
- [ ] 类名更新：MappingJackson2... → Jackson...
- [ ] ObjectMapper → JsonMapper（builder 构建）
- [ ] 回归接口响应：字段顺序 / 斜杠转义 / 日期格式 / null 处理
- [ ] @JsonView 场景验证 hint 生效（7.0 新机制）
- [ ] 自定义序列化器/反序列化器适配 Jackson 3 API

> 🎯 **核心要点**：消息转换层 = 转换器链（类型×媒体类型匹配）+ Jackson 3（新包名/新类名/新默认行为）+ hint 机制（@JsonView 等意图传递）。升级 Boot 4 后**第一个要回归的就是接口响应**——字段顺序、斜杠转义、日期格式三个默认值变化足以让一堆客户端悄悄坏掉。

---

**上一模块**：[03-参数绑定与数据校验](03-参数绑定与数据校验.md)　**下一模块**：[05-异常处理：@ExceptionHandler与ProblemDetail](05-异常处理：@ExceptionHandler与ProblemDetail.md)
