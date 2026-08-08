# Web 高阶知识总览
> HTTP 协议、缓存、TLS、HTTP/2/3、跨域与浏览器安全、实时通信、Web 攻防、性能——"Web 服务器之上、业务代码之下"的协议与浏览器生态专项体系

## 📚 目录
1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [学习路线推荐](#3-学习路线推荐)
4. [Web 高阶知识定位](#4-web-高阶知识定位)
5. [核心概念速查](#5-核心概念速查)
6. [参考来源](#6-参考来源)

## 1. 知识体系导图

```text
Web 高阶知识体系（9 篇，2026-08 基准）
│
├─ 协议层 ─────────────────────────────
│   ├─ 00 总览（本文）
│   ├─ 01 HTTP 协议深入（报文/方法/状态码/头字段体系）
│   ├─ 02 HTTP 缓存体系（强缓存/协商缓存/缓存策略设计）
│   ├─ 03 HTTPS 与 TLS 深入（握手/证书/TLS1.3/双向TLS）
│   └─ 04 HTTP/2 与 HTTP/3（多路复用/QUIC/升级路径）
│
├─ 安全层 ─────────────────────────────
│   ├─ 05 跨域与浏览器安全机制（CORS/SameSite/安全头/CSP）
│   └─ 07 Web 安全攻防（OWASP Top 10 2025/攻防清单）
│
├─ 通信层 ─────────────────────────────
│   └─ 06 实时通信（WebSocket/SSE/长轮询选型）
│
└─ 实战层 ─────────────────────────────
│   ├─ 08 Web 性能与浏览器机制（关键渲染路径/Core Web Vitals）
│   └─ 09 生产实践与面试题（避坑/面试）
```

> 🎯 定位一句话：Web 高阶知识是 **"协议与浏览器生态"专项**——HTTP 怎么缓存、TLS 怎么握手、为什么跨域、攻击怎么防、性能怎么度量。Web 服务器的配置（Nginx 体系）、JavaWeb 组件（Servlet/Filter 体系）是"怎么落地"，本体系回答"底层为什么"。

## 2. 模块导航

| 序号 | 模块 | 核心内容 | 适合人群 |
|:---:|------|---------|---------|
| 00 | [总览（本文）](00-Web高阶知识总览.md) | 导图、定位、速查 | 所有人 |
| 01 | [HTTP 协议深入](01-HTTP协议深入.md) | 报文、方法语义与幂等、状态码全表、头字段 | 入门必读 |
| 02 | [HTTP 缓存体系](02-HTTP缓存体系.md) | 强缓存/协商缓存、Cache-Control、ETag、策略设计 | 重点 |
| 03 | [HTTPS 与 TLS 深入](03-HTTPS与TLS深入.md) | 握手、证书链、TLS 1.3、双向 TLS、HSTS | 重点 |
| 04 | [HTTP/2 与 HTTP/3](04-HTTP2与HTTP3.md) | 多路复用、HPACK、QUIC、升级路径 | 进阶 |
| 05 | [跨域与浏览器安全机制](05-跨域与浏览器安全机制.md) | CORS 全流程、SameSite、安全头、CSP | 重点 |
| 06 | [实时通信 WebSocket 与 SSE](06-实时通信WebSocket与SSE.md) | 长轮询/WS/SSE 对比、握手、心跳、选型 | 进阶 |
| 07 | [Web 安全攻防](07-Web安全攻防.md) | OWASP Top 10 2025、XSS/CSRF/注入/SSRF、防护清单 | 重点 |
| 08 | [Web 性能与浏览器机制](08-Web性能与浏览器机制.md) | 关键渲染路径、Core Web Vitals、资源加载优化 | 进阶 |
| 09 | [生产实践与面试题](09-生产实践与面试题.md) | 避坑、调试工具链、面试 | 收尾 |

## 3. 学习路线推荐

| 路线 | 人群 | 路径 |
|------|------|------|
| 快速入门（1 天） | 后端开发者 | 00 → 01 → 02 → 03 |
| 面试冲刺（2 天） | 备战后端面试 | 00 → 01 → 02 → 05 → 07 → 09 |
| 全栈/性能专家（3 天） | 进阶 | 04 → 06 → 08 |

> 💡 与本目录兄弟体系的分工：**协议原理**在本体系，**服务器配置落地**在 [Nginx](../Nginx/00-Nginx知识体系总览.md)/[Web 服务器](../Web%20服务器/00-Web服务器总览.md) 体系，**网络排障**在 [网络问题排查](../../07-工程运维基础/运维/网络问题排查/00-网络问题排查总览.md) 体系——三者配套食用。

## 4. Web 高阶知识定位

```text
一次 HTTPS 请求涉及的全部知识（本体系覆盖高亮）：
浏览器 ──TLS 握手(03)──> Web 服务器 ──反代──> 应用
  │                        │                    │
  ├─ 缓存命中？(02)         ├─ HTTP/2/3？(04)     ├─ 参数校验/注入防(07)
  ├─ 跨域检查？(05)         ├─ WebSocket？(06)    └─ 性能度量(08)
  └─ 安全头？(05)           └─ 状态码语义(01)
```

| 知识域 | 解决什么问题 | 落地上游 |
|--------|-------------|---------|
| HTTP 协议 | 报文怎么读写、语义怎么选 | Servlet/Spring MVC 封装了它 |
| 缓存 | 为什么 304、怎么 0 请求 | Nginx 缓存/CDN 配置 |
| TLS/HTTPS | 证书怎么签、握手怎么快 | Nginx SSL/证书续期 |
| HTTP/2/3 | 为什么更快、怎么升级 | 服务器/网关开启配置 |
| 跨域/安全头 | 浏览器为什么拦、怎么放行 | CORS 配置/CSP 头 |
| 实时通信 | 推送选什么方案 | SSE/WebSocket 落地 |
| 攻防 | 攻击原理与防线 | 安全编码/Filter/网关 WAF |
| 性能 | 慢在哪、怎么度量 | 监控指标/压测 |

## 5. 核心概念速查

| 概念 | 一句话 |
|------|--------|
| HTTP 报文 | 请求行/状态行 + 头 + body 的三段结构 |
| 幂等 | 同一请求多次执行结果一致（GET/PUT/DELETE 幂等，POST 不幂等） |
| 强缓存 | 命中不发请求（Cache-Control: max-age） |
| 协商缓存 | 每次发请求，304 免 body（ETag/Last-Modified） |
| TLS 握手 | 协商密钥 + 证书验证，1-RTT（TLS 1.3） |
| TLS 1.3 | 当前主流（2026 占加密流量 ~70-73%），0-RTT 恢复 |
| HTTP/2 | 二进制帧 + 多路复用 + HPACK（流量主力 ~51-55%） |
| HTTP/3 | QUIC/UDP，2026 站点支持 ~40%、流量 ~21-34% |
| 同源策略 | 浏览器安全基石：协议+域名+端口一致才同源 |
| CORS | 跨域资源共享协议（预检/响应头放行） |
| SameSite | Cookie 跨站携带策略（Lax/Strict/None） |
| CSP | 内容安全策略（白名单资源加载） |
| WebSocket | 全双工长连接（ws/wss） |
| SSE | 单向服务端推送（EventSource，HTTP 之上） |
| OWASP Top 10 | 2025 版：访问控制 #1、配置错误 #2、供应链 #3 |
| Core Web Vitals | LCP/INP/CLS 三大性能指标 |
| 关键渲染路径 | HTML→CSS→渲染树→布局→绘制 |

## 6. 参考来源

- [HTTP 规范（RFC 9110/9111/9112/9113/9114）](https://www.rfc-editor.org/rfc/rfc9110)
- [W3Techs HTTP/2 vs HTTP/3 使用统计](https://w3techs.com/technologies/comparison/ce-http2,ce-http3,ce-httpsdefault)
- [HTTP 协议采用率分析 2026（TechnologyChecker）](https://technologychecker.io/blog/http-protocol-adoption)
- [OWASP Top 10 2025 官方](https://owasp.org/Top10/2025/)
- [Cloudflare TLS/HTTP 采用度数据（Radar）](https://radar.cloudflare.com/adoption-and-usage)
- [TLS 与互联网安全 2026 Q1 状态（SSL Reminder）](https://sslreminder.pro/blog/posts/state-of-tls-q1-2026/)
- [MDN：HTTP 文档](https://developer.mozilla.org/zh-CN/docs/Web/HTTP)

---

**下一模块**：[01-HTTP协议深入](01-HTTP协议深入.md)
