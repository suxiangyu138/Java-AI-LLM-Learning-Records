# ⛓️ LangChain 详细知识点

> **核心摘要**：LangChain 是连接大语言模型与真实世界的开源开发框架，提供标准化的组件和流程编排能力。本文系统梳理 LangChain 的发展历史、六大核心组件（Models、Tools、Agents、Memory、Retrievers、Document Processing）、应用模式及实战基础。

**前置阅读**：[[快速吃透 LangChain]] | [[LangChain文档处理：加载、切分与检索链实战]]

---

## 目录

1. [核心定义与定位](#一langchain核心定义与定位)
2. [发展历史](#二langchain发展历史)
3. [核心架构与组件](#三langchain核心架构与组件)
4. [核心应用模式](#四langchain核心应用模式common-patterns)
5. [生态工具](#五langchain生态工具)
6. [实战基础](#六langchain实战基础)
7. [应用场景与注意事项](#七langchain常见应用场景)

---

## 一、LangChain 核心定义与定位

**LangChain** 是 2022 年由 Harrison Chase 与 Ankush Gola 创立的开源开发框架，核心定位是"连接大语言模型（LLM）与真实世界"。它为 LLM 应用开发提供标准化的组件和流程编排能力。

**核心价值**：解决 LLM 三大原生痛点——上下文窗口有限、无法访问外部动态数据、缺乏与外部工具交互的能力。

**核心优势**：
- 模块化设计，提升开发效率
- 预训练模型集成（OpenAI、Hugging Face）
- 链式调用引擎（LCEL 声明式编排）
- 数据感知能力（整合多源数据）
- 智能记忆管理（多轮上下文关联）
- 全栈生态（开发、调试、部署）

## 二、LangChain 发展历史

| 年份 | 里程碑 |
|------|--------|
| 2022 | 作为开源框架诞生，核心抽象提示管理、链式调用、外部数据集成 |
| 2023 | GitHub 星标突破 38,000；推出 LangSmith 开发者平台 |
| 2024 | 完成红杉资本领投融资；发布 LangServe 部署工具 |
| 2025+ | 推出 langgraph 构建多智能体系统；探索医疗诊断、工业自动化 |

## 三、LangChain 核心架构与组件

### 3.1 核心组件总览

| 组件类别 | 核心用途 | 关键组件 |
|----------|----------|----------|
| **Models** | AI 推理和生成能力 | ChatModel、LLM、Embedding 模型 |
| **Tools** | 外部能力，弥补 LLM 局限 | APIs、数据库、搜索引擎、计算器 |
| **Agents** | 自主决策和任务拆解 | ReAct agents、Tool calling agents |
| **Memory** | 保存上下文信息 | ConversationBufferMemory、SummaryMemory |
| **Retrievers** | 从外部数据源检索信息 | Vector retrievers、web retrievers |
| **Document Processing** | 原始数据→结构化文档 | Document loaders、Text splitters |
| **Vector Stores** | 向量存储与语义搜索 | Chroma、Pinecone、FAISS |

### 3.2 关键组件详解

#### Models：LLM 的交互入口

| 接口 | 说明 |
|------|------|
| **LLM 接口** | 纯文本补全，不推荐使用 |
| **ChatModel 接口** | 官方主推，输入消息列表（system/user/assistant） |

模型连接方式：`ChatOpenAI` 类（适配 OpenAI 规范）或 `init_chat_model` 方法（通用连接）。

#### Model I/O：输入输出管理

- **Prompt Template**：定义固定提示结构，动态参数填充
- **Output Parsers**：StrOutputParser、JsonOutputParser、ListOutputParser
- **模型调用参数**：temperature、max_tokens、streaming

#### Chains：流程编排核心

| Chain 类型 | 说明 |
|------------|------|
| **LLMChain** | Prompt + 模型 + 输出解析器 |
| **RetrievalQA** | RAG 核心链 |
| **SequentialChain** | 串行链 |
| **ParallelChain** | 并行链 |

核心编排语言：**LCEL**（LangChain Expression Language）

#### Memory：上下文感知

| 类型 | 特点 |
|------|------|
| ConversationBufferMemory | 完整存储，简单直观 |
| ConversationSummaryMemory | 摘要存储，省空间 |
| VectorStoreRetrieverMemory | 向量检索历史 |
| ConversationBufferWindowMemory | 仅最近 N 轮 |

#### Agents：自主决策

Agent 工作流程：分析任务 → 选择工具 → 执行 → 评估 → 调整 → 反馈

| Agent 类型 | 说明 |
|------------|------|
| **ReAct Agent** | 思考-行动-观察循环 |
| **Tool Calling Agent** | 专注工具调用 |
| **Multi-agent System** | 多智能体协作（langgraph） |

#### Data Connection：数据访问桥梁

- **文档加载**：PDF、Word、TXT、Excel、网页、Notion、数据库
- **文本分割**：按字符、句子、段落分割
- **嵌入与存储**：Embedding → Vector Store

## 四、核心应用模式

### 4.1 RAG（检索增强生成）

数据准备 → 检索阶段 → 生成阶段，参考 [[AI-RAG-快速学会]]。

### 4.2 Agent with Tools

Agent 自主调用外部工具，适用于实时数据、复杂计算、外部系统交互。

### 4.3 Multi-agent System

通过 langgraph 构建状态化多智能体协作系统。

## 五、生态工具

| 工具 | 功能 |
|------|------|
| **LangSmith** | 开发者调试平台：日志、Prompt 调试、版本控制 |
| **LangServe** | 部署工具：Chain/Agent → REST API |
| **LangGraph** | 状态化多智能体系统 |
| **LangChain Hub** | Prompt、Chain、Agent 共享平台 |

## 六、实战基础

### 6.1 环境搭建

```bash
python -m venv langchain-env
langchain-env\Scripts\activate  # Windows
pip install langchain openai pypdf chromadb
```

### 6.2 基础配置

```python
import os
from langchain_openai import ChatOpenAI

os.environ["OPENAI_API_KEY"] = "your-api-key"
llm = ChatOpenAI(model_name="gpt-3.5-turbo", temperature=0.7)
response = llm.invoke("请简要介绍 LangChain 框架")
print(response.content)
```

### 6.3 核心示例

```python
from langchain_core.prompts import PromptTemplate
from langchain_openai import ChatOpenAI
from langchain_core.output_parsers import StrOutputParser

prompt = PromptTemplate.from_template("用一句话描述{topic}")
model = ChatOpenAI(model_name="gpt-3.5-turbo")
parser = StrOutputParser()
chain = prompt | model | parser
result = chain.invoke({"topic": "人工智能"})
print(result)
```

## 七、常见应用场景

| 场景 | 说明 |
|------|------|
| RAG 系统 | 企业知识库、PDF 问答、法律检索 |
| 智能对话 | 客服、个人助手、教育辅导 |
| AI Agents | 报告生成、数据分析、代码生成 |
| 文档处理 | 摘要、对比分析、合同审核 |
| 教育与内容创作 | 学习方案、文案创作、剧本生成 |

---

## 核心要点回顾

- LangChain 六大核心组件：Models、Tools、Agents、Memory、Retrievers、Vector Stores
- 三大应用模式：RAG、Agent with Tools、Multi-agent System
- 生态工具：LangSmith（调试）、LangServe（部署）、LangGraph（多智能体）
- 开发首选 Python 版本，基础链为 Prompt → Model → OutputParser

## 参考资料

1. [[快速吃透 LangChain]]
2. [[LangChain文档处理：加载、切分与检索链实战]]
3. [[LangChain4j 核心知识点]]
