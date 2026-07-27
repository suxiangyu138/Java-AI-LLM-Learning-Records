# SpringBoot 必做项目清单 面试问答
> 🎯 基于 SpringBoot 项目实战清单，涵盖面试高频问题与完美解答方案，帮助你在面试中脱颖而出。

## 目录
1. [基础概念与核心原理](#1-基础概念与核心原理)
2. [项目实战深度问答](#2-项目实战深度问答)
3. [进阶与系统设计](#3-进阶与系统设计)
4. [场景题与故障排查](#4-场景题与故障排查)

---

## 1. 基础概念与核心原理

> 💡 面试官在这一环节考察你的基本功是否扎实

### Q1：请说一下 SpringBoot 自动配置的原理，`@SpringBootApplication` 做了什么？

**面试官意图：** 考察对 SpringBoot 最核心的自动配置机制的理解深度，而不是只会写启动类。

**完美解答：**

`@SpringBootApplication` 是一个组合注解，由三个核心注解组成：

```java
@SpringBootConfiguration   // 标记为配置类
@EnableAutoConfiguration   // 开启自动配置
@ComponentScan             // 包扫描
```

**自动配置的核心机制：**

```java
@EnableAutoConfiguration
→ @Import(AutoConfigurationImportSelector.class)
```

`AutoConfigurationImportSelector` 内部通过 **Spring SPI 机制**加载自动配置类：

**加载流程详解：**

1. Spring Boot 启动时，`AutoConfigurationImportSelector` 扫描所有 jar 包中的 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 文件

2. 读取该文件中配置的自动配置类列表（如 `RedisAutoConfiguration`、`DataSourceAutoConfiguration` 等）

3. 遍历这些配置类，检查每个配置类上的 `@Conditional` 条件注解是否满足条件

4. 条件满足的配置类就会被注册为 Bean

```java
// 典型的自动配置类示例
@AutoConfiguration
@ConditionalOnClass(RedisOperations.class)  // classpath 中有 Redis 相关类才生效
@EnableConfigurationProperties(RedisProperties.class)
public class RedisAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean(name = "redisTemplate")
    public RedisTemplate<Object, Object> redisTemplate(
            RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<Object, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        return template;
    }
}
```

**条件注解家族：**

| 注解 | 作用 | 使用场景 |
|------|------|---------|
| `@ConditionalOnClass` | classpath 中存在指定类时生效 | 按依赖自动配置 |
| `@ConditionalOnMissingBean` | 容器中没有指定 Bean 时生效 | 允许用户覆盖默认配置 |
| `@ConditionalOnProperty` | 配置文件中有指定属性时生效 | 通过配置开关控制 |
| `@ConditionalOnExpression` | SpEL 表达式为 true 时生效 | 复杂条件判断 |
| `@ConditionalOnWebApplication` | Web 应用时生效 | 区分 Web/非 Web |

> 💡 理解自动配置原理的关键在于**条件化注册**——Spring Boot 不是一股脑加载所有配置，而是按"你引入了什么依赖"来判断"你需要什么配置"。

**延伸追问应对：** 如果问"怎么自定义 Starter？"，回答需要做四件事：1. 写自动配置类 2. 定义配置属性类（`@ConfigurationProperties`）3. 在 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 中注册 4. 打包为独立 jar。

---

### Q2：SpringBoot 中如何实现统一异常处理和统一返回体？

**面试官意图：** 考察 RESTful API 的规范化设计，这是企业项目最基本的要求。

**完美解答：**

**统一返回体：**

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {
    private Integer code;
    private String message;
    private T data;
    private Long timestamp;
    
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "success", data, System.currentTimeMillis());
    }
    
    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null, System.currentTimeMillis());
    }
    
    public static <T> Result<T> error(String message) {
        return new Result<>(500, message, null, System.currentTimeMillis());
    }
}
```

**统一异常处理：**

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    // 业务异常
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusiness(BusinessException e) {
        log.warn("业务异常：{}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }
    
    // 参数校验异常
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ":" + error.getDefaultMessage())
            .collect(Collectors.joining(", "));
        return Result.error(400, "参数校验失败：" + message);
    }
    
    // 参数类型不匹配
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraint(ConstraintViolationException e) {
        return Result.error(400, "参数校验失败：" + e.getMessage());
    }
    
    // 404
    @ExceptionHandler(NoHandlerFoundException.class)
    public Result<Void> handleNotFound(NoHandlerFoundException e) {
        return Result.error(404, "接口不存在");
    }
    
    // 兜底异常
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e, HttpServletRequest request) {
        log.error("未知异常：{} {}", request.getRequestURI(), e.getMessage(), e);
        return Result.error(500, "系统繁忙，请稍后重试");
    }
}
```

