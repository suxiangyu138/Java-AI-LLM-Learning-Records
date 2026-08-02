# 07 - 自定义 Starter 与扩展点实战

> 🎯 把前 6 个模块的机制知识转化为工业级产出 — 从零写一个生产级 Spring Boot Starter，包含 @AutoConfiguration、.imports 文件、条件装配、配置属性绑定、健康检查全流程。附扩展点选型速查

---

## 目录

1. [Starter 开发核心要素](#1-starter-开发核心要素)
2. [完整开发步骤（Boot 4 适配）](#2-完整开发步骤boot-4-适配)
3. [扩展点选型速查](#3-扩展点选型速查)
4. [工业级质量要求](#4-工业级质量要求)

---

## 1. Starter 开发核心要素

```text
一个合格的 Spring Boot Starter 包含：
├── 自动配置类（@AutoConfiguration）— 条件创建 Bean
├── .imports 文件 — 注册自动配置类（Boot 4 的新方式）
├── 配置属性类（@ConfigurationProperties）— 让用户自定义参数
├── 条件注解（@ConditionalOnClass/Bean/Property/MissingBean）— 按需启用
├── spring-boot-configuration-processor — 生成的元数据（IDE 提示）
└── 可选：健康检查（HealthIndicator）、指标（MeterBinder）、启动日志
```

### 1.1 命名规范

| 模块 | 命名 | 示例 |
|------|------|------|
| 自动配置模块 | `xxx-spring-boot-autoconfigure` | `myclient-spring-boot-autoconfigure` |
| Starter 模块 | `xxx-spring-boot-starter` | `myclient-spring-boot-starter` |
| starter 模块本身不写代码 | 只依赖 autoconfigure 模块 + 传递依赖 | 让用户一个 starter 搞定 |

---

## 2. 完整开发步骤（Boot 4 适配）

### 步骤 1：创建配置属性类

```java
@ConfigurationProperties(prefix = "my.client")  // 前缀：application.yml 的 my.client.*
public class MyClientProperties {
    private boolean enabled = true;             // 默认值
    private String endpoint = "http://localhost:8080";
    private int connectTimeout = 5000;          // 毫秒
    private int readTimeout = 10000;
    // getter/setter ...
}
```

### 步骤 2：编写自动配置类

```java
// ★ 使用 @AutoConfiguration 而非 @Configuration（Boot 4 专用）
@AutoConfiguration
@EnableConfigurationProperties(MyClientProperties.class)
// ★ 顺序控制：确保在需要的自动配置之后加载
@AutoConfigureAfter({JacksonAutoConfiguration.class})
// ★ 条件装配：classpath 有目标依赖类时才加载
@ConditionalOnClass(MyClient.class)
@ConditionalOnProperty(prefix = "my.client", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MyClientAutoConfiguration {

    private final MyClientProperties properties;

    public MyClientAutoConfiguration(MyClientProperties properties) {
        this.properties = properties;
    }

    @Bean
    @ConditionalOnMissingBean  // ★ 允许用户自定义覆盖
    public MyClient myClient() {
        return new MyClientBuilder()
            .endpoint(properties.getEndpoint())
            .connectTimeout(properties.getConnectTimeout())
            .readTimeout(properties.getReadTimeout())
            .build();
    }

    // 可选：健康检查（配合 Actuator /health 端点）
    @Bean
    @ConditionalOnClass(HealthIndicator.class)
    @ConditionalOnMissingBean(name = "myClientHealthIndicator")
    public HealthIndicator myClientHealthIndicator(MyClient client) {
        return () -> {
            boolean reachable = client.ping();
            return reachable ? Health.up().build() : Health.down().build();
        };
    }
}
```

### 步骤 3：创建 .imports 文件（★ Boot 4 关键变化）

```text
# 文件位置：
# META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports

# ★ 不再是 spring.factories！标点格式更简单，更适合静态分析
com.example.myclient.autoconfigure.MyClientAutoConfiguration
```

### 步骤 4：添加配置元数据支持

```xml
<!-- pom.xml 中加依赖 — 编译时生成 META-INF/spring-configuration-metadata.json -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-configuration-processor</artifactId>
    <optional>true</optional>
</dependency>
<!-- 生成的 JSON 包含属性名、类型、描述、默认值 — IDE 自动提示 -->
```

### 步骤 5：项目结构

```text
myclient-spring-boot-starter/
├── pom.xml                      # starter 模块：只依赖 autoconfigure
│   (pom 类型，引入 autoconfigure + 传递依赖)
│
myclient-spring-boot-autoconfigure/
├── pom.xml
└── src/main/
    ├── java/.../
    │   ├── MyClientAutoConfiguration.java
    │   ├── MyClientProperties.java
    │   └── MyClientHealthIndicator.java
    └── resources/
        └── META-INF/
            └── spring/
                └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

### 步骤 6：用户使用

```yaml
# application.yml
my:
  client:
    endpoint: https://api.example.com
    connect-timeout: 3000
```

```java
// 用户代码 — 注入即可用
@Autowired MyClient myClient;
```

---

## 3. 扩展点选型速查

**面对需求"我要在 Spring 创建 Bean 时做某事"→ 选哪个扩展点？**

### 3.1 需求 → 扩展点对照

| 需求 | 选哪个扩展点 | 说明 |
|------|-------------|------|
| 动态注册新 Bean | BeanDefinitionRegistryPostProcessor | 最早，可注册新 BeanDefinition |
| 修改已有的 BeanDefinition | BeanFactoryPostProcessor | 改属性/作用域等 |
| 注入自定义注解的依赖 | InstantiationAwareBPP#postProcessProperties | @Autowired 在这个位置注入 |
| 在所有 Bean 属性填充前做处理 | InstantiationAwareBPP#postProcessAfterInstantiation | 返回 false 跳过填充 |
| 初始化前做前置增强 | BeanPostProcessor#postProcessBeforeInitialization | 如注入 Aware 接口 |
| 创建 AOP 代理 | BeanPostProcessor#postProcessAfterInitialization | 在初始化完成后包装代理 |
| 监听所有单例 Bean 初始化完 | SmartInitializingSingleton#afterSingletonsInstantiated | Nacos/Ribbon 经典用法 |
| 启动后执行一次逻辑 | CommandLineRunner / ApplicationRunner | 启动后一次性任务 |
| 监听容器刷新完成 | ApplicationListener<ContextRefreshedEvent> | 容器就绪 |
| 监听 Web 服务器启动（端口就绪） | ApplicationListener<WebServerInitializedEvent> | Nacos 注册时机 |
| 监听事务提交 | @TransactionalEventListener(AFTER_COMMIT) | 发消息/清缓存 |
| Bean 销毁前释放资源 | @PreDestroy / DestructionAwareBPP | 清理连接池等 |
| 自定义条件判断 | 实现 Condition + @Conditional(MyCondition.class) | 创建自定义条件注解 |

### 3.2 选型口诀

```text
"改图纸" → BFPP / BDRPP
"改成品" → BPP / InstantiationAwareBPP
"做代理" → afterInitialization
"等全局就绪" → SmartInitializingSingleton / ContextRefreshedEvent
"等端口就绪" → WebServerInitializedEvent
"一次性任务" → CommandLineRunner
```

---

## 4. 工业级质量要求

| 检查项 | 标准 |
|--------|------|
| 条件装配 | 必须 @ConditionalOnClass + @ConditionalOnMissingBean（允许用户覆盖） |
| 默认值 | 所有配置属性有合理默认值（最小化用户配置） |
| 配置提示 | 引入 configuration-processor（IDE 自动提示） |
| 命名前缀 | 配置类用 `@ConfigurationProperties(prefix = "xxx")` — 短且不冲突 |
| 健康检查 | 接入外部服务/中间件的一定提供 HealthIndicator |
| 优雅关闭 | @PreDestroy / DisposableBean 中关闭资源（连接池/线程池/客户端） |
| 日志 | 自动配置类加载时输出信息级别日志（@Conditional 匹配才打） |
| 测试 | `@SpringBootTest` 验证自动配置生效、条件失效验证、配置属性覆盖测试 |

---

> 🎯 **核心要点**：自定义 Starter 是把"机制理解"转化为"工业产出"的标准路径。最重要的三件事：① `.imports 文件（Boot 4 新标准）`② `@AutoConfiguration + @ConditionalOnMissingBean（允许用户覆盖）`③ `配置属性绑定（@ConfigurationProperties + metadata 生成）`。一个好的 Starter = 用户引入依赖 + 写两行配置即可用，零额外代码。

**返回总览**：[00-深度剖析总览](00-Spring生态深度剖析总览.md)
