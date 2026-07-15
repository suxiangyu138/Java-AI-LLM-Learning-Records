# Spring AI 理论与实战

> **定位**：Spring 官方大模型集成框架，统一不同 AI 厂商 API。类比 Spring Data 之于数据库。

---

## 目录

1. [核心理论](#1-核心理论)
2. [实战入门](#2-实战入门)
3. [RAG 进阶](#3-rag-进阶)

---

## 1. 核心理论

### 1.1 是什么

| 类比 | 说明 |
|------|------|
| Spring Data → 统一数据库访问 | Spring AI → 统一大模型调用 |
| 切换 MySQL → 改依赖 | 切换 OpenAI → 改依赖 |

### 1.2 核心概念

| 概念 | 说明 |
|------|------|
| **ChatClient** | 对话入口，同步/流式 |
| **Prompt / PromptTemplate** | 提示词 + 参数化模板 |
| **ConversationMemory** | 多轮对话记忆 |
| **EmbeddingClient** | 文本→向量 |
| **FunctionCalling** | LLM 调用 Java 方法 |

### 1.3 框架对比

| 框架 | 定位 | 适用 |
|------|------|------|
| **Spring AI** | Java/Spring 生态 AI 集成 | Java 团队 |
| LangChain | Python 生态 AI 工具箱 | Python 开发者 |
| Dify | 低代码 AI 平台 | 非技术人员 |
| LangGraph | 复杂 AI 工作流 | 资深工程师 |

---

## 2. 实战入门

### 2.1 环境要求

| 项 | 要求 |
|----|------|
| JDK | 17+ |
| Spring Boot | 3.2+ |
| 模型 | OpenAI GPT-3.5（或其他） |

### 2.2 依赖与配置

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
    <version>1.0.0-M1</version>
</dependency>
```

```yaml
spring:
  ai:
    openai:
      api-key: 你的Key
      chat:
        model: gpt-3.5-turbo
        temperature: 0.7
```

### 2.3 单次对话

```java
@Service
public class SimpleChatService {
    private final ChatClient chatClient;

    public String singleChat(String message) {
        return chatClient.prompt(message).call().content();
    }
}
```

### 2.4 Prompt 模板

```java
String template = "请根据需求：{user需求}，按照{format}格式输出";
PromptTemplate pt = new PromptTemplate(template);
Map<String, Object> params = Map.of("user需求", "介绍SpringAI", "format", "markdown");
return chatClient.prompt(pt.create(params)).call().content();
```

### 2.5 多轮对话（ConversationMemory）

```java
InMemoryConversationMemory memory = new InMemoryConversationMemory();
Conversation conversation = new Conversation(conversationId, memory);
conversation.addUserMessage(message);
var response = chatClient.prompt(conversation).call();
conversation.addAssistantMessage(response.getResult().getOutput().getContent());
```

---

## 3. RAG 进阶

### 3.1 核心流程

```text
私有文档 → TikaReader 读取 → Embedding 向量化 → Redis 向量库
用户提问 → 向量检索相似文档 → 拼接上下文 → LLM 生成答案
```

### 3.2 文档加载入库

```java
TikaDocumentReader reader = new TikaDocumentReader(new File(filePath));
List<Document> documents = reader.get();
redisVectorStore.add(documents);
```

### 3.3 RAG 问答

```java
var retrievedDocs = vectorStore.similaritySearch(question, 3);
String template = "基于参考文档回答：\n{retrievedDocs}\n问题：{question}";
Prompt prompt = new PromptTemplate(template).create(params);
return chatClient.prompt(prompt).call().content();
```

---

## 4. 进阶方向

| 方向 | 说明 |
|------|------|
| 多模型动态切换 | 配置中心切换 OpenAI/通义千问/文心 |
| 对话记忆持久化 | Redis/MySQL 替代内存记忆 |
| Function Calling | LLM 调用数据库/API |
| 监控 | Actuator + 日志 + 告警 |
| 多模态 | 文生图 + 语音识别 |
