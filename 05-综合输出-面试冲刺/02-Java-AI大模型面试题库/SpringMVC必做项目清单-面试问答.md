# SpringMVC 必做项目清单 面试问答
> 🎯 基于 SpringMVC 项目实战清单，涵盖面试高频问题与完美解答方案，帮助你在面试中脱颖而出。

## 目录
1. [基础概念与核心原理](#1-基础概念与核心原理)
2. [项目实战深度问答](#2-项目实战深度问答)
3. [进阶与系统设计](#3-进阶与系统设计)
4. [场景题与故障排查](#4-场景题与故障排查)

---

## 1. 基础概念与核心原理

> 💡 面试官在这一环节考察你的基本功是否扎实

### Q1：请说一下 SpringMVC 的请求处理流程（执行原理）

**面试官意图：** 考察对 SpringMVC 核心架构的理解，这是 Java Web 面试必考题。

**完美解答：**

SpringMVC 的核心是 **DispatcherServlet**，所有请求都经过它统一分发。完整的请求处理流程如下：

```
1. 用户发送请求 → DispatcherServlet（前端控制器）
2. DispatcherServlet → HandlerMapping（处理器映射器）
3. HandlerMapping 返回 HandlerExecutionChain（包含拦截器链 + Handler）
4. DispatcherServlet → HandlerAdapter（处理器适配器）
5. HandlerAdapter 执行 Controller 方法（调用真正的业务逻辑）
6. Controller 返回 ModelAndView
7. HandlerAdapter 返回 ModelAndView 给 DispatcherServlet
8. DispatcherServlet → ViewResolver（视图解析器）
9. ViewResolver 返回 View 对象
10. DispatcherServlet → View 渲染（将 Model 数据填充到 View）
11. 返回响应给客户端
```

**如果是前后端分离（@ResponseBody 场景），流程简化：**

```
DispatcherServlet → HandlerMapping → HandlerAdapter → Controller
                                                          ↓
DispatcherServlet ←  返回数据  ←  @ResponseBody(Jackson序列化)
     ↓
JSON 响应给客户端
```

**核心组件说明：**

| 组件 | 作用 | 说明 |
|------|------|------|
| DispatcherServlet | 前端控制器 | 统一接收请求，调度其他组件工作 |
| HandlerMapping | 处理器映射器 | 根据 URL 找到对应的 Controller 方法 |
| HandlerAdapter | 处理器适配器 | 适配不同类型的处理器执行（方法、Servlet 等） |
| ViewResolver | 视图解析器 | 根据逻辑视图名解析为物理视图 |
| HandlerInterceptor | 拦截器 | 在 handler 前后执行预处理和后处理 |

```java
// DispatcherServlet 核心逻辑（简化）
public class DispatcherServlet extends HttpServlet {
    
    protected void doDispatch(HttpServletRequest request, 
                               HttpServletResponse response) {
        // 1. 获取处理器执行链（Handler + 拦截器）
        HandlerExecutionChain chain = getHandler(request);
        
        // 2. 获取处理器适配器
        HandlerAdapter adapter = getHandlerAdapter(chain.getHandler());
        
        // 3. 执行拦截器 preHandle
        if (!chain.applyPreHandle(request, response)) {
            return; // 拦截器返回 false，不继续执行
        }
        
        // 4. 执行 Controller 方法
        ModelAndView mv = adapter.handle(request, response, chain.getHandler());
        
        // 5. 执行拦截器 postHandle
        chain.applyPostHandle(request, response, mv);
        
        // 6. 渲染视图
        processDispatchResult(request, response, mv);
    }
}
```

> 💡 理解了这个流程，就理解了 SpringMVC 的"约定优于配置"是怎么做到的——请求来了，找谁处理、怎么适配、怎么响应，都是框架已经安排好的。

---

### Q2：请说说 @Controller 和 @RestController 的区别，什么时候用哪个？

**面试官意图：** 考察 Web 开发的基础理解，以及前后端分离场景下的 API 设计。

**完美解答：**

```java
// @Controller：返回视图页面（传统 MVC）
@Controller
@RequestMapping("/page")
public class PageController {
    @GetMapping("/user")
    public String userPage(Model model) {
        model.addAttribute("name", "张三");
        return "user"; // 返回逻辑视图名，由视图解析器解析到 user.html/user.jsp
    }
}

// @RestController：返回 JSON 数据（前后端分离）
@RestController
@RequestMapping("/api/user")
public class UserApiController {
    @GetMapping("/{id}")
    public Result<User> getUser(@PathVariable Long id) {
        User user = userService.getById(id);
        return Result.success(user); // 直接返回对象，Jackson 序列化为 JSON
    }
}
```

**核心区别：**

| 对比维度 | @Controller | @RestController |
|---------|-------------|----------------|
| 返回值 | 视图名（ModelAndView） | 对象（JSON/XML） |
| 是否需要 @ResponseBody | 需要（否则走视图解析器） | 不需要（已经组合了 @ResponseBody） |
| 适用场景 | 传统 JSP/Thymeleaf 页面渲染 | 前后端分离、RESTful API |
| 内部组合 | 单一注解 | @Controller + @ResponseBody |

**但实际上 @RestController 就是 @Controller + @ResponseBody 的组合：**

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Controller
@ResponseBody
public @interface RestController {
    String value() default "";
}
```

> 💡 一个常见的坑：在 @RestController 中 try-catch 异常后手动返回 Result，但没有使用全局异常处理，导致异常信息不一致。正确做法是**统一用 @RestControllerAdvice 处理异常**，而不是在每个方法中 try-catch。

---

### Q3：请说说 SpringMVC 的参数绑定方式，各种注解的使用场景

**面试官意图：** 考察对请求参数处理的全面掌握，不遗漏常见场景。

**完美解答：**

SpringMVC 提供了丰富的参数绑定注解：

```java
@RestController
@RequestMapping("/user")
public class UserController {
    
    // 1. @RequestParam - 单个查询参数
    // URL: /user/list?page=1&size=10
    @GetMapping("/list")
    public Result<Page<User>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return Result.success(userService.page(page, size, keyword));
    }
    
    // 2. @PathVariable - URL 路径参数
    // URL: /user/100
    @GetMapping("/{id}")
    public Result<User> getById(@PathVariable Long id) {
        return Result.success(userService.getById(id));
    }
    
    // 3. @RequestBody - JSON 请求体（POST）
    // Body: {"name":"张三","age":25}
    @PostMapping
    public Result<Void> create(@Validated @RequestBody UserCreateReq req) {
        userService.create(req);
        return Result.success(null);
    }
    
    // 4. @ModelAttribute - 表单参数绑定到对象
    // POST: name=张三&age=25&email=test@qq.com
    @PostMapping("/form")
    public Result<Void> createFrom(@Validated @ModelAttribute UserCreateReq req) {
        userService.create(req);
        return Result.success(null);
    }
    
    // 5. @RequestHeader - 请求头参数
    @GetMapping("/header")
    public String getHeader(@RequestHeader("Authorization") String auth) {
        return auth;
    }
    
    // 6. @CookieValue - Cookie 参数
    @GetMapping("/cookie")
    public String getCookie(@CookieValue("token") String token) {
        return token;
    }

    // 7. 直接注入 Servlet API
    @GetMapping("/request")
    public void getRequest(HttpServletRequest request, 
                            HttpServletResponse response,
                            HttpSession session) {
        // 直接操作原生 Servlet 对象
    }
    
    // 8. @DateTimeFormat - 日期格式化
    @GetMapping("/date")
    public Result<Void> getByDate(
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime start) {
        return Result.success(null);
    }
}
```

**延伸追问应对：** 如果问"@RequestParam 和 @PathVariable 怎么选择？"，回答 RESTful 风格中路径参数用于标识资源（`/user/1`），查询参数用于过滤或分页（`/user?page=1&size=10`）。

---

## 2. 项目实战深度问答

> 💡 面试官会深挖你的项目细节，验证你"真的做过"而不是"背过"

### Q4：你在用户登录注册系统中，怎么处理 Session 管理、密码加密和登录拦截的？

**面试官意图：** 考察 Web 项目中最基础的安全处理，以及有没有良好的编码习惯。

**完美解答：**

**密码加密（绝对不能存明文）：**

```java
@Service
public class UserService {
    
    // BCrypt 加密，每次加密结果不同但可以校验
    public void register(RegisterReq req) {
        // 校验用户名是否已存在
        if (userMapper.existsByUsername(req.getUsername())) {
            throw new BusinessException("用户名已存在");
        }
        
        User user = new User();
        user.setUsername(req.getUsername());
        // BCrypt 自动加随机盐，不需要额外处理
        user.setPassword(BCrypt.hashpw(req.getPassword(), BCrypt.gensalt()));
        user.setCreateTime(LocalDateTime.now());
        userMapper.insert(user);
    }
    
    public User login(LoginReq req) {
        User user = userMapper.selectByUsername(req.getUsername());
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }
        // BCrypt 校验
        if (!BCrypt.checkpw(req.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误"); // 不具体提示哪个错了，防暴力枚举
        }
        return user;
    }
}
```

**Session 登录状态管理：**

```java
@PostMapping("/login")
public Result<UserVO> login(@RequestBody LoginReq req, HttpSession session) {
    User user = userService.login(req);
    // 将用户信息存入 Session
    UserVO vo = UserVO.from(user);
    session.setAttribute("user", vo);
    // 设置 Session 过期时间（秒）
    session.setMaxInactiveInterval(3600); // 1 小时
    return Result.success(vo);
}

@PostMapping("/logout")
public Result<Void> logout(HttpSession session) {
    session.invalidate(); // 使 Session 失效
    return Result.success(null);
}
```

**拦截器实现登录鉴权：**

```java
@Component
public class LoginInterceptor implements HandlerInterceptor {
    
    // 白名单——不需要登录就能访问的路径
    private static final List<String> WHITE_LIST = List.of(
        "/user/login", "/user/register", 
        "/", "/index", "/error",
        "/css/**", "/js/**", "/images/**"
    );
    
    @Override
    public boolean preHandle(HttpServletRequest request, 
                             HttpServletResponse response, 
                             Object handler) throws Exception {
        
        // 1. 如果是静态资源直接放行
        if (handler instanceof ResourceHttpRequestHandler) {
            return true;
        }
        
        // 2. 白名单路径放行
        String path = request.getRequestURI();
        for (String whitePath : WHITE_LIST) {
            if (whitePath.endsWith("/**")) {
                if (path.startsWith(whitePath.replace("/**", ""))) {
                    return true;
                }
            } else if (path.equals(whitePath)) {
                return true;
            }
        }
        
        // 3. 检查 Session
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            // 判断是否是 AJAX 请求
            String xRequestedWith = request.getHeader("X-Requested-With");
            if ("XMLHttpRequest".equals(xRequestedWith)) {
                // AJAX 请求返回 JSON
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write(JSON.toJSONString(Result.error(401, "未登录")));
            } else {
                // 页面请求重定向到登录页
                response.sendRedirect("/user/login");
            }
            return false;
        }
        
        return true;
    }
}

// 注册拦截器
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Autowired
    private LoginInterceptor loginInterceptor;
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
            .addPathPatterns("/**")
            .excludePathPatterns("/user/login", "/user/register", "/error");
    }
}
```

---

### Q5：你在项目中怎么实现全局异常处理和统一返回体的？

**面试官意图：** 考察 API 规范化的能力，以及 @RestControllerAdvice 的使用。

**完美解答：**

**统一返回体 Result 类：**

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {
    private Integer code;
    private String message;
    private T data;
    
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data);
    }
    
    public static <T> Result<T> success() {
        return new Result<>(200, "操作成功", null);
    }
    
    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null);
    }
}
```

