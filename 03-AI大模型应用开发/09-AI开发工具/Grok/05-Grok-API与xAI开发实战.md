# 05 - Grok API 与 SpaceXAI 开发实战

> 🎯 Grok 4.5 API 于 2026 年 7 月全面开放——$2/$6 per 1M tokens 的定价、OpenAI SDK 完全兼容、Token 效率 4.2× 优于同级、原生集成 Cursor。Java 开发者可以最低成本接入"效率旗舰"

---

## 📚 目录

1. [API 概览与接入](#1-api-概览与接入)
2. [Grok 4.5 API 核心能力](#2-grok-45-api-核心能力)
3. [Java 集成实战](#3-java-集成实战)
4. [Function Calling 工具调用](#4-function-calling-工具调用)
5. [成本控制与 Token 优化](#5-成本控制与-token-优化)
6. [错误处理与重试策略](#6-错误处理与重试策略)

---

## 1. API 概览与接入

### 1.1 2026.07 定价速查

| 模型 | 输入/1M tokens | 输出/1M tokens | 缓存输入 | 上下文 |
|------|:---:|:---:|:---:|:---:|
| **grok-4.5** | **$2.00** | **$6.00** | $0.50 (75% off) | 500K |
| grok-3 | $3.00 | $15.00 | $0.75 | 1M |
| grok-3-mini | $0.30 | $4.00 | $0.075 | 1M |

### 1.2 API 端点

| 端点 | 用途 |
|------|------|
| `https://api.x.ai/v1/chat/completions` | 对话补全（OpenAI 兼容） |
| `https://api.x.ai/v1/responses` | 新版 Responses API |
| `https://api.x.ai/v1/embeddings` | 文本嵌入 |

### 1.3 Python 快速调用

```python
from openai import OpenAI

client = OpenAI(
    api_key="xai-your-key",
    base_url="https://api.x.ai/v1",
)

# Grok 4.5 — 默认模型
response = client.chat.completions.create(
    model="grok-4.5",
    messages=[
        {"role": "system", "content": "你是一个 Java 后端专家"},
        {"role": "user", "content": "用 Java 21 虚拟线程优化这段代码"}
    ],
    temperature=0.3,
    max_tokens=4096,
)

print(response.choices[0].message.content)
# Grok 4.5 的优势：同样的回答，输出 Token 大约是 GPT 的 1/3
```

### 1.4 多模型分层调用策略

```text
简单任务 → grok-3-mini ($0.30/$4)
  CRUD 生成 / 代码格式化 / 文档注释 / 简单问答

中等任务 → grok-4.5 ($2/$6)
  日常开发主力 / Bug 修复 / 代码审查 / 重构

复杂任务 → grok-4.5 + Thinking Mode
  架构设计 / 复杂算法 / 多文件修改 / Agent 任务

Token 效率是关键优势：
  grok-4.5 完成同等任务的 Token 消耗是 Opus 4.8 的 1/4
  → 实际成本比定价数字看起来更划算
```

---

## 2. Grok 4.5 API 核心能力

| 能力 | 支持 | 说明 |
|------|:---:|------|
| 文本对话 | ✅ | 基础问答与聊天 |
| 流式输出 (SSE) | ✅ | 实时流式响应 |
| Function Calling | ✅ | 工具/函数调用 |
| JSON Mode | ✅ | 强制 JSON 输出 |
| 多模态 | ✅ | 图片输入（20MB, JPG/PNG） |
| 推理模式 | ✅ | Thinking tokens |
| 缓存 | ✅ | 75% 折扣 |

---

## 3. Java 集成实战

### 3.1 Spring AI 集成 Grok 4.5

```yaml
# application.yml
spring:
  ai:
    openai:
      api-key: ${XAI_API_KEY}
      base-url: https://api.x.ai
      chat:
        options:
          model: grok-4.5       # 主力模型
          temperature: 0.3
          max-tokens: 4096
```

```java
@Configuration
public class GrokConfig {

    @Bean
    public OpenAiApi grokApi() {
        return new OpenAiApi(
            "https://api.x.ai",
            System.getenv("XAI_API_KEY")
        );
    }

    @Bean
    public OpenAiChatModel grokChatModel(OpenAiApi api) {
        return OpenAiChatModel.builder()
            .openAiApi(api)
            .defaultOptions(OpenAiChatOptions.builder()
                .model("grok-4.5")
                .temperature(0.3)
                .maxTokens(4096)
                .build())
            .build();
    }
}
```

### 3.2 利用 Grok 4.5 的 Token 效率优势

```java
@Service
public class EfficientGrokService {

    private final OpenAiChatModel chatModel;

    /**
     * 批量任务 — Grok 4.5 的 Token 效率在这里最有价值
     * 100 个 CRUD 接口生成，总成本约 $5，而 Opus 4.8 需要 ~$25
     */
    public List<String> generateBatchCrud(List<EntityDefinition> entities) {
        return entities.stream()
            .map(entity -> {
                String prompt = buildCrudPrompt(entity);
                return chatModel.call(prompt);
            })
            .toList();
    }

    /**
     * Token 消耗对比监控
     */
    public TaskResult executeWithTracking(String task, String model) {
        long startTokens = getCurrentTokenUsage();
        String result = chatModel.call(task);
        long tokensUsed = getCurrentTokenUsage() - startTokens;

        log.info("Grok 4.5 任务完成: {} tokens", tokensUsed);
        // 典型结果：同样任务 Grok 4.5 用 ~15K tokens，Opus 用 ~67K tokens

        return new TaskResult(result, tokensUsed);
    }
}
```

### 3.3 直接 HTTP 调用（无框架依赖）

```java
// Grok 4.5 API 完全兼容 OpenAI SDK 格式
// 只需修改 base URL 和 api key
public class GrokHttpClient {
    private static final String API_URL =
        "https://api.x.ai/v1/chat/completions";

    public String chat(String message) throws Exception {
        var requestBody = String.format("""
        {
            "model": "grok-4.5",
            "messages": [{"role": "user", "content": "%s"}],
            "temperature": 0.3,
            "max_tokens": 4096
        }
        """, message.replace("\"", "\\\""));

        var request = HttpRequest.newBuilder()
            .uri(URI.create(API_URL))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + System.getenv("XAI_API_KEY"))
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();

        var response = HttpClient.newHttpClient()
            .send(request, HttpResponse.BodyHandlers.ofString());

        // 解析 OpenAI 兼容的响应格式
        return extractContent(response.body());
    }
}
```

---

## 4. Function Calling 工具调用

Grok 4.5 支持完整的 Function Calling，与 OpenAI 格式完全兼容：

```java
// 工具定义（与 OpenAI 格式 100% 兼容）
public static final String WEATHER_TOOL = """
{
    "type": "function",
    "function": {
        "name": "get_weather",
        "description": "获取指定城市的实时天气",
        "parameters": {
            "type": "object",
            "properties": {
                "city": {
                    "type": "string",
                    "description": "城市名称"
                }
            },
            "required": ["city"]
        }
    }
}
""";

// Grok 4.5 的优势：FC 调用的 Token 消耗更少
// → Agent 循环中每一步都更省 → 总体 Agent 成本更低
```

---

## 5. 成本控制与 Token 优化

### 5.1 Grok 4.5 的 Token 效率红利

```java
public class TokenEfficiencyTracker {

    /**
     * 对比不同模型完成同样任务的开销
     */
    public record EfficiencyReport(
        String model,
        long tokensUsed,
        double cost,
        String efficiencyRating
    ) {}

    public EfficiencyReport benchmark(String task, String model) {
        long tokens = executeAndCount(task, model);
        double cost = calculateCost(model, tokens);

        // Grok 4.5 典型结果：15K tokens，$0.10
        // 对比 Opus 4.8：67K tokens，$1.68
        return new EfficiencyReport(model, tokens, cost,
            model.equals("grok-4.5") ? "⭐⭐⭐⭐⭐ 极致省 Token" : "标准");
    }
}
```

### 5.2 月度成本估算

```text
场景：Java 开发团队，日均 200 次 API 调用

使用 Claude Opus 4.8：
  200 次 × $2.50/次 × 30 天 = $15,000/月

使用 Grok 4.5：
  200 次 × $0.50/次 × 30 天 = $3,000/月

年节省：$144,000 — 够雇两个初级开发了
```

---

## 6. 错误处理与重试策略

```java
/**
 * Grok 4.5 的 Token 效率意味着重试的成本更低 —
 * 可以把重试次数从 3 次提升到 5 次，仍然比 Opus 4.8 的 1 次便宜
 */
public String chatWithRetry(String message) {
    int maxRetries = 5; // Grok 4.5 可以更大胆地重试
    for (int i = 0; i < maxRetries; i++) {
        try {
            return chat(message);
        } catch (HttpServerErrorException | RateLimitException e) {
            if (i == maxRetries - 1) throw e;
            sleep((long) Math.pow(2, i) * 500); // 更快退避（成本低不怕）
        }
    }
    throw new IllegalStateException("unreachable");
}
```

> 🎯 **核心要点**：Grok 4.5 API 的核心竞争力不是"绝对能力最强"，而是"每美元完成的任务最多"——$0.10 完成 Opus 4.8 要花 $1.68 的任务。对于高频 API 调用场景（Agent 自动化、批量处理、CI/CD 集成），这是结构性的成本优势。

---

**上一模块**：[04-Grok平台与X生态深度集成](04-Grok平台与X生态深度集成.md) | **下一模块**：[06-Grok-DeepSearch深度搜索解密](06-Grok-DeepSearch深度搜索解密.md) | **返回总览**：[00-Grok知识体系总览](00-Grok知识体系总览.md)
