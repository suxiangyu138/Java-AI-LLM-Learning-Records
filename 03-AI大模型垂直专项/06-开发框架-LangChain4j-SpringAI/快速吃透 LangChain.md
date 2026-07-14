# ⛓️ 快速吃透 LangChain

> **核心摘要**：LangChain 是大模型应用开发的标准框架，核心解决上下文管理、知识库检索、工具调用、任务编排、Agent 智能体五大痛点。本文用极简方式梳理六大核心组件、两大落地架构（RAG 和 Agent）及极简可运行示例。

**前置阅读**：[[AI-RAG-快速学会]] | [[LangChain文档处理：加载、切分与检索链实战]]

---

## 一、LangChain 是什么

**LangChain** = 大模型应用开发框架。核心定位：把裸 LLM 快速封装成 RAG、AI 机器人、智能 Agent、私有知识库问答。

> **一句话**：只调用大模型 API 只能聊天；用 LangChain 才能做正经 AI 项目。

## 二、LangChain 解决的 5 个核心痛点

| 痛点 | 对应组件 |
|------|----------|
| 上下文太长、手动拼接历史对话 | Memory 记忆组件 |
| 大模型不懂私有文档 | Document 文档体系 |
| 大模型不能联网/查数据库/操作工具 | Tool 工具调用 |
| 复杂任务不会分步拆解 | Chain 链式编排 |
| 手写 Prompt 杂乱难维护 | Prompt 模板统一管理 |

## 三、六大核心组件

### 1. Models 模型层

对接各类大模型：云端（GPT、通义千问、GLM、DeepSeek）、本地（Ollama）。

| 模型类型 | 说明 |
|----------|------|
| **LLM** | 文本补全 |
| **ChatModel** | 对话模型（日常开发主流） |

### 2. Prompts 提示词模板

统一管理 Prompt，避免硬编码。内置对话模板、RAG 问答模板、Agent 指令模板，支持变量注入（`{context}`、`{question}`、`{history}`）。

### 3. Memory 记忆组件

| 类型 | 说明 |
|------|------|
| **ConversationBufferMemory** | 完整历史 |
| **SummaryMemory** | 超长对话自动摘要压缩 |
| **ConversationBufferWindowMemory** | 只存储最近 N 轮 |

### 4. Document 文档体系（RAG 核心）

```
文档加载 → 文本分割 → 向量化 → 存入向量库 → 检索召回
```

- **Loader**：PDF/MD/TXT/网页/Word
- **Splitter**：长文本切块
- **Embedding**：文本转向量
- **VectorStore**：Milvus、Chroma

### 5. Chain 链路编排

| 内置 Chain | 说明 |
|------------|------|
| **RetrievalQA** | 检索 + 问答（标准 RAG） |
| **ConversationChain** | 多轮对话 |
| **自定义 Chain** | 拼接复杂业务流程 |

### 6. Agents 智能体

以 LLM 为决策大脑，自动思考 → 拆解任务 → 选择工具 → 执行 → 汇总结果。

## 四、两大核心落地架构

### 1. RAG 检索增强生成

```
私有文档 → 切片 → Embedding → 存入 Milvus
用户提问 → 向量检索相似片段 → 拼接Prompt → 大模型回答
```

### 2. AI Agent 自动任务

```
用户目标 → LLM 拆解步骤 → 自动选择工具 → 循环执行 → 输出结果
```

## 五、极简使用流程

```python
from langchain_openai import ChatOpenAI
from langchain_core.prompts import ChatPromptTemplate

llm = ChatOpenAI(
    base_url="http://localhost:11434/v1",
    api_key="ollama",
    model="qwen"
)

prompt = ChatPromptTemplate.from_messages([
    ("system", "你是一名计算机后端讲师，回答简洁结构化"),
    ("user", "{input}")
])

chain = prompt | llm
res = chain.invoke({"input": "解释 RabbitMQ"})
print(res.content)
```

## 六、依赖安装

```bash
pip install langchain langchain-community langchain-openai
```

## 七、高频概念区分

| 概念 | 说明 |
|------|------|
| **LLM** | 单纯大模型，只会被动回答 |
| **LangChain** | 框架，组装 LLM + 知识库 + 工具 + 流程 |
| **RAG** | 一种应用架构，基于 LangChain 实现私有知识库 |
| **Agent** | 高级智能形态，基于 LangChain 实现自主任务 |

---

## 核心要点回顾

- LangChain = 大模型应用的脚手架
- 核心六件套：模型、提示词、记忆、文档、链式编排、Agent
- 做私有问答用 RAG，做自动任务用 Agent
- 本地开发首选：Ollama + LangChain + Milvus

## 参考资料

1. [[LangChain详细知识点]]
2. [[LangChain文档处理：加载、切分与检索链实战]]
3. [[LangChain4j 核心知识点]]
