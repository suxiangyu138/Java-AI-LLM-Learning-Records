# 06 - Qwen 在开发框架中的集成

> 🎯 Qwen 兼容 OpenAI 协议 = 所有主流框架零成本接入 — LangChain4j、Spring AI、Python LangChain、OpenClaw 全部改 baseUrl 即用。百炼平台一个 Key 还能调用 GLM/Kimi/MiniMax/DeepSeek

---

## 目录

1. [集成总览](#1-集成总览)
2. [LangChain4j 集成](#2-langchain4j-集成)
3. [Spring AI 集成](#3-spring-ai-集成)
4. [Python LangChain 集成](#4-python-langchain-集成)
5. [OpenClaw 与编程工具集成](#5-openclaw-与编程工具集成)

---

## 1. 集成总览

```text
Qwen API 兼容 OpenAI 协议（base_url: dashscope.aliyuncs.com/compatible-mode/v1）

因此：
├── OpenAI SDK（Python/Node.js/Java）→ 改 base_url 即用
├── LangChain4j OpenAiChatModel → 改 baseUrl 即用
├── Spring AI openai starter → 改 base-url 即用
├── Python LangChain ChatOpenAI → 改 base_url 即用
├── OpenClaw openai-compatible → 改 baseUrl 即用
└── 百炼平台 100+ 模型 API 一个 Key 全调用
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
public class QwenConfig {

    @Bean
    public ChatLanguageModel qwenChatModel() {
        return OpenAiChatModel.builder()
            .apiKey(System.getenv("DASHSCOPE_API_KEY"))
            .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")  // ★ 百炼
            .modelName("qwen3.5-plus")                                     // ★ 模型 ID
            .timeout(Duration.ofSeconds(60))
            .build();
    }

    @Bean
    public ChatLanguageModel qwenFlashModel() {
        return OpenAiChatModel.builder()
            .apiKey(System.getenv("DASHSCOPE_API_KEY"))
            .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")
            .modelName("qwen3.5-flash")      // 最便宜（¥0.2）
            .build();
    }
}

// AiServices 使用
interface Assistant {
    @SystemMessage("你是 Qwen 驱动的助手")
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
      api-key: ${DASHSCOPE_API_KEY}
      base-url: https://dashscope.aliyuncs.com/compatible-mode/v1   # ★ 百炼
      chat:
        options:
          model: qwen3.5-plus
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
            .content();                       // Qwen 驱动
    }
}
```

---

## 4. Python LangChain 集成

```python
from langchain_openai import ChatOpenAI

# Qwen3.5-Plus（旗舰）
llm = ChatOpenAI(
    base_url="https://dashscope.aliyuncs.com/compatible-mode/v1",
    api_key=os.getenv("DASHSCOPE_API_KEY"),
    model="qwen3.5-plus",
    temperature=0.3
)

# Qwen3.5-Flash（最便宜）
llm_flash = ChatOpenAI(
    base_url="https://dashscope.aliyuncs.com/compatible-mode/v1",
    api_key=os.getenv("DASHSCOPE_API_KEY"),
    model="qwen3.5-flash"
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

## 5. OpenClaw 与编程工具集成

```json
// openclaw.json
{
  "models": {
    "default": "qwen3.5-plus",
    "providers": {
      "qwen3.5-plus": {
        "type": "openai-compatible",
        "baseUrl": "https://dashscope.aliyuncs.com/compatible-mode/v1",
        "apiKey": "${DASHSCOPE_API_KEY}",
        "model": "qwen3.5-plus"
      }
    }
  }
}
```

**编程工具接入（Coding Plan）：**

```bash
# Claude Code 接入 Qwen（OpenAI 兼容端点）
export OPENAI_BASE_URL="https://dashscope.aliyuncs.com/compatible-mode/v1"
export OPENAI_API_KEY="$DASHSCOPE_API_KEY"
# 或使用 Qwen Code / Cline / Cursor（Coding Plan 覆盖）

# Cursor 接入（百炼 Coding Plan）
# 设置 → Models → OpenAI API Key → base URL 改为百炼端点
```

> 💡 **百炼 Coding Plan 的价值：** 一个订阅（Pro ¥200/月）即可在 Claude Code/Cline/OpenClaw/Cursor 中自由切换 Qwen3.5/GLM-5/Kimi/MiniMax 四大开源模型。

---

> 🎯 **核心要点**：Qwen 集成一句话 — **"OpenAI 兼容 = 所有框架零成本接入"**。所有框架只需改 `baseUrl`（dashscope.aliyuncs.com/compatible-mode/v1）+ `model`（qwen3.5-plus/flash）。最大卖点：**百炼平台一个 Key 调用 100+ 模型**（Qwen/GLM/Kimi/MiniMax/DeepSeek）+ Coding Plan 订阅跨工具自由切换。

**下一模块**：[07-Qwen选型与生态对比](07-Qwen选型与生态对比.md) / **返回总览**：[00-总览](00-Qwen-API知识体系总览.md)
