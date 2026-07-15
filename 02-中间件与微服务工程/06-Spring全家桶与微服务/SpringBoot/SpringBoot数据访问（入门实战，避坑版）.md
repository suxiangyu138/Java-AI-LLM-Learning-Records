# SpringBoot 数据访问（入门实战，避坑版）

> **定位**：JDBC（JdbcTemplate）+ MyBatis（注解/XML）两种方式。核心：依赖 + 配置 + Mapper + Service。

---

## 目录

1. [JDBC 整合](#1-jdbc-整合)
2. [MyBatis 整合](#2-mybatis-整合)
3. [避坑指南](#3-避坑指南)

---

## 1. JDBC 整合

### 依赖

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-jdbc</artifactId>
</dependency>
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <version>8.0.30</version>
    <scope>runtime</scope>
</dependency>
```

### 配置

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/springboot_db?serverTimezone=Asia/Shanghai&useSSL=false
spring.datasource.username=root
spring.datasource.password=123456
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
```

### JdbcTemplate CRUD

```java
@Service
public class UserJdbcService {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    public int addUser(User user) {
        return jdbcTemplate.update("INSERT INTO user(name, age) VALUES(?, ?)",
                user.getName(), user.getAge());
    }

    public User getUserById(Integer id) {
        return jdbcTemplate.queryForObject("SELECT * FROM user WHERE id=?",
                new BeanPropertyRowMapper<>(User.class), id);
    }

    public List<User> getAllUser() {
        return jdbcTemplate.query("SELECT * FROM user",
                new BeanPropertyRowMapper<>(User.class));
    }
}
```

> ⚠️ `BeanPropertyRowMapper` 要求字段名一致。`update()` 返回影响行数。

---

## 2. MyBatis 整合

### 依赖

```xml
<dependency>
    <groupId>org.mybatis.spring.boot</groupId>
    <artifactId>mybatis-spring-boot-starter</artifactId>
    <version>2.2.2</version>
</dependency>
```

### 配置

```properties
mybatis.mapper-locations=classpath:mapper/*.xml
mybatis.type-aliases-package=com.example.entity
mybatis.configuration.map-underscore-to-camel-case=true
```

### 注解方式

```java
@Mapper
public interface UserMapper {
    @Select("SELECT * FROM user WHERE id=#{id}")
    User getUserById(Integer id);

    @Insert("INSERT INTO user(name, age) VALUES(#{name}, #{age})")
    int addUser(User user);

    @Update("UPDATE user SET name=#{name} WHERE id=#{id}")
    int updateUser(User user);

    @Delete("DELETE FROM user WHERE id=#{id}")
    int deleteUser(Integer id);
}
```

### XML 方式

```xml
<mapper namespace="com.example.mapper.UserXmlMapper">
    <select id="getUserById" resultType="User">
        SELECT * FROM user WHERE id=#{id}
    </select>
</mapper>
```

> ⚠️ `namespace` = 接口全类名，`id` = 方法名，`resultType` 配置别名后可直接写类名。

---

## 3. 避坑指南

| 问题 | 解决 |
|------|------|
| 连接失败 `Access denied` | 检查用户名/密码/端口/防火墙 |
| 查询字段为 null | 开启驼峰映射 + 字段名一致 |
| `Could not find resource mapper/xxx.xml` | 检查 `mapper-locations` + 文件在 `resources/mapper/` |
| SQL 注入 | 用 `#{ }`，禁用 `${ }` |
| 时区异常 | URL 加 `serverTimezone=Asia/Shanghai` |
