# SSM 框架面试宝典
> 基于课程大纲全面覆盖 SSM（Spring + SpringMVC + MyBatis）面试高频考点

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

### 1.1 SSM 框架中三个组件各自的作用是什么？
| 组件 | 作用 | 职责 |
|------|------|------|
| Spring | IoC 容器 + AOP + 事务管理 | 管理对象生命周期和依赖 |
| SpringMVC | Web MVC 框架 | 请求接收、分发、响应 |
| MyBatis | 持久层框架 | 数据库操作、ORM 映射 |

> 💡 SSM 整合本质上就是：Spring 管理 Service + 事务，SpringMVC 管理 Controller，MyBatis 管理 DAO。三者通过 Spring IoC 容器串联。

### 1.2 SpringMVC 的核心组件有哪些？
| 组件 | 说明 |
|------|------|
| `DispatcherServlet` | 前端控制器，请求入口 |
| `HandlerMapping` | 请求到 Handler 的映射 |
| `HandlerAdapter` | Handler 的执行适配器 |
| `HandlerExceptionResolver` | 异常解析器 |
| `ViewResolver` | 视图解析器（逻辑视图 → 物理视图）|
| `MultipartResolver` | 文件上传解析器 |
| `LocaleResolver` | 国际化解析器 |
| `ThemeResolver` | 主题解析器 |
| `RequestToViewNameTranslator` | 请求到默认视图名翻译器 |

### 1.3 DispatcherServlet 的工作流程是什么？
```
1. 用户发送请求 → DispatcherServlet
2. DispatcherServlet 调用 HandlerMapping 获取 HandlerExecutionChain
3. DispatcherServlet 调用 HandlerAdapter 执行 Handler
4. HandlerAdapter 执行 Controller 方法
5. Controller 返回 ModelAndView
6. DispatcherServlet 调用 ViewResolver 解析视图
7. 视图渲染返回响应
```

### 1.4 SpringMVC 中 RestController 和 Controller 的区别？
```java
@Controller   // 返回视图名（需要 ViewResolver 解析）
@ResponseBody // 将返回值直接写入响应体（配合 @Controller 使用）

@RestController // = @Controller + @ResponseBody（组合注解）
// 所有方法默认返回 JSON/XML 而不是视图
```

### 1.5 HandlerInterceptor 的常用方法有哪些？
```java
public interface HandlerInterceptor {
    // Controller 方法执行前调用
    default boolean preHandle(HttpServletRequest request, HttpServletResponse response, 
                              Object handler) throws Exception {
        return true; // 返回 true 继续执行，false 中断请求
    }
    
    // Controller 方法执行后、视图渲染前调用
    default void postHandle(HttpServletRequest request, HttpServletResponse response,
                            Object handler, @Nullable ModelAndView modelAndView) throws Exception {
    }
    
    // 整个请求完成后（视图渲染后）调用
    default void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                 Object handler, @Nullable Exception ex) throws Exception {
    }
}
```

### 1.6 过滤器（Filter）和拦截器（Interceptor）的执行顺序
```
请求到达
  ↓
Filter.doFilter() — 前置处理
  ↓
DispatcherServlet
  ↓
Interceptor.preHandle() — 前置处理
  ↓
Controller 方法执行
  ↓
Interceptor.postHandle() — 后置处理
  ↓
视图渲染
  ↓
Interceptor.afterCompletion() — 完成处理
  ↓
Filter.doFilter() — 后置处理
  ↓
响应返回
```

### 1.7 RESTful 风格设计的基本原则是什么？
| 原则 | 说明 | 示例 |
|------|------|------|
| 资源导向 | URL 表示资源，而不是操作 | `/users`（用户资源）|
| HTTP 方法表达动作 | GET 查询、POST 创建、PUT 更新、DELETE 删除 | `GET /users/1` |
| 无状态 | 每个请求独立，服务端不保存客户端状态 | Token 传递认证信息 |
| 统一接口 | 资源操作通过统一的 HTTP 方法 | `POST /users` 创建 |
| 状态码表达结果 | 200/201/204/400/404/500 | `201 Created` |

```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @GetMapping("/{id}")        // 查询
    public Result<User> get(@PathVariable Long id) { }
    
    @PostMapping                // 创建
    public Result<User> create(@RequestBody User user) { }
    
    @PutMapping("/{id}")        // 更新
    public Result<User> update(@PathVariable Long id, @RequestBody User user) { }
    
    @DeleteMapping("/{id}")     // 删除
    public Result<Void> delete(@PathVariable Long id) { }
    
    @GetMapping                  // 列表
    public Result<List<User>> list(@RequestParam int page, @RequestParam int size) { }
}
```

### 1.8 如何接收 JSON 请求参数？
```java
// 1. @RequestBody 接收 JSON 对象
@PostMapping("/save")
public Result save(@RequestBody User user) { }

// 2. @RequestParam 接收 URL 参数
@GetMapping("/detail")
public Result detail(@RequestParam("id") Long userId) { }

// 3. @PathVariable 接收路径变量
@GetMapping("/{id}")
public Result get(@PathVariable Long id) { }

// 4. @RequestHeader 接收请求头
@GetMapping("/header")
public Result header(@RequestHeader("Authorization") String token) { }
```

### 1.9 如何处理日期参数？
```java
// 方式一：@DateTimeFormat
@GetMapping("/search")
public Result search(
    @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate start,
    @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime end) { }

// 方式二：全局配置
@Configuration
public class DateConfig implements WebMvcConfigurer {
    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addFormatter(new DateFormatter("yyyy-MM-dd"));
    }
}

// 方式三：Jackson 配置
@Bean
public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer() {
    return builder -> builder
        .serializerByType(LocalDate.class, new LocalDateSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
        .deserializerByType(LocalDate.class, new LocalDateDeserializer(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
}
```

