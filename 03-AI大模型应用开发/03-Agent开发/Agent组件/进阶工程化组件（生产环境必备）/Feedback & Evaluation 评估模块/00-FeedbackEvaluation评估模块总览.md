# Feedback & Evaluation 评估模块知识体系总览

> 定位：Agent 生产工程化组件之「评估模块」——回答"Agent 做得好不好"的工程：LLM-as-Judge 技术、轨迹评估、数据集构建、框架选型、在线反馈环。2026 核心共识：**输出评测不够，轨迹是评估单位**（"正确输出可以掩盖错误的推理"）；**评估是持续流水线**（上个月通过的 eval 说明不了这周）；**同名指标不可互换**（不同 judge 提示词产出不同分数）。总览做索引与速查，子主题各一篇。

## 📚 目录

1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [学习路线推荐](#3-学习路线推荐)
4. [核心概念速查](#4-核心概念速查)
5. [2026 生态基准](#5-2026-生态基准)

## 1. 知识体系导图

```text
Feedback & Evaluation 评估模块
├── 01 评估全景：为什么输出评测不够          输出 vs 轨迹 / 评估定位 / 四类度量
├── 02 LLM-as-Judge 深潜                     四失败模式 / 分解 rubric / 分层 tier
├── 03 轨迹评估：工具选择与顺序              TRAJECT-Bench / 轨迹指标 / 执行验证
├── 04 多轮评估：会话级质量                  tau-bench / pass^k / 会话指标
├── 05 评估数据集：从黄金集到生产种子         golden 局限 / 失败 trace 转用例 / 版本化
├── 06 评估指标体系                          指标分类 / 同名不可互换 / 选型
├── 07 评估框架：DeepEval vs Promptfoo vs Ragas  对比 / 成本 / eval-driven
├── 08 在线评估与反馈环                      采样 / 标注队列 / judge 校准 / 闭环
├── 09 评估工程化：CI 门禁与持续评估         minPassRate / 版本化 / 观测集成
└── 10 生产冲刺：落地清单与面试              12 避坑 / 面试题
```

## 2. 模块导航

| 序号 | 模块 | 核心内容 | 适合人群 |
|---|---|---|---|
| 01 | [评估全景：为什么输出评测不够](01-评估全景：为什么输出评测不够.md) | 输出 vs 轨迹、评估定位 | 全部（地基） |
| 02 | [LLM-as-Judge 深潜](02-LLM-as-Judge深潜.md) | 四失败模式、最佳实践、分层 tier | Agent 工程师 |
| 03 | [轨迹评估：工具选择与顺序](03-轨迹评估：工具选择与顺序.md) | TRAJECT-Bench、轨迹指标、执行验证 | Agent 工程师 |
| 04 | [多轮评估：会话级质量](04-多轮评估：会话级质量.md) | 多轮基准、会话指标、pass^k | 评测关注者 |
| 05 | [评估数据集：从黄金集到生产种子](05-评估数据集：从黄金集到生产种子.md) | golden 局限、失败 trace 转用例 | 评测关注者 |
| 06 | [评估指标体系](06-评估指标体系.md) | 指标分类、同名陷阱、选型 | 全部 |
| 07 | [评估框架：DeepEval vs Promptfoo vs Ragas](07-评估框架：DeepEval-vs-Promptfoo-vs-Ragas.md) | 三框架对比、成本、选型 | 选型决策者 |
| 08 | [在线评估与反馈环](08-在线评估与反馈环.md) | 采样、标注队列、judge 校准、闭环 | 落地开发者 |
| 09 | [评估工程化：CI 门禁与持续评估](09-评估工程化：CI门禁与持续评估.md) | eval-driven、门禁、观测集成 | 落地开发者 |
| 10 | [生产冲刺：落地清单与面试](10-生产冲刺：落地清单与面试.md) | 12 避坑、面试题、落地清单 | 面试/上线前 |

## 3. 学习路线推荐

| 路线 | 路径 | 目标 |
|---|---|---|
| 入门（1 天） | 01 → 06 → 07 → 10 | 能搭第一个评估套件 |
| 进阶（1 周） | 01-03 → 05 → 07 → 10 | 能做轨迹评估与数据集构建 |
| 高级（2 周） | 全量 + 04 → 08-09 | 能做多轮评估与在线闭环 |

## 4. 核心概念速查

| 概念 | 一句话定义 |
|---|---|
| 轨迹评估 | 以工具调用轨迹为评估单位（选择/参数/顺序），不只最终答案 |
| LLM-as-Judge | 用模型按 rubric 给输出打分——2026 默认评分器 |
| 四失败模式 | 长度偏见/位置偏见/自判偏见/延迟成本 |
| 分解式 rubric | G-Eval 链式思维模式——"0-1 打分帮助度"不是 rubric |
| 分层评估 tier | 确定性检查 → 训练分类器 → 前沿 judge（仲裁争议） |
| 执行验证 | 检查数据库状态/跑测试套件——优于工具调用语法或最终文本检查 |
| TRAJECT-Bench | ICLR 2026 轨迹感知基准：工具选择/参数化/顺序正确性 |
| 工具选择指标 | 选择准确率 + "irrelevance bucket"（不该调时调了） |
| pass^k | 同一任务重复 k 次全通过的比例——非确定性度量（tau2-bench） |
| 会话级指标 | Conversation Completeness/Knowledge Retention 等——单轮平均会骗人 |
| 生产种子数据集 | 失败 trace 转测试用例，ground truth 含工具调用与推理步骤 |
| 同名指标陷阱 | "DeepEval 的 Faithfulness ≠ Ragas 的 Faithfulness"——锁定版本 |
| Eval-driven development | 评估套件即工作规范、分数即发布判定、PR 按分数差门禁 |
| minPassRate | CI 门禁：通过率阈值（如 0.95）、maxRegressions 0 |
| judge 校准 | 人类纠正当 ground truth → few-shot 校准 → 一致性追踪 |
| 评估飞轮 | 生产 trace → 自动打分 → 人工评审 → 数据集 → 改进 → 回归 |

## 5. 2026 生态基准

> 📅 基准窗口：2026-08。版本与事实以各模块【参考来源】为准。

- **轨迹成为评估单位**：TRAJECT-Bench（ICLR 2026）检验工具"是否被正确选择、参数化、排序"——诊断出"相似工具混淆"与"参数盲目选择"失败模式；2026 共识金句：**"正确输出可以掩盖错误的推理"**（LangChain/业界共识）。
- **Judge 技术成熟**：四失败模式编目（长度/位置/自判/延迟成本）；最佳实践=混合 judge 家族 + 分解 rubric + **锁定 judge 模型/版本/温度/rubric**（升级 judge 会漂移分数）；分层 tier（确定性检查→训练分类器→前沿 judge 仲裁）；TIR-Judge（ICLR 2026）工具集成 RL judge 超越推理型基线 6.4-7.7%（8B 匹配 Opus-4 listwise）。
- **标准 pass/fail 漏掉 65-93% 安全问题**——执行验证（检查数据库状态/跑测试）优于最终文本检查；代理状态评估（ACL 2026 Industry）：LLM 状态追踪器 + judge 验证目标完成，人机一致 >90%。
- **框架三分天下**：DeepEval v3.9.7（2025-12：agent 指标 + 多轮合成 goldens，pytest 原生）/ Promptfoo（2026-03-09 被 OpenAI 收购保持 MIT，红队 40+ 插件）/ Ragas（指标库非平台，RAG 参考实现 + tool call accuracy/F1）；**"跑哪两个"而非"哪个最好"**；同名指标不可互换。
- **多轮评估标准化**：tau-bench（客服+数据库状态验证）、tau2-bench（pass^k 一致性指标成为 2026 标准）、MultiChallenge 2026 更新（Gemini 2.5 Pro judge，+5pp 人机一致）；单轮平均是错误做法——会话级指标 + CI 门禁。
- **数据集工程化**：golden sets 结构性局限（生产流量≠测试集、无 ground truth）；**生产种子**——失败 trace 转测试用例（ground truth 含工具调用与推理步骤）；未来派"Error Feed"周更失败样本防数据集老化。
- **在线反馈闭环**：标注队列 + judge 校准（人类纠正当 ground truth）+ Review Apps（MLflow 3）；闭环系统实证——PsiArena（AAAI 2026）三边反馈自反思 **+141%**、AutoLibra（ICLR 2026）开放式反馈自动转评估指标、GEN-EVALLM RL 对齐。
- **评估成本现实**：DeepEval 全指标套件 ~$0.02-0.04/样本、Ragas ~$0.02-0.05/样本（10K traces/天 ≈ $200-600/月 GPT-4o 级）；便宜 judge 省 60-80%——**成本是评估工程第一约束**（观测 06 篇 Trust Tax 联动）。
- **与体系分工**：[观测&可观测组件](..%2F观测%26可观测组件%2F00-观测可观测组件总览.md) 06 篇讲"评估在可观测栈的位置"（遥测契约/三级粒度）；本体系深潜"评估模块本体"（judge 技术/轨迹/数据集/框架/闭环）；[RAG 08 篇](..%2F..%2F..%2F..%2F04-RAG检索增强生成%2F08-RAG评估与质量保障.md) 管检索侧评估；[Reflection 08 篇](..%2F..%2F..%2FAgent%20子组件专项学习%2FReflection%20反思模块%2F08-反思评测：三指标方法论.md) 管反思组件评测。

---

**下一模块**：[01-评估全景：为什么输出评测不够](01-评估全景：为什么输出评测不够.md)

## 参考来源

- [AI Agent Evaluation (2026): Metrics, Frameworks, and Production Failures（MorphLLM）](https://www.morphllm.com/ai-agent-evaluation)
- [Multi-Turn LLM Evaluation in 2026: A Practical Guide（FutureAGI）](https://futureagi.com/blog/multi-turn-llm-evaluation-2026/)
- [TRAJECT-Bench: A Trajectory-Aware Benchmark for Evaluating Agentic Tool Use（ICLR 2026）](https://iclr.cc/virtual/2026/poster/10009304)
- [Promptfoo vs DeepEval vs RAGAS: 2026 Comparison（GenAI.QA）](https://genai.qa/blog/promptfoo-vs-deepeval-vs-ragas/)
- [The Definitive Guide to AI Agent Evaluation (2026)（FutureAGI）](https://futureagi.com/blog/definitive-guide-ai-agent-evaluation-2026/)
- [Toward Scalable Verifiable Reward: Proxy State-Based Evaluation（ACL 2026 Industry）](https://aclanthology.org/2026.acl-industry.87/)
- [AutoLibra: Agent Metric Induction from Open-Ended Human Feedback（ICLR 2026）](https://iclr.cc/virtual/2026/poster/10011573)
- [How to Calibrate LLM-as-Judge with Human Corrections（LangChain）](https://www.langchain.com/resources/llm-as-a-judge)
