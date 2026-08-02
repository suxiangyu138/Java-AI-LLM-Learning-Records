# 02 - Servlet 容器与 MVC 框架

> 定位：Servlet 规范、容器生态位、Spring MVC 架构、框架对比、内嵌容器——Web 应用的"发动机"

## 📚 目录

1. [Servlet 规范](#1-servlet-规范)
2. [容器生态位](#2-容器生态位)
3. [Spring MVC 架构](#3-spring-mvc-架构)
4. [MVC 框架对比](#4-mvc-框架对比)
5. [内嵌容器与部署](#5-内嵌容器与部署)

---

## 1. Servlet 规范

### 1.1 Servlet 是什么

```
Servlet = Java Web 的请求处理规范
  处理 HTTP 请求的核心接口

演进（Jakarta EE）：
  Servlet 4.0（Tomcat 9，javax.*）
  Servlet 6.0（Tomcat 10.1）
  Servlet 6.1（Tomcat 11，Jakarta EE 11）

⚠️ 面试必答：
"Servlet 是 Java Web 的基石规范——
 Spring MVC 的 DispatcherServlet 也是 Servlet；
 所有 Web 框架最终都跑在 Servlet 容器上。"
```

### 1.2 核心组件

```
Servlet 生态三组件：
  Servlet：处理请求（业务入口）
  Filter：拦截请求（认证/编码/日志）
  Listener：监听事件（初始化/销毁）

请求生命周期：
  init（一次）→ service（每次）→ destroy（一次）

⚠️ 面试必答：
"Servlet 三组件 + 生命周期——
 Spring Boot 里 Filter 依然可用
 （OncePerRequestFilter 实现 JWT 认证）。"
```

---

## 2. 容器生态位

### 2.1 主流容器

| 容器 | 特点 | 场景 |
|------|------|------|
| Tomcat | 轻量、默认 | **Java Web 事实标准** |
| Jetty | 更轻、嵌入式友好 | 高并发 IO 场景 |
| Undertow | 高性能 | ⚠️ 已从 Boot 4 移除 |
| Netty | 非 Servlet、NIO | 网关/RPC（非 Web 容器） |

```
⚠️ 面试必答：
"Tomcat 是事实标准（Servlet 容器）、
 Jetty 更轻（嵌入式）、
 Netty 不是 Servlet 容器（网关/RPC 底层）。"
```

### 2.2 容器 vs 框架

```
分工：
  容器：接收连接、解析 HTTP、管理 Servlet 生命周期
  框架：路由、参数绑定、业务调度

Tomcat 收请求 → 交给 Spring MVC 的 DispatcherServlet
（它本身是一个 Servlet，注册在容器中）

⚠️ 面试必答：
"容器管'通信与生命周期'、框架管'分发与业务'——
 DispatcherServlet 是连接两者的桥梁
 （一个特殊的 Servlet）。"
```

> 深入：[Tomcat 架构与请求处理流程](../Tomcat/01-Tomcat架构与请求处理流程.md)

---

## 3. Spring MVC 架构

### 3.1 核心组件

```
Spring MVC 五核心：
  DispatcherServlet：前端控制器（入口）
  HandlerMapping：URL → Controller 方法
  HandlerAdapter：执行方法 + 参数绑定
  ViewResolver：视图解析（前后端分离后少用）
  @ControllerAdvice：全局异常/绑定

请求链路：
  请求 → DispatcherServlet → HandlerMapping
  → HandlerAdapter → Controller → 响应

⚠️ 面试必答：
"Spring MVC 五组件——DispatcherServlet 分发、
 HandlerMapping 找方法、HandlerAdapter 执行、
 Controller 业务、Advice 异常。"
```

### 3.2 MVC 职责

```
Controller：接收参数、调用 Service、返回结果（薄）
Service：业务逻辑、事务（核心）
Repository：数据访问

⚠️ 规范：
  Controller 不写 SQL、Service 不写 SQL 拼接
  分层解耦是维护性的根基
```

> 深入：[SpringBoot Web 开发](../06-Spring全家桶/SpringBoot/03-SpringBootWeb开发.md)

---

## 4. MVC 框架对比

| 框架 | 特点 | 定位 |
|------|------|------|
| Spring MVC | 生态之王、注解式 | 主流标准 |
| Struts2 | 历史框架 | 已淘汰 |
| JSP+Servlet | 原始方式 | 历史 |
| JAX-RS | REST 规范 | 轻量场景 |

```
⚠️ 面试必答：
"Spring MVC 是事实标准（生态 + 注解 + 与 Boot 一体）；
 Struts2 是历史（安全漏洞多、已淘汰）；
 新项目一律 Spring MVC。"
```

---

## 5. 内嵌容器与部署

### 5.1 内嵌容器（Spring Boot）

```
Spring Boot 内嵌容器 = 应用自带 Tomcat
  java -jar app.jar 直接运行（无需外部容器）

对比传统部署：
  传统：WAR 部署到外部 Tomcat
  现代：jar 内嵌 Tomcat（微服务标配）

⚠️ 面试必答：
"内嵌容器是 Boot 的核心设计——
 jar 自带 Tomcat、一键运行；
 传统 WAR 外部容器已基本退出。"
```

### 5.2 部署形态

| 形态 | 场景 |
|------|------|
| java -jar | 单体/微服务（主流） |
| Docker 容器 | 云原生（标配） |
| WAR + 外部 Tomcat | 遗留系统 |

```
⚠️ 面试必答：
"部署演进——WAR（传统）→ jar（Boot）→
 Docker/K8s（云原生）；
 微服务时代内嵌容器 + 容器编排。"
```

---

> 🎯 **核心要点**：容器与 MVC = **Servlet 规范**（三组件 + 生命周期）+ **容器分工**（Tomcat 通信、框架分发）+ **Spring MVC 五组件**（DispatcherServlet 分发链）+ **框架选型**（Spring MVC 标准）+ **内嵌容器**（jar 一键运行）。"容器收、框架分发、业务执行"是 Web 应用的完整引擎。

---

**返回总览**：[00-Web开发生态总览](00-Web开发生态总览.md) | **上一篇**：[01-HTTP协议与网络基础](01-HTTP协议与网络基础.md) | **下一篇**：[03-会话与状态管理](03-会话与状态管理.md)
