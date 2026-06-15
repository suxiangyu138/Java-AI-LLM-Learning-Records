# 🚀 Ollama + Java Client 核心知识点

> **核心摘要**：Ollama 是轻量级本地大模型运行工具，支持一行命令下载并运行 Llama、Qwen、Mistral 等开源模型。本文涵盖 Ollama 基础操作、Java 集成（原生 HTTP Client / LangChain4j / Spring AI）、Modelfile 自定义模型、量化优化与生产化部署。

> **前置阅读**：[[Ollama本地部署与量化模型实战]]、[[Ollama实战：本地私有化部署开源大模型]]

---

## 一、概述

**Ollama** 是一个轻量级的本地大模型运行工具，支持一行命令下载并运行 Llama、Qwen、Mistral 等开源模型。其核心定位为**本地私有化部署大模型的最简方案**，适用于数据不出内网的高隐私场景。

### Ollama 核心优势

- **一键运行**：下载即用，无需复杂环境配置
- **隐私保护**：数据不出本地，适合金融/医疗等敏感场景
- **离线可用**：下载后无需联网即可推理
- **OpenAI 兼容**：提供兼容接口，方便替换云端 API
- **Java 生态**：支持 LangChain4j、Spring AI 等框架集成

---

## 二、Ollama 基础

### 2.1 安装

```bash
# macOS / Linux 安装
curl -fsSL https://ollama.com/install.sh | sh

# Windows：下载安装包 https://ollama.com/download
```

### 2.2 模型管理

```bash
# 拉取模型
ollama pull qwen2.5:7b        # 通义千问 7B
ollama pull llama3:8b         # Meta Llama 3 8B
ollama pull deepseek-coder:6.7b  # DeepSeek 编码模型

# 列出本地模型
ollama list

# 运行模型（命令行交互）
ollama run qwen2.5:7b
```

### 2.3 常用模型推荐（2026 年）

| 模型 | 参数量 | 适合场景 | 内存需求 |
|---|---|---|---|
| **qwen2.5:7b** | 7B | 通用对话、代码生成 | 8GB+ |
| **qwen2.5:14b** | 14B | 复杂推理、高质量生成 | 16GB+ |
| **deepseek-coder:6.7b** | 6.7B | 代码生成专项 | 8GB+ |
| **llama3:8b** | 8B | 通用英文 / 中文 | 8GB+ |
| **codellama:7b** | 7B | Python / Java 代码 | 8GB+ |
| **mistral:7b** | 7B | 轻量高效英文 | 8GB+ |

---

## 三、Java 集成方式

### 3.1 原生 HTTP Client

Ollama 默认监听 `localhost:11434`，Java 可通过标准 HTTP Client 调用：

```java
HttpClient client = HttpClient.newHttpClient();

// 请求体
String body = """
    {
        "model": "qwen2.5:7b",
        "messages": [
            {"role": "user", "content": "用Java写一个快速排序"}
        ],
        "stream": false
    }
    """;

HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create("http://localhost:11434/api/chat"))
    .header("Content-Type", "application/json")
    .POST(HttpRequest.BodyPublishers.ofString(body))
    .build();

HttpResponse<String> response = client.send(request,
    HttpResponse.BodyHandlers.ofString());

// 解析响应
JsonNode result = objectMapper.readTree(response.body());
String content = result.get("message").get("content").asText();
```

### 3.2 流式响应

```java
String streamBody = """
    {
        "model": "qwen2.5:7b",
        "messages": [{"role": "user", "content": "解释JVM垃圾回收"}],
        "stream": true
    }
    """;

HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create("http://localhost:11434/api/chat"))
    .POST(HttpRequest.BodyPublishers.ofString(streamBody))
    .build();

// 流式读取
client.send(request, HttpResponse.BodyHandlers.ofLines())
    .body()
    .map(line -> objectMapper.readTree(line)
        .get("message").get("content").asText())
    .forEach(System.out::print);
```