**全局异常处理 @RestControllerAdvice：**

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    // 1. 自定义业务异常
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusiness(BusinessException e) {
        log.warn("业务异常：code={}, message={}", e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }
    
    // 2. 参数校验失败
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
            .map(f -> f.getField() + ":" + f.getDefaultMessage())
            .collect(Collectors.joining("; "));
        return Result.error(400, "参数错误：" + msg);
    }
    
    // 3. 参数类型转换异常
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraint(ConstraintViolationException e) {
        return Result.error(400, "参数校验失败：" + e.getMessage());
    }
    
    // 4. 404 找不到
    @ExceptionHandler(NoHandlerFoundException.class)
    public Result<Void> handleNotFound(NoHandlerFoundException e) {
        return Result.error(404, "请求的资源不存在");
    }
    
    // 5. HTTP 请求方法不支持
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<Void> handleMethodNotSupport(HttpRequestMethodNotSupportedException e) {
        return Result.error(405, "请求方法不支持，支持的方法：" + 
            Arrays.toString(e.getSupportedMethods()));
    }
    
    // 6. 兜底——所有未分类的异常
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e, HttpServletRequest request) {
        log.error("系统异常：URI={}", request.getRequestURI(), e);
        return Result.error(500, "系统繁忙，请稍后重试");
    }
}
```

> 💡 好的异常处理一定要**分类**：给用户看友好的提示，给排查人员看详细的日志。不要把堆栈信息抛给前端，也不要只打日志不给用户反馈。

---

### Q6：你在 SSM 整合博客系统中，事务是怎么控制的？遇到过什么坑？

**面试官意图：** 考察 SSM 项目中的事务配置和实战经验。

**完美解答：**

**在 Spring 配置文件中配置事务管理器：**

```xml
<!-- 配置事务管理器 -->
<bean id="transactionManager" 
      class="org.springframework.jdbc.datasource.DataSourceTransactionManager">
    <property name="dataSource" ref="dataSource"/>
