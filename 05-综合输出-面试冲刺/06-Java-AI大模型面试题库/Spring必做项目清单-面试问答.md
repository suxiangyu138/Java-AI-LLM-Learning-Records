# Spring 必做项目清单 面试问答
> 🎯 基于 Spring 全家桶项目实战清单，涵盖面试高频问题与完美解答方案，帮助你在面试中脱颖而出。

## 目录
1. [基础概念与核心原理](#1-基础概念与核心原理)
2. [项目实战深度问答](#2-项目实战深度问答)
3. [进阶与系统设计](#3-进阶与系统设计)
4. [场景题与故障排查](#4-场景题与故障排查)

---

## 1. 基础概念与核心原理

> 💡 面试官在这一环节考察你的基本功是否扎实

### Q1：请说一下 Spring IoC 容器的核心原理，以及 Bean 的生命周期

**面试官意图：** 考察对 Spring 最核心的 IoC 机制的理解深度，不满足于只会用注解。

**完美解答：**

**IoC（控制反转）** 的核心思想是将对象的创建和管理权交给容器。传统开发中我们主动 `new` 对象，使用 IoC 后由容器注入依赖，实现了"好莱坞原则"——别打电话给我们，我们会打给你。

**Spring IoC 容器的核心工作流程：**

```
加载配置 -> 解析 BeanDefinition -> 实例化 -> 填充属性 -> 初始化 -> 放入容器 -> 使用 -> 销毁
```

**Bean 的完整生命周期（10 个步骤）：**

```text
1. 实例化 Bean (反射创建对象)
2. 填充属性 (依赖注入)
3. 检查 Aware 接口：BeanNameAware、BeanFactoryAware、ApplicationContextAware
4. BeanPostProcessor#postProcessBeforeInitialization (前置处理)
5. 执行 InitializingBean#afterPropertiesSet
6. 执行 init-method (自定义初始化方法)
7. BeanPostProcessor#postProcessAfterInitialization (后置处理) [AOP 在此步创建代理]
8. Bean 准备就绪，可以使用
9. 容器关闭时，执行 DisposableBean#destroy
10. 执行 destroy-method (自定义销毁方法)
```

**手写简易 IoC 容器的核心代码：**

```java
public class SimpleIoCContainer {
    private final Map<String, Object> singletonMap = new ConcurrentHashMap<>();
    private final Map<String, BeanDefinition> beanDefinitionMap = new HashMap<>();
    
    // 1. 扫描包，解析带有 @Component 的类
    public void scanPackage(String basePackage) {
        // 获取包下所有类
        Set<Class<?>> classes = getClasses(basePackage);
        for (Class<?> clazz : classes) {
            if (clazz.isAnnotationPresent(Component.class)) {
                BeanDefinition bd = new BeanDefinition();
                bd.setBeanClass(clazz);
                bd.setScope(clazz.getAnnotation(Component.class).scope());
                beanDefinitionMap.put(toLowerFirst(clazz.getSimpleName()), bd);
            }
        }
    }
    
    // 2. 获取 Bean——先从单例池取，没有再创建
    public Object getBean(String name) {
        Object bean = singletonMap.get(name);
        if (bean != null) return bean;
        
        BeanDefinition bd = beanDefinitionMap.get(name);
        if (bd == null) throw new NoSuchBeanDefinitionException(name);
        
        // 创建 Bean 实例
        bean = createBeanInstance(bd);
        
        // 如果是单例，放入单例池
        if (SingletonScope.SINGLETON.equals(bd.getScope())) {
            singletonMap.put(name, bean);
        }
        return bean;
    }
    
    // 3. 填充属性——遍历字段，找到 @Autowired 注入
    private void injectDependencies(Object bean) {
        Field[] fields = bean.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(Autowired.class)) {
                Object dependency = getBean(field.getName());
                field.setAccessible(true);
                field.set(bean, dependency);
            }
        }
    }
}
```

> 💡 理解 IoC 原理最好的方式是手写一个简易容器。不需要造轮子，但这个过程能让你理解 Spring 为什么要那样设计。

**延伸追问应对：** 如果问"Bean 的 scope 有哪些"，回答 singleton（默认）、prototype（每次创建新实例）、request/session/application（Web 环境下使用）。

---

### Q2：请说说 Spring AOP 的实现原理，JDK 动态代理和 CGLIB 有什么区别？

**面试官意图：** 考察 AOP 的底层实现理解，特别是代理机制的选择和区别。

**完美解答：**

Spring AOP 基于**代理模式**实现，核心是两种代理方式。

**JDK 动态代理：**
- 要求目标对象**必须实现接口**
- 通过 `Proxy.newProxyInstance()` 生成目标接口的代理对象
- 代理对象调用方法时，通过 `InvocationHandler.invoke()` 拦截

```java
public class JdkProxyFactory {
    public static Object createProxy(Object target) {
        return Proxy.newProxyInstance(
            target.getClass().getClassLoader(),
            target.getClass().getInterfaces(),
            (proxy, method, args) -> {
                System.out.println("前置通知");
                Object result = method.invoke(target, args);
                System.out.println("后置通知");
                return result;
            }
        );
    }
}
```

**CGLIB 动态代理：**
- 目标类**不需要实现接口**
- 通过字节码技术**生成目标类的子类**作为代理
- 代理对象调用方法时，通过 `MethodInterceptor.intercept()` 拦截

```java
public class CglibProxyFactory implements MethodInterceptor {
    public Object createProxy(Class<?> targetClass) {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(targetClass);
        enhancer.setCallback(this);
        return enhancer.create();
    }
    
    @Override
    public Object intercept(Object obj, Method method, Object[] args, 
                            MethodProxy proxy) throws Throwable {
        System.out.println("前置通知");
        Object result = proxy.invokeSuper(obj, args); // 调用父类方法
        System.out.println("后置通知");
        return result;
    }
}
```

**两者的对比：**

| 对比维度 | JDK 动态代理 | CGLIB 代理 |
|---------|-------------|-----------|
| 要求 | 目标类必须实现接口 | 不要求接口 |
| 原理 | 生成接口的代理实现 | 生成目标类的子类 |
| 性能（JDK 8+） | 高（优化后） | 中等 |
| 限制 | 只能代理接口中的方法 | final 类/方法无法代理 |
| Spring 默认行为 | 接口实现类使用 JDK 代理 | 无接口的使用 CGLIB 代理 |

**延伸追问应对：** 如果问"Spring Boot 2.x 之后的默认行为"，回答 Spring Boot 2.x 将 `spring.aop.proxy-target-class` 默认设为 `true`，即使有接口，默认也使用 CGLIB 代理。原因是通过 JDK 代理只能注入接口类型，不能注入实现类类型，而 CGLIB 没有这个限制。

---

### Q3：请说一下 Spring 事务的传播机制，以及 @Transactional 失效的场景

**面试官意图：** 考察对声明式事务的深度理解，特别是事务传播和常见踩坑经验。

**完美解答：**

**事务传播机制（7 种）：**

| 传播行为 | 含义 | 使用场景 |
|---------|------|---------|
| REQUIRED (默认) | 有事务则加入，没有则新建 | 最常用，数据修改操作 |
| REQUIRES_NEW | 无论有无事务都新建 | 独立操作日志记录 |
| NESTED | 嵌套事务，子事务回滚不影响主事务 | 批量操作中单条失败只回滚本条 |
| SUPPORTS | 有则加入，没有则以非事务方式执行 | 查询方法 |
| NOT_SUPPORTED | 以非事务方式执行 | 发送通知、邮件 |
| MANDATORY | 必须在事务中执行，否则抛异常 | 要求调用方已开启事务 |
| NEVER | 不能在事务中执行，否则抛异常 | 测试、验证方法 |

**@Transactional 失效的 6 种经典场景：**

1. **`private` 方法上使用**：AOP 代理无法拦截 private 方法
2. **同类方法内部调用**：`insertA()` 中调用 `insertB()`，`this.insertB()` 不会走代理
3. **`rollbackFor` 未指定**：默认只回滚 `RuntimeException`，不回滚受检异常
4. **被 `try-catch` 吞掉了异常**：异常被捕获后事务无法感知
5. **数据库引擎不支持事务**：MySQL 的 MyISAM 引擎不支持
6. **传播机制设置不当**：如设置了 `NOT_SUPPORTED` 自然不生效

**解决内部调用事务失效的方案：**

```java
// 方案一：注入自身代理
@Service
public class OrderService {
    @Autowired
    private OrderService self; // 注入自身代理
    
    public void outerMethod() {
        self.innerMethod(); // 走代理，事务生效
    }
    
    @Transactional
    public void innerMethod() {
        // 事务性操作
    }
}

// 方案二：使用 AopContext 获取当前代理
public void outerMethod() {
    ((OrderService) AopContext.currentProxy()).innerMethod();
}
// 需要在启动类上加 @EnableAspectJAutoProxy(exposeProxy = true)
```

---

## 2. 项目实战深度问答

> 💡 面试官会深挖你的项目细节，验证你"真的做过"而不是"背过"

### Q4：你手写简易 IoC 容器时，怎么处理循环依赖的？

**面试官意图：** 考察对 Spring 三级缓存解决循环依赖的理解，以及是否亲自实践过。

**完美解答：**

**什么是循环依赖：**
A 依赖 B，B 依赖 A，两者相互引用。如果直接用构造器注入，会导致死循环。

**Spring 的解决方案——三级缓存：**

```java
// 三级缓存定义
Map<String, Object> singletonObjects = new ConcurrentHashMap<>();      // 一级缓存：成品 Bean
Map<String, Object> earlySingletonObjects = new HashMap<>();           // 二级缓存：半成品 Bean
Map<String, ObjectFactory<?>> singletonFactories = new HashMap<>();    // 三级缓存：Bean 工厂
```

**三级缓存的工作流程（以 A 依赖 B、B 依赖 A 为例）：**

```
1. 开始创建 A
2. A 实例化（调用构造方法），得到一个半成品
3. 将 A 的 ObjectFactory 放入三级缓存（singletonFactories）
4. A 开始填充属性，发现需要 B
5. 创建 B —— 同样流程，B 实例化后放入三级缓存
6. B 填充属性，发现需要 A
7. 从三级缓存拿到 A 的工厂，创建 A 的提前引用，放入二级缓存（earlySingletonObjects），删除三级缓存
8. B 得到 A 的引用，B 创建完成，放入一级缓存
9. 继续填充 A，A 注入完整的 B
10. A 创建完成，放入一级缓存，删除二级缓存
```

**为什么要三级缓存而不是二级？**

关键区别在于：三级缓存存的是 `ObjectFactory`，可以在 Bean 实例化后、初始化前执行一些逻辑（如 AOP 生成代理对象）。如果只用二级缓存，那么所有 Bean 在实例化后就必须决定是否生成代理，这不灵活。

具体来说：如果一个 Bean 需要 AOP 增强，ObjectFactory 的 `getObject()` 会返回代理对象而不是原始对象。**三级缓存的存在使得代理的创建延迟到真正需要引用的时候**，而不是预先创建。

```java
// 手写简易三级缓存解决循环依赖
public class SimpleCircularDependencyResolver {
    private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>();
    private final Map<String, Object> earlySingletonObjects = new HashMap<>();
    private final Map<String, Supplier<Object>> singletonFactories = new HashMap<>();
    
    public Object getBean(String name) {
        // 查一级缓存
        Object bean = singletonObjects.get(name);
        if (bean != null) return bean;
        // 查二级缓存
        bean = earlySingletonObjects.get(name);
        if (bean != null) return bean;
        // 查三级缓存
        Supplier<Object> factory = singletonFactories.get(name);
        if (factory != null) {
            bean = factory.get(); // 可能是 AOP 代理
            earlySingletonObjects.put(name, bean);
            singletonFactories.remove(name);
        }
        return bean;
    }
}
```

---

### Q5：你在项目中怎么做 AOP 切面监控的？说下具体实现

**面试官意图：** 考察 AOP 的实际应用能力，不只停留在概念层面。

**完美解答：**

我做了两个 AOP 实战项目：**请求耗时监控**和**操作日志埋点**。

**项目一：接口耗时监控切面**

```java
@Aspect
@Component
public class ApiMonitorAspect {
    
    private final MeterRegistry meterRegistry;
    
    @Around("@annotation(org.springframework.web.bind.annotation.RequestMapping) || "
          + "@annotation(org.springframework.web.bind.annotation.PostMapping) || "
          + "@annotation(org.springframework.web.bind.annotation.GetMapping)")
    public Object monitorApi(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().toShortString();
        
        try {
            Object result = joinPoint.proceed();
            long cost = System.currentTimeMillis() - start;
            
            // 记录到 Metrics
            meterRegistry.timer("api.call.duration", 
                "method", methodName,
                "status", "success"
            ).record(cost, TimeUnit.MILLISECONDS);
            
            // 慢请求告警
            if (cost > 1000) {
                log.warn("慢请求告警：{} 耗时 {}ms", methodName, cost);
            }
            return result;
        } catch (Exception e) {
            long cost = System.currentTimeMillis() - start;
            meterRegistry.timer("api.call.duration",
                "method", methodName,
                "status", "error"
            ).record(cost, TimeUnit.MILLISECONDS);
            throw e;
        }
    }
}
```

**项目二：操作日志埋点**

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperationLog {
    String module();    // 模块名：用户管理、订单管理
    String action();    // 操作类型：新增、修改、删除
    String description() default "";
}

@Aspect
@Component
public class OperationLogAspect {
    
    @Around("@annotation(operationLog)")
    public Object logOperation(ProceedingJoinPoint joinPoint, 
                                OperationLog operationLog) throws Throwable {
        // 记录操作前数据
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        
        try {
            Object result = joinPoint.proceed();
            // 异步记录操作日志
            logService.saveAsync(OperationLogDO.builder()
                .module(operationLog.module())
                .action(operationLog.action())
                .description(operationLog.description())
                .params(JSON.toJSONString(args))
                .operator(CurrentUser.getUserId())
                .result("成功")
                .build());
            return result;
        } catch (Exception e) {
            logService.saveAsync(OperationLogDO.builder()
                .module(operationLog.module())
                .action(operationLog.action())
                .params(JSON.toJSONString(args))
                .operator(CurrentUser.getUserId())
                .result("失败：" + e.getMessage())
                .build());
            throw e;
        }
    }
}
```

> 💡 AOP 最优雅的地方在于**关注点分离**——业务代码不需要关心日志、监控、事务这些横切关注点，核心逻辑保持干净。

---

### Q6：讲一下你做的 SSM 校园二手交易平台中，多表联查和动态 SQL 的经验

**面试官意图：** 考察 MyBatis 的实战能力，特别是复杂查询场景的处理。

**完美解答：**

**动态 SQL 的核心应用场景：**

二手商品列表需要支持多条件筛选：按分类、价格区间、成色、地区、关键词搜索，每个条件可选组合。

```xml
<!-- 商品多条件查询 -->
<select id="searchProducts" resultMap="ProductDetailMap">
    SELECT p.*, c.name as category_name, u.nickname as seller_name
    FROM product p
    LEFT JOIN category c ON p.category_id = c.id
    LEFT JOIN user u ON p.seller_id = u.id
    <where>
        <if test="categoryId != null">
            AND p.category_id = #{categoryId}
        </if>
        <if test="minPrice != null">
            AND p.price >= #{minPrice}
        </if>
        <if test="maxPrice != null">
            AND p.price &lt;= #{maxPrice}
        </if>
        <if test="keyword != null and keyword != ''">
            AND (p.title LIKE CONCAT('%', #{keyword}, '%')
                 OR p.description LIKE CONCAT('%', #{keyword}, '%'))
        </if>
        <if test="condition != null">
            AND p.condition_level = #{condition}
        </if>
        AND p.status = 1  <!-- 仅显示上架商品 -->
    </where>
    ORDER BY p.create_time DESC
</select>
```

**多表联查的两个方案：**

```java
// 方案一：嵌套查询（N+1 问题风险）
<collection property="orderItems" column="id" 
    select="com.xxx.mapper.OrderItemMapper.selectByOrderId"/>

// 方案二：联合查询（推荐）
<resultMap id="OrderDetailMap" type="OrderVO">
    <id column="id" property="id"/>
    <result column="order_no" property="orderNo"/>
    <collection property="items" ofType="OrderItemVO">
        <id column="item_id" property="id"/>
        <result column="product_name" property="productName"/>
        <result column="quantity" property="quantity"/>
        <result column="price" property="price"/>
    </collection>
</resultMap>
```

**优化经验：** 当数据量大时，分页查询一定要配合 `COUNT` 优化，先查总数再查数据。另外，电商类项目的"商品+分类+卖家"查询，建议对商品表做索引覆盖，避免回表查询。

---

## 3. 进阶与系统设计

> 💡 这是拉开差距的环节，考察架构思维

### Q7：如果让你设计一个基于 Spring 的 RPC 框架，你怎么做？

**面试官意图：** 考察对 RPC 原理的理解和架构设计能力。

**完美解答：**

RPC（远程过程调用）的核心目标是让**调用远程服务像调用本地方法一样简单**。

**架构设计：**

```
┌──────────────┐     ┌──────────────┐
│  服务消费者   │     │  服务提供者   │
│  (客户端)     │     │  (服务端)     │
│              │     │              │
│ 1. 动态代理   │     │ 4. 网络接收   │
│ 2. 序列化     │     │ 5. 反序列化   │
│ 3. 网络发送   │     │ 6. 反射调用   │
│              │     │ 7. 返回结果   │
└──────┬───────┘     └──────┬───────┘
       │                     │
       └──────────────────────
              网络通信 (Netty)
```

**核心组件实现：**

```java
// 1. 服务暴露注解
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface RpcService {
    Class<?> interfaceClass() default void.class;
}

// 2. 动态代理——客户端调用远程服务
public class RpcProxyFactory {
    
    @SuppressWarnings("unchecked")
    public static <T> T create(Class<T> interfaceClass, String host, int port) {
        return (T) Proxy.newProxyInstance(
            interfaceClass.getClassLoader(),
            new Class[]{interfaceClass},
            (proxy, method, args) -> {
                // 封装请求
                RpcRequest request = RpcRequest.builder()
                    .interfaceName(interfaceClass.getName())
                    .methodName(method.getName())
                    .parameterTypes(method.getParameterTypes())
                    .parameters(args)
                    .build();
                
                // 序列化（JSON / Hessian / Protobuf）
                byte[] data = serializer.serialize(request);
                
                // 网络发送（Netty）
                RpcResponse response = nettyClient.send(data);
                
                if (response.getError() != null) {
                    throw new RuntimeException(response.getError());
                }
                return response.getResult();
            }
        );
    }
}

// 3. 服务端反射调用
public class RpcServerHandler extends SimpleChannelInboundHandler<byte[]> {
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, byte[] msg) {
        RpcRequest request = serializer.deserialize(msg, RpcRequest.class);
        
        // 从 Spring 容器获取服务实现
        Object serviceBean = applicationContext.getBean(request.getInterfaceName());
        
        // 反射调用
        Method method = serviceBean.getClass().getMethod(
            request.getMethodName(), request.getParameterTypes());
        Object result = method.invoke(serviceBean, request.getParameters());
        
        // 返回响应
        ctx.writeAndFlush(serializer.serialize(RpcResponse.success(result)));
    }
}
```

**关键设计点：**
- **协议设计**：自定义协议头（魔数、序列化类型、消息长度）+ 数据体
- **序列化选择**：Hessian < JSON < Protobuf（性能从低到高）
- **注册中心**：对接 Nacos/Zookeeper 实现服务发现
- **负载均衡**：随机、轮询、一致性哈希
- **超时重试**：连接超时和读超时分开配置

---

### Q8：你怎么理解 Spring 的声明式事务？项目中怎么选择合适的隔离级别？

**面试官意图：** 考察对事务隔离级别的理解以及实际项目中的权衡。

**完美解答：**

**事务隔离级别（4种）：**

| 隔离级别 | 脏读 | 不可重复读 | 幻读 | 性能 |
|---------|------|-----------|------|------|
| READ UNCOMMITTED | 可能 | 可能 | 可能 | 最高 |
| READ COMMITTED | 避免 | 可能 | 可能 | 高 |
| REPEATABLE READ | 避免 | 避免 | 可能 | 中 |
| SERIALIZABLE | 避免 | 避免 | 避免 | 最低 |

**项目中的选择：**

大部分业务使用 **READ COMMITTED**（MySQL 默认是 REPEATABLE READ，但很多公司改成 RC 级别），原因是：

1. 性能更优——不需要维护间隙锁
2. 满足大部分业务需求——我们可以通过代码逻辑避免不可重复读
3. 主从复制兼容性更好——RC 级别在 binlog 中产生更少的锁

**特定场景使用 REPEATABLE READ：**
- 对账系统——需要在一个事务中多次读取同一数据保证一致
- 报表统计——需要事务内的快照一致性

```java
@Transactional(isolation = Isolation.READ_COMMITTED)
public Order createOrder(OrderCreateReq req) {
    // 使用乐观锁解决并发问题，而不是依赖更高的隔离级别
    int rows = productMapper.deductStock(req.getProductId(), req.getQuantity(), req.getVersion());
    if (rows == 0) {
        throw new BusinessException("库存不足或数据已变更");
    }
    // 创建订单...
}
```

> 💡 实际项目中，**不要依赖数据库隔离级别解决高并发问题**。更推荐的做法是使用 READ COMMITTED + 乐观锁/分布式锁，既保证性能又保证数据正确性。

---

## 4. 场景题与故障排查

> 💡 考察实际解决问题的能力

### Q9：线上 Spring 事务不生效了，你怎么排查？

**面试官意图：** 考察对事务机制的理解深度，以及排查问题的思路。

**完美解答：**

我会按以下步骤排查：

**第一步：确认数据库和表引擎**
```sql
SHOW CREATE TABLE xxx; -- 确认是 InnoDB，不是 MyISAM
```

**第二步：确认方法的访问修饰符**
`@Transactional` 必须在 `public` 方法上才生效。Spring 的 AOP 不会代理 private 方法。

**第三步：确认调用方式**
看是否是同类方法内部调用——`insertA()` 中直接调用 `insertB()`，`this.insertB()` 不走代理。
```java
// 排查方法：把内部调用的方法改成注入自身代理调用
@Autowired
private OrderService self;
```

**第四步：确认异常类型和 rollbackFor**
```java
@Transactional(rollbackFor = Exception.class) // 必须显式指定！
```
如果不指定，`SQLException` 等受检异常默认不回滚。

**第五步：确认异常没有被 try-catch 吃掉**
```java
@Transactional
public void doSomething() {
    try {
        // 业务代码
    } catch (Exception e) {
        // 异常被吃了！事务不会回滚
        log.error("错误", e);
    }
}
```

**第六步：检查是否声明式事务和编程式事务混用**
如果在同一个方法里混合使用 `@Transactional` 和 `TransactionTemplate`，要确认它们用的是同一个 `PlatformTransactionManager`。

**第七步：看日志排查**
开启事务日志：
```yaml
logging:
  level:
    org.springframework.transaction: DEBUG
    org.springframework.jdbc.datasource: DEBUG
```

观察是否有 `Creating new transaction`、`Acquired Connection`、`Committing transaction` 等日志输出。

---

### Q10：线上某接口频繁超时，排查后发现是数据库连接池满了，怎么解决？

**面试官意图：** 考察对连接池管理和数据库性能的综合排查能力。

**完美解答：**

**问题分析：**
当数据库连接池满了，新的请求会阻塞等待连接释放，直到超时，错误日志会出现 `HikariPool-1 - Connection is not available, request timed out after 30000ms`。

**排查思路：**

**1. 确认连接池配置是否合理**
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20       # 最大连接数
      minimum-idle: 5             # 最小空闲连接
      connection-timeout: 30000   # 等待超时
      idle-timeout: 600000        # 空闲超时
      max-lifetime: 1800000       # 最大存活时间
```

最常用的判断：`最大连接数 = ((CPU核心数 * 2) + 有效磁盘数)`。但这个公式只是参考，更要看实际的压测数据。

**2. 排查 SQL 性能**
最可能的原因是慢 SQL 占用了连接。开启慢查询日志：
```sql
SET GLOBAL slow_query_log = ON;
SET GLOBAL long_query_time = 1;   -- 超过 1 秒的记录
```
找到慢 SQL 后用 `EXPLAIN` 分析执行计划，检查是否全表扫描、有没有走索引。

**3. 排查是否有连接泄漏**
Spring 的 `@Transactional` 如果没有正确配置，可能导致事务延长，连接不释放。

```java
// 问题模式：事务范围内做了远程调用，连接长时间被占用
@Transactional
public void processOrder(Order order) {
    orderMapper.update(order);
    httpClient.callRemoteService(); // 远程调用耗时 3 秒，连接一直被占用
}
```

**解决方案：**
```java
// 将远程调用移出事务
public void processOrder(Order order) {
    orderMapper.update(order);         // 事务范围内只操作数据库
    httpClient.callRemoteService();    // 无事务上下文
}
```

**4. 区分读写库**
如果查询量太大，可以做**读写分离**：读操作走从库，写操作走主库，分开连接池。

---

### Q11：Maven 依赖冲突导致项目启动报错，你怎么解决？

**面试官意图：** 考察对依赖管理的理解，特别是依赖冲突的排查解决能力。

**完美解答：**

**现象：** Spring 项目启动报 `NoSuchMethodError`、`ClassNotFoundException` 或 `AbstractMethodError`，通常是 jar 包版本冲突。

**排查步骤：**

**1. 定位冲突**
```bash
# 查看依赖树
mvn dependency:tree > tree.txt

# 查找特定依赖
mvn dependency:tree -Dincludes=com.fasterxml.jackson
```

**2. 分析冲突原因**
```text
[INFO] +- org.springframework.boot:spring-boot-starter-web:2.7.0
[INFO] |  \- com.fasterxml.jackson.core:jackson-databind:2.13.3
[INFO] +- com.alibaba:fastjson:1.2.83
[INFO] \- com.xxx:other-module:1.0
[INFO]    \- com.fasterxml.jackson.core:jackson-databind:2.12.5 (冲突)
```

两个不同版本的 `jackson-databind`，Maven 按"最短路径优先"原则选择 2.13.3，但业务代码可能需要 2.12.5 的某个 API。

**3. 解决方案**

**方案一：在 pom.xml 中显式排除**
```xml
<dependency>
    <groupId>com.xxx</groupId>
    <artifactId>other-module</artifactId>
    <version>1.0</version>
    <exclusions>
        <exclusion>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

**方案二：在父 POM 中统一版本管理**
```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.fasterxml.jackson</groupId>
            <artifactId>jackson-bom</artifactId>
            <version>2.13.3</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

> 💡 最佳实践是使用 Spring Boot 的 BOM（Bill of Materials）管理版本，Spring Boot 已经测试了所有内置组件的兼容性，尽量避免手动引入与 Spring Boot 版本不匹配的第三方依赖。

---

### Q12：Spring 应用启动慢，怎么排查和优化？

**面试官意图：** 考察对 Spring Boot 启动过程的理解以及性能优化经验。

**完美解答：**

**排查手段：**

**1. 开启启动过程分析**
```yaml
# 打印 Bean 加载耗时
logging:
  level:
    org.springframework.boot.autoconfigure: DEBUG
    org.springframework.context.support: DEBUG
```

或者使用 Spring Boot 2.4+ 的启动端点：
```yaml
# application.yaml
spring:
  application:
    startup:
      snapshot:
        name: startup-step.txt
```

启动后会在当前目录生成 `startup-step.txt`，包含每个 Bean 初始化的耗时排序。

**2. 常见瓶颈和优化方案**

| 瓶颈类型 | 典型表现 | 优化方案 |
|---------|---------|---------|
| 懒加载 Bean 过多 | 大量 Bean 在启动时初始化 | 使用 `@Lazy` 延迟初始化非必要 Bean |
| 数据库连接检查 | 等待数据库连接超时 | 确认网络连接正常，或设置 `spring.sql.init.continue-on-error=true` |
| 大量 @ComponentScan | 扫描了不必要包 | 指定精确扫描路径 |
| JPA 实体管理初始化 | 审计日志慢 | JPA 项目用 `spring.jpa.open-in-view=false` |
| 类过多 | 反射解析慢 | 考虑 Spring AOT（GraalVM Native Image） |

**3. 实际案例**

一次项目中启动花了 3 分钟，排查后发现：
- `@ComponentScan("com.xxx")` 扫描了整个包，包含了大量不相关的模块 → 改为只扫描需要的子包
- 某个 `@PostConstruct` 方法中做了远程调用，没有设置超时 → 增加超时，改为异步
- Redis 连接池初始化配置了连接测试 → 关闭连接测试

优化后启动时间从 3 分钟降到 15 秒。

---

## 💎 面试加分金句

1. "我理解 Spring 的核心思想是**约定优于配置、关注点分离**。IoC 分离了对象创建和使用，AOP 分离了横切关注点和业务逻辑，这两个思想在写任何代码时都可以借鉴。"
2. "手写 IoC 容器让我真正理解了 Spring 的设计哲学——**复杂的事情简单化**。反射、注解、代理、缓存，这些基础技术组合起来就是一个强大的框架。"
3. "Spring 事务的传播机制在生产环境中一定要谨慎使用，特别是 REQUIRES_NEW 和 NESTED。我建议团队**优先用 REQUIRED + 独立事务管理器**来实现复杂的隔离需求。"
4. "AOP 最难的不是写切面，而是**保证切面不会影响业务逻辑的正确性**。我每次加切面都会做充分的测试，确保异常情况下不会吞掉原始异常。"
5. "看 Spring 源码最好的方式是带着问题去读——比如循环依赖怎么解决的、事务怎么失效的。**从 bug 出发读源码**，比通篇通读效率高得多。"

---

## 📋 高频追问清单

| 追问方向 | 应对策略 |
|----------|----------|
| "Spring 和 SpringBoot 有什么区别？" | SpringBoot 是 Spring 的封装，简化了配置、内嵌了服务器、提供了自动配置、生态整合更好 |
| "BeanFactory 和 ApplicationContext 区别？" | 前者是核心容器，提供懒加载；后者继承前者，增加 AOP、事件、国际化等企业级功能 |
| "@Autowired 和 @Resource 区别？" | @Autowired 按类型注入（Spring 注解），@Resource 按名称注入（JSR-250 规范） |
| "Spring AOP 的通知类型有哪些？" | @Before、@AfterReturning、@AfterThrowing、@After、@Around |
| "Spring 事件机制用过吗？" | 基于观察者模式：@EventListener + 异步执行，用于解耦业务 |

---

## 🔗 关联知识点

- [SpringBoot必做项目清单-面试问答](SpringBoot必做项目清单-面试问答.md) — SpringBoot 自动配置与项目实战
- [SpringMVC必做项目清单-面试问答](SpringMVC必做项目清单-面试问答.md) — SpringMVC 请求处理与拦截器
- [Java高并发必做项目清单-面试问答](Java高并发必做项目清单-面试问答.md) — 高并发与分布式事务
