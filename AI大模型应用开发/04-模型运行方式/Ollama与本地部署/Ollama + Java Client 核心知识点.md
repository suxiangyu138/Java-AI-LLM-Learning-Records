# Ollama + Java Client 核心知识点

## 一、概述

Ollama 是一个轻量级的本地大模型运行工具，支持一行命令下载并运行 Llama、Qwen、Mistral 等开源模型。通过 Java HTTP Client 或 LangChain4j/Semantic Kernel 等框架的集成，Java 后端可以直接调用本地模型。

**核心定位：** 本地私有化部署大模型的最简方案，**隐私要求高时必选**。

**官网：** https://ollama.com

## 二、Ollama 基础

### 2.1 安装与模型管理

```bash
# macOS / Linux 安装
curl -fsSL https://ollama.com/install.sh | sh

# Windows：下载安装包 https://ollama.com/download

# 拉取模型
ollama pull qwen2.5:7b        # 通义千问 7B
ollama pull llama3:8b         # Meta Llama 3 8B
ollama pull deepseek-coder:6.7b  # DeepSeek 编码模型

# 列出本地模型
ollama list

# 运行模型（命令行交互）
ollama run qwen2.5:7b
```

### 2.2 常用模型推荐（2026年）

| 模型 | 参数量 | 适合场景 | 内存需求 |
|------|--------|----------|----------|
| **qwen2.5:7b** | 7B | 通用对话、代码生成 | 8GB+ |
| **qwen2.5:14b** | 14B | 复杂推理、高质量生成 | 16GB+ |
| **deepseek-coder:6.7b** | 6.7B | 代码生成专项 | 8GB+ |
| **llama3:8b** | 8B | 通用英文/中文 | 8GB+ |
| **codellama:7b** | 7B | Python/Java 代码 | 8GB+ |
| **mistral:7b** | 7B | 轻量高效英文 | 8GB+ |

## 三、Java 集成方式

### 3.1 原生 HTTP Client

```java
// Ollama 默认监听 localhost:11434
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

## 四、自定义模型（Modelfile）

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

```bash
# Ubuntu：安装 NVIDIA CUDA 后自动使用 GPU
# Windows/macOS：Metal / CUDA 自动检测

# 查看 GPU 是否可用
ollama run qwen2.5:7b --verbose
# GPU 使用率在输出中显示
```

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
|------|------|
| **响应延迟** | 本地推理，无网络延迟，但模型推理本身需 1-10 秒 |
| **吞吐量** | 单卡 7B 模型约 50-100 tokens/s |
| **高可用** | 部署多实例 + 前端负载均衡 |
| **监控** | 集成 Prometheus + Grafana 监控 GPU 使用率、请求延迟 |
| **模型更新** | 热重载新模型文件，不需要重启服务 |

## 七、与其他方案对比

| 方案 | 隐私性 | 延迟 | 成本 | 质量 |
|------|--------|------|------|------|
| **Ollama 本地** | 最高 | 中 | 硬件成本 | 取决于模型 |
| **OpenAI API** | 低 | 低 | API 费用 | 最高 |
| **vLLM 自建** | 高 | 低 | 硬件+运维 | 取决于模型 |
| **阿里云/腾讯云** | 中 | 低 | API 费用 | 高 |

## 八、总结

Ollama + Java Client 是**本地私有化部署大模型的最简路径**。适合数据不出内网的金融/医疗场景、离线开发环境、或需要频繁实验但不想付 API 费用的个人开发者。搭配 LangChain4j 或 Spring AI 可以无缝融入现有 Java 技术栈。
