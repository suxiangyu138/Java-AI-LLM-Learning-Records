# 00 - Web 开发生态总览

> 定位：Web 开发全生态地图——一次请求的完整旅程、技术栈分层、演进历史、模块导航

## 📚 目录

1. [Web 开发是什么](#1-web-开发是什么)
2. [一次请求的完整旅程](#2-一次请求的完整旅程)
3. [技术栈分层地图](#3-技术栈分层地图)
4. [Web 技术演进史](#4-web-技术演进史)
5. [模块导航](#5-模块导航)

---

## 1. Web 开发是什么

```
Web 开发 = 构建浏览器可访问的应用

三大参与者：
  浏览器（客户端）：发请求、渲染页面
  服务器（应用）：处理业务逻辑
  数据库（存储）：持久化数据

Java Web 开发生态：
  协议层：HTTP/HTTPS
  容器层：Tomcat/Jetty/Undertow
  框架层：Spring MVC/Spring Boot
  数据层：MyBatis/JPA + MySQL/Redis
  安全层：Spring Security/Shiro/JWT

⚠️ 面试必答：
"Web 生态 = 协议（HTTP）+ 容器（Tomcat）+
 框架（Spring）+ 数据 + 安全——
 一次请求贯穿全部层次。"
```

---

## 2. 一次请求的完整旅程

```
浏览器输入 URL → 返回页面的完整链路：

① DNS 解析：域名 → IP
② TCP 连接：三次握手（HTTPS 加 TLS 握手）
③ HTTP 请求：浏览器发 GET/POST
④ Nginx（可选）：静态资源/负载均衡/反向代理
⑤ Tomcat：接收 → Servlet 容器解析
⑥ Spring MVC：DispatcherServlet 分发到 Controller
⑦ Service 层：业务逻辑 + 事务
⑧ 数据层：MyBatis/JPA 访问 MySQL、Redis 缓存
⑨ 响应返回：JSON → 浏览器渲染

⚠️ 面试必答：
"一次请求九步——DNS → TCP/TLS → HTTP →
 Nginx → Tomcat → MVC → Service → 数据 →
 响应。面试'描述一次请求'就按这条链答。"
```

---

## 3. 技术栈分层地图

| 层次 | 技术 | 职责 |
|------|------|------|
| 客户端 | HTML/CSS/JS、Vue/React | 页面渲染与交互 |
| 接入层 | Nginx、API 网关 | 静态资源、路由、限流 |
| 应用层 | Spring Boot、Spring MVC | 业务逻辑 |
| 容器层 | Tomcat、Jetty、Undertow | Servlet 运行 |
| 数据层 | MySQL、Redis、ES | 存储与缓存 |
| 消息层 | RabbitMQ、Kafka | 异步解耦 |
| 安全层 | Spring Security、JWT | 认证授权 |
| 监控层 | Prometheus、Grafana、ELK | 可观测 |

```
⚠️ 面试必答：
"Web 栈八层——客户端/接入/应用/容器/
 数据/消息/安全/监控；
 每层都有生态位（本体系逐层剖析）。"
```

---

## 4. Web 技术演进史

| 时代 | 技术 | 特点 |
|------|------|------|
| 静态时代 | HTML 页面 | 纯静态展示 |
| Servlet 时代 | JSP/Servlet | 动态页面（前后端混合） |
| 框架时代 | SSH/SSM | 分层解耦 |
| 前后端分离 | REST + JSON + Vue/React | 独立演进 |
| 微服务时代 | Spring Cloud + 容器 | 分布式 |
| 云原生时代 | K8s + Serverless | 弹性伸缩 |

```
⚠️ 面试必答：
"演进主线——静态 → Servlet → 框架 →
 前后端分离 → 微服务 → 云原生；
 当前主流 = 前后端分离 + 微服务。"
```

---

## 5. 模块导航

| 序号 | 模块 | 核心内容 | 定位 |
|------|------|---------|------|
| 00 | [本总览](00-Web开发生态总览.md) | 请求旅程、生态地图、演进 | 入口 |
| 01 | [HTTP协议与网络基础](01-HTTP协议与网络基础.md) | HTTP 版本、状态码、HTTPS、Cookie | 协议 |
| 02 | [Servlet容器与MVC框架](02-Servlet容器与MVC框架.md) | Servlet 规范、Tomcat 生态位、MVC | 容器框架 |
| 03 | [会话与状态管理](03-会话与状态管理.md) | Cookie/Session/JWT/Redis 会话 | 状态 |
| 04 | [Web安全与防护](04-Web安全与防护.md) | 认证授权、XSS/CSRF/注入、OWASP | 安全 |
| 05 | [前后端协作与API设计](05-前后端协作与API设计.md) | RESTful、校验、跨域、幂等、版本 | 协作 |
| 06 | [性能优化体系](06-性能优化体系.md) | 缓存三件套、CDN、连接池、压测 | 性能 |
| 07 | [高可用与规模化](07-高可用与规模化.md) | 集群、负载均衡、限流熔断、监控 | 架构 |
| 08 | [面试题精选与学习路径](08-面试题精选与学习路径.md) | 面试十问、学习路线 | 面试 |

---

> 🎯 **本体系学习建议**：Web 生态是"一条链路"——从协议（01）到容器框架（02）、状态（03）、安全（04）、协作（05）、性能（06）、高可用（07）。先建立全链路地图（00），再逐层深入；本体系与 Tomcat/SpringBoot/企业级标准体系互补（聚焦生态整合视角）。

---

**下一篇**：[01-HTTP协议与网络基础](01-HTTP协议与网络基础.md)
