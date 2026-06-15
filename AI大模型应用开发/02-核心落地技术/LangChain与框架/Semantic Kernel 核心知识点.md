# ⛓️ Semantic Kernel (Java) 核心知识点

> **核心摘要**：Semantic Kernel（SK）是微软开源的轻量级 AI 编排框架，Java 版本保持了与 .NET/Python 版本一致的设计理念。核心特点是轻量级、多模态支持和与 Azure AI 服务的深度集成，适合跨语言协同和多模态场景。

**前置阅读**：[[LangChain4j 核心知识点]] | [[Genkit Java 核心知识点]]

---

## 一、概述

**Semantic Kernel（SK）** 是微软开源的轻量级 AI 编排框架。核心定位：微软生态的轻量级 AI 编排框架，适合跨语言协同和多模态场景。

## 二、核心概念

### 2.1 架构

```
Kernel（内核）
├── Plugins（插件）
├── Planner（规划器）
├── Memories（记忆）
├── Connectors（连接器）
└── Functions（函数）
```

### 2.2 核心组件

| 组件 | 说明 |
|------|------|
| **Kernel** | 核心调度器，管理插件和 AI 服务的注册与调用 |
| **Plugin** | 可复用的 AI/业务功能单元 |
| **Planner** | 自动编排多个 Plugin 实现复杂目标 |
| **Function** | Plugin 中的最小执行单元（Semantic / Native Function） |
| **Memory** | 向量化记忆存储，支持语义检索 |

## 三、快速上手

### 3.1 依赖

```xml
<dependency>
    <groupId>com.microsoft.semantic-kernel</groupId>
    <artifactId>semantickernel-api</artifactId>
    <version>1.6.0</version>
</dependency>
<dependency>
    <groupId>com.microsoft.semantic-kernel</groupId>
    <artifactId>semantickernel-aiservices-openai</artifactId>
    <version>1.6.0</version>
</dependency>
```

### 3.2 基础使用

```java
Kernel kernel = Kernel.builder()
    .withAIService(ChatCompletion.class,
        OpenAIChatCompletion.builder()
            .withModelId("gpt-4o")
            .withApiKey(System.getenv("OPENAI_API_KEY"))
            .build())
    .build();

public class TimePlugin {
    @KernelFunction("获取当前日期")
    public String getDate() {
        return LocalDate.now().toString();
    }
}

kernel.importPluginFromObject(new TimePlugin(), "TimePlugin");
String result = kernel.invokePrompt("现在是 {{TimePlugin.getDate}}，几点了？")
    .getResult();
```

### 3.3 Planner 自动编排

```java
kernel.importPluginFromObject(new FilePlugin(), "FilePlugin");
kernel.importPluginFromObject(new EmailPlugin(), "EmailPlugin");

Planner planner = new SequentialPlanner(kernel);
Plan plan = planner.createPlanAsync(userRequest);
String result = plan.invokeAsync(kernel);
```

## 四、核心特性

### 4.1 多模态支持

| 模态 | 支持状态 |
|------|----------|
| 文本 | 完整支持 |
| 代码生成 | 完整支持 |
| 图像理解 | 通过 GPT-4V / Gemini 支持 |
| 文本→图像 | 通过 DALL-E 插件支持 |

### 4.2 连接器生态

| 连接器 | 用途 |
|--------|------|
| OpenAI | GPT-4 / GPT-4o |
| Azure OpenAI | Azure 托管的 GPT 服务 |
| HuggingFace | 开源模型 |
| Google Gemini | Google AI 服务 |
| Ollama | 本地开源模型 |
| Qdrant / Pinecone / Redis | 向量存储 |

### 4.3 Memory 语义记忆

```java
var memory = new VolatileMemoryStore();
var semanticTextMemory = new SemanticTextMemory(memory,
    OpenAITextEmbedding.builder().withApiKey(key).build());

semanticTextMemory.saveInformation("collection1",
    "用户偏好：代码审查要求检查线程安全", "user-pref-01");

var results = semanticTextMemory.search("collection1",
    "并发问题检查", 5, 0.7);
```

## 五、框架对比

| 维度 | Semantic Kernel | LangChain4j | Spring AI |
|------|----------------|-------------|-----------|
| 所属生态 | **微软 / Azure** | 社区驱动 | Spring |
| 轻量级 | **是** | 中等 | 中等 |
| 多模态 | **强**（文本+代码+图像） | 文本为主 | 文本为主 |
| Planner 编排 | 内置 | Agent 机制 | 基础 |
| Azure 集成 | **原生** | 需适配 | 需适配 |
| 学习曲线 | 低 | 中等 | 最低（Spring 用户） |

## 六、适用场景

- **Azure 云用户**：与 Azure OpenAI / Cognitive Services 无缝集成
- **跨语言团队**：.NET + Python + Java 团队使用统一框架
- **轻量级集成**：不想引入重框架，只需简单的 LLM 调用编排
- **多模态应用**：需要文本、代码、图像混合处理的场景

---

## 核心要点回顾

- Semantic Kernel 的核心优势：微软/Azure 生态原生集成、跨语言统一架构、多模态支持
- 四大组件：Kernel（调度器）+ Plugin（功能单元）+ Planner（编排）+ Memory（记忆）
- 与 LangChain4j 对比：SK 更轻量、多模态更强；LangChain4j Agent 更灵活
- 最适合：Azure 云用户和跨语言团队

## 参考资料

1. [[LangChain4j 核心知识点]]
2. [[Genkit Java 核心知识点]]
3. [[快速吃透 LangChain]]