**自定义业务异常：**

```java
@Data
@EqualsAndHashCode(callSuper = true)
public class BusinessException extends RuntimeException {
    private Integer code;
    
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
    
    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }
}
```

> 💡 好的异常处理要**分类清晰**——业务异常给用户友好的提示，系统异常给技术排查信息，不要把堆栈信息直接吐给前端。

---

### Q3：SpringBoot 中参数校验你是怎么做的？复杂的校验场景怎么处理？

**面试官意图：** 考察对 Bean Validation 的掌握程度以及复杂场景的处理经验。

**完美解答：**

**基础参数校验：**

```java
@Data
public class UserCreateReq {
    
    @NotBlank(message = "用户名不能为空")
    @Length(min = 2, max = 20, message = "用户名长度 2-20 个字符")
    private String username;
    
    @NotBlank(message = "密码不能为空")
    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d).{6,20}$", 
             message = "密码需包含字母和数字，长度 6-20")
    private String password;
    
    @NotNull(message = "年龄不能为空")
    @Min(value = 1, message = "年龄不能小于 1")
    @Max(value = 150, message = "年龄不能大于 150")
    private Integer age;
    
    @Email(message = "邮箱格式不正确")
    private String email;
    
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
}
```

**分组校验（不同场景校验不同字段）：**

```java
@Data
public class UserReq {
    @Null(groups = Create.class, message = "创建时 ID 必须为空")
    @NotNull(groups = Update.class, message = "更新时 ID 不能为空")
    private Long id;
    
    @NotBlank(groups = Create.class, message = "创建时密码必填")
    private String password;
    
    private String nickname;  // 创建和更新都不校验
    
    public interface Create {}
    public interface Update {}
}

// Controller 中使用
@PostMapping("/user")
public Result<Void> create(@Validated(UserReq.Create.class) @RequestBody UserReq req) {
    userService.create(req);
    return Result.success(null);
}

@PutMapping("/user")
public Result<Void> update(@Validated(UserReq.Update.class) @RequestBody UserReq req) {
    userService.update(req);
    return Result.success(null);
}
```

**自定义校验注解：**

```java
@Target({FIELD, PARAMETER})
@Retention(RUNTIME)
@Constraint(validatedBy = EnumValidator.class)
public @interface ValidEnum {
    String message() default "枚举值不合法";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    Class<? extends Enum> enumClass();
}

public class EnumValidator implements ConstraintValidator<ValidEnum, Object> {
    private Object[] values;
    
    @Override
    public void initialize(ValidEnum annotation) {
        values = annotation.enumClass().getEnumConstants();
    }
    
    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) return true;
        for (Object enumValue : values) {
            if (enumValue.toString().equals(value.toString())) return true;
        }
        return false;
    }
}

// 使用
@ValidEnum(enumClass = OrderStatus.class, message = "订单状态不合法")
private String status;
```

---

## 2. 项目实战深度问答

> 💡 面试官会深挖你的项目细节，验证你"真的做过"而不是"背过"

### Q4：你做的个人博客后端的整体架构和核心设计是什么样的？

