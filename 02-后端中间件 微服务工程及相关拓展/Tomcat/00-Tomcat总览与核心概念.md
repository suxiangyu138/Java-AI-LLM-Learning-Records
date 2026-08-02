# 00 - Tomcat 总览与核心概念

> 定位：Tomcat 知识体系入口——Servlet 容器定位、版本现状（2026-07）、体系导图、核心术语表、模块导航

## 📚 目录

1. [Tomcat 是什么](#1-tomcat-是什么)
2. [版本现状与选型（2026-07 验证）](#2-版本现状与选型2026-07-验证)
3. [知识体系导图](#3-知识体系导图)
4. [核心术语表](#4-核心术语表)
5. [模块导航](#5-模块导航)

---

## 1. Tomcat 是什么

```
Apache Tomcat = Java Servlet 容器（Web 服务器）
  运行 Java Web 应用（Servlet/JSP）
  实现 Jakarta EE 的 Servlet/JSP 规范

定位：
  轻量级 Java Web 服务器（内嵌/独立部署）
  Spring Boot 默认内嵌容器之一
  与 Nginx（静态/反向代理）分工配合

⚠️ 面试必答：
"Tomcat 是 Servlet 容器——实现 Servlet/JSP
 规范、管理 Servlet 生命周期、处理 HTTP 请求。
 Java Web 应用的标准运行环境。"
```

---

## 2. 版本现状与选型（2026-07 验证）

### 2.1 版本线

| 版本 | 规范 | Java 要求 | 状态 |
|------|------|:---:|------|
| Tomcat 9 | Servlet 4.0（javax.*） | Java 8+ | 维护中（老项目） |
| Tomcat 10.1 | Servlet 6.0（jakarta.*） | Java 11+ | 主流 |
| **Tomcat 11.0** | **Servlet 6.1（jakarta.*）** | **Java 17+** | ✅ 最新（11.0.24，2026-07-08） |

### 2.2 Tomcat 11 亮点

| 特性 | 说明 |
|------|------|
| Servlet 6.1 | Jakarta EE 11 部分规范 |
| 虚拟线程 | Java 21 虚拟线程支持——高并发吞吐提升、内存降低 |
| HTTP/2 | 改进（畸形消息流重置等） |
| 安全更新 | 11.0.22 修复多个 CVE（WebDAV/HTTP2 头/WebSocket） |

### 2.3 迁移注意

```
⚠️ Tomcat 10 起 javax.* → jakarta.*（重大变更）
  Tomcat 9 → 10/11 需改包名（官方迁移工具）
⚠️ Spring Boot 内嵌 Tomcat 跟随 Boot 版本
  Boot 3.x = Tomcat 10.1；Boot 4.x（2025 底）= Tomcat 11

⚠️ 面试必答：
"Tomcat 10 起 API 从 javax 迁到 jakarta——
 升级是'改名 + 重编译'；生产选型：
 新项目 Tomcat 11 + Java 21 虚拟线程。"
```

---

## 3. 知识体系导图

```
                    Tomcat 知识体系
                          │
      ┌─────────┬─────────┼──────────┬────────────┐
      │         │         │          │            │
  基础层      架构层     配置层     优化层       安全层
  总览术语    请求处理    配置文件   并发模型     会话与安全
  部署启动    生命周期    server.xml 性能调优    SSL/认证
      │         │         │          │            │
  00 总览     01 架构    02 配置    03 连接器    04 会话安全
                              └────── 05 优化排查面试
```

---

## 4. 核心术语表

| 术语 | 一句话定义 | 详见 |
|------|-----------|------|
| Servlet | 处理请求的 Java 类（doGet/doPost） | 01 |
| JSP | 服务端模板（编译成 Servlet） | 01 |
| Connector | 接收请求的组件（HTTP/AJP） | 01、03 |
| Container | 处理请求的引擎（Catalina） | 01 |
| Catalina | Tomcat 的 Servlet 容器实现 | 01 |
| Coyote | Tomcat 的连接器组件 | 03 |
| Engine / Host / Context | 容器层级（引擎/虚拟主机/应用） | 01 |
| Valve | 请求拦截器（Pipeline 管道） | 01 |
| Filter | 过滤器（应用层拦截） | 01 |
| Listener | 监听器（事件回调） | 01 |
| server.xml | 全局配置（端口/连接器/主机） | 02 |
| web.xml | 应用配置（Servlet/Filter 映射） | 02 |
| context.xml | 应用上下文配置（数据源） | 02 |
| NIO 连接器 | 非阻塞 I/O 连接器（默认） | 03 |
| Executor | 线程池（Tomcat 9+ 默认） | 03 |
| 虚拟线程 | Java 21 虚拟线程支持（11 亮点） | 03 |
| Session | 会话（JSESSIONID Cookie） | 04 |
| Realm | 认证领域（用户/角色存储） | 04 |
| SSL/TLS | 加密连接（连接器配置） | 04 |
| AJP | 与 Apache/Nginx 通信协议 | 03 |
| Manager | 管理应用（部署/热部署） | 05 |

---

## 5. 模块导航

| 序号 | 模块 | 核心内容 | 定位 |
|------|------|---------|------|
| 00 | [本总览](00-Tomcat总览与核心概念.md) | 版本、术语、导图 | 入口 |
| 01 | [架构与请求处理流程](01-Tomcat架构与请求处理流程.md) | Catalina 层级、请求生命周期、Servlet 规范 | 核心原理 |
| 02 | [配置文件详解](02-Tomcat配置文件详解.md) | server.xml/web.xml/context.xml 全解析 | 配置实战 |
| 03 | [连接器与并发模型](03-Tomcat连接器与并发模型.md) | NIO、线程池、HTTP/2、虚拟线程 | 性能核心 |
| 04 | [会话管理与安全](04-Tomcat会话管理与安全.md) | Session、SSL、认证 Realm、安全实践 | 安全 |
| 05 | [性能优化与故障排查](05-Tomcat性能优化与故障排查.md) | JVM 调优、部署运维、面试题 | 实战 |

---

> 🎯 **本体系学习建议**：Tomcat 面试主线——**请求处理流程**（Connector→Container 九步）、**并发模型**（NIO/线程池/虚拟线程）、**配置**（server.xml 三个连接器）、**安全**（SSL/会话）。先懂架构（01）再学配置（02），并发（03）是进阶分水岭。

---

**下一篇**：[01-Tomcat架构与请求处理流程](01-Tomcat架构与请求处理流程.md)
