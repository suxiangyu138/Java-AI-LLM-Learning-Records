# Java 后端高频面试题 · 简洁标准答案（考前速记版）

> 🎯 **定位**：面试前 30 分钟快速过一遍，每道题 3~5 句话标准答法。
> 📖 **详细版**：需要深入理解时，跳转 `苏巷雨-面试题库-01-Java基础与主流中间件高频题.md`。
> 🔥 = 必考（90%+概率）  ⭐ = 高频（60%+）  💡 = 加分项

---

## 一、Java 基础

### 🔥 1. HashMap 底层原理（1.7 vs 1.8）

- **1.7**：数组 + 链表，头插法，扩容时可能死循环（多线程 resize 形成环链）
- **1.8**：数组 + 链表 + 红黑树，尾插法，链表长度 ≥ 8 且数组长度 ≥ 64 时转红黑树，树节点 ≤ 6 退化为链表
- 默认容量 16，负载因子 0.75，扩容 2 倍
- 线程不安全 → 用 ConcurrentHashMap 或 Collections.synchronizedMap
📌 项目落脚点：Flavor Dash中用ConcurrentHashMap缓存商家数据，避免并发扩容死循环

### 🔥 2. ConcurrentHashMap 原理

- **1.7**：Segment 分段锁（继承 ReentrantLock），默认 16 段，并发度 = Segment 数
- **1.8**：CAS + synchronized，锁粒度降到 **Node 级别**；put 时 CAS 尝试写，失败则 synchronized 锁住头节点
- size() 用 `baseCount + CounterCell[]` 累加，不锁整个表
📌 项目落脚点：Flavor Dash中缓存商家数据，CAS+synchronized保证线程安全

### 🔥 3. String / StringBuilder / StringBuffer

- **String**：final 修饰 char[]/byte[]，不可变 → 线程安全，每次修改都 new 新对象 → 频繁拼接性能差
- **StringBuilder**：可变，线程不安全，单线程下性能最高
- **StringBuffer**：可变，方法加 synchronized，线程安全但慢于 StringBuilder
📌 项目落脚点：Flavor Dash中拼接SQL和日志用StringBuilder，避免频繁创建对象

### ⭐ 4. == 和 equals

- `==`：基本类型比值，引用类型比地址
- `equals`：Object 默认比地址（同 ==），String/Integer 等重写后比内容
- 重写 equals **必须重写 hashCode**（HashMap 判断 key 相等时两者都校验）
📌 项目落脚点：Flavor Dash中为商家DTO重写equals/hashCode用于集合去重

### ⭐ 5. 异常体系

- **Throwable** → Error（不可处理，OOM/StackOverflow）+ Exception
- **Exception** → 编译时异常（IOException/SQLException，必须 try-catch 或 throws）+ 运行时异常（RuntimeException：NPE/ClassCast/IndexOutOfBounds，可不显式处理）
- finally 中不要写 return（会吞掉 try 中的返回值和异常）
📌 项目落脚点：Flavor Dash中用全局@RestControllerAdvice统一捕获异常

### ⭐ 6. 反射机制

- 运行时动态获取类的 Class 对象 → 操作构造器/方法/字段
- 核心 API：`Class.forName()` → `getConstructor()` / `getMethod()` / `getField()`
- 应用：Spring IoC 依赖注入、MyBatis 结果映射、动态代理
- 代价：性能较低、破坏封装、安全检查开销
📌 项目落脚点：Flavor Dash中基于反射实现通用DTO转换工具

### 💡 7. final / finally / finalize

- **final**：类不可继承、方法不可重写、变量不可修改（引用不可变，内容可变）
- **finally**：try-catch 后必然执行（除非 System.exit()）
- **finalize**：GC 回收前调用（JDK 9 已废弃，不可靠，用 try-with-resources 代替）
📌 项目落脚点：Flavor Dash中用final修饰配置类，finally释放SSE连接资源

### 💡 8. 接口 vs 抽象类（JDK 8+）

- **接口**：默认 public abstract 方法，JDK 8+ 可有 default/static 方法，多实现
- **抽象类**：可有构造器、成员变量、已实现方法，单继承
- 选接口：定义行为契约；选抽象类：共享代码 + 共同属性
📌 项目落脚点：Flavor Dash中定义LLM Client接口，多厂商各自实现