**面试官意图：** 考察 SpringBoot 基础项目的完整设计能力，包括分层、权限、数据关系等。

**完美解答：**

**技术栈：** SpringBoot + MyBatis-Plus + MySQL + Redis + Knife4j

**项目分层架构：**

```
controller/        # 接收请求，参数校验，调用 service
    ├── ArticleController
    ├── UserController
    ├── CommentController
    └── CategoryController
service/           # 业务逻辑层
    ├── impl/
    └── service/
mapper/            # 数据访问层 (MyBatis-Plus)
entity/             # 数据库实体
dto/                # 数据传输对象 (请求/响应)
config/             # 配置类
    ├── WebMvcConfig      # 跨域、拦截器
    ├── RedisConfig
    └── Knife4jConfig
exception/          # 异常定义
util/               # 工具类
```

**核心数据表设计：**

```sql
-- 文章表
CREATE TABLE article (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    content LONGTEXT,
    summary VARCHAR(500),
    cover_image VARCHAR(500),
    category_id BIGINT,
    author_id BIGINT NOT NULL,
    status TINYINT DEFAULT 0,  -- 0 草稿 1 发布 2 删除
    view_count INT DEFAULT 0,
    comment_count INT DEFAULT 0,
    create_time DATETIME,
    update_time DATETIME
);

-- 评论表
CREATE TABLE comment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    article_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    parent_id BIGINT,       -- 父评论 ID（支持嵌套回复）
    content TEXT NOT NULL,
    status TINYINT DEFAULT 1,
    create_time DATETIME
);
```

**权限控制设计：**

采用了简单的角色判断机制：博客文章中区分**作者**和**管理员**。作者只能编辑自己的文章，管理员可以编辑所有文章。点赞和评论使用 Redis 实现，通过拦截器从 Session 中获取当前用户：

```java
@Component
public class LoginInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, 
                             HttpServletResponse response, 
                             Object handler) {
        // 如果是 OPTIONS 请求，直接放行（跨域预检）
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("user");
        if (user == null) {
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(JSON.toJSONString(Result.error(401, "请先登录")));
            return false;
        }
        // 将用户信息存入 ThreadLocal，方便后续获取
        UserContext.set(user);
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, 
                                HttpServletResponse response, 
                                Object handler, Exception ex) {
        UserContext.clear(); // 防止内存泄漏
    }
}
```

---

### Q5：你在 RBAC 权限管理系统（前后端分离）中，JWT 是怎么设计和实现的？

**面试官意图：** 考察 JWT 在前后端分离项目中的实战经验，包括 Token 刷新、续期、安全等细节。

**完美解答：**

**JWT 结构：**

JWT 分为三部分：`Header.Payload.Signature`，用 base64 编码，可读但不可篡改。

```java
@Data
public class JwtPayload {
    private Long userId;        // 用户 ID
    private String username;    // 用户名
    private List<String> roles; // 角色列表
    private List<String> permissions; // 权限标识列表
    // 标准声明
    private String iss;         // 签发者
    private Long exp;           // 过期时间
    private Long iat;           // 签发时间
}
```

**Token 设计（双 Token 机制）：**

```java
@Component
public class JwtTokenProvider {
    
    @Value("${jwt.access-token-expire:3600}")   // 1 小时
    private long accessTokenExpire;
    @Value("${jwt.refresh-token-expire:604800}") // 7 天
    private long refreshTokenExpire;
    @Value("${jwt.secret}")
    private String secret;
    
    // 生成 Access Token
    public String generateAccessToken(Long userId, String username, 
                                       List<String> roles) {
        return Jwts.builder()
            .setSubject(String.valueOf(userId))
            .claim("username", username)
            .claim("roles", roles)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + 
                accessTokenExpire * 1000))
            .signWith(SignatureAlgorithm.HS256, secret)
            .compact();
    }
    
    // 生成 Refresh Token（有效期更长）
    public String generateRefreshToken(Long userId) {
        return Jwts.builder()
            .setSubject(String.valueOf(userId))
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + 
                refreshTokenExpire * 1000))
            .signWith(SignatureAlgorithm.HS256, secret)
            .compact();
    }
    
    // 刷新 Token
    public String refreshAccessToken(String refreshToken) {
        if (!validateToken(refreshToken)) {
            throw new BusinessException("Token 已过期，请重新登录");
        }
        Long userId = Long.parseLong(getSubject(refreshToken));
        User user = userService.getById(userId);
        return generateAccessToken(userId, user.getUsername(), user.getRoles());
    }
}
```

