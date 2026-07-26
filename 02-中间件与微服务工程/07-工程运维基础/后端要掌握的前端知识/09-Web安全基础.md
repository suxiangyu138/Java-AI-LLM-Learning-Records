# 09-Web安全基础
> 🎯 Web安全不是"前端的事" — 理解XSS、CSRF、CORS、CSP的攻击原理和防御方案，是后端工程能力的核心组成

---

## 目录
1. [XSS 跨站脚本攻击](#1-xss-跨站脚本攻击)
2. [CSRF 跨站请求伪造](#2-csrf-跨站请求伪造)
3. [CORS 跨域资源共享](#3-cors-跨域资源共享)
4. [CSP 内容安全策略](#4-csp-内容安全策略)
5. [其他常见Web安全威胁](#5-其他常见web安全威胁)

---

## 1. XSS 跨站脚本攻击

### 1.1 攻击原理

```text
攻击者将恶意脚本注入到网页中，当其他用户访问时执行恶意代码。

存储型XSS：
  攻击者提交 <script>steal(document.cookie)</script> 作为用户名
  → 存储到数据库 → 其他用户查看该用户名时脚本被执行
```

### 1.2 XSS 防御 — 后端和前端共同责任

```java
// ❌ 危险：直接返回未转义的用户输入
@GetMapping("/search")
public String search(@RequestParam String keyword, Model model) {
    model.addAttribute("keyword", keyword); // ← 可能包含<script>
    return "search";
}
```

```html
<!-- 模板引擎自动转义（Thymeleaf/JSP默认安全） -->
<span th:text="${keyword}"></span>     <!-- ✅ 自动HTML转义 -->
<span th:utext="${keyword}"></span>    <!-- ❌ 不转义，危险！ -->
```

| 防御措施 | 位置 | 说明 |
|----------|:---:|------|
| **输出编码** | 后端/前端 | HTML输出时转义`<`→`&lt;` `"`→`&quot;` |
| **输入校验** | 后端 | 白名单校验，拒绝非法字符 |
| **HttpOnly Cookie** | 后端 | `Set-Cookie: token=xxx; HttpOnly` → JS无法读取 |
| **CSP** | 后端(HTTP头) | 限制脚本来源 |

```java
// 后端设置HttpOnly Cookie
@PostMapping("/login")
public ResponseEntity<Void> login(@RequestBody LoginDTO dto) {
    String token = authService.login(dto);
    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, 
            "token=" + token + "; HttpOnly; Secure; SameSite=Strict")
        .build();
}
```

---

## 2. CSRF 跨站请求伪造

### 2.1 攻击原理

```text
1. 用户登录了 bank.com（Cookie中存有session）
2. 用户访问了 evil.com（恶意网站）
3. evil.com 自动提交表单到 bank.com/transfer?to=hacker&amount=10000
4. 浏览器自动带上bank.com的Cookie → 请求成功！
```

### 2.2 CSRF防御方案

| 方案 | 复杂度 | 效果 | 说明 |
|------|:---:|:---:|------|
| **CSRF Token** | 中 | ✅ 传统方案 | 每次请求携带随机Token，服务端校验 |
| **SameSite Cookie** | 低 | ✅ 现代方案 | `Set-Cookie: SameSite=Strict/Lax` |
| **Referer校验** | 低 | ⚠️ 可绕过 | 检查请求来源Referer |
| **自定义Header** | 低 | ✅ | Ajax请求才能加自定义头，form不能 |

```java
// Spring Security 默认开启 CSRF 防护
// 前后端分离(JWT)时可关闭（因为不依赖Cookie认证）
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) {
    http.csrf(csrf -> csrf
        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
    );
    return http.build();
}
```

> ⚠️ **关键判断**：如果认证方式是JWT(Header中Bearer token)，则**天然免疫CSRF**（因为form无法添加Authorization头）。如果认证方式是Cookie/Session，则**必须防御CSRF**。

---

## 3. CORS 跨域资源共享

### 3.1 什么是跨域

```text
同源策略（Same-Origin Policy）：
  协议 + 域名 + 端口 三者完全相同 = 同源

http://localhost:3000 → http://localhost:8080  ❌ 跨域（端口不同）
https://api.example.com → https://www.example.com ❌ 跨域（子域名不同）
```

### 3.2 后端CORS配置

```java
// Spring Boot CORS全局配置
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins("http://localhost:3000")  // 前端地址
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true)    // 允许携带Cookie
            .maxAge(3600);            // 预检请求缓存时间
    }
}
```

### 3.3 CORS响应头速查

| 响应头 | 含义 |
|--------|------|
| `Access-Control-Allow-Origin` | 允许的源 |
| `Access-Control-Allow-Methods` | 允许的方法 |
| `Access-Control-Allow-Headers` | 允许的请求头 |
| `Access-Control-Allow-Credentials` | 允许携带Cookie |
| `Access-Control-Max-Age` | 预检请求缓存时间（秒） |

> ⚠️ `Access-Control-Allow-Origin: *` 和 `Allow-Credentials: true` 不能同时使用。如果需要携带Cookie，必须指定具体Origin。

### 3.4 预检请求(Preflight)

```
浏览器对复杂请求先发OPTIONS预检 → 服务器返回允许的CORS头 → 浏览器确认允许后才发真实请求

复杂请求 = 非GET/POST/HEAD + 自定义Content-Type + 自定义Header
简单请求 = GET/POST/HEAD + 标准Content-Type
```

---

## 4. CSP 内容安全策略

> 通过HTTP头告诉浏览器"只允许从哪些来源加载资源"

```text
# 后端设置CSP响应头
Content-Security-Policy: 
  default-src 'self';                    # 默认只允许同源
  script-src 'self' 'nonce-abc123';      # 脚本只允许同源+带nonce的内联
  style-src 'self' 'unsafe-inline';      # 样式允许同源+内联
  img-src 'self' https://cdn.example.com; # 图片允许同源+CDN
  connect-src 'self' https://api.example.com; # XHR/Fetch只允许同源+指定API
```

> 💡 **后端只需知道**：CSP是HTTP响应头，后端配置。现代框架（Spring Security）可方便配置CSP。

---

## 5. 其他常见Web安全威胁

| 威胁 | 简述 | 后端防御 |
|------|------|----------|
| **SQL注入** | 用户输入拼接到SQL | 参数化查询（MyBatis `#{}` 不用 `${}`） |
| **点击劫持** | 透明iframe覆盖在按钮上 | `X-Frame-Options: DENY` |
| **文件上传漏洞** | 上传可执行文件 | 校验文件类型(魔数而非扩展名) + 隔离存储 |
| **敏感信息泄露** | 错误信息暴露内网IP/栈信息 | 统一错误处理，生产不显示StackTrace |
| **暴力破解** | 大量尝试密码 | 登录限流 + 验证码 + 账号锁定 |

---

> 🎯 **后端安全底线**：XSS输出编码 + CSRF/SameSite + CORS白名单 + SQL参数化。这四条做到了，就挡住了90%的Web攻击。
