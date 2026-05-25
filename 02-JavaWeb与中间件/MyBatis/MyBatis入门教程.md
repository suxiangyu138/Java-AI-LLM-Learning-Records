# MyBatis 入门教程（Java 后端企业级实战版）

> **文档定位**：Java 后端企业级技术文档 | MyBatis 入门  
> **MyBatis 版本**：3.5.x  
> **前置基础**：Java 基础、Maven、MySQL 基础操作

---

## 一、核心概念

### 1.1 什么是 MyBatis

MyBatis 是一款基于 Java 的 **半自动化 ORM 持久层框架**，通过 XML 或注解将 SQL 与 Java 对象映射，避免传统 JDBC 的硬编码和繁琐的结果集解析。与 JDBC 相比，MyBatis 无需手动管理连接、处理 ResultSet，同时保留了 SQL 的灵活控制权。

### 1.2 核心特点

| 特点 | 说明 |
|------|------|
| **简单易学** | 体积小，无第三方依赖，简单配置即可使用 |
| **灵活可控** | SQL 独立存放在 XML 文件中，支持动态修改无需重新编译 |
| **解耦性强** | SQL 与 Java 代码分离，提高可维护性和可测试性 |
| **自动化映射** | 通过 ResultMap 自动将 ResultSet 转换为 POJO 对象 |
| **动态 SQL** | 基于 OGNL 表达式，根据参数动态生成 SQL |
| **集成便捷** | 与 Spring/Spring Boot 无缝集成 |

### 1.3 适用场景

- ✅ 需要精细控制 SQL、复杂报表查询、遗留系统改造
- ❌ 超简单 CRUD（用 MyBatis-Plus 更高效）、纯 NoSQL 架构

---

## 二、底层原理

### 2.1 MyBatis 工作流程

```
mybatis-config.xml → SqlSessionFactory → SqlSession → Mapper 代理 → 执行 SQL → ResultMap → POJO
```

### 2.2 核心组件

| 组件 | 作用 |
|------|------|
| `SqlSessionFactory` | 根据配置创建，全局唯一，生产 SqlSession |
| `SqlSession` | 一次数据库会话，执行 SQL、获取 Mapper、控制事务 |
| `Mapper` 接口 | 定义 SQL 方法，MyBatis 动态代理实现 |
| XML 映射文件 | 存放 SQL 语句，与 Mapper 接口绑定 |

---

## 三、代码实现

### 3.1 环境准备

- JDK 1.8+、Maven 3.6+、MySQL 8.0、IDEA

### 3.2 Maven 依赖

```xml
<dependencies>
    <dependency>
        <groupId>org.mybatis</groupId>
        <artifactId>mybatis</artifactId>
        <version>3.5.10</version>
    </dependency>
    <dependency>
        <groupId>mysql</groupId>
        <artifactId>mysql-connector-java</artifactId>
        <version>8.0.30</version>
    </dependency>
    <dependency>
        <groupId>ch.qos.logback</groupId>
        <artifactId>logback-classic</artifactId>
        <version>1.4.5</version>
    </dependency>
    <dependency>
        <groupId>junit</groupId>
        <artifactId>junit</artifactId>
        <version>4.13.2</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### 3.3 数据库准备

```sql
CREATE DATABASE IF NOT EXISTS mybatis_db CHARACTER SET utf8mb4;
USE mybatis_db;

CREATE TABLE IF NOT EXISTS user (
    id       INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(50) NOT NULL,
    age      INT,
    email    VARCHAR(100)
);

INSERT INTO user (username, password, age, email) VALUES
('zhangsan', '123456', 20, 'zhangsan@163.com'),
('lisi',     '654321', 22, 'lisi@163.com');
```

### 3.4 MyBatis 核心配置

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE configuration PUBLIC "-//mybatis.org//DTD Config 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-config.dtd">
<configuration>
    <environments default="development">
        <environment id="development">
            <transactionManager type="JDBC"/>
            <dataSource type="POOLED">
                <property name="driver" value="com.mysql.cj.jdbc.Driver"/>
                <property name="url"
                          value="jdbc:mysql://localhost:3306/mybatis_db?useSSL=false&serverTimezone=UTC"/>
                <property name="username" value="root"/>
                <property name="password" value="your_password"/>
            </dataSource>
        </environment>
    </environments>
    <mappers>
        <mapper resource="mapper/UserMapper.xml"/>
    </mappers>
</configuration>
```

### 3.5 实体类 + Mapper 接口

```java
/** 用户实体类 */
public class User {
    private Integer id;
    private String username;
    private String password;
    private Integer age;
    private String email;
    // getter/setter + toString
}
```

```java
/** Mapper 接口 */
public interface UserMapper {
    User findById(Integer id);
    List<User> findAll();
    int insertUser(User user);
    int updateUser(User user);
    int deleteUser(Integer id);
}
```

### 3.6 XML 映射文件

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.example.mapper.UserMapper">

    <select id="findById" parameterType="int" resultType="com.example.pojo.User">
        SELECT * FROM user WHERE id = #{id}
    </select>

    <select id="findAll" resultType="com.example.pojo.User">
        SELECT * FROM user
    </select>

    <insert id="insertUser" parameterType="com.example.pojo.User">
        INSERT INTO user (username, password, age, email)
        VALUES (#{username}, #{password}, #{age}, #{email})
    </insert>

    <update id="updateUser" parameterType="com.example.pojo.User">
        UPDATE user SET username=#{username}, password=#{password}, age=#{age}, email=#{email}
        WHERE id = #{id}
    </update>

    <delete id="deleteUser" parameterType="int">
        DELETE FROM user WHERE id = #{id}
    </delete>

</mapper>
```

### 3.7 测试

```java
@Test
public void testFindById() throws IOException {
    InputStream is = Resources.getResourceAsStream("mybatis-config.xml");
    SqlSessionFactory factory = new SqlSessionFactoryBuilder().build(is);
    try (SqlSession session = factory.openSession()) {
        UserMapper mapper = session.getMapper(UserMapper.class);
        User user = mapper.findById(1);
        System.out.println(user);
    }
}
```

---

## 四、实战要点

| 要点 | 说明 |
|------|------|
| **全局唯一 SqlSessionFactory** | 应用启动时创建一次，整个生命周期复用 |
| **SqlSession 用完即关** | `try-with-resources` 确保关闭 |
| **`#{}` 而非 `${}`** | 预编译参数绑定，防止 SQL 注入 |
| **namespace** | 必须与 Mapper 接口全限定名一致 |

---

## 五、避坑总结

| 坑点 | 解决 |
|------|------|
| **驱动类错误** | MySQL 8.0+ 用 `com.mysql.cj.jdbc.Driver` |
| **namespace 不匹配** | namespace = Mapper 接口全限定名 |
| **resultType 找不到类** | 写全限定名或配置 typeAliases |
| **`${}` SQL 注入** | 用户输入必须用 `#{}` |

---

## 六、企业级最佳实践

- **接口 + XML 分离**：保持灵活性，复杂 SQL 写在 XML 中
- **`#{}` 统一参数绑定**：杜绝 SQL 注入
- **SqlSession 用 try-with-resources**：确保资源释放
- **后续升级路径**：MyBatis → MyBatis-Plus（单表 CRUD 免写 SQL）→ Spring Boot 集成
