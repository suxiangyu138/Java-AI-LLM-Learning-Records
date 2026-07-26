# 01 - Prometheus 概述与架构

> 🎯 Prometheus 是 CNCF 第二个毕业项目（仅次于 K8s），基于拉模型 + 时序数据库 + PromQL 的监控体系 — 理解其架构选择是用好整个技术栈的前提

---

## 目录

1. [Prometheus 是什么](#1-prometheus-是什么)
2. [核心架构](#2-核心架构)
3. [拉模型 vs 推模型](#3-拉模型-vs-推模型)
4. [核心组件](#4-核心组件)
5. [Prometheus vs Zabbix vs ELK](#5-prometheus-vs-zabbix-vs-elk)
6. [Java 后端视角的监控四层模型](#6-java-后端视角的监控四层模型)

---

## 1. Prometheus 是什么

Prometheus（普罗米修斯）是 SoundCloud 开源的系统监控与告警工具包，2016 年加入 CNCF，是 Kubernetes 之后第二个毕业项目。

| 特性 | 说明 |
|------|------|
| **时序数据库** | 内置 TSDB，按时间序列存储指标数据 |
| **拉模型** | Server 主动从 Target 拉取（Pull）指标 |
| **PromQL** | 强大的查询语言，支持聚合/运算/预测 |
| **多维数据模型** | 指标名 + Key-Value Labels 标识时间序列 |
| **服务发现** | 自动发现 K8s/Consul/云厂商的监控目标 |
| **告警管理** | AlertManager 独立组件，分组/路由/静默 |

---

## 2. 核心架构

```
                          ┌──────────────┐
                          │  AlertManager │ → 邮件/钉钉/企微/Slack/PagerDuty
                          └──────┬───────┘
                                 │ 告警推送
                          ┌──────▼───────┐
       ┌──────────────────│  Prometheus  │───┐
       │   Pull 拉取指标   │   Server     │   │ Pushgateway
       │                  └──────┬───────┘   │ (短任务推送)
       │                         │           │
  ┌────▼────┐  ┌────────┐  ┌────▼────┐  ┌───▼──────┐
  │ Node    │  │ Spring │  │  Redis  │  │ 自定义    │
  │Exporter │  │ Boot   │  │Exporter │  │ Exporter  │
  └─────────┘  │/actuator│ └─────────┘  └──────────┘
               └─────────┘

  存储：本地 TSDB（默认 15 天）→ Remote Write 到 VictoriaMetrics/Thanos 长期存储
  可视化：Grafana 通过 PromQL 查询 → Dashboard 展示
```

### 架构核心要点

| 要点 | 说明 |
|------|------|
| **Pull 模型** | Prometheus Server 定期从 Target 的 `/metrics` 端点拉取数据 |
| **Exporter** | 将非 Prometheus 格式的指标转换为 `/metrics` 端点的代理程序 |
| **TSDB** | 内置时间序列数据库，按 2 小时 Block 压缩存储 |
| **AlertManager** | 独立告警组件，从 Prometheus 接收告警后进行分组/抑制/静默/路由 |

---

## 3. 拉模型 vs 推模型

| 维度 | 拉模型（Prometheus） | 推模型（Zabbix/Graphite） |
|------|---------------------|--------------------------|
| 数据流向 | Server → Agent 拉取 | Agent → Server 推送 |
| 健康检测 | 拉取失败 = 目标 down | 需额外心跳机制 |
| 服务发现 | 天然匹配 K8s/Consul | 需 Agent 注册 |
| 安全控制 | Server 端集中控制 | 需 Agent 端认证 |
| 短任务 | ❌ 不友好（需 Pushgateway） | ✅ 任务结束前推送 |
| 网络要求 | Server 需可达 Target | Agent 需可达 Server |

> 💡 Prometheus 选择拉模型的核心原因：**服务发现 + 健康检测一体化**。拉取成功说明 Target 存活，拉取失败可立即告警。K8s 环境下，Prometheus 通过 K8s API 自动发现 Pod/Service 并抓取。

---

## 4. 核心组件

| 组件 | 作用 | 部署方式 |
|------|------|----------|
| **Prometheus Server** | 核心服务，抓取并存储时序数据 | 二进制/Docker/K8s |
| **Exporters** | 暴露指标给 Prometheus 抓取 | 与被监控目标部署在一起 |
| **AlertManager** | 告警管理：去重/分组/路由/静默 | 独立部署 |
| **Pushgateway** | 接收短任务推送的指标 | 独立部署 |
| **Grafana** | 可视化仪表盘 | 独立部署 |

### 常用 Exporter

| Exporter | 监控对象 | 默认端口 |
|----------|----------|:---:|
| Node Exporter | Linux 主机（CPU/内存/磁盘/网络） | 9100 |
| Blackbox Exporter | HTTP/TCP/DNS 探活 | 9115 |
| MySQL Exporter | MySQL 数据库 | 9104 |
| Redis Exporter | Redis | 9121 |
| Kafka Exporter | Kafka | 9308 |
| Spring Boot Actuator | JVM + 应用指标 | 8080/actuator |

---

## 5. Prometheus vs Zabbix vs ELK

| 维度 | Prometheus | Zabbix | ELK |
|------|-----------|--------|-----|
| 数据模型 | 多维 Labels + 时序 | 结构化监控项 | 全文搜索 + 日志 |
| 查询语言 | PromQL（强大） | Zabbix 表达式 | Lucene/KQL |
| 存储 | 本地 TSDB（短期） | MySQL/PostgreSQL | Elasticsearch |
| 可视化 | Grafana（最佳拍档） | 内置（较弱） | Kibana |
| 告警 | AlertManager（灵活） | 内置（完善） | Watcher |
| 云原生 | ⭐⭐⭐ K8s 原生 | ⭐ | ⭐⭐ |
| 学习曲线 | ⭐⭐⭐ 较陡 | ⭐⭐ 中等 | ⭐⭐⭐ 陡峭 |
| **主场景** | **微服务/K8s 指标监控** | 传统 IDC 基础设施 | **日志分析** |

> 💡 **选型建议**：微服务/云原生场景选 Prometheus + Grafana；传统机房运维选 Zabbix；日志分析选 ELK。三者可互补共存。

---

## 6. Java 后端视角的监控四层模型

```
┌─────────────────────────────────────────────┐
│ L4: 业务监控    │ 订单量/用户注册/API QPS/错误率  │ ← 自定义 Metrics
├─────────────────────────────────────────────┤
│ L3: 应用监控    │ JVM/GC/线程池/连接池/HTTP 延迟 │ ← Spring Boot Actuator
├─────────────────────────────────────────────┤
│ L2: 中间件监控  │ MySQL/Redis/Kafka 连接池/慢查询 │ ← Exporter
├─────────────────────────────────────────────┤
│ L1: 基础设施监控 │ CPU/内存/磁盘/网络/容器        │ ← Node Exporter
└─────────────────────────────────────────────┘
```

> 🎯 **每一层都有对应的 Prometheus Exporter 和 Grafana Dashboard**。从下往上逐步建设：先 L1+L2 保底，再 L3+L4 精细化。
