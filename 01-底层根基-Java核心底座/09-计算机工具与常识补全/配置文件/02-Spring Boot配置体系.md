# 02 - Spring Boot 配置体系

> **核心摘要**：Spring Boot 的配置体系 = 配置文件 + 注入方式 + 绑定机制三件套。本文深度拆解 application.yml 规范、@Value 与 @ConfigurationProperties 的对比取舍、占位符机制、松散绑定与类型安全校验。

> **前置阅读**：[[01-配置文件格式详解]]

---

## 📚 目录

1. [Spring Boot 配置体系全景](#1-spring-boot-配置体系全景)
2. [application.yml 规范](#2-applicationyml-规范)
3. [@Value：单值注入](#3-value单值注入)
4. [@ConfigurationProperties：类型安全绑定](#4-configurationproperties类型安全绑定)
5. [占位符与属性引用](#5-占位符与属性引用)
6. [松散绑定与类型转换](#6-松散绑定与类型转换)
7. [配置校验与默认值](#7-配置校验与默认值)
8. [常见陷阱与排查](#8-常见陷阱与排查)
9. [核心要点](#9-核心要点)

---

## 1. Spring Boot 配置体系全景

> **背景**：Spring Boot 的「自动配置」哲学延伸到配置——开发者只需提供属性值，框架负责绑定与生效。
> **目的**：一套代码 + 外部配置 = 任意环境运行（外部化配置的落地载体）。
> **适用范围**：Spring Boot 2.x/3.x 全部 Java 服务。
> **前提假设**：配置通过 PropertySource 机制统一注入，支持多来源覆盖（详见 03 篇）。

```
Spring Boot 配置三件套
┌─────────────────────────────────────────┐
│ ① 配置文件：application.yml / properties │ ← 声明配置
├─────────────────────────────────────────┤
│ ② 注入方式：                            │
│    ├── @Value("${key}")      单值注入    │ ← 读取配置
│    ├── @ConfigurationProperties 绑定对象 │
│    └── Environment 编程式获取            │
├─────────────────────────────────────────┤
│ ③ 绑定机制：占位符/松散绑定/类型转换/校验  │ ← 处理配置
└─────────────────────────────────────────┘
```

---

## 2. application.yml 规范

### 2.1 文件结构与命名

```yaml
# application.yml —— 基础配置（所有环境共享的默认值）
server:
  port: 8080

spring:
  application:
    name: order-service          # 服务名（Nacos/日志/监控都用它）
  profiles:
    active: dev                  # 激活环境（生产用环境变量覆盖！）
  datasource:
    url: jdbc:mysql://localhost:3306/order_db
    username: root
    password: ${DB_PASSWORD}     # 敏感信息用占位符（外部注入！）

myapp:
  timeout-ms: 3000               # 松散绑定：timeout-ms ↔ timeoutMs
  retry-count: 3
  enabled: true
```

### 2.2 文件命名规则（易错点）

```text
配置文件命名与位置（Spring Boot 标准）
├── application.yml              # 基础配置（classpath 根）
├── application-dev.yml          # dev 环境覆盖
├── application-prod.yml         # prod 环境覆盖
├── application-{profile}.yml    # 规则：环境名后缀
├── bootstrap.yml                # 引导配置（配置中心地址，最低优先级）
├── META-INF/spring/xxx.imports  # Spring Boot 3.x 自动配置
└── 位置优先级：外部 config/ > 外部根 > classpath:/config/ > classpath:/

⚠️ 命名陷阱：
├── application-dev.yml 与 spring.profiles.active=dev 必须完全对应
├── 文件后缀混乱（.yaml 与 .yml 混用）→ 可能加载不到
└── bootstrap.yml 在纯 Spring Boot 3.x 不生效（需 Spring Cloud 依赖）
```

### 2.3 配置分段建议（团队规范）

```yaml
# 按域组织（大型项目规范）
server:                    # 服务层
  port: 8080

spring:                    # 框架层
  datasource: { ... }
  redis: { ... }

myapp:                     # 业务层（自定义配置统一前缀 myapp）
  order: { ... }
  payment: { ... }

logging:                   # 日志层
  level:
    root: info
    com.example: debug
```

> 🎯 **规范**：自定义配置统一前缀（`myapp.*`/`app.*`）——避免与框架配置混在一起，也避免与第三方库配置 key 冲突。

---

## 3. @Value：单值注入

### 3.1 基本用法

```java
@Service
public class OrderService {

    // 简单注入
    @Value("${myapp.timeout-ms}")
    private int timeoutMs;

    // 带默认值（配置缺失时的兜底——推荐！）
    @Value("${myapp.retry-count:3}")
    private int retryCount;

    // 嵌套属性
    @Value("${myapp.order.status:PAYING}")
    private String defaultStatus;

    // 占位符引用其他属性
    @Value("${myapp.base-url:${server.port:8080}}")
    private String baseUrl;
}
```

### 3.2 适用边界（什么时候用它）

```text
@Value 适合：
├── 单个零散配置（1-3 个值）
├── 快速原型
└── 无类型校验要求

@Value 不适合（升级信号）：
├── 配置超过 5 个 → 绑定对象更清晰
├── 需要校验/嵌套/复用 → @ConfigurationProperties
├── 同一配置多处引用 → 常量类收敛
└── 生产级项目 → 统一 @ConfigurationProperties（企业规范）
```

### 3.3 @Value 的已知陷阱

| # | 陷阱 | 表现 | 解法 |
|---|------|------|------|
| 1 | **缺失即启动失败** | `${key}` 无默认值且缺失 → 启动报错 | 加默认值 `${key:default}` |
| 2 | **类型转换错误** | 配置值无法转 int → 启动报错 | 校验配置 + 错误信息含 key |
| 3 | **静态字段失效** | `@Value` 加在 static 字段上不注入 | 用实例字段或 setter |
| 4 | **不可变类失效** | final 字段不注入 | 用构造函数注入（Spring 4.3+） |
| 5 | **刷新不生效** | Nacos 动态刷新对 @Value 需 @RefreshScope | 或用 @ConfigurationProperties |

```java
// 陷阱 4 解法：构造器注入
@Service
public class OrderService {
    private final int timeoutMs;

    public OrderService(@Value("${myapp.timeout-ms:3000}") int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }
}
```

---

## 4. @ConfigurationProperties：类型安全绑定

### 4.1 基本用法（推荐方案）

```java
@Component
@ConfigurationProperties(prefix = "myapp")   // 绑定 myapp.* 前缀
@Validated                                // 开启校验
public class AppProperties {

    @NotNull
    private String name;                  // myapp.name

    @Min(1)
    private int timeoutMs = 3000;         // myapp.timeout-ms（带默认值）

    private boolean enabled = true;       // myapp.enabled

    private final Retry retry = new Retry();   // 嵌套对象（myapp.retry.*）

    private List<String> hosts = new ArrayList<>();  // 列表

    private Map<String, Integer> limits = new HashMap<>();  // 映射

    // getter/setter 必须（Spring 绑定需要）
    // 嵌套类
    public static class Retry {
        private int count = 3;
        private int delayMs = 100;
        // getter/setter
    }
}
```

```yaml
# 对应配置
myapp:
  name: order-service
  timeout-ms: 3000
  enabled: true
  retry:
    count: 5
    delay-ms: 200
  hosts:
    - node1.internal
    - node2.internal
  limits:
    query: 100
    batch: 1000
```

### 4.2 三种注册方式

```java
// 方式 1：@Component + @ConfigurationProperties（最简）
@Component
@ConfigurationProperties(prefix = "myapp")
public class AppProperties { ... }

// 方式 2：@EnableConfigurationProperties（推荐，配置类管理）
@Configuration
@EnableConfigurationProperties(AppProperties.class)
public class AppConfig { ... }

// 方式 3：@Bean（灵活）
@Bean
@ConfigurationProperties(prefix = "myapp")
public AppProperties appProperties() { return new AppProperties(); }
```

### 4.3 与 @Value 的对比（权衡决策）

| 维度 | @Value | @ConfigurationProperties |
|------|:---:|:---:|
| 绑定方式 | 逐字段 | 整对象 |
| 类型安全 | 运行时转换 | **编译期对象 + 强类型** |
| 校验支持 | ❌ | ✅ @Validated + 校验注解 |
| 嵌套结构 | 手写前缀拼 | **自动嵌套绑定** |
| 列表/Map | 手动解析 | **原生支持** |
| 动态刷新 | 需 @RefreshScope | ✅ 配合刷新 |
| 可测试性 | 差 | **好（直接 new + setter）** |
| 2026 推荐 | 少量零散值 | **生产项目首选** |

> 🎯 **企业规范结论**：**业务配置一律 @ConfigurationProperties + @Validated**；@Value 只用于零散单值（端口/超时）；代码中禁止到处 @Value 散落。

---

## 5. 占位符与属性引用

### 5.1 三种占位符用法

```yaml
# ① 默认值
app:
  timeout: ${myapp.timeout:3000}        # myapp.timeout 缺失时用 3000

# ② 引用其他属性
app:
  base-url: http://${server.host:localhost}:${server.port:8080}

# ③ 环境变量引用（部署注入）
app:
  password: ${DB_PASSWORD}              # 来自环境变量（必填，缺失启动失败）
  token: ${API_TOKEN:}                  # 空默认值（缺失 = 空串）
```

### 5.2 随机值（测试/演练场景）

```yaml
myapp:
  instance-id: ${random.uuid}            # 随机 UUID
  port-range: ${random.int(1000,9999)}   # 随机整数
```

### 5.3 占位符陷阱

```text
占位符常见陷阱
├── ① 嵌套占位符层级过深 → 解析失败（Spring 支持有限层）
├── ② 环境变量缺失无默认值 → 启动失败（by design，但报错信息模糊）
├── ③ 中文字符串占位符 → 编码问题
└── ④ 循环引用（A 引用 B，B 引用 A）→ 启动失败
排查：启动日志会打印 "Could not resolve placeholder 'xxx'"
```

---

## 6. 松散绑定与类型转换

### 6.1 松散绑定规则（宽松绑定）

> 🎯 **松散绑定**：配置文件中的 key 与 Java 字段名之间的宽松匹配规则——环境变量、命令行、配置文件写法不同但能绑定到同一字段：

```text
字段 timeoutMs 可被以下写法绑定：
├── myapp.timeout-ms       ← kebab-case（配置文件标准写法）
├── myapp.timeoutMs        ← camelCase（Java 风格）
├── myapp.timeout_ms       ← snake_case
├── MYAPP_TIMEOUTMS        ← 环境变量（全大写）
└── myapp.TIMEOUTMS        ← 命令行

⚠️ 注意：@Value 不适用松散绑定！@Value 的 ${key} 必须精确匹配。
```

### 6.2 类型转换支持

```text
Spring 自动类型转换（开箱即用）
├── 基本类型：int/long/boolean/double
├── 时间：Duration（"30s"/"5m"）、Period
├── 大小：DataSize（"10MB"）
├── 枚举：Enum.valueOf
├── List/Map/Set：自动集合转换
└── 自定义：Converter Bean 注册

示例：
myapp:
  cache-ttl: 30s                    # → Duration.ofSeconds(30)
  max-file-size: 10MB               # → DataSize.ofMegabytes(10)
  mode: fast                        # → Mode.fast（枚举）
```

---

## 7. 配置校验与默认值

### 7.1 校验实战（fail-fast 原则）

```java
@Component
@ConfigurationProperties(prefix = "myapp")
@Validated
public class AppProperties {

    @NotBlank
    private String name;                    // 必填，空白报错

    @Min(1) @Max(86400)
    private int timeoutMs = 3000;           // 范围校验

    @Valid
    private Retry retry = new Retry();      // 嵌套校验

    @Pattern(regexp = "^[a-z-]+$")
    private String env = "dev";             // 格式校验

    @AssertTrue
    private boolean valid = true;

    public static class Retry {
        @Min(0) @Max(10)
        private int count = 3;
        // ...
    }
}
```

### 7.2 校验失败的行为

```text
校验失败时的行为（3.x 默认）
├── 启动失败：BindValidationException（fail-fast，防止带病上线）✅
├── 定位信息：属性路径（myapp.timeoutMs）+ 违规原因
├── 可选：宽松模式（spring.bind.handler.ignore-invalid-fields=true）
│   └── 不推荐——错误配置应尽早暴露
└── 生产建议：校验是配置质量的第一道防线，必开
```

### 7.3 默认值策略

```text
默认值三原则
├── ① 默认值写在 Java 字段（属性初始化），不写在配置文件
│   → 配置文件只放「与环境相关的值」，通用默认值代码化
├── ② 敏感配置无默认值（缺失即报错，防止静默用错值）
└── ③ 开关类配置默认关闭（新功能默认 false，灰度再开）
```

---

## 8. 常见陷阱与排查

### 8.1 陷阱清单

| # | 陷阱 | 现象 | 排查 |
|---|------|------|------|
| 1 | 配置文件未加载 | 属性为 null | 检查文件名/位置/编码 |
| 2 | 同 key 多来源 | 值不是预期的 | 03 篇优先级 + /actuator/env |
| 3 | properties 与 yml 并存 | yml 不生效（properties 优先） | 统一只留一种 |
| 4 | @Value 拼写不符 | 启动失败 | 检查 key 精确匹配（无松散绑定） |
| 5 | 嵌套绑定失败 | 嵌套对象为 null | 检查 getter/setter + 嵌套类需 public |
| 6 | 校验没生效 | 非法值静默通过 | 确认 @Validated + 校验注解 |
| 7 | 中文乱码 | 配置值乱码 | 文件 UTF-8 + 无 BOM |

### 8.2 排查三板斧

```text
配置问题排查三板斧
├── ① 启动加 --debug：打印所有 PropertySource 加载顺序
├── ② /actuator/env：查看每个 key 的来源与最终值（精准定位）
├── ③ Environment 遍历：代码里打印 MutablePropertySources
└── 示例：
    curl localhost:8080/actuator/env/myapp.timeout-ms
    → {"property": {"source": "application.yml", "value": "3000"}}
```

---

## 9. 核心要点

> 🎯 **核心要点**：
> 1. 三件套：配置文件（application.yml）+ 注入（@Value/@ConfigurationProperties）+ 绑定（占位符/松散/转换/校验）
> 2. 企业规范：**业务配置一律 @ConfigurationProperties + @Validated**；@Value 只用于零散单值
> 3. @Value 五大陷阱：无默认值启动失败、static/final 失效、无松散绑定、无校验、刷新不生效
> 4. 松散绑定：`timeout-ms` ↔ `timeoutMs` ↔ `TIMEOUTMS` 自动匹配（仅 @ConfigurationProperties）
> 5. 默认值写在 Java 字段；敏感配置无默认值；校验 fail-fast 是配置质量第一道防线

---

**下一模块**：[03-外部化配置与加载优先级](03-外部化配置与加载优先级.md) | **返回总览**：[00-配置文件知识体系总览](00-配置文件知识体系总览.md)
