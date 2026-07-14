# Java后端开发理论五大件

> Java后端开发的知识体系庞大，但核心可归纳为五大理论模块。本文为这五大件提供了一份精炼的学习大纲，涵盖计算机网络、操作系统、数据结构与算法、数据库、计算机组成原理。

## 📑 目录

- [一、计算机网络（后端核心）](#一计算机网络后端核心)
- [二、操作系统（后端高频）](#二操作系统后端高频)
- [三、数据结构与算法（面试核心）](#三数据结构与算法面试核心)
- [四、数据库（后端吃饭技能）](#四数据库后端吃饭技能)
- [五、计算机组成原理（后端几乎不用深学）](#五计算机组成原理后端几乎不用深学)
- [六、五大件交叉面试题示例](#六五大件交叉面试题示例)
- [📖 相关阅读](#📖-相关阅读)

---

## 一、计算机网络（后端核心）

> 网络是后端开发的基石，理解网络协议栈对于接口开发、问题排查和架构设计至关重要。

### 知识清单

| 序号 | 知识点 | 掌握程度 | 面试频率 | 详细说明 |
|------|--------|---------|---------|---------|
| 1 | **网络模型** | 理解 | ⭐⭐⭐ | TCP/IP 四层 vs OSI 七层，只需记住四层：应用层→传输层→网络层→链路层 |
| 2 | **TCP** | 精通 | ⭐⭐⭐⭐⭐ | 三次握手（SYN/SYN-ACK/ACK）、四次挥手（FIN/ACK）、可靠传输（确认+重传）、流量控制（滑动窗口）、拥塞控制（慢启动/拥塞避免/快重传/快恢复） |
| 3 | **HTTP/1.1** | 精通 | ⭐⭐⭐⭐⭐ | 请求方法（GET/POST/PUT/DELETE/PATCH）、状态码（2xx/3xx/4xx/5xx）、Header（Content-Type/Authorization/Cache-Control）、长连接（Keep-Alive）、管线化 |
| 4 | **HTTP/2 & HTTP/3** | 了解 | ⭐⭐⭐ | 多路复用、头部压缩（HPACK）、服务器推送、QUIC（基于 UDP） |
| 5 | **HTTPS** | 理解 | ⭐⭐⭐⭐ | TLS 握手流程、对称/非对称加密、数字证书与 CA、证书链验证 |
| 6 | **Cookie / Session / Token** | 精通 | ⭐⭐⭐⭐⭐ | Cookie 属性（Domain/Path/HttpOnly/Secure/SameSite）、Session 存储（Redis）、JWT 结构（Header.Payload.Signature）、OAuth2.0 流程 |
| 7 | **DNS** | 理解 | ⭐⭐⭐ | 解析流程（浏览器缓存→OS缓存→本地DNS→根DNS→顶级DNS→权威DNS）、A/AAAA/CNAME/MX/NS 记录类型、CDN 与 DNS 的关系 |
| 8 | **跨域 CORS** | 理解 | ⭐⭐⭐ | 同源策略、简单请求 vs 预检请求（OPTIONS）、Access-Control-Allow-Origin/Credentials |
| 9 | **WebSocket** | 了解 | ⭐⭐ | 与 HTTP 的区别、全双工通信、心跳保活、适用场景（IM/实时推送） |
| 10 | **常见网络问题排查** | 掌握 | ⭐⭐⭐⭐ | 超时排查（connectTimeout/readTimeout）、重试与幂等性、SYN Flood / TIME_WAIT 过多、TCP 粘包/拆包 |

### Java 后端网络编程必知

| 概念 | 说明 |
|------|------|
| BIO / NIO / AIO | 阻塞 IO → 非阻塞 IO（Selector + Channel）→ 异步 IO，Netty 基于 NIO |
| Netty | Java 网络编程事实标准，Reactor 模式，EventLoopGroup + Pipeline |
| TCP 粘包拆包 | 原因：TCP 流式传输无边界；解决：定长/分隔符/长度字段/Netty 编解码器 |
| 连接池 | HTTP 连接池（HttpClient）、数据库连接池（HikariCP）、Redis 连接池（Lettuce） |

---

## 二、操作系统（后端高频）

> 操作系统知识直接影响对并发编程、内存管理和I/O模型的理解深度。

### 知识清单

| 序号 | 知识点 | 掌握程度 | 面试频率 | 详细说明 |
|------|--------|---------|---------|---------|
| 1 | **进程与线程** | 精通 | ⭐⭐⭐⭐⭐ | 区别（资源/调度/通信/开销）、状态转换（就绪/运行/阻塞/创建/终止）、上下文切换成本、协程（用户态线程） |
| 2 | **并发与锁** | 精通 | ⭐⭐⭐⭐⭐ | synchronized / Lock / AQS / CAS / volatile、死锁四大条件（互斥/持有并等待/不可剥夺/循环等待）、活锁与饥饿 |
| 3 | **内存管理** | 理解 | ⭐⭐⭐⭐ | 虚拟内存（页表/MMU/TLB）、分页与分段、堆与栈的区别、内存映射（mmap）、Java 堆内存结构（年轻代/老年代/Metaspace） |
| 4 | **I/O 模型** | 理解 | ⭐⭐⭐⭐ | BIO（阻塞）→ NIO（非阻塞 + 多路复用 select/poll/epoll）→ AIO（异步），epoll 的 LT 与 ET 模式 |
| 5 | **文件系统** | 了解 | ⭐⭐ | inode、硬链接 vs 软链接、文件描述符、Page Cache 与脏页回写 |
| 6 | **Linux 常用命令** | 掌握 | ⭐⭐⭐⭐⭐ | 进程：`ps/top/htop`；网络：`netstat/ss/tcpdump`；文件：`find/grep/awk/sed`；磁盘：`df/du/iostat`；日志排查：`tail/less/journalctl` |
| 7 | **中断与系统调用** | 了解 | ⭐⭐ | 用户态→内核态切换、系统调用开销、零拷贝（sendfile / mmap） |

### Java 并发编程核心

| 概念 | 说明 |
|------|------|
| 线程池 | `ThreadPoolExecutor` 七大参数、四种拒绝策略、合适的线程数估算 |
| synchronized 锁升级 | 无锁 → 偏向锁 → 轻量级锁 → 重量级锁（JDK 15+ 默认禁用偏向锁） |
| AQS | `AbstractQueuedSynchronizer`，CLH 队列，`ReentrantLock`/`Semaphore`/`CountDownLatch` 的基础 |
| volatile | 保证可见性 + 禁止指令重排（内存屏障），不保证原子性 |
| ThreadLocal | 线程本地变量，内存泄漏风险（key 为弱引用），`InheritableThreadLocal` 与线程池传递 |

---

## 三、数据结构与算法（面试核心）

> 算法能力是技术面试的考察重点，掌握常见数据结构和算法模板是通关关键。

### 知识清单

| 序号 | 知识点 | 掌握程度 | 面试频率 | LeetCode 题量建议 |
|------|--------|---------|---------|-----------------|
| 1 | **数组与链表** | 精通 | ⭐⭐⭐⭐⭐ | 数组 30+ 题，链表 20+ 题 |
| 2 | **栈与队列** | 精通 | ⭐⭐⭐⭐ | 单调栈、单调队列各 5 题 |
| 3 | **哈希表** | 精通 | ⭐⭐⭐⭐⭐ | 两数之和系列、字母异位词 10+ 题 |
| 4 | **二叉树** | 精通 | ⭐⭐⭐⭐⭐ | 遍历（前/中/后/层）、路径、LCA、序列化 30+ 题 |
| 5 | **堆（优先队列）** | 掌握 | ⭐⭐⭐⭐ | TopK 系列、合并 K 个链表 10+ 题 |
| 6 | **图** | 掌握 | ⭐⭐⭐ | BFS/DFS、拓扑排序、最短路径（Dijkstra） 10+ 题 |
| 7 | **排序算法** | 理解思想 | ⭐⭐ | 快排/归并/堆排——手写快排和归并即可 |
| 8 | **二分查找** | 精通 | ⭐⭐⭐⭐⭐ | 二分模板（左边界/右边界）、旋转数组 15+ 题 |
| 9 | **双指针 / 滑动窗口** | 精通 | ⭐⭐⭐⭐⭐ | 三数之和、接雨水、无重复最长子串 15+ 题 |
| 10 | **动态规划** | 掌握 | ⭐⭐⭐⭐⭐ | 背包系列、打家劫舍、股票买卖、子序列 40+ 题 |
| 11 | **回溯** | 掌握 | ⭐⭐⭐⭐ | 全排列、组合总和、N 皇后、数独 15+ 题 |
| 12 | **贪心** | 了解 | ⭐⭐⭐ | 区间调度、跳跃游戏 5+ 题 |
| 13 | **位运算** | 了解 | ⭐⭐ | 异或找单数、位图（BitMap/Bloom Filter） |

### Java 集合框架速查

| 接口 | 实现类 | 底层结构 | 特点 |
|------|--------|---------|------|
| List | ArrayList | 数组 | 随机访问 O(1)，尾部增删 O(1)，中间增删 O(n) |
| List | LinkedList | 双向链表 | 头尾增删 O(1)，不支持随机访问 |
| Map | HashMap | 数组+链表+红黑树 | O(1) 查找，JDK8+ 链表长度≥8 转红黑树 |
| Map | LinkedHashMap | HashMap + 双向链表 | 维护插入/访问顺序，实现 LRU |
| Map | TreeMap | 红黑树 | 有序，O(log n) |
| Map | ConcurrentHashMap | 分段锁（JDK7）→ CAS+synchronized（JDK8） | 线程安全 |
| Set | HashSet | HashMap | 基于 HashMap 实现 |
| Queue | PriorityQueue | 二叉堆（小顶堆默认） | O(log n) 插入删除，O(1) 获取最值 |
| Deque | ArrayDeque | 循环数组 | 两端操作 O(1)，替代 Stack 和 LinkedList 队列 |

---

## 四、数据库（后端吃饭技能）

> 数据库是后端开发的核心技能，从SQL编写到索引优化再到事务处理，每项都是日常工作的必备能力。

### 知识清单

| 序号 | 知识点 | 掌握程度 | 面试频率 | 详细说明 |
|------|--------|---------|---------|---------|
| 1 | **SQL 编写** | 精通 | ⭐⭐⭐⭐⭐ | JOIN（INNER/LEFT/RIGHT/FULL）、子查询、聚合函数、窗口函数（ROW_NUMBER/RANK/DENSE_RANK/LAG/LEAD）、UNION、CTE |
| 2 | **索引** | 精通 | ⭐⭐⭐⭐⭐ | B+ 树原理、聚簇索引 vs 非聚簇索引、最左前缀匹配、覆盖索引、索引下推（ICP）、回表、索引失效场景 |
| 3 | **事务 ACID** | 精通 | ⭐⭐⭐⭐⭐ | 原子性（undo log）、一致性、隔离性（MVCC + 锁）、持久性（redo log） |
| 4 | **隔离级别** | 精通 | ⭐⭐⭐⭐⭐ | 读未提交/读已提交/可重复读/串行化，脏读/不可重复读/幻读的定义与解决 |
| 5 | **MVCC** | 理解 | ⭐⭐⭐⭐ | ReadView、undo log 版本链、快照读 vs 当前读 |
| 6 | **锁机制** | 理解 | ⭐⭐⭐⭐ | 行锁（Record Lock/Gap Lock/Next-Key Lock）、表锁、意向锁、死锁检测 |
| 7 | **慢查询优化** | 掌握 | ⭐⭐⭐⭐ | EXPLAIN 分析（type/rows/Extra）、慢查询日志、SQL 改写、索引优化 |
| 8 | **分库分表** | 了解 | ⭐⭐⭐ | 垂直拆分 vs 水平拆分、分片键选择、ShardingSphere、跨库 JOIN/分页问题 |
| 9 | **主从复制** | 了解 | ⭐⭐⭐ | binlog（ROW/STATEMENT/MIXED）、异步/半同步复制、主从延迟处理 |
| 10 | **Redis 核心** | 掌握 | ⭐⭐⭐⭐⭐ | 5 种数据结构（String/Hash/List/Set/ZSet）、缓存穿透/击穿/雪崩、分布式锁（Redisson）、过期策略（惰性+定期）、淘汰策略（LRU/LFU/allkeys-lru）、持久化（RDB/AOF 混合） |

### MySQL 面试高频 SQL 模式

```sql
-- 1. 窗口函数：每个部门工资前三
SELECT dept, name, salary FROM (
  SELECT dept, name, salary,
         DENSE_RANK() OVER (PARTITION BY dept ORDER BY salary DESC) AS rk
  FROM employee
) t WHERE rk <= 3;

-- 2. JOIN + 聚合：订单最多的用户
SELECT u.name, COUNT(o.id) AS cnt
FROM users u LEFT JOIN orders o ON u.id = o.user_id
GROUP BY u.id ORDER BY cnt DESC LIMIT 10;

-- 3. 子查询：从未下过单的用户
SELECT name FROM users WHERE id NOT IN (
  SELECT DISTINCT user_id FROM orders
);
```

---

## 五、计算机组成原理（后端几乎不用深学）

> 只需理解与后端开发相关的核心概念，无需深入学习底层硬件细节。

### 知识清单

| 序号 | 知识点 | 掌握程度 | 面试频率 | 详细说明 |
|------|--------|---------|---------|---------|
| 1 | **CPU 缓存** | 理解 | ⭐⭐⭐ | L1/L2/L3 Cache、缓存行（Cache Line 通常 64 字节）、伪共享（False Sharing）与 `@Contended` 注解 |
| 2 | **内存体系** | 理解 | ⭐⭐ | 寄存器 → L1 → L2 → L3 → 内存 → 磁盘，速度差 100~1000 倍 |
| 3 | **缓存一致性** | 了解 | ⭐⭐ | MESI 协议（Modified/Exclusive/Shared/Invalid），volatile 与内存屏障 |
| 4 | **大小端** | 了解 | ⭐⭐ | 大端（高位低地址）vs 小端（低位低地址），网络字节序为大端 |
| 5 | **内存对齐** | 了解 | ⭐⭐ | 对齐规则、Java 对象头（Mark Word + Klass Pointer）、压缩指针 |
| 6 | **CPU 分支预测** | 了解 | ⭐ | 有序数组 > 无序数组遍历快（分支预测命中率高） |
| 7 | **指令集 / 汇编** | 不用学 | ⭐ | 后端开发无需深入，了解 JIT 编译即可 |

### 与 Java 后端的关联

| 组成原理概念 | Java 中的体现 |
|-------------|-------------|
| 缓存行（Cache Line） | `@Contended` 避免伪共享，Disruptor 高性能队列的设计基础 |
| 内存屏障 | `volatile`、`synchronized`、`Unsafe.loadFence/storeFence/fullFence` |
| 大小端 | `ByteBuffer.order(ByteOrder.BIG_ENDIAN/LITTLE_ENDIAN)` |
| 内存对齐 | 压缩指针（`-XX:+UseCompressedOops`），32G 堆内存限制 |
| CPU 乱序执行 | 指令重排序 → 单例模式必须用 `volatile`（DCL 问题） |

---

## 六、五大件交叉面试题示例

这些是典型的"一个面试题考多个领域"的高频题：

| 面试题 | 涉及领域 |
|--------|---------|
| "从浏览器输入 URL 到看到页面，发生了什么？" | 网络（DNS/TCP/HTTP/HTTPS）+ OS（进程调度/IO）+ 数据库（查询缓存） |
| "一条 SQL 执行很慢，怎么排查？" | 数据库（索引/锁/MVCC）+ OS（磁盘 IO/内存）+ 网络（连接池/超时） |
| "如何设计一个短链系统？" | 算法（哈希/Base62）+ 数据库（分库分表/缓存）+ 网络（重定向/301 vs 302） |
| "线程池的核心参数怎么设置？" | OS（CPU 核心数/上下文切换）+ 算法（排队论）+ 网络（IO 密集型 vs CPU 密集型） |
| "HashMap 为什么线程不安全？" | 数据结构（链表/红黑树）+ OS（并发/CPU 缓存）+ 组成原理（CAS/内存屏障） |

---

## 📖 核心要点速记

1. **网络**：TCP 三次握手/四次挥手 + HTTP/HTTPS 是绝对核心，出镜率最高
2. **操作系统**：进程线程 + 锁 + 内存管理 + IO 模型，面试高频
3. **算法**：LeetCode 200+ 题是基础线，Hot 100 + 剑指 Offer 优先刷
4. **数据库**：索引 + 事务 + 锁 + MVCC，写 SQL 像写作文一样熟练
5. **组成原理**：学够用就行——缓存一致性 + 大小端 + 内存屏障

---

## 📖 相关阅读

- [计算机导论-后端必备基础](./计算机导论-后端必备基础.md)
- [计算机五大件必学重点清单](./计算机五大件必学重点清单.md)
- [计算机「四大件」终极模块化精编](./计算机「四大件」终极模块化精编.md)
- [2024年和2025年计算机专业就业前景较好的城市](./2024年和2025年计算机专业就业前景较好的城市：.md)
