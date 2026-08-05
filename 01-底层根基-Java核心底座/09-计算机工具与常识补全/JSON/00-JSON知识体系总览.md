# JSON 知识体系总览

> JSON 是 Java 后端开发的"数据"——从 REST API 到配置文件，从序列化到安全防护，每个 Java 开发者都必须精通

## 📚 目录

1. [知识体系导图](#1)
2. [模块导航](#2)
3. [Java JSON 生态全景图](#3)
4. [核心概念速查](#4)
5. [学习路线推荐](#5)
6. [JSON 库选型决策树](#6)

---

## 1. 知识体系导图 {#1}

```
JSON 知识体系（Java 开发者视角）
│
├── 01 JSON 基础与规范
│   ├── JSON 语法规范 (RFC 8259 / ECMA-404)
│   │   ├── 数据类型 (String / Number / Boolean / null / Array / Object)
│   │   ├── 编码规则与转义字符
│   │   └── JSON vs YAML vs XML vs Protobuf
│   ├── JSON 数据结构设计
│   │   ├── 扁平 vs 嵌套结构
│   │   ├── 命名规范 (camelCase vs snake_case)
│   │   └── 常见反模式 (深层嵌套 / 超大数组)
│   ├── JSONPath —— JSON 的 XPath
│   │   ├── 基本表达式 ($ / . / [] / * / ..)
│   │   ├── 过滤表达式 [?(@.price > 10)]
│   │   └── Jayway JsonPath 实战
│   └── JSON Schema
│       ├── Schema 结构定义 (type / properties / required)
│       ├── 数据校验与约束
│       └── 自动生成与工具链
│
├── 02 Jackson 核心实战
│   ├── ObjectMapper 核心配置
│   │   ├── 序列化 / 反序列化特性 (SerializationFeature / DeserializationFeature)
│   │   ├── 属性包含策略 (JsonInclude)
│   │   ├── 命名策略 (PropertyNamingStrategy)
│   │   └── 单例复用与线程安全
│   ├── Jackson 注解完全指南
│   │   ├── @JsonProperty / @JsonAlias / @JsonIgnore
│   │   ├── @JsonFormat / @JsonInclude
│   │   ├── @JsonCreator / @JsonValue
│   │   ├── @JsonTypeInfo / @JsonSubTypes (多态)
│   │   └── @JsonUnwrapped / @JsonAnySetter
│   ├── 泛型与复杂类型处理
│   │   ├── TypeReference<T>
│   │   ├── JavaType / TypeFactory
│   │   └── 嵌套泛型 List<Map<String, User>>
│   ├── 流式 API (Streaming API)
│   │   ├── JsonParser (增量读取)
│   │   └── JsonGenerator (增量写入)
│   ├── 树模型 (Tree Model)
│   │   ├── JsonNode / ObjectNode / ArrayNode
│   │   └── 遍历、修改、创建
│   └── 扩展模块
│       ├── jackson-datatype-jsr310 (Java 8 时间)
│       ├── jackson-datatype-jdk8 (Optional / Stream)
│       ├── jackson-module-kotlin
│       └── jackson-dataformat-xml / yaml / csv
│
├── 03 Gson 与 Fastjson2
│   ├── Gson 核心实战
│   │   ├── GsonBuilder 配置
│   │   ├── TypeToken 泛型处理
│   │   ├── 自定义序列化器 (JsonSerializer / JsonDeserializer)
│   │   ├── @SerializedName / @Expose 注解
│   │   └── Gson vs Jackson 场景选型
│   ├── Fastjson2 实战与迁移
│   │   ├── Fastjson 1.x → 2.x 迁移指南
│   │   ├── JSON.parseObject / JSON.toJSONString
│   │   ├── JSONPath 路径查询
│   │   ├── 安全配置 (SafeMode / AutoType 白名单)
│   │   └── 性能优化配置
│   └── 三库全景对比
│       ├── 性能基准 (JMH)
│       ├── 功能矩阵
│       ├── 安全性对比
│       └── 生态兼容性
│
├── 04 JSON 与 Spring Boot
│   ├── Spring Boot Jackson 自动配置
│   │   ├── JacksonAutoConfiguration 原理
│   │   ├── application.yml 常用配置项
│   │   └── 自定义 ObjectMapper Bean
│   ├── REST API JSON 最佳实践
│   │   ├── @RequestBody / @ResponseBody
│   │   ├── 统一响应格式设计
│   │   ├── 字段过滤 (@JsonView / @JsonFilter)
│   │   └── 空值处理策略
│   ├── 全局异常处理与校验
│   │   ├── HttpMessageNotReadableException
│   │   ├── @Valid + @RequestBody 组合使用
│   │   └── 自定义错误响应格式
│   └── 日期时间处理
│       ├── Java 8 Time API 序列化
│       ├── 时区与时区偏移
│       └── @JsonFormat(pattern) 最佳实践
│
└── 05 JSON 最佳实践与安全
    ├── 性能优化策略
    │   ├── ObjectMapper 单例复用
    │   ├── 流式 API 处理大 JSON
    │   ├── Jackson 2.18 零拷贝数值解析
    │   ├── 异步 + 分块处理
    │   └── 序列化缓存预热
    ├── 安全防护
    │   ├── 反序列化漏洞原理 (RCE via @type)
    │   ├── Fastjson 历史漏洞全景 (CVE-2022-25845 等)
    │   ├── Jackson 安全配置 (enableDefaultTyping 禁用)
    │   ├── 输入大小限制与格式校验
    │   └── 深度防护策略
    ├── JSON Schema 校验
    │   ├── networknt/json-schema-validator
    │   ├── Schema 版本控制策略
    │   └── CI/CD 集成校验
    └── 常见问题排查
        ├── 序列化循环引用 (StackOverflowError)
        ├── 泛型擦除导致类型错误
        ├── 日期格式不一致
        └── 未知属性处理策略
```

---

## 2. 模块导航 {#2}

| 序号 | 模块 | 核心内容 | 适合人群 | 前置要求 |
|:---:|------|---------|---------|---------|
| 01 | JSON 基础与规范 | 语法、数据类型、JSONPath、JSON Schema | 所有 Java 开发者 | 无 |
| 02 | Jackson 核心实战 | ObjectMapper、注解、泛型、流式API、树模型 | 初中级 Java 开发者 | JSON 基础 |
| 03 | Gson 与 Fastjson2 | Gson 实战、Fastjson2 迁移、三库对比 | 中级开发者 | Jackson 基础 |
| 04 | JSON 与 Spring Boot | 自动配置、REST API、异常处理、日期序列化 | Spring Boot 开发者 | Jackson + Spring Boot |
| 05 | JSON 最佳实践与安全 | 性能优化、安全防护、Schema 校验、问题排查 | 中高级开发者 | Jackson + 生产经验 |

---

## 3. Java JSON 生态全景图 {#3}

```
┌──────────────────────────────────────────────────────────────────────────┐
│                         Java JSON 生态全景                                │
│                                                                          │
│  ┌────────────────── JSON 规范层 ──────────────────┐                     │
│  │  RFC 8259 / ECMA-404  │  JSONPath  │  JSON Schema│                     │
│  └──────────────────────────┬──────────────────────┘                     │
│                             │                                            │
│  ┌────────────────── JSON 解析库层 ────────────────┐                     │
│  │                                                  │                    │
│  │  ┌──────────────┐  ┌──────────┐  ┌───────────┐  │                    │
│  │  │   Jackson     │  │   Gson   │  │ Fastjson2  │  │                    │
│  │  │  (企业标准)    │  │ (轻量)   │  │  (高性能)   │  │                    │
│  │  │              │  │          │  │            │  │                    │
│  │  │ 2.19.x       │  │ 2.12.x   │  │ 2.0.x      │  │                    │
│  │  │ Spring 默认   │  │ Google   │  │ 阿里巴巴    │  │                    │
│  │  └──────┬───────┘  └────┬─────┘  └─────┬──────┘  │                    │
│  │         │               │              │         │                    │
│  └─────────┼───────────────┼──────────────┼─────────┘                    │
│            │               │              │                              │
│  ┌─────────┴───────────────┴──────────────┴─────────┐                    │
│  │                  Spring Boot 集成层               │                    │
│  │                                                  │                    │
│  │  JacksonAutoConfiguration                        │                    │
│  │  ├── ObjectMapper 自动装配                        │                    │
│  │  ├── @RequestBody / @ResponseBody 消息转换         │                    │
│  │  ├── application.yml 全局配置                     │                    │
│  │  └── HttpMessageConverter 体系                   │                    │
│  └──────────────────────────────────────────────────┘                    │
│                                                                          │
│  ┌──────────────── 辅助工具层 ────────────────┐                          │
│  │                                            │                          │
│  │  JSON Schema Validator  │  JSON Diff       │                          │
│  │  (networknt)            │  (json-patch)    │                          │
│  │                                            │                          │
│  │  JSON-lib (遗留)  │  Hutool JSON  │  FxTools│                          │
│  └────────────────────────────────────────────┘                          │
└──────────────────────────────────────────────────────────────────────────┘
```

---

## 4. 核心概念速查 {#4}

| 概念 | 一句话解释 | 出现位置 | 典型场景 |
|------|-----------|---------|---------|
| **ObjectMapper** | Jackson 核心类，线程安全，全局单例复用 | 02-模块 | 所有序列化/反序列化 |
| **TypeReference** | Jackson 泛型令牌，解决泛型擦除 | 02-模块 | `List<User>` 反序列化 |
| **JsonNode** | Jackson 树模型节点，动态访问 JSON | 02-模块 | 结构未知的 JSON |
| **@JsonProperty** | 字段-属性名映射，最常用 Jackson 注解 | 02-模块 | 自定义字段名 |
| **@JsonIgnore** | 排除字段不参与序列化 | 02-模块 | 密码、敏感字段 |
| **TypeToken** | Gson 泛型令牌 | 03-模块 | Gson 泛型反序列化 |
| **@type / AutoType** | Fastjson 多态机制，安全风险入口 | 03/05-模块 | ⚠️ 安全高危 |
| **SafeMode** | Fastjson 安全模式，禁用 @type | 03-模块 | 安全防护 |
| **JSONPath** | JSON 查询表达式（类比 XPath） | 01-模块 | 数据提取、校验 |
| **JSON Schema** | JSON 结构规范定义语言 | 01/05-模块 | API 契约校验 |
| **HttpMessageConverter** | Spring MVC JSON 消息转换器 | 04-模块 | REST API |
| **流式 API** | 逐 token 读写，内存友好 | 02-模块 | 超大 JSON 文件 |
| **RCE** | 远程代码执行，JSON 反序列化最严重风险 | 05-模块 | 安全防护 |
| **CVE-2022-25845** | Fastjson 1.x 著名 RCE 漏洞 | 05-模块 | 版本升级依据 |
| **JsonInclude** | 空值/默认值序列化策略 | 02/04-模块 | 精简响应 |
| **JsonView** | 同一对象不同视图的字段序列化 | 04-模块 | API 版本控制 |

---

## 5. 学习路线推荐 {#5}

### 🟢 初级路线：JSON 基础入门（1-2 周）

```
JSON 语法规范 → Jackson ObjectMapper 基本用法 → Spring Boot JSON 开发 → Gson 入门
```

> 目标：能熟练处理 REST API 中的 JSON 请求/响应，掌握 Jackson 常用注解。

### 🟡 中级路线：JSON 深度应用（3-4 周）

```
Jackson 高级特性（泛型/流式/树模型）→ Gson/Fastjson2 对比 → JSON Schema 校验 → 自定义序列化器
```

> 目标：能处理复杂 JSON 场景（嵌套泛型、大文件、多态），理解各库的选型依据。

### 🔴 高级路线：性能优化与安全（3-4 周）

```
性能优化实战（JMH基准测试）→ 反序列化安全防护 → JSON Schema 平台化 → 数据契约治理
```

> 目标：能主导 JSON 库选型、安全审计、性能调优，建立团队级 JSON 规范。

---

## 6. JSON 库选型决策树 {#6}

```
选择哪个 JSON 库？
│
├── Spring Boot 项目？
│   └── → Jackson（默认集成，开箱即用）
│       额外需求：性能极致 → 可搭配 Fastjson2 作为特定场景补充
│
├── Android / 移动端？
│   └── → Gson（轻量 ~240KB，ProGuard 友好）
│
├── 高并发 API 网关，序列化吞吐量第一？
│   └── → Fastjson2（性能领先 20-30%，内存更低）
│       ⚠️ 必须启用 SafeMode，禁用 AutoType
│
├── 需要处理 JSON + XML + YAML + CSV 多格式？
│   └── → Jackson（多格式扩展最完善）
│
├── 微服务 / Serverless，冷启动敏感？
│   └── → Jackson（功能最全）或 FxTools（冷启动快 5-7 倍）
│
├── 遗留系统用 Fastjson 1.x？
│   └── → 立即迁移到 Fastjson2，启用 SafeMode
│
├── 大型 JSON 文件（GB 级）？
│   └── → Jackson 流式 API（JsonParser / JsonGenerator）
│
└── 动态 JSON / Schema 多变？
    └── → Jackson Tree Model (JsonNode) + JSON Schema Validator
```

> 🎯 **核心结论**：Jackson 是 Java 企业开发的"默认选项"——Spring Boot 内置、生态最全、安全可靠。Gson 适合轻量场景，Fastjson2 在极致性能场景是有效补充，但需严格安全配置。

---

**下一模块：** [01-JSON 基础与规范](./01-JSON基础与规范.md)
