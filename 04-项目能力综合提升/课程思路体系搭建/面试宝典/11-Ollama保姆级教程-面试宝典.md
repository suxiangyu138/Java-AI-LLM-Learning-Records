# Ollama 面试宝典
> 基于课程大纲全面覆盖 Ollama 本地大模型部署、模型管理、API 调用、Spring AI 集成及多模态应用面试高频考点

## 目录
1. [一、基础概念速答（18题）](#一基础概念速答18题)
2. [二、深度原理剖析（12题）](#二深度原理剖析12题)
3. [三、实战场景题（10题）](#三实战场景题10题)
4. [四、手写代码题（8题）](#四手写代码题8题)
5. [五、系统设计题（5题）](#五系统设计题5题)
6. [六、常见坑点与最佳实践](#六常见坑点与最佳实践)
7. [七、面试回答模板（Top 5）](#七面试回答模板top-5)
8. [八、快速查漏补缺Checklist](#八快速查漏补缺checklist)

---

## 一、基础概念速答（18题）

### Q1: 什么是 Ollama？
> Ollama 是一个开源的大模型本地部署与管理平台，支持一键下载、运行和管理各类开源大模型（如 Llama、Mistral、DeepSeek、Qwen 等），提供 OpenAI 兼容的 REST API 接口。

### Q2: Ollama 的核心功能有哪些？
- **模型管理**：`ollama pull`、`ollama list`、`ollama rm`、`ollama cp`、`ollama show`
- **模型运行**：`ollama run <model>` 交互式对话
- **自定义模型**：通过 Modelfile 自定义模型配置
- **REST API**：原生 HTTP API + OpenAI 兼容接口
- **GPU 加速**：支持 NVIDIA CUDA 和 AMD ROCm

### Q3: Ollama 支持哪些模型格式？
| 模型家族 | 典型模型 | 参数量 |
|---------|---------|--------|
| Llama | Llama 3.x、Llama 2 | 7B/13B/70B/405B |
| Mistral | Mistral v0.3、Mixtral 8x7B | 7B/8x7B |
| Qwen | Qwen 2.5、Qwen 2 | 0.5B~72B |
| DeepSeek | DeepSeek-V2、DeepSeek-R1 | 7B~67B |
| Gemma | Gemma 2 | 2B/9B/27B |
| Phi | Phi-3/Phi-4 | 3.8B/14B |
| Code | CodeLlama、DeepSeek-Coder | 7B~34B |
| Embedding | nomic-embed-text、mxbai-embed-large | - |

### Q4: Ollama 如何安装？
- **Windows**：从 [ollama.com](https://ollama.com) 下载安装包
- **macOS**：`brew install ollama`
- **Linux**：`curl -fsSL https://ollama.com/install.sh | sh`
- **Docker**：`docker run -d --gpus all -v ollama:/root/.ollama -p 11434:11434 --name ollama ollama/ollama`

### Q5: Ollama 的默认端口是多少？
> 默认监听端口为 `11434`，可通过环境变量 `OLLAMA_HOST` 修改。

### Q6: 如何拉取并运行一个模型？
```bash
# 拉取模型
ollama pull qwen2.5:7b

# 运行模型交互式对话
ollama run qwen2.5:7b

# 直接发送提示
ollama run qwen2.5:7b "请解释什么是RAG"
```

### Q7: Ollama 的 Modelfile 是什么？
> Modelfile 是 Ollama 的模型定制配置文件，类似于 Dockerfile，用于定义模型的基础权重、系统提示词、运行参数、模板等。

```dockerfile
FROM qwen2.5:7b
SYSTEM "你是一个专业的Java面试官，请用中文回答技术问题。"
PARAMETER temperature 0.7
PARAMETER top_p 0.9
PARAMETER num_ctx 4096
TEMPLATE """
{{ if .System }}<|im_start|>system
{{ .System }}<|im_end|>
{{ end }}<|im_start|>user
{{ .Prompt }}<|im_end|>
<|im_start|>assistant
"""
```

### Q8: 如何创建自定义模型？
```bash
# 创建 Modelfile
echo 'FROM qwen2.5:7b
SYSTEM "你是一名资深算法工程师。"
PARAMETER temperature 0.3' > Modelfile

# 构建自定义模型
ollama create my-algo-assistant -f Modelfile

# 运行自定义模型
ollama run my-algo-assistant
```

### Q9: Ollama 的 REST API 有哪些核心端点？
| 端点 | 方法 | 功能 |
|------|------|------|
| `/api/generate` | POST | 生成完整响应 |
| `/api/chat` | POST | 聊天对话接口 |
| `/api/embed` | POST | 文本向量化 |
| `/api/create` | POST | 创建模型 |
| `/api/pull` | POST | 拉取模型 |
| `/api/push` | POST | 推送模型 |
| `/api/list` | GET | 列出本地模型 |
| `/api/tags` | GET | 列出模型标签 |
| `/api/delete` | DELETE | 删除模型 |
| `/api/copy` | POST | 复制模型 |
| `/api/show` | POST | 查看模型详情 |
| `v1/chat/completions` | POST | OpenAI 兼容接口 |
| `v1/embeddings` | POST | OpenAI 兼容向量接口 |
| `v1/models` | GET | OpenAI 兼容模型列表 |

### Q10: Ollama 的 OpenAI 兼容接口如何使用？
```bash
curl http://localhost:11434/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "model": "qwen2.5:7b",
    "messages": [
      {"role": "system", "content": "你是AI助手"},
      {"role": "user", "content": "你好"}
    ],
    "temperature": 0.7,
    "max_tokens": 2048
  }'
```

### Q11: Ollama 如何启用 GPU 加速？
- **NVIDIA CUDA**：默认支持，需安装 NVIDIA Driver + CUDA Toolkit
- **AMD ROCm**：Linux 下通过 ROCm 支持
- **验证 GPU 是否生效**：
  ```bash
  # 查看 ollama 服务日志
  # Windows: 任务管理器查看 GPU 使用率
  # Linux: nvidia-smi 查看显存占用
  ```
- **环境变量**：
  - `OLLAMA_CUDA_VISIBLE_DEVICES=0` 指定 GPU
  - `OLLAMA_NUM_PARALLEL=4` 并行请求数

### Q12: Ollama 的 Embedding 模型如何使用？
```bash
# 拉取嵌入模型
ollama pull nomic-embed-text

# API 调用
curl http://localhost:11434/api/embed \
  -H "Content-Type: application/json" \
  -d '{
    "model": "nomic-embed-text",
    "input": "什么是向量数据库？"
  }'

# OpenAI 兼容接口
curl http://localhost:11434/v1/embeddings \
  -H "Content-Type: application/json" \
  -d '{
    "model": "nomic-embed-text",
    "input": "什么是向量数据库？"
  }'
```

### Q13: Ollama 支持的量化格式有哪些？
| 量化格式 | 精度 | 显存占用（7B模型） | 质量损失 |
|---------|------|-------------------|---------|
| Q4_K_M | 4-bit | ~4.5GB | 极小 |
| Q5_K_M | 5-bit | ~5.5GB | 很小 |
| Q8_0 | 8-bit | ~8GB | 几乎无损 |
| F16 | 16-bit | ~14GB | 无损 |
| Q2_K | 2-bit | ~2.5GB | 明显 |

### Q14: Ollama 环境变量有哪些？
| 变量 | 作用 | 示例 |
|------|------|------|
| `OLLAMA_HOST` | 监听地址 | `0.0.0.0:11434` |
| `OLLAMA_MODELS` | 模型存储路径 | `D:\ollama_models` |
| `OLLAMA_NUM_PARALLEL` | 并行请求数 | `4` |
| `OLLAMA_KEEP_ALIVE` | 模型驻留时间 | `5m` |
| `OLLAMA_DEBUG` | 调试模式 | `1` |
| `OLLAMA_CUDA_VISIBLE_DEVICES` | 指定GPU | `0,1` |

### Q15: Ollama 如何配置跨域和远程访问？
> 💡 **提示**：生产环境需谨慎开启远程访问，建议配合反向代理使用认证。

```bash
# 允许所有来源访问
set OLLAMA_HOST=0.0.0.0:11434
set OLLAMA_ORIGINS=*

# Linux 环境变量写入
echo 'export OLLAMA_HOST=0.0.0.0:11434' >> ~/.bashrc
```

### Q16: Ollama 如何处理并发请求？
> Ollama 默认支持请求排队，可通过 `OLLAMA_NUM_PARALLEL` 控制并行度。每个模型加载到显存后，多个请求共享同一模型实例。请求队列采用 FIFO 策略，超时请求会被丢弃。

### Q17: Ollama 对比 llama.cpp 和 vLLM 的差异？
| 特性 | Ollama | llama.cpp | vLLM |
|------|--------|-----------|------|
| 定位 | 一站式模型管理平台 | 底层推理引擎 | 高性能推理引擎 |
| 安装复杂度 | 极低 | 中等 | 较高 |
| API 兼容 | OpenAI 兼容 | 基础 API | OpenAI 兼容 |
| GPU 加速 | CUDA/ROCm | CUDA/Metal | CUDA |
| PagedAttention | 不支持 | 不支持 | 支持 |
| 生产部署 | 适合开发/小规模 | 适合边缘设备 | 适合大规模生产 |
| 量化支持 | 内置多种量化 | GGUF 量化 | AWQ/GPTQ |
| 集群部署 | 不支持 | 不支持 | 支持多节点 |

### Q18: Ollama 如何查看模型信息和运行状态？
```bash
# 查看所有本地模型
ollama list

# 查看模型详细信息
ollama show qwen2.5:7b

# 查看运行中的模型
ollama ps

# 停止正在运行的模型（发送 SIGTERM）

# 查看服务日志
# Linux/Mac: journalctl -u ollama
# 或直接查看日志文件
```

---

## 二、深度原理剖析（12题）

### Q1: Ollama 的架构设计原理？
> 🎯 **核心要点**：Ollama 采用 C/S 架构，底层基于 llama.cpp 进行推理优化，通过 Go 语言编写的服务层管理模型生命周期。

- **服务层（Go）**：HTTP 服务器、模型管理、请求调度、生命周期管理
- **推理层（C/C++）**：基于 llama.cpp，支持 GGUF 格式模型文件
- **模型格式**：统一使用 GGUF（GPT-Generated Unified Format），包含权重量化、模型配置、分词器等
- **进程模型**：每个模型运行在独立子进程中，请求通过 stdin/stdout 通信

```
┌─────────────────┐     HTTP API      ┌──────────────────┐
│  客户端 (CLI/API) │ ──────────────> │  Ollama Server    │
└─────────────────┘                   │  (Go, Port:11434) │
                                       └────────┬─────────┘
                                                │ 进程管理
                                       ┌────────▼─────────┐
                                       │  llama.cpp 子进程  │
                                       │  (模型推理引擎)     │
                                       └──────────────────┘
```

### Q2: Ollama 的模型下载和存储机制是怎样的？
> Ollama 将模型分层下载，每层使用 SHA256 哈希校验，支持断点续传。模型存储在 `~/.ollama/models/` 目录。

- **Manifest 文件**：记录模型的各层信息（blob清单）
- **Blob 存储**：每层权重文件以 SHA256 哈希命名存储
- **模型来源**：默认从 Ollama 官方 Registry 下载，支持私有 Registry

### Q3: Ollama 的上下文窗口如何配置？
> 通过 Modelfile 的 `num_ctx` 参数或 API 的 `num_ctx` 字段控制。

```bash
# Modelfile 方式
PARAMETER num_ctx 8192

# API 方式
curl http://localhost:11434/api/chat \
  -d '{"model":"qwen2.5:7b","messages":[{"role":"user","content":"hello"}],"options":{"num_ctx":8192}}'

# OpenAI 兼容接口不支持直接设置 num_ctx，需通过 Ollama API
```

> ⚠️ **注意**：增大上下文窗口会显著增加显存占用，7B 模型在 4096 上下文约需 5GB 显存，32768 上下文约需 10GB+。

### Q4: Ollama 的 Keep-Alive 机制如何工作？
> 模型在无请求后不会立即卸载，而是保持驻留在显存中一段时间（默认 5 分钟），避免频繁加载/卸载带来的性能开销。

```bash
# 设置保持时间
export OLLAMA_KEEP_ALIVE=10m  # 10分钟
export OLLAMA_KEEP_ALIVE=-1   # 永久驻留
export OLLAMA_KEEP_ALIVE=0    # 请求后立即卸载
```

### Q5: Ollama 如何实现流式输出？
> Ollama 支持 Server-Sent Events (SSE) 流式输出，客户端可以逐 token 接收响应。

```python
import httpx
import json

# 流式聊天
with httpx.stream("POST", "http://localhost:11434/api/chat",
    json={
        "model": "qwen2.5:7b",
        "messages": [{"role": "user", "content": "写一首诗"}],
        "stream": True
    }, timeout=None) as response:
    for line in response.iter_lines():
        if line:
            data = json.loads(line)
            if not data.get("done"):
                print(data["message"]["content"], end="", flush=True)
```

### Q6: Ollama 的 Embedding 向量化原理？
> Ollama 的 Embedding 接口将输入文本通过模型的嵌入层转换为固定维度的向量表示，用于语义搜索和 RAG 场景。

- **模型选择**：推荐专用嵌入模型（如 `nomic-embed-text`、`mxbai-embed-large`）
- **维度**：`nomic-embed-text` 为 768 维，`mxbai-embed-large` 为 1024 维
- **归一化**：默认进行 L2 归一化
- **批处理**：支持批量文本向量化

### Q7: Ollama 的模板引擎（TEMPLATE）工作原理？
> TEMPLATE 使用 Go 模板语法，定义模型输入格式。不同的模型系列需要不同的模板格式来匹配其预训练时的对话格式。

```dockerfile
# ChatML 格式（Qwen、Phi 系列）
TEMPLATE """<|im_start|>system
{{ .System }}<|im_end|>
<|im_start|>user
{{ .Prompt }}<|im_end|>
<|im_start|>assistant
"""

# Llama 3 格式
TEMPLATE """<|begin_of_text|><|start_header_id|>system<|end_header_id|>
{{ .System }}<|eot_id|>
<|start_header_id|>user<|end_header_id|>
{{ .Prompt }}<|eot_id|>
<|start_header_id|>assistant<|end_header_id|>
"""
```

### Q8: Ollama 如何通过 Dify 集成实现知识库 RAG？
> ⚠️ **常见面试题**：Ollama + Dify 是中小企业构建私有知识库的经典方案。

```yaml
# Dify 中配置 Ollama 作为模型提供商
# 设置 -> 模型供应商 -> Ollama
配置项:
  - 模型名称: qwen2.5:7b
  - 服务器URL: http://192.168.1.100:11434
  - 模型类型: LLM / Embedding
  
# RAG 工作流程
1. 文档上传 -> 文本切片 (Chunk)
2. 向量化 (Ollama Embedding) -> 存入向量数据库
3. 用户提问 -> 向量化查询 -> 检索相似文档
4. 结合 Prompt -> LLM 生成回答
```

### Q9: Spring AI 集成 Ollama 的原理？
```java
// Spring Boot + Ollama 集成
// 1. 添加依赖
// build.gradle
implementation 'org.springframework.ai:spring-ai-ollama-spring-boot-starter'

// 2. 配置
// application.yml
spring:
  ai:
    ollama:
      base-url: http://localhost:11434
      chat:
        model: qwen2.5:7b
        options:
          temperature: 0.7
          top-p: 0.9
      embedding:
        model: nomic-embed-text

// 3. 使用 ChatClient
@Service
public class AIService {
    private final ChatClient chatClient;
    
    public AIService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }
    
    public String chat(String message) {
        return chatClient.prompt()
                .user(message)
                .call()
                .content();
    }
}
```

### Q10: RAGFlow 与 Ollama 集成的架构？
> RAGFlow 是一个开源的 RAG 引擎，深度的文档解析能力远超 Dify。结合 Ollama 可实现完全的本地化部署。

```yaml
# RAGFlow + Ollama 集成架构
┌─────────────┐     ┌──────────┐     ┌──────────┐
│   RAGFlow    │────>│  Ollama   │────>│  模型推理  │
│  (文档解析、   │     │ (API代理)  │     │ (本地GPU) │
│   检索排序)   │<────│          │<────│          │
└─────────────┘     └──────────┘     └──────────┘

# RAGFlow 的 Ollama 配置
# service_conf.yaml
ollama:
  api_base: http://localhost:11434
  llm_model: qwen2.5:7b
  embedding_model: nomic-embed-text
```

### Q11: Ollama 的多模态模型推理原理？
> Ollama 支持视觉多模态模型（如 LLaVA、Qwen-VL），通过将图像编码为视觉 token 并与文本 token 拼接输入到语言模型。

```bash
# 运行多模态模型
ollama pull llava:7b
ollama run llava:7b "描述这张图片的内容" /path/to/image.jpg

# API 调用
curl http://localhost:11434/api/chat \
  -H "Content-Type: application/json" \
  -d '{
    "model": "llava:7b",
    "messages": [
      {
        "role": "user",
        "content": "描述这张图片",
        "images": ["base64_encoded_image"]
      }
    ]
  }'
```

### Q12: Ollama 的并发处理与资源调度原理？
> Ollama 内部维护一个请求队列和模型池，每个模型对应一个独立的 llama.cpp 进程。请求进入队列后，Ollama Server 检查目标模型是否已加载，如未加载则启动新进程，已加载则直接转发请求。

```text
请求调度流程:
用户请求 -> HTTP Server -> 路由匹配 -> 模型检查
    ├── 模型已加载 -> 直接转发给 llama.cpp 进程
    └── 模型未加载 -> 加载新进程（加载时间：5-30秒）
                    -> 初始化上下文 -> 转发请求
请求完成 -> 返回响应 -> 根据 Keep-Alive 决定是否卸载
```

---

## 三、实战场景题（10题）

### 场景1: 本地私有化知识库搭建
**需求**：企业需要搭建内部私有知识库，要求完全本地化部署，不能将数据发送到云端。

**方案**：Ollama + Dify/RAGFlow + 向量数据库

```yaml
推荐技术栈:
  - Ollama: DeepSeek-R1 或 Qwen2.5 (7B/14B)
  - Embedding: nomic-embed-text
  - 知识库平台: Dify (社区版)
  - 向量数据库: Milvus 或 Qdrant
  - 文档类型: PDF/Word/Markdown/Excel
```

### 场景2: Spring Boot 集成 AI 聊天
**需求**：在现有 Java Spring Boot 项目中集成 AI 对话能力。

**方案**：Spring AI + Ollama

```java
// Chat 控制器
@RestController
@RequestMapping("/ai")
public class ChatController {
    @Autowired
    private ChatClient chatClient;
    
    @PostMapping("/chat")
    public String chat(@RequestBody ChatRequest request) {
        return chatClient.prompt()
            .user(u -> u.text(request.getMessage()))
            .call()
            .content();
    }
    
    @PostMapping("/chat/stream")
    public Flux<String> chatStream(@RequestBody ChatRequest request) {
        return chatClient.prompt()
            .user(request.getMessage())
            .stream()
            .content();
    }
}
```

### 场景3: 文本向量化与相似度搜索
**需求**：对产品文档进行语义搜索。

**方案**：Ollama Embedding API + 向量索引

```python
import requests
import numpy as np

def get_embedding(text):
    response = requests.post(
        "http://localhost:11434/api/embed",
        json={"model": "nomic-embed-text", "input": text}
    )
    return response.json()["embeddings"][0]

def cosine_similarity(a, b):
    return np.dot(a, b) / (np.linalg.norm(a) * np.linalg.norm(b))

# 示例
query_vec = get_embedding("如何退款")
doc_vecs = [get_embedding(doc) for doc in documents]
scores = [cosine_similarity(query_vec, dv) for dv in doc_vecs]
# 返回最相似的 Top-K 文档
```

### 场景4: 多模型切换与负载均衡
**需求**：根据不同任务使用不同模型（小模型处理简单问答，大模型处理复杂推理）。

```python
MODEL_MAP = {
    "simple": "qwen2.5:1.5b",   # 简单问答
    "code": "deepseek-coder:6.7b", # 代码生成
    "reasoning": "deepseek-r1:7b", # 复杂推理
    "embedding": "nomic-embed-text" # 向量化
}

def route_to_model(task_type: str, prompt: str):
    model = MODEL_MAP.get(task_type, "qwen2.5:7b")
    response = requests.post("http://localhost:11434/api/generate",
        json={"model": model, "prompt": prompt, "stream": False})
    return response.json()["response"]
```

### 场景5: 语音转文字 + 大模型处理
**需求**：语音输入 -> 语音识别 -> 大模型处理 -> 回复。

```bash
# 使用 Whisper 模型
ollama pull whisper-large-v3

# Python 调用示例
import whisper

model = whisper.load_model("large-v3")
result = model.transcribe("audio.mp3")
user_text = result["text"]

# 传给 Ollama LLM
response = requests.post("http://localhost:11434/api/generate",
    json={"model": "qwen2.5:7b", "prompt": user_text})
```

### 场景6: 多模态聊天数字人
**需求**：构建支持图像理解 + 语音交互的聊天数字人。

**方案**：Ollama (LLaVA/Qwen-VL) + 语音模型 + 前端展示

```python
# 多模态处理流程
def process_multimodal(image_path, user_text):
    # Step 1: 图像编码为 base64
    with open(image_path, "rb") as f:
        import base64
        image_base64 = base64.b64encode(f.read()).decode()
    
    # Step 2: 发送给多模态模型
    response = requests.post("http://localhost:11434/api/chat",
        json={
            "model": "llava:7b",
            "messages": [{
                "role": "user",
                "content": user_text,
                "images": [image_base64]
            }]
        })
    return response.json()["message"]["content"]
```

### 场景7: 模型量化与显存优化
**需求**：在有限显存（如 8GB）下运行 7B 模型。

```dockerfile
# Modelfile 优化
FROM qwen2.5:7b

# 降低上下文窗口
PARAMETER num_ctx 2048

# 降低批处理大小
PARAMETER num_batch 256

# 使用量化版本（GGUF 内部已量化）
# 拉取时选择 Q4_K_M 版本
# ollama pull qwen2.5:7b-q4_K_M
```

### 场景8: Docker 部署 Ollama 服务
```bash
# 带 GPU 支持的 Docker 部署
docker run -d \
  --gpus all \
  -v ollama_data:/root/.ollama \
  -v /mnt/models:/models \
  -p 11434:11434 \
  --name ollama \
  -e OLLAMA_KEEP_ALIVE=-1 \
  -e OLLAMA_NUM_PARALLEL=2 \
  ollama/ollama
```

### 场景9: OpenAI 兼容接口迁移
**需求**：从 OpenAI 迁移到本地 Ollama，最小化代码改动。

```python
# 原 OpenAI 代码
import openai
client = openai.OpenAI(api_key="sk-xxx", base_url="https://api.openai.com/v1")

# 修改后：只需改 base_url
client = openai.OpenAI(
    api_key="ollama",  # Ollama 不需要 key，但需要占位
    base_url="http://localhost:11434/v1"
)

# 代码无需其他改动
response = client.chat.completions.create(
    model="qwen2.5:7b",  # 只需改 model 名称
    messages=[{"role": "user", "content": "Hello"}]
)
```

### 场景10: ICL（上下文学习）在 Ollama 中的实现
> 💡 **面试关注点**：如何在聊天模板中实现 In-Context Learning。

```java
// Spring AI 中使用 ICL
@GetMapping("/icl-example")
public String iclExample() {
    return chatClient.prompt()
        .user(u -> u.text("""
            以下是几个情感分类的例子：
            文本：今天心情真好！ -> 正面
            文本：这太糟糕了。 -> 负面
            文本：一般般吧。 -> 中性
            
            请分类：这个产品太令人失望了。
            """))
        .call()
        .content();
}
```

---

## 四、手写代码题（8题）

### 题1: Ollama 聊天接口封装（Python）
```python
import requests
from typing import List, Dict, Optional

class OllamaClient:
    def __init__(self, base_url: str = "http://localhost:11434"):
        self.base_url = base_url
    
    def chat(self, model: str, messages: List[Dict], 
             stream: bool = False, **kwargs) -> Dict:
        payload = {
            "model": model,
            "messages": messages,
            "stream": stream,
            "options": kwargs
        }
        resp = requests.post(f"{self.base_url}/api/chat", json=payload)
        return resp.json()
    
    def embed(self, model: str, texts: List[str]) -> List[List[float]]:
        resp = requests.post(f"{self.base_url}/api/embed",
            json={"model": model, "input": texts})
        return resp.json()["embeddings"]
    
    def list_models(self) -> List[Dict]:
        resp = requests.get(f"{self.base_url}/api/tags")
        return resp.json()["models"]
```

### 题2: 流式聊天前端展示（JavaScript）
```javascript
async function streamChat(model, message) {
    const response = await fetch('http://localhost:11434/api/chat', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({
            model: model,
            messages: [{role: 'user', content: message}],
            stream: true
        })
    });
    
    const reader = response.body.getReader();
    const decoder = new TextDecoder();
    let fullContent = '';
    
    while (true) {
        const {done, value} = await reader.read();
        if (done) break;
        
        const lines = decoder.decode(value).split('\n');
        for (const line of lines) {
            if (line.trim()) {
                const data = JSON.parse(line);
                if (data.message?.content) {
                    fullContent += data.message.content;
                    // 更新 UI
                    document.getElementById('output').textContent = fullContent;
                }
            }
        }
    }
}
```

### 题3: RAG 检索增强生成实现
```python
import numpy as np
from typing import List

class SimpleRAG:
    def __init__(self, ollama_url: str = "http://localhost:11434"):
        self.ollama_url = ollama_url
        self.documents = []
        self.embeddings = []
    
    def add_documents(self, docs: List[str]):
        resp = requests.post(f"{self.ollama_url}/api/embed",
            json={"model": "nomic-embed-text", "input": docs})
        self.documents.extend(docs)
        self.embeddings.extend(resp.json()["embeddings"])
    
    def retrieve(self, query: str, k: int = 3) -> List[str]:
        resp = requests.post(f"{self.ollama_url}/api/embed",
            json={"model": "nomic-embed-text", "input": [query]})
        q_emb = resp.json()["embeddings"][0]
        
        scores = [np.dot(q_emb, doc_emb) 
                 for doc_emb in self.embeddings]
        top_k = np.argsort(scores)[-k:][::-1]
        return [self.documents[i] for i in top_k]
    
    def generate(self, query: str) -> str:
        context = "\n".join(self.retrieve(query))
        prompt = f"""基于以下信息回答问题：
        
信息：
{context}

问题：{query}

回答："""
        resp = requests.post(f"{self.ollama_url}/api/generate",
            json={"model": "qwen2.5:7b", "prompt": prompt, "stream": False})
        return resp.json()["response"]
```

### 题4: Spring Boot + Ollama 流式聊天
```java
@RestController
@RequestMapping("/api/chat")
public class StreamChatController {
    
    private final OllamaChatModel chatModel;
    
    // Spring AI 自动注入
    public StreamChatController(OllamaChatModel chatModel) {
        this.chatModel = chatModel;
    }
    
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamChat(@RequestParam String message) {
        Prompt prompt = new Prompt(new UserMessage(message));
        
        return chatModel.stream(prompt)
            .map(response -> {
                String content = response.getResult().getOutput().getContent();
                return ServerSentEvent.<String>builder()
                    .data(content)
                    .build();
            });
    }
}
```

### 题5: Ollama API 健康检查与模型自动切换
```python
import time
import requests
from typing import Optional

class OllamaHealthChecker:
    def __init__(self, base_url: str = "http://localhost:11434"):
        self.base_url = base_url
        self.primary_model = "qwen2.5:7b"
        self.fallback_model = "qwen2.5:1.5b"
    
    def check_health(self) -> bool:
        try:
            resp = requests.get(f"{self.base_url}/api/tags", timeout=5)
            return resp.status_code == 200
        except:
            return False
    
    def get_gpu_memory(self) -> Optional[int]:
        """检查 GPU 可用显存（需安装 nvidia-ml-py3）"""
        try:
            import pynvml
            pynvml.nvmlInit()
            handle = pynvml.nvmlDeviceGetHandleByIndex(0)
            info = pynvml.nvmlDeviceGetMemoryInfo(handle)
            return info.free // (1024**2)  # MB
        except:
            return None
    
    def auto_select_model(self) -> str:
        if not self.check_health():
            raise Exception("Ollama 服务不可用")
        
        free_mem = self.get_gpu_memory()
        if free_mem and free_mem < 4096:  # 小于 4GB
            return self.fallback_model
        return self.primary_model
```

### 题6: OpenAI SDK 兼容层适配
```python
from openai import OpenAI
import os

class OllamaOpenAIAdapter:
    """使用 OpenAI SDK 调用 Ollama"""
    
    def __init__(self, model: str = "qwen2.5:7b"):
        self.client = OpenAI(
            api_key="ollama",  # 占位
            base_url="http://localhost:11434/v1"
        )
        self.model = model
    
    def chat_completion(self, messages: list, **kwargs):
        return self.client.chat.completions.create(
            model=self.model,
            messages=messages,
            **kwargs
        )
    
    def embeddings(self, input_text: str):
        return self.client.embeddings.create(
            model="nomic-embed-text",
            input=input_text
        )

# 使用示例
adapter = OllamaOpenAIAdapter()
response = adapter.chat_completion([
    {"role": "system", "content": "你是AI助手"},
    {"role": "user", "content": "你好"}
])
print(response.choices[0].message.content)
```

### 题7: 多轮对话历史管理（Java）
```java
import java.util.*;

public class ConversationManager {
    private final Map<String, List<Map<String, String>>> sessions = new HashMap<>();
    private static final int MAX_HISTORY = 20;
    
    public void addMessage(String sessionId, String role, String content) {
        sessions.computeIfAbsent(sessionId, k -> new ArrayList<>());
        List<Map<String, String>> history = sessions.get(sessionId);
        
        Map<String, String> message = new HashMap<>();
        message.put("role", role);
        message.put("content", content);
        history.add(message);
        
        // 限制历史长度
        if (history.size() > MAX_HISTORY * 2) {
            history.subList(0, history.size() - MAX_HISTORY * 2).clear();
        }
    }
    
    public List<Map<String, String>> getHistory(String sessionId) {
        return sessions.getOrDefault(sessionId, new ArrayList<>());
    }
    
    public void clearSession(String sessionId) {
        sessions.remove(sessionId);
    }
}
```

### 题8: 批量文档向量化与索引构建
```python
import json
import numpy as np
from pathlib import Path

class DocumentIndexer:
    def __init__(self, ollama_url: str = "http://localhost:11434"):
        self.ollama_url = ollama_url
    
    def chunk_document(self, text: str, chunk_size: int = 512, overlap: int = 50):
        chunks = []
        start = 0
        while start < len(text):
            end = min(start + chunk_size, len(text))
            chunks.append(text[start:end])
            start += chunk_size - overlap
        return chunks
    
    def index_documents(self, doc_dir: str, output_file: str = "index.json"):
        index = {"documents": [], "embeddings": []}
        
        for file_path in Path(doc_dir).glob("*.md"):
            text = file_path.read_text(encoding="utf-8")
            chunks = self.chunk_document(text)
            
            for chunk in chunks:
                resp = requests.post(f"{self.ollama_url}/api/embed",
                    json={"model": "nomic-embed-text", "input": [chunk]})
                index["documents"].append({
                    "source": str(file_path),
                    "content": chunk
                })
                index["embeddings"].append(resp.json()["embeddings"][0])
        
        # 保存索引
        with open(output_file, "w") as f:
            json.dump(index, f)
        
        return len(index["documents"])
```

---

## 五、系统设计题（5题）

### 题1: 设计一个企业级私有 LLM 服务平台
**需求**：为 200+ 员工提供内部 AI 助手服务，涵盖文档问答、代码辅助、会议纪要等。

```yaml
架构设计:
  接入层:
    - Nginx 反向代理 + HTTPS
    - API Gateway（限流/鉴权/审计）
    
  服务层:
    - Ollama 集群（多节点负载均衡）
    - 模型路由（小型/大型模型分离）
    - Prompt 管理服务
    
  知识库层:
    - Dify/RAGFlow 作为 RAG 引擎
    - Milvus/Qdrant 向量数据库
    - PostgreSQL（元数据存储）
    
  应用层:
    - Web 管理后台
    - Slack/钉钉机器人
    - VS Code 插件（代码辅助）
    
  监控:
    - Prometheus + Grafana（GPU/内存/请求量）
    - 请求审计日志
    
模型规划:
  - DeepSeek-R1:7B（推理任务）
  - Qwen2.5:7B（通用对话）
  - nomic-embed-text（向量化）
  - Whisper（语音识别）
```

### 题2: 设计一个高可用的 Ollama 模型推理集群
**关键挑战**：多节点负载、模型一致性、故障转移。

```text
┌─────────┐     ┌─────────────┐     ┌──────────────┐
│ 客户端   │────>│ 负载均衡器    │────>│ Ollama Node 1 │
│         │     │ (haproxy/    │     ├──────────────┤
│         │     │  nginx)      │────>│ Ollama Node 2 │
└─────────┘     └─────────────┘     ├──────────────┤
                                     │ Ollama Node 3 │
                                     └──────────────┘

关键设计:
  - 每个节点部署相同模型（冗余）
  - 共享存储（NFS/S3）存放模型文件
  - 会话亲和性（Sticky Session）
  - 健康检查（/api/tags + GPU 状态）
  - 自动扩缩容（K8s + GPU Operator）
```

### 题3: 设计 RAG 知识库问答系统
> 🎯 **面试高频系统设计题**

```text
核心流程:
  1. 文档处理管道
     - 格式解析 (PDF/Word/HTML/图片OCR)
     - 文本切片 (Chunk Strategy: sliding window, semantic)
     - 向量化 (Ollama Embedding)
     - 元数据提取
  
  2. 检索策略
     - Dense Retrieval (向量相似度)
     - Keyword Retrieval (BM25)
     - Hybrid Search (加权融合)
     - Reranker (Cross-encoder 重排)
  
  3. 生成策略
     - Query Rewriting (查询重写)
     - Context Window Management
     - Citation (引用标注)
     - Hallucination Detection

系统指标:
  - Recall@5 > 90%
  - 端到端延迟 < 3s
  - 文档索引准确率 > 95%
```

### 题4: 多模态聊天机器人架构设计
```text
输入层: [文本] [图片] [语音] [视频]
          │      │       │       │
          ▼      ▼       ▼       ▼
处理层:  ┌────────────────────────────┐
         │  多模态输入路由器            │
         │  - 文本: 直接发送给 LLM      │
         │  - 图片: OCR + 图像描述      │
         │  - 语音: Whisper 转文本      │
         │  - 视频: 关键帧提取 + 描述   │
         └────────────────────────────┘
                      │
                      ▼
         ┌────────────────────────────┐
         │  大模型推理 (Ollama)         │
         │  - LLaVA/Qwen-VL(多模态)    │
         │  - DeepSeek(推理)           │
         └────────────────────────────┘
                      │
                      ▼
输出层: [文本回复] [语音合成] [图像生成]
```

### 题5: 大模型 API 网关设计
```text
API 网关功能:
  ┌─────────────────────────────────────┐
  │           LLM API Gateway            │
  ├─────────────────────────────────────┤
  │  1. 模型路由                          │
  │     - 请求级别: prompt 复杂度分析       │
  │     - 用户级别: 按用户等级分配模型       │
  │     - 成本级别: 按 token 预算路由       │
  │                                       │
  │  2. 流量控制                          │
  │     - Token 级限流 (token/s)           │
  │     - 请求级限流 (QPS)                 │
  │     - 并发控制 (semaphore)             │
  │     - 退避策略 (exponential backoff)   │
  │                                       │
  │  3. 缓存策略                          │
  │     - Semantic Cache (语义缓存)        │
  │     - Exact Cache (精确缓存)           │
  │     - Cache invalidation              │
  │                                       │
  │  4. 监控审计                          │
  │     - Token 用量统计                   │
  │     - 响应质量评分                     │
  │     - 成本核算                         │
  └─────────────────────────────────────┘
```

---

## 六、常见坑点与最佳实践

| 坑点 | 原因 | 解决方案 |
|------|------|---------|
| 显存不足（OOM） | 模型过大或上下文设置过长 | 使用量化模型（Q4_K_M）；减小 `num_ctx`；使用 `OLLAMA_NUM_PARALLEL=1` |
| 模型加载慢 | Ollama 需要加载完整的模型权重到 GPU | 设置 `OLLAMA_KEEP_ALIVE=-1` 避免反复加载 |
| 中文回答质量差 | 使用了不支持中文的模型 | 使用 Qwen、DeepSeek 等中文优化模型 |
| API 调用报错 404 | 使用了错误的 API 路径 | Ollama API 路径为 `/api/chat`，OpenAI 兼容为 `/v1/chat/completions` |
| 线程安全问题 | 多线程同时修改会话历史 | 使用 `ConcurrentHashMap` 或 `synchronized` 保护会话 |
| Docker 中无法使用 GPU | 未配置 `--gpus all` 或者 NVIDIA Container Toolkit 未安装 | 安装 `nvidia-container-toolkit`；添加 `--gpus all` |
| Windows 路径包含中文 | Ollama 路径中有中文导致模型加载失败 | 设置 `OLLAMA_MODELS` 为纯英文路径 |
| 流式响应中断 | 客户端读取超时或网络不稳定 | 设置超时时间为 0（不超时）；实现重试机制 |
| Embedding 维度不匹配 | 不同模型输出的向量维度不同 | 统一使用同一个 Embedding 模型；检查向量数据库 schema |
| Modelfile 修改不生效 | 未重新创建模型 | 修改 Modelfile 后需要执行 `ollama create` 重新构建 |

> 💡 **最佳实践总结**：
> 1. 生产环境始终使用反向代理（Nginx）加 HTTPS
> 2. 根据任务类型选择合适大小的模型，避免"杀鸡用牛刀"
> 3. 监控 GPU 显存和温度，防止过热降频
> 4. 使用 `ollama ps` 定期检查运行模型状态
> 5. 为 Embedding 模型建立缓存层，减少重复计算

---

## 七、面试回答模板（Top 5）

### 模板1：Ollama 原理介绍（适合"请介绍一下你了解的 LLM 部署工具"）
> "Ollama 是一个开源的大模型本地部署管理平台，它基于 llama.cpp 作为底层推理引擎，通过 Go 语言封装了模型管理、API 服务和进程调度能力。它的核心优势在于：第一，一键安装和模型管理，像 Docker 一样拉取和运行模型；第二，提供 OpenAI 兼容的 REST API，可实现零代码迁移；第三，通过 Modelfile 支持模型自定义配置。在项目中我们使用 Ollama 部署了 Qwen2.5 和 DeepSeek 等模型，结合 Dify 搭建了企业内部知识库系统。"

### 模板2：Ollama + Spring AI 集成（适合"Java 项目如何集成大模型"）
> "在 Spring Boot 项目中，我们使用 Spring AI 框架集成 Ollama。Spring AI 提供了类似 Spring Data 的抽象层，通过 `spring-ai-ollama-spring-boot-starter` 可以快速配置。具体实现上，我们使用 `ChatClient` 进行对话交互，支持同步和流式两种模式。对于 RAG 场景，我们通过 `EmbeddingClient` 进行文本向量化，结合向量数据库实现语义检索。Spring AI 的抽象设计使得切换底层模型提供商（从 Ollama 切换到 OpenAI）仅需修改配置，业务代码无需改动。"

### 模板3：RAG 系统设计（适合"请设计一个知识库问答系统"）
> "基于 Ollama 的 RAG 系统包含四个核心环节：文档处理、向量化、检索和生成。文档处理阶段使用 LangChain 或 Dify 对 PDF、Word 等格式进行解析和切片；向量化阶段通过 Ollama 的 Embedding 模型将文本转为向量并存入 Milvus；检索阶段采用向量相似度 + 关键词 BM25 的混合检索，然后通过 Reranker 进行精排；生成阶段将检索到的文档片段作为上下文注入 Prompt，由 LLM 生成带有引用的回答。这套方案完全本地化部署，保障了数据安全。"

### 模板4：多模态应用（适合"如何实现多模态 AI 应用"）
> "Ollama 支持 LLaVA、Qwen-VL 等多模态模型，能够同时理解文本和图像。实现多模态应用的关键在于：使用 Modelfile 配置多模态模型的模板格式，在 API 调用时将图片进行 base64 编码后传入 `images` 字段。我的一个实践是构建了多模态聊天数字人，结合 Ollama + Whisper 语音识别 + 语音合成，支持语音输入、图像理解和文本回复的完整交互流程。"

### 模板5：Ollama 生产部署（适合"Ollama 如何用于生产环境"）
> "Ollama 生产部署需要注意四个方面：第一，资源管理 -- 根据模型大小和量化级别合理分配 GPU 显存，通过 `OLLAMA_NUM_PARALLEL` 控制并发；第二，高可用 -- 多节点部署 + Nginx 负载均衡，节点间共享模型存储；第三，监控告警 -- 对接 Prometheus 监控 GPU 使用率、请求延迟和错误率；第四，安全 -- 使用反向代理增加认证和 HTTPS，限制 API 访问来源。对于面向外部用户的服务，建议使用 vLLM 等专用推理框架替代 Ollama。"

---

## 八、快速查漏补缺Checklist

- [ ] 知道 Ollama 的安装方式和基本命令（pull/list/run/rm/cp/show/ps）
- [ ] 理解 Modelfile 的 FROM/SYSTEM/PARAMETER/TEMPLATE 四个核心指令
- [ ] 能手写一个完整的 Modelfile 并创建自定义模型
- [ ] 掌握 `/api/chat`、`/api/generate`、`/api/embed` 三个核心 API 的调用方式
- [ ] 了解 OpenAI 兼容接口的 base_url 和模型名称配置
- [ ] 知道如何配置 GPU 加速和 CUDA 相关环境变量
- [ ] 理解 Embedding 模型在 RAG 中的作用和调用方式
- [ ] 能设计 Spring AI + Ollama 的集成架构
- [ ] 知道 Dify/RAGFlow 与 Ollama 的集成方案
- [ ] 了解量化格式（Q4_K_M/Q5_K_M/Q8_0/F16）的内存和精度 trade-off
- [ ] 掌握流式请求的实现方式（SSE）
- [ ] 能回答 Ollama vs llama.cpp vs vLLM 的对比
- [ ] 知道如何处理并发请求和 Keep-Alive 机制
- [ ] 能设计 RAG 知识库系统的完整架构
- [ ] 了解多模态模型的 API 调用方式（图片 base64 编码）
- [ ] 知道常见坑点（显存 OOM、中文支持、路径中文等）和解决方案

---

> 🎯 **总结**：Ollama 是 LLM 本地部署的"入口级"工具，面试重点在于 API 调用、Modelfile 定制、与 Spring AI/Dify/RAGFlow 的集成方案以及 RAG 系统架构设计。掌握这些知识点足以应对 90% 以上的 Ollama 相关面试题。
