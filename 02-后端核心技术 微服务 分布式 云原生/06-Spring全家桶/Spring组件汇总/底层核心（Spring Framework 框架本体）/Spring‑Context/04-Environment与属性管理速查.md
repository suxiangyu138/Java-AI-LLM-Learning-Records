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

---

**下一模块**：[05-事件驱动机制速查](05-事件驱动机制速查.md)　**返回总览**：[00-Spring Context组件总览](00-Spring Context组件总览.md)
