# Spring 高级面试宝典
> 基于 Spring 高级课程全面覆盖源码级面试考点，从 BeanFactory 底层到 AOP、SpringMVC、SpringBoot 完整启动流程

## 目录
1. [一、基础概念速答](#一基础概念速答15-20题)
2. [二、深度原理剖析](#二深度原理剖析10-15题)
3. [三、实战场景题](#三实战场景题8-12题)
4. [四、手写代码题](#四手写代码题5-8题)
5. [五、系统设计题](#五系统设计题3-5题)
6. [六、常见坑点与最佳实践](#六常见坑点与最佳实践)
7. [七、面试回答模板](#七面试回答模板top-5高频题的结构化回答模板)
8. [八、快速查漏补缺 Checklist](#八快速查漏补缺-checklist)

---

## 一、基础概念速答（15-20题）

### 1.1 BeanFactory 和 ApplicationContext 的功能区别（源码级）
| 维度 | BeanFactory | ApplicationContext |
|------|------------|-------------------|
| 接口层级 | 顶层容器接口 | 继承 BeanFactory、ListableBeanFactory、HierarchicalBeanFactory 等多个接口 |
| 事件发布 | 不提供 | 提供 ApplicationEventPublisher |
| 国际化 | 不提供 | 提供 MessageSource |
| 资源加载 | 不提供 | 提供 ResourcePatternResolver |
| Bean 后处理器 | 需手动注册 | 自动检测并注册 |
| 延迟加载 | 默认懒加载 | 默认立即加载 |

**ApplicationContext 额外功能**：
1. `EnvironmentCapable`：环境配置管理
2. `ApplicationEventPublisher`：事件发布
3. `MessageSource`：国际化
4. `ResourcePatternResolver`：资源加载

### 1.2 BeanFactory 的层次结构是怎样的？
```
BeanFactory（顶层接口）
  └── HierarchicalBeanFactory（父子容器支持）
        └── ConfigurableBeanFactory（配置能力：作用域、别名、后处理器）
              └── ListableBeanFactory（批量获取）
                    └── ApplicationContext（完整容器）
```

### 1.3 BeanFactory 的主要实现类有哪些？
| 实现类 | 说明 |
|--------|------|
| `DefaultListableBeanFactory` | 最核心实现，维护 BeanDefinition Map |
| `XmlBeanFactory` | 已废弃，用于 XML 配置 |
| `AnnotationConfigApplicationContext` | 基于注解的容器 |
| `ClassPathXmlApplicationContext` | 基于 XML 类路径配置 |
| `GenericApplicationContext` | 通用容器，可编程注册 BeanDefinition |

### 1.4 ApplicationContext 的实现类有哪些？
| 实现类 | 适用场景 |
|--------|---------|
| `AnnotationConfigApplicationContext` | 注解配置（最常用）|
| `ClassPathXmlApplicationContext` | XML 配置文件 |
| `FileSystemXmlApplicationContext` | 文件系统 XML |
| `AnnotationConfigWebApplicationContext` | Web 注解配置 |
| `XmlWebApplicationContext` | Web XML 配置 |

### 1.5 Spring 的 BeanDefinition 包含哪些关键信息？
```java
public interface BeanDefinition extends AttributeAccessor, BeanMetadataElement {
    String SCOPE_SINGLETON = ConfigurableBeanFactory.SCOPE_SINGLETON;
    String SCOPE_PROTOTYPE = ConfigurableBeanFactory.SCOPE_PROTOTYPE;
    
    // Bean 类名
    void setBeanClassName(String beanClassName);
    // 作用域
    void setScope(String scope);
    // 是否懒加载
    void setLazyInit(boolean lazyInit);
    // 初始化方法名
    void setInitMethodName(String initMethodName);
    // 销毁方法名
    void setDestroyMethodName(String destroyMethodName);
    // 构造器参数
    ConstructorArgumentValues getConstructorArgumentValues();
    // 属性值
    MutablePropertyValues getPropertyValues();
    // 工厂 Bean 名称
    void setFactoryBeanName(String factoryBeanName);
    // 工厂方法名
    void setFactoryMethodName(String factoryMethodName);
}
```

### 1.6 Spring 中常见 Bean 后处理器有哪些？各自作用是什么？
| 后处理器 | 作用 |
|---------|------|
| `AutowiredAnnotationBeanPostProcessor` | 处理 @Autowired、@Value、@Inject |
| `CommonAnnotationBeanPostProcessor` | 处理 @Resource、@PostConstruct、@PreDestroy |
| `RequiredAnnotationBeanPostProcessor` | 检查 @Required 属性是否注入（5.1 已弃用）|
| `PersistenceAnnotationBeanPostProcessor` | 处理 @PersistenceUnit、@PersistenceContext |
| `InitDestroyAnnotationBeanPostProcessor` | 处理 @PostConstruct、@PreDestroy |
| `ApplicationContextAwareProcessor` | 处理 Aware 接口回调 |
| `AsyncAnnotationBeanPostProcessor` | 处理 @Async |

### 1.7 常见的工厂后处理器有哪些？
| 工厂后处理器 | 作用 |
|------------|------|
| `ConfigurationClassPostProcessor` | 核心！处理 @Configuration、@ComponentScan、@Import |
| `PropertySourcesPlaceholderConfigurer` | 处理 @Value 占位符解析 |
| `CustomScopeConfigurer` | 注册自定义作用域 |
| `EventListenerMethodProcessor` | 处理 @EventListener |
| `MapperScannerConfigurer` | MyBatis Mapper 扫描 |

### 1.8 @Autowired 注解注入的完整流程是什么？
```java
// 核心步骤（由 AutowiredAnnotationBeanPostProcessor 处理）
1. buildAutowiringMetadata() — 解析 @Autowired/@Inject/@Value 字段和方法
2. postProcessPropertyValues() — 属性注入
   → InjectionMetadata.inject()
      → AutowiredFieldElement.inject()
         → beanFactory.resolveDependency()
            → 1. 按类型查找 @Qualifier @Primary
            → 2. 创建代理（如果依赖是 FactoryBean）
            → 3. 处理集合（List/Map）
            → 4. 处理 Optional/Provider
            → 5. 设置字段可访问并赋值
```

### 1.9 Spring AOP 中的 Advisor 是什么？
Advisor 是由 **Pointcut（切点）** 和 **Advice（通知）** 组成的完整切面。Spring 内部使用低级 Advisor（`InstantiationModelAwarePointcutAdvisor`）存储 AOP 配置。

```
@Aspect 注解的切面
  → 解析为多个 Advisor
     → 每个 @Before/@After/@Around 对应一个 Advisor
     → Pointcut（切点表达式）+ Advice（通知逻辑）
```

### 1.10 AOP 代理的创建时机和条件是什么？
```java
// 创建时机：Bean 初始化之后
AbstractAutoProxyCreator.postProcessAfterInitialization()

// 创建条件：Bean 存在匹配的 Advisor
protected Object wrapIfNecessary(Object bean, String beanName, Object cacheKey) {
    // 1. 检查是否已有代理（避免重复创建）
    if (bean instanceof AopInfrastructureBean) return bean;
    
    // 2. 查找匹配该 Bean 的 Advisor
    List<Advisor> advisors = findEligibleAdvisors(beanClass, beanName);
    
    // 3. 有匹配则创建代理
    if (!advisors.isEmpty()) {
        Object proxy = createProxy(bean.getClass(), beanName, advisors, bean);
        return proxy;
    }
    return bean;
}
```

### 1.11 JDK 动态代理的底层字节码生成原理
JDK 动态代理在运行时动态生成字节码文件，核心在 `ProxyGenerator.generateProxyClass()`。生成的代理类：
1. 继承 `Proxy`，实现目标接口
2. 包含所有接口方法的 `invoke` 调用
3. 通过 `InvocationHandler.invoke()` 回调
4. **性能优化**：JDK 8+ 使用 `ProxyGenerator` 生成优化后的字节码

### 1.12 CGLIB 动态代理的 FastClass 机制
CGLIB 通过 `Enhancer` 创建子类代理，同时生成两个类：
- **代理类**：增强目标类的方法，调用 `MethodInterceptor`
- **FastClass**：为每个方法建立索引，通过索引而不是反射调用，性能更高

```java
// CGLIB FastClass 原理
// 目标类有方法 foo() 和 bar()
// FastClass 维护索引: foo → 0, bar → 1
// 调用时：fastClass.invoke(0, obj, args) 避免反射
// 相比 JDK 反射优势：索引直接定位，无反射开销
```

### 1.13 SpringMVC 的 DispatcherServlet 初始化过程
```java
// HttpServletBean.init() → FrameworkServlet.initServletBean()
// → DispatcherServlet.onRefresh(ApplicationContext context)
// → initStrategies(context) 初始化 9 大组件

protected void initStrategies(ApplicationContext context) {
    initMultipartResolver(context);          // 文件上传解析器
    initLocaleResolver(context);              // 国际化解析器
    initThemeResolver(context);               // 主题解析器
    initHandlerMappings(context);             // 处理器映射器（核心）
    initHandlerAdapters(context);             // 处理器适配器（核心）
    initHandlerExceptionResolvers(context);   // 异常解析器
    initRequestToViewNameTranslator(context); // 请求到视图名翻译
    initViewResolvers(context);               // 视图解析器
    initFlashMapManager(context);             // FlashMap 管理器
}
```

### 1.14 HandlerMapping 和 HandlerAdapter 的作用
| 组件 | 作用 | 常用实现 |
|------|------|---------|
| `HandlerMapping` | 将请求 URL 映射到对应的 Handler | `RequestMappingHandlerMapping`、`SimpleUrlHandlerMapping` |
| `HandlerAdapter` | 执行 Handler 的方法 | `RequestMappingHandlerAdapter`、`HttpRequestHandlerAdapter`、`SimpleControllerHandlerAdapter` |

HandlerMapping 返回 `HandlerExecutionChain`（包含 Handler + 拦截器链），HandlerAdapter 负责真正的调用。

### 1.15 Spring 中的参数解析器和返回值处理器
**参数解析器（HandlerMethodArgumentResolver）**：
```java
public interface HandlerMethodArgumentResolver {
    boolean supportsParameter(MethodParameter parameter);  // 是否支持该参数
    Object resolveArgument(MethodParameter parameter, ...); // 解析参数值
}
// 实现：@RequestParam、@PathVariable、@RequestBody、@ModelAttribute 等
```

**返回值处理器（HandlerMethodReturnValueHandler）**：
```java
public interface HandlerMethodReturnValueHandler {
    boolean supportsReturnType(MethodParameter returnType);
    void handleReturnValue(Object returnValue, ...); // 处理返回值
}
// 实现：ModelAndView、@ResponseBody、@ModelAttribute 等
```

### 1.16 MessageConverter 消息转换器的作用
`HttpMessageConverter` 负责 HTTP 请求/响应消息与 Java 对象互相转换。

```java
public interface HttpMessageConverter<T> {
    boolean canRead(Class<?> clazz, MediaType mediaType);
    boolean canWrite(Class<?> clazz, MediaType mediaType);
    T read(Class<? extends T> clazz, HttpInputMessage inputMessage);
    void write(T t, MediaType contentType, HttpOutputMessage outputMessage);
}
// 常用实现：MappingJackson2HttpMessageConverter（JSON）
//            StringHttpMessageConverter（文本）
//            ByteArrayHttpMessageConverter（字节数组）
```

### 1.17 Spring 事件监听机制（完整流程）
```java
// 1. 事件 - ApplicationEvent
public class OrderEvent extends ApplicationEvent {
    private Long orderId;
    public OrderEvent(Object source, Long orderId) { super(source); }
}

// 2. 发布 - ApplicationEventPublisher
applicationEventPublisher.publishEvent(new OrderEvent(this, 1001L));

// 3. 监听 - @EventListener
@EventListener
public void onOrderEvent(OrderEvent event) { }
// 或实现 ApplicationListener<T>

// 4. 异步执行
@Async
@EventListener
public void asyncHandle(OrderEvent event) { }

// 5. 底层实现
// SimpleApplicationEventMulticaster.multicastEvent()
// → 遍历 ApplicationListener
// → 匹配泛型类型
// → 异步或同步执行
```

### 1.18 Spring 事务的 TransactionInterceptor 执行流程
```java
// TransactionInterceptor.invoke() 核心逻辑
1. 获取 @Transactional 的配置属性
2. 获取 PlatformTransactionManager
3. TransactionAspectSupport.invokeWithinTransaction()
   3.1 createTransactionIfNecessary()
       → 根据传播行为决定挂起/创建新事务
       → 设置隔离级别、超时、只读等
   3.2 执行目标方法（proceed）
   3.3 正常 → commitTransactionAfterReturning()
   3.4 异常 → completeTransactionAfterThrowing()
          → 根据 rollbackFor 判断是否回滚
4. 清理事务资源
```

### 1.19 @ControllerAdvice 的作用
`@ControllerAdvice` 是全局控制器增强，提供三个扩展点：
```java
@ControllerAdvice
public class GlobalControllerAdvice {
    // 全局异常处理
    @ExceptionHandler(Exception.class)
    public Result handle(Exception e) { }
    
    // 全局数据绑定
    @ModelAttribute
    public User currentUser() { }
    
    // 全局数据预处理
    @InitBinder
    public void initBinder(WebDataBinder binder) { }
    
    // 全局响应处理
    @InitBinder
    public void initBinder(WebDataBinder binder) { }
}
```

### 1.20 Spring 的类型转换机制（两套转换接口）
```java
// 第一套：底层 SPI 接口
public interface Converter<S, T> { T convert(S source); }

// 第二套：通用转换接口
public interface GenericConverter {
    Set<ConvertiblePair> getConvertibleTypes();
    Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType);
}

// 高层 API：ConversionService
public interface ConversionService {
    boolean canConvert(TypeDescriptor sourceType, TypeDescriptor targetType);
    Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType);
}

// 数据绑定：DataBinder 使用 ConversionService
// 自定义扩展：
@InitBinder
public void initBinder(WebDataBinder binder) {
    binder.addCustomFormatter(new DateFormatter("yyyy-MM-dd"));
}
```

---

## 二、深度原理剖析（10-15题）

### 2.1 DefaultListableBeanFactory 的底层实现
`DefaultListableBeanFactory` 是最核心的 IoC 容器实现，内部维护两个核心 Map：

```java
public class DefaultListableBeanFactory extends AbstractAutowireCapableBeanFactory
        implements ConfigurableListableBeanFactory, BeanDefinitionRegistry, Serializable {
    
    // BeanDefinition 注册表：beanName → BeanDefinition
    private final Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>(256);
    
    // 手动注册的单例 Bean
    private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>(256);
    
    // Bean 名称列表（按注册顺序）
    private volatile List<String> beanDefinitionNames = new ArrayList<>(256);
}
```

**核心方法**：
- `registerBeanDefinition()`：注册 Bean 定义
- `getBeanDefinition()`：获取 Bean 定义
- `getBean()`：获取/创建 Bean 实例
- `preInstantiateSingletons()`：提前实例化所有非懒加载单例

### 2.2 AbstractApplicationContext.refresh() 完整源码分析
```java
public void refresh() throws BeansException, IllegalStateException {
    synchronized (this.startupShutdownMonitor) {
        // 1. 准备刷新上下文（设置启动时间、验证必需属性）
        prepareRefresh();
        
        // 2. 获取 BeanFactory（GenericApplicationContext 的 beanFactory）
        ConfigurableListableBeanFactory beanFactory = obtainFreshBeanFactory();
        
        // 3. BeanFactory 预准备（设置类加载器、后处理器、忽略依赖接口等）
        prepareBeanFactory(beanFactory);
        
        try {
            // 4. BeanFactory 后置处理（允许子类在标准初始化前修改）
            postProcessBeanFactory(beanFactory);
            
            // 5. 调用 BeanFactoryPostProcessor（处理 @Configuration、@ComponentScan 等）
            invokeBeanFactoryPostProcessors(beanFactory);
            
            // 6. 注册 BeanPostProcessor（@Autowired、@Resource 等处理器的注册）
            registerBeanPostProcessors(beanFactory);
            
            // 7. 初始化消息源（国际化）
            initMessageSource();
            
            // 8. 初始化事件广播器
            initApplicationEventMulticaster();
            
            // 9. 模板方法，子类实现（Web 容器在此创建）
            onRefresh();
            
            // 10. 注册事件监听器
            registerListeners();
            
            // 11. 实例化所有非懒加载单例 Bean（核心步骤）
            finishBeanFactoryInitialization(beanFactory);
            
            // 12. 完成刷新（发布 ContextRefreshedEvent）
            finishRefresh();
            
        } catch (BeansException ex) {
            // 异常回滚——销毁已创建的 Bean
            destroyBeans();
            cancelRefresh(ex);
            throw ex;
        }
    }
}
```

### 2.3 三级缓存解决循环依赖的源码级分析
```java
// DefaultSingletonBeanRegistry 核心源码

protected Object getSingleton(String beanName, boolean allowEarlyReference) {
    // 1. 一级缓存查询
    Object singletonObject = this.singletonObjects.get(beanName);
    if (singletonObject == null && isSingletonCurrentlyInCreation(beanName)) {
        synchronized (this.singletonObjects) {
            // 2. 二级缓存查询
            singletonObject = this.earlySingletonObjects.get(beanName);
            if (singletonObject == null && allowEarlyReference) {
                // 3. 三级缓存获取工厂
                ObjectFactory<?> singletonFactory = this.singletonFactories.get(beanName);
                if (singletonFactory != null) {
                    // 4. 通过工厂获取早期引用（可能生成 AOP 代理）
                    singletonObject = singletonFactory.getObject();
                    // 5. 移到二级缓存
                    this.earlySingletonObjects.put(beanName, singletonObject);
                    this.singletonFactories.remove(beanName);
                }
            }
        }
    }
    return singletonObject;
}

// 为什么需要三级缓存而不是二级？
// 三级缓存放的是 ObjectFactory，在 AOP 场景下，
// 只有真正产生循环依赖时才调用 ObjectFactory.getObject()
// 生成代理对象。如果没有循环依赖，则在初始化后才生成代理（让 BeanPostProcessor 处理）
```

### 2.4 AOP 中 Advisor 查找和匹配源码
```java
// AbstractAdvisorAutoProxyCreator.findEligibleAdvisors()
protected List<Advisor> findEligibleAdvisors(Class<?> beanClass, String beanName) {
    // 1. 获取所有 Advisor
    List<Advisor> candidateAdvisors = findCandidateAdvisors();
    
    // 2. 匹配切点
    List<Advisor> eligibleAdvisors = findAdvisorsThatCanApply(candidateAdvisors, beanClass, beanName);
    
    // 3. 添加 ExposeInvocationInterceptor（暴露 MethodInvocation）
    extendAdvisors(eligibleAdvisors);
    
    // 4. 排序（@Order 影响执行顺序）
    if (!eligibleAdvisors.isEmpty()) {
        eligibleAdvisors = sortAdvisors(eligibleAdvisors);
    }
    return eligibleAdvisors;
}

// AopUtils.canApply() 切点匹配
public static boolean canApply(Pointcut pc, Class<?> targetClass, boolean hasIntroductions) {
    // 1. ClassFilter 匹配
    if (!pc.getClassFilter().matches(targetClass)) return false;
    
    // 2. MethodMatcher 匹配（遍历所有方法）
    MethodMatcher methodMatcher = pc.getMethodMatcher();
    for (Method method : targetClass.getMethods()) {
        if (methodMatcher.matches(method, targetClass)) return true;
    }
    return false;
}
```

### 2.5 AOP 代理的责任链（Interceptor Chain）执行过程
```java
// ReflectiveMethodInvocation.proceed() 核心逻辑
public Object proceed() throws Throwable {
    // currentInterceptorIndex 从 -1 开始，逐个推进
    if (this.currentInterceptorIndex == this.interceptorsAndDynamicMethodMatchers.size() - 1) {
        // 到了链尾，执行目标方法
        return invokeJoinpoint();
    }
    
    Object interceptorOrInterceptionAdvice = 
        this.interceptorsAndDynamicMethodMatchers.get(++this.currentInterceptorIndex);
    
    if (interceptorOrInterceptionAdvice instanceof InterceptorAndDynamicMethodMatcher) {
        // 动态方法匹配（看是否真的匹配当前方法）
        InterceptorAndDynamicMethodMatcher dm = (InterceptorAndDynamicMethodMatcher) interceptorOrInterceptionAdvice;
        if (dm.methodMatcher.matches(this.method, this.targetClass, this.arguments)) {
            return dm.interceptor.invoke(this);
        } else {
            // 不匹配，跳过
            return proceed();
        }
    } else {
        // MethodInterceptor 直接执行
        return ((MethodInterceptor) interceptorOrInterceptionAdvice).invoke(this);
    }
}
```

### 2.6 AdvisorAdapter 适配器模式
```java
// 将不同类型的 Advice 适配为 MethodInterceptor
public interface AdvisorAdapter {
    boolean supportsAdvice(Advice advice);           // 是否支持该通知类型
    MethodInterceptor getInterceptor(Advisor advisor); // 转换为拦截器
}

// 实现类
class MethodBeforeAdviceAdapter implements AdvisorAdapter {
    public boolean supportsAdvice(Advice advice) {
        return advice instanceof MethodBeforeAdvice;
    }
    public MethodInterceptor getInterceptor(Advisor advisor) {
        MethodBeforeAdvice advice = (MethodBeforeAdvice) advisor.getAdvice();
        return new MethodBeforeAdviceInterceptor(advice);
    }
}

class AfterReturningAdviceAdapter implements AdvisorAdapter { ... }
class ThrowsAdviceAdapter implements AdvisorAdapter { ... }
```

### 2.7 SpringMVC 控制器方法完整执行流程
```java
1. DispatcherServlet.doDispatch(HttpServletRequest request, HttpServletResponse response)
2.  → getHandler(request)          // HandlerMapping 找到 HandlerExecutionChain
3.  → getHandlerAdapter(handler)   // 找到匹配的 HandlerAdapter
4.  → applyPreHandle(chain, request, response)  // 前置拦截器
5.  → handlerAdapter.handle(request, response, handler)  // 执行目标方法
      → RequestMappingHandlerAdapter.handleInternal()
         → 读取 @RequestMapping 信息
         → 参数解析器解析参数（HandlerMethodArgumentResolver）
         → 通过反射执行方法
         → 返回值处理器处理返回（HandlerMethodReturnValueHandler）
6.  → applyPostHandle(chain, request, response)  // 后置拦截器
7.  → processDispatchResult()     // 处理结果（视图解析/异常处理）
      → render()                  // 视图渲染
```

### 2.8 RequestMappingHandlerAdapter 参数解析器解析过程
```java
// 请求参数解析器列表（默认注册 26 个解析器）
private List<HandlerMethodArgumentResolver> argumentResolvers;

// 解析流程
protected ModelAndView invokeHandlerMethod(HttpServletRequest request, HttpServletResponse response, 
                                            HandlerMethod handlerMethod) {
    // 1. 创建 InvocableHandlerMethod
    InvocableHandlerMethod invocableMethod = createInvocableHandlerMethod(handlerMethod);
    
    // 2. 设置参数解析器
    invocableMethod.setHandlerMethodArgumentResolvers(this.argumentResolvers);
    
    // 3. 设置返回值处理器
    invocableMethod.setHandlerMethodReturnValueHandlers(this.returnValueHandlers);
    
    // 4. 执行方法
    invocableMethod.invokeAndHandle(webRequest, mavContainer, ...);
}

// InvocableHandlerMethod 的参数解析
protected Object[] getMethodArgumentValues(NativeWebRequest request, ModelAndViewContainer container, 
                                           Object... providedArgs) {
    MethodParameter[] parameters = getMethodParameters();
    Object[] args = new Object[parameters.length];
    for (int i = 0; i < parameters.length; i++) {
        // 遍历所有参数解析器，找到支持的
        for (HandlerMethodArgumentResolver resolver : this.resolvers) {
            if (resolver.supportsParameter(parameters[i])) {
                args[i] = resolver.resolveArgument(parameters[i], ...);
                break;
            }
        }
    }
    return args;
}
```

### 2.9 Spring Boot 启动流程（构造阶段源码级）
```java
// 构造阶段（new SpringApplication()）
public SpringApplication(ResourceLoader resourceLoader, Class<?>... primarySources) {
    // 1. 设置主配置类
    this.primarySources = new LinkedHashSet<>(Arrays.asList(primarySources));
    
    // 2. 推断 Web 应用类型（从 classpath 检查）
    this.webApplicationType = WebApplicationType.deduceFromClasspath();
    
    // 3. 加载引导注册器（spring.factories 的 Bootstrapper）
    
    // 4. 加载 ApplicationContextInitializer（SPI 机制）
    setInitializers((Collection) getSpringFactoriesInstances(
            ApplicationContextInitializer.class));
    
    // 5. 加载 ApplicationListener（SPI 机制）
    setListeners((Collection) getSpringFactoriesInstances(ApplicationListener.class));
    
    // 6. 推断主启动类（通过堆栈信息找到 main 方法所在类）
    this.mainApplicationClass = deduceMainApplicationClass();
}
```

### 2.10 Spring Boot 启动流程（run 方法源码级）
```java
public ConfigurableApplicationContext run(String... args) {
    // 1. 启动计时器
    StopWatch stopWatch = new StopWatch();
    stopWatch.start();
    
    // 2. 创建引导上下文
    DefaultBootstrapContext bootstrapContext = createBootstrapContext();
    
    // 3. 配置 headless
    configureHeadlessProperty();
    
    // 4. 获取运行监听器
    SpringApplicationRunListeners listeners = getRunListeners(args);
    
    // 5. 发布 ApplicationStartingEvent
    listeners.starting(bootstrapContext);
    
    try {
        ApplicationArguments applicationArguments = new DefaultApplicationArguments(args);
        
        // 6. 准备环境（加载配置）
        ConfigurableEnvironment environment = prepareEnvironment(listeners, bootstrapContext, applicationArguments);
        
        // 7. 打印 Banner
        printBanner(environment);
        
        // 8. 创建 ApplicationContext
        context = createApplicationContext();
        
        // 9. 准备上下文
        prepareContext(bootstrapContext, context, environment, listeners, applicationArguments, printedBanner);
        
        // 10. 刷新上下文（调用 AbstractApplicationContext.refresh()）
        refreshContext(context);
        
        // 11. 刷新后处理
        afterRefresh(context, applicationArguments);
        
        stopWatch.stop();
        
        // 12. 发布 ApplicationReadyEvent
        listeners.started(context);
        
        // 13. 调用 Runner
        callRunners(context, applicationArguments);
        
    } catch (Throwable ex) {
        handleRunFailure(context, ex, listeners);
        throw new IllegalStateException(ex);
    }
    
    return context;
}
```

### 2.11 Spring Boot 自动配置类加载（AutoConfiguration.imports 机制）
```java
// Spring Boot 2.7+ 使用新的自动配置机制

// AutoConfigurationImportSelector.selectImports()
public String[] selectImports(AnnotationMetadata annotationMetadata) {
    if (!isEnabled(annotationMetadata)) {
        return NO_IMPORTS;
    }
    // 1. 获取 AutoConfigurationEntry
    AutoConfigurationEntry autoConfigurationEntry = getAutoConfigurationEntry(annotationMetadata);
    return StringUtils.toStringArray(autoConfigurationEntry.getConfigurations());
}

protected AutoConfigurationEntry getAutoConfigurationEntry(AnnotationMetadata annotationMetadata) {
    // 2. 检查是否启用
    if (!isEnabled(annotationMetadata)) {
        return EMPTY_ENTRY;
    }
    
    // 3. 获取注解属性（exclude, excludeName）
    AnnotationAttributes attributes = getAttributes(annotationMetadata);
    
    // 4. 加载候选配置
    List<String> configurations = getCandidateConfigurations(annotationMetadata, attributes);
    
    // 5. 去重
    configurations = removeDuplicates(configurations);
    
    // 6. 排除
    Set<String> exclusions = getExclusions(annotationMetadata, attributes);
    configurations.removeAll(exclusions);
    
    // 7. 过滤（@Conditional 条件匹配）
    configurations = filter(configurations, autoConfigurationMetadata);
    
    // 8. 触发 AutoConfigurationImportEvent
    fireAutoConfigurationImportEvents(configurations, exclusions);
    
    return new AutoConfigurationEntry(configurations, exclusions);
}

// 加载 spring.factories 或 AutoConfiguration.imports
protected List<String> getCandidateConfigurations(AnnotationMetadata metadata, AnnotationAttributes attributes) {
    List<String> configurations = SpringFactoriesLoader.loadFactoryNames(
            getSpringFactoriesLoaderFactoryClass(), getBeanClassLoader());
    Assert.notEmpty(configurations, "No auto configuration classes found");
    return configurations;
}
```

### 2.12 @Conditional 条件装配底层实现
```java
// ConfigurationClassParser 解析配置类时
public void parse(Set<BeanDefinitionHolder> configCandidates) {
    for (BeanDefinitionHolder holder : configCandidates) {
        BeanDefinition bd = holder.getBeanDefinition();
        if (bd instanceof AnnotatedBeanDefinition) {
            parse(((AnnotatedBeanDefinition) bd).getMetadata(), holder.getBeanName());
        }
    }
}

// 条件评估 — ConditionEvaluator.shouldSkip()
public boolean shouldSkip(AnnotatedTypeMetadata metadata, ConfigurationPhase phase) {
    // 1. 获取 @Conditional 注解
    for (AnnotationAttributes attributes : 
         getAllAnnotationAttributes(metadata, Conditional.class.getName())) {
        
        // 2. 获取条件类
        Class<?>[] conditionClasses = attributes.getClassArray("value");
        
        // 3. 实例化并执行 matches()
        for (Class<?> conditionClass : conditionClasses) {
            Condition condition = (Condition) BeanUtils.instantiateClass(conditionClass);
            ConfigurationPhase annotationPhase = getPhase(condition);
            
            if (annotationPhase == null || annotationPhase == phase) {
                if (!condition.matches(this.context, metadata)) {
                    return true; // 条件不满足，跳过
                }
            }
        }
    }
    return false;
}

// 以 OnClassCondition 为例
// 在解析配置阶段之前，通过 AutoConfigurationImportFilter 过滤
// 使用 spring-autoconfigure-metadata.properties 缓存条件
// 避免加载不必要的类，提高启动速度
```

### 2.13 @Value 底层注入原理
```java
// AutowiredAnnotationBeanPostProcessor 处理 @Value
// 但占位符解析需要 PropertySourcesPlaceholderConfigurer

// 处理流程：
1. PropertySourcesPlaceholderConfigurer 作为 BeanFactoryPostProcessor
2. 在 postProcessBeanFactory() 时
3.   → 读取所有 BeanDefinition 中的 PropertyValues
4.   → 使用 StringValueResolver 解析 ${...} 占位符
5.   → 替换为 Environment 中的实际值

// @Value + SpEL 表达式（如 #{systemProperties['user.region']}）
// 由 StandardBeanExpressionResolver 解析

// AutowiredAnnotationBeanPostProcessor 处理 @Value 注入：
// 1. 构建元数据时发现 @Value 注解
// 2. resolveDependency() 时解析值
// 3. 类型转换（由 TypeConverter 实现）
```

### 2.14 Spring 事件发布器底层实现
```java
// SimpleApplicationEventMulticaster.multicastEvent()
public void multicastEvent(ApplicationEvent event, @Nullable ResolvableType eventType) {
    ResolvableType type = (eventType != null) ? eventType : resolveDefaultEventType(event);
    Executor executor = getTaskExecutor(); // 获取异步执行器
    for (ApplicationListener<?> listener : getApplicationListeners(event, type)) {
        if (executor != null) {
            executor.execute(() -> invokeListener(listener, event)); // 异步发布
        } else {
            invokeListener(listener, event); // 同步发布
        }
    }
}

// 监听器匹配（Bridge 方法支持泛型事件）
// ApplicationListenerMethodAdapter 处理 @EventListener
// → 使用 ResolvableType 泛型匹配
// → SpEL 条件过滤：@EventListener(condition = "#event.orderId > 100")
// → @Async 异步执行

// 事件发布流程
// publishEvent() → getApplicationEventMulticaster()
// → multicastEvent() → invokeListener()
// → doInvokeListener() → listener.onApplicationEvent(event)
```

---

## 三、实战场景题（8-12题）

### 3.1 Spring 中如何禁用某个自动配置？
```java
// 方式一：@SpringBootApplication 排除
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})

// 方式二：配置文件排除
spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration

// 方式三：@EnableAutoConfiguration 排除
@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class})

// 方式四：条件判断覆盖
@Bean
@ConditionalOnMissingBean(DataSource.class)
public DataSource myDataSource() { }
```

### 3.2 如何自定义 Spring Boot 的 Banner？
```java
// 方式一：在 resources 下创建 banner.txt
// 方式二：实现 Banner 接口
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        new SpringApplicationBuilder(Application.class)
            .bannerMode(Banner.Mode.CONSOLE)
            .banner((environment, sourceClass, out) -> {
                out.println("  ____ _ _       _    _ ");
                out.println(" / ___(_) |_ ___| |  | |");
                out.println("| |  _| | __/ _ \\ |  | |");
                out.println("| |_| | | ||  __/ |__| |");
                out.println(" \\____|_|\\__\\___|\\____/");
            })
            .run(args);
    }
}
```

### 3.3 如何实现 Spring Bean 的异步初始化？
```java
// 方式一：@Async + @PostConstruct（不推荐）
@Component
public class AsyncInitializer {
    @Async
    @PostConstruct
    public void init() {
        // 耗时的初始化操作
    }
}

// 方式二：实现 SmartInitializingSingleton
@Component
public class MyInitializer implements SmartInitializingSingleton {
    @Override
    public void afterSingletonsInstantiated() {
        // 所有单例 Bean 创建完成后执行
    }
}

// 方式三：实现 ApplicationListener<ContextRefreshedEvent>
@Component
public class AppListener implements ApplicationListener<ContextRefreshedEvent> {
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        // 容器刷新完成后执行
    }
}
```

### 3.4 如何在 Spring 中实现 Bean 的动态注册？
```java
// 方式一：通过 BeanDefinitionRegistryPostProcessor
@Component
public class DynamicBeanRegistrar implements BeanDefinitionRegistryPostProcessor {
    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
        // 编程式注册 Bean
        GenericBeanDefinition bd = new GenericBeanDefinition();
        bd.setBeanClass(UserService.class);
        bd.getPropertyValues().add("prefix", "dynamic-");
        registry.registerBeanDefinition("dynamicUserService", bd);
    }
}

// 方式二：通过 ImportBeanDefinitionRegistrar
public class MyRegistrar implements ImportBeanDefinitionRegistrar {
    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, 
                                        BeanDefinitionRegistry registry) {
        // 根据条件注册
    }
}

// 方式三：使用 BeanFactory.registerSingleton()
@Bean
public InitializingBean registerDynamicBean(ConfigurableListableBeanFactory beanFactory) {
    return () -> beanFactory.registerSingleton("dynamicBean", new Object());
}
```

### 3.5 Spring AOP 中同一切面内方法互相调用不生效
```java
@Service
public class UserServiceImpl implements UserService {
    
    @Override
    public void methodA() {
        // 直接调用 methodB() — AOP 失效
        this.methodB(); 
    }
    
    @Override
    @Transactional
    public void methodB() {
        // 事务不会生效！
    }
    
    // ====== 解决方案 ======
    // 方案一：注入自身代理
    @Autowired
    private UserService self;
    
    @Override
    public void methodA() {
        self.methodB(); // 通过代理调用
    }
    
    // 方案二：使用 AopContext
    @Override
    public void methodA() {
        ((UserService) AopContext.currentProxy()).methodB();
    }
    
    // 方案三：拆分为不同 Service
}
```

### 3.6 Spring 中如何获取泛型参数？
```java
// 方式一：使用 ResolvableType（Spring 4+）
public abstract class BaseController<T extends BaseEntity> {
    private final Class<T> entityClass;
    
    public BaseController() {
        // 获取泛型参数
        this.entityClass = (Class<T>) ResolvableType
                .forClass(getClass())
                .getSuperType()
                .getGeneric(0)
                .resolve();
    }
}

// 方式二：使用 ParameterizedType
public abstract class BaseService<T> {
    private Class<T> clazz;
    
    @SuppressWarnings("unchecked")
    public BaseService() {
        Type type = getClass().getGenericSuperclass();
        if (type instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType) type;
            clazz = (Class<T>) pType.getActualTypeArguments()[0];
        }
    }
}
```

### 3.7 SpringMVC 中的异常处理机制
```java
// 异常处理层级（优先级从高到低）
// 1. @ExceptionHandler（当前 Controller）
// 2. @ControllerAdvice + @ExceptionHandler（全局）
// 3. HandlerExceptionResolver 链
// 4. Tomcat 的 error-page 配置

// HandlerExceptionResolver 实现类
// 1. ExceptionHandlerExceptionResolver — 处理 @ExceptionHandler
// 2. ResponseStatusExceptionResolver — 处理 @ResponseStatus
// 3. DefaultHandlerExceptionResolver — 处理 SpringMVC 标准异常

// DispatcherServlet.processHandlerException()
protected ModelAndView processHandlerException(HttpServletRequest request, 
                                               HttpServletResponse response,
                                               @Nullable Object handler, Exception ex) {
    ModelAndView exMv = null;
    // 遍历 HandlerExceptionResolver 链
    for (HandlerExceptionResolver resolver : this.handlerExceptionResolvers) {
        exMv = resolver.resolveException(request, response, handler, ex);
        if (exMv != null) break;
    }
    if (exMv != null) {
        if (exMv.isEmpty()) {
            request.setAttribute(EXCEPTION_ATTRIBUTE, ex);
            return null; // 已处理
        }
        return exMv;
    }
    throw ex; // 未处理，交由容器
}
```

### 3.8 SpringMVC 中拦截器和过滤器的区别
| 维度 | Filter | Interceptor |
|------|--------|-------------|
| 规范 | Servlet 规范 | Spring 框架 |
| 作用范围 | 所有 URL（含静态资源）| 仅 Controller 请求 |
| 上下文 | 无 Spring 上下文 | 可获取 Spring Bean |
| 粒度 | 请求级别 | 方法级别 |
| 执行顺序 | Servlet 容器执行 | Spring 容器执行 |

### 3.9 Spring 中如何全局处理响应格式？
```java
// 方式一：统一包装（ResponseBodyAdvice）
@ControllerAdvice
public class ResponseWrapper implements ResponseBodyAdvice<Object> {
    
    @Override
    public boolean supports(MethodParameter returnType, Class converterType) {
        // 跳过已经包装的或者无需包装的
        return !returnType.getParameterType().equals(Result.class);
    }
    
    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType,
                                  MediaType selectedContentType, Class selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        if (body instanceof Result) return body;
        return Result.success(body);
    }
}

// 方式二：AOP 环绕通知（更灵活但性能略低）
@Around("execution(* com.example.controller.*.*(..))")
public Object wrapResponse(ProceedingJoinPoint pjp) throws Throwable {
    Object result = pjp.proceed();
    if (result instanceof Result) return result;
    return Result.success(result);
}
```

---

## 四、手写代码题（5-8题）

### 4.1 手写简化版 DefaultListableBeanFactory
```java
public class SimpleBeanFactory {
    private final Map<String, Class<?>> beanClassMap = new ConcurrentHashMap<>();
    private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>();
    private final Map<String, Object> earlySingletonObjects = new ConcurrentHashMap<>();
    private final Map<String, ObjectFactory<?>> singletonFactories = new ConcurrentHashMap<>();
    
    // 注册 Bean 定义
    public void registerBean(String name, Class<?> clazz) {
        beanClassMap.put(name, clazz);
    }
    
    // 获取 Bean
    public Object getBean(String name) {
        // 1. 一级缓存
        Object bean = singletonObjects.get(name);
        if (bean != null) return bean;
        
        // 2. 二级缓存
        bean = earlySingletonObjects.get(name);
        if (bean != null) return bean;
        
        // 3. 三级缓存
        ObjectFactory<?> factory = singletonFactories.get(name);
        if (factory != null) {
            bean = factory.getObject();
            earlySingletonObjects.put(name, bean);
            singletonFactories.remove(name);
            return bean;
        }
        
        // 4. 创建 Bean
        return createBean(name);
    }
    
    private Object createBean(String name) {
        Class<?> clazz = beanClassMap.get(name);
        try {
            // 实例化
            Object instance = clazz.getDeclaredConstructor().newInstance();
            
            // 放入三级缓存（暴露早期引用，用于解决循环依赖）
            singletonFactories.put(name, () -> instance);
            
            // 属性注入（简化处理）
            processInjection(instance);
            
            // 初始化
            singletonObjects.put(name, instance);
            earlySingletonObjects.remove(name);
            singletonFactories.remove(name);
            
            return instance;
        } catch (Exception e) {
            throw new RuntimeException("Bean 创建失败: " + name, e);
        }
    }
}
```

### 4.2 手写简化版 AOP 代理
```java
public class SimpleAop {
    
    // JDK 动态代理封装
    @SuppressWarnings("unchecked")
    public static <T> T createProxy(T target, MethodInterceptor interceptor) {
        return (T) Proxy.newProxyInstance(
                target.getClass().getClassLoader(),
                target.getClass().getInterfaces(),
                (proxy, method, args) -> {
                    // 构建 MethodInvocation
                    MethodInvocation invocation = new MethodInvocation() {
                        @Override
                        public Object proceed() throws Throwable {
                            return method.invoke(target, args);
                        }
                        @Override
                        public Object getThis() { return target; }
                        @Override
                        public Method getMethod() { return method; }
                        @Override
                        public Object[] getArguments() { return args; }
                    };
                    return interceptor.invoke(invocation);
                });
    }
    
    // CGLIB 动态代理封装
    @SuppressWarnings("unchecked")
    public static <T> T createCglibProxy(Class<T> targetClass, MethodInterceptor interceptor) {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(targetClass);
        enhancer.setCallback((org.springframework.cglib.proxy.MethodInterceptor)
                (obj, method, args, proxy) -> interceptor.invoke(
                        new MethodInvocation() {
                            @Override
                            public Object proceed() throws Throwable {
                                return proxy.invokeSuper(obj, args);
                            }
                            @Override
                            public Object getThis() { return obj; }
                            @Override
                            public Method getMethod() { return method; }
                            @Override
                            public Object[] getArguments() { return args; }
                        }
                ));
        return (T) enhancer.create();
    }
}
```

### 4.3 手写简化版 DispatcherServlet
```java
public class SimpleDispatcherServlet extends HttpServlet {
    
    private List<HandlerMapping> handlerMappings;
    private List<HandlerAdapter> handlerAdapters;
    
    @Override
    public void init() {
        // 初始化处理器映射器
        handlerMappings = new ArrayList<>();
        handlerMappings.add(new RequestMappingHandlerMapping());
        
        // 初始化处理器适配器
        handlerAdapters = new ArrayList<>();
        handlerAdapters.add(new RequestMappingHandlerAdapter());
    }
    
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        try {
            doDispatch(req, resp);
        } catch (Exception e) {
            processHandlerException(req, resp, e);
        }
    }
    
    protected void doDispatch(HttpServletRequest request, HttpServletResponse response) {
        // 1. 查找 Handler
        String url = request.getRequestURI();
        HandlerExecutionChain chain = null;
        for (HandlerMapping mapping : handlerMappings) {
            chain = mapping.getHandler(request);
            if (chain != null) break;
        }
        
        // 2. 查找适配器并执行
        for (HandlerAdapter adapter : handlerAdapters) {
            if (adapter.supports(chain.getHandler())) {
                ModelAndView mv = adapter.handle(request, response, chain.getHandler());
                return;
            }
        }
    }
}
```

### 4.4 手写简化版 @Autowired 注入处理器
```java
public class SimpleAutowiredBeanPostProcessor implements BeanPostProcessor, PriorityOrdered {
    
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        Class<?> clazz = bean.getClass();
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(Autowired.class)) {
                field.setAccessible(true);
                try {
                    // 从容器中获取依赖
                    String dependencyName = lowerFirst(field.getType().getSimpleName());
                    Object dependency = getBeanFactory().getBean(dependencyName);
                    field.set(bean, dependency);
                } catch (Exception e) {
                    throw new RuntimeException("自动注入失败: " + field.getName(), e);
                }
            }
        }
        return bean;
    }
    
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        return bean;
    }
}
```

### 4.5 手写简化版 TransactionInterceptor
```java
public class SimpleTransactionInterceptor implements MethodInterceptor {
    
    private PlatformTransactionManager transactionManager;
    
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        // 获取事务注解
        Method method = invocation.getMethod();
        Transactional transactional = method.getAnnotation(Transactional.class);
        
        if (transactional == null) {
            return invocation.proceed();
        }
        
        // 创建事务
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setPropagationBehavior(transactional.propagation().value());
        def.setIsolationLevel(transactional.isolation().value());
        def.setTimeout(transactional.timeout());
        def.setReadOnly(transactional.readOnly());
        
        TransactionStatus status = transactionManager.getTransaction(def);
        
        try {
            Object result = invocation.proceed();
            transactionManager.commit(status);
            return result;
        } catch (Exception e) {
            // 判断是否需要回滚
            for (Class<?> rollbackFor : transactional.rollbackFor()) {
                if (rollbackFor.isInstance(e)) {
                    transactionManager.rollback(status);
                    throw e;
                }
            }
            transactionManager.commit(status);
            throw e;
        }
    }
}
```

### 4.6 手写简化版 Spring Boot 自动配置
```java
// 1. 条件注解
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConditionalOnClass {
    String value();
}

// 2. 自动配置加载器
public class AutoConfigurationLoader {
    
    public List<Class<?>> loadConfigurations() {
        List<Class<?>> configurations = new ArrayList<>();
        try {
            // 读取 META-INF/auto-configuration.imports
            Enumeration<URL> resources = getClass().getClassLoader()
                    .getResources("META-INF/auto-configuration.imports");
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(url.openStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (!line.startsWith("#")) {
                            configurations.add(Class.forName(line.trim()));
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("加载自动配置失败", e);
        }
        return configurations;
    }
    
    public void applyConfigurations(BeanDefinitionRegistry registry) {
        List<Class<?>> candidates = loadConfigurations();
        for (Class<?> configClass : candidates) {
            if (shouldSkip(configClass)) continue;
            // 注册为 BeanDefinition
            GenericBeanDefinition bd = new GenericBeanDefinition();
            bd.setBeanClass(configClass);
            registry.registerBeanDefinition(
                    lowerFirst(configClass.getSimpleName()), bd);
        }
    }
    
    private boolean shouldSkip(Class<?> configClass) {
        // 检查是否有 @ConditionalOnClass
        if (configClass.isAnnotationPresent(ConditionalOnClass.class)) {
            ConditionalOnClass condition = configClass.getAnnotation(ConditionalOnClass.class);
            try {
                Class.forName(condition.value());
                return false;
            } catch (ClassNotFoundException e) {
                return true; // 类不存在，跳过
            }
        }
        return false;
    }
}
```

---

## 五、系统设计题（3-5题）

### 5.1 设计一个支持动态配置的 Spring 事务管理器
```java
public class DynamicTransactionManager implements PlatformTransactionManager {
    
    private final Map<String, PlatformTransactionManager> managers = new ConcurrentHashMap<>();
    
    public void registerTransactionManager(String dsName, PlatformTransactionManager tm) {
        managers.put(dsName, tm);
    }
    
    @Override
    public TransactionStatus getTransaction(TransactionDefinition definition) {
        String ds = DynamicDataSourceContextHolder.get();
        PlatformTransactionManager tm = managers.get(ds);
        if (tm == null) throw new IllegalStateException("未知数据源: " + ds);
        return tm.getTransaction(definition);
    }
    
    @Override
    public void commit(TransactionStatus status) {
        getCurrentManager().commit(status);
    }
    
    @Override
    public void rollback(TransactionStatus status) {
        getCurrentManager().rollback(status);
    }
    
    private PlatformTransactionManager getCurrentManager() {
        String ds = DynamicDataSourceContextHolder.get();
        return managers.get(ds);
    }
}
```

### 5.2 设计一个基于 Spring 事件机制的分布式事件总线
```java
@Configuration
public class DistributedEventBusConfig {
    
    @Bean
    public ApplicationEventMulticaster distributedEventMulticaster() {
        SimpleApplicationEventMulticaster multicaster = new SimpleApplicationEventMulticaster();
        // 设置异步执行器
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.initialize();
        multicaster.setTaskExecutor(executor);
        
        // 添加错误处理器
        multicaster.setErrorHandler(t -> {
            log.error("事件处理异常", t);
            // 发送到死信队列
            deadLetterQueue.send(t);
        });
        return multicaster;
    }
    
    // 跨服务事件发布（MQ 实现）
    @Bean
    public ApplicationListener<RemoteEvent> remoteEventListener() {
        return event -> {
            // 序列化事件并通过 MQ 发送到其他服务
            rabbitTemplate.convertAndSend("event.exchange", "event.route", event);
        };
    }
}
```

### 5.3 设计一个 Spring 应用的性能分析器（Profiler）
```java
@Aspect
@Component
public class PerformanceProfiler {
    
    private final MeterRegistry meterRegistry;
    private final Map<String, LongSummaryStatistics> stats = new ConcurrentHashMap<>();
    
    @Around("@annotation(Profiled)")
    public Object profile(ProceedingJoinPoint pjp) throws Throwable {
        String signature = pjp.getSignature().toShortString();
        long start = System.nanoTime();
        
        try {
            return pjp.proceed();
        } finally {
            long duration = System.nanoTime() - start;
            long ms = TimeUnit.NANOSECONDS.toMillis(duration);
            
            // 记录到 Micrometer
            meterRegistry.timer("method.execution.time", "method", signature)
                    .record(duration, TimeUnit.NANOSECONDS);
            
            // 统计
            stats.computeIfAbsent(signature, k -> new LongSummaryStatistics())
                    .accept(ms);
            
            // 慢调用日志
            if (ms > 1000) {
                log.warn("慢调用: {} 耗时 {}ms", signature, ms);
            }
        }
    }
    
    public Map<String, String> getStatistics() {
        return stats.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> String.format("avg=%.2fms max=%dms count=%d",
                                e.getValue().getAverage(),
                                e.getValue().getMax(),
                                e.getValue().getCount())));
    }
}
```

---

## 六、常见坑点与最佳实践（表格）

| 坑点 | 问题描述 | 源码分析 | 解决方案 | 最佳实践 |
|------|---------|---------|---------|---------|
| @Autowired 注入静态字段失败 | Spring 不支持静态字段注入 | `AutowiredAnnotationBeanPostProcessor` 不会处理 static 字段 | 用 setter 方法接收后赋值给静态字段 | 不要用 @Autowired 注入静态字段 |
| @Transactional 同类方法调用失效 | this.method() 不走代理 | AOP 代理是 BeanPostProcessor 后创建的，this 指向原始对象 | 注入自己、AopContext.currentProxy()、拆分类 | 事务方法单独放在 Service 层 |
| Bean 初始化顺序不可控 | 依赖 Bean 还未创建 | Spring 使用拓扑排序处理 depens-on | @DependsOn 注解 | 使用 ApplicationListener 或 @PostConstruct |
| prototype Bean 注入到单例失效 | prototype 在单例中只注入一次 | 单例在创建时注入，prototype 被固定下来 | 使用 @Lookup、ObjectFactory、Provider | 用 Provider 或 Scope-proxy |
| 循环依赖构造器注入失败 | 构造器在实例化阶段就需要依赖 | 三级缓存在实例化后创建，构造器注入发生在实例化阶段 | 改用 setter 注入或 @Lazy | 优先用 setter 注入解决循环依赖 |
| @Value 注入失败 | 占位符未解析 | PropertySourcesPlaceholderConfigurer 未注册 | 确保配置类上有 @PropertySource | 统一用 application.yml |
| CGLIB 代理导致 final 方法不可调用 | CGLIB 生成子类，无法重写 final 方法 | Enhancer 跳过 final 方法 | 去掉方法 final 修饰符 | 避免在代理类中使用 final |
| ApplicationListener 阻塞启动 | 监听器执行时间过长 | refresh() 中 finishRefresh() 会同步触发 | 监听方法加 @Async | 启动监听器谨慎使用 |
| Spring Boot 2.6+ 循环依赖禁止启动 | 2.6 默认不允许循环引用 | 新增 `spring.main.allow-circular-references` | 开启允许或重构 | 尽量重构消除循环依赖 |
| FactoryBean 返回类型不匹配 | getObject() 与容器期望类型不一致 | FactoryBean 返回的是 getObject() 类型 | 检查 FactoryBean 泛型 | 明确实现 getObjectType() |

---

## 七、面试回答模板（Top 5高频题的结构化回答模板）

### 7.1 Spring Bean 生命周期（源码级）
**回答结构**（按 refresh 流程走）：
1. **实例化**：`AbstractAutowireCapableBeanFactory.createBeanInstance()` → 构造器/工厂
2. **属性注入**：`populateBean()` → 调用 BeanPostProcessor 处理 @Autowired
3. **Aware 接口**：`invokeAwareMethods()` → BeanNameAware、BeanFactoryAware
4. **初始化前**：`BeanPostProcessor.postProcessBeforeInitialization()`
5. **初始化**：`invokeInitMethods()` → @PostConstruct → InitializingBean.afterPropertiesSet() → init-method
6. **初始化后**：`BeanPostProcessor.postProcessAfterInitialization()`（AOP 代理在此创建）
7. **Bean 就绪**
8. **销毁**：@PreDestroy → DisposableBean.destroy() → destroy-method

### 7.2 Spring 三级缓存如何解决循环依赖
**回答结构**（场景 + 三级结构 + 流程 + 局限）：
1. **场景**：setter 注入 + singleton + A 依赖 B，B 依赖 A
2. **三级缓存**：singletonObjects（成品）、earlySingletonObjects（半成品代理）、singletonFactories（工厂）
3. **流程**：A 实例化 → `addSingletonFactory` → populateBean 发现 B → 创建 B → B 实例化 → populateBean 发现 A → 从三级缓存拿到工厂 → 调用 getObject 获取早期引用 → A 注入 B → B 完成 → A 继续完成
4. **为什么三级**：区分普通 Bean 和 AOP 代理场景。非 AOP 二级就够了，AOP 需要 ObjectFactory 在循环依赖发生时才提前生成代理
5. **不能解决**：构造器注入（实例化阶段死锁）、prototype（无缓存）

### 7.3 AOP 代理创建和执行过程
1. **创建时机**：`BeanPostProcessor.postProcessAfterInitialization()` → `AbstractAutoProxyCreator.wrapIfNecessary()`
2. **匹配 Advisor**：`findEligibleAdvisors()` → 遍历所有 Advisor → Pointcut 匹配
3. **创建代理**：`createProxy()` → JDK（有接口）或 CGLIB（无接口或强制）
4. **调用链**：`ReflectiveMethodInvocation.proceed()` → 责任链模式 → 依次执行各 Advisor → 最后调用目标方法
5. **注意**：多个切面通过 @Order 排序，Around 最外层

### 7.4 Spring Boot 启动流程（完整）
1. **构造**：推断 Web 类型 → 加载 Initializer → 加载 Listener → 推断主类
2. **run**：启动监听器 → 准备 Environment → 创建上下文 → 准备上下文（配置、初始化器）→ refresh（核心）
3. **refresh**：preRefresh → obtainFreshBeanFactory → invokeBeanFactoryPostProcessors（处理 @Configuration）→ registerBeanPostProcessors → onRefresh（创建内嵌 Tomcat）→ finishBeanFactoryInitialization（实例化 Bean）→ finishRefresh（发布事件）
4. **启动后**：发布 ApplicationReadyEvent → 执行 Runner

### 7.5 SpringMVC 完整执行流程
1. **请求进入**：Tomcat → DispatcherServlet.doDispatch()
2. **查找 Handler**：HandlerMapping 返回 HandlerExecutionChain（Handler + 拦截器链）
3. **查找 Adapter**：合适的 HandlerAdapter
4. **拦截器前置**：preHandle
5. **执行**：adapter.handle() → 参数解析器解析参数 → 反射调用 Controller 方法 → 返回值处理器处理
6. **拦截器后置**：postHandle
7. **结果处理**：视图解析（ViewResolver）→ 视图渲染（View.render()）
8. **异常处理**：HandlerExceptionResolver 处理异常

---

## 八、快速查漏补缺 Checklist

- [ ] BeanFactory 层次结构（接口继承链）
- [ ] DefaultListableBeanFactory 内部两个核心 Map
- [ ] BeanDefinition 关键属性
- [ ] AbstractApplicationContext.refresh() 12 步骤
- [ ] 三级缓存解决循环依赖（为什么需要三级）
- [ ] @Autowired 注入底层（AutowiredAnnotationBeanPostProcessor）
- [ ] BeanPostProcessor vs BeanFactoryPostProcessor（执行时机）
- [ ] FactoryBean 原理（getObject）
- [ ] AOP Advisor 结构（Pointcut + Advice）
- [ ] AbstractAutoProxyCreator.wrapIfNecessary()
- [ ] JDK 动态代理 + CGLIB 选择逻辑
- [ ] CGLIB FastClass 机制
- [ ] ReflectiveMethodInvocation 责任链
- [ ] AdvisorAdapter 适配器
- [ ] @Transactional TransactionInterceptor 源码
- [ ] DispatcherServlet 9 大组件初始化
- [ ] HandlerMapping → HandlerAdapter 执行流程
- [ ] 参数解析器 + 返回值处理器
- [ ] MessageConverter 消息转换
- [ ] @ControllerAdvice 三个扩展点
- [ ] 异常处理优先级（HandlerExceptionResolver 链）
- [ ] 类型转换两套接口（Converter/GenericConverter/ConversionService）
- [ ] Spring Boot 构造阶段（webType/initializer/listener/mainClass）
- [ ] Spring Boot run 阶段（13 个子步骤）
- [ ] AutoConfigurationImportSelector.selectImports()
- [ ] @Conditional 条件装配 ConditionEvaluator
- [ ] @Value 底层 + 占位符解析
- [ ] 事件发布器 SimpleApplicationEventMulticaster
- [ ] Spring Boot 自动配置过滤（spring-autoconfigure-metadata.properties）
- [ ] GenericApplicationContext 和 AnnotationConfigApplicationContext 关系
