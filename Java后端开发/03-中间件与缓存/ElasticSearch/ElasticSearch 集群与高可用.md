# ElasticSearch 集群与高可用（Java 后端企业级实战版）

> **文档定位**：Java 后端企业级技术文档 | ES 集群架构与高可用方案
> **版本**：Elasticsearch 7.x/8.x
> **核心场景**：多节点集群部署、故障转移、扩容缩容、数据安全

---

## 一、集群架构概览

```
                  Client
                     ↓
              Nginx / 负载均衡
                     ↓
       ┌─────────────┬──────────────┬─────────────┐
       ↓             ↓              ↓             ↓
    Node-1        Node-2         Node-3        Node-4
  ┌────────┐   ┌────────┐    ┌────────┐    ┌────────┐
  │P0 R1 R3│   │P1 R2 R4│    │P3 R0 R2│    │ P4 R2  │
  │ Master★│   │ Data   │    │ Data   │    │ Data   │
  └────────┘   └────────┘    └────────┘    └────────┘
  
  P = Primary Shard（主分片，写入口）
  R = Replica Shard（副本，读负载 + 故障转移）
  ★ = 集群主节点
```

---

## 二、节点角色分配（生产配置）

### 2.1 为什么要角色分离

- Master 负责集群管理（创建索引、分配分片），不存数据
- Data 负责存储和查询
- 角色混合 = 一个节点挂了可能同时丢失 Master 和数据

### 2.2 生产建议配置

```yaml
# ----- Master 节点 -----
node.master: true
node.data: false
node.ingest: false

# ----- Data 节点 -----
node.master: false
node.data: true
node.ingest: false

# ----- Ingest 节点（数据预处理） -----
node.master: false
node.data: false
node.ingest: true

# ----- Coordinating 节点（请求路由，类网关） -----
node.master: false
node.data: false
node.ingest: false
```

### 2.3 最小生产集群（5 节点推荐）

| 角色 | 数量 | 配置 | 职责 |
|---|---|---|---|
| **Master** | 3 台 | 2C 4G | 集群管理、选举 |
| **Data** | 2+ 台 | 8C 32G + SSD | 存储 + 查询 |
| **Coordinating** | 1 台 | 4C 8G | 请求路由（可选） |

---

## 三、Master 选举与脑裂问题

### 3.1 Master 选举机制

> ES 使用 **Bully 算法** 改进版进行选主，Master 节点宕机后自动选举新 Master。

选举核心参数：

```yaml
# elasticsearch.yml
discovery.seed_hosts: ["node1:9300", "node2:9300", "node3:9300"]
cluster.initial_master_nodes: ["node1", "node2", "node3"]
```

### 3.2 脑裂（Split Brain）

**场景**：网络分区导致集群分裂为两个独立集群，各自选出 Master。

```
原集群: [M1★, M2, M3, D1, D2, D3]

      网络分区
         ↓
    [M1★, D1]          [M2, M3, D2, D3]
        ↓                    ↓
    M1 仍是 Master       M2 选为新 Master★
    
    两个 Master 同时接受写入 → 数据不一致！
```

### 3.3 防止脑裂

```yaml
# 最小 Master 候选节点数（必配）
discovery.zen.minimum_master_nodes: 2   # ES 7.x 前
# (ES 7.x+ 自动计算：(N/2 + 1)，N 为 Master 候选节点数)
```

公式：`minimum_master_nodes = N / 2 + 1`（N 为 Master 候选节点数）

- 3 个 Master：至少 2 个存活才能选主
- 意思：一个 Master 节点不能独自决定成为 Master

---

## 四、分片分配与感知

### 4.1 分片分配策略

```yaml
# 机架感知：同一 Index 的主分片和副本不在同一机架
cluster.routing.allocation.awareness.attributes: rack_id
node.attr.rack_id: rack1
```

```
      Rack-1              Rack-2
   ┌──────────┐       ┌──────────┐
   │ Node1    │       │ Node3    │
   │  P0 R1   │       │  R0 P1   │
   │ Node2    │       │ Node4    │
   │  P2 R0   │       │  R2 P3   │
   └──────────┘       └──────────┘

P0 和 R0 永远不在同一机架 → 整个机架宕机不丢数据
```

### 4.2 分片数计算

