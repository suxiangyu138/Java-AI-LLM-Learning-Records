手把手带你写 Spring 入门练习项目
我们做一个最简单的 Spring 核心功能练习项目：用户管理小系统（包含依赖注入、IOC、Bean 管理、Controller 接口、Service 业务层），全程跟着做，10 分钟搞定！
一、项目准备（环境要求）
开发工具：IDEA（推荐）/ Eclipse
JDK 8+
Maven 3.6+
Spring 框架（用 Spring Boot 简化配置）
二、快速创建项目（Spring Initializr）
1. 打开项目生成地址
    https://start.spring.io/
2. 配置项目
    Project：Maven
    Language：Java
    Spring Boot：3.2.x（稳定版）
    Group：com.example
    Artifact：spring-demo
    Name：spring-demo
    Package name：com.example.springdemo
    Java：17
3. 选择依赖
    勾选 Spring Web（做接口用）
4. 点击 GENERATE 下载项目，解压后用 IDEA 打开
    三、项目结构（标准三层架构）
    src/main/java/com/example/springdemo/
    ├── SpringDemoApplication.java  # 项目启动类
    ├── controller/        # 控制层（接收前端请求）
    │   └── UserController.java
    ├── service/           # 业务层（处理逻辑）
    │   ├── UserService.java
    │   └── impl/
    │       └── UserServiceImpl.java
    ├── entity/            # 实体类
    │   └── User.java
    └── mapper/            # 数据层（简化版，不用数据库）
    └── UserMapper.java
    四、开始写代码（一步一步来）
    1. 实体类：User.java（entity 包）
    存放用户数据
    package com.example.springdemo.entity;
    public class User {
    private Long id;
    private String username;
    private String password;
    // 无参构造
    public User() {}
    // 有参构造
    public User(Long id, String username, String password) {
        this.id = id;
        this.username = username;
        this.password = password;
    }
    // getter & setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    }
2. 业务层接口：UserService.java（service 包）
    package com.example.springdemo.service;
    import com.example.springdemo.entity.User;
    import java.util.List;
    // 业务层接口
    public interface UserService {
    // 获取所有用户
    List<User> getAllUsers();
    // 根据ID查询用户
    User getUserById(Long id);
    }
3. 业务层实现类：UserServiceImpl.java（service/impl）
    @Service：交给 Spring 管理（IOC 核心）
    @Autowired：自动注入依赖（DI 依赖注入）
    package com.example.springdemo.service.impl;
    import com.example.springdemo.entity.User;
    import com.example.springdemo.mapper.UserMapper;
    import com.example.springdemo.service.UserService;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.stereotype.Service;
    import java.util.List;
    @Service
    public class UserServiceImpl implements UserService {
    // 自动注入数据层
    @Autowired
    private UserMapper userMapper;
    @Override
    public List<User> getAllUsers() {
        return userMapper.selectAllUsers();
    }
    @Override
    public User getUserById(Long id) {
        return userMapper.selectUserById(id);
    }
    }
4. 数据层：UserMapper.java（mapper 包）
    模拟数据库，不用真的连库
    @Repository：数据层 Bean
    package com.example.springdemo.mapper;
    import com.example.springdemo.entity.User;
    import org.springframework.stereotype.Repository;
    import javax.annotation.PostConstruct;
    import java.util.ArrayList;
    import java.util.List;
    @Repository
    public class UserMapper {
    // 模拟数据库
    private List<User> userList = new ArrayList<>();
    // 项目启动时初始化数据
    @PostConstruct
    public void initData() {
        userList.add(new User(1L, "张三", "123456"));
        userList.add(new User(2L, "李四", "654321"));
        userList.add(new User(3L, "王五", "111222"));
    }
    // 查询所有用户
    public List<User> selectAllUsers() {
        return userList;
    }
    // 根据ID查询
    public User selectUserById(Long id) {
        for (User user : userList) {
            if (user.getId().equals(id)) {
                return user;
            }
        }
        return null;
    }
    }
5. 控制层：UserController.java（controller 包）
    @RestController：返回 JSON 数据
    @Autowired：注入 Service
    package com.example.springdemo.controller;
    import com.example.springdemo.entity.User;
    import com.example.springdemo.service.UserService;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.web.bind.annotation.GetMapping;
    import org.springframework.web.bind.annotation.PathVariable;
    import org.springframework.web.bind.annotation.RequestMapping;
    import org.springframework.web.bind.annotation.RestController;
    import java.util.List;
    @RestController
    @RequestMapping("/user")
    public class UserController {
    @Autowired
    private UserService userService;
    // 1. 获取所有用户
    @GetMapping("/all")
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }
    // 2. 根据ID查询用户
    @GetMapping("/{id}")
    public User getUserById(@PathVariable Long id) {
        return userService.getUserById(id);
    }
    }
6. 启动类：SpringDemoApplication.java
    package com.example.springdemo;
    import org.springframework.boot.SpringApplication;
    import org.springframework.boot.autoconfigure.SpringBootApplication;
    // Spring Boot 核心注解，自动扫描包
    @SpringBootApplication
    public class SpringDemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpringDemoApplication.class, args);
    }
    }
    五、运行项目
    打开 SpringDemoApplication.java
    点击运行（绿色三角）
    看到 Tomcat started on port 8080 说明启动成功
    六、测试接口（浏览器直接访问）
    获取所有用户
    http://localhost:8080/user/all
    根据ID查询用户
    http://localhost:8080/user/1
    http://localhost:8080/user/2
    能正常返回 JSON 数据，项目就跑通啦！
    七、你学到了什么？（Spring 核心知识点）
    IOC 控制反转：对象由 Spring 创建，不用自己 new
    DI 依赖注入：@Autowired 自动装配对象
    三层架构
    Controller：接收请求
    Service：处理业务
    Mapper：操作数据
    Spring 常用注解
    @SpringBootApplication
    @RestController
    @Service
    @Repository
    @Autowired
    @GetMapping
    八、可以继续扩展的练习（进阶）
    你可以自己试着加功能：
    添加用户（POST）
    删除用户
    修改用户
    连接 MySQL 数据库
    加 MyBatis
    加统一返回结果、异常处理
    总结
    这是一个标准 Spring Boot 入门项目，覆盖核心用法
    全程无复杂配置，开箱即用
    学会后你就掌握了 Spring 最常用的开发模式
    接口可直接测试，非常适合练习
    需要我带你扩展功能（比如连接数据库、增删改查）吗？