### 1.10 Spring 整合 MyBatis 的核心配置是什么？
```xml
<!-- 1. 数据源配置 -->
<bean id="dataSource" class="com.zaxxer.hikari.HikariDataSource">
    <property name="driverClassName" value="com.mysql.cj.jdbc.Driver"/>
    <property name="jdbcUrl" value="jdbc:mysql://localhost:3306/db"/>
    <property name="username" value="root"/>
    <property name="password" value="123456"/>
</bean>

<!-- 2. SqlSessionFactory -->
<bean id="sqlSessionFactory" class="org.mybatis.spring.SqlSessionFactoryBean">
    <property name="dataSource" ref="dataSource"/>
    <property name="mapperLocations" value="classpath:mapper/**/*.xml"/>
    <property name="typeAliasesPackage" value="com.example.entity"/>
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
<tx:annotation-driven transaction-manager="transactionManager"/>
```

### 1.11 MyBatis 的 SqlSessionFactoryBean 作用是什么？
`SqlSessionFactoryBean` 是 Spring 整合 MyBatis 的核心类，实现了 `FactoryBean<SqlSessionFactory>` 接口。它在 `getObject()` 中读取 MyBatis 核心配置、Mapper XML 文件路径、类型别名包扫描等，创建并返回 `SqlSessionFactory` 实例。

```java
public class SqlSessionFactoryBean implements FactoryBean<SqlSessionFactory>, InitializingBean {
    private DataSource dataSource;
    private Resource[] mapperLocations;
    private String typeAliasesPackage;
    
    @Override
    public SqlSessionFactory getObject() throws Exception {
        // 构建 SqlSessionFactory
        SqlSessionFactoryBean 内部使用 XMLConfigBuilder 构建 Configuration
    }
}
```

### 1.12 MapperScannerConfigurer 的作用是什么？
`MapperScannerConfigurer` 实现了 `BeanDefinitionRegistryPostProcessor`，在 Spring 容器初始化阶段扫描指定包下的 Mapper 接口，将每个接口注册为 BeanDefinition（实际注册的是 `MapperFactoryBean`）。当获取 Mapper Bean 时，`MapperFactoryBean` 通过 `SqlSession.getMapper()` 返回动态代理对象。

### 1.13 Spring 中如何配置视图解析器？
```xml
<!-- JSP 视图解析器 -->
<bean class="org.springframework.web.servlet.view.InternalResourceViewResolver">
    <property name="prefix" value="/WEB-INF/views/"/>
    <property name="suffix" value=".jsp"/>
</bean>

<!-- JSON 视图（不返回视图，直接返回 JSON）-->
@EnableWebMvc
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(new MappingJackson2HttpMessageConverter());
    }
}
```

### 1.14 @RequestMapping 有哪些属性？
| 属性 | 作用 | 示例 |
|------|------|------|
| `value` | 请求 URL | `/users/{id}` |
| `method` | HTTP 方法 | `RequestMethod.GET` |
| `params` | 请求参数条件 | `params = "type=1"` |
| `headers` | 请求头条件 | `headers = "token"` |
| `consumes` | 请求的 Content-Type | `consumes = "application/json"` |
| `produces` | 返回的 Content-Type | `produces = "application/json;charset=utf-8"` |

### 1.15 SpringMVC 中如何实现文件上传？
```java
// 1. 配置文件上传解析器
@Bean
public MultipartResolver multipartResolver() {
    CommonsMultipartResolver resolver = new CommonsMultipartResolver();
    resolver.setMaxUploadSize(10 * 1024 * 1024); // 10MB
    resolver.setDefaultEncoding("UTF-8");
    return resolver;
}

// 2. Controller 接收文件
@PostMapping("/upload")
public Result<String> upload(@RequestParam("file") MultipartFile file) {
    if (file.isEmpty()) return Result.error("文件为空");
    
    String originalFilename = file.getOriginalFilename();
    String fileName = UUID.randomUUID() + "_" + originalFilename;
    
    // 保存文件
    file.transferTo(new File("/data/upload/" + fileName));
    
    return Result.success(fileName);
}

// 3. 多文件上传
@PostMapping("/uploads")
public Result<List<String>> uploads(@RequestParam("files") List<MultipartFile> files) {
    List<String> urls = files.stream().map(this::saveFile).collect(Collectors.toList());
    return Result.success(urls);
}
```

### 1.16 SpringMVC 中如何实现数据校验？
```java
// 1. 添加校验注解到实体
public class UserCreateRequest {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 20, message = "用户名长度2-20")
    private String username;
    
    @NotNull
    @Email(message = "邮箱格式不正确")
    private String email;
    
    @Min(0) @Max(150)
    private Integer age;
}

// 2. Controller 使用 @Valid 或 @Validated
@PostMapping
public Result<User> create(@Valid @RequestBody UserCreateRequest request) {
    // 校验失败会自动抛出 MethodArgumentNotValidException
    return Result.success(userService.create(request));
}

// 3. 自定义校验注解
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PhoneValidator.class)
public @interface Phone {
    String message() default "手机号格式不正确";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

public class PhoneValidator implements ConstraintValidator<Phone, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value != null && value.matches("^1[3-9]\\d{9}$");
    }
}
```

### 1.17 SSM 整合时如何处理静态资源访问？
```java
// 方式一：配置类
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(3600)
                .resourceChain(true)
                .addResolver(new PathResourceResolver());
    }
}

// 方式二：XML 配置
<mvc:resources mapping="/static/**" location="/static/" cache-period="3600"/>
```

### 1.18 Maven 的分模块开发如何组织 SSM 项目？
```
parent-project
├── pom.xml (聚合工程，管理依赖版本)
├── common-module          (公共模块：工具类、通用实体)
│   └── pom.xml
├── pojo-module            (POJO/Entity 实体)
│   └── pom.xml
├── dao-module             (MyBatis Mapper + XML)
│   └── pom.xml
├── service-module         (业务逻辑)
│   └── pom.xml
└── web-module             (Controller + 前端资源，war 包)
    └── pom.xml
```

---

## 二、深度原理剖析（10-15题）

