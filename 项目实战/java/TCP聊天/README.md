# TCP 聊天 (TCP Chat)

> 基于 Java Socket 的 TCP 实时聊天系统，C/S 架构 + 多线程

## 项目概述

基于 Java 原生 Socket API 开发的 TCP 实时聊天系统。采用经典 Client-Server 架构，服务端使用多线程处理并发客户端连接，支持多人同时在线聊天。通过 TCP 协议保证消息可靠传输。

## 技术栈

| 技术 | 说明 |
|------|------|
| Java | JDK 8+ |
| java.net.Socket | TCP 客户端连接 |
| java.net.ServerSocket | TCP 服务端监听 |
| 多线程 | Thread / ExecutorService 并发处理 |
| I/O Streams | BufferedReader / PrintWriter 消息收发 |
| Swing（可选） | GUI 聊天界面 |

## 功能特性

- **多人同时在线**：服务端线程池处理并发客户端连接
- **广播消息**：某客户端发送消息，服务端转发给所有在线客户端
- **私聊消息**：指定接收者发送私密消息
- **上线/下线通知**：客户端进入和离开的广播通知
- **昵称设置**：进入聊天室时设置显示昵称
- **在线用户列表**：查看当前在线用户

## 项目结构

```
TCP聊天/
├── src/main/java/com/tcpchat/
│   ├── server/
│   │   ├── ChatServer.java          # 服务端入口
│   │   └── ClientHandler.java       # 客户端连接处理器（线程）
│   ├── client/
│   │   ├── ChatClient.java          # 客户端入口
│   │   └── MessageReceiver.java     # 消息接收线程
│   ├── protocol/
│   │   └── Message.java             # 消息格式定义
│   └── util/
│       └── MessageUtil.java         # 消息序列化工具
├── pom.xml
└── README.md
```

## 通信协议设计

```
消息格式：TYPE|SENDER|CONTENT

消息类型：
- MSG      广播消息
- PRIVATE  私聊消息
- JOIN     加入通知
- LEAVE    离开通知
- USERS    在线用户列表
- SYSTEM   系统消息
```

## 快速开始

```bash
# 编译
mvn compile

# 先启动服务端
java -cp target/classes com.tcpchat.server.ChatServer

# 再启动客户端（可开多个终端）
java -cp target/classes com.tcpchat.client.ChatClient
```

## 核心知识点

| 知识点 | 说明 |
|--------|------|
| ServerSocket.accept() | 阻塞等待客户端连接 |
| Socket I/O | getInputStream() / getOutputStream() |
| 多线程处理 | 每个客户端一个线程（ClientHandler） |
| 线程安全集合 | ConcurrentHashMap 管理在线客户端 |
| 消息广播 | 遍历所有客户端连接发送消息 |
| 资源管理 | try-with-resources 确保 Socket 关闭 |

## TCP vs UDP 聊天对比

| 特性 | TCP 聊天 | UDP 聊天 |
|------|----------|----------|
| 连接 | 面向连接（三次握手） | 无连接 |
| 可靠性 | 保证送达、有序 | 不保证送达 |
| 速度 | 较慢 | 较快 |
| 资源消耗 | 较高（维护连接状态） | 较低 |
| 适用场景 | 私聊、文件传输 | 实时语音/视频、广播 |

## 注意事项

- 服务端需处理客户端异常断开的清理逻辑
- BufferedReader.readLine() 是阻塞操作，需在独立线程中运行
- 多线程共享集合务必使用线程安全实现（ConcurrentHashMap）
- 消息协议需约定好边界（行分隔 / 长度前缀 / 特殊字符）
