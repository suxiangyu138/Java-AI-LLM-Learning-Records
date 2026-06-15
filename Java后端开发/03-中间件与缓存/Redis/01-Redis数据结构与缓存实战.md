# Redis 数据结构与缓存实战

## 前言

Redis（Remote Dictionary Server）是一个开源的、基于内存的键值存储系统，以其极高的吞吐量（单机 QPS 可达 10 万+）和丰富的数据结构著称。它不仅仅是一个缓存中间件，更是一个数据结构服务器。本文将深入剖析 Redis 五种核心数据结构及高级数据结构的底层实现原理，详解持久化机制与高可用架构，并通过大量实战代码案例剖析缓存穿透、击穿、雪崩、一致性等难题，最后给出 Spring Boot 集成方案与高频面试题。

---

## 一、五种核心数据结构

### 1.1 String（字符串）

String 是 Redis 最基础的数据结构，value 最大可存储 **512MB**。使用场景极为广泛。

#### 底层实现：SDS（Simple Dynamic String）

Redis 没有直接使用 C 语言的 `char*` 字符串，而是自己构建了 SDS 抽象类型。在 Redis 3.2+ 中，SDS 有 5 种类型（sdshdr5/sdshdr8/sdshdr16/sdshdr32/sdshdr64），其核心结构如下（以 sdshdr8 为例）：

```c
struct __attribute__((__packed__)) sdshdr8 {
    uint8_t len;      // 已使用的字节数
    uint8_t alloc;    // 分配的总字节数（不含头部和空终止符）
    unsigned char flags; // 低3位表示类型，高5位保留
    char buf[];       // 实际存储数据的字节数组
};
```

**SDS 相比 C 字符串的优势：**

| 特性 | C 字符串 | SDS |
|------|----------|-----|
| 获取长度 | O(n) 遍历 | O(1) 直接读 len 字段 |
| 二进制安全 | 不兼容 `\0` | 完全兼容，以 len 判断结束 |
| 缓冲区溢出 | 可能溢出 | 自动扩容，alloc - len 即剩余空间 |
| 修改时内存重分配 | 每次都要 | 空间预分配（小于1MB翻倍，大于1MB加1MB）+ 惰性释放 |
| 函数调用 | 不安全 | 安全 API |

**空间预分配策略**：当对 SDS 修改后长度 `< 1MB`，分配 `2 * len + 1` 字节；当长度 `>= 1MB`，分配 `len + 1MB + 1` 字节。这大大减少了内存重分配次数。

#### 常用命令

```bash
# 基本操作
SET key "hello"
GET key
GETSET key "newvalue"    # 设置新值并返回旧值
MSET k1 v1 k2 v2         # 批量设置
MGET k1 k2               # 批量获取

# 数值操作（底层通过将字符串转为长整型/双精度浮点型计算）
INCR counter             # 原子自增 1
INCRBY counter 10        # 原子自增 10
DECR counter
DECRBY counter 5
INCRBYFLOAT price 3.5    # 浮点数自增

# 分布式锁（经典用法）
SET lock:resource1 value NX EX 30
# NX: 仅在 key 不存在时设置（SETNX 的升级版）
# EX: 过期时间 30 秒

# 分布式 ID
SET id:user 1000
INCR id:user             # 返回 1001，作为下一个用户 ID

# 其他
SETEX key 60 "value"     # 设置值 + 过期时间（60秒）
SETNX key "value"        # key 不存在才设置
STRLEN key               # 获取字符串长度
APPEND key "append"      # 追加内容
GETRANGE key 0 3         # 截取子串
```

#### 典型应用场景

**1. 分布式锁：**

```bash
# 加锁
SET lock:order:1234 "thread-1" NX EX 30
# 解锁（使用 Lua 脚本保证原子性）
if redis.call("get", KEYS[1]) == ARGV[1] then
    return redis.call("del", KEYS[1])
else
    return 0
end
```

**2. 计数器：**

```bash
# 文章阅读量
INCR article:readcount:9527
# 当日登录用户数统计（每天一个 key）
INCR login:count:2026-06-13
```

**3. 缓存 JSON 对象：**

```bash
# 序列化用户对象为 JSON 字符串后存入
SET user:1001 '{"id":1001,"name":"张三","age":25}' EX 3600
```

**4. 分布式全局 ID：**

```bash
# 每个业务使用独立的 key，利用 INCR 获取单调递增 ID
INCR id:order
INCR id:product
```

---

### 1.2 Hash（哈希）

Hash 类似于 Java 中的 `Map<String, String>`，适合存储结构化对象。

#### 底层编码

Hash 底层使用两种编码：

| 编码方式 | 条件 | 描述 |
|----------|------|------|
| **ziplist**（压缩列表） | 同时满足：元素个数 `< 512`（hash-max-ziplist-entries）且每个 value 长度 `< 64 字节`（hash-max-ziplist-value） | 连续内存块存储，节省内存 |
| **hashtable**（字典） | 不满足 ziplist 条件时升级为 hashtable | O(1) 读写，内存开销较大 |

**ziplist 结构：** 特殊编码的双向链表，内存连续，每个 entry 包含前一个 entry 的长度（用于反向遍历）+ encoding + content。插入和删除可能引发连锁更新（链式扩容）。

**Redis 7.0 变化：** ziplist 被 listpack 取代，listpack 彻底解决了连锁更新问题。

#### 常用命令

```bash
HSET user:1001 name "张三" age "25" city "北京"
HGET user:1001 name            # 返回 "张三"
HGETALL user:1001              # 返回所有 field-value
HMGET user:1001 name age       # 批量获取多个 field
HMSET user:1001 email "zhangsan@example.com"  # 批量设置

HEXISTS user:1001 name         # 判断 field 是否存在
HDEL user:1001 age             # 删除 field
HLEN user:1001                 # field 数量
HINCRBY user:1001 age 1        # 原子自增某个 field
HKEYS user:1001                # 获取所有 field
HVALS user:1001                # 获取所有 value
HSTRLEN user:1001 name         # field 的 value 长度
```

#### 典型应用场景

**1. 用户信息缓存：**

```bash
# 替代 String JSON 序列化方式，Hash 支持单独更新某个字段
HSET session:token:abc123 user_id 1001 last_access "2026-06-13 10:30:00"
# 相比 String，Hash 修改一个字段无需整个反序列化再序列化
```

**2. 购物车实现：**

```bash
# 用户 1001 的购物车
HSET cart:1001 product:201 "2"    # 商品201 数量2
HSET cart:1001 product:302 "1"    # 商品302 数量1
HINCRBY cart:1001 product:201 1   # 商品201 数量 +1
HGETALL cart:1001                 # 查看整个购物车
HDEL cart:1001 product:302        # 移除商品302
HLEN cart:1001                    # 购物车中商品种类数
```

**3. 短链接跳转：**

```bash
HSET shortlink:abc123 url "https://example.com/long/url" create_time "2026-06-13" visit_count "0"
HINCRBY shortlink:abc123 visit_count 1
```

---

### 1.3 List（列表）

List 是按照插入顺序排序的字符串链表，可以在头部（Left）和尾部（Right）进行操作。

#### 底层实现：quicklist

Redis 3.2 之后，List 底层统一使用 **quicklist**，它是 **ziplist + 双向链表** 的混合体：

- quicklist 是一个双向链表
- 链表的每个节点是一个 **ziplist**（Redis 7.0 后为 listpack）
- 每个 ziplist 存储一定数量的元素（由 `list-max-ziplist-size` 控制）
- 这种设计在 **内存利用率** 和 **读写性能** 之间取得了平衡

```
quicklist
  ┌──────┐    ┌──────┐    ┌──────┐
  │ node │<-->│ node │<-->│ node │
  └──────┘    └──────┘    └──────┘
      │           │           │
  ┌───────┐  ┌───────┐  ┌───────┐
  │ziplist│  │ziplist│  │ziplist│
  │[e1,e2]│  │[e3,e4]│  │[e5,e6]│
  └───────┘  └───────┘  └───────┘
```

#### 常用命令

```bash
# 入队
LPUSH list1 "a"             # 从左侧插入，返回列表长度
RPUSH list1 "b"             # 从右侧插入
LPUSHX list1 "c"            # 仅当列表存在时插入
RPUSHX list1 "d"

# 出队
LPOP list1                  # 弹出左侧第一个元素
RPOP list1                  # 弹出右侧第一个元素
BLPOP list1 5               # 阻塞式弹出左侧，超时5秒（实现消息队列的关键）
BRPOP list1 5               # 阻塞式弹出右侧

# 查看
LRANGE list1 0 -1           # 查看全部元素（-1 表示最后一个）
LINDEX list1 0              # 获取指定下标元素
LLEN list1                  # 列表长度
LTRIM list1 0 10            # 截取保留 0~10 个元素（裁剪列表）

# 其他
LINSERT list1 BEFORE "a" "x" # 在元素前插入
LSET list1 0 "newvalue"     # 修改指定下标的元素
LREM list1 2 "a"            # 删除 2 个值为 "a" 的元素
RPOPLPUSH list1 list2       # 从 list1 尾部弹出到 list2 头部（安全队列，备份机制）
```

