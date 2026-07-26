# Java 后端开发核心技能体系

## 概述

本文系统梳理 Java 后端开发工程师必备的核心技能体系，覆盖 **Java 基础、Spring 全家桶、持久层、数据库、缓存、中间件、网络、工程化、设计模式、高频面试题** 十大模块。适用于初中级至中级后端工程师的能力自检与学习规划。

---

## 一、Java 基础

### 1.1 集合框架

| 类 | 需掌握的核心点 |
|----|--------------|
| **HashMap** | 底层数据结构（数组+链表+红黑树）、扩容机制、put/get 流程、为什么线程不安全 |
| **ConcurrentHashMap** | 分段锁 → CAS + synchronized 的演进、sizeCtl 含义、扩容时的多线程协作 |
| **ArrayList** | 底层数组、扩容机制（1.5 倍）、fail-fast 机制、与 LinkedList 的选型对比 |
| **LinkedList** | 双向链表结构、适合频繁增删的场景 |
| **HashSet** | 基于 HashMap 实现、add 方法的底层逻辑 |

### 1.2 JVM

| 知识点 | 核心内容 |
|--------|---------|
| 内存模型 | 堆（年轻代/老年代）、栈（虚拟机栈/本地方法栈）、方法区/元空间、程序计数器 |
| 垃圾回收 | 判断算法（引用计数/可达性分析）、GC 算法（标记-清除/标记-整理/复制）、常见回收器（Serial/Parallel/CMS/G1/ZGC） |
| 类加载 | 加载→验证→准备→解析→初始化、双亲委派模型、打破双亲委派的场景 |

### 1.3 异常体系

| 知识点 | 核心内容 |
|--------|---------|
| 异常分类 | Throwable → Error（不可处理）/ Exception（可处理）；RuntimeException（非受检）/ 其他 Exception（受检） |
| 异常处理 | try-catch-finally 执行顺序、return 在 finally 中的行为、try-with-resources |

### 1.4 多线程与并发

| 知识点 | 核心内容 |
|--------|---------|
| 线程基础 | Thread、Runnable、Callable、Future、线程生命周期（6 种状态） |
| 线程池 | ThreadPoolExecutor 七大参数、四种拒绝策略、常用线程池（Fixed/Cached/Single/Scheduled） |
| 锁机制 | synchronized（锁升级：偏向锁→轻量级锁→重量级锁）、volatile（可见性/禁止指令重排）、Lock（ReentrantLock/AQS） |
| CAS | 原理、ABA 问题及解决方案、Atomic 原子类 |
| ThreadLocal | 原理（ThreadLocalMap）、内存泄漏问题与解决 |

### 1.5 高级特性

| 特性 | 核心内容 |
|------|---------|
| 泛型 | 类型擦除、通配符（? extends / ? super）、泛型方法 |
| 反射 | Class 对象获取、动态创建实例与调用方法、反射的性能影响 |
| 注解 | 元注解、自定义注解、注解处理器（编译时/运行时） |
| Lambda & Stream | 函数式接口、Stream 中间操作与终止操作、并行流 |

---

## 二、Spring 全家桶

### 2.1 核心原理

| 知识点 | 必须掌握的内容 |
|--------|--------------|
| IoC 容器 | 控制反转概念、依赖注入方式（构造器/Setter/字段）、ApplicationContext 与 BeanFactory 区别 |
| AOP | 动态代理（JDK 动态代理 vs CGLIB）、切面/切点/通知/连接点概念、AOP 实现场景（日志/事务/权限） |
| Bean 生命周期 | 实例化 → 属性填充 → Aware 回调 → 前置处理 → 初始化 → 后置处理 → 使用 → 销毁 |
| 事务管理 | 事务传播机制（7 种）、隔离级别（5 种）、事务失效的常见场景 |
| Spring MVC | DispatcherServlet 完整请求处理流程（9 大组件）、拦截器与过滤器的区别 |

### 2.2 Spring Boot

| 知识点 | 核心内容 |
|--------|---------|
| 自动配置 | `@SpringBootApplication` 组合注解原理、`@EnableAutoConfiguration` 的 SPI 机制、条件注解（`@ConditionalOnXxx`） |
| 起步依赖 | starter 机制、版本仲裁、自定义 starter |

