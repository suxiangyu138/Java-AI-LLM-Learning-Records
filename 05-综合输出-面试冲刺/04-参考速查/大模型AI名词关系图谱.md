# 主流大模型 AI 名词关系图谱

> 一图胜千言。理清从底层概念到上层应用的全景关系网。

---

## 一、概念层级关系（从大到小）

```
人工智能 (AI — Artificial Intelligence)
│
├── 机器学习 (ML — Machine Learning)
│   │
│   ├── 传统机器学习
│   │   ├── 监督学习：线性回归、决策树、SVM、XGBoost
│   │   ├── 无监督学习：K-Means、PCA、DBSCAN
│   │   └── 强化学习：Q-Learning、PPO
│   │
│   └── 深度学习 (DL — Deep Learning)
│       │
│       ├── 判别模型：CNN、RNN、LSTM、Transformer
│       │
│       └── 生成式 AI (Generative AI)
│           │
│           ├── 文本生成：GPT、LLaMA、Claude、Gemini
│           ├── 图像生成：Stable Diffusion、DALL·E、Midjourney
│           ├── 视频生成：Sora、Runway、Kling
│           ├── 音频/语音：Whisper、TTS 模型
│           └── 代码生成：Codex、CodeLlama、StarCoder
│
└── 非 ML 方法：规则系统、专家系统、搜索算法
```

**一句话**：AI ⊃ ML ⊃ DL ⊃ 生成式 AI ⊃ 大语言模型 (LLM)

---

## 二、大模型架构谱系

### 2.1 按 Transformer 架构分类

```
                     Transformer (2017, "Attention Is All You Need")
                            │
          ┌─────────────────┼──────────────────┐
          │                 │                  │
   Encoder-only        Decoder-only       Encoder-Decoder
   (双向注意力)         (因果/单向注意力)      (完整架构)
          │                 │                  │
    ┌─────┴──────┐     ┌────┴────────┐    ┌───┴────────┐
    │            │     │             │    │            │
  BERT         RoBERTa  GPT系列    LLaMA  T5          BART
  (2018)       (2019)  GPT-1 (2018) (2023) (2020)    (2020)
                        GPT-2 (2019) Qwen
                        GPT-3 (2020) DeepSeek           │
                        GPT-4 (2023) Mistral         GLM (ChatGLM
                                                 双向编码+自回归解码)
```

**关键区分**：

| 架构 | 注意力方式 | 代表模型 | 擅长任务 |
|------|-----------|----------|----------|
| Encoder-only | 双向（看到前后文） | BERT、RoBERTa | 分类、NER、阅读理解（理解任务） |
| Decoder-only | 因果单向（只看上文） | GPT、LLaMA、Claude | 文本生成、对话（生成任务） |
| Encoder-Decoder | 编码双向 + 解码单向 | T5、BART、GLM | 翻译、摘要、问答 |

> **现代主流 LLM 全是 Decoder-only**：GPT-4、Claude 4、Gemini 2.5、LLaMA 3/4、Qwen 2.5/3、DeepSeek-V3/R1、Mistral、Gemma

---

## 三、主流模型全景图

### 3.1 闭源商业模型

| 模型 | 公司 | 架构特点 | 关键能力 |
|------|------|----------|----------|
| **GPT-4o / 4.1** | OpenAI | Decoder-only, 多模态原生 | 文本/图像/语音全模态 |
| **GPT-5** | OpenAI | 多模态, 推理增强 | 复杂推理、长上下文 |
| **Claude 4 (Opus/Sonnet/Haiku)** | Anthropic | Decoder-only, Constitutional AI | 长上下文 200K、安全对齐、代码 |
| **Gemini 2.5 (Pro/Flash)** | Google | Decoder-only, MoE? | 多模态、超长上下文 1M+ |
| **Grok-3** | xAI | Decoder-only | 实时信息、推理 |
| **豆包 (Doubao)** | 字节跳动 | — | 中文对话 |
| **文心一言 4.5** | 百度 | — | 中文、搜索增强 |
| **通义千问 (Qwen-Max)** | 阿里云 | Decoder-only | 中文、多模态 |

### 3.2 开源模型

