Java后端高频面试题 完整答案汇总
这份面试题覆盖Java基础、多线程、Spring全家桶、微服务、MySQL、Redis、中间件、JVM、RAG、容器、项目实战，是Java后端求职核心考点，下面给出精简、可直接背诵的标准答案。
 
一、自我介绍
答题模板
我是XX，XX大学计算机专业，熟悉Java后端核心技术栈，精通SpringBoot、SpringCloud微服务体系，熟练掌握MySQL、Redis、RabbitMQ等中间件，理解JVM底层原理与多线程并发编程。
有实际项目开发经验，擅长业务落地、性能调优与线上问题排查，具备良好的代码规范与架构思维，学习能力强，能够快速适配业务需求。
 
二、技术栈面试题
1、用到微服务哪些组件？分别用来干什么？
- Nacos：服务注册/发现、配置中心、动态配置管理
- Gateway：API网关，路由转发、鉴权、限流、熔断
- Feign：声明式远程调用，简化服务间通信
- Sentinel：服务熔断、降级、限流，保障高可用
- Seata：分布式事务，解决跨库事务一致性
- RocketMQ/Kafka：消息队列，异步解耦、削峰填谷
- Sleuth+Zipkin：分布式链路追踪，排查跨服务问题
 
2、线程池的执行流程？队列区别？拒绝策略？
执行流程
1. 提交任务，核心线程数未满 → 创建核心线程执行
2. 核心线程已满 → 任务进入阻塞队列
3. 队列已满 → 创建非核心线程执行
4. 最大线程数已满 → 触发拒绝策略
    队列区别
    - ArrayBlockingQueue：有界阻塞队列，固定容量
    - LinkedBlockingQueue：无界/有界，链表实现，吞吐量高
    - SynchronousQueue：不存储元素，直接交付任务
    - PriorityBlockingQueue：优先级队列，按优先级排序
    拒绝策略
    -  AbortPolicy ：直接抛出异常（默认）
    -  CallerRunsPolicy ：调用者线程执行任务
    -  DiscardPolicy ：直接丢弃任务
    -  DiscardOldestPolicy ：丢弃队列最旧任务，执行新任务
 
3、ReentrantLock 和 synchronized 区别？底层实现？
核心区别
1. 底层：synchronized 是JVM层面，依赖对象头MarkWord；ReentrantLock 是API层面，依赖AQS
2. 可重入：都支持可重入锁
3. 公平锁：synchronized 非公平；ReentrantLock 支持公平/非公平
4. 功能：ReentrantLock 支持Condition精准唤醒、可中断、尝试获取锁
5. 性能：JDK1.6后synchronized优化，性能基本持平
    底层实现
    - synchronized：对象头MarkWord存储锁状态，偏向锁→轻量级锁→重量级锁升级
    - ReentrantLock：基于AQS，CAS修改state状态，CLH双向链表管理等待线程
 
4、volatile 能保证线程安全吗？解决了什么问题？
- 不能保证线程安全，只能保证可见性、有序性，不保证原子性
- 解决问题：
    1. 可见性：一个线程修改volatile变量，其他线程立即感知
    2. 有序性：禁止指令重排，避免单例模式DCL失效
- 不适用场景：i++、复合操作等非原子场景
 
5、ConcurrentHashMap 为什么线程安全？
1. JDK1.7：分段锁Segment，每个Segment独立加锁，降低竞争粒度
2. JDK1.8：
    - 底层：数组+链表+红黑树
    - 锁机制：CAS + synchronized，只锁住链表/红黑树头节点
    - 并发扩容：多线程辅助扩容，提升扩容效率
    - size统计：CounterCell数组，分散统计，减少竞争
 
6、SpringBoot 自动配置原理
核心：@EnableAutoConfiguration + SPI机制
1. 启动类注解 @SpringBootApplication 包含 @EnableAutoConfiguration 
2. 从 META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports 加载自动配置类
3. 配置类通过 @Conditional 条件注解，根据依赖、环境自动装配Bean
4.  application.yml 配置文件绑定属性，自定义组件参数
 
