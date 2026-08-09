# 多 Agent 框架选型

> 框架 = 多 Agent 系统的"地基"。2026 最大变化：**AutoGen 进入维护模式**（v0.7.5 2025-09-30 最后版本）——Microsoft Agent Framework（MAF）2026-04 1.0 GA 继任；**CrewAI（角色 crews）vs LangGraph（状态图）**成为两大主流心智模型。本章给四框架深潜与选型决策。

## 1. 2026 格局剧变

| 框架 | 状态 | 关键事实 |
|---|---|---|
| AutoGen | **维护模式** | v0.7.5（2025-09-30）最后版本；新项目不推荐；迁移目标 MAF |
| Microsoft Agent Framework | 继任者 | 2026-04 1.0 GA + LTS 承诺；Python/C# 双语言 |
| CrewAI | 活跃 | v1.14.4（2026-04）；角色 crews + Process |
| LangGraph | 活跃 | 1.0 GA（2025-10 随 LangChain v1）；StateGraph + checkpointer |

> ⚠️ **别按 GitHub star 选框架**——AutoGen ~58k star 最多却在维护模式；star 数滞后于维护状态变化（2026 多来源警示）。

## 2. 四框架对比

| 维度 | CrewAI | LangGraph | AutoGen | MAF |
|---|---|---|---|---|
| 心智模型 | 角色 crew + Process | StateGraph 节点/边 | 对话 Agent + GroupChat | 对话/状态混合 |
| 持久化 | 记忆存储（checkpoint 新） | **checkpointer + time-travel**（最强） | 仅日志 | 状态持久化 |
| 分布式 | 无（单进程） | LangGraph Platform（可选） | Core gRPC | 云托管 |
| 语言 | Python | Python/TS | Python/.NET | Python/C# |
| 学习曲线 | 低（~40-80 行） | 陡（~120 行） | 中 | 中 |

## 3. Benchmark 数据（2026，同一 3-Agent 研究任务）

| 框架 | 质量 | 延迟 | Token | 特征 |
|---|---|---|---|---|
| MAF | **9.87** | **93s** | **~7,006** | 方差最低 |
| CrewAI | 9.66 | 246s | ~27,684 | 3-5x 单 Agent token |
| AutoGen | 9.63 | 572s | ~10,793 | 最慢最贵 |
| LangGraph | 9.42 | 506s | ~8,823 | 图控制流 |

> 🎯 核心要点：**质量差异小（9.0+ 都优秀），差异在速度（6x 差距）、token 效率（4x 差距）与一致性**——选框架看工程指标不看质量分数；CrewAI 的 27.7K tokens 说明角色抽象有 token 税。

## 4. LangGraph：生产状态机标杆

| 优势 | 机制 |
|---|---|
| Time-travel 调试 | 回退任意 checkpoint、改状态、重放——**"第一次生产失败就值回票价"** |
| 人类介入 | checkpoint 审批（执行前批准、编辑中间状态） |
| 持久执行 | 每节点存状态 |
| 1.0 GA | 稳定性承诺 + Platform 托管 |

| 短板 | 说明 |
|---|---|
| 学习曲线 | TypedDict 状态 + 条件边——适度工作流 ~120 行 vs 替代 40-80 行 |
| 内存 | 高 |
| 锁定 | 生态耦合 |

> ⚠️ **checkpointer ≠ durability**：LangGraph/MAF/LlamaIndex 都持久化状态，但**都不检测运行死亡**——持久执行层可能仍需（2026 共识）。

## 5. CrewAI：快速原型标杆

| 优势 | 机制 |
|---|---|
| 快速 MVP | 最少样板、人可读配置（非技术干系人可读） |
| Process 抽象 | 顺序/层级两种流程 |
| 角色化 | Agent/Task/Crew 心智直观 |

| 短板 | 说明 |
|---|---|
| Token 开销 | crews 消耗单 Agent 的 3-5x（benchmark 27.7K） |
| 伪协作 | manager-worker 常是串行非真协作 |
| 生产路径 | **Vercel 指导：Flows 上生产，不在 Crews 上** |

