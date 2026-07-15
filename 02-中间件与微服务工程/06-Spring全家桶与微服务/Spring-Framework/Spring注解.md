# Spring 注解速查

> **定位**：Spring 注解 7 大分类速查——IoC/DI、AOP、事务、MVC、条件、异步定时、缓存事件。

---

## 1. IoC/DI 注解

### Bean 定义

| 注解 | 用途 | 层级 |
|------|------|:--:|
| `@Component` | 通用 Bean | 任意 |
| `@Controller` | MVC 控制器 | Web |
| `@Service` | 业务逻辑层 | Service |
| `@Repository` | 数据访问层 | DAO |
| `@Configuration` | 配置类（替代 XML） | — |
| `@Bean` | 方法返回值注册为 Bean | — |
| `@ComponentScan` | 指定扫描包路径 | — |
| `@Scope` | Bean 作用域 | 默认 `singleton` |
| `@Lazy` | 延迟初始化 | — |

### 依赖注入

| 注解 | 匹配方式 | 位置 |
|------|----------|------|
| `@Autowired` | 按类型 | 字段/构造/setter |
| `@Qualifier` | 按名称（配合 `@Autowired`） | — |
| `@Resource` | 默认按名称 | JDK 自带 |
| `@Value` | 注入基本类型/配置文件值 | — |
| `@PropertySource` | 加载外部配置文件 | — |

### 注入方式推荐

```java
// ✅ 推荐：构造方法注入
private final OrderService orderService;
public UserService(OrderService orderService) {
    this.orderService = orderService;
}
```

---

## 2. AOP 注解

| 注解 | 说明 |
|------|------|
| `@Aspect` | 标识切面类 |
| `@Pointcut` | 定义切点表达式 |
| `@Before` | 前置通知 |
| `@After` | 后置通知（无论异常） |
| `@AfterReturning` | 返回通知（正常执行） |
| `@AfterThrowing` | 异常通知 |
| `@Around` | 环绕通知（最灵活） |

### 通知执行顺序

```text
正常：@Before → 目标方法 → @AfterReturning → @After
异常：@Before → 目标方法(异常) → @AfterThrowing → @After
```

---

## 3. 事务注解

| 注解 | 属性 |
|------|------|
| `@Transactional` | `propagation`（传播）/ `isolation`（隔离）/ `rollbackFor` |
| `@EnableTransactionManagement` | 开启声明式事务 |

### 传播行为

| 值 | 说明 |
|----|------|
| `REQUIRED`（默认） | 有则加入，无则新建 |
| `REQUIRES_NEW` | 新建事务，暂停当前 |
| `SUPPORTS` | 有则加入，无非事务 |
| `NESTED` | 嵌套事务 |

---

## 4. MVC / Web 注解

| 注解 | 说明 |
|------|------|
| `@RestController` | `@Controller` + `@ResponseBody` |
| `@RequestMapping` | 映射 HTTP 请求 |
| `@GetMapping` / `@PostMapping` / `@PutMapping` / `@DeleteMapping` | 简化映射 |
| `@PathVariable` | URL 路径参数 |
| `@RequestParam` | 查询参数 |
| `@RequestBody` | POST 请求体（JSON） |
| `@RequestHeader` | 请求头参数 |
| `@ExceptionHandler` | 控制器异常处理 |
| `@RestControllerAdvice` | 全局异常处理 |

---

## 5. 条件注解

| 注解 | 条件 |
|------|------|
| `@ConditionalOnClass` | 类路径存在指定类 |
| `@ConditionalOnMissingBean` | 容器中不存在指定 Bean |
| `@ConditionalOnProperty` | 配置文件属性匹配 |
| `@ConditionalOnWebApplication` | Web 环境 |

---

## 6. 异步/定时

| 注解 | 说明 |
|------|------|
| `@Async` + `@EnableAsync` | 异步执行 |
| `@Scheduled` + `@EnableScheduling` | 定时任务（fixedRate/cron） |

### Cron 示例

```java
@Scheduled(cron = "0 0 0 * * ?")   // 每天凌晨
@Scheduled(fixedRate = 5000)        // 每 5 秒（固定速率）
@Scheduled(fixedDelay = 10000)      // 上次结束后 10 秒
```

---

## 7. 缓存/事件

| 注解 | 说明 |
|------|------|
| `@Cacheable` | 查缓存 → 无则执行方法并缓存 |
| `@CachePut` | 执行方法并更新缓存 |
| `@CacheEvict` | 清除缓存 |
| `@EventListener` | 监听 Spring 事件 |
