# 02 - Spring AI Chat 与对话管理

> 🎯 对话不只是"一问一答" — 本章覆盖 Prompt 模板、流式 SSE、多轮对话记忆（Advisor 自动管理）、结构化输出、输出解析器、Advisors 拦截链。从简单对话到生产级多会话系统

---

## 目录

1. [ChatClient：统一对话入口](#1-chatclient统一对话入口)
2. [System Prompt 与 Prompt 模板](#2-system-prompt-与-prompt-模板)
3. [流式输出 SSE：从 Flux 到前端](#3-流式输出-sse从-flux-到前端)
4. [多轮对话记忆：MessageChatMemoryAdvisor](#4-多轮对话记忆messagechatmemoryadvisor)
5. [结构化输出：JSON/Bean/Entity](#5-结构化输出jsonbeanentity)
6. [Advisors 拦截链](#6-advisors-拦截链)
7. [多用户会话隔离](#7-多用户会话隔离)

---

## 1. ChatClient：统一对话入口

```java
// Spring AI 2.0 中 ChatClient 是唯一对话入口（替代废弃的 ChatModel.call）
@Autowired private ChatClient.Builder chatClientBuilder;

// ① 最简调用
String reply = chatClientBuilder.build()
    .prompt().user("Hello").call().content();

// ② 带参数的调用
String reply = chatClientBuilder.build()
    .prompt()
    .system("你是一个友好的助手")
    .user("解释 Java 多态")
    .options(OpenAiChatOptions.builder()
        .temperature(0.7).maxTokens(1024).build())
    .call()
    .content();

// ③ ChatClient 生命周期管理
//    - Builder 注入一次（单例）
//    - 每次请求可以 .build() 或 .mutate() 定制版本
//    - 线程安全、无状态（状态在 Advisor 中管理）
```

---

## 2. System Prompt 与 Prompt 模板

### 2.1 外部文件 System Prompt（生产推荐）

```java
@Service
public class ExpertService {
    private final ChatClient chatClient;

    public ExpertService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public String answer(String question) {
        return chatClient.prompt()
            // ★ 从 classpath 加载 System Prompt（版本管理、多环境差异）
            .system(s -> s.text(
                new ClassPathResource("prompts/java-expert.st")))
            .user(question)
            .call()
            .content();
    }
}
```

```text
# prompts/java-expert.st 模板文件
你是 {role}，专精 {specialty}。
回答规则：
1. 先给出结论，再展开解释
2. 必须包含可运行的代码示例
3. 如果问题超出你的知识范围，请坦诚说明
```

### 2.2 动态模板变量

```java
// PromptTemplate + 变量绑定（支持 Map 和 Object）
String template = "你是 {role}，用 {language} 翻译以下文本：{text}";

PromptTemplate pt = new PromptTemplate(template);
Prompt prompt = pt.create(Map.of(
    "role", "专业翻译",
    "language", "英文",
    "text", "Spring AI 让 Java 开发者轻松接入大模型"
));
String reply = chatClient.prompt(prompt).call().content();
```

---

## 3. 流式输出 SSE：从 Flux 到前端

```java
@RestController
public class StreamController {
    private final ChatClient chatClient;

    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> stream(@RequestParam String message) {
        return chatClient.prompt()
            .user(message)
            .stream()
            .content()                               // Flux<String>
            .map(chunk -> ServerSentEvent.<String>builder()
                .data(chunk).build())                // → SSE 格式
            .concatWith(Flux.just(                   // 结束标记
                ServerSentEvent.<String>builder()
                    .event("done").data("[DONE]").build()));
    }
}
```

```javascript
// 前端消费 SSE（原生 fetch + ReadableStream）
const response = await fetch("/chat/stream?message=hello");
const reader = response.body.getReader();
while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    const text = new TextDecoder().decode(value);
    appendToChat(text);  // 逐 chunk 追加
}
```

---

## 4. 多轮对话记忆：MessageChatMemoryAdvisor

```java
// ★ Spring AI 2.0 中记忆由 Advisor 管理（非手动 List<Message>）

@Configuration
public class MemoryConfig {

    // ① 简单版：内存存储（开发/单机）
    @Bean
    public ChatMemory chatMemory() {
        return new InMemoryChatMemory();
    }

    // ② 生产版：Redis 持久化
    @Bean
    public ChatMemory redisChatMemory(RedisTemplate<String, Object> redis) {
        return new RedisChatMemory(redis);
    }

    @Bean
    public MessageChatMemoryAdvisor memoryAdvisor(ChatMemory chatMemory) {
        return MessageChatMemoryAdvisor.builder(chatMemory)
            .conversationId("default")           // 默认会话 ID
            .maxMessages(20)                     // 滑动窗口：只记最近 20 条
            .build();
    }
}

// ③ 使用：注入 Advisor → 每次调用自动管理历史
@RestController
public class ChatController {
    private final ChatClient chatClient;

    public ChatController(ChatClient.Builder builder, MessageChatMemoryAdvisor memoryAdvisor) {
        this.chatClient = builder.defaultAdvisors(memoryAdvisor).build();
    }

    @PostMapping("/chat")
    public String chat(@RequestBody ChatRequest req) {
        return chatClient.prompt()
            .advisors(a -> a.param("chat_memory_conversation_id", req.sessionId()))
            .user(req.message())
            .call()
            .content();
    }
}
```

---

## 5. 结构化输出：JSON/Bean/Entity

```java
// ① 简单版：BeanOutputConverter（基于 JSON Schema）
record Movie(String title, String director, int year) {}

BeanOutputConverter<Movie> converter = new BeanOutputConverter<>(Movie.class);

String reply = chatClient.prompt()
    .user("《肖申克的救赎》的导演是谁？哪一年拍的？")
    .call()
    .content();

Movie movie = converter.convert(reply);  // 自动反序列化

// ② 高级版：直接调用 .entity()（推荐，Spring AI 2.0）
Movie movie2 = chatClient.prompt()
    .user("《肖申克的救赎》的导演是谁？哪一年拍的？")
    .call()
    .entity(Movie.class);               // 一行搞定

// ③ 列表结构化
List<Movie> movies = chatClient.prompt()
    .user("列出诺兰导演的 3 部代表作")
    .call()
    .entity(new ParameterizedTypeReference<List<Movie>>() {});
```

---

## 6. Advisors 拦截链

```java
// Advisors = Spring MVC Interceptor 的 AI 版本 — 洋葱模型拦截请求/响应

ChatClient client = ChatClient.builder(model)
    .defaultAdvisors(
        new SimpleLoggerAdvisor(),              // ① 日志（order=100）
        new MessageChatMemoryAdvisor(memory),   // ② 记忆注入（order=1000+）
        new QuestionAnswerAdvisor(vectorStore), // ③ RAG 检索（order=1001+）
        new SafeGuardAdvisor()                  // ④ 安全护栏（order=LOWEST-1）
    )
    .build();

// 执行顺序：①→②→③→④→LLM→④→③→②→①（洋葱模型）
// 请求：正序（order 小→大）
// 响应：逆序（order 大→小）
```

**追问:** `defaultAdvisors` vs 动态 `advisors()`：
```java
// defaultAdvisors：全局生效，每次调用都执行
ChatClient baseClient = builder.defaultAdvisors(memoryAdvisor).build();

// 动态 advisors()：单次调用覆盖
baseClient.prompt()
    .advisors(a -> a.param("chat_memory_conversation_id", sessionId))  // 改会话
    .user(msg)
    .call();
```

---

## 7. 多用户会话隔离

```java
@RestController
public class MultiUserChatController {
    private final ChatClient chatClient;

    @PostMapping("/chat/{userId}")
    public String chat(@PathVariable String userId, @RequestBody String message) {
        return chatClient.prompt()
            .advisors(a -> a
                .param("chat_memory_conversation_id", userId)  // ★ 按用户隔离
            )
            .user(message)
            .call()
            .content();
    }
    // 每个 userId 独立维护自己的对话历史
    // Redis 版 ChatMemory 中 key 格式：chat:memory:{userId}
}
```

---

> 🎯 **核心要点**：Spring AI 对话管理的四个核心机制 — **① ChatClient 统一入口 ② MessageChatMemoryAdvisor 自动管理多轮记忆（替代手动 List<Message>）③ .entity() 结构化输出 ④ Advisors 洋葱链（日志→记忆→RAG→安全）**。多用户隔离只需改 `chat_memory_conversation_id` 一个参数。

**下一模块**：[03-Spring AI RAG实战](03-Spring-AI-RAG实战.md) / **返回总览**：[00-总览](00-Java-AI开发框架体系总览.md)
