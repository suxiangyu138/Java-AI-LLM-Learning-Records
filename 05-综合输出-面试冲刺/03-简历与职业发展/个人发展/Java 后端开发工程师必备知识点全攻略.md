# Java 后端开发工程师必备知识点全攻略

## 概述

本文按六层知识体系 + 扩展技术栈，系统梳理 Java 后端开发工程师从入门到资深需要掌握的全部知识点，覆盖 **Java 基础、数据库存储、核心框架、中间件与分布式、工程化运维、架构设计**，适用于学习规划与面试复习。

---

## 知识体系全景图

```
        ┌─────────────────────────────────────┐
        │    六、架构设计与软实力（顶层）        │
        ├─────────────────────────────────────┤
        │    五、工程化与运维（交付层）           │
        ├─────────────────────────────────────┤
        │    四、中间件与分布式技术（能力层）      │
        ├─────────────────────────────────────┤
        │    三、后端核心框架（骨架层）           │
        ├─────────────────────────────────────┤
        │    二、数据库与数据存储（数据层）        │
        ├─────────────────────────────────────┤
        │    一、Java 基础核心（地基）            │
        └─────────────────────────────────────┘
```

---

## 一、Java 基础核心

### 1.1 语法与数据结构

| 模块 | 核心知识点 |
|------|-----------|
| **基础语法** | 变量、数据类型、运算符、流程控制（if/switch/for/while）；OOP 三大特性（封装/继承/多态）；抽象类 vs 接口；final/static/this/super |
| **异常处理** | try-catch-finally、throw/throws、自定义异常；运行时异常 vs 编译时异常 |
| **泛型** | 泛型类/方法、通配符（`<?>` / `<? extends T>` / `<? super T>`）、类型擦除原理 |
| **注解** | 元注解（`@Target`/`@Retention`）、自定义注解、注解解析（反射） |
| **反射** | Class 获取方式、反射创建对象/调用方法/访问属性；JDK 动态代理 vs CGLIB 代理 |
| **JDK 8+ 新特性** | Lambda 表达式、函数式接口（Function/Consumer/Supplier）、Stream 流、Optional、接口默认方法；JDK 11+ var 关键字 |

### 1.2 集合框架

| 集合 | 底层结构 | 核心要点 |
|------|---------|---------|
| **ArrayList** | 数组 | 扩容机制（1.5 倍）、随机访问快 |
| **LinkedList** | 双向链表 | 插入删除快、实现 Deque 接口 |
| **HashMap** | JDK 7：数组+链表；JDK 8：数组+链表+红黑树 | 负载因子 0.75、哈希冲突、扩容机制 |
| **ConcurrentHashMap** | JDK 7：分段锁；JDK 8：CAS+synchronized | 线程安全、不允许 null |
| **HashSet** | 基于 HashMap | 元素唯一 |
| **TreeSet** | 红黑树 | 有序、元素可排序 |
| **并发集合** | CopyOnWriteArrayList / ConcurrentLinkedQueue / BlockingQueue | 读写分离、无界/有界队列 |

### 1.3 IO/NIO

| 模型 | 特点 | 核心组件 |
|------|------|---------|
| **BIO**（同步阻塞） | 一个连接一个线程 | InputStream/OutputStream、Reader/Writer |
| **NIO**（同步非阻塞） | 一个线程处理多个连接 | Channel/Buffer/Selector、零拷贝 |
| **AIO**（异步非阻塞） | 回调机制 | AsynchronousSocketChannel |

### 1.4 多线程与并发编程

| 模块 | 核心知识点 |
|------|-----------|
| **线程基础** | 创建方式（Thread/Runnable/Callable+FutureTask/线程池）；生命周期（7 种状态）；守护线程 vs 用户线程；ThreadLocal 原理与内存泄漏 |
| **同步机制** | volatile（可见性/禁止指令重排）；synchronized（锁升级：偏向→轻量级→重量级）；Lock（ReentrantLock/ReadWriteLock/StampedLock） |
| **原子类** | AtomicInteger/AtomicReference/AtomicStampedReference（解决 ABA 问题） |
| **并发工具类** | CountDownLatch/CyclicBarrier/Semaphore/Exchanger/Phaser |
| **线程池** | ThreadPoolExecutor 七大参数、工作流程、拒绝策略；CPU 密集型 vs IO 密集型配置 |

---

## 二、数据库与数据存储

### 2.1 MySQL 核心