**认证过滤器：**

```java
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain chain) {
        String token = extractToken(request);
        if (token != null && jwtTokenProvider.validateToken(token)) {
            Authentication auth = jwtTokenProvider.getAuthentication(token);
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        chain.doFilter(request, response);
    }
    
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
```

**RBAC 权限控制实现：**

```java
// 自定义权限注解
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface PreAuthorize {
    String value(); // 如 "system:user:create"
}

// AOP 实现权限校验
@Aspect
@Component
public class PreAuthorizeAspect {
    
    @Around("@annotation(preAuthorize)")
    public Object checkPermission(ProceedingJoinPoint joinPoint, 
                                   PreAuthorize preAuthorize) {
        String permission = preAuthorize.value();
        User currentUser = UserContext.get();
        if (!currentUser.getPermissions().contains(permission)) {
            throw new BusinessException(403, "无权限访问");
        }
        return joinPoint.proceed();
    }
}
```

---

### Q6：你做的 Redis 缓存商品详情系统中，怎么解决缓存穿透/击穿/雪崩的？

**面试官意图：** 考察对缓存三大问题的实战解决方案。

**完美解答：**

**第一层：缓存穿透（查询不存在的数据）**

用布隆过滤器预处理，拦截明显不存在的 key：

```java
@Component
public class BloomFilterService {
    
    private final BloomFilter<Long> bloomFilter;
    
    @PostConstruct
    public void init() {
        // 初始化布隆过滤器，预期 10 万数据，误判率 1%
        bloomFilter = BloomFilter.create(Funnels.longFunnel(), 100000, 0.01);
        // 加载所有存在的商品 ID
        List<Long> allIds = productService.getAllProductIds();
        allIds.forEach(bloomFilter::put);
    }
    
    public boolean mightContain(Long id) {
        return bloomFilter.mightContain(id);
    }
}

// 使用
public Product getProduct(Long id) {
    if (!bloomFilterService.mightContain(id)) {
        throw new BusinessException("商品不存在");
    }
    // 查缓存 -> 查数据库
}
```

**第二层：缓存击穿（热点 key 过期）**

使用**逻辑过期**方案——缓存中不设置 TTL，而是存一个逻辑过期时间：

```java
@Data
public class CacheData<T> {
    private T data;
    private LocalDateTime expireTime; // 逻辑过期时间
}

public Product getProduct(Long id) {
    String cacheKey = "product:" + id;
    String json = redisTemplate.opsForValue().get(cacheKey);
    if (json == null) {
        return loadFromDb(id); // 首次加载
    }
    
    CacheData<Product> cacheData = JSON.parseObject(json, 
        new TypeReference<CacheData<Product>>(){});
    
    // 逻辑过期时间未到，直接返回
    if (cacheData.getExpireTime().isAfter(LocalDateTime.now())) {
        return cacheData.getData();
    }
    
    // 逻辑过期已到，尝试获取互斥锁去更新
    String lockKey = "lock:product:" + id;
    if (redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 3, TimeUnit.SECONDS)) {
        try {
            Product product = productMapper.selectById(id);
            CacheData<Product> newCache = new CacheData<>();
            newCache.setData(product);
            newCache.setExpireTime(LocalDateTime.now().plusMinutes(30));
            redisTemplate.opsForValue().set(cacheKey, JSON.toJSONString(newCache));
            return product;
        } finally {
            redisTemplate.delete(lockKey);
        }
    }
    
    // 没抢到锁的线程，直接返回旧缓存数据
    return cacheData.getData();
}
```

