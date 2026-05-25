# AI 大模型结合项目合集（Java 后端 + Ollama + RAG 企业级实战）

本文档整理了 4 个适合 Java 后端方向、同时能体现 AI 大模型应用能力的项目，重点突出你“Java 后端 + AI 大模型应用开发”的差异化竞争力。整体方案围绕 `Spring Boot 3 + Ollama + Redis + MySQL + 向量数据库/ES` 展开，其中 Ollama 提供本地模型推理与 embedding 能力，Spring AI 提供 Java 侧的模型接入封装，Milvus 适合作为 Java 场景下的向量数据库方案 [web:33][page:6][web:38][web:44]。

## 项目总览

| 项目 | 技术栈 | 核心能力 | 简历价值 |
|---|---|---|---|
| Java + Ollama 本地大模型问答接口 | Spring Boot + HttpClient/OkHttp + Ollama | 本地模型调用、参数封装、会话接口设计 | 体现 LLM 接入基础能力 |
| RAG 知识库问答系统（Java版） | Spring Boot + Ollama + Milvus/ES + Redis | 文档解析、向量化、检索增强、上下文拼接 | 体现 AI 应用核心落地能力 |
| AI 智能客服系统 | Spring Boot + Ollama + Redis + RAG | 多轮对话、意图识别、知识匹配、限流熔断 | 体现业务化 AI 系统设计能力 |
| AI 代码生成工具 | Spring Boot + Ollama/OpenAI API + 格式化器 + 静态校验 | Prompt 封装、代码格式化、语法校验、结果回显 | 体现开发提效工具产品化能力 |

---

## 1. Java + Ollama 本地大模型问答接口

### 1.1 项目目标

实现一个基于 Java 的本地大模型问答接口服务，让后端系统能够通过 HTTP 调用本机 Ollama 模型，完成问答、摘要、改写、参数化配置等能力。Ollama 默认通过 `http://localhost:11434` 暴露 API，适合本地部署与内网推理；Spring AI 也提供了对 Ollama 的自动配置与 Java 封装支持 [page:6][web:39][web:45]。

### 1.2 应用场景

- 本地知识问答。
- 简历优化助手。
- 开发文档总结。
- 代码解释与报错排查。
- 低成本内网 AI 服务接入。

### 1.3 架构设计

```text
前端/调用方
   ↓
Spring Boot AI Service
   ↓ HTTP
Ollama Server
   ↓
qwen / llama / mistral / deepseek-r1 等本地模型
```

### 1.4 核心能力

- 对 Ollama HTTP 接口做统一封装。
- 支持模型名、温度、上下文长度等参数配置。
- 支持流式与非流式问答。
- 支持统一异常处理、超时控制、日志记录。
- 对外暴露 RESTful API，便于前端或其他服务调用。

### 1.5 Ollama 接口认知

Ollama 提供本地 HTTP API，其中 embedding 接口为 `POST /api/embed`，请求体至少包含 `model` 和 `input` 字段，并返回 `embeddings` 数组、总耗时、加载耗时等信息；这说明聊天、问答、向量化都可以通过同类 HTTP 调用模式统一封装 [page:5]。

### 1.6 配置文件示例

```yaml
server:
  port: 8080
ollama:
  base-url: http://localhost:11434
  model: qwen2.5:7b
  timeout: 60000
```

### 1.7 请求封装对象

```java
public class ChatRequest {
    private String model;
    private String prompt;
    private Double temperature;
    private Boolean stream;
}
```

### 1.8 Controller 示例

```java
@RestController
@RequestMapping("/api/llm")
public class OllamaController {

    @Resource
    private OllamaService ollamaService;

    @PostMapping("/chat")
    public Map<String, Object> chat(@RequestBody ChatRequest request) {
        return ollamaService.chat(request);
    }
}
```

### 1.9 Service 核心代码骨架

