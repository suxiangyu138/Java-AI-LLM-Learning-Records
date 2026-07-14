# Java 后端开发工程师必备知识点全攻略
Java 后端开发需构建基础语法-核心框架-中间件-架构设计-工程化的完整知识体系，以下是详细且分层次的知识点梳理，覆盖入门到资深工程师的学习路径：

## 一、 Java 基础核心

### （一） 语法与数据结构
1. 基础语法
    - 变量、数据类型（基本类型/引用类型）、运算符、流程控制（分支  if/switch 、循环  for/while/do-while ）。
    - 面向对象三大特性：封装、继承、多态；抽象类与接口的区别； final 、 static 、 this 、 super  关键字。
    - 异常处理： try-catch-finally 、 throw / throws 、自定义异常；运行时异常 vs 编译时异常。
    - 泛型：泛型类、泛型方法、通配符（ <?> / <? extends T> / <? super T> ）、类型擦除原理。
    - 注解：元注解（ @Target / @Retention  等）、自定义注解、注解解析（反射）。
    - 反射： Class  类获取方式、反射创建对象/调用方法/访问属性、动态代理（JDK 动态代理/CGLIB 动态代理）。
    - 新特性（JDK 8+）：Lambda 表达式、函数式接口（ Function / Consumer / Supplier  等）、Stream 流（中间操作/终止操作）、 Optional  类、接口默认方法/静态方法、JDK 9+ 模块化、JDK 11+ 简化语法（如  var  关键字）。
2. 集合框架
    - 集合体系： Collection （ List / Set / Queue ）、 Map  接口及其实现类。
    - 核心实现类原理：
    -  ArrayList  vs  LinkedList ：底层数组/双向链表、扩容机制、访问/插入性能对比。
    -  HashSet / TreeSet ：基于  HashMap  实现、红黑树排序原理。
    -  HashMap ：JDK 7 数组+链表、JDK 8 数组+链表+红黑树、哈希冲突解决、扩容机制（负载因子 0.75）、 ConcurrentHashMap  并发安全实现（分段锁/CAS+ synchronized ）。
    - 并发集合： CopyOnWriteArrayList 、 ConcurrentLinkedQueue 、 BlockingQueue  及其实现类（ ArrayBlockingQueue / LinkedBlockingQueue ）。
3. IO/NIO
    - BIO（同步阻塞）：字节流（ InputStream / OutputStream ）、字符流（ Reader / Writer ）、缓冲流（ BufferedXXX ）、转换流（ InputStreamReader ）。
    - NIO（同步非阻塞）：三大核心组件（ Channel / Buffer / Selector ）、零拷贝原理、 FileChannel / SocketChannel  用法。
    - AIO（异步非阻塞）： AsynchronousSocketChannel 、回调函数机制。
    - 文件操作： File  类、JDK 7  Path / Files  工具类。

### （二） 多线程与并发编程
1. 线程基础
    - 线程创建方式：继承  Thread 、实现  Runnable 、 Callable + FutureTask 、线程池。
    - 线程生命周期：新建、就绪、运行、阻塞、等待、超时等待、终止；状态转换方法（ start() / sleep() / yield() / join() / interrupt() ）。
    - 守护线程 vs 用户线程；线程优先级； ThreadLocal  原理（线程私有变量、内存泄漏问题及解决）。
2. 同步机制
    - 线程安全问题：原子性、可见性、有序性； volatile  关键字（禁止指令重排、保证可见性）。
    - 锁机制：
    - 隐式锁： synchronized  关键字（修饰方法/代码块、锁升级机制：偏向锁→轻量级锁→重量级锁）。
    - 显式锁： Lock  接口（ ReentrantLock  公平锁/非公平锁、 ReentrantReadWriteLock  读写分离、 StampedLock  乐观读）。
    - 原子类： java.util.concurrent.atomic  包（ AtomicInteger / AtomicReference / AtomicStampedReference  解决 ABA 问题）。
