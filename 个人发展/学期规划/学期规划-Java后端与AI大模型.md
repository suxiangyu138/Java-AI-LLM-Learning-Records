# Java 后端 + AI 大模型 — 技术攻坚与项目落地

> 目标：熟练 SpringBoot、MyBatis、MySQL、Redis，完成可面试后端项目，刷完高频算法题，具备春招实习面试能力。

---

## 一、技术全景图

```
┌─────────────────────────────────────────────┐
│                Java 后端技术栈               │
│                                             │
│  基础层：Java SE（集合、IO、多线程、JVM）      │
│  框架层：Spring / SpringBoot / MyBatis        │
│  数据层：MySQL + Redis + 消息队列              │
│  工程层：Git / Maven / Linux / Docker          │
│  项目层：完整可面试项目                        │
│                                             │
├─────────────────────────────────────────────┤
│                AI 大模型技术栈                │
│                                             │
│  理论层：Transformer / LLM 原理               │
│  应用层：Prompt Engineering / RAG / Agent     │
│  开发层：Spring AI / LangChain4j              │
│                                             │
└─────────────────────────────────────────────┘
```

---

## 二、Java SE 核心知识（地基）

### 2.1 必须掌握的清单

```
Java 基础
├── 面向对象三大特性（封装、继承、多态）
├── 接口 vs 抽象类
├── String / StringBuilder / StringBuffer
├── 异常体系（Checked vs Unchecked）
├── 泛型（类型擦除、通配符、PECS 原则）
├── 注解（@Override / @Deprecated / 自定义注解 + 反射）
├── 反射（Class 对象、动态代理）
└── Lambda + Stream API（函数式编程）

集合框架（面试重灾区）
├── List：ArrayList（数组）/ LinkedList（双向链表）/ Vector（线程安全）
├── Set：HashSet（HashMap 实现）/ TreeSet（红黑树）/ LinkedHashSet
├── Map：
│   ├── HashMap：数组+链表+红黑树，1.7 头插法→1.8 尾插法，扩容机制
│   ├── ConcurrentHashMap：分段锁→synchronized+CAS (1.8)
│   ├── TreeMap：红黑树
│   └── LinkedHashMap：双向链表维护插入顺序
├── Collections 工具类
└── 排序：Comparable vs Comparator

多线程与并发（高级）
├── 线程创建：Thread / Runnable / Callable + Future / 线程池
├── 线程状态：NEW → RUNNABLE → BLOCKED → WAITING → TIMED_WAITING → TERMINATED
├── 锁：
│   ├── synchronized（偏向锁→轻量锁→重量锁，锁升级过程）
│   ├── ReentrantLock（AQS 原理，公平/非公平）
│   ├── ReadWriteLock / StampedLock
│   └── synchronized vs Lock
├── 线程池：
│   ├── 核心参数：corePoolSize / maxPoolSize / keepAlive / BlockingQueue
│   ├── 执行流程：核心线程→工作队列→最大线程→拒绝策略
│   ├── 四种拒绝策略（Abort/CallerRuns/Discard/DiscardOldest）
│   └── Executors 不建议（OOM 风险），用 ThreadPoolExecutor 手动创建
├── volatile：保证可见性 + 禁止指令重排（单例双重检查）
├── CAS + 原子类（AtomicInteger 等）
├── ThreadLocal（线程本地变量，注意内存泄漏）
└── 并发工具类：CountDownLatch / CyclicBarrier / Semaphore

JVM（面试必问）
├── JVM 内存结构：
│   ├── 线程共享：堆（对象）、方法区/元空间（类信息、常量池）
│   ├── 线程私有：程序计数器、虚拟机栈（栈帧）、本地方法栈
│   └── 1.7 方法区=永久代 vs 1.8 方法区=元空间（直接内存）
├── 类加载：加载→验证→准备→解析→初始化
├── 双亲委派模型：Bootstrap→Extension→Application→Custom ClassLoader
├── 垃圾回收：
│   ├── 如何判断对象可回收：引用计数法、可达性分析（GC Roots）
│   ├── 引用类型：强/软/弱/虚
│   ├── 回收算法：标记-清除（碎片）/标记-整理/复制（新生代）
│   ├── 分代回收：新生代(Eden:S0:S1=8:1:1) + 老年代
│   ├── 垃圾收集器：Serial / Parallel / CMS / G1（JDK9+ 默认）/ ZGC（低延迟）
│   └── Minor GC / Major GC / Full GC 区别
└── JVM 调优参数：-Xms / -Xmx / -Xmn / -XX:+UseG1GC 等

IO
├── BIO（阻塞 IO）：一连接一线程
├── NIO（非阻塞 IO）：Channel + Buffer + Selector 多路复用
└── AIO（异步 IO）：回调机制
```