</bean>

<!-- 启用注解驱动的事务管理 -->
<tx:annotation-driven transaction-manager="transactionManager"/>
```

**业务中使用 @Transactional：**

```java
@Service
public class ArticleServiceImpl implements ArticleService {
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishArticle(Article article, List<Long> tagIds) {
        // 1. 插入文章
        articleMapper.insert(article);
        
        // 2. 关联标签
        if (tagIds != null && !tagIds.isEmpty()) {
            articleTagMapper.batchInsert(article.getId(), tagIds);
        }
        
        // 3. 更新文章数量统计
        // 如果在第 3 步失败，第 1、2 步自动回滚
    }
}
```

**踩过的坑：**

**坑 1：MyBatis 的 `SqlSession` 没有交给 Spring 管理**

如果自己手动创建 `SqlSession` 而没有使用 Spring 管理的 `SqlSessionTemplate`，事务控制会失效。

**坑 2：多数据源事务**

在 SSM 后期项目中引入了多个数据源，`@Transactional` 默认只对一个数据源生效。解决方案是使用 **ChainedTransactionManager** 或 **Atomikos** 做 JTA 分布式事务。

**坑 3：try-catch 吃掉异常**

```java
@Transactional
public void doSomething() {
    try {
        // 业务代码
    } catch (Exception e) {
        log.error("错误", e);
        // 没有抛出异常，事务不会回滚！
    }
}
```

**怎么让事务在 catch 后仍然回滚：**

```java
@Transactional
public void doSomething() {
    try {
        // 业务代码
    } catch (Exception e) {
        log.error("错误", e);
        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
    }
}
```

---

### Q7：你的文件上传下载项目中，怎么处理大文件上传和进度显示？

**面试官意图：** 考察文件处理的经验，特别是大文件场景下的优化。

**完美解答：**

**文件上传配置：**

```xml
<!-- springmvc.xml: 配置文件上传解析器 -->
<bean id="multipartResolver" 
      class="org.springframework.web.multipart.commons.CommonsMultipartResolver">
    <!-- 最大上传 100MB -->
    <property name="maxUploadSize" value="104857600"/>
    <!-- 单个文件最大 50MB -->
    <property name="maxUploadSizePerFile" value="52428800"/>
    <!-- 默认编码 -->
    <property name="defaultEncoding" value="UTF-8"/>
