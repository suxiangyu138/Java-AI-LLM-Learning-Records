# 04 - MyBatis 缓存机制

> 🎯 MyBatis 的两级缓存是面试高频考点——一级缓存（SqlSession 级别，默认开启）和二级缓存（Mapper 级别，需手动开启）。理解缓存的生效条件和失效场景，是解决"数据不一致"Bug 的关键

---

## 目录

1. [一级缓存（本地缓存）](#1-一级缓存本地缓存)
2. [二级缓存（全局缓存）](#2-二级缓存全局缓存)
3. [自定义缓存](#3-自定义缓存)

---

## 1. 一级缓存（本地缓存）

```text
一级缓存 = SqlSession 级别的缓存（默认开启，无法关闭）

作用范围：同一个 SqlSession 内
生命周期：SqlSession 关闭时清空
失效条件：
  1. 不同的 SqlSession
  2. 同一个 SqlSession 内执行了 INSERT/UPDATE/DELETE（清空全部缓存）
  3. 手动清空：sqlSession.clearCache()
  4. 查询语句不同、参数不同
```

```java
// 一级缓存演示
try (SqlSession session = sqlSessionFactory.openSession()) {
    UserMapper mapper = session.getMapper(UserMapper.class);

    User u1 = mapper.findById(1L);  // SELECT * FROM users WHERE id = 1
    User u2 = mapper.findById(1L);  // 走缓存！不发 SQL！
    System.out.println(u1 == u2);   // true（同一对象引用）

    // 执行更新 → 清空一级缓存
    mapper.updateName(1L, "Bob");   // UPDATE ...

    User u3 = mapper.findById(1L);  // 重新查！再发 SQL
    System.out.println(u1 == u3);   // false
}
```

## 2. 二级缓存（全局缓存）

```text
二级缓存 = Mapper 级别（namespace 维度），跨 SqlSession

开启条件：
  1. mybatis-config.xml: <setting name="cacheEnabled" value="true"/>
  2. Mapper XML: <cache/> 或 <cache-ref namespace="..."/>
  3. 实体类实现 Serializable（二级缓存需要序列化）

执行流程：
  查询请求 → 二级缓存(命中返回) → 一级缓存(命中返回) → 数据库
```

```xml
<!-- mybatis-config.xml -->
<settings>
    <setting name="cacheEnabled" value="true"/>
</settings>

<!-- UserMapper.xml -->
<mapper namespace="com.example.UserMapper">
    <cache
        eviction="LRU"               <!-- 淘汰策略：LRU/FIFO/SOFT/WEAK -->
        flushInterval="60000"        <!-- 刷新间隔（ms） -->
        size="1024"                  <!-- 最大缓存对象数 -->
        readOnly="true"              <!-- 只读=True 性能更好 -->
    />
    <select id="findById" resultType="User" useCache="true">
        SELECT * FROM users WHERE id = #{id}
    </select>
</mapper>
```

### 二级缓存注意事项

```text
⚠️ 二级缓存的坑：

1. 多表关联时缓存不一致
   UserMapper 有二级缓存，OrderMapper 没有
   → 更新 Order 不会清空 User 的缓存
   → 缓存和数据库不一致！

   解决：<cache-ref namespace="com.example.OrderMapper"/>
        让 OrderMapper 共享 UserMapper 的缓存

2. 分布式环境
   二级缓存是本地缓存 → 多实例间不一致
   解决：用 Redis 做分布式缓存替代二级缓存

3. 细粒度控制
   useCache="false" → 单条查询不使用二级缓存
   flushCache="true" → 该操作执行后清空缓存
```

## 3. 自定义缓存

```java
// 实现 MyBatis Cache 接口 → 用 Redis 替换本地缓存
public class RedisCache implements Cache {
    private final String id;
    private final RedisTemplate<String, Object> redis;

    public RedisCache(String id) {
        this.id = id;
        this.redis = SpringContextHolder.getBean(RedisTemplate.class);
    }

    @Override
    public void putObject(Object key, Object value) {
        redis.opsForValue().set(key.toString(), value, Duration.ofMinutes(30));
    }

    @Override
    public Object getObject(Object key) {
        return redis.opsForValue().get(key.toString());
    }
    // removeObject / clear / getSize ...
}
```

```xml
<!-- 使用自定义缓存 -->
<cache type="com.example.RedisCache"/>
```

## 核心要点回顾

- 一级缓存 = SqlSession 级（默认存在，更新即清空）
- 二级缓存 = Mapper 级（需手动开启 + 实体 Serializable）
- 二级缓存的坑：多表不一致、分布式不同步
- 生产环境建议：关闭二级缓存，用 Redis 做应用层缓存
- 查询顺序：二级 → 一级 → 数据库

## 参考资料

1. MyBatis 缓存文档
2. MyBatis-Redis-Cache 开源项目
