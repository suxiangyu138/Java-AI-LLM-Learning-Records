# Redis 核心命令大全（Java 后端企业级实战版）

> **文档定位**：Java 后端企业级技术文档 | Redis 命令速查  
> **版本**：Redis 7.x/8.x  
> **核心场景**：缓存、计数器、分布式锁、消息队列、排行榜

---

## 一、核心概念

### 1.1 五大核心数据类型

| 类型 | 特点 | 企业级场景 |
|------|------|------------|
| **String** | 字符串/二进制，最大 512MB | 缓存、计数器、分布式锁 |
| **Hash** | 键值对集合，类似 Java Map | 存储对象（用户信息、商品信息） |
| **List** | 有序可重复，双向链表 | 消息队列、时间线 |
| **Set** | 无序不重复 | 标签、好友列表、去重 |
| **Sorted Set** | 按分数排序，成员不重复 | 排行榜、优先级队列 |

### 1.2 键设计规范

```
业务模块:数据类型:唯一标识
user:info:100    product:info:1001    hot:search:rank
```

---

## 二、底层原理

### 2.1 Redis 单线程模型

Redis 主线程为**单线程**执行命令，避免阻塞主线程：
- 禁止 `KEYS *`（用 `SCAN` 替代）
- 禁止 `FLUSHALL`/`FLUSHDB`（生产环境禁用）
- 大 Hash/Set 不用 `HGETALL`/`SMEMBERS`（用 `HSCAN`/`SSCAN`）

---

## 三、代码实现（核心命令）

### 3.1 通用命令

| 命令 | 说明 | 注意事项 |
|------|------|----------|
| `KEYS <pattern>` | 查询匹配的键 | 生产禁用，用 `SCAN` |
| `EXISTS <key>` | 判断键是否存在 | 返回 1/0 |
| `DEL <key>` | 删除键 | 支持多个 |
| `EXPIRE <key> <seconds>` | 设置过期（秒） | `PEXPIRE` 毫秒版 |
| `TTL <key>` | 查看剩余过期 | -1:永不过期, -2:不存在 |
| `PERSIST <key>` | 移除过期 | 变为永不过期 |
| `TYPE <key>` | 查看数据类型 | string/hash/list/set/zset |
| `PING` | 测试连接 | 返回 PONG |
| `SELECT <dbid>` | 切换数据库 | 0-15，默认 0 号 |

```bash
# 排查用 SCAN 替代 KEYS
SCAN 0 MATCH product:* COUNT 100
```

### 3.2 String 命令（最常用）

| 命令 | 说明 | 场景 |
|------|------|------|
| `SET key value [EX s] [NX\|XX]` | 设置键值 | 通用缓存 + 分布式锁 |
| `GET key` | 获取值 | — |
| `SETNX key value` | 不存在才设置 | **分布式锁** |
| `SETEX key s value` | 设置并过期 | Token 缓存 |
| `INCR key` | 原子递增 1 | **计数器** |
| `INCRBY key n` | 原子递增 n | 阅读量 |
| `DECR / DECRBY` | 原子递减 | 库存扣减 |
| `APPEND key value` | 追加字符串 | — |
| `STRLEN key` | 字符串长度 | — |
| `GETSET key value` | 设新值返旧值 | — |

```bash
# 分布式锁
SET lock:order:1001 "1" NX EX 30

# 文章阅读量递增
INCR article:read:1001

# 缓存 + 自动过期
SETEX user:token:100 86400 "abc123xyz"
```

### 3.3 Hash 命令（存储对象）

| 命令 | 说明 |
|------|------|
| `HSET key field value` | 设置字段 |
| `HGET key field` | 获取字段 |
| `HGETALL key` | 获取所有字段（大数据慎用） |
| `HDEL key field` | 删除字段 |
| `HEXISTS key field` | 判断字段存在 |
| `HKEYS key` | 所有字段名 |
| `HVALS key` | 所有字段值 |
| `HLEN key` | 字段数量 |
| `HINCRBY key field n` | 字段原子递增 |