</bean>
```

**Controller 实现：**

```java
@Controller
@RequestMapping("/file")
public class FileController {
    
    @PostMapping("/upload")
    @ResponseBody
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        // 1. 校验文件是否为空
        if (file.isEmpty()) {
            return Result.error(400, "文件不能为空");
        }
        
        // 2. 校验文件类型（白名单）
        String originalName = file.getOriginalFilename();
        String extension = originalName.substring(originalName.lastIndexOf("."));
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            return Result.error(400, "不支持的文件类型");
        }
        
        // 3. 生成唯一文件名（防止冲突和安全问题）
        String savedName = UUID.randomUUID().toString() + extension;
        
        // 4. 按日期分目录存储
        String datePath = new SimpleDateFormat("yyyy/MM/dd").format(new Date());
        String fullPath = UPLOAD_DIR + datePath + "/" + savedName;
        
        // 5. 保存文件
        File dest = new File(fullPath);
        dest.getParentFile().mkdirs();
        file.transferTo(dest);
        
        // 6. 返回访问 URL
        String fileUrl = "/files/" + datePath + "/" + savedName;
        return Result.success(fileUrl);
    }
}
```

**大文件断点续传方案：**

```java
// 前端分片上传，后端接收分片，最后合并
@PostMapping("/upload/chunk")
@ResponseBody
public Result<Void> uploadChunk(
        @RequestParam String fileId,    // 文件唯一标识
        @RequestParam int chunkIndex,   // 分片序号
        @RequestParam int totalChunks,  // 总分片数
        @RequestParam MultipartFile file) {
    
    // 1. 保存分片到临时目录
    String chunkDir = TEMP_DIR + fileId + "/";
    File chunkFile = new File(chunkDir + chunkIndex);
    chunkFile.getParentFile().mkdirs();
    file.transferTo(chunkFile);
    
    // 2. 检查是否所有分片都已上传
    if (isAllChunksUploaded(chunkDir, totalChunks)) {
        // 3. 合并分片
        String mergedPath = mergeChunks(fileId, chunkDir, totalChunks);
        // 4. 删除临时分片
        deleteChunks(chunkDir);
    }
    
    return Result.success(null);
}
```

---

## 3. 进阶与系统设计

> 💡 这是拉开差距的环节，考察架构思维

### Q8：请设计一个完整的后台权限管理系统，说说表结构和拦截逻辑

**面试官意图：** 考察 RBAC 权限模型的设计能力，以及拦截器 + 角色的组合使用。

**完美解答：**

**RBAC 权限模型（标准五表）：**

```sql
-- 1. 用户表
CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(100) NOT NULL,
    nickname VARCHAR(50),
    status TINYINT DEFAULT 1,
    create_time DATETIME
);