| 模型 | 厂商 | 参数规模 | 亮点 |
|------|------|----------|------|
| **LLaMA 4 (Scout/Maverick)** | Meta | 17B-400B | MoE 架构，开源标杆 |
| **DeepSeek-V3** | DeepSeek | 671B (37B 激活) | MoE，极致性价比 |
| **DeepSeek-R1** | DeepSeek | 671B (37B 激活) | 推理增强 (RL)，思维链 |
| **Qwen 2.5 / Qwen 3** | 阿里 | 0.5B-235B | 全尺寸开源，中文最强之一 |
| **Mistral Large 2** | Mistral AI | 123B | 欧洲最强 |
| **Gemma 3** | Google | 1B-27B | 轻量开源 |
| **Phi-4** | Microsoft | 14B | 小模型高性能 |
| **Yi (零一万物)** | 01.AI | 6B-34B | 中文优秀 |
| **MiniCPM** | 面壁智能 | 2B-8B | 端侧部署 |
| **ChatGLM-4** | 智谱 | 9B-128B | 国产先驱 |

### 3.3 模型家族生态

```
OpenAI 生态：GPT-4o → GPT-4.1 → GPT-5（闭源旗舰）
    │
    └── o1 / o3 / o4-mini（推理增强系列，擅长数学/编程/逻辑）

Anthropic 生态：Claude 3.5 → Claude 4 Opus / Sonnet / Haiku
    │
    └── Constitutional AI + RLAIF（安全对齐路线）

Google 生态：Gemini 1.5 → 2.0 → 2.5 Pro / Flash
    │
    └── 多模态原生 + 超长上下文 (1M-2M tokens)

Meta 生态：LLaMA 2 → 3 → 4（开源标杆，推动社区）
    │
    └── LLaMA 衍生：Alpaca、Vicuna、Chinese-LLaMA-Alpaca

DeepSeek 生态：DeepSeek-V2 → V3 (MoE) → R1 (推理) → R2
    │
    └── 极致性价比路线，MoE + 多头潜在注意力 (MLA)

阿里生态：Qwen → Qwen 2.5 → Qwen 3（全尺寸 + 多模态）
    │
    └── Qwen-VL（多模态）、Qwen-Audio、Qwen-Agent
```

---

## 四、训练相关名词关系

### 4.1 完整训练流水线

```
        Pre-training (预训练)
               │
    ┌──────────┴─────────────┐
    │  海量无标注互联网文本    │
    │  自监督：Next Token Pred │
    │  产出：Base Model (基座) │
    │  耗时：数月，千万美元      │
    └──────────┬─────────────┘
               │
               ▼
        SFT (Supervised Fine-Tuning，监督微调)
               │
    ┌──────────┴─────────────┐
    │  高质量人工标注问答对     │
    │  监督学习：模仿人类回答   │
    │  产出：Chat / Instruct   │
    │  耗时：数天-数周          │
    └──────────┬─────────────┘
               │
               ▼
        Alignment (对齐，让模型"听话、安全、有用")
               │
    ┌──────────┴──────────────┐
    │                         │
    ▼                         ▼
  RLHF                      DPO
  (人类反馈强化学习)         (直接偏好优化)
    │                         │
  Reward Model              不需要 Reward Model
  → PPO 优化                → 直接从偏好对学习
    │                         │
    ▼                         ▼
       Aligned Model (对齐后模型)
```

### 4.2 各训练方法对比

| 阶段 | 数据 | 学习方式 | 角色 |
|------|------|----------|------|
| **Pre-training** | 数万亿 token 无标注文本 | 自监督（预测下一个 token） | 学会语言、知识、推理 |
| **SFT** | 数万-数十万条问答/指令 | 监督学习 | 学会对话格式、遵循指令 |
| **RLHF** | 人类偏好比较数据 | 强化学习 (PPO) | 学会"好回答"的标准 |
| **DPO** | 人类偏好比较数据 | 直接优化（无需 RM） | 替代 RLHF，更简单稳定 |
| **RLVR/GRPO** | 可验证奖励（数学/代码） | 强化学习（无需 RM） | 增强推理能力（DeepSeek-R1 路线）|

### 4.3 微调相关名词

