# Genkit Java 核心知识点

## 一、概述

Genkit Java 是 Google 推出的全栈式生成式 AI 框架，Genkit 的 Java 语言实现。它在 Google 内部经过大规模验证，在 **高性能推理** 和 **深度优化** 方面具有显著优势。2026年，Genkit 已成为金融、医疗等高性能场景的首选方案之一。

**核心定位：** Google 生态的高性能生成式 AI 框架，Java 原生支持，适合自建/私有化部署大模型的场景。

## 二、核心特性

### 2.1 关键能力

| 特性 | 说明 |
|------|------|
| **高性能推理** | 对推理 Pipeline 进行深度优化，延迟和吞吐优于同类框架 |
| **Flow（流程）** | 有向无环图（DAG）编排多个 AI 步骤 |
| **可观测性** | 内置 OpenTelemetry 集成，全链路追踪 |
| **插件生态** | 向量数据库、模型提供商等丰富插件 |
| **Prompt 管理** | 集中式 Prompt 模板管理与版本控制 |
| **评估框架** | 内置 Eval 工具，自动化评估生成质量 |

### 2.2 架构

```
┌──────────────────────────────────────────────┐
│              应用层                           │
│  ┌──────────┬──────────┬──────────────────┐  │
│  │ Chat     │  RAG     │  Agent / Flow    │  │
│  └──────────┴──────────┴──────────────────┘  │
├──────────────────────────────────────────────┤
│              Genkit Core                     │
│  ┌──────────┬──────────┬──────────────────┐  │
│  │ Model    │ Prompt   │  Flow            │  │
│  │ Registry │ Library  │  Engine          │  │
│  └──────────┴──────────┴──────────────────┘  │
├──────────────────────────────────────────────┤
│              插件层                           │
│  ┌──────────┬──────────┬──────────────────┐  │
│  │ Gemini   │ VertexAI │  Ollama / 开源   │  │
│  └──────────┴──────────┴──────────────────┘  │
└──────────────────────────────────────────────┘
```

## 三、快速上手

### 3.1 Maven 依赖

```xml
<dependency>
    <groupId>com.google.genkit</groupId>
    <artifactId>genkit</artifactId>
    <version>0.1.0</version>
</dependency>
<dependency>
    <groupId>com.google.genkit</groupId>
    <artifactId>genkit-google-cloud</artifactId>
    <version>0.1.0</version>
</dependency>
```

### 3.2 基础调用

```java
// 配置 Genkit
Genkit genkit = Genkit.builder()
    .withPlugin(new GoogleCloudPlugin("my-gcp-project", "us-central1"))
    .build();

// 获取模型
GenerativeModel model = genkit.getGenerativeModel("gemini-1.5-pro");

// 生成
GenerateResponse response = model.generate("解释CAP理论");
System.out.println(response.text());
```

### 3.3 Flow 编排

```java
@Flow(name = "document-qa-flow")
public class DocumentQAFlow {

    @Step(name = "retrieve-docs")
    public List<Document> retrieve(String query) {
        return retriever.search(query, 5);
    }

    @Step(name = "generate-answer")
    public String generate(List<Document> docs, String query) {
        String context = docs.stream()
            .map(Document::text)
            .collect(Collectors.joining("\n"));
        return model.generate("""
            基于以下文档回答问题：
            %s
            
            问题：%s
            """.formatted(context, query)).text();
    }

    public String execute(String query) {
        List<Document> docs = retrieve(query);
        return generate(docs, query);
    }
}
```

### 3.4 Prompt 管理

```java
// 定义 Prompt 模板（可在 Genkit Studio 中管理）
@Prompt(name = "java-code-review")
public class CodeReviewPrompt {
    @PromptTemplate
    String template = """
        你是一位资深Java架构师。请审查以下代码：
        - 关注线程安全、资源管理、性能
        - 给出具体改进建议
        
        代码：
        {{code}}
        """;
}

// 使用
PromptRenderer renderer = genkit.getPrompt("java-code-review");
String review = renderer.render(Map.of("code", sourceCode));
GenerateResponse response = model.generate(review);
```

## 四、核心优势

### 4.1 性能优化

- **推理 Pipeline 编译优化**：将 Flow 编译为优化的执行图
- **请求批处理**：自动合并并发的模型请求，减少 API 调用次数
- **响应缓存**：语义缓存，相同/相似问题的结果复用
- **流式优先**：默认使用 SSE 流式响应，减少首字延迟

### 4.2 可观测性

```java
// 内置 OpenTelemetry 集成
// 自动追踪每个 Step 的耗时、Token 用量、错误
Genkit genkit = Genkit.builder()
    .withPlugin(new GoogleCloudPlugin(projectId, region))
    .withEnableTracing(true)
    .build();

// 自动在 Cloud Trace 中生成完整的 AI 调用链路
```

### 4.3 评估框架

```java
@Eval(name = "qa-accuracy")
public void evaluateQA() {
    List<TestCase> cases = loadTestCases("qa_dataset.jsonl");
    
    for (TestCase tc : cases) {
        String actual = documentQAFlow.execute(tc.query);
        EvalResult result = judge.evaluate(tc.expected, actual);
        EvalReporter.record(tc.id(), result);
    }
}
```

## 五、与其他框架对比

| 维度 | Genkit Java | LangChain4j | Spring AI |
|------|-------------|-------------|-----------|
| 所属生态 | Google Cloud | 社区驱动 | Spring (VMware) |
| 性能 | **最高** | 中等 | 中等 |
| Agent 能力 | Flow 编排（DAG） | **最灵活** | 基础 |
| 可观测性 | **内置 OpenTelemetry** | 需自行集成 | 需 Spring Actuator |
| 学习曲线 | 中等 | 中等 | **最低** |
| 适用场景 | 高性能/自建模型 | 复杂 Agent | Spring 集成 |

## 六、适用场景

- **金融推理服务**：低延迟、高并发的 AI 推理需求
- **医疗 AI 应用**：全链路可追踪，满足合规要求
- **Google Cloud 用户**：与 Vertex AI、Cloud Run 原生集成
- **自建模型团队**：Flow 引擎优化自建的 Serving Pipeline

## 七、总结

Genkit Java 是 Google 云生态 AI 推理的最佳入口。其核心竞争力在于 **高性能 Flow 引擎** 和 **内置可观测性**。如果项目对推理延迟敏感，或者已深度使用 GCP，Genkit Java 是最优选择。
