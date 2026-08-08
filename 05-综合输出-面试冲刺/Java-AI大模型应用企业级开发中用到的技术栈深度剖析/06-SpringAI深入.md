# 06 - Spring AI 深入

> 定位：Advisors 机制、Memory 管理、ChatClient 全能力、RAG 集成、结构化输出、多模型路由——Spring AI 完整实践

## 📚 目录

1. [Spring AI 架构](#1-spring-ai-架构)
2. [Advisors 拦截机制](#2-advisors-拦截机制)
3. [ChatMemory 对话记忆](#3-chatmemory-对话记忆)
4. [RAG 集成实践](#4-rag-集成实践)
5. [ChatClient 全能力](#5-chatclient-全能力)
6. [Spring AI 2.0 展望](#6-spring-ai-20-展望)

---

## 1. Spring AI 架构

```
Spring AI 分层：
  Model API：ChatModel/EmbeddingModel（统一接口）
  ChatClient：高层 Fluent API（推荐用法）
  Advisors：拦截链（RAG/记忆/审计）
  VectorStore：向量库抽象
  Tools/MCP：工具集成

⚠️ 面试必答：
"Spring AI = 模型统一抽象 + ChatClient
 + Advisor 拦截 + 向量抽象——
 一套 API 对接 20+ 模型（OpenAI/Claude/DeepSeek/Qwen）。"
```

---

## 2. Advisors 拦截机制

### 2.1 Advisor 是什么

```
Advisor = AI 调用链的拦截器（类似 Spring AOP/Around）
  在"请求模型前 / 响应后"插入逻辑

内置 Advisors：
  QuestionAnswerAdvisor：一行开启 RAG
  MessageChatMemoryAdvisor：对话记忆
  SafeGuardAdvisor：敏感词过滤
  SimpleLoggerAdvisor：日志审计
  VectorStoreChatMemoryAdvisor：向量记忆

⚠️ 面试必答：
"Advisor = AI 调用链的拦截器——
 内置 RAG/记忆/安全/日志四类；
 自定义 Advisor 实现审计/合规。"
```

### 2.2 使用与自定义

```java
// 使用内置 Advisor
ChatClient client = ChatClient.builder(chatModel)
        .defaultAdvisors(new QuestionAnswerAdvisor(vectorStore))   // ⚠️ RAG 一行开启
        .defaultAdvisors(new MessageChatMemoryAdvisor(chatMemory))
        .build();

// 自定义 Advisor（审计/合规场景）
public class AuditAdvisor implements CallAdvisor {

    @Override
    public ChatResponse aroundCall(AdvisedRequest request, CallAdvisorChain chain) {
        long start = System.currentTimeMillis();
        ChatResponse response = chain.next(request);   // ⚠️ 执行调用
        // 审计：记录请求/响应/耗时/成本
        auditService.log(request.userText(), response.getResult(), 
                         System.currentTimeMillis() - start);
        return response;
    }
}
```

> 🎯 **要点**：Advisor = AI 的 AOP——RAG（QuestionAnswerAdvisor）、记忆（ChatMemoryAdvisor）、审计（自定义）都是拦截器。**一行开启 RAG** 是 Spring AI 的标志性能力。

---

## 3. ChatMemory 对话记忆

```java
// ① 选择记忆实现
@Configuration
public class MemoryConfig {

    @Bean
    public ChatMemory chatMemory() {
        // InMemory：单机；VectorStore：长期
        return new MessageWindowChatMemory(
            MessageWindowChatMemory.builder()
                .chatMemoryStore(new InMemoryChatMemoryStore())
                .maxMessages(20)              // ⚠️ 最近 20 条
                .build());
    }
}

// ② 使用（多用户隔离）
@Service
public class ChatService {

    private final ChatClient chatClient;

    public String chat(String userId, String question) {
        return chatClient.prompt()
                .user(question)
                .advisors(advisors -> advisors
                    // ⚠️ 按会话 ID 隔离记忆
                    .param(ChatMemory.CHAT_MEMORY_CONVERSATION_ID_KEY, userId)
                    .param(ChatMemory.CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .call().content();
    }
}
```

### 3.1 记忆策略选择

| 记忆实现 | 特点 | 适用 |
|---------|------|------|
| MessageWindowChatMemory | 最近 N 条 | 短对话 |
| TokenWindowChatMemory | 按 token 裁剪 | 控制成本 |
| VectorStoreChatMemory | 向量检索历史 | 长期记忆 |

> 🎯 **要点**：ChatMemory = 会话历史的存储与检索——conversationId 隔离 + retrieveSize 控制窗口。**多用户场景 conversationId 必传**（否则串会话）。

---

## 4. RAG 集成实践

### 4.1 一键 RAG（Advisor 方式）

```java
// ⚠️ 最简单：QuestionAnswerAdvisor 自动检索
ChatClient ragClient = ChatClient.builder(chatModel)
        .defaultAdvisors(new QuestionAnswerAdvisor(vectorStore,
                SearchRequest.builder()
                    .topK(5)
                    .similarityThreshold(0.7)
                    .build()))
        .build();

// 使用：自动检索 + 注入上下文 + 生成
String answer = ragClient.prompt()
        .user("订单如何退款？")
        .call().content();
// ⚠️ 内部流程：问题向量化 → 检索 → 拼接知识 → 模型回答
```

### 4.2 手动 RAG（精细控制）

```java
// 手动：检索 + 拼装（需要精细控制时）
public String ragChat(String question) {
    // ① 检索
    List<Document> docs = vectorStore.similaritySearch(
        SearchRequest.builder().query(question).topK(5).build());

    // ② 拼装上下文
    String context = docs.stream()
        .map(Document::getContent)
        .collect(Collectors.joining("\n\n"));

    // ③ 生成（带来源引用）
    return chatClient.prompt()
        .system("基于提供的资料回答，并标注来源文档。\n资料：\n" + context)
        .user(question)
        .call().content();
}
```

> 🎯 **要点**：RAG 两条路——Advisor 一行开启（默认流程）vs 手动检索（精细控制）。**生产建议手动 + 溯源**（回答带来源可验证）。

---

## 5. ChatClient 全能力

```java
// ChatClient 能力全景
chatClient.prompt()
    // ① 多输入：system + user + 历史
    .system(systemPrompt)
    .user(userText)
    .messages(historyMessages)

    // ② 参数配置
    .options(OpenAiChatOptions.builder()
        .model("gpt-4o")
        .temperature(0.7)
        .maxTokens(500)
        .build())

    // ③ 工具
    .tools(orderTools, productTools)

    // ④ Advisors（RAG/记忆/审计）
    .advisors(advisorSpec -> advisorSpec.param("key", "value"))

    // ⑤ 调用方式
    .call().content()              // 同步
    .stream().content()            // 流式
    .call().entity(MyDto.class);   // 结构化

// 多模型路由 + 多 ChatClient（见 01 篇）
```

---

## 6. Spring AI 2.0 展望

```
Spring AI 2.0（2026 开发中，M 版本）：
  Java 21+ 基线
  Agent 能力增强（Agentic 编排）
  MCP 集成深化
  模型抽象重构（多模态扩展）

⚠️ 面试必答：
"Spring AI 2.0 方向——Java 21 基线、
 Agent 编排增强、MCP 深化；
 当前 1.x 已覆盖 Chat/RAG/工具/MCP
 主要场景（生产可用）。"
```

---

> 🎯 **核心要点**：Spring AI 体系 = **架构**（Model API + ChatClient + Advisor + VectorStore）+ **Advisors**（RAG/记忆/审计拦截器）+ **ChatMemory**（conversationId 隔离 + 窗口控制）+ **RAG 集成**（一行 Advisor vs 手动精细）+ **ChatClient 全能力**（输入/工具/流式/结构化）。"Advisor 机制是 Spring AI 的灵魂"——AOP 思想在 AI 调用的复现。

---

**返回总览**：[00-JavaAI技术栈总览与全景架构](00-JavaAI技术栈总览与全景架构.md) | **上一篇**：[05-Agent架构设计](05-Agent架构设计.md) | **下一篇**：[07-LangChain4j深入](07-LangChain4j深入.md)