### 2.1 DispatcherServlet 的 doDispatch 源码分析
```java
protected void doDispatch(HttpServletRequest request, HttpServletResponse response) {
    HttpServletRequest processedRequest = request;
    HandlerExecutionChain mappedHandler = null;
    ModelAndView mv;
    
    try {
        ModelAndViewDefiningException mavEx = null;
        
        // 1. 文件上传请求预处理
        processedRequest = checkMultipart(request);
        
        // 2. 获取 HandlerExecutionChain（Handler + 拦截器）
        mappedHandler = getHandler(processedRequest);
        if (mappedHandler == null) {
            noHandlerFound(processedRequest, response);
            return;
        }
        
        // 3. 获取 HandlerAdapter
        HandlerAdapter ha = getHandlerAdapter(mappedHandler.getHandler());
        
        // 4. 执行拦截器 preHandle
        if (!mappedHandler.applyPreHandle(processedRequest, response)) {
            return; // 拦截器返回 false 直接返回
        }
        
        // 5. 真正执行 Controller 方法
        mv = ha.handle(processedRequest, response, mappedHandler.getHandler());
        
        // 6. 设置默认视图名
        if (asyncManager.isConcurrentHandlingStarted()) return;
        applyDefaultViewName(processedRequest, mv);
        
        // 7. 执行拦截器 postHandle
        mappedHandler.applyPostHandle(processedRequest, response, mv);
        
    } catch (Exception ex) {
        dispatchException = ex;
    } catch (Throwable err) {
        dispatchException = new NestedServletException("Handler dispatch failed", err);
    }
    
    // 8. 处理返回结果（视图渲染/异常处理）
    processDispatchResult(processedRequest, response, mappedHandler, mv, dispatchException);
}
```

### 2.2 RequestMappingHandlerMapping 如何建立 URL 映射？
```java
// RequestMappingHandlerMapping.afterPropertiesSet() 阶段
// 调用父类 AbstractHandlerMethodMapping.initHandlerMethods()
// → detectHandlerMethods() 遍历容器中的所有 Bean
// → mappingForMethod() 解析 @RequestMapping 注解
// → registerHandlerMethod() 将 URL 映射注册到 MappingRegistry

// MappingRegistry 内部结构
class MappingRegistry {
    private final Map<T, HandlerMethod> mappingLookup = new LinkedHashMap<>();  // URL → HandlerMethod
    private final MultiValueMap<String, T> urlLookup = new LinkedMultiValueMap<>(); // URL → Mapping
    private final Map<String, List<HandlerMethod>> directUrlMap = new LinkedHashMap<>(); // 直接 URL 映射
    private final Map<String, HandlerMethod> nameLookup = new ConcurrentHashMap<>();    // 方法名
}

// URL → HandlerMethod 查找
// AbstractHandlerMethodMapping.getHandlerInternal()
// → lookupHandlerMethod(lookupPath, request)
// → 精确匹配 → 通配符匹配 → 排序选择最优匹配
```

### 2.3 RequestMappingHandlerAdapter 的参数解析器注册过程
```java
// RequestMappingHandlerAdapter.afterPropertiesSet()
// → getDefaultArgumentResolvers() 注册默认 26 个参数解析器

private List<HandlerMethodArgumentResolver> getDefaultArgumentResolvers() {
    List<HandlerMethodArgumentResolver> resolvers = new ArrayList<>(30);
    
    // 基于注解的参数解析器
    resolvers.add(new RequestParamMethodArgumentResolver(getBeanFactory(), false));  // @RequestParam
    resolvers.add(new RequestParamMapMethodArgumentResolver());                       // @RequestParam Map
    resolvers.add(new PathVariableMethodArgumentResolver());                          // @PathVariable
    resolvers.add(new PathVariableMapMethodArgumentResolver());                       // @PathVariable Map
    resolvers.add(new MatrixVariableMethodArgumentResolver());                        // @MatrixVariable
    resolvers.add(new MatrixVariableMapMethodArgumentResolver());                     // @MatrixVariable Map
    resolvers.add(new ServletModelAttributeMethodProcessor(true));                   // @ModelAttribute
    
    // 基于类型的参数解析器
    resolvers.add(new ServletRequestMethodArgumentResolver());                        // HttpServletRequest
    resolvers.add(new ServletResponseMethodArgumentResolver());                       // HttpServletResponse
    resolvers.add(new HttpEntityMethodProcessor(getMessageConverters()));             // HttpEntity
    resolvers.add(new RequestBodyMethodProcessor(getMessageConverters()));            // @RequestBody
    resolvers.add(new ModelMethodProcessor());                                        // Model
    
    // 其他
    resolvers.add(new SessionStatusMethodArgumentResolver());                         // SessionStatus
    resolvers.add(new UriComponentsBuilderMethodArgumentResolver());                  // UriComponentsBuilder
    
    // 自定义解析器（按需添加）
    if (getCustomArgumentResolvers() != null) {
        resolvers.addAll(getCustomArgumentResolvers());
    }
    
    return resolvers;
}
```

### 2.4 SpringMVC 视图解析过程
```java
// DispatcherServlet.processDispatchResult()
protected void processDispatchResult(HttpServletRequest request, HttpServletResponse response,
                                     HandlerExecutionChain mappedHandler, ModelAndView mv, Exception e) {
    boolean errorView = false;
    
    if (e != null) {
        // 异常处理
        if (e instanceof ModelAndViewDefiningException) {
            mv = ((ModelAndViewDefiningException) e).getModelAndView();
        } else {
            Object handler = mappedHandler != null ? mappedHandler.getHandler() : null;
            mv = processHandlerException(request, response, handler, e);
            errorView = mv != null;
        }
    }
    
    // 视图渲染
    if (mv != null && !mv.wasCleared()) {
        render(mv, request, response);
    }
}

// render() 方法
protected void render(ModelAndView mv, HttpServletRequest request, HttpServletResponse response) {
    // 获取 View 对象
    View view;
    String viewName = mv.getViewName();
    if (viewName != null) {
        // 通过 ViewResolver 解析逻辑视图名 → 物理视图
        view = resolveViewName(viewName, mv.getModelInternal(), locale, request);
    } else {
        view = mv.getView();
    }
    
    // 渲染视图
    view.render(mv.getModelInternal(), request, response);
}

// 多个 ViewResolver 链式处理
protected View resolveViewName(String viewName, Map<String, Object> model, 
                               Locale locale, HttpServletRequest request) {
    for (ViewResolver viewResolver : this.viewResolvers) {
        View view = viewResolver.resolveViewName(viewName, locale);
        if (view != null) return view;
    }
    return null;
}
```

