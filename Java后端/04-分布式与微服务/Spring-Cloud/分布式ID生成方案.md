# 分布式 ID 生成方案

## 为什么需要分布式 ID

单库自增 ID 在分库分表或多服务场景下会重复。分布式 ID 需要满足：**全局唯一** + **趋势递增**（便于索引）。

## 方案一：雪花算法（Snowflake）

Twitter 开源，最常用的方案。64 bit Long 型：

```
0 | 41位时间戳 | 5位机房ID | 5位机器ID | 12位序列号
```

- 41 位时间戳：约 69 年
- 10 位工作机器 ID：支持 1024 台机器
- 12 位序列号：每毫秒 4096 个 ID

### MyBatis-Plus 内置（推荐）

```java
@Configuration
public class IdConfig {
    @Bean
    public IdentifierGenerator idGenerator() {
        return new DefaultIdentifierGenerator();  // 雪花算法
    }
}

@Entity
public class Order {
    @TableId(type = IdType.ASSIGN_ID)  // 自动分配雪花 ID
    private Long id;
}
```

### Hutool 版本

```java
long id = IdUtil.getSnowflake(1, 2).nextId();
```

### 时钟回拨问题

雪花算法依赖机器时钟，时钟回拨会导致 ID 重复。MyBatis-Plus 的 DefaultIdentifierGenerator 使用了序列号缓存的机制处理小幅回拨。严重的时钟回拨会抛异常等待时钟追上。

## 方案二：美团 Leaf

两种模式：
- **号段模式**：每次从数据库取一段 ID（如 1-1000），用完再取。性能高但有号段浪费。
- **雪花模式**：基于雪花算法改进，用 ZooKeeper 持久节点 + 时间偏移解决时钟回拨。

## 方案三：数据库号段

```sql
CREATE TABLE id_alloc (
    biz_tag VARCHAR(32) PRIMARY KEY,
    max_id  BIGINT NOT NULL,
    step    INT NOT NULL DEFAULT 1000
);

-- 获取号段（事务保证）
UPDATE id_alloc SET max_id = max_id + step WHERE biz_tag = 'order';
SELECT max_id - step, max_id FROM id_alloc WHERE biz_tag = 'order';
```

适合中小规模项目。

## 方案四：Redis 生成

利用 `INCR` 原子操作：
```java
long id = redisTemplate.opsForValue().increment("id:order", 1);
```

简单但依赖 Redis 持久化，重启后可能重复（需 AOF 持久化）。

## 方案对比

| 方案 | 性能 | 依赖 | 趋势递增 | 适用场景 |
|------|------|------|----------|----------|
| 雪花算法 | 极高（本地生成） | 无外部依赖 | 是 | 绝大多数场景 |
| 美团 Leaf | 高 | DB / ZK | 是 | 大厂自建 |
| DB 号段 | 中 | 数据库 | 严格递增 | 小型项目 |
| Redis INCR | 中 | Redis | 严格递增 | 简单场景 |
| UUID | 极高 | 无 | 否 | 不适合主键 |

## 实战建议

- **直接用 MyBatis-Plus 的雪花算法**：`IdType.ASSIGN_ID`，零配置
- **不要用 UUID 做主键**：非递增，B+Tree 索引会频繁分裂，性能差
- **不要以为雪花 ID 能排序**：毫秒内生成的 ID 顺序不严格保证，需要精确排序请加 `create_time` 字段
- **前端用 Long 接收时注意 JS 精度丢失**：JS 安全整数最大 `2^53-1`，雪花 ID 可能超出。解决方案：序列化时转为 String