```
Fine-tuning (微调)
│
├── Full Fine-tuning（全量微调）
│   更新全部参数，效果最好，最贵
│
├── Parameter-Efficient Fine-Tuning (PEFT，参数高效微调)
│   │
│   ├── LoRA（Low-Rank Adaptation）
│   │   用低秩矩阵 AB 近似参数更新 ΔW = BA
│   │   只训练 A、B，冻结原始权重
│   │   参数量 ~0.1%-1%，可以"即插即用"
│   │
│   ├── QLoRA
│   │   LoRA + 4-bit 量化基础模型
│   │   单张 4090 就能微调 70B 模型
│   │
│   ├── Adapter
│   │   在层间插入小型可训练模块
│   │
│   ├── Prefix Tuning / P-Tuning v2
│   │   在每层前加可训练的虚拟 token（soft prompt）
│   │
│   └── IA³（Infused Adapter by Inhibiting and Amplifying）
│       对 K/V/FFN 乘缩放向量，极轻量
│
├── Instruction Tuning（指令微调 = SFT 的一种）
│   用（指令→回答）数据训练，让模型学会遵循指令
│
├── Continued Pre-training（继续预训练）
│   用领域数据（医学/法律/代码）继续预训练
│   给模型注入垂直领域知识
│
└── Post-Training（后训练 = SFT + RLHF/DPO 的总称）
```

---

## 五、推理与部署相关名词

### 5.1 推理架构

```
用户请求
    │
    ▼
┌──────────────┐
│  推理引擎     │  ← vLLM / TGI / Ollama / TensorRT-LLM
│  (Inference)  │
└──────┬───────┘
       │
       ▼
┌──────────────┐
│  量化模型     │  ← GPTQ / AWQ / GGUF / bitsandbytes
│  (Quantized)  │
└──────┬───────┘
       │
       ▼
┌──────────────┐
│  GPU/CPU/    │  ← CUDA / ROCm / Metal / Vulkan
│  NPU         │
└──────────────┘
```

### 5.2 推理引擎对比

| 引擎 | 特点 | 适用场景 |
|------|------|----------|
| **vLLM** | PagedAttention, 连续批处理, 高吞吐 | 生产环境服务 |
| **Ollama** | 一键部署，极简 CLI，量化模型库 | 本地开发体验 |
| **llama.cpp** | C++ 纯 CPU 推理，GGUF 格式 | 消费级设备本地运行 |
| **TGI (Text Generation Inference)** | HuggingFace 官方，兼容性好 | HF 生态用户 |
| **TensorRT-LLM** | NVIDIA 官方极致优化 | 企业级 GPU 集群 |
| **SGLang** | 结构化生成，RadixAttention | 前沿研究/生产 |
| **LMDeploy** | TurboMind 引擎，高吞吐 | 国产部署方案 |

### 5.3 量化格式

```
量化 (Quantization) — 把模型参数从高精度压缩到低精度

精度层级：FP32 → FP16 → BF16 → INT8 → INT4 → INT2/NVFP4

量化方法：
│
├── 训练后量化 (PTQ — Post-Training Quantization)
│   │
│   ├── GPTQ：逐层量化 + 最优脑外科校正
│   ├── AWQ：激活感知 + 权重加权
│   ├── GGUF (GGML Universal Format)：llama.cpp 专用格式
│   │   常见：Q4_K_M（推荐）、Q5_K_M、Q8_0、Q2_K
│   └── bitsandbytes：HuggingFace 集成的 INT8/NF4 量化
│
└── 量化感知训练 (QAT)
    训练时就考虑量化误差，质量更高但成本大
```

**模型格式对应关系**：

| 格式后缀 | 量化方法 | 推理引擎 |
|----------|----------|----------|
| `.bin` / `.safetensors` | 无/原始 | PyTorch/HF |
| `.gguf` | GGUF (Q4_K_M, Q5_K_M...) | llama.cpp / Ollama |
| `.pt` (GPTQ) | GPTQ INT4/INT8 | vLLM / TGI |
| `.awq` | AWQ INT4 | vLLM / TGI |

---

## 六、模型架构高级名词

### 6.1 MoE (Mixture of Experts，混合专家)

