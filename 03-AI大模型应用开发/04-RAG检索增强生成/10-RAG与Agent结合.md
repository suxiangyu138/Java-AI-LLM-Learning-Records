# 10 - RAG 与 Agent 结合

> 🎯 RAG + Agent = 从"被动检索"到"主动探查" — Agent 决定何时检索、检索什么、怎么用检索结果，这是 RAG 的下一站

---

## 目录

1. [从 RAG 到 Agentic RAG](#1-从-rag-到-agentic-rag)
2. [ReAct + RAG = 推理检索Agent](#2-react--rag--推理检索agent)
3. [Tool-Augmented RAG](#3-tool-augmented-rag)
4. [多步检索 Planner](#4-多步检索-planner)
5. [Java 后端 Agent 集成方案](#5-java-后端-agent-集成方案)

---

## 1. 从 RAG 到 Agentic RAG

```text
传统 RAG：线性流程
  问题 → 检索 → 生成 → 回答
  → 一次检索，一次生成

Agentic RAG：动态决策
  问题 → 思考 → 需要查资料？→ 检索 → 够了吗？
    → 不够 → 换关键词再查 → 还需要计算？→ 调计算器
    → 够了 → 综合生成 → 回答

关键差异：Agent 有"判断力"
  → 什么时候需要检索
  → 检索什么
  → 检索结果好不好
  → 需要补充什么
```

---

## 2. ReAct + RAG = 推理检索 Agent

```python
# ReAct Agent with RAG tool
from langchain.agents import create_react_agent, Tool
from langchain_community.vectorstores import Chroma

# 定义 RAG 检索工具
retriever = vectorstore.as_retriever()

def search_knowledge_base(query):
    """检索企业知识库"""
    docs = retriever.get_relevant_documents(query)
    return "\n".join([d.page_content for d in docs])

tools = [
    Tool(
        name="KnowledgeBase",
        func=search_knowledge_base,
        description="搜索企业内部知识库。当你需要查找公司政策、技术文档时使用。"
    ),
    Tool(
        name="Calculator",
        func=lambda x: eval(x),
        description="执行数学计算。输入数学表达式。"
    ),
]

# 创建 Agent
agent = create_react_agent(llm, tools, prompt_template)

# Agent 自主决定何时检索
response = agent.invoke({
    "input": "公司年假怎么算？我有5年工龄，可以休多少天？"
})
# Agent 会：
#   Thought: 需要查公司年假政策
#   Action: KnowledgeBase("年假计算公式")
#   Thought: 找到了, 年假=工龄×5天
#   Action: Calculator("5*5")
#   Thought: 答案是25天
#   Final Answer: 你有5年工龄，可以休25天年假
```

---

## 3. Tool-Augmented RAG

```text
Tool-RAG = RAG 不只是检索文本，还能调用其他工具

工具矩阵：
  ① 文档检索（Knowledge Base）
  ② Web 搜索（实时信息）
  ③ 数据库查询（结构化数据）
  ④ 代码执行（计算/数据处理）
  ⑤ API 调用（外部服务）

Agent 根据问题自主选择工具组合
```

```python
# 多工具 RAG Agent
tools = [
    Tool(name="SearchDocs", func=search_docs, 
         description="搜索内部文档"),
    Tool(name="WebSearch", func=web_search, 
         description="搜索互联网，查实时信息"),
    Tool(name="QueryDB", func=query_database, 
         description="查询数据库，如'最近一周的订单量'"),
    Tool(name="RunCode", func=execute_python, 
         description="执行Python代码做数据分析"),
]
```

---

## 4. 多步检索 Planner

```text
多步检索 = 复杂问题拆解为子问题 → 逐个检索 → 综合

问题："公司上个季度的销售额和客户满意度评分是多少？
       哪个产品线增长最快？"

传统 RAG：一次检索 → 可能漏掉部分信息

多步检索：
  Step 1: Thought: 需要查上季度销售额
          Action: SearchDocs("2026 Q2 销售报告")
  Step 2: Thought: 还需要客户满意度数据
          Action: SearchDocs("2026 Q2 客户满意度调查")
  Step 3: Thought: 需要比较各产品线增长率
          Action: QueryDB("SELECT product_line, growth_rate FROM q2_report")
  Step 4: Thought: 信息齐全 → 综合回答
```

---

## 5. Java 后端 Agent 集成方案

```text
Java 后端集成 RAG Agent 的方案：

  ① 独立 Agent 服务（推荐）
    Python LangChain Agent → FastAPI 服务 → Java HTTP 调用
    优势：利用 Python 生态，Java 只负责调用

  ② Spring AI
    Spring 官方 AI 框架，原生 Java API
    支持 OpenAI/ollama/向量数据库
    但 Agent 能力不如 LangChain 成熟

  ③ LangChain4j
    LangChain 的 Java 移植版
    支持基本 RAG 但 Agent 功能有限

  ④ 混合架构（推荐组合）
    Java 端：业务逻辑 + 用户管理 + 权限控制
    Python 端：LangChain Agent + RAG 检索 + LLM 推理
    通信：HTTP/gRPC
```

---

## 核心要点回顾

- Agentic RAG = 动态决策：何时检索、检索什么、要不要再搜
- ReAct + RAG：思考→行动→观察→思考的循环
- Tool-RAG：不只检索文本，还能调数据库/API/代码
- 多步检索：复杂问题拆解 → 逐个检索 → 综合
- Java 集成：Python Agent 服务 + HTTP 调用（推荐）
