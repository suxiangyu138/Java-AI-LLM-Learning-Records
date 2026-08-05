# HTTP 安全实践

> 安全的 HTTP 应用 = 传输层（HTTPS 强制）+ 头部层（CSP/HSTS/X-Frame-Options 等安全头）+ 应用层（注入/XSS/CSRF 防护）+ 运维层（证书/日志/监控）。本章给出可落地的安全配置清单与攻防对照

---

## 📚 目录

1. [威胁模型：Web 应用四大攻击面](#1-威胁模型web-应用四大攻击面)
2. [注入攻击与防御](#2-注入攻击与防御)
3. [XSS 与内容安全策略（CSP）](#3-xss-与内容安全策略csp)
4. [CSRF 与点击劫持](#4-csrf-与点击劫持)
5. [安全响应头全景](#5-安全响应头全景)
6. [CORS 配置安全规范](#6-cors-配置安全规范)
7. [HTTPS 强制与 HSTS](#7-https-强制与-hsts)
8. [证书与密钥运维](#8-证书与密钥运维)
9. [核心要点与思考题](#9-核心要点与思考题)

---

## 1. 威胁模型：Web 应用四大攻击面

```text
攻击面一：输入（参数/主体/文件）→ 注入、XSS、路径穿越
攻击面二：身份（Cookie/Token）→ 窃取、伪造、重放
攻击面三：浏览器信任（同源策略的漏洞面）→ CSRF、点击劫持、CORS 误配
攻击面四：传输与基础设施 → 明文、证书错误、日志泄漏

防御总纲：纵深防御——
  传输加密（HTTPS）+ 边界校验（输入验证）+ 平台防护（CSP）
  + 最小权限（CORS/同源）+ 监控审计（日志/告警）
```

## 2. 注入攻击与防御

### 2.1 SQL 注入

```java
// ❌ 危险：字符串拼接
String sql = "SELECT * FROM users WHERE name = '" + name + "'";
// 输入: ' OR '1'='1 → 全表查询！输入: '; DROP TABLE users; -- → 删表

// ✅ 正确：参数化查询（预编译）
PreparedStatement ps = conn.prepareStatement(
    "SELECT * FROM users WHERE name = ?");
ps.setString(1, name);        // 参数与 SQL 结构分离，注入无效
```

| 防御手段 | 说明 |
|---------|------|
| **参数化查询** | 首选（SQL 结构永不被输入污染） |
| ORM 框架 | MyBatis 用 `#{}`（预编译）不用 `${}`（拼接） |
| 白名单校验 | 枚举/正则限制输入形态 |
| 最小权限 | 数据库账号只授权所需表（防 DROP 类） |

### 2.2 其他注入面

| 类型 | 攻击形态 | 防御 |
|------|---------|------|
| 命令注入 | `Runtime.exec("ls " + userInput)` | 禁止拼接执行；白名单 |
| 路径穿越 | `../../etc/passwd` | 规范化路径 + 白名单目录 |
| 日志注入 | CRLF 伪造日志行 | 过滤 `\r\n`；结构化日志 |
| LDAP/SSRF | 构造协议请求 | 白名单目标、网络隔离 |

## 3. XSS 与内容安全策略（CSP）

### 3.1 三种 XSS

| 类型 | 注入位置 | 例子 |
|------|---------|------|
| 反射型 | 响应中直接回显输入 | `?q=<script>` 回显在页面 |
| 存储型 | 存数据库再渲染（最危险） | 评论区的 `<script>` |
| DOM 型 | JS 读取 DOM 后 innerHTML 执行 | `location.hash` 注入 |

### 3.2 防御三层

```text
① 输出编码（第一道）：
   HTML 上下文 → &lt; &gt;（框架自动转义：Vue/React 默认转义）
   JS 上下文 → 字符串转义
   URL/属性上下文 → URL 编码
   原则：在"渲染点"编码，而非"存储点"（存储保持原样，输出时编码）

② CSP（第二道，纵深）：
   Content-Security-Policy 头限制浏览器行为：
     脚本来源、样式来源、内联执行、eval

③ HttpOnly Cookie（第三道）：
   即使 XSS 执行，也拿不到会话 Cookie（04 章）
```

### 3.3 CSP 配置

```http
# 只允许同源脚本 + 指定 CDN
Content-Security-Policy: default-src 'self';
                        script-src 'self' https://cdn.example.com;
                        style-src 'self' 'unsafe-inline';
                        img-src 'self' data:;
                        frame-ancestors 'none';          # 防点击劫持
                        report-uri /csp-report            # 违规上报
```

| 指令 | 作用 |
|------|------|
| `script-src` | 脚本来源（核心） |
| `frame-ancestors` | 允许被谁嵌入（防点击劫持，替代 X-Frame-Options） |
| `default-src` | 兜底 |
| `upgrade-insecure-requests` | 页面内 HTTP 资源自动升级 HTTPS |

> ⚠️ **部署注意**：CSP 收紧会误伤内联脚本/第三方组件——先 `Content-Security-Policy-Report-Only` 观察违规报告，再逐步收紧。

## 4. CSRF 与点击劫持

### 4.1 CSRF 防御回顾（04 章已述，此处补全）

```text
完整防线：
  ① SameSite=Lax（Cookie 层，默认基建）
  ② CSRF Token（应用层，表单/头携带）
  ③ 自定义头（API 层，X-Requested-With 等）
  ④ 敏感操作二次校验（验证码/支付密码）
```

### 4.2 点击劫持（Clickjacking）

```text
攻击：恶意页面用透明 iframe 覆盖"转账按钮"，诱导用户点击
防御：
  X-Frame-Options: DENY          （老方案）
  CSP frame-ancestors 'none'      （新方案，覆盖 X-Frame-Options）
  同源嵌入例外：frame-ancestors 'self'
```

## 5. 安全响应头全景

| 响应头 | 作用 | 建议值 |
|--------|------|--------|
| `Strict-Transport-Security` | 强制 HTTPS（见第 7 节） | `max-age=31536000; includeSubDomains` |
| `Content-Security-Policy` | 资源来源白名单 | 按应用收紧 |
| `X-Frame-Options` | 防点击劫持 | `DENY`（CSP 下可省） |
| `X-Content-Type-Options` | 禁止 MIME 嗅探 | `nosniff` |
| `Referrer-Policy` | 控制 Referer 外泄 | `strict-origin-when-cross-origin` |
| `Permissions-Policy` | 限制浏览器 API（摄像头等） | 按需 |
| `X-XSS-Protection` | 浏览器 XSS 过滤器 | `0`（现代浏览器已由 CSP 取代） |

```nginx
# Nginx 全量安全头（模板）
add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
add_header Content-Security-Policy "default-src 'self'; frame-ancestors 'none'" always;
add_header X-Content-Type-Options "nosniff" always;
add_header Referrer-Policy "strict-origin-when-cross-origin" always;
```

## 6. CORS 配置安全规范

```text
原则：最小授权 + 精确匹配
  ❌ Access-Control-Allow-Origin: *（配合凭据时非法）
  ❌ 允许全部 Header/方法（扩大攻击面）
  ❌ 来源用正则模糊匹配（example.com.evil.com 绕过）

✅ 规范：
  白名单精确到协议+域名+端口
  Allow-Credentials 与具体 Origin 配对
  预检缓存（Max-Age）合理（3600s 内）
  敏感接口不参与 CORS（或仅同源）
  服务器侧二次校验 Origin（不信任浏览器转发）
```

```java
// Spring 规范写法（白名单 + 凭据）
corsConfigurationSource.setAllowedOriginPatterns(
    List.of("https://app.example.com", "https://admin.example.com"));
corsConfigurationSource.setAllowCredentials(true);
// 禁止使用 allowedOrigins(List.of("*")) + allowCredentials(true)
```

## 7. HTTPS 强制与 HSTS

### 7.1 强制 HTTPS 的三个层次

```text
① 服务器重定向：HTTP 80 → HTTPS 443（301）
② HSTS 头：浏览器记住"该域名只走 HTTPS"（302 之前就拦截）
③ HSTS 预加载：浏览器出厂内置域名列表（首次访问也安全）

  Strict-Transport-Security: max-age=31536000; includeSubDomains
  → 浏览器在未来 1 年内：该域及子域只发 HTTPS 请求
  → 中间人无法把用户降级回 HTTP
```

### 7.2 HSTS 预加载

```text
提交 hstspreload.org 后浏览器内置：
  首次访问（无任何 HSTS 信息）也直接 HTTPS
  ⚠️ 提交前必须确认全子域 HTTPS 就绪（否则子域永久不可访问 HTTP）
```

## 8. 证书与密钥运维

```text
□ 证书短寿命自动续期（ACME/Let's Encrypt 90 天；2026 行业共识）
□ 私钥权限 600、不入仓库、定期轮换
□ 多环境分离（测试证书不混生产）
□ 吊销策略：泄露即吊销 + OCSP Stapling
□ 密钥类型：RSA 2048+ 或 ECDSA P-256（性能更优）
□ 后量子就绪：OpenSSL 3.5+（07 章 X25519MLKEM768）
□ 监控：证书到期 T-30 告警、TLS 评分（SSL Labs A+）
```

## 9. 核心要点与思考题

> 🎯 **本章三句话总结**：
> 1. 纵深防御五层：HTTPS 强制（传输）+ 输入校验（注入）+ 输出编码与 CSP（XSS）+ SameSite/Token（CSRF）+ 安全头与监控（基建）；
> 2. 安全头是"免费的最优防护"：HSTS + CSP + X-Content-Type-Options + Referrer-Policy 四件套必配；
> 3. CORS 最小授权、证书自动续期、日志不泄敏——安全是配置与流程，不是单一技术。

**思考题**：

1. 存储型 XSS 的防御关键在哪一层？（→ 3.2 渲染点编码）
2. CSP 为什么先开 Report-Only？（→ 3.3 部署注意）
3. HSTS 预加载提交前为什么必须全子域 HTTPS？（→ 7.2）
4. `Access-Control-Allow-Origin: *` 为什么不能配凭据？（→ 6）

---

**下一模块**：[09-面试高频考点与总结](09-面试高频考点与总结.md)｜**返回总览**：[00-HTTP与HTTPS知识体系总览](00-HTTP与HTTPS知识体系总览.md)
