# 01 - LlamaIndex 是什么

> 本体系第一课：LlamaIndex 的定位——数据框架、RAG 事实标准、生态四件套——"LangChain 管编排，LlamaIndex 管数据"

---

## 📚 目录

1. [一句话定位](#1-一句话定位)
2. [解决什么问题](#2-解决什么问题)
3. [2026 版本基线](#3-2026-版本基线)
4. [生态四件套](#4-生态四件套)
5. [与 LangChain 的分工](#5-与-langchain-的分工)
6. [练习 5 题](#6-练习-5-题)
7. [本节验收](#7-本节验收)

---

## 1. 一句话定位

**LlamaIndex = 数据框架——把"私有数据"变成"LLM 能用的知识"的桥梁**：

```text
一句话定位
├── 本质：数据框架（Data Framework）——文档接入/切分/索引/检索的标准化
├── 出身：2022 年开源（前身 GPT Index）——MIT 许可——40K+ 星标
├── 现状：llama-index-core 0.14.23（2026-08——周更节奏）
├── 地位：RAG 的事实标准（300+ LlamaHub 集成——LlamaParse 处理文档超 5 亿份）
└── 比喻：LlamaIndex 是"图书馆管理员"——文档是"书"，索引是"书架"，
    查询引擎是"借书台"——"你问问题，管理员拿对的书回答你"
```

**定位心智**：**"LlamaIndex 解决的核心问题：LLM 不知道你的私有数据——把它切碎、嵌入、索引、检索出来"**——"**一个 RAG 应用 = 加载 + 切分 + 嵌入 + 存储 + 检索 + 合成——LlamaIndex 把'数据侧'六步标准化——'自己写也能写（但切分/索引/检索全是坑），用框架写更稳（统一抽象 + 现成集成）'"**（"框架的价值：① 统一数据抽象（Document/Node）；② 现成集成（300+ 连接器）；③ 检索质量组件（重排/混合检索/图谱）——**'三个价值 = 用 LlamaIndex 的理由'"**）；**什么时候用**——"简单问答（数据在提示词里）直接用 SDK；有文档/知识库要接（RAG 场景）用 LlamaIndex——**'数据接入复杂度决定用不用'"**。

## 2. 解决什么问题

**LlamaIndex 解决的四个"数据侧重复造轮子"问题**：

```text
解决四问题
├── ① 文档接入不统一：PDF/网页/数据库/Notion 各有解析方式
│    → LlamaHub Readers + LlamaParse（03 篇——300+ 现成连接器）
├── ② 切分质量难保证：切太粗检索不准，切太细上下文割裂
│    → NodeParser 家族（04 篇——定长/语义切分）
├── ③ 检索只懂相似度：关键词/语义/图关系不会组合
│    → QueryFusionRetriever + Reranker（06 篇——混合检索与重排）
└── ④ 知识关系丢失：事实跨文档关联、多跳问题答不出
    → PropertyGraphIndex（07 篇——GraphRAG）
    ——"四个问题 = RAG 应用的数据侧四大通用环节——LlamaIndex 给标准解"
```

**问题心智**：**"四个问题的记忆：'接入/切分/检索/关系'——RAG 的数据链路"**——"**为什么是'通用'问题：不管什么知识库（客服/文档问答/审计）都遇到——LlamaIndex 给标准答案**"；**解决方式的定位**——"LlamaIndex 不解决：模型本身（那是模型厂商的）、业务逻辑（那是你的）——**'LlamaIndex 管'数据到知识'这一层——模型之下、业务之上'"**。

## 3. 2026 版本基线

**llama-index-core 0.14.x——周更节奏、多包架构（老教程大量过时的原因）**：

```text
2026 版本基线（新代码必须知道）
├── ① 多包架构：核心（llama-index-core）+ 300+ 集成包（llama-index-llms-openai 等）
│    —— 装什么用什么，按需 pip install
├── ② 0.13.0（2025 末）破坏性变更：HybridQueryEngine 移除
│    → RetrieverQueryEngine + QueryFusionRetriever（06 篇）
├── ③ PropertyGraphIndex.from_documents 需显式 mode="llm"/"custom"（07 篇）
├── ④ QueryPipeline（DAG）弃用 → Workflows 事件驱动（08 篇）
└── ⑤ 版本策略：周更 + 锁版本——生产用 `>=0.14,<0.15` 区间
    ——"0.13 前后的 API 差异 = 老教程过时的根源——看到 HybridQueryEngine 等名字 → 换新写法"
```

**版本心智**：**"0.14 时代的记忆：'融合检索 + 显式 mode + Workflows——三新'"**——"**老教程（0.10 以前）的三大过时：HybridQueryEngine（移除）、PropertyGraphIndex 默认 mode（需显式）、QueryPipeline（弃用）——'看到这三个 → 旧写法'"**；**版本心态**——"周更不代表不稳定——核心 API 稳定、集成包快速迭代——**'锁版本区间 + 看 release notes = 生产心态'"**。

**演进简史**（理解老教程为啥过时）：2022 年诞生（前身 GPT Index——"把文档变成 GPT 能用的索引"）；2024 年 0.10 大重构（拆成核心 + 300+ 集成包的多包架构——装什么用什么）；2025 年 0.13（融合检索 API 重构、PropertyGraphIndex 显式 mode）；2025 末-2026 年 0.14（Workflows 成熟、AgentWorkflow 成为多 Agent 入口、QueryPipeline 彻底退场）——**"演进主线：RAG 库 → 数据框架 → Agent 编排框架——'LlamaIndex 早已不只是 RAG'"**。

## 4. 生态四件套

**LlamaIndex 生态四件套——各管一段（2026 全景）**：

```text
生态四件套
├── llama-index-core：开源核心（文档/索引/检索/查询——本体系主体）
├── LlamaHub：集成中心（300+ Readers/向量库/工具——pip install 即用）
├── LlamaParse：云解析服务（PDF/表格/手写/多模态——复杂文档首选）
└── LlamaCloud：托管平台（索引/Agent/评估——企业级全托管）
    ——"四件套的分工：核心组装、Hub 扩展、Parse 解析、Cloud 托管"
```

**生态心智**：**"四件套的记忆：'核心/Hub/解析/托管'"**——"**学习顺序：先核心（本体系 02-09）→ 需要时接 Hub → 复杂文档上 LlamaParse → 规模化考虑 LlamaCloud——'四件套是递进关系（免费到付费）'"**；**生态地位**——"2026 年：LlamaParse 处理文档超 5 亿份、20 万+ LlamaCloud 用户——**'四件套 = 2026 年 RAG 的标准栈'"**（"了解即可：知道存在与分工——小项目用核心 + Hub 就够"）。

**开源 vs 商业边界**：核心库 MIT 协议完全免费（加载/切分/索引/检索/Agent 全在核心）；LlamaParse/LlamaCloud 是商业服务（按用量付费）——**"学习与原型零成本，生产按需付费——'免费的核心 + 付费的云 = LlamaIndex 的商业模型'"**（与 vLLM 体系同为"开源核心 + 商业服务"模式——**"国产替代心态：国内生产可用 OpenAI 兼容端点 + 本地解析替代云服务（03 篇）"**）。

## 5. 与 LangChain 的分工

**选型的关键是"需求匹配"，不是"谁更好"**：

| 维度 | LlamaIndex | LangChain |
|------|-----------|-----------|
| 核心定位 | 数据框架（RAG 优先） | 编排框架（Agent 优先） |
| 强项 | 切分/索引/检索质量 | 工具调用/链/多 Agent |
| 文档解析 | LlamaParse（复杂 PDF/表格） | 基础 Loader |
| 集成数量 | 300+（LlamaHub） | 700+ 集成、100K+ 星标 |
| 框架开销 | 约 6ms/查询 | LCEL 约 10ms、LangGraph 约 14ms |
| 适合场景 | 知识库问答、RAG | 复杂 Agent、多步工作流 |

**选型心智**：**"纯 RAG（文档问答）→ LlamaIndex；复杂 Agent → LangChain/LangGraph；两者可混用——LlamaIndex 当检索层嵌进 LangChain Agent"**——"**'检索质量是 LlamaIndex 的主场'——混合检索/重排/图谱开箱即用；'编排生态是 LangChain 的主场'——工具/多 Agent 更成熟'"**（"**互补多于竞争**：常见生产架构 = LlamaIndex 检索 + LangChain 编排——**'选型三问：数据多不多？Agent 复杂不复杂？要引用/图谱吗？'"**）。

## 6. 练习 5 题

1. LlamaIndex 一句话定位？（数据框架——RAG 事实标准）
2. 解决的四个问题？（接入/切分/检索/关系）
3. 2026 版本基线？（core 0.14.23——周更；0.13 移除 HybridQueryEngine）
4. 生态四件套？（核心/LlamaHub/LlamaParse/LlamaCloud）
5. 与 LangChain 怎么分工？（数据 vs 编排——可混用）

## 7. 本节验收

**验收动作**：① 默写一句话定位 + 四个问题；② 背下"三新"（融合检索/显式 mode/Workflows）；③ 装 `llama-index` 并验证版本（0.14.x）；④ 打开官方文档确认最新写法——**"定位 + 问题 + 版本 + 生态 = 认识 LlamaIndex"**——**练习纪律**：看到 0.10 教程先怀疑——"API 变化大——以官方文档为准"。

> 🎯 **核心要点**：LlamaIndex = 数据框架（**图书馆管理员——RAG 事实标准**）；解决四问题（**接入/切分/检索/关系**）；**0.14 三新（融合检索 + 显式 mode + Workflows）**；**生态四件套（核心/LlamaHub/LlamaParse/LlamaCloud）**；**与 LangChain 互补（数据 vs 编排——可混用）**。

---

**上一模块**：[00-LlamaIndex总览.md](./00-LlamaIndex总览.md) / **下一模块**：[02-核心抽象：Document与Settings.md](./02-核心抽象：Document与Settings.md)
