# 11 - 微服务安全：OAuth2 与 JWT 认证

> 🎯 微服务安全的核心是"无状态认证" — JWT 自包含令牌 + OAuth2 授权框架 + Spring Security，构建从网关到服务的全链路认证体系

---

## 目录

1. [微服务认证方案对比](#1-微服务认证方案对比)
2. [JWT 原理与实战](#2-jwt-原理与实战)
3. [OAuth2 授权码模式](#3-oauth2-授权码模式)
4. [网关统一鉴权](#4-网关统一鉴权)
5. [Token 传递与刷新](#5-token-传递与刷新)

---

## 1. 微服务认证方案对比

| 方案 | 原理 | 优点 | 缺点 | 适用 |
|------|------|------|------|------|
| **Session + Cookie** | 服务端存 Session | 简单、安全 | 不适用于分布式 | ❌ 微服务不推荐 |
| **JWT 无状态** | 客户端持有签名 Token | 无状态、跨服务 | Token 无法主动失效 | ⭐ 内部微服务 |
| **OAuth2 + JWT** | 授权码 + JWT | 标准、第三方接入 | 配置复杂 | ⭐ 对外开放 API |
| **API Key** | 固定密钥 | 极简 | 不安全 | 内部工具 |

---

## 2. JWT 原理与实战

### 2.1 JWT 结构

```text
JWT = Header.Payload.Signature

Header:   {"alg": "RS256", "typ": "JWT"}
Payload:  {"sub": "10086", "name": "张三", "roles": ["USER"], "exp": 1705315200}
Signature: RSASHA256(base64(Header) + "." + base64(Payload), privateKey)

⚠️ Payload 是 Base64 编码，不是加密！不要放敏感信息（如密码）
```

### 2.2 Spring Security + JWT 配置

```java
// ⭐ JWT 工具类
public class JwtUtil {
    private static final KeyPair keyPair = Keys.keyPairFor(SignatureAlgorithm.RS256);

    public static String createToken(Long userId, String username, List<String> roles) {
        return Jwts.builder()
            .subject(userId.toString())
            .claim("username", username)
            .claim("roles", roles)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + 7200000)) // 2h
            .signWith(keyPair.getPrivate())
            .compact();
    }

    public static Claims parseToken(String token) {
        return Jwts.parser()
            .verifyWith(keyPair.getPublic())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}
```

### 2.3 资源服务器配置

```java
@Configuration
@EnableWebSecurity
public class ResourceServerConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter())))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .build();
    }
}
```

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://auth-server:9000   # 验证 JWT 签名
```

---

## 3. OAuth2 授权码模式

```text
OAuth2 四种模式：
├── 授权码模式（Authorization Code）— ⭐ 最安全，Web 前后端分离
├── 密码模式（Password）— 不推荐，已废弃
├── 客户端模式（Client Credentials）— 服务间调用
└── 简化模式（Implicit）— 不推荐，已废弃

推荐组合：
  Web 前端：授权码 + PKCE
  移动端：授权码 + PKCE
  服务间：Client Credentials
```

### 授权码流程

```text
1. 用户 → 前端 → 跳转 auth-server/authorize?response_type=code&...
2. 用户登录授权 → auth-server 返回授权码 code
3. 前端拿到 code → 后端 /oauth2/token?code=xxx → 换取 access_token
4. 后续请求 → Authorization: Bearer <access_token>
```

```yaml
# Spring Authorization Server
spring:
  security:
    oauth2:
      authorizationserver:
        issuer: http://localhost:9000
        client:
          gateway-client:
            registration:
              client-id: gateway
              client-secret: "{noop}secret"
              authorization-grant-types: authorization_code,refresh_token
              redirect-uris: http://localhost:8080/login/oauth2/code/gateway
              scopes: openid,profile,read,write
```

---

## 4. 网关统一鉴权

```java
// Gateway Filter — 只验签，不查库
@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        // 白名单
        if (path.startsWith("/api/auth/") || path.startsWith("/api/public/")) {
            return chain.filter(exchange);
        }

        String token = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            return unauthorized(exchange, "缺少令牌");
        }

        try {
            Claims claims = JwtUtil.parseToken(token.substring(7));
            // 注入用户信息到 Header → 下游服务直接使用
            ServerHttpRequest request = exchange.getRequest().mutate()
                .header("X-User-Id", claims.getSubject())
                .header("X-Username", claims.get("username", String.class))
                .header("X-Roles", String.join(",", claims.get("roles", List.class)))
                .build();
            return chain.filter(exchange.mutate().request(request).build());
        } catch (JwtException e) {
            return unauthorized(exchange, "令牌无效或已过期");
        }
    }

    @Override
    public int getOrder() { return -200; }
}
```

---

## 5. Token 传递与刷新

### 服务间 Token 透传

```java
// Feign 拦截器 — 传递当前请求的 Token
@Component
public class TokenRelayInterceptor implements RequestInterceptor {
    @Override
    public void apply(RequestTemplate template) {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes) {
            String token = ((ServletRequestAttributes) attrs)
                .getRequest().getHeader("Authorization");
            if (token != null) {
                template.header("Authorization", token);
            }
        }
    }
}
```

### Token 刷新策略

```text
双 Token 策略：
  access_token: 短期有效（15min-2h），用于 API 调用
  refresh_token: 长期有效（7-30天），用于刷新 access_token

刷新时机：
  → 收到 401 → 用 refresh_token 换新 access_token
  → 刷新失败 → 跳转登录页
```

> 🎯 **微服务安全铁律**：JWT 不存密码、access_token 短时效、refresh_token 安全存储、网关统一验签、服务间 Feign 拦截器透传 Token。
