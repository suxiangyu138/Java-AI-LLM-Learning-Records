# 04-Token的存储与传输
> 定位：Token 存哪里、怎么送，直接决定安全基线——localStorage 是 2026 公认反模式，HttpOnly Cookie 与内存 + 静默刷新是两条正路；Bearer 头与 Cookie 两条传输线各有攻防配套。

## 📚 目录
1. [存储三方案对比](#1-存储三方案对比)
2. [Cookie 安全属性全配](#2-cookie-安全属性全配)
3. [内存存储与静默刷新](#3-内存存储与静默刷新)
4. [传输：Bearer 头 vs Cookie](#4-传输bearer-头-vs-cookie)
5. [CORS 配合](#5-cors-配合)
6. [接口防重放](#6-接口防重放)
7. [安全清单](#7-安全清单)
8. [练习](#8-练习)

## 1. 存储三方案对比

| 存储位置 | XSS 风险 | CSRF 风险 | 持久化 | 适用 |
|----------|:---:|:---:|:---:|------|
| **localStorage** | ⚠️ 高（JS 可读） | ✅ 安全（不自动发送） | ✅ | **2026 反模式** |
| **Cookie HttpOnly** | ✅ 安全（JS 不可读） | ⚠️ 需防 CSRF | ✅ | Web 应用推荐 |
| **内存（变量）** | ✅ 安全 | ✅ 安全 | ❌（刷新丢失） | SPA + 静默刷新 |

**localStorage 的判决**：任何 XSS（哪怕一行注入）都能 `localStorage.getItem('accessToken')` 偷走全部凭证——XSS 是 Web 第一大漏洞，把令牌放在 XSS 的射程内是 2026 年的公认反模式。内部后台、低安全场景也许可用，但「能用」不等于「应该用」——选 Cookie 或内存。

**Cookie HttpOnly**：JS 完全无法读取（XSS 拿不到），浏览器自动携带——但「自动携带」也意味着 CSRF 请求会自动带凭证，需要 SameSite 等防护（下节）。**内存存储**：最安全但刷新即丢——配静默刷新（内存丢 access 后拿 refresh 换新的）达成「安全 + 体验」平衡，SPA 场景的现代答案。

## 2. Cookie 安全属性全配

Cookie 存 Token 的完整配置——**每一个属性都有对应的攻击**：

```javascript
// 后端 Set-Cookie 示例（Java 伪码对照）
// Set-Cookie: access_token=xxx; HttpOnly; Secure; SameSite=Lax; Path=/; Max-Age=900
// Set-Cookie: refresh_token=yyy; HttpOnly; Secure; SameSite=Strict; Path=/auth; Max-Age=2592000
```

| 属性 | 值 | 防什么 |
|------|----|-------|
| `HttpOnly` | 必开 | XSS 窃取（JS 不可读） |
| `Secure` | 必开 | 明文传输窃听（仅 HTTPS 发送） |
| `SameSite` | Lax/Strict | CSRF（跨站请求不自动带凭证） |
| `Path` | 收窄（refresh 限 `/auth`） | 凭证作用域扩大 |
| `__Host-` 前缀 | 高安全场景 | 锁定单源（强制 Secure、禁 Domain、Path=/） |

**SameSite 的粒度**：`Lax` 是 2026 默认——跨站 GET 顶级导航会带 Cookie（部分场景必需）、跨站 POST 不带（CSRF 主防线）；`Strict` 更严但影响跨站跳转体验；`None`（配合 Secure）只在确需跨站携带时用。**refresh 单独 Cookie + 收窄 Path** 是双 Token 的进阶姿势——业务请求（`/api/*`）只带 access，refresh 只在 `/auth` 端点出现，攻击面再收一层。

## 3. 内存存储与静默刷新

SPA 的内存存储方案——「安全最高 + 体验不折损」的标准组合：

```javascript
// tokenStore.js — 内存存储 + 静默刷新
let accessToken = null;
let refreshPromise = null;

export const getAccessToken = () => accessToken;
export function saveTokens(access, refresh) {
  accessToken = access;
  refreshToken = refresh;              // refresh 也在内存（或 HttpOnly Cookie）
}
export async function ensureToken() {
  if (accessToken) return accessToken;
  if (!refreshPromise) {
    refreshPromise = api.post('/auth/refresh', { refreshToken })
      .then(({ data }) => { saveTokens(data.accessToken, data.refreshToken); return data.accessToken; })
      .finally(() => { refreshPromise = null; });
  }
  return refreshPromise;               // 并发等待同一个刷新
}
```

要点：**刷新即丢不是缺陷是特性**——页面刷新后 access 没了，但 refresh 还在（Cookie 或持久化），`ensureToken` 在首个请求前静默换新，用户无感知；**「刷新 → 换新 → 继续」是内存方案的生命线**——没有静默刷新的内存存储就是「刷新页面就掉线」。完整生命周期：页面加载 → ensureToken 静默换 access → 业务请求携带 → 过期再换。

## 4. 传输：Bearer 头 vs Cookie

两条传输线各有攻防配套，选型看场景：

**Authorization: Bearer <token>**——显式携带，前端拦截器注入。优点：跨域无碍（不触发 CORS 凭证模式）、不自动发送（天然防 CSRF）、日志可控（可显式脱敏）。缺点：**XSS 可读**（所以配套内存或 Cookie+拦截器读）；**要防 header 泄露**（前端代码注入、第三方脚本）。**绝不放 URL 参数**——URL 进浏览器历史、进网关日志、进 Referer，OAuth 2.1 明文禁止。

**Cookie（HttpOnly）**——浏览器自动携带。优点：XSS 拿不到；缺点：跨域要 `withCredentials` + CORS 白名单、自动携带带来 CSRF 风险（SameSite 缓解）、多域名共享要 Domain 属性（扩大攻击面）。

**选型对照**：前后端同域部署 → Cookie 顺手；前后端分离跨域 → Bearer 头 + 内存/Cookie 混合；内部系统 → 怎么方便怎么来，安全属性不缩水。

## 5. CORS 配合

跨域场景下 Token 传输的 CORS 三件套（细节见 [[../HTTP%20规范、跨域、状态码、Chrome%20调试工具/04-跨域CORS原理与解决方案|CORS 体系]]）：

**Bearer 头方案**：跨域请求带自定义头会触发预检（OPTIONS）——后端必须允许 `Authorization` 头与对应方法，否则「请求发了但浏览器拦了」。

**Cookie 方案**：前端 `axios.create({ withCredentials: true })`，后端 CORS 必须 `Access-Control-Allow-Origin` 精确到源（不能 `*`）+ `Access-Control-Allow-Credentials: true`——漏任何一个 Cookie 都带不过去。**排错口诀**：CORS 报错看响应头缺哪个；带不上 Cookie 先查 withCredentials 与 Allow-Credentials。

## 6. 接口防重放

防重放是 Token 方案的补位安全（防抓包重放请求）：**时间戳 + 随机数（nonce）**——请求带 `timestamp + nonce`，服务端检查时间窗（±5 分钟）与 nonce 去重（Redis 短窗口）；**jti 去重**——JWT 的 jti 声明在短窗口内去重（02 篇）；**幂等键**——写操作带 `Idempotency-Key`，服务端按键去重（支付、下单场景）。三层选择：普通接口时间戳窗足够；敏感写操作加幂等键；高安全场景（转账）加 nonce 去重。**防重放是纵深防御的一层**——不是替代 HTTPS，而是「HTTPS 被终端恶意软件截获后」的兜底。

**存储选型的决策树**（把三方案落到具体项目）：**项目是服务端渲染的传统 Web** → Cookie HttpOnly（浏览器全程自动，后端设置最简单）；**项目是纯 SPA 且不跨域** → Cookie HttpOnly（`withCredentials` 同域无感）或内存 + 静默刷新（更安全但要处理刷新体验）；**项目是 SPA + 跨域多客户端（移动端也要用同一套 API）** → Bearer 头 + 内存/HttpOnly 混合（移动端天然走 header）；**内部后台、快速交付** → 内存 + 静默刷新（安全与开发成本平衡）。**记住一个反模式就够**：任何方案都不该把 access 落 localStorage——它的唯一优势（持久化）用 refresh 就能替代（refresh 存 HttpOnly Cookie 或受限持久化），而它的风险（XSS 直读）没有任何补偿。**「持久化 access」本身就不是需求**——短 TTL 的 access 本就不该持久，需要持久的是 refresh。

## 7. 安全清单

- [ ] 登录/刷新/登出全走 HTTPS
- [ ] Cookie 全属性：HttpOnly + Secure + SameSite（+ `__Host-` 高安全）
- [ ] 存储不用 localStorage（内部后台除外且知晓风险）
- [ ] Bearer 头方案：拦截器注入 + 日志脱敏 + 绝不放 URL
- [ ] Cookie 方案：withCredentials + Allow-Credentials + 精确 Origin
- [ ] 防重放：时间戳窗 + jti/nonce 去重（敏感接口）
- [ ] 刷新端点限流（09 篇）
- [ ] 前端日志与错误上报不打印 Token
- [ ] 第三方脚本（统计/监控）不进鉴权页面或隔离
- [ ] 跨站场景（iframe/支付回调）Cookie 配 `Partitioned`（CHIPS），同站不画蛇添足
- [ ] 定期安全自查：devtools 清点 Cookie 属性、抓包检查请求头无 Token 泄露、日志与错误上报无明文凭证（自查进发布检查单，随版本循环）

## 8. 练习

1. 存储三方案的 XSS/CSRF/持久化对比与选型？
2. localStorage 为什么是 2026 反模式？
3. Cookie 的 HttpOnly/Secure/SameSite 各防什么？
4. 内存存储为什么必须配静默刷新？
5. Bearer 头与 Cookie 的攻防配套各是什么？

> 🎯 **核心要点**：存储三方案——localStorage 反模式、Cookie HttpOnly 是 Web 标准、内存 + 静默刷新是 SPA 现代答案；Cookie 属性一个都不能省；Bearer 头防 CSRF 但防不了 XSS；跨域三件套；防重放是纵深防御。

---

**下一模块**：[05-双Token模式](05-双Token模式.md)｜**返回总览**：[00-Token总览](00-Token总览.md)
