# Spring 整合 MyBatis 详解

> **文档定位**：Java 后端技术参考文档 | 传统 Spring + MyBatis 整合  
> **整合意义**：让 Spring 接管 MyBatis 核心组件（SqlSessionFactory、SqlSession、Mapper），实现依赖注入和声明式事务管理  
> **整合优势**：无需手动创建 SqlSession，Mapper 可直接注入，事务由 Spring 统一控制  
> **与前文关系**：无缝衔接 MyBatis 基础（复用实体类、Mapper 接口）

---

## 目录

- [一、整合概述](#一整合概述)
- [二、环境准备](#二环境准备)
- [三、核心配置（applicationContext.xml）](#三核心配置applicationcontextxml)
- [四、声明式事务管理](#四声明式事务管理)
- [五、实战测试](#五实战测试)
- [六、常见问题与注意事项](#六常见问题与注意事项)

---

## 一、整合概述

### 整合前后对比

| 对比 | 纯 MyBatis | Spring + MyBatis |
|------|-----------|-----------------|
| SqlSession 管理 | 手动创建和关闭 | Spring 自动管理 |
| Mapper 获取 | `sqlSession.getMapper()` | `@Autowired` 注入 |
| 事务管理 | `sqlSession.commit()` 手动控制 | `@Transactional` 声明式 |
| 依赖管理 | 耦合 MyBatis API | 依赖注入，组件解耦 |

### 核心 3 步

```
1. 导入依赖（Spring + MyBatis + mybatis-spring）
2. 配置 Spring 核心文件（数据源、SqlSessionFactory、Mapper 扫描器）
3. 编写代码（Mapper 直接用 @Autowired 注入）
```

---

## 二、环境准备

### Maven 依赖

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
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-aop</artifactId>
    <version>5.3.20</version>
</dependency>
<dependency>
    <groupId>org.aspectj</groupId>
    <artifactId>aspectjweaver</artifactId>
    <version>1.9.9.1</version>
</dependency>

<!-- MyBatis 核心 -->
<dependency>
    <groupId>org.mybatis</groupId>
    <artifactId>mybatis</artifactId>
    <version>3.5.9</version>
</dependency>

<!-- Spring + MyBatis 整合包（核心桥梁） -->
<dependency>
    <groupId>org.mybatis</groupId>
    <artifactId>mybatis-spring</artifactId>
    <version>2.0.7</version>
</dependency>

<!-- 数据库驱动 + 连接池 -->
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <version>8.0.30</version>
</dependency>
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>druid</artifactId>
    <version>1.2.16</version>
</dependency>

<!-- 测试 -->
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-test</artifactId>
    <version>5.3.20</version>
    <scope>test</scope>
</dependency>
```

### db.properties

```properties
jdbc.driver=com.mysql.cj.jdbc.Driver
jdbc.url=jdbc:mysql://localhost:3306/mybatis_db?serverTimezone=UTC&useSSL=false
jdbc.username=root
jdbc.password=123456
```

---

## 三、核心配置（applicationContext.xml）

### 完整配置

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:context="http://www.springframework.org/schema/context"
       xmlns:tx="http://www.springframework.org/schema/tx"
       xmlns:aop="http://www.springframework.org/schema/aop"
       xsi:schemaLocation="...">

    <!-- 1. 加载数据库配置 -->
    <context:property-placeholder location="classpath:db.properties"/>

    <!-- 2. 配置数据源（Druid 连接池）-->
    <bean id="dataSource" class="com.alibaba.druid.pool.DruidDataSource">
        <property name="driverClassName" value="${jdbc.driver}"/>
        <property name="url" value="${jdbc.url}"/>
        <property name="username" value="${jdbc.username}"/>
        <property name="password" value="${jdbc.password}"/>
    </bean>

    <!-- 3. 配置 SqlSessionFactory（核心！）-->
    <bean id="sqlSessionFactory" class="org.mybatis.spring.SqlSessionFactoryBean">
        <property name="dataSource" ref="dataSource"/>
        <property name="configLocation" value="classpath:mybatis-config.xml"/>
        <property name="typeAliasesPackage" value="com.example.pojo"/>
        <property name="mapperLocations" value="classpath:com/example/mapper/*.xml"/>
    </bean>

    <!-- 4. 配置 Mapper 扫描器（核心！无需手动注入 Mapper）-->
    <bean class="org.mybatis.spring.mapper.MapperScannerConfigurer">
        <property name="basePackage" value="com.example.mapper"/>
        <property name="sqlSessionFactoryBeanName" value="sqlSessionFactory"/>
    </bean>

    <!-- 5. 配置事务管理器 -->
    <bean id="transactionManager" class="org.springframework.jdbc.datasource.DataSourceTransactionManager">
        <property name="dataSource" ref="dataSource"/>
    </bean>

    <!-- 6. 开启事务注解驱动 -->
    <tx:annotation-driven transaction-manager="transactionManager"/>

    <!-- 7. 开启 Spring 注解扫描（Service 层） -->
    <context:component-scan base-package="com.example.service"/>
</beans>
```

### 配置要点速查

| 配置项 | 作用 |
|--------|------|
| `context:property-placeholder` | 加载 db.properties |
| `dataSource` Bean | 数据源（推荐 Druid） |
| `SqlSessionFactoryBean` | 创建 SqlSessionFactory |
| `MapperScannerConfigurer` | 自动扫描 Mapper 接口 → 生成代理对象 |
| `DataSourceTransactionManager` | 事务管理器 |
| `tx:annotation-driven` | 开启 `@Transactional` 注解 |

---

## 四、声明式事务管理

### 注解方式（推荐）

```java
@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserMapper userMapper;

    @Transactional(readOnly = true)  // 只读事务（查询）
    @Override
    public User findById(Integer id) {
        return userMapper.findById(id);
    }

    @Transactional  // 读写事务（增删改，异常自动回滚）
    @Override
    public int addUser(User user) {
        return userMapper.addUser(user);
    }
}
```

---

## 五、实战测试

```java
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:applicationContext.xml")
public class SpringMyBatisTest {

    @Autowired
    private UserService userService;

    @Test
    public void testFindById() {
        User user = userService.findById(1);
        System.out.println("查询结果：" + user);
    }

    @Test
    public void testAddUser() {
        User user = new User("test", "123456", 26, "test@163.com");
        int rows = userService.addUser(user);
        System.out.println("新增成功，受影响行数：" + rows);
    }
}
```

---

## 六、常见问题与注意事项

| 问题 | 原因 | 解决 |
|------|------|------|
| **Mapper 无法注入** | MapperScannerConfigurer 包路径错误 | 核对 `basePackage` 路径 |
| **事务不生效** | 未开启 `tx:annotation-driven` | 检查事务管理器 + `@Transactional` |
| **二级缓存失效** | 未开启全局缓存 / 实体类未序列化 | 检查配置 + 实体类实现 `Serializable` |
| **数据库连接失败** | db.properties 配置错误 | 核对 URL、用户名、密码 |

---

## 核心总结

| 4 个核心配置 | 说明 |
|-------------|------|
| **数据源** | Spring 管理，推荐 Druid |
| **SqlSessionFactory** | 关联数据源 + MyBatis 配置 |
| **Mapper 扫描器** | 自动扫描 → 生成代理 → IOC 容器 |
| **声明式事务** | `@Transactional` 注解，无需手动控制 |

> 后续可结合 Spring MVC 实现 **SSM 三层架构**（Spring MVC + Spring + MyBatis）。
