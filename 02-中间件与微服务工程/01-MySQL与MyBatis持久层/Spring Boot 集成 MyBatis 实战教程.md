# Spring Boot 集成 MyBatis 实战教程

> **文档定位**：Java 后端技术参考文档 | Spring Boot + MyBatis 集成实战  
> **核心特点**：基于 Spring Boot 自动配置，无需复杂 XML，仅通过依赖、配置和注解完成集成  
> **适用版本**：Spring Boot 2.7.x + MyBatis 3.x

---

## 目录

- [一、核心依赖](#一核心依赖)
- [二、核心配置](#二核心配置)
- [三、代码编写](#三代码编写)
- [四、测试集成效果](#四测试集成效果)
- [五、关键注意事项](#五关键注意事项)
- [六、常见问题排查](#六常见问题排查)

---

## 一、核心依赖

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>2.7.18</version>
</parent>

<dependencies>
    <!-- 核心整合包 -->
    <dependency>
        <groupId>org.mybatis.spring.boot</groupId>
        <artifactId>mybatis-spring-boot-starter</artifactId>
        <version>2.3.2</version>
    </dependency>
    <!-- 数据库驱动 -->
    <dependency>
        <groupId>mysql</groupId>
        <artifactId>mysql-connector-java</artifactId>
        <scope>runtime</scope>
    </dependency>
    <!-- 连接池（推荐 Druid） -->
    <dependency>
        <groupId>com.alibaba</groupId>
        <artifactId>druid-spring-boot-starter</artifactId>
        <version>1.2.20</version>
    </dependency>
</dependencies>
```

---

## 二、核心配置（application.yml）

```yaml
spring:
  datasource:
    type: com.alibaba.druid.pool.DruidDataSource
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/test_db?useSSL=false&serverTimezone=UTC&characterEncoding=utf8
    username: root
    password: 123456
    druid:
      initial-size: 5
      max-active: 20
      min-idle: 5

mybatis:
  mapper-locations: classpath:mapper/*.xml          # Mapper XML 路径
  type-aliases-package: com.example.entity           # 实体类别名包
  configuration:
    map-underscore-to-camel-case: true               # 驼峰命名映射
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl  # SQL 日志（开发环境）

logging:
  level:
    com.example.mapper: debug                        # 只打印 Mapper 日志
```

---

## 三、代码编写

### 实体类（Entity）

```java
package com.example.entity;

public class User {
    private Long id;
    private String userName;  // 对应数据库字段 user_name（驼峰映射）
    private Integer age;
    private String phone;

    public User() {}
    // getter/setter/toString...
}
```

### Mapper 接口

```java
package com.example.mapper;

import com.example.entity.User;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface UserMapper {
    User selectById(@Param("id") Long id);
    List<User> selectAll();
    int insert(User user);
    int update(User user);
    int deleteById(@Param("id") Long id);
}
```

### Mapper XML

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
    "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.example.mapper.UserMapper">

    <insert id="insert">
        INSERT INTO user (user_name, age, phone)
        VALUES (#{userName}, #{age}, #{phone})
    </insert>

    <select id="selectById" resultType="User">
        SELECT * FROM user WHERE id = #{id}
    </select>

    <select id="selectAll" resultType="User">
        SELECT * FROM user
    </select>

    <update id="update">
        UPDATE user SET user_name = #{userName}, age = #{age}, phone = #{phone}
        WHERE id = #{id}
    </update>

    <delete id="deleteById">
        DELETE FROM user WHERE id = #{id}
    </delete>
</mapper>
```

### Service 层

```java
@Service
public class UserService {
    @Autowired
    private UserMapper userMapper;

    public User getUserById(Long id) { return userMapper.selectById(id); }
    public List<User> getAllUsers() { return userMapper.selectAll(); }
    public int addUser(User user) { return userMapper.insert(user); }
    public int updateUser(User user) { return userMapper.update(user); }
    public int deleteUser(Long id) { return userMapper.deleteById(id); }
}
```

### 启动类

```java
@SpringBootApplication
@MapperScan("com.example.mapper")  // 扫描 Mapper 接口（核心！）
public class MyBatisSpringBootApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyBatisSpringBootApplication.class, args);
    }
}
```

---

## 四、测试集成效果

```java
@SpringBootTest
public class MyBatisIntegrationTest {

    @Autowired
    private UserService userService;

    @Test
    public void testSelectById() {
        User user = userService.getUserById(1L);
        System.out.println("查询结果：" + user);
    }

    @Test
    public void testAddUser() {
        User newUser = new User();
        newUser.setUserName("张三");
        newUser.setAge(25);
        newUser.setPhone("13800138000");
        int count = userService.addUser(newUser);
        System.out.println("新增数量：" + count);
    }
}
```

---

## 五、关键注意事项

| 注意点 | 说明 |
|--------|------|
| **namespace 匹配** | XML 的 namespace 必须与 Mapper 全路径一致 |
| **id 匹配** | SQL 标签 id 必须与 Mapper 方法名一致 |
| **数据源驱动** | MySQL 8.0+ 必须用 `com.mysql.cj.jdbc.Driver` |
| **@MapperScan 路径** | 必须包含所有 Mapper 接口 |
| **实体类 getter/setter** | MyBatis 映射结果时需要 |
| **SQL 日志** | 仅开发环境启用，生产关闭 |
| **多参数** | 必须用 `@Param` 标注参数名 |

---

## 六、常见问题排查

| 问题 | 原因 | 解决 |
|------|------|------|
| **Invalid bound statement (not found)** | namespace/id 不匹配或路径错误 | 检查 namespace、标签 id、mapper-locations |
| **No qualifying bean** | Mapper 未被扫描 | 添加 `@MapperScan` 或 `@Mapper` |
| **Communications link failure** | 数据库连接失败 | 检查 MySQL 服务、URL、驱动类 |

---

## 核心流程总结

```
引入依赖 → 配置 application.yml → 编写 Entity/Mapper/Service → 测试
```

> 核心关键点：Mapper 接口与 XML 关联正确 → 数据源配置无误 → Mapper 被 Spring 扫描到。
