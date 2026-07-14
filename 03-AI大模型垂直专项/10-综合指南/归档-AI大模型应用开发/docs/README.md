# AI 大模型应用开发 —— 企业级技术知识库

> 🎯 面向 **Java 后端开发工程师**，系统化、企业级视角的 AI 大模型应用开发知识体系。每篇独立成章，可按需阅读，也可按学习路线循序渐进。

---

## 📚 文档索引

### 🔰 基础篇（入门必读）

| 序号 | 文档 | 内容概要 |
|:---:|------|---------|
| 01 | [AI 与大模型基础概念](./01-AI与大模型基础概念.md) | AI/ML/DL/LLM/SLM/VLM 定义、参数量、推理/训练、Token 概念、上下文窗口、涌现能力、幻觉 |
| 02 | [大模型架构与 Transformer 详解](./02-大模型架构与Transformer详解.md) | Transformer 完整架构、Self-Attention 数学推导、QKV、MHA/MQA/GQA、FFN、位置编码 (RoPE/ALiBi)、MoE、Mamba/SSM |
| 03 | [大模型训练与微调](./03-大模型训练与微调.md) | Pre-training → SFT → RLHF → DPO 全流程、LoRA/QLoRA、量化 (GPTQ/AWQ/GGUF)、分布式训练 (DP/DDP/FSDP/TP/PP)、数据工程 |

### 🛠️ 核心技能篇

| 序号 | 文档 | 内容概要 |
|:---:|------|---------|
| 04 | [提示工程 Prompt Engineering](./04-提示工程Prompt-Engineering.md) | Few-Shot/CoT/ToT/ReAct/Self-Consistency、System Prompt 设计模式、Prompt 模板化、Prompt 版本管理、A/B 测试、防注入设计 |
| 05 | [RAG 检索增强生成](./05-RAG检索增强生成.md) | 完整 RAG 流水线、Chunking 策略对比、Embedding 模型选型、向量检索 + BM25 混合检索、Re-ranking、Query Rewriting、Self-RAG/CRAG/Graph RAG、多模态 RAG、评估指标 |
| 06 | [Agent 智能体](./06-Agent智能体.md) | 架构模式、ReAct/Plan-Execute/Reflection、Multi-Agent 协作、记忆系统、LangGraph/CrewAI/AutoGen 框架对比、企业级 Agent 设计 |
| 07 | [Function Calling 与 Tool Use](./07-Function-Calling与Tool-Use.md) | 机制详解、JSON Schema 设计、并行调用、流式 Tool Call、错误处理、企业级 Tool 管理、Spring AI Tool 实战 |
| 08 | [MCP 模型上下文协议](./08-MCP模型上下文协议.md) | 协议架构、Server/Client 开发、Transport (stdio/SSE/Streamable HTTP)、Resource/Tool/Prompt、与 Function Calling 对比、MCP 市场生态 |

### 🚀 工程实战篇

| 序号 | 文档 | 内容概要 |
|:---:|------|---------|
| 09 | [模型部署与服务化](./09-模型部署与服务化.md) | vLLM/Ollama/TGI/llama.cpp/TensorRT-LLM 部署方案、GPU 选型、K8s 部署、自动扩缩容、模型网关、冷启动优化 |
| 10 | [向量数据库深度解析](./10-向量数据库深度解析.md) | 索引算法 (HNSW/IVF/PQ)、Milvus/PGVector/Redis/Elasticsearch/Qdrant/Weaviate/Pinecone 详细对比、性能基准、企业选型决策树、数据迁移策略 |
| 11 | [Java 后端 AI 开发框架](./11-Java后端AI开发框架.md) | Spring AI vs LangChain4j vs Semantic Kernel 对比、架构设计、核心 API、项目搭建、依赖管理、多模型适配 |
| 12 | [Spring AI 实战详解](./12-Spring-AI实战详解.md) | ChatClient/Embedding/VectorStore/Image/Audio 完整示例、RAG 实战、Function Calling、Advisor 链、流式输出、生产配置 |
| 13 | [流式输出与实时通信](./13-流式输出与实时通信.md) | SSE/WebSocket/Chunked Transfer、Spring WebFlux 实现、前端 EventSource/Fetch Stream、网关配置 (Nginx/Kong)、断流重连、背压控制 |
| 14 | [多模态 AI 应用开发](./14-多模态AI应用开发.md) | 视觉理解/文生图/文生视频/TTS/ASR、GPT-4V/Claude Vision/Gemini Vision、Stable Diffusion 集成、音视频处理流水线 |

