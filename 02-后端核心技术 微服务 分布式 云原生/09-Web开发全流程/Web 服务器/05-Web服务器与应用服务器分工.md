# Web 服务器与应用服务器分工
> 动静分离架构的本质：为什么"Web 服务器管静态、应用服务器管动态"是现代架构的默认答案——职责边界、组合模式与反代层清单

## 📚 目录
1. [职责边界：谁干什么](#1-职责边界谁干什么)
2. [为什么必须分离](#2-为什么必须分离)
3. [经典组合架构模式](#3-经典组合架构模式)
4. [反代层职责清单](#4-反代层职责清单)
5. [Java 世界的标准组合](#5-java-世界的标准组合)
6. [云原生时代的演变](#6-云原生时代的演变)

## 1. 职责边界：谁干什么

| 维度 | Web 服务器（Nginx/Apache/Caddy） | 应用服务器（Tomcat/Boot/ASP.NET Core） |
|------|:---:|:---:|
| 静态文件 | ✅ 直出（极快） | ❌ 也能做但浪费 |
| 动态内容 | ❌ 不做 | ✅ 业务计算 |
| TLS 终止 | ✅（入口处集中） | 可做（一般不做） |
| 反向代理 | ✅ 核心能力 | ❌ |
| 缓存 | ✅（静态/微缓存） | 业务级缓存（Redis） |
| 限流/WAF | ✅ 入口层 | 应用层兜底 |
| 会话状态 | ❌ | ✅ Session/Token |
| 扩缩容 | 无状态随便扩 | 有状态需处理会话 |

> 🎯 一句话：**Web 服务器是"流量管道"，应用服务器是"业务计算"**——管道的职责是快、稳、安全地转发；计算的职责是正确、一致地处理。

## 2. 为什么必须分离

```text
动静不分的历史问题：
  Apache mod_php：PHP 解释器在 Web 服务器进程里
  → 1 个静态请求也占满 PHP 进程（4MB+）
  → 静态并发与动态并发互相拖累
  → 扩容只能整体扩（浪费）
```

| 分离收益 | 说明 |
|---------|------|
| **性能** | 静态资源由事件驱动服务器直出（万级并发），应用线程池只服务动态请求 |
| **独立扩缩容** | 静态瓶颈扩 Web 服务器；动态瓶颈扩应用集群（成本差异巨大） |
| **安全** | Web 服务器挡在最前（WAF/限流/黑名单），应用不直接暴露 |
| **TLS 集中** | 证书只配一处（入口），应用层全内网明文 |
| **发布解耦** | 前端/后端独立发布（前端资源更新不动应用） |

> 🎯 面试金句：**"动静分离的本质是让最便宜的资源（Nginx 事件驱动）处理最频繁的请求（静态资源），让最贵的资源（应用线程）只处理真正需要计算的内容"**。

## 3. 经典组合架构模式

```text
模式一：单机动静分离（中小站）
浏览器 → Nginx（静态 + 反代 /api/）→ 单台 Tomcat/Boot
         （/static/** 直出，/api/** 转发）

模式二：集群动静分离（标准生产）
浏览器 → CDN（静态缓存）→ Nginx 集群（负载均衡）→ 多台应用节点
                                              ├─ Boot-1
                                              └─ Boot-2

模式三：微服务网关化（云原生）
浏览器 → CDN → 网关（Spring Cloud Gateway/Ingress）→ 服务集群
         （网关兼 Web 服务器 + 路由 + 限流 + 认证）
```

| 模式 | 规模 | 关键点 |
|------|------|--------|
| 单机分离 | 中小站 | 静态/动态路径清晰分区（`/static` vs `/api`） |
| 集群分离 | 生产标准 | CDN + Nginx LB + 应用集群 + 会话外置 |
| 网关化 | 微服务 | 网关吸收 Web 服务器职责，见云原生节 |

## 4. 反代层职责清单

```nginx
# 反代层的"五件套"（Nginx 视角，其他服务器同构）
server {
    listen 443 ssl;
    ssl_certificate     /etc/nginx/certs/fullchain.pem;   # ① TLS 终止
    gzip on;                                             # ② 压缩
    proxy_cache_path /var/cache/nginx levels=1:2 keys_zone=api:10m;  # ③ 缓存
    limit_req_zone $binary_remote_addr zone=api:10m rate=10r/s;      # ④ 限流

    location /api/ {
        limit_req zone=api burst=20;
        proxy_pass http://app_cluster;                   # ⑤ 负载均衡
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

| 职责 | 说明 | 联动体系 |
|------|------|---------|
| TLS 终止 | 证书集中管理、自动续期 | [Nginx HTTPS 章节](../Nginx/10-Nginx-SSL与安全加固.md) |
| 压缩 | gzip/brotli，文本类 70%+ 收益 | [Nginx 性能章节](../Nginx/11-Nginx-性能调优与故障排查.md) |
| 静态缓存 | 长缓存 + 版本化 | [SpringBoot Web 静态资源](../SpringBoot%20Web/06-静态资源与SPA路由.md) |
| 限流 | 入口限流（应用层再兜一层） | [Nginx 体系总览](../Nginx/00-Nginx知识体系总览.md) |
| 超时控制 | 连接/读/写超时三件套 | [Nginx 反代章节](../Nginx/03-Nginx-核心配置.md) |
| 请求头转发 | X-Forwarded-*（应用取真实 IP/协议） | [Boot 反代配置](../SpringBoot%20Web/07-部署形态与HTTPS配置.md) |

> ⚠️ 反代层与应用层的**超时必须分层设计**：反代超时 > 应用超时（否则反代先断导致"客户端看到超时但应用还在跑"）；统一走日志与 traceId 串起两段耗时。

## 5. Java 世界的标准组合

```text
        ┌────────────── Nginx（Web 服务器/反代）──────────────┐
        │  /static/** → 静态直出                               │
        │  /api/**    → proxy_pass → Tomcat 集群               │
        └────────────────────────────────────────────────────┘
                                   │
                    ┌──────────────┼──────────────┐
                    ▼              ▼              ▼
              Tomcat-1        Tomcat-2        Tomcat-3
              （Servlet 6.1 / 虚拟线程）
```

| 组件 | 职责 | 深挖体系 |
|------|------|---------|
| Nginx | 静态 + 反代 + TLS + 限流 | [Nginx 体系](../Nginx/00-Nginx知识体系总览.md) |
| Tomcat | Servlet 容器（内嵌于 Boot） | [Tomcat 体系](../Tomcat/00-Tomcat总览与核心概念.md) |
| Spring Boot | MVC/业务/会话外置 | [SpringBoot Web 体系](../SpringBoot%20Web/00-SpringBootWeb总览.md) |
| 会话外置 | Redis/Token（多节点共享） | [Cookie & Session 体系](../Cookie%20%26%20Session（会话技术）/00-会话技术总览.md) |

> 🎯 生产要点：**集群模式下会话不能留在 Tomcat 内存**（粘滞会话是反模式）——Session 外置 Redis 或换 JWT 无状态方案；应用层无状态后，Nginx 集群可随意扩，这才是动静分离的完整形态。

## 6. 云原生时代的演变

```text
传统：浏览器 → Nginx（反代）→ 应用集群
K8s： 浏览器 → Ingress Controller（Nginx/Envoy）
            → Service → Pod（应用，内含内嵌容器）
            → 静态资源交给 CDN/对象存储

演变本质：
  · Web 服务器的角色被 Ingress/网关吸收（路由/限流/TLS）
  · 应用服务器"内嵌化"（Boot 自带 Tomcat）
  · 静态资源"外部化"（CDN/OSS）
```

| 时代 | Web 服务器形态 | 应用服务器形态 |
|------|---------------|---------------|
| 传统 | 独立 Nginx/Apache | 独立 Tomcat/War |
| 现代 | Nginx + CDN | 内嵌容器（Boot Jar） |
| 云原生 | Ingress/网关 + CDN | Pod 内嵌 + 虚拟线程 |

> 🎯 结论：**"Web 服务器"这个概念正在隐形**——静态资源上 CDN、反代职责进网关/Ingress、应用自带容器。但**架构分层思想没变**：入口层（管道）与业务层（计算）永远分离。云原生细节见 [Docker/K8s 体系](../../11-云原生/Docker/00-Docker知识体系总览.md)。

---

**下一模块**：[06-选型决策与生产实践](06-选型决策与生产实践.md) / **返回总览**：[00-Web服务器总览](00-Web服务器总览.md)
