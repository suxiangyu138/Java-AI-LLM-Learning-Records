# 02 - Token 存储与安全考量

> 🎯 Token 存储方式直接决定安全性 — localStorage 有 XSS 风险、Cookie HttpOnly 防 XSS 但不防 CSRF、内存存最安全但不持久，三种方案各有取舍

---

## 目录

1. [三种存储方式对比](#1-三种存储方式对比)
2. [Cookie HttpOnly + Secure 方案](#2-cookie-httponly--secure-方案)
3. [接口防重放](#3-接口防重放)
4. [安全清单](#4-安全清单)

---

## 1. 三种存储方式对比

| 存储位置 | XSS 风险 | CSRF 风险 | 持久化 | 适用 |
|----------|:---:|:---:|:---:|------|
| **localStorage** | ⚠️ 高（JS 可读） | ✅ 安全（不自动发送） | ✅ | 内部后台 |
| **Cookie HttpOnly** | ✅ 安全（JS 不可读） | ⚠️ 需防 CSRF | ✅ | ⭐ 推荐 |
| **内存（变量）** | ✅ 安全 | ✅ 安全 | ❌（刷新丢失） | SPA + 静默刷新 |

```javascript
// ❌ 不推荐 — 完全的 localStorage（XSS 攻击可窃取）
localStorage.setItem('accessToken', token);

// ⭐ 推荐 — Cookie HttpOnly + Secure
// 由后端 Set-Cookie，JS 完全无法访问
// Response Headers: Set-Cookie: token=xxx; HttpOnly; Secure; SameSite=Strict; Max-Age=900
```

---

## 2. Cookie HttpOnly + Secure 方案

```java
// ⭐ 后端设置安全 Cookie（推荐）
@PostMapping("/login")
public ResponseEntity<Result<?>> login(@RequestBody LoginDTO dto) {
    String accessToken = JwtUtil.createAccessToken(user.getId(), roles);

    ResponseCookie cookie = ResponseCookie.from("access_token", accessToken)
        .httpOnly(true)          // ⚠️ JS 不可读取（防 XSS）
        .secure(true)            // 仅 HTTPS 发送
        .sameSite("Strict")      // 防 CSRF（跨站不发 Cookie）
        .maxAge(900)             // 15 分钟
        .path("/")
        .build();

    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, cookie.toString())
        .body(Result.ok());
}
```

```text
Cookie 安全属性：

HttpOnly   → JS 无法读取 document.cookie → 防 XSS 窃取
Secure     → 仅 HTTPS 连接发送        → 防中间人截获
SameSite   → Strict/Lax/None          → 防 CSRF
  Strict: 任何跨站请求都不发送 Cookie（最安全）
  Lax:    GET 跨站请求可发送（默认，兼容性好）
  None:   所有跨站请求都发送（需配合 Secure）
```

---

## 3. 接口防重放

```java
// 方案：请求时间戳 + Nonce（一次性随机数）
// 前端在请求头带 X-Timestamp + X-Nonce

// 后端 Filter 校验
@Component
public class ReplayAttackFilter implements Filter {
    private final RedisTemplate<String, String> redis;

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) {
        HttpServletRequest request = (HttpServletRequest) req;
        String timestamp = request.getHeader("X-Timestamp");
        String nonce = request.getHeader("X-Nonce");

        // 1. 时间戳校验（±5 分钟有效）
        long diff = Math.abs(System.currentTimeMillis() - Long.parseLong(timestamp));
        if (diff > 5 * 60 * 1000) {
            throw new BizException(400, "请求已过期");
        }

        // 2. Nonce 校验（60 秒内不允许重复）— 仅敏感接口
        Boolean exist = redis.opsForValue()
            .setIfAbsent("nonce:" + nonce, "1", 60, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(exist)) {
            throw new BizException(400, "请求已处理过");
        }

        chain.doFilter(req, res);
    }
}
```

---

## 4. 安全清单

| 检查项 | 措施 | 优先级 |
|--------|------|:---:|
| 密码传输 | HTTPS + 前端先 SHA256 再传给后端 | 🔴 |
| 密码存储 | BCrypt（后端不可逆加密）| 🔴 |
| JWT 签名 | RS256（非对称，私钥签名+公钥验签）| 🔴 |
| Token 存储 | Cookie HttpOnly + Secure + SameSite | 🔴 |
| RefreshToken | Redis 存储，退出时删除 | 🟡 |
| Token 黑名单 | Redis Set — 强制下线 | 🟡 |
| 防暴力破解 | 登录接口限流 + 验证码 | 🟡 |
| 防重放 | Timestamp + Nonce（敏感接口）| 🟢 |
| 异常检测 | 异地登录/频繁失败 告警 | 🟢 |

> 🎯 **安全铁三角**：HTTPS（传输加密）+ Cookie HttpOnly Secure SameSite（存储安全）+ BCrypt（密码不可逆）。这三个到位，90% 的认证安全问题就解决了。
