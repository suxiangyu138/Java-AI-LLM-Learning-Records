# AI Agent 零基础入门 面试宝典
> 基于课程大纲全面覆盖面试高频考点 — 从 Transformer 到 Agent 全链路知识体系

## 目录

1. [一、基础概念速答](#一基础概念速答)
2. [二、深度原理剖析](#二深度原理剖析)
3. [三、实战场景题](#三实战场景题)
4. [四、手写代码题](#四手写代码题)
5. [五、系统设计题](#五系统设计题)
6. [六、常见坑点与最佳实践](#六常见坑点与最佳实践)
7. [七、面试回答模板](#七面试回答模板)
8. [八、快速查漏补缺 Checklist](#八快速查漏补缺-checklist)

---

## 一、基础概念速答

> 面试前快速扫读，确保每个问题都能用 1-2 句话作答。

### Q1: 什么是 Transformer？为什么它比 RNN 强？

Transformer 是一种完全基于 **Self-Attention** 机制的序列建模架构，抛弃了 RNN 的循环结构。它通过 **并行计算** 解决长序列依赖问题，训练速度远快于 RNN，且能捕捉更远距离的语义关系。

### Q2: 什么是 Self-Attention（自注意力）？

Self-Attention 计算序列中每个位置与其他所有位置的关联权重。核心公式为：

```
Attention(Q, K, V) = softmax(Q × K^T / √d_k) × V
```

其中 Q/K/V 来自同一输入经过线性变换，除以 √d_k 防梯度消失。

### Q3: Multi-Head Attention 为什么有效？

多头注意力将 Q/K/V 拆成 h 个头，每个头在不同子空间学习不同的注意力模式（如语法关系、语义关系），最后拼接。相当于 **集成多个独立关注视角**，比单头更丰富。

### Q4: 什么是 Positional Encoding（位置编码）？

Transformer 没有递归结构，必须额外注入位置信息。位置编码使用固定频率的 sin/cos 函数生成位置向量，加到输入嵌入中，让模型感知词语顺序。

### Q5: 什么是 Layer Normalization？

Layer Normalization 对每个样本的所有隐藏层神经元做归一化，稳定训练过程。Transformer 中采用 **Pre-LN**（先归一化再送入子层）或 Post-LN（先子层再归一化）。

### Q6: SFT 和 RLHF 的区别是什么？

| 维度 | SFT（监督微调） | RLHF（人类反馈强化学习） |
|------|----------------|------------------------|
| 数据 | 人工标注的 (输入, 输出) 对 | 人类对模型输出的偏好排序 |
| 目标 | 模仿人类回答格式 | 对齐人类偏好（有用性、诚实性、安全性） |
| 方法 | 交叉熵损失，最大化似然 | PPO 算法，基于奖励模型优化 |
| 阶段 | 第一阶段 | 第二阶段（在 SFT 之后） |

### Q7: RAG 的核心流程是什么？

RAG（Retrieval-Augmented Generation）分四步：
1. **Indexing** — 文档切块 → 向量化 → 存入向量库
2. **Retrieval** — 用户查询向量化 → 相似度搜索 → 召回 Top-K
3. **Rerank** — 对召回结果重排序，提升精度
4. **Generation** — 将检索结果拼入 Prompt → LLM 生成答案

### Q8: 什么是 Chunking？常见策略有哪些？

Chunking 是将长文档切分为检索单元的过程。

| 策略 | 说明 | 适用场景 |
|------|------|---------|
| Fixed-size | 固定 token 数切分 | 通用场景 |
| Recursive Character | 按分隔符递归切分 | 代码、结构化文本 |
| Semantic | 按语义边界切分 | 高精度需求 |
| Agentic | 利用 LLM 决策切分 | 复杂文档 |

### Q9: LangChain 的核心组件有哪些？

LangChain V1.2 核心组件包括：
- **Model I/O** — 模型调用、Prompt 模板、Output Parser
- **Memory** — 对话记忆（Buffer / Window / Summary）
- **Chains** — 串行/并行调用多个组件
- **Agents** — 自主决策调用工具
- **RAG** — 文档加载、向量存储、检索器
- **Callbacks** — 中间件式日志与监控

### Q10: Coze 和 Dify 的区别是什么？

| 维度 | Coze | Dify |
|------|------|------|
| 定位 | 对话式 AI Bot 构建平台 | 开源 LLM 应用开发平台 |
| 开源 | 闭源（字节跳动） | 开源（可自部署） |
| 核心能力 | Bot 商店、插件生态、工作流 | RAG 引擎、Agent、Pipeline |
| 数据隐私 | 托管在 Coze 云 | 可私有化部署 |
| 适用人群 | 非技术人员快速搭建 | 开发者深度定制 |

### Q11: 什么是 Prompt Engineering？核心技巧有哪些？

Prompt Engineering 是设计输入提示以引导 LLM 输出期望结果的技术。

核心技巧：
- **Few-shot** — 给几个示例的上下文学习
- **Chain-of-Thought (CoT)** — 引导模型逐步推理
- **Tree-of-Thought (ToT)** — 探索多条推理路径
- **Role Prompting** — 给模型设定角色身份
- **Structured Output** — 要求输出 JSON/Markdown 格式

### Q12: Agent 的核心架构是什么？

一个典型的 Agent 包含四个核心组件：

```
Agent Core (LLM as Brain)
  ├── Perception — 感知环境和用户输入
  ├── Planning — 任务分解与路径规划
  ├── Tool Use — 调用外部工具/API
  └── Memory — 短期记忆+长期记忆
```

### Q13: 什么是 GraphRAG？

GraphRAG 是微软提出的增强版 RAG，在传统向量检索基础上构建 **知识图谱**（实体-关系结构）。它通过社区检测和层级摘要，能回答需要全局理解的抽象问题，而普通 RAG 仅能检索局部片段。

### Q14: Fine-Tuning 的全流程是什么？

```
数据准备 → 数据清洗 → 格式转换 → 加载基座模型
→ 选择微调方法（Full / LoRA / QLoRA）→ 训练
→ 评估 → 合并权重 → 部署
```

LoRA 通过在冻结的原始权重旁插入低秩矩阵（rank=8~64），显著降低显存需求。

### Q15: 什么是 Rerank？为什么需要它？

Rerank 是对向量检索初选结果用交叉编码器（Cross-Encoder）重新排序。向量检索速度快但精度低，Rerank 用精细模型二次筛选，显著提升 Top-K 命中率。

### Q16: Embedding Model 和 LLM 的区别？

| 维度 | Embedding Model | LLM |
|------|----------------|-----|
| 输出 | 固定维度向量（如 768维） | 自然语言文本 |
| 任务 | 语义相似度计算 | 文本生成、推理、对话 |
| 典型 | text-embedding-3-small | GPT-4, Claude |

### Q17: 什么是 Tool Calling / Function Calling？

LLM 输出结构化 JSON 表示要调用的函数名+参数，由应用层执行并返回结果。例如：

```json
{"name": "get_weather", "arguments": {"city": "北京"}}
```

这是 Agent 调用外部工具的标准接口。

### Q18: 什么是 PPO（Proximal Policy Optimization）？

PPO 是 RLHF 中使用的强化学习算法。它通过**裁剪（clip）策略更新幅度**防止训练崩坏，在保持模型稳定性的同时最大化奖励模型给出的分数。

---

## 二、深度原理剖析

> 面试中会追问的细节，需要理解并能展开说明。

### Q19: 请详细解释 Transformer 的 Encoder 结构

Transformer Encoder 由 N 个相同层堆叠，每层包含：

```
Input → [Multi-Head Attention → Add & LN → FFN → Add & LN]
```

1. **Multi-Head Self-Attention** — 输入 Q/K/V 来自上一层的输出，h 个头并行计算后拼接并通过线性层投影。
2. **Add & Layer Norm** — 残差连接（Residual Connection）解决深层退化问题；LN 稳定激活分布。
3. **Feed-Forward Network (FFN)** — 两层全连接 `ReLU(W₁x + b₁)W₂ + b₂`，中间维度通常放大 4 倍（如 512→2048→512），引入非线性变换。

面试常考点：**为什么用 Layer Norm 而不是 Batch Norm？** 因为序列长度可变，BN 在批次维度统计不稳定的均值和方差，LN 在特征维度归一化更自然。

### Q20: Self-Attention 的复杂度是多少？如何优化？

标准 Self-Attention 的复杂度是 **O(n²·d)**，其中 n 为序列长度，d 为隐藏维度。

优化方案：
| 方法 | 思路 | 复杂度 |
|------|------|--------|
| Sparse Attention | 只计算局部窗口注意力 | O(n·w) |
| Linear Attention | 用核方法近似 | O(n·d²) |
| Flash Attention | GPU 显存 IO 优化 | O(n²) 但加速 2-4 倍 |
| KV Cache | 推理时缓存历史 K/V | 节省重复计算 |

### Q21: 请解释 RLHF 的完整流程

RLHF 分三步：

```
Step 1: SFT — 用人工标注数据微调基座模型
Step 2: 训练 Reward Model — 用偏好对排序训练奖励模型
Step 3: PPO 优化 — 以 Step 2 的奖励模型为信号，优化 LLM 策略
```

关键公式（PPO 优化目标）：
```
max E[ min(ratio × A, clip(ratio, 1-ε, 1+ε) × A) ]
```
其中 ratio = π_θ / π_old 是新旧策略概率比，A 是优势函数。

### Q22: RAG 中 Embedding 和 Rerank 的配合逻辑

Embedding 采用双编码器（Bi-Encoder），将查询和文档分别编码为向量，速度快但精度有限。

Rerank 采用交叉编码器（Cross-Encoder），将查询和文档拼接后一次性经过 Transformer 打分，精度高但速度慢。

标准管道：**Embedding 先粗筛 Top-K（如 100→20），Rerank 再精排 Top-R（如 20→5）**。

### Q23: LangChain 的 Agent 是如何工作的？

LangChain Agent 的循环决策过程：

```
1. LLM 接收 System Prompt + 工具描述
2. LLM 决定是否调用工具（输出 Function Call JSON）
3. 应用层执行工具，返回结果到 LLM
4. LLM 综合初始输入 + 工具结果，给出最终回答
5. 如果还有任务未完成，重复 2-4 步
```

核心参数：`max_iterations`（最大轮次）和 `early_stopping`（提前停止条件）。

### Q24: LoRA 的原理是什么？为什么能省显存？

LoRA（Low-Rank Adaptation）假设权重更新量 ΔW 是低秩的，可分解为两个小矩阵：

```
W' = W₀ + BA, 其中 B ∈ R^(d×r), A ∈ R^(r×d), r << d
```

- 训练时只更新 B 和 A（参数量仅为原来的 r/d）
- 原始 W₀ 冻结，无需存储优化器状态
- 推理时可将 BA 合并入 W₀，不增加推理延迟

### Q25: 几种 RAG 增强方式对比

| 方式 | 说明 | 优势 |
|------|------|------|
| Naive RAG | Index → Retrieve → Generate | 简单直接 |
| Advanced RAG | 查询重写、HyDE、Self-RAG | 提升检索质量 |
| Modular RAG | 可插拔模块（路由、过滤、融合） | 灵活定制 |
| GraphRAG | 构建知识图谱 | 全局理解 |

---

## 三、实战场景题

> 通常以 "如果让你做 XXX，你会怎么做？" 的形式出现。

### Q26: 如何为一个客服系统构建 RAG 方案？

**回答要点：**

1. **文档处理** — 将 FAQ、操作手册等分类，按章节递归切块（chunk_size=512, overlap=128）
2. **向量化** — 使用 bge-large-zh 或 text-embedding-3-small 生成向量
3. **存储** — 使用 Milvus 或 FAISS 做向量检索
4. **检索增强** — 先语义检索 Top-10，用 BGE-Rerank 重排取 Top-3
5. **生成** — 拼入 Prompt：`基于以下文档回答：\n{docs}\n\n问题：{query}`
6. **兜底** — 如果检索相关性低于阈值，回复"无法回答该问题"

### Q27: 如何评估一个 RAG 系统的效果？

| 指标 | 说明 | 测量对象 |
|------|------|---------|
| Hit Rate | 检索结果是否包含正确答案 | 检索模块 |
| MRR | 第一个正确答案的排名倒数平均 | 检索模块 |
| Faithfulness | LLM 回答是否忠实于检索文档 | 生成模块 |
| Answer Relevancy | 回答是否与问题相关 | 生成模块 |
| End-to-End Accuracy | 整体回答正确率 | 全系统 |

### Q28: Agent 在处理多轮对话时如何管理记忆？

**分层记忆策略：**

- **短期记忆** — 对话历史窗口（最近 10 轮），直接用 ChatMessageHistory
- **长期记忆** — 关键信息摘要，定期用 LLM 总结存储到向量库
- **实体记忆** — 提取用户提到的实体（如"我的订单号是 123"），持久化存储

实现方式：LangChain 的 `ConversationSummaryMemory` + `VectorStoreRetrieverMemory` 组合使用。

### Q29: 如何让 LLM 输出结构化的 JSON？

方法一（Output Parser）：

```python
from langchain.output_parsers import PydanticOutputParser
from pydantic import BaseModel, Field

class Product(BaseModel):
    name: str = Field(description="产品名称")
    price: float = Field(description="价格")
    specs: dict = Field(description="规格参数")

parser = PydanticOutputParser(pydantic_object=Product)
prompt = PromptTemplate(
    template="提取产品信息：\n{input}\n{format_instructions}",
    input_variables=["input"],
    partial_variables={"format_instructions": parser.get_format_instructions()}
)
```

方法二（直接约束 Prompt）：在 System Prompt 中写明 `请以 JSON 格式输出，格式为：{"key": "value"}`。

### Q30: 微调一个文本分类模型的完整步骤

```python
# 1. 数据格式
{"instruction": "分类以下文本", "input": "这家酒店服务很差", "output": "负面"}

# 2. 加载模型
from transformers import AutoModelForCausalLM, AutoTokenizer
model = AutoModelForCausalLM.from_pretrained("Qwen/Qwen2-7B-Instruct")
tokenizer = AutoTokenizer.from_pretrained("Qwen/Qwen2-7B-Instruct")

# 3. 应用 LoRA
from peft import LoraConfig, get_peft_model
lora_config = LoraConfig(
    r=8, lora_alpha=32,
    target_modules=["q_proj", "v_proj"],
    lora_dropout=0.1
)
model = get_peft_model(model, lora_config)

# 4. 训练
from transformers import Trainer, TrainingArguments
training_args = TrainingArguments(
    output_dir="./output", num_train_epochs=3,
    per_device_train_batch_size=4,
    learning_rate=2e-4,
    fp16=True
)
trainer = Trainer(model=model, args=training_args, train_dataset=dataset)
trainer.train()
```

### Q31: 线上部署 RAG 系统需要注意什么？

| 关注点 | 建议 |
|--------|------|
| 延迟 | 使用 Embedding Cache + KV Cache |
| 吞吐 | 向量库建索引（IVF_FLAT / HNSW） |
| 文档更新 | 异步增量更新向量库 |
| 幻觉控制 | 设置相关性阈值，低分拒绝回答 |
| 监控 | 记录检索延迟、召回率、用户反馈 |

### Q32: 怎么做一个信息抽取 Agent？

1. **定义 Schema** — 用 Pydantic 定义要抽取的字段
2. **设计 Prompt** — 写明抽取规则：实体类型、关系类型
3. **复用 Tool Calling** — 将抽取结果定义为工具输出
4. **后处理** — 校验 JSON 完整性，缺失字段用默认值填充
5. **批量处理** — 用 LangChain 的 `batch()` 并行调 LLM

---

## 四、手写代码题

> 面试中可能要求白板或在线编码。每题给出核心实现。

### Q33: 手写 Scaled Dot-Product Attention

```python
import torch
import torch.nn.functional as F

def scaled_dot_product_attention(Q, K, V, mask=None):
    """
    Q, K, V: (batch, heads, seq_len, d_k)
    """
    d_k = Q.size(-1)
    scores = torch.matmul(Q, K.transpose(-2, -1)) / (d_k ** 0.5)

    if mask is not None:
        scores = scores.masked_fill(mask == 0, float("-inf"))

    attn_weights = F.softmax(scores, dim=-1)
    output = torch.matmul(attn_weights, V)
    return output, attn_weights
```

### Q34: 手写 Multi-Head Attention 模块

```python
import torch.nn as nn

class MultiHeadAttention(nn.Module):
    def __init__(self, d_model, h, dropout=0.1):
        super().__init__()
        self.h = h
        self.d_k = d_model // h
        self.w_q = nn.Linear(d_model, d_model)
        self.w_k = nn.Linear(d_model, d_model)
        self.w_v = nn.Linear(d_model, d_model)
        self.w_o = nn.Linear(d_model, d_model)
        self.dropout = nn.Dropout(dropout)

    def forward(self, x, mask=None):
        batch, seq_len, _ = x.shape
        Q = self.w_q(x).view(batch, seq_len, self.h, self.d_k).transpose(1, 2)
        K = self.w_k(x).view(batch, seq_len, self.h, self.d_k).transpose(1, 2)
        V = self.w_v(x).view(batch, seq_len, self.h, self.d_k).transpose(1, 2)

        out, _ = scaled_dot_product_attention(Q, K, V, mask)
        out = out.transpose(1, 2).contiguous().view(batch, seq_len, -1)
        return self.w_o(out)
```

### Q35: 手写 LangChain RAG Pipeline

```python
from langchain_community.vectorstores import FAISS
from langchain_community.embeddings import HuggingFaceEmbeddings
from langchain.text_splitter import RecursiveCharacterTextSplitter
from langchain.chains import RetrievalQA
from langchain_community.llms import Ollama

# 1. 文档加载与切分
text_splitter = RecursiveCharacterTextSplitter(
    chunk_size=500, chunk_overlap=50
)
docs = text_splitter.create_documents([long_text])

# 2. 向量化并存储
embeddings = HuggingFaceEmbeddings(model_name="BAAI/bge-small-zh-v1.5")
vectorstore = FAISS.from_documents(docs, embeddings)

# 3. 构建检索 QA Chain
qa_chain = RetrievalQA.from_chain_type(
    llm=Ollama(model="qwen2:7b"),
    retriever=vectorstore.as_retriever(search_kwargs={"k": 3}),
    return_source_documents=True
)

# 4. 执行查询
result = qa_chain.invoke({"query": "什么是注意力机制？"})
print(result["result"])
```

### Q36: 手写一个简单的 Agent（ReAct 模式）

```python
from langchain.agents import Tool, AgentExecutor, create_react_agent
from langchain.prompts import PromptTemplate
from langchain_community.llms import Ollama

# 定义工具
def get_weather(city: str) -> str:
    """获取指定城市的天气"""
    return f"{city} 今天晴，25°C"

tools = [Tool(name="get_weather", func=get_weather, description="查询天气")]

# 创建 ReAct Agent
llm = Ollama(model="qwen2:7b")
prompt = PromptTemplate.from_template(
    """Answer the following question using the tools provided.

Question: {input}
Thought: Let me think step by step.
{agent_scratchpad}"""
)

agent = create_react_agent(llm, tools, prompt)
agent_executor = AgentExecutor(
    agent=agent, tools=tools,
    max_iterations=5, verbose=True
)

agent_executor.invoke({"input": "北京今天天气怎么样？"})
```

### Q37: 手写一个简单的向量检索

```python
import numpy as np
from sklearn.metrics.pairwise import cosine_similarity

class SimpleVectorDB:
    def __init__(self):
        self.documents = []
        self.embeddings = []

    def add(self, text: str, embedding: list):
        self.documents.append(text)
        self.embeddings.append(embedding)

    def search(self, query_emb: list, k: int = 3):
        sims = cosine_similarity([query_emb], self.embeddings)[0]
        top_k_idx = np.argsort(sims)[::-1][:k]
        return [(self.documents[i], sims[i]) for i in top_k_idx]
```

### Q38: 手写 CoT Prompt 调用

```python
from openai import OpenAI

client = OpenAI()

response = client.chat.completions.create(
    model="gpt-4",
    messages=[
        {"role": "system", "content": "请逐步推理并给出最终答案。"},
        {"role": "user", "content": """问题：一个池塘里的睡莲每天翻倍生长，
第30天覆盖整个池塘。问第几天覆盖一半？
请逐步推理。"""}
    ]
)
print(response.choices[0].message.content)
# 逐步推理 → 第29天
```

### Q39: 手写简单的 Tokenizer（BPE 思想）

```python
from collections import Counter
import re

def bpe_tokenize(text, num_merges=10):
    words = re.findall(r'\w+|[^\w\s]', text.lower())
    vocab = {w: list(w) + ['</w>'] for w in words}

    for _ in range(num_merges):
        pairs = Counter()
        for tokens in vocab.values():
            for i in range(len(tokens)-1):
                pairs[(tokens[i], tokens[i+1])] += 1
        if not pairs: break
        best = max(pairs, key=pairs.get)
        new_token = ''.join(best)
        new_vocab = {}
        for w, tokens in vocab.items():
            new_tokens = []
            i = 0
            while i < len(tokens):
                if i < len(tokens)-1 and (tokens[i], tokens[i+1]) == best:
                    new_tokens.append(new_token)
                    i += 2
                else:
                    new_tokens.append(tokens[i])
                    i += 1
            new_vocab[w] = new_tokens
        vocab = new_vocab
    return vocab

# 示例
print(bpe_tokenize("low lower lowest", 5))
```

---

## 五、系统设计题

> 考察架构能力和技术视野。

### Q40: 设计一个企业级 RAG 知识库系统

**架构分层：**

```
┌─────────────────────────────────────────┐
│  Application Layer                      │
│  Chat UI / API Gateway / Auth           │
├─────────────────────────────────────────┤
│  Orchestration Layer                    │
│  Query Rewriting → Routing → Fallback   │
├─────────────────────────────────────────┤
│  Retrieval Layer                        │
│  Hybrid Search (Dense + Sparse)         │
│  → Rerank → Filter → Context Window     │
├─────────────────────────────────────────┤
│  Indexing Layer                         │
│  Doc Parser → Chunker → Embedder → DB   │
├─────────────────────────────────────────┤
│  Storage Layer                          │
│  Vector DB (Milvus) + KB (PG) + Cache   │
└─────────────────────────────────────────┘
```

**关键设计决策：**
- **混合检索** — Dense（语义） + Sparse（BM25 关键词）互补
- **多租户隔离** — Collection 级隔离 + 权限校验
- **异步索引** — 文档上传后异步处理，消息队列缓冲
- **缓存策略** — 高频查询缓存 Embedding 和检索结果

### Q41: 设计一个通用 AI Agent 平台

**核心模块：**

| 模块 | 职责 | 技术选型 |
|------|------|---------|
| Agent Runtime | 执行 Agent 循环（思考→行动→观察） | LangChain / Custom |
| Tool Registry | 注册、发现、调用外部工具 | OpenAPI / gRPC |
| Memory Store | 对话历史、知识记忆、实体记忆 | Redis + Vector DB |
| Task Planner | 复杂任务分解为子任务 | LLM + ReAct / Plan-and-Execute |
| Evaluation | 端到端评测 Agent 表现 | LangSmith / Custom |

**关键挑战：** 工具调用失败恢复、长期任务中断续接、成本控制。

### Q42: 如何设计一个文档信息抽取系统？

```
Input (PDF/Word/图片) → OCR/解析 → Document Layout
→ LLM Chunking → Schema Alignment → Multi-round Extraction
→ Validation → Structured Output (JSON)
```

技术要点：
- **表格抽取** — 使用 Camelot / Tabula 提取表格结构
- **混合策略** — 规则（正则） + 模型（LLM）结合
- **质量校验** — 用另一个 LLM 做输出验证（Self-Consistency）
- **增量更新** — 只抽取变更文档，去重合并

### Q43: 设计一个 Prompt 管理与评测系统

**模块：**

1. **Prompt 版本管理** — Git-like 版本控制，A/B 测试
2. **变量模板引擎** — `{context}`、`{query}` 插值
3. **评测 Pipeline** — 自动化评估准确率、相关性、安全性
4. **监控告警** — LLM 输出延迟、Token 消耗、错误率

### Q44: 如何设计 Fine-Tuning 的数据闭环？

```
原始数据 → 清洗 → 格式标准化 → 人工标注/校验
→ 数据增强 → 训练 → 模型评估 → 错误分析
→ 补充标注 → 再训练（迭代闭环）
```

关键指标：标注一致性（Inter-Annotator Agreement）、数据量至少 500+ 条。

---

## 六、常见坑点与最佳实践

> 面试中展示经验深度的核心素材。

| 坑点 | 现象 | 最佳实践 |
|------|------|---------|
| Chunk 过小 | 上下文不完整，回答碎片化 | Chunk 256-512 tokens，overlap 10-20% |
| Chunk 过大 | 检索噪声多，回答偏离主题 | 按语义段落切分，不要机械固定长度 |
| Embedding 模型与语言不匹配 | 中文检索效果差 | 使用 bge-large-zh / m3e 等中文模型 |
| 未做 Rerank | Top-1 命中率低 | 必须加 Rerank，Precision 提升 20-30% |
| Prompt 过长超出上下文 | 模型截断导致关键信息丢失 | 动态裁剪上下文，保留最相关片段 |
| Agent 循环不终止 | Token 消耗失控 | 设置 max_iterations 和 timeout |
| 工具描述不清晰 | LLM 无法正确选工具 | 用自然语言写清楚功能、参数、返回值 |
| 忽略系统 Prompt 长度 | 指令被对话历史淹没 | 确保指令在 Token 预算前段 |
| 微调数据质量差 | 模型学不到知识 | 至少清洗 3 轮，验证标注一致性 > 80% |
| 未做安全审核 | 输出涉黄暴恐内容 | 加内容安全过滤器 + 输出护栏 |
| 单轮重试策略缺失 | 临时错误导致接口不可用 | 指数退避（Exponential Backoff） |
| 未监控 Token 消耗 | 上线后成本爆炸 | 设置 Token 预算 + 告警阈值 |
| 向量库索引类型错误 | 大库查询慢 | >100万条用 IVF_FLAT 或 HNSW |
| 忽略 Embedding Cache | 重复计算浪费资源 | 高频查询结果缓存 1 小时 |

---

## 七、面试回答模板

> 针对最高频的 5 个问题，提供结构化回答模板。

### 模板 1: "请解释一下 Transformer 的原理"

**回答框架（4 点）：**

1. **结构总览**：Transformer 由 Encoder 和 Decoder 组成，核心是 Self-Attention。
2. **Self-Attention**：Q/K/V 计算注意力权重，公式 `softmax(QK^T/√d)V`，O(n²) 复杂度。
3. **Multi-Head**：h 个注意力头并行，捕捉不同子空间的语义信息。
4. **其他组件**：Positional Encoding（位置感知）、FFN（非线性变换）、Layer Norm（稳定训练）、Residual Connection（缓解退化）。

**一句话总结**："Transformer 用自注意力替代 RNN 的循环，实现并行计算和长距离依赖建模。"

### 模板 2: "RAG 和 Fine-Tuning 怎么选？"

**回答框架（3 点）：**

1. **RAG 更适合**——知识频繁更新、需要引用原文、冷门知识多。成本低，无需训练。
2. **Fine-Tuning 更适合**——需要改变模型行为/风格/格式、特定任务精度要求极高。
3. **组合使用**——先用 RAG 注入知识，再用 SFT 优化回答格式，最后 RLHF 对齐偏好。

**一句话总结**："RAG 管知识注入，Fine-Tuning 管行为对齐，两者互补不冲突。"

### 模板 3: "Agent 和普通 LLM 调用的区别？"

**回答框架（3 点）：**

1. **普通调用**——一次输入一次输出，无状态无工具。
2. **Agent**——循环决策：思考→选择工具→执行→观察结果→继续思考。
3. **关键能力**：工具调用、任务规划、记忆管理、错误恢复。

**一句话总结**："Agent 让 LLM 从'被动回答'变成'主动行动'，能调用工具完成任务。"

### 模板 4: "你项目中遇到的最大挑战是什么？"

**回答框架（STAR 法则）：**

1. **Situation**：做信息抽取项目，文档格式多样（PDF/图片/表格）。
2. **Task**：需要统一抽取到结构化 JSON。
3. **Action**：OCR → Layout 解析 → 按区域分块 → 多轮 LLM 抽取 → 交叉验证。
4. **Result**：准确率从 65% 提升到 92%，支持 5 种文档类型。

**关键**：突出你的**思考过程**而非结果。

### 模板 5: "你怎么保证 LLM 输出的质量？"

**回答框架（5 层防线）：**

1. **Prompt 优化**——Few-shot 示例 + 明确的输出格式约束。
2. **检索质量**——Rerank 阈值过滤，低分直接拒答。
3. **验证层**——用另一个 LLM 做 Factuality Check。
4. **用户反馈**——点赞/点踩数据反哺优化。
5. **监控**——持续跟踪 Faithfulness 和 Answer Relevancy 指标。

**一句话总结**："靠'Prompt + 检索 + 验证 + 反馈 + 监控'五层闭环控制质量。"

---

## 八、快速查漏补缺 Checklist

> 面试前逐条自检，打勾确认掌握。

### Transformer / LLM 基础

- [ ] 能默写 Scaled Dot-Product Attention 公式
- [ ] 能说出 Multi-Head Attention 和单头的区别
- [ ] 理解 Positional Encoding 为什么用 sin/cos
- [ ] 知道 Layer Norm 和 Batch Norm 的区别
- [ ] 能解释 FFN 的结构和作用
- [ ] 理解 Pre-LN 和 Post-LN 的区别
- [ ] 知道 Transformer 的 O(n²) 复杂度瓶颈
- [ ] 了解 Flash Attention / KV Cache 等优化手段

### RAG

- [ ] 能画出 RAG 四步流程图（Index → Retrieve → Rerank → Generate）
- [ ] 知道至少 3 种 Chunking 策略及其适用场景
- [ ] 理解 Embedding 和 Rerank 的配合逻辑
- [ ] 了解 GraphRAG 的核心思路
- [ ] 知道 Hybrid Search（Dense + Sparse）的原理
- [ ] 知道常用的向量数据库（FAISS / Milvus / Pinecone）
- [ ] 知道如何评估 RAG 效果（Hit Rate / MRR / Faithfulness）

### Agent

- [ ] 能说清 Agent 的核心四组件（Perception / Planning / Tool / Memory）
- [ ] 理解 ReAct 模式的 "Thought → Action → Observation" 循环
- [ ] 知道 Tool Calling / Function Calling 的原理
- [ ] 了解如何管理 Agent 的多轮对话记忆
- [ ] 了解 Plan-and-Execute 与 ReAct 的区别

### LangChain

- [ ] 了解 Model I/O、Memory、Chains、Agents 四个核心模块
- [ ] 能用 LangChain 写一个 RAG Pipeline
- [ ] 能写一个 LangChain Agent 调用工具
- [ ] 了解 PydanticOutputParser 的结构化输出
- [ ] 知道 LangChain 的 Callback 机制

### Fine-Tuning

- [ ] 知道 Full Fine-Tuning / LoRA / QLoRA 的区别
- [ ] 能说清 LoRA 的 low-rank 矩阵分解原理
- [ ] 知道微调的数据格式（对话模板、Instruction 格式）
- [ ] 了解 SFT 和 RLHF 的区别和配合方式
- [ ] 了解 PPO 的核心思想（clip 机制）

### Prompt Engineering

- [ ] 能说出 Few-shot / CoT / ToT 的区别
- [ ] 知道如何设计 System Prompt
- [ ] 了解 Structured Output 的实现方法
- [ ] 知道 Role Prompting 和 Negative Prompting

### 平台与工具

- [ ] 了解 Coze 和 Dify 的定位区别
- [ ] 知道至少一种 Embedding Model 名称
- [ ] 知道至少一种向量数据库
- [ ] 了解常用的评测框架（LangSmith / RAGAS）

### 系统设计

- [ ] 能设计一个简单的 RAG 知识库系统
- [ ] 能设计一个 Agent 平台的核心模块
- [ ] 能设计一个信息抽取 Pipeline
- [ ] 了解 Prompt 管理和版本控制方案

---

> **复习策略建议**：先通读一遍，标记红/黄/绿三种掌握程度。红色重点攻克，黄色巩固细节，绿色快速过。面试前一天只看红色和黄色条目。
>
> **祝面试顺利，Offer 到手！**

---

*本宝典基于已有课程大纲整理，覆盖从 Transformer 原理到 Agent 工程实践的全链路知识体系。适用于应届生和转行 AI Agent 方向的零基础候选人。*
