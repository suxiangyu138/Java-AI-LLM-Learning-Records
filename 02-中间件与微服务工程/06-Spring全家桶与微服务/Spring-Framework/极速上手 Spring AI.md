# 极速上手 Spring AI

> **一句话**：Spring AI = Java 生态的 AI 集成框架，统一封装各大模型 API。核心 = ChatClient 对话 + Embedding 向量 + RAG 检索 + Function Calling。

---

## 1. 核心概念

| 概念 | 说明 |
|------|------|
| **ChatClient** | 对话入口，同步/流式 |
| **Prompt** | 请求封装（系统提示+用户消息） |
| **Embedding** | 文本→向量（语义检索基础） |
| **VectorStore** | 向量数据库（Milvus/Redis） |
| **RAG** | 检索增强生成（私有文档→检索→LLM 生成） |
| **Function Calling** | LLM 调用你的 Java 方法 |

---

## 2. 10 分钟搭建

### 依赖

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-dashscope-spring-boot-starter</artifactId>
</dependency>
```

### 配置

```yaml
spring:
  ai:
    dashscope:
      api-key: ${AI_DASHSCOPE_API_KEY}
      chat:
        options:
          model: qwen-turbo
```

### 第一个对话

```java
@Bean
CommandLineRunner run(ChatClient.Builder builder) {
    return args -> {
        ChatClient client = builder.build();
        String res = client.prompt("用Java写一个单例模式").call().content();
        System.out.println(res);
    };
}
```

---

## 3. 五大核心用法

### 同步对话

```java
public String ask(String question) {
    return chatClient.prompt()
            .system("你是资深Java工程师，回答简洁专业")
            .user(question)
            .call().content();
}
```

### 流式输出

```java
public Flux<String> streamAsk(String question) {
    return chatClient.prompt().user(question).stream().content();
}
```

### 结构化输出

```java
public UserInfo extractUser(String text) {
    return chatClient.prompt()
            .user("提取用户信息：" + text)
            .call().entity(UserInfo.class);
}
```

### Embedding + 向量库

```java
// 存文档
vectorStore.add(embeddingClient.embed(List.of(new Document(content))));

// 语义检索
List<Document> docs = vectorStore.similaritySearch(query);
```

### Function Calling

```java
@Tool
public String queryOrder(String orderId) {
    return "订单" + orderId + "状态：已发货";
}
// LLM 自动判断是否调用
```

---

## 4. RAG 最简实现

```java
public String ragAsk(String question) {
    // ① 检索相关文档
    List<Document> docs = vectorStore.similaritySearch(question);
    String context = docs.stream().map(Document::getContent).collect(joining("\n"));
    // ② 给 LLM 生成答案
    return chatClient.prompt()
            .system("基于以下上下文回答：\n" + context)
            .user(question).call().content();
}
```

---

## 5. 学习路线

| 天 | 内容 |
|:--:|------|
| 1 | 搭环境、跑通基础对话 |
| 2 | 流式输出 + 结构化输出 |
| 3 | Embedding + 向量库 |
| 4 | RAG 知识库问答 |
| 5 | Function Calling 集成 |
| 6 | 多模型切换 + 配置优化 |
| 7 | 完整项目（智能客服/文档助手） |
