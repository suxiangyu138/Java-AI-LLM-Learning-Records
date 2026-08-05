# 05 - Spring AI 多模态与生产

> 🎯 Spring AI 不止文本 — 图片理解、语音合成、Actuator 监控、AOT 原生编译。这是从 Demo 到生产的最后一公里

## 1. 多模态：图片理解

```java
@RestController
public class VisionController {
    
    @GetMapping("/analyze-image")
    public String analyzeImage(@RequestParam String imageUrl, 
                                @RequestParam String question) {
        var userMessage = new UserMessage(question,
            List.of(new Media(MimeType.IMAGE_PNG, 
                new UrlResource(imageUrl)))
        );
        
        return chatClient.prompt()
            .user(userMessage)
            .call()
            .content();
    }
}
```

## 2. 语音合成

```yaml
spring.ai.openai:
  audio:
    speech:
      model: tts-1
      voice: alloy
```

```java
@GetMapping(value = "/speak", produces = "audio/mpeg")
public byte[] speak(@RequestParam String text) {
    SpeechPrompt prompt = new SpeechPrompt(text);
    SpeechResponse response = speechClient.call(prompt);
    return response.getResult().getOutput();  // byte[] MP3
}
```

## 3. Actuator 监控

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,ai
  metrics:
    tags:
      application: ${spring.application.name}
```

```text
Spring AI 提供的 Actuator 端点：

  /actuator/ai/chat  — Chat 模型信息
  /actuator/metrics  — 调用次数/延迟/Token统计
  /actuator/health   — 模型可用性检查
```

## 4. AOT 编译与 GraalVM

```xml
<!-- 原生编译 → 启动从 5s 降到 0.1s -->
<plugin>
    <groupId>org.graalvm.buildtools</groupId>
    <artifactId>native-maven-plugin</artifactId>
</plugin>
```

```bash
mvn -Pnative native:compile
./target/my-ai-app  # 原生可执行文件，启动 <100ms
```

## 5. 生产配置

```yaml
spring.ai.openai:
  api-key: ${OPENAI_API_KEY}
  chat:
    model: gpt-4o-mini
    temperature: 0.7
    max-tokens: 2048
  retry:
    max-attempts: 3
    backoff:
      initial-interval: 1000
      multiplier: 2

# 连接池
spring.ai.openai.http-client:
  connect-timeout: 10s
  read-timeout: 60s
  max-connections: 50

# 多供应商容灾
spring.ai:
  primary: openai
  fallback:
    - provider: azure-openai
      api-key: ${AZURE_API_KEY}
    - provider: ollama
      base-url: http://localhost:11434
```

## 6. 自定义 Starter

```java
// 封装企业内部 AI 配置为 Starter
@AutoConfiguration
public class CompanyAIAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    public ChatClient companyChatClient() {
        return ChatClient.builder(chatModel)
            .defaultSystem("你是XX公司内部助手")
            .defaultAdvisors(
                new SafeGuardAdvisor(),              // 安全
                new LoggingAdvisor(),                 // 审计
                new QuestionAnswerAdvisor(vectorStore) // RAG
            )
            .build();
    }
}

// 其他团队只需：
// ① 加依赖 <artifactId>company-ai-starter</artifactId>
// ② 注入 ChatClient → 直接用
```
