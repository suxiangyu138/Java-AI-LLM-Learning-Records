# MyBatis 入门到精通
> MyBatis 是一款优秀的 Java 持久层框架，通过 XML 或注解将 SQL 与 Java 代码解耦。本文从零开始，覆盖 MyBatis 核心概念、XML/注解用法、动态 SQL、关联映射、缓存机制、插件开发及代码生成器，循序渐进掌握 MyBatis。

## 目录
1. [MyBatis 核心概念](#1-mybatis-核心概念)
2. [环境搭建与快速入门](#2-环境搭建与快速入门)
3. [XML 方式基本用法](#3-xml-方式基本用法)
4. [注解方式基本用法](#4-注解方式基本用法)
5. [动态 SQL](#5-动态-sql)
6. [关联映射](#6-关联映射)
7. [高级查询](#7-高级查询)
8. [缓存机制](#8-缓存机制)
9. [插件开发](#9-插件开发)
10. [代码生成器](#10-代码生成器)
11. [核心面试点](#11-核心面试点)

---

## 1. MyBatis 核心概念

### 1.1 什么是 MyBatis

MyBatis 是一款**优秀的持久层框架**，它支持**自定义 SQL、存储过程以及高级映射**。

- 用来**操作数据库**（增删改查）
- 代替原生 JDBC，**简化代码**
- 把 SQL 和 Java 代码分离，方便维护
- **半自动 ORM**——灵活、可控（不像 JPA/Hibernate 那样全自动）

### 1.2 核心组件

| 组件 | 职责 | 生命周期 |
|------|------|----------|
| `SqlSessionFactoryBuilder` | 解析配置文件，构建工厂 | 方法局部，用完即弃 |
| `SqlSessionFactory` | 创建 SqlSession 的工厂 | 应用级别单例 |
| `SqlSession` | 数据库会话门面，提供 CRUD API | 请求/方法级别 |
| `Executor` | SQL 执行器，负责缓存、事务、语句执行 | SqlSession 级别 |
| `StatementHandler` | 封装 JDBC Statement，参数预编译 | 语句级别 |
| `ParameterHandler` | 将 Java 参数转为 JDBC 类型 | 语句级别 |
| `ResultSetHandler` | 将 JDBC ResultSet 映射为 Java 对象 | 语句级别 |

### 1.3 核心架构链路

```
SqlSessionFactoryBuilder → SqlSessionFactory → SqlSession
                                                    ↓
                                               Executor
                                                    ↓
                                          StatementHandler
                                                    ↓
                                          ParameterHandler → ResultSetHandler
```

### 1.4 与 JDBC 对比

| 维度 | JDBC | MyBatis |
|------|------|---------|
| 代码量 | 大量样板代码 | 简洁，SQL 与代码分离 |
| 参数设置 | 手动 `setString`、`setInt` | 自动映射 |
| 结果映射 | 手动 `getString`、`getInt` | 自动映射为 POJO |
| SQL 与代码 | 混在一起 | XML/注解分离 |
| 连接管理 | 手动获取/关闭 | 由框架管理 |

---

## 2. 环境搭建与快速入门

### 2.1 Maven 依赖

```xml
<dependencies>
    <!-- MyBatis -->
    <dependency>
        <groupId>org.mybatis</groupId>
        <artifactId>mybatis</artifactId>
        <version>3.5.16</version>
    </dependency>
    <!-- MySQL 驱动 -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <version>8.0.33</version>
    </dependency>
    <!-- 日志 -->
    <dependency>
        <groupId>ch.qos.logback</groupId>
        <artifactId>logback-classic</artifactId>
        <version>1.4.14</version>
    </dependency>
</dependencies>
```

### 2.2 核心配置文件（mybatis-config.xml）

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE configuration PUBLIC "-//mybatis.org//DTD Config 3.0//EN"
        "https://mybatis.org/dtd/mybatis-3-config.dtd">
<configuration>
    <settings>
        <!-- 下划线转驼峰：user_name → userName -->
        <setting name="mapUnderscoreToCamelCase" value="true"/>
        <!-- 日志实现 -->
        <setting name="logImpl" value="STDOUT_LOGGING"/>
    </settings>

    <environments default="development">
        <environment id="development">
            <transactionManager type="JDBC"/>
            <dataSource type="POOLED">
                <property name="driver" value="com.mysql.cj.jdbc.Driver"/>
                <property name="url" value="jdbc:mysql://localhost:3306/mydb"/>
                <property name="username" value="root"/>
                <property name="password" value="123456"/>
            </dataSource>
        </environment>
    </environments>

    <!-- 注册 Mapper XML -->
    <mappers>
        <mapper resource="mapper/UserMapper.xml"/>
    </mappers>
</configuration>
```

### 2.3 实体类

```java
public class User {
    private Long id;
    private String username;
    private String password;
    private String phone;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // getters / setters / toString
}
```

### 2.4 Mapper 接口

```java
public interface UserMapper {
    User selectById(Long id);
    List<User> selectAll();
    int insert(User user);
    int update(User user);
    int deleteById(Long id);
}
```

### 2.5 Mapper XML 映射文件

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "https://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="com.example.mapper.UserMapper">
    <select id="selectById" resultType="com.example.entity.User">
        SELECT * FROM t_user WHERE id = #{id}
    </select>

    <select id="selectAll" resultType="com.example.entity.User">
        SELECT * FROM t_user
    </select>

    <insert id="insert" useGeneratedKeys="true" keyProperty="id">
        INSERT INTO t_user(username, password, phone)
        VALUES (#{username}, #{password}, #{phone})
    </insert>

    <update id="update">
        UPDATE t_user SET username = #{username}, phone = #{phone}
        WHERE id = #{id}
    </update>

    <delete id="deleteById">
        DELETE FROM t_user WHERE id = #{id}
    </delete>
</mapper>
```

### 2.6 测试代码

```java
public class MyBatisDemo {
    public static void main(String[] args) throws IOException {
        // 1. 加载配置文件
        InputStream inputStream = Resources.getResourceAsStream("mybatis-config.xml");
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);

        // 2. 获取 SqlSession
        try (SqlSession session = sqlSessionFactory.openSession()) {
            // 3. 获取 Mapper
            UserMapper mapper = session.getMapper(UserMapper.class);

            // 4. 执行 CRUD
            User user = mapper.selectById(1L);
            System.out.println(user);

            // 5. 提交事务（DML 操作必须）
            session.commit();
        }
    }
}
```

---

## 3. XML 方式基本用法

### 3.1 参数传递

```xml
<!-- 单个参数：直接用 #{任意名} -->
<select id="selectById" resultType="User">
    SELECT * FROM t_user WHERE id = #{id}
</select>

<!-- 多个参数：用 @Param 指定名称 -->
<!-- Java: User selectByNameAndAge(@Param("name") String name, @Param("age") Integer age) -->
<select id="selectByNameAndAge" resultType="User">
    SELECT * FROM t_user WHERE name = #{name} AND age = #{age}
</select>

<!-- POJO 参数：直接用属性名 -->
<!-- Java: User selectByUser(User user) -->
<select id="selectByUser" resultType="User">
    SELECT * FROM t_user WHERE username = #{username} AND age = #{age}
</select>

<!-- Map 参数：用 key 名 -->
<!-- Java: User selectByMap(Map<String, Object> map) -->
<select id="selectByMap" resultType="User">
    SELECT * FROM t_user WHERE username = #{username} AND age = #{age}
</select>
```

### 3.2 结果映射（resultMap）

```xml
<!-- 当字段名和属性名不一致或需要复杂映射时使用 -->
<resultMap id="userResultMap" type="User">
    <id property="id" column="id"/>
    <result property="userName" column="user_name"/>
    <result property="createTime" column="created_at"/>
</resultMap>

<select id="selectById" resultMap="userResultMap">
    SELECT * FROM t_user WHERE id = #{id}
</select>
```

### 3.3 主键回填

```xml
<!-- useGeneratedKeys：获取自增主键值 -->
<insert id="insert" useGeneratedKeys="true" keyProperty="id">
    INSERT INTO t_user(username, password) VALUES (#{username}, #{password})
</insert>

<!-- 或使用 selectKey（非自增场景） -->
<insert id="insert">
    <selectKey keyProperty="id" resultType="long" order="BEFORE">
        SELECT NEXT_ID from id_generator
    </selectKey>
    INSERT INTO t_user(id, username, password) VALUES (#{id}, #{username}, #{password})
</insert>
```

---

## 4. 注解方式基本用法

### 4.1 基本 CRUD 注解

```java
public interface UserMapper {
    @Select("SELECT * FROM t_user WHERE id = #{id}")
    User selectById(Long id);

    @Select("SELECT * FROM t_user")
    List<User> selectAll();

    @Insert("INSERT INTO t_user(username, password, phone) VALUES (#{username}, #{password}, #{phone})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    @Update("UPDATE t_user SET username = #{username} WHERE id = #{id}")
    int update(User user);

    @Delete("DELETE FROM t_user WHERE id = #{id}")
    int deleteById(Long id);
}
```

### 4.2 注解方式配置

```xml
<!-- mybatis-config.xml 中注册 -->
<mappers>
    <mapper class="com.example.mapper.UserMapper"/>
</mappers>

<!-- 或 Spring Boot 中 @MapperScan -->
@SpringBootApplication
@MapperScan("com.example.mapper")
public class Application { }
```

### 4.3 注解 vs XML 选择

| 场景 | 推荐方式 | 原因 |
|------|----------|------|
| 简单 CRUD | 注解 | 代码少，直观 |
| 复杂查询 / 动态 SQL | XML | 可读性强，易于维护 |
| 多行 SQL / 存储过程 | XML | 注解中拼接长 SQL 丑陋 |
| 团队统一规范 | XML | 集中管理 SQL |

---

## 5. 动态 SQL

### 5.1 if

```xml
<select id="findByCondition" resultType="User">
    SELECT * FROM t_user WHERE 1=1
    <if test="username != null and username != ''">
        AND username LIKE CONCAT('%', #{username}, '%')
    </if>
    <if test="status != null">
        AND status = #{status}
    </if>
</select>
```

### 5.2 where

自动处理 `WHERE` 关键字和多余的 `AND`/`OR`：

```xml
<select id="findByCondition" resultType="User">
    SELECT * FROM t_user
    <where>
        <if test="username != null">
            AND username LIKE CONCAT('%', #{username}, '%')
        </if>
        <if test="status != null">
            AND status = #{status}
        </if>
    </where>
</select>
```

### 5.3 set

自动处理 `SET` 关键字和末尾逗号：

```xml
<update id="updateSelective">
    UPDATE t_user
    <set>
        <if test="username != null">username = #{username},</if>
        <if test="phone != null">phone = #{phone},</if>
        <if test="status != null">status = #{status},</if>
    </set>
    WHERE id = #{id}
</update>
```

### 5.4 foreach

```xml
<!-- 批量查询 -->
<select id="selectByIds" resultType="User">
    SELECT * FROM t_user WHERE id IN
    <foreach collection="ids" item="id" open="(" separator="," close=")">
        #{id}
    </foreach>
</select>

<!-- 批量插入 -->
<insert id="batchInsert">
    INSERT INTO t_user(username, password) VALUES
    <foreach collection="list" item="user" separator=",">
        (#{user.username}, #{user.password})
    </foreach>
</insert>

<!-- 批量更新（MySQL 语法） -->
<update id="batchUpdate">
    <foreach collection="list" item="user" separator=";">
        UPDATE t_user SET username = #{user.username} WHERE id = #{user.id}
    </foreach>
</update>
```

### 5.5 choose / when / otherwise（类似 switch）

```xml
<select id="findByCondition" resultType="User">
    SELECT * FROM t_user
    <where>
        <choose>
            <when test="username != null">
                AND username = #{username}
            </when>
            <when test="phone != null">
                AND phone = #{phone}
            </when>
            <otherwise>
                AND status = 1
            </otherwise>
        </choose>
    </where>
</select>
```

### 5.6 #{} vs ${}

| 对比维度 | `#{}` | `${}` |
|----------|-------|-------|
| 底层实现 | PreparedStatement（预编译，`?` 占位） | Statement（字符串拼接） |
| 参数处理 | 自动识别类型、加引号、转义 | 原样拼接，不加引号不转义 |
| 核心用途 | 传递字段值（WHERE/INSERT/UPDATE） | 拼接 SQL 结构（表名/列名/ORDER BY） |
| SQL 注入 | 无风险，自带防护 | 有风险，需手动防护 |
| 使用优先级 | 优先使用 | 仅必要时使用 |

```xml
<!-- ${} 必用场景：动态表名、列名、排序 -->
<select id="selectByTable" resultType="User">
    SELECT * FROM ${tableName} WHERE status = 1
</select>

<select id="selectOrderBy" resultType="User">
    SELECT * FROM t_user ORDER BY ${sortCol} ${dir}
</select>
```

> ⚠️ **核心原则**：优先 `#{}`，仅必要时用 `${}` 并做好安全防护（白名单过滤、参数可控）。

---

## 6. 关联映射

### 6.1 一对一 association

```java
public class User {
    private Long id;
    private String username;
    private UserProfile profile;  // 一对一关联
}
```

```xml
<resultMap id="userWithProfileMap" type="User">
    <id property="id" column="id"/>
    <result property="username" column="username"/>
    <!-- 嵌套查询（N+1 问题，慎用） -->
    <association property="profile" column="id"
                 select="com.example.mapper.UserProfileMapper.selectByUserId"/>
</resultMap>

<!-- 推荐：联合查询 -->
<resultMap id="userWithProfileMap" type="User">
    <id property="id" column="id"/>
    <result property="username" column="username"/>
    <association property="profile" javaType="UserProfile">
        <id property="id" column="profile_id"/>
        <result property="avatar" column="avatar"/>
        <result property="bio" column="bio"/>
    </association>
</resultMap>

<select id="selectUserWithProfile" resultMap="userWithProfileMap">
    SELECT u.*, up.id AS profile_id, up.avatar, up.bio
    FROM t_user u
    LEFT JOIN t_user_profile up ON u.id = up.user_id
    WHERE u.id = #{id}
</select>
```

### 6.2 一对多 collection

```java
public class User {
    private Long id;
    private String username;
    private List<Order> orders;  // 一对多关联
}
```

```xml
<resultMap id="userWithOrdersMap" type="User">
    <id property="id" column="id"/>
    <result property="username" column="username"/>
    <collection property="orders" ofType="Order">
        <id property="id" column="order_id"/>
        <result property="orderNo" column="order_no"/>
        <result property="amount" column="amount"/>
    </collection>
</resultMap>

<select id="selectUserWithOrders" resultMap="userWithOrdersMap">
    SELECT u.*, o.id AS order_id, o.order_no, o.amount
    FROM t_user u
    LEFT JOIN t_order o ON u.id = o.user_id
    WHERE u.id = #{id}
</select>
```

---

## 7. 高级查询

### 7.1 分页查询

```xml
<!-- 手动分页 -->
<select id="selectPage" resultType="User">
    SELECT * FROM t_user ORDER BY id LIMIT #{offset}, #{limit}
</select>
```

```java
// 配合 PageHelper（推荐）
<dependency>
    <groupId>com.github.pagehelper</groupId>
    <artifactId>pagehelper-spring-boot-starter</artifactId>
    <version>1.4.6</version>
</dependency>

PageHelper.startPage(pageNum, pageSize);
List<User> list = userMapper.selectAll();
PageInfo<User> pageInfo = new PageInfo<>(list);
// pageInfo.getTotal(), getPages(), getList()...
```

### 7.2 多条件组合查询

```xml
<select id="searchUsers" resultType="User">
    SELECT * FROM t_user
    <where>
        <if test="keyword != null">
            AND (username LIKE CONCAT('%', #{keyword}, '%')
                 OR phone LIKE CONCAT('%', #{keyword}, '%'))
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
    </where>
    ORDER BY created_at DESC
</select>
```

---

## 8. 缓存机制

### 8.1 一级缓存（SqlSession 级别）

- 默认开启，无法关闭
- 同一个 SqlSession 中，相同的查询只执行一次 SQL
- 范围：同一个 SqlSession 内部
- 失效条件：执行 DML（增删改）后清空缓存

```
session1: select * from t_user where id=1  → 查数据库
session1: select * from t_user where id=1  → 走缓存
session1: update t_user ... where id=1     → 清空缓存
session1: select * from t_user where id=1  → 重新查数据库
```

### 8.2 二级缓存（Mapper 级别）

- 默认关闭，需手动开启
- 范围：同一个 Mapper 命名空间（跨 SqlSession 共享）
- 开启方式：

```xml
<!-- mybatis-config.xml -->
<settings>
    <setting name="cacheEnabled" value="true"/>
</settings>

<!-- Mapper XML 中加 <cache/> -->
<mapper namespace="com.example.mapper.UserMapper">
    <cache/>
    ...
</mapper>
```

| 对比 | 一级缓存 | 二级缓存 |
|------|---------|---------|
| 默认状态 | 开启 | 关闭 |
| 作用范围 | SqlSession | Mapper Namespace |
| 跨会话 | 不共享 | 共享 |
| 序列化要求 | 无 | 实体需实现 Serializable |
| 生产使用 | 默认 | 谨慎使用 |

> ⚠️ **二级缓存注意**：多表查询时可能产生脏数据（关联表的更新不会清空本 Mapper 的缓存），生产环境**建议使用 Redis 等外部缓存**代替 MyBatis 二级缓存。

---

## 9. 插件开发

### 9.1 插件原理

MyBatis 允许拦截以下四大核心对象的方法：

| 拦截对象 | 可拦截方法 | 典型用途 |
|----------|-----------|----------|
| Executor | update, query, commit, rollback | SQL 监控、分页 |
| StatementHandler | prepare, parameterize, batch | 改写 SQL |
| ParameterHandler | setParameters | 参数处理 |
| ResultSetHandler | handleResultSets, handleCursorResultSets | 结果加密/脱敏 |

### 9.2 实现分页插件示例

```java
@Intercepts({
    @Signature(type = Executor.class, method = "query",
              args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class})
})
public class PageInterceptor implements Interceptor {
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        // 1. 获取参数
        Object[] args = invocation.getArgs();
        MappedStatement ms = (MappedStatement) args[0];
        Object parameter = args[1];

        // 2. 判断是否分页查询
        if (parameter instanceof Page) {
            Page page = (Page) parameter;
            // 3. 拼接 count 查询
            // 4. 拼接 limit 语句
            // 5. 设置总数
        }
        return invocation.proceed();
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }
}
```

### 9.3 注册插件

```xml
<plugins>
    <plugin interceptor="com.example.plugin.PageInterceptor"/>
</plugins>
```

### 9.4 常用插件

| 插件 | 用途 |
|------|------|
| PageHelper | 物理分页（最常用） |
| MyBatis Generator | 代码生成器 |
| MyBatis Plus | 增强工具（内置分页、乐观锁等） |

---

## 10. 代码生成器

### 10.1 MyBatis Generator（MBG）

```xml
<plugin>
    <groupId>org.mybatis.generator</groupId>
    <artifactId>mybatis-generator-maven-plugin</artifactId>
    <version>1.4.2</version>
    <configuration>
        <configurationFile>src/main/resources/generator-config.xml</configurationFile>
        <overwrite>true</overwrite>
    </configuration>
</plugin>
```

配置文件 `generator-config.xml`：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE generatorConfiguration PUBLIC
 "-//mybatis.org//DTD MyBatis Generator Configuration 1.0//EN"
 "http://mybatis.org/dtd/mybatis-generator-config_1_0.dtd">

<generatorConfiguration>
    <!-- 数据库连接 -->
    <jdbcConnection driverClass="com.mysql.cj.jdbc.Driver"
                    connectionURL="jdbc:mysql://localhost:3306/mydb"
                    userId="root" password="123456"/>

    <!-- 实体类生成位置 -->
    <javaModelGenerator targetPackage="com.example.entity"
                        targetProject="src/main/java"/>

    <!-- Mapper XML 生成位置 -->
    <sqlMapGenerator targetPackage="mapper"
                     targetProject="src/main/resources"/>

    <!-- Mapper 接口生成位置 -->
    <javaClientGenerator type="XMLMAPPER"
                         targetPackage="com.example.mapper"
                         targetProject="src/main/java"/>

    <!-- 要生成代码的表 -->
    <table tableName="t_user" domainObjectName="User"/>
    <table tableName="t_order" domainObjectName="Order"/>
</generatorConfiguration>
```

运行命令：

```bash
mvn mybatis-generator:generate
```

---

## 11. 核心面试点

| 问题 | 回答要点 |
|------|----------|
| MyBatis 和 JDBC 区别 | 代码量、参数映射、结果映射、SQL 分离、连接管理 |
| #{} 和 ${} 区别 | 预编译 vs 拼接；防注入 vs 有风险；字段值 vs SQL 结构 |
| 一级缓存和二级缓存 | 作用范围、开启方式、失效条件、生产建议 |
| MyBatis 插件原理 | 四大拦截对象（Executor/StatementHandler/ParameterHandler/ResultSetHandler） |
| 分页实现 | PageHelper 原理：拦截 SQL → COUNT + LIMIT |
| 关联映射 | association（一对一）、collection（一对多） |
| MyBatis 与 MyBatis-Plus 区别 | MP 是增强工具，BaseMapper 自带 CRUD，LambdaQueryWrapper 类型安全 |

> 🎯 **MyBatis 的核心优势在于灵活控制 SQL**——不限制你写什么 SQL，不像 JPA 那样有黑盒风险。这也是它在国内企业级开发中占据主流地位的原因。
