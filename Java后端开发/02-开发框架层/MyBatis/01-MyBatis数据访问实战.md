# MyBatis 数据访问实战

## 目录

1. [MyBatis 核心架构](#1-mybatis-核心架构)
2. [SQL 映射详解](#2-sql-映射详解)
3. [动态 SQL 编程](#3-动态-sql-编程)
4. [MyBatis-Plus 增强](#4-mybatis-plus-增强)
5. [最佳实践与性能优化](#5-最佳实践与性能优化)
6. [与 Spring Boot 整合](#6-与-spring-boot-整合)
7. [总结清单](#7-总结清单)

---

## 1. MyBatis 核心架构

### 1.1 整体架构与核心组件

MyBatis 是一款优秀的持久层框架，它通过 XML 或注解的方式将 Java 对象与 SQL 语句进行映射，屏蔽了 JDBC 底层繁琐的 API 调用。要真正掌握 MyBatis，首先需要理解它的核心架构链路：

```
SqlSessionFactoryBuilder → SqlSessionFactory → SqlSession
                                                    ↓
                                               Executor
                                                    ↓
                                          StatementHandler
                                                    ↓
                                          ParameterHandler
                                                    ↓
                                          ResultSetHandler
```

下面逐一分析每个组件的职责：

| 组件 | 职责 | 生命周�� |
|------|------|----------|
| `SqlSessionFactoryBuilder` | 解析 mybatis-config.xml 配置文件，构建工厂对象 | 方法局部，用完即弃 |
| `SqlSessionFactory` | 创建 SqlSession 的工厂，持有全局配置信息 | 应用级别单例 |
| `SqlSession` | 数据库会话门面，提供增删改查 API | 请求/方法级别 |
| `Executor` | SQL 执行器，负责缓存维护、事务管理、语句执行 | SqlSession 级别 |
| `StatementHandler` | 封装 JDBC Statement 操作，处理参数预编译 | 语句级别 |
| `ParameterHandler` | 将 Java 参数转换为 JDBC 类型并设置到 PreparedStatement | 语句级别 |
| `ResultSetHandler` | 将 JDBC ResultSet 映射为 Java 对象（或集合） | 语句级别 |

**核心链路解读：**

```java
// 第1步：通过构建器解析配置文件，构建工厂
String resource = "mybatis-config.xml";
InputStream inputStream = Resources.getResourceAsStream(resource);
SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);

// 第2步：打开会话（内部创建 Executor）
try (SqlSession session = sqlSessionFactory.openSession()) {
    // 第3步：通过动态代理调用 Mapper 接口
    UserMapper mapper = session.getMapper(UserMapper.class);
    User user = mapper.selectById(1L);
}
```

在 `openSession()` 时，SqlSessionFactory 会根据配置创建 `Executor` 实例（默认 `SimpleExecutor`，可配置为 `ReuseExecutor` 或 `BatchExecutor`）。`Executor` 在执行时又会创建 `StatementHandler`，后者通过 `ParameterHandler` 设置参数，最终由 `ResultSetHandler` 处理返回结果。

### 1.2 配置文件 mybatis-config.xml

MyBatis 的全局配置文件是整个框架的"总开关"，下面是一个典型配置：

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE configuration
        PUBLIC "-//mybatis.org//DTD Config 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-config.dtd">
<configuration>
    <!-- 属性文件引入 -->
    <properties resource="db.properties"/>

    <!-- 全局设置 -->
    <settings>
        <!-- 开启驼峰命名自动映射：数据库列 user_name -> Java 属性 userName -->
        <setting name="mapUnderscoreToCamelCase" value="true"/>
        <!-- 开启延迟加载 -->
        <setting name="lazyLoadingEnabled" value="true"/>
        <!-- 日志实现 -->
        <setting name="logImpl" value="STDOUT_LOGGING"/>
        <!-- 二级缓存全局开关 -->
        <setting name="cacheEnabled" value="true"/>
    </settings>

    <!-- 类型别名 -->
    <typeAliases>
        <package name="com.example.entity"/>
        <!-- 等价于为包下每个实体注册简短别名：User、Order... -->
    </typeAliases>

    <!-- 环境配置（可配置多环境：dev/test/prod） -->
    <environments default="dev">
        <environment id="dev">
            <transactionManager type="JDBC"/>
            <dataSource type="POOLED">
                <property name="driver" value="${jdbc.driver}"/>
                <property name="url" value="${jdbc.url}"/>
                <property name="username" value="${jdbc.username}"/>
                <property name="password" value="${jdbc.password}"/>
            </dataSource>
        </environment>
    </environments>

    <!-- 注册映射文件 -->
    <mappers>
        <mapper resource="mapper/UserMapper.xml"/>
        <package name="com.example.mapper"/>
    </mappers>
</configuration>
```

**关键 setting 说明：**

- `mapUnderscoreToCamelCase`：开启后 `user_name` 自动映射到 `userName`，避免手写繁琐的 resultMap。
- `lazyLoadingEnabled`：延迟加载的全局开关，配合 `aggressiveLazyLoading=false` 实现按需加载。
- `cacheEnabled`：二级缓存的全局开关，默认为 true。
- `logImpl`：指定日志实现，可选 SLF4J、LOG4J、STDOUT_LOGGING 等。

### 1.3 映射文件 mapper.xml 结构

映射文件是 MyBatis 的灵魂所在，它将 Java 方法与 SQL 语句关联起来：

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">

<!-- namespace 必须绑定 Mapper 接口的全限定名 -->
<mapper namespace="com.example.mapper.UserMapper">

    <!-- 结果映射：将数据库列映射到 Java 属性 -->
    <resultMap id="userMap" type="User">
        <id property="id" column="id"/>
        <result property="userName" column="user_name"/>
        <result property="email" column="email"/>
        <result property="createTime" column="create_time"/>
    </resultMap>

    <!-- 参数映射（3.x 后已弱化，推荐使用注解或自动推断） -->
    <parameterMap id="userParam" type="User">
        <parameter property="userName"/>
    </parameterMap>

    <!-- SQL 片段复用 -->
    <sql id="baseColumns">id, user_name, email, create_time</sql>

    <select id="selectById" resultMap="userMap">
        SELECT <include refid="baseColumns"/>
        FROM user WHERE id = #{id}
    </select>

</mapper>
```

**namespace 的重要性：** MyBatis 通过 namespace + 方法名定位 SQL 语句，如果 namespace 不与 Mapper 接口全限定名一致，动态代理将无法绑定。

### 1.4 Mapper 接口与动态代理原理

MyBatis 允许我们只定义接口而不写实现类，这是通过 JDK 动态代理实现的。

**MapperProxy 源码简化分析：**

```java
// MapperProxy 实现了 InvocationHandler
public class MapperProxy<T> implements InvocationHandler, Serializable {
    private final SqlSession sqlSession;
    private final Class<T> mapperInterface;

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 过滤 Object 原生方法（toString、hashCode 等）
        if (Object.class.equals(method.getDeclaringClass())) {
            return method.invoke(this, args);
        }
        // 核心：创建 MapperMethodInvoker 并执行
        return cachedInvoker(method).invoke(proxy, method, args, sqlSession);
    }
}
```

**绑定流程：**

```
1. SqlSession.getMapper(UserMapper.class)
2. → Configuration.getMapper(Class, SqlSession)
3. → MapperRegistry.getMapper(Class, SqlSession)
4. → MapperProxyFactory.newInstance(SqlSession)
5. → Proxy.newProxyInstance(mapperInterface, MapperProxy)
```

`MapperProxyFactory` 负责创建代理实例：

```java
public class MapperProxyFactory<T> {
    private final Class<T> mapperInterface;

    @SuppressWarnings("unchecked")
    protected T newInstance(MapperProxy<T> mapperProxy) {
        return (T) Proxy.newProxyInstance(
                mapperInterface.getClassLoader(),
                new Class[]{mapperInterface},
                mapperProxy);
    }
}
```

**与 JPA 的对比：** JPA 也使用动态代理，但 JPA 的代理底层通过 `EntityManager` 管理持久化上下文和脏检查，而 MyBatis 的代理仅仅是"方法到 SQL 的桥接"，更加轻量透明。

---

## 2. SQL 映射详解

### 2.1 CRUD 标签

MyBatis 提供了四个核心标签对应数据库的基本操作：

#### Select

```xml
<select id="selectById" parameterType="long" resultType="User">
    SELECT * FROM user WHERE id = #{id}
</select>

<select id="findByCondition" parameterType="map" resultType="User">
    SELECT * FROM user WHERE user_name = #{name} AND status = #{status}
</select>
```

#### Insert 与自增主键

```xml
<!-- useGeneratedKeys：使用 JDBC 的 getGeneratedKeys 获取自增主键 -->
<!-- keyProperty：将生成的主键值赋给 Java 对象的哪个属性 -->
<insert id="insert" useGeneratedKeys="true" keyProperty="id">
    INSERT INTO user(user_name, email, status)
    VALUES(#{userName}, #{email}, #{status})
</insert>
```

调用后，传入的 User 对象的 id 属性会被自动填充：

```java
User user = new User("张三", "zhangsan@example.com", 1);
userMapper.insert(user);
System.out.println(user.getId()); // 自动填充了数据库生成的自增ID
```

**Oracle 序列方式（不支持自增时）：**

```xml
<insert id="insertOracle">
    <selectKey keyProperty="id" order="BEFORE" resultType="long">
        SELECT SEQ_USER.NEXTVAL FROM DUAL
    </selectKey>
    INSERT INTO user(id, user_name) VALUES(#{id}, #{userName})
</insert>
```

#### Update

```xml
<update id="updateById">
    UPDATE user
    SET user_name = #{userName}, email = #{email}
    WHERE id = #{id}
</update>
```

#### Delete

```xml
<delete id="deleteById">
    DELETE FROM user WHERE id = #{id}
</delete>
```

### 2.2 参数传递机制

#### `#{}` 与 `${}` 的区别

这是 MyBatis 面试中最核心的问题之一：

| 特性 | `#{}` | `${}` |
|------|-------|-------|
| 预编译 | 是，使用 `PreparedStatement` 占位符 `?` | 否，直接字符串替换 |
| SQL 注入 | 安全，参数值不会被解析为 SQL | 不安全，可能导致注入攻击 |
| 适用场景 | 大多数值传递（字段值、条件值） | 表名、列名、ORDER BY 等动态 SQL 片段 |
| 性能 | 高，可复用预编译语句 | 低，每次生成新语句 |

**正确使用 `${}` 的场景：**

```java
// 场景1：动态表名（分表）
@Select("SELECT * FROM ${tableName} WHERE id = #{id}")
User findByTable(@Param("tableName") String tableName, @Param("id") Long id);

// 场景2：动态排序
@Select("SELECT * FROM user ORDER BY ${orderColumn} ${orderDir}")
List<User> findAllOrdered(@Param("orderColumn") String column,
                          @Param("orderDir") String direction);
// !!! 注意：此处需要对传入值做白名单校验，防止 SQL 注入

// 场景3：动态列名
@Select("SELECT ${column} FROM user WHERE id = #{id}")
Object getColumnValue(@Param("column") String column, @Param("id") Long id);
```

**安全原则：** 用户可控的输入永远用 `#{}`；`${}` 仅用于开发者可控的元数据（表名、列名），且必须做白名单校验。

#### 多参数传递方式

MyBatis 支持四种参数传递方式：

```java
// 方式1：使用 @Param 注解（推荐，最清晰）
User findByNameAndStatus(@Param("name") String name,
                         @Param("status") Integer status);
// XML：#{name} #{status}

// 方式2：使用 Map（灵活性高，但类型不安全）
User findByMap(Map<String, Object> params);
// XML：#{name} #{status}

// 方式3：使用 Java Bean（参数较多时推荐）
User findByCondition(UserQuery query);
// XML：#{userName} #{status}

// 方式4：单参数（基本类型/String 直接使用）
User findById(Long id);
// XML：#{id} 或任意名称如 #{value}
```

**MyBatis 3.x 后的参数名解析：** 从 3.4.2 开始，如果编译时启用了 `-parameters` 选项（Spring Boot 默认开启），可以不使用 `@Param`，直接使用方法参数名：

```xml
<select id="findByNameAndStatus" resultType="User">
    SELECT * FROM user WHERE user_name = #{name} AND status = #{status}
</select>
```

### 2.3 结果映射：resultType vs resultMap

#### resultType（自动映射）

当数据库列名与 Java 属性名一致时，使用 `resultType` 最为简便：

```xml
<select id="findAll" resultType="com.example.entity.User">
    SELECT id, user_name, email FROM user
</select>
```

配合 `mapUnderscoreToCamelCase=true`，`user_name` 会自动映射到 `userName`。

```xml
<settings>
    <setting name="mapUnderscoreToCamelCase" value="true"/>
</settings>
```

**自动映射级别：**

- `NONE`：禁用自动映射
- `PARTIAL`（默认）：自动映射除内嵌结果外的所有属性
- `FULL`：自动映射所有属性（包括内嵌结果，可能产生性能问题）

#### resultMap（复杂映射）

当列名与属性名不一致、存在关联对象或一对多集合时，需要使用 `resultMap`：

```xml
<resultMap id="userMap" type="User">
    <!-- id 标签标记主键，优化性能 -->
    <id property="id" column="id"/>
    <result property="userName" column="user_name"/>
    <result property="email" column="email"/>
    <result property="status" column="status"/>
    <result property="createTime" column="create_time"/>
</resultMap>
```

### 2.4 resultMap 高级用法

#### association（一对一）

一个用户对应一个详情档案：

```java
public class User {
    private Long id;
    private String userName;
    private UserProfile profile; // 一对一关联
}
```

**方式一：嵌套结果（推荐，单条 SQL 联合查询）**

```xml
<resultMap id="userWithProfile" type="User">
    <id property="id" column="id"/>
    <result property="userName" column="user_name"/>
    <!-- association 嵌套结果映射 -->
    <association property="profile" javaType="UserProfile">
        <id property="id" column="profile_id"/>
        <result property="realName" column="real_name"/>
        <result property="phone" column="phone"/>
        <result property="avatar" column="avatar"/>
    </association>
</resultMap>

<select id="selectUserWithProfile" resultMap="userWithProfile">
    SELECT u.*, p.id AS profile_id, p.real_name, p.phone, p.avatar
    FROM user u
    LEFT JOIN user_profile p ON u.id = p.user_id
    WHERE u.id = #{id}
</select>
```

**方式二：嵌套查询（延迟加载）**

```xml
<resultMap id="userWithProfileLazy" type="User">
    <id property="id" column="id"/>
    <result property="userName" column="user_name"/>
    <!-- select 指向另一个查询，column 为传给该查询的参数 -->
    <association property="profile"
                 javaType="UserProfile"
                 column="id"
                 select="com.example.mapper.ProfileMapper.selectByUserId"
                 fetchType="lazy"/>
</resultMap>
```

#### collection（一对多）

一个用户有多个订单：

```java
public class User {
    private Long id;
    private String userName;
    private List<Order> orders; // 一对多集合
}
```

```xml
<resultMap id="userWithOrders" type="User">
    <id property="id" column="id"/>
    <result property="userName" column="user_name"/>
    <!-- ofType 指定集合元素的类型 -->
    <collection property="orders" ofType="Order">
        <id property="id" column="order_id"/>
        <result property="orderNo" column="order_no"/>
        <result property="amount" column="amount"/>
        <result property="createTime" column="order_time"/>
    </collection>
</resultMap>

<select id="selectUserWithOrders" resultMap="userWithOrders">
    SELECT u.*, o.id AS order_id, o.order_no, o.amount, o.create_time AS order_time
    FROM user u
    LEFT JOIN order o ON u.id = o.user_id
    WHERE u.id = #{id}
</select>
```

**关于 N+1 的提示：** 使用嵌套查询的 `association`/`collection` 时，如果循环遍历结果集访问关联属性，每访问一次就会触发一条额外的 SQL（这就是 N+1 问题）。解决方案有两种：

1. 改用嵌套结果（单条 JOIN 查询）
2. 启用延迟加载的激进加载 `aggressiveLazyLoading=true`（会在一次会话中批量加载）

#### discriminator（鉴别器）

根据某列的值决定使用不同的结果映射，类似于 Java 中的 `switch`：

```xml
<resultMap id="vehicleMap" type="Vehicle">
    <id property="id" column="id"/>
    <result property="brand" column="brand"/>
    <discriminator javaType="int" column="vehicle_type">
        <case value="1" resultType="Car">
            <result property="doorCount" column="door_count"/>
        </case>
        <case value="2" resultType="Truck">
            <result property="loadCapacity" column="load_capacity"/>
        </case>
    </discriminator>
</resultMap>
```

#### extends（继承映射）

映射文件之间的 resultMap 复用：

```xml
<!-- 基础映射 -->
<resultMap id="baseUser" type="User">
    <id property="id" column="id"/>
    <result property="userName" column="user_name"/>
</resultMap>

<!-- 继承基础映射并扩展 -->
<resultMap id="userWithExt" type="User" extends="baseUser">
    <result property="email" column="email"/>
    <collection property="orders" ofType="Order" .../>
</resultMap>
```

### 2.5 延迟加载

延迟加载（Lazy Loading）允许我们在需要访问关联对象时才真正执行 SQL，而不是在主查询时一股脑加载所有数据。

**全局配置：**

```xml
<settings>
    <!-- 启用延迟加载 -->
    <setting name="lazyLoadingEnabled" value="true"/>
    <!-- 是否激进加载：true=在加载主对象时立即加载所有延迟属性 -->
    <!-- false=按需加载，访问哪个属性才加载哪个 -->
    <setting name="aggressiveLazyLoading" value="false"/>
    <!-- 延迟加载触发方法，默认值足够 -->
    <setting name="lazyLoadTriggerMethods" value="equals,clone,hashCode,toString"/>
</settings>
```

**按需细粒度控制：** 在每个 association/collection 上通过 `fetchType` 覆盖全局配置：

```xml
<association property="profile"
             select="...ProfileMapper.selectByUserId"
             column="id"
             fetchType="lazy"/>    <!-- 可选：lazy / eager -->
```

**注意事项：**

1. 延迟加载需要返回的是代理对象（CGLIB 或 Javassist），因此实体类不能是 `final` 的。
2. 在会话关闭后访问延迟加载属性会抛出 `LazyInitializationException`，解决方案：
   - 在事务范围内访问（Spring 的 `OpenSessionInViewFilter`）
   - 或调用 `Hibernate.initialize(proxy)` 主动初始化

**与 JPA 的对比：** JPA 的延迟加载也是通过代理实现，但 JPA 的持久化上下文（Persistence Context）使得会话管理更加复杂。MyBatis 的延迟加载相对轻量，但也因此缺少 JPA 的一级缓存与懒加载的深度集成。

---

## 3. 动态 SQL 编程

动态 SQL 是 MyBatis 最强大的特性之一，它允许我们在 XML 中编写条件判断、循环等逻辑，避免在 Java 代码中拼接 SQL 字符串。

### 3.1 if

最基础的判断标签，用于条件拼接：

```xml
<select id="findByCondition" resultType="User">
    SELECT * FROM user WHERE 1=1
    <if test="userName != null and userName != ''">
        AND user_name LIKE CONCAT('%', #{userName}, '%')
    </if>
    <if test="status != null">
        AND status = #{status}
    </if>
    <if test="createTime != null">
        AND create_time >= #{createTime}
    </if>
</select>
```

**注意：** `test` 属性中可以使用 OGNL 表达式，如 `!= null`、`!= ''`、`list != null and list.size() > 0` 等。

### 3.2 choose/when/otherwise

类似于 Java 的 `switch-case`，从多个条件中选择一个：

```xml
<select id="findByCondition" resultType="User">
    SELECT * FROM user WHERE status = 1
    <choose>
        <when test="userName != null and userName != ''">
            AND user_name LIKE CONCAT('%', #{userName}, '%')
        </when>
        <when test="email != null and email != ''">
            AND email = #{email}
        </when>
        <otherwise>
            AND create_time >= CURDATE()
        </otherwise>
    </choose>
</select>
```

### 3.3 trim/where/set

这些标签解决了动态 SQL 中最令人头疼的问题——多余的 `AND`/`OR`/逗号。

#### where

自动处理 WHERE 关键字，如果标签内有内容则插入 WHERE，并智能去除第一个 `AND` 或 `OR`：

```xml
<select id="findByCondition" resultType="User">
    SELECT * FROM user
    <where>
        <if test="userName != null and userName != ''">
            AND user_name LIKE CONCAT('%', #{userName}, '%')
        </if>
        <if test="status != null">
            AND status = #{status}
        </if>
    </where>
</select>
```

如果两个条件都不满足，不会生成 WHERE 关键字；如果只有 status 满足，生成的 SQL 为 `WHERE status = ?`（AND 被智能去除）。

#### set

用于 UPDATE 语句，智能处理 SET 关键字和多余逗号：

```xml
<update id="updateSelective" parameterType="User">
    UPDATE user
    <set>
        <if test="userName != null">user_name = #{userName},</if>
        <if test="email != null">email = #{email},</if>
        <if test="status != null">status = #{status},</if>
    </set>
    WHERE id = #{id}
</update>
```

如果只更新 email，生成的 SQL 为 `UPDATE user SET email = ? WHERE id = ?`（末尾逗号被智能去除）。

#### trim（自定义处理）

`where` 和 `set` 本质上是 `trim` 的特例：

```xml
<!-- 等价于 <where> -->
<trim prefix="WHERE" prefixOverrides="AND |OR ">
    ...
</trim>

<!-- 等价于 <set> -->
<trim prefix="SET" suffixOverrides=",">
    ...
</trim>
```

`trim` 的属性：

- `prefix`：在内容前添加的前缀
- `suffix`：在内容后添加的后缀
- `prefixOverrides`：去除内容开头的指定字符串
- `suffixOverrides`：去除内容末尾的指定字符串

### 3.4 foreach

用于批量操作，非常高频：

```xml
<!-- 批量查询：WHERE id IN (...)
     collection: 参数名
     item: 循环变量
     open/close: 前后缀
     separator: 分隔符 -->
<select id="selectByIds" resultType="User">
    SELECT * FROM user WHERE id IN
    <foreach collection="ids" item="id" open="(" close=")" separator=",">
        #{id}
    </foreach>
</select>
```

**collection 属性值的确定：**

| 参数类型 | @Param | collection 值 | 示例 |
|---------|--------|---------------|------|
| `List` | 无 | `list` | `List<Long> ids` |
| `List` | `@Param("ids")` | `ids` | `List<Long> ids` |
| 数组 `[]` | 无 | `array` | `Long[] ids` |
| 数组 `[]` | `@Param("ids")` | `ids` | `Long[] ids` |
| `Set` | `@Param("ids")` | `ids` | `Set<Long> ids` |

**批量插入：**

```xml
<insert id="batchInsert" useGeneratedKeys="true" keyProperty="id">
    INSERT INTO user(user_name, email, status)
    VALUES
    <foreach collection="list" item="user" separator=",">
        (#{user.userName}, #{user.email}, #{user.status})
    </foreach>
</insert>
```

### 3.5 bind

用于在 XML 中声明变量，常用于模糊查询的字符串拼接：

```xml
<select id="findByUserName" resultType="User">
    <!-- bind 将 OGNL 表达式的值赋给一个变量 -->
    <bind name="likeName" value="'%' + userName + '%'"/>
    SELECT * FROM user WHERE user_name LIKE #{likeName}
</select>
```

在不同数据库间移植时很有用——MySQL 用 `CONCAT`，Oracle 用 `||`，而 `bind` 屏蔽了这种差异。

### 3.6 sql/include

用于提取和复用 SQL 片段，减少重复：

```xml
<!-- 定义基列 -->
<sql id="baseColumns">
    id, user_name, email, status, create_time
</sql>

<!-- 定义 JOIN 片段 -->
<sql id="joinWithProfile">
    LEFT JOIN user_profile p ON u.id = p.user_id
</sql>

<sql id="whereClause">
    <where>
        <if test="userName != null">AND u.user_name = #{userName}</if>
    </where>
</sql>

<!-- 使用 -->
<select id="selectById" resultMap="userMap">
    SELECT <include refid="baseColumns"/>
    FROM user u
    <include refid="joinWithProfile"/>
    WHERE u.id = #{id}
</select>

<select id="findByCondition" resultType="User">
    SELECT <include refid="baseColumns"/>
    FROM user u
    <include refid="whereClause"/>
</select>
```

`include` 还可以通过 `<property>` 向 SQL 片段传递参数：

```xml
<sql id="someSql">
    SELECT ${alias}.id FROM user ${alias}
</sql>

<include refid="someSql">
    <property name="alias" value="u"/>
</include>
```

---

## 4. MyBatis-Plus 增强

### 4.1 核心优势

MyBatis-Plus（简称 MP）是 MyBatis 的增强工具，在 MyBatis 的基础上只做增强不做改变，为简化开发而生。

**三大核心优势：**

| 特性 | MyBatis | MyBatis-Plus |
|------|---------|--------------|
| CRUD | 需手写 Mapper XML | 继承 `BaseMapper` 即可 |
| 条件构造器 | 需手写 SQL | `QueryWrapper` / `LambdaQueryWrapper` |
| 分页 | 需手写 limit/方言 | `Page` 对象 + 分页插件 |
| 代码量 | 较高 | 降低约 50%-70% |

### 4.2 通用 CRUD（BaseMapper）

引入 MP 后，Mapper 接口只需继承 `BaseMapper` 即可获得大量通用方法：

```java
public interface UserMapper extends BaseMapper<User> {
    // 无需任何 XML 或注解即可使用以下方法：
    // int insert(T entity);
    // int deleteById(Serializable id);
    // int updateById(T entity);
    // T selectById(Serializable id);
    // List<T> selectList(Wrapper<T> queryWrapper);
    // IPage<T> selectPage(IPage<T> page, Wrapper<T> queryWrapper);
    // int deleteBatchIds(Collection<? extends Serializable> idList);
    // ... 以及其他数十个方法
}
```

**实体类映射：**

```java
@Data
@TableName("user")  // 指定表名（默认用类名驼峰转下划线）
public class User {
    @TableId(type = IdType.AUTO)  // 自增主键
    private Long id;

    private String userName;
    private String email;
    private Integer status;

    @TableField(fill = FieldFill.INSERT)  // 插入时自动填充
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)  // 插入和更新时自动填充
    private LocalDateTime updateTime;

    @TableLogic  // 逻辑删除字段
    private Integer deleted;
}
```

### 4.3 条件构造器

条件构造器是 MP 的灵魂，它提供了类型安全的 SQL 条件构建方式。

#### QueryWrapper（传统方式）

```java
// 构建条件：user_name LIKE '%张%' AND status = 1 AND create_time > '2024-01-01'
QueryWrapper<User> wrapper = new QueryWrapper<>();
wrapper.like("user_name", "张")
       .eq("status", 1)
       .gt("create_time", "2024-01-01")
       .orderByDesc("create_time");

List<User> users = userMapper.selectList(wrapper);
```

#### LambdaQueryWrapper（推荐，防字段名写错）

```java
// 使用 Lambda 表达式引用属性，编译期检查字段名
LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
wrapper.like(User::getUserName, "张")
       .eq(User::getStatus, 1)
       .gt(User::getCreateTime, LocalDate.of(2024, 1, 1))
       .orderByDesc(User::getCreateTime);

List<User> users = userMapper.selectList(wrapper);
```

**常用条件 API：**

```java
wrapper.eq(User::getStatus, 1)           // 等于
      .ne(User::getStatus, 0)            // 不等于
      .gt(User::getAge, 18)              // 大于
      .ge(User::getAge, 18)              // 大于等于
      .lt(User::getAge, 60)              // 小于
      .le(User::getAge, 60)              // 小于等于
      .between(User::getAge, 18, 60)     // BETWEEN
      .notBetween(User::getAge, 18, 60)  // NOT BETWEEN
      .like(User::getUserName, "张")      // LIKE '%张%'
      .notLike(User::getUserName, "张")   // NOT LIKE '%张%'
      .likeLeft(User::getUserName, "张")  // LIKE '%张'
      .likeRight(User::getUserName, "张") // LIKE '张%'
      .isNull(User::getEmail)            // IS NULL
      .isNotNull(User::getEmail)         // IS NOT NULL
      .in(User::getStatus, 1, 2, 3)      // IN
      .notIn(User::getStatus, 0)         // NOT IN
      .groupBy(User::getStatus)          // GROUP BY
      .having("COUNT(*) > {0}", 5)       // HAVING
      .orderByAsc(User::getCreateTime)   // ORDER BY ASC
      .orderByDesc(User::getCreateTime)  // ORDER BY DESC
      .last("LIMIT 10");                 // 追加任意 SQL 片段
```

### 4.4 分页插件

```java
@Configuration
public class MybatisPlusConfig {
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 添加分页拦截器，指定数据库类型
        interceptor.addInnerInterceptor(
                new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
```

使用：

```java
// 第1页，每页10条
Page<User> page = new Page<>(1, 10);

// 执行分页查询
Page<User> result = userMapper.selectPage(page, null);

// 获取分页数据
List<User> records = result.getRecords();    // 当前页数据
long total = result.getTotal();              // 总记录数
long pages = result.getPages();              // 总页数
boolean hasNext = result.hasNext();          // 是否有下一页
```

### 4.5 高级特性

#### ActiveRecord 模式

ActiveRecord 模式让实体类直接操作数据库：

```java
@Data
@EqualsAndHashCode(callSuper = false)
public class User extends Model<User> {
    @TableId
    private Long id;
    private String userName;
    private String email;

    // 使用 ActiveRecord 模式
    public static void main(String[] args) {
        User user = new User();
        user.setUserName("张三");
        user.insert();  // 直接插入

        User u = new User().selectById().setId(1L);  // 查询
        u.setEmail("new@email.com");
        u.updateById();  // 更新

        u.deleteById();  // 删除
    }
}
```

#### 代码生成器

MP 的代码生成器（AutoGenerator）可以根据数据库表一键生成 Entity、Mapper、Service、Controller：

```java
public class CodeGenerator {
    public static void main(String[] args) {
        FastAutoGenerator.create("jdbc:mysql://localhost:3306/db", "root", "password")
            .globalConfig(builder -> builder
                .author("开发者")
                .outputDir("src/main/java")
                .enableSwagger()
            )
            .packageConfig(builder -> builder
                .parent("com.example")
                .entity("entity")
                .mapper("mapper")
                .service("service")
                .controller("controller")
            )
            .strategyConfig(builder -> builder
                .addInclude("user", "order", "product")  // 需要生成的表
                .addTablePrefix("t_")                     // 表前缀过滤
                .entityBuilder().enableLombok()
                .controllerBuilder().enableRestStyle()
            )
            .execute();
    }
}
```

#### 逻辑删除

```java
// 实体类
@TableLogic
private Integer deleted;  // 0-未删除，1-已删除

// 配置
mybatis-plus:
  global-config:
    db-config:
      logic-delete-field: deleted
      logic-delete-value: 1      # 逻辑已删除值
      logic-not-delete-value: 0  # 逻辑未删除值
```

使用后，`deleteById` 实际执行的是 `UPDATE user SET deleted=1 WHERE id=?`，`selectList` 会自动追加 `AND deleted=0`。

#### 自动填充

```java
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {
    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createTime", LocalDateTime::now, LocalDateTime.class);
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime::now, LocalDateTime.class);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime::now, LocalDateTime.class);
    }
}
```

#### 乐观锁

```java
// 实体类
@Version
private Integer version;  // 版本号，每次更新 +1

// 配置
@Bean
public MybatisPlusInterceptor mybatisPlusInterceptor() {
    MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
    interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
    return interceptor;
}
```

乐观锁的工作原理：

```sql
-- 更新时自动附加 version 条件，并执行 version + 1
UPDATE user SET user_name = '新名字', version = version + 1
WHERE id = ? AND version = 旧版本号
-- 如果影响行数为 0，说明数据已被其他线程修改，抛出异常
```

**与 JPA 的对比：** JPA 同样支持乐观锁（`@Version`），但 JPA 的版本冲突处理更加集成化（通过 `OptimisticLockException`）。MyBatis-Plus 的乐观锁更轻量，但需要用户自行处理冲突场景。

### 4.6 与 MyBatis 的关系

MyBatis-Plus 不是替代 MyBatis 的框架，而是 MyBatis 的增强工具。理解这个关系至关重要：

- **无侵入：** MP 不强奸 MyBatis 的使用方式，你可以同时使用 MP 的 `BaseMapper` 和自己手写的 XML 映射。
- **共存：** MP 的 `MybatisPlusInterceptor` 基于 MyBatis 的 `Interceptor` 机制实现，本质上是 MyBatis 插件。
- **回退：** 当 MP 无法满足复杂查询时，你完全可以回到手写 SQL + resultMap 的方式。

---

## 5. 最佳实践与性能优化

### 5.1 性能优化

#### 批量操作

**方式一：foreach 批量 SQL（推荐，一次网络往返）**

```xml
<insert id="batchInsert">
    INSERT INTO user(user_name, email) VALUES
    <foreach collection="list" item="u" separator=",">
        (#{u.userName}, #{u.email})
    </foreach>
</insert>
```

注意：MySQL 对单条 SQL 的长度有限制（`max_allowed_packet`，默认 4MB），单次批量建议控制在 1000-5000 条。

**方式二：ExecutorType.BATCH（对大量数据且需要事务时）**

```java
// 在 Spring 中配置 BATCH 执行器
@Bean
public SqlSessionTemplate sqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
    return new SqlSessionTemplate(sqlSessionFactory, ExecutorType.BATCH);
}

// 使用 BATCH 模式（需手动 flush）
@Autowired
private SqlSessionTemplate sqlSessionTemplate;

public void batchInsert(List<User> users) {
    UserMapper mapper = sqlSessionTemplate.getMapper(UserMapper.class);
    for (int i = 0; i < users.size(); i++) {
        mapper.insert(users.get(i));
        // 每 1000 条 flush 一次，避免缓存过大
        if (i % 1000 == 0 && i > 0) {
            sqlSessionTemplate.flushStatements();
        }
    }
    sqlSessionTemplate.flushStatements();
}
```

BATCH 模式将多条 INSERT 语句打包成一次网络往返，但不会返回自增主键（除非手动 flush）。

#### 分页

**PageHelper（传统 MyBatis 分页）：**

```java
// 分页必须在查询之前调用（PageHelper 通过 ThreadLocal 传递分页参数）
PageHelper.startPage(1, 10);
List<User> users = userMapper.findAll();
PageInfo<User> pageInfo = new PageInfo<>(users);
// pageInfo.getTotal(), pageInfo.getPages(), pageInfo.getList()
```

**MyBatis-Plus 分页（推荐）：**

```java
Page<User> page = new Page<>(1, 10);
Page<User> result = userMapper.selectPage(page,
    new LambdaQueryWrapper<User>().eq(User::getStatus, 1));
```

**与 JPA 的对比：** JPA 的 `Pageable` / `Page` 分页标准是标准化的，在多数据源间切换更自然。MyBatis-Plus 的分页通过拦截器自动生成 count 查询和 limit 语句，对复杂 SQL 的 count 优化（如去除 `ORDER BY`）更加智能。

#### N+1 问题与处理

N+1 问题是 ORM 框架中最常见的性能陷阱：

```java
// N+1 问题复现
List<User> users = userMapper.findAll();  // 1 条 SQL
for (User user : users) {
    System.out.println(user.getProfile().getPhone());  // N 条 SQL（延迟加载触发的额外查询）
}
```

**解决方案：**

1. **使用 JOIN 查询（推荐）：** 在 SQL 层面一次性 JOIN 查询所有关联表。
2. **使用 `@BatchSize`：** MyBatis 没有内置此功能，可以手动使用 `IN` 查询批量加载。
3. **关联查询+resultMap：** 利用 `collection`/`association` 的嵌套结果方式。
4. **MyBatis-Plus 的 `selectBatchIds`：** 批量查询代替循环查询。

### 5.2 SQL 调试

#### MyBatis 日志输出

```xml
<settings>
    <setting name="logImpl" value="STDOUT_LOGGING"/>
    <!-- 其他可选值：SLF4J | LOG4J | LOG4J2 | JDK_LOGGING | COMMONS_LOGGING | NO_LOGGING -->
</settings>
```

在 Spring Boot 中配置：

```yaml
# application.yml
mybatis:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl

# 更推荐的方式：使用 MyBatis 的日志前缀控制日志级别
logging:
  level:
    com.example.mapper: debug  # 只打印 Mapper 包下的 SQL
```

#### p6spy — SQL 分析利器

p6spy 通过代理 JDBC 驱动拦截所有 SQL 执行，可以打印出完整的 SQL（含参数值）和执行耗时。

```yaml
# 1. 引入依赖
# <dependency><groupId>p6spy</groupId><artifactId>p6spy</artifactId></dependency>

# 2. 修改数据源驱动
spring:
  datasource:
    driver-class-name: com.p6spy.engine.spy.P6SpyDriver
    url: jdbc:p6spy:mysql://localhost:3306/db

# 3. spy.properties 配置
appender=com.p6spy.engine.spy.appender.Slf4JLogger
logMessageFormat=com.p6spy.engine.spy.appender.CustomLineFormat
customLogMessageFormat=%(currentTime)|%(executionTime)ms|%(sqlSingleLine)
```

#### 慢 SQL 拦截插件

自定义 MyBatis 插件监控慢 SQL：

```java
@Intercepts({
    @Signature(type = StatementHandler.class, method = "query", args = {Statement.class, ResultHandler.class}),
    @Signature(type = StatementHandler.class, method = "update", args = {Statement.class})
})
@Component
public class SlowSqlInterceptor implements Interceptor {

    private static final long SLOW_TIME_MS = 1000L; // 慢 SQL 阈值

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            return invocation.proceed();
        } finally {
            long cost = System.currentTimeMillis() - start;
            if (cost > SLOW_TIME_MS) {
                StatementHandler handler = (StatementHandler) invocation.getTarget();
                BoundSql boundSql = handler.getBoundSql();
                String sql = boundSql.getSql();
                // 替换占位符为实际参数值（简化版本）
                log.warn("慢 SQL 告警 | 耗时: {}ms | SQL: {}", cost, formatSql(sql, boundSql));
            }
        }
    }
}
```

### 5.3 插件机制

MyBatis 的四大对象都允许被插件拦截：

| 拦截对象 | 拦截方法 | 用途 |
|----------|----------|------|
| `Executor` | update, query, flushStatements, commit, rollback, getTransaction, close, isClosed | 缓存、分页、事务管理 |
| `StatementHandler` | prepare, parameterize, batch, update, query | SQL 重写、分页逻辑 |
| `ParameterHandler` | getParameterObject, setParameters | 参数处理 |
| `ResultSetHandler` | handleResultSets, handleOutputParameters | 结果处理 |

**分页插件实现原理：**

```java
@Intercepts({
    @Signature(type = Executor.class, method = "query",
              args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class})
})
public class PageInterceptor implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object[] args = invocation.getArgs();
        MappedStatement ms = (MappedStatement) args[0];
        Object parameter = args[1];

        // 检测是否需要分页
        if (parameter instanceof Page) {
            Page page = (Page) parameter;
            // 1. 生成 count 查询并执行
            String countSql = generateCountSql(ms.getBoundSql(parameter).getSql());
            // ... 执行 count 查询

            // 2. 改写 SQL 添加 LIMIT/OFFSET
            String pageSql = generatePageSql(ms.getBoundSql(parameter).getSql(), page);
            // ... 执行分页查询
        }

        return invocation.proceed();
    }
}
```

**与 JPA 的对比：** JPA 的 `Criteria API` 在编译期生成查询元模型，而 MyBatis 的插件机制在运行期拦截和改写 SQL。前者更安全（编译期检查），后者更灵活（可以做 SQL 审计、分片、加密等）。

### 5.4 缓存机制

#### 一级缓存（SqlSession 级别）

一级缓存是 MyBatis 默认开启的缓存，作用域为一个 SqlSession：

```java
// 同一个 SqlSession，同一个查询在缓存未失效前只执行一次 SQL
try (SqlSession session = sqlSessionFactory.openSession()) {
    UserMapper mapper = session.getMapper(UserMapper.class);

    User u1 = mapper.selectById(1L);  // 执行 SQL，放入缓存
    User u2 = mapper.selectById(1L);  // 从缓存读取，不执行 SQL
    System.out.println(u1 == u2);     // true（同一对象引用）
}
```

**一级缓存失效的情况：**

1. 执行了 INSERT/UPDATE/DELETE 操作（包含 `flushCache=true` 的查询）
2. 手动调用 `sqlSession.clearCache()`
3. 执行了不同条件的查询
4. 跨 SqlSession

**注意陷阱：** 在 Spring 托管的 MyBatis 中，如果未开启事务，每个 Mapper 方法会使用独立的 SqlSession（通过 `SqlSessionTemplate` 的 `getMapper` 获取），一级缓存天然失效。只有将多个查询放在同一个事务中，才能利用一级缓存：

```java
@Transactional
public void cachedQuery() {
    userMapper.selectById(1L);  // 第一次，执行 SQL
    userMapper.selectById(1L);  // 第二次，命中一级缓存
}
```

#### 二级缓存（Mapper 级别）

二级缓存的作用域跨 SqlSession，按 namespace 隔离：

```xml
<mapper namespace="com.example.mapper.UserMapper">
    <!-- 开启二级缓存 -->
    <cache eviction="LRU"        <!-- 淘汰策略：LRU/FIFO/SOFT/WEAK -->
          flushInterval="60000"  <!-- 刷新间隔(ms) -->
          size="512"             <!-- 缓存引用数量 -->
          readOnly="false"/>     <!-- 是否只读 -->
</mapper>
```

**实体需要序列化：**

```java
public class User implements Serializable {
    private static final long serialVersionUID = 1L;
    // ...
}
```

**二级缓存注意事项：**

- 脏读问题：两个表通过 JOIN 查询的结果缓存到某个 namespace 下，如果另一个 mapper 修改了关联表的数据，缓存不会失效。
- 细粒度控制：`<select>` 上的 `useCache="false"` 可以跳过二级缓存。
- 更新操作：INSERT/UPDATE/DELETE 会清空所在 namespace 的二级缓存。

**推荐策略：** 大多数业务场景仅使用一级缓存即可，二级缓存在高并发且数据更新不频繁的读多写少场景下使用。对于关键业务数据，更推荐使用 Redis 等外部缓存，而非 MyBatis 二级缓存。

**与 JPA 的对比：** JPA 的一级缓存（Persistence Context）比 MyBatis 更强大，它不仅能缓存查询结果，还能跟踪实体状态变化（脏检查），在 flush 时自动提交变更。

---

## 6. 与 Spring Boot 整合

### 6.1 mybatis-spring-boot-starter

```xml
<dependency>
    <groupId>org.mybatis.spring.boot</groupId>
    <artifactId>mybatis-spring-boot-starter</artifactId>
    <version>3.0.3</version>
</dependency>
```

**完整的配置示例：**

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/db?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: root
    password: 123456
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      minimum-idle: 5
      maximum-pool-size: 20
      connection-timeout: 30000

mybatis:
  # mapper.xml 文件位置
  mapper-locations: classpath:mapper/**/*.xml
  # 实体类所在包（自动注册别名）
  type-aliases-package: com.example.entity
  configuration:
    # 驼峰命名转换
    map-underscore-to-camel-case: true
    # 延迟加载
    lazy-loading-enabled: true
    aggressive-lazy-loading: false
    # 日志
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
    # 二级缓存
    cache-enabled: true
  # 指定全局配置文件（如果使用 Java 配置则不需要）
  # config-location: classpath:mybatis-config.xml
```

**@Mapper 与 @MapperScan：**

```java
// 方式1：在每个 Mapper 接口上添加 @Mapper
@Mapper
public interface UserMapper {
    // ...
}

// 方式2：在配置类上添加 @MapperScan（推荐，全局扫描）
@SpringBootApplication
@MapperScan("com.example.mapper")  // 扫描指定包下的所有 Mapper
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

**SqlSession 的生命周期管理：** Spring 整合后，`SqlSessionTemplate` 会帮我们管理 SqlSession 的生命周期，每个线程绑定一个 SqlSession，在事务提交或回滚时自动关闭。不需要手动 try-with-resources。

### 6.2 多数据源配置

使用 `dynamic-datasource-spring-boot-starter`（baomidou 出品，MyBatis-Plus 官方推荐）：

```xml
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>dynamic-datasource-spring-boot-starter</artifactId>
    <version>4.3.0</version>
</dependency>
```

**配置：**

```yaml
spring:
  datasource:
    dynamic:
      primary: master                    # 默认数据源
      strict: true                       # 严格模式，未匹配到数据源时抛出异常
      datasource:
        master:
          url: jdbc:mysql://localhost:3306/db_master
          username: root
          password: 123456
          driver-class-name: com.mysql.cj.jdbc.Driver
        slave_1:
          url: jdbc:mysql://localhost:3306/db_slave_1
          username: root
          password: 123456
          driver-class-name: com.mysql.cj.jdbc.Driver
        slave_2:
          url: jdbc:mysql://localhost:3306/db_slave_2
          username: root
          password: 123456
          driver-class-name: com.mysql.cj.jdbc.Driver
```

**使用 @DS 注解切换数据源：**

```java
@Service
public class UserService {

    // 默认使用 master（primary 数据源）
    public List<User> findAll() {
        return userMapper.selectList(null);
    }

    @DS("slave_1")        // 指定使用 slave_1 数据源
    public List<User> findFromSlave1() {
        return userMapper.selectList(null);
    }

    @DS("slave_2")        // 指定使用 slave_2 数据源
    public List<User> findFromSlave2() {
        return userMapper.selectList(null);
    }

    @DS("master")         // 强制走主库
    @Transactional
    public void saveUser(User user) {
        userMapper.insert(user);
    }
}
```

**读写分离策略：**

虽然 `@DS` 可以实现手动读写分离，但在实际项目中，更推荐结合 AOP 自动切换：

```java
@Aspect
@Component
public class DataSourceAspect {

    @Around("@annotation(ds)")
    public Object switchDs(ProceedingJoinPoint point, DS ds) throws Throwable {
        // @DS 注解已经由 dynamic-datasource 自动处理
        // 这里可以加入业务逻辑，如：强制走主库
        return point.proceed();
    }

    // 或者基于方法名自动切换
    @Around("execution(* com.example.service.*.*(..))")
    public Object autoSwitchDs(ProceedingJoinPoint point) throws Throwable {
        String methodName = point.getSignature().getName();
        if (methodName.startsWith("get") || methodName.startsWith("find")
            || methodName.startsWith("select") || methodName.startsWith("query")) {
            DynamicDataSourceContextHolder.push("slave");
        } else {
            DynamicDataSourceContextHolder.push("master");
        }
        try {
            return point.proceed();
        } finally {
            DynamicDataSourceContextHolder.poll();
        }
    }
}
```

### 6.3 MyBatis-Spring 整合原理

Spring 整合 MyBatis 的核心是 `SqlSessionTemplate`：

```java
public class SqlSessionTemplate implements SqlSession {

    private final SqlSessionFactory sqlSessionFactory;
    private final ExecutorType executorType;
    private final SqlSession sqlSessionProxy;  // 代理对象

    public SqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
        // 创建 JDK 动态代理，代理 SqlSession 的每个方法
        this.sqlSessionProxy = (SqlSession) Proxy.newProxyInstance(
            SqlSessionFactory.class.getClassLoader(),
            new Class[]{SqlSession.class},
            new SqlSessionInterceptor());
    }

    private class SqlSessionInterceptor implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            // 核心逻辑：获取当前事务绑定的 SqlSession
            SqlSession sqlSession = getSqlSession(
                SqlSessionTemplate.this.sqlSessionFactory,
                SqlSessionTemplate.this.executorType,
                holder);
            // 如果存在事务，使用事务绑定的 SqlSession（同一 SqlSession）
            // 如果不存在事务，每次创建新的 SqlSession
            try {
                return method.invoke(sqlSession, args);
            } finally {
                if (!isTransactionActive) {
                    sqlSession.close();  // 非事务下自动关闭
                }
            }
        }
    }
}
```

这就是为什么在 Spring 中使用 MyBatis 时，事务内共享同一个 SqlSession（一级缓存生效），事务外每个 Mapper 方法单独使用 SqlSession。

---

## 7. 总结清单

### 架构理解

- [ ] 掌握 MyBatis 核心链路：`SqlSessionFactoryBuilder → SqlSessionFactory → SqlSession → Executor → StatementHandler → ResultSetHandler`
- [ ] 理解 MapperProxy 动态代理的实现原理
- [ ] 掌握 mybatis-config.xml 的常用配置项含义
- [ ] 理解 namespace 与 Mapper 接口的绑定关系

### SQL 映射

- [ ] 熟练使用 CRUD 标签，掌握 `useGeneratedKeys` + `keyProperty` 获取自增主键
- [ ] 理解 `#{}` 与 `${}` 的区别，只在表名列名等元数据处使用 `${}`
- [ ] 掌握多参数传递的四种方式，推荐 `@Param` 注解或 `-parameters` 编译选项
- [ ] 熟练使用 resultType 自动映射，开启 `mapUnderscoreToCamelCase`
- [ ] 掌握 resultMap 高级映射：association（一对一）、collection（一对多）、discriminator、extends
- [ ] 理解嵌套查询 vs 嵌套结果各自适用场景
- [ ] 理解延迟加载的原理和配置，注意 `LazyInitializationException`

### 动态 SQL

- [ ] 熟练使用 if/choose/trim/where/set/foreach 标签
- [ ] 理解 foreach 的 collection 参数命名规则
- [ ] 使用 sql/include 抽取公共 SQL 片段
- [ ] 使用 bind 进行数据库无关的字符串拼接

### MyBatis-Plus

- [ ] 掌握 BaseMapper 通用 CRUD 方法
- [ ] 熟练使用 LambdaQueryWrapper 构建类型安全的条件
- [ ] 配置分页插件 PaginationInnerInterceptor
- [ ] 掌握逻辑删除（@TableLogic）、自动填充（@TableField fill）、乐观锁（@Version）
- [ ] 理解 MP 是对 MyBatis 的增强而非替代

### 最佳实践

- [ ] 使用 foreach 批量 SQL 进行批量操作，注意 SQL 长度限制
- [ ] 使用分页插件代替手写 limit
- [ ] 使用 JOIN 查询解决 N+1 问题
- [ ] 配置 SQL 日志输出，使用 p6spy 分析慢 SQL
- [ ] 理解 MyBatis 插件机制，掌握四大拦截对象
- [ ] 理解一级缓存（SqlSession 级别）和二级缓存（Mapper 级别）的作用域和失效条件

### Spring Boot 整合

- [ ] 使用 `mybatis-spring-boot-starter` 快速整合
- [ ] 使用 `@MapperScan` 批量注册 Mapper
- [ ] 配置多数据源，使用 `@DS` 注解切换

---

> **最后的话：** MyBatis 的核心哲学是"SQL 优先"——它将 SQL 的控制权完整地交还给开发者，不做过度的自动化和抽象。这与 JPA 的"领域模型驱动"形成了鲜明的对比。在实际项目中，选择 MyBatis 还是 JPA 取决于团队对 SQL 的控制需求：如果追求灵活、透明、可优化的 SQL 控制，MyBatis 是更好的选择；如果追求标准的对象关系映射和更少的 SQL 编写，JPA 更适合。无论选择哪个，理解 MyBatis 的底层架构和 SQL 映射原理，都是一名合格 Java 后端开发者的必修课。
