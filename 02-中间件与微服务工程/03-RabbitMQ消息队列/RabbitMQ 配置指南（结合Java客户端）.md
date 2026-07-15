# RabbitMQ 配置指南（结合 Java 客户端）

> **定位**：聚焦 `rabbitmq.conf` 核心配置 + Java 客户端联动，覆盖开发/测试/生产三环境。

---

## 目录

1. [配置文件基础](#1-配置文件基础)
2. [核心配置项](#2-核心配置项)
3. [Java 客户端联动](#3-java-客户端联动)
4. [三环境配置](#4-三环境配置)

---

## 1. 配置文件基础

| 部署方式 | 路径 |
|----------|------|
| Docker | `/etc/rabbitmq/rabbitmq.conf`（挂载目录映射） |
| Linux | `/etc/rabbitmq/rabbitmq.conf` |
| Windows | `C:\Program Files\RabbitMQ Server\...\etc\rabbitmq.conf` |

> 格式：`key = value`，`#` 注释。修改后需重启服务。

---

## 2. 核心配置项

### 网络配置

```ini
listeners.tcp.default = 5672
management.tcp.port = 15672
loopback_users = none          # 允许远程连接
connection_timeout = 10000
```

### 账号安全

```ini
default_user = java_client
default_pass = Java@123456
default_vhost = /java_prod
```

### 持久化与性能

```ini
default_message_durability = true
queue_index_embed_msgs_below = 4096
persistent_queue_store_write_strategy = buffered
disk_free_limit.absolute = 1024MB
```

### 连接/信道

```ini
connections.max = 200          # > Java 连接池 × 1.5
channels.max = 2000
```

### 死信队列

```ini
dead_letter_exchange = dlx_exchange
dead_letter_routing_key = dlx_key
default_message_ttl = 60000
default_queue_max_length = 10000
```

---

## 3. Java 客户端联动

| RabbitMQ 配置 | Java 配置 | 不一致后果 |
|--------------|----------|-----------|
| `listeners.tcp.default = 5672` | `spring.rabbitmq.port: 5672` | 连接失败 |
| `default_user = java_client` | `spring.rabbitmq.username: java_client` | access refused |
| `default_vhost = /java_prod` | `spring.rabbitmq.virtual-host: /java_prod` | 无法访问资源 |

```yaml
spring:
  rabbitmq:
    host: 192.168.1.100
    port: 5672
    username: java_client
    password: Java@123456
    virtual-host: /java_prod
    template:
      delivery-mode: persistent  # 与 default_message_durability 呼应
    listener:
      simple:
        ack-mode: manual
        prefetch: 5
```

---

## 4. 三环境配置

| 配置项 | 开发 | 测试 | 生产 |
|--------|:--:|:---:|:---:|
| `connections.max` | 50 | 100 | 300 |
| 死信队列 | ❌ | ✅ | ✅ |
| 密码存储 | 明文 | 明文 | **加密哈希** |
| 日志级别 | debug | debug | info |
| `disk_free_limit` | 无 | 无 | 1024MB |
| 集群 | ❌ | ❌ | ✅ |

---

## 避坑要点

| 坑点 | 正确做法 |
|------|----------|
| 配置不一致 | RabbitMQ + Java 客户端端口/账号/vhost 必须一致 |
| 明文密码 | 生产用 `rabbitmqctl hash_password` 加密 |
| 修改不重启 | 改配置后必须重启服务 |
| 集群不一致 | 所有节点除 `nodename` 外配置必须相同 |
