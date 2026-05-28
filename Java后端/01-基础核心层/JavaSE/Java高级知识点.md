## 整体能力版图（先有全局观）

对标高级 Java 开发工程师，主线一般包含：JVM、并发编程、I/O & NIO、框架原理（Spring 全家桶）、数据库与 Redis、分布式与微服务、性能与故障排查等。 [processon](https://www.processon.com/view/67ffa15ae6ad2b0bea1abbc2)

一个常见的高级 Java 知识体系大致会包括这些模块： [developer.aliyun](https://developer.aliyun.com/article/619264)

- 语言进阶：泛型、反射、注解、序列化、集合源码  
- JVM：内存模型、垃圾回收、类加载、调优工具  
- 并发编程：线程模型、锁、并发容器、原子类、CAS  
- I/O & NIO：BIO/NIO/AIO、网络编程、零拷贝  
- Web & 框架：Servlet、Spring/Spring MVC/Spring Boot 核心原理  
- 数据库与中间件：MySQL 设计与 SQL 优化、Redis、消息队列  
- 分布式与微服务：注册中心、配置中心、限流熔断、CAP、负载均衡  
- 工程能力：Linux 基础、Shell、Git、性能压测与监控  

***

## Java 语言进阶与 JVM

### 1. 高级语言特性与源码视角

这些是从“会用 Java”升级到“理解 Java 工作方式”的关键： [javaguide](https://javaguide.cn/java/basis/java-basic-questions-01.html)

- 集合框架深入  
  - ArrayList / LinkedList / HashMap / ConcurrentHashMap 的底层结构与时间复杂度  
  - fail-fast 机制、扩容机制、红黑树化条件（HashMap 8u 之后）  
- 泛型  
  - 擦除机制、泛型方法、上下界通配符、为什么运行时不能拿到泛型具体类型  
- 反射与注解  
  - Class 对象、Method/Field 操作、动态代理（JDK/CGLIB）  
  - 自定义注解 + 运行时解析（这是理解 Spring IOC/AOP 的基础）  
- 序列化  
  - JDK 序列化的问题、serialVersionUID、transient  
  - 以及为什么生产上更常用 JSON/Protobuf 等  

### 2. JVM 核心知识点

JVM 是高级 Java 的“考点之王”： [github](https://github.com/loversgzl/Learning/blob/master/notes/JAVA/JAVA-%E9%AB%98%E7%BA%A7%E7%BC%96%E7%A8%8B.md)

- 运行时内存结构  
  - 堆、栈、方法区（元空间）、程序计数器、本地方法栈  
- 垃圾回收与内存分配策略  
  - 新生代 / 老年代、Eden/Survivor、Minor GC / Major GC / Full GC  
  - 常见 GC 算法：标记-清除、标记-整理、复制、分代收集  
  - 常见收集器（Serial / Parallel / CMS / G1 的特点与适用场景）  
- 类加载机制  
  - 双亲委派模型、类加载器种类（BootStrap/Ext/App/自定义）  
  - 破坏双亲委派的典型案例（SPI、Tomcat、OSGi 思路）  
- JVM 调优与排查  
  - 常用参数（堆大小、元空间大小、GC 日志开关）  
  - jps、jstack、jmap、jstat 等工具的用途  
  - 使用可视化工具（如 JVisualVM、JMC）做内存与线程分析  

***

## 并发编程与多线程

并发编程是从“中级”走向“高级”的分水岭： [blog.csdn](https://blog.csdn.net/m0_55400356/article/details/121862441)

- Java 内存模型（JMM）  
  - 可见性、有序性、原子性  
  - happens-before 规则、重排序、volatile 语义  
- 线程基础  
  - 线程生命周期、创建方式（继承 Thread / 实现 Runnable / Callable + Future）  
  - 线程池（ThreadPoolExecutor 核心参数、拒绝策略、队列类型）  
- 同步与锁  
  - synchronized 底层原理（偏向锁、轻量级锁、重量级锁概念）  
  - ReentrantLock、Condition、读写锁、StampedLock 的适用场景  
- 并发工具类与容器  
  - AtomicXXX 原子类与 CAS 原理  
  - CountDownLatch、CyclicBarrier、Semaphore、FutureTask  
  - ConcurrentHashMap、CopyOnWriteArrayList 等并发集合内部策略  

***

## I/O、NIO 与网络编程

为后面学 Netty、RPC 框架打地基： [cnblogs](https://www.cnblogs.com/javastack/p/12966261.html)

- BIO / NIO / AIO 模型区别  
  - 阻塞 vs 非阻塞、同步 vs 异步 的组合关系  
- Java NIO 组件  
  - Buffer、Channel、Selector 的使用方式与工作机制  
- 零拷贝与高性能网络通信思想  
  - 文件传输、磁盘 I/O 优化策略基础认知  

***

## Web & Spring 体系的“原理向”掌握

### 1. Servlet 与 Web 基础

理解 Web 底层是理解 Spring MVC 必经之路： [processon](https://www.processon.com/view/67ffa15ae6ad2b0bea1abbc2)

- HTTP 协议基础（请求、响应、状态码、常见请求头与响应头）  
- Servlet 生命周期、Filter 与 Listener 用途  
- 会手写一个简单的登录/过滤/权限校验链路  

### 2. Spring / Spring MVC / Spring Boot

“高级 Java 后端”基本都要求你理解这些框架的核心原理： [developer.aliyun](https://developer.aliyun.com/article/619264)

- Spring 核心  
  - IOC 容器：BeanDefinition、BeanFactory、ApplicationContext、Bean 生命周期  
  - AOP：代理模式、切点、通知、JDK 动态代理 & CGLIB 的差别  
- Spring MVC  
  - 请求处理流程：DispatcherServlet → HandlerMapping → HandlerAdapter → Controller → ViewResolver  
  - 参数绑定、拦截器、异常处理机制  
- Spring Boot  
  - 自动配置原理（@SpringBootApplication、@EnableAutoConfiguration）  
  - 启动流程、外部化配置、Starter 的设计思路  

***

## 数据库、Redis 与中间件

### 1. MySQL 高级

只会写 CRUD 远远不够，对标高级要做到“能设计 + 能优化”： [cnblogs](https://www.cnblogs.com/javastack/p/12966261.html)

- 表设计与范式（1NF/2NF/3NF）与适度反范式  
- 索引原理  
  - B+ 树、聚簇索引与二级索引、覆盖索引、索引失效的常见场景  
- SQL 性能优化  
  - EXPLAIN 关键字段解读（type、key、rows、extra）  
  - 常见慢 SQL 模式（模糊查询、函数计算、隐式转换、OR 太多）  

### 2. Redis 与缓存体系

Redis 基本是 Java 后端的必备： [processon](https://www.processon.com/view/67ffa15ae6ad2b0bea1abbc2)

- 核心数据结构与典型使用场景（String/Hash/List/Set/ZSet/Bitmap/HyperLogLog）  
- 缓存穿透、击穿、雪崩 的概念与解决手段  
- Redis 持久化（RDB/AOF）、主从、哨兵、集群的基础原理  

### 3. 消息队列（MQ）基础

为以后做分布式、削峰填谷做准备： [developer.aliyun](https://developer.aliyun.com/article/619264)

- 使用场景：异步解耦、流量削峰、最终一致性  
- 常见 MQ（如 RabbitMQ、Kafka）的基本概念：生产者、消费者、主题、分区、offset 等  

***

## 分布式与微服务（为高级后端预热）

对标高级工程师，中大型公司几乎都涉及分布式/微服务： [javabetter](https://javabetter.cn/xuexiluxian/java/yitiaolong.html)

- 分布式基础理论  
  - CAP、BASE 思想、强一致 / 最终一致概念  
- 微服务基础组件  
  - 注册中心（如 Nacos）、配置中心、API 网关、服务调用与负载均衡  
  - 链路追踪、限流熔断（Hystrix/Resilience4j 类似思想）  
- 常见分布式问题  
  - 分布式锁（基于 Redis / Zookeeper 的实现思路）  
  - 全局唯一 ID（雪花算法、数据库自增、Redis、自研 ID 服务）  
  - 分布式事务的常见方案（TCC、可靠消息、最大努力通知思想）  

***

## 工程实践能力（Linux、Shell、Git）

高级 Java 不只是写代码，还要有较强的工程与排障能力： [cnblogs](https://www.cnblogs.com/javastack/p/12966261.html)

- Linux 基础  
  - 常用命令（文件、网络、进程）、服务启动、日志查看  
- Shell 脚本基础  
  - 简单部署脚本、定时任务脚本，注意避免高危操作（如 rm -rf）  
- Git 与项目协作  
  - 分支模型（Git Flow/Trunk Based）、常见命令与多人协作流程  

***

## 学习建议 & 提问

结合你“Java 后端 + AI 应用 + 想走合肥稳定中产”的路线，可以按模块规划学习顺序：  
1）先补强：Java 语言进阶 + 集合源码 + JVM 基础  
2）然后：并发编程 + Spring 原理 + MySQL + Redis  
3）再上：分布式与微服务 + 工程化（Linux/监控/排障）  
