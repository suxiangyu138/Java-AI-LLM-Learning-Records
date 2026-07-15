# RabbitMQ 高阶：网络分区（成因 + 检测 + 处理 + 预防，含 Java 适配）

> **定位**：网络分区是集群间因网络故障分裂为多个独立分区的故障。核心原则：**先恢复网络 → 再合并分区 → 最后校验数据**。

---

## 目录

1. [核心成因](#1-核心成因)
2. [检测方法](#2-检测方法)
3. [应急处理](#3-应急处理)
4. [长效预防](#4-长效预防)

---

## 1. 核心成因

| 成因 | 说明 |
|------|------|
| **网络硬件故障** | 交换机/路由器宕机、网线断裂（最常见） |
| 网络配置异常 | 防火墙拦截 5672/25672 端口 |
| 节点负载过高 | CPU/内存/磁盘耗尽，节点无响应 |
| 部署不合理 | 跨公网、不同网段，延迟 >100ms |

> 默认心跳间隔 60 秒，超时判定"失联"→ 触发分区。

---

## 2. 检测方法

### 命令行

```bash
rabbitmqctl cluster_status | grep -A 10 "partitions"
# 正常：partitions 为空
# 分区：显示各分区节点列表

ping <节点IP>
telnet <节点IP> 25672
```

### Web 界面

| 入口 | 操作 |
|------|------|
| Overview → Cluster Partitions | >0 表示有分区 |
| Admin → Nodes | 状态 "down" = 可能分区 |

### 监控告警

> Prometheus + Grafana 监控 `cluster_partitions` > 0 → 立即告警。

---

## 3. 应急处理

### Step 1：恢复网络

> 排查硬件 + 防火墙 + ping 验证。

### Step 2：合并分区

| 方案 | 操作 | 适用 |
|------|------|------|
| **自动合并** | `cluster_formation.auto_heal = true` 已开启 → 等 1-5 分钟 | 推荐 |
| 手动合并 | `stop_app → reset → join_cluster → start_app` | 自动失败时 |

### Step 3：数据校验 + Java 重连

```bash
rabbitmqctl cluster_status | grep "partitions"  # 验证合并
```

```java
// Java 客户端重连
connectionFactory.destroy();
connectionFactory.resetConnection();
```

---

## 4. 长效预防

### 配置优化

```ini
heartbeat = 30
cluster_formation.heartbeat_timeout = 60
cluster_formation.auto_heal = true
cluster_formation.connection_timeout = 10000
```

### 部署优化

| 原则 | 说明 |
|------|------|
| 节点奇数 | 3-5 个，避免脑裂 |
| 同局域网 | 避免跨公网，跨地域用专线 |
| 冗余网络 | 双交换机/路由器 |

### Java 客户端适配

| 措施 | 说明 |
|------|------|
| 重连机制 | `connection-retry: enabled: true` |
| 幂等校验 | 防止分区期间重复投递 |
| 持久化 | 全程开启，防宕机丢失 |
| 多地址 | 连接所有节点，自动切换 |
