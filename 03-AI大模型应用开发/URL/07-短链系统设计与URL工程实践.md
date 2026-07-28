# 07 - 短链系统设计与 URL 工程实践

> 面试系统设计高频题——从发号器选型到 Base62 编码、从 301 vs 302 到布隆过滤器穿透、从跳转 SEO 到 UTM 埋点，一次讲透短链系统的完整设计链路

---

## 📚 目录

1. [短链系统核心需求](#1-短链系统核心需求)
2. [发号器方案对比](#2-发号器方案对比)
3. [短码生成策略](#3-短码生成策略)
4. [Base62 编码详解](#4-base62-编码详解)
5. [301 vs 302 跳转深度解析](#5-301-vs-302-跳转深度解析)
6. [缓存策略与布隆过滤器](#6-缓存策略与布隆过滤器)
7. [高可用与数据分片](#7-高可用与数据分片)
8. [SEO 与统计埋点](#8-seo-与统计埋点)
9. [系统容量估算](#9-系统容量估算)
10. [面试答辩要点](#10-面试答辩要点)

---

## 1. 短链系统核心需求

### 1.1 功能需求

```text
核心流程：
用户输入长 URL → 生成短链 → 用户分享 → 他人点击短链 → 302 跳转到长 URL

两个 API：
POST /api/shorten    { "longUrl": "https://..." }  →  { "shortUrl": "https://s.cn/abc123" }
GET  /{shortCode}    →  302 Location: https://original-long-url...
```

### 1.2 非功能需求

| 维度 | 要求 | 说明 |
|------|------|------|
| 低延迟 | 写入 < 10ms，跳转 < 5ms | 跳转是热路径，用户体验的最后一公里 |
| 高可用 | 99.99% | 短链挂了 = 所有分享链接全挂 |
| 高并发 | 写 10万 QPS，读 100万 QPS | 读 >> 写，热点事件可能瞬间爆发 |
| 短码长度 | 6-8 位 | 太短冲突率高，太长失去意义 |
| 不过期 | 默认永不过期 | 可选的过期时间 |
| 唯一性 | 同一长 URL 多次生成？ | 看业务：可同一短链（去重）/ 每次不同（统计独立） |

### 1.3 边界条件

```text
□ 长 URL 最长多长？        → 建议限制 2048 字符（浏览器兼容 + 数据库索引）
□ 短码字符集选什么？       → Base62（0-9a-zA-Z），避免 Base64 的 +/= 
□ 短码长度多少？           → 7 位 Base62 = 62^7 ≈ 3.5 万亿，够用
□ 长链改了内容怎么办？     → 短链不变，只指向新内容（类似 DNS）
□ 恶意链接怎么处理？       → 异步内容安全扫描 + 访问时实时检查
□ 已删除的短链怎么处理？   → 返回 410 Gone（不是 404）
□ 如何防刷？               → 令牌桶限流 + 验证码
```

---

## 2. 发号器方案对比

短链的核心技术挑战是**生成全局唯一的短 ID**。以下是四大方案。

### 2.1 方案对比

| 方案 | 原理 | 短码长度 | 性能 | 扩展性 | 依赖 |
|------|------|:---:|:---:|:---:|------|
| **Hash 算法** | `MD5/SHA1(url)` 取前 N 位 | 7-8 位 | ⭐⭐⭐ | ⭐⭐ 冲突需处理 | 无 |
| **UUID + Base62** | UUID → 62进制 | 过长（>20位） | ⭐⭐⭐ | ⭐⭐⭐ | 无 |
| **自增 ID + Base62** | MySQL/Redis 自增 ID | 随 ID 增长 | ⭐⭐ | ⭐ 单点瓶颈 | DB/Redis |
| **Snowflake + Base62** | 分布式 ID 生成 | 随 ID 增长 | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | 无 DB 依赖 ✅ |
| **随机字符串** | 随机 N 位字符 | 固定 7 位 | ⭐⭐⭐⭐ | ⭐⭐⭐ | 需判重 |

### 2.2 方案一：Hash 算法（简单但冲突率需处理）

```java
import java.security.MessageDigest;
import java.util.HexFormat;

public String hashShorten(String longUrl) {
    byte[] hash = MessageDigest.getInstance("SHA-256")
            .digest(longUrl.getBytes(StandardCharsets.UTF_8));
    String hex = HexFormat.of().formatHex(hash);

    // 取前 7 位（16^7 = 2.68亿），冲突率太高
    // 取前 12 位（16^12 = 2.8×10^14），太长
    // → Hash 的十六进制不适合直接当短码，需转 Base62

    // 取前 42 bit（7 位 Base62 刚好装下）
    long value = Long.parseLong(hex.substring(0, 10), 16) & 0x3FFFFFFFFFFL;
    return base62Encode(value);
}
```

| Hash 冲突解决 | 做法 |
|--------------|------|
| 发生冲突时 | 在长链尾部追加随机 salt → 重新 hash → 直到不冲突 |
| 布隆过滤器 | 先过布隆确认"可能存在冲突"，减少 DB 查询 |

> ⚠️ Hash 方案下**同一长 URL 每次生成相同短链**（去重效果好），但需要处理冲突重试。

### 2.3 方案二：Snowflake 雪花算法（推荐）

```java
/**
 * 简化版 Snowflake（适合短链场景）
 *
 * 64 位长整型 = 1 位保留 + 41 位时间戳 + 10 位机器 ID + 12 位序列号
 *
 *  0 - 0000000000 0000000000 0000000000 0000000000 0 - 0000000000 - 000000000000
 *  ↑   └───────────────── 41 bit 时间戳 ──────────────┘↑  └─10 bit─┘ └─12 bit─┘
 *  保留（始终0）       （毫秒，从自定义起点算）        保留  机器 ID    序列号
 */
public class SnowflakeIdGenerator {

    private static final long EPOCH = 1700000000000L;  // 自定义起始时间
    private static final long WORKER_ID_BITS = 10L;
    private static final long SEQUENCE_BITS = 12L;
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
    private static final long MAX_SEQUENCE  = ~(-1L << SEQUENCE_BITS);
    private static final long TIMESTAMP_SHIFT = WORKER_ID_BITS + SEQUENCE_BITS;
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;

    private final long workerId;
    private long sequence = 0L;
    private long lastTimestamp = -1L;

    public SnowflakeIdGenerator(long workerId) {
        if (workerId > MAX_WORKER_ID || workerId < 0) {
            throw new IllegalArgumentException("workerId out of range");
        }
        this.workerId = workerId;
    }

    public synchronized long nextId() {
        long timestamp = System.currentTimeMillis();
        if (timestamp < lastTimestamp) {
            throw new RuntimeException("Clock moved backwards");
        }
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0) {  // 当前毫秒序列号用完，等下一毫秒
                while (timestamp <= lastTimestamp) {
                    timestamp = System.currentTimeMillis();
                }
            }
        } else {
            sequence = 0L;
        }
        lastTimestamp = timestamp;
        return ((timestamp - EPOCH) << TIMESTAMP_SHIFT)
             | (workerId << WORKER_ID_SHIFT)
             | sequence;
    }
}

// 使用
SnowflakeIdGenerator gen = new SnowflakeIdGenerator(1);
long id = gen.nextId();                         // 比如 7261234567890123
String shortCode = Base62.encode(id);           // → "a3Bx9Kp"
```

### 2.4 Snowflake + Base62 编码长度分析

| 时间范围 | Snowflake ID 大致范围 | Base62 编码长度 |
|---------|---------------------|:---:|
| 第 1 天 | ~ 10^12 | 7 位 (`62^6 < 10^12 < 62^7`) |
| 第 1 年 | ~ 10^15 | 9 位 |
| 第 10 年 | ~ 10^16 | 9 位 |
| 第 69 年 | ~ 10^17 | 10 位 |

> 💡 **前几年用 7 位够，之后自然变 8 位。** 短码长度可以动态增长——比一开始就定 8 位更显专业（面试加分点）。

### 2.5 Redis 自增方案（小规模最简）

```java
@Component
public class RedisIdGenerator {

    private final StringRedisTemplate redis;

    public long nextId() {
        // INCR 原子自增，单机 10 万 QPS，集群更高
        return redis.opsForValue().increment("short_url:id");
    }

    // 多实例时预分配号段，减少 Redis 访问
    // 每次取 1000 个号，本地用完了再取
}
```

> 🎯 **方案选型结论**：并发 < 1 万 QPS 用 Redis 自增（最简单）；高并发用 **Snowflake**（无 DB 依赖）；同一 URL 去重用 **Hash + 布隆过滤器**。

---

## 3. 短码生成策略

### 3.1 两种生成模式

| 模式 | 做法 | 优点 | 缺点 |
|------|------|------|------|
| **Hash 模式** | `shortCode = hash(longUrl)` | 天然去重，同一 URL 同一短链 | 冲突需处理，短码信息量大 |
| **发号器模式** | `shortCode = encode(generator.nextId())` | 无冲突，性能好 | 同一 URL 每次生成不同短链（可选） |

### 3.2 短码字符集选择

| 字符集 | 字符数 | 7 位空间 | 可读性 | URL 安全 |
|--------|:---:|---------|:---:|:---:|
| 十六进制 `0-9a-f` | 16 | 2.68 亿 | ✅ | ✅ |
| Base36 `0-9a-z` | 36 | 783 亿 | ✅ | ✅ |
| **Base62 `0-9a-zA-Z`** | **62** | **3.5 万亿** | ✅ | ✅ |
| Base64 `0-9a-zA-Z+/` | 64 | 4.4 万亿 | ⚠️ `+`/`/` | ❌ URL 不安全 |
| Base64URL `0-9a-zA-Z-_` | 64 | 4.4 万亿 | ⚠️ `-`/`_` | ✅ |
| Base58（比特币）`123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz` | 58 | 2.2 万亿 | ✅ 去掉了 `0OIl` | ✅ |

> 🎯 **Base62 是最佳平衡点**：容量够大 + 无 URL 特殊字符 + 无肉眼混淆字符问题（相比 Base58 少了一步字符过滤，实现更简单）。

### 3.3 排除易混淆字符

```java
// 可选的字符集优化：去掉 0/O、1/I/l 等易混淆字符
private static final char[] CHARS = 
    "23456789ABCDEFGHJKMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz".toCharArray();
// 52 个字符，7 位也有 52^7 ≈ 1 万亿，足够
```

---

## 4. Base62 编码详解

### 4.1 算法实现

```java
public final class Base62 {

    private static final String ALPHABET =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int BASE = 62;
    private static final char[] CHARS = ALPHABET.toCharArray();
    // 查找表：每个 ASCII 字符 → 对应的值（-1 表示不在字符集中）
    private static final int[] INDEX = new int[128];
    static {
        Arrays.fill(INDEX, -1);
        for (int i = 0; i < CHARS.length; i++) {
            INDEX[CHARS[i]] = i;
        }
    }

    /** 数字 → Base62 字符串 */
    public static String encode(long num) {
        if (num == 0) return "0";
        StringBuilder sb = new StringBuilder();
        while (num > 0) {
            sb.append(CHARS[(int) (num % BASE)]);
            num /= BASE;
        }
        return sb.reverse().toString();   // 高位在前
    }

    /** Base62 字符串 → 数字 */
    public static long decode(String str) {
        long result = 0;
        for (char c : str.toCharArray()) {
            int val = INDEX[c];
            if (val == -1) throw new IllegalArgumentException("Invalid Base62 char: " + c);
            result = result * BASE + val;
        }
        return result;
    }
}

// 测试
Base62.encode(0L);             // "0"
Base62.encode(123456789L);     // "8m0Kx"
Base62.encode(Long.MAX_VALUE); // "aZl8N0y58M7"（11 位）
```

### 4.2 编码长度对照表

| 十进制范围 | Base62 长度 | 对应场景 |
|-----------|:---:|---------|
| 0 ~ 61 | 1 | — |
| 62 ~ 3,843 | 2 | — |
| 3,844 ~ 238,327 | 3 | — |
| 238,328 ~ 14,776,335 | 4 | — |
| 14,776,336 ~ 916,132,831 | 5 | 亿级 |
| 916,132,832 ~ 56,800,235,583 | 6 | 百亿级 |
| **56,800,235,584 ~ 3,521,614,606,207** | **7** | **万亿级 ← 短链主力区间** |
| 3,521,614,606,208 ~ 218,340,105,584,895 | 8 | 百万亿级 |

> 💡 Snowflake 一天产生约 10^12 量级的 ID，刚好落在 7 位 Base62 区间。**第一年就是 7 位短码，第 10 年以后自然过渡到 8 位。**

---

## 5. 301 vs 302 跳转深度解析

这是短链系统最重要的技术决策之一。

### 5.1 行为差异

| 维度 | 301 Moved Permanently | 302 Found (Temporary) |
|------|----------------------|----------------------|
| 语义 | 资源**永久**迁移 | **临时**跳转 |
| 浏览器行为 | **强缓存**，第二次直接跳过短链服务器 | 每次请求都打到短链服务器 |
| 后续请求 | ❌ **不到短链服务** | ✅ 每次都到短链服务 |
| 统计准确性 | ❌ 只有首次能统计 | ✅ 每次都能统计 |
| 性能 | ✅ 热链接无延迟 | ⚠️ 每次多一次重定向延迟 |
| 短链服务器 | ✅ 负载小 | ❌ 每次访问都打到服务器 |
| 修改目标 | ❌ 浏览器缓存后改不了 | ✅ 随时可改 |

### 5.2 决策：短链系统应该用哪个？

```text
场景分析：
          ┌─ 需要统计点击量/来源/地域？────► 必须 302
          │
用户点击 ─┤  需要后期修改跳转目标？────────► 必须 302
          │
          └─ 目标永不变、无需统计？─────────► 可以用 301
```

> 🎯 **商业短链（微信/微博/抖音/广告投放）一律用 302**。用 301 会让你失去点击量数据和广告归因。只有个人博客的迁移链接才用 301。

### 5.3 实现

```java
@GetMapping("/{shortCode}")
public ResponseEntity<Void> redirect(@PathVariable String shortCode,
                                      HttpServletRequest request) {
    String longUrl = shortUrlService.getLongUrl(shortCode);
    if (longUrl == null) {
        return ResponseEntity.notFound().build();
    }

    // 异步记录访问日志（不阻塞跳转）
    shortUrlService.recordAccessAsync(shortCode, buildAccessLog(request));

    return ResponseEntity.status(HttpStatus.FOUND)    // 302
            .location(URI.create(longUrl))
            .cacheControl(CacheControl.noCache())       // 禁止浏览器缓存
            .build();
}
```

### 5.4 Nginx 层面优化热链

```nginx
# 高频短链直接 Nginx 跳转，不穿透到 Java
# 方式一：共享内存字典
http {
    # Nginx js / lua 模块查询 Redis 或本地缓存，命中直接 return 302
}

# 方式二：Java 把热点短链推送到 Nginx 本地文件，定期 reload
# /etc/nginx/hot_short_links.conf:
# location = /abc123 { return 302 https://target-url; }
```

---

## 6. 缓存策略与布隆过滤器

### 6.1 缓存分层

```text
          ┌─────────────┐
          │ Nginx 本地   │  热点 Top 1000 短链
          │ 共享内存     │  < 1ms
          └──────┬──────┘
                 │ miss
          ┌──────▼──────┐
          │    Redis     │  全量短链缓存
          │  KV 存储     │  < 2ms，TTL 1h~24h
          └──────┬──────┘
                 │ miss
          ┌──────▼──────┐
          │    MySQL     │  持久化存储
          │  主键索引    │  < 10ms
          └─────────────┘
```

### 6.2 缓存穿透与布隆过滤器

**问题**：攻击者大量请求不存在的短链 → 全部穿透缓存打到 DB。

```java
@Component
public class ShortUrlCacheService {

    private final StringRedisTemplate redis;
    private final BloomFilter bloomFilter;       // Guava / Redisson
    private final ShortUrlRepository db;

    private static final String CACHE_PREFIX = "short:";
    private static final Duration CACHE_TTL = Duration.ofHours(24);
    private static final String NULL_PLACEHOLDER = "__NULL__";  // 缓存空值

    public String getLongUrl(String shortCode) {
        // ① 布隆过滤器：快速判断"一定不存在"
        if (!bloomFilter.mightContain(shortCode)) {
            return null;    // 100% 不存在，避免查 Redis / DB
        }

        // ② Redis 缓存
        String cached = redis.opsForValue().get(CACHE_PREFIX + shortCode);
        if (cached != null) {
            return NULL_PLACEHOLDER.equals(cached) ? null : cached;
        }

        // ③ DB 兜底
        String longUrl = db.findByShortCode(shortCode);
        if (longUrl != null) {
            redis.opsForValue().set(CACHE_PREFIX + shortCode, longUrl, CACHE_TTL);
            return longUrl;
        } else {
            // ④ 缓存空值，TTL 短一些，防止缓存穿透
            redis.opsForValue().set(CACHE_PREFIX + shortCode, NULL_PLACEHOLDER,
                    Duration.ofMinutes(5));
            return null;
        }
    }
}
```

### 6.3 布隆过滤器初始化与维护

```java
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;

@Component
public class ShortCodeBloomFilter {

    private BloomFilter<String> filter;
    private volatile BloomFilter<String> activeFilter;  // 双 buffer，不停机更新

    @PostConstruct
    public void init() {
        // 预估 10 亿个短码，误判率 0.01%
        filter = BloomFilter.create(
                Funnels.stringFunnel(StandardCharsets.UTF_8),
                1_000_000_000L,
                0.0001
        );
        activeFilter = filter;
        // 启动时从 DB 全量加载已有短码
        loadFromDb();
    }

    public void add(String shortCode) {
        activeFilter.put(shortCode);
    }

    public boolean mightContain(String shortCode) {
        return activeFilter.mightContain(shortCode);
    }

    // 定期全量重建（凌晨低峰），双 buffer 切换，不停机
    @Scheduled(cron = "0 0 3 * * ?")
    public void rebuild() {
        BloomFilter<String> newFilter = BloomFilter.create(
                Funnels.stringFunnel(StandardCharsets.UTF_8),
                1_200_000_000L,    // 留 20% 增长空间
                0.0001
        );
        loadFromDbInto(newFilter);
        activeFilter = newFilter;  // 原子切换
        filter = newFilter;
    }
}
```

### 6.4 缓存击穿（热点 Key 过期）

```java
// 热点短链在过期瞬间大量请求穿透到 DB
public String getLongUrlWithLock(String shortCode) {
    String cached = redis.opsForValue().get(CACHE_PREFIX + shortCode);
    if (cached != null) return NULL_PLACEHOLDER.equals(cached) ? null : cached;

    // 分布式锁：同一短链只让一个线程去查 DB
    String lockKey = "lock:short:" + shortCode;
    String lockVal = UUID.randomUUID().toString();
    try {
        if (redis.opsForValue().setIfAbsent(lockKey, lockVal, Duration.ofSeconds(5))) {
            // 双重检查
            cached = redis.opsForValue().get(CACHE_PREFIX + shortCode);
            if (cached != null) return NULL_PLACEHOLDER.equals(cached) ? null : cached;

            String longUrl = db.findByShortCode(shortCode);
            if (longUrl != null) {
                redis.opsForValue().set(CACHE_PREFIX + shortCode, longUrl, CACHE_TTL);
            } else {
                redis.opsForValue().set(CACHE_PREFIX + shortCode, NULL_PLACEHOLDER,
                        Duration.ofMinutes(5));
            }
            return longUrl;
        } else {
            Thread.sleep(50);  // 等持锁线程写入缓存
            return getLongUrl(shortCode);  // 重试读缓存
        }
    } finally {
        // Lua 脚本安全释放锁（判断 value 相同才删）
        String script = "if redis.call('get',KEYS[1])==ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end";
        redis.execute(new DefaultRedisScript<>(script, Long.class),
                List.of(lockKey), lockVal);
    }
}
```

---

## 7. 高可用与数据分片

### 7.1 数据库设计

```sql
CREATE TABLE short_url (
    id          BIGINT PRIMARY KEY,
    short_code  VARCHAR(10) NOT NULL UNIQUE,
    long_url    VARCHAR(2048) NOT NULL,
    -- 原始长 URL 的 hash，用于去重查询
    long_hash   CHAR(64) NOT NULL,
    creator     VARCHAR(64),
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at  DATETIME,
    status      TINYINT NOT NULL DEFAULT 1,   -- 1:正常 0:禁用
    INDEX idx_long_hash (long_hash),
    INDEX idx_expires_at (expires_at)
);

CREATE TABLE access_log (
    id          BIGINT PRIMARY KEY,
    short_code  VARCHAR(10) NOT NULL,
    access_time DATETIME NOT NULL,
    ip          VARCHAR(45),
    user_agent  VARCHAR(512),
    referer     VARCHAR(2048),
    country     VARCHAR(64),       -- IP 库解析
    INDEX idx_short_code_time (short_code, access_time)
    -- 按天分表：access_log_20260729
);
```

### 7.2 分片策略

| 策略 | 做法 | 优点 | 缺点 |
|------|------|------|------|
| **按 short_code 哈希** | `hash(short_code) % N` | 均匀分布 | 扩容需数据迁移 |
| **按 short_code 首字符** | `a-g → 库1, h-n → 库2...` | 简单直观 | 分布不均 |
| **一致性哈希** | 虚拟节点环 | 扩容只需迁移少部分数据 | 实现复杂 |
| **范围分片** | `id 1~10亿 → 库1` | 易扩容（加新库放新 ID） | 最新库可能热点 |

> 🎯 **推荐**：短链系统推荐**范围分片**——新数据自然写到新库，老库无需拆分。配合读写分离，读可从多个从库分担。

### 7.3 短链服务无状态化

```text
短链服务 = 纯计算层（无状态） + 缓存层（Redis） + 存储层（MySQL）

纯计算层可能成为瓶颈吗？
- 生成短链：Snowflake 是纯本地计算，无 IO
- 跳转：查 Redis（本地缓存热点）+ 极少查 DB

→ 服务层可以无限水平扩展
→ 瓶颈在 Redis（单机 10 万 QPS，集群更高）和 MySQL（分库分表）
```

---

## 8. SEO 与统计埋点

### 8.1 短链对 SEO 的影响

| 做法 | SEO 影响 | 说明 |
|------|:---:|------|
| 302 跳转 | ⚠️ 搜索引擎收录的是**短链域名** | 长链域名拿不到外链权重 |
| 301 跳转 | ✅ 权重传递给目标 URL | 但失去统计能力 |
| `<link rel="canonical">` | ✅ 告诉搜索引擎"规范 URL 是长链" | 仅网页内容页，JS 跳转无效 |

> 💡 **SEO 最佳实践**：自己网站的内容不要用短链分享。短链只用于社交媒体 / 短信 / 印刷品。内容页自己用 `<link rel="canonical" href="https://mysite.com/full-url">`。

### 8.2 UTM 参数自动追加

```java
@Component
public class UtmEnricher {

    /**
     * 自动给短链目标 URL 加上 UTM 追踪参数
     * 用于统一管理各渠道的广告归因
     */
    public String enrich(String longUrl, String source, String medium, String campaign) {
        URI uri = URI.create(longUrl);
        String query = uri.getRawQuery();

        // 用 UriComponentsBuilder 追加 UTM 参数
        UriComponentsBuilder builder = UriComponentsBuilder.fromUri(uri);
        if (source != null && !source.isEmpty())
            builder.queryParam("utm_source", source);
        if (medium != null && !medium.isEmpty())
            builder.queryParam("utm_medium", medium);
        if (campaign != null && !campaign.isEmpty())
            builder.queryParam("utm_campaign", campaign);

        return builder.build().encode().toUriString();
    }
}

// 生成短链时：
// 原始长链：https://mysite.com/promo/2026-summer
// 加 UTM 后：https://mysite.com/promo/2026-summer?utm_source=wechat&utm_medium=social&utm_campaign=summer2026
// 再生成短链 → 跳转到带 UTM 的长链
```

### 8.3 点击数据统计维度

```text
每个短链的统计面板：

📊 总点击量（PV）/ 独立访客（UV）
   ├── 按时间：24h 趋势图、过去 7/30 天
   ├── 按地域：国家 / 省份 / 城市
   ├── 按设备：桌面 / 移动 / 平板 / OS / 浏览器
   ├── 按来源：直接访问 / 微信 / 微博 / 搜索引擎
   └── 按时段：每小时热力图
```

```java
// 异步记录（MQ 解耦，不阻塞跳转响应）
@Async
public void recordAccess(String shortCode, AccessLog log) {
    // ① 发到 Kafka（解耦 + 削峰）
    kafkaTemplate.send("short-url-access", shortCode, toJson(log));

    // ② 实时计数器（Redis HyperLogLog 做 UV）
    redis.opsForHyperLogLog().add("uv:" + shortCode, log.getIp());

    // ③ 实时计数器（Redis INCR 做 PV）
    redis.opsForValue().increment("pv:" + shortCode + ":" + today());
}
```

---

## 9. 系统容量估算

### 9.1 假设条件

```text
假设做的是"微信生态通用短链服务"：
- 日生成短链：1000 万条
- 日访问量：10 亿次（平均每条 100 次点击）
- 存储 5 年
- 读写比例：1:100
```

### 9.2 各层资源估算

| 资源 | 计算 | 结果 |
|------|------|:---:|
| **总短链数** | 1000万 × 365 × 5 | 182.5 亿条 |
| **MySQL 存储** | 182.5亿 × 500B（行） | ~850 GB |
| **MySQL 分库数** | 单库建议 500GB 以内 | **2 个库** |
| **Redis 全量缓存** | 182.5亿 × 100B（short→long） | ~1.7 TB |
| **Redis 热数据** | 最近 30 天的短链（3亿 × 2KB） | ~60 GB |
| **Redis QPS（读）** | 10亿 / 86400 ≈ 11,574 QPS 平均 | 峰值 5万 QPS → 1 主 2 从够 |
| **带宽** | 10亿 × 500B / 86400 × 8 | ~46 Mbps 平均 |
| **访问日志** | 10亿/天 × 200B | 200 GB/天 → Kafka + ClickHouse |

### 9.3 面试估算套路

```text
1. 先和面试官确认假设（日活、数据量、保存时长）
2. 算存储（每行大小 × 总量 → 分库分表）
3. 算 QPS（总量 / 秒数 × 峰值系数 3~5）
4. 算带宽（QPS × 响应大小）
5. 分析瓶颈（通常是热点数据 → 多级缓存）
```

---

## 10. 面试答辩要点

### 10.1 必答问题

| # | 问题 | 要点 |
|:---:|------|------|
| 1 | 短码怎么生成？ | 发号器（Snowflake）+ Base62，并说清楚 Hash 方案的取舍 |
| 2 | 用 301 还是 302？ | 302（临时），为了统计点击量；但补充说明 301 适用场景 |
| 3 | 同一长链两次生成，返回相同还是不同的短链？ | 看业务需要；说出去重方案（Hash + 布隆过滤器）和非去重方案 |
| 4 | 如何防止缓存穿透？ | 布隆过滤器 + 缓存空值 + 分布式锁防击穿 |
| 5 | 短码用完了怎么办？ | 7 位 Base62 = 3.5 万亿，够用；也可动态扩展位数 |
| 6 | 怎么防刷（恶意生成大量短链）？ | 令牌桶限流（IP + 用户维度）+ 验证码 |
| 7 | 短链服务挂了会怎样？ | 所有分享出去的短链全挂 → 必须多机房/多云容灾 |
| 8 | 如何统计点击量？ | 异步 MQ + Redis 计数器 + ClickHouse 离线分析 |
| 9 | 恶意链接怎么处理？ | 生成时异步内容安全扫描 + 已发现恶意链接实时封禁返回 410 |
| 10 | Base62 vs Base64 怎么选？ | Base62 无须 URL 编码；Base64 有 `+/=` 不 URL 安全 |

### 10.2 加分亮点

```text
□ 短码长度可以动态增长（前 5 年 7 位，之后 8 位），而非一开始就定死
□ 提到 URL 归一化——不同形态的同一个长链应生成相同短链
□ 提到 Nginx 本地热点缓存，最高频的 1000 个短链不走 Java
□ 提到 Snowflake 时钟回拨处理（等一会或抛异常）
□ 提到访问日志异步记录 + MQ 解耦 + ClickHouse 分析
□ 提到安全：长链 SSRF 检测、恶意链接异步扫描
□ 提到灰度：新短链先在从库验证再切主库
```

---

> 🎯 **本篇核心要点**
> 1. 发号器选 Snowflake（无 DB 依赖），编码用 **Base62**（URL 安全 + 7 位够 3.5 万亿）
> 2. 跳转用 **302**（保留统计能力），配合 `Cache-Control: no-cache` 防止浏览器缓存
> 3. 缓存穿透三层防御：**布隆过滤器 → 缓存空值 → 分布式锁防击穿**
> 4. 访问日志异步处理（MQ + ClickHouse），不阻塞用户跳转
> 5. 同一个长链是否去重要**和面试官确认业务需求**，分别给出方案

---

**上一模块**：[06-URL安全与常见漏洞防护.md](06-URL安全与常见漏洞防护.md) / **下一模块**：[08-URL在AI大模型应用中的实战.md](08-URL在AI大模型应用中的实战.md)
