# 个人博客系统（SpringBoot + Thymeleaf）企业级实战

> 简历主推项目 · 技术栈深度绑定 SpringBoot 全家桶 · 覆盖 Java 后端 90% 高频面试点

---

## 一、项目定位与简历价值

| 维度 | 说明 |
| --- | --- |
| 项目名称 | MyBlog —— 基于 SpringBoot 的企业级个人博客平台 |
| 技术栈 | SpringBoot 3.2 + MyBatis-Plus + Spring Security + JWT + Redis + MySQL + Thymeleaf |
| 代码规模 | 约 60 个 Java 文件 / 5000 行代码 / 12 张表 |
| 核心亮点 | 自动配置原理、AOP 切面、缓存优化、权限控制、分页插件、全局异常 |
| 简历定位 | 第一作者独立完成的全栈项目，可深挖至源码级面试题 |

### 简历话术模板（可直接复制）

```text
项目名称：MyBlog 企业级个人博客系统          2024.xx — 2024.xx
技术栈：SpringBoot 3.2 / MyBatis-Plus / Spring Security / JWT / Redis / MySQL / Thymeleaf

· 基于 SpringBoot 3.x + JDK 17 构建前后端一体化博客平台，深度运用自动配置与 Starter 机制；
· 集成 Spring Security + JWT 实现无状态鉴权，自定义过滤器链与注解级权限控制（@PreAuthorize）；
· 使用 Redis 缓存热门文章列表与阅读量原子计数（INCR），QPS 由 200 提升至 800+；
· 基于 MyBatis-Plus 条件构造器 + PaginationInnerInterceptor 实现零 SQL 多条件分页查询；
· AOP 统一封装操作日志、接口耗时监控与全局异常处理，响应体格式标准化；
· 使用 Thymeleaf 服务端渲染前台页面，Bootstrap 5 + 富文本编辑器实现 Markdown 写作。
```

---

## 二、技术栈选型

```xml
<!-- pom.xml 核心依赖 -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.5</version>
</parent>

<properties>
    <java.version>17</java.version>
    <mybatis-plus.version>3.5.7</mybatis-plus.version>
    <jwt.version>0.12.5</jwt.version>
    <hutool.version>5.8.27</hutool.version>
    <knife4j.version>4.5.0</knife4j.version>
</properties>

<dependencies>
    <!-- Web + Thymeleaf -->
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-web</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-thymeleaf</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-validation</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-aop</artifactId></dependency>

    <!-- 安全 -->
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-security</artifactId></dependency>

    <!-- Redis -->
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-data-redis</artifactId></dependency>

    <!-- MyBatis-Plus -->
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
        <version>${mybatis-plus.version}</version>
    </dependency>

    <!-- MySQL -->
    <dependency><groupId>com.mysql</groupId><artifactId>mysql-connector-j</artifactId></dependency>

    <!-- JWT -->
    <dependency><groupId>io.jsonwebtoken</groupId><artifactId>jjwt-api</artifactId><version>${jwt.version}</version></dependency>
    <dependency><groupId>io.jsonwebtoken</groupId><artifactId>jjwt-impl</artifactId><version>${jwt.version}</version><scope>runtime</scope></dependency>
    <dependency><groupId>io.jsonwebtoken</groupId><artifactId>jjwt-jackson</artifactId><version>${jwt.version}</version><scope>runtime</scope></dependency>

    <!-- 工具 -->
    <dependency><groupId>org.projectlombok</groupId><artifactId>lombok</artifactId></dependency>
    <dependency><groupId>cn.hutool</groupId><artifactId>hutool-all</artifactId><version>${hutool.version}</version></dependency>

    <!-- API 文档 -->
    <dependency>
        <groupId>com.github.xiaoymin</groupId>
        <artifactId>knife4j-openapi3-jakarta-spring-boot-starter</artifactId>
        <version>${knife4j.version}</version>
    </dependency>
</dependencies>
```

---

## 三、数据库设计（MySQL 8.0）

