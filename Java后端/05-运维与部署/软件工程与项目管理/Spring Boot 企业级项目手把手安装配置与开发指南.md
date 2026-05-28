Spring Boot 企业级项目手把手安装配置与开发指南
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
    二、Spring Boot 项目创建与配置
    1. 创建Spring Boot项目
    1. IDEA打开 → New Project → Spring Initializr → Next
    2. 填写Group（企业域名反写，如com.company）、Artifact（项目名，如springboot-enterprise-demo）→ Next
    3. 选择依赖（企业级必备）：
    - Spring Web（Web开发）
    - Spring Data JDBC（数据库操作）
    - MySQL Driver（MySQL驱动）
    - Lombok（简化代码）
    - Spring Boot DevTools（热部署）
    - Spring Boot Configuration Processor（配置提示）
4. 点击Finish，等待项目初始化完成
2. 项目结构说明（企业级标准）
    src/main/java/com.company
    ├── SpringbootEnterpriseDemoApplication.java  # 启动类
    ├── controller  # 控制层：接收请求、返回响应
    ├── service     # 业务层：核心业务逻辑
    │   └── impl    # 业务实现类
    ├── mapper      # 数据访问层：数据库交互
    ├── entity      # 实体层：数据库映射对象
    ├── config      # 配置层：自定义配置类
    ├── aspect      # AOP切面：日志、事务、权限
    └── util        # 工具层：通用工具类
    src/main/resources
    ├── application.yml  # Spring Boot核心配置文件
    └── static  # 静态资源
    └── templates  # 模板文件
3. 核心配置文件（application.yml）
    server:
    port: 8080  # 服务端口
    servlet:
    context-path: /api  # 项目上下文路径
    spring:
    数据库配置
    datasource:
    url: jdbc:mysql://localhost:3306/enterprise_db?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false
    username: root
    password: 你的数据库密码
    driver-class-name: com.mysql.cj.jdbc.Driver

# Druid连接池配置
type: com.alibaba.druid.pool.DruidDataSource
druid:
initial-size: 5
max-active: 20
min-idle: 5
max-wait: 60000
日志配置
logging:
level:
root: info
com.company: debug
三、企业级项目代码实现
1. 实体层（entity/User.java）
    package com.company.entity;
    import lombok.Data;
    /**
    - 用户实体类
    */
    @Data
    public class User {
    private Long id;
    private String username;
    private String password;
    private String email;
    }
2. 数据访问层（mapper/UserMapper.java）
    package com.company.mapper;
    import com.company.entity.User;
    import org.apache.ibatis.annotations.Select;
    import org.springframework.stereotype.Repository;
    /**
    - 用户数据访问层
    */
    @Repository
    public interface UserMapper {@Select("SELECT id, username, password, email FROM user WHERE id = #{id}")
    User findById(Long id);
    }
3. 业务层（service/UserService.java + impl/UserServiceImpl.java）
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
    return userMapper.findById(id);
    }
    }
4. 控制层（controller/UserController.java）
    package com.company.controller;
    import com.company.entity.User;
    import com.company.service.UserService;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.web.bind.annotation.GetMapping;
    import org.springframework.web.bind.annotation.PathVariable;
    import org.springframework.web.bind.annotation.RequestMapping;
    import org.springframework.web.bind.annotation.RestController;
    /**
    - 用户控制层
    */
    @RestController
    @RequestMapping("/user")
    public class UserController {@Autowired
    private UserService userService;@GetMapping("/{id}")
    public User getUserById(@PathVariable Long id) {
    return userService.findUserById(id);
    }
    }
5. AOP切面（aspect/LogAspect.java）
    package com.company.aspect;
    import org.aspectj.lang.JoinPoint;
    import org.aspectj.lang.annotation.AfterReturning;
    import org.aspectj.lang.annotation.Aspect;
    import org.aspectj.lang.annotation.Before;
    import org.slf4j.Logger;
    import org.slf4j.LoggerFactory;
    import org.springframework.stereotype.Component;
    /**
    - 日志切面
    */
    @Aspect
    @Component
    public class LogAspect {private static final Logger logger = LoggerFactory.getLogger(LogAspect.class);@Before("execution(* com.company.service.impl..(..))")
    public void beforeMethod(JoinPoint joinPoint) {
    String methodName = joinPoint.getSignature().getName();
    logger.info("方法执行前：{}", methodName);
    }@AfterReturning(pointcut = "execution(* com.company.service.impl..(..))", returning = "result")
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
    1. 启动类（SpringbootEnterpriseDemoApplication.java）
    package com.company;
    import org.mybatis.spring.annotation.MapperScan;
    import org.springframework.boot.SpringApplication;
    import org.springframework.boot.autoconfigure.SpringBootApplication;
    /**
    - Spring Boot启动类
    */
    @SpringBootApplication
    @MapperScan("com.company.mapper")
    public class SpringbootEnterpriseDemoApplication {public static void main(String[] args) {
    SpringApplication.run(SpringbootEnterpriseDemoApplication.class, args);
    }
    }
2. 运行测试
    1. 确认数据库连接信息（application.yml中url、username、password正确）
    2. 运行启动类的main方法
    3. 浏览器访问：http://localhost:8080/api/user/1
    4. 响应结果：
    {"id":1,"username":"admin","password":"123456","email":"admin@company.com"}
    六、企业级扩展（进阶配置）
    1. 整合MyBatis-Plus：简化CRUD操作
    2. 整合Redis：实现缓存
    3. 整合Spring Security：实现权限控制
    4. 整合Swagger：生成API文档
    5. 多环境配置：开发、测试、生产环境分离
    6. 部署打包：使用Maven打包为jar包，部署到服务器