```
输入 token
    │
    ▼
┌──────────────┐
│  Router/Gate  │ ← 为每个 token 选择 top-k 个专家
│  (路由/门控)   │
└──────┬───────┘
       │
   ┌───┼───┬───┐
   ▼   ▼   ▼   ▼
 Expert Expert Expert ... Expert
   0     1     2          N-1
   │     │     │          │
   └──┬──┴──┬──┘──────────┘
      │     │
      ▼     ▼
   加权合并输出
```

**核心**：MoE 有大量专家（Expert），但每个 token 只激活一小部分（通常 top-2），所以总参数多但计算量小。

| 属性 | Dense 模型 | MoE 模型 |
|------|-----------|----------|
| 所有参数激活 | ✅ 全部 | ❌ 部分 (~5%-10%) |
| 总参数量 | 7B, 70B... | 52B总 → 7B激活 |
| 训练速度 | 基准 | 更快（同计算量） |
| 推理显存 | 全量加载 | 全量加载（但计算少） |
| 代表 | LLaMA、Qwen-Dense | DeepSeek-V3、Mixtral 8×7B、LLaMA 4 Maverick |

### 6.2 其他关键架构概念

| 名词 | 含义 | 代表模型 |
|------|------|----------|
| **MLA (Multi-head Latent Attention)** | 将 KV 压缩到低维潜在空间，大幅减少 KV Cache | DeepSeek-V2/V3 |
| **GQA (Grouped Query Attention)** | 多个 Query 头共享一组 KV 头 | LLaMA 2/3、Mistral |
| **MQA (Multi-Query Attention)** | 所有 Query 头共享一组 KV | PaLM、Gemini (早期) |
| **Flash Attention** | CUDA kernel 级 IO 优化 | 几乎所有现代 LLM |
| **RoPE (旋转位置编码)** | 用旋转矩阵编码位置 | LLaMA、Qwen、DeepSeek |
| **ALiBi** | 线性偏置替代位置编码 | BLOOM、旧版 MPT |
| **SwiGLU** | Swish + GLU 激活的 FFN | LLaMA、PaLM |
| **RMSNorm** | 去除均值中心化的 LayerNorm | LLaMA、大多数模型 |
| **Pre-Norm** | 先 Norm 再 Attention/FFN | GPT-3+ 标准做法 |

---

## 七、Agent（智能体）相关名词

### 7.1 Agent 概念层级

```
LLM (大语言模型)
│
├── Prompt Engineering (提示工程)
│   最简单的使用方式：问 → 答
│
├── RAG (Retrieval-Augmented Generation，检索增强生成)
│   检索外部知识 → 增强回答
│   LLM + 向量数据库 + 检索器
│
├── Function Calling / Tool Use (工具调用)
│   LLM 调用外部 API/工具
│   让模型"动手"，不只是"动嘴"
│
├── Agent (智能体)
│   LLM + 规划 + 工具调用 + 记忆 + 行动循环
│   自主决策 → 执行 → 观察 → 调整
│   Think → Act → Observe → Think → ...
│
└── Multi-Agent (多智能体)
    多个 Agent 协作/竞争
    分角色：分析师 Agent + 程序员 Agent + 审查 Agent
```

### 7.2 Agent 核心组件

```
┌─────────────────────────────────────────┐
│                 Agent                    │
│  ┌─────────┐  ┌──────────┐              │
│  │  LLM    │  │  记忆     │              │
│  │ (大脑)   │  │ (Memory)  │              │
│  └────┬────┘  └────┬─────┘              │
│       │            │                     │
│  ┌────┴────────────┴─────┐              │
│  │      规划 (Planning)   │              │
│  │  - 任务分解             │              │
│  │  - 反思与修正           │              │
│  └────┬──────────────────┘              │
│       │                                 │
│  ┌────┴──────────────────┐              │
│  │   工具调用 (Tool Use)   │              │
│  │  - 搜索/浏览器/代码执行  │              │
│  │  - API 调用/文件操作    │              │
│  └────────────────────────┘              │
└─────────────────────────────────────────┘
```

### 7.3 关键 Agent 框架

| 框架 | 特点 |
|------|------|
| **LangChain** | Agent 开发最流行的 Python 框架 |
| **LangGraph** | 有状态、图结构的 Agent 工作流 |
| **AutoGPT** | 第一个现象级自主 Agent |
| **CrewAI** | 多 Agent 协作框架 |
| **MCP (Model Context Protocol)** | Anthropic 提出的 Agent ↔ 工具的标准化协议 |
| **A2A (Agent-to-Agent)** | Google 提出的 Agent 间通信协议 |
| **Dify / Coze** | 低代码 Agent 构建平台 |
| **Semantic Kernel** | 微软的 Agent 开发 SDK |

