# 06 - MiMo 在开发框架中的集成

> 🎯 MiMo 兼容 OpenAI 协议 = 所有主流框架零成本接入 — LangChain4j、Spring AI、Python LangChain、OpenClaw 全部改 baseUrl 即用。1M 上下文让"整库加载"成为框架级能力

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
MiMo API 兼容 OpenAI 协议

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
public class MiMoConfig {

    @Bean
    public ChatLanguageModel mimoChatModel() {
        return OpenAiChatModel.builder()
            .apiKey(System.getenv("XIAOMI_API_KEY"))
            .baseUrl("https://api.xiaomimimo.com/v1")    // ★ MiMo 端点
            .modelName("MiMo-V2.5-Pro")                  // ★ 模型 ID
            .timeout(Duration.ofSeconds(60))
            .build();
    }
}

// 长文本场景：直接塞入大文档（1M 上下文）
interface ContractAnalyst {
    @SystemMessage("你是合同审查专家")
    String analyze(String contractText);   // 直接传入全文
}
```

---

## 3. Spring AI 集成

```yaml
# application.yml
spring:
  ai:
    openai:
      api-key: ${XIAOMI_API_KEY}
      base-url: https://api.xiaomimimo.com/v1   # ★ 指向 MiMo
      chat:
        options:
          model: MiMo-V2.5-Pro
          temperature: 0.3
```

```java
// ChatClient 代码零修改
@RestController
public class LongDocController {

    private final ChatClient chatClient;

    public LongDocController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @PostMapping("/analyze-doc")
    public String analyze(@RequestBody String document) {
        return chatClient.prompt()
            .user("分析这份文档：" + document)   // 1M 上下文直接塞入
            .call()
            .content();
    }
}
```

---

## 4. Python LangChain 集成

```python
from langchain_openai import ChatOpenAI

llm = ChatOpenAI(
    base_url="https://api.xiaomimimo.com/v1",
    api_key=os.getenv("XIAOMI_API_KEY"),
    model="MiMo-V2.5-Pro",
    temperature=0.3
)

# 长文本 RAG 场景（1M 上下文替代分块）
from langchain_core.messages import HumanMessage

with open("large_report.txt", encoding="utf-8") as f:
    content = f.read()

response = llm.invoke([
    HumanMessage(content=f"分析这份报告的关键发现：\n\n{content}")
])
print(response.content)
```

---

## 5. OpenClaw 集成

```json
// openclaw.json
{
  "models": {
    "default": "mimo-pro",
    "providers": {
      "mimo-pro": {
        "type": "openai-compatible",
        "baseUrl": "https://api.xiaomimimo.com/v1",
        "apiKey": "${XIAOMI_API_KEY}",
        "model": "MiMo-V2.5-Pro"
      }
    }
  }
}
```

> 💡 **OpenClaw + MiMo 的组合优势：** OpenClaw 长链路 Agent 的上下文管理 + MiMo 的 1M 窗口（整库加载）+ 缓存命中极低价（¥0.025/百万）— 长链路 Agent 的成本最优解之一。

---

> 🎯 **核心要点**：MiMo 集成一句话 — **"OpenAI 兼容 = 所有框架零成本接入"**。所有框架只需改 `baseUrl`（api.xiaomimimo.com/v1）+ `model`（MiMo-V2.5-Pro）。杀手级用法：1M 上下文的框架级"直接整库加载" — 无需 RAG 基础设施，长文档/代码库场景框架代码最简。

**下一模块**：[07-MiMo选型与生态对比](07-MiMo选型与生态对比.md) / **返回总览**：[00-总览](00-Mimo-API知识体系总览.md)
