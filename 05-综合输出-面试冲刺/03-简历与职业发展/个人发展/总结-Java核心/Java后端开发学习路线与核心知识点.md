# Java 后端开发学习路线与核心知识点

## 概述

本文按 **6 大阶段 + 36 个模块** 系统梳理 Java 后端开发从基础到高级的完整学习路线与核心知识点，涵盖语言基础、框架、中间件、工程化、架构与面试，适用于体系化学习与面试备考。

---

## 学习路线全景图

```
阶段一：Java 基础         阶段二：框架核心          阶段三：中间件与分布式
 Java SE / OOP              Spring 全家桶               Redis / MQ / ES
 集合 / IO / 多线程          Spring Boot / MVC           Docker / K8s
 JVM / 反射 / 注解           MyBatis / JPA              Spring Cloud

阶段四：数据库              阶段五：工程化              阶段六：架构与面试
 MySQL / Redis               Maven / Git / Linux         设计模式 / 微服务
 JDBC / 连接池               CI/CD / 日志                性能优化 / 高频考点
 RESTful API                单元测试 / 工具链            安全 / 项目实战
```

---

## 一、Java 基础核心（阶段一）

### 1.1 基础语法与 OOP

| 模块 | 核心知识点 |
|------|-----------|
| **基本语法** | 8 种基本类型（byte/short/int/long/float/double/char/boolean）、变量与常量（final）、运算符（算术/关系/逻辑/位/赋值/三元）、流程控制（if-else/switch/for/while/do-while/break/continue）、数组（一维/二维、Arrays 工具类） |
| **OOP** | 类与对象（构造方法/this）、封装（private/getter/setter/包与权限）、继承（extends/super/方法重写@Overide）、多态（向上转型/向下转型/重载vs重写）、抽象类（abstract）vs 接口（interface/default/static）、内部类（成员/局部/匿名/静态） |
| **异常处理** | Throwable 体系（Error vs Exception）、Checked vs Unchecked（RuntimeException）、try-catch-finally、throw vs throws、try-with-resources（JDK 7+） |
| **泛型** | 泛型类/泛型方法/泛型接口、通配符（? extends T / ? super T / ?）、类型擦除 |

### 1.2 集合框架

| 接口 | 核心实现类 | 关键特性 |
|------|-----------|---------|
| **List** | ArrayList（动态数组）/ LinkedList（双向链表）/ Vector（线程安全） | 有序可重复 |
| **Set** | HashSet（哈希表）/ LinkedHashSet（有序）/ TreeSet（红黑树排序） | 无序不可重复 |
| **Map** | HashMap（数组+链表+红黑树）/ LinkedHashMap（有序）/ TreeMap（排序）/ ConcurrentHashMap（CAS+synchronized） | 键值对 |
| **工具** | Collections（sort/binarySearch/synchronizedXXX）、Iterator/ListIterator | — |

### 1.3 IO/NIO 与多线程

| 模块 | 核心知识点 |
|------|-----------|
| **IO** | 字节流（InputStream/OutputStream 体系）、字符流（Reader/Writer 体系）、缓冲流、对象序列化（Serializable） |
| **NIO** | Channel/Buffer/Selector 三大核心组件、Path/Files 工具类 |
| **多线程** | 创建方式（Thread/Runnable/Callable+FutureTask/线程池）、生命周期（NEW→RUNNABLE→BLOCKED→WAITING→TIMED_WAITING→TERMINATED）、同步（synchronized/Lock/volatile）、线程通信（wait/notify/Condition）、并发工具（CountDownLatch/CyclicBarrier/Semaphore/Exchanger） |
| **网络编程** | TCP（Socket/ServerSocket 三次握手四次挥手）、UDP（DatagramSocket/DatagramPacket）、NIO 网络（ServerSocketChannel/Selector） |

### 1.4 反射、注解与 JVM

| 模块 | 核心知识点 |
|------|-----------|
| **反射** | Class 获取（forName/getClass/ClassName.class）、Constructor/Method/Field、invoke/get/set |
| **注解** | 内置注解（@Override/@Deprecated）、元注解（@Target/@Retention）、自定义注解、APT |
| **JVM** | 内存结构（程序计数器/虚拟机栈/堆/元空间 Metaspace）、GC 算法（标记-清除/复制/标记-整理/分代收集）、收集器（Serial/Parallel/CMS/G1/ZGC）、类加载（双亲委派模型）、JIT 编译 |

---

## 二、框架核心（阶段二）

### 2.1 Spring 全家桶

