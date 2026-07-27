# 03 - 单点登录 SSO 与 OAuth2 社交登录

> 🎯 单点登录（SSO）和社交登录（微信/Google）都基于 Token 机制 — SSO 是同组织内的统一认证，OAuth2 是跨组织的授权协议

---

## 目录

1. [单点登录 SSO](#1-单点登录-sso)
2. [OAuth2 社交登录流程](#2-oauth2-社交登录流程)
3. [JWT SSO vs CAS SSO](#3-jwt-sso-vs-cas-sso)

---

## 1. 单点登录 SSO

```text
SSO 核心流程（JWT 方案）：

  1. 用户访问 app1.example.com → 无 Token → 重定向到 sso.example.com/login
  2. 用户在 SSO 登录 → 返回 Token → 重定向回 app1
  3. 用户访问 app2.example.com → 无 Token → 重定向到 sso.example.com
  4. SSO 已有登录态 → 直接返回 Token → 重定向回 app2

关键：所有应用的认证都委托给 SSO 中心
```

```text
SSO Token 传递方案：

方案1：重定向 URL 参数
  app1.example.com?token=xxx  ← ⚠️ Token 暴露在 URL（不安全）

方案2：SSO 中心 Set-Cookie（同域名）
  SSO → Set-Cookie: token=xxx; Domain=.example.com
  → app1.example.com 和 app2.example.com 都能读取 ← ⭐ 推荐同域名

方案3：OAuth2 Authorization Code
  标准流程，适合跨域名场景
```

---

## 2. OAuth2 社交登录流程

```text
微信/Google/GitHub 登录流程：

  1. 用户点"微信登录"
  2. 跳转微信授权页 → 用户同意授权
  3. 微信回调 → 带上 authorization_code
  4. 后端用 code 换 access_token（向微信服务器请求）
  5. 后端用 access_token 获取用户信息（openid/头像/昵称）
  6. 后端创建或匹配本地用户 → 生成自己系统的 JWT → 返回前端
```

```java
@GetMapping("/oauth/callback/{provider}")
public ResponseEntity<?> oauthCallback(@PathVariable String provider,
                                       @RequestParam String code) {
    // 1. 用 code 换 access_token
    String accessToken = oauthService.exchangeCode(provider, code);

    // 2. 获取第三方用户信息
    OAuthUserInfo userInfo = oauthService.getUserInfo(provider, accessToken);
    // { openId: "oxxx", nickname: "张三", avatar: "https://..." }

    // 3. 绑定或创建本地用户
    User user = userService.findOrCreateByOpenId(provider, userInfo.getOpenId());
    user.setNickname(userInfo.getNickname());

    // 4. 生成自己的 JWT
    String jwt = JwtUtil.createAccessToken(user.getId(), user.getRoles());

    // 5. 重定向回前端（带 Token）
    return ResponseEntity.status(302)
        .location(URI.create("http://localhost:3000/oauth/callback?token=" + jwt))
        .build();
}
```

### OAuth2 核心角色

| 角色 | 说明 | 示例 |
|------|------|------|
| **Resource Owner** | 用户（资源拥有者） | 你 |
| **Client** | 第三方应用 | 你的 App |
| **Authorization Server** | 授权服务器 | 微信开放平台 |
| **Resource Server** | 资源服务器 | 微信用户信息 API |

---

## 3. JWT SSO vs CAS SSO

| 维度 | JWT SSO | CAS（传统 SSO） |
|------|---------|-----------------|
| 工作方式 | Token 自包含（客户端持有） | Ticket 票据（服务端验证） |
| 认证中心压力 | 低（发 Token 后不管） | 高（每次需验证 Ticket） |
| 单点退出 | 困难（Token 无法主动失效） | ✅ 容易（中心化控制） |
| 横向扩展 | ✅ 无状态易扩展 | ⚠️ 中心节点压力 |
| 适用 | 微服务/前后端分离 | 传统单体/内网系统 |

```text
SLO（Single Logout — 单点退出）的 JWT 方案：

JWT 本身无法主动失效 → 需要配合 Redis 黑名单

  退出流程：
    1. 前端清除本地 Token
    2. 后端将 accessToken 加入 Redis 黑名单（TTL = Token 剩余有效期）
    3. 网关 Filter 检查 Token 是否在黑名单中

  → 虽然 Token 签名仍有效，但网关层拦截
```

> 🎯 **选型**：内部微服务 SSO → JWT + 双Token + Redis 黑名单；跨组织授权 → OAuth2 标准流程；传统内网系统 → CAS（已有基础设施）。社交登录本质上就是 OAuth2。
