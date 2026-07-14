# Java 后端开发工程师核心理论知识体系

## 概述

本文按九大理论模块系统梳理 Java 后端开发工程师必须掌握的核心理论知识体系，覆盖 **语言基础、JVM、数据存储、Web 开发、框架、分布式中间件、DevOps、软件设计、其他理论**。适用于体系化学习规划与面试知识自查。

---

## 知识体系全景

```
一、Java 语言基础           二、JVM 深度理解            三、数据库与存储
  OOP / 集合 / IO            内存结构 / GC / JMM         MySQL / Redis / ES
  泛型 / 注解 / 反射          类加载 / 调优               ORM（MyBatis/JPA）

四、Web 开发                 五、主流框架                六、分布式中间件
  HTTP / Servlet             Spring / Spring Boot        MQ / 缓存 / ES
  Filter / JSP               Spring Cloud                ZK / 分布式事务 / ID

七、DevOps 与工程化          八、软件设计与架构          九、其他理论
  Git / Maven / CI/CD        SOLID / 设计模式           安全 / 性能 / 测试
  Docker / K8s / 监控         架构模式 / DDD              敏捷 / 持续学习
```

---

## 一、Java 语言基础与核心类库

### 1.1 基础语法与 OOP

| 模块 | 核心知识点 |
|------|-----------|
| **基础语法** | 8 种基本类型 + 引用类型、运算符（算术/关系/逻辑/位/三元）、流程控制（if-else/switch/for/while）、数组 |
| **OOP 核心** | 类与对象、封装（4 种访问修饰符）、继承（extends/super/重写）、多态（向上/向下转型、重载 vs 重写） |
| **高级特性** | 抽象类 vs 接口（default/static 方法）、内部类（成员/局部/匿名/静态）、枚举 |
| **异常处理** | Error vs Exception、Checked vs Unchecked、try-catch-finally、throw vs throws、try-with-resources |

### 1.2 常用类库

| 包 | 核心类/接口 |
|----|-----------|
| **java.lang** | Object（toString/equals/hashCode）、String（不可变/StringBuilder/StringBuffer）、Math、System、包装类（自动装箱拆箱） |
| **java.util** | Collections Framework（List/Set/Queue/Map 体系）、java.time（LocalDate/LocalDateTime/Duration）、Optional、Comparator/Comparable、Stream API（中间操作 map/filter/flatMap + 终端操作 collect/reduce） |
| **java.io** | 字节流（InputStream/OutputStream 体系）、字符流（Reader/Writer 体系）、序列化（Serializable/serialVersionUID） |
| **java.nio** | Buffer/Channel/Selector 三大核心组件、Path/Files |

### 1.3 泛型、注解与反射

| 模块 | 核心知识点 |
|------|-----------|
| **泛型** | 泛型类/接口/方法、类型擦除、通配符（`? extends T` / `? super T` / `?`） |
| **注解** | 内置注解（`@Override`/`@Deprecated`）、元注解（`@Target`/`@Retention`）、自定义注解、APT 基础 |
| **反射** | Class 获取方式（forName/getClass/ClassName.class）、反射获取 Constructor/Field/Method、反射调用与性能开销 |

---

## 二、JVM 深度理解

### 2.1 内存结构与类加载

| 模块 | 核心知识点 |
|------|-----------|
| **内存结构** | 程序计数器、虚拟机栈（栈帧/局部变量表/操作数栈）、本地方法栈、堆（Eden/Survivor/老年代）、元空间（Metaspace，JDK 8+ 替代永久代） |
| **类加载** | 加载→验证→准备→解析→初始化；Bootstrap/Extension/Application ClassLoader；双亲委派模型及其破坏场景 |

### 2.2 垃圾收集（GC）

| 维度 | 核心内容 |
|------|---------|
| **存活判定** | 引用计数法（循环引用问题）、可达性分析（GC Roots） |
| **引用类型** | 强引用 / 软引用（SoftReference）/ 弱引用（WeakReference）/ 虚引用（PhantomReference） |
| **GC 算法** | 标记-清除 / 标记-复制 / 标记-整理 / 分代收集 |
| **GC 收集器** | Serial / ParNew / Parallel Scavenge / Serial Old / Parallel Old / CMS / G1 / ZGC / Shenandoah |
| **调优** | GC 日志分析、关键 JVM 参数 |

### 2.3 JMM 与并发

