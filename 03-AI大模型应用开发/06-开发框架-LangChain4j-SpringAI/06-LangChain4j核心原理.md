# 06 - LangChain4j 核心原理

> 🎯 LangChain4j = LangChain 的 Java 实现 — 如果你熟悉 Python LangChain，这就是你在 Java 世界的"家"

## 1. 核心架构

```text
LangChain4j 分层：

  ┌──────────────────────────────────────────┐
  │  AiServices (声明式 AI 接口)              │
  ├──────────────────────────────────────────┤
  │  Chains / Agents / RAG                   │
  ├──────────────────────────────────────────┤
  │  ChatLanguageModel / EmbeddingModel      │
  │  ChatMemory / Tool / DocumentLoader      │
  ├──────────────────────────────────────────┤
  │  Adapter: OpenAI | Ollama | Claude | ... │
  └──────────────────────────────────────────┘
```

## 2. 快速开始

```xml
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-open-ai</artifactId>
    <version>0.36.2</version>
</dependency>
```

```java
// 最简调用
var model = OpenAiChatModel.builder()
    .apiKey(System.getenv("OPENAI_API_KEY"))
    .modelName("gpt-4o-mini")
    .build();

String answer = model.generate("解释 Java 多态");
```

## 3. AiServices 声明式接口

```java
// 定义接口 → LangChain4j 自动实现！
interface Assistant {
    @SystemMessage("你是 Java 后端专家，回答简洁")
    String chat(String userMessage);
}

// 创建实例（动态代理）
Assistant assistant = AiServices.create(Assistant.class, model);
String answer = assistant.chat("解释 Spring IoC");
```

### 进阶：带 Tool 的 AiService

```java
interface CalculatorAgent {
    @SystemMessage("你是数学助手，可以用计算工具")
    String calculate(String question);
}

class Calculator {
    @Tool("计算数学表达式")
    double calc(@P("表达式") String expr) {
        return new ExpressionEvaluator().evaluate(expr);
    }
}

CalculatorAgent agent = AiServices.builder(CalculatorAgent.class)
    .chatLanguageModel(model)
    .tools(new Calculator())  // 注册工具
    .build();

// "123 × 456 等于多少？" → LLM 自动调用 calc("123*456")
```

## 4. ChatMemory 对话记忆

```java
// 自动管理多轮对话历史
ChatMemory memory = MessageWindowChatMemory.withMaxMessages(20);

Assistant assistant = AiServices.builder(Assistant.class)
    .chatLanguageModel(model)
    .chatMemory(memory)     // ← 自动记住历史
    .build();

// 第一轮
assistant.chat("我叫张三");    // LLM 记住了这个名字
// 第二轮
assistant.chat("我叫什么？");  // → "你叫张三" ✅
```

## 5. 多模型支持

```java
// 一行切换模型
var openai = OpenAiChatModel.builder().apiKey(key).build();
var ollama = OllamaChatModel.builder()
    .baseUrl("http://localhost:11434")
    .modelName("qwen2:7b").build();
var claude = AnthropicChatModel.builder().apiKey(key).build();

// 或从配置动态创建
ChatLanguageModel model = ModelFactory.create(
    provider, apiKey, modelName
);
```

## 6. 流式输出

```java
// 流式回调
model.generate("写一首诗", new StreamingResponseHandler<>() {
    @Override
    public void onNext(String token) {
        System.out.print(token);  // 逐 token 输出
    }
    @Override
    public void onComplete(Response<Token> response) {
        System.out.println("\n[完成]");
    }
});
```
