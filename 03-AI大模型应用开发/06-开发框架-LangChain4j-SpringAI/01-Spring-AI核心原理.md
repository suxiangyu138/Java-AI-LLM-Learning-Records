# 01 - Spring AI 核心原理

> 🎯 Spring AI = Spring 生态的 AI 集成层 — 统一 ChatClient 接口屏蔽 OpenAI/Claude/Ollama 差异、自动配置开箱即用

## 1. 架构设计

```text
Spring AI 分层架构：

  ┌─────────────────────────────────────────────┐
  │  应用层：@Service / @RestController         │
  ├─────────────────────────────────────────────┤
  │  Spring AI API                              │
  │  ChatClient / EmbeddingClient / ImageClient │
  │  VectorStore / DocumentReader / Tool        │
  ├─────────────────────────────────────────────┤
  │  Adapter 层（屏蔽多供应商差异）              │
  │  OpenAI | Azure | Ollama | Anthropic        │
  │  Gemini | 智谱 | 通义 | DeepSeek            │
  └─────────────────────────────────────────────┘
```

## 2. 快速开始

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-openai</artifactId>
    <version>1.0.0-M6</version>
</dependency>
```

```yaml
spring.ai.openai:
  api-key: ${OPENAI_API_KEY}
  chat:
    model: gpt-4o-mini
    temperature: 0.7
```

```java
@RestController
public class ChatController {
    
    private final ChatClient chatClient;
    
    public ChatController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }
    
    @GetMapping("/chat")
    public String chat(@RequestParam String message) {
        return chatClient.prompt()
            .user(message)
            .call()
            .content();
    }
}
```

## 3. 多模型统一 API

```java
// Spring AI 抽象层 → 切换模型只需改配置
public interface ChatModel {
    ChatResponse call(Prompt prompt);
}

// 同样的代码，不同的实现
// OpenAI → spring.ai.openai.api-key=sk-xxx
// Ollama → spring.ai.ollama.chat.model=qwen2:7b
// 无需改代码！
```

### 支持的模型提供商

| 提供商 | Starter | 支持 Chat | Embedding | 图片 |
|--------|---------|:---:|:---:|:---:|
| OpenAI | `spring-ai-starter-openai` | ✅ | ✅ | ✅ |
| Azure OpenAI | `spring-ai-starter-azure-openai` | ✅ | ✅ | — |
| Ollama | `spring-ai-starter-ollama` | ✅ | ✅ | — |
| Anthropic | `spring-ai-starter-anthropic` | ✅ | — | — |
| 智谱 GLM | `spring-ai-starter-zhipuai` | ✅ | ✅ | — |
| 通义千问 | `spring-ai-starter-qianfan` | ✅ | ✅ | — |

## 4. Embedding 统一 API

```java
@RestController
public class EmbeddingController {
    private final EmbeddingClient embeddingClient;
    
    @GetMapping("/embed")
    public List<Double> embed(@RequestParam String text) {
        return embeddingClient.embed(text);  // 返回 List<Double>
    }
}
```

## 5. 自动配置原理

```java
// Spring Boot 自动配置 → 零代码启动
@AutoConfiguration
public class OpenAiAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    public ChatClient chatClient(OpenAiApi openAiApi) {
        return new OpenAiChatClient(openAiApi);
    }
    
    @Bean
    public OpenAiApi openAiApi(OpenAiProperties props) {
        return new OpenAiApi(props.getApiKey());
    }
}

// 只需加依赖 → 配 application.yml → 注入即可用
```

## 6. 完整示例：统一 Chat 服务

```java
@Service
public class AIService {
    
    @Value("${spring.ai.provider:openai}")
    private String provider;
    
    @Autowired
    private Map<String, ChatClient> chatClients;  // 注入所有提供商
    
    public String chat(String message, String modelProvider) {
        ChatClient client = chatClients.getOrDefault(
            modelProvider, chatClients.get(provider)
        );
        return client.prompt().user(message).call().content();
    }
}
```
