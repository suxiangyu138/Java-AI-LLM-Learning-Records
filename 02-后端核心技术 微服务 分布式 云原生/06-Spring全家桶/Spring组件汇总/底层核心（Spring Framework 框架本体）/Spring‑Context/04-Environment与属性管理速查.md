# 04 Environment 与属性管理速查

> PropertySource 链、Profile 激活、占位符解析、@PropertySource 与外部化配置——"配置从哪来"的完整链路

---

## 📚 目录

1. [Environment 体系](#1-environment-体系)
2. [PropertySource 解析链](#2-propertysource-解析链)
3. [Profile：环境激活机制](#3-profile环境激活机制)
4. [占位符解析：${} 的幕后](#4-占位符解析-的幕后)
5. [@PropertySource 与外部化配置](#5-propertysource-与外部化配置)

---

## 1. Environment 体系

| 组件 | 职责 |
|------|------|
| `PropertyResolver`（接口） | `getProperty(key)` / `resolvePlaceholders(str)` / `resolveRequiredPlaceholders` |
| `Environment`（接口） | 继承 PropertyResolver + `getActiveProfiles()` / `acceptsProfiles(...)` |
| `ConfigurableEnvironment` | `setActiveProfiles` / `addFirstPropertySource` 等修改能力 |
| `StandardEnvironment` | 默认实现：系统属性 + 系统环境变量两个 PropertySource |
| `StandardServletEnvironment` | Web 环境：追加 Servlet 上下文参数/配置 |

> 💡 知识归属：`org.springframework.core.env` 包在 **spring-core**——Environment 接口是 core 的，context 提供默认实现装配与 `EnvironmentAware`/注入支持。

**获取方式**：

```java
// ① 注入 Environment
@Service
public class ConfigService {
    private final Environment env;
    public ConfigService(Environment env) { this.env = env; }

    public String get() {
        return env.getProperty("app.name", "default");     // 默认值
        // return env.resolvePlaceholders("${app.name}");
    }
}

// ② 实现 EnvironmentAware 回调
// ③ 静态工具（不推荐）：ApplicationContext 持有者
```

### 1.1 获取属性的四种方式对比与取舍

| 方式 | 示例 | 时机 | 取舍 |
|------|------|------|------|
| 构造器注入 `Environment` | `private final Environment env;` | Bean 创建时 | ✅ 首选——静态分析友好、可测试 |
| `@Value("${...}")` 字段/参数 | `@Value("${app.name}") String name` | Bean 创建时（占位符解析） | ✅ 简单场景首选；注意解析发生在 BeanFactoryPostProcessor 阶段 |
| `EnvironmentAware` 回调 | `setEnvironment(Environment)` | Bean 创建早期 | 需要环境又不想注入时的接口方案 |
| 静态持有者 | `ApplicationContextHolder.getEnv()` | 任意时刻 | ⚠️ 仅工具类兜底——隐藏依赖、难测试、7.0 静态化趋势下更应避免 |

```java
// 两种注入方式的边界差异
@Service
public class ConfService {
    // @Value 注入的是"解析后的字符串"——支持默认值、类型转换
    @Value("${app.timeout-ms:1000}")
    private long timeoutMs;

    // Environment 注入的是"解析器本身"——可运行时动态取值、枚举 PropertySource
    public String resolve(String key) {
        return env.getProperty(key, "unknown");   // 动态 key 必须用 Environment
    }
}
```

> 🎯 **核心取舍**：`@Value` 是**编译期确定 key** 的静态绑定（错误 key 启动即失败）；`Environment.getProperty` 是**运行期动态解析**（key 可拼接、可缺省）。动态 key、批量遍历、按需组合场景用 Environment；单值配置用 @Value 更简洁且有启动期校验。

### 1.2 面试追问：配置体系三连

| 追问 | 答案要点 |
|------|---------|
| "系统属性与系统环境变量谁优先？" | **系统属性**（systemProperties）在链中靠前——因为 `-D` 是显式意图，环境变量是环境隐式注入；这也是"命令行覆盖环境变量"的底层原因 |
| "getProperty 与 @Value 的解析时机差在哪？" | `getProperty` 随时可调（运行期动态）；@Value 在 **BeanFactoryPostProcessor 阶段**（refresh 第 5 步）静态替换——所以"运行期改文件 @Value 不会变" |
| "PropertySource 链能否在运行期修改？" | 能——`ConfigurableEnvironment.getPropertySources()` 暴露增删改；但**已解析的 @Value 不会重算**，只影响后续 `getProperty` 调用 |
| "为什么属性名用小写点分（app.name）？" | 约定：属性名 = 层级命名空间；Boot 还支持**宽松绑定**（`app.name` ↔ `APP_NAME` ↔ `appName`），context 层无此能力 |

> 💡 **记忆**：context 层的 Environment 是"**解析链 + Profile**"的朴素实现；Boot 在其上加了三样东西——**文件发现（application.yml）、宽松绑定（relaxed binding）、类型化配置类（@ConfigurationProperties）**。面试答"Boot 配置比原生强在哪"就用这三点。

## 2. PropertySource 解析链

```text
StandardEnvironment 内置 PropertySource 链（按查找顺序）：
  1. systemProperties    JVM 系统属性（-Dapp.name=xxx）      ← 优先
  2. systemEnvironment   操作系统环境变量（APP_NAME=xxx）

追加顺序决定优先级（addFirstPropertySource 插最前）：
  @PropertySource → Boot application.yml → 系统属性 → 环境变量
```

```java
// 查看当前完整解析链（排查"值从哪来"的利器）
for (PropertySource<?> ps : env.getPropertySources()) {
    System.out.println(ps.getName() + " = " + ps.getProperty("app.name"));
}
```

> 🎯 **核心要点**：`env.getProperty(key)` 按链从前往后查，**第一个命中的值生效**——这就是"环境变量覆盖配置文件、命令行覆盖一切"的依据；Boot 的"配置优先级"本质是 PropertySource 排序（见 Boot 系列）。

### 2.1 自定义 PropertySource：接入数据库/远程配置中心

```java
// 场景：从配置中心（Nacos/自研）拉取属性，插入链首获得最高优先级
public class RemotePropertySource extends MapPropertySource {
    public RemotePropertySource(String name, Map<String, Object> source) {
        super(name, source);
    }
}

@Configuration
public class RemoteConfigBootstrap {
    @Autowired
    private Environment env;

    @PostConstruct
    public void inject() {
        Map<String, Object> remote = fetchFromConfigCenter();   // 伪代码：HTTP 拉取
        ConfigurableEnvironment ce = (ConfigurableEnvironment) env;
        ce.getPropertySources().addFirst(new RemotePropertySource("config-center", remote));
        // addFirst → 压过 systemProperties，成为最高优先级
    }
}
```

| 插入 API | 位置 | 效果 |
|---------|------|------|
| `addFirst` | 链首 | 覆盖一切（含命令行）——**慎用** |
| `addLast` | 链尾 | 最低优先级兜底 |
| `addBefore(name, ps)` / `addAfter(name, ps)` | 指定锚点 | 最常用：插在 systemProperties 之前即可覆盖 JVM 参数 |

> ⚠️ **边界**：`@PostConstruct` 里注入属性源有顺序风险——**占位符解析（BeanFactoryPostProcessor）早于 Bean 的 @PostConstruct**，若其他 Bean 的 @Value 依赖该源，会解析失败。安全姿势是注册 `ApplicationContextInitializer`（refresh 前执行）或实现 `BeanFactoryPostProcessor` 并在其中操作 `ConfigurableEnvironment`。

### 2.2 Boot 完整优先级链（自上而下覆盖关系）

```text
命令行参数 > SPRING_APPLICATION_JSON > ServletConfig 参数 > ServletContext 参数
  > JNDI > Java 系统属性 > OS 环境变量 > RandomValue 属性源
  > jar 外 application-{profile}.yml > jar 内 application-{profile}.yml
  > jar 外 application.yml > jar 内 application.yml > @PropertySource 加载的文件
```

> 💡 **记忆锚点**："外 > 内、具体（profile）> 通用、运行期（命令行/环境变量）> 声明期（文件）"——Boot 的优先级表就是按这个直觉排序的，与 context 层"PropertySource 顺序即优先级"是同一机制的两层表现。

### 2.3 属性覆盖的实战陷阱

| 陷阱 | 现象 | 根因与对策 |
|------|------|-----------|
| 环境变量覆盖不了 | 设置了 `APP_NAME` 但生效的仍是 yml 值 | 环境变量名映射：`APP_NAME` → key `app.name` 需 Boot 宽松绑定；**纯 context 层不支持该映射**，用系统属性 `-Dapp.name=` |
| 系统属性大小写 | `-DAPP.NAME` 与 `app.name` 是**两个 key** | 属性 key 大小写敏感，统一小写约定 |
| 链首被第三方占用 | 自定义 addFirst 后第三方配置中心失效 | 优先 `addBefore("systemProperties", ps)` 精确插入 |
| 属性值首尾空格 | `getProperty` 返回带空格值 | 生产文件人工编辑易漏——启动校验 trim 或 CI 检查 |
| 同 key 多来源值不一致 | 线上与本地行为不同 | 启动时打印"生效值 + 来源名"（见第 2 节代码） |

> ⚠️ **生产建议**：把"配置生效值审计"做成启动日志——遍历关键配置 key，输出 `key=value (来源: propertySourceName)`；上线对比差异是排查"配置没生效"最有效的手段，成本只要十几行代码。

## 3. Profile：环境激活机制

| 激活方式 | 写法 |
|---------|------|
| 命令行 | `--spring.profiles.active=dev,preview` |
| 环境变量 | `SPRING_PROFILES_ACTIVE=dev` |
| 配置文件 | `spring.profiles.active: dev`（application.yml） |
| 代码 | `ctx.getEnvironment().setActiveProfiles("dev")`（refresh 前） |
| 表达式 | `@Profile("dev \| preview")`、`@Profile("!test")`、`@Profile("dev & !ci")` |

```java
// 默认 profile + 条件组合
@Configuration
public class DataConfig {
    @Bean
    @Profile("dev")        // 仅 dev
    public DataSource devDs() { return new EmbeddedDb(); }

    @Bean
    @Profile("prod")
    @Conditional(HasSecret.class)   // 可叠加
    public DataSource prodDs() { return new CloudDb(); }
}
```

> ⚠️ **高频坑**：`setActiveProfiles` 必须在 `refresh()` 之前调用；`@Profile` 评估在 refresh 第 5 步——之后设置不生效。Boot 中配置文件里的 `spring.profiles.active` 在自动配置处理时读取，正常。

### 3.1 Profile 进阶边界与生产实践

| 边界/问题 | 行为 | 解法 |
|----------|------|------|
| 未激活任何 profile | `getActiveProfiles()` 返回空数组，`@Profile("dev")` 全部不生效 | 设置 `spring.profiles.default` 兜底；或 `default` 关键字：`@Profile("default")` |
| 多个 profile 同时激活 | `@Profile("dev & preview")` = 两个都要激活；`"dev \| preview"` = 任一即可 | 用 `&`（且）/`\|`（或）/`!`（非）组合表达式 |
| profile 名含特殊字符 | 非法字符启动报 `IllegalArgumentException` | 只允许字母/数字/`-`/`_`；不要用点号 |
| 测试环境忘记激活 | dev 专属 Bean 缺失 → 注入失败 | 测试注解 `@ActiveProfiles("test")` |
| 生产误激活 dev | 内存库顶替生产库 | CI 校验 `spring.profiles.active` 白名单；`@Profile("!dev")` 保护生产 Bean |

```java
// 生产实践：profile 组合 + 默认值兜底
@Bean
@Profile("prod & !canary")          // 生产且非金丝雀
public DataSource prodDs() { return prodPool(); }

@Bean
@Profile("default")                 // 未显式激活任何 profile 时的兜底
public DataSource fallbackDs() { return localPool(); }
```

> 🎯 **面试追问**："@Profile 和 @Conditional 什么关系？"——`@Profile` 本身就是 `@Conditional(ProfileCondition.class)` 的派生注解（元注解带 @Conditional）；所以 profile 是条件装配的**内置特例**，自定义条件用 `@Conditional` 任意扩展——两者可以叠加使用（见 [03-配置类与扫描机制速查](03-配置类与扫描机制速查.md) 第 6 节）。

### 3.2 Profile 与配置中心的配合（生产形态）

| 形态 | 做法 | 注意 |
|------|------|------|
| 纯文件 | application-{profile}.yml 多套 | 文件膨胀，易漂移（三套配置互相抄） |
| 文件 + 覆盖 | 基线 application.yml + profile 只放差异 | ✅ 推荐——差异越小漂移越少 |
| 配置中心 | Nacos/Apollo 按 group/dataId 区分环境 | 运维体系成本；启动期需要拉取（见 2.1） |
| 环境注入 | 容器/K8s 注入 `SPRING_PROFILES_ACTIVE` | 部署平台管理环境，代码零感知 |

```yaml
# 推荐的 profile 文件组织（差异最小化）
application.yml          # 基线：通用配置 + 默认值
application-dev.yml      # 仅差异：本地库、日志级别
application-prod.yml     # 仅差异：生产库、线程池、监控开关
```

> 💡 **实践原则**：profile 的数量 = 环境的数量，**每个 profile 文件只写与基线不同的键**；差异越大，测试环境验证覆盖越差——"dev 能跑、prod 爆炸"多半是 profile 文件里藏了环境专属逻辑，而不是环境本身的问题。

## 4. 占位符解析：${} 的幕后

```text
@Value("${app.name}") 的完整链路：
  ① PropertySourcesPlaceholderConfigurer（BeanFactoryPostProcessor，context 包）
     → 扫描全部 BeanDefinition 中的 ${...} 占位符
  ② 用 Environment 的解析链解析（含默认值 ${app.name:default}）
  ③ 属性缺失且无默认值 → BeanDefinitionValidationException（启动即失败）

  ※ 7.0 起占位符解析还支持类型转换（String → 目标类型由 ConversionService 完成）
```

| 写法 | 含义 |
|------|------|
| `${app.name}` | 必须存在，否则启动失败 |
| `${app.name:default}` | 缺省值 |
| `${app.name:${fallback.name}}` | 嵌套占位符 |
| `#{...}` | SpEL 表达式（见 [Spring-Core-06](../Spring‑Core/06-SpEL表达式速查.md)） |
| `${pool.size:8}` 注入 `int` 字段 | 自动类型转换 |

> 🎯 **核心要点**：占位符解析发生在"图纸阶段"（BeanFactoryPostProcessor）而非实例化阶段——**属性写错 = 启动即失败**，这是 Spring 配置安全性的根基；@Value 失败排查先看 PropertySource 链（第 2 节）。

### 4.1 占位符解析的边界与高频坑

| 场景 | 行为 | 应对 |
|------|------|------|
| `${a:${b}}` 嵌套缺省 | b 缺失则整体失败（嵌套只解析一层默认） | 保证内层 key 存在或都带默认值 |
| `${a:${b:c}}` 双层缺省 | b 缺则用 c | 可多级嵌套，每层都需可解析 |
| `${a}` 在 `@Scheduled` cron 中 | cron 表达式含 `*` 与占位符混用易解析错 | 拆成独立属性或 `@Value` 取整串 |
| XML 中的 `${}` | 由 `PropertySourcesPlaceholderConfigurer` 统一处理 | 与注解 @Value 同一机制 |
| 属性值含 `$` 或 `{}` | 解析器可能二次解析误伤 | 用 `@Value("${a}")` 的转义 `\${` 或拆分存储 |
| 类型转换失败 | 启动失败：`IllegalArgumentException`（如 "abc" → int） | 确认值可转换；7.0 起 ConversionService 参与 |
| 运行时改属性文件 | **占位符不重新解析**（静态绑定） | 需要热更新用配置中心/Actuator refresh |

```java
// 7.0 占位符类型转换示例（构造器参数同样支持）
@Configuration
public class AppConfig {
    @Bean
    public PoolConfig poolConfig(
            @Value("${pool.size:8}") int size,          // String → int 自动转换
            @Value("${pool.ratio:0.8}") double ratio,   // String → double
            @Value("${pool.mode:auto}") PoolMode mode) {  // String → enum（名称匹配）
        return new PoolConfig(size, ratio, mode);
    }
}
```

> ⚠️ **与 SpEL 的边界**：`${}` 占位符是**属性解析**（先查链后替换）；`#{}` 是 SpEL 表达式（可运算、调方法）——两者可以组合：`#{'${a:10} * 2'}`。顺序是**先解析 ${} 再执行 SpEL**（`PlaceholderConfigurerSupport` 在 SpEL 求值前完成占位符替换）。

### 4.2 @Value 迁移与配置重构实践

**何时需要从 @Value 迁移到类型化配置（Boot 的 @ConfigurationProperties）**：

| 信号 | 说明 |
|------|------|
| 同一个 Bean 有 5+ 个 @Value | 可读性崩塌，改为类型化前缀对象 |
| key 被多处重复引用 | 改一个 key 要全局替换 |
| 配置与校验逻辑耦合 | 类型化配置支持 `@Validated` |
| 配置值需要默认对象（非字符串） | @Value 只能字符串默认值 |

```java
// 迁移示例：散落的 @Value → 一个前缀配置类
// 迁移前
@Value("${order.timeout:30}")  private int timeout;
@Value("${order.retry:3}")     private int retry;
// 迁移后（Boot）
@ConfigurationProperties(prefix = "order")
@Validated
public record OrderProps(
        @Min(1) int timeout,       // 类型 + 校验
        @Min(0) int retry) { }
```

> 💡 **边界**：纯 context（无 Boot）项目没有 @ConfigurationProperties——重构只能止步于"自定义 Properties 类 + 构造器传入"，或引入 Boot 配置模块。**先确认工程形态再决定重构方案**，不要为"优雅"引入不必要的依赖。

### 4.3 属性诊断实战：三分钟定位"值从哪来"

```java
// 诊断工具：打印任意 key 的解析来源（生产排障利器）
public static void diagnose(Environment env, String key) {
    System.out.println("key = " + key);
    System.out.println("生效值 = " + env.getProperty(key, "<缺失>"));
    for (PropertySource<?> ps : ((ConfigurableEnvironment) env).getPropertySources()) {
        Object v = ps.getProperty(key);
        if (v != null) {
            System.out.println("  ← 来源: " + ps.getName() + " = " + v);
        }
    }
}
// 输出示例：
// key = app.name
// 生效值 = prod-app
//   ← 来源: systemProperties = prod-app     （-D 覆盖了配置文件）
//   ← 来源: applicationConfig: [classpath:/application.yml] = dev-app
```

| 诊断现象 | 结论 |
|---------|------|
| 多个来源都命中 | 链序最前的生效——打印顺序即优先级 |
| 只有一处命中 | 改该来源即可 |
| 无任何来源命中 | key 名拼写/前缀问题；检查占位符 `:default` 是否掩盖了缺失 |
| 命中值带意外空格 | 文件编辑问题（见 2.3） |

> 🎯 **核心心法**：**"先问值从哪来，再问为什么错"**——80% 的配置事故是"我以为的源"与"实际的源"不一致；把诊断脚本固化进运维工具（或 Boot 的 `/actuator/env` 端点），任何配置疑问 30 秒定位，而不是改代码反复试。

## 5. @PropertySource 与外部化配置

```java
@Configuration
@PropertySource("classpath:app.properties")            // 单个
@PropertySource("classpath:${profile:prod}-config.properties")  // 占位符路径
public class AppConfig { }

// 动态选择属性文件（Boot 优雅做法）
// spring.config.import: optional:classpath:extra.properties  （Boot 4 推荐）
```

| 场景 | 推荐做法 |
|------|---------|
| 少量固定属性 | `application.yml`（Boot） |
| 环境差异 | Profile 化文件 `application-dev.yml` |
| 外部追加 | `spring.config.import: optional:file:./ext/conf.properties` |
| 第三方模块属性 | `@PropertySource` + 前缀常量 |
| 加密/敏感 | 不要在属性文件存明文，用环境变量/密钥管理 |

> ⚠️ **Boot 注意**：`@PropertySource` 加载的文件**优先级低于** application.yml，且不参与 Boot 的 profile 切换——需要"按 profile 追加"用 `spring.config.import`（Boot 专属，见 Spring Boot 系列）。

### 5.1 外部化配置方案选型

| 需求 | context 原生方案 | Boot 方案 | 取舍 |
|------|-----------------|----------|------|
| 少量固定属性 | `@PropertySource` + properties | application.yml | yml 支持层级/类型更强，Boot 首选 |
| 环境差异化 | 多 `@PropertySource` + @Profile 组合 | `application-{profile}.yml` | 原生方案笨重，Boot 优雅 |
| 外部文件覆盖 | `@PropertySource("file:...")` | `spring.config.import: optional:file:...` | 原生不参与 profile；Boot 支持 optional |
| 敏感信息 | 环境变量直接进链（无需配置） | 环境变量 + 密钥管理 | 明文密钥 = 事故，两者都禁止落盘 |
| 配置中心 | 自定义 PropertySource（见 2.1） | Spring Cloud Config / Nacos | 需要 Client 依赖与运维体系 |

**生产实践清单**：

- 所有可变值走属性，**禁止硬编码**（包括超时、阈值、开关）；
- 环境差异只依赖 `spring.profiles.active` 一个入口，不写死机器名/环境名；
- 敏感项（密码、Token）**不落配置文件**，用环境变量或密钥管理注入；
- 配置变更走发布流程（GitOps/配置中心），禁止生产机直接改文件；
- 用 `optional:` 前缀允许外部文件缺失（开发机无配置文件也能启动）。

> 🎯 **面试总结**："Spring 的配置体系"一条线：**PropertySource 链（从哪取值）→ Profile（按环境过滤）→ 占位符解析（绑定到代码）→ 外部化（运行时注入）**——四个环节全部在 context 层有对应机制，Boot 只是把文件发现与绑定做得更好用。

---

**下一模块**：[05-事件驱动机制速查](05-事件驱动机制速查.md)　**返回总览**：[00-Spring Context组件总览](00-Spring Context组件总览.md)
