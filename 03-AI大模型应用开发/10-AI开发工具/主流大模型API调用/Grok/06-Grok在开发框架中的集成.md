# 06 - Grok 在开发框架中的集成

> 🎯 Grok 兼容 OpenAI 协议 = 所有主流框架零成本接入 — LangChain4j、Spring AI、Python LangChain、OpenClaw 全部改 baseUrl 即用。本章给出完整接入代码

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
Grok API 兼容 OpenAI 协议（base_url: https://api.x.ai/v1）

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
// pom.xml：OpenAI 兼容 starter
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-open-ai</artifactId>
</dependency>

@Configuration
public class GrokConfig {

    @Bean
    public ChatLanguageModel grokChatModel() {
        return OpenAiChatModel.builder()
            .apiKey(System.getenv("XAI_API_KEY"))
            .baseUrl("https://api.x.ai/v1")          // ★ 指向 xAI
            .modelName("grok-4.5")                   // ★ Grok 模型
            .timeout(Duration.ofSeconds(60))
            .build();
    }

    @Bean
    public ChatLanguageModel grokFastModel() {
        return OpenAiChatModel.builder()
            .apiKey(System.getenv("XAI_API_KEY"))
            .baseUrl("https://api.x.ai/v1")
            .modelName("grok-4-1-fast")              // 2M 上下文
            .build();
    }
}

// AiServices 使用（超长文档场景直接整库加载）
interface RepoAnalyst {
    String analyze(String repoContent);   // 2M 窗口直接塞仓库内容
}
```

---

## 3. Spring AI 集成

```yaml
# application.yml
spring:
  ai:
    openai:
      api-key: ${XAI_API_KEY}
      base-url: https://api.x.ai/v1      # ★ 指向 Grok
      chat:
        options:
          model: grok-4.5
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
            .content();                   // Grok 驱动
    }
}
```

---

## 4. Python LangChain 集成

```python
from langchain_openai import ChatOpenAI

# 常规模型
llm = ChatOpenAI(
    base_url="https://api.x.ai/v1",
    api_key=os.getenv("XAI_API_KEY"),
    model="grok-4.5",
    temperature=0.3
)

# 2M 超长上下文模型（整库分析）
llm_long = ChatOpenAI(
    base_url="https://api.x.ai/v1",
    api_key=os.getenv("XAI_API_KEY"),
    model="grok-4-1-fast"
)

# 工具调用
from langchain_core.tools import tool

@tool
def search_x_tweets(query: str) -> str:
    """搜索 X 实时帖子（Grok 独家能力）"""
    return x_search(query)

llm_with_tools = llm.bind_tools([search_x_tweets])
```

---

## 5. OpenClaw 集成

```json
// openclaw.json
{
  "models": {
    "default": "grok-4.5",
    "providers": {
      "grok-4.5": {
        "type": "openai-compatible",
        "baseUrl": "https://api.x.ai/v1",
        "apiKey": "${XAI_API_KEY}",
        "model": "grok-4.5"
      },
      "grok-fast": {
        "type": "openai-compatible",
        "baseUrl": "https://api.x.ai/v1",
        "apiKey": "${XAI_API_KEY}",
        "model": "grok-4-1-fast"
      }
    }
  }
}
```

> 💡 **OpenClaw + Grok 的组合优势：** OpenClaw 的长链路 Agent 需要超长上下文（2M 整库加载）+ Grok 的实时 X 数据（舆情监控 Agent）— 天然匹配。

---

> 🎯 **核心要点**：Grok 集成一句话 — **"OpenAI 兼容 = 所有框架零成本接入"**。所有框架只需改 `baseUrl`（api.x.ai/v1）+ `model`（grok-4.5/grok-4-1-fast/grok-4.3/grok-4.20）。OpenClaw + Grok 4.1 Fast（2M 上下文）是长链路 Agent 的理想组合。

**下一模块**：[07-Grok选型与生态对比](07-Grok选型与生态对比.md) / **返回总览**：[00-总览](00-Grok-API知识体系总览.md)
