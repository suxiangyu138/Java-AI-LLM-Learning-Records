# Java后端面试八股文核心考点（按高频程度排序）

---

## 一、Java基础（必考，占比约20%）
1. 基础语法
   - 数据类型：基本类型vs包装类、自动装箱/拆箱、常量池（Integer缓存-128~127）；
   - 修饰符：public/private/protected/default、static/final/abstract/volatile/synchronized；
   - 字符串：String/StringBuffer/StringBuilder区别、String不可变原因、intern()方法；
   - 异常：异常体系（Checked/Unchecked）、try-catch-finally执行顺序、finally必执行吗？
   - 集合：Collection体系（List/Set/Map）、ArrayList&LinkedList区别、HashMap底层原理（JDK7 vs JDK8）、HashSet实现、ConcurrentHashMap线程安全机制。
2. 面向对象
   - 三大特性：封装、继承、多态（静态/动态多态）；
   - 接口vs抽象类：区别、使用场景；
   - 重写vs重载：定义、区别；
   - 设计模式：单例（饿汉/懒汉/枚举/双重校验锁）、工厂、代理（静态/动态）、观察者，重点问单例和代理。
3. JVM（高频核心）
   - 内存模型：堆/方法区/虚拟机栈/本地方法栈/程序计数器、各区域作用及OOM场景；
   - 垃圾回收：GC算法（标记清除/标记整理/复制）、垃圾收集器（CMS/G1/ZGC）、可达性分析、强/软/弱/虚引用；
   - 类加载：类加载过程（加载/验证/准备/解析/初始化）、双亲委派模型、自定义类加载器；
   - 调优：JVM参数（Xms/Xmx/Xmn）、内存泄漏排查、GC日志分析。

---

## 二、Java进阶（高频，占比约25%）
1. 并发编程（核心难点）
   - 线程基础：创建线程方式（继承Thread/实现Runnable/Callable+Future）、线程状态（新建/就绪/运行/阻塞/死亡）；
   - 同步机制：synchronized（对象锁/类锁、底层monitor）、Lock（ReentrantLock）、synchronized vs Lock区别；
   - 线程池：ThreadPoolExecutor核心参数（核心线程数/最大线程数/队列/拒绝策略）、常见线程池（FixedThreadPool/CachedThreadPool）、为什么不用Executors创建；
   - 原子类：AtomicInteger底层（CAS）、CASABA问题、AtomicReference；
   - 并发工具：CountDownLatch/CyclicBarrier/Semaphore、ThreadLocal（原理、内存泄漏原因）；
   - 死锁：产生条件、避免方式、排查死锁。
2. IO/NIO
   - BIO/NIO/AIO区别、适用场景；
   - NIO核心：Buffer/Channel/Selector、零拷贝；
   - 文件操作：InputStream/OutputStream、Reader/Writer。
3. 新特性（Java8+）
   - Lambda表达式、函数式接口（Consumer/Supplier/Function/Predicate）；
   - Stream流：中间操作/终止操作、并行流；
   - Optional：解决空指针；
   - CompletableFuture：异步编程。

---

## 三、框架（核心考点，占比约30%）
1. Spring
   - IOC容器：Bean的生命周期、依赖注入方式（构造器/setter/注解）、Bean作用域（singleton/prototype）；
   - AOP：原理（动态代理）、通知类型（前置/后置/环绕/异常/最终）、切入点表达式、应用场景（事务/日志/权限）；
   - 注解：@Autowired/@Resource区别、@Component/@Service/@Controller/@Repository；
   - 事务：声明式事务（@Transactional）、事务传播机制、隔离级别、事务失效场景；
   - SpringMVC：执行流程（DispatcherServlet→HandlerMapping→HandlerAdapter→Controller→ViewResolver）、参数绑定、拦截器（Interceptor）。
2. MyBatis
   - 核心原理：Mapper接口如何生成代理对象、SqlSessionFactory/SqlSession；
   - 映射配置：XML映射（resultMap/parameterMap）、注解映射、#{ } vs ${ }区别（防SQL注入）；
   - 缓存：一级缓存（SqlSession级别）、二级缓存（Mapper级别）；
   - 分页：PageHelper原理、自定义分页；
   - 动态SQL：if/where/foreach/choose。
