# 01 - JWT 登录认证完整流程

> 🎯 JWT 是无状态认证的标准方案 — 理解从前端点登录按钮到后端返回 Token、前端存储到每次请求携带的完整闭环

---

## 目录

1. [认证流程全景](#1-认证流程全景)
2. [前端代码实现](#2-前端代码实现)
3. [后端代码实现](#3-后端代码实现)

---

## 1. 认证流程全景

```text
一次完整的登录 → 请求 → 退出流程：

  前端                             后端
  ┌──────────┐                   ┌──────────┐
  │ 登录页面  │ ──POST /auth/login──→│    认证    │
  │          │                   │验证用户名密码│
  │          │←─{accessToken, refreshToken}─│
  │ 存储Token │                   └──────────┘
  │          │
  │ API 请求  │ ──GET /api/users──→┌──────────┐
  │ Authorization: Bearer xxx │    │  JWT验签   │
  │          │←──200 { data } ────│ 业务处理    │
  │          │                   └──────────┘
  │          │
  │ Token过期 │ ──GET /api/users──→ 返回 401
  │          │
  │ 刷新Token│ ──POST /auth/refresh──→ 生成新Token
  │          │←──{accessToken}────
  │ 重试请求  │ ──GET /api/users──→  200
  │          │
  │   退出   │  清除本地Token
  └──────────┘
```

---

## 2. 前端代码实现

```javascript
// ═══ auth.js — 认证模块 ═══
import api from './api';

// 登录
export async function login(username, password) {
  const { data } = await api.post('/auth/login', { username, password });
  // data = { accessToken: "...", refreshToken: "..." }
  localStorage.setItem('accessToken', data.accessToken);
  localStorage.setItem('refreshToken', data.refreshToken);
  return data;
}

// 刷新 Token
export async function refreshToken() {
  const refreshToken = localStorage.getItem('refreshToken');
  if (!refreshToken) throw new Error('无 RefreshToken');
  const { data } = await api.post('/auth/refresh', { refreshToken });
  localStorage.setItem('accessToken', data.accessToken);
  return data.accessToken;
}

// 退出
export function logout() {
  localStorage.removeItem('accessToken');
  localStorage.removeItem('refreshToken');
  router.push('/login');
}
```

```javascript
// ⭐ axios 拦截器 — 自动刷新 Token
let isRefreshing = false;
let refreshQueue = [];

api.interceptors.request.use(config => {
  const token = localStorage.getItem('accessToken');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

api.interceptors.response.use(
  response => response,
  async error => {
    const { config, response } = error;
    if (response?.status !== 401 || config._retry) return Promise.reject(error);

    // Token 过期 → 刷新
    if (!isRefreshing) {
      isRefreshing = true;
      try {
        const newToken = await refreshToken();
        refreshQueue.forEach(cb => cb(newToken));
        refreshQueue = [];
        config.headers.Authorization = `Bearer ${newToken}`;
        return api(config);    // 重试原请求
      } catch (err) {
        logout();
        return Promise.reject(err);
      } finally {
        isRefreshing = false;
      }
    }

    // 其他 401 请求排队等刷新结果
    return new Promise(resolve => {
      refreshQueue.push(newToken => {
        config.headers.Authorization = `Bearer ${newToken}`;
        resolve(api(config));
      });
    });
  }
);
```

---

## 3. 后端代码实现

```java
@RestController
@RequestMapping("/auth")
public class AuthController {

    @PostMapping("/login")
    public Result<TokenVO> login(@RequestBody @Valid LoginDTO dto) {
        // 1. 验证用户名密码
        User user = userService.authenticate(dto.getUsername(), dto.getPassword());

        // 2. 生成双Token
        String accessToken = JwtUtil.createAccessToken(user.getId(), user.getRoles());
        String refreshToken = JwtUtil.createRefreshToken(user.getId());

        // 3. 存储 RefreshToken（Redis）
        redisTemplate.opsForValue()
            .set("refresh:" + user.getId(), refreshToken, 7, TimeUnit.DAYS);

        return Result.ok(new TokenVO(accessToken, refreshToken));
    }

    @PostMapping("/refresh")
    public Result<TokenVO> refresh(@RequestBody RefreshDTO dto) {
        Claims claims = JwtUtil.parseToken(dto.getRefreshToken());
        Long userId = Long.parseLong(claims.getSubject());

        // 验证 Redis 中的 RefreshToken
        String stored = redisTemplate.opsForValue().get("refresh:" + userId);
        if (!dto.getRefreshToken().equals(stored)) {
            throw new BizException(401, "RefreshToken 无效");
        }

        User user = userService.getById(userId);
        String newAccessToken = JwtUtil.createAccessToken(user.getId(), user.getRoles());
        return Result.ok(new TokenVO(newAccessToken, null));
    }

    @DeleteMapping("/logout")
    public Result<?> logout(@RequestHeader("Authorization") String token) {
        Long userId = JwtUtil.getUserId(token);
        redisTemplate.delete("refresh:" + userId);     // 删除 RefreshToken
        return Result.ok();
    }
}
```

### Token 时效配置

```java
public class JwtUtil {
    private static final long ACCESS_EXPIRE = 15 * 60 * 1000;     // 15 分钟
    private static final long REFRESH_EXPIRE = 7 * 24 * 3600 * 1000; // 7 天

    public static String createAccessToken(Long userId, List<String> roles) {
        return Jwts.builder()
            .subject(userId.toString())
            .claim("roles", roles)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + ACCESS_EXPIRE))
            .signWith(getPrivateKey())
            .compact();
    }
}
```

> 🎯 **双Token策略**：accessToken 短时效(15min) + refreshToken 长时效(7天)。401 → 前端拦截器自动刷新 → 无缝续期。用户感知不到过期。