**第三层：缓存雪崩（大量 key 同时过期）**

```java
// 过期时间 = 基础时间 + 随机偏移
public void setProductCache(Long id, Product product) {
    int baseExpire = 60 * 30; // 30 分钟
    int random = new Random().nextInt(600); // 0-10 分钟随机
    redisTemplate.opsForValue().set(
        "product:" + id, 
        JSON.toJSONString(product), 
        baseExpire + random, 
        TimeUnit.SECONDS
    );
}
```

同时配合**多级缓存**：本地 Caffeine 缓存做第一级（几秒的短过期），Redis 做第二级（分钟级过期），数据库兜底。

---

## 3. 进阶与系统设计

> 💡 这是拉开差距的环节，考察架构思维

### Q7：怎么设计一个高可用的文件上传下载系统？

**面试官意图：** 考察文件系统的完整设计能力，包括大文件处理、断点续传、权限控制等。

**完美解答：**

**架构设计：**

```yaml
客户端上传 -> 文件类型/大小校验 -> 上传到 OSS/MinIO -> 记录文件元数据到 DB -> 返回文件 URL
                                                                        |
客户端下载 -> 鉴权 -> 生成临时访问 URL -> 重定向到 OSS 直链
```

**核心实现：**

**1. 大文件分片上传**

```java
@RestController
@RequestMapping("/file")
public class FileController {
    
    // 初始化分片上传
    @PostMapping("/upload/init")
    public Result<InitVO> initUpload(@RequestBody FileInitReq req) {
        String uploadId = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(
            "upload:" + uploadId, 
            JSON.toJSONString(req), 
            1, TimeUnit.DAYS
        );
        return Result.success(new InitVO(uploadId, req.getFileName()));
    }
    
    // 上传分片
    @PostMapping("/upload/chunk")
    public Result<Void> uploadChunk(@RequestParam String uploadId,
                                     @RequestParam Integer chunkIndex,
                                     @RequestParam MultipartFile file) {
        // 保存分片到临时目录
        String chunkPath = storageService.saveChunk(uploadId, chunkIndex, file);
        // 记录分片上传状态
        redisTemplate.opsForHash().put("upload:" + uploadId + ":chunks", 
                                       String.valueOf(chunkIndex), chunkPath);
        return Result.success(null);
    }
    
    // 合并分片
    @PostMapping("/upload/merge")
    public Result<String> mergeChunks(@RequestParam String uploadId) {
        // 校验所有分片已上传
        Map<Object, Object> chunks = redisTemplate.opsForHash()
            .entries("upload:" + uploadId + ":chunks");
        // 按顺序合并
        String fileUrl = storageService.mergeChunks(uploadId, chunks);
        return Result.success(fileUrl);
    }
}
```

**2. 文件类型白名单校验**

```java
@Component
public class FileValidationService {
    
    // 白名单（禁止使用黑名单，攻击者可以绕过后缀名）
    private static final Set<String> ALLOWED_TYPES = Set.of(
        "image/jpeg", "image/png", "image/gif",
        "application/pdf",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "video/mp4"
    );
    
    public void validate(MultipartFile file) {
        // 1. 校验文件大小
        if (file.getSize() > 100 * 1024 * 1024) { // 100MB
            throw new BusinessException("文件大小超过限制");
        }
        // 2. 校验 MIME 类型（读取文件头字节，不依赖前端传的 content-type）
        String magicNumber = getFileMagicNumber(file);
        if (!isAllowedMagicNumber(magicNumber)) {
            throw new BusinessException("不支持的文件类型");
        }
        // 3. 校验文件扩展名
        String extension = getExtension(file.getOriginalFilename());
        if (!isAllowedExtension(extension)) {
            throw new BusinessException("不允许的文件扩展名");
        }
    }
}
```

