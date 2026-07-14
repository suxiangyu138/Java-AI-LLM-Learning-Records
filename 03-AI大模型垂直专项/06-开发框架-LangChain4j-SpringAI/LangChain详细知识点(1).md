# ⛓️ LangChain 详细知识点

> **核心摘要**：LangChain 是连接大语言模型与真实世界的开源开发框架，提供标准化的组件和流程编排能力。本文系统梳理 LangChain 的发展历史、六大核心组件、应用模式及实战基础。

**前置阅读**：[[快速吃透 LangChain]] | [[LangChain文档处理：加载、切分与检索链实战]]

---

> **注意**：本文与 [[LangChain详细知识点]] 内容一致，因文件名重复保留。推荐直接阅读主文件。

## 一、LangChain 核心定义与定位

**LangChain** 是 2022 年由 Harrison Chase 与 Ankush Gola 创立的开源开发框架，核心定位是"连接大语言模型（LLM）与真实世界"。核心价值在于解决 LLM 三大原生痛点：上下文窗口有限、无法访问外部动态数据、缺乏与外部工具交互的能力。

**核心优势**：模块化设计、预训练模型集成、链式调用引擎（LCEL）、数据感知能力、智能记忆管理、全栈生态支持。

## 二、发展历史

| 年份 | 里程碑 |
|------|--------|
| 2022 | 诞生，开源 | 
| 2023 | GitHub 38,000+ 星标，推出 LangSmith |
| 2024 | 红杉资本领投，发布 LangServe |
| 2025+ | langgraph 多智能体系统 |

## 三、核心架构与组件

| 组件 | 关键组件 | 典型场景 |
|------|----------|----------|
| Models | ChatModel、LLM、Embedding | 文本生成、语义理解 |
| Tools | APIs、数据库、搜索引擎 | Web 搜索、数据访问 |
| Agents | ReAct agents | 复杂任务决策 |
| Memory | ConversationBufferMemory | 多轮对话 |
| Retrievers | Vector retrievers | RAG 系统 |
| Document Processing | Document loaders、Text splitters | PDF 处理、网页抓取 |
| Vector Stores | Chroma、Pinecone、FAISS | 相似性搜索 |

### 3.1 Models

LangChain 提供两种模型接口：**LLM 接口**（纯文本补全，不推荐）和 **ChatModel 接口**（对话格式，官方主推）。模型连接方式包括 `ChatOpenAI` 类和 `init_chat_model` 方法。

### 3.2 Model I/O

- **Prompt Template**：字符串模板、对话模板（ChatPromptTemplate）
- **Output Parsers**：StrOutputParser、JsonOutputParser 等
- **调用参数**：temperature、max_tokens、streaming

### 3.3 Chains

| 类型 | 说明 |
|------|------|
| LLMChain | 基础链 |
| RetrievalQA | RAG 核心链 |
| SequentialChain | 串行链 |
| ParallelChain | 并行链 |

编排语言：**LCEL**（LangChain Expression Language）

### 3.4 Memory

| 类型 | 特点 |
|------|------|
| ConversationBufferMemory | 完整存储 |
| ConversationSummaryMemory | 摘要压缩 |
| VectorStoreRetrieverMemory | 向量检索 |
| ConversationBufferWindowMemory | 最近 N 轮 |

### 3.5 Agents

核心工作流程：分析任务 → 选择工具 → 执行 → 评估 → 调整 → 反馈。

### 3.6 Data Connection

文档加载 → 文本分割 → 嵌入与存储，参考 [[LangChain文档处理：加载、切分与检索链实战]]。

## 四、核心应用模式

- **RAG**：数据准备 → 检索 → 生成
- **Agent with Tools**：工具调用自动化
- **Multi-agent System**：多智能体协作（langgraph）

## 五、生态工具

| 工具 | 功能 |
|------|------|
| LangSmith | 调试平台 |
| LangServe | REST API 部署 |
| LangGraph | 多智能体系统 |
| LangChain Hub | Prompt 共享 |

## 六、实战基础

```bash
pip install langchain openai pypdf chromadb
```

```python
from langchain_openai import ChatOpenAI
llm = ChatOpenAI(model_name="gpt-3.5-turbo", temperature=0.7)
response = llm.invoke("请简要介绍 LangChain 框架")
print(response.content)
```

---

## 核心要点回顾

- 核心六件套：模型、提示词、记忆、文档、链式编排、Agent
- 编排核心：LCEL 声明式链式调用
- 三大应用：RAG、Agent with Tools、Multi-agent System
- 生态：LangSmith + LangServe + LangGraph

## 参考资料

1. [[LangChain详细知识点]]
2. [[快速吃透 LangChain]]
3. [[LangChain文档处理：加载、切分与检索链实战]]
