# 06 - MiniMax 在开发框架中的集成

> 🎯 M3 双协议兼容 = 全生态零成本接入 — Anthropic 协议接 Claude 生态工具（Claude Code/Roo Code/Cline），OpenAI 协议接 OpenAI 生态。已接入 10+ 主流 AI 编程工具

---

## 目录

1. [集成总览：双协议生态](#1-集成总览双协议生态)
2. [LangChain4j 集成](#2-langchain4j-集成)
3. [Spring AI 集成](#3-spring-ai-集成)
4. [Python LangChain 集成](#4-python-langchain-集成)
5. [OpenClaw 与编程工具集成](#5-openclaw-与编程工具集成)

---

## 1. 集成总览：双协议生态

```text
M3 双协议 = 两个生态全覆盖：

Anthropic 协议（推荐）：
├── Claude Code ✅（已接入）
├── Roo Code ✅
├── Cline ✅
├── LangChain4j AnthropicChatModel
└── 所有 Claude 生态工具

OpenAI 协议：
├── Cursor ✅（已接入）
├── OpenClaw（openai-compatible）
├── Spring AI（openai starter）
└── 所有 OpenAI 生态工具

已接入的 10+ 主流编程工具：Claude Code / Roo Code / Cline / Cursor / Codex 等
```

---

## 2. LangChain4j 集成

```java
// 方式 1：Anthropic 协议
@Bean
public ChatLanguageModel m3AnthropicModel() {
    return AnthropicChatModel.builder()
        .apiKey(System.getenv("MINIMAX_API_KEY"))
        .baseUrl("https://api.minimaxi.com/v1")       // ★ MiniMax 端点
        .modelName("MiniMax-M3")
        .build();
}

// 方式 2：OpenAI 协议
@Bean
public ChatLanguageModel m3OpenAiModel() {
    return OpenAiChatModel.builder()
        .apiKey(System.getenv("MINIMAX_API_KEY"))
        .baseUrl("https://api.minimaxi.com/v1")
        .modelName("MiniMax-M3")
        .build();
}

// AiServices 使用
interface Assistant {
    @SystemMessage("你是 M3 驱动的助手")
    String chat(@UserMessage String message);
}
```

---

## 3. Spring AI 集成

```yaml
# application.yml（OpenAI 协议）
spring:
  ai:
    openai:
      api-key: ${MINIMAX_API_KEY}
      base-url: https://api.minimaxi.com/v1    # ★ 指向 MiniMax
      chat:
        options:
          model: MiniMax-M3
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
            .content();                          // M3 驱动
    }
}
```

---

## 4. Python LangChain 集成

```python
# 方式 1：Anthropic 协议
from langchain_anthropic import ChatAnthropic

llm = ChatAnthropic(
    anthropic_api_key=os.getenv("MINIMAX_API_KEY"),
    base_url="https://api.minimaxi.com/v1",
    model="MiniMax-M3"
)

# 方式 2：OpenAI 协议
from langchain_openai import ChatOpenAI

llm = ChatOpenAI(
    base_url="https://api.minimaxi.com/v1",
    api_key=os.getenv("MINIMAX_API_KEY"),
    model="MiniMax-M3",
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

## 5. OpenClaw 与编程工具集成

```json
// OpenClaw 集成（OpenAI 兼容协议）
{
  "models": {
    "default": "minimax-m3",
    "providers": {
      "minimax-m3": {
        "type": "openai-compatible",
        "baseUrl": "https://api.minimaxi.com/v1",
        "apiKey": "${MINIMAX_API_KEY}",
        "model": "MiniMax-M3"
      }
    }
  }
}
```

**编程工具接入：**

```bash
# Claude Code 接入 M3（Anthropic 协议）
# 环境变量设置：
export ANTHROPIC_BASE_URL="https://api.minimaxi.com/v1"
export ANTHROPIC_AUTH_TOKEN="$MINIMAX_API_KEY"
export ANTHROPIC_MODEL="MiniMax-M3"
claude                          # 用 M3 驱动 Claude Code！

# Cursor 接入 M3（OpenAI 协议）
# 设置 → Models → OpenAI API Key → base URL 改为 MiniMax 端点
```

> 💡 **M3 + Claude Code 组合**：用 MiniMax-M3 驱动 Claude Code 的 Agent 能力 — 享受 M3 的编程性能（SWE-Bench 59%）+ Claude Code 的工程化界面，成本仅为 Claude 的 1/15。

---

> 🎯 **核心要点**：M3 集成一句话 — **"双协议兼容 = 全生态零成本接入"**。Anthropic 协议接 Claude 生态（Claude Code/Roo Code/Cline），OpenAI 协议接 OpenAI 生态（Cursor/OpenClaw/Spring AI）。最爽的用法：环境变量切换 `ANTHROPIC_BASE_URL` 让 Claude Code 跑 M3 模型（成本 1/15）。

**下一模块**：[07-MiniMax选型与生态对比](07-MiniMax选型与生态对比.md) / **返回总览**：[00-总览](00-MiniMax-API知识体系总览.md)
