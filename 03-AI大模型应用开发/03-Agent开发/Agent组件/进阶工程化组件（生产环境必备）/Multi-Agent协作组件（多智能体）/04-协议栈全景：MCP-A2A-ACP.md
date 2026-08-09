# 协议栈全景：MCP/A2A/ACP

> 协议栈 = 多 Agent 互操作的"TCP/IP 时刻"——分层协议解决不同层的问题。2026 现状：**MCP（工具访问）→ A2A（Agent 协作）→ ACP（安全联邦编排）**三层演进 + LSP 补符号理解；IETF/NIST 入场标准制定。本章给协议栈全景与选型。

## 1. 协议分层栈

```text
多 Agent 协议栈（类比互联网分层）：
  LSP  —— 符号/代码理解（语言服务器协议）
  MCP  —— 工具与数据访问（低层标准，垂直）
  A2A  —— Agent 间通信与编排（高层标准，水平）
  ACP  —— 安全/联邦/自主协商（演进方向）
```

| 层 | 解决 | 状态 |
|---|---|---|
| LSP | 代码符号理解 | 成熟（IDE 生态） |
| MCP | Agent→工具/数据 | 2026 事实标准（10 SDK） |
| A2A | Agent↔Agent 协作 | 1.0.0 正式版（03 篇） |
| ACP | 安全联邦编排 | 研究/演进（arXiv 2602.15055） |

> 🎯 核心要点：**协议分层与互联网同构**——没有单一协议解决所有问题；2026 架构 = MCP 管工具 + A2A 管协作 + ACP 演进安全（还有 ANP/ERC-8004 等补充身份/发现/声誉）。

## 2. ACP：安全联邦编排的推进

| 创新 | 机制 |
|---|---|
| Agent Cards | 机器可读数字名片：身份（DID）/能力/约束/信任分/接口端点——无人工语义发现 |
| 混合发现 | 本地广播发现（mDNS 类）私有网络 + 联盟链 DHT 全局发现（不可变/透明/无单点） |
| 四阶段协商 | Inquiry（PROBE）→ Proposal（BID）→ Agreement（COMMIT 密码学哈希"软契约"）→ Execution & Settlement（声誉更新） |
| 信任机制 | 去中心化标识符（DID）+ 可验证凭证（VC）+ 签名消息（不可否认） |

> 💡 ACP 的意义：**把"协商"变成协议的一部分**——Agent 不是"被指派"，而是"可发现、可谈判、可签契约"；声誉系统让可靠 Agent 浮现。前沿方向，2026 关注其演进。

## 3. 标准治理生态（2026）

| 组织 | 动作 |
|---|---|
| IETF | 筹备 AIPROTO WG（Agent 互操作，以 A2A 为基线） |
| NIST | AI Agent Standards Initiative（2026-02-17，美国首个 Agent 标准项目） |
| Linux Foundation | A2A 托管 + 150+ 组织 |
| OWASP | Agentic AI Top 10（AG01-10）风险框架（Guardrails 09 篇） |

> 🎯 核心要点：**标准正在制度化**——IETF/NIST 入场意味着 Agent 互操作从"框架生态"走向"基础设施标准"；企业应提前对齐（审计轨迹与身份模型现在就建，别等标准成熟）。

## 4. 互操作治理缺口

| 缺口 | 现状 | 行动 |
|---|---|---|
| Shadow IT | 任何开发者可给 Agent 接工具（安全团队看不到） | MCP 工具注册治理（Tool 02 篇） |
| 治理成熟度 | 87% 优先互操作 vs 7-8% 成熟治理 | 身份模型 + 审计轨迹现在建 |
| 身份碎片 | Agent 身份无统一规范（ACP 推进中） | DID/VC 预研 |
| 声誉缺失 | 无法判断对方可靠度 | 声誉/信任分（ACP 方向） |

