# 07 - Spring 与 SpringBoot

> 🎯 Spring 题从"会不会用"到"懂不懂原理"层层递进——Bean 生命周期、循环依赖三级缓存、AOP 动态代理、事务失效场景、自动装配是五大必考主线。本模块对标 Spring Framework 7 / Boot 4（2026 生产基线）

## 📚 目录

1. [IOC 容器与 DI 思想](#1-ioc-容器与-di-思想)
2. [Bean 生命周期十二步](#2-bean-生命周期十二步)
3. [循环依赖与三级缓存](#3-循环依赖与三级缓存)
4. [AOP：动态代理与切面](#4-aop动态代理与切面)
5. [Spring 事务：传播与失效](#5-spring-事务传播与失效)
6. [SpringBoot 自动装配](#6-springboot-自动装配)
7. [常用注解速查](#7-常用注解速查)
8. [高频面试题与追问预案](#8-高频面试题与追问预案)

## 1. IOC 容器与 DI 思想

**IOC（控制反转）**：对象创建与依赖管理的控制权从代码里反转给容器——你不再 `new` 依赖，而是声明需求，容器注入。**DI（依赖注入）**是 IOC 的实现手段（构造器/Setter/字段注入）。一句话区分必背："IOC 是思想，DI 是手段，容器（ApplicationContext）是载体"。

**为什么用 IOC**（面试必问动机）：解耦（类只面向接口声明依赖，替换实现零改动——测试 mock 也方便）、统一生命周期管理（单例复用、延迟加载、销毁回调）、集中配置（Bean 定义与业务分离）。追问"构造器注入 vs 字段注入"——官方推荐**构造器注入**（依赖不可变、显式、可测试、防循环依赖提前暴露），字段注入虽简洁但依赖隐藏且无法 final。Boot 4 时代默认单例，prototype 与 request/session 作用域按需。

## 2. Bean 生命周期十二步

Bean 生命周期是必背大题，按"实例化 → 属性填充 → 初始化 → 销毁"四段记忆：**实例化**（构造器/工厂）→ **属性填充**（依赖注入）→ **Aware 回调**（BeanNameAware → BeanFactoryAware → ApplicationContextAware）→ **BeanPostProcessor#postProcessBeforeInitialization** → **@PostConstruct / InitializingBean / init-method**（三者执行顺序）→ **BeanPostProcessor#postProcessAfterInitialization**（AOP 代理在这里生成）→ 使用 → **@PreDestroy / DisposableBean / destroy-method**。

两个关键理解：**AOP 代理在 postProcessAfterInitialization 阶段生成**——所以代理对象才是最终放进容器的 Bean；**回调顺序**（Aware → 初始化前 → 初始化 → 初始化后）要能默写。追问"初始化和构造的区别"——构造是 new 对象，初始化是依赖就绪后的自定义逻辑（@PostConstruct 里才能安全使用注入的依赖）。

## 3. 循环依赖与三级缓存

循环依赖：A 依赖 B、B 依赖 A，Spring 用**三级缓存**解决单例的 setter 注入循环依赖：**一级（singletonObjects）存成品 Bean、二级（earlySingletonObjects）存提前暴露的半成品、三级（singletonFactories）存创建工厂（ObjectFactory）**。

完整流程一句话：**A 实例化 → 填充依赖发现 B → 创建 B → B 填充依赖发现 A → A 从三级缓存拿到 ObjectFactory 生成早期引用暴露给 B（AOP 提前代理）→ B 完成 → A 拿到 B 继续完成**。两个必背边界：**构造器注入无法解决循环依赖**（实例化阶段就互相需要，直接报错）；**多例 prototype 不解决**（容器不缓存）。追问"三级缓存为什么三级不是两级"——三级存 ObjectFactory 是为了**延迟 AOP 代理创建**：若 A 不需要代理则直接返回原始引用，避免提前代理；若需要代理则在三级缓存生成。这个解释答出来直接封神。

## 4. AOP：动态代理与切面

AOP（面向切面编程）：把日志、事务、鉴权、性能监控等**横切逻辑**从业务代码中抽离，运行时织入。**两种代理机制必背**：JDK 动态代理（基于接口，Proxy + InvocationHandler，**目标类必须实现接口**）与 CGLIB（基于继承，字节码生成子类，**无接口可用**；Boot 默认 proxyTargetClass=true 优先 CGLIB）。

**Spring AOP vs AspectJ 辨析**：Spring AOP 是**运行期动态代理**（对象级，只代理 Spring 管理的 Bean，只能拦截 public 方法调用）；AspectJ 是**编译期/加载期织入**（类级，可拦截构造器、静态方法、私有方法）。**切面执行顺序**（@Around 前置 → @Before → 方法 → @AfterReturning/@AfterThrowing → @After → @Around 后置）要会排。经典陷阱：**同类内部调用 `this.method()` 不走代理**（自调用失效——AOP、@Transactional 同理失效），解法是注入自身代理或拆类。

## 5. Spring 事务：传播与失效

事务传播行为七个，必背三个：**REQUIRED**（默认，有事务就加入、没有就新建）、**REQUIRES_NEW**（挂起当前事务开新事务——日志记录/发消息场景）、**NESTED**（嵌套事务，savepoint 回滚点）。**事务隔离级别**与 MySQL 篇对应：DEFAULT 跟随数据库（MySQL 可重复读）、READ_COMMITTED 常用（锁范围更小）。

**事务失效八大场景**（最高频考点）：**方法非 public、自调用（同类 this.调用）、异常被 catch 吞掉、抛出受检异常（默认不回滚，需 rollbackFor）、类没被 Spring 管理、final 方法（CGLIB 无法代理）、事务方法跨线程调用、数据库引擎不支持事务**。每题答案配一个真实场景记忆：比如"异常被吞"——try-catch 后事务感知不到异常，提交了脏数据。回答模板：说出场景 + 为什么失效 + 怎么修。

## 6. SpringBoot 自动装配

自动装配三件套必背：**@EnableAutoConfiguration → spring.factories/`AutoConfiguration.imports` → @ConditionalOnXxx**。流程：启动类 @SpringBootApplication 组合了 @EnableAutoConfiguration → 加载 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 里的全部自动配置类 → 每个配置类用 @ConditionalOnClass/@ConditionalOnMissingBean 等条件注解按需生效。**starter 的本质**：一个自动配置模块——`spring-boot-starter-data-redis` 引入依赖 + 自动配置类 + 默认属性，开箱即用。

追问"自定义 starter 怎么做"：三步——写自动配置类（@Configuration + @ConditionalOnClass）、`AutoConfiguration.imports` 注册、提供属性绑定类（@ConfigurationProperties）。**Boot 4 新视角**：自动装配机制不变，但底层换 Framework 7——虚拟线程原生集成（VirtualThreadTaskExecutor）、@Retryable/@CircuitBreaker 原生注解（无需 Resilience4j）、JSpecify 空安全注解、Jakarta EE 11 命名空间（javax→jakarta）。能带出这些 2026 新特性是加分项。

**为什么叫"约定优于配置"**（Convention over Configuration）是自动装配的思想底座：框架提供合理默认值（默认端口 8080、默认数据源、默认日志级别），开发者只需要覆盖差异——starter 就是把"约定"打包成可复用模块。追问"配置项怎么生效"：`application.yml` 里的 `spring.data.redis.*` 属性被属性绑定类（如 RedisProperties）读取，自动配置类根据绑定值装配 RedisConnectionFactory——**属性 → 绑定类 → 自动配置类**三层链路要能说清。再追问"两个 starter 都定义了同一个 Bean 会怎样"——条件注解 + @Primary/@Order 控制优先级，答出 ConditionalOnMissingBean（自定义优先）即加分。

## 7. 常用注解速查

速查表按层记忆，回答"你在项目里用过哪些注解"时按层展开显体系感：

| 层级 | 注解 |
|------|------|
| 组件注册 | @Component / @Service / @Repository / @Controller / @Configuration |
| 依赖注入 | @Autowired / @Resource / @Value / @Qualifier / @Primary |
| Web 层 | @RestController / @RequestMapping / @PathVariable / @RequestBody / @Validated |
| AOP | @Aspect / @Before / @After / @Around / @Pointcut |
| 事务 | @Transactional（readOnly/timeout/rollbackFor/传播/隔离） |
| 配置 | @ConfigurationProperties / @EnableConfigurationProperties / @Profile |
| 测试 | @SpringBootTest / @MockitoBean（Boot 4 新名）/ @TestBean |

追问点：@Autowired 与 @Resource 区别（类型 vs 名称优先）；@Component 与 @Configuration 区别（后者 CGLIB 代理保证单例）；@Transactional 加在 Service 还是 Controller（Service——事务边界在业务层）。

## 8. 高频面试题与追问预案

### 基础概念档

| # | 题目 | 答题要点 |
|---|------|----------|
| 1 | IOC 是什么？ | 控制反转 / DI 注入 / 解耦-生命周期-配置 |
| 2 | Bean 生命周期？ | 实例化→属性填充→Aware→初始化前后→销毁 四段十二步 |
| 3 | JDK 代理和 CGLIB 区别？ | 接口 vs 继承 / 目标类是否实现接口 |
| 4 | 事务传播行为？ | REQUIRED / REQUIRES_NEW / NESTED 三个必背 |

### 工程实践档

| # | 题目 | 答题要点 |
|---|------|----------|
| 5 | 事务失效遇到过吗？ | 八大场景挑 2-3 个 / 自调用与异常吞掉最常见 / 修复方式 |
| 6 | 循环依赖项目里出现过？ | 构造器注入报错 / setter 注入三级缓存解决 / 设计上避免 |
| 7 | 自定义 starter？ | 自动配置类 + imports 注册 + 属性绑定 |

### 架构设计档

| # | 题目 | 答题要点 + 追问点 |
|---|------|------------------|
| 8 | 三级缓存为什么设计成三级？ | ObjectFactory 延迟 AOP / 提前暴露 vs 提前代理；追问"构造器循环依赖为什么无解" |
| 9 | AOP 用来做什么？ | 日志/事务/鉴权/监控 / 切面复用；追问"同类自调用为什么失效" |
| 10 | Boot 4 相比 3 的变化？ | Framework 7 / 虚拟线程原生 / 原生弹性注解 / jakarta 命名空间 |

### 追问速查

@Autowired 注入原理（BeanPostProcessor 的 AutowiredAnnotationBeanPostProcessor）；prototype Bean 的 AOP 代理是每次新代理吗（是）；@Transactional 和 AOP 什么关系（事务就是 AOP 的一个切面实现）；Spring 容器是单例吗（默认单例池）；Boot 4 后 @Transactional 的默认回滚行为有变化吗（没有，依旧默认运行时异常回滚、受检异常需 rollbackFor 显式声明）。

> 🎯 **核心要点**：Spring 题的通吃公式 = **思想（IOC/AOP）→ 机制（生命周期/三级缓存/动态代理）→ 场景（事务/失效/自调用）→ 版本（Boot 4/Framework 7）**。Spring 题最能区分"背答案"和"真懂"——追问一次机制原理就现形，所以每道题都要准备"为什么"的下一层。

---

**下一模块**：[08-计算机网络与HTTP](08-计算机网络与HTTP.md) / **返回总览**：[00-阶段一基础复盘总览](00-阶段一基础复盘总览.md)