---

## 二、并发编程

### 🔥 1. synchronized 和 ReentrantLock 区别

| 维度 | synchronized | ReentrantLock |
|------|-------------|---------------|
| 实现 | JVM 内置（monitorenter/exit） | JDK API（AQS） |
| 锁释放 | 自动（代码块结束/异常） | 手动 unlock()，**必须放 finally** |
| 特性 | 非公平（默认） | 可选公平/非公平 |
| 高级功能 | 无 | tryLock(超时)、lockInterruptibly、Condition 多条件 |
| 性能 | 1.6 优化后相近 | 略优（高竞争场景） |
📌 项目落脚点：Flavor Dash中用ReentrantLock+Condition实现订单超时取消

### 🔥 2. volatile 关键字

- **保证可见性**：写 volatile 变量立即刷回主存，读直接从主存取（跳过 CPU 缓存）
- **禁止指令重排**：通过内存屏障（Memory Barrier）阻止前后的指令重排
- **不保证原子性**：i++ 这种复合操作仍不安全（读-改-写三步），需用 AtomicInteger 或加锁
- 经典应用：DCL 单例中 `volatile` 防止指令重排导致半初始化对象逸出
📌 项目落脚点：Flavor Dash中DCL单例管理Redis客户端，volatile防指令重排

### 🔥 3. 线程池核心参数（7 个）

```
new ThreadPoolExecutor(
    corePoolSize,      // 核心线程数（常驻），即使空闲也不销毁（除非 allowCoreThreadTimeOut）
    maximumPoolSize,   // 最大线程数
    keepAliveTime,     // 非核心线程空闲存活时间
    unit,              // 时间单位
    workQueue,         // 阻塞队列（LinkedBlockingQueue/ArrayBlockingQueue/SynchronousQueue）
    threadFactory,     // 线程工厂（命名线程，便于排查）
    rejectedHandler    // 拒绝策略（Abort抛异常/CallerRuns回退调用者线程/Discard丢弃/DiscardOldest丢弃最老）
)
```
📌 项目落脚点：Flavor Dash中自定义线程池处理SSE流式推送

### 🔥 4. 线程池工作流程

1. 任务来 → 核心线程有空闲 → 核心线程执行
2. 核心线程忙 → 任务入队列等待
3. 队列满 → 创建新线程（不超过 maximumPoolSize）
4. 线程数已达最大 + 队列满 → 执行**拒绝策略**
5. 线程空闲超过 keepAliveTime → 回收非核心线程
📌 项目落脚点：Flavor Dash中线程池异步处理订单推送

### ⭐ 5. ThreadLocal 原理与内存泄漏

- 每个 Thread 内部有 `ThreadLocalMap`，key 是 ThreadLocal 的**弱引用**，value 是存的值
- **为什么弱引用**：ThreadLocal 外部的强引用没了 → GC 回收 ThreadLocal → key 变 null，防止 ThreadLocal 本身无法回收
- **内存泄漏**：key 被回收后 value 仍在 ThreadLocalMap 中（Entry 强引用 value），线程池中线程复用时更严重
- **解决**：每次用完调 `remove()`
📌 项目落脚点：Flavor Dash中用ThreadLocal传递用户身份到拦截器，finally remove防泄漏

### ⭐ 6. CAS（Compare And Swap）

- 乐观锁，CPU 级别原子指令 `cmpxchg`，比较内存值 == 期望值 → 替换为新值
- Java 实现：`Unsafe.compareAndSwapInt()` → `AtomicInteger.incrementAndGet()`
- **ABA 问题**：A→B→A，CAS 检测不到 → 用 `AtomicStampedReference` 加版本号
- **自旋开销**：高竞争下 CAS 失败频繁 → CPU 空转 → 此时不适合，应升级为锁
📌 项目落脚点：Flavor Dash中用AtomicInteger统计实时并发请求数

### 💡 7. AQS（AbstractQueuedSynchronizer）

