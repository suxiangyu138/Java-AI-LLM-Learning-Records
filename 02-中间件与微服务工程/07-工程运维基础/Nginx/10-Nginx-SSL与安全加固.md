# 10 - Nginx SSL 与安全加固

> 🎯 HTTPS 已是生产标配，安全加固是守门人的天职 — 从 SSL 证书配置到 HSTS/CSP 安全头、从限流防刷到 WAF 防护，让 Nginx 真正成为应用的第一道防线

---

## 目录

1. [HTTPS 与 SSL/TLS 配置](#1-https-与-ssltls-配置)
2. [HTTPS 安全增强](#2-https-安全增强)
3. [HTTP 安全头](#3-http-安全头)
4. [限流与防刷](#4-限流与防刷)
5. [防盗链与访问控制](#5-防盗链与访问控制)
6. [基础 WAF 规则](#6-基础-waf-规则)
7. [安全配置自查清单](#7-安全配置自查清单)

---

## 1. HTTPS 与 SSL/TLS 配置

### 1.1 基本 HTTPS 配置

```nginx
server {
    listen 443 ssl http2;
    server_name api.example.com;

    # ═══ 证书配置 ═══
    ssl_certificate     /etc/nginx/ssl/fullchain.pem;
    ssl_certificate_key /etc/nginx/ssl/privkey.pem;

    # ═══ 协议与加密套件 ═══
    ssl_protocols TLSv1.2 TLSv1.3;                # 禁用 TLSv1.0/1.1
    ssl_ciphers ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384;
    ssl_prefer_server_ciphers on;

    # ═══ Session 优化 ═══
    ssl_session_cache   shared:SSL:10m;            # 10MB ≈ 40000 sessions
    ssl_session_timeout 10m;

    location / {
        proxy_pass http://java_backend;
    }
}

# ═══ HTTP → HTTPS 强制跳转 ═══
server {
    listen 80;
    server_name api.example.com;
    return 301 https://$host$request_uri;
}
```

### 1.2 Let's Encrypt 免费证书 + 自动续期

```bash
# 安装 certbot
yum install -y certbot python3-certbot-nginx   # CentOS
apt install -y certbot python3-certbot-nginx   # Ubuntu

# 申请证书（自动配置 Nginx）
certbot --nginx -d api.example.com -d www.example.com

# 自动续期（添加到 crontab）
0 3 * * * certbot renew --quiet --post-hook "nginx -s reload"
```

### 1.3 SSL 安全等级检测

```bash
# 在线检测
# https://www.ssllabs.com/ssltest/

# 命令行检测
openssl s_client -connect api.example.com:443 -tls1_2
nmap --script ssl-enum-ciphers -p 443 api.example.com
```

---

## 2. HTTPS 安全增强

### 2.1 HSTS（HTTP Strict Transport Security）

```nginx
add_header Strict-Transport-Security "max-age=63072000; includeSubDomains; preload" always;
```

| 参数 | 说明 |
|------|------|
| `max-age` | 浏览器强制 HTTPS 的时长（秒），推荐 ≥ 1年（31536000） |
| `includeSubDomains` | 子域名也强制 HTTPS |
| `preload` | 加入浏览器 HSTS Preload 列表 |

### 2.2 OCSP Stapling

```nginx
ssl_stapling on;
ssl_stapling_verify on;
ssl_trusted_certificate /etc/nginx/ssl/fullchain.pem;
resolver 8.8.8.8 8.8.4.4 valid=300s;
resolver_timeout 5s;
```

> 💡 OCSP Stapling 让 Nginx 代替客户端查询证书吊销状态，减少客户端验证延迟。

---

## 3. HTTP 安全头

```nginx
# ═══ 推荐的安全头组合 ═══
server {
    # XSS 防护
    add_header X-XSS-Protection "1; mode=block" always;

    # 禁止 MIME 类型嗅探
    add_header X-Content-Type-Options "nosniff" always;

    # 控制 frame 嵌入（防点击劫持）
    add_header X-Frame-Options "SAMEORIGIN" always;

    # Referrer 策略
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;

    # 权限策略
    add_header Permissions-Policy "geolocation=(), microphone=(), camera=()" always;

    # CSP（内容安全策略）— 防 XSS 的终极方案
    add_header Content-Security-Policy "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:;" always;
}
```

| 安全头 | 防护类型 | 优先级 |
|--------|----------|:---:|
| `Strict-Transport-Security` | SSL 降级攻击 | 🔴 必配 |
| `X-Content-Type-Options` | MIME 嗅探攻击 | 🔴 必配 |
| `X-Frame-Options` | 点击劫持 | 🟡 |
| `X-XSS-Protection` | 反射型 XSS | 🟡 |
| `Content-Security-Policy` | XSS / 数据注入 | 🟡（复杂） |
| `Referrer-Policy` | 信息泄漏 | 🟢 |

---

## 4. 限流与防刷

### 4.1 基于 IP 的请求限流

```nginx
http {
    # 定义限流区域：10MB 可存约 16 万个 IP
    limit_req_zone $binary_remote_addr zone=api_limit:10m rate=10r/s;

    # 突发流量队列
    limit_req_zone $binary_remote_addr zone=burst_limit:10m rate=5r/s;

    server {
        location /api/ {
            # 基本限流
            limit_req zone=api_limit burst=20 nodelay;
            proxy_pass http://java_backend;
        }

        location /api/login {
            # 登录接口严格限流 — 防暴力破解
            limit_req zone=burst_limit burst=3 nodelay;
            proxy_pass http://java_backend;
        }
    }
}
```

| 参数 | 说明 |
|------|------|
| `zone=api_limit:10m` | 共享内存区域名 + 大小 |
| `rate=10r/s` | 每秒最多 10 个请求 |
| `burst=20` | 允许 20 个请求排队 |
| `nodelay` | 队列满后立即返回 503 |

### 4.2 并发连接数限制

```nginx
http {
    limit_conn_zone $binary_remote_addr zone=conn_limit:10m;

    server {
        location / {
            limit_conn conn_limit 10;              # 单 IP 最多 10 个并发连接
            limit_rate 500k;                        # 单连接限速 500KB/s
            proxy_pass http://java_backend;
        }

        location /download/ {
            limit_rate_after 10m;                   # 前 10MB 不限速
            limit_rate 1m;                          # 之后限速 1MB/s
        }
    }
}
```

### 4.3 防 DDoS 基础配置

```nginx
# ═══ 连接超时优化 ═══
client_body_timeout 10s;
client_header_timeout 10s;
keepalive_timeout 30s;
send_timeout 10s;

# ═══ 请求体大小限制 ═══
client_max_body_size 10m;
client_body_buffer_size 128k;

# ═══ 限制请求速率 ═══
limit_req_zone $binary_remote_addr zone=ddos:10m rate=30r/m;
```

---

## 5. 防盗链与访问控制

### 5.1 Referer 防盗链

```nginx
location ~* \.(jpg|jpeg|png|gif|webp|svg|mp4)$ {
    valid_referers none blocked *.example.com ~\.google\. ~\.bing\.;
    if ($invalid_referer) {
        return 403;
        # rewrite ^/ /images/hotlink-denied.jpg last;  # 或返回提示图
    }
}
```

### 5.2 IP 白名单/黑名单

```nginx
# ═══ 白名单：仅允许指定 IP ═══
location /admin/ {
    allow 192.168.1.0/24;
    allow 10.0.0.0/8;
    deny all;
    proxy_pass http://java_backend;
}

# ═══ 黑名单：拦截恶意 IP ═══
location / {
    deny 123.45.67.89;
    deny 98.76.54.0/24;
    # ... allow all implicit
    proxy_pass http://java_backend;
}
```

### 5.3 基于 Auth 的访问控制

```nginx
# Basic Auth
location /private/ {
    auth_basic "Restricted Area";
    auth_basic_user_file /etc/nginx/.htpasswd;
    proxy_pass http://java_backend;
}
```

```bash
# 生成密码文件
htpasswd -c /etc/nginx/.htpasswd admin
```

---

## 6. 基础 WAF 规则

### 6.1 拦截常见攻击特征

```nginx
server {
    # ═══ 拦截 SQL 注入 ═══
    set $block_sql 0;
    if ($query_string ~* "union.*select.*(") { set $block_sql 1; }
    if ($query_string ~* "union.*all.*select.*") { set $block_sql 1; }
    if ($args ~* "(<|%3C)script") { set $block_sql 1; }
    if ($block_sql = 1) { return 403; }

    # ═══ 拦截路径遍历 ═══
    if ($uri ~* "\.\./") { return 403; }

    # ═══ 拦截非法请求方法 ═══
    if ($request_method !~ ^(GET|HEAD|POST|PUT|DELETE|OPTIONS)$) {
        return 405;
    }

    # ═══ 隐藏 Nginx 版本号 ═══
    server_tokens off;

    location / {
        proxy_pass http://java_backend;
    }
}
```

### 6.2 ModSecurity WAF（专业方案）

```bash
# 编译安装 ModSecurity + Nginx 连接器
# 适合高安全要求的场景，提供 OWASP 核心规则集 (CRS)
# 轻量场景用上面的 Nginx 原生规则即可
```

---

## 7. 安全配置自查清单

| 分类 | 检查项 | 配置 |
|------|--------|------|
| **HTTPS** | 启用 TLSv1.2/1.3 | `ssl_protocols TLSv1.2 TLSv1.3;` |
| **HTTPS** | HTTP 强制跳转 HTTPS | `return 301 https://...` |
| **HTTPS** | HSTS 头 | `add_header Strict-Transport-Security ...` |
| **HTTPS** | 证书自动续期 | certbot + crontab |
| **安全头** | X-Content-Type-Options | `nosniff` |
| **安全头** | X-Frame-Options | `SAMEORIGIN` |
| **信息隐藏** | 隐藏版本号 | `server_tokens off;` |
| **信息隐藏** | 自定义错误页 | `error_page 500 502 503 504 /50x.html;` |
| **限流** | 登录接口限流 | `limit_req` 严格速率 |
| **限流** | 并发连接限制 | `limit_conn` |
| **访问控制** | 管理后台 IP 白名单 | `allow/deny` |
| **请求限制** | 请求体大小限制 | `client_max_body_size` |
| **请求限制** | 非法 Method 拦截 | `if ($request_method ...)` |
| **WAF** | SQL 注入/XSS 基础拦截 | 正则匹配关键字符 |

> 🎯 **安全是 1，其他是 0** — 没有安全的性能优化和架构设计都是空中楼阁。Nginx 作为所有流量的入口，是安全防护的理想位置。
