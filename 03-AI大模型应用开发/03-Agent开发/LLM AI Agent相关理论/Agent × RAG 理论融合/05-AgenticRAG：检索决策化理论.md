# Agentic RAG：检索决策化理论

> 理论篇：Agentic RAG 的**理论内核**——"检索决策化"的五步决策模型、CRAG 五 Agent 架构理论、Self-RAG 反思理论、多跳推理的链式理论，以及生产失败模式与成本理论。工程实现见 [RAG 体系 17-AgenticRAG 与自我反思](..%2F..%2F..%2F04-RAG检索增强生成%2F17-AgenticRAG与自我反思.md)。

## 1. 检索决策化：五个决策点（理论核心）

Agentic RAG 的理论本质：**把检索从"流水线步骤"变成"决策序列"**——每次检索前有判断、检索后有评估：

| 决策点 | 问题 | 机制 |
|---|---|---|
| ① 检不检 | 这个问题需要外部知识吗？ | Agent 判断（无关问题直接答，省一次检索） |
| ② 检什么 | 查询怎么表达？ | 改写/拆解/多路查询（query rewriting） |
| ③ 怎么检 | 哪个检索器、top-k、重排深度？ | 按查询选择（learned policy） |
| ④ 够不够 | 检索结果充分吗？相关吗？ | 相关性评估（relevance judge） |
| ⑤ 不够怎么办 | 重检/换源/澄清/放弃？ | 纠正循环（CRAG）/ 追问 / 承认不知道 |

> 🎯 核心要点（背诵版）：**纯 RAG 只有"②怎么检"一步是技术决策，其余全固定；Agentic RAG 把①③④⑤全部变成模型决策**——这就是"检索获得判断力"的理论含义。五个决策点依次展开，就是完整的 Agentic RAG 循环。

## 2. CRAG：纠正式检索的理论架构（五 Agent）

CRAG（Corrective RAG，2024）是 Agentic RAG 的经典理论模型，由五个 Agent 协作：

| Agent | 职责 | 触发条件 |
|---|---|---|
| 上下文检索 Agent | 执行检索，取回候选上下文 | 始终 |
| 相关性评估 Agent | 评估检索结果质量 | 每次检索后 |
| 查询精化 Agent | 质量差 → 改写查询重检 | 评估不通过 |
| 外部动态检索 Agent | 内部库不足 → 转 Web 外部检索 | 评估仍不足 |
| 响应综合 Agent | 基于最终上下文生成答案 | 上下文充分 |

> 💡 理论价值：**CRAG 把"检索质量"显式建模为一个可评估、可纠正的状态机**——评估 Agent 是循环的"裁判"，其余 Agent 是"执行者"；这正是反思组件（独立批评者理论）在检索域的落地。2026 综述（arXiv 2501.09136）把 CRAG 列为 Agentic RAG 的经典架构。

## 3. Self-RAG：反思式检索理论

| 概念 | 内容 |
|---|---|
| 理论 | 让模型生成时**反思自己是否需要检索、检索结果是否 grounded、答案是否忠实** |
| 标记（token） | 生成特殊反思标记：检索需求判断 → 相关性判断 → 支持性判断（faithfulness） |
| 与 CRAG 的区别 | CRAG 在"检索后"纠正；Self-RAG 把反思**贯穿生成全程**（边生成边自查） |
| 面试表述 | "Self-RAG 让模型生成自己的'质检报告'——检前问需求、检后查相关、答后验忠实" |

> ⚠️ 理论前提（与反思组件一致）：Self-RAG 的反思有效性依赖**可验证信号**（检索结果可判相关、回答可判 grounded）——这正是检索域 vs 开放生成域的优势：**检索域天然有 oracle（文档即证据），所以反思在 Agentic RAG 里比在开放任务里更可靠**。

## 4. 多跳推理的链式理论

| 概念 | 内容 |
|---|---|
| 多跳（multi-hop） | 答案需要"查 A → 用 A 的结果查 B → 综合"——每跳一次检索 |
| 跳的模型 | hop = 一次"推理+检索"单元；链式依赖：跳 n 的查询 = 跳 n-1 的结果 |
| 失败机制（2026 实证） | 推理链**过早坍缩**（跳太少就下结论）或**过度延伸**（无效跳累积噪声）——AgenticRAGTracer 最难关 GPT-5 仅 22.6% |
| 理论对策 | 跳间校验（每跳结果与问题对齐检查）+ 终止条件（证据充分即停——防止过度延伸） |

