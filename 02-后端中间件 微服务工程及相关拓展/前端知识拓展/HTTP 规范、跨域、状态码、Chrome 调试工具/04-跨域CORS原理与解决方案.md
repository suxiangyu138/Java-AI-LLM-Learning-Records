# 04 - 跨域 CORS 原理与解决方案

> 🎯 跨域是前后端分离项目的"第一关" — 同源策略是浏览器的安全机制，CORS 是标准解决方案。后端配置几个 HTTP 头就能彻底解决

---

## 目录

1. [同源策略](#1-同源策略)
2. [CORS 机制详解](#2-cors-机制详解)
3. [Spring Boot 跨域配置](#3-spring-boot-跨域配置)
4. [Nginx 跨域配置](#4-nginx-跨域配置)
5. [其他跨域方案](#5-其他跨域方案)

---

## 1. 同源策略

```
同源 = 协议 + 域名 + 端口 完全相同

http://example.com:8080/ 和
  ├── http://example.com:8080/api  → ✅ 同源（路径不同不影响）
  ├── https://example.com:8080/    → ❌ 协议不同
  ├── http://api.example.com:8080/ → ❌ 域名不同
  └── http://example.com:9090/     → ❌ 端口不同

同源策略限制：
  ✅ 允许：<script> <link> <img> <video> 等标签跨域加载
  ❌ 禁止：XHR/Fetch API 跨域请求
  ❌ 禁止：跨域读取 Cookie/LocalStorage/IndexDB
  ❌ 禁止：跨域操作 DOM（iframe 跨域）
```

---

## 2. CORS 机制详解

### 简单请求 vs 预检请求

| 类型 | 条件 | 请求方式 |
|------|------|:---:|
| **简单请求** | GET/HEAD/POST + 仅简单头（Accept/Accept-Language/Content-Language/Content-Type: form/text/plain） | 直接发 |
| **预检请求** | PUT/DELETE/PATCH + Content-Type: application/json + 自定义 Header | OPTIONS |

```text
预检请求流程（Preflight）：

1. 浏览器发送 OPTIONS 请求
   Origin: http://localhost:3000
   Access-Control-Request-Method: PUT
   Access-Control-Request-Headers: Authorization, X-Custom-Header

2. 服务器返回允许的配置
   Access-Control-Allow-Origin: http://localhost:3000
   Access-Control-Allow-Methods: GET, POST, PUT, DELETE
   Access-Control-Allow-Headers: Authorization, X-Custom-Header
   Access-Control-Max-Age: 3600      ← 缓存 1 小时，期间不重复预检

3. 浏览器校验通过 → 发送真正的 PUT 请求
```

### CORS 响应头

| Header | 说明 | 示例 |
|--------|------|------|
| `Access-Control-Allow-Origin` | 允许的源 | `*` 或 `http://localhost:3000` |
| `Access-Control-Allow-Methods` | 允许的方法 | `GET,POST,PUT,DELETE` |
| `Access-Control-Allow-Headers` | 允许的请求头 | `Authorization,Content-Type` |
| `Access-Control-Allow-Credentials` | 允许携带 Cookie | `true` |
| `Access-Control-Max-Age` | 预检缓存时间（秒） | `3600` |
| `Access-Control-Expose-Headers` | 允许 JS 读取的响应头 | `X-Total-Count` |

> ⚠️ `Access-Control-Allow-Origin: *` 和 `Access-Control-Allow-Credentials: true` **不能同时使用**！带 Credentials 时必须指定具体 Origin。

---

## 3. Spring Boot 跨域配置

```java
// ⭐ 方式1：全局配置（推荐）
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOriginPatterns("http://localhost:*", "https://*.example.com")
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true)
            .maxAge(3600);
    }
}

// 方式2：注解单个接口
@CrossOrigin(origins = "http://localhost:3000", maxAge = 3600)
@GetMapping("/api/users")

// 方式3：CorsFilter（Spring Security 场景）
@Bean
public CorsFilter corsFilter() {
    CorsConfiguration config = new CorsConfiguration();
    config.addAllowedOriginPattern("*");
    config.addAllowedMethod("*");
    config.addAllowedHeader("*");
    config.setAllowCredentials(true);
    config.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return new CorsFilter(source);
}
```

---

## 4. Nginx 跨域配置

```nginx
server {
    listen 80;
    server_name api.example.com;

    location /api/ {
        # ═══ CORS 头 ═══
        add_header Access-Control-Allow-Origin $http_origin always;
        add_header Access-Control-Allow-Methods 'GET, POST, PUT, DELETE, OPTIONS' always;
        add_header Access-Control-Allow-Headers 'Authorization, Content-Type, X-Requested-With' always;
        add_header Access-Control-Allow-Credentials 'true' always;
        add_header Access-Control-Max-Age 3600 always;

        # ═══ 预检请求直接返回 204 ═══
        if ($request_method = 'OPTIONS') {
            return 204;
        }

        proxy_pass http://backend:8080;
    }
}
```

---

## 5. 其他跨域方案

| 方案 | 原理 | 适用场景 |
|------|------|----------|
| **CORS** | 服务端设置响应头允许跨域 | ⭐ 标准方案 |
| **JSONP** | `<script>` 标签不受同源限制 | ⚠️ 仅 GET、老旧项目 |
| **反向代理** | 同源部署（Nginx 代理前后端） | 生产环境 |
| **postMessage** | iframe 跨域通信 | 嵌入页面场景 |
| **WebSocket** | 不受同源策略限制 | 实时通信 |

```nginx
# 反向代理方案（前后端同域，一劳永逸）
server {
    listen 80;
    server_name www.example.com;

    location / {
        root /usr/share/nginx/html;     # 前端静态文件
    }

    location /api/ {
        proxy_pass http://backend:8080;  # 后端 API
    }
}
# → 前后端都在 www.example.com，不存在跨域
```

> 🎯 **最佳实践**：开发环境用 Spring Boot CORS 配置；生产环境用 Nginx 反向代理（前后端同域）或 Nginx CORS 头。`OPTIONS` 预检请求走 Nginx 直接返回 204（不穿透到后端），减少无谓开销。