#### 典型应用场景

**1. 消息队列（LPUSH + BRPOP）：**

```bash
# 生产者
LPUSH queue:task "send_email:user1001"
LPUSH queue:task "generate_report:2026-06"

# 消费者（阻塞等待，长轮询）
BRPOP queue:task 0    # 0 表示永不超时
```

**2. 最新动态/时间线（LPUSH + LTRIM）：**

```bash
# 用户 1001 的朋友圈时间线，只保留最近 50 条
LPUSH timeline:1001 "post:9527"
LTRIM timeline:1001 0 49
# LRANGE timeline:1001 0 -1  # 获取全部最新动态
```

**3. 栈结构（LPUSH + LPOP）：**

```bash
LPUSH stack "action1"
LPUSH stack "action2"
LPOP stack          # 返回 "action2"，LIFO
```

---

### 1.4 Set（集合）

Set 是无序且元素唯一的字符串集合，适合做交并差运算。

#### 底层编码

| 编码方式 | 条件 | 结构 |
|----------|------|------|
| **intset**（整数集合） | 全部元素为整数且数量 `< set-max-intset-entries`（默认 512） | 有序整数数组，二分查找 O(logN) |
| **hashtable**（哈希表） | 不满足 intset 条件时升级 | value 指向 NULL，只利用 key 的唯一性 |

**intset 升级：** 当存入一个比当前编码类型更大的整数时（如从 int16_t 升级到 int32_t），触发全量升级，所有元素重新分配内存。

#### 常用命令

```bash
SADD set1 "a" "b" "c"       # 添加元素，返回成功添加的数量
SREM set1 "a"                # 移除元素
SCARD set1                   # 获取元素数量
SMEMBERS set1                # 返回所有元素（无序，慎用于大集合）
SISMEMBER set1 "a"           # 判断元素是否存在（O(1)）

# 交并差运算（核心应用场景）
SINTER set1 set2             # 交集
SUNION set1 set2             # 并集
SDIFF set1 set2              # 差集（在 set1 中但不在 set2 中）
SINTERSTORE dest set1 set2   # 交集结果存储到 dest
SUNIONSTORE dest set1 set2
SDIFFSTORE dest set1 set2

# 随机操作
SPOP set1                    # 随机弹出一个元素
SRANDMEMBER set1 3           # 随机返回 3 个元素（不弹出）

# 移动
SMOVE set1 set2 "a"          # 将元素从 set1 移动到 set2
```

#### 典型应用场景

**1. 共同好友/关注（SINTER）：**

```bash
SADD user:1001:follow "user:2001" "user:2002" "user:2003"
SADD user:1002:follow "user:2002" "user:2003" "user:2004"

# 共同关注
SINTER user:1001:follow user:1002:follow
# 可能认识的人：取差集再随机返回
SDIFF user:1001:follow user:1002:follow
```

**2. 标签系统：**

```bash
SADD article:9527:tags "java" "redis" "spring"
SADD tag:java:articles "article:9527" "article:9528"

# 同时含有 "java" 和 "redis" 标签的文章
SINTER tag:java:articles tag:redis:articles
```

**3. 抽奖系统（SPOP）：**

```bash
SADD lottery:20260613 "user1" "user2" "user3" ... "userN"

# 抽取 1 个一等奖
SPOP lottery:20260613
# 抽取 3 个二等奖
SPOP lottery:20260613 3
```

**4. 网站 UV 去重（非精确场景）：**

```bash
SADD uv:article:9527 "ip:192.168.1.1"
SCARD uv:article:9527        # 返回 UV 数
# 注意：Set 方式精确但内存占用大，大数据量建议用 HyperLogLog
```

---

### 1.5 ZSet（Sorted Set / 有序集合）

ZSet 在 Set 的基础上增加了 `score` 分值，元素按 score 排序。

#### 底层编码

| 编码方式 | 条件 | 结构 |
|----------|------|------|
| **ziplist** | 元素 `< zset-max-ziplist-entries（128）` 且 value 长度 `< zset-max-ziplist-value（64）` | 按 score 有序存储，每个 entry 为 value+score 成对出现 |
| **skiplist + dict** | 不满足 ziplist 条件时 | skiplist 保证有序性和范围查询 O(logN)，dict 保证单点查询 O(1) |

#### Skiplist（跳表）原理

Skiplist 是一种基于 **多层有序链表** 的概率性数据结构，由 William Pugh 在 1989 年提出。Redis 使用它替代平衡树（如红黑树）来实现 ZSet。

**结构示意：**

```
Level 4:  -∞ ────────────────────────────> 100 ──────────> +∞
Level 3:  -∞ ──────────> 50 ────────────> 100 ──────────> +∞
Level 2:  -∞ ──> 30 ───> 50 ──> 70 ─────> 100 ──> 130 ──> +∞
Level 1:  -∞ ──> 10 ──> 30 ──> 50 ──> 60 ──> 70 ──> 90 ──> 100 ──> 120 ──> 130 ──> +∞
```

**核心特点：**

- **概率平衡：** 每个节点在创建时决定层数（`power law` 随机，多数节点在低层）
- **查询 O(logN)：** 从最高层开始向右查找，遇到更大值则降一层
- **插入 O(logN)：** 先查询插入位置，随机确定层数后更新指针
- **范围查询高效：** 找到起点后沿底层链表遍历即可
- **实现简单：** 比红黑树更易实现和调试

**为何不用红黑树？** Skiplist 实现更简单，支持范围查询更方便（无需中序遍历），且对内存更友好。

#### 常用命令

```bash
# 添加元素
ZADD leaderboard 100 "user:1001" 200 "user:1002" 50 "user:1003"

# 查询
ZRANGE leaderboard 0 -1            # 按 score 升序返回（带下标）
ZREVRANGE leaderboard 0 2          # 降序取前 3 名（排行榜）
ZRANGEBYSCORE leaderboard 80 200   # 按分数范围查询
ZSCORE leaderboard "user:1001"     # 获取某个元素的分数
ZCARD leaderboard                  # 元素数量

# 更新
ZINCRBY leaderboard 30 "user:1001" # 给用户加分，原子操作

# 删除
ZREM leaderboard "user:1003"       # 删除元素
ZREMRANGEBYRANK leaderboard 0 0    # 删除最低分的一个
ZREMRANGEBYSCORE leaderboard 0 50  # 删除分数 <= 50 的

# 排名
ZRANK leaderboard "user:1001"      # 升序排名（从 0 开始）
ZREVRANK leaderboard "user:1001"   # 降序排名

# 集合运算
ZUNIONSTORE dest 2 zset1 zset2 WEIGHTS 1 2 AGGREGATE SUM
ZINTERSTORE dest 2 zset1 zset2 WEIGHTS 1 1 AGGREGATE MAX

# 按字典序（当 score 相同时）
ZRANGEBYLEX set1 [a [z             # 字典序范围
ZLEXCOUNT set1 [a [z               # 字典序范围内元素数量
```

#### 典型应用场景

**1. 排行榜（最经典）：**

```bash
# 每日热榜
ZADD hot:20260613 5000 "article:1001" 3200 "article:1002"

# 获取前三名（带分数）
ZREVRANGE hot:20260613 0 2 WITHSCORES

# 定时更新
ZINCRBY hot:20260613 100 "article:1001"  # 增加热度
```

**2. 延时队列（score = 时间戳）：**

```bash
# 订单超时取消：score 为超时时间戳
ZADD delay:order:cancel 1718200000 "order:8888"
ZADD delay:order:cancel 1718286400 "order:8889"

# 消费者轮询，取出已到期的任务
while true:
    # 返回当前时间之前的所有任务
    orders = ZRANGEBYSCORE delay:order:cancel 0 now WITHSCORES LIMIT 0 100
    for order in orders:
        if ZREM delay:order:cancel order["member"] > 0:
            process(order)  # 执行取消逻辑
    sleep(1)
```

**3. 带权重的消息队列：**

```bash
# VIP 用户的消息权重更高，score 为优先级
ZADD msg:queue 10 "vip_msg:001"    # 优先级 10
ZADD msg:queue 5 "normal_msg:002"  # 优先级 5
ZADD msg:queue 1 "low_msg:003"     # 优先级 1

# 消费者总是取优先级最高的
ZREVRANGE msg:queue 0 0
```

---

## 二、高级数据结构

### 2.1 Bitmap（位图）

Bitmap 实际上就是 String 类型的二进制位操作，一个 String 最大 512MB，即最多有 `2^32` 个 bit 位。

#### 底层原理

