# Spring 全家桶精通 —— 企业开发标配

> **原则：不只会用注解，还要理解底层原理。**
> IoC 容器 + AOP 切面 + Spring Boot 自动配置 + Spring Cloud 微服务 = 面试必问四大件。

---

## 一、Spring IoC 容器（★★★★★）

### 1.1 Bean 完整生命周期

```
┌─────────────────────────────────────────────────────────────┐
│ Bean 生命周期 13 个步骤（浓缩版）                              │
├─────────────────────────────────────────────────────────────┤
│ 1. 实例化：反射调用构造方法创建对象实例                          │
│ 2. 属性赋值：populateBean() 填充属性（含依赖注入）              │
│ 3. Aware 回调：BeanNameAware → BeanFactoryAware →            │
│               ApplicationContextAware                        │
│ 4. BeanPostProcessor#postProcessBeforeInitialization()       │
│ 5. @PostConstruct 方法                                       │
│ 6. InitializingBean#afterPropertiesSet()                     │
│ 7. init-method（XML 配置的自定义初始化方法）                    │
│ 8. BeanPostProcessor#postProcessAfterInitialization()        │
│       ↓↓↓ Bean 就绪，可使用 ↓↓↓                               │
│ 9. @PreDestroy 方法                                          │
│10. DisposableBean#destroy()                                  │
│11. destroy-method（XML 配置的自定义销毁方法）                    │
└─────────────────────────────────────────────────────────────┘

关键：BeanPostProcessor 是 Spring 的扩展核心，
     AOP、事务、@Autowired 注入都通过它实现。
```

**助记口诀**：实例化 → 属性赋值 → 初始化前处理 → 初始化 → 初始化后处理 → 就绪 → 销毁。

### 1.2 BeanFactory vs ApplicationContext

| 对比维度 | BeanFactory | ApplicationContext |
|---------|------------|-------------------|
| 定位 | IoC 底层容器接口 | 应用上下文，继承 BeanFactory |
| Bean 加载 | 延迟加载（getBean() 时才实例化） | 预加载（容器启动时实例化单例 Bean） |
| 国际化 | ❌ | ✅ MessageSource |
| 事件发布 | ❌ | ✅ ApplicationEvent |
| 资源加载 | ❌ | ✅ ResourceLoader |
| 使用场景 | 内存敏感的嵌入式设备 | 99% 的企业应用 |

### 1.3 循环依赖 —— 三级缓存方案

```
Spring 只解决单例 Bean 的 Setter 注入循环依赖，
构造器注入的循环依赖无法解决（会抛 BeanCurrentlyInCreationException）。

三级缓存：
┌─────────────┬──────────────────────────────────┐
│ 缓存名       │ 存储内容                          │
├─────────────┼──────────────────────────────────┤
│ singletonObjects    │ 完全初始化好的 Bean（成品）      │
│ earlySingletonObjects│ 提前曝光的 Bean 引用（半成品）  │
│ singletonFactories   │ ObjectFactory（可生成代理对象） │
└─────────────┴──────────────────────────────────┘

解决流程（A ↔ B 循环依赖）：
1. A 实例化 → 存入三级缓存（singletonFactories）
2. A 属性赋值时发现依赖 B → 去获取 B
3. B 实例化 → 存入三级缓存
4. B 属性赋值时发现依赖 A → 从三级缓存获取 A 的早期引用
5. A 的 ObjectFactory.getObject() 返回 A 的引用（可能生成代理）
6. A 的早期引用存入二级缓存（earlySingletonObjects）
7. B 完成初始化 → 存入一级缓存
8. A 完成初始化 → 存入一级缓存
```

**面试要点**：三级缓存的核心价值是处理 AOP 代理——当循环依赖涉及代理对象时，ObjectFactory 可以提前生成代理。

---

## 二、Spring AOP（★★★★★）

### 2.1 核心概念

| 概念 | 说明 | 示例 |
|------|------|------|
| JoinPoint | 可增强的点（方法执行/异常抛出） | UserService 所有方法 |
| Pointcut | 实际增强的点（切点表达式） | `execution(* com.example..*.*(..))` |
| Advice | 增强逻辑 | @Before / @After / @Around |
| Aspect | 切面 = 切点 + 增强 | @Aspect + @Component |
| Weaving | 织入：将切面应用到目标对象 | 编译时/类加载时/运行时 |

### 2.2 Advice 类型及执行顺序

```
正常执行：
@Around 前半段 → @Before → 目标方法 → @Around 后半段 → @After → @AfterReturning

异常执行：
@Around 前半段 → @Before → 目标方法异常 → @After → @AfterThrowing
（@Around 后半段不执行，@AfterReturning 不执行）
```

### 2.3 Spring AOP 底层实现

