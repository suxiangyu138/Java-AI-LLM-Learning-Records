# 02 - 分布式共识算法：Paxos 与 Raft

> 🎯 共识算法是分布式一致性的发动机 — Paxos 是理论基础、Raft 是工程实现、ZAB 是 ZooKeeper 的定制方案。理解 Raft 的选举+复制+安全三子问题是面试和架构设计的高频考点

---

## 目录

1. [共识问题定义](#1-共识问题定义)
2. [Paxos：理论的基石](#2-paxos理论的基石)
3. [Raft：可理解性优先](#3-raft可理解性优先)
4. [ZAB：ZooKeeper 的共识协议](#4-zabzookeeper-的共识协议)
5. [其他共识算法](#5-其他共识算法)
6. [面试高频问题](#6-面试高频问题)

---

## 1. 共识问题定义

> **共识（Consensus）**：多个节点就某个值达成一致。

### 1.1 三要素

| 要素 | 定义 |
|------|------|
| **终止性** | 每个正确节点最终都会做出决定 |
| **一致性** | 所有正确节点决定的值相同 |
| **有效性** | 决定的值必须是某个节点提出的 |

### 1.2 FLP 不可能性（1985）

> ⚠️ Fisher-Lynch-Paterson 证明：在**纯异步模型**中，即使只有一个节点故障，也不存在确定性的共识算法。

**但实践中可以达成共识**：通过随机超时（部分同步模型）— Raft 的随机选举超时（150-300ms）正是利用这一点规避 FLP。

---

## 2. Paxos：理论的基石

### 2.1 角色

| 角色 | 职责 |
|------|------|
| **Proposer** | 提案者，发起提案 |
| **Acceptor** | 接受者，投票决定是否接受提案 |
| **Learner** | 学习者，学习已达成共识的值 |

### 2.2 Basic Paxos 两阶段

```
Phase 1（Prepare 准备阶段）：
  Proposer → 选一个全局唯一的 Proposal Number n
           → 发送 Prepare(n) 给所有 Acceptor
  Acceptor → 如果 n > 之前见过的所有 Prepare Number：
             承诺不再接受 < n 的提案
             返回曾接受过的最大编号的提案 (v, n')

Phase 2（Accept 接受阶段）：
  Proposer → 收到多数派 Promise 后：
             如果 Acceptor 返回了之前接受的提案 → 选编号最大的那个值
             如果都没返回 → 选自己的值
           → 发送 Accept(n, value) 给所有 Acceptor
  Acceptor → 如果 n ≥ 承诺过的最小编号：
             接受该提案 → 共识达成！
```

### 2.3 Multi-Paxos

> 💡 Basic Paxos 每次达成一个值的共识。Multi-Paxos 为多个值连续达成共识。

```
优化：选出一个 Leader，只有 Leader 发起提案
  → Phase 1 只需执行一次（Leader 任期开始时）
  → 后续提案直接从 Phase 2 开始
  → 相当于两轮消息 → 一轮消息
```

### 2.4 Paxos 的难点

| 问题 | 说明 |
|------|------|
| 理论精巧但难理解 | 角落案例多，论文晦涩 |
| 工程实现复杂 | 日志压缩、成员变更、Leader 选举未定义 |
| Multi-Paxos 无标准 | Lamport 未给出完整算法 |

---

## 3. Raft：可理解性优先

> 🎯 Diego Ongaro, 2014 — 设计哲学：将共识分解为三个相对独立的子问题。

### 3.1 三种角色与状态转换

```
        超时，开始选举
  Follower ──────────→ Candidate
     ↑                    │
     │  发现新 Leader       │ 获得多数票
     │  或更高 term        │
     │                    ↓
     └────────────────── Leader
           发现更高 term（退位）
```

| 角色 | 行为 |
|------|------|
| **Leader** | 处理所有客户端请求，发送心跳，复制日志到 Followers |
| **Follower** | 被动响应 Leader 的 RPC，不主动发起请求 |
| **Candidate** | 竞选 Leader，向其他节点请求投票 |

### 3.2 三个子问题

#### 子问题 1：Leader 选举

```text
1. Follower 超时（150-300ms 随机）未收到 Leader 心跳
2. → 转为 Candidate，term++
3. → 投票给自己，向其他节点发送 RequestVote RPC
4. → 获得多数票 → 成为 Leader
5. → 立即发送心跳（AppendEntries RPC with no log）确立权威

关键设计：
  - 随机超时（150-300ms）防止多个 Candidate 同时竞选导致平票
  - term（任期号）单调递增，旧 term 的消息被拒绝
  - Candidate 的日志至少和多数节点一样新（安全性约束）
```

#### 子问题 2：日志复制

```text
1. Client 请求 → Leader 追加到本地日志
2. Leader 并行发送 AppendEntries RPC 给所有 Followers
3. 多数派确认 → Leader 提交（commit = 应用到状态机 + 回复 Client）
4. Leader 在下一次心跳中告知 Followers commit 位置
5. Followers 应用已提交日志到状态机

关键设计：
  - 日志匹配特性：相同 index+term 的日志条目一定相同
  - Leader 强制 Followers 日志与自己的匹配（找到最后一致点，覆盖不一致的后缀）
```

#### 子问题 3：安全性

```text
1. 选举限制：Candidate 的日志必须包含所有已提交的条目
   → RequestVote RPC 携带 candidate 最后日志的 index+term
   → Voter 只投票给日志不比自己的旧的 Candidate

2. 只提交当前 term 的日志：
   → 避免前任 Leader 的日志被"强制"提交后又丢失
```

### 3.3 日志压缩（Snapshot）

```
Raft 日志会无限增长 → 定期做 Snapshot：

Leader 将当前状态机快照 + 快照的最后 index+term
  → 发送给慢 Follower（InstallSnapshot RPC）
  → 替代重放大量日志
```

### 3.4 Raft 工程实现

| 系统 | 用途 |
|------|------|
| **etcd** | Kubernetes 的后端存储，用 Raft 保证强一致性 |
| **Consul** | 服务发现 + KV 存储，Raft 保证一致性 |
| **TiKV** | TiDB 的分布式 KV 层，Multi-Raft 架构 |
| **Nacos** | CP 模式用 Raft（JRaft） |

### 3.5 Raft vs Paxos

| 维度 | Paxos | Raft |
|------|-------|------|
| 可理解性 | ⭐ 极难 | ⭐⭐⭐ 清晰 |
| Leader 选举 | 未定义（需额外设计） | 明确定义 |
| 日志复制 | Multi-Paxos 不标准 | 标准流程 |
| 成员变更 | 复杂（多人多版） | 联合共识（Joint Consensus） |
| 工业实现 | Google Chubby | etcd/Consul/TiKV |
| 学习资料 | Lamport 论文 | [raft.github.io](https://raft.github.io) 动画 |

---

## 4. ZAB：ZooKeeper 的共识协议

> ZAB（ZooKeeper Atomic Broadcast）是 ZooKeeper 专用的共识协议，类似 Raft 但有所不同。

### 4.1 两种模式

| 模式 | 说明 |
|------|------|
| **广播模式** | Leader 正常运行时，将事务原子广播给 Followers |
| **恢复模式** | Leader 选举期间，确保新 Leader 包含所有已提交的事务 |

### 4.2 ZAB vs Raft

| 维度 | ZAB | Raft |
|------|-----|------|
| 心跳方向 | Leader → Follower（相同） | Leader → Follower |
| 日志提交 | 所有 Follower 都 ACK 才算提交 | 多数派 ACK 即可 |
| 乱序提交 | 允许 | 不允许（index 严格递增） |

---

## 5. 其他共识算法

| 算法 | 特点 | 应用 |
|------|------|------|
| **PBFT** | 拜占庭容错，容忍 ≤1/3 恶意节点 | 联盟链（Hyperledger Fabric） |
| **PoW** | 算力竞赛，最终一致性 | 比特币 |
| **PoS** | 权益质押 | 以太坊 2.0 |
| **HotStuff** | BFT 类，线性通信复杂度 | Libra/Diem |

---

## 6. 面试高频问题

### Q1：Raft 选举时如何保证不会出现两个 Leader？

> 一个 term 内每个 Server 只能投一票。Candidate 需要获得多数票（≥ N/2+1）才能成为 Leader。多数派原则保证了同一 term 最多一个 Leader。

### Q2：为什么 Raft 只提交当前 term 的日志？

> 防止"已提交日志被覆盖"。如果前任 Leader 提交了一条日志但部分 Follower 未收到，新任 Leader 覆盖这条日志时会违反安全性。Raft 通过"只提交当前 term 日志"来规避。

### Q3：为什么 Raft 选举需要随机超时？

> 防止多个 Candidate 同时超时、同时竞选，导致平票无人胜出。随机超时（150-300ms）使不同节点的超时时间错开，大概率只有一个节点先超时发起选举。

> 🎯 **记忆口诀**：Paxos 是理论（理解两阶段即可），Raft 是实践（选举+复制+安全三子问题），MUST 看 [raft.github.io](https://raft.github.io) 动画。
