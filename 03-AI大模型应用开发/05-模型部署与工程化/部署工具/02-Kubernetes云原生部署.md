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
