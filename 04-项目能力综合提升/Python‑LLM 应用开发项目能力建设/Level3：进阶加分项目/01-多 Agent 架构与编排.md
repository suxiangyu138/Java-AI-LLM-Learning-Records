# 01 多 Agent 架构与编排

> 第一个加分项：把 Level2 的单 Agent（一个 LangGraph 图）升级为多 Agent 系统——Supervisor 集中路由或 Swarm 对等交接。理解 2026 年多 Agent 的两大主流模式与选型决策，并落地一个可演示的多 Agent 场景。

## 📚 目录

1. [目标与验收](#1-目标与验收)
2. [为什么要多 Agent：单 Agent 的边界](#2-为什么要多-agent单-agent-的边界)
3. [两大模式：Supervisor 与 Swarm](#3-两大模式supervisor-与-swarm)
4. [模式选型决策树](#4-模式选型决策树)
5. [落地：知识库问答的多 Agent 化](#5-落地知识库问答的多-agent-化)
6. [多 Agent 的成本与风险](#6-多-agent-的成本与风险)
7. [常见坑](#7-常见坑)

---

## 1. 目标与验收

本模块的产出：把 Level2 项目升级为多 Agent——一个"主管 + 三个专家"的 Supervisor 架构（文档问答 Agent、工具查询 Agent、对话 Agent），路由准确率与单 Agent 基线对比记录。验收标准：**能画出多 Agent 架构图（节点、交接、回退）**；**能讲清 Supervisor 与 Swarm 的适用边界**；**评估数据证明多 Agent 在目标场景优于单 Agent**（或诚实记录"未优于，原因是什么"——后者同样是加分项，说明你有评估意识）。版本基线（2026-08）：LangGraph 1.x（`langgraph-supervisor` 工厂可用）、DeepSeek V4 系列做角色分工（flash 做执行、pro 做路由/审查）。

## 2. 为什么要多 Agent：单 Agent 的边界

Level2 的单 Agent 图（检索→生成→工具）已经能回答文档问题，为什么要拆多 Agent？三个动因：**职责混杂**——一个 Agent 既要管文档检索、又要调实时工具、还要寒暄对话，system 提示词越长越混乱（"提示词即代码"的复杂度爆发），拆开后的每个 Agent 提示词短而专注；**模型错配**——路由与审查需要强推理（贵模型），执行可以便宜快（便宜模型），混在一个 Agent 里只能取中间档；**可演进性**——新场景（加一个"邮件 Agent"）在单 Agent 里是加提示词，在多 Agent 里是加一个节点，独立演进互不干扰。**2026 年的产业信号**：Gartner 预测 2026 年底 40% 的企业应用将包含任务型 Agent（2025 年初还不到 5%）——多 Agent 已经是从论文走向生产的成熟模式，面试官对它的追问深度远超单 Agent。**反面提醒**：单 Agent 能解决的不上多 Agent——"一个提示词写得好好的 Agent 胜过路由一团糟的多 Agent"，这是社区 2026 年的共识第一句。

## 3. 两大模式：Supervisor 与 Swarm

多 Agent 的两大主流模式，对应两种协作哲学：

**Supervisor（主管模式）**——一个中央路由 Agent（supervisor）接收任务、拆解、分发给专业 worker Agent（通过 handoff 工具调用），worker 完成后结果回流 supervisor 汇总。特点：**路由集中**（每个任务交给谁有明确决策，全程可审计——trace 里能看到每次路由的推理）；**角色清晰**（worker 只懂自己的领域）；**典型规模 3-8 个 Agent**，延迟中等（多一跳）。2026 企业部署的主流形态：**强模型做 supervisor（如 pro 档），快模型做 worker（如 flash 档）**——成本与能力的最优配比。

**Swarm（蜂群模式）**——没有中央调度，Agent 之间对等交接：当前 Agent 判断"这不是我的领域"，直接通过 handoff 工具把对话转给另一个 Agent（LangGraph 里是 Command 转移）。特点：**无单点瓶颈**（少一跳，LLM 调用量约省 40%）；**自然语言驱动的路由**（交接由模型自主判断）；**容错分散**；代价是**可预测性低、调试难**（路由决策散落在每个 Agent 里）。适合开放对话场景（客服、闲聊助手），典型规模 2-15 个 Agent。

两者不是竞争是选择：**结构化任务流（职责边界清晰）用 Supervisor，开放对话（边界模糊、靠上下文判断）用 Swarm**；复杂层级（>15 Agent）用分层（Hierarchical）——CEO → 经理 → 员工的三层树（2026-03 出现的 "Paperclip" 模式）。

## 4. 模式选型决策树

选型决策树（2026 实践指南，写入项目文档作为选型记录）：

| 约束 | 选择 | 理由 |
|---|---|---|
| 路由准确性 > 延迟 | Supervisor | 集中路由节点，实测准确率约 94% |
| 延迟是首要约束 | Swarm | 直接交接，约省 40% LLM 调用 |
| 领域边界清晰 | Swarm | 交接干脆，极少误路由 |
| 领域边界模糊 | Supervisor | 专职路由器化解重叠 |
| 少于 3 个领域 | 不做多 Agent | 单 Agent 更简单 |
| 需要集中审计 | Supervisor | 所有路由决策可见 |
| 嵌套团队结构 | 分层（Hierarchical） | 子图组合，逐层委派 |

两个决策原则：**从 Supervisor 起步**（路由显式、审计完整、好调试，90% 的场景它够用；跑通了再加 Swarm 元素）；**先单 Agent 后多 Agent**（本层顺序与 Level2 一致——单 Agent 没跑通就拆多 Agent，等于放大一个没解决的问题）。**"不做多 Agent"也是合法决策**——决策记录里写明"评估后 3 个领域单 Agent 足够"，比盲目上多 Agent 专业得多。

## 5. 落地：知识库问答的多 Agent 化

把 Level2 项目升级为 Supervisor 架构（worker 复用 Level2 的节点能力，新增的是路由层）：

```python
from langgraph_supervisor import create_supervisor
from langgraph.prebuilt import create_react_agent

# 三个 worker：复用 Level2 的检索与工具能力
doc_agent = create_react_agent(llm_worker, tools=[retrieve_tool, rerank_tool],
                               prompt="你是文档问答专家，只依据检索内容回答，可调用检索工具")
tool_agent = create_react_agent(llm_worker, tools=[weather_tool, time_tool],
                                prompt="你是实时信息专家，负责天气、时间等实时查询")
chat_agent = create_react_agent(llm_worker, tools=[],
                                prompt="你是闲聊助手，寒暄与通用对话")

# Supervisor：强模型做路由，按领域分派
supervisor = create_supervisor(
    agents=[doc_agent, tool_agent, chat_agent],
    model=llm_supervisor,          # 用更强的模型做路由
    prompt="你是任务主管：文档问题→文档专家；实时数据→信息专家；寒暄→闲聊助手",
)
app = supervisor.compile(checkpointer=sqlite_checkpointer)   # 检查点复用 L2 05
```

落地的三个设计点：**worker 复用 Level2 组件**（检索/工具/生成是现成的，多 Agent 只新增"路由层"——这不是重写，是重构）；**supervisor 的 prompt 是路由质量的决定因素**（写清"什么情况找谁"，模糊的路由 prompt 直接导致误派——路由准确率 94% 的前提是描述清晰）；**检查点与审计**（supervisor 模式的最大工程红利：每次路由决策都在 trace 里，出问题能回放"为什么派给了它"）。验收实验：准备 30 个混合问题（文档类/实时类/闲聊类各 10 个），对比单 Agent 与多 Agent 的正确率与延迟——**这个对比表就是本模块的量化产出**。

## 6. 多 Agent 的成本与风险

多 Agent 不是免费的午餐，先算账再动手：**成本 3-10 倍**——路由跳数、worker 多轮工具调用、可能的重试，token 消耗远超单 Agent（2026 实践数据：Agentic 循环比经典 RAG 多耗 3-10 倍 token）。本项目的成本控制三板斧：**路由模型降档**（supervisor 用 pro、worker 用 flash——路由是"分类任务"，flash 级足够，只有复杂推理场景才上 pro）；**工具调用上限**（每个 worker 的 max_iterations 硬性限制，Level1 07 的护栏在 worker 层逐个生效）；**缓存与响应缓存**（03 篇的 prompt caching 对多 Agent 收益最大——路由 prompt 与 worker 前缀高度重复）。**风险清单**：**路由漂移**（supervisor 把问题派错——加"不确定就返回澄清"的逃生分支）；**级联失败**（worker 超时整个流程卡住——每层超时 + 降级策略：supervisor 跳步给默认回答，Swarm 交回上一 Agent）；**调试地狱**（多 Agent 的 trace 是单 Agent 的几倍长——用 LangSmith/LangGraph Studio 的每步回放，08 篇的 trace_id 在 worker 内也要贯穿）。

## 7. 常见坑

**路由 prompt 含糊**：worker 描述不清导致误派——按"做什么/何时用/边界"三段式写每个 Agent 的描述（Level1 07 的工具描述规范同样适用）。

**所有 Agent 共用一种模型**：成本与能力的双重浪费——supervisor 强 worker 弱的组合是 2026 标准姿势。

**共享工具冲突**：多个 worker 都挂检索工具，supervisor 无法区分——工具按 worker 隔离（doc_agent 只挂检索，tool_agent 只挂实时工具），职责边界的物理隔离。

**状态在 Agent 间丢失**：worker 完成任务不把结果写回共享 State——统一走 State 流转（Level2 05 的 State 纪律在多 Agent 下加倍重要），检查点才能完整回放。

**无限交接**：Swarm 里 A→B→A 循环——handoff 次数上限 + 兜底 Agent（回到 supervisor），像 Level1 07 的 max_iterations 一样是硬护栏。

**多 Agent 当卖点但不评估**：上了多 Agent 拿不出对比数据——本模块的验收实验必做，数字不理想就把架构回退单 Agent 并记录原因——**"评估后回退"也是工程决策，不是失败**。

> 🎯 **核心要点**：多 Agent 的本质是**把"一个全能提示词"拆成"一组专业提示词 + 一个路由大脑"**——Supervisor 管清晰分工、Swarm 管开放交接、分层管规模。评估对比与成本控制是它与单 Agent 的分水岭：**没有对比数据与成本护栏的多 Agent，是技术表演；有这两者的，才是工程加分项**。

---

**下一模块**：[02 Agentic RAG 与高级检索](./02-Agentic%20RAG%20与高级检索.md) | **返回总览**：[Level3 总览](./00-Level3%20进阶加分项目%20总览.md)

【参考来源】
- [Multi-Agent Orchestration Patterns: Supervisor vs Swarm vs Hierarchical](https://qubittool.com/blog/multi-agent-orchestration-patterns)
- [LangGraph Supervisor 文档](https://langchain-ai.github.io/langgraph/concepts/multi_agent/)
- [Building Real-World Agentic RAG Systems](https://learned-memory.kit.com/posts/building-real-world-agentic-rag-systems)
- [Engineering Multi-Agent Workflows with LangGraph（2026）](https://www.thriftbooks.com/w/engineering-multi-agent-workflows-with-langgraph-architectures-and-patterns-for-production-deployment-in-complex-llm-systems-the-professional-ai-agent-systems-engineering-series_miro-thalden/58238153/)