### 2.5 拦截器链的执行顺序解析
```java
// HandlerExecutionChain 维护拦截器链

// 前置执行（顺序：注册顺序）
boolean applyPreHandle(HttpServletRequest request, HttpServletResponse response) {
    for (int i = 0; i < this.interceptorList.size(); i++) {
        HandlerInterceptor interceptor = this.interceptorList.get(i);
        if (!interceptor.preHandle(request, response, this.handler)) {
            // 如果前置返回 false，触发已执行拦截器的 afterCompletion
            triggerAfterCompletion(request, response, null);
            return false;
        }
        this.interceptorIndex = i; // 记录已执行的索引
    }
    return true;
}

// 后置执行（逆序）
void applyPostHandle(HttpServletRequest request, HttpServletResponse response, ModelAndView mv) {
    for (int i = this.interceptorList.size() - 1; i >= 0; i--) {
        this.interceptorList.get(i).postHandle(request, response, this.handler, mv);
    }
}

// 完成执行（逆序，异常时也触发）
void triggerAfterCompletion(HttpServletRequest request, HttpServletResponse response, Exception ex) {
    for (int i = this.interceptorIndex; i >= 0; i--) {
        this.interceptorList.get(i).afterCompletion(request, response, this.handler, ex);
    }
}
```

### 2.6 SpringMVC 异常处理机制源码
```java
// DispatcherServlet.processHandlerException()
protected ModelAndView processHandlerException(HttpServletRequest request, 
                                               HttpServletResponse response,
                                               Object handler, Exception ex) {
    ModelAndView exMv = null;
    
    // 遍历 HandlerExceptionResolver 链
    if (this.handlerExceptionResolvers != null) {
        for (HandlerExceptionResolver resolver : this.handlerExceptionResolvers) {
            exMv = resolver.resolveException(request, response, handler, ex);
            if (exMv != null) break;
        }
    }
    
    if (exMv != null) {
        if (exMv.isEmpty()) {
            request.setAttribute(EXCEPTION_ATTRIBUTE, ex);
            return null;
        }
        // 设置异常视图
        if (!exMv.hasView()) {
            exMv.setViewName(getDefaultViewName(request));
        }
        return exMv;
    }
    
    throw ex; // 没处理则抛给 Servlet 容器
}

// ExceptionHandlerExceptionResolver 处理 @ExceptionHandler
// → 查找 @ControllerAdvice 中的 @ExceptionHandler
// → 匹配异常类型（支持继承关系匹配）
// → MethodParameter 解析参数
// → 执行异常处理方法
```

### 2.7 SSM 中 MapperScannerConfigurer 与 @MapperScan 的关系
```java
// @MapperScan 是 MyBatis-Spring 提供的注解式 Mapper 扫描
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import(MapperScannerRegistrar.class) // 通过 ImportBeanDefinitionRegistrar 实现
public @interface MapperScan {
    String[] value() default {};
    String[] basePackages() default {};
    Class<? extends Annotation> annotationClass() default Annotation.class;
}

// MapperScannerRegistrar 注册 MapperScannerConfigurer 类型的 BeanDefinition
// MapperScannerConfigurer.postProcessBeanDefinitionRegistry() 时
// → ClassPathMapperScanner.scan()
// → 扫描 @Mapper 注解标记的接口
// → 注册为 BeanDefinition（BeanClass 为 MapperFactoryBean）
// → 设置 SqlSessionFactory 到 BeanDefinition

// 两种方式等价：
// XML：<bean class="org.mybatis.spring.mapper.MapperScannerConfigurer" ...>
// 注解：@MapperScan("com.example.mapper")
```

### 2.8 SSM 整合中 SqlSessionTemplate 的作用
`SqlSessionTemplate` 是 Spring 整合 MyBatis 的核心，是 `SqlSession` 的线程安全代理。内部使用 `SqlSessionInterceptor`（JDK 动态代理）对 `SqlSession` 的所有方法进行拦截：

```java
public class SqlSessionTemplate implements SqlSession {
    private final SqlSessionFactory sqlSessionFactory;
    private final SqlSessionInterceptor interceptor;
    private final SqlSession sqlSessionProxy; // 代理对象
    
    public SqlSessionTemplate(SqlSessionFactory sqlSessionFactory, ExecutorType executorType,
                               PersistenceExceptionTranslator exceptionTranslator) {
        this.sqlSessionFactory = sqlSessionFactory;
        this.interceptor = new SqlSessionInterceptor();
        // 创建代理
        this.sqlSessionProxy = (SqlSession) Proxy.newProxyInstance(
                SqlSessionFactory.class.getClassLoader(),
                new Class[]{SqlSession.class},
                this.interceptor);
    }
    
    // 代理拦截逻辑
    private class SqlSessionInterceptor implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            // 1. 从当前 Spring 事务中获取 SqlSession
            SqlSession sqlSession = getSqlSession(SqlSessionTemplate.this.sqlSessionFactory,
                    SqlSessionTemplate.this.executorType,
                    SqlSessionTemplate.this.exceptionTranslator);
            
            try {
                // 2. 执行实际方法
                Object result = method.invoke(sqlSession, args);
                
                // 3. 如果不是事务管理，直接提交
                if (!isSqlSessionTransactional(sqlSession, sqlSessionFactory)) {
                    sqlSession.commit(true);
                }
                return result;
            } catch (Throwable t) {
                // 4. 异常转换（PersistenceExceptionTranslator）
                throw translateException(t);
            } finally {
                // 5. 如果是独立 SqlSession，关闭；事务中的则返回
                if (sqlSession != null) {
                    closeSqlSession(sqlSession, sqlSessionFactory);
                }
            }
        }
    }
}
```

### 2.9 SpringMVC 中异步请求处理
SpringMVC 3.2+ 支持异步处理（Servlet 3.0 AsyncContext），提高吞吐量：

```java
// 方式一：DeferredResult
@GetMapping("/async")
public DeferredResult<String> asyncRequest() {
    DeferredResult<String> result = new DeferredResult<>(5000L); // 超时 5 秒
    // 在另一个线程中设置结果
    asyncTaskExecutor.execute(() -> {
        String data = remoteService.call();
        result.setResult(data);
    });
    return result;
}

// 方式二：Callable
@GetMapping("/callable")
public Callable<String> callableRequest() {
    return () -> {
        Thread.sleep(2000);
        return "异步结果";
    };
}

// 方式三：WebAsyncTask
@GetMapping("/webAsyncTask")
public WebAsyncTask<String> webAsyncTask() {
    WebAsyncTask<String> task = new WebAsyncTask<>(3000, () -> {
        return "处理完成";
    });
    task.onTimeout(() -> "超时返回");
    return task;
}
```

