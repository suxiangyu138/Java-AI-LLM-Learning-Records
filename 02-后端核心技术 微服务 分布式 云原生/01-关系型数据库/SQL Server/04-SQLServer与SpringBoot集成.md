# 04 - SQL Server 与 Spring Boot 集成

> 定位：MSSQL JDBC 驱动配置、JPA 方言、连接池、存储过程调用、Windows 集成认证、多数据源——Java 后端连接 SQL Server 的完整实践

## 📚 目录

1. [驱动与基础配置](#1-驱动与基础配置)
2. [JPA 适配](#2-jpa-适配)
3. [存储过程调用](#3-存储过程调用)
4. [Windows 集成认证](#4-windows-集成认证)
5. [多数据源](#5-多数据源)

---

## 1. 驱动与基础配置

```xml
<dependency>
    <groupId>com.microsoft.sqlserver</groupId>
    <artifactId>mssql-jdbc</artifactId>
    <!-- ⚠️ 版本跟随 Spring Boot BOM -->
</dependency>
```

```yaml
spring:
  datasource:
    url: jdbc:sqlserver://localhost:1433;databaseName=myapp
    # ⚠️ 连接参数（; 分隔）
    url: jdbc:sqlserver://localhost:1433;database=myapp;
         encrypt=true;trustServerCertificate=true;
         applicationIntent=ReadOnly      # 只读路由（Always On 场景）
    username: ${DB_USER}
    password: ${DB_PASSWORD}
    driver-class-name: com.microsoft.sqlserver.jdbc.SQLServerDriver

    hikari:
      maximum-pool-size: 20
      connection-timeout: 30000

  jpa:
    database-platform: org.hibernate.dialect.SQLServerDialect
    hibernate:
      ddl-auto: validate          # ⚠️ 生产用 validate
```

### 1.1 JDBC URL 参数速查

| 参数 | 作用 |
|------|------|
| encrypt | 强制加密（Azure 必开） |
| trustServerCertificate | 跳过证书验证（开发环境） |
| applicationIntent | ReadOnly（Always On 只读路由） |
| databaseName | 数据库名 |
| integratedSecurity | Windows 集成认证 |

---

## 2. JPA 适配

### 2.1 映射注解

```java
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                         // ⚠️ SQL Server IDENTITY = 自增

    @Column(name = "user_name", length = 50)
    private String name;

    // NVARCHAR 映射（SQL Server 的 Unicode 列）
    @Column(name = "description", columnDefinition = "NVARCHAR(500)")
    private String description;

    // ⚠️ SQL Server 特有类型映射
    @Column(name = "is_active", columnDefinition = "BIT DEFAULT 1")
    private Boolean isActive;                // BIT → Boolean

    // DATETIME2 代替 DATETIME（更高精度）
    @Column(name = "created_at", columnDefinition = "DATETIME2")
    private LocalDateTime createdAt;

    // UNIQUEIDENTIFIER → UUID mapping
    @Column(name = "guid", columnDefinition = "UNIQUEIDENTIFIER")
    private UUID guid;
}
```

### 2.2 命名策略

```java
// ⚠️ SQL Server 默认不区分大小写
// Spring Boot 默认驼峰→下划线映射无需额外配置
// 实体名 User → 表名 user

// 若需自定义
@Configuration
public class JpaConfig {
    @Bean
    public PhysicalNamingStrategy namingStrategy() {
        return new PhysicalNamingStrategyStandardImpl();
    }
}
```

---

## 3. 存储过程调用

SQL Server 项目中大量使用存储过程（政企场景普遍），Java 侧三种调用方式：

```java
// ① JPA @Procedure 注解
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @Procedure(name = "usp_get_users_by_dept")
    List<User> getUsersByDept(@Param("deptId") Long deptId);
}

// ② EntityManager 原生调用
@PersistenceContext
private EntityManager em;

public List<User> callProcedure(Long deptId) {
    StoredProcedureQuery sp = em.createStoredProcedureQuery("usp_get_users");
    sp.registerStoredProcedureParameter("deptId", Long.class, ParameterMode.IN);
    sp.setParameter("deptId", deptId);
    return sp.getResultList();
}

// ③ JdbcTemplate（最灵活）
@Autowired
private JdbcTemplate jdbc;

public List<User> callProc(Long deptId) {
    SimpleJdbcCall call = new SimpleJdbcCall(jdbc)
        .withProcedureName("usp_get_users")
        .returningResultSet("result", BeanPropertyRowMapper.newInstance(User.class));
    Map<String, Object> result = call.execute(Map.of("deptId", deptId));
    return (List<User>) result.get("result");
}
```

> 🎯 **要点**：政企 SQL Server 项目存储过程占比高——JdbcTemplate 的 `SimpleJdbcCall` 最灵活（支持 OUTPUT 参数、多个结果集）。

---

## 4. Windows 集成认证

```yaml
# ① JDBC URL 启用集成认证（免密码）
spring:
  datasource:
    url: jdbc:sqlserver://localhost:1433;database=myapp;
         integratedSecurity=true;authenticationScheme=NTLM

# ② 需加载 DLL 到 JVM（sqljdbc_auth.dll）
# 或使用 pureJava 模式（新驱动支持）
# url: ...;integratedSecurity=true;authentication=ActiveDirectoryIntegrated
```

```bash
# Java 启动参数
java -Djava.library.path=/path/to/dll -jar app.jar
```

> 🎯 **要点**：Windows 集成认证 = 域账户登录 SQL Server（政务/国企使用 Active Directory 的统一凭证）。Java 侧需 sqljdbc_auth.dll 或 ActiveDirectoryIntegrated 模式。

---

## 5. 多数据源

```java
// SQL Server 多数据源（数据库拆分 / 读写分离场景）
@Configuration
public class DataSourceConfig {

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource.primary")
    public DataSource primaryDs() { return DataSourceBuilder.create().build(); }

    @Bean
    @ConfigurationProperties("spring.datasource.secondary")
    public DataSource secondaryDs() { return DataSourceBuilder.create().build(); }
}

// ⚠️ SQL Server 读写分离建议
//   方案一：Always On 可读副本（数据库层自动路由）
//   方案二：应用层 @DS 注解（dynamic-datasource）
//   Always On 优先（对应用透明）
```

```yaml
spring:
  datasource:
    primary:
      url: jdbc:sqlserver://master:1433;database=myapp
    secondary:
      url: jdbc:sqlserver://slave:1433;database=myapp
```

---

> 🎯 **核心要点**：SQL Server + Spring Boot = **MSSQL JDBC 驱动**（Windows 认证 + 加密参数）+ **JPA 类型映射**（IDENTITY 自增/BIT Boolean/DATETIME2）+ **存储过程调用**（SimpleJdbcCall 最灵活）+ **Always On**（读写分离对应用透明）。类型映射（BIT → Boolean）是最高频踩坑点。

---

**返回总览**：[00-SQLServer总览与核心概念](00-SQLServer总览与核心概念.md) | **上一篇**：[03-SQLServer事务与并发控制](03-SQLServer事务与并发控制.md) | **下一篇**：[05-SQLServer运维与高可用](05-SQLServer运维与高可用.md)
