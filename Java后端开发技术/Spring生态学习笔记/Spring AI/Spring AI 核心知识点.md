# Spring AI 核心知识点
一句话定位：**Spring 官方 AI 工程化框架，用 Spring 方式把 LLM/向量库/工具调用集成到 Java 应用，主打统一抽象、可移植、企业级生产可用**。

---

## 一、核心定位与设计哲学
### 1）解决的痛点
- **模型碎片化**：统一 OpenAI、Azure、通义千问、Ollama 等接口，换模型不改业务代码。
- **工程化缺失**：给 Java 生态补全 LLM 应用的标准栈（RAG、工具调用、可观测、重试熔断）。
- **生态割裂**：无缝集成 Spring Boot、Spring Data、Security，和现有系统打通。

### 2）设计原则（Spring 经典）
- **约定优于配置**、**面向接口编程**、**依赖注入**
- **POJO 优先**：AI 输入输出映射到 Java 对象。
- **可移植抽象层**：一套 API 适配多厂商，最小改动切换组件。

---

## 二、核心架构与四大抽象
### 1）整体架构（三层）
- **应用层**：Controller/Service → 调用 Spring AI 抽象接口
- **抽象层（核心）**：`ChatClient`/`EmbeddingClient`/`VectorStore`/`Tool`
- **适配层**：各厂商实现（OpenAI、Ollama、Milvus 等）

### 2）四大核心抽象（必背）
#### ✅ ChatClient（对话模型）
- 统一调用 LLM：聊天补全、流式响应、多轮会话
- 支持：同步/流式、上下文管理、参数配置（温度、最大 token）
```java
ChatClient chatClient = ChatClient.create(openAiChatModel);
String response=chatClient.prompt("你好").call().content();
```

#### ✅ EmbeddingClient（嵌入模型）
- 文本转向量，用于**语义搜索、RAG、相似度计算**
- 输出：`List<Double>` 向量
```java
Embedding embedding=embeddingClient.embed("Spring AI 核心知识点");
```

#### ✅ VectorStore（向量存储）
- 统一接口对接主流向量库：**Milvus、Redis、PGVector、Chroma、Weaviate**
- 核心能力：**存储向量+元数据、相似度检索、SQL 式过滤、分页**
```java
List<Document> docs=vectorStore.similaritySearch(SearchRequest.query("Spring AI").withTopK(3));
```

#### ✅ Tool / Function Calling（工具调用）
- 让 LLM 调用本地 Java 方法（ReAct 模式：思考→行动→观察）
- 注解驱动：`@Tool` 暴露方法，模型自动识别并调用
```java
@Component
public class WeatherService {
    @Tool("获取指定城市的实时天气")
    public String getWeather(String city) {
        return city + "：晴，25℃";
    }
}
```


---

## 三、核心功能（高频必用）
### 1）结构化输出（Structured Outputs）
- LLM 输出直接映射到 **Java POJO**，免手动解析 JSON
```java
record User(String name, int age) {}
User user=chatClient.prompt("生成用户：张三，20岁").call().entity(User.class);
```

### 2）RAG（检索增强生成）
- **企业知识库问答标配**：私有文档→分块→向量化→存入向量库→检索→喂给 LLM→生成答案
- 核心组件：`DocumentReader`（文档加载）、`TextSplitter`（文本分块）、`VectorStore`（向量存储）、`Retriever`（检索）


### 3）Prompt Template（提示词模板）
- 复用提示词、动态参数、统一格式
```java
PromptTemplate template=PromptTemplate.create("请用{style}风格回答：{question}");
Prompt prompt=template.create(Map.of("style", "简洁", "question", "什么是 Spring AI？"));
```

### 4）流式响应（Streaming）
- 支持打字机效果，适合聊天界面
```java
Flux<String> flux=chatClient.prompt("讲个故事").stream().content();
```

### 5）可观测性（Observability）
- 内置：**日志、指标、链路追踪**（集成 Micrometer、OTel）
- 监控：调用耗时、token 消耗、成功率、错误类型

### 6）企业级特性
- **重试/限流/熔断**：适配不稳定模型接口
- **安全**：集成 Spring Security，控制 AI 接口访问权限
- **多模型路由**：按场景自动选择最优模型（如简单任务用轻量模型，复杂任务用 GPT-4）

---

## 四、主流模型与向量库支持
### 1）模型提供商（开箱即用）
- 闭源：**OpenAI、Azure OpenAI、Google Vertex AI、Anthropic**
- 开源本地：**Ollama（推荐，一键跑 Llama3/Qwen）**
- 国内：**通义千问、文心一言、星火认知**

### 2）向量数据库（统一接口）
- 云原生：**Milvus、Pinecone、Weaviate**
- 轻量/嵌入式：**Chroma、Redis**
- 关系型扩展：**PostgreSQL（pgvector）、MySQL**

---

## 五、快速入门（依赖+配置+调用）
### 1）引入依赖（Maven）
```xml
<!-- Spring AI 父依赖 -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-bom</artifactId>
            <version>1.1.2</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<!-- OpenAI 聊天模型 -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
</dependency>

<!-- Milvus 向量库 -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-milvus-spring-boot-starter</artifactId>
</dependency>
```

### 2）配置（application.yml）
```yaml
spring:
  ai:
    openai:
      api-key: sk-xxx
      chat:
        model: gpt-3.5-turbo
    milvus:
      client:
        host: localhost
        port: 19530
      database-name: default
```

### 3）核心调用示例
```java
@RestController
public class AIController {
    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    public AIController(OpenAiChatModel chatModel, VectorStore vectorStore) {
        this.chatClient=ChatClient.create(chatModel);
        this.vectorStore=vectorStore;
    }

    // 简单聊天
    @GetMapping("/chat")
    public String chat(String msg) {
        return chatClient.prompt(msg).call().content();
    }

    // RAG 知识库问答
    @GetMapping("/rag")
    public String rag(String question) {
        List<Document> docs=vectorStore.similaritySearch(question);
        String context=docs.stream().map(Document::getContent).collect(Collectors.joining("\n"));
        String prompt=String.format("基于以下上下文回答问题：\n上下文：%s\n问题：%s", context, question);
        return chatClient.prompt(prompt).call().content();
    }
}
```

---

## 六、面试/实战必背总结
1. **一句话**：Spring AI 是 Spring 生态的 AI 工程化框架，统一 LLM/向量库接口，支持 RAG、工具调用、结构化输出。
2. **四大抽象**：`ChatClient`、`EmbeddingClient`、`VectorStore`、`Tool`。
3. **核心场景**：**聊天机器人、知识库问答（RAG）、AI 助手、文档分析、智能客服**。
4. **企业价值**：**统一标准、降低集成成本、生产可用、无缝融入 Spring 生态**。

---