---

## 八、应用层相关名词

### 8.1 常见 LLM 应用形态

```
┌────────────────────────────────────────────┐
│              应用形态                        │
│                                            │
│  Chatbot         AI 搜索        AI 编程     │
│  (客服/陪伴)     (Perplexity)   (Copilot/   │
│                  (秘塔搜索)      Cursor/Cline│
│                                            │
│  AI 写作         AI 绘画        AI 视频     │
│  (Jasper/        (Midjourney/   (Sora/     │
│   Notion AI)      SD/DALL·E)     Runway)    │
│                                            │
│  AI 数据分析      AI 教育        Workflow   │
│  (ChatGPT Code   (Duolingo      (n8n/      │
│   Interpreter)    Max)           Dify)     │
└────────────────────────────────────────────┘
```

### 8.2 RAG 技术栈

```
文档
  │
  ▼
┌──────────┐    ┌───────────┐    ┌──────────┐
│ 文档解析  │ →  │ 文本分块   │ →  │ 向量嵌入   │
│ (PDF/DOCX │    │ (Chunking) │    │ (Embedding│
│  /HTML)   │    │ 512/1024   │    │  Model)   │
└──────────┘    └───────────┘    └─────┬─────┘
                                       │
                                       ▼
                                  ┌──────────┐
                                  │ 向量数据库 │
                                  │ (Vector  │
                                  │  Store)  │
                                  └─────┬────┘
                                        │
  用户问题 → Embedding → 相似检索 ───────┘
                                        │
                                  Top-K 相关片段
                                        │
                                        ▼
                                   LLM 生成回答
                            (Prompt: 问题 + 检索到的上下文)
```

**RAG 关键技术点**：

| 环节 | 技术选择 |
|------|----------|
| Embedding 模型 | text-embedding-3、BGE、GTE、Jina |
| 向量数据库 | Milvus、Pinecone、Qdrant、Weaviate、Chroma、FAISS |
| 分块策略 | 固定大小、语义分块、递归分块、HyDE |
| 检索策略 | 稠密检索、稀疏检索 (BM25)、混合检索、Rerank |
| 高级 RAG | Self-RAG、Corrective RAG、Graph RAG、Agentic RAG |

### 8.3 Prompt Engineering

```
基础技巧：
├── Zero-shot：不给例子，直接问
├── Few-shot：给几个例子再问
├── Chain-of-Thought (CoT)：让模型"一步步思考"
├── Role Prompting：指定角色 "你是一个资深 Python 工程师"
└── Structured Output：要求 JSON/XML 格式输出

进阶技巧：
├── Self-Consistency：多次采样 + 投票
├── Tree of Thoughts (ToT)：树状搜索多个推理路径
├── ReAct：推理 (Reasoning) + 行动 (Acting) 交替
└── Prompt Compression：压缩长 prompt 节省 token
```

---

## 九、评测与基准

### 9.1 常见评测基准

| 基准 | 测什么 | 题型 |
|------|--------|------|
| **MMLU** | 多学科知识 (57 个学科) | 选择题 |
| **MMLU-Pro** | MMLU 升级版，更难 | 选择题 |
| **HumanEval** | 代码生成 | 编程题 |
| **MBPP** | Python 编程 | 编程题 |
| **GSM8K** | 小学数学应用题 | 数学 |
| **MATH** | 竞赛数学 | 数学 |
| **HellaSwag** | 常识推理 | 选择题 |
| **ARC** | 科学推理 | 选择题 |
| **Winogrande** | 代词消歧 | 选择题 |
| **TruthfulQA** | 真实性（是否胡说） | 问答 |
| **AIME** | 美国数学邀请赛（高难） | 数学 |
| **GPQA** | 研究生级问答 | 多选题 |
| **SWE-bench** | 真实软件工程任务 | 代码修复 |
| **Chatbot Arena** | 人类盲评 (LMSYS) | Elo 排名 |

### 9.2 评测维度

