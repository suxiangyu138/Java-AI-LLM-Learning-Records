# 🚀 Ollama 实战：本地私有化部署开源大模型

> **核心摘要**：Ollama 是最简单的本地大模型部署工具，封装了模型下载、量化、推理、API 服务全流程。本文覆盖安装部署、模型管理、REST API/OpenAI 兼容接口、Modelfile 自定义模型、量化选型与生产环境 Docker Compose 部署。

> **前置阅读**：[[03-Ollama量化部署与性能优化]]、[[04-Ollama与Java Client集成]]

---

## 目录

1. [Ollama 是什么](#1-ollama-是什么)
2. [安装与快速开始](#2-安装与快速开始)
3. [API 服务](#3-api-服务)
4. [自定义模型](#4-自定义模型)
5. [量化格式理解](#5-量化格式理解)
6. [生产环境部署](#6-生产环境部署)
7. [选型决策](#7-选型决策)

---

## 1. Ollama 是什么

**Ollama** 是最简单的本地大模型部署工具，封装了模型下载、量化、推理、API 服务的全流程。

```
一行命令启动一个本地大模型：
ollama run qwen2.5:7b

等价于手动完成：
下载模型 → 量化加载 → 启动推理 → 暴露 API 接口
```

### 核心优势

- **一键部署**：无需手动处理模型文件和环境配置
- **数据安全**：所有推理在本地完成，数据不出域
- **OpenAI 兼容**：提供 `/v1` 兼容接口，方便代码切换
- **量化支持**：内置 GGUF 量化格式，降低硬件门槛
- **Java 生态**：通过 HTTP API 可被任何语言调用

---

## 2. 安装与快速开始

### 2.1 安装

```bash
# Linux / macOS
curl -fsSL https://ollama.com/install.sh | sh

# Windows
# 下载 OllamaSetup.exe: https://ollama.com/download

# Docker
docker run -d -v ollama:/root/.ollama -p 11434:11434 \
  --name ollama ollama/ollama
```

### 2.2 模型拉取与运行

```bash
# 拉取模型
ollama pull qwen2.5:7b          # 通义千问 7B（推荐中文）
ollama pull qwen2.5:1.5b        # 轻量版
ollama pull llama3.2:3b         # Meta Llama
ollama pull deepseek-r1:8b      # DeepSeek 推理模型

# 交互式对话
ollama run qwen2.5:7b

# 查看已安装模型
ollama list
```

### 2.3 常用参数

```bash
# 设置上下文窗口（默认 2048）
ollama run qwen2.5:7b
>>> /set parameter num_ctx 4096
>>> /set parameter temperature 0.7
>>> /set parameter  # 查看当前参数
```

---

## 3. API 服务

Ollama 启动后自动在 `localhost:11434` 提供 REST API。

### 3.1 对话 API（非流式）

```python
import requests

response = requests.post("http://localhost:11434/api/chat", json={
    "model": "qwen2.5:7b",
    "messages": [
        {"role": "system", "content": "你是 Java 技术专家"},
        {"role": "user", "content": "解释 Spring Bean 的生命周期"}
    ],
    "options": {
        "temperature": 0.3,
        "num_ctx": 4096,
    }
})
print(response.json()["message"]["content"])
```

### 3.2 对话 API（流式）

```python
import requests
import json

def stream_chat(model, prompt):
    response = requests.post("http://localhost:11434/api/chat", json={
        "model": model,
        "messages": [{"role": "user", "content": prompt}],
        "stream": True
    }, stream=True)

    for line in response.iter_lines():
        if line:
            data = json.loads(line)
            if "message" in data:
                print(data["message"]["content"], end="", flush=True)

stream_chat("qwen2.5:7b", "解释 MySQL 索引类型")
```

### 3.3 Embedding API

```python
resp = requests.post("http://localhost:11434/api/embeddings", json={
    "model": "nomic-embed-text",
    "prompt": "Spring Boot 自动配置原理"
})
vector = resp.json()["embedding"]
print(f"向量维度: {len(vector)}")
```

### 3.4 OpenAI SDK 调用

```python
from openai import OpenAI

client = OpenAI(
    base_url="http://localhost:11434/v1",
    api_key="ollama"  # 任意值即可
)

response = client.chat.completions.create(
    model="qwen2.5:7b",
    messages=[{"role": "user", "content": "Hello"}]
)
```

> **重点**：Ollama 的 OpenAI 兼容模式使得同一套代码可在本地模型和云端 API 之间无缝切换，只需修改 `base_url` 即可。

---

## 4. 自定义模型

### 4.1 Modelfile

```dockerfile
# Modelfile
FROM qwen2.5:7b

# 系统提示词
SYSTEM "你是 Java 后端技术专家，擅长 Spring Boot、MySQL、Redis 相关技术。"

# 参数
PARAMETER temperature 0.3
PARAMETER num_ctx 4096
PARAMETER top_p 0.9

# 自定义模板
TEMPLATE """{{ if .System }}<|im_start|>system
{{ .System }}<|im_end|>
{{ end }}<|im_start|>user
{{ .Prompt }}<|im_end|>
<|im_start|>assistant
"""
```

```bash
# 创建自定义模型
ollama create java-expert -f Modelfile

# 运行
ollama run java-expert
```

### 4.2 导入微调后的模型

```bash
# Modelfile
FROM ./my-finetuned-model

TEMPLATE """<|im_start|>system
{{ .System }}<|im_end|>
<|im_start|>user
{{ .Prompt }}<|im_end|>
<|im_start|>assistant
"""
```

---

## 5. 量化格式理解

| 格式 | 精度 | 显存（7B） | 质量损失 | 适用场景 |
|---|---|---|---|---|
| FP16 | 16-bit | ~14 GB | 无 | 服务器部署 |
| Q8_0 | 8-bit | ~7 GB | 极小 | 高性能本地推理 |
| **Q4_K_M** | **4-bit** | **~4 GB** | **小** | **消费级 GPU / Mac** |
| Q2_K | 2-bit | ~2.5 GB | 中等 | CPU 推理 / 低配设备 |
| GGUF | 可调 | 可变 | — | Ollama 默认格式 |

```bash
# 拉取指定量化版本
ollama pull qwen2.5:7b-q4_K_M   # 4-bit 量化（推荐）
ollama pull qwen2.5:7b-q8_0     # 8-bit 量化
```

---

## 6. 生产环境部署

### 6.1 Docker Compose（含 Open WebUI）

```yaml
# docker-compose.yml
version: "3.8"
services:
  ollama:
    image: ollama/ollama:latest
    ports:
      - "11434:11434"
    volumes:
      - ollama_data:/root/.ollama
    deploy:
      resources:
        reservations:
          devices:
            - driver: nvidia
              count: 1
              capabilities: [gpu]
    restart: unless-stopped

  open-webui:
    image: ghcr.io/open-webui/open-webui:main
    ports:
      - "3000:8080"
    environment:
      - OLLAMA_BASE_URL=http://ollama:11434
    volumes:
      - webui_data:/app/backend/data
    depends_on:
      - ollama

volumes:
  ollama_data:
  webui_data:
```

### 6.2 性能优化

```bash
# 设置并发请求数
export OLLAMA_NUM_PARALLEL=4

# 设置最大加载模型数
export OLLAMA_MAX_LOADED_MODELS=2

# GPU 层数控制
ollama run qwen2.5:7b
>>> /set parameter num_gpu 32
```

> **注意**：生产环境建议使用 Docker Compose 部署并开启 GPU 加速，配合 Open WebUI 提供可视化交互界面。

---

## 7. 选型决策

```
需要数据完全不出域？
├── 是 → Ollama 本地部署
│   ├── 有 GPU (8GB+) → Qwen2.5:7b-q4
│   ├── Mac (M1/M2/M3) → Qwen2.5:14b-q4
│   └── 纯 CPU → Qwen2.5:1.5b
└── 否 → 使用 DeepSeek API（成本低，效果好）
```

---

## 核心要点回顾

- Ollama 一行命令即可部署本地大模型，内置模型下载、量化、推理、API 全流程
- 提供 REST API 和 OpenAI 兼容接口，支持对话、流式输出、Embedding
- 量化选型推荐 `Q4_K_M`，7B 模型仅需 ~4GB 显存
- Modelfile 支持基于基础模型自定义系统提示词和参数
- Docker Compose + GPU 加速实现生产化部署

## 参考资料

1. Ollama 官方文档：https://ollama.com/docs
2. Ollama GitHub：https://github.com/ollama/ollama
3. Open WebUI：https://github.com/open-webui/open-webui
4. Ollama API 文档：https://github.com/ollama/ollama/blob/main/docs/api.md