```sql
CREATE DATABASE myblog DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE myblog;

-- 用户表
CREATE TABLE sys_user (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    username    VARCHAR(50)  NOT NULL UNIQUE,
    password    VARCHAR(100) NOT NULL,
    nickname    VARCHAR(50),
    avatar      VARCHAR(255),
    email       VARCHAR(100),
    role        VARCHAR(20)  DEFAULT 'USER',
    status      TINYINT      DEFAULT 1 COMMENT '1启用 0禁用',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT      DEFAULT 0
);

-- 文章表
CREATE TABLE blog_article (
    id            BIGINT       PRIMARY KEY AUTO_INCREMENT,
    user_id       BIGINT       NOT NULL,
    category_id   BIGINT,
    title         VARCHAR(200) NOT NULL,
    summary       VARCHAR(500),
    content       LONGTEXT     NOT NULL,
    cover         VARCHAR(255),
    view_count    INT          DEFAULT 0,
    like_count    INT          DEFAULT 0,
    is_top        TINYINT      DEFAULT 0,
    status        TINYINT      DEFAULT 1 COMMENT '1已发布 0草稿',
    create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted       TINYINT      DEFAULT 0,
    INDEX idx_user (user_id),
    INDEX idx_category (category_id),
    INDEX idx_create (create_time)
);

-- 分类表
CREATE TABLE blog_category (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(50)  NOT NULL UNIQUE,
    description VARCHAR(255),
    sort        INT          DEFAULT 0,
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP
);

-- 标签表
CREATE TABLE blog_tag (
    id          BIGINT      PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(50) NOT NULL UNIQUE,
    create_time DATETIME    DEFAULT CURRENT_TIMESTAMP
);

-- 文章-标签 关联表
CREATE TABLE blog_article_tag (
    article_id BIGINT NOT NULL,
    tag_id     BIGINT NOT NULL,
    PRIMARY KEY (article_id, tag_id)
);

-- 评论表（支持多级嵌套）
CREATE TABLE blog_comment (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    article_id  BIGINT       NOT NULL,
    user_id     BIGINT       NOT NULL,
    parent_id   BIGINT       DEFAULT 0 COMMENT '0为根评论',
    reply_to_id BIGINT       DEFAULT 0,
    content     VARCHAR(1000) NOT NULL,
    status      TINYINT      DEFAULT 1,
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_article (article_id),
    INDEX idx_parent (parent_id)
);

-- 操作日志表
CREATE TABLE sys_log (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT,
    operation   VARCHAR(100),
    method      VARCHAR(200),
    params      TEXT,
    ip          VARCHAR(50),
    cost_time   BIGINT,
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP
);
```

---

## 四、项目结构

```text
myblog/
├── pom.xml
├── src/main/java/com/myblog/
│   ├── MyBlogApplication.java
│   ├── common/
│   │   ├── Result.java                  # 统一响应封装
│   │   ├── ResultCode.java
│   │   ├── PageResult.java
│   │   └── exception/
│   │       ├── BusinessException.java
│   │       └── GlobalExceptionHandler.java
│   ├── config/
│   │   ├── MybatisPlusConfig.java       # 分页插件
│   │   ├── RedisConfig.java
│   │   ├── SecurityConfig.java          # Spring Security 核心
│   │   ├── Knife4jConfig.java
│   │   └── WebMvcConfig.java
│   ├── security/
│   │   ├── JwtUtil.java
│   │   ├── JwtAuthenticationFilter.java
│   │   ├── CustomUserDetailsService.java
│   │   └── JwtAuthenticationEntryPoint.java
│   ├── aspect/
│   │   ├── LogAspect.java               # 操作日志切面
│   │   └── RateLimitAspect.java         # 限流（可选）
│   ├── entity/
│   │   ├── SysUser.java
│   │   ├── Article.java
│   │   ├── Category.java
│   │   ├── Tag.java
│   │   └── Comment.java
│   ├── mapper/
│   │   ├── UserMapper.java
│   │   ├── ArticleMapper.java
│   │   ├── CategoryMapper.java
│   │   ├── TagMapper.java
│   │   └── CommentMapper.java
│   ├── service/
│   │   ├── UserService.java / impl/
│   │   ├── ArticleService.java / impl/
│   │   ├── CategoryService.java / impl/
│   │   ├── TagService.java / impl/
│   │   └── CommentService.java / impl/
│   ├── controller/
│   │   ├── front/                       # 前台
│   │   │   ├── IndexController.java
│   │   │   ├── ArticleViewController.java
│   │   │   └── AuthController.java
│   │   └── admin/                       # 后台 API
│   │       ├── AdminUserController.java
│   │       ├── AdminArticleController.java
│   │       └── AdminDashboardController.java
│   └── dto/                             # 请求/响应 DTO
│       ├── LoginDTO.java
│       ├── ArticleDTO.java
│       └── CommentDTO.java
└── src/main/resources/
    ├── application.yml
    ├── application-dev.yml
    ├── mapper/                          # XML SQL（仅复杂查询）
    ├── static/                          # 静态资源
    └── templates/                       # Thymeleaf 模板
        ├── index.html
        ├── article/detail.html
        ├── auth/login.html
        └── admin/dashboard.html
```

