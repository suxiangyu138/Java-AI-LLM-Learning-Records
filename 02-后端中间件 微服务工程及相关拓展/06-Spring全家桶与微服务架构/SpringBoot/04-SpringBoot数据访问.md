# 04 - Spring Boot 数据访问

> 定位：Spring Data JPA 与 MyBatis 双框架、事务管理、连接池（HikariCP）、多数据源、Spring Data Redis——数据访问全解

## 📚 目录

1. [数据访问框架选型](#1-数据访问框架选型)
2. [Spring Data JPA](#2-spring-data-jpa)
3. [MyBatis](#3-mybatis)
4. [事务管理](#4-事务管理)
5. [连接池 HikariCP](#5-连接池-hikaricp)
6. [多数据源与 Redis](#6-多数据源与-redis)

---

## 1. 数据访问框架选型

| 框架 | 特点 | 适用 |
|------|------|------|
| Spring Data JPA | 对象映射、自动 CRUD、简单 | 快速开发、领域驱动 |
| MyBatis | SQL 可控、灵活、团队熟悉 | 复杂 SQL、国内主流 |
| JdbcTemplate | 轻量、手写 SQL | 简单场景 |

```
⚠️ 面试必答：
"JPA 对象思维（快）、MyBatis SQL 思维（可控）——
 国内互联网 MyBatis 主流、欧洲 JPA 主流；
 两者都基于 Spring 事务抽象。"
```

---

## 2. Spring Data JPA

### 2.1 实体与仓库

```java
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;
    // getter/setter
}

// ⚠️ 接口继承即自动实现（方法名推导 SQL）
public interface UserRepository extends JpaRepository<User, Long> {
    List<User> findByName(String name);               // 按名查
    List<User> findByAgeGreaterThan(int age);         // 大于
    List<User> findByNameAndAge(String name, int age);// 多条件
    boolean existsByEmail(String email);
    long countByStatus(int status);

    @Query("select u from User u where u.name like :kw")
    List<User> search(@Param("kw") String keyword);   // 自定义 JPQL
}
```

### 2.2 JPA 核心

```
⚠️ 方法名推导规则：
  findBy + 字段 + 条件（And/Or/GreaterThan/Like/In）

⚠️ 面试必答：
"JPA 三能力——方法名推导 SQL、
 @Query 自定义 JPQL/原生 SQL、
 Specification 动态查询；
 复杂查询用 @Query、动态条件用 Specification。"
```

---

## 3. MyBatis

### 3.1 注解与 XML

```java
// 注解方式（简单 SQL）
@Mapper
public interface UserMapper {
    @Select("SELECT * FROM users WHERE id = #{id}")
    User findById(Long id);

    @Insert("INSERT INTO users(name) VALUES(#{name})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);
}
```

```xml
<!-- XML 方式（复杂 SQL 推荐） -->
<mapper namespace="com.example.UserMapper">
    <select id="pageQuery" resultType="User">
        SELECT * FROM users
        <where>
            <if test="name != null and name != ''">
                AND name LIKE CONCAT('%', #{name}, '%')
            </if>
            <if test="status != null">
                AND status = #{status}
            </if>
        </where>
        ORDER BY id DESC
    </select>

    <insert id="batchInsert" useGeneratedKeys="true">
        INSERT INTO users(name) VALUES
        <foreach collection="list" item="u" separator=",">
            (#{u.name})
        </foreach>
    </insert>
</mapper>
```

### 3.2 MyBatis 核心要点

```
⚠️ #{} vs ${}（安全第一考点）：
  #{}：预编译参数（✅ 防注入）
  ${}：字符串拼接（❌ 注入风险，仅表名/排序用）

⚠️ 面试必答：
"MyBatis 安全铁律——参数一律 #{}（预编译），
 ${} 只允许表名/排序字段（且白名单校验）；
 动态 SQL 用 <where>/<if>/<foreach>。"
```

---

## 4. 事务管理

### 4.1 声明式事务

```java
@Service
public class OrderService {

    @Transactional                        // ⚠️ 声明式事务（默认回滚 RuntimeException）
    public void createOrder(OrderDTO dto) {
        orderRepo.save(order);
        // 异常 → 自动回滚
    }

    // 高级配置
    @Transactional(
        rollbackFor = Exception.class,     // 所有异常都回滚（默认只回滚运行时）
        isolation = Isolation.READ_COMMITTED,
        propagation = Propagation.REQUIRED,  // 默认：有事务则加入
        timeout = 10                        // 超时秒数
    )
    public void complex() { }
}
```

### 4.2 事务失效场景（面试高频）

| 场景 | 原因 |
|------|------|
| 同类内部调用 | 代理绕过（this.method()） |
| 方法非 public | 代理只拦 public |
| 异常被捕获 | 没抛出去就不回滚 |
| 异常类型不匹配 | 默认只回滚 RuntimeException |
| 自注入循环 | 设计问题 |

```java
// ⚠️ 经典失效：同类调用（this 调用不走代理）
@Service
public class OrderService {
    public void create() {
        this.doCreate();      // ❌ this 调用无事务
    }
    @Transactional
    public void doCreate() { }
}
// 解决：注入自身代理 / 拆分到其他 Service
```

> 🎯 **要点**：事务失效五场景——同类调用、非 public、吞异常、异常类型、自注入。**同类调用不经过代理**是最高频考点。

---

## 5. 连接池 HikariCP

### 5.1 为什么 HikariCP

```
HikariCP = 默认连接池（性能最快、代码精简）
  对比：Druid（阿里，监控全）/ DBCP（历史）

⚠️ 面试必答：
"Boot 默认 HikariCP——性能标杆；
 Druid 的优势是监控（SQL 审计/慢查询统计），
 需要监控可换 Druid。"
```

### 5.2 配置调优

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20        # 最大连接（公式：核数×2 + 磁盘数）
      minimum-idle: 5              # 常驻
      connection-timeout: 30000    # 获取连接超时（30s）
      max-lifetime: 1800000        # 连接最大存活（30min < 数据库等待）
      pool-name: OrderHikariPool
      # 连接泄漏检测
      leak-detection-threshold: 60000   # 60s 未归还告警
```

```
⚠️ 池大小经验：
  池大小 = (核数 × 2) + 有效磁盘数
  过度设大反而慢（上下文切换）
  max-lifetime 必须小于数据库 wait_timeout
```

---

## 6. 多数据源与 Redis

### 6.1 多数据源

```java
// ⚠️ 多数据源：一个为主、其他为读（读写分离应用层实现）
@Configuration
public class DataSourceConfig {

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource.master")
    public DataSource masterDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    @ConfigurationProperties("spring.datasource.slave")
    public DataSource slaveDataSource() {
        return DataSourceBuilder.create().build();
    }
}
// ⚠️ 或使用 @DS("slave") 注解（dynamic-datasource 库）
// @DS("slave") public User getById() { }   ← 读写分离注解式
```

### 6.2 Spring Data Redis

```java
// 配置
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password: ${REDIS_PASSWORD}
      lettuce:
        pool:
          max-active: 16           # 连接池

// 使用（StringRedisTemplate / RedisTemplate）
@Service
public class CacheService {
    private final StringRedisTemplate redis;

    public CacheService(StringRedisTemplate redis) { this.redis = redis; }

    public void set(String key, String value, long ttlSeconds) {
        redis.opsForValue().set(key, value, ttlSeconds, TimeUnit.SECONDS);
    }

    public String get(String key) {
        return redis.opsForValue().get(key);
    }

    // 分布式锁（Redis 经典场景）
    public boolean tryLock(String key, long timeoutSeconds) {
        Boolean ok = redis.opsForValue()
            .setIfAbsent(key, "1", timeoutSeconds, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(ok);   // ⚠️ 防拆箱 NPE
    }
}
```

> 🎯 **要点**：Redis 集成 = Spring Data Redis（StringRedisTemplate）+ 连接池 + 分布式锁（setIfAbsent + 过期）。缓存/锁/会话三大场景是微服务标配。

---

> 🎯 **核心要点**：数据访问体系 = **框架选型**（JPA vs MyBatis）+ **JPA**（方法名推导 + @Query）+ **MyBatis**（#{} 防注入 + 动态 SQL）+ **事务**（声明式 + 失效五场景）+ **HikariCP**（池大小公式 + 泄漏检测）+ **多数据源/Redis**（@DS 读写分离 + 分布式锁）。"#{} vs ${}"与"事务失效"是两大必考。

---

**返回总览**：[00-SpringBoot总览与核心概念](00-SpringBoot总览与核心概念.md) | **上一篇**：[03-SpringBootWeb开发](03-SpringBootWeb开发.md) | **下一篇**：[05-SpringBoot测试体系](05-SpringBoot测试体系.md)