Bitmap 通过 SETBIT/GETBIT 等命令直接操作字符串上的二进制位（bit），每个 bit 可以表示 0 或 1，**极大地节省了内存**。例如，记录 1 亿用户的签到状态，仅需 `100000000 / 8 / 1024 / 1024 ≈ 12MB`。

#### 常用命令

```bash
SETBIT sign:20260613 1001 1     # 用户 1001 签到（第 1001 位设为 1）
GETBIT sign:20260613 1001       # 查询用户 1001 是否签到
BITCOUNT sign:20260613          # 统计当天签到总人数
BITPOS sign:20260613 0          # 查找第一个未签到的用户位置

# BITOP 位运算
SETBIT sign:20260613 1001 1
SETBIT sign:20260614 1001 1
# 连续签到用户
BITOP AND dest sign:20260613 sign:20260614
BITCOUNT dest                   # 连续两天签到人数

# 日活统计（user_id 作为 bit 偏移）
SETBIT dau:2026-06-13 42 1      # 用户 42 访问
SETBIT dau:2026-06-13 100 1
BITCOUNT dau:2026-06-13         # 当天日活
```

#### 典型应用场景

**1. 用户签到：**

```bash
# 用户 1001 全年签到记录
SETBIT sign:2026:1001 0 1       # 1月1日
SETBIT sign:2026:1001 100 1     # 4月11日
BITCOUNT sign:2026:1001         # 全年签到天数
```

**2. 日活用户统计（DAU）：**

```bash
# 每个用户映射到一个 bit 偏移位
SETBIT dau:20260613 1001 1
SETBIT dau:20260613 1002 1
# 周活：7 天日活的 OR 结果
# 月活：30 天日活的 OR 结果
```

**3. 布隆过滤器简化版：**

```bash
# 使用多个 hash 函数计算多个 bit 位置
# 例如存储 "spam:keyword1"
SETBIT bloom:spam h1("spam:keyword1") 1
SETBIT bloom:spam h2("spam:keyword1") 1
SETBIT bloom:spam h3("spam:keyword1") 1
# 检查时所有位都为 1 则可能存在
```

---

### 2.2 HyperLogLog（基数统计）

HyperLogLog 是一种 **概率性基数统计** 算法，用于计算集合中不重复元素的数量，牺牲极小的精确度换取极低的内存消耗。

#### 核心特性

| 特性 | 值 |
|------|-----|
| 标准误差 | 0.81% |
| 单 key 内存 | 固定 12KB |
| 可统计基数范围 | 最大 2^64（约 1.8×10^19） |
| 是否可回查元素 | 不可以，只能统计基数 |

#### 常用命令

```bash
PFADD uv:article:9527 "user:1001" "user:1002" "user:1001"
PFCOUNT uv:article:9527                # 返回 2（用户1001去重）
PFMERGE dest source1 source2           # 合并多个 HyperLogLog
```

#### 典型应用场景

**UV（独立访客）统计：**

```bash
# 每个页面 UV
PFADD uv:index "ip:192.168.1.1"
PFADD uv:index "ip:192.168.1.2"
PFCOUNT uv:index  # 返回独立 IP 数

# 全局 UV
PFADD uv:site:20260613 "uid:1001" "uid:1002"
PFADD uv:site:20260613 "uid:1001"
PFCOUNT uv:site:20260613  # 返回 2
```

**注意事项：** HyperLogLog 适用场景是不需要精确数据的统计场景，如果需要精确计数应使用 Set 或 精确数据库。

---

### 2.3 Geo（地理空间）

Geo 是 Redis 3.2 引入的地理空间索引功能，底层基于 ZSet + GeoHash 编码实现。

#### 底层原理

1. 每个地理位置通过 **GeoHash 算法** 编码为一个 52 位整数（二维经纬度降维到一维整数）
2. 这个整数作为 ZSet 的 **score** 存储
3. 利用 ZSet 的 score 范围查询能力实现经纬度范围搜索
4. GeoHash 的编码长度决定了精度（越长越精确），且编码前缀匹配表示空间邻近

#### 常用命令

```bash
GEOADD cities 116.397128 39.916527 "beijing"   # 添加北京
GEOADD cities 121.473701 31.230416 "shanghai"  # 添加上海
GEOADD cities 113.264385 23.129110 "guangzhou" # 添加广州

GEODIST cities beijing shanghai km    # 两地距离（单位：km）
GEODIST cities beijing shanghai m     # 两地距离（单位：m）

GEOPOS cities beijing                  # 获取经纬度
GEOHASH cities beijing                 # 获取 GeoHash 字符串（11字符）

# 查找附近的人（半径 100km 内，前 10 个，带距离）
GEORADIUS cities 116.39 39.91 100 km WITHDIST COUNT 10 ASC

# 以某个已有成员为中心
GEORADIUSBYMEMBER cities beijing 500 km WITHDIST COUNT 10
```

#### 典型应用场景

**1. 附近的人/LBS 服务：**

```bash
# 用户上报位置
GEOADD user:location 116.397128 39.916527 "user:1001"

# 查找用户 1001 附近 1000 米内的用户
GEORADIUSBYMEMBER user:location "user:1001" 1000 m WITHDIST COUNT 20

# 查找指定坐标附近的商铺
GEOADD shops 116.397128 39.916527 "shop:8888"
GEORADIUS shops 116.40 39.92 500 m WITHDIST WITHCOORD COUNT 10
```

---

### 2.4 Stream（流）

Stream 是 Redis 5.0 引入的消息队列数据结构，它解决了 List 作为消息队列的诸多不足（如无法多消费者订阅、消息确认困难等），提供了类似 Kafka 的消费者组机制。

#### 核心概念

| 概念 | 说明 |
|------|------|
| **消息** | 由有序递增 ID + 多个 field-value 对组成 |
| **消费者组** | 多个消费者可以分组消费，消息在组内被分摊 |
| **待处理列表（PEL）** | 已投递但未确认的消息列表，确保 at-least-once 语义 |
| **消息确认（ACK）** | 消费者处理完成后发送 XACK，消息从 PEL 移除 |

#### 常用命令

```bash
# 添加消息（* 表示自动生成时间戳序列号 ID）
XADD mystream * sensor-id 1234 temperature 19.8
XADD mystream * sensor-id 1235 temperature 20.1

# 读取消息
XRANGE mystream - +            # 读取所有消息
XRANGE mystream 1700000000000-0 + COUNT 10  # 从某个 ID 开始读 10 条
XREVRANGE mystream + - COUNT 5 # 反向读取最新 5 条
XREAD COUNT 2 BLOCK 5000 STREAMS mystream 0  # 阻塞读取

# 消费者组
XGROUP CREATE mystream mygroup $ MKSTREAM  # 创建消费者组（$ 表示从最新开始）
XGROUP CREATE mystream mygroup 0 MKSTREAM  # 从第一条开始

# 消费者读取（consumer1 从 mygroup 中读取消息）
XREADGROUP GROUP mygroup consumer1 COUNT 1 BLOCK 5000 STREAMS mystream >

# 消息确认
XACK mystream mygroup 1700000000000-0

# 查看待处理消息
XPENDING mystream mygroup

# 转移消息（某消费者崩溃后，转移其待处理消息给其他消费者）
XCLAIM mystream mygroup consumer2 3600000 1700000000000-0

# 查看 Stream 信息
XINFO STREAM mystream
XINFO GROUPS mystream
XINFO CONSUMERS mystream mygroup
```

#### Stream 与 List 作为消息队列的对比

| 特性 | List (LPUSH+BRPOP) | Stream |
|------|-------------------|--------|
| 消息可靠性 | 无 ACK，消费者崩溃即丢失 | 支持 ACK + PEL，可靠投递 |
| 消费者组 | 不支持 | 原生支持，负载均衡消费 |
| 消息回溯 | 不支持 | 支持从头重新消费 |
| 阻塞读取 | BRPOP 支持 | XREADGROUP 支持 |
| 多播 | 不支持 | 多消费者组，各自独立消费 |
| 复杂度 | 低 | 中等 |

#### 典型应用场景

**1. 可靠消息队列（订单处理）：**

```bash
# 生产者：创建订单
XADD order:events * event "order_created" order_id "8888" user_id "1001" amount "299.00"

# 消费者组：order_processor 组
XGROUP CREATE order:events order_processor $ MKSTREAM

# 消费者 1 处理
XREADGROUP GROUP order_processor consumer1 COUNT 1 BLOCK 2000 STREAMS order:events >
# 处理完成后确认
XACK order:events order_processor 1700000000000-0
```

---

### 2.5 Bloom Filter（布隆过滤器）

Bloom Filter 是一种 **概率数据结构**，用于判断一个元素是否 **可能** 存在于集合中。它的特性：

- **查询不存在：一定不存在**（绝对准确）
- **查询存在：可能存在**（有误判率）

#### 底层原理

