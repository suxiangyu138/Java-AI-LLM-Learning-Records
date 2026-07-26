# 06 - 缓存技术与Redis深度解析

> 从本地缓存到分布式缓存，从 Redis 底层原理到高可用架构，从缓存设计到企业最佳实践。

---

## 目录

1. [缓存架构全景](#1-缓存架构全景)
2. [本地缓存详解](#2-本地缓存详解)
3. [Redis 运行原理](#3-redis-运行原理)
4. [Redis 数据结构与底层实现](#4-redis-数据结构与底层实现)
5. [Redis 持久化机制](#5-redis-持久化机制)
6. [Redis 高可用与集群](#6-redis-高可用与集群)
7. [缓存设计模式](#7-缓存设计模式)
8. [缓存三大问题与解决方案](#8-缓存三大问题与解决方案)
9. [分布式锁详解](#9-分布式锁详解)
10. [缓存与数据库一致性](#10-缓存与数据库一致性)
11. [Redis 性能优化](#11-redis-性能优化)
12. [Spring Cache 抽象](#12-spring-cache-抽象)
13. [常见面试题深度解析](#13-常见面试题深度解析)

---

## 1. 缓存架构全景

### 1.1 多级缓存体系

```
┌─────────────────────────────────────────────────────────┐
│ 第一层：客户端缓存（浏览器 HTTP Cache、Service Worker）   │
├─────────────────────────────────────────────────────────┤
│ 第二层：CDN 缓存（边缘节点，静态资源加速）                 │
├─────────────────────────────────────────────────────────┤
│ 第三层：反向代理缓存（Nginx proxy_cache）                 │
├─────────────────────────────────────────────────────────┤
│ 第四层：进程内缓存（Caffeine / Ehcache / Guava Cache）    │
│         查询速度：纳秒级                                  │
│         容量：MB 级别（受 JVM 堆限制）                    │
│         特点：不走网络，最快但互相不共享                    │
├─────────────────────────────────────────────────────────┤
│ 第五层：分布式缓存（Redis / Memcached）                   │
│         查询速度：毫秒级（网络 IO）                        │
│         容量：GB~TB（可以横向扩展）                        │
│         特点：多实例共享，需要网络开销                     │
├─────────────────────────────────────────────────────────┤
│ 第六层：数据库缓存（Buffer Pool 等）                      │
├─────────────────────────────────────────────────────────┤
│ 数据源：数据库（MySQL / PostgreSQL / ...）                │
└─────────────────────────────────────────────────────────┘

读取流程：
  请求 → 本地缓存 → (miss) → Redis → (miss) → 数据库 → 回填缓存
```

---

## 2. 本地缓存详解

### 2.1 三大本地缓存对比

| 维度 | Caffeine | Guava Cache | Ehcache |
|------|---------|-------------|---------|
| **淘汰算法** | W-TinyLFU | LRU | LRU / LFU / FIFO |
| **命中率** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐ |
| **写入性能** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ |
| **读取性能** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ |
| **磁盘持久化** | ❌ | ❌ | ✅ |
| **分布式** | ❌ | ❌ | ✅（Terracotta） |
| **JCache 支持** | ✅ | ❌ | ✅ |
| **Spring 集成** | ✅ Spring Boot 2.x+ 默认 | ✅ (旧) | ✅ |
| **活跃度** | 高 | 低（作者去了 Caffeine） | 中 |

### 2.2 Caffeine 配置详解

```java
// Caffeine 完整配置
Cache<Long, User> cache = Caffeine.newBuilder()
    .initialCapacity(100)               // 初始容量
    .maximumSize(10_000)               // 最大条目数
    .maximumWeight(100_000)            // 最大权重（与 weigher 配合）
    .expireAfterWrite(10, TimeUnit.MINUTES)   // 写入后多久过期
    .expireAfterAccess(5, TimeUnit.MINUTES)   // 访问后多久过期
    .expireAfter(new Expiry<Long, User>() {   // 自定义过期
        public long expireAfterCreate(Long key, User user, long currentTime) {
            if (user.isVip()) return TimeUnit.HOURS.toNanos(1);
            return TimeUnit.MINUTES.toNanos(10);
        }
        public long expireAfterUpdate(...) { return Long.MAX_VALUE; }
        public long expireAfterRead(...) { return currentDuration; }
    })
    .refreshAfterWrite(8, TimeUnit.MINUTES)    // 异步刷新（不阻塞读）
    .weakKeys()                         // 弱引用 key（GC 回收时自动移除）
    .weakValues()                       // 弱引用 value
    .softValues()                       // 软引用 value（OOM 前回收）
    .recordStats()                      // 开启统计
    .removalListener((key, value, cause) -> {  // 移除监听
        log.info("缓存移除: key={}, cause={}", key, cause);
    })
    .build();

// 主要淘汰策略对比：
// W-TinyLFU（Caffeine 默认，最优）：
//   = Window TinyLFU
//   = 一个小的 Window 缓存（%1 容量）+ 主缓存 SLRU（Segmented LRU）
//   = Window 缓存降低突发流量误判
//   = TinyLFU 用 Count-Min Sketch 记录访问频率
//   = 综合了 LRU（近期性）+ LFU（频率性）的各自优势

// LRU（Least Recently Used）：
//   - 淘汰最久未使用的
//   - 问题：偶发的高频访问会驱逐长期热点
//   例：缓存已满，突然来一波新数据 → 踢掉的可能是长期热点

// LFU（Least Frequently Used）：
//   - 淘汰访问频率最低的
//   - 问题：旧的历史热点难以被替换
//   例：某个曾经很热的商品缓存了很久，即使不再热门也不会被淘汰
```

---

## 3. Redis 运行原理

### 3.1 Redis 为什么快？

```
Redis 高性能的 6 个核心原因：

1. 纯内存操作
   数据存在内存中，读写不用磁盘 IO
   内存访问是纳秒级，磁盘是毫秒级

2. 单线程模型（主线程）
   - 避免多线程的上下文切换
   - 避免锁竞争
   - Redis 6.0+ 引入了多线程 IO（网络读写），但命令执行仍是单线程
   注意：Redis 实际上有后台线程（AOF 刷盘、惰性删除等）

3. IO 多路复用（epoll/kqueue）
   单线程监听多个 Socket，哪个就绪处理哪个
   不是"一个连接一个线程"（BIO 模式）

4. 高效的数据结构
   专门为内存设计的紧凑数据结构（SDS、ziplist、quicklist、skiplist 等）

5. 简单协议（RESP）
   Redis Serialization Protocol，解析简单高效

6. 操作系统优化
   - 使用 jemalloc 替代 glibc malloc（减少内存碎片）
   - Overcommit memory 策略
   - THP（Transparent Huge Page）优化
```

### 3.2 IO 多路复用模型

```
BIO（传统阻塞 IO）：
  每个 Socket 一个线程来 read() → 阻塞等待数据到达
  10000 个连接 → 10000 个线程 → 内存 10GB+

IO 多路复用（Redis 使用的模型）：
  1 个线程 + epoll（Linux）：
  注册所有 Socket fd 到 epoll
  调用 epoll_wait() → 内核通知哪些 fd 可读/可写
  遍历就绪 fd 列表，逐个处理
  10000 个连接 → 1 个线程足够

Redis 6.0+ 多线程 IO：
  之前的瓶颈：不是命令执行，而是网络 IO 读写（大量数据的序列化/反序列化）
  解决方案：主线程负责命令执行，IO 线程负责 Socket 读写
  默认关闭，需要 io-threads 参数开启
  命令执行仍然是单线程（保证原子性）
```

### 3.3 Redis 命令执行过程

```
1. 客户端发送命令（如 SET key value）
   ↓
2. Server 端 Socket 可读 → epoll 通知 → readQueryFromClient()
   → 读取 querybuf（输入缓冲区）
   ↓
3. processInputBuffer() → 解析 RESP 协议 → 提取命令和参数
   ↓
4. processCommand()
   - 检查：是否认证、是否不在事务中、命令是否存在
   - 如果集群模式 → 检查是否在正确的节点
   - 如果 maxmemory 达到 → 执行淘汰
   ↓
5. call() → 执行命令处理函数（如 setCommand）
   ↓
6. addReply() → 将响应数据添加到 client 的输出缓冲区（buf/replylist）
   ↓
7. 下一个 epoll_wait 循环
   → 发现 Socket 可写 → sendReplyToClient() → 发送响应
```

---

## 4. Redis 数据结构与底层实现

### 4.1 底层数据结构全景

```
Redis 数据结构的演进（3.2+）：

┌────────────┬──────────────────────────────┬─────────────────────┐
│ 对外类型    │ 3.2 之前底层实现               │ 3.2+ 底层实现        │
├────────────┼──────────────────────────────┼─────────────────────┤
│ String     │ int / embstr / raw           │ 同左                 │
│ Hash       │ ziplist / hashtable          │ listpack / hashtable │
│ List       │ ziplist / linkedlist         │ quicklist            │
│ Set        │ intset / hashtable           │ listpack / hashtable │
│ ZSet       │ ziplist / skiplist+hashtable │ listpack / skiplist+ht│
│ Stream     │ - (5.0+)                     │ rax (基数树)          │
└────────────┴──────────────────────────────┴─────────────────────┘

ziplist → listpack 的演进：
  ziplist 问题：连锁更新（级联更新）
    - 每个 entry 存前一个 entry 的长度（prelen）
    - 如果某个 entry 从短变长，后续 entry 的 prelen 都要更新
    - 最坏情况：连锁更新所有 entry → 性能雪崩
  listpack 解决：
    - 存当前 entry 的长度（不是上一个）
    - 避免了连锁更新问题
```

### 4.2 核心数据结构源码级理解

```c
// SDS（Simple Dynamic String）— String 的底层实现
// 为什么不用 C 字符串？
// 1. O(1) 获取长度（len 字段，C 字符串需要 O(n) strlen）
// 2. 二进制安全（存 \0 不会截断，C 字符串以 \0 结束）
// 3. 缓冲区溢出保护（API 自动检查长度）
// 4. 空间预分配（减少内存重分配次数）
// 5. 惰性空间释放（释放时不一定立刻释放内存）

struct sdshdr {
    int len;    // 已使用长度
    int free;   // 剩余可用长度
    char buf[]; // 实际数据（柔性数组）
};

// 编码转换条件：
// int    → 存的是整数（如 SET key 123）
// embstr → ≤ 44 字节（Redis 3.2+），连续内存，一次分配
// raw    → > 44 字节，SDS 单独分配内存
// embstr vs raw：embstr 连续内存分配一次，raw 分配两次
//                 embstr 是只读的，修改会转 raw
```

### 4.3 五种数据类型场景

```
String（字符串）：
  场景：缓存 JSON、计数（INCR）、分布式锁、Session
  命令：SET GET INCR DECR SETEX SETNX MGET MSET
  注意：单个 value 最大 512MB

Hash（哈希）：
  场景：存储对象（如用户信息）、购物车
  命令：HSET HGET HGETALL HDEL HINCRBY
  优点：可单独更新某个字段（不用整个对象序列化/反序列化）

List（列表）：
  场景：消息队列（LPUSH + RPOP）、最新动态（LPUSH + LTRIM）
  命令：LPUSH RPUSH LPOP RPOP LRANGE LTRIM
  底层：quicklist（ziplist 组成的双向链表）

Set（集合）：
  场景：标签（SADD user:1:tags "java" "spring"）、共同好友（SINTER）
  命令：SADD SREM SMEMBERS SINTER SUNION SDIFF
  特点：无序、唯一、支持交集并集差集

ZSet（有序集合）：
  场景：排行榜（ZADD rank 100 user1 200 user2）
        延迟队列（ZADD delay 时间戳 orderId）
  命令：ZADD ZRANGE ZRANK ZSCORE ZREVRANKBYSCORE
  底层：skiplist（跳表）+ hashtable（用于 O(1) 查 score）
```

### 4.4 跳表（SkipList）原理

```
为什么 ZSet 用跳表而不用红黑树？
  1. 跳表实现简单（容易理解和维护）
  2. 跳表支持范围查询（ZRANGEBYSCORE），天然有序
  3. 跳表查询性能 O(log n)，与红黑树相当

跳表结构：
  Level 3:  1 ─────────────────────────── 9
  Level 2:  1 ───────── 5 ────────────── 9
  Level 1:  1 ── 3 ── 5 ── 7 ──────── 9
  Level 0:  1 ── 3 ── 5 ── 7 ── 8 ── 9
            (最底层是完整的有序链表)

插入流程：
  1. 从最高层开始向右搜索，找到每层的插入位置
  2. 随机决定新节点的高度（每层 50% 概率向上，层数越高概率越低）
  3. 在各层插入新节点，更新前后指针

查询流程：
  1. 从最高层开始查找
  2. 如果目标值 > 当前节点的下一个节点值 → 向右移动
  3. 如果目标值 < 当前节点的下一个节点值 → 向下移动
  4. 到底层后找到目标（或找不到）
```

### 4.5 高级数据结构

```
HyperLogLog：
  用途：基数统计（UV 统计）
  原理：概率算法，用 12KB 内存估算数十亿的唯一计数
  误差：0.81%
  命令：PFADD PFCOUNT PFMERGE

Bitmap（位图）：
  用途：签到打卡、用户在线状态、布隆过滤器
  原理：String 的位操作
  命令：SETBIT GETBIT BITCOUNT BITOP
  例：用户 ID=1000 今日签到 → SETBIT sign:20240612 1000 1

GEO（地理位置）：
  用途：附近的人、附近的商店
  原理：底层使用 ZSet（将经纬度编码为 GeoHash，作为 score）
  命令：GEOADD GEORADIUS GEODIST
  例：GEOADD cities 116.40 39.90 "北京" 121.47 31.23 "上海"

Stream（流，5.0+）：
  用途：持久化的消息队列
  特点：消费者组、消息确认、消息回溯
  命令：XADD XREAD XREADGROUP XACK
  底层：Rax（基数树）
```

---

## 5. Redis 持久化机制

### 5.1 RDB（快照持久化）

```
RDB 原理：
  将某个时间点的内存数据快照保存到磁盘（dump.rdb）

触发方式：
  - 手动：SAVE（阻塞主线程）/ BGSAVE（fork 子进程）
  - 配置自动：save 900 1 (900秒内1次修改)
              save 300 10 (300秒内10次修改)
              save 60 10000 (60秒内10000次修改)

fork 子进程机制（Copy-On-Write）：
  1. 主进程 fork() 出一个子进程
  2. fork 时子进程共享父进程的内存页（只读）
  3. 父进程继续处理写请求，修改某个内存页时触发 COW
     → OS 复制该页 → 父进程修改新页，子进程仍看旧页
  4. 子进程将内存数据写入 dump.rdb

优点：
  - 文件紧凑（二进制格式）
  - 恢复速度快（一次性加载）
  - 对性能影响小（子进程处理，父进程不阻塞）

缺点：
  - 可能丢失最后一次快照之后的数据（数据安全度取决于 save 配置）
  - fork 开销（大内存时 fork 耗时较长，可能阻塞父进程）
```

### 5.2 AOF（追加文件持久化）

```
AOF 原理：
  记录所有写命令，恢复时重放命令

appendfsync 三种策略：
  always    → 每个写命令立即 fsync → 数据最安全，性能最差
  everysec  → 每秒 fsync 一次 → 最多丢 1 秒数据（推荐）
  no        → 由操作系统决定何时 fsync → 性能最高，安全性最低

AOF 重写（Rewrite）：
  问题：AOF 文件随时间不断变大
    例：INCR key 执行 100 次 → AOF 中有 100 条 INCR key
    重写后 → 1 条 SET key 100
  
  重写流程：
  1. fork 子进程
  2. 子进程根据当前内存数据生成新的 AOF 文件
  3. 主进程将重写期间的增量命令写入 AOF 重写缓冲区
  4. 子进程完成 → 主进程将增量缓冲区追加到新文件
  5. 原子替换旧 AOF 文件

  触发条件（两个同时满足）：
    auto-aof-rewrite-percentage: 100  (文件增长到上次重写后的 200%)
    auto-aof-rewrite-min-size: 64mb   (文件超过 64MB)

优点：
  - 数据安全性高（可配置为每条都持久化）
  - 易读（文本格式，可手动编辑）

缺点：
  - 文件比 RDB 大
  - 恢复速度比 RDB 慢（需要重放命令）
```

### 5.3 混合持久化（4.0+，推荐）

```
混合持久化 = RDB 快照 + AOF 增量

AOF 文件结构：
  ┌──────────────────────────────────────────────┐
  │ [RDB 格式的快照数据] [AOF 格式的增量命令]        │
  │      前半段             后半段                 │
  └──────────────────────────────────────────────┘

  重写时：子进程将当前内存数据以 RDB 格式写入 AOF 文件
  后续增量：以 AOF 格式追加

优点：
  - 恢复快（RDB 部分一次性加载）
  - 数据安全（AOF 增量保证不丢数据）

配置：
  aof-use-rdb-preamble yes  (4.0+ 默认开启)
```

### 5.4 持久化选型

```
生产环境推荐方案：
  ┌──────────────────────────────────────────────┐
  │ 同时开启 RDB + AOF（混合持久化）                 │
  │ aof-use-rdb-preamble yes                      │
  │ appendfsync everysec                          │
  │ save 900 1  (RDB 做最后的保险)                 │
  └──────────────────────────────────────────────┘

不同场景建议：
  - 纯缓存：关闭持久化（提高性能）
  - 需要数据不可丢：always 刷盘
  - 一般业务：每秒刷盘 + RDB 定时备份
  - 机器内存大（>50G）：注意 fork 开销
```

---

## 6. Redis 高可用与集群

### 6.1 主从复制

```
主从复制架构：
  主节点 → 读写
  从节点 → 只读

全量复制流程（SYNC/PSYNC ? -1）：
  1. 从节点连接主节点 → 发送 PSYNC ? -1
  2. 主节点 fork 子进程 → bgsave 生成 RDB → 同时记录增量缓冲区
  3. 主节点发送 RDB 给从节点 → 从节点清空旧数据 → 加载 RDB
  4. 主节点发送增量缓冲区命令 → 从节点执行

部分复制（PSYNC <replication_id> <offset>）：
  1. 从节点重连 → 发送 PSYNC runid offset
  2. 主节点检查 offset 是否在复制积压缓冲区中
  3. 在 → 只发送 offset 之后的命令（部分复制）
  4. 不在 → 全量复制

主从延迟：
  原因：主节点写入速度 > 从节点消费速度
  监控：INFO replication → lag 字段
  解决：优化网络 / 减少从节点负载 / 避免大 key 操作
```

### 6.2 哨兵模式（Sentinel）

```
Sentinel 的作用：
  1. 监控（Monitoring）：定期 PING 所有节点，判断是否存活
  2. 通知（Notification）：通知客户端主节点变更
  3. 自动故障转移（Automatic Failover）：
     主节点宕机 → 选举一个从节点升级为主节点
  4. 配置提供者（Configuration Provider）：客户端连接 Sentinel 获取主节点地址

部署架构：
  ┌──────────────┐
  │  Sentinel-1  │
  │  Sentinel-2  │  ← 至少 3 个节点（避免脑裂）
  │  Sentinel-3  │
  └──────────────┘
       ↓ 监控
  ┌──────┐  ┌──────┐  ┌──────┐
  │ Master│ ←│Slave1│ ←│Slave2│
  └──────┘  └──────┘  └──────┘

故障转移流程：
  1. Sentinel 主观下线（SDOWN）：一个 Sentinel 认为主节点下线
  2. Sentinel 客观下线（ODOWN）：超过 quorum 个 Sentinel 认为主节点下线
  3. 选举一个 Sentinel 作为 Leader（Raft 算法）
  4. Leader 选举一个新主节点（选数据最新的从节点）
  5. 配置其他从节点跟随新主节点
  6. 通知客户端更新主节点地址

关于哨兵个数：
  quorum = 2, sentinel = 3  → 至少 2 个哨兵同意才能做故障转移
  奇数个哨兵：避免平票
  至少 3 个：因为只有 1 个哨兵无法判断自己不是网络隔离
```

### 6.3 Cluster 集群

```
Redis Cluster 特征：
  1. 去中心化（无代理，客户端直连节点）
  2. 数据分片：16384 个 Hash Slot
     slot = CRC16(key) % 16384
     每个节点负责一部分 slot
  3. 故障转移：每个主节点配一个从节点
  4. Gossip 协议通信（节点间交换状态）

分片示例：
  Node A: slot 0-5460
  Node B: slot 5461-10922
  Node C: slot 10923-16383

客户端路由：
  1. 客户端连任意一个节点
  2. 执行命令（如 SET user:1000 "张三"）
  3. 节点计算 slot = CRC16("user:1000") % 16384
  4. slot 在本节点 → 直接执行
  5. slot 不在本节点 → 返回 MOVED 错误 + 目标节点地址
  6. 客户端缓存 slot → 节点的映射（后续无需重定向）

Hash Tag 机制：
  如果 key 包含 {...}，只有 {} 内的部分参与 slot 计算
  例：user:{1000}:name 和 user:{1000}:age → 都在同一个 slot
  用途：确保相关的 key 在同一个节点，支持多 key 操作

Cluster 限制：
  - 不支持多 key 跨 slot 操作（除非用 hash tag）
  - 不支持事务跨 slot
  - 不支持 Lua 脚本跨 slot
  - 至少 3 主 3 从保证高可用
```

### 6.4 三种高可用方案对比

| 维度 | 主从复制 | 哨兵模式 | Cluster 集群 |
|------|---------|---------|-------------|
| 数据分片 | ❌ | ❌ | ✅ (16384 slots) |
| 自动故障转移 | ❌ | ✅ | ✅ |
| 水平扩展 | ❌ | ❌ | ✅ |
| 部署复杂度 | 低 | 中 | 高 |
| 客户端复杂度 | 低 | 中 | 高（需支持 Cluster 协议） |
| 适用数据量 | 小 | 中 | 大 |
| 多 key 操作 | ✅ | ✅ | ⚠️ (同 slot 才行) |
| 推荐场景 | 开发测试 | 中小型生产 | 大型生产 |

---

## 7. 缓存设计模式

### 7.1 Cache-Aside（旁路缓存 — 最常用）

```
Cache-Aside（应用主动管理缓存）：

读流程：
  1. 先查缓存 → 命中返回
  2. 未命中 → 查数据库
  3. 数据库有数据 → 写入缓存 → 返回
  4. 数据库没有数据 → 返回 null（不缓存 或 缓存空值）

写流程：
  先更新数据库 → 删除缓存
  （注意："先删缓存再更新数据库"可能造成不一致！）

代码模式：
  // 读
  public User getUser(Long id) {
      String key = "user:" + id;
      User user = (User) redisTemplate.opsForValue().get(key);
      if (user != null) return user;
      // 缓存未命中
      user = userMapper.findById(id);
      if (user != null) {
          redisTemplate.opsForValue().set(key, user, 30, TimeUnit.MINUTES);
      }
      return user;
  }

  // 写
  @Transactional
  public void updateUser(User user) {
      userMapper.update(user);                      // 先更新数据库
      redisTemplate.delete("user:" + user.getId());  // 删除缓存
  }
```

### 7.2 Read/Write Through（缓存代理）

```
Read Through：
  应用只跟缓存打交道
  缓存层自动去数据库加载数据
  → 应用不直接访问数据库

Write Through：
  应用只跟缓存打交道
  缓存层同步写数据库和缓存
  → 写入较慢（需要同时写两份）

实现：一般需要专门的缓存代理（如 RedisGear、自定义中间件）
```

### 7.3 Write Behind（异步写回）

```
Write Behind（Write Back）：
  1. 应用只写缓存
  2. 缓存异步批量写入数据库
  调用端感知延迟低
  风险：缓存宕机 → 数据丢失

Redis 可以结合 MQ 实现：
  应用写 Redis → 发 MQ 消息 → 消费者异步写 DB
```

---

## 8. 缓存三大问题与解决方案

### 8.1 缓存穿透 (Cache Penetration)

```
现象：
  查询的数据既不在缓存也不在数据库
  每次请求直接落到数据库 → 数据库压力大
  常见场景：
    - 查询不存在的用户 ID（如 id=-1）
    - 恶意攻击（大量查询不存在的 key）

解决方案：
  方案 1：缓存空值（最简单）
    if (user == null) {
        redisTemplate.opsForValue().set(key, "null", 1, TimeUnit.MINUTES);
    }
    注意：TTL 要短（如 1-5 分钟），防止占用大量缓存空间

  方案 2：布隆过滤器（Bloom Filter）
    在缓存前面加一道布隆过滤器
    查询先问布隆过滤器："这个 key 可能存在吗？"
    - 布隆过滤器说"不存在" → 一定不存在，直接返回（无需查 DB）
    - 布隆过滤器说"可能存在" → 查缓存 → 查 DB
    误判率可控（如 1%）

  方案 3：参数校验
    对请求参数做合法性校验
    id <= 0 → 直接拒绝
    字符串长度/格式校验

布隆过滤器原理：
  用 K 个 Hash 函数将元素映射到 m 比特的位数组中
  插入：K 个 Hash 函数分别计算出 K 个位置，设为 1
  查询：同样 K 个 Hash 函数计算位置，全部为 1 → 可能存在
        有一个为 0 → 一定不存在
  关键：牺牲精度换空间和时间
```

### 8.2 缓存击穿 (Cache Breakdown)

```
现象：
  一个热点 key 过期瞬间，大量并发请求同时打到数据库
  针对的是"一个 key"

解决方案：
  方案 1：互斥锁（Mutex Lock）
    public User getUser(Long id) {
        String key = "user:" + id;
        User user = redis.get(key);
        if (user != null) return user;
        // 尝试获取锁
        String lockKey = "lock:" + id;
        boolean locked = redis.setnx(lockKey, "1", 10, TimeUnit.SECONDS);
        if (locked) {
            try {
                user = userMapper.findById(id);   // 查 DB
                redis.set(key, user, 30, MINUTES); // 写缓存
            } finally {
                redis.del(lockKey);               // 释放锁
            }
        } else {
            Thread.sleep(100);  // 等一会再读缓存
            return getUser(id);  // 重试
        }
        return user;
    }

  方案 2：永不过期 + 异步刷新（逻辑过期）
    user 缓存不设 TTL
    在 value 中维护一个过期时间戳
    读取时发现"逻辑过期" → 返回旧数据 → 异步线程去更新

  方案 3：提前预热
    定时任务在高峰期前主动刷新热点数据
```

### 8.3 缓存雪崩 (Cache Avalanche)

```
现象：
  大量 key 同时过期，或 Redis 宕机
  请求全部落到数据库 → 数据库可能被打挂

解决方案：
  方案 1：TTL + 随机值
    int ttl = 1800 + ThreadLocalRandom.current().nextInt(300);
    // 30 分钟 + 随机 0~5 分钟
    避免大量 key 在同一秒过期

  方案 2：Redis 高可用
    哨兵 / Cluster（前面已详述）

  方案 3：多级缓存
    本地缓存（Caffeine）+ Redis + DB
    Redis 挂了本地缓存还能顶

  方案 4：限流降级
    Sentinel / Hystrix 对数据库访问进行限流
    超出限制 → 直接返回兜底数据或提示用户稍后重试

  方案 5：数据库保护
    数据库连接池限流
    慢 SQL 熔断
```

---

## 9. 分布式锁详解

### 9.1 Redis 分布式锁演进

```java
// V1：基本的 SETNX
// 问题：锁没有过期时间 → 死锁（客户端挂了没有释放锁）
SETNX lock:order:1 "1"

// V2：SETNX + EXPIRE
// 问题：SETNX 和 EXPIRE 不是原子操作 → 可能 SETNX 后 EXPIRE 前宕机 → 死锁
SETNX lock:order:1 "1"
EXPIRE lock:order:1 30

// V3：SET NX PX（原子命令，基本可用）
// SET key value NX PX 30000
// 问题 1：线程 A 释放了线程 B 的锁（A 的锁过期了，B 获取了新锁，A 误删了 B 的锁）
// 问题 2：锁过期但业务没执行完 → 并发问题

// V4：SET NX PX + value 唯一标识 + Lua 脚本释放
String value = UUID.randomUUID().toString();
// 加锁
Boolean locked = redisTemplate.opsForValue()
    .setIfAbsent("lock:order:1", value, 30, TimeUnit.SECONDS);
// 释放锁（Lua 脚本保证原子性）
String script = "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                "return redis.call('del', KEYS[1]) else return 0 end";
redisTemplate.execute(new DefaultRedisScript<>(script, Long.class),
    Collections.singletonList("lock:order:1"), value);
// 问题：锁过期时间不好设置（太短业务没执行完，太长影响并发）

// V5：Redisson（生产级，推荐）
// 通过看门狗（WatchDog）自动续期
RLock lock = redissonClient.getLock("lock:order:1");
try {
    // tryLock 有三个参数：等待时间、锁过期时间、时间单位
    // 如果不指定锁过期时间 → 默认 30s，WatchDog 每 10s 续期（30/3）
    // 如果指定锁过期时间 → WatchDog 不生效
    if (lock.tryLock(10, 30, TimeUnit.SECONDS)) {
        // 业务逻辑
    }
} finally {
    if (lock.isHeldByCurrentThread()) {
        lock.unlock();
    }
}
```

### 9.2 红锁 (RedLock)

```
场景：Redis 主节点挂了，从节点可能还没同步到锁 → 两个客户端同时持有锁

RedLock 算法（Redis 作者提出，有争议）：
  假设有 5 个独立的 Redis 实例（非主从，完全独立的 5 台）

  加锁流程：
  1. 客户端获取当前时间
  2. 依次向 5 个实例获取锁（设置相同的 key + value + 过期时间）
  3. 计算获取锁总耗时
  4. 成功获取 ≥ 3 个（N/2+1）锁，且总耗时 < 锁的过期时间 → 获取成功
  5. 如果获取失败 → 向所有实例释放锁

Redisson 的 RedLock：
  RedissonRedLock lock = new RedissonRedLock(lock1, lock2, lock3);
  lock.tryLock(10, 30, TimeUnit.SECONDS);

争议点：
  Martin Kleppmann（《数据密集型应用系统设计》作者）指出 RedLock 的问题：
  - 依赖系统时钟（即使短暂的时间跳跃都可能破坏安全保证）
  - RedLock 不是"完全正确"的分布式锁（它牺牲了安全性换取了可用性）

实际建议：
  如果确实需要强一致性的锁 → ZooKeeper（CP 系统）
  如果性能更重要且能容忍极少量不安全 → Redis（AP 系统）
  大多数业务场景：单实例 Redis + Redisson 已足够
```

### 9.3 ZooKeeper 分布式锁 vs Redis 分布式锁

```
ZooKeeper 分布式锁：
  原理：临时顺序节点 + Watcher
  1. 每个客户端在 /lock 下创建临时顺序节点
  2. 判断自己是不是序号最小的节点
  3. 是 → 获取锁
  4. 不是 → 对前一个节点注册 Watcher → 等待前一个节点释放后被通知
  优点：
    - 可靠性高（CP 系统，不会出现多个客户端同时拿到锁）
    - 自动释放（客户端断开 → 临时节点自动删除 → 锁自动释放）
  缺点：
    - 性能差于 Redis
    - 需要维护 ZK 集群

选型：
  可靠性优先 → ZooKeeper
  性能优先（且可以接受极小概率的不安全）→ Redis + Redisson
  简单易用 → Redis + Redisson（大多数企业的选择）
```

---

## 10. 缓存与数据库一致性

### 10.1 四种更新策略比较

```
1. 先更新数据库，再更新缓存
   问题：并发写时可能缓存是旧值
   线程 A 更新 DB=1 → 线程 B 更新 DB=2（最新）
   线程 B 更新 CACHE=2 → 线程 A 更新 CACHE=1（后发的覆盖了最新的）
   结果：DB=2, CACHE=1 → 不一致！

2. 先删除缓存，再更新数据库
   问题：并发读写时可能读到旧值
   线程 A 删除缓存 → 线程 B 读缓存(miss) → 线程 B 读 DB(旧值)
   线程 A 更新 DB → 线程 B 写缓存(旧值)
   结果：DB=新, CACHE=旧 → 不一致！

3. 先更新数据库，再删除缓存 ✅（推荐）
   线程 A 读缓存(miss) → 线程 A 读 DB(旧值)
   线程 B 更新 DB(新值) → 线程 B 删除缓存
   线程 A 写缓存(旧值)
   这种情况概率极低（缓存写完之前数据库必须被改了）
   结果：在极少情况下可能出现短暂不一致

4. 延迟双删
   先删除缓存 → 更新数据库 → 延迟 N ms → 再次删除缓存
   用来解决"先删除缓存再更新数据库"的并发问题
   延迟时间 > 读请求执行时间
```

### 10.2 最终一致性方案

```
为什么不能保证强一致性？
  CAP 理论：缓存（Redis）和数据库是两个独立的存储系统
  无法在同一个事务中操作

最终一致性方案：

方案 1：Canal + MQ
  MySQL binlog → Canal 监听 → 发 MQ → 消费端更新/删除 Redis
  优点：无侵入、实时性好
  适用：高一致性要求

方案 2：缓存 TTL
  缓存设置合理的过期时间
  即使有不一致，TTL 过期后自动修复
  最简单有效

方案 3：读写穿透
  读：只读缓存，miss 才读 DB
  写：同时写 DB + 删除缓存，事务保证

方案 4：分布式事务（过重，不推荐）
  Seata AT 同时操作 DB 和 Redis
```

---

## 11. Redis 性能优化

### 11.1 常见问题与优化

```
1. 大 Key 问题
   定义：String > 10KB / 集合 > 10000 个元素
   危害：
     - 内存不均（某个节点内存特别大）
     - 阻塞（DEL 大 key 会阻塞）
     - 网络拥塞（传输大 value 带宽打满）
     - 慢查询（HGETALL 所有元素）
   发现：
     redis-cli --bigkeys
     SCAN + TYPE + MEMORY USAGE
   解决：
     - 拆分（大 String 拆成多个小 String）
     - 用 Hash 替代 String（可部分读取）
     - 清理无用数据
     - 异步删除：UNLINK（4.0+，用后台线程异步释放内存）

2. 热 Key 问题
   定义：某 key 的 QPS 特别高（> 集群总 QPS 的 10%）
   危害：单节点成为瓶颈
   发现：
     redis-cli --hotkeys
     客户端统计
   解决：
     - 复制到多节点（不同 key 名，如 hotkey:1, hotkey:2）
     - 本地缓存 Caffeine 挡一道
     - 读写分离（读从节点）

3. 慢查询
   配置：
     slowlog-log-slower-than 10000  # 超过 10ms 记录
     slowlog-max-len 128
   常见慢操作：
     KEYS * （全量遍历，应改用 SCAN）
     HGETALL（一次性取所有字段，应改用 HSCAN）
     SMEMBERS（大集合全取，应改用 SSCAN）
     SORT（对大数据集排序）
     DEL 大 key（应改用 UNLINK）
```

### 11.2 内存优化

```
内存优化策略：

1. 合理的过期策略
   - 根据业务设置 TTL
   - 避免大量 key 同时过期

2. 数据结构选择
   - 能用 Hash 存对象就别用 String（省内存）
   - Hash 编码：ziplist/listpack 比 hashtable 省内存
   - 注意编码转换阈值：hash-max-ziplist-entries 512

3. 内存回收策略配置
   maxmemory-policy allkeys-lru  # 推荐

4. 共享对象池（0~10000 的整数）
   Redis 内部对 0~9999 的整数有全局共享对象
   在这个范围内的整数，不会分配新内存

5. 内存碎片整理
   4.0+ 支持自动内存碎片整理
   activedefrag yes
```

---

## 12. Spring Cache 抽象

### 12.1 基本使用

```java
@Configuration
@EnableCaching
public class CacheConfig {
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(30))
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()))
            .prefixCacheNameWith("cache:")
            .disableCachingNullValues();        // 不缓存 null

        return RedisCacheManager.builder(factory)
            .cacheDefaults(config)
            .withInitialCacheConfigurations(
                Map.of("user", config.entryTtl(Duration.ofMinutes(10)),
                       "dict", config.entryTtl(Duration.ofHours(1))))
            .transactionAware()                  // 事务感知
            .build();
    }
}

// 使用缓存注解
@Service
public class UserService {

    @Cacheable(value = "user", key = "#id", unless = "#result == null")
    public User getUser(Long id) {
        return userMapper.findById(id);
    }

    @CachePut(value = "user", key = "#user.id")
    public User updateUser(User user) {
        userMapper.update(user);
        return user;
    }

    @CacheEvict(value = "user", key = "#id")
    public void deleteUser(Long id) {
        userMapper.deleteById(id);
    }

    // 多个缓存操作
    @Caching(
        evict = {
            @CacheEvict(value = "user", key = "#user.id"),
            @CacheEvict(value = "userList", allEntries = true)
        }
    )
    public User updateUser(User user) { ... }
}
```

### 12.2 注解详解

```
@Cacheable：查询时使用
  - 先查缓存，有则返回，无则执行方法并缓存结果
  - value/cacheNames：缓存名
  - key：缓存键（SpEL 表达式）
  - condition：缓存条件（满足才缓存）
  - unless：不缓存条件（满足不缓存）
  - sync：同步模式（加锁，防止缓存击穿）

@CachePut：更新缓存
  - 总是执行方法，并用返回值更新缓存
  - 参数同 @Cacheable

@CacheEvict：删除缓存
  - allEntries: true（删除 value 下所有缓存）
  - beforeInvocation: true（方法执行前就删除）

SpEL 表达式示例：
  #id              → 方法的 id 参数
  #user.id         → user 参数的 id 属性
  #root.methodName → 方法名
  #root.args[0]    → 第一个参数
```

---

## 13. 常见面试题深度解析

### Q1: Redis 为什么单线程还这么快？

```
1. 纯内存操作（核心原因）
   所有数据在内存中，读写都是内存操作

2. IO 多路复用
   单线程监听多个连接，非阻塞 IO

3. 避免多线程开销
   - 无需上下文切换
   - 无需加锁/解锁
   - 无死锁问题

4. 高效数据结构
   精心设计的内存数据结构：SDS/ziplist/listpack/skiplist/quicklist

5. 简单协议（RESP）
   协议解析开销低

6. 主线程不处理耗时操作
   - 持久化：fork 子进程（bgsave）
   - 异步删除：后台线程（UNLINK）
   - 关闭 TCP：后台线程
   - AOF 刷盘：后台线程

Redis 6.0+ 多线程 IO：
  只是网络 IO 多线程（读写 Socket 数据）
  命令执行仍然是单线程（保证原子性）
```

### Q2: Redis 如何实现延迟队列？

```
方案 1：ZSet + 定时轮询
  // 添加延迟任务
  ZADD delay_queue <执行时间戳> <taskId>
  // 消费延迟任务
  ZRANGEBYSCORE delay_queue 0 <当前时间戳> LIMIT 0 10
  // 删除已消费
  ZREM delay_queue <taskId>

方案 2：Redis Stream + 消费者组
  更完善的方案（5.0+）

方案 3：Redisson DelayedQueue
  RDelayedQueue<Order> delayedQueue = redisson.getDelayedQueue(queue);
  delayedQueue.offer(order, 10, TimeUnit.SECONDS);
  // 10 秒后 order 会自动出现在目标 Queue 中
```

### Q3: Redis 集群模式下数据如何分布？为什么选 16384 个槽？

```
分布方式：slot = CRC16(key) % 16384

为什么是 16384？
  1. 心跳包大小考虑
     16384 个 slot 的位图仅需 2KB (16384/8/1024)
     65536 个 slot 需要 8KB
     集群节点间需要定期发送 Gossip 消息交换 slot 信息
     2KB 的心跳包更轻量

  2. 连接数考虑
     Redis Cluster 限制了最多 1000 个主节点
     16384 / 1000 ≈ 16 slot/节点 → 足够了
     用 65536 意义不大

  3. CRC16 算法
     16 位哈希 → 最大 65536
     取 16384 是 2^14，位操作高效
```

> **上一篇：** [05-持久层与数据库技术](./05-持久层与数据库技术.md)
>
> **下一篇：** [07-消息队列技术选型与实践](./07-消息队列技术选型与实践.md)