---

## 三、Spring 全家桶

### 3.1 Spring Framework

```
IoC（控制反转）
├── IoC 容器：BeanFactory vs ApplicationContext
├── Bean 生命周期：实例化 → 属性填充 → Aware → BeanPostProcessor → init → 就绪 → 销毁
├── 依赖注入：构造器注入（推荐）/ Setter 注入 / @Autowired
├── Bean 作用域：singleton（默认）/ prototype / request / session
├── 循环依赖：三级缓存机制（singletonFactories → earlySingletonObjects → singletonObjects）
└── @Autowired vs @Resource（按类型 vs 按名称+类型）

AOP（面向切面编程）
├── 核心概念：切面/切点/通知/连接点/织入
├── 通知类型：@Before @After @Around @AfterReturning @AfterThrowing
├── 实现原理：JDK 动态代理（接口）+ CGLIB（类）
├── 应用场景：事务管理、日志、权限校验、性能监控
└── 动态代理 vs 静态代理

事务管理
├── ACID 四大特性
├── 事务传播行为（7 种）：REQUIRED（默认，有就用/没有就新建）
├── 事务隔离级别（5 种）：DEFAULT / RU / RC / RR / Serializable
├── @Transactional 原理：AOP 代理
└── 失效场景：非 public 方法、同类内部调用(this.)、异常被 catch 吞掉、数据库引擎不支持
```

### 3.2 SpringBoot

```
核心机制
├── 自动配置：@SpringBootApplication = @Configuration + @EnableAutoConfiguration + @ComponentScan
├── starter 机制：spring-boot-starter-web / starter-data-redis 等即插即用
├── 内嵌 Tomcat/Jetty：直接 java -jar 运行
├── 外部化配置：application.yml / Properties / 环境变量 / 命令行参数优先级
├── Actuator 健康监控：/actuator/health / /metrics / /loggers
├── 统一异常处理：@RestControllerAdvice + @ExceptionHandler
├── 拦截器 vs 过滤器：
│   ├── 过滤器（Filter）：Java Servlet 层面，在请求到达 Servlet 前处理
│   └── 拦截器（Interceptor）：Spring 层面，在 Controller 处理前后
└── 统一响应封装（Result<T> 类）

SpringBoot 启动流程
@SpringBootApplication
  → SpringApplication.run()
    → 创建 ApplicationContext
      → 加载 AutoConfiguration（spi 机制 spring.factories）
        → refresh()（Bean 生命周期）
          → 内嵌 Tomcat 启动
```

### 3.3 Spring AI / LangChain4j（Java 与大模型整合）

```
Spring AI（Spring 官方）
├── ChatClient：统一调用 OpenAI / Claude / 通义千问 等
├── EmbeddingClient：文本向量化
├── VectorStore：对接向量数据库
├── DocumentReader：文档解析（PDF/HTML/JSON）
├── ETL Pipeline：文档 → 分块 → Embedding → VectorStore
├── Prompt Template：模板化管理 Prompt
├── Function Calling：自动转换 @Tool 方法
└── RAG 一条龙支持

LangChain4j（社区方案）
├── ChatLanguageModel：多模型支持
├── EmbeddingModel + EmbeddingStore
├── RAG 完整支持
├── Agent (Tools + Memory)
└── 与 Quarkus/SpringBoot 集成
```

---

## 四、MyBatis / MyBatis-Plus

```
核心概念
├── 配置：SqlSessionFactory → SqlSession → Mapper
├── XML 映射 vs 注解映射
├── #{} vs ${}：预编译防注入 vs 直接拼接（有注入风险）
├── 动态 SQL：if / choose / foreach / where / trim / set
├── 插件机制：拦截 Executor/StatementHandler/ParameterHandler/ResultSetHandler
├── 缓存：一级缓存（SqlSession 级别，默认开启）/ 二级缓存（Mapper 级别，需配置）
├── 延迟加载：association / collection 的 fetchType="lazy"
├── 结果映射：resultMap 处理复杂对象关系
└── PageHelper 分页插件

MyBatis-Plus 增强
├── BaseMapper 通用 CRUD
├── 条件构造器：QueryWrapper / LambdaQueryWrapper
├── 分页插件（更简单）
└── 自动填充（@TableField fill）
```

---

## 五、MySQL（数据库核心）

