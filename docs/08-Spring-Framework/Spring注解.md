03.15 23:17
Spring注解
--------------------------------------------------------------------------------------------------------------------------------------
一、Spring注解核心分类
1. 核心容器注解（IoC/DI相关）：控制Bean的创建、依赖注入，是Spring最基础的注解
2. 面向切面编程（AOP）注解：实现切面、通知、切点等AOP核心功能
3. 事务管理注解：声明式事务控制，简化事务操作
4. Web相关注解（Spring Web/Spring MVC）：处理HTTP请求、参数绑定、响应等
5. 条件注解：根据条件动态注册Bean
6. 异步/定时任务注解：实现异步执行、定时调度
7. 其他扩展注解：如配置属性、缓存、事件等
--------------------------------------------------------------------------------------------------------------------------------------
二、核心容器注解（IoC/DI）
1. Bean定义注解
① @Component：通用注解，标记类为Spring管理的Bean，适用于任意层
② @Controller：标注MVC的控制器层（处理HTTP请求），@Component的特例
③ @Service：标注业务逻辑层（Service层），@Component的特例
④ @Repository：标注数据访问层（DAO/Mapper层），@Component的特例，支持持久化异常转换
- 示例：
@Service
public class UserService {
    // 业务逻辑代码
}
⑤ @Configuration：标注类为配置类，替代XML配置文件，类中可定义@Bean方法
⑥ @Bean：标注在方法上，将方法返回值注册为Spring Bean，常用于第三方类的Bean定义
- 示例：
@Configuration
public class AppConfig {
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
⑦ @ComponentScan：指定Spring扫描Bean的包路径，替代XML中的<context:component-scan>
- 示例：
@Configuration
@ComponentScan("com.example.demo") // 扫描指定包及其子包下的@Component注解类
public class AppConfig {}
⑧ @Scope：指定Bean的作用域，默认单例（singleton）
- 常用值：
  - singleton：单例（默认），整个应用只有一个实例
  - prototype：原型，每次获取Bean创建新实例
  - request：HTTP请求级别，每个请求一个实例（Web环境）
  - session：HTTP会话级别，每个会话一个实例（Web环境）
- 示例：
@Service
@Scope("prototype")
public class UserService {}
⑨ @Lazy：延迟初始化Bean，默认单例Bean在容器启动时创建，加此注解后首次获取时创建
- 示例：
@Service
@Lazy
public class UserService {}
2. 依赖注入注解
① @Autowired：自动注入依赖Bean，按类型匹配，默认要求依赖必须存在
- 用法：标注在字段、构造方法、setter方法上
- 示例：
@Service
public class UserService {
    // 字段注入（简洁，推荐构造方法注入）
    @Autowired
    private UserMapper userMapper;
    // 构造方法注入（推荐，支持依赖不可变，便于测试）
    private final OrderService orderService;
    @Autowired
    public UserService(OrderService orderService) {
        this.orderService = orderService;
    }
    // setter方法注入
    private LogService logService;
    @Autowired
    public void setLogService(LogService logService) {
        this.logService = logService;
    }
}
② @Qualifier：配合@Autowired使用，按Bean名称匹配，解决同类型多个Bean的注入问题
- 示例：
@Service("userServiceV1")
public class UserServiceV1 implements UserService {}
@Service("userServiceV2")
public class UserServiceV2 implements UserService {}
@Controller
public class UserController {
    @Autowired
    @Qualifier("userServiceV1") // 指定注入名称为userServiceV1的Bean
    private UserService userService;
}
③ @Resource：JDK自带注解，按名称匹配注入（默认），也可按类型，替代@Autowired+@Qualifier
- 示例：
@Controller
public class UserController {
    @Resource(name = "userServiceV2") // 按名称注入
    private UserService userService;
}
④ @Value：注入基本类型/字符串值，支持SpEL表达式（Spring表达式语言），常用于读取配置
- 示例：
@Service
public class UserService {
    // 注入常量
    @Value("10")
    private int pageSize;
    // 注入配置文件中的值（需配置PropertySource）
    @Value("${app.name}")
    private String appName;
    // SpEL表达式注入
    @Value("#{ T(java.lang.Math).random() * 100 }")
    private double randomNum;
}
⑤ @PropertySource：加载外部配置文件（如.properties/.yml），配合@Value使用
- 示例：
@Configuration
@PropertySource("classpath:application.properties") // 加载类路径下的配置文件
public class AppConfig {}
--------------------------------------------------------------------------------------------------------------------------------------
三、AOP相关注解
1. @Aspect：标注类为切面类，需配合@Component使类成为Bean
2. @Pointcut：定义切点表达式，复用切点规则
- 常用切点表达式：
  - execution：匹配方法执行（最常用），如execution(* com.example.service.*.*(..))
  - within：匹配指定包/类下的所有方法
  - @annotation：匹配标注指定注解的方法
- 示例：
@Aspect
@Component
public class LogAspect {
    // 定义切点：匹配com.example.service包下所有类的所有方法
    @Pointcut("execution(* com.example.service.*.*(..))")
    public void servicePointcut() {}
}
3. 通知注解（标注在切面类的方法上）
① @Before：前置通知，目标方法执行前执行
② @After：后置通知，目标方法执行后执行（无论是否异常）
③ @AfterReturning：返回通知，目标方法正常返回后执行，可获取返回值
④ @AfterThrowing：异常通知，目标方法抛出异常后执行，可获取异常信息
⑤ @Around：环绕通知，包裹目标方法，可控制目标方法的执行时机、参数、返回值，功能最强
- 示例：
@Aspect
@Component
public class LogAspect {
    @Pointcut("execution(* com.example.service.UserService.*(..))")
    public void userServicePointcut() {}
    // 前置通知
    @Before("userServicePointcut()")
    public void beforeAdvice(JoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().getName();
        System.out.println("前置通知：方法" + methodName + "开始执行");
    }
    // 返回通知
    @AfterReturning(value = "userServicePointcut()", returning = "result")
    public void afterReturningAdvice(JoinPoint joinPoint, Object result) {
        String methodName = joinPoint.getSignature().getName();
        System.out.println("返回通知：方法" + methodName + "返回值：" + result);
    }
    // 异常通知
    @AfterThrowing(value = "userServicePointcut()", throwing = "e")
    public void afterThrowingAdvice(JoinPoint joinPoint, Exception e) {
        String methodName = joinPoint.getSignature().getName();
        System.out.println("异常通知：方法" + methodName + "抛出异常：" + e.getMessage());
    }
    // 环绕通知
    @Around("userServicePointcut()")
    public Object aroundAdvice(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            // 执行目标方法
            Object result = joinPoint.proceed();
            long end = System.currentTimeMillis();
            System.out.println("环绕通知：方法执行耗时" + (end - start) + "ms");
            return result;
        } catch (Throwable e) {
            System.out.println("环绕通知：方法执行异常：" + e.getMessage());
            throw e;
        }
    }
}
--------------------------------------------------------------------------------------------------------------------------------------
四、事务管理注解
1. @Transactional：声明式事务核心注解，标注在类/方法上，实现事务控制
- 标注在类上：该类所有public方法都应用事务规则
- 标注在方法上：覆盖类级别的事务规则（优先级更高）
- 核心属性：
  - propagation：事务传播行为（默认REQUIRED）
    - REQUIRED：如果当前有事务，加入事务；无则新建
    - REQUIRES_NEW：新建事务，暂停当前事务（如有）
    - SUPPORTS：有事务则加入，无则非事务执行
    - NOT_SUPPORTED：非事务执行，暂停当前事务（如有）
    - NEVER：非事务执行，有事务则抛异常
    - MANDATORY：必须在事务中执行，无则抛异常
    - NESTED：嵌套事务，依赖主事务
  - isolation：事务隔离级别（默认DEFAULT，使用数据库默认）
    - READ_UNCOMMITTED：读未提交
    - READ_COMMITTED：读已提交（主流）
    - REPEATABLE_READ：可重复读（MySQL默认）
    - SERIALIZABLE：串行化
  - timeout：事务超时时间（秒），默认-1（无超时）
  - readOnly：是否只读事务（默认false），只读事务可优化性能
  - rollbackFor：指定触发回滚的异常类型（默认运行时异常）
  - noRollbackFor：指定不触发回滚的异常类型
