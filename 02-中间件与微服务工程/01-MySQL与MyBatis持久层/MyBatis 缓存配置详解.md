# MyBatis 缓存配置详解

> **定位**：MyBatis 缓存机制是优化数据库访问性能的核心手段。分为**一级缓存**（SqlSession 级别，默认开启）和**二级缓存**（Mapper 级别，需手动配置）。

---

## 目录

1. [缓存层级对比](#1-缓存层级对比)
2. [一级缓存](#2-一级缓存)
3. [二级缓存](#3-二级缓存)
4. [第三方缓存集成（Redis）](#4-第三方缓存集成redis)
5. [注意事项与使用原则](#5-注意事项与使用原则)

---

## 1. 缓存层级对比

| 维度 | 一级缓存 | 二级缓存 |
|------|----------|----------|
| **作用域** | SqlSession 级别 | Mapper（namespace）级别 |
| **默认状态** | ✅ 自动开启 | ❌ 需手动配置 |
| **跨 SqlSession** | ❌ 不共享 | ✅ 共享 |
| **清空时机** | insert/update/delete / close | insert/update/delete / 手动清空 / 过期 |
| **适用场景** | 单会话内重复查询 | 多会话共享、读多写少 |

---

## 2. 一级缓存

> SqlSession 级别，默认开启，无需任何配置。

### 2.1 命中与失效

| 行为 | 说明 |
|------|------|
| ✅ **命中** | 同一 SqlSession、相同 SQL + 参数 |
| ❌ **失效** | `insert/update/delete` 操作后 |
| ❌ **失效** | `sqlSession.clearCache()` 手动清空 |
| ❌ **失效** | `sqlSession.close()` |

### 2.2 示例

```java
try (SqlSession session = sqlSessionFactory.openSession()) {
    UserMapper mapper = session.getMapper(UserMapper.class);

    User u1 = mapper.selectById(1);  // 走数据库
    User u2 = mapper.selectById(1);  // 从缓存获取
    System.out.println(u1 == u2);    // true（同一对象）

    mapper.updateName(1, "newName"); // update 操作 → 清空缓存
    User u3 = mapper.selectById(1);  // 重新走数据库
    System.out.println(u1 == u3);    // false
}
```

### 2.3 注意事项

| 注意 | 说明 |
|------|------|
| 无法跨 SqlSession | 多个 SqlSession 执行相同查询仍多次访问 DB |
| 避免长会话 | Web 项目中未及时关闭的 SqlSession 可能导致脏数据 |

---

## 3. 二级缓存

> Mapper 级别，多个 SqlSession 共享，需三步配置。

### 3.1 三步开启

**第一步：全局开关**（mybatis-config.xml）

```xml
<settings>
    <setting name="cacheEnabled" value="true"/>
</settings>
```

**第二步：Mapper 中开启**

```xml
<!-- XML 方式 -->
<mapper namespace="com.example.mapper.UserMapper">
    <cache/>
    ...
</mapper>
```

```java
// 注解方式
@CacheNamespace
public interface UserMapper {
    @Select("SELECT * FROM user WHERE id = #{id}")
    User selectById(Integer id);
}
```

**第三步：实体类序列化**

```java
public class User implements Serializable { ... }  // ⚠️ 必须！
```

### 3.2 `<cache>` 高级属性

```xml
<cache
    eviction="LRU"          <!-- 回收策略：LRU/FIFO/SOFT/WEAK -->
    flushInterval="60000"   <!-- 刷新间隔（毫秒） -->
    size="1024"             <!-- 最大缓存对象数 -->
    readOnly="false"/>      <!-- true=只读(性能高) / false=可读写(默认) -->
```

| 回收策略 | 说明 |
|----------|------|
| **LRU**（默认） | 移除最长时间未使用的对象 |
| **FIFO** | 按缓存添加顺序移除最早对象 |
| SOFT | JVM 软引用，内存不足时移除 |
| WEAK | JVM 弱引用，GC 即移除 |

---

## 4. 第三方缓存集成（Redis）

> 默认二级缓存为内存缓存，重启丢失且分布式不共享。生产环境推荐 Redis。

### 4.1 Maven 依赖

```xml
<dependency>
    <groupId>org.mybatis.caches</groupId>
    <artifactId>mybatis-redis</artifactId>
    <version>1.0.0-beta2</version>
</dependency>
```

### 4.2 Mapper 中使用

```xml
<mapper namespace="com.example.mapper.UserMapper">
    <cache type="org.mybatis.caches.redis.RedisCache"/>
    ...
</mapper>
```

### 4.3 自定义 Redis 连接

> 在 `resources/redis.properties` 中：

```properties
redis.host=192.168.1.100
redis.port=6379
redis.password=123456
redis.timeout=3000
redis.database=0
```

---

## 5. 注意事项与使用原则

### 5.1 禁用缓存场景

| 场景 | 原因 |
|------|------|
| 高频更新数据（订单、库存） | 缓存频繁失效，反而增加开销 |
| 带随机函数/当前时间的 SQL | 结果不固定，无缓存意义 |

### 5.2 单个 SQL 禁用缓存

```xml
<!-- XML -->
<select id="selectById" resultType="user" useCache="false">...</select>
```

```java
// 注解
@Select("SELECT * FROM user WHERE id = #{id}")
@Options(useCache = false)
User selectById(Integer id);
```

### 5.3 核心原则

| 原则 | 说明 |
|------|------|
| **读多写少才用** | 商品详情、字典数据 ✅ |
| **分布式必用 Redis** | 默认内存缓存无法跨节点共享 |
| **实体类必须序列化** | 二级缓存依赖 `Serializable` |
| **一致性优先于性能** | 先保证数据正确，再追求性能优化 |

---

> 🎯 **总结**：一级缓存默认开启、无需配置；二级缓存需三步（全局开 + Mapper 开 + 序列化）；生产环境分布式 → 集成 Redis。核心原则：**空间换时间，读多写少才用，数据一致性优先。**