```
设计层面
├── ER 图设计 → 建表语句
├── 三范式（到 3NF 即可）
├── 字段类型选择（int/bigint/varchar/text/datetime/decimal）
├── 索引设计（单列索引/联合索引，最左前缀原则）
└── 分库分表思想（垂直分库/水平分表）

SQL 编写
├── 复杂查询（多表 JOIN + 子查询）
├── GROUP BY + HAVING + 聚合函数
├── 窗口函数（ROW_NUMBER/RANK/DENSE_RANK/LAG/LEAD）
├── 分页查询（LIMIT offset, size 及深分页优化）
└── 优化（EXPLAIN 分析、避免 SELECT *、合理使用索引）

事务与锁
├── 事务使用场景 + @Transactional
├── 行锁 vs 表锁 vs 间隙锁（InnoDB）
├── 死锁（如何产生、如何避免）
└── 乐观锁（version 字段）+ 悲观锁（SELECT FOR UPDATE）
```

---

## 六、Redis

```
五种基础数据类型
├── String：缓存对象、计数器、分布式锁
│   命令：SET / GET / INCR / SETEX / SETNX
├── List：消息队列、最新列表
│   命令：LPUSH / RPUSH / LPOP / RPOP / LRANGE
├── Set：标签、共同好友、抽奖去重
│   命令：SADD / SREM / SINTER / SUNION / SPOP
├── ZSet：排行榜、优先级队列
│   命令：ZADD / ZRANK / ZRANGE / ZSCORE
└── Hash：存储对象、购物车
    命令：HSET / HGET / HGETALL / HDEL

高级特性
├── 持久化：
│   ├── RDB（快照，适合备份，可能丢最近数据）
│   └── AOF（日志，数据安全，文件大恢复慢）
├── 过期策略：定期删除 + 惰性删除
├── 内存淘汰策略（8 种）：
│   allkeys-lru：最常用
│   volatile-lru：只淘汰设了过期时间的
│   noeviction：不淘汰，内存满就报错（默认）
├── 缓存问题：
│   ├── 缓存穿透：布隆过滤器 + 缓存空值
│   ├── 缓存击穿：互斥锁（SETNX）+ 逻辑过期（热点 key）
│   └── 缓存雪崩：过期时间 + 随机值、多级缓存、限流
├── 分布式锁：
│   SETNX + EXPIRE（原子执行）
│   Redisson（Watch Dog 自动续期，红锁算法）
│   Lua 脚本保证原子性
├── 主从 + 哨兵：读写分离，高可用
├── 集群（Cluster）：分片存储，hash slot 16384 个槽位
└── Pipeline：批量执行命令，减少 RTT

通用设计
├── 缓存与数据库一致性：
│   Cache Aside（先更新 DB → 再删除缓存）最常用
│   延迟双删、订阅 binlog + 消息队列异步更新
├── Key 设计规范：业务名:对象名:id:属性
└── Big Key 问题与解决：拆分为多个小 key
```

---

## 七、高频算法题（面试刷题清单）

### 按考点分类

```
数据结构类（必刷）
├── 数组：两数之和、三数之和、移动零、缺失的第一个正数
├── 链表：反转链表、环形链表、合并有序链表、链表倒数第 K 个
├── 栈/队列：有效括号、用栈实现队列、最小栈
├── 哈希表：字母异位词分组、最长连续序列
├── 二叉树：层序遍历、最大深度、对称二叉树、路径总和
├── 堆：数组中的第 K 个最大元素、前 K 个高频元素

算法思想类（必刷）
├── 双指针：接雨水、盛最多水的容器
├── 滑动窗口：无重复字符的最长子串、找到字符串中所有字母异位词
├── 二分查找：搜索旋转排序数组、在排序数组中查找元素的第一个和最后一个位置
├── 动态规划：爬楼梯、最大子数组和、最长递增子序列、打家劫舍
├── 贪心：跳跃游戏、分发饼干
├── 回溯：全排列、子集、组合总和、N 皇后
└── 分治：归并排序

高频 TOP 20（面试频率最高）
 1. 两数之和（哈希表）
 2. 有效的括号（栈）
 3. 合并两个有序链表（链表 + 递归）
 4. 反转链表（双指针）
 5. 无重复字符的最长子串（滑动窗口）
 6. 最长回文子串（中心扩展/DP）
 7. 二分查找（闭区间/开区间）
 8. 接雨水（双指针/单调栈）
 9. 三数之和（排序 + 双指针）
10. 环形链表（快慢指针）
11. 岛屿数量（DFS/BFS）
12. 二叉树层序遍历（BFS 队列）
13. 全排列（回溯 vis[i]）
14. 最大子数组和（DP/Kadane）
15. 爬楼梯（斐波那契 DP）
16. 相交链表（双指针 互相走过对方的路）
17. 排序数组 TopK（堆/快排 partition）
18. LRU 缓存（HashMap + 双向链表）
19. 买股票的最佳时机（一次买卖/多次买卖）
20. 最长递增子序列（DP + 二分优化）
```