```bash
# 存储用户信息
HSET user:info:100 username "zhangsan" age 25 gender "male"
HGET user:info:100 username
HINCRBY user:info:100 score 10
```

### 3.4 List 命令（队列/栈）

| 命令 | 说明 | 场景 |
|------|------|------|
| `LPUSH / RPUSH` | 左/右插入 | 入队 |
| `LPOP / RPOP` | 左/右弹出 | 出队 |
| `LRANGE key 0 -1` | 获取所有元素 | 查看队列 |
| `LLEN key` | 列表长度 | — |
| **`BRPOP key timeout`** | 阻塞弹出 | **消息队列** |
| `LREM key count value` | 删除指定元素 | — |

```bash
# 消息队列
LPUSH msg:queue "task1" "task2"
BRPOP msg:queue 10  # 阻塞等待10秒
```

### 3.5 Set 命令（标签/去重）

| 命令 | 说明 | 场景 |
|------|------|------|
| `SADD key member` | 添加成员 | 添加标签 |
| `SREM key member` | 删除成员 | — |
| `SMEMBERS key` | 所有成员（大数据慎用） | — |
| `SISMEMBER key member` | 判断成员存在 | 权限判断 |
| `SCARD key` | 成员数量 | — |
| `SINTER key1 key2` | **交集** | 共同好友 |
| `SUNION key1 key2` | **并集** | 全部标签 |
| `SDIFF key1 key2` | **差集** | 独有标签 |

```bash
SADD user:tag:100 "java" "redis" "mysql"
SISMEMBER user:tag:100 "java"  # 1 存在
SINTER user:tag:100 user:tag:101  # 共同标签
```

### 3.6 Sorted Set 命令（排行榜）

| 命令 | 说明 | 场景 |
|------|------|------|
| `ZADD key score member` | 添加（更新分数） | 热搜录入 |
| `ZREM key member` | 删除成员 | — |
| `ZREVRANGE key 0 N WITHSCORES` | 降序前 N | **排行榜 TopN** |
| `ZSCORE key member` | 获取分数 | — |
| `ZINCRBY key n member` | 分数递增 | **点击量递增** |
| `ZREVRANK key member` | 降序排名 | 查排名 |
| `ZCARD key` | 成员数量 | — |

```bash
# 热搜排行榜
ZADD hot:search:rank 100 "Redis" 80 "Java"
ZINCRBY hot:search:rank 5 "Redis"    # Redis 热度 +5
ZREVRANGE hot:search:rank 0 2 WITHSCORES  # Top 3
ZREVRANK hot:search:rank "Redis"     # Redis 排名
```

---

## 四、实战要点

| 要点 | 说明 |
|------|------|
| **原子命令优先** | 计数器/锁用 `INCR`/`SETNX`/`ZINCRBY`，避免 Lua 脚本复杂度 |
| **键设计规范** | 统一格式 `业务:类型:ID`，避免冲突和过长 |
| **过期时间合理** | 根据更新频率设置，核心数据"永不过期+主动更新" |
| **批量操作** | 用 `HSET`/`SADD` 批量参数，减少网络往返 |

---

## 五、避坑总结

| 坑点 | 正确做法 |
|------|----------|
| **`KEYS *` 阻塞生产** | 用 `SCAN` 游标分批遍历 |
| **`FLUSHALL` 误操作** | `redis.conf` 禁掉危险命令 |
| **`HGETALL` 大 Hash** | 用 `HSCAN` 分批 |
| **过期时间过短** | 缓存穿透风险增加，设置合理 TTL |
| **键命名随意** | 统一规范，避免冲突和键过长 |

---

## 六、企业级最佳实践

- **生产禁用危险命令**：`redis.conf` 中 rename `FLUSHALL`/`FLUSHDB`/`KEYS` 为空
- **连接池配置**：Jedis/Lettuce 连接池，避免每次新建连接
- **缓存策略**：旁路缓存（Cache-Aside），先查缓存 → 查 DB → 回写缓存
- **分布式锁**：`SET key value NX EX 30`，注意锁续期和释放（Redisson）
- **持久化**：RDB 定期快照 + AOF 实时追加，生产建议同时开启
