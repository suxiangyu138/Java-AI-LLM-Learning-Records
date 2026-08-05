# 08 配置管理：Environment 与配置属性

> 配置是"代码之外的变化点"——Environment/PropertySource 体系、@Value 与 @ConfigurationProperties 的分工、Profile 与优先级，是配置管理的完整地图

---

## 📚 目录

1. [Environment 与 PropertySource 体系](#1-environment-与-propertysource-体系)
2. [配置文件加载与优先级](#2-配置文件加载与优先级)
3. [@Value：占位符与 SpEL](#3-value占位符与-spel)
4. [@ConfigurationProperties：类型安全绑定](#4-configurationproperties类型安全绑定)
5. [Profile：环境切换](#5-profile环境切换)
6. [配置管理最佳实践](#6-配置管理最佳实践)

---

## 1. Environment 与 PropertySource 体系

**Environment 是配置的"统一视图"**——把系统属性、环境变量、配置文件等所有配置源封装成可查询的分层结构：

```text
Environment（统一查询入口）
  └── PropertySources（有序属性源列表，后注册的优先）
        ├── 系统属性 System.getProperties()      （-D 参数）
        ├── 环境变量 System.getenv()
        ├── application.yml / application.properties（Boot 加载）
        ├── 命令行参数（--server.port=8081）
        └── 自定义 PropertySource（如配置中心 Nacos Config）
```

```java
// 编程访问（组件内注入 Environment）
@Service
public class ConfigProbe {
    private final Environment env;

    public ConfigProbe(Environment env) { this.env = env; }

    public void probe() {
        String port = env.getProperty("server.port");          // 统一查询，不分来源
        boolean flag = env.getProperty("feature.enabled", Boolean.class, false);
        String[] profiles = env.getActiveProfiles();           // 当前激活的 Profile
    }
}
```

> 🎯 **核心要点**：Environment 让"配置来源"透明——代码只问"配置里是什么"，不关心它来自命令行、环境变量还是配置文件。**属性源顺序 = 优先级**，先注册的在后（Boot 中后声明的覆盖先声明的）。

---

## 2. 配置文件加载与优先级

**Spring Boot 的配置文件**：`application.properties` / `application.yml`（YAML 优先于 properties，若都存在则 YAML 生效）。

**配置来源优先级**（从高到低，覆盖规则）：

```text
1. 命令行参数（--server.port=8081）        ← 最高
2. Java 系统属性（-Dserver.port=8081）
3. 操作系统环境变量
4. 外部配置文件（jar 包外 config/）
5. jar 包内配置（classpath:）
6. @PropertySource 注解
7. 默认值
```

**同名键的覆盖链**：环境变量 > 命令行 > 外部文件 > 包内文件——"**运行时覆盖打包值**"靠的就是这条链（容器部署传环境变量即可覆盖）。

```yaml
# application.yml 基础结构
server:
  port: 8081
  servlet:
    context-path: /api

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/order_db
    username: ${DB_USER:root}          # 占位符 + 默认值：环境变量没有时用 root
    password: ${DB_PASSWORD}           # 敏感信息从环境变量注入（不要写死在文件！）

app:
  order:
    max-per-user: 100
```

> ⚠️ **敏感配置纪律**：密码、密钥**禁止提交到配置文件进 Git**——用环境变量 / 配置中心（Nacos Config，见 `Spring全家桶/10`）/ K8s Secret 注入。

---

## 3. @Value：占位符与 SpEL

**@Value 注入单个配置值**——占位符（`${}`）读 PropertySource，SpEL（`#{}`）算表达式：

```java
@Component
public class AppSettings {
    @Value("${app.name}")                       // 占位符：从配置读
    private String appName;

    @Value("${app.max-retry:3}")                // 带默认值：配置缺失用 3
    private int maxRetry;

    @Value("${server.port}")                    // 类型自动转换（String → int）
    private int port;

    @Value("#{systemProperties['user.home']}")  // SpEL：访问系统属性
    private String homeDir;

    @Value("#{T(java.lang.Math).random()}")     // SpEL：调用静态方法
    private double random;
}
```

**@Value 的边界**（何时不适合）：

| 场景 | 问题 | 替代 |
|------|------|------|
| 10+ 个相关配置 | 字段散落、无分组 | @ConfigurationProperties |
| 需要类型安全的嵌套结构 | 字符串拼接、类型靠猜 | @ConfigurationProperties |
| 配置与代码校验 | 无编译期检查 | record + @ConfigurationProperties |

> 🎯 **核心要点**：**@Value 适合"零散的单值"**（几个配置），**@ConfigurationProperties 适合"成组的配置对象"**——工程规范（阿里规约）：配置 3 个以上相关项就建配置类。

---

## 4. @ConfigurationProperties：类型安全绑定

**类型安全配置**：把一组配置绑定到 POJO/record——IDE 提示、编译期校验、可复用：

```yaml
# application.yml
app:
  order:
    max-per-user: 100
    default-status: PENDING
    retry:
      max-attempts: 3
      delay-ms: 500
```

```java
// 绑定类（record 写法，Spring Boot 3+ 支持）
@ConfigurationProperties(prefix = "app.order")
public record OrderProps(
        int maxPerUser,                       // 自动绑定 app.order.max-per-user（kebab-case 映射）
        String defaultStatus,
        RetryProps retry                      // 嵌套绑定
) {
    public record RetryProps(int maxAttempts, long delayMs) { }
}

// 启用方式 1：@ConfigurationPropertiesScan（推荐，扫描包内所有配置类）
@SpringBootApplication
@ConfigurationPropertiesScan
public class Application { ... }

// 启用方式 2：@EnableConfigurationProperties(OrderProps.class)
// 使用：直接注入，与普通 Bean 无异
@Service
public class OrderService {
    private final OrderProps props;

    public OrderService(OrderProps props) {
        this.props = props;
        // props.maxPerUser()  —— 编译期类型安全，改配置名 IDE 会警告
    }
}
```

**映射规则**：

| 配置文件写法 | Java 字段 |
|------------|----------|
| `app.order.max-per-user` | `maxPerUser`（kebab-case → camelCase） |
| `app.order.default-status` | `defaultStatus` |
| 嵌套层级 | 嵌套 record/类 |

**校验**（`@Validated` + jakarta validation，`spring-boot-starter-validation`）：

```java
@ConfigurationProperties(prefix = "app.order")
@Validated
public record OrderProps(
        @Min(1) @Max(1000) int maxPerUser,        // 启动时校验，不合法直接启动失败
        @NotBlank String defaultStatus
) { }
```

> 🎯 **核心要点**：**@ConfigurationProperties 的三大优势 = 类型安全（编译期）+ 分组（可读）+ 启动期校验（错误前置）**——Spring Boot 自身全部配置（`server.*`、`spring.datasource.*`）都是这么绑定的，这就是"配置类文档"的来源。

---

## 5. Profile：环境切换

**Profile = 环境维度的配置分组**（dev / test / prod），三套配置互相隔离：

```text
application.yml              ← 公共配置（所有环境）
application-dev.yml          ← 开发环境
application-test.yml         ← 测试环境
application-prod.yml         ← 生产环境
```

```yaml
# application.yml
spring:
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}    # 激活方式 1：环境变量（生产推荐）

# 命令行方式：--spring.profiles.active=prod
# 打包方式：-Dspring.profiles.active=prod
```

```java
// 代码级 Profile 控制
@Profile("dev")
@Configuration
public class DevDataSourceConfig { ... }          // 仅 dev 环境加载

@Profile("!prod")                                 // 非生产
@Component
public class MockClient { ... }

@Service
public class FeatureService {
    @Value("${feature.new-checkout:false}")
    private boolean newCheckout;                  // 按环境给不同默认值
}
```

**Profile 的两个注意点**：

1. **配置文件按 Profile 加载**：`application-{profile}.yml` 中同键覆盖基础文件的键；
2. **生产事故高发点**：`spring.profiles.active` 忘了配 → 加载的是默认/开发配置 → **生产用错了数据源**。规范：生产环境强制从环境变量/启动脚本注入 Profile，代码里不要写死 `active: prod`（K8s 部署时环境变量注入）。

> ⚠️ 配置管理的心法：**代码里的默认值只面向开发**——生产所有环境差异（数据源、密钥、开关）都必须由部署层注入，代码仓库里不应该出现生产密码。

---

## 6. 配置管理最佳实践

| 实践 | 说明 |
|------|------|
| 成组配置用 @ConfigurationProperties | 3+ 相关项建配置类，禁止散落 @Value |
| 默认值写全 | `${key:default}` 让配置缺失时仍可启动（敏感项除外） |
| 敏感信息不入库 | 环境变量/配置中心注入 |
| Profile 从部署层注入 | 不写死 active |
| 配置变更可追溯 | 配置中心（Nacos）版本化，变更记录审计 |
| 配置项文档化 | 配置类 Javadoc / 配置清单表 |
| 启动期校验 | @Validated 让错误配置"启动即失败"而非"运行期爆炸" |
| 环境一致性 | dev/test/prod 配置差异最小化（只留必须差异） |

**配置排查三步法**（线上"配置没生效"）：

```text
① 确认加载了哪个文件：actuator env 端点 / 启动日志的 Active profiles
② 确认值从哪来：Actuator /env 端点查看解析后的值与其来源（PropertySource 链）
③ 确认覆盖关系：同名键按优先级链（命令行 > 环境变量 > 外部文件 > 包内）逐一排查
```

> 🎯 **核心要点**：配置管理的本质是"**变化点与代码分离 + 可追踪 + 可校验**"——@ConfigurationProperties 管结构、Profile 管环境、配置中心管运行时变更、@Validated 管正确性，四件套组合使用。

---

**下一模块**：[09-资源国际化与测试](./09-资源国际化与测试.md) / **返回总览**：[00-Spring框架核心知识体系总览](./00-Spring框架核心知识体系总览.md)
