# Java 数据库相关核心知识点

> **定位**：Java 数据库编程是后端开发的核心模块，核心是通过 JDBC 规范与数据库交互，结合 ORM 框架、连接池、事务管理等技术，实现高效、安全、可扩展的数据操作。

---

## 目录

1. [基础概念与技术选型](#1-基础概念与技术选型)
2. [JDBC 原生操作](#2-jdbc-原生操作)
3. [MyBatis 核心操作](#3-mybatis-核心操作)
4. [事务管理](#4-事务管理)
5. [连接池与性能优化](#5-连接池与性能优化)
6. [分布式场景拓展](#6-分布式场景拓展)
7. [常见问题与避坑](#7-常见问题与避坑)

---

## 1. 基础概念与技术选型

### 1.1 数据库选型

| 类型 | 产品 | 适用场景 | Java 客户端 |
|------|------|----------|------------|
| 关系型 | **MySQL** | 中小企业首选，开源免费 | JDBC Driver |
| 关系型 | Oracle | 大型企业、高并发 | JDBC Driver |
| 关系型 | SQL Server | 微软生态 | JDBC Driver |
| 缓存 | **Redis** | 缓存首选 | Jedis、Redisson |
| 文档 | MongoDB | 非结构化数据 | MongoTemplate |
| 搜索 | Elasticsearch | 全文检索 | HighLevelRestClient |

### 1.2 Java 技术栈

| 层级 | 技术 | 作用 |
|------|------|------|
| 底层规范 | **JDBC** | Java 访问数据库的标准接口 |
| ORM 框架 | **MyBatis**（半自动）、Hibernate（全自动）、Spring Data JPA | 对象关系映射，简化 CRUD |
| 连接池 | **HikariCP**（Spring Boot 默认）、Druid（阿里）、C3P0 | 管理数据库连接，提升并发性能 |
| 驱动 | `mysql-connector-java` | 数据库厂商提供的 JDBC 实现 |

### 1.3 核心依赖（Maven）

```xml
<!-- MySQL 驱动 -->
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <version>8.0.36</version>
    <scope>runtime</scope>
</dependency>

<!-- MyBatis Spring Boot Starter -->
<dependency>
    <groupId>org.mybatis.spring.boot</groupId>
    <artifactId>mybatis-spring-boot-starter</artifactId>
    <version>3.0.3</version>
</dependency>
```

---

## 2. JDBC 原生操作

> 所有 ORM 框架和连接池都基于 JDBC 封装，需理解原理。

### 2.1 核心步骤

```java
String url = "jdbc:mysql://localhost:3306/test_db?useSSL=false&serverTimezone=UTC";
String username = "root";
String password = "123456";
String sql = "SELECT * FROM user WHERE id = ?";

// try-with-resources 自动关闭资源（Java 7+）
try (Connection conn = DriverManager.getConnection(url, username, password);
     PreparedStatement pstmt = conn.prepareStatement(sql)) {

    pstmt.setInt(1, 1);                              // 绑定参数（索引从 1 开始）
    try (ResultSet rs = pstmt.executeQuery()) {       // 执行查询
        List<User> userList = new ArrayList<>();
        while (rs.next()) {
            User user = new User();
            user.setId(rs.getInt("id"));
            user.setName(rs.getString("name"));
            userList.add(user);
        }
    }
} catch (SQLException e) {
    e.printStackTrace();
}
```

### 2.2 JDBC 注意事项

| 问题 | 说明 |
|------|------|
| 资源关闭 | 按 `ResultSet → Statement → Connection` 顺序，推荐 try-with-resources |
| SQL 注入 | ❌ 字符串拼接 → ✅ `PreparedStatement` 参数化查询 |
| 性能 | 频繁创建/关闭 Connection 开销大 → 必须用连接池 |

---

## 3. MyBatis 核心操作

> 半自动化 ORM，兼顾灵活性与开发效率，是 Java 后端最常用框架。

### 3.1 Spring Boot 配置

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/test_db?useSSL=false&serverTimezone=UTC
    username: root
    password: 123456
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 3000

mybatis:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: com.example.demo.entity
  configuration:
    map-underscore-to-camel-case: true  # 下划线转驼峰
```

### 3.2 核心组件

```java
// Mapper 接口
public interface UserMapper {
    User selectById(@Param("id") Integer id);
    int insert(User user);
    int update(User user);
    int deleteById(@Param("id") Integer id);
}
```

```xml
<!-- Mapper.xml -->
<mapper namespace="com.example.demo.mapper.UserMapper">
    <resultMap id="userMap" type="User">
        <id column="id" property="id"/>
        <result column="name" property="name"/>
    </resultMap>

    <select id="selectById" resultMap="userMap">
        SELECT id, name, age FROM user WHERE id = #{id}
    </select>

    <insert id="insert" parameterType="User">
        INSERT INTO user (name, age) VALUES (#{name}, #{age})
    </insert>

    <update id="update" parameterType="User">
        UPDATE user
        <set>
            <if test="name != null">name = #{name},</if>
            <if test="age != null">age = #{age}</if>
        </set>
        WHERE id = #{id}
    </update>

    <delete id="deleteById">
        DELETE FROM user WHERE id = #{id}
    </delete>
</mapper>
```

```java
// 直接注入使用
@Service
public class UserService {
    @Autowired
    private UserMapper userMapper;

    public User getUserById(Integer id) {
        return userMapper.selectById(id);
    }
}
```

### 3.3 #{} vs ${}

| 方式 | 机制 | SQL 注入 | 场景 |
|------|------|:--------:|------|
| `#{ }` | 参数化查询（PreparedStatement） | ✅ 安全 | 所有值的占位 |
| `${ }` | 直接字符串拼接 | ❌ 危险 | 动态表名/列名（需做白名单校验） |

---

## 4. 事务管理

### 4.1 Spring 声明式事务（推荐）

```java
@Service
public class AccountService {
    @Autowired
    private AccountMapper accountMapper;

    @Transactional(rollbackFor = Exception.class)
    public void transfer(Integer fromId, Integer toId, Integer money) {
        accountMapper.deductBalance(fromId, money);  // 扣减
        // int i = 1 / 0;                            // 模拟异常，自动回滚
        accountMapper.addBalance(toId, money);        // 增加
    }
}
```

> ⚠️ `@Transactional` 必须作用在 `public` 方法上；异常被 `try-catch` 捕获后事务不会回滚。

### 4.2 事务隔离级别

| 级别 | 常量 | 脏读 | 不可重复读 | 幻读 |
|------|------|:---:|:---------:|:---:|
| 读未提交 | `READ_UNCOMMITTED` | ✅ | ✅ | ✅ |
| 读已提交 | `READ_COMMITTED`（MySQL 默认） | ❌ | ✅ | ✅ |
| 可重复读 | `REPEATABLE_READ` | ❌ | ❌ | ✅ |
| 串行化 | `SERIALIZABLE` | ❌ | ❌ | ❌ |

---

## 5. 连接池与性能优化

### 5.1 主流连接池对比

| 连接池 | 特点 | 适用场景 |
|--------|------|----------|
| **HikariCP** | Spring Boot 默认，性能最优，轻量 | 所有项目，尤其高并发 |
| **Druid** | 阿里出品，监控 + 防 SQL 注入 + 统计 | 中大型项目，需监控 |
| C3P0 | 老牌，兼容性好，性能一般 | 老旧项目 |

### 5.2 性能优化三方向

| 方向 | 优化点 |
|------|--------|
| **SQL 优化** | 避免 `SELECT *`、避免全表扫描、用 JOIN 替代子查询、批量操作用 `foreach` |
| **索引优化** | WHERE/ORDER BY/JOIN ON 字段加索引；避免过多索引（≤5）；覆盖索引避免回表 |
| **代码优化** | 连接池合理配置、Redis 缓存高频查询数据、避免长事务、大数据分页查询 |

---

## 6. 分布式场景拓展

| 技术 | 方案 | 工具 |
|------|------|------|
| **分库分表** | 水平分片（按行）+ 垂直分库（按业务） | ShardingSphere、MyCat |
| **分布式事务** | 2PC / TCC / SAGA / 本地消息表 | Seata |
| **读写分离** | 主库写入 + 从库读取 | ShardingSphere |

---

## 7. 常见问题与避坑

| 问题 | 原因 | 解决方案 |
|------|------|----------|
| **SQL 注入** | `${ }` 拼接 SQL / JDBC 字符串拼接 | 优先 `#{ }`；必须 `${ }` 时做白名单校验 |
| **连接泄露** | 未关闭 Connection/Statement/ResultSet | try-with-resources；检查连接池配置 |
| **事务不回滚** | 未配 `rollbackFor` / 异常被 catch / 非 public | 配 `rollbackFor = Exception.class` |
| **查询性能差** | 未加索引、全表扫描、无分页 | 加索引 + 优化 SQL + 分页 + Redis 缓存 |
| **MyBatis 映射异常** | 字段不匹配 / namespace 错误 | 开启下划线转驼峰；检查 namespace 与接口全路径一致 |

---

> 🎯 **核心总结**：Java 数据库编程 = JDBC（底层规范）→ MyBatis（ORM 框架）→ HikariCP（连接池）→ 事务管理 + 性能优化。基础必懂 JDBC 原理，开发首选 MyBatis + Spring Boot，核心避坑是 SQL 注入和连接泄露。
