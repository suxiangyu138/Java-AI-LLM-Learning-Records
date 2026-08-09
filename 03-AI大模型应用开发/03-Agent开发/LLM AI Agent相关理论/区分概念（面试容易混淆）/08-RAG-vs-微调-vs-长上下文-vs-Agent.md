# RAG vs 微调 vs 长上下文 vs Agent

> 辨析篇：四大 LLM 架构选型——**考试类比 + 决策规则**。深潜见 [RAG 体系](..%2F..%2F..%2F04-RAG检索增强生成%2F00-RAG知识体系总览.md)。

## 1. 考试类比（2026 经典）

| 架构 | 类比 | 特点 |
|---|---|---|
| 长上下文 | 把整本教科书带进考场 | 都在，但考场上翻页靠你 |
| RAG | 开卷考 + 图书管理员 | 图书管理员取两页相关 |
| 微调 | 学一学期 | 知识内化，但结课即冻结 |
| Agent | 行动 | 不只答题——去做事 |

> 🎯 核心要点：**面试用类比开场、用决策规则落地**——"Long context = 全书带进考场；RAG = 开卷+管理员；Fine-tuning = 学一学期"（2026 共识比喻）。

## 2. 决策规则表

| 你的场景 | 选型 |
|---|---|
| 私有/动态文档问答 | **RAG**（90% 知识库场景默认） |
| 实时数据 | RAG 或 Agent |
| 自定义语气/风格/格式 | **微调**（改行为） |
| 行动（API/工具/多步） | **Agent** |
| 语料 <50K-100K 且稳定 | 长上下文（省基建） |
| 预算紧/MVP | RAG（最便宜起步） |
| 速度关键（<500ms） | 微调（小模型更快） |
| 复杂企业流程 | 混合（Agent+RAG+微调） |

## 3. 两两辨析（混淆点）

| 概念对 | 判据 | 数据 |
|---|---|---|
| RAG vs 微调 | **"微调是 Form（行为），RAG 是 Fact（事实）"** | 微调加知识=最普遍误用（自信幻觉） |
| 长上下文 vs RAG | 全量塞入 vs 按需检索 | context rot 98%→64%；lost-in-the-middle |
| RAG vs Agent | 答知识 vs 做行动 | RAG +100-500ms；Agent 500ms-3s+ |
| 微调 vs Agent | 行为定型 vs 动态决策 | 微调无可追溯性；Agent 需可观测 |

> ⚠️ 2026 反模式编目：① 简单 Q&A 用 Agent（零收益加成本）② **微调加知识**（模型自信地幻觉更新信息）③ 未验证简单方案先建 Agent ④ v0 过度工程（先有评估器再谈架构）。

## 4. 各自优劣（面试背诵版）

| 架构 | 赢 | 输 |
|---|---|---|
| 长上下文 | 简单/无基建/小语料 | 每 token 付费/context rot/引用弱 |
| RAG | 新鲜/引用/低成本 | 检索质量是 #1 失败点/延迟 |
| 微调 | 行为/格式/速度（5x） | 知识冻结/无可追溯/需标注 |
| Agent | 行动/多步/自适应 | 最贵最慢/难调试/非确定 |

> 🎯 核心要点：**"检索质量差 = 自信的胡说"**（RAG）；**"微调是训练模式不是内容"**（数据不一致会变成学到的行为）——两句话是面试点睛。

## 5. 组合是答案（2026 共识）

```text
企业客服机器人（标准组合）：
  微调模型 → 路由/意图分类（快/便宜/一致）
  RAG → 检索知识库文章/订单历史（事实）
  Agent → 行动：建工单/退款/查订单（动作）
  长上下文 → 提示窗口管理（装配）
```

> 💡 2026 共识：**"架构不是对手是层"**——多数生产系统组合 2-4 种；决策框架层次化：任务复杂度（简单→直接 LLM、动态知识→RAG、多步决策→Agent）× 数据更新频率（静态→微调、日更→RAG 缓存、实时→Agent 动态查询）。

## 6. 面试速记

| 问题 | 一句话答案 |
|---|---|
| 考试类比？ | 全书带进考场/开卷+管理员/学一学期/去做事 |
| Form vs Fact？ | 微调改行为、RAG 给事实 |
| 长上下文适用？ | <50K-100K 稳定语料 |
| context rot？ | 98%→64% 全量塞入衰减 |
| 最普遍误用？ | 微调加知识 |
| Agent 何时值？ | 行动/多步——简单 Q&A 不值 |
| RAG 失败点？ | 检索质量（#1） |
| 速度关键？ | 微调（小模型 5x 快） |
| 组合？ | 微调路由+RAG 检索+Agent 行动 |
| v0 原则？ | 先有评估器再谈架构 |

---

**下一模块**：[09-推理模型 vs 普通模型 vs 思考](09-推理模型-vs-普通模型-vs-思考.md)　**返回总览**：[00-区分概念总览](00-区分概念总览.md)

## 参考来源

- [RAG vs Fine-tuning vs AI Agents (2026)（dev.to）](https://dev.to/agdex_ai/rag-vs-fine-tuning-vs-ai-agents-which-llm-architecture-to-choose-in-2026-402a)
- [RAG vs Long Context vs Fine-Tuning（AIBuilderClub）](https://www.aibuilderclub.com/blog/rag-vs-long-context-vs-fine-tuning)
- [RAG vs Fine-Tuning vs Agents: A Decision Framework（BEON.tech）](https://beon.tech/blog/rag-vs-fine-tuning-vs-agents/)
- [Intellibooks Guide to RAG vs Fine-Tuning vs Agentic AI vs Context Engineering（dev.to）](https://dev.to/intellibooks_ai/intellibooks-guide-to-rag-vs-fine-tuning-vs-agentic-ai-vs-context-engineering-324f)
