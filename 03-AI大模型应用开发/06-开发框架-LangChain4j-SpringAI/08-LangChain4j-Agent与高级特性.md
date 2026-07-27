# 08 - LangChain4j Agent 与高级特性

> 🎯 多个 @Tool + AiServices = Java Agent。流式输出、自定义 Chain、Spring Boot 集成 — LangChain4j 的高级玩法

## 1. Agent = LLM + Tools

```java
// 定义多个工具
class DevTools {
    @Tool("搜索内部代码库") String searchCode(@P("关键词") String q) { ... }
    @Tool("查询数据库") List<Map> queryDB(@P("SQL") String sql) { ... }
    @Tool("执行Python代码") String runPython(@P("代码") String code) { ... }
    @Tool("发送企微消息") String sendWechatMsg(@P("内容") String msg) { ... }
}

// 声明式 Agent 接口
interface DevAgent {
    @SystemMessage("你是全栈开发助手，可查代码/查库/执行脚本/发消息")
    String execute(String task);
}

// 创建 Agent
DevAgent agent = AiServices.builder(DevAgent.class)
    .chatLanguageModel(model)
    .tools(new DevTools())
    .build();

// Agent 自主决策使用哪些工具
agent.execute("查数据库找到最近7天的新用户，发企微消息通知团队");
```

## 2. 自定义 Chain

```java
// Chain = 一系列步骤的串联
Chain<String, String> chain = SequentialChain.<String, String>builder()
    // Step 1: 翻译成英文
    .addStep(input -> model.generate("翻译成英文：" + input))
    // Step 2: 润色
    .addStep(input -> model.generate("使以下文字更正式：" + input))
    // Step 3: 摘要
    .addStep(input -> model.generate("用一句话总结：" + input, 50))
    .build();

String result = chain.execute("今天天气真好呀，适合出去玩");
```

## 3. 流式 + Tool 调用

```java
// 流式 Agent → 逐 token 输出
StreamingChatLanguageModel streamingModel = OpenAiStreamingChatModel
    .builder().apiKey(key).modelName("gpt-4o").build();

TokenStream stream = AiServices.builder(Assistant.class)
    .streamingChatLanguageModel(streamingModel)
    .tools(new Calculator())
    .build()
    .chat("123 × 456 等于多少？");

stream.onNext(token -> System.out.print(token))
      .onComplete(r -> System.out.println("\n[完成]"))
      .start();
```

## 4. Spring Boot 集成

```yaml
langchain4j.open-ai:
  api-key: ${OPENAI_API_KEY}
  chat-model:
    model-name: gpt-4o-mini
    temperature: 0.7
  embedding-model:
    model-name: text-embedding-3-small
```

```java
@SpringBootApplication
public class LangChain4jApp {
    
    @Bean
    public Assistant assistant(ChatLanguageModel model) {
        return AiServices.create(Assistant.class, model);
    }
    
    // 直接注入使用
    @Autowired private Assistant assistant;
}
```

## 5. LangChain4j vs LangChain (Python)

| 特性 | LangChain4j | LangChain (Python) |
|------|:---:|:---:|
| 声明式 AiServices | ✅ 核心特性 | ❌ 需手动实现 |
| Agent 编排 | ✅ | ✅ 更成熟 |
| RAG | ✅ 完整 | ✅ 更丰富 |
| 文档加载器 | 30+ | 80+ |
| 社区活跃度 | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| Java/Spring 集成 | ✅ 原生 | ❌ |
| 版本稳定 | ⚠️ 0.x（快速迭代） | ⚠️ 0.x→1.0 |