7、如何自定义 SpringBoot Starter？
1. 创建两个模块：autoconfigure配置模块 + starter启动模块
2. 编写自动配置类，添加 @Configuration 、 @Conditional 注解
3. 编写配置属性类，添加 @ConfigurationProperties 
4. 在 resources/META-INF/spring/ 创建imports文件，注册自动配置类
5. starter模块依赖autoconfigure模块，打包后引入即可自动装配
 
8、SpringCloud 和 SpringBoot 区别
- SpringBoot：快速开发脚手架，简化Spring应用配置，内置Web容器，专注单体应用开发
- SpringCloud：微服务全家桶，基于SpringBoot，提供服务注册、网关、分布式事务、限流等微服务组件，解决分布式问题
- 关系：SpringCloud依赖SpringBoot
 
9、SpringCloud Gateway 工作流程
1. 客户端请求到达Gateway
2. 路由：根据断言匹配目标服务
3. 过滤：执行全局过滤器、路由过滤器（鉴权、限流、日志、跨域）
4. 负载均衡转发到下游微服务
5. 接收响应，执行后置过滤器，返回给客户端
 
10、Seata AT模式原理
无侵入分布式事务，二阶段提交
1. 一阶段：执行业务SQL，记录undo_log回滚日志，提交本地事务，释放锁
2. 二阶段提交：删除undo_log日志
3. 二阶段回滚：根据undo_log生成补偿SQL，回滚数据，删除日志
 
11、SpringMVC 请求处理流程
1. 前端请求 → DispatcherServlet 前端控制器
2. HandlerMapping：匹配请求路径，找到处理器
3. HandlerAdapter：执行Controller方法
4. 业务逻辑处理，返回ModelAndView
5. ViewResolver：解析视图
6. 渲染视图，返回响应
 
12、MySQL索引为什么用B+Tree而不是B-Tree
1. 磁盘IO更少：B+Tree非叶子节点不存数据，只存索引，树高更低，IO次数更少
2. 范围查询更快：叶子节点链表相连，范围查询直接遍历链表
3. 排序优化：叶子节点有序，全表扫描、排序查询效率更高
4. 聚簇索引适配：主键有序存储，完美适配InnoDB聚簇索引
 
13、聚簇索引 & 非聚簇索引
- 聚簇索引：主键索引，叶子节点存整行数据，一个表只能有1个
- 非聚簇索引：二级索引，叶子节点存主键值，一个表可以有多个
- 查询逻辑：二级索引→主键→回表查询整行数据
 
14、回表 & 索引覆盖
- 回表：二级索引查询到主键，再通过主键去聚簇索引查询整行数据
- 索引覆盖：查询字段全部在二级索引中，无需回表，性能最优
 
15、事务ACID & 隔离级别
ACID
- 原子性(Atomicity)：事务不可分割，要么全成要么全败
- 一致性(Consistency)：事务前后数据完整性一致
- 隔离性(Isolation)：事务之间互不干扰
- 持久性(Durability)：提交后数据永久生效
    隔离级别（由低到高）
    1. 读未提交：脏读、不可重复读、幻读
    2. 读已提交：解决脏读，存在不可重复读、幻读
    3. 可重复读(InnoDB默认)：解决脏读、不可重复读，存在幻读
    4. 串行化：全部解决，性能最低
 
16、MVCC 原理
多版本并发控制，InnoDB实现隔离级别的核心
1. 每行数据隐藏字段：DB_TRX_ID(事务ID)、DB_ROLL_PTR(回滚指针)
2. 每次修改生成undo_log，通过回滚指针串联版本链
3. ReadView：记录活跃事务ID，通过可见性规则判断数据版本是否可见
4. 实现无锁读，提升并发性能
 
17、binlog / redo log / undo log 区别
- redo log：崩溃恢复，保证事务持久性，物理日志，循环写入
- undo log：事务回滚、MVCC，保证原子性，逻辑日志
- binlog：主从复制、数据恢复，记录SQL逻辑，归档存储
 