1. 初始化一个长度为 m 的 bit 数组，所有位为 0
2. 添加元素时，使用 k 个独立的哈希函数计算 k 个位置，全部设为 1
3. 查询时，检查 k 个位置是否全为 1
   - 如果有 0，则元素 **一定不在**
   - 如果全为 1，则元素 **可能在**（可能误判）

#### 所需模块

RedisBloom 模块（自 Redis Stack 内置）：
```bash
# 加载模块
redis-server --loadmodule /path/to/redisbloom.so

# 使用
BF.BLOOMFILTER myfilter 0.01 100000  # 创建，误判率1%，预期容量10万
BF.ADD myfilter "user:1001"
BF.EXISTS myfilter "user:1001"  # 返回 1
BF.EXISTS myfilter "user:9999"  # 返回 0（一定不存在）
BF.MADD myfilter "a" "b" "c"    # 批量添加
BF.MEXISTS myfilter "a" "b"     # 批量查询

# 动态布隆过滤器（可扩容）
BF.RESERVE myfilter 0.01 100000 EXPANSION 2  # expansion=2 扩容加倍
BF.INSERT myfilter CAPACITY 100000 ERROR 0.01 ITEMS "a" "b"
```

#### 典型应用场景

**缓存穿透防护（最经典）：**

```java
// 流程：查询数据前，先检查布隆过滤器
// 如果布隆过滤器说不存在，直接返回 null，避免穿透到 DB
public Object queryWithBloomFilter(String key) {
    // 1. 先查布隆过滤器
    if (!redis.call("BF.EXISTS", "bloom:filter", key)) {
        return null; // 一定不存在，直接返回
    }
    // 2. 布隆过滤器说可能存在，查缓存
    Object cache = redisTemplate.opsForValue().get(key);
    if (cache != null) return cache;
    // 3. 查数据库
    Object db = queryDB(key);
    if (db != null) {
        redisTemplate.opsForValue().set(key, db, 3600, TimeUnit.SECONDS);
    }
    return db;
}
```

---

## 三、持久化机制

Redis 是内存数据库，数据保存在内存中。为防止进程退出或机器宕机导致数据丢失，Redis 提供了 **RDB** 和 **AOF** 两种持久化方式。

### 3.1 RDB（Redis Database）

RDB 是 **快照式** 持久化，将某个时间点的全量数据写入磁盘文件（默认 `dump.rdb`）。

#### 触发方式

| 方式 | 命令 | 特点 |
|------|------|------|
| **手动 - SAVE** | `SAVE` | **阻塞** 主进程直至完成，期间不处理任何请求 |
| **手动 - BGSAVE** | `BGSAVE` | **fork 子进程** 写入 RDB，主进程继续提供服务 |
| **自动** | `save 900 1` | 900 秒内至少 1 次修改，触发 BGSAVE |
| **自动** | `save 300 10` | 300 秒内至少 10 次修改 |
| **自动** | `save 60 10000` | 60 秒内至少 10000 次修改 |
| **关闭时** | `shutdown` | 自动执行 SAVE（如果配置了 RDB） |

#### 配置

```bash
# redis.conf
save 900 1
save 300 10
save 60 10000
dbfilename dump.rdb
dir /var/lib/redis/
stop-writes-on-bgsave-error yes  # BGSAVE 失败时停止写入
rdbcompression yes               # 是否压缩（LZF 压缩）
rdbchecksum yes                  # 写入时校验和
```

#### BGSAVE 原理（Copy-on-Write）

1. 主进程 fork 一个子进程
2. 子进程将 Redis 内存数据写入临时 RDB 文件
3. 子进程利用 **子进程与主进程共享内存页**（fork 的特性）
4. 主进程继续处理请求，若修改某个内存页，触发 **写时复制（Copy-on-Write）**，复制该页到新位置再修改（主进程修改的是自己的副本）
5. 子进程写入完成后，用临时 RDB 文件覆盖旧文件

**写时复制的代价：** 若在此期间有大量写操作，内存页复制导致额外内存开销和 CPU 消耗。

#### 优缺点

| 优点 | 缺点 |
|------|------|
| 文件紧凑，恢复速度快 | 可能丢失最后一次快照后的数据 |
| 适合备份和灾难恢复 | fork 子进程可能耗时（Redis 内存越大，fork 越慢） |
| 子进程不影响主进程服务 | 数据量大时 I/O 压力大 |

---

### 3.2 AOF（Append Only File）

AOF 以 **追加写命令** 的方式记录每一条写操作，恢复时重放这些命令。

#### 工作流程

```
SET key value  =>  追加到 AOF 缓冲区  =>  根据策略 fsync 到磁盘
```

#### 刷盘策略（appendfsync）

| 策略 | 说明 | 安全性 | 性能 |
|------|------|--------|------|
| **always** | 每条写命令都 fsync 到磁盘 | 最高，最多丢 1 条命令 | 最慢 |
| **everysec** | 每秒钟 fsync 一次 | 中等，最多丢 1 秒数据 | 性能好（默认） |
| **no** | 由操作系统决定何时刷盘（通常 30 秒内） | 最低，可能丢大量数据 | 最快 |

#### 配置

```bash
# redis.conf
appendonly yes
appendfilename "appendonly.aof"
appendfsync everysec
auto-aof-rewrite-percentage 100   # AOF 文件比上次重写时增长 100% 触发重写
auto-aof-rewrite-min-size 64mb    # 文件至少 64MB 才触发重写
no-appendfsync-on-rewrite no      # 重写时是否暂停 fsync
```

#### AOF 重写（BGREWRITEAOF）

AOF 文件会不断增大，Redis 通过 **重写** 来压缩 AOF 文件。

**重写原理：** 读取当前内存中的键值对状态，转换成最小的写命令集合（如 `RPUSH list a b c` 替代多次 `RPUSH`）。

**触发方式：**
- 手动：`BGREWRITEAOF`
- 自动：`auto-aof-rewrite-percentage` + `auto-aof-rewrite-min-size`

**重写过程：**
1. fork 子进程
2. 子进程将当前内存数据转化为写命令，写入新 AOF 文件
3. 重写期间主进程的写命令，同时追加到 **旧 AOF 缓冲区** 和 **重写缓冲区**
4. 子进程完成后，父进程将重写缓冲区内容追加到新 AOF 文件
5. 用新 AOF 文件替换旧文件

#### 优缺点

| 优点 | 缺点 |
|------|------|
| 数据安全性高（everysec 最多丢 1 秒数据） | AOF 文件比 RDB 大 |
| AOF 文件可读，可手动修复 | 恢复速度比 RDB 慢 |
| 支持 AOF 重写自动压缩 | 性能开销略高于 RDB |

---

### 3.3 混合持久化（Redis 4.0+）

#### 原理

Redis 4.0 引入混合持久化模式，将 **RDB 的快速恢复** 与 **AOF 的数据安全** 结合。

**工作方式：** AOF 重写时，不再是纯 AOF 格式的命令追加，而是：

```
[RDB 格式的全量数据] + [AOF 格式的增量命令]
```

**恢复过程：**
1. 加载 RDB 部分（快速恢复全量数据）
2. 重放 AOF 部分（应用重写期间的增量命令）

#### 配置

```bash
# redis.conf
aof-use-rdb-preamble yes
```

#### 优势

- **重启恢复更快：** RDB 部分恢复全量数据，比纯 AOF 重放快得多
- **数据更安全：** 增量 AOF 部分保证重写期间的写命令不丢失
- **文件更小：** 比纯 AOF 文件小

---

### 3.4 持久化选择建议

| 场景 | 推荐方案 |
|------|----------|
| 允许几分钟数据丢失，追求极致性能 | 仅 RDB |
| 不允许数据丢失 | 仅 AOF（everysec）或混合持久化 |
| 两者都要（推荐） | RDB + AOF 混合持久化 |
| 纯缓存场景，可以全丢 | 关闭持久化 |

> **RDB vs AOF 恢复优先级：** 当两种文件都存在时，Redis 优先使用 AOF 恢复（数据更完整）。

---

## 四、高可用架构

### 4.1 主从复制（Replication）

主从复制是 Redis 高可用的基础，一个 Master 可以有多个 Slave。

#### 复制流程（PSYNC2，Redis 4.0+）

**1. 全量复制（Full Resynchronization）：**

```
Master                    Slave
  |                        |
  |<--- PSYNC ? -167508--- |  # 发送 runid 和偏移量
  |--- +FULLRESYNC runid --|> # 需要全量复制
  |--- 执行 BGSAVE --------|  # Master 生成 RDB 快照
  |--- 发送 RDB 文件 ------|>  # Slave 清空自身数据并加载
  |--- 发送复制缓冲区数据 ->|>  # 缓冲区内增量命令
```

**2. 增量复制（Partial Resynchronization）：**

