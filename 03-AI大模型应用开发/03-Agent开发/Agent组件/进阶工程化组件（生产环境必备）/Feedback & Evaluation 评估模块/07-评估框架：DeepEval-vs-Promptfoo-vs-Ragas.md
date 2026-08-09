# 评估框架：DeepEval vs Promptfoo vs Ragas

> 框架 = 评估的"工具链"。2026 三雄：**DeepEval（pytest 原生）/ Promptfoo（CLI/YAML + 红队）/ Ragas（指标库非平台）**——选型共识是**"跑哪两个"而非"哪个最好"**；Agent 评估 DeepEval 当前占优（v3.9.7 agent 指标），Ragas 补工具调用正确性，Promptfoo 补红队与多模型对比。本章给三框架深潜与选型。

## 1. 三框架定位

| 维度 | DeepEval | Promptfoo | Ragas |
|---|---|---|---|
| 出身 | Confident AI | OpenAI 收购（2026-03-09，保持 MIT） | Vibrant Labs |
| 形态 | pytest 原生框架 | CLI/YAML 测试框架 | **指标库（非平台）** |
| 许可 | Apache 2.0 | MIT | Apache 2.0 |
| 哲学 | 单元测试式断言 | 提示词矩阵 + 红队 | RAG 指标参考实现 |
| 亮点 | 50+ 指标、G-Eval、agent 指标 | 多模型对比、40+ 红队插件 | 合成测试数据、RAG 全指标 |

> 🎯 核心要点：**DeepEval 是框架（断言+CI）、Ragas 是库（指标）、Promptfoo 是测试器（矩阵+红队）**——形态不同决定集成方式不同：DeepEval 进 pytest、Promptfoo 进 YAML 配置、Ragas 自己写 harness。

## 2. Agent 化能力对比（2026）

| 能力 | DeepEval | Promptfoo | Ragas |
|---|---|---|---|
| 多轮评估 | ✅ | ✅ | ✅（v0.2+） |
| Agent 轨迹 | ✅（preview） | ❌（非一等） | ❌ |
| Agent 指标 | ✅（v3.9.7：agent metrics + 多轮合成 goldens） | ❌ | ✅（tool call accuracy/F1、goal accuracy） |
| 工具调用正确性 | ✅（preview） | ❌ | ✅ |
| 红队/注入 | ✅（基础） | ✅（40+ 插件，最强） | ❌ |

> ⚠️ 2026 结论：**Agent 化评估 DeepEval 领先**（v3.9.7 2025-12 发布 agent 指标 + 多轮合成 goldens）；Ragas 有 agent 指标但缺轨迹；**Promptfoo 的最终答案评分会漏工具选择/重试/会话漂移**（常见坑）——纯最终答案评分的框架不完整。

## 3. 成本对比（judge API 是主体）

| 框架 | 单样本成本 | 说明 |
|---|---|---|
| Promptfoo（基础断言） | ~$0 | 0 judge 调用 |
| Promptfoo（rubric+红队） | $0.005-0.02 | 1-3 次调用 |
| DeepEval | $0.005-0.01/指标 | 4 指标全套 ~$0.02-0.04 |
| Ragas（全 RAG 套件） | $0.02-0.05 | 3-5 次调用 |

> 💡 规模换算：**10K RAG traces/天 ≈ $200-600/月**（GPT-4o 级 judge）；便宜 judge（Haiku/mini 级）省 60-80%——**评估成本第一约束**（00 篇）；与观测 06 篇 Trust Tax 联动。

## 4. Eval-Driven Development（2026 主流工作流）

| 环节 | 做法 |
|---|---|
| 评估套件即规范 | eval 套件是工作的"规格书"——分数是发布判定 |
| PR 门禁 | 每 PR 跑前后对比 eval——低于门禁失败构建 |
| 门禁参数 | `minPassRate 0.95`、`maxRegressions 0` |
| Promptfoo 集成 | GitHub Action：PR 评论交互视图 + LLM 调用缓存 |
| DeepEval 集成 | assert_test 低于阈值抛错——pytest CI 步骤 |

