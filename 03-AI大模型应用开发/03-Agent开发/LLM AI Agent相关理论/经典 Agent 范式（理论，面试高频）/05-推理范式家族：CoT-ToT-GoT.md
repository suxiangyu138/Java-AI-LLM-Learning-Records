# 推理范式家族：CoT-ToT-GoT

> 理论篇：让模型"更会想"的推理范式家族——从思维链到树到图。2026 关键：**推理模型内建搜索改变了外部范式的算法**。

## 1. 家族地图

| 范式 | 结构 | 关键实证 | 2026 状态 |
|---|---|---|---|
| CoT（2022） | 线性链（中间步骤） | 多步算术大幅提升 | 前沿模型"推理内建"；小模型仍大增益 |
| Self-Consistency | N 条 CoT 采样投票 | "穷人版 ToT" | 仍有效（无评估器成本） |
| ToT（2023） | 树（分支+评估+搜索） | Game of 24：4%→74% | 推理模型内部搜索超越；外部留特定场景 |
| GoT（2023） | 图（聚合+反馈环） | +62% 质量、-31% 计算 | 复杂度高，特定任务 |
| PAL（2023） | 程序执行辅助 | GSM8K 72% vs CoT 65.6% | 数值/计算类任务 |
| Scratchpad | 显式中间工作区 | 加法 35%→95% | 长算术 |
| ReAct（2022） | 推理+行动 | Agent 奠基（02 篇） | 原生化 |

> 🎯 核心要点：**家族共同权衡——"探索深度 × 计算成本"**：线性链便宜但不能回溯；树/图能探索但贵（3-30x token）；2026 的理论问题是"在推理模型时代外部结构还有多少价值"。

## 2. CoT：2026 的微妙状态

| 场景 | CoT 的作用 |
|---|---|
| 前沿模型（GPT-5.5/Opus 4.7/Gemini 3.1） | 推理内建——显式"let's think step by step"不再帮助甚至有害（HumanEval -15pp） |
| 小模型 | 仍是大增益 |
| 可审计任务 | 需要可见推理轨迹 |
| 概念基础 | 一切现代推理范式的奠基 |

> ⚠️ 2026 调查结论（Chen 等）：**Long CoT vs Short CoT 二分**——Long CoT 特征为深度推理/广泛探索/可行反思，支持比浅链更复杂的任务；但 overthinking（冗余长轨迹）是效率主题（SwiReasoning 限思考块切换）。

## 3. ToT：树搜索深潜

| 要素 | 说明 |
|---|---|
| 节点 | 部分思想（partial thought） |
| 生成器 | 提出候选分支 |
| 评估器 | 打分（LLM judge/确定性检查） |
| 搜索 | BFS/DFS/beam——剪枝扩展 |

| 2026 算法变化 | 说明 |
|---|---|
| 推理模型内部搜索 | 前向传播内做树搜索（数千 thinking tokens）——同预算下通常打败外部 ToT |
| 外部 ToT 保留场景 | ① 可观察分支（用户需比较备选）② 可转向搜索（独立评估器不同偏见）③ 搜索即 API（已有工具循环）④ 成本上限（内部搜索无界） |

> 💡 生产模式（2026）：**级联**（CoT 先行、低置信升 ToT，上限 b=3/k=2/d=2）；**计划级光束**（ToT 出 3-5 候选计划、评估取 2、每支单 CoT）；**自一致性**（N 条 CoT 投票——多数场景补齐差距）；**确定性评估器优先**（schema/正则/单测——便宜一个数量级）。

## 4. ToT 三 yes 决策规则（2026 实用指南）

| 问题 | 倾向 ToT |
|---|---|
| CoT 在你评估集 <50%？ | ✅ |
| 失败模式是前 1-2 步错误承诺？ | ✅ |
| 能写出忠实评估器 + 容忍 3-30x token？ | ✅ |

> ⚠️ **两个或更少 yes：留在 CoT**——ToT 不是免费升级（CoT 已强的任务上 ToT 是成本-正确率回归）；常见错误：评估器未校准（弱 judge 选最差分支与最好一样频繁）、分支太宽（b=8-10 不如 3-5）、太深（d>4 平台期）、无终止规则。

## 5. 2026 前沿

| 方向 | 机制 |
|---|---|
| SwiReasoning（ICLR 2026） | 显式 CoT ↔ 隐式潜空间切换（熵引导置信）——+1.8-3.1%、token 效率 +57-79% |
| FoT（SURGeLLM 2026） | 通用推理框架（ToT/GoT/ProbTree 统一 + 超参调优 + 并行 + 缓存） |
| TSE | 多链生成新节点——探索解空间盲区 |
| ARCHE（AAAI 2026） | 潜在推理链提取（推理逻辑树）——**无当前 LLM 能提取完整科学论证标准链** |

## 6. 面试速记

| 问题 | 一句话答案 |
|---|---|
| 家族成员？ | CoT/SC/ToT/GoT/PAL/Scratchpad/ReAct |
| 共同权衡？ | 探索深度 × 计算成本 |
| CoT 2026 状态？ | 前沿推理内建；小模型仍大增益 |
| ToT 机制？ | 生成候选 + 评估 + BFS/DFS 搜索 |
| Game of 24？ | 4%→74%（ToT vs CoT） |
| 推理模型冲击？ | 内部搜索通常打败外部 ToT |
| 外部 ToT 保留？ | 可观察/可转向/搜索即 API/成本上限 |
| 三 yes 规则？ | CoT<50% + 前步错误承诺 + 忠实评估器 |
| 级联？ | CoT 先行、低置信升 ToT |
| 常见错误？ | 评估器未校准/分支太宽/太深/无终止 |

---

**下一模块**：[06-范式路由：2026 新理论](06-范式路由：2026新理论.md)　**返回总览**：[00-经典 Agent 范式理论总览](00-经典Agent范式理论总览.md)

## 参考来源

- [What is Tree of Thoughts Prompting? Branching Reasoning in 2026（FutureAGI）](https://futureagi.com/blog/what-is-tree-of-thoughts-prompting-2026/)
- [What Is Chain-of-Thought Prompting? (2026)（Respan）](https://www.respan.ai/articles/what-is-chain-of-thought-prompting)
- [Demystifying Chains, Trees, and Graphs of Thoughts（arXiv 2401.14295）](https://arxiv-org.ezproxy.obspm.fr/html/2401.14295v6)
- [Tree of Thoughts as a Classical Heuristic Search Problem（arXiv 2605.28566）](https://arxiv-org.ezproxy.obspm.fr/html/2605.28566v1)
- [SwiReasoning: Switch-Thinking in Latent and Explicit（ICLR 2026）](https://mlanthology.org/iclr/2026/shi2026iclr-swireasoning/)
- [Towards reasoning era: a survey of long chain-of-thought（Semantic Scholar）](https://www.semanticscholar.org/paper/Towards-reasoning-era%3A-a-survey-of-long-for-large-Chen-Qin/f078092a132049b931419847200ca570ec99cfa2)