当 Slave 重连时，如果复制偏移量仍在 Master 的 **积压缓冲区（repl-backlog）** 内，则只发送缺失的增量数据，避免全量复制的开销。

**核心要素：**
- **replication ID（runid）：** 标识 Master 实例
- **replication offset（复制偏移量）：** 已复制的字节数
- **repl-backlog-size（积压缓冲区）：** 默认 1MB，决定增量复制的容忍范围

#### 复制风暴

- 当 Master 挂掉后，多个 Slave 同时尝试全量复制到新 Master，导致网络和 CPU 瞬间过载
- **解决方案：** 使用树形复制结构（Slave 下面再挂 Slave），或限制同时重连的 Slave 数量

#### 配置

```bash
# Slave 节点配置
replicaof 192.168.1.100 6379
replica-serve-stale-data yes   # 同步期间是否继续提供服务
replica-read-only yes          # 从节点默认只读

# Master 节点配置
repl-backlog-size 1mb          # 积压缓冲区大小
repl-backlog-ttl 3600          # 无 Slave 时保留积压缓冲区的秒数
repl-diskless-sync no          # 是否无盘复制（直接网络传输）
min-replicas-to-write 3        # 最少写复制节点数
min-replicas-max-lag 10        # 最大延迟秒数
```

---

### 4.2 哨兵模式（Sentinel）

Sentinel 是 Redis 的高可用解决方案，提供 **自动故障转移**。

#### 核心功能

1. **监控：** 持续检查 Master 和 Slave 是否正常运行
2. **自动故障转移：** Master 宕机时，自动将某个 Slave 提升为 Master
3. **通知：** 将新 Master 地址通知客户端
4. **配置提供：** 充当服务发现的角色

#### 故障转移流程

```
1. 主观下线（SDOWN）：单个 Sentinel 认为 Master 不可达
2. 客观下线（ODOWN）：多个 Sentinel（>= quorum）都认为 Master 不可达
3. 选举 Leader Sentinel：通过 Raft 算法选出负责故障转移的 Sentinel
4. 选新 Master：Leader 选举一个 Slave 作为新 Master
   - 优先级（replica-priority）最高
   - 复制偏移量最大（数据最新）
   - runid 最小
5. 通知所有 Slave 复制新 Master
6. 通知客户端新 Master 地址

Sentinel 数量要求：至少 3 个实例（奇数），quorum = 2
```

#### 配置

```bash
# sentinel.conf
sentinel monitor mymaster 127.0.0.1 6379 2  # 2 为 quorum
sentinel down-after-milliseconds mymaster 5000  # 5 秒无响应判定为下线
sentinel failover-timeout mymaster 60000       # 故障转移超时
sentinel parallel-syncs mymaster 1             # 同时同步的 Slave 数量
sentinel auth-pass mymaster password           # 认证密码
```

**客户端连接模式：** 不直接连 Redis，而是连 Sentinel，通过 Sentinel 获取当前 Master 地址。

---

### 4.3 集群模式（Cluster）

Redis Cluster 是 Redis 3.0 引入的官方分布式解决方案，实现了 **数据分片（Sharding）** 和 **高可用**。

#### 数据分片：槽（Slot）

```
Hash Slot = CRC16(key) % 16384

集群中的 16384 个哈希槽被分配到各节点
例如 3 个节点：
  Node A: 0~5460
  Node B: 5461~10922
  Node C: 10923~16383
```

**为什么是 16384 个槽？** CRC16 产生的值是 16 位的（0~65535），但 Redis 取 16384，原因是：
- 心跳包使用位图（bitmap）来传递槽信息，16384 个槽的位图 = 2KB，而 65535 个槽需要 8KB
- 节点数量一般不会超过 1000 个，16384 足够

#### MOVED 重定向

```bash
# 客户端向 Node A 查询 key，如果该 key 的槽不在 A 上
-MOVED 3999 192.168.1.2:6379
# 客户端需重新向 192.168.1.2:6379 发起请求（客户端实现 smart 路由）
```

#### ASK 重定向

槽迁移过程中，数据可能部分在源节点、部分在目标节点：

```bash
-ASK 3999 192.168.1.3:6379
# 客户端先发 ASKING 命令，再发请求到目标节点
```

#### 节点间通信：Gossip 协议

节点使用 **Gossip 协议** 交换状态信息：

- **PING/PONG：** 定期随机向其他节点发送 PING，获取状态
- **FAIL 消息：** 当某个节点被半数以上节点标记为 PFAIL（疑似下线）时，广播 FAIL 消息
- **更新拓扑：** 节点增删时，通过 Gossip 传播到全集群

**Gossip 的优势：** 不需要中心节点，集群规模可以扩展。

#### 主要限制

- 不支持多 key 操作（如果 key 不在同一 slot）
- 支持事务，但事务内所有 key 必须在同一节点
- 不支持分页通配符查询（如 `KEYS`）

---

### 4.4 一致性哈希（Consistent Hashing）

Redis Cluster 使用的不是一致性哈希，而是 **固定哈希槽** 方案。一致性哈希在 Redis 客户端（如 Jedis ShardedJedis、Twemproxy）中应用更多。

#### 原理

1. 构建一个 `[0, 2^32 - 1]` 的哈希环
2. 对节点进行哈希，放到环上
3. 对 key 进行哈希，沿环顺时针找到第一个节点

#### 虚拟节点

为了解决 **数据倾斜** 问题，一致性哈希引入虚拟节点：

- 每个物理节点在环上对应多个虚拟节点
- 当某个节点下线，其数据被均摊到其他节点（而非全部压到下一个节点）

```
无虚拟节点：
  Node A 下线 => Node A 的所有数据全部转移到 Node B
有虚拟节点：
  Node A 下线 => Node A 的虚拟节点均匀分布到 Node B/C/D
```

**集中式方案（Codis/Redis Cluster 的 Proxy 模式）：**

- 使用 ZooKeeper 或 etcd 存储槽位映射关系
- 槽迁移（resharding）在线进行
- 对客户端透明

---

## 五、缓存实战（重点）

### 5.1 缓存穿透

**定义：** 查询一个 **根本不存在** 的数据，导致请求每次都被打到数据库。

**流程：** `查询缓存（无） -> 查询数据库（无） -> 不写入缓存` 循环

#### 解决方案

**方案一：布隆过滤器（推荐）**

```java
// 前置布隆过滤器，拦截不存在的 key
public Object queryWithBloom(String key) {
    // 1. 布隆过滤器拦截
    if (!bloomFilter.mightContain(key)) {
        return null;  // 一定不存在
    }
    // 2. 查缓存
    Object cache = redisTemplate.opsForValue().get(key);
    if (cache != null) return cache;
    // 3. 查数据库
    Object db = queryDB(key);
    if (db != null) {
        redisTemplate.opsForValue().set(key, db, 3600, TimeUnit.SECONDS);
    }
    return db;
}
```

**方案二：缓存空值（短 TTL）**

```java
public Object queryWithNullCache(String key) {
    Object cache = redisTemplate.opsForValue().get(key);
    if (cache != null) {
        // 增加空值判断
        if ("NULL_VALUE".equals(cache)) return null;
        return cache;
    }
    Object db = queryDB(key);
    if (db != null) {
        redisTemplate.opsForValue().set(key, db, 3600, TimeUnit.SECONDS);
    } else {
        // 缓存空值，TTL 极短（防止长期占用）
        redisTemplate.opsForValue().set(key, "NULL_VALUE", 60, TimeUnit.SECONDS);
    }
    return db;
}
```

**方案三：参数校验（最基础）**

```java
// 在 API 入口层拒绝非法参数（如 id < 0 直接返回）
public Object queryProduct(Integer id) {
    if (id == null || id <= 0) {
        throw new IllegalArgumentException("非法参数");
    }
    // ... 正常查询
}
```

---

### 5.2 缓存击穿

**定义：** 一个 **热点 key** 在过期瞬间，大量并发请求同时涌入，全部穿透到数据库。

**与穿透的区别：** 击穿是 key 存在但刚好过期，而穿透是 key 本身不存在。

#### 解决方案

**方案一：互斥锁（分布式锁，推荐）**

```java
public Object queryWithMutex(String key) {
    Object cache = redisTemplate.opsForValue().get(key);
    if (cache != null) return cache;

    String lockKey = "lock:" + key;
    String threadId = UUID.randomUUID().toString();

    // 尝试获取锁（SET NX EX）
    Boolean locked = redisTemplate.opsForValue()
        .setIfAbsent(lockKey, threadId, 30, TimeUnit.SECONDS);

    if (Boolean.TRUE.equals(locked)) {
        try {
            // 双重检查：可能其他线程已经重建了缓存
            cache = redisTemplate.opsForValue().get(key);
            if (cache != null) return cache;

            // 查数据库
            Object db = queryDB(key);
            redisTemplate.opsForValue().set(key, db, 3600, TimeUnit.SECONDS);
            return db;
        } finally {
            // 释放锁（Lua 脚本保证原子性）
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                           "return redis.call('del', KEYS[1]) else return 0 end";
            redisTemplate.execute(new DefaultRedisScript<>(script, Long.class),
                    Arrays.asList(lockKey), threadId);
        }
    } else {
        // 没拿到锁，等待后重试
        Thread.sleep(50);
        return queryWithMutex(key);  // 递归重试，注意防栈溢出
    }
}
```

