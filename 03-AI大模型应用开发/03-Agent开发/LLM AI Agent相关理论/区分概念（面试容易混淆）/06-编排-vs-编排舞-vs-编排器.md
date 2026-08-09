# 编排 vs 编排舞 vs 编排器

> 辨析篇：三个"编排"概念——**模式 vs 模式 vs 组件**。深潜见 [Multi-Agent 02 篇](..%2F..%2FAgent组件%2F进阶工程化组件（生产环境必备）%2FMulti-Agent协作组件（多智能体）%2F02-架构拓扑：编排vs编排舞.md)。

## 1. 三概念定位

| 概念 | 是什么 | 一句话 |
|---|---|---|
| 编排（Orchestration） | 组织模式 | 中央控制器分解目标按序调用 |
| 编排舞（Choreography） | 组织模式 | 去中心化——Agent 订阅事件自主行动 |
| 编排器（Orchestrator） | 组件/工具 | 实现编排的运行时（Temporal/LangGraph Platform 等） |

> 🎯 核心要点：**前两个是"模式"（怎么组织），第三个是"实现"（用什么做）**——面试答"编排 vs 编排舞"讲模式权衡，答"编排器"讲组件选型；三者不同层。

## 2. 混淆点 1：编排 vs 编排舞

| 维度 | 编排 | 编排舞 |
|---|---|---|
| 控制 | 中央控制器 | 无中央（事件自治） |
| 审计 | ✅ 天然轨迹 | ❌ 需补追踪 |
| 调试 | 单一调试点 | 分布式排查 |
| 瓶颈 | 控制器瓶颈/单点 | 无瓶颈 |
| 弹性 | 控制器挂全停 | 成员自治存活 |
| 适用 | 需审计/合规步骤 | 需速度/弹性步骤 |

> 💡 2026 生产共识：**不是二选一是混合**——"需审计与合规签字的步骤用编排、需速度与弹性的步骤用编排舞"；纯编排 = 单点脆弱，纯编排舞 = 无法审计。

## 3. 混淆点 2：编排 vs 编排器

| 维度 | 编排（模式） | 编排器（组件） |
|---|---|---|
| 抽象层 | 组织方式（概念） | 执行引擎（实现） |
| 例子 | hub-and-spoke 模式 | Temporal/Agent Executor |
| 决策 | 拓扑选型（Multi-Agent 02 篇） | 引擎选型（Orchestrator 07 篇） |
| 面试问法 | "你用什么模式组织" | "你用什么引擎跑" |

> ⚠️ 2026 常见混淆：**"编排器"常被当"模式"讨论**——面试问"编排"先澄清问的是模式还是引擎；Temporal 是编排器（引擎）不是编排（模式）——"Temporal 管持久执行，不决定你怎么组织 Agent"。

## 4. 混淆点 3：编排 vs 编排舞的衍生概念

| 概念 | 归属 | 说明 |
|---|---|---|
| DAG 约束 | 编排的实现纪律 | 有向无环防循环（执行者不互调） |
| 事件总线 | 编排舞的实现机制 | 订阅/发布解耦 |
| 联邦编排 | 编排舞的演进 | 无主 Agent 动态结盟（前沿） |
| 层级监督 | 编排的拓扑变体 | supervisor 树 |

> 🎯 一句话：**"编排与编排舞是两极，生产取中间带"**——层级监督/DAG/事件总线都是中间态的工程实现；面试答"混合 + 中间态"比答"选一个"高级。

## 5. 面试速记

| 问题 | 一句话答案 |
|---|---|
| 三概念关系？ | 模式（编排/编排舞）vs 组件（编排器） |
| 编排 vs 编排舞？ | 中央控制（审计）vs 事件自治（弹性） |
| 生产共识？ | 混合——审计步骤编排、弹性步骤编排舞 |
| 编排器是什么？ | 实现编排的引擎（Temporal/Agent Executor） |
| Temporal 是模式吗？ | 不是——引擎，不决定组织方式 |
| DAG？ | 编排的实现纪律（防循环） |
| 联邦编排？ | 编排舞演进——无主动态结盟 |
| 纯编排问题？ | 单点脆弱 |
| 纯编排舞问题？ | 无法审计 |
| 与 Multi-Agent 02 关系？ | 拓扑深潜 |

---

**下一模块**：[07-Agent vs 多智能体 vs 子 Agent](07-Agent-vs-多智能体-vs-子Agent.md)　**返回总览**：[00-区分概念总览](00-区分概念总览.md)

## 参考来源

- [LLM-Based Multi-Agent Orchestration: A Survey（MDPI）](https://www.mdpi.com/1999-5903/18/6/326)
- [The Architecture of AI Agents（All Things Open 2026）](https://2026.allthingsopen.org/sessions/the-architecture-of-ai-agents-patterns-protocols-and-pitfalls-in-2026)
- [LangGraph vs Temporal（LangChain）](https://www.langchain.com/resources/langgraph-vs-temporal)
- [When AI Agents Collide（NASSCOM）](https://community.nasscom.in/communities/ai/when-ai-agents-collide-multi-agent-orchestration-failure-playbook-2026)
