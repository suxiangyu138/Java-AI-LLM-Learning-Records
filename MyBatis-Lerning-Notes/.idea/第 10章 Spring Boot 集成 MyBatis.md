# 第10章 SpringBoot 集成 MyBatis
## 10.1 集成概述
SpringBoot 大幅简化 MyBatis 整合流程：
- 抛弃繁琐 XML 配置
- 自动装配数据源、SqlSession 环境
- 一键引入starter依赖，**零配置快速开发**
- 同时支持 **XML映射 + 注解开发** 两种方式

---

## 10.2 核心依赖
SpringBoot 整合 MyBatis 专用启动器：
```xml
<!-- SpringBoot 整合 MyBatis 启动器 -->
<dependency>
    <groupId>org.mybatis.spring.boot</groupId>
    <artifactId>mybatis-spring-boot-starter</artifactId>
    <version>2.2.2</version>
</dependency>

<!-- MySQL 驱动 -->
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <scope>runtime</scope>
</dependency>
```
`mybatis-spring-boot-starter` 内部已整合：
MyBatis核心 + mybatis-spring 整合包 + 自动装配类

---

## 10.3 核心配置（application.yml）
```yaml
# 数据源配置
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/mybatis_test?serverTimezone=UTC&useSSL=false
    username: root
    password: 你的密码

# MyBatis 配置
mybatis:
  # 映射文件路径
  mapper-locations: classpath:mapper/*.xml
  # 实体类别名扫描
  type-aliases-package: com.pojo
  configuration:
    # 开启驼峰命名自动转换
    map-underscore-to-camel-case: true
```

---

## 10.4 核心注解
### 1. @MapperScan
作用：**扫描 Mapper 接口**，自动创建代理对象，交给Spring容器管理
放在启动类上：
```java
@SpringBootApplication
@MapperScan("com.mapper")
public class MyBatisApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyBatisApplication.class,args);
    }
}
```

### 2. @Mapper
单个 Mapper 接口上加注解，替代全局扫描（不推荐）

---

## 10.5 方式一：XML 映射开发
### 1. 实体类
```java
public class User {
    private Integer id;
    private String username;
    private String password;
    private Integer age;
    // getter / setter / toString
}
```

### 2. Mapper 接口
```java
public interface UserMapper {
    User findById(Integer id);
}
```

### 3. Mapper XML 文件
resources/mapper/UserMapper.xml
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="com.mapper.UserMapper">
    <select id="findById" resultType="User">
        select * from user where id = #{id}
    </select>
</mapper>
```

### 4. Service 层使用
```java
@Service
public class UserServiceImpl {

    @Autowired
    private UserMapper userMapper;

    public User getUserById(Integer id){
        return userMapper.findById(id);
    }
}
```

---

## 10.6 方式二：注解开发（无XML）
直接在 Mapper 接口写 SQL，省略 XML 文件
```java
@Mapper
public interface UserMapper {

    @Select("select * from user where id = #{id}")
    User findById(Integer id);

    @Insert("insert into user(username,password) values(#{username},#{password})")
    int addUser(User user);
}
```

---

## 10.7 新增、删除、修改 & 事务
1. 增删改直接使用对应注解：
   `@Insert` 、`@Update` 、`@Delete`
2. 业务层添加 `@Transactional` 开启事务控制
```java
@Service
@Transactional
public class UserService {
    // 多条DML自动事务管理
}
```

---

## 10.8 常用拓展配置
### 1. 开启日志打印SQL
```yaml
mybatis:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

### 2. 整合分页插件 PageHelper
引入依赖 + 自动配置，即可快速实现分页，无需手写 limit

---

## 10.9 Spring vs SpringBoot 整合对比
1. **SSM 整合**
- 手动配置：DataSource、SqlSessionFactoryBean、Mapper扫描
- 大量XML配置，繁琐笨重
2. **SpringBoot 整合**
- 自动装配，starter一键依赖
- 仅需配置数据库连接信息
- 极简配置，开发效率极高

---

## 10.10 本章核心总结
1. 核心依赖：`mybatis-spring-boot-starter`
2. 关键注解：`@MapperScan` 扫描持久层接口
3. 配置文件：仅需配置数据源 + MyBatis基础参数
4. 双模式兼容：XML复杂SQL、注解简单CRUD
5. 完全融入SpringBoot事务、IOC容器
6. 企业主流开发方案：**SpringBoot + MyBatis**

---

