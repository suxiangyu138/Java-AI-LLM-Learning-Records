# MyBatis 动态 SQL 详解（XML + 注解方式）

> **文档定位**：Java 后端企业级技术文档 | MyBatis 动态 SQL  
> **核心特性**：根据参数条件动态拼接 SQL，避免手动拼接  
> **前置基础**：MyBatis 基础用法、Mapper 接口、XML 映射文件

---

## 一、核心概念

### 1.1 什么是动态 SQL

动态 SQL 是 MyBatis 的核心特性之一，允许在 SQL 语句中根据传入的参数条件，**动态拼接、调整 SQL 的结构**（条件判断、循环拼接、分支选择等），避免手动拼接 SQL 带来的繁琐操作和 SQL 注入风险。

> 例如：多条件查询时，用户可能输入用户名、年龄、邮箱的任意组合，动态 SQL 自动判断哪些条件不为空，只拼接有效条件。

### 1.2 核心作用

| 作用 | 说明 |
|------|------|
| **简化多条件查询** | 无需手动判断参数是否为空 |
| **避免 SQL 注入** | 动态标签自动对参数转义（结合 `#{}`） |
| **提升 SQL 复用性** | 通过 `<sql>` 标签抽取公共片段 |
| **适配灵活业务** | 支持动态新增（只插非空字段）、动态修改（只更新非空字段） |

### 1.3 六大核心动态标签

| 标签 | 作用 | 适用场景 |
|------|------|----------|
| `<if>` | 条件判断，满足则拼接 | 多条件查询、动态新增/修改 |
| `<where>` | 自动处理 WHERE + 剔除多余 AND/OR | 替代 `WHERE 1=1` |
| `<set>` | 自动处理 SET + 剔除多余逗号 | 动态修改（只更新非空字段） |
| `<foreach>` | 循环遍历集合/数组 | 批量查询、批量新增/删除（IN 子句） |
| `<sql>` / `<include>` | 抽取/引用公共 SQL 片段 | 重复字段列表、公用条件 |
| `<choose>/<when>/<otherwise>` | 分支选择（类似 switch-case） | 多条件互斥查询 |

---

## 二、底层原理

### 2.1 动态 SQL 解析机制

1. MyBatis 解析 XML 映射文件，将动态标签构建为 **SqlNode 树**（OGNL 表达式节点）
2. 运行时根据传入参数计算 OGNL 表达式（`test` 属性），决定是否拼接对应 SQL 片段
3. `<where>` / `<set>` / `<trim>` 等标签在拼接后自动修正语法（去除多余 AND/OR/逗号）
4. 最终生成完整的 SQL → 参数绑定（`#{}` 预编译）→ 执行

### 2.2 `#{}` vs `${}` 安全对比

| 方式 | 安全性 | 说明 |
|------|--------|------|
| `#{}` | ✅ 安全 | 预编译占位符，自动转义防注入 |
| `${}` | ❌ 危险 | 直接拼接字符串，存在 SQL 注入风险 |

---

## 三、代码实现

### 3.1 `<if>` + `<where>` 标签（多条件查询）

```xml
<!-- UserMapper.xml -->
<mapper namespace="com.example.mapper.UserMapper">

    <!-- 多条件查询用户：<where> + <if> -->
    <select id="findUserByCondition" resultType="com.example.pojo.User">
        SELECT * FROM user
        <where>
            <if test="username != null and username != ''">
                AND username LIKE CONCAT('%', #{username}, '%')
            </if>
            <if test="age != null">
                AND age = #{age}
            </if>
            <if test="email != null and email != ''">
                AND email = #{email}
            </if>
        </where>
    </select>

</mapper>
```

```java
/** Mapper 接口 */
public interface UserMapper {
    List<User> findUserByCondition(User user);
}

/** 测试方法 */
@Test
public void testFindUserByCondition() {
    User condition = new User();
    condition.setUsername("zhangsan"); // 只输入用户名
    List<User> users = userMapper.findUserByCondition(condition);
}
```

> `<where>` 自动处理：有满足条件时自动添加 WHERE 并剔除前缀 AND/OR；全部为空时 WHERE 不生成。

### 3.2 `<set>` 标签（动态修改）

```xml
<!-- 动态修改用户：只更新传入的非空字段 -->
<update id="updateUserDynamic">
    UPDATE user
    <set>
        <if test="username != null and username != ''">
            username = #{username},
        </if>
        <if test="password != null and password != ''">
            password = #{password},
        </if>
        <if test="age != null">
            age = #{age},
        </if>
        <if test="email != null and email != ''">
            email = #{email}
        </if>
    </set>
    WHERE id = #{id}
</update>
```

