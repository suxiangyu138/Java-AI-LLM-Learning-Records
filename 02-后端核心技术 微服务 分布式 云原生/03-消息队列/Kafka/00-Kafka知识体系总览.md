# 00 - Kafka 知识体系总览

> 🎯 Kafka 是分布式消息队列的事实标准 — 万亿级消息处理、高吞吐低延迟、持久化存储。理解 Kafka 才能驾驭现代数据管道和事件驱动架构

---

## 目录

1. [知识全景](#1-知识全景)
2. [文件导航](#2-文件导航)
3. [精通级学习路线](#3-精通级学习路线)

---

## 1. 知识全景

```
Kafka 精通体系（10个文件）
│
├── 🏗️ 基础入门（01-02）
│   ├── 01-Kafka概述与消息演进.md       # MQ 演进/Kafka 定位/核心特性/与 RocketMQ 对比
│   └── 02-快速入门与安装部署.md         # 单机/集群/Docker/KRaft 模式/基础命令行
│
├── 🔧 核心原理（03-05）
│   ├── 03-核心架构与存储原理.md         # 分层架构/日志存储/分段/ISR/水印机制
│   ├── 04-生产者深度解析.md             # 分区策略/ACK 机制/幂等/事务/批量发送
│   └── 05-消费者与消费组.md             # 消费组/Rebalance/位移管理/再均衡策略
│
├── 🚀 进阶实战（06-08）
│   ├── 06-Broker与集群管理.md           # Controller/KRaft/分区副本/Leader 选举/故障恢复
│   ├── 07-Kafka性能优化与监控.md        # 零拷贝/页缓存/参数调优/带宽控制/JMX 监控
│   └── 08-Kafka与Spring生态集成.md      # Spring Kafka/配置实战/消息转换/错误处理
│
├── 📋 生态与面试（09）
│   └── 09-Kafka生态与最佳实践.md        # Streams/Connect/MirrorMaker/生产避坑/高频面试题
│
└── 📌 00-Kafka知识体系总览.md            # ← 本文件
```

---

## 2. 文件导航

| # | 文件 | 核心内容 | 级别 |
|---|------|----------|:---:|
| 00 | Kafka知识体系总览 | 全景导航 + 学习路线 | — |
| 01 | Kafka概述与消息演进 | MQ 演进史/Kafka 核心特性/与 RocketMQ 对比/适用场景 | ⭐⭐ |
| 02 | 快速入门与安装部署 | 单机/集群/Docker-Compose/KRaft/ZK 模式/命令行工具 | ⭐ |
| 03 | 核心架构与存储原理 | Topic/Partition/Segment/ISR/HW/LEO/日志清理/零拷贝 | ⭐⭐⭐⭐ |
| 04 | 生产者深度解析 | 分区策略/ACK 机制(0/1/all)/幂等性/事务/Sender 线程 | ⭐⭐⭐ |
| 05 | 消费者与消费组 | 消费组/Rebalance 协议/位移提交(idx/stg/auto)/再均衡监听器 | ⭐⭐⭐⭐ |
| 06 | Broker与集群管理 | Controller 选举/分区副本分配/Leader 切换/KRaft 共识 | ⭐⭐⭐ |
| 07 | Kafka性能优化与监控 | OS 页缓存/零拷贝 sendfile/批量压缩/JMX 指标/带宽控制 | ⭐⭐⭐ |
| 08 | Kafka与Spring生态集成 | Spring Kafka 配置/KafkaTemplate/Listener 容器/错误处理 | ⭐⭐ |
| 09 | Kafka生态与最佳实践 | Streams/Connect/MirrorMaker/生产避坑/20+ 高频面试题 | ⭐⭐⭐ |

---

## 3. 精通级学习路线

### 🟢 L1：认识 Kafka，搭起来用起来（半天）

```
01-概述 → 02-安装部署
产出：能搭建 Kafka 集群、用 CLI 收发消息
```

### 🔵 L2：理解存储与生产消费模型（1天）

```
03-核心架构与存储原理 → 04-生产者深度解析
产出：理解 Partition/ISR/HW/LEO、掌握生产者 ACK 机制
```

### 🟣 L3：掌握消费者与集群运维（1天）

```
05-消费者与消费组 → 06-Broker与集群管理
产出：理解 Rebalance 机制、能运维 Kafka 集群
```

### 🟡 L4：性能调优与 Spring 集成（半天）

```
07-Kafka性能优化与监控 → 08-Kafka与Spring生态集成
产出：能定位性能瓶颈、在 Spring Boot 中集成 Kafka
```

### 🔴 L5：生态扩展与面试冲刺（半天）

```
09-Kafka生态与最佳实践 → 系统回顾
产出：了解 Kafka 生态、拿下高频面试题
```

---

## 4. Kafka 版本说明

| 版本 | 关键变化 |
|------|----------|
| **0.10.x** | 引入消息时间戳、Consumer Interceptor |
| **1.0.x** | Java 9 支持、磁盘故障容错增强 |
| **2.0.x** | 前缀 ACL、Hostname 验证、动态配置增强 |
| **2.4.x** | Sticky Partitioner、增量 Rebalance |
| **2.8.x** | KRaft 早期预览（ZooKeeper-less） |
| **3.0.x** | KRaft 生产可用、ZooKeeper 废弃 |
| **3.3.x** | KRaft 稳定版、完全去除 ZK |

> 💡 本文档基于 Kafka 3.x 编写，覆盖 KRaft 模式（推荐）和传统 ZK 模式。

---

## 5. 前置知识要求

| 知识点 | 重要程度 | 说明 |
|--------|:---:|------|
| Java 基础（多线程/IO） | ⭐⭐⭐⭐ | Kafka 客户端及源码分析需要 |
| Linux 基础 | ⭐⭐⭐ | 部署运维、OS 参数调优 |
| 计算机网络 | ⭐⭐⭐ | TCP 连接管理、零拷贝原理 |
| ZAB/Raft 共识算法 | ⭐⭐ | Controller 选举、KRaft 理解 |
| Spring Boot | ⭐⭐ | Spring Kafka 集成 |
| OS 文件系统 | ⭐⭐ | 页缓存、顺序读写、sendfile |
