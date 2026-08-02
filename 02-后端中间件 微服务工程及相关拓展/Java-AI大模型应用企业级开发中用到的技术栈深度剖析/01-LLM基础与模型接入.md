# 01 - LLM 基础与模型接入

> 定位：ChatClient 统一调用、多模型接入与路由、流式输出、结构化输出、上下文管理——LLM 接入的基础能力

## 📚 目录

1. [LLM 基础概念](#1-llm-基础概念)
2. [ChatClient 统一调用](#2-chatclient-统一调用)
3. [多模型接入与路由](#3-多模型接入与路由)
4. [流式输出（SSE）](#4-流式输出sse)
5. [结构化输出](#5-结构化输出)
6. [上下文管理](#6-上下文管理)

---

## 1. LLM 基础概念

```
LLM（大语言模型）核心概念：
  Token：最小文本单元（约 0.75 个英文单词）
  Context Window：上下文窗口（输入+输出上限）
  温度（temperature）：随机性（0 确定、1 发散）
  Prompt：输入指令
  Completion：模型输出

⚠️ 面试必答：
"LLM = 基于上下文预测下一个 token 的模型——
 token 是计费与窗口的基本单位；
 temperature 控制确定性。"
```

### 1.1 Token 与计费

```
Token 估算：1 个汉字 ≈ 1-2 token，1 个英文单词 ≈ 1.3 token
计费：输入 + 输出分别计费（输出通常更贵）
成本优化：精简 prompt、压缩历史、小模型做简单任务

⚠️ 面试必答：
"Token 是成本单元——prompt 精简、
 历史压缩、模型路由是三大成本手段。"
```

---

## 2. ChatClient 统一调用

```java
// Spring AI ChatClient（Fluent API 一行调用）
@Service
public class ChatService {

    private final ChatClient chatClient;

    public ChatService(ChatClient.Builder builder) {
        // ⚠️ 统一构建器（多模型可配多个 ChatClient）
        this.chatClient = builder.build();
    }

    public String chat(String message) {
        return chatClient.prompt()
                .user(message)                    // 用户输入
                .call()                           // 同步调用
                .content();                       // 取文本
    }

    public String chatWithSystem(String system, String user) {
        return chatClient.prompt()
                .system(system)                   // ⚠️ 系统提示（角色设定）
                .user(user)
                .call()
                .content();
    }
}
```

```xml
<!-- 依赖与配置 -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-model-openai</artifactId>
</dependency>

spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          model: gpt-4o
          temperature: 0.7
```

> 🎯 **要点**：ChatClient = 统一调用接口（Fluent API）——prompt → call → content 三件套。system（角色）+ user（输入）是对话基础结构。

---

## 3. 多模型接入与路由

### 3.1 多模型配置

```java
// ⚠️ 多模型路由：简单任务用小模型、复杂任务用旗舰模型
@Configuration
public class ModelConfig {

    // 小模型（便宜/快）：DeepSeek、Qwen-turbo
    @Bean
    public ChatClient cheapClient(ChatClient.Builder builder) {
        return builder.defaultOptions(OpenAiChatOptions.builder()
                .model("deepseek-chat")
                .temperature(0.2)
                .build())
            .build();
    }

    // 旗舰模型（贵/强）：GPT-4o、Claude
    @Bean
    public ChatClient flagshipClient(ChatClient.Builder builder) {
        return builder.defaultOptions(OpenAiChatOptions.builder()
                .model("gpt-4o")
                .temperature(0.7)
                .build())
            .build();
    }
}

// 路由使用
public String handle(String task, int complexity) {
    return complexity > 5
        ? flagshipClient.prompt().user(task).call().content()
        : cheapClient.prompt().user(task).call().content();
}
```

### 3.2 模型选择策略

| 任务 | 模型 | 理由 |
|------|------|------|
| 分类/抽取 | 小模型 | 便宜快 |
| 摘要/翻译 | 小-中 | 常规 |
| 复杂推理/代码 | 旗舰模型 | 质量优先 |
| 思考型任务 | 推理模型 | 但 token 消耗 3-10 倍 |

> 🎯 **要点**：模型路由 = 成本控制核心——"简单任务小模型、复杂任务旗舰模型"。推理模型（o1/DeepSeek-R1）token 消耗 3-10 倍，需异步化。

---

## 4. 流式输出（SSE）

```java
// ⚠️ 流式输出：打字机效果（Flux + SSE）
@RestController
public class ChatController {

    private final ChatClient chatClient;

    // ① 流式接口：返回 Flux<String>（SSE 推送）
    @PostMapping(value = "/api/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChat(@RequestBody ChatRequest request) {
        return chatClient.prompt()
                .user(request.getMessage())
                .stream()                       // ⚠️ 流式调用
                .content();                     // Flux<String> 逐块输出
    }
}

// ② 前端消费（EventSource 或 fetch）
// const es = new EventSource('/api/chat/stream');
// es.onmessage = (e) => appendText(e.data);   // 逐块追加
```

```
⚠️ 为什么流式是标配：
  LLM 生成慢（秒级）→ 同步等待体验差
  流式让首字秒出（打字机效果）
  推理模型（10s+）必须流式

⚠️ 面试必答：
"流式 = SSE（Server-Sent Events）——
 Spring AI 的 .stream() 返回 Flux；
 长生成场景必用（首字体验 + 推理模型适配）。"
```

---

## 5. 结构化输出

```java
// ⚠️ 结构化输出：JSON Schema 约束 + DTO 自动映射
// 解决"模型输出格式不稳定"问题

public record SentimentResult(String sentiment, double confidence, List<String> keywords) { }

public SentimentResult analyze(String text) {
    return chatClient.prompt()
            .user("分析以下评论的情感：" + text)
            .call()
            .entity(SentimentResult.class);     // ⚠️ 自动映射 JSON → DTO
}

// 返回：
// {"sentiment":"positive","confidence":0.92,"keywords":["好用","推荐"]}
```

| 结构化输出方式 | 说明 |
|------|------|
| .entity(Class) | DTO 自动映射（最常用） |
| BeanOutputConverter | 底层转换器 |
| JSON Schema 约束 | 强制格式 |

> 🎯 **要点**：结构化输出 = .entity(DTO) 自动映射——**让 LLM 输出可直接落入业务对象**，是"LLM 与系统集成"的桥梁。

---

## 6. 上下文管理

### 6.1 对话记忆

```java
// 对话记忆（多轮对话需要历史）
// ⚠️ 方式一：手动携带历史
List<Message> history = getHistory(sessionId);
String result = chatClient.prompt()
        .messages(history)                     // 携带历史
        .user(currentQuestion)
        .call().content();

// ⚠️ 方式二：Memory Advisor（Spring AI）
// .advisors(advisors -> advisors
//     .param(ChatMemory.CHAT_MEMORY_CONVERSATION_ID_KEY, sessionId)
//     .param(ChatMemory.CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
```

### 6.2 上下文裁剪（成本与窗口）

```
上下文三大问题：
  ① 窗口有限（超长截断）
  ② 历史冗余（token 浪费）
  ③ 信息稀释（长历史降低回答质量）

裁剪策略：
  ① 只保留最近 N 轮（retrieveSize）
  ② 历史摘要压缩（摘要模型替换旧历史）
  ③ 关键信息提取（RAG 只检索相关内容）

⚠️ 面试必答：
"上下文管理三策略——最近 N 轮、
 摘要压缩、RAG 精选；
 窗口是有限资源，'精选上下文'是
 质量与成本的关键。"
```

---

> 🎯 **核心要点**：LLM 接入 = **ChatClient 统一调用**（prompt/call/content）+ **模型路由**（小模型省钱、旗舰模型保质量）+ **流式 SSE**（打字机体验）+ **结构化输出**（.entity DTO 映射）+ **上下文管理**（最近轮/摘要/RAG）。这五块是 LLM 应用的基础能力。

---

**返回总览**：[00-JavaAI技术栈总览与全景架构](00-JavaAI技术栈总览与全景架构.md) | **下一篇**：[02-Prompt工程](02-Prompt工程.md)
