# 附录B-高频踩坑故障汇总手册
> 🎯 覆盖Spring全家桶+微服务90%的线上常见底层问题，按组件分类，每个故障提供错误现象 → 根源分析 → 修复方案 → 长期规避规范

---

## 目录
1. [Spring Framework 核心故障](#1-spring-framework-核心故障)
2. [Spring MVC 故障](#2-spring-mvc-故障)
3. [Spring Boot 故障](#3-spring-boot-故障)
4. [Spring Data 故障](#4-spring-data-故障)
5. [Spring Security 故障](#5-spring-security-故障)
6. [Spring Cloud 故障](#6-spring-cloud-故障)
7. [分布式中间件故障](#7-分布式中间件故障)
8. [性能与运维故障](#8-性能与运维故障)

---

## 1. Spring Framework 核心故障

### 1.1 循环依赖 - BeanCurrentlyInCreationException

**错误现象**
```
Error creating bean with name 'a': Requested bean is currently in creation: 
Is there an unresolvable circular reference?
```

**根源分析**
A依赖B，B依赖A，且是**构造器注入**。Spring三级缓存只能解决setter注入的循环依赖，构造器注入的循环依赖无法解决。

**修复方案**
```java
// ❌ 错误：构造器循环依赖
@Service
public class A {
    public A(B b) { } // A构造器依赖B
}
@Service
public class B {
    public B(A a) { } // B构造器依赖A → 死循环
}

// ✅ 方案1：改为setter注入
@Service
public class A {
    @Autowired private B b; // 先实例化A → 注入B时从三级缓存获取A的引用
}

// ✅ 方案2：@Lazy延迟加载
@Service
public class A {
    public A(@Lazy B b) { } // @Lazy使Spring先给代理，B在真正调用时才创建
}

// ✅ 方案3（推荐）：重构代码消除循环依赖
// 引入中间层或事件机制解耦
```

**长期规避**
- 编码规范：避免Service层互相依赖，通过事件/消息解耦
- 使用构造器注入时注意检查依赖关系图
- CI集成循环依赖检测工具

---

### 1.2 @Transactional事务不生效

**错误现象**
方法报异常但数据没有回滚。

**场景1：同类内部调用**
```java
// ❌ this调用绕过代理
public void register() {
    doSave(); // this调用，@Transactional不生效
}
@Transactional
public void doSave() { ... }

// ✅ 拆分类或注入自己
@Autowired private UserService self;
public void register() {
    self.doSave(); // 通过代理调用
}
```

**场景2：异常被捕获** → 必须抛出或手动setRollbackOnly

**场景3：rollbackFor不匹配** → 默认只回滚RuntimeException，需添加`rollbackFor = Exception.class`

**场景4：方法非public** → Spring AOP只能代理public方法

**场景5：数据库引擎不支持** → MySQL MyISAM不支持事务，使用InnoDB

---

### 1.3 @Autowired注入为null

**错误现象**
`NullPointerException`，注入的Service为null。

**根源分析**
- 在构造器中使用@Autowired字段（注入发生在实例化之后）
- 自己new的对象没有经过Spring容器管理
- Bean没有被Spring扫描到（ComponentScan路径不对）

**修复方案**
```java
// ✅ 构造器注入（字段在构造时已有值）
private final UserService userService;
public OrderService(UserService userService) {
    this.userService = userService;
    userService.doSomething(); // 安全使用
}
```

---

### 1.4 AOP切面不生效

**常见原因清单**
| # | 原因 | 检查方法 |
|---|------|----------|
| 1 | 切面类没有加`@Component` | 检查切面是否被Spring管理 |
| 2 | 同类内部方法调用 | `this.method()` → 绕过代理 |
| 3 | 方法是private/final/static | AOP只代理public非final方法 |
| 4 | `@Around`忘记调`proceed()` | 目标方法不会执行 |
| 5 | 扫描路径不对 | @ComponentScan是否包含切面所在包 |

---

## 2. Spring MVC 故障

### 2.1 POST请求参数为null

**原因**：使用`@RequestParam`接收JSON Body。
```java
// ❌ 错误
@PostMapping("/user")
public void create(@RequestParam UserDTO dto) { } // JSON Body用@RequestParam

// ✅ 正确
@PostMapping("/user")
public void create(@RequestBody UserDTO dto) { } // JSON Body用@RequestBody
```

### 2.2 文件上传413 Request Entity Too Large
**解决**：配置`spring.servlet.multipart.max-file-size`和`max-request-size`

### 2.3 CORS跨域预检失败
**解决**：拦截器放行OPTIONS请求 + 配置CORS全局配置

---

## 3. Spring Boot 故障

### 3.1 启动报错"Unable to start web server"

**常见原因**
- 端口被占用（`netstat -ano | findstr 8080`）
- 缺少Web容器依赖
- Context路径冲突

### 3.2 自动装配不生效

**排查步骤**：
```yaml
logging:
  level:
    org.springframework.boot.autoconfigure: DEBUG
# 查看启动日志中 Positive matches / Negative matches
```

### 3.3 不同配置文件优先级混乱

**优先级（从高到低）**：命令行参数 > 环境变量 > application-{profile}.yml > application.yml

---

## 4. Spring Data 故障

### 4.1 JPA：N+1查询问题

```java
// ❌ N+1：查1条Order → 100条OrderItem → 101次SQL
@Entity public class Order {
    @OneToMany(fetch = FetchType.EAGER) // 默认EAGER，每次查Order都联查Item
    private List<OrderItem> items;
}

// ✅ 方案1：改为LAZY + @EntityGraph按需加载
@OneToMany(fetch = FetchType.LAZY)
private List<OrderItem> items;

@EntityGraph(attributePaths = "items")
List<Order> findAll(); // 仅在需要时联查

// ✅ 方案2：使用JOIN FETCH JPQL
@Query("SELECT o FROM Order o JOIN FETCH o.items WHERE o.userId = :userId")
List<Order> findByUserId(@Param("userId") Long userId);
```

### 4.2 Redis：序列化乱码

```java
// ❌ 默认JDK序列化 → Redis中存的是二进制乱码
// ✅ 使用Jackson2JsonRedisSerializer或StringRedisSerializer
@Bean
public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(factory);
    template.setKeySerializer(new StringRedisSerializer());
    template.setValueSerializer(new Jackson2JsonRedisSerializer<>(Object.class));
    return template;
}
```

---

## 5. Spring Security 故障

### 5.1 permitAll()接口仍然401

**原因**：`permitAll()`只跳过认证检查，但如果请求经过了Security过滤器链中的其他过滤器仍可能被拦截。

### 5.2 JWT过期没有合理处理

**方案**：双Token机制（Access Token 短期 + Refresh Token 长期）

### 5.3 PasswordEncoder未配置
```
java.lang.IllegalArgumentException: There is no PasswordEncoder mapped for the id "null"
```
**解决**：配置`BCryptPasswordEncoder` Bean

---

## 6. Spring Cloud 故障

### 6.1 Nacos服务不注册

**排查清单**：
1. Nacos Server是否启动（访问 http://localhost:8848/nacos）
2. `@EnableDiscoveryClient`是否添加
3. `spring.cloud.nacos.discovery.server-addr`配置是否正确
4. 网络是否通（ping localhost 8848）
5. 检查Nacos控制台-服务列表

### 6.2 Feign调用超时/500

**常见原因**：
- 连接超时/读超时设置过短
- 被调用服务不存在或未注册到Nacos
- 服务名拼写错误（`@FeignClient(name = "user-service")` 必须与Nacos中的服务名一致）
- Feign请求头丢失（如Authorization）

**请求头传递方案**：
```java
@Configuration
public class FeignConfig {
    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            ServletRequestAttributes attrs = (ServletRequestAttributes) 
                RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                String token = attrs.getRequest().getHeader("Authorization");
                requestTemplate.header("Authorization", token);
            }
        };
    }
}
```

### 6.3 Gateway 503 Service Unavailable

**排查清单**：
1. 目标服务是否已注册到Nacos
2. 服务名大小写是否正确（lb://user-service）
3. Gateway的webflux依赖冲突

### 6.4 Sentinel规则不持久化

**解决**：将规则推送到Nacos持久化，重启后不丢失。

### 6.5 Seata分布式事务不回滚

**排查清单**：
1. `@GlobalTransactional`是否正确标注
2. undo_log表是否存在
3. seata-server是否启动
4. 各服务的seata配置是否正确

---

## 7. 分布式中间件故障

### 7.1 Redis缓存穿透/击穿/雪崩

| 问题 | 现象 | 方案 |
|------|------|------|
| **穿透** | 查不存在的key，请求打到DB | 布隆过滤器 + 缓存空值 |
| **击穿** | 热点key过期，大量请求打到DB | 互斥锁 + 逻辑过期 |
| **雪崩** | 大量key同时过期，DB被打垮 | 随机过期时间 + 多级缓存 + 限流 |

### 7.2 MQ消息丢失

**RabbitMQ消息可靠性方案**：
- 生产者：confirm模式 + 消息落库 + 定时补偿
- Broker：持久化（持久化交换机 + 持久化队列 + 持久化消息）
- 消费者：手动ACK + 消费成功后确认

### 7.3 消息重复消费（幂等性问题）

```java
// 方案：消费前检查Redis中的消费记录
@RabbitListener(queues = "order.queue")
public void handle(OrderMessage msg) {
    String key = "msg:consumed:" + msg.getMessageId();
    Boolean exists = redisTemplate.opsForValue().setIfAbsent(key, "1", 24, TimeUnit.HOURS);
    if (Boolean.FALSE.equals(exists)) {
        log.warn("重复消息，跳过: {}", msg.getMessageId());
        return; // 已消费过，直接返回
    }
    // 处理业务逻辑...
}
```

---

## 8. 性能与运维故障

### 8.1 接口响应慢排查步骤

```
1. 网络层：ping、telnet检查网络延迟
2. 应用层：Arthas trace追踪方法耗时
3. 数据库：慢SQL日志分析（explain看执行计划、是否有索引）
4. 缓存：检查缓存命中率
5. 外部调用：Feign超时、MQ延迟
6. GC：jstat -gc查看GC频率和耗时
7. 线程：jstack查看线程状态（BLOCKED/WAITING）
```

### 8.2 CPU飙升排查

```bash
# 1. 找到CPU最高的Java进程
top -H -p <pid>

# 2. 线程ID转十六进制
printf '%x\n' <tid>

# 3. 查看线程堆栈
jstack <pid> | grep <hex_tid> -A 30

# 4. 定位到具体代码行
# 常见原因：死循环、正则回溯、频繁GC
```

### 8.3 内存溢出OOM

```bash
# 启动时添加参数，OOM时自动dump
-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp/heapdump.hprof

# 使用MAT分析dump文件：
# 1. 查看Dominator Tree → 定位最大对象
# 2. 查看Leak Suspects → 自动分析泄漏嫌疑
# 3. 查看Histogram → 按类统计对象数量
```

### 8.4 数据库连接池耗尽

```
HikariPool-1 - Connection is not available, request timed out after 30000ms
```

**排查**：检查是否有事务未提交/未关闭的长时间持有连接、连接池大小是否需要调整、是否有慢SQL阻塞连接释放

---

> 🎯 **建议**：将此手册作为团队知识库，每个线上新增故障通过Post-Mortem流程补充进来，持续积累。