-- 2. 角色表
CREATE TABLE sys_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_name VARCHAR(50) UNIQUE NOT NULL,
    role_code VARCHAR(50) UNIQUE NOT NULL,  -- 如 "admin", "user"
    status TINYINT DEFAULT 1
);

-- 3. 菜单/权限表
CREATE TABLE sys_menu (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    parent_id BIGINT DEFAULT 0,
    menu_name VARCHAR(50) NOT NULL,
    permission VARCHAR(100),          -- 权限标识，如 "system:user:list"
    path VARCHAR(200),                -- 前端路由路径
    component VARCHAR(200),           -- 前端组件路径
    type TINYINT,                     -- 0目录 1菜单 2按钮
    sort_order INT DEFAULT 0,
    status TINYINT DEFAULT 1
);

-- 4. 用户角色关联表
CREATE TABLE sys_user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id)
);

-- 5. 角色菜单关联表
CREATE TABLE sys_role_menu (
    role_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, menu_id)
);
```

**权限拦截器实现：**

```java
@Component
public class PermissionInterceptor implements HandlerInterceptor {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Override
    public boolean preHandle(HttpServletRequest request,
                              HttpServletResponse response,
                              Object handler) throws Exception {
        // 1. 从请求中获取用户 Token
        String token = request.getHeader("Authorization");
        if (token == null) {
            writeError(response, 401, "未登录");
            return false;
        }
        
        // 2. 获取用户权限列表（从 Redis 缓存中）
        @SuppressWarnings("unchecked")
        Set<String> permissions = (Set<String>) redisTemplate.opsForValue()
            .get("permissions:" + token);
        if (permissions == null) {
            writeError(response, 401, "登录已过期");
            return false;
        }
        
        // 3. 获取当前请求需要的权限
        String requiredPermission = getRequiredPermission(request);
        
        // 4. 如果是超级管理员，直接放行
        if (permissions.contains("*")) {
            return true;
        }
        
        // 5. 校验权限
        if (!permissions.contains(requiredPermission)) {
            writeError(response, 403, "无权限访问");
            return false;
        }
        
        return true;
    }
    
