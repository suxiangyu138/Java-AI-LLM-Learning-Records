# Token 知识体系总览
> 一句话定位：前后端协作的鉴权凭证全流程——从登录拿到 Token、前端存储携带、过期刷新到安全防护；2026 年的正确姿势是「Web 用会话、API 用 JWT、短时访问令牌 + 刷新令牌轮换」。

## 📚 目录
1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [学习路线推荐](#3-学习路线推荐)
4. [核心概念速查](#4-核心概念速查)
5. [常见误区](#5-常见误区)
6. [一周计划](#6-一周计划)
7. [自测题](#7-自测题)
8. [参考来源](#8-参考来源)

## 1. 知识体系导图

```text
Token（前端鉴权凭证全流程）
├── 01 是什么（Token家族/session vs JWT本质/2026基线）
├── 02 JWT 原理与验证（三段结构/签名算法/验证五要点）
├── 03 登录认证流程（登录→存储→携带→过期→刷新→登出闭环）
├── 04 存储与传输（localStorage vs Cookie/HttpOnly/Authorization头）
├── 05 双 Token 模式（access+refresh/TTL设计/轮换与重用检测）
├── 06 会话与 Token 选型（决策框架/混合方案）
├── 07 单点登录与第三方登录（SSO/OAuth2.1+PKCE/OIDC）
├── 08 前端集成实战（axios拦截器/401刷新重试/多tab同步）
├── 09 安全红线与攻防（XSS/CSRF/泄露面/DPoP）
└── 10 生产实战与自测（登录体系实战/面试/20题）
```

## 2. 模块导航

| 序号 | 模块 | 核心内容 | 适合人群 |
|:---:|------|---------|---------|
| 01 | 是什么 | Token 家族、状态位置本质、2026 基线 | 所有人 |
| 02 | JWT 原理 | 结构、签名、验证五要点、攻击 | 必须 |
| 03 | 登录认证流程 | 完整闭环、前后端代码 | 必须 |
| 04 | 存储与传输 | 存储三方案、Cookie 安全属性 | 必须 |
| 05 | 双 Token 模式 | access/refresh、轮换、重用检测 | 必须 |
| 06 | 会话与选型 | session vs JWT 决策框架 | 进阶 |
| 07 | SSO 与 OAuth | 授权码 + PKCE、社交登录 | 进阶 |
| 08 | 前端集成 | axios 拦截器、401 重试、多 tab | 必须 |
| 09 | 安全红线 | XSS/CSRF/泄露面/DPoP/限流审计 | 进阶 |
| 10 | 实战与自测 | 登录体系实战、面试、20 题 | 毕业 |

## 3. 学习路线推荐

**路线一：快速上手（1-2 天）**——01 → 03 → 04 → 08 → 10。目标：能独立实现「登录 → 携带 Token → 过期刷新」的前后端闭环。

**路线二：安全进阶（3-5 天）**——路线一 + 02 → 05 → 09。目标：理解 JWT 验证细节与攻击面，双 Token 模式与安全红线烂熟。

**路线三：架构视野（一周）**——路线二 + 06 → 07。目标：能设计多应用 SSO、OAuth 第三方登录、会话/Token 混合架构。

**先厘清定位**：本体系是「前端知识拓展」——从前后端协作视角讲鉴权凭证，代码以 JavaScript/axios 为主、Java 后端对照；后端安全框架的深度实现见 [[../../06-Spring全家桶/Spring%20Security与Spring%20Security%20OAuth2/00-Spring%20Security与OAuth2知识体系总览|Spring Security 体系]]，会话技术基础见 [[../../09-Web开发全流程/Cookie%20&%20Session（会话技术）/00-会话技术总览|Cookie & Session 体系]]，HTTP 与跨域基础见 [[../HTTP%20规范、跨域、状态码、Chrome%20调试工具/00-HTTP规范与调试知识体系总览|HTTP 规范体系]]。

## 4. 核心概念速查

| 概念 | 一句话 |
|------|-------|
| 会话 ID | 服务端状态 + 客户端不透明凭证（酒店房卡模式） |
| JWT | 自包含签名凭证，任何服务可验（数字护照模式） |
| Access Token | 短时访问凭证（5-15 分钟），每次请求携带 |
| Refresh Token | 长时换新凭证（7-30 天），仅用于换 access |
| HttpOnly | Cookie 属性：JS 不可读，防 XSS 窃取 |
| SameSite | Cookie 属性：防 CSRF 的浏览器侧防线 |
| Bearer | Authorization 头的 Token 携带方式：`Bearer xxx` |
| 轮换 | Refresh 每次使用即换新、旧作废 |
| 重用检测 | 旧 refresh 再次出现 = 被盗信号，触发撤销 |
| 授权码 | OAuth 核心流程：code 换 token，token 不落 URL |
| PKCE | 授权码流程的代码挑战，2026 全客户端必选 |
| ID Token | OIDC 的身份声明凭证（JWT 形态） |
| DPoP | 令牌绑定客户端密钥（RFC 9449），防盗用 |
| BOLA | 越权访问（OWASP API Top10 第一名） |

## 5. 常见误区

**误区一：Web 应用也用 JWT 做会话**。2026 共识——Web 应用用服务端会话（HttpOnly Cookie），JWT 留给 API 场景；JWT 无状态是优点也是撤销难的缺点。

**误区二：localStorage 存 Token**。XSS 一读即窃取——2026 已是公认反模式；存 HttpOnly Cookie 或内存。

**误区三：Refresh Token 无限期复用**。固定 refresh 泄漏即永失——轮换 + 重用检测是 2026 强制姿势（OAuth 2.1 亦然）。

**误区四：校验 JWT 只验签名**。`alg:none`、算法混淆、`kid` 路径遍历都是攻击——算法白名单 + iss/aud/exp 全验。

**误区五：Payload 里存敏感数据**。JWT payload 是 base64 不是加密——手机号、身份证放进去等于明文泄露。

**误区六：401 就跳登录页**。Access Token 过期是常态——先静默刷新重试，刷新失败才跳登录（08 篇拦截器）。

**误区七：Token 传 URL 参数**。URL 进日志、进历史记录——Bearer 只走 Authorization 头（OAuth 2.1 明文禁止）。

**误区八：有认证就等于有授权**。Token 验证了「你是谁」，不代表「你能做什么」——越权（BOLA）是 API 安全第一名，授权要独立设计。

**误区九：登录接口不做限流**。登录/刷新/重置是暴力破解与撞库的正面战场——不限流等于把密码库敞开，限流 + 恒定响应是认证端点的底线。

**误区十：密码模式还能用**。OAuth 密码模式（用户名密码换 token）已被 OAuth 2.1 移除——任何场景都不该让密码直达授权服务器，用授权码。

八个误区的共同根源只有一个：**把 Token 当成「登录完就完事」的开关**。Token 是一套完整生命周期的工程——签发、存储、携带、过期、轮换、撤销、审计，每一环都有对应的攻防与设计决策。带着「登录一下就结束」的心态，存储、轮换、限流这些环节必然漏；换成「凭证生命周期」的心智——签发即开始管理、存储即考虑泄露、使用即考虑过期——整个体系才立得住。

**学习姿态的约定**：认证体系的每个机制都要动手验证——JWT 用 jwt.io 解一段真实令牌看三段结构；双 Token 用本地前后端各跑一遍登录→过期→刷新；攻防用 devtools 手动改令牌看后端反应。本体系代码以 JavaScript 为主（前端视角），Java 对照在后端章节——后端读者建议对照 [[../../09-Web开发全流程/Cookie%20&%20Session（会话技术）/00-会话技术总览|Cookie & Session 体系]] 的 Java 实现补齐代码面。

## 6. 一周计划

| 天 | 内容 | 产出 |
|:---:|------|------|
| Day1 | 01 + 02：Token 家族与 JWT 原理 | 手写 JWT 三段结构解码 |
| Day2 | 03：登录认证流程 | 前后端登录闭环跑通 |
| Day3 | 04 + 05：存储与双 Token | 双 Token + 轮换实现 |
| Day4 | 08：前端集成 | axios 拦截器 + 401 刷新 |
| Day5 | 09：安全红线 | 攻防演练 + 加固清单 |
| Day6 | 06 + 07：选型与 SSO | 选型决策 + OAuth 流程 |
| Day7 | 10：综合实战 + 自测 | 登录体系 + 20 题 |

## 7. 自测题

1. session 与 JWT 的本质区别是状态位置还是传输方式？
2. 2026 年 Web 应用与 API 各自的推荐鉴权姿势？
3. JWT 三段结构各是什么？签名怎么验？
4. 校验 JWT 必须验证哪五个点？
5. localStorage 存 Token 为什么是反模式？
6. Refresh Token 为什么要轮换？重用检测是什么？
7. 401 的正确处理流程是什么？
8. OAuth 2.1 相对 2.0 改了什么？
9. PKCE 解决什么问题？
10. 认证与授权的区别？

## 8. 参考来源

- [Web Authentication Best Practices 2026（JWT/OAuth 2.1/Passkeys）](https://dev.to/_6638a39c349d7e9c85ee20/web-authentication-best-practices-2026-jwt-oauth-21-passkeys-4e79)
- [JWT vs Session Authentication: Real Scaling Differences](https://www.loginradius.com/blog/identity/jwt-vs-session-based-authentication)
- [为新 Web 应用选择身份验证策略](https://blog.openreplay.com/zh/authentication-%E7%AD%96%E7%95%A5-web-app/)
- [API Authentication & Authorization: A Developer Guide](https://www.securecodinghub.com/blog/api-authentication-authorization-developer-guide)
- [JWT 官网（RFC 7519）](https://jwt.io/)
- [RFC 9700：OAuth 2.0 Security Best Current Practice](https://www.rfc-editor.org/rfc/rfc9700)

---

**下一模块**：[01-Token是什么](01-Token是什么.md) → 从 Token 家族与 2026 基线开始。
