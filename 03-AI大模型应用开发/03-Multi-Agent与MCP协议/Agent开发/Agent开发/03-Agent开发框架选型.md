# 03 - Agent 开发框架选型

> **核心摘要**：框架选错，事倍功半。本文横向对比 2026 年主流 Agent 开发框架（LangGraph/LangChain/AutoGen/CrewAI/Dify/Coze/Agent SDK），给出能力矩阵、选型决策树与各框架的完整上手示例。

> **前置阅读**：[[01-Agent核心概念与架构]]、[[02-Agent设计模式全景]]

---

## 📚 目录

1. [框架全景](#1-框架全景)
2. [能力矩阵对比](#2-能力矩阵对比)
3. [LangGraph：有状态图编程](#3-langgraph有状态图编程)
4. [AutoGen / CrewAI：多智能体协作](#4-autogen--crewai多智能体协作)
5. [Dify / Coze：低代码平台](#5-dify--coze低代码平台)
6. [Anthropic / OpenAI Agent SDK](#6-anthropic--openai-agent-sdk)
7. [新兴框架与基准](#7-新兴框架与基准)
8. [选型决策树](#8-选型决策树)
9. [核心要点](#9-核心要点)

---

## 1. 框架全景

### 1.1 2026 年框架谱系

```text
Agent 框架谱系
├── 代码优先（深度控制）
│   ├── LangGraph / LangChain（Python/JS）
│   ├── AutoGen（微软）
│   ├── CrewAI（角色扮演协作）
│   ├── Anthropic Agent SDK（Claude 生态）
│   └── OpenAI Agents SDK（GPT 生态）
├── 低代码平台（快速搭建）
│   ├── Dify（开源，企业自托管）
│   ├── Coze（字节，托管平台）
│   └── 各类 Agent Builder
└── 特殊形态
    ├── Autono（ReAct 强鲁棒）
    └── 自研轻量循环（学习首选）
```

### 1.2 选型的核心问题

```text
选型四问
├── ① 控制粒度：需要精细控制每一步吗？（图编程 vs 低代码）
├── ② 团队技能：团队会 Python/JS 吗？（代码优先 vs 可视化）
├── ③ 部署形态：私有化还是托管？（数据合规决定）
├── ④ 生态需求：要什么模型/工具生态？
```

---

## 2. 能力矩阵对比

| 维度 | LangGraph | AutoGen | CrewAI | Dify | Coze | Agent SDK |
|------|:---:|:---:|:---:|:---:|:---:|:---:|
| 形态 | 代码库 | 代码库 | 代码库 | 低代码 | 低代码 | 代码库 |
| 状态管理 | ✅✅（Checkpointer） | ✅ | ✅ | ✅ | ✅ | ✅ |
| 多 Agent | ✅ 子图 | ✅✅（原生） | ✅✅（角色） | ✅ 工作流 | ✅ | ✅ 委派 |
| 记忆 | ✅ 持久化 | ✅ | ✅ | ✅ 知识库 | ✅ | ✅ |
| MCP 支持 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| 部署 | 自托管 | 自托管 | 自托管 | 自托管/云 | 云 | 云/自托管 |
| 学习曲线 | 中高 | 中 | 低中 | 低 | 最低 | 中 |
| 适合 | 复杂流程 | 研究/协作 | 快速多 Agent | 业务快速落地 | 非技术者 | 官方生态 |

> 🎯 **一句话结论**：**复杂流程选 LangGraph，多 Agent 研究选 AutoGen，快速协作选 CrewAI，业务落地选 Dify，零代码选 Coze，官方生态选对应 Agent SDK**。

---

## 3. LangGraph：有状态图编程

### 3.1 核心理念

LangGraph 把 Agent 流程建模为**有向图**——节点（Node）是处理单元，边（Edge）是流转条件，状态（State）在图中传播：

```python
# LangGraph 最小示例：ReAct Agent 骨架
from langgraph.graph import StateGraph, END
from typing import TypedDict

class State(TypedDict):
    messages: list
    step: int

def agent_node(state: State):
    """节点：模型决策下一步"""
    # 调用模型，输出动作或最终答案
    return {"messages": state["messages"] + ["agent 决策..."]}

def tools_node(state: State):
    """节点：执行工具调用"""
    return {"messages": state["messages"] + ["工具执行结果..."]}

def router(state: State) -> str:
    """边：条件路由"""
    if state["step"] >= 20:          # 循环上限
        return END
    return "tools" if 需要调用工具(state) else END

# 构图
graph = StateGraph(State)
graph.add_node("agent", agent_node)
graph.add_node("tools", tools_node)
graph.add_edge("agent", "tools")
graph.add_conditional_edges("agent", router)
graph.add_edge("tools", "agent")     # 循环边
app = graph.compile()
```

### 3.2 核心能力

| 能力 | 说明 |
|------|------|
| **Checkpointer** | 状态持久化：断点续跑、时间旅行调试 |
| **条件路由** | 图边按条件分支（循环/终止/异常） |
| **子图** | 复杂流程模块化（多 Agent 基础） |
| **人机交互** | interrupt 节点插入人工确认 |

> 💡 **LangGraph 的价值**：把「循环 + 状态 + 恢复」从手写变成原生能力——这是 2025 年以来 Agent 开发的新基准。

---

## 4. AutoGen / CrewAI：多智能体协作

### 4.1 AutoGen（微软）

**核心理念**：多 Agent 对话协作——Agent 之间通过「对话」完成任务：

```python
# AutoGen 示例：双 Agent 协作
from autogen import ConversableAgent

coder = ConversableAgent(
    "coder",
    system_message="你是一名 Python 工程师，只输出可运行的代码",
    llm_config={"model": "gpt-4o"},
)

reviewer = ConversableAgent(
    "reviewer",
    system_message="你是代码审查员，检查 bug 后要求修改，直到通过",
    llm_config={"model": "gpt-4o"},
)

result = coder.initiate_chat(
    reviewer,
    message="写一个读取 CSV 并输出统计的函数",
    max_turns=10,
)
```

### 4.2 CrewAI（角色协作）

**核心理念**：像组建团队一样定义角色（Role）、目标（Goal）、分工（Crew）：

```python
# CrewAI 示例：内容团队
from crewai import Agent, Task, Crew

writer = Agent(
    role="技术作者",
    goal="撰写清晰的技术教程",
    backstory="10 年 Java 经验的技术作家",
)

reviewer = Agent(
    role="审核编辑",
    goal="检查技术准确性与表达",
)

write_task = Task(description="写 Agent 入门教程", agent=writer)
review_task = Task(description="审核并修订教程", agent=reviewer)

crew = Crew(agents=[writer, reviewer], tasks=[write_task, review_task])
crew.kickoff()
```

### 4.3 对比

| 维度 | AutoGen | CrewAI |
|------|---------|--------|
| 协作模型 | 自由对话 | 角色分工（更像团队） |
| 控制 | 细（对话轮次控制） | 粗（任务编排） |
| 学习曲线 | 中 | 低中 |
| 适用 | 研究/复杂对话协作 | 业务团队模拟 |

---

## 5. Dify / Coze：低代码平台

### 5.1 Dify（开源低代码）

```text
Dify 核心能力
├── 可视化编排：拖拽式 Agent/工作流
├── 知识库：RAG 内置（文档/网页/API 接入）
├── 插件生态：工具插件（含 MCP）
├── 应用发布：Web/API/嵌入
└── 部署：自托管（企业数据合规首选）
适用：业务团队快速落地、企业私有化 AI 应用
```

### 5.2 Coze（字节托管平台）

```text
Coze 核心能力
├── 拖拽式 Bot 搭建（无需代码）
├── 插件市场：海量现成插件
├── 工作流/知识库/记忆一体
├── 多平台发布（飞书/微信/网页）
└── 云托管：零运维
适用：非技术人员、快速上线验证
```

### 5.3 选型提示

> ⚠️ **低代码的边界**：适合「流程固定、需求清晰」的场景；复杂状态管理、深度定制、高性能要求时，低代码平台会卡住——**先低代码验证，再代码化迁移**是常见路径。

---

## 6. Anthropic / OpenAI Agent SDK

### 6.1 Anthropic Agent SDK

```python
# Claude Agent SDK 示例（官方）
from claude_agent_sdk import ClaudeAgent, AgentLoop

def fetch_tool(query: str) -> str:
    """查询工具"""
    return search(query)

agent = ClaudeAgent(
    name="research-agent",
    model="claude-sonnet-5",
    tools=[fetch_tool],
    loop=AgentLoop.while_loop(
        max_steps=25,
        terminal_conditions=["final_answer"],
    ),
)

result = agent.run("调研 Agent 开发趋势，输出摘要")
```

**特点**：官方 SDK 与 Claude 模型深度集成、Tool Runner 原生支持、自动 while-loop、子 Agent 委派。

### 6.2 OpenAI Agents SDK

```python
# OpenAI Agents SDK 示例
from agents import Agent, Runner, handoff

triage = Agent(
    name="triage",
    instructions="路由到正确的手下 Agent",
    handoffs=[billing_agent, support_agent],   # Handoff 机制
)

result = Runner.run_sync(triage, "我的账单有问题")
```

**特点**：Handoff（交接）机制、Guardrails 内置、与 GPT 系列模型深度集成。

### 6.3 选择逻辑

| 场景 | 选择 |
|------|------|
| Claude 模型生态 + 终端/CLI | Anthropic Agent SDK |
| GPT 模型生态 + 云服务 | OpenAI Agents SDK |
| 多模型混用 | LangGraph（模型无关） |
| 深度自定义 | 框架选型后二次开发 |

---

## 7. 新兴框架与基准

### 7.1 Autono（ReAct 强鲁棒框架）

> 📊 实验数据：Autono 在**多步任务含失败场景**下表现 93.3%，远超 autogen 与 langchain 的 3.3%——差异来自其「高鲁棒性 Harness」设计（失败重试、状态检查）。

| 框架 | 多步任务成功率（含失败场景） |
|------|:---:|
| **Autono** | **93.3%** |
| AutoGen | 3.3% |
| LangChain（Chain 范式） | 3.3% |

> 💡 **启示**：框架的「Harness 能力」（失败恢复、状态管理）对鲁棒性影响巨大——选型时不要只看生态，要看失败场景下的表现。

### 7.2 模型底座要求

```text
Agent 开发对模型的要求（2026）
├── Function Calling：结构化工具调用必须稳定
├── 长上下文：复杂任务需要（128K-1M）
├── 推理能力：强推理模型（Claude/GPT/Gemini 旗舰）
├── 成本：路由策略（简单任务用小模型）
└── 推荐：Gemini 2.x/3.x、Claude 3.5+、DeepSeek-V3+、GPT-4o+
```

---

## 8. 选型决策树

```text
开始
 │
 ├─ 团队会编程吗？
 │   ├─ 不会 → Dify / Coze（低代码平台）
 │   └─ 会 → 下一步
 │
 ├─ 要私有化部署吗？
 │   ├─ 是 → LangGraph / AutoGen / 自研
 │   └─ 否 → 可选托管平台
 │
 ├─ 复杂程度？
 │   ├─ 简单线性流程 → Chain / 低代码
 │   ├─ 复杂图流程 → LangGraph
 │   └─ 多 Agent 协作 → AutoGen / CrewAI
 │
 ├─ 模型生态？
 │   ├─ Claude → Anthropic Agent SDK
 │   ├─ GPT → OpenAI Agents SDK
 │   └─ 多模型 → LangGraph
 │
 └─ 学习/验证？
     └─ 自研轻量循环（理解原理）→ 框架（生产）
```

> 🎯 **最终建议**：学习顺序 = 自研轻量循环（懂原理）→ LangGraph（生产主力）→ 按需引入 Agent SDK/CrewAI；平台工具（Dify/Coze）用于快速验证。

---

## 9. 核心要点

> 🎯 **核心要点**：
> 1. 框架谱系：代码优先（LangGraph/AutoGen/CrewAI/Agent SDK）vs 低代码（Dify/Coze）——控制粒度与上手速度的权衡
> 2. LangGraph 是 2026 新基准：图编程 + Checkpointer + 条件路由 + 中断恢复
> 3. 多 Agent：AutoGen 对话协作、CrewAI 角色分工；官方生态用对应 Agent SDK
> 4. 选型看「失败场景表现」：Autono 93.3% vs 3.3% 证明 Harness 能力 > 生态名气

---

**下一模块**：[04-记忆与状态管理](04-记忆与状态管理.md) | **返回总览**：[00-Agent开发知识体系总览](00-Agent开发知识体系总览.md)