```
接口 + JDK 动态代理：目标类实现了接口 → 使用 Proxy + InvocationHandler
无接口 + CGLIB：目标类未实现接口 → 使用 CGLIB 生成子类

Spring Boot 2.x 默认：有接口用 JDK，无接口用 CGLIB
Spring Boot 3.x 默认：全部使用 CGLIB
可通过 spring.aop.proxy-target-class=true/false 修改
```

### 2.4 @Transactional 失效场景（8 种）

| 场景 | 原因 | 解决办法 |
|------|------|---------|
| 1. 非 public 方法 | AOP 只能代理 public 方法 | 改为 public |
| 2. 同类内部调用 | 绕过了代理对象，直接调用 this.method() | 注入自身代理 / 拆分到不同类 |
| 3. 异常被 try-catch 吞掉 | 代理感知不到异常 | 在 catch 块中 throw 或手动回滚 |
| 4. rollbackFor 不匹配 | 默认只回滚 RuntimeException | 设置 `rollbackFor = Exception.class` |
| 5. 数据库引擎不支持 | MyISAM 不支持事务 | 改用 InnoDB |
| 6. 多线程环境 | 事务绑定在线程上 | 分布式事务方案 |
| 7. 方法被 final 修饰 | CGLIB 不能代理 final 方法 | 去掉 final |
| 8. 传播行为设置不当 | NEVER/NOT_SUPPORTED | 检查 propagation 设置 |

---

## 三、Spring MVC（★★★★☆）

### 3.1 DispatcherServlet 核心流程

```
请求 → DispatcherServlet
  1. HandlerMapping：根据 URL 找到 Handler（Controller 方法）
  2. HandlerAdapter：调用 Handler
  3. Handler 执行（参数解析 → 业务逻辑 → 返回值）
  4. ViewResolver：解析视图（前后端分离项目中此步常省略）
  5. 响应返回客户端

拦截器 vs 过滤器：
┌──────────┬──────────────┬───────────────────────┐
│          │ 过滤器(Filter) │ 拦截器(Interceptor)     │
├──────────┼──────────────┼───────────────────────┤
│ 容器     │ Servlet 容器   │ Spring 容器             │
│ 范围     │ 所有请求       │ 只拦截 Controller 请求    │
│ 粒度     │ 只能在 doFilter│ pre/post/after 三阶段    │
│ 依赖注入 │ ❌ 不能注入 Bean│ ✅ 可以注入 Spring Bean   │
└──────────┴──────────────┴───────────────────────┘
```

---

## 四、Spring Boot（★★★★★）

### 4.1 自动配置原理

```
核心注解：@SpringBootApplication
= @SpringBootConfiguration + @EnableAutoConfiguration + @ComponentScan

自动配置流程：
1. @EnableAutoConfiguration → @Import(AutoConfigurationImportSelector.class)
2. AutoConfigurationImportSelector 读取 META-INF/spring/org.springframework.boot
   .autoconfigure.AutoConfiguration.imports 文件（Spring Boot 2.x 是 spring.factories）
3. 加载 100+ 个 xxxAutoConfiguration 类
4. 每个 AutoConfiguration 类通过 @ConditionalOnXxx 条件注解判断是否生效
   - @ConditionalOnClass：classpath 有某类时才生效
   - @ConditionalOnMissingBean：用户未自定义 Bean 时才生效
   - @ConditionalOnProperty：配置文件中某属性满足条件才生效

关键：用户自定义 > 自动配置（@ConditionalOnMissingBean 保证）
```

### 4.2 Starter 机制

```
starter 的本质：Maven 依赖的聚合包，帮你管理所有传递依赖。

以 spring-boot-starter-web 为例：
├── spring-boot-starter（核心 starter）
├── spring-boot-starter-tomcat（内嵌 Tomcat）
├── spring-webmvc（Spring MVC）
└── spring-boot-starter-json（Jackson）

自定义 Starter 命名规范：
- 官方：spring-boot-starter-xxx
- 第三方：xxx-spring-boot-starter
```

### 4.3 SpringApplication.run() 启动流程

```
SpringApplication.run() 7 个关键步骤：

1. 创建 SpringApplication 实例
   - 推断应用类型（SERVLET/REACTIVE/NONE）

2. 获取 SpringApplicationRunListeners
   - EventPublishingRunListener 发布启动事件

3. 准备 Environment
   - 加载 application.properties/yml
   - 加载命令行参数、系统属性

4. 创建 ApplicationContext
   - 根据应用类型创建（AnnotationConfigServletWebServerApplicationContext）

5. 准备 ApplicationContext
   - 执行 BeanDefinition 的注册

6. 刷新 ApplicationContext（核心！）
   - 调用 AbstractApplicationContext.refresh() → 执行 12 个步骤

7. 发布 started/ready 事件
   - ApplicationStartedEvent → ApplicationReadyEvent
```