```
主分片数 = 数据量预估 / 单个分片容量（建议 30-50GB）
总数据量 500GB → 500/50 = 10 个主分片（考虑增长：15-20 个）

注意：主分片创建后不可改！只能 Reindex！
```

### 4.3 副本数建议

| 环境 | 副本数 | 说明 |
|---|---|---|
| 开发 | 0 | 省资源 |
| 测试 | 0-1 | |
| 生产（单机房） | 1 | 可容忍 1 节点宕机 |
| 生产（核心业务） | 2 | 可容忍 2 节点宕机 |

---

## 五、集群状态

| 颜色 | 含义 | 影响 |
|---|---|---|
| **Green** | 所有 Primary + Replica 都已分配 | 正常 |
| **Yellow** | Primary 全部分配，部分 Replica 未分配 | 可读可写，但有丢失风险 |
| **Red** | 部分 Primary 未分配 | 部分数据不可用 |

### 5.1 常用监控命令

```json
// 集群健康
GET _cluster/health
{
  "cluster_name": "my-es",
  "status": "green",
  "number_of_nodes": 5,
  "active_shards": 48,
  "unassigned_shards": 0
}

// 分片明细
GET _cat/shards?v

// 未分配分片原因
GET _cluster/allocation/explain

// 手动移动分片
POST _cluster/reroute
{
  "commands": [
    {
      "move": {
        "index": "goods", "shard": 0,
        "from_node": "node-1", "to_node": "node-2"
      }
    }
  ]
}

// 节点统计
GET _cat/nodes?v&h=name,heap.percent,ram.percent,cpu,load_1m,disk.used_percent
```

---

## 六、扩容与缩容

### 6.1 添加新节点

```yaml
# 新节点只需：1. 同样 cluster.name  2. 同样的 discovery.seed_hosts
cluster.name: my-es-cluster
node.name: node-new
discovery.seed_hosts: ["node1", "node2", "node3"]
```

启动后 ES 自动迁移分片到新节点，无需手动干预。

### 6.2 安全下线节点

```json
// 1. 禁止向该节点分配新分片
PUT _cluster/settings
{
  "transient": {
    "cluster.routing.allocation.exclude._name": "node-to-remove"
  }
}

// 2. 等待分片迁移完成（监控 _cat/shards）

// 3. 关闭节点
// 4. 删除排除规则
PUT _cluster/settings
{
  "transient": {
    "cluster.routing.allocation": null
  }
}
```

---

## 七、快照与恢复（Snapshot）

### 7.1 注册快照仓库

```json
// elasticsearch.yml 中配置：
// path.repo: ["/mount/backups", "/mount/longterm_backups"]

PUT _snapshot/es_backup
{
  "type": "fs",
  "settings": {
    "location": "/mount/backups/es_snapshot",
    "compress": true
  }
}
```

### 7.2 创建与恢复

```json
// 创建快照
PUT _snapshot/es_backup/snapshot_20240101
{
  "indices": "goods,orders,user*",
  "ignore_unavailable": true
}
// 异步: ?wait_for_completion=false

// 查看快照
GET _snapshot/es_backup/snapshot_20240101

// 恢复
POST _snapshot/es_backup/snapshot_20240101/_restore

// SLM 自动备份策略（定时快照）
PUT _slm/policy/daily-snapshot
{
  "schedule": "0 30 2 * * ?",        // 每天凌晨 2:30
  "name": "<daily-snapshot-{now/d}>",
  "repository": "es_backup",
  "config": { "indices": ["*"] },
  "retention": {
    "expire_after": "30d",           // 保留 30 天
    "min_count": 7
  }
}
```

---

## 八、面试核心要点

1. **脑裂怎么防？** `minimum_master_nodes = N/2 + 1`
2. **Green/Yellow/Red 含义？** Green 正常，Yellow 副本未分配，Red 主分片丢失
3. **节点扩容做什么？** 加节点配置，ES 自动迁移分片
4. **主分片为什么不可改？** 路由公式 `hash % num_shards` 依赖分片数
5. **快照用什么做？** Snapshot API + 共享文件系统 / S3 / HDFS

---

## 九、极简总结

```
3 Master = 只要还活着 2 个就能提供服务（防脑裂）
N/2+1 = 最小 Master 候选数（3 个 Master 至少活 2 个）
主分片创建后不能改 = 只能 Reindex
加节点 = 自动迁移分片，无需手动
Snapshot = 定期备份，SLM 自动执行
```
