# 02-核心架构与 ChatClient
> Spring AI 分层架构、核心接口职责、ChatClient 流式 DSL：从一次 `call()` 出发理解整条调用链

## 📚 目录
1. [分层架构总览](#1-分层架构总览)
2. [核心接口与职责](#2-核心接口与职责)
3. [ChatClient：主 API](#3-chatclient主-api)
4. [一次 call() 的完整旅程](#4-一次-call-的完整旅程)
5. [Advisor 链模型](#5-advisor-链模型)
6. [核心要点](#6-核心要点)
7. [参考来源](#7-参考来源)

## 1. 分层架构总览

```text
┌─────────────────────────────────────────────────────────┐
│ 应用层（你的代码）                                        │
│   Controller / Service → ChatClient / StreamBridge(消息)  │
├─────────────────────────────────────────────────────────┤
│ ChatClient（主 API，流式 DSL）                            │
│   prompt().system().user().tools().advisors().call()     │
├─────────────────────────────────────────────────────────┤
│ Advisor 链（横切能力，可递归）                             │
│   ToolCallingAdvisor → Memory → SafeGuard → Logger → ... │
├─────────────────────────────────────────────────────────┤
│ 模型抽象层                                               │
│   ChatModel（底层） / EmbeddingModel / ImageModel / ...  │
├─────────────────────────────────────────────────────────┤
│ Provider 适配（SDK）                                     │
│   OpenAI SDK / Anthropic SDK / DeepSeek / Ollama / ...   │
└─────────────────────────────────────────────────────────┘
```

设计哲学与 Web 层一一对应：

| Spring AI | Spring Web | 职责 |
|-----------|-----------|------|
| `ChatClient` | `RestClient`/`WebClient` | 面向业务的主 API，流式声明 |
| `ChatModel` | `RestTemplate` 底层实现 | 协议细节，框架开发者使用 |
| Advisor 链 | Filter/Interceptor | 横切能力编排 |
| `ToolCallback` | HandlerMapping 的 Handler | 工具端点注册 |
| Starter 自动配置 | Spring Boot 自动配置 | 依赖即服务 |

> 🎯 **核心要点**：2.0 把 `ChatClient` 定为唯一主 API，`ChatModel` 降为底层构建块——业务代码永远写 `chatClient.prompt()...`，只有要造框架/深度定制模型行为才碰 `ChatModel`。

## 2. 核心接口与职责

| 接口 | 职责 | 典型实现 |
|------|------|---------|
| `ChatModel` | 一次对话请求 → 响应（底层） | `OpenAiChatModel`、`AnthropicChatModel`、`DeepSeekChatModel` |
| `ChatClient` | 主 API：组装 prompt/工具/Advisor | `DefaultChatClient`（自动装配 `ChatClient.Builder`） |
| `EmbeddingModel` | 文本 → 向量 | `OpenAiEmbeddingModel`、`OllamaEmbeddingModel` |
| `VectorStore` | 向量存/取/相似检索 | Milvus/PGVector/Redis/Qdrant 实现 |
| `ToolCallback` | 工具统一接口（schema + 执行） | `@Tool` 方法包装、MCP 工具 |
| `DocumentRetriever` | RAG 检索（返回文档列表） | 向量库检索器、DashScope 知识库检索器 |
| `ChatMemoryRepository` | 会话记忆读写 | `InMemoryChatMemoryRepository`、Redis 实现 |
| `ChatResponse` | 模型响应（含 token 用量、工具调用） | 统一响应模型 |

## 3. ChatClient：主 API

### 3.1 流式 DSL 骨架

```java
@RestController
public class ChatController {

    private final ChatClient chatClient;

    public ChatController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @GetMapping("/chat")
    public String chat(@RequestParam String question) {
        return chatClient.prompt()                       // 1. 开始 prompt
                .system("你是电商客服助手，回答要简洁。")   // 2. 系统提示词
                .user(question)                          // 3. 用户输入
                .call()                                  // 4. 执行（同步）
                .content();                              // 5. 取文本
    }
}
```

### 3.2 DSL 能力全景

| 方法 | 能力 | 说明 |
|------|------|------|
| `prompt()` | 入口 | 返回 PromptSpec |
| `.system(...)` / `.user(...)` | 消息组装 | 支持文本/消息对象/函数式构建 |
| `.options(...)` | 模型参数 | ChatOptions 不可变构建器（temperature 等） |
| `.tools(...)` | 挂载工具 | `ToolCallback`/集合/数组；本地 + MCP 混用 |
| `.advisors(...)` | 挂载横切 | 记忆、安全、校验等 Advisor |
| `.entity(Class)` | 结构化输出 | 直接映射 POJO，支持 `EntityParamSpec` |
| `.call()` | 同步执行 | 返回 `ChatResponse` |
| `.stream()` | 流式执行 | 返回 `Flux<ChatResponse>`，逐 token 输出 |
| `.chatClient()` / `.build()` | 复用/构建 | 链式共享配置 |

### 3.3 流式输出（打字机效果）

```java
@GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<String> stream(@RequestParam String question) {
    return chatClient.prompt()
            .user(question)
            .stream()                       // 流式
            .content();                     // Flux<String>，逐增量
}
```

> ⚠️ 流式 + 工具调用的交互：工具调用需要先收集完整响应再执行，`ToolCallingAdvisor` 内部做流聚合后递归——自定义流式场景时优先用官方 `ToolCallingAdvisor` 的扩展钩子，别手写循环。

## 4. 一次 call() 的完整旅程

```text
chatClient.prompt().user(q).tools(t).call()
  │
  ├─ 1. DefaultChatClient 组装 Prompt（消息 + Options + 工具 schema）
  ├─ 2. 进入 Advisor 链（按 precedence 排序）
  │      ├─ Memory Advisor：注入历史消息
  │      ├─ SafeGuard：敏感词拦截（可选）
  │      └─ ToolCallingAdvisor：执行工具循环（见 04）
  ├─ 3. ChatModel.call(prompt) → provider SDK → LLM
  ├─ 4. 响应回传：Advisor 后处理（记忆更新/日志/校验）
  └─ 5. 返回 ChatResponse（文本 + 元数据 + 累计 token 用量）
```

| 环节 | 关键类 | 观察点 |
|------|--------|--------|
| 组装 | `DefaultChatClient` | 工具 schema 注入时机 |
| 链执行 | `CallAdvisor`/`StreamAdvisor` | `aroundCall`/`aroundStream` 钩子 |
| 模型调用 | `ChatModel` | provider 协议差异被抽象 |
| 递归 | `CallAdvisorChain.copy(...)` | 工具循环/校验重试的引擎 |

## 5. Advisor 链模型

### 5.1 接口与排序

| 接口 | 场景 | 排序机制 |
|------|------|---------|
| `CallAdvisor` | 同步调用 | `getOrder()`（precedence），数值小者在外层 |
| `StreamAdvisor` | 流式调用 | 同上 |

内置 Advisor 及默认顺序（2.0）：

| Advisor | 职责 | 默认排序（越低越外层） |
|---------|------|------------------------|
| `SafeGuardAdvisor` | 敏感词拦截 | 最低（最外层） |
| `MessageChatMemoryAdvisor` | 注入/存储记忆 | 低 |
| `ToolCallingAdvisor` | 工具循环 | 中（自动注册） |
| `StructuredOutputValidationAdvisor` | 输出 schema 校验 + 重试 | 高 |
| `SimpleLoggerAdvisor` | 日志 | 可配 |

### 5.2 递归：2.0 的关键机制

```text
Advisor 链: A → B → C → ChatModel
递归场景：C 内部需要"再跑一遍 B→C→ChatModel"（如工具循环下一轮）
实现：CallAdvisorChain.copy(C) 生成只含 C 之后顾问的子链
```

| 递归用途 | 说明 |
|----------|------|
| 工具循环 | 模型多次请求调用工具，每轮重新入链 |
| 结构化输出重试 | 校验失败后带错误信息重试（默认 3 次） |
| 评估循环 | 评估 Agent 的结果并反馈重试 |

> ⚠️ 单工具 Advisor 不变量：`DefaultChatClient` 强制一条链中**只能有一个**工具 Advisor（`ToolAdvisor` 标记接口），防止工具执行双循环 bug。

## 6. 核心要点

> 🎯 **核心要点**：
> - 2.0 架构 = ChatClient（主 API） + Advisor 链（横切+递归） + ChatModel（底层）；
> - Advisor 链是扩展总纲：记忆/工具/安全/校验全部是 Advisor，自定义能力也写 Advisor；
> - 递归链（`CallAdvisorChain.copy`）是工具循环与校验重试的引擎，理解它才算懂 2.0；
> - 流式与工具调用：用官方 ToolCallingAdvisor，别手写循环。

## 7. 参考来源

- [Spring AI Reference：ChatClient](https://docs.spring.io/spring-ai/reference/api/chatclient.html)
- [Spring AI 2.0 API（advisor 包）](https://docs.spring.io/spring-ai/docs/2.0.0/api/org/springframework/ai/chat/client/advisor/package-summary.html)
- [Recursive Advisors（2.0 文档）](https://docs.spring.io/spring-ai/reference/2.0-SNAPSHOT/api/advisors-recursive.html)
- [Tool Calling in Spring AI 2.0（官方博客）](https://spring.io/blog/2026/06/15/spring-ai-composable-tool-calling)

---

**下一模块**：[03-模型接入与结构化输出](03-模型接入与结构化输出.md)　/　**返回总览**：[00-总览](00-Spring%20AI总览.md)
