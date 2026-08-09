# Agent × RAG 理论融合总览

> 定位：「LLM AI Agent 相关理论」之「Agent × RAG」**理论融合层**——回答"Agent 与 RAG 是什么关系、怎么融合"：分工论（知识轴 × 行动轴）、双方失效边界、四种融合形态、检索决策化理论、知识库即记忆、选型决策。与 [RAG 知识体系](..%2F..%2F..%2F04-RAG检索增强生成%2F00-RAG知识体系总览.md)（工程实践 23 篇）和 [区分概念 08-RAG-vs-微调-vs-长上下文-vs-Agent](..%2F区分概念（面试容易混淆）%2F08-RAG-vs-微调-vs-长上下文-vs-Agent.md)（四大架构选型）分工：本体系讲**融合的理论框架**。2026 一句话：**"RAG 回答'知道什么'，Agent 回答'做什么'——融合的时机是任务同时需要两者"**。

## 📚 目录

1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [融合一图速记](#3-融合一图速记)
4. [学习路线推荐](#4-学习路线推荐)
5. [2026 生态基准](#5-2026-生态基准)

## 1. 知识体系导图

```text
Agent × RAG 理论融合
├── 01 分工论：RAG给知识，Agent给行动      知识轴×行动轴 / 四象限 / 正交性
├── 02 RAG的失效边界：何时必须Agent介入    单次检索 50-60% 上限 / 多跳 / 歧义
├── 03 Agent的知识缺口：何时必须RAG介入    幻觉 / 知识截止 / 外部状态 / 记忆外置
├── 04 融合形态全景：四种融合模式           RAG即工具 / 流程智能 / 记忆合一 / 编排多源
├── 05 Agentic RAG：检索决策化理论          检索即工具 / CRAG / Self-RAG / 多跳理论
├── 06 Agent in RAG：检索流程智能化         路由分类器 / 改写 / 检索策略=learned policy
├── 07 知识库即记忆：RAG与记忆组件融合       长期记忆=RAG实现 / 技术同构语义不同
├── 08 融合架构模式与选型决策               融合谱 / 决策树 / 成本延迟 / 评估分层
└── 09 面试高频问答冲刺                     18 题 + 答题范式 + 金句弹药库
```

## 2. 模块导航

| 序号 | 模块 | 核心内容 | 适合人群 |
|---|---|---|---|
| 01 | [分工论：RAG 给知识，Agent 给行动](01-分工论：RAG给知识，Agent给行动.md) | 知识轴×行动轴、四象限、正交性 | 全部（地基） |
| 02 | [RAG 的失效边界：何时必须 Agent 介入](02-RAG的失效边界：何时必须Agent介入.md) | 多跳/歧义/迭代检索、50-60% 上限实证 | 全部（面试高频） |
| 03 | [Agent 的知识缺口：何时必须 RAG 介入](03-Agent的知识缺口：何时必须RAG介入.md) | 幻觉/知识截止/私有数据、四条知识来源 | 全部（面试高频） |
| 04 | [融合形态全景：四种融合模式](04-融合形态全景：四种融合模式.md) | RAG即工具/流程智能/记忆合一/编排多源 | 全部 |
| 05 | [Agentic RAG：检索决策化理论](05-AgenticRAG：检索决策化理论.md) | 检索=决策、CRAG 五 Agent、Self-RAG、多跳 | Agent 工程师 |
| 06 | [Agent in RAG：检索流程智能化](06-AgentinRAG：检索流程智能化.md) | 路由分类器、改写、learned policy、+15-20% | Agent 工程师 |
| 07 | [知识库即记忆：RAG 与记忆组件融合](07-知识库即记忆：RAG与记忆组件融合.md) | 长期记忆=RAG、技术同构、生产合一 | 全部 |
| 08 | [融合架构模式与选型决策](08-融合架构模式与选型决策.md) | 融合谱、决策树、成本延迟、评估分层 | 架构师 |
| 09 | [面试高频问答冲刺](09-面试高频问答冲刺.md) | 18 题 + 答题范式 + 金句弹药库 | 面试前 |

## 3. 融合一图速记

```text
核心理论框架：知识轴 × 行动轴（两个正交维度）
  行动轴（Agent 强）▲
                    │
  低知识+高行动      │  高知识+高行动
  （工具型 Agent）   │  （Agentic RAG = 融合主形态）
                    │
  ─────────────────┼────────────────► 知识轴（RAG 强）
                    │
  低知识+低行动      │  高知识+低行动
  （直接生成）       │  （纯 RAG 问答）

融合的本质：检索从"流水线一步"变成"Agent 的一次决策"
  ——何时检索、检什么、检几次、够不够、不够怎么办
  ——单次检索在多跳问题上上限 50-60%（实证）

2026 一句话：Agentic RAG 已是严肃 Agent 技术栈的默认检索形态
  （LangGraph/OpenAI Agents SDK/CrewAI/MCP 驱动 Agent 均把检索当工具）
```

## 4. 学习路线推荐

| 路线 | 路径 | 目标 |
|---|---|---|
| 面试速成（1 天） | 00 → 01 → 02/03 → 08 → 09 | 能答分工、边界与选型 |
| 理论深潜（2 天） | 01-03 → 04-06 → 07 → 09 | 能讲清融合形态与决策理论 |
| 工程视角（3 天） | 全量 + [RAG 体系 10/17 篇](..%2F..%2F..%2F04-RAG检索增强生成%2F10-RAG与Agent结合.md) | 能落地 Agentic RAG |

## 5. 2026 生态基准

> 📅 基准窗口：2026-08。版本与事实以各模块【参考来源】为准。

- **Agentic RAG 成为默认形态**：到 2026-05，Agentic RAG 已是严肃 Agent 技术栈的默认检索模式——LangGraph 1.x、OpenAI Agents SDK、CrewAI、Agno 以及任何 MCP 驱动、把搜索暴露为工具的 Agent；MCP 标准化了"检索作为工具"的调用方式。
- **单次检索的上限实证**：CRAG/MultiHop-RAG 基准显示单次检索在多跳问题上约 50-60% 上限；AgenticRAGTracer（1,305 条多跳验证数据）最难关卡 GPT-5 精确匹配仅 22.6%——**失败源于推理链过早坍缩或过度延伸（hop-aware 诊断）**。
- **检索决策化（learned policy）**：2026 趋势——检索策略从静态流水线变为学习式策略：Agent 按查询选择检索器（BM25/稠密/混合/图）、top-k、重排深度与切片大小，早期系统检索质量提升 15-20%；"工具误路由"（语义查询却选了 BM25）是常见失败模式，用 ToolSelectionAccuracy 度量。
- **生产失败模式成型**：over-retrieval（8-12 次检索烧 token——硬性步骤预算 ~4 次）、under-retrieval（检 1 次就停——faithfulness 法官触发再检索）、judge drift（法官漂移——50-100 条人工标签校准，Cohen's kappa ~0.6）、state contamination（跨轮状态污染——每轮重置+摘要压缩）。
- **成本现实**：复杂查询 4 轮检索+重排的成本是 naive RAG 的 20-40 倍（典型 token 花费 8 倍），每轮 +200-500ms（向量）+300-800ms（重排），复杂查询落地 5-15 秒——**融合不是免费的，选型必须先算账**。
- **评估四层分离**：检索层（recall@k、context relevance）/ 轨迹层（任务完成）/ 生成层（faithfulness、groundedness）/ 路由层（tool-selection accuracy）——OTel 追踪挂每步检索 span。
- **与体系分工**：[RAG 知识体系](..%2F..%2F..%2F04-RAG检索增强生成%2F00-RAG知识体系总览.md)（10/17/18 篇）讲 Agentic RAG 的工程实现与实战；[区分概念 08](..%2F区分概念（面试容易混淆）%2F08-RAG-vs-微调-vs-长上下文-vs-Agent.md) 讲四大架构选型；本体系讲**融合的理论框架**（分工论/边界/形态/决策理论）。

---

**下一模块**：[01-分工论：RAG 给知识，Agent 给行动](01-分工论：RAG给知识，Agent给行动.md)

## 参考来源

- [Agentic RAG in 2026: Patterns, Code, Observability（FutureAGI）](https://futureagi.com/blog/agentic-rag-systems-2025/)
- [What Is Agentic RAG? Definition & FutureAGI Guide (2026)](https://futureagi.com/glossary/agentic-rag/)
- [Agentic RAG in 2026: Architecture Patterns, Frameworks & When to Use It（jobsbyculture）](https://jobsbyculture.com/blog/agentic-rag-guide-2026)
- [Agentic Retrieval-Augmented Generation: A Survey on Agentic RAG（arXiv 2501.09136）](https://arxiv-org.ezproxy.obspm.fr/html/2501.09136v4)
- [Data-Centric Perspectives on Agentic Retrieval-Augmented Generation: A Survey（ACL Findings 2026）](https://aclanthology.org/2026.findings-acl.78/)
- [From vectors to knowledge graphs: A comprehensive analysis of modern retrieval-augmented generation architectures（Computer Science Review 2026）](https://www.sciencedirect.com/science/article/abs/pii/S1574013726000341)
