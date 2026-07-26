# 01 - Netty 概述与 IO 模型

> 🎯 Netty 的选择不是偶然 — BIO 的阻塞困境、NIO 的编程复杂度、AIO 的成熟度不足。理解 IO 模型的演进才能理解 Netty 为什么是最终答案

---

## 目录

1. [Netty 是什么](#1-netty-是什么)
2. [四种 IO 模型](#2-四种-io-模型)
3. [Reactor 线程模型](#3-reactor-线程模型)
4. [Netty 的 Reactor 实现](#4-netty-的-reactor-实现)
5. [谁在用 Netty](#5-谁在用-netty)

---

## 1. Netty 是什么

> Netty 是异步事件驱动的网络应用框架，用于快速开发高性能、高可靠性的网络 IO 程序。

| 特性 | 说明 |
|------|------|
| **异步 NIO** | 基于 Java NIO，非阻塞 IO |
| **Reactor 模型** | 主从 Reactor 多线程模型 |
| **零拷贝** | Direct Memory + CompositeByteBuf + FileRegion |
| **Pipeline** | 可插拔的 Handler 链，易扩展 |
| **高性能** | 百万级并发长连接 |

**Netty 在 Java 生态的定位**：

```
应用层     Dubbo / RocketMQ / Elasticsearch / gRPC / ZK
              ↑
网络层     Netty（统一网络通信框架）
              ↑
传输层     Java NIO / Epoll（Linux）
```

---

## 2. 四种 IO 模型

### 2.1 BIO（Blocking IO）

```java
// 每个连接一个线程 — 线程资源浪费
ServerSocket server = new ServerSocket(8080);
while (true) {
    Socket client = server.accept();   // ⚠️ 阻塞等待连接
    new Thread(() -> {
        InputStream in = client.getInputStream();
        byte[] buf = new byte[1024];
        int len = in.read(buf);        // ⚠️ 阻塞等待数据
        // 处理...
    }).start();
}
```

| 问题 | 说明 |
|------|------|
| 线程消耗 | 1 连接 = 1 线程，10K 连接 = 10K 线程 → OOM |
| 阻塞浪费 | 线程大部分时间在等待 IO（不消耗 CPU 但占内存） |
| 切换开销 | 大量线程上下文切换 |

### 2.2 NIO（Non-blocking IO）

```java
// 一个线程管理多个连接 — Selector 多路复用
Selector selector = Selector.open();
ServerSocketChannel server = ServerSocketChannel.open();
server.configureBlocking(false);
server.register(selector, SelectionKey.OP_ACCEPT);

while (true) {
    selector.select();                  // 阻塞直到有事件
    Set<SelectionKey> keys = selector.selectedKeys();
    for (SelectionKey key : keys) {
        if (key.isAcceptable()) { /* 处理新连接 */ }
        if (key.isReadable())   { /* 处理读事件 */ }
    }
}
```

| 优势 | 说明 |
|------|------|
| 少线程 | 1 个 Selector 管理 N 个 Channel |
| 非阻塞 | 没有数据可读时立即返回 |
| 事件驱动 | 有事件才处理，不空转 |

### 2.3 四种模型对比

| 模型 | 阻塞 | 数据读写 | 线程模型 | 适用 |
|------|:---:|------|------|------|
| **BIO** | ✅ | 流式 | 1 连接 1 线程 | 低并发（< 1000） |
| **NIO** | ❌ | 缓冲区 | 1 线程 N 连接 | ⭐ 高并发 |
| **AIO** | ❌ | 回调 | OS 通知 | Windows（Linux 不成熟） |
| **Epoll** | ❌ | 缓冲区 | 1 线程 N 连接 | Linux 高性能（Netty 底层） |

> ⚠️ **Java NIO 的痛点**：API 复杂（Buffer/Channel/Selector）、Epoll 空轮询 Bug、断线重连/粘包拆包需手写 → **Netty 封装了这一切**。

---

## 3. Reactor 线程模型

### 3.1 单 Reactor 单线程

```
Reactor（单线程）
  ├── 接收连接（accept）
  ├── 读取数据（read）
  ├── 业务处理（handler）
  └── 发送响应（send）

→ 最简单，但 handler 阻塞会拖慢所有连接
```

### 3.2 单 Reactor 多线程

```
Reactor（单线程）           Worker 线程池
  ├── 接收连接（accept）      ├── handler 业务1
  └── 读取数据（read）        ├── handler 业务2
         │                    └── handler 业务3
         └──→ 分发到 Worker 线程池
```

### 3.3 主从 Reactor 多线程（⭐ Netty 采用）

```
Main Reactor（Boss Group）          Sub Reactor（Worker Group）
  ├── 接收连接（accept）              ├── Worker-1: read → handler → send
  │                                  ├── Worker-2: read → handler → send
  └──→ 注册到 Worker Group           └── Worker-3: read → handler → send
```

| 角色 | 职责 | 线程数 |
|------|------|:---:|
| **Boss Group** | 接收 TCP 连接，注册到 Worker | 1（通常） |
| **Worker Group** | 处理 IO 读写 + 业务 Handler | CPU 核数 × 2 |

---

## 4. Netty 的 Reactor 实现

```java
// Netty 主从 Reactor 代码实现
EventLoopGroup bossGroup = new NioEventLoopGroup(1);       // Boss: 1 线程
EventLoopGroup workerGroup = new NioEventLoopGroup();      // Worker: CPU × 2

ServerBootstrap bootstrap = new ServerBootstrap();
bootstrap.group(bossGroup, workerGroup)
         .channel(NioServerSocketChannel.class)
         .childHandler(new ChannelInitializer<SocketChannel>() {
             @Override
             protected void initChannel(SocketChannel ch) {
                 ch.pipeline().addLast(new MyHandler());   // 业务 Handler
             }
         });

ChannelFuture future = bootstrap.bind(8080).sync();
```

---

## 5. 谁在用 Netty

| 项目 | 用途 |
|------|------|
| **Dubbo** | RPC 通信（dubbo 协议基于 Netty） |
| **RocketMQ** | Broker-Client 消息通信 |
| **Elasticsearch** | 节点间通信（Zen Discovery + Transport） |
| **gRPC** | Java 服务端/客户端（Netty 是默认传输层） |
| **ZooKeeper** | 3.6+ 版本 Netty 替代 NIO |
| **Spring WebFlux** | Reactor Netty 作为默认服务器 |
| **Spark** | Shuffle 数据传输 |

> 🎯 **Netty 是 Java 中间件的"通用 TCP 引擎"** — 掌握了 Netty，就掌握了 Java 网络通信的底层密码。
