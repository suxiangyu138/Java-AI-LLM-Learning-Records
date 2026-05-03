# 第9章 Spring 集成 MyBatis
## 9.1 集成目的
1. 手动 MyBatis 存在问题
    - 手动创建 `SqlSessionFactory`、`SqlSession`，代码冗余
    - 事务手动提交、回滚，维护繁琐
    - 数据库连接、硬编码、耦合度高
2. Spring 整合优势
    - 由 **Spring IOC 容器** 统一管理：数据源、SqlSessionFactory、Mapper
    - 整合 Spring 声明式事务，无需手动操作事务
    - 简化配置、代码解耦、适配项目开发规范

---

## 9.2 核心依赖
Spring 整合 MyBatis 必须引入**整合专用包**：
```xml
<!-- Spring 核心 -->
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-context</artifactId>
    <version>5.3.20</version>
</dependency>
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-jdbc</artifactId>
    <version>5.3.20</version>
</dependency>

<!-- MyBatis -->
<dependency>
    <groupId>org.mybatis</groupId>
    <artifactId>mybatis</artifactId>
    <version>3.5.13</version>
</dependency>

<!-- Spring 与 MyBatis 整合包【核心】 -->
<dependency>
    <groupId>org.mybatis</groupId>
    <artifactId>mybatis-spring</artifactId>
    <version>2.0.7</version>
</dependency>

<!-- MySQL驱动 + 连接池 -->
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <version>8.0.33</version>
</dependency>
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>druid</artifactId>
    <version>1.2.16</version>
</dependency>
```

---

## 9.3 整合核心组件
1. **DataSource**
   数据源，数据库连接池，替代 MyBatis 内置 `POOLED` 数据源
2. **SqlSessionFactoryBean**
   Spring 提供，用于创建 `SqlSessionFactory`，替代原生构建方式
3. **MapperScannerConfigurer**
   动态扫描 Mapper 接口，自动创建 Mapper 代理对象，注入 IOC 容器
4. **SqlSessionTemplate**
   Spring 封装的 SqlSession，线程安全，替代原生 SqlSession

---

## 9.4 完整整合步骤（XML 配置版）
### 步骤1：数据库配置文件 jdbc.properties
```properties
jdbc.driver=com.mysql.cj.jdbc.Driver
jdbc.url=jdbc:mysql://localhost:3306/mybatis_test?serverTimezone=UTC&useSSL=false
jdbc.username=root
jdbc.password=你的密码
```

### 步骤2：Spring 核心配置文件 applicationContext.xml
```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:context="http://www.springframework.org/schema/context"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="
       http://www.springframework.org/schema/beans
       http://www.springframework.org/schema/beans/spring-beans.xsd
       http://www.springframework.org/schema/context
       http://www.springframework.org/schema/context/spring-context.xsd">

    <!-- 1. 加载外部配置文件 -->
    <context:property-placeholder location="classpath:jdbc.properties"/>

    <!-- 2. 配置数据源 Druid -->
    <bean id="dataSource" class="com.alibaba.druid.pool.DruidDataSource">
        <property name="driverClassName" value="${jdbc.driver}"/>
        <property name="url" value="${jdbc.url}"/>
        <property name="username" value="${jdbc.username}"/>
        <property name="password" value="${jdbc.password}"/>
    </bean>

    <!-- 3. 配置 SqlSessionFactoryBean：整合 MyBatis -->
    <bean id="sqlSessionFactory" class="org.mybatis.spring.SqlSessionFactoryBean">
        <!-- 注入数据源 -->
        <property name="dataSource" ref="dataSource"/>
        <!-- 绑定 MyBatis 全局配置文件 -->
        <property name="configLocation" value="classpath:mybatis-config.xml"/>
        <!-- 绑定 Mapper XML 映射文件 -->
        <property name="mapperLocations" value="classpath:mapper/*.xml"/>
    </bean>

    <!-- 4. 扫描 Mapper 接口，自动生成代理对象 -->
    <bean class="org.mybatis.spring.mapper.MapperScannerConfigurer">
        <!-- 指定 Mapper 接口所在包 -->
        <property name="basePackage" value="com.mapper"/>
    </bean>

</beans>
```

### 步骤3：简化 MyBatis 全局配置文件
整合后，`mybatis-config.xml` 只保留基础设置，**无需配置数据源、mapper 映射**：
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE configuration
        PUBLIC "-//mybatis.org//DTD Config 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-config.dtd">
<configuration>
    <!-- 全局设置：驼峰命名转换、日志、缓存等 -->
    <settings>
        <setting name="mapUnderscoreToCamelCase" value="true"/>
    </settings>
    
    <!-- 别名配置 -->
    <typeAliases>
        <package name="com.pojo"/>
    </typeAliases>
</configuration>
```

---

## 9.5 业务层使用（完全解耦）
### 1. Service 层注入 Mapper
```java
@Service
public class UserServiceImpl implements UserService {

    // 直接注入 Mapper 代理对象
    @Autowired
    private UserMapper userMapper;

    @Override
    public User findUserById(Integer id) {
        return userMapper.findById(id);
    }
}
```

### 2. 测试类
```java
public class SpringMyBatisTest {
    public static void main(String[] args) {
        ApplicationContext context = new ClassPathXmlApplicationContext("applicationContext.xml");
        UserService userService = context.getBean(UserService.class);
        User user = userService.findUserById(1);
        System.out.println(user);
    }
}
```

---

## 9.6 整合事务
Spring 接管事务，只需开启**事务注解**：
1. 配置文件添加事务管理器
```xml
<!-- 事务管理器 -->
<bean id="transactionManager" class="org.springframework.jdbc.datasource.DataSourceTransactionManager">
    <property name="dataSource" ref="dataSource"/>
</bean>
<!-- 开启事务注解驱动 -->
<tx:annotation-driven transaction-manager="transactionManager"/>
```
2. Service 方法添加 `@Transactional`
```java
@Transactional
public void addUser() {
    // 多条DML操作，自动事务控制
}
```

---

## 9.7 整合核心要点
1. 核心容器：`SqlSessionFactoryBean` 替代原生 `SqlSessionFactoryBuilder`
2. 数据源：由 Spring 连接池管理，脱离 MyBatis 配置
3. Mapper 接口：通过 `MapperScannerConfigurer` 自动扫描、自动注入
4. 会话对象：底层使用 `SqlSessionTemplate`，线程安全，可在全局共用
5. 配置分离：
    - Spring：数据源、事务、IOC、扫描
    - MyBatis：别名、全局设置、动态SQL、映射文件

---

## 9.8 原生MyBatis 与 Spring整合区别
1. 原生：手动创建工厂、会话，手动关闭资源、手动提交事务
2. 整合版：Spring 容器统一管理资源，自动创建、回收、事务控制
3. 开发模式：整合后遵循 **三层架构（Controller-Service-Dao）** 标准开发

---

## 9.9 本章总结
1. Spring 整合 MyBatis 的核心依赖：`mybatis-spring`
2. 三大核心Bean：数据源、SqlSessionFactoryBean、Mapper扫描器
3. 配置分工：Spring管连接与事务，MyBatis管SQL与映射
4. 整合后无需手动操作 SqlSession，直接注入 Mapper 使用
5. 无缝衔接 Spring 事务，满足企业项目开发标准

