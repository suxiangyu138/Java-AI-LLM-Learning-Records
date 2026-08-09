# Memory 记忆系统知识体系总览

> 定位：Agent 子组件专项之「记忆系统」——[四大核心组件](../../Agent%20四大核心组件/05-记忆组件：短中长时记忆架构.md)记忆组件的深化篇：从"三层记忆"讲到"提取、召回、冲突、遗忘"的全生命周期工程。总览做索引与速查，子主题各一篇。

## 📚 目录

1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [学习路线推荐](#3-学习路线推荐)
4. [核心概念速查](#4-核心概念速查)
5. [2026 生态基准](#5-2026-生态基准)

## 1. 知识体系导图

```text
Memory 记忆系统
├── 01 记忆系统全景：Agent 的分水岭        记忆的三类角色 / 原始对话 RAG 之败 / 三流派
├── 02 工作记忆：上下文窗口管理            token 预算 / 有界读取 / 滚动摘要 / 压缩
├── 03 情景记忆：会话历史与检查点          历史表 / 检查点 / 恢复回放 / 事件记忆
├── 04 长期记忆架构：向量与结构化          语义记忆 / claim 表 / 混合检索 / 一库化
├── 05 记忆写入：提取与沉淀                单遍 ADD-only / 提取质量 / 实体链接
├── 06 记忆召回：检索与注入                三信号融合 / RRF / 注入预算 / 信噪控制
├── 07 记忆冲突与时序：更新与遗忘          claim 生命周期 / 双时间戳 / 矛盾检测
├── 08 记忆安全与治理                      PII 脱敏 / 隔离 / 注入防御 / 保留策略
├── 09 记忆框架与产品实战                  Mem0 / Letta / Zep / 自研路线
└── 10 生产实践与面试冲刺                 评测协议 / 12 避坑 / 面试题
```

## 2. 模块导航

| 序号 | 模块 | 核心内容 | 适合人群 |
|---|---|---|---|
| 01 | [记忆系统全景：Agent 的分水岭](01-记忆系统全景：Agent的分水岭.md) | 记忆三类角色、RAG 之败、三流派 | 全部（地基） |
| 02 | [工作记忆：上下文窗口管理](02-工作记忆：上下文窗口管理.md) | token 预算、有界读取、摘要、压缩 | 全部（地基） |
| 03 | [情景记忆：会话历史与检查点](03-情景记忆：会话历史与检查点.md) | 历史表、检查点、恢复回放 | Agent 工程师 |
| 04 | [长期记忆架构：向量与结构化](04-长期记忆架构：向量与结构化.md) | 语义记忆、claim 表、混合检索 | Agent 工程师 |
| 05 | [记忆写入：提取与沉淀](05-记忆写入：提取与沉淀.md) | 单遍 ADD-only、提取质量、实体链接 | Agent 工程师 |
| 06 | [记忆召回：检索与注入](06-记忆召回：检索与注入.md) | 三信号融合、RRF、注入预算 | Agent 工程师 |
| 07 | [记忆冲突与时序：更新与遗忘](07-记忆冲突与时序：更新与遗忘.md) | claim 生命周期、双时间戳、矛盾检测 | 架构师 |
| 08 | [记忆安全与治理](08-记忆安全与治理.md) | PII、隔离、注入防御、保留、合规 | 安全关注者 |
| 09 | [记忆框架与产品实战](09-记忆框架与产品实战.md) | Mem0/Letta/Zep 对比、接入、自研 | 落地开发者 |
| 10 | [生产实践与面试冲刺](10-生产实践与面试冲刺.md) | 评测协议、12 避坑、面试题 | 面试/上线前 |

## 3. 学习路线推荐

| 路线 | 路径 | 目标 |
|---|---|---|
| 入门（1 天） | 01 → 02 → 06 → 10 | 理解记忆分层与"注入预算"，能排查上下文问题 |
| 进阶（1 周） | 01-02 → 04 → 05 → 06 → 10 | 掌握提取与召回工程，能自建记忆管道 |
| 高级（2 周） | 全量 + 07 → 08 → 09 | 能设计冲突/时序/遗忘机制，会选型记忆产品 |

## 4. 核心概念速查

| 概念 | 一句话定义 |
|---|---|
| 工作记忆 | LLM 上下文窗口中的当前任务信息（要省 token） |
| 情景记忆 | 会话级事件与历史（要全，审计与恢复用） |
| 长期记忆 | 跨会话持久的事实/偏好/经验（要准、可检索） |
| 记忆管理器 | 决定存什么/取什么/摘要什么/丢什么的策略层 |
| 原始对话 RAG | 把对话历史直接向量化检索——信噪崩塌，2026 已证伪 |
| 单遍 ADD-only 提取 | 一次 LLM 调用只做"新增事实"，不做对账——快且不毁原事实 |
| 实体链接（Entity Linking） | 事实间实体关联，召回时用于提升 |
| 三信号融合 | 语义（向量）+ 关键词（BM25）+ 实体并行打分后融合 |
| RRF（互惠排名融合） | 多路召回结果按排名倒数融合的无参算法 |
| claim 生命周期 | 事实状态机 PROPOSED→ACCEPTED→DEPRECATED，废弃即过滤 |
| 双时间戳 | event-time（事实在世界成立的时间）+ ingestion-time（系统得知时间） |
| 点查询（As-of Query） | "某一时刻该事实是什么"的历史查询 |
| 注入预算 | 每轮记忆注入上下文的 token 上限与装配优先级 |
| 遗忘机制 | 过期/废弃记忆的淘汰策略（不是删，是降权/归档） |
| LoCoMo / LongMemEval / BEAM | 记忆评测基准（会话长程回忆 / 长上下文记忆 / 百万 token 规模） |

## 5. 2026 生态基准

> 📅 基准窗口：2026-08。版本与事实以各模块【参考来源】为准。

- **三流派格局**：Mem0 v3（"记忆即 SDK"：ADD/UPDATE/DELETE/NOOP 四操作、单遍 ADD-only 提取、三信号融合检索、多租户隔离，LoCoMo 91.6-92.5 供应商口径）；Letta/MemGPT（"记忆即运行时"：OS 三阶 core/recall/archival、Agent 自主管理记忆、REST 服务化，LoCoMo ~74%）；Zep/Graphiti（"记忆即时序图谱"：双时间戳 bi-temporal 图、点查询、语义+BM25+图遍历融合，LongMemEval 63.8% vs Mem0 49.0% 独立评估）。
- **提取范式转变**：Mem0 2026-04 从"两遍对账式提取"（候选→比对→增改删，慢且毁原事实）改为**单遍 ADD-only**（只新增，新事实与旧事实并存）——提取延迟减半、记忆质量提升；Agent 生成的事实（"我已订好 3 月 3 日航班"）成为一等公民。
- **评测协议陷阱**：同一系统不同协议下得分差 30 分（True Memory 93.0% vs mem0 61.4% vs Zep ~71%，arXiv 2605.04897 对照实验）——判题宽松度、答案模型、注入粒度都会显著影响分数；**BEAM（1M/10M token 规模）比 LoCoMo 更贴近生产**，LoCoMo 可被"激进检索+大窗口+前沿模型"刷分。
- **记忆产品商业化成熟**：Mem0 自托管服务器（Docker Compose + 鉴权默认开启）、Zep 三合规认证（SOC 2/HIPAA/GDPR，唯一有认证的记忆产品）、Letta 全面转向运行时定位（legacy 仓库冻结、lett-code 新项目）。
- **"存储≠记忆"共识**（arXiv 2605.04897）：记忆系统的重心从"存什么"转向"怎么取"——检索中心架构（retrieval-centered）成为设计主线；记忆检索 token 效率成硬指标（Mem0 <7K tokens/次 vs 全上下文 25K+）。
- **与体系内其他模块分工**：[四大核心组件](../../Agent%20四大核心组件/05-记忆组件：短中长时记忆架构.md) 05 篇讲"记忆组件的角色与位置"（概览），本体系讲"记忆系统的完整工程"（深潜）；持久化存储细节见[数据库交互](../../Agent%20配套技术生态模块/数据库交互/05-Agent结构化记忆：会话与状态持久化.md) 05 篇。

---

**下一模块**：[01-记忆系统全景：Agent 的分水岭](01-记忆系统全景：Agent的分水岭.md)

## 参考来源

- [AI Agent Memory in 2026: MemPalace, Mem0, and Persistent Context（Eden AI）](https://www.edenai.co/post/ai-agent-memory-mempalace-mem0-and-persistent-context)
- [Best AI Agent Memory Systems in 2026: 8 Frameworks Compared（Vectorize）](https://vectorize.io/articles/best-ai-agent-memory-systems)
- [Agent Memory 2026: Mem0, Letta and Zep Compared（innobu）](https://www.innobu.com/en/articles/agent-memory-2026-mem0-letta-zep-hermes-openclaude-comparison.html)
- [AI Memory Solutions Compared: Q3 2026（Mnemoverse）](https://mnemoverse.com/docs/library/ai-memory-solutions-2026-q3)
- [AI Memory Benchmarks 2026: LoCoMo, LongMemEval & BEAM（Mem0 Blog）](https://mem0.ai/blog/ai-memory-benchmarks-in-2026)
- [Introducing The Token-Efficient Memory Algorithm（Mem0 Blog）](https://mem0.ai/blog/mem0-the-token-efficient-memory-algorithm)
- [Storage Is Not Memory: A Retrieval-Centered Architecture for Agent Recall（arXiv 2605.04897）](https://arxiv-org.ezproxy.obspm.fr/abs/2605.04897)
- [Which Agent Memory Approach Is Best for Long Conversations?（Oracle）](https://blogs.oracle.com/developers/which-agent-memory-approach-is-best-for-long-conversations)
