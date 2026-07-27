# 02 - Spring AI Chat 与对话管理

> 🎯 对话不只是"一问一答" — Prompt 模板、流式 SSE、多轮对话、System Prompt，Spring AI 全部原生支持

## 1. Prompt 模板

```java
// 使用 PromptTemplate 变量化
String template = """
    你是 {role}，请用 {language} 回答：{question}
    """;

PromptTemplate promptTemplate = new PromptTemplate(template);
Prompt prompt = promptTemplate.create(Map.of(
    "role", "Java 架构师",
    "language", "中文",
    "question", "解释 Spring IoC"
));

String answer = chatClient.call(prompt).getResult().getOutput().getContent();
```

## 2. System Prompt 与多轮对话

```java
@Service
public class ConversationService {
    private final ChatClient chatClient;
    
    // 持久化 System Prompt
    public String chatWithSystem(String userMessage) {
        return chatClient.prompt()
            .system("你是 Java 后端专家，回答简洁，给出代码示例。")
            .user(userMessage)
            .call()
            .content();
    }
    
    // 多轮对话：手动管理历史
    private final List<Message> history = new ArrayList<>();
    
    public String multiTurnChat(String userMessage) {
        history.add(new UserMessage(userMessage));
        
        Prompt prompt = new Prompt(history);
        String response = chatClient.call(prompt)
            .getResult().getOutput().getContent();
        
        history.add(new AssistantMessage(response));
        return response;
    }
}
```

## 3. 流式输出 SSE

```java
@RestController
public class StreamController {
    
    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChat(@RequestParam String message) {
        return chatClient.prompt()
            .user(message)
            .stream()
            .content();  // Flux<String> → Server-Sent Events
    }
}

// 前端消费：
// const eventSource = new EventSource('/chat/stream?message=hello');
// eventSource.onmessage = (e) => appendToChat(e.data);
```

## 4. 输出解析

```java
// BeanOutputConverter：强制输出为 Java 对象
record ActorFilms(String actor, List<String> movies) {}

@RestController
public class StructuredOutputController {
    
    @GetMapping("/actor")
    public ActorFilms getActorFilms(@RequestParam String actor) {
        var converter = new BeanOutputConverter<>(ActorFilms.class);
        String format = converter.getFormat();  // 自动生成 JSON Schema
        
        String response = chatClient.prompt()
            .user("列出 {actor} 主演的3部电影")
            .call()
            .content();
        
        return converter.convert(response);  // 字符串 → ActorFilms
    }
}
```

## 5. Advisors 链

```java
// Advisors = 请求/响应的拦截器链
ChatClient chatClient = ChatClient.builder(model)
    .defaultAdvisors(
        new SimpleLoggerAdvisor(),        // 日志
        new QuestionAnswerAdvisor(vectorStore),  // RAG
        new SafeGuardAdvisor()            // 安全检查
    )
    .build();

// 自定义 Advisor
public class SafeGuardAdvisor implements RequestResponseAdvisor {
    @Override
    public AdvisedResponse adviseResponse(AdvisedRequest req, ChatClient client) {
        String response = client.call(req).content();
        if (containsSensitiveInfo(response)) {
            return new AdvisedResponse("【内容已过滤】");
        }
        return new AdvisedResponse(response);
    }
}
```
