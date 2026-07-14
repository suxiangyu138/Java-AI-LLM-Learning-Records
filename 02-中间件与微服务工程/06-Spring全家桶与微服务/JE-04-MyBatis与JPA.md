# ORM 框架: MyBatis 与 JPA

> **"The database is the truth. How you map it to objects defines your pain."**  
> 版本: Spring Boot 3.x / MyBatis 3.5+ / Hibernate 6.x / JDK 17+  
> 适用: 企业级数据访问层设计

---

## Table of Contents

1. [ORM 概念与演进](#1-orm-概念与演进)
2. [MyBatis 架构与核心](#2-mybatis-架构与核心)
3. [MyBatis XML 映射](#3-mybatis-xml-映射)
4. [MyBatis 动态 SQL](#4-mybatis-动态-sql)
5. [MyBatis 高级特性](#5-mybatis-高级特性)
6. [MyBatis-Plus 扩展](#6-mybatis-plus-扩展)
7. [JPA 与 Hibernate 核心](#7-jpa-与-hibernate-核心)
8. [实体映射与关系](#8-实体映射与关系)
9. [JPQL 与 Criteria API](#9-jpql-与-criteria-api)
10. [Spring Data JPA](#10-spring-data-jpa)
11. [事务管理](#11-事务管理)
12. [性能优化与 N+1 解决](#12-性能优化与-n1-解决)
13. [MyBatis vs JPA 对比](#13-mybatis-vs-jpa-对比)
14. [企业实践: 两者结合使用](#14-企业实践-两者结合使用)
15. [面试题精选](#15-面试题精选)

---

## 1. ORM 概念与演进

### 1.1 什么是 ORM

```
┌─────────────────────────────────────────────────────────────────────┐
│                       ORM 概念                                       │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  Object-Relational Mapping (对象关系映射)                            │
│                                                                     │
│  Java 对象                 关系数据库                                │
│  ┌──────────────┐         ┌──────────────────┐                    │
│  │  User        │         │  users 表         │                    │
│  │  - id: Long  │ ──────▶ │  id BIGINT PK     │                    │
│  │  - name      │         │  name VARCHAR(50)  │                    │
│  │  - email     │         │  email VARCHAR(100)│                    │
│  │  - createdAt │         │  created_at DATETIME│                   │
│  └──────────────┘         └──────────────────┘                    │
│                                                                     │
│  ORM 框架负责:                                                      │
│  1. 对象 ↔ 表映射 (哪张表, 哪些列)                                   │
│  2. 类型转换 (Java Type ↔ SQL Type)                                │
│  3. 关系映射 (外键 → 对象引用)                                       │
│  4. 状态跟踪 (新增/修改/删除/查询)                                   │
│  5. 查询构建 (JPQL/SQL → 对象查询)                                  │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### 1.2 JDBC → ORM 进化史

```java
// ========== 第 1 代: 纯 JDBC (痛苦) ==========
public User findById(Long id) {
    Connection conn = null;
    PreparedStatement stmt = null;
    ResultSet rs = null;
    try {
        conn = dataSource.getConnection();
        stmt = conn.prepareStatement("SELECT * FROM users WHERE id = ?");
        stmt.setLong(1, id);
        rs = stmt.executeQuery();
        
        if (rs.next()) {
            User user = new User();
            user.setId(rs.getLong("id"));
            user.setName(rs.getString("name"));
            user.setEmail(rs.getString("email"));
            user.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            return user;
        }
        return null;
    } catch (SQLException e) {
        throw new RuntimeException(e);
    } finally {
        // 需要手动关闭所有资源...
        if (rs != null) try { rs.close(); } catch (SQLException e) { }
        if (stmt != null) try { stmt.close(); } catch (SQLException e) { }
        if (conn != null) try { conn.close(); } catch (SQLException e) { }
    }
    // 问题: 大量样板代码, 资源管理易出错, 类型转换繁琐
}

// ========== 第 2 代: Spring JdbcTemplate (改进) ==========
public User findById(Long id) {
    return jdbcTemplate.queryForObject(
        "SELECT * FROM users WHERE id = ?",
        (rs, rowNum) -> {
            User user = new User();
            user.setId(rs.getLong("id"));
            user.setName(rs.getString("name"));
            user.setEmail(rs.getString("email"));
            user.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            return user;
        },
        id
    );
    // 仍有手动映射, 但资源管理由 Spring 处理
}

// ========== 第 3 代: ORM 框架 (MyBatis/JPA) ==========
// MyBatis: SQL 在 XML 中, 框架自动映射
// JPA: 完全自动, 无需写 SQL (但复杂查询仍需)
```

### 1.3 主流的 Java ORM 框架

```
┌─────────────────────────────────────────────────────────────────────┐
│                   Java ORM 框架对比                                 │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  MyBatis (国内主流)              JPA/Hibernate (国外主流)            │
│  ───────────────────────         ──────────────────────────          │
│  SQL 由开发者完全控制             自动生成 SQL                        │
│  XML/注解写 SQL                   JPQL/HQL 写查询                   │
│  轻量级, 学习成本低               重量级, 概念多                     │
│  细粒度 SQL 优化                 粗粒度操作                          │
│  国内互联网公司首选               国外企业/传统行业首选               │
│                                                                     │
│  共同点:                                                             │
│  ● 都支持声明式事务 @Transactional                                   │
│  ● 都支持连接池 (HikariCP)                                         │
│  ● 都支持 Spring Boot Starter                                      │
│  ● 都是企业级项目的主流选择                                          │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 2. MyBatis 架构与核心

### 2.1 MyBatis 架构

```
┌─────────────────────────────────────────────────────────────────────┐
│                     MyBatis 架构图                                   │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  应用层                                                             │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │  SqlSessionFactoryBuilder → SqlSessionFactory → SqlSession   │  │
│  │                          ↓                                   │  │
│  │                     Mapper 接口 ← MapperProxy                 │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                                                                    │
│  核心层                                                             │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │  Configuration                                                │  │
│  │  ├── Environments (数据源, 事务管理器)                        │  │
│  │  ├── MapperRegistry (Mapper 接口注册)                        │  │
│  │  ├── MappedStatement (SQL 语句封装)                          │  │
│  │  ├── TypeHandlerRegistry (类型处理器)                        │  │
│  │  ├── InterceptorChain (插件链)                               │  │
│  │  └── Cache (二级缓存)                                        │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                                                                    │
│  执行层                                                             │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │  Executor → StatementHandler → ParameterHandler → ResultHandler│ │
│  │  │              │                    │                 │      │  │
│  │  │              ▼                    ▼                 ▼      │  │
│  │  │         SQL 解析           参数绑定          结果集映射     │  │
│  │  ▼                                                           │  │
│  │  JDBC (Connection, PreparedStatement, ResultSet)             │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                                                                    │
└─────────────────────────────────────────────────────────────────────┘
```

### 2.2 Spring Boot 集成 MyBatis

```xml
<!-- ========== Maven 依赖 ========== -->
<dependency>
    <groupId>org.mybatis.spring.boot</groupId>
    <artifactId>mybatis-spring-boot-starter</artifactId>
    <version>3.0.3</version>
</dependency>
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>
```

```yaml
# ========== application.yml 配置 ==========
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/myapp?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: root
    password: password
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000

mybatis:
  # Mapper XML 文件位置
  mapper-locations: classpath:mapper/**/*.xml
  # 实体类包路径 (XML 中 resultType 使用别名)
  type-aliases-package: com.example.entity
  # 驼峰命名转换 (user_name → userName)
  configuration:
    map-underscore-to-camel-case: true
    # 开启二级缓存
    cache-enabled: true
    # 启用懒加载
    lazy-loading-enabled: true
    # 积极懒加载 (如果为 false, 按需加载)
    aggressive-lazy-loading: false
    # 执行器类型: SIMPLE(默认), REUSE, BATCH
    default-executor-type: SIMPLE
    # SQL 日志
    log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl
```

### 2.3 SqlSessionFactory & SqlSession

```java
// ========== MyBatis 核心 API ==========
// SqlSessionFactory: 创建 SqlSession 的工厂 (线程安全, 全局一个)
// SqlSession: 数据库会话 (线程不安全, 每次请求创建新的)

// 通常不直接使用这些 API, 而是通过 Mapper 接口
// 但理解底层原理很重要

// ========== 直接使用 SqlSession ==========
@Service
public class UserDao {
    
    private final SqlSessionFactory sqlSessionFactory;
    
    public UserDao(SqlSessionFactory sqlSessionFactory) {
        this.sqlSessionFactory = sqlSessionFactory;
    }
    
    public User findById(Long id) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            // 命名空间 + SQL ID
            return session.selectOne("com.example.mapper.UserMapper.findById", id);
        }
    }
    
    public void insert(User user) {
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            // true: 自动提交
            session.insert("com.example.mapper.UserMapper.insert", user);
        }
    }
}

// ========== 使用 Mapper 接口 (推荐) ==========
// MyBatis 通过 JDK 动态代理为 Mapper 接口创建实现类
// 不需要自己写实现类!

@Mapper  // 标记为 MyBatis Mapper
public interface UserMapper {
    User findById(@Param("id") Long id);
    List<User> findAll();
    void insert(User user);
    void update(User user);
    void deleteById(@Param("id") Long id);
}

// 使用:
@Service
public class UserService {
    
    private final UserMapper userMapper;
    
    public UserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }
    
    public User findById(Long id) {
        return userMapper.findById(id);
    }
}
```

### 2.4 @MapperScan 配置

```java
// ========== Mapper 扫描配置 ==========

// 方式 1: 在主启动类添加
@SpringBootApplication
@MapperScan("com.example.mapper")  // 扫描指定包
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}

// 方式 2: 在配置类添加
@Configuration
@MapperScan(
    basePackages = "com.example.mapper",
    sqlSessionFactoryRef = "sqlSessionFactory",
    sqlSessionTemplateRef = "sqlSessionTemplate"
)
public class MyBatisConfig { }

// 方式 3: 在每个 Mapper 上加 @Mapper (不推荐, 繁琐)
@Mapper
public interface UserMapper { ... }

// 最佳实践: 方式 1 + 方式 3 的组合
// 在启动类使用 @MapperScan 扫包
// 同时 Mapper 接口上保留 @Mapper (IDE 语法高亮)
```

---

## 3. MyBatis XML 映射

### 3.1 基础映射 (CRUD)

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" 
    "http://mybatis.org/dtd/mybatis-3-mapper.dtd">

<!-- namespace: 对应 Mapper 接口全限定名 -->
<mapper namespace="com.example.mapper.UserMapper">

    <!-- ========== 1. 基础 resultMap ========== -->
    <!-- id 标签映射主键, result 标签映射普通列 -->
    <resultMap id="userMap" type="User">
        <id property="id" column="id" javaType="Long"/>
        <result property="userName" column="user_name"/>
        <result property="email" column="email"/>
        <result property="phone" column="phone"/>
        <result property="status" column="status" 
                typeHandler="org.apache.ibatis.type.EnumTypeHandler"/>
        <result property="createdAt" column="created_at"/>
        <result property="updatedAt" column="updated_at"/>
    </resultMap>

    <!-- ========== 2. 插入 ========== -->
    <!-- useGeneratedKeys: 获取自增主键 -->
    <!-- keyProperty: 将生成的主键赋值到哪个字段 -->
    <insert id="insert" useGeneratedKeys="true" keyProperty="id">
        INSERT INTO users (user_name, email, phone, status, created_at, updated_at)
        VALUES (#{userName}, #{email}, #{phone}, #{status}, NOW(), NOW())
    </insert>
    
    <!-- 批量插入 -->
    <insert id="batchInsert" useGeneratedKeys="true" keyProperty="id">
        INSERT INTO users (user_name, email, created_at, updated_at)
        VALUES
        <foreach collection="list" item="user" separator=",">
            (#{user.userName}, #{user.email}, NOW(), NOW())
        </foreach>
    </insert>

    <!-- ========== 3. 更新 ========== -->
    <update id="update">
        UPDATE users
        SET user_name = #{userName},
            email = #{email},
            phone = #{phone},
            updated_at = NOW()
        WHERE id = #{id}
    </update>

    <!-- ========== 4. 删除 ========== -->
    <delete id="deleteById">
        DELETE FROM users WHERE id = #{id}
    </delete>
    
    <!-- 逻辑删除 (推荐) -->
    <update id="logicDeleteById">
        UPDATE users SET deleted = 1, deleted_at = NOW() WHERE id = #{id}
    </update>

    <!-- ========== 5. 查询 ========== -->
    <select id="findById" resultMap="userMap">
        SELECT * FROM users WHERE id = #{id} AND deleted = 0
    </select>

    <select id="findAll" resultMap="userMap">
        SELECT * FROM users WHERE deleted = 0 ORDER BY created_at DESC
    </select>
    
    <!-- 分页查询 -->
    <select id="findPage" resultMap="userMap">
        SELECT * FROM users WHERE deleted = 0
        ORDER BY created_at DESC
        LIMIT #{offset}, #{limit}
    </select>
    
    <select id="countAll" resultType="long">
        SELECT COUNT(*) FROM users WHERE deleted = 0
    </select>

</mapper>
```

### 3.2 resultMap 高级映射

```xml
<!-- ========== 一对一关联 (association) ========== -->
<!-- 用户 - 详情 (1:1) -->
<resultMap id="userWithDetailMap" type="User" extends="userMap">
    <!-- association: 嵌套对象映射 -->
    <association property="userDetail" javaType="UserDetail">
        <id property="id" column="detail_id"/>
        <result property="realName" column="real_name"/>
        <result property="idCard" column="id_card"/>
        <result property="birthday" column="birthday"/>
        <result property="gender" column="gender"/>
    </association>
</resultMap>

<select id="findUserWithDetail" resultMap="userWithDetailMap">
    SELECT u.*, ud.id AS detail_id, ud.real_name, ud.id_card, 
           ud.birthday, ud.gender
    FROM users u
    LEFT JOIN user_details ud ON u.id = ud.user_id
    WHERE u.id = #{id}
</select>

<!-- ========== 一对多关联 (collection) ========== -->
<!-- 用户 - 订单 (1:N) -->
<resultMap id="userWithOrdersMap" type="User" extends="userMap">
    <!-- collection: 嵌套集合映射 -->
    <!-- ofType: 集合元素的类型 -->
    <collection property="orders" ofType="Order" columnPrefix="order_">
        <id property="id" column="id"/>
        <result property="totalAmount" column="total_amount"/>
        <result property="status" column="status"/>
        <result property="createdAt" column="created_at"/>
    </collection>
</resultMap>

<select id="findUserWithOrders" resultMap="userWithOrdersMap">
    SELECT u.*, 
           o.id AS order_id, 
           o.total_amount AS order_total_amount,
           o.status AS order_status,
           o.created_at AS order_created_at
    FROM users u
    LEFT JOIN orders o ON u.id = o.user_id
    WHERE u.id = #{id}
    ORDER BY o.created_at DESC
</select>

<!-- ========== 逻辑分页查询 (先查主表再查子表) ========== -->
<!-- 步骤 1: 查询用户分页 -->
<select id="findUserPage" resultMap="userMap">
    SELECT * FROM users WHERE deleted = 0
    ORDER BY created_at DESC
    LIMIT #{offset}, #{limit}
</select>
<!-- 步骤 2: 在 Service 中批量查询订单 -->
<!-- 步骤 3: 在 Service 中组装 -->

<!-- ========== 嵌套查询 (懒加载) ========== -->
<resultMap id="userLazyOrdersMap" type="User" extends="userMap">
    <!-- select: 调用的另一个查询 -->
    <!-- column: 传给子查询的参数 -->
    <!-- fetchType: lazy(懒加载) / eager(立即加载) -->
    <collection property="orders" 
                select="com.example.mapper.OrderMapper.findByUserId"
                column="id"
                fetchType="lazy"/>
</resultMap>

<select id="findUserLazy" resultMap="userLazyOrdersMap">
    SELECT * FROM users WHERE id = #{id}
</select>

<!-- ========== 鉴别器 (discriminator) ========== -->
<!-- 根据字段值选择不同的 resultMap -->
<resultMap id="baseArticleMap" type="Article">
    <id property="id" column="id"/>
    <result property="title" column="title"/>
    <result property="type" column="type"/>
    <discriminator javaType="int" column="type">
        <case value="1" resultMap="newsArticleMap"/>
        <case value="2" resultMap="blogArticleMap"/>
        <case value="3" resultMap="reviewArticleMap"/>
    </discriminator>
</resultMap>
```

### 3.3 注解方式 SQL

```java
// ========== MyBatis 注解方式 (适合简单 SQL) ==========
// 优点: 不用 XML 文件, 快速开发
// 缺点: 复杂 SQL 可读性差, 动态 SQL 困难
// 建议: 简单 CRUD 用注解, 复杂查询用 XML

@Mapper
public interface UserAnnotationMapper {
    
    @Select("SELECT * FROM users WHERE id = #{id} AND deleted = 0")
    @Results(id = "userMap", value = {
        @Result(property = "id", column = "id", id = true),
        @Result(property = "userName", column = "user_name"),
        @Result(property = "email", column = "email"),
        @Result(property = "createdAt", column = "created_at")
    })
    User findById(@Param("id") Long id);
    
    @Select("SELECT * FROM users WHERE deleted = 0 ORDER BY created_at DESC")
    @ResultMap("userMap")
    List<User> findAll();
    
    @Insert("INSERT INTO users(user_name, email, phone, status, created_at, updated_at) " +
            "VALUES(#{userName}, #{email}, #{phone}, #{status}, NOW(), NOW())")
    @SelectKey(statement = "SELECT LAST_INSERT_ID()", keyProperty = "id",
               before = false, resultType = Long.class)
    void insert(User user);
    
    @Update("UPDATE users SET user_name=#{userName}, email=#{email}, updated_at=NOW() " +
            "WHERE id=#{id}")
    int update(User user);
    
    @Delete("DELETE FROM users WHERE id = #{id}")
    int deleteById(@Param("id") Long id);
    
    // 多参数
    @Select("SELECT * FROM users WHERE user_name LIKE CONCAT('%', #{keyword}, '%') " +
            "AND status = #{status} LIMIT #{offset}, #{limit}")
    List<User> search(@Param("keyword") String keyword,
                      @Param("status") Integer status,
                      @Param("offset") int offset,
                      @Param("limit") int limit);
}
```

### 3.4 #{} 与 ${} 的区别

```xml
<!-- ========== #{} 与 ${} 的本质区别 ========== -->

<!-- #{}: PreparedStatement 参数占位符 (推荐) -->
<!-- 会生成 ? 占位符, 由 JDBC 驱动处理参数 -->
<!-- 自动加引号, 防 SQL 注入 -->

<!-- ${}: 字符串替换 (危险!) -->
<!-- 直接拼接 SQL 字符串 -->
<!-- 不自动加引号, 有 SQL 注入风险 -->
<!-- 只能用于: 表名, 列名, ORDER BY 字段等不能占位符化的场景 -->

<!-- 安全用法 (#{}) -->
<select id="findByName" resultMap="userMap">
    SELECT * FROM users WHERE user_name = #{name}
    -- 最终 SQL: SELECT * FROM users WHERE user_name = ?
    -- 参数: 'John' → PreparedStatement 安全设置
    -- 即使传入 "1' OR '1'='1" 也是安全的
</select>

<!-- 危险用法 (尽量不要用 ${}) -->
<select id="findByColumn" resultMap="userMap">
    SELECT * FROM users ORDER BY ${orderBy} ${orderDir}
    -- 必须保证 orderBy 来自白名单校验!
</select>

<!-- 为什么还需要 ${}? 总结场景: -->
<!-- 1. 动态表名: SELECT * FROM ${tableName} -->
<!-- 2. 动态列名: ORDER BY ${columnName} -->
<!-- 3. LIKE 模糊查询: 使用 CONCAT 避免 ${} -->
<!-- 4. IN 查询: 使用 <foreach> 避免 ${} -->

<!-- LIKE 查询的正确做法 -->
<select id="searchByName" resultMap="userMap">
    -- 推荐: 使用 CONCAT 函数
    SELECT * FROM users WHERE user_name LIKE CONCAT('%', #{keyword}, '%')
    
    -- 或者: 在 Java 代码中拼好 %
    -- Java: mapper.searchByName("%" + keyword + "%");
    -- 不推荐使用 ${} 做 LIKE
</select>

<!-- IN 查询的正确做法 -->
<select id="findByIds" resultMap="userMap">
    SELECT * FROM users WHERE id IN
    <foreach collection="ids" item="id" open="(" separator="," close=")">
        #{id}
    </foreach>
    -- 推荐: 使用 foreach 生成 ?,?,?
    -- 不推荐: WHERE id IN (${ids}) — SQL 注入!
</select>
```

---

## 4. MyBatis 动态 SQL

### 4.1 核心标签

```xml
<!-- ========== <if> 条件判断 ========== -->
<select id="findByCondition" resultMap="userMap">
    SELECT * FROM users WHERE deleted = 0
    <if test="userName != null and userName != ''">
        AND user_name LIKE CONCAT('%', #{userName}, '%')
    </if>
    <if test="email != null and email != ''">
        AND email = #{email}
    </if>
    <if test="status != null">
        AND status = #{status}
    </if>
    <if test="startDate != null">
        AND created_at >= #{startDate}
    </if>
    <if test="endDate != null">
        AND created_at &lt;= #{endDate}
    </if>
    ORDER BY created_at DESC
</select>

<!-- ========== <choose>/<when>/<otherwise> 分支选择 ========== -->
<select id="findByQueryType" resultMap="userMap">
    SELECT * FROM users WHERE deleted = 0
    <choose>
        <when test="queryType == 'name'">
            AND user_name LIKE CONCAT('%', #{keyword}, '%')
        </when>
        <when test="queryType == 'email'">
            AND email LIKE CONCAT('%', #{keyword}, '%')
        </when>
        <when test="queryType == 'phone'">
            AND phone LIKE CONCAT('%', #{keyword}, '%')
        </when>
        <otherwise>
            AND (user_name LIKE CONCAT('%', #{keyword}, '%')
                 OR email LIKE CONCAT('%', #{keyword}, '%'))
        </otherwise>
    </choose>
</select>

<!-- ========== <where> 自动处理 WHERE 和 AND ========== -->
<select id="search" resultMap="userMap">
    SELECT * FROM users
    <where>  <!-- 自动添加 WHERE, 并去掉多余的 AND -->
        deleted = 0
        <if test="userName != null">
            AND user_name LIKE CONCAT('%', #{userName}, '%')
        </if>
        <if test="email != null">
            AND email = #{email}
        </if>
        <if test="status != null">
            AND status = #{status}
        </if>
    </where>
    ORDER BY created_at DESC
</select>

<!-- ========== <set> 动态更新 (自动处理 SET 和逗号) ========== -->
<update id="updateSelective">
    UPDATE users
    <set>
        <if test="userName != null">user_name = #{userName},</if>
        <if test="email != null">email = #{email},</if>
        <if test="phone != null">phone = #{phone},</if>
        <if test="status != null">status = #{status},</if>
        updated_at = NOW()
    </set>
    WHERE id = #{id}
</update>

<!-- ========== <foreach> 集合遍历 ========== -->
<!-- 可用 IN 查询, 批量插入, 批量更新 -->

<!-- IN 查询 -->
<select id="findByIds" resultMap="userMap">
    SELECT * FROM users WHERE id IN
    <foreach collection="list" item="id" open="(" separator="," close=")">
        #{id}
    </foreach>
</select>

<!-- 批量插入 -->
<insert id="batchInsert">
    INSERT INTO users (user_name, email, status, created_at, updated_at)
    VALUES
    <foreach collection="list" item="user" separator=",">
        (#{user.userName}, #{user.email}, #{user.status}, NOW(), NOW())
    </foreach>
</insert>

<!-- 批量更新 (MySQL 语法) -->
<update id="batchUpdateStatus" parameterType="list">
    UPDATE users SET status =
    <foreach collection="list" item="item" index="index" separator=" " open="CASE id" close="END">
        WHEN #{item.id} THEN #{item.status}
    </foreach>
    WHERE id IN
    <foreach collection="list" item="item" open="(" separator="," close=")">
        #{item.id}
    </foreach>
</update>

<!-- 遍历 Map -->
<select id="findByAnyColumn" resultMap="userMap">
    SELECT * FROM users
    <where>
        <foreach collection="params.entrySet()" item="value" index="key">
            <if test="value != null">
                AND ${key} = #{value}
            </if>
        </foreach>
    </where>
</select>

<!-- ========== <trim> 自定义前缀后缀 ========== -->
<!-- WHERE 等价于 <trim prefix="WHERE" prefixOverrides="AND |OR "> -->
<!-- SET 等价于 <trim prefix="SET" suffixOverrides=","> -->

<select id="searchWithTrim" resultMap="userMap">
    SELECT * FROM users
    <trim prefix="WHERE" prefixOverrides="AND |OR ">
        <if test="name != null">AND user_name = #{name}</if>
        <if test="email != null">AND email = #{email}</if>
    </trim>
</select>

<update id="updateWithTrim">
    UPDATE users
    <trim prefix="SET" suffixOverrides=",">
        <if test="userName != null">user_name = #{userName},</if>
        <if test="email != null">email = #{email},</if>
        updated_at = NOW(),
    </trim>
    WHERE id = #{id}
</update>

<!-- ========== <bind> 变量绑定 ========== -->
<!-- 在 XML 中定义变量, 避免在 Java 代码中拼接 -->
<select id="searchUser" resultMap="userMap">
    <bind name="likePattern" value="'%' + keyword + '%'"/>
    SELECT * FROM users
    WHERE user_name LIKE #{likePattern}
    OR email LIKE #{likePattern}
</select>
```

### 4.2 完整的动态查询示例

```xml
<!-- ========== 复杂业务查询 ========== -->
<mapper namespace="com.example.mapper.OrderMapper">

    <resultMap id="orderWithItemsMap" type="OrderVO">
        <id property="id" column="id"/>
        <result property="orderNo" column="order_no"/>
        <result property="userId" column="user_id"/>
        <result property="totalAmount" column="total_amount"/>
        <result property="status" column="status"/>
        <result property="createdAt" column="created_at"/>
        <collection property="items" ofType="OrderItemVO">
            <id property="id" column="item_id"/>
            <result property="productName" column="product_name"/>
            <result property="quantity" column="quantity"/>
            <result property="price" column="price"/>
            <result property="subtotal" column="subtotal"/>
        </collection>
    </resultMap>

    <!-- 多表关联动态查询 -->
    <select id="findOrders" resultMap="orderWithItemsMap">
        SELECT 
            o.*,
            oi.id AS item_id,
            oi.product_name,
            oi.quantity,
            oi.price,
            oi.subtotal
        FROM orders o
        LEFT JOIN order_items oi ON o.id = oi.order_id
        <where>
            <if test="userId != null">
                AND o.user_id = #{userId}
            </if>
            <if test="status != null">
                AND o.status = #{status}
            </if>
            <if test="orderNo != null and orderNo != ''">
                AND o.order_no LIKE CONCAT('%', #{orderNo}, '%')
            </if>
            <if test="minAmount != null">
                AND o.total_amount >= #{minAmount}
            </if>
            <if test="maxAmount != null">
                AND o.total_amount &lt;= #{maxAmount}
            </if>
            <if test="startDate != null">
                AND o.created_at >= #{startDate}
            </if>
            <if test="endDate != null">
                AND o.created_at &lt;= #{endDate}
            </if>
        </where>
        ORDER BY o.created_at DESC
        LIMIT #{offset}, #{limit}
    </select>
    
    <select id="countOrders" resultType="long">
        SELECT COUNT(DISTINCT o.id) FROM orders o
        <where>
            <if test="userId != null">AND o.user_id = #{userId}</if>
            <if test="status != null">AND o.status = #{status}</if>
            <if test="orderNo != null">AND o.order_no LIKE CONCAT('%', #{orderNo}, '%')</if>
            <if test="minAmount != null">AND o.total_amount >= #{minAmount}</if>
            <if test="maxAmount != null">AND o.total_amount &lt;= #{maxAmount}</if>
            <if test="startDate != null">AND o.created_at >= #{startDate}</if>
            <if test="endDate != null">AND o.created_at &lt;= #{endDate}</if>
        </where>
    </select>

</mapper>
```

---

## 5. MyBatis 高级特性

### 5.1 主键生成策略

```xml
<!-- ========== 主键生成策略 ========== -->

<!-- 方式 1: 数据库自增 (推荐) -->
<insert id="insert" useGeneratedKeys="true" keyProperty="id" keyColumn="id">
    INSERT INTO users (user_name, email) VALUES (#{userName}, #{email})
</insert>

<!-- 方式 2: selectKey 查询主键 (Oracle 序列) -->
<insert id="insertOracle">
    <selectKey keyProperty="id" resultType="long" order="BEFORE">
        SELECT SEQ_USERS.NEXTVAL FROM DUAL
    </selectKey>
    INSERT INTO users (id, user_name, email) VALUES (#{id}, #{userName}, #{email})
</insert>

<!-- 方式 3: UUID 主键 -->
<insert id="insertWithUUID">
    <selectKey keyProperty="id" resultType="string" order="BEFORE">
        SELECT REPLACE(UUID(), '-', '')
    </selectKey>
    INSERT INTO users (id, user_name, email) VALUES (#{id}, #{userName}, #{email})
</insert>
```

### 5.2 缓存机制

```java
// ========== MyBatis 缓存 ==========
//
// 一级缓存 (SqlSession 级别, 默认开启)
//   ● 同一个 SqlSession 中, 相同的查询只执行一次 SQL
//   ● 更新操作会清空一级缓存
//   ● 无法关闭, 也不能跨 SqlSession 共享
//
// 二级缓存 (Mapper 级别, 需手动开启)
//   ● 跨 SqlSession 共享
//   ● 通过 Cache 接口实现
//   ● 默认使用 PerpetualCache
//   ● 分布式环境下需要谨慎 (缓存不一致)
```

```xml
<!-- ========== 二级缓存配置 ========== -->

<!-- 1. 在 mybatis-config.xml 或 application.yml 中开启 -->
<!-- mybatis.configuration.cache-enabled: true -->

<!-- 2. 在 Mapper XML 中配置缓存 -->
<mapper namespace="com.example.mapper.UserMapper">
    <!-- 开启二级缓存 -->
    <!--
    eviction: 缓存回收策略
        LRU — 最近最少使用 (默认)
        FIFO — 先进先出
        SOFT — 软引用
        WEAK — 弱引用
    flushInterval: 刷新间隔 (毫秒)
    size: 缓存引用数量
    readOnly: 只读 (true 性能好, 但有线程安全问题)
    -->
    <cache eviction="LRU"
           flushInterval="60000"
           size="512"
           readOnly="false"/>
    
    <!-- 3. 每个查询语句可以单独设置是否使用缓存 -->
    <select id="findById" resultMap="userMap" useCache="true">
        SELECT * FROM users WHERE id = #{id}
    </select>
    
    <!-- 4. 每次更新操作会自动清空缓存 -->
    <update id="update" flushCache="true">
        UPDATE users SET ... WHERE id = #{id}
    </update>
</mapper>

<!-- ========== 自定义缓存 ========== -->
<!-- 实现 org.apache.ibatis.cache.Cache 接口 -->
<!-- 可以用 Redis 作为二级缓存 -->
<mapper namespace="com.example.mapper.UserMapper">
    <cache type="com.example.cache.RedisCache">
        <property name="host" value="localhost"/>
    </cache>
</mapper>

<!-- ========== 注意事项 ========== -->
<!-- 1. 二级缓存只对查询有效, 更新操作会刷新缓存 -->
<!-- 2. 分布式环境下推荐使用 Redis 等集中式缓存, 而非 MyBatis 二级缓存 -->
<!-- 3. 查询频繁且少更新的表适合缓存 -->
<!-- 4. 关联查询的结果可能因其他表的更新而变脏 -->
```

### 5.3 插件机制 (Interceptor)

```java
// ========== MyBatis 插件 ==========
// 可以拦截 4 种对象的方法:
// - Executor (update, query, flushStatements, commit, rollback)
// - StatementHandler (prepare, parameterize, batch, update, query)
// - ParameterHandler (getParameterObject, setParameters)
// - ResultHandler (handleResultSets, handleOutputParameters)

// ========== 分页插件 (PageHelper) ==========
// 最常用的 MyBatis 插件
<dependency>
    <groupId>com.github.pagehelper</groupId>
    <artifactId>pagehelper-spring-boot-starter</artifactId>
    <version>1.4.6</version>
</dependency>

@Service
public class UserService {
    
    @Autowired
    private UserMapper userMapper;
    
    public PageInfo<User> findPage(int pageNum, int pageSize) {
        // 在查询前调用, 自动拦截并分页
        PageHelper.startPage(pageNum, pageSize);
        
        List<User> users = userMapper.findAll();
        
        // 包装为 PageInfo (包含总页数, 总记录数等)
        return new PageInfo<>(users);
    }
    
    // @PageableDefault 配合 Spring 的分页参数
    public PageInfo<User> search(UserQuery query) {
        PageHelper.startPage(query.getPage(), query.getSize(), query.getOrderBy());
        List<User> users = userMapper.searchByCondition(query);
        return new PageInfo<>(users);
    }
}

// ========== 自定义插件: SQL 审计 ==========
@Intercepts({
    @Signature(type = Executor.class, method = "update", 
              args = {MappedStatement.class, Object.class}),
    @Signature(type = Executor.class, method = "query",
              args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class})
})
@Component
public class SqlAuditInterceptor implements Interceptor {
    
    private static final Logger log = LoggerFactory.getLogger(SqlAuditInterceptor.class);
    
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        long start = System.currentTimeMillis();
        
        try {
            return invocation.proceed();
        } finally {
            long duration = System.currentTimeMillis() - start;
            
            MappedStatement ms = (MappedStatement) invocation.getArgs()[0];
            String sqlId = ms.getId();
            
            log.info("SQL: {} | Duration: {}ms", sqlId, duration);
            
            // 慢 SQL 告警
            if (duration > 1000) {
                log.warn("慢 SQL 告警: {} ({}ms)", sqlId, duration);
            }
        }
    }
    
    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }
    
    @Override
    public void setProperties(Properties properties) {
        // 读取 plugin 配置参数
    }
}

// ========== 自定义插件: 数据加密/解密 ==========
@Component
@Intercepts({
    @Signature(type = ParameterHandler.class, method = "setParameters", 
              args = {PreparedStatement.class}),
    @Signature(type = ResultSetHandler.class, method = "handleResultSets", 
              args = {Statement.class})
})
public class EncryptInterceptor implements Interceptor {
    // 在参数设置时加密敏感字段
    // 在结果处理时解密敏感字段
    // 应用场景: 手机号, 身份证, 银行卡号
}
```

### 5.4 TypeHandler

```java
// ========== 自定义 TypeHandler ==========
// 处理 Java 类型 ↔ JDBC 类型之间的转换

// 场景: JSON 字段自动序列化/反序列化
@MappedTypes(List.class)
@MappedJdbcTypes(JdbcType.VARCHAR)
public class JsonListTypeHandler extends BaseTypeHandler<List<String>> {
    
    private static final ObjectMapper mapper = new ObjectMapper();
    
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, 
                                   List<String> parameter, JdbcType jdbcType) 
            throws SQLException {
        ps.setString(i, toJson(parameter));
    }
    
    @Override
    public List<String> getNullableResult(ResultSet rs, String columnName) 
            throws SQLException {
        return fromJson(rs.getString(columnName));
    }
    
    @Override
    public List<String> getNullableResult(ResultSet rs, int columnIndex) 
            throws SQLException {
        return fromJson(rs.getString(columnIndex));
    }
    
    @Override
    public List<String> getNullableResult(CallableStatement cs, int columnIndex) 
            throws SQLException {
        return fromJson(cs.getString(columnIndex));
    }
    
    private String toJson(List<String> list) {
        try {
            return mapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
    
    private List<String> fromJson(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return mapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}

// 使用:
public class User {
    // 数据库中存储为 VARCHAR JSON 字符串
    // Java 中为 List<String>
    private List<String> tags;
    
    // getters, setters...
}

// XML 配置:
// <result property="tags" column="tags" 
//         typeHandler="com.example.handler.JsonListTypeHandler"/>

// 或在 application.yml 中注册:
// mybatis:
//   type-handlers-package: com.example.handler
```

---

## 6. MyBatis-Plus 扩展

### 6.1 MyBatis-Plus 特性

```xml
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
    <version>3.5.5</version>
</dependency>
```

```java
// ========== MyBatis-Plus: BaseMapper 自动 CRUD ==========
// 继承 BaseMapper, 自动获得 CRUD 方法
// 无需写 SQL 和 XML

@Mapper
public interface UserMapper extends BaseMapper<User> {
    // 继承的方法 (不需要手动实现):
    // int insert(T entity)
    // int deleteById(Serializable id)
    // int deleteByMap(Map<String, Object> columnMap)
    // int deleteBatchIds(Collection<? extends Serializable> idList)
    // int updateById(T entity)
    // T selectById(Serializable id)
    // List<T> selectBatchIds(Collection<? extends Serializable> idList)
    // List<T> selectByMap(Map<String, Object> columnMap)
    // T selectOne(Wrapper<T> queryWrapper)
    // Integer selectCount(Wrapper<T> queryWrapper)
    // List<T> selectList(Wrapper<T> queryWrapper)
    // List<T> selectPage(IPage<T> page, Wrapper<T> queryWrapper)
    // ...
    
    // 自定义复杂查询
    @Select("SELECT * FROM users WHERE deleted = 0 AND status = #{status}")
    List<User> findByStatus(@Param("status") Integer status);
}
```

### 6.2 条件构造器 (Wrapper)

```java
// ========== QueryWrapper ==========
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> 
                             implements UserService {
    
    // Wrapper 是 MyBatis-Plus 的核心
    public List<User> findUsers(String name, Integer status, LocalDate startDate) {
        // LambdaQueryWrapper: 类型安全, 推荐
        LambdaQueryWrapper<User> wrapper = Wrappers.lambdaQuery();
        
        wrapper
            .eq(User::getDeleted, 0)           // = 条件
            .like(StringUtils.isNotBlank(name), User::getUserName, name)  // LIKE, 条件成立才添加
            .eq(status != null, User::getStatus, status)  // 条件成立才添加
            .ge(startDate != null, User::getCreatedAt, startDate)  // >=
            .orderByDesc(User::getCreatedAt);   // ORDER BY
        
        return baseMapper.selectList(wrapper);
    }
    
    // QueryWrapper (不推荐, 列名硬编码)
    public List<User> findUsersOld(String name) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted", 0)
               .like("user_name", name)
               .orderByDesc("created_at");
        return baseMapper.selectList(wrapper);
    }
    
    // 复杂查询: 多表关联
    // mybatis-plus-join 扩展:
    // https://github.com/yulichang/mybatis-plus-join
}
```

### 6.3 分页插件

```java
// ========== 分页配置 ==========
@Configuration
public class MyBatisPlusConfig {
    
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        
        // 分页插件 (重要: 必须配置)
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        
        // 乐观锁插件
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        
        return interceptor;
    }
}

// ========== 分页使用 ==========
@Service
public class UserService {
    
    @Autowired
    private UserMapper userMapper;
    
    public Page<User> findPage(int current, int size) {
        // 创建分页对象
        Page<User> page = new Page<>(current, size);
        
        // 条件构造器
        LambdaQueryWrapper<User> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(User::getDeleted, 0);
        wrapper.orderByDesc(User::getCreatedAt);
        
        // 执行分页查询 (MyBatis-Plus 自动拦截并生成 COUNT + LIMIT)
        return userMapper.selectPage(page, wrapper);
        // 结果包含: records, total, pages, current, size
    }
}
```

### 6.4 逻辑删除

```java
// ========== 逻辑删除 ==========
// 物理删除: 真正从数据库删除
// 逻辑删除: 标记为已删除 (推荐)

// 1. 配置
// application.yml:
// mybatis-plus:
//   global-config:
//     db-config:
//       logic-delete-field: deleted     # 全局逻辑删除字段
//       logic-delete-value: 1           # 已删除值
//       logic-not-delete-value: 0       # 未删除值

// 2. 实体类
@Data
public class User {
    private Long id;
    private String userName;
    private String email;
    
    @TableLogic  // 逻辑删除注解
    @TableField(select = false)  // 默认查询不返回此字段
    private Integer deleted;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}

// 3. 使用
// userMapper.deleteById(1L);  // 实际执行: UPDATE users SET deleted=1 WHERE id=1
// userMapper.selectList(wrapper);  // 自动追加: AND deleted=0
```

### 6.5 自动填充与乐观锁

```java
// ========== 自动填充 ==========
// 自动填充创建时间, 更新时间等字段

@Component
public class MyMetaObjectHandler implements MetaObjectHandler {
    
    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "deleted", Integer.class, 0);
    }
    
    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
    }
}

// 实体类:
@Data
public class User {
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}

// ========== 乐观锁 ==========
// 解决并发更新冲突

// 1. 实体类添加 @Version
@Data
public class User {
    @Version  // 乐观锁
    @TableField(fill = FieldFill.INSERT)
    private Integer version;
    
    // version 初始值: 1
    // 更新时: UPDATE users SET name='xxx', version=2 WHERE id=1 AND version=1
    // 如果 version 不匹配, 更新失败 (抛出 OptimisticLockException)
}

// 2. 配置乐观锁插件 (已经在 MybatisPlusInterceptor 中配置)
// 3. 使用
// User user = userMapper.selectById(1L);
// user.setName("NewName");
// userMapper.updateById(user);  // 自动检查 version
```

---

## 7. JPA 与 Hibernate 核心

### 7.1 JPA 规范 vs Hibernate 实现

```
┌─────────────────────────────────────────────────────────────────────┐
│                    JPA 与 Hibernate 的关系                           │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  JPA (Jakarta Persistence API) — 规范                               │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │  ● javax.persistence.* → jakarta.persistence.* (3.x)          │ │
│  │  ● @Entity, @Id, @Column, @OneToMany, @ManyToOne             │ │
│  │  ● EntityManager, EntityManagerFactory                        │ │
│  │  ● JPQL, Criteria API                                         │ │
│  │  ● 只是一组接口和注解, 不提供实现                              │ │
│  └────────────────────────────────────────────────────────────────┘ │
│                               ↑ implements                          │
│  Hibernate — 实现                                                   │
│  ┌────────────────────────────────────────────────────────────────┐ │
│  │  ● 最流行的 JPA 实现                                           │ │
│  │  ● 扩展: Hibernate Search, Hibernate Validator                │ │
│  │  ● 附加功能: 缓存 (二级缓存), 延迟加载, 批量操作               │ │
│  │  ● Spring Boot 默认 JPA 实现                                   │ │
│  └────────────────────────────────────────────────────────────────┘ │
│                                                                     │
│  其他 JPA 实现: EclipseLink, OpenJPA                               │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### 7.2 Spring Boot 集成 JPA

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>
```

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/myapp
    username: root
    password: password
    driver-class-name: com.mysql.cj.jdbc.Driver
    
  jpa:
    # 数据库类型
    database: mysql
    database-platform: org.hibernate.dialect.MySQLDialect
    
    # DDL 自动处理
    # none: 啥也不做
    # validate: 验证实体与表结构是否匹配
    # update: 自动更新表结构 (开发用)
    # create: 每次启动删除并创建 (危险!)
    # create-drop: 启动创建, 关闭删除 (测试用)
    hibernate:
      ddl-auto: validate  # 生产环境用 validate
    
    # SQL 日志
    show-sql: true
    properties:
      hibernate:
        format_sql: true      # 格式化 SQL
        highlight_sql: true   # 语法高亮
        use_sql_comments: true # 显示 JPQL 注释
        
        # 批量操作
        jdbc:
          batch_size: 25       # 批量插入批次大小
          order_inserts: true  # 优化批量插入
          order_updates: true  # 优化批量更新
        
        # 二级缓存
        # cache:
        #   use_second_level_cache: true
        #   region.factory_class: org.hibernate.cache.jcache.JCacheRegionFactory
    
    # 开启 Open-in-View (延迟加载在视图层可用)
    # 默认为 true, 生产环境建议关闭 (性能问题)
    open-in-view: false
```

### 7.3 @Entity 注解

```java
// ========== 基础实体 ==========
@Data  // Lombok: @Getter, @Setter, @ToString, @EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Entity  // 标记为 JPA 实体
@Table(name = "users")  // 对应表名
@DynamicInsert  // 只插入非 null 字段
@DynamicUpdate  // 只更新有变化的字段
public class User {
    
    @Id  // 主键
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // 自增策略
    private Long id;
    
    @Column(name = "user_name", nullable = false, length = 50)
    private String userName;
    
    @Column(nullable = false, unique = true, length = 100)
    private String email;
    
    @Column(length = 20)
    private String phone;
    
    @Column(length = 255)
    @JsonIgnore  // 密码不返回给前端
    private String password;
    
    @Enumerated(EnumType.STRING)  // 枚举存储为字符串
    @Column(length = 20)
    private UserStatus status;
    
    @Column(name = "deleted", columnDefinition = "TINYINT DEFAULT 0")
    private Boolean deleted = false;
    
    // 时间字段
    @CreatedDate  // 自动填充创建时间 (需开启审计)
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate  // 自动填充更新时间
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Version  // 乐观锁
    private Integer version;
}

// ========== @GeneratedValue 策略 ==========
// IDENTITY:    数据库自增 (MySQL, SQL Server)
// SEQUENCE:    数据库序列 (Oracle, PostgreSQL) — 需要 @SequenceGenerator
// TABLE:       使用数据库模拟序列 (性能差, 不推荐)
// AUTO:        自动选择 (默认, 各数据库可能不一致)

// 序列生成器示例:
@Id
@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_seq")
@SequenceGenerator(name = "user_seq", sequenceName = "SEQ_USERS", allocationSize = 50)
private Long id;
// allocationSize: 预分配步长 (性能优化, 默认 50)
```

### 7.4 枚举映射

```java
// ========== 枚举映射 ==========
public enum UserStatus {
    ACTIVE("active"),
    INACTIVE("inactive"),
    BANNED("banned");
    
    private final String code;
    
    UserStatus(String code) { this.code = code; }
    
    @JsonValue  // Jackson 序列化
    public String getCode() { return code; }
    
    @JsonCreator  // Jackson 反序列化
    public static UserStatus fromCode(String code) {
        for (UserStatus s : values()) {
            if (s.code.equals(code)) return s;
        }
        throw new IllegalArgumentException("Unknown status: " + code);
    }
}

// 方式 1: @Enumerated(EnumType.STRING) — 存储枚举名
@Enumerated(EnumType.STRING)
@Column(length = 20)
private UserStatus status;  // 数据库存 "ACTIVE"

// 方式 2: @Enumerated(EnumType.ORDINAL) — 存储枚举序号 (不推荐)
@Enumerated(EnumType.ORDINAL)
private UserStatus status;  // 数据库存 0,1,2 (改了枚举顺序就出问题)

// 方式 3: 自定义 Converter — 存储自定义 code
@Convert(converter = UserStatusConverter.class)
@Column(length = 20)
private UserStatus status;  // 数据库存 "active"

@Converter
public class UserStatusConverter implements AttributeConverter<UserStatus, String> {
    
    @Override
    public String convertToDatabaseColumn(UserStatus attribute) {
        return attribute == null ? null : attribute.getCode();
    }
    
    @Override
    public UserStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : UserStatus.fromCode(dbData);
    }
}
```

---

## 8. 实体映射与关系

### 8.1 关联关系总览

```java
// ========== JPA 关联关系 ==========
// 
// @OneToOne      — 一对一 (User ↔ UserProfile)
// @OneToMany     — 一对多 (User ↔ Order)
// @ManyToOne     — 多对一 (Order ↔ User)
// @ManyToMany    — 多对多 (Student ↔ Course)
//
// 必须指定关系的维护方和反向方：
// - 维护方 (owning side): 包含外键的那一方
// - 反向方 (inverse side): mappedBy 指向的一方
```

### 8.2 @OneToOne (一对一)

```java
// ========== @OneToOne — 用户与用户详情 ==========

// 维护方 (User — 包含 user_detail_id 外键)
@Entity
@Table(name = "users")
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "detail_id", referencedColumnName = "id")
    // 创建外键 user_detail_id → user_details.id
    private UserDetail detail;
}

// 反向方 (UserDetail — mappedBy 指向 User 的 detail 属性)
@Entity
@Table(name = "user_details")
public class UserDetail {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne(mappedBy = "detail")  // 由 User.detail 维护关系
    private User user;
}
```

### 8.3 @OneToMany / @ManyToOne (一对多)

```java
// ========== @OneToMany / @ManyToOne — 用户与订单 ==========

// "多"的一方 (Order) — 维护方, 包含外键
@Entity
@Table(name = "orders")
public class Order {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private BigDecimal totalAmount;
    
    @ManyToOne(fetch = FetchType.LAZY)  // 多对一, 懒加载
    @JoinColumn(name = "user_id")       // 外键 user_id → users.id
    private User user;
}

// "一"的一方 (User) — 反向方, mappedBy
@Entity
@Table(name = "users")
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    // mappedBy = "user" → 由 Order.user 字段维护关系
    // orphanRemoval = true → 移除子项时自动删除数据库记录
    private List<Order> orders = new ArrayList<>();
    
    // 辅助方法 — 维护双方数据一致性
    public void addOrder(Order order) {
        orders.add(order);
        order.setUser(this);
    }
    
    public void removeOrder(Order order) {
        orders.remove(order);
        order.setUser(null);
    }
}
```

### 8.4 @ManyToMany (多对多)

```java
// ========== @ManyToMany — 学生与课程 ==========

@Entity
@Table(name = "students")
public class Student {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToMany
    @JoinTable(
        name = "student_courses",  // 中间表
        joinColumns = @JoinColumn(name = "student_id"),    // 当前表的外键
        inverseJoinColumns = @JoinColumn(name = "course_id")  // 对方表的外键
    )
    private Set<Course> courses = new HashSet<>();
}

@Entity
@Table(name = "courses")
public class Course {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToMany(mappedBy = "courses")  // 由 Student.courses 维护
    private Set<Student> students = new HashSet<>();
}

// ========== 带额外字段的中间表 (推荐) ==========
// 如果中间表需要额外字段 (如选课时间, 成绩), 使用 @OneToMany + 实体类

@Entity
@Table(name = "student_courses")
public class StudentCourse {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private Student student;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;
    
    private LocalDateTime enrolledAt;
    private Integer score;
}

@Entity
public class Student {
    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL)
    private List<StudentCourse> enrollments = new ArrayList<>();
}
```

### 8.5 CascadeType 详解

```java
// ========== CascadeType 级联操作 ==========
// 级联: 对当前实体的操作传播到关联实体

@Entity
public class Order {
    
    // 常用的级联类型:
    
    // CascadeType.ALL — 所有操作级联 (最常用)
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items;
    
    // CascadeType.PERSIST — 保存时级联
    @OneToMany(cascade = CascadeType.PERSIST)
    private List<Attachment> attachments;
    
    // CascadeType.MERGE — 更新时级联
    @OneToMany(cascade = CascadeType.MERGE)
    private List<Tag> tags;
    
    // CascadeType.REMOVE — 删除时级联
    @OneToMany(cascade = CascadeType.REMOVE)
    private List<Log> logs;
    
    // CascadeType.REFRESH — 刷新时级联 (从数据库重新读取)
    @OneToMany(cascade = CascadeType.REFRESH)
    private List<CacheItem> cacheItems;
    
    // CascadeType.DETACH — 分离时级联 (从持久化上下文移除)
    @OneToMany(cascade = CascadeType.DETACH)
    private List<AuditRecord> auditRecords;
}

// ========== 级联使用示例 ==========
@Service
public class OrderService {
    
    @Transactional
    public Order createOrder(OrderRequest request) {
        Order order = new Order();
        order.setTotalAmount(request.getAmount());
        
        // 级联保存: 只需要保存 Order, OrderItem 自动保存
        request.getItems().forEach(item -> {
            OrderItem orderItem = new OrderItem();
            orderItem.setProductName(item.getProductName());
            orderItem.setQuantity(item.getQuantity());
            orderItem.setPrice(item.getPrice());
            order.addItem(orderItem);  // 双向维护
        });
        
        return orderRepository.save(order);
        // 级联: INSERT order + INSERT 3 order_items
    }
    
    @Transactional
    public void deleteOrder(Long id) {
        Order order = orderRepository.findById(id).orElseThrow();
        orderRepository.delete(order);
        // 级联: DELETE order_items WHERE order_id = ? + DELETE order
    }
}
```

### 8.6 FetchType 与 N+1 问题

```java
// ========== FetchType 策略 ==========

// FetchType.LAZY (懒加载, 推荐)
//   ● 使用代理对象
//   ● 只在访问时才查询数据库
//   ● @ManyToOne 默认 LAZY
//   ● @OneToMany 默认 LAZY
@ManyToOne(fetch = FetchType.LAZY)
private User user;

// FetchType.EAGER (立即加载, 谨慎使用)
//   ● 查询主实体时同时 JOIN 查询关联实体
//   ● @OneToOne 默认 EAGER
//   ● @ManyToMany 默认 LAZY
@OneToMany(fetch = FetchType.EAGER)
private List<Order> orders;

// ========== N+1 查询问题 ==========
// 问题场景: 查询 1 个用户 + N 个订单的详情
// 执行 1 + N 次 SQL 查询!

// 代码:
List<User> users = userRepository.findAll();  // 1 次查询
for (User user : users) {
    System.out.println(user.getOrders().size());  // N 次查询!
}

// 为什么 N+1 是性能杀手?
// 用户量 1000 → 1 + 1000 = 1001 次 SQL 查询!
// 用户量 100000 → 100001 次查询!

// ========== 解决方案: 见第 12 章 ==========
```

---

## 9. JPQL 与 Criteria API

### 9.1 JPQL (Java Persistence Query Language)

```java
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    // ========== JPQL 查询 ==========
    
    // 基本查询
    @Query("SELECT o FROM Order o WHERE o.status = :status")
    List<Order> findByStatus(@Param("status") OrderStatus status);
    
    // 多条件
    @Query("SELECT o FROM Order o WHERE o.user.id = :userId " +
           "AND o.totalAmount >= :minAmount " +
           "ORDER BY o.createdAt DESC")
    List<Order> findUserOrders(
            @Param("userId") Long userId, 
            @Param("minAmount") BigDecimal minAmount);
    
    // 关联查询 (JOIN FETCH 解决 N+1)
    @Query("SELECT o FROM Order o JOIN FETCH o.items WHERE o.id = :id")
    Optional<Order> findByIdWithItems(@Param("id") Long id);
    
    // 投影查询 (只查询部分字段)
    @Query("SELECT new com.example.dto.OrderSummary(o.id, o.totalAmount, o.status) " +
           "FROM Order o WHERE o.user.id = :userId")
    List<OrderSummary> findOrderSummaries(@Param("userId") Long userId);
    
    // 聚合查询
    @Query("SELECT o.status, COUNT(o), SUM(o.totalAmount) " +
           "FROM Order o GROUP BY o.status")
    List<Object[]> aggregateByStatus();
    
    // 分页 + 排序
    @Query("SELECT o FROM Order o WHERE o.user.id = :userId")
    Page<Order> findByUserId(@Param("userId") Long userId, Pageable pageable);
    
    // 模糊查询
    @Query("SELECT o FROM Order o WHERE o.orderNo LIKE %:keyword%")
    List<Order> searchByOrderNo(@Param("keyword") String keyword);
    
    // 原生 SQL (Native Query)
    @Query(value = "SELECT * FROM orders WHERE DATE(created_at) = :date", 
           nativeQuery = true)
    List<Order> findByDate(@Param("date") LocalDate date);
    
    // 更新操作 (需要 @Modifying + @Transactional)
    @Modifying
    @Query("UPDATE Order o SET o.status = :newStatus " +
           "WHERE o.id IN :ids")
    int batchUpdateStatus(
            @Param("ids") List<Long> ids, 
            @Param("newStatus") OrderStatus newStatus);
    
    // 删除操作
    @Modifying
    @Query("DELETE FROM Order o WHERE o.createdAt < :before")
    int deleteOldOrders(@Param("before") LocalDateTime before);
}
```

### 9.2 Criteria API (类型安全查询)

```java
// ========== Criteria API ==========
// 类型安全, 编译期检查
// 适合动态查询条件

@Service
public class OrderCriteriaService {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    public List<Order> findOrdersByCriteria(OrderSearchCriteria criteria) {
        // 1. 创建 CriteriaBuilder
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        
        // 2. 创建 CriteriaQuery
        CriteriaQuery<Order> query = cb.createQuery(Order.class);
        
        // 3. 定义根实体
        Root<Order> root = query.from(Order.class);
        
        // 4. 构建 Predicate 列表
        List<Predicate> predicates = new ArrayList<>();
        
        if (criteria.getUserId() != null) {
            predicates.add(cb.equal(root.get("user").get("id"), criteria.getUserId()));
        }
        
        if (criteria.getStatus() != null) {
            predicates.add(cb.equal(root.get("status"), criteria.getStatus()));
        }
        
        if (criteria.getMinAmount() != null) {
            predicates.add(cb.greaterThanOrEqualTo(
                root.get("totalAmount"), criteria.getMinAmount()));
        }
        
        if (criteria.getMaxAmount() != null) {
            predicates.add(cb.lessThanOrEqualTo(
                root.get("totalAmount"), criteria.getMaxAmount()));
        }
        
        if (criteria.getStartDate() != null) {
            predicates.add(cb.greaterThanOrEqualTo(
                root.get("createdAt"), criteria.getStartDate()));
        }
        
        if (criteria.getEndDate() != null) {
            predicates.add(cb.lessThanOrEqualTo(
                root.get("createdAt"), criteria.getEndDate()));
        }
        
        // 5. 组合条件
        query.where(predicates.toArray(new Predicate[0]));
        
        // 6. 排序
        query.orderBy(cb.desc(root.get("createdAt")));
        
        // 7. 执行查询
        return entityManager.createQuery(query).getResultList();
    }
    
    // 分页版本
    public Page<Order> findOrdersPage(OrderSearchCriteria criteria, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        
        // 查询条件
        CriteriaQuery<Order> query = cb.createQuery(Order.class);
        Root<Order> root = query.from(Order.class);
        query.where(buildPredicates(cb, root, criteria));
        query.orderBy(buildOrders(cb, root, pageable));
        
        // 计数查询
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Order> countRoot = countQuery.from(Order.class);
        countQuery.select(cb.count(countRoot));
        countQuery.where(buildPredicates(cb, countRoot, criteria));
        
        Long totalCount = entityManager.createQuery(countQuery).getSingleResult();
        
        // 分页结果
        TypedQuery<Order> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());
        
        List<Order> content = typedQuery.getResultList();
        
        return new PageImpl<>(content, pageable, totalCount);
    }
    
    private Predicate[] buildPredicates(CriteriaBuilder cb, Root<?> root, 
                                        OrderSearchCriteria criteria) {
        List<Predicate> predicates = new ArrayList<>();
        // ... 同上
        return predicates.toArray(new Predicate[0]);
    }
    
    private List<Order> buildOrders(CriteriaBuilder cb, Root<?> root, Pageable pageable) {
        // 解析 Sort 并转换为 CriteriaBuilder Order
        // ...
        return List.of(cb.desc(root.get("createdAt")));
    }
}

@Data
public class OrderSearchCriteria {
    private Long userId;
    private OrderStatus status;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
```

### 9.3 QueryDSL (第三方类型安全查询)

```xml
<dependency>
    <groupId>com.querydsl</groupId>
    <artifactId>querydsl-jpa</artifactId>
    <classifier>jakarta</classifier>
</dependency>
<dependency>
    <groupId>com.querydsl</groupId>
    <artifactId>querydsl-apt</artifactId>
    <classifier>jakarta</classifier>
    <scope>provided</scope>
</dependency>
```

```java
// ========== QueryDSL — 类型安全查询 (比 Criteria API 更简洁) ==========
// 自动生成 Q 类 (QUser, QOrder 等)

// 查询示例:
@Service
public class QueryDslUserService {
    
    private final JPAQueryFactory queryFactory;
    
    public QueryDslUserService(EntityManager entityManager) {
        this.queryFactory = new JPAQueryFactory(entityManager);
    }
    
    public List<User> findActiveUsers(String name, Integer status) {
        QUser user = QUser.user;
        
        return queryFactory.selectFrom(user)
                .where(user.deleted.isFalse(),
                       name != null ? user.userName.contains(name) : null,
                       status != null ? user.status.eq(status) : null)
                .orderBy(user.createdAt.desc())
                .limit(20)
                .fetch();
    }
    
    public long countUsersByStatus(UserStatus status) {
        QUser user = QUser.user;
        
        return queryFactory.select(user.count())
                .from(user)
                .where(user.status.eq(status))
                .fetchOne();
    }
}
```

---

## 10. Spring Data JPA

### 10.1 Repository 层次

```java
// ========== Spring Data JPA Repository 层次 ==========
//
// Repository<T, ID> (标记接口)
//   │
//   ├── CrudRepository<T, ID> (CRUD 操作)
//   │   ├── save(S), saveAll(Iterable<S>)
//   │   ├── findById(ID), existsById(ID)
//   │   ├── findAll(), findAllById(Iterable<ID>)
//   │   ├── count()
//   │   ├── deleteById(ID), delete(T), deleteAll()
//   │
//   ├── PagingAndSortingRepository<T, ID> (分页 + 排序)
//   │   ├── findAll(Sort)
//   │   ├── findAll(Pageable)
//   │
//   └── JpaRepository<T, ID> (JPA 特有)
//       ├── findAll(Sort), findAll(Pageable)
//       ├── flush(), saveAndFlush(), deleteInBatch()
//       ├── getOne(ID) (已废弃)
//       ├── getReferenceById(ID) (3.x)
//       └── findAll(Specification)

// 产品线: 通常继承 JpaRepository (集合了所有功能)
public interface UserRepository extends JpaRepository<User, Long> {
    // 自动获得: save, findById, findAll, count, delete, 分页, 排序...
}
```

### 10.2 方法命名查询

```java
// ========== 方法命名查询 ==========
// 根据方法名自动生成 SQL (不需要 @Query)
// 约定: findBy + 字段名 + 操作符

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    // ─── 基础查询 ───
    
    // = 查询
    Optional<User> findByEmail(String email);
    User findByUserName(String userName);
    
    // AND 条件
    Optional<User> findByUserNameAndEmail(String userName, String email);
    
    // OR 条件
    List<User> findByUserNameOrEmail(String userName, String email);
    
    // 不等于
    List<User> findByStatusNot(Integer status);
    
    // ─── 模糊查询 ───
    
    // LIKE (全模糊)
    List<User> findByUserNameLike(String pattern);
    
    // LIKE (以...开头)
    List<User> findByUserNameStartingWith(String prefix);
    
    // LIKE (以...结尾)
    List<User> findByUserNameEndingWith(String suffix);
    
    // LIKE (包含)
    List<User> findByUserNameContaining(String keyword);
    
    // ─── 范围查询 ───
    
    // GreaterThan (>)
    List<User> findByAgeGreaterThan(int age);
    
    // LessThan (<)
    List<User> findByAgeLessThan(int age);
    
    // Between (BETWEEN)
    List<User> findByAgeBetween(int min, int max);
    
    // In (IN)
    List<User> findByIdIn(List<Long> ids);
    
    // ─── 空值判断 ───
    
    List<User> findByPhoneIsNull();
    List<User> findByPhoneIsNotNull();
    
    // ─── 日期查询 ───
    
    List<User> findByCreatedAtAfter(LocalDateTime date);
    List<User> findByCreatedAtBefore(LocalDateTime date);
    List<User> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    
    // ─── 排序 ───
    
    List<User> findByStatusOrderByCreatedAtDesc(Integer status);
    List<User> findByStatus(Integer status, Sort sort);  // 参数排序
    
    // ─── 分页 ───
    
    Page<User> findByStatus(Integer status, Pageable pageable);
    Slice<User> findByCreatedAtAfter(LocalDateTime date, Pageable pageable);
    // Page vs Slice: Page 有总记录数 (count), Slice 只有是否有下一页
    
    // ─── 限制 ───
    
    List<User> findTop10ByOrderByCreatedAtDesc();
    Optional<User> findFirstByOrderByCreatedAtDesc();
    
    // ─── 计数/删除 ───
    
    long countByStatus(Integer status);
    boolean existsByEmail(String email);
    int deleteByUserName(String userName);  // 需要 @Transactional + @Modifying
}

// ========== 关键字表 ==========
// And          — findByUserNameAndEmail
// Or           — findByUserNameOrEmail
// Is, Equals  — findByUserName, findByIdIs
// Between     — findByAgeBetween
// LessThan    — findByAgeLessThan
// GreaterThan — findByAgeGreaterThan
// After       — findByCreatedAtAfter
// Before      — findByCreatedAtBefore
// IsNull      — findByPhoneIsNull
// IsNotNull   — findByPhoneIsNotNull
// Like        — findByUserNameLike
// NotLike     — findByUserNameNotLike
// StartingWith — findByNameStartingWith
// EndingWith  — findByNameEndingWith
// Containing  — findByNameContaining
// OrderBy     — findByNameOrderByAgeDesc
// Not         — findByNameNot
// In          — findByIdIn
// NotIn       — findByIdNotIn
// True        — findByActiveTrue
// False       — findByActiveFalse
// IgnoreCase  — findByNameIgnoreCase
```

### 10.3 分页与排序

```java
// ========== 分页查询 ==========
@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    
    @Autowired
    private UserRepository userRepository;
    
    // ========== 服务端分页 (推荐) ==========
    @GetMapping
    public Result<Page<User>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String[] sort) {
        
        // 构建排序条件
        List<Sort.Order> orders = new ArrayList<>();
        for (String sortParam : sort) {
            String[] parts = sortParam.split(",");
            Sort.Direction direction = parts.length > 1 && "desc".equals(parts[1])
                ? Sort.Direction.DESC : Sort.Direction.ASC;
            orders.add(new Sort.Order(direction, parts[0]));
        }
        
        // 构建 Pageable (Spring Data 自动处理 COUNT + LIMIT)
        Pageable pageable = PageRequest.of(page, size, Sort.by(orders));
        
        // 查询
        Page<User> result = userRepository.findAll(pageable);
        
        return Result.success(result);
    }
    
    @GetMapping("/search")
    public Result<Page<User>> search(
            @RequestParam(required = false) String keyword,
            Pageable pageable) {  // Spring Data 自动解析 page, size, sort 参数
        
        // Pageable 参数: ?page=0&size=20&sort=createdAt,desc&sort=id,asc
        if (keyword != null) {
            return Result.success(userRepository.findByUserNameContaining(keyword, pageable));
        }
        return Result.success(userRepository.findAll(pageable));
    }
}

// ========== 自定义分页查询 ==========
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    // 方法命名分页
    Page<User> findByStatus(Integer status, Pageable pageable);
    Slice<User> findByUserNameContaining(String keyword, Pageable pageable);
    
    // JPQL 分页
    @Query("SELECT u FROM User u WHERE u.status = :status")
    Page<User> findByStatusCustom(@Param("status") Integer status, Pageable pageable);
    
    // 复杂分页: 使用 Specification
    Page<User> findAll(Specification<User> spec, Pageable pageable);
}
```

### 10.4 JPA Auditing (审计)

```java
// ========== JPA 审计 ==========
// 自动填充创建时间、更新时间、创建人、更新人

// 1. 启用审计
@Configuration
@EnableJpaAuditing  // 开启 JPA 审计
public class JpaConfig {
    
    @Bean
    public AuditorAware<String> auditorAware() {
        // 从 SecurityContext 或 RequestContext 中获取当前用户
        return () -> {
            // 实际从 SecurityContextHolder 获取
            String currentUser = SecurityContextHolder.getContext()
                .getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getName()
                : "system";
            return Optional.of(currentUser);
        };
    }
}

// 2. 创建 BaseEntity (可复用)
@Data
@MappedSuperclass  // 不映射为表, 子类继承字段
@EntityListeners(AuditingEntityListener.class)  // 开启审计监听
public abstract class BaseEntity {
    
    @CreatedDate  // 创建时自动填充
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;
    
    @LastModifiedDate  // 更新时自动填充
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @CreatedBy  // 创建人
    @Column(name = "created_by", updatable = false)
    private String createdBy;
    
    @LastModifiedBy  // 更新人
    @Column(name = "updated_by")
    private String updatedBy;
}

// 3. 实体继承 BaseEntity
@Entity
@Table(name = "users")
public class User extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String userName;
    private String email;
    // 自动获得: createdAt, updatedAt, createdBy, updatedBy
}
```

### 10.5 Specification (动态查询)

```java
// ========== Specification 动态查询 ==========
// 在不写 JPQL/Criteria API 代码的情况下实现动态查询

// 1. Repository 继承 JpaSpecificationExecutor
@Repository
public interface UserRepository extends 
        JpaRepository<User, Long>, 
        JpaSpecificationExecutor<User> {  // 添加 Specification 支持
    // 自动获得: findAll(Specification), count(Specification)
}

// 2. 创建 Specification 工厂
public class UserSpecifications {
    
    public static Specification<User> hasUserName(String userName) {
        return (root, query, cb) -> {
            if (userName == null || userName.isBlank()) {
                return cb.conjunction();  // 无条件
            }
            return cb.like(root.get("userName"), "%" + userName + "%");
        };
    }
    
    public static Specification<User> hasEmail(String email) {
        return (root, query, cb) -> {
            if (email == null || email.isBlank()) {
                return cb.conjunction();
            }
            return cb.equal(root.get("email"), email);
        };
    }
    
    public static Specification<User> hasStatus(Integer status) {
        return (root, query, cb) -> {
            if (status == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("status"), status);
        };
    }
    
    public static Specification<User> createdAfter(LocalDateTime date) {
        return (root, query, cb) -> {
            if (date == null) {
                return cb.conjunction();
            }
            return cb.greaterThanOrEqualTo(root.get("createdAt"), date);
        };
    }
    
    public static Specification<User> createdBefore(LocalDateTime date) {
        return (root, query, cb) -> {
            if (date == null) return cb.conjunction();
            return cb.lessThanOrEqualTo(root.get("createdAt"), date);
        };
    }
}

// 3. 使用
@Service
public class UserSpecService {
    
    @Autowired
    private UserRepository userRepository;
    
    public Page<User> search(UserSearchRequest request, Pageable pageable) {
        // 组合多个 Specification
        Specification<User> spec = Specification
            .where(UserSpecifications.hasUserName(request.getUserName()))
            .and(UserSpecifications.hasEmail(request.getEmail()))
            .and(UserSpecifications.hasStatus(request.getStatus()))
            .and(UserSpecifications.createdAfter(request.getStartDate()))
            .and(UserSpecifications.createdBefore(request.getEndDate()));
        
        // 所有条件自动组合为 WHERE ... AND ... AND ...
        return userRepository.findAll(spec, pageable);
    }
}

// 4. 泛型 Specification 构建器
public class SpecificationBuilder<T> {
    
    private final List<Specification<T>> specs = new ArrayList<>();
    
    public SpecificationBuilder<T> and(Specification<T> spec) {
        specs.add(spec);
        return this;
    }
    
    public Specification<T> build() {
        return specs.stream()
            .reduce(Specification.where(null), Specification::and);
    }
}

// 使用:
Specification<User> spec = new SpecificationBuilder<User>()
    .and((root, query, cb) -> cb.equal(root.get("status"), 1))
    .and((root, query, cb) -> cb.like(root.get("userName"), "%test%"))
    .build();
```

---

## 11. 事务管理

### 11.1 @Transactional 详解

```java
// ========== @Transactional 参数详解 ==========
@Service
@Transactional(readOnly = true)  // 类级别: 默认只读事务
public class TransactionalUserService {
    
    @Autowired
    private UserRepository userRepository;
    
    // 只读查询 (性能优化)
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }
    
    // 写事务 — 覆盖类级别配置
    @Transactional(readOnly = false, rollbackFor = Exception.class)
    public User create(User user) {
        return userRepository.save(user);
    }
    
    // 事务传播行为 (Propagation)
    @Transactional(propagation = Propagation.REQUIRED)  // 默认: 支持当前事务, 不存在则创建
    public void methodA() { }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)  // 挂起当前事务, 创建新事务
    public void methodB() { }
    
    @Transactional(propagation = Propagation.NESTED)  // 嵌套事务 (Savepoint)
    public void methodC() { }
    
    @Transactional(propagation = Propagation.MANDATORY)  // 必须在已有事务中运行
    public void methodD() { }
    
    @Transactional(propagation = Propagation.NEVER)  // 不能在事务中运行
    public void methodE() { }
    
    @Transactional(propagation = Propagation.SUPPORTS)  // 支持当前事务, 没有则不创建
    public void methodF() { }
    
    @Transactional(propagation = Propagation.NOT_SUPPORTED)  // 挂起当前事务, 无事务运行
    public void methodG() { }
    
    // 隔离级别 (Isolation)
    @Transactional(isolation = Isolation.READ_COMMITTED)  // 默认: 已提交读
    public void withIsolation() {
        // READ_UNCOMMITTED: 脏读 (最低)
        // READ_COMMITTED: 不可重复读 (MySQL 默认)
        // REPEATABLE_READ: 幻读 (MySQL InnoDB 默认)
        // SERIALIZABLE: 无并发问题 (性能差)
    }
    
    // 事务超时 (秒)
    @Transactional(timeout = 30)
    public void withTimeout() { }
    
    // 回滚条件
    @Transactional(
        noRollbackFor = IllegalArgumentException.class,  // 某些异常不回滚
        rollbackFor = Exception.class                     // 所有异常都回滚 (默认只回滚 RuntimeException)
    )
    public void withRollbackRules() { }
}
```

### 11.2 事务传播行为示例

```java
// ========== REQUIRED (默认) ==========
// 业务: 创建订单 + 扣减库存 — 要么全成功, 要么全回滚

@Service
public class OrderService {
    
    @Autowired
    private InventoryService inventoryService;
    
    @Transactional
    public Order createOrder(OrderRequest request) {
        // 1. 创建订单 (当前事务)
        Order order = saveOrder(request);
        
        // 2. 扣减库存 (同一个事务 — REQUIRED)
        inventoryService.deduct(request.getProductId(), request.getQuantity());
        
        return order;
    }
}

@Service
public class InventoryService {
    @Transactional(propagation = Propagation.REQUIRED)  // 默认, 加入调用方的事务
    public void deduct(Long productId, int quantity) {
        // 如果 OrderService 事务回滚, 这里的操作也回滚
        // 库存扣减不会生效
    }
}

// ========== REQUIRES_NEW ==========
// 业务: 创建订单 + 记录操作日志 — 日志独立事务

@Service
public class OrderService {
    
    @Autowired
    private AuditLogService auditLogService;
    
    @Transactional
    public Order createOrder(OrderRequest request) {
        Order order = saveOrder(request);
        
        // 即使订单创建失败, 日志也要记录
        try {
            auditLogService.log("ORDER_CREATE", request);
        } catch (Exception e) {
            // 日志失败不影响主流程
            log.warn("日志记录失败", e);
        }
        
        return order;
    }
}

@Service
public class AuditLogService {
    @Transactional(propagation = Propagation.REQUIRES_NEW)  // 独立事务
    public void log(String action, Object payload) {
        // 这个操作在新事务中执行
        // 即使外层事务回滚, 这个日志也会被保存
    }
}
```

### 11.3 @Transactional 常见陷阱

```java
// ========== 陷阱 1: 自调用导致事务失效 ==========
@Service
public class UserService {
    
    @Transactional
    public void methodA() {
        // 事务生效
        userRepository.save(new User("John"));
    }
    
    public void methodB() {
        // 内部调用 methodA — 事务不生效!
        this.methodA();  // ❌ 自调用不经过代理
    }
    
    // 解决: 注入自身代理
    @Autowired
    private UserService self;
    
    public void methodC() {
        self.methodA();  // ✅ 经过代理, 事务生效
    }
}

// ========== 陷阱 2: 异常被捕获导致不回滚 ==========
@Transactional
public void createUser(User user) {
    try {
        userRepository.save(user);
        // 发生异常
        throw new RuntimeException("数据库错误");
    } catch (Exception e) {
        // ❌ 异常被捕获了! 事务不会回滚!
        log.error("Error", e);
    }
    // 结果: 用户被保存了, 事务没回滚! 
}

// 正确做法: 抛出异常或手动回滚
@Transactional
public void createUser(User user) {
    userRepository.save(user);
    // 让异常传播出去, 触发事务回滚
}

// ========== 陷阱 3: 事务方法必须是 public ==========
@Transactional
private void privateMethod() {  // ❌ 不生效
    // 事务在代理类上增强, private 方法不被代理
}
```

---

## 12. 性能优化与 N+1 解决

### 12.1 N+1 查询问题的完整解决

```java
// ========== 问题复现 ==========
@Entity
public class User {
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<Order> orders;
}

// Controller
@GetMapping("/users")
public List<User> listUsers() {
    List<User> users = userRepository.findAll();  // 1 次查询: SELECT * FROM users
    for (User user : users) {
        // 每个用户触发 1 次查询: SELECT * FROM orders WHERE user_id = ?
        System.out.println("Orders: " + user.getOrders().size());  // N 次查询
    }
    // 总计: 1 + N 次 SQL 查询
}

// ========== 解决方案 1: JOIN FETCH (推荐) ==========
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.orders")
    List<User> findAllWithOrders();
    // 生成一条 SQL: SELECT u.*, o.* FROM users u LEFT JOIN orders o ON u.id = o.user_id
    // 只执行 1 次 SQL!
}

// ========== 解决方案 2: @EntityGraph ==========
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    @EntityGraph(attributePaths = {"orders"})
    @Query("SELECT u FROM User u")
    List<User> findAllWithOrdersUsingEntityGraph();
    // 效果同 JOIN FETCH, 使用 @NamedEntityGraph
}

// 在实体上定义:
@Entity
@NamedEntityGraph(name = "User.orders", attributeNodes = @NamedAttributeNode("orders"))
public class User { ... }

// ========== 解决方案 3: 批量获取 (Batch Size) ==========
// 配置批量加载大小
@Entity
public class User {
    
    @OneToMany(mappedBy = "user")
    @BatchSize(size = 10)  // 每批加载 10 个用户的订单
    private List<Order> orders;
}

// application.yml:
// spring:
//   jpa:
//     properties:
//       hibernate.default_batch_fetch_size: 10
//
// 效果: 如果查询 20 个用户, N+1 从 1+20=21 优化为 1+2=3
// 第一次: SELECT * FROM users
// 第二次: SELECT * FROM orders WHERE user_id IN (?,?,...,?) (10 个)
// 第三次: SELECT * FROM orders WHERE user_id IN (?,?,...,?) (10 个)

// ========== 解决方案 4: DTO 投影 ==========
// 只查询需要的字段, 避免加载关联
@Query("SELECT new com.example.dto.UserOrderCount(u.id, u.userName, COUNT(o)) " +
       "FROM User u LEFT JOIN u.orders o GROUP BY u.id")
List<UserOrderCount> findUserOrderCounts();

// ========== 解决方案 5: 查询后批量初始化 ==========
@Service
public class UserService {
    
    @Transactional
    public List<User> findAllWithOrders() {
        List<User> users = userRepository.findAll();
        // 批量初始化 orders (生成 IN 查询)
        Hibernate.initialize(users.get(0).getOrders());  // 触发生成 IN 查询
        // 实际使用:
        for (User user : users) {
            user.getOrders().size();  // 直接使用缓存, 不再查库
        }
        return users;
    }
}
```

### 12.2 批量操作优化

```java
// ========== 批量插入优化 ==========
// application.yml:
// spring.jpa.properties.hibernate.jdbc.batch_size=25
// spring.jpa.properties.hibernate.order_inserts=true
// spring.jpa.properties.hibernate.order_updates=true

// 批量插入:
@Transactional
public void batchInsert(List<User> users) {
    // 每 25 条 flush + clear 一次
    for (int i = 0; i < users.size(); i++) {
        entityManager.persist(users.get(i));
        if (i % 25 == 0) {
            entityManager.flush();  // 提交批处理
            entityManager.clear();  // 清空持久化上下文
        }
    }
}

// Spring Data JPA 批量保存:
@Transactional
public void batchSave(List<User> users) {
    List<List<User>> batches = Lists.partition(users, 25);
    for (List<User> batch : batches) {
        userRepository.saveAll(batch);
        userRepository.flush();
    }
}

// ========== 批量更新 ==========
@Modifying
@Query("UPDATE User u SET u.status = :status " +
       "WHERE u.lastLoginAt < :before")
int batchDeactivateUsers(@Param("status") Integer status, 
                         @Param("before") LocalDateTime before);

// ========== 只读查询优化 ==========
@Transactional(readOnly = true)
public List<User> searchUsers() {
    // readOnly = true 表示:
    // 1. 不会脏检查 (性能提升)
    // 2. 可以设置 FlushMode.MANUAL (避免不必要的 flush)
    // 3. 底层可以优化 (如只读连接)
    return userRepository.findAll();
}
```

### 12.3 DTO 投影

```java
// ========== DTO 投影 ==========
// 只查询需要的字段, 而不是整个实体

// 方式 1: 接口投影 (推荐 Spring Data)
public interface UserNameOnly {
    String getUserName();
    String getEmail();
    // 也可以返回表达式
    @Value("#{target.userName + ' <' + target.email + '>'}")
    String getDisplayName();
}

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // 返回接口投影
    List<UserNameOnly> findByStatus(Integer status);
}

// 方式 2: 类投影 (DTO)
public class UserSummary {
    private final Long id;
    private final String userName;
    private final String email;
    
    // 必须有带参构造器
    public UserSummary(Long id, String userName, String email) {
        this.id = id;
        this.userName = userName;
        this.email = email;
    }
    
    // getters...
}

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    @Query("SELECT new com.example.dto.UserSummary(u.id, u.userName, u.email) " +
           "FROM User u WHERE u.status = :status")
    List<UserSummary> findSummariesByStatus(@Param("status") Integer status);
}

// 方式 3: Tuple
@Query("SELECT u.id AS id, u.userName AS name FROM User u")
List<Tuple> findUserTuples();
// Tuple t = result.get(0);
// Long id = t.get("id", Long.class);
// String name = t.get("name", String.class);
```

---

## 13. MyBatis vs JPA 对比

### 13.1 核心差异

```
┌─────────────────────────────────────────────────────────────────────┐
│              MyBatis vs JPA/Hibernate 核心差异                       │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  对比维度        | MyBatis                | JPA/Hibernate           │
│  ────────────────|────────────────────────|──────────────────────────│
│  SQL 控制        | 完全控制, 手写 SQL     | 自动生成 SQL             │
│  开发效率 (CRUD) | 需要写 SQL/Mapper      | 零 SQL, 方法命名即可    │
│  复杂查询        | 强项, 动态 SQL 灵活    | JPQL/Criteria 较复杂    │
│  性能调优        | 精确控制每条 SQL       | 需要了解 Hibernate 优化  │
│  学习曲线        | 低 (SQL 基础)          | 高 (JPA 概念多)         │
│  缓存            | 简单, 一级+二级        | 完善, 一级+二级+查询缓存 │
│  批量操作        | 灵活                   | 需要特殊配置             │
│  实体关系        | 半自动映射             | 全自动 ORM               │
│  国内流行度      | 高 (互联网公司)        | 一般 (外企/传统行业)     │
│  国外流行度      | 一般                   | 高 (主流)                │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

### 13.2 场景选择指南

```java
// ========== 何时选择 MyBatis ==========

// 1. 复杂 SQL 查询
//    多表关联, 子查询, 聚合查询, 报表
//    需要精确控制 SQL 执行计划
//    例: 订单报表, 数据统计

// 2. 数据库特定功能
//    MySQL/PostgreSQL 特定语法, 窗口函数, 全文索引
//    例: 地理位置查询, JSON 字段查询

// 3. 已有 SQL 团队
//    DBA 团队提供优化后的 SQL
//    需要快速迁移现有 SQL

// 4. 高性能要求
//    需要精细的 SQL 优化
//    SQL 调优经验丰富的团队


// ========== 何时选择 JPA ==========

// 1. CRUD 密集型应用
//    管理后台, CMS, 内部系统
//    实体关系复杂, 关联多

// 2. 标准化的项目
//    需要 JPA 规范的可移植性
//    团队熟悉面向对象设计

// 3. 快速原型开发
//    JPA 的自动建表 + 方法命名查询
//    显著提升初期开发效率

// 4. 代码生成友好
//    Spring Data REST 自动生成 REST API
//    QueryDSL 类型安全查询
```

### 13.3 选择决策树

```
选择 MyBatis 还是 JPA?

你的项目类型?
│
├── 数据报表/统计分析/大数据查询
│   └──→ MyBatis (SQL 完全控制)
│
├── 标准 CRUD + 复杂业务逻辑
│   │
│   ├── 团队 SQL 能力强?
│   │   ├── 是 → MyBatis
│   │   └── 否 → JPA
│   │
│   ├── 需要快速开发?
│   │   ├── 是 → JPA (方法命名查询)
│   │   └── 否 → MyBatis
│   │
│   ├── 实体关系复杂 (10+ 关联)?
│   │   ├── 是 → JPA (自动关系管理)
│   │   └── 否 → MyBatis
│   │
│   └── 性能要求极高 (万级 QPS)?
│       ├── 是 → MyBatis (精细 SQL)
│       └── 否 → JPA (够用)
│
└── 项目同时有 CRUD 和报表?
    └──→ 两者结合 (JPA + MyBatis)
```

---

## 14. 企业实践: 两者结合使用

### 14.1 混合架构

```java
// ========== 企业实践: JPA + MyBatis 混合使用 ==========
//
// JPA 负责:
//   - 标准 CRUD 操作
//   - 实体关系管理
//   - 自动建表
//   - 事务管理
//
// MyBatis 负责:
//   - 复杂查询 (多表关联, 报表)
//   - 批量操作 (大批量插入/更新)
//   - 性能敏感的 SQL

// 项目结构:
// com.example.project/
// ├── entity/          ← JPA 实体
// ├── repository/      ← Spring Data JPA Repository
// ├── mapper/          ← MyBatis Mapper (复杂查询)
// └── service/
//     ├── UserService.java  (使用 JPA)
//     ├── OrderService.java (使用 JPA)
//     └── ReportService.java (使用 MyBatis)

// ========== 配置 ==========
// JPA 配置 (自动建表, 简单 CRUD)
// MyBatis 配置 (复杂查询)
// 两者共用同一个 DataSource 和事务管理器

@SpringBootApplication
@MapperScan("com.example.project.mapper")  // MyBatis Mapper 扫描
public class Application { }

// ========== 使用示例 ==========
@Service
public class OrderService {
    
    // JPA — 事务管理, 简单操作
    @Autowired
    private OrderRepository orderRepository;
    
    // MyBatis — 复杂查询
    @Autowired
    private OrderReportMapper orderReportMapper;
    
    @Transactional
    public Order createOrder(OrderRequest request) {
        // JPA 处理复杂的实体关系
        Order order = new Order();
        order.setUser(userRepository.getReferenceById(request.getUserId()));
        // ... 业务逻辑
        return orderRepository.save(order);
    }
    
    // MyBatis 处理复杂报表
    public List<OrderReportVO> getMonthlyReport(Integer year, Integer month) {
        return orderReportMapper.generateMonthlyReport(year, month);
    }
}
```

### 14.2 事务一致性

```java
// ========== 混合使用的事务保证 ==========
// JPA 和 MyBatis 共用同一个 PlatformTransactionManager
// @Transactional 同时对两者生效

@Service
public class MixedService {
    
    @Autowired
    private UserRepository userRepository;  // JPA
    
    @Autowired
    private UserMapper userMapper;          // MyBatis
    
    @Transactional  // 同一个事务
    public void updateUser(Long id, String name) {
        // JPA 操作
        User user = userRepository.findById(id).orElseThrow();
        user.setUserName(name);
        userRepository.save(user);
        
        // MyBatis 操作 — 同一个事务!
        userMapper.insertLog(id, "Updated name to: " + name);
        
        // 如果 MyBatis 操作失败, JPA 操作也会回滚
        // 反之亦然
    }
}

// ========== 注意: 一级缓存不一致 ==========
// JPA 有 PersistenceContext (一级缓存)
// MyBatis 有 SqlSession 一级缓存
// 两者缓存不互通, 在同一个事务中混合使用时要注意

@Transactional
public void mixedOperation() {
    // JPA 查询
    User user = userRepository.findById(1L).orElseThrow();
    user.setUserName("NewName");
    userRepository.save(user);
    // JPA 的 flush 还未发生
    
    // MyBatis 查询 — 可能读到旧数据!
    User user2 = userMapper.findById(1L);
    // 因为 JPA 还没 flush, 数据库还是旧值
    
    // 解决: 显式 flush JPA
    userRepository.flush();
    // 或使用: userRepository.saveAndFlush(user);
}
```

---

## 15. 面试题精选

### 15.1 MyBatis 面试题

**Q1: #{} 和 ${} 的区别？**

A: #{} 是 PreparedStatement 参数占位符，自动加引号，防 SQL 注入。${} 是字符串替换，不防注入，只能用于表名/列名等动态场景。

**Q2: MyBatis 的一级缓存和二级缓存？**

A: 一级缓存是 SqlSession 级别，默认开启，无法关闭。二级缓存是 Mapper 级别，需手动开启，跨 SqlSession 共享，分布式环境需谨慎使用。

**Q3: MyBatis 中的 resultType 和 resultMap 的区别？**

A: resultType 用于简单映射（列名同属性名），resultMap 用于复杂映射（列名不同、关联查询、嵌套对象）。

**Q4: MyBatis-Plus 相比 MyBatis 的增强点？**

A: 自动 CRUD (BaseMapper)、条件构造器 (Wrapper)、分页插件、逻辑删除、乐观锁、自动填充、代码生成器。

### 15.2 JPA 面试题

**Q5: JPA 中 CascadeType 的区别？**

A: ALL (全部), PERSIST (保存级联), MERGE (更新级联), REMOVE (删除级联), REFRESH (刷新级联), DETACH (分离级联)。

**Q6: 什么是 N+1 查询？如何解决？**

A: 查询主实体后，访问每个实体的懒加载关联时触发额外查询。解决方案: JOIN FETCH、@EntityGraph、@BatchSize、DTO 投影。

**Q7: JPA 实体状态有哪些？**

A: transient (新建, 无 ID), managed (托管, 有 ID, 被 EntityManager 管理), detached (游离, 有 ID 但不受管理), removed (删除)。

**Q8: @Version 乐观锁的原理？**

A: 在实体上加 @Version 字段，更新时检查版本号：UPDATE ... SET version=version+1 WHERE id=? AND version=oldVersion。如果不匹配表示数据已被修改，抛出 OptimisticLockException。

### 15.3 综合面试题

**Q9: MyBatis 和 JPA 如何选择？**

A: 看场景: 复杂查询/报表/SQL 精细优化 → MyBatis; CRUD 密集/实体关系复杂/快速开发 → JPA。大型项目通常两者结合使用。

**Q10: 如何在同一个 Spring Boot 项目中同时使用 JPA 和 MyBatis？**

A: 配置两个数据源或共用同一数据源。JPA 负责标准 CRUD（JpaRepository），MyBatis 负责复杂查询（@Mapper）。共用同一 @Transactional 管理器保证事务一致性。

---

> **数据访问层是应用的基石。MyBatis 给你自由, JPA 给你效率。理解两者的设计哲学和适用场景, 才能在企业项目中做出正确的技术选择。记住: 没有银弹, 只有适合场景的工具。**
