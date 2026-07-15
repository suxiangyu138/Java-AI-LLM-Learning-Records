# RabbitMQ 集群元数据信息示例（生产级）

> **定位**：集群元数据是 RabbitMQ 集群的"核心配置目录"，涵盖节点信息、用户权限、交换机/队列配置、联邦/镜像策略，是运维、扩展、故障排查的核心依据。

---

## 1. 单集群元数据（3 节点）

### 1.1 集群基础信息

```json
{
  "cluster_name": "java_prod_rabbitmq_cluster",
  "nodes": [
    {
      "node_name": "rabbit@node1",
      "type": "disc",
      "status": "running",
      "ip_address": "192.168.1.100",
      "port": { "amqp": 5672, "management": 15672, "inter_node": 25672 },
      "plugins": ["rabbitmq_delayed_message_exchange", "rabbitmq_tracing"]
    }
    // node2 (192.168.1.101) / node3 (192.168.1.102) ...
  ],
  "cluster_status": "healthy",
  "auto_heal": true,
  "heartbeat": 30
}
```

### 1.2 用户与权限

| 用户 | 角色 | vhost | 权限 |
|------|------|-------|------|
| `java_client` | management | `/java_prod` | `.*` `.*` `.*` |
| `admin` | administrator | `/` | `.*` `.*` `.*` |

### 1.3 交换机与队列

| 名称 | 类型 | vhost | 特性 |
|------|------|-------|------|
| `java_prod_exchange` | direct | `/java_prod` | 持久化 |
| `advanced_delay_exchange` | x-delayed-message | `/java_prod` | x-delayed-type: direct |
| `java_order_queue` | — | `/java_prod` | lazy 模式 + 镜像全节点 |

---

## 2. 多集群联动元数据（华东 ↔ 华北）

| 集群 | 角色 | 联邦上游 |
|------|:--:|----------|
| `huadong_cluster` | master | → `huabei_cluster` (192.168.2.100:5672) |
| `huabei_cluster` | slave | → `huadong_cluster` (192.168.1.100:5672) |

### 联邦链路状态

```json
{
  "source": "huadong_cluster",
  "destination": "huabei_cluster",
  "status": "running",
  "last_sync_time": "2026-03-22 15:30:00"
}
```

### Java 客户端关联

| 配置 | 值 |
|------|-----|
| addresses | 华东 2 节点 + 华北 2 节点 |
| routing | `region_based` |
| retry | 5 次, 间隔 1s |
| 幂等 | `rabbitmq:msg:id:` → 24h 过期 |

---

## 3. 元数据管理要点

| 原则 | 说明 |
|------|------|
| **实时同步** | 扩容/联动后确认元数据一致 |
| **定期备份** | `rabbitmqctl export_definitions` 导出 |
| **与 Java 一致** | 交换机/路由键/vhost 必须匹配 |
| **分区排查** | 关注 `cluster_status` + `federation_links` |
