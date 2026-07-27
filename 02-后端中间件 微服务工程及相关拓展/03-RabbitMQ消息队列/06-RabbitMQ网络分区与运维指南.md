# RabbitMQ 网络分区与运维指南
> 网络分区是集群最危险的状态——从成因检测到应急处理再到长效预防，全流程覆盖。日常运维四项指标保障集群稳定运行。

## 目录
1. [网络分区全流程](#1-网络分区全流程)
2. [成因分析](#2-成因分析)
3. [分区模式策略](#3-分区模式策略)
4. [检测方法](#4-检测方法)
5. [应急处理](#5-应急处理)
6. [合并分区](#6-合并分区)
7. [长效预防](#7-长效预防)
8. [日常运维](#8-日常运维)
9. [故障排查手册](#9-故障排查手册)

---

## 1. 网络分区全流程

```text
成因(A) → 发生分区(B) → 检测(C) → 应急处理(D/E) → 恢复(F) → 长效预防(G)
```

| 阶段 | 操作 |
|:----:|------|
| A. 成因 | 网络硬件故障 / 配置异常 / 节点过载 / 部署不合理 |
| B. 分区发生 | 集群分裂为多个独立分区 |
| C. 检测 | /api/nodes / rabbitmqctl cluster_status / Web 界面 |
| D. 紧急止损 | 通知团队 → 暂停收发 → 备份数据 |
| E. 恢复网络 | 排查硬件 → 防火墙 → ping 验证 |
| F. 合并分区 | 自动合并 / 手动合并 → 数据校验 |
| G. 长效预防 | 奇数节点 / 心跳调优 / 同局域网 / 冗余网络 |

### 1.1 分区类型
| 类型 | 描述 | 典型场景 |
|:----:|------|----------|
| **对称分区** | 集群分裂为两个互不连通的子集，各子集内节点互通 | 交换机故障导致网络断裂 |
| **不对称分区** | 部分节点可以连通某些节点但无法连通另一些 | 防火墙规则局部异常 |
| **节点隔离** | 单个节点与集群其他所有节点失联 | 节点负载过高、硬件故障 |

### 1.2 分区对业务的影响
| 影响层面 | 具体表现 |
|----------|----------|
| 消息可用性 | 各分区独立运行，同一队列在不同分区产生不同副本 |
| 数据一致性 | 分区合并时可能出现消息丢失或重复 |
| 客户端连接 | 连接到不同分区的客户端相互不可见 |
| 管理操作 | 无法统一管理所有节点 |

---

## 2. 成因分析

| 成因 | 说明 | 比重 |
|------|------|:----:|
| **网络硬件故障** | 交换机/路由器宕机、网线断裂 | 最高频 |
| 网络配置异常 | 防火墙拦截 5672/25672 端口、IP 变更 | 常见 |
| 节点负载过高 | CPU/内存/磁盘耗尽，节点无响应 | 常见 |
| 部署不合理 | 跨公网、不同网段，延迟 >100ms | 架构问题 |

> 默认心跳间隔 60 秒，超时后判定节点"失联"，触发分区。

### 2.1 网络硬件故障排查
```bash
# 检查交换机状态
ping -c 10 <节点IP>

# 检查是否有丢包
mtr -r <节点IP>

# 检查端口连通性
telnet <节点IP> 25672

# 查看系统日志
journalctl -u rabbitmq-server -n 100 --no-pager | grep -i "partition\|net_tick"
```

### 2.2 节点过载征兆
| 指标 | 临界值 | 后果 |
|------|:------:|------|
| CPU 使用率 | > 80% | 节点无响应 |
| 内存使用率 | > memory_limit 的 90% | 触发内存告警，停止接收消息 |
| 磁盘剩余空间 | < disk_free_limit | 触发磁盘告警 |
| 文件描述符使用率 | > fd_total 的 80% | 无法接受新连接 |

---

## 3. 分区模式策略

RabbitMQ 提供三种分区处理策略，通过 `cluster_partition_handling` 配置。

### 3.1 三种策略对比
| 策略 | 配置值 | 行为 | 推荐场景 |
|:----:|:------:|------|:--------:|
| **pause-minority** | `pause_minority` | 暂停少数派分区节点 | 奇数节点集群 |
| **autoheal** | `autoheal` | 自动合并，多数派获胜 | 偶数节点集群 |
| **ignore** | `ignore` | 不自动处理，保持分区 | 需要人工干预的场景 |

### 3.2 pause-minority 策略（推荐）
```ini
# rabbitmq.conf
cluster_partition_handling = pause_minority
```

- 检测到分区后，节点按数量分为"多数派"与"少数派"
- 少数派节点自动暂停所有服务（停止接收连接和消息）
- 网络恢复后，少数派节点自动重新加入
- 优点：数据一致性最高，不会产生分裂
- 缺点：少数派节点完全不可用

### 3.3 autoheal 策略
```ini
# rabbitmq.conf
cluster_partition_handling = autoheal
```

- 分区发生后，多数派节点获胜，其余节点重启
- 重启的节点以磁盘节点身份重新加入
- 优点：自动恢复，无需人工干预
- 缺点：重启节点上的消息会丢失（内存节点）

### 3.4 ignore 策略
```ini
# rabbitmq.conf
cluster_partition_handling = ignore
```

- 不执行任何自动处理，各分区独立运行
- 需要人工通过 `rabbitmqctl stop_app` / `join_cluster` 恢复
- 适用场景：需要评估数据后再合并的生产环境

> ⚠️ ignore 策略风险最高，分区期间各分区数据会产生分歧，手动合并时可能丢失消息。

---

## 4. 检测方法

### 方式 1：命令行检测
```bash
rabbitmqctl cluster_status | grep -A 10 "partitions"
# 正常：partitions 为空
# 分区：显示各分区节点列表

# 查看具体分区详情
rabbitmqctl cluster_status | jq '.partitions'

# 网络连通测试
ping <节点IP>
telnet <节点IP> 25672
```

### 方式 2：Web 界面
| 入口 | 正常 | 异常 |
|------|:----:|:----:|
| Overview → Cluster Partitions | 0 | >0 表示分区 |
| Admin → Nodes | running | down |

### 方式 3：API 监控
```bash
curl -u admin:pass http://192.168.1.100:15672/api/nodes | jq '.[].partitions'
# [] = 正常
# ["rabbit@node2"] = 分区

# 批量检测所有节点
for node in node1 node2 node3; do
  echo "=== $node ==="
  curl -s -u admin:pass http://$node:15672/api/nodes/$node | jq '{name, partitions}'
done
```

### 方式 4：监控告警
> Prometheus + Grafana 监控 `cluster_partitions > 0` → 立即告警。

```yaml
# prometheus 告警规则
groups:
- name: rabbitmq_alerts
  rules:
  - alert: RabbitMQPartition
    expr: rabbitmq_cluster_partitions > 0
    for: 30s
    labels:
      severity: critical
    annotations:
      summary: "RabbitMQ 集群发生网络分区"
```

### 方式 5：日志检测
```bash
# 检查日志中的分区记录
grep "network partition" /var/log/rabbitmq/rabbitmq.log

# 实时监控
tail -f /var/log/rabbitmq/rabbitmq.log | grep -i "partition\|down\|error"

# 日志输出示例
2024-05-20 14:30:01 [error] <0.123.0> Mnesia: ** ERROR **
  Detected a network partition between nodes rabbit@node1 and rabbit@node2;
  waiting for the partition to clear ...
```

---

## 5. 应急处理

### Step 1：紧急止损
```text
D1 通知 Java 开发 + 运维团队
D2 暂停消息收发（可选）
D3 备份各分区数据
```

### Step 2：恢复网络
```text
E1 排查硬件（交换机/网线/网卡）
E2 检查防火墙规则（放行 5672/25672）
E3 ping + telnet 验证节点互通
E4 确认所有节点网络连通
```

### 分区状态示例
```text
正常：rabbit@node1 ←→ rabbit@node2 ←→ rabbit@node3
分区：rabbit@node1 ✗ ↔ [rabbit@node2, rabbit@node3]
```

### 分区期间数据保护
```bash
# 1. 暂停消费者（防止重复消费）
rabbitmqctl set_policy pause-consumers "^" '{"consumer-priority": -1}' --apply-to queues

# 2. 备份各分区元数据
rabbitmqctl export_definitions /backup/partition_$(hostname)_$(date +%Y%m%d%H%M).json

# 3. 记录各分区队列状态
rabbitmqctl list_queues name messages consumers > /tmp/queues_$(hostname).txt
```

---

## 6. 合并分区

### 方式 1：自动合并（推荐）
```ini
cluster_formation.auto_heal = true
```
已开启 → 等待 1-5 分钟。自动合并时，**保留最多节点所在分区**，其他分区重启。

### 方式 2：手动合并（自动失败时）
```bash
# 1. 确定多数派节点（假设 node1、node2 是多数派）
# 2. 在"少数派"节点上执行
rabbitmqctl stop_app
rabbitmqctl reset
rabbitmqctl join_cluster rabbit@node1  # 加入"多数派"节点
rabbitmqctl start_app
```

### 方式 3：强制重置并加入
```bash
# 当少数派节点无法正常 stop_app 时
rabbitmqctl force_boot

# 然后执行
rabbitmqctl stop_app
rabbitmqctl reset
rabbitmqctl join_cluster rabbit@majority_node
rabbitmqctl start_app
```

### 方式 4：数据校验
```bash
# 验证合并
rabbitmqctl cluster_status | grep "partitions"

# 确认队列消息数、交换机绑定、元数据一致性
rabbitmqctl list_queues name messages consumers
rabbitmqctl list_exchanges name type
rabbitmqctl list_bindings

# 检查是否有未同步的镜像队列
rabbitmqctl list_queues name synchronised_slave_pids unsynchronised_slave_pids
```

### Java 客户端重连
```yaml
spring:
  rabbitmq:
    addresses: 192.168.1.100:5672,192.168.1.101:5672,192.168.1.102:5672
    connection-retry:
      enabled: true
      max-attempts: 5
```

```java
@Bean
public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
    RabbitTemplate template = new RabbitTemplate(connectionFactory);
    template.setRetryTemplate(new RetryTemplate() {{
        setRetryOperationsMap(Map.of(
            RetryTemplate.DEFAULT_KEY, new SimpleRetryPolicy(5)
        ));
        setBackOffPolicy(new ExponentialBackOffPolicy() {{
            setInitialInterval(1000);
            setMultiplier(2);
            setMaxInterval(10000);
        }});
    }});
    return template;
}
```

---

## 7. 长效预防

### 7.1 配置调优
```ini
# rabbitmq.conf
heartbeat = 30                          # 默认 60 → 30 秒，加速故障检测
cluster_formation.heartbeat_timeout = 60
cluster_formation.auto_heal = true      # 自动合并
cluster_formation.connection_timeout = 10000

# 网络调优
tcp_listen_options.backlog = 4096
tcp_listen_options.keepalive = true
tcp_listen_options.nodelay = true

# 内存和磁盘阈值
vm_memory_high_watermark.relative = 0.7    # 内存阈值 70%
disk_free_limit.absolute = 2GB             # 磁盘告警 2GB
```

### 7.2 部署原则
| 原则 | 说明 |
|------|------|
| **奇数节点** | 3-5 个，避免脑裂 |
| **同局域网** | 避免跨公网，跨地域用专线 |
| **冗余网络** | 双交换机/双路由器 |
| **监控告警** | 分区 >0 立即告警 |
| **定期演练** | 每月 1 次容灾切换测试 |

### 7.3 Java 客户端适配
| 措施 | 说明 |
|------|------|
| 重连机制 | `connection-retry: enabled: true` |
| 幂等校验 | 防止分区期间重复投递 |
| 持久化 | 全程开启，防宕机丢失 |
| 多地址 | 连接所有节点，自动切换 |

### 7.4 冗余网络架构
```text
          ┌───────────────┐
          │    Switch 1   │
       ┌──┤  (主交换机)   ├──┐
       │  └───────────────┘  │
  ┌────▼────┐          ┌────▼────┐
  │  Node1   │          │  Node2   │
  │(双网卡)  │          │(双网卡)  │
  └────┬────┘          └────┬────┘
       │  ┌───────────────┐  │
       └──┤    Switch 2   ├──┘
          │  (备交换机)   │
          └───────────────┘
```

### 7.5 定期演练清单
| 项目 | 周期 | 步骤 |
|------|:----:|------|
| 单节点宕机 | 月度 | 停止一个节点，确认业务切换 |
| 网络中断 | 季度 | 断开一个节点网线，观察自动恢复 |
| 分区合并 | 季度 | 模拟分区后执行手动/自动合并 |
| 全量备份恢复 | 半年 | 从备份完整恢复一个集群 |

---

## 8. 日常运维

### 8.1 每日巡检（10 分钟）
```bash
systemctl status rabbitmq-server                  # 服务状态
rabbitmqctl list_queues name messages_ready messages_unacknowledged  # 队列状态
rabbitmqctl list_connections -q | wc -l           # 连接数
df -h | grep /var/lib/rabbitmq                    # 磁盘空间
tail -f /var/log/rabbitmq/rabbitmq.log           # 日志检查
```

### 8.2 四项核心指标
| 指标 | 异常判断 | 关联 Java |
|------|----------|-----------|
| **连接数** | 接近 `connections.max` | 连接池需扩容 |
| **Ready 消息** | 持续增长 | 消费者异常 |
| **Unacked 消息** | 过多 | 手动确认异常 |
| **磁盘空间** | 低于 `disk_free_limit` | 停止接收消息 |

### 8.3 每周维护
```bash
# 清理旧日志
find /var/log/rabbitmq -name "*.log.*" -mtime +7 -delete

# 清理死信（确认后）
rabbitmqctl purge_queue dlx_queue

# 检查集群状态一致性
rabbitmqctl cluster_status
rabbitmqctl list_policies
rabbitmqctl list_parameters

# 检查磁盘使用排名（按队列）
du -sh /var/lib/rabbitmq/mnesia/*/msg_storage/*/ | sort -rh | head -10
```

### 8.4 备份与恢复
```bash
# 全量备份（配置 + 消息）
rabbitmqctl export_definitions /backup/backup_$(date +%Y%m%d).json

# 异地备份（SCP 到备份服务器）
scp /backup/backup_*.json backup-server:/backup/rabbitmq/

# 恢复
systemctl stop rabbitmq-server
rabbitmqctl import_definitions /backup/backup_20240520.json
systemctl start rabbitmq-server
rabbitmqctl list_queues && rabbitmqctl list_exchanges  # 验证
```

### 8.5 性能监控 Dashboard（Prometheus + Grafana）
```yaml
# prometheus 关键指标
rabbitmq_queue_messages_ready_total   # 待消费消息数
rabbitmq_queue_messages_unacked_total # 未确认消息数
rabbitmq_connections_total            # 总连接数
rabbitmq_channels_total               # 总通道数
rabbitmq_consumers_total              # 总消费者数
rabbitmq_process_resident_memory_bytes # 进程内存
rabbitmq_disk_space_available_bytes   # 可用磁盘
```

---

## 9. 故障排查手册

### 故障 1：Java 连接失败

| 步骤 | 排查 |
|:----:|------|
| 1 | `systemctl status rabbitmq-server` 确认服务启动 |
| 2 | `ping` + `telnet IP 5672` 确认网络 |
| 3 | 账号密码 + vhost 权限一致 |
| 4 | `loopback_users = none` 允许远程 |
| 5 | 连接数是否达 `connections.max` |

```java
// 连接失败排查辅助代码
@Slf4j
@Component
public class ConnectionTester {

    public void testConnection(String host, int port, String user, String pass) {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(host);
            factory.setPort(port);
            factory.setUsername(user);
            factory.setPassword(pass);
            factory.setConnectionTimeout(5000);
            Connection conn = factory.newConnection();
            log.info("连接成功: {}:{}", host, port);
            conn.close();
        } catch (Exception e) {
            log.error("连接失败: {}:{}, 原因: {}", host, port, e.getMessage());
        }
    }
}
```

### 故障 2：消息积压

| 步骤 | 排查 |
|:----:|------|
| 1 | Java 消费者是否正常运行 |
| 2 | Unacked 过多 → 手动确认异常 |
| 3 | 消费逻辑是否有耗时操作 |
| 4 | 预取数 `prefetch` 是否过小 |

**解决**：增加消费者节点 + 优化消费逻辑 + 调整 `prefetch: 8`

```java
// 调整 prefetch（手动确认）
@Bean
public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
        ConnectionFactory connectionFactory) {
    SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
    factory.setConnectionFactory(connectionFactory);
    factory.setPrefetchCount(8);        // 预取数
    factory.setConcurrentConsumers(3);  // 最小消费者
    factory.setMaxConcurrentConsumers(10); // 最大消费者
    factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
    return factory;
}
```

### 故障 3：消息丢失

| 步骤 | 排查 |
|:----:|------|
| 1 | 三持久化（Exchange + Queue + Message）|
| 2 | 生产者发送是否有异常 |
| 3 | 路由键是否匹配 |
| 4 | 手动确认代码是否执行 |
| 5 | 磁盘空间是否充足 |

**消费者确认代码**：
```java
@RabbitListener(queues = "order.queue")
public void handleMessage(Order order, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag) {
    try {
        orderService.process(order);
        channel.basicAck(tag, false);  // 手动 ACK
    } catch (Exception e) {
        channel.basicNack(tag, false, true);  // 重试
    }
}
```

### 故障 4：磁盘/内存告警
```bash
rabbitmqctl list_queues | wc -l        # 队列数
df -h /var/lib/rabbitmq                 # 磁盘
rabbitmqctl status | grep memory        # 内存

# 临时方案：提高告警阈值
rabbitmqctl set_vm_memory_high_watermark 0.8

# 根本方案：扩容磁盘或清理队列
rabbitmqctl purge_queue <队列名>  # 清理（确认无业务影响）
```

### 故障 5：节点无法加入集群
| 原因 | 排查 | 解决 |
|------|------|------|
| Erlang Cookie 不一致 | 比较各节点 Cookie | 统一复制 |
| 端口不通 | telnet 25672 | 放行防火墙 |
| 已存在集群数据 | 检查 mnesia | `rabbitmqctl reset` 清理 |
| 版本不一致 | `rabbitmqctl status` 对比版本 | 升级到同一版本 |
| 节点名冲突 | `rabbitmqctl cluster_status` | 使用不同节点名 |

### 故障 6：镜像队列未同步
```bash
# 查看同步状态
rabbitmqctl list_queues name slave_pids synchronised_slave_pids

# 手动触发同步
rabbitmqctl sync_queue <queue_name>

# 排查原因
rabbitmqctl list_queue <queue_name> arguments
```

---

## 运维最佳实践

| 原则 | 做法 |
|------|------|
| **环境隔离** | 开发/测试/生产完全独立 |
| **自动化** | crontab 定时备份/清理/巡检 |
| **监控告警** | Prometheus + Grafana，积压 >1000 告警 |
| **灰度发布** | 先测试 → 再小范围 → 再全量 |
| **文档留存** | 配置/节点/操作日志/故障处理记录 |

### 运维脚本示例
```bash
#!/bin/bash
# rabbitmq_daily_check.sh — 每日巡检脚本

echo "=== RabbitMQ 巡检报告: $(date) ==="

# 1. 服务状态
systemctl is-active rabbitmq-server || echo "FAIL: 服务未运行"

# 2. 集群状态
rabbitmqctl cluster_status | grep -E "partitions|running_nodes"

# 3. 队列积压
rabbitmqctl list_queues name messages_ready messages_unacknowledged | \
  awk '$2 > 1000 {print "积压告警: " $0}'

# 4. 磁盘和内存
df -h /var/lib/rabbitmq
rabbitmqctl status | grep -E "memory|disk"

# 5. 连接数
echo "连接数: $(rabbitmqctl list_connections -q | wc -l)"
```

> 🎯 **运维核心**：稳定 + 可靠 + 安全。日常巡检看四项指标，分区做到自动合并+多地址连接，备份保异地+定期。
