# RabbitMQ 集群与扩展指南
> 从单节点到多集群，从 RabbitMQ 到 MQTT——RabbitMQ 的集群架构、跨地域联邦、API 监控、插件扩展全解析。

## 目录
1. [集群架构](#1-集群架构)
2. [集群部署与管理](#2-集群部署与管理)
3. [跨集群协同](#3-跨集群协同)
4. [API 监控接口](#4-api-监控接口)
5. [集群元数据管理](#5-集群元数据管理)
6. [插件扩展](#6-插件扩展)

---

## 1. 集群架构

### 1.1 节点类型
RabbitMQ 集群基于 Erlang 分布式能力，节点分为两种类型：

| 类型 | 存储内容 | 恢复能力 | 推荐 |
|------|----------|----------|:----:|
| **磁盘节点 (Disc)** | 所有元数据 + 消息 | 完全恢复 | 生产必用 |
| **内存节点 (RAM)** | 仅内存 | 重启后依赖磁盘节点 | 性能优化 |

> ⚠️ 生产环境所有节点都应为磁盘节点。建议 3 个节点的奇数组合，避免脑裂。

### 1.2 集群架构图
```text
         ┌──────────────┐
         │    Load      │  负载均衡器（HAProxy/Nginx）
         │   Balancer   │
         └──────┬───────┘
        ┌───────┼───────┐
        │       │       │
   ┌────▼───┐ ┌─▼─────┐ ┌▼──────┐
   │ Node1  │ │ Node2 │ │ Node3 │
   │ (disc) │ │ (disc)│ │ (disc)│
   └────────┘ └───────┘ └───────┘
    5672/15672  5672/15672  5672/15672
```

### 1.3 集群特征
| 特性 | 说明 |
|------|------|
| 全节点互通 | 所有节点互相对等，无主从之分 |
| 元数据共享 | 交换机、队列、绑定自动同步 |
| 消息分区 | 队列只在创建它的节点上，非全局 |
| 镜像队列 | 可配置队列内容跨节点复制（高可用）|
| 客户端透明 | 连接任意节点获取完整服务 |

### 1.4 Erlang Cookie 认证
集群节点间通过 Erlang Cookie 进行身份认证，所有节点必须使用相同的 Cookie 值。

```bash
# 查看当前 cookie
cat /var/lib/rabbitmq/.erlang.cookie

# 同步 cookie 到其他节点
scp /var/lib/rabbitmq/.erlang.cookie rabbit@node2:/var/lib/rabbitmq/
scp /var/lib/rabbitmq/.erlang.cookie rabbit@node3:/var/lib/rabbitmq/

# 修改后需重启服务
systemctl restart rabbitmq-server
```

| 注意事项 | 说明 |
|----------|------|
| Cookie 权限 | 必须是 400 或 600，属主为 rabbitmq 用户 |
| Cookie 内容 | 所有节点必须完全一致，包含末尾换行符 |
| 更改时机 | 集群搭建前设置，运行中更改会导致节点失联 |
| 安全保护 | 勿将 Cookie 提交到代码仓库或日志中 |

### 1.5 节点发现方式
RabbitMQ 集群支持多种节点发现机制：

| 方式 | 配置 | 适用场景 |
|------|------|----------|
| **静态列表** | `cluster_formation.classic_config.nodes.1 = rabbit@node1` | 小规模固定集群 |
| **DNS 发现** | `cluster_formation.peer_discovery_backend = peer_discovery_dns` | 动态扩缩容 |
| **AWS 发现** | `cluster_formation.peer_discovery_backend = peer_discovery_aws` | AWS 云环境 |
| **K8s 发现** | `cluster_formation.peer_discovery_backend = peer_discovery_k8s` | Kubernetes 部署 |

### 1.6 集群端口说明
| 端口 | 用途 | 防火墙策略 |
|:----:|------|:----------:|
| 5672 | AMQP 客户端连接 | 对内网开放 |
| 15672 | Management Web 界面 | 管理网段 |
| 25672 | Erlang 节点间通信 | 集群内部互通 |
| 4369 | EPMD 端口映射 | 集群内部 |
| 61613 | STOMP 协议 | 按需开放 |
| 1883 | MQTT 协议 | 按需开放 |

---

## 2. 集群部署与管理

### 2.1 集群搭建
```bash
# 节点 1（初始化）
rabbitmq-server -detached
rabbitmqctl start_app

# 节点 2（加入集群）
rabbitmqctl stop_app
rabbitmqctl reset
rabbitmqctl join_cluster rabbit@node1
rabbitmqctl start_app

# 节点 3（加入集群）
rabbitmqctl stop_app
rabbitmqctl reset
rabbitmqctl join_cluster rabbit@node1
rabbitmqctl start_app

# 验证
rabbitmqctl cluster_status
```

### 2.2 集群状态检查
```bash
rabbitmqctl cluster_status          # 集群状态
rabbitmqctl list_nodes              # 节点列表
rabbitmqctl sync_all_queues         # 数据同步

# 详细状态输出示例
Cluster status of node rabbit@node1 ...
Basics
├── Nodes: 3
│   ├── rabbit@node1 (disc, running)
│   ├── rabbit@node2 (disc, running)
│   └── rabbit@node3 (disc, running)
├── Partitions: []  # 正常时空数组
└── Version: 3.12.x
```

### 2.3 节点管理操作
```bash
# 从集群中移除节点
rabbitmqctl forget_cluster_node rabbit@node2

# 节点改名
rabbitmqctl rename_cluster_node <old> <new>

# 强制成为独立节点
rabbitmqctl force_boot

# 清理节点（重置所有数据）
rabbitmqctl stop_app
rabbitmqctl reset
rabbitmqctl start_app
```

### 2.4 镜像队列（高可用）
```bash
# 镜像到所有节点
rabbitmqctl set_policy ha-all "^" \
  '{"ha-mode":"all","ha-sync-mode":"automatic"}' --apply-to queues

# 镜像到 N 个节点
rabbitmqctl set_policy ha-two "^" \
  '{"ha-mode":"exactly","ha-params":2,"ha-sync-mode":"automatic"}' --apply-to queues

# 查看已有策略
rabbitmqctl list_policies

# 清除策略
rabbitmqctl clear_policy ha-all
```

### 2.5 镜像队列参数详解
| 参数 | 取值 | 说明 |
|------|------|------|
| `ha-mode` | `all` / `exactly` / `nodes` | 镜像范围 |
| `ha-params` | 数字 / 节点名列表 | 配合 ha-mode 使用 |
| `ha-sync-mode` | `automatic` / `manual` | 同步方式 |
| `ha-promote-on-shutdown` | `always` / `when-synced` | 宕机时主队列提升策略 |

> 💡 `ha-mode=exactly` 配合 `ha-params=2` 实现 2 副本，兼顾可用性与性能。

### 2.6 镜像队列状态监控
```bash
# 查看队列镜像状态
rabbitmqctl list_queues name node policy synchronised_slave_pids

# 查看未同步队列
rabbitmqctl list_queues name slave_pids synchronised_slave_pids
```

### 2.7 Java 集群连接
```yaml
spring:
  rabbitmq:
    addresses: 192.168.1.100:5672,192.168.1.101:5672,192.168.1.102:5672
    connection-retry:
      enabled: true
      max-attempts: 5
```

### 2.8 负载均衡配置

**HAProxy**
```haproxy
listen rabbitmq-cluster
    bind 0.0.0.0:5672
    mode tcp
    balance roundrobin
    server node1 192.168.1.100:5672 check inter 5s rise 2 fall 3
    server node2 192.168.1.101:5672 check inter 5s rise 2 fall 3
    server node3 192.168.1.102:5672 check inter 5s rise 2 fall 3
```

**Nginx TCP 代理**
```nginx
stream {
    upstream rabbitmq_backend {
        server 192.168.1.100:5672;
        server 192.168.1.101:5672;
        server 192.168.1.102:5672;
    }
    server { listen 5672; proxy_pass rabbitmq_backend; }
}
```

**HAProxy 配置**
```haproxy
listen rabbitmq-cluster
    bind 0.0.0.0:5672
    mode tcp
    balance roundrobin
    option tcpka
    option clitcpka
    server node1 192.168.1.100:5672 check inter 5s rise 2 fall 3
    server node2 192.168.1.101:5672 check inter 5s rise 2 fall 3
    server node3 192.168.1.102:5672 check inter 5s rise 2 fall 3

# Management 界面
listen rabbitmq-management
    bind 0.0.0.0:15672
    mode tcp
    server node1 192.168.1.100:15672 check
```

**Nginx TCP 代理**
```nginx
stream {
    upstream rabbitmq_backend {
        server 192.168.1.100:5672;
        server 192.168.1.101:5672;
        server 192.168.1.102:5672;
    }

    server {
        listen 5672;
        proxy_pass rabbitmq_backend;
        proxy_timeout 60s;
    }
}
```

---

## 3. 跨集群协同

### 3.1 三大核心场景

| 场景 | 问题 | 方案 |
|------|------|------|
| **跨地域协同** | 华东应用访问华北集群延迟高 | 就近连接 + 联邦链路同步 |
| **容量扩容** | 单集群连接数/消息量达上限 | 多集群分片负载 |
| **容灾备份** | 单集群宕机全业务中断 | 主备实时镜像 + 自动切换 |

### 3.2 联邦集群（Federation）—— 推荐方案

联邦集群的核心思想是"异步拉取"：下游集群主动从上游集群拉取消息，而非上游推送。

```bash
# 开启插件
rabbitmq-plugins enable rabbitmq_federation rabbitmq_federation_management

# 下游集群配置上游
rabbitmqctl set_parameter federation-upstream upstream_cluster1 \
  '{"uri":"amqp://user:pass@上游IP:5672/vhost","exchange":"test_exchange"}'

# 配置联邦策略（自动转发匹配的交换机）
rabbitmqctl set_policy federation-policy "^fed_" \
  '{"federation-upstream-set":"all"}' --apply-to exchanges
```

| 优势 | 说明 |
|------|------|
| 对 Java 透明 | 无需改代码，仅调整连接地址 |
| 跨地域 | 适合跨地域消息同步 |
| 性能 | 异步拉取，不阻塞主集群 |

### 3.3 Federation 上游参数详解
```bash
rabbitmqctl set_parameter federation-upstream upstream_cluster1 '{
  "uri":"amqp://user:pass@remote:5672",
  "exchange":"src_exchange",
  "max-hops":1,
  "prefetch-count":1000,
  "reconnect-delay":1,
  "ack-mode":"on-confirm",
  "trust-user-id":false
}'
```

| 参数 | 默认值 | 说明 |
|------|:------:|------|
| `uri` | 必填 | 上游连接地址 |
| `exchange` | 无 | 要消费的交换机名 |
| `max-hops` | 1 | 最大转发跳数，防循环 |
| `prefetch-count` | 1000 | 批量预取数量 |
| `reconnect-delay` | 1 | 重连等待秒数 |
| `ack-mode` | `on-confirm` | 确认模式 |
| `trust-user-id` | false | 是否信任用户 ID |

### 3.4 联邦集群 vs 镜像集群

| 维度 | 联邦集群 | 镜像集群 |
|------|:--------:|:--------:|
| 适用 | 跨地域同步 | 同城容灾 |
| 性能 | 中 | 中 |
| 复杂度 | 低 | 中 |
| 数据同步方向 | 单向/双向 | 自动复制 |
| 网络要求 | 跨公网可用 | 低延迟内网 |
| 数据一致性 | 最终一致 | 强一致 |

### 3.5 多集群客户端策略
| 策略 | 做法 |
|------|------|
| **就近连接** | 客户端连接本地集群 |
| **故障切换** | `RetryTemplate` + 多地址自动切换 |
| **幂等** | 防止跨集群重复投递 |


---

## 4. API 监控接口

### 4.1 /api/nodes 接口
Management API 的核心接口，用于监控集群所有节点。

| 属性 | 说明 |
|------|------|
| 地址 | `http://{host}:15672/api/nodes` |
| 方式 | GET |
| 认证 | Basic Auth |
| 格式 | JSON 数组 |

```bash
# 查询所有节点
curl -u admin:pass http://192.168.1.100:15672/api/nodes | jq

# 查询单个节点
curl -u admin:pass http://192.168.1.100:15672/api/nodes/rabbit@node1
```

### 4.2 核心返回字段
| 字段 | 类型 | 说明 | 用途 |
|------|------|------|------|
| `name` | String | 节点名 | 元数据校验 |
| `running` | Boolean | 运行状态 | 故障监控 |
| `partitions` | Array | 分区列表，空=正常 | **网络分区核心指标** |
| `mem_alarm` | Boolean | 内存告警 | 性能优化 |
| `disk_free_alarm` | Boolean | 磁盘告警 | 存储扩展 |
| `mem_used` / `mem_limit` | Number | 已用/限制内存 | 性能监控 |
| `disk_free` / `disk_free_limit` | Number | 剩余/告警阈值 | 存储管理 |
| `fd_used` / `fd_total` | Number | 文件描述符 | 高并发优化 |
| `uptime` | Number | 运行时长 | 稳定性监控 |

### 4.3 正常 vs 分区状态
```json
// 正常
{ "name": "rabbit@node1", "running": true, "partitions": [] }

// 分区
{ "name": "rabbit@node1", "running": true, "partitions": ["rabbit@node2", "rabbit@node3"] }
```

### 4.4 其他关键 API 接口
| 接口路径 | 返回内容 | 用途 |
|----------|----------|------|
| `/api/overview` | 集群全局概览 | 总连接数/队列数/消息速率 |
| `/api/queues` | 所有队列详情 | 消息积压监控 |
| `/api/exchanges` | 交换机列表 | 路由配置校验 |
| `/api/connections` | 客户端连接 | 连接泄露排查 |
| `/api/channels` | 通道详情 | 通道状态监控 |
| `/api/consumers` | 消费者信息 | 消费健康度 |
| `/api/bindings` | 绑定关系 | 路由关系校验 |
| `/api/health/checks/alarms` | 健康检查 | 告警状态确认 |

```bash
# 队列消息积压
curl -u admin:pass http://localhost:15672/api/queues | \
  jq '.[] | {name: .name, messages: .messages_ready, consumers: .consumers}'

# 查看连接状态
curl -u admin:pass http://localhost:15672/api/connections | \
  jq '.[] | {user: .user, peer_host: .peer_host, state: .state}'
```

### 4.5 Java 监控客户端
```java
public class RabbitMonitor {

    private final RestTemplate restTemplate = new RestTemplate();

    public String checkNodes(String host, String username, String password) {
        String auth = username + ":" + password;
        String encoded = Base64.getEncoder().encodeToString(auth.getBytes());
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Basic " + encoded);
        ResponseEntity<String> resp = restTemplate.exchange(
            "http://" + host + ":15672/api/nodes",
            HttpMethod.GET, new HttpEntity<>(headers), String.class);
        return resp.getBody();
    }

    public List<Map<String, Object>> getQueuesWithBacklog(String host, String user, String pass) {
        HttpHeaders headers = createAuthHeaders(user, pass);
        ResponseEntity<List<Map<String, Object>>> resp = restTemplate.exchange(
            "http://" + host + ":15672/api/queues", HttpMethod.GET,
            new HttpEntity<>(headers),
            new ParameterizedTypeReference<List<Map<String, Object>>>() {});
        return resp.getBody().stream()
            .filter(q -> (Integer) q.get("messages_ready") > 1000)
            .collect(Collectors.toList());
    }

    private HttpHeaders createAuthHeaders(String user, String pass) {
        String auth = user + ":" + pass;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Basic " + Base64.getEncoder().encodeToString(auth.getBytes()));
        return headers;
    }
}
```

---

## 5. 集群元数据管理

### 5.1 元数据内容
| 类别 | 内容 |
|------|------|
| 节点信息 | 节点名、类型、IP、端口、插件 |
| 用户权限 | 用户名、角色、vhost、权限 |
| 交换机队列 | 名称、类型、vhost、绑定关系 |
| 策略 | 镜像策略、TTL 策略 |
| 联邦链路 | 上游/下游配置、同步状态 |
| 运行时状态 | 连接数、通道数、消息数、速率 |

### 5.2 导出与导入
```bash
# 全量导出
rabbitmqctl export_definitions /backup/backup_$(date +%Y%m%d).json

# 全量导入
rabbitmqctl stop_app
rabbitmqctl reset
rabbitmqctl import_definitions /backup/backup_20240520.json
rabbitmqctl start_app

# 每日自动备份（crontab）
0 2 * * * rabbitmqctl export_definitions /backup/backup_$(date +%Y%m%d).json

# 保留最近 30 天备份
0 3 * * * find /backup/ -name "backup_*.json" -mtime +30 -delete
```

### 5.3 备份文件示例（部分）
```json
{
  "rabbit_version": "3.12.0",
  "users": [
    {"name": "admin", "password_hash": "...", "tags": "administrator"}
  ],
  "vhosts": [
    {"name": "/"},
    {"name": "prod_vhost"}
  ],
  "permissions": [
    {"user": "admin", "vhost": "/", "configure": ".*", "write": ".*", "read": ".*"}
  ],
  "queues": [
    {"name": "order.queue", "vhost": "prod_vhost", "durable": true, "auto_delete": false}
  ],
  "exchanges": [
    {"name": "order.exchange", "vhost": "prod_vhost", "type": "topic", "durable": true}
  ],
  "bindings": [
    {"source": "order.exchange", "vhost": "prod_vhost",
     "destination": "order.queue", "routing_key": "order.#", "destination_type": "queue"}
  ],
  "policies": [
    {"vhost": "prod_vhost", "name": "ha-all",
     "pattern": "^", "definition": {"ha-mode": "all"}, "apply-to": "queues"}
  ]
}
```

### 5.4 元数据管理要点
| 原则 | 说明 |
|------|------|
| 实时同步 | 扩容/联动后确认元数据一致 |
| 定期备份 | `rabbitmqctl export_definitions` 导出 |
| 与 Java 一致 | 交换机/路由键/vhost 必须匹配 |
| 分区排查 | 关注 `cluster_status` + `federation_links` |

### 5.5 元数据不一致处理
```bash
# 排查元数据差异
# 比较两个节点导出文件
diff <(ssh node1 rabbitmqctl export_definitions -) \
     <(ssh node2 rabbitmqctl export_definitions -)

# 手动同步（备份 → 恢复备节点）
rabbitmqctl stop_app
rabbitmqctl reset
rabbitmqctl import_definitions /backup/latest.json
rabbitmqctl start_app
```

---

## 6. 插件扩展

### 6.1 四大生产必备插件
```bash
rabbitmq-plugins enable rabbitmq_delayed_message_exchange  # 延迟消息
rabbitmq-plugins enable rabbitmq_tracing                   # 消息追踪
rabbitmq-plugins enable rabbitmq_auth_backend_http         # 安全加固
rabbitmq-plugins enable rabbitmq_federation_management     # 集群同步
```

| 插件 | 场景 |
|------|------|
| `delayed_message_exchange` | 精准延迟消息（替代 TTL+DLX 方案）|
| `tracing` | 排查消息丢失/重复 |
| `auth_backend_http` | 防未授权访问 |
| `federation_management` | 跨集群数据同步 |

### 6.2 插件管理命令
```bash
# 查看已启用插件
rabbitmq-plugins list -e

# 查看所有可用插件
rabbitmq-plugins list

# 启用插件（需重启生效）
rabbitmq-plugins enable <plugin_name>

# 禁用插件
rabbitmq-plugins disable <plugin_name>

# 启用插件并立即生效（Erlang 集群环境）
rabbitmq-plugins enable <plugin_name> --online
```

### 6.3 延迟消息插件详解
```bash
rabbitmq-plugins enable rabbitmq_delayed_message_exchange
```

```java
// Java 发送延迟消息
@Autowired
private RabbitTemplate rabbitTemplate;

public void sendDelayedMessage(String orderId, long delayMillis) {
    MessageProperties props = new MessageProperties();
    props.setHeader("x-delay", delayMillis);  // 毫秒
    props.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
    Message message = MessageBuilder.withBody(orderId.getBytes())
        .andProperties(props)
        .build();

    rabbitTemplate.send("delayed.exchange", "order.delay", message);
}
```

| 对比 | 延迟消息插件 | TTL + DLX 方案 |
|------|:-----------:|:--------------:|
| 精确度 | 毫秒级 | 秒级 |
| 队列数 | 1 个 | N 个（每延迟级别一个）|
| 维护成本 | 低 | 高 |
| 支持延迟级别 | 任意毫秒 | 固定级别 |

### 6.4 消息追踪插件
```bash
rabbitmq-plugins enable rabbitmq_tracing

# 启动追踪
rabbitmqctl trace_on -p prod_vhost

# 查看追踪日志
tail -f /var/tmp/rabbitmq-tracing/trace.log
```

### 6.5 协议扩展
| 协议 | 插件 | 端口 | 场景 |
|------|------|:----:|------|
| **MQTT** | `rabbitmq_mqtt` | 1883 | IoT 设备消息采集 |
| **HTTP** | `rabbitmq_web_stomp` | 15674 | Web 应用收发消息 |
| **STOMP** | `rabbitmq_stomp` | 61613 | 多语言简单文本消息 |
| **WebSocket** | `rabbitmq_web_mqtt` | 15675 | 浏览器 MQTT |

### 6.6 MQTT 配置示例
```ini
# rabbitmq.conf
mqtt.listeners.tcp.default = 1883
mqtt.allow_anonymous = false
mqtt.default_user = mqtt_user
mqtt.default_pass = mqtt_pass
mqtt.tcp_listen_options.keepalive = 60
```

```java
// Java MQTT 客户端发布消息
MqttClient client = new MqttClient("tcp://192.168.1.100:1883", "producer001");
client.connect();
MqttMessage message = new MqttMessage("sensor_data".getBytes());
message.setQos(1);
client.publish("sensors/temperature", message);
```

### 6.7 认证扩展
```bash
rabbitmq-plugins enable rabbitmq_auth_backend_http
```

```ini
# rabbitmq.conf
auth_backends.1 = http
auth_backends.2 = internal

auth_http.http_method = post
auth_http.user_path = http://auth-service:8080/auth/user
auth_http.vhost_path = http://auth-service:8080/auth/vhost
auth_http.resource_path = http://auth-service:8080/auth/resource
```

### 6.8 存储扩展
| 方案 | 操作 | 适用 |
|------|------|------|
| 本地扩容 | 挂载新磁盘 + 迁移 mnesia | 单节点/小集群 |
| 分布式存储 | GlusterFS/Ceph 挂载到所有节点 | 大规模集群 |

### 6.9 扩展避坑
| 原则 | 说明 |
|------|------|
| 测试先行 | 所有扩展先在测试环境验证 |
| 不中断服务 | 扩容时节点逐个加入 |
| 配置一致 | 集群节点除 nodename 外必须相同 |
| 版本匹配 | 插件版本必须与 RabbitMQ 版本一致 |
| 插件兼容性 | 部分插件互不兼容，查阅官方文档确认 |

---

## 集群运维速查

### 节点宕机处理流程
```text
① 确认宕机节点 → ② Java 自动切换其他节点
→ ③ 重启宕机节点（自动加入集群）→ ④ 查日志排查原因
→ ⑤ 确认队列镜像同步完成 → ⑥ 恢复生产消费
```

### 扩容流程
```text
① 新节点安装 RabbitMQ → ② 同步 Erlang Cookie
→ ③ join_cluster 加入 → ④ 检查 cluster_status
→ ⑤ 配置镜像策略覆盖 → ⑥ 更新负载均衡器
```

### 缩容流程
```text
① 确认节点无消费者 → ② 迁移队列到其他节点
→ ③ forget_cluster_node → ④ 停止节点服务
→ ⑤ 更新负载均衡器配置
```

### 日常检查
```bash
# 每日巡检
rabbitmqctl cluster_status          # 集群健康
rabbitmqctl list_queues             # 队列状态
df -h | grep rabbitmq               # 磁盘空间
rabbitmqctl list_connections -q | wc -l  # 连接数

# 镜像队列同步状态
rabbitmqctl list_queues name synchronised_slave_pids unsynchronised_slave_pids

# 查看日志
tail -n 100 /var/log/rabbitmq/rabbitmq.log | grep -i "error\|alarm\|partition"
```

> 🎯 **核心要点**：集群保奇数节点+自动修复，元数据保备份+定期，跨地域用联邦，插件必测试再上线。
