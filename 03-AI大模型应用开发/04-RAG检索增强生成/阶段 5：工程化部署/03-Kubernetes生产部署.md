# 03 Kubernetes 生产部署

> 生产环境的标准形态：K8s 多命名空间隔离、探针保障就绪、HPA 弹性扩缩、Helm 管理发布——从"容器能跑"到"集群能扛"。

## 📚 目录

1. [什么时候需要 K8s](#1-什么时候需要-k8s)
2. [核心概念速览](#2-核心概念速览)
3. [RAG 的 K8s 参考架构](#3-rag-的-k8s-参考架构)
4. [探针与优雅启停](#4-探针与优雅启停)
5. [HPA 弹性扩缩](#5-hpa-弹性扩缩)
6. [Helm 与发布管理](#6-helm-与发布管理)
7. [多集群与容灾](#7-多集群与容灾)
8. [面试高频问法](#8-面试高频问法)

## 1. 什么时候需要 K8s

### 从 Compose 到 K8s 的信号

| 信号 | 说明 |
|---|---|
| 多副本需求 | 单副本扛不住 QPS |
| 弹性扩缩 | 流量波动需要自动扩缩 |
| 高可用 | 节点故障自动恢复 |
| 多环境 | 开发/测试/生产独立集群 |
| 团队协作 | 发布流程需要规范化 |

### 判断

```
QPS < 100 且可接受停机 → Docker Compose 足够
QPS 高/需要弹性/需要 HA → K8s
```

> 💡 别过度设计：单机/Compose 能扛住时，K8s 是负担而非资产。

## 2. 核心概念速览

| 概念 | 作用 | RAG 中的例子 |
|---|---|---|
| Deployment | 无状态服务的副本管理 | 编排服务、摄取 worker |
| StatefulSet | 有状态服务 | 向量库、数据库（可选） |
| Service | 服务发现与负载均衡 | 对内访问 Embedding |
| Ingress | 外部入口 | 用户访问编排服务 |
| ConfigMap/Secret | 配置与密钥 | 模型配置、API Key |
| HPA | 水平自动扩缩 | 按 QPS 扩副本 |
| NetworkPolicy | 网络隔离 | 命名空间 default-deny |
| Job/CronJob | 一次性/定时任务 | 摄取、Ragas 定时评估 |

## 3. RAG 的 K8s 参考架构

### 命名空间划分（01 篇回顾 + 部署落地）

```
rag-platform：编排 Deployment + 摄取 CronJob
embeddings：  TEI Deployment（GPU，仅内部）
vectordb：    Qdrant StatefulSet（3 节点）
llm-gateway： LiteLLM Deployment + Postgres/Redis
observability：Langfuse + Prometheus + Grafana
data：        Postgres + Redis + Kafka
ingress-nginx：共享入口
```

### 编排服务示例

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: rag-api
  namespace: rag-platform
spec:
  replicas: 3                       # 副本数（04 篇容量计算）
  selector:
    matchLabels: { app: rag-api }
  template:
    metadata:
      labels: { app: rag-api }
    spec:
      containers:
        - name: rag-api
          image: registry/rag-api:1.2.0   # 锁定版本
          ports: [{ containerPort: 8000 }]
          envFrom:
            - configMapRef: { name: rag-config }
            - secretRef: { name: rag-secrets }
          resources:
            requests: { cpu: "1", memory: "2G" }
            limits: { cpu: "2", memory: "4G" }
          readinessProbe:
            httpGet: { path: /health, port: 8000 }
            initialDelaySeconds: 10
          livenessProbe:
            httpGet: { path: /health, port: 8000 }
            periodSeconds: 30
```

### NetworkPolicy 示例（default-deny）

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: default-deny
  namespace: embeddings
spec:
  podSelector: {}              # 匹配所有 Pod
  policyTypes: ["Ingress"]     # 默认拒绝所有入站
---
# 显式允许：rag-platform → embeddings
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: allow-rag-to-tei
  namespace: embeddings
spec:
  podSelector:
    matchLabels: { app: tei }
  policyTypes: ["Ingress"]
  ingress:
    - from:
        - namespaceSelector:
            matchLabels: { name: rag-platform }
```

## 4. 探针与优雅启停

### 三种探针

| 探针 | 检查 | 失败行为 |
|---|---|---|
| startupProbe | 启动是否完成（慢启动服务用） | 重启 |
| readinessProbe | 是否可接流量 | 从 Service 摘除 |
| livenessProbe | 是否存活 | 重启容器 |

### RAG 场景的探针设计

| 服务 | readiness | 要点 |
|---|---|---|
| 编排服务 | /health（含依赖检查） | 依赖挂时不要接流量 |
| Embedding | /health（模型加载完成） | 模型加载慢，startup 要长 |
| 向量库 | /readyz | 分片就绪才接流量 |
| LLM 网关 | /health/live | 供应商不可用也算不健康 |

### 优雅停机

```
terminationGracePeriodSeconds: 30   # 给处理中的请求留时间
preStop hook：排空连接 → 停止接受新请求
```

## 5. HPA 弹性扩缩

### 基于自定义指标扩缩

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: rag-api-hpa
  namespace: rag-platform
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: rag-api
  minReplicas: 3
  maxReplicas: 20
  metrics:
    - type: Pods
      pods:
        metric: { name: rag_qps }      # Prometheus 自定义指标
        target: { type: AverageValue, averageValue: "50" }
```

### 扩缩设计

| 层 | 指标 | 说明 |
|---|---|---|
| 编排 | QPS/请求延迟 | 50 QPS/副本（04 篇容量） |
| Embedding | 队列长度 | GPU 利用率 |
| 向量库 | 查询延迟 | 分片扩展（手动为主） |
| LLM 网关 | 供应商限流 | 网关扩副本 |

### HPA 注意事项

```
① 预留 30% 余量（HPA 响应有延迟）
② 扩缩下限保护（minReplicas 防缩没）
③ 冷却时间（防抖动）
④ GPU 服务扩缩较慢，阈值要保守
```

## 6. Helm 与发布管理

### Helm 的价值

```
Compose 文件 → Helm Chart（模板化 + 版本化 + 回滚）
```

| 能力 | 说明 |
|---|---|
| 模板化 | values.yaml 一处改全环境 |
| 版本管理 | Chart 版本可回滚 |
| 复用 | 一套 Chart 部署 dev/prod |
| 依赖管理 | 向量库等第三方 Chart |

### 发布策略

| 策略 | RAG 适用 |
|---|---|
| 滚动更新 | 编排服务（默认） |
| 蓝绿 | 大版本升级（模型切换） |
| 金丝雀 | 新模型小流量验证 |

### 发布检查清单

```
① 镜像版本锁定（不用 latest）
② ConfigMap 变更确认（环境变量）
③ 数据库迁移脚本先行
④ 模型版本记录（哪个 Embedding/LLM）
⑤ 回滚预案（Helm rollback）
```

## 7. 多集群与容灾

### 跨区域故障切换（2026 实践）

```
主区域：Azure OpenAI（UAE North）
备区域：Bedrock（Bahrain）
仅数据共享 DPA 允许时启用
切换触发：主区域不可用超过阈值
```

### 容灾设计

| 层 | 方案 |
|---|---|
| 数据 | 向量库备份 + 跨区域复制 |
| 模型 | 多供应商（DeepSeek + 备选） |
| 应用 | 多区域部署 + DNS 切换 |
| 评估 | 容灾演练（恢复时长目标） |

### 容灾演练

```
季度演练：
① 模拟主区域故障
② 观察切换耗时与数据一致性
③ 记录 RTO（恢复时间）与 RPO（数据损失）
④ 改进演练暴露的问题
```

## 8. 面试高频问法

| 问题 | 回答要点 |
|---|---|
| 什么时候上 K8s？ | 多副本/弹性/HA 需求出现时；小规模别过度设计 |
| 探针三种类型？ | startup/readiness/liveness，各管各的 |
| HPA 怎么设计？ | 按 QPS 自定义指标 + 预留余量 + 冷却 |
| NetworkPolicy？ | default-deny + 显式允许（安全基线） |
| 有状态服务怎么办？ | StatefulSet + 持久卷（向量库/DB） |
| 发布怎么管？ | Helm Chart + 滚动更新 + 回滚预案 |
| 容灾怎么做？ | 跨区域 + 多供应商 + 季度演练 |

### 面试加分表达

> "RAG 上 K8s 的关键不是部署，是运维语义：探针保证流量只进健康的 Pod，HPA 按自定义 QPS 指标扩缩（预留 30% 余量），NetworkPolicy 让 Embedding/向量库默认拒绝外部访问。模型版本用 Helm values 管理，升级走金丝雀。"

## 9. K8s 部署常见坑

| 坑 | 表现 | 对策 |
|---|---|---|
| 探针误杀慢启动 | 模型加载慢被 liveness 重启 | startupProbe 长超时 |
| HPA 抖动 | 扩缩震荡 | 冷却时间 + 余量 |
| ConfigMap 不生效 | 改配置没变化 | 滚动重启触发重新加载 |
| 命名空间隔离失效 | 未配 NetworkPolicy | default-deny 是基线 |
| GPU Pod 调度失败 | 无可用 GPU 节点 | 节点池规划 + 亲和 |
| 有状态服务乱扩 | 数据分片错乱 | StatefulSet + 固定标识 |

### K8s 排障命令速查

```bash
# 看状态
kubectl get pods -n rag-platform
kubectl describe pod <pod>            # 事件（镜像拉取/探针失败）
# 看日志
kubectl logs <pod> --tail=100
kubectl logs <pod> --previous         # 上次崩溃的日志
# 看网络
kubectl get networkpolicy -A
# 看扩缩
kubectl get hpa
# 进入容器调试
kubectl exec -it <pod> -- sh
```

### 排障顺序

```
① 看 Pod 状态（Pending/CrashLoopBackOff/Error）
② describe 看事件（原因都在这里）
③ 日志（当前/previous）
④ 探针与资源（limits 是否太小）
⑤ 网络策略（是否被隔离拦了）
```

> 🎯 核心要点：K8s 解决"多副本、弹性、HA"三个问题——多命名空间隔离 + default-deny 是安全基线；探针保障"只把流量给健康 Pod"；HPA 按 QPS 自定义指标扩缩（预留余量）；Helm 管理版本与回滚；容灾做跨区域 + 多供应商 + 季度演练；排障顺序"状态→事件→日志→资源→网络"；判断"是否需要 K8s"看规模，别过度设计。

---

**下一模块**：[04-容量规划与高可用](04-容量规划与高可用.md) / **返回总览**：[00-阶段总览与学习路径](00-阶段总览与学习路径.md)