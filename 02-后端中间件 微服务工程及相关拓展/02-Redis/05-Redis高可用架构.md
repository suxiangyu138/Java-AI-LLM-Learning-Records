# Redis 高可用架构
> 从单机到集群的进阶之路：主从复制 → 哨兵模式 → Redis Cluster，逐级解决单点故障、读写瓶颈、数据分片问题。

## 目录
1. [主从复制](#1-主从复制)
2. [哨兵模式 Sentinel](#2-哨兵模式-sentinel)
3. [Redis Cluster 集群](#3-redis-cluster-集群)
4. [一致性哈希](#4-一致性哈希)
5. [高可用方案对比](#5-高可用方案对比)

---

## 1. 主从复制

### 1.1 架构
```text
         ┌─────────┐
         │  Master  │  ← 写操作
         └────┬─────┘
       ┌──────┼──────┐
       │      │      │
  ┌────▼─┐ ┌──▼──┐ ┌▼────┐
  │ Slave│ │Slave│ │Slave│  → 读操作
  └──────┘ └─────┘ └─────┘
```

主节点负责写操作，从节点负责读操作，实现读写分离。主节点数据变更后异步同步到从节点。

### 1.2 PSYNC2 复制流程

**全量复制（首次或 offset 不在 backlog 内）：**
```text
1. Slave 发送 PSYNC ? -1
2. Master 返回 +FULLRESYNC <runid> <offset>
3. Master 执行 BGSAVE 生成 RDB
4. Master 发送 RDB 给 Slave
5. Slave 清空旧数据，加载 RDB
6. Master 发送复制缓冲区增量命令
7. Slave 执行增量命令，追上主库
```

**增量复制（断线重连，offset 在 backlog 内）：**
```text
1. Slave 发送 PSYNC <runid> <offset>
2. Master 检查 offset 在 repl-backlog 内
3. Master 发送 +CONTINUE
4. Master 发送 backlog 中的缺失命令
5. Slave 执行缺失命令，追上主库
```

**PSYNC2 相对于 PSYNC 的优化：**
- 主从切换后，新 Master 的 runid 变化，PSYNC 需要全量复制
- PSYNC2 通过复制偏移量+主节点 repl-id，切换后仍可能增量同步
- Redis 4.0+ 默认使用 PSYNC2

### 1.3 配置

```bash
# Slave 节点配置
replicaof 192.168.1.10 6379
replica-serve-stale-data yes   # 同步中是否提供服务
replica-read-only yes          # 从节点只读

# Master 配置
repl-backlog-size 1mb          # 复制缓冲区大小
repl-backlog-ttl 3600          # 缓冲区过期时间
repl-diskless-sync no          # 是否无盘同步
min-replicas-to-write 1        # 最少从节点数才可写
min-replicas-max-lag 10        # 从节点最大延迟
```

> 💡 `repl-backlog-size` 根据写入量调整：`backlog_size = 主库断线时间(秒) × 每秒写入量 × 2`。公式：若主库断线 60 秒内每秒写入 1MB，则设为 120MB。

### 1.4 复制风暴与解决方案

**问题：** 主节点故障后，大量从节点同时尝试全量复制主节点，导致带宽耗尽。

**解决方案：**
- 树形复制结构：Slave 不仅复制 Master，还可复制其他 Slave
- 限制同时重连数量
- 使用 Sentinel 避免同时切换

```text
树形复制架构：
         ┌─────────┐
         │ Master  │
         └────┬────┘
         ┌────┴────┐
    ┌────▼──┐  ┌───▼────┐
    │Slave A│  │Slave B │
    └───┬───┘  └───┬────┘
   ┌────┴────┐    (不继续传播)
   │Slave A1│
   └────────┘
```

> ⚠️ 树形复制延迟会逐级增加，不适合对实时性要求高的场景。Slave 的 Slave 复制偏移量落后更多，故障转移时数据丢失风险也更大。

### 1.5 优缺点

| 优点 | 缺点 |
|------|------|
| 读写分离，提升读吞吐 | 主节点宕机需手动切换 |
| 数据热备份 | 写能力仍受单机限制 |
| 简单易配置 | 无法自动故障转移 |

---

## 2. 哨兵模式 Sentinel

### 2.1 核心功能

| 功能 | 说明 |
|------|------|
| 监控 | 定期检查主从节点健康状态 |
| 自动故障转移 | Master 宕机自动选新 Master |
| 通知 | 通知客户端新 Master 地址 |
| 配置提供 | 充当客户端服务发现 |

### 2.2 架构

```text
         ┌──────────────────┐
         │  Sentinel 集群   │  (至少 3 个，奇数)
         └───┬──┬──┬──┬────┘
             │  │  │  │
        ┌────▼──▼──▼──▼─────┐
        │     Master        │
        └────────┬──────────┘
            ┌────┼────┐
            │    │    │
         ┌──▼┐ ┌▼──┐ ┌▼──┐
         │ S1│ │ S2│ │ S3│
         └───┘ └───┘ └───┘
```

### 2.3 故障转移流程

```text
1. SDOWN（主观下线）
   单个 Sentinel 发现 Master 没有响应 PING

2. ODOWN（客观下线）
   多个 Sentinel（quorum 配置数）都认为 Master 下线
   通过 Sentinel 间的 Gossip 协议确认

3. Leader 选举（Raft 算法）
   Sentinel 集群选举 Leader 负责故障转移

4. 选新 Master
   优先级：replica-priority（越小越优先）
   → 复制偏移量最大的 Slave
   → runid 最小的 Slave

5. 通知
   所有 Sentinel 更新配置，客户端感知新 Master
```

> 💡 **SDOWN vs ODOWN**：SDOWN 是单一 Sentinel 的主观判断，ODOWN 是 quorum 个 Sentinel 达成共识后的客观下线。从节点只会有 SDOWN，不会产生 ODOWN。

### 2.4 Raft 算法在 Sentinel 中的应用

```text
Leader 选举过程：

1. 每个 Sentinel 有选举纪元（epoch），类似于 Raft 的 term
2. 当 ODOWN 确认后，Sentinel 自增 epoch，进入竞选状态
3. 向其他 Sentinel 发送投票请求
4. 每个 Sentinel 在一个 epoch 内只能投票一次（先到先得）
5. 获得半数以上投票的 Sentinel 成为 Leader
6. 如果无人过半，随机等待后重新选举
```

| Raft 概念 | Sentinel 对应 |
|-----------|--------------|
| Term | 选举纪元（epoch） |
| 候选人 | 发起投票的 Sentinel |
| 投票 | Sentinel 间投票应答 |
| 多数派 | N/2 + 1 票 |
| Log | 配置版本（config-epoch） |

### 2.5 配置

```bash
# sentinel.conf
sentinel monitor mymaster 127.0.0.1 6379 2   # 2 = quorum
sentinel down-after-milliseconds mymaster 5000   # 5 秒无响应判下
sentinel failover-timeout mymaster 60000         # 故障转移超时
sentinel parallel-syncs mymaster 1               # 同时同步的从节点数
sentinel auth-pass mymaster password             # 主节点密码
```

**参数解读：**
```text
parallel-syncs 1：故障转移后，新 Master 同时通知 1 个 Slave 同步
                 过大会导致新 Master 负载突增
failover-timeout 60000：如果 60 秒内故障转移未完成，视为失败

down-after-milliseconds 5000：连续 5 秒无法响应 PING
                              判为下线，避免网络抖动误判
```

### 2.6 部署要求

| 要求 | 说明 |
|------|------|
| 至少 3 个 Sentinel | 奇数，保证 quorum 投票有效 |
| quorum >= N/2 + 1 | 多数决策 |
| Sentinel 与 Master 分离部署 | 避免同时宕机 |

**选举可用性计算：**
```text
3 个 Sentinel, quorum=2：最多允许 1 个 Sentinel 宕机
5 个 Sentinel, quorum=3：最多允许 2 个 Sentinel 宕机
N 个 Sentinel, quorum=N/2+1：最多允许 (N-1)/2 个宕机
```

### 2.7 客户端配置

```java
// Jedis Sentinel 配置
Set<String> sentinels = new HashSet<>();
sentinels.add("192.168.1.1:26379");
sentinels.add("192.168.1.2:26379");
sentinels.add("192.168.1.3:26379");

JedisSentinelPool pool = new JedisSentinelPool("mymaster", sentinels);
try (Jedis jedis = pool.getResource()) {
    jedis.set("key", "value");
}
```

---

## 3. Redis Cluster 集群

### 3.1 数据分片：16384 个哈希槽

```text
key → CRC16(key) % 16384 → 存储到对应槽的节点

集群有 3 个节点时：
Node A: 0-5460   哈希槽
Node B: 5461-10922 哈希槽
Node C: 10923-16383 哈希槽
```

> 为什么是 16384 个槽？
> - 16384 个槽的位图仅 2KB，节点间交换槽信息非常高效
> - 如果用 65536 个槽，位图 8KB，心跳包更大
> - 对于典型集群规模（<1000 节点），16384 足够均衡

**槽分配计算：**
```text
槽位图大小 = 16384 / 8 = 2048 字节 = 2KB
如果是 65535 个槽：65535 / 8 = 8191 字节 ≈ 8KB

每个心跳包都携带槽位图，2KB vs 8KB，集群规模大时差异显著
```

### 3.2 请求路由

```text
客户端请求 key → CRC16(key) % 16384 计算槽
  ↓
节点本地槽 = 直接处理
  ↓
节点不拥有该槽 → 返回 MOVED 重定向
  ↓
MOVED <slot> <ip>:<port>  → 客户端重发到正确节点
```

| 重定向类型 | 说明 |
|-----------|------|
| MOVED | 槽归属已永久变更，客户端应更新路由表 |
| ASK | 槽正在迁移中，ASK 到目标节点 |
| CLUSTERDOWN | 集群不可用（部分槽没有节点服务） |

```java
// Smart 客户端示例（Jedis Cluster）
Set<HostAndPort> nodes = Set.of(
    new HostAndPort("127.0.0.1", 6379),
    new HostAndPort("127.0.0.1", 6380)
);

JedisCluster jedisCluster = new JedisCluster(nodes);
// Smart 客户端自动维护路由表，自动处理 MOVED/ASK
jedisCluster.set("foo", "bar");
String value = jedisCluster.get("foo");
```

### 3.3 Gossip 协议

Gossip 是 Redis Cluster 节点间通信的核心协议，用于传播节点状态和槽位信息。

```text
每个节点定期与其他节点通信：
- PING：检测其他节点存活
- PONG：响应 PING
- MEET：加入新节点
- FAIL：广播节点故障

Gossip 消息包含：
- 发送节点自身状态
- 随机选取的其他节点状态
- 疑似故障节点信息
```

| Gossip 消息类型 | 触发条件 | 频率 |
|----------------|---------|------|
| PING | 周期性 | 默认每秒 10 次 |
| PONG | 收到 PING/MEET | 即时响应 |
| MEET | 新节点加入 | 手动触发 |
| FAIL | 节点故障确认 | 故障时触发 |

**Gossip 收敛速度：**
```text
集群 N 个节点，消息传播收敛时间 ≈ O(log(N))
100 个节点：约 3-5 轮 Gossip 即可全集群感知
1000 个节点：约 7-10 轮 Gossip
```

### 3.4 节点故障检测

```text
节点 A 无响应 → A 标记为 PFAIL（疑似故障）
    ↓
节点 A 被随机 Gossip 传播
    ↓
半数以上节点标记 PFAIL → 标记为 FAIL（确认故障）
    ↓
FAIL 消息广播给整个集群
```

### 3.5 配置

```bash
# redis.conf
cluster-enabled yes
cluster-config-file nodes-6379.conf
cluster-node-timeout 15000
cluster-require-full-coverage yes  # 全部槽覆盖才可用
cluster-migration-barrier 1        # 从节点迁移主节点条件
```

```bash
# 创建集群命令
redis-cli --cluster create \
    192.168.1.1:6379 192.168.1.2:6379 192.168.1.3:6379 \
    192.168.1.4:6379 192.168.1.5:6379 192.168.1.6:6379 \
    --cluster-replicas 1
```

### 3.6 集群限制

| 限制 | 说明 |
|------|------|
| 不支持多 key 操作 | 除非所有 key 在同一 slot（用 hash tag） |
| 不支持多数据库 | 只有 db 0 |
| 事务受限 | 事务内所有 key 必须在同一 slot |
| Lua 脚本受限 | KEYS 数组内的 key 必须在同一 slot |
| 渐进式迁移 | 槽迁移期间性能下降 |

**Hash Tag 解决多 key 同槽：**
```java
// 使用 {} 包裹的 key 会计算 {} 内的哈希值
// 以下两个 key 一定在同一 slot
redisTemplate.opsForValue().set("user:{1001}:name", "张三");
redisTemplate.opsForValue().set("user:{1001}:email", "zhang@example.com");
```

### 3.7 在线扩缩容

```text
扩容步骤：
1. cluster meet 将新节点加入集群
2. 迁移 slot：源节点标记 slot 为 migrating
3. 目标节点标记 slot 为 importing
4. 对每个 key：MIGRATE 命令迁移
5. 更新槽位映射

缩容步骤：
1. 迁移要删除节点上的所有 slot
2. cluster forget 通知集群移除
3. 新节点下线
```

```bash
# 在线迁移 slot
redis-cli --cluster reshard <host>:<port>
# 会交互式提示：迁移多少个 slot → 目标节点 ID → 源节点 ID
```

### 3.8 数据倾斜应对

> 数据倾斜指集群中部分节点负载远高于其他节点，常见于哈希碰撞、热 key、大 key。

| 原因 | 解决方案 |
|------|----------|
| 热 key | 拆分子 key + hash tag 分散 |
| 大 key | 拆分大 key |
| 槽分配不均 | 重新分配 slot |
| 节点规格差异 | 权重分配 slot 数 |

---

## 4. 一致性哈希

### 4.1 原理

```text
哈希环 [0, 2^32-1]：
         ┌───────┐
     ┌───┤  NodeA ├───┐
     │   └───────┘   │
     │               │
  ┌──▼──┐         ┌──▼──┐
  │NodeC│         │NodeB│
  └─────┘         └─────┘
     │               │
     └───  key ──────┘
         顺时针查找
```

key 的哈希值顺时针找到第一个节点。

**增加节点的影响：**
```text
NodeD 加入 NodeA 和 NodeB 之间：
受影响 key：原来属于 NodeB 的 (NodeA-NodeD) 区间的 key
          需要重新映射到 NodeD
其他 key：不受影响
```

> 💡 一致性哈希主要用在 Memcached 和 Codis 等 Proxy 型缓存方案中，Redis Cluster 使用固定的 16384 槽映射方案，不采用一致性哈希。

### 4.2 虚拟节点解决数据倾斜

```text
不加虚拟节点：NodeA 100 个 key，NodeB 1 个 key（分布不均）

加虚拟节点：每个物理节点映射 N 个虚拟节点
┌──────┐  ┌──────┐  ┌──────┐
│NodeA │  │NodeB │  │NodeC │
├──────┤  ├──────┤  ├──────┤
│A-V1  │  │B-V1  │  │C-V1  │
│A-V2  │  │B-V2  │  │C-V2  │
│...   │  │...   │  │...   │
└──────┘  └──────┘  └──────┘
         ↕ 均匀分布在哈希环上
```

| 方案 | 均匀性 | 增减节点影响范围 | 实现复杂度 |
|------|--------|-----------------|-----------|
| 无虚拟节点 | 差 | 小（仅相邻节点） | 低 |
| 有虚拟节点 | 好 | 小 | 中 |

### 4.3 集中式方案（Codis/Proxy 模式）

```text
客户端 → Proxy (Codis/Twemproxy) → Redis 节点
              ↕
         ZooKeeper/etcd 存储槽位映射
```

Proxy 模式对客户端透明，但增加了一跳延迟。

| 特点 | Codis (Proxy) | Redis Cluster |
|------|---------------|---------------|
| 客户端兼容性 | 全兼容（无感知） | 需 Smart 客户端 |
| 数据迁移 | 在线平滑迁移 | 在线迁移 |
| 额外组件 | ZooKeeper/etcd + Proxy | 无 |
| 延迟 | 多一次 Proxy 转发 | 直连节点 |

---

## 5. 高可用方案对比

### 5.1 综合对比

| 维度 | 主从复制 | 哨兵模式 | Redis Cluster |
|------|---------|---------|--------------|
| 自动故障转移 | 否 | 是 | 是 |
| 读写分离 | 是（手动配置） | 是 | 是（slave 读） |
| 数据分片 | 否 | 否 | 是（16384 槽） |
| 水平扩展 | 否（只能加从） | 否 | 是 |
| 客户端复杂度 | 低 | 低 | 中（需支持 Cluster） |
| 节点数 | 2+ | 3+ | 6+（3主3从） |
| 适用规模 | 中小 | 中型 | 大型 |
| QPS 上限 | 10 万+ | 10 万+ | 百万+ |

### 5.2 数据安全对比

| 维度 | 主从复制 | 哨兵模式 | Redis Cluster |
|------|---------|---------|--------------|
| 数据丢失窗口 | 异步复制有延迟丢数 | 同主从 | 同主从，可配置等待 |
| 脑裂保护 | 无 | 有 | 有 |
| 网络分区容错 | 无 | 强 | 强 |
| RPO（恢复点目标） | 秒级 | 秒级 | 秒级 |
| RTO（恢复时间目标） | 手动 | 30s-1min | 自动 |

### 5.3 选型建议

- **中小型项目（< 1000 QPS）**：单机 + AOF 持久化即可
- **中型项目（1k-10k QPS）**：主从 + Sentinel
- **大型项目（10k+ QPS / 海量数据）**：Redis Cluster

> 🎯 **选型总结：** 单机能扛就用单机，扛不住上主从+哨兵，再扛不住上 Cluster。不要为了高可用而高可用，每一层复杂度都有代价。

---

## 面试核心问题

**Q: Sentinel 故障转移过程？**
SDOWN → ODOWN（quorum 确认）→ Raft 选 Leader → 选新 Master（优先级→偏移量→runid）→ 通知。

**Q: Redis Cluster 为什么是 16384 个槽？**
16384 槽位图仅 2KB，节点间心跳包轻量；65536 槽位图 8KB，对于 <1000 节点规模的集群，16384 足够均衡。超过 1000 节点时建议使用多集群拆分。

**Q: MOVED 和 ASK 的区别？**
MOVED 表示槽已永久迁移到另一节点，客户端应更新路由表；ASK 表示槽正在迁移中（仅在目标节点返回），客户端应重试。MOVED 是永久重定向，ASK 是临时重定向。

**Q: 一致性哈希虚拟节点解决什么问题？**
解决数据倾斜问题：物理节点映射多个虚拟节点均匀分布在哈希环上，节点增减时只有相邻虚拟节点受影响，分布更均衡。

**Q: Redis Cluster 中数据如何迁移？**
源节点标记 slot 为 migrating，目标节点标记为 importing，按 key 逐个 MIGRATE 迁移。迁移期间 key 的查询先在源节点查，查不到通过 ASK 重定向到目标节点。

**Q: 主从复制延迟如何监控？**
`INFO replication` 命令查看 slave 的 lag 字段，或用 `redis-cli --replica-latency` 监控复制延迟。
