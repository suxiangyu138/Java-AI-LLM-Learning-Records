# 01 - LangChain 1.x 生态全景

> **核心摘要**：LangChain 于 2025.10.22 发布 v1.0 GA（2026.07 最新 **1.3.14**，要求 Python 3.10+）——AgentExecutor 进入维护模式、create_agent 统一 Agent API、Middleware 中间件系统是三大变化。本文覆盖 v1.0 核心变化、v1.1→v1.3 演进、分包格局与框架分工。

> **前置阅读**：[[00-LangChain知识体系总览]]

---

## 📚 目录

1. [v1.0 的定位](#1-v10-的定位)
2. [三大核心变化](#2-三大核心变化)
3. [分包格局](#3-分包格局)
4. [框架分工：LangChain vs LangGraph](#4-框架分工langchain-vs-langgraph)
5. [v1.0 新特性](#5-v10-新特性)
6. [v1.1 → v1.3 演进（2026）](#6-v11--v13-演进2026)
7. [安装与最小依赖](#7-安装与最小依赖)
8. [核心要点](#8-核心要点)

---

## 1. v1.0 的定位

> **背景**：LangChain 与 LangGraph 于 2025.10.22 同时发布 v1.0 GA——从「快速迭代」进入「稳定生产」。
> **目的**：理解 v1.0 的定位变化（面向生产）。
> **适用范围**：2026 新项目（Python 3.10+，3.9 已不支持）。

```text
v1.0 的定位
├── ① 生产稳定：API 稳定（不再频繁破坏）
├── ② 分工明确：LangChain（框架）+ LangGraph（编排）
├── ③ Agent 统一：create_agent 取代多种旧 API
├── ④ 可观测：LangSmith 深度集成
└── ⑤ 金句：1.x = 「从玩具到生产」的分水岭

版本要求
├── Python 3.10+（3.9 已不支持）
├── 当前版本：1.3.14（2026.07 最新）
└── 升级注意：0.x → 1.x 有破坏性变化
```

---

## 2. 三大核心变化

### 2.1 AgentExecutor 进入维护模式

```text
变化 1：AgentExecutor 维护模式（至 2026.12）
├── ① 旧 API：AgentExecutor / create_react_agent（弃用）
├── ② 新 API：create_agent(model, tools, prompt)
│   ├── 底层自动生成 LangGraph 状态机
│   ├── 开箱即用：流式/持久化/可观测
│   └── 统一入口（不再多种 Agent 类型）
└── ③ 金句：新代码禁用 AgentExecutor——用 create_agent

对比（0.x vs 1.x）
├── 0.x：十几种 Agent API（类型多、易选错）
├── 1.x：create_agent（一个入口 + 参数配置）
└── 简单：create_agent = 默认隐藏决策逻辑
```

### 2.2 Middleware 中间件

```text
变化 2：Middleware 系统（1.x 新特性）
├── ① SummarizationMiddleware：自动摘要压缩对话
│   ├── 长对话自动总结（上下文管理）
│   └── 解决上下文溢出
├── ② HumanInTheLoopMiddleware：高风险操作人工审批
│   ├── 资金/删除类操作拦截
│   └── 类似 Harness 的 HITL（见本仓库 Harness 系列）
└── ③ 金句：中间件 = 「横切能力的插拔式注入」

使用
from langchain.agents import create_agent
agent = create_agent(
    model=model,
    tools=tools,
    middleware=[SummarizationMiddleware()]
)
```

### 2.3 统一输出结构

```text
变化 3：content_blocks 统一结构化输出
├── ① 自动区分：reasoning（推理）与 text（回答）
├── ② 结构化：不再依赖「猜输出格式」
├── ③ 场景：多步推理的中间过程可见
└── ④ 金句：输出 = 「推理 + 回答」结构化分离
```

---

## 3. 分包格局

> 🎯 **v1.0 分包清晰**——按需引入（不再大而全）：

```text
分包结构
├── ① langchain-core：Runnable/LCEL/基础抽象（基石）
│   ├── 所有框架的公共基础
│   └── 必装
├── ② langchain：高层封装（Agent/Chain）
├── ③ langchain-community：第三方集成（社区）
├── ④ 独立 Provider 包：langchain-openai / langchain-anthropic 等
└── ⑤ 工具包：langchain-text-splitters（文本切块）等

新项目最小依赖
├── langchain-core
├── langchain-openai（或对应 Provider）
└── langchain-text-splitters（RAG 需要）
→ 金句：按需引入——不用全家桶
```

```bash
# 安装（2026 最小集）
pip install langchain-core langchain-openai langchain-text-splitters
# RAG 需要
pip install chromadb pypdf
# 完整
pip install langchain
```

---

## 4. 框架分工：LangChain vs LangGraph

> 🎯 **官方明确定位**：两者不是替代关系——同一技术栈的两个抽象层：

| 维度 | LangChain | LangGraph |
|------|:---:|:---:|
| 定位 | **Agent 框架** | **编排运行时** |
| 内容 | 模型/工具/循环抽象 + 1000+ 集成 | 状态/持久化/HITL |
| 解决的问题 | 直线/简单循环（for） | **while 循环/分支/HITL** |
| 状态管理 | 无 | ✅ StateGraph + Checkpointer |
| 持久化 | 无 | ✅（崩溃恢复） |
| 流式 | ✅ | ✅ |
| 性能 | 快（无状态开销） | 慢约 68%（checkpoint 序列化） |

```text
选型黄金法则（2026）
├── 「需要 while 循环或暂停等人工 → LangGraph」
├── 「只是 for 循环或直线（≤3 步）→ LangChain」
├── 线性 RAG/单轮 QA → LCEL（约 10 行；LangGraph 40 行 = 过度工程）
├── 单 Agent + 工具（3-8 步）→ create_agent
├── 多步骤 + HITL（5-20 步）→ LangGraph
└── 金句：复杂度决定框架——不为编排而编排
```

---

## 5. v1.0 新特性

```text
v1.0 其他新特性
├── ① Pydantic 结构化输出三策略：
│   ├── ProviderStrategy（模型原生支持）
│   ├── ToolStrategy（工具方式）
│   └── 自动策略（自动选择）
├── ② Runnable 三态同源：
│   ├── invoke（同步）/ainvoke（异步）/stream（流式）
│   └── 零改动切换
├── ③ LangSmith 可观测：
│   ├── 每个节点自动记录
│   └── 调试/评估
└── ④ 金句：1.x = 生产优先（稳定 + 可观测 + 可维护）
```

---

## 6. v1.1 → v1.3 演进（2026）

> 🎯 **v1.0 只是起点——2026 上半年快速迭代到 1.3.14**，按需跟进即可：

| 版本 | 时间 | 关键变化 |
|------|:---:|------|
| 1.1.x | 2026 Q1 | 结构化输出在主循环内直接生成（减少一次 LLM 调用）、PIIMiddleware 内置 |
| 1.2.x | 2026 Q2 | 中间件体系成熟（ToolRetry 指数退避重试）、agent 状态模式（state_schema） |
| 1.3.x | 2026 Q2-Q3 | **事件流式 stream_events v3**（typed-projection API）、ToolErrorMiddleware（#38781）、工具重试只重试可重试异常（#38845） |

```python
# v1.3 事件流式（typed-projection，替代 stream_mode 分支判断）
from langchain.agents import create_agent

agent = create_agent(model="openai:gpt-5.5", tools=[get_weather])
stream = agent.stream_events(
    {"messages": [{"role": "user", "content": "上海天气如何？"}]},
    version="v3",
)
for name, item in stream.interleave("messages", "tool_calls"):
    if name == "messages":
        print(item.text, end="", flush=True)
    else:
        print(f"\n[工具] {item.tool_name}({item.input}) → {item.output}")
final_state = stream.output  # 最终状态对象
```

> ⚠️ **版本认知**：网上 2025 年教程大多停留在 v0.3/1.0——搜索资料时认准 `1.3.x` 文档（[官方 Release Notes](https://docs.langchain.com/oss/python/releases)）。

---

## 7. 安装与最小依赖

```bash
# ═══════════ 基础安装 ═══════════
pip install langchain-core       # 核心（Runnable/LCEL）
pip install langchain-openai     # OpenAI/兼容 Provider
pip install langchain-anthropic  # Claude Provider

# ═══════════ RAG 组件 ═══════════
pip install langchain-text-splitters  # 文本切块
pip install langchain-community      # 社区集成（向量库等）
pip install chromadb                 # 向量库（示例）
pip install pypdf                    # PDF 解析

# ═══════════ Agent/编排 ═══════════
pip install langgraph                # 编排运行时
pip install langchain                # 高层封装

# ═══════════ 环境变量 ═══════════
export OPENAI_API_KEY=xxx
export ANTHROPIC_API_KEY=xxx
```

```python
# 最小使用（验证安装）
from langchain_openai import ChatOpenAI

model = ChatOpenAI(model="gpt-4o-mini")
response = model.invoke("你好")
print(response.content)
```

---

## 8. 核心要点

> 🎯 **核心要点**：
> 1. v1.0（2025.10 GA）：生产稳定、Agent 统一、分工明确
> 2. **三大变化**：AgentExecutor 维护（用 create_agent）/Middleware 中间件（摘要/HITL）/content_blocks 结构化输出
> 3. 分包：core（基石）+ langchain（高层）+ Provider 独立包——按需引入
> 4. **分工**：LangChain（框架）+ LangGraph（编排）——不是替代是同一栈两层
> 5. 选型法则：while 循环/HITL → LangGraph；直线 → LangChain
> 6. 新项目最小依赖：core + Provider + text-splitters

---

**下一模块**：[02-LCEL表达式语言](02-LCEL表达式语言.md) | **返回总览**：[00-LangChain知识体系总览](00-LangChain知识体系总览.md)
