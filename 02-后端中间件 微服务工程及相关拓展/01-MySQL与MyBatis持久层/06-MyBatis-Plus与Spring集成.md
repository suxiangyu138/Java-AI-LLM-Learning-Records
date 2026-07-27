# MyBatis-Plus 与 Spring 集成
> MyBatis-Plus 是 MyBatis 的增强工具，只做增强不做修改。Spring 与 Spring Boot 提供了完善的 MyBatis 集成支持。本文涵盖 Spring 集成 MyBatis、Spring Boot 集成 MyBatis、MyBatis-Plus 核心功能及实战用法。

## 目录
1. [Spring 集成 MyBatis](#1-spring-集成-mybatis)
2. [Spring Boot 集成 MyBatis](#2-spring-boot-集成-mybatis)
3. [MyBatis-Plus 概述](#3-mybatis-plus-概述)
4. [BaseMapper 与 Service 封装](#4-basemapper-与-service-封装)
5. [条件构造器 LambdaQueryWrapper](#5-条件构造器-lambdaquerywrapper)
6. [分页查询](#6-分页查询)
7. [逻辑删除](#7-逻辑删除)
8. [自动填充](#8-自动填充)
9. [主键策略](#9-主键策略)
10. [常用配置与最佳实践](#10-常用配置与最佳实践)

---

## 1. Spring 集成 MyBatis

### 1.1 Maven 依赖

```xml
<dependencies>
    <!-- Spring + MyBatis 整合包 -->
    <dependency>
        <groupId>org.mybatis</groupId>
        <artifactId>mybatis-spring</artifactId>
        <version>2.1.2</version>
    </dependency>
    <!-- MyBatis -->
    <dependency>
        <groupId>org.mybatis</groupId>
        <artifactId>mybatis</artifactId>
        <version>3.5.16</version>
    </dependency>
    <!-- 连接池 -->
    <dependency>
        <groupId>com.zaxxer</groupId>
        <artifactId>HikariCP</artifactId>
        <version>5.0.1</version>
    </dependency>
    <!-- MySQL 驱动 -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <version>8.0.33</version>
    </dependency>
</dependencies>
```

### 1.2 Spring XML 配置

```xml
<!-- 1. 数据源 -->
<bean id="dataSource" class="com.zaxxer.hikari.HikariDataSource">
    <property name="driverClassName" value="com.mysql.cj.jdbc.Driver"/>
    <property name="jdbcUrl" value="jdbc:mysql://localhost:3306/mydb"/>
    <property name="username" value="root"/>
    <property name="password" value="123456"/>
</bean>

<!-- 2. SqlSessionFactory -->
<bean id="sqlSessionFactory" class="org.mybatis.spring.SqlSessionFactoryBean">
    <property name="dataSource" ref="dataSource"/>
    <property name="configLocation" value="classpath:mybatis-config.xml"/>
    <property name="mapperLocations" value="classpath:mapper/**/*.xml"/>
</bean>

<!-- 3. Mapper 扫描 -->
<bean class="org.mybatis.spring.mapper.MapperScannerConfigurer">
    <property name="basePackage" value="com.example.mapper"/>
</bean>

<!-- 4. 事务管理器 -->
<bean id="transactionManager" class="org.springframework.jdbc.datasource.DataSourceTransactionManager">
    <property name="dataSource" ref="dataSource"/>
</bean>

<!-- 5. 开启注解事务 -->
<tx:annotation-driven/>
```

### 1.3 Java 配置方式（推荐）

```java
@Configuration
@MapperScan("com.example.mapper")
@EnableTransactionManagement
public class MyBatisConfig {

    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://localhost:3306/mydb");
        config.setUsername("root");
        config.setPassword("123456");
        config.setMaximumPoolSize(20);
        return new HikariDataSource(config);
    }

    @Bean
    public SqlSessionFactoryBean sqlSessionFactory(DataSource dataSource) {
        SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setTypeAliasesPackage("com.example.entity");
        factory.setMapperLocations(new PathMatchingResourcePatternResolver()
                .getResources("classpath:mapper/**/*.xml"));
        return factory;
    }

    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
```

---

## 2. Spring Boot 集成 MyBatis

### 2.1 Maven 依赖

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>2.7.18</version>
</parent>

<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <!-- MyBatis Spring Boot Starter -->
    <dependency>
        <groupId>org.mybatis.spring.boot</groupId>
        <artifactId>mybatis-spring-boot-starter</artifactId>
        <version>2.3.2</version>
    </dependency>
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <scope>runtime</scope>
    </dependency>
</dependencies>
```

### 2.2 application.yml 配置

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mydb?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8mb4
    username: root
    password: 123456
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      idle-timeout: 600000
      connection-timeout: 30000
      max-lifetime: 1800000

mybatis:
  mapper-locations: classpath:mapper/**/*.xml
  type-aliases-package: com.example.entity
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

### 2.3 Mapper 扫描

```java
@SpringBootApplication
@MapperScan("com.example.mapper")   // 扫描 Mapper 接口
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### 2.4 Service 层使用

```java
@Service
@Transactional
public class UserServiceImpl implements UserService {
    @Autowired
    private UserMapper userMapper;

    public User findById(Long id) {
        return userMapper.selectById(id);
    }

    public void createUser(User user) {
        userMapper.insert(user);
    }
}
```

---

## 3. MyBatis-Plus 概述

MyBatis-Plus（简称 MP）是 MyBatis 的增强工具，**只做增强不做修改**——你仍然可以用 MyBatis 的一切能力，MP 帮你省掉重复的 CRUD SQL。

### 3.1 Spring Boot 集成 MP

```xml
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-boot-starter</artifactId>
    <version>3.5.5</version>
</dependency>
```

```yaml
mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
    map-underscore-to-camel-case: true
  global-config:
    db-config:
      id-type: assign_id
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
  mapper-locations: classpath:/mapper/**/*.xml
```

---

## 4. BaseMapper 与 Service 封装

### 4.1 Mapper 继承 BaseMapper

```java
@Mapper
public interface UserMapper extends BaseMapper<User> {
    // 继承后自带以下方法，不需要写任何 SQL
    // insert, deleteById, delete, updateById, update, selectById,
    // selectList, selectCount, selectPage, selectOne, selectBatchIds...
}

// 使用
@Autowired
private UserMapper userMapper;

userMapper.insert(user);
userMapper.selectById(1L);
userMapper.selectList(null);            // 查询全部
userMapper.selectCount(null);           // 计数
userMapper.selectBatchIds(Arrays.asList(1L, 2L, 3L));
```

### 4.2 Service 层封装

```java
public interface UserService extends IService<User> { }

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    // 继承后自带 CRUD 方法
}

// 使用
userService.save(user);                    // 单个保存
userService.saveBatch(list);               // 批量保存
userService.saveOrUpdate(user);            // 有 ID 则更新，无则插入
userService.removeById(1L);                // 删除
userService.updateById(user);              // 更新
userService.getById(1L);                   // 查询
userService.listByIds(ids);                // 批量查
userService.page(page, wrapper);           // 分页
```

---

## 5. 条件构造器 LambdaQueryWrapper

### 5.1 基本用法

```java
// 传统方式：字段名写死（容易出错）
QueryWrapper<User> qw = new QueryWrapper<>();
qw.eq("username", "zhangsan");

// Lambda 方式：编译器检查（推荐）
LambdaQueryWrapper<User> lqw = new LambdaQueryWrapper<>();
lqw.eq(User::getUsername, "zhangsan");
```

### 5.2 常用条件

```java
LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
    .eq(User::getStatus, "ACTIVE")           // 等于
    .ne(User::getDeleted, 1)                 // 不等于
    .gt(User::getAge, 18)                    // 大于
    .ge(User::getAge, 18)                    // 大于等于
    .lt(User::getAge, 60)                    // 小于
    .le(User::getAge, 60)                    // 小于等于
    .like(User::getUsername, "张")           // 模糊
    .notLike(User::getUsername, "测试")      // 不包含
    .in(User::getId, Arrays.asList(1, 2, 3)) // IN
    .notIn(User::getId, ids)                 // NOT IN
    .between(User::getAge, 18, 30)           // 范围
    .isNull(User::getEmail)                  // IS NULL
    .isNotNull(User::getPhone)               // IS NOT NULL
    .orderByDesc(User::getCreatedAt)         // 降序
    .orderByAsc(User::getId)                 // 升序
    .last("LIMIT 10");                       // 尾部追加 SQL

// 多条件组合
List<User> users = userMapper.selectList(wrapper);
```

### 5.3 LambdaUpdateWrapper

```java
LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<User>()
    .set(User::getStatus, "INACTIVE")        // SET status = 'INACTIVE'
    .set(User::getUpdatedAt, LocalDateTime.now())
    .eq(User::getId, 1L);                    // WHERE id = 1

userMapper.update(null, wrapper);            // 第一个参数 null，只更新 set 字段
```

---

## 6. 分页查询

### 6.1 配置分页插件

```java
@Configuration
public class MybatisPlusConfig {
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(
            new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
```

### 6.2 使用分页

```java
// Controller
@GetMapping("/users")
public Result<IPage<UserVO>> list(@RequestParam(defaultValue = "1") int page,
                                   @RequestParam(defaultValue = "10") int size) {
    Page<User> pageParam = new Page<>(page, size);
    IPage<UserVO> result = userService.pageUsers(pageParam);
    return Result.ok(result);
}

// Service
public IPage<UserVO> pageUsers(Page<User> page) {
    LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
        .eq(User::getStatus, 1)
        .orderByDesc(User::getCreatedAt);
    return userMapper.selectPage(page, wrapper);
    // Page 对象包含：records, total, pages, current, size
}
```

---

## 7. 逻辑删除

### 7.1 配置

```yaml
mybatis-plus:
  global-config:
    db-config:
      logic-delete-field: deleted        # 逻辑删除字段
      logic-delete-value: 1              # 已删除
      logic-not-delete-value: 0          # 未删除
```

### 7.2 实体类

```java
public class User {
    @TableLogic
    private Integer deleted;
}
```

### 7.3 使用效果

```java
// 物理删除变为逻辑删除
userMapper.deleteById(1L);
// 实际执行：UPDATE t_user SET deleted=1 WHERE id=1

// 查询自动加条件
userMapper.selectList(null);
// 实际执行：SELECT * FROM t_user WHERE deleted=0
```

---

## 8. 自动填充

### 8.1 实现 MetaObjectHandler

```java
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {
    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createdAt",
            LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updatedAt",
            LocalDateTime.class, LocalDateTime.now());
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updatedAt",
            LocalDateTime.class, LocalDateTime.now());
    }
}
```

### 8.2 实体类标注

```java
public class User {
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
```

---

## 9. 主键策略

```java
public class User {
    @TableId(type = IdType.AUTO)         // 数据库自增
    private Long id;
}

@TableId(type = IdType.ASSIGN_ID)        // 雪花算法（默认）推荐
@TableId(type = IdType.AUTO)             // 数据库自增
@TableId(type = IdType.INPUT)            // 手动输入
@TableId(type = IdType.ASSIGN_UUID)      // UUID（去横线）
```

> 💡 **推荐 `ASSIGN_ID`（雪花算法）**——全局唯一、有序递增、适合分布式环境。

---

## 10. 常用配置与最佳实践

### 10.1 完整配置

```yaml
mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl  # SQL 日志
    map-underscore-to-camel-case: true                      # 下划线转驼峰
    cache-enabled: false                                     # 关闭二级缓存
  global-config:
    db-config:
      id-type: assign_id                                    # 主键策略
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
      table-prefix: t_                                      # 表前缀
  mapper-locations: classpath:/mapper/**/*.xml
  type-aliases-package: com.example.entity
```

### 10.2 最佳实践

| 实践 | 说明 |
|------|------|
| 简单 CRUD 用 BaseMapper | 无需写 SQL，继承即用 |
| 复杂查询用 XML | 长 SQL 写在 XML 中，可读性更好 |
| 条件用 LambdaQueryWrapper | 类型安全，避免字段名拼写错误 |
| Service 层继承 IService | 自带批量操作、分页等方法 |
| 分页用 PaginationInnerInterceptor | 物理分页，性能优于内存分页 |
| 逻辑删除 | 数据不可丢，用 `@TableLogic` |
| 枚举用通用枚举 | `@EnumValue` 注解，避免魔法值 |

> 🎯 **MyBatis-Plus 的核心价值**：省掉 80% 的重复 CRUD 代码，让你专注于业务逻辑和复杂 SQL 的编写。