### 2.3 核心注解速查

| 分类 | 注解 |
|------|------|
| 组件声明 | `@Controller` / `@Service` / `@Repository` / `@Component` |
| 依赖注入 | `@Autowired` / `@Qualifier` / `@Value` |
| 配置类 | `@Configuration` / `@Bean` |
| 事务 | `@Transactional` |
| REST 接口 | `@RestController` / `@RequestMapping` / `@GetMapping` / `@PostMapping` / `@PutMapping` / `@DeleteMapping` |
| 参数绑定 | `@RequestParam` / `@PathVariable` / `@RequestBody` / `@RequestHeader` |

---

## 三、持久层框架：MyBatis / MyBatis-Plus

| 知识点 | 核心内容 |
|--------|---------|
| 核心原理 | SqlSession 执行流程、Mapper 代理机制、插件拦截器原理 |
| SQL 相关 | 动态 SQL（if/foreach/choose）、`#{}`（预编译防注入）与 `${}`（直接拼接）的区别 |
| 缓存机制 | 一级缓存（SqlSession 级别，默认开启）、二级缓存（Mapper 级别，需手动配置） |
| 高级功能 | 分页插件（PageHelper）、批量操作、逻辑删除、乐观锁（MyBatis-Plus） |
| 结果映射 | resultMap 复杂映射（一对一/一对多/多对多） |

---

## 四、MySQL

### 4.1 核心知识点

| 知识点 | 必须掌握的内容 |
|--------|--------------|
| 索引 | B+Tree 结构与原理、聚簇索引 vs 非聚簇索引、最左前缀原则、覆盖索引、索引下推 |
| SQL 优化 | EXPLAIN 执行计划解读（type/rows/Extra/Using filesort/Using temporary）、慢查询定位与优化 |
| 事务 | ACID 特性、四种隔离级别及对应问题（脏读/不可重复读/幻读）、MVCC 原理（ReadView + Undo Log） |
| 锁机制 | 行锁/间隙锁/临键锁、乐观锁（版本号）vs 悲观锁（SELECT FOR UPDATE） |
| SQL 能力 | 多表联查（JOIN/LEFT JOIN）、子查询（IN/EXISTS）、GROUP BY + HAVING、UNION、窗口函数 |

### 4.2 索引优化决策树

```
查询慢
  → EXPLAIN 分析执行计划
     → type 为 ALL（全表扫描）→ 考虑加索引
     → rows 扫描行数过大 → 优化索引或 SQL
     → Extra 出现 Using filesort → 优化排序字段的索引
     → Extra 出现 Using temporary → 优化 GROUP BY/ORDER BY
```

---

## 五、Redis

| 知识点 | 核心内容 |
|--------|---------|
| 数据结构 | String / Hash / List / Set / ZSet 五种基本类型及其底层实现（SDS/ziplist/skiplist 等） |
| 缓存问题 | 缓存穿透（布隆过滤器/空值缓存）、缓存击穿（互斥锁/永不过期）、缓存雪崩（过期时间随机化/多级缓存/熔断） |
| 分布式锁 | SET NX EX 实现、Redisson 的看门狗机制、RedLock 算法 |
| 过期与淘汰 | 定期删除 + 惰性删除、8 种内存淘汰策略（LRU/LFU/TTL 等） |
| 应用场景 | 限流（滑动窗口/令牌桶）、计数器、Session 共享、排行榜（ZSet）、消息队列（List/Stream） |

---

## 六、中间件与基础设施

### 6.1 消息队列

| 知识点 | 核心内容 |
|--------|---------|
| 通用能力 | 异步解耦、流量削峰、最终一致性、消息可靠性（持久化 + 确认机制 + 死信队列） |
| RabbitMQ | Exchange 类型（Direct/Topic/Fanout/Headers）、消息确认（Publisher Confirm + Consumer Ack）、死信队列与延迟消息 |
| RocketMQ | 事务消息、顺序消息、延迟消息、NameServer 架构 |
| Kafka | 高吞吐原理（顺序写/零拷贝/分区并行）、消费组机制、ISR 副本同步 |

### 6.2 其他基础设施

