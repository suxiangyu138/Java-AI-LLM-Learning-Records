# 07-Spring Security认证与授权
> 🎯 Spring Security是Java生态的事实标准安全框架 — 掌握认证授权体系、Filter Chain架构、JWT无状态认证、OAuth2集成，是后端开发P1就业必备的安全防线技能

---

## 目录
1. [本章总览](#1-本章总览)
2. [认证与授权理论体系](#2-认证与授权理论体系)
3. [Spring Security Filter Chain架构](#3-spring-security-filter-chain架构)
4. [PasswordEncoder密码编码](#4-passwordencoder密码编码)
5. [Security配置方式](#5-security配置方式)
6. [JWT令牌机制](#6-jwt令牌机制)
7. [OAuth2协议族](#7-oauth2协议族)
8. [方法级安全控制](#8-方法级安全控制)
9. [高频踩坑与误区](#9-高频踩坑与误区)
10. [随堂基础练习](#10-随堂基础练习)
11. [章节综合实操案例](#11-章节综合实操案例)
12. [分层综合习题](#12-分层综合习题)
13. [本章复盘速记清单](#13-本章复盘速记清单)
14. [精通拓展补充-P2](#14-精通拓展补充-p2)

---

## 1. 本章总览

### 1.1 知识定位

- **归属**：Spring全家桶核心组件 → 层级2 P1就业必备
- **前置依赖**：Spring MVC + Spring Boot + Servlet基础（Filter/Interceptor概念）
- **难度等级**：⭐⭐⭐⭐（框架深度高，涉及架构设计思维）
- **重要性**：⭐⭐⭐⭐⭐（任何Web应用必须考虑的安全防护）

### 1.2 为什么需要Spring Security

> 💡 安全不是功能，是基础设施。直接写在业务代码中的权限判断会导致代码耦合、难以维护、容易遗漏漏洞。

| 自行实现的问题 | Spring Security的优势 |
|---------------|---------------------|
| 过滤器链手工维护，容易遗漏路径 | 声明式安全配置，自动注册Filter Chain |
| 密码明文存储或简单Hash | 内置BCrypt/Argon2/scrypt等安全编码器 |
| Session固定攻击、CSRF等需自行防御 | 开箱即用的安全攻击防护 |
| 认证逻辑与业务代码耦合 | 通过SecurityContextHolder解耦 |
| RBAC权限模型代码分散 | 注解式方法级别控制 (@PreAuthorize) |
| 集成OAuth2需要大量胶水代码 | Spring Security OAuth2 模块原生支持 |

### 1.3 三层学习目标

| 级别 | 目标 |
|------|------|
| **基础** | 理解认证与授权概念，能配置SecurityFilterChain实现表单登录和HTTP Basic认证，使用BCryptPasswordEncoder |
| **熟练** | 掌握JWT令牌生成与验证，实现无状态RESTful API认证，配置方法级安全注解 |
| **精通** | 深入理解Filter Chain源码流程，能自定义AuthenticationProvider，集成OAuth2 Authorization Server，设计零信任安全架构 |

---

## 2. 认证与授权理论体系

### 2.1 核心概念

| 概念 | 英文 | 定义 | 比喻 |
|------|------|------|------|
| **认证** | Authentication | 验证"你是谁" | 出示身份证进大门 |
| **授权** | Authorization | 你能"做什么" | 身份证验证后，判定能否进某个房间 |
| **凭证** | Credential | 身份凭据（密码/令牌/证书） | 身份证件本身 |
| **主体** | Principal | 已认证的身份信息 | 验证通过后的"你" |
| **角色** | Role | 权限的集合抽象 | 管理员、普通用户、VIP |
| **权限** | Permission | 具体的操作许可 | 读、写、删除、审批 |

> 🎯 **认证解决身份问题，授权解决权限问题。认证在前，授权在后。**

### 2.2 RBAC权限模型

RBAC（Role-Based Access Control）是业界最广泛使用的权限模型。

```
用户(User) ──多对多──> 角色(Role) ──多对多──> 权限(Permission)
```

**数据表设计**：

```sql
-- 用户表
CREATE TABLE sys_user (
    id       BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50)  NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    enabled  TINYINT(1)   DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 角色表
CREATE TABLE sys_role (
    id   BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50)  NOT NULL UNIQUE,   -- ROLE_ADMIN, ROLE_USER
    label VARCHAR(50) NOT NULL           -- 管理员, 普通用户
);

-- 权限表
CREATE TABLE sys_permission (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL,   -- sys:user:create
    label       VARCHAR(100) NOT NULL,   -- 创建用户
    resource    VARCHAR(100),            -- /api/users/**
    action      VARCHAR(20)              -- POST
);

-- 用户-角色关联
CREATE TABLE sys_user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id)
);

-- 角色-权限关联
CREATE TABLE sys_role_permission (
    role_id       BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id)
);
```

**Spring Security中的RBAC表达**：

```java
// 角色判断（需 ROLE_ 前缀）
hasRole("ADMIN")            → 需要 ROLE_ADMIN 角色
hasAnyRole("ADMIN","USER")  → 需要 ROLE_ADMIN 或 ROLE_USER

// 权限判断（无需前缀）
hasAuthority("sys:user:create")  → 需要 sys:user:create 权限
hasAnyAuthority("sys:user:create", "sys:user:update")
```

### 2.3 常见认证方式对比

| 认证方式 | 状态存储 | 适用场景 | 优点 | 缺点 |
|---------|---------|---------|------|------|
| HTTP Basic | 无状态 | 内部API、测试环境 | 实现简单 | 明文传输密码（需HTTPS） |
| Session-Cookie | 服务端有状态 | 传统MVC应用 | 成熟稳定 | 集群需共享Session |
| JWT令牌 | 客户端无状态 | RESTful API、前后端分离 | 无状态、跨域友好 | 令牌不可撤销 |
| OAuth2 | 授权服务器管理 | 第三方登录、开放API | 授权与认证解耦 | 架构复杂 |
| SSO | 统一认证中心 | 多系统统一登录 | 一次登录访问所有系统 | 单点故障风险 |

---

## 3. Spring Security Filter Chain架构

### 3.1 核心体系架构

```
HTTP Request
    │
    ▼
 DelegatingFilterProxy (web.xml中注册，委托给Spring容器的Filter)
    │
    ▼
 FilterChainProxy (Spring Security的入口，管理多个SecurityFilterChain)
    │
    ├── SecurityFilterChain #1 (匹配 /api/**)
    │   ├── SecurityContextPersistenceFilter
    │   ├── LogoutFilter
    │   ├── UsernamePasswordAuthenticationFilter
    │   ├── ... (根据配置动态增减)
    │   └── FilterSecurityInterceptor
    │
    ├── SecurityFilterChain #2 (匹配 /admin/**)
    │   └── ... (另一套过滤规则)
    │
    └── SecurityFilterChain #3 (anyRequest)
        └── ... (兜底过滤规则)
    │
    ▼
 DispatcherServlet (Spring MVC)
```

### 3.2 关键过滤器详解

| 过滤器 | 类名 | 职责 | 默认顺序 |
|--------|------|------|---------|
| 安全上下文 | `SecurityContextPersistenceFilter` | 请求开始从Session恢复SecurityContext，请求结束存入Session | 1 |
| 登出 | `LogoutFilter` | 处理/logout请求，清除认证信息和Session | 2 |
| 用户名密码认证 | `UsernamePasswordAuthenticationFilter` | 拦截POST /login，解析表单参数进行认证 | 3 |
| Session管理 | `SessionManagementFilter` | 检测Session固定攻击，控制并发登录 | 4 |
| 异常处理 | `ExceptionTranslationFilter` | 捕获AuthenticationException和AccessDeniedException | 5 |
| 匿名认证 | `AnonymousAuthenticationFilter` | 未登录用户自动赋予匿名身份 | 6 |
| 权限裁决 | `FilterSecurityInterceptor` | 最终的权限校验，调用AccessDecisionManager | 7 |
| 基本认证 | `BasicAuthenticationFilter` | HTTP Basic认证 | 8 |
| 记住我 | `RememberMeAuthenticationFilter` | Cookie记住登录态 | 9 |
| JWT (自定义) | 自定义 `OncePerRequestFilter` | 解析JWT令牌，设置SecurityContext | 自定义 |

### 3.3 认证核心流程（UsernamePasswordAuthenticationFilter）

```
POST /login  (username + password)
    │
    ▼
UsernamePasswordAuthenticationFilter.attemptAuthentication()
    │ 创建 UsernamePasswordAuthenticationToken（未认证）
    ▼
ProviderManager.authenticate()
    │ 遍历持有的 AuthenticationProvider 列表
    ▼
DaoAuthenticationProvider  (最常用的实现)
    │ 调用 UserDetailsService.loadUserByUsername()
    ▼
UserDetailsService (自定义实现，从DB加载用户)
    │ 返回 UserDetails（用户名、密码密文、权限集合）
    ▼
DaoAuthenticationProvider
    │ 使用 PasswordEncoder.matches() 比对密码
    ▼
认证成功 → 创建 UsernamePasswordAuthenticationToken（已认证）
    │       存入 SecurityContextHolder
    ▼
 认证失败 → 抛出 AuthenticationException
```

### 3.4 SecurityContextHolder

```java
// 获取当前登录用户信息（可在任何层获取）
SecurityContext context = SecurityContextHolder.getContext();
Authentication authentication = context.getAuthentication();

String username = authentication.getName();
Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
Object principal = authentication.getPrincipal();  // 通常是 UserDetails 对象

// ⚠️ 注意：在异步任务中需要手动传递上下文
// 默认策略：MODE_THREADLOCAL（每个线程独立）
```

> 💡 `SecurityContextHolder` 使用 `ThreadLocal` 存储认证信息，因此请求处理链路的任何代码都能通过 `SecurityContextHolder.getContext()` 获取当前用户。**异步处理时需手动传递上下文**，或使用 `@Async` + `SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL)`。

---

## 4. PasswordEncoder密码编码

### 4.1 为什么不能用明文密码

> ⚠️ **明文存储密码是严重的安全漏洞。** 一旦数据库泄露，所有用户的密码直接暴露。即使用户密码被Hash处理，也需要使用加盐（Salt）的慢Hash算法。

| Hash算法 | 安全性 | 说明 |
|---------|--------|------|
| MD5 | ❌ 不安全 | 彩虹表可秒破，需加盐但现代机器仍可暴力破解 |
| SHA-1/SHA-256 | ⚠️ 不安全 | 设计目标是数据完整性校验，非密码存储，速度太快易被爆破 |
| **BCrypt** | ✅ 安全 | 内置随机盐，可调工作因子(强度)，慢Hash |
| **SCrypt** | ✅ 安全 | 内存密集型，抗GPU/ASIC攻击 |
| **Argon2** | ✅ 最安全 | 2015年Password Hashing Competition冠军 |

### 4.2 BCrypt原理

```
BCryptHash(String password, int strength)

1. 生成随机盐（16字节，每次不同）
2. 使用Blowfish加密算法，迭代 2^strength 次
3. 输出格式：$2a$10$salt(22字符).hash(31字符)
   ├── $2a        → BCrypt版本
   ├── $10        → 工作因子（2^10 = 1024轮迭代）
   ├── salt       → 随机盐（Base64编码，22字符）
   └── hash       → 密码哈希结果（Base64编码，31字符）
```

```java
// 工作因子默认10，范围4-31
// 因子每+1，耗时翻倍（10约80ms → 12约320ms）
BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

// 加密：每次调用结果不同（因为随机盐）
String encoded = encoder.encode("myPassword");
// $2a$12$9TqlPJmXBr5V/r4zOO1B6uYbgV6MyP5gx7CGR4sO4y0y0eJnZEvCq

// 校验
boolean matches = encoder.matches("myPassword", encoded);  // true
boolean wrong = encoder.matches("wrongPassword", encoded); // false
```

### 4.3 DelegatingPasswordEncoder

> 💡 Spring Security 5.0+ 默认使用 `DelegatingPasswordEncoder`，支持多种编码格式并存，便于密码升级。

```java
// Spring Boot自动配置的默认PasswordEncoder
@Bean
public PasswordEncoder passwordEncoder() {
    // 默认使用 BCrypt，但支持多种格式
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    // 输出格式：{bcrypt}$2a$10$...
    // 其他支持：{noop}plaintext, {pbkdf2}..., {argon2}...
}

// 解码流程
// 1. 读取 {前缀} 识别编码类型
// 2. 委托对应 PasswordEncoder 校验
// 3. 不同编码格式可共存，便于密码升级迁移
```

### 4.4 密码升级迁移策略

```java
// 方案：登录时检测密码编码，自动升级
@Service
public class UserService {
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private UserRepository userRepository;

    public boolean login(String username, String rawPassword) {
        User user = userRepository.findByUsername(username);
        if (passwordEncoder.matches(rawPassword, user.getPassword())) {
            // 如果当前密码不是BCrypt（是旧格式），自动升级
            if (passwordEncoder.upgradeEncoding(user.getPassword())) {
                user.setPassword(passwordEncoder.encode(rawPassword));
                userRepository.save(user);
            }
            return true;
        }
        return false;
    }
}
```

---

## 5. Security配置方式

### 5.1 基础配置 — SecurityFilterChain Bean

Spring Security 5.7+ 使用 Lambda DSL + SecurityFilterChain Bean 配置（替代已 deprecated 的 `WebSecurityConfigurerAdapter`）：

```java
@Configuration
@EnableWebSecurity  // 开启Security（Spring Boot中可省略）
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 授权配置
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/api/public/**").permitAll()       // 公开接口
                .requestMatchers("/api/admin/**").hasRole("ADMIN")   // 仅管理员
                .requestMatchers("/api/user/**").hasAnyRole("USER", "ADMIN")
                .anyRequest().authenticated()                       // 其他需认证
            )
            // 表单登录
            .formLogin(form -> form
                .loginPage("/login")         // 自定义登录页
                .loginProcessingUrl("/doLogin")
                .defaultSuccessUrl("/index")
                .failureUrl("/login?error")
                .permitAll()
            )
            // 登出
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .clearAuthentication(true)
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
            )
            // 禁用CSRF（RESTful API建议关闭）
            .csrf(csrf -> csrf.disable())
            // Session管理
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .maximumSessions(1)
                .expiredUrl("/login?expired")
            );

        return http.build();
    }
}
```

### 5.2 HttpSecurity DSL配置选项

| DSL方法 | 作用 | 常用子配置 |
|---------|------|-----------|
| `authorizeHttpRequests()` | URL授权规则 | `permitAll`, `authenticated`, `hasRole`, `hasAuthority`, `denyAll` |
| `formLogin()` | 表单登录 | `loginPage`, `successHandler`, `failureHandler` |
| `httpBasic()` | HTTP Basic认证 | `realmName` |
| `csrf()` | CSRF防护 | `disable`, `ignoringRequestMatchers` |
| `cors()` | CORS跨域配置 | `configurationSource` |
| `sessionManagement()` | Session策略 | `sessionCreationPolicy`, `maximumSessions` |
| `rememberMe()` | 记住我功能 | `key`, `tokenValiditySeconds`, `userDetailsService` |
| `exceptionHandling()` | 异常处理 | `authenticationEntryPoint`, `accessDeniedHandler` |
| `logout()` | 登出配置 | `logoutUrl`, `logoutSuccessHandler` |
| `oauth2Login()` | OAuth2登录 | `loginPage`, `authorizationEndpoint`, `tokenEndpoint` |
| `oauth2ResourceServer()` | OAuth2资源服务器 | `jwt()`, `opaqueToken()` |
| `addFilterBefore/After()` | 自定义过滤器位置 | 指定在哪个过滤器之前/之后插入 |

### 5.3 Session策略

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .sessionManagement(session -> session
            // 四种策略
            .sessionCreationPolicy(SessionCreationPolicy.ALWAYS)       // 总是创建Session
            // .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED) // 需要时创建（默认）
            // .sessionCreationPolicy(SessionCreationPolicy.NEVER)       // 不主动创建，但有则不删除
            // .sessionCreationPolicy(SessionCreationPolicy.STATELESS)   // 无状态，不创建不使用Session
        );
    return http.build();
}
```

| SessionCreationPolicy | 说明 | 适用场景 |
|----------------------|------|---------|
| `IF_REQUIRED` | 需要认证时创建Session（默认） | 传统MVC应用 |
| `STATELESS` | 绝不创建或使用Session | RESTful API + JWT |
| `ALWAYS` | 总是创建Session | 某些遗留系统 |
| `NEVER` | 不主动创建，但不销毁已有的 | 特殊场景 |

### 5.4 CORS配置

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .csrf(csrf -> csrf.disable());  // 前端跨域时通常也需要禁用CSRF
    return http.build();
}

@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(Arrays.asList("http://localhost:3000", "https://myapp.com"));
    config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(Arrays.asList("*"));
    config.setAllowCredentials(true);
    config.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
}
```

### 5.5 UserDetailsService 自定义加载

```java
@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("用户不存在: " + username));

        // 构建权限集合（角色 + 权限）
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().getName()));
        // 粒度更细的权限
        user.getRole().getPermissions().forEach(p ->
            authorities.add(new SimpleGrantedAuthority(p.getName()))
        );

        return new User(
            user.getUsername(),
            user.getPassword(),
            user.getEnabled(),
            true,  // accountNonExpired
            true,  // credentialsNonExpired
            true,  // accountNonLocked
            authorities
        );
    }
}
```

---

## 6. JWT令牌机制

### 6.1 什么是JWT

> 💡 JWT（JSON Web Token）是一种自包含的令牌格式，将用户信息编码在令牌中，服务端无需存储Session即可完成认证。

### 6.2 JWT结构

```
header.payload.signature

eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.
eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWUsImlhdCI6MTUxNjIzOTAyMiwiZXhwIjoxNTE2MjQyNjIyfQ.
SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c
```

| 部分 | 说明 | 示例内容 |
|------|------|---------|
| **Header** | 令牌类型和签名算法 | `{"alg":"HS256","typ":"JWT"}` |
| **Payload** | 声明（Claims），包含用户信息 | `{"sub":"123","name":"John","iat":1516239022,"exp":1516242622}` |
| **Signature** | 防止篡改的签名 | `HMACSHA256(base64UrlEncode(header) + "." + base64UrlEncode(payload), secret)` |

**标准声明（Registered Claims）**：

| 声明 | 全称 | 含义 |
|------|------|------|
| `iss` | Issuer | 签发者 |
| `sub` | Subject | 面向用户 |
| `aud` | Audience | 接收方 |
| `exp` | Expiration Time | 过期时间（时间戳） |
| `nbf` | Not Before | 生效时间 |
| `iat` | Issued At | 签发时间 |
| `jti` | JWT ID | 唯一标识 |

### 6.3 JWT生成与验证

```java
// ========== Maven依赖（推荐使用Nimbus JOSE + JWT，Spring Security自带） ==========
// spring-boot-starter-oauth2-resource-server 已包含 nimbus-jose-jwt

// ========== JWT工具类 ==========
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;           // 密钥（至少256位）
    @Value("${jwt.expiration:3600000}")
    private long expiration;         // 过期时间（默认1小时）
    @Value("${jwt.refresh-expiration:604800000}")
    private long refreshExpiration;  // Refresh Token过期时间（默认7天）

    // 生成Access Token
    public String generateToken(String username, Collection<? extends GrantedAuthority> authorities) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        // 构建Claims
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", username);
        claims.put("roles", authorities.stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toList()));

        return Jwts.builder()
            .setClaims(claims)
            .setSubject(username)
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(SignatureAlgorithm.HS256, secret.getBytes(StandardCharsets.UTF_8))
            .compact();
    }

    // 生成Refresh Token
    public String generateRefreshToken(String username) {
        Date now = new Date();
        return Jwts.builder()
            .setSubject(username)
            .setIssuedAt(now)
            .setExpiration(new Date(now.getTime() + refreshExpiration))
            .signWith(SignatureAlgorithm.HS256, secret.getBytes(StandardCharsets.UTF_8))
            .compact();
    }

    // 从令牌中解析用户名
    public String getUsernameFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.getSubject();
    }

    // 验证令牌
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // 解析令牌
    private Claims parseToken(String token) {
        return Jwts.parser()
            .setSigningKey(secret.getBytes(StandardCharsets.UTF_8))
            .parseClaimsJws(token)
            .getBody();
    }
}
```

> ⚠️ **安全性警告**：JWT密钥必须足够强度，HS256需要至少256位（32字节）密钥。生产环境建议使用非对称签名RS256，将私钥保存在认证服务端，公钥交给资源服务器。

### 6.4 JWT认证过滤器

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // 从请求头获取令牌
        String token = extractToken(request);

        if (token != null && jwtUtil.validateToken(token)) {
            String username = jwtUtil.getUsernameFromToken(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            // 构建已认证的Authentication对象
            UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities()
                );
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // 设置到SecurityContext
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    // 提取JWT令牌（从Authorization请求头）
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}

// ========== 注册过滤器到SecurityFilterChain ==========
@Bean
public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .sessionManagement(session -> session
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)  // 无状态
        )
        .authorizeHttpRequests(authz -> authz
            .requestMatchers("/api/auth/**").permitAll()
            .anyRequest().authenticated()
        )
        // 在UsernamePasswordAuthenticationFilter之前插入JWT过滤器
        .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
}
```

### 6.5 Access Token 与 Refresh Token

```
┌─────────────┐                    ┌───────────────┐
│   客户端     │                    │   认证服务器    │
└──────┬──────┘                    └───────┬───────┘
       │   POST /api/auth/login            │
       │   {username, password}            │
       │──────────────────────────────────>│
       │                                    │  验证用户名密码
       │                                    │  生成Access Token（15分钟有效）
       │                                    │  生成Refresh Token（7天有效）
       │   200 OK                           │
       │<──────────────────────────────────│
       │   {accessToken, refreshToken}      │
       │                                    │
       │   GET /api/users                   │
       │   Authorization: Bearer <access>   │
       │──────────────────────────────────>│  验证JWT → 返回数据
       │                                    │
       │   当accessToken过期（401）          │
       │<──────────────────────────────────│
       │                                    │
       │   POST /api/auth/refresh           │
       │   {refreshToken}                   │
       │──────────────────────────────────>│  验证refreshToken
       │                                    │  签发新的accessToken
       │   200 OK                           │
       │<──────────────────────────────────│
       │   {accessToken, refreshToken}      │
```

```java
// ========== Token刷新接口 ==========
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private CustomUserDetailsService userDetailsService;

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        if (!jwtUtil.validateToken(refreshToken)) {
            return ResponseEntity.status(401).body("Refresh Token无效或已过期");
        }

        String username = jwtUtil.getUsernameFromToken(refreshToken);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        // 签发新的Access Token
        String newAccessToken = jwtUtil.generateToken(username, userDetails.getAuthorities());
        String newRefreshToken = jwtUtil.generateRefreshToken(username);

        return ResponseEntity.ok(new TokenResponse(newAccessToken, newRefreshToken));
    }
}
```

### 6.6 Session vs JWT对比

| 维度 | Session-Cookie | JWT |
|------|---------------|-----|
| **状态存储** | 服务端内存/Redis | 客户端令牌自身 |
| **扩展性** | 需共享Session（粘性Session/Redis集中存储） | 天然无状态，任意节点均可验证 |
| **撤销能力** | 立即删除Session即可 | 令牌在有效期内无法主动撤销（除非黑名单） |
| **跨域支持** | 需处理跨域Cookie | 通过Authorization头，天然支持CORS |
| **安全性** | CSRF需防护 | XSS泄露风险较大（令牌存在localStorage） |
| **存储开销** | 服务端存储空间与活跃用户数成正比 | 服务端无存储开销 |
| **Token大小** | Session ID很小 | JWT包含用户信息，体积较大 |

---

## 7. OAuth2协议族

### 7.1 OAuth2本质

> 💡 OAuth2是一个**授权框架**，不是认证协议。它解决的是"第三方应用如何获取用户资源的授权"问题，而非"你是谁"的问题。**OAuth2 + JWT = 分布式的认证授权解决方案。**

### 7.2 四大角色

| 角色 | 说明 | 示例 |
|------|------|------|
| **Resource Owner** | 资源所有者（用户） | 你本人 |
| **Client** | 第三方应用 | 你的App、前端应用 |
| **Authorization Server** | 授权服务器 | 认证中心，颁发令牌 |
| **Resource Server** | 资源服务器 | API网关，校验令牌提供资源 |

### 7.3 四种授权模式

| 授权模式 | 安全等级 | 适用场景 | 当前状态 |
|---------|---------|---------|---------|
| **Authorization Code** | ⭐⭐⭐⭐⭐ | 有后端的Web应用 | ✅ 推荐使用 |
| **Authorization Code + PKCE** | ⭐⭐⭐⭐⭐ | 移动端/SPA单页应用 | ✅ 推荐使用 |
| **Client Credentials** | ⭐⭐⭐⭐ | 服务间调用（机器对机器） | ✅ 推荐使用 |
| **Password** | ⭐⭐ | 遗留系统/信任的第一方应用 | ❌ 已废弃（RFC 6749 → RFC 9449） |
| **Implicit** | ⭐ | 纯前端应用（简化流程） | ❌ 已废弃（安全性差） |

### 7.4 Authorization Code 授权码模式（完整流程）

```
┌──────────┐        ┌──────────┐        ┌──────────────┐        ┌──────────────┐
│  用户     │        │ 客户端    │        │ 授权服务器    │        │ 资源服务器    │
│ (浏览器)  │        │ (App)    │        │ (Auth中心)    │        │ (API服务)    │
└────┬─────┘        └────┬─────┘        └──────┬───────┘        └──────┬───────┘
     │                    │                      │                      │
     │  1.访问资源         │                      │                      │
     │───────────────────>│                      │                      │
     │                    │                      │                      │
     │  2.重定向到Auth    │                      │                      │
     │<───────────────────│                      │                      │
     │                    │                      │                      │
     │  3.用户认证授权     │                      │                      │
     │──────────────────────────────────────────>│                      │
     │                    │                      │                      │
     │  4.返回授权码      │                      │                      │
     │<──────────────────────────────────────────│                      │
     │                    │                      │                      │
     │  5.用授权码交换    │                      │                      │
     │                    │─────────────────────>│                      │
     │                    │                      │                      │
     │  6.返回AccessToken │                      │                      │
     │                    │<─────────────────────│                      │
     │                    │                      │                      │
     │  7.携带Token请求   │                      │                      │
     │                    │─────────────────────────────────────────────>│
     │                    │                      │                      │
     │  8.返回资源数据    │                      │                      │
     │                    │<─────────────────────────────────────────────│
     │                    │                      │                      │
```

### 7.5 Spring Authorization Server（Spring Boot 3.x官方方案）

```xml
<!-- Spring Authorization Server 依赖 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-authorization-server</artifactId>
</dependency>
```

```java
// ========== 授权服务器配置 ==========
@Configuration
@EnableAuthorizationServer
public class AuthorizationServerConfig {

    @Bean
    public RegisteredClientRepository registeredClientRepository() {
        // 注册客户端
        RegisteredClient registeredClient = RegisteredClient.withId(UUID.randomUUID().toString())
            .clientId("my-client-id")
            .clientSecret("{noop}my-secret")
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
            .redirectUri("http://localhost:8080/login/oauth2/code/my-client")
            .scope("openid", "profile", "email")
            .tokenSettings(TokenSettings.builder()
                .accessTokenTimeToLive(Duration.ofMinutes(30))
                .refreshTokenTimeToLive(Duration.ofDays(7))
                .build())
            .build();

        return new InMemoryRegisteredClientRepository(registeredClient);
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        // 生成RSA密钥对用于签名JWT
        RSAKey rsaKey = Jwks.generateRsa();
        JWKSet jwkSet = new JWKSet(rsaKey);
        return (jwkSelector, securityContext) -> jwkSelector.select(jwkSet);
    }

    @Bean
    public ProviderSettings providerSettings() {
        return ProviderSettings.builder()
            .issuer("http://auth-server:9000")
            .build();
    }
}
```

### 7.6 OAuth2 Resource Server 配置

```java
@Configuration
@EnableWebSecurity
public class ResourceServerConfig {

    @Bean
    public SecurityFilterChain resourceServerFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/**")    // 仅拦截 /api/**
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/api/public/**").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            );

        return http.build();
    }

    // 自定义JWT认证转换器（将JWT claims转为GrantedAuthority）
    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthorities = new JwtGrantedAuthoritiesConverter();
        grantedAuthorities.setAuthorityPrefix("ROLE_");
        grantedAuthorities.setAuthoritiesClaimName("roles");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(grantedAuthorities);
        return converter;
    }
}
```

---

## 8. 方法级安全控制

### 8.1 启用方法级安全

```java
@Configuration
@EnableMethodSecurity   // Spring Security 6.0+ 推荐（替代 @EnableGlobalMethodSecurity）
// @EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)  // 旧版
public class MethodSecurityConfig {
    // 无需额外代码
}
```

### 8.2 核心注解

| 注解 | 说明 | 示例 |
|------|------|------|
| `@PreAuthorize` | 方法执行前校验权限 | `@PreAuthorize("hasRole('ADMIN')")` |
| `@PostAuthorize` | 方法执行后校验权限，可访问返回值 | `@PostAuthorize("returnObject.creator == authentication.name")` |
| `@PreFilter` | 方法执行前过滤集合参数 | `@PreFilter("filterObject.isPublic()")` |
| `@PostFilter` | 方法执行后过滤返回集合 | `@PostFilter("filterObject.creator == authentication.name")` |
| `@Secured` | 简化版角色校验（不支持SpEL） | `@Secured("ROLE_ADMIN")` |
| `@RolesAllowed` | JSR-250标准注解 | `@RolesAllowed("ADMIN")` |

### 8.3 SpEL权限表达式

| 表达式 | 含义 |
|--------|------|
| `hasRole('ADMIN')` | 是否有 ROLE_ADMIN 角色 |
| `hasAnyRole('ADMIN','USER')` | 是否有任意指定角色 |
| `hasAuthority('sys:user:create')` | 是否有指定权限 |
| `hasAnyAuthority('p1','p2')` | 是否有任一指定权限 |
| `permitAll()` | 总是允许 |
| `denyAll()` | 总是拒绝 |
| `isAnonymous()` | 是否匿名用户 |
| `isAuthenticated()` | 是否已认证（排除匿名） |
| `isFullyAuthenticated()` | 是否完整认证（排除记住我） |
| `#oauth2.hasScope('read')` | OAuth2 scope检查 |
| `authentication.name` | 当前用户名 |
| `#参数名` | 方法参数引用 |

### 8.4 实战用法

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    // 仅管理员可创建用户
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result createUser(@Valid @RequestBody UserCreateRequest request) {
        return Result.success(userService.create(request));
    }

    // 用户只能查询自己的数据，管理员可查所有
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    public Result getUser(@PathVariable Long id) {
        return Result.success(userService.getById(id));
    }

    // 方法后校验：只能获取自己创建的订单
    @GetMapping("/my-orders")
    @PostAuthorize("returnObject.data.creator == authentication.name")
    public Result getOrders() {
        return Result.success(orderService.getCurrentUserOrders());
    }

    // 过滤返回结果：只保留公开数据
    @GetMapping("/public")
    @PostFilter("filterObject.visible == true")
    public List<Document> getPublicDocs() {
        return documentService.findAll();
    }

    // 复杂表达式：组合条件
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or " +
                  "(hasRole('MANAGER') and #id == authentication.principal.deptId)")
    public Result deleteUser(@PathVariable Long id) {
        return Result.success(userService.delete(id));
    }
}
```

### 8.5 自定义权限校验

```java
// 1. 定义权限校验器
@Component("customAuth")
public class CustomAuthorizationEvaluator {

    public boolean isMemberOfTeam(Long teamId, Authentication authentication) {
        // 验证当前用户是否属于指定团队
        UserDetailsImpl user = (UserDetailsImpl) authentication.getPrincipal();
        return user.getTeamIds().contains(teamId);
    }

    public boolean canAccessResource(String resourceId, Authentication auth) {
        // 复杂业务逻辑判断
        return false;
    }
}

// 2. 在注解中使用
@RestController
@RequestMapping("/api/teams")
public class TeamController {

    @GetMapping("/{teamId}/members")
    @PreAuthorize("@customAuth.isMemberOfTeam(#teamId, authentication)")
    public Result getTeamMembers(@PathVariable Long teamId) {
        return Result.success(teamService.getMembers(teamId));
    }
}
```

---

## 9. 高频踩坑与误区

### 9.1 CORS配置后仍然跨域报错

**现象**：配置了 `cors()` 但前端仍报跨域错误。

**原因**：**CORS配置必须在Spring Security的FilterChain中生效**，而不是只在Spring MVC中配置。Security的过滤器在MVC之前执行，先拦截了OPTIONS预检请求。

```java
// ❌ 错误：只在MVC配置CORS
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**").allowedOrigins("*");  // Security拦在前面，无效
    }
}

// ✅ 正确：在Security中配置CORS
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        // .csrf(csrf -> csrf.disable())
        ...
}

// ✅ 或：在Security和MVC都配置（Security允许CORS通过，MVC增强）
```

> ⚠️ **关键提醒**：`cors()` 只是将Spring Security的过滤器链设置为允许CORS通过，你需要通过 `CorsConfigurationSource` 指定具体允许的源、方法、头。跨域且需要携带Cookie时，`allowedOrigins` 不能使用 `*`，必须明确指定。

### 9.2 permitAll() 配置了但访问仍返回401

**现象**：配置了 `.requestMatchers("/api/public/**").permitAll()` 但访问 `/api/public/hello` 仍被拦截。

**原因分析**：

```java
// ❌ 错误1：permitAll() 配在 authenticated() 之后
.authorizeHttpRequests(authz -> authz
    .anyRequest().authenticated()
    .requestMatchers("/api/public/**").permitAll()  // 永远无法匹配！
)

// ❌ 错误2：路径不匹配（Ant风格路径必须完全一致）
.requestMatchers("/api/public/").permitAll()
// 访问 /api/public/hello → 不匹配，走后续规则被拦截

// ✅ 正确：permitAll() 必须在前，精确匹配优先
.authorizeHttpRequests(authz -> authz
    .requestMatchers("/api/public/**").permitAll()      // 1. 公开API
    .requestMatchers("/api/admin/**").hasRole("ADMIN")  // 2. 管理API
    .anyRequest().authenticated()                       // 3. 其他需认证
)
```

> 💡 **规则匹配顺序**：`authorizeHttpRequests` 中的规则是按书写顺序匹配的，**第一条匹配的规则生效**。因此宽松规则写在前面，严格规则写在后面。

### 9.3 There is no PasswordEncoder mapped for the id "null"

**现象**：内存用户配置后登录报错。

```java
// ❌ 错误：明文密码未加编码前缀
@Bean
public UserDetailsService users() {
    UserDetails user = User.withDefaultPasswordEncoder()
        .username("admin")
        .password("admin123")  // 存储在内存中为 {noop}admin123
        .roles("ADMIN")
        .build();
    return new InMemoryUserDetailsManager(user);
}

// ❌ 错误：使用 DelegatingPasswordEncoder 但密码没有 {前缀}
// 数据库中存储的密码：$2a$10$...（没有{bcrypt}前缀）

// ✅ 正确1：使用 PasswordEncoder 编码后赋值
@Bean
public UserDetailsService users(PasswordEncoder encoder) {
    UserDetails user = User.builder()
        .username("admin")
        .password(encoder.encode("admin123"))  // 编码后存储
        .roles("ADMIN")
        .build();
    return new InMemoryUserDetailsManager(user);
}

// ✅ 正确2：使用 DelegatingPasswordEncoder 格式
// 数据库密码字段存储：{bcrypt}$2a$10$...
// 此时 UserDetails 的 getPassword() 必须返回完整格式
```

### 9.4 JWT认证后无法获取Authentication

**现象**：`SecurityContextHolder.getContext().getAuthentication()` 返回 `null`。

**原因分析**：

```java
// ❌ 错误1：JWT过滤器中未设置SecurityContext
// 解析了Token，但没有调用：
SecurityContextHolder.getContext().setAuthentication(authentication);

// ❌ 错误2：异步线程中丢失上下文
@Async
public void asyncMethod() {
    // 新线程中 SecurityContextHolder 为空！
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
}

// ✅ 正确：设置策略为 MODE_INHERITABLETHREADLOCAL
SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);

// ✅ 或：手动传递
@Async
public void asyncMethod(Authentication authentication) {
    SecurityContextHolder.getContext().setAuthentication(authentication);
    // 业务逻辑
}
```

### 9.5 JWT过期后无限循环刷新

**现象**：Access Token过期后，客户端自动刷新，但Refresh Token也过期了，导致无限重定向/循环请求。

```java
// 解决方案：区分Token过期类型
public class TokenExpiredException extends AuthenticationException {
    public TokenExpiredException(String msg) {
        super(msg);
    }
}

// 异常处理
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .exceptionHandling(ex -> ex
            .authenticationEntryPoint((request, response, authException) -> {
                response.setContentType("application/json;charset=UTF-8");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

                String message;
                if (authException instanceof TokenExpiredException) {
                    // AccessToken过期 → 客户端尝试刷新
                    message = "AccessToken已过期，请使用RefreshToken刷新";
                    response.setHeader("X-Token-Expired", "true");
                } else {
                    message = "未认证，请先登录";
                }

                response.getWriter().write(
                    JSON.toJSONString(Result.error(401, message))
                );
            })
        );
    return http.build();
}
```

### 9.6 CSRF 导致 POST/PUT/DELETE 403

**现象**：GET请求正常，POST/PUT/DELETE请求返回403。

```java
// ❌ 原因：Spring Security 默认开启 CSRF 防护
// 所有非GET/HEAD/TRACE/OPTIONS请求都需要CSRF Token

// ✅ 解决方案1：前后端分离REST API，关闭CSRF
http.csrf(csrf -> csrf.disable());

// ✅ 解决方案2：传统MVC应用，使用Thymeleaf自动注入CSRF Token
// <input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}" />

// ✅ 解决方案3：将CSRF Token通过Cookie返回，前端读取后添加到请求头
http.csrf(csrf -> csrf
    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
);
```

> 💡 **RESTful API无状态原则**：当使用JWT无状态认证（SessionCreationPolicy.STATELESS）时，CSRF防护通常是**不需要**的，因为浏览器不会自动附加JWT到跨站请求（Token在Authorization头，不在Cookie中）。

---

## 10. 随堂基础练习

### 练习1：Spring Boot集成Security

创建一个Spring Boot项目，集成Spring Security，配置内存用户，实现表单登录。

**要求**：
1. 创建至少2个内存用户（admin/管理员角色，user/普通用户角色）
2. 配置 `/api/public/**` 允许匿名访问
3. 配置 `/api/admin/**` 仅管理员可访问
4. 其他路径需要认证

<details>
<summary>参考答案</summary>

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form.permitAll())
            .httpBasic(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder encoder) {
        UserDetails admin = User.builder()
            .username("admin").password(encoder.encode("admin123")).roles("ADMIN").build();
        UserDetails user = User.builder()
            .username("user").password(encoder.encode("user123")).roles("USER").build();
        return new InMemoryUserDetailsManager(admin, user);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

</details>

### 练习2：实现JWT过滤器

自定义 `OncePerRequestFilter`，实现JWT校验过滤器，注册到SecurityFilterChain中。

### 练习3：方法级权限

创建一个Service，声明3个方法分别用 `@PreAuthorize` 控制：
- `getAdminData()` — 仅管理员可访问
- `getUserData()` — 管理员和用户均可访问
- `getPersonalData(#userId)` — 仅本人可访问

---

## 11. 章节综合实操案例

### 案例：基于JWT + Spring Security的前后端分离登录系统

#### 11.1 整体架构

```
┌─────────────────────┐       ┌──────────────────────────────────────┐
│   前端 (React/Vue)   │       │         后端 Spring Boot             │
│                     │       │                                      │
│  1. 用户输入密码     │       │  ┌──────────────────────────────┐   │
│  2. 发送 POST /auth/login ──>  │  AuthController               │   │
│  3. 接收 JWT Token   │       │  └──────────┬───────────────────┘   │
│  4. 后续请求携带     │       │             ▼                      │
│     Authorization:   │       │  ┌──────────────────────────────┐   │
│     Bearer <token>   │       │  │  JwtAuthenticationFilter     │   │
│                     │       │  │  (解析Token, 设置上下文)      │   │
│                     │       │  └──────────┬───────────────────┘   │
│                     │       │             ▼                      │
│                     │       │  ┌──────────────────────────────┐   │
│                     │       │  │  SecurityFilterChain          │   │
│                     │       │  │  (URL规则 + 异常处理)         │   │
│                     │       │  └──────────┬───────────────────┘   │
│                     │       │             ▼                      │
│                     │       │  ┌──────────────────────────────┐   │
│                     │       │  │  @PreAuthorize               │   │
│                     │       │  │  (方法级权限控制)             │   │
│                     │       │  └──────────────────────────────┘   │
└─────────────────────┘       └──────────────────────────────────────┘
```

#### 11.2 项目依赖

```xml
<dependencies>
    <!-- Spring Boot + Security -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    <!-- JWT -->
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-api</artifactId>
        <version>0.11.5</version>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-impl</artifactId>
        <version>0.11.5</version>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-jackson</artifactId>
        <version>0.11.5</version>
        <scope>runtime</scope>
    </dependency>
    <!-- Lombok -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

#### 11.3 安全配置

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // 开启方法级安全
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtFilter,
            JwtAuthenticationEntryPoint entryPoint) throws Exception {

        http
            // 禁用CSRF（前后端分离）
            .csrf(csrf -> csrf.disable())
            // 启用CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // 无状态（不创建Session）
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            // URL授权规则
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            // JWT过滤器（在用户名密码过滤器之前）
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            // 异常处理
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(entryPoint)
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setContentType("application/json;charset=UTF-8");
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.getWriter().write(
                        "{\"code\":403,\"message\":\"权限不足\"}"
                    );
                })
            );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:3000"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

#### 11.4 JWT 工具类

```java
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secretKey;               // Base64编码的密钥
    @Value("${jwt.access-token-expiration:900000}")
    private long accessTokenExpiration;     // 15分钟
    @Value("${jwt.refresh-token-expiration:604800000}")
    private long refreshTokenExpiration;    // 7天

    private Key signingKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));
    }

    // 生成Access Token
    public String generateAccessToken(Long userId, String username, Set<String> roles) {
        Date now = new Date();
        return Jwts.builder()
            .setSubject(username)
            .claim("userId", userId)
            .claim("roles", roles)
            .setIssuedAt(now)
            .setExpiration(new Date(now.getTime() + accessTokenExpiration))
            .signWith(signingKey())
            .compact();
    }

    // 生成Refresh Token
    public String generateRefreshToken(String username) {
        Date now = new Date();
        return Jwts.builder()
            .setSubject(username)
            .setIssuedAt(now)
            .setExpiration(new Date(now.getTime() + refreshTokenExpiration))
            .signWith(signingKey())
            .compact();
    }

    // 从Token提取用户名
    public String getUsername(String token) {
        return parseClaims(token).getSubject();
    }

    // 从Token提取Claims
    public Claims parseClaims(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(signingKey())
            .build()
            .parseClaimsJws(token)
            .getBody();
    }

    // 验证Token有效性
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
```

#### 11.5 JWT认证过滤器

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    @Autowired
    private UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String token = resolveToken(request);

        if (token != null && jwtTokenProvider.validateToken(token)) {
            String username = jwtTokenProvider.getUsername(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities()
                );
            authentication.setDetails(
                new WebAuthenticationDetailsSource().buildDetails(request)
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
```

#### 11.6 认证入口点异常处理

```java
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        String message = "未认证，请先登录";
        // 判断是否JWT过期（可以在过滤器中设置自定义异常属性）
        if (request.getAttribute("token-expired") != null) {
            message = "令牌已过期，请使用RefreshToken刷新";
        }

        response.getWriter().write(
            "{\"code\":401,\"message\":\"" + message + "\"}"
        );
    }
}
```

#### 11.7 登录控制器

```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    @Autowired
    private UserDetailsService userDetailsService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        // 1. 认证用户
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                loginRequest.getUsername(),
                loginRequest.getPassword()
            )
        );

        // 2. 获取用户信息
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        // 3. 生成Token
        String accessToken = jwtTokenProvider.generateAccessToken(
            userDetails.getId(),
            userDetails.getUsername(),
            userDetails.getRoles()
        );
        String refreshToken = jwtTokenProvider.generateRefreshToken(
            userDetails.getUsername()
        );

        // 4. 返回
        return ResponseEntity.ok(new LoginResponse(
            accessToken,
            refreshToken,
            "Bearer",
            userDetails.getUsername(),
            userDetails.getRoles()
        ));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            return ResponseEntity.status(401).body(
                new ErrorResponse("Refresh Token无效或已过期")
            );
        }

        String username = jwtTokenProvider.getUsername(refreshToken);
        UserDetailsImpl userDetails =
            (UserDetailsImpl) userDetailsService.loadUserByUsername(username);

        String newAccessToken = jwtTokenProvider.generateAccessToken(
            userDetails.getId(), username, userDetails.getRoles()
        );
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(username);

        return ResponseEntity.ok(new LoginResponse(
            newAccessToken, newRefreshToken, "Bearer",
            username, userDetails.getRoles()
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        // 无状态JWT：客户端删除Token即可
        // 如需服务端黑名单，可将Token加入Redis黑名单（TTL = Token过期时间）
        return ResponseEntity.ok(new MessageResponse("已退出登录"));
    }
}
```

#### 11.8 业务控制器 — 方法级权限演示

```java
@RestController
@RequestMapping("/api")
public class DemoController {

    // 公开接口 — 无需认证
    @GetMapping("/public/hello")
    public Result<String> publicHello() {
        return Result.success("Hello, 这是一个公开接口");
    }

    // 所有登录用户可访问
    @GetMapping("/user/profile")
    @PreAuthorize("isAuthenticated()")
    public Result<UserProfile> getProfile() {
        UserDetailsImpl user = (UserDetailsImpl) SecurityContextHolder
            .getContext().getAuthentication().getPrincipal();
        return Result.success(new UserProfile(user.getId(), user.getUsername(), user.getRoles()));
    }

    // 仅管理员
    @GetMapping("/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<UserVO>> listUsers() {
        return Result.success(userService.listAllUsers());
    }

    // 管理员或数据创建者本人
    @DeleteMapping("/admin/users/{id}")
    @PreAuthorize("hasRole('ADMIN') or @dataAuth.isOwner(#id, authentication)")
    public Result<?> deleteUser(@PathVariable Long id) {
        userService.deleteById(id);
        return Result.success("删除成功");
    }

    // 复杂权限：部门经理可查看本部门数据
    @GetMapping("/dept/{deptId}/data")
    @PreAuthorize("@dataAuth.canAccessDept(#deptId, authentication)")
    public Result<?> getDeptData(@PathVariable String deptId) {
        return Result.success(dataService.getByDept(deptId));
    }
}
```

#### 11.9 application.yml配置

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/security_demo?useUnicode=true&characterEncoding=utf-8
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true

jwt:
  secret: bXlTZWNyZXRLZXlGb3JKV1RBdXRoZW50aWNhdGlvbkFwcGxpY2F0aW9u
  access-token-expiration: 900000      # 15分钟
  refresh-token-expiration: 604800000  # 7天
```

#### 11.10 请求测试

```bash
# 1. 登录获取Token
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# 响应：
# {
#   "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
#   "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
#   "tokenType": "Bearer",
#   "username": "admin",
#   "roles": ["ROLE_ADMIN"]
# }

# 2. 携带Token访问受保护资源
curl -X GET http://localhost:8080/api/admin/users \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."

# 3. 无Token访问 → 401
curl -X GET http://localhost:8080/api/admin/users
# {"code":401,"message":"未认证，请先登录"}

# 4. 普通用户访问管理员接口 → 403
curl -X GET http://localhost:8080/api/admin/users \
  -H "Authorization: Bearer <user_token>"
# {"code":403,"message":"权限不足"}
```

---

## 12. 分层综合习题

### 12.1 基础题

1. **概念理解**：简述Authentication和Authorization的区别。
2. **RBAC模型**：画出RBAC权限模型的ER图，说明用户-角色-权限的关系。
3. **PasswordEncoder**：解释为什么不能使用MD5存储密码，BCrypt是如何解决这个问题的。
4. **过滤器链**：列出Spring Security Filter Chain中至少5个关键过滤器及其职责。
5. **配置题**：写一段Security配置，允许 `/api/public/**` 匿名访问，`/api/admin/**` 需要 ADMIN 角色。

### 12.2 进阶题

1. **JWT结构**：画出JWT的三段结构，说明每一段的编码方式和内容。
2. **JWT过滤器**：自定义一个 `OncePerRequestFilter`，实现从请求头提取JWT、验证、设置SecurityContext的完整逻辑。
3. **异常处理**：配置 `AuthenticationEntryPoint` 和 `AccessDeniedHandler`，使未认证返回401、权限不足返回403，均为JSON格式。
4. **CORS排查**：前端访问后端出现跨域错误，列出所有可能的原因和解决方案。
5. **方法安全**：使用 `@PreAuthorize` 实现一个自定义权限校验：用户只能删除自己创建的文章（文章作者ID = 当前用户ID）。

### 12.3 精通题

1. **OAuth2流程**：详细描述 Authorization Code 模式的完整流程，说明为什么比 Password 模式更安全。
2. **Token认证架构**：设计一个微服务架构下的Token认证方案，包括认证中心、资源服务器、网关的交互流程。
3. **JWT撤销方案**：JWT天然不可撤销，设计一个支持撤销的JWT方案（不考虑Refresh Token轮转）。
4. **多SecurityFilterChain**：配置两套 `SecurityFilterChain`，分别处理 `/api/**`（JWT无状态）和 `/page/**`（Session表单登录），解决一个系统同时支持RESTful API和MVC页面的场景。
5. **自定义AuthenticationProvider**：实现一个支持验证码 + 用户名密码双因素认证的 `AuthenticationProvider`。

---

## 13. 本章复盘速记清单

### 13.1 核心概念

| 概念 | 一句话记忆 |
|------|-----------|
| 认证(Authentication) | 验证你是谁 |
| 授权(Authorization) | 你能做什么 |
| RBAC | 用户 → 角色 → 权限 |
| Filter Chain | 请求经过层层过滤器最终到达业务代码 |
| SecurityContextHolder | 存放当前认证信息的ThreadLocal容器 |

### 13.2 核心注解速查

| 注解 | 作用 | 位置 |
|------|------|------|
| `@EnableWebSecurity` | 开启Security配置 | 配置类 |
| `@EnableMethodSecurity` | 开启方法级安全注解 | 配置类 |
| `@PreAuthorize` | 方法执行前权限校验 | 方法 |
| `@PostAuthorize` | 方法执行后权限校验 | 方法 |
| `@PreFilter` | 执行前过滤集合参数 | 方法 |
| `@PostFilter` | 执行后过滤返回集合 | 方法 |
| `@Secured` | 简化角色校验（不支持SpEL） | 方法 |
| `@RolesAllowed` | JSR-250标准注解 | 方法 |

### 13.3 核心配置参数

| 参数 | 说明 | 推荐值 |
|------|------|--------|
| `SessionCreationPolicy.STATELESS` | 无状态（JWT场景） | JWT API |
| `SessionCreationPolicy.IF_REQUIRED` | 需要时创建Session | 传统MVC |
| `.csrf().disable()` | 禁用CSRF | RESTful API |
| `BCryptPasswordEncoder(10-12)` | 密码编码强度 | 10-12 |
| `jwt.access-token-expiration` | Access Token过期时间 | 15-30分钟 |
| `jwt.refresh-token-expiration` | Refresh Token过期时间 | 7-30天 |

### 13.4 核心过滤器顺序（按优先级）

1. `SecurityContextPersistenceFilter` — 上下文恢复/存储
2. `LogoutFilter` — 登出处理
3. `UsernamePasswordAuthenticationFilter` — 表单登录认证
4. `JwtAuthenticationFilter` (自定义) — JWT认证（通常插入在3之前）
5. `ExceptionTranslationFilter` — 异常处理
6. `FilterSecurityInterceptor` — 最终权限裁决

### 13.5 常用权限表达式速查

```
hasRole('ADMIN')          → ROLE_ADMIN角色
hasAnyRole('A','B')       → ROLE_A 或 ROLE_B
hasAuthority('perm')      → 拥有perm权限
hasAnyAuthority('a','b')  → 拥有a或b权限
isAuthenticated()         → 已登录（非匿名）
isAnonymous()             → 匿名用户
permitAll()               → 全部放行
denyAll()                 → 全部拒绝
#this == authentication.name  → 当前用户名等于参数
@bean.method(#param, auth)    → 调用Bean方法校验
```

### 13.6 常见异常及解决

| 异常 | 原因 | 解决 |
|------|------|------|
| `401 Unauthorized` | 未认证或Token过期 | 检查Token、登录 |
| `403 Forbidden` | 权限不足 | 检查角色/权限配置 |
| `There is no PasswordEncoder mapped` | 密码格式不匹配 | 使用正确编码格式 `{bcrypt}...` |
| `AccessDeniedException` | 方法级权限不满足 | 检查`@PreAuthorize`表达式 |
| `JwtException` | JWT签名无效/过期 | 检查密钥、Token时效 |
| `CORS 403` | 跨域被Security拦截 | 配置`.cors()` + `CorsConfigurationSource` |

---

## 14. 精通拓展补充-P2

### 14.1 OAuth2授权码模式详解（含PKCE）

#### 14.1.1 标准授权码模式

```
GET /authorize?response_type=code
              &client_id=my-client
              &redirect_uri=http://localhost:8080/callback
              &scope=openid%20profile
              &state=xyz123
```

**关键安全机制**：

| 机制 | 说明 | 防护目标 |
|------|------|---------|
| `state` 参数 | 客户端生成随机值，回调时校验是否一致 | CSRF攻击（授权码劫持） |
| `redirect_uri` | 预注册的合法回调地址，服务器严格校验 | 授权码被重定向到恶意站点 |
| `code` 一次性 | 授权码使用即失效，有效期短（通常1-2分钟） | 授权码泄露后的利用风险 |
| `client_secret` | 只有客户端和授权服务器知道的凭证 | 防止伪造客户端 |

#### 14.1.2 授权码 + PKCE（Proof Key for Code Exchange）

> 💡 PKCE（读作"pixie"）是增强版授权码模式，专为没有后端服务器的SPA和移动端设计，防止授权码拦截攻击。

```
┌──────────┐                    ┌──────────┐                    ┌──────────────┐
│  客户端    │                    │  浏览器    │                    │  授权服务器    │
│ (SPA/Mobile)│                │            │                    │              │
└─────┬────┘                    └─────┬──────┘                    └──────┬───────┘
      │                               │                                │
      │  1. 生成 code_verifier(随机串)  │                                │
      │  2. 计算 code_challenge         │                                │
      │     = BASE64URL(SHA256(verifier))                               │
      │                               │                                │
      │  3. 请求授权 + code_challenge   │                                │
      │───────────────────────────────────────────────────────────────>│
      │                               │                                │
      │  4. 用户认证 & 授权              │                                │
      │                               │    用户登录页面                   │
      │                              <─────────────────────────────────│
      │                               │                                │
      │  5. 返回授权码                  │                                │
      │<────────────────────────────────────────────────────────────────│
      │                               │                                │
      │  6. 交换Token                  │                                │
      │     POST /token                │                                │
      │     code + code_verifier       │                                │
      │───────────────────────────────────────────────────────────────>│
      │                               │                                │
      │  7. 服务器校验 code_challenge   │                                │
      │     = BASE64URL(SHA256(code_verifier))                         │
      │                               │                                │
      │  8. 返回 AccessToken           │                                │
      │<────────────────────────────────────────────────────────────────│
```

**与标准模式的差异**：

| 维度 | 标准授权码 | 授权码 + PKCE |
|------|-----------|---------------|
| `client_secret` | 需要（后端安全存储） | 不需要（公开客户端可用） |
| `code_challenge` | 无 | SHA256(code_verifier) 或 code_verifier本身 |
| `code_verifier` | 无 | 客户端生成的随机字符串 |
| 适用客户端 | 有后端的Web应用 | SPA、移动端、原生应用 |
| 抗拦截攻击 | 依赖client_secret | 数学上保证安全 |

### 14.2 自定义AuthenticationProvider

```java
// 场景：双因素认证（密码 + 短信验证码）
@Component
public class TwoFactorAuthenticationProvider implements AuthenticationProvider {

    @Autowired
    private CustomUserDetailsService userDetailsService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private SmsCodeService smsCodeService;

    @Override
    public Authentication authenticate(Authentication authentication)
            throws AuthenticationException {

        String username = authentication.getName();
        String password = (String) authentication.getCredentials();
        String smsCode = (String) authentication.getDetails();  // 验证码

        // 1. 加载用户
        UserDetails userDetails;
        try {
            userDetails = userDetailsService.loadUserByUsername(username);
        } catch (UsernameNotFoundException e) {
            throw new BadCredentialsException("用户名或密码错误");
        }

        // 2. 校验密码
        if (!passwordEncoder.matches(password, userDetails.getPassword())) {
            throw new BadCredentialsException("用户名或密码错误");
        }

        // 3. 校验短信验证码
        if (!smsCodeService.validateSmsCode(username, smsCode)) {
            throw new BadCredentialsException("短信验证码错误");
        }

        // 4. 认证成功
        return new UsernamePasswordAuthenticationToken(
            userDetails,
            null,
            userDetails.getAuthorities()
        );
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}

// 注册到ProviderManager
@Configuration
public class AuthProviderConfig {

    @Autowired
    private TwoFactorAuthenticationProvider twoFactorProvider;

    @Bean
    public AuthenticationManager authenticationManager(
            List<AuthenticationProvider> providers) {
        return new ProviderManager(providers);
        // ProviderManager 会遍历所有 Provider，按 supports() 匹配
    }
}
```

### 14.3 Session vs 无状态JWT架构深度对比

#### 14.3.1 有状态Session架构

```
┌─────────┐      ┌─────────┐      ┌─────────┐
│  Load    │      │  App    │      │  Redis  │
│ Balancer │      │ Server  │      │ (Session)│
└────┬────┘      └────┬────┘      └────┬────┘
     │                │                │
     │───请求1───────>│                │
     │                │───查Session───>│
     │                │<───用户信息─────│
     │                │                │
     │───请求2───────>│ (同一台或不同)   │
     │                │───查Session───>│
     │                │<───用户信息─────│
```

**优点**：
- 服务端可直接控制Session（踢人、强制下线）
- Token体积小（仅Session ID）
- 易于实现并发登录控制

**缺点**：
- 需要集中式Session存储（Redis）
- 每个请求都要查Redis，增加延迟
- 粘性Session或Session共享增加复杂度

#### 14.3.2 无状态JWT架构

```
┌─────────┐      ┌─────────┐
│  Load    │      │  App    │
│ Balancer │      │ Server  │
└────┬────┘      └────┬────┘
     │                │
     │───请求1 + JWT─>│
     │                │──本地校验JWT签名
     │                │──从JWT提取用户信息
     │                │
     │───请求2 + JWT─>│ (任意节点)
     │                │──本地校验JWT签名
```

**优点**：
- 无状态，任意节点都可处理请求
- 无需集中存储，天然支持水平扩展
- 减少Redis查询开销
- 跨域友好

**缺点**：
- Token无法主动撤销（需黑名单方案）
- Token体积较大
- 无法实现精确的并发登录控制

#### 14.3.3 混合架构（最优实践）

```java
// 方案：JWT做"通行证"，Redis黑名单做"撤销控制"
@Component
public class RevocableJwtFilter extends OncePerRequestFilter {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String token = resolveToken(request);
        if (token != null) {
            // 1. 先检查黑名单（Redis）
            String jti = jwtTokenProvider.getTokenId(token);  // JWT ID
            if (Boolean.TRUE.equals(redisTemplate.hasKey("jwt:blacklist:" + jti))) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token已被撤销");
                return;
            }

            // 2. 再验证签名和有效期
            if (jwtTokenProvider.validateToken(token)) {
                // 3. 设置认证信息
                // ...
            }
        }

        filterChain.doFilter(request, response);
    }
}

// 撤销Token
@Service
public class TokenRevocationService {
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    public void revokeToken(String token, long ttlSeconds) {
        String jti = jwtTokenProvider.getTokenId(token);
        // 将JWT ID加入黑名单，TTL = Token剩余有效期
        redisTemplate.opsForValue().set(
            "jwt:blacklist:" + jti,
            "revoked",
            Duration.ofSeconds(ttlSeconds)
        );
    }

    // 用户登出时撤销所有Token
    public void revokeAllUserTokens(Long userId) {
        // 方案：增加用户版本号，JWT中包含该版本号
        // 用户登出/修改密码时，版本号+1，旧Token自动失效
        redisTemplate.opsForValue().increment("jwt:version:user:" + userId);
    }
}
```

### 14.4 SecurityContextHolder策略详解

```java
// SecurityContextHolder 有三种策略模式：

// 1. MODE_THREADLOCAL（默认）
// 每个线程独立存储，子线程无法继承
SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_THREADLOCAL);

// 2. MODE_INHERITABLETHREADLOCAL
// 子线程可继承父线程的SecurityContext
// 适用于 @Async、线程池场景
SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);

// 3. MODE_GLOBAL
// 全局单例（仅适用于单线程应用）
SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_GLOBAL);
```

```java
// 异步任务中传递上下文的更安全方式：手动传递
@Async
public CompletableFuture<Void> processAsync() {
    // 获取当前线程的上下文
    SecurityContext context = SecurityContextHolder.getContext();

    // 异步执行
    executorService.submit(() -> {
        try {
            // 在新线程中设置上下文
            SecurityContextHolder.setContext(context);
            // 执行业务逻辑...
        } finally {
            // 清理，防止内存泄漏
            SecurityContextHolder.clearContext();
        }
    });

    return CompletableFuture.completedFuture(null);
}
```

### 14.5 零信任安全架构概述

> 🎯 **零信任（Zero Trust）**的核心原则：**永不信任，始终验证**。不再假设内部网络是安全的，每个请求都必须经过认证和授权。

**Spring Security 在零信任架构中的对应**：

| 零信任原则 | Spring Security实现 |
|-----------|-------------------|
| 始终验证身份 | JWT/OAuth2认证，每个请求都验证Token |
| 最小权限原则 | RBAC + 方法级 `@PreAuthorize` 精细化控制 |
| 微隔离 | 多个 `SecurityFilterChain` 隔离不同服务 |
| 持续监控 | Actuator + SecurityEvent审计日志 |
| 默认拒绝 | `denyAll()` + 白名单 `permitAll()` |

---

> 🎯 **Spring Security不是一个单纯的框架，而是一套完整的安全架构思想。** 理解其Filter Chain设计模式、认证与授权分离、RBAC权限模型，比记住API用法更重要。掌握了这些底层原理，无论框架如何演进，你都能快速上手。在实际项目中，**安全设计从第一天就要开始**，而不是在功能完成后才补上。
