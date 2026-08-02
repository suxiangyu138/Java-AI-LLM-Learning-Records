# 06 - GLM 在开发框架中的集成

> 🎯 GLM 兼容 OpenAI 协议 = 天然适配所有主流框架 — LangChain4j、Spring AI、Python LangChain、OpenClaw 全部改 baseUrl 即接入。本章给出各框架完整接入代码

---

## 目录

1. [集成总览](#1-集成总览)
2. [LangChain4j 集成](#2-langchain4j-集成)
3. [Spring AI 集成](#3-spring-ai-集成)
4. [Python LangChain 集成](#4-python-langchain-集成)
5. [OpenClaw 集成](#5-openclaw-集成)

---

## 1. 集成总览

```text
GLM API 兼容 OpenAI 协议（base_url: https://open.bigmodel.cn/api/paas/v4）

因此：
├── OpenAI SDK（Python/Node.js/Java）→ 改 base_url 即用
├── LangChain4j OpenAiChatModel → 改 baseUrl 即用
├── Spring AI openai starter → 改 base-url 即用
├── Python LangChain ChatOpenAI → 改 base_url 即用
├── OpenClaw openai-compatible → 改 baseUrl 即用
└── Dify/LobeChat/NextChat → 全部零成本接入
```

---

## 2. LangChain4j 集成

```java
// pom.xml：只需 OpenAI 兼容 starter
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-open-ai</artifactId>
</dependency>

@Configuration
public class GlmConfig {

    @Bean
    public ChatLanguageModel glmChatModel() {
        return OpenAiChatModel.builder()
            .apiKey(System.getenv("ZHIPU_API_KEY"))
            .baseUrl("https://open.bigmodel.cn/api/paas/v4")  // ★ 关键
            .modelName("glm-5")                                // ★ 模型 ID
            .timeout(Duration.ofSeconds(60))
            .build();
    }

    @Bean
    public StreamingChatLanguageModel glmStreamingModel() {
        return OpenAiStreamingChatModel.builder()
            .apiKey(System.getenv("ZHIPU_API_KEY"))
            .baseUrl("https://open.bigmodel.cn/api/paas/v4")
            .modelName("glm-5")
            .build();
    }
}

// AiServices 完全不变
interface Assistant {
    @SystemMessage("你是 GLM 驱动的助手")
    String chat(@UserMessage String message);
}
```

---

## 3. Spring AI 集成

```yaml
# application.yml
spring:
  ai:
    openai:
      api-key: ${ZHIPU_API_KEY}
      base-url: https://open.bigmodel.cn/api/paas/v4   # ★ 指向智谱
      chat:
        options:
          model: glm-5
          temperature: 0.3
```

```java
// ChatClient 代码零修改
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
            .content();          // GLM 驱动
    }
}
```

---

## 4. Python LangChain 集成

```python
from langchain_openai import ChatOpenAI

llm = ChatOpenAI(
    base_url="https://open.bigmodel.cn/api/paas/v4",
    api_key=os.getenv("ZHIPU_API_KEY"),
    model="glm-5",
    temperature=0.3
)

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
// openclaw.json
{
  "models": {
    "default": "glm-5-turbo",
    "providers": {
      "glm-5-turbo": {
        "type": "openai-compatible",
        "baseUrl": "https://open.bigmodel.cn/api/paas/v4",
        "apiKey": "${ZHIPU_API_KEY}",
        "model": "glm-5-turbo"
      }
    }
  }
}
```

> 💡 **GLM-5-Turbo 是 OpenClaw 官方推荐的龙虾场景模型** — 39 元/月套餐（3500 万 Token）是长链路 Agent 的最低成本方案。

---

> 🎯 **核心要点**：GLM 集成一句话 — **"OpenAI 兼容 = 所有框架零成本接入"**。所有框架只需改 `baseUrl`（open.bigmodel.cn/api/paas/v4）+ `model`（glm-5/glm-5.1/glm-5-turbo/glm-5.2）。OpenClaw 场景直接用 GLM-5-Turbo + 龙虾套餐最划算。

**下一模块**：[07-GLM选型与生态对比](07-GLM选型与生态对比.md) / **返回总览**：[00-总览](00-GLM-API知识体系总览.md)
