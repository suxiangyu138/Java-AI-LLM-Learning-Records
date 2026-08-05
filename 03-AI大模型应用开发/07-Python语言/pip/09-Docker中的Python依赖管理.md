# 09 - Docker 中的 Python 依赖管理

> 🎯 Docker 镜像中的 Python 依赖决定了部署的成败——镜像大小、构建速度、缓存命中率、安全性，都取决于你如何写 Dockerfile

---

## 目录

1. [Dockerfile 最佳实践](#1-dockerfile-最佳实践)
2. [AI 项目 Dockerfile 模板](#2-ai-项目-dockerfile-模板)
3. [多阶段构建](#3-多阶段构建)
4. [镜像体积优化](#4-镜像体积优化)

---

## 1. Dockerfile 最佳实践

```dockerfile
# ❌ 坏的实践
FROM python:3.12
COPY . .
RUN pip install -r requirements.txt    # 每次代码变动都重装依赖！

# ✅ 好的实践：利用 Docker 层缓存
FROM python:3.12-slim

WORKDIR /app

# 1. 先拷贝依赖文件（代码不常变，依赖更不常变）
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 2. 再拷贝代码（代码经常变，但 pip install 层已缓存）
COPY . .

CMD ["python", "main.py"]
```

### Dockerfile 层缓存原理

```text
Docker 按顺序构建层，每层有缓存：
  1. FROM python:3.12-slim        ← 缓存 ✅
  2. COPY requirements.txt .      ← 文件没变 → 缓存 ✅
  3. RUN pip install ...           ← 复用缓存 ✅ (秒级！)
  4. COPY . .                     ← 代码变了 → 缓存失效 ❌
  5. CMD ...

关键：把不常变的放前面！依赖 > 配置 > 代码
```

## 2. AI 项目 Dockerfile 模板

```dockerfile
# AI LLM 项目 Dockerfile
FROM nvidia/cuda:12.1.0-runtime-ubuntu22.04

# 系统依赖
RUN apt-get update && apt-get install -y --no-install-recommends \
    python3.11 python3-pip git && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Python 依赖（优先缓存）
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 模型文件（如果在镜像里，可选）
# COPY models/ /app/models/

# 代码
COPY . .

# 非 root 用户
RUN useradd -m appuser && chown -R appuser /app
USER appuser

EXPOSE 8000
CMD ["python3", "-m", "uvicorn", "api:app", "--host", "0.0.0.0", "--port", "8000"]
```

### 开发 vs 生产 Dockerfile 分离

```dockerfile
# Dockerfile.dev — 开发环境
FROM python:3.12-slim
RUN pip install --no-cache-dir -r requirements-dev.txt
# 包含 pytest, black, debugpy 等

# Dockerfile — 生产环境
FROM python:3.12-slim
RUN pip install --no-cache-dir -r requirements.txt
# 只包含运行时依赖
```

## 3. 多阶段构建

```dockerfile
# 多阶段构建：编译阶段 + 运行阶段
# 最终镜像不包含编译工具，体积大幅减小

# ── Stage 1: Builder ──
FROM python:3.12 AS builder
WORKDIR /app
COPY requirements.txt .
# 编译 C 扩展的包
RUN pip install --no-cache-dir \
    --target=/app/deps \
    -r requirements.txt

# ── Stage 2: Runtime ──
FROM python:3.12-slim
WORKDIR /app
COPY --from=builder /app/deps /usr/local/lib/python3.12/site-packages/
COPY . .
CMD ["python", "main.py"]
```

## 4. 镜像体积优化

| 技巧 | 效果 | 做法 |
|------|:---:|------|
| **slim 镜像** | -500MB | `python:3.12-slim` 代替 `python:3.12` |
| **`--no-cache-dir`** | -200MB+ | `pip install --no-cache-dir` |
| **清理 apt 缓存** | -50MB | `rm -rf /var/lib/apt/lists/*` |
| **多阶段构建** | -100MB+ | 编译和运行分离 |
| **`.dockerignore`** | -不定 | 排除 `.venv/`, `__pycache__/`, `.git/` |

```text
# .dockerignore
.venv/
__pycache__/
*.pyc
.git/
.env
*.md
notebooks/
tests/
```

### 最终效果对比

```text
AI 项目镜像大小对比：

❌ 原始：python:3.12 + pip install + 缓存 + 源码 + git 历史
   → 3.5 GB（含 CUDA 镜像）

✅ 优化后：nvidia/cuda:12.1-runtime + slim + --no-cache-dir + .dockerignore
   → 2.1 GB（减 40%）
```

## 核心要点回顾

- Dockerfile 顺序是关键：依赖文件先 COPY → 代码后 COPY → 缓存命中率高
- AI 项目用 `nvidia/cuda` 基础镜像，不是 `python`
- 开发和生产分离 Dockerfile（开发含测试工具，生产只含运行时）
- 三个必备优化：slim 镜像 + `--no-cache-dir` + `.dockerignore`
- `docker run --gpus all` 启动 GPU 容器

## 参考资料

1. Docker 官方最佳实践 — docs.docker.com/develop/dev-best-practices
2. NVIDIA Container Toolkit — docs.nvidia.com/datacenter
