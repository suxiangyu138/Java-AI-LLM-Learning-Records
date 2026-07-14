# 手把手带你写 Spring 入门练习项目

> 一个最简单的 Spring 核心功能练习项目：用户管理小系统，包含依赖注入、IOC、Bean 管理、Controller 接口、Service 业务层，全程跟着做，10 分钟搞定！

---

## 目录

- [1. 环境要求](#1-环境要求)
- [2. 快速创建项目](#2-快速创建项目)
- [3. 项目结构](#3-项目结构)
- [4. 开始写代码](#4-开始写代码)
- [5. 运行项目](#5-运行项目)
- [6. 测试接口](#6-测试接口)
- [7. 核心知识点总结](#7-核心知识点总结)
- [8. 扩展练习](#8-扩展练习)

---

## 1. 环境要求

| 工具 | 版本要求 |
|------|----------|
| 开发工具 | IDEA（推荐）/ Eclipse |
| JDK | 8+ |
| Maven | 3.6+ |
| 框架 | Spring Boot（简化配置） |

---

## 2. 快速创建项目

### 2.1 打开项目生成地址

```
https://start.spring.io/
```

### 2.2 配置项目

| 配置项 | 值 |
|--------|-----|
| Project | Maven |
| Language | Java |
| Spring Boot | 3.2.x（稳定版） |
| Group | com.example |
| Artifact | spring-demo |
| Name | spring-demo |
| Package name | com.example.springdemo |
| Java | 17 |

### 2.3 选择依赖

勾选 **Spring Web**（做接口用）

### 2.4 下载项目

点击 **GENERATE** 下载项目，解压后用 IDEA 打开。

---

## 3. 项目结构

```
src/main/java/com/example/springdemo/
├── SpringDemoApplication.java    # 项目启动类
├── controller/                   # 控制层（接收前端请求）
│   └── UserController.java
├── service/                      # 业务层（处理逻辑）
│   ├── UserService.java
│   └── impl/
│       └── UserServiceImpl.java
├── entity/                       # 实体类
│   └── User.java
└── mapper/                       # 数据层（简化版，不用数据库）
    └── UserMapper.java
```

---

## 4. 开始写代码

### 4.1 实体类：User.java（entity 包）

```java
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
```

### 4.2 业务层接口：UserService.java（service 包）

```java
package com.example.springdemo.service;

import com.example.springdemo.entity.User;
import java.util.List;

public interface UserService {
    /** 获取所有用户 */
    List<User> getAllUsers();
    /** 根据ID查询用户 */
    User getUserById(Long id);
}
```

### 4.3 业务层实现类：UserServiceImpl.java（service/impl 包）

> **`@Service`**：交给 Spring 管理（IOC 核心）
> **`@Autowired`**：自动注入依赖（DI 依赖注入）

```java
package com.example.springdemo.service.impl;

import com.example.springdemo.entity.User;
import com.example.springdemo.mapper.UserMapper;
import com.example.springdemo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

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
```

### 4.4 数据层：UserMapper.java（mapper 包）

> **`@Repository`**：数据层 Bean
> **`@PostConstruct`**：项目启动时初始化数据

```java
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
```

### 4.5 控制层：UserController.java（controller 包）

> **`@RestController`**：返回 JSON 数据
> **`@Autowired`**：注入 Service

```java
package com.example.springdemo.controller;

import com.example.springdemo.entity.User;
import com.example.springdemo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
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
```

### 4.6 启动类：SpringDemoApplication.java

```java
package com.example.springdemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication  // Spring Boot 核心注解，自动扫描包
public class SpringDemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpringDemoApplication.class, args);
    }
}
```

---

## 5. 运行项目

1. 打开 `SpringDemoApplication.java`
2. 点击运行（绿色三角）
3. 看到 **Tomcat started on port 8080** 说明启动成功

---

## 6. 测试接口

浏览器直接访问：

| 接口 | URL | 说明 |
|------|-----|------|
| 获取所有用户 | `http://localhost:8080/user/all` | 返回用户列表 JSON |
| 查询用户 1 | `http://localhost:8080/user/1` | 返回用户详情 JSON |
| 查询用户 2 | `http://localhost:8080/user/2` | 返回用户详情 JSON |

> 能正常返回 JSON 数据，项目就跑通啦！

---

## 7. 核心知识点总结

### Spring 核心概念

| 概念 | 说明 | 对应注解/机制 |
|------|------|---------------|
| IOC 控制反转 | 对象由 Spring 创建，不用自己 new | `@Service`、`@Repository`、`@Component` |
| DI 依赖注入 | 自动装配对象 | `@Autowired` |

### 三层架构

| 层 | 职责 | 注解 |
|----|------|------|
| Controller | 接收请求 | `@RestController` |
| Service | 处理业务 | `@Service` |
| Mapper | 操作数据 | `@Repository` |

### 常用注解

| 注解 | 说明 |
|------|------|
| `@SpringBootApplication` | Spring Boot 核心注解 |
| `@RestController` | 返回 JSON 数据的 Controller |
| `@Service` | 业务层注解 |
| `@Repository` | 数据层注解 |
| `@Autowired` | 依赖注入 |
| `@GetMapping` | GET 请求映射 |

---

## 8. 扩展练习

可以自己试着加功能：

- 添加用户（POST 请求）
- 删除用户
- 修改用户
- 连接 MySQL 数据库
- 集成 MyBatis
- 添加统一返回结果和异常处理

---

## 总结

- 这是一个标准 Spring Boot 入门项目，覆盖核心用法
- 全程无复杂配置，开箱即用
- 学会后你就掌握了 Spring 最常用的开发模式
- 接口可直接测试，非常适合练习
