# Redis 使用教程

> **定位**：Redis 快速入门，覆盖安装、五种核心数据结构、持久化、Java 集成。

---

## 1. Redis 是什么

基于内存的键值型数据库，读写极快。常用场景：缓存、限流、分布式锁、消息队列、会话存储。

---

## 2. 安装与启动

```bash
# Linux
wget https://download.redis.io/releases/redis-7.0.10.tar.gz
tar -zxvf redis-7.0.10.tar.gz && cd redis-7.0.10
make && make install PREFIX=/usr/local/redis

# 配置守护进程 + 密码
daemonize yes
requirepass 你的密码
bind 0.0.0.0

# 启动
/usr/local/redis/bin/redis-server /usr/local/redis/redis.conf
/usr/local/redis/bin/redis-cli -a 密码
```

### 常用配置

| 配置 | 说明 |
|------|------|
| `bind` | 绑定 IP，生产不用 `0.0.0.0` |
| `requirepass` | 密码，生产必设 |
| `maxmemory` | 最大内存 |
| `appendonly` | AOF 持久化开关 |

---

## 3. 五种核心数据结构

### String

```bash
SET key value         # 设置
SET key value EX 10   # 带过期时间
GET key               # 获取
INCR key / DECR key   # 自增/自减
```

### Hash

```bash
HSET key field value         # 设置字段
HGET key field               # 获取字段
HGETALL key                  # 获取所有
HDEL key field               # 删除字段
```

### List

```bash
LPUSH key v1 v2     # 左插
RPUSH key v1 v2     # 右插
LPOP key / RPOP key # 弹出
LRANGE key 0 -1     # 查看全部
LLEN key            # 长度
```

### Set

```bash
SADD key v1 v2       # 添加
SMEMBERS key         # 查看全部
SISMEMBER key value  # 判断存在
SINTER key1 key2     # 交集
SUNION key1 key2     # 并集
```

### ZSet

```bash
ZADD key score1 v1 score2 v2  # 添加
ZRANGE key 0 -1               # 升序
ZREVRANGE key 0 -1            # 降序
ZSCORE key value              # 获取分数
ZREM key value                # 删除
```

---

## 4. 持久化

| 方式 | 优点 | 缺点 |
|------|------|------|
| **RDB** | 恢复快、文件小 | 可能丢失最后快照后数据 |
| **AOF** | 数据安全性高 | 文件大、恢复慢 |

> 生产环境推荐 RDB + AOF 组合。

---

## 5. Java 集成（Spring Boot）

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

```yaml
spring:
  redis:
    host: 127.0.0.1
    port: 6379
    password: 密码
```

```java
@Autowired
private StringRedisTemplate redisTemplate;
// opsForValue / opsForHash / opsForList / opsForSet / opsForZSet
```

---

## 6. 常见应用场景

| 场景 | 数据结构 |
|------|----------|
| 缓存 | String/Hash |
| 分布式锁 | String（SET NX EX） |
| 限流 | String（INCR + 过期） |
| 会话存储 | String/Hash |
| 排行榜 | ZSet |
| 消息队列 | List（LPUSH/BRPOP） |
| 去重 | Set |
| 延时任务 | ZSet（Score = 时间戳） |
