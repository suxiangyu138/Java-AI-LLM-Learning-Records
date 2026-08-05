# 06 - Spring Boot 整合实战

> 🎯 Spring Boot + MyBatis 是 Java 后端最主流的组合。自动配置、多数据源、事务管理、Mapper 扫描——掌握这四项，日常开发游刃有余

---

## 目录

1. [快速集成](#1-快速集成)
2. [多数据源配置](#2-多数据源配置)
3. [事务管理](#3-事务管理)

---

## 1. 快速集成

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.mybatis.spring.boot</groupId>
    <artifactId>mybatis-spring-boot-starter</artifactId>
    <version>3.0.3</version>
</dependency>
```

```yaml
# application.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mydb?useSSL=false
    username: root
    password: root

mybatis:
  mapper-locations: classpath:mapper/**/*.xml   # XML 位置
  type-aliases-package: com.example.entity      # 别名包
  configuration:
    map-underscore-to-camel-case: true           # 下划线转驼峰
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

```java
// Mapper 接口
@Mapper  // 或启动类加 @MapperScan("com.example.mapper")
public interface UserMapper {
    User findById(Long id);
    int insert(User user);
}
```

### 配置对比

| 方式 | 优点 | 缺点 |
|------|------|------|
| **注解 @Select** | 简单直观，SQL 和代码在一起 | 复杂 SQL 难维护 |
| **XML 映射** | 复杂 SQL 清晰，支持动态 SQL | 文件分离，跳转不便 |
| **混合** | 简单用注解，复杂用 XML | **推荐** ✅ |

## 2. 多数据源配置

```java
@Configuration
public class DataSourceConfig {

    @Primary
    @Bean(name = "masterDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.master")
    public DataSource masterDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "slaveDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.slave")
    public DataSource slaveDataSource() {
        return DataSourceBuilder.create().build();
    }

    // 多数据源必须手动配置 SqlSessionFactory
    @Bean(name = "masterSqlSessionFactory")
    public SqlSessionFactory masterSqlSessionFactory(
            @Qualifier("masterDataSource") DataSource ds) throws Exception {
        SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
        bean.setDataSource(ds);
        bean.setMapperLocations(new PathMatchingResourcePatternResolver()
            .getResources("classpath:mapper/master/**/*.xml"));
        return bean.getObject();
    }
    // slaveSqlSessionFactory 类似...
}
```

```yaml
spring:
  datasource:
    master:
      url: jdbc:mysql://master:3306/db
    slave:
      url: jdbc:mysql://slave:3306/db
```

## 3. 事务管理

```java
// Spring 事务 + MyBatis = @Transactional
@Service
public class UserService {
    private final UserMapper userMapper;

    @Transactional(rollbackFor = Exception.class)  // 任何异常都回滚
    public void transfer(Long fromId, Long toId, BigDecimal amount) {
        userMapper.decreaseBalance(fromId, amount);  // SQL 1
        // 如果这里抛异常 → SQL 1 也回滚
        userMapper.increaseBalance(toId, amount);    // SQL 2
    }

    // 事务传播
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logTransfer(Long fromId, Long toId, BigDecimal amount) {
        // 独立事务，不受外部事务影响
    }
}
```

```text
MyBatis + Spring 事务原理：
  @Transactional → AOP 代理
  → 开启事务 → sqlSession.commit() 或 rollback()
  → SqlSession 生命周期绑定到 Spring 事务

关键：同一个事务内的 Mapper 调用共享同一个 SqlSession
   → 一级缓存在事务内持续有效
```

## 核心要点回顾

- `@MapperScan` 替代每个 Mapper 上的 `@Mapper`
- `map-underscore-to-camel-case: true` 自动列名下划线转驼峰
- 多数据源 = 手动配置 2 套 SqlSessionFactory + DataSource
- `@Transactional(rollbackFor = Exception.class)` 不要省略
- 简单 SQL 用注解，复杂 SQL 用 XML

## 参考资料

1. MyBatis-Spring-Boot-Starter 文档
2. Spring 事务管理文档
