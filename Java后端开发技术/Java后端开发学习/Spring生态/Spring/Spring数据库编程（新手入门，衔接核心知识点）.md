# Spring 数据库编程（新手入门，衔接核心知识点）

> **文档定位**：Java 后端企业级技术文档 | Spring 数据库编程  
> **核心工具**：JdbcTemplate + HikariCP  
> **前置基础**：Spring IOC、Bean 装配、AOP

---

## 一、核心概念

### 1.1 Spring 数据库编程的定位

Spring 数据库编程通过 **JdbcTemplate** 和注解简化传统 JDBC 的繁琐操作（加载驱动、创建连接、处理结果集、关闭资源），同时基于 AOP 实现 **声明式事务控制**。

> 数据库操作相关的 Bean（DataSource、JdbcTemplate）全部交给 Spring 容器管理，完全遵循 IOC 思想。

---

## 二、底层原理

### 2.1 JdbcTemplate 工作流程

```
Spring 容器管理 DataSource → JdbcTemplate 从 DataSource 获取连接
    → 执行 SQL（#{} 参数绑定）→ ResultSet → RowMapper 映射为对象 → 归还连接
```

---

## 三、代码实现

### 3.1 环境准备

```sql
CREATE DATABASE IF NOT EXISTS spring_demo CHARACTER SET utf8mb4;
USE spring_demo;
CREATE TABLE IF NOT EXISTS user (
    id     INT PRIMARY KEY AUTO_INCREMENT,
    name   VARCHAR(50) NOT NULL,
    age    INT NOT NULL,
    gender VARCHAR(10) NOT NULL
);
```

### 3.2 Maven 依赖

```xml
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-jdbc</artifactId>
    <version>5.3.28</version>
</dependency>
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <version>8.0.36</version>
</dependency>
<dependency>
    <groupId>com.zaxxer</groupId>
    <artifactId>HikariCP</artifactId>
    <version>4.0.3</version>
</dependency>
```

### 3.3 XML 配置数据源 + JdbcTemplate

```xml
<beans>
    <context:component-scan base-package="com.example"/>

    <!-- HikariCP 数据源 -->
    <bean id="dataSource" class="com.zaxxer.hikari.HikariDataSource">
        <property name="driverClassName" value="com.mysql.cj.jdbc.Driver"/>
        <property name="jdbcUrl"
                  value="jdbc:mysql://localhost:3306/spring_demo?useSSL=false&serverTimezone=UTC"/>
        <property name="username" value="root"/>
        <property name="password" value="your_password"/>
    </bean>

    <!-- JdbcTemplate -->
    <bean id="jdbcTemplate" class="org.springframework.jdbc.core.JdbcTemplate">
        <property name="dataSource" ref="dataSource"/>
    </bean>
</beans>
```

### 3.4 UserDao CRUD 实现

```java
@Repository
public class UserDao {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /** 新增用户 */
    public int addUser(User user) {
        String sql = "INSERT INTO user (name, age, gender) VALUES (?, ?, ?)";
        return jdbcTemplate.update(sql, user.getName(), user.getAge(), user.getGender());
    }

    /** 根据 ID 查询 */
    public User findById(int id) {
        String sql = "SELECT * FROM user WHERE id = ?";
        return jdbcTemplate.queryForObject(sql, new BeanPropertyRowMapper<>(User.class), id);
    }

    /** 查询所有 */
    public List<User> findAll() {
        String sql = "SELECT * FROM user";
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(User.class));
    }

    /** 更新用户 */
    public int updateUser(User user) {
        String sql = "UPDATE user SET name=?, age=?, gender=? WHERE id=?";
        return jdbcTemplate.update(sql, user.getName(), user.getAge(), user.getGender(), user.getId());
    }

    /** 删除用户 */
    public int deleteUser(int id) {
        String sql = "DELETE FROM user WHERE id = ?";
        return jdbcTemplate.update(sql, id);
    }
}
```

---

## 四、实战要点

| 要点 | 说明 |
|------|------|
| **JdbcTemplate 线程安全** | 单例注入，无需每次 new |
| **RowMapper** | `BeanPropertyRowMapper` 自动映射（字段名需一致） |
| **连接池** | HikariCP 默认推荐，轻量高效 |
| **事务控制** | `@Transactional` 注解 + AOP 切面 |

---

## 五、避坑总结

| 坑点 | 解决 |
|------|------|
| **驱动类错误** | MySQL 8.x 必须写 `com.mysql.cj.jdbc.Driver` |
| **字段名不匹配** | 数据库下划线 ↔ Java 驼峰需保持一致或用自定义 RowMapper |
| **忘记配置 HikariCP** | 需显式引入 HikariCP 依赖 |

---

## 六、企业级最佳实践

- **生产环境用 HikariCP**（Spring Boot 默认，性能最优）
- **JdbcTemplate 适合简单查询**，复杂查询/多表映射推荐 MyBatis
- **事务用 `@Transactional`** 声明式，最小范围原则