| 组件 | 核心用途 |
|------|---------|
| Nginx | 反向代理、负载均衡策略（轮询/权重/IP Hash）、动静分离、限流配置 |
| Docker | 镜像构建（Dockerfile）、容器运行、Docker Compose 多服务编排 |

---

## 七、网络与通用知识

| 知识点 | 核心内容 |
|--------|---------|
| HTTP / HTTPS | 请求方法、状态码、HTTPS 握手流程（TLS/SSL）、HTTP/1.1 vs HTTP/2 区别 |
| RESTful API | 资源命名规范、HTTP 方法语义、统一返回格式、全局异常处理 |
| 认证与授权 | Cookie / Session 机制、Token（JWT 结构与验证流程）、OAuth2.0 授权码模式 |
| 跨域 | CORS 原理（简单请求 vs 预检请求）、`Access-Control-*` 响应头、Spring 中的跨域配置 |
| 接口幂等性 | 唯一 ID + 状态机、Token 机制、数据库唯一约束 |

---

## 八、工程化与工具链

| 工具 | 核心能力要求 |
|------|-------------|
| Maven / Gradle | 依赖管理、多模块构建、生命周期与插件、版本冲突解决 |
| Git | 常用命令（clone/add/commit/push/pull/branch/merge/rebase）、Git Flow 分支模型、冲突解决 |
| Postman | 接口测试、环境变量、集合管理、自动化测试脚本 |
| Linux | 文件操作（ls/cd/cp/mv/rm）、权限管理（chmod/chown）、进程管理（ps/top/kill）、日志查看（tail/grep/less）、磁盘管理（df/du） |
| 日志框架 | SLF4J + Logback 配置、日志级别、MDC 链路追踪、异步日志 |

---

## 九、设计模式

| 模式 | 应用场景 |
|------|---------|
| **单例** | Spring Bean 默认作用域、数据库连接池、配置管理器 |
| **工厂** | Spring BeanFactory、支付渠道工厂 |
| **策略** | 不同支付方式、不同消息推送渠道、不同优惠计算规则 |
| **模板方法** | JdbcTemplate、RabbitTemplate、RestTemplate |
| **建造者** | Lombok @Builder、复杂对象构建（如 HTTP 请求构造） |
| **装饰器** | Java IO 流（BufferedReader → InputStreamReader → FileReader） |
| **适配器** | Spring MVC 的 HandlerAdapter、新旧系统接口兼容 |
| **代理** | Spring AOP（JDK 动态代理 + CGLIB）、MyBatis Mapper 代理 |
| **观察者** | Spring Event 事件机制、消息队列的发布订阅模型 |

---

## 十、高频面试题速查

| # | 问题 | 考查方向 |
|:--:|------|---------|
| 1 | HashMap 为什么线程不安全？ | 集合 + 并发 |
| 2 | MySQL 索引如何建立与优化？ | 数据库 |
| 3 | Spring 事务失效的常见场景有哪些？ | Spring |
| 4 | 如何解决超卖问题？ | Redis + 并发 |
| 5 | 分布式锁如何实现？对比不同方案？ | Redis/ZK + 分布式 |
| 6 | 接口性能如何优化？ | 综合能力 |
| 7 | 百万级数据导出如何避免 OOM？ | JVM + 工程实践 |
| 8 | ConcurrentHashMap 如何保证线程安全？ | 集合 + 并发 |
| 9 | Redis 缓存穿透/击穿/雪崩如何解决？ | 缓存 |
| 10 | Spring Bean 生命周期是怎样的？ | Spring |

---

## 附录：技能掌握程度自查标准

| 等级 | 标准 | 达成标志 |
|:----:|------|---------|
| **了解** | 知道概念和基本用法 | 能说出是什么、用来干什么 |
| **熟悉** | 理解原理，能独立完成日常开发 | 能用代码实现、能配置、能排查简单问题 |
| **掌握** | 深入理解底层原理，能对比不同方案 | 面试中能口述原理、能分析源码关键逻辑 |
| **精通** | 可独立设计架构方案，解决复杂问题 | 能主导技术选型、能指导他人、能定位疑难问题 |

---

*最后更新：2026-07-15*