    private String getRequiredPermission(HttpServletRequest request) {
        // 从请求方法和 URL 映射到权限标识
        // 如 GET /api/user -> system:user:list
        //    POST /api/user -> system:user:create
        String method = request.getMethod();
        String uri = request.getRequestURI();
        // 根据映射规则查询权限标识
        return permissionMapping.getPermission(method, uri);
    }
}
```

---

### Q9：说说 SpringMVC 中拦截器和过滤器的区别，项目中怎么选的？

**面试官意图：** 考察对两种拦截机制的理解，以及在实际场景中的选型能力。

**完美解答：**

| 对比维度 | Filter（过滤器） | Interceptor（拦截器） |
|---------|-----------------|---------------------|
| 所属容器 | Servlet 容器 | Spring 容器 |
| 配置方式 | web.xml / @WebFilter | Spring 配置 / WebMvcConfigurer |
| 拦截范围 | 所有 URL（包括静态资源） | 仅 Controller 请求 |
| 是否可获取 IoC Bean | 否（需特殊配置） | 是（直接注入） |
| 粒度 | 粗粒度（只能拦截 URL） | 细粒度（可拦截特定 Controller、方法） |
| 执行顺序 | 先 Filter 后 Interceptor | 在 Filter 之后执行 |
| 是否可多次调用 | 不支持 | 支持（preHandle/postHandle/afterCompletion） |

**实际项目中的选择：**

```java
// 1. 用 Filter：字符编码、CORS 跨域、XSS 过滤（不依赖 Spring 容器）
@WebFilter("/*")
public class CharacterEncodingFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, 
                         FilterChain chain) {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        chain.doFilter(request, response);
    }
}

// 2. 用 Interceptor：登录鉴权、权限校验、请求日志（需要注入 Spring Bean）
@Component
public class LoginInterceptor implements HandlerInterceptor {
    @Autowired
    private UserService userService; // 直接注入，Filter 做不到
    
    @Override
    public boolean preHandle(HttpServletRequest request,
                              HttpServletResponse response,
                              Object handler) {
        // 权限校验逻辑
    }
}
```

> 💡 总结：**跟 Servlet 请求本身相关的用 Filter，跟业务逻辑相关的用 Interceptor**。Filter 优先级更高、范围更广；Interceptor 更细粒度、更容易和 Spring 集成。

---

## 4. 场景题与故障排查

> 💡 考察实际解决问题的能力

### Q10：你遇到过乱码问题吗？在 SpringMVC 项目中怎么解决的？

**面试官意图：** 考察 Web 开发中最常见的编码问题处理经验。

**完美解答：**

**乱码分为三个层面：请求乱码、响应乱码、数据库乱码。**

**1. POST 请求参数乱码**

```xml
<!-- web.xml：配置 Spring 的编码过滤器 -->
<filter>
    <filter-name>encodingFilter</filter-name>
    <filter-class>org.springframework.web.filter.CharacterEncodingFilter</filter-class>
    <init-param>
        <param-name>encoding</param-name>
        <param-value>UTF-8</param-value>
    </init-param>
    <init-param>
        <param-name>forceEncoding</param-name>
        <param-value>true</param-value>
    </init-param>
