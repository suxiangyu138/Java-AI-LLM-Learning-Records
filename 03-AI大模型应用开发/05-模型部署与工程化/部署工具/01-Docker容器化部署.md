# 01 - Docker 容器化部署

> 🎯 Docker 是模型部署的第一课 — 解决环境一致性问题、一行命令拉起模型服务、GPU 直通零损耗

---

## 目录

1. [Dockerfile 最佳实践](#1-dockerfile-最佳实践)
2. [GPU 支持](#2-gpu-支持)
3. [Docker Compose 编排](#3-docker-compose-编排)
4. [镜像瘦身技巧](#4-镜像瘦身技巧)

---

## 1. Dockerfile 最佳实践

```dockerfile
# 模型推理服务 Dockerfile
FROM nvidia/cuda:12.4.0-runtime-ubuntu22.04

# ① 先装系统依赖（利用缓存）
RUN apt-get update && apt-get install -y \
    python3.11 python3-pip \
    && rm -rf /var/lib/apt/lists/*

# ② 再装 Python 依赖（分层缓存）
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# ③ 最后复制代码（经常变的放后面）
COPY ./app /app
WORKDIR /app

# ④ 非 root 用户
RUN useradd -m model && chown -R model /app
USER model

EXPOSE 8000
CMD ["python", "server.py"]
```

---

## 2. GPU 支持

```bash
# 安装 NVIDIA Container Toolkit
distribution=$(. /etc/os-release;echo $ID$VERSION_ID)
curl -s -L https://nvidia.github.io/nvidia-docker/gpgkey | apt-key add -
curl -s -L https://nvidia.github.io/nvidia-docker/$distribution/nvidia-docker.list | \
    tee /etc/apt/sources.list.d/nvidia-docker.list
apt-get update && apt-get install -y nvidia-container-toolkit

# 运行 GPU 容器
docker run --gpus all -p 8000:8000 my-model:latest

# 指定 GPU
docker run --gpus '"device=0,1"' -p 8000:8000 my-model:latest
```

---

## 3. Docker Compose 编排

```yaml
# docker-compose.yml — 完整 LLM 服务栈
version: '3.8'
services:
  # vLLM 推理服务
  llm:
    image: vllm/vllm-openai:latest
    runtime: nvidia
    environment:
      - NVIDIA_VISIBLE_DEVICES=all
    command: >
      --model Qwen/Qwen2-7B-Instruct
      --max-model-len 8192
    ports: ["8000:8000"]
    volumes:
      - model_cache:/root/.cache/huggingface
    deploy:
      resources:
        reservations:
          devices:
            - driver: nvidia
              count: 1
              capabilities: [gpu]

  # 向量数据库
  milvus:
    image: milvusdb/milvus:v2.4.0
    ports: ["19530:19530"]
    command: milvus run standalone

  # Redis 缓存
  redis:
    image: redis:7-alpine
    ports: ["6379:6379"]

volumes:
  model_cache:
```

---

## 4. 镜像瘦身技巧

| 技巧 | 做法 | 效果 |
|------|------|:---:|
| **多阶段构建** | 构建和运行分离 | 减 50%+ |
| **Alpine 基础镜像** | 用 `python:3.11-alpine` | 减 80% |
| **--no-cache-dir** | pip install 不加缓存 | 减 100MB+ |
| **.dockerignore** | 排除 `__pycache__/.git` | 减杂项 |
| **清理 apt 缓存** | `rm -rf /var/lib/apt/lists/*` | 减 50MB+ |

```dockerfile
# 多阶段构建示例
FROM python:3.11 AS builder
COPY requirements.txt .
RUN pip install --user -r requirements.txt

FROM python:3.11-slim
COPY --from=builder /root/.local /root/.local
COPY ./app /app
CMD ["python", "/app/server.py"]
```

---

## 核心要点回顾

- Dockerfile：系统依赖→Python依赖→代码（利用缓存分层）
- GPU：nvidia-container-toolkit + `--gpus all`
- Compose：一行编排 LLM + Milvus + Redis
- 镜像瘦身：多阶段构建 + Alpine + 无缓存