---

## 八、项目实战：可面试项目设计

### 8.1 经典项目：AI 智能题库系统（Java 后端 + AI 大模型融合）

```
技术栈：
SpringBoot + MyBatis-Plus + MySQL + Redis + RabbitMQ + Spring AI / LangChain4j
+ Vue3 前端（可选）+ Docker 部署

功能模块：
├── 用户模块
│   ├── 注册/登录（JWT 认证）
│   ├── 权限管理（RBAC 角色-权限模型）
│   └── 个人中心
│
├── 题库管理（核心业务）
│   ├── 题目 CRUD（分页、筛选、搜索）
│   ├── 题目分类/标签
│   ├── 题库导入导出（Excel 批量处理）← 考察 IO/并发
│   └── 题目收藏/点赞
│
├── AI 智能出题（大模型集成）★ 亮点
│   ├── AI 自动生成题目（LLM）
│   ├── AI 题目难度评估（分析复杂度）
│   ├── AI 智能讲解（RAG：题目知识库 → 相似题讲解）
│   └── AI 对话辅导（Agent 模式）
│
├── 刷题功能
│   ├── 随机组卷（按难度/知识点）
│   ├── 答题记录（正确率统计）
│   ├── 错题本（自动收录）
│   └── 排行榜（Redis ZSet 实现）
│
├── 性能优化
│   ├── Redis 缓存热点题目
│   ├── 高并发处理（限流 + 异步 + 连接池）
│   └── SQL 优化（索引 + 慢查询监控）
│
└── 部署运维
    ├── Docker Compose 一键部署
    ├── Nginx 反向代理
    └── CI/CD (GitHub Actions 自动构建)
```

### 8.2 项目亮点话术

```
面试时可以说：

1. "我在项目中集成了 Spring AI，调用 LLM 实现了
   AI 出题和智能讲解功能，而不是纯 CRUD"

2. "我在 Redis 上做了缓存方案，用布隆过滤器解决缓存穿透，
   用互斥锁解决热点 key 的缓存击穿"

3. "我用了 RabbitMQ 异步处理题库导入通知，解耦了
   上传和处理流程，提升用户体验"

4. "项目部署我用了 Docker Compose 编排，把前后端、
   数据库、缓存、消息队列都容器化了"

5. "我的 SQL 都用 EXPLAIN 分析过，对高频查询建了
   合理的联合索引，避免全表扫描"
```

---

## 九、面试准备清单

### 9.1 技术面试能力 Checklist

```
Java 基础
□ 能画 HashMap 扩容流程图
□ 能手写线程池 7 参数
□ 能说出 ConcurrentHashMap 1.7 vs 1.8 区别
□ 能手写生产者-消费者
□ 能讲清楚 volatile 做的事情
□ 能画出 JVM 内存结构图
□ 能讲清楚 CMS/G1 回收器的区别
□ 能说出类加载双亲委派机制

Spring
□ 能说出 Bean 生命周期
□ 能解释循环依赖怎么解决（三级缓存）
□ 能说出 AOP 两种代理方式
□ 能说出事务失效的 5 种场景
□ 能讲清楚 SpringBoot 自动配置原理

MySQL
□ 能画出 B+ 树结构
□ 能说清楚最左前缀原则
□ 能讲清楚 MVCC 怎么实现
□ 能写出 EXPLAIN 分析
□ 能说出慢查询优化步骤

Redis
□ 能说出 5 种基础数据类型 + 应用场景
□ 能说清楚缓存穿透/击穿/雪崩
□ 能说清楚 RDB vs AOF
□ 能写 SETNX 实现分布式锁
□ 能说出内存淘汰策略

算法
□ LeetCode 刷题 150+ 道
□ Hot 100 刷 2-3 遍
□ 常见数据结构熟练手写
□ 动态规划能独立做中等题

项目
□ 能把项目架构图画出来
□ 能说出 3 个项目中遇到的问题+解决方案
□ 能解释为什么选择某个技术栈
□ 能讲清楚高并发场景怎么设计
```

### 9.2 面试节奏建议

```
大二下 3-5 月：打基础（Java SE + 数据结构 + SQL）
大二下 6-7 月：学框架（SpringBoot + MyBatis + Redis）+ 做项目
大二暑假 7-8 月：刷高频算法题 + 项目完善 + 简历打磨
大三上 9-10 月：投简历 → 面试 → offer
（秋招正式批，也是春招实习黄金窗口）
```

---

> **核心心法**：框架学多了记不住？每学一个技术重复三问——(1) 为什么需要它？(2) 它的核心机制是什么（画图）？(3) 项目中怎么用它？三问通了才是真正懂了。