```java
@Service
public class OllamaService {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public Map<String, Object> chat(ChatRequest request) {
        String body = """
                {
                  \"model\": \"%s\",
                  \"prompt\": \"%s\",
                  \"stream\": %s
                }
                """.formatted(request.getModel(), request.getPrompt(), request.getStream());
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:11434/api/generate"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return Map.of("success", true);
    }
}
```

### 1.10 可扩展功能

- 模型切换：支持 `qwen2.5`、`llama3`、`deepseek-r1` 等。
- Prompt 模板管理：区分问答、总结、翻译、代码生成模板。
- Token 限制与敏感词过滤。
- 接口级鉴权、限流、调用日志。

### 1.11 面试亮点

- 完成 Java 后端对本地 LLM 的服务化封装，实现内网可控、低成本推理接入。
- 具备模型参数抽象、接口治理、超时与异常处理能力。
- 为后续 RAG、智能客服、代码生成工具打下统一模型接入基础。

---

## 2. RAG 知识库问答系统（Java版）

### 2.1 项目目标

实现一个 Java 版检索增强生成系统，支持上传 PDF、Word、Markdown、TXT 等文档，完成解析、切分、向量化、向量检索、上下文拼接后问答。Milvus 文档将向量数据库定位为适合语义搜索、RAG 和知识库场景的核心基础设施；Ollama 则可以本地生成 embeddings 供向量检索使用 [web:44][page:5][web:38]。

### 2.2 技术栈

- Spring Boot 3
- Ollama
- Milvus 或 Elasticsearch
- Redis
- MySQL
- Apache Tika / PDFBox / POI

### 2.3 架构设计

```text
文档上传
   ↓
文档解析服务
   ↓
文本切分 Chunk
   ↓
Ollama Embedding
   ↓
Milvus / ES 向量入库
   ↓
用户提问
   ↓
Query Embedding
   ↓
向量检索 TopK
   ↓
拼接上下文 Prompt
   ↓
Ollama 生成答案
```

### 2.4 RAG 核心流程

1. 上传文档。
2. 解析文本。
3. 按段落或固定窗口切分 chunk。
4. 调用 Ollama embedding 模型生成向量。
5. 向量与原文片段一起写入 Milvus/ES。
6. 用户提问时把 query 也转成向量。
7. 检索最相近的 TopK 文本片段。
8. 把检索结果拼接进 prompt，让大模型基于上下文作答。

### 2.5 Embedding 依据

Ollama 官方文档说明 `POST /api/embed` 支持单条或数组文本输入，并返回 `number[][]` 向量结果，可选 `truncate`、`dimensions`、`keep_alive` 等参数；这非常适合批量文档切片向量化场景 [page:5]。

Spring AI 文档进一步说明 `spring.ai.ollama.base-url` 默认就是 `http://localhost:11434`，并可通过 `spring.ai.ollama.embedding.options.model` 指定 embedding 模型，例如 `mxbai-embed-large`，还支持运行时覆盖参数 [page:6]。

### 2.6 Spring AI 配置示例

```yaml
spring:
  ai:
    ollama:
      base-url: http://localhost:11434
      embedding:
        options:
          model: mxbai-embed-large
```

Spring AI 参考文档给出了 `spring-ai-starter-model-ollama` 依赖和 `EmbeddingModel` 注入方式，适合 Java 项目快速接入 Ollama embedding 能力 [page:6]。

### 2.7 依赖建议

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-model-ollama</artifactId>
</dependency>
<dependency>
    <groupId>io.milvus</groupId>
    <artifactId>milvus-sdk-java</artifactId>
    <version>2.6.13</version>
</dependency>
```

Milvus 官方安装文档给出了 Java SDK Maven 坐标 `io.milvus:milvus-sdk-java:2.6.13`，说明 Java 生态对 Milvus 的支持已比较成熟 [web:38]。

### 2.8 文档分片策略

推荐方案：

- 普通文档：每段 300 到 500 字。
- 技术文档：按标题层级 + 段落分片。
- 相邻 chunk 保留 50 到 100 字重叠。
- 存储 `docId`、`chunkId`、`content`、`vector`、`metadata`。

### 2.9 Milvus 集合设计

```text
collection: kb_chunk
fields:
- id
- doc_id
- chunk_id
- content
- vector
- title
- source
```

Milvus Quickstart 说明向量数据库的核心用途就是存储和搜索向量，以支撑语义搜索、RAG 和大规模 AI 应用 [web:44]。

### 2.10 检索问答代码骨架

```java
@Service
public class RagService {

