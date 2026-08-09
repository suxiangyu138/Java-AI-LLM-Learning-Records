# Planner 规划器知识体系总览

> 定位：Agent 子组件专项之「规划器」——[四大核心组件](..%2F..%2FAgent%20四大核心组件%2F03-规划组件：任务分解与行动编排.md)规划组件的深化篇：从任务分解讲到规划表示、执行模式、失败治理与评测。2026 年核心：长程规划仍是最弱环节，验证增强与分层分解是破局方向。总览做索引与速查，子主题各一篇。

## 📚 目录

1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [学习路线推荐](#3-学习路线推荐)
4. [核心概念速查](#4-核心概念速查)
5. [2026 生态基准](#5-2026-生态基准)

## 1. 知识体系导图

```text
Planner 规划器
├── 01 规划器全景：规划在 Agent 中的角色      职责边界 / 与决策分工 / 失败成本
├── 02 任务分解：规划的核心技能              分解粒度 / 分解方法 / MAP 模块化
├── 03 规划表示：计划的数据结构              计划 JSON 契约 / 状态机 / DAG
├── 04 规划执行模式：Plan-Execute vs ReWOO    静态 vs 动态 / 选型矩阵 / PER 循环
├── 05 重规划：观察驱动的计划修正            触发信号 / 部分重规划 / 目标锚定
├── 06 规划器的失败模式与治理                级联失败 / 幻觉 / 进度误判 / 验证增强
├── 07 规划评测：从 PlanBench 到生产          PlanningBench / PlanBench-XL / 自建集
├── 08 规划器工程化：提示词与微调             规划提示词 / SFT-GRPO / QLoRA / 选型
├── 09 规划器框架与实现                      LangGraph / 手写规划器 / MAP 架构
└── 10 生产实践与面试冲刺                    预算 / 12 避坑 / 面试题
```

## 2. 模块导航

| 序号 | 模块 | 核心内容 | 适合人群 |
|---|---|---|---|
| 01 | [规划器全景](01-规划器全景.md) | 职责边界、与决策分工、失败成本 | 全部（地基） |
| 02 | [任务分解：规划的核心技能](02-任务分解：规划的核心技能.md) | 分解粒度、方法、MAP 模块化 | Agent 工程师 |
| 03 | [规划表示：计划的数据结构](03-规划表示：计划的数据结构.md) | 计划 JSON 契约、状态机、DAG | 落地开发者 |
| 04 | [规划执行模式](04-规划执行模式：Plan-Execute与ReWOO.md) | 静态 vs 动态、选型矩阵、PER | Agent 工程师 |
| 05 | [重规划：观察驱动的计划修正](05-重规划：观察驱动的计划修正.md) | 触发信号、部分重规划、目标锚定 | Agent 工程师 |
| 06 | [规划器的失败模式与治理](06-规划器的失败模式与治理.md) | 级联/幻觉/进度误判、验证增强 | 架构师 |
| 07 | [规划评测](07-规划评测：从PlanBench到生产.md) | PlanningBench/XL、评测协议、自建集 | 评测关注者 |
| 08 | [规划器工程化：提示词与微调](08-规划器工程化：提示词与微调.md) | 提示词模板、SFT-GRPO、QLoRA | 落地开发者 |
| 09 | [规划器框架与实现](09-规划器框架与实现.md) | LangGraph/手写/MAP 架构 | 落地开发者 |
| 10 | [生产实践与面试冲刺](10-生产实践与面试冲刺.md) | 预算、12 避坑、面试题 | 面试/上线前 |

## 3. 学习路线推荐

| 路线 | 路径 | 目标 |
|---|---|---|
| 入门（1 天） | 01 → 03 → 04 → 10 | 理解规划器职责与计划数据结构 |
| 进阶（1 周） | 01-02 → 04 → 05 → 06 → 10 | 能设计任务分解与重规划闭环 |
| 高级（2 周） | 全量 + 07 → 08 → 09 | 能评测规划质量、选择微调路线 |

## 4. 核心概念速查

| 概念 | 一句话定义 |
|---|---|
| 规划器（Planner） | 把目标分解为可执行步骤的组件（决策的"参谋部"） |
| 任务分解 | 把长目标拆成原子自包含步骤的核心技能 |
| 规划表示 | 计划的数据结构（步骤/依赖/状态的 JSON 契约） |
| 计划状态机 | 步骤状态 pending/running/done/failed |
| DAG 计划 | 有依赖关系的步骤图（ReWOO 的表示） |
| Plan-and-Execute | 规划与执行分离（规划器出步骤，执行器干活） |
| ReWOO / Blueprint | 一次性规划出工具 DAG 再执行（静态，~2 次 LLM 调用） |
| 重规划（Replan） | 执行中观察驱动地修正计划 |
| 目标锚定 | 每轮重规划注入原始目标（防 goal drift） |
| 级联失败 | 早期一步错，后续全错（无事务语义） |
| 动作 schema 幻觉 | 模型生成看似合理实则错误的动作序列 |
| 进度误判 | 不知道自己走到哪（提前终止或重复循环） |
| 验证增强规划 | 中间步骤对照约束检查（verification-augmented） |
| PlanningBench | 2026 开源规划基准（467 实例/6 类场景/逐步验证） |
| PlanBench-XL | 大规模工具生态长程规划基准（327 任务/1665 工具） |
| 约束耦合 | 多约束叠加时规划成功率骤降（2026 关键发现） |

## 5. 2026 生态基准

> 📅 基准窗口：2026-08。版本与事实以各模块【参考来源】为准。

- **规划是 Agent 最弱环节**：2026 综合调查（200+ 论文）——LLM 规划器在长程任务上显著退化；编排器 PlanBench 约 55%、自主 Agent 多步任务约 50%、开放网页导航 14% vs 人类 78%；**Gartner 预测 >40% Agentic 项目 2027 前被取消，规划不可靠是核心原因**。
- **失败三分法**（调查共识）：级联失败（早期错误传播，无事务语义/回滚）、动作 schema 幻觉（类型复杂场景生成貌似合理的错误动作）、进度误判（提前终止或冗余循环）——破局方向：验证增强规划 + 分层分解（明确成功标准）。
- **基准体系成形**：PlanningBench（2026-06，腾讯混元+人大：467 实例/6 类场景/30+ 任务类型/约束清单逐步验证，GPT-5.4 全通过 63.17%、耦合约束下成功率骤降）；PlanBench-XL（2026-06：327 零售任务/1665 工具，无阻塞 51.90% → 严重阻塞 11.36%）；TRAJECT-Bench（工具选择/参数/依赖三元正确性）。
- **微调路线成熟**：Planner 专属 SFT+GRPO（AAAI 2026，工具选择准确率 SOTA）；MagicAgent（SFT+多目标 RL，32B 达 75.1% Worfbench/86.9% BFCL-v3）；**QLoRA 小模型**（Gemma E4B/Qwen3-4B 微调 ~1700 例：描述免推理、输入 -82.6%）；VOLTS 逐 token 验证（小模型 76% 合法计划 vs GPT-4o 7%）。
- **任务分解实证**：MAP（Mila 模块化规划器：冲突监控/状态预测/状态评估/分解/协调五模块）——LLM 单独能做各模块，但自主协调是瓶颈；Mano-P 部署观察：**分解与计划调整能力比单步操作准确率更决定任务完成率**。
- **生产现实**：窄域有界任务已可用；长程+跨会话记忆+多 Agent 协调仍差 12-24 个月成熟——**工程上先做"窄而稳"**。
- **与体系分工**：[四大核心组件](..%2F..%2FAgent%20四大核心组件%2F03-规划组件：任务分解与行动编排.md) 03 篇讲规划组件的概览与选型矩阵，本体系深潜"规划器本体"（分解/表示/失败/评测/微调）。

---

**下一模块**：[01-规划器全景](01-规划器全景.md)

## 参考来源

- [What 200+ Papers Reveal About Why AI Agents Still Fail: 2026 Academic Synthesis（Agent Market Cap）](https://agentmarketcap.ai/blog/2026/04/08/llm-reasoning-autonomous-agents-2026-academic-synthesis-arxiv)
- [PlanningBench: Generating Scalable and Verifiable Planning Data（arXiv）](https://www.chatpaper.ai/dashboard/paper/2ef80634-20fa-4848-9a0a-36c060411f31)
- [PlanningBench Benchmark Scores & Leaderboard（BenchmarkList）](https://benchmarklist.com/benchmarks/planningbench/)
- [PlanBench-XL: Long-Horizon Planning in Large Tool Ecosystems（arXiv 2606.22388）](https://arxiv-org.ezproxy.obspm.fr/html/2606.22388v1)
- [Modular Agentic Planner（Mila，Taylor Webb）](https://mila.quebec/en/directory/taylor-webb)
- [Beyond ReAct: A Planner-Centric Framework（AAAI 2026）](https://ojs.aaai.org/index.php/AAAI/article/view/40676)
- [MagicAgent: Towards Generalized Agent Planning（arXiv 2602.19000）](https://papers.cool/arxiv/2602.19000)
- [VOLTS: Validated Output through Logit Tree Search（PMLR）](https://proceedings.mlr.press/v318/massad26a.html)
- [TRAJECT-Bench（ICLR 2026）](https://mlanthology.org/iclr/2026/he2026iclr-trajectbench/)