- JUC 的基石，ReentrantLock / CountDownLatch / Semaphore / ReentrantReadWriteLock 都基于它
- 核心：`volatile int state` + **CLH 双向队列**（存放等待线程）+ 模板方法模式
- state=0 无人持锁，state>0 有人持锁（可重入时 state 递增）
- 详见 `苏巷雨-面试题库-06 §AQS`
📌 项目落脚点：SuGuangMall中自定义同步器控制秒杀令牌发放

### 💡 8. CompletableFuture（异步编排）

- JDK 8 引入，解决 Future 的 get() 阻塞痛点，支持函数式回调链
- 核心方法：`supplyAsync()` 异步执行 → `thenApply()` 转换 → `thenCombine()` 合并 → `exceptionally()` 异常处理 → `allOf()` 等待全部
- 自带线程池 ForkJoinPool（守护线程，慎用），生产环境必须**传入自定义线程池**
📌 项目落脚点：Flavor Dash中编排AI推荐等多模型调用，allOf合并结果

---

## 三、JVM

### 🔥 1. JVM 内存模型（运行时数据区）

| 区域 | 线程 | 内容 | 异常 |
|------|------|------|------|
| **堆** | 共享 | 对象实例、数组（GC 主要区域） | OOM |
| **方法区**（元空间） | 共享 | 类信息、常量、静态变量（JDK8+ 用本地内存） | OOM |
| **虚拟机栈** | 私有 | 栈帧（局部变量表/操作数栈/方法出口） | StackOverflow |
| **本地方法栈** | 私有 | Native 方法 | StackOverflow |
| **程序计数器** | 私有 | 当前执行字节码行号 | — |

📌 项目落脚点：Flavor Dash中排查堆OOM用jmap dump堆转储

### 🔥 2. 垃圾回收算法

- **标记-清除**：标记存活对象 → 清除未标记 → 产生内存碎片；CMS 老年代使用
- **标记-复制**：分两块区域，存活对象复制到另一块，清空原区域 → 空间利用率 50%；新生代（Eden:Survivor=8:1:1）
- **标记-整理**：标记后存活对象向一端移动 → 无碎片，STW 长；Serial Old / Parallel Old 使用
- **分代收集**：新生代复制，老年代清除+整理，是 JVM 默认策略
📌 项目落脚点：SuGuangMall秒杀场景调优G1，控制STW在50ms内

### ⭐ 3. 类加载过程 & 双亲委派

- **过程**：加载 → 验证 → 准备（static 变量赋默认值）→ 解析（符号引用→直接引用）→ 初始化（static 变量赋实际值 + static 代码块）
- **双亲委派**：先向上委托父类加载器（App → Ext → Bootstrap），父加载不了才自己加载
- **作用**：避免重复加载 + 保护核心类不被篡改（如自定义 java.lang.String 不会生效）

### 💡 4. GC 日志怎么看

- `-Xlog:gc*`（JDK 9+）或 `-XX:+PrintGCDetails -XX:+PrintGCDateStamps`
- 关注：GC 频率（是否频繁 Full GC）、单次 GC 耗时、GC 后内存是否持续增长（内存泄漏信号）
📌 项目落脚点：Flavor Dash中通过GC日志发现Full GC频繁

---

## 四、MySQL

### 🔥 1. 索引底层：B+ 树

- **为什么不用二叉树/红黑树**：数据量大时高度太高 → 磁盘 I/O 次数多
- **为什么不用 B 树**：B 树非叶子也存数据 → 每页存的 key 少 → 树更高
- **B+ 树优势**：非叶子只存 key → 单个节点能存更多 key → 树更矮（3 层可存千万级）；叶子形成**有序链表** → 范围查询一次定位后顺序遍历；叶子存完整数据行或主键
- 详见 `苏巷雨-面试题库-01 §四`
📌 项目落脚点：SuGuangMall中订单表建联合索引(user_id+status+create_time)

### 🔥 2. 聚簇索引 vs 非聚簇索引（回表）

- **聚簇索引**：叶子存**整行数据**；InnoDB 主键索引就是聚簇索引，只有一个
- **非聚簇索引（二级索引）**：叶子存**主键值**；查到主键后回聚簇索引取完整行 → **回表**
- **覆盖索引**：查询列都在二级索引中 → 不回表 → `Using index`，性能最优
- **联合索引**：建立时注意区分度高的列放前面，遵循最左前缀原则
📌 项目落脚点：SuGuangMall中用覆盖索引查询商品，Extra显示Using index

