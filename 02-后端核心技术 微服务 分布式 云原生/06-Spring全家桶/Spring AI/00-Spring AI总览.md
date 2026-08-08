# Spring AI 总览
> Spring 官方 AI 工程框架（2.0.0 GA）：ChatClient 流式 API + Advisor 链 + 统一工具调用 + MCP 原生集成 + RAG/ETL，把 AI 能力变成 Spring 生态的普通 Bean

## 📚 目录
1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [学习路线推荐](#3-学习路线推荐)
4. [核心概念速查](#4-核心概念速查)
5. [版本窗口说明](#5-版本窗口说明)
6. [参考来源](#6-参考来源)

## 1. 知识体系导图

```text
Spring AI（2.0.0 GA / Spring Boot 4.0-4.1 / Framework 7 / Jackson 3）
│
├─ 应用层 ─────────────────────────────
│   ├─ ChatClient（主 API，流式 DSL，类 WebClient）
│   ├─ 结构化输出（entity() / EntityParamSpec / 校验重试）
│   ├─ 工具调用（@Tool / ToolCallback / ToolCallingAdvisor）
│   └─ MCP（@McpTool / Client+Server 双角色 / Streamable HTTP）
│
├─ Advisor 链（横切能力层）────────────
│   ├─ 记忆（MessageChatMemoryAdvisor / PromptChatMemoryAdvisor）
│   ├─ 工具循环（ToolCallingAdvisor / ToolSearchToolCallingAdvisor）
│   ├─ 安全（SafeGuardAdvisor / 输出校验）
│   └─ 可观测（SimpleLoggerAdvisor / Tracing）
│
├─ 模型层 ─────────────────────────────
│   ├─ ChatModel（底层构建块）/ EmbeddingModel
│   ├─ Providers：OpenAI / Anthropic / DeepSeek / Ollama / Bedrock / Google / Mistral
│   └─ ChatOptions（不可变构建器，默认值下沉 provider）
│
├─ RAG 与知识 ─────────────────────────
│   ├─ ETL 管道（DocumentReader → Transformer → DocumentWriter）
│   ├─ 向量库抽象（20+：Milvus / PGVector / Redis / Qdrant）
│   └─ 检索（QuestionAnswerAdvisor / RetrievalAugmentationAdvisor / 重排）
│
└─ 工程化 ─────────────────────────────
    ├─ 可观测性（Micrometer Tracing / 指标）
    ├─ 评估（模型评估 / 结构化校验）
    └─ 生产（超时重试 / 成本控制 / 提示注入防护）
```

## 2. 模块导航

| 序号 | 模块 | 核心内容 | 适合人群 |
|:---:|------|---------|---------|
| 00 | [总览（本文）](00-Spring%20AI总览.md) | 导图、路线、核心概念速查 | 所有人 |
| 01 | [模块清单与版本矩阵](01-模块清单与版本矩阵.md) | starters、版本对应、2.0 重大变更、迁移 | 选型/升级 |
| 02 | [核心架构与 ChatClient](02-核心架构与ChatClient.md) | 分层架构、核心接口、流式 DSL | 入门必读 |
| 03 | [模型接入与结构化输出](03-模型接入与结构化输出.md) | 7 大 provider、ChatOptions、entity() 校验重试 | 入门必读 |
| 04 | [工具调用与 Agent](04-工具调用与Agent开发.md) | @Tool、ToolCallingAdvisor、递归链、按需发现 | 进阶重点 |
| 05 | [快速开始实战](05-快速开始实战.md) | Boot 4 + Spring AI 2.0 三连 Demo | 上手实操 |
| 06 | [MCP 协议集成](06-MCP协议集成.md) | @McpTool、Client/Server、传输层、安全 | 进阶重点 |
| 07 | [RAG 与向量数据库](07-RAG与向量数据库.md) | RAG 四步、双 Advisor、20+ 向量库、重排 | 重点 |
| 08 | [文档 ETL 管道](08-文档ETL管道.md) | Reader/Transformer/Writer、分块策略 | 进阶 |
| 09 | [记忆与 Advisor 链](09-记忆与Advisor链.md) | ChatMemory、窗口裁剪、链序、自定义 | 进阶 |
| 10 | [可观测性评估与生产避坑](10-可观测性评估与生产避坑.md) | Tracing、评估、安全、对比选型、面试 | 收尾 |

## 3. 学习路线推荐

| 路线 | 人群 | 路径 |
|------|------|------|
| 入门路线（1~2 天） | 有 Boot 4 + LLM API 基础的开发 | 00 → 01 → 02 → 03 → 05 → 07 |
| 进阶路线（3~5 天） | 要落地 RAG/Agent 的工程师 | 入门 + 04 → 06 → 08 → 09 |
| 生产路线（1 周） | 负责 AI 服务上线 | 全部 + 10（评估/安全/成本） |

## 4. 核心概念速查

| 概念 | 一句话解释 | 2.0 定位 |
|------|-----------|---------|
| `ChatClient` | 与模型对话的流式 DSL（类比 WebClient），工具/记忆/校验都在它的 Advisor 链里 | **主 API** |
| `ChatModel` | 底层模型抽象，框架开发者用的构建块 | 降为底层 |
| Advisor | 横切 AI 模式（记忆/工具/安全/校验），可递归重入 | 核心扩展点 |
| `ToolCallback` | 工具的统一接口，本地 `@Tool` 与远程 MCP 工具混用 | 统一抽象 |
| MCP | 模型上下文协议：让 LLM 安全调用外部工具/资源 | 原生集成 |
| `EmbeddingModel` | 文本 → 向量 | 基础组件 |
| `VectorStore` | 向量数据库抽象（20+ 实现） | RAG 存储层 |
| Document ETL | Reader → Transformer → Writer 的文档处理管道 | RAG 数据准备 |
| ChatMemory | 多轮会话记忆存储抽象 | 对话连续性 |

## 5. 版本窗口说明

| 项 | 本文档基准 | 说明 |
|----|-----------|------|
| Spring AI | **2.0.0 GA（2026-06-12 发布）** | 自 1.0.0 以来最大升级；历经 8 个里程碑 + 2 个 RC |
| Spring Boot | 4.0.x / 4.1.x | **只能在 Boot 4 上运行**，Boot 3.x 无法直接升级 |
| Spring Framework | 7.0 | Jakarta EE 11 基线 |
| Java | 17 最低，**推荐 21** | 全库 JSpecify 空安全注解 |
| JSON | Jackson 3 | 可用 `JsonHelper` 定制序列化 |
| 模型 providers | OpenAI / Anthropic / DeepSeek / Ollama / Bedrock / Google / Mistral | 收敛为"官方维护核心集" |
| 旧线 | 1.1.x ↔ Boot 3.5.x | Boot 3 项目只能停在该线 |
| MCP 传输 | Streamable HTTP 默认（SSE 弃用）+ STDIO | 传输层移入 Spring AI 内部实现 |

> ⚠️ **时效性**：本文档按 2026-08 官方发布博客、Reference 与 Javadoc 编写。2.0 是破坏性大版本，1.x 用户迁移前务必读官方 [Upgrade Notes](https://docs.spring.io/spring-ai/reference/upgrade-notes.html)。

## 6. 参考来源

- [Spring AI 2.0.0 GA Available Now（官方发布博客）](https://spring.io/blog/2026/06/12/spring-ai-2-0-0-GA-available-now)
- [Spring AI Reference](https://docs.spring.io/spring-ai/reference/)
- [Spring AI Getting Started](https://docs.spring.io/spring-ai/reference/getting-started.html)
- [Spring AI 2.0 API（Javadoc）](https://docs.spring.io/spring-ai/docs/current/api/)
- [Tool Calling in Spring AI 2.0（官方博客）](https://spring.io/blog/2026/06/15/spring-ai-composable-tool-calling)

---

**下一模块**：[01-模块清单与版本矩阵](01-模块清单与版本矩阵.md)
