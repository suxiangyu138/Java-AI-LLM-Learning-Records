# Postman 认证与鉴权

> 🔐 Bearer Token、OAuth 2.0、JWT 自动续期、API Key、Basic Auth —— Postman 中 7 种认证方式完全攻略

---

## 📚 目录

1. [认证方式速览](#1-认证方式速览)
2. [Bearer Token](#2-bearer-token)
3. [OAuth 2.0 自动化](#3-oauth-20-自动化)
4. [JWT 自动续期](#4-jwt-自动续期)
5. [API Key](#5-api-key)
6. [Basic Auth / Digest Auth](#6-basic-auth--digest-auth)
7. [认证最佳实践](#7-认证最佳实践)

---

## 1. 认证方式速览

| 认证方式 | 配置复杂度 | 自动化 | 典型场景 |
|---------|:------:|:----:|---------|
| **No Auth** | - | - | 公开 API |
| **Bearer Token** | ⭐ | 手动 | RESTful API 最常用 |
| **OAuth 2.0** | ⭐⭐⭐ | ✅ 内置自动刷新 | 第三方登录 |
| **API Key** | ⭐ | 手动 | 开放平台（Header/Query） |
| **Basic Auth** | ⭐ | 手动 | 内部系统/调试 |
| **Digest Auth** | ⭐⭐ | 手动 | 需要更高安全性的 HTTP 认证 |
| **AWS Signature** | ⭐⭐⭐ | ✅ 内置 | AWS API Gateway |

---

## 2. Bearer Token

```text
配置方式（最简单）：

  Authorization Tab → Type: Bearer Token
  → Token 栏填写：{{authToken}}

  自动附加请求头：
  Authorization: Bearer {{authToken}}

常见流程：
  1. 登录接口 → Tests 脚本提取 token
  2. 存到 Collection Variables
  3. 其他请求引用 {{authToken}}
  4. token 过期 → 重新登录刷新
```

```javascript
// ===== 登录请求 Tests（提取 Token）=====
let response = pm.response.json();
pm.collectionVariables.set("authToken", response.data.accessToken);
pm.collectionVariables.set("refreshToken", response.data.refreshToken);
```

---

## 3. OAuth 2.0 自动化

### 3.1 配置步骤

```text
Authorization Tab → Type: OAuth 2.0 → 右侧 Configure New Token

配置项：
  Token Name       → 自定义名称（如"Google API Token"）
  Grant Type       → Authorization Code / Client Credentials / Implicit
  Callback URL     → https://www.getpostman.com/oauth2/callback
  Auth URL         → https://accounts.google.com/o/oauth2/v2/auth
  Access Token URL → https://oauth2.googleapis.com/token
  Client ID        → 你的 Client ID
  Client Secret    → 你的 Client Secret
  Scope            → https://www.googleapis.com/auth/userinfo.profile
  State            → 随机字符串（防 CSRF）

→ 点击 "Get New Access Token"
→ 浏览器弹出授权页 → 用户授权 → Postman 自动获取 token
→ 后续请求自动使用该 token
```

### 3.2 自动刷新 Token

```text
Postman 内置 OAuth 2.0 Token 自动刷新：

  1. 检测 Access Token 即将过期（根据 expires_in）
  2. 自动用 Refresh Token 换取新 Access Token
  3. 无缝更新，无需手动干预

支持 Grant Type：
  ✅ Authorization Code + PKCE
  ✅ Client Credentials
  ✅ Implicit
```

---

## 4. JWT 自动续期

```javascript
// ===== Collection 级别 Pre-request Script =====
// 方案：每次请求前检查 token 是否过期，过期则自动刷新

let accessToken = pm.collectionVariables.get("accessToken");
let tokenExpiry = pm.collectionVariables.get("tokenExpiry");
let refreshToken = pm.collectionVariables.get("refreshToken");

// 如果 token 将在 60 秒内过期 → 自动刷新
if (!accessToken || Date.now() > tokenExpiry - 60000) {
    console.log("Token expired, refreshing...");

    pm.sendRequest({
        url: pm.variables.get("baseUrl") + "/auth/refresh",
        method: 'POST',
        header: { 'Content-Type': 'application/json' },
        body: {
            mode: 'raw',
            raw: JSON.stringify({ refreshToken: refreshToken })
        }
    }, (err, res) => {
        if (!err && res.code === 200) {
            let data = res.json().data;
            pm.collectionVariables.set("accessToken", data.accessToken);
            pm.collectionVariables.set("refreshToken", data.refreshToken);
            pm.collectionVariables.set("tokenExpiry", Date.now() + data.expiresIn * 1000);
            console.log("Token refreshed successfully");
        } else {
            console.error("Token refresh failed:", err || res.status);
        }
    });
}
```

---

## 5. API Key

```text
两种传递方式：

  1. Header 传递（最常见）：
     Authorization Tab → Type: API Key
     → Key: X-API-Key
     → Value: {{apiKey}}
     → Add to: Header

     实际发送：
     X-API-Key: abc123xyz

  2. Query Params 传递（部分开放平台）：
     → Add to: Query Params
     → 实际发送：
     GET /users?api_key=abc123xyz
```

---

## 6. Basic Auth / Digest Auth

```text
Basic Auth：
  → Base64 编码 username:password
  → Authorization: Basic dXNlcjpwYXNz

  填写 Username / Password，Postman 自动生成 Header
  ⚠️ 明文传输（仅 Base64 编码）→ 必须配 HTTPS！

Digest Auth：
  → 服务端发送 nonce → 客户端 hash(username:realm:password:nonce:...)
  → 比 Basic 安全，密码不直接传输
  → Postman 自动处理 nonce 和 hash 计算
```

---

## 7. 认证最佳实践

```text
✅ 使用集合级 Authorization
   → 整个 Collection 的请求共用同一认证方式
   → 需要特殊认证的单个请求可覆盖

✅ Token 放 Collection Variables
   → 登录获取 token → 脚本存到 Collection Variables
   → 其他请求统一用 {{authToken}}

✅ 敏感信息放 Environment 变量
   → password, client_secret, api_key → Environment
   → 不放入 Collection JSON 导出文件

✅ Pre-request Script 处理 Token 过期
   → 每次请求前检查过期时间
   → 过期自动调用 refresh 接口

✅ 不同环境不同认证
   → Dev 环境：简单 Basic Auth
   → Prod 环境：OAuth 2.0 + JWT
   → 切换环境时认证自动切换
```

| 最佳实践 | 做法 |
|-----|------|
| **集合级认证** | Collection Authorization 统一设置，单个请求可覆盖 |
| **Token 集中管理** | 登录获取的 token 存 Collection Variables |
| **敏感信息隔离** | password/secret 放 Environment，不导出 |
| **自动续期** | Pre-request Script 检查过期 → pm.sendRequest 刷新 |
| **环境隔离** | Dev 用 Basic，Prod 用 OAuth2 |

---

> 🎯 **核心要点**：大部分场景用 **Bearer Token + 自动续期脚本** 就够了；第三方集成用 **OAuth 2.0**（Postman 原生支持自动刷新）；开放平台用 **API Key**。

---

*创建于：2026年7月*
