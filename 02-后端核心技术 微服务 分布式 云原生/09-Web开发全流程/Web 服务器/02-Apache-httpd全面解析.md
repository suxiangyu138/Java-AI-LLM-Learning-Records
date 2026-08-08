# Apache httpd 全面解析
> 统治互联网二十年的服务器：模块化架构、MPM 三模型、虚拟主机、.htaccess 生态——以及它为何从霸主滑落到老三

## 📚 目录
1. [现状：2.4 的十五年长跑](#1-现状24-的十五年长跑)
2. [模块化架构](#2-模块化架构)
3. [MPM：三种进程模型](#3-mpm三种进程模型)
4. [虚拟主机](#4-虚拟主机)
5. [配置体系：httpd.conf 与 .htaccess](#5-配置体系httpdconf-与-htaccess)
6. [常用模块清单](#6-常用模块清单)
7. [Apache 的今天：还有谁在用](#7-apache-的今天还有谁在用)

## 1. 现状：2.4 的十五年长跑

| 事实 | 数值 |
|------|------|
| 当前稳定版 | **2.4.68**（2026-06-08 发布，安全+功能更新） |
| 2.4 分支年龄 | 2012-02-21 首发，**14 年**仍在积极维护 |
| 2.5/3.0 | 无发布计划（trunk 持续演进但无新大版本） |
| 全站份额（W3Techs 2026-08） | **23.1%**（老三，仍大于 Caddy/IIS 之和） |
| Top 1k 站点份额 | **11.9%**（头部流失严重） |
| 运行要求 | APR 1.5+ / APR-Util 1.5+（Apache 可移植运行库） |

> 🎯 一句话：Apache 是**"活化石级成熟"**——2.4 分支十年如一日地稳，但新特性/新生态基本停滞；存量庞大，增量萎缩。选它=选稳定与兼容，不是选性能与创新。

## 2. 模块化架构

```text
Apache 架构 = 核心（core）+ 60+ 可选模块
                │
                ├─ 静态编译：编译期打入（-static）
                └─ DSO 动态加载：LoadModule 运行时加载（主流）
```

| 模块 | 职责 |
|------|------|
| mod_ssl | TLS 终止（OpenSSL） |
| mod_rewrite | URL 重写（最著名的模块） |
| mod_proxy / mod_proxy_http | 反向代理 |
| mod_php / mod_wsgi / mod_fcgid | PHP/Python 动态处理 |
| mod_deflate | gzip 压缩 |
| mod_cache / mod_cache_disk | 缓存 |
| mod_security | WAF（OWASP CRS） |
| mod_http2 | HTTP/2（v2.0.39 仍在持续回填） |

> 💡 模块化是 Apache 的立身之本：**按需加载 = 精简内存**；mod_php 这类"解释器打进服务器进程"的设计也是它当年大获成功（一条命令起 PHP 站）后来被诟病（进程膨胀）的根源。

## 3. MPM：三种进程模型

```conf
# 选型配置（编译时或 httpd.conf）：
# 1. prefork：多进程单线程（默认，老而稳）
<IfModule mpm_prefork_module>
    StartServers 5
    MinSpareServers 5
    MaxSpareServers 10
    MaxRequestWorkers 256     # 最大进程数
</IfModule>

# 2. worker：多进程多线程
<IfModule mpm_worker_module>
    ServerLimit 16
    MaxRequestWorkers 400     # 线程总数
</IfModule>

# 3. event：worker + 事件循环（2.4 起可用，处理 keep-alive 空闲连接）
<IfModule mpm_event_module>
    StartServers 3
    MaxRequestWorkers 400
</IfModule>
```

| MPM | 模型 | 1 连接成本 | 适用 | 状态 |
|-----|------|:---:|------|:---:|
| **prefork** | 1 进程 1 线程 | 进程级（最重） | mod_php 老栈、线程不安全模块 | 默认/兼容 |
| **worker** | 1 线程 | 线程级 | 多数场景 | 可用 |
| **event** | 事件 + 线程 | 空闲连接零占用 | **高并发推荐** | 2.4 起推荐 |

> 🎯 结论：**Apache 也能高并发——用 event MPM**。它的历史包袱不是"做不到"，而是"默认 prefork 时做不到"。生产 Apache 必选 event MPM（除非强依赖 mod_php 线程不安全的老 PHP）。

## 4. 虚拟主机

```conf
# 基于域名的虚拟主机（Name-based，主流）
<VirtualHost *:80>
    ServerName www.example.com
    ServerAlias example.com
    DocumentRoot /var/www/example
    ErrorLog logs/example-error.log
</VirtualHost>

<VirtualHost *:80>
    ServerName api.example.com
    ProxyPass / http://127.0.0.1:8080/     # 反代到应用
    ProxyPassReverse / http://127.0.0.1:8080/
</VirtualHost>

# 基于 IP 的虚拟主机（IP-based）：Listen 多 IP + <VirtualHost 192.168.1.10:80>
```

| 类型 | 判定依据 | 场景 |
|------|---------|------|
| Name-based | Host 头 | **主流**（一个 IP 挂无数域名） |
| IP-based | 源 IP:端口 | 特殊合规/老系统 |

> 💡 虚拟主机是"一台服务器托管多站点"的标准答案（共享主机时代的核心能力）——Nginx 的 `server {}` 块与它一一对应。

## 5. 配置体系：httpd.conf 与 .htaccess

| 配置层级 | 位置 | 生效范围 | 特点 |
|---------|------|---------|------|
| **全局** | httpd.conf | 整个服务器 | 主配置 |
| **虚拟主机** | `<VirtualHost>` | 单站点 | 站点级覆盖 |
| **目录级** | `<Directory>` | 目录树 | 权限/选项控制 |
| **.htaccess** | 站点目录内 | 该目录及子目录 | **运行时解析，每请求生效** |

```conf
# httpd.conf 目录级配置
<Directory /var/www/example>
    Options -Indexes               # 禁止目录列表
    AllowOverride All              # 允许 .htaccess（关闭可提速）
    Require all granted
</Directory>
```

> ⚠️ **.htaccess 的性能代价**：Apache 对每个请求都会向上查找并解析所有 .htaccess 文件——高流量站点应 `AllowOverride None` 并全部收敛到 httpd.conf。这是 Apache vs Nginx 的一个经典性能差异点（Nginx 没有 .htaccess 概念，配置只能由管理员改）。

## 6. 常用模块清单

| 用途 | 模块 | 速记 |
|------|------|------|
| URL 重写 | mod_rewrite | `RewriteRule ^/user/(\d+)$ /user?id=$1` |
| 反代 | mod_proxy | `ProxyPass /api/ http://backend:8080/api/` |
| HTTPS | mod_ssl | `SSLCertificateFile` / `SSLCertificateKeyFile` |
| 压缩 | mod_deflate | `AddOutputFilterByType DEFLATE application/json` |
| 认证 | mod_auth_basic | 基础认证（内网工具站） |
| 目录索引 | mod_autoindex | `Options Indexes` |
| 访问控制 | mod_authz_core | `Require ip 10.0.0.0/8` |
| HTTP/2 | mod_http2 | `Protocols h2 http/1.1` |

> 🎯 记住两条最常用的：**mod_rewrite（路由/伪静态）** 与 **mod_proxy（反代）**——Apache 场景 80% 需求这两者解决。

## 7. Apache 的今天：还有谁在用

| 场景 | 为什么还用 Apache |
|------|------------------|
| 共享主机托管 | .htaccess 让用户自助配置（Nginx 无法替代） |
| 老 PHP 栈（LAMP） | mod_php 一键集成、文档海量 |
| 传统企业内网 | 稳定 + 团队熟悉 + 合规认证（2.4 持续安全更新） |
| mod_security WAF | 老牌 WAF 生态 |
| CMS 托管（WordPress 老主机） | 生态配套（cPanel 等控制面板基于 Apache） |

```text
何时换掉 Apache？
  · 高并发新站 → Nginx/Caddy
  · 需要 .htaccess 自助 → 保留 Apache（或 Nginx + 兼容层）
  · 新项目 → 默认 Nginx/Caddy，Apache 不选
```

> ⚠️ 2026 年选型提醒：Apache 2.4 无新大版本，**新增功能基本停摆**；新项目上 Apache 的唯一理由是"存量生态绑定"。迁移到 Nginx 的要点见 [06-选型决策与生产实践](06-选型决策与生产实践.md)。

---

**下一模块**：[03-Caddy与自动HTTPS时代](03-Caddy与自动HTTPS时代.md) / **返回总览**：[00-Web服务器总览](00-Web服务器总览.md)