- 示例：
@Service
public class UserService {
    @Autowired
    private UserMapper userMapper;
    // 声明事务，指定传播行为和回滚规则
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void transfer(Long fromId, Long toId, BigDecimal amount) throws Exception {
        // 扣减余额
        userMapper.decreaseBalance(fromId, amount);
        // 模拟异常
        if (amount.compareTo(new BigDecimal("1000")) > 0) {
            throw new Exception("转账金额超过限制");
        }
        // 增加余额
        userMapper.increaseBalance(toId, amount);
    }
    // 只读事务
    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        return userMapper.selectById(id);
    }
}
2. @EnableTransactionManagement：在配置类上标注，开启声明式事务支持（Spring Boot自动开启）
- 示例：
@Configuration
@EnableTransactionManagement
public class TransactionConfig {
    // 配置事务管理器（Spring Boot自动配置，无需手动定义）
    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
--------------------------------------------------------------------------------------------------------------------------------------
五、Spring MVC/Web注解
1. 核心控制器注解
① @RestController：组合注解（@Controller + @ResponseBody），标注类为RESTful控制器，方法返回值直接转为JSON/XML响应
② @Controller：标注类为MVC控制器，方法返回值为视图名（配合视图解析器）
- 示例：
@RestController
@RequestMapping("/user")
public class UserController {
    // 接口方法
}
2. 请求映射注解
① @RequestMapping：映射HTTP请求到方法/类，支持指定URL、请求方法、参数、头信息等
- 核心属性：
  - value/path：请求URL路径
  - method：请求方法（RequestMethod.GET/POST/PUT/DELETE等）
  - params：匹配请求参数（如params = "id"，要求请求包含id参数）
  - headers：匹配请求头
  - consumes：匹配请求体类型（如application/json）
  - produces：指定响应体类型（如application/json）
- 示例：
@RequestMapping(value = "/list", method = RequestMethod.GET)
public List<User> getUserList() {
    // 业务逻辑
}
② 简化映射注解（Spring 4.3+）：
  - @GetMapping：等价于@RequestMapping(method = RequestMethod.GET)
  - @PostMapping：等价于@RequestMapping(method = RequestMethod.POST)
  - @PutMapping：等价于@RequestMapping(method = RequestMethod.PUT)
  - @DeleteMapping：等价于@RequestMapping(method = RequestMethod.DELETE)
  - @PatchMapping：等价于@RequestMapping(method = RequestMethod.PATCH)
- 示例：
@GetMapping("/{id}")
public User getUserById(@PathVariable Long id) {
    // 业务逻辑
}
3. 请求参数绑定注解
① @PathVariable：绑定URL路径中的占位符参数
- 示例：
@GetMapping("/{id}")
public User getUserById(@PathVariable("id") Long userId) {
    return userService.getById(userId);
}
② @RequestParam：绑定请求参数（URL查询参数/表单参数）
- 核心属性：
  - value/name：参数名
  - required：是否必填（默认true）
  - defaultValue：默认值
- 示例：
@GetMapping("/list")
public List<User> getUserList(
    @RequestParam(required = false, defaultValue = "1") Integer pageNum,
    @RequestParam(required = false, defaultValue = "10") Integer pageSize
) {
    return userService.list(pageNum, pageSize);
}
③ @RequestBody：绑定HTTP请求体（JSON/XML）到Java对象，适用于POST/PUT请求
- 示例：
@PostMapping("/save")
public Result saveUser(@RequestBody User user) {
    userService.save(user);
    return Result.success();
}
④ @RequestHeader：绑定请求头参数
- 示例：
@GetMapping("/info")
public Result getUserInfo(@RequestHeader("token") String token) {
    // 验证token并返回信息
}
⑤ @CookieValue：绑定Cookie参数
- 示例：
@GetMapping("/cart")
public Result getCart(@CookieValue("cartId") String cartId) {
    // 业务逻辑
}
4. 响应注解
① @ResponseBody：标注在方法/类上，将返回值转为HTTP响应体（JSON/XML），@RestController已包含此注解
② @ResponseStatus：指定响应的HTTP状态码
- 示例：
@PostMapping("/save")
@ResponseStatus(HttpStatus.CREATED) // 响应201状态码
public Result saveUser(@RequestBody User user) {
    userService.save(user);
    return Result.success();
}
③ @ExceptionHandler：标注在控制器方法上，处理当前控制器的异常
④ @ControllerAdvice/@RestControllerAdvice：全局异常处理，配合@ExceptionHandler使用
- 示例：
@RestControllerAdvice // 全局异常处理器
public class GlobalExceptionHandler {
    // 处理运行时异常
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result handleRuntimeException(RuntimeException e) {
        return Result.error("系统异常：" + e.getMessage());
    }
    // 处理自定义异常
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result handleBusinessException(BusinessException e) {
        return Result.error(e.getCode(), e.getMessage());
    }
}
--------------------------------------------------------------------------------------------------------------------------------------
六、条件注解
1. @Conditional：根据指定条件决定是否注册Bean，参数为Condition接口实现类
2. 常用派生注解（Spring Boot提供）：
   - @ConditionalOnClass：类路径存在指定类时生效
   - @ConditionalOnMissingClass：类路径不存在指定类时生效
   - @ConditionalOnBean：容器中存在指定Bean时生效
   - @ConditionalOnMissingBean：容器中不存在指定Bean时生效
   - @ConditionalOnProperty：配置文件中存在指定属性且值匹配时生效
   - @ConditionalOnWebApplication：Web应用环境时生效
   - @ConditionalOnNotWebApplication：非Web应用环境时生效
- 示例：
@Configuration
public class DataSourceConfig {
    // 配置文件中app.datasource.type=mysql时生效
    @Bean
    @ConditionalOnProperty(name = "app.datasource.type", havingValue = "mysql")
    public DataSource mysqlDataSource() {
        // 配置MySQL数据源
    }
    // 配置文件中app.datasource.type=oracle时生效
    @Bean
    @ConditionalOnProperty(name = "app.datasource.type", havingValue = "oracle")
    public DataSource oracleDataSource() {
        // 配置Oracle数据源
    }
}
--------------------------------------------------------------------------------------------------------------------------------------
七、异步/定时任务注解
1. 异步注解
① @Async：标注在方法上，实现方法异步执行，需配合@EnableAsync开启
② @EnableAsync：在配置类上标注，开启异步支持
- 示例：
@Configuration
@EnableAsync
public class AsyncConfig {}
@Service
public class AsyncService {
    // 异步执行此方法
    @Async
    public CompletableFuture<String> doAsyncTask() {
        // 耗时任务
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return CompletableFuture.completedFuture("任务完成");
    }
}
// 调用
@Controller
public class AsyncController {
    @Autowired
    private AsyncService asyncService;
    @GetMapping("/async")
    public String testAsync() throws Exception {
        CompletableFuture<String> future = asyncService.doAsyncTask();
        System.out.println("主线程继续执行");
        String result = future.get(); // 获取异步结果（阻塞）
        return result;
    }
}
2. 定时任务注解
① @Scheduled：标注在方法上，实现定时/周期性执行，需配合@EnableScheduling开启
② @EnableScheduling：在配置类上标注，开启定时任务支持
- @Scheduled核心属性：
  - fixedRate：固定速率，以上次任务开始时间为基准（毫秒）
  - fixedDelay：固定延迟，以上次任务结束时间为基准（毫秒）
  - initialDelay：首次执行延迟时间（毫秒）
  - cron：Cron表达式（更灵活，如"0 0 0 * * ?" 每天凌晨执行）
- 示例：
@Configuration
@EnableScheduling
public class SchedulerConfig {}
@Service
public class ScheduledService {
    // 每5秒执行一次（固定速率）
    @Scheduled(fixedRate = 5000)
    public void fixedRateTask() {
        System.out.println("固定速率任务执行：" + LocalDateTime.now());
    }
    // 延迟1秒后，每10秒执行一次（固定延迟）
    @Scheduled(fixedDelay = 10000, initialDelay = 1000)
    public void fixedDelayTask() {
        System.out.println("固定延迟任务执行：" + LocalDateTime.now());
    }
    // Cron表达式：每天23点59分59秒执行
    @Scheduled(cron = "59 59 23 * * ?")
    public void cronTask() {
        System.out.println("Cron任务执行：" + LocalDateTime.now());
    }
}
--------------------------------------------------------------------------------------------------------------------------------------
八、其他常用注解
1. 缓存注解（Spring Cache）
① @EnableCaching：开启缓存支持
② @Cacheable：标注在方法上，方法执行前先查缓存，有则返回，无则执行方法并缓存结果
③ @CachePut：更新缓存，执行方法后将结果存入缓存
④ @CacheEvict：清除缓存
- 示例：
@Configuration
@EnableCaching
public class CacheConfig {}
@Service
public class UserService {
    // 缓存key为user::id的结果，缓存名user
    @Cacheable(value = "user", key = "#id")
    public User getUserById(Long id) {
        // 从数据库查询
        return userMapper.selectById(id);
    }
    // 更新缓存
    @CachePut(value = "user", key = "#user.id")
    public User updateUser(User user) {
        userMapper.updateById(user);
        return user;
    }
    // 清除指定key的缓存
    @CacheEvict(value = "user", key = "#id")
    public void deleteUser(Long id) {
        userMapper.deleteById(id);
    }
}
2. 事件注解
① @EventListener：标注在方法上，监听指定Spring事件
② @TransactionalEventListener：事务完成后触发的事件监听
- 示例：
// 自定义事件
public class UserCreatedEvent extends ApplicationEvent {
    private User user;
    public UserCreatedEvent(Object source, User user) {
        super(source);
        this.user = user;
    }
    // getter/setter
}
// 发布事件
@Service
public class UserService {
    @Autowired
    private ApplicationEventPublisher publisher;
    public void createUser(User user) {
        userMapper.insert(user);
        // 发布事件
        publisher.publishEvent(new UserCreatedEvent(this, user));
    }
}
// 监听事件
@Service
public class UserEventListener {
    @EventListener
    public void handleUserCreatedEvent(UserCreatedEvent event) {
        User user = event.getUser();
        System.out.println("用户创建事件：" + user.getUsername());
        // 执行后续操作（如发送短信、记录日志）
    }
}
3. 注解组合
① @AliasFor：为注解属性设置别名，常用于自定义注解
- 示例（自定义注解）：
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@RequestMapping(method = RequestMethod.GET) // 组合@RequestMapping
public @interface MyGetMapping {
    // 为value设置别名path
    @AliasFor(attribute = "value")
    String[] path() default {};
    @AliasFor(attribute = "path")
    String[] value() default {};
}
// 使用自定义注解
@RestController
@RequestMapping("/user")
public class UserController {
    @MyGetMapping("/list") // 等价于@RequestMapping(value = "/list", method = GET)
    public List<User> list() {
        return userService.list();
    }
}
--------------------------------------------------------------------------------------------------------------------------------------
总结
1. Spring注解核心围绕IoC/DI、AOP、事务、Web、异步/定时五大核心场景，替代XML配置实现轻量化开发
2. 核心容器注解中，@Component/@Controller/@Service/@Repository定义Bean，@Autowired/@Resource实现依赖注入，@Configuration/@Bean用于配置类
3. 实际开发中，优先使用Spring Boot自动配置的注解（如@RestController、@Transactional），自定义注解可通过组合原生注解简化开发