### 🔥 3. 事务隔离级别 & MVCC

| 级别 | 脏读 | 不可重复读 | 幻读 | 实现 |
|------|------|-----------|------|------|
| 读未提交 | ✅ | ✅ | ✅ | 无锁 |
| 读已提交 | ❌ | ✅ | ✅ | 每次快照读生成新 ReadView（RC） |
| 可重复读 | ❌ | ❌ | ❌（InnoDB 消除） | 事务开始生成一次 ReadView（RR）+ Next-Key Lock 防幻读 |
| 串行化 | ❌ | ❌ | ❌ | 读写锁 |

- **MVCC**：每行有隐藏列 `trx_id`（最后修改的事务 ID）+ `roll_pointer`（回滚指针指向 undo log）；通过 ReadView 判断可见性
📌 项目落脚点：SuGuangMall中RR隔离级别+MVCC保证订单一致性

### ⭐ 4. SQL 优化思路

1. **慢查询日志定位** → `EXPLAIN` 看 type/rows/Extra（type 从好到差：system > const > ref > range > index > ALL）
2. **加索引**：WHERE/JOIN/ORDER BY/GROUP BY 列建联合索引，注意最左前缀
3. **避免索引失效**：`%like` 开头、WHERE 列用函数、OR 两边不同列、类型隐式转换
4. **大表分页**：`LIMIT 100000,20` → 改为 `WHERE id > 100000 LIMIT 20`（基于主键的延迟关联）
5. **读写分离** + 分库分表（水平拆：按 user_id 哈希 → 多库，垂直拆：按业务模块）
6. 详见 `苏巷雨-面试题库-01 §四 #7~8` + `题库 05 #7`
📌 项目落脚点：SuGuangMall中用EXPLAIN定位全表扫描，加索引后2s→5ms

### ⭐ 5. InnoDB vs MyISAM

- **InnoDB**：支持事务（ACID）、行锁（条件走索引才行锁否则表锁）、MVCC、外键、崩溃恢复（redo log），默认引擎
- **MyISAM**：不支持事务、表锁、查询插入快（无事务开销）、不支持外键、崩溃后数据可能损坏
- 现在 99% 场景用 InnoDB
📌 项目落脚点：SuGuangMall中全部InnoDB，事务+行锁支撑秒杀

---

## 五、Redis

### 🔥 1. 缓存穿透 / 击穿 / 雪崩

| 问题 | 现象 | 解决方案 |
|------|------|---------|
| **穿透** | 查不存在的数据 → 每次都打到 DB | 布隆过滤器（预判是否存在）+ 缓存空值（TTL 短） |
| **击穿** | 热点 key 过期瞬间 → 大量请求打 DB | 互斥锁（setnx 抢锁重建）+ 逻辑过期（异步重建）+ 永不过期 |
| **雪崩** | 大量 key 同时过期或 Redis 宕机 | TTL 加随机值（原值 ± 30%）、Redis Cluster / 哨兵、多级缓存（Caffeine 本地兜底） |
📌 项目落脚点：Flavor Dash中布隆过滤器+互斥锁+TTL随机化三管齐下

### 🔥 2. Redis 分布式锁

```java
// 加锁：SET key value NX EX 30
// 解锁：Lua 脚本原子校验 value 再删除
String script = "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                "return redis.call('del', KEYS[1]) else return 0 end";
```

- **为什么用 Lua**：get + del 两次操作非原子，万一 get 后 key 过期、其他线程加锁 → 删了别人的锁
- **看门狗**（Redisson）：锁快过期时自动续期（默认 30s → 每 10s 续到 30s），避免业务没执行完锁就释放
- **Redlock**：多节点加锁（N/2+1 成功才算成功），解决单点故障，但争议较大
📌 项目落脚点：SuGuangMall秒杀中用Redisson+Lua脚本保证原子性

### ⭐ 3. Redis 持久化

