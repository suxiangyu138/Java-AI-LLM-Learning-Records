# 08 - AlertManager 告警管理

> 🎯 告警不是"发了就行" — AlertManager 的分组/抑制/静默/路由机制，将海量告警转化为可行动的精准通知，避免告警风暴淹没真正的问题

---

## 目录

1. [AlertManager 概述](#1-alertmanager-概述)
2. [安装与配置](#2-安装与配置)
3. [告警路由（Routing Tree）](#3-告警路由routing-tree)
4. [告警分组与抑制](#4-告警分组与抑制)
5. [静默（Silence）](#5-静默silence)
6. [通知渠道配置](#6-通知渠道配置)
7. [告警规则编写最佳实践](#7-告警规则编写最佳实践)

---

## 1. AlertManager 概述

### 告警处理流程

```
Prometheus Server
  │ 评估告警规则 → 触发告警
  ▼
AlertManager
  │ 1. 去重（相同告警合并）
  │ 2. 分组（相同 Label 的告警聚合）
  │ 3. 抑制（高优先级抑制低优先级）
  │ 4. 静默（维护期间屏蔽）
  │ 5. 路由（按 Label 分发到不同渠道）
  ▼
通知渠道（邮件/钉钉/企微/Slack/PagerDuty）
```

| 功能 | 说明 | 效果 |
|------|------|------|
| **分组** | 将同类告警合并为一条通知 | 避免 100 台机器宕机发 100 封邮件 |
| **抑制** | 高优先级告警触发后抑制相关低级告警 | 机房断网时不发单机宕机告警 |
| **静默** | 维护窗口期间屏蔽告警 | 发版时不触发告警 |
| **路由** | 按严重级别/团队分发到不同渠道 | P0 电话、P1 企微、P2 邮件 |

---

## 2. 安装与配置

```bash
# Docker
docker run -d --name alertmanager -p 9093:9093 \
  -v /opt/alertmanager/alertmanager.yml:/etc/alertmanager/alertmanager.yml \
  prom/alertmanager:v0.27.0
```

```yaml
# alertmanager.yml
global:
  resolve_timeout: 5m            # 告警恢复判定时间
  smtp_smarthost: 'smtp.example.com:587'
  smtp_from: 'alert@example.com'

# ═══ 路由树（核心） ═══
route:
  receiver: 'default'             # 默认接收器
  group_by: ['alertname', 'cluster']  # 分组标签
  group_wait: 10s                 # 首次等待（等待同组告警）
  group_interval: 5m              # 同组新告警发送间隔
  repeat_interval: 4h             # 重复发送间隔
  routes:                         # 子路由
    - match:
        severity: critical
      receiver: 'pagerduty-critical'
      continue: true
    - match:
        severity: warning
      receiver: 'slack-warning'
    - match_re:
        team: 'backend|platform'
      receiver: 'dingtalk-backend'

# ═══ 抑制规则 ═══
inhibit_rules:
  - source_match:
      severity: critical
      alertname: 'NetworkDown'
    target_match_re:
      alertname: '(NodeDown|ServiceDown)'
    # 网络断掉时，抑制所有主机/服务宕机告警

# ═══ 接收器 ═══
receivers:
  - name: 'default'
    email_configs:
      - to: 'ops@example.com'
```

---

## 3. 告警路由（Routing Tree）

> 💡 路由树是 AlertManager 的核心 — 按 Label 匹配将告警分发到不同的通知渠道。

### 路由匹配规则

```yaml
route:
  receiver: 'default'
  routes:
    # ═══ 精确匹配 ═══
    - match:
        severity: critical        # severity == "critical"
      receiver: 'oncall'

    # ═══ 正则匹配 ═══
    - match_re:
        app: 'user.*'             # app 以 user 开头
      receiver: 'user-team'

    # ═══ 多条件 AND ═══
    - match:
        severity: critical
        team: backend
      receiver: 'backend-oncall'

    # ═══ continue: true（匹配后继续往下） ═══
    - match:
        severity: critical
      receiver: 'pagerduty'
      continue: true              # 继续匹配后续规则
```

| 参数 | 说明 | 推荐值 |
|------|------|--------|
| `group_by` | 按哪些 Label 分组合并 | `['alertname', 'cluster']` |
| `group_wait` | 首次等待时间 | 10-30s |
| `group_interval` | 同组内新告警的发送间隔 | 5m |
| `repeat_interval` | 未恢复告警的重复通知间隔 | 4h（避免轰炸） |

---

## 4. 告警分组与抑制

### 4.1 分组示意图

```
无分组（300 台机器宕机 = 300 条通知）:
  [NodeDown host01] [NodeDown host02] ... [NodeDown host300]

有分组（group_by: alertname）:
  [NodeDown — 300 instances affected]
    host01, host02, host03, ..., host300
```

### 4.2 抑制规则

```yaml
inhibit_rules:
  # 规则1：机房断网抑制主机宕机
  - source_match:
      alertname: 'DatacenterDown'
      severity: critical
    target_match:
      alertname: 'NodeDown'
    equal: ['datacenter']          # 同机房才抑制

  # 规则2：K8s 节点 NotReady 抑制 Pod Crash
  - source_match:
      alertname: 'KubeNodeNotReady'
    target_match_re:
      alertname: '(KubePodCrashLooping|KubePodNotReady)'
    equal: ['node']
```

---

## 5. 静默（Silence）

### 通过 Web UI 创建

```
AlertManager UI (http://localhost:9093) → New Silence
  Matchers: instance=~"10.0.1.10"
  Duration: 2h
  Comment: "应用发版维护窗口"
```

### 通过 API 创建

```bash
curl -X POST http://localhost:9093/api/v2/silences \
  -H "Content-Type: application/json" \
  -d '{
    "matchers": [{"name": "instance", "value": "10.0.1.10", "isRegex": false}],
    "startsAt": "2024-01-15T02:00:00Z",
    "endsAt": "2024-01-15T04:00:00Z",
    "comment": "应用发版维护窗口",
    "createdBy": "张三"
  }'
```

> 💡 发版/维护前务必创建 Silence！否则发版期间重启服务会触发大量宕机告警。

---

## 6. 通知渠道配置

### 6.1 邮件

```yaml
receivers:
  - name: 'email-ops'
    email_configs:
      - to: 'ops@example.com'
        headers:
          Subject: '[{{ .Status | toUpper }}] {{ .GroupLabels.alertname }}'
        html: |
          <h2>{{ .GroupLabels.alertname }}</h2>
          <p>状态: {{ .Status }}</p>
          {{ range .Alerts }}
          <p>{{ .Annotations.summary }}</p>
          {{ end }}
```

### 6.2 钉钉 Webhook

```yaml
receivers:
  - name: 'dingtalk'
    webhook_configs:
      - url: 'https://oapi.dingtalk.com/robot/send?access_token=xxx'
        send_resolved: true
        message: |
          {
            "msgtype": "markdown",
            "markdown": {
              "title": "{{ .GroupLabels.alertname }}",
              "text": "### {{ .Status | toUpper }}\n{{ range .Alerts }}\n- {{ .Annotations.summary }}\n{{ end }}"
            }
          }
```

### 6.3 企业微信

```yaml
receivers:
  - name: 'wecom'
    webhook_configs:
      - url: 'https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=xxx'
        send_resolved: true
```

---

## 7. 告警规则编写最佳实践

### ✅ DO

```yaml
# 1. 标签命名
labels:
  severity: critical     # critical / warning / info
  team: backend          # 归属团队
  component: mysql       # 组件名称

# 2. 注解提供上下文
annotations:
  summary: "{{ $labels.instance }} 内存使用率 > 90%"
  description: "当前值: {{ $value | humanize }}%\n详情: https://grafana.example.com/d/xxx"
  runbook_url: "https://wiki.example.com/runbooks/high-memory"

# 3. for 持续时间（避免抖动）
for: 5m                  # 持续 5 分钟才触发
```

### ❌ DON'T

| 反模式 | 说明 | 正确做法 |
|--------|------|----------|
| `for: 0s` | 抖动即告警 | 至少 `for: 1m` |
| 阈值太严格 | CPU > 50% 就告警 | CPU > 80% 持续 10m |
| 无 runbook | 收到告警不知道怎么处理 | 附上处理手册链接 |
| 无 severity 标签 | 无法区分严重程度 | 统一设置 severity |
| `repeat_interval: 1m` | 每分钟重复轰炸 | 至少 1h，推荐 4h |

> 🎯 **告警哲学**：每一条告警都应该有可执行的响应动作。如果告警触发后你只是看一眼然后忽略，那这条告警就不该存在。
