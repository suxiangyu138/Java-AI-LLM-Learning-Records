# 第4章 MyBatis 动态 SQL
## 4.1 学习目标
1. 掌握动态 SQL 核心标签：`<if>`、`<where>`、`<trim>`、`<choose>`、`<foreach>`
2. 解决**多条件组合查询**、**非空判断**、**批量操作**场景
3. 避免 SQL 语法错误（多余 `and/or`、多余逗号）
4. 理解动态 SQL 适用场景，仅**XML 方式**支持完整动态SQL

> 前置：沿用前序项目环境，使用 XML 映射文件开发

---

## 4.2 动态 SQL 概述
### 4.2.1 什么是动态 SQL
根据**传入参数是否为空、是否有值**，**动态拼接、生成 SQL 语句**，适配多变查询条件。

### 4.2.2 应用场景
- 后台多条件筛选（姓名、年龄、性别模糊查询）
- 选择性字段更新（只更新传入不为空的字段）
- 批量新增、批量删除、批量查询

### 4.2.3 常用核心标签
- `<if>`：条件判断
- `<where>`：动态拼接 where 子句，自动去除多余 and/or
- `<choose>/<when>/<otherwise>`：多条件单选（类似 switch）
- `<set>`：动态更新，自动去除尾部多余逗号
- `<foreach>`：遍历集合，实现批量操作
- `<trim>`：自定义前缀、后缀、移除指定字符

---

## 4.3 \<if\> 标签：条件判断
### 场景
多条件查询，**参数不为空、不为null** 才拼接条件。

### Mapper 接口
```java
List<User> findUserByCondition(User user);
```

### XML 映射
```xml
<select id="findUserByCondition" resultType="com.pojo.User">
    SELECT * FROM user
    WHERE 1=1
    <if test="username != null and username != ''">
        AND username = #{username}
    </if>
    <if test="age != null">
        AND age = #{age}
    </if>
</select>
```
- `test`：判断条件，支持 OGNL 表达式
- `1=1` 固定占位，防止所有 if 不生效时 SQL 语法报错

---

## 4.4 \<where\> 标签（推荐替代 1=1）
自动识别：
1. 自动添加 `where` 关键字
2. 自动去除**第一个**多余的 `and / or`

```xml
<select id="findUserByCondition" resultType="com.pojo.User">
    SELECT * FROM user
    <where>
        <if test="username != null and username != ''">
            AND username = #{username}
        </if>
        <if test="age != null">
            AND age = #{age}
        </if>
    </where>
</select>
```

---

## 4.5 \<set\> 标签：动态更新
### 场景
部分字段更新，只修改前端传入有值的字段，自动去除尾部多余逗号。

```xml
<update id="updateUserSelective">
    UPDATE user
    <set>
        <if test="username != null and username != ''">
            username = #{username},
        </if>
        <if test="password != null and password != ''">
            password = #{password},
        </if>
        <if test="age != null">
            age = #{age}
        </if>
    </set>
    WHERE id = #{id}
</update>
```

---

## 4.6 \<choose\> / \<when\> / \<otherwise\>
**单选分支**：只会拼接第一个满足条件的语句，类似 `switch-case`。

需求：优先按姓名查，姓名为空按年龄查，都为空查全部。
```xml
<select id="findUserChoose" resultType="com.pojo.User">
    SELECT * FROM user
    <where>
        <choose>
            <when test="username != null and username != ''">
                username = #{username}
            </when>
            <when test="age != null">
                age = #{age}
            </when>
            <otherwise>
                1=1
            </otherwise>
        </choose>
    </where>
</select>
```

---

## 4.7 \<trim\> 自定义字符串截取
手动定制前缀、后缀、去除前后多余字符，可替代 `where` / `set`。

### 替代 where
```xml
<trim prefix="where" prefixOverrides="and|or">
    <if test="username != null">
        AND username = #{username}
    </if>
</trim>
```

### 替代 set
```xml
<trim prefix="set" suffixOverrides=",">
    <if test="username != null">
        username = #{username},
    </if>
</trim>
```

---

## 4.8 \<foreach\> 标签：批量操作
核心属性：
- `collection`：遍历的集合类型（List、Array、集合）
- `item`：遍历中单个元素别名
- `open`：起始拼接符号
- `close`：结束拼接符号
- `separator`：元素之间分隔符

### 4.8.1 批量查询（in 语句）
接口方法：
```java
List<User> findUserByIds(@Param("idList") List<Integer> idList);
```

XML：
```xml
<select id="findUserByIds" resultType="com.pojo.User">
    SELECT * FROM user
    WHERE id IN
    <foreach collection="idList" item="id" open="(" close=")" separator=",">
        #{id}
    </foreach>
</select>
```

### 4.8.2 批量删除
```xml
<delete id="deleteBatch">
    DELETE FROM user
    WHERE id IN
    <foreach collection="idList" item="id" open="(" close=")" separator=",">
        #{id}
    </foreach>
</delete>
```

### 4.8.3 批量新增
```xml
<insert id="insertBatch">
    INSERT INTO user(username,age)
    VALUES
    <foreach collection="userList" item="user" separator=",">
        (#{user.username},#{user.age})
    </foreach>
</insert>
```

---

## 4.9 SQL 片段抽取 \<sql\>
抽取重复 SQL 片段，复用、减少冗余代码。

### 定义片段
```xml
<sql id="userColumn">
    id,username,password,age
</sql>
```

### 引入片段
```xml
<select id="findAll" resultType="com.pojo.User">
    SELECT <include refid="userColumn"/> FROM user
</select>
```

---

## 4.10 本章易错点总结
1. `test` 表达式中，**字符串非空判断**必须写 `!= ''`
2. `<where>` 只能自动去掉**开头**的 and/or
3. `<set>` 自动去掉**结尾**逗号
4. `foreach` 的 `collection`：
    - 单个 List：填 list
    - 加 @Param 别名：填自定义别名
5. 注解开发**不支持动态SQL标签**，复杂动态SQL必须用 XML

---

## 4.11 本章核心总结
1. 动态SQL核心作用：**条件动态拼接，适配多场景查询更新**
2. 常用组合：
    - 多条件查询：`<if>` + `<where>`
    - 动态修改：`<set>` + `<if>`
    - 单选条件：`<choose>`
    - 批量操作：`<foreach>`
3. 代码优化：使用 `<sql>` 抽取公共字段，提高复用性

---
