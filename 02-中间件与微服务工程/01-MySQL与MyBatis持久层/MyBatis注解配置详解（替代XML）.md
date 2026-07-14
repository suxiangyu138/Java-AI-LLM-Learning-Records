# MyBatis 注解配置详解（替代 XML）

> **文档定位**：Java 后端技术参考文档 | MyBatis 注解配置全面指南  
> **核心作用**：通过 Java 注解直接在 Mapper 接口上编写 SQL 和映射规则，替代 XML 映射文件  
> **适用场景**：SQL 逻辑简单、快速开发场景；复杂 SQL 仍推荐 XML

---

## 目录

- [一、注解配置概述](#一注解配置概述)
- [二、基础 CRUD 注解](#二基础-crud-注解)
- [三、映射配置注解（字段映射 + 关联映射）](#三映射配置注解字段映射--关联映射)
- [四、动态 SQL 注解（Provider）](#四动态-sql-注解provider)
- [五、注解 vs XML 对比与选型](#五注解-vs-xml-对比与选型)

---

## 一、注解配置概述

### 优缺点

| 优点 | 缺点 |
|------|------|
| 简洁高效，无需创建 XML 文件 | 复杂 SQL 可读性差 |
| 配置简单，只需扫描 Mapper 包 | 高级特性实现繁琐 |
| 轻量灵活，适合简单 SQL | 调试难度比 XML 高 |

### 核心注解分类

| 类别 | 注解 | 作用 |
|------|------|------|
| **基础 CRUD** | `@Select`、`@Insert`、`@Update`、`@Delete` | 替代 XML 四标签 |
| **映射配置** | `@Results`、`@Result`、`@One`、`@Many` | 替代 `<resultMap>` 和关联映射 |
| **动态 SQL** | `@SelectProvider` 等 Provider 注解 | 替代 XML 动态标签 |

### 前置配置

```xml
<mappers>
    <package name="com.example.mapper"/>  <!-- 扫描 Mapper 接口 -->
</mappers>
```

---

## 二、基础 CRUD 注解

### @Select（查询）

```java
public interface UserMapper {
    @Select("SELECT * FROM user WHERE id = #{id}")
    User findById(@Param("id") Integer id);

    @Select("SELECT * FROM user")
    List<User> findAll();

    @Select("SELECT * FROM user WHERE username LIKE CONCAT('%', #{username}, '%')")
    List<User> findByUsername(@Param("username") String username);
}
```

### @Insert（新增 + 自增主键）

```java
@Insert("INSERT INTO user (username, password, age, email) VALUES (#{username}, #{password}, #{age}, #{email})")
@Options(useGeneratedKeys = true, keyProperty = "id")
int addUser(User user);
```

### @Update（修改）

```java
@Update("UPDATE user SET username = #{username}, password = #{password}, age = #{age}, email = #{email} WHERE id = #{id}")
int updateUser(User user);
```

### @Delete（删除）

```java
@Delete("DELETE FROM user WHERE id = #{id}")
int deleteUser(@Param("id") Integer id);
```

---

## 三、映射配置注解（字段映射 + 关联映射）

### @Results + @Result（字段映射）

```java
// 解决实体类属性与数据库字段名不一致
@Results(id = "userResultMap", value = {
    @Result(column = "id", property = "id", id = true),
    @Result(column = "username", property = "userName"),  // 字段名 → 属性名
    @Result(column = "password", property = "passWord"),
    @Result(column = "age", property = "age"),
    @Result(column = "email", property = "email")
})
@Select("SELECT * FROM user WHERE id = #{id}")
User findById(@Param("id") Integer id);

// 复用映射
@ResultMap("userResultMap")
@Select("SELECT * FROM user")
List<User> findAll();
```

### @One（一对一关联）

```java
@Results(id = "userWithDetailMap", value = {
    @Result(column = "id", property = "id", id = true),
    @Result(column = "username", property = "userName"),
    @Result(column = "id", property = "userDetail",
        one = @One(
            select = "com.example.mapper.UserDetailMapper.findByUserId",
            fetchType = FetchType.EAGER  // EAGER: 立即加载 / LAZY: 延迟加载
        ))
})
@Select("SELECT * FROM user WHERE id = #{id}")
User findUserWithDetail(@Param("id") Integer id);
```

### @Many（一对多关联）

```java
@Results(id = "deptWithEmpMap", value = {
    @Result(column = "id", property = "id", id = true),
    @Result(column = "dept_name", property = "deptName"),
    @Result(column = "id", property = "empList",
        many = @Many(
            select = "com.example.mapper.EmpMapper.findByDeptId",
            fetchType = FetchType.LAZY
        ))
})
@Select("SELECT * FROM dept WHERE id = #{id}")
Dept findDeptWithEmp(@Param("id") Integer id);
```

---

## 四、动态 SQL 注解（Provider）

### Provider 类

```java
package com.example.provider;

import com.example.pojo.User;
import org.apache.ibatis.jdbc.SQL;

public class UserSqlProvider {
    public String findUserByCondition(User user) {
        return new SQL() {{
            SELECT("*");
            FROM("user");
            if (user.getUserName() != null && !user.getUserName().equals("")) {
                WHERE("username LIKE CONCAT('%', #{userName}, '%')");
            }
            if (user.getAge() != null) {
                WHERE("age = #{age}");
            }
        }}.toString();
    }
}
```

### Mapper 接口引用

```java
public interface UserMapper {
    @SelectProvider(type = UserSqlProvider.class, method = "findUserByCondition")
    List<User> findUserByCondition(User user);
}
```

> 推荐使用 MyBatis 提供的 `SQL` 类拼接动态 SQL，自动处理 WHERE、AND 等关键字。

---

## 五、注解 vs XML 对比与选型

| 维度 | 注解配置 | XML 配置 |
|------|----------|----------|
| 配置复杂度 | 简单 | 稍复杂 |
| SQL 可读性 | 简单清晰，复杂混乱 | 结构清晰，易维护 |
| 高级特性 | 有限 | 全面 |
| 调试难度 | 较高 | 较低 |

### 选型建议

| 场景 | 推荐方式 |
|------|----------|
| 小型项目 / 工具类 | 注解 |
| 中大型项目 | XML |
| 最佳实践 | **混合使用**：简单 CRUD 用注解，复杂 SQL 用 XML |
