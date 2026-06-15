# MyBatis 教程

> **文档定位**：Java 后端技术参考文档 | MyBatis 入门教程  
> **核心定义**：MyBatis 是一款 Java 持久层框架，将 SQL 写在 XML 或注解里，与 Java 代码分离  
> **企业地位**：企业级开发最常用的数据库访问框架之一

---

## 目录

- [一、MyBatis 是什么](#一mybatis-是什么)
- [二、Spring Boot 整合 MyBatis](#二spring-boot-整合-mybatis)
- [三、基本使用步骤](#三基本使用步骤)
- [四、MyBatis 核心知识点](#四mybatis-核心知识点)
- [五、企业常用写法](#五企业常用写法)
- [六、MyBatis-Plus 简介](#六mybatis-plus-简介)

---

## 一、MyBatis 是什么

MyBatis 是一款 Java 持久层框架，负责数据库操作。它可以把 SQL 写在 XML 或者注解里，和 Java 代码分开，方便维护。

---

## 二、Spring Boot 整合 MyBatis

### 引入依赖

```xml
<dependency>
    <groupId>org.mybatis.spring.boot</groupId>
    <artifactId>mybatis-spring-boot-starter</artifactId>
    <version>2.3.0</version>
</dependency>
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>
```

### 配置文件（application.yml）

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/数据库名?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: 账号
    password: 密码
    driver-class-name: com.mysql.cj.jdbc.Driver

mybatis:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: 实体类所在包
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

---

## 三、基本使用步骤

### 1. 建表

```sql
CREATE TABLE user (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50),
    password VARCHAR(100),
    nickname VARCHAR(50),
    age      INT
);
```

### 2. 实体类

```java
package com.example.entity;

public class User {
    private Long id;
    private String username;
    private String password;
    private String nickname;
    private Integer age;
    // getter/setter...
}
```

### 3. Mapper 接口

```java
package com.example.mapper;

import com.example.entity.User;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface UserMapper {
    @Select("SELECT * FROM user WHERE id = #{id}")
    User findById(Long id);

    @Insert("INSERT INTO user(username, password, nickname, age) VALUES(#{username}, #{password}, #{nickname}, #{age})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    List<User> findList();       // XML 中定义
    int update(User user);        // XML 中定义
    int delete(Long id);          // XML 中定义
}
```

### 4. Mapper XML

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
    "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.example.mapper.UserMapper">

    <select id="findList" resultType="com.example.entity.User">
        SELECT * FROM user
    </select>

    <update id="update">
        UPDATE user
        <set>
            <if test="username != null">username = #{username},</if>
            <if test="nickname != null">nickname = #{nickname},</if>
            <if test="age != null">age = #{age}</if>
        </set>
        WHERE id = #{id}
    </update>

    <delete id="delete">
        DELETE FROM user WHERE id = #{id}
    </delete>
</mapper>
```

### 5. Service 层

```java
@Service
public class UserService {
    @Resource
    private UserMapper userMapper;

    public User findById(Long id) { return userMapper.findById(id); }
    public int add(User user) { return userMapper.insert(user); }
    public List<User> findList() { return userMapper.findList(); }
    public int update(User user) { return userMapper.update(user); }
    public int delete(Long id) { return userMapper.delete(id); }
}
```

### 6. Controller 层

```java
@RestController
@RequestMapping("/user")
public class UserController {
    @Resource
    private UserService userService;

    @GetMapping("/{id}")
    public User findById(@PathVariable Long id) { return userService.findById(id); }

    @PostMapping
    public String add(@RequestBody User user) { userService.add(user); return "success"; }

    @GetMapping("/list")
    public List<User> list() { return userService.findList(); }

    @PutMapping
    public String update(@RequestBody User user) { userService.update(user); return "success"; }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id) { userService.delete(id); return "success"; }
}
```

---

## 四、MyBatis 核心知识点

### `#{}` 和 `${}` 区别

| 占位符 | 特点 | 使用场景 |
|--------|------|----------|
| `#{}` | 预编译，防 SQL 注入 | 查询参数（推荐） |
| `${}` | 直接拼接字符串，有注入风险 | 动态表名、排序字段 |

### 动态 SQL

| 标签 | 用途 |
|------|------|
| `<if>` | 条件判断 |
| `<choose>` / `<when>` / `<otherwise>` | 多分支判断 |
| `<where>` | 自动处理 AND/OR |
| `<set>` | 自动处理逗号 |
| `<foreach>` | 循环拼接（IN 查询、批量操作） |

### 结果映射

| 方式 | 适用场景 |
|------|----------|
| `resultType` | 简单类型、实体类 |
| `resultMap` | 复杂映射（一对一、一对多） |

### 分页方式

| 方式 | 说明 |
|------|------|
| 手写 LIMIT | `LIMIT #{offset}, #{pageSize}` |
| PageHelper 插件 | 企业常用，一行代码分页 |

---

## 五、企业常用写法

1. Mapper 接口 + XML 分离 SQL
2. 统一返回结果
3. 分页查询
4. 条件构造查询
5. 批量操作
6. 逻辑删除而非物理删除

---

## 六、MyBatis-Plus 简介

MyBatis-Plus 是对 MyBatis 的增强，不改变原有功能，只做增强。

| 特性 | 说明 |
|------|------|
| 通用 CRUD | 无需编写 SQL |
| 条件构造器 | 链式构建查询条件 |
| 分页插件 | 内置分页支持 |
| 逻辑删除 | 内置逻辑删除 |