3. 并发工具类
    -  CountDownLatch （倒计时门闩）、 CyclicBarrier （循环栅栏）、 Semaphore （信号量）、 Exchanger （交换器）、 Phaser （阶段器）。
    - 线程池： ThreadPoolExecutor  核心参数、工作流程； Executors  工具类创建线程池（ newFixedThreadPool / newCachedThreadPool  等）；合理配置线程池（CPU 密集型/IO 密集型）；拒绝策略。

## 二、 数据库与数据存储

### （一） 关系型数据库
1. MySQL 核心
    - SQL 语法：DQL（查询  SELECT 、聚合函数  COUNT/SUM/AVG 、分组  GROUP BY 、排序  ORDER BY 、分页  LIMIT ）、DML（增删改  INSERT/UPDATE/DELETE ）、DDL（创建表  CREATE TABLE 、修改表  ALTER TABLE ）、DCL（权限管理  GRANT/REVOKE ）。
    - 存储引擎： InnoDB （事务、行锁、外键、聚簇索引）vs  MyISAM （表锁、全文索引）。
    - 索引：索引类型（主键索引/唯一索引/普通索引/组合索引/全文索引）；索引原理（B+ 树）；索引失效场景（ like %xxx 、函数操作、隐式类型转换）；慢查询优化（ EXPLAIN  执行计划分析）。
    - 事务：ACID 特性；事务隔离级别（读未提交/读已提交/可重复读/串行化）；脏读/不可重复读/幻读及解决。
    - 锁机制：行锁 vs 表锁；乐观锁 vs 悲观锁；间隙锁、临键锁。
    - 分库分表：垂直分表/水平分表；分表策略（哈希分表/范围分表）；中间件（Sharding-JDBC）。
2. Oracle/SQL Server（可选，根据业务需求）
    - Oracle 特有语法（ ROWNUM  分页、序列  SEQUENCE ）；SQL Server 存储过程、触发器。

### （二） 非关系型数据库
1. Redis
    - 数据类型： String / Hash / List / Set / Sorted Set / Bitmap / HyperLogLog / Geo ，各类型应用场景。
    - 核心特性：持久化（RDB/AOF）、过期策略（惰性删除/定期删除）、内存淘汰策略（ volatile-lru  等）、事务（ MULTI/EXEC ）、管道（Pipeline）。
    - 高级特性：主从复制、哨兵模式（高可用）、集群模式（分片）；缓存问题（缓存穿透/缓存击穿/缓存雪崩）及解决方案。
    - Java 客户端：Jedis、Lettuce、Redisson（分布式锁、对象映射）。
2. MongoDB（可选）
    - 文档存储模型；CRUD 操作；索引优化；副本集与分片集群。
3. 其他 NoSQL：Elasticsearch（全文检索）、Memcached（分布式缓存）、HBase（列存数据库）。

### （三） 数据库连接与 ORM 框架
1. JDBC： Connection / Statement / PreparedStatement / ResultSet ；数据库连接池（原理、 Druid / HikariCP  配置与优化）。
2. ORM 框架
    - MyBatis：核心配置（ mybatis-config.xml / Mapper.xml ）；动态 SQL（ if/choose/foreach ）； ResultMap  结果映射；分页插件（PageHelper）；MyBatis-Plus（CRUD 封装、条件构造器）。
    - Hibernate/JPA：实体映射注解（ @Entity / @Table / @Id ）；HQL 语句；一级缓存/二级缓存；JPA 规范与 Spring Data JPA 用法。

## 三、 后端核心框架

### （一） Spring 全家桶
1. Spring Core
    - IoC 容器：控制反转、依赖注入（DI）；Bean 的生命周期；Bean 的作用域（ singleton / prototype  等）；依赖注入方式（构造器注入/Setter 注入/注解注入）。
    - 注解驱动： @Component / @Controller / @Service / @Repository 、 @Autowired / @Resource 、 @Value 。
    - AOP 面向切面编程：核心概念（切面/切点/通知/连接点）；动态代理实现；注解（ @Aspect / @Pointcut / @Before / @After / @Around ）；应用场景（日志/事务/权限控制）。