18、Redis数据类型 & 场景
1. String：缓存、计数器、分布式ID
2. Hash：存储对象、购物车
3. List：消息队列、栈、排行榜
4. Set：去重、交集/并集/差集、好友关系
5. ZSet：有序排行榜、延时队列
6. Stream：消息队列，支持消费确认
 
19、Redis持久化机制
- RDB：定时全量快照，恢复快，可能丢数据
- AOF：记录写命令，追加日志，丢数据少，文件大恢复慢
- 混合持久化：RDB+AOF结合，兼顾速度与安全
 
20、Redis过期删除策略
1. 惰性删除：访问时判断过期，删除数据
2. 定期删除：定时随机抽样删除过期key
3. 内存淘汰：内存满时触发，如LRU、LFU、随机删除
 
21、缓存穿透/击穿/雪崩 解决方案
- 穿透：查询不存在数据 → 布隆过滤器、缓存空值、接口校验
- 击穿：热点key过期 → 互斥锁、永不过期、逻辑过期
- 雪崩：大量key同时过期/Redis宕机 → 过期时间随机、Redis集群、熔断降级
 
22、Redis分布式锁 & 注意事项
实现
 SET key value NX EX 过期时间 
注意事项
1. 锁超时时间 > 业务执行时间
2. 解锁必须Lua脚本，保证原子性
3. 主从切换可能丢锁，用Redlock
4. 避免死锁，设置过期时间
 
24、Redis单线程为什么快
1. 纯内存操作，无磁盘IO
2. 单线程，无线程切换、锁竞争开销
3. IO多路复用，epoll模型，高并发连接
4. 数据结构高效，底层自定义编码优化
 
25、Redis集群实现
1. 主从复制：一主多从，读写分离
2. 哨兵Sentinel：监控、自动故障转移、高可用
3. Cluster集群：16384槽位，哈希分片，主从节点，水平扩容
 
26、Young GC / Full GC / Old GC 触发条件
- Young GC：Eden区满，触发Minor GC
- Old GC：老年代满，触发Major GC
- Full GC：元空间满、老年代满、System.gc()、空间分配担保失败
 
27、双亲委派模型 & 设计原因
流程
1. 类加载器收到请求，委托父类加载
2. 父类无法加载，子类自行加载
3. 层级：启动类加载器→扩展类加载器→应用类加载器→自定义加载器
    设计原因
    1. 安全：防止核心类被篡改
    2. 复用：类只加载一次，节省内存
 
28、CPU飙高 & OOM排查
CPU飙高
1. top定位高CPU线程
2. jstack导出线程栈
3. 转换16进制线程ID，定位死循环、锁竞争
    OOM排查
    1. jmap导出堆快照
    2. MAT分析大对象、内存泄漏
    3. 定位未释放资源、集合溢出
 
29、Kafka vs RabbitMQ 核心区别
1. 架构：Kafka分区+副本；RabbitMQ交换机+队列
2. 性能：Kafka吞吐量极高，适合大数据；RabbitMQ低延迟，适合业务消息
3. 可靠性：Kafka持久化强；RabbitMQ事务、死信队列完善
4. 场景：Kafka日志、大数据；RabbitMQ业务异步、解耦
 
30、RabbitMQ消息路由 & 交换机类型
路由流程
生产者→交换机→路由键→绑定队列→消费者
交换机类型
1. Direct：精准匹配路由键
2. Fanout：广播，全部队列接收
3. Topic：模糊匹配，通配符
4. Headers：参数匹配
 
31、Kafka AR / ISR / OSR & Leader选举
- AR：所有副本集合
- ISR：与Leader同步的副本集合
- OSR：落后于Leader的副本集合
    选举保障
    1. 必须ISR内副本才能当选Leader
    2. 选举优先选ISR中最接近Leader的副本，保证数据不丢
 