    @Resource
    private EmbeddingModel embeddingModel;

    public String ask(String question) {
        EmbeddingResponse response = embeddingModel.embedForResponse(List.of(question));
        List<Double> queryVector = response.getResults().get(0).getOutput();
        List<String> chunks = searchTopK(queryVector);
        String context = String.join("\n", chunks);
        return callChatModel(buildPrompt(context, question));
    }
}
```

### 2.11 关键优化点

- Chunk 不宜过大，否则召回粒度太粗。
- 检索结果要做去重与重排。
- Prompt 中明确要求“仅基于上下文回答，未知则说不知道”。
- 热门问题结果可缓存到 Redis。
- 文档上传、切分、向量化可异步化处理。

### 2.12 面试亮点

- 使用 Java 构建完整 RAG 链路，而非仅停留在 Python Demo。
- 本地 Ollama + Milvus 方案具备低成本私有化部署优势 [page:5][web:38][web:44]。
- 同时理解 embedding、chunk、TopK 检索、上下文增强与回答约束等 AI 应用关键细节。

---

## 3. AI 智能客服系统

### 3.1 项目目标

实现一个面向电商、校园服务或企业知识问答场景的 AI 智能客服系统，支持多轮对话、意图识别、知识库匹配、人工兜底、接口限流。这个项目比单纯 RAG 更接近真实业务系统，因为它强调会话管理、问题分类、异常兜底与并发治理。

### 3.2 应用场景

- 电商售前售后咨询。
- 校园办事问答。
- 企业内部 IT 帮助台。
- 培训机构课程咨询。

### 3.3 架构设计

```text
Web / 小程序 / 管理后台
   ↓
客服网关层
   ↓
会话服务 + 用户上下文服务
   ↓
意图识别服务
   ├─ FAQ/RAG 检索
   ├─ 工单转人工
   └─ LLM 对话生成
   ↓
Redis + MySQL + 向量库
```

### 3.4 核心能力

- 多轮对话：保存最近 N 轮上下文。
- 意图识别：判断是 FAQ、订单查询、投诉、闲聊还是转人工。
- 知识库匹配：优先 FAQ / RAG，降低模型幻觉。
- 限流保护：按用户、IP、接口维度限流，防止高频刷接口。
- 敏感问题兜底：命中高风险场景时走固定回复或转人工。

### 3.5 会话设计

Redis 可作为短期会话上下文缓存层：

```text
chat:session:{sessionId} -> 最近 10 轮对话 JSON
chat:user:last_intent:{userId} -> 最近一次识别意图
chat:rate:{userId} -> 限流计数器
```

### 3.6 意图识别方案

推荐先做轻量规则 + 大模型分类双层策略：

1. 先基于关键词、正则、菜单点击做粗分类。
2. 无法命中规则时，调用 LLM 做意图分类。
3. 订单类问题走业务接口查询。
4. FAQ 类问题走知识库检索。
5. 高风险问题转人工。

这样做比“所有请求都直接扔给大模型”更稳，也更像企业真实系统。

### 3.7 对话上下文管理

- 仅保留最近 5 到 10 轮消息。
- 对超长历史做摘要压缩。
- Prompt 中显式区分系统提示、历史消息、检索上下文、用户问题。
- 回复后把摘要和最新消息同步写入 Redis。

### 3.8 接口限流

客服系统通常会面临高频轮询或恶意刷问，因此必须加限流。可以基于 Redis 实现固定窗口或滑动窗口，对每个用户每分钟请求数做限制；超限后返回统一提示，保护 Ollama 推理服务稳定性。

### 3.9 示例接口设计

```text
POST /api/chat/send
POST /api/chat/intent
GET  /api/chat/history/{sessionId}
POST /api/chat/transfer
```

### 3.10 多轮问答代码骨架

```java
@Service
public class CustomerAiService {

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    public String chat(String sessionId, String message) {
        List<String> history = loadHistory(sessionId);
        String intent = detectIntent(message, history);
        if ("ORDER_QUERY".equals(intent)) {
            return queryOrder(message);
        }
        if ("FAQ".equals(intent)) {
            return ragAnswer(message, history);
        }
        return llmAnswer(message, history);
    }
}
```

### 3.11 推荐扩展能力

- 接入工单系统，实现转人工。
- 对接订单、物流、退款接口。
- 增加情绪识别与投诉分流。
- 增加会话评价与客服质检后台。
- 对敏感词、违规问题进行审计。

### 3.12 面试亮点

- 不只是“接大模型接口”，而是完整考虑多轮对话、业务分流、知识库检索、限流保护。
- 体现 Java 后端工程化能力与 AI 应用落地能力的结合。
- 适合包装成“企业知识问答客服平台”或“电商智能客服平台”项目。

---

## 4. AI 代码生成工具

### 4.1 项目目标

实现一个面向开发者的 AI 代码生成工具，支持自然语言生成 Java/Python/SQL/前端代码，并对输出结果进行格式化、语法校验和安全过滤。这个项目很适合你，因为它直接贴合“Java 后端 + AI 提效工具”路线。

### 4.2 应用场景

- 根据需求生成 Controller / Service / Mapper 初版代码。
- 生成 SQL、测试用例、接口文档。
- 自动补全重复模板代码。
- 对已有代码做解释、重构、优化建议。

### 4.3 架构设计

```text
前端编辑器 / Web IDE
   ↓