**方案二：逻辑过期（主动更新 + 后台刷新）**

```java
// 缓存中存的是逻辑过期时间，而非 Redis 的 TTL
// 每次读取时检查逻辑时间是否过期
// 如果过期，尝试获取分布式锁，后台线程异步更新缓存
// 其他线程返回旧数据（而不是等待）

public class CacheData<T> {
    private T data;
    private long expireTime;  // 逻辑过期时间戳
    // getter/setter...
}

public Object queryWithLogicalExpire(String key) {
    // 1. 查缓存
    CacheData<Object> cacheData = (CacheData<Object>) redisTemplate.opsForValue().get(key);
    long now = System.currentTimeMillis();

    // 2. 未过期，直接返回
    if (cacheData != null && cacheData.getExpireTime() > now) {
        return cacheData.getData();
    }

    // 3. 逻辑过期，尝试加锁
    String lockKey = "lock:" + key;
    Boolean locked = redisTemplate.opsForValue()
        .setIfAbsent(lockKey, "1", 3, TimeUnit.SECONDS);

    if (Boolean.TRUE.equals(locked)) {
        // 后台异步更新缓存
        executorService.submit(() -> {
            Object db = queryDB(key);
            CacheData<Object> newData = new CacheData<>();
            newData.setData(db);
            newData.setExpireTime(now + 3600 * 1000);
            redisTemplate.opsForValue().set(key, newData);
            redisTemplate.delete(lockKey);
        });
    }

    // 4. 返回旧数据（即使逻辑过期也返回旧数据）
    return cacheData != null ? cacheData.getData() : null;
}
```

---

### 5.3 缓存雪崩

**定义：** 大量缓存 key 在同一时间 **集中过期**，或 Redis 服务宕机，导致大量请求直接打到数据库。

#### 解决方案

**方案一：过期时间加随机值**

```java
// 缓存设置时，在 TTL 基础上加上随机秒数
int ttl = 3600 + RandomUtils.nextInt(0, 300);  // 3600 + 0~300 秒随机
redisTemplate.opsForValue().set(key, value, ttl, TimeUnit.SECONDS);
```

**方案二：多级缓存（本地缓存 + Redis 缓存）**

```java
// 使用 Caffeine 作为一级本地缓存
Cache<String, Object> localCache = Caffeine.newBuilder()
    .maximumSize(10000)
    .expireAfterWrite(10, TimeUnit.SECONDS)  // 本地缓存 TTL 较短
    .build();

public Object queryWithMultiLevel(String key) {
    // 1. 查本地缓存
    Object local = localCache.getIfPresent(key);
    if (local != null) return local;

    // 2. 查 Redis
    Object redis = redisTemplate.opsForValue().get(key);
    if (redis != null) {
        localCache.put(key, redis);  // 回填本地缓存
        return redis;
    }

    // 3. 查数据库
    Object db = queryDB(key);
    if (db != null) {
        redisTemplate.opsForValue().set(key, db, 3600, TimeUnit.SECONDS);
        localCache.put(key, db);
    }
    return db;
}
```

**方案三：限流 + 熔断**

```java
// 使用 Sentinel、Hystrix、Resilience4j 进行限流保护
// 当数据库层压力过大时，直接熔断，返回默认值
```

**方案四：Redis 高可用 + 持久化**

- 使用 Sentinel 或 Cluster 防止单点故障
- 开启持久化，宕机后快速恢复

---

### 5.4 缓存一致性

**问题核心：** 当数据库数据更新后，如何保证缓存中的数据和数据库一致？

#### 方案一：Cache Aside Pattern（旁路缓存，推荐）

**读流程：**
```
读缓存 -> 命中则返回 -> 未命中则查 DB -> 写入缓存 -> 返回
```

**写流程：**
```
更新 DB -> 删除缓存（而非更新缓存）
```

**为什么是删除缓存不是更新缓存？**
1. 更新缓存可能涉及复杂计算（如聚合查询），成本高
2. 写频繁时，缓存被反复更新但可能不被读取，浪费资源
3. 并发写时，缓存和 DB 的更新顺序可能导致数据不一致

**延迟双删（强化版 Cache Aside）：**

```java
public void updateData(String key, Object newValue) {
    // 1. 第一次删除缓存
    redisTemplate.delete(key);

    // 2. 更新数据库
    database.update(newValue);

    // 3. 休眠一小段时间（如 500ms）
    Thread.sleep(500);

    // 4. 第二次删除缓存（处理期间可能被其他线程写入的旧数据）
    redisTemplate.delete(key);
}
```

**注意：** 延迟双删是 **最终一致性** 方案，不是强一致性。

#### 方案二：订阅 binlog + MQ 异步更新（推荐）

**架构：**

```
应用程序 -> 更新 DB
                   -> MySQL binlog
                         -> Canal（解析 binlog）
                              -> MQ（如 RocketMQ / Kafka）
                                   -> 消费端删除/更新缓存
```

**优势：**
- 与业务代码 **解耦**，不需要在业务代码中手动写缓存删除逻辑
- 保证 **最终一致性**
- 即使缓存删除失败，MQ 重试机制保证最终执行

```java
// Canal 消费端代码示例
@Component
public class BinlogCacheConsumer {

    @RabbitListener(queues = "cache.sync.queue")
    public void handleCacheSync(BinlogMessage msg) {
        String tableName = msg.getTableName();
        String eventType = msg.getEventType(); // INSERT/UPDATE/DELETE
        Long id = msg.getPrimaryKey();

        if ("UPDATE".equals(eventType) || "DELETE".equals(eventType)) {
            String cacheKey = tableName + ":" + id;
            redisTemplate.delete(cacheKey);
            log.info("缓存删除: {}", cacheKey);
        }
    }
}
```

#### 方案三：读写锁 + 分布式锁

读多写少的场景，读操作共享锁，写操作互斥锁：

```java
// 写操作：获取写锁 -> 更新 DB -> 删除缓存 -> 释放写锁
// 读操作：获取读锁 -> 读缓存/DB -> 释放读锁

// 但 Redisson 的读写锁支持分布式场景
RLock readLock = redissonClient.getReadWriteLock("lock:" + key).readLock();
RLock writeLock = redissonClient.getReadWriteLock("lock:" + key).writeLock();
```

---

### 5.5 缓存淘汰策略

当 Redis 内存达到 `maxmemory` 限制时，触发淘汰策略。

#### 8 种淘汰策略

| 策略 | 说明 | 适用场景 |
|------|------|----------|
| **noeviction** | 不淘汰，直接返回错误（OOM） | 默认 |
| **volatile-lru** | 在设置了 TTL 的 key 中，淘汰最近最少使用 | 常用 |
| **allkeys-lru** | 在所有 key 中，淘汰最近最少使用 | 通用推荐 |
| **volatile-lfu** | 在设置了 TTL 的 key 中，淘汰最不经常使用（4.0+） | 访问频率差异大 |
| **allkeys-lfu** | 在所有 key 中，淘汰最不经常使用（4.0+） | 同上 |
| **volatile-random** | 在设置了 TTL 的 key 中，随机淘汰 | 较少使用 |
| **allkeys-random** | 在所有 key 中，随机淘汰 | 较少使用 |
| **volatile-ttl** | 在设置了 TTL 的 key 中，淘汰 TTL 最小的 | 较少使用 |

#### LRU vs LFU

- **LRU（Least Recently Used）：** 基于最近访问时间，淘汰最长时间未被访问的
- **LFU（Least Frequently Used，4.0+）：** 基于访问频率，淘汰访问次数最少的
- **LFU 的改进：** 解决 LRU 的"偶发大量访问"问题（如秒杀商品被大量访问后可能一直占据缓存）

#### 配置

```bash
# redis.conf
maxmemory 1gb
maxmemory-policy allkeys-lru
maxmemory-samples 5  # LRU/LFU 采样数量（近似算法，提高性能）
```

---

### 5.6 大 Key / 热 Key

#### 大 Key 问题

**定义：** 单个 key 的 value 过大（通常 > 10KB），或集合类型元素过多（如 List 有几十万元素）。

**危害：**
- 操作耗时增加，阻塞其他请求（Redis 是单线程模型）
- 网络传输带宽压力大
- 删除大 key 时阻塞主进程

**检测方法：**

```bash
# 1. 查看 key 占用内存
MEMORY USAGE key

# 2. Redis-cli 扫描大 key
redis-cli --bigkeys

# 3. 手动扫描
SCAN 0 COUNT 100
```

