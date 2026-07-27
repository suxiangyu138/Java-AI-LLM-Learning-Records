# 08-HPA弹性伸缩
> 🎯 弹性伸缩是K8s最吸引人的特性之一 — 让集群根据业务负载自动调整Pod副本数，高峰期自动扩容承载流量，低谷期自动缩容节省资源，实现"按需付费"的云原生理想

---

## 目录
1. [HPA弹性伸缩概述](#1-hpa弹性伸缩概述)
2. [HPA工作原理深入](#2-hpa工作原理深入)
3. [安装Metrics Server](#3-安装metrics-server)
4. [基于CPU/Memory的HPA](#4-基于cpumemory的hpa)
5. [基于自定义指标的HPA](#5-基于自定义指标的hpa)
6. [HPA扩缩容策略详解](#6-hpa扩缩容策略详解)
7. [HPA vs VPA vs Cluster Autoscaler](#7-hpa-vs-vpa-vs-cluster-autoscaler)
8. [完整示例：user-service HPA配置](#8-完整示例user-service-hpa配置)

---

## 1. HPA弹性伸缩概述

### 1.1 什么是HPA

**HPA** (Horizontal Pod Autoscaler，水平Pod自动伸缩) 是K8s内置的自动扩缩容机制。它根据观测到的资源指标（CPU、内存、自定义指标）自动调整Deployment或StatefulSet的`replicas`副本数。

### 1.2 为什么需要HPA

```
传统部署方式：
  流量波峰 ──► CPU 100% ──► 服务超时 ──► 手动增加副本 ──► 流量已过
  流量波谷 ──► CPU 10%  ──► 资源闲置 ──► 手动减少副本 ──► 浪费成本
                        ↑
                总是慢半拍，运维心力交瘁

HPA自动伸缩：
  流量波峰 ──► CPU 80%  ──► 自动增加到10副本 ──► 平稳承接
  流量波谷 ──► CPU 20%  ──► 自动缩到3副本   ──► 节省成本
                        ↑
                自动化，无需人工介入
```

### 1.3 HPA适用场景

| 场景 | 是否适合HPA | 原因 |
|------|------------|------|
| 无状态Web服务（SpringBoot/Go） | ✅ 非常适合 | 每个Pod独立，扩容无副作用 |
| API网关 / 代理 | ✅ 非常适合 | 水平扩展友好 |
| 消息消费者 | ✅ 非常适合 | 根据队列深度动态调整消费者数量 |
| MySQL / Redis | ❌ 不适合 | 有状态应用HPA会导致数据混乱 |
| WebSocket长连接服务 | ⚠️ 需谨慎 | 扩容后连接迁移复杂，需业务层支持 |
| 批处理任务 | ❌ 不适合 | 由Job或Workflow管理，不需HPA |

> 💡 HPA最适合**无状态**应用。对于Deployment管理的有状态应用（StatefulSet），HPA也可以工作，但需要确保每个副本能独立处理请求（如分片模式）。

---

## 2. HPA工作原理深入

### 2.1 核心流程

```
    ┌─────────────┐     ┌──────────────────┐
    │ Metrics     │◄────│  kubelet (cAdvisor)│
    │ Server      │     │  Pod 1 / 2 / 3 ... │
    └──────┬──────┘     └──────────────────┘
           │ 每15秒拉取Pod资源指标
           ▼
    ┌─────────────┐
    │ HPA         │  计算逻辑：
    │ Controller  │  desiredReplicas = ceil[currentReplicas × (currentMetric / desiredMetric)]
    └──────┬──────┘
           │ 调整Deployment.replicas
           ▼
    ┌─────────────┐     ┌──────────────────┐
    │ Deployment  │────►│  ReplicaSet      │
    │ Controller  │     │  创建/删除Pod    │
    └─────────────┘     └──────────────────┘
```

### 2.2 关键时间参数

| 参数 | 默认值 | 说明 |
|------|--------|------|
| Metrics采集间隔 | 15秒 | kubelet cAdvisor每15秒一次 |
| HPA计算周期 | 15秒 | Metrics Server一次获取 |
| 扩容容忍延迟 | 15秒 | 检测到指标超阈值后立即触发 |
| 缩容容忍延迟 | 5分钟 | 缩容有冷却期，防止指标抖动 |
| 稳定窗口（默认） | 5分钟（扩容）/ 0（缩容） | K8s 1.25+ 可配置 |

### 2.3 期望副本数计算公式

HPA Controller的核心计算逻辑：

```
desiredReplicas = ceil[currentReplicas × (currentMetricValue / desiredMetricValue)]

示例：
  currentReplicas = 4,
  currentCPU = 85%（平均使用率）,
  targetCPU = 50%
  
  desiredReplicas = ceil[4 × (85 / 50)] = ceil[4 × 1.7] = ceil[6.8] = 7
```

> ⚠️ HPA的扩缩容是按比例平滑进行的，不会一次性从4跳到7，而是逐步调整。同时受`--horizontal-pod-autoscaler-tolerance`（默认0.1，即10%偏差）影响，偏差小于10%时不调整。

### 2.4 多条指标的聚合

HPA可以同时指定CPU和内存（或自定义指标），Controller会分别计算每个指标所需的副本数，然后取**最大值**：

```
当前副本数：4

CPU指标  → 当前80% / 目标50% → 需要 7 副本
内存指标 → 当前60% / 目标80% → 需要 3 副本
QPS指标  → 当前500 / 目标200  → 需要 10 副本

取最大值 → 最终目标：10 副本
```

---

## 3. 安装Metrics Server

Metrics Server是HPA的**前提条件**，它从kubelet收集Pod和Node的CPU/内存指标，供HPA Controller使用。

### 3.1 安装Metrics Server

```bash
# 下载最新版本
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml

# 验证安装
kubectl get deployment metrics-server -n kube-system
# NAME             READY   UP-TO-DATE   AVAILABLE   AGE
# metrics-server   1/1     1            1           30s

# 验证指标采集（等待1-2分钟）
kubectl top nodes
# NAME           CPU(cores)   CPU%   MEMORY(bytes)   MEMORY%
# k8s-master     542m         27%    2145Mi          56%
# k8s-worker-1   386m         19%    1832Mi          48%
# k8s-worker-2   412m         20%    1901Mi          49%

kubectl top pods
# NAME                              CPU(cores)   MEMORY(bytes)
# user-service-6d8f4f5b6c-abc12    125m         256Mi
# user-service-6d8f4f5b6c-def34    110m         240Mi
```

### 3.2 常见问题：Metrics Server在本地环境无法启动

```bash
# Minikube或Kind等本地环境，需要启用--kubelet-insecure-tls
kubectl edit deployment metrics-server -n kube-system
# 在args中添加：
# - --kubelet-insecure-tls
# - --kubelet-use-node-status-port

# Minikube一键安装
minikube addons enable metrics-server
```

### 3.3 Metrics Server能做什么 / 不能做什么

| 能力 | Metrics Server | Prometheus（自定义指标用） |
|------|---------------|---------------------------|
| CPU使用率 | ✅ | ✅ |
| 内存使用率 | ✅ | ✅ |
| 自定义指标（QPS/RT） | ❌ | ✅ |
| 历史数据存储 | ❌ | ✅（PromQL查询） |
| 长期趋势 | ❌ | ✅ |
| 存储消耗 | 极小（仅当前值） | 较大（时间序列） |
| 资源消耗 | 极低 | 较高 |

---

## 4. 基于CPU/Memory的HPA

### 4.1 最简单的CPU HPA

```yaml
apiVersion: autoscaling/v2          # ⭐ v2支持多种指标
kind: HorizontalPodAutoscaler
metadata:
  name: user-service-hpa
spec:
  scaleTargetRef:                   # 目标资源（必须支持扩缩容）
    apiVersion: apps/v1
    kind: Deployment
    name: user-service
  minReplicas: 2                    # ⭐ 最小副本数
  maxReplicas: 10                   # ⭐ 最大副本数
  metrics:                          # ⭐ 指标定义
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization           # 使用率模式（相对于Pod request）
        averageUtilization: 70      # CPU平均使用率超过70%触发
```

> 💡 `averageUtilization: 70`意味着：所有Pod的CPU使用率平均值超过70%时触发扩容。例如deployment有3个Pod，request.cpu=500m，当前平均使用率为350m（70%），正好在阈值，不触发。

### 4.2 CPU + Memory双重指标

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: dual-metric-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: user-service
  minReplicas: 2
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70      # CPU超过70%
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80      # 或内存超过80%
  # ⭐ 两种指标遵循"取最大值"原则
```

### 4.3 手动测试HPA

```bash
# 1. 创建一个带资源限制的Deployment
kubectl apply -f user-service.yaml

# 2. 创建HPA
kubectl apply -f user-service-hpa.yaml

# 3. 查看HPA状态
kubectl get hpa -w
# NAME                REFERENCE                  TARGETS    MINPODS   MAXPODS   REPLICAS   AGE
# user-service-hpa    Deployment/user-service    35%/70%    2         10        2          1m

# 4. 模拟负载（生成CPU压力）
kubectl run -it --rm load-generator --image=busybox -- sh -c "while true; do wget -q -O- http://user-service:8080/api/slow; done"

# 5. 观察HPA自动扩容（约1-2分钟）
kubectl get hpa -w
# 35%/70% → 88%/70% → 120%/70% → 扩容开始 → 5副本 → 8副本 → ...

# 6. 停止负载后观察缩容（约5分钟冷却期后）
# 88%/70% → 45%/70% → 35%/70% → 缩容开始 → 3副本 → 2副本
```

---

## 5. 基于自定义指标的HPA

### 5.1 架构

基于CPU/内存只能做基础伸缩，生产环境需要基于业务指标（QPS、请求延迟、队列深度）来实现更精准的弹性伸缩：

```
                    ┌───────────────┐
                    │  Prometheus   │
                    │  (监控系统)   │
                    └──────┬────────┘
                           │ PromQL查询
                           ▼
               ┌───────────────────────┐
               │   Prometheus Adapter  │
               │   (自定义指标适配器)   │
               └──────┬────────────────┘
                      │ /apis/custom.metrics.k8s.io
                      ▼
               ┌───────────────┐
               │  HPA Controller│
               └──────┬────────┘
                      │
                      ▼
               ┌───────────────┐
               │  Deployment   │
               └───────────────┘
```

### 5.2 安装Prometheus Adapter

```bash
# 使用Helm安装prometheus-adapter
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm install prometheus-adapter prometheus-community/prometheus-adapter \
  --namespace monitoring \
  --set prometheus.url=http://prometheus.monitoring.svc:9090 \
  --set config.logLevel=4

# 验证自定义指标API
kubectl get --raw /apis/custom.metrics.k8s.io/v1beta1 | jq .
```

### 5.3 基于QPS（每秒请求数）的HPA

```yaml
# Prometheus Adapter配置：将Prometheus指标暴露为自定义指标
# values.yaml for helm
prometheus-adapter:
  prometheus:
    url: http://prometheus.monitoring.svc:9090
  rules:
    default: false
    custom:
    - seriesQuery: 'rate(http_requests_total{namespace!=""}[1m])'
      resources:
        overrides:
          namespace: {resource: "namespace"}
          pod: {resource: "pod"}
      name:
        matches: "http_requests_total"
        as: "qps_per_pod"
      metricsQuery: |
        sum(rate(<<.Series>>{<<.LabelMatchers>>}[1m])) by (<<.GroupBy>>)
---
# HPA配置：基于QPS自动伸缩
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: qps-based-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: user-service
  minReplicas: 2
  maxReplicas: 20
  metrics:
  - type: Pods                     # ⭐ Pods类型：每个Pod的指标平均值
    pods:
      metric:
        name: qps_per_pod
      target:
        type: AverageValue
        averageValue: 100          # ⭐ 每个Pod QPS超过100时扩容
  behavior:                        # ⭐ 扩缩容策略
    scaleDown:
      stabilizationWindowSeconds: 60  # 缩容稳定窗口
    scaleUp:
      stabilizationWindowSeconds: 0
```

### 5.4 基于请求延迟（RT）的HPA

```yaml
# Prometheus Adapter配置
rules:
- seriesQuery: 'histogram_quantile(0.95, sum(rate(http_request_duration_seconds_bucket{namespace!=""}[5m])) by (le, pod))'
  resources:
    overrides:
      namespace: {resource: "namespace"}
      pod: {resource: "pod"}
  name:
    as: "p99_latency_seconds"
  metricsQuery: |
    histogram_quantile(0.99, sum(rate(<<.Series>>{<<.LabelMatchers>>}[5m])) by (le, <<.GroupBy>>))
---
# HPA
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: latency-based-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: user-service
  minReplicas: 2
  maxReplicas: 15
  metrics:
  - type: Pods
    pods:
      metric:
        name: p99_latency_seconds
      target:
        type: AverageValue
        averageValue: 0.5           # P99延迟超过500ms时扩容
```

### 5.5 基于队列深度的HPA（Kafka/RabbitMQ消费者）

```yaml
# 假设Prometheus采集了对应Kafka队列深度
rules:
- seriesQuery: 'kafka_consumer_lag{namespace!=""}'
  resources:
    overrides:
      namespace: {resource: "namespace"}
      pod: {resource: "pod"}
  name:
    as: "kafka_lag"
  metricsQuery: |
    sum(<<.Series>>{<<.LabelMatchers>>}) by (<<.GroupBy>>)
---
# HPA
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: consumer-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: message-consumer
  minReplicas: 1
  maxReplicas: 30
  metrics:
  - type: Pods
    pods:
      metric:
        name: kafka_lag
      target:
        type: AverageValue
        averageValue: 1000          # 每个消费者积压超过1000条时扩容
```

> 🎯 **自定义指标的最佳实践**：不同的业务场景选择不同的指标。Web服务用QPS或RT，消息消费者用队列深度，批处理任务用待处理任务数。建议将CPU/内存作为保底配置（防止自定义指标服务宕机），再加一个业务指标。

---

## 6. HPA扩缩容策略详解

### 6.1 为什么需要策略配置？

HPA的默认行为有"缩容过慢"或"扩容过快"的问题。K8s 1.18+引入了`behavior`字段，允许精细控制扩缩容行为：

### 6.2 完整的扩缩容策略

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: smart-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: user-service
  minReplicas: 2
  maxReplicas: 20
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  behavior:                            # ⭐ 精确控制扩缩容行为
    scaleDown:                         # 缩容策略
      stabilizationWindowSeconds: 300  # ⭐ 缩容冷静期（5分钟）
      policies:                        # 多条策略，取最大变化量
      - type: Percent
        value: 10                      # 每次最多缩容10%
        periodSeconds: 60
      - type: Pods
        value: 3                       # 每次最多缩3个Pod
        periodSeconds: 60
      selectPolicy: Min                # 取约束更严的策略（取最小值）
    scaleUp:                           # 扩容策略
      stabilizationWindowSeconds: 0    # 立即扩容，不需等待
      policies:
      - type: Percent
        value: 100                     # 每次最多扩容100%
        periodSeconds: 15
      - type: Pods
        value: 4                       # 每次最多扩4个Pod
        periodSeconds: 15
      selectPolicy: Max                # 取更宽松的策略（取最大值）
```

### 6.3 策略配置详解

| 参数 | 说明 | 推荐值 | 作用 |
|------|------|--------|------|
| `stabilizationWindowSeconds` | **稳定窗口** — 指标达标后等待这段时间才开始缩容 | 扩容: 0，缩容: 300 | 防止指标抖动导致频繁缩容 |
| `policies[].type` | 策略类型：`Pods`（绝对数量）或 `Percent`（百分比） | 扩容用Percent，缩容用Pods | 扩容时允许快速翻倍，缩容时平滑减少 |
| `policies[].value` | 变化量 | 扩容100%，缩容10% | 限制单次变化的幅度 |
| `policies[].periodSeconds` | 策略生效时间窗口（秒） | 扩容15s，缩容60s | 该窗口内最多变化多少 |
| `selectPolicy` | 多策略冲突时选择哪个 | 扩容`Max`，缩容`Min` | 扩容取宽松值加速，缩容取保守值平滑 |

### 6.4 不同场景的推荐策略

```yaml
# 场景1：Web应用（对突发流量敏感，快速扩容 + 平滑缩容）
behavior:
  scaleUp:
    stabilizationWindowSeconds: 0
    policies:
    - type: Percent
      value: 100            # 快速翻倍
      periodSeconds: 15
  scaleDown:
    stabilizationWindowSeconds: 300   # 保持5分钟
    policies:
    - type: Percent
      value: 10            # 每次最多缩10%
      periodSeconds: 60
---
# 场景2：消息消费者（队列深度驱动，避免过度扩缩）
behavior:
  scaleUp:
    stabilizationWindowSeconds: 30     # 等30秒确认队列持续积压
    policies:
    - type: Pods
      value: 2                         # 每次最多加2个消费者
      periodSeconds: 30
  scaleDown:
    stabilizationWindowSeconds: 180    # 确保队列清空后再缩
    policies:
    - type: Pods
      value: 1                         # 每次最多缩1个
      periodSeconds: 60
---
# 场景3：定时低谷业务（如凌晨报表系统，缩容需谨慎）
behavior:
  scaleDown:
    stabilizationWindowSeconds: 900    # 15分钟冷静期！
    policies:
    - type: Pods
      value: 1
      periodSeconds: 300               # 每5分钟缩1个
```

---

## 7. HPA vs VPA vs Cluster Autoscaler

这三种弹性伸缩机制分别解决不同层面的问题，共同构成K8s完整的弹性伸缩体系：

### 7.1 对比总表

| 特性 | HPA (水平Pod伸缩) | VPA (垂直Pod伸缩) | Cluster Autoscaler (节点伸缩) |
|------|-------------------|-------------------|------------------------------|
| **伸缩对象** | Pod副本数 | Pod资源限制（CPU/Memory） | 集群节点数 |
| **伸缩级别** | Pod级（水平） | Pod级（垂直） | 节点级 |
| **触发条件** | CPU/内存/自定义指标 | CPU/内存使用率历史 | 调度失败的Pod（Pending） |
| **扩容速度** | 秒~分钟级（创建Pod） | 分钟级（Pod重建） | 分钟~小时级（云节点创建） |
| **适用场景** | 无状态应用扩展 | 有状态应用资源调整 | 集群容量不足，节点不够 |
| **状态影响** | 无（多副本负载均衡） | Pod需重建（有影响） | 无（对Pod透明） |
| **典型配置** | min/max副本数 | requests/limits调整 | 节点池最小/最大节点数 |
| **依赖** | Metrics Server | Metrics Server | 云厂商API（AWS/GCP/Azure） |
| **成本影响** | 中等（Pod更多） | 较低（Pod大小调整） | 显著（节点增减） |
| **与Java应用** | SpringBoot非常适合 | 不适合（JVM需调优） | 间接影响 |

### 7.2 协同工作原理

```
                      ┌─────────────────┐
                      │  业务访问量      │
                      └────────┬────────┘
                               ▼
                    ┌─────────────────────┐
                    │  HPA (水平伸缩)      │ ← 无状态服务快速扩容
                    │  Deployment.Relicas  │
                    └────────┬────────────┘
                             │ Pod资源不足？
                             ▼
                    ┌─────────────────────┐
                    │  VPA (垂直伸缩)      │ ← 有状态应用资源调整
                    │  Pod Requests/Limits │
                    └────────┬────────────┘
                             │ 集群节点不足？
                             ▼
                    ┌─────────────────────┐
                    │  Cluster Autoscaler  │ ← 节点级扩展
                    │  新增/删除节点      │
                    └─────────────────────┘
```

### 7.3 什么时候该用什么

```yaml
# HPA场景：无状态Web服务
# ── 流量增加 → CPU/QPS上升 → HPA增加副本 → 流量被更多Pod分担
# ── 需求：SpringBoot、Nginx、API Gateway

# VPA场景：有状态服务、批处理
# ── Pod内存不足 → VPA分析历史使用率 → 增大内存limits → Pod重建生效
# ── 需求：Elasticsearch、Cassandra（需要精确资源，但不愿改代码）
# ⚠️ VPA重调度Pod会导致短暂中断

# Cluster Autoscaler场景：所有Pod扩容后仍Pending
# ── HPA将Pod从5扩到20，但节点资源不足 → Pod卡在Pending
# ── Cluster Autoscaler检测Pending Pod → 调用云API创建新节点
# ── 缩容：节点利用率低于阈值持续一段时间 → 驱逐Pod → 删除节点
```

> ⚠️ CDP（Critical Deployment Pattern）：**三者协同使用**才能实现完整的弹性伸缩。仅配HPA而没用Cluster Autoscaler，当节点资源耗尽时新Pod会一直Pending。建议：HPA + Cluster Autoscaler 配合使用，VPA按需引入。

---

## 8. 完整示例：user-service HPA配置

### 8.1 前置条件

```bash
# 1. 安装Metrics Server
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml

# 2. 确保Deployment设置了资源requests
# （HPA依赖requests计算使用率！）
```

### 8.2 Deployment — 必须设置资源限制

> ⚠️ **HPA必须配合resources.requests使用**！如果没有设置CPU request，HPA无法计算Utilization，会自动忽略CPU指标。

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: user-service
  namespace: production
  labels:
    app: user-service
spec:
  replicas: 2                        # ⭐ 初始副本
  selector:
    matchLabels:
      app: user-service
  template:
    metadata:
      labels:
        app: user-service
    spec:
      containers:
      - name: user-service
        image: registry.example.com/user-service:v1.2.3
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "production"
        - name: JAVA_OPTS
          value: "-Xms512m -Xmx512m -XX:+UseG1GC"
        resources:                     # ⭐ HPA依赖这里！
          requests:                    # ─ 必须设置requests
            cpu: "500m"                #   request 0.5核CPU
            memory: "512Mi"            #   request 512MB内存
          limits:                      # ─ limits建议和requests一致
            cpu: "1000m"
            memory: "1Gi"
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 20
          periodSeconds: 5
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 15
```

### 8.3 HPA完整配置

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: user-service-hpa
  namespace: production
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: user-service
  minReplicas: 2                      # ⭐ 保底：至少2个副本
  maxReplicas: 10                     # ⭐ 上限：最多10个副本
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70        # ⭐ CPU平均使用率70% → 扩容
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80        # ⭐ 内存80% → 扩容
  behavior:
    scaleUp:                          # ⭐ 扩容策略：激进
      stabilizationWindowSeconds: 0
      policies:
      - type: Percent
        value: 100                    # 每次最大扩容100%（翻倍）
        periodSeconds: 15
      - type: Pods
        value: 4
        periodSeconds: 15
      selectPolicy: Max               # 取最大值（扩更快）
    scaleDown:                        # ⭐ 缩容策略：保守
      stabilizationWindowSeconds: 300 # 等5分钟确认稳定
      policies:
      - type: Percent
        value: 10                     # 每次最多缩小10%
        periodSeconds: 60
      - type: Pods
        value: 2
        periodSeconds: 60
      selectPolicy: Min               # 取最小值（缩更慢）
```

### 8.4 监控HPA运行状态

```bash
# 实时监控HPA
kubectl get hpa -n production -w
# NAME               REFERENCE                 TARGETS                 MINPODS   MAXPODS   REPLICAS   AGE
# user-service-hpa   Deployment/user-service   45%/70%, 55%/80%        2         10        2          5m

# 查看HPA详情（重要：看Events知道为什么扩缩）
kubectl describe hpa user-service-hpa -n production
# ...
# Events:
#   Type    Reason             Age   From                       Message
#   ----    ------             ----  ----                       -------
#   Normal  SuccessfulRescale  2m    horizontal-pod-autoscaler  New size: 4; reason: cpu resource utilization (percentage of request) above target
#   Normal  SuccessfulRescale  5m    horizontal-pod-autoscaler  New size: 8; reason: cpu resource utilization (percentage of request) above target
#   Normal  SuccessfulRescale  12m   horizontal-pod-autoscaler  New size: 4; reason: All metrics below target

# 查看Pod的当前资源使用
kubectl top pod -n production -l app=user-service
# NAME                             CPU(cores)   MEMORY(bytes)
# user-service-6d8f4f5b6c-abc12   125m         256Mi
# user-service-6d8f4f5b6c-def34   110m         240Mi
# user-service-6d8f4f5b6c-ghi56   98m          230Mi       ← 扩容后的新Pod，负载更低
```

### 8.5 HPA常见问题排查

| 问题 | 原因 | 解决方案 |
|------|------|----------|
| HPA <unknown> 目标 | Metrics Server未安装或不可用 | `kubectl get apiservice | grep metrics` 检查API |
| HPA配置不生效 | targetRef未指向正确的Deployment | `kubectl get deployment` 验证名称和namespace |
| CPU指标为0或缺失 | Pod未设置resources.requests | 必须为Pod设置CPU request |
| 一直不缩容 | 缩容冷却期未过（默认5分钟） | 检查`stabilizationWindowSeconds` |
| 扩容后马上缩容 | 指标抖动 | 增大扩缩容忍偏差（`--horizontal-pod-autoscaler-tolerance`） |
| Pod扩到maxReplicas还在Pending | 节点资源不足 | 检查节点状态，考虑Cluster Autoscaler |
| HPA未触发即使指标超标 | 偏差小于10%（默认tolerance） | 指标超标10%以上才会触发调整 |
| 自定义指标不可用 | Prometheus Adapter配置错误 | `kubectl get --raw /apis/custom.metrics.k8s.io/v1beta1` 调试 |

### 8.6 SpringBoot应用的HPA最佳实践

```yaml
# SpringBoot + HPA的关键配置清单
# ┌─ Deployment: ──────────────────────────────────────────
# │ resources.requests.cpu = 500m          ← 基准CPU
# │ resources.requests.memory = 512Mi      ← 基准内存
# │ resources.limits = 与requests相同      ← QoS Guaranteed，更稳定
# │ JAVA_OPTS = -Xms512m -Xmx512m          ← JVM堆与Pod内存一致
# │ readinessProbe = /actuator/health      ← 确保新Pod准备就绪再加入LB
# │ livenessProbe = /actuator/health       ← 检测JVM是否健康
# │
# ├─ HPA: ────────────────────────────────────────────────
# │ minReplicas = 2                        ← 至少2副本（高可用）
# │ maxReplicas = 10                       ← 根据QPS估算上限
# │ cpu.averageUtilization = 70            ← 70%是黄金阈值
# │ memory.averageUtilization = 80         ← 内存阈值稍高
# │ scaleUp = 0s稳定 / 100%翻倍            ← 快速扩容
# │ scaleDown = 300s稳定 / 10%平滑         ← 缓慢缩容
```

> 💡 **CPU阈值为什么建议70%？** 因为要达到70%阈值意味着实际当前值=request×0.7=350m（500m request时）。还有30%的缓冲空间应对突发流量，不会因短时毛刺就立即扩容。设置太低（如30%）会导致频繁扩缩，设置太高（如90%）则扩容不及时可能已经超时了。

---

> 🎯 **HPA核心要点总结**：HPA是K8s无状态应用的"自动变速箱" — 根据CPU/内存/业务指标自动调整Pod副本数。**前提条件**：部署Metrics Server并给Pod设置resources.requests。生产环境建议"CPU+自定义指标"双指标监控，扩容策略激进（立即扩、快速扩）、缩容策略保守（5分钟冷静期、每次缩10%）。配合Cluster Autoscaler实现真正的端到端弹性伸缩。**没有HPA的K8s集群等于开手动挡汽车** — 永远在手动调整副本数的路上疲于奔命。