代码生成服务
   ├─ Prompt 模板管理
   ├─ 模型调用层
   ├─ 代码格式化器
   ├─ 语法校验器
   └─ 风险内容过滤
   ↓
Ollama / 第三方大模型 API
```

### 4.4 核心能力

- Prompt 模板：按语言、场景封装生成模板。
- 代码格式化：Java 用 Google Java Format，前端可接 Prettier。
- 语法校验：对生成结果做基础编译或解析检查。
- 结果回显：展示原始输出、格式化输出、错误提示。
- 历史记录：保存最近生成记录，便于回溯。

### 4.5 Prompt 模板示例

```text
你是一个严格的 Java 后端开发助手。
请根据以下需求生成 Spring Boot 3 代码：
1. 仅输出代码
2. 不输出解释
3. Controller、Service、DTO 分层清晰
4. 默认使用 MySQL
需求：{userRequirement}
```

### 4.6 接口设计

```text
POST /api/code/generate
POST /api/code/format
POST /api/code/check
GET  /api/code/history
```

### 4.7 代码生成流程

1. 用户输入需求。
2. 系统根据语言和场景选择 Prompt 模板。
3. 调用本地 Ollama 或远程模型 API。
4. 对结果做格式化。
5. 对格式化后的代码做语法校验。
6. 若校验失败，触发二次修复 Prompt。
7. 返回最终可复制代码。

### 4.8 代码校验策略

- Java：可调用 `JavaCompiler` 或做 AST 解析。
- Python：调用 `python -m py_compile`。
- SQL：做基础语法和危险关键字校验。
- 前端：可接 ESLint / Prettier。

### 4.9 代码生成服务骨架

```java
@Service
public class CodeGenService {

