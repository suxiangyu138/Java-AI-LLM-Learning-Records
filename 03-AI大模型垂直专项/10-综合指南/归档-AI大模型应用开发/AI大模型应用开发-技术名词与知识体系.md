# AI 大模型应用开发 —— 技术名词与知识体系（Java 后端向）

> 本文档面向 Java 后端开发工程师，系统梳理 AI 大模型应用开发领域的所有核心名词、技术栈和知识点，帮助快速建立完整的认知体系。

---

## 目录

1. [一、AI / LLM 基础概念](#一ai--llm-基础概念)
2. [二、大模型架构与训练范式](#二大模型架构与训练范式)
3. [三、提示工程 Prompt Engineering](#三提示工程-prompt-engineering)
4. [四、RAG —— 检索增强生成](#四rag--检索增强生成)
5. [五、Agent 智能体](#五agent-智能体)
6. [六、MCP —— 模型上下文协议](#六mcp--模型上下文协议)
7. [七、模型服务化与部署](#七模型服务化与部署)
8. [八、Java 后端 AI 开发技术栈](#八java-后端-ai-开发技术栈)
9. [九、Spring AI 框架详解](#九spring-ai-框架详解)
10. [十、向量数据库](#十向量数据库)
11. [十一、多模态](#十一多模态)
12. [十二、流式输出与 SSE](#十二流式输出与-sse)
13. [十三、Token 与计费模型](#十三token-与计费模型)
14. [十四、安全与合规](#十四安全与合规)
15. [十五、Function Calling / Tool Use](#十五function-calling--tool-use)
16. [十六、常见大模型厂商与 API](#十六常见大模型厂商与-api)
17. [十七、AI 应用架构模式](#十七ai-应用架构模式)
18. [十八、开发工具链与平台](#十八开发工具链与平台)
19. [十九、术语速查表](#十九术语速查表)

---

## 一、AI / LLM 基础概念

### 1.1 核心名词

| 名词 | 全称 | 解释 |
|------|------|------|
| **AI** | Artificial Intelligence | 人工智能，模拟人类智能的系统 |
| **ML** | Machine Learning | 机器学习，从数据中学习的算法 |
| **DL** | Deep Learning | 深度学习，使用多层神经网络的 ML |
| **NLP** | Natural Language Processing | 自然语言处理 |
| **CV** | Computer Vision | 计算机视觉 |
| **LLM** | Large Language Model | 大语言模型，如 GPT-4、Claude、Gemini |
| **SLM** | Small Language Model | 小语言模型，参数量较小的模型（1B~14B） |
| **VLM** | Vision-Language Model | 视觉语言模型，可理解图片+文本 |
| **LMM** | Large Multimodal Model | 大型多模态模型，支持文本+图片+音频+视频 |
| **FM** | Foundation Model | 基础模型，预训练好的通用模型 |
| **GenAI** | Generative AI | 生成式 AI，生成文本/图片/视频/代码等 |

### 1.2 模型参数相关

| 名词 | 解释 |
|------|------|
| **Parameters（参数量）** | 模型中的可训练权重数量，如 7B = 70 亿参数 |
| **Weights（权重）** | 神经网络中的连接强度值 |
| **Checkpoint（检查点）** | 训练过程中保存的模型快照 |
| **Quantization（量化）** | 将模型参数从高精度（FP32/FP16）压缩到低精度（INT8/INT4），减少显存 |
| **GGUF** | GPT-Generated Unified Format，llama.cpp 使用的量化模型格式 |
| **GPTQ / AWQ** | 两种主流权重量化方法 |
| **DType** | 数据类型，如 float32、bfloat16、int8 |

### 1.3 推理相关

| 名词 | 解释 |
|------|------|
| **Inference（推理）** | 模型根据输入生成输出的过程 |
| **Training（训练）** | 在大量数据上调整模型参数的过程 |
| **Pre-training（预训练）** | 在海量通用语料上的初始训练 |
| **Fine-tuning（微调）** | 在特定任务数据上进一步训练预训练模型 |
| **SFT** | Supervised Fine-Tuning，有监督微调 |
| **RLHF** | Reinforcement Learning from Human Feedback，基于人类反馈的强化学习 |
| **DPO** | Direct Preference Optimization，直接偏好优化（RLHF 的简化替代） |
| **LoRA** | Low-Rank Adaptation，低秩适配，一种参数高效的微调方法 |
| **QLoRA** | Quantized LoRA，量化版 LoRA，可在消费级 GPU 上微调 |

### 1.4 模型架构类型

| 架构 | 代表模型 | 特点 |
|------|---------|------|
| **Decoder-Only（仅解码器）** | GPT 系列、LLaMA、Claude | 自回归生成，当前主流 |
| **Encoder-Only（仅编码器）** | BERT | 适合理解任务（分类、NER） |
| **Encoder-Decoder** | T5、BART | 适合翻译、摘要等 seq2seq 任务 |
| **MoE** | Mixture of Experts | 每次推理只激活部分参数（如 Mixtral、DeepSeek-V3） |
| **Mamba / SSM** | State Space Model | 线性复杂度替代 Transformer 注意力 |
| **Diffusion（扩散模型）** | Stable Diffusion | 图像/视频生成 |

---

## 二、大模型架构与训练范式

### 2.1 Transformer 核心组件

```
Transformer 架构核心：
┌─────────────────────────────────┐
│         Output Probabilities     │
│              ▲                   │
│         Softmax + Linear         │
│              ▲                   │
│   ┌── Add & Norm ────────────┐   │
│   │       ▲                  │   │
│   │  Feed Forward Network    │   │
│   │       ▲                  │   │
│   └── Add & Norm ────────────┘   │
│   │       ▲                  │   │
│   │  Multi-Head Attention    │   │ ← N× 重复
│   │       ▲                  │   │
│   └── Add & Norm ────────────┘   │
│          ▲                       │
│     Input Embedding              │
└─────────────────────────────────┘
```

| 组件 | 解释 |
|------|------|
| **Self-Attention（自注意力）** | 让每个 token 关注序列中所有其他 token |
| **Multi-Head Attention（多头注意力）** | 多个注意力头并行，捕获不同子空间的信息 |
| **Q / K / V** | Query（查询）、Key（键）、Value（值），注意力的三个投影矩阵 |
| **Feed-Forward Network（FFN）** | 前馈全连接层，每个 token 位置独立处理 |
| **Layer Normalization** | 层归一化，稳定训练 |
| **Residual Connection（残差连接）** | 缓解深层网络的梯度消失 |
| **Positional Encoding（位置编码）** | 让模型感知 token 的顺序位置 |
| **RoPE** | Rotary Position Embedding，旋转位置编码（LLaMA 系列采用） |
| **ALiBi** | Attention with Linear Biases，线性偏置注意力位置编码 |

### 2.2 训练流程

```
大规模语料 ──→ [Pre-training 预训练] ──→ Base Model（基座模型）
                                              │
                              ┌───────────────┼───────────────┐
                              ▼               ▼               ▼
                         [SFT 监督微调]   [RLHF 强化学习]   [DPO 偏好对齐]
                              │               │               │
                              ▼               ▼               ▼
                         Chat / Instruct Model（对话模型 / 指令模型）
```

### 2.3 训练相关名词

| 名词 | 解释 |
|------|------|
| **Epoch** | 完整过一遍训练数据的轮次 |
| **Batch Size** | 每次训练的样本数量 |
| **Learning Rate** | 学习率，控制参数更新步长 |
| **Gradient（梯度）** | 损失函数对参数的偏导，指导参数更新方向 |
| **Loss（损失）** | 预测值与真实值的差距 |
| **Perplexity（困惑度）** | 衡量语言模型质量的指标，越低越好 |
| **Scaling Law（扩展定律）** | 模型性能随参数量/数据量/计算量增长的规律 |
| **Emergent Ability（涌现能力）** | 模型大到一定程度后突然出现的新能力 |
| **Hallucination（幻觉）** | 模型生成看似合理但事实错误的内容 |
| **Overfitting（过拟合）** | 模型在训练集上表现好但在新数据上差 |
| **Alignment（对齐）** | 让模型行为与人类价值观和意图一致 |

---

## 三、提示工程 Prompt Engineering

### 3.1 基础概念

| 名词 | 解释 |
|------|------|
| **Prompt** | 发送给模型的输入文本/指令 |
| **Completion** | 模型对 prompt 的回复/生成内容 |
| **System Prompt** | 系统级提示词，设定模型角色和行为边界（最高优先级） |
| **User Prompt** | 用户输入的消息 |
| **Assistant Message** | 模型的历史回复消息 |
| **Context Window** | 上下文窗口，模型一次能处理的最大 token 数 |
| **Prompt Template** | 提示词模板，可复用的结构化 prompt |

### 3.2 提示策略

| 策略 | 解释 | 适用场景 |
|------|------|---------|
| **Zero-Shot** | 不给示例，直接提问 | 简单任务 |
| **Few-Shot** | 给 N 个示例再提问 | 格式要求严格的任务 |
| **Chain-of-Thought (CoT)** | 让模型一步步推理 | 数学、逻辑推理 |
| **Zero-Shot CoT** | 加一句 "Let's think step by step" | 触发推理链 |
| **Tree-of-Thought (ToT)** | 多路径探索+回溯 | 复杂规划问题 |
| **ReAct** | Reasoning + Acting，推理与行动交替 | Agent 场景 |
| **Self-Consistency** | 多次采样取多数结果 | 提高推理准确率 |
| **Structured Output** | 要求模型按 JSON/XML 等格式输出 | 程序化解析 |
| **Role Prompting** | 给模型分配角色（"你是一个资深 Java 架构师..."）| 专业领域问答 |
| **Negative Prompting** | 告诉模型不要做什么 | 安全约束 |

### 3.3 Prompt 工程最佳实践

```
好的 Prompt 要素：
1. 角色设定 —— "你是一位资深的 Java 后端开发专家"
2. 任务描述 —— "请帮我分析以下代码的性能瓶颈"
3. 上下文信息 —— 提供相关代码片段、业务背景
4. 输出格式 —— "请以 JSON 格式返回，包含以下字段..."
5. 约束条件 —— "请用中文回答，控制在 200 字以内"
6. 示例（Few-Shot）—— 提供 2-3 组输入→输出示例
```

---

## 四、RAG —— 检索增强生成

### 4.1 RAG 概念

> **RAG（Retrieval-Augmented Generation）** = 检索 + 生成，给 LLM 外挂"知识库"，解决幻觉问题和知识时效性问题。

```
传统 LLM：  User Query ──→ LLM ──→ Response
                  (仅依赖训练时学到的知识，可能过时或幻觉)

RAG 流程：  User Query ──→ [检索] ──→ 相关文档 ──→ [拼接到 Prompt] ──→ LLM ──→ Response
                  ▲                              │
                  └── 向量数据库 / 知识库 ────────┘
```

### 4.2 RAG 核心流程

```
步骤 1：文档处理（Ingestion / Indexing）
  PDF/Word/网页/数据库
    → 文档解析（Document Loader）
    → 文本分割（Text Splitter / Chunking）
    → 向量化（Embedding）
    → 存入向量数据库（Vector Store）

步骤 2：检索（Retrieval）
  用户问题
    → 向量化（Embedding）
    → 相似度检索（Similarity Search）
    → 返回 Top-K 相关文档块

步骤 3：增强生成（Augmented Generation）
  System: "根据以下资料回答问题：{检索到的文档块}"
  User: "{用户问题}"
    → LLM 生成带来源引用的回答
```

### 4.3 RAG 技术名词

| 名词 | 解释 |
|------|------|
| **Chunk / Chunking** | 文本分块，将长文档切分为合适大小的片段 |
| **Chunk Size** | 每块的大小（通常 256-1024 tokens） |
| **Chunk Overlap** | 块与块之间的重叠大小（防止信息断裂） |
| **Embedding** | 将文本转换为高维向量（语义向量） |
| **Embedding Model** | 嵌入模型，如 text-embedding-3-small、bge-large-zh |
| **Vector Dimension** | 向量维度，如 1024、1536、4096 |
| **Vector Store** | 向量数据库（见第十章） |
| **Similarity Search** | 相似度搜索，找最相关的文档 |
| **Cosine Similarity** | 余弦相似度，最常用的向量相似度度量 |
| **Euclidean Distance** | 欧氏距离，另一种距离度量 |
| **Dot Product** | 内积相似度 |
| **MMR** | Maximal Marginal Relevance，最大边际相关性，兼顾相关性和多样性 |
| **Hybrid Search** | 混合搜索，结合向量检索 + 关键词检索（BM25） |
| **Re-ranking** | 重排序，对初检结果用更精准的模型二次排序 |
| **Metadata Filtering** | 按元数据过滤（如按时间、分类、标签过滤） |
| **Parent Document Retriever** | 检索小块，返回大块（检索用小粒度，返回用大粒度） |

### 4.4 高级 RAG 策略

| 策略 | 解释 |
|------|------|
| **Naive RAG** | 最基本的 RAG：检索→拼接→生成 |
| **Advanced RAG** | 加入 Query Rewriting、Re-ranking、Hybrid Search |
| **Self-RAG** | 模型自我判断是否需要检索、检索结果是否相关 |
| **Corrective RAG (CRAG)** | 评估检索质量，质量差时自动 Web 搜索补充 |
| **Graph RAG** | 基于知识图谱的 RAG，用实体关系增强检索 |
| **Agentic RAG** | Agent 自主决策检索策略、多轮检索的 RAG |
| **Multi-Modal RAG** | 支持图片、表格等多模态内容的 RAG |

---

## 五、Agent 智能体

### 5.1 Agent 概念

> **Agent（智能体）** = LLM + 规划 + 工具使用 + 记忆。Agent 能自主决策、调用工具、多步执行来完成复杂任务。

```
Agent 架构模式：
┌──────────────────────────────────────────┐
│                   Agent                    │
│  ┌─────────┐  ┌──────────┐  ┌─────────┐  │
│  │  LLM    │  │ Planning │  │ Memory  │  │
│  │ (大脑)   │  │ (规划)   │  │ (记忆)   │  │
│  └────┬────┘  └────┬─────┘  └────┬────┘  │
│       │            │              │        │
│       └────────────┼──────────────┘        │
│                    │                       │
│            ┌───────▼───────┐               │
│            │   Tool Use    │               │
│            │   (工具调用)   │               │
│            └───────┬───────┘               │
└────────────────────┼──────────────────────┘
        ┌────────────┼────────────┐
        ▼            ▼            ▼
   ┌─────────┐ ┌─────────┐ ┌─────────┐
   │ 搜索API  │ │ 数据库   │ │ 代码执行 │
   └─────────┘ └─────────┘ └─────────┘
```

### 5.2 Agent 相关名词

| 名词 | 解释 |
|------|------|
| **Single Agent** | 单个 Agent 独立完成任务 |
| **Multi-Agent** | 多个 Agent 协作，各司其职 |
| **Planning（规划）** | Agent 分解任务、制定执行步骤 |
| **Task Decomposition** | 任务分解，将复杂任务拆成子任务 |
| **Tool Use / Function Calling** | Agent 调用外部工具/API（见第十五章） |
| **Memory（记忆）** | Agent 的短期/长期记忆机制 |
| **Reflection（反思）** | Agent 审视自己的输出并自我纠错 |
| **ReAct** | Reasoning + Acting 循环，推理与行动交替 |
| **Plan-and-Execute** | 先制定完整计划，再逐步执行 |
| **AutoGPT** | 最早的自主 Agent 开源项目 |
| **LangGraph** | LangChain 的状态图 Agent 编排框架 |
| **CrewAI** | 多 Agent 协作框架 |
| **AutoGen** | 微软的多 Agent 对话框架 |

### 5.3 Agent 记忆类型

| 类型 | 解释 | 实现方式 |
|------|------|---------|
| **Sensory Memory** | 当前对话的短期感知 | 上下文窗口内的消息 |
| **Short-Term Memory** | 对话内短期记忆 | Conversation Buffer / Summary |
| **Long-Term Memory** | 跨对话的长期记忆 | 向量数据库 / 知识图谱 |
| **Working Memory** | 当前任务的中间状态 | 上下文中的临时数据结构 |

---

## 六、MCP —— 模型上下文协议

### 6.1 MCP 概念

> **MCP（Model Context Protocol）** 是由 Anthropic 提出的一种开放协议，标准化了 AI 模型与外部工具/数据源之间的交互方式。类似于"AI 的 USB-C 接口"。

```
传统方式：每个 LLM → 各自集成工具（N×M 集成）
    LLM-A ──→ 工具1, 工具2, 工具3
    LLM-B ──→ 工具1, 工具2, 工具3
    LLM-C ──→ 工具1, 工具2, 工具3

MCP 方式：LLM ←→ MCP Server ←→ 工具（统一协议）
    任意 LLM ──→ MCP Client ──→ MCP Server ──→ 工具/数据
```

### 6.2 MCP 核心概念

| 名词 | 解释 |
|------|------|
| **MCP Server** | MCP 服务端，封装工具和数据源 |
| **MCP Client** | MCP 客户端（如 Claude Desktop、VS Code 插件） |
| **Tool** | MCP 暴露的工具（可被 LLM 调用的函数） |
| **Resource** | MCP 暴露的资源（文件内容、数据库记录等） |
| **Prompt Template** | MCP 预定义的提示词模板 |
| **Transport** | 传输协议：stdio / HTTP+SSE |
| **mcp.json / .mcp.json** | MCP 配置文件 |

### 6.3 MCP 与 Function Calling 的区别

| 维度 | MCP | Function Calling |
|------|-----|------------------|
| 标准化 | ✅ 开放协议 | ❌ 各厂商自定义 |
| 工具发现 | 自动发现 Server 的所有工具 | 需要手动注册 |
| 解耦 | 工具与 LLM 完全解耦 | 工具耦合在调用代码中 |
| 复用 | 一套工具多个 LLM 共用 | 每个 LLM 需单独集成 |

---

## 七、模型服务化与部署

### 7.1 部署方式

| 方式 | 解释 | 适用场景 |
|------|------|---------|
| **API 调用** | 调用云端大模型 API | 快速 MVP、成本敏感 |
| **私有化部署** | 在自己服务器部署开源模型 | 数据安全、定制需求 |
| **边缘部署** | 在终端设备运行小模型 | 离线场景、IoT |
| **混合部署** | 敏感数据本地 + 通用能力云端 | 企业级应用 |

### 7.2 模型服务化框架

| 框架/平台 | 解释 |
|------|------|
| **vLLM** | 高吞吐量 LLM 推理引擎（PagedAttention） |
| **Ollama** | 本地一键运行开源模型（Llama、Mistral 等） |
| **llama.cpp** | C++ 实现的 LLM 推理（支持 CPU + GPU 混合） |
| **Text Generation Inference (TGI)** | HuggingFace 的推理服务 |
| **TensorRT-LLM** | NVIDIA 的 LLM 推理加速 |
| **LM Studio** | 桌面端本地模型运行工具 |
| **LocalAI** | OpenAI API 兼容的本地推理服务 |
| **Xinference** | 分布式模型推理平台 |
| **FastChat** | LMSYS 的开放模型服务平台 |
| **Ray Serve** | 分布式推理部署 |

### 7.3 部署名词

| 名词 | 解释 |
|------|------|
| **GPU** | 图形处理器（NVIDIA A100/H100/RTX 4090） |
| **VRAM / 显存** | GPU 内存，决定能跑多大的模型 |
| **CUDA** | NVIDIA 的并行计算平台 |
| **TPU** | Google 的张量处理器 |
| **Inference Engine** | 推理引擎，优化模型推理性能 |
| **KV Cache** | Key-Value 缓存，加速自回归生成 |
| **PagedAttention** | vLLM 的内存管理技术，类似操作系统分页 |
| **Continuous Batching** | 持续批处理，提高吞吐量 |
| **Speculative Decoding** | 推测解码，小模型快速起草，大模型验证 |
| **Latency（延迟）** | 从请求到首 token 的时间（TTFT） |
| **Throughput（吞吐量）** | 每秒生成的 token 数（TPS） |

---

## 八、Java 后端 AI 开发技术栈

### 8.1 核心框架

| 框架 | 说明 |
|------|------|
| **Spring AI** | Spring 官方的 AI 集成框架（对标 LangChain） |
| **LangChain4j** | Java 版 LangChain，社区活跃 |
| **Spring Boot** | 基础 Web 框架 |
| **Spring WebFlux** | 响应式 Web 框架（SSE 流式输出必备） |

### 8.2 Spring AI 核心组件

```
Spring AI 架构概览：
┌──────────────────────────────────────────────────────┐
│                    Spring AI                          │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌─────────┐ │
│  │  Chat    │ │ Embedding│ │  Vector  │ │  Image  │ │
│  │  Client  │ │  Client  │ │  Store   │ │  Client │ │
│  └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬────┘ │
│       │            │            │            │       │
│  ┌────┴────────────┴────────────┴────────────┴────┐  │
│  │               Adapter Layer（适配器层）          │  │
│  │  OpenAI | Azure | Ollama | Gemini | QianFan    │  │
│  │  Anthropic | Vertex AI | ZhiPu | DeepSeek     │  │
│  └────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────┐    │
│  │        Vector Store Adapter                   │    │
│  │  Milvus | Redis | PGVector | Elasticsearch   │    │
│  │  Chroma | Pinecone | Weaviate | Qdrant       │    │
│  └──────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────┘
```

### 8.3 主流 LLM 服务提供商 Java SDK

| 提供商 | Java SDK / 调用方式 |
|------|------|
| **OpenAI** | `openai-java`、Spring AI OpenAI starter、HTTP + OkHttp |
| **Azure OpenAI** | Spring AI Azure OpenAI starter |
| **阿里云百炼（DashScope）** | `dashscope-sdk-java`、HTTP API |
| **百度千帆** | `qianfan-java-sdk` |
| **讯飞星火** | HTTP WebSocket API |
| **智谱 GLM** | `zhipuai-sdk-java` |
| **DeepSeek** | OpenAI 兼容接口（直接用 OpenAI SDK 访问） |
| **Ollama（本地）** | Spring AI Ollama starter、HTTP API |
| **Anthropic Claude** | Spring AI Anthropic starter、HTTP API |
| **Google Gemini** | Spring AI Vertex AI / Gemini starter |

### 8.4 Java 项目常用依赖

```xml
<!-- Spring AI BOM -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-bom</artifactId>
    <version>1.0.0-M6</version>
</dependency>

<!-- OpenAI -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
</dependency>

<!-- Ollama (本地) -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-ollama-spring-boot-starter</artifactId>
</dependency>

<!-- PGVector -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-pgvector-store-spring-boot-starter</artifactId>
</dependency>

<!-- LangChain4j -->
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j</artifactId>
    <version>0.36.2</version>
</dependency>
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-open-ai</artifactId>
    <version>0.36.2</version>
</dependency>
```

### 8.5 Java AI 开发常用库

| 库 | 用途 |
|------|------|
| **OkHttp / WebClient** | HTTP 调用 LLM API |
| **Jackson / Gson** | JSON 解析（Prompt/Response） |
| **Reactor (WebFlux)** | 响应式流式处理 |
| **Apache PDFBox** | PDF 文档解析（RAG 文档处理） |
| **Apache POI** | Word/Excel 文档解析 |
| **Apache Tika** | 通用文档内容提取 |
| **Jsoup** | HTML 解析 |
| **HikariCP** | 数据库连接池 |
| **Redis** | 缓存 Session / Token / 限流 |
| **RabbitMQ / Kafka** | 异步任务队列 |
| **Resilience4j** | 熔断/重试/限流（调用 LLM API 必备） |

---

## 九、Spring AI 框架详解

### 9.1 核心 API

```java
// ===== 1. ChatClient —— 对话 =====
@Autowired
private ChatClient chatClient;

// 简单对话
String response = chatClient.prompt()
    .user("什么是 Spring AI？")
    .call()
    .content();

// 带 System Prompt + 结构化输出
record Answer(String summary, List<String> points) {}

Answer answer = chatClient.prompt()
    .system("你是一个技术专家，请用中文回答")
    .user("介绍 Spring AI 的核心功能")
    .call()
    .entity(Answer.class);  // 自动解析为结构化对象

// 流式输出
Flux<String> stream = chatClient.prompt()
    .user("用 Java 写一个冒泡排序")
    .stream()
    .content();

// ===== 2. EmbeddingClient —— 向量化 =====
@Autowired
private EmbeddingClient embeddingClient;

List<Double> vector = embeddingClient.embed("你好世界");

// 批量向量化
List<List<Double>> vectors = embeddingClient.embed(
    List.of("文本1", "文本2", "文本3"));

// ===== 3. VectorStore —— 向量存储 =====
@Autowired
private VectorStore vectorStore;

// 存储文档
List<Document> documents = List.of(
    new Document("内容1", Map.of("source", "doc1.pdf")),
    new Document("内容2", Map.of("source", "doc2.pdf"))
);
vectorStore.add(documents);

// 相似度搜索
SearchRequest request = SearchRequest.query("用户问题")
    .withTopK(5)                          // 返回 Top-5
    .withSimilarityThreshold(0.7)         // 相似度阈值
    .withFilterExpression("source == 'doc1.pdf'");  // 元数据过滤

List<Document> results = vectorStore.similaritySearch(request);

// ===== 4. ImageClient —— 图片生成 =====
@Autowired
private ImageClient imageClient;

ImageResponse response = imageClient.call(
    new ImagePrompt("一只在月光下弹吉他的猫",
        OpenAiImageOptions.builder()
            .withQuality("hd")
            .withN(1)
            .withHeight(1024)
            .withWidth(1024)
            .build()
    ));
String imageUrl = response.getResult().getOutput().getUrl();

// ===== 5. AudioClient —— 语音 =====
@Autowired
private AudioTranscriptionClient transcriptionClient;

// 语音转文字
String text = transcriptionClient.call(
    new AudioTranscriptionPrompt(audioResource));
```

### 9.2 RAG 完整示例（Spring AI）

```java
@Service
public class RagService {
    @Autowired
    private ChatClient chatClient;
    @Autowired
    private VectorStore vectorStore;

    public String ask(String question) {
        // 1. 从向量库检索相关文档
        List<Document> docs = vectorStore.similaritySearch(
            SearchRequest.query(question).withTopK(3));

        // 2. 拼接上下文
        String context = docs.stream()
            .map(Document::getContent)
            .collect(Collectors.joining("\n\n"));

        // 3. 使用 RAG Prompt 提问
        return chatClient.prompt()
            .system("""
                你是一个知识助手。请根据以下资料回答问题。
                如果资料中没有相关信息，请如实说明。

                参考资料：
                {context}
                """)
            .user(question)
            .call()
            .content();
    }
}
```

### 9.3 Spring AI 核心概念

| 概念 | 说明 |
|------|------|
| **ChatClient** | 对话客户端，统一 API |
| **ChatModel** | 底层对话模型接口 |
| **StreamingChatModel** | 支持流式输出的模型接口 |
| **EmbeddingModel** | 向量嵌入模型接口 |
| **VectorStore** | 向量数据库抽象 |
| **Document** | 文档对象（内容 + 元数据） |
| **DocumentReader** | 文档读取器（PDF、JSON、Text 等） |
| **DocumentTransformer** | 文档转换器（文本分割等） |
| **DocumentWriter** | 文档写入器 |
| **ToolCallback** | Function Calling 回调 |
| **Advisor** | 拦截器链（类似 AOP），如日志、RAG 增强 |

### 9.4 Function Calling（Spring AI）

```java
// 定义 Tool
@Component
public class WeatherTools {

    @Tool(description = "获取指定城市的天气信息")
    public String getWeather(@ToolParam(description = "城市名称") String city) {
        // 调用实际天气 API
        return city + "今天晴，25°C";
    }
}

// 注册并使用 Tool
@Autowired
private WeatherTools weatherTools;

String answer = chatClient.prompt()
    .user("北京今天天气怎么样？")
    .tools(weatherTools)  // 注册工具
    .call()
    .content();
// → 模型自动识别需要调用 getWeather("北京")，然后基于结果回答
```

---

## 十、向量数据库

### 10.1 核心概念

| 名词 | 解释 |
|------|------|
| **Vector / Embedding** | 文本/图片的数学表示，高维浮点数组 |
| **Vector Index** | 向量索引，加速相似性搜索的数据结构 |
| **ANN** | Approximate Nearest Neighbor，近似最近邻搜索 |
| **KNN** | K-Nearest Neighbors，K 最近邻 |
| **HNSW** | Hierarchical Navigable Small World，分层可导航小世界图（常用索引算法） |
| **IVF** | Inverted File Index，倒排文件索引 |
| **PQ** | Product Quantization，乘积量化（压缩向量） |
| **LSH** | Locality-Sensitive Hashing，局部敏感哈希 |

### 10.2 主流向量数据库

| 数据库 | 类型 | 特点 | Java 生态 |
|------|------|------|------|
| **Milvus** | 专用向量数据库 | 分布式、高性能、GPU 加速 | ✅ Milvus SDK |
| **Pinecone** | 云原生向量数据库 | 全托管、无需运维 | ✅ REST API |
| **Weaviate** | 向量数据库 | GraphQL API、内置模块 | ✅ Java Client |
| **Qdrant** | 向量数据库 | Rust 编写、高性能 | ✅ REST API |
| **Chroma** | 轻量向量数据库 | 简单易用、适合原型 | ✅ REST API |
| **PGVector** | PostgreSQL 扩展 | 基于 PG，SQL 友好 | ✅ JDBC/JPA |
| **Redis Stack** | 缓存 + 向量 | 低延迟、内存级 | ✅ Jedis/Lettuce |
| **Elasticsearch** | 搜索引擎 + 向量 | 全文检索 + 向量混合 | ✅ RestHighLevelClient |
| **OpenSearch** | 搜索 + 向量 | ES 分支，KNN 插件 | ✅ Java Client |
| **FAISS** | 向量索引库 | Meta 开源，纯算法 | ✅ JNI 绑定 |
| **Annoy** | 向量索引库 | Spotify 开源，轻量 | ❌ 需 JNI |

### 10.3 选型建议

```
选型决策树：
需要全托管云服务？
├── 是 → Pinecone / Zilliz Cloud
└── 否 → 已有 PostgreSQL？
          ├── 是 → PGVector（最简单，零运维成本）
          └── 否 → 数据量级？
                    ├── < 百万级 → Chroma / Qdrant
                    └── > 百万级 → Milvus / Weaviate
```

---

## 十一、多模态

### 11.1 多模态类型

| 模态 | 输入→输出 | 代表模型 |
|------|----------|---------|
| **Text → Text** | 文本→文本 | GPT-4、Claude、DeepSeek |
| **Text → Image** | 文生图 | DALL-E 3、Stable Diffusion、Midjourney |
| **Text → Video** | 文生视频 | Sora、Runway Gen-3、Kling（可灵） |
| **Text → Audio** | 文生语音（TTS） | OpenAI TTS、ElevenLabs、Fish Audio |
| **Image → Text** | 图生文（视觉理解） | GPT-4V、Claude 3.5、Gemini Pro Vision |
| **Audio → Text** | 语音转文字（ASR） | Whisper、SenseVoice |
| **Image → Image** | 图生图 | Stable Diffusion img2img |
| **Text → Code** | 代码生成 | GPT-4、Claude、DeepSeek-Coder |
| **Text → 3D** | 文生3D模型 | Meshy、Luma AI |

### 11.2 多模态技术名词

| 名词 | 解释 |
|------|------|
| **TTS (Text-to-Speech)** | 文本转语音 |
| **ASR (Automatic Speech Recognition)** | 自动语音识别 |
| **Vision Encoder** | 视觉编码器（如 CLIP ViT） |
| **CLIP** | Contrastive Language-Image Pre-training，图文对齐模型 |
| **DiT** | Diffusion Transformer，扩散+Transformer（Sora 架构） |
| **VAE** | Variational Autoencoder，变分自编码器 |
| **GAN** | Generative Adversarial Network，生成对抗网络 |
| **Tokenizer** | 分词器，将文本/图片切分为 token |
| **Image Tokenization** | 将图片切分为 patches → 送入 Transformer |

---

## 十二、流式输出与 SSE

### 12.1 流式输出概念

```
普通（非流式）：
  Client ──req──→ Server ──[等待全部生成完毕]──→ 一次性返回

流式（Streaming）：
  Client ──req──→ Server ──token1──→ token2──→ token3──→ ... ──→ [DONE]
                             实时逐字返回，体验更好
```

### 12.2 技术协议

| 协议 | 全称 | 特点 |
|------|------|------|
| **SSE** | Server-Sent Events | 服务端→客户端单向推送，基于 HTTP，简单 |
| **WebSocket** | 全双工通信 | 双向实时通信 |
| **Chunked Transfer** | HTTP 分块传输 | 流式 HTTP 响应 |

### 12.3 Spring 流式输出实现

```java
// ===== Spring WebFlux SSE =====
@RestController
public class ChatController {
    @Autowired
    private ChatClient chatClient;

    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@RequestParam String message) {
        return chatClient.prompt()
            .user(message)
            .stream()
            .content();
    }
}

// ===== Server-Sent Events 格式 =====
/*
data: {"content": "你"}
data: {"content": "好"}
data: {"content": "！"}
data: [DONE]
*/
```

### 12.4 流式输出前端接收

```javascript
// 前端 EventSource 接收 SSE
const eventSource = new EventSource('/chat/stream?message=你好');
eventSource.onmessage = (event) => {
    if (event.data === '[DONE]') {
        eventSource.close();
        return;
    }
    const json = JSON.parse(event.data);
    appendToChat(json.content);
};
```

---

## 十三、Token 与计费模型

### 13.1 Token 概念

| 名词 | 解释 |
|------|------|
| **Token** | 文本的最小处理单元，≈ 英文 0.75 个单词，中文 ≈ 1-2 个字符 |
| **Input Token** | 输入消耗的 token（Prompt） |
| **Output Token** | 输出消耗的 token（Completion） |
| **Context Window** | 上下文窗口大小（模型一次能处理的最大 token 数） |
| **Max Output Tokens** | 单次最大输出 token 数 |
| **Tokenizer** | 分词器，如 tiktoken（OpenAI）、cl100k_base |
| **TPM** | Tokens Per Minute，每分钟 token 限制 |
| **RPM** | Requests Per Minute，每分钟请求限制 |

### 13.2 Token 计算示例

```
中文："你好，世界" → 约 4-6 tokens
英文："Hello, world" → 约 3 tokens
代码："public static void main" → 约 5 tokens
图片（GPT-4V）：根据分辨率 85-170 tokens 起

常见模型上下文窗口：
GPT-4o:          128K tokens
GPT-4 Turbo:     128K tokens
Claude 3.5 Haiku: 200K tokens
Claude Opus 4:   200K tokens
Gemini 1.5 Pro:  1M tokens（最大）
DeepSeek-V3:     128K tokens
Qwen2.5:         128K tokens
```

### 13.3 计费模型

```
按量计费（Pay-as-you-go）：
  API 调用费用 = Input Token 数 × 输入单价 + Output Token 数 × 输出单价

  OpenAI GPT-4o 示例（参考）：
    Input:  $2.50 / 1M tokens
    Output: $10.00 / 1M tokens

包月/包年（Provisioned Throughput）：
  预购吞吐量单位（PTU），适合大规模生产环境

本地部署：
  一次性硬件成本 + 电费运维成本
```

---

## 十四、安全与合规

### 14.1 安全风险

| 风险 | 解释 | 防护措施 |
|------|------|---------|
| **Prompt Injection** | 恶意注入指令覆盖 System Prompt | 输入清洗、分隔符隔离、权限最小化 |
| **Jailbreak** | 越狱攻击，绕过安全限制 | 内容审核、安全层 |
| **Data Leakage** | 数据泄露（训练数据/Prompt） | 数据脱敏、不传敏感信息 |
| **Hallucination** | 幻觉，生成虚假信息 | RAG、事实核查 |
| **PII Exposure** | 个人身份信息暴露 | 过滤、脱敏 |
| **DoS** | Token 耗尽攻击 | 限流、Token 预算 |
| **Model Poisoning** | 模型投毒（微调环节） | 数据来源审核 |

### 14.2 合规要求

| 法规/标准 | 适用范围 | 关键要求 |
|------|------|------|
| **《生成式人工智能服务管理暂行办法》** | 中国 | 内容审核、训练数据合法、用户知情同意 |
| **GDPR** | 欧盟 | 数据保护、被遗忘权 |
| **SOC 2** | 国际 | 安全、可用性、保密性 |
| **ISO 42001** | 国际 | AI 管理系统标准 |
| **《数据安全法》** | 中国 | 数据分类分级、跨境传输 |

### 14.3 安全最佳实践

```
1. 输入层     → 内容安全审核（敏感词过滤、越狱检测）
2. 模型层     → System Prompt 加固、Guardrails
3. 输出层     → 内容过滤、事实性检查、格式校验
4. 基础设施层  → API Key 管理、访问控制、审计日志
5. 数据层     → 加密存储、脱敏处理、最小权限
```

---

## 十五、Function Calling / Tool Use

### 15.1 核心概念

> **Function Calling（函数调用 / 工具使用）** 是让 LLM 自主决定何时调用外部 API/函数的机制。模型不会实际执行函数，而是生成结构化的函数调用请求，由开发者的代码来执行。

```
Function Calling 流程：
User: "北京今天天气怎么样？"
  │
  ▼
LLM 判断：需要调用 get_weather(city="北京")
  │
  ▼  (返回 function_call JSON)
Server 代码执行 get_weather("北京") → "晴，25°C"
  │
  ▼  (将结果回传给 LLM)
LLM 基于结果回答："北京今天晴，气温 25°C"
```

### 15.2 Function Calling JSON Schema 定义

```java
// 定义 Tool 的 JSON Schema
public record WeatherFunction() implements Function<WeatherFunction.Request, WeatherFunction.Response> {

    @JsonPropertyDescription("获取指定城市的天气信息")
    public record Request(
        @JsonPropertyDescription("城市名称，如\"北京\"") String city
    ) {}

    public record Response(String weather, double temperature, String unit) {}

    @Override
    public Response apply(Request request) {
        // 实际调用天气 API
        return new Response("晴", 25.0, "celsius");
    }
}
```

### 15.3 并行 Function Calling

```
User: "比较一下北京和上海的天气"

LLM 判断：需要同时调用
  → get_weather(city="北京")
  → get_weather(city="上海")
  
  (两个调用并行执行，或一次响应中返回多个 tool_calls)
```

---

## 十六、常见大模型厂商与 API

### 16.1 国际厂商

| 厂商 | 模型系列 | API 格式 | Java 调用 |
|------|---------|---------|----------|
| **OpenAI** | GPT-4o、GPT-4o-mini、o3、o4-mini | OpenAI API | Spring AI / openai-java |
| **Anthropic** | Claude Opus 4、Sonnet 4、Haiku 4.5 | Anthropic API (Messages) | Spring AI / HTTP |
| **Google** | Gemini 2.5 Pro/Flash | Gemini API | Spring AI / google-cloud-aiplatform |
| **Meta** | Llama 4 Scout/Maverick | 开源，需自部署 | Ollama / vLLM |
| **Mistral** | Mistral Large、Codestral | Mistral API | Spring AI / HTTP |
| **Cohere** | Command R/R+ | Cohere API | cohere-java |

### 16.2 国内厂商

| 厂商 | 模型系列 | API 平台 | Java 调用 |
|------|---------|---------|----------|
| **阿里云** | 通义千问 Qwen 系列 | 百炼（DashScope） | dashscope-sdk-java |
| **百度** | 文心一言 ERNIE 系列 | 千帆大模型平台 | qianfan-java-sdk |
| **字节跳动** | 豆包 Doubao 系列 | 火山引擎 Ark | HTTP API |
| **腾讯** | 混元 Hunyuan 系列 | 腾讯云 | tencentcloud-sdk-java |
| **讯飞** | 星火 Spark 系列 | 讯飞开放平台 | HTTP WebSocket |
| **智谱 AI** | GLM-4 系列 | 智谱开放平台 | zhipuai-sdk-java |
| **深度求索** | DeepSeek-V3、DeepSeek-R1 | DeepSeek API | OpenAI 兼容（复用 OpenAI SDK） |
| **月之暗面** | Moonshot / Kimi | Moonshot API | HTTP API |
| **百川智能** | Baichuan 系列 | 百川 API | HTTP API |
| **MiniMax** | abab 系列 | MiniMax API | HTTP API |
| **零一万物** | Yi 系列 | 零一万物 API | HTTP API |

### 16.3 API 鉴权方式

```
# 认证方式（因厂商而异）：

1. API Key（最常见）
   Authorization: Bearer sk-xxxxxxxxxxxxxxxx

2. AK/SK（国内云厂商常用）
   使用 AccessKey + SecretKey 签名

3. OAuth 2.0
   企业级集成使用
```

---

## 十七、AI 应用架构模式

### 17.1 典型分层架构

```
┌──────────────────────────────────────────────────────┐
│                  前端 (React / Vue / 小程序)          │
│              SSE 流式接收 / WebSocket                │
├──────────────────────────────────────────────────────┤
│                   API Gateway (Kong / Nginx)         │
│              认证 / 限流 / 路由 / 日志               │
├──────────────────────────────────────────────────────┤
│                   Java 后端 (Spring Boot)             │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌─────────┐ │
│  │对话管理   │ │ RAG 服务  │ │ Agent    │ │用户管理  │ │
│  │- Session │ │- 文档处理 │ │- 规划    │ │- 权限    │ │
│  │- 多轮对话 │ │- 向量检索 │ │- 工具调用 │ │- 配额    │ │
│  │- 流式输出 │ │- 重排序  │ │- 反思    │ │- 统计    │ │
│  └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬────┘ │
│       │            │            │            │       │
│  ┌────┴────────────┴────────────┴────────────┴────┐  │
│  │              数据层                               │  │
│  │  MySQL/PG (业务数据) │ Redis (缓存/会话)         │  │
│  │  PGVector/Milvus (向量) │ ES (全文检索)         │  │
│  │  RabbitMQ/Kafka (异步任务) │ MinIO (文件存储)    │  │
│  └─────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────┘
```

### 17.2 AI 应用常见模式

| 模式 | 描述 | 典型场景 |
|------|------|---------|
| **ChatBot** | 对话机器人 | 客服、助手 |
| **RAG Q&A** | 基于知识库的问答 | 企业文档问答、法律咨询 |
| **Copilot** | 辅助编程/写作 | IDE 插件、Word 辅助 |
| **Agent** | 自主执行复杂任务 | 数据分析、自动化运维 |
| **Multi-Agent** | 多智能体协作 | 复杂工作流、模拟社会 |
| **Text-to-API** | 自然语言转 API 调用 | NL2SQL、日志查询 |
| **Content Generation** | 内容生成 | 营销文案、报告生成 |
| **Summarization** | 摘要/总结 | 会议纪要、文档摘要 |

### 17.3 Java 项目包结构建议

```
com.example.ai-app
├── config/              # 配置类
│   ├── AiConfig.java           # Spring AI 配置
│   ├── VectorStoreConfig.java  # 向量库配置
│   └── RetryConfig.java        # 重试/熔断配置
├── controller/          # 控制器
│   ├── ChatController.java
│   └── RagController.java
├── service/             # 业务服务
│   ├── ChatService.java
│   ├── RagService.java
│   ├── EmbeddingService.java
│   └── AgentService.java
├── tools/               # Function Calling 工具
│   ├── WeatherTool.java
│   ├── DatabaseTool.java
│   └── SearchTool.java
├── rag/                 # RAG 相关
│   ├── DocumentLoader.java
│   ├── TextSplitter.java
│   └── RetrieverService.java
├── model/               # 数据模型
│   ├── ChatMessage.java
│   ├── RagDocument.java
│   └── AgentTask.java
├── repository/          # 数据访问
│   ├── ConversationRepository.java
│   └── DocumentRepository.java
└── util/                # 工具类
    ├── TokenCounter.java
    └── PromptTemplateUtil.java
```

---

## 十八、开发工具链与平台

### 18.1 AI 开发平台

| 平台 | 说明 |
|------|------|
| **HuggingFace** | 最大的模型社区，模型仓库 + Spaces 部署 |
| **ModelScope（魔搭）** | 阿里开源模型社区，国内首选 |
| **Ollama** | 本地运行和管理开源模型 |
| **LM Studio** | 桌面端模型管理与推理 |
| **Replicate** | 云端一键运行开源模型 |
| **Together AI** | GPU 云推理平台 |
| **Fireworks AI** | 快速推理平台 |
| **Groq** | LPU 超低延迟推理 |
| **CivitAI** | 图像模型社区（Stable Diffusion 生态）|

### 18.2 开发/编排框架

| 框架 | 语言 | 定位 |
|------|------|------|
| **LangChain** | Python/JS | 通用 AI 应用框架 |
| **LangChain4j** | Java | Java 版 LangChain |
| **Spring AI** | Java | Spring 官方 AI 框架 |
| **LlamaIndex** | Python | 数据索引与 RAG 框架 |
| **Semantic Kernel** | C#/Python/Java | 微软 AI 编排框架 |
| **Dify** | Python | 低代码 AI 应用平台 |
| **FastGPT** | TypeScript | 低代码知识库问答平台 |
| **Flowise** | TypeScript | 拖拽式 AI 工作流 |

### 18.3 Agent 框架

| 框架 | 特点 |
|------|------|
| **LangGraph** | 有状态、有循环的 Agent 编排（LangChain 出品） |
| **CrewAI** | 角色扮演式多 Agent 协作 |
| **AutoGen** | 微软多 Agent 对话框架 |
| **MetaGPT** | 模拟软件公司的多 Agent 框架 |
| **Agno** | 轻量 Agent 框架 |
| **Spring AI Agent** | Spring AI 1.0 M6+ 内置 Agent 支持（Tool Calling + Advisor） |

### 18.4 监控与可观测

| 工具 | 用途 |
|------|------|
| **LangSmith** | LangChain 的调试/监控平台 |
| **LangFuse** | 开源 LLM 可观测平台（追踪、成本、评估） |
| **Phoenix (Arize)** | LLM 可观测与评估 |
| **Weights & Biases** | ML 实验跟踪 |
| **Prometheus + Grafana** | 通用监控（API 延迟、Token 消耗、错误率） |
| **Micrometer** | Spring 指标采集（集成 Prometheus） |

---

## 十九、术语速查表

### A - C

| 缩写 | 全称 | 一句话解释 |
|------|------|----------|
| **AGI** | Artificial General Intelligence | 通用人工智能（尚未实现） |
| **ANN** | Approximate Nearest Neighbor | 近似最近邻搜索算法 |
| **API** | Application Programming Interface | 应用程序编程接口 |
| **ASR** | Automatic Speech Recognition | 语音识别 |
| **Attention** | Attention Mechanism | 注意力机制，Transformer 核心 |
| **BPE** | Byte Pair Encoding | 字节对编码（分词算法） |
| **CoT** | Chain of Thought | 思维链提示法 |
| **CRAG** | Corrective RAG | 纠正式检索增强生成 |
| **CUDA** | Compute Unified Device Architecture | NVIDIA 并行计算架构 |

### D - G

| **DiT** | Diffusion Transformer | 扩散 Transformer（视频生成） |
| **DL** | Deep Learning | 深度学习 |
| **DPO** | Direct Preference Optimization | 直接偏好优化 |
| **Embedding** | — | 文本/图片的向量表示 |
| **Fine-tuning** | — | 微调，在特定数据上继续训练 |
| **FP16 / FP32** | 16/32-bit Floating Point | 半精度/全精度浮点 |
| **Function Calling** | — | 模型自主调用外部函数 |
| **GGUF** | GPT-Generated Unified Format | llama.cpp 量化模型格式 |
| **GPU** | Graphics Processing Unit | 图形处理器（AI 计算主力） |
| **Guardrails** | — | 安全护栏，约束模型输出 |
| **GQA** | Grouped Query Attention | 分组查询注意力（Llama 2+） |
| **Graph RAG** | — | 基于知识图谱的 RAG |

### H - M

| **HNSW** | Hierarchical Navigable Small World | 分层可导航小世界（向量索引） |
| **Instruct / Chat Model** | — | 指令微调后的对话模型 |
| **KV Cache** | Key-Value Cache | 键值缓存（加速推理） |
| **LoRA** | Low-Rank Adaptation | 低秩适配微调 |
| **LLM** | Large Language Model | 大语言模型 |
| **MCP** | Model Context Protocol | 模型上下文协议 |
| **ML** | Machine Learning | 机器学习 |
| **MMR** | Maximal Marginal Relevance | 最大边际相关性（检索多样化） |
| **MoE** | Mixture of Experts | 混合专家架构 |
| **MQA** | Multi-Query Attention | 多查询注意力 |

### N - R

| **NER** | Named Entity Recognition | 命名实体识别 |
| **NLP** | Natural Language Processing | 自然语言处理 |
| **NLU** | Natural Language Understanding | 自然语言理解 |
| **NLG** | Natural Language Generation | 自然语言生成 |
| **PEFT** | Parameter-Efficient Fine-Tuning | 参数高效微调（LoRA 等） |
| **PPO** | Proximal Policy Optimization | 近端策略优化（RLHF 用） |
| **Prompt** | — | 提示词 |
| **PTU** | Provisioned Throughput Unit | 预置吞吐量单位 |
| **QKV** | Query / Key / Value | 注意力机制三矩阵 |
| **QLoRA** | Quantized LoRA | 量化低秩适配 |
| **RAG** | Retrieval-Augmented Generation | 检索增强生成 |
| **RLHF** | Reinforcement Learning from Human Feedback | 基于人类反馈的强化学习 |
| **RoPE** | Rotary Position Embedding | 旋转位置编码 |

### S - Z

| **SFT** | Supervised Fine-Tuning | 有监督微调 |
| **SLM** | Small Language Model | 小语言模型 |
| **SSE** | Server-Sent Events | 服务端推送事件（流式输出） |
| **SSM** | State Space Model | 状态空间模型（Mamba） |
| **TGI** | Text Generation Inference | HuggingFace 推理引擎 |
| **Token** | — | 文本最小处理单元 |
| **ToT** | Tree of Thoughts | 思维树提示法 |
| **TTS** | Text-to-Speech | 文本转语音 |
| **VLM** | Vision-Language Model | 视觉语言模型 |
| **VRAM** | Video RAM | 显存（GPU 内存） |

---

## 学习路线建议

```
Phase 1：基础认知（1-2 周）
├── 理解 LLM 是什么、能做什么
├── 了解 Transformer 基本结构
├── 尝试在线 Chat 产品（ChatGPT / Claude / Kimi）
└── 学习 Prompt Engineering 基础

Phase 2：API 调用实战（1-2 周）
├── 注册 API（OpenAI / 百炼 / DeepSeek）
├── Java 项目调用 API（Spring AI / HTTP）
├── 实现流式输出（SSE + WebFlux）
└── 理解 Token 与计费

Phase 3：RAG 实战（2-3 周）
├── 理解 Embedding 与向量数据库
├── 搭建 PGVector / Milvus / Redis Stack
├── 实现文档问答 RAG 系统
├── 优化：Hybrid Search + Re-ranking

Phase 4：Agent 与 Tool Use（2-3 周）
├── 学习 Function Calling 机制
├── 实现 Tool 定义与注册
├── 理解 ReAct 循环
├── 尝试 Agent 框架

Phase 5：生产化（持续）
├── 安全防护（Prompt Injection 等）
├── 性能优化（缓存、并发、限流）
├── 监控（LangFuse + Prometheus）
├── CI/CD + 灰度发布

Phase 6：进阶
├── 模型微调（LoRA / QLoRA）
├── 多模态应用
├── Multi-Agent 系统
├── MCP Server 开发
```

---

> **持续更新**：AI 大模型领域技术迭代极快，建议持续关注各厂商官方文档、技术博客以及开源社区动态。

> **参考资源**：
> - Spring AI 官方文档：https://docs.spring.io/spring-ai/reference/
> - LangChain4j 文档：https://docs.langchain4j.dev/
> - OpenAI 官方文档：https://platform.openai.com/docs/
> - Anthropic 官方文档：https://docs.anthropic.com/
> - 阿里云百炼文档：https://help.aliyun.com/product/2808366.html