### 2.10 SpringMVC 中的 ContentNegotiation 内容协商
```java
// 内容协商：根据请求决定返回的数据格式（JSON/XML/HTML）
// 策略：
// 1. URL 后缀：/users.json → JSON, /users.xml → XML
// 2. 请求参数：/users?format=json
// 3. Accept 请求头：Accept: application/json

// 配置内容协商
@Configuration
public class ContentNegotiationConfig implements WebMvcConfigurer {
    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer
            .favorParameter(true)
            .parameterName("format")
            .ignoreAcceptHeader(false)
            .defaultContentType(MediaType.APPLICATION_JSON)
            .mediaType("json", MediaType.APPLICATION_JSON)
            .mediaType("xml", MediaType.APPLICATION_XML);
    }
}
```

---

## 三、实战场景题（8-12题）

### 3.1 SSM 项目中如何配置统一返回格式？
```java
// 1. 统一返回实体
@Data
public class Result<T> {
    private int code;
    private String message;
    private T data;
    private long timestamp;
    
    public static <T> Result<T> success(T data) {
        return new Result<T>(200, "success", data, System.currentTimeMillis());
    }
    
    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message, null, System.currentTimeMillis());
    }
}

// 2. 全局响应处理（ResponseBodyAdvice）
@ControllerAdvice
public class GlobalResponseHandler implements ResponseBodyAdvice<Object> {
    @Override
    public boolean supports(MethodParameter returnType, Class converterType) {
        // 跳过 String 类型（StringHttpMessageConverter 处理）
        return !returnType.getParameterType().equals(Result.class)
               && !returnType.getParameterType().equals(String.class);
    }
    
    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType,
                                  MediaType selectedContentType, Class selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        return Result.success(body);
    }
}
```

### 3.2 SSM 项目中如何实现自定义分页查询？
```java
// 1. 通用分页请求
@Data
public class PageRequest {
    private int page = 1;
    private int pageSize = 10;
    
    public int getOffset() {
        return (page - 1) * pageSize;
    }
}

// 2. 通用分页响应
@Data
public class PageResult<T> {
    private int page;
    private int pageSize;
    private long total;
    private List<T> records;
}

// 3. MyBatis 分页 SQL
<select id="findPage" resultType="User">
    SELECT * FROM user
    <where>
        <if test="name != null">
            AND name LIKE CONCAT('%', #{name}, '%')
        </if>
    </where>
    ORDER BY id DESC
    LIMIT #{offset}, #{pageSize}
</select>

<select id="count" resultType="long">
    SELECT COUNT(*) FROM user
    <where>
        <if test="name != null">
            AND name LIKE CONCAT('%', #{name}, '%')
        </if>
    </where>
</select>

// 4. Controller
@GetMapping
public Result<PageResult<User>> list(PageRequest pageRequest, @RequestParam(required = false) String name) {
    List<User> list = userMapper.findPage(pageRequest.getOffset(), pageRequest.getPageSize(), name);
    long total = userMapper.count(name);
    PageResult<User> result = new PageResult<>();
    result.setPage(pageRequest.getPage());
    result.setPageSize(pageRequest.getPageSize());
    result.setTotal(total);
    result.setRecords(list);
    return Result.success(result);
}
```

### 3.3 SSM 整合时如何实现多环境配置？
```xml
<!-- 方式一：Spring Profile -->
<!-- web.xml -->
<context-param>
    <param-name>spring.profiles.active</param-name>
    <param-value>dev</param-value>
</context-param>

<!-- spring-context.xml -->
<beans profile="dev">
    <bean id="dataSource" class="com.zaxxer.hikari.HikariDataSource">
        <property name="jdbcUrl" value="jdbc:mysql://localhost:3306/dev_db"/>
    </bean>
</beans>

<beans profile="prod">
    <bean id="dataSource" class="com.zaxxer.hikari.HikariDataSource">
        <property name="jdbcUrl" value="jdbc:mysql://prod-host:3306/prod_db"/>
        <property name="username" value="${jdbc.username}"/>
        <property name="password" value="${jdbc.password}"/>
    </bean>
</beans>

<!-- 方式二：Maven Profile + 资源过滤 -->
<!-- pom.xml -->
<profiles>
    <profile>
        <id>dev</id>
        <activation><activeByDefault>true</activeByDefault></activation>
        <properties>
            <db.url>jdbc:mysql://localhost:3306/dev</db.url>
        </properties>
    </profile>
    <profile>
        <id>prod</id>
        <properties>
            <db.url>jdbc:mysql://prod:3306/prod</db.url>
        </properties>
    </profile>
</profiles>
```

### 3.4 SSM 项目中如何实现权限管理？
```java
// 方式一：拦截器 + 注解
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresPermission {
    String value(); // 权限标识
}

@Component
public class PermissionInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        if (handler instanceof HandlerMethod) {
            HandlerMethod hm = (HandlerMethod) handler;
            RequiresPermission annotation = hm.getMethodAnnotation(RequiresPermission.class);
            if (annotation != null) {
                String permission = annotation.value();
                User currentUser = (User) request.getSession().getAttribute("user");
                if (currentUser == null || !currentUser.hasPermission(permission)) {
                    response.setStatus(403);
                    return false;
                }
            }
        }
        return true;
    }
}

// 方式二：Spring Security + JWT
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf().disable()
            .authorizeRequests()
                .antMatchers("/api/public/**").permitAll()
                .antMatchers("/api/admin/**").hasRole("ADMIN")
                .antMatchers("/api/**").authenticated()
            .and()
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
```

### 3.5 SpringMVC Controller 中如何获取 Request 和 Response？
```java
// 方式一：方法参数中注入
@GetMapping("/test")
public Result test(HttpServletRequest request, HttpServletResponse response) {
    String token = request.getHeader("Authorization");
    response.setHeader("X-Custom-Header", "value");
    return Result.success("ok");
}

// 方式二：从 RequestContextHolder 获取（任意地方）
public static HttpServletRequest getRequest() {
    ServletRequestAttributes attrs = (ServletRequestAttributes) 
            RequestContextHolder.getRequestAttributes();
    return attrs != null ? attrs.getRequest() : null;
}

// 方式三：自动注入（单例问题）
@Component
@Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class RequestContext {
    public void doSomething() {
        HttpServletRequest request = ((ServletRequestAttributes) 
                RequestContextHolder.currentRequestAttributes()).getRequest();
    }
}
```

