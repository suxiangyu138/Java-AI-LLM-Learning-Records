# 06 - Spring Boot 安全与认证

> 定位：Spring Security 过滤器链、JWT 认证、方法级安全、OAuth2、常见安全配置——Boot 安全全解

## 📚 目录

1. [Spring Security 核心](#1-spring-security-核心)
2. [认证与授权模型](#2-认证与授权模型)
3. [JWT 认证实战](#3-jwt-认证实战)
4. [方法级安全](#4-方法级安全)
5. [OAuth2 与第三方登录](#5-oauth2-与第三方登录)
6. [常见安全配置](#6-常见安全配置)

---

## 1. Spring Security 核心

### 1.1 过滤器链

```
Spring Security = 过滤器链（SecurityFilterChain）
  请求 → 认证过滤器 → 授权过滤器 → Controller

核心过滤器：
  UsernamePasswordAuthenticationFilter（表单登录）
  JwtAuthenticationFilter（自定义 JWT）
  ExceptionTranslationFilter（异常 → 401/403）
  AuthorizationFilter（授权判断）

⚠️ 面试必答：
"Spring Security 是过滤器链——
 认证（你是谁）、授权（你能做什么）
 通过过滤器链分层完成；
 自定义过滤器插入链中。"
```

### 1.2 基础配置

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // ① 关闭 CSRF（无状态 JWT 场景）
            .csrf(csrf -> csrf.disable())
            // ② 会话无状态（JWT 不用 Session）
            .sessionManagement(sm ->
                sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // ③ 请求授权规则
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**", "/actuator/health").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            // ④ 自定义 JWT 过滤器（在用户名密码过滤器之前）
            .addFilterBefore(jwtFilter,
                UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
```

---

## 2. 认证与授权模型

```
Spring Security 核心接口：
  认证：Authentication（主体 + 权限）
  UserDetailsService（加载用户）
  UserDetails（用户信息）

授权：GrantedAuthority（权限）
  ROLE_ADMIN（角色） vs PERMISSION_READ（权限）

⚠️ 面试必答：
"认证 = Authentication 对象（主体是谁）；
 授权 = GrantedAuthority（能做什么）；
 UserDetailsService 加载用户是认证入口。"
```

---

## 3. JWT 认证实战

### 3.1 登录与令牌

```java
// ① 登录接口：校验后签发 JWT
@PostMapping("/auth/login")
public Result<LoginVO> login(@RequestBody LoginDTO dto) {
    // 认证（AuthenticationManager）
    Authentication auth = authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(dto.getUsername(), dto.getPassword()));

    // 生成 JWT（含用户名 + 角色）
    String token = jwtUtil.generateToken(auth);
    return Result.success(new LoginVO(token));
}
```

### 3.2 JWT 过滤器

```java
// ⚠️ 核心：JWT 认证过滤器（每个请求校验令牌）
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            String username = jwtUtil.getUsername(token);    // 解析

            if (username != null
                    && SecurityContextHolder.getContext()
                       .getAuthentication() == null) {
                UserDetails user = userDetailsService.loadUserByUsername(username);
                // ⚠️ 校验令牌有效性 + 放入安全上下文
                if (jwtUtil.validateToken(token, user)) {
                    var auth = new UsernamePasswordAuthenticationToken(
                        user, null, user.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        }
        chain.doFilter(request, response);    // 继续过滤器链
    }
}
```

### 3.3 JWT 工具类要点

```java
// JWT 安全规范：
// ① 签名 RS256（非对称）或 HS256（对称需保密密钥）
// ② 过期时间（15-30 分钟短时效）
// ③ 声明：sub（用户）、roles（角色）、exp（过期）
// ④ 密钥不硬编码（环境变量/配置中心）
// 常用库：jjwt / java-jwt
```

> 🎯 **要点**：JWT 认证三件套——登录签发（AuthenticationManager）、过滤器校验（Bearer 解析 + SecurityContext）、安全配置（无状态 + 白名单）。**认证成功后每次请求靠过滤器重建上下文**。

---

## 4. 方法级安全

```java
@Configuration
@EnableMethodSecurity                    // ⚠️ 开启方法级安全
public class MethodSecurityConfig { }

// 使用：方法级注解
@Service
public class OrderService {

    // ① 角色限制
    @PreAuthorize("hasRole('ADMIN')")
    public void adminOperation() { }

    // ② 权限限制
    @PreAuthorize("hasAuthority('order:delete')")
    public void deleteOrder(Long id) { }

    // ③ SpEL 表达式（灵活）
    @PreAuthorize("hasRole('ADMIN') or #order.userId == authentication.principal.id")
    public void updateOrder(Order order) { }

    // ④ 返回值过滤（少用）
    @PostAuthorize("returnObject.userId == authentication.principal.id")
    public Order getOrder(Long id) { }

    // ⑤ 运行时校验
    @PreAuthorize("hasRole('ADMIN')")
    public void risky() { }
}
```

> 🎯 **要点**：方法级安全 = @EnableMethodSecurity + @PreAuthorize（SpEL）。**"或"表达式实现属主校验**（本人或管理员）是越权防护的标准答案。

---

## 5. OAuth2 与第三方登录

### 5.1 OAuth2 角色

```
OAuth2 四角色：
  资源所有者（用户）
  客户端（应用）
  授权服务器（认证中心）
  资源服务器（API）

授权模式（常用）：
  授权码模式（Authorization Code）：Web 应用标准
  密码模式（Password）：第一方应用（已弃用趋势）

⚠️ 面试必答：
"OAuth2 = 授权框架——应用不接触密码，
 通过授权码/令牌访问用户资源；
 授权码模式是 Web 标准。"
```

### 5.2 Spring Security OAuth2

```java
// ① 资源服务器配置
@Configuration
@EnableWebSecurity
public class ResourceServerConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwkSetUri("http://auth-server/.well-known/jwks.json")));
        return http.build();
    }
}

// ② 第三方登录（GitHub/微信等）
// spring-boot-starter-oauth2-client
// 配置：
// spring.security.oauth2.client.registration.github.client-id=xxx
// .client-secret=xxx

// ③ 授权服务器：Spring Authorization Server（自建认证中心）
```

---

## 6. 常见安全配置

### 6.1 安全配置清单

```yaml
# application.yml 安全配置
server:
  # ① TLS（HTTPS 证书）
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-type: PKCS12

spring:
  # ② 请求大小限制（防超大请求 DoS）
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB
```

```java
// ③ 全局 CORS 配置（跨域）
@Configuration
public class CorsConfig {
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOriginPattern("*");      // 生产限定域名
        config.addAllowedMethod("*");
        config.addAllowedHeader("*");
        config.setAllowCredentials(true);
        return new CorsFilter(source -> config);  // 简化写法
    }
}
```

### 6.2 安全清单总结

```
✅ CSRF 关闭（仅无状态 JWT 场景）
✅ Session 无状态（STATELESS）
✅ 密码 BCrypt 加密（不存明文）
✅ 登录限流（防暴力破解）
✅ 接口白名单最小化（permitAll 只放必要接口）
✅ 响应不泄露内部异常
✅ 敏感接口加审计日志
✅ 依赖漏洞扫描（CI 门禁）
```

> 🎯 **要点**：安全配置 = SecurityFilterChain（规则）+ JWT 过滤器 + 方法级授权 + OAuth2 集成 + 工程安全（TLS/CORS/限流/BCrypt）。"白名单最小化 + 无状态 + BCrypt"是标准姿势。

---

> 🎯 **核心要点**：安全体系 = **过滤器链**（认证 + 授权分层）+ **JWT 三件套**（登录签发/过滤器校验/无状态配置）+ **方法级安全**（@PreAuthorize + 属主校验）+ **OAuth2**（授权码模式 + 资源服务器）+ **工程清单**（BCrypt/限流/TLS/CORS）。"JWT 怎么接入 Security"与"越权怎么防"是两大必考。

---

**返回总览**：[00-SpringBoot总览与核心概念](00-SpringBoot总览与核心概念.md) | **上一篇**：[05-SpringBoot测试体系](05-SpringBoot测试体系.md) | **下一篇**：[07-SpringBoot生产运维与可观测](07-SpringBoot生产运维与可观测.md)
