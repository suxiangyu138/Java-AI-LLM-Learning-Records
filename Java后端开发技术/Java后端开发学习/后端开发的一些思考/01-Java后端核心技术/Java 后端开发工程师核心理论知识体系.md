04.04 21:37
Java 后端开发工程师核心理论知识体系
一、 Java 语言基础与核心类库 (The Foundation)
Java 语言基础: 基本语法: 数据类型 (8种基本类型、引用类型)、变量与常量、运算符 (算术、关系、逻辑、位、赋值、三元)、流程控制 (if-else, switch, for, while, do-while, break, continue)。 
数组: 一维数组、多维数组、数组的声明、初始化、遍历、Arrays 工具类。 
面向对象编程 (OOP): 核心概念: 类与对象、封装 (访问修饰符 private, default, protected, public)、继承 (extends, super, 方法重写 @Override, 构造方法调用)、多态 (向上转型、向下转型 instanceof, 编译时多态-重载 Overload, 运行时多态-重写 Override)。 
高级特性: 抽象类 (abstract class, abstract method)、接口 (interface, default method, static method, 多实现)、内部类 (成员内部类、局部内部类、匿名内部类、静态内部类)、枚举 (enum)。 
异常处理: Error vs Exception、Checked Exception vs Unchecked Exception (RuntimeException)、try-catch-finally、throw vs throws、自定义异常、try-with-resources (JDK7+)。 
常用类库: java.lang: Object (toString(), equals(), hashCode(), getClass(), clone(), finalize() 已废弃), String (不可变性, String, StringBuilder, StringBuffer 区别), Math, System, Thread, Class, Enum, Wrapper Classes (自动装箱拆箱)。 
java.util: Collections Framework (List, Set, Queue, Map 及其主要实现类 ArrayList, LinkedList, Vector, Stack, HashSet, LinkedHashSet, TreeSet, HashMap, Hashtable, LinkedHashMap, TreeMap, PriorityQueue, ArrayDeque, Collections 工具类), Date (已过时) & Calendar (已过时) & java.time (JSR-310, JDK8+) (LocalDate, LocalTime, LocalDateTime, ZonedDateTime, Duration, Period, DateTimeFormatter, Instant, ZoneId, ZoneOffset), Random, Scanner, Properties, Optional, Comparable & Comparator, Iterator & ListIterator, Stream API (JDK8+) (Stream, IntStream, LongStream, DoubleStream, 中间操作 map, filter, flatMap, distinct, sorted, limit, skip; 终端操作 collect, forEach, count, min, max, anyMatch, allMatch, noneMatch, findFirst, findAny, reduce)。 
java.io: 字节流 (InputStream, OutputStream, FileInputStream, FileOutputStream, BufferedInputStream, BufferedOutputStream, DataInputStream, DataOutputStream, ObjectInputStream, ObjectOutputStream, ByteArrayInputStream, ByteArrayOutputStream, PipedInputStream, PipedOutputStream)、字符流 (Reader, Writer, FileReader, FileWriter, BufferedReader, BufferedWriter, InputStreamReader, OutputStreamWriter, CharArrayReader, CharArrayWriter)、文件操作 (File, Paths, Files)、序列化与反序列化 (Serializable 接口, serialVersionUID)。 
java.nio: Buffer (ByteBuffer, CharBuffer, ShortBuffer, IntBuffer, LongBuffer, FloatBuffer, DoubleBuffer), Channel (FileChannel, SocketChannel, ServerSocketChannel, DatagramChannel), Selector (多路复用器), Path, Paths, Files。 
泛型 (Generics): 泛型类、泛型接口、泛型方法。 
类型擦除 (Type Erasure)。 
通配符 (? extends T, ? super T, ?)。 
注解 (Annotations): 内置注解 (@Override, @Deprecated, @SuppressWarnings, @FunctionalInterface)。 
元注解 (@Target, @Retention, @Documented, @Inherited)。 
自定义注解。 
APT (Annotation Processing Tool) 基础。 
反射 (Reflection): Class 类与获取 Class 对象的方式 (Class.forName(), obj.getClass(), ClassName.class)。 
反射获取构造方法 (Constructor)、成员变量 (Field)、成员方法 (Method)。 
反射调用方法、访问和修改成员变量。 
反射的应用场景与注意事项 (性能开销、安全问题)。 
二、 Java 虚拟机 (JVM) 深度理解 (The Runtime)
JVM 内存结构: 程序计数器 (Program Counter Register)。 
Java 虚拟机栈 (Java Virtual Machine Stacks): 栈帧 (Stack Frame)、局部变量表 (Local Variables Table)、操作数栈 (Operand Stack)、动态链接 (Dynamic Linking)、方法返回地址 (Return Address)。 
本地方法栈 (Native Method Stack)。 
堆 (Heap): Eden 区、Survivor 区 (From Survivor, To Survivor)、老年代 (Old Generation/Tenured Generation)、元空间 (Metaspace, JDK8+, 取代永久代 PermGen)。 
方法区 (Method Area): 已被 Metaspace 取代，存储类信息、常量、静态变量、即时编译器编译后的代码等。 
类加载机制: 类加载过程: 加载 (Loading)、验证 (Verification)、准备 (Preparation)、解析 (Resolution)、初始化 (Initialization)。 
类加载器 (ClassLoader): 启动类加载器 (Bootstrap ClassLoader)、扩展类加载器 (Extension ClassLoader)、应用程序类加载器 (Application ClassLoader/System ClassLoader)、自定义类加载器。 
双亲委派模型 (Parent Delegation Model) 及其破坏。 
垃圾收集 (Garbage Collection, GC): 确定对象是否存活: 引用计数法 (Reference Counting, 有循环引用问题)、可达性分析算法 (Reachability Analysis, GC Roots: 虚拟机栈中引用的对象、方法区中类静态属性引用的对象、方法区中常量引用的对象、本地方法栈中 JNI 引用的对象)。 
Java 引用类型: 强引用 (Strong Reference)、软引用 (Soft Reference)、弱引用 (Weak Reference)、虚引用 (Phantom Reference)。 
垃圾收集算法: 标记-清除 (Mark-Sweep)、标记-复制 (Mark-Copy)、标记-整理 (Mark-Compact)、分代收集 (Generational Collection) 思想。 
垃圾收集器: Serial, ParNew, Parallel Scavenge, Serial Old, Parallel Old, CMS (Concurrent Mark Sweep), G1 (Garbage-First), ZGC (Z Garbage Collector), Shenandoah GC。各自的优缺点、适用场景及关键 JVM 参数。 
GC 日志分析与调优基础。 
Java 内存模型 (Java Memory Model, JMM): 主内存与工作内存。 
内存间的交互操作 (lock, unlock, read, load, use, assign, store, write)。 
原子性 (Atomicity)、可见性 (Visibility)、有序性 (Ordering)。 
happens-before 原则。 
volatile 关键字 (保证可见性和有序性，不保证原子性)。 
synchronized 关键字 (监视器锁 Monitor, 保证原子性、可见性、有序性)。 
Lock 接口及其实现类 (ReentrantLock, ReadWriteLock)。 
原子类 (java.util.concurrent.atomic)。 
线程池 (Executor Framework): Executor, ExecutorService, ThreadPoolExecutor, ScheduledThreadPoolExecutor, Callable, Future, FutureTask, CompletionService。线程池核心参数 (corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, rejectedExecutionHandler)。 
并发集合 (Concurrent Collections): ConcurrentHashMap (JDK7 Segment + HashEntry, JDK8 Node + CAS + synchronized), CopyOnWriteArrayList, CopyOnWriteArraySet, ConcurrentLinkedQueue, ConcurrentSkipListMap, ConcurrentSkipListSet, BlockingQueue (ArrayBlockingQueue, LinkedBlockingQueue, PriorityBlockingQueue, SynchronousQueue, DelayQueue)。 
同步工具类 (Synchronization Utilities): CountDownLatch, CyclicBarrier, Semaphore, Exchanger, Phaser。 
线程安全策略: 不可变对象、线程封闭 (Thread Confinement)、同步容器 (Collections.synchronizedXXX)、并发容器、同步代码块/方法、ThreadLocal (原理及内存泄漏风险)。 
三、 数据库与数据存储 (Data Persistence)
关系型数据库 (Relational Database): SQL 语言精通: DDL (CREATE, ALTER, DROP, TRUNCATE), DML (INSERT, UPDATE, DELETE), DQL (SELECT, JOIN - INNER JOIN, LEFT JOIN, RIGHT JOIN, FULL OUTER JOIN, CROSS JOIN; WHERE, GROUP BY, HAVING, ORDER BY, LIMIT/OFFSET, UNION/UNION ALL, DISTINCT, IN, BETWEEN AND, LIKE, IS NULL, EXISTS, CASE WHEN THEN ELSE END)。 
MySQL 深入: 体系结构与组件 (连接池、管理服务、SQL接口、解析器、优化器、缓存、存储引擎)。 
存储引擎对比: InnoDB (事务支持、行级锁、外键、MVCC) vs MyISAM (表级锁、不支持事务和外键、全文索引)。 
数据类型选择与优化。 
索引原理与实践: 索引类型: B-Tree (B+Tree 变种) 索引 (聚集索引 Clustered Index, 非聚集索引 Secondary Index/Non-Clustered Index)、哈希索引 (Memory 引擎)、全文索引 (Full-text Index)、空间索引 (Spatial Index)。 
索引优缺点及适用场景。 
索引失效的常见情况。 
索引设计原则与优化技巧。 
事务 (Transaction): ACID 特性 (原子性 Atomicity, 一致性 Consistency, 隔离性 Isolation, 持久性 Durability)。 
并发事务问题与隔离级别: 脏读 (Dirty Read)、不可重复读 (Non-repeatable Read)、幻读 (Phantom Read)。四种隔离级别 (READ UNCOMMITTED, READ COMMITTED, REPEATABLE READ - MySQL默认, SERIALIZABLE)。 
锁机制详解: 锁粒度: 表锁 (Table Lock)、行锁 (Row Lock)。 
锁类型: 共享锁 (Shared Lock/S Lock)、排他锁 (Exclusive Lock/X Lock)。 
意向锁 (Intention Lock)。 
间隙锁 (Gap Lock)、临键锁 (Next-Key Lock) - InnoDB RR 级别下解决幻读。 
乐观锁 (Optimistic Locking - 版本号/时间戳) vs 悲观锁 (Pessimistic Locking)。 
日志系统: Redo Log (物理日志，保证持久性)、Undo Log (逻辑日志，保证原子性和 MVCC)。 
MVCC (Multi-Version Concurrency Control) 原理: Read View, Undo Log 版本链。 
SQL 执行计划 (EXPLAIN) 解读与优化。 
慢查询日志分析与优化。 
分库分表 (Sharding) 理论与实践: 垂直拆分、水平拆分，分片键 (Sharding Key) 选择，分片策略 (Range, Hash, List, Composite)，分片中间件 (Sharding-JDBC, MyCat)。 
主从复制 (Master-Slave Replication) 原理 (Binlog, Relaylog) 与读写分离。 
非关系型数据库 (NoSQL): Redis: 核心数据结构: String, Hash, List, Set, Sorted Set (ZSet), Bitmaps, HyperLogLogs, Geospatial indexes。 
持久化机制: RDB (Redis Database) 快照, AOF (Append Only File) 日志。 
事务 (MULTI/EXEC/DISCARD/WATCH)。 
发布订阅 (Pub/Sub)。 
主从复制、哨兵 (Sentinel) 高可用、集群 (Cluster) 模式。 
内存淘汰策略 (maxmemory-policy)。 
缓存穿透、缓存击穿、缓存雪崩问题及解决方案。 
分布式锁实现 (SET NX EX)。 
MongoDB (文档型数据库): 文档 (BSON), 集合 (Collection), 数据库 (Database), CRUD 操作, 索引, 副本集 (Replica Set), 分片 (Sharding)。 
Elasticsearch (搜索引擎): 核心概念 (Index, Type - deprecated, Document, Mapping, Shard, Replica), 倒排索引 (Inverted Index), 基本查询 DSL, 聚合分析, 分词器。 
数据库连接与 ORM: JDBC 规范与编程: DriverManager, Connection, Statement, PreparedStatement, CallableStatement, ResultSet, 批处理, 事务管理, 连接池 (Druid, HikariCP, C3P0) 原理与配置。 
ORM 框架 (以 MyBatis 为主): MyBatis 核心组件: SqlSessionFactory, SqlSession, Mapper Interface, Mapper XML/Annotation。 
{} 与 ${} 的区别，防 SQL 注入。
MyBatis 配置 (mybatis-config.xml, mapper locations)。 
Mapper XML 文件详解: <select>, <insert>, <update>, <delete>, <resultMap>, <sql>, <include>。 
动态 SQL: <if>, <choose> <when> <otherwise>, <trim> <where> <set>, <foreach>。 
高级特性: 延迟加载 (Lazy Loading), 一级缓存 (SqlSession 级别) 与二级缓存 (Mapper 级别), 插件 (Plugins - Interceptor)。 
JPA (Java Persistence API) 与 Hibernate: 实体 (Entity), 实体关系 (OneToOne, OneToMany, ManyToOne, ManyToMany), JPQL, Criteria API。理解 ORM 思想即可，MyBatis 是目前主流选择。 
四、 Web 开发与 Servlet 容器 (Web Development)
HTTP 协议详解: 请求报文结构: 请求行 (Method, URI, Version), 请求头 (Headers), 空行, 请求体 (Body)。 
响应报文结构: 状态行 (Version, Status Code, Reason Phrase), 响应头 (Headers), 空行, 响应体 (Body)。 
常用 HTTP 方法: GET, POST, PUT, DELETE, HEAD, OPTIONS, PATCH。 
常用 HTTP 状态码: 1xx (Informational), 2xx (Success - 200 OK, 201 Created), 3xx (Redirection - 301 Moved Permanently, 302 Found, 304 Not Modified), 4xx (Client Error - 400 Bad Request, 401 Unauthorized, 403 Forbidden, 404 Not Found), 5xx (Server Error - 500 Internal Server Error, 502 Bad Gateway, 503 Service Unavailable)。 
常用 HTTP 头部字段: Host, User-Agent, Accept, Accept-Language, Accept-Encoding, Content-Type (application/json, application/x-www-form-urlencoded, multipart/form-data), Authorization (Bearer token), Cookie, Set-Cookie, Cache-Control, Connection, Location。 
Cookie 与 Session 的工作原理及区别。 
HTTPS 基本原理 (SSL/TLS 握手, 对称加密与非对称加密)。 
Servlet API: Servlet 生命周期: init(), service(ServletRequest req, ServletResponse res), destroy()。 
ServletConfig 与 ServletContext。 
HttpServletRequest (获取请求参数 getParameter(), getParameterValues(), getHeader(), getCookies(), getSession(), getRequestDispatcher(); 请求转发 forward(), 请求包含 include())。 
HttpServletResponse (设置状态码 sendError(), setStatus(); 设置响应头 setHeader(), addHeader(); 发送响应体 getWriter(), getOutputStream(); 重定向 sendRedirect())。 
Filter (过滤器): 生命周期 (init(), doFilter(), destroy()), 拦截路径配置, 应用场景 (权限控制、日志记录、编码过滤)。 
Listener (监听器): ServletContextListener, ServletContextAttributeListener, HttpSessionListener, HttpSessionAttributeListener, ServletRequestListener, ServletRequestAttributeListener。 
Session 管理机制。 
JSP (JavaServer Pages) 基础: JSP 生命周期, Scriptlet (<% %>), Declaration (<%! %>), Expression (<%= %>), Directive (<%@ %>), Action (<jsp:action>), JSTL (JSP Standard Tag Library) 和 EL (Expression Language) 表达式。注: 现在更多使用 Thymeleaf, FreeMarker, Velocity 等模板引擎，或前后端分离。 
Web 应用部署与配置: WAR 包, web.xml 配置, 应用服务器 (Tomcat, Jetty, Undertow) 核心概念与配置 (server.xml, context.xml, 连接器 Connector, 容器 Container)。 
五、 主流开发框架 (Frameworks)
Spring Framework 核心: Spring IoC (Inversion of Control) 控制反转 / DI (Dependency Injection) 依赖注入: IoC 容器: BeanFactory (基础容器) 和 ApplicationContext (高级容器，继承 BeanFactory，提供更多企业级功能)。 
Bean 的生命周期: 实例化 -> 属性赋值 (依赖注入) -> 初始化 (BeanPostProcessor, InitializingBean, init-method) -> 使用 -> 销毁 (DisposableBean, destroy-method)。 
Bean 的作用域: singleton, prototype, request, session, application, websocket。 
Bean 的装配方式: XML 配置, 注解配置 (@Component, @Service, @Controller, @Repository, @Autowired, @Qualifier, @Resource, @Value, @Scope, @PostConstruct, @PreDestroy), JavaConfig 配置 (@Configuration, @Bean, @ComponentScan, @PropertySource)。 
@Autowired 与 @Resource 区别。 
Spring AOP (Aspect-Oriented Programming) 面向切面编程: 核心概念: Aspect (切面), Join Point (连接点), Pointcut (切入点), Advice (通知: Before, AfterReturning, AfterThrowing, After, Around), Introduction (引介), Target Object (目标对象), AOP Proxy (AOP 代理: JDK 动态代理, CGLIB 代理)。 
AOP 实现原理: 动态代理。 
AOP 应用场景: 日志记录、性能统计、安全控制、事务管理、异常处理。 
@Aspect, @Pointcut, @Before, @After, @AfterReturning, @AfterThrowing, @Around 注解。 
Spring MVC 框架: DispatcherServlet (前端控制器) 作用。 
HandlerMapping (处理器映射器) 与 HandlerAdapter (处理器适配器)。 
Controller (控制器) 开发: @Controller, @RequestMapping, @GetMapping, @PostMapping, @PutMapping, @DeleteMapping, @PatchMapping, @PathVariable, @RequestParam, @RequestBody, @ResponseBody, @ModelAttribute, @SessionAttribute, @CookieValue, @RequestHeader, @Valid, BindingResult。 
View Resolver (视图解析器) 与 View (视图)。 
Model (模型) 与 ModelAndView。 
数据校验 (JSR-303/JSR-380: @NotNull, @Size, @Min, @Max, @Email 等, Hibernate Validator 实现)。 
文件上传下载。 
拦截器 (HandlerInterceptor) 与过滤器 (Filter) 区别。 
Spring Boot 框架: 核心特性: 自动配置 (Auto-configuration): @EnableAutoConfiguration, spring.factories 文件, @Conditional 系列注解 (@ConditionalOnClass, @ConditionalOnMissingBean 等)。 
起步依赖 (Starters): spring-boot-starter-*。 
嵌入式 Servlet 容器 (Embedded Servlet Container: Tomcat, Jetty, Undertow)。 
Actuator (监控与管理端点)。 
简化的外部化配置 (Externalized Configuration)。 
配置文件: application.properties/application.yml/yaml 语法, Profile (多环境配置: application-{profile}.properties/yml)。 
自定义 Starter: 命名规范, 自动配置类编写。 
Spring Boot 应用的启动流程: SpringApplication.run()。 
日志配置: SLF4J + Logback/Log4j2。 
热部署: spring-boot-devtools。 
Spring Cloud (微服务全家桶): 服务注册与发现: Eureka (AP), Consul (CP), Zookeeper (CP), Nacos (AP/CP)。服务注册、心跳检测、服务剔除、服务发现。 
客户端负载均衡: Ribbon (已停更，推荐使用 Spring Cloud LoadBalancer)。 
声明式 REST 客户端: OpenFeign (集成 Ribbon/Hystrix)。 
熔断器与服务降级: Resilience4j (推荐), Sentinel (阿里开源), Hystrix (Netflix 停更)。 
API 网关: Spring Cloud Gateway (异步非阻塞), Zuul (同步阻塞, 已停更)。 
分布式配置中心: Spring Cloud Config (+ Bus), Nacos Config, Apollo (携程开源)。 
分布式链路追踪: Spring Cloud Sleuth (+ Zipkin), SkyWalking (推荐)。 
消息总线: Spring Cloud Bus (+ RabbitMQ/Kafka)。 
微服务架构设计原则与挑战: 服务拆分粒度、服务通信 (REST/gRPC)、数据一致性 (分布式事务解决方案: 2PC, TCC, Saga, 本地消息表, 最大努力通知)、服务治理、配置管理、监控告警、DevOps。 
六、 分布式系统与中间件 (Distributed Systems & Middleware)
消息队列 (Message Queue): 核心概念: Producer, Consumer, Topic/Exchange, Queue, Message, Broker, ACK, Routing Key。 
RabbitMQ: AMQP 协议, Exchange 类型 (direct, fanout, topic, headers), 队列持久化, 消息确认机制, 死信队列 (DLX), 延迟队列实现。 
Kafka: 高吞吐量, 分布式, 发布-订阅模型, Topic, Partition, Replica, Leader/Follower, ISR, Consumer Group, Offset, 零拷贝, 顺序写盘, 页缓存, 适用场景 (日志收集, 流式计算)。 
RocketMQ: 阿里开源, 支持事务消息, 顺序消息, 延迟消息, 高可靠, 高可用。 
消息队列的作用: 异步处理, 应用解耦, 流量削峰, 日志处理, 最终一致性。 
缓存 (Caching): 本地缓存: Caffeine, Guava Cache。 
分布式缓存: Redis, Memcached。 
缓存策略: Cache-Aside (旁路缓存), Write-Through (写透), Write-Behind (写回)。 
缓存常见问题及解决方案: 缓存穿透 (Bloom Filter, 缓存空值), 缓存击穿 (互斥锁, 热点数据永不过期), 缓存雪崩 (过期时间随机化, 集群部署, 熔断降级), 缓存一致性问题 (Cache Aside Pattern 下的更新策略)。 
搜索引擎 (Search Engine): Elasticsearch: 索引 (Index), 文档 (Document), 映射 (Mapping), 分片 (Shard), 副本 (Replica), 倒排索引 (Inverted Index), 分词器 (Analyzer), Query DSL (查询领域特定语言), Aggregations (聚合)。 
分布式协调与服务治理: ZooKeeper: CP 特性, ZNode, Watcher, 临时节点, 持久节点, 分布式锁, 选主, 配置中心, 命名服务。 
Etcd: KV 存储, Raft 共识算法, Kubernetes 的核心依赖。 
分布式事务: XA 协议 (两阶段提交 2PC, 三阶段提交 3PC)。 
TCC (Try-Confirm-Cancel) 模式。 
Saga 模式 (长事务拆分)。 
本地消息表 (Local Message Table)。 
最大努力通知 (Best Effort Notification)。 
分布式 ID 生成: UUID/GUID。 
数据库自增序列 (雪花算法 Snowflake 变种)。 
雪花算法 (Snowflake): Twitter 开源, 64位 ID (符号位 + 时间戳 + 机器 ID + 序列号)。 
美团 Leaf, 百度 UidGenerator。 
七、 DevOps 与工程化 (DevOps & Engineering)
版本控制系统: Git: 核心概念 (工作区, 暂存区, 本地仓库, 远程仓库), 基本操作 (clone, add, commit, push, pull, fetch, checkout, branch, merge, rebase, reset, revert, stash), 分支管理策略 (Git Flow, GitHub Flow, Trunk Based Development), 冲突解决。 
构建工具: Maven: POM (Project Object Model), 坐标 (groupId, artifactId, version), 依赖管理 (dependencies, dependency scope), 生命周期 (clean, default, site), 插件 (plugins), 仓库 (local, central, remote)。 
Gradle: Groovy/Kotlin DSL, 灵活高效的构建工具。 
持续集成/持续部署 (CI/CD): 概念: CI (Continuous Integration), CD (Continuous Delivery/Deployment)。 
工具: Jenkins, GitLab CI/CD, GitHub Actions, Travis CI, CircleCI。Pipeline as Code (Jenkinsfile, .gitlab-ci.yml)。 
流程: 代码提交 -> 触发构建 -> 编译打包 -> 单元测试 -> 代码质量扫描 -> 集成测试 -> 制品归档 -> 部署到测试环境 -> 验收测试 -> 部署到生产环境 (手动/自动)。 
容器化技术: Docker: 镜像 (Image), 容器 (Container), 仓库 (Registry), Dockerfile (FROM, RUN, COPY, ADD, ENV, EXPOSE, CMD, ENTRYPOINT), Docker Compose (多容器编排)。 
容器编排: Kubernetes (K8s) 核心概念 (Pod, Service, Deployment, StatefulSet, DaemonSet, Job, CronJob, Namespace, ConfigMap, Secret, Ingress, Volume, PV/PVC, Label, Selector, ReplicaSet, Horizontal Pod Autoscaler)。 
监控与日志: 监控指标: QPS/TPS, 响应时间 (RT), 并发数, CPU 使用率, 内存使用率, 磁盘 I/O, 网络 I/O, JVM 指标 (GC 次数/耗时, 堆内存使用)。 
监控系统: Prometheus (时序数据库, Pull 模式) + Grafana (可视化), Zabbix, Nagios, SkyWalking, Pinpoint, Cat。 
日志收集与分析: ELK Stack (Elasticsearch, Logstash/Filebeat, Kibana), EFK Stack (Elasticsearch, Fluentd, Kibana), Graylog。日志规范与最佳实践。 
代码质量与安全: 代码规范: Google Java Style Guide, Alibaba Java Coding Guidelines。 
静态代码分析工具: SonarQube, Checkstyle, PMD, SpotBugs。 
安全扫描: OWASP Top 10, FindSecBugs, Dependency-Check (依赖漏洞扫描)。 
八、 软件设计与架构 (Software Design & Architecture)
设计原则 (SOLID): SRP (Single Responsibility Principle) 单一职责原则。 
OCP (Open/Closed Principle) 开闭原则。 
LSP (Liskov Substitution Principle) 里氏替换原则。 
ISP (Interface Segregation Principle) 接口隔离原则。 
DIP (Dependency Inversion Principle) 依赖倒置原则。 
设计模式 (Design Patterns): 创建型模式: 单例 (Singleton), 工厂方法 (Factory Method), 抽象工厂 (Abstract Factory), 建造者 (Builder), 原型 (Prototype)。 
结构型模式: 适配器 (Adapter), 装饰器 (Decorator), 代理 (Proxy - 静态代理, 动态代理 JDK/CGLIB), 外观 (Facade), 桥接 (Bridge), 组合 (Composite), 享元 (Flyweight)。 
行为型模式: 策略 (Strategy), 模板方法 (Template Method), 观察者 (Observer), 迭代器 (Iterator), 责任链 (Chain of Responsibility), 命令 (Command), 备忘录 (Memento), 状态 (State), 访问者 (Visitor), 中介者 (Mediator), 解释器 (Interpreter)。 
软件架构风格与模式: 分层架构 (Layered Architecture): Presentation Layer, Application Layer, Domain Layer (Business Logic Layer), Infrastructure Layer (Persistence Layer)。 
MVC/MVP/MVVM: 前端架构模式，理解思想。 
事件驱动架构 (Event-Driven Architecture, EDA)。 
微服务架构 (Microservices Architecture): 特点, 优势, 挑战, 拆分策略。 
RESTful API 设计: 资源, URI 命名规范, HTTP 方法语义化, 状态码正确使用, HATEOAS。 
API 网关模式 (API Gateway Pattern)。 
断路器模式 (Circuit Breaker Pattern)。 
分布式架构理论基础: CAP 定理 (Consistency, Availability, Partition Tolerance), BASE 理论 (Basically Available, Soft state, Eventually consistent)。 
领域驱动设计 (Domain-Driven Design, DDD): 核心概念: 领域模型 (Domain Model), 限界上下文 (Bounded Context), 聚合根 (Aggregate Root), 实体 (Entity), 值对象 (Value Object), 领域服务 (Domain Service), 仓储 (Repository), 领域事件 (Domain Event), 战术设计, 战略设计。了解思想，实践中逐步应用。 
九、 其他重要理论与知识 (Other Important Theories & Knowledge)
RESTful API 设计与规范: 详见上文架构部分。 
GraphQL: Facebook 开源的 API 查询语言和运行时。 
gRPC: Google 开源的高性能 RPC 框架，基于 HTTP/2 和 Protocol Buffers。 
WebSocket: 全双工通信协议。 
安全相关知识: 认证 (Authentication): Session-Cookie, Token (JWT - JSON Web Token), OAuth 2.0 (授权框架), OpenID Connect (身份认证层)。 
授权 (Authorization): RBAC (Role-Based Access Control), ABAC (Attribute-Based Access Control)。 
常见 Web 攻击与防御: XSS (Cross-Site Scripting), CSRF (Cross-Site Request Forgery), SQL 注入 (SQL Injection), XXE (XML External Entity), 点击劫持 (Clickjacking), 敏感数据泄露。OWASP Top 10。 
密码学基础: 对称加密 (AES), 非对称加密 (RSA), 哈希算法 (MD5, SHA-256, BCrypt, Argon2), 数字签名, HMAC。了解原理和应用场景，不要求自己实现。 
性能优化理论与方法: 性能测试: 负载测试, 压力测试, 并发测试, 稳定性测试。工具: JMeter, Gatling, LoadRunner。 
性能分析方法论: 性能瓶颈定位 (CPU, 内存, 磁盘, 网络, 数据库, 代码逻辑), 性能调优步骤。 
JVM 调优: 详见 JVM 部分。 
SQL 调优: 详见数据库部分。 
应用层调优: 缓存策略, 异步处理, 批处理, 减少数据库交互次数, 优化算法和数据结构。 
单元测试与集成测试: JUnit 5: @Test, @BeforeEach, @AfterEach, @BeforeAll, @AfterAll, @DisplayName, Assertions。 
Mockito: @Mock, @InjectMocks, when().thenReturn(), verify()。 
测试金字塔 (Test Pyramid): 单元测试 (Unit Tests) 占大部分, 集成测试 (Integration Tests) 次之, UI/端到端测试 (E2E Tests) 少量。 
敏捷开发方法论: Scrum, Kanban, XP (Extreme Programming) 的基本概念和实践。 
技术文档阅读与理解能力: 官方文档, API 文档, 技术白皮书, RFC 文档。 
问题排查与解决能力: 日志分析, 断点调试, 网络抓包 (Wireshark, Fiddler, Charles), 系统监控指标分析, 经验积累。 
持续学习意识: 技术更新迭代快，需要保持学习热情和习惯，关注行业动态和技术趋势。 

