# RabbitMQ /api/nodes 接口详细内容（生产级）

> **定位**：`/api/nodes` 是 RabbitMQ Management API 核心接口，用于获取集群所有节点详细信息。是网络分区排查、自动化监控、元数据校验的核心工具。

---

## 1. 接口基础信息

| 属性 | 说明 |
|------|------|
| **地址** | `http://{host}:15672/api/nodes` |
| **方式** | GET |
| **认证** | Basic Auth |
| **依赖** | `rabbitmq_management` 插件 |
| **格式** | JSON 数组 |

---

## 2. 请求方式

### curl

```bash
curl -u admin:pass http://192.168.1.100:15672/api/nodes | jq
curl -u admin:pass http://192.168.1.100:15672/api/nodes/rabbit@node1
```

### Java

```java
String auth = username + ":" + password;
String encoded = Base64.getEncoder().encodeToString(auth.getBytes());
headers.set("Authorization", "Basic " + encoded);
ResponseEntity<String> resp = restTemplate.exchange(
    "http://" + host + ":15672/api/nodes", HttpMethod.GET, entity, String.class);
```

---

## 3. 核心返回字段

| 字段 | 类型 | 说明 | 关联前文 |
|------|------|------|----------|
| `name` | String | 节点名 `rabbit@node1` | 元数据/扩容 |
| `running` | Boolean | 运行状态 | 故障监控 |
| **`partitions`** | Array | ⭐ 分区列表，空=正常 | 网络分区核心指标 |
| `mem_alarm` | Boolean | 内存告警 | 性能优化 |
| `disk_free_alarm` | Boolean | 磁盘告警 | 存储扩展 |
| `mem_used` / `mem_limit` | Number | 已用/限制内存 | 性能优化 |
| `disk_free` / `disk_free_limit` | Number | 剩余/告警阈值磁盘 | 存储扩展 |
| `fd_used` / `fd_total` | Number | 文件描述符 | 高并发优化 |
| `sockets_used` / `sockets_total` | Number | 套接字 | 连接优化 |
| `uptime` | Number | 运行时长(ms) | 稳定性监控 |
| `plugins` | Array | 已启用插件 | 插件扩展 |
| `listeners` | Array | 监听端口(5672/15672/25672) | 网络配置 |

### 正常状态示例

```json
{
  "name": "rabbit@node1",
  "running": true,
  "partitions": [],
  "mem_alarm": false,
  "disk_free_alarm": false
}
```

### 分区状态示例

```json
{
  "name": "rabbit@node1",
  "running": true,
  "partitions": ["rabbit@node2", "rabbit@node3"]
}
```

---

## 4. 运维场景应用

| 场景 | 字段 | 动作 |
|------|------|------|
| **分区排查** | `partitions` ≠ `[]` | 触发告警 → 应急处理 |
| **节点宕机** | `running` = false | 重启节点 |
| **内存过载** | `mem_alarm` = true | 优化消费/调整内存配置 |
| **磁盘不足** | `disk_free_alarm` = true | 扩容/清理消息 |
| **元数据校验** | `name`/`plugins`/`listeners` | 扩容/插件后验证 |

---

## 5. 注意事项

| 注意点 | 说明 |
|--------|------|
| 插件必需 | `rabbitmq_management` 未启 → 404 |
| 权限 | 账号需 `management` 或 `administrator` 标签 |
| 频率 | 监控建议 ≤1 次/分钟 |
| 多集群 | 各集群单独调用，避免混淆 |
| 版本 | 字段随版本略有差异，适配 3.12.x |
