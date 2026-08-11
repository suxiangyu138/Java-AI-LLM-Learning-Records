# 02-静态资源与 location 匹配
> location 匹配优先级（面试必考）、静态资源服务、gzip 压缩、缓存头——"Nginx 最擅长的事：服务静态文件"

## 📚 目录
1. [location 匹配规则与优先级](#1-location-匹配规则与优先级)
2. [静态资源服务](#2-静态资源服务)
3. [gzip 压缩](#3-gzip-压缩)
4. [静态资源缓存头](#4-静态资源缓存头)
5. [try_files 与前端路由](#5-try_files-与前端路由)
6. [目录索引与防盗链](#6-目录索引与防盗链)
7. [核心要点](#7-核心要点)
8. [参考来源](#8-参考来源)

## 1. location 匹配规则与优先级

### 1.1 四种匹配

| 匹配符 | 含义 | 示例 |
|--------|------|------|
| `=` | 精确匹配 | `location = /login` |
| `^~` | 前缀匹配（命中即停） | `location ^~ /static/` |
| `~` | 正则匹配（区分大小写） | `location ~ \.png$` |
| `~*` | 正则匹配（忽略大小写） | `location ~* \.(jpg\|png)$` |
| 无符号 | 普通前缀匹配 | `location /api/` |

### 1.2 匹配优先级（面试必考）

```text
匹配顺序：
  ① = 精确匹配（命中直接使用，停止）
  ② ^~ 前缀匹配（命中直接使用，停止，不查正则）
  ③ ~ / ~* 正则匹配（按配置顺序第一个命中）
  ④ 普通前缀匹配（最长匹配）
  ⑤ 都没有 → /

记忆口诀：精确 > 前缀(^~) > 正则 > 最长前缀 > 默认
```

```nginx
location = /login        { ... }   # ① 精确（最高优先级）
location ^~ /static/    { ... }   # ② 前缀（命中即停）
location ~ \.php$       { ... }   # ③ 正则
location /api/          { ... }   # ④ 普通前缀（最长匹配）
location /              { ... }   # ⑤ 兜底
```

> ⚠️ **正则的坑**：正则按**配置文件中的顺序**匹配（第一个命中生效）——相同匹配度的正则，写在前面优先。

## 2. 静态资源服务

```nginx
server {
    listen 80;
    server_name static.example.com;
    root /usr/share/nginx/html;        # 站点根目录
    index index.html;

    location /static/ {
        alias /data/static/;           # alias：替换路径前缀
    }
    # root vs alias：
    #   root  /data/;        → /static/a.js  → /data/static/a.js（拼接）
    #   alias /data/static/; → /static/a.js  → /data/static/a.js（替换）
}
```

| 指令 | 路径处理 | 适用 |
|------|---------|------|
| `root` | 路径**拼接**（root + URI） | 站点根目录 |
| `alias` | 路径**替换**（alias 替换 location 前缀） | 指定目录映射 |

> 💡 root vs alias 是高频考点：**root 拼接、alias 替换**——`location /static/` + `alias /data/` 时，`/static/a.js` → `/data/a.js`。

## 3. gzip 压缩

```nginx
http {
    gzip on;
    gzip_types text/plain text/css application/json application/javascript
               application/xml image/svg+xml;
    gzip_min_length 1k;          # 小于 1k 不压缩（小文件压缩无收益）
    gzip_comp_level 5;           # 压缩级别（1-9，5 平衡）
    gzip_vary on;                # 响应头 Vary: Accept-Encoding
    # 已压缩格式不再压缩（图片/视频）
    gzip_types 中不要包含 image/jpeg 等已压缩格式
}
```

| 配置 | 建议 |
|------|------|
| `gzip on` | 开启 |
| 压缩类型 | 文本/JSON/CSS/JS（**不含图片视频**） |
| `gzip_min_length` | 1k（小文件不压） |
| `gzip_comp_level` | 5（平衡 CPU 与压缩率） |

> 🎯 gzip 的收益：**JSON/JS/CSS 压缩率 60-80%**——接入层开启后全站响应体积大幅下降；动态接口也受益（代理响应可压缩）。

## 4. 静态资源缓存头

```nginx
location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff2?)$ {
    expires 30d;                 # 浏览器缓存 30 天
    add_header Cache-Control "public, max-age=2592000";
    # 或
    # expires -1;                # 不缓存（HTML 页面）
}
```

| 资源类型 | 缓存策略 |
|----------|---------|
| HTML | 不缓存或短缓存（`expires -1` / `no-cache`） |
| JS/CSS | 长缓存（30d）+ 文件名带版本/hash |
| 图片/字体 | 长缓存（30-365d） |

```text
静态资源缓存最佳实践：
  HTML 不缓存（内容会变）
  带 hash 的文件名（webpack 产物）→ 长缓存 365d
  不带 hash 的文件 → 短缓存 + 版本号
```

> ⚠️ **缓存与更新的矛盾**：长缓存 + 文件名 hash 是标准解（hash 变化 = 新文件 = 新缓存）；**不要对 HTML 长缓存**（更新后用户拿旧页面）。

## 5. try_files 与前端路由

```nginx
# 前端 SPA（Vue/React）：路由刷新 404 问题
server {
    root /data/frontend/dist;
    index index.html;

    location / {
        # 尝试：文件 → 目录 → 全部回退到 index.html（SPA 路由）
        try_files $uri $uri/ /index.html;
    }
}
```

```text
try_files 语法：try_files file1 file2 ... fallback;
  按顺序尝试文件是否存在
  $uri：请求路径
  $uri/：目录
  /index.html：回退（SPA 入口）
```

> 🎯 **SPA 刷新 404 的解法**：`try_files $uri $uri/ /index.html`——前端路由（history 模式）刷新时服务端没有对应文件，回退到入口页由前端路由接管。

## 6. 目录索引与防盗链

```nginx
# 目录索引（禁止默认列出目录）
location / {
    autoindex off;            # 默认关闭（安全）
}

# 防盗链（图片/资源）
location ~* \.(png|jpg|jpeg|gif)$ {
    valid_referers none blocked *.example.com;
    if ($invalid_referer) {
        return 403;
    }
}

# 禁止访问隐藏文件
location ~ /\. {
    deny all;
}
```

| 安全配置 | 说明 |
|----------|------|
| `autoindex off` | 禁止目录列表（默认） |
| `valid_referers` | 防盗链（允许的来源） |
| `deny all` | 禁止访问 `.` 开头文件（.env/.git） |

## 7. 核心要点

> 🎯 **核心要点**：
> - location 优先级口诀：**精确 > 前缀(^~) > 正则 > 最长前缀 > 默认**（面试必考）；
> - root 拼接 vs alias 替换（高频考点）；
> - gzip：文本类压缩（JS/JSON 省 60-80%），图片视频不压；
> - 缓存策略：HTML 不缓存 + hash 文件名长缓存；
> - try_files 解决 SPA 刷新 404（前端路由）；
> - 安全三件：autoindex off、防盗链 valid_referers、deny 隐藏文件。

## 8. 参考来源

- [Nginx location 官方文档](https://nginx.org/en/docs/http/ngx_http_core_module.html#location)
- [Nginx gzip 模块文档](https://nginx.org/en/docs/http/ngx_http_gzip_module.html)
- [Nginx try_files 文档](https://nginx.org/en/docs/http/ngx_http_core_module.html#try_files)

---

**下一模块**：[03-反向代理配置](03-反向代理配置.md)　/　**返回总览**：[00-总览](00-Nginx知识体系总览.md)