- **RDB**：定时全量快照（`save 900 1` 900 秒内 1 次修改触发），二进制文件，恢复快，可能丢最后几分钟数据
- **AOF**：追加每条写命令，`appendfsync everysec` 每秒刷盘，丢 1 秒数据，文件大需重写（bgrewriteaof）
- **生产**：RDB + AOF 混合（Redis 4.0+），兼顾恢复速度与数据安全
📌 项目落脚点：Flavor Dash中RDB+AOF混合持久化

### 💡 4. 五大数据类型 & 使用场景

- **String**：分布式锁、计数器、Session 共享
- **Hash**：对象存储（用户信息）、购物车
- **List**：消息队列（LPUSH + BRPOP）、最新列表
- **Set**：标签、共同关注、抽奖（SRANDMEMBER）
- **ZSet**：排行榜（score=分数）、延迟队列（score=时间戳）
📌 项目落脚点：Flavor Dash中String做分布式锁、ZSet做排行榜、Set做标签推荐

---

## 六、Spring 生态

### 🔥 1. IoC & DI 原理

- **IoC**：对象创建和依赖管理从"自己 new"反转为"容器管理"
- **DI**：容器在创建 Bean 时，自动将其依赖的对象注入（构造器/Setter/字段注入，推荐构造器）
- **实现**：读取配置（XML/注解）→ BeanDefinition → BeanFactory 反射创建 → 注入依赖 → 放入单例池（DefaultSingletonBeanRegistry 的 ConcurrentHashMap）
📌 项目落脚点：Flavor Dash中构造器注入+@Qualifier选择LLM实现

### 🔥 2. AOP 原理（JDK 动态代理 vs CGLIB）

- **JDK 动态代理**：基于接口，`Proxy.newProxyInstance()` 生成代理类实现接口
- **CGLIB**：基于继承，生成目标类的子类，final 类/方法无法代理
- Spring Boot 2.x 默认 JDK 代理，Spring Boot 3.x 默认 CGLIB（因为大多不需要接口）
- **应用**：`@Transactional`（事务切面）、`@Cacheable`（缓存切面）、日志切面、权限切面
📌 项目落脚点：Flavor Dash中用@Transactional声明事务+自定义日志切面

### 🔥 3. @Transactional 失效场景（必问必答）

1. **同类方法调用**：`this.methodB()` → 不经过代理，AOP 不生效 → 用 `AopContext.currentProxy()` 或拆分到另一个 Service
2. **非 public 方法**：CGLIB 只能代理 public 方法
3. **异常被 catch 吃掉**：事务只回滚 RuntimeException 和 Error（`rollbackFor = Exception.class` 可指定）
4. **数据库引擎不支持**：MyISAM 不支持事务
5. **多线程**：事务绑定当前线程（ThreadLocal），新线程不在同一事务中
📌 项目落脚点：Flavor Dash中遇到过同类调用失效，用AopContext解决

### 🔥 4. Bean 生命周期（简化版）

```
实例化 → 属性填充 → BeanNameAware → BeanFactoryAware → 
BeanPostProcessor.before → @PostConstruct → InitializingBean → 
BeanPostProcessor.after（AOP 在这里生成代理） → Bean 就绪 → 
@PreDestroy → DisposableBean → 销毁
```
📌 项目落脚点：Flavor Dash中@PostConstruct初始化Redis连接池

### ⭐ 5. Spring Boot 自动配置原理

- `@SpringBootApplication` → `@EnableAutoConfiguration` → `@Import(AutoConfigurationImportSelector)`
- 读取 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`（3.x）中注册的 XxxAutoConfiguration 类
- 每个 `@AutoConfiguration` 配合 `@ConditionalOnClass` / `@ConditionalOnMissingBean` 等条件注解
- 用户定义了 Bean → 不覆盖；没定义 → 自动配置生效
📌 项目落脚点：Flavor Dash中自定义LLM Client自动配置

### ⭐ 6. Spring 循环依赖（三级缓存）

| 缓存 | 名称 | 存什么 |
|------|------|--------|
| 一级 | singletonObjects | 成品 Bean |
| 二级 | earlySingletonObjects | 半成品 Bean（已实例化未填充） |
| 三级 | singletonFactories | ObjectFactory（可生成代理对象） |

流程：A 创建 → 发现依赖 B → 创建 B → B 发现依赖 A → 从三级缓存拿 A 的早期引用 → B 完成 → A 继续完成
**仅支持单例 setter 注入的循环依赖**，构造器注入和 prototype 不行

---

## 七、消息队列（RabbitMQ）

### ⭐ 1. 消息不丢失（可靠性投递）

| 环节 | 措施 |
|------|------|
| 生产者→MQ | Publisher Confirm（confirmCallback 确认 Broker 收到）+ 失败重试 |
| MQ 自身 | Queue + Message 持久化（durable=true） |
| MQ→消费者 | 手动 ACK（消费成功再确认，失败不确认→重回队列） |
| 兜底 | 死信队列 + 定时补偿任务（扫描状态未变更的订单） |
📌 项目落脚点：Flavor Dash中Publisher Confirm+手动ACK+死信队列

### ⭐ 2. 消息重复消费（幂等性）

- 消费端用**业务唯一 ID**（如订单号）+ Redis setnx 判重 → 已处理则跳过
- 或数据库唯一索引防重复插入
- RabbitMQ 保证"至少一次"投递，不保证"恰好一次"
📌 项目落脚点：Flavor Dash中订单号+Redis setnx判重

### 💡 3. 死信队列应用

- 消息被拒绝（basic.reject 且 requeue=false）+ TTL 过期 + 队列满了 → 转发到死信交换机
- **延迟队列**：设消息 TTL 30min → 死信队列消费 → 实现 30min 后关单/取消未支付订单
📌 项目落脚点：SuGuangMall中延迟队列实现30分钟关单

---

## 八、微服务 & 分布式

### 🔥 1. CAP 理论

- **C（一致性）**：所有节点同一时刻数据相同
- **A（可用性）**：每个请求都能得到响应（不报错）
- **P（分区容错）**：网络分区后系统仍能工作 → **分布式系统 P 必须保证**，只能在 C 和 A 之间权衡
- **CP**（ZooKeeper/Nacos CP 模式）：选主期间不可用，保证一致性
- **AP**（Eureka/Nacos AP 模式）：始终可用，但可能读到旧数据
📌 项目落脚点：SuGuangMall中Nacos AP模式+Seata CP模式

### ⭐ 2. 服务注册发现 & Nacos

- 服务启动 → 向 Nacos 注册（IP+端口+元数据）→ 维持心跳（5s）→ Nacos 剔除不健康实例
- 调用方从 Nacos 拉取服务列表 → 本地缓存 + Ribbon/LoadBalancer 负载均衡
- Nacos 同时支持 **AP**（默认，优先可用性）和 **CP**（一致性优先，选主期间不可用）
📌 项目落脚点：SuGuangMall中所有微服务注册到Nacos

### ⭐ 3. Sentinel 限流熔断

- **限流（Flow）**：QPS/线程数控制，策略：直接/关联/链路；效果：快速失败/Warm Up/排队等待
- **熔断（Degrade）**：慢调用比例（RT>阈值）/ 异常比例 / 异常数 → OPEN（拒绝）→ HALF_OPEN（探测）→ CLOSE（恢复）
- 控制台配置实时生效，无需重启
📌 项目落脚点：SuGuangMall中限流秒杀接口

### 💡 4. Seata AT 模式原理

- **两阶段**：一阶段各 RM 执行 SQL + 记录 undo_log（前后镜像）→ 二阶段 TC 通知全局提交（删 undo_log）或回滚（根据 undo_log 生成反向 SQL）
- **ACID**：通过全局锁 + undo_log 实现，性能约为无事务的 60%~70%
- **不适用**：高并发秒杀 → 用消息队列保证最终一致性
📌 项目落脚点：SuGuangMall中实现下单扣库存分布式事务

---

## 九、计算机网络

### 🔥 1. TCP 三次握手四次挥手

- **三次握手**：SYN → SYN+ACK → ACK；第三次可带数据；防历史连接 + 同步序列号
- **四次挥手**：FIN → ACK → FIN → ACK；TIME_WAIT 2MSL（确保最后的 ACK 到达 + 让旧连接数据包消失）
- 详见 `苏巷雨-面试题库-02 §一`

### 🔥 2. HTTPS（SSL/TLS）加密过程

1. 客户端 → 服务端：支持的加密套件 + 随机数1
2. 服务端 → 客户端：选定加密套件 + 随机数2 + **证书（含公钥）**
3. 客户端验证证书 → 生成随机数3（Pre-Master Secret）→ 公钥加密发送
4. 双方用随机数1+2+3 生成**对称密钥** → 后续通信对称加密（AES）
📌 项目落脚点：Flavor Dash中微信支付回调校验SSL证书

### ⭐ 3. HTTP 状态码

- **2xx**：200 OK、201 Created、204 No Content
- **3xx**：301 永久重定向、302 临时重定向、304 Not Modified（缓存有效）
- **4xx**：400 参数错误、401 未认证、403 无权限、404 不存在
- **5xx**：500 服务器内部错误、502 网关错误、503 服务不可用
📌 项目落脚点：Flavor Dash中统一响应封装

---

## 十、项目 & 场景题

### ⭐ 1. 请介绍你最熟悉的一个项目

**标准结构（STAR 法则）**：
- **S（背景）**：项目是什么、解决什么问题
- **T（任务）**：你负责的部分
- **A（行动）**：技术选型 + 架构设计 + 具体怎么做的
- **R（结果）**：量化成果（QPS 2000+ / 成功率 99.5% / 召回率 88%）
- 详见 `苏巷雨-面试题库-01 §十一`（三项目逐题深挖） + `苏巷雨-面试题库-09-简历定制与项目深挖.md`
📌 项目落脚点：以Flavor Dash为主线，展示Java+AI全栈能力

### ⭐ 2. 你遇到过最大的技术挑战？

- 选一个具体案例，如：秒杀零超卖 / LLM 模型网关多提供商统一 / RAG 检索精度提升
- 讲清楚：问题现象 → 排查过程 → 方案对比 → 最终方案 → 效果验证
📌 项目落脚点：Flavor Dash中LLM多模型网关统一管理难题

### ⭐ 3. 线上接口突然变慢，怎么排查？

1. **定位**：监控看是哪个接口（Grafana/Prometheus）→ 该时间段是否有发布/流量峰值
2. **CPU**：`top -Hp <pid>` 找高 CPU 线程 → `jstack` 转十六进制定位代码行
3. **内存**：`jstat -gc <pid> 1000` 看 GC 频率，频繁 Full GC 可能是内存泄漏
4. **DB**：`SHOW PROCESSLIST` 看慢查询 + `EXPLAIN` 分析 → 缺索引/锁等待
5. **外部依赖**：看 Redis/RabbitMQ/下游服务响应时间
6. 详见 `苏巷雨-面试题库-05 #7`
📌 项目落脚点：Flavor Dash中某接口变慢，jstack+EXPLAIN定位

