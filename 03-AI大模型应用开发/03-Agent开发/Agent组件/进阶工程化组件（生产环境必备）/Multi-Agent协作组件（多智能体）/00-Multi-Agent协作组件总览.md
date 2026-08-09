# Multi-Agent 协作组件知识体系总览

> 定位：Agent 生产工程化组件之「多智能体协作」——从单体 Agent 走向 Agent 群（swarm）的工程：架构拓扑、A2A 协议、任务交接、框架选型、级联安全与轨迹评估。2026 核心共识：**协议标准化（A2A/MCP 分层）是互操作基石；级联失败是主导威胁（~12% 长运行含传播错误）；AutoGen 进入维护模式，MAF 继任**。总览做索引与速查，子主题各一篇。

## 📚 目录

1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [学习路线推荐](#3-学习路线推荐)
4. [核心概念速查](#4-核心概念速查)
5. [2026 生态基准](#5-2026-生态基准)

## 1. 知识体系导图

```text
Multi-Agent 协作组件
├── 01 多 Agent 全景：为什么协作是组件        单体→swarm / 协作代价 / 何时不用
├── 02 架构拓扑：编排 vs 编排舞               orchestration-choreography / DAG / 选型
├── 03 A2A 协议：Agent 间通信标准             Agent Cards / JSON-RPC / 任务生命周期
├── 04 协议栈全景：MCP-A2A-ACP               协议分层 / ACP 创新 / 互操作生态
├── 05 任务分配与交接                         分配语义 / token 乘数 / 交接状态
├── 06 协调机制：群聊-投票-仲裁               group chat / debate / 冲突解决
├── 07 多 Agent 框架选型                      AutoGen 维护 / MAF / CrewAI / LangGraph
├── 08 多 Agent 安全：信任边界与级联          级联失败 / ACI / 内存毒化 / 五结构缺陷
├── 09 多 Agent 评估与观测                    轨迹级评估 / ACIArena / SLO / 分布式追踪
└── 10 生产冲刺：落地清单与面试               12 避坑 / 面试题
```

## 2. 模块导航

| 序号 | 模块 | 核心内容 | 适合人群 |
|---|---|---|---|
| 01 | [多 Agent 全景：为什么协作是组件](01-多Agent全景：为什么协作是组件.md) | 单体→swarm、代价、选型前提 | 全部（地基） |
| 02 | [架构拓扑：编排 vs 编排舞](02-架构拓扑：编排vs编排舞.md) | 混合编排、DAG、拓扑选型 | 架构师 |
| 03 | [A2A 协议：Agent 间通信标准](03-A2A协议：Agent间通信标准.md) | Agent Cards、JSON-RPC、生命周期 | Agent 工程师 |
| 04 | [协议栈全景：MCP/A2A/ACP](04-协议栈全景：MCP-A2A-ACP.md) | 分层栈、ACP 创新、生态 | 架构师 |
| 05 | [任务分配与交接](05-任务分配与交接.md) | 交接语义、token 乘数、状态传递 | Agent 工程师 |
| 06 | [协调机制：群聊-投票-仲裁](06-协调机制：群聊-投票-仲裁.md) | 协作模式、冲突解决 | Agent 工程师 |
| 07 | [多 Agent 框架选型](07-多Agent框架选型.md) | 四框架对比、benchmark、决策 | 选型决策者 |
| 08 | [多 Agent 安全：信任边界与级联](08-多Agent安全：信任边界与级联.md) | 级联失败、ACI、五结构缺陷 | 架构师/安全 |
| 09 | [多 Agent 评估与观测](09-多Agent评估与观测.md) | 轨迹评估、ACIArena、SLO、追踪 | 评测关注者 |
| 10 | [生产冲刺：落地清单与面试](10-生产冲刺：落地清单与面试.md) | 12 避坑、面试题、落地清单 | 面试/上线前 |

## 3. 学习路线推荐

| 路线 | 路径 | 目标 |
|---|---|---|
| 入门（1 天） | 01 → 02 → 07 → 10 | 能判断要不要上多 Agent、选框架 |
| 进阶（1 周） | 01-03 → 05 → 07 → 10 | 能做 A2A 交接与任务编排 |
| 高级（2 周） | 全量 + 04 → 06 → 08-09 | 能治理级联安全与轨迹评估 |

## 4. 核心概念速查

| 概念 | 一句话定义 |
|---|---|
| Swarm | 小型专业 Agent 群（三站式：Triage 路由 + 窄专家）——2026 生产主流 |
| 编排（Orchestration） | 中央控制器分解目标按序调用专家（可审计，有瓶颈） |
| 编排舞（Choreography） | 去中心化：Agent 订阅事件自主行动（快，难审计） |
| DAG 约束 | 有向无环图控制流——防无限循环与循环依赖 |
| A2A 协议 | Agent 间通信开放标准（Linux Foundation，150+ 组织）——"Agent 界的 HTTP" |
| Agent Card | 机器可读"数字名片"（`/.well-known/agent.json`）：身份/能力/约束/安全方案 |
| Task 生命周期 | submitted → working → completed/failed（A2A 任务状态机） |
| Token 乘数效应 | 每次交接触发新 LLM 调用——多 Agent token 是单 Agent 的 3-10x |
| 级联失败 | 上游错误经信任交接传播放大——~12% 长运行含单步评估不标记的错误 |
| ACI | Agent Cascading Injection：被攻破 Agent 借互信传播恶意指令（ACIArena） |
| Tool-chaining | 单个都授权、组合越权的调用链（91% of 847 部署易感） |
| Morris-II 蠕虫 | 零点击提示词级联传播——单点攻破变全群沦陷（OWASP ASI08） |
| 内存毒化 | 单次毒化写入污染所有读共享内存的 Agent（94% 跨会话记忆 Agent 易感） |
| 五结构缺陷 | 协调延迟/上下文碎片/幻觉传播/API drift/审计碎片（SOC 语境） |
| Agentic SLO | 成功率（含人工审计）+ 交接延迟（30 秒阈值）+ 工具调用保真 |
| 断路器监控 | 轻量模型盯"agent tennis"（争论循环）/停滞/礼貌螺旋 |

## 5. 2026 生态基准

> 📅 基准窗口：2026-08。版本与事实以各模块【参考来源】为准。

- **Swarm 取代单体**：生产团队偏好 Triage Agent 路由 + 窄专家（SQL Agent 一个 execute_query、Python Agent 隔离容器）而非一个大模型挂几十个工具；Agent 单次调用无状态、系统跨流程保状态。
- **协议标准化年**：A2A 一周年（150+ 组织、生产部署于供应链/金融/保险/IT 运维、Spec 1.0.0、集成 Azure AI Foundry/AWS Bedrock AgentCore/GCP）；**MCP 管"垂直"（Agent→工具）、A2A 管"水平"（Agent↔Agent）——分层组合是 2026 架构共识**；ACP 新协议推进安全联邦编排（Agent Cards + 混合发现 + 四阶段协商）；IETF 筹备 AIPROTO WG；NIST AI Agent Standards Initiative（2026-02-17）。
- **框架格局剧变**：**AutoGen 进入维护模式**（v0.7.5 2025-09-30 最后版本）——Microsoft Agent Framework（MAF）2026-04 1.0 GA 继任；CrewAI v1.14.4（角色 crews，3-5x token 开销）；LangGraph 1.0 GA（checkpointer + **time-travel 调试**最强）；benchmark：MAF 质量 9.87/93s/~7K tokens 最高，CrewAI 9.66/246s/27.7K tokens，AutoGen 9.63/572s，LangGraph 9.42/506s——**别按 GitHub star 选框架**（AutoGen star 最多却在维护模式）。
- **级联失败是主导威胁**：~12% 长运行包含单步评估从不标记的传播错误（FutureAGI 2026 trace 数据）；工具链越权 91%（847 部署）；跨会话记忆毒化 94%；ACIArena（ACL 2026）1356 用例统一评估级联注入——**拓扑评估不足，需角色设计与交互控制**；单步评估无法检测传播错误，轨迹级评估（轨迹通过率 vs 单步通过率缺口）是检测信号。
- **五结构缺陷编目**（D3 Security）：协调延迟（每次交接序列化/传输/反序列化）、上下文碎片（下游 Agent 消费第三代摘要）、幻觉传播（捏造被下游当 ground truth"洗白"）、API drift（集成维护随 Agent 数倍增）、审计碎片（事故记录需从 N 个日志重建）——多 Agent 固有，非可修补 bug。
- **治理成熟度极低**：87% 组织优先互操作，仅 7-8% 报告成熟 Agent 治理——**零信任是必须**（每次 A2A 调用单独认证授权，mTLS + OAuth2 组合，W3C Trace Context 传播可观测）。
- **与体系分工**：[主流 Agent 范式 07 篇](..%2F..%2F..%2F主流%20Agent%20范式%2F07-多Agent协作范式.md) 讲协作模式概念（群聊/投票/拓扑概览）；本体系深潜"协作组件工程"（协议/交接/框架/安全/评估）；[观测&可观测组件](..%2F观测%26可观测组件%2F00-观测可观测组件总览.md) 管单 Agent 观测，本体系 09 篇管多 Agent 轨迹与级联；[Guardrails](..%2F安全护栏%20Guardrails%2F00-安全护栏Guardrails总览.md) 管单点拦截，本体系 08 篇管信任边界。

---

**下一模块**：[01-多 Agent 全景：为什么协作是组件](01-多Agent全景：为什么协作是组件.md)

## 参考来源

- [LLM-Based Multi-Agent Orchestration: A Survey（MDPI）](https://www.mdpi.com/1999-5903/18/6/326)
- [Beyond Context Sharing: A Unified Agent Communication Protocol (ACP)（arXiv 2602.15055）](https://ar5iv.labs.arxiv.org/html/2602.15055)
- [Agent2Agent vs MCP: 2 Protocols Your 2026 Stack Needs（Beam AI）](https://beam.ai/agentic-insights/agent2agent-vs-mcp-2026-ai-agent-stack)
- [The practical guide to A2A agent framework integrations（Tyk）](https://tyk.io/learning-center/the-practical-guide-to-a2a-agent-framework-integrations/)
- [CrewAI vs LangGraph vs AutoGen 2026（FutureAGI）](https://futureagi.com/blog/crewai-vs-langgraph-vs-autogen-2026/)
- [Best Multi-Agent Frameworks 2026（FutureAGI）](https://futureagi.com/blog/best-multi-agent-frameworks-2026/)
- [ACIArena: Toward Unified Evaluation for Agent Cascading Injection（ACL 2026）](https://aclanthology.org/2026.acl-long.457/)
- [When AI Agents Collide: Multi-Agent Orchestration Failure Playbook（NASSCOM）](https://community.nasscom.in/communities/ai/when-ai-agents-collide-multi-agent-orchestration-failure-playbook-2026)
- [What Is a Cascading Failure?（FutureAGI）](https://futureagi.com/glossary/cascading-failure/)
- [5 Architectural Flaws in Agentic AI SOC Platforms（D3 Security）](https://d3security.com/resources/5-architectural-flaws-agentic-ai-soc/)
