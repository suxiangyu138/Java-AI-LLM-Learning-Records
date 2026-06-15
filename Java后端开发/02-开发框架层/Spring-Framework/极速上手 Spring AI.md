极速上手 Spring AI
一、Spring AI 是什么（一句话懂）
Spring AI = Spring 生态的 AI 集成框架，把「各种大模型（OpenAI/通义千问/Ollama）+ 向量库（Milvus/Redis）+ 工具调用」统一成一套 Spring 风格 API，Java 程序员不用转 Python 就能做 AI 应用 。
核心价值：
- 统一抽象：一套代码切换不同大模型
- Spring 原生：DI、自动配置、和 Spring Boot/Cloud 无缝集成
- 企业级能力：RAG、函数调用、结构化输出、可观测性 
 
二、核心概念（必背）
1. ChatClient：对话入口，发 Prompt、拿回答（同步/流式）
2. Prompt：请求封装（系统提示+用户消息+参数）
3. Embedding：文本转向量（用于语义检索、RAG）
4. VectorStore：向量数据库（存 Embedding，如 Milvus、Redis）
5. RAG：检索增强生成（私有文档→向量库→检索→给 LLM 生成答案）
6. Function Calling：让 LLM 调用你的 Java 方法（查数据库、调接口） 
 
三、10分钟搭建第一个项目（直接复制）
1. 建项目（Maven）
    依赖（pom.xml）：
    xml
    <properties>
    <spring-ai.version>1.1.4</spring-ai.version>
    </properties>
    <dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-bom</artifactId>
            <version>${spring-ai.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
    </dependencyManagement>
    <dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <!-- 选一个模型：国内推荐通义千问，或用 Ollama 本地部署 -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-dashscope-spring-boot-starter</artifactId>
    </dependency>
    <!-- 向量库：Milvus 或 Redis -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-milvus-spring-boot-starter</artifactId>
    </dependency>
    </dependencies>
 
2. 配置密钥（application.yml）
    yaml
    spring:
  ai:
    dashscope:
      api-key: ${AI_DASHSCOPE_API_KEY}  # 通义千问密钥
      chat:
        options:
          model: qwen-turbo  # 模型名
 
- 通义千问：dashscope.console.aliyun.com 注册拿 Key
- 本地模型：用 Ollama，不用 Key，直接连本地服务
    3. 启动类+第一个对话
    java
    @SpringBootApplication
    public class AiDemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiDemoApplication.class, args);
    }
    @Bean
    public CommandLineRunner run(ChatClient.Builder chatClientBuilder) {
        return args -> {
            ChatClient chatClient = chatClientBuilder.build();
            String res = chatClient.prompt("用Java写一个单例模式").call().content();
            System.out.println(res);
        };
    }
    }
 
运行： mvn spring-boot:run ，控制台直接出答案 。
 
四、5大核心用法（直接抄代码）
1. 基础对话（同步）
    java
    @Service
    public class ChatService {
    private final ChatClient chatClient;
    public ChatService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }
    public String ask(String question) {
        return chatClient.prompt()
                .system("你是资深Java工程师，回答简洁专业")
                .user(question)
                .call()
                .content();
    }
    }
 
2. 流式输出（打字机效果，Flux）
    java
    public Flux<String> streamAsk(String question) {
    return chatClient.prompt()
            .user(question)
            .stream()
            .content();
    }
 
Controller 直接返回 Flux，前端 SSE 接收。
3. 结构化输出（返回 POJO）
    java
    // 定义实体
    public class UserInfo {
    private String name;
    private int age;
    private String city;
    // getter/setter
    }
    // 调用
    public UserInfo extractUser(String text) {
    return chatClient.prompt()
            .user("提取用户信息：" + text)
            .call()
            .entity(UserInfo.class);
    }
