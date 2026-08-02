# 02 - Spring Boot 启动流程与自动配置内核

> 🎯 Spring Boot 的"魔法"不是黑盒 — SpringApplication.run 五阶段 + 自动配置加载链路 + @Conditional 条件树。Boot 4 的关键变化：ImportCandidates 取代 spring.factories、@AutoConfiguration 注解、AOT 静态化

---

## 目录

1. [启动入口与准备阶段](#1-启动入口与准备阶段)
2. [SpringApplication 构造阶段](#2-springapplication-构造阶段)
3. [run() 五阶段全流程](#3-run-五阶段全流程)
4. [自动配置加载链路](#4-自动配置加载链路)
5. [条件装配：@Conditional 判断树](#5-条件装配conditional-判断树)
6. [Boot 4 关键变化](#6-boot-4-关键变化)
7. [调试与排查方法](#7-调试与排查方法)

---

## 1. 启动入口与准备阶段

```java
@SpringBootApplication
public class App {
    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
        // 等价于 new SpringApplication(App.class).run(args)
    }
}
```

`@SpringBootApplication` = `@Configuration` + `@ComponentScan` + `@EnableAutoConfiguration`。其中 `@EnableAutoConfiguration` 是自动配置的总开关。

---

## 2. SpringApplication 构造阶段

在 `run()` 执行前，构造器已完成三件事：

```java
// ① 推断应用类型（通过 classpath 中的类判断）
//    判断顺序：DispatcherHandler（WebFlux）→ DispatcherServlet（SERVLET）→ NONE
this.webApplicationType = WebApplicationType.deduceFromClasspath();

// ② 加载 ApplicationContextInitializer（上下文初始化器）
//    SpringFactoriesLoader 读 META-INF/spring.factories
this.initializers = SpringFactoriesLoader.loadFactories(...);

// ③ 加载 ApplicationListener（事件监听器）
this.listeners = SpringFactoriesLoader.loadFactories(...);

// ④ 推断主启动类
//    Boot 3/4 使用 StackWalker（RETAIN_CLASS_REFERENCE）替代旧版 getStackTrace()
//    遍历调用栈找到 main 方法所在类，性能更优
this.mainApplicationClass = deduceMainApplicationClass();
```

**追问：** 为什么 Boot 4 用 `StackWalker`？→ `RuntimeException.getStackTrace()` 需要填充整个调用栈数组，内存开销大；`StackWalker` 按需遍历，性能好且对 GraalVM 友好。

---

## 3. run() 五阶段全流程

```text
SpringApplication.run() 核心流程（Spring Boot 4 / Framework 7）：
┌─────────────────────────────────────────────────────────┐
│ 阶段1：启动监听与环境构建                                  │
│  ① 启动 StopWatch 记录耗时                                │
│  ② 创建 BootstrapContext（环境构建前的临时上下文）          │
│  ③ 发布 ApplicationStartingEvent                          │
│  ④ 创建 Environment（加载配置源）                          │
│      → application.yml / JVM 属性 / OS 环境变量 / 命令行参数│
│  ⑤ 发布 ApplicationEnvironmentPreparedEvent               │
│  ⑥ 打印 Banner                                             │
├─────────────────────────────────────────────────────────┤
│ 阶段2：创建 ApplicationContext                             │
│  ① 根据 webApplicationType 反射创建 Context               │
│     SERVLET → AnnotationConfigServletWebServer...Context   │
│  ② 关联 Environment                                       │
│  ③ 执行 ApplicationContextInitializer（配置加载等）       │
│  ④ 发布 ApplicationContextInitializedEvent                │
├─────────────────────────────────────────────────────────┤
│ 阶段3：注册配置类与执行 runner                             │
│  ① 加载主启动类（@SpringBootApplication 标注的类）         │
│  ② 发布 ApplicationPreparedEvent（此时 context 已准备就绪）│
├─────────────────────────────────────────────────────────┤
│ 阶段4：刷新容器（refreshContext → refresh 12 步）          │
│  prepareRefresh()          → 准备上下文（属性源/验证）     │
│  obtainFreshBeanFactory    → 创建 BeanFactory             │
│  prepareBeanFactory        → 配置 BeanFactory 标准特性     │
│  postProcessBeanFactory    → 给子类扩展（空实现）          │
│  invokeBFPP                → ★ 自动配置类在此被处理       │
│  registerBPP               → 注册 BeanPostProcessor       │
│  initMessageSource         → 国际化消息源                  │
│  initApplicationEventMC    → 事件广播器                    │
│  onRefresh()               → ★ 启动内嵌 Web 服务器         │
│                         　     ServletWebServerFactory →   │
│                         　     Tomcat.start() 开启端口监听  │
│  registerListeners         → 注册监听器                    │
│  finishBeanFactoryInit     → 预实例化单例                  │
│                              listeners + converters +      │
│                              preInstantiateSingletons      │
│  finishRefresh()           → 清除缓存 + 发布 ContextRefresh│
├─────────────────────────────────────────────────────────┤
│ 阶段5：启动完成与收尾                                      │
│  ① afterRefresh（空实现，留给子类扩展）                     │
│  ② 发布 ApplicationStartedEvent                            │
│  ③ 执行 CommandLineRunner / ApplicationRunner              │
│  ④ 发布 ApplicationReadyEvent（应用就绪）                   │
└─────────────────────────────────────────────────────────┘
```

**追问：** `onRefresh()` 中做什么？→ `ServletWebServerApplicationContext` 重写此方法：通过 `ServletWebServerFactory` 创建 Tomcat/Jetty 实例 → 注册 DispatcherServlet → `webServer.start()` 开启监听。

---

## 4. 自动配置加载链路

### 4.1 触发入口

```text
@EnableAutoConfiguration
    ↓ 内嵌 @Import(AutoConfigurationImportSelector.class)
AutoConfigurationImportSelector
    ↓ 实现 DeferredImportSelector（延迟导入，用户自定义 Bean 优先）
AutoConfigurationGroup.process()
    ↓
getAutoConfigurationEntry()
    ↓ ① getCandidateConfigurations() → 读候选自动配置类
    ↓ ② removeDuplicates() → 去重
    ↓ ③ getExclusions() → 读用户排除项（spring.autoconfigure.exclude）
    ↓ ④ getConfigurationClassFilter().filter() → @Conditional 提前过滤
    ↓ ⑤ fireAutoConfigurationImportEvents() → 发布导入事件
最终注入符合条件的所有自动配置类
```

### 4.2 自动配置在哪里被处理

| 时机 | 具体步骤 |
|------|----------|
| refresh 第 5 步 | `invokeBeanFactoryPostProcessors` → `ConfigurationClassPostProcessor` 解析 @Import → 触发 AutoConfigurationImportSelector |
| 执行顺序 | `DeferredImportSelector` 在 `ConfigurationClassPostProcessor` 最后阶段处理，保证用户自定义 Bean 优先于自动配置 |

---

## 5. 条件装配：@Conditional 判断树

### 5.1 核心接口

```java
// 所有条件注解的底层接口：容器注册 Bean 前的一次 if 判断
// 多个条件组合为 AND（全部返回 true 才创建 Bean）
@FunctionalInterface
public interface Condition {
    boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata);
}
```

### 5.2 Boot 扩展的条件注解

| 注解 | 底层实现 | 用途 |
|------|----------|------|
| `@ConditionalOnClass` | 反射或 ClassLoader 检查类是否存在 | "有此类才配"（如 DataSource 在 classpath） |
| `@ConditionalOnMissingClass` | 同上取反 | "没有才配"（如排除某实现） |
| `@ConditionalOnBean` | BeanFactory#containsBean 检查 | "已有 Bean 才配"（依赖前置 Bean） |
| `@ConditionalOnMissingBean` | 同上取反 | "用户没定义才配默认"（覆盖默认值的关键） |
| `@ConditionalOnProperty` | Environment 读配置值匹配 | "配置了才配"（如 spring.redis.enabled） |
| `@ConditionalOnWebApplication` | WebApplicationType 判断 | "Web/非 Web 分别配" |

**追问：** `@ConditionalOnBean` 的坑？→ 它依赖 Bean 注册顺序，若依赖的 Bean 在后面注册则条件判断失败 → 应配合 `@AutoConfigureAfter` 控制顺序。

### 5.3 SpringBootCondition 的优势

```java
// 比原生 Condition 多：记录匹配/不匹配日志
// 开启 debug: true 可看到 Condition Evaluation Report
//   Positive matches: [列出所有成功匹配的条件]
//   Negative matches: [列出失败原因（排查"为什么不配"的关键！）]
//   Exclusions: [用户排除项]
```

---

## 6. Boot 4 关键变化

### 6.1 spring.factories → .imports 文件

| 对比 | spring.factories（旧） | `.imports` 文件（新，Boot 4 强制） |
|------|------------------------|-------------------------------------|
| 格式 | Properties（key=value） | 纯文本，每行一个类全限定名 |
| 解析 | Properties.load（慢） | 按行读取（快 ~40%） |
| 位置 | META-INF/spring.factories | **`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`** |
| 加载时机 | SpringApplication 构造阶段全量加载 | refresh() 阶段按需加载 |
| AOT | 不友好（格式解析复杂） | 天然适合静态分析 |
| 使用工具 | SpringFactoriesLoader | **ImportCandidates** |

```text
# 新写法（.imports 文件）
com.example.autoconfigure.MyAutoConfiguration
com.example.autoconfigure.MySecurityAutoConfiguration
```

### 6.2 @AutoConfiguration 取代 @Configuration

```java
// 新的专用注解 — 而非 @Configuration
@AutoConfiguration
@AutoConfigureAfter(DataSourceAutoConfiguration.class)  // 控制加载顺序
@ConditionalOnClass(MyService.class)
public class MyAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public MyService myService() { return new DefaultMyService(); }
}
```

**追问：** `@AutoConfiguration` vs `@Configuration`？→ 语义不同：前者明确标注"这是自动配置类"；支持 `@AutoConfigureBefore/After` 控制顺序；Boot 4 中自动配置应使用前者。

---

## 7. 调试与排查方法

```bash
# 1. 看自动配置报告（排查"为什么不生效"的第一工具）
# application.yml 中设置
debug: true
# 启动后控制台打印完整的自动配置评估报告

# 2. 断点关键方法
# AutoConfigurationImportSelector#getAutoConfigurationEntry()
# ConfigurationClassParser#processImports()        → 解析 @Import 导入
# SpringBootCondition#matches()                    → 条件评估入口

# 3. 命令行查看
mvn dependency:tree                     # 检查 classpath
java -jar app.jar --spring.autoconfigure.exclude=... # 排除自动配置
```

**排查口诀：** `debug:true` 看报告 → `dependency:tree` 看类路径 → 打断点看决策 → 改 `.imports` 文件。

---

> 🎯 **核心要点**：Boot 启动 = 构造期（类型推断 + 加载初始化器/监听器）→ 环境构建 → 创建 Context → **refresh 12 步**（自动配置 + Web 服务器在这里起作用）→ 发布就绪事件。自动配置的加载链路 = `@Import → AutoConfigurationImportSelector → ImportCandidates 读 .imports → @Conditional 过滤`。Boot 4 的关键改动：`.imports 文件取代 spring.factories`。

**下一模块**：[03-循环依赖与三级缓存深度剖析](03-循环依赖与三级缓存深度剖析.md) / **返回总览**：[00-深度剖析总览](00-Spring生态深度剖析总览.md)
