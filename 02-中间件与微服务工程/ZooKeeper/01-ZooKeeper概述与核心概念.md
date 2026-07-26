# 01 - ZooKeeper 概述与核心概念

> 🎯 ZooKeeper 是 Apache 的分布式协调服务 — CP 系统代表、ZAB 原子广播、顺序一致性。理解它的设计哲学才能理解 Dubbo/Kafka/Hadoop 的协调层

---

## 目录

1. [ZooKeeper 是什么](#1-zookeeper-是什么)
2. [核心应用场景](#2-核心应用场景)
3. [集群角色与 ZAB 协议](#3-集群角色与-zab-协议)
4. [ZooKeeper vs etcd vs Consul](#4-zookeeper-vs-etcd-vs-consul)
5. [谁在用 ZooKeeper](#5-谁在用-zookeeper)

---

## 1. ZooKeeper 是什么

ZooKeeper 是 Apache 开源的分布式协调服务，提供**强一致性**的数据存储和通知机制。

| 特性 | 说明 |
|------|------|
| **数据模型** | 类文件系统的树形节点（ZNode） |
| **一致性** | 顺序一致性（ZAB 协议保证） |
| **CAP 定位** | CP 系统（Leader 宕机期间不可写） |
| **Session** | 客户端与 ZK 建立长连接，心跳保活 |
| **Watch** | 数据变更推送通知（一次性触发） |
| **集群** | 奇数台（≥3），过半存活即可用 |

---

## 2. 核心应用场景

| 场景 | 实现原理 | 使用者 |
|------|----------|--------|
| **配置中心** | 持久 ZNode + Watch 推送变更 | 早期 Dubbo、Hadoop |
| **分布式锁** | 临时顺序节点 + Watch 前驱 | Curator |
| **Leader 选举** | 临时顺序节点 + 最小序号当选 | Kafka Controller、HBase Master |
| **服务发现** | 临时节点 + 服务名路径 | Dubbo（已迁移到 Nacos） |

---

## 3. 集群角色与 ZAB 协议

### 3.1 三种角色

| 角色 | 职责 |
|------|------|
| **Leader** | 处理所有写请求，发起投票（Proposal），协调提交 |
| **Follower** | 参与投票，转发写请求到 Leader，处理读请求 |
| **Observer** | 不参与投票，只处理读请求（提高读性能） |

### 3.2 ZAB 协议（ZooKeeper Atomic Broadcast）

```text
ZAB 两种模式：

1. 广播模式（正常运行）：
   Leader 收到写请求 → 生成 Proposal → 广播给 Followers
   → 过半 Follower ACK → Leader 发 Commit → 所有节点提交

2. 恢复模式（Leader 选举）：
   集群启动/Leader 宕机 → 选举新 Leader
   → 新 Leader 确保自己拥有所有已提交事务
   → 同步数据给 Followers → 进入广播模式
```

### 3.3 ZAB vs Raft

| 维度 | ZAB | Raft |
|------|-----|------|
| 心跳方向 | Leader → Follower | Leader → Follower |
| 日志提交 | **所有** Follower ACK | **多数派** ACK |
| 乱序提交 | ✅ 允许 | ❌ 严格顺序 |
| 代表实现 | ZooKeeper | etcd / Consul |

---

## 4. ZooKeeper vs etcd vs Consul

| 维度 | ZooKeeper | etcd | Consul |
|------|-----------|------|--------|
| 共识协议 | ZAB | Raft | Raft |
| 数据模型 | 树形 ZNode | 扁平 KV | KV + 服务 |
| 一致性 | 顺序一致性 | 强一致性 | CP/可配置 |
| Watch | 一次性 | 流式（持续） | 长轮询 |
| 语言 | Java | Go | Go |
| 生态 | Hadoop/Kafka/Dubbo | K8s | HashiCorp 生态 |
| 趋势 | 被 Nacos/etcd 替代 | ⭐ K8s 原生 | ⭐ HashiCorp |

---

## 5. 谁在用 ZooKeeper

| 项目 | 用途 |
|------|------|
| **Kafka** | Controller 选举、Broker 元数据、Consumer offset（老版本） |
| **Hadoop** | NameNode HA、YARN ResourceManager HA |
| **HBase** | Master 选举、RegionServer 注册 |
| **Dubbo 2.x** | 注册中心（3.x 已迁移到应用级+Nacos） |
| **Canal** | 集群选主 |

> 🎯 **现状**：ZK 在存量大厂系统中仍然广泛使用，但新项目微服务协调推荐 Nacos/etcd。学习 ZK 的核心价值是理解分布式协调的思想，而非工具本身。
