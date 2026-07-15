# RabbitMQ 运维指南

> **定位**：核心目标 = 高可用、高可靠、高性能。覆盖日常运维、故障排查、集群运维、备份恢复、安全运维及 Java 客户端联动。

---

## 目录

1. [日常运维](#1-日常运维)
2. [故障排查](#2-故障排查)
3. [集群运维](#3-集群运维)
4. [备份与恢复](#4-备份与恢复)
5. [安全运维](#5-安全运维)
6. [Java 联动与最佳实践](#6-java-联动与最佳实践)

---

## 1. 日常运维

### 日常巡检（每日 1 次，10 分钟）

```bash
systemctl status rabbitmq-server          # 服务状态
rabbitmqctl list_queues name messages_ready messages_unacknowledged
rabbitmqctl list_connections | wc -l      # 连接数
df -h | grep /var/lib/rabbitmq            # 磁盘空间
tail -f /var/log/rabbitmq/rabbitmq.log   # 日志检查
```

### 四项核心指标

| 指标 | 异常判断 | 关联 Java |
|------|----------|-----------|
| **连接数** | 接近 `connections.max` | 连接池需扩容 |
| **Ready 消息** | 持续增长 | 消费者异常 |
| **Unacked 消息** | 过多 | 手动确认异常 |
| **磁盘空间** | 低于 `disk_free_limit` | 停止接收消息 |

### 每周维护

```bash
find /var/log/rabbitmq -name "*.log.*" -mtime +7 -delete  # 清理旧日志
rabbitmqctl purge_queue dlx_queue                          # 清理死信（确认后）
```

---

## 2. 故障排查

### 故障 1：Java 连接失败

| 步骤 | 排查 |
|:----:|------|
| 1 | `systemctl status rabbitmq-server` 确认服务启动 |
| 2 | `ping` + `telnet IP 5672` 确认网络 |
| 3 | 账号密码 + vhost 权限一致 |
| 4 | `loopback_users = none` 允许远程 |
| 5 | 连接数是否达 `connections.max` |

### 故障 2：消息积压

| 步骤 | 排查 |
|:----:|------|
| 1 | Java 消费者是否正常运行 |
| 2 | Unacked 过多 → 手动确认异常 |
| 3 | 消费逻辑是否有耗时操作 |
| 4 | 预取数 `prefetch` 是否过小 |

**解决**：增加消费者节点 + 优化消费逻辑 + 调整 `prefetch: 8`

### 故障 3：消息丢失

| 步骤 | 排查 |
|:----:|------|
| 1 | 三持久化（Exchange + Queue + Message） |
| 2 | 生产者发送是否有异常 |
| 3 | 路由键是否匹配 |
| 4 | 手动确认代码是否执行 |
| 5 | 磁盘空间是否充足 |

---

## 3. 集群运维

### 日常检查

```bash
rabbitmqctl cluster_status          # 集群状态
rabbitmqctl list_nodes              # 节点在线
rabbitmqctl sync_all_queues        # 数据同步
```

### 节点宕机处理

```text
① 确认宕机节点 → ② Java 自动切换其他节点 → ③ 重启宕机节点（自动加入集群）→ ④ 查日志排查原因
```

### Java 集群连接

```yaml
spring:
  rabbitmq:
    addresses: 192.168.1.100:5672,192.168.1.101:5672,192.168.1.102:5672
```

---

## 4. 备份与恢复

### 备份

```bash
# 全量备份（配置 + 消息）
rabbitmqctl export_definitions /backup/backup_$(date +%Y%m%d).json

# 每周日 02:00 自动备份
0 2 * * 0 rabbitmqctl export_definitions /backup/backup_$(date +%Y%m%d).json
```

### 恢复

```bash
systemctl stop rabbitmq-server
rabbitmqctl import_definitions /backup/backup_20240520.json
systemctl start rabbitmq-server
rabbitmqctl list_queues && rabbitmqctl list_exchanges  # 验证
```

---

## 5. 安全运维

| 措施 | 说明 |
|------|------|
| **账号** | 禁用 guest 远程、专属账号最小权限、每月更换密码 |
| **端口** | 5672/15672 仅允许内网访问，禁止公网暴露 |
| **日志** | root 权限限制、定期清理 |
| **配置** | root 权限限制修改 |

---

## 6. Java 联动与最佳实践

### 协同要点

| 场景 | 运维 | 开发 |
|------|------|------|
| 配置变更 | 提前通知 | 同步修改 `application.yml` |
| 故障排查 | 提供 MQ 日志/节点状态 | 提供客户端日志 |
| 版本更新 | 升级前测试兼容性 | 配合回归测试 |
| 压力测试 | 调整连接数/信道数 | 模拟高并发 |

### 最佳实践

| 原则 | 做法 |
|------|------|
| **环境隔离** | 开发/测试/生产完全独立 |
| **自动化** | crontab 定时备份/清理/巡检 |
| **监控告警** | Prometheus + Grafana，积压 >1000 告警 |
| **灰度发布** | 先测试 → 再小范围 → 再全量 |
| **文档留存** | 配置/节点/操作日志/故障处理记录 |

---

> 🎯 **运维核心**：稳定 + 可靠 + 安全。日常巡检看四项指标，故障排查按优先级逐个排除，集群保奇数节点+自动修复，备份保异地+定期。
