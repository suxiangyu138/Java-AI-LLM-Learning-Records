# 08 - Docker 与 K8s 容器化部署

> 🎯 将 Ollama/vLLM 部署到 Docker 和 K8s 是生产化的必经之路——GPU 透传、健康检查、资源限制、自动扩缩容，每一步都直接影响服务稳定性

> **前置阅读**：[[03-Ollama量化部署与性能优化]]、[[07-vLLM与llama-cpp对比实战]]

---

## 目录

1. [Docker Compose 一键部署](#1-docker-compose-一键部署)
2. [GPU 透传配置](#2-gpu-透传配置)
3. [K8s 生产部署](#3-k8s-生产部署)
4. [健康检查与优雅关闭](#4-健康检查与优雅关闭)
5. [CI/CD 流水线](#5-cicd-流水线)

---

## 1. Docker Compose 一键部署

### 1.1 Ollama + Open WebUI

```yaml
# docker-compose.yml
version: "3.8"

services:
  ollama:
    image: ollama/ollama:latest
    container_name: ollama
    ports:
      - "11434:11434"
    volumes:
      - ollama_data:/root/.ollama
    environment:
      - OLLAMA_KEEP_ALIVE=24h         # 模型常驻内存
      - OLLAMA_HOST=0.0.0.0
      - OLLAMA_NUM_PARALLEL=4         # 并行请求数
      - OLLAMA_MAX_LOADED_MODELS=2    # 最多加载模型数
    deploy:
      resources:
        reservations:
          devices:
            - driver: nvidia
              count: 1
              capabilities: [gpu]
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "ollama", "list"]
      interval: 30s
      timeout: 10s
      retries: 3

  open-webui:
    image: ghcr.io/open-webui/open-webui:main
    container_name: open-webui
    ports:
      - "3000:8080"
    volumes:
      - open_webui_data:/app/backend/data
    environment:
      - OLLAMA_BASE_URL=http://ollama:11434
    depends_on:
      ollama:
        condition: service_healthy
    restart: unless-stopped

volumes:
  ollama_data:
  open_webui_data:
```

```bash
# 启动
docker compose up -d

# 初始化模型
docker exec -it ollama ollama pull qwen2.5:7b

# 查看日志
docker compose logs -f ollama
```

### 1.2 vLLM 服务

```yaml
# docker-compose-vllm.yml
services:
  vllm:
    image: vllm/vllm-openai:latest
    container_name: vllm-server
    ports:
      - "8000:8000"
    volumes:
      - ~/.cache/huggingface:/root/.cache/huggingface
    command: >
      --model NousResearch/Hermes-3-Llama-3.1-8B
      --dtype auto
      --max-model-len 32768
      --gpu-memory-utilization 0.90
      --max-num-seqs 16
    deploy:
      resources:
        reservations:
          devices:
            - driver: nvidia
              count: 1
              capabilities: [gpu]
    restart: unless-stopped
```

## 2. GPU 透传配置

### 2.1 NVIDIA Container Toolkit

```bash
# 安装 nvidia-container-toolkit
# Ubuntu/Debian
curl -fsSL https://nvidia.github.io/libnvidia-container/gpgkey | \
  sudo gpg --dearmor -o /usr/share/keyrings/nvidia-container-toolkit-keyring.gpg

sudo apt-get install -y nvidia-container-toolkit
sudo nvidia-ctk runtime configure --runtime=docker
sudo systemctl restart docker

# 验证 GPU 可用
docker run --rm --gpus all nvidia/cuda:12.1-base nvidia-smi
```

### 2.2 多 GPU 配置

```yaml
# 指定 GPU 设备
deploy:
  resources:
    reservations:
      devices:
        - driver: nvidia
          device_ids: ["0", "1"]     # 使用 GPU 0 和 1
          capabilities: [gpu]

# 环境变量控制 tensor parallelism (vLLM)
environment:
  - CUDA_VISIBLE_DEVICES=0,1
command: >
  --tensor-parallel-size 2
  --model meta-llama/Llama-3.1-70B
```

## 3. K8s 生产部署

### 3.1 Ollama Deployment

```yaml
# ollama-k8s.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: ai-services
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ollama
  namespace: ai-services
spec:
  replicas: 1                # GPU 限制，通常单副本
  selector:
    matchLabels:
      app: ollama
  template:
    metadata:
      labels:
        app: ollama
    spec:
      containers:
        - name: ollama
          image: ollama/ollama:latest
          ports:
            - containerPort: 11434
          env:
            - name: OLLAMA_HOST
              value: "0.0.0.0"
            - name: OLLAMA_KEEP_ALIVE
              value: "24h"
            - name: OLLAMA_NUM_PARALLEL
              value: "4"
          resources:
            limits:
              nvidia.com/gpu: 1      # GPU 资源声明
              memory: "16Gi"
            requests:
              nvidia.com/gpu: 1
              memory: "12Gi"
          volumeMounts:
            - name: ollama-storage
              mountPath: /root/.ollama
          livenessProbe:
            httpGet:
              path: /
              port: 11434
            initialDelaySeconds: 30
            periodSeconds: 10
          readinessProbe:
            httpGet:
              path: /
              port: 11434
            initialDelaySeconds: 10
            periodSeconds: 5
      volumes:
        - name: ollama-storage
          persistentVolumeClaim:
            claimName: ollama-pvc
---
apiVersion: v1
kind: Service
metadata:
  name: ollama-service
  namespace: ai-services
spec:
  selector:
    app: ollama
  ports:
    - port: 11434
      targetPort: 11434
  type: ClusterIP
---
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: ollama-pvc
  namespace: ai-services
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 100Gi        # 模型文件很大，留足空间
```

### 3.2 Init Container 预加载模型

```yaml
initContainers:
  - name: model-downloader
    image: ollama/ollama:latest
    command:
      - /bin/sh
      - -c
      - |
        ollama serve &
        sleep 5
        ollama pull qwen2.5:7b
        ollama pull nomic-embed-text
        kill %1
    volumeMounts:
      - name: ollama-storage
        mountPath: /root/.ollama
```

## 4. 健康检查与优雅关闭

```yaml
# Ollama 健康检查
livenessProbe:
  httpGet:
    path: /
    port: 11434
  initialDelaySeconds: 60     # Ollama 启动慢
  periodSeconds: 15
  timeoutSeconds: 5
  failureThreshold: 3

readinessProbe:
  httpGet:
    path: /api/tags           # 模型列表可用 = 就绪
    port: 11434
  initialDelaySeconds: 30
  periodSeconds: 10
  failureThreshold: 3

# 优雅关闭
lifecycle:
  preStop:
    exec:
      command: ["/bin/sh", "-c", "sleep 15"]
      # 给 15 秒完成正在处理的请求
```

## 5. CI/CD 流水线

```yaml
# .github/workflows/deploy-ollama.yml
name: Deploy Ollama

on:
  push:
    branches: [main]
    paths:
      - 'Modelfile'
      - 'docker-compose.yml'

jobs:
  build-and-deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Build custom model
        run: |
          docker compose run --rm ollama ollama create my-model -f Modelfile

      - name: Deploy to server
        run: |
          docker compose up -d --force-recreate
          docker compose exec -T ollama ollama list
```

## 核心要点回顾

- Docker Compose 一文件搞定 Ollama + Open WebUI + GPU 透传
- GPU 透传需要 NVIDIA Container Toolkit + `deploy.resources.reservations.devices`
- K8s 部署核心：PersistentVolume (模型存储) + GPU resource limits + InitContainer (预加载)
- 健康检查：`/` 端点检查存活，`/api/tags` 端点检查就绪
- 优雅关闭：`preStop` hook 给 15s 完成请求，`OLLAMA_KEEP_ALIVE=24h` 避免频繁加载

## 参考资料

1. Ollama Docker 官方镜像 — hub.docker.com/r/ollama/ollama
2. NVIDIA Container Toolkit — docs.nvidia.com/datacenter/cloud-native
3. K8s GPU 调度文档 — kubernetes.io/docs/tasks/manage-gpus
