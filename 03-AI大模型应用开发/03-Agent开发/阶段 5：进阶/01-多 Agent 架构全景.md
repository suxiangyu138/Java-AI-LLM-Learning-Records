# 01 多 Agent 架构全景

> 定位：多 Agent 的世界地图——模式谱系、选型决策、2026 生态格局、A2A 跨厂商编排（2026-08 基准）

## 📚 目录

1. [为什么需要多 Agent](#1-为什么需要多-agent)
2. [模式谱系：六种编排模式](#2-模式谱系六种编排模式)
3. [选型决策：什么场景用哪种](#3-选型决策什么场景用哪种)
4. [Supervisor vs Handoffs 对比](#4-supervisor-vs-handoffs-对比)
5. [2026 生态格局](#5-2026-生态格局)
6. [跨厂商编排：A2A](#6-跨厂商编排a2a)

## 1. 为什么需要多 Agent

单 Agent 的边界在哪？

| 单 Agent 的困境 | 多 Agent 的解法 |
|----------------|----------------|
| 一个 prompt 塞不下多领域技能 | 每个专业 Agent 只带自己的技能与工具 |
| 工具集太大 → 选择准确率下降 | 子 Agent 只暴露少量工具 |
| 上下文被多任务污染 | 任务隔离、上下文分离 |
| 一次推理无法并行 | 多 Agent 并行分工 |
| 出错全责连带 | 责任域分离 + 审查节点 |

> 🎯 **核心要点**：多 Agent 的收益不是"多个模型一起想"（成本反而更高）——是**职责分离、上下文隔离、工具最小化、并行化**。用错了场景 = 多付钱多出故障。

## 2. 模式谱系：六种编排模式

| 模式 | 结构 | 适合 | 2026 地位 |
|------|------|------|:---:|
| **Supervisor（监督者）** | 中央协调者路由给专业 Worker | 结构化任务、明确分工 | ⭐ 企业最常用 |
| **Handoffs（交接）** | 无中央瓶颈，Agent 间平级转交 | 开放式对话、动态路由 | 常用 |
| **层级网络（Hierarchical）** | 监督者管监督者（多级） | 大型组织式任务 | 前沿常用 |
| **顺序编排（Sequential）** | 链式流水线（A→B→C） | 固定流程 | 简单场景 |
| **并行编排（Parallel/Fan-out）** | 同一任务分片并行 | 批量处理 | 效率场景 |
| **Paperclip（回形针）** | CEO 分解目标 → 经理 Agent → 工人 Agent（2026-03 发布） | 顶层目标驱动 | 前沿实验 |

```text
层级示例（2026 典型）：
顶层 Supervisor
├── 研究团队 Supervisor
│   ├── Researcher A
│   └── Researcher B
└── 写作团队 Supervisor
    ├── Writer A
    └── Editor B
```

> 💡 理论深潜见 [主流 Agent 范式 07-多Agent协作范式](../主流%20Agent%20范式/07-多Agent协作范式.md)——本阶段讲"怎么实现"。

## 3. 选型决策：什么场景用哪种

```text
任务结构清晰、可预分配？ → Supervisor（企业默认）
路由取决于对话上下文？   → Handoffs（Swarm）
固定顺序流程？           → Sequential（最简，别过度设计）
批量同构任务？           → Parallel（Fan-out）
顶层目标 + 多层级组织？   → Hierarchical / Paperclip
```

| 场景 | 推荐 | 原因 |
|------|------|------|
| 客服分流（订单/退换/投诉） | Supervisor | 意图清晰、分工明确 |
| 研究 → 写作 → 编辑 流水线 | Handoffs/Sequential | 上下文接力 |
| 100 份文档摘要 | Parallel | 同构并行 |
| 公司级复杂任务 | Hierarchical | 组织化分解 |
| 探索性创新任务 | Paperclip | 目标驱动自组织 |

> ⚠️ **反模式：过度监督**——简单线性流（User→Supervisor→A1→Supervisor→A2）纯属绕路，用 Handoffs 直连。

## 4. Supervisor vs Handoffs 对比

| 维度 | Supervisor | Handoffs |
|------|-----------|----------|
| 控制 | 中央协调（有瓶颈） | 平级转交（无瓶颈） |
| 路由决策 | 监督者统一判断 | 每个 Agent 自行决定 |
| 适用 | 结构化分工 | 开放式对话 |
| 上下文 | 监督者持有全局 | 随交接传递 |
| 实现 | create_agent 工具包裹（02 篇） | Command 转移工具（03 篇） |
| 调试 | 中央日志清晰 | 交接链需追踪 |

## 5. 2026 生态格局

| 框架 | 定位 | 特点 |
|------|------|------|
| **LangGraph** | 图编排主流（24k+ star） | 状态感知层级循环最成熟 |
| Claude Agent SDK | 层级树 | Supervisor 内置 |
| Google ADK | A2A 原生 | 跨厂商编排 |
| Microsoft Agent Framework | 企业集成 | 微软生态 |
| CrewAI | 角色化 Crew | 轻量上手 |

**框架选型主线（结合阶段 3 结论）**：

```text
LangGraph/LangChain 栈（本阶段主线）→ create_agent 起步，StateGraph 下沉
跨厂商编排需求 → A2A（见第 6 节）
企业微软生态 → MS Agent Framework
```

## 6. 跨厂商编排：A2A

Agent-to-Agent（A2A）协议让**不同框架的 Agent 互相协作**：

```text
典型 2026 编排：
LangGraph 编排器
├── 委托合规检查 → Google ADK Agent（A2A 发现 + 委托）
├── 库存查询 → MCP 连接的工具
└── 合同生成 → CrewAI crew
```

| A2A 要素 | 说明 |
|---------|------|
| Agent Card | 能力发现（JSON 元数据） |
| 任务委托 | HTTP/SSE 协议传递任务 |
| 并行委托 | 一个任务拆给多个 Agent |

> 💡 A2A 协议深度见 [Multi-Agent 与 MCP 协议体系](../Agent与MCP协议/)——本阶段记住"跨厂商编排的协议层存在"即可，工程主线仍在 LangGraph。

> 🎯 **核心要点**：多 Agent 选型的灵魂 = **"模式匹配任务结构"**——先画任务的依赖与分工图，再选模式。默认从 Supervisor 起步（2026 企业最常用、最易调试），简单线性流别过度设计。Gartner 40% 的预测意味着：**多 Agent 不再是高级选项，而是主流技能**。

---

**返回总览**：[00-阶段总览：进阶](00-阶段总览：进阶.md) / **下一模块**：[02-Supervisor 模式工程](02-Supervisor%20模式工程.md)
