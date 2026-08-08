# 静态资源与 SPA 路由
> 默认静态目录、缓存策略与版本化资源、WebJars，以及 SPA history 路由的三种落地姿势——"后端只出 API、前端托管页面"的现代架构在 Boot 里的正确打开方式

## 📚 目录
1. [默认静态资源策略](#1-默认静态资源策略)
2. [spring.web.resources 配置](#2-springwebresources-配置)
3. [缓存策略与版本化资源](#3-缓存策略与版本化资源)
4. [WebJars 前端依赖](#4-webjars-前端依赖)
5. [SPA history 路由的三条出路](#5-spa-history-路由的三条出路)
6. [前后端分离架构模式](#6-前后端分离架构模式)

## 1. 默认静态资源策略

```text
Boot 默认扫描以下 classpath 目录（顺序即优先级）：
  classpath:/META-INF/resources/
  classpath:/resources/
  classpath:/static/          ← 实际项目 99% 用这个
  classpath:/public/
  + 请求路径映射：/**（未命中 Controller 时兜底）
```

| 目录 | 说明 |
|------|------|
| `static/` | 主目录：CSS/JS/图片/字体/favicon |
| `public/` | 非打包资源（上传的临时静态） |
| `resources/` | 兜底目录 |
| `META-INF/resources/` | jar 内组件自带静态资源（WebJars 原理） |

> 🎯 请求链路：`/styles.css` → Controller 无映射 → `ResourceHttpRequestHandler` 按优先级找文件 → 命中返回 + Content-Type（按扩展名）→ 未命中 404。

## 2. spring.web.resources 配置

```yaml
spring:
  web:
    resources:
      static-locations:         # 自定义静态位置（覆盖默认，需包含默认的才保留）
        - classpath:/static/
        - file:/data/static/    # 磁盘目录
      cache:
        period: 3600s           # 缓存头 Cache-Control: max-age=3600
        cachecontrol:
          max-age: 30d
          cache-public: true
          no-cache: false
      chain:
        strategy:
          content:
            enabled: true       # 内容哈希版本化（CSS/JS 指纹）
            paths: /static/**
```

```java
// Boot 4：静态资源路径的工具类（含新增 /fonts/** 默认放行）
PathRequest.toStaticResources().atCommonLocations();   // 常用位置
```

> 💡 `PathRequest.toStaticResources()`（Boot 4 起）用于给静态资源统一加缓存/安全规则——如配合 Security 放行静态资源、配合缓存控制，避免手写一堆路径。

## 3. 缓存策略与版本化资源

### 3.1 两级缓存思维

| 层级 | 对象 | 缓存头 | 说明 |
|------|------|--------|------|
| 浏览器缓存 | CSS/JS/图片 | `Cache-Control: max-age=30d`（带版本指纹） | 命中则零请求 |
| 内容协商 | HTML/API | `no-cache`（每次校验） | ETag/Last-Modified 304 |

```yaml
# 版本化资源：文件名带哈希 → 内容变则 URL 变 → 可放心长缓存
spring:
  web:
    resources:
      chain:
        strategy:
          content:
            enabled: true
            paths: /static/**
      cache:
        cachecontrol:
          max-age: 30d
```

> 🎯 **版本化 = 长缓存的钥匙**：`app.abc123.js`（内容哈希）→ 文件不变 URL 不变（浏览器用缓存）、文件变 URL 变（浏览器重新拉）——规避"缓存了旧 JS"与"每次重新下载"两个极端。

### 3.2 动态资源缓存（API）

```java
// API 响应缓存：ETag 协商（Boot 内置支持）
@GetMapping("/config")
public ResponseEntity<ConfigVO> config() {
    ConfigVO vo = configService.get();
    return ResponseEntity.ok()
            .eTag(String.valueOf(vo.getVersion()))     // 版本号做 ETag
            .cacheControl(CacheControl.maxAge(60, TimeUnit.SECONDS))
            .body(vo);
}
// 客户端带 If-None-Match → 匹配则 304，省流量
```

## 4. WebJars 前端依赖

```xml
<!-- Maven 引入前端库（自动托管到 /webjars/**） -->
<dependency>
    <groupId>org.webjars</groupId>
    <artifactId>jquery</artifactId>
    <version>3.7.1</version>
</dependency>
```

```html
<!-- 引用方式：自动版本号解析（无需写版本） -->
<script src="/webjars/jquery/jquery.min.js"></script>
```

| 要点 | 说明 |
|------|------|
| 原理 | jar 内 `META-INF/resources/webjars/` 被默认静态扫描覆盖 |
| 版本解析 | Boot 支持 `webjars-locator` 去掉版本号引用 |
| 现状 | 前后端分离后使用率下降；遗留系统（JSP 时代）仍常见 |

## 5. SPA history 路由的三条出路

> ⚠️ 先说结论（2026-08 验证）：**Boot 4 / Framework 7 没有内置的 SPA fallback 配置项**——history 模式路由（`/user/123` 直达刷新 → 后端无此路径 → 404）需要自己解决。三种方案见下。

### 方案一：自定义 PathResourceResolver 兜底（后端解决，适合单 jar 部署）

```java
@Configuration
public class SpaConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    // 未命中静态文件时 → 交给前端路由（返回 index.html）
                    @Override
                    protected Resource getResource(String resourcePath, Resource location)
                            throws IOException {
                        Resource requested = location.createRelative(resourcePath);
                        if (requested.exists() && requested.isReadable()) {
                            return requested;                 // 真实文件（JS/CSS/图片）
                        }
                        Resource index = location.createRelative("index.html");
                        return index.exists() ? index : null; // 兜底到 SPA 入口
                    }
                });
    }
}
```

> ⚠️ 方案一的坑：`/**` 会接管一切未匹配请求——**必须放最后**（MVC 路由优先），且 API 接口 404 也会被"兜底成 index.html"（前端路由会再 404 一次，可接受，但语义不干净）。注意与 [05-错误处理机制深潜](05-错误处理机制深潜.md) 的 /error 兜底配合。

### 方案二：网关/Nginx 层解决（推荐，前后端分离标准姿势）

```nginx
# Nginx：静态文件优先，未命中 → index.html（SPA history 路由）
location / {
    root /data/spa;
    try_files $uri $uri/ /index.html;      # 关键一行
}

location /api/ {
    proxy_pass http://backend:8080/;       # API 反向代理到 Boot
}
```

| 优点 | 说明 |
|------|------|
| 职责分离 | SPA 由静态服务器托管，Boot 只出 API |
| 天然正确 | API 404 不会被 SPA 吞掉（路径隔离） |
| 性能 | 静态文件由 Nginx 直出，不占应用线程 |
| 缓存 | 静态缓存规则在 Nginx 统一管理 |

### 方案三：API 与前端完全分离部署（终极形态）

```text
CDN/对象存储（静态资源，全球加速）
   └─ 前端 SPA（history 路由由托管平台解决，如 S3 + CloudFront 404 回退）
API 域（api.example.com）→ Nginx → Boot 集群（只出 JSON）
```

> 🎯 选型结论：**单 jar 全栈（含前端）**用方案一；**标准前后端分离**用方案二；**大规模/ToC** 用方案三。面试答"SPA 路由 404 怎么解决"时，先讲方案二（Nginx try_files）是行业标准答案。

## 6. 前后端分离架构模式

```text
                   ┌─────────────────────────────────┐
 浏览器 ──> Nginx ─┼─ /       → SPA 静态文件（try_files 兜底）
                   ┼─ /api/** → Boot 集群（JSON）
                   └─────────────────────────────────┘
                          │
                          └─ CORS：同域反向代理（/api 前缀）→ 无需跨域配置！
```

| 架构决策 | 推荐 | 原因 |
|---------|------|------|
| 前端与 API 同域？ | **同域（/api 前缀代理）** | 消灭 CORS、Cookie 直接可用 |
| 跨域 | 仅当前后端不同域 | addCorsMappings 白名单（见 03 §3） |
| 静态资源放哪 | Nginx/CDN | 应用服务器只处理 API |
| Boot 里放前端 | 仅小工具/内网项目 | 方案一实现 |

> 💡 关联体系：静态资源缓存与访问控制再深挖见 [Nginx 体系](../Nginx/00-Nginx知识体系总览.md)；网关统一入口见 Spring Cloud Gateway 体系。

---

**下一模块**：[07-部署形态与HTTPS配置](07-部署形态与HTTPS配置.md) / **返回总览**：[00-SpringBootWeb总览](00-SpringBootWeb总览.md)
