# 附录A-核心注解与配置速查表
> 🎯 Spring全家桶全模块核心注解、配置参数速查，面试前快速过一遍

---

## 目录
1. [Spring Framework 核心注解](#1-spring-framework-核心注解)
2. [Spring MVC 注解](#2-spring-mvc-注解)
3. [Spring Boot 注解与配置](#3-spring-boot-注解与配置)
4. [Spring Data 注解](#4-spring-data-注解)
5. [Spring Security 注解](#5-spring-security-注解)
6. [Spring Cloud 注解](#6-spring-cloud-注解)
7. [配置参数速查](#7-配置参数速查)

---

## 1. Spring Framework 核心注解

### IoC 容器

| 注解 | 作用 | 使用位置 |
|------|------|----------|
| `@Component` | 通用组件，纳入Spring管理 | 类 |
| `@Service` | 业务逻辑层组件 | Service类 |
| `@Repository` | 数据访问层组件（含异常翻译） | DAO类 |
| `@Controller` | 控制器层组件 | Controller类 |
| `@RestController` | = @Controller + @ResponseBody | REST Controller类 |
| `@Configuration` | 标识配置类 | 配置类 |
| `@Bean` | 声明Bean | @Configuration类的方法 |
| `@Autowired` | 按类型注入 | 字段/构造器/setter |
| `@Qualifier` | 按名称指定注入Bean | 配合@Autowired |
| `@Primary` | 优先注入 | 类 |
| `@Resource` | JSR-250，按名称注入 | 字段/setter |
| `@Scope` | 指定Bean作用域 | 类 |
| `@PostConstruct` | 初始化回调 | 方法 |
| `@PreDestroy` | 销毁回调 | 方法 |
| `@Lazy` | 延迟初始化 | 类/方法 |
| `@Conditional` | 条件注册Bean | 类/方法 |
| `@ConditionalOnClass` | 类存在时注册 | Spring Boot |
| `@ConditionalOnMissingBean` | Bean不存在时注册 | Spring Boot |
| `@ConditionalOnProperty` | 配置属性匹配时注册 | Spring Boot |
| `@Import` | 导入其他配置类 | 配置类 |
| `@ComponentScan` | 指定扫描包路径 | 配置类 |
| `@PropertySource` | 加载properties文件 | 配置类 |
| `@Value` | 注入配置值 | 字段 |
| `@ConfigurationProperties` | 类型安全绑定配置 | 类 |
| `@EnableConfigurationProperties` | 启用@ConfigurationProperties | 配置类 |

### AOP 切面

| 注解 | 作用 |
|------|------|
| `@Aspect` | 标识切面类 |
| `@Pointcut` | 定义切点 |
| `@Before` | 前置通知 |
| `@After` | 后置通知（finally） |
| `@AfterReturning` | 返回通知 |
| `@AfterThrowing` | 异常通知 |
| `@Around` | 环绕通知 |
| `@Order` | 切面执行顺序（值越小越先） |

### 事务

| 注解 | 作用 |
|------|------|
| `@Transactional` | 声明式事务 |
| `@EnableTransactionManagement` | 开启事务管理 |
| `propagation` | 传播行为（REQUIRED/REQUIRES_NEW/NESTED等） |
| `isolation` | 隔离级别 |
| `rollbackFor` | 回滚异常类型 |
| `noRollbackFor` | 不回滚异常类型 |
| `readOnly` | 只读事务 |
| `timeout` | 超时时间（秒） |

---

## 2. Spring MVC 注解

| 注解 | 作用 |
|------|------|
| `@RequestMapping` | 通用请求映射 |
| `@GetMapping` | GET请求映射 |
| `@PostMapping` | POST请求映射 |
| `@PutMapping` | PUT请求映射 |
| `@DeleteMapping` | DELETE请求映射 |
| `@PatchMapping` | PATCH请求映射 |
| `@PathVariable` | 绑定路径参数 |
| `@RequestParam` | 绑定查询参数 |
| `@RequestBody` | 绑定请求体（JSON） |
| `@RequestHeader` | 绑定请求头 |
| `@CookieValue` | 绑定Cookie |
| `@ResponseBody` | 返回JSON |
| `@ResponseStatus` | 指定HTTP状态码 |
| `@ControllerAdvice` | 全局异常处理/数据绑定 |
| `@RestControllerAdvice` | = @ControllerAdvice + @ResponseBody |
| `@ExceptionHandler` | 异常处理方法 |
| `@InitBinder` | 数据绑定初始化 |
| `@ModelAttribute` | 模型属性 |
| `@CrossOrigin` | 跨域配置 |
| `@Valid` | 触发参数校验 |
| `@Validated` | Spring校验分组 |
| `@NotBlank` | 字符串非空校验 |
| `@NotNull` | 非null校验 |
| `@NotEmpty` | 集合非空校验 |
| `@Size` | 长度限制 |
| `@Email` | 邮箱格式 |
| `@Min` / `@Max` | 数值范围 |
| `@Pattern` | 正则匹配 |
| `@JsonFormat` | JSON日期格式 |
| `@DateTimeFormat` | 请求参数日期格式 |

---

## 3. Spring Boot 注解与配置

| 注解 | 作用 |
|------|------|
| `@SpringBootApplication` | = @Configuration + @EnableAutoConfiguration + @ComponentScan |
| `@EnableAutoConfiguration` | 开启自动装配 |
| `@SpringBootConfiguration` | = @Configuration |
| `@ServletComponentScan` | 扫描@WebFilter/@WebServlet |
| `@ConfigurationPropertiesScan` | 扫描@ConfigurationProperties |
| `@EnableScheduling` | 开启定时任务 |
| `@EnableAsync` | 开启异步 |
| `@EnableCaching` | 开启缓存 |

### 常用配置参数

```yaml
# 服务器
server:
  port: 8080
  servlet.context-path: /api

# 数据源
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/db
    username: root
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000

  # JPA
  jpa:
    hibernate.ddl-auto: update
    show-sql: true

  # Redis
  redis:
    host: localhost
    port: 6379
    lettuce.pool.max-active: 8

  # 文件上传
  servlet.multipart:
    max-file-size: 10MB
    max-request-size: 100MB

# 日志
logging:
  level:
    root: INFO
    com.example: DEBUG

# Actuator
management:
  endpoints.web.exposure.include: health,info,metrics
  endpoint.health.show-details: always
```

---

## 4. Spring Data 注解

| 注解 | 作用 |
|------|------|
| `@Entity` | JPA实体 |
| `@Table` | 指定表名 |
| `@Id` | 主键 |
| `@GeneratedValue` | 主键生成策略 |
| `@Column` | 列映射 |
| `@OneToMany` | 一对多 |
| `@ManyToOne` | 多对一 |
| `@ManyToMany` | 多对多 |
| `@JoinColumn` | 外键 |
| `@Query` | 自定义JPQL/SQL |
| `@Modifying` | 修改操作配合@Query |
| `@Transactional` | 事务 |
| `@Cacheable` | 缓存结果 |
| `@CacheEvict` | 清除缓存 |
| `@CachePut` | 更新缓存 |
| `@Caching` | 组合缓存操作 |
| `@EnableCaching` | 开启缓存 |
| `@Repository` | DAO组件 |
| `@NoRepositoryBean` | 中间Repository |

---

## 5. Spring Security 注解

| 注解 | 作用 |
|------|------|
| `@EnableWebSecurity` | 开启Web安全 |
| `@EnableMethodSecurity` | 开启方法级安全（替代@EnableGlobalMethodSecurity） |
| `@PreAuthorize` | 方法执行前权限校验 |
| `@PostAuthorize` | 方法执行后权限校验 |
| `@Secured` | JSR-250，角色校验 |
| `@RolesAllowed` | JSR-250，允许角色 |
| `@AuthenticationPrincipal` | 获取当前用户 |

---

## 6. Spring Cloud 注解

| 注解 | 作用 |
|------|------|
| `@EnableDiscoveryClient` | 开启服务发现 |
| `@EnableFeignClients` | 开启Feign客户端 |
| `@FeignClient` | 声明Feign客户端接口 |
| `@RefreshScope` | 配置动态刷新 |
| `@SentinelResource` | Sentinel资源定义 |
| `@GlobalTransactional` | Seata分布式事务 |
| `@SpringCloudApplication` | = @SpringBootApplication + @EnableDiscoveryClient + @EnableCircuitBreaker |
| `@LoadBalanced` | RestTemplate负载均衡 |

---

## 7. 配置参数速查

### Nacos
```yaml
spring.cloud.nacos.discovery.server-addr: localhost:8848
spring.cloud.nacos.config.server-addr: localhost:8848
spring.cloud.nacos.config.file-extension: yaml
```

### Sentinel
```yaml
spring.cloud.sentinel.transport.dashboard: localhost:8080
spring.cloud.sentinel.transport.port: 8719
feign.sentinel.enabled: true
```

### Gateway
```yaml
spring.cloud.gateway.routes[0].id: user-service
spring.cloud.gateway.routes[0].uri: lb://user-service
spring.cloud.gateway.routes[0].predicates[0]: Path=/api/users/**
```

### Feign
```yaml
spring.cloud.openfeign.client.config.default.connectTimeout: 5000
spring.cloud.openfeign.client.config.default.readTimeout: 5000
feign.client.config.default.loggerLevel: BASIC
```

### Seata
```yaml
seata.tx-service-group: my_tx_group
seata.service.vgroup-mapping.my_tx_group: default
seata.service.grouplist.default: 127.0.0.1:8091
```

### Redisson
```yaml
spring.redis.redisson.config: |
  singleServerConfig:
    address: "redis://127.0.0.1:6379"
    connectionMinimumIdleSize: 8
```

### 定时任务
```yaml
spring.task.scheduling.pool.size: 5
spring.task.scheduling.thread-name-prefix: task-
```

---

> 🎯 **使用建议**：本速查表适合面试前快速回顾和日常开发查阅。每个注解的具体用法请参考对应章节的详细讲解。
