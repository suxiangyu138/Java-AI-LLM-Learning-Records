# Java AI 生态知识体系总览

> Java AI 生态 2026 全景图：从 LangChain4j 到 Spring AI，从 MCP 协议到 Agentic RAG——JVM 上的 AI 应用开发已全面进入生产就绪阶段

---

## 📚 目录

1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [学习路线推荐](#3-学习路线推荐)
4. [核心概念速查](#4-核心概念速查)
5. [2026 生态格局总览](#5-2026-生态格局总览)

---

## 1. 知识体系导图

```text
Java AI 生态（2026）
│
├── 🏗️ 核心框架层
│   ├── LangChain4j 1.12.x —— 框架无关、多模型、A2A 协议、采用率 68%
│   ├── Spring AI 1.1 → 2.0 —— Spring Boot 深度集成、MCP 全支持
│   ├── 直接 API 调用 —— 零依赖，仅限简单场景
│   └── 选型决策矩阵 —— 按团队技术栈/场景/模型需求选择
│
├── 🔌 协议与通信层
│   ├── MCP（Model Context Protocol）—— Java 实现三方案
│   │   ├── Spring AI MCP Server（Spring Boot 原生）
│   │   ├── 官方 Java MCP SDK（框架无关）
│   │   └── Quarkus MCP Server（LangChain4j 推荐）
│   ├── A2A（Agent-to-Agent）—— LangChain4j 原生支持
│   └── Function Calling —— @Tool / @AiFunction 注解驱动
│
├── 🔍 检索增强生成（RAG）
│   ├── 文档摄入 Pipeline —— Tika 解析 → TokenTextSplitter 切片
│   ├── 向量存储 —— PGVector / Milvus / Qdrant / Redis / ES
│   ├── 混合检索 —— 向量相似度 + BM25 关键词 → RRF 融合
│   ├── PGVector sparsevec —— 2026 纯 PG 原生混合检索
│   └── 高级模式 —— Agentic RAG + 查询重写 + 自我修正
│
├── 🤖 Agent 智能体层
│   ├── LangChain4j Agent —— AiServices + @Tool + 多 Agent 编排
│   ├── LangGraph4j —— 有状态图编排、循环图、人机交互
│   ├── Spring AI Agent —— FunctionCallback + Advisor 链
│   ├── AgentScope Java —— 阿里企业级 Agent 框架
│   └── Jakarta Agentic AI —— Jakarta EE 标准 API（新兴）
│
├── 🗄️ 向量数据库生态
│   ├── PGVector —— PostgreSQL 扩展，与 RDS 无缝集成
│   ├── Elasticsearch —— 全文检索 + 向量检索
│   ├── Milvus / Qdrant / Weaviate —— 专业向量数据库
│   ├── Redis Stack —— 内存级向量检索
│   └── Embedding 模型 —— bge-m3 / text-embedding-3 / nomic
│
├── 🌐 多模型集成
│   ├── 国产模型 —— 通义千问 / 百度千帆 / DeepSeek / GLM
│   ├── 国际模型 —— OpenAI / Anthropic / Google Gemini
│   ├── 本地模型 —— Ollama / Llama.cpp / vLLM
│   ├── ModelRouter —— 按复杂度自动路由（LangChain4j）
│   └── 降级策略 —— 主模型 → 备用模型 → 规则引擎
│
├── 🚀 企业级部署
│   ├── GraalVM 原生镜像 —— 启动 <100ms，内存 50-100MB
│   ├── Quarkus + LangChain4j —— 云原生首选
│   ├── Spring Boot + Spring AI —— 企业微服务体系
│   ├── 可观测性 —— Micrometer + OpenTelemetry + Actuator
│   └── 安全 —— OAuth2 / API Key 管理 / Prompt 注入防护
│
└── 🔮 新兴趋势
    ├── Jakarta Agentic AI —— Jakarta EE 标准 AI API
    ├── Embeddable（Rod Johnson）—— JVM 高级 Agent 编排
    ├── Browser Use —— AI 操控浏览器自动化
    ├── Docker 代码执行 —— 沙箱安全隔离
    └── Python 原型 → Java 生产 —— 业界主流路径
```

---

## 2. 模块导航

| 序号 | 模块 | 核心内容 | 适合人群 |
|:---:|------|---------|---------|
| 01 | [核心框架对决](01-核心框架-LangChain4j与SpringAI.md) | 两大框架深度对比、API 风格、性能、选型决策矩阵 | 所有人 |
| 02 | [MCP 协议 Java 实现](02-MCP协议Java实现.md) | MCP Server/Client 三种方案、Spring AI MCP Starter、官方 SDK | 集成开发 |
| 03 | [RAG 检索增强生成](03-RAG检索增强生成.md) | 完整 RAG 管道、PGVector+ES 混合检索、RRF 融合、sparsevec | 全栈开发 |
| 04 | [Agent 智能体开发](04-Agent智能体开发.md) | AiServices、LangGraph4j、多 Agent 编排、A2A 协议 | 高级开发 |
| 05 | [向量数据库与嵌入](05-向量数据库与嵌入.md) | PGVector/Milvus/ES/Qdrant、Embedding 模型选型、HNSW 优化 | 架构师 |
| 06 | [多模型集成与路由](06-多模型集成与路由.md) | 国产模型适配、ModelRouter、降级策略、成本控制 | 全栈开发 |
| 07 | [企业级部署与性能](07-企业级部署与性能优化.md) | GraalVM 原生镜像、Quarkus、可观测性、安全、CI/CD | DevOps/架构师 |
| 08 | [新兴项目与生态趋势](08-新兴项目与生态趋势.md) | Jakarta Agentic AI、LangGraph4j、AgentScope、2026 趋势 | 所有人 |

---

## 3. 学习路线推荐

### 🟢 入门路线（1-2 天）

```
01 核心框架对决 → 03 RAG 检索增强生成
```

先搞懂 LangChain4j 和 Spring AI 的区别，知道什么时候选哪个。再动手跑通一个完整的 RAG 管道（文档摄入 → 向量化 → 混合检索 → LLM 回答）。

### 🟡 进阶路线（3-5 天）

```
01 → 02 MCP 协议 → 04 Agent 开发 → 06 多模型集成
```

深入 MCP 协议在 Java 中的实现、构建自己的 Agent 系统、掌握多模型路由和降级——这三项是生产级 Java AI 应用的核心能力。

### 🔴 深入路线（1-2 周）

```
01 → 02 → 03 → 04 → 05 → 06 → 07 → 08（全模块 + 动手实践）
```

覆盖从框架选型到企业部署的完整链路。适合团队 Tech Lead 或架构师，需要为团队制定 Java AI 技术路线。

---

## 4. 核心概念速查

| 概念 | 一句话定义 | 详细模块 |
|------|-----------|:---:|
| **LangChain4j** | 框架无关的 Java LLM 集成工具箱，采用率 68%，支持 A2A/MCP/多 Agent | [01](01-核心框架-LangChain4j与SpringAI.md) |
| **Spring AI** | Spring 官方 AI 框架，深度 Spring Boot 集成，MCP Server/Client 全支持 | [01](01-核心框架-LangChain4j与SpringAI.md) |
| **MCP（Model Context Protocol）** | Anthropic 发布的标准化 AI-外部工具连接协议，Java 有三种实现方案 | [02](02-MCP协议Java实现.md) |
| **A2A（Agent-to-Agent）** | Google 发布的跨 Agent 通信协议，LangChain4j 原生支持 | [04](04-Agent智能体开发.md) |
| **RAG（检索增强生成）** | 检索 + 生成的组合：先从知识库检索相关内容，再注入 LLM 上下文 | [03](03-RAG检索增强生成.md) |
| **RRF（Reciprocal Rank Fusion）** | 混合检索融合算法，用排名而非分数合并向量和关键词检索结果 | [03](03-RAG检索增强生成.md) |
| **PGVector** | PostgreSQL 向量扩展，2026 年 sparsevec 支持原生混合检索 | [05](05-向量数据库与嵌入.md) |
| **AiServices** | LangChain4j 声明式 AI 服务接口：`@AiService` + `@SystemMessage` | [04](04-Agent智能体开发.md) |
| **ModelRouter** | LangChain4j 智能模型路由：简单问题用小模型，复杂问题用大模型 | [06](06-多模型集成与路由.md) |
| **GraalVM Native Image** | 将 Java 编译为原生可执行文件，启动 <100ms，内存 50MB | [07](07-企业级部署与性能优化.md) |
| **Agentic RAG** | Agent 自主决策检索策略的增强 RAG：查询重写、多步推理、自我修正 | [03](03-RAG检索增强生成.md) |
| **LangGraph4j** | Java 有状态多 Agent 图编排框架，支持循环图和检查点 | [04](04-Agent智能体开发.md) |

---

## 5. 2026 生态格局总览

```text
                    Java AI 生态成熟度曲线（2026）

    成熟度
      ↑
    生产级 ──●──●──●──●──●──●──  LangChain4j, Spring AI, PGVector
      │        ●──●──●──●──────  GraalVM Native, ES Vector, MCP SDK
      │            ●──●──────────  LangGraph4j, A2A Protocol
      │                ●─────────  AgentScope, ModelRouter
      │                    ●─────  Jakarta Agentic AI
      │                        ●─  Browser Use
    实验级 ─────────────────────●── Embeddable
      │
      └──────────────────────────────→ 时间
         2025 H1   2025 H2   2026 H1   2026 H2

    市场份额（JetBrains 2025 Q1 调查）：
    ┌──────────────────┬─────────┐
    │ LangChain4j       │   68%   │ ████████████████████████████████
    │ Spring AI         │   52%   │ ██████████████████████████
    │ 直接 API 调用      │   35%   │ █████████████████
    │ Semantic Kernel   │   12%   │ ██████
    └──────────────────┴─────────┘
```

> 🎯 **核心要点**：2026 年 Java AI 生态已从"追赶 Python"进化为"生产级首选"。LangChain4j 和 Spring AI 两大框架各有优势，无需二选一——核心交易链路用 Spring AI 保稳定，边缘 AI 服务用 LangChain4j 快速迭代，是企业级最佳实践。

---

**入门推荐**：[01-核心框架对决](01-核心框架-LangChain4j与SpringAI.md) ｜ **快速动手**：[03-RAG 检索增强生成](03-RAG检索增强生成.md)