    public CodeResult generate(CodeGenRequest request) {
        String prompt = buildPrompt(request);
        String rawCode = callModel(prompt);
        String formattedCode = formatCode(request.getLanguage(), rawCode);
        CheckResult checkResult = checkSyntax(request.getLanguage(), formattedCode);
        return new CodeResult(rawCode, formattedCode, checkResult);
    }
}
```

### 4.10 关键优化点

- 限制生成长度，避免输出失控。
- 对代码块做正则提取，防止模型返回多余解释。
- 做危险命令过滤，例如 `rm -rf`、数据库删库语句等。
- 生成失败时自动二次修复，提高可用率。
- 保存用户历史需求，支持复用与二次编辑。

### 4.11 面试亮点

- 从“模型调用”升级到“开发工具产品化”，更有差异化竞争力。
- 同时覆盖 Prompt 工程、结果后处理、语法校验与安全治理。
- 很适合和你的 Java 后端定位结合，包装成“AI 开发辅助平台”。

---

## 5. 统一目录结构建议

```text
java-ai-projects/
├── ollama-chat-api/
│   ├── controller/
│   ├── service/
│   ├── dto/
│   └── resources/
├── rag-kb-system/
│   ├── controller/
│   ├── service/
│   ├── parser/
│   ├── vector/
│   ├── entity/
│   └── resources/
├── ai-customer-service/
│   ├── controller/
│   ├── service/
│   ├── intent/
│   ├── rag/
│   └── resources/
└── ai-code-generator/
    ├── controller/
    ├── service/
    ├── formatter/
    ├── checker/
    └── resources/
```

---

## 6. 技术栈建议

| 层级 | 选型 |
|---|---|
| 后端框架 | Spring Boot 3 |
| AI 模型接入 | Ollama / Spring AI |
| 向量数据库 | Milvus |
| 检索方案 | Milvus 向量检索 / Elasticsearch |
| 缓存 | Redis |
| 数据库 | MySQL 8 |
| 文档解析 | PDFBox / Apache Tika / POI |
| 消息队列 | RabbitMQ |
| 部署 | Docker Compose |
| 前端管理台 | Vue 3 / React |

---

## 7. 简历写法模板

### Java + Ollama 本地大模型问答接口

- 基于 Spring Boot 封装本地 Ollama 大模型问答接口，实现模型参数配置、统一异常处理与服务化调用，支持内网低成本 AI 能力接入 [page:6][page:5]。
- 设计统一 Prompt 请求结构，支持问答、总结、改写等多种能力扩展。
- 为后续 RAG、智能客服与代码生成工具提供统一模型调用底座。

### RAG 知识库问答系统（Java版）

- 基于 Spring Boot + Ollama + Milvus 实现 Java 版 RAG 知识库问答系统，完成文档解析、切片、向量化、向量检索与上下文增强回答 [page:5][page:6][web:38][web:44]。
- 利用 Ollama embedding 接口生成文本向量，并结合向量数据库完成语义召回 [page:5]。
- 通过 Prompt 约束与检索结果重排降低大模型幻觉，提升问答准确性。

### AI 智能客服系统

- 设计面向业务场景的 AI 智能客服系统，支持多轮对话、意图识别、知识库检索与人工兜底。
- 基于 Redis 管理会话上下文与接口限流，提升高并发场景下系统稳定性。
- 将 FAQ、RAG、业务接口查询与大模型对话结合，实现更贴近企业落地的智能客服能力。

### AI 代码生成工具

- 基于大模型 API 构建开发辅助工具，支持自然语言生成 Java/Python/SQL 代码，并完成结果格式化与语法校验。
- 设计 Prompt 模板、二次修复与危险命令过滤机制，提高生成代码可用性与安全性。
- 将 AI 能力落地为开发提效产品，突出 Java 后端与 AI 工具化方向的结合能力。

---

## 8. 推荐完成顺序

建议按以下顺序落地：

1. 先做 Java + Ollama 本地大模型问答接口，打通模型接入底座。
2. 再做 RAG 知识库问答系统，补齐向量检索与知识增强能力。
3. 然后做 AI 智能客服系统，把会话、意图、限流、业务分流串起来。
4. 最后做 AI 代码生成工具，形成你在“AI + 开发提效”方向的差异化标签。

对你来说，这 4 个项目的组合价值非常高：第一个体现模型接入能力，第二个体现 AI 应用核心架构能力，第三个体现业务化落地能力，第四个体现产品化与开发者工具思维，整体非常贴合 Java 后端 + AI 大模型应用开发路线。
