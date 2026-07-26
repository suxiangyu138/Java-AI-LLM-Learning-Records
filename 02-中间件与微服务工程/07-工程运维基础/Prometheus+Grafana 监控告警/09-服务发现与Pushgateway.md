# 09 - 服务发现与 Pushgateway

> 🎯 动态环境中手动维护 Target 列表是运维噩梦 — K8s/Consul/file_sd 服务发现让 Prometheus 自动感知目标变化；Pushgateway 解决短任务监控的拉模型短板

---

## 目录

1. [服务发现概述](#1-服务发现概述)
2. [Kubernetes 服务发现](#2-kubernetes-服务发现)
3. [Consul 服务发现](#3-consul-服务发现)
4. [基于文件的服务发现（file_sd）](#4-基于文件的服务发现file_sd)
5. [Pushgateway：短任务监控](#5-pushgateway短任务监控)
6. [Pushgateway 使用规范](#6-pushgateway-使用规范)

---

## 1. 服务发现概述

| 方式 | 适用场景 | 复杂度 |
|------|----------|:---:|
| `static_configs` | 固定 IP 的传统部署 | ⭐ |
| `file_sd` | CMDB/Ansible 生成 Target 列表 | ⭐⭐ |
| `kubernetes_sd` | K8s 环境（自动发现 Pod/Service） | ⭐⭐⭐ |
| `consul_sd` | Consul 注册中心环境 | ⭐⭐ |
| `dns_sd` | DNS SRV 记录 | ⭐ |

---

## 2. Kubernetes 服务发现

### 2.1 四种发现角色

| Role | 发现对象 | 典型用途 |
|------|----------|----------|
| `pod` | 所有 Pod（需要 annotation 标注） | 应用 Pod 监控 |
| `service` | Service 端点 | 通过 Service 发现后端 |
| `endpoints` | Service 的 Endpoint IP | 抓取每个 Pod |
| `node` | K8s Node | Node Exporter |

### 2.2 Pod 发现配置

```yaml
scrape_configs:
  - job_name: 'kubernetes-pods'
    kubernetes_sd_configs:
      - role: pod
    relabel_configs:
      # 仅抓取有 prometheus.io/scrape: "true" 注解的 Pod
      - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_scrape]
        action: keep
        regex: true
      # 从注解获取 metrics 路径
      - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_path]
        action: replace
        target_label: __metrics_path__
        regex: (.+)
      # 从注解获取端口
      - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_port]
        action: replace
        target_label: __address__
        regex: (.+)
        replacement: $1
      # 添加 K8s 元数据标签
      - source_labels: [__meta_kubernetes_namespace]
        target_label: namespace
      - source_labels: [__meta_kubernetes_pod_name]
        target_label: pod
```

### 2.3 Spring Boot Pod 注解

```yaml
# Deployment 中为 Pod 添加 Prometheus 注解
apiVersion: apps/v1
kind: Deployment
metadata:
  name: user-service
spec:
  template:
    metadata:
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/port: "8080"
        prometheus.io/path: "/actuator/prometheus"
```

### 2.4 Node Exporter 自动发现

```yaml
scrape_configs:
  - job_name: 'kubernetes-nodes'
    kubernetes_sd_configs:
      - role: node
    relabel_configs:
      - target_label: __address__
        replacement: kubernetes.default.svc:443
      - source_labels: [__meta_kubernetes_node_name]
        target_label: __metrics_path__
        replacement: /api/v1/nodes/$1/proxy/metrics
```

---

## 3. Consul 服务发现

```yaml
scrape_configs:
  - job_name: 'consul-services'
    consul_sd_configs:
      - server: 'consul.example.com:8500'
        services: ['user-service', 'order-service', 'gateway']
    relabel_configs:
      - source_labels: [__meta_consul_service]
        target_label: job
      - source_labels: [__meta_consul_tags]
        action: keep
        regex: '.*prometheus.*'       # 仅抓取有 prometheus 标签的服务
```

```bash
# Consul 注册服务时添加 Prometheus 标签
consul services register -name=user-service -port=8080 \
  -tag=prometheus -tag=prod
```

---

## 4. 基于文件的服务发现（file_sd）

> 💡 适合非 K8s 的动态环境，配合 CMDB/CI 自动生成 Target 文件。

```yaml
scrape_configs:
  - job_name: 'spring-boot'
    file_sd_configs:
      - files:
          - '/etc/prometheus/targets/*.json'
        refresh_interval: 1m         # 每分钟重载文件
```

```json
// /etc/prometheus/targets/apps.json
// 由 CMDB/CI 系统自动生成，无需重启 Prometheus
[
  {
    "targets": ["10.0.1.10:8080", "10.0.1.11:8080"],
    "labels": {
      "app": "user-service",
      "env": "prod",
      "team": "backend"
    }
  },
  {
    "targets": ["10.0.1.20:8080", "10.0.1.21:8080"],
    "labels": {
      "app": "order-service",
      "env": "prod",
      "team": "backend"
    }
  }
]
```

```bash
# CI/CD 部署脚本自动注册/注销 Target
register_target() {
    local APP=$1 INSTANCE=$2
    # 追加到 targets JSON 文件
    jq ". += [{\"targets\": [\"$INSTANCE\"], \"labels\": {\"app\": \"$APP\"}}]" \
      /etc/prometheus/targets/apps.json > /tmp/apps.json
    mv /tmp/apps.json /etc/prometheus/targets/apps.json
}
```

---

## 5. Pushgateway：短任务监控

> 💡 Prometheus 拉模型不适合短任务（Cron Job/Batch）。Pushgateway 作为中间网关接收短任务主动推送的指标。

### 5.1 架构

```
Cron Job / Batch Process
    │ ① 任务结束前 push 指标
    ▼
Pushgateway（内存存储）
    │ ② Prometheus 定期 pull
    ▼
Prometheus Server
```

### 5.2 安装

```bash
docker run -d --name pushgateway -p 9091:9091 prom/pushgateway:v1.7.0
```

```yaml
# prometheus.yml 采集 Pushgateway
scrape_configs:
  - job_name: 'pushgateway'
    honor_labels: true              # 保留推送时的原始标签
    static_configs:
      - targets: ['localhost:9091']
```

### 5.3 推送指标

```bash
# 推送单个指标
echo "batch_job_duration_seconds{job_name=\"data-export\"} 123" \
  | curl --data-binary @- http://localhost:9091/metrics/job/batch_jobs

# 推送多个指标
cat <<EOF | curl --data-binary @- http://localhost:9091/metrics/job/batch_jobs/instance/localhost
# HELP batch_job_last_success_timestamp 最后成功时间
# TYPE batch_job_last_success_timestamp gauge
batch_job_last_success_timestamp{job_name="data-export"} $(date +%s)
# HELP batch_job_status 状态 1=成功 0=失败
# TYPE batch_job_status gauge
batch_job_status{job_name="data-export"} 1
# HELP batch_job_duration_seconds 任务执行耗时
# TYPE batch_job_duration_seconds gauge
batch_job_duration_seconds{job_name="data-export"} 123
EOF
```

### 5.4 Java 代码推送

```java
// 使用 Prometheus Java Client 推送
PushGateway pushGateway = new PushGateway("localhost:9091");

CollectorRegistry registry = new CollectorRegistry();
Gauge.build("batch_job_status", "Job status")
    .labelNames("job_name")
    .register(registry)
    .labels("data-export")
    .set(1);

pushGateway.pushAdd(registry, "batch_jobs",
    Map.of("instance", InetAddress.getLocalHost().getHostName()));
```

---

## 6. Pushgateway 使用规范

| ✅ DO | ❌ DON'T |
|-------|----------|
| 仅用于短任务/Cron Job | 不要用于长期运行的服务 |
| 任务结束前推送一次 | 不要持续推送（会覆盖历史） |
| 推送后清理旧指标 | 不要积累僵尸指标 |
| 使用 `pushAdd`（增量） | 慎用 `push`（全量替换） |
| 配合 `honor_labels: true` | — |

> ⚠️ **Pushgateway 陷阱**：Pushgateway 不会自动清理过期指标。如果短任务挂了，上次推送的指标会永存，导致误报"任务成功"。解决方案：使用 `push_time` 指标 + 告警规则检查时效性。

```promql
# 告警：指标超过 2 小时未更新（任务可能已挂）
time() - push_time_seconds{job="batch_jobs"} > 7200
```
