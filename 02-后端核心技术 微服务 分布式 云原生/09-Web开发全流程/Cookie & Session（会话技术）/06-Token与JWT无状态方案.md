# 06-Token 与 JWT 无状态方案
> 无状态认证：JWT 结构与签名、双 Token 模式、与 Session 的对比选型——"服务器不存状态，客户端自己证明身份"

## 📚 目录
1. [为什么需要无状态方案](#1-为什么需要无状态方案)
2. [JWT 结构与签名](#2-jwt-结构与签名)
3. [双 Token 模式](#3-双-token-模式)
4. [JWT vs Session 全面对比](#4-jwt-vs-session-全面对比)
5. [JWT 的坑与安全实践](#5-jwt-的坑与安全实践)
6. [选型决策](#6-选型决策)
7. [核心要点](#7-核心要点)
8. [参考来源](#8-参考来源)

## 1. 为什么需要无状态方案

```text
Session 的痛点：
  ① 服务器存状态（内存/Redis）——存储成本
  ② 跨域麻烦（Cookie 三层配合）
  ③ 移动端/第三方不友好（无浏览器 Cookie 语义）

无状态方案（JWT）：
  服务器不存会话——客户端持有"自证明的令牌"
  令牌可验证（签名）→ 服务器验签即可信任
```

| 场景 | Session 的痛点 | JWT 的优势 |
|------|---------------|-----------|
| 分布式 | 需共享存储 | 天然无状态 |
| 跨域/移动端 | Cookie 限制多 | Header 携带，无 Cookie 依赖 |
| 微服务 | 每服务查会话 | 验签即可（共享密钥/公钥） |

> 🎯 **JWT 的本质**：**把"服务器记住你"变成"你自己证明你是谁"**——令牌签名保证不可伪造，服务器验签不查库。

## 2. JWT 结构与签名

```text
JWT = Header.Payload.Signature（三部分，Base64Url 编码）

Header：{"alg":"HS256","typ":"JWT"}
Payload：{"sub":"1001","name":"张三","exp":...,"iat":...}
Signature：HMAC-SHA256(header.payload, secret)

签名保证：内容不可篡改（改一个字符 → 验签失败）
```

| 部分 | 内容 | 说明 |
|------|------|------|
| Header | 算法/类型 | alg（HS256/RS256） |
| Payload | 声明（claim） | sub/exp/iat/自定义 |
| Signature | 签名 | 防篡改（核心） |

```text
⚠️ Payload 只是 Base64 编码，不是加密！
  任何人都能解码看内容（JWT 不保密数据）
  敏感信息不能放 Payload（只能放"可公开+可验证"的信息）
```

> ⚠️ **JWT 的经典误解**："JWT 加密了"——**没有**；Payload 是 Base64 可解码。**JWT 保证的是"完整性"（不可篡改），不是"机密性"**。

## 3. 双 Token 模式

```text
为什么需要双 Token：
  Access Token 泄露风险高（频繁传输）
  → 短效（15 分钟）+ Refresh Token 长效（7-30 天）

Access Token：短效（15m-1h），业务请求携带
Refresh Token：长效（7-30 天），仅用于换新 Access Token
```

```text
流程：
① 登录 → 颁发 Access Token（15m）+ Refresh Token（7d）
② 业务请求带 Access Token（验签通过即信任）
③ Access 过期 → 用 Refresh 换新 Access（服务器校验）
④ Refresh 过期 → 重新登录

Refresh Token 存储：服务器端（可撤销）或安全存储（客户端）
```

| 双 Token | Access | Refresh |
|----------|--------|---------|
| 有效期 | 15m-1h（短） | 7-30 天（长） |
| 用途 | 业务请求 | 换新 Access |
| 泄露损失 | 小（短效） | 大（需撤销机制） |
| 存储 | 客户端（内存/安全存储） | 服务端或安全存储 |

> 🎯 **双 Token 的价值**：**短效 Access 缩小泄露窗口 + Refresh 免频繁登录**——现代 Token 方案的标配（Dual-Token 模式）。

## 4. JWT vs Session 全面对比

| 维度 | Session + Redis | JWT |
|------|-----------------|-----|
| 状态存储 | 服务器（Redis） | 客户端（令牌） |
| 服务器成本 | 存储 + 查询 | 验签（CPU） |
| **可撤销** | ✅ 随时踢下线 | ❌ 天然不可撤销（等过期） |
| 跨域/移动端 | Cookie 限制 | Header 携带 ✅ |
| 数据大小 | SessionID 小 | 令牌可能较大（Payload 内容） |
| 安全 | 数据不暴露 | 签名防篡改，Payload 可解码 |
| 登出 | invalidate | 需黑名单/短效 |
| 微服务 | 共享存储 | 验签直通 |
| 适用 | **登录态/需要控制的会话** | 无状态 API/跨域/第三方 |

> 🎯 **核心差异一句话**：**Session 可撤销（服务器掌控），JWT 不可撤销（等过期）**——需要"随时踢人/权限变更即时生效"的业务，Session 更合适；纯无状态接口，JWT 更合适。

## 5. JWT 的坑与安全实践

| 坑 | 防护 |
|----|------|
| 密钥弱/泄露 | 强密钥（HS256 ≥ 256bit）；RS256 用私钥签发 |
| 算法混淆攻击 | 固定算法（服务端白名单 alg） |
| Payload 放敏感信息 | 只放可公开信息（ID/角色/过期） |
| 无过期/过期过长 | exp 必设 + 短效 Access |
| 泄露无法撤销 | 双 Token + Refresh 撤销 + 黑名单 |
| 存储不安全 | 客户端不存 localStorage（防 XSS）——存内存/HttpOnly Cookie |

```text
JWT 安全基线：
  ① 强密钥 + 固定算法（alg 白名单）
  ② exp/iat 必设（短效 Access）
  ③ Payload 不放敏感数据
  ④ 客户端存储：内存/HttpOnly Cookie（不用 localStorage）
  ⑤ 双 Token + 撤销机制（关键场景）
```

> ⚠️ **JWT 存储的争议点**：localStorage（XSS 可读）vs HttpOnly Cookie（防 XSS 但受 CSRF/CORS 限制）——**现代建议：HttpOnly Cookie 存 Refresh，内存存 Access**。

## 6. 选型决策

```text
选型决策树：
  需要"随时踢下线/权限即时生效"？
    ├─ 是 → Session + Redis（Spring Session）
    └─ 否 ↓
  跨域/移动端/第三方？
    ├─ 是 → JWT 双 Token
    └─ 否 ↓
  内部系统/管理端？
    └─ Session（简单可控）

2026 常见组合（混合）：
  登录态（前端 Web）：Session + Redis（可控可撤销）
  移动端 API：JWT 双 Token（无 Cookie 依赖）
  微服务间信任：JWT/内部 Token
```

| 场景 | 推荐 |
|------|------|
| 传统 Web 管理端 | Session + Redis |
| 前后端分离 SPA | Session（SameSite 配置）或 JWT |
| 移动端/第三方 | **JWT 双 Token** |
| 微服务间认证 | JWT（验签直通） |
| 需要强制下线 | Session（可撤销） |

## 7. 核心要点

> 🎯 **核心要点**：
> - JWT = Header.Payload.Signature：签名保证**完整性**，不保证机密性（Payload 可解码）；
> - 双 Token：短效 Access（缩小泄露窗口）+ 长效 Refresh（免频繁登录）；
> - 核心差异：**Session 可撤销 vs JWT 不可撤销**（等过期）；
> - JWT 安全基线：强密钥 + 固定算法 + 短效 + Payload 不放敏感 + 存储位置；
> - 选型：可控登录态用 Session，无状态接口用 JWT，微服务间信任用 JWT；
> - 2026 常态是**混合**（Session + JWT 并存按场景选）。

## 8. 参考来源

- [JWT 官方规范（RFC 7519）](https://www.rfc-editor.org/rfc/rfc7519)
- [OWASP：JWT 安全指南](https://cheatsheetseries.owasp.org/cheatsheets/JSON_Web_Token_for_Java_Cheat_Sheet.html)
- [Auth0：JWT 最佳实践](https://auth0.com/blog/a-look-at-the-latest-draft-for-jwt-bcp/)

---

**下一模块**：[07-生产实践与面试题](07-生产实践与面试题.md)　/　**返回总览**：[00-总览](00-会话技术总览.md)
