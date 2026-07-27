# Spring 框架面试问答清单（7个必做项目版）
> 🎯 基于 Spring 7 个必做项目清单，涵盖从 IOC/AOP 底层原理到 AI 融合的全部面试高频问题，帮助你在面试中脱颖而出。

## 目录
1. [基础概念与核心原理](#1-基础概念与核心原理)
2. [项目实战深度问答](#2-项目实战深度问答)
3. [进阶与系统设计](#3-进阶与系统设计)
4. [场景题与故障排查](#4-场景题与故障排查)

---

## 1. 基础概念与核心原理

> 💡 面试官在这一环节考察你的基本功是否扎实

### Q1：Spring IOC 容器的工作原理是什么？Bean 的生命周期是怎样的？

**面试官意图：** 考察 Spring 最核心概念的理解深度，这是 Spring 面试的"送分题"也是"屠龙刀"。

**完美解答：**

Spring IOC（Inversion of Control，控制反转）容器是一个管理 Bean 的生命周期和依赖关系的框架。核心思想是：**原来由程序员手动 new 对象，现在交给 Spring 容器管理**，程序员只需要声明依赖关系即可。

**Bean 的完整生命周期（关键！）：**

```
1. 实例化（Instantiation）：通过反射创建 Bean 实例
2. 属性赋值（Populate）：设置 Bean 的属性和依赖
3. Aware 接口回调：BeanNameAware、BeanFactoryAware、ApplicationContextAware
4. BeanPostProcessor#postProcessBeforeInitialization：初始化前处理
5. @PostConstruct / InitializingBean#afterPropertiesSet：自定义初始化方法
6. BeanPostProcessor#postProcessAfterInitialization：初始化后处理（AOP 在此创建代理）
7. Bean 就绪，可以使用
8. @PreDestroy / DisposableBean#destroy：容器关闭时销毁
```

**手写简易 IOC 容器（面试展示用）：**

```java
public class SimpleIocContainer {
    private Map<String, Object> singletonObjects = new ConcurrentHashMap<>();
    
    public void registerBean(String beanName, Class<?> clazz) {
        // Step 1: 解析注解，获取类上的依赖信息
        Object bean = createBean(clazz);
        singletonObjects.put(beanName, bean);
    }
    
    private Object createBean(Class<?> clazz) {
        try {
            // Step 2: 实例化
            Object bean = clazz.getDeclaredConstructor().newInstance();
            
            // Step 3: 依赖注入（处理 @Autowired）
            for (Field field : clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(Autowired.class)) {
                    Object dependency = singletonObjects.get(field.getName());
                    if (dependency == null) {
                        dependency = createBean(field.getType());
                        singletonObjects.put(field.getName(), dependency);
                    }
                    field.setAccessible(true);
                    field.set(bean, dependency);
                }
            }
            return bean;
        } catch (Exception e) {
            throw new RuntimeException("Bean 创建失败", e);
        }
    }
    
    public <T> T getBean(String name, Class<T> clazz) {
        return clazz.cast(singletonObjects.get(name));
    }
}
```

**延伸追问应对：** 面试官可能追问"Spring 如何解决循环依赖"，可以从三级缓存机制回答：

> Spring 通过**三级缓存**解决单例 Bean 的循环依赖：一级缓存（完成品）、二级缓存（半成品，未初始化完）、三级缓存（工厂对象）。当 A 依赖 B、B 依赖 A 时，A 创建时提前暴露一个"工厂对象"到三级缓存，B 创建时从三级缓存拿到 A 的半成品完成注入。关键点：只能用三级缓存，因为 AOP 代理需要在属性注入之前就创建。

---

### Q2：Spring AOP 的原理是什么？JDK 动态代理和 CGLIB 代理有什么区别？

**面试官意图：** 考察对 AOP 底层实现的理解，这是区分"会用"和"懂原理"的分水岭。

**完美解答：**

Spring AOP（Aspect Oriented Programming，面向切面编程）的核心原理是**动态代理**。Spring 在运行时为目标 Bean 创建代理对象，在代理对象中织入切面逻辑。

**两种动态代理方式对比：**

```java
// 场景：一个用户服务需要做日志记录

// JDK 动态代理（目标类实现了接口）
public interface UserService {
    void createUser(User user);
}

@Service
public class UserServiceImpl implements UserService {
    @Override
    public void createUser(User user) {
        // 业务逻辑
    }
}
// Spring 会使用 Proxy.newProxyInstance() 创建代理
// 代理对象实现了 UserService 接口

// CGLIB 代理（目标类没有实现接口）
@Service
public class UserService {
    public void createUser(User user) {
        // 业务逻辑
    }
}
// Spring 使用 CGLIB Enhancer 创建子类代理
// 代理对象继承 UserService 类
```

| 对比维度 | JDK 动态代理 | CGLIB 代理 |
|---------|-------------|-----------|
| 要求 | 目标类必须实现接口 | 目标类不需要接口 |
| 实现原理 | InvocationHandler 反射调用 | ASM 字节码增强生成子类 |
| 性能（创建） | 快 | 较慢（字节码操作） |
| 性能（调用） | 较慢（反射调用） | 快（直接方法调用） |
| 限制 | 仅对接口方法有效 | `final` 方法/类无法代理 |
| Spring Boot 默认策略 | 有接口用 JDK 代理 | 无接口或用 CGLIB |

**核心 AOP 概念对应关系：**

```java
@Aspect
@Component
public class LogAspect {
    
    // 切点（Pointcut）：哪些方法需要增强
    @Pointcut("execution(* com.example.service.*.*(..))")
    public void servicePointcut() {}
    
    // 通知（Advice）：增强的逻辑
    @Around("servicePointcut()")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            // 执行目标方法
            Object result = joinPoint.proceed();
            return result;
        } finally {
            long duration = System.currentTimeMillis() - start;
            log.info("{} 执行耗时: {}ms", joinPoint.getSignature(), duration);
        }
    }
}
```

> 💡 **面试加分**：Spring Boot 2.x 之后默认使用 CGLIB 代理，通过 `spring.aop.proxy-target-class=true` 控制。如果你的 Bean 没有实现接口但需要 AOP，Spring Boot 会自动使用 CGLIB。

---

### Q3：SpringBoot 自动配置的原理是什么？如何自定义一个 Starter？

**面试官意图：** 考察对 SpringBoot 核心机制的理解，以及能否独立封装通用组件。

**完美解答：**

SpringBoot 自动配置的核心是 **@EnableAutoConfiguration + @Conditional 条件注解 + spring.factories 配置文件** 三者的组合。

**自动配置工作流程：**

```
SpringBoot 启动 -> @SpringBootApplication(包含@EnableAutoConfiguration)
  -> 读取 META-INF/spring.factories 文件中的配置类
    -> 按条件（@Conditional）加载符合条件的配置
      -> 创建自动配置的 Bean
```

**关键源码分析：**

```java
// 1. spring.factories 文件（定义所有自动配置类）
// META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration

// 2. 自动配置类示例（Redis）
@AutoConfiguration          // 这是一个自动配置类
@ConditionalOnClass(RedisOperations.class)  // 当类路径中存在 RedisOperations 才生效
@EnableConfigurationProperties(RedisProperties.class)  // 绑定配置属性
public class RedisAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean(name = "redisTemplate")
    public RedisTemplate<Object, Object> redisTemplate(
            RedisConnectionFactory redisConnectionFactory) {
        // 创建 RedisTemplate Bean
        RedisTemplate<Object, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        return template;
    }
}

// 3. 配置属性绑定
@ConfigurationProperties(prefix = "spring.redis")
public class RedisProperties {
    private String host = "localhost";
    private int port = 6379;
    private String password;
}

// 用户只需要在 application.yml 中配置：
// spring.redis.host=192.168.1.100
// spring.redis.port=6379
// RedisTemplate 的 Bean 就自动创建好了
```

**如何自定义 Starter：**

```java
// Step 1: 创建自动配置类
@AutoConfiguration
@ConditionalOnClass(MonitorService.class)
@EnableConfigurationProperties(MonitorProperties.class)
public class MonitorAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    public MonitorService monitorService() {
        return new MonitorService();
    }
}

// Step 2: 创建配置属性
@ConfigurationProperties(prefix = "monitor")
public class MonitorProperties {
    private boolean enabled = true;
    private int interval = 60;
}

// Step 3: 注册到 spring.factories
// META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
// com.example.monitor.MonitorAutoConfiguration
```

> 🎯 **总结一句话**：SpringBoot 自动配置 = 约定优于配置 + 条件装配 + 配置属性绑定。它让开发者只需关注业务，不用关心组件的初始化过程。

---

## 2. 项目实战深度问答

> 💡 面试官会深挖你的项目细节，验证你"真的做过"而不是"背过"

### Q4：你手写过 IOC 容器和 AOP 的实现吗？说说当时是怎么设计和实现的？

**面试官意图：** 考察对 Spring 底层原理的真正理解，验证候选人是否做过这个"必做项目"。

**完美解答：**

是的，我为了吃透 Spring 原理，手写过一个极简版的 Spring IOC + AOP 容器。

**IOC 容器设计：**

```java
public class MyApplicationContext {
    
    private Properties config = new Properties();
    private Map<String, Object> singletonObjects = new ConcurrentHashMap<>();
    private Map<String, BeanDefinition> beanDefinitions = new ConcurrentHashMap<>();
    
    // 1. 基于配置文件扫描
    public MyApplicationContext(String basePackage) {
        // 扫描包下所有带有 @Component 注解的类
        Set<Class<?>> classes = scanPackage(basePackage);
        
        // 2. 注册 BeanDefinition
        for (Class<?> clazz : classes) {
            Component annotation = clazz.getAnnotation(Component.class);
            String beanName = annotation.value().isEmpty() 
                ? lowerFirst(clazz.getSimpleName()) 
                : annotation.value();
            beanDefinitions.put(beanName, new BeanDefinition(clazz, ScopeType.SINGLETON));
        }
        
        // 3. 创建单例 Bean
        for (String beanName : beanDefinitions.keySet()) {
            BeanDefinition bd = beanDefinitions.get(beanName);
            if (bd.getScope() == ScopeType.SINGLETON) {
                singletonObjects.put(beanName, createBean(beanName, bd));
            }
        }
    }
    
    // 4. 创建 Bean（包含依赖注入）
    private Object createBean(String beanName, BeanDefinition bd) {
        Class<?> clazz = bd.getClazz();
        Object bean = clazz.getDeclaredConstructor().newInstance();  // 实例化
        
        // 依赖注入：处理 @Autowired 注解
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(Autowired.class)) {
                String fieldName = field.getName();
                Object dependency = singletonObjects.get(fieldName);
                if (dependency == null) {
                    // 如果没有就创建一个
                    dependency = createBean(fieldName, beanDefinitions.get(fieldName));
                    singletonObjects.put(fieldName, dependency);
                }
                field.setAccessible(true);
                field.set(bean, dependency);
            }
        }
        
        // 执行 @PostConstruct 初始化方法
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(PostConstruct.class)) {
                method.invoke(bean);
            }
        }
        
        return bean;
    }
}

// AOP 实现：在 createBean 之后判断是否需要生成代理
private Object wrapIfNecessary(Object bean, Class<?> clazz) {
    if (hasAspectAnnotation(clazz) && hasInterface(clazz)) {
        // JDK 动态代理
        return Proxy.newProxyInstance(
            clazz.getClassLoader(),
            clazz.getInterfaces(),
            (proxy, method, args) -> {
                // 前置通知
                System.out.println("Before: " + method.getName());
                Object result = method.invoke(bean, args);
                // 后置通知
                System.out.println("After: " + method.getName());
                return result;
            }
        );
    }
    return bean;
}
```

> 💡 **面试加分**：手写 IOC/AOP 这个项目会极大提升面试官对你的评价。关键不是写得多完善，而是展示你对反射、动态代理、注解解析、单例模式这些底层技术的掌握程度。

---

### Q5：你在秒杀项目中是怎么设计库存扣减的？用到了哪些核心技术？

**面试官意图：** 考察高并发场景的实战经验，秒杀系统是简历上的最佳亮点。

**完美解答：**

秒杀系统的库存扣减是核心难点——既要保证不超卖，又要保证高并发性能。我用了"三层防护"体系。

**第一层：Redis 原子扣减**

```java
// 使用 Lua 脚本保证原子性
public class StockService {
    
    private static final String DEDUCT_STOCK_LUA = """
        local key = KEYS[1]
        local quantity = tonumber(ARGV[1])
        local stock = tonumber(redis.call('get', key))
        
        if not stock or stock < quantity then
            return 0  -- 库存不足
        end
        
        redis.call('decrby', key, quantity)
        return 1  -- 扣减成功
        """;
    
    public boolean deductStock(Long productId, Integer quantity) {
        String key = "seckill:stock:" + productId;
        Long result = redisTemplate.execute(
            new DefaultRedisScript<>(DEDUCT_STOCK_LUA, Long.class),
            Collections.singletonList(key),
            quantity.toString()
        );
        return result != null && result == 1;
    }
}
```

**第二层：本地标记 + 限流**

```java
@Component
public class SeckillService {
    
    // 本地内存标记：秒杀结束的商品不再请求 Redis
    private final Map<Long, Boolean> localOverMap = new ConcurrentHashMap<>();
    
    public Result seckill(Long productId, Long userId) {
        // 1. 本地标记检查（第一道过滤，减少 Redis 请求）
        if (Boolean.TRUE.equals(localOverMap.get(productId))) {
            return Result.error("商品已售罄");
        }
        
        // 2. 接口限流（单用户每秒只能请求一次）
        String rateLimitKey = "rate:user:" + userId;
        if (!rateLimitService.tryAcquire(rateLimitKey, 1, 1, TimeUnit.SECONDS)) {
            return Result.error("操作过于频繁，请稍后再试");
        }
        
        // 3. Redis Lua 脚本扣减库存
        boolean success = stockService.deductStock(productId, 1);
        if (!success) {
            localOverMap.put(productId, true);  // 标记本地售罄
            return Result.error("商品已售罄");
        }
        
        // 4. 异步生成订单（MQ 削峰）
        seckillProducer.sendMessage(productId, userId);
        
        return Result.success("抢购成功，正在处理订单");
    }
}
```

**第三层：MQ 异步削峰 + 数据库最终一致性**

```java
@Component
@Slf4j
public class SeckillConsumer {
    
    @RabbitListener(queues = "seckill.order.queue")
    public void handleSeckillOrder(SeckillMessage message) {
        // 数据库层面最终扣减（带乐观锁）
        boolean success = orderService.createSeckillOrder(message);
        if (!success) {
            // 如果数据库扣减失败（乐观锁冲突），恢复 Redis 库存
            redisTemplate.opsForValue()
                .increment("seckill:stock:" + message.getProductId());
            log.warn("订单创建失败，已恢复库存：{}", message.getProductId());
        }
    }
}
```

**性能数据：** 单机 Redis Lua 脚本可以支撑 5 万 QPS 的扣减，配合 MQ 削峰，整个系统可以扛住 10 万 QPS 的秒杀流量，而不把数据库打垮。

> ⚠️ **关键经验**：秒杀系统的本质是"把写操作尽最大可能前置到缓存层"。数据库只做最终一致性写入，不做实时写入。如果数据库扣减失败，回滚 Redis 库存即可，保证了最终不超卖。

---

### Q6：你们项目中的 Redis 缓存是怎么解决缓存穿透、击穿、雪崩的？实际效果如何？

**面试官意图：** 考察缓存三大经典问题的实战解决方案，验证是否在真实项目中处理过。

**完美解答：**

我们有一个商品详情页优化项目，上线前每天数据库 QPS 峰值 8000+，经常出现慢查询。我们通过系统性的缓存优化，将数据库 QPS 降到了 500 以下。

**完整的缓存解决方案：**

```java
@Service
public class ProductCacheService {
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    // 缓存 Key 前缀
    private static final String CACHE_KEY_PREFIX = "product:";
    private static final String LOCK_KEY_PREFIX = "lock:product:";
    
    /**
     * 防穿透 + 防击穿 + 防雪崩 的完整缓存查询
     */
    public Product getProduct(Long id) {
        String cacheKey = CACHE_KEY_PREFIX + id;
        
        // ========== 防御 1：缓存穿透（布隆过滤器前置拦截）==========
        if (!bloomFilter.mightContain(id)) {
            log.info("布隆过滤器拦截不存在数据：id={}", id);
            return null;
        }
        
        // ========== 查询一级缓存 ==========
        String json = redisTemplate.opsForValue().get(cacheKey);
        
        // 缓存命中
        if (json != null) {
            // 处理缓存空值（穿透防御）
            if ("NULL_VALUE".equals(json)) {
                return null;
            }
            return JSON.parseObject(json, Product.class);
        }
        
        // ========== 防御 2：缓存击穿（互斥锁）==========
        String lockKey = LOCK_KEY_PREFIX + id;
        boolean locked = redisTemplate.opsForValue()
            .setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);
        
        if (!locked) {
            // 没拿到锁，说明其他线程正在加载，等待后重试
            try { Thread.sleep(50); } catch (InterruptedException e) {}
            return getProduct(id);  // 递归重试
        }
        
        // ========== 查询数据库 ==========
        try {
            Product product = productMapper.selectById(id);
            
            if (product == null) {
                // 防御穿透：缓存空值（5 分钟过期）
                redisTemplate.opsForValue()
                    .set(cacheKey, "NULL_VALUE", 5 + RandomUtils.nextInt(0, 300), TimeUnit.SECONDS);
                return null;
            }
            
            // ========== 防御 3：缓存雪崩（过期时间加随机值）==========
            int baseExpire = 3600;  // 基础过期时间 1 小时
            int randomExpire = RandomUtils.nextInt(0, 600);  // 随机 0-10 分钟
            redisTemplate.opsForValue()
                .set(cacheKey, JSON.toJSONString(product), baseExpire + randomExpire, TimeUnit.SECONDS);
            
            return product;
        } finally {
            // 释放锁
            redisTemplate.delete(lockKey);
        }
    }
}
```

**解决方案对比总结：**

| 问题 | 现象 | 解决方案 | 效果 |
|------|------|---------|------|
| 缓存穿透 | 数据库 QPS 飙升，大量不存在的数据请求 | 布隆过滤器 + 缓存空值 | 拦截 99.9% 的无意义请求 |
| 缓存击穿 | 热点 Key 过期瞬间，大量请求打到 DB | 互斥锁 + 逻辑过期 | 保证同时只有 1 个线程查 DB |
| 缓存雪崩 | 大量 Key 同时过期，DB 压力暴增 | 过期时间加随机值 | 均匀分布过期时间 |

> 🎯 **效果数据**：优化前数据库 QPS 8000+，优化后 500-。商品详情页接口的 P99 延迟从 2s 降到 30ms。

---

## 3. 进阶与系统设计

> 💡 这是拉开差距的环节，考察架构思维

### Q7：如果要设计一个企业级的 RabbitMQ 异步消息系统，需要考虑哪些关键点？

**面试官意图：** 考察消息队列的实战经验，不仅仅是会用 @RabbitListener。

**完美解答：**

从项目的订单异步通知系统出发，需要从可靠性、幂等性、顺序性、可监控性四个维度设计。

**1. 消息可靠性（防丢失）：**

```java
// 生产者：确认模式 + 回调
@Configuration
public class RabbitConfig {
    
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        
        // 开启生产者确认
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                log.error("消息发送失败：correlationId={}, cause={}", 
                    correlationData.getId(), cause);
                // 将消息存入数据库，定时任务重新发送
                messageRepository.save(MessageRecord.failed(correlationData.getId()));
            }
        });
        
        // 开启消息到达队列确认
        template.setReturnsCallback(returned -> {
            log.error("消息未到达队列：exchange={}, routingKey={}", 
                returned.getExchange(), returned.getRoutingKey());
        });
        
        return template;
    }
}

// 消费者：手动 ACK
@RabbitListener(queues = "order.notify.queue")
public void handleMessage(String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag) {
    try {
        process(message);
        channel.basicAck(tag, false);  // 手动确认，处理成功才 ACK
    } catch (Exception e) {
        log.error("消息处理失败", e);
        channel.basicNack(tag, false, true);  // 重新入队重试
    }
}
```

**2. 消息幂等性（防重复消费）：**

```java
@Component
public class IdempotentHandler {
    
    public boolean isProcessed(String messageId) {
        // 使用 Redis 记录已处理的消息 ID，过期时间 1 小时
        Boolean exists = redisTemplate.hasKey("msg:" + messageId);
        if (Boolean.TRUE.equals(exists)) {
            return true;  // 已处理，跳过
        }
        // 标记为已处理（SET NX + 过期时间）
        Boolean success = redisTemplate.opsForValue()
            .setIfAbsent("msg:" + messageId, "1", 1, TimeUnit.HOURS);
        return Boolean.FALSE.equals(success);
    }
}
```

**3. 死信队列（延迟消息 + 失败兜底）：**

```java
@Configuration
public class DlxConfig {
    
    // 定义死信队列（处理失败超过 3 次的消息）
    @Bean
    public Queue dlxQueue() {
        return QueueBuilder.durable("dlx.queue").build();
    }
    
    @Bean
    public DirectExchange dlxExchange() {
        return new DirectExchange("dlx.exchange");
    }
    
    @Bean
    public Binding dlxBinding() {
        return BindingBuilder.bind(dlxQueue()).to(dlxExchange()).with("dlx");
    }
    
    // 主队列设置死信路由
    @Bean
    public Queue businessQueue() {
        return QueueBuilder.durable("order.notify.queue")
            .withArgument("x-dead-letter-exchange", "dlx.exchange")
            .withArgument("x-dead-letter-routing-key", "dlx")
            .build();
    }
}

// 死信处理器
@RabbitListener(queues = "dlx.queue")
public void handleDlxMessage(String message) {
    log.warn("消息超过重试次数，进入死信队列：{}", message);
    // 发送告警通知
    alertService.sendAlert("消息处理失败", message);
    // 存入数据库，人工介入处理
}
```

**核心设计要点：**

| 维度 | 方案 |
|------|------|
| 消息不丢失 | 生产者确认 + 持久化 + 消费者手动 ACK |
| 消息不重复 | Redis 消息 ID 去重（幂等性设计） |
| 消息顺序性 | 同一个业务 ID 的消息路由到同一个队列 |
| 故障隔离 | 死信队列兜底 + 告警通知 |
| 监控告警 | 队列堆积告警 + 消费延迟告警 |

> 💡 **面试加分点**：可以补充"消息积压"的应对经验——当消费者跟不上生产者时，先排查消费者性能瓶颈，临时解决方案是增加消费者数量，永久方案是优化消费逻辑或增加消费者机器。

---

### Q8：你们的 SpringBoot 博客后端项目的分层架构是怎么设计的？为什么？

**面试官意图：** 考察工程化能力和架构分层思维。

**完美解答：**

采用了标准的**四层架构**：Controller -> Service -> Manager -> Mapper。

**分层设计：**

```java
// 1. Controller 层（接口定义层）
// 作用：接收请求、参数校验、响应返回
// 不做：不包含任何业务逻辑
@RestController
@RequestMapping("/api/articles")
@Api(tags = "文章管理")
public class ArticleController {
    
    @Autowired
    private ArticleService articleService;
    
    @GetMapping("/{id}")
    @ApiOperation("获取文章详情")
    public Result<ArticleVO> getArticle(@PathVariable Long id) {
        ArticleVO article = articleService.getArticleById(id);
        return Result.success(article);
    }
}

// 2. Service 层（业务流程层）
// 作用：编排业务逻辑、事务管理、调用多个 Manager
@Service
@Transactional(readOnly = true)
public class ArticleServiceImpl implements ArticleService {
    
    @Autowired
    private ArticleManager articleManager;
    @Autowired
    private CommentManager commentManager;
    
    @Override
    public ArticleVO getArticleById(Long id) {
        Article article = articleManager.getById(id);
        // 增加浏览量
        articleManager.incrementViewCount(id);
        // 获取评论数
        long commentCount = commentManager.countByArticleId(id);
        return ArticleVO.from(article, commentCount);
    }
}

// 3. Manager 层（复用逻辑层）
// 作用：缓存策略、第三方服务调用、通用业务逻辑
// 为什么多这一层？因为 Service 可能调用多个 Manager
@Component
public class ArticleManager {
    
    public Article getById(Long id) {
        // 先查缓存，再查数据库（缓存穿透保护）
        return cacheService.getOrSet("article:" + id, 
            () -> articleMapper.selectById(id), 
            30, TimeUnit.MINUTES);
    }
}

// 4. Mapper 层（数据访问层）
// 作用：数据库操作，MyBatis 映射
@Mapper
public interface ArticleMapper extends BaseMapper<Article> {
    // 自定义复杂 SQL 使用 XML
}
```

**为什么这么分层：**

| 层 | 职责 | 服务调用者 | 好处 |
|----|------|-----------|------|
| Controller | 输入输出处理 | 前端/MV | 接口与逻辑解耦 |
| Service | 业务编排 | Controller | 事务管理、业务流程清晰 |
| Manager | 通用能力复用 | Service | 避免 Service 层重复代码 |
| Mapper | 数据访问 | Manager | 隔离 ORM 框架 |

> 🎯 **好的架构的特点**：当需求变更时，只需要修改一个层。比如缓存策略调整只改 Manager 层，不涉及 Controller 和 Service。

---

## 4. 场景题与故障排查

> 💡 考察实际解决问题的能力

### Q9：线上 SpringBoot 项目启动很慢，可能是什么原因？怎么排查？

**面试官意图：** 考察 SpringBoot 项目的调优经验和故障排查思路。

**完美解答：**

SpringBoot 项目启动慢通常有以下原因，我从最常见到最罕见排序：

**排查流程：**

```bash
# 1. 开启启动过程日志，查看耗时分布
# application.yml 添加：
debug: true
logging:
  level:
    org.springframework.boot: DEBUG
    org.springframework.context: TRACE

# 2. 或者使用 Spring Boot Actuator 的启动端点
# 查看 Bean 创建耗时
management:
  endpoints:
    web:
      exposure:
        include: startup
```

**常见原因及解决方案：**

| 原因 | 特征 | 解决方案 |
|------|------|----------|
| 类路径扫描范围太大 | 启动日志中 ComponentScan 耗时高 | 指定扫描包范围：`@ComponentScan("com.example")` |
| 数据源连接验证慢 | 启动时等待数据库连接 | 配置连接池 `spring.datasource.hikari.connection-timeout=5000` |
| 大量 Bean 懒加载 | 启动时初始化所有 Bean | 非核心 Bean 加 `@Lazy` 延迟加载 |
| Redis 不可用 | 启动时连接 Redis 超时 | Redis 做好高可用或配置重试 |
| 大量自动配置类 | 不需要的配置也被加载 | 使用 `@SpringBootApplication(exclude=...)` 排除 |
| 日志配置问题 | Logback 初始化慢 | 简化日志配置，异步日志输出 |

```java
// 优化方案：显示指定扫描范围 + 排除不必要的自动配置
@SpringBootApplication(
    scanBasePackages = {"com.example.blog"},
    exclude = {
        DataSourceAutoConfiguration.class,  // 如果不使用数据库
        SecurityAutoConfiguration.class     // 如果不使用安全框架
    }
)
// 非核心 Service 懒加载
@Service
@Lazy
public class ReportService {
    // 只有在第一次使用时才初始化
}
```

> 💡 **经验值**：正常 SpringBoot 2.x 应用的启动时间在 5-15 秒之间。如果超过 30 秒，一定存在问题需要排查。

---

### Q10：SpringBoot 项目上线后出现了 OOM，怎么快速定位是哪个对象导致的？

**面试官意图：** 考察 JVM 故障排查实战能力，以及是否经历过线上问题。

**完美解答：**

**标准排查流程：**

```bash
# Step 1: 查看进程状态
jps -v | grep -i blog  # 确认 Java 进程 PID

# Step 2: 查看 JVM 内存使用概览
jstat -gcutil <PID> 2000 5
# 重点关注 O（老年代使用率），如果持续上升说明有内存泄漏

# Step 3: 查看内存中大对象
jmap -histo:live <PID> | head -20

# Step 4: 生成堆转储（线上操作前务必保持现场）
jmap -dump:live,format=b,file=/tmp/heap.hprof <PID>
```

**分析 Heap Dump 的典型案例：**

```java
// 案例 1：ThreadLocal 使用不当导致内存泄漏
// 常见于拦截器或过滤器中的用户信息存储
@Component
public class UserContextFilter implements Filter {
    
    private static final ThreadLocal<User> userHolder = new ThreadLocal<>();
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
        try {
            // 设置用户信息
            User user = extractUser(request);
            userHolder.set(user);
            chain.doFilter(request, response);
        } finally {
            // 没有在 finally 中 remove！
            // 如果线程池的线程被复用，ThreadLocal 中的对象一直无法 GC
            // userHolder.remove();  // 缺少这行！
        }
    }
}

// 案例 2：缓存无限增长
@Component
public class LocalCache {
    // 使用 HashMap 当缓存，没有大小限制
    private Map<String, Object> cache = new HashMap<>();
    
    public void put(String key, Object value) {
        cache.put(key, value);  // 无限增长，最终 OOM
    }
}

// 正确做法：使用 Caffeine 等有淘汰策略的缓存
@Component
public class LocalCache {
    private Cache<String, Object> cache = Caffeine.newBuilder()
        .maximumSize(10000)           // 最大 10000 条
        .expireAfterWrite(10, TimeUnit.MINUTES)  // 写入 10 分钟后过期
        .recordStats()                // 记录统计信息
        .build();
}
```

**MAT 分析关键步骤：**

```
1. 打开 heap.hprof -> 点击 "Leak Suspects"（泄漏嫌疑分析）
2. 查看 Dominator Tree（支配树），按 Retained Heap 排序
3. 找到最大的对象 -> 查看 GC Root 引用链
4. 对照代码，找到谁创建了这个对象，为什么没有被释放
```

> 🎯 **最佳实践**：线上 JVM 一定要配置以下参数，否则出问题后像"没带钥匙出门"：
> ```
> -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/var/log/
> -XX:+PrintGCDetails -Xloggc:/var/log/gc.log
> ```

---

### Q11：如果让你给 SpringBoot 项目接入一个自定义的配置中心，你会怎么做？

**面试官意图：** 考察对配置管理方案的理解，以及 Spring 扩展机制的掌握。

**完美解答：**

我会利用 Spring 的 **Environment 扩展接口 + @RefreshScope + 配置监听器** 来实现动态配置中心。

**核心实现：**

```java
// 1. 自定义 PropertySource，从配置中心获取配置
@Component
public class CustomPropertySource implements PropertySourceLocator {
    
    @Override
    public PropertySource<?> locate(Environment environment) {
        // 从配置中心获取所有配置
        Map<String, Object> configs = configCenterClient.getAllConfigs("my-app", "default");
        return new MapPropertySource("customConfig", configs);
    }
}

// 2. 实现配置热更新
@Component
public class ConfigRefreshListener {
    
    @Autowired
    private ConfigurableApplicationContext context;
    
    @EventListener
    public void onConfigChanged(ConfigChangeEvent event) {
        // 配置中心推送变更通知
        for (String key : event.getChangedKeys()) {
            // 更新 Environment 中的配置
            ConfigurableEnvironment env = context.getEnvironment();
            MutablePropertySources sources = env.getPropertySources();
            
            // 找到我们自定义的 PropertySource 并更新
            MapPropertySource source = (MapPropertySource) sources.get("customConfig");
            if (source != null) {
                source.getSource().put(key, event.getNewValue(key));
            }
        }
        
        // 触发 @RefreshScope 的 Bean 刷新
        context.getBean(RefreshScope.class).refreshAll();
    }
}

// 3. 使用方式（配置变更后自动刷新）
@RefreshScope  // 这个注解是关键
@Component
@ConfigurationProperties(prefix = "order")
public class OrderConfig {
    private Integer timeout;
    private Integer maxRetryCount;
    // 配置变更后，这些字段自动刷新成新值
}
```

> 💡 **核心理解**：动态配置的本质是"每当配置变更时，销毁旧的 Bean 并重新创建新的 Bean"。`@RefreshScope` 就是做这件事的——当配置中心的配置变更时，它会让被标注的 Bean 重新初始化一次。

---

## 💎 面试加分金句
- "手写 IOC 容器让我真正理解了 Spring 的设计思想——不是框架有多神秘，而是反射 + 注解 + 单例模式三个基础技术的组合。" 
- "面试官，关于循环依赖我想补充一点：三级缓存的'工厂对象'是为了处理 AOP 代理的场景，如果没有 AOP，二级缓存就够了。"
- "秒杀系统的核心不是'快'而是'准'——库存扣减必须原子性，哪怕慢一点也不能超卖，这是一个技术底线问题。"
- "缓存三大问题（穿透、击穿、雪崩）本质上是同一个问题：缓存和数据库之间的数据同步不一致。理解了这个本质，就能举一反三。"
- "好的架构不是一蹴而就的，而是在业务迭代中不断演进的结果。初期够用就好，不要过度设计。"

## 📋 高频追问清单
| 追问方向 | 应对策略 |
|----------|----------|
| Spring IOC 和 DI 是什么关系？ | DI 是 IOC 的一种实现方式，IOC 是设计思想，DI 是具体手段 |
| 单例 Bean 是线程安全的吗？ | 不是，Spring 不保证 Bean 的线程安全，有状态 Bean 需要自己处理并发 |
| @Autowired 和 @Resource 的区别？ | @Autowired 按类型注入，@Resource 按名称注入 |
| SpringBoot 和 Spring 的区别？ | SpringBoot = Spring 框架 + 自动配置 + 嵌入式服务器 + 简化配置 |
| Spring 事务在什么情况下会失效？ | 非 public 方法、同类方法自调用、异常被 catch、事务传播属性变更 |
| @Transactional 底层原理？ | AOP 实现，通过代理对象在方法前后添加事务开启/提交/回滚逻辑 |

## 🔗 关联知识点
- [SpringCloud 面试问答](./SpringCloud必做项目-面试问答.md)
- [SpringAI 面试问答](./SpringAI必做项目-面试问答.md)
- [MyBatis 面试问答](./MyBatis必做项目清单-面试问答.md)
- [Maven 面试问答](./Maven必做项目清单-面试问答.md)