**3. 下载权限控制**

```java
// 生成临时下载 URL（带过期时间签名）
public String generateDownloadUrl(Long fileId, Long userId) {
    String token = AES.encrypt(fileId + ":" + userId + ":" + 
                               (System.currentTimeMillis() + 600_000)); // 10 分钟有效
    return "/file/download/" + fileId + "?token=" + URLEncoder.encode(token);
}

@GetMapping("/download/{fileId}")
public void download(@PathVariable Long fileId, 
                      @RequestParam String token,
                      HttpServletResponse response) {
    // 校验 Token
    String decrypted = AES.decrypt(token);
    String[] parts = decrypted.split(":");
    // 检查过期时间
    if (Long.parseLong(parts[2]) < System.currentTimeMillis()) {
        throw new BusinessException("下载链接已过期");
    }
    // 下载文件
    FileMeta meta = fileService.getFile(fileId);
    // 设置下载响应头
    response.setContentType(meta.getContentType());
    response.setHeader("Content-Disposition", 
        "attachment; filename=" + URLEncoder.encode(meta.getFileName(), "UTF-8"));
    // 流式输出
    storageService.download(fileId, response.getOutputStream());
}
```

---

### Q8：请讲一下你的 RAG 知识库问答系统的技术架构和实现细节

**面试官意图：** 考察 Java + AI 的融合能力，这是拉开差距的差异化竞争力。

**完美解答：**

**系统架构：**

```
用户输入 -> 问题解析 -> 向量检索 -> 上下文拼接 -> 大模型生成 -> 流式输出
                                                     ↑
                                        Ollama 本地模型 / 云端 API
```

**核心实现：**

**1. 文档处理流水线**

```java
@Component
public class DocumentPipeline {
    
    public List<DocumentChunk> process(MultipartFile file) {
        // 1. 文档解析
        String text = fileParser.parse(file);  // PDF/MD/HTML 解析
        
        // 2. 文档分块（chunking）
        List<DocumentChunk> chunks = chunker.split(text, ChunkConfig.builder()
            .chunkSize(512)          // 每块 512 tokens
            .overlap(128)            // 重叠 128 tokens（保持上下文连续性）
            .separators(List.of("\n## ", "\n### ", "\n", ". "))  // 优先按标题分割
            .build());
        
        // 3. 向量化并存储
        for (DocumentChunk chunk : chunks) {
            float[] embedding = embeddingService.embed(chunk.getContent());
            chunk.setEmbedding(embedding);
            vectorStore.store(chunk);  // 存入 Milvus
        }
        
        return chunks;
    }
}
```

**2. 检索增强生成**

```java
@Service
public class RagService {
    
    public Flux<String> streamAnswer(String question, String sessionId) {
        // 1. 问题向量化
        float[] questionEmbedding = embeddingService.embed(question);
        
        // 2. 向量检索（召回 top-5 最相关文档块）
        List<DocumentChunk> relevantDocs = vectorStore.search(
            questionEmbedding, 5, 0.7 // 召回 5 条，相似度阈值 0.7
        );
        
        // 3. 获取对话历史
        List<ChatMessage> history = chatHistoryService.getHistory(sessionId);
        
        // 4. 构建增强 Prompt
        String context = relevantDocs.stream()
            .map(DocumentChunk::getContent)
            .collect(Collectors.joining("\n---\n"));
        
        String systemPrompt = String.format(
            "你是一个智能知识库助手。请基于以下参考资料回答问题。\n" +
            "如果参考资料无法回答问题，请如实说'未在知识库中找到相关信息'，不要编造。\n\n" +
            "参考资料：\n%s", context);
        
        // 5. 流式调用大模型（SSE）
        return ollamaService.streamChat(systemPrompt, question, history);
    }
}

// 前端通过 EventSource 接收流式输出
@GetMapping(value = "/api/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<ServerSentEvent<String>> streamChat(@RequestParam String question,
                                                  @RequestParam String sessionId) {
    return ragService.streamAnswer(question, sessionId)
        .map(content -> ServerSentEvent.<String>builder()
            .data(content)
            .build())
        .onErrorResume(e -> Flux.just(
            ServerSentEvent.<String>builder()
                .event("error")
                .data(e.getMessage())
                .build()));
}
```