| 领域 | 核心知识点 |
|------|-----------|
| **SQL** | DQL（SELECT/聚合函数/GROUP BY/ORDER BY/LIMIT）、DML（INSERT/UPDATE/DELETE）、DDL（CREATE/ALTER）、DCL（GRANT/REVOKE） |
| **存储引擎** | InnoDB（事务/行锁/外键/聚簇索引）vs MyISAM（表锁/全文索引） |
| **索引** | B+ 树原理；聚簇 vs 非聚簇；组合索引/最左前缀；索引失效场景（like %xxx/函数操作/隐式类型转换）；EXPLAIN 执行计划 |
| **事务** | ACID 特性、隔离级别（读未提交/读已提交/可重复读/串行化）、脏读/不可重复读/幻读 |
| **锁机制** | 行锁 vs 表锁；乐观锁 vs 悲观锁；间隙锁/临键锁 |
| **分库分表** | 垂直分表/水平分表；哈希分表/范围分表；ShardingSphere-JDBC |

### 2.2 非关系型数据库

| 数据库 | 类型 | 核心知识点 |
|--------|------|-----------|
| **Redis** | 键值存储 | String/Hash/List/Set/ZSet/Bitmap/HyperLogLog/Geo；RDB/AOF 持久化；惰性删除/定期删除；8 种内存淘汰策略；主从/哨兵/Cluster；缓存穿透/击穿/雪崩 |
| **MongoDB** | 文档存储 | 文档模型、聚合管道、副本集、分片集群 |
| **Elasticsearch** | 搜索引擎 | 倒排索引、DSL 查询、IK 分词、聚合分析 |
| **Memcached** | 分布式缓存 | 简单 KV 缓存，多线程模型 |
| **HBase** | 列存储 | 基于 HDFS，海量结构化数据 |

### 2.3 数据库连接与 ORM

| 技术 | 核心要点 |
|------|---------|
| **JDBC** | Connection/Statement/PreparedStatement/ResultSet；连接池（Druid/HikariCP） |
| **MyBatis** | Mapper.xml/动态 SQL/ResultMap；分页插件（PageHelper）；MyBatis-Plus（CRUD 封装/条件构造器） |
| **Hibernate/JPA** | 实体映射注解（`@Entity`/`@Table`）、HQL、一级/二级缓存、JPA 规范 |

---

## 三、后端核心框架

### 3.1 Spring 全家桶

| 模块 | 核心知识点 |
|------|-----------|
| **Spring Core** | IoC/DI、Bean 生命周期与作用域、注解驱动（`@Component`/`@Autowired`/`@Value`） |
| **AOP** | 切面/切点/通知/连接点；JDK 动态代理 vs CGLIB；`@Aspect`/`@Around`；应用场景（日志/事务/权限） |
| **Spring MVC** | DispatcherServlet → HandlerMapping → HandlerAdapter → ViewResolver 全链路 |
| **Spring Boot** | 自动配置、Starter、Actuator、application.yml、多环境配置、热部署 |
| **Spring Transaction** | 声明式 vs 编程式事务；`@Transactional` 传播行为（7 种）/隔离级别/回滚规则/失效场景 |
| **Spring Security** | 认证（JWT/OAuth2）、授权（RBAC）、CSRF/CORS 防护 |

### 3.2 Spring Cloud 微服务组件

| 组件 | 技术选项 | 核心功能 |
|------|---------|---------|
| 注册中心 | Eureka / Nacos / Consul | 服务注册与发现 |
| 配置中心 | Spring Cloud Config / Nacos Config / Apollo | 统一配置管理 |
| 服务调用 | Ribbon / Feign / OpenFeign | 负载均衡 + 声明式 HTTP 调用 |
| 熔断降级 | Hystrix → Sentinel / Resilience4j | 服务熔断、限流、降级 |
| 网关 | Zuul / Spring Cloud Gateway | 路由转发、过滤器 |
| 分布式事务 | Seata（AT/TCC/SAGA） | 跨服务数据一致性 |
| 链路追踪 | Sleuth + Zipkin / SkyWalking | 调用链追踪 |

### 3.3 其他常用框架

| 类别 | 技术 |
|------|------|
| **日志** | SLF4J（门面）+ Logback / Log4j2 |
| **JSON** | Jackson / FastJSON / Gson |
| **校验** | Hibernate Validator（`@NotNull`/`@Size`/`@Email`/分组校验） |

---

## 四、中间件与分布式技术

### 4.1 消息队列

| 维度 | RabbitMQ | Kafka | RocketMQ |
|------|---------|-------|----------|
| **协议** | AMQP | 自定义 | 自定义 |
| **核心概念** | Exchange/Queue/Binding | Topic/Partition/Consumer Group | NameServer/Topic/Tag |
| **核心能力** | 死信队列/延迟队列 | 高吞吐/Exactly-Once/Streams | 事务消息/顺序消息 |
| **适用场景** | 业务解耦、可靠消息 | 日志采集、流处理 | 电商交易、金融 |

