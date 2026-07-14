# MyBatis 注解方式的基本用法

> **文档定位**：Java 后端技术参考文档 | MyBatis 注解方式  
> **核心优势**：无需编写 Mapper XML，直接在接口方法上通过注解编写 SQL，简化配置  
> **适用场景**：SQL 逻辑简单、CRUD 操作单一的场景  
> **混合使用**：注解与 XML 可共存，简单 SQL 用注解，复杂 SQL 用 XML

---

## 目录

- [一、前期准备](#一前期准备)
- [二、MyBatis 核心注解](#二mybatis-核心注解)
- [三、注解方式 CRUD 实操](#三注解方式-crud-实操)
- [四、进阶：字段映射不匹配解决](#四进阶字段映射不匹配解决)
- [五、注意事项](#五注意事项)
- [六、注解 vs XML 对比](#六注解-vs-xml-对比)

---

## 一、前期准备

### 核心配置文件关键修改

```xml
<mappers>
    <!-- 注解方式核心：扫描 Mapper 接口包，无需加载 XML -->
    <package name="com.example.mapper"/>
</mappers>
```

> 数据库、实体类、工具类与 XML 方式完全一致，无需修改。

---

## 二、MyBatis 核心注解

| 注解 | 作用 | 对应 XML |
|------|------|----------|
| **`@Select`** | 执行查询 SQL | `<select>` |
| **`@Insert`** | 执行插入 SQL | `<insert>` |
| **`@Update`** | 执行更新 SQL | `<update>` |
| **`@Delete`** | 执行删除 SQL | `<delete>` |
| **`@Param`** | 多参数命名 | 同 XML 的 `@Param` |
| **`@Options`** | 配置额外参数（如自增主键） | `useGeneratedKeys` |
| **`@Results`** | 配置结果映射 | `<resultMap>` |
| **`@ResultMap`** | 引用已有结果映射 | 复用 |

---

## 三、注解方式 CRUD 实操

### 完整 Mapper 接口

```java
package com.example.mapper;

import com.example.pojo.User;
import org.apache.ibatis.annotations.*;
import java.util.List;

public interface UserMapper {

    // 1. 查询（@Select）
    @Select("SELECT * FROM user WHERE id = #{id}")
    User selectUserById(@Param("id") Integer id);

    @Select("SELECT * FROM user")
    List<User> selectAllUser();

    // 2. 新增（@Insert + @Options 获取自增主键）
    @Insert("INSERT INTO user (username, password, age, email) VALUES (#{username}, #{password}, #{age}, #{email})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertUser(User user);

    // 3. 修改（@Update）
    @Update("UPDATE user SET username = #{username}, password = #{password}, age = #{age}, email = #{email} WHERE id = #{id}")
    int updateUser(User user);

    // 4. 删除（@Delete）
    @Delete("DELETE FROM user WHERE id = #{id}")
    int deleteUserById(@Param("id") Integer id);
}
```

### 测试示例

```java
@Test
public void testInsertUser() {
    try (SqlSession sqlSession = MyBatisUtils.getSqlSession()) {
        UserMapper userMapper = sqlSession.getMapper(UserMapper.class);
        User user = new User();
        user.setUsername("zhaoliu");
        user.setPassword("888888");
        int rows = userMapper.insertUser(user);
        System.out.println("新增成功，自增id：" + user.getId());  // @Options 生效
    }
}
```

---

## 四、进阶：字段映射不匹配解决

### 使用 @Results + @Result

```java
// 假设实体类属性 userAge 对应数据库字段 age
@Select("SELECT id, username, password, age, email FROM user WHERE id = #{id}")
@Results(id = "userResultMap", value = {
    @Result(column = "id", property = "id", id = true),
    @Result(column = "username", property = "username"),
    @Result(column = "age", property = "userAge"),  // 字段名 → 属性名
    @Result(column = "email", property = "userEmail")
})
User selectUserById(@Param("id") Integer id);

// 复用映射
@Select("SELECT * FROM user")
@ResultMap("userResultMap")
List<User> selectAllUser();
```

---

## 五、注意事项

| 注意点 | 说明 |
|--------|------|
| **简单优先** | 注解仅适合简单 SQL，复杂动态 SQL 推荐 XML |
| **扫描配置** | `<mappers>` 必须扫描 Mapper 接口包 |
| **特殊字符** | `>` `<` `&` 需转义或双引号包裹 |
| **多参数** | 必须用 `@Param` 命名参数 |
| **自增主键** | 必须加 `@Options(useGeneratedKeys = true, keyProperty = "id")` |
| **混合使用** | 注解与 XML 可共存，简单用注解、复杂用 XML |
| **拼写错误** | 注解无 XML 解析校验，拼写错误运行时报错 |

---

## 六、注解 vs XML 对比

| 对比维度 | 注解方式 | XML 方式 |
|----------|----------|----------|
| **配置复杂度** | 简单 | 稍复杂 |
| **SQL 可读性** | 简单 SQL 清晰，复杂 SQL 杂乱 | 集中管理，复杂 SQL 可读性强 |
| **适用场景** | 基础 CRUD、简单 SQL | 复杂 SQL、动态 SQL、多表关联 |
| **维护成本** | 简单 SQL 方便，复杂 SQL 困难 | 集中维护，便于优化 |
