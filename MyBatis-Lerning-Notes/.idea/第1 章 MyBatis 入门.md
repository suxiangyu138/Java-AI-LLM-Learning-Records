# 第1章 MyBatis 入门（超详细新手教程）
这一章专门带你**从零开始学会 MyBatis 核心用法**，不用复杂框架，纯原生 MyBatis 快速上手，看完就能独立写项目。

---

## 1.1 什么是 MyBatis？
### 官方定义
MyBatis 是一款**优秀的持久层框架**，它支持**自定义 SQL、存储过程以及高级映射**。

### 简单理解
- 用来**操作数据库**（增删改查）
- 代替原生 JDBC，**简化代码**
- 把 SQL 和 Java 代码分离，方便维护
- 半自动 ORM 框架（灵活、可控）

### 核心优势
1. 手写 SQL，灵活度极高
2. 学习成本低，上手快
3. 性能优秀，轻量级
4. 与 Spring/SpringBoot 无缝整合

---

## 1.2 MyBatis 核心组件
入门必须记住 5 个核心对象：

1. **`SqlSessionFactory`**：会话工厂，创建 SqlSession（全局只用一个）
2. **`SqlSession`**：一次数据库会话，执行 SQL、提交事务
3. **Mapper 接口**：定义操作数据库的方法
4. **Mapper XML**：编写具体 SQL 语句
5. **配置文件**：`mybatis-config.xml`，配置环境、数据源、映射

---

## 1.3 开发环境准备
### 1）创建 Maven 项目
### 2）导入依赖（pom.xml）
```xml
<dependencies>
    <!-- MyBatis 核心依赖 -->
    <dependency>
        <groupId>org.mybatis</groupId>
        <artifactId>mybatis</artifactId>
        <version>3.5.13</version>
    </dependency>

    <!-- MySQL 驱动 -->
    <dependency>
        <groupId>mysql</groupId>
        <artifactId>mysql-connector-java</artifactId>
        <version>8.0.33</version>
    </dependency>

    <!-- 单元测试 -->
    <dependency>
        <groupId>junit</groupId>
        <artifactId>junit</artifactId>
        <version>4.13.2</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

---

## 1.4 快速入门案例（完整步骤）
### 步骤1：创建数据库表
```sql
CREATE TABLE user (
    id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(20),
    password VARCHAR(20),
    age INT
);
```

### 步骤2：编写实体类 User
```java
public class User {
    private Integer id;
    private String username;
    private String password;
    private Integer age;

    // 无参、有参构造
    // getter/setter
    // toString()
}
```

### 步骤3：编写 MyBatis 核心配置文件
**resources/mybatis-config.xml**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE configuration
        PUBLIC "-//mybatis.org//DTD Config 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-config.dtd">

<configuration>
    <!-- 环境配置：默认使用开发环境 -->
    <environments default="development">
        <environment id="development">
            <transactionManager type="JDBC"/>
            <dataSource type="POOLED">
                <property name="driver" value="com.mysql.cj.jdbc.Driver"/>
                <property name="url" value="jdbc:mysql://localhost:3306/mybatis_test?useSSL=false&amp;serverTimezone=UTC"/>
                <property name="username" value="root"/>
                <property name="password" value="你的密码"/>
            </dataSource>
        </environment>
    </environments>

    <!-- 注册 Mapper 文件 -->
    <mappers>
        <mapper resource="UserMapper.xml"/>
    </mappers>
</configuration>
```

### 步骤4：编写 Mapper 接口
```java
public interface UserMapper {
    // 根据 id 查询用户
    User findById(Integer id);
}
```

### 步骤5：编写 Mapper XML（写 SQL）
**resources/UserMapper.xml**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">

<!-- namespace 必须对应接口全类名 -->
<mapper namespace="com.mapper.UserMapper">
    <!-- id 对应接口方法名 -->
    <select id="findById" resultType="com.pojo.User">
        SELECT * FROM user WHERE id = #{id}
    </select>
</mapper>
```

### 步骤6：编写 MyBatis 工具类
```java
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import java.io.IOException;
import java.io.InputStream;

public class MyBatisUtil {
    private static SqlSessionFactory sqlSessionFactory;

    // 静态代码块：项目启动时只加载一次
    static {
        try {
            String resource = "mybatis-config.xml";
            InputStream inputStream = Resources.getResourceAsStream(resource);
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 获取 SqlSession
    public static SqlSession getSqlSession() {
        // true：自动提交事务
        return sqlSessionFactory.openSession(true);
    }
}
```

### 步骤7：编写测试类
```java
import org.apache.ibatis.session.SqlSession;
import org.junit.Test;

public class MyBatisTest {
    @Test
    public void testFindById() {
        // 1. 获取会话
        try (SqlSession session = MyBatisUtil.getSqlSession()) {
            // 2. 获取 Mapper 接口
            UserMapper mapper = session.getMapper(UserMapper.class);
            // 3. 调用方法
            User user = mapper.findById(1);
            // 4. 输出结果
            System.out.println(user);
        }
    }
}
```

运行成功，就说明**MyBatis 入门完成**！

---

## 1.5 核心 API 讲解
### 1. SqlSessionFactory
- 作用：创建 `SqlSession`
- 特点：**一个项目只创建一次**（单例）

### 2. SqlSession
- 一次数据库连接会话
- 用来获取 Mapper、执行 SQL
- 使用完毕必须**关闭**（推荐 try-with-resources）

### 3. Mapper 接口 + Mapper XML
- 接口定义方法
- XML 写 SQL
- **namespace + id 必须一一对应**

---

## 1.6 入门常见错误（必看）
1. **XML 文件没有放在 resources**
2. **namespace 写错**（必须是接口全类名）
3. **id 与方法名不一致**
4. **数据库密码错误**
5. **驱动类名写错**（MySQL8+ 要加 `cj`）
6. **Mapper 没有注册到 mybatis-config.xml**

---

## 1.7 本章总结（必背）
1. MyBatis 是**持久层框架**，简化 JDBC
2. 核心流程：**配置文件 → SqlSessionFactory → SqlSession → Mapper → 执行 SQL**
3. Mapper 接口与 XML **必须绑定**
4. 工具类保证 `SqlSessionFactory` 单例
5. 增删改必须**提交事务**（查询不用）

---