### 3.3 通过 LangChain4j 调用

```java
ChatLanguageModel model = OllamaChatModel.builder()
    .baseUrl("http://localhost:11434")
    .modelName("qwen2.5:7b")
    .temperature(0.7)
    .build();

String answer = model.generate("解释 Spring Boot 自动配置原理");
```

### 3.4 通过 Spring AI 调用

```java
@Bean
public OllamaChatModel ollamaChatModel() {
    return OllamaChatModel.builder()
        .baseUrl("http://localhost:11434")
        .model("qwen2.5:7b")
        .build();
}
```

> **重点**：对于 Java 后端项目，推荐优先使用 LangChain4j 或 Spring AI 进行集成，它们提供了更高层的抽象，包括对话管理、工具调用等能力。

---

## 四、自定义模型（Modelfile）

通过 Modelfile 可以基于基础模型进行定制，设定系统提示词和运行时参数：

```dockerfile
# Modelfile：基于基础模型定制
FROM qwen2.5:7b

# 系统提示词
SYSTEM "你是一位资深Java后端开发专家，擅长Spring Boot、微服务、高并发"

# 温度参数
PARAMETER temperature 0.3

# 上下文长度
PARAMETER num_ctx 8192
```

```bash
ollama create java-expert -f Modelfile
ollama run java-expert
```

---

## 五、性能优化

### 5.1 量化模型

```bash
# 使用量化版本减少内存
ollama pull qwen2.5:7b-q4_0    # 4-bit 量化，约 4GB
ollama pull qwen2.5:7b-q8_0    # 8-bit 量化，约 7GB
```

### 5.2 并发配置

```bash
# 环境变量控制并发
export OLLAMA_NUM_PARALLEL=4   # 最大并发请求数
export OLLAMA_MAX_LOADED_MODELS=2  # 同时加载的模型数
```

### 5.3 GPU 加速

Ollama 自动检测 GPU 环境（CUDA / Metal），启动后可通过 `--verbose` 查看 GPU 使用率。

---

## 六、生产化部署

### 6.1 Docker 部署

```yaml
# docker-compose.yml
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

volumes:
  ollama_data:
```

### 6.2 生产考量

| 因素 | 说明 |
|---|---|
| **响应延迟** | 本地推理无网络延迟，但模型推理需 1-10 秒 |
| **吞吐量** | 单卡 7B 模型约 50-100 tokens/s |
| **高可用** | 部署多实例 + 前端负载均衡 |
| **监控** | 集成 Prometheus + Grafana 监控 GPU 使用率、请求延迟 |
| **模型更新** | 热重载新模型文件，无需重启服务 |

---

## 七、方案对比

| 方案 | 隐私性 | 延迟 | 成本 | 质量 |
|---|---|---|---|---|
| **Ollama 本地** | 最高 | 中 | 硬件成本 | 取决于模型 |
| **OpenAI API** | 低 | 低 | API 费用 | 最高 |
| **vLLM 自建** | 高 | 低 | 硬件+运维 | 取决于模型 |
| **阿里云/腾讯云** | 中 | 低 | API 费用 | 高 |

---

## 核心要点回顾

- Ollama 是本地私有化部署大模型的最简方案，适合数据不出内网的高隐私场景
- Java 集成方式：原生 HTTP Client（底层）、LangChain4j（推荐）、Spring AI（Spring 生态）
- Modelfile 支持自定义系统提示词和模型参数
- 量化模型（q4_0/q8_0）可显著降低内存需求
- Docker 部署 + GPU 加速 + 多实例负载均衡实现生产化

## 参考资料

1. Ollama 官方文档：https://ollama.com/docs
2. LangChain4j Ollama 集成：https://docs.langchain4j.dev/integrations/language-models/ollama
3. Spring AI Ollama 文档：https://docs.spring.io/spring-ai/reference/api/chat/ollama-chat.html
4. Ollama Docker 镜像：https://hub.docker.com/r/ollama/ollama
