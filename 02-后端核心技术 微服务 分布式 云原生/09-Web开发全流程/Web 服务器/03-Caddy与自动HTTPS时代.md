# Caddy 与自动 HTTPS 时代
> 现代 Web 服务器的"极简主义"答案：Go 编写、自动 HTTPS、HTTP/3、几条 Caddyfile 起站——为什么它成为个人与中小站的首选

## 📚 目录
1. [Caddy 是什么：极简 + 自动化的定位](#1-caddy-是什么极简--自动化的定位)
2. [自动 HTTPS 全解析](#2-自动-https-全解析)
3. [Caddyfile：配置即代码](#3-caddyfile配置即代码)
4. [反向代理与负载均衡](#4-反向代理与负载均衡)
5. [HTTP/3 与前卫特性](#5-http3-与前卫特性)
6. [与 Nginx 对比](#6-与-nginx-对比)
7. [适用场景与避坑](#7-适用场景与避坑)

## 1. Caddy 是什么：极简 + 自动化的定位

| 事实 | 数值 |
|------|------|
| 当前版本 | **v2.11.4**（2026-07-02） |
| 语言 | Go（单二进制、零依赖、天然跨平台） |
| 许可证 | Apache-2.0（核心免费开源） |
| 份额 | 头部扫描约 3%（2026），上升最快的新服务器 |
| 定位 | 自动 HTTPS 一站式服务器（静态 + 反代） |

```text
Caddy 的设计哲学：
  "服务器配置应该比写个 JSON 配置还简单"
  · 域名一写 → HTTPS 自动搞定（不用 Certbot/cron/证书路径）
  · 配置即代码（Caddyfile）→ Git 化管理
  · 一个二进制 → 静态服务 + 反代 + TLS + HTTP/3
```

> 🎯 一句话：**Caddy 消灭了 HTTPS 配置**——Nginx 要写证书路径、续期脚本、重定向规则三件事，Caddy 写一个域名就全自动。这是它 2015 年发布后快速崛起、2026 年成为"新项目默认选项之一"的根本原因。

## 2. 自动 HTTPS 全解析

### 2.1 内置 ACME 客户端的完整生命周期

```text
配置里出现公共域名
  → 自动向 Let's Encrypt/ZeroSSL 申请证书
  → 到期前 30 天自动续期（无需重启/重载）
  → OCSP stapling 自动更新
  → HTTP → HTTPS 自动重定向
  → 全程零配置零脚本
```

### 2.2 三种 ACME 验证方式

| 方式 | 原理 | 适用 |
|------|------|------|
| **HTTP-01** | 80 端口放挑战文件 | 默认，需 80 可达 |
| **TLS-ALPN-01** | 443 端口特殊握手 | 默认，不依赖 80 |
| **DNS-01** | 写 DNS TXT 记录 | **通配符证书**、防火墙后、内网 |

```caddyfile
# 通配符证书：DNS-01 挑战（需 DNS 插件，如 Cloudflare）
example.com, *.example.com {
    tls {
        dns cloudflare {env.CF_API_TOKEN}
    }
}
```

### 2.3 本地/内网证书

```caddyfile
# localhost / 内网 IP / .local 域名：自动本地 CA + 自签证书
# Caddy 自动把 CA 装进系统信任库（Linux/macOS；Docker/CI 需手动装）
localhost:8443 {
    root * /var/www
    file_server
}
```

> 💡 内网 HTTPS 的痛点（自签证书不被信任、浏览器告警）被 Caddy 的"本地 CA + 自动安装"基本解决——内网系统、开发环境体验远超手写自签。

### 2.4 On-demand TLS（按需证书）

```caddyfile
# 多租户 SaaS：成百上千子域名，证书按首次握手即时签发（配 ask 端点防滥用）
{
    on_demand_tls {
        ask https://auth.example.com/check
    }
}
```

> ⚠️ On-demand TLS 必须配 `ask` 校验端点——否则任何人都能触发你向 ACME 申请证书（速率限制滥用/配额耗尽）。

## 3. Caddyfile：配置即代码

```caddyfile
# 一个静态站点 = 3 行
example.com {
    root * /var/www/example
    file_server
}

# 一个反代 = 2 行
api.example.com {
    reverse_proxy 127.0.0.1:8080
}

# 完整示例：静态 + 反代 + 缓存 + 安全头
app.example.com {
    # 静态资源直出（带缓存头）
    handle /static/* {
        root * /var/www/app/static
        file_server {
            cache-control max-age=30d
        }
    }
    # 其余反代到应用
    handle {
        reverse_proxy 127.0.0.1:8080
    }
    # 安全头
    header {
        X-Content-Type-Options nosniff
        X-Frame-Options DENY
        Referrer-Policy strict-origin-when-cross-origin
    }
    # 日志
    log {
        output file /var/log/caddy/app.log
    }
}
```

| Caddyfile 特性 | 说明 |
|---------------|------|
| 人类可读 | 比 Nginx 配置简洁 5-10 倍 |
| 分段指令 | `handle` 按前缀路由（避免 Nginx location 嵌套地狱） |
| 全局块 | `{}` 顶层全局配置（on_demand_tls 等） |
| 环境变量 | `{env.X}` 注入密钥 |
| 热重载 | `caddy reload` 零停机 |

> 🎯 与 Nginx 配置模型对比：Nginx 的 `location` 嵌套 + 上下文继承是学习曲线大头；Caddy 的 `handle` 是扁平化的"顺序匹配"——心智负担低一个量级。

## 4. 反向代理与负载均衡

```caddyfile
# 负载均衡：多后端 + 健康检查
api.example.com {
    reverse_proxy 10.0.1.10:8080 10.0.1.11:8080 {
        lb_policy round_robin
        health_uri /actuator/health
        health_interval 10s
    }
}

# WebSocket / SSE：原生支持（无需特殊配置）
ws.example.com {
    reverse_proxy 127.0.0.1:3000
}
```

| 能力 | Caddy | 说明 |
|------|:---:|------|
| 负载均衡 | ✅ | round_robin / least_conn / ip_hash / random |
| 健康检查 | ✅ | URI + 间隔配置 |
| WebSocket | ✅ | 自动升级 |
| SSE | ✅ | 流式透传 |
| gRPC | ✅ | h2c 支持 |
| 重试/熔断 | 部分 | 简单场景够用，复杂靠网关 |

> ⚠️ 定位提醒：Caddy 是"**好用够用的反代**"，不是"企业级网关"——服务发现、金丝雀、细粒度限流等复杂流量治理用 Envoy/网关（见 Spring Cloud Gateway 体系）。

## 5. HTTP/3 与前卫特性

| 特性 | 版本 | 说明 |
|------|------|------|
| **HTTP/3 (QUIC)** | 原生 | UDP 传输、0-RTT、连接迁移；配置零成本 |
| **ECH（加密 ClientHello）** | 2.10+ | 加密 TLS 握手中最后明文部分，隐藏 SNI |
| **PQC 后量子加密** | 2.10+ | 支持标准化 `x25519mlkem768` 密钥交换组 |
| **通配符证书默认** | 2.10+ | 子域名默认用通配证书（减少证书数） |
| ACME profiles | 2.10+ | 实验性证书属性支持 |

> 🎯 Caddy 是**新协议试验田**：HTTP/3、ECH、后量子加密这些"未来特性"它总是第一个落地——对追求前沿的中小站/个人项目是加分项，对追求稳的企业反而是"等 Nginx/云厂商跟进"的理由。

## 6. 与 Nginx 对比

| 维度 | Nginx | Caddy |
|------|:---:|:---:|
| 语言/依赖 | C / 生态庞大 | Go / 单二进制 |
| 配置 | 强大但陡峭（location 嵌套） | 极简（handle 扁平） |
| HTTPS | 手动（证书/续期/重定向三件事） | **全自动** |
| HTTP/3 | 主线 1.25+ 支持 | 原生默认 |
| 动态模块 | 编译期/部分热加载 | Go 插件（较少） |
| 生态 | **极丰富**（Lua/OpenResty/控制面板） | 轻量但增长快 |
| 性能 | 极致（多年打磨） | 优秀（同量级差距小） |
| 团队熟悉度 | 行业标准 | 新人友好 |
| 适合 | 生产大站/复杂架构 | 中小站/新项目/开发者自用 |

> 🎯 选型：**大规模/复杂生产 → Nginx（生态与成熟度）；中小站点/个人项目/内网 → Caddy（自动 HTTPS + 简单）**。二者不冲突：Caddy 可以接在 Nginx 后面只做 TLS。

## 7. 适用场景与避坑

| 场景 | 推荐 | 原因 |
|------|:---:|------|
| 个人博客/小站 | Caddy | 域名一写 HTTPS 全自动 |
| 内网工具（Jenkins/Grafana） | Caddy | 内网 CA 自动信任 |
| 多租户 SaaS 子域名 | Caddy | On-demand TLS |
| 企业大流量 | Nginx | 生态/团队/调优积累 |
| 云原生 K8s | 网关/Ingress | Caddy 可作出口反代 |

| 避坑 | 说明 |
|------|------|
| 升级不及时 | **v2.11.1 曾修复 6 个 CVE**（含 FastCGI RCE、CSRF 绕过）——Go 写的也不免疫，保持升级 |
| On-demand TLS 无 ask | 证书滥用（见 §2.4） |
| 80 端口被占 | HTTP-01 挑战失败 → 用 TLS-ALPN-01 或 DNS-01 |
| 企业要审计 | Caddy 新生态文档/认证资料少于 Nginx，合规场景慎选 |
| 误以为能替代网关 | 复杂流量治理仍需网关层（见 §4 提醒） |

---

**下一模块**：[04-IIS与Windows生态](04-IIS与Windows生态.md) / **返回总览**：[00-Web服务器总览](00-Web服务器总览.md)