| 模块 | 核心知识点 |
|------|-----------|
| **JMM** | 主内存与工作内存、原子性/可见性/有序性、happens-before 原则 |
| **volatile** | 保证可见性和有序性，不保证原子性 |
| **synchronized** | Monitor 监视器锁，保证原子性/可见性/有序性 |
| **Lock** | ReentrantLock（公平/非公平）、ReadWriteLock |
| **线程池** | ThreadPoolExecutor 七大参数（corePoolSize/maximumPoolSize/keepAliveTime/unit/workQueue/threadFactory/rejectedExecutionHandler） |
| **并发工具** | CountDownLatch / CyclicBarrier / Semaphore / Exchanger / Phaser |
| **并发集合** | ConcurrentHashMap（JDK 7 Segment → JDK 8 CAS+synchronized）、CopyOnWriteArrayList、BlockingQueue 体系 |
| **线程安全策略** | 不可变对象、线程封闭（ThreadLocal 原理与内存泄漏）、同步容器 vs 并发容器 |

---

## 三、数据库与数据存储

### 3.1 MySQL 核心

| 领域 | 核心知识点 |
|------|-----------|
| **SQL** | DDL/DML/DQL（JOIN/GROUP BY/HAVING/UNION/DISTINCT/CASE WHEN） |
| **存储引擎** | InnoDB（事务/行锁/MVCC/外键）vs MyISAM（表锁/全文索引） |
| **索引** | B+Tree（聚簇 vs 非聚簇）、Hash、Full-Text；最左前缀原则、索引失效场景、EXPLAIN 执行计划 |
| **事务** | ACID、四种隔离级别、脏读/不可重复读/幻读 |
| **锁** | 共享锁(S)/排他锁(X)、意向锁、间隙锁/临键锁（InnoDB RR 解决幻读）、乐观锁（版本号）vs 悲观锁 |
| **日志** | Redo Log（物理日志/持久性）、Undo Log（逻辑日志/原子性+MVCC） |
| **MVCC** | Read View + Undo Log 版本链 |
| **分库分表** | 垂直/水平拆分、分片键与分片策略（Range/Hash/List）、ShardingSphere-JDBC/MyCat |
| **主从复制** | Binlog + Relaylog 原理、读写分离 |

### 3.2 NoSQL 数据库

| 数据库 | 类型 | 核心知识点 |
|--------|------|-----------|
| **Redis** | KV 存储 | 5 种基本类型 + Bitmaps/HyperLogLog/Geo；RDB/AOF 持久化；主从/哨兵/Cluster；8 种淘汰策略；缓存穿透/击穿/雪崩；分布式锁（SET NX EX） |
| **MongoDB** | 文档存储 | BSON 文档、CRUD、索引、副本集、分片 |
| **Elasticsearch** | 搜索引擎 | Index/Mapping/Shard/Replica、倒排索引、Query DSL、聚合分析、分词器 |

### 3.3 ORM 框架

| 框架 | 核心知识点 |
|------|-----------|
| **JDBC** | DriverManager/Connection/Statement/PreparedStatement/ResultSet、连接池（Druid/HikariCP/C3P0） |
| **MyBatis** | SqlSessionFactory → SqlSession → Mapper；`#{}` vs `${}`（防 SQL 注入）；动态 SQL（if/choose/foreach）；一级缓存（SqlSession）/二级缓存（Mapper）；插件（Interceptor） |
| **JPA/Hibernate** | 实体/实体关系（OneToOne/OneToMany/ManyToMany）、JPQL、Criteria API |

---

## 四、Web 开发与 Servlet 容器

| 模块 | 核心知识点 |
|------|-----------|
| **HTTP 协议** | 请求/响应报文结构；常用方法（GET/POST/PUT/DELETE）；状态码（2xx/3xx/4xx/5xx）；常用头部字段（Content-Type/Authorization/Cache-Control）；Cookie vs Session |
| **HTTPS** | SSL/TLS 握手、对称加密 vs 非对称加密 |
| **Servlet** | 生命周期（init → service → destroy）；HttpServletRequest/HttpServletResponse；转发 vs 重定向 |
| **Filter** | 生命周期与拦截链，应用场景（权限/日志/编码） |
| **Listener** | ServletContextListener、HttpSessionListener 等 8 种监听器 |
| **JSP 与模板** | JSP 生命周期/Scriptlet/EL/JSTL；现代替代：Thymeleaf/FreeMarker（或前后端分离） |
| **部署** | WAR 包、web.xml、Tomcat（server.xml/Connector/Container） |

