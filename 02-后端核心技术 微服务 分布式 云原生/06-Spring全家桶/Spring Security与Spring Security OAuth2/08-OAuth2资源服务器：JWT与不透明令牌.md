# 08 OAuth2 资源服务器：JWT 与不透明令牌

> 资源服务器是"校验令牌、放行受保护 API"的一方：JWT 本地无状态校验（jwk-set-uri）、7.0 的 typ 头默认校验、多租户/多签发方配置，以及不透明令牌的 introspection 方案——微服务 API 安全的核心落点

---

## 📚 目录

1. [资源服务器的职责与两种令牌形态](#1-资源服务器的职责与两种令牌形态)
2. [JWT 资源服务器：最小配置](#2-jwt-资源服务器最小配置)
3. [JwtDecoder 校验链：7.0 的默认收紧](#3-jwtdecoder-校验链70-的默认收紧)
4. [权限映射：scope 与角色](#4-权限映射scope-与角色)
5. [多签发方与多租户](#5-多签发方与多租户)
6. [不透明令牌：Token Introspection](#6-不透明令牌token-introspection)
7. [常见坑与排查](#7-常见坑与排查)

---

## 1. 资源服务器的职责与两种令牌形态

```text
客户端 → 资源服务器（我们的 API）
          ├── JWT：本地校验签名+过期 → 无需联网（无状态、高性能）
          └── 不透明令牌：回调授权服务器 introspection 端点 → 联网校验
```

| 维度 | JWT | 不透明令牌 |
|------|:---:|:---:|
| 校验方式 | 本地（签名 + exp） | 远程 introspection |
| 性能 | 高（无网络开销） | 低（每次联网） |
| 状态性 | 无状态 | 授权服务器可即时撤销 |
| 信息携带 | 自含 claims | 需 introspection 响应 |
| 适用 | 高频 API、微服务内部 | 强撤销要求、第三方令牌 |

> 🎯 **要点**：JWT 是 2026 年绝对主流（自建授权服务器默认发 JWT）；不透明令牌用于"必须能即时踢人"的场景。两者在 Spring Security 里都是 `oauth2ResourceServer` 的一个开关。

## 2. JWT 资源服务器：最小配置

### 2.1 依赖与配置

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
```

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://auth.example.com        # 方式一：从 issuer 自动发现 jwks
          # 或显式指定公钥端点：
          # jwk-set-uri: https://auth.example.com/.well-known/jwks.json
```

### 2.2 安全链

```java
@Bean
SecurityFilterChain apiChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())                      // JWT 无 Cookie 会话
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .oauth2ResourceServer(rs -> rs
            .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/public/**").permitAll()
            .requestMatchers("/api/orders/**").hasAuthority("SCOPE_order:read")
            .anyRequest().authenticated());
    return http.build();
}
```

### 2.3 请求侧

```java
// 控制器拿当前令牌身份
@GetMapping("/api/me")
public Map<String, Object> me(@AuthenticationPrincipal Jwt jwt) {
    return Map.of(
        "user", jwt.getSubject(),
        "scopes", jwt.getClaimAsStringList("scope")
    );
}

// 或注入 Authentication
@GetMapping("/api/orders")
public List<Order> orders(Authentication auth) {
    Jwt jwt = (Jwt) auth.getPrincipal();
    ...
}
```

## 3. JwtDecoder 校验链：7.0 的默认收紧

### 3.1 校验步骤（NimbusJwtDecoder 默认）

```text
收到 Bearer JWT →
  ① 结构解析（header.payload.signature 三段）
  ② 签名校验（jwk-set 公钥）
  ③ 过期校验（exp claim）
  ④ 生效时间校验（nbf claim）
  ⑤ 签发方校验（iss claim 与配置匹配）
  ⑥ 受众校验（aud claim 包含本服务资源 ID）★ 7.0 默认
  ⑦ typ 头校验（typ == JWT）★ 7.0 默认
```

### 3.2 7.0 新增的默认校验

| 校验 | 6.x | 7.0 | 风险（不校验时） |
|------|:---:|:---:|----------------|
| `typ` 头校验 | 不强制 | **默认校验** | 攻击者把非 JWT 令牌（如 JWE）当 JWT 解析的混淆攻击 |
| `aud` 受众校验 | 需手动 | 需手动（配置资源 ID） | 令牌被其它资源服务器复用（越权） |

```java
// 显式配置受众（推荐开启）
@Bean
JwtDecoder jwtDecoder(JwtDecoder jwtDecoder) {
    OAuth2TokenValidator<Jwt> audienceValidator =
            new JwtClaimValidator<List<String>>("aud", aud -> aud != null && aud.contains("order-service"));
    OAuth2TokenValidator<Jwt> validators =
            new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefault(), audienceValidator);
    ((NimbusJwtDecoder) jwtDecoder).setJwtValidator(validators);
    return jwtDecoder;
}
```

> ⚠️ **6.x 迁移注意**：6.x 里很多项目为了兼容手动关掉了 typ 校验（`setJwtValidator` 覆盖）——7.0 默认开启后，**工作区配置会被默认校验覆盖**，先测再升级。

### 3.3 手动构建 JwtDecoder（多场景）

```java
@Bean
JwtDecoder jwtDecoder() {
    return NimbusJwtDecoder
            .withJwkSetUri("https://auth.example.com/.well-known/jwks.json")
            .build();
}
```

## 4. 权限映射：scope 与角色

JWT 的 `scope` claim 是授权服务器给的权限清单，默认映射为 `SCOPE_xxx` 权限：

```java
// 默认：scope: order:read → SCOPE_order:read
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/orders/**").hasAuthority("SCOPE_order:read"));

// 自定义映射：scope → 业务角色
@Bean
Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
    JwtGrantedAuthoritiesConverter scopes = new JwtGrantedAuthoritiesConverter();
    scopes.setAuthorityPrefix("");        // 去掉 SCOPE_ 前缀

    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(jwt -> {
        Collection<GrantedAuthority> authorities = new ArrayList<>(scopes.convert(jwt));
        // 从自定义 claim 映射角色：realm_access.roles → ROLE_xxx
        Map<String, Object> realm = jwt.getClaimAsMap("realm_access");
        if (realm != null && realm.get("roles") instanceof List<?> roles) {
            roles.forEach(r -> authorities.add(new SimpleGrantedAuthority("ROLE_" + r)));
        }
        return authorities;
    });
    return converter;
}
```

| 映射策略 | 适用 |
|---------|------|
| `SCOPE_` 前缀（默认） | 权限模型 = scope（推荐，最清晰） |
| 角色映射（realm_access 等） | 对接 Keycloak/自建授权服务器的角色模型 |
| 自定义 claim | 业务自定义权限载体 |

> 💡 **设计建议**：API 授权一律走 scope（与 OAuth2 语义对齐）；角色映射仅在与既有系统对接时使用。

## 5. 多签发方与多租户

### 5.1 多签发方：一个资源服务器信任多个授权服务器

```java
// Spring Security 7 多签发方支持（multi-tenancy）
JwtDecoder multiIssuer = JwtDecoders.fromIssuerLocations(
        "https://auth-company-a.example.com",
        "https://auth-company-b.example.com"
);
```

### 5.2 多租户：按请求头路由签发方

```java
// 按 X-Tenant 头选择 issuer（租户隔离的常见做法）
@Bean
Converter<HttpServletRequest, JwtDecoder> tenantAwareDecoder() {
    Map<String, JwtDecoder> decoders = Map.of(
        "tenant-a", JwtDecoders.fromIssuerLocation("https://auth-a.example.com"),
        "tenant-b", JwtDecoders.fromIssuerLocation("https://auth-b.example.com")
    );
    return request -> {
        String tenant = request.getHeader("X-Tenant");
        JwtDecoder decoder = decoders.get(tenant);
        if (decoder == null) throw new IllegalStateException("unknown tenant");
        return decoder;
    };
}
```

> ⚠️ **多租户注意**：按请求头路由**必须配合路径/域名隔离校验**（防止租户 A 的请求头冒充租户 B）；iss 校验始终开启——解码器路由只是第一步，签名与 issuer 校验仍按各租户独立执行。

## 6. 不透明令牌：Token Introspection

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        opaque-token:
          introspection-uri: https://auth.example.com/oauth2/introspect
          client-id: order-service
          client-secret: secret
```

```java
http.oauth2ResourceServer(rs -> rs
        .opaqueToken(opaque -> opaque
                .introspectionUri("https://auth.example.com/oauth2/introspect")
                .introspectionClientCredentials(new UsernamePasswordAuthenticationToken("order-service", "secret"))));

// 控制器：拿到 introspect 响应
@GetMapping("/api/me")
public Map<String, Object> me(@AuthenticationPrincipal OAuth2AuthenticatedPrincipal principal) {
    return principal.getAttributes();    // 授权服务器返回的用户信息
}
```

| 要点 | 说明 |
|------|------|
| 每次校验联网 | 性能敏感场景优先 JWT |
| 即时撤销 | 授权服务器删 token → introspection 立即失败 |
| 缓存 | 可对 introspection 结果做短 TTL 缓存降载（需接受撤销延迟） |
| 凭据保护 | introspection client 凭据仅服务端持有 |

## 7. 常见坑与排查

| 坑 | 现象 | 处理 |
|----|------|------|
| 令牌在 jwt.io 能解、服务端 401 | 签名不匹配 | 确认 jwk-set-uri/公钥一致、算法一致（RS256 vs HS256） |
| 升级 7.0 后有效令牌 401 | typ 校验拒绝 | 检查签发方是否输出非标准 typ（需签发方修正） |
| 401 vs 403 分不清 | 认证失败/授权失败 | 无令牌或坏令牌=401（EntryPoint）；有令牌无权限=403 |
| 跨服务令牌复用 | 一个令牌到处用 | 配置 aud 校验（资源 ID 白名单） |
| 时钟偏差 | 令牌"提前过期" | 授权/校验两端 NTP 对齐；Nimbus 有 leeway 可配 |
| 多签发方漏配 | 某租户登录全挂 | 逐签发方测试解码链 |
| scope 映射不生效 | hasAuthority("SCOPE_...") 恒失败 | 检查 JWT 里 claim 名（scope vs scp） |

> 🎯 **核心要点**：资源服务器 = 令牌校验 + 权限映射 + 授权规则。JWT 走"本地校验链"（签名/过期/iss/typ/aud——7.0 默认收紧 typ），不透明令牌走"远程 introspection"。权限模型首选 scope（SCOPE_xxx）；多租户按请求路由解码器但必须保 iss 校验。**401 是"你是谁"的问题，403 是"你能干什么"的问题**——排障先分清。

---

**上一模块**：[07-OAuth2客户端：登录、授权码与PKCE](07-OAuth2客户端：登录、授权码与PKCE.md)　**下一模块**：[09-授权服务器：Spring Authorization Server深入](09-授权服务器：Spring Authorization Server深入.md)
