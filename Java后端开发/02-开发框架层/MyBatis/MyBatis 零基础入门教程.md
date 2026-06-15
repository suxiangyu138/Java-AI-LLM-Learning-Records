# MyBatis 零基础入门教程

> **文档定位**：Java 后端技术参考文档 | MyBatis 零基础入门  
> **核心定义**：MyBatis 是一款持久层框架，用于简化 JDBC 操作，通过 XML 或注解将 SQL 与 Java 代码解耦  
> **前置条件**：JDK 8+、IDEA、Maven、MySQL

---

## 目录

- [一、核心概念](#一核心概念)
- [二、环境准备与依赖](#二环境准备与依赖)
- [三、核心配置文件](#三核心配置文件)
- [四、编写实体类、Mapper 接口和映射文件](#四编写实体类mapper-接口和映射文件)
- [五、工具类获取 SqlSession](#五工具类获取-sqlsession)
- [六、编写测试类](#六编写测试类)

---

## 一、核心概念

MyBatis 是一款持久层框架，用于简化 JDBC 操作，通过 XML 或注解方式将 SQL 语句与 Java 代码解耦，支持自定义 SQL、存储过程和高级映射。

---

## 二、环境准备与依赖

### 新建 Maven 项目

IDEA → 文件 → 新建 → 项目 → Maven → 取消勾选模板 → 填写名称（如 `MyBatisDemo`）→ 完成

### pom.xml 依赖

```xml
<dependencies>
    <!-- MyBatis 核心 -->
    <dependency>
        <groupId>org.mybatis</groupId>
        <artifactId>mybatis</artifactId>
        <version>3.5.9</version>
    </dependency>
    <!-- MySQL 驱动 -->
    <dependency>
        <groupId>mysql</groupId>
        <artifactId>mysql-connector-java</artifactId>
        <version>8.0.28</version>
    </dependency>
    <!-- JUnit 测试 -->
    <dependency>
        <groupId>junit</groupId>
        <artifactId>junit</artifactId>
        <version>4.13.2</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

---

## 三、核心配置文件

### 数据库准备

```sql
CREATE DATABASE IF NOT EXISTS mybatis_demo;
USE mybatis_demo;

CREATE TABLE IF NOT EXISTS user (
    id       INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    age      INT NOT NULL
);

INSERT INTO user (username, age) VALUES ('zhangsan', 20), ('lisi', 22);
```

### mybatis-config.xml

在 `src/main/resources` 下创建：

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE configuration
    PUBLIC "-//mybatis.org//DTD Config 3.0//EN"
    "http://mybatis.org/dtd/mybatis-3-config.dtd">
<configuration>
    <environments default="development">
        <environment id="development">
            <transactionManager type="JDBC"/>
            <dataSource type="POOLED">
                <property name="driver" value="com.mysql.cj.jdbc.Driver"/>
                <property name="url" value="jdbc:mysql://localhost:3306/mybatis_demo?useSSL=false&amp;serverTimezone=UTC"/>
                <property name="username" value="root"/>
                <property name="password" value="root"/>
            </dataSource>
        </environment>
    </environments>
    <mappers>
        <mapper resource="com/mapper/UserMapper.xml"/>
    </mappers>
</configuration>
```

---

## 四、编写实体类、Mapper 接口和映射文件

### 实体类（com.pojo.User）

```java
package com.pojo;

public class User {
    private Integer id;
    private String username;
    private Integer age;

    public User() {}
    public User(Integer id, String username, Integer age) {
        this.id = id;
        this.username = username;
        this.age = age;
    }
    // getter/setter/toString...
}
```

### Mapper 接口（com.mapper.UserMapper）

```java
package com.mapper;

import com.pojo.User;
import java.util.List;

public interface UserMapper {
    List<User> selectAll();
}
```

### Mapper 映射文件（UserMapper.xml）

在 `src/main/resources/com/mapper/` 下创建：

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
    PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
    "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.mapper.UserMapper">
    <select id="selectAll" resultType="com.pojo.User">
        SELECT * FROM user
    </select>
</mapper>
```

---

## 五、工具类获取 SqlSession

```java
package com.util;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import java.io.IOException;
import java.io.InputStream;

public class MyBatisUtil {
    private static SqlSessionFactory sqlSessionFactory;

    static {
        try {
            String resource = "mybatis-config.xml";
            InputStream inputStream = Resources.getResourceAsStream(resource);
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static SqlSession getSqlSession() {
        return sqlSessionFactory.openSession(true);  // 自动提交
    }
}
```

---

## 六、编写测试类

```java
package com.test;

import com.mapper.UserMapper;
import com.pojo.User;
import com.util.MyBatisUtil;
import org.apache.ibatis.session.SqlSession;
import org.junit.Test;
import java.util.List;

public class UserTest {
    @Test
    public void testSelectAll() {
        try (SqlSession session = MyBatisUtil.getSqlSession()) {
            UserMapper mapper = session.getMapper(UserMapper.class);
            List<User> userList = mapper.selectAll();
            for (User user : userList) {
                System.out.println(user);
            }
        }
    }
}
```

---

## 执行流程总结

```
mybatis-config.xml → SqlSessionFactory → SqlSession → Mapper 代理对象 → 执行 SQL
```
