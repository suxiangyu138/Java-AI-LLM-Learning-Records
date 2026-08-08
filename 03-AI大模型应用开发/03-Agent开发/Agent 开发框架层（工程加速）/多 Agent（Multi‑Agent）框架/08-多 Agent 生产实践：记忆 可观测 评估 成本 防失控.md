# 08 多 Agent 生产实践：记忆 / 可观测 / 评估 / 成本 / 防失控

> 定位：生产篇——多 Agent 系统上线的五大战区：记忆、可观测、评估、成本控制、防失控护栏；2026 年行业共识与落地清单（2026-08 基准）

## 📚 目录

1. [生产五战区总览](#1-生产五战区总览)
2. [记忆：2026 年最不成熟的能力](#2-记忆2026-年最不成熟的能力)
3. [可观测：多 Agent 追踪的复杂性](#3-可观测多-agent-追踪的复杂性)
4. [评估：从用例到评测体系](#4-评估从用例到评测体系)
5. [成本控制：多 Agent 的成本放大器](#5-成本控制多-agent-的成本放大器)
6. [防失控护栏](#6-防失控护栏)
7. [上线检查清单](#7-上线检查清单)
8. [核心要点](#8-核心要点)

## 1. 生产五战区总览

| 战区 | 2026 行业共识 | 谁领先 |
|------|-------------|--------|
| 记忆 | **没有任何框架达到生产级长期记忆**（含语义检索+剪枝） | 无（全体未解） |
| 可观测 | LangSmith 最成熟；价值已向运营层迁移 | LangGraph + LangSmith |
| 评估 | 框架内 eval 工具雏形（ADK pytest、n8n Evals）；体系化靠自建 | 各家起步 |
| 成本 | 多 Agent 是 token 放大器，需显式预算 | 框架层监控 + 自建 |
| 防失控 | 循环/预算/人工升级三件套是必需品 | 各家护栏成熟度不同 |

> 🎯 **2026 最重要的行业判断**：框架选型的差异在缩小，**持久的问题在运营**（可观测/评估/监控）——生产投入应向运营层倾斜。

## 2. 记忆：2026 年最不成熟的能力

| 记忆层级 | 框架支持度 | 生产注意 |
|---------|-----------|---------|
| 短期（会话内） | 全部支持 | 线程内状态（LangGraph checkpointer/MemorySaver） |
| 跨会话 | LangGraph InMemoryStore/PostgresSaver、CrewAI 长期记忆 | 需要显式设计存储与过期 |
| 语义检索 + 剪枝 | **无生产级方案** | 自建：向量库 + 定期剪枝 + 召回过滤 |

**自建记忆设计要点**：
1. 只存**决策性结论**（任务结果/偏好），不存原始对话
2. 召回必须过滤（全量召回 = 噪声注入，见低代码模块 Agent 记忆示例）
3. 定期剪枝（过期/低价值条目清理）
4. 记忆写入与读取都要审计（记忆是 Agent 的行为基础）

## 3. 可观测：多 Agent 追踪的复杂性

| 层级 | 追踪什么 | 工具 |
|------|---------|------|
| 单调用 | LLM 调用（模型/token/延迟） | LangSmith、自建日志 |
| 单 Agent | 工具调用链、循环轮次 | LangSmith traces |
| **多 Agent** | **编排路由、移交历史、子 Agent 嵌套调用** | LangSmith（嵌套工具调用栈） |
| 协议层 | A2A 任务状态、MCP 调用 | 协议日志 |
| 业务层 | 任务完成率、用户满意度 | 自建指标 |

```text
多 Agent 可观测的最小三件套：
1. 追踪 ID 贯穿（一次用户请求 → 全链路 traceId）
2. 每次路由/移交记录（from_agent, to_agent, reason, result）
3. 失败信号（移交失败、循环检测触发、hop 耗尽、人工升级）
```

> ⚠️ **多 Agent 特有坑**：子 Agent 嵌套调用会让日志栈深 N 层——追踪必须支持**嵌套展开**（LangGraph subagents 模式天然支持）；扁平日志无法定位"哪个子 Agent 失败导致整体失败"。

## 4. 评估：从用例到评测体系

| 层级 | 内容 | 2026 工具 |
|------|------|----------|
| 用例级 | 典型场景通过/失败（20+ 用例） | 自建脚本 |
| 组件级 | 单 Agent 工具调用正确率 | LangSmith 回放 |
| 编排级 | **路由正确率、移交效率、循环次数** | 框架 eval（ADK pytest、n8n Evals）+ 自建 |
| 端到端 | 任务完成率、用户反馈 | 灰度 + 指标 |

**多 Agent 特有评估维度**：
1. 路由准确率（任务是否派对了 Agent）
2. 移交次数分布（异常高 = 编排混乱信号）
3. 循环/死胡同触发率（护栏报警率）
4. 每任务 token 成本（编排是否高效）
5. 人工介入率（HITL 频率异常 = 自动化不足）

> 💡 **评估要回答的问题不是"模型好不好"，而是"编排好不好"**——同样模型，Supervisor 提示词差会导致 30% 路由错误；评估体系必须覆盖编排层。

## 5. 成本控制：多 Agent 的成本放大器

```text
成本 = Σ（每个 Agent 的调用轮数 × 上下文量 × 单价）

多 Agent 成本爆炸的四个源头：
1. 重复上下文：同一上下文被多个 Agent 重复读入（最大头）
2. 角色扮演开销：CrewAI role/backstory 等提示词膨胀
3. 移交开销：每次 handoff 传递整个会话上下文
4. 无界循环：无 hop 预算的 swarm 无限烧钱
```

| 控制手段 | 说明 |
|---------|------|
| 上下文最小化 | 子 Agent 只收必要输入，不回传全量历史 |
| 模型分层 | 编排者用强模型，Worker 用快/廉模型（Opus 编排 + Haiku 干活） |
| 移交瘦身 | handoff 只传结构化摘要，不传原始对话 |
| Hop/轮次预算 | 硬上限（MAX_HOPS、MAX_ITERATIONS） |
| token 监控 | 每 Agent 每任务成本埋点 + 告警 |
| 用单 Agent 兜底 | 复杂度不足以支撑多 Agent 时，回退单 Agent + 工具链 |

## 6. 防失控护栏

（机制详见 02 篇第 9 节——本篇给出生产配置标准）

| 护栏 | 生产配置 |
|------|---------|
| 循环检测 | 移交前查 trace 集合，拒绝重复 |
| Hop 预算 | `MAX_HOPS = 5-10`（按场景调） |
| 轮次预算 | 每 Agent 最大工具轮次 |
| Token 预算 | 每任务硬上限 |
| 人工升级 | 循环/死胡同/预算耗尽 → Human 节点（HITL interrupt） |
| 检查点 | 每节点持久化，异常可回滚（checkpointer） |

> ⚠️ **护栏必须默认开启**：护栏是成本控制的一部分——没有 hop 预算的 swarm 失败模式不是"报错"而是"静默烧钱到预算耗尽"。

## 7. 上线检查清单

```text
□ 范式与规模匹配（3-10 用 Supervisor，别为 3 个 Agent 上层级网络）
□ 每个 Agent 有明确角色边界与失败降级（Agent 挂了怎么办）
□ 护栏三件套开启（循环检测 / hop 预算 / 人工升级）
□ checkpointer 持久化（swarm 无它即失忆）
□ 追踪 ID 贯穿 + 嵌套展开
□ 路由/移交/失败全埋点
□ 评估基线 ≥20 用例，含编排层指标
□ 成本埋点 + 告警（每 Agent 每任务 token）
□ 模型分层（强编排 + 廉干活）
□ 长期记忆方案明确（自建，别指望框架）
□ 安全：工具按 Agent 最小权限；A2A/MCP 端点鉴权
□ 灰度发布 + 人工兜底
```

## 8. 核心要点

> 🎯 **核心要点**：
> 1. 2026 行业判断：**价值已向运营迁移**——可观测/评估/监控比框架选型更重要
> 2. 记忆是全体未解难题：短期靠框架，长期自建（决策性结论 + 过滤 + 剪枝）
> 3. 多 Agent 成本四大放大器：重复上下文 / 角色开销 / 移交开销 / 无界循环——模型分层 + 上下文最小化是主杠杆
> 4. 护栏 = 成本控制：默认开启，防止"静默烧钱"
> 5. 评估必须覆盖编排层（路由准确率/移交次数/人工介入率），不只是模型层

---

**上一模块**：[07 A2A 与 MCP](07-A2A%20与%20MCP：多%20Agent%20协作的协议层.md)　**下一模块**：[09 跨框架对比与选型实战](09-跨框架对比与选型实战.md)　**返回总览**：[00 总览](00-总览：多%20Agent（Multi‑Agent）框架知识体系.md)

## 【参考来源】

- [State of AI Agents — March 2026 (GitHub)](https://github.com/zzhiyuann/state-of-ai-agents)
- [Multi-Agent Orchestration Frameworks 2026 (Presenc AI)](https://presenc.ai/research/multi-agent-orchestration-frameworks-2026)
- [The best AI agent frameworks in 2026 (LangChain)](https://www.langchain.com/resources/ai-agent-frameworks)
- [CrewAI Changelog (v1.15.2)](https://docs.crewai.com/v1.15.2/en/changelog)
- [Migrate from langgraph-supervisor - LangChain Docs](https://docs.langchain.com/oss/python/migrate/langgraph-supervisor)
- [AI Agent Frameworks (2026 Update): 8 SDKs Compared (MorphLLM)](https://www.morphllm.com/ai-agent-framework)