3. Spring Boot/Spring Cloud
   - Spring Boot：自动配置原理、starter机制、核心注解（@SpringBootApplication）、配置文件（application.yml/properties）；
   - Spring Cloud：微服务核心组件（Nacos/Eureka注册中心、OpenFeign远程调用、Gateway网关、Sentinel限流、Ribbon负载均衡、Feign和OpenFeign区别）。

---

## 四、数据库（必考，占比约15%）
1. MySQL
   - 存储引擎：InnoDB vs MyISAM（事务/锁/索引）；
   - 索引：索引类型（主键/唯一/普通/联合/全文）、B+树索引原理、索引失效场景、最左前缀原则；
   - 事务：ACID原则、隔离级别（读未提交/读已提交/可重复读/串行化）、MVCC、脏读/幻读/不可重复读；
   - SQL优化：慢查询优化、EXPLAIN分析、避免SELECT *、分页优化（LIMIT偏移量大问题）；
   - 锁：行锁/表锁、乐观锁/悲观锁、死锁避免。
2. 非关系型数据库（Redis为主）
   - 数据类型：String/Hash/List/Set/ZSet及应用场景；
   - 持久化：RDB/AOF区别、混合持久化；
   - 缓存问题：缓存穿透/缓存击穿/缓存雪崩及解决方案、缓存更新策略（Cache Aside）；
   - 分布式锁：Redis实现分布式锁（SETNX+EXPIRE、Redisson）；
   - 集群：主从复制、哨兵、分片集群。

---

## 五、中间件&分布式（中高频，占比约8%）
1. 消息队列（MQ）
   - 核心：消息队列作用（解耦/异步/削峰）、Kafka/RabbitMQ/RocketMQ区别；
   - 可靠性：消息丢失/重复消费/顺序消费解决方案；
   - 核心概念：生产者/消费者、主题/队列、分区、offset、ACK机制。
2. 分布式
   - 分布式锁：Redis/Zookeeper实现；
   - 分布式事务：2PC/TCC/SAGA/本地消息表/最终一致性；
   - 分布式ID：雪花算法、UUID、数据库自增；
   - 一致性算法：CAP理论、BASE理论、Paxos/Raft（ZAB）。
3. 其他中间件
   - Nginx：反向代理、负载均衡算法、动静分离；
   - Zookeeper：核心功能（配置中心/注册中心/分布式锁）、ZAB协议、Watcher机制。

---

## 六、项目&工程化（必考，占比约2%）
1. 项目经验
   - 核心：项目架构、负责模块、技术难点及解决方案、性能优化点；
   - 高频问题：如何做接口限流、如何保证接口幂等性、如何处理高并发场景。
2. 工程化
   - 构建工具：Maven/Gradle（依赖管理、打包、私服）；
   - 版本控制：Git（分支管理、冲突解决、rebase vs merge）；
   - 部署：Docker容器化、CI/CD、Tomcat/Nginx部署。

---

## 七、网络&操作系统（低频，占比约0%）
1. 计算机网络
   - HTTP/HTTPS：状态码、请求方法、HTTPS加密过程（SSL/TLS）、HTTP1.1/2.0/3.0区别；
   - TCP/UDP：区别、TCP三次握手/四次挥手、拥塞控制、粘包拆包；
   - RESTful API：设计规范。
2. 操作系统
   - 进程/线程/协程：区别；
   - 内存管理：分页/分段、虚拟内存；
   - 锁机制：互斥锁/自旋锁。

---

## 八、核心总结
1. 高频核心：Java基础（集合/JVM）、并发编程、Spring（IOC/AOP/事务）、MyBatis、MySQL（索引/事务/优化）、Redis（数据类型/缓存问题）；
2. 进阶考点：Spring Boot/Cloud、MQ、分布式（锁/事务/ID）；
3. 面试策略：先掌握基础和框架核心，再攻克并发、JVM、分布式难点，最后补充中间件和网络知识；
4. 关键技巧：每个考点不仅要记概念，还要结合项目场景说应用（如HashMap在项目中用在哪、Redis缓存雪崩如何解决）。

---
