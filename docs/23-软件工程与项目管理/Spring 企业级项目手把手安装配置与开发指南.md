03.31 03:25
Spring 企业级项目手把手安装配置与开发指南
一、环境准备（前置条件）
1. 必备软件安装
1. JDK 8+（推荐JDK11，企业级主流版本）
- 下载地址：https://www.oracle.com/java/technologies/downloads/
- 配置环境变量：JAVA_HOME、Path（添加%JAVA_HOME%\bin）
- 验证：cmd输入java -version、javac -version，显示版本即成功
2. Maven 3.6+（依赖管理工具）
- 下载地址：https://maven.apache.org/download.cgi
- 配置环境变量：MAVEN_HOME、Path（添加%MAVEN_HOME%\bin）
- 验证：cmd输入mvn -v，显示版本即成功
- 配置阿里云镜像（加速依赖下载）：
打开maven/conf/settings.xml，在标签内添加：
aliyunmaven
central
https://maven.aliyun.com/repository/public
3. IDEA 2020+（开发工具，社区版/旗舰版均可）
- 下载地址：https://www.jetbrains.com/idea/download/
2. IDEA 基础配置
1. 配置Maven：File → Settings → Build,Execution,Deployment → Build Tools → Maven
- Maven home path：选择本地Maven安装目录
- User settings file：选择maven/conf/settings.xml
- Local repository：默认即可（建议自定义非中文路径）
2. 配置JDK：File → Project Structure → Project Settings → Project
- Project SDK：选择已安装的JDK版本
- Project language level：对应JDK版本（JDK11选11）
二、Spring 核心框架安装配置（纯Spring，非Spring Boot）
1. 创建Maven项目
1. IDEA打开 → New Project → Maven → 勾选Create from archetype → 选择maven-archetype-quickstart → Next
2. 填写GroupId（企业域名反写，如com.company）、ArtifactId（项目名，如spring-enterprise-demo）→ Next
3. 确认Maven配置 → Finish，等待项目初始化完成
2. 引入Spring核心依赖（pom.xml）
打开项目根目录的pom.xml，在标签内添加以下依赖（企业级常用版本：Spring 5.3.x）：org.springframeworkspring-context5.3.20org.springframeworkspring-beans5.3.20org.springframeworkspring-core5.3.20org.springframeworkspring-aop5.3.20com.alibabadruid1.2.16mysqlmysql-connector-java8.0.30org.slf4jslf4j-api1.7.36ch.qos.logbacklogback-classic1.2.11junitjunit4.13.2test
3. Spring 核心配置文件（applicationContext.xml）
1. 在src/main/resources目录下创建applicationContext.xml（Spring核心配置文件）
2. 配置内容（企业级标准配置，包含包扫描、数据源、AOP）：
<context:component-scan base-package="com.company"/>
aop:aspectj-autoproxy/
4. 日志配置文件（logback.xml）
在src/main/resources目录下创建logback.xml，配置企业级日志输出：%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{50} - %msg%nUTF-8
三、企业级项目架构搭建（分层架构，标准MVC）
1. 项目包结构（企业级标准分层）
src/main/java/com.company（对应GroupId）
├── controller  # 控制层：接收请求、返回响应
├── service     # 业务层：核心业务逻辑
│   └── impl    # 业务实现类
├── mapper      # 数据访问层：数据库交互
├── entity      # 实体层：数据库映射对象
├── config      # 配置层：自定义配置类
├── aspect      # AOP切面：日志、事务、权限
└── util        # 工具层：通用工具类
2. 核心代码实现（企业级标准代码）
（1）实体层（entity/User.java）
package com.company.entity;
/**
- 用户实体类（数据库映射对象）
*/
public class User {
private Long id;
private String username;
private String password;
private String email;// 无参构造（Spring反射必备）
public User() {}// getter/setter
public Long getId() { return id; }
public void setId(Long id) { this.id = id; }
public String getUsername() { return username; }
public void setUsername(String username) { this.username = username; }
public String getPassword() { return password; }
public void setPassword(String password) { this.password = password; }
public String getEmail() { return email; }
public void setEmail(String email) { this.email = email; }@Override
public String toString() {
return "User{id=" + id + ", username='" + username + "', email='" + email + "'}";
}
}
（2）数据访问层（mapper/UserMapper.java）
package com.company.mapper;
import com.company.entity.User;
import com.alibaba.druid.pool.DruidDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
/**
- 用户数据访问层（数据库交互）
*/
@Repository
public class UserMapper {@Autowired
private DruidDataSource dataSource;/**
- 根据ID查询用户
- @param id 用户ID
- @return 用户对象
*/
public User findById(Long id) {
Connection conn = null;
PreparedStatement pstmt = null;
ResultSet rs = null;
User user = null;
try {
conn = dataSource.getConnection();
String sql = "SELECT id, username, password, email FROM user WHERE id = ?";
pstmt = conn.prepareStatement(sql);
pstmt.setLong(1, id);
rs = pstmt.executeQuery();
if (rs.next()) {
user = new User();
user.setId(rs.getLong("id"));
user.setUsername(rs.getString("username"));
user.setPassword(rs.getString("password"));
user.setEmail(rs.getString("email"));
}
} catch (SQLException e) {
e.printStackTrace();
} finally {
// 关闭资源（企业级必须释放资源）
try {
if (rs != null) rs.close();
if (pstmt != null) pstmt.close();
if (conn != null) conn.close();
} catch (SQLException e) {
e.printStackTrace();
}
}
return user;
}
}
（3）业务层（service/UserService.java + impl/UserServiceImpl.java）
// UserService.java
package com.company.service;
import com.company.entity.User;
/**
- 用户业务接口
*/
public interface UserService {
User findUserById(Long id);
}
// UserServiceImpl.java
package com.company.service.impl;
import com.company.entity.User;
import com.company.mapper.UserMapper;
import com.company.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
/**
- 用户业务实现类
*/
@Service
public class UserServiceImpl implements UserService {@Autowired
private UserMapper userMapper;@Override
public User findUserById(Long id) {
// 业务逻辑：查询用户
return userMapper.findById(id);
}
}
（4）控制层（controller/UserController.java）
package com.company.controller;
import com.company.entity.User;
import com.company.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
/**
- 用户控制层（接收请求、调用业务）
*/
@Controller
public class UserController {@Autowired
private UserService userService;/**
- 根据ID查询用户
- @param id 用户ID
- @return 用户信息
*/
public User getUserById(Long id) {
return userService.findUserById(id);
}
}
（5）AOP切面（aspect/LogAspect.java）
package com.company.aspect;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
/**
- 日志切面（AOP实现，统一记录方法执行日志）
*/
@Aspect
@Component
public class LogAspect {private static final Logger logger = LoggerFactory.getLogger(LogAspect.class);/**
- 前置通知：方法执行前打印日志
/
@Before("execution( com.company.service.impl..(..))")
public void beforeMethod(JoinPoint joinPoint) {
String methodName = joinPoint.getSignature().getName();
logger.info("方法执行前：{}", methodName);
}
/**
- 后置通知：方法执行后打印返回结果
/
@AfterReturning(pointcut = "execution( com.company.service.impl..(..))", returning = "result")
public void afterReturningMethod(Object result) {
logger.info("方法执行后，返回结果：{}", result);
}
}
四、数据库准备（MySQL）
1. 创建数据库：
CREATE DATABASE enterprise_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
2. 创建用户表：
USE enterprise_db;
CREATE TABLE user (
id BIGINT PRIMARY KEY AUTO_INCREMENT,
username VARCHAR(50) NOT NULL UNIQUE,
password VARCHAR(100) NOT NULL,
email VARCHAR(100)
);
3. 插入测试数据：
INSERT INTO user (username, password, email) VALUES ('admin', '123456', 'admin@company.com');
五、项目启动与测试
1. 启动类（App.java）
package com.company;
import com.company.controller.UserController;
import com.company.entity.User;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
/**
- 项目启动类（Spring容器初始化入口）
*/
public class App {
public static void main(String[] args) {
// 1. 初始化Spring容器（加载核心配置文件）
ApplicationContext context = new ClassPathXmlApplicationContext("applicationContext.xml");// 2. 从容器中获取Bean（控制层对象）
UserController userController = context.getBean(UserController.class);// 3. 调用方法测试
User user = userController.getUserById(1L);
System.out.println("查询到的用户：" + user);
}
}
2. 运行测试
1. 确认数据库连接信息（applicationContext.xml中url、username、password正确）
2. 运行App.java的main方法
3. 控制台输出：
2024-XX-XX XX:XX:XX [main] INFO  com.company.aspect.LogAspect - 方法执行前：findUserById
2024-XX-XX XX:XX:XX [main] INFO  com.company.aspect.LogAspect - 方法执行后，返回结果：User{id=1, username='admin', email='admin@company.com'}
查询到的用户：User{id=1, username='admin', email='admin@company.com'}
六、企业级扩展（进阶配置）
1. 事务管理：添加spring-tx依赖，配置事务管理器，实现声明式事务
2. 整合MyBatis：替换原生JDBC，简化数据库操作（企业级主流）
3. 整合Spring MVC：实现Web接口，支持HTTP请求（前后端分离）
4. 整合Redis：实现缓存，提升查询性能
5. 配置文件分离：开发/测试/生产环境多配置切换（application-dev.xml、application-prod.xml）

