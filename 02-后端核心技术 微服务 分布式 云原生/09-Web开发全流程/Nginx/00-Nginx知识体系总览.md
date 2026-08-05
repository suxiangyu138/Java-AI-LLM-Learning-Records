# 00 - Nginx 知识体系总览

> 🎯 Nginx 是 Java 后端架构的"守门人"和"调度员" — 从静态资源到反向代理、从负载均衡到高可用集群，掌握 Nginx 是后端工程师从"会写代码"到"能扛流量"的分水岭

---

## 目录

1. [知识全景](#1-知识全景)
2. [文件导航](#2-文件导航)
3. [精通级学习路线](#3-精通级学习路线)

---

## 1. 知识全景

```
Nginx 精通体系（12个文件）
│
├── 🏗️ 基础入门（01-03）
│   ├── 01-Nginx概述与核心原理.md      # 是什么/能做什么/进程模型/事件驱动
│   ├── 02-Nginx安装与部署.md          # yum/源码编译/Docker/目录结构
│   └── 03-Nginx核心配置.md            # 配置层级/全局/http/server/location
│
├── 🚀 核心能力（04-06）
│   ├── 04-Nginx-Web服务与代理.md       # 静态站点/反向代理/动静分离/WebSocket
│   ├── 05-Nginx-负载均衡.md            # upstream/策略/健康检查/Session保持
│   └── 06-Nginx-缓存与优化.md          # proxy_cache/浏览器缓存/gzip/连接池
│
├── 🔒 运维保障（07-08）
│   ├── 07-Nginx-日志与监控.md          # access/error日志/自定义格式/日志轮转
│   └── 08-Nginx-集群与高可用.md        # Keepalived/VRRP/主备切换/Ansible
│
├── ☁️ 云原生（09）
│   └── 09-Nginx-Kubernetes与微服务.md   # Ingress Controller/ConfigMap/Deployment
│
├── 🛡️ 安全与性能（10-11）
│   ├── 10-Nginx-SSL与安全加固.md        # HTTPS配置/HSTS/CSP/限流/WAF
│   └── 11-Nginx-性能调优与故障排查.md    # Worker优化/内核参数/缓冲区/故障排查
│
└── 📌 00-Nginx知识体系总览.md            # ← 本文件
```

---

## 2. 文件导航

| # | 文件 | 核心内容 | 级别 |
|---|------|----------|:---:|
| 00 | Nginx知识体系总览 | 全景导航 + 学习路线 | — |
| 01 | Nginx概述与核心原理 | Master-Worker 模型/事件驱动/epoll/正向vs反向代理 | ⭐⭐ |
| 02 | Nginx安装与部署 | yum/源码编译/Docker/目录结构/启停命令 | ⭐ |
| 03 | Nginx核心配置 | 配置层级/http/server/location匹配规则/变量 | ⭐⭐⭐ |
| 04 | Nginx Web服务与代理 | 静态站点/Vue React部署/反向代理/动静分离/WebSocket | ⭐⭐ |
| 05 | Nginx负载均衡 | upstream/轮询/权重/ip_hash/least_conn/健康检查 | ⭐⭐⭐ |
| 06 | Nginx缓存与优化 | proxy_cache/gzip/浏览器缓存/连接池/sendfile | ⭐⭐⭐ |
| 07 | Nginx日志与监控 | access/error日志/自定义格式/JSON日志/日志轮转 | ⭐⭐ |
| 08 | Nginx集群与高可用 | Keepalived/VRRP/主备切换/Ansible/Nginx Plus | ⭐⭐⭐⭐ |
| 09 | Nginx Kubernetes与微服务 | Ingress Controller/ConfigMap/Deployment/灰度发布 | ⭐⭐⭐ |
| 10 | Nginx SSL与安全加固 | HTTPS/HSTS/CSP/限流/防盗链/WAF/安全头 | ⭐⭐⭐ |
| 11 | Nginx性能调优与故障排查 | Worker优化/缓冲区/内核参数/常见故障排查流程 | ⭐⭐⭐ |

---

## 3. 精通级学习路线

### 🟢 L1：能部署 Nginx 代理 Java 应用（半天）

```
01-概述 → 02-安装 → 03-核心配置 → 04-Web服务与代理
产出：能安装 Nginx、配置反向代理、部署前端静态资源
```

### 🔵 L2：能扛住流量（1天）

```
05-负载均衡 → 06-缓存与优化 → 11-性能调优
产出：能配置负载均衡策略、启用缓存、优化并发性能
```

### 🟣 L3：生产级运维（半天）

```
07-日志与监控 → 08-集群与高可用 → 10-SSL与安全加固
产出：能搭建高可用集群、配置 HTTPS、加固安全、排查故障
```

### 🟡 L4：云原生架构（半天）

```
09-Kubernetes与微服务 → 结合 K8s/Spring Cloud 实战
产出：能在 K8s 中部署 Nginx Ingress、配置灰度发布
```

---

> 🎯 **Nginx 是 Java 后端架构的核心组件** — HTTP 请求的第一入口、静态资源的加速器、后端服务的保护伞。不会 Nginx 的 Java 后端就像不懂网关的微服务架构师。
