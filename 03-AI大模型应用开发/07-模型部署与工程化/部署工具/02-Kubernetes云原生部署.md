# 02 - Kubernetes 云原生部署

> 🎯 K8s 是 LLM 服务走向大规模、高可用的必经之路 — GPU 调度、HPA 自动扩缩、滚动更新零停机

---

## 目录

1. [GPU 节点配置](#1-gpu-节点配置)
2. [Deployment 与 Service](#2-deployment-与-service)
3. [HPA 自动扩缩](#3-hpa-自动扩缩)
4. [滚动更新与回滚](#4-滚动更新与回滚)
5. [Helm 部署](#5-helm-部署)

---

## 1. GPU 节点配置

```bash
# 安装 NVIDIA Device Plugin
kubectl create -f https://raw.githubusercontent.com/NVIDIA/k8s-device-plugin/v0.14.0/nvidia-device-plugin.yml

# 验证 GPU 可用
kubectl describe node gpu-node | grep nvidia.com/gpu
# nvidia.com/gpu: 2  ← 该节点有 2 张 GPU
```

---

## 2. Deployment 与 Service

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: llm-inference
spec:
  replicas: 2
  selector:
    matchLabels:
      app: llm-inference
  template:
    spec:
      containers:
      - name: vllm
        image: vllm/vllm-openai:latest
        args: ["--model", "Qwen/Qwen2-7B", "--max-model-len", "8192"]
        ports:
        - containerPort: 8000
        resources:
          limits:
            nvidia.com/gpu: 1      # 每个 Pod 一张 GPU
            memory: "16Gi"
          requests:
            nvidia.com/gpu: 1
            memory: "12Gi"
        readinessProbe:             # 就绪探针
          httpGet:
            path: /health
            port: 8000
          initialDelaySeconds: 60
---
apiVersion: v1
kind: Service
metadata:
  name: llm-service
spec:
  selector:
    app: llm-inference
  ports:
  - port: 8000
    targetPort: 8000
  type: ClusterIP
```

---

## 3. HPA 自动扩缩

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: llm-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: llm-inference
  minReplicas: 2
  maxReplicas: 8
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
```

---

## 4. 滚动更新与回滚

```yaml
# Deployment 中配置更新策略
spec:
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1        # 最多多出 1 个 Pod
      maxUnavailable: 0  # 最少保持 0 个不可用

# 更新
kubectl set image deployment/llm-inference vllm=vllm/vllm-openai:v0.6.0

# 查看更新状态
kubectl rollout status deployment/llm-inference

# 回滚
kubectl rollout undo deployment/llm-inference
```

---

## 5. Helm 部署

```bash
# 安装 vLLM Helm Chart
helm repo add vllm https://vllm.github.io/charts
helm install llm vllm/vllm \
  --set model.name=Qwen/Qwen2-7B \
  --set gpu.count=2 \
  --set autoscaling.enabled=true \
  --set autoscaling.minReplicas=2
```

---

## 核心要点回顾

- GPU 调度：nvidia-device-plugin + `nvidia.com/gpu` 资源声明
- HPA：基于 CPU/内存自动扩缩，LLM 建议基于 GPU 利用率
- 滚动更新：`maxSurge=1, maxUnavailable=0` 保证零停机
- Helm：一键部署 LLM 服务栈

---

## 6. GPU 调度与 LLM 部署速查

**K8s GPU 调度三件套**：

```text
① nvidia-device-plugin：节点 GPU 资源上报（nvidia.com/gpu）
   → 部署为 DaemonSet，让 K8s 能调度 GPU
② Pod 声明：resources.limits: nvidia.com/gpu: 1
③ 多卡：requests/limits 设 2/4/8（配合 vLLM 张量并行）

示例：
resources:
  limits:
    nvidia.com/gpu: 1      # 声明需要 1 张 GPU
    memory: 32Gi
```

**LLM 服务的 K8s 部署要点**：

| 要点 | 说明 |
|------|------|
| HPA 扩容 | **GPU 场景 HPA 需谨慎**（模型加载耗时，扩缩容滞后）——按 QPS 而非 CPU |
| 滚动更新 | 模型版本更新走滚动（recreate 策略避免双份显存） |
| 显存亲和 | 大模型单卡放不下 → 张量并行（多卡同 Pod 的 NDM 机制） |
| 模型存储 | PV 挂载模型（避免每次启动下载） |
| 节点选择 | nodeSelector: gpu-type（A100/H100 分池） |

> 🎯 **核心要点**：K8s GPU 部署 = "**device-plugin（资源上报）+ limits 声明 + PV 挂模型**"三件套——**GPU 场景的 HPA 别按 CPU 配**（模型加载慢，弹性滞后），滚动更新用 recreate 防显存双份。

---

## 7. Helm 与运维速查

**Helm 部署 LLM 服务的要点**：

```yaml
# values.yaml 关键配置
resources:
  limits:
    nvidia.com/gpu: 1      # GPU 声明
    memory: 64Gi
persistence:
  enabled: true            # 模型 PV 挂载
  size: 50Gi
autoscaling:
  enabled: false           # GPU 场景默认关 HPA（模型加载慢）
```

**运维常用命令**：

```bash
# 部署与更新
kubectl apply -f deployment.yaml          # 部署
kubectl rollout status deployment/llm     # 查看滚动状态
kubectl rollout undo deployment/llm       # 回滚

# 排查
kubectl get pods -o wide                  # 查看 Pod 与节点
kubectl describe pod <pod>                # 事件（含 GPU 调度失败原因）
kubectl logs <pod>                        # 日志
kubectl top nodes                         # 资源使用（GPU 需额外插件）

# 常见 GPU 调度失败排查
# ① 无可用 GPU 节点 → nodeSelector 检查
# ② 显存不足 → 同节点多 Pod 竞争（显存是硬限制）
# ③ device-plugin 未部署 → DaemonSet 检查
```

> 🎯 **核心要点**：K8s LLM 运维 = "**GPU 声明（limits）+ PV 挂模型 + 调度失败三查**"——**GPU 显存是硬限制**（不像 CPU 可超卖），调度失败排查三件套（节点/显存/插件）是标配。