32、RAG知识库检索准确率优化
1. 文本分块：合理切块，保留语义完整性
2. 向量化优化：选择适配业务的Embedding模型
3. 重排序：检索后Rerank，提升相关性
4. 关键词+向量混合检索
5. 知识清洗：去重、过滤低质量文档
6. Prompt优化：精准引导大模型回答
 
33、Docker & K8s常用命令
Docker
bash

# 镜像
docker pull/build/rmi/images

# 容器
docker run/start/stop/exec/ps

# 日志
docker logs
 
K8s
bash
kubectl get pod/deployment/service
kubectl describe
kubectl logs
kubectl apply/delete
 
 
34、MyBatis & MyBatis-Plus区别
1. MyBatis：原生框架，手写XML，灵活度高，代码量大
2. MyBatis-Plus：增强工具，CRUD封装，无XML，代码生成器，简化开发
3. 关系：MP基于MyBatis，不改变原生逻辑
 
三、实际项目应用
1、支付场景设计
1. 流程：创建订单→生成支付单→调用第三方支付→异步回调→更新订单→对账
2. 技术：SpringCloud、Seata分布式事务、Redis幂等、MQ异步、签名验签、定时任务对账、熔断降级
 
2、支付无返回状态处理
1. 前端：轮询查询支付状态
2. 后端：定时任务补偿查询第三方接口
3. MQ：回调消息重试、死信队列
4. 幂等：保证状态更新不重复
 
3、JVM调优
1. 内存参数：Xmx/Xms新生代/老年代比例
2. 垃圾回收器：G1/ZGC，低延迟
3. 优化点：内存泄漏排查、GC频率优化、元空间调优
4. 工具：jmap/jstack/jstat/Arthas
 
4、慢SQL定位优化
1. 定位：慢查询日志、Explain执行计划、监控平台
2. 优化：
    - 加索引、联合索引
    - 避免select *、函数操作、隐式转换
    - 分页优化、分库分表
    - 读写分离
 
5、熔断/降级/限流 区别 & 实现
- 限流：限制请求量，保护系统，Sentinel/Guava RateLimiter
- 熔断：依赖服务故障，快速失败，避免雪崩
- 降级：非核心业务降级，保障核心业务
 
6、OOM排查解决
1. 导出堆dump，MAT分析
2. 定位大对象、未关闭连接、集合内存泄漏
3. 优化代码、增加内存、分批处理、资源关闭
 
7、多线程CPU频繁切换优化
1. 减少线程数量，合理设置线程池参数
2. 线程绑定CPU亲和性
3. 减少锁竞争，分段锁、CAS、无锁编程
4. 避免频繁创建销毁线程
 
8、消息不丢不重复 & 积压处理
不丢消息
- 生产者：事务消息、重试机制
- 服务端：持久化、集群
- 消费者：手动ACK
    不重复消费
- 幂等设计：唯一键、Redis幂等、业务去重
    消息积压
    1. 临时扩容消费者
    2. 死信队列处理失败消息
    3. 限流、削峰填谷
 
9、智能客服系统设计
1. 流程：用户提问→意图识别→知识库检索(RAG)→大模型生成→答案返回
2. 技术：向量数据库、Embedding、RAG、大模型API、对话管理、知识库管理
 
10、线上系统问题排查
1. 监控告警：日志、指标、链路追踪
2. 日志排查：ELK、SkyWalking
3. 工具定位：Arthas、jstack、jmap
4. 分层排查：网络→应用→数据库→中间件
 
11、线上性能瓶颈优化
答题模板
遇到接口响应慢，先通过SkyWalking定位到慢SQL，用Explain发现缺少索引，添加联合索引；
发现Redis缓存击穿，添加互斥锁+逻辑过期；
线程池参数不合理，调整核心线程数、队列长度；
最终接口响应从500ms优化到50ms。
 
12、项目遇到的问题 & 解决
答题模板
项目中遇到分布式事务不一致问题，使用Seata AT模式解决；
遇到缓存雪崩，设置随机过期时间+Redis集群；
遇到消息重复消费，实现接口幂等性；
遇到OOM，定位内存泄漏，优化代码释放资源。