---

## 五、Spring Cloud 微服务（★★★★★）

### 5.1 核心组件全景图

```
┌──────────────────────────────────────────────────────┐
│                   Spring Cloud 微服务架构              │
├──────────────────────────────────────────────────────┤
│                                                       │
│  客户端 → Gateway（网关）→ 业务服务                      │
│              │  ↑                                      │
│              ↓  │                                      │
│          Nacos（注册中心 + 配置中心）                    │
│              │                                         │
│  业务服务 ←→ OpenFeign（远程调用）                      │
│              │                                         │
│   Sentinel（限流/熔断/降级）                            │
│              │                                         │
│   Seata（分布式事务）                                   │
│              │                                         │
│   RocketMQ / RabbitMQ（消息队列）                       │
│                                                       │
└──────────────────────────────────────────────────────┘
```

### 5.2 组件对比与职责

| 组件 | 职责 | 阿里替代 | Netflix 替代 |
|------|------|---------|-------------|
| Nacos | 注册中心 + 配置中心 | 一站式 | Eureka + Config |
| OpenFeign | 声明式 HTTP 客户端 | — | Feign |
| Gateway | API 网关 | — | Zuul |
| Sentinel | 流量控制 + 熔断降级 | — | Hystrix |
| Seata | 分布式事务 | — | — |

### 5.3 服务注册与发现流程

```
1. 服务提供者启动 → 向 Nacos 注册（发送 IP + 端口 + 服务名）
2. Nacos 维护服务注册表 + 定时健康检查（心跳机制）
3. 服务消费者启动 → 从 Nacos 拉取服务注册表
4. 服务消费者通过服务名调用 → OpenFeign → Ribbon 负载均衡 → 选择实例
5. 服务提供者下线 → Nacos 剔除该实例 → 通知消费者更新注册表

CAP 选择：
- Nacos 支持 AP 和 CP 模式切换（默认 AP）
- Eureka 是 AP 模式（优先可用性）
- Zookeeper 是 CP 模式（优先一致性）
```

---

## 六、MyBatis / MyBatis Plus（★★★★★）

### 6.1 核心执行流程

```
SqlSession → Executor → StatementHandler → ParameterHandler → ResultSetHandler

一级缓存 vs 二级缓存：
┌──────────┬────────────────┬──────────────────────┐
│          │ 一级缓存        │ 二级缓存               │
├──────────┼────────────────┼──────────────────────┤
│ 作用域   │ SqlSession 级别 │ Mapper 级别（跨 Session）│
│ 默认开启 │ ✅ 是          │ ❌ 否                  │
│ 数据一致 │ 安全            │ 可能读到脏数据          │
│ 注意事项 │ —              │ 同一 Mapper 跨 Namespace│
│          │                │ 时需注意缓存失效         │
└──────────┴────────────────┴──────────────────────┘
```

### 6.2 MyBatis 与 Spring 整合原理

```
Mapper 接口如何被 Spring 管理？
1. @MapperScan 注解导入 MapperScannerRegistrar
2. MapperScannerRegistrar 注册 MapperScannerConfigurer
3. MapperScannerConfigurer 扫描指定包下的所有接口
4. 每个接口通过 MapperFactoryBean 创建代理对象
5. 代理对象内部调用 SqlSession.getMapper() 获取 MyBatis 代理
6. 最终注入到 Service 层的 Mapper 引用是一个 JDK 动态代理对象

关键：你写的 Mapper 接口根本没有实现类，全靠动态代理。
```

### 6.3 MyBatis Plus 核心功能

```
- BaseMapper<T>：继承后自动拥有 CRUD 方法
- 条件构造器：LambdaQueryWrapper<T>（类型安全，防字段名写错）
- 分页插件：PaginationInterceptor 自动拦截 SQL 添加 LIMIT
- 代码生成器：根据表结构自动生成 Entity/Mapper/Service/Controller
- 自动填充：@TableField(fill = FieldFill.INSERT) 创建时间自动填充
- 逻辑删除：@TableLogic 标记，delete → update is_deleted = 1
```

---

## 面试自查清单

```
□ Bean 生命周期 13 个步骤能画图讲清楚
□ 循环依赖的三级缓存方案能写出来
□ @Transactional 失效的 8 种场景能列举至少 5 种
□ DispatcherServlet 处理请求的全链路能口述
□ Spring Boot 自动配置 @ConditionalOnXxx 机制理解
□ Gateway vs Nginx 的区别
□ Nacos CAP 模式切换的应用场景
□ MyBatis Mapper 接口为什么可以不用实现类
```
