# 05 Workflows 与 Agent 编排

> LlamaIndex 的编排层：Workflows 1.0 事件驱动异步编排（非 DAG）、LlamaAgents 文档处理 Agent 一键部署——生产基准 6ms 框架开销，比 LangGraph 更轻。

## 📚 目录

1. [Workflows 设计思想](#1-workflows-设计思想)
2. [事件驱动 vs DAG](#2-事件驱动-vs-dag)
3. [Workflows 核心语法](#3-workflows-核心语法)
4. [子工作流与嵌套](#4-子工作流与嵌套)
5. [LlamaAgents](#5-llamaagents)
6. [生产基准与对比](#6-生产基准与对比)
7. [面试高频问法](#7-面试高频问法)
8. [Workflows 完整示例](#8-workflows-完整示例)
9. [常见误区](#9-常见误区)

## 1. Workflows 设计思想

### 是什么

```
Workflows 1.0（2025-06-30 GA）：
LlamaIndex 的异步智能体编排系统
步骤 = 类型化事件处理器（监听事件 → 处理 → 发出事件）
```

### 核心特性

| 特性 | 说明 |
|---|---|
| 事件驱动 | 步骤间靠事件传递（非显式边） |
| 异步 | 原生 async（并发高效） |
| 类型化事件 | 每步声明输入/输出事件类型 |
| 状态持久化 | 逐步保存与重放 |
| 子工作流 | 嵌套组合（智能体模式） |
| 模型无关 | 任意 LLM（Claude/GPT/Gemini/Llama） |
| 可观测 | OTel + LlamaTrace |

### 定位

```
Workflows 与 LangGraph/CrewAI 竞争智能体编排
但保留了 LlamaIndex 的数据层优势（检索/解析原生集成）
```

## 2. 事件驱动 vs DAG

| 维度 | 事件驱动（Workflows） | DAG（LangGraph/Haystack） |
|---|---|---|
| 结构 | 步骤 + 事件流 | 节点 + 显式边 |
| 路由 | 动态（事件类型决定） | 静态（图结构决定） |
| 修改 | 加步骤不改图 | 加节点要连边 |
| 异步 | 原生 | 部分 |
| 调试 | 事件追踪 | 图可视化 |
| 心智 | 发布-订阅思想 | 状态机思想 |

### 事件驱动的优势场景

```
动态流程（下一步取决于上一步结果）→ 事件驱动灵活
固定流程（顺序/并行明确）→ DAG 更直观
```

## 3. Workflows 核心语法

```python
from llama_index.core.workflow import (
    Workflow, StartEvent, StopEvent, step, Event,
)

# 自定义事件类型
class RetrieveEvent(Event):
    query: str

class GenerateEvent(Event):
    query: str
    context: list

class RAGWorkflow(Workflow):
    """事件驱动的 RAG 工作流"""

    @step
    async def retrieve(self, ev: StartEvent) -> GenerateEvent:
        # 监听 StartEvent → 检索 → 发出 GenerateEvent
        nodes = retriever.retrieve(ev.query)
        return GenerateEvent(query=ev.query, context=nodes)

    @step
    async def generate(self, ev: GenerateEvent) -> StopEvent:
        # 监听 GenerateEvent → 生成 → 发出 StopEvent
        answer = llm.complete(build_prompt(ev.query, ev.context))
        return StopEvent(result=answer)

# 运行
wf = RAGWorkflow(timeout=120)
result = await wf.run(query="问题")
```

### 语法要点

| 要素 | 说明 |
|---|---|
| @step | 标记步骤（异步函数） |
| 事件类型 | StartEvent 入口 / StopEvent 出口 / 自定义事件 |
| 参数类型 | 步骤参数 = 监听的事件类型 |
| timeout | 整体超时（防卡死） |

## 4. 子工作流与嵌套

### 嵌套模式

```python
class SubFlow(Workflow):
    @step
    async def run(self, ev: StartEvent) -> StopEvent:
        return StopEvent(result=f"子任务完成：{ev.data}")

class MainFlow(Workflow):
    @step
    async def orchestrate(self, ev: StartEvent) -> StopEvent:
        # 调用子工作流（智能体模式）
        sub = SubFlow()
        result = await sub.run(data=ev.query)
        return StopEvent(result=result)
```

### 嵌套的价值

```
子工作流 = 可复用能力单元：
文档处理子流程 / 检索子流程 / 评估子流程
组合出复杂智能体（对应多 Agent 分层思想）
```

## 5. LlamaAgents

### 是什么

```
2025 年推出：文档处理智能体一键部署
内置模板：发票处理/合同审查/理赔处理
构建在 Workflows 之上
```

### 特点

| 特点 | 说明 |
|---|---|
| 一键部署 | 模板开箱即用（带 API + UI） |
| Builder | 自然语言界面定制（2026-02 支持文件上传） |
| 数据层 | 原生集成 LlamaParse/LlamaCloud 索引 |
| 定位 | "把混乱文档变成结构化数据"的 Agent |

### 典型用例

```
发票对账智能体：
LlamaParse（解析发票）→ LlamaCloud Index（索引）
→ Workflows（编排）→ 结构化数据输出
```

## 6. 生产基准与对比

### 2026 基准数据（框架开销）

| 框架 | 每次查询框架开销 | Token 开销 |
|---|---|---|
| **LlamaIndex** | **约 6ms** | **约 1.6K tokens** |
| LangGraph | 约 14ms | 约 2.4K tokens |

### 基准解读

```
LlamaIndex 编排更轻：
- 开销低 = 高并发场景吞吐更高
- token 省 = 成本更低（每查询少 0.8K）
- 但：功能深度（状态机能力）不如 LangGraph
```

### 代码量对比

```
基础 RAG 流水线：
LlamaIndex 比 LangChain 少 30-40% 代码
（封装深度：建索引/查询引擎一条语句）
```

## 7. 面试高频问法

| 问题 | 回答要点 |
|---|---|
| Workflows 是什么？ | 事件驱动异步编排（1.0，2025-06） |
| 与 DAG 区别？ | 事件传递 vs 显式边；动态 vs 静态 |
| 核心语法？ | @step + StartEvent/StopEvent/自定义事件 |
| 子工作流？ | 嵌套组合（可复用能力单元） |
| LlamaAgents？ | 文档处理 Agent 一键部署（发票/合同模板） |
| 基准数据？ | 6ms 开销 vs LangGraph 14ms |

### 面试加分表达

> "Workflows 的事件驱动设计和 DAG 是两种心智：事件驱动适合动态流程（下一步取决于上一步），改流程不用改图；生产基准上 LlamaIndex 每次查询 6ms 开销、1.6K token，比 LangGraph 轻一半。LlamaAgents 把文档处理（发票/合同）做成一键部署，数据层是它的差异化。"

## 8. Workflows 完整示例

```python
"""Agentic RAG：检索 → 评估 → 不足再检索（反思循环）"""
from llama_index.core.workflow import (
    Workflow, StartEvent, StopEvent, step, Event,
)

class RetrieveEvent(Event):
    query: str
    rounds: int

class AssessEvent(Event):
    query: str
    context: list
    rounds: int

class AgenticRAGWorkflow(Workflow):
    """多轮反思检索（对应 Self-RAG 思想）"""

    @step
    async def retrieve(self, ev: StartEvent) -> AssessEvent:
        nodes = retriever.retrieve(ev.query)
        return AssessEvent(query=ev.query, context=nodes, rounds=1)

    @step
    async def assess(self, ev: AssessEvent) -> StopEvent | RetrieveEvent:
        # 评估检索是否充分（反思）
        answer = llm.complete(build_prompt(ev.query, ev.context))
        sufficient = judge(answer, ev.context)
        if sufficient or ev.rounds >= 3:      # 充分或达上限
            return StopEvent(result=answer)
        return RetrieveEvent(query=ev.query, rounds=ev.rounds + 1)  # 再检索

wf = AgenticRAGWorkflow(timeout=120)
result = await wf.run(query="复杂多跳问题")
```

### 示例解读

```
事件驱动优势的体现：assess 步骤动态决定"继续还是结束"
（DAG 中这需要显式条件边；事件驱动天然支持）
轮次上限 = 终止兜底（阶段 3 纪律）
```

## 9. 常见误区

| 误区 | 真相 |
|---|---|
| "Workflows = 流程图" | 是事件流（步骤间发布-订阅） |
| "必须画图" | 代码定义步骤与事件，无需图工具 |
| "事件驱动不可预测" | 有类型化事件 + OTel 追踪（可控） |
| "比 LangGraph 全面" | 更轻但状态机深度不如 |
| "Agent 必须用 LlamaAgents" | 自定义 Workflows 是基础，模板是加速 |

> 🎯 核心要点：Workflows = 事件驱动异步编排（@step + 类型化事件，非 DAG）；动态流程选事件驱动、固定流程选 DAG；反思循环（检索→评估→再检索）用事件驱动天然实现；子工作流实现嵌套智能体；LlamaAgents 是文档处理 Agent 一键部署（数据层差异化）；基准 6ms/1.6K token 优于 LangGraph（但状态机深度不如它）。

---

**下一模块**：[06-LlamaParse文档解析](06-LlamaParse文档解析.md) / **返回总览**：[00-LlamaIndex知识体系总览](00-LlamaIndex知识体系总览.md)
