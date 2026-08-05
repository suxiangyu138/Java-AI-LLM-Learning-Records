# 02 BeanDefinition 与三种配置方式

> BeanDefinition 是 Bean 的"图纸"，配置方式决定图纸从哪来——XML、注解、JavaConfig 三代的演进理解透，才能解释"为什么这么写"

---

## 📚 目录

1. [BeanDefinition：Bean 的图纸](#1-beandefinitionbean-的图纸)
2. [配置方式演进：XML → 注解 → JavaConfig](#2-配置方式演进xml--注解--javaconfig)
3. [组件扫描：@ComponentScan 与过滤规则](#3-组件扫描componentscan-与过滤规则)
4. [@Configuration 与 @Bean 的细节](#4-configuration-与-bean-的细节)
5. [条件装配：@Conditional 家族](#5-条件装配conditional-家族)
6. [Spring 7.0 新成员：BeanRegistrar](#6-spring-70-新成员beanregistrar)

---

## 1. BeanDefinition：Bean 的图纸

**BeanDefinition** 描述"容器该怎么创建这个 Bean"——类名、作用域、是否懒加载、初始化方法、依赖关系、构造参数……容器根据图纸 `createBean`。

```java
// BeanDefinition 的核心元数据（属性视角）
// class：实例化哪个类
// scope：singleton / prototype
// lazyInit：是否懒加载
// initMethodName / destroyMethodName：生命周期回调
// autowireMode：自动装配模式
// constructorArgumentValues：构造参数
// propertyValues：属性值
// primary / dependsOn：优先级与依赖顺序
```

**图纸的三种来源**：

| 来源 | 机制 | 说明 |
|------|------|------|
| XML 解析 | `XmlBeanDefinitionReader` | `<bean class="..."/>` 标签 → BeanDefinition |
| 注解扫描 | `ClassPathBeanDefinitionScanner` | `@Component` 等 → BeanDefinition |
| 编程式 | `registerBeanDefinition` / `registerBean` | 代码直接注册图纸 |

```java
// 编程式注册（源码/框架内部常见）
GenericApplicationContext ctx = new GenericApplicationContext();
ctx.registerBeanDefinition("orderService",
        new RootBeanDefinition(OrderService.class));   // 手动造图纸
ctx.refresh();
```

> 🎯 **核心要点**：**BeanFactoryPostProcessor 改的是图纸（BeanDefinition），BeanPostProcessor 改的是成品（Bean 实例）**——这个区分是理解 Spring 扩展体系的第一把钥匙（源码细节见 `Spring生态深度剖析/01`）。

---

## 2. 配置方式演进：XML → 注解 → JavaConfig

| 时代 | 方式 | 典型写法 | 现状 |
|------|------|---------|------|
| Spring 1.x~2.x | **XML** | `<bean id="dao" class="..."/>` + `<property name="dao" ref="dao"/>` | 遗留项目仍见，新项目废弃 |
| Spring 2.5+ | **注解** | `@Component` + `@Autowired` | 现代主流（配合扫描） |
| Spring 3.0+ | **JavaConfig** | `@Configuration` + `@Bean` | 现代主流（第三方类/复杂装配） |

**三种方式的能力对比**：

| 能力 | XML | 注解 | JavaConfig |
|------|:---:|:----:|:----------:|
| 声明自己的类为 Bean | ✅ | ✅（@Component） | ✅（@Bean） |
| 声明第三方类为 Bean | ✅ | ❌（无法改别人的类） | ✅（@Bean 工厂方法） |
| 条件逻辑装配 | 弱 | 弱 | ✅（Java 代码任意写） |
| 编译期检查 | ❌（运行时才暴露） | ✅ | ✅ |
| 重构友好 | ❌（字符串脆弱） | ✅ | ✅ |

**为什么要用 JavaConfig 注册第三方类**：

```java
// ❌ 第三方类无法加 @Component —— 只能靠配置
@Configuration
public class CacheConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();  // 别人的类
        template.setConnectionFactory(factory);
        return template;
    }
}
```

> 🎯 **核心要点**：**自己写的类用 @Component 扫描；第三方/条件性/参数复杂的 Bean 用 @Configuration + @Bean**——这是现代 Spring 配置的标准分工。XML 仅存于遗留系统。

---

## 3. 组件扫描：@ComponentScan 与过滤规则

**@ComponentScan** 告诉容器"去哪里找 @Component 图纸"（Spring Boot 的 `@SpringBootApplication` 内含 `@ComponentScan`，默认扫描**主类所在包及子包**）：

```java
@Configuration
@ComponentScan(
        basePackages = "com.example.order",        // 扫描范围
        excludeFilters = @ComponentScan.Filter(    // 排除规则
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {JpaOrderDao.class}),
        includeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = ".*Mock.*")              // 包含规则（需 useDefaultFilters=false）
)
public class AppConfig { }
```

**@Component 的四个分层语义注解**（本质相同，语义不同）：

| 注解 | 语义 | 典型位置 |
|------|------|---------|
| `@Component` | 通用组件 | 通用类 |
| `@Service` | 业务服务 | Service 层 |
| `@Repository` | 数据访问（DAO 异常转 Spring 数据异常） | DAO/Repository 层 |
| `@Controller` / `@RestController` | Web 控制器 | Controller 层 |

**扫描过滤器的五种类型**：

```java
FilterType.ANNOTATION       // 按注解排除（默认）
FilterType.ASSIGNABLE_TYPE  // 按类型/接口
FilterType.ASPECTJ          // AspectJ 表达式
FilterType.REGEX            // 类名正则
FilterType.CUSTOM           // 自定义 TypeFilter
```

> ⚠️ **经典坑**：Spring Boot 主类放在 `com.example`，而某个 `@Configuration` 放在 `com.example.other` 包外 → 扫描不到 → "Bean 找不到"。排查第一步：确认类的包在扫描范围内。

---

## 4. @Configuration 与 @Bean 的细节

**@Configuration 类的两个关键行为**：

```java
@Configuration
public class DataSourceConfig {

    @Bean
    public DataSource dataSource() {
        return new HikariDataSource();
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource ds) {   // 参数注入：容器自动找到 dataSource
        return new JdbcTemplate(ds);
    }
}
```

| 行为 | 说明 |
|------|------|
| 类被 CGLIB 代理 | 保证 `@Bean` 方法**单例语义**：内部互调 `dataSource()` 返回同一实例（不是重新 new） |
| 参数注入 | `@Bean` 方法参数自动装配（按类型），无需手动 `ctx.getBean` |
| 方法名即 Bean 名 | `dataSource()` → Bean 名 `dataSource`（`@Bean(name="...")` 可改名） |
| 返回类型即类型 | 配置的 Bean 类型是**方法返回类型** |

**@Bean 与 @Component 的本质区别**：

| 维度 | @Component | @Bean |
|------|:----------:|:-----:|
| 作用对象 | 类 | 方法 |
| 谁写的类 | 自己的类 | 任意类（含第三方） |
| 创建时机 | 容器直接实例化 | 方法被容器调用返回 |
| 可控性 | 弱（全靠反射默认） | 强（Java 代码完全可控） |

> 💡 **CGLIB 代理细节**（与 05 模块 AOP 呼应）：`@Configuration` 类本身被 CGLIB 代理——`@Bean` 方法被拦截后先查单例缓存，命中直接返回。**@Component 类没有这个行为**——所以 @Component 里的 @Bean 方法（反模式）每次调用都会新建。

---

## 5. 条件装配：@Conditional 家族

**条件装配**：BeanDefinition 注册时按条件判断"要不要注册"——是框架做"自动配置开关"的机制（Spring Boot 自动配置的核心）：

```java
// 内置条件注解（Spring Boot 提供，Spring 核心提供 @Conditional 基元）
@ConditionalOnProperty(name = "cache.enabled", havingValue = "true")   // 配置开关
@ConditionalOnClass(name = "redis.clients.jedis.Jedis")                 // 类路径存在
@ConditionalOnMissingBean(RedisTemplate.class)                           // 没有则注册
@ConditionalOnBean(...)                                                  // 有则注册
@ConditionalOnWebApplication / @ConditionalOnNotWebApplication          // 环境
```

```java
// 自定义条件：实现 Condition 接口
public class CacheCondition implements Condition {
    @Override
    public boolean matches(ConditionContext ctx, AnnotatedTypeMetadata meta) {
        String mode = ctx.getEnvironment().getProperty("cache.mode");
        return "redis".equals(mode);
    }
}

@Configuration
@Conditional(CacheCondition.class)          // 满足条件才加载整个配置类
public class RedisCacheConfig { ... }
```

**条件装配的判断时机**：BeanDefinition **注册阶段**（`ConditionEvaluator` 在解析配置类时求值）——早于 Bean 创建，所以它是"图纸级"开关。

> 🎯 **核心要点**：`@ConditionalOnMissingBean` 是"默认实现 + 允许覆盖"的标准姿势——框架提供默认 Bean，业务方想自定义就注册自己的，自动配置自动退让（`Spring生态深度剖析/02` 有自动配置内核）。

---

## 6. Spring 7.0 新成员：BeanRegistrar

**Spring Framework 7.0（2025-11）** 引入 **BeanRegistrar**——一等公民的**编程式 Bean 注册 API**，专为 AOT/原生镜像设计（可被构建期分析）：

```java
// 传统编程式注册（6.x）：运行时反射驱动，GraalVM 原生镜像下需要额外 reachability 配置
GenericApplicationContext ctx = ...;
ctx.registerBean("myService", MyService.class, bd -> bd.setScope("prototype"));

// Spring 7.0：BeanRegistrar 显式声明注册逻辑，构建期可静态分析
public class MyRegistrar implements BeanRegistrar {
    @Override
    public void registerBeans(BeanRegistrarRegistry registry) {
        registry.registerBean("myService", MyService.class, bd -> bd.setScope("prototype"));
    }
}
// 在 @Configuration 类上：@Import(MyRegistrar.class) 或 @Bean 方法返回
```

**BeanRegistrar 的价值**（面试谈 7.0 时的加分点）：

| 维度 | 旧 registerBean | BeanRegistrar |
|------|:---------------:|:-------------:|
| AOT 分析 | 运行时才可见 | 构建期可静态分析 |
| 原生镜像 | 需手写 reachability 元数据 | 开箱即用 |
| API 设计 | 分散在容器上 | 集中、可测试 |
| 版本 | 6.x | **7.0+** |

> ⚠️ 版本提示：Spring Framework 7.0 是代际升级——`javax.annotation`/`javax.inject` 已被**完全移除**（静默忽略，不报错——迁移陷阱），必须换 `jakarta.*`；`spring-jcl` 模块移除（用 Apache Commons Logging 1.3）。迁移细节见 `Spring生态/03-SpringBoot4与SpringFramework7新特性.md`。

---

**下一模块**：[03-依赖注入详解与自动装配](./03-依赖注入详解与自动装配.md) / **返回总览**：[00-Spring框架核心知识体系总览](./00-Spring框架核心知识体系总览.md)