### ⭐ 4. 秒杀系统怎么设计？

核心链路：**Nginx 限流** → **CDN 静态化** → **Redis Lua 原子扣库存**（预占）→ **RabbitMQ 异步创建订单** → **死信队列延迟关单回滚库存** → **前端防重复提交**
详见 `苏巷雨-面试题库-05 #1` + `苏巷雨-面试题库-09-简历定制与项目深挖.md #12~14`
📌 项目落脚点：SuGuangMall核心亮点，Redis Lua+MQ实现QPS 2000+零超卖

---

## 📖 交叉索引（需要深入看这里）

| 模块 | 速记版（本文） | 详细版 |
|------|:---:|------|
| Java 基础+并发 | §一~二 | `苏巷雨-面试题库-01-Java基础与主流中间件高频题.md` §一 |
| JVM | §三 | 题库 01 §二 |
| MySQL | §四 | 题库 01 §四 |
| Redis | §五 | 题库 01 §五 |
| Spring | §六 | 题库 01 §三 |
| RabbitMQ | §七 | 题库 01 §六 |
| 微服务 | §八 | 题库 01 §八 + 题库 05 |
| 计算机网络 | §九 | 题库 02 §一 |
| 项目深挖 | §十 | **`苏巷雨-面试题库-09-简历定制与项目深挖.md`** |
| 系统设计+场景 | §十 | 题库 05 |
| 行为面试+反问 | — | 题库 07 |

---

> ⏱️ 本文适合面试前 **30 分钟快速过一遍**，追求"每道题能说 3~5 句"。深入理解请跳到详细版题库 01~09。
