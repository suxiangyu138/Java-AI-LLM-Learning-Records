# 09 - Spring AI vs LangChain4j 对比

> 🎯 两个框架都能做 Chat/RAG/Agent/Function Calling — 核心差异在于设计哲学和生态整合

## 十维度对比

| 维度 | Spring AI | LangChain4j |
|------|-----------|-------------|
| **开发者** | Spring 官方 | 社区驱动 |
| **设计哲学** | Spring Boot 原生集成 | LangChain 的 Java 移植 |
| **Chat API** | ChatClient（Builder模式） | ChatLanguageModel + AiServices |
| **声明式接口** | ❌ 无动态代理 | ✅ AiServices（核心特色） |
| **RAG** | QuestionAnswerAdvisor | EmbeddingStoreRetriever |
| **Function Calling** | @Tool + MethodToolCallback | @Tool + AiServices 自动集成 |
| **多轮对话** | List<Message> 手动管理 | ChatMemory 自动管理 |
| **流式** | Flux<String> (Reactor) | StreamingResponseHandler |
| **Spring 集成** | ⭐⭐⭐⭐⭐ 原生 | ⭐⭐⭐ 需手动配置 |
| **文档加载** | PagePdfDocumentReader 等 | 30+ 加载器 |
| **多模型** | 统一的 ChatModel 接口 | 各自独立 Builder |
| **版本** | 1.0.0-M6 (里程碑) | 0.36.x |
| **Java版本** | 17+ (Spring Boot 3) | 8+ |

## 核心差异

```text
设计哲学差异：

  Spring AI：让 AI 成为 Spring 生态的一部分
    → 就像 spring-data-redis 一样自然
    → 统一的 ChatModel/EmbeddingClient 抽象
    → @AutoConfiguration 开箱即用

  LangChain4j：把 LangChain 的设计思想带到 Java
    → AiServices 声明式接口（JDK 动态代理）
    → 更接近 Python LangChain 的 API 风格
    → Chain/Agent 的显式组合
```

## 选型决策树

```text
  Spring Boot 项目？
    ├── 是 → Spring AI（原生集成）
    │       ├── 简单 Chat → ChatClient
    │       ├── RAG → QuestionAnswerAdvisor + VectorStore
    │       └── Agent → @Tool + ChatClient
    │
    └── 不是 / 非 Spring → LangChain4j

  需要动态代理式 AI 接口？
    └── LangChain4j（AiServices 是独有特色）

  团队熟悉 Python LangChain？
    └── LangChain4j（API 风格一致，学习成本低）

  追求稳定性和企业支持？
    └── Spring AI（Spring 官方团队维护）
```

## 典型场景推荐

| 场景 | 推荐 | 原因 |
|------|:---:|------|
| Spring Boot + AI Chat | Spring AI | 自动配置 + 一行注入 |
| Spring Boot + RAG | Spring AI | Advisor 链原生集成 |
| 非 Spring 项目 + AI | LangChain4j | 无框架依赖 |
| 复杂 Agent 编排 | LangChain4j | Chain/Agent 更成熟 |
| 多供应商 AI 网关 | Spring AI | 统一 Model 抽象 |
| 快速原型 | LangChain4j | AiServices 声明式最快 |
