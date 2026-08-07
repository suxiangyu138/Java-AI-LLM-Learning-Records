# Kibana 告警与监控
> Alerting 把「看板等人盯」变成「异常自动报警」，Stack Monitoring 让 ELK 自身健康可视——规则类型、连接器、执行链路、监控面板，一文打通

## 目录
1. [Kibana Alerting 体系](#1-kibana-alerting-体系)
2. [规则类型（Rule Types）](#2-规则类型rule-types)
3. [连接器（Connectors）](#3-连接器connectors)
4. [规则创建实战：日志异常告警](#4-规则创建实战日志异常告警)
5. [规则执行链路与去重](#5-规则执行链路与去重)
6. [Stack Monitoring：监控 ELK 自身](#6-stack-monitoring监控-elk-自身)
7. [告警最佳实践](#7-告警最佳实践)

---

## 1. Kibana Alerting 体系

### 1.1 告警架构

```text
规则（Rule）定时触发
   │
   ▼
ES 执行查询（阈值判断）
   │
   ▼
满足条件 → 生成告警（Alert）→ 执行动作（Actions）
   │
   ▼
Connector 发送通知（邮件/Webhook/钉钉/企业微信/Serverless）
```

| 组件 | 说明 |
|------|------|
| Rule（规则） | 「什么条件 + 多久查一次 + 触发后干什么」的完整定义 |
| Alert（告警实例） | 规则被触发时创建的告警记录 |
| Action（动作） | 触发后执行的通知动作 |
| Connector（连接器） | 动作的发送通道（邮件、Webhook 等） |
| Alerting UI | 规则管理、告警列表、案例管理 |

### 1.2 9.x 架构变化

```text
· 告警规则执行下沉：ES 9.x 中规则执行与查询都在 ES 侧完成
  （Alerting 框架基于 ES），Kibana 负责 UI 与连接器触发
· 规则执行状态：成功/失败/超时可从 ES 侧查询
· Kibana 保存规则的 Saved Object（.kibana_alerting_*）
```

---

## 2. 规则类型（Rule Types）

### 2.1 常用规则类型

| 规则类型 | 适用场景 | 配置要点 |
|------|------|------|
| Elasticsearch query | 日志/业务数据阈值 | 查询 DSL + 阈值条件 |
| ES|QL query | 管道分析告警（9.x 推荐） | ES|QL 语句 + 行数/值条件 |
| Metric threshold | 监控指标阈值 | 聚合 + 比较符 |
| Log threshold | 日志计数阈值 | 分组 + 计数比较 |
| PromQL | Prometheus 指标告警（技术预览） | PromQL 表达式 |
| Anomaly detection | 异常检测（需 ML 订阅） | 任务 + 偏离度 |

### 2.2 规则的核心配置项

| 配置项 | 说明 | 示例 |
|------|------|------|
| 数据视图/索引 | 查询目标 | `logs-*` |
| 查询条件 | KQL/DSL/ES|QL | `level: ERROR` |
| 阈值 | 比较条件 | `count >= 10` |
| 时间窗口 | 每次评估看多久 | Last 5 minutes |
| 检查频率 | 多久评估一次 | Every 1 minute |
| 分组 | 按字段分组告警 | BY service.name |
| 恢复条件 | 何时视为恢复 | 连续 2 次不满足 |

---

## 3. 连接器（Connectors）

### 3.1 常用连接器

| 连接器 | 用途 | 配置 |
|------|------|------|
| Email | 邮件通知 | SMTP 服务器 + 发件人 |
| Webhook | 任意 HTTP 回调 | URL + 请求体模板 |
| Slack / MS Teams | 聊天通知 | 机器人 Webhook |
| 钉钉/企业微信 | 国内办公通知 | 机器人 Webhook |
| Index | 写入 ES 索引（审计） | 目标索引 |
| Serverless | 云函数触发 | 云平台配置 |

### 3.2 Webhook 模板示例（钉钉）

```json
{
  "msgtype": "text",
  "text": {
    "content": "【日志告警】{{context.rule.name}}\n触发时间: {{context.timestamp}}\n命中数: {{context.conditions}}"
  }
}
```

> ⚠️ 注意：9.x 中连接器密钥加密存储（`.kibana_security` 索引）；Webhook 生产环境务必 HTTPS。

---

## 4. 规则创建实战：日志异常告警

### 4.1 场景：ERROR 日志 5 分钟超 10 条告警

```text
① Alerting → Rules → Create rule
② 规则类型：Elasticsearch query（或 ES|QL query）
③ 数据视图：logs-*
④ 查询：level: "ERROR"
⑤ 阈值：count >= 10
⑥ 时间窗口：Last 5 minutes；检查频率：Every 1 minute
⑦ 按 service.name 分组（区分告警来源）
⑧ 动作：Webhook（钉钉）→ 保存
```

### 4.2 ES|QL 告警规则示例

```sql
-- 规则查询：统计最近 5 分钟错误数按服务分组
FROM logs-*
| WHERE level == "ERROR" AND @timestamp > now() - 5m
| STATS error_count = COUNT(*) BY service.name
| WHERE error_count >= 10
```

| 条件 | 语义 |
|------|------|
| 返回行数 ≥ 1 | 有服务超阈值 → 触发 |
| 按行触发 | 每个超阈值服务生成一条告警 |

---

## 5. 规则执行链路与去重

### 5.1 执行链路

```text
调度器（Scheduler）→ 触发规则评估
→ ES 执行查询/聚合
→ 阈值判断
→ 满足 → 生成 Alert 实例（按分组键去重）
→ 执行 Actions（连接器发送）
→ 不满足且之前触发 → 生成恢复事件
```

### 5.2 告警去重与分组

| 机制 | 说明 |
|------|------|
| 分组键 | 按 `service.name` 分组 → 每个服务一个告警实例 |
| 去重窗口 | 相同实例在规则周期内不重复告警 |
| 恢复 | 条件连续不满足 → 自动标记恢复（可配置恢复动作） |
| 静默 | 可临时静默某个规则或分组 |

### 5.3 告警风暴防护

```text
① 分组收敛：按服务/集群分组，避免逐条日志告警
② 阈值合理：先观察基线（P95 等）再定阈值
③ 检查频率：1m 足够，别设 5s（成本与噪音）
④ 恢复动作：告警 + 恢复都通知，运维才知道「好了」
⑤ 静默窗口：变更窗口期静默已知告警
```

---

## 6. Stack Monitoring：监控 ELK 自身

### 6.1 监控架构

```text
Elasticsearch / Logstash / Kibana / Beats
    ↓ 指标上报（xpack.monitoring）
ES 监控索引（.monitoring-*）
    ↑
Kibana → Stack Monitoring（可视化）
```

### 6.2 各组件监控要点

| 组件 | 关键指标 |
|------|------|
| Elasticsearch | 集群状态（绿/黄/红）、节点堆内存、CPU、磁盘水位、分片数、查询延迟 |
| Kibana | 请求数、响应时间、内存（Node.js） |
| Logstash | pipeline 吞吐、worker 数、队列积压、JVM 堆 |
| Filebeat/Beats | 采集速率、错误数 |

### 6.3 启用配置

```yaml
# ES：xpack.monitoring.collection.enabled: true
# Kibana：xpack.monitoring.ui.container.elasticsearch.enabled: true（容器场景）
# Logstash：xpack.monitoring.enabled: true + 上报地址
```

```yaml
# logstash.yml
xpack.monitoring.enabled: true
xpack.monitoring.elasticsearch.hosts: ["http://es-node:9200"]
xpack.monitoring.elasticsearch.username: "logstash_system"
xpack.monitoring.elasticsearch.password: "xxx"
```

### 6.4 集群健康的三色判断

| 状态 | 含义 | 处理 |
|------|------|------|
| Green | 主分片 + 副本都就绪 | 正常 |
| Yellow | 主分片就绪、副本缺失 | 单节点常见，扩容节点/检查副本 |
| Red | 有主分片缺失 | 数据不可用，紧急排查 |

---

## 7. 告警最佳实践

### 7.1 告警设计七条

| # | 原则 | 说明 |
|:---:|------|------|
| 1 | 先有基线再定阈值 | 观察 1-2 周正常波动 |
| 2 | 分层告警 | 紧急（页面打不开）/ 重要（错误率超线）/ 提示（容量预警） |
| 3 | 分组收敛 | 按服务分组，别逐条 |
| 4 | 带上上下文 | 告警内容含服务、时间段、链接（直达 Discover） |
| 5 | 双通道 | 主通道 + 备用通道（Webhook + 邮件） |
| 6 | 恢复必报 | 告警与恢复成对，避免「永远在响」 |
| 7 | 定期演练 | 每月一次真实触发验证链路 |

### 7.2 告警内容模板（生产参考）

```text
【严重】订单服务错误率告警
服务：order-service
时间段：2026-08-07 14:00 - 14:05
错误数：152 条（阈值 10）
错误 Top 接口：/order/create（87 条）
直达链接：https://kibana.example.com/...（预过滤的 Discover 深链）
```

> 🎯 **核心要点**：Kibana 告警 = **规则（条件 + 频率）+ 连接器（通道）+ 分组去重（收敛）** 三件套；9.x 中规则执行已下沉 ES 侧。生产落地顺序：先启 Stack Monitoring 看基线 → 再建规则（带分组与恢复）→ 接双通道。告警设计三句诀：**分组收敛不轰炸、阈值基于基线、恢复必报闭环**。

---

**下一模块**：[05-Kibana安全与索引管理](05-Kibana安全与索引管理.md) | **返回总览**：[00-Kibana专题总览](00-Kibana专题总览.md)
