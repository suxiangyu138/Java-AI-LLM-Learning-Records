# 06 - LangChain4j 核心原理

> 🎯 LangChain4j = LangChain 的 Java 实现 — 如果你熟悉 Python LangChain，这就是你在 Java 世界的"家"。本章覆盖架构分层、AiServices 代理原理、ChatMemory、多模型切换、流式输出

---

## 目录

1. [核心架构四层](#1-核心架构四层)
2. [AiServices：JDK 动态代理揭秘](#2-aiservicesjdk-动态代理揭秘)
3. [ChatMemory：自动对话记忆](#3-chatmemory自动对话记忆)
4. [多模型支持与切换](#4-多模型支持与切换)
5. [流式输出：TokenStream 与 SSE](#5-流式输出tokenstream-与-sse)
6. [Spring Boot 集成方式](#6-spring-boot-集成方式)
7. [生产级配置最佳实践](#7-生产级配置最佳实践)

---

## 1. 核心架构四层

```text
LangChain4j 分层：

┌────────────────────────────────────────────────┐
│  ④ AiServices（声明式 AI 接口）                 │  ← JDK 动态代理，接口即服务
├────────────────────────────────────────────────┤
│  ③ High-Level：Chains / Agent / RAG            │  ← 编排层
│     ContentRetriever / ChatMemory / Tool        │
├────────────────────────────────────────────────┤
│  ② Low-Level：ChatLanguageModel / EmbeddingModel│  ← 能力层（核心抽象）
│     DocumentLoader / TextSplitter               │
├────────────────────────────────────────────────┤
│  ① Adapter：OpenAI / Ollama / Claude / Gemini   │  ← 模型适配层（10+ 集成）
│     / DeepSeek / 通义 / 豆包 / 文心 / Azure     │
└────────────────────────────────────────────────┘
```

**核心设计原则：** 上层抽象（AiServices/Chain/Agent）依赖下层接口（ChatLanguageModel），而非具体实现。换模型只需改 Adapter，上层代码零修改。

---

## 2. AiServices：JDK 动态代理揭秘

```java
// 用户看到的（极简）：
interface Assistant {
    @SystemMessage("你是 Java 专家")
    String chat(@MemoryId String userId, @UserMessage String message);
}

Assistant assistant = AiServices.create(Assistant.class, model);
String reply = assistant.chat("user1", "解释多态");

// 框架内部实际做的事（JDK 动态代理 + 自动编排）：
// ① Proxy.newProxyInstance() 创建代理对象
// ② 调用 assistant.chat() 时被 InvocationHandler 拦截
// ③ 解析 @SystemMessage → 提取 System Prompt
// ④ 解析 @UserMessage → 提取用户输入
// ⑤ 解析 @MemoryId → 从 ChatMemoryProvider 获取该用户的 ChatMemory
// ⑥ 组装 ChatRequest：SystemMessage + ChatMemory 历史 + UserMessage + ToolSpecifications
// ⑦ 调用 ChatLanguageModel.generate(request)
// ⑧ 检查 AiMessage 是否有 ToolExecutionRequest？
//      是 → 执行工具 → 结果拼回 → goto ⑦
//      否 → 返回 String 内容
```

**AiServices 支持的方法签名：**

| 返回类型 | 行为 | 示例 |
|----------|------|------|
| `String` | 同步，返回完整文本 | `String chat(String msg)` |
| `TokenStream` | 流式，返回 TokenStream（需手动消费） | `TokenStream chat(String msg)` |
| `Flux<String>` | 流式（Spring WebFlux） | `Flux<String> chat(String msg)` |
| `Response<AiMessage>` | 返回完整响应对象（含 Token 用量、FinishReason） | `Response<AiMessage> chat(...)` |
| `boolean / enum / POJO` | **结构化输出**，自动反序列化 | `OrderInfo extractOrder(String input)` |
| `List<T>` | 列表结构化输出 | `List<Movie> recommendMovies(String genre)` |

---

## 3. ChatMemory：自动对话记忆

```java
// ChatMemory 类层次：
//   ChatMemory (interface)
//     → MessageWindowChatMemory    滑动窗口（保留最近 N 条，最常用）
//     → TokenWindowChatMemory      按 Token 数裁剪（精确控制上下文长度）

// ① 内存存储（开发/单机）
ChatMemory memory = MessageWindowChatMemory.withMaxMessages(20);

// ② 自定义存储后端（生产）
ChatMemory memory = MessageWindowChatMemory.builder()
    .id("user-123")                   // 按用户 ID 隔离
    .maxMessages(20)                  // 只保留最近 20 条消息
    .chatMemoryStore(new MyRedisChatMemoryStore())  // 持久化！重启不丢失
    .build();

// ③ ChatMemoryProvider：每次请求动态获取（多用户隔离）
ChatMemoryProvider provider = memoryId ->
    MessageWindowChatMemory.builder()
        .id(memoryId)
        .maxMessages(20)
        .chatMemoryStore(redisStore)
        .build();

// AiServices 使用 Provider：
Assistant assistant = AiServices.builder(Assistant.class)
    .chatLanguageModel(model)
    .chatMemoryProvider(provider)     // ★ 每次 chat() 自动获取对应用户的记忆
    .build();

// 用户 A 和用户 B 互不干扰（各自独立的 ChatMemory）
assistant.chat("user-A", "我叫张三");   // user-A 的记忆包含"我叫张三"
assistant.chat("user-B", "我叫李四");   // user-B 的记忆包含"我叫李四"
```

---

## 4. 多模型支持与切换

```java
// ① 一行切换模型（所有模型实现相同接口 ChatLanguageModel）
var openai   = OpenAiChatModel.builder().apiKey(key).build();
var ollama   = OllamaChatModel.builder().baseUrl("http://localhost:11434")
                    .modelName("qwen3").build();
var claude   = AnthropicChatModel.builder().apiKey(key).build();
var deepseek = OpenAiChatModel.builder()    // DeepSeek 兼容 OpenAI 格式
                    .baseUrl("https://api.deepseek.com/v1")
                    .apiKey(key).modelName("deepseek-chat").build();

// ② 策略模式切换（按场景选模型）
@Component
public class ModelSelector {
    public ChatLanguageModel select(String taskType) {
        return switch (taskType) {
            case "code"    -> claudeModel;     // Claude 编程最强
            case "chat"    -> deepseekModel;   // DeepSeek 便宜
            case "vision"  -> gpt4Vision;       // GPT-4 多模态
            default       -> defaultModel;
        };
    }
}

// ③ Spring Boot 配置文件切换
langchain4j.open-ai.chat-model:
  api-key: ${OPENAI_API_KEY}
  base-url: ${OPENAI_BASE_URL:https://api.openai.com/v1}
  model-name: gpt-5-turbo
  timeout: 60s
```

| 模型 | 适配方式 | 说明 |
|------|----------|------|
| OpenAI / GPT | `OpenAiChatModel` | 原生支持 |
| Claude | `AnthropicChatModel` | 原生支持 |
| DeepSeek / 通义 / 豆包 | `OpenAiChatModel` + 改 baseUrl | 兼容 OpenAI 格式 |
| Ollama / 本地模型 | `OllamaChatModel` | 本地部署 |
| Google Gemini | `VertexAiGeminiChatModel` | 原生支持 |
| Azure OpenAI | `AzureOpenAiChatModel` | 原生支持 |

---

## 5. 流式输出：TokenStream 与 SSE

```java
// ① TokenStream 回调模式
StreamingChatLanguageModel streamingModel = OpenAiStreamingChatModel.builder()
    .apiKey(key).modelName("gpt-5-turbo").build();

streamingModel.generate("写一首唐诗", new StreamingResponseHandler<>() {
    @Override public void onNext(String token) { System.out.print(token); }
    @Override public void onComplete(Response<AiMessage> resp) {
        System.out.println("\n[完成] Token 用量: " + resp.tokenUsage());
    }
    @Override public void onError(Throwable e) { e.printStackTrace(); }
});

// ② AiServices 流式接口（更简洁）
interface StreamingAssistant {
    TokenStream chat(String userMessage);  // 返回 TokenStream
}

StreamingAssistant assistant = AiServices.builder(StreamingAssistant.class)
    .streamingChatLanguageModel(streamingModel)
    .build();

TokenStream stream = assistant.chat("解释 Java 多态");
stream.onNext(System.out::print)
      .onComplete(r -> log.info("完成"))
      .onError(e -> log.error("出错", e))
      .start();

// ③ Spring WebFlux SSE 集成
@PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<String> stream(@RequestBody String message) {
    return Flux.create(sink -> {
        TokenStream stream = assistant.chat(message);
        stream.onNext(sink::next)
              .onComplete(r -> sink.complete())
              .onError(sink::error)
              .start();
    });
}
```

---

## 6. Spring Boot 集成方式

```java
// 方式 1：@AiService 自动装配（最简洁，LangChain4j 2.0+）
@AiService(
    chatModel = "openAiChatModel",
    streamingChatModel = "openAiStreamingChatModel",
    chatMemoryProvider = "chatMemoryProvider",
    wiringMode = AiServiceWiringMode.EXPLICIT  // 手动指定 Bean
)
public interface Consultant {
    @SystemMessage("你是资深技术顾问")
    Flux<String> chat(@MemoryId String memoryId, @UserMessage String message);
}

// 方式 2：@Bean 手动装配（更灵活）
@Bean
public Consultant consultant(ChatLanguageModel model,
                              ChatMemoryProvider memoryProvider) {
    return AiServices.builder(Consultant.class)
        .chatLanguageModel(model)
        .chatMemoryProvider(memoryProvider)
        .build();
}
```

---

## 7. 生产级配置最佳实践

```yaml
langchain4j:
  open-ai:
    chat-model:
      api-key: ${LLM_API_KEY}           # 环境变量注入，不写死
      model-name: deepseek-chat         # 换模型只改这里
      base-url: ${LLM_BASE_URL}
      temperature: 0.3                  # RAG/Agent 用 0.1-0.3
      timeout: 60s
      max-retries: 3
      log-requests: false               # 生产关闭（Token 含敏感数据）
      log-responses: false
    streaming-chat-model:               # 流式独立配置
      api-key: ${LLM_API_KEY}
      model-name: deepseek-chat
      temperature: 0.3
      timeout: 120s                     # 流式超时更长
  logging:
    level: INFO                         # 生产用 INFO
```

| 配置项 | 建议值 | 原因 |
|--------|--------|------|
| `temperature` | Agent: 0.1-0.3 / Chat: 0.7 | 低温度减少幻象工具调用 |
| `timeout` | 60s（普通）/ 120s（流式） | Agent 可能多次 LLM 调用 |
| `max-retries` | 3 | 网络抖动自动恢复 |
| `chat-memory.maxMessages` | 20 | 控制上下文窗口 + Token 消耗 |
| API Key | `${ENV_VAR}` | 绝不硬编码 |

---

> 🎯 **核心要点**：LangChain4j 的两个"灵魂"—— **① AiServices 声明式接口**（JDK 动态代理自动编排工具/记忆/RAG/流式）② **ChatMemory 自动记忆管理**（滑动窗口 + Provider 多用户隔离 + Redis 持久化）**。换模型只需改 Adapter 或配置文件，上层 AiServices 零修改。生产配置三件套：**环境变量存 API Key + temperature 0.3（Agent）+ maxMessages 20**。

**下一模块**：[07-LangChain4j RAG实战](07-LangChain4j-RAG实战.md) / **返回总览**：[00-总览](00-Java-AI开发框架体系总览.md)