**处理方案：**

```bash
# 1. 拆分大 key
# 例如大 Hash 拆分为多个小 Hash
HSET big:hash:part1 field1 value1
HSET big:hash:part2 field2 value2

# 2. 异步删除（Redis 4.0+）
UNLINK big_key   # 在后台线程释放内存，不阻塞主线程
# 替代 DEL（同步删除）

# 3. 压缩存储
# 使用压缩算法（如 Snappy）压缩 value 后存储
```

#### 热 Key 问题

**定义：** 某个 key 被 **极高频率** 访问，导致单个 Redis 节点 CPU 过载。

**检测方法：**

```bash
# 1. Redis-cli 监控
redis-cli --hotkeys

# 2. 使用 MONITOR 命令（慎用，会降低性能）
MONITOR

# 3. 客户端统计
# 在 Jedis 或 Lettuce 中嵌入热 key 检测
```

**处理方案：**

```bash
# 1. 本地缓存 + Redis 多级缓存
# 热点数据在 JVM 中缓存一份，减少 Redis 压力

# 2. 读写分离
# 热 key 复制到多个 Slave，读请求分散到不同 Slave

# 3. 拆分为多个子 key
# 例如 hot_key 拆分为 hot_key:1 ~ hot_key:N
# 客户端根据 hash 随机读取其中一个子 key
# 将访问压力分散到多个 Redis 节点
```

---

## 六、Spring Boot 集成

### 6.1 Spring Data Redis

#### Maven 依赖

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<!-- 连接池 -->
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-pool2</artifactId>
</dependency>
```

#### 配置

```yaml
# application.yml
spring:
  redis:
    host: 192.168.1.100
    port: 6379
    password: 123456
    database: 0
    timeout: 3000ms
    lettuce:
      pool:
        max-active: 16
        max-idle: 8
        min-idle: 4
        max-wait: 200ms
```

#### RedisTemplate 配置

```java
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // JSON 序列化
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer =
                new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper om = new ObjectMapper();
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        om.activateDefaultTyping(LazyValidatorFactory.class, ObjectMapper.DefaultTyping.NON_FINAL);
        jackson2JsonRedisSerializer.setObjectMapper(om);

        // String 序列化
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();

        // key 和 hash key 使用 String 序列化
        template.setKeySerializer(stringRedisSerializer);
        template.setHashKeySerializer(stringRedisSerializer);
        // value 和 hash value 使用 JSON 序列化
        template.setValueSerializer(jackson2JsonRedisSerializer);
        template.setHashValueSerializer(jackson2JsonRedisSerializer);

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory factory) {
        return new StringRedisTemplate(factory);
    }
}
```

#### 使用示例

```java
@Service
public class RedisCacheService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    // 缓存对象
    public void cacheUser(User user) {
        redisTemplate.opsForValue()
            .set("user:" + user.getId(), user, 1, TimeUnit.HOURS);
    }

    public User getUser(Long id) {
        return (User) redisTemplate.opsForValue().get("user:" + id);
    }

    // Hash 操作
    public void hashOps() {
        HashOperations<String, String, Object> hashOps = redisTemplate.opsForHash();
        hashOps.put("cart:1001", "product:201", 2);
        Integer count = (Integer) hashOps.get("cart:1001", "product:201");
        Map<String, Object> entries = hashOps.entries("cart:1001");
    }

    // List 操作
    public void listOps() {
        ListOperations<String, Object> listOps = redisTemplate.opsForList();
        listOps.leftPush("timeline:1001", "post:9527");
        listOps.trim("timeline:1001", 0, 49);
        List<Object> list = listOps.range("timeline:1001", 0, -1);
    }

    // Set 操作
    public void setOps() {
        SetOperations<String, Object> setOps = redisTemplate.opsForSet();
        setOps.add("user:1001:follow", "user:2001", "user:2002");
        Set<Object> intersect = setOps.intersect("user:1001:follow", "user:1002:follow");
    }

    // ZSet 操作
    public void zSetOps() {
        ZSetOperations<String, Object> zSetOps = redisTemplate.opsForZSet();
        zSetOps.add("leaderboard", "user:1001", 100);
        Set<ZSetOperations.TypedTuple<Object>> top3 =
            zSetOps.reverseRangeWithScores("leaderboard", 0, 2);
    }

    // 分布式锁（SET NX EX）
    public boolean tryLock(String key, String value, long expireSeconds) {
        return Boolean.TRUE.equals(
            redisTemplate.opsForValue().setIfAbsent(key, value, expireSeconds, TimeUnit.SECONDS)
        );
    }

    public void unlock(String key, String value) {
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                       "return redis.call('del', KEYS[1]) else return 0 end";
        redisTemplate.execute(new DefaultRedisScript<>(script, Long.class),
                Collections.singletonList(key), value);
    }
}
```

#### 注解式缓存

```java
@CacheConfig(cacheNames = "users")
@Service
public class UserService {

    @Cacheable(key = "#id", unless = "#result == null")
    public User getUserById(Long id) {
        // 从数据库查询
        return userMapper.selectById(id);
    }

    @CachePut(key = "#user.id")
    public User updateUser(User user) {
        userMapper.updateById(user);
        return user;
    }

    @CacheEvict(key = "#id")
    public void deleteUser(Long id) {
        userMapper.deleteById(id);
    }

    // 使用 Caching 组合多个注解
    @Caching(
        put = { @CachePut(key = "#user.id") },
        evict = { @CacheEvict(key = "'user_list'") }
    )
    public User saveUser(User user) {
        userMapper.insert(user);
        return user;
    }
}
```

---

### 6.2 Redisson

Redisson 是 Redis 官方推荐的 Java 客户端，提供了丰富的分布式数据结构和服务，最著名的就是 **可重入分布式锁** 和 **看门狗（Watchdog）** 机制。

#### Maven 依赖

```xml
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson-spring-boot-starter</artifactId>
    <version>3.24.3</version>
</dependency>
```

#### 配置

```yaml
# application.yml
spring:
  redis:
    redisson:
      config: |
        singleServerConfig:
          address: "redis://192.168.1.100:6379"
          password: "123456"
          connectionPoolSize: 16
          connectionMinimumIdleSize: 4
        codec: !<org.redisson.codec.JsonJacksonCodec> {}
```

或 Java 配置：

```java
@Configuration
public class RedissonConfig {

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer()
            .setAddress("redis://192.168.1.100:6379")
            .setPassword("123456")
            .setConnectionPoolSize(16)
            .setConnectionMinimumIdleSize(4);
        config.setCodec(new JsonJacksonCodec());
        return Redisson.create(config);
    }
}
```

#### 分布式锁（看门狗自动续期）

```java
@Service
public class OrderService {

    @Autowired
    private RedissonClient redissonClient;

