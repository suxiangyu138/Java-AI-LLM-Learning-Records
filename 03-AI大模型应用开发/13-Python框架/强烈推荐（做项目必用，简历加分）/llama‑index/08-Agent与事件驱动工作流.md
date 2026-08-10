# 08 - Agent 与事件驱动工作流

> 本体系第八课：从"问答"到"执行"——Workflows 事件驱动编排、AgentWorkflow 多 Agent 协作——"查询引擎答问题，工作流干活"

---

## 📚 目录

1. [一句话定位](#1-一句话定位)
2. [Workflows：事件驱动编排](#2-workflows事件驱动编排)
3. [AgentWorkflow：多 Agent 协作](#3-agentworkflow多-agent-协作)
4. [人机协同](#4-人机协同)
5. [与 LangGraph 的对比](#5-与-langgraph-的对比)
6. [生产要点](#6-生产要点)
7. [事件设计实践](#7-事件设计实践)
8. [练习 5 题](#8-练习-5-题)
9. [本节验收](#9-本节验收)

---

## 1. 一句话定位

**Workflows 是 LlamaIndex 的编排标准（0.14 取代 QueryPipeline）——事件驱动、类型安全、像写普通 async Python；AgentWorkflow 是它上面的多 Agent 编排层**：

```text
编排两层
├── Workflows：事件驱动状态机（step 装饰器 + 类型化事件）——通用编排
└── AgentWorkflow：多 Agent（root_agent + handoff 互转 + 共享状态）——Agent 编排
    ——"QueryPipeline（DAG）已弃用——新代码一律 Workflows"
```

**定位心智**：**"LlamaIndex 的定位演进：RAG 库 → 事件驱动 Agent 框架——2026 年它已不只是'检索'，是'检索 + 工具 + 多步流程'的完整编排"**——"**为什么事件驱动：步骤间靠'事件类型'松耦合——加步骤不改别人的代码（对比 QueryPipeline 的硬连线 DAG）"**。

## 2. Workflows：事件驱动编排

```python
from llama_index.core.workflow import (
    Workflow, StartEvent, StopEvent, step, Context, Event
)

class RetrieveEvent(Event):
    query: str

class MyRAGWorkflow(Workflow):
    @step
    async def retrieve(self, ctx: Context, ev: StartEvent) -> RetrieveEvent:
        nodes = await self.retriever.aretrieve(ev.query)   # 业务步骤
        return RetrieveEvent(query=ev.query, nodes=nodes)

    @step
    async def synthesize(self, ctx: Context, ev: RetrieveEvent) -> StopEvent:
        answer = await self.llm.acomplete(  # 合成
            f"上下文：{ev.nodes}\n问题：{ev.query}")
        return StopEvent(result=str(answer))

wf = MyRAGWorkflow(retriever=retriever, llm=Settings.llm)   # 依赖注入
result = await wf.run(query="……")                           # 事件自动路由
```

**要点**：① `@step` 装饰器 + 入参事件类型 = 节点；**返回什么事件类型，事件就路由到消费它的 step——边是"推断"出来的**；② `Context` 在步骤间传状态（`ctx.data[...]`）；③ `run()` 全异步、`wf.run_stream()` 逐步出结果（流式 UI）；④ 支持 subworkflow 嵌套（一个 Workflow 调另一个）——**"简单用 Workflow，复杂上 AgentWorkflow"**。

## 3. AgentWorkflow：多 Agent 协作

```python
from llama_index.core.agent.workflow import (
    AgentWorkflow, FunctionAgent, ReActAgent,
)

# 定义两个 Agent（模型 + 工具 + 系统提示词）
researcher = FunctionAgent(
    name="Researcher", tools=[search_tool, read_tool],
    system_prompt="负责检索资料，输出事实摘要",
)
writer = ReActAgent(
    name="Writer", tools=[write_tool],
    system_prompt="负责把摘要写成报告",
    can_handoff_to=["Researcher"],   # 允许转给谁
)

agent_workflow = AgentWorkflow(
    agents=[researcher, writer],
    root_agent="Researcher",          # 入口 Agent（接首个问题）
)
resp = await agent_workflow.run(user_msg="调研 LlamaIndex 并写报告")
```

**机制要点**：① **handoff 自动注入**——每个 Agent 自带一个 `handOff` 工具，调用即把执行权转给目标 Agent（受 `can_handoff_to` 白名单约束）；② **共享状态**——`ctx.store` 全局字典所有 Agent 可见（调研结果随手传）；③ **统一记忆**——一个 `ChatMemoryBuffer` 跨 Agent 保留对话历史；④ **结构化输出**——`output_cls=Pydantic 模型` 让最终答案强类型返回；⑤ `run_stream()` 流式看每个 Agent 的轨迹——**"多 Agent 不是各干各的——是'转交 + 共享 + 统一记忆'"**。

**结构化输出示例**（Agent 返回强类型结果——下游直接消费）：

```python
from pydantic import BaseModel, Field

class ResearchReport(BaseModel):
    summary: str = Field(description="报告摘要")
    findings: list[str] = Field(description="要点列表")
    citations: list[str] = Field(description="引用来源")

resp = await agent_workflow.run(
    user_msg="调研 LlamaIndex 的 2026 特性",
    output_cls=ResearchReport,   # 最终输出强制为 Pydantic 模型
)
report: ResearchReport = resp.data   # 类型安全的答案
```

**要点**：`output_cls` 是 AgentWorkflow 的"结构化出口"——**"下游（数据库/前端/另一个 Agent）拿到的是对象不是字符串——'结构化输出 = Agent 的可编程性'"**（与 Function Calling 体系的结构化输出同思想）；`resp.data` 是最终结果、`resp.agent_name` 是哪个 Agent 完成的——"**调试先看这两字段：结果对不对 + 谁出的结果"**。

## 4. 人机协同

**Human-in-the-loop——关键步骤暂停等人工确认**：

```text
人机协同模式
├── 审批暂停：Agent 生成待确认结果 → 流程暂停 → 人工批准/驳回后继续
├── 工具护栏：高危工具（转账/删除）执行前强制人工确认
└── 纠偏注入：中途人工给方向性指示，Agent 接着干
    ——"生产 Agent 不是全自动——该停下来问人的地方必须停"
```

**心智**：**"人机协同是'生产 Agent'与'玩具 Agent'的分水岭——高风险动作（改数据/发消息/付款）全部设人工确认点"**——"**与 Function Calling 体系（`../../../02-大模型基础与Prompt工程/Function%20Calling%20函数调用【Agent%20基石】/`）的确认-执行模式同源——框架层实现的是协议层的设计"**。

## 5. 与 LangGraph 的对比

| 维度 | LlamaIndex Workflows | LangGraph |
|------|----------------------|-----------|
| 建图方式 | 事件类型推断边（像 async Python） | 显式 add_node/add_edge |
| 状态管理 | ctx/ctx.store（可选） | 集中 State 对象（默认） |
| 持久化 | WorkflowCheckpointer（可选） | Checkpointer 一等公民 |
| 心智负担 | 低（贴近普通代码） | 高（图思维） |
| 适合 | LlamaIndex 生态内的流程 | 复杂长时状态 Agent |

**选型心智**：**"在 LlamaIndex 栈内做流程用 Workflows；要做'长时运行 + 时间旅行 + 强状态'的复杂 Agent 选 LangGraph——"**（"**LlamaIndex 官方定位：Workflows 补足'编排'短板——与 LangGraph 竞争而非替代（00 篇分工）"**）；**换用姿势**——Workflows 的事件模型与 LangGraph 状态机是两套心智——**"别在 LlamaIndex 里硬套 LangGraph 的图思维（显式边），也别把 Workflows 的隐式路由当黑盒——'选一个就按一个的规矩写'"**。

## 6. 生产要点

**① 状态持久化**：`WorkflowCheckpointer` 挂上，中断/重跑可恢复——"**长流程（调研/审批链）必挂：任务一半挂了能续，不用从头来"**；**② 可观测性**：Workflows 原生 OTel 追踪——每一步事件耗时、工具调用全记录（09 篇）；**③ 工具治理**：工具 = 生产端点——超时/限流/幂等照 API 标准做；**④ 提示注入**：约束 Agent 动作空间（工具白名单 + 输出校验）——"**工作流把'跑通'变'可靠'，靠的是持久化 + 观测 + 护栏三件套**"。

**工具复用 RAG 能力**：查询引擎/检索器一行转工具——`query_engine.as_tool(description="...")`（10 篇实战骨架）——**"RAG 是 Agent 的一个工具——'知识库问答 + 工具调用 + 多步流程 = 完整 Agent'（与 Function Calling 体系的 Agent 公式同构：模型 + 工具 + 循环）"**。

## 7. 事件设计实践

**状态共享与分支（Context 是步骤间的"小纸条"）**：

```python
from llama_index.core.workflow import Context, Event, step, StartEvent, StopEvent

class NeedApproval(Event):          # 分支事件：人工审批
    summary: str

class WorkflowWithGate(Workflow):
    @step
    async def draft(self, ctx: Context, ev: StartEvent) -> Event:
        ctx.data["round"] = ctx.data.get("round", 0) + 1   # 状态跨步骤传递
        if ctx.data["round"] > 3:                          # 循环上限（护栏）
            return StopEvent(result="已达重试上限")
        return NeedApproval(summary=await self.llm.acomplete(ev.query))

    @step
    async def gate(self, ctx: Context, ev: NeedApproval) -> Event:
        # 人机协同点：调用审批 API，驳回则回到 draft（事件循环）
        if await self.approve_api.is_approved(ev.summary):
            return StopEvent(result=ev.summary)
        return StartEvent(query="重写")
```

**设计四原则**：

```text
事件设计四原则
├── ① 事件 = 数据契约：字段即接口——改字段所有消费方要同步（类型即文档）
├── ② 一个 step 只干一件事：检索/合成/审批分开——可复用可测试
├── ③ 循环必须有上限：护栏事件（如 round > 3 强制停）防死循环
├── ④ 错误用事件表达：失败也返回事件（RetryEvent）而不是抛异常——
    工作流能"接住"并决定重试/降级
    ——"事件驱动的心智：流程是'事件流'，不是'函数调用栈'"
```

**流式与超时**：`wf.run_stream()` 逐事件输出（前端可渲染步骤进度）；单步超时用 `asyncio.timeout` 包住工具调用——**"生产工作流 = 事件契约 + 护栏 + 观测（09 篇）"**。

## 8. 练习 5 题

1. Workflows 用什么替代了？（QueryPipeline——0.14 弃用 DAG）
2. 步骤间怎么连线？（事件类型路由——返回什么事件到哪个 step）
3. AgentWorkflow 三要素？（root_agent/agents/handoff）
4. handoff 的约束？（can_handoff_to 白名单）
5. 人机协同什么时候必须？（高危动作——确认后再执行）

## 9. 本节验收

**验收动作**：① 手写一个"检索 → 合成"双步骤 Workflow 并流式输出；② 两个 Agent 跑通 handoff（调研 → 写报告）；③ 给高危工具加人工确认点——**"事件驱动 + 多 Agent + 人机协同 = 生产级编排"**。

> 🎯 **核心要点**：**Workflows 事件驱动（QueryPipeline 弃用）**；**AgentWorkflow = root_agent + handoff + ctx.store + 统一记忆**；**人机协同是生产分水岭**；**生产三件套（持久化/观测/护栏）**。

---

**上一模块**：[07-知识图谱：PropertyGraphIndex.md](./07-知识图谱：PropertyGraphIndex.md) / **下一模块**：[09-记忆与可观测性.md](./09-记忆与可观测性.md)
