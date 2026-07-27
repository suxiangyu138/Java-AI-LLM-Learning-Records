# Redis 核心数据结构详解
> Redis 不仅是缓存，更是数据结构服务器。5 种核心数据结构各有底层编码优化，理解它们才能在项目中选型正确、性能最优。

## 目录
1. [SDS 底层原理](#1-sds-底层原理)
2. [String 字符串](#2-string-字符串)
3. [Hash 哈希](#3-hash-哈希)
4. [List 列表](#4-list-列表)
5. [Set 集合](#5-set-集合)
6. [ZSet 有序集合](#6-zset-有序集合)
7. [命令速查总表](#7-命令速查总表)
8. [数据结构选型指南](#8-数据结构选型指南)

---

## 1. SDS 底层原理

### 1.1 为什么需要 SDS

Redis 使用 C 语言开发，但并未直接使用 C 字符串（以 `\0` 结尾的 char 数组），而是封装了 SDS（Simple Dynamic String，简单动态字符串）。这是因为 C 字符串存在以下缺陷：

| 问题 | C 字符串 | SDS |
|------|----------|-----|
| 获取长度 | 需遍历 O(n) | 直接读 len 字段 O(1) |
| 二进制安全 | 遇到 `\0` 截断 | 以 len 为准，完全兼容二进制 |
| 缓冲区溢出 | 拼接时可能溢出 | 自动检测和扩容 |
| 修改时内存重分配 | 每次修改都要重新分配 | 空间预分配 + 惰性空间释放 |

### 1.2 SDS 结构定义

```c
// Redis 7.0 之前的 sdshdr 结构（以 sdshdr8 为例）
struct sdshdr8 {
    uint8_t len;         // 已使用字节数，O(1) 长度获取
    uint8_t alloc;       // 总分配字节数，不含头部和 null 终止符
    unsigned char flags; // 头部类型，低 3 位表示类型，高 5 位未用
    char buf[];          // 柔性数组，存储字符串数据
};

// Redis 7.0+ 的 SDS 结构（移除 flags，直接编码在头部）
struct __attribute__((__packed__)) sdshdr8 {
    volatile uint8_t len;    // 已使用长度
    volatile uint8_t alloc;  // 总分配长度
    char buf[];              // 数据
};
```

SDS 根据字符串长度自动选择不同的头部类型，以节省内存：

| 类型 | 最大长度 | len / alloc 类型 | 头部总开销 |
|------|----------|------------------|-----------|
| sdshdr5 | 32 字节 | uint8_t（但用 flags 编码）| 1 字节 |
| sdshdr8 | 255 字节 | uint8_t | 3 字节 |
| sdshdr16 | 65535 字节 | uint16_t | 5 字节 |
| sdshdr32 | 4 GB | uint32_t | 9 字节 |
| sdshdr64 | 2^64 字节 | uint64_t | 17 字节 |

> 💡 sdshdr5 是特殊优化类型，直接将长度编码在 flags 字段中，不单独占用 len/alloc。Redis 7.0+ 简化了 SDS，移除了 sdshdr5 类型。

### 1.3 空间预分配策略

SDS 通过空间预分配（space preallocation）减少内存重分配次数：

```
修改后的 SDS 长度                 分配策略
    │
    ├── 小于 1MB ──→ 分配 2 * len + 1 字节
    │               （预留等量空闲空间）
    │
    └── 大于等于 1MB ──→ 分配 len + 1MB + 1 字节
                    （预留 1MB 空闲空间）
```

```c
// 伪代码：SDS 扩容逻辑
sds sdsMakeRoomFor(sds s, size_t addlen) {
    size_t len = sdslen(s);
    size_t newlen = len + addlen;

    if (newlen < 1024 * 1024) {
        // 小于 1MB，翻倍预分配
        newlen *= 2;
    } else {
        // 大于等于 1MB，多分配 1MB
        newlen += 1024 * 1024;
    }
    // 重新分配内存
    return sds_realloc(s, newlen + 1);
}
```

### 1.4 惰性空间释放

当 SDS 字符串缩短时，并不立即释放多余内存，而是用 free 字段记录空闲字节数，以备将来拼接时直接使用。如果需要真正释放内存，可以调用 `sdsRemoveFreeSpace()`。

> ⚠️ 惰性释放的优势是避免频繁内存操作，但若写入大量临时数据后不再使用，会导致内存浪费。好在 Redis 本身有 `maxmemory` 限制，可以通过淘汰策略回收。

---

## 2. String 字符串

最基础也最常用的类型，value 最大 512MB。
### 2.1 底层编码

| 编码类型 | 触发条件 | 存储结构 | 特点 |
|----------|----------|----------|------|
| int | 纯整数（可用 long 表示） | 直接存 long 值，64 位 | 内存极小，无额外开销 |
| embstr | 长度 <= 44 字节（Redis 7.0+ 为 48 字节） | SDS 和 robj 分配在连续内存 | 一次内存分配，CPU 缓存友好 |
| raw | 长度 > 44 字节 | SDS 和 robj 分开分配 | 两次内存分配，灵活处理大字符串 |

```
embstr 编码（连续内存）：
┌───────────────────────────────────┐
│           一次 malloc              │
│  ┌──────────┬──────────────────┐  │
│  │  robj    │    sdshdr8       │  │
│  │ type=4   │    len=10        │  │
│  │ encoding=│    alloc=10      │  │
│  │ ptr──────┤    buf="hello"   │  │
│  └──────────┴──────────────────┘  │
└───────────────────────────────────┘

raw 编码（非连续内存）：
┌──────────┐     ┌──────────────────┐
│  robj    │     │    sdshdr8       │
│ type=4   │────→│    len=100       │
│ encoding=│     │    alloc=128     │
│ ptr──────┤     │    buf="..."     │
└──────────┘     └──────────────────┘
    两次 malloc
```

> 💡 Redis 7.0+ 的 embstr 上限为 48 字节。

### 2.2 核心命令

| 命令 | 时间复杂度 | 说明 | 典型场景 |
|------|-----------|------|----------|
| SET key value [EX] [PX] [NX] [XX] | O(1) | 设置键值，支持过期和条件 | 通用写入 |
| GET key | O(1) | 获取值 | 通用读取 |
| STRLEN key | O(1) | 获取字符串长度 | 数据校验 |
| GETSET key value | O(1) | 设置新值返回旧值 | 轮替计数 |
| SETNX key value | O(1) | 不存在才设置 | 分布式锁 |
| SETEX key seconds value | O(1) | 设置值 + 过期时间 | Session / Token |
| MSET key val [key val ...] | O(n) | 批量设置 | 批量写入 |
| MGET key [key ...] | O(n) | 批量获取 | 批量查询 |
| INCR key | O(1) | 原子自增 1 | 计数器 |
| INCRBY key increment | O(1) | 原子自增 N | 点赞/浏览量 |
| DECR key | O(1) | 原子自减 1 | 库存扣减 |
| DECRBY key decrement | O(1) | 原子自减 N | 批量扣减 |
| INCRBYFLOAT key increment | O(1) | 浮点数自增 | 金额计算 |
| APPEND key value | O(n) | 字符串追加 | 日志拼接 |
| GETRANGE key start end | O(n) | 获取子串 | 按需读取 |

### 2.3 INCR 原子性原理

```text
多线程同时 INCR "count":
                    Redis 单线程
时间 ──→
          线程 A: INCR count ──→ ┌──┐
          线程 B: INCR count ──→ │  │──→ 读取 count (当前 5)
          线程 C: INCR count ──→ │单│──→ count + 1 = 6
                                 │线│──→ 写入 count = 6
                                 │程│──→ 读取 count (当前 6)
                                 │ │──→ count + 1 = 7
                                 │  │──→ 写入 count = 7
                                 └──┘
不会出现并发安全问题（对比 Java 多线程 +1 需要 synchronized）
```

> 💡 INCR 是原子操作，因为 Redis 命令执行是单线程串行的，天然无竞争。这也是 Redis 分布式锁的基础。

### 2.4 Java 实战

```java
// Spring Data Redis 模板注入
@Autowired
private StringRedisTemplate redisTemplate;

// ===== 缓存穿透防护 =====
// 缓存空值
String userJson = redisTemplate.opsForValue().get("user:1001");
if (userJson == null) {
    User user = userDao.findById(1001);
    if (user == null) {
        // 缓存空值，防止穿透
        redisTemplate.opsForValue().set("user:1001", "", 60, TimeUnit.SECONDS);
    } else {
        redisTemplate.opsForValue().set("user:1001", JSON.toJSONString(user), 3600, TimeUnit.SECONDS);
    }
}

// ===== 计数器 =====
// 文章阅读量
redisTemplate.opsForValue().increment("article:read:1001");
// 阅读量批量写入 DB（定期执行）
String readCount = redisTemplate.opsForValue().get("article:read:1001");
if (readCount != null && Integer.parseInt(readCount) >= 100) {
    articleDao.updateReadCount(1001, Integer.parseInt(readCount));
    redisTemplate.opsForValue().set("article:read:1001", "0");
}

// ===== 分布式锁 =====
String lockKey = "lock:order:1001";
String requestId = UUID.randomUUID().toString();
Boolean locked = redisTemplate.opsForValue()
    .setIfAbsent(lockKey, requestId, 30, TimeUnit.SECONDS);
if (Boolean.TRUE.equals(locked)) {
    try {
        // 执行业务逻辑
        processOrder(1001);
    } finally {
        // Lua 脚本释放锁（保证原子性）
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        redisTemplate.execute(new DefaultRedisScript<>(script, Long.class),
            Collections.singletonList(lockKey), requestId);
    }
}
```

---

## 3. Hash 哈希

类似 Java 的 `Map<String, String>`，适合存储结构化对象，如用户信息、商品详情等。

### 3.1 底层编码

| 编码类型 | 触发条件 | 结构 | 特点 |
|----------|----------|------|------|
| ziplist | 元素 < 512 且所有 value < 64B | 紧凑连续内存 | 高内存利用率 |
| listpack（7.0+） | 元素 < 512 且所有 value < 64B | 更优的紧凑结构 | 消除连锁更新 |
| hashtable | 超出阈值 | 数组 + 链表/红黑树 | O(1) 读写，内存消耗大 |

> ⚠️ ziplist 的连锁更新问题：ziplist 每个 entry 包含前一个 entry 的长度，当插入/删除导致后续 entry 长度变化时，需要级联更新所有后续 entry 的 prevlen 字段，最坏情况 O(n²)。Redis 7.0 用 listpack 彻底解决了这个问题——listpack 每个 entry 只记录自己的长度，不依赖前一个 entry。

```
ziplist 内存布局：
┌─────┬──────┬────────┬────────┬────────┬─────┐
│zlbytes│zltail│zllen│entry1│entry2│...│zlend│
└─────┴──────┴────────┴────────┴────────┴─────┘
每个 entry:
┌────────────────┬───────────────┬────────┐
│prev_entry_len  │  encoding     │ content │
│（连锁更新源头） │               │        │
└────────────────┴───────────────┴────────┘

listpack 内存布局（7.0+）：
┌─────┬──────┬───────┬────────┬────────┬────┐
│tot-bytes│num-ele│entry1│entry2│...│end│
└─────┴──────┴───────┴────────┴────────┴────┘
每个 entry:
┌───────┬────────────┬───────────────────┐
│encoding-type│element-data│element-tot-len│
│               │（后向遍历用）              │
└───────┴────────────┴───────────────────┘
（无 prevlen，无连锁更新）
```

### 3.2 核心命令

| 命令 | 时间复杂度 | 说明 | 返回值 |
|------|-----------|------|--------|
| HSET key field value | O(1) | 设置字段值 | 新增返回1，覆盖返回0 |
| HGET key field | O(1) | 获取字段值 | 值或 nil |
| HGETALL key | O(n) | 获取所有字段和值 | 字段值交替列表 |
| HDEL key field [field ...] | O(1) 每个 | 删除字段 | 成功删除数量 |
| HEXISTS key field | O(1) | 判断字段是否存在 | 0/1 |
| HKEYS key | O(n) | 所有字段名 | 字段名列表 |
| HVALS key | O(n) | 所有字段值 | 值列表 |
| HLEN key | O(1) | 字段数量 | 整数 |
| HSETNX key field value | O(1) | 字段不存在才设置 | 0/1 |
| HINCRBY key field increment | O(1) | 字段原子递增 | 递增后值 |
| HINCRBYFLOAT key field increment | O(1) | 浮点原子递增 | 递增后值 |
| HSTRLEN key field | O(1) | 字段值长度 | 整数 |
| HSCAN key cursor [MATCH] [COUNT] | O(1) 每次 | 游标遍历 | 游标+数据 |

### 3.3 Hash 与 String 存对象的对比

```bash
# String 方式：JSON 序列化整个对象
SET user:1001 '{"name":"张三","age":25,"phone":"13800138000"}'
# 缺点：修改单个字段需要整体读写

# Hash 方式：每个字段独立存储
HSET user:1001 name "张三" age 25 phone "13800138000"
# 优点：修改单个字段开销小，内存更省
HGET user:1001 name    # 获取单字段
```

| 对比维度 | String + JSON | Hash |
|----------|---------------|------|
| 内存效率 | 较低（重复键名） | 较高（字段名独立存） |
| 修改单字段 | 整体序列化读写 | 单字段操作 O(1) |
| 批量读取 | GET 即可 | HGETALL O(n) |
| 部分更新 | 不友好 | 天然支持 |
| 编码效率 | 需序列化/反序列化 | 直接操作字段 |
| 适用场景 | 小对象、整体读写 | 大对象、频繁修改部分字段 |

### 3.4 Java 实战

```java
// ===== 用户信息缓存 =====
// 存储
Map<String, Object> userMap = new HashMap<>();
userMap.put("name", "张三");
userMap.put("age", "25");
userMap.put("phone", "13800138000");
userMap.put("score", "100");
redisTemplate.opsForHash().putAll("user:1001", userMap);

// 获取单个字段
String name = (String) redisTemplate.opsForHash().get("user:1001", "name");

// 获取所有字段
Map<Object, Object> user = redisTemplate.opsForHash().entries("user:1001");

// 原子递增评分
redisTemplate.opsForHash().increment("user:1001", "score", 10);

// 字段是否存在
Boolean hasPhone = redisTemplate.opsForHash().hasKey("user:1001", "phone");

// ===== 购物车案例 =====
// 添加商品到购物车
redisTemplate.opsForHash().put("cart:user:1001", "sku:1001", "2");   // 2 件
redisTemplate.opsForHash().increment("cart:user:1001", "sku:1001", 1); // 加 1 件

// 获取购物车商品数量
Long cartSize = redisTemplate.opsForHash().size("cart:user:1001");

// 清空购物车
redisTemplate.opsForHash().delete("cart:user:1001", "sku:1001", "sku:1002");
```

---

## 4. List 列表

有序可重复集合，基于 quicklist（双向链表 + 压缩列表/listpack）实现。

### 4.1 底层结构

Redis 3.2 之后采用 quicklist 替代了原始的 ziplist + linkedlist 双编码方案。

```text
quicklist 结构：
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│  quicklist    │    │  quicklist    │    │  quicklist    │
│  Node 1       │ →  │  Node 2       │ →  │  Node 3       │
│              │    │              │    │              │
│ ┌──────────┐ │    │ ┌──────────┐ │    │ ┌──────────┐ │
│ │ listpack │ │    │ │ listpack │ │    │ │ listpack │ │
│ │ [A,B,C]  │ │    │ │ [D,E,F]  │ │    │ │ [G,H]    │ │
│ └──────────┘ │    │ └──────────┘ │    │ └──────────┘ │
└──────────────┘    └──────────────┘    └──────────────┘
```

每个 quicklist 节点是一个 listpack（Redis 7.0+）或 ziplist（旧版本），通过 `list-max-listpack-size` 配置每个节点的最大大小。

> 💡 quicklist 兼顾了双向链表的插入删除效率和压缩列表的内存紧凑性。头尾操作 O(1)，中间操作 O(n)。`list-compress-depth` 可以配置两端不压缩的节点数，进一步节省内存。

### 4.2 核心命令

| 命令 | 时间复杂度 | 说明 | 典型场景 |
|------|-----------|------|----------|
| LPUSH key val [val ...] | O(1) | 左端插入 | 最新消息 |
| RPUSH key val [val ...] | O(1) | 右端插入 | 队列入队 |
| LPOP key | O(1) | 左端弹出 | 队列出队 |
| RPOP key | O(1) | 右端弹出 | 栈顶弹出 |
| BLPOP key timeout | O(1) | 阻塞左端弹出 | 阻塞队列 |
| BRPOP key timeout | O(1) | 阻塞右端弹出 | 阻塞队列 |
| LRANGE key start stop | O(n) | 范围获取 | 列表查看 |
| LINDEX key index | O(n) | 索引获取 | 指定位置 |
| LLEN key | O(1) | 列表长度 | — |
| LSET key index val | O(n) | 设置指定索引 | 修改元素 |
| LINSERT key BEFORE/AFTER pivot val | O(n) | 指定元素前后插入 | — |
| LREM key count val | O(n) | 删除指定值 | 清理元素 |
| LTRIM key start stop | O(n) | 裁剪范围 | 限流保留 |
| RPOPLPUSH src dest | O(1) | 右弹出左推入 | 安全队列 |
| BRPOPLPUSH src dest timeout | O(1) | 阻塞版 | 可靠传输 |

### 4.3 消息队列实现

```bash
# 生产者
RPUSH queue:task "task:1001" "task:1002"

# 消费者（非阻塞）
LPOP queue:task

# 消费者（阻塞等待，推荐）
BRPOP queue:task 0    # 0 表示一直阻塞

# 多个消费者竞争消费时，消息在消费者间分摊
# 但 List 的 ACK 机制需要手动实现
```

### 4.4 Java 实战

```java
// ===== 消息队列 =====
// 生产者
redisTemplate.opsForList().rightPush("queue:order", orderId);

// 消费者（阻塞，超时 3 秒）
String orderId = (String) redisTemplate.opsForList()
    .leftPop("queue:order", 3, TimeUnit.SECONDS);

// ===== 操作日志（保留最新 100 条）=====
String logKey = "log:user:1001";
redisTemplate.opsForList().leftPush(logKey, "登录系统");
redisTemplate.opsForList().leftPush(logKey, "查看订单");
// LTRIM 只保留前 100 条
redisTemplate.opsForList().trim(logKey, 0, 99);

// ===== 消息队列 + 担保机制 =====
// 处理队列（正常处理）
String task = (String) redisTemplate.opsForList()
    .rightPopAndLeftPush("queue:processing", "queue:backup", 1, TimeUnit.SECONDS);
// 如果 task 不为 null，正常处理完后从 backup 删除
// 如果客户端崩溃，task 在 backup 队列中，启动时检查 backup 重新处理
redisTemplate.opsForList().remove("queue:backup", 1, task);
```

---

## 5. Set 集合

无序不重复，自动去重，支持集合运算。

### 5.1 底层编码

| 编码类型 | 触发条件 | 结构 | 特点 |
|----------|----------|------|------|
| intset | 全部整数且元素 < 512 | 有序整数数组（二分查找） | 紧凑，O(log n) 查找 |
| hashtable | 不满足 intset 条件 | 哈希表（value 指向 NULL） | O(1) 操作，利用 key 唯一性 |

```
intset 编码：
┌──────┬──────┬──────┬──────┬──────┬──────┐
│encoding│length│ 2  │ 5  │ 8  │ 15  │ 23  │
└──────┴──────┴──────┴──────┴──────┴──────┘
有序整数数组，二分查找

hashtable 编码（Set）：
┌─────────────────┐
│ 哈希表           │
│ ┌─────┬───────┐ │
│ │ key │ NULL  │ │  ← value 指向 NULL，只利用 key 去重
│ ├─────┼───────┤ │
│ │ key │ NULL  │ │
│ └─────┴───────┘ │
└─────────────────┘
```

### 5.2 核心命令

| 命令 | 时间复杂度 | 说明 | 典型场景 |
|------|-----------|------|----------|
| SADD key mem [mem ...] | O(1) 每个 | 添加成员 | 添加标签 |
| SREM key mem [mem ...] | O(1) 每个 | 删除成员 | 移除标签 |
| SMEMBERS key | O(n) | 所有成员 | 查看集合（大 Set 慎用）|
| SISMEMBER key mem | O(1) | 判断是否存在 | 快速判重 |
| SCARD key | O(1) | 成员数量 | 集合大小 |
| SPOP key [count] | O(1) | 随机弹出 | 抽奖/随机 |
| SRANDMEMBER key [count] | O(1) | 随机获取不删除 | 随机推荐 |
| SMOVE src dest mem | O(1) | 移动元素 | 状态迁移 |
| SINTER key [key ...] | O(n) | 交集 | 共同好友 |
| SUNION key [key ...] | O(n) | 并集 | 合并集合 |
| SDIFF key [key ...] | O(n) | 差集 | 推荐未关注 |
| SINTERSTORE / SUNIONSTORE / SDIFFSTORE | O(n) | 集合运算存结果 | 缓存结果 |
| SSCAN key cursor [MATCH] [COUNT] | O(1) 每次 | 游标遍历 | 大集合遍历 |

### 5.3 Java 实战

```java
// ===== 用户标签系统 =====
// 为用户打标签
redisTemplate.opsForSet().add("user:tag:1001", "java", "redis", "spring", "mysql");
redisTemplate.opsForSet().add("user:tag:1002", "java", "python", "docker");

// 共同标签（交集）
Set<Object> common = redisTemplate.opsForSet()
    .intersect("user:tag:1001", "user:tag:1002");
// → [java]

// 推荐标签（差集：1001 有但 1002 没有）
Set<Object> recommend = redisTemplate.opsForSet()
    .difference("user:tag:1001", "user:tag:1002");
// → [redis, spring, mysql]

// 所有标签合并（并集）
Set<Object> union = redisTemplate.opsForSet()
    .union("user:tag:1001", "user:tag:1002");
// → [java, redis, spring, mysql, python, docker]

// ===== 在线用户去重 =====
// 用户上线
redisTemplate.opsForSet().add("online:20260101", "user:1001", "user:1002");
// 在线用户数
Long onlineCount = redisTemplate.opsForSet().size("online:20260101");
// 用户是否在线
Boolean isOnline = redisTemplate.opsForSet()
    .isMember("online:20260101", "user:1001");

// ===== 抽奖 =====
// 用户参与抽奖
redisTemplate.opsForSet().add("lottery:20260101", "user:1001", "user:1002", "user:1003");
// 随机抽取一等奖（弹出，不重复）
String firstPrize = (String) redisTemplate.opsForSet().pop("lottery:20260101");
// 随机抽取二等奖（不删除）
String secondPrize = (String) redisTemplate.opsForSet()
    .randomMember("lottery:20260101");
```

---

## 6. ZSet 有序集合

每个元素关联一个 double 类型的 score，按 score 排序。Redis 中最强大的数据结构。

### 6.1 底层编码

| 编码类型 | 触发条件 | 结构 | 特点 |
|----------|----------|------|------|
| ziplist | 元素 < 128 且 value < 64B | 按 score 有序存储 | 紧凑，线性扫描 |
| listpack（7.0+） | 元素 < 128 且 value < 64B | 紧凑有序 | 消除连锁更新 |
| skiplist + dict | 超出阈值 | 跳表 O(log n) + hash O(1) | 兼顾有序和单点 |

### 6.2 Skiplist 跳表原理

跳表（skiplist）通过维护多层索引实现快速查找，平均 O(log n) 时间复杂度。

```
Level 4:  HEAD ─────────────────────────────────→ 90 ──→ NULL
           │                                       │
Level 3:  HEAD ───────────→ 30 ──────────────────→ 90 ──→ NULL
           │                │                      │
Level 2:  HEAD ───→ 10 ──→ 30 ──────────→ 70 ───→ 90 ──→ NULL
           │          │      │              │       │
Level 1:  HEAD ─→ 5 ─→ 10 ─→ 20 ─→ 30 ─→ 50 ─→ 70 ─→ 90 ──→ NULL
                    ↓      ↓      ↓      ↓      ↓       ↓
                    [5]   [10]   [20]   [30]   [50]   [70]  (实际数据)

查找 50 的过程：Level 4 → Level 3 → Level 2 → Level 1（找到）
每层跨越一半节点，类似二分查找
```

**为什么用跳表不用红黑树？**

| 对比维度 | 跳表 | 红黑树 |
|----------|------|--------|
| 实现复杂度 | 简单（几十行代码） | 复杂（旋转、变色） |
| 范围查询 | 极快，沿着指针遍历即可 | 需要中序遍历，复杂 |
| 内存占用 | 每个节点平均 1/(1-p) 层指针 | 每个节点 2-3 个指针 + 颜色位 |
| 平衡维护 | 概率平衡，无需调整 | 严格平衡，插入删除需旋转 |
| 调试难度 | 易于调试和可视化 | 调试困难 |

> 💡 跳表通过概率算法决定节点层数，避免了红黑树复杂的旋转操作。Redis 中跳表的层数上限为 32 层，概率 p = 0.25。

### 6.3 核心命令

| 命令 | 时间复杂度 | 说明 | 典型场景 |
|------|-----------|------|----------|
| ZADD key [NX/XX] [CH] [INCR] score member | O(log n) | 添加/更新成员 | 排行榜写入 |
| ZREM key member [member ...] | O(log n) | 删除成员 | 移除排名 |
| ZRANGE key start stop [WITHSCORES] | O(log n + k) | 按排名范围获取（正序） | 排行榜展示 |
| ZREVRANGE key start stop [WITHSCORES] | O(log n + k) | 按排名范围获取（逆序） | Top N |
| ZRANGEBYSCORE key min max [WITHSCORES] | O(log n + k) | 按分数范围获取 | 延时任务 |
| ZRANK key member | O(log n) | 获取正序排名 | 排名查询 |
| ZREVRANK key member | O(log n) | 获取倒序排名 | 排名查询 |
| ZSCORE key member | O(1) | 获取分数 | 查用户分数 |
| ZINCRBY key increment member | O(log n) | 原子增减分数 | 加分/扣分 |
| ZCARD key | O(1) | 成员数量 | 排行榜总数 |
| ZCOUNT key min max | O(log n) | 分数区间计数 | 统计人数 |
| ZREMRANGEBYRANK key start stop | O(log n + k) | 按排名范围删除 | 删除低排名 |
| ZREMRANGEBYSCORE key min max | O(log n + k) | 按分数范围删除 | 清理过期 |
| ZUNIONSTORE dest numkeys key [key ...] | O(n) | 并集计算 | 多榜合并 |
| ZINTERSTORE dest numkeys key [key ...] | O(n) | 交集计算 | 共同排名 |
| ZRANGEBYLEX key min max | O(log n + k) | 字典序范围 | 自动补全 |
| ZLEXCOUNT key min max | O(log n) | 字典序计数 | — |
| ZPOPMAX / ZPOPMIN | O(log n) | 弹出最大/最小 | — |
| BZPOPMAX / BZPOPMIN | O(log n) | 阻塞弹出 | 延时队列 |

### 6.4 实战场景

**实时排行榜**

```java
// ===== 游戏积分榜 =====
String rankKey = "rank:game:001";

// 玩家得分
redisTemplate.opsForZSet().add(rankKey, "player:1001", 9500);
redisTemplate.opsForZSet().add(rankKey, "player:1002", 8800);
redisTemplate.opsForZSet().add(rankKey, "player:1003", 12000);

// 玩家加分
redisTemplate.opsForZSet().incrementScore(rankKey, "player:1001", 500);

// 获取 Top 10（逆序，高分在前）
Set<ZSetOperations.TypedTuple<Object>> top10 = 
    redisTemplate.opsForZSet().reverseRangeWithScores(rankKey, 0, 9);
int rank = 1;
for (ZSetOperations.TypedTuple<Object> tuple : top10) {
    System.out.println("第" + rank + "名: " + tuple.getValue() 
        + " 分数: " + tuple.getScore());
    rank++;
}

// 查询玩家排名（从 0 开始）
Long playerRank = redisTemplate.opsForZSet()
    .reverseRank(rankKey, "player:1001");

// 查询玩家分数
Double playerScore = redisTemplate.opsForZSet()
    .score(rankKey, "player:1001");
```

**延时队列**

```java
// ===== 延时任务队列 =====
// 添加延时任务（score = 执行时间戳）
redisTemplate.opsForZSet().add("delay:queue", "task:1001", 
    System.currentTimeMillis() + 60000); // 60 秒后执行

// 取出到期任务（定时执行）
Set<String> dueTasks = redisTemplate.opsForZSet()
    .rangeByScore("delay:queue", 0, System.currentTimeMillis());
for (String task : dueTasks) {
    // 处理任务
    processTask(task);
    // 移除
    redisTemplate.opsForZSet().remove("delay:queue", task);
}

// ===== 商品热度排序 =====
// 用户点击商品时增加热度
redisTemplate.opsForZSet().incrementScore("hot:products", "sku:1001", 1);
// 获取热榜 Top 20
Set<ZSetOperations.TypedTuple<Object>> hotProducts = 
    redisTemplate.opsForZSet().reverseRangeWithScores("hot:products", 0, 19);
```

---

## 7. 命令速查总表

| 类型 | 写操作 | 读操作 | 删除 | 其他 |
|------|--------|--------|------|------|
| String | SET / MSET / SETNX / SETEX / PSETEX | GET / MGET / STRLEN / GETRANGE | DEL / UNLINK | INCR / DECR / INCRBY / DECRBY / APPEND / GETSET |
| Hash | HSET / HMSET / HSETNX | HGET / HGETALL / HKEYS / HVALS / HLEN / HEXISTS / HSTRLEN | HDEL | HINCRBY / HINCRBYFLOAT / HSCAN |
| List | LPUSH / RPUSH / LINSERT / LSET | LRANGE / LINDEX / LLEN | LREM / LTRIM / LPOP / RPOP | BLPOP / BRPOP / RPOPLPUSH / BRPOPLPUSH |
| Set | SADD / SMOVE | SMEMBERS / SCARD / SISMEMBER / SRANDMEMBER | SREM / SPOP | SINTER / SUNION / SDIFF / SINTERSTORE / SSCAN |
| ZSet | ZADD / ZINCRBY | ZRANGE / ZREVRANGE / ZRANGEBYSCORE / ZSCORE / ZCARD / ZRANK / ZREVRANK / ZCOUNT | ZREM / ZREMRANGEBYRANK / ZREMRANGEBYSCORE / ZPOPMAX / ZPOPMIN | ZUNIONSTORE / ZINTERSTORE / ZRANGEBYLEX / ZLEXCOUNT / BZPOPMAX |

---

## 8. 数据结构选型指南

### 8.1 场景匹配

| 场景 | 推荐结构 | 原因 |
|------|----------|------|
| 缓存单值 | String | 最简单高效 |
| 缓存对象 | Hash | 省内存，可单独操作字段 |
| 消息队列 | List / Stream | List 简单快速 |
| 去重统计 | Set | 自动去重 + 集合运算 |
| 排行榜 | ZSet | 天然按 score 排序 |
| 延时任务 | ZSet | score 做时间戳 |
| 计数器 | String (INCR) | 原子操作 O(1) |
| 标签系统 | Set | SINTER 交集运算 |
| 分布式锁 | String (SETNX) | 原子 + 过期 |

### 8.2 内存优化建议

| 优化项 | 说明 |
|--------|------|
| 纯数字优先 | 整数自动 int 编码，8 字节存储 |
| 小集合用压缩编码 | 利用 ziplist/listpack/intset 阈值 |
| 缩短字段名 | `user_name` 改为 `un` |
| 合理设置过期 | TTL 自动清理，避免堆积 |
| 监控大 Key | String > 10MB 或集合 > 1 万元素 |

### 8.3 大 Key 处理

```bash
# 发现大 Key
redis-cli --bigkeys

# 渐进删除（避免 DEL 阻塞）
redis-cli UNLINK bigkey                 # String 异步删除
redis-cli HSCAN bigkey 0 COUNT 100      # Hash 分批删除
redis-cli HDEL bigkey field1 field2 ...
redis-cli ZREMRANGEBYRANK bigkey 0 99   # ZSet 分批删除
```

> 🎯 核心选型原则：根据访问模式、数据规模、操作类型综合选择。没有万能结构，只有最合适的。