```
评估维度：
├── 知识：MMLU、GPQA
├── 推理：ARC、GSM8K、MATH、AIME
├── 代码：HumanEval、MBPP、SWE-bench、LiveCodeBench
├── 语言：MultiLingual Benchmarks
├── 安全：Toxic、Bias、Red-teaming
├── 幻觉：TruthfulQA、HaluEval
├── 指令遵循：IFEval、MT-Bench
├── 长上下文：Needle in a Haystack、RULER
└── 多模态：MMBench、MMMU、Video-MME
```

---

## 十、一站式关系速览图

```
                    ┌──────────────┐
                    │ AI 人工智能    │
                    └──────┬───────┘
                           │
                    ┌──────┴───────┐
                    │ ML 机器学习    │
                    └──────┬───────┘
                           │
              ┌────────────┼────────────┐
              │            │            │
        监督学习      无监督学习     强化学习
              │            │            │
              └────────────┼────────────┘
                           │
                    ┌──────┴───────┐
                    │ DL 深度学习    │
                    └──────┬───────┘
                           │
                    ┌──────┴──────────┐
                    │ Generative AI   │
                    │ 生成式 AI        │
                    └──────┬──────────┘
                           │
              ┌────────────┼────────────┬──────────┐
              │            │            │          │
          文本生成      图像生成      视频生成    音频生成
              │            │            │          │
         ┌────┴────┐  Stable      Sora/Runway   Whisper
         │         │  Diffusion                 /TTS
    Decoder-only  Encoder-
    (GPT/Claude)  Decoder
              │
    ┌─────────┼──────────┐
    │         │          │
 训练阶段   推理阶段   应用层
    │         │          │
Pre-training 推理引擎   Chatbot
SFT         vLLM       RAG
RLHF/DPO    Ollama     Agent
LoRA/QLoRA  量化       Code Copilot
```

---

## 十一、快速索引（按字母）

| 名词 | 全称 | 一句话解释 |
|------|------|-----------|
| **Agent** | 智能体 | LLM 驱动，自主规划+执行任务的系统 |
| **AGI** | 通用人工智能 | 达到/超过人类水平的全领域 AI |
| **COT** | 思维链 (Chain of Thought) | 让模型一步步推理而不仅给最终答案 |
| **Dense** | 稠密模型 | 每个 token 激活所有参数（传统架构） |
| **DPO** | 直接偏好优化 | 替代 RLHF 的偏好对齐方法 |
| **Embedding** | 向量嵌入 | 将文本/图像映射为高维向量表示 |
| **Flash Attention** | — | 优化 Attention 计算的 CUDA kernel |
| **GQA** | 分组查询注意力 | 多个 Q 头共享一组 KV 头 |
| **Hallucination** | 幻觉 | 模型自信地输出虚假/不存在的信息 |
| **KV Cache** | 键值缓存 | 缓存已计算的 Key/Value 避免重复计算 |
| **LLM** | 大语言模型 | 基于 Transformer 的超大规模语言模型 |
| **LoRA** | 低秩适配 | 用低秩矩阵微调大模型，高效省钱 |
| **MCP** | 模型上下文协议 | Agent 调用外部工具的标准化协议 |
| **MoE** | 混合专家 | 总参数多但每次只激活部分专家的架构 |
| **RAG** | 检索增强生成 | 先检索相关知识，再让 LLM 生成回答 |
| **RLHF** | 人类反馈强化学习 | 用人类偏好训练奖励模型指导 LLM |
| **Scaling Law** | 扩展定律 | 模型性能随参数/数据/算力幂律增长 |
| **SFT** | 监督微调 | 用高质量标注数据微调基座模型 |
| **Temperature** | 温度参数 | 控制输出随机性 (高→创造，低→确定) |
| **Token** | 令牌/词元 | LLM 处理文本的最小单位（≈0.75 英文词） |
| **Transformer** | — | 2017 年提出的 Attention 架构，LLM 基石 |

---

> **一句话贯穿全文**：AI ⊃ ML ⊃ DL ⊃ Generative AI ⊃ LLM，当代 LLM 几乎都是 **Decoder-only Transformer**，通过 **Pre-training → SFT → RLHF/DPO** 训练，用 **LoRA/量化** 降本，靠 **RAG + Agent + MCP** 落地应用。
