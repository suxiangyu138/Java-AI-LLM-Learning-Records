03.13 16:41
MyBatis 零基础入门教程
一、核心概念
MyBatis 是一款持久层框架，用于简化 JDBC 操作，通过 XML 或注解方式将 SQL 语句与 Java 代码解耦，支持自定义 SQL、存储过程和高级映射。
二、环境准备
1. 确保已安装 JDK 8+、中文版 IntelliJ IDEA、Maven，本地安装 MySQL 数据库并启动
2. 新建 Maven 项目
- 打开 IDEA → 点击文件 → 新建 → 项目
- 选择 Maven → 取消勾选从模板创建 → 点击下一步
- 填写项目名称（如  MyBatisDemo ）、存储路径 → 点击完成
三、添加依赖（pom.xml）
在 pom.xml 中添加 MyBatis、MySQL 驱动、Junit 依赖，Maven 会自动下载 jar 包
xml
<dependencies>
    <!-- MyBatis 核心依赖 -->
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
    <!-- Junit 测试依赖 -->
    <dependency>
        <groupId>junit</groupId>
        <artifactId>junit</artifactId>
        <version>4.13.2</version>
        <scope>test</scope>
    </dependency>
</dependencies>
 
添加后点击 IDEA 右上角的 刷新 按钮，导入依赖
四、编写核心配置文件
1. 数据库准备
在 MySQL 中创建数据库和用户表
sql
CREATE DATABASE IF NOT EXISTS mybatis_demo;
USE mybatis_demo;
CREATE TABLE IF NOT EXISTS user (
    id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    age INT NOT NULL
);
INSERT INTO user (username, age) VALUES ('zhangsan', 20), ('lisi', 22);
 
2. 配置 MyBatis 核心文件  mybatis-config.xml 
在  src/main/resources  下新建  mybatis-config.xml 
xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE configuration
        PUBLIC "-//mybatis.org//DTD Config 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-config.dtd">
<configuration>
    <!-- 配置环境 -->
    <environments default="development">
        <environment id="development">
            <transactionManager type="JDBC"/>
            <dataSource type="POOLED">
                <property name="driver" value="com.mysql.cj.jdbc.Driver"/>
                <property name="url" value="jdbc:mysql://localhost:3306/mybatis_demo?useSSL=false&amp;serverTimezone=UTC"/>
                <property name="username" value="root"/> <!-- 替换为你的 MySQL 用户名 -->
                <property name="password" value="root"/> <!-- 替换为你的 MySQL 密码 -->
            </dataSource>
        </environment>
    </environments>
    <!-- 配置映射器 -->
    <mappers>
        <mapper resource="com/mapper/UserMapper.xml"/>
    </mappers>
</configuration>
 
五、编写实体类、Mapper 接口和映射文件
1. 编写实体类  User.java 
在  src/main/java  下新建包  com.pojo ，创建  User.java 
java
package com.pojo;
public class User {
    private Integer id;
    private String username;
    private Integer age;
    // 无参构造、有参构造
    public User() {}
    public User(Integer id, String username, Integer age) {
        this.id = id;
        this.username = username;
        this.age = age;
    }
    // Getter 和 Setter 方法
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    // toString 方法
    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", age=" + age +
                '}';
    }
}
 
2. 编写 Mapper 接口  UserMapper.java 
在  src/main/java  下新建包  com.mapper ，创建  UserMapper.java 
java
package com.mapper;
import com.pojo.User;
import java.util.List;
public interface UserMapper {
    // 查询所有用户
    List<User> selectAll();
}
 
3. 编写 Mapper 映射文件  UserMapper.xml 
在  src/main/resources  下新建文件夹  com/mapper ，创建  UserMapper.xml 
xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<!-- namespace 对应 Mapper 接口的全类名 -->
<mapper namespace="com.mapper.UserMapper">
    <!-- id 对应接口中的方法名，resultType 对应返回值类型 -->
    <select id="selectAll" resultType="com.pojo.User">
        SELECT * FROM user
    </select>
</mapper>
 
六、编写工具类获取 SqlSession
在  src/main/java  下新建包  com.util ，创建  MyBatisUtil.java 
java
package com.util;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import java.io.IOException;
import java.io.InputStream;
public class MyBatisUtil {
    private static SqlSessionFactory sqlSessionFactory;
    // 静态代码块，只执行一次
    static {
        try {
            // 加载核心配置文件
            String resource = "mybatis-config.xml";
            InputStream inputStream = Resources.getResourceAsStream(resource);
            // 构建 SqlSessionFactory
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    // 获取 SqlSession 对象
    public static SqlSession getSqlSession() {
        // 自动提交事务：openSession(true)
        return sqlSessionFactory.openSession(true);
    }
}
 
七、编写测试类
在  src/test/java  下新建包  com.test ，创建  UserTest.java 
java
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
        // 获取 SqlSession
        try (SqlSession session = MyBatisUtil.getSqlSession()) {
            // 获取 Mapper 代理对象
            UserMapper mapper = session.getMapper(UserMapper.class);
            // 调用方法执行查询
            List<User> userList = mapper.selectAll();
            // 遍历输出结果
            for (User user : userList) {
                System.out.println(user);
            }
        }
    }
}
 
八、运行测试
1. 右键点击测试类  UserTest  → 选择 运行 'UserTest'
2. 控制台输出用户数据，说明 MyBatis 环境搭建成功

