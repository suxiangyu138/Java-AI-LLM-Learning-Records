# 06 - Kimi 在开发框架中的集成

> 🎯 Kimi 兼容 OpenAI 协议 = 天然适配所有主流 AI 框架 — LangChain4j、Spring AI、OpenAI SDK、OpenClaw 全部改配置即可接入。本章给出各框架的完整接入代码

---

## 目录

1. [集成总览：为什么 Kimi 接入零成本](#1-集成总览为什么-kimi-接入零成本)
2. [LangChain4j 集成](#2-langchain4j-集成)
3. [Spring AI 集成](#3-spring-ai-集成)
4. [Python LangChain 集成](#4-python-langchain-集成)
5. [OpenClaw 集成](#5-openclaw-集成)
6. [第三方平台接入](#6-第三方平台接入)

---

## 1. 集成总览：为什么 Kimi 接入零成本

```text
Kimi API 兼容 OpenAI 协议（base_url: https://api.moonshot.cn/v1）

因此：
├── OpenAI 官方 SDK（Python/Node.js/Java）→ 改 base_url 即用
├── LangChain4j OpenAiChatModel → 改 baseUrl 即用
├── Spring AI openai starter → 改 base-url 即用
├── Python LangChain ChatOpenAI → 改 base_url 即用
├── OpenClaw openai-compatible → 改 baseUrl 即用
└── 其他 OpenAI 兼容工具（Dify/LobeChat/NextChat...）→ 全部零成本接入
```

---

## 2. LangChain4j 集成

```java
// pom.xml 只需引入 OpenAI 兼容 starter
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-open-ai</artifactId>
    <version>1.15.0-beta25</version>
</dependency>

// 配置类
@Configuration
public class KimiConfig {

    @Bean
    public ChatLanguageModel kimiChatModel() {
        return OpenAiChatModel.builder()
            .apiKey(System.getenv("MOONSHOT_API_KEY"))
            .baseUrl("https://api.moonshot.cn/v1")      // ★ 关键：改 baseUrl
            .modelName("kimi-k2.6")                     // ★ Kimi 模型 ID
            .timeout(Duration.ofSeconds(60))
            .build();
    }

    @Bean
    public StreamingChatLanguageModel kimiStreamingModel() {
        return OpenAiStreamingChatModel.builder()
            .apiKey(System.getenv("MOONSHOT_API_KEY"))
            .baseUrl("https://api.moonshot.cn/v1")
            .modelName("kimi-k2.6")
            .build();
    }

    @Bean
    public EmbeddingModel embeddingModel() { ... }      // 嵌入可继续用 BGE 等
}

// 使用（AiServices 完全不变）
interface Assistant {
    @SystemMessage("你是 Kimi 驱动的助手")
    String chat(@UserMessage String message);
}

@Bean
public Assistant assistant(ChatLanguageModel model) {
    return AiServices.builder(Assistant.class)
        .chatLanguageModel(model)
        .build();
}
```

---

## 3. Spring AI 集成

```yaml
# application.yml — Spring AI 接入 Kimi（走 OpenAI 兼容端点）
spring:
  ai:
    openai:
      api-key: ${MOONSHOT_API_KEY}
      base-url: https://api.moonshot.cn/v1    # ★ 指向 Kimi
      chat:
        options:
          model: kimi-k3                      # ★ Kimi 模型 ID
          temperature: 0.3
      embedding:
        options:
          model: text-embedding-3-small
```

```java
// Spring AI 2.x 的 ChatClient 完全不变
@RestController
public class ChatController {

    private final ChatClient chatClient;

    public ChatController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @PostMapping("/chat")
    public String chat(@RequestBody String message) {
        return chatClient.prompt()
            .user(message)
            .call()
            .content();                     // Kimi 驱动，代码零修改
    }

    // 工具调用（Kimi 支持 Function Calling）
    @Service
    public class OrderTools {
        @Tool(description = "查询订单状态")
        public String queryOrder(@ToolParam(description = "订单号") String orderId) {
            return "订单 " + orderId + "：已发货";
        }
    }
}
```

---

## 4. Python LangChain 集成

```python
from langchain_openai import ChatOpenAI

# ★ 只需三个参数（base_url + api_key + model）
llm = ChatOpenAI(
    base_url="https://api.moonshot.cn/v1",     # ★ 指向 Kimi
    api_key=os.getenv("MOONSHOT_API_KEY"),
    model="kimi-k2.6",
    temperature=0.3
)

# 与其他 LangChain 组件完全兼容
from langchain_core.messages import HumanMessage
response = llm.invoke([HumanMessage(content="解释什么是 RAG")])

# 工具调用
from langchain_core.tools import tool

@tool
def get_weather(city: str) -> str:
    """查询城市天气"""
    return f"{city}: 晴 25°C"

llm_with_tools = llm.bind_tools([get_weather])
```

---

## 5. OpenClaw 集成

```json
// openclaw.json — OpenClaw 接入 Kimi
{
  "models": {
    "default": "kimi-k2.6",
    "providers": {
      "kimi-k2.6": {
        "type": "openai-compatible",          // ★ OpenAI 兼容类型
        "baseUrl": "https://api.moonshot.cn/v1",
        "apiKey": "${MOONSHOT_API_KEY}",
        "model": "kimi-k2.6"
      },
      "kimi-k3": {
        "type": "openai-compatible",
        "baseUrl": "https://api.moonshot.cn/v1",
        "apiKey": "${MOONSHOT_API_KEY}",
        "model": "kimi-k3"
      }
    }
  }
}
```

> 💡 **为什么 K2.6 是 OpenClaw 最佳搭档**：工具调用稳定（Toolathlon +79.9%）+ 5 天长程自主 + 256K 上下文 — 完美匹配 OpenClaw 的持续 Agent 场景。

---

## 6. 第三方平台接入

```text
Kimi K2.6 已首发登陆的第三方平台（一个 API Key 全搞定）：

├── 腾讯云 TokenHub
├── 360 智脑 API
├── 京东云
├── 阿里云百炼
└── AIHubMix 等聚合网关

接入方式：全部兼容 OpenAI 协议 → 只需把 base_url 换成各平台地址
```

| 平台 | base_url 示例 |
|------|--------------|
| 官方（国内） | `https://api.moonshot.cn/v1` |
| 官方（国际） | `https://api.moonshot.ai/v1` |
| 腾讯云 TokenHub | `https://tokenhub.tencent.com/v1` |
| 聚合网关 AIHubMix | `https://aihubmix.com/v1` |

---

> 🎯 **核心要点**：Kimi 集成一句话 — **"OpenAI 兼容 = 所有框架零成本接入"**。LangChain4j/Spring AI 只需改 `baseUrl` + `modelName`；Python LangChain 改 `base_url`；OpenClaw 配 `openai-compatible` 类型。所有下游代码（AiServices/ChatClient/工具调用）完全不用改。

**下一模块**：[07-Kimi选型与生态对比](07-Kimi选型与生态对比.md) / **返回总览**：[00-总览](00-Kimi知识体系总览.md)
