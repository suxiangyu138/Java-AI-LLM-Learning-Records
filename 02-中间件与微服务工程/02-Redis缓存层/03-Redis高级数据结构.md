# Redis 高级数据结构
> Bitmap、HyperLogLog、GEO、Stream、Bloom Filter 解决特定领域问题，每个都通过极低内存实现强大功能。

## 目录
1. [Bitmap 位图](#1-bitmap-位图)
2. [HyperLogLog 基数统计](#2-hyperloglog-基数统计)
3. [GEO 地理空间](#3-geo-地理空间)
4. [Stream 消息流](#4-stream-消息流)
5. [Bloom Filter 布隆过滤器](#5-bloom-filter-布隆过滤器)
6. [高级数据结构对比](#6-高级数据结构对比)

---

## 1. Bitmap 位图

Bitmap 不是独立的数据类型，而是基于 String 的二进制位操作。每个 bit 表示一个二元状态（0/1），通过极低内存实现大规模状态标记。

### 1.1 原理

```text
String 底层是字节数组，Bitmap 将每个字节的 8 个 bit 独立操作。

一个字节 8 个 bit:
┌────┬────┬────┬────┬────┬────┬────┬────┐
│b0  │b1  │b2  │b3  │b4  │b5  │b6  │b7  │
│bit7│bit6│bit5│bit4│bit3│bit2│bit1│bit0│
└────┴────┴────┴────┴────┴────┴────┴────┘

1 亿个用户 → 100,000,000 bit ÷ 8 = 12,500,000 字节 ≈ 12.5 MB
```

| 数据规模 | 占用内存 |
|----------|----------|
| 100 万用户 | ~125 KB |
| 1000 万用户 | ~1.25 MB |
| 1 亿用户 | ~12.5 MB |
| 10 亿用户 | ~125 MB |

### 1.2 核心命令

| 命令 | 语法 | 说明 | 时间复杂度 |
|------|------|------|-----------|
| SETBIT | `SETBIT key offset value` | 设置指定位为 0 或 1 | O(1) |
| GETBIT | `GETBIT key offset` | 获取指定位的值 | O(1) |
| BITCOUNT | `BITCOUNT key [start end]` | 统计 1 的数量 | O(n) |
| BITPOS | `BITPOS key bit [start end]` | 查找第一个 0 或 1 的位置 | O(n) |
| BITOP | `BITOP operation destkey key [key ...]` | 位运算 AND/OR/XOR/NOT | O(n) |
| BITFIELD | `BITFIELD key [GET type offset] [SET type offset value] [INCRBY type offset increment]` | 批量位操作 | O(n) |

### 1.3 实战场景

**用户签到**

```bash
# 用户 1001 在 2026 年 1 月的签到记录
# offset = 日 - 1（1 日 offset=0，2 日 offset=1）

# 1 月 1 日签到
SETBIT sign:user:1001:202601 0 1

# 1 月 2 日签到
SETBIT sign:user:1001:202601 1 1

# 1 月 3 日未签到
SETBIT sign:user:1001:202601 2 0

# 查询 1 月 2 日是否签到
GETBIT sign:user:1001:202601 1   # → 1

# 当月签到天数
BITCOUNT sign:user:1001:202601   # → 2
```

```java
// 用户签到
public boolean signIn(String userId, LocalDate date) {
    String key = "sign:" + userId + ":" + date.format(DateTimeFormatter.ofPattern("yyyyMM"));
    int offset = date.getDayOfMonth() - 1;
    return Boolean.TRUE.equals(
        redisTemplate.opsForValue().setBit(key, offset, true));
}

// 当月签到天数
public Long getSignCount(String userId, YearMonth yearMonth) {
    String key = "sign:" + userId + ":" + yearMonth.format(DateTimeFormatter.ofPattern("yyyyMM"));
    return redisTemplate.execute(
        (RedisCallback<Long>) conn -> conn.bitCount(key.getBytes()));
}
```

**DAU 日活统计**

```bash
# 用户 ID 作为 offset
SETBIT dau:20260101 1001 1
SETBIT dau:20260101 1002 1
BITCOUNT dau:20260101   # → 日活

# 周活跃（连续 7 天 OR 运算）
BITOP OR dau:week1 dau:20260101 dau:20260102 dau:20260103
BITCOUNT dau:week1
```

> 💡 Bitmap 的 BITOP 支持 AND/OR/XOR/NOT 四种运算，可用于复杂的留存分析、用户画像场景。BITFIELD 支持批量设置和原子递增，适用于更复杂的位级操作。

### 1.4 BITFIELD 进阶用法

```bash
# BITFIELD 批量操作：SET + INCRBY + GET
BITFIELD mybitset SET u8 0 255 INCRBY u8 0 1 GET u8 0

# 溢出控制（SAT 饱和模式 / FAIL 报错模式）
BITFIELD mybitset OVERFLOW SAT INCRBY u8 0 1
```

### 1.5 适用场景

| 场景 | 优势 | 示例 |
|------|------|------|
| 用户签到 | 一年仅需 365 bit ≈ 46 字节/用户 | 签到日历 |
| DAU / MAU | 日活/月活统计，亿级用户仅需几十 MB | 运营报表 |
| 在线状态 | 实时跟踪用户在线/离线 | IM 系统 |
| 布隆过滤器简化版 | 简单存在性判断 | 黑名单 |
| 权限控制 | 每个 bit 代表一个权限 | RBAC 位图 |
| 布尔状态存储 | 多个开关/标志位 | 用户设置 |

> ⚠️ Bitmap 适合稀疏场景（大多数为 0，偶发为 1）。如果几乎所有位都会设置为 1，直接用 Set 会更合适。此外 Bitmap 的 offset 不能重复使用同一个 key 存不同业务的数据。

---

## 2. HyperLogLog 基数统计

超低内存实现海量数据去重计数。用于统计 UV、独立访客等只需计数不需要回查元素的场景。

### 2.1 核心特性

| 特性 | 值 |
|------|-----|
| 单 key 内存 | 固定 12 KB |
| 标准误差 | 0.81%（约 1/120） |
| 可统计基数 | 最大 2^64 |
| 是否可回查元素 | **不可以**，只能统计基数 |
| 是否可合并 | 可以，PFMERGE 合并多个 HLL |
| 是否可删除元素 | 不可以，只能 PFADD 添加 |

> 💡 12 KB 的固定内存意味着无论统计 1 万个 UV 还是 1 亿个 UV，都只占用 12 KB。这是 HyperLogLog 最核心的优势。

### 2.2 原理简述

HyperLogLog 基于伯努利过程的概率估算：

```text
核心思想：通过哈希值二进制前导零的最大长度来估算基数。

添加元素：
  元素 → 哈希函数 → 64 bit 二进制
                     ↓
              统计前导零个数 ρ
                     ↓
              更新寄存器：ρ 的最大值

统计基数 ≈ 2^(所有寄存器 ρ 平均值)

固定使用 16384 个寄存器（2^14），每个寄存器 6 bit
总内存 = 16384 × 6 = 98304 bit = 12288 字节 ≈ 12 KB
```

### 2.3 核心命令

```bash
# 添加元素
PFADD uv:20260101 user1 user2 user3
PFADD uv:20260101 user4 user5

# 统计基数
PFCOUNT uv:20260101   # → 5（近似）

# 合并多个 HLL（合并后基数 ≈ 多个集合的并集基数）
PFADD uv:0101 user1 user2
PFADD uv:0102 user2 user3
PFMERGE uv:week uv:0101 uv:0102
PFCOUNT uv:week   # → 3（user1, user2, user3）

# 批量添加
PFADD uv:20260101 user1 user2 user3 user4 user5
```

### 2.4 Java 实战

```java
// ===== 页面 UV 统计 =====
@Autowired
private StringRedisTemplate redisTemplate;

// 记录 UV
public void recordUV(String pageId, String userId) {
    redisTemplate.opsForHyperLogLog()
        .add("uv:" + pageId + ":" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")), userId);
}

// 获取 UV
public Long getUV(String pageId, LocalDate date) {
    return redisTemplate.opsForHyperLogLog()
        .size("uv:" + pageId + ":" + date.format(DateTimeFormatter.ofPattern("yyyyMMdd")));
}

// 合并周 UV
public Long getWeekUV(String pageId, LocalDate date) {
    String dest = "uv:" + pageId + ":week";
    String[] keys = java.util.stream.IntStream.range(0, 7)
        .mapToObj(i -> "uv:" + pageId + ":" + date.minusDays(i).format(DateTimeFormatter.ofPattern("yyyyMMdd")))
        .toArray(String[]::new);
    redisTemplate.opsForHyperLogLog().union(dest, keys);
    return redisTemplate.opsForHyperLogLog().size(dest);
}
```

### 2.5 HyperLogLog vs Set 统计 UV

| 对比维度 | Set | HyperLogLog |
|----------|-----|-------------|
| 内存（100 万 UV） | ~40 MB | 12 KB |
| 精确度 | 100% | ~99.19% |
| 是否能回查元素 | 可以 | 不能 |
| 是否能删除元素 | 可以 | 不能 |
| 命令复杂度 | O(1) 添加，O(n) 返回 | O(1) 添加，O(1) 返回 |
| 适合场景 | 小规模精确去重 | 海量 UV / PV 统计 |

> 💡 很多公司的 PV/UV 报表使用 HyperLogLog 统计，误差 0.81% 在业务可接受范围内。对精度要求高的场景（如涉及金额、精确计数的场景），请使用 Set 或数据库。

---

## 3. GEO 地理空间

基于 ZSet + GeoHash 编码实现地理位置存储和附近位置查询。

### 3.1 原理

```text
GEO 底层使用 ZSet 存储，score 为 GeoHash 编码后的 52 位整数。

存储流程：
  经度、纬度
      │
      ▼
  GeoHash 编码（52 bit 整数）
      │
      ▼
  ZADD key score member

查询流程（附近 5km）：
  目标经纬度 → GeoHash 编码 → score 范围 → ZRANGEBYSCORE → 结果过滤

GeoHash 特点：
  - 将二维经纬度编码为一维字符串
  - 前缀匹配实现邻近搜索
  - 编码越长精度越高
  - 存在边界问题：相邻的点可能编码相差很大（需额外处理）
```

| GeoHash 编码长度 | 精度（约） |
|-----------------|-----------|
| 1 字符 | 2500 km |
| 2 字符 | 630 km |
| 3 字符 | 78 km |
| 4 字符 | 20 km |
| 5 字符 | 2.4 km |
| 6 字符 | 610 m |
| 7 字符 | 76 m |
| 8 字符 | 19 m |
| 9 字符 | 2 m |
| 10 字符 | 0.2 m |

### 3.2 核心命令

```bash
# 添加地理位置（经度 纬度 成员）
GEOADD city 116.397128 39.916527 "天安门"
GEOADD city 121.473701 31.230416 "东方明珠"
GEOADD city 113.264385 23.129112 "广州塔"
GEOADD city 120.155070 30.274085 "西湖"

# 获取地理位置（经度、纬度）
GEOPOS city "天安门" "东方明珠"

# 两地距离
GEODIST city "天安门" "东方明珠" km    # → 1067.5 km

# GeoHash 字符串（可用于共享位置）
GEOHASH city "天安门"   # → wx4g0f6k3g0

# 附近位置搜索（基于指定经纬度）
GEORADIUS city 116.40 39.90 100 km WITHDIST WITHCOORD ASC COUNT 10

# 附近位置搜索（基于已有成员）
GEORADIUSBYMEMBER city "天安门" 50 km WITHDIST WITHCOORD ASC

# GEO 底层命令（可直接操作 ZSet）
ZRANGE city 0 -1            # 查看所有位置
ZREM city "天安门"           # 删除位置
```

### 3.3 Java 实战

```java
// ===== 附近门店推荐 =====
@Autowired
private RedisTemplate<String, Object> redisTemplate;

// 导入门店位置
public void addShop(String key, double lng, double lat, String shopId) {
    redisTemplate.opsForGeo().add(key, new Point(lng, lat), shopId);
}

// 查询附近门店
public List<Map<String, Object>> getNearbyShops(String key, double lng, double lat, double km, int limit) {
    Circle circle = new Circle(new Point(lng, lat), new Distance(km, RedisGeoCommands.DistanceUnit.KILOMETERS));
    RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
        .includeDistance().sortAscending().limit(limit);
    GeoResults<RedisGeoCommands.GeoLocation<Object>> results = redisTemplate.opsForGeo().radius(key, circle, args);

    return results.getContent().stream().map(r -> {
        Map<String, Object> m = new HashMap<>();
        m.put("shopId", r.getContent().getName());
        m.put("distance", r.getDistance().getValue());
        return m;
    }).collect(Collectors.toList());
}
```

> 💡 GEO 的 GEORADIUS 和 GEORADIUSBYMEMBER 是 Redis 3.2 引入的原生命令，底层通过 ZSet 的 score 范围查询实现。精度足够满足大多数 LBS 场景，但地理围栏等复杂场景建议使用专业的 GIS 数据库。

---

## 4. Stream 消息流

Redis 5.0 引入的持久化消息队列，支持消费者组和消息 ACK 确认。弥补了 List 消息队列功能不足的问题。

### 4.1 核心概念

| 概念 | 说明 |
|------|------|
| 消息（Message） | KV 对集合，每条消息有唯一 ID（格式：`时间戳-序号`） |
| 流（Stream） | 消息的有序链表，支持范围查询 |
| 消费者组（Consumer Group） | 多个消费者分摊消费同一 Stream |
| 消费者（Consumer） | 组内的具体消费实例 |
| 游标（last_delivered_id） | 消费者组已交付的最新消息 ID |
| PEL（Pending Entries List） | 已投递但未确认的消息列表 |
| ACK（XACK） | 消费者确认消息已处理完成 |

```text
Stream 架构：
┌────────────────────────────────────────┐
│              Stream                     │
│  ┌─────┐ ┌─────┐ ┌─────┐ ┌─────┐     │
│  │msg1 │→│msg2 │→│msg3 │→│msg4 │→...  │
│  └─────┘ └─────┘ └─────┘ └─────┘     │
│                    ↑                   │
│              last_delivered_id         │
└────────────────────────────────────────┘
             ↙         ↘
    ┌────────────┐  ┌────────────┐
    │ Group A     │  │ Group B     │
    │             │  │             │
    │ ┌────────┐ │  │ ┌────────┐ │
    │ │consumer1│ │  │ │consumer1│ │
    │ │consumer2│ │  │ │consumer2│ │
    │ └────────┘ │  │ └────────┘ │
    │ 各自独立消费 │  │ 各自独立消费 │
    └────────────┘  └────────────┘
```

### 4.2 Stream vs List 消息队列

| 特性 | List（LPUSH+BRPOP） | Stream |
|------|---------------------|--------|
| 消息可靠性 | 无 ACK，消费者崩溃消息丢失 | 支持 ACK + PEL，可靠投递 |
| 消费者组 | 不支持（多个消费者竞争消费）| 原生支持，消息组内分摊 |
| 消息回溯 | 不支持（消费后删除） | 支持从头重新消费 |
| 阻塞读取 | BRPOP 支持 | XREADGROUP 支持 |
| 多播 | 不支持 | 多消费者组独立消费 |
| 消息 ID | 无 | 有序自动递增 ID |
| 消息容量 | 受内存限制 | 同上，支持删除历史 |
| 复杂度 | 低 | 中等 |
| 推荐场景 | 简单异步任务 | 需要可靠消费的场景 |

### 4.3 核心命令

```bash
# 添加消息
XADD order:stream * orderId 1001 amount 99.9
XADD order:stream * orderId 1002 amount 199.0

# 添加消息使用自定义 ID
XADD order:stream 1700000000000-0 orderId 1003 amount 299.0

# 获取消息数量
XLEN order:stream

# 范围查询
XRANGE order:stream - +                    # 所有消息
XRANGE order:stream 1700000000000-0 +      # 指定 ID 开始
XREVRANGE order:stream + -                 # 反向查询
XREAD COUNT 2 STREAMS order:stream 0       # 从 ID=0 开始读 2 条

# 创建消费者组
XGROUP CREATE order:stream order-group $
# $ 表示创建后交付新消息，0 表示从历史消息开始

# 消费消息
XREADGROUP GROUP order-group consumer-1 COUNT 1 STREAMS order:stream >

# 确认消息
XACK order:stream order-group 1700000000000-0

# 查看待处理消息
XPENDING order:stream order-group

# 查看待处理消息详情
XPENDING order:stream order-group - + 10

# 转移消息（其他消费者认领超时未确认的消息）
XCLAIM order:stream order-group consumer-2 3600000 1700000000000-0

# 管理
XINFO STREAM order:stream     # 流信息
XINFO GROUPS order:stream     # 消费者组信息
XINFO CONSUMERS order:stream order-group  # 消费者信息

# 删除消息
XDEL order:stream 1700000000000-0

# 截断（删除旧消息）
XTRIM order:stream MAXLEN 1000   # 保留最新 1000 条
```

### 4.4 Java 实战

```java
// ===== 订单消息处理（Stream 生产者 + 消费者）=====
@Autowired
private RedisTemplate<String, Object> redisTemplate;

private static final String STREAM_KEY = "order:stream";
private static final String GROUP_NAME = "order-group";

@PostConstruct
public void init() {
    try { redisTemplate.opsForStream().createGroup(STREAM_KEY, GROUP_NAME); } 
    catch (Exception ignored) {}
}

// 生产者
public void sendOrder(String orderId, String amount) {
    redisTemplate.opsForStream().add(STREAM_KEY,
        Map.of("orderId", orderId, "amount", amount));
}

// 消费者（阻塞 2 秒，处理完后 ACK）
public void consumeOrder() {
    var records = redisTemplate.opsForStream().read(
        Consumer.from(GROUP_NAME, "consumer-1"),
        StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2)),
        StreamOffset.create(STREAM_KEY, ReadOffset.lastConsumed()));
    for (var record : records) {
        try {
            processOrder((String) record.getValue().get("orderId"));
            redisTemplate.opsForStream().acknowledge(STREAM_KEY, GROUP_NAME, record.getId());
        } catch (Exception e) {
            log.error("处理失败，消息保留在 PEL: {}", record.getId(), e);
        }
    }
}
```

### 4.5 使用建议

```text
Stream 适用场景：
├── 订单处理队列（需要 ACK）
├── 任务调度（需要回溯重试）
├── 日志收集（需要多消费者组）
├── 通知推送（多播场景）
└── 事件驱动架构（Event Sourcing）

何时不用 Stream：
├── 简单异步任务 → List
├── 大规模消息系统（百万 QPS） → Kafka / RocketMQ
├── 延时消息 → ZSet 更简单
└── 发布订阅 → PUB/SUB
```

> ⚠️ Redis Stream 适合中小规模的消息场景（QPS 万级）。如果你需要百万 QPS 的消息吞吐、消息持久化到磁盘、存储海量历史消息，请使用 Kafka / RabbitMQ / RocketMQ 等专业消息队列。

---

## 5. Bloom Filter 布隆过滤器

布隆过滤器判断"一定不存在"和"可能存在"，用于高效防止缓存穿透、URL 去重等场景。

### 5.1 原理

**核心思想**：使用一个很长的二进制位数组和多个哈希函数。

```text
添加元素 "redis"：
                       位数组
"redis" ─→ hash1() ─→ [0] set 1
        ─→ hash2() ─→ [3] set 1
        ─→ hash3() ─→ [6] set 1

查询元素 "redis"：
"redis" ─→ hash1() ─→ [0] = 1 ✓（继续）
        ─→ hash2() ─→ [3] = 1 ✓（继续）
        ─→ hash3() ─→ [6] = 1 ✓（可能存在）

查询元素 "mysql"（未添加）：
"mysql" ─→ hash1() ─→ [0] = 1 ✓（继续）
        ─→ hash2() ─→ [5] = 0 ✗（一定不存在！）

结论：
- 所有位为 1 → 可能存在（有误判率）
- 任一为 0 → 一定不存在
```

**误判率公式**：
```
误判率 p = (1 - e^(-k * n / m))^k
其中：
  m = 位数组长度
  n = 插入元素数量
  k = 哈希函数数量

已知 n 和要求 p，求 m 和 k：
  m = -n * ln(p) / (ln2)^2
  k = m/n * ln2
```

| 期望误判率 | 每元素位数 | 哈希函数数 | 100 万元素内存 |
|-----------|-----------|-----------|---------------|
| 10% | 4.8 bit | 3 | ~600 KB |
| 1% | 9.6 bit | 7 | ~1.2 MB |
| 0.1% | 14.4 bit | 10 | ~1.8 MB |
| 0.01% | 19.2 bit | 14 | ~2.4 MB |

### 5.2 核心命令（RedisBloom 模块）

Redis 标准版不带 Bloom Filter，需安装 RedisBloom 模块：

```bash
# Docker 安装带 RedisBloom 的 Redis
docker run -d --name redis-bloom -p 6379:6379 redislabs/rebloom:latest

# 或在 redis.conf 中加载模块
loadmodule /path/to/redisbloom.so
```

```bash
# 创建布隆过滤器
BF.RESERVE cache:filter 0.01 1000000
# 参数：key、误判率、容量

# 添加元素
BF.ADD cache:filter "user:1001"
BF.ADD cache:filter "user:1002"
BF.ADD cache:filter "user:1003"

# 判断存在
BF.EXISTS cache:filter "user:1001"   # → 1 (可能存在)
BF.EXISTS cache:filter "user:9999"   # → 0 (一定不存在)

# 批量添加
BF.MADD cache:filter "a" "b" "c"

# 批量判断
BF.MEXISTS cache:filter "a" "b" "d"

# 查看信息
BF.INFO cache:filter
```

### 5.3 Java 实战

```java
// ===== Redisson 布隆过滤器 + 缓存穿透防护 =====
@Service
public class UserService {
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private StringRedisTemplate redisTemplate;

    private RBloomFilter<String> bloomFilter;

    @PostConstruct
    public void init() {
        bloomFilter = redissonClient.getBloomFilter("user:bloom");
        bloomFilter.tryInit(1000000L, 0.01); // 容量 100 万，误判率 1%
        // 启动时将 DB 用户 ID 加载到布隆过滤器
        userDao.findAllIds().forEach(id -> bloomFilter.add("user:" + id));
    }

    public User getUserById(Long userId) {
        String key = "user:" + userId;
        // Step 1: 布隆过滤器 — 不存在直接返回，避免穿透
        if (!bloomFilter.contains(key)) return null;
        // Step 2: 查缓存
        String json = redisTemplate.opsForValue().get(key);
        if (json != null) return json.isEmpty() ? null : JSON.parseObject(json, User.class);
        // Step 3: 查数据库 + 回写缓存
        User user = userDao.findById(userId);
        if (user != null) {
            redisTemplate.opsForValue().set(key, JSON.toJSONString(user), 3600, TimeUnit.SECONDS);
        } else {
            redisTemplate.opsForValue().set(key, "", 60, TimeUnit.SECONDS); // 空值缓存
        }
        return user;
    }
}
```

### 5.4 适用场景

| 场景 | 说明 | 优势 |
|------|------|------|
| 缓存穿透防护 | 查缓存前先过 Bloom Filter | 拦截不存在 key，保护 DB |
| 爬虫 URL 去重 | 亿级 URL 判重 | 省内存，亿级仅需几百 MB |
| 推荐去重 | 已推荐内容过滤 | 快速判重 |
| 用户名/邮箱查重 | 快速判断是否已注册 | 避免查库 |
| 黑名单系统 | IP / 手机号黑名单 | 空间效率极高 |
| 垃圾邮件过滤 | 发件地址/内容哈希判重 | 配合其他方案降低误判 |

### 5.5 局限性

| 局限性 | 说明 | 解决方案 |
|--------|------|----------|
| 不支持删除 | 无法清空单个元素 | Counting Bloom Filter |
| 有误判率 | 可能把不存在判为存在 | 配合精确校验 |
| 不可扩展 | 容量固定后无法扩容 | 用多个 Bloom Filter 分片 |
| 无法遍历 | 不能获取已存储的元素 | 仅用于存在性判断 |

> ⚠️ 布隆过滤器说"不存在"一定正确，说"存在"可能误判。业务场景需容忍误判，或在误判后再查一次精确存储兜底。

---

## 6. 高级数据结构对比

| 类型 | 内存占用 | 误差 | 核心能力 | 典型场景 | 推荐与否 |
|------|----------|------|----------|----------|----------|
| Bitmap | 1 亿 bit / 12.5 MB | 无 | 位级操作 | 签到 / DAU / 在线状态 | 强烈推荐 |
| HyperLogLog | 固定 12 KB | 0.81% | 海量基数统计 | UV 统计 / PV 报表 | 强烈推荐 |
| GEO | 每个位置 ~64 bit | 米级精度 | 地理位置查询 | 附近的人 / 门店推荐 | 中等规模推荐 |
| Stream | 按消息量 | 无 | 可靠消息队列 | 订单队列 / 任务调度 | 中小规模推荐 |
| Bloom Filter | 100 万 ~1 MB | 可配置 | 存在性判断 | 缓存穿透 / URL 去重 | 强烈推荐 |

---

## 面试核心问题

**Q: HyperLogLog 原理？**
通过伯努利过程估算基数，固定 12 KB 内存可统计 2^64 基数，误差率 0.81%。使用 16384 个寄存器，每个 6 bit。

**Q: 布隆过滤器是否支持删除？**
标准 BF 不支持（无法确定位是否为多个元素共享）。需删除用 Counting Bloom Filter 或 Cuckoo Filter。

**Q: Stream vs Kafka？**
Stream 是 Redis 内置的轻量消息队列，适合中小规模（万级 QPS）；Kafka 是分布式消息平台，适合海量数据。

**Q: GEO 的底层实现？**
GEO 基于 ZSet + GeoHash 编码。GeoHash 将经纬度编码为 52 位整数作为 ZSet 的 score，范围查询实现附近搜索。

**Q: Bitmap 内存计算？**
总 bit / 8 / 1024 / 1024 = MB。1 亿用户约 12.5 MB，每个用户每天 1 bit。

> 🎯 高级数据结构的核心价值：用极低的内存成本解决特定的领域问题。Bitmap 解决状态标记、HyperLogLog 解决基数统计、Bloom Filter 解决存在性判断、GEO 解决位置查询、Stream 解决可靠消息。选型时优先考虑内存效率和业务匹配度。

