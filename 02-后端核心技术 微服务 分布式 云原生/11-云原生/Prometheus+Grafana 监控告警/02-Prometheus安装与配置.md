# 02 - Prometheus 安装与配置

> 🎯 从 Docker 快速体验到生产级二进制部署、从 prometheus.yml 到 scrape 配置 — 搭起来是理解一切的前提

---

## 目录

1. [部署方式对比](#1-部署方式对比)
2. [Docker 快速安装](#2-docker-快速安装)
3. [二进制生产部署](#3-二进制生产部署)
4. [prometheus.yml 核心配置](#4-prometheusyml-核心配置)
5. [scrape_configs 详解](#5-scrape_configs-详解)
6. [Federation 联邦集群](#6-federation-联邦集群)

---

## 1. 部署方式对比

| 方式 | 适用场景 | 优点 | 缺点 |
|------|---------|------|------|
| **Docker** | 开发/测试环境 | 一键启动、隔离 | 生产需额外配置持久化 |
| **Docker Compose** | 开发环境全套 | Prometheus + Grafana + Exporters | 单机 |
| **二进制** | 生产物理机/虚拟机 | 性能最优、可控 | 需手动管理 |
| **Kubernetes** | 生产 K8s 环境 | Helm/Prometheus Operator | 依赖 K8s |

---

## 2. Docker 快速安装

### 2.1 单命令启动

```bash
# 启动 Prometheus（挂载配置文件）
docker run -d --name prometheus -p 9090:9090 \
  -v /opt/prometheus/prometheus.yml:/etc/prometheus/prometheus.yml \
  -v /opt/prometheus/data:/prometheus \
  prom/prometheus:v2.51.0

# 验证
curl http://localhost:9090/-/healthy
# Prometheus is Healthy.
```

### 2.2 Docker Compose 全套

```yaml
# docker-compose.yml
version: '3.8'
services:
  prometheus:
    image: prom/prometheus:v2.51.0
    container_name: prometheus
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
      - ./data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--storage.tsdb.retention.time=15d'
    restart: always

  grafana:
    image: grafana/grafana:10.4.0
    container_name: grafana
    ports:
      - "3000:3000"
    volumes:
      - ./grafana/data:/var/lib/grafana
    restart: always

  node-exporter:
    image: prom/node-exporter:v1.7.0
    container_name: node-exporter
    ports:
      - "9100:9100"
    restart: always
```

```bash
docker-compose up -d
```

---

## 3. 二进制生产部署

### 3.1 安装步骤

```bash
# 下载
wget https://github.com/prometheus/prometheus/releases/download/v2.51.0/prometheus-2.51.0.linux-amd64.tar.gz
tar -xzf prometheus-2.51.0.linux-amd64.tar.gz

# 创建目录与用户
sudo useradd -r -s /sbin/nologin prometheus
sudo mkdir -p /opt/prometheus /var/lib/prometheus

# 复制文件
sudo cp prometheus-2.51.0.linux-amd64/prometheus /usr/local/bin/
sudo cp prometheus-2.51.0.linux-amd64/promtool /usr/local/bin/
sudo cp -r prometheus-2.51.0.linux-amd64/consoles prometheus-2.51.0.linux-amd64/console_libraries /etc/prometheus/

# 权限
sudo chown -R prometheus:prometheus /opt/prometheus /var/lib/prometheus /etc/prometheus
```

### 3.2 systemd 服务

```ini
# /etc/systemd/system/prometheus.service
[Unit]
Description=Prometheus Server
After=network.target

[Service]
Type=simple
User=prometheus
ExecStart=/usr/local/bin/prometheus \
    --config.file=/etc/prometheus/prometheus.yml \
    --storage.tsdb.path=/var/lib/prometheus \
    --storage.tsdb.retention.time=15d \
    --web.listen-address=0.0.0.0:9090
Restart=on-failure
LimitNOFILE=65535

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now prometheus
sudo systemctl status prometheus
```

---

## 4. prometheus.yml 核心配置

```yaml
# /etc/prometheus/prometheus.yml
global:
  scrape_interval: 15s         # 采集间隔（默认 1m，建议 15-30s）
  evaluation_interval: 15s     # 告警规则评估间隔
  external_labels:             # 外部标签（Federation 时标识集群）
    cluster: 'prod-shanghai'

# ═══ 告警规则文件 ═══
rule_files:
  - "rules/*.yml"

# ═══ 告警管理 ═══
alerting:
  alertmanagers:
    - static_configs:
        - targets: ['localhost:9093']

# ═══ 采集目标 ═══
scrape_configs:
  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']

  - job_name: 'node-exporter'
    static_configs:
      - targets:
          - '192.168.1.10:9100'
          - '192.168.1.11:9100'
        labels:
          env: 'prod'
```

| 全局参数 | 默认值 | 推荐值 | 说明 |
|----------|--------|--------|------|
| `scrape_interval` | 1m | 15-30s | 采集频率，高精场景 5-10s |
| `evaluation_interval` | 1m | 15s-1m | 告警评估频率 |
| `scrape_timeout` | 10s | 10-30s | 单次采集超时 |

---

## 5. scrape_configs 详解

### 5.1 Static Config（静态配置）

```yaml
scrape_configs:
  - job_name: 'spring-boot-apps'
    scrape_interval: 30s
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets:
          - '10.0.1.10:8080'
          - '10.0.1.11:8080'
        labels:
          app: 'user-service'
          env: 'prod'
```

### 5.2 文件服务发现

```yaml
scrape_configs:
  - job_name: 'spring-boot-apps'
    file_sd_configs:
      - files:
          - '/etc/prometheus/targets/*.json'
        refresh_interval: 1m
```

```json
// /etc/prometheus/targets/apps.json
[
  {
    "targets": ["10.0.1.10:8080", "10.0.1.11:8080"],
    "labels": {
      "app": "user-service",
      "env": "prod"
    }
  }
]
```

### 5.3 relabel_configs（标签重写）

```yaml
scrape_configs:
  - job_name: 'spring-boot-apps'
    static_configs:
      - targets: ['10.0.1.10:8080']
    relabel_configs:
      # 保留以 app_ 开头的标签
      - action: keep
        regex: 'app_.*'
        source_labels: [__meta_*]
      # 替换标签名
      - action: labelmap
        regex: __meta_kubernetes_pod_label_(.+)
```

---

## 6. Federation 联邦集群

> 💡 当监控规模超过单机 Prometheus 时，使用 Federation 分层聚合。

```
                    ┌──────────────┐
                    │   Global      │  ← 全局 Prometheus（只聚合，不抓取 Target）
                    │  Prometheus   │
                    └──────┬───────┘
              ┌────────────┼────────────┐
              ▼            ▼            ▼
      ┌──────────┐ ┌──────────┐ ┌──────────┐
      │ 机房A    │ │ 机房B    │ │  K8s     │  ← 区域 Prometheus
      │Prometheus│ │Prometheus│ │Prometheus│
      └──────────┘ └──────────┘ └──────────┘
```

```yaml
# 全局 Prometheus 配置：从区域 Prometheus 聚合
scrape_configs:
  - job_name: 'federate'
    honor_labels: true
    metrics_path: '/federate'
    params:
      'match[]':
        - '{job=~".+"}'              # 聚合所有指标
    static_configs:
      - targets:
          - 'prometheus-dc1:9090'
          - 'prometheus-dc2:9090'
          - 'prometheus-k8s:9090'
```

> 🎯 **建议**：单集群/中小规模直接单机 Prometheus；大规模（>1000 Target 或 >100 万 Series）使用 Federation 或 Thanos/Cortex 方案。
