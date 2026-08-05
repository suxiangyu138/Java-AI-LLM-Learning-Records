# 00 - Prometheus 与 Grafana 知识体系总览

> 🎯 Prometheus + Grafana 是云原生监控的事实标准 — 从指标采集到可视化、从告警规则到故障定位，构建 Java 微服务可观测性的核心基座

---

## 目录

1. [知识全景](#1-知识全景)
2. [文件导航](#2-文件导航)
3. [精通级学习路线](#3-精通级学习路线)

---

## 1. 知识全景

```
Prometheus + Grafana 监控告警体系（11个文件）
│
├── 🏗️ 基础入门（01-03）
│   ├── 01-Prometheus概述与架构.md        # 时序数据库/拉模型/架构/生态
│   ├── 02-Prometheus安装与配置.md        # 部署/prometheus.yml/scrape/federation
│   └── 03-数据模型与指标类型.md           # Metric/Counter/Gauge/Histogram/Summary
│
├── 📊 核心能力（04-05）
│   ├── 04-PromQL查询语言.md              # 选择器/运算符/函数/聚合/子查询
│   └── 05-Grafana安装与仪表盘.md          # 数据源/面板/Dashboard/变量/告警
│
├── 🔍 实战监控（06-07）
│   ├── 06-Node-Exporter主机监控.md        # CPU/内存/磁盘/网络/进程监控
│   └── 07-Spring-Boot应用监控.md          # Micrometer/Actuator/JVM指标/自定义指标
│
├── 🚨 告警体系（08）
│   └── 08-AlertManager告警管理.md         # 告警规则/路由/分组/静默/通知
│
├── ⚙️ 运维进阶（09-10）
│   ├── 09-服务发现与Pushgateway.md        # K8s SD/Consul/短任务推送
│   └── 10-生产最佳实践与故障排查.md        # 容量规划/存储/高可用/常见故障
│
└── 📌 00-Prometheus与Grafana知识体系总览.md  # ← 本文件
```

---

## 2. 文件导航

| # | 文件 | 核心内容 | 级别 |
|---|------|----------|:---:|
| 00 | Prometheus与Grafana知识体系总览 | 全景导航 + 学习路线 | — |
| 01 | Prometheus概述与架构 | 时序数据库/拉模型/组件架构/与Zabbix对比 | ⭐⭐ |
| 02 | Prometheus安装与配置 | Docker/二进制部署/prometheus.yml/scrape配置 | ⭐ |
| 03 | 数据模型与指标类型 | Metric/Counter/Gauge/Histogram/Summary/Label | ⭐⭐⭐ |
| 04 | PromQL查询语言 | 选择器/运算符/rate/irate/聚合/子查询/TopK | ⭐⭐⭐⭐ |
| 05 | Grafana安装与仪表盘 | 数据源/面板类型/Dashboard/变量/Import/Export | ⭐⭐ |
| 06 | Node-Exporter主机监控 | CPU/内存/磁盘/网络/进程/自定义textfile | ⭐⭐ |
| 07 | Spring-Boot应用监控 | Micrometer/Actuator/JVM指标/自定义Metrics/@Timed | ⭐⭐⭐ |
| 08 | AlertManager告警管理 | 告警规则/routing tree/分组/静默/企业微信/钉钉 | ⭐⭐⭐ |
| 09 | 服务发现与Pushgateway | K8s SD/Consul/file_sd/Pushgateway/短任务 | ⭐⭐⭐ |
| 10 | 生产最佳实践与故障排查 | 容量规划/存储TSDB/高可用Thanos/常见故障排查 | ⭐⭐⭐⭐ |

---

## 3. 精通级学习路线

### 🟢 L1：搭起来看到数据（半天）

```
01-概述 → 02-安装配置 → 06-Node-Exporter → 05-Grafana仪表盘
产出：能部署 Prometheus + Grafana、监控主机、导入 Dashboard
```

### 🔵 L2：Java 应用可观测（1天）

```
03-数据模型 → 07-Spring-Boot监控 → 04-PromQL → 05-Grafana进阶
产出：能监控 JVM 指标、自定义业务指标、编写 PromQL 查询
```

### 🟣 L3：告警与运维自动化（半天）

```
08-AlertManager → 04-PromQL进阶 → 10-生产最佳实践
产出：能配置告警规则、对接通知渠道、处理告警风暴
```

### 🟡 L4：大规模生产落地（1天）

```
09-服务发现 → 10-高可用Thanos → 10-容量规划与存储
产出：能在 K8s 环境下自动发现服务、搭建高可用监控体系
```

---

> 🎯 **监控是可观测性的基础** — 没有指标就没有告警，没有告警就没有 SLO。Prometheus + Grafana 是云原生时代 Java 后端必须掌握的监控技术栈。
