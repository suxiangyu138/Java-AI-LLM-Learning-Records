# 05 - Grafana 安装与仪表盘

> 🎯 Grafana 是 Prometheus 数据的最佳可视化平台 — 从数据源配置到面板设计、从 Dashboard 变量到告警通道，将冷冰冰的指标数据转化为一眼可读的业务洞察

---

## 目录

1. [Grafana 安装](#1-grafana-安装)
2. [数据源配置](#2-数据源配置)
3. [Dashboard 核心概念](#3-dashboard-核心概念)
4. [面板类型速查](#4-面板类型速查)
5. [变量与模板化](#5-变量与模板化)
6. [Import / Export Dashboard](#6-import--export-dashboard)
7. [Grafana 告警（内置）](#7-grafana-告警内置)

---

## 1. Grafana 安装

### 1.1 Docker

```bash
docker run -d --name grafana -p 3000:3000 \
  -v /opt/grafana/data:/var/lib/grafana \
  grafana/grafana:10.4.0

# 默认登录：admin / admin（首次需修改密码）
```

### 1.2 二进制部署

```bash
wget https://dl.grafana.com/oss/release/grafana-10.4.0.linux-amd64.tar.gz
tar -xzf grafana-10.4.0.linux-amd64.tar.gz
cd grafana-10.4.0
./bin/grafana-server
```

### 1.3 配置要点

```ini
# /etc/grafana/grafana.ini
[server]
http_port = 3000
domain = monitor.example.com
root_url = https://monitor.example.com/

[auth.anonymous]
enabled = false          # ⚠️ 禁止匿名访问

[dashboards]
default_home_dashboard_path = /etc/grafana/dashboards/home.json
```

---

## 2. 数据源配置

| 步骤 | 操作 |
|------|------|
| 1 | `Configuration → Data Sources → Add data source` |
| 2 | 选择 **Prometheus** |
| 3 | URL: `http://localhost:9090` |
| 4 | Scrape interval: `15s` |
| 5 | `Save & Test` |

```yaml
# 多数据源场景
datasources:
  - name: Prometheus-Prod
    url: http://prometheus-prod:9090
  - name: Prometheus-Dev
    url: http://prometheus-dev:9090
  - name: Loki
    url: http://loki:3100
```

---

## 3. Dashboard 核心概念

```
Dashboard（仪表盘）
├── Row（行）— 逻辑分组
│   ├── Panel（面板）— 单个图表/表格/文本
│   │   ├── Query（查询）— PromQL
│   │   ├── Visualization（可视化）— 图表类型
│   │   └── Alert（告警）— 面板级告警规则
│   └── Panel ...
├── Variable（变量）— 动态筛选
└── Time Range（时间范围）— 全局或面板级
```

### 创建第一个 Dashboard

```
1. New Dashboard → Add visualization
2. 选择数据源 Prometheus
3. 编写 PromQL：
   rate(http_server_requests_seconds_count[5m])
4. 设置 Legend: {{method}} {{uri}}
5. 选择图表类型：Time series / Stat / Gauge / Table
6. Save
```

---

## 4. 面板类型速查

| 面板类型 | 适用场景 | 示例 |
|----------|----------|------|
| **Time series** | 趋势图（最常用） | QPS/延迟/CPU 随时间变化 |
| **Stat** | 单一数值 | 当前在线用户数/错误率 |
| **Gauge** | 仪表盘 | 内存使用率%（绿/黄/红） |
| **Bar gauge** | 横向进度条 | 各实例 CPU 使用率排名 |
| **Table** | 表格列表 | Top 10 慢接口 |
| **Heatmap** | 热力图 | 延迟分布（x=时间, y=延迟, 颜色=数量） |
| **Pie chart** | 饼图 | 各 HTTP 状态码占比 |
| **Text** | Markdown/HTML | 标题/说明/跳转链接 |

### Stat 面板阈值配置

```yaml
Thresholds:
  - Green:  0-80  (正常)
  - Orange: 80-90 (警告)
  - Red:    90-100(严重)
```

---

## 5. 变量与模板化

> 💡 Dashboard 变量让一个仪表盘适配多环境/多应用，核心价值。

### 5.1 变量类型

| 类型 | 说明 | 示例 |
|------|------|------|
| **Query** | PromQL 查询结果 | 获取所有 `instance` 标签值 |
| **Custom** | 手动定义固定值 | `prod,staging,dev` |
| **Interval** | 时间间隔 | `1m,5m,10m,30m,1h` |
| **Data source** | 数据源选择 | 切换生产/开发 Prometheus |

### 5.2 Query 变量实战

```promql
# 变量名: instance
# 查询语句:
label_values(node_cpu_seconds_total, instance)

# 变量名: handler
# 查询语句:
label_values(http_requests_total, handler)

# 变量名: env
# 类型: Custom
# 值: prod,staging,dev
```

### 5.3 在 PromQL 中使用变量

```promql
# 通过 $varname 或 ${varname} 引用
rate(http_requests_total{instance="$instance", handler="$handler"}[5m])

# 多选时用 =~
rate(http_requests_total{instance=~"$instance"}[5m])
```

---

## 6. Import / Export Dashboard

### 6.1 导入社区 Dashboard

```
Grafana 官方仓库: https://grafana.com/grafana/dashboards/

热门 ID:
├── 1860  Node Exporter Full        ← 必装
├── 4701  JVM (Micrometer)          ← Java 应用
├── 14430 Spring Boot 2.x/3.x       ← Spring Boot
├── 7362  MySQL Overview
├── 763   Redis Dashboard
└── 12230 Kubernetes Cluster

导入方式: Dashboards → Import → 输入 ID → Load
```

### 6.2 导出为 JSON

```bash
# Dashboard → Settings → JSON Model → Copy/Download
# 导出后可纳入 Git 版本管理（Infrastructure as Code）
```

---

## 7. Grafana 告警（内置）

```yaml
# Alert Rule 示例
# Panel → Alert → Create alert rule

Condition:
  avg() of query(A, 5m) > 0.8         # 过去 5 分钟平均值 > 80%

Evaluate:
  Evaluate every 1m for 5m            # 每 1 分钟评估，持续 5 分钟才触发

Labels:
  severity: warning
  team: backend

Annotations:
  summary: "{{ $labels.instance }} 内存使用率超过 80%"
  description: "当前值: {{ $values.A.Value }}%"
```

| 通知渠道 | 配置 |
|----------|------|
| 邮件 | SMTP 配置 + Contact Point |
| 钉钉 | Webhook → 自定义消息模板 |
| 企业微信 | Webhook → 自定义消息模板 |
| Slack | 内置集成 |
| PagerDuty | 内置集成 |

> 💡 **Grafana 内置告警 vs AlertManager**：简单场景用 Grafana 告警就够了；复杂路由/分组/静默需求用 AlertManager。两者可共存。
