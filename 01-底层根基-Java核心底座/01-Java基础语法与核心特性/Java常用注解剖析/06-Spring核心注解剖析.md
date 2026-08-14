# 06 Spring 核心注解剖析

> Spring 注解体系的本质是把「对象如何被创建、装配、增强」从代码里搬到声明上：@Component 族解决"注册什么"、@Autowired 解决"注入什么"、@Configuration 解决"怎么创建"、@Transactional 解决"围绕方法加什么行为"。Spring 7 的新动作是 Resilience 注解入核与 JSpecify 空安全

## 📚 目录

1. [组件注册四件套与派生注解机制](#1-组件注册四件套与派生注解机制)
2. [装配与配置：@Autowired / @Value / @Configuration](#2-装配与配置autowired--value--configuration)
3. [行为注解：@Transactional / @Async / @Scheduled / @Cacheable](#3-行为注解transactional--async--scheduled--cacheable)
4. [Spring 7 新注解与迁移红线](#4-spring-7-新注解与迁移红线)
5. [高频坑与面试题](#5-高频坑与面试题)

---

## 1. 组件注册四件套与派生注解机制

@Component/@Service/@Repository/@Controller 是**派生关系**：后三者是 @Component 的元注解组合（meta-annotation composition）。这个机制值得深挖——Spring 扫描时用 `MergedAnnotations` 递归搜索注解的注解，于是"标了 @Service 的类"自动等价于"标了 @Component"。理解这一点才能理解两件事：

1. **自定义派生注解**：`@Retention(RUNTIME) @Target(TYPE) @Component public @interface MyService {}` 就能获得可被扫描的团队专属注解——这正是 10 篇自定义注解实战的 Spring 版姿势；
2. **@AliasFor 别名桥**：@Repository 内部的 `value` 通过 `@AliasFor(annotation = Component.class, attribute = "value")` 与 @Component.value 建立别名关系，指定任意一个等于同时指定两个。

四件套的差异在语义与副作用：@Repository 额外获得**持久层异常翻译**（把 JDBC/Hibernate 的厂商异常转译为 Spring 的 `DataAccessException` 体系，要求 `PersistenceExceptionTranslator` 支持）；@Controller 在 Web 体系里触发 Handler 注册；@Service/@Component 纯标记。工程规范是"分层标注"——虽然技术上全用 @Component 也能跑，但异常翻译、AOP 切点、文档语义都会退化。

---

## 2. 装配与配置：@Autowired / @Value / @Configuration

**@Autowired** 的 2026 共识是**构造器注入为标准**：不可变 final 字段、可测试性、天然暴露依赖爆炸。Spring 4.3+ 起单构造器场景 @Autowired 可省略，Spring 7 延续此语义。字段注入仍大量存在于存量代码，其代价是：字段可变、隐式依赖、单元测试必须启动容器。@Autowired 按类型装配，多个候选时按名称匹配（字段名/参数名），仍歧义则报 `NoUniqueBeanDefinitionException`——解决手段：`@Primary`（默认优先）或 `@Qualifier("xxx")`（点名，优先级高于 @Primary）。注意 `@Resource`（jakarta.annotation）的匹配顺序是**先按名后按类型**，与 @Autowired 相反。

**@Value** 两条语法线：占位符 `${server.port:8080}`（Environment 解析，冒号后默认值——**URL 含冒号的场景要用 `${x:}` 显式空默认再拼接**，否则解析异常）与 SpEL `#{T(java.lang.Math).random()}`。Spring 7 起配置类趋势是 `@ConfigurationProperties` + record：不可变、可校验、可生成元数据，@Value 只留在散点注入场景。

**@Configuration** 的 `proxyBeanMethods` 是关键开关：true（full 模式，默认）时配置类被 CGLIB 代理，@Bean 方法间调用会拦截并复用容器单例——依赖"方法调用返回同一实例"的写法成立，代价是代理开销与 final 方法/private 方法的限制；false（lite 模式）代理关闭，每次调用创建新实例，但启动更快、更贴近纯函数。@Bean/@Scope/@Lazy/@Profile/@ConditionalOnXxx 与 @Configuration 协作构成"条件化创建"闭环。

**条件装配族**（@ConditionalOnClass/@ConditionalOnMissingBean/@ConditionalOnProperty/@ConditionalOnWebApplication 等）是自动配置的地基：条件不满足的配置类整体跳过。踩坑重点在 `@ConditionalOnProperty`——属性名拼写错误时条件永远为 false，组件**静默消失**，排查方向是看 `ConditionEvaluationReport`（Actuator 的 `/conditions` 端点直接列出每个条件评估结果）。同族的 `@ConditionalOnMissingBean` 是"用户自定义优先于默认实现"的标准写法（Spring Boot 自动配置的每个 Bean 都有这个保护）。`@EnableXxx` 家族（@EnableAsync/@EnableScheduling/@EnableTransactionManagement）则是功能开关模式：注解上 @Import 导入配置类，把"引依赖"与"开功能"解耦——自研组件对外暴露开关时照抄这个模式。另两个装配期注解：`@DependsOn("xxx")`（显式声明 Bean 初始化顺序，兜底无依赖声明时的初始化竞态）与 `@Order/@Priority`（同类型 Bean 的排序，影响注入集合与拦截器/过滤器链顺序）。`@EventListener` 替代 ApplicationListener 接口——任意 @Bean 方法加注解即注册事件监听，`@TransactionalEventListener` 让监听器在事务提交后执行（发消息、刷缓存的正确时机）。

---

## 3. 行为注解：@Transactional / @Async / @Scheduled / @Cacheable

这类注解的共同机制是**AOP 代理拦截**：注解只是切点标记，行为由代理对象在方法调用前后织入。机制相同，失效模式也就相同——**凡是绕过代理的调用，注解全部失效**。

@Transactional 的失效清单（面试与生产双料高频）：自调用（类内 `this.method()` 不过代理）；非 public 方法（Spring AOP 只拦截 public；@Transactional 用在 private 方法静默无效）；异常被 try-catch 吞掉（事务管理器感知不到异常）；受检异常默认不回滚（需 `rollbackFor = Exception.class`，RuntimeException/Error 才默认回滚）；传播行为理解错误（REQUIRES_NEW 要求独立代理边界）；数据库引擎不支持事务（MyISAM）。正确姿势：事务注解放在 Service 公共入口方法，异常上抛不吞。

传播与隔离两个属性的正确心智：`propagation` 最常用的是 REQUIRED（默认，加入或新建）与 REQUIRES_NEW（挂起当前事务新开一个——**独立提交独立回滚**，用于"子流程失败不影响主流程"；注意它要求通过代理调用才生效，自调用时 REQUIRES_NEW 直接退化为加入当前事务，这是隐性 bug 高发点）；`isolation` 默认走数据库默认级别（MySQL 为 REPEATABLE_READ），显式调级要给出业务理由（如 READ_COMMITTED 减锁、SERIALIZABLE 强一致），`readOnly = true` 是查询优化提示而非强制约束。

@Async 的坑同源：自调用失效、必须配置专属线程池（默认 SimpleAsyncTaskExecutor 会为每次调用建新线程，生产必须换 ThreadPoolTaskExecutor）、返回值用 Future/CompletableFuture、异常在调用线程感知不到（需 AsyncUncaughtExceptionHandler）。

@Scheduled 要点：cron 是 6 位（秒开头），Spring 7 支持更多扩展位；fixedDelay 是"上次结束 + N 秒"、fixedRate 是"上次开始 + N 秒"（慢任务会漂移）；默认单线程调度器，多个任务相互阻塞——配置 `spring.task.scheduling.pool.size`。@Cacheable/@CacheEvict/@CachePut 三件套的细节（key 的 SpEL 生成、条件缓存、双删一致性）属缓存体系范畴，此处只记一个坑：@CacheEvict 的 key 表达式必须与 @Cacheable 完全一致，否则清不掉。

---

## 4. Spring 7 新注解与迁移红线

**Resilience 注解入核**（Spring Framework 7 起，retry/concurrency 能力从 spring-retry 并入 spring-core/context）：

- `@Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000))`：声明式重试，**支持响应式返回类型**（经 Reactor 的 retryWhen 装饰，这是相对 spring-retry 的核心增强）；
- `@Recover`：重试耗尽后的兜底方法（方法签名需与 @Retryable 方法兼容）；
- `@ConcurrencyLimit`：限制方法并发调用数，`ThrottlePolicy.BLOCK`（排队）/`REJECT`（立即拒绝）——虚拟线程时代没有池上限，此注解成为防拖垮下游的标配；
- `@Timeout`：声明式超时切断；
- 激活方式：配置类上用 **`@EnableResilientMethods`** 替代旧 `@EnableRetry`；
- ⚠️ 行为变化：7.0.2+ 中接口上标注的 @Retryable **不再对实现类生效**（#36233），必须标注在实现类。

**@ApiVersion 版本化**：类级 `@ApiVersion("v1")` 让多个控制器版本共存，配合 `@GetMapping(path = "/{id}", version = "2.0+")` 的方法级版本语义——细节在 07 篇展开。

**两条迁移红线**：其一，`javax.annotation`（@PostConstruct/@PreDestroy/@Resource）必须迁至 `jakarta.annotation`，**javax 版编译通过但 Spring 7 静默忽略**——初始化逻辑无声失效，这是升级最阴险的坑；其二，空安全注解全面拥抱 **JSpecify**：`org.jspecify.annotations` 的 @NonNull/@Nullable/@NullMarked 取代 `org.springframework.lang.*` 旧注解，@NullMarked 标在 package-info.java 上实现"包级默认非空"，配合 NullAway/Error Prone 编译期强制。Spring 的 @NonNullApi/@NonNullFields 已弃用。

---

## 5. 高频坑与面试题

1. **@Transactional 失效三件套**：自调用、非 public、吞异常——面试按"代理机制 + 三场景"框架作答，生产按"公共入口 + 上抛"纪律编码；
2. **@Configuration proxyBeanMethods 与 @Bean 方法调用**：full 模式下方法内调用返回容器单例，lite 模式返回新实例——行为差异取决于一个开关，排查配置类诡异行为时先看它；
3. **@Value 冒号默认值与 URL**：`${url:}` 显式空默认是最佳实践；
4. **javax 静默忽略**：Spring 7 升级必须全局搜 javax.annotation 替换 jakarta.annotation；
5. **面试必答框架**：「Spring 如何解析注解？」——扫描阶段 `MergedAnnotations` 递归读元注解链 → 注册 BeanDefinition → 行为注解由 AOP 代理在运行期拦截执行；「@Autowired 与 @Resource 区别？」——Spring 容器注解（类型优先）vs Jakarta 标准注解（名称优先），装配歧义解决手段不同；
6. **「@Retryable 与 @ConcurrencyLimit 为什么进 Spring 7 核心？」**——虚拟线程时代重试/并发限制成为通用基础设施（无池上限后并发失控风险上升），从 spring-retry 收编进框架核心并补上响应式支持；激活注解从 @EnableRetry 更名为 @EnableResilientMethods；
7. **「@PostConstruct 在 Spring 7 为什么可能不执行？」**——import 停留在 javax.annotation 的版本被静默忽略，必须迁移 jakarta.annotation——编译通过 + 行为消失是升级事故的标准形态，靠测试与全局 import 扫描双重兜底。

---

**下一模块**：[07 Spring Web 注解剖析](./07-SpringWeb注解剖析.md) · **返回总览**：[00 总览](./00-Java常用注解知识体系总览.md)

**相关体系**：[Spring 框架核心](../../../02-后端核心技术%20微服务%20分布式%20云原生/06-Spring全家桶/Spring框架核心/00-Spring框架核心知识体系总览.md) · [泛型 反射 注解（Spring 源码中的三者协作）](../泛型%20反射%20注解/00-泛型反射注解知识体系总览.md)

---

【参考来源】
- [Spring Resilience 模块（@Retryable/@ConcurrencyLimit 示例）](https://github.com/ercansormaz/spring-resilience)
- [spring-projects/spring-framework Issue #36233: @Retryable 标注接口的行为变化](https://github.com/spring-projects/spring-framework/issues/36233)
- [JSpecify + Spring Boot 4: 空安全注解迁移（Java Code Geeks）](https://www.javacodegeeks.com/2026/06/jspecify-spring-boot-4-finally-fixing-javas-billion-dollar-mistake.html)
- [Spring Framework 6→7 迁移指南（javax→jakarta 静默忽略等）](https://dev.to/ankurm/spring-framework-6-to-7-migration-guide-breaking-changes-deprecated-apis-and-upgrade-checklist-3bf6)
- [Spring Annotations: The 2026 Essential Cheat Sheet（Marco Molteni）](https://marmo.dev/spring-annotation-meaning)
- [聚焦 Spring Framework 7 与 Spring Boot 4：Spring 团队专访（InfoQ 中文）](https://www.infoq.cn/article/z4msV9uzNy7CXYFC4K2J)