> `<set>` 自动剔除尾部多余的逗号；必须有 WHERE 条件避免批量误更新。

### 3.3 `<foreach>` 标签（批量操作）

```xml
<!-- 批量查询：IN 子句 -->
<select id="findByIds" resultType="User">
    SELECT * FROM user
    WHERE id IN
    <foreach collection="idList" item="id" open="(" separator="," close=")">
        #{id}
    </foreach>
</select>

<!-- 批量新增 -->
<insert id="batchInsert">
    INSERT INTO user (username, password, age) VALUES
    <foreach collection="userList" item="user" separator=",">
        (#{user.username}, #{user.password}, #{user.age})
    </foreach>
</insert>
```

```java
/** 对应接口 */
public interface UserMapper {
    List<User> findByIds(@Param("idList") List<Integer> idList);
    int batchInsert(@Param("userList") List<User> userList);
}
```

| `foreach` 属性 | 说明 |
|----------------|------|
| `collection` | 集合参数名 |
| `item` | 每次迭代的元素别名 |
| `open` | 前缀字符串 |
| `separator` | 分隔符 |
| `close` | 后缀字符串 |

### 3.4 `<sql>` + `<include>` 标签（SQL 片段复用）

```xml
<!-- 抽取公共字段列表 -->
<sql id="userColumns">
    id, username, password, age, email, create_time
</sql>

<!-- 引用公共字段 -->
<select id="findAllUsers" resultType="User">
    SELECT <include refid="userColumns"/> FROM user
</select>
```

### 3.5 `<choose>/<when>/<otherwise>` 标签（分支选择）

```xml
<!-- 多条件互斥查询：用户只输入一个条件时用 -->
<select id="findUserBySingleCondition" resultType="User">
    SELECT * FROM user WHERE 1=1
    <choose>
        <when test="username != null and username != ''">
            AND username = #{username}
        </when>
        <when test="age != null">
            AND age = #{age}
        </when>
        <otherwise>
            AND id = 1
        </otherwise>
    </choose>
</select>
```

---

## 四、实战要点

### 4.1 标签选择速查

| 需求 | 推荐标签 |
|------|----------|
| 多条件查询（条件可组合） | `<where>` + `<if>` |
| 多条件互斥查询 | `<choose>/<when>/<otherwise>` |
| 动态更新非空字段 | `<set>` + `<if>` |
| 批量操作（IN 子句/批量插入） | `<foreach>` |
| 重复 SQL 片段 | `<sql>` + `<include>` |
| 去掉 WHERE 1=1 | 用 `<where>` 替代 |

### 4.2 注意事项

- `<where>` 标签仅剔除 **前缀** AND/OR，`a = 1 AND b = 2` 中间的不剔除
- `<set>` 标签仅剔除 **尾部** 多余逗号
- `<foreach>` 的 `collection` 如果是 `List`，默认参数名是 `list`；建议用 `@Param` 指定

---

## 五、避坑总结

| 坑点 | 原因 | 解决方案 |
|------|------|----------|
| **WHERE 1=1 不规范** | 所有 `<if>` 不满足时语法错误 | 用 `<where>` 标签替代 |
| **动态修改忘记 WHERE** | `<set>` 没有 WHERE 条件 | 必须加 `WHERE id = #{id}` |
| **OGNL 表达式错误** | `test="username != ''"` | 应写 `test="username != null and username != ''"` |
| **逗号残留** | `<set>` 中逗号放错位置 | 逗号统一放在每条语句末尾 |
| **`${}` SQL 注入** | 用 `${}` 拼接用户输入 | 必须用 `#{}` 预编译参数 |

---

## 六、企业级最佳实践

### 6.1 核心原则

| 原则 | 说明 |
|------|------|
| **动态标签 + `#{}`** | 所有用户输入都通过 `#{}` 预编译，严禁 `${}` 拼接 |
| **`<sql>` 抽取复用** | 超过 2 处使用的字段列表或条件片段，抽取到 `<sql>` |
| **`@Param` 明确参数名** | 多参数场景统一用 `@Param` 指定名称 |
| **批量操作限制** | 单次 `foreach` 不超过 1000 条，大数据量分批处理 |

### 6.2 代码审查 Checklist

- [ ] 所有用户输入使用 `#{}` 而非 `${}`
- [ ] `<set>` 有对应的 WHERE 条件
- [ ] `<foreach>` 的 `collection` 参数名与 `@Param` 一致
- [ ] `<if test>` 同时判断 `null` 和空字符串
- [ ] 公共 SQL 片段已抽取到 `<sql>`
