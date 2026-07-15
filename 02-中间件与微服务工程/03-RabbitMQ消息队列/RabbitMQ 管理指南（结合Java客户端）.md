# RabbitMQ 管理指南（结合 Java 客户端）

> **定位**：Web 界面 + 命令行 + 监控告警，覆盖日常运维高频操作。

---

## 目录

1. [Web 界面管理](#1-web-界面管理)
2. [命令行管理](#2-命令行管理)
3. [监控告警](#3-监控告警)
4. [注意事项](#4-注意事项)

---

## 1. Web 界面管理

> 访问 `http://IP:15672`，默认 `guest/guest`（仅本地）。

### 核心操作

| 操作 | 路径 |
|------|------|
| **创建 vhost** | Virtual Hosts → Add |
| **创建用户** | Users → Add → 选角色 → Set permission |
| **查看队列状态** | Queues → 看 Ready/Unacked/Total |
| **排查消息** | Exchange → Publish message 测试路由 |
| **查看死信** | 死信队列 → Get messages |

### 关键指标

| 指标 | 异常判断 |
|------|----------|
| Unacked 过多 | 消费者未正常 ACK |
| Ready 持续增长 | 消息积压 |
| Connections 激增 | 连接池配置异常 |

---

## 2. 命令行管理

### 服务管理

```bash
systemctl start/stop/restart rabbitmq-server
systemctl status rabbitmq-server
journalctl -u rabbitmq-server -f   # 实时日志
```

### 用户与权限

```bash
rabbitmqctl add_user java_client 123456
rabbitmqctl set_user_tags java_client administrator
rabbitmqctl set_permissions -p /java_prod java_client ".*" ".*" ".*"
rabbitmqctl list_users
```

### 队列管理

```bash
rabbitmqctl list_queues name messages_ready messages_unacknowledged
rabbitmqctl purge_queue test_queue      # ⚠️ 生产慎用
rabbitmqctl delete_queue test_queue
```

---

## 3. 监控告警

| 方式 | 工具 | 监控指标 |
|------|------|----------|
| Web | Overview 面板 | Connections/Channels/Queues/Disk |
| CLI | `rabbitmqctl status` / `list_connections` | 连接详情 |
| 外部 | Prometheus + Grafana | 消息积压/连接数/磁盘 |
| 日志 | ELK | connection refused / disk full |

---

## 4. 注意事项

| 原则 | 说明 |
|------|------|
| **账号最小权限** | 不用 guest，为每个应用创建独立账号 |
| **配置一致性** | RabbitMQ 配置与 Java 客户端一致 |
| **生产禁危命令** | 禁止 `purge_queue`/`delete_queue` |
| **定期备份** | 队列配置 + 消息数据 |
| **集群检查** | Java 客户端配置所有节点地址 |
