# 03 - 动态 SQL 实战

> 🎯 动态 SQL 是 MyBatis 最强大的特性——`<if>`、`<foreach>`、`<choose>`、`<where>`、`<trim>`、`<set>` 六大标签，让 SQL 像代码一样灵活组合

---

## 目录

1. [六大标签速查](#1-六大标签速查)
2. [实战场景](#2-实战场景)
3. [性能陷阱](#3-性能陷阱)

---

## 1. 六大标签速查

```xml
<!-- 1. if：条件判断 -->
<select id="findByCondition" resultType="User">
    SELECT * FROM users
    WHERE 1=1
    <if test="name != null and name != ''">
        AND name = #{name}
    </if>
    <if test="email != null">
        AND email = #{email}
    </if>
</select>

<!-- 2. where（自动去除 AND/OR + 处理空条件） -->
<select id="findByCondition" resultType="User">
    SELECT * FROM users
    <where>
        <if test="name != null">AND name = #{name}</if>
        <if test="email != null">AND email = #{email}</if>
    </where>
    <!-- 如果两个条件都为空 → WHERE 不输出！ -->
</select>

<!-- 3. foreach：集合遍历 -->
<select id="findByIds" resultType="User">
    SELECT * FROM users WHERE id IN
    <foreach collection="ids" item="id" open="(" separator="," close=")">
        #{id}
    </foreach>
</select>
<!-- 批量插入 -->
<insert id="batchInsert">
    INSERT INTO users (name, email) VALUES
    <foreach collection="users" item="u" separator=",">
        (#{u.name}, #{u.email})
    </foreach>
</insert>

<!-- 4. choose-when-otherwise（switch-case） -->
<select id="findByPriority" resultType="User">
    SELECT * FROM users
    <where>
        <choose>
            <when test="id != null">AND id = #{id}</when>
            <when test="email != null">AND email = #{email}</when>
            <otherwise>AND status = 'ACTIVE'</otherwise>
        </choose>
    </where>
</select>

<!-- 5. set（UPDATE 自动去逗号） -->
<update id="updateSelective">
    UPDATE users
    <set>
        <if test="name != null">name = #{name},</if>
        <if test="email != null">email = #{email},</if>
    </set>
    WHERE id = #{id}
</update>

<!-- 6. trim（自定义前缀/后缀/去除） -->
<select id="findByCondition" resultType="User">
    SELECT * FROM users
    <trim prefix="WHERE" prefixOverrides="AND |OR ">
        <if test="name != null">AND name = #{name}</if>
        <if test="email != null">AND email = #{email}</if>
    </trim>
</select>
```

## 2. 实战场景

```xml
<!-- 多条件组合查询（最常见的场景） -->
<select id="searchUsers" resultType="User">
    SELECT * FROM users
    <where>
        <if test="keyword != null">
            AND (name LIKE CONCAT('%', #{keyword}, '%')
                 OR email LIKE CONCAT('%', #{keyword}, '%'))
        </if>
        <if test="status != null">
            AND status = #{status}
        </if>
        <if test="createTimeStart != null">
            AND create_time >= #{createTimeStart}
        </if>
        <if test="createTimeEnd != null">
            AND create_time <![CDATA[ <= ]]> #{createTimeEnd}
        </if>
    </where>
    ORDER BY create_time DESC
</select>

<!-- 批量操作 -->
<update id="batchUpdateStatus">
    UPDATE users SET status = #{status}
    WHERE id IN
    <foreach collection="ids" item="id" open="(" separator="," close=")">
        #{id}
    </foreach>
</update>
```

## 3. 性能陷阱

```xml
<!-- ❌ foreach 在 IN 中数据量过大 → 产生超长 SQL -->
<select id="findByIds">
    SELECT * FROM users WHERE id IN
    <foreach collection="ids" item="id" separator=",">#{id}</foreach>
    <!-- ids 有 10000 个 → SQL 长达 100KB → MySQL 可能拒绝执行 -->
</select>

<!-- ✅ 解决：分批 IN 或临时表 JOIN -->
```

## 核心要点回顾

- `<where>` 优先于 `WHERE 1=1`（自动处理 AND/OR）
- `<set>` 优先于手动逗号处理（UPDATE 场景）
- `<foreach>` 的 `item` 不能和外部变量重名
- `CDATA` 处理 `<`、`>` 等 XML 特殊字符
- 动态 SQL 的 OGNL 表达式里 `and`/`or` 比 `&&`/`||` 更可靠

## 参考资料

1. MyBatis 动态 SQL 文档