### 3.6 如何实现请求日志记录？
```java
// 方式一：拦截器
@Component
public class RequestLogInterceptor implements HandlerInterceptor {
    private static final Logger log = LoggerFactory.getLogger(RequestLogInterceptor.class);
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) {
        // 记录开始时间
        request.setAttribute("startTime", System.currentTimeMillis());
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        long startTime = (Long) request.getAttribute("startTime");
        long duration = System.currentTimeMillis() - startTime;
        
        String method = request.getMethod();
        String uri = request.getRequestURI();
        int status = response.getStatus();
        
        log.info("[{}] {} -> {} 耗时:{}ms", method, uri, status, duration);
    }
}

// 方式二：Spring Boot Actuator + WebFilter
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 包装可重复读
        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        
        filterChain.doFilter(requestWrapper, responseWrapper);
        
        String requestBody = new String(requestWrapper.getContentAsByteArray(), StandardCharsets.UTF_8);
        String responseBody = new String(responseWrapper.getContentAsByteArray(), StandardCharsets.UTF_8);
        log.debug("Request: {} {}, Response: {}", request.getMethod(), request.getRequestURI(), responseBody);
        
        responseWrapper.copyBodyToResponse();
    }
}
```

### 3.7 SSM 项目中如何配置 Druid 连接池监控？
```java
// 1. 配置 Druid 数据源
@Configuration
public class DruidConfig {
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource druidDataSource() {
        return new DruidDataSource();
    }
    
    // 2. 配置 Druid 监控 Servlet
    @Bean
    public ServletRegistrationBean<StatViewServlet> statViewServlet() {
        ServletRegistrationBean<StatViewServlet> bean = new ServletRegistrationBean<>(
                new StatViewServlet(), "/druid/*");
        bean.addInitParameter("loginUsername", "admin");
        bean.addInitParameter("loginPassword", "admin123");
        bean.addInitParameter("resetEnable", "false");
        return bean;
    }
    
    // 3. 配置 Web 监控 Filter
    @Bean
    public FilterRegistrationBean<WebStatFilter> webStatFilter() {
        FilterRegistrationBean<WebStatFilter> bean = new FilterRegistrationBean<>(
                new WebStatFilter());
        bean.addUrlPatterns("/*");
        bean.addInitParameter("exclusions", "*.js,*.css,/druid/*");
        return bean;
    }
    
    // 4. 配置 Spring 监控切面
    @Bean
    public DruidStatInterceptor druidStatInterceptor() {
        return new DruidStatInterceptor();
    }
}
```

### 3.8 SSM 项目中 MyBatis 日志如何配置？
```xml
<!-- 方式一：MyBatis 内部日志 -->
<configuration>
    <settings>
        <setting name="logImpl" value="STDOUT_LOGGING"/>
    </settings>
</configuration>

<!-- 方式二：Log4j2 + MyBatis -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-log4j2</artifactId>
</dependency>
```

```properties
# log4j2.xml 配置 MyBatis SQL 日志（只显示 SQL）
logger.mybatis.name=com.example.mapper
logger.mybatis.level=DEBUG

# 显示 SQL 参数
logger.mybatis-sql.name=com.example.mapper.UserMapper
logger.mybatis-sql.level=TRACE
```

```yaml
# Spring Boot 配置
mybatis:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
logging:
  level:
    com.example.mapper: DEBUG
```

---

## 四、手写代码题（5-8题）

### 4.1 手写 SSM 整合的配置类（纯注解版）
```java
@Configuration // 替换 XML 配置
@ComponentScan(basePackages = {"com.example.service", "com.example.dao"})
@PropertySource("classpath:jdbc.properties")
@EnableTransactionManagement
public class SpringConfig {
    
    @Value("${jdbc.url}")
    private String url;
    @Value("${jdbc.username}")
    private String username;
    @Value("${jdbc.password}")
    private String password;
    
    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setMaximumPoolSize(20);
        return new HikariDataSource(config);
    }
    
    @Bean
    public SqlSessionFactoryBean sqlSessionFactory(DataSource dataSource) {
        SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
        bean.setDataSource(dataSource);
        bean.setTypeAliasesPackage("com.example.entity");
        bean.setMapperLocations(new PathMatchingResourcePatternResolver()
                .getResources("classpath:mapper/**/*.xml"));
        return bean;
    }
    
    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}

// SpringMVC 配置
@Configuration
@EnableWebMvc
@ComponentScan("com.example.controller")
public class WebConfig implements WebMvcConfigurer {
    
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(new MappingJackson2HttpMessageConverter());
    }
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LoginInterceptor())
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/login");
    }
}

// 启动类
public class ApplicationInitializer implements WebApplicationInitializer {
    @Override
    public void onStartup(ServletContext servletContext) {
        AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
        context.register(SpringConfig.class);
        
        DispatcherServlet servlet = new DispatcherServlet(context);
        ServletRegistration.Dynamic registration = servletContext.addServlet("dispatcher", servlet);
        registration.setLoadOnStartup(1);
        registration.addMapping("/");
    }
}
```

### 4.2 手写自定义 HandlerMethodArgumentResolver
```java
// 场景：自定义注解注入当前登录用户
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {}

public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {
    
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
                && parameter.getParameterType().equals(User.class);
    }
    
    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        HttpServletRequest request = (HttpServletRequest) webRequest.getNativeRequest();
        // 从 Token 中解析用户
        String token = request.getHeader("Authorization");
        if (token != null) {
            User user = parseToken(token);
            return user;
        }
        return null;
    }
    
    private User parseToken(String token) {
        // JWT 解析逻辑
        return null;
    }
}

// 注册自定义解析器
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new CurrentUserArgumentResolver());
    }
}

// 使用
@GetMapping("/profile")
public Result<User> profile(@CurrentUser User user) {
    return Result.success(user);
}
```