---

## 4. 场景题与故障排查

> 💡 考察实际解决问题的能力

### Q9：SpringBoot 项目启动报错 "Failed to configure a DataSource"，怎么排查？

**面试官意图：** 考察启动报错的基本排查能力，以及配置问题的排除思路。

**完美解答：**

**这个错误的根本原因是 SpringBoot 自动配置尝试创建一个 DataSource，但没有找到任何数据库连接配置或者依赖。**

**排查步骤：**

**第一步：检查是否引入了数据库依赖但没有配置**
如果你的 `pom.xml` 里有以下依赖，但没有配置数据库连接信息，SpringBoot 会自动尝试配置连接池：
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<!-- 或 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-jdbc</artifactId>
</dependency>
```
**解决方案**：在 `application.yml` 中配置数据源，或者在启动类上排除自动配置：
```java
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
```

**第二步：确认 application.yml 配置正确**
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/db_name?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: root
    password: your_password
    driver-class-name: com.mysql.cj.jdbc.Driver
```
注意：`driver-class-name` 在 JDBC 4.0 之后可以省略，但如果省略后报错，补上试试。

**第三步：检查 MySQL 服务是否正常运行**
```bash
mysql -u root -p -h localhost -P 3306
```
也可能是连接超时或防火墙端口没开。

**第四步：Maven 依赖冲突**
多个版本的 MySQL 驱动或者连接池驱动冲突：
```bash
mvn dependency:tree -Dincludes=mysql
```

---

### Q10：SpringBoot 应用在线上频繁 OOM，你怎么排查？

**面试官意图：** 考察结合 SpringBoot 和 JVM 的综合问题排查能力。

**完美解答：**

**Step 1：确认 OOM 类型**
查看日志，看是哪类 OOM：
- `Java heap space` → 堆内存不足
- `Metaspace` → 元空间不足（类加载过多）
- `Direct buffer memory` → 直接内存不足（NIO 相关）
- `Unable to create new native thread` → 线程数过多

**Step 2：配置自动 dump**
```yaml
# 已经在 JVM 参数中配置（建议所有线上项目都加）
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/var/log/heap-dump.hprof
-XX:+PrintGCDetails
-XX:+PrintGCDateStamps
```

**Step 3：分析 heap dump**
重启后，用 MAT 打开 `heap-dump.hprof`：
- **Leak Suspects Report** → 看 MAT 自动分析的可疑泄漏点
- **Dominator Tree** → 按保留堆大小排序，看最大的对象是哪些
- **Path to GC Roots** → 找泄漏对象的 GC Root 引用链

**SpringBoot 项目常见的 OOM 原因：**

| 场景 | 根因 | 解决 |
|------|------|------|
| 批量导出 | 一次性查全部数据到内存 | 流式查询、分批处理 |
| 上传大文件 | Tomcat 缓存文件到内存 | 设置 `spring.servlet.multipart.max-request-size` + 临时文件 |
| 日志过多 | 日志队列打满 | AsyncAppender 设置合适队列大小 |
| 无限创建线程 | 线程池未限制 | 使用有界线程池 |
| HTTP 请求未设置超时 | 连接不释放 | RestTemplate 设置 connectTimeout/readTimeout |

---

### Q11：SpringBoot 应用的跨域问题怎么解决？你遇到过什么坑？

