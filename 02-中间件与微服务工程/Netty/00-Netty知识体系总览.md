# 00 - Netty 知识体系总览

> 🎯 Netty 是 Java 高性能网络编程的事实标准 — Dubbo、RocketMQ、Elasticsearch、gRPC 的通信层都基于它。理解 Reactor 模型和 Pipeline 机制是进阶 Java 后端的必经之路

---

## 目录

1. [知识全景](#1-知识全景)
2. [文件导航](#2-文件导航)
3. [精通级学习路线](#3-精通级学习路线)

---

## 1. 知识全景

```
Netty 精通体系（8个文件）
│
├── 🏗️ 基础入门（01-03）
│   ├── 01-Netty概述与IO模型.md      # BIO/NIO/AIO/Reactor/Netty架构定位
│   ├── 02-第一个Netty应用.md         # Server/Bootstrap/启动流程
│   └── 03-Channel与EventLoop.md     # Channel生命周期/EventLoopGroup/线程模型
│
├── 🔧 核心机制（04-05）
│   ├── 04-ChannelPipeline与Handler.md # Pipeline链/入站出站/Handler传播
│   └── 05-编解码与粘包拆包.md          # Encoder/Decoder/定长/分隔符/长度域
│
├── 💎 内存管理（06）
│   └── 06-ByteBuf内存管理.md         # 堆/直接内存/池化/引用计数/零拷贝
│
├── 🚀 进阶实战（07-08）
│   ├── 07-心跳与空闲检测.md           # IdleStateHandler/心跳/重连机制
│   └── 08-高性能调优与最佳实践.md      # 参数调优/内存泄漏/常见模式
│
└── 📌 00-Netty知识体系总览.md        # ← 本文件
```

---

## 2. 文件导航

| # | 文件 | 核心内容 | 级别 |
|---|------|----------|:---:|
| 00 | Netty知识体系总览 | 全景导航 + 学习路线 | — |
| 01 | Netty概述与IO模型 | BIO/NIO/AIO/Reactor/Netty 定位 | ⭐⭐ |
| 02 | 第一个Netty应用 | ServerBootstrap/ChannelInitializer/启动流程 | ⭐ |
| 03 | Channel与EventLoop | Channel 生命周期/EventLoopGroup/线程绑定 | ⭐⭐⭐ |
| 04 | ChannelPipeline与Handler | Pipeline 双向链表/入站出站/Handler 传播 | ⭐⭐⭐ |
| 05 | 编解码与粘包拆包 | Encoder/Decoder/定长/分隔符/LengthFieldBased | ⭐⭐⭐ |
| 06 | ByteBuf内存管理 | 堆/直接/池化/引用计数/Composite/slice 零拷贝 | ⭐⭐⭐⭐ |
| 07 | 心跳与空闲检测 | IdleStateHandler/心跳/断线重连/连接保活 | ⭐⭐ |
| 08 | 高性能调优与最佳实践 | Boss-Worker调优/内存泄漏检测/常见设计模式 | ⭐⭐⭐ |

---

## 3. 精通级学习路线

### 🟢 L1：能写一个 Netty 服务（半天）

```
01-IO模型 → 02-第一个应用
产出：能跑通 Netty Server/Client，理解启动流程
```

### 🔵 L2：理解核心机制（1天）

```
03-Channel+EventLoop → 04-Pipeline+Handler → 05-编解码
产出：能自定义协议编解码、理解事件传播机制
```

### 🟣 L3：掌握内存模型（半天）

```
06-ByteBuf内存管理
产出：理解池化/直接内存/零拷贝，能排查内存泄漏
```

### 🟡 L4：生产落地（半天）

```
07-心跳重连 → 08-高性能调优
产出：能实现断线重连、调优线程模型、排查常见故障
```

---

> 🎯 **Netty 是 Java 中间件的通用网络层** — 理解 Netty 就是理解 Dubbo/RocketMQ/ES 通信原理的捷径。