### 4.3 手写自定义 ViewResolver
```java
// 场景：支持 JSON 视图
public class JsonViewResolver implements ViewResolver {
    @Override
    public View resolveViewName(String viewName, Locale locale) throws Exception {
        if (viewName.startsWith("json:")) {
            return new View() {
                @Override
                public void render(Map<String, ?> model, HttpServletRequest request,
                                   HttpServletResponse response) throws Exception {
                    response.setContentType("application/json;charset=utf-8");
                    String json = new ObjectMapper().writeValueAsString(model);
                    response.getWriter().write(json);
                }
            };
        }
        return null;
    }
}

// 注册
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void configureViewResolvers(ViewResolverRegistry registry) {
        registry.viewResolver(new JsonViewResolver());
        registry.jsp("/WEB-INF/views/", ".jsp");
    }
}
```

### 4.4 手写一整套 RESTful 风格的 User CRUD
```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @PostMapping
    public Result<User> create(@Valid @RequestBody UserCreateRequest request) {
        User user = userService.create(request);
        return Result.success(user);
    }
    
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.success(null);
    }
    
    @PutMapping("/{id}")
    public Result<User> update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        User user = userService.update(id, request);
        return Result.success(user);
    }
    
    @GetMapping("/{id}")
    public Result<User> get(@PathVariable Long id) {
        User user = userService.findById(id);
        return Result.success(user);
    }
    
    @GetMapping
    public Result<PageResult<User>> list(PageRequest pageRequest, UserQueryRequest query) {
        PageResult<User> page = userService.findPage(pageRequest, query);
        return Result.success(page);
    }
}
```

### 4.5 手写 Spring Security + JWT 整合
```java
// JWT 工具类
@Component
public class JwtUtil {
    private static final String SECRET = "your-secret-key";
    private static final long EXPIRATION = 86400000L; // 24h
    
    public String generateToken(Long userId, String username) {
        return Jwts.builder()
                .setSubject(username)
                .claim("userId", userId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(SignatureAlgorithm.HS256, SECRET)
                .compact();
    }
    
    public Claims parseToken(String token) {
        return Jwts.parser().setSigningKey(SECRET).parseClaimsJws(token).getBody();
    }
}

// JWT 认证过滤器
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Claims claims = jwtUtil.parseToken(token);
                String username = claims.getSubject();
                // 设置认证信息
                UsernamePasswordAuthenticationToken auth = 
                    new UsernamePasswordAuthenticationToken(username, null, new ArrayList<>());
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception e) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}
```

---

## 五、系统设计题（3-5题）

### 5.1 设计一个 SSM 项目的统一鉴权网关
```java
// 基于拦截器链的鉴权网关
@Component
public class AuthGatewayInterceptor implements HandlerInterceptor {
    
    @Autowired
    private TokenService tokenService;
    
    @Autowired
    private PermissionService permissionService;
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) return true;
        
        String uri = request.getRequestURI();
        String method = request.getMethod();
        
        // 1. 白名单放行
        if (isWhitelist(uri)) return true;
        
        // 2. Token 校验
        String token = extractToken(request);
        if (token == null || !tokenService.validate(token)) {
            writeResponse(response, 401, "未授权");
            return false;
        }
        
        // 3. 权限校验
        User user = tokenService.getUser(token);
        if (!permissionService.hasPermission(user.getRole(), method, uri)) {
            writeResponse(response, 403, "无权限");
            return false;
        }
        
        // 4. 设置用户上下文
        UserContext.set(user);
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContext.clear(); // 防止内存泄漏
    }
}
```

### 5.2 设计一个通用 SSM 项目骨架的模块划分
```
my-app
├── my-app-common        # 公共工具类、常量、异常定义
│   ├── util/
│   ├── constant/
│   └── exception/
├── my-app-dao           # 数据访问层
│   ├── entity/          # 实体类
│   ├── mapper/          # Mapper 接口
│   └── mapper/*.xml     # SQL 映射
├── my-app-service       # 业务层
│   ├── service/         # 业务接口
│   └── impl/            # 实现类
├── my-app-web           # 表现层
│   ├── controller/      # Controller
│   ├── interceptor/     # 拦截器
│   ├── advice/          # 统一异常/返回处理
│   ├── dto/             # 请求/响应 DTO
│   └── config/          # Web 配置
└── my-app-start         # 启动模块（parent pom）
    ├── resources/
    │   ├── config/      # 外部配置
    │   ├── spring/      # Spring XML 或 javaconfig
    │   └── webapp/      # 前端资源
    └── ApplicationInitializer.java
```

### 5.3 设计一个高可用的 SSM 部署方案
```yaml
# 架构分层
负载均衡层: Nginx/HAProxy
  → 反向代理、SSL 终止、限流
Web 层: Tomcat 集群（多实例部署）
  → 水平扩展、Session 共享（Redis）
应用层: Spring + SpringMVC + MyBatis
  → 无状态设计、RESTful API
缓存层: Redis 集群（主从 + Sentinel）
  → 热点数据缓存、Session 存储
数据层: MySQL 主从（读写分离） + MyBatis
  → 主库写入、从库读取

# 部署配置
server:
  tomcat:
    max-threads: 200
    min-spare-threads: 10
    accept-count: 100
    connection-timeout: 5000
    max-connections: 8192

spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      idle-timeout: 300000
      max-lifetime: 1200000
  
  redis:
    timeout: 3000
    lettuce:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 0
```

---

## 六、常见坑点与最佳实践（表格）