| 框架 | 核心知识点 |
|------|-----------|
| **Spring IoC/DI** | ApplicationContext、Bean 生命周期（实例化→赋值→初始化→销毁）、作用域（singleton/prototype）、装配方式（XML/@Component/@Configuration）、`@Autowired` vs `@Resource` |
| **Spring AOP** | 切面/切入点/通知（@Before/@After/@Around）、JDK 动态代理 vs CGLIB、应用场景（日志/事务/权限） |
| **Spring MVC** | DispatcherServlet → HandlerMapping → HandlerAdapter → ViewResolver 全链路；`@Controller`/`@RequestMapping`/`@PathVariable`/`@RequestBody`/`@RestController` |
| **Spring Boot** | 自动配置（`@EnableAutoConfiguration`/`@Conditional` 系列）、Starter、Actuator、application.yml 多环境配置、内嵌容器 |
| **Spring Cloud** | 注册中心（Nacos/Eureka/Consul）、远程调用（OpenFeign）、网关（Gateway）、熔断（Sentinel/Resilience4j）、配置中心（Nacos Config/Apollo）、链路追踪（Sleuth+Zipkin/SkyWalking） |

### 2.2 持久层框架

| 框架 | 核心知识点 |
|------|-----------|
| **MyBatis** | SqlSessionFactory → SqlSession → Mapper；`#{}` vs `${}`（防 SQL 注入）；动态 SQL（if/choose/foreach）；一级缓存（SqlSession）/二级缓存（Mapper）；插件（Interceptor） |
| **JPA/Hibernate** | `@Entity`/`@Table`/`@Id`、JPQL、一级缓存/二级缓存、脏检查、N+1 问题 |

---

## 三、数据库技术（阶段三）

### 3.1 MySQL 核心

| 领域 | 核心知识点 |
|------|-----------|
| **SQL** | DDL/DML/DQL（JOIN/GROUP BY/HAVING/ORDER BY/LIMIT/子查询）/DCL |
| **存储引擎** | InnoDB（事务/行锁/MVCC/外键）vs MyISAM（表锁/全文索引） |
| **索引** | B+树索引（聚簇 vs 非聚簇）、Hash 索引、最左前缀原则、索引失效场景、EXPLAIN 执行计划 |
| **事务** | ACID、四种隔离级别（READ UNCOMMITTED→SERIALIZABLE）、脏读/不可重复读/幻读 |
| **锁** | 共享锁(S)/排他锁(X)、意向锁、间隙锁/临键锁、乐观锁（版本号）vs 悲观锁 |
| **MVCC** | Read View + Undo Log 版本链 |

### 3.2 Redis

| 领域 | 核心知识点 |
|------|-----------|
| **数据结构** | String/Hash/List/Set/ZSet 及其底层实现（SDS/ziplist/skiplist） |
| **持久化** | RDB（快照）vs AOF（日志追加），混合持久化 |
| **高可用** | 主从复制、哨兵（Sentinel）、集群（Cluster，16384 槽位） |
| **缓存问题** | 穿透（Bloom Filter/空值缓存）、击穿（互斥锁/永不过期）、雪崩（过期随机化/熔断） |
| **应用** | 分布式锁（SET NX EX + Redisson 看门狗）、计数器（INCR）、消息队列（List/Stream） |

---

## 四、中间件与分布式（阶段四）

| 领域 | 核心内容 |
|------|---------|
| **消息队列** | RabbitMQ（AMQP/交换机/死信队列/延迟队列）、Kafka（Topic/Partition/Consumer Group/ISR/零拷贝/Exactly-Once）、RocketMQ（事务消息/顺序消息/延迟消息） |
| **搜索引擎** | Elasticsearch（倒排索引/Query DSL/聚合分析/IK 分词器/分片与副本） |
| **Docker** | 镜像/容器/仓库、Dockerfile（FROM/RUN/COPY/CMD/ENTRYPOINT）、Docker Compose |
| **微服务治理** | 服务注册发现、负载均衡、熔断降级、网关路由、配置中心、链路追踪 |

---

## 五、工程化与工具链（阶段五）