```[[__LINK_ICON]](https://springframework.org.cn/projects/spring-ai/?f_link_type=f_linkinlinenote&flow_extra=eyJpbmxpbmVfZGlzcGxheV9wb3NpdGlvbiI6MCwiZG9jX3Bvc2l0aW9uIjowLCJkb2NfaWQiOiI5MDI4ZTU4MGNiODhkNmE5LTIyZjc5NTY0OTNlNThmM2QifQ%3D%3D&inline_doc_id=9028e580cb88d6a9-22f7956493e58f3d)
### 4. Embedding + 向量库（RAG 基础）
```java
@Service
public class EmbeddingService {
    private final EmbeddingClient embeddingClient;
    private final VectorStore vectorStore;
    public EmbeddingService(EmbeddingClient embeddingClient, VectorStore vectorStore) {
        this.embeddingClient = embeddingClient;
        this.vectorStore = vectorStore;
    }
    // 存文档到向量库
    public void saveDoc(String content) {
        List<Document> docs = List.of(new Document(content));
        vectorStore.add(embeddingClient.embed(docs));
    }
    // 语义检索
    public List<Document> search(String query) {
        return vectorStore.similaritySearch(query);
    }
}
```[[__LINK_ICON]](https://springframework.org.cn/projects/spring-ai/?f_link_type=f_linkinlinenote&flow_extra=eyJkb2NfaWQiOiI5MDI4ZTU4MGNiODhkNmE5LTIyZjc5NTY0OTNlNThmM2QiLCJpbmxpbmVfZGlzcGxheV9wb3NpdGlvbiI6MCwiZG9jX3Bvc2l0aW9uIjowfQ%3D%3D&inline_doc_id=9028e580cb88d6a9-22f7956493e58f3d)
### 5. Function Calling（让 LLM 调 Java 方法）
```java
// 1. 定义工具方法
@Tool
public String queryOrder(String orderId) {
    // 实际查数据库/接口
    return "订单" + orderId + "状态：已发货";
}
// 2. 注入并调用
@Service
public class ToolChatService {
    private final ChatClient chatClient;
    public ToolChatService(ChatClient.Builder builder, QueryOrderTool tool) {
        this.chatClient = builder
                .defaultTools(tool) // 注册工具
                .build();
    }
    public String askWithTool(String question) {
        return chatClient.prompt().user(question).call().content();
    }
}
 
LLM 会自动判断是否调用  queryOrder  方法 。
 
五、RAG 最简实现（企业知识库问答）
流程：私有文档→分块→Embedding→存 Milvus→用户问题→检索相似文档→给 LLM 生成答案
java
public String ragAsk(String question) {
    // 1. 检索
    List<Document> docs = vectorStore.similaritySearch(question);
    // 2. 拼接上下文
    String context = docs.stream().map(Document::getContent).collect(Collectors.joining("\n"));
    // 3. 给 LLM 生成
    return chatClient.prompt()
            .system("基于以下上下文回答问题，不要编造：\n" + context)
            .user(question)
            .call()
            .content();
}
 
 
六、国内环境最佳实践（避坑）
1. 模型选择：优先通义千问（dashscope）、百度千帆，或本地 Ollama（部署 Llama3/Qwen）
2. 向量库：Milvus（主流）、Redis（简单）、PGVector（PostgreSQL 插件）
3. 密钥管理：用环境变量/配置中心，不要硬编码
4. 限流/计费：生产环境加限流，监控 Token 消耗
 
七、学习路线（7天精通）
1. 第1天：搭环境、跑通基础对话
2. 第2天：流式输出+结构化输出
3. 第3天：Embedding+向量库基础操作
4. 第4天：RAG 实现知识库问答
5. 第5天：Function Calling 集成业务接口
6. 第6天：多模型切换+配置优化
7. 第7天：做一个完整项目（智能客服/文档助手）
 
一句话总结：Spring AI 让 Java 后端开发 AI 应用像写 CRUD 一样简单，核心是「ChatClient 对话 + Embedding 向量 + RAG 检索 + Function 工具调用」。