    public void createOrder(Long orderId) {
        RLock lock = redissonClient.getLock("lock:order:" + orderId);

        try {
            // 尝试加锁，最多等待 10 秒，锁 30 秒自动释放
            boolean locked = lock.tryLock(10, 30, TimeUnit.SECONDS);
            if (!locked) {
                throw new RuntimeException("获取锁失败");
            }

            // 执行业务逻辑
            // 看门狗（Watchdog）每 10 秒自动续期（默认 leaseTime = -1 时启用）
            // 如果服务宕机，无法续期，锁自动释放（解决死锁问题）
            processOrder(orderId);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            // 释放锁（只有持有锁的线程才能释放）
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    // 简化版：使用注解
    @RLock(name = "lock:order:#orderId", waitTime = 10, leaseTime = 30)
    public void createOrderWithAnnotation(Long orderId) {
        processOrder(orderId);
    }
}
```

**看门狗（Watchdog）原理：**

1. 当 `leaseTime = -1`（默认）时，Redisson 启用看门狗
2. 加锁成功时，锁的过期时间设为 30 秒
3. 每隔 10 秒（leaseTime / 3），检查锁是否还被当前线程持有
4. 如果持有，则续期 30 秒
5. 如果线程崩溃或服务宕机，看门狗不再续期，锁自动释放

**这完美解决了两个问题：**
- 业务执行时间超过锁过期时间 -> 自动续期，不会提前释放
- 服务宕机导致锁永远不释放 -> 不再续期，锁自动过期

#### 分布式集合

```java
@Service
public class RedissonCollectionService {

    @Autowired
    private RedissonClient redissonClient;

    // 分布式 Map
    public void distributedMap() {
        RMap<String, Object> map = redissonClient.getMap("user:1001");
        map.put("name", "张三");
        map.put("age", 25);
        Object name = map.get("name");

        // 支持本地缓存加速（RLocalCachedMap）
        RLocalCachedMap<String, Object> cachedMap = redissonClient.getLocalCachedMap("config",
            LocalCachedMapOptions.defaults());
    }

    // 分布式 Set
    public void distributedSet() {
        RSet<String> set = redissonClient.getSet("user:1001:follow");
        set.add("user:2001");
        set.add("user:2002");
        boolean contains = set.contains("user:2001");
    }

    // 分布式 List
    public void distributedList() {
        RList<String> list = redissonClient.getList("timeline:1001");
        list.add("post:9527");
        list.trim(0, 49);
    }

    // 分布式原子计数器
    public void distributedCounter() {
        RAtomicLong counter = redissonClient.getAtomicLong("order:id");
        long id = counter.incrementAndGet();  // 分布式 ID
    }

    // 分布式队列（支持阻塞）
    public void distributedQueue() throws InterruptedException {
        RBlockingQueue<String> queue = redissonClient.getBlockingQueue("queue:task");
        // 生产者
        queue.put("task:001");
        // 消费者（阻塞等待）
        String task = queue.take();

        // 延时队列
        RDelayedQueue<String> delayedQueue = redissonClient.getDelayedQueue(queue);
        delayedQueue.offer("task:002", 5, TimeUnit.SECONDS);  // 5 秒后进入队列
    }

    // 布隆过滤器
    public void bloomFilter() {
        RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter("bloom:users");
        bloomFilter.tryInit(100000, 0.01);  // 预期容量 10 万，误判率 1%
        bloomFilter.add("user:1001");
        boolean exists = bloomFilter.contains("user:9999");  // false
    }
}
```

---

## 七、Redis 面试核心问题

### 1. 基础数据结构

**Q1: String 的底层 SDS 相比 C 字符串有哪些优势？**
- O(1) 获取长度（len 字段）
- 二进制安全（不依赖 `\0` 判断结束）
- 空间预分配和惰性释放，减少内存重分配
- 自动防止缓冲区溢出

**Q2: 说说 Redis 的 ziplist 和链式更新问题？**
- ziplist 是压缩的双向链表，内存连续，每个 entry 保存前一个 entry 的长度
- 当插入/删除元素导致前一个 entry 长度变化，可能触发后续 entry 的连锁更新
- 极端情况下性能 O(n^2)，Redis 7.0 用 listpack 替代 ziplist，彻底解决此问题

**Q3: ZSet 底层为什么用跳表而不用红黑树？**
- 跳表更易实现和调试
- 范围查询：跳表在底层链表遍历即可，红黑树需要中序遍历且实现更复杂
- 跳表支持平均 O(logN) 的查找、插入、删除
- 内存上，跳表可以通过调整概率参数灵活控制

**Q4: quicklist 的设计优势是什么？**
- 将大的 ziplist 拆分成多个小 ziplist，降低连锁更新的影响范围
- 在内存利用率和操作性能之间取得平衡
- 支持头部和尾部的快速插入/删除（O(1)）

**Q5: Redis 的 INCR 命令是原子的吗？为什么？**
- 是原子的。Redis 是单线程模型，一个命令的执行不会被其他命令打断
- 即使多个客户端同时执行 INCR，也会串行执行

### 2. 高级数据结构

**Q6: HyperLogLog 的原理？为什么 12KB 能存储 2^64 个基数？**
- HyperLogLog 利用概率算法：通过 hash 函数计算元素，统计二进制表示中前导零的最大位数
- 前导零位数越多，意味着数据集越大
- 即使统计 2^64 个元素，单个桶的前导零位数不超过 64，需要 6 bit 存储
- 16384 个桶 × 6 bit = 12KB

**Q7: 布隆过滤器支持删除吗？怎么实现支持删除的布隆过滤器？**
- 标准布隆过滤器不支持删除，因为多个元素可能映射到同一位
- 可以用 **Counting Bloom Filter**：将 bit 位改为计数器（通常 4 bit），添加时 +1，删除时 -1
- 或使用 **Cuckoo Filter**（布谷鸟过滤器），支持删除且空间效率更高

**Q8: GeoHash 编码原理和精度？**
- 将经纬度二分编码，交替取经度/纬度的二进制位，合并后 Base32 编码
- 编码越长越精确（约 1 字符 ≈ 5 bit）
- 编码前缀匹配表示空间邻近，但存在边界跳跃问题（两个相邻但在编码边界的分界处）

### 3. 持久化

**Q9: RDB 和 AOF 的优缺点对比？如何选择？**
- RDB：恢复快、文件小、适合备份；但可能丢数据、fork 时有性能开销
- AOF（everysec）：最多丢 1 秒数据、文件可读；但文件大、恢复慢
- 推荐：混合持久化（Redis 4.0+），兼顾恢复速度和数据安全

**Q10: BGSAVE 的写时复制（Copy-on-Write）原理？**
- fork 子进程时，父子进程共享内存页
- 主进程修改某页时，复制该页到新位置再修改（写时复制）
- 子进程持有的是一致性的内存快照

**Q11: AOF 重写的过程？**
- fork 子进程读取当前内存数据，生成最小写命令集合
- 子进程写入临时文件，主进程写命令同时写入重写缓冲区
- 子进程完成通知主进程，主进程将重写缓冲区内容追加到临时文件
- 用临时文件替换旧 AOF 文件

### 4. 高可用

**Q12: Redis 哨兵（Sentinel）的故障转移过程？**
- SDOWN -> ODOM -> Leader 选举（Raft） -> 选新 Master -> 通知其余 Slave 和客户端

**Q13: Redis Cluster 为什么是 16384 个槽？**
- CRC16 输出 16 位，16384 = 2^14，是平衡后选择的值
- 心跳包用位图传递槽信息，16384 个槽 = 2KB，而 65535 需要 8KB
- 一般集群节点不超 1000 个，16384 足够

**Q14: Redis Cluster 中 MOVED 和 ASK 的区别？**
- MOVED：槽的归属权已变更，客户端应更新路由表，下次直接访问新节点
- ASK：槽正在迁移中，数据可能还在源节点，客户端需发 ASKING 命令后再操作

**Q15: 一致性哈希的虚拟节点解决了什么问题？**
- 解决数据分布不均匀（数据倾斜）问题
- 节点增减时，数据在新旧节点间平滑迁移
- 避免一个节点下线导致所有数据压到下一个节点

### 5. 缓存实战

**Q16: 缓存穿透、击穿、雪崩的区别和解决方案？**

| 问题 | 原因 | 解决方案 |
|------|------|----------|
| 穿透 | 查询不存在的数据 | 布隆过滤器 + 缓存空值 |
| 击穿 | 热点 key 过期 | 互斥锁 + 逻辑过期 |
| 雪崩 | 大量 key 同时过期 / Redis 宕机 | TTL 随机化 + 多级缓存 + 限流 |

**Q17: 如何保证缓存和数据库的一致性？**
- 推荐：Cache Aside（先更新 DB 再删缓存）
- 强化：延迟双删
- 企业级：Canal 订阅 binlog + MQ 异步更新
- 注意：强一致性几乎不可能，大多数场景接受最终一致性

**Q18: Redis 大 key 有哪些危害？如何定位和处理？**
- 危害：阻塞操作、网络带宽、删除阻塞
- 定位：`--bigkeys` 参数、`MEMORY USAGE` 命令
- 处理：拆分、异步删除（`UNLINK`）、压缩

**Q19: Redisson 看门狗（Watchdog）机制？**
- 加锁时默认 leaseTime = -1，启用看门狗
- 默认锁过期 30 秒，每隔 10 秒续期一次
- 业务执行长时可自动续期，服务崩溃时不再续期自动释放
- 解决锁超时和死锁的矛盾问题

**Q20: Redis 单线程为什么这么快？**
- **纯内存操作：** 数据在内存中，读写时延约 100ns
- **单线程避免竞争：** 无锁上下文切换和同步开销
- **I/O 多路复用：** 使用 epoll（Linux）处理大量连接
- **高效的数据结构：** SDS、跳表、ziplist 等精心设计的数据结构
- **注意：** Redis 6.0+ 在网络 I/O 使用了多线程，但命令执行仍然是单线程

---

## 总结

Redis 的成功不仅仅因为它是一个高速缓存中间件，更在于它对 **数据结构的深入思考** 和 **场景的精准抽象**。SDS 解决 C 字符串的缺陷、跳表替代平衡树的高明选择、ziplist/listpack 对内存的极致优化、Stream 对消息队列功能的完善——这些都展现了 Redis 在工程实践上的精湛技艺。

在生产环境中，理解数据结构的底层原理能帮助我们做出正确的选型；掌握持久化和高可用机制能保障系统的稳定性；而深刻理解缓存穿透、击穿、雪崩等经典问题及其解决方案，则是每位后端工程师的必修课。

从单机到集群，从缓存到消息队列，从业务开发到架构设计，Redis 始终是 Java 后端生态中不可或缺的一环。希望本文能帮助你在面试和实战中从容应对 Redis 相关的挑战。
