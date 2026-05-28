MyBatis 教程
一、MyBatis 是什么
MyBatis 是一款 Java 持久层框架，负责数据库操作。
它可以把 SQL 写在 XML 或者注解里，和 Java 代码分开，方便维护。
是企业级开发最常用的数据库访问框架之一。

---------------------------------------------------------------------------------------------------------------------------------------
二、Spring Boot 整合 MyBatis
1. 引入依赖
    在 pom.xml 中添加
    <dependency>
    <groupId>org.mybatis.spring.boot</groupId>
    <artifactId>mybatis-spring-boot-starter</artifactId>
    <version>2.3.0</version>
    </dependency>
    <dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
    </dependency>
2. 配置文件 application.yml
    spring:
  datasource:
    url: jdbc:mysql://localhost:3306/数据库名?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: 账号
    password: 密码
    driver-class-name: com.mysql.cj.jdbc.Driver
    mybatis:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: 实体类所在包
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl

---------------------------------------------------------------------------------------------------------------------------------------
三、基本使用步骤
1. 建立数据库表
    例如用户表
    create table user
    (
    id          bigint auto_increment primary key,
    username    varchar(50),
    password    varchar(100),
    nickname    varchar(50),
    age         int
    );
2. 建立实体类
    package com.example.entity;
    public class User {
    private Long id;
    private String username;
    private String password;
    private String nickname;
    private Integer age;
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public String getNickname() {
        return nickname;
    }
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
    public Integer getAge() {
        return age;
    }
    public void setAge(Integer age) {
        this.age = age;
    }
    }
3. 编写 Mapper 接口
    package com.example.mapper;
    import com.example.entity.User;
    import org.apache.ibatis.annotations.Insert;
    import org.apache.ibatis.annotations.Mapper;
    import org.apache.ibatis.annotations.Options;
    import org.apache.ibatis.annotations.Select;
    import java.util.List;
    @Mapper
    public interface UserMapper {
    @Select("select * from user where id = #{id}")
    User findById(Long id);
    @Insert("insert into user(username,password,nickname,age) values(#{username},#{password},#{nickname},#{age})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);
    List<User> findList();
    int update(User user);
    int delete(Long id);
    }
4. 编写 XML 映射文件
    在 resources/mapper 下创建 UserMapper.xml
    <?xml version="1.0" encoding="UTF-8"?>
    <!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
    <mapper namespace="com.example.mapper.UserMapper">
    <select id="findList" resultType="com.example.entity.User">
        select * from user
    </select>
    <update id="update">
        update user
        <set>
            <if test="username != null">
                username = #{username},
            </if>
            <if test="nickname != null">
                nickname = #{nickname},
            </if>
            <if test="age != null">
                age = #{age}
            </if>
        </set>
        where id = #{id}
    </update>
    <delete id="delete">
        delete from user where id = #{id}
    </delete>
    </mapper>
5. 编写 Service 层
    package com.example.service;
    import com.example.entity.User;
    import com.example.mapper.UserMapper;
    import org.springframework.stereotype.Service;
    import javax.annotation.Resource;
    import java.util.List;
    @Service
    public class UserService {
    @Resource
    private UserMapper userMapper;
    public User findById(Long id) {
        return userMapper.findById(id);
    }
    public int add(User user) {
        return userMapper.insert(user);
    }
    public List<User> findList() {
        return userMapper.findList();
    }
    public int update(User user) {
        return userMapper.update(user);
    }
    public int delete(Long id) {
        return userMapper.delete(id);
    }
    }
6. 编写 Controller 层
    package com.example.controller;
    import com.example.entity.User;
    import com.example.service.UserService;
    import org.springframework.web.bind.annotation.*;
    import javax.annotation.Resource;
    import java.util.List;
    @RestController
    @RequestMapping("/user")
    public class UserController {
    @Resource
    private UserService userService;
    @GetMapping("/{id}")
    public User findById(@PathVariable Long id) {
        return userService.findById(id);
    }
    @PostMapping
    public String add(@RequestBody User user) {
        userService.add(user);
        return "success";
    }
    @GetMapping("/list")
    public List<User> list() {
        return userService.findList();
    }
    @PutMapping
    public String update(@RequestBody User user) {
        userService.update(user);
        return "success";
    }
    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id) {
        userService.delete(id);
        return "success";
    }
    }
    --------------------------------------------------------------------------------------------------------------------------------------- 四、MyBatis 核心知识点
    1. #{} 和 ${} 区别

# } 会预编译，防止 SQL 注入，推荐使用
${} 直接拼接字符串，有 SQL 注入风险，只用于动态表名、排序字段
2. 动态 SQL
    常用标签
    if
    choose
    when
    where
    set
    foreach
    主要用于拼接条件、批量插入、批量删除等
3. 结果映射
    resultType 简单类型、实体类
    resultMap 复杂映射，一对一、一对多
4. 分页
    企业常用两种方式
    1. 手写 limit
    2. 使用 PageHelper 分页插件
    --------------------------------------------------------------------------------------------------------------------------------------- 五、企业常用写法
    1. Mapper 接口 + XML 分离 SQL
    2. 统一返回结果
    3. 分页查询
    4. 条件构造查询
    5. 批量操作
    6. 逻辑删除而非物理删除
    --------------------------------------------------------------------------------------------------------------------------------------- 六、MyBatis-Plus 简介
    MyBatis-Plus 是对 MyBatis 的增强
    不改变原有功能，只做增强
    提供通用 CRUD、条件构造器、分页、逻辑删除等功能
    可以大幅简化单表操作代码
