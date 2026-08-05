# 附录D-分层综合习题库
> 🎯 覆盖全模块的分层习题集，按基础题→进阶应用题→精通拔高题三级划分，每道题配有参考答案要点

---

## 目录
1. [Spring Framework 核心习题](#1-spring-framework-核心习题)
2. [Spring MVC 习题](#2-spring-mvc-习题)
3. [Spring Boot 习题](#3-spring-boot-习题)
4. [Spring Data 习题](#4-spring-data-习题)
5. [Spring Security 习题](#5-spring-security-习题)
6. [微服务与Spring Cloud 习题](#6-微服务与spring-cloud-习题)
7. [中间件整合习题](#7-中间件整合习题)
8. [调优与运维习题](#8-调优与运维习题)
9. [综合项目设计题](#9-综合项目设计题)

---

## 1. Spring Framework 核心习题

### 基础题

**Q1.1** 列举Spring中注册Bean的5种注解及其使用场景。
> **参考答案**：`@Component`通用、`@Service`业务层、`@Repository`数据层（异常翻译）、`@Controller`控制器、`@Configuration`+`@Bean`第三方类

**Q1.2** `@Autowired`和`@Resource`有什么区别？
> **参考答案**：`@Autowired`是Spring注解，默认按类型注入，配合`@Qualifier`按名称；`@Resource`是JSR-250，默认按名称注入，找不到再按类型

**Q1.3** singleton和prototype Bean的区别是什么？
> **参考答案**：singleton在容器中只有一个实例（默认），prototype每次获取都创建新实例

**Q1.4** Spring AOP的五种通知类型分别是什么？
> **参考答案**：`@Before`前置、`@After`后置(finally)、`@AfterReturning`返回、`@AfterThrowing`异常、`@Around`环绕

### 进阶应用题

**Q1.5** 描述Spring Bean的完整生命周期（列出所有关键回调）。
> **参考答案**：实例化→属性填充→BeanNameAware→BeanFactoryAware→ApplicationContextAware→BeanPostProcessor#before→@PostConstruct→afterPropertiesSet→initMethod→BeanPostProcessor#after→就绪→@PreDestroy→destroy→destroyMethod

**Q1.6** `BeanFactory`和`ApplicationContext`的区别是什么？
> **参考答案**：BeanFactory延迟加载、功能简单；ApplicationContext预加载、支持国际化/事件/资源加载/AOP等

**Q1.7** 事务传播行为REQUIRED和REQUIRES_NEW的区别是什么？
> **参考答案**：REQUIRED加入外层事务（共享一个事务），REQUIRES_NEW新建独立事务（外层挂起，内层独立提交回滚）

### 精通拔高题

**Q1.8** 画出Spring三级缓存解决循环依赖的完整流程图。
> **参考答案**：A创建→实例化→三级缓存存ObjectFactory→属性填充需B→B创建→实例化→三级缓存存ObjectFactory→属性填充需A→从三级缓存获取A的早期引用→升级到二级→B完成→一级缓存→A拿到完整B→A完成→一级缓存

**Q1.9** `@Transactional`在什么情况下会失效？列举至少5种场景并给出原因。
> **参考答案**：方法非public（AOP只代理public）、同类内部调用（this绕过代理）、异常被捕获（try-catch吞异常）、rollbackFor设置错误（受检异常不回滚）、数据库引擎不支持事务

**Q1.10** `@Configuration`注解的`proxyBeanMethods`属性有何作用？
> **参考答案**：true（默认）时Configuration类被CGLIB代理，@Bean方法调用会通过容器获取单例保证单例；false时不代理，@Bean方法每次调用创建新实例，适用于Lite模式

---

## 2. Spring MVC 习题

### 基础题

**Q2.1** `@PathVariable`和`@RequestParam`的区别是什么？
> **参考答案**：@PathVariable从URL路径中取值（如`/users/{id}`），@RequestParam从查询参数中取值（如`?page=1`）

**Q2.2** RESTful API中GET、POST、PUT、DELETE分别对应什么操作？
> **参考答案**：GET查询、POST创建、PUT全量更新、DELETE删除

**Q2.3** 如何用Spring MVC返回JSON数据？
> **参考答案**：使用`@RestController`或`@Controller`+`@ResponseBody`

### 进阶应用题

**Q2.4** 过滤器和拦截器的执行顺序是怎样的？各适用什么场景？
> **参考答案**：Filter先于Interceptor执行；Filter适用于编码设置、XSS过滤、通用安全过滤；Interceptor适用于登录校验、权限控制（可注入Spring Bean）

**Q2.5** 如何设计一个全局异常处理方案？
> **参考答案**：`@RestControllerAdvice` + `@ExceptionHandler`，分别处理业务异常、参数校验异常、系统异常

### 精通拔高题

**Q2.6** 画出`DispatcherServlet.doDispatch()`的完整执行流程。
> **参考答案**：getHandler→getHandlerAdapter→applyPreHandle→ha.handle→applyPostHandle→processDispatchResult（视图解析/JSON转换）→异常走processDispatchException→finally triggerAfterCompletion

**Q2.7** 如何自定义一个`HandlerMethodArgumentResolver`？
> **参考答案**：实现`supportsParameter()`和`resolveArgument()`，注册到`WebMvcConfigurer.addArgumentResolvers()`

---

## 3. Spring Boot 习题

### 基础题

**Q3.1** `@SpringBootApplication`由哪三个注解组成？
> **参考答案**：`@SpringBootConfiguration`（=@Configuration）+ `@EnableAutoConfiguration` + `@ComponentScan`

**Q3.2** 如何切换SpringBoot的内置Web容器？
> **参考答案**：排除Tomcat依赖，引入Jetty或Undertow的starter

### 进阶应用题

**Q3.3** 描述Spring Boot自动装配的完整流程。
> **参考答案**：`@EnableAutoConfiguration`→`AutoConfigurationImportSelector`→读取`spring.factories`/`AutoConfiguration.imports`→加载自动配置类列表→通过`@Conditional`注解过滤→加载满足条件的配置类

**Q3.4** Spring Boot配置文件加载的优先级顺序是什么？
> **参考答案**（从高到低）：命令行参数 > 环境变量 > application-{profile}.yml > application.yml > @PropertySource > 默认配置

### 精通拔高题

**Q3.5** 如何自定义一个Spring Boot Starter？
> **参考答案**：新建Maven项目→创建AutoConfiguration类→创建Properties类（@ConfigurationProperties）→在META-INF/spring下创建AutoConfiguration.imports或spring.factories→使用@ConditionalOnXxx进行条件控制

**Q3.6** `AutoConfigurationImportSelector.selectImports()`是如何过滤自动配置类的？
> **参考答案**：通过`getAutoConfigurationEntry()`→`filter()`方法，遍历每个配置类上的`@Conditional`注解（如`@ConditionalOnClass`、`@ConditionalOnMissingBean`），调用对应的Condition#matches()判断是否满足条件

---

## 4. Spring Data 习题

### 基础题

**Q4.1** JPA中`@Entity`和`@Table`的区别是什么？
> **参考答案**：`@Entity`标识实体类，`@Table`指定对应的数据库表名（可选，默认类名转下划线）

**Q4.2** `@Cacheable`、`@CacheEvict`、`@CachePut`的区别是什么？
> **参考答案**：`@Cacheable`缓存方法返回值（缓存中有则直接返回），`@CacheEvict`清除缓存，`@CachePut`执行方法并更新缓存

**Q4.3** JPA和MyBatis的核心区别是什么？
> **参考答案**：JPA自动生成SQL（ORM完整映射），MyBatis手写SQL（灵活控制）；JPA适合标准化CRUD，MyBatis适合复杂SQL场景

### 进阶应用题

**Q4.4** 什么是JPA的N+1查询问题？如何解决？
> **参考答案**：查1条主实体时，对每个关联子实体再发1条SQL（共1+N条）。解决方案：`@EntityGraph`、`JOIN FETCH` JPQL、设置`@BatchSize`

**Q4.5** Spring Data Redis的序列化方案有哪些？推荐哪种？
> **参考答案**：JdkSerializationRedisSerializer（默认，二进制不可读）、StringRedisSerializer、Jackson2JsonRedisSerializer、GenericJackson2JsonRedisSerializer。推荐StringRedisSerializer用于Key，Jackson2JsonRedisSerializer用于Value

### 精通拔高题

**Q4.6** 缓存穿透、缓存击穿、缓存雪崩分别是什么？如何解决？
> **参考答案**：穿透→查不存在的数据（布隆过滤器+缓存空值）；击穿→热点key过期（互斥锁+逻辑过期）；雪崩→大量key同时过期（随机过期时间+多级缓存+限流）

**Q4.7** JPA的一级缓存和二级缓存有什么区别？
> **参考答案**：一级缓存是EntityManager级别的（同一个事务内命中），默认开启；二级缓存是EntityManagerFactory级别的（跨事务共享），需要手动开启并配置缓存提供商（如EhCache）

---

## 5. Spring Security 习题

### 基础题

**Q5.1** Authentication（认证）和Authorization（授权）的区别？
> **参考答案**：认证=你是谁（验证身份），授权=你能做什么（检查权限）

**Q5.2** BCrypt加密结果每次都不一样，如何验证密码？
> **参考答案**：BCrypt的hash中包含随机salt，验证时使用`BCryptPasswordEncoder.matches(rawPassword, encodedPassword)`，内部从encodedPassword中提取salt后重新计算对比

**Q5.3** JWT由哪三部分组成？
> **参考答案**：Header（算法类型）、Payload（声明数据）、Signature（签名=Base64(Header).Base64(Payload) + secret用算法加密）

### 进阶应用题

**Q5.4** 如何设计Access Token + Refresh Token双令牌机制？
> **参考答案**：登录返回AT（短期15min）+ RT（长期7天）；AT过期后用RT获取新AT；RT过期则重新登录；RT泄露可通过黑名单吊销

**Q5.5** 如何实现RBAC权限模型？
> **参考答案**：用户表(user)→关联表(user_role)→角色表(role)→关联表(role_permission)→权限表(permission)；`@PreAuthorize("hasRole('ADMIN')")`或`hasAuthority('user:delete')`

### 精通拔高题

**Q5.6** Session认证和JWT无状态认证的架构差异是什么？各有什么优缺点？
> **参考答案**：Session状态存储在服务端（需共享存储），JWT状态编码在Token中（无状态）。Session适合传统Web、便于吊销；JWT适合微服务/移动端、无状态扩展、但吊销困难

**Q5.7** Spring Security的过滤器链是如何工作的？
> **参考答案**：DelegatingFilterProxy→FilterChainProxy→SecurityFilterChain列表→匹配请求的第一个链执行→链内包含多个Filter（如UsernamePasswordAuthenticationFilter、ExceptionTranslationFilter、FilterSecurityInterceptor）按顺序执行

---

## 6. 微服务与Spring Cloud 习题

### 基础题

**Q6.1** 微服务和单体架构的核心区别是什么？
> **参考答案**：单体=单一代码库/单一部署/共享数据库；微服务=按业务拆分为独立服务/每个服务独立部署/每个服务有独立数据库/通过HTTP或MQ通信

**Q6.2** Nacos是什么？它解决了什么问题？
> **参考答案**：Nacos是阿里巴巴开源的服务注册发现+配置中心，同时解决微服务中的"谁在哪"（注册中心）和"配置怎么管"（配置中心）两个核心问题

**Q6.3** OpenFeign的作用是什么？
> **参考答案**：声明式HTTP客户端，通过接口+注解的方式调用远程服务，底层集成Ribbon/LoadBalancer实现负载均衡

**Q6.4** Sentinel的核心功能有哪些？
> **参考答案**：流量控制（限流）、熔断降级、系统负载保护、热点参数限流

**Q6.5** Spring Cloud Gateway的路由组成三要素是什么？
> **参考答案**：Route ID（路由标识）、Predicate（断言，匹配规则）、Filter（过滤器，请求处理）

### 进阶应用题

**Q6.6** 微服务之间如何进行通信？同步和异步方式各有什么适用场景？
> **参考答案**：同步（OpenFeign/REST→查询、强一致性操作）、异步（MQ→事件通知、解耦、削峰填谷）

**Q6.7** 如何解决Feign调用时请求头（如Authorization）丢失的问题？
> **参考答案**：使用`RequestInterceptor`，从`RequestContextHolder`获取当前请求的Header，设置到Feign的`RequestTemplate`中

**Q6.8** Nacos和Eureka在CAP模型上的选择有何不同？
> **参考答案**：Nacos支持AP和CP模式切换（默认AP），Eureka是纯AP模型

**Q6.9** Gateway如何实现统一鉴权？
> **参考答案**：实现`GlobalFilter`，在filter中检查请求Header中的Token，解析并验证，将用户信息传递到下游服务

**Q6.10** Seata的AT模式和TCC模式有什么区别？分别适用什么场景？
> **参考答案**：AT模式自动回滚（通过undo_log），无业务侵入，适合大部分场景；TCC需手动实现Try/Confirm/Cancel，有侵入但性能更高，适合资金类核心业务

### 精通拔高题

**Q6.11** 什么是"服务雪崩"？如何通过分层防护防止雪崩？
> **参考答案**：服务雪崩=一个服务故障导致依赖它的服务连锁故障。分层防护：Nginx层限流→Gateway层限流+熔断→Sentinel服务层熔断降级→线程池隔离→降级兜底（默认值/缓存/提示）

**Q6.12** 分布式事务Seata中，AT模式的全局锁有什么作用？如果出现锁冲突会怎样？
> **参考答案**：全局锁防止其他事务同时修改同一行数据（写隔离）。如果锁冲突，后续事务会等待（可配置超时），超时后回滚并重试

**Q6.13** Sentinel的滑动窗口限流算法是如何实现的？
> **参考答案**：基于 LeapArray 数据结构，将时间窗口划分为多个小窗口（格子），每个格子独立计数，每次统计滑动窗口内的总请求数来判断是否限流

---

## 7. 中间件整合习题

### 基础题

**Q7.1** RabbitMQ有哪些交换机类型？
> **参考答案**：Direct（精确匹配）、Topic（通配符匹配）、Fanout（广播）、Headers（头匹配）

**Q7.2** 缓存穿透、击穿、雪崩各是什么？解决方案是什么？
> **参考答案**：见Q4.6

**Q7.3** XXL-JOB的核心架构是什么？
> **参考答案**：调度中心（Admin）+ 执行器（Executor），Admin负责任务调度、路由策略、故障转移，Executor执行具体任务逻辑

### 进阶应用题

**Q7.4** 如何保证RabbitMQ的消息可靠性？
> **参考答案**：生产者Confirm模式 + 消息持久化（持久化交换机/队列/消息） + 消费者手动ACK + 死信队列重试 + 消息落库补偿

**Q7.5** 如何实现接口的幂等性？
> **参考答案**：数据库唯一约束（最可靠）+ Redis Token机制 + 状态机 + 乐观锁（version字段）

**Q7.6** Redisson分布式锁的Watch Dog机制是什么？
> **参考答案**：Watch Dog（看门狗）是Redisson的自动续期机制。如果未指定锁的leaseTime，Redisson会启动一个定时任务，每10秒（internalLockLeaseTime/3=30/3）自动延长锁的过期时间，避免业务执行时间超过锁的过期时间导致锁被释放

### 精通拔高题

**Q7.7** 分库分表后如何保证全局唯一ID？
> **参考答案**：Snowflake算法（Twitter，64位：1+41时间戳+10机器+12序列号）、号段模式（Leaf-Segment，美团）、数据库自增（步长=N，各库ID=N*k+m）

**Q7.8** 分布式定时任务如何避免多实例重复执行？
> **参考答案**：XXL-JOB自动处理（通过路由策略+数据库锁）；Quartz使用数据库锁；或使用分布式锁（Redisson/ZooKeeper）在任务执行前获取锁

---

## 8. 调优与运维习题

### 基础题

**Q8.1** Tomcat线程池的核心参数有哪些？
> **参考答案**：maxThreads（最大工作线程）、minSpareThreads（最小空闲线程）、acceptCount（等待队列长度）、maxConnections（最大连接数）

**Q8.2** Dockerfile中常用的指令有哪些？
> **参考答案**：FROM（基础镜像）、COPY/ADD（复制文件）、RUN（执行命令）、EXPOSE（暴露端口）、CMD/ENTRYPOINT（容器启动命令）

### 进阶应用题

**Q8.3** 接口响应慢的排查步骤是什么？
> **参考答案**：1.网络检查(ping/telnet) → 2.Arthas trace追踪耗时方法 → 3.慢SQL分析(explain) → 4.检查缓存命中率 → 5.外部调用超时排查 → 6.GC频率分析(jstat) → 7.线程状态分析(jstack)

**Q8.4** 如何进行JVM调优？
> **参考答案**：根据业务场景选择GC（G1GC适合大堆/低延迟），合理设置堆大小（-Xms=-Xmx），设置MetaspaceSize，开启GC日志，使用-XX:+HeapDumpOnOutOfMemoryError

### 精通拔高题

**Q8.5** 如何设计一个系统的高可用（HA）方案？
> **参考答案**：多节点部署+负载均衡、同城双活/异地多活、限流降级熔断兜底、数据库主从+读写分离、缓存高可用（Redis哨兵/集群）、消息队列高可用、无状态服务设计、自动化故障检测和切换

**Q8.6** 什么是蓝绿部署和金丝雀发布？
> **参考答案**：蓝绿部署=维护两套完全相同的环境（蓝+绿），发布时切换流量；金丝雀发布=先让一小部分用户使用新版本，验证通过后逐步扩大范围（灰度发布）。Nacos可实现基于权重的灰度路由

---

## 9. 综合项目设计题

### 题目1：电商微服务系统设计

**要求**：设计一个电商微服务系统架构图，包含以下功能：
- 用户注册/登录（JWT）
- 商品浏览/搜索
- 购物车 + 下单（分布式事务）
- 支付（第三方支付模拟）
- 订单状态流转

**请包含**：
1. 微服务拆分方案（列出每个服务及职责）
2. 技术栈选择（注册中心、配置中心、网关、服务调用、熔断限流、消息队列等）
3. 数据库设计要点（每服务独立DB，核心表结构）
4. 分布式事务处理方案
5. 高可用设计方案

### 题目2：秒杀系统设计

**要求**：设计一个高并发秒杀系统

**请包含**：
1. 前端限流方案（按钮防重复、验证码）
2. 网关层限流方案
3. 服务层限流+排队方案（Redis + MQ）
4. 库存扣减方案（Redis预扣库存 + DB最终扣减）
5. 订单异步生成方案
6. 集群部署方案

### 题目3：微服务故障演练

**场景**：你的电商系统部署了用户服务、商品服务、订单服务、支付服务4个微服务，使用Nacos注册中心、Gateway网关、OpenFeign调用、Sentinel保护。

**问题**：
1. 如果商品服务挂了，其他服务会怎样？如何保护？
2. 如果Nacos挂了，系统还能正常工作吗？
3. 订单服务和支付服务之间的分布式事务如何处理？
4. 如何设计一个故障演练方案来验证系统容错能力？

---

> 🎯 **使用建议**：
> - **基础题**：每天做5-10道，用于知识巩固
> - **进阶题**：每周做3-5道，提升工程应用能力
> - **精通题**：面试前集中刷，应对大厂面试
> - **综合设计题**：每学完一个大模块做1道，融会贯通
