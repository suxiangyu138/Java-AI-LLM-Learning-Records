# 使用 Redis 构建 Web 应用

> **定位**：Redis 作为 Web 应用的性能加速器，解决高并发响应延迟、数据库压力、会话管理三大核心痛点。

---

## 目录

1. [核心价值](#1-核心价值)
2. [核心集成场景](#2-核心集成场景)
3. [最佳实践](#3-最佳实践)

---

## 1. 核心价值

| 痛点 | Redis 方案 |
|------|-----------|
| 响应延迟 | 热点数据缓存，毫秒级→微秒级 |
| DB 压力 | 80% 请求命中缓存，减少 DB 查询 |
| 分布式会话 | Spring Session + Redis 共享 |
| 高并发写 | 原子 INCR/DECR/INCRBY |

---

## 2. 核心集成场景

### 2.1 热点数据缓存

```java
public Product getProductById(Long productId) {
    String key = "product:info:" + productId;
    Product product = (Product) redisTemplate.opsForValue().get(key);
    if (product != null) return product;
    // 未命中 → 查 DB → 写入缓存
    product = productMapper.selectById(productId);
    redisTemplate.opsForValue().set(key, product, 30, TimeUnit.MINUTES);
    return product;
}
```

### 2.2 分布式会话（Spring Session）

```yaml
spring:
  session:
    store-type: redis
    redis:
      namespace: web:session
    timeout: 86400
```

### 2.3 高并发计数器

```java
// 点赞
redisTemplate.opsForValue().increment("article:like:" + articleId, 1);
// 秒杀库存
Long stock = redisTemplate.opsForValue().increment("seckill:stock:" + id, -1);
if (stock < 0) { /* 回滚 */ }
```

### 2.4 简单消息队列

```java
// 生产者
redisTemplate.opsForList().leftPush("email:task:queue", task);
// 消费者（阻塞）
Object task = redisTemplate.opsForList().rightPop("email:task:queue", 1, TimeUnit.SECONDS);
```

### 2.5 排行榜（ZSet）

```java
redisTemplate.opsForZSet().incrementScore("hot:search:rank", keyword, 1);
Set<Object> top10 = redisTemplate.opsForZSet().reverseRange("hot:search:rank", 0, 9);
```

---

## 3. 最佳实践

### 缓存三大问题

| 问题 | 原因 | 方案 |
|------|------|------|
| **雪崩** | 大量缓存同时过期 | 随机过期时间 + 预热 + 降级 |
| **穿透** | 查询不存在的数据 | 空值缓存 + 参数校验 + 布隆过滤器 |
| **击穿** | 热点 key 过期 | 互斥锁 + 永不过期 + 逻辑过期 |

### 其他要点

| 原则 | 说明 |
|------|------|
| Key 设计 | `业务:类型:ID` |
| 数据一致性 | 先更新 DB，再删除缓存 |
| 内存管理 | 设 `maxmemory` + `allkeys-lru` |
| 高可用 | 主从+哨兵 / Redis Cluster |
