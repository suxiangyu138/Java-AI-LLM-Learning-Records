# LLM-as-Judge 深潜

> LLM-as-Judge = 用模型按 rubric 给 Agent 输出打分——2026 默认评分器（LangSmith/Braintrust/Phoenix/DeepEval 均内建）。但它有**四失败模式**与严格的最佳实践：混合 judge 家族、分解式 rubric、锁定版本、分层 tier。本章给 judge 技术完整深潜。

## 1. 四大失败模式

| 失败模式 | 机制 | 数据 |
|---|---|---|
| 长度偏见 | 更长更详细的答案得分更高，与正确性无关 | >90% 偏好长答案 |
| 位置偏见 | 评估顺序影响分数 | 70% 首位偏好；~10-15% 翻盘率 |
| 自判偏见 | 生产模型与 judge 同族——自我偏好 | 模型给自己的族打分膨胀 |
| 延迟/成本 | 全量跑 judge 太慢太贵 | 在线场景需毫秒级分类器替代 |

> ⚠️ **约 80% 人机一致是聚合值，不是逐任务保证**（2026 共识）——具体任务的一致率波动大，逐任务验证是必需动作。

## 2. 2026 最佳实践

| 实践 | 做法 |
|---|---|
| 混合 judge 模型 | 高利害评分用跨家族 judge——不让 Agent 自评 |
| 分解式 rubric | G-Eval 链式思维模式——"0-1 打分帮助度"不是 rubric |
| 锁定一切 | judge 模型/版本/温度/rubric 文本全部固定——**升级 judge 会漂移分数** |
| 分层 tier | 便宜确定检查 → 训练分类器 → 前沿 judge 仲裁争议 |
| 采样 G-Eval | 按失败信号/长度/段采样——不全量跑 |
| 校准 | 人类纠正当 ground truth → few-shot 校准 → 一致性追踪（08 篇） |

> 🎯 核心要点：judge 的可靠性公式——**rubric 质量 × 模型混合 × 版本锁定 × 人类校准**。四者任一缺失，judge 分数都是"看起来像数"的噪声。

## 3. 分层评估 tier（2026 架构）

```text
评估 tier（按成本与精度递增）：
  Tier 1：确定性检查（正则/schema/余弦）——毫秒级，fail-fast 闸门
  Tier 2：训练分类器（Turing/Galileo Luna-2 级）——高容量语义打分
  Tier 3：前沿 BYOK judge——只仲裁 Tier 2 争议分数
```

| Tier | 成本 | 精度 | 用途 |
|---|---|---|---|
| T1 确定性 | ~0 | 低（客观项） | 格式/schema/规则闸 |
| T2 分类器 | 毫秒级 | 中 | 高容量在线打分 |
| T3 前沿 judge | 高 | 高 | 争议仲裁/抽样深评 |

> 💡 分层价值：**在线场景不能全量跑 LLM judge**（延迟+成本）——T2 分类器覆盖 95% 打分，T3 只处理争议；这是"评估成本第一约束"（00 篇）的架构解。

## 4. 分解式 rubric 设计

| 反例 | 正例 |
|---|---|
| "按 0-1 打分回答的帮助度" | "按以下维度打分：① 是否直接回答用户问题 ② 是否使用了最新数据 ③ 语气是否专业 ④ 是否包含不支持的断言……每维度 1-5 分" |
| 单一全局分 | 维度分解 + 权重 + 判定标准（G-Eval 模式） |

> 🎯 核心要点：**rubric 是 judge 的"检查清单"**——维度分解让 judge 逐项核查而不是凭感觉打分；G-Eval 的链式思维模式（先列维度再逐维评）是 2026 标准。

## 5. Judge 新研究（2026）

| 研究 | 贡献 |
|---|---|
| LRBench + Judge-R1（ACL 2026） | 10 万样本长上下文 judge 基准（六原则：逻辑/事实/偏见/接地/帮助/无害）；SOTA judge 在长上下文微妙错误上挣扎；RL+多轮搜索优于单轮基线 |
| TIR-Judge（ICLR 2026） | 工具集成 RL 训练 judge（Python 执行器验证）——点式超越推理型 6.4%、配对式 7.7%；**8B 参数匹配 Opus-4 listwise** |

