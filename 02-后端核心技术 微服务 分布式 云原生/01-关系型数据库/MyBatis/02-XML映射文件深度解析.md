# 02 - XML 映射文件深度解析

> 🎯 XML Mapper 是 MyBatis 的灵魂——resultMap 解决了 90% 的 ORM 难题，association/collection 处理复杂嵌套，discriminator 实现多态映射

---

## 目录

1. [CRUD 元素速查](#1-crud-元素速查)
2. [resultMap 详解](#2-resultmap-详解)
3. [关联映射](#3-关联映射)

---

## 1. CRUD 元素速查

```xml
<!-- 查询 -->
<select id="findById" resultType="User">
    SELECT * FROM users WHERE id = #{id}
</select>

<!-- 插入（返回自增ID） -->
<insert id="insert" useGeneratedKeys="true" keyProperty="id">
    INSERT INTO users (name, email) VALUES (#{name}, #{email})
</insert>

<!-- 更新 -->
<update id="update">
    UPDATE users SET name = #{name} WHERE id = #{id}
</update>

<!-- 删除 -->
<delete id="deleteById">
    DELETE FROM users WHERE id = #{id}
</delete>
```

### #{} vs ${}

| 语法 | 机制 | 防注入 | 使用场景 |
|------|------|:---:|------|
| `#{name}` | **PreparedStatement ?** 占位 | ✅ 安全 | 99% 的场景 |
| `${name}` | **字符串拼接** | ❌ 危险 | ORDER BY / 动态表名 |

```xml
<!-- ✅ 安全 -->
<select id="findByName" resultType="User">
    SELECT * FROM users WHERE name = #{name}
</select>

<!-- ⚠️ 仅用于 ORDER BY（白名单校验后） -->
<select id="findByOrder">
    SELECT * FROM users ORDER BY ${column} ${direction}
</select>
```

## 2. resultMap 详解

```xml
<!-- resultType：简单场景（列名=属性名） -->
<select id="findAll" resultType="com.example.User"/>

<!-- resultMap：复杂场景（列名≠属性名 或 嵌套映射） -->
<resultMap id="userMap" type="User">
    <id property="id" column="user_id"/>              <!-- 主键 -->
    <result property="name" column="user_name"/>
    <result property="email" column="user_email"/>
    <result property="createdAt" column="created_at"
            javaType="java.time.LocalDateTime"/>       <!-- 类型转换 -->
    <result property="status" column="status"
            typeHandler="com.example.StatusHandler"/>  <!-- 自定义处理器 -->
</resultMap>
```

### 构造器注入

```xml
<resultMap id="userMap" type="User">
    <constructor>                        <!-- 使用构造函数 -->
        <idArg column="id" javaType="long"/>
        <arg column="name" javaType="String"/>
    </constructor>
    <result property="email" column="email"/>
</resultMap>
```

## 3. 关联映射

```xml
<!-- 一对一：association -->
<resultMap id="orderMap" type="Order">
    <id property="id" column="id"/>
    <result property="amount" column="amount"/>
    <association property="user" javaType="User">
        <id property="id" column="user_id"/>
        <result property="name" column="user_name"/>
    </association>
</resultMap>

<!-- 一对多：collection -->
<resultMap id="userOrdersMap" type="User">
    <id property="id" column="id"/>
    <result property="name" column="name"/>
    <collection property="orders" ofType="Order">
        <id property="id" column="order_id"/>
        <result property="amount" column="amount"/>
    </collection>
</resultMap>
```

### 嵌套查询 vs 嵌套结果

```xml
<!-- 嵌套查询（N+1 问题！） -->
<association property="user" column="user_id"
    select="com.example.UserMapper.findById"/>    <!-- 再查一次 -->

<!-- 嵌套结果（推荐，一条 SQL） -->
<association property="user" javaType="User">
    <id property="id" column="user_id"/>          <!-- 同一 SQL 的列 -->
</association>
```

## 核心要点回顾

- 99% 场景用 `#{}`（预编译安全），`${}` 只用 ORDER BY
- `resultMap` 解决列名≠属性名、嵌套对象、类型转换
- `association`(一对一) / `collection`(一对多)
- 嵌套结果（一条 SQL）> 嵌套查询（N+1 问题）
- `useGeneratedKeys` 自动回填自增主键

## 参考资料

1. MyBatis XML Mapper 文档