| 模块 | 核心知识点 |
|------|-----------|
| **Maven** | POM 坐标（groupId/artifactId/version）、依赖范围（compile/test/provided）、生命周期（clean/default/site）、依赖冲突解决 |
| **Git** | 工作区/暂存区/本地仓库/远程仓库；分支管理（merge/rebase）；远程协作（clone/push/pull/fetch）；stash/revert/reset |
| **Linux** | 文件操作（ls/cd/cp/mv/rm/cat/tail）、权限管理（chmod/chown）、进程管理（ps/kill/top）、网络（ifconfig/ping/netstat）、Shell 脚本基础 |
| **RESTful API** | 资源命名（名词复数）、HTTP 动词语义化（GET/POST/PUT/DELETE/PATCH）、状态码（2xx/3xx/4xx/5xx）、JSON 统一格式、版本控制 |
| **单元测试** | JUnit 5（@Test/@BeforeEach/断言）、Mockito（@Mock/@InjectMocks/when-thenReturn/verify）、JaCoCo 覆盖率 |
| **日志系统** | SLF4J 门面 + Logback/Log4j2 实现、日志级别（ERROR/WARN/INFO/DEBUG）、ELK 日志体系 |
| **CI/CD** | Jenkins Pipeline、GitHub Actions、GitLab CI/CD |
| **工具** | IDEA、Navicat/DataGrip、Postman、Swagger/OpenAPI、Lombok、MapStruct |

---

## 六、架构设计与面试（阶段六）

### 6.1 设计模式与安全

| 领域 | 核心内容 |
|------|---------|
| **设计模式** | 创建型（单例 5 种实现/工厂/建造者）、结构型（代理 JDK/CGLIB/适配器/装饰器）、行为型（观察者/策略/模板方法/责任链） |
| **安全** | Spring Security（认证+授权+过滤器链）、Shiro（Subject/SecurityManager/Realm）、JWT（Header.Payload.Signature）、OAuth2 |

### 6.2 性能优化

| 层面 | 核心方法 |
|------|---------|
| **JVM** | 堆内存配置（-Xms/-Xmx）、GC 收集器选择（G1/ZGC）、GC 日志分析 |
| **SQL** | 添加索引、避免 SELECT *、避免索引列使用函数、JOIN 代替子查询、分页优化 |
| **缓存** | 多级缓存（Caffeine+Redis）、穿透/击穿/雪崩方案 |
| **并发** | 减小锁粒度、使用读写锁/无锁数据结构、线程池合理配置 |

### 6.3 高频面试考点

| 领域 | 核心考点 |
|------|---------|
| **Java** | HashMap 原理（JDK 7 vs 8）、ConcurrentHashMap 线程安全、synchronized 锁升级、volatile 作用、ThreadLocal 内存泄漏、CAS 与 ABA 问题 |
| **JVM** | 类加载过程、双亲委派模型、GC 算法与收集器对比、OOM 排查思路 |
| **Spring** | IoC 容器初始化、Bean 生命周期、AOP 实现原理、事务失效场景、循环依赖（三级缓存） |
| **MySQL** | B+Tree 索引原理、事务隔离级别、MVCC、锁机制、慢查询优化 |
| **Redis** | 数据结构底层实现、持久化区别、缓存穿透/击穿/雪崩、分布式锁、集群方案 |
| **分布式** | CAP 定理、分布式事务方案（2PC/TCC/SAGA）、服务注册发现、熔断降级 |

---

## 七、推荐学习计划

| 阶段 | 时长 | 核心内容 | 产出 |
|:----:|:---:|---------|------|
| **阶段一** | 1~2 月 | Java SE（OOP/集合/IO/多线程）+ MySQL 基本操作 + JDBC + Maven/Git/Linux | 可独立编写 CRUD 程序 |
| **阶段二** | 2~3 月 | Spring（IoC/AOP/MVC）+ Spring Boot + MyBatis + Redis | 完成 1 个完整 Web 项目 |
| **阶段三** | 2~3 月 | Spring Cloud 微服务 + MQ + ES + Docker | 完成微服务项目 |
| **阶段四** | 长期 | 源码研究（Spring/MyBatis/JDK）、性能优化、高可用架构、开源贡献 | 达到高级/架构师水平 |

---

## 八、推荐资源

| 类别 | 资源 |
|------|------|
| **书籍** | 《Java 核心技术 卷I》《深入理解 Java 虚拟机》《Effective Java》《Spring 实战》《MySQL 必知必会》《Redis 设计与实现》 |
| **视频** | 尚硅谷 Java 全套、黑马程序员 JavaEE 就业班、尚硅谷 Spring Cloud 全家桶 |
| **官方文档** | Spring Framework Reference、MyBatis 官方文档、MySQL/Redis 官方文档 |
| **社区** | 掘金、CSDN、博客园、GitHub、Stack Overflow |
| **项目** | 电商系统（用户/商品/订单/支付）、秒杀系统（高并发）、博客系统（文章/评论） |

---

*最后更新：2026-07-15*
