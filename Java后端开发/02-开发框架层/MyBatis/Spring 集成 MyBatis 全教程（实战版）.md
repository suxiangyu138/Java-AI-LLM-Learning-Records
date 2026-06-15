# Spring 集成 MyBatis 全教程（实战版）

> **文档定位**：Java 后端技术参考文档 | Spring 集成 MyBatis 完整指南  
> **核心桥梁**：`mybatis-spring` 组件将 MyBatis 核心对象交给 Spring 容器管理  
> **两种方式**：传统 XML 配置（SSM 项目）+ Spring Boot Java 配置（推荐）

---

## 目录

- [一、集成核心前提与依赖](#一集成核心前提与依赖)
- [二、传统 Spring + MyBatis（XML 配置）](#二传统-spring--mybatisxml-配置)
- [三、Spring Boot + MyBatis（Java 配置，推荐）](#三spring-boot--mybatisjava-配置推荐)
- [四、关键注意事项](#四关键注意事项)
- [五、常见问题排查](#五常见问题排查)

---

## 一、集成核心前提与依赖

### 集成原理

```
Spring 管理 DataSource → SqlSessionFactoryBean 创建 SqlSessionFactory 
→ SqlSessionTemplate（线程安全）→ MapperScannerConfigurer 自动扫描 Mapper
```

### 传统 Spring + MyBatis 依赖

```xml
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-context</artifactId>
    <version>5.3.28</version>
</dependency>
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-jdbc</artifactId>
    <version>5.3.28</version>
</dependency>
<dependency>
    <groupId>org.mybatis</groupId>
    <artifactId>mybatis</artifactId>
    <version>3.5.16</version>
</dependency>
<!-- 核心桥梁 -->
<dependency>
    <groupId>org.mybatis</groupId>
    <artifactId>mybatis-spring</artifactId>
    <version>2.1.6</version>
</dependency>
```

### Spring Boot + MyBatis 依赖（推荐）

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>2.7.18</version>
</parent>

<dependency>
    <groupId>org.mybatis.spring.boot</groupId>
    <artifactId>mybatis-spring-boot-starter</artifactId>
    <version>2.3.2</version>
</dependency>
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <scope>runtime</scope>
</dependency>
```

---

## 二、传统 Spring + MyBatis（XML 配置）

### 步骤 1：配置数据源

```xml
<bean id="dataSource" class="com.alibaba.druid.pool.DruidDataSource">
    <property name="driverClassName" value="com.mysql.cj.jdbc.Driver"/>
    <property name="url" value="jdbc:mysql://localhost:3306/test_db?useSSL=false&serverTimezone=UTC"/>
    <property name="username" value="root"/>
    <property name="password" value="123456"/>
</bean>
```

### 步骤 2：配置 SqlSessionFactoryBean

```xml
<bean id="sqlSessionFactory" class="org.mybatis.spring.SqlSessionFactoryBean">
    <property name="dataSource" ref="dataSource"/>
    <property name="configLocation" value="classpath:mybatis-config.xml"/>
    <property name="mapperLocations" value="classpath:mapper/*.xml"/>
    <property name="typeAliasesPackage" value="com.example.entity"/>
</bean>
```

### 步骤 3：配置 Mapper 扫描器

```xml
<bean class="org.mybatis.spring.mapper.MapperScannerConfigurer">
    <property name="basePackage" value="com.example.mapper"/>
    <property name="sqlSessionFactoryBeanName" value="sqlSessionFactory"/>
</bean>
```

### 步骤 4：MyBatis 全局配置（可选）

```xml
<!-- mybatis-config.xml -->
<configuration>
    <settings>
        <setting name="logImpl" value="STDOUT_LOGGING"/>
        <setting name="mapUnderscoreToCamelCase" value="true"/>
    </settings>
    <plugins>
        <plugin interceptor="com.github.pagehelper.PageInterceptor">
            <property name="helperDialect" value="mysql"/>
        </plugin>
    </plugins>
</configuration>
```

### 步骤 5：业务代码

```java
@Service
public class UserService {
    @Autowired
    private UserMapper userMapper;  // Spring 自动注入

    public User getUserById(Long id) { return userMapper.selectById(id); }
    public int addUser(User user) { return userMapper.insert(user); }
}
```

### 步骤 6：测试

```java
ApplicationContext context = new ClassPathXmlApplicationContext("applicationContext.xml");
UserService userService = context.getBean(UserService.class);
User user = userService.getUserById(1L);
```

---

## 三、Spring Boot + MyBatis（Java 配置，推荐）

### application.yml

```yaml
spring:
  datasource:
    type: com.alibaba.druid.pool.DruidDataSource
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/test_db?useSSL=false&serverTimezone=UTC&characterEncoding=utf8
    username: root
    password: 123456
    druid:
      initial-size: 5
      max-active: 20

mybatis:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: com.example.entity
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

### 启动类

```java
@SpringBootApplication
@MapperScan("com.example.mapper")
public class MyBatisSpringBootApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyBatisSpringBootApplication.class, args);
    }
}
```

### 测试

```java
@SpringBootTest
public class MyBatisSpringBootTest {
    @Autowired
    private UserService userService;

    @Test
    public void testSelectById() {
        User user = userService.getUserById(1L);
        System.out.println(user);
    }
}
```

---

## 四、关键注意事项

| 注意点 | 说明 |
|--------|------|
| **namespace 关联** | Mapper XML 的 namespace 必须与 Mapper 接口全路径一致 |
| **数据源配置** | MySQL 8.0+ 驱动为 `com.mysql.cj.jdbc.Driver`，URL 需加 `serverTimezone=UTC` |
| **Mapper 扫描路径** | `@MapperScan` 或 `MapperScannerConfigurer` 的包路径必须包含所有 Mapper |
| **驼峰映射** | 开启后 `user_name` → `userName`，避免手动配置 resultMap |
| **SqlSession 线程安全** | Spring 通过 `SqlSessionTemplate` + ThreadLocal 保证 |
| **插件兼容性** | 分页插件等需确保与 MyBatis、Spring 版本兼容 |
| **自动配置失效** | 检查是否引入 starter、配置了 mapper-locations、添加了 @MapperScan |

---

## 五、常见问题排查

| 问题 | 原因 | 解决 |
|------|------|------|
| **Invalid bound statement** | namespace/id 不匹配或路径错误 | 检查三点：namespace、标签 id、mapper-locations |
| **No qualifying bean** | Mapper 未被扫描 | 添加 @MapperScan 或在接口上加 @Mapper |
| **Communications link failure** | 数据库连接失败 | 检查 MySQL 服务、URL、驱动类、serverTimezone |

---

## 总结

| 方式 | 适用场景 | 核心配置 |
|------|----------|----------|
| **传统 XML 配置** | SSM 项目 | applicationContext.xml + MapperScannerConfigurer |
| **Spring Boot** | Spring Boot 项目（推荐） | application.yml + @MapperScan |

> 两种方式核心逻辑一致：**通过 mybatis-spring 将 MyBatis 核心对象交给 Spring 管理**。
