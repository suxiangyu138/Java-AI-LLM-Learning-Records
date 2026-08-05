# LangChain教程 面试宝典
> 基于LangChain V1.3全套实战教程大纲，涵盖LangChain核心架构、RAG全链路优化、Agent构建、LangGraph多智能体及MCP协议集成面试考点

## 目录
1. [一、基础概念速答](#一基础概念速答15-20题)
2. [二、深度原理剖析](#二深度原理剖析10-15题)
3. [三、实战场景题](#三实战场景题8-12题)
4. [四、手写代码题](#四手写代码题5-8题)
5. [五、系统设计题](#五系统设计题3-5题)
6. [六、常见坑点与最佳实践](#六常见坑点与最佳实践)
7. [七、面试回答模板](#七面试回答模板top-5高频题的结构化回答模板)
8. [八、快速查漏补缺Checklist](#八快速查漏补缺-checklist)

---

## 一、基础概念速答（15-20题）

### 1.1 LangChain是什么？解决了什么问题？
LangChain是一个**大语言模型（LLM）应用开发框架**，旨在解决LLM在实际应用中的三大痛点：
- **上下文长度限制**：通过链式调用、摘要和检索增强生成（RAG）解决
- **工具集成困难**：提供标准化的Tool Calling和Agent机制
- **状态管理复杂**：通过LangGraph支持有状态、多步骤的工作流编排

> 💡 面试中可强调：LangChain的核心理念是"LLM应用开发的乐高积木"，组件化、可组合是其最大特色。

### 1.2 LangChain V1.3相比旧版本的新特性？
LangChain V1.3架构做了重大调整：

| 特性 | V1.3变化 |
|------|---------|
| 架构重心 | 从Chain转向LangGraph，Chain被标记为Legacy |
| Runnable协议 | 统一了所有组件的调用接口(`invoke`/`batch`/`stream`) |
| Agent体系 | 重构为LangGraph-based Agent，更灵活的状态管理 |
| MCP集成 | 原生支持MCP协议接入 |
| LCEL强化 | 管道的可组合性和流式支持增强 |

### 1.3 LangChain的核心组件有哪些？
| 组件 | 作用 |
|------|------|
| **Model I/O** | LLM调用封装（ChatOpenAI、ChatAnthropic等）|
| **Retrieval** | 文档加载、文本分割、向量存储、检索器 |
| **Chain / LCEL** | 串联多个组件的调用链（Legacy Chain 和 RunnableSequence）|
| **Agent** | 让LLM自主决策调用哪些工具 |
| **Memory** | 对话历史管理（ConversationBufferMemory等）|
| **Callbacks** | 事件回调机制（日志、监控、追踪）|

### 1.4 LCEL是什么？
LCEL（LangChain Expression Language）是LangChain的**声明式表达式语言**，通过 `|` 管道符将Runnable组件串联成调用链。

```python
# LCEL链：将提示模板、模型和输出解析器串联
chain = prompt | model | output_parser
result = chain.invoke({"topic": "AI医疗"})
```

本质上是 `RunnableSequence` 的语法糖，支持 `invoke`、`stream`、`batch` 等统一接口。

### 1.5 什么是Agent？Agent Executor的作用？
Agent是能**自主决策调用哪些工具**的LLM应用。核心流程（ReAct循环）：

```
用户输入 → LLM推理(思考需要什么工具) → 调用工具 → 获取结果 → LLM推理(决定下一步) → 输出最终答案
```

**Agent Executor** 是V1.2之前负责运行Agent循环的执行器。V1.3中已被 **LangGraph-based Agent** 取代，后者提供更精细的状态控制和中断恢复能力。

### 1.6 LangGraph与LangChain的关系？
LangGraph是LangChain团队推出的**有状态图编排框架**，用于构建多步骤、多智能体的LLM工作流。

| LangChain传统Agent | LangGraph Agent |
|-------------------|-----------------|
| 线性ReAct循环 | 图结构，支持分支和循环 |
| 无状态（每次调用独立） | 有状态，通过State管理 |
| 难以中断和恢复 | 支持Human-in-the-Loop中断 |
| 单Agent为主 | 原生支持多智能体协作 |

> 🎯 面试亮点：LangGraph的核心理念——将Agent逻辑建模为"节点（Node）+ 边（Edge）"的有向图，Node执行业务逻辑，Edge控制流转路径。

### 1.7 MCP协议是什么？
MCP（Model Context Protocol）是Anthropic推出的**模型上下文协议**，旨在标准化LLM与外部工具/数据源的交互方式。MCP定义了三种核心资源：
- **Tools**：可被LLM调用的函数
- **Resources**：可被LLM读取的静态数据
- **Prompts**：可复用的提示模板

LangChain V1.3原生支持通过 `MCPAdapter` 接入MCP服务器。

### 1.8 什么是RAG？LangChain中如何实现RAG？
RAG（Retrieval-Augmented Generation）即检索增强生成，流程如下：

```
用户问题 → 检索相关知识 → 将知识注入Prompt → LLM生成回答
```

LangChain中的标准RAG实现：

```python
# 基础RAG链路
from langchain_community.vectorstores import Chroma
from langchain.text_splitter import RecursiveCharacterTextSplitter
from langchain_openai import OpenAIEmbeddings, ChatOpenAI
from langchain.chains import create_retrieval_chain
from langchain.chains.combine_documents import create_stuff_documents_chain

# 1. 文档加载与分块
docs = loader.load()
splitter = RecursiveCharacterTextSplitter(chunk_size=500, chunk_overlap=50)
chunks = splitter.split_documents(docs)

# 2. 向量化与存储
vectorstore = Chroma.from_documents(chunks, OpenAIEmbeddings())

# 3. 构建RAG链
retriever = vectorstore.as_retriever()
llm = ChatOpenAI(model="gpt-4")
combine_docs_chain = create_stuff_documents_chain(llm, prompt)
rag_chain = create_retrieval_chain(retriever, combine_docs_chain)

result = rag_chain.invoke({"input": "什么是LangChain？"})
```

### 1.9 Embedding策略有哪些？
| 策略 | 特点 | 适用场景 |
|------|------|---------|
| OpenAI Embeddings | 1536维，质量高 | 通用场景 |
| BGE Embeddings | 多语言支持好 | 中文场景 |
| Sentence-Transformers | 本地部署，可微调 | 私有化部署 |
| Cohere Embeddings | 支持1000+维度 | 企业级应用 |
| 稀疏Embedding（SPLADE） | 关键词匹配强 | 专业术语多的场景 |

> ⚠️ 面试常问：Embedding模型选型需关注**维度、推理延迟、语义理解能力**，中文场景推荐BGE或m3e。

### 1.10 重排算法（Reranker）的作用？
Reranker在检索后进行**二次排序**，解决向量检索"语义相近但相关性不够"的问题：

```
向量检索(召回Top-50) → Reranker重排 → 取Top-5
```

常见方案：`Cohere Rerank`、`BGE-Reranker`、`Cross-Encoder`。

```python
from langchain.retrievers import ContextualCompressionRetriever
from langchain.retrievers.document_compressors import CrossEncoderReranker
from langchain_community.cross_encoders import HuggingFaceCrossEncoder

# 重排器集成
reranker = CrossEncoderReranker(
    model=HuggingFaceCrossEncoder(model_name="BAAI/bge-reranker-v2-m3"),
    top_n=5
)
compression_retriever = ContextualCompressionRetriever(
    base_compressor=reranker, base_retriever=retriever
)
```

### 1.11 什么是多向量检索器？
MultiVector Retriever为每个文档块维护多个向量表示（如摘要向量、子块向量、假设性问题向量），提高检索覆盖率。

```
文档 → 多个视角的向量 → 统一索引 → 检索时多路召回
```

在LangChain中通过 `MultiVectorRetriever` 实现，适合长文档或复杂内容的检索。

### 1.12 LangSmith的作用？
LangSmith是一个 **LLM应用全链路监控与调试平台**，提供：
- **追踪（Tracing）**：记录每一次LLM调用的输入输出、延迟、Token消耗
- **评估（Evaluation）**：自动化测试RAG/Agent质量
- **数据集管理**：构建和管理测试用例
- **反馈收集**：支持人工标注和用户反馈

```python
# 启用LangSmith追踪
import os
os.environ["LANGCHAIN_TRACING_V2"] = "true"
os.environ["LANGCHAIN_API_KEY"] = "ls_..."
```

### 1.13 什么是Human-in-the-Loop？
Human-in-the-Loop（人工介入）是Agent执行过程中的**人工审核/确认机制**。LangGraph通过 `interrupt` 节点实现：

```python
# LangGraph中的人工中断
def human_review(state):
    # Agent在此等待人工确认
    human_response = interrupt({"question": "请确认是否执行此操作？"})
    state["approved"] = human_response["approved"]
    return state
```

典型场景：医疗诊断建议需要医生确认后才能输出、金融交易需要风控审核。

### 1.14 向量数据库选型对比
| 数据库 | 部署方式 | 性能特点 | 适用场景 |
|--------|---------|---------|---------|
| FAISS | 内存/本地 | 单机最快 | 原型开发、小规模 |
| Chroma | 本地/嵌入式 | 轻量易用 | 快速上手 |
| Milvus | 分布式 | 十亿级规模 | 生产环境大规模 |
| Pinecone | 云托管 | 免运维 | 快速上线 |
| Weaviate | 混合部署 | 原生多模态 | 多媒体搜索 |
| Qdrant | 混合部署 | 过滤能力强 | 筛选条件复杂的场景 |

### 1.15 什么是Graph？StateGraph vs MessageGraph？
LangGraph中，Graph是**有状态的工作流图**，节点（Node）执行业务逻辑，边（Edge）控制流转。

| Graph类型 | 状态类型 | 适用场景 |
|-----------|---------|---------|
| **StateGraph** | 自定义状态字典 | 通用工作流，需要维护复杂状态 |
| **MessageGraph** | 消息列表 | 对话式流程，仅需维护消息列表 |

StateGraph更灵活，MessageGraph更简洁。V1.3推荐使用StateGraph。

### 1.16 Tavily搜索工具的作用？
Tavily是一个 **LLM优化的搜索引擎API**，专为AI应用设计：
- 返回结构化结果（标题、摘要、内容、来源）
- 低延迟（500ms内返回）
- 支持自定义搜索深度

LangChain中集成：`from langchain_community.tools.tavily_search import TavilySearchResults`。

### 1.17 LangChain中Chain和Runnable的关系？
V1.3中，**Runnable**是新架构的核心抽象接口，支持 `invoke/batch/stream`。传统**Chain**是Runnable的子类，但被标记为Legacy。V1.3推荐使用：
- `RunnableSequence`（LCEL管道链代替传统Chain）
- `RunnableParallel`（并行执行）
- `RunnablePassthrough`（透传数据）

### 1.18 什么是文档分割器（TextSplitter）？
LangChain提供多种文档分割策略：

| 分割器 | 分割依据 | 适用场景 |
|--------|---------|---------|
| RecursiveCharacterTextSplitter | 递归按分隔符分割 | 通用文本 |
| CharacterTextSplitter | 固定字符数 | 简单场景 |
| TokenTextSplitter | Token数 | 控制Token消耗 |
| MarkdownHeaderTextSplitter | Markdown标题 | 结构化文档 |
| SemanticChunker | 语义相似度 | 高质量分块 |

---

## 二、深度原理剖析（10-15题）

### 2.1 LangChain V1.3架构深度解析

**三层架构**：

```
应用层：Agent / RAG / Chain / 自定义应用
            ↓
核心层：Runnable协议 + LCEL + Callbacks
            ↓
模型层：LLM / Embedding / VectorStore / Tool
```

**Runnable协议**是LangChain V1.3最核心的抽象，所有组件实现五个标准方法：

| 方法 | 作用 | 支持LCEL |
|------|------|---------|
| `invoke(input)` | 同步调用 | 是 |
| `ainvoke(input)` | 异步调用 | 是 |
| `batch(inputs)` | 批量调用 | 是 |
| `stream(input)` | 流式输出 | 是 |
| `astream(input)` | 异步流式 | 是 |

**LCEL设计模式**：通过 `|` 操作符构建RunnableSequence，本质上是**管道过滤器模式**（Pipes and Filters），每个节点实现Runnable接口，数据从上游流向下游逐层处理。

```python
# LCEL底层等价于
from langchain_core.runnables import RunnableSequence

chain = RunnableSequence(first=prompt, middle=[model], last=output_parser)
# 等价于
chain = prompt | model | output_parser
```

**组件化思想**：每个组件（提示模板、模型、输出解析器、检索器）都是独立的Runnable，可以任意排列组合，极大提高了代码复用性。

> 🎯 面试亮点：LCEL的流式支持是V1.3的重要改进，每个Runnable节点都必须实现 `transform()` 方法，使得整个管道天然支持流式输出。

### 2.2 Agent执行机制：ReAct循环与Tool Calling

**ReAct循环**（Reasoning + Acting）的完整流程：

```
1. 用户输入问题
2. Agent将问题、可用工具列表、历史记录组装成Prompt
3. LLM推理 → 输出Action或Final Answer
4. 如果是Action：Agent Executor调用对应Tool，获取Observation
5. 将Observation追加到上下文，回到步骤3
6. 如果是Final Answer：返回给用户
```

**Tool Calling机制**（V1.3新特性）：

```python
# V1.3中使用Tool Calling（绑定工具到LLM）
from langchain_openai import ChatOpenAI
from langchain_core.tools import tool

@tool
def search_medical_info(query: str) -> str:
    """搜索医疗相关信息"""
    return tavily_search(query)

llm = ChatOpenAI(model="gpt-4", temperature=0)
llm_with_tools = llm.bind_tools([search_medical_info])
```

V1.3的Tool Calling相比V1.2的 `AgentExecutor` 更轻量、更稳定，直接利用LLM的function calling能力，不需要额外的Agent提示工程。

**Memory集成**：

```python
from langchain.memory import ConversationBufferWindowMemory
from langchain.agents import AgentExecutor, create_tool_calling_agent

memory = ConversationBufferWindowMemory(k=5, memory_key="chat_history")
agent = create_tool_calling_agent(llm, tools, prompt)
agent_executor = AgentExecutor(agent=agent, tools=tools, memory=memory)
```

### 2.3 RAG全链路优化详解

RAG的完整优化链路分为六个环节：

```
Chunking → Embedding → Indexing → Retrieval → Reranking → Generation
```

**1. Chunking（分块策略）**：
| 策略 | 参数 | 效果 |
|------|------|------|
| 固定大小分块 | chunk_size=500, overlap=50 | 简单通用 |
| 语义分块 | SemanticChunker | 保语义完整性 |
| Agent辅助分块 | LLM自动识别边界 | 精准但慢 |
| 分层分块 | ParentDocumentRetriever | 兼顾精确与上下文 |

**2. Embedding（向量化）**：
- 多字段Embedding：同时编码标题、摘要、正文
- 混合Embedding：稠密向量 + 稀疏向量（如SPLADE）
- 领域微调：用领域数据微调Embedding模型

**3. Indexing（索引构建）**：
- 分层索引：摘要索引 → 块索引
- 元数据过滤：过滤条件（时间、来源、类型）降低搜索空间
- 多路索引：不同分块策略建立多个索引

**4. Retrieval（检索策略）**：
- Multi-Query：LLM生成多个相关问题，分别检索后去重
- Self-Query：LLM从问题中提取过滤条件
- Ensemble Retrieval：多路召回（向量 + BM25 + SQL）
- Contextual Compression：压缩检索结果，去除冗余

**5. Reranking（重排）**：使用Cross-Encoder对检索结果二次排序

**6. Generation（生成优化）**：
- 动态上下文窗口：根据Token限制动态调整
- Prompt压缩：去除检索结果中不相关内容
- 引用溯源：生成结果附带引用来源

```python
# 完整RAG优化链路
from langchain.retrievers import (
    ContextualCompressionRetriever,
    MultiQueryRetriever,
)
from langchain.retrievers.document_compressors import CrossEncoderReranker
from langchain.retrievers.ensemble import EnsembleRetriever

# 1. Multi-Query + Reranker + 混合检索
retriever_multi = MultiQueryRetriever.from_llm(retriever=base_retriever, llm=llm)
bm25_retriever = BM25Retriever.from_documents(docs)

# 2. 混合检索
ensemble_retriever = EnsembleRetriever(
    retrievers=[retriever_multi, bm25_retriever],
    weights=[0.7, 0.3]
)

# 3. Reranker重排
reranker = CrossEncoderReranker(
    model=HuggingFaceCrossEncoder(model_name="BAAI/bge-reranker-v2-m3"),
    top_n=5
)
final_retriever = ContextualCompressionRetriever(
    base_compressor=reranker,
    base_retriever=ensemble_retriever
)
```

### 2.4 LangGraph图执行机制

LangGraph的核心概念：

| 概念 | 说明 | 示例 |
|------|------|------|
| **State** | 全局共享状态，在节点间传递 | `dict`或`TypedDict` |
| **Node** | 处理逻辑单元，接收并更新State | `def node(state) -> state` |
| **Edge** | 连接节点的有向边 | `graph.add_edge(node_a, node_b)` |
| **Conditional Edge** | 条件路由边 | 根据State中的字段决定走向 |
| **Checkpoint** | 状态检查点，支持中断和恢复 | 自动保存每个Step后的State |

执行流程：

```
调用graph.invoke(input_state)
    → 从START节点开始
    → 执行当前节点（接收state，处理后返回新的state）
    → 根据Conditional Edge判断下一个节点
    → 重复直到到达END节点
    → 返回最终state
```

```python
from typing import TypedDict, Literal
from langgraph.graph import StateGraph, END

# 定义状态
class AgentState(TypedDict):
    messages: list
    next_agent: str

# 定义节点
def agent_a(state: AgentState) -> AgentState:
    state["messages"].append("A处理完毕")
    state["next_agent"] = "B"
    return state

def agent_b(state: AgentState) -> AgentState:
    state["messages"].append("B处理完毕")
    return state

# 条件边
def router(state: AgentState) -> Literal["agent_b", END]:
    return "agent_b" if state["next_agent"] == "B" else END

# 构建图
graph = StateGraph(AgentState)
graph.add_node("agent_a", agent_a)
graph.add_node("agent_b", agent_b)
graph.set_entry_point("agent_a")
graph.add_conditional_edges("agent_a", router)
graph.add_edge("agent_b", END)

app = graph.compile()
```

### 2.5 MCP协议的设计思想与LangChain集成

**MCP协议设计思想**：
- **标准化接口**：统一LLM调用外部工具的协议
- **服务端/客户端架构**：MCP Server提供工具资源，MCP Client消费
- **资源抽象**：将API、数据库、文件系统等抽象为统一资源

**LangChain集成方式**：

```python
# LangChain V1.3接入MCP Server
from langchain_mcp import MCPAdapter
from langchain.agents import create_tool_calling_agent

# MCP客户端连接
mcp_client = MCPAdapter.connect("http://localhost:8000/mcp")
mcp_tools = mcp_client.get_tools()

# 将MCP工具集成到Agent
agent = create_tool_calling_agent(llm, mcp_tools, prompt)
```

### 2.6 检索器进阶：Self-Query / Multi-Query / Contextual Compression

**Self-Query Retriever**：自动从自然语言问题中提取查询条件和过滤参数：
```python
from langchain.retrievers.self_query import SelfQueryRetriever

# 自查询检索器：从问题提取查询条件和过滤条件
self_query_retriever = SelfQueryRetriever.from_llm(
    llm=llm,
    vectorstore=vectorstore,
    document_contents="医疗知识库文档",
    metadata_field_info=[
        {"name": "department", "type": "string", "description": "科室名称"},
        {"name": "year", "type": "int", "description": "发布年份"},
    ]
)
```

**Multi-Query Retriever**：LLM生成多个角度的问题，分别检索后去重合并。

**Contextual Compression Retriever**：对检索结果进行压缩（摘要抽取、段落过滤），保留最相关内容。

### 2.7 LLM调用优化：Streaming / Batching / Caching / Fallback

```python
from langchain_core.runnables import RunnablePassthrough, RunnableLambda
from langchain_community.cache import InMemoryCache
from langchain.globals import set_llm_cache

# 1. 缓存优化
set_llm_cache(InMemoryCache())

# 2. Fallback机制（主模型降级）
from langchain_openai import ChatOpenAI

primary_llm = ChatOpenAI(model="gpt-4", timeout=30)
fallback_llm = ChatOpenAI(model="gpt-3.5-turbo", timeout=30)

chain_with_fallback = primary_llm.with_fallbacks([fallback_llm])

# 3. 流式输出
async for chunk in chain.astream({"input": "请详细介绍"}):
    print(chunk.content, end="", flush=True)

# 4. 批量处理
results = chain.batch([{"input": q} for q in questions])
```

### 2.8 Human-in-the-Loop设计模式

在LangGraph中实现人工审核的三种模式：

**模式一：前置审核**（执行前确认）
```python
# Agent执行关键操作前暂停，等人工确认
graph = graph_builder.compile(checkpointer=MemorySaver())
config = {"configurable": {"thread_id": "1"}}

# 第一次调用：Agent执行到需要确认的步骤时暂停
result = graph.invoke({"input": "给患者开药"}, config)

# 人工审核后继续
human_approval = input("是否同意此操作？(y/n): ")
if human_approval == "y":
    result = graph.invoke(None, config)
```

**模式二：后置审核**（执行后审核）
模式三：流式审核（逐个步骤审核）

---

## 三、实战场景题（8-12题）

### 3.1 LangChain构建RAG应用（完整流程）

```python
import os
from langchain_community.document_loaders import TextLoader, DirectoryLoader
from langchain.text_splitter import RecursiveCharacterTextSplitter
from langchain_openai import OpenAIEmbeddings, ChatOpenAI
from langchain_community.vectorstores import Chroma
from langchain.chains import create_retrieval_chain
from langchain.chains.combine_documents import create_stuff_documents_chain
from langchain_core.prompts import ChatPromptTemplate

# 1. 文档加载
loader = DirectoryLoader("./docs/", glob="**/*.txt", loader_cls=TextLoader)
docs = loader.load()

# 2. 文档分块
text_splitter = RecursiveCharacterTextSplitter(
    chunk_size=500,
    chunk_overlap=50,
    separators=["\n\n", "\n", "。", "！", "？", " ", ""]
)
chunks = text_splitter.split_documents(docs)

# 3. 向量化与索引
embeddings = OpenAIEmbeddings(model="text-embedding-3-small")
vectorstore = Chroma.from_documents(chunks, embeddings, persist_directory="./chroma_db")

# 4. 构建RAG链
llm = ChatOpenAI(model="gpt-4", temperature=0)
prompt = ChatPromptTemplate.from_template("""
你是一个专业的问答助手，请根据已知的上下文信息回答用户的问题。
如果无法从上下文中找到答案，请诚实地说"不知道"，不要编造。

上下文：{context}

问题：{input}
""")

combine_docs_chain = create_stuff_documents_chain(llm, prompt)
retrieval_chain = create_retrieval_chain(vectorstore.as_retriever(search_kwargs={"k": 5}), combine_docs_chain)

# 5. 执行查询
result = retrieval_chain.invoke({"input": "LangChain是什么框架？"})
print(f"答案：{result['answer']}")
```

### 3.2 Agent + Tool调用实现

```python
from langchain_openai import ChatOpenAI
from langchain_core.tools import tool
from langchain.agents import create_tool_calling_agent, AgentExecutor
from langchain_core.prompts import ChatPromptTemplate
import requests

# 定义工具
@tool
def search_patient_info(patient_id: str) -> str:
    """根据患者ID查询患者基本信息（姓名、年龄、科室）"""
    # 模拟返回
    return f"患者{patient_id}：张三，45岁，心内科"

@tool
def check_drug_interaction(drug_a: str, drug_b: str) -> str:
    """检查两种药物是否存在相互作用"""
    # 实际场景中调用药品知识库API
    return f"{drug_a}与{drug_b}存在中度相互作用，建议间隔2小时服用"

@tool
def get_weather(city: str) -> str:
    """查询指定城市的天气"""
    return f"{city}今日天气：晴，25-30℃"

# 构建Agent
llm = ChatOpenAI(model="gpt-4", temperature=0)
tools = [search_patient_info, check_drug_interaction, get_weather]

prompt = ChatPromptTemplate.from_messages([
    ("system", "你是医疗辅助助手，可以查询患者信息和药物相互作用。请使用工具准确回答问题。"),
    ("human", "{input}"),
    ("placeholder", "{agent_scratchpad}"),
])

agent = create_tool_calling_agent(llm, tools, prompt)
agent_executor = AgentExecutor(agent=agent, tools=tools, verbose=True)

# 执行
agent_executor.invoke({"input": "查询患者P001的信息，并检查他正在服用的阿司匹林和氯吡格雷是否有相互作用？"})
```

### 3.3 LangGraph构建多智能体工作流

```python
from typing import TypedDict, Literal, Annotated
from langgraph.graph import StateGraph, END, add_messages
from langchain_openai import ChatOpenAI
from langchain_core.messages import HumanMessage, AIMessage

# 定义状态
class MedicalState(TypedDict):
    messages: Annotated[list, add_messages]
    patient_info: str
    diagnosis: str
    final_report: str

llm = ChatOpenAI(model="gpt-4", temperature=0)

# 节点1：分诊Agent
def triage_agent(state: MedicalState) -> MedicalState:
    messages = state["messages"]
    response = llm.invoke([
        HumanMessage(content=f"根据患者描述，判断需要哪个科室处理：{messages[-1].content}")
    ])
    state["messages"].append(AIMessage(content=response.content))
    state["diagnosis"] = response.content
    return state

# 节点2：专科Agent
def specialist_agent(state: MedicalState) -> MedicalState:
    diagnosis = state["diagnosis"]
    response = llm.invoke([
        HumanMessage(content=f"基于分诊结果，给出详细诊疗建议：{diagnosis}")
    ])
    state["messages"].append(AIMessage(content=response.content))
    return state

# 节点3：报告整合Agent
def report_agent(state: MedicalState) -> MedicalState:
    state["final_report"] = "【综合诊断报告】\n" + "\n".join([m.content for m in state["messages"]])
    return state

# 条件路由
def triage_router(state: MedicalState) -> Literal["specialist", END]:
    content = state["messages"][-1].content
    if "急诊" in content or "危重" in content:
        return END  # 紧急情况直接结束
    return "specialist"

# 构建多智能体图
builder = StateGraph(MedicalState)
builder.add_node("triage", triage_agent)
builder.add_node("specialist", specialist_agent)
builder.add_node("report", report_agent)

builder.set_entry_point("triage")
builder.add_conditional_edges("triage", triage_router)
builder.add_edge("specialist", "report")
builder.add_edge("report", END)

app = builder.compile()
```

### 3.4 MCP服务搭建与LangChain集成

```python
# MCP Server端（使用FastMCP）
from mcp.server.fastmcp import FastMCP

mcp = FastMCP("医疗知识库MCP")

@mcp.tool()
def query_medication(drug_name: str) -> str:
    """查询药品信息"""
    return f"【{drug_name}】适应症：高血压，用量：10mg/次"

@mcp.resource("file:///hospital/policies.md")
def get_policy() -> str:
    """医院政策文档"""
    return "本院实行实名制挂号..."

mcp.run(port=8000)
```

```python
# LangChain Client端
from langchain_mcp import MCPAdapter
from langchain.agents import create_tool_calling_agent, AgentExecutor

mcp_client = MCPAdapter.connect("http://localhost:8000/mcp")
mcp_tools = mcp_client.get_tools()

llm = ChatOpenAI(model="gpt-4")
agent = create_tool_calling_agent(llm, mcp_tools, prompt)
executor = AgentExecutor(agent=agent, tools=mcp_tools)

executor.invoke({"input": "查询阿莫西林的药品信息"})
```

### 3.5 RAG全链路优化（分块+Embedding+Reranker）

```python
from langchain_text_splitters import (
    RecursiveCharacterTextSplitter,
    MarkdownHeaderTextSplitter,
)
from langchain.embeddings import CacheBackedEmbeddings
from langchain.storage import LocalFileStore
from langchain.retrievers import ContextualCompressionRetriever
from langchain.retrievers.document_compressors import CrossEncoderReranker
from langchain_community.cross_encoders import HuggingFaceCrossEncoder
from langchain_community.vectorstores import FAISS

# 1. 分层分块策略
markdown_splitter = MarkdownHeaderTextSplitter(
    headers_to_split_on=[("#", "H1"), ("##", "H2"), ("###", "H3")]
)
char_splitter = RecursiveCharacterTextSplitter(chunk_size=400, chunk_overlap=50)

# 2. 带缓存的Embedding
underlying_embeddings = OpenAIEmbeddings(model="text-embedding-3-small")
store = LocalFileStore("./cache/")
cached_embeddings = CacheBackedEmbeddings.from_bytes_store(
    underlying_embeddings, store, namespace="medical"
)

# 3. Reranker重排
reranker = CrossEncoderReranker(
    model=HuggingFaceCrossEncoder(model_name="BAAI/bge-reranker-v2-m3"),
    top_n=5
)
compression_retriever = ContextualCompressionRetriever(
    base_compressor=reranker,
    base_retriever=FAISS.from_documents(chunks, cached_embeddings).as_retriever(
        search_kwargs={"k": 20}
    )
)
```

### 3.6 医疗行业Agent构建（含人类监督）

```python
from typing import TypedDict
from langgraph.graph import StateGraph, END
from langgraph.checkpoint.memory import MemorySaver

class MedicalAgentState(TypedDict):
    messages: list
    diagnosis: str
    prescription: str
    approved: bool

# 自动诊断节点
def auto_diagnose(state: MedicalAgentState) -> MedicalAgentState:
    symptom = state["messages"][-1].content
    diagnosis = llm.invoke(f"根据症状'{symptom}'给出初步诊断和建议用药")
    state["diagnosis"] = diagnosis.content
    state["messages"].append(diagnosis)
    return state

# 人工审核节点（Human-in-the-Loop）
def human_review(state: MedicalAgentState) -> MedicalAgentState:
    # 此处Agent暂停等待人工审核
    from langgraph.types import interrupt
    approval = interrupt({
        "diagnosis": state["diagnosis"],
        "question": "请医生确认诊断和用药方案是否合理？"
    })
    state["approved"] = approval.get("approved", False)
    return state

# 路由判断
def after_review(state: MedicalAgentState):
    if state["approved"]:
        return "generate_report"
    return "auto_diagnose"  # 重新诊断

# 构建带监督的医疗Agent
builder = StateGraph(MedicalAgentState)
builder.add_node("diagnose", auto_diagnose)
builder.add_node("review", human_review)
builder.add_node("report", generate_report)

builder.set_entry_point("diagnose")
builder.add_edge("diagnose", "review")
builder.add_conditional_edges("review", after_review)
builder.add_edge("report", END)

app = builder.compile(checkpointer=MemorySaver())
```

### 3.7 上下文窗口优化实现

```python
from langchain.text_splitter import TokenTextSplitter
from langchain_community.chat_models import ChatOpenAI
from langchain.memory import ConversationSummaryBufferMemory
from langchain_core.messages import SystemMessage

# 策略一：对话摘要压缩
memory = ConversationSummaryBufferMemory(
    llm=ChatOpenAI(model="gpt-3.5-turbo"),
    max_token_limit=2000,
    memory_key="chat_history",
    return_messages=True
)

# 策略二：滑动窗口
from langchain.memory import ConversationBufferWindowMemory
window_memory = ConversationBufferWindowMemory(k=5)

# 策略三：检索摘要压缩（RAG场景）
def compress_context(docs, max_tokens=3000):
    """动态压缩检索上下文，超出Token限制时截断"""
    token_splitter = TokenTextSplitter(chunk_size=max_tokens, chunk_overlap=0)
    # 按相关性排序后截断
    sorted_docs = sorted(docs, key=lambda d: d.metadata.get("relevance_score", 0), reverse=True)
    return token_splitter.split_documents(sorted_docs)
```

### 3.8 RAG评估指标体系搭建

```python
from langsmith import Client
from langsmith.evaluation import evaluate
from langchain_openai import ChatOpenAI

# 定义评估指标
def faithfulness(run, example):
    """忠实度：生成内容是否来源于检索文档"""
    outputs = run.outputs
    inputs = example.inputs
    llm = ChatOpenAI(model="gpt-4")
    response = llm.invoke(f"""
    判断以下答案是否完全基于给定上下文，没有编造信息。
    答案：{outputs['answer']}
    上下文：{inputs['context']}
    请回答：是/否，并说明原因。
    """)
    return {"score": 1 if "是" in response.content else 0}

# RAG评估指标体系
RAG_EVAL_METRICS = {
    "忠实度(Faithfulness)": "生成内容是否忠实于检索文档",
    "答案相关性(Answer Relevancy)": "答案是否直接回应问题",
    "上下文精度(Context Precision)": "检索结果中相关文档的比例",
    "上下文召回(Context Recall)": "相关文档被检索到的比例",
    "幻觉率(Hallucination Rate)": "生成内容中无依据信息的比例",
    "端到端延迟(End-to-end Latency)": "从输入到输出的总耗时",
}

# 自动化评估
def evaluate_rag_system(rag_chain, test_dataset):
    for example in test_dataset:
        result = rag_chain.invoke({"input": example["question"]})
        metrics = {
            "faithfulness": faithfulness(result, example),
            "answer_relevancy": compute_relevancy(result["answer"], example["question"]),
            "context_precision": compute_context_precision(result["context"], example["golden_docs"]),
            "latency": result.get("latency_ms", 0),
        }
        print(f"Question: {example['question']}, Metrics: {metrics}")
```

---

## 四、手写代码题（5-8题）

### 4.1 手写LCEL链（RunnableSequence）

```python
from langchain_core.runnables import RunnablePassthrough, RunnableParallel
from langchain_core.output_parsers import StrOutputParser
from langchain_openai import ChatOpenAI
from langchain_core.prompts import ChatPromptTemplate

# 带并行分支的LCEL链
prompt = ChatPromptTemplate.from_template("用{style}的风格写一段关于{topic}的介绍")
model = ChatOpenAI(model="gpt-4")

# 方式一：管道符
chain = prompt | model | StrOutputParser()

# 方式二：显式构建RunnableSequence（等价）
from langchain_core.runnables import RunnableSequence
chain2 = RunnableSequence(first=prompt, middle=[model], last=StrOutputParser())

# 并行分支
parallel_chain = RunnableParallel(
    formal=prompt | model | StrOutputParser(),  # 正式风格
    casual=prompt | model | StrOutputParser(),  # 口语风格
)

result = parallel_chain.invoke({"topic": "AI医疗", "style": "技术"})
```

### 4.2 手写自定义Retriever

```python
from langchain_core.retrievers import BaseRetriever
from langchain_core.documents import Document
from typing import List

class MedicalKnowledgeRetriever(BaseRetriever):
    """自定义医疗知识检索器：支持多维度检索"""
    vectorstore: object  # 向量数据库
    bm25_index: object   # BM25索引
    department_filter: str = ""  # 科室过滤

    def _get_relevant_documents(self, query: str) -> List[Document]:
        # 1. 向量检索
        vector_results = self.vectorstore.similarity_search(
            query, k=10, filter={"department": self.department_filter}
        )

        # 2. 关键词检索（BM25补充）
        keyword_results = self.bm25_index.search(query, k=5)

        # 3. 合并去重
        seen_ids = set()
        merged = []
        for doc in vector_results + keyword_results:
            if doc.id not in seen_ids:
                seen_ids.add(doc.id)
                merged.append(doc)

        # 4. 按相关性排序
        merged.sort(key=lambda d: d.metadata.get("score", 0), reverse=True)
        return merged[:10]

    async def _aget_relevant_documents(self, query: str) -> List[Document]:
        return self._get_relevant_documents(query)
```

### 4.3 手写Agent工具定义

```python
from langchain_core.tools import tool
from typing import List, Dict
import json

# 方式一：装饰器定义
@tool
def calculate_bmi(weight: float, height: float) -> str:
    """计算BMI指数（体重kg / 身高m的平方）
    Args:
        weight: 体重（公斤）
        height: 身高（米）
    Returns:
        BMI指数和健康建议
    """
    bmi = weight / (height ** 2)
    if bmi < 18.5:
        advice = "偏瘦，建议增加营养摄入"
    elif bmi < 24:
        advice = "正常范围"
    elif bmi < 28:
        advice = "偏胖，建议控制饮食和运动"
    else:
        advice = "肥胖，建议就医"
    return json.dumps({"bmi": round(bmi, 1), "advice": advice}, ensure_ascii=False)

# 方式二：BaseTool子类
from langchain_core.tools import BaseTool
from pydantic import BaseModel, Field

class DrugQueryInput(BaseModel):
    drug_name: str = Field(description="药品名称")
    specification: str = Field(default="", description="规格")

class DrugQueryTool(BaseTool):
    name: str = "query_drug_info"
    description: str = "查询药品详细信息，包括适应症、用法用量、禁忌"
    args_schema: type = DrugQueryInput

    def _run(self, drug_name: str, specification: str = "") -> str:
        # 调用药品数据库API
        return f"【{drug_name}】适应症：高血压，禁忌：孕妇禁用，规格：{specification or '10mg'}"

# 工具列表
tools: List[BaseTool] = [calculate_bmi, DrugQueryTool()]
```

### 4.4 手写LangGraph StateGraph

```python
from typing import TypedDict, Literal, Annotated
from langgraph.graph import StateGraph, END
from langgraph.checkpoint.memory import MemorySaver
from langchain_core.messages import AnyMessage
from langgraph.graph.message import add_messages

# 定义带Message的State
class AgentState(TypedDict):
    messages: Annotated[list[AnyMessage], add_messages]
    next_step: str
    final_output: str

# 节点函数
def input_node(state: AgentState) -> AgentState:
    """处理用户输入"""
    return state

def thinking_node(state: AgentState) -> AgentState:
    """LLM推理节点"""
    from langchain_openai import ChatOpenAI
    llm = ChatOpenAI(model="gpt-4")
    response = llm.invoke(state["messages"])
    state["messages"].append(response)
    state["next_step"] = "tool_call" if "需要工具" in response.content else "output"
    return state

def tool_node(state: AgentState) -> AgentState:
    """工具调用节点"""
    # 模拟工具调用
    state["messages"].append({"role": "tool", "content": "工具返回结果"})
    state["next_step"] = "output"
    return state

def output_node(state: AgentState) -> AgentState:
    """输出节点"""
    last_msg = state["messages"][-1]
    state["final_output"] = last_msg.content if hasattr(last_msg, "content") else str(last_msg)
    return state

# 条件路由
def router(state: AgentState) -> Literal["thinking", "tool", "output"]:
    return state["next_step"]

# 构建图
builder = StateGraph(AgentState)
builder.add_node("input", input_node)
builder.add_node("thinking", thinking_node)
builder.add_node("tool", tool_node)
builder.add_node("output", output_node)

builder.set_entry_point("input")
builder.add_edge("input", "thinking")
builder.add_conditional_edges(
    "thinking",
    lambda s: s["next_step"],
    {"tool_call": "tool", "output": "output"}
)
builder.add_edge("tool", "thinking")
builder.add_edge("output", END)

app = builder.compile(checkpointer=MemorySaver())
```

### 4.5 手写自定义Callback Handler

```python
from langchain_core.callbacks import BaseCallbackHandler
from typing import Any, Dict, List

class MetricCallbackHandler(BaseCallbackHandler):
    """自定义监控回调：收集延迟、Token消耗等指标"""

    def __init__(self):
        self.starts: Dict[str, float] = {}
        self.metrics: List[Dict] = []

    def on_llm_start(self, serialized: Dict[str, Any], prompts: List[str], **kwargs) -> None:
        """LLM开始调用时记录时间"""
        run_id = kwargs.get("run_id")
        self.starts[str(run_id)] = __import__("time").time()

    def on_llm_end(self, response, **kwargs) -> None:
        """LLM结束时计算延迟"""
        run_id = kwargs.get("run_id")
        start = self.starts.pop(str(run_id), None)
        if start:
            latency = __import__("time").time() - start
            self.metrics.append({
                "event": "llm_call",
                "latency": round(latency, 3),
                "tokens": response.llm_output.get("token_usage", {}) if response.llm_output else {},
            })

    def on_tool_start(self, serialized: Dict[str, Any], input_str: str, **kwargs) -> None:
        run_id = kwargs.get("run_id")
        self.starts[str(run_id)] = __import__("time").time()

    def on_tool_end(self, output: str, **kwargs) -> None:
        run_id = kwargs.get("run_id")
        start = self.starts.pop(str(run_id), None)
        if start:
            latency = __import__("time").time() - start
            self.metrics.append({
                "event": "tool_call",
                "latency": round(latency, 3),
                "output_length": len(output),
            })

# 使用
callback = MetricCallbackHandler()
chain.invoke({"input": "查询信息"}, config={"callbacks": [callback]})
print(callback.metrics)
```

### 4.6 手写TextSplitter（RecursiveCharacter）

```python
from langchain_text_splitters import TextSplitter
from typing import List

class RecursiveCharacterTextSplitter(TextSplitter):
    """手写递归字符文本分割器"""

    def __init__(self, chunk_size: int = 400, chunk_overlap: int = 50,
                 separators: List[str] = None):
        super().__init__(chunk_size=chunk_size, chunk_overlap=chunk_overlap)
        self.separators = separators or ["\n\n", "\n", "。", "！", "？", " ", ""]

    def split_text(self, text: str) -> List[str]:
        chunks = []
        self._split_text_recursive(text, self.separators, chunks)
        return chunks

    def _split_text_recursive(self, text: str, separators: List[str], chunks: List[str]):
        """递归分割，优先使用高级分隔符"""
        if len(text) <= self._chunk_size:
            chunks.append(text)
            return

        if not separators:
            # 无更多分隔符，按字符截断
            chunks.append(text[:self._chunk_size])
            remaining = text[self._chunk_size - self._chunk_overlap:]
            if remaining:
                self._split_text_recursive(remaining, separators, chunks)
            return

        separator = separators[0]
        if separator:
            parts = text.split(separator)
        else:
            parts = list(text)

        current_chunk = ""
        for part in parts:
            candidate = current_chunk + (separator if current_chunk else "") + part
            if len(candidate) <= self._chunk_size:
                current_chunk = candidate
            else:
                if current_chunk:
                    chunks.append(current_chunk)
                # 检查part是否需要继续分割
                if len(part) > self._chunk_size:
                    self._split_text_recursive(part, separators[1:], chunks)
                else:
                    current_chunk = part
                # 维护overlap
                overlap_parts = current_chunk[-self._chunk_overlap:] if current_chunk else ""
                current_chunk = overlap_parts + part if overlap_parts else part

        if current_chunk:
            chunks.append(current_chunk)
```

---

## 五、系统设计题（3-5题）

### 5.1 设计生产级RAG系统

**需求**：构建一个支撑医疗知识库问答的生产级RAG系统

**架构设计**：

```
┌─────────────────────────────────────────────┐
│                 应用层                        │
│  REST API / WebSocket / 流式输出             │
├─────────────────────────────────────────────┤
│                 RAG编排层                    │
│  查询改写 → 多路召回 → Reranker → 生成      │
│  策略：Query Rewrite + Multi-Query + Ensemble│
├──────────────┬──────────────┬───────────────┤
│   索引层      │   缓存层     │   监控层       │
│  Chroma/Milvus│ Redis Cache │ LangSmith     │
│  ES(全文检索) │ 结果缓存    │ 指标告警      │
├──────────────┴──────────────┴───────────────┤
│                模型层                        │
│  Embedding: BGE-M3 / OpenAI                 │
│  LLM: GPT-4 / 本地模型                      │
│  Reranker: BGE-Reranker                     │
├─────────────────────────────────────────────┤
│               数据管道层                      │
│  文档解析 → 分块 → Embedding → 索引构建     │
│  增量更新 / 全量重建 / 数据版本管理          │
└─────────────────────────────────────────────┘
```

**核心设计要点**：
1. **多路召回**：向量检索 + BM25全文检索 + SQL结构化查询
2. **查询改写**：LLM将模糊提问改写为规范查询
3. **异步流水线**：使用Celery/Ray异步处理索引构建
4. **缓存策略**：相同问题缓存结果，降低LLM调用成本
5. **评估体系**：Faithfulness、Relevancy、Precision、Recall、Latency

### 5.2 设计多Agent协作系统（医疗诊断场景）

**场景**：多科室协作诊断系统

**架构设计**：

```python
# 多Agent拓扑
class MedicalMultiAgentSystem:
    """
    Agent拓扑：分诊Agent → 科室Agent → 会诊Agent → 审核Agent

    - 分诊Agent：判断科室，处理紧急转诊
    - 科室Agent：心内科/呼吸科/消化科等专科诊断
    - 会诊Agent：跨科室汇总，生成综合报告
    - 审核Agent：主任医师（Human-in-the-Loop）最终确认
    """
    def design_workflow(self):
        return """
        1. 用户输入症状
        2. 分诊Agent → 识别科室，提取关键信息
        3. 科室Agent → 专科诊断，推荐检查方案
        4. 检查结果分析Agent → 解读化验单/影像报告
        5. 会诊Agent → 汇总各科室意见，生成综合方案
        6. 审核Agent → 人工审核（Human-in-the-Loop）
        7. 输出最终诊断报告
        """
```

**关键设计要素**：
- **Agent间通信**：通过共享State传递信息
- **异常处理**：某个Agent超时或失败时的降级策略
- **权限控制**：不同Agent访问不同数据源
- **审计追踪**：所有Agent决策记录到LangSmith

### 5.3 设计RAG评估监控平台

**评估维度**：

| 维度 | 指标 | 阈值 | 告警策略 |
|------|------|------|---------|
| 检索质量 | Recall@5, Precision@5 | Recall > 0.8 | 低于阈值触发 |
| 生成质量 | Faithfulness, Relevancy | Faithfulness > 0.9 | 连续3条低于阈值 |
| 性能 | p50/p99延迟 | p99 < 3s | 超时告警 |
| 成本 | 每日Token消耗 | 预算范围内 | 超出80%预警 |
| 用户反馈 | 点赞/点踩比例 | 满意率 > 85% | 周报分析 |

**技术方案**：
- 使用LangSmith作为Tracing基础设施
- 构建自动化测试流水线（CI/CD中集成RAG评估）
- 使用LLM-as-Judge自动评分
- 定期人工抽样评估，校准自动评估结果

### 5.4 设计MCP服务生态系统

**架构**：

```
┌────────────┐    ┌────────────┐    ┌────────────┐
│ MCP Client │───▶│ MCP Gateway│───▶│ MCP Server │
│ (LangChain)│    │ (路由/认证) │    │ (医疗知识库)│
└────────────┘    └────────────┘    └────────────┘
                         │
              ┌──────────┼──────────┐
              ▼          ▼          ▼
        ┌─────────┐ ┌─────────┐ ┌─────────┐
        │ 药品MCP  │ │ 病历MCP │ │ 检查MCP  │
        └─────────┘ └─────────┘ └─────────┘
```

**设计要点**：
1. **MCP Gateway**：统一入口，负责路由、认证、限流
2. **服务发现**：MCP Server动态注册
3. **协议版本管理**：兼容不同MCP版本
4. **安全策略**：OAuth2/JWT认证 + 操作审计

---

## 六、常见坑点与最佳实践（表格）

| 坑点 | 问题描述 | 解决方案 | 最佳实践 |
|------|---------|---------|---------|
| Token超限 | 检索结果过多导致上下文窗口溢出 | 使用 `ContextualCompressionRetriever` 压缩，或设置 `max_tokens` 截断 | 始终评估最大Token消耗，设置熔断机制 |
| 幻觉问题 | LLM编造不存在的信息 | RAG忠实度校验 + 引用溯源 + 未知答案时明确说"不知道" | 在Prompt中强制要求引用来源，输出格式结构化 |
| Agent循环 | Agent陷入无限调用循环 | 设置 `max_iterations` 和 `early_stopping_method` | V1.3推荐使用LangGraph代替传统Agent Executor |
| Embedding模型与LLM不匹配 | 检索到的内容LLM无法理解 | 确保Embedding和LLM使用相同或相近的Tokenizer | 同一模型家族选型（如OpenAI全家桶）|
| 分块边界切断语义 | 文档分割断开完整句子 | 使用Overlap + 语义分块器（SemanticChunker） | 优先以自然段落为边界，overlap设为chunk_size的10% |
| 向量数据库选择错误 | 规模预估不足导致性能瓶颈 | 根据数据量选型：<10万用FAISS，<1000万用Milvus | 生产环境先用Milvus或Qdrant，避免后期迁移成本 |
| LangSmith Token泄露 | 生产环境误开启Tracing暴露API Key | 使用 `LANGCHAIN_API_KEY` 环境变量，不硬编码 | 区分开发/生产环境的Tracing开关 |
| LCEL链中Stream失效 | 某些Runnable不支持stream | 确保所有节点实现 `transform()` 方法 | 自定义组件继承 `RunnableGenerator` 基类 |
| 多Agent冲突 | 多个Agent修改同一State字段 | 使用 `Annotated[list, add_messages]` 合并策略 | 明确每个Agent的State所有者和访问权限 |
| MCP Server连接超时 | 外部服务不稳定导致Agent卡死 | 设置 `timeout` 参数和Fallback策略 | 所有MCP调用增加超时和重试机制 |
| 会话记忆累积 | Memory无限增长导致Token超限 | 使用 `ConversationSummaryBufferMemory` 或 `ConversationBufferWindowMemory` | 设置 `max_token_limit` 或 `k` 参数限制记忆大小 |
| RAG检索召回率为0 | 问题表述与文档用词完全不匹配 | 使用HyDE（假设性问题）或Multi-Query策略 | 同时部署稠密+稀疏向量混合检索 |
| 跨语言检索不准确 | 中文问题检索英文文档效果差 | 使用多语言Embedding（BGE-m3、mE5） | 对于中文场景，优先选BGE或m3e系列 |
| 安全性问题 | Prompt注入攻击 | 使用 `hub.prompt_template` 结构化模板 + 输入清洗 | 所有用户输入都经过安全过滤，不直接拼接到Prompt |

---

## 七、面试回答模板（Top 5高频题的结构化回答模板）

### 7.1 "LangChain的LCEL和传统Chain有什么区别？"

**四层结构回答**：

1. **设计理念**：传统Chain是预定义的模板化调用链（如 `LLMChain`、`RetrievalQAChain`），而LCEL是声明式管道式组装，更灵活
2. **核心接口**：LCEL基于统一的Runnable协议（`invoke/batch/stream/ainvoke/astream`），传统Chain接口不统一
3. **组合能力**：LCEL支持 `|` 管道符、`RunnableParallel` 并行、`RunnableBranch` 条件分支等高阶组合；传统Chain只能线性调用
4. **流式支持**：LCEL天然支持整个管道的流式输出；传统Chain的流式支持有限

**加分回答**：V1.3中传统Chain被标记为Legacy，推荐基于LCEL的RunnableSequence替代。LCEL底层将 `|` 解析为 `RunnableSequence`，每个节点都是Runnable的子类，因此支持任意的管道嵌套。

### 7.2 "LangGraph解决了什么问题？和LangChain Agent有什么区别？"

**五层分析**：

1. **状态管理**：传统Agent是无状态的，每次调用独立；LangGraph通过State管理跨步骤状态
2. **控制流**：传统Agent是固定的ReAct循环；LangGraph支持条件分支、循环、并行执行
3. **中断恢复**：LangGraph的Checkpointer支持在任意节点中断和恢复执行，实现Human-in-the-Loop
4. **多智能体协作**：LangGraph天然支持多Agent拓扑（顺序、并行、分层），Agent间通过State传递信息
5. **可观测性**：每个Step的状态都可追踪，调试更容易

**总结**：LangGraph不是替代Agent，而是为Agent提供了更强大的执行引擎——"Agent是你开车的方式，LangGraph是路网系统"。

### 7.3 "如何评估和优化RAG系统的质量？"

**四层回答框架**：

**第一层：评估维度**
| 维度 | 指标 |
|------|------|
| 检索质量 | Context Precision, Context Recall, MRR, MAP |
| 生成质量 | Faithfulness, Answer Relevancy, Harmlessness |
| 用户体验 | 端到端延迟, 首Token延迟, 用户满意度 |
| 成本 | Token消耗, API调用次数, 缓存命中率 |

**第二层：优化方法论（从最有效到一般）**
1. **查询改写**：LLM将用户问题改写为标准格式 → 提升召回率 30-50%
2. **Reranker重排**：Cross-Encoder二次排序 → 提升精度 20-30%
3. **Multi-Query**：多角度检索 → 提升召回率 15-25%
4. **优化分块策略**：语义分块 + 合适overlap → 提升5-15%
5. **Embedding可选**：领域微调Embedding → 提升5-10%

**第三层：工具链**
- LangSmith：Tracing和自动化评估
- RAGAS框架：标准化RAG评估指标
- Phoenix/Arize：LLM可观测性

**第四层：持续监控**
- 搭建自动化测试流水线
- LLM-as-Judge自动打分
- 定期人工抽检校准

### 7.4 "MCP协议是什么？对AI生态有什么影响？"

**三层剖析**：

1. **定义**：MCP（Model Context Protocol）是Anthropic推出的模型上下文协议，定义LLM与外部工具/数据源交互的统一标准

2. **核心机制**：
   - Tools：LLM可调用的函数（类似插件API）
   - Resources：LLM可读取的静态数据（文件、数据库）
   - Prompts：可复用的提示模板
   - 采用C/S架构：MCP Server暴露资源，MCP Client（LangChain）消费

3. **生态影响**：
   - **标准化**：统一了LLM工具调用协议，类似HTTP之于Web
   - **可移植性**：同一MCP Server可被不同框架（LangChain、AutoGen、Semantic Kernel）调用
   - **去中心化**：模型和工具解耦，任何兼容MCP的LLM都能使用丰富的工具生态
   - **安全性**：MCP Server可独立部署，通过权限控制保证数据安全

**面试亮点**：可以对比MCP和Function Calling的区别——MCP是协议层，Function Calling是模型能力层，两者互补。

### 7.5 "生产环境中LangChain Agent的可靠性如何保证？"

**四层保障体系**：

**第一层：防御性设计**
- Fallback机制：主模型失败自动降级到次模型
- 超时控制：所有Tool调用设置超时（默认10s）
- 最大迭代限制：`recursion_limit` 防止无限循环
- 熔断机制：连续失败N次后停止Agent

**第二层：可观测性**
- 全链路Tracing：LangSmith记录每次调用
- 指标监控：延迟、Token消耗、成功率
- 告警规则：异常行为自动告警

**第三层：测试与评估**
- 回归测试集：核心场景的自动化测试
- 对抗测试：Prompt注入、边界情况测试
- 性能基准测试：P99延迟、并发能力

**第四层：Human-in-the-Loop**
- 高风险操作人工确认
- 内容输出审核
- 反馈闭环：用户反馈持续改进Agent

```python
# 生产Agent配置模板
production_config = {
    "timeout": 30,
    "max_retries": 2,
    "fallback_llm": ChatOpenAI(model="gpt-3.5-turbo"),
    "recursion_limit": 15,
    "callbacks": [LangSmithTracer(), MetricCollector()],
    "checkpointer": MemorySaver(),
}
```

---

## 八、快速查漏补缺 Checklist

- [ ] LangChain V1.3架构变化（Chain → Runnable + LangGraph）
- [ ] Runnable协议接口（invoke/batch/stream/ainvoke/astream）
- [ ] LCEL管道符号及RunnableSequence底层原理
- [ ] PromptTemplate和ChatPromptTemplate用法
- [ ] OutputParser类型（StrOutputParser、PydanticOutputParser、JsonOutputParser）
- [ ] ReAct Agent循环机制（Reasoning → Acting → Observation）
- [ ] Tool Calling机制（bind_tools装饰器）
- [ ] Agent Executor vs LangGraph Agent区别
- [ ] LangGraph核心概念（StateGraph / Node / Edge / Conditional Edge）
- [ ] LangGraph状态管理（State TypedDict + add_messages合并策略）
- [ ] LangGraph Checkpointer（MemorySaver中断恢复）
- [ ] RAG完整链路（Chunking → Embedding → Indexing → Retrieval → Reranking → Generation）
- [ ] 文档分块策略（RecursiveCharacter / Semantic / MarkdownHeader）
- [ ] 多路召回（Multi-Query / Self-Query / Ensemble）
- [ ] Reranker重排原理（Cross-Encoder vs Bi-Encoder）
- [ ] 向量数据库选型（FAISS / Chroma / Milvus / Qdrant）
- [ ] MCP协议核心概念（Tools / Resources / Prompts）
- [ ] MCP Server搭建（FastMCP装饰器）
- [ ] MCP Client集成（MCPAdapter连接LangChain）
- [ ] Human-in-the-Loop实现（LangGraph interrupt节点）
- [ ] 多智能体工作流设计（拓扑结构、State共享）
- [ ] Callback机制（自定义MetricCallbackHandler）
- [ ] 检索增强技术（Contextual Compression / HyDE / Query Rewrite）
- [ ] Token和上下文窗口优化策略
- [ ] LangSmith全链路Tracing配置
- [ ] RAG评估体系（Faithfulness / Relevancy / Precision / Recall）
- [ ] 医疗行业RAG专项（知识库构建、药品查询、诊断辅助）
- [ ] 生产Agent可靠性保障（Fallback / 超时 / 熔断）
- [ ] Streaming流式输出实现
- [ ] 缓存策略（LLM Cache / Embedding Cache / 结果缓存）
- [ ] 安全性防范（Prompt注入 / 数据脱敏 / 权限控制）
- [ ] LCEL并行分支（RunnableParallel、RunnablePassthrough）
- [ ] 工具定义方式（@tool装饰器 / BaseTool子类）
- [ ] 自定义Retriever实现（继承BaseRetriever）
- [ ] 对话记忆类型（Buffer / Summary / Window / VectorStore记忆）
- [ ] LangChain与LlamaIndex结合使用场景
