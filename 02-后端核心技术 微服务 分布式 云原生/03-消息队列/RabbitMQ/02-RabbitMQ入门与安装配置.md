# RabbitMQ 入门与安装配置
> 基于 AMQP 0-9-1 协议的开源消息中间件，Erlang 开发——轻量、稳定、路由灵活，中小项目首选 MQ。

## 目录
1. [RabbitMQ 是什么](#1-rabbitmq-是什么)
2. [核心架构与组件](#2-核心架构与组件)
3. [四种交换机类型](#3-四种交换机类型)
4. [消息流转机制](#4-消息流转机制)
5. [安装与部署](#5-安装与部署)
6. [Web 界面管理](#6-web-界面管理)
7. [核心命令速查](#7-核心命令速查)
8. [MQ 选型：为什么选 RabbitMQ](#8-mq-选型为什么选-rabbitmq)

---

## 1. RabbitMQ 是什么

### 1.1 基本概念

| 项目 | 说明 |
|------|------|
| 全称 | RabbitMQ |
| 协议 | AMQP 0-9-1 |
| 开发语言 | Erlang |
| 定位 | 轻量消息中间件，支持复杂路由 |
| 单机 QPS | 万级（约 1-5 万）|
| 延迟 | us 级 |
| 首个版本 | 2007 年 |
| 当前版本 | 3.13.x（2024）|
| 许可证 | MPL 2.0 |

### 1.2 核心优势

| 优势 | 说明 |
|------|------|
| **高可靠** | 持久化 + 三层确认 + 死信队列，全链路不丢 |
| **灵活路由** | Direct/Topic/Fanout/Headers 四种交换机匹配 |
| **多语言** | Java/Go/Python/PHP/Ruby/C# 等全语言 SDK |
| **轻量稳定** | Erlang 天生高并发（Actor 模型）|
| **易管理** | Web 管理界面 + 命令行 + REST API |
| **社区活跃** | 十余年积累，文档与社区资源丰富 |
| **集群易扩展** | 原生集群 + 镜像队列 + 仲裁队列 |

### 1.3 RabbitMQ 发展历程

| 阶段 | 时间 | 特性 |
|------|------|------|
| 诞生 | 2007 | Rabbit Technologies 推出 |
| 3.0 | 2011 | 性能大幅提升，镜像队列 |
| 3.6 | 2015 | 延迟消息插件、管理增强 |
| 3.8 | 2019 | 仲裁队列（Quorum Queue）|
| 3.12 | 2023 | 流队列（Stream Queue）、性能优化 |
| 3.13 | 2024 | 进一步强化流式支持 |

### 1.4 典型应用场景

- **服务解耦**：订单→库存→物流→积分 异步联动
- **异步通信**：注册后发短信/邮件/推送
- **流量削峰**：秒杀高并发缓冲
- **延迟任务**：订单超时 30 分钟未支付自动取消
- **分布式任务分发**：批量处理、数据导出
- **应用解耦**：电商下单→支付→发货异步链路
- **日志收集**：多服务日志异步采集
- **事件驱动**：领域事件通知机制

### 1.5 不适合的场景

| 场景 | 原因 |
|------|------|
| 超高吞吐（>10 万 QPS） | 建议用 Kafka 或 RocketMQ |
| 流式处理/大数据管道 | Kafka 有原生 Stream API |
| 物联网终端 | MQTT 更轻量 |
| 分布式事务强一致性 | MQ 只提供最终一致性 |

---

## 2. 核心架构与组件

### 2.1 架构图

```text
Producer → Connection → Channel → Exchange → Binding → Queue → Consumer
                                          ↑
                                      Virtual Host（多租户隔离）
```

### 2.2 六大核心组件

| 组件 | 说明 |
|------|------|
| **Broker** | RabbitMQ 服务实例，接收/存储/转发消息 |
| **Producer** | 生产者，通过 Channel 发送消息到 Exchange |
| **Consumer** | 消费者，监听队列（推/拉两种模式）|
| **Exchange** | 交换机，消息路由器（不存储消息）|
| **Queue** | 队列，FIFO 顺序存储消息 |
| **Virtual Host** | 虚拟主机，多租户逻辑隔离 |

### 2.3 关键辅助组件

| 组件 | 说明 |
|------|------|
| **Connection** | TCP 连接，开销较大，建议连接池复用 |
| **Channel** | 轻量信道，一个 Connection 可开多个 Channel（复用 TCP）|
| **Binding** | 交换机到队列的路由规则（RoutingKey 匹配）|

### 2.4 Connection 与 Channel 的关系

```text
Application
    │
    ├── Connection 1 (TCP: 5672)
    │   ├── Channel 1
    │   ├── Channel 2
    │   └── Channel 3
    │
    └── Connection 2 (TCP: 5672)
        ├── Channel 1
        └── Channel 2
```

> 💡 **最佳实践**：应用级别创建 1-2 个 Connection，每个线程使用独立的 Channel。Channel 是线程安全的，建议用完即关。

### 2.5 队列类型

| 队列类型 | 说明 | 适用场景 |
|----------|------|----------|
| **经典队列（Classic）** | 默认队列，内存 + 磁盘存储 | 通用场景 |
| **仲裁队列（Quorum）** | Raft 协议实现，高一致性 | 高可靠性场景 |
| **流队列（Stream）** | 追加写，支持回溯消费 | 日志、事件溯源 |

---

## 3. 四种交换机类型

### 3.1 交换机类型总览

| 类型 | 匹配方式 | 场景 |
|------|----------|------|
| **Direct** | `RoutingKey` = `BindingKey` 精确匹配 | 一对一、订单回调、支付通知 |
| **Topic** ⭐ | `*` 匹配一个单词，`#` 匹配零或多个 | 日志收集、多服务分类订阅 |
| **Fanout** | 无视 RoutingKey，全队列广播 | 公告推送、缓存刷新、配置广播 |
| **Headers** | 消息头属性匹配（key-value）| 复杂路由（几乎不用）|

### 3.2 Direct Exchange

```text
RoutingKey: "order.create"
              │
    ┌─────────┴─────────┐
    │   Direct Exchange  │
    └─────────┬─────────┘
              │
     ┌────────┴────────┐
     │ BindingKey:     │ BindingKey:
     │ "order.create"  │ "order.pay"
     │                 │
   Queue A            Queue B
   (收到消息)         (不匹配)
```

```java
// 生产者
channel.basicPublish("direct.exchange", "order.create", null, msg.getBytes());

// 消费者绑定
channel.queueBind("order.queue", "direct.exchange", "order.create");
```

### 3.3 Topic Exchange ⭐

```text
路由键: "log.error"
              │
    ┌─────────┴─────────┐
    │   Topic Exchange   │
    └─────────┬─────────┘
              │
     ┌────────┴────────┐
     │ BindingKey:     │ BindingKey:
     │ "log.#"         │ "log.*"
     │                 │
   Queue A            Queue B
   (匹配: log.error   (匹配: log.error
    log.info.order)   不匹配: log.info.order)
```

**匹配规则示例：**

| Binding Key | Routing Key | 匹配 |
|:-----------:|:-----------:|:----:|
| `log.#` | `log.error` | ✅ |
| `log.#` | `log.info.order` | ✅（# 匹配多级）|
| `log.*` | `log.error` | ✅ |
| `log.*` | `log.info.order` | ❌（* 只匹配一级）|
| `#` | 任意 | ✅（匹配所有）|
| `*.error` | `order.error` | ✅ |
| `*.error` | `log.error` | ✅ |
| `*.error` | `log.info.error` | ❌ |

### 3.4 Fanout Exchange

```text
RoutingKey: 任意（忽略）
              │
    ┌─────────┴─────────┐
    │   Fanout Exchange  │
    └─────────┬─────────┘
              │
     ┌────────┼────────┐
     │        │        │
   Queue A  Queue B  Queue C
   (收到)   (收到)   (收到)
```

```java
// 所有绑定队列都收到消息，无视 RoutingKey
channel.basicPublish("fanout.exchange", "", null, msg.getBytes());
```

### 3.5 Headers Exchange

```java
Map<String, Object> headers = new HashMap<>();
headers.put("x-match", "all");    // all: 全部匹配，any: 任一匹配
headers.put("format", "json");
headers.put("type", "order");

// 发送
AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
    .headers(headers).build();
channel.basicPublish("headers.exchange", "", props, msg.getBytes());
```

> ⚠️ Headers Exchange 性能较差，实践中基本被 Topic 替代，了解即可。

---

## 4. 消息流转机制

### 4.1 生产者发送流程

```text
建立 Connection
    ↓
创建 Channel
    ↓
声明 Exchange / Queue / Binding（或已存在）
    ↓
封装 Message（设置属性：持久化、contentType、messageId）
    ↓
指定 Exchange + RoutingKey → 发送
    ↓
交换机匹配 Binding
    ├── 匹配成功 → 路由到队列 → 存储
    └── 无匹配队列 → 丢弃（或 Publisher Return 回调）
```

### 4.2 消费者接收流程

```text
建立 Connection
    ↓
创建 Channel
    ↓
监听队列（basicConsume）
    ↓
Broker 推送消息（Push 模式）
    或 Consumer 拉取（Pull 模式）
    ↓
业务处理（反序列化 + 业务逻辑）
    ↓
成功 → basicAck（删除消息）
失败 → basicNack（重新投递或死信）
```

### 4.3 三层确认机制

| 机制 | 保障范围 | 说明 |
|------|----------|------|
| **Publisher Confirm** | 生产者→Broker | 确保消息到达交换机 |
| **Publisher Return** | 交换机→队列 | 无匹配队列时退回给生产者 |
| **Consumer ACK** | 消费者→Broker | 处理完才签收，保障消费安全 |

### 4.4 消息生命周期

```text
Producer               Exchange              Queue              Consumer
   │                      │                    │                   │
   ├── 发送 ────────────→ │                    │                   │
   │                      ├── 路由 ──────────→ │                   │
   │                      │                    ├── 存储 ──────────→│
   │                      │                    │                   ├── 消费
   │                      │                    │                   ├── ACK
   │                      │                    ├── 删除 ─────────┘ │
   │                      │                    │                   │
   │  ←── Confirm ──────│                    │                   │
   │  ←── Return (可选)│                    │                   │
```

### 4.5 Push vs Pull 模式

| 模式 | 说明 | 使用方式 | 适用 |
|------|------|----------|------|
| **Push（推）** | Broker 主动推送 | `basicConsume` | 实时性高，多数场景 |
| **Pull（拉）** | Consumer 主动拉取 | `basicGet` | 批量处理、手动控制 |

> 💡 **推荐 Push 模式**：大多数业务场景使用 Push。Pull 模式适合消费者需要自主控制节奏的场景。

---

## 5. 安装与部署

### 5.1 Docker 部署（最推荐）

```bash
# 单机运行
docker run -d \
  --name rabbitmq \
  -p 5672:5672 -p 15672:15672 \
  -e RABBITMQ_DEFAULT_USER=admin \
  -e RABBITMQ_DEFAULT_PASS=123456 \
  --restart always \
  rabbitmq:3-management

# 查看日志
docker logs -f rabbitmq

# 持久化数据
docker run -d \
  --name rabbitmq \
  -p 5672:5672 -p 15672:15672 \
  -v rabbitmq_data:/var/lib/rabbitmq \
  -e RABBITMQ_DEFAULT_USER=admin \
  -e RABBITMQ_DEFAULT_PASS=123456 \
  --restart always \
  rabbitmq:3-management
```

**端口说明：**

| 端口 | 用途 |
|:----:|------|
| 5672 | AMQP 协议连接端口（程序使用）|
| 15672 | Web 管理控制台 |
| 25672 | 集群节点间通信（Erlang 分发）|
| 61613 | STOMP 协议（可选）|
| 1883 | MQTT 协议（可选）|

### 5.2 Linux 直接安装

```bash
# Ubuntu / Debian
sudo apt install rabbitmq-server -y
sudo systemctl enable rabbitmq-server
sudo systemctl start rabbitmq-server

# 启用管理界面
sudo rabbitmq-plugins enable rabbitmq_management

# CentOS / RHEL / Rocky
sudo yum install epel-release -y
sudo yum install rabbitmq-server -y
sudo systemctl start rabbitmq-server
sudo systemctl enable rabbitmq-server

# 防火墙放行
sudo firewall-cmd --add-port=5672/tcp --permanent
sudo firewall-cmd --add-port=15672/tcp --permanent
sudo firewall-cmd --reload
```

### 5.3 Windows 安装

```text
1. 安装 Erlang
   - 下载: https://erlang.org/download/otp_versions_tree.html
   - 安装 stable 版本 (Erlang 26+)
   
2. 安装 RabbitMQ Server
   - 下载: https://github.com/rabbitmq/rabbitmq-server/releases
   - 运行安装程序 ( .exe )

3. 以管理员身份运行：
   net start RabbitMQ
   
4. 启用管理插件：
   cd C:\Program Files\RabbitMQ Server\rabbitmq_server-3.13.x\sbin
   rabbitmq-plugins enable rabbitmq_management

5. 验证：
   http://localhost:15672   (默认 admin / admin)
```

### 5.4 验证安装

```bash
# 查看服务状态
rabbitmqctl status

# 查看已安装插件
rabbitmq-plugins list

# 测试访问
curl http://localhost:15672  # Web 界面
curl http://localhost:5672    # AMQP 端口

# 预期输出
# Status of node rabbit@hostname ...
# Runtime: OTP 26.x, Erlang 26.x ...
# PID: ...
```

### 5.5 常见安装问题

| 问题 | 原因 | 解决 |
|------|------|------|
| 端口占用 | 5672/15672 被占用 | 修改端口或释放原端口 |
| Erlang 版本不兼容 | RabbitMQ 需要特定 Erlang | 查看版本兼容表 |
| 管理界面无法访问 | 未启用插件 | `rabbitmq-plugins enable rabbitmq_management` |
| 远程连接失败 | 默认只允许 localhost | 创建新用户或修改 `loopback_users` |
| 磁盘空间满 | 超过 `disk_free_limit` | 释放空间或调整阈值 |
| 内存报警 | 超过内存阈值 | 调整 `vm_memory_high_watermark` |

---

## 6. Web 界面管理

### 6.1 访问与默认账号

- **地址**：`http://localhost:15672`
- **默认账号**：`guest / guest`
- **限制**：仅本地访问（默认禁止远程登录）

> ⚠️ **远程访问**：生产环境请创建新用户并分配权限，不要使用 guest。

### 6.2 核心操作流程

| 步骤 | 操作 |
|:----:|------|
| 1 | **创建 Virtual Host**：Admin → Virtual Hosts → Add a virtual host |
| 2 | **创建用户**：Admin → Users → Add user → 选择角色 → Set permission |
| 3 | **创建队列**：Queues → Add a new queue → 输入名称 + durable |
| 4 | **创建交换机**：Exchanges → Add a new exchange → 选类型 |
| 5 | **绑定**：点击 Exchange → Bindings → Routing key → 选择 Queue |
| 6 | **发送消息**：Exchange → Publish message → Routing key → Payload |
| 7 | **查看消息**：Queue → Get messages → Ack Mode |
| 8 | **监控**：Queues → 查看 Ready / Unacked / Total 指标 |

### 6.3 Web 界面功能分区

| 标签页 | 功能 |
|--------|------|
| **Overview** | 概览：节点信息、连接数、队列数、消息统计 |
| **Connections** | 查看/管理 TCP 连接 |
| **Channels** | 查看/管理信道 |
| **Exchanges** | 管理交换机（创建、删除、绑定）|
| **Queues** | 管理队列（查看消息、清空、删除）|
| **Admin** | 用户、Virtual Host、权限管理 |

### 6.4 关键监控指标

| 指标 | 含义 | 异常判断 |
|------|------|----------|
| Ready | 待消费消息数 | 持续增长 → 消息积压 |
| Unacked | 已投递未确认数 | 过多 → 消费者 ACK 异常 |
| Total | 队列消息总量 | — |
| Connections | TCP 连接数 | 激增 → 连接池异常 |
| Channels | 信道数 | 过多 → 信道泄漏 |
| Disk Free | 磁盘剩余空间 | 低于 `disk_free_limit` → 触发 block |
| Memory | 内存使用 | 接近上限 → 影响性能 |

### 6.5 REST API 管理

```bash
# 获取所有队列
curl -u admin:123456 http://localhost:15672/api/queues

# 获取特定队列详情
curl -u admin:123456 http://localhost:15672/api/queues/%2F/myqueue

# 创建队列
curl -u admin:123456 -X PUT \
  -H "Content-Type: application/json" \
  -d '{"durable":true}' \
  http://localhost:15672/api/queues/%2F/newqueue

# 获取节点状态
curl -u admin:123456 http://localhost:15672/api/nodes
```

---

## 7. 核心命令速查

### 7.1 服务管理

```bash
# systemd 管理
systemctl start rabbitmq-server
systemctl stop rabbitmq-server
systemctl restart rabbitmq-server
systemctl status rabbitmq-server
systemctl enable rabbitmq-server

# 实时查看日志
journalctl -u rabbitmq-server -f

# 查看日志文件
tail -f /var/log/rabbitmq/rabbitmq.log
```

### 7.2 用户与权限

```bash
# 添加用户
rabbitmqctl add_user myuser mypass

# 设置角色
rabbitmqctl set_user_tags myuser administrator
# 角色级别: none, management, policymaker, monitoring, administrator

# 设置权限 (vhost: 配置权限 读权限 写权限)
rabbitmqctl set_permissions -p /myvhost myuser ".*" ".*" ".*"

# 查看用户
rabbitmqctl list_users

# 修改密码
rabbitmqctl change_password myuser newpass

# 删除用户
rabbitmqctl delete_user myuser

# 清除权限
rabbitmqctl clear_permissions -p /myvhost myuser
```

### 7.3 Virtual Host

```bash
# 创建 vhost
rabbitmqctl add_vhost /myvhost

# 查看
rabbitmqctl list_vhosts

# 删除
rabbitmqctl delete_vhost /myvhost
```

### 7.4 队列管理

```bash
# 查看所有队列及关键指标
rabbitmqctl list_queues name messages messages_ready messages_unacknowledged consumers

# 格式化输出
rabbitmqctl list_queues -q name messages_ready messages_unacknowledged

# 清空队列（⚠️ 生产慎用）
rabbitmqctl purge_queue myqueue

# 删除队列
rabbitmqctl delete_queue myqueue

# 查看绑定
rabbitmqctl list_bindings
```

### 7.5 交换机和绑定

```bash
# 查看所有交换机
rabbitmqctl list_exchanges

# 查看绑定关系
rabbitmqctl list_bindings

# 声明交换机（通过 rabbitmqadmin）
rabbitmqadmin declare exchange name=my.exchange type=direct
```

### 7.6 集群管理

```bash
# 查看集群状态
rabbitmqctl cluster_status

# 加入集群
rabbitmqctl stop_app
rabbitmqctl reset                    # 注意: 会清除所有数据
rabbitmqctl join_cluster rabbit@node1
rabbitmqctl start_app

# 离开集群
rabbitmqctl stop_app
rabbitmqctl reset
rabbitmqctl start_app

# 更改节点类型
rabbitmqctl change_cluster_node_type disc  # disc | ram
```

### 7.7 插件管理

```bash
# 列出所有插件
rabbitmq-plugins list

# 启用插件
rabbitmq-plugins enable rabbitmq_management
rabbitmq-plugins enable rabbitmq_delayed_message_exchange
rabbitmq-plugins enable rabbitmq_shovel
rabbitmq-plugins enable rabbitmq_federation

# 禁用插件
rabbitmq-plugins disable rabbitmq_management
```

### 7.8 监控命令

```bash
# 查看节点状态
rabbitmqctl status

# 查看环境变量
rabbitmqctl environment

# 查看评估指标
rabbitmqctl eval 'rabbit_diagnostics:maybe_stuck().'
```

---

## 8. MQ 选型：为什么选 RabbitMQ

### 8.1 选型对比总表

| 选型因素 | RabbitMQ | Kafka | RocketMQ |
|----------|:--------:|:-----:|:--------:|
| 中小项目 | ⭐⭐⭐ | ⭐ | ⭐⭐ |
| 复杂路由 | ⭐⭐⭐ | ⭐ | ⭐⭐ |
| 消息可靠性 | ⭐⭐⭐ | ⭐⭐ | ⭐⭐⭐ |
| 运维友好度 | ⭐⭐⭐ | ⭐⭐ | ⭐⭐⭐ |
| 社区生态 | ⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐ |
| 超高吞吐 | ⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐ |
| 延迟时间 | ⭐⭐⭐（us级） | ⭐⭐（ms级） | ⭐⭐（ms级）|
| 多协议支持 | ⭐⭐⭐ | ⭐ | ⭐⭐ |

### 8.2 行业最佳实践

| 业务场景 | 推荐 MQ | 理由 |
|----------|---------|------|
| 中小项目 / 对可靠性要求高 / 路由复杂 | **RabbitMQ** | 轻量、易用、功能完备 |
| 高并发电商 / 金融交易 / 事务消息 | **RocketMQ** | 高吞吐、事务消息原生支持 |
| 大数据 / 日志采集 / 流处理 | **Kafka** | 超高吞吐、流式处理 |
| 云原生 / 弹性扩缩 | **Pulsar** | 存算分离、原生多租户 |

### 8.3 选择 RabbitMQ 的核心理由

| 理由 | 说明 |
|------|------|
| **路由灵活** | 四种 Exchange 类型满足各种匹配策略 |
| **成熟可靠** | 2007 年至今，广泛应用于企业级系统 |
| **运维简易** | Web 管理界面完善，命令行工具丰富 |
| **多语言 SDK** | Java / .NET / Python / Go / PHP / JS 等 |
| **社区文档丰富** | 官方文档 + Spring 深度集成 |
| **Spring 生态** | `spring-boot-starter-amqp` 开箱即用 |

> 💡 **选型口诀**：中小项目用 Rabbit，电商金融 Rocket，大数据日志找 Kafka。

### 8.4 入门避坑总结

| 坑点 | 正确做法 |
|------|----------|
| 默认只能本地访问 | 远程需创建新用户，设置 `loopback_users = none` |
| 消息默认不持久化 | 队列 `durable=true` + 消息 `deliveryMode=2`（PERSISTENT）|
| 滥用交换机类型 | 入门先用 Direct，后续学 Topic / Fanout |
| 不装管理插件 | 必装 `rabbitmq_management`，极大方便调试 |
| 配置不匹配 | 客户端服务端配置必须一致 |
| 不设连接池 | 每个线程创建 Connection 开销极大 |
| 不配重试机制 | 消费失败直接丢弃消息 |
| 忽略死信队列 | 核心业务必须配置 DLX |

> 🎯 **RabbitMQ 入门三步走**：1) Docker 拉起服务 2) 理解四种 Exchange 3) Spring Boot starter 跑通生产消费。