| 坑点 | 问题描述 | 解决方案 | 最佳实践 |
|------|---------|---------|---------|
| 404 但 Controller 没问题 | DispatcherServlet 拦截路径不匹配 | 检查 web.xml 中的 servlet-mapping | 使用 `/` 或 `/*` 明确 |
| JSON 日期序列化格式不对 | Date/LocalDate 返回为时间戳 | 配置 Jackson 日期格式 | 统一使用 `yyyy-MM-dd HH:mm:ss` |
| 跨域请求被拦截 | 前后端分离项目浏览器 CORS 限制 | 配置 CORS 跨域 | 使用 Nginx 或 Spring 跨域配置 |
| XML 映射文件未找到 | Mapper XML 扫描路径错误 | 检查 mapperLocations 配置 | 使用 classpath:mapper/**/*.xml |
| Session 不共享 | 多实例部署时 Session 不一致 | 使用 Redis 集中存储 Session | 采用无状态 Token 认证 |
| MyBatis 二级缓存脏读 | 多表关联操作缓存不一致 | 引用 cache-ref 或关闭二级缓存 | 生产环境用 Redis 替代 |
| AOP 切面不生效 | 类内部方法调用不经过代理 | 注入自身代理或拆分类 | 跨类调用 AOP |
| @Transactional 方法自调用失效 | this 调用不走代理 | 使用 AopContext.currentProxy() | 事务方法单独放 Service |
| 异步请求 @Async 事务失效 | 异步线程事务上下文丢失 | 异步方法内使用编程式事务 | 异步和事务明确分离 |
| 统一异常处理和 Spring Security 冲突 | 认证异常被 Security 内部处理而不是全局异常处理器 | 配置 Security 的 ExceptionTranslationFilter | Security 异常用 Security 方式处理 |
| 文件上传过大 | 超过 Tomcat 默认限制 | 配置 max-upload-size 和 Tomcat connector | 前端限制 + 后端限制双层校验 |

---

## 七、面试回答模板（Top 5高频题的结构化回答模板）

### 7.1 DispatcherServlet 工作流程
**回答结构**（分步说明）：
1. **请求进入**：Tomcat 接收请求，转发给 DispatcherServlet
2. **查找 Handler**：DispatcherServlet 调用 HandlerMapping 获取 HandlerExecutionChain（Handler + 拦截器链）
3. **查找适配器**：DispatcherServlet 从 HandlerAdapters 中找到支持该 Handler 的 HandlerAdapter
4. **拦截器前置**：执行 HandlerExecutionChain 的 applyPreHandle()
5. **执行 Handler**：HandlerAdapter.handle() → 参数解析器解析参数 → 反射执行 Controller 方法 → 返回值处理器处理
6. **拦截器后置**：applyPostHandle()
7. **处理结果**：ViewResolver 解析视图名 → View.render() 渲染 → 返回响应
8. **异常处理**：HandlerExceptionResolver 处理过程中抛出的异常

### 7.2 拦截器和过滤器的区别
**对比法回答**：
1. **规范不同**：Filter 是 Servlet 规范，Interceptor 是 Spring 规范
2. **上下文不同**：Filter 无法访问 Spring 容器，Interceptor 可以获取 Spring Bean
3. **粒度不同**：Filter 拦截所有 URL（含静态资源），Interceptor 只拦截 Controller 请求
4. **执行顺序**：Filter → DispatcherServlet → Interceptor.preHandle → Controller → Interceptor.postHandle → Interceptor.afterCompletion → Filter
5. **典型应用**：Filter 用于字符编码、跨域；Interceptor 用于权限校验、请求日志

### 7.3 Spring 如何整合 MyBatis
**三步骤回答**：
1. **配置数据源**：HikariCP/Druid 管理数据库连接
2. **配置 SqlSessionFactory**：SqlSessionFactoryBean（实现 FactoryBean）读取 mapper XML 和配置
3. **扫描 Mapper**：@MapperScan 或 MapperScannerConfigurer 扫描 Mapper 接口，注册 MapperFactoryBean
4. **事务管理**：DataSourceTransactionManager 管理事务，@Transactional 声明式控制

### 7.4 RESTful API 设计原则
**资源导向回答**：
1. **URL 是名词不是动词**：`GET /users`（查询用户列表），不是 `GET /getUsers`
2. **HTTP 方法语义化**：GET 查询、POST 创建、PUT 全量更新、PATCH 部分更新、DELETE 删除
3. **状态码正确使用**：200 成功、201 创建成功、204 删除成功、400 参数错误、401 未认证、404 不存在、500 服务器错误
4. **版本控制**：`/api/v1/users` 或 `Accept: application/vnd.company.v1+json`
5. **分页/过滤/排序**：统一参数 `page/size/sort/order`/`field=value`

### 7.5 如何解决 SSM 项目的性能问题
**分层优化回答**：
1. **SQL 优化**：慢 SQL 日志、索引优化、explain 分析、避免 N+1 查询
2. **缓存优化**：Redis 缓存热点数据、MyBatis 一级/二级缓存配置
3. **连接池优化**：HikariCP 参数调优（池大小、超时、空闲检测）
4. **Tomcat 优化**：线程池大小、连接超时、acceptCount
5. **代码层**：批量操作代替循环单条操作、懒加载控制、DTO 精简返回值
6. **架构层**：读写分离、分库分表、异步化、CDN 加速静态资源

---

## 八、快速查漏补缺 Checklist

- [ ] DispatcherServlet 工作流程（doDispatch 步骤）
- [ ] HandlerMapping → HandlerMapping 映射原理
- [ ] HandlerAdapter 参数解析器（26 个默认解析器）
- [ ] 返回值处理器（HandlerMethodReturnValueHandler）
- [ ] 拦截器（HandlerInterceptor）三个方法
- [ ] 过滤器（Filter）vs 拦截器（Interceptor）
- [ ] ViewResolver 视图解析过程
- [ ] @ControllerAdvice 三个扩展点
- [ ] 异常处理优先级（@ExceptionHandler → HandlerExceptionResolver → Tomcat）
- [ ] RESTful 设计原则
- [ ] 内容协商（ContentNegotiation）
- [ ] 文件上传配置
- [ ] 数据校验（@Valid+JSR-303/@Validated）
- [ ] Spring 整合 MyBatis 核心配置
- [ ] SqlSessionFactoryBean（FactoryBean 实现）
- [ ] MapperScannerConfigurer（BDDRPP 实现）
- [ ] SqlSessionTemplate 线程安全原理
- [ ] Maven 分模块开发
- [ ] @CrossOrigin 跨域配置
- [ ] 统一返回格式封装（ResponseBodyAdvice）
- [ ] 异步处理（DeferredResult/Callable）
- [ ] JWT 认证整合
- [ ] SSM 项目模块划分
- [ ] 多环境（Profile）配置
- [ ] Druid 监控配置
- [ ] 请求日志记录（拦截器/Filter）
- [ ] 自定义参数解析器
- [ ] 分页查询通用封装
