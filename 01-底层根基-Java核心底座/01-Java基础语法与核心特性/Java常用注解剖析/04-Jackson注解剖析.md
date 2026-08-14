# 04 Jackson 注解剖析

> Jackson 是 Java 世界 JSON 序列化的事实标准，它的注解体系回答了三个问题：字段与 JSON 键怎么对应（@JsonProperty/@JsonAlias）、哪些不参与序列化（@JsonIgnore 家族）、值怎么变形（@JsonFormat/@JsonInclude/@JsonView）。2026 年的主线是 Jackson 3 迁移——注解包不动、核心 API 全换、默认行为三处反转

## 📚 目录

1. [Jackson 3 迁移全景：什么变了什么没变](#1-jackson-3-迁移全景什么变了什么没变)
2. [映射与忽略：@JsonProperty / @JsonAlias / @JsonIgnore](#2-映射与忽略jsonproperty--jsonalias--jsonignore)
3. [格式与空值：@JsonFormat / @JsonInclude](#3-格式与空值jsonformat--jsoninclude)
4. [视图、多态与定制序列化](#4-视图多态与定制序列化)
5. [高频坑与面试题](#5-高频坑与面试题)

---

## 1. Jackson 3 迁移全景：什么变了什么没变

Jackson 3（2025 年 GA，Spring Boot 4 起为默认版本）的迁移设计很聪明：**核心实现类搬家，注解原地不动**。`ObjectMapper`、`JsonNode` 等从 `com.fasterxml.jackson.databind` 迁至 `tools.jackson.databind`（对应 Maven 坐标 `tools.jackson.core`），但 `@JsonProperty`、`@JsonIgnore` 等注解**仍留在 `com.fasterxml.jackson.annotation` 包**——Jackson 2.x 与 3.x 共享 2.20+ 的注解 JAR，存量 DTO 的注解 import 一行都不用改。

例外只有两个注解：`@JsonSerialize`/`@JsonDeserialize` 移到 `tools.jackson.databind.annotation`（它们引用核心 API 类型，必须跟着搬家）。官方迁移指南给出机械规则：`com.fasterxml.jackson.` → `tools.jackson.`，**除 `com.fasterxml.jackson.annotation` 外**。

比搬家更重要的是三处默认行为反转（Boot 4 升级后最常见的事故源）：

1. **日期序列化默认 ISO-8601 字符串**：`WRITE_DATES_AS_TIMESTAMPS` 由开变关，`Date` 不再输出 epoch 毫秒——前端拿到的时间格式全变；
2. **未知属性不再报错**：`FAIL_ON_UNKNOWN_PROPERTIES` 由开变关，上游多传字段被静默忽略；
3. **null 赋给原始类型报错**：`FAIL_ON_NULL_FOR_PRIMITIVES` 由关变开，`{"count":null}` 反序列化到 `int count` 直接失败。

`ObjectMapper` 被不可变、builder 风格的 `JsonMapper.builder()...build()` 取代，异常体系改为非受检的 `JacksonException`。生产迁移建议：先跑一遍序列化快照对比测试，再按需逐项回滚默认值。

---

## 2. 映射与忽略：@JsonProperty / @JsonAlias / @JsonIgnore

**@JsonProperty** 是出现频率最高的 Jackson 注解，四个属性：

- `value`：字段与 JSON 键名不一致时的映射（`@JsonProperty("user_id")`）；
- `required`：仅影响 Schema 生成与文档，**不参与运行时校验**——这是最常见的误解；
- `access`：READ_ONLY/WRITE_ONLY/READ_WRITE 控制序列化或反序列化方向；
- `defaultValue`：同样只进 Schema，不参与实际赋值。

**@JsonAlias** 只作用于**反序列化**：为字段登记别名键，序列化时仍写正式名。典型场景：接口从 `userName` 迁移到 `name`，兼容期 `@JsonAlias("userName")` 让旧客户端继续工作，响应则只输出 `name`。

**@JsonIgnore** 家族按作用域分层：

- `@JsonIgnore` 字段级完全排除，可加 `value = false` 反转为"不忽略"（配合全局忽略策略做例外放行）；
- `@JsonIgnoreProperties(ignoreUnknown = true)` 类级，同时声明"忽略未知属性"（应对 Jackson 2 默认报错的经典配置，Jackson 3 下已非必需）；
- `@JsonIgnoreProperties` 的 `allowGetters/allowSetters` 实现"序列化要、反序列化不要"的细粒度方向控制；
- `@JsonIgnoreType` 类级，把某个类型整体排除（如工具类混入 DTO 的场景）。

> ⚠️ **Lombok 交互坑**：`@Jacksonized` 与显式 `@JsonIgnore` 共存时，Lombok 1.18.46 之前会停止生成 @JsonProperty——升级到 1.18.46+ 修复（#4022）。

### 2.4 其余映射注解速览

- `@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)`：类级批量命名策略，字段 `userId` 自动映射 `user_id`，替代逐字段 @JsonProperty——对接下划线风格外部 API 的首选；
- `@JsonPropertyOrder(alphabetic = true)` 或显式字段顺序：稳定输出顺序（对缓存 key、哈希签名、快照测试至关重要，Jackson 3 甚至新增 `SORT_PROPERTIES_ALPHABETICALLY` 默认开关）；
- `@JsonUnwrapped`：内嵌对象字段**平铺**到父级 JSON（`User.Address` 平铺为 `{"city":...}`），反序列化同样重组——嵌套 DTO 组合利器；
- `@JsonAnyGetter/@JsonAnySetter`：把 `Map<String, Object>` 字段的条目**摊开**为同级 JSON 键（动态字段场景，如网关透传未知字段）；setter 侧可配合 `@JsonAnySetter` 捕获所有未匹配键；
- `@JsonRootName`：包装根节点（配合 `SerializationFeature.WRAP_ROOT_VALUE`，Spring 默认关闭）。

### 2.5 TypeReference：泛型反序列化的类型令牌

泛型信息在编译后擦除，`readValue(json, List<User>.class)` 无法表达"List 里的 User"，直接传 `List.class` 会反序列化成 `List<LinkedHashMap>`。标准解法是匿名子类捕获类型参数：

```java
List<User> users = mapper.readValue(json, new TypeReference<List<User>>() {});
```

原理：匿名子类的泛型父类签名保留在字节码的 Signature 属性里，Jackson 反射读出实际类型参数。Spring 的 `RestClient`/`WebClient` 调用 `bodyToMono(List.class)` 时同样踩坑，需传 `ParameterizedTypeReference`（同源机制）。

---

## 3. 格式与空值：@JsonFormat / @JsonInclude

**@JsonFormat** 最常用于日期，三个属性构成完整姿势：

```java
@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
private LocalDateTime createdAt;   // 需 jackson-datatype-jsr310
```

关键认知：Jackson 2 中 `Date` 默认走时间戳（配合 `WRITE_DATES_AS_TIMESTAMPS`），标注 pattern 才输出字符串；Jackson 3 默认即 ISO-8601，pattern 只在需要自定义格式时出现。`LocalDateTime` 等 JSR-310 类型必须引入 `jackson-datatype-jsr310` 模块并注册 `JavaTimeModule`（Spring Boot 自动注册），否则报 `InvalidDefinitionException`。`shape` 属性还可把枚举输出为 `STRING`/`NUMBER`/`OBJECT`。

时间戳与字符串的另一个隐患是**时区漂移**：pattern 未配 `timezone` 时按 JVM 默认时区序列化，容器镜像的时区（常为 UTC）与业务时区不一致会造成"日期少 8 小时"类事故——**@JsonFormat 的 timezone 必须显式**，或全局配置 `spring.jackson.time-zone=GMT+8`。`@JsonFormat` 还支持数值型 `shape = Shape.STRING` 输出字符串数字（避免大数值前端精度丢失），以及 `lenient` 控制宽松解析。

**@JsonInclude** 控制"什么时候这个字段不出现"，取值按严格度递增：`ALWAYS`（总是出现，含 null）→ `NON_NULL` → `NON_ABSENT`（Optional 为空也不出现）→ `NON_EMPTY`（空串/空集合/空数组也不出现）→ `NON_DEFAULT`（等于默认值不出现）→ `NON_EMPTY` 之上的 `CUSTOM`。选型逻辑：对外 API 响应用 NON_NULL 瘦身、内部日志落库用 ALWAYS 保留全貌。可配置到类级（整类生效）或全局默认（JsonMapper builder 上设置），优先级：字段 > 类 > 全局。

---

## 4. 视图、多态与定制序列化

**@JsonView** 实现"同一对象不同接口不同字段"：接口侧定义视图接口（如 `Views.List.class`/`Views.Detail.class`），字段标注所属视图，序列化时 `writeValueAsString` 指定视图。坑在 `MapperFeature.DEFAULT_VIEW_INCLUSION`：默认**未标注 @JsonView 的字段在所有视图下都输出**，关闭该特性后未标注字段被排除——团队必须二选一并写进规范，否则视图过滤形同虚设。

**多态**用 `@JsonTypeInfo(use = Id.NAME, include = As.PROPERTY, property = "type")` + `@JsonSubTypes({@Type(value = Cat.class, name = "cat")...})`。安全提示：`ObjectMapper` 的 default typing 曾造成多起反序列化 RCE（2017 CVE 系列），永远显式指定白名单类型，不用全局 default typing。

**定制序列化**两条路线：轻量用 `@JsonSerialize(using = MySerializer.class)` 单字段挂自定义逻辑（脱敏、加密、格式化），Jackson 3 中自定义类改为继承 `ValueSerializer`（替代 `JsonSerializer`）；重量用 `@JsonCreator` 接管整个构造过程——标注在构造器/静态工厂上，配合参数上的 @JsonProperty 完成不可变对象（record、@Value 类）反序列化，record 类型 Jackson 自动按规范构造器处理，无需额外注解。

枚举序列化的惯用法是 `@JsonValue` 标注枚举的某个 getter，使序列化输出该值（如 code）而非枚举名。**@JsonMixIn** 是"不改源码定制映射"的注解：当 DTO 属于第三方库或字段污染严重时，定义一个 MixIn 接口/类承载 Jackson 注解，注册后合并进目标类的注解视图——`mapper.addMixIn(ThirdPartyUser.class, UserMixIn.class)`。它的价值场景：为第三方类加忽略/重命名、为同一个类维护多套映射视图（不同接口不同 MixIn）、老代码无注解时的兼容补丁。代价是注解与类分离，可读性下降，只应在无法直接标注时使用。

属性发现机制的底层逻辑值得记牢：Jackson 按 **getter/setter > 字段 > record 组件** 的顺序发现属性，@JsonProperty 显式标注字段会覆盖命名冲突。record 的组件名自动成为属性名；`@JsonAutoDetect` 类级注解可整体调整发现策略（如只认字段不认 getter），排查"字段明明在却不输出"时从发现机制入手。

---

## 5. 高频坑与面试题

1. **@JsonProperty(required=true) 不校验**：只影响 Schema，运行时不会抛错——校验必须走 Jakarta 校验体系（见 05 篇）；
2. **泛型反序列化丢类型**：`readValue(json, List<User>.class)` 根本编译不过；`List.class` 会变成 `LinkedHashMap`——用 `TypeReference<List<User>>` 或 `TypeFactory.constructCollectionType`；
3. **@JsonIgnore 加在 getter 上同时影响序列化与反序列化**，且与字段级注解叠加时可能冲突——ignore 类注解统一加在字段上最可预期；
4. **日期三大坑**：忘加 jsr310 模块、pattern 无 timezone 导致时区漂移、Jackson 3 默认格式反转导致的接口快照变化；
5. **面试必答框架**：「Jackson 3 迁移改了什么？」——核心 API 迁 `tools.jackson.*`、注解包原地不动、@JsonSerialize/@JsonDeserialize 例外搬家、JsonMapper 取代 ObjectMapper、三处默认行为反转；「@JsonIgnore 与 @JsonIgnoreProperties 区别？」——字段级 vs 类级，前者彻底排除，后者可控制 ignoreUnknown 与读写方向；
6. **「@JsonInclude 的取值怎么选？」**——按严格度递增回答：ALWAYS（日志/落库保留全貌）→ NON_NULL（对外 API 瘦身）→ NON_EMPTY（字符串/集合空值也裁掉）→ NON_DEFAULT，并说明优先级"字段 > 类 > 全局默认"；
7. **「反序列化安全怎么防？」**——三条：关闭 default typing（历史上多次 RCE 的根源，多态必须 @JsonTypeInfo 白名单）；`StreamReadConstraints` 限制最大字符串长度/嵌套深度（防炸弹 JSON）；未知属性按 FAIL_ON_UNKNOWN_PROPERTIES 策略决定严格或宽松。

---

**下一模块**：[05 Jakarta 校验注解剖析](./05-Jakarta校验注解剖析.md) · **返回总览**：[00 总览](./00-Java常用注解知识体系总览.md)

**相关体系**：[SpringBoot Web（JSON 消息转换器链路）](../../../02-后端核心技术%20微服务%20分布式%20云原生/09-Web开发全流程/SpringBoot%20Web/00-SpringBootWeb总览.md)

---

【参考来源】
- [Jackson 3 迁移指南（官方 MIGRATING_TO_JACKSON_3.md，含注解包不变声明）](https://github.com/FasterXML/jackson/blob/master/jackson3/MIGRATING_TO_JACKSON_3.md)
- [Spring Boot 4 升级 Jackson 3 详解（包名变更/日期格式/配置迁移）](https://yunpan.plus/t/7941-1-1)
- [Spring Boot 4 实战：Jackson 2.x 升级 3.x 踩坑记录](https://www.jb51.net/program/363267zxn.htm)
- [Lombok Changelog（1.18.46：@Jacksonized 与 @JsonIgnore 修复 #4022）](https://projectlombok.org/changelog)
- [projectlombok/lombok Issue #4004（Jackson 3.0.3 字段发现行为）](https://github.com/projectlombok/lombok/issues/4004)
