# RabbitMQ 扩展指南（高阶实操 + Java 适配）

> **定位**：突破 RabbitMQ 原生能力边界，覆盖插件/集群/存储/协议四大扩展方向。

---

## 目录

1. [插件扩展](#1-插件扩展)
2. [集群扩展](#2-集群扩展)
3. [存储扩展](#3-存储扩展)
4. [协议扩展](#4-协议扩展)

---

## 1. 插件扩展

### 生产必装四大插件

```bash
rabbitmq-plugins enable rabbitmq_delayed_message_exchange  # 延迟消息
rabbitmq-plugins enable rabbitmq_tracing                   # 消息追踪
rabbitmq-plugins enable rabbitmq_auth_backend_http         # 安全加固
rabbitmq-plugins enable rabbitmq_federation_management     # 集群同步优化
```

| 插件 | 场景 |
|------|------|
| `delayed_message_exchange` | 精准延迟消息 |
| `tracing` | 排查消息丢失/重复 |
| `auth_backend_http` | 防未授权访问 |
| `federation_management` | 跨集群数据同步 |

> ⚠️ 插件版本必须与 RabbitMQ 版本完全匹配，安装后需重启。

---

## 2. 集群扩展

### 单集群扩容

```bash
# 新节点加入集群
rabbitmqctl stop_app
rabbitmqctl reset
rabbitmqctl join_cluster rabbit@node1
rabbitmqctl start_app
```

```yaml
# Java 客户端添加新节点地址
spring:
  rabbitmq:
    addresses: 192.168.1.100:5672,192.168.1.101:5672,192.168.1.104:5672
```

### 多集群联动

```text
华东主集群 ←→ 华北集群（联邦链路双向同步）
     │              │
  华东应用        华北应用（就近连接）
```

---

## 3. 存储扩展

| 方案 | 操作 | 适用 |
|------|------|------|
| **本地扩容** | 挂载新磁盘 + 迁移 mnesia 数据 | 单节点/小集群 |
| **分布式存储** | GlusterFS/Ceph 挂载到所有节点 | 大规模集群 |

```bash
# 本地扩容关键步骤
systemctl stop rabbitmq-server
cp -r /var/lib/rabbitmq/mnesia/* /new_storage/
echo "MNESIA_BASE=/new_storage" >> /etc/rabbitmq/rabbitmq-env.conf
systemctl start rabbitmq-server
```

---

## 4. 协议扩展

| 协议 | 插件 | 场景 |
|------|------|------|
| **MQTT** | `rabbitmq_mqtt` | IoT 设备消息采集（端口 1883） |
| **HTTP** | `rabbitmq_web_stomp` | Web 应用直接 HTTP 收发 |

---

## 扩展避坑要点

| 原则 | 说明 |
|------|------|
| 测试先行 | 所有扩展先在测试环境验证 |
| 不中断服务 | 扩容时节点逐个加入 |
| 配置一致 | 集群节点除 nodename 外必须相同 |
| 避免过度扩展 | 不装无用插件，不部署过多节点 |