> ⚠️ 前沿信号：**"验证型 judge"（工具集成）正在超越"推理型 judge"**——让 judge 执行代码/查库再判分（03 篇执行验证同源）；8B 级模型匹配前沿，说明 judge 的瓶颈在验证机制不在参数规模。

## 6. 生产 judge 运行规范

| 规范 | 内容 |
|---|---|
| 版本固定 | judge 模型+版本+温度+rubric 文本——升级走变更流程 |
| 一致性监控 | judge 自一致性漂移检测（同输入多次打分方差） |
| 校准基线 | 人类标注子集持续比对（人机一致率趋势） |
| 成本预算 | Trust Tax 入账（观测 06 篇）——分层 tier 控成本 |
| 采样策略 | 按失败信号/长度/段采样（tail-based 同源） |

> 💡 与观测 06 篇衔接：judge 分数写 span 属性（gen_ai 语义）——**评估与可观测共享同一数据平面**；judge 版本也要进 span，否则分数跨版本不可比。

## 7. 常见误区

| 误区 | 真相 |
|---|---|
| "judge 分数可信" | 四偏见 + 80% 聚合值非逐任务保证 |
| "同族模型当 judge" | 自判偏见——跨家族混合 |
| "rubric 一句话就行" | 分解式 rubric 是必须——G-Eval 模式 |
| "judge 升级无感" | 版本漂移分数——锁定一切 |
| "全量跑 judge" | 延迟成本不可控——分层 tier |
| "judge 越大越准" | TIR-Judge 8B 匹配 Opus-4——验证机制比规模重要 |
| "校准一次就够" | 持续校准——人机一致率趋势监控 |

## 8. 面试速记

| 问题 | 一句话答案 |
|---|---|
| 四失败模式？ | 长度/位置/自判/延迟成本 |
| 自判偏见？ | 生产模型与 judge 同族——跨家族混合 |
| 分解式 rubric？ | G-Eval 链式：先列维度再逐维评 |
| 锁定什么？ | judge 模型/版本/温度/rubric 文本 |
| 分层 tier？ | 确定性→分类器→前沿 judge 仲裁 |
| 为什么分层？ | 在线不能全量 LLM judge——T2 覆盖 T3 仲裁 |
| 80% 人机一致？ | 聚合值非逐任务保证——逐任务验证 |
| TIR-Judge？ | 工具集成 RL judge——8B 匹配 Opus-4 listwise |
| 校准？ | 人类纠正当 ground truth → few-shot → 一致性追踪 |
| 与观测集成？ | 分数写 span 属性 + judge 版本进 span |
| 验证型 vs 推理型 judge？ | 工具集成验证超越推理——瓶颈在验证机制 |
| 采样 G-Eval？ | 按失败信号/长度/段采样，不全量 |

---

**下一模块**：[03-轨迹评估：工具选择与顺序](03-轨迹评估：工具选择与顺序.md)　**返回总览**：[00-Feedback & Evaluation 评估模块总览](00-FeedbackEvaluation评估模块总览.md)

## 参考来源

- [G-Eval vs DeepEval Metrics in 2026（FutureAGI）](https://futureagi.com/blog/g-eval-vs-deepeval-metrics-2026/)
- [LRBench and Judge-R1: Principled Evaluation of LLM-Based Judges（ACL 2026 Findings）](https://aclanthology.org/2026.findings-acl.2029/)
- [Incentivizing Agentic Reasoning in LLM Judges via Tool-Integrated RL（ICLR 2026）](https://mlanthology.org/iclr/2026/xu2026iclr-incentivizing/)
- [How to Calibrate LLM-as-Judge with Human Corrections（LangChain）](https://www.langchain.com/resources/llm-as-a-judge)
- [Multi-Turn LLM Evaluation in 2026（FutureAGI）](https://futureagi.com/blog/multi-turn-llm-evaluation-2026/)