---

## 五、主流开发框架

### 5.1 Spring Framework 核心

| 模块 | 核心知识点 |
|------|-----------|
| **IoC/DI** | BeanFactory vs ApplicationContext；Bean 生命周期（实例化→属性填充→初始化→使用→销毁）；Bean 作用域（singleton/prototype/request/session） |
| **AOP** | 切面/连接点/切入点/通知（Before/After/Around）；JDK 动态代理 vs CGLIB；应用场景（日志/事务/权限） |
| **Spring MVC** | DispatcherServlet → HandlerMapping → HandlerAdapter → ViewResolver 全链路；Controller 注解（`@RequestMapping`/`@PathVariable`/`@RequestBody`）；拦截器 vs 过滤器 |
| **Spring Boot** | 自动配置（`@EnableAutoConfiguration`/`spring.factories`/`@Conditional` 系列）；Starter；Actuator；application.yml 多环境配置；启动流程（`SpringApplication.run()`） |
| **Spring Transaction** | `@Transactional`：7 种传播行为、5 种隔离级别、失效场景 |
| **日志** | SLF4J + Logback/Log4j2 |

### 5.2 Spring Cloud 微服务组件矩阵

| 组件 | 技术选项 | 核心功能 |
|------|---------|---------|
| 注册中心 | Eureka(AP) / Nacos(AP+CP) / Consul(CP) / ZK(CP) | 服务注册、心跳检测、服务发现 |
| 配置中心 | Spring Cloud Config / Nacos Config / Apollo | 统一配置、动态刷新 |
| 负载均衡 | Ribbon → Spring Cloud LoadBalancer | 客户端负载均衡 |
| 远程调用 | OpenFeign | 声明式 HTTP 客户端 |
| 熔断降级 | Resilience4j / Sentinel | 熔断、限流、降级 |
| 网关 | Spring Cloud Gateway（异步非阻塞） | 路由转发、过滤器链 |
| 链路追踪 | Sleuth+Zipkin / SkyWalking | 调用链追踪 |
| 消息总线 | Spring Cloud Bus + RabbitMQ/Kafka | 配置刷新广播 |

---

## 六、分布式系统与中间件

### 6.1 消息队列对比

| 维度 | RabbitMQ | Kafka | RocketMQ |
|------|---------|-------|----------|
| 协议 | AMQP | 自定义 | 自定义 |
| 吞吐量 | 中等 | 极高 | 高 |
| 可靠性 | 高（确认机制） | 可配置 | 高（同步刷盘） |
| 核心能力 | 死信队列/延迟队列 | Exactly-Once/Streams/零拷贝 | 事务消息/顺序消息/延迟消息 |
| 适用场景 | 业务解耦 | 日志/流处理 | 电商/金融 |

### 6.2 缓存与搜索

| 领域 | 核心知识点 |
|------|-----------|
| **本地缓存** | Caffeine、Guava Cache |
| **分布式缓存** | Redis Cluster、Memcached |
| **缓存策略** | Cache-Aside / Read/Write-Through / Write-Behind |
| **缓存问题** | 穿透（Bloom Filter/空值缓存）、击穿（互斥锁/永不过期）、雪崩（过期随机化/集群/熔断） |
| **搜索引擎** | Elasticsearch：倒排索引 → Query DSL → 聚合 → 分词器 |

### 6.3 分布式协调与事务

| 领域 | 核心技术方案 |
|------|------------|
| **分布式协调** | ZooKeeper（CP/ZNode/Watcher/分布式锁/选主）、Etcd（Raft/K8s 核心依赖） |
| **分布式事务** | XA（2PC/3PC）→ TCC（Try-Confirm-Cancel）→ Saga（长事务）→ 本地消息表 → 最大努力通知 |
| **分布式 ID** | UUID → 雪花算法（Snowflake，64 位：符号位+时间戳+机器ID+序列号）→ Leaf/UidGenerator |

---

## 七、DevOps 与工程化

