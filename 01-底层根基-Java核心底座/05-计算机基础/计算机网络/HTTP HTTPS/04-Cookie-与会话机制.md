# Cookie 与会话机制

> HTTP 无状态，会话靠机制模拟：Cookie 是客户端存储的凭证载体，Session 是服务器侧状态，Token 是自包含凭证——三者加上 SameSite/HttpOnly 安全属性与 CORS/CSRF 攻防，构成 Web 身份体系的完整拼图

---

## 📚 目录

1. [会话的本质：在无状态协议上模拟状态](#1-会话的本质在无状态协议上模拟状态)
2. [Cookie 机制与属性全解析](#2-cookie-机制与属性全解析)
3. [Session 管理三种方案](#3-session-管理三种方案)
4. [Token 与 JWT](#4-token-与-jwt)
5. [CORS：跨域请求的闸门](#5-cors跨域请求的闸门)
6. [CSRF：第三方发起的伪装请求](#6-csrf第三方发起的伪装请求)
7. [登录态安全规范](#7-登录态安全规范)
8. [核心要点与思考题](#8-核心要点与思考题)

---

## 1. 会话的本质：在无状态协议上模拟状态

```text
HTTP 无状态（01 章）→ 服务器不认识"同一个人"的两次请求
会话（session）的三种实现层次：
  ① 凭证载体：客户端每次请求携带一个"我是谁"的凭证
     （Cookie / Authorization 头）
  ② 状态存储：服务器侧记住"这个凭证对应什么状态"
     （Session 表 / Redis / JWT 自包含）
  ③ 安全边界：凭证怎么防窃取、防伪造、防冒用
     （HttpOnly / SameSite / 签名 / CORS / CSRF 防御）
```

## 2. Cookie 机制与属性全解析

### 2.1 工作机制

```text
登录成功 → 服务器 Set-Cookie: sessionId=abc123
客户端存储 → 后续请求自动带 Cookie: sessionId=abc123（同域）
退出/过期 → 服务器 Set-Cookie 过期时间或客户端删除
```

### 2.2 六个属性

| 属性 | 作用 | 不设的风险 |
|------|------|-----------|
| `Expires`/`Max-Age` | 过期时间 | 会话 Cookie（关浏览器即失） |
| `Domain` | 哪些域名携带（默认当前域） | 跨子域失效/范围过大 |
| `Path` | 哪些路径携带 | 范围控制 |
| **`HttpOnly`** | **JS 不可读**（document.cookie 拿不到） | **XSS 直接偷凭证** |
| **`Secure`** | 仅 HTTPS 传输 | 明文网络被截获 |
| **`SameSite`** | 跨站请求是否携带 | **CSRF 的第一道闸** |

```java
// Java 服务端设置安全 Cookie
response.addCookie(buildCookie("sessionId", sid,
    maxAge = 7*24*3600,   // 7 天
    httpOnly = true,      // 防 XSS 读取
    secure = true,        // 仅 HTTPS
    sameSite = "Lax"));   // 跨站拦截
```

### 2.3 SameSite 三值

| 值 | 跨站请求（其他站点发起的） | 场景 |
|:---:|:---:|------|
| `Strict` | 一律不带 | 银行等强安全（体验差：站外链接进入也丢会话） |
| **`Lax`（默认）** | **顶级导航（GET 跳转）带，其他不带** | 默认安全基线 |
| `None` | 都带（必须配合 Secure） | 第三方 Cookie/SSO 场景 |

> 🎯 **核心要点**：Cookie 三件套（HttpOnly + Secure + SameSite=Lax）是**登录态安全的最小配置**——不设 HttpOnly，一个 XSS 就带走会话；不设 SameSite，一个链接点击就可能被 CSRF。

## 3. Session 管理三种方案

| 方案 | 存储 | 凭证 | 优点 | 缺点 |
|------|------|------|------|------|
| **服务端内存 Session** | JVM 内存 | Cookie 里的 sessionId | 实现简单 | **重启丢失、不跨实例**（单机玩具） |
| **集中存储 Session** | Redis/DB | sessionId | 水平扩展、共享 | Redis 是单点（需高可用） |
| **无状态 Token** | 无（自包含） | Token（JWT） | 天然水平扩展 | 无法主动注销（黑名单兜底） |

```java
// 方案二：Redis 集中 Session（生产主流）
// 依赖：spring-session-data-redis
@Configuration
public class SessionConfig {
    @Bean
    public RedisHttpSessionConfiguration config() {
        // session 存 Redis，多实例共享
        // Cookie 名/过期/安全属性统一在此配置
    }
}
```

```text
方案三的取舍（JWT 时代）：
  JWT 自包含（Header.Payload.Signature）→ 服务器无需查库即可验签
  但：无法主动吊销（改了密码旧 token 仍有效）→ 黑名单/短过期+刷新
  敏感操作（改密/支付）建议走"可撤销"方案（会话黑名单或服务端状态）
```

## 4. Token 与 JWT

### 4.1 结构

```text
JWT = Base64Url(Header).Base64Url(Payload).Signature
  Header:  {"alg":"HS256","typ":"JWT"}
  Payload: {"sub":"42","name":"alice","exp":1760000000,"iat":...}
  Signature: HMAC-SHA256(header.payload, secret)  或 RSA/ECDSA 签名

特点：
  自包含：payload 里带身份信息，验签即可信任（无需查库）
  无状态：服务器不存会话 → 水平扩展零成本
  风险：payload 只是 Base64 编码（非加密）——别放敏感数据！
```

### 4.2 传输与存储规范

| 位置 | 做法 | 注意 |
|------|------|------|
| Authorization 头 | `Bearer <token>` | 推荐；不落 Cookie → 免 CSRF |
| Cookie 存储 | HttpOnly + Secure + SameSite | 自动携带但有 CSRF 面（SameSite 兜住） |
| localStorage | ❌ 不推荐 | XSS 可读 |
| URL 参数 | ❌ 禁止 | 日志泄漏 |

## 5. CORS：跨域请求的闸门

### 5.1 同源策略与 CORS 本质

```text
同源：协议 + 域名 + 端口 三者相同
浏览器同源策略：跨源请求默认被浏览器拦截（脚本发起时）
CORS = 服务器通过响应头"声明"允许哪些跨源请求 —— 闸门在浏览器，钥匙在服务器

注意：
  ① 拦截发生在"响应侧"（请求可能已发出、服务器已执行！）
  ② 简单请求（GET/POST 表单）不预检；复杂请求先 OPTIONS 预检
  ③ CORS 只约束浏览器，不约束 curl/服务端调用
```

### 5.2 服务器响应头

```http
Access-Control-Allow-Origin: https://app.example.com   ← 允许的来源
Access-Control-Allow-Methods: GET, POST, PUT, DELETE   ← 允许的方法
Access-Control-Allow-Headers: Content-Type, Authorization
Access-Control-Allow-Credentials: true                 ← 允许带 Cookie（需配具体 Origin，不能 *)
Access-Control-Max-Age: 3600                           ← 预检结果缓存
```

```java
// Spring 配置 CORS
@Configuration
public class CorsConfig {
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration c = new CorsConfiguration();
        c.addAllowedOriginPattern("https://*.example.com");   // 白名单（勿用 *)
        c.addAllowedMethod("*");
        c.addAllowedHeader("*");
        c.setAllowCredentials(true);
        // ...
    }
}
```

> ⚠️ **CORS 三大反模式**：`*` 通配 + `Allow-Credentials` 组合（非法且危险）；来源校验用正则而非精确匹配；允许所有 Header 但业务依赖特定头鉴权。

## 6. CSRF：第三方发起的伪装请求

### 6.1 攻击模型

```text
用户已登录 app.com（Cookie 在浏览器）
用户访问恶意站点 evil.com
evil.com 的页面发起：<img src="https://app.com/transfer?to=hacker&amount=1000">
→ 浏览器自动携带 app.com 的 Cookie → 服务器以为是本人操作！

成立条件：请求"仅靠 Cookie 就能通过认证"（无自定义头/无 CSRF token）
```

### 6.2 三层防御

| 层次 | 手段 | 说明 |
|------|------|------|
| **SameSite=Lax/Strict** | Cookie 跨站不自动带 | 从源头切断（默认 Lax 已覆盖绝大多数） |
| **CSRF Token** | 表单/请求头带随机 token，服务端校验 | 双提交 Cookie/同步令牌 |
| **自定义头校验** | API 要求 `X-Requested-With` 等自定义头 | 跨站脚本无法构造（需 CORS 放行） |

```text
防线选择：
  SameSite 是"默认基建"，Token 是"纵深防御"
  纯 API（Authorization 头鉴权）天然免疫 CSRF（凭证不在 Cookie）
  Cookie 鉴权的传统表单应用：SameSite + Token 双保险
```

## 7. 登录态安全规范

```text
□ Cookie 一律 HttpOnly + Secure + SameSite=Lax（会话相关）
□ 密码不落 Cookie；Token 走 Authorization 头
□ 登录接口限流（防暴力破解）+ 验证码兜底
□ 改密/注销后：服务端 Session 失效（非 JWT）/ JWT 黑名单或短过期
□ 敏感接口二次校验（支付密码/验证码）
□ 全部页面 HTTPS（Cookie Secure 才有意义）
□ 登录失败不区分"用户不存在/密码错误"（防枚举）
□ CORS 白名单精确到子域，禁止 *
```

## 8. 核心要点与思考题

> 🎯 **本章三句话总结**：
> 1. 会话三方案：内存 Session（单机）、Redis Session（集群）、JWT（无状态）——扩展性递增、可撤销性递减；
> 2. Cookie 三属性是安全地基：HttpOnly 防 XSS 窃取、Secure 防明文泄露、SameSite=Lax 防 CSRF；
> 3. CORS 是"浏览器侧的闸门、服务器发的钥匙"，只防浏览器不防 curl——鉴权永远在服务端做。

**思考题**：

1. 为什么 JWT 无法主动注销？（→ 3/4.2）
2. `Access-Control-Allow-Origin: *` 能配合 `Allow-Credentials: true` 吗？（→ 5.2）
3. CSRF 为什么对纯 API（Bearer Token）无效？（→ 6.2）
4. 改了密码后旧的 JWT 还能用吗？怎么处理？（→ 3 方案三取舍）

---

**下一模块**：[05-HTTP/1.1 连接管理与细节](05-HTTP/1.1 连接管理与细节.md)｜**返回总览**：[00-HTTP与HTTPS知识体系总览](00-HTTP与HTTPS知识体系总览.md)