**面试官意图：** 考察前后端分离项目的常见问题处理经验。

**完美解答：**

**核心方案：**

```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOriginPatterns("*")   // 允许的前端域名
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true)       // 允许携带 Cookie
            .maxAge(3600);                // 预检请求缓存时间（1 小时）
    }
}
```

**踩过的坑：**

**坑 1：`allowCredentials(true)` 不能和 `allowedOrigins("*")` 同时使用**

浏览器安全限制，当 `allowCredentials` 为 true 时，不能使用通配符 `*`。解决方案是 `allowedOriginPatterns("*")` 或者指定具体域名。

**坑 2：自定义 Filter 的顺序问题**

如果自定义 Filter 在 CorsFilter 之前执行，可能先被其他 Filter 拦截。解决方案是使用 `CorsFilter` 而非 `WebMvcConfigurer`：

```java
@Bean
public CorsFilter corsFilter() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOriginPatterns(List.of("*"));
    config.setAllowedMethods(List.of("*"));
    config.setAllowedHeaders(List.of("*"));
    config.setAllowCredentials(true);
    
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return new CorsFilter(source);
}
```

**坑 3：Spring Security 的跨域配置**

如果使用了 Spring Security，需要额外配置，否则 Security 会在 CorsFilter 之前拦截：

```java
@Configuration
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(Customizer.withDefaults()) // 使用 Spring MVC 的 CORS 配置
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/public/**").permitAll()
                .anyRequest().authenticated());
        return http.build();
    }
}
```

---

## 💎 面试加分金句

1. "SpringBoot 的自动配置用好了是神器，不理解时就是玄学。我踩过很多坑后总结了一个原则：**自动配置出问题时，先去检查类路径上有没有预期之外的 jar**。"
2. "我坚持代码分层规范：Controller 只做参数校验和结果封装，Service 只做业务逻辑，DAO 只做数据访问。**每一层都只做一件事**，这是代码可维护性的基石。"
3. "RAG 知识库问答系统让我意识到，Java 在后端 AI 应用中的优势在于**成熟的生态和工程化能力**——SpringBoot 管理依赖、事务、缓存，ML 模型管理可以交给 Python 服务。"
4. "做文件上传系统时，一定要记住**永远不要信任前端传的文件信息**——文件类型、文件大小、所有信息都必须在后端重新校验。"
5. "对于一个 SpringBoot 项目，我第一个加的依赖永远是 Knife4j，第二个永远是配置全局异常处理。**API 可视化和规范化的异常处理，能省掉前后端联调 90% 的沟通成本。**"

---

## 📋 高频追问清单

| 追问方向 | 应对策略 |
|----------|----------|
| "SpringBoot 怎么打 war 包？" | 修改 pom.xml `<packaging>war</packaging>`，启动类继承 `SpringBootServletInitializer` |
| "Actuator 暴露了哪些端点？" | health、info、metrics、env、beans、threaddump、heapdump，注意生产环境只暴露 health 和 info |
| "怎么替换内嵌 Tomcat？" | 排除 `spring-boot-starter-tomcat`，引入 `spring-boot-starter-undertow`（性能更高） |
| "@SpringBootTest 怎么用？" | 加载完整上下文进行集成测试，配合 `@TestConfiguration` 覆盖特定 Bean |
| "怎么实现多环境配置？" | application-dev/prod/test.yml，通过 `spring.profiles.active` 激活 |

---

## 🔗 关联知识点

- [Spring必做项目清单-面试问答](Spring必做项目清单-面试问答.md) — IoC/AOP/事务原理
- [SpringMVC必做项目清单-面试问答](SpringMVC必做项目清单-面试问答.md) — MVC 架构与请求处理
- [Java高并发必做项目清单-面试问答](Java高并发必做项目清单-面试问答.md) — 高并发与异步处理
- [Java必做项目清单-面试问答](Java必做项目清单-面试问答.md) — 综合项目面试问答
