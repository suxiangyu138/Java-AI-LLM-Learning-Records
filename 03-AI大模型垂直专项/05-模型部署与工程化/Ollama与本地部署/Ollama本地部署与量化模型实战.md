# 🚀 Ollama 本地部署与量化模型实战

> **核心摘要**：Ollama 让本地部署大模型像 `docker pull` 一样简单。结合 GGUF 量化技术，7B 模型仅需 4GB 显存即可流畅运行。本文从安装部署、量化选型、Modelfile 自定义、Spring Boot 集成到生产化部署（Docker Compose + GPU 加速）提供完整实战指南。

> **前置阅读**：[[AI模型服务化部署全解析]]、[[Ollama + Java Client 核心知识点]]

---

## 目录

1. [Ollama 快速上手](#1-ollama-快速上手)
2. [模型量化基础](#2-模型量化基础)
3. [创建自定义模型（Modelfile）](#3-创建自定义模型modelfile)
4. [生产部署架构](#4-生产部署架构)
5. [性能优化](#5-性能优化)
6. [常见问题排查](#6-常见问题排查)

---

## 1. Ollama 快速上手

### 1.1 安装

```bash
# Linux / WSL
curl -fsSL https://ollama.com/install.sh | sh

# macOS
brew install ollama

# Windows
# 下载 .exe 安装包：https://ollama.com/download/windows
```

### 1.2 基础命令

```bash
# 拉取并运行模型
ollama run qwen2:7b          # Qwen2 7B（~4.4GB）
ollama run llama3:8b         # Llama3 8B（~4.7GB）
ollama run deepseek-coder    # DeepSeek Coder（~4.5GB）

# 查看已下载的模型
ollama list

# 删除模型
ollama rm qwen2:7b

# 启动服务（API 模式）
ollama serve
# 默认监听 http://localhost:11434
```

### 1.3 命令行对话

```bash
ollama run qwen2:7b "Java中HashMap和ConcurrentHashMap的区别？"
```

### 1.4 API 调用（OpenAI 兼容格式）

```python
from openai import OpenAI

client = OpenAI(
    base_url="http://localhost:11434/v1",  # Ollama 默认地址
    api_key="ollama"  # 本地不需要真实 key
)

response = client.chat.completions.create(
    model="qwen2:7b",
    messages=[{"role": "user", "content": "解释Java的GC机制"}],
    temperature=0.7,
    max_tokens=1024
)
print(response.choices[0].message.content)
```

### 1.5 流式 API 调用

```python
def chat_stream(prompt: str, model: str = "qwen2:7b"):
    stream = client.chat.completions.create(
        model=model,
        messages=[{"role": "user", "content": prompt}],
        stream=True
    )
    for chunk in stream:
        content = chunk.choices[0].delta.content or ""
        if content:
            yield content
```

---

## 2. 模型量化基础

### 2.1 什么是量化？

```
FP16（原始）：  每个参数 16 bits → 7B 模型 = 14 GB 显存
INT8（量化）：  每个参数 8 bits  → 7B 模型 = 7 GB 显存
INT4（量化）：  每个参数 4 bits  → 7B 模型 = 3.5 GB 显存

代价：精度微小损失（通常 < 1% 质量下降）
```

### 2.2 GGUF 量化级别

| 量化级别 | 大小（7B） | 质量 | 适用场景 |
|---|---|---|---|
| `q2_K` | ~2.9 GB | 明显下降 | 仅用于测试 |
| `q3_K_M` | ~3.5 GB | 可接受 | 资源极度受限 |
| **`q4_K_M`** | **~4.4 GB** | **几乎无损失** | **日常推荐** |
| `q5_K_M` | ~5.2 GB | 极佳 | 高质量要求 |
| `q8_0` | ~7.7 GB | 接近无损 | 生产环境 |
| `f16` | ~14 GB | 原始精度 | 显存充裕时 |

### 2.3 拉取特定量化版本

```bash
ollama run qwen2:7b-q4_K_M    # 推荐日常使用
ollama run qwen2:7b-q5_K_M    # 质量优先
ollama run qwen2:7b-q2_K      # 显存优先
```

> **重点**：`q4_K_M` 是精度与资源消耗的最佳平衡点，推荐作为日常使用的默认选择。

---

## 3. 创建自定义模型（Modelfile）

### 3.1 基础 Modelfile

```dockerfile
# Modelfile — 类似 Dockerfile，定义模型行为
FROM qwen2:7b

# 系统提示词
SYSTEM "你是一位资深的Java后端开发工程师，拥有10年微服务架构经验。请用中文回答，给出可直接使用的代码示例。"

# 调整参数
PARAMETER temperature 0.7
PARAMETER top_p 0.9
PARAMETER top_k 40
PARAMETER num_ctx 4096  # 上下文窗口大小

# 停止词
PARAMETER stop "<|im_end|>"
PARAMETER stop "<|im_start|>"
```

### 3.2 构建与运行

```bash
# 构建
ollama create java-expert -f Modelfile

# 运行
ollama run java-expert

# 导出为可分享的文件
ollama export java-expert -o java-expert.tar
```

### 3.3 从 GGUF 文件创建

```bash
# 1. 下载 GGUF 文件（从 HuggingFace）
# 2. 编写 Modelfile
cat > Modelfile << 'EOF'
FROM ./qwen2-7b-instruct-q4_k_m.gguf

TEMPLATE """<|im_start|>system
{{ .System }}<|im_end|>
<|im_start|>user
{{ .Prompt }}<|im_end|>
<|im_start|>assistant
"""

SYSTEM "You are a helpful assistant."
PARAMETER temperature 0.7
PARAMETER stop "<|im_end|>"
EOF

# 3. 创建模型
ollama create my-qwen -f Modelfile
```

---

## 4. 生产部署架构

### 4.1 Spring Boot + Ollama 集成

```yaml
# application.yml
ai:
  ollama:
    base-url: http://localhost:11434
    model: java-expert:latest
```

```java
@Service
public class OllamaService {

    private final RestClient restClient;

    public OllamaService(@Value("${ai.ollama.base-url}") String baseUrl) {
        this.restClient = RestClient.builder()
            .baseUrl(baseUrl)
            .build();
    }

    public String chat(String prompt, String model) {
        Map<String, Object> request = Map.of(
            "model", model,
            "messages", List.of(Map.of("role", "user", "content", prompt)),
            "stream", false,
            "options", Map.of("temperature", 0.7, "num_ctx", 4096)
        );

        Map response = restClient.post()
            .uri("/api/chat")
            .body(request)
            .retrieve()
            .body(Map.class);

        return (String) ((Map) response.get("message")).get("content");
    }

    // 流式接口（SSE）
    public Flux<String> chatStream(String prompt, String model) {
        Map<String, Object> request = Map.of(
            "model", model,
            "messages", List.of(Map.of("role", "user", "content", prompt)),
            "stream", true
        );

        return WebClient.builder()
            .baseUrl(baseUrl)
            .build()
            .post()
            .uri("/api/chat")
            .bodyValue(request)
            .retrieve()
            .bodyToFlux(Map.class)
            .map(m -> (String) ((Map) m.get("message")).get("content"));
    }
}
```

### 4.2 Docker Compose 全栈部署

```yaml
# docker-compose.yml
version: '3.8'
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
    entrypoint: ["/bin/sh", "-c"]
    command:
      - |
        ollama serve &
        sleep 3
        ollama pull qwen2:7b-q4_K_M
        tail -f /dev/null

  spring-app:
    image: my-app:latest
    ports:
      - "8080:8080"
    environment:
      AI_OLLAMA_BASE_URL: http://ollama:11434
    depends_on:
      - ollama

volumes:
  ollama_data:
```

---

## 5. 性能优化

### 5.1 GPU 加速

```bash
# 检查 GPU 是否可用
ollama run qwen2:7b --verbose
# Docker 环境需加 --gpus all
docker run --gpus all ollama/ollama
```

### 5.2 并发处理

```python
import asyncio
from concurrent.futures import ThreadPoolExecutor

class OllamaPool:
    """Ollama 连接池——并发处理多个请求"""

    def __init__(self, base_url: str = "http://localhost:11434", max_workers: int = 4):
        self.client = OpenAI(base_url=base_url + "/v1", api_key="ollama")
        self.executor = ThreadPoolExecutor(max_workers=max_workers)

    async def batch_chat(self, prompts: list[str], model: str = "qwen2:7b") -> list[str]:
        loop = asyncio.get_event_loop()
        tasks = [
            loop.run_in_executor(
                self.executor,
                lambda p=prompt: self.client.chat.completions.create(
                    model=model,
                    messages=[{"role": "user", "content": p}]
                ).choices[0].message.content
            )
            for prompt in prompts
        ]
        return await asyncio.gather(*tasks)
```

### 5.3 响应时间参考

| 模型 | GPU | 首 Token 延迟 | 生成速度 |
|---|---|---|---|
| Qwen2-7B-Q4 | RTX 3060 12GB | ~0.5s | ~60 tokens/s |
| Qwen2-7B-Q4 | RTX 4090 24GB | ~0.2s | ~120 tokens/s |
| Qwen2-7B-Q4 | CPU (M2) | ~2s | ~15 tokens/s |
| Qwen2-7B-Q4 | CPU (i7) | ~5s | ~5 tokens/s |

---

## 6. 常见问题排查

```bash
# 问题1：模型卡在加载中
ollama ps                     # 看模型是否在运行
ollama stop qwen2:7b          # 停止卡住的模型

# 问题2：Ollama 未启动
sudo systemctl status ollama  # Linux
brew services list | grep ollama  # macOS

# 问题3：显存不足
ollama run qwen2:7b-q2_K     # 换更小的量化版本

# 问题4：GPU 未识别
docker run --gpus all ollama/ollama  # 加 --gpus all

# 问题5：API 连接不上
curl http://localhost:11434/api/tags  # 测试 API 是否正常
```

---

## 快速调试检查清单

- [ ] `ollama serve` 是否正常运行？`curl http://localhost:11434/api/tags`
- [ ] 模型文件是否完整？显存是否足够加载？
- [ ] Docker 部署是否加了 `--gpus all`？
- [ ] Modelfile 的 TEMPLATE 格式是否匹配模型？
- [ ] `num_ctx` 设置是否合理？（默认 2048，大文档建议 8192+）

---

## 核心要点回顾

- Ollama 让本地大模型部署像 docker pull 一样简单，提供 OpenAI 兼容 API
- 量化级别 `q4_K_M` 是精度与资源的最佳平衡，7B 模型仅需 4.4GB
- Modelfile 支持自定义系统提示词和模型参数，类似 Dockerfile
- Spring Boot 通过 RestClient/WebClient 即可集成 Ollama
- Docker Compose 可实现 Ollama + Spring 应用的一键部署

## 参考资料

1. Ollama 官方文档：https://ollama.com/docs
2. Ollama GitHub：https://github.com/ollama/ollama
3. GGUF 量化说明：https://github.com/ggerganov/llama.cpp/blob/master/gguf-py/README.md
4. Spring AI Ollama：https://docs.spring.io/spring-ai/reference/api/chat/ollama-chat.html