> 🎯 理论结论：**多跳的本质是"推理链与检索链的耦合"**——每一跳既是推理步骤也是检索步骤；失败根源在链的管理（坍缩/延伸），不在单次检索质量。这与 Agent 循环的终止条件理论（无进展检测）同构。

## 5. 生产失败模式与成本理论（2026 实证）

| 失败模式 | 表现 | 理论修复 |
|---|---|---|
| Over-retrieval | 8-12 次检索烧 token | 硬性步骤预算（~4 次）——检索次数是成本函数不是质量函数 |
| Under-retrieval | 检 1 次就停（其实需 3 次） | faithfulness 法官触发再检索——"没查够"要可检测 |
| Judge drift | 评估 Agent 标准漂移 | 50-100 条人工标签定期校准（Cohen's kappa ~0.6） |
| State contamination | 跨轮检索状态污染下一轮 | 每轮状态重置 + 摘要压缩 |
| 工具误路由 | 语义查询选了 BM25 | 按工具命中率指标 + ToolSelectionAccuracy 度量 |

| 成本维度 | 数值 |
|---|---|
| 复杂查询 vs naive RAG | 20-40× 总成本（典型 token 8 倍） |
| 每轮检索延迟 | 向量 +200-500ms，重排 +300-800ms |
| 复杂查询端到端 | 5-15s |
| 经验法则 | 检索次数 ≤4、评估轮次 ≤2（超出 = 设计问题） |

## 6. 评估分层理论

| 层 | 指标 | 回答的问题 |
|---|---|---|
| 检索层 | recall@k、context relevance | 检到对的东西了吗 |
| 轨迹层 | 任务完成率、步数效率 | 循环走对了吗 |
| 生成层 | faithfulness、groundedness | 答案忠于证据吗 |
| 路由层 | tool-selection accuracy | 检索工具选对了吗 |

> 💡 2026 共识：**Agentic RAG 的评估必须四层分离**——"检索好但答案差"（生成层问题）与"检索差"（检索层问题）是不同病灶，聚合指标会掩盖根因；所有指标挂同一 OTel span 统一聚类。

## 7. 面试速记

| 问题 | 一句话答案 |
|---|---|
| Agentic RAG 理论内核？ | 检索决策化——检不检/检什么/怎么检/够不够/不够怎么办五决策点 |
| CRAG 五 Agent？ | 检索/相关性评估/查询精化/外部兜底/响应综合——评估 Agent 是裁判 |
| Self-RAG 与 CRAG 区别？ | CRAG 检索后纠正；Self-RAG 反思贯穿生成全程（faithfulness） |
| 多跳失败机制？ | 推理链过早坍缩或过度延伸——链管理问题，非单次检索问题 |
| 生产失败模式？ | over/under-retrieval、judge drift、状态污染、工具误路由 |
| 成本？ | 复杂查询 20-40×、5-15s——检索次数 ≤4 是设计纪律 |
| 评估分几层？ | 检索/轨迹/生成/路由四层分离，OTel 统一追踪 |

---

**下一模块**：[06-Agent in RAG：检索流程智能化](06-AgentinRAG：检索流程智能化.md)　**返回总览**：[00-Agent×RAG 理论融合总览](00-Agent×RAG理论融合总览.md)

## 参考来源

- [Agentic Retrieval-Augmented Generation: A Survey on Agentic RAG（arXiv 2501.09136）](https://arxiv-org.ezproxy.obspm.fr/html/2501.09136v4)
- [Agentic RAG in 2026: Patterns, Code, Observability（FutureAGI）](https://futureagi.com/blog/agentic-rag-systems-2025/)
- [What Is Agentic RAG? Definition & FutureAGI Guide (2026)](https://futureagi.com/glossary/agentic-rag/)
- [Data-Centric Perspectives on Agentic Retrieval-Augmented Generation: A Survey（ACL Findings 2026）](https://aclanthology.org/2026.findings-acl.78/)
