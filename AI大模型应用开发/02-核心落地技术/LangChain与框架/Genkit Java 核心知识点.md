# ⛓️ Genkit Java 核心知识点

> **核心摘要**：Genkit Java 是 Google 推出的全栈式生成式 AI 框架，在 Google 内部经过大规模验证，核心优势在于高性能推理（Flow 引擎编译优化）和内置可观测性（OpenTelemetry 集成）。2026 年，Genkit 已成为金融、医疗等高性能场景的首选方案之一。

**前置阅读**：[[LangChain4j 核心知识点]] | [[Semantic Kernel 核心知识点]]

---

## 一、概述

**Genkit Java** 是 Google 推出的全栈式生成式 AI 框架的 Java 语言实现。核心定位：Google 生态的高性能生成式 AI 框架，Java 原生支持，适合自建/私有化部署大模型的场景。

## 二、核心特性

| 特性 | 说明 |
|------|------|
| **高性能推理** | 推理 Pipeline 深度优化，延迟和吞吐优于同类框架 |
| **Flow（流程）** | DAG 编排多个 AI 步骤 |
| **可观测性** | 内置 OpenTelemetry 集成，全链路追踪 |
| **插件生态** | 向量数据库、模型提供商等丰富插件 |
| **Prompt 管理** | 集中式 Prompt 模板管理与版本控制 |
| **评估框架** | 内置 Eval 工具，自动化评估生成质量 |

### 架构

```
应用层：Chat | RAG | Agent / Flow
Genkit Core：Model Registry | Prompt Library | Flow Engine
插件层：Gemini | VertexAI | Ollama / 开源
```

## 三、快速上手

### 3.1 依赖

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
Genkit genkit = Genkit.builder()
    .withPlugin(new GoogleCloudPlugin("my-gcp-project", "us-central1"))
    .build();

GenerativeModel model = genkit.getGenerativeModel("gemini-1.5-pro");
GenerateResponse response = model.generate("解释 CAP 理论");
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
        String context = docs.stream().map(Document::text)
            .collect(Collectors.joining("\n"));
        return model.generate("基于以下文档回答问题：\n%s\n\n问题：%s"
            .formatted(context, query)).text();
    }

    public String execute(String query) {
        return generate(retrieve(query), query);
    }
}
```

### 3.4 Prompt 管理

```java
@Prompt(name = "java-code-review")
public class CodeReviewPrompt {
    @PromptTemplate
    String template = """
        你是一位资深Java架构师。请审查以下代码：
        - 关注线程安全、资源管理、性能
        - 给出具体改进建议
        代码：{{code}}
        """;
}
```

## 四、核心优势

### 4.1 性能优化

| 特性 | 说明 |
|------|------|
| 推理 Pipeline 编译优化 | Flow 编译为优化的执行图 |
| 请求批处理 | 自动合并并发的模型请求 |
| 响应缓存 | 语义缓存，相同/相似问题结果复用 |
| 流式优先 | 默认 SSE 流式响应 |

### 4.2 可观测性

内置 OpenTelemetry，自动追踪每个 Step 的耗时、Token 用量、错误，在 Cloud Trace 中生成完整的 AI 调用链路。

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

## 五、框架对比

| 维度 | Genkit Java | LangChain4j | Spring AI |
|------|-------------|-------------|-----------|
| 所属生态 | Google Cloud | 社区驱动 | Spring (VMware) |
| 性能 | **最高** | 中等 | 中等 |
| Agent 能力 | Flow 编排（DAG） | **最灵活** | 基础 |
| 可观测性 | **内置 OpenTelemetry** | 需自行集成 | 需 Spring Actuator |
| 学习曲线 | 中等 | 中等 | **最低** |

## 六、适用场景

- **金融推理服务**：低延迟、高并发的 AI 推理需求
- **医疗 AI 应用**：全链路可追踪，满足合规要求
- **Google Cloud 用户**：与 Vertex AI、Cloud Run 原生集成
- **自建模型团队**：Flow 引擎优化自建的 Serving Pipeline

---

## 核心要点回顾

- Genkit Java 是 Google 云生态 AI 推理的最佳入口
- 核心竞争力：高性能 Flow 引擎和内置可观测性
- 特色：Flow 编排（DAG）、Prompt 版本管理、Eval 评估框架
- 最适合场景：对推理延迟敏感或深度使用 GCP 的项目

## 参考资料

1. [[LangChain4j 核心知识点]]
2. [[Semantic Kernel 核心知识点]]
3. [[LangChain详细知识点]]
