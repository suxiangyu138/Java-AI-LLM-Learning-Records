# MyBatis 动态 SQL 详解（XML + 注解方式）

> **定位**：动态 SQL 是 MyBatis 核心特性——根据传入参数条件动态生成不同 SQL，避免手动拼接带来的冗余、错误和 SQL 注入。支持 XML 和注解两种方式。

---

## 目录

1. [核心原理](#1-核心原理)
2. [XML 方式动态 SQL](#2-xml-方式动态-sql)
3. [注解方式动态 SQL](#3-注解方式动态-sql)
4. [两种方式对比](#4-两种方式对比)
5. [注意事项](#5-注意事项)

---

## 1. 核心原理

> MyBatis 动态 SQL 基于 **OGNL**（对象图导航语言）解析参数，根据参数条件动态拼接。

| 优势 | 说明 |
|------|------|
| 无需手动拼接 SQL | 减少语法错误和注入风险 |
| 动态适配参数 | 多条件查询、动态更新等 |
| XML/注解灵活选择 | 适配不同复杂度 |

---

## 2. XML 方式动态 SQL

### 2.1 核心标签速查

| 标签 | 作用 | 说明 |
|------|------|------|
| `<if>` | 条件判断 | `test` 属性 OGNL 表达式 |
| `<where>` | 替代 `WHERE` 关键字 | 自动删除多余的 `AND/OR` |
| `<set>` | 用于 `UPDATE` | 自动删除多余逗号 |
| `<foreach>` | 循环遍历 | `collection`/`item`/`separator`/`open`/`close` |
| `<choose><when><otherwise>` | 多分支判断 | 类似 `switch-case` |

### 2.2 多条件查询（`<if>` + `<where>`）

```xml
<select id="selectUserByCondition" resultType="user">
    SELECT * FROM user
    <where>
        <if test="username != null and username != ''">
            AND username LIKE CONCAT('%', #{username}, '%')
        </if>
        <if test="age != null">
            AND age = #{age}
        </if>
    </where>
</select>
```

### 2.3 动态更新（`<if>` + `<set>`）

```xml
<update id="updateUserDynamic">
    UPDATE user
    <set>
        <if test="username != null and username != ''">username = #{username},</if>
        <if test="age != null">age = #{age},</if>
    </set>
    WHERE id = #{id}
</update>
```

### 2.4 批量操作（`<foreach>`）

```xml
<!-- 批量查询（IN） -->
<select id="selectUserByIds" resultType="user">
    SELECT * FROM user
    WHERE id IN
    <foreach collection="ids" item="id" open="(" close=")" separator=",">
        #{id}
    </foreach>
</select>

<!-- 批量插入 -->
<insert id="insertUserBatch">
    INSERT INTO user (username, password, age) VALUES
    <foreach collection="list" item="user" separator=",">
        (#{user.username}, #{user.password}, #{user.age})
    </foreach>
</insert>
```

### 2.5 多分支判断（`<choose>`）

```xml
<select id="selectUserByChoose" resultType="user">
    SELECT * FROM user
    <where>
        <choose>
            <when test="id != null">AND id = #{id}</when>
            <when test="username != null">AND username LIKE CONCAT('%', #{username}, '%')</when>
            <otherwise>AND 1=1</otherwise>
        </choose>
    </where>
</select>
```

---

## 3. 注解方式动态 SQL

### 3.1 Provider 注解（推荐）

**Provider 类**：

```java
public class UserProvider {
    public String selectUserByCondition(String username, Integer age) {
        return new SQL() {{
            SELECT("*");
            FROM("user");
            if (username != null && !username.equals("")) {
                WHERE("username LIKE CONCAT('%', #{username}, '%')");
            }
            if (age != null) {
                WHERE("age = #{age}");
            }
        }}.toString();
    }
}
```

**Mapper 接口引用**：

```java
@SelectProvider(type = UserProvider.class, method = "selectUserByCondition")
List<User> selectUserByCondition(@Param("username") String username,
                                  @Param("age") Integer age);
```

### 3.2 动态 SQL 注解（MyBatis 3.5+）

```java
@Select("SELECT * FROM user")
@Where({
    @If(test = "username != null and username != ''",
        value = "AND username LIKE CONCAT('%', #{username}, '%')"),
    @If(test = "age != null", value = "AND age = #{age}")
})
List<User> selectUserByCondition(@Param("username") String username,
                                  @Param("age") Integer age);
```

---

## 4. 两种方式对比

| 维度 | XML 方式 | 注解方式 |
|------|----------|----------|
| **可读性** | ✅ 标签清晰，嵌套直观 | 简单清晰，复杂嵌套可读性差 |
| **适用场景** | 复杂动态 SQL、批量操作 | 简单至中等复杂度 |
| **维护成本** | SQL 集中管理，便于修改 | Provider 类需单独维护 |
| **灵活性** | ✅ 标签丰富 | 受注解语法限制 |

> **建议**：复杂动态 SQL → XML；简单场景 → 注解。

---

## 5. 注意事项

| 注意点 | 说明 |
|--------|------|
| **OGNL 判空** | 字符串非空：`test="name != null and name != ''"`，不可简写为 `test="name"` |
| **`<where>` / `<set>` 必用** | 避免手动拼 `WHERE`/`SET`，否则多余 `AND/OR` 或逗号 |
| **`foreach` 的 collection** | List 未加 `@Param` → `collection="list"`；数组 → `collection="array"` |
| **SQL 注入** | 字段值用 `#{}`，仅 `$ { }` 用于表名/列名 |
| **多参数必加 `@Param`** | OGNL 无法识别未注解的参数名 |
| **特殊字符转义** | XML 中 `<` → `&lt;`，`>` → `&gt;` |
