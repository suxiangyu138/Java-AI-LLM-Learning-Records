# 04 - Ollama 本地部署实战

> 🎯 Ollama = 个人开发者的 LLM 部署神器 — 一行命令启动模型、OpenAI 兼容 API、支持 GPU/Metal 加速

### 核心命令

```bash
ollama pull qwen2:7b        # 拉取模型
ollama run qwen2:7b          # 交互运行
ollama serve                  # 启动 API 服务 (端口 11434)
ollama list                   # 已下载模型
ollama rm qwen2:7b           # 删除模型
```

### Modelfile 自定义

```dockerfile
FROM qwen2:7b
PARAMETER temperature 0.7
PARAMETER num_ctx 8192
SYSTEM "你是 Java 后端专家，擅长 Spring Boot 和微服务。"
```

```bash
ollama create java-expert -f Modelfile
ollama run java-expert
```

### API 调用

```python
import requests, json
resp = requests.post("http://localhost:11434/api/generate",
    json={"model": "qwen2:7b", "prompt": "解释 Java 多态", "stream": False})
print(resp.json()["response"])
```

### Java 端调用

```java
HttpClient client = HttpClient.newHttpClient();
HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create("http://localhost:11434/api/generate"))
    .POST(BodyPublishers.ofString("""
        {"model":"qwen2:7b","prompt":"解释Java多态","stream":false}
    """))
    .build();
```

### 硬件需求

| 模型 | 量化 | 显存 | 推荐设备 |
|------|:---:|------|------|
| Qwen2-7B | Q4 | ~5GB | RTX 3060+ |
| Qwen2-7B | FP16 | ~15GB | RTX 4080+ |
| Qwen2-72B | Q4 | ~40GB | A100 / 双4090 |
| Qwen2-1.5B | Q4 | ~1.5GB | CPU |

> 详见 [Embedding/05-本地Embedding部署](../../01-大模型基础与Prompt工程/Embedding/05-本地Embedding部署-Ollama与BGE-M3.md)
