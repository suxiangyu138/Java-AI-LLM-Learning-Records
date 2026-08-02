# 03 - Tomcat 连接器与并发模型

> 定位：Coyote 连接器、NIO 线程模型、线程池与 Executor、HTTP/2、虚拟线程——Tomcat 并发与性能核心

## 📚 目录

1. [连接器 Coyote](#1-连接器-coyote)
2. [IO 模型演进](#2-io-模型演进)
3. [NIO 线程模型](#3-nio-线程模型)
4. [线程池调优](#4-线程池调优)
5. [HTTP/2 与 AJP](#5-http2-与-ajp)
6. [虚拟线程（Java 21）](#6-虚拟线程java-21)

---

## 1. 连接器 Coyote

```
Coyote = Tomcat 的连接器组件（通信层）

职责：
  ① 监听端口接收连接
  ② 解析 HTTP 请求（请求行/头/体）
  ③ 封装 Request/Response 对象
  ④ 交给 Catalina 处理
  ⑤ 发送响应

三种协议：
  HTTP/1.1（NIO 默认）
  HTTPS（HTTP + TLS）
  AJP（与 Apache/Nginx 通信）

⚠️ 面试必答：
"Coyote 是连接器——管'通信'不管'业务'；
 支持 HTTP/HTTPS/AJP 三种协议；
 与 Catalina（容器）解耦。"
```

---

## 2. IO 模型演进

### 2.1 三代演进

| 模型 | 版本 | 特点 |
|------|:---:|------|
| BIO | Tomcat 7- | 一连接一线程（阻塞） |
| NIO | Tomcat 8+（默认） | 非阻塞 + 事件驱动 |
| NIO2 | Tomcat 8.5+ | 异步 IO（AIO） |

```
⚠️ BIO vs NIO：
  BIO：每连接一个线程 → 线程数 = 连接数 → 高并发线程爆炸
  NIO：少量线程处理大量连接（Selector 事件驱动）
  → NIO 是默认与推荐的唯一选择

⚠️ 面试必答：
"BIO 一连接一线程（C10K 崩溃）、
 NIO 用 Selector 事件驱动（少量线程
 处理海量连接）——Tomcat 8+ 默认 NIO。"
```

### 2.2 NIO 核心组件

```
NIO 三件套：
  Channel（通道）：连接抽象
  Buffer（缓冲区）：数据读写
  Selector（选择器）：事件多路复用

Tomcat 的 NIO 角色分工：
  Acceptor 线程：接收新连接
  Poller 线程：监听事件（Selector）
  Worker 线程：执行 Servlet（线程池）

⚠️ 面试必答：
"NIO = Channel/Buffer/Selector 三件套——
 Tomcat 里 Acceptor 收连接、Poller 轮询
 事件、Worker 池执行业务。"
```

---

## 3. NIO 线程模型

### 3.1 三线程分工

```
请求处理线程模型：
  Acceptor（1 个）：accept 新连接 → 注册到 Poller
  Poller（多个）：Selector 监听读写事件 → 分发给 Worker
  Worker（线程池）：执行 Servlet 业务逻辑

连接数 >> 线程数（非阻塞的威力）：
  10 万连接 ≈ 几百个线程即可服务

⚠️ 面试必答：
"NIO 三线程——Acceptor 收连接、
 Poller 轮询事件、Worker 池执行业务；
 线程数与连接数解耦（连接数可远大于线程数）。"
```

### 3.2 关键参数

```xml
<Connector port="8080"
           protocol="org.apache.coyote.http11.Http11NioProtocol"
           acceptorThreadCount="1"
           pollerThreadCount="1"
           executor="tomcatThreadPool"
           maxConnections="10000"
           acceptCount="100"
           connectionTimeout="20000" />

<Executor name="tomcatThreadPool"
          maxThreads="200"
          minSpareThreads="10" />
```

| 参数 | 默认 | 说明 |
|------|:---:|------|
| maxThreads | 200 | 业务线程上限 |
| minSpareThreads | 10 | 常驻线程 |
| maxConnections | 8192（NIO） | 最大连接数 |
| acceptCount | 100 | 等待队列 |
| acceptorThreadCount | 1 | 接收线程 |
| connectionTimeout | 20000ms | 连接超时 |

---

## 4. 线程池调优

### 4.1 线程数估算

```
线程数经验公式：
  单核：线程数 = CPU 核数 × (1 + 等待比)
  等待比 = 等待时间 / 计算时间

IO 密集（Web 应用常见）：
  线程数 = CPU 核数 × 2 ~ 4（等待多）
CPU 密集：
  线程数 = CPU 核数 + 1

⚠️ 面试必答：
"线程数 = 核数 × (1 + 等待/计算比)——Web 应用
 IO 密集等待多，线程数取核数 2-4 倍；
 实际靠压测调优（ab/JMeter）。"
```

### 4.2 线程池行为

```
Tomcat Executor 饱和策略：
  ① 常驻线程（minSpareThreads）处理
  ② 不足 → 新建线程到 maxThreads
  ③ 满 → 进队列（maxQueueSize）
  ④ 队列满 → 拒绝（Connection refused）

⚠️ 面试必答：
"线程池四级——常驻 → 扩容 → 排队 → 拒绝。
 队列打满时新连接被拒（表现为连接失败），
 这是高并发下的典型瓶颈信号。"
```

---

## 5. HTTP/2 与 AJP

### 5.1 HTTP/2

```
HTTP/2 特性：
  多路复用（一个连接并行多个请求）
  头部压缩（HPACK）
  服务端推送（Server Push，已弃用）

Tomcat 支持：
  HTTP/2 需 TLS（h2）或明文升级（h2c）
  <Connector ... upgradeProtocol="h2c" />

⚠️ 面试必答：
"HTTP/2 的核心是多路复用——一个 TCP
 连接并行多个请求（解决队头阻塞）；
 Tomcat 11 对 HTTP/2 改进（畸形消息
 流重置而非断连）。"
```

### 5.2 AJP 协议

```
AJP = Apache JServ Protocol
  用途：Tomcat 与 Apache/Nginx 通信
  场景：Nginx 做静态/负载均衡 → AJP 转发到 Tomcat

⚠️ 安全警告：
  AJP 无认证（暴露 8009 端口有风险）
  生产仅内网使用或禁用

⚠️ 面试必答：
"AJP 是 Tomcat 与 Web 服务器（Apache/Nginx）
 的专用协议——现代架构多用 HTTP 反向代理
 替代（Nginx proxy_pass），AJP 渐少。"
```

---

## 6. 虚拟线程（Java 21）

### 6.1 虚拟线程是什么

```
虚拟线程（Project Loom，Java 21 正式）：
  轻量级线程（百万级可创建）
  阻塞不占平台线程（阻塞时自动让出）

Tomcat 11 + 虚拟线程：
  每请求一个虚拟线程（类似 BIO 模型）
  但无线程爆炸问题（虚拟线程极轻）

⚠️ 面试必答：
"虚拟线程让'每请求一线程'回归——
 Java 21 的虚拟线程阻塞不占资源，
 Tomcat 11 支持后高并发吞吐提升、
 内存开销降低（对比 NIO 的复杂度）。"
```

### 6.2 启用与对比

```xml
<!-- Tomcat 11 启用虚拟线程执行器 -->
<Executor name="virtualThreads" virtualThreads="true" />
```

| 维度 | NIO + 线程池 | 虚拟线程 |
|------|:---:|:---:|
| 模型 | 事件驱动复用线程 | 每请求一虚拟线程 |
| 代码复杂度 | 高（异步/回调） | 低（同步阻塞风格） |
| 线程数 | 有限（200 左右） | 百万级 |
| 适用 | 传统稳定 | Java 21+ 新项目 |

> 🎯 **要点**：2026 并发趋势——**虚拟线程是 NIO 的优雅替代**（同步代码 + 海量线程）。Spring Boot 3.2+ / Tomcat 11 均支持；面试表达"虚拟线程阻塞不占资源"即可。

---

> 🎯 **核心要点**：连接器与并发 = **Coyote**（通信层）+ **NIO 三线程**（Acceptor/Poller/Worker）+ **线程池四级**（常驻/扩容/排队/拒绝）+ **HTTP/2 多路复用** + **虚拟线程**（Java 21 新范式）。并发模型演进：BIO → NIO → 虚拟线程是 Tomcat 性能面试主线。

---

**返回总览**：[00-Tomcat总览与核心概念](00-Tomcat总览与核心概念.md) | **上一篇**：[02-Tomcat配置文件详解](02-Tomcat配置文件详解.md) | **下一篇**：[04-Tomcat会话管理与安全](04-Tomcat会话管理与安全.md)