> 🎯 核心要点：**"不失败构建的评估框架是研究工具，不是生产评估"**（2026 共识）——门禁是评估工程化的分水岭；Promptfoo 的 PR 自动评论 + DeepEval 的 pytest 断言是两条成熟路线。

## 5. 选型决策："跑哪两个"

| 场景 | 组合 |
|---|---|
| RAG 为主 | Ragas（指标）+ DeepEval（G-Eval 自定义） |
| Python + pytest | DeepEval（最低摩擦 CI） |
| 多模型对比/红队 | Promptfoo（默认起步点） |
| Agent 化评估 | DeepEval（agent 指标）+ Ragas（工具调用 F1，若涉 RAG） |
| 成熟团队 | Ragas 诊断 + DeepEval 或 Promptfoo + 观测平台（Langfuse/LangSmith） |

> 💡 组合逻辑：**指标按域分工 + 平台按观测分工**——"框架管评估、平台管观测"；2026 主流是"两个框架 + 一个观测平台"。

## 6. 框架选型陷阱

| 陷阱 | 真相 |
|---|---|
| "框架=平台" | DeepEval 是框架、Ragas 是库、Confident AI 是平台——形态别混 |
| "一个框架全包" | 指标按域选——轨迹/红队/RAG 各有所长 |
| "只计框架成本" | judge token/重试/延迟/工程师小时才是主体 |
| "没门禁也行" | 不失败构建 = 研究工具 |
| "框架测了应用流程" | 框架只测你告诉它的——多租户隔离/UI/上下文对抗测不了 |
| "Promptfoo 评最终答案够" | 漏工具选择/重试/会话漂移 |
| "红队是安全的事" | Promptfoo 40+ 插件是构建期红队标配（Guardrails 08 联动） |

## 7. 面试速记

| 问题 | 一句话答案 |
|---|---|
| 三框架定位？ | DeepEval 断言框架/Ragas 指标库/Promptfoo 测试器 |
| Agent 评估谁领先？ | DeepEval v3.9.7（agent 指标 + 多轮合成 goldens） |
| Promptfoo 弱项？ | 无一等轨迹指标——最终答案评分漏工具选择 |
| Ragas 弱项？ | 指标库非平台——自带 harness |
| 单样本成本？ | DeepEval ~$0.02-0.04、Ragas ~$0.02-0.05 |
| 门禁参数？ | minPassRate 0.95、maxRegressions 0 |
| 不失败构建的框架？ | 研究工具不是生产评估 |
| 选型共识？ | "跑哪两个"——指标按域分工 |
| 2026 事件？ | OpenAI 收购 Promptfoo（保持 MIT） |
| 红队最强？ | Promptfoo 40+ 插件 |
| 成熟团队组合？ | Ragas + DeepEval/Promptfoo + 观测平台 |
| 框架测不到什么？ | 应用级流程/多租户/UI/上下文对抗 |

---

**下一模块**：[08-在线评估与反馈环](08-在线评估与反馈环.md)　**返回总览**：[00-Feedback & Evaluation 评估模块总览](00-FeedbackEvaluation评估模块总览.md)

## 参考来源

- [Promptfoo vs DeepEval vs RAGAS: 2026 Comparison（GenAI.QA）](https://genai.qa/blog/promptfoo-vs-deepeval-vs-ragas/)
- [Best Open-Source and OSS-Client LLM Eval Frameworks in 2026（FutureAGI）](https://futureagi.com/blog/best-open-source-eval-frameworks-2026/)
- [AI Evals Frameworks Compared (2026)（JobsByCulture）](https://jobsbyculture.com/blog/ai-evals-frameworks-compared-2026)
- [LLM Evaluation Frameworks Compared（MachineLearningMastery）](https://machinelearningmastery.com/llm-evaluation-frameworks-compared-how-to-actually-measure-what-your-model-does/)