### 4.2 分布式核心技术

| 领域 | 核心技术方案 |
|------|------------|
| **分布式锁** | Redis（SET NX + Redisson 看门狗）、ZooKeeper（临时顺序节点） |
| **分布式 ID** | UUID、雪花算法（Snowflake）、数据库自增、Redis 自增、Leaf/UidGenerator |
| **分布式缓存** | Redis Cluster、多级缓存（Caffeine + Redis）、Cache-Aside 模式 |
| **分布式事务** | 2PC/3PC、TCC、SAGA、最终一致性；Seata 框架 |

### 4.3 容器与编排

| 技术 | 核心要点 |
|------|---------|
| **Docker** | 镜像/容器/仓库、Dockerfile 编写、Docker Compose 编排 |
| **Kubernetes** | Pod/Deployment/Service/Ingress/ConfigMap/Secret、HPA 自动扩缩容 |

---

## 五、工程化与运维

### 5.1 项目构建与版本控制

| 领域 | 核心技术点 |
|------|-----------|
| **Maven/Gradle** | POM 配置、依赖范围（compile/test/provided）、多模块构建、Nexus 私服 |
| **Git** | clone/add/commit/push/pull、分支管理（master/dev/feature/hotfix）、Git Flow、Pull Request |
| **CI/CD** | Jenkins Pipeline、GitLab CI、GitHub Actions（拉取→编译→测试→打包→部署） |

### 5.2 系统监控与性能优化

| 领域 | 核心技术点 |
|------|-----------|
| **JVM 调优** | 内存模型（堆/栈/方法区）、GC 算法与收集器（Serial/Parallel/CMS/G1/ZGC）、调优工具（jps/jstack/jstat/jmap/Arthas） |
| **应用监控** | Spring Boot Actuator、Micrometer + Prometheus + Grafana |
| **日志收集** | ELK 栈（ES + Logstash + Kibana）、Filebeat |
| **接口优化** | 幂等性设计、限流（令牌桶/漏桶）、熔断降级（Sentinel）、异步化（CompletableFuture） |
| **数据库优化** | 索引优化、SQL 优化、分库分表、读写分离、连接池调优 |

---

## 六、架构设计与软实力

### 6.1 架构设计

| 领域 | 核心内容 |
|------|---------|
| **架构模式** | MVC、分层架构、微服务、分布式、SOA、DDD 领域驱动设计 |
| **设计原则** | SOLID（单一职责/开闭/里氏替换/接口隔离/依赖倒置）+ 高内聚低耦合 |
| **设计模式** | 创建型（单例/工厂/建造者）、结构型（代理/装饰器/适配器）、行为型（观察者/策略/模板方法/责任链） |
| **高可用** | 服务冗余、故障转移、限流熔断、降级兜底、数据备份与恢复 |
| **高并发** | 无状态服务、缓存优化、异步处理、队列削峰、分布式锁 |

### 6.2 软实力

| 能力 | 要求 |
|------|------|
| **文档编写** | 接口文档（Swagger/OpenAPI）、设计文档、用户手册 |
| **问题排查** | 日志分析、异常定位、性能瓶颈排查 |
| **持续学习** | 阅读源码（Spring/MyBatis/Redis）、跟踪技术前沿 |
| **团队协作** | 跨团队沟通（前端/测试/运维）、敏捷开发（Scrum） |

---

## 七、扩展技术栈（按需学习）

| 方向 | 技术 |
|------|------|
| **大数据** | Hadoop、Spark、Flink、数据仓库、ETL |
| **搜索引擎** | Elasticsearch 全文检索、分词器、聚合分析 |
| **网关进阶** | Kong、APISIX |
| **云原生** | Service Mesh（Istio）、Serverless、云平台（阿里云/腾讯云/AWS） |
| **安全** | HTTPS、RSA/AES 加密、签名验证、XSS/CSRF/SQL 注入防护 |

---

## 八、技能掌握度自查

| 模块 | 了解 | 会用 | 熟练 | 精通 |
|------|:---:|:---:|:---:|:---:|
| Java SE 核心语法 | | | | |
| 集合框架（HashMap/JUC） | | | | |
| 多线程/并发编程 | | | | |
| JVM 内存模型与调优 | | | | |
| MySQL 索引/事务/锁 | | | | |
| Redis 缓存策略/集群 | | | | |
| Spring Boot/Spring MVC | | | | |
| Spring Cloud 微服务 | | | | |
| MQ（RabbitMQ/Kafka） | | | | |
| Docker/K8s | | | | |

---

*最后更新：2026-07-15*
