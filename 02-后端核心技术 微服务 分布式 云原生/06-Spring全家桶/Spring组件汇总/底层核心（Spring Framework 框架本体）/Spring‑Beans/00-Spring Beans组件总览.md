# 00 Spring Beans 组件总览

> 组件卡片：spring-beans 是什么、版本现状、能做什么、与深度体系如何衔接——本系列是"组件汇总"速查型文档，深挖见 [Spring框架核心-01~04](../../../Spring框架核心/00-Spring框架核心知识体系总览.md)

---

## 📚 目录

1. [组件一句话定位](#1-组件一句话定位)
2. [版本现状（2026-08）](#2-版本现状2026-08)
3. [能力地图](#3-能力地图)
4. [与深度体系的映射](#4-与深度体系的映射)
5. [快速上手 3 步](#5-快速上手-3-步)
6. [速查导航](#6-速查导航)
7. [学习路线推荐](#7-学习路线推荐)
8. [核心概念速查](#8-核心概念速查)

---

## 1. 组件一句话定位

**spring-beans 是 Spring IoC 容器的心脏**——提供 Bean 的定义（BeanDefinition）、注册（BeanDefinitionRegistry）、工厂（BeanFactory）、装配（Autowired/类型转换）与生命周期回调（BeanPostProcessor）全部机制，是 @Autowired、@Bean、@Component 以及一切"容器管理对象"的底层地基。

```text
核心心智模型：
  BeanDefinition（图纸：如何创建这个对象？）
      ↓ 注册
  BeanDefinitionRegistry（图纸仓库）
      ↓ 实例化流程（getBean）
  BeanFactory（工厂：创建 + 初始化 + 缓存单例）
      ↑
  BeanPostProcessor（在每个 Bean 创建前后插手）
      ↑
  ApplicationContext（企业级容器 = 工厂 + 更多能力）

  一句话：图纸 → 仓库 → 工厂 → 后处理器，四层协作管理对象生命周期。
```

| 维度 | 说明 |
|------|------|
| 所属 | Spring Framework 底层核心模块（spring-beans） |
| 版本线 | 随 Spring Framework 7.x（2025-11 起） |
| 语言要求 | Java 17+ |
| 定位 | IoC 容器与 Bean 生命周期管理（一切 Spring 能力的地基） |

### 1.1 四大机制与源码锚点

| 机制 | 一句话职责 | 关键源码位置 |
|------|-----------|-------------|
| 定义机制 | 把 XML / 注解 / 程序化代码统一翻译成 BeanDefinition | `XmlBeanDefinitionReader` / `ConfigurationClassParser` |
| 注册机制 | 图纸入库、可覆盖、可移除 | `BeanDefinitionRegistry.registerBeanDefinition` |
| 工厂机制 | 按图纸创建对象、缓存单例、处理依赖 | `AbstractBeanFactory.doGetBean` |
| 回调机制 | 在创建关键节点允许外部插手 | `BeanPostProcessor` 链（容器内排序后逐个执行） |

> ⚠️ **边界认知**：spring-beans 模块本身**不含**事件（context）、事务（tx）、Web 作用域（web）——这些能力由上层模块依赖 beans 后叠加。排查问题时先确认"报错的类在哪个模块"，避免在 beans 层找 context 的能力。

### 1.2 为什么需要"容器"——设计动机与代价

- **控制权反转**：`new` 的权利交给容器，业务代码只声明"我需要什么"，实现切换零改动；
- **统一生命周期管理**：连接池、线程池等资源型 Bean 的创建、初始化、销毁由容器保证不泄漏；
- **AOP 织入的挂载点**：没有容器统一管理实例，代理就无法在创建时自动套上（见 07 篇第 1 节）。

> 💡 **权衡取舍**：容器的代价是"启动扫描与代理开销"+"隐式行为难以直接阅读"——因此现代实践强调构造器注入（依赖显式化）与 Full 模式 `@Configuration`（行为可预期），把隐式魔法压到最小。

## 2. 版本现状（2026-08）

| 版本 | 说明 |
|------|------|
| Spring Framework 7.0（2025-11） | 当前主线（Boot 4.0 配套） |
| Spring Framework 7.1（2026-05） | 最新维护线（Boot 4.1 配套） |
| Spring Framework 6.2.x | 存量主线（Boot 3.4/3.5 配套） |

**7.x beans 关键变化：**

- **`BeanRegistrar`（新接口，7.0）**：程序化注册 Bean 的一等公民——函数式接口 `register(BeanRegistry, Environment)`，配合 `@Import` 使用，支持 if/for/Environment 条件注册，**AOT / GraalVM 原生镜像友好**；取代了繁琐的 `BeanDefinitionRegistryPostProcessor` 手写 BeanDefinition 模式，Kotlin 侧有 `BeanRegistrarDsl`（2025-03 的 7.0.0-M3 引入）；
- **`@Fallback`（新注解，7.0）**：与 `@Primary` 相对的兜底候选——标注后仅当没有其他候选 Bean 时才被选中，程序化注册时对应 `fallback` 标志位；
- **`@Proxyable`（新注解，7.0）**：按 Bean 粒度指定代理类型（如 `@Proxyable(proxyTargetClass = true)`），与 Boot 的全局 CGLIB 默认互补；
- **注册选项扩展**：`registerBean` 规格支持 `backgroundInit`（后台初始化）、`infrastructure`、`order` 等细粒度控制；
- **BeanDefinition API 持续演进**：7.0.x 仍在使用 `RootBeanDefinition` / `GenericBeanDefinition` / `SimpleBeanDefinitionRegistry`，但推荐新代码走 `registerBean` 高 API 而非手工构造。

> ⚠️ **要点**：7.0 的 beans 变化是"注册方式升级"而非"容器机制重写"——`BeanRegistrar` 是新代码的首选注册方式，但 `@Component` 扫描、`@Bean`、XML 三种传统方式完全兼容；升级主要影响"手写 BeanDefinition + BeanDefinitionRegistryPostProcessor"的存量代码，可平滑迁移。

### 2.1 版本演进脉络速查

| 版本线 | 关键变化 |
|--------|---------|
| 2.x ~ 3.x（2005-2010） | XML 为主；`@Autowired` 注解注入兴起（2.5 引入） |
| 4.x（2014） | 单构造器注入可省略 `@Autowired`；支持 Java 8 |
| 5.x（2017） | 内部重构支撑响应式；`@Indexed` 候选组件索引出现 |
| 6.x（2022） | `javax.*` → `jakarta.*` 命名空间迁移；AOT 起步 |
| 7.x（2025） | `BeanRegistrar` / `@Fallback` / `@Proxyable`；Java 17+ 基线 |

> 💡 面试追问"Spring 6 升级为什么痛苦"：主要是 `javax.*` → `jakarta.*` 的**包名迁移**，beans 层容器机制本身没有推翻重来——理解这一点，就能判断报错到底是"命名空间问题"还是"机制问题"。

### 2.2 升级 7.x 前的检查清单

| # | 检查项 | 处理动作 |
|---|--------|---------|
| 1 | 是否手写 BeanDefinition + Registry 注册 | 迁到 `BeanRegistrar`（新代码的首选） |
| 2 | 是否依赖 6.x 已废弃 API | 按 IDE 编译警告清单逐个替换 |
| 3 | 自定义 `BeanPostProcessor` 是否依赖注册顺序 | 7.x 对后处理器排序更严格，显式实现 `Ordered` |
| 4 | 是否使用第三方扩展的 `@Fallback` 语义 | 需框架 7.x 才识别，确认扩展已升级 |
| 5 | Java 版本是否满足 17+ | 7.x 基线 Java 17，低于此需先升 JDK |

> ⚠️ 结论：6.x → 7.x 的破坏面远小于 5.x → 6.x（那次是命名空间大迁移）；绝大多数存量应用升级只涉及依赖坐标与少量配置，容器语义不变。

## 3. 能力地图

| 能力域 | 能力点 | 对应注解/API |
|--------|--------|-------------|
| Bean 定义 | 三类定义来源 | XML `<bean>`、`@Bean`、`@Component` 扫描 |
| 容器工厂 | 最底层容器 | `BeanFactory`、`DefaultListableBeanFactory` |
| 企业容器 | 完整上下文 | `ApplicationContext`（见 Spring-Context 系列） |
| 依赖注入 | 注入与自动装配 | `@Autowired`、`@Resource`、构造器注入 |
| 装配裁决 | 冲突解决 | `@Primary`、`@Fallback`（7.0）、`@Qualifier` |
| 生命周期 | 创建/初始化/销毁钩子 | `BeanPostProcessor`、`@PostConstruct`、`InitializingBean` |
| 作用域 | 单例/原型/请求/会话 | `@Scope`、`@Lazy`、`ObjectProvider` |
| 程序化注册 | 代码注册 Bean | `BeanRegistrar`（7.0）、`BeanDefinitionRegistry.registerBeanDefinition` |
| 条件装配 | 按条件启停 | `@Conditional`、`@Profile` |

### 3.1 最小闭环：纯 beans API 手写容器

```java
// 脱离 Spring Boot / context，只用 spring-beans 跑通"定义→注册→获取"
DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
factory.registerBean("greet", GreetService.class);        // 图纸=类，容器自动推导
factory.registerBean("greeter", Greeter.class, spec -> spec
        .supplier(ctx -> new Greeter(ctx.bean(GreetService.class))));  // 声明依赖

Greeter greeter = factory.getBean(Greeter.class);         // 按类型获取
greeter.sayHello();                                       // 无需扫描/事件/AOP
```

> 💡 这段代码演示了 beans 的**能力下限**：`DefaultListableBeanFactory` + `registerBean` + `getBean` 即构成最小 IoC；加上 `BeanPostProcessor` 就接近完整容器——理解下限，再看上层模块如何叠加能力。

### 3.2 能力地图对应的启动阶段（refresh 时序）

| 启动阶段 | 能力域生效点 | 对应能力 |
|---------|-------------|---------|
| 配置解析期 | `ConfigurationClassPostProcessor`（BeanFactoryPostProcessor） | Bean 定义、条件装配 |
| 后处理器注册期 | `registerBeanPostProcessors` | 回调机制就位 |
| 单例预加载期 | `finishBeanFactoryInitialization` → 逐个 getBean | 工厂机制、生命周期、作用域 |
| 收尾期 | `SmartInitializingSingleton` | 全量回调 |

> 🎯 记忆锚点：**能力地图的每一行，都能在 refresh() 的某一阶段找到落点**——面试问"启动分几步"时，按"解析定义 → 注册后处理器 → 预加载单例"三步主干展开即可。

### 3.3 能力边界：什么不在 beans 里

| 能力 | 所属模块 | 常见误解 |
|------|---------|---------|
| 事件发布/监听 | spring-context | 误以为容器能力属于 beans |
| `@Transactional` | spring-tx + aop | 只是"挂"在 beans 的生命周期上 |
| 请求/会话作用域 | spring-web（注册实现）+ context | 定义在 beans，实现在 web |
| 自动配置 | spring-boot-autoconfigure | Boot 专属，beans 无此概念 |

> 💡 区分技巧：**beans 管"一个 Bean 怎么活"，其余模块管"Bean 有哪些能力"**——凡是问"注解/能力属于哪一层"，按此二分法先切一刀。

## 4. 与深度体系的映射

| 速查文档 | 深度体系模块 |
|---------|-------------|
| 01-模块清单 | [Spring框架核心-01-容器体系](../../../Spring框架核心/01-容器体系-BeanFactory与ApplicationContext.md) |
| 02-核心类与接口速查 | [Spring框架核心-01](../../../Spring框架核心/01-容器体系-BeanFactory与ApplicationContext.md) |
| 03-注解与元数据速查 | [Spring框架核心-02-BeanDefinition与三种配置方式](../../../Spring框架核心/02-BeanDefinition与三种配置方式.md) |
| 04-配置属性速查 | [Spring框架核心-02](../../../Spring框架核心/02-BeanDefinition与三种配置方式.md) |
| 05-Bean生命周期与作用域速查 | [Spring框架核心-04-Bean生命周期与作用域](../../../Spring框架核心/04-Bean生命周期与作用域.md) |
| 06-依赖注入与自动装配速查 | [Spring框架核心-03-依赖注入详解与自动装配](../../../Spring框架核心/03-依赖注入详解与自动装配.md) |
| 07-集成地图与常见问题 | [Spring生态深度剖析-01-Bean生命周期与PostProcessor](../../../Spring生态深度剖析/01-Bean生命周期与PostProcessor扩展体系.md) |

> 💡 本系列定位"查得快"，深度体系定位"学得透"——速查命中后，跳转深度体系看机制与源码；循环依赖、三级缓存等深入话题见 [Spring生态深度剖析-03-循环依赖与三级缓存](../../../Spring生态深度剖析/03-循环依赖与三级缓存深度剖析.md)。

### 4.1 两套体系的使用分工

| 场景 | 用什么 | 为什么 |
|------|--------|--------|
| 面试突击 / 日常查 API | 本系列速查 | 一张表一个主题，命中快 |
| 写扩展 / 二次开发框架 | 深度体系 | 需要源码级机制与扩展契约 |
| 排查诡异故障 | 07 常见问题 → 深度体系溯源 | 现象表先定位，再深挖根因 |

> 🎯 **配套使用建议**：读深度体系时带着本系列的问题视角（"这段讲的是哪个速查表的哪一行"），读速查时记下深挖入口——两套体系互为索引，比单独啃任何一套都高效。

## 5. 快速上手 3 步

**① 引入依赖**（任何 Spring 项目都已间接引入，无需显式声明）：

```xml
<!-- 通常只需 spring-context，它传递依赖 spring-beans + spring-core -->
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-context</artifactId>
    <version>7.0.6</version>
</dependency>
```

**② 定义 Bean**（三种方式任选，见 03-注解与元数据速查）：

```java
// 方式 A：组件扫描（最常用）
@Service
public class OrderService { }

// 方式 B：@Bean 工厂方法
@Configuration
public class AppConfig {
    @Bean
    public OrderService orderService() {
        return new OrderService();
    }
}

// 方式 C：程序化注册（Spring 7 推荐新方式）
@Configuration
@Import(MyRegistrar.class)
public class AppConfig { }

class MyRegistrar implements BeanRegistrar {
    @Override
    public void register(BeanRegistry registry, Environment env) {
        registry.registerBean("orderService", OrderService.class);
    }
}
```

**③ 注入使用**：

```java
@RestController
public class OrderController {
    private final OrderService orderService;   // 构造器注入（首选）

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }
}
```

**⑤ 手动验证容器**（脱离 Boot 的单元级验证）：

```java
AnnotationConfigApplicationContext ctx =
        new AnnotationConfigApplicationContext(AppConfig.class);
OrderService orderService = ctx.getBean(OrderService.class);   // 按类型取
String[] names = ctx.getBeanDefinitionNames();                 // 注册全量
ctx.close();                                                   // 触发销毁回调
```

**失败表现速查**（前三步哪一环出错，报什么错）：

| 环节出错 | 典型报错 | 含义 |
|---------|---------|------|
| 没扫描到 | `NoSuchBeanDefinitionException` | 类没注册（路径/条件/注解问题） |
| 多候选无裁决 | `NoUniqueBeanDefinitionException` | 缺 `@Primary` / `@Qualifier` |
| 构造参数解析失败 | `UnsatisfiedDependencyException` | 依赖 Bean 缺失或类型不匹配 |
| 循环依赖 | `BeanCurrentlyInCreationException` | 构造器环，或 Boot 默认禁环生效 |

**三种定义方式怎么选**：

| 方式 | 适用场景 | 优势 | 代价 |
|------|---------|------|------|
| `@Component` 扫描 | 自研业务类 | 零配置、语义清晰 | 类需可被扫描（路径/条件） |
| `@Bean` 工厂方法 | 三方库类、需定制初始化 | 创建逻辑可见、可参数化 | 需要配置类（Full/Lite 注意） |
| `BeanRegistrar`（7.0） | 动态/批量/条件注册 | if/for/Environment、AOT 友好 | 可读性弱于注解 |

> 💡 决策顺序：**静态确定 → 注解；动态确定 → BeanRegistrar**；两种混用完全合法——一个应用里三者共存是常态，不必强求统一。

> ⚠️ 唯一需要避免的：**同一定义在多处注册**（如 @Component + @Bean 同时声明同类）——会触发 BeanDefinition 覆盖或重复创建，属设计缺陷而非选择问题。

## 6. 速查导航

| 文档 | 内容 |
|------|------|
| [01-模块清单](01-模块清单.md) | spring-beans artifact 与包结构、依赖边界 |
| [02-核心类与接口速查](02-核心类与接口速查.md) | BeanFactory/BeanDefinition/Registry/PostProcessor/BeanRegistrar |
| [03-注解与元数据速查](03-注解与元数据速查.md) | @Component 家族 / @Bean / @Autowired 全家 / @Conditional |
| [04-配置属性速查](04-配置属性速查.md) | XML 命名空间 + Boot 属性 + registerBean 规格 |
| [05-Bean生命周期与作用域速查](05-Bean生命周期与作用域速查.md) | 生命周期时序、Aware、五大作用域 |
| [06-依赖注入与自动装配速查](06-依赖注入与自动装配速查.md) | 注入方式、装配规则、冲突解决、循环依赖 |
| [07-集成地图与常见问题](07-集成地图与常见问题.md) | 与 Core/Context/AOP/TX/Web 联动 + 高频坑 |

**按问题翻文档**：

| 我的问题 | 翻哪篇 |
|---------|--------|
| 某个注解怎么用 / 为什么不生效 | 03-注解与元数据速查 |
| 容器启动流程 / 工厂类关系 | 02-核心类与接口速查 |
| 生命周期回调顺序 | 05-Bean生命周期与作用域速查 |
| 注入报错 / 多实现选择 | 06-依赖注入与自动装配速查 |
| 升级 7.x 注意什么 | 01-模块清单 + 04-配置属性速查 |
| 生产故障现象 | 07-集成地图与常见问题 |

## 7. 学习路线推荐

| 路线 | 适用 | 顺序 |
|------|------|------|
| 入门速查 | 第一次接触 Spring 容器 | 00 总览 → 03 注解速查 → 05 生命周期 → 06 依赖注入 → 01 模块清单 |
| 项目实践 | 用/排查 Spring 应用 | 02 核心类 → 04 配置属性 → 07 常见问题 → 深度体系（Spring框架核心-01~04） |
| 面试冲刺 | 高频考点覆盖 | 05 生命周期 → 06 循环依赖 → 02 容器体系 → 07 考点清单 → [Spring框架核心-10-面试高频考点](../../../Spring框架核心/10-面试高频考点与总结.md) |

## 8. 核心概念速查

| 术语 | 一句话解释 |
|------|-----------|
| BeanDefinition | Bean 的"图纸"——所有配置最终翻译成的定义 |
| BeanFactory | 最底层容器工厂（getBean 入口） |
| ApplicationContext | 企业级容器（工厂 + 事件/国际化/属性） |
| BeanPostProcessor | 每个 Bean 初始化前后插手的扩展点 |
| 循环依赖 | A↔B 相互依赖，三级缓存解决字段注入场景 |
| FactoryBean | 工厂 Bean：getObject() 产出目标对象 |
| ObjectProvider | 延迟/可选/流式获取 Bean 的能力注入 |
| BeanRegistrar（7.0） | 程序化注册 Bean 的新标准接口 |

---

**下一模块**：[01-模块清单](01-模块清单.md)　**返回总览**：本页

**【参考来源】**：Spring Framework 7.0.0-M3 发布说明（spring.io/blog）、[Programmatic Bean Registration 官方文档](https://docs.spring.io/spring-framework/reference/core/beans/java/programmatic-bean-registration.html)、[Baeldung: Spring BeanRegistrar 程序化注册](https://baeldung.cn/spring-beanregistrar-registration)、Spring Framework Javadoc 7.0.0-M1（docs.spring.io）
