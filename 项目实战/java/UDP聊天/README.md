# UDP 聊天 (UDP Chat)

> 基于 Java DatagramSocket 的 UDP 即时通讯系统，支持单播/组播/广播

## 项目概述

基于 Java DatagramSocket 的 UDP 即时通讯系统。无需建立持久连接，通过数据报文（DatagramPacket）直接发送消息。相较于 TCP 聊天，UDP 具有更低延迟和更小的资源消耗，适合实时性要求高的场景（如语音通话、视频流）。

## 技术栈

| 技术 | 说明 |
|------|------|
| Java | JDK 8+ |
| java.net.DatagramSocket | UDP 套接字 |
| java.net.DatagramPacket | 数据报文 |
| java.net.MulticastSocket | 组播套接字 |
| java.net.InetAddress | IP 地址处理 |
| 多线程 | 收发分离线程 |

## 功能特性

- **UDP 单播**：点对点消息发送
- **UDP 广播**：向局域网内所有主机发送消息（255.255.255.255）
- **UDP 组播**：加入组播组（如 224.0.0.1），组内成员共享消息
- **收发分离**：发送线程 + 接收线程独立运行
- **低延迟**：无连接建立和确认机制，消息即时发送

## 项目结构

```
UDP聊天/
├── src/main/java/com/udpchat/
│   ├── unicast/
│   │   ├── UDPSender.java           # 单播发送端
│   │   └── UDPReceiver.java         # 单播接收端
│   ├── broadcast/
│   │   ├── BroadcastSender.java     # 广播发送端
│   │   └── BroadcastReceiver.java   # 广播接收端
│   ├── multicast/
│   │   ├── MulticastSender.java     # 组播发送端
│   │   └── MulticastReceiver.java   # 组播接收端
│   └── chat/
│       ├── UDPChatClient.java       # 聊天客户端
│       └── MessageHandler.java      # 消息处理
├── pom.xml
└── README.md
```

## 通信模式对比

| 模式 | 地址 | 说明 |
|------|------|------|
| 单播 (Unicast) | 具体 IP | 一对一通信 |
| 广播 (Broadcast) | 255.255.255.255 | 局域网内所有主机接收 |
| 组播 (Multicast) | 224.0.0.0 ~ 239.255.255.255 | 加入同一组的主机接收 |

## 快速开始

```bash
# 编译
mvn compile

# 单播示例：先启动接收端，再启动发送端
java -cp target/classes com.udpchat.unicast.UDPReceiver
java -cp target/classes com.udpchat.unicast.UDPSender

# 广播示例
java -cp target/classes com.udpchat.broadcast.BroadcastReceiver
java -cp target/classes com.udpchat.broadcast.BroadcastSender

# 组播示例
java -cp target/classes com.udpchat.multicast.MulticastReceiver
java -cp target/classes com.udpchat.multicast.MulticastSender
```

## 核心知识点

| 知识点 | 说明 |
|--------|------|
| DatagramSocket | UDP 通信端点，绑定端口 |
| DatagramPacket | 封装数据报文（数据 + 地址 + 端口） |
| socket.send(packet) | 发送数据报文 |
| socket.receive(packet) | 阻塞接收数据报文 |
| MulticastSocket | 组播通信，joinGroup/leaveGroup |
| InetAddress.getByName() | 解析地址字符串 |
| setBroadcast(true) | 开启广播权限 |

## TCP vs UDP 聊天对比

| 特性 | TCP 聊天 | UDP 聊天 |
|------|----------|----------|
| 连接方式 | 面向连接（三次握手） | 无连接 |
| 可靠性 | 保证送达、有序 | 不保证送达、可能丢包 |
| 传输速度 | 较慢（有确认机制） | 较快（直接发送） |
| 资源消耗 | 较高（维护连接状态） | 较低 |
| 数据大小 | 无限制（流式） | 单包 ≤ 64KB |
| 适用场景 | 私聊、文件传输 | 实时语音/视频、广播通知 |

## 注意事项

- UDP 不保证消息送达，重要消息需应用层实现确认机制
- DatagramPacket 缓冲区大小限制为 65507 字节（UDP 最大载荷）
- 广播需设置 `socket.setBroadcast(true)`
- 组播地址范围：224.0.0.0 ~ 239.255.255.255
- receive() 是阻塞方法，需在独立线程中运行
- 路由器通常不转发广播包，广播仅限于局域网