### 🏗️ 架构与运维篇

| 序号 | 文档 | 内容概要 |
|:---:|------|---------|
| 15 | [Token 与计费模型](./15-Token与计费模型.md) | Token 计算原理、tiktoken/Tokenizer、各厂商计费对比、成本优化策略（缓存/压缩/路由）、预算告警、企业级成本管控 |
| 16 | [AI 应用安全与合规](./16-AI应用安全与合规.md) | Prompt Injection/Jailbreak 防御、内容安全、PII 保护、OWASP LLM Top 10、生成式 AI 法规、企业安全架构 |
| 17 | [AI 应用架构设计](./17-AI应用架构设计.md) | 企业级分层架构、多租户设计、对话管理、缓存策略、异步任务、灰度发布、数据库设计、微服务拆分 |
| 18 | [大模型厂商与平台选型](./18-大模型厂商与平台选型.md) | 30+ 模型全面对比、性能/价格/能力矩阵、多模型路由、Fallback 策略、选型决策框架 |
| 19 | [开发工具链与生态平台](./19-开发工具链与生态平台.md) | Dify/FastGPT/LangFuse/LangSmith/Phoenix/HuggingFace/Ollama/ModelScope 全解析 |
| 20 | [企业级 AI 应用监控与运维](./20-企业级AI应用监控与运维.md) | 可观测性三支柱、LangFuse 追踪、Prometheus+Grafana 监控、告警规则、SLO/SLA、成本分析、A/B 评估 |

### 📖 参考篇

| 序号 | 文档 | 内容概要 |
|:---:|------|---------|
| 21 | [术语速查表](./21-术语速查表.md) | 200+ 核心术语中英文对照 + 一句话解释（A-Z 排序） |

---

## 🎯 学习路线图

```
Phase 1：基础认知 (1-2 周)
  ├── 01-AI与大模型基础概念.md        ← 从这里开始
  ├── 02-大模型架构与Transformer详解.md
  └── 03-大模型训练与微调.md

Phase 2：核心技能 (2-3 周)
  ├── 04-提示工程Prompt-Engineering.md
  ├── 05-RAG检索增强生成.md           ← 最重要！
  ├── 06-Agent智能体.md
  ├── 07-Function-Calling与Tool-Use.md
  └── 08-MCP模型上下文协议.md

Phase 3：Java 工程实战 (3-4 周)
  ├── 11-Java后端AI开发框架.md
  ├── 12-Spring-AI实战详解.md         ← Java 开发者必读
  ├── 13-流式输出与实时通信.md
  ├── 10-向量数据库深度解析.md
  └── 09-模型部署与服务化.md

Phase 4：架构与运维 (2-3 周)
  ├── 17-AI应用架构设计.md
  ├── 15-Token与计费模型.md
  ├── 16-AI应用安全与合规.md
  └── 20-企业级AI应用监控与运维.md

Phase 5：扩展与选型 (1-2 周)
  ├── 14-多模态AI应用开发.md
  ├── 18-大模型厂商与平台选型.md
  └── 19-开发工具链与生态平台.md

随时查阅
  └── 21-术语速查表.md               ← 忘记名词就来这里查
```

---

## 🔧 使用建议

- **快速复习**：直接看每篇文档开头的「核心名词表」和末尾的「总结」
- **面试准备**：重点看 01-07 + 11-13 + 17
- **项目搭建**：直接跳到 12-Spring AI 实战详解 + 17-AI 应用架构设计
- **问题排查**：先查 21-术语速查表 确认概念，再看对应章节

---

> 📝 持续更新中，欢迎补充和纠错。