2. Spring MVC
    - 工作流程：前端控制器  DispatcherServlet 、处理器映射器  HandlerMapping 、处理器适配器  HandlerAdapter 、视图解析器  ViewResolver 。
    - 核心注解： @Controller / @RestController 、 @RequestMapping / @GetMapping / @PostMapping 、 @RequestParam / @PathVariable / @RequestBody / @ResponseBody 。
    - 请求参数绑定：简单参数、对象参数、数组/集合参数、文件上传（ MultipartFile ）。
    - 异常处理： @ControllerAdvice + @ExceptionHandler  全局异常处理。
3. Spring Boot
    - 核心特性：自动配置、起步依赖、嵌入式服务器（Tomcat/Jetty/Undertow）。
    - 配置方式： application.properties / application.yml ；自定义配置；多环境配置（ dev/test/prod ）。
    - 常用功能：Spring Boot Starter（ spring-boot-starter-web / spring-boot-starter-data-jpa  等）；Actuator 监控；热部署（ spring-boot-devtools ）。
4. Spring Transaction
    - 事务管理：编程式事务 vs 声明式事务； @Transactional  注解（传播行为/隔离级别/超时时间/回滚规则）；事务失效场景。
5. Spring Cloud（微服务架构核心）
    - 服务注册与发现：Eureka、Nacos、Consul。
    - 服务调用：Ribbon（负载均衡）、Feign（声明式 HTTP 客户端）、OpenFeign。
    - 服务熔断与降级：Hystrix、Sentinel。
    - 网关：Zuul、Spring Cloud Gateway（路由转发、过滤器）。
    - 配置中心：Spring Cloud Config、Nacos Config。
    - 分布式事务：Seata（AT/TCC/SAGA 模式）。
    - 链路追踪：Sleuth + Zipkin。

### （二） 其他常用框架
1. 日志框架：SLF4J（门面）、Logback、Log4j2；日志级别（ DEBUG / INFO / WARN / ERROR ）；日志配置。
2. JSON 框架：Jackson、FastJSON、Gson；对象与 JSON 互转；日期格式化。
3. 校验框架：Hibernate Validator；注解校验（ @NotNull / @Size / @Email ）；分组校验。

## 四、 中间件与分布式技术

### （一） 消息队列
1. 核心概念：生产者/消费者、队列/交换机、消息持久化、死信队列、延迟队列。
2. 主流 MQ
    - RabbitMQ：AMQP 协议；交换机类型（Direct/Topic/Fanout/Headers）；路由键；消息确认机制（生产者确认/消费者 ACK）。
    - RocketMQ：分布式事务消息；顺序消息；广播消息；高可用架构。
    - Kafka：分区/副本机制；消费者组；offset 管理；高吞吐量原理；应用场景（日志收集、大数据处理）。
3. 应用场景：异步通信、流量削峰、系统解耦、最终一致性。

### （二） 分布式技术
1. 分布式锁：实现方案（Redis 分布式锁、ZooKeeper 分布式锁）；Redisson 分布式锁用法；锁的原子性与可重入性。
2. 分布式 ID：生成方案（UUID、雪花算法 Snowflake、数据库自增、Redis 自增）；Leaf、UidGenerator 等开源工具。
3. 分布式缓存：Redis 集群；缓存一致性（Cache-Aside 模式）；多级缓存（本地缓存 Caffeine + 分布式缓存 Redis）。
4. 分布式事务：2PC/3PC、TCC、SAGA、最终一致性；Seata 框架实战。

### （三） 容器与虚拟化
1. Docker：镜像/容器/仓库；Dockerfile 编写；容器编排（Docker Compose）。
2. Kubernetes（K8s）：核心组件（Pod/Deployment/Service/Ingress）；资源调度；自动扩缩容；配置中心与密钥管理。

## 五、 工程化与运维

