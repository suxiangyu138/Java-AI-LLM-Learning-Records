# 05 Web 防护：CSRF、CORS 与安全头

> 升级 Boot 4 后"接口全是 403"的元凶、SPA 场景的 CSRF 新解法、跨域配置与安全响应头——Web 防护层是 7.0 默认行为变化最大的地方，也是生产事故高发区

---

## 📚 目录

1. [CSRF：原理与攻击模型](#1-csrf原理与攻击模型)
2. [Boot 4 默认开启：403 事故与正确解法](#2-boot-4-默认开启403-事故与正确解法)
3. [7.0 SPA 友好方案：csrf.spa()](#3-70-spa-友好方案csrfspa)
4. [CORS：跨域配置全解](#4-cors跨域配置全解)
5. [安全响应头](#5-安全响应头)
6. [点击劫持与 XSS 防护](#6-点击劫持与-xss-防护)
7. [防护配置清单](#7-防护配置清单)

---

## 1. CSRF：原理与攻击模型

**CSRF（跨站请求伪造）**：用户在已登录的站点 A 保持会话，攻击者诱导用户浏览器向站点 A 发起恶意请求（如转账），浏览器**自动携带 Cookie**，请求被服务器当成用户本人操作。

```text
攻击模型：
  受害者已登录 bank.com（会话 Cookie 生效）
  攻击页面 evil.com 里隐藏：
      <img src="https://bank.com/transfer?to=attacker&amount=10000">
  受害者访问 evil.com → 浏览器向 bank.com 发请求并带 Cookie
  → 服务器认为是本人操作 → 转账成功
```

**防御核心**：让"请求合法性"与 Cookie 解耦——要求请求携带一个**攻击者无法获取的额外令牌**（CSRF Token）：

| 防御方式 | 机制 | Spring 支持 |
|---------|------|:---:|
| 同步令牌模式 | Session 绑定随机 token，请求头/表单回传 | ✅ 默认 |
| SameSite Cookie | 浏览器限制跨站携带 Cookie | ✅ 配合 |
| 双重提交 | Cookie 与请求参数各一个 token 并比对 | 可配置 |

## 2. Boot 4 默认开启：403 事故与正确解法

### 2.1 事故现场

```text
升级 Boot 3.x → 4.x 之后：
  POST /api/order  →  403 Forbidden
  GET  /api/order  →  200 OK（读不受影响）

根因：Boot 4 + Security 7 的 CSRF 保护默认覆盖所有端点（包括无状态 API）
```

### 2.2 正确解法（分场景）

```java
// 场景一：纯无状态 REST API（JWT/Token 认证，无 Cookie 会话）
http.csrf(csrf -> csrf.disable());      // 无 Cookie → 无 CSRF 风险面 → 可关

// 场景二：有状态 Session + 前后端同源（服务端渲染/同源前端）
// 默认开启即可，表单自动带令牌（Thymeleaf 自动注入隐藏域）

// 场景三：有状态 Session + 前后端分离（SPA）
http.csrf(csrf -> csrf.spa());          // 7.0 新 DSL：SPA 友好模式（见下节）
```

> ⚠️ **判定准则**：CSRF 防护针对的是 **Cookie 会话**。若认证凭据不在 Cookie（JWT 在内存/请求头），CSRF 风险面不存在，可安全关闭；若用 Cookie 会话 + SPA，用 `csrf.spa()` 而非粗暴关闭。

### 2.3 关闭 CSRF 前自检

- [ ] 认证方式确认：JWT 无 Cookie？→ 可关
- [ ] Cookie 会话？→ 不能关，用令牌或 spa()
- [ ] 有管理后台？（管理类操作高危）→ 必须保留防护
- [ ] 团队知道"谁关的、为什么"（注释说明）

## 3. 7.0 SPA 友好方案：csrf.spa()

SPA 场景的经典矛盾：前端拿不到 CSRF Token（不是服务端渲染、不读 Cookie），所以旧方案要么关 CSRF，要么专门开接口发 Token。7.0 的 `csrf.spa()` 把这条路走通：

```java
http.csrf(csrf -> csrf.spa());
```

**spa() 做了什么：**

```text
1. CSRF 保护保持开启（不安全请求仍校验令牌）
2. 对 "SPA 专用预检路径"（如 /api/login、/api/user 等认证端点）
   不要求令牌，返回可用的 CSRF Token（Cookie 方式下发）
3. 前端从 Cookie 读取 token，之后请求带 X-XSRF-TOKEN 头
   （Angular/React 生态的标准做法：Cookie → 请求头回传）
```

| 对比 | 传统 | csrf.spa()（7.0） |
|------|------|:---:|
| SPA 拿令牌 | 手动开接口 | 自动下发 |
| 保护强度 | 关 CSRF 则无 | **保持开启** |
| 前端适配 | 自研 | 标准 `X-XSRF-TOKEN` 头 |

> 🎯 **面试点**：`csrf.spa()` 是 7.0 的差异化特性——"SPA 场景不必在安全和体验之间二选一"。答"前后端分离的 CSRF 怎么做"时，这是 2026 年标准答案。

## 4. CORS：跨域配置全解

### 4.1 与 CSRF 的关系（先理清）

| 概念 | 防护对象 | 机制 |
|------|---------|------|
| CORS | 浏览器跨域**读取**响应 | 服务端声明允许哪些源（Access-Control-Allow-Origin） |
| CSRF | 跨站**伪造写请求** | 令牌校验 |

**CORS 是"允许谁来读"，CSRF 是"防止伪造写"**——两者常一起配，但语义完全不同。

### 4.2 配置

```java
http.cors(cors -> cors.configurationSource(corsConfigurationSource()));

@Bean
CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(List.of("https://admin.example.com"));  // 白名单，勿用 *
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-XSRF-TOKEN"));
    config.setAllowCredentials(true);     // 允许携带 Cookie（与 allowedOrigins 白名单联动）
    config.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
}
```

### 4.3 常见坑

| 坑 | 现象 | 处理 |
|----|------|------|
| `allowedOrigins("*")` + `allowCredentials(true)` | 启动报错/浏览器拒绝 | **不能并存**：带凭据必须明确源 |
| 预检 OPTIONS 被安全链拦截 | 跨域请求 403 | CORS 配置在授权规则**之前**生效（Spring Security 自动处理预检） |
| 多环境域名 | 测试环境跨域失败 | 配置中心化：`allowedOriginPatterns` |
| 遗漏 `X-XSRF-TOKEN` 头 | spa() 模式请求 403 | 请求头加入白名单 |

## 5. 安全响应头

`HeaderWriterFilter` 默认给每个响应注入安全头（可用 `http.headers()` 定制）：

```java
http.headers(headers -> headers
        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'"))  // CSP
        .frameOptions(frame -> frame.deny()));   // 防点击劫持（默认 SAMEORIGIN）
```

| 响应头 | 作用 | 默认值 |
|--------|------|:---:|
| `Cache-Control: no-store` | 禁止缓存敏感页 | ✅ |
| `X-Content-Type-Options: nosniff` | 防 MIME 嗅探 | ✅ |
| `X-Frame-Options: SAMEORIGIN` | 防点击劫持 | ✅ |
| `Strict-Transport-Security` | HSTS 强制 HTTPS | ✅ |
| `Content-Security-Policy` | CSP 内容安全策略 | 需显式配置 |
| `X-XSS-Protection` | 旧式 XSS 过滤 | 现代浏览器弃用（CSP 替代） |

> 💡 **HSTS 注意**：`Strict-Transport-Security` 生效后浏览器会强制 HTTPS——**HTTP 内网环境会整站打不开**，内网/测试环境需评估再开。

## 6. 点击劫持与 XSS 防护

### 6.1 点击劫持（Clickjacking）

```text
攻击：攻击者用透明 iframe 覆盖在诱导页面上
      受害者点击"按钮"实际点到"删除账号"等隐藏功能
防御：X-Frame-Options（DENY/SAMEORIGIN）或 CSP frame-ancestors
```

```java
http.headers(headers -> headers.frameOptions(frame -> frame.deny()));
// 需要被 iframe 嵌入的业务页面（如报表）→ SAMEORIGIN 并按需放行
```

### 6.2 XSS（跨站脚本）

Spring Security 不直接做 XSS 过滤（那是前端/模板引擎的职责），但：

| 层 | 职责 |
|----|------|
| 模板层 | Thymeleaf `th:text` 自动转义（**禁 `th:utext` 渲染用户输入**） |
| 输入层 | 参数白名单/长度校验 |
| 响应层 | CSP 头限制脚本来源 |
| 输出层 | 富文本场景使用白名单清洗库 |

## 7. 防护配置清单

```java
@Bean
SecurityFilterChain apiChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())                    // 纯 JWT 无状态 API：可关
        .cors(Customizer.withDefaults())                 // 配合 CORS 配置源
        .headers(headers -> headers
            .frameOptions(frame -> frame.deny()))
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth.anyRequest().authenticated());
    return http.build();
}
```

**上线前 Web 防护自检：**

- [ ] CSRF 决策明确：无状态 API 已关（注释说明原因）/ 有状态已开启
- [ ] SPA 场景用 `csrf.spa()` 而非关闭
- [ ] CORS 白名单精确（无 `*` + credentials 并存）
- [ ] 敏感接口 HTTPS + HSTS（评估内网影响）
- [ ] 响应头默认集完整（nosniff/frame-options/cache-control）
- [ ] 上传/富文本场景有 XSS 清洗

> 🎯 **核心要点**：Boot 4 升级的 403 浪潮根因就是 CSRF 默认值变化——**关闭 CSRF 不是默认动作，而是有条件的决策**（无 Cookie 会话才可关）。7.0 的 `csrf.spa()` 让 SPA 保持防护；CORS 管"谁能读"、CSRF 管"不许伪造写"，两者不可混。安全头是最后一道免费防线，默认值已很完善，CSP 按需加强。

---

**上一模块**：[04-方法级安全：@EnableMethodSecurity与表达式](04-方法级安全：@EnableMethodSecurity与表达式.md)　**下一模块**：[06-Session与会话安全](06-Session与会话安全.md)
