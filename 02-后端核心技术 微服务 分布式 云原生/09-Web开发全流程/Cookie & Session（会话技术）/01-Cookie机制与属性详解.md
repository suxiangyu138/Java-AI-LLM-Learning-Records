# 01-Cookie 机制与属性详解
> Cookie 是什么、六大属性（HttpOnly/Secure/SameSite/Domain/Path/Max-Age）、作用域规则、Java 操作——"浏览器存储与自动携带的小纸条"

## 📚 目录
1. [Cookie 是什么](#1-cookie-是什么)
2. [六大属性详解](#2-六大属性详解)
3. [SameSite 策略（现代浏览器默认）](#3-samesite-策略现代浏览器默认)
4. [作用域规则](#4-作用域规则)
5. [Java 操作 Cookie（Servlet API）](#5-java-操作-cookieservlet-api)
6. [Cookie 限制与安全](#6-cookie-限制与安全)
7. [核心要点](#7-核心要点)
8. [参考来源](#8-参考来源)

## 1. Cookie 是什么

```text
Cookie = 服务器下发给浏览器的"小纸条"（键值对）
  服务器：Set-Cookie 响应头下发
  浏览器：存储 → 后续同域请求自动携带（Cookie 请求头）

典型用途：
  会话标识（JSESSIONID）
  用户偏好（语言/主题）
  追踪/统计
```

```text
Cookie 流转：
① 浏览器请求 → ② 服务器 Set-Cookie: token=abc; HttpOnly; SameSite=Lax
③ 浏览器存储 → ④ 后续请求自动带 Cookie: token=abc
⑤ 服务器凭 Cookie 识别用户
```

> 🎯 核心：**Cookie 是"客户端存储 + 自动携带"**——服务器不主动读浏览器存储，而是浏览器每次自动带上——这就是"无状态 HTTP 记住你"的第一种实现。

## 2. 六大属性详解

| 属性 | 含义 | 生产建议 |
|------|------|---------|
| `Domain` | 作用域域名 | 不设 = 仅当前域；跨子域共享才设 |
| `Path` | 作用路径 | 默认 `/` |
| `Max-Age` / `Expires` | 有效期 | 会话 Cookie 不设（关浏览器消失）；持久用 Max-Age |
| `HttpOnly` | 禁止 JS 读取 | **会话/认证 Cookie 必设** |
| `Secure` | 仅 HTTPS 传输 | **生产必设** |
| `SameSite` | 跨站发送策略 | Lax（现代默认） |

```text
Set-Cookie: JSESSIONID=abc123; Path=/; HttpOnly; Secure; SameSite=Lax; Max-Age=3600
```

> 🎯 **认证 Cookie 的黄金组合**：`HttpOnly + Secure + SameSite=Lax`——三件套挡住三类攻击（XSS 窃取、明文窃听、CSRF 滥用），现代浏览器默认 SameSite=Lax 已挡住大部分 CSRF。

## 3. SameSite 策略（现代浏览器默认）

| 值 | 行为 | 场景 |
|----|------|------|
| `Lax` | 顶级导航（链接跳转）携带，跨站子请求不携带 | **默认**（2020 起 Chrome 默认） |
| `Strict` | 任何跨站都不携带 | 强安全（影响第三方链接登录态） |
| `None` | 全部携带 | **必须配 `Secure`**（HTTPS）；跨域需 CORS 凭证 |

```text
SameSite 的实战影响：
  Lax：用户从邮件链接进站 → 登录态可用（顶级导航）
      站内 AJAX 请求跨站 → 不带 Cookie（防 CSRF 第一道防线）
  None+Secure：前后端分离跨域 → 需 CORS Allow-Credentials

注意（Spring Boot 4）：DefaultCookieSerializer 默认 sameSite 为 Lax，
  但未显式配置时存在被 null 覆盖的回归风险（Issue #48830）——显式配置最稳
```

> 💡 **SameSite=Lax 的意义**：**现代浏览器的 CSRF 第一道防线**——跨站 POST 不带 Cookie，CSRF 攻击自动失效大半；前后端分离跨域时评估 None+Secure。

## 4. 作用域规则

```text
Cookie 发送规则：
  域名匹配：完全匹配或 Domain 后缀匹配（example.com 覆盖 sub.example.com）
  路径匹配：Path 前缀匹配
  协议：Secure 属性要求 HTTPS
  SameSite：跨站限制
```

| 场景 | 配置 |
|------|------|
| 单域 | 不设 Domain（仅当前域） |
| 多子域共享 | `Domain=example.com`（a.example.com 与 b.example.com 共享） |
| 跨域（前后端分离） | CORS + SameSite=None + Secure |
| 路径限定 | Path=/api（仅 API 路径携带） |

> ⚠️ **跨域 Cookie 是"三层配合"**：CORS（Access-Control-Allow-Credentials: true）+ SameSite=None + Secure——少一层就失败；纯跨域无 Cookie 场景（移动端/第三方）评估 Token 方案（见 [06](06-Token与JWT无状态方案.md)）。

## 5. Java 操作 Cookie（Servlet API）

```java
// 设置 Cookie
Cookie cookie = new Cookie("theme", "dark");
cookie.setMaxAge(3600);            // 有效期 1 小时（不设 = 会话 Cookie）
cookie.setPath("/");               // 作用路径
cookie.setHttpOnly(true);          // 禁止 JS 读取
cookie.setSecure(true);            // 仅 HTTPS
cookie.setAttribute("SameSite", "Lax");   // 现代 Servlet 支持
response.addCookie(cookie);

// 读取 Cookie
Cookie[] cookies = request.getCookies();
if (cookies != null) {
    for (Cookie c : cookies) {
        if ("theme".equals(c.getName())) {
            String theme = c.getValue();
        }
    }
}

// 删除 Cookie（同名 + Max-Age=0）
Cookie delete = new Cookie("theme", "");
delete.setMaxAge(0);
delete.setPath("/");
response.addCookie(delete);
```

| 操作 | API |
|------|-----|
| 下发 | `response.addCookie(cookie)` |
| 读取 | `request.getCookies()` |
| 删除 | 同名 Cookie + `setMaxAge(0)` |
| 属性 | setHttpOnly/setSecure/setMaxAge/setPath |

> 💡 Spring 应用更常用：`HttpSession`（内部用 Cookie 存 JSESSIONID，见 [02](02-Session机制与Java实现.md)）；直接操作 Cookie 用于偏好类数据。

## 6. Cookie 限制与安全

| 限制 | 说明 |
|------|------|
| 大小 | 单 Cookie 约 4KB |
| 数量 | 每域约 50 个 |
| 明文 | 不加密（Secure 只是传输加密，存储仍明文）——**不存敏感数据** |
| JS 可读 | 无 HttpOnly 时 `document.cookie` 可读 |

```text
Cookie 安全纪律：
  ① 认证数据必 HttpOnly（防 XSS 窃取）
  ② 不存明文敏感信息（密码/身份证）
  ③ Secure + HTTPS（传输）
  ④ SameSite=Lax（防 CSRF）
  ⑤ 敏感操作验证（改密码需重新认证）
```

> ⚠️ **Cookie 存储的是"凭证或偏好"，不是"机密数据"**——真正的用户信息放服务器（Session/DB），Cookie 只放"标识"（SessionID/Token）。

## 7. 核心要点

> 🎯 **核心要点**：
> - Cookie = 服务器下发 + 浏览器自动携带的键值对（无状态 HTTP 的第一种记忆）；
> - 六大属性：Domain/Path/Max-Age/HttpOnly/Secure/SameSite；
> - 认证 Cookie 黄金组合：**HttpOnly + Secure + SameSite=Lax**；
> - SameSite：Lax 现代默认（CSRF 第一道防线）；None 必须配 Secure + CORS；
> - 跨域 Cookie 三层配合：CORS 凭证 + None + Secure；
> - Java 操作：addCookie/getCookies/删除（Max-Age=0）；
> - Cookie 存"标识"不存"机密"；~4KB 限制。

## 8. 参考来源

- [MDN：HTTP Cookie](https://developer.mozilla.org/zh-CN/docs/Web/HTTP/Cookies)
- [MDN：SameSite 属性](https://developer.mozilla.org/zh-CN/docs/Web/HTTP/Headers/Set-Cookie)
- [OWASP：Cookie 安全](https://cheatsheetseries.owasp.org/cheatsheets/HTTP_Strict_Transport_Security_Cheat_Sheet.html)
- [Spring Boot 4 SameSite Issue #48830](https://github.com/spring-projects/spring-boot/issues/48830)

---

**下一模块**：[02-Session机制与Java实现](02-Session机制与Java实现.md)　/　**返回总览**：[00-总览](00-会话技术总览.md)