## 6. 选型决策

```text
框架选型：
├─ 状态机/持久/时移调试必需（长流程/分支/重试/HITL）
│   └─ LangGraph（企业/合规/关键任务）
├─ 角色化快速原型/MVP
│   └─ CrewAI（生产用 Flows）
├─ Microsoft/.NET 栈
│   └─ MAF（2026-04 1.0 GA）
├─ 研究/对话实验（存量）
│   └─ AutoGen（维护模式认知下使用）
└─ 框架无关
    └─ 协议层组合（A2A/MCP，03/04 篇）
```

| 决策问题 | 答案 |
|---|---|
| 状态与持久必需？ | LangGraph |
| 快速出活？ | CrewAI |
| MS 栈？ | MAF |
| 新项目？ | 不选 AutoGen |
| 跨框架？ | 协议层（A2A/MCP） |

> 🎯 核心要点：**没有最佳框架**——LangGraph 管状态化可控生产编排、CrewAI 管角色化快速原型、MAF 管 MS 生态；跨框架互操作走协议层（A2A/MCP），不让框架锁死架构。

## 7. 选型常见错误

| 错误 | 真相 |
|---|---|
| 按 star 选 | AutoGen star 最多却维护模式——看维护状态 |
| 低估调试 | time-travel 调试"第一次生产失败就值回票价" |
| 多 Agent 至上 | 单 Agent+好工具通常赢（01 篇） |
| checkpointer=durability | 不检测运行死亡——持久执行层仍可能 |
| 跳过评估选型 | 运行时决策与评估决策独立——OTel 集成 + 中立评估层 |
| Crews 上生产 | Flows 上生产（Vercel 指导） |

## 8. 面试速记

| 问题 | 一句话答案 |
|---|---|
| AutoGen 状态？ | 维护模式（v0.7.5 最后）——MAF 继任 |
| MAF？ | 2026-04 1.0 GA + LTS——MS 栈推荐 |
| LangGraph 强项？ | checkpointer + time-travel 调试 + 持久执行 |
| CrewAI 强项？ | 角色化快速原型——token 3-5x |
| benchmark 数据？ | MAF 9.87/93s/7K tokens 最高；CrewAI 27.7K 最耗 |
| 别按什么选？ | GitHub star——维护状态才是信号 |
| checkpointer≠？ | durability——不检测运行死亡 |
| Crews vs Flows？ | Flows 上生产 |
| 跨框架？ | 协议层（A2A/MCP）不让框架锁死 |
| 学习曲线？ | LangGraph 陡（120 行）CrewAI 缓（40-80 行） |
| 质量差异？ | 9.0+ 都优秀——差异在速度/token/一致性 |
| 新项目选什么？ | LangGraph/CrewAI/MAF——不选 AutoGen |

---

**下一模块**：[08-多 Agent 安全：信任边界与级联](08-多Agent安全：信任边界与级联.md)　**返回总览**：[00-Multi-Agent 协作组件总览](00-Multi-Agent协作组件总览.md)

## 参考来源

- [CrewAI vs LangGraph vs AutoGen 2026（FutureAGI）](https://futureagi.com/blog/crewai-vs-langgraph-vs-autogen-2026/)
- [Best Multi-Agent Frameworks 2026（FutureAGI）](https://futureagi.com/blog/best-multi-agent-frameworks-2026/)
- [Comparing Open-Source AI Agent Frameworks in 2026（FutureAGI）](https://futureagi.com/blog/oss-agent-frameworks-2026/)
- [How to Choose AI Agent Frameworks in 2026?（Vercel）](https://vercel.com/i/ai-agent-frameworks)
- [AI Agent Frameworks Compared: LangChain vs. AutoGen vs. CrewAI（[x]cube）](https://xcubelabs.com/blog/ai-agent-frameworks-compared-langchain-vs-autogen-vs-crewai-for-enterprise-use-cases)
