# Memory 记忆组件最简定义总览

> 定位：四大基础核心组件之「记忆」的最简速记层——3 分钟看懂"记忆组件"是什么；深化见 [Memory 记忆系统](..%2F..%2F..%2FAgent%20子组件专项学习%2FMemory%20记忆系统%2F00-Memory记忆系统总览.md)（11 篇）。2026 一句话：**记忆 = 短时（上下文窗口）+ 长时（跨会话持久）+ 治理（来源/审计/防投毒）**——记忆是基础设施，不是事后附加。

## 📚 目录

1. [速记导图](#1-速记导图)
2. [30 秒速查表](#2-30-秒速查表)
3. [学习路径](#3-学习路径)
4. [2026 关键事实](#4-2026-关键事实)

## 1. 速记导图

```text
Memory 记忆组件（最简定义）
├── 01 记忆组件是什么        定义 / 七类记忆速查 / 短时 vs 长时
├── 02 记忆工作流            写入 / 召回 / 整合 + 决策树五问
└── 03 实战速查与误区         企业治理 / 记忆投毒 / 防御清单
```

## 2. 30 秒速查表

| 概念 | 一句话 |
|---|---|
| 记忆组件 | Agent 的存储层——跨轮/跨会话保留信息（大脑只管当下） |
| 短时记忆 | 上下文窗口（单会话）——LLM 大脑自带的"工作台" |
| 长时记忆 | 跨会话持久——事实/偏好/历史/经验（外部存储） |
| 七类记忆 | 工作/会话/语义/情景/程序/实体/摘要 |
| 记忆工作流 | 写入（提取沉淀）→ 召回（检索注入）→ 整合（巩固压缩） |
| 决策树五问 | 持久吗 → 跨会话吗 → 事实还是事件 → 怎么召回 → 检索策略 |
| 记忆投毒 | 攻击者把恶意内容持久化进长时存储——跨会话持续生效（比会话注入危险） |
| 治理三件套 | 来源标记 + 版本/审计 + 租户隔离（记忆是治理对象） |

## 3. 学习路径

| 路径 | 目标 |
|---|---|
| 速记（10 分钟） | 本体系 01-03——概念入门 |
| 深化（1 天） | [Memory 记忆系统](..%2F..%2F..%2FAgent%20子组件专项学习%2FMemory%20记忆系统%2F00-Memory记忆系统总览.md) 11 篇——完整架构 |
| 安全（半天） | 记忆安全与治理（本体系 03 + Memory 08 篇） |

## 4. 2026 关键事实

> 📅 基准窗口：2026-08。详见各篇【参考来源】。

- **记忆是正式学科**：2026 共识——"成功更少取决于更大的上下文窗口，更多取决于治理架构（存储/巩固/验证/随时间检索）"（SitePoint）；记忆工程（memory engineering）成为独立领域。
- **架构演进**：类型化记忆对象（ENGRAM/Hindsight 认识论类型/MemInsight）让检索精确；检索算子超越 top-k（混合候选 + 图算子 + 时间算子 + 融合重排）；层次整合（TiMem 时间层级/HiMem 场景层级，LoCoMo 80.71% vs Mem0 68.74%）；延迟批量合成省 18x 写入 token（StructMem）。
- **记忆投毒成为顶级威胁**：**sleeper memory poisoning**（2026-05）在 GPT-5.5 达 **99.8%** 工具级注入率、检索率 94-98%；MAFIA 证明写时审计 + 检索时检测**不足**；向量库注入 100% 持久；多模态投毒（Lucid）61.6% 成功率——**比会话注入危险（持久跨会话）**。
- **防御共识**：外部内容不可直接授权记忆写入（显式确认 + 来源标记）；记忆隔离（按用户/会话分库）；检索后验证（指令层级优先于记忆内容）；记忆治理（来源/版本/审计/RBAC/保留策略）；OWASP 记忆隔离模式（新记忆先验证再晋升长时）。
- **治理**：记忆成为企业治理对象（Atlan/MintMCP）——"向量库问题"视角是陷阱（向量检索不告诉你哪个权威/过期/租户可读）；Git 式记忆工作流（公司所有、可审、可移植）。
- **与体系分工**：[Memory 记忆系统](..%2F..%2F..%2FAgent%20子组件专项学习%2FMemory%20记忆系统%2F00-Memory记忆系统总览.md) 深潜完整架构；本体系只做最简速记；[Guardrails](..%2F..%2F进阶工程化组件（生产环境必备）%2F安全护栏%20Guardrails%2F00-安全护栏Guardrails总览.md) 管记忆读写拦截（本体系 03 篇投毒防御联动）。

---

**下一模块**：[01-记忆组件是什么](01-记忆组件是什么.md)

## 参考来源

- [The New Reality of Agent Memory: The Complete Guide (2026)（SitePoint）](https://www.sitepoint.com/ai-agent-memory-guide/)
- [Choosing the Right AI Agent Memory Strategy: A Decision-Tree Approach（MachineLearningMastery）](https://machinelearningmastery.com/choosing-the-right-ai-agent-memory-strategy-a-decision-tree-approach/)
- [MEMTIER: Tiered Retrieval for Long-Running LLM Agents（MDPI）](https://www.mdpi.com/1999-4893/19/7/607/xml)
- [Long-Term Memory for AI Agents（MintMCP）](https://docs.mintmcp.com/blog/long-term-memory-ai-agents)
- [Enterprise Memory for AI Agents: The Governed Substrate（Atlan）](https://atlan.com/know/what-is-enterprise-memory/)
- [Sleeper Memory Poisoning in LLM Agents（promptfoo Security DB）](https://www.promptfoo.dev/lm-security-db/vuln/sleeper-memory-poisoning-in-llm-agents-9f001ee2/)
- [MemPoison: Persistent Memory Threats in LLM Agents（arXiv 2607.14651）](https://arxiv-org.ezproxy.obspm.fr/html/2607.14651v1)
- [MAFIA: Query-Only Memory Attacks（arXiv 2608.03844）](https://arxiv-org.ezproxy.obspm.fr/html/2608.03844v1)
- [agentic-memory: Academic & Industry Analysis（GitHub）](https://github.com/lhl/agentic-memory/blob/main/ANALYSIS-academic-industry.md)