</filter>
<filter-mapping>
    <filter-name>encodingFilter</filter-name>
    <url-pattern>/*</url-pattern>
</filter-mapping>
```

**2. GET 请求乱码（Tomcat 默认使用 ISO-8859-1 解析 URL）**

```xml
<!-- Tomcat server.xml：修改 URI 编码 -->
<Connector port="8080" protocol="HTTP/1.1"
           URIEncoding="UTF-8"
           connectionTimeout="20000"
           redirectPort="8443"/>
```

或者手动编码转换：
```java
@GetMapping("/search")
public String search(@RequestParam String keyword) {
    // 如果 GET 请求乱码，手动转码
    keyword = new String(keyword.getBytes(StandardCharsets.ISO_8859_1), 
                         StandardCharsets.UTF_8);
}
```

**3. 响应乱码**

```java
// 方法一：在 @RequestMapping 中指定 produces
@GetMapping(value = "/data", produces = "application/json;charset=UTF-8")
@ResponseBody
public String getData() { ... }

// 方法二：配置 StringHttpMessageConverter
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        StringHttpMessageConverter converter = new StringHttpMessageConverter(StandardCharsets.UTF_8);
        converters.add(converter);
    }
}
```

**4. 数据库乱码**

```yaml
# JDBC URL 指定编码
jdbc:mysql://localhost:3306/db_name?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
```

> 💡 解决乱码的核心原则是保证**所有环节编码一致**：页面编码 -> 请求编码 -> 应用编码 -> 数据库编码全部统一为 UTF-8。

---

### Q11：SpringMVC 请求参数接收不到，返回 400 错误，怎么排查？

**面试官意图：** 考察参数绑定问题的排查思路。

**完美解答：**

**400 Bad Request 表示请求格式有误，服务端无法解析。**

**常见原因和排查：**

**原因 1：Content-Type 不匹配**

```java
@PostMapping("/user")
public Result<Void> create(@RequestBody UserCreateReq req) { ... }
```

如果前端传的 Content-Type 是 `application/x-www-form-urlencoded` 而不是 `application/json`，`@RequestBody` 无法解析。

**原因 2：JSON 字段名和 Java 字段名不匹配**

```json
// 前端传：
{"userName": "张三", "userAge": 25}

// Java 类定义：
private String username;  // 字段名不匹配
private Integer age;
```

解决方案：使用 `@JsonProperty` 映射或统一命名规范。

**原因 3：日期格式问题**

```java
private Date createTime; // 前端传 "2024-01-15 10:30:00"
```

如果 `String` 转 `Date` 格式不匹配会报 400。解决方案是全局配置日期格式：

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
        objectMapper.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
        converter.setObjectMapper(objectMapper);
        converters.add(0, converter);
    }
}
```

**原因 4：枚举类型转换失败**

如果请求参数是枚举类型，前端传的值必须和枚举定义一致。

**排查方法：**
1. 打开 DEBUG 日志看 SpringMVC 的绑定过程
```xml
<logger name="org.springframework.web" level="DEBUG"/>
```
2. 用 Postman/curl 测试相同的请求，排除前端问题
3. 查看是否有自定义的 `Converter` 或 `Formatter` 注册

---

### Q12：SpringMVC 静态资源被拦截了怎么办？

**面试官意图：** 考察静态资源处理和拦截器配置的常见问题。

**完美解答：**

**问题：** 在项目中使用拦截器后，CSS/JS/图片等静态资源请求也被拦截，页面样式丢失。

**解决方案：**

**方案一：在 SpringMVC 配置中放行静态资源**

```xml
<!-- springmvc.xml -->
<mvc:resources mapping="/css/**" location="/static/css/"/>
<mvc:resources mapping="/js/**" location="/static/js/"/>
<mvc:resources mapping="/images/**" location="/static/images/"/>
<mvc:resources mapping="/favicon.ico" location="/static/favicon.ico"/>
```

或使用 Java 配置：

```java
@Configuration
@EnableWebMvc
public class WebConfig implements WebMvcConfigurer {
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 将 /static/** 的请求映射到 classpath:/static/ 目录
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");
        
        // 文件上传的访问路径映射
        registry.addResourceHandler("/files/**")
                .addResourceLocations("file:D:/uploads/");
    }
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
            .addPathPatterns("/**")
            .excludePathPatterns("/static/**", "/user/login", "/error");
    }
}
```

**方案二：使用默认 Servlet 处理静态资源（配合 web.xml）**

```xml
<!-- web.xml -->
<servlet-mapping>
    <servlet-name>default</servlet-name>
    <url-pattern>*.css</url-pattern>
    <url-pattern>*.js</url-pattern>
    <url-pattern>*.png</url-pattern>
    <url-pattern>*.jpg</url-pattern>
</servlet-mapping>
```

**方案三：在拦截器中判断是否是静态资源并放行**

```java
@Component
public class LoginInterceptor implements HandlerInterceptor {
    
    @Override
    public boolean preHandle(HttpServletRequest request, 
                             HttpServletResponse response, 
                             Object handler) throws Exception {
        
        // 判断是否是静态资源
        if (handler instanceof ResourceHttpRequestHandler) {
            return true; // 静态资源直接放行
        }
        
        // 继续其他鉴权逻辑...
    }
}
```

> 💡 最佳实践是**在 `addInterceptors` 中使用 `excludePathPatterns` 明确排除静态资源路径**，而不是在拦截器代码中硬编码判断。

---

## 💎 面试加分金句

1. "理解 SpringMVC 最重要的就是理解 **DispatcherServlet 的分发逻辑**——从请求到 HandlerMapping 到 HandlerAdapter 再到 ViewResolver，每个组件各自做一件事，贯穿起来就是一个完整的请求处理链路。"
2. "在 SSM 项目中，我最重视的是**三层架构的清晰分离**——Controller 只做参数校验和结果返回，Service 只做业务逻辑，Mapper 只做数据操作。每一层不乱入其他职责。"
3. "前后端分离后，@RestControllerAdvice 全局异常处理成了标配。我设计的异常处理规范是：**系统异常走兜底 500，业务异常走自定义 4xx，参数异常走 400**。"
4. "拦截器和过滤器的选择我遵循一个原则：**和业务逻辑相关的用拦截器，和 HTTP 协议相关的用过滤器**。比如编码、CORS 用过滤器；鉴权、日志用拦截器。"
5. "文件上传中最大的安全风险是**上传可执行文件**，我用三层防护：白名单扩展名校验 -> 文件头魔数校验 -> 重命名存储。三步缺一不可。"

---

## 📋 高频追问清单

| 追问方向 | 应对策略 |
|----------|----------|
| "转发和重定向有什么区别？" | 转发是一次请求（服务端内部），地址栏不变；重定向是两次请求（客户端跳转），地址栏改变 |
| "怎么获取 request 和 response？" | 方法参数直接注入，或通过 `RequestContextHolder` 获取 |
| "@SessionAttribute 和 @RequestAttribute 区别？" | 前者从 Session 取，后者从 Request 域取 |
| "异步请求怎么处理？" | 使用 `Callable` 或 `DeferredResult` 实现异步返回 |
| "SpringMVC 统一异常处理方式有哪些？" | 三种：HandlerExceptionResolver、@ExceptionHandler、@ControllerAdvice |

---

## 🔗 关联知识点

- [Spring必做项目清单-面试问答](Spring必做项目清单-面试问答.md) — IoC/AOP 底层原理
- [SpringBoot必做项目清单-面试问答](SpringBoot必做项目清单-面试问答.md) — SpringBoot 自动配置与项目实战
- [Java必做项目清单-面试问答](Java必做项目清单-面试问答.md) — 综合项目面试问答
