# 03 — Redis 实战 (Redis in Practice)

> **Core Reading**: This document covers Redis from fundamentals through enterprise production practices. It is designed for Java developers who need to use Redis as a cache, distributed lock, session store, and more.

---

## Table of Contents

1. [Redis Fundamentals](#redis-fundamentals)
2. [Data Structures Deep Dive](#data-structures-deep-dive)
3. [Redis Persistence](#redis-persistence)
4. [Redis Advanced Features](#redis-advanced-features)
5. [Redis for Caching](#redis-for-caching)
6. [Redis for Distributed Lock](#redis-for-distributed-lock)
7. [Redis Cluster](#redis-cluster)
8. [Redis Sentinel](#redis-sentinel)
9. [Redis Enterprise Practices](#redis-enterprise-practices)
10. [Spring Boot + Redis Integration](#spring-boot--redis-integration)
11. [Redisson Deep Dive](#redisson-deep-dive)
12. [Performance Tuning](#performance-tuning)
13. [Interview Questions](#interview-questions)

---

## Redis Fundamentals

### What Makes Redis Special?

Redis is an **in-memory data structure store** used as database, cache, message broker, and streaming engine.

```
Key Redis Differentiators:
┌────────────────────────────────────────────────────────────┐
│  1. In-memory: All data in RAM (sub-millisecond latency)   │
│  2. Single-threaded core: No race conditions, simple model  │
│  3. Rich data structures: Beyond simple key-value          │
│  4. Persistence: RDB + AOF for durability                  │
│  5. Replication: Master-replica for HA                     │
│  6. Cluster: Auto-sharding for horizontal scaling          │
│  7. Pub/Sub + Streams: Message broker capabilities          │
│  8. Lua scripting: Server-side atomic operations            │
└────────────────────────────────────────────────────────────┘
```

### Redis 6.0+ Multi-Threading

```
Redis 6.0+ IO Threads:
┌───────────────────────────────────────────────────────────┐
│  Before 6.0:                                               │
│  [Network IO] ─→ [Single thread: read, process, write]    │
│                                                             │
│  After 6.0: (IO threads, configurable)                     │
│  [Network IO thread 1] ─┐                                  │
│  [Network IO thread 2] ─┤                                  │
│  [Network IO thread 3] ─┼─→ [Single thread: core logic]   │
│  [Network IO thread 4] ─┘                                  │
│                                                             │
│  Core logic (command execution) remains SINGLE-THREADED.   │
│  Only IO (read/write to sockets) is multi-threaded.         │
│  Result: 2x throughput on multi-core machines.              │
└───────────────────────────────────────────────────────────┘
```

```bash
# Check Redis version
redis-server --version
# Redis server v=7.2.5

# Redis config for IO threads (redis.conf)
io-threads 4            # Enable 4 IO threads (default: 1)
io-threads-do-reads yes # Also thread reads (default: no)
```

### Redis vs Other Caches

| Feature | Redis | Memcached | Local Cache (Caffeine) |
|---|---|---|---|
| **Data structures** | Rich (8+ types) | Simple (string only) | Key-value |
| **Persistence** | RDB, AOF, hybrid | None | None |
| **Cluster** | Native (sharding) | Client-side (ketama) | N/A |
| **Transactions** | MULTI/EXEC + WATCH | None | N/A |
| **Lua scripting** | Built-in | None | N/A |
| **Pub/Sub** | Built-in | None | N/A |
| **Streams** | 5.0+ | None | N/A |
| **Performance** | ~100K ops/sec (single node) | ~100K ops/sec | Nanosecond (heap) |
| **Durability** | Configurable | None | None |
| **Data size** | RAM-bound | RAM-bound | Heap-bound |

### Installing Redis

```bash
# Docker (easiest for dev)
docker run -d --name redis-dev \
  -p 6379:6379 \
  -v redis-data:/data \
  redis:7.2.5-alpine \
  redis-server --appendonly yes --requirepass devpassword

# Redis with config file
docker run -d --name redis-dev \
  -p 6379:6379 \
  -v /path/to/redis.conf:/usr/local/etc/redis/redis.conf \
  -v redis-data:/data \
  redis:7.2.5-alpine \
  redis-server /usr/local/etc/redis/redis.conf

# Redis CLI
docker exec -it redis-dev redis-cli -a devpassword
```

```bash
# Redis.conf basics
# redis.conf
bind 0.0.0.0                    # Listen on all interfaces
port 6379
daemonize no                     # Run as foreground (Docker)
loglevel notice
logfile ""

# Security
requirepass yourpassword         # Auth password
rename-command FLUSHALL ""       # Disable dangerous commands
rename-command FLUSHDB ""
rename-command CONFIG ""

# Memory management
maxmemory 4gb                    # Max memory usage
maxmemory-policy allkeys-lru     # Eviction policy

# Persistence
save 900 1                       # RDB: 15 min if 1 key changed
save 300 10                      # RDB: 5 min if 10 keys changed
save 60 10000                    # RDB: 1 min if 10000 keys changed
appendonly yes                   # AOF enabled
appendfsync everysec             # AOF fsync policy
```

### Basic Redis Commands

```bash
# Connection
redis-cli -h localhost -p 6379 -a password
AUTH password

# Key operations
SET key value                    # Set key
GET key                          # Get key
DEL key                          # Delete key
EXISTS key                       # Check existence
EXPIRE key 3600                  # Set TTL (seconds)
TTL key                          # Get remaining TTL
PERSIST key                      # Remove TTL
KEYS pattern                     # Find keys (avoid in production!)
SCAN cursor [MATCH pattern] [COUNT count]  # Production-safe key scan

# Server
INFO [section]                   # Server info (memory, stats, etc.)
DBSIZE                           # Number of keys
FLUSHALL                         # Clear ALL data (dangerous)
MONITOR                          # Real-time command monitoring (dev only)
```

---

## Data Structures Deep Dive

### 1. String

**Operations and time complexity:**

```
SET key value              O(1)
GET key                    O(1)
MSET k1 v1 k2 v2          O(N)
MGET k1 k2                 O(N)
INCR key                   O(1)
INCRBY key n               O(1)
DECR key                   O(1)
STRLEN key                 O(1)
GETRANGE key start end     O(N)
SETEX key ttl value        O(1)  (SET + EXPIRE atomic)
SETNX key value            O(1)  (SET if Not eXists)
GETSET key value           O(1)
APPEND key value           O(1)
```

**Use cases:**
- Simple caching (HTML fragments, API responses)
- Counters (page views, likes)
- Distributed ID generation (INCR)
- Session data (JSON serialized)
- Rate limiting (INCR + EXPIRE)

**Internal encoding:**
```
String value length:
  < 44 bytes → embstr (embedded string, single allocation)
  ≥ 44 bytes → raw (SDS — Simple Dynamic String)
```

```java
// Redis String operations via Spring Data Redis
@Service
public class RedisStringService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    // Cache-aside pattern
    public String getCachedHtml(String pageKey) {
        String html = redisTemplate.opsForValue().get("html:" + pageKey);
        if (html == null) {
            html = renderHtml(pageKey); // Expensive operation
            redisTemplate.opsForValue().set("html:" + pageKey, html, 3600, TimeUnit.SECONDS);
        }
        return html;
    }

    // Counter
    public long incrementViewCount(String articleId) {
        return redisTemplate.opsForValue().increment("views:" + articleId);
    }

    // Distributed ID
    public long nextId(String keyPrefix) {
        return redisTemplate.opsForValue().increment("id:" + keyPrefix);
    }

    // SETNX — set if not exists
    public boolean tryAcquireLock(String lockKey, String value, long ttlMs) {
        return Boolean.TRUE.equals(
            redisTemplate.opsForValue()
                .setIfAbsent(lockKey, value, Duration.ofMillis(ttlMs))
        );
    }
}
```

### 2. Hash

**Operations:**

```
HSET key field value          O(1)  — Set field
HGET key field                O(1)  — Get field
HMSET key f1 v1 f2 v2        O(N)
HMGET key f1 f2              O(N)
HDEL key field                O(1)
HEXISTS key field             O(1)
HGETALL key                   O(N)  — Get ALL fields (use with caution)
HKEYS key                     O(N)
HVALS key                     O(N)
HLEN key                      O(1)
HINCRBY key field n           O(1)  — Increment field
HSCAN key cursor              O(1) per step
```

**Use cases:**
- Object storage (user profile, product details)
- Session data (multiple fields per session)
- Shopping cart (user_id → product_id → quantity)
- Configurations (feature flags, settings)

**Internal encoding:**
```
Hash has < 512 fields AND each value < 64 bytes → ziplist
Otherwise → hashtable
```

```java
// Hash operations
@Service
public class RedisHashService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // User profile as hash
    public void saveUserProfile(UserProfile profile) {
        String key = "user:" + profile.getUserId();
        Map<String, Object> fields = new HashMap<>();
        fields.put("name", profile.getName());
        fields.put("email", profile.getEmail());
        fields.put("age", profile.getAge());
        fields.put("createdAt", profile.getCreatedAt().toString());

        redisTemplate.opsForHash().putAll(key, fields);
    }

    public UserProfile getUserProfile(Long userId) {
        String key = "user:" + userId;
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
        if (entries.isEmpty()) return null;

        return UserProfile.builder()
            .userId(userId)
            .name((String) entries.get("name"))
            .email((String) entries.get("email"))
            .age((Integer) entries.get("age"))
            .build();
    }

    // Shopping cart
    public void addToCart(Long userId, Long productId, int quantity) {
        redisTemplate.opsForHash()
            .increment("cart:" + userId, String.valueOf(productId), quantity);
    }

    public Map<String, Integer> getCart(Long userId) {
        Map<Object, Object> cart = redisTemplate.opsForHash()
            .entries("cart:" + userId);
        Map<String, Integer> result = new HashMap<>();
        cart.forEach((k, v) -> result.put((String) k, (Integer) v));
        return result;
    }
}
```

### 3. List

**Operations:**

```
LPUSH key value           O(1)  — Push to left
RPUSH key value           O(1)  — Push to right
LPOP key                  O(1)  — Pop from left
RPOP key                  O(1)  — Pop from right
LLEN key                  O(1)
LRANGE key start stop     O(N)  — Get range
LINDEX key index          O(N)  — Get by index
LSET key index value      O(N)  — Set by index
LREM key count value      O(N)  — Remove elements
BLPOP key timeout         O(1)  — Blocking left pop
BRPOP key timeout         O(1)  — Blocking right pop
RPOPLPUSH src dst         O(1)  — Pop right, push left to another list
BRPOPLPUSH src dst timeout O(1) — Blocking version
```

**Use cases:**
- Simple message queue (LPUSH + BRPOP)
- Activity timeline (LPUSH + LRANGE)
- Latest items (LPUSH + LTRIM)
- Work queue (RPUSH + BLPOP)
- Inter-process communication (BLPOP)

```java
// List operations — simple message queue
@Service
public class RedisListQueue {

    @Autowired
    private StringRedisTemplate redis;

    private static final String QUEUE_KEY = "queue:order-events";

    // Producer
    public void sendOrderEvent(String eventJson) {
        redis.opsForList().leftPush(QUEUE_KEY, eventJson);
    }

    // Consumer (blocking, use with thread pool)
    public void startConsumer() {
        Executors.newSingleThreadExecutor().submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                // BLPOP with 30-second timeout
                String event = redis.opsForList()
                    .rightPop(QUEUE_KEY, 30, TimeUnit.SECONDS);
                if (event != null) {
                    processEvent(event);
                }
            }
        });
    }
}
```

### 4. Set

**Operations:**

```
SADD key member           O(1)
SREM key member           O(1)
SMEMBERS key              O(N)  — Get ALL members
SISMEMBER key member      O(1)  — Membership check
SCARD key                 O(1)  — Cardinality
SINTER key1 key2          O(N)  — Intersection
SUNION key1 key2          O(N)  — Union
SDIFF key1 key2           O(N)  — Difference
SPOP key [count]          O(1)  — Random pop
SRANDMEMBER key [count]   O(1)  — Random member without pop
SMOVE src dst member      O(1)
SSCAN key cursor          O(1) per step
```

**Use cases:**
- Tags/labels (article tags, user interests)
- Unique visitors (SADD for each visit)
- Social features (friends, followers, likes)
- Role-based access (user roles set)
- Random item selection (SRANDMEMBER, SPOP)

```java
// Set operations
@Service
public class RedisSetService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    // Tag system
    public void addTags(String articleId, List<String> tags) {
        String key = "article:tags:" + articleId;
        redisTemplate.opsForSet().add(key, tags.toArray(new String[0]));
    }

    public Set<String> getIntersectingArticles(String tag1, String tag2) {
        // Find articles that have BOTH tags
        return redisTemplate.opsForSet()
            .intersect("tag:" + tag1, Set.of("tag:" + tag2));
    }

    // Unique visitors
    public void recordVisit(String pageId, String userId) {
        redisTemplate.opsForSet()
            .add("page:visitors:" + pageId, userId);
    }

    public long getUniqueVisitorCount(String pageId) {
        return redisTemplate.opsForSet().size("page:visitors:" + pageId);
    }
}
```

### 5. Sorted Set (ZSet)

**Operations:**

```
ZADD key score member         O(log N)
ZREM key member               O(log N)
ZSCORE key member             O(1)
ZINCRBY key inc member        O(log N)
ZCARD key                     O(1)  — Cardinality
ZRANK key member              O(log N)  — Rank ascending
ZREVRANK key member           O(log N)  — Rank descending
ZRANGE key start stop         O(log N + M)  — Range by index
ZREVRANGE key start stop      O(log N + M)  — Range by index descending
ZRANGEBYSCORE key min max     O(log N + M)  — Range by score
ZREVRANGEBYSCORE key max min  O(log N + M)
ZREM key member               O(log N)  — Remove member
ZREMRANGEBYRANK key start stop O(log N + M)  — Remove by rank
ZREMRANGEBYSCORE key min max  O(log N + M)  — Remove by score
ZCOUNT key min max            O(log N)  — Count within score range
ZPOPMIN key [count]           O(log N)  — Pop minimum
ZPOPMAX key [count]           O(log N)  — Pop maximum
ZSCAN key cursor              O(1) per step
```

**Use cases:**
- Leaderboards (gaming, sales rankings)
- Rate limiting (sliding window via score timestamps)
- Priority queues (score = priority)
- Auto-complete (sorted strings)
- Delayed tasks (score = execution timestamp)
- Time-series data (score = timestamp)

```java
// Sorted Set — Leaderboard
@Service
public class RedisLeaderboardService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private static final String LEADERBOARD_KEY = "leaderboard:sales";

    // Update score
    public void recordSale(String sellerId, double amount) {
        redisTemplate.opsForZSet()
            .incrementScore(LEADERBOARD_KEY, sellerId, amount);
    }

    // Top 10 sellers
    public List<LeaderboardEntry> getTopSellers(int topN) {
        Set<ZSetOperations.TypedTuple<String>> entries = redisTemplate.opsForZSet()
            .reverseRangeWithScores(LEADERBOARD_KEY, 0, topN - 1);

        List<LeaderboardEntry> result = new ArrayList<>();
        int rank = 1;
        for (ZSetOperations.TypedTuple<String> entry : entries) {
            result.add(new LeaderboardEntry(rank++, entry.getValue(), entry.getScore()));
        }
        return result;
    }

    // Get seller rank
    public long getSellerRank(String sellerId) {
        Long rank = redisTemplate.opsForZSet()
            .reverseRank(LEADERBOARD_KEY, sellerId);
        return rank != null ? rank + 1 : -1;
    }

    // Sliding window rate limiter
    public boolean isRateLimited(String userId, int maxRequests, long windowMs) {
        String key = "ratelimit:" + userId;
        long now = System.currentTimeMillis();
        long windowStart = now - windowMs;

        // Remove old entries outside window
        redisTemplate.opsForZSet().removeRangeByScore(key, 0, windowStart);

        // Count current entries
        Long count = redisTemplate.opsForZSet().size(key);
        if (count != null && count >= maxRequests) {
            return true; // Rate limited
        }

        // Add current request
        redisTemplate.opsForZSet().add(key, String.valueOf(now), now);
        // Set TTL on the key
        redisTemplate.expire(key, Duration.ofMillis(windowMs));
        return false;
    }
}
```

### 6. Streams (Redis 5.0+)

Redis Streams is a log-based data structure for message queues with consumer groups. It is more feature-rich than Pub/Sub (persistence, acknowledgment, consumer groups).

**Operations:**

```
XADD key MAXLEN size * field value     O(1)  — Append message
XREAD COUNT count BLOCK ms STREAMS key O(1)  — Read messages
XGROUP CREATE key group start          O(1)  — Create consumer group
XREADGROUP GROUP group consumer        O(1)  — Read as consumer
XACK key group id                      O(1)  — Acknowledge message
XDEL key id                            O(1)  — Delete message
XLEN key                               O(1)  — Length
XRANGE key start end                   O(N)  — Range by ID
XREVRANGE key end start                O(N)  — Reverse range
XPENDING key group                     O(1)  — Pending messages
XCLAIM key group consumer min-idle-time id  — Claim pending message
XTRIM key MAXLEN size                 O(N)  — Trim stream
```

```java
// Redis Streams — Producer/Consumer
@Service
public class RedisStreamService {

    @Autowired
    private StringRedisTemplate redis;

    private static final String STREAM_KEY = "stream:orders";
    private static final String GROUP_NAME = "order-processors";

    @PostConstruct
    public void init() {
        // Create consumer group (if not exists)
        try {
            redis.opsForStream().createGroup(STREAM_KEY, GROUP_NAME);
        } catch (Exception e) {
            // Group already exists
        }
    }

    // Producer
    public String publishOrderEvent(OrderEvent event) {
        Map<String, String> fields = new HashMap<>();
        fields.put("orderId", event.orderId().toString());
        fields.put("userId", event.userId().toString());
        fields.put("amount", event.amount().toString());
        fields.put("type", event.type());
        fields.put("timestamp", String.valueOf(System.currentTimeMillis()));

        Record<String, String> record = StreamRecords.newRecord()
            .in(STREAM_KEY)
            .ofMap(fields);

        RecordId recordId = redis.opsForStream().add(record);
        return recordId.getValue();
    }

    // Consumer
    @Scheduled(fixedDelay = 100)
    public void consumeOrders() {
        List<MapRecord<String, String, String>> messages = redis.opsForStream()
            .read(Consumer.from(GROUP_NAME, "consumer-1"),
                  StreamReadOptions.empty().count(10).block(Duration.ofMillis(100)),
                  StreamOffset.create(STREAM_KEY, ReadOffset.lastConsumed()));

        if (messages == null) return;

        for (MapRecord<String, String, String> msg : messages) {
            try {
                processOrderMessage(msg.getValue());
                // Acknowledge
                redis.opsForStream().acknowledge(STREAM_KEY, GROUP_NAME, msg.getId());
            } catch (Exception e) {
                log.error("Failed to process message: {}", msg.getId(), e);
                // Message remains pending — will be retried
            }
        }
    }

    // Handle pending messages (recovery)
    @Scheduled(fixedDelay = 60000)
    public void claimPendingMessages() {
        PendingMessages pending = redis.opsForStream()
            .pending(STREAM_KEY, GROUP_NAME);

        if (pending != null && pending.size() > 0) {
            List<MapRecord<String, String, String>> pendingMsgs = redis.opsForStream()
                .read(Consumer.from(GROUP_NAME, "consumer-1"),
                      StreamReadOptions.empty().count(10),
                      StreamOffset.unbounded(STREAM_KEY));

            for (MapRecord<String, String, String> msg : pendingMsgs) {
                // Check idle time — if > 60 seconds, reclaim
                PendingRecord pendingRecord = ...; // Check idle time
                if (pendingRecord != null && pendingRecord.getIdleTimeInMs() > 60000) {
                    // Auto-claim
                    List<MapRecord<String, String, String>> claimed =
                        redis.<String, String>opsForStream()
                            .claim(STREAM_KEY, GROUP_NAME, "consumer-1",
                                   Duration.ofSeconds(60), msg.getId());
                    // Reprocess...
                }
            }
        }
    }
}
```

### 7. HyperLogLog

**Operations:**

```
PFADD key element           O(1)
PFCOUNT key                 O(1)  — Approximate cardinality
PFMERGE dest source1 source2  O(N)
```

- Uses ~12KB of memory regardless of data size
- Standard error: 0.81%
- **Use case**: Unique visitor counting (acceptable error for analytics)

```java
public long countUniqueVisitors(String pageId, String userId) {
    redisTemplate.opsForHyperLogLog()
        .add("uv:" + pageId, userId);
    return redisTemplate.opsForHyperLogLog().size("uv:" + pageId);
}
```

### 8. Bitmaps (String bit operations)

**Operations:**

```
SETBIT key offset value      O(1)
GETBIT key offset            O(1)
BITCOUNT key [start end]     O(N)
BITOP AND/OR/XOR/NOT dest    O(N)
BITPOS key bit [start end]   O(N)
```

- Space-efficient: 1 bit per user
- **Use case**: Daily active users, feature flags, bloom filters (basic)

```java
// Daily active user tracking (1 bit per user per day)
public void markActiveUser(long userId) {
    String key = "dau:" + LocalDate.now();
    redisTemplate.opsForValue().setBit(key, userId, true);
}

public boolean isActiveToday(long userId) {
    String key = "dau:" + LocalDate.now();
    return Boolean.TRUE.equals(redisTemplate.opsForValue().getBit(key, userId));
}

public long getDailyActiveUserCount() {
    String key = "dau:" + LocalDate.now();
    Long count = redisTemplate.execute(
        (RedisCallback<Long>) conn -> conn.bitCount(key.getBytes())
    );
    return count != null ? count : 0;
}
```

### 9. Geospatial

**Operations:**

```
GEOADD key lon lat member         O(log N)
GEOPOS key member                 O(log N)  — Get coordinates
GEODIST key m1 m2 [unit]         O(log N)  — Distance
GEORADIUS key lon lat radius unit  O(N log N)  — Nearby members
GEORADIUSBYMEMBER key member radius unit  O(log N)
```

- **Use case**: Nearby stores, ride-hailing, location-based features

```java
public void addStoreLocation(Long storeId, double lng, double lat) {
    redisTemplate.opsForGeo()
        .add("stores", new Point(lng, lat), storeId.toString());
}

public List<String> findNearbyStores(double lng, double lat, double radiusKm) {
    Circle circle = new Circle(new Point(lng, lat), new Distance(radiusKm, RedisGeoCommands.DistanceUnit.KILOMETERS));
    GeoResults<RedisGeoCommands.GeoLocation<String>> results = redisTemplate.opsForGeo()
        .radius("stores", circle);
    return results.getContent().stream()
        .map(r -> r.getContent().getName())
        .collect(Collectors.toList());
}
```

---

## Redis Persistence

### RDB (Redis Database File)

Point-in-time snapshots generated at configurable intervals.

```
Pros:
- Compact single file, perfect for backups
- Fast recovery on restart (load entire DB into memory)
- Minimal performance impact (fork-based BGSAVE)
- Versioned backups (keep daily/weekly snapshots)

Cons:
- Data loss between snapshots (configurable up to last 60s)
- Fork can be slow for large datasets (> 10GB)
- Not suitable for durable data storage
```

```conf
# redis.conf RDB configuration
save 900 1              # 15 min if at least 1 key changed
save 300 10             # 5 min if at least 10 keys changed
save 60 10000           # 1 min if at least 10000 keys changed

# Manual save commands
# BGSAVE  → background save (recommended, non-blocking)
# SAVE    → foreground save (blocking, use only in emergency!)
# LASTSAVE → check last successful save time

dbfilename dump.rdb
dir /data
rdbcompression yes       # Compress (LZF), saves space, uses CPU
rdbchecksum yes          # CRC64 checksum (performance impact)
```

### AOF (Append-Only File)

Logs every write operation, guaranteed durability.

```
Pros:
- Minimal data loss (fsync everysec = max 1 second of data)
- Append-only, sequential writes (fast, no fragmentation)
- Human-readable (can tail -f to see operations)
- Redis 7.0+ uses multi-part AOF with manifest

Cons:
- Larger file size than RDB
- Slower recovery (must replay all operations)
- AOF rewrite is expensive (but runs in background)
```

```conf
# redis.conf AOF configuration
appendonly yes                    # Enable AOF
appendfilename "appendonly.aof"

# fsync policy:
appendfsync everysec              # Every second (sweet spot)
# appendfsync always              # Every write (slow, 100% durable)
# appendfsync no                  # OS decides (fast, risky)

# AOF rewrite (compaction)
auto-aof-rewrite-percentage 100    # Rewrite when AOF grows by 100%
auto-aof-rewrite-min-size 64mb     # Minimum size to trigger rewrite
aof-use-rdb-preamble yes           # Hybrid RDB+AOF (Redis 4.0+)
no-appendfsync-on-rewrite no       # During rewrite, disable fsync?
```

### Hybrid Persistence (RDB + AOF, Redis 4.0+)

```
AOF rewrite produces a combination:
- RDB snapshot at the beginning
- AOF incremental log for operations during rewrite

Benefits:
- Faster AOF rewrite (RDB dump first, then incremental)
- Faster recovery (load RDB, replay minimal AOF)
- More compact AOF file
```

```conf
aof-use-rdb-preamble yes   # Default since Redis 5.0
```

### Backup Strategy

```bash
# 1. RDB backups (cron job)
0 */6 * * * cp /data/dump.rdb /backups/redis/$(date +\%Y\%m\%d_\%H).rdb

# 2. S3/OSS upload for offsite backup
0 2 * * * aws s3 cp /data/dump.rdb s3://my-backups/redis/dump-$(date +\%Y\%m\%d).rdb

# 3. AOF file for minimal data loss
# Keep AOF in production alongside RDB

# 4. Redis replication for HA
# Master continues operating, replica has real-time copy
```

### Recovery Process

```
Redis startup recovery order:
1. Check if AOF is enabled and AOF file exists → load AOF
2. If no AOF, check for RDB file → load RDB
3. If neither → empty DB

On corruption:
- RDB: redis-check-rdb /data/dump.rdb
- AOF: redis-check-aof /data/appendonly.aof
```

---

## Redis Advanced Features

### Transactions

Redis transactions are not like SQL transactions. They provide **isolation and atomicity** but NOT rollback.

```
MULTI           # Start transaction
SET k1 v1      # Command 1 (queued)
SET k2 v2      # Command 2 (queued)
INCR k1        # Command 3 (queued — will execute even though k1 is a string!)
EXEC            # Execute all commands atomically
DISCARD         # Discard transaction
```

```java
// Spring Data Redis transaction
@Autowired
private RedisTemplate<String, Object> redisTemplate;

public void transferBalance(String from, String to, int amount) {
    redisTemplate.execute(new SessionCallback<List<Object>>() {
        @Override
        public List<Object> execute(RedisOperations operations) throws DataAccessException {
            operations.multi();
            operations.opsForValue().decrement(from, amount);
            operations.opsForValue().increment(to, amount);
            return operations.exec();
        }
    });
}
```

**WATCH — Optimistic Locking:**

```java
// WATCH + MULTI = CAS (Compare And Set) pattern
public boolean updateIfNotChanged(String key, String newValue) {
    return redisTemplate.execute(new SessionCallback<Boolean>() {
        @Override
        public Boolean execute(RedisOperations ops) {
            ops.watch(key);                           // Watch key for changes
            String current = (String) ops.opsForValue().get(key);
            ops.multi();
            ops.opsForValue().set(key, newValue);     // Only executes if key didn't change
            List<Object> result = ops.exec();
            return result != null && !result.isEmpty(); // null = WATCH triggered, abort
        }
    });
}
```

### Pipeline

Batch multiple commands to reduce network round-trips:

```java
// Without pipeline: 1000 commands = 1000 RTTs
// With pipeline: 1000 commands = 1 RTT

public void insertBatch(Map<String, String> keyValues) {
    redisTemplate.executePipelined(new RedisCallback<Object>() {
        @Override
        public Object doInRedis(RedisConnection connection) {
            keyValues.forEach((key, value) -> {
                connection.set(
                    redisTemplate.getStringSerializer().serialize(key),
                    redisTemplate.getStringSerializer().serialize(value)
                );
            });
            return null; // Pipeline results are discarded
        }
    });
}

// Get multiple keys via pipeline
public List<String> getMultiValues(List<String> keys) {
    List<Object> results = redisTemplate.executePipelined(
        (RedisCallback<String>) connection -> {
            keys.forEach(key -> {
                connection.get(redisTemplate.getStringSerializer().serialize(key));
            });
            return null;
        }
    );
    return results.stream()
        .map(r -> (String) r)
        .collect(Collectors.toList());
}
```

### Lua Scripting

Atomic server-side execution of custom logic:

```lua
-- lua/check_and_set.lua
-- Atomic: SET if condition is met
local key = KEYS[1]
local expectedValue = ARGV[1]
local newValue = ARGV[2]
local ttl = ARGV[3]

local current = redis.call('GET', key)
if current == expectedValue then
    redis.call('SET', key, newValue)
    if ttl then
        redis.call('EXPIRE', key, ttl)
    end
    return 1
else
    return 0
end
```

```lua
-- lua/rate_limiter.lua
-- Sliding window rate limiter in Lua
local key = KEYS[1]
local windowMs = tonumber(ARGV[1])
local maxRequests = tonumber(ARGV[2])
local now = tonumber(ARGV[3])

-- Remove old entries
redis.call('ZREMRANGEBYSCORE', key, 0, now - windowMs)

-- Count current entries
local count = redis.call('ZCARD', key)

if count >= maxRequests then
    return 0  -- Rate limited
end

-- Add current request
redis.call('ZADD', key, now, now .. ':' .. math.random())
redis.call('EXPIRE', key, math.ceil(windowMs / 1000))
return 1  -- Allowed
```

```java
// Execute Lua script
@Service
public class LuaScriptService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public boolean checkAndSet(String key, String expectedValue, String newValue, long ttlSeconds) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(
            new ClassPathResource("lua/check_and_set.lua")));
        script.setResultType(Long.class);

        Long result = redisTemplate.execute(script,
            Collections.singletonList(key),
            expectedValue, newValue, String.valueOf(ttlSeconds));
        return Long.valueOf(1).equals(result);
    }

    // Rate limiter via Lua
    public boolean tryAcquire(String key, int maxRequests, long windowMs) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(
            new ClassPathResource("lua/rate_limiter.lua")));
        script.setResultType(Long.class);

        Long result = redisTemplate.execute(script,
            Collections.singletonList(key),
            String.valueOf(windowMs),
            String.valueOf(maxRequests),
            String.valueOf(System.currentTimeMillis()));
        return Long.valueOf(1).equals(result);
    }
}
```

### Pub/Sub

Simple publish/subscribe messaging (fire-and-forget, no persistence):

```java
// Publisher
@Service
public class RedisPublisher {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public void publish(String channel, Object message) {
        redisTemplate.convertAndSend(channel, message);
    }
}

// Subscriber
@Component
public class RedisSubscriber extends MessageListenerAdapter {

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel());
        String body = new String(message.getBody());
        System.out.printf("Received [%s]: %s%n", channel, body);
    }
}

// Configuration
@Configuration
public class RedisPubSubConfig {

    @Bean
    public MessageListenerAdapter messageListener(RedisSubscriber subscriber) {
        return new MessageListenerAdapter(subscriber);
    }

    @Bean
    public RedisMessageListenerContainer redisContainer(
            RedisConnectionFactory connectionFactory,
            MessageListenerAdapter listener) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        // Subscribe to channels
        container.addMessageListener(listener,
            new PatternTopic("orders:*"));
        container.addMessageListener(listener,
            new PatternTopic("notifications"));

        return container;
    }
}
```

---

## Redis for Caching

### Cache Patterns

#### Cache-Aside (Most Common)

Application manages the cache explicitly:

```
1. Read:     Check cache → MISS → Load from DB → Store in cache → Return
2. Write:    Write to DB → Delete cache entry (or update)
```

```java
@Service
public class CacheAsideService {

    @Autowired
    private RedisTemplate<String, Product> redisCache;

    @Autowired
    private ProductRepository productRepository;

    public Product getProduct(Long id) {
        // 1. Try cache
        String cacheKey = "product:" + id;
        Product cached = redisCache.opsForValue().get(cacheKey);

        if (cached != null) {
            return cached;  // Cache hit
        }

        // 2. Cache miss — load from DB
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ProductNotFoundException(id));

        // 3. Store in cache with TTL
        redisCache.opsForValue().set(cacheKey, product, Duration.ofHours(1));

        return product;
    }

    @Transactional
    public Product updateProduct(Long id, ProductUpdate update) {
        // 1. Update DB
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ProductNotFoundException(id));
        update.applyTo(product);
        productRepository.save(product);

        // 2. Invalidate cache (NOT update — let next read populate it)
        redisCache.delete("product:" + id);

        return product;
    }
}
```

#### Read-Through

Cache sits between application and DB. The cache itself loads data from DB on miss.

```java
// Using Spring's @Cacheable (default is read-through semantics)
@Service
public class ProductService {

    @Cacheable(value = "products", key = "#id", unless = "#result == null")
    public Product getProduct(Long id) {
        // This method is only called on a cache miss
        return productRepository.findById(id)
            .orElseThrow(() -> new ProductNotFoundException(id));
    }
}
```

#### Write-Through

Data is written to cache first, then cache writes to DB synchronously.

```
Write: Write to cache → Cache writes to DB → Confirm
```

#### Write-Behind (Write-Back)

Data is written to cache first, then **asynchronously** written to DB.

```
Write: Write to cache → Confirm immediately → Async write to DB later
Advantage: Very fast writes
Risk: Data loss if cache fails before DB write
```

### Cache Invalidation Strategies

| Strategy | How It Works | Best For |
|---|---|---|
| **TTL** | Key auto-expires after a fixed time | Simple, widely used |
| **LRU** | Evict least recently used | Memory management |
| **LFU** | Evict least frequently used | Hot data stays |
| **Random** | Evict random keys | Simple, no overhead |
| **Manual** | Application explicitly invalidates | Write-heavy scenarios |

```conf
# Redis eviction policies (maxmemory-policy)
# allkeys-lru:        Evict any key using LRU (most common)
# allkeys-lfu:        Evict least frequently used
# volatile-lru:       Evict only keys WITH TTL using LRU
# volatile-lfu:       Evict only keys WITH TTL using LFU
# volatile-ttl:       Evict key with shortest TTL
# allkeys-random:     Evict random key
# volatile-random:    Evict random key with TTL
# noeviction:         Return OOM error (default)

maxmemory 4gb
maxmemory-policy allkeys-lru
```

### Cache Problems and Solutions

#### Cache Penetration (穿透)

**Problem**: Requesting non-existent data bypasses the cache and hits the database every time.

```
Request for ID=99999 (doesn't exist):
  → Cache MISS
  → DB query (returns null)
  → Cache not set (null not cached)
  → Next request repeats cycle
  → DB gets hammered
```

**Solution 1: Cache null values (short TTL)**
```java
public Product getProduct(Long id) {
    String key = "product:" + id;
    Product cached = redis.opsForValue().get(key);

    if (cached != null) {
        return cached == NULL_PRODUCT ? null : cached;
    }

    Product product = productRepository.findById(id).orElse(null);
    // Cache null with short TTL (prevents penetration)
    redis.opsForValue().set(key,
        product != null ? product : NULL_PRODUCT,
        Duration.ofMinutes(product != null ? 60 : 5)
    );
    return product;
}
```

**Solution 2: Bloom Filter**
```java
// Bloom filter — probabilistic data structure
// Tells you: definitely NOT present, or MAYBE present
// 0.1% false positive rate, but no false negatives

@Component
public class BloomFilterService {

    private final RedisTemplate<String, String> redis;

    // Using Redisson's Bloom filter
    private final RBloomFilter<Long> bloomFilter;

    public BloomFilterService(RedissonClient redisson) {
        this.bloomFilter = redisson.getBloomFilter("product-id-bloom");
        this.bloomFilter.tryInit(10000000L, 0.001); // 10M expected, 0.1% FP rate

        // Load existing IDs into filter
        loadExistingIds();
    }

    public boolean mightExist(Long productId) {
        return bloomFilter.contains(productId);
    }

    public void add(Long productId) {
        bloomFilter.add(productId);
    }

    // Usage in service
    public Product getProductSafe(Long id) {
        // Fast check — reject non-existent IDs immediately
        if (!bloomFilter.mightExist(id)) {
            return null; // Definitively doesn't exist
        }
        // May exist — proceed with cache/DB
        return getProduct(id);
    }
}
```

#### Cache Avalanche (雪崩)

**Problem**: A large number of cached keys expire at the same time, causing a massive DB load spike.

```
Timeline:
T+0:  Cache keys set with TTL=3600
T+3600: ALL keys expire simultaneously
T+3600: 10,000 requests → 10,000 DB queries → DB overload
```

**Solution 1: Random TTL**
```java
// Add random jitter to TTL
public void cacheProduct(Product product) {
    // Base TTL: 1 hour, random jitter: ±10 minutes
    long ttl = 3600 + ThreadLocalRandom.current().nextLong(-600, 600);
    redis.opsForValue().set("product:" + product.getId(), product, Duration.ofSeconds(ttl));
}

// Spring Cache with configurable TTL
@Cacheable(value = "products", key = "#id")
public Product getProduct(Long id) {
    return productRepository.findById(id).orElse(null);
}

// Custom TTL configuration in RedisCacheManager
@Bean
public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
    RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(Duration.ofSeconds(3600 + ThreadLocalRandom.current().nextLong(-600, 600)))
        .disableCachingNullValues()
        .serializeKeysWith(RedisSerializationContext.SerializationPair
            .fromSerializer(new StringRedisSerializer()))
        .serializeValuesWith(RedisSerializationContext.SerializationPair
            .fromSerializer(new GenericJackson2JsonRedisSerializer()));

    return RedisCacheManager.builder(factory)
        .cacheDefaults(config)
        .build();
}
```

**Solution 2: Multi-level caching**
```java
// L1: Local cache (Caffeine) + L2: Redis
// Fast L1 hit avoids Redis/DB call entirely
@Service
public class MultiLevelCacheService {

    private final Cache<String, Product> localCache = Caffeine.newBuilder()
        .maximumSize(10000)
        .expireAfterWrite(Duration.ofMinutes(5))
        .recordStats()
        .build();

    @Autowired
    private RedisTemplate<String, Product> redisCache;

    public Product getProduct(Long id) {
        String key = "product:" + id;

        // L1: Local cache (nanosecond access)
        Product local = localCache.getIfPresent(key);
        if (local != null) {
            return local;
        }

        // L2: Redis (millisecond access)
        Product redis = redisCache.opsForValue().get(key);
        if (redis != null) {
            localCache.put(key, redis);
            return redis;
        }

        // L3: Database (10-100ms)
        Product db = productRepository.findById(id)
            .orElseThrow(() -> new ProductNotFoundException(id));
        redisCache.opsForValue().set(key, db, Duration.ofHours(1));
        localCache.put(key, db);
        return db;
    }
}
```

#### Cache Breakdown (击穿/热点)

**Problem**: A specific hot key expires, and concurrent requests overwhelm the DB.

```
Scenario: Product #12345 is a hot item
T+0: Key expires
T+0.001: 5000 concurrent requests request Product #12345
T+0.001: All miss cache, all hit DB simultaneously
```

**Solution 1: Mutex lock**
```java
public Product getProductWithMutex(Long id) {
    String key = "product:" + id;
    String lockKey = "lock:product:" + id;

    Product cached = redis.opsForValue().get(key);
    if (cached != null) {
        return cached;
    }

    // Only ONE thread gets the lock
    Boolean locked = redis.opsForValue()
        .setIfAbsent(lockKey, "LOCKED", Duration.ofSeconds(10));

    if (Boolean.TRUE.equals(locked)) {
        try {
            // Double-check: another thread might have already loaded it
            cached = redis.opsForValue().get(key);
            if (cached != null) {
                return cached;
            }

            // Load from DB
            cached = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
            redis.opsForValue().set(key, cached, Duration.ofHours(1));
            return cached;
        } finally {
            redis.delete(lockKey); // Release lock
        }
    } else {
        // Another thread is loading — wait briefly and retry
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return getProductWithMutex(id); // Retry
    }
}
```

**Solution 2: Never-expire hot keys + background refresh**
```java
// Hot keys have no TTL, but are refreshed in background
@Service
public class HotKeyService {

    @PostConstruct
    public void startRefreshTask() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
        scheduler.scheduleAtFixedRate(this::refreshHotProducts, 0, 30, TimeUnit.SECONDS);
    }

    private void refreshHotProducts() {
        // Load hot keys into Redis with no TTL
        List<Long> hotIds = getHotProductIds(); // 10 hottest products
        for (Long id : hotIds) {
            Product product = productRepository.findById(id).orElse(null);
            if (product != null) {
                // Never expire
                redis.opsForValue().set("product:" + id, product);
            }
        }
    }

    // Also refresh on read if stale
    public Product getProduct(Long id) {
        String key = "product:" + id;
        Product cached = redis.opsForValue().get(key);
        if (cached == null) {
            cached = loadFromDB(id);
            redis.opsForValue().set(key, cached, Duration.ofMinutes(30));
            // For hot keys, extend TTL or no TTL
            if (isHotProduct(id)) {
                redis.expire(key, Duration.ofHours(1));
            }
        }
        return cached;
    }
}
```

### Spring Cache Abstraction

```java
@Service
@CacheConfig(cacheNames = "products") // Default cache name
public class ProductService {

    // Cache-Aside: Read
    @Cacheable(
        value = "products",
        key = "#id",
        unless = "#result == null",          // Don't cache null
        condition = "#id > 0",               // Only cache valid IDs
        sync = true                           // Mutex for concurrent access (breakdown protection)
    )
    public Product getProduct(Long id) {
        return productRepository.findById(id)
            .orElseThrow(() -> new ProductNotFoundException(id));
    }

    // Multi-key cache
    @Cacheable(value = "products", key = "#ids")
    public List<Product> getProducts(List<Long> ids) {
        return productRepository.findAllById(ids);
    }

    // Write: Update cache
    @CachePut(value = "products", key = "#product.id")
    public Product updateProduct(Product product) {
        return productRepository.save(product);
    }

    // Write: Evict cache
    @CacheEvict(value = "products", key = "#id")
    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }

    // Clear entire cache
    @CacheEvict(value = "products", allEntries = true)
    public void clearCache() {
        // Just empties the cache, no DB operation needed
    }

    // Combined: multiple cache operations
    @Caching(
        put = { @CachePut(value = "products", key = "#product.id") },
        evict = { @CacheEvict(value = "product-lists", allEntries = true) }
    )
    public Product saveProduct(Product product) {
        return productRepository.save(product);
    }

    // Custom key generator
    @Cacheable(value = "search-results", key = "@customKeyGenerator.generate(#query, #page)")
    public SearchResult searchProducts(String query, int page) {
        return productRepository.search(query, page);
    }
}
```

### Cache Configuration

```java
@Configuration
@EnableCaching
public class CacheConfig extends CachingConfigurerSupport {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        // Default configuration
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofHours(1))           // Default TTL: 1 hour
            .disableCachingNullValues()               // Don't cache null
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair
                    .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair
                    .fromSerializer(new GenericJackson2JsonRedisSerializer()));

        // Per-cache configuration
        Map<String, RedisCacheConfiguration> configs = new HashMap<>();
        configs.put("products", defaultConfig.entryTtl(Duration.ofHours(2)));
        configs.put("users", defaultConfig.entryTtl(Duration.ofMinutes(30)));
        configs.put("sessions", defaultConfig.entryTtl(Duration.ofMinutes(15)));
        configs.put("search-results", defaultConfig.entryTtl(Duration.ofMinutes(5)));

        return RedisCacheManager.builder(factory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(configs)
            .transactionAware()  // Participate in Spring transactions
            .build();
    }

    @Bean
    public KeyGenerator customKeyGenerator() {
        return (target, method, params) -> {
            StringBuilder sb = new StringBuilder();
            sb.append(target.getClass().getSimpleName()).append(":");
            sb.append(method.getName());
            for (Object param : params) {
                sb.append(":").append(param);
            }
            return sb.toString();
        };
    }
}
```

---

## Redis for Distributed Lock

### Why a Distributed Lock?

In a distributed system, `synchronized` and `ReentrantLock` don't work because they only lock within a single JVM. A distributed lock coordinates access across multiple service instances.

```
Without distributed lock:
Service Instance 1: read count=10, increment, write count=11
Service Instance 2: read count=10, increment, write count=11
                                     ^^^ LOST UPDATE!

With distributed lock:
Service Instance 1: ACQUIRE LOCK, read count=10, inc, write count=11, RELEASE
Service Instance 2: WAIT...              ACQUIRE LOCK, read count=11, inc, write 12
```

### Simple Lock: SET NX EX

```java
@Service
public class SimpleRedisLock {

    @Autowired
    private StringRedisTemplate redis;

    // Try to acquire lock
    public boolean tryLock(String lockKey, String requestId, long expireMs) {
        // SET lock_key request_id NX PX expireMs
        // NX = set if not exists, PX = expire in milliseconds
        Boolean result = redis.opsForValue()
            .setIfAbsent(lockKey, requestId, Duration.ofMillis(expireMs));
        return Boolean.TRUE.equals(result);
    }

    // Release lock (must be ATOMIC — check owner, then delete)
    public boolean unlock(String lockKey, String requestId) {
        // Use Lua script for atomicity
        String script = """
            if redis.call('GET', KEYS[1]) == ARGV[1] then
                return redis.call('DEL', KEYS[1])
            else
                return 0
            end
            """;

        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(script, Long.class);
        Long result = redis.execute(redisScript, Collections.singletonList(lockKey), requestId);
        return Long.valueOf(1).equals(result);
    }

    // Usage
    public void doWithLock(String lockKey, Runnable action) {
        String requestId = UUID.randomUUID().toString();
        try {
            if (tryLock(lockKey, requestId, 30000)) {
                try {
                    action.run();
                } finally {
                    unlock(lockKey, requestId);
                }
            } else {
                throw new LockAcquisitionException("Could not acquire lock: " + lockKey);
            }
        }
    }
}
```

**Problems with simple locks:**
1. **No reentrancy**: Same thread can't re-acquire the lock
2. **No automatic renewal**: If operation takes longer than expire time, lock is released prematurely
3. **No retry mechanism**: Single attempt only
4. **No wait mechanism**: Immediately fails if lock is held

### Redlock Algorithm

The Redlock algorithm (proposed by Redis creator Antirez) provides a consensus-based distributed lock across multiple Redis nodes.

```
Client → Lock with N/2+1 Redis nodes
1. Get current timestamp (ms)
2. Lock on each node sequentially (with short timeout)
3. If majority (≥ N/2+1) acquired AND total time < lock TTL → LOCK ACQUIRED
4. If lock failed → unlock ALL nodes

Debate: Some distributed systems experts (including Martin Kleppmann)
argue Redlock has fundamental issues with clock drift and GC pauses.
Use with understanding of the tradeoffs.
```

### Redisson Deep Dive

Redisson is a Redis Java client that provides production-ready distributed lock implementations and more.

```xml
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson-spring-boot-starter</artifactId>
    <version>3.27.0</version>
</dependency>
```

```yaml
# application.yml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password: devpassword
      timeout: 3000ms
      lettuce:
        pool:
          enabled: true
          max-active: 16
          max-idle: 8
          min-idle: 4
          max-wait: 3000ms
  # OR use Redisson config
  redis:
    redisson:
      config: |
        singleServerConfig:
          address: "redis://localhost:6379"
          password: devpassword
          connectionPoolSize: 16
          connectionMinimumIdleSize: 4
          idleConnectionTimeout: 10000
          connectTimeout: 10000
          timeout: 3000
          retryAttempts: 3
          retryInterval: 1500
        threads: 4
        nettyThreads: 8
```

```java
@Service
public class DistributedLockService {

    @Autowired
    private RedissonClient redisson;

    // Basic lock
    public void basicLockExample(String orderId) {
        RLock lock = redisson.getLock("lock:order:" + orderId);

        try {
            // Acquire with timeout
            if (lock.tryLock(5, 30, TimeUnit.SECONDS)) {
                try {
                    // Critical section
                    processOrder(orderId);
                } finally {
                    lock.unlock();
                }
            } else {
                throw new LockAcquisitionException("Could not acquire lock");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // Reentrant lock (same thread can lock multiple times)
    public void reentrantLockExample() {
        RLock lock = redisson.getLock("reentrant:lock");

        lock.lock();
        try {
            nestedMethod(); // Acquires the SAME lock again
        } finally {
            lock.unlock();
        }
    }

    private void nestedMethod() {
        RLock lock = redisson.getLock("reentrant:lock");
        lock.lock(); // Works because Redisson lock is reentrant
        try {
            // ...
        } finally {
            lock.unlock();
        }
    }

    // Read-Write Lock
    public void readWriteLockExample(Long productId) {
        RReadWriteLock rwLock = redisson.getReadWriteLock("rw-lock:product:" + productId);
        RLock readLock = rwLock.readLock();
        RLock writeLock = rwLock.writeLock();

        // Multiple readers can read simultaneously
        readLock.lock();
        try {
            Product product = getFromCache(productId);
            // Reading...
        } finally {
            readLock.unlock();
        }

        // Only one writer, blocks all readers
        writeLock.lock();
        try {
            // Writing...
            updateCache(productId, newData);
        } finally {
            writeLock.unlock();
        }
    }

    // Semaphore (limited concurrent access)
    public void semaphoreExample() {
        RSemaphore semaphore = redisson.getSemaphore("semaphore:db-connections");
        semaphore.trySetPermits(10); // Max 10 concurrent

        try {
            if (semaphore.tryAcquire(5, TimeUnit.SECONDS)) {
                try {
                    databaseOperation();
                } finally {
                    semaphore.release();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // CountDownLatch (wait for N operations to complete)
    public void countDownLatchExample() {
        RCountDownLatch latch = redisson.getCountDownLatch("latch:batch-job");

        // Setup
        latch.trySetCount(3); // Wait for 3 workers

        // Worker calls
        Executors.newFixedThreadPool(3).submit(() -> {
            // Do work...
            latch.countDown();
        });

        // Wait for all 3 to finish
        try {
            latch.await(30, TimeUnit.SECONDS);
            log.info("All workers completed");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

### Redisson Watchdog (Automatic Lock Renewal)

Redisson's watchdog prevents premature lock expiration by automatically renewing the lock's TTL.

```
Lock acquired: TTL = 30s
After 10s: Watchdog renews TTL to 30s
After 20s: Watchdog renews TTL to 30s
...continues until unlock() is called or application crashes

If application crashes:
- No more heartbeat
- Lock expires after 30 seconds (TTL from last renewal)
- Other instances can acquire the lock
```

```java
// Watchdog is automatic — just use lock() without specifying leaseTime
@Service
public class WatchdogDemo {

    @Autowired
    private RedissonClient redisson;

    public void autoRenewalLock() {
        RLock lock = redisson.getLock("watchdog:lock");

        // lock() with NO leaseTime → Watchdog enabled (default 30s renewal)
        lock.lock();

        // CANCEL the watchdog by providing leaseTime:
        // lock.lock(30, TimeUnit.SECONDS); // NO watchdog — lock auto-expires after 30s

        try {
            // Long-running operation (could take 1 hour)
            processLargeDataset();
        } finally {
            lock.unlock(); // Watchdog stops
        }
    }

    // Custom watchdog timeout
    @Bean
    public Config redissonConfig() {
        Config config = new Config();
        config.useSingleServer()
            .setAddress("redis://localhost:6379")
            .setPassword("devpassword");

        // Default watchdog timeout: 30 seconds
        // When lock is acquired without leaseTime:
        // Watchdog renews every internalLockLeaseTime / 3 = 10s
        // If renewal fails, lock auto-expires after internalLockLeaseTime (30s)

        // Custom lock watchdog timeout:
        config.setLockWatchdogTimeout(15_000); // 15 seconds (default: 30000)

        return config;
    }
}
```

### Distributed Lock Best Practices

```java
// Best practice: Use tryLock with timeout + leaseTime
public String createOrderSafe(OrderRequest request) {
    String lockKey = "lock:order-create:" + request.getUserId();
    RLock lock = redisson.getLock(lockKey);

    try {
        // Wait max 3s for lock, auto-expire after 10s (no watchdog needed)
        if (lock.tryLock(3, 10, TimeUnit.SECONDS)) {
            try {
                return processOrderCreation(request);
            } finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        } else {
            throw new ServiceException("System busy, please retry");
        }
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new ServiceException("Operation interrupted");
    }
}

// Never use this pattern (race condition):
// if (lock.tryLock()) {
//     doSomething();
// }
// lock.unlock(); // What if doSomething() throws? Lock leaks!
```

**Lock Key Design:**

```
# Naming convention: lock:<domain>:<business-id>
lock:order:12345              # Lock for order 12345
lock:user:67890:payment       # Lock for user 67890's payment
lock:product:111:inventory    # Lock for product 111's inventory update

# Granularity:
# FINE-GRAINED: lock per order ID (better concurrency)
# COARSE: lock all orders (worse concurrency, simpler)
```

---

## Redis Cluster

### Cluster Architecture

```
Redis Cluster: Auto-sharding with hash slots
┌──────────────────────────────────────────────────────────────┐
│                     Redis Cluster                            │
│                                                              │
│  ┌──────────┐   ┌──────────┐   ┌──────────┐   ┌──────────┐  │
│  │  Node 1  │   │  Node 2  │   │  Node 3  │   │  Node 4  │  │
│  │ Slots:   │   │ Slots:   │   │ Slots:   │   │ Slots:   │  │
│  │ 0-4095   │   │ 4096-8191│   │ 8192-12287│  │12288-16383│  │
│  │ M→R1     │   │ M→R2     │   │ M→R3     │   │ M→R4     │  │
│  │          │   │          │   │          │   │          │  │
│  │ Replica  │   │ Replica  │   │ Replica  │   │ Replica  │  │
│  │ of R4    │   │ of R1    │   │ of R2    │   │ of R3    │  │
│  └──────────┘   └──────────┘   └──────────┘   └──────────┘  │
│                                                              │
│  16384 hash slots total: CRC16(key) % 16384                  │
│  Each node handles a range of hash slots                      │
│  Each master has 1+ replicas for HA                          │
└──────────────────────────────────────────────────────────────┘
```

### Hash Slot Calculation

```
Hash Slot = CRC16(key) mod 16384

For keys with hash tags:
- Key: {user123}.profile  → CRC16("user123") mod 16384
- Key: {user123}.orders   → CRC16("user123") mod 16384
- Same hash tag → Same slot → Same node → Can use transactions

Use hash tags to co-locate related keys:
  order:12345:detail     → slot 5432 (Node 2)
  order:12345:items      → slot 5432 (Node 2)  ← Same slot if using hash tag
  order:{12345}:detail   → same slot as order:{12345}:items
```

### Cluster Setup

```bash
# Docker compose for minimal Redis cluster (6 nodes: 3M + 3R)
version: "3.8"
services:
  redis-cluster:
    image: redis:7.2.5-alpine
    command: redis-cli --cluster create
      127.0.0.1:6371 127.0.0.1:6372 127.0.0.1:6373
      127.0.0.1:6374 127.0.0.1:6375 127.0.0.1:6376
      --cluster-replicas 1
    depends_on:
      - redis-1
      - redis-2
      - redis-3
      - redis-4
      - redis-5
      - redis-6

# Create cluster manually:
# docker exec -it redis-1 redis-cli --cluster create \
#   10.0.0.1:6371 10.0.0.2:6372 10.0.0.3:6373 \
#   10.0.0.4:6374 10.0.0.5:6375 10.0.0.6:6376 \
#   --cluster-replicas 1
```

### Client-Side Sharding vs Cluster

| Approach | How It Works | Pros | Cons |
|---|---|---|---|
| **Client-side** | Application calculates which Redis node to use | Simple, no cluster overhead | Rebalancing requires app changes, no auto-failover |
| **Proxy (Twemproxy, Codis)** | Proxy routes requests | Transparent to client | Extra hop, proxy is SPOF |
| **Redis Cluster** | Auto sharding + failover | Native Redis, no proxy, auto-rebalance | Complex setup, limited multi-key operations |

### Cluster Operations

```bash
# Cluster info
redis-cli -c -h 127.0.0.1 -p 6371
127.0.0.1:6371> CLUSTER INFO
127.0.0.1:6371> CLUSTER NODES
127.0.0.1:6371> CLUSTER SLOTS

# When a key belongs to a different node:
# Redirect with MOVED error (client must follow)
127.0.0.1:6371> SET mykey value
(error) MOVED 1234 10.0.0.2:6372

# Redis CLI with -c flag handles MOVED automatically
# Redis Cluster client (Jedis, Lettuce) handles MOVED transparently
```

### Cluster Scaling

```bash
# Add a new node
redis-cli --cluster add-node 10.0.0.5:6375 10.0.0.1:6371

# Reshard slots to new node
redis-cli --cluster reshard 10.0.0.1:6371
# → Interactive: how many slots, which node gets them

# Add replica
redis-cli --cluster add-node \
  10.0.0.6:6376 10.0.0.1:6371 \
  --cluster-slave --cluster-master-id <master-node-id>

# Delete node (must have 0 slots)
redis-cli --cluster del-node 10.0.0.5:6375 <node-id>
```

### Cluster Limitations

```java
// 1. Multi-key operations only work if keys are on the SAME node
// WRONG: Keys might be on different nodes
redis.opsForValue().multiGet(Set.of("key1", "key2")); // Works only if same slot

// RIGHT: Use hash tags
redis.opsForValue().multiGet(Set.of("{user:123}:profile", "{user:123}:settings"));

// 2. Transactions (MULTI/EXEC) only work on same node
// Use hash tags for transactional key groups

// 3. Pipeline works but commands are sent per-node
// No cross-node atomicity

// 4. Lua scripts must operate on same-node keys
// Use KEYS array with same hash tag

// 5. SCAN with pattern can't guarantee full scan
// Must SCAN each node independently
```

---

## Redis Sentinel

### Sentinel Architecture

```
              ┌──────────────────┐
              │   Sentinel 1     │
              │  (monitoring)    │
              └────────┬─────────┘
                       │
         ┌─────────────┼─────────────┐
         │             │             │
  ┌──────▼──────┐ ┌───▼───────┐ ┌───▼──────┐
  │  Sentinel 2  │ │ Sentinel 3│ │Sentinel 4│
  │  (Voting)   │ │ (Leader)  │ │(Voting)  │
  └──────┬──────┘ └──────┬────┘ └─────┬────┘
         │               │            │
         │         ┌─────┴─────┐      │
         │         │  Master   │      │
         ├─────────│  Redis    │◄─────┤
         │         │  (active) │      │
         │         └───────────┘      │
         │               │            │
         │         ┌─────▼─────┐      │
         └─────────│  Replica  │◄─────┘
                   │  Redis    │
                   └───────────┘
```

### Sentinel Functions

| Function | Description |
|---|---|
| **Monitoring** | Checks if master/replicas are functioning (PING every 1s) |
| **Notification** | Alert via API when something goes wrong |
| **Auto-failover** | Promote replica to master if master fails |
| **Configuration** | Provide current master address to clients |

### Sentinel Configuration

```conf
# sentinel.conf (port 26379)
port 26379
sentinel monitor mymaster 127.0.0.1 6379 2
sentinel down-after-milliseconds mymaster 5000
sentinel failover-timeout mymaster 60000
sentinel parallel-syncs mymaster 1
sentinel auth-pass mymaster yourpassword
```

```bash
# Start Sentinel
redis-sentinel /path/to/sentinel.conf

# Docker Compose
version: "3.8"
services:
  redis-master:
    image: redis:7.2.5-alpine
    command: redis-server --requirepass yourpassword
  redis-replica:
    image: redis:7.2.5-alpine
    command: redis-server --requirepass yourpassword --slaveof redis-master 6379
    depends_on:
      - redis-master
  sentinel:
    image: redis:7.2.5-alpine
    command: redis-sentinel /sentinel.conf
    volumes:
      - ./sentinel.conf:/sentinel.conf
    depends_on:
      - redis-master
      - redis-replica
```

### Spring Boot with Sentinel

```yaml
spring:
  data:
    redis:
      sentinel:
        master: mymaster
        nodes:
          - localhost:26379
          - localhost:26380
          - localhost:26381
      password: yourpassword
```

---

## Spring Boot + Redis Integration

### Maven Dependencies

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-pool2</artifactId> <!-- Connection pooling -->
</dependency>
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson-spring-boot-starter</artifactId>
    <version>3.27.0</version>
</dependency>
```

### Configuration

```yaml
# Single Redis
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password: devpassword
      timeout: 3000ms
      lettuce:
        pool:
          enabled: true
          max-active: 16        # Max connections
          max-idle: 8           # Max idle connections
          min-idle: 4           # Min idle connections
          max-wait: 3000ms      # Wait for connection timeout
        shutdown-timeout: 200ms

# Redis Cluster
# spring:
#   data:
#     redis:
#       cluster:
#         nodes:
#           - 10.0.0.1:6371
#           - 10.0.0.2:6372
#           - 10.0.0.3:6373
#       timeout: 3000ms
#       lettuce:
#         cluster:
#           refresh:
#             adaptive: true
#             period: 2000ms

# Redis Sentinel
# spring:
#   data:
#     redis:
#       sentinel:
#         master: mymaster
#         nodes:
#           - localhost:26379
#           - localhost:26380
```

### Custom Redis Template

```java
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory connectionFactory) {

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Key serializer
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        template.setKeySerializer(keySerializer);
        template.setHashKeySerializer(keySerializer);

        // Value serializer (JSON)
        GenericJackson2JsonRedisSerializer valueSerializer =
            new GenericJackson2JsonRedisSerializer();
        template.setValueSerializer(valueSerializer);
        template.setHashValueSerializer(valueSerializer);

        // Enable transaction support
        template.setEnableTransactionSupport(true);

        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(
            RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }
}
```

### Redis Operations Template

```java
@Service
public class RedisService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    // ─── String Operations ───
    public void setValue(String key, Object value, long ttlSeconds) {
        redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(ttlSeconds));
    }

    public <T> T getValue(String key, Class<T> type) {
        return (T) redisTemplate.opsForValue().get(key);
    }

    // ─── Hash Operations ───
    public void setHashField(String key, String field, Object value) {
        redisTemplate.opsForHash().put(key, field, value);
    }

    public <T> T getHashField(String key, String field) {
        return (T) redisTemplate.opsForHash().get(key, field);
    }

    // ─── List Operations ───
    public void pushToList(String key, Object value) {
        redisTemplate.opsForList().leftPush(key, value);
    }

    public <T> T popFromList(String key) {
        return (T) redisTemplate.opsForList().rightPop(key);
    }

    // ─── Set Operations ───
    public void addToSet(String key, Object... values) {
        redisTemplate.opsForSet().add(key, values);
    }

    public boolean isMember(String key, Object value) {
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(key, value));
    }

    // ─── Sorted Set Operations ───
    public void addToSortedSet(String key, Object value, double score) {
        redisTemplate.opsForZSet().add(key, value, score);
    }

    public Set<Object> getTopFromSortedSet(String key, int topN) {
        return redisTemplate.opsForZSet().reverseRange(key, 0, topN - 1);
    }

    // ─── Key Operations ───
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public boolean expire(String key, long ttlSeconds) {
        return Boolean.TRUE.equals(redisTemplate.expire(key, ttlSeconds, TimeUnit.SECONDS));
    }

    // ─── Counters ───
    public long increment(String key) {
        Long value = redisTemplate.opsForValue().increment(key);
        return value != null ? value : 0;
    }

    public long incrementBy(String key, long delta) {
        Long value = redisTemplate.opsForValue().increment(key, delta);
        return value != null ? value : 0;
    }

    // ─── HyperLogLog ───
    public void pfadd(String key, String element) {
        redisTemplate.opsForHyperLogLog().add(key, element);
    }

    public long pfcount(String key) {
        Long count = redisTemplate.opsForHyperLogLog().size(key);
        return count != null ? count : 0;
    }
}
```

---

## Performance Tuning

### Connection Pool Configuration

```yaml
# Lettuce connection pool (recommended for Spring Boot 2.x/3.x)
spring:
  data:
    redis:
      lettuce:
        pool:
          max-active: 16           # Max connections (default: 8)
          max-idle: 8              # Max idle connections
          min-idle: 4              # Min idle connections (keep-alive)
          max-wait: 3000ms         # Max wait for connection

# Pool sizing guide:
# max-active = (CPU cores × 2) + IO wait factor
# For high IO (slow commands): more connections
# For fast operations: fewer connections (CPU-bound)
```

### Pipelining for Bulk Operations

```java
// Bad: 1000 individual round trips (1000 RTTs)
public void badBulkInsert(Map<String, String> data) {
    data.forEach((k, v) -> redisTemplate.opsForValue().set(k, v));
    // 1000 network round trips!
}

// Good: 1 pipeline round trip
public void bulkInsertPipelined(Map<String, String> data) {
    redisTemplate.executePipelined(new RedisCallback<Void>() {
        @Override
        public Void doInRedis(RedisConnection connection) {
            data.forEach((k, v) -> {
                connection.set(
                    stringSerializer.serialize(k),
                    stringSerializer.serialize(v));
            });
            return null;
        }
    });
}
```

### Key Design Best Practices

```
Good key design:
─────────────────
user:12345:profile          ✓ Clear namespace
order:202401:12345          ✓ Sortable (date prefix)
product:{12345}:detail      ✓ Hash tag for cluster

Bad key design:
─────────────────
12345                        ✗ No context
user:getProfile:12345       ✗ Method name in key
user:12345:a:v:e:r:y:long   ✗ Too long
user/USER/User:12345        ✗ Inconsistent case
temp_abc                    ✗ No cleanup strategy
```

### Memory Optimization

```bash
# Memory analysis
INFO memory
# used_memory: 1073741824 (1GB)
# used_memory_rss: 1200000000
# used_memory_peak: 2048000000
# used_memory_overhead: 500000000  ← Data structure overhead
# mem_fragmentation_ratio: 1.12    ← 1.0-1.5 is normal, >1.5 indicates fragmentation

MEMORY STATS                    # Detailed memory breakdown
MEMORY USAGE key                # Memory used by a specific key
MEMORY PURGE                    # Defragment memory (if Jemalloc support)
```

### Big Key Detection and Handling

Big keys (large strings, huge hashes/sets) cause:
- Slow operations (HGETALL on a hash with 1M fields)
- Blocking during BGSAVE/AOF rewrite (fork copies entire memory)
- Network congestion (returning 100MB to client)

```bash
# Detect big keys
redis-cli --bigkeys

# Output example:
# Biggest string  found: 'product:12345' has 5.2 MB
# Biggest hash   found: 'user:12345:transactions' has 15462 fields
# Biggest set    found: 'page:59291:visitors' has 890123 members

# Alternative: Scan for large keys manually
redis-cli --scan --pattern '*' | while read key; do
    size=$(redis-cli MEMORY USAGE "$key")
    if [ "$size" -gt 1048576 ]; then  # >1MB
        echo "Large key: $key ($size bytes)"
    fi
done
```

**Handling strategies:**

```java
// 1. Split big hash into sharded hashes
// Instead of: user:12345:favorites (50,000 items)
// Use:        user:12345:favorites:1, user:12345:favorites:2, ...

// 2. Use Sorted Set with ZRANGE instead of Hash with HGETALL

// 3. Compress large string values
@Service
public class CompressedRedisService {

    public void setCompressed(String key, String value) {
        try {
            byte[] compressed = compress(value);
            redisTemplate.opsForValue().set(key, compressed);
        } catch (IOException e) {
            throw new RuntimeException("Compression failed", e);
        }
    }

    public String getCompressed(String key) {
        byte[] data = (byte[]) redisTemplate.opsForValue().get(key);
        if (data == null) return null;
        try {
            return decompress(data);
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("Decompression failed", e);
        }
    }

    private byte[] compress(String str) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(baos)) {
            gzip.write(str.getBytes(StandardCharsets.UTF_8));
        }
        return baos.toByteArray();
    }

    private String decompress(byte[] data) throws IOException {
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(data))) {
            return new String(gzip.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
```

### Hot Key Detection and Resolution

**Detection:**
```bash
# 1. Redis Monitor (dev only — avoid in production)
redis-cli monitor | head -1000

# 2. INFO commandstats (aggregated)
redis-cli INFO commandstats

# 3. Redis 7.0+ — Keyspace notification for hot keys
# Enable: CONFIG SET notify-keyspace-events K

# 4. Application-level counter
```

**Resolution strategies:**

```java
// Strategy 1: Local cache + Redis (multi-level)
// Strategy 2: Replica reads (spread hot reads across replicas)
// Strategy 3: Key prefix splitting (shard the hot key)

// Strategy 1 implementation:
@Service
public class HotKeyResolver {

    private final Cache<String, Object> localCache = Caffeine.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(Duration.ofSeconds(5))
        .build();

    @Autowired
    private RedisTemplate<String, Object> redis;

    // Read through local cache first, then Redis
    public Object getHotKey(String key) {
        // L1: Local cache
        Object local = localCache.getIfPresent(key);
        if (local != null) return local;

        // L2: Redis
        Object redisVal = redis.opsForValue().get(key);
        if (redisVal != null) {
            localCache.put(key, redisVal);
        }
        return redisVal;
    }

    // Strategy 3: Key splitting (shard hot key into N sub-keys)
    // Instead of one hot key "product:12345" which handles 100K QPS
    // Use N replicas "product:12345:{0..9}"
    private static final int SHARD_COUNT = 10;

    public Object getHotKeySharded(String baseKey) {
        int shard = ThreadLocalRandom.current().nextInt(SHARD_COUNT);
        String shardedKey = baseKey + ":{" + shard + "}";
        return redis.opsForValue().get(shardedKey);
    }

    // Write must update ALL shards
    public void setHotKeySharded(String baseKey, Object value, long ttl) {
        for (int i = 0; i < SHARD_COUNT; i++) {
            String shardedKey = baseKey + ":{" + i + "}";
            redis.opsForValue().set(shardedKey, value, Duration.ofSeconds(ttl));
        }
    }
}
```

### Slow Query Log

```bash
# Configure slow log
CONFIG SET slowlog-log-slower-than 10000  # Log queries > 10ms (microseconds)
CONFIG SET slowlog-max-len 128            # Keep last 128 entries

# View slow queries
SLOWLOG GET 10                            # Last 10 slow queries
SLOWLOG LEN                               # Number of slow queries
SLOWLOG RESET                             # Clear slow log

# Common slow commands:
# KEYS *               — NEVER use in production (SCAN instead)
# SORT key             — Can be slow for large lists
# HGETALL big_hash     — O(N) where N = number of fields
# SMEMBERS big_set     — O(N) where N = members
# LRANGE list 0 -1     — O(N) for large lists

# Instead of SLOWLOG, use Grafana + Redis Exporter for production
```

### Production Monitoring Checklist

```yaml
# Prometheus Redis Exporter metrics:
redis_up                            # Is Redis reachable?
redis_memory_used_bytes             # Memory usage
redis_memory_max_bytes              # Max memory
redis_cpu_sys_seconds_total         # CPU usage
redis_connected_clients             # Client connections
redis_db_keys                       # Key count per DB
redis_commands_duration_seconds_total # Command execution time
redis_net_input_bytes_total         # Network throughput
redis_hit_ratio                     # Cache hit rate (keyspace_hits / (hits + misses))

# Key Metrics to Watch:
# - Memory: Alert at > 80% maxmemory
# - Hit rate: Alert at < 80%
# - Connected clients: Alert at > 80% of max
# - Replication lag: Alert at > 10 seconds
# - Slow queries: Alert if > 10 per minute
# - Evicted keys: Alert on any evictions (if not intended)
```

---

## Interview Questions

### Basic

1. **"What data structures does Redis support?"**
   - String, Hash, List, Set, Sorted Set, Stream, Geospatial, Bitmap, HyperLogLog

2. **"How does Redis persistence work? RDB vs AOF?"**
   - RDB: Point-in-time snapshots, compact, fast recovery, data loss between snapshots
   - AOF: Append-only log, configurable fsync (everysec recommended), slower recovery
   - Hybrid (4.0+): RDB + AOF during rewrite for best of both

3. **"What is Redis' eviction policy?"**
   - allkeys-lru (most common), allkeys-lfu, volatile-lru, volatile-ttl, noeviction

### Intermediate

4. **"How do you implement a distributed lock with Redis?"**
   - SET key value NX PX (SET if Not eXists with expiry)
   - Release: Lua script (GET + DEL atomic)
   - Redisson: RLock with watchdog for auto-renewal
   - Redlock: Consensus across N Redis nodes

5. **"Explain cache penetration, avalanche, and breakdown."**
   - Penetration: Non-existent data, use Bloom filter or cache null
   - Avalanche: Mass key expiration, use random TTL, multi-level cache
   - Breakdown: Hot key expires under concurrent load, use mutex or never-expire

6. **"What is the difference between Redis Cluster and Sentinel?"**
   - Sentinel: HA only (master failover), all nodes have full data
   - Cluster: HA + sharding (data split across nodes), 16384 hash slots

### Advanced

7. **"How does single-threaded Redis handle many concurrent connections?"**
   - Event loop with epoll/kqueue (IO multiplexing)
   - All operations are in-memory and fast (no context switching)
   - 6.0+ IO threads handle socket read/write, core logic still single-threaded

8. **"Design a rate limiter using Redis."**
   - Fixed window: INCR + EXPIRE, simple but allows bursts at boundaries
   - Sliding window: ZSet with timestamps as scores, ZREMRANGEBYSCORE + ZCOUNT
   - Token bucket: Lua script with last-refill-time and token count
   - Redis Cell module (4.0+): CL.THROTTLE command

9. **"How would you migrate a Redis instance to a new cluster without downtime?"**
   - Setup replication from old to new (SLAVEOF)
   - Wait for sync (check replication offset)
   - Cutover: update app config → new cluster
   - Or: use proxy (Twemproxy) for transparent migration

10. **"What are the pros and cons of Redlock?"**
    - Pros: Multi-node consensus, tolerates N/2-1 node failures
    - Cons: Clock drift assumptions, no fencing tokens, debated by experts
    - Alternative: Use ZooKeeper or etcd for strongly consistent locking

---

## Summary

| Topic | Key Takeaway |
|---|---|
| **Data structures** | 9 types, each with specific O-complexity and use cases |
| **Persistence** | Hybrid RDB+AOF for production; RDB for backups |
| **Caching** | Cache-Aside most common; protect against penetration/avalanche/breakdown |
| **Distributed locking** | Redisson RLock with watchdog; never implement from scratch |
| **Cluster** | Auto sharding with 16384 slots; MOVED redirection |
| **Performance** | Pipeline > individual commands; local cache > Redis for hot keys |
| **Monitoring** | Memory, hit rate, slow log, replication lag, eviction rate |
