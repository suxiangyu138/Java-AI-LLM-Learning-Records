# Ollama 实战：本地私有化部署开源大模型

> **所属阶段**：阶段四 — 大模型微调与部署
> **前置知识**：命令行基础
> **核心目标**：一键部署本地大模型，实现数据不出域

---

## 1. 什么是 Ollama

Ollama 是最简单的本地大模型部署工具，封装了模型下载、量化、推理、API 服务。

```
一行命令启动一个本地大模型：
ollama run qwen2.5:7b

等价于手动完成：
下载模型 → 量化加载 → 启动推理 → 暴露 API 接口
```

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
ollama pull qwen2.5:7b          # 通义千问 7B (推荐中文)
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

# 设置温度
>>> /set parameter temperature 0.7

# 查看当前参数
>>> /set parameter
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

### 3.4 用 OpenAI SDK 调用

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

---

## 4. 自定义模型

### 4.1 Modelfile

```bash
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

| 格式 | 精度 | 显存 (7B) | 质量损失 | 适用场景 |
|------|------|-----------|----------|----------|
| FP16 | 16-bit | ~14 GB | 无 | 服务器部署 |
| Q8_0 | 8-bit | ~7 GB | 极小 | 高性能本地推理 |
| Q4_K_M | 4-bit | ~4 GB | 小 | 消费级 GPU / Mac |
| Q2_K | 2-bit | ~2.5 GB | 中等 | CPU 推理 / 低配设备 |
| GGUF | 可调 | 可变 | — | Ollama 默认格式 |

```bash
# 拉取指定量化版本
ollama pull qwen2.5:7b-q4_K_M   # 4-bit 量化
ollama pull qwen2.5:7b-q8_0     # 8-bit 量化
```

---

## 6. 生产环境部署

### 6.1 Docker Compose

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

# GPU 层数（控制 GPU 卸载量）
ollama run qwen2.5:7b
>>> /set parameter num_gpu 32  # 层数越大，GPU 利用率越高
```

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
