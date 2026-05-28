# OAuth2 + JWT 认证与授权实战

## 基础概念

- **认证（Authentication）**：你是谁？→ 登录
- **授权（Authorization）**：你能干什么？→ 权限控制

## OAuth2 四种模式

| 模式 | 场景 | 流程 |
|------|------|------|
| 授权码模式 | 有后端的 Web 应用 | 最安全，前后端分离首选 |
| 密码模式 | 自家 App/客户端 | 直接传用户名密码换 Token（已不推荐） |
| 客户端模式 | 服务间调用 | client_id + client_secret 换 Token |
| 隐式模式 | 纯前端 SPA | 不安全，已被 PKCE 替代 |

## JWT 结构

JSON Web Token 由三部分组成，Base64 编码，用 `.` 分隔：

```
Header.Payload.Signature
```

- **Header**：`{"alg": "HS256", "typ": "JWT"}`
- **Payload**：`{"sub": "123", "name": "zhangsan", "iat": 1516239022, "exp": 1516242622}`
- **Signature**：对 Header + Payload 用密钥签名，防篡改

**重要**：JWT 的 Payload 只是 Base64 编码，不是加密。不要存密码等敏感信息。

## Spring Security + JWT 实战

### Token 生成

```java
@Component
public class JwtUtils {
    @Value("${jwt.secret}")
    private String secret;

    public String generateToken(String username) {
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 7 * 24 * 3600 * 1000))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes()))
                .compact();
    }

    public String getUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean validate(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(secret.getBytes()))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
```

### 登录接口

```java
@PostMapping("/login")
public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
    // 1. 校验用户名密码
    UsernamePasswordAuthenticationToken authToken =
            new UsernamePasswordAuthenticationToken(dto.getUsername(), dto.getPassword());
    Authentication auth = authenticationManager.authenticate(authToken);

    // 2. 生成 token
    String token = jwtUtils.generateToken(dto.getUsername());

    return Result.ok(new LoginVO(token));
}
```

### JWT 过滤器

```java
@Component
public class JwtFilter extends OncePerRequestFilter {
    @Autowired
    private JwtUtils jwtUtils;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (jwtUtils.validate(token)) {
                String username = jwtUtils.getUsername(token);
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(username, null, List.of());
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        chain.doFilter(request, response);
    }
}
```

## JWT vs Session

| 特性 | JWT | Session |
|------|-----|--------|
| 存储位置 | 客户端 | 服务端 |
| 水平扩展 | 天然支持（无状态） | 需要共享 Session（Redis） |
| 主动失效 | 困难（需黑名单） | 简单（删除 Session） |
| 体积 | 较大（每次请求携带） | 仅 SessionID |
| 适用 | 微服务/分布式 | 单体应用 |

## 权限控制

```java
// 方法级权限
@PreAuthorize("hasRole('ADMIN')")
@DeleteMapping("/users/{id}")
public Result<?> delete(@PathVariable Long id) { }

@PreAuthorize("hasAuthority('user:write')")
@PostMapping("/users")
public Result<?> create(@Valid @RequestBody UserDTO dto) { }
```

启用注解支持：
```java
@Configuration
@EnableMethodSecurity  // Spring Security 6+
public class SecurityConfig { }
```

## 生产实践

- **Token 有效期**：Access Token 短一些（15-30分钟），配 Refresh Token 续期
- **密钥管理**：`jwt.secret` 不要硬编码，放环境变量或配置中心
- **黑名单**：登出时把 Token 加入 Redis 黑名单，有效期设为 Token 剩余时长
- **HTTPS 必须**：Token 明文传输被截获则安全归零
