# 02 多 Agent 编排范式与模式：Supervisor / Swarm / Handoff / Router

> 定位：范式篇——多 Agent 系统的五种编排模式（Supervisor、Swarm、Handoff、Router、层级）的机制、适用边界与防失控设计；先懂范式再选框架（2026-08 基准）

## 📚 目录

1. [范式总览：五模式对照](#1-范式总览五模式对照)
2. [Supervisor：中心化编排](#2-supervisor中心化编排)
3. [Swarm：去中心化移交](#3-swarm去中心化移交)
4. [Handoff 链：顺序流水线](#4-handoff-链顺序流水线)
5. [Router：分类分发](#5-router分类分发)
6. [层级网络：Supervisor 的嵌套](#6-层级网络supervisor-的嵌套)
7. [其他模式：群聊 / 辩论 / 评审](#7-其他模式群聊--辩论--评审)
8. [模式选择决策树](#8-模式选择决策树)
9. [防失控设计：多 Agent 的护栏](#9-防失控设计多-agent-的护栏)
10. [核心要点](#10-核心要点)

## 1. 范式总览：五模式对照

| 模式 | 控制流 | 适合规模 | 典型框架实现 | 一句话 |
|------|--------|:---:|------------|--------|
| Supervisor | 中心化（hub-and-spoke） | 3-10 Agent | LangGraph supervisor、CrewAI hierarchical | 一个 orchestrator 路由分发 |
| Swarm | 去中心化（peer-to-peer） | 5-15 Agent | LangGraph swarm 包、OpenAI Handoffs | Agent 之间直接移交 |
| Handoff 链 | 顺序链式 | 2-5 Agent | CrewAI sequential、LangGraph 链 | 任务依次传递 |
| Router | 分类分发 | 1-3 Agent | LangGraph 条件边 | 先分类再派给专精 Agent |
| 层级网络 | 多级中心化 | 10-30+ Agent | LangGraph 嵌套 | Supervisor 嵌套 Supervisor |

> 🎯 **范式先行**：先确定控制流形态，再选框架——框架的差异主要就是"如何表达这几种模式"。

## 2. Supervisor：中心化编排

```text
         ┌─────────────────────────┐
         │  Supervisor（编排者）     │
         │  理解任务 → 路由 → 汇总   │
         └──────┬──────┬──────┬────┘
                │      │      │
           ┌────▼─┐ ┌─▼────┐ ┌▼─────┐
           │Worker1│ │Worker2│ │Worker3│
           │ 研究   │ │ 写作  │ │ 审查  │
           └───────┘ └──────┘ └──────┘
```

| 维度 | 说明 |
|------|------|
| 机制 | 单个 orchestrator 接收用户输入，决定哪个子 Agent 最合适，委派任务 |
| 优点 | 路由可追踪可调试；适合任务边界清晰的结构化工作流 |
| 缺点 | 编排者是单点瓶颈；编排提示词复杂 |
| 2026 实践 | 企业最常见模式；高推理模型（如 Opus 级）做编排，快速廉价模型（Haiku 级）做 Worker |
| 典型场景 | 客服中心（分诊→售后→退款）、内容流水线（研究→写作→审查） |

**2026 关键变化**：LangGraph 官方弃用 `langgraph-supervisor` 包，推荐 **subagents 模式**——`create_agent` 作为编排者，Worker 包装为 `@tool` 函数（`subagent.invoke(...)`），编排者通过工具调用子 Agent（详见 03 篇）。

## 3. Swarm：去中心化移交

```text
  Agent A ──handoff──▶ Agent B ──handoff──▶ Agent C
     ▲                    │                    │
     └────────handoff─────┘                    │
     （检测到已移交过 → 拒绝循环）               │
                                               ▼
                                      人工升级（dead-end/超预算）
```

| 维度 | 说明 |
|------|------|
| 机制 | 无中心协调者；每个 Agent 携带 handoff 工具，任务超出自身领域时把对话上下文移交给另一个专精 Agent |
| 优点 | 灵活；适合"上下文决定该谁接"的开放式对话 |
| 缺点 | 控制流不可预测；**移交循环风险** |
| 实现 | LangGraph `create_handoff_tool`/`Command` 转交、`@langchain/langgraph-swarm`（createSwarm）、OpenAI Agents SDK Handoffs 原生 |
| 2026 细节 | npm `@langchain/langgraph-swarm` v1.0.2 带 "last active agent" 记忆——多轮对话回到正确专精 Agent |

**Swarm 生产护栏**（2026 参考实现标准）：
1. **循环检测**：拒绝移交给已在会话 trace 中的 peer
2. **死胡同检测**：无 peer 匹配时升级
3. **Hop 预算**：`MAX_HOPS` 兜底上限，防无限弹跳
4. **人工升级**：循环/死胡同/预算耗尽 → 交给人

## 4. Handoff 链：顺序流水线

```text
任务 ──▶ Agent1 ──▶ Agent2 ──▶ Agent3 ──▶ 完成
        （解析）    （处理）    （输出）
```

| 维度 | 说明 |
|------|------|
| 机制 | 任务按固定顺序在 Agent 间传递，每个 Agent 处理一段 |
| 优点 | 最简单、可预测、易测试 |
| 缺点 | 无回退；串行慢；中间失败难恢复 |
| 实现 | CrewAI sequential process、LangGraph 线性链 |
| 场景 | 数据清洗→分析→报告、翻译链、审批链 |

## 5. Router：分类分发

```text
输入 ──▶ 分类器（一次 LLM 调用）──▶ 分支1：售后 Agent
                             ├──▶ 分支2：技术 Agent
                             └──▶ 分支3：销售 Agent
```

| 维度 | 说明 |
|------|------|
| 机制 | 先用一次分类调用决定走哪条路，再交给专精 Agent（或工作流） |
| 优点 | 成本低（一次分类调用）；路由可控可测 |
| 缺点 | 分类错误即路由错误；无二次协商 |
| 实现 | LangGraph conditional_edges、任何框架的条件分支 |
| 场景 | 工单分诊、意图识别分发——"用 Router 还是 Supervisor"取决于是否需要编排者继续参与后续决策 |

## 6. 层级网络：Supervisor 的嵌套

```text
              Root Supervisor
              │         │
        ┌─────▼───┐   ┌▼────────────┐
        │ 研究主管  │   │ 交付主管     │
        │ Supervisor│   │ Supervisor  │
        │   │   │   │   │   │   │     │
        │  A   B   C   │   D   E   F  │
        └─────────┘   └──────────────┘
```

| 维度 | 说明 |
|------|------|
| 机制 | Supervisor 嵌套 Supervisor，逐级下钻 |
| 适合 | 10-30+ Agent 的大型组织型编排；学习型系统（Master Agent 生成子 Agent） |
| 实现 | LangGraph 嵌套（子 supervisor 作为 tool 包装）、Agent Framework 层级 |
| 注意 | 层级越深，延迟与 token 成本越高；每级都要有明确的路由边界 |

## 7. 其他模式：群聊 / 辩论 / 评审

| 模式 | 机制 | 适合 | 成本警示 |
|------|------|------|---------|
| 群聊（Group Chat） | 多个 Agent 围绕一个对话轮流发言 | 头脑风暴、多方协商 | AutoGen 正统；单交互 20+ LLM 调用 |
| 辩论（Debate） | 多 Agent 各自立场互辩后收敛 | 事实核查、决策对抗 | 收敛性差，成本高 |
| 评审（Reviewer） | 生产 Agent + 评审 Agent 双角色 | 内容质量门禁 | 多一轮延迟 |

> 💡 这些模式在 2026 生产中使用率低于四大主流模式（Supervisor/Swarm/Handoff/Router）——作为范式了解即可，落地先想清楚收益。

## 8. 模式选择决策树

```text
任务结构清晰、角色分工明确？
├─ 是，且边界固定 → Router（简单）或 Supervisor（需要编排者全程参与）
├─ 是，且顺序流水线 → Handoff 链
├─ 否，上下文决定谁接 → Swarm（需要强护栏）
└─ 规模 10+，组织型 → 层级网络

规模速查：2-5 顺序 → Handoff；3-10 分工 → Supervisor；
          5-15 开放对话 → Swarm；10-30+ → 层级
```

> 🎯 **经验法则**：90% 的场景用 **Supervisor**（或 Router）就够——Swarm 的灵活性只在"无法预知谁该接"的开放式对话中才值得；层级网络只在大规模组织型场景用。

## 9. 防失控设计：多 Agent 的护栏

| 护栏 | 说明 | 实现 |
|------|------|------|
| 循环检测 | 拒绝重复移交 | trace 集合 + handoff 前检查 |
| Hop 预算 | 最大移交次数 | MAX_HOPS 常量 |
| 死胡同升级 | 无 Agent 可接时升级 | 兜底 Router/人工节点 |
| 人工升级 | 循环/死胡同/预算耗尽 → 交给人 | 终端 Human 节点（HITL） |
| Token 预算 | 每 Agent 每次调用成本上限 | 框架层监控 + 告警 |
| 状态检查点 | 每节点持久化，可回滚 | checkpointer（LangGraph/CrewAI 2026 均支持） |

> ⚠️ **护栏是必需的不是可选的**：没有护栏的 Swarm 在生产中会以"无限循环烧钱"的方式失败——参考实现（Agentic-AI-Orchestration ch07）把四件套（循环检测/死胡同/hop 预算/人工升级）作为 swarm 的标准配置。

## 10. 核心要点

> 🎯 **核心要点**：
> 1. 五种模式：Supervisor（中心化，3-10）、Swarm（去中心化，5-15）、Handoff 链（顺序，2-5）、Router（分类分发）、层级网络（10-30+）
> 2. 90% 场景用 Supervisor 或 Router 就够；Swarm 只在开放式对话值得，且必须配四件套护栏
> 3. 2026 变化：LangGraph 弃用 supervisor 包 → subagents 模式（子 Agent 包装为 @tool）
> 4. 先定范式再选框架——框架差异 = 如何表达这些模式

---

**上一模块**：[01 编排全景与选型](01-多%20Agent%20编排全景与选型：2026%20框架地图.md)　**下一模块**：[03 LangGraph](03-LangGraph：多%20Agent%20生产编排标杆.md)　**返回总览**：[00 总览](00-总览：多%20Agent（Multi‑Agent）框架知识体系.md)

## 【参考来源】

- [LangGraph Multi-Agent Patterns (langgraph-101 / DeepWiki)](https://deepwiki.com/langchain-ai/langgraph-101/6-utilities)
- [Migrate from langgraph-supervisor - LangChain Docs](https://docs.langchain.com/oss/python/migrate/langgraph-supervisor)
- [@langchain/langgraph-swarm (npm)](https://www.npmjs.com/package/@langchain/langgraph-swarm)
- [Agentic-AI-Orchestration ch07: Swarm AI Architecture](https://github.com/zahurul-islam/Agentic-AI-Orchestration/blob/main/ch07-swarm-ai-architecture/starter/support_swarm.py)
- [ai-system-design-guide: Multi-Agent Orchestration](https://github.com/ombharatiya/ai-system-design-guide/blob/main/07-agentic-systems/04-multi-agent-orchestration.md)
- [Practical Multi-Agent AI Systems (Wiley, 2026-10)](https://www.wiley.com/en-cn/shop/general-introductory-computer-science/practical-multi-agent-ai-systems-how-to-architect-build-and-scale-next-generation-ai-systems-that-work-in-the-real-world-p-9781394418497)