### （一） 项目构建与依赖管理
1. Maven/Gradle：POM.xml 配置；依赖范围（ compile / test / provided ）；仓库（本地仓库/中央仓库/私服）；打包插件（ maven-jar-plugin / spring-boot-maven-plugin ）；多模块项目构建。
2. 私服：Nexus 搭建与配置；私服上传/下载依赖。

### （二） 版本控制
1. Git：基本操作（ clone / add / commit / push / pull ）；分支管理（ master / dev / feature / hotfix ）；冲突解决；标签（ tag ）；Git Flow 工作流。
2. 代码托管平台：GitHub/GitLab/Gitee；Pull Request/Merge Request 流程；代码审查。

### （三） 持续集成/持续部署（CI/CD）
1. Jenkins：Pipeline 流水线编写；代码拉取→编译→测试→打包→部署自动化流程；与 Git、Docker 集成。
2. 其他 CI/CD 工具：GitLab CI、GitHub Actions。

### （四） 系统监控与问题排查
1. JVM 监控与调优
    - JVM 内存模型：堆（新生代/老年代）、方法区、虚拟机栈、本地方法栈、程序计数器。
    - GC 算法：标记-清除/标记-复制/标记-整理/分代收集；GC 收集器（SerialGC/ParallelGC/CMS/G1/ZGC）。
    - 调优工具： jps / jstack / jstat / jmap / jconsole / VisualVM ；Arthas 线上诊断。
    - 调优参数：堆大小（ -Xms / -Xmx ）、新生代大小（ -Xmn ）、GC 日志（ -XX:+PrintGCDetails ）。
2. 系统监控
    - 应用监控：Spring Boot Actuator、Micrometer + Prometheus + Grafana。
    - 服务器监控：Zabbix、Nagios；CPU/内存/磁盘/网络监控。
    - 日志收集：ELK 栈（Elasticsearch + Logstash + Kibana）、Filebeat。

### （五） 性能优化
1. 接口优化：接口幂等性设计；限流（令牌桶/漏桶算法）；熔断降级（Sentinel）；异步化（ CompletableFuture ）。
2. 数据库优化：索引优化；SQL 优化；分库分表；读写分离；数据库连接池优化。
3. 网络优化：HTTP 连接复用（Keep-Alive）；压缩传输（Gzip）；CDN 加速。

## 六、 架构设计与软实力

### （一） 架构设计
1. 架构模式：MVC 模式、分层架构、微服务架构、分布式架构、SOA 架构、DDD（领域驱动设计）。
2. 设计原则：SOLID 原则（单一职责/开闭原则/里氏替换/接口隔离/依赖倒置）；高内聚低耦合。
3. 设计模式：创建型（单例/工厂/建造者/原型）、结构型（代理/装饰器/适配器/组合）、行为型（观察者/策略/模板方法/责任链）；实战应用场景。
4. 高可用设计：服务冗余、故障转移、限流熔断、降级兜底、数据备份与恢复。
5. 高并发设计：无状态服务、缓存优化、异步处理、队列削峰、分布式锁。

### （二） 软实力
1. 技术文档编写：接口文档（Swagger/OpenAPI）、设计文档、用户手册。
2. 问题排查能力：日志分析、异常定位、性能瓶颈排查。
3. 学习能力：跟踪技术前沿（Spring 新版本、云原生、微服务）；阅读源码（Spring、MyBatis、Redis）。
4. 协作能力：跨团队沟通（前端/测试/运维）；敏捷开发流程（Scrum）。

## 七、 扩展技术栈（按需学习）
1. 大数据相关：Hadoop、Spark、Flink；数据仓库、ETL 工具。
2. 搜索引擎：Elasticsearch；全文检索、分词器、聚合分析。
3. 微服务网关进阶：Kong、APISIX。
4. 云原生技术：Service Mesh（Istio）、Serverless、云平台（阿里云/腾讯云/AWS）。
5. 安全技术：HTTPS 配置、接口加密（RSA/AES）、签名验证、XSS/CSRF/SQL 注入防护。