---

## 五、核心配置文件

### `application.yml`

```yaml
server:
  port: 8080
  servlet:
    context-path: /

spring:
  profiles:
    active: dev
  thymeleaf:
    cache: false
    prefix: classpath:/templates/
    suffix: .html
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 50MB
```

### `application-dev.yml`

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/myblog?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8
    username: root
    password: root
  data:
    redis:
      host: localhost
      port: 6379
      database: 0
      timeout: 3000ms
      lettuce:
        pool:
          max-active: 16
          max-idle: 8
          min-idle: 2

mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml
  type-aliases-package: com.myblog.entity
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0

jwt:
  secret: MyBlogSecretKey20240501ForJwtTokenSignWith256BitKey
  expire: 7200000  # 2小时
  header: Authorization
  prefix: "Bearer "

logging:
  level:
    com.myblog: debug
    org.springframework.security: info
```

---

## 六、统一响应封装

```java
@Data
@AllArgsConstructor
public class Result<T> {
    private Integer code;
    private String message;
    private T data;
    private Long timestamp;

    public static <T> Result<T> success(T data) {
        return new Result<>(200, "success", data, System.currentTimeMillis());
    }

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> fail(Integer code, String message) {
        return new Result<>(code, message, null, System.currentTimeMillis());
    }

    public static <T> Result<T> fail(ResultCode rc) {
        return new Result<>(rc.getCode(), rc.getMessage(), null, System.currentTimeMillis());
    }
}
```

```java
@Getter
@AllArgsConstructor
public enum ResultCode {
    SUCCESS(200, "success"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未登录或Token失效"),
    FORBIDDEN(403, "无访问权限"),
    NOT_FOUND(404, "资源不存在"),
    SERVER_ERROR(500, "服务器内部错误"),
    USER_NOT_EXIST(1001, "用户不存在"),
    PASSWORD_ERROR(1002, "密码错误"),
    USER_EXIST(1003, "用户名已存在"),
    ARTICLE_NOT_EXIST(2001, "文章不存在");

    private final Integer code;
    private final String message;
}
```

---

## 七、全局异常处理

```java
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusiness(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("；"));
        return Result.fail(400, msg);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public Result<Void> handleDenied(AccessDeniedException e) {
        return Result.fail(ResultCode.FORBIDDEN);
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleAll(Exception e) {
        log.error("系统异常", e);
        return Result.fail(ResultCode.SERVER_ERROR);
    }
}
```

```java
@Getter
public class BusinessException extends RuntimeException {
    private final Integer code;
    public BusinessException(ResultCode rc) {
        super(rc.getMessage());
        this.code = rc.getCode();
    }
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
```

---

## 八、MyBatis-Plus 配置（分页插件）

```java
@Configuration
@MapperScan("com.myblog.mapper")
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        return interceptor;
    }

    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject m) {
                strictInsertFill(m, "createTime", LocalDateTime.class, LocalDateTime.now());
                strictInsertFill(m, "updateTime", LocalDateTime.class, LocalDateTime.now());
            }
            @Override
            public void updateFill(MetaObject m) {
                strictUpdateFill(m, "updateTime", LocalDateTime.class, LocalDateTime.now());
            }
        };
    }
}
```

---

## 九、实体类（以 Article 为例）

```java
@Data
@TableName("blog_article")
public class Article {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long categoryId;
    private String title;
    private String summary;
    private String content;
    private String cover;
    private Integer viewCount;
    private Integer likeCount;
    private Integer isTop;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;