| 领域 | 核心技术点 |
|------|-----------|
| **Git** | 工作区/暂存区/本地仓库/远程仓库；分支管理（Git Flow/GitHub Flow/Trunk-Based）；merge/rebase/reset/revert/stash |
| **Maven/Gradle** | POM 坐标（groupId/artifactId/version）、依赖范围、生命周期、插件、仓库体系 |
| **CI/CD** | Jenkins Pipeline / GitLab CI / GitHub Actions；流程：提交→编译→测试→扫描→归档→部署 |
| **Docker** | 镜像/容器/仓库、Dockerfile（FROM/RUN/COPY/CMD/ENTRYPOINT）、Docker Compose |
| **K8s** | Pod/Service/Deployment/StatefulSet/DaemonSet/ConfigMap/Secret/Ingress/HPA/PV-PVC |
| **监控** | Prometheus（Pull 模式时序库）+ Grafana + Alertmanager |
| **日志** | ELK（ES+Logstash+Kibana）/ EFK（ES+Fluentd+Kibana）|
| **代码质量** | SonarQube / Checkstyle / PMD / SpotBugs；OWASP 安全扫描 |

---

## 八、软件设计与架构

### 8.1 设计原则与模式

| 类别 | 内容 |
|------|------|
| **SOLID** | 单一职责(SRP) / 开闭原则(OCP) / 里氏替换(LSP) / 接口隔离(ISP) / 依赖倒置(DIP) |
| **创建型** | 单例、工厂方法、抽象工厂、建造者、原型 |
| **结构型** | 适配器、装饰器、代理（静态/JDK/CGLIB）、外观、桥接、组合、享元 |
| **行为型** | 策略、模板方法、观察者、迭代器、责任链、命令、状态 |

### 8.2 架构模式与理论

| 领域 | 核心内容 |
|------|---------|
| **架构风格** | 分层架构（表示层→应用层→领域层→基础设施层）、MVC/MVP/MVVM、事件驱动架构（EDA）、微服务架构 |
| **分布式理论** | CAP 定理（一致性/可用性/分区容错）、BASE 理论（基本可用/软状态/最终一致） |
| **API 设计** | RESTful（URI 命名/HTTP 动词语义化/状态码/HATEOAS）、GraphQL、gRPC（HTTP/2+Protobuf） |
| **DDD** | 领域模型、限界上下文、聚合根、实体、值对象、领域服务、仓储、领域事件 |

---

## 九、其他重要理论

### 9.1 安全知识

| 领域 | 核心内容 |
|------|---------|
| **认证授权** | Session-Cookie / JWT / OAuth 2.0 / OpenID Connect；RBAC / ABAC |
| **Web 攻击防御** | XSS / CSRF / SQL 注入 / XXE / 点击劫持；OWASP Top 10 |
| **密码学** | 对称加密（AES）、非对称加密（RSA）、哈希（MD5/SHA-256/BCrypt/Argon2）、HMAC、数字签名 |

### 9.2 性能优化

| 层面 | 核心方法 |
|------|---------|
| **测试工具** | JMeter / Gatling / LoadRunner（负载/压力/并发/稳定性测试） |
| **JVM 调优** | GC 日志分析、堆内存配置、收集器选择 |
| **SQL 调优** | EXPLAIN 分析、索引优化、慢查询治理 |
| **应用层** | 缓存策略、异步处理（CompletableFuture）、批处理、减少 DB 交互 |

### 9.3 测试与工程实践

| 领域 | 核心内容 |
|------|---------|
| **单元测试** | JUnit 5（`@Test`/`@BeforeEach`/assertions）、Mockito（`@Mock`/`@InjectMocks`/`when-thenReturn`） |
| **测试金字塔** | 单元测试（多）→ 集成测试（中）→ E2E 测试（少） |
| **敏捷开发** | Scrum（Sprint/Stand-up/Retrospective）、Kanban、XP |

---

## 十、知识掌握度自查矩阵

| 理论模块 | 了解 | 理解 | 掌握 | 精通 |
|---------|:---:|:---:|:---:|:---:|
| OOP / 集合 / IO / 泛型 / 反射 | | | | |
| JVM 内存 / GC / JMM / 并发 | | | | |
| MySQL 索引 / 事务 / 锁 / MVCC | | | | |
| Redis 数据结构 / 持久化 / 集群 | | | | |
| Spring IoC / AOP / MVC / Boot | | | | |
| Spring Cloud 微服务组件 | | | | |
| MQ / 缓存 / 分布式事务 | | | | |
| Docker / K8s / CI/CD | | | | |
| 设计模式 / SOLID / DDD | | | | |
| 安全 / 性能 / 测试 | | | | |

---

*最后更新：2026-07-15*