> ⚠️ 2026 警示：**协议标准化解决"能通"，不解决"该通"**——互操作 ≠ 互信；治理缺口（身份/审计/声誉）要企业自己补，别等标准。

## 5. 协议选型

| 场景 | 协议 |
|---|---|
| Agent 调工具/数据 | MCP |
| Agent 委派专家（跨框架） | A2A |
| 高安全联邦编排 | ACP 方向（预研） |
| 代码 Agent 符号理解 | LSP |
| 单框架内协作 | 框架原生（LangGraph subgraph 等） |

> 💡 选型原则：**能协议就不自定义**——自定义协议 = 未来迁移成本 + 生态隔绝；单框架内部用原生，跨框架用 A2A/MCP 标准。

## 6. 与 MCP 体系的衔接

| MCP 体系（06-MCP协议与Agent Skill） | 本体系 |
|---|---|
| MCP 服务器构建/工具设计 | 工具层协议（垂直） |
| 本体系 | Agent 层协议（水平）——协作与编排 |

> 🎯 核心要点：**MCP 与 A2A 是"栈"不是"竞品"**——MCP 体系讲工具分发，本体系讲 Agent 协作；生产栈两者都要（03 篇 §7 分层架构）。

## 7. 常见误区

| 误区 | 真相 |
|---|---|
| "一个协议通吃" | 分层栈——MCP 工具/A2A 协作/ACP 安全演进 |
| "标准成熟再行动" | 身份与审计现在建——治理缺口别等标准 |
| "互操作=互信" | 协议解决"能通"，治理解决"该通" |
| "自定义协议更灵活" | 迁移成本+生态隔绝——能标准就标准 |
| "ACP 是替代 A2A" | ACP 推进安全联邦方向——A2A 是基线 |
| "单框架够用" | 跨组织/跨平台必须协议——锁死单框架是隔离 |
| "标准=合规" | 标准是技术契约——合规要审计/身份/治理 |

## 8. 面试速记

| 问题 | 一句话答案 |
|---|---|
| 协议栈分层？ | LSP 符号/MCP 工具/A2A 协作/ACP 安全演进 |
| ACP 四阶段协商？ | PROBE→BID→COMMIT（软契约）→Settlement（声誉） |
| ACP 信任机制？ | DID + VC + 签名消息（不可否认） |
| 混合发现？ | 本地广播（私有）+ 联盟链 DHT（全局） |
| IETF/NIST？ | AIPROTO WG 筹备 + AI Agent Standards Initiative（2026-02） |
| Shadow IT？ | 任何开发者接工具安全团队看不到——注册治理 |
| 治理现实？ | 87% 优先互操作 vs 7-8% 成熟治理 |
| 互操作≠？ | 互信——治理缺口自补 |
| 协议选型？ | 工具 MCP/协作 A2A/单框架原生 |
| 自定义协议代价？ | 迁移成本+生态隔绝 |
| 与 MCP 体系关系？ | 栈不是竞品——垂直+水平 |
| 企业行动？ | 身份模型+审计轨迹现在建 |

---

**下一模块**：[05-任务分配与交接](05-任务分配与交接.md)　**返回总览**：[00-Multi-Agent 协作组件总览](00-Multi-Agent协作组件总览.md)

## 参考来源

- [Beyond Context Sharing: A Unified Agent Communication Protocol (ACP)（arXiv 2602.15055）](https://ar5iv.labs.arxiv.org/html/2602.15055)
- [Agent protocols: A complete guide to MCP, A2A, and ACP（Tyk）](https://tyk.io/learning-center/agent-protocols-a-complete-guide-to-mcp-a2a-and-acp/)
- [Agent Interoperability: A Developer's Architecture Guide（Lyzr）](https://www.lyzr.ai/blog/agent-interoperability-architecture-guide/)
- [From Code Understanding to Multi-Agent Collaboration: A Layered Protocol Stack（IEEE）](https://ieeexplore.ieee.org/document/11569555)
