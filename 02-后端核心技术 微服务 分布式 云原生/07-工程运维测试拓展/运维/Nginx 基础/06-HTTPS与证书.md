# 06-HTTPS 与证书
> TLS 证书配置、HTTP→HTTPS 跳转、TLS 性能优化、HTTP/2、证书管理——"接入层集中做 TLS 终止"

## 📚 目录
1. [HTTPS 基础与证书类型](#1-https-基础与证书类型)
2. [HTTPS 服务器配置](#2-https-服务器配置)
3. [HTTP → HTTPS 跳转](#3-http--https-跳转)
4. [TLS 性能优化](#4-tls-性能优化)
5. [HTTP/2 配置](#5-http2-配置)
6. [证书管理（签发/续期/监控）](#6-证书管理签发续期监控)
7. [常见 HTTPS 问题](#7-常见-https-问题)
8. [核心要点](#8-核心要点)
9. [参考来源](#9-参考来源)

## 1. HTTPS 基础与证书类型

```text
HTTPS = HTTP + TLS（加密传输）
Nginx 的角色：TLS 终止（证书集中管理，后端不感知加密）

证书类型：
  单域名证书：example.com
  泛域名证书：*.example.com（推荐多子域）
  多域名证书：SAN 含多个域名
```

| 证书来源 | 适用 |
|----------|------|
| Let's Encrypt（免费，90 天） | 公网站点（自动续期） |
| 云厂商证书 | 云上（阿里云/腾讯云免费证书） |
| 自签名 | 内网测试（客户端需信任） |
| 商业证书 | 高信任要求 |

## 2. HTTPS 服务器配置

```nginx
server {
    listen 443 ssl;
    server_name example.com;

    # 证书（Nginx 集中管理）
    ssl_certificate     /etc/nginx/certs/example.com.crt;
    ssl_certificate_key /etc/nginx/certs/example.com.key;

    # 协议与加密套件
    ssl_protocols TLSv1.2 TLSv1.3;          # 禁用 TLS1.0/1.1（不安全）
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;

    # 会话复用（性能）
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 10m;

    location / {
        proxy_pass http://backend;
    }
}
```

| 配置 | 建议 |
|------|------|
| `ssl_certificate` | 证书链文件（crt） |
| `ssl_certificate_key` | 私钥（**权限 600**） |
| `ssl_protocols` | TLSv1.2 TLSv1.3（禁旧版） |
| `ssl_session_cache` | 会话复用（性能关键） |
| `ssl_prefer_server_ciphers` | 服务端优先套件 |

> ⚠️ **私钥安全**：`chmod 600` 私钥文件；证书链要完整（缺中间证书 = 移动端/部分浏览器报错）。

## 3. HTTP → HTTPS 跳转

```nginx
# 方案 1：单独 server 跳转（推荐）
server {
    listen 80;
    server_name example.com;
    return 301 https://$host$request_uri;    # 全部跳 HTTPS
}

# 方案 2：同 server 内（不推荐，混用）
server {
    listen 80;
    listen 443 ssl;
    # ...
}
```

```text
跳转注意：
  301 永久跳转（SEO 友好）
  $request_uri 保留原路径参数
  部分场景（支付回调/兼容）可 302 临时
```

> 🎯 **HTTPS 全站跳转**：80 端口 server 统一 `return 301 https://$host$request_uri;`——**保留路径与参数**（`$request_uri` 而非 `$uri`，后者丢参数）。

## 4. TLS 性能优化

| 优化项 | 配置 | 收益 |
|--------|------|------|
| 会话复用 | `ssl_session_cache shared:SSL:10m` | 减少握手次数 |
| 会话超时 | `ssl_session_timeout 10m` | 复用窗口 |
| TLS 票据 | `ssl_session_tickets on` | 多 worker 共享 |
| OCSP 装订 | `ssl_stapling on` | 证书状态缓存 |
| 协议 | TLS 1.3（握手 1-RTT） | 显著提速 |

```nginx
# 完整性能配置
ssl_session_cache shared:SSL:10m;
ssl_session_timeout 10m;
ssl_session_tickets on;
ssl_stapling on;
ssl_stapling_verify on;
ssl_protocols TLSv1.2 TLSv1.3;
```

> 💡 **TLS 1.3 的价值**：握手从 2-RTT 降到 1-RTT（会话恢复 0-RTT）——**升级 TLS1.3 是最有效的 HTTPS 提速**。

## 5. HTTP/2 配置

```nginx
server {
    listen 443 ssl http2;      # HTTP/2（TLS 上启用）
    server_name example.com;
    # ...
}
```

```text
HTTP/2 能力：
  多路复用（单连接并行请求）
  头部压缩（HPACK）
  服务端推送（已废弃，HTTP/3 移除）

注意：
  HTTP/2 需要 TLS（Nginx 实现）
  与后端通信仍 HTTP/1.1（proxy_http_version 1.1）
```

| 对比 | HTTP/1.1 | HTTP/2 |
|------|----------|--------|
| 连接 | 每请求排队 | 多路复用 |
| 队头阻塞 | 有 | 传输层仍有（TCP） |
| 头部 | 明文重复 | HPACK 压缩 |
| 配置 | - | `listen 443 ssl http2` |

> ⚠️ HTTP/2 的队头阻塞仍在 TCP 层——HTTP/3（QUIC）才彻底解决（Nginx 1.25+ 实验支持 QUIC/HTTP3）。

## 6. 证书管理（签发/续期/监控）

### 6.1 Let's Encrypt 自动续期

```bash
# certbot 签发 + 自动续期
apt install certbot python3-certbot-nginx
certbot --nginx -d example.com -d www.example.com

# 自动续期（cron/timer 内置）
certbot renew --dry-run    # 测试续期
```

### 6.2 证书监控（关键）

```text
证书过期 = 定时炸弹（服务中断事故）
监控：证书 7 天内过期告警（见日志监控指标体系）

手动检查：
openssl s_client -connect example.com:443 2>/dev/null \
  | openssl x509 -noout -dates
```

> ⚠️ **证书纪律**：自动续期（certbot）+ 过期告警（7 天）+ 私钥权限 600——三件套缺一不可；自签名证书仅内网测试。

## 7. 常见 HTTPS 问题

| 问题 | 原因 | 解决 |
|------|------|------|
| 证书错误（部分设备） | 证书链不完整（缺中间证书） | 拼接完整链 |
| 证书过期 | 未续期/未监控 | certbot 自动续期 + 告警 |
| 域名不匹配 | SAN 不含访问域名 | 泛域名证书/补 SAN |
| TLS 握手失败 | 协议版本不兼容（客户端太老） | 保留 TLS1.2 兼容 |
| 混合内容 | 页面 http 资源被拦 | 全站跳转 + 资源改 https |
| 私钥泄露 | 权限不足/误传 | chmod 600 + 轮换 |

## 8. 核心要点

> 🎯 **核心要点**：
> - Nginx 做 TLS 终止：证书集中管理，后端无感知；
> - 全站跳转：80 端口 `return 301 https://$host$request_uri;`（保留路径参数）；
> - 安全基线：TLSv1.2/1.3 + 私钥 600 + 完整证书链；
> - 性能三件套：会话复用（cache）+ TLS1.3 + HTTP/2（`listen 443 ssl http2`）；
> - 证书纪律：certbot 自动续期 + 7 天过期告警；
> - 排查：证书链/过期/SAN/混合内容是四大高频问题。

## 9. 参考来源

- [Nginx SSL 模块文档](https://nginx.org/en/docs/http/ngx_http_ssl_module.html)
- [Nginx HTTP/2 文档](https://nginx.org/en/docs/http/ngx_http_v2_module.html)
- [certbot 官方文档](https://certbot.eff.org/)
- [Mozilla SSL 配置生成器](https://ssl-config.mozilla.org/)

---

**下一模块**：[07-缓存配置](07-缓存配置.md)　/　**返回总览**：[00-总览](00-Nginx知识体系总览.md)