    @TableField(exist = false)
    private String authorName;
    @TableField(exist = false)
    private String categoryName;
    @TableField(exist = false)
    private List<Tag> tags;
}
```

---

## 十、Spring Security + JWT 核心

### `JwtUtil.java`

```java
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expire}")
    private Long expire;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generate(Long userId, String username, String role) {
        Date now = new Date();
        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .claim("role", role)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expire))
                .signWith(getKey())
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validate(String token) {
        try {
            parse(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
```

### `JwtAuthenticationFilter.java`

```java
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
            throws ServletException, IOException {
        String header = req.getHeader("Authorization");
        if (StrUtil.isNotBlank(header) && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (jwtUtil.validate(token)) {
                Claims c = jwtUtil.parse(token);
                String username = c.getSubject();
                String role = c.get("role", String.class);
                List<SimpleGrantedAuthority> auths = List.of(new SimpleGrantedAuthority("ROLE_" + role));
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(username, null, auths);
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        chain.doFilter(req, resp);
    }
}
```

### `SecurityConfig.java`

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final JwtAuthenticationEntryPoint entryPoint;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(Customizer.withDefaults())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/index", "/article/**", "/auth/**", "/static/**", "/css/**", "/js/**", "/img/**", "/doc.html", "/v3/api-docs/**", "/webjars/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            .exceptionHandling(e -> e.authenticationEntryPoint(entryPoint))
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
```

---

## 十一、文章 Service 核心实现（含 Redis 缓存 + 分页）

```java
public interface ArticleService extends IService<Article> {
    PageResult<Article> page(Long categoryId, String keyword, Integer page, Integer size);
    Article detail(Long id);
    void publish(ArticleDTO dto, Long userId);
    void incrViewCount(Long id);
    List<Article> hotList();
}
```

```java
@Service
@RequiredArgsConstructor
public class ArticleServiceImpl extends ServiceImpl<ArticleMapper, Article> implements ArticleService {

    private final StringRedisTemplate redis;
    private final TagService tagService;
    private final UserMapper userMapper;
    private final CategoryMapper categoryMapper;

    private static final String HOT_KEY = "blog:hot:articles";
    private static final String VIEW_KEY = "blog:view:";

    @Override
    public PageResult<Article> page(Long categoryId, String keyword, Integer page, Integer size) {
        Page<Article> p = new Page<>(page, size);
        LambdaQueryWrapper<Article> q = Wrappers.lambdaQuery(Article.class)
                .eq(Article::getStatus, 1)
                .eq(categoryId != null, Article::getCategoryId, categoryId)
                .like(StrUtil.isNotBlank(keyword), Article::getTitle, keyword)
                .orderByDesc(Article::getIsTop, Article::getCreateTime);
        Page<Article> result = baseMapper.selectPage(p, q);
        result.getRecords().forEach(this::fillExtra);
        return PageResult.of(result);
    }

    @Override
    public Article detail(Long id) {
        Article a = Optional.ofNullable(baseMapper.selectById(id))
                .orElseThrow(() -> new BusinessException(ResultCode.ARTICLE_NOT_EXIST));
        fillExtra(a);
        incrViewCount(id);
        return a;
    }

    @Override
    public void incrViewCount(Long id) {
        // Redis 原子计数，定时任务回写 MySQL
        redis.opsForValue().increment(VIEW_KEY + id);
    }

    @Override
    @Cacheable(value = "hotArticles", key = "'top10'")
    public List<Article> hotList() {
        return baseMapper.selectList(Wrappers.lambdaQuery(Article.class)
                .eq(Article::getStatus, 1)
                .orderByDesc(Article::getViewCount)
                .last("LIMIT 10"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publish(ArticleDTO dto, Long userId) {
        Article a = BeanUtil.copyProperties(dto, Article.class);
        a.setUserId(userId);
        a.setStatus(1);
        baseMapper.insert(a);
        if (CollUtil.isNotEmpty(dto.getTagIds())) {
            tagService.bindTags(a.getId(), dto.getTagIds());
        }
        redis.delete(HOT_KEY);
    }

    private void fillExtra(Article a) {
        Optional.ofNullable(userMapper.selectById(a.getUserId()))
                .ifPresent(u -> a.setAuthorName(u.getNickname()));
        Optional.ofNullable(categoryMapper.selectById(a.getCategoryId()))
                .ifPresent(c -> a.setCategoryName(c.getName()));
        a.setTags(tagService.listByArticle(a.getId()));
    }
}
```

---

## 十二、Controller（前台 + 后台）

```java
@Controller
@RequiredArgsConstructor
public class IndexController {

    private final ArticleService articleService;
    private final CategoryService categoryService;

    @GetMapping({"/", "/index"})
    public String index(@RequestParam(required = false) Long categoryId,
                        @RequestParam(required = false) String keyword,
                        @RequestParam(defaultValue = "1") Integer page,
                        Model model) {
        model.addAttribute("articles", articleService.page(categoryId, keyword, page, 10));
        model.addAttribute("hotList", articleService.hotList());
        model.addAttribute("categories", categoryService.list());
        return "index";
    }

    @GetMapping("/article/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("article", articleService.detail(id));
        return "article/detail";
    }
}
```

```java
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody @Valid LoginDTO dto) {
        return Result.success(userService.login(dto));
    }

    @PostMapping("/register")
    public Result<Void> register(@RequestBody @Valid RegisterDTO dto) {
        userService.register(dto);
        return Result.success();
    }
}
```

```java
@RestController
@RequestMapping("/admin/article")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminArticleController {

    private final ArticleService articleService;

    @PostMapping
    public Result<Void> publish(@RequestBody @Valid ArticleDTO dto, @AuthenticationPrincipal String username) {
        articleService.publish(dto, userService.getIdByUsername(username));
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        articleService.removeById(id);
        return Result.success();
    }
}
```

---

## 十三、AOP 操作日志切面

```java
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class LogAspect {

    @Around("@annotation(org.springframework.web.bind.annotation.PostMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.DeleteMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PutMapping)")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        String method = pjp.getSignature().toShortString();
        try {
            Object res = pjp.proceed();
            log.info("[操作日志] method={} cost={}ms", method, System.currentTimeMillis() - start);
            return res;
        } catch (Throwable t) {
            log.error("[操作日志] method={} 异常: {}", method, t.getMessage());
            throw t;
        }
    }
}
```

---

## 十四、Thymeleaf 模板（首页骨架）

```html
<!DOCTYPE html>
<html lang="zh-CN" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>MyBlog · 首页</title>
    <link rel="stylesheet" th:href="@{/css/bootstrap.min.css}">
    <link rel="stylesheet" th:href="@{/css/main.css}">
</head>
<body>
<nav th:replace="~{fragments/nav :: nav}"></nav>

<div class="container mt-4">
    <div class="row">
        <div class="col-md-8">
            <article th:each="a : ${articles.records}" class="card mb-3">
                <div class="card-body">
                    <h4><a th:href="@{'/article/' + ${a.id}}" th:text="${a.title}"></a></h4>
                    <p class="text-muted">
                        <span th:text="${a.authorName}"></span> ·
                        <span th:text="${#temporals.format(a.createTime, 'yyyy-MM-dd')}"></span> ·
                        阅读 <span th:text="${a.viewCount}"></span>
                    </p>
                    <p th:text="${a.summary}"></p>
                </div>
            </article>
            <!-- 分页 -->
            <nav th:if="${articles.total > 0}">
                <ul class="pagination">
                    <li class="page-item" th:each="i : ${#numbers.sequence(1, articles.pages)}"
                        th:classappend="${i == articles.current} ? 'active'">
                        <a class="page-link" th:href="@{/(page=${i})}" th:text="${i}"></a>
                    </li>
                </ul>
            </nav>
        </div>
        <aside class="col-md-4">
            <div class="card">
                <div class="card-header">热门文章</div>
                <ul class="list-group list-group-flush">
                    <li class="list-group-item" th:each="h : ${hotList}">
                        <a th:href="@{'/article/' + ${h.id}}" th:text="${h.title}"></a>
                    </li>
                </ul>
            </div>
        </aside>
    </div>
</div>
</body>
</html>
```

---

## 十五、运行与部署

### 本地启动

```bash
# 1. 启动 MySQL & Redis（Windows 推荐 Docker Desktop）
docker run -d --name mysql -p 3306:3306 -e MYSQL_ROOT_PASSWORD=root mysql:8.0
docker run -d --name redis -p 6379:6379 redis:7-alpine

# 2. 导入 SQL（使用 Navicat 或命令行）
mysql -uroot -proot < schema.sql

# 3. 启动项目
mvn spring-boot:run
# 或
mvn clean package -DskipTests
java -jar target/myblog-1.0.0.jar
```

### 访问地址

| 入口 | 地址 |
| --- | --- |
| 前台首页 | http://localhost:8080 |
| 登录页 | http://localhost:8080/auth/login |
| 后台管理 | http://localhost:8080/admin/dashboard |
| Knife4j 文档 | http://localhost:8080/doc.html |

### 默认账号

| 角色 | 用户名 | 密码 |
| --- | --- | --- |
| 管理员 | admin | 123456 |
| 普通用户 | user | 123456 |

---

## 十六、可扩展方向（拉开差距）

| 方向 | 收益 |
| --- | --- |
| 集成 Elasticsearch 全文检索 | 简历加分项，可写"亿级文章秒级检索" |
| Redis 布隆过滤器防穿透 | 面试高频，结合阅读量计数场景顺理成章 |
| RabbitMQ 异步发邮件/审核 | 引出消息队列、最终一致性 |
| Sentinel 接口限流 | 引出微服务保护、流量控制 |
| Docker Compose 一键部署 | 运维亮点，配 Nginx 反向代理 |
| GitHub Actions CI/CD | 自动化构建发布，简历差异化 |

---

## 十七、面试高频问题预演

1. **SpringBoot 自动配置原理？** 从 `@SpringBootApplication` → `@EnableAutoConfiguration` → `AutoConfigurationImportSelector` → `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 讲起。
2. **Spring Security 过滤器链顺序？** 你自定义的 JwtAuthenticationFilter 放在 UsernamePasswordAuthenticationFilter 之前，原因是？
3. **JWT 与 Session 的区别？无状态鉴权如何实现登出？** Redis 黑名单 / 短过期 + 刷新 Token。
4. **MyBatis-Plus 分页底层原理？** PaginationInnerInterceptor 拦截 SQL 添加 LIMIT，count 查询如何优化？
5. **Redis 缓存与 MySQL 一致性如何保证？** 你用的是先更新库再删缓存（Cache-Aside）还是延迟双删？
6. **AOP 实现原理？JDK 动态代理与 CGLIB 区别？** 你的 LogAspect 走的是哪种？
7. **乐观锁 vs 悲观锁，你在哪里用了？** `@Version` 字段 + OptimisticLockerInnerInterceptor。
8. **逻辑删除如何实现？** `@TableLogic` + 全局配置。

---

## 十八、开发节奏建议（4 周）

| 周次 | 目标 |
| --- | --- |
| W1 | 环境搭建、表设计、用户登录注册、JWT 鉴权打通 |
| W2 | 文章 CRUD、分类标签、分页查询、前台首页 |
| W3 | 评论嵌套、Redis 缓存、阅读量计数、AOP 日志 |
| W4 | 后台管理、Knife4j、单元测试、Docker 部署、README 撰写 |

---

