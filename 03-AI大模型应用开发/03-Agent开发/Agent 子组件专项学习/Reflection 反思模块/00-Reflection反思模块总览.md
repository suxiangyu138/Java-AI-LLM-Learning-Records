# Reflection 反思模块知识体系总览

> 定位：Agent 子组件专项之「反思模块」——[四大核心组件](..%2F..%2FAgent%20四大核心组件%2F04-反思组件：自我评估与纠错.md)反思组件的深化篇：从 Reflexion 机制讲到评判器设计、失败模式、外部验证与评测。2026 年核心共识：**自我反思不可信，可靠评估必须外部化**。总览做索引与速查，子主题各一篇。

## 📚 目录

1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [学习路线推荐](#3-学习路线推荐)
4. [核心概念速查](#4-核心概念速查)
5. [2026 生态基准](#5-2026-生态基准)

## 1. 知识体系导图

```text
Reflection 反思模块
├── 01 反思全景：自我修正的边界           三组件 / 自省不可靠 / 适用边界
├── 02 反思机制：从 Reflexion 到变体       Reflexion / Self-Refine / LATS / CRITIC
├── 03 评判器设计：谁来评估                评判器层级 / 偏差 / 独立评判器
├── 04 反思触发与轮次控制                 触发时机 / 2-3 轮上限 / 级联 / 回滚
├── 05 反思的失败模式                     过度修正 / 幻觉雪球 / 进度幻影
├── 06 反思与工具：外部验证                外部反馈 > 自我评判 / CRITIC / out-of-band
├── 07 过程奖励模型与前沿                  PRM vs ORM / AgentPRM / LATS
├── 08 反思评测：三指标方法论               pre/post delta / 过度修正率 / 成本
├── 09 反思工程化：级联与成本              级联反思 / 成本画像 / distilled
└── 10 生产实践与面试冲刺                 12 避坑 / 面试题
```

## 2. 模块导航

| 序号 | 模块 | 核心内容 | 适合人群 |
|---|---|---|---|
| 01 | [反思全景：自我修正的边界](01-反思全景：自我修正的边界.md) | 三组件、自省不可靠、适用边界 | 全部（地基） |
| 02 | [反思机制：从 Reflexion 到变体](02-反思机制：从Reflexion到变体.md) | Reflexion/Self-Refine/LATS/CRITIC | Agent 工程师 |
| 03 | [评判器设计：谁来评估](03-评判器设计：谁来评估.md) | 评判器层级、偏差、独立评判器 | Agent 工程师 |
| 04 | [反思触发与轮次控制](04-反思触发与轮次控制.md) | 触发时机、轮次上限、级联、回滚 | 落地开发者 |
| 05 | [反思的失败模式](05-反思的失败模式.md) | 过度修正、幻觉雪球、进度幻影 | 架构师 |
| 06 | [反思与工具：外部验证](06-反思与工具：外部验证.md) | 外部反馈、CRITIC、out-of-band | Agent 工程师 |
| 07 | [过程奖励模型与前沿](07-过程奖励模型与前沿.md) | PRM vs ORM、AgentPRM、LATS | 前沿关注者 |
| 08 | [反思评测：三指标方法论](08-反思评测：三指标方法论.md) | pre/post delta、过度修正率、成本 | 评测关注者 |
| 09 | [反思工程化：级联与成本](09-反思工程化：级联与成本.md) | 级联反思、成本画像、distilled | 落地开发者 |
| 10 | [生产实践与面试冲刺](10-生产实践与面试冲刺.md) | 12 避坑、面试题、落地清单 | 面试/上线前 |

## 3. 学习路线推荐

| 路线 | 路径 | 目标 |
|---|---|---|
| 入门（1 天） | 01 → 04 → 06 → 10 | 理解反思边界与外部验证原则 |
| 进阶（1 周） | 01-02 → 03 → 05 → 06 → 10 | 能设计评判器与轮次控制 |
| 高级（2 周） | 全量 + 07 → 08 → 09 | 能评测反思价值、做成本优化 |

## 4. 核心概念速查

| 概念 | 一句话定义 |
|---|---|
| 反思（Reflection） | Agent 评估自己的输出并改进的机制（有意识地自省） |
| Reflexion | 无权重更新的语言化自我批评（verbal RL，2023 奠基） |
| 三组件 | Actor（生成）+ Evaluator（评估）+ Self-Reflection（分析改进） |
| Self-Refine | 用模型**自己的批评**做反馈（与 Reflexion 外部反馈相对） |
| 评判器（Critic） | 评估输出的组件：规则/独立模型/自我/人工 |
| 确认偏误 | 自我评判强化自己的错误（同模型同盲区） |
| 谄媚批评 | 弱提示词的评判器回"看起来不错" |
| 演员-观察者不对称 | 自己归因外部因素，观察别人归因内部（ReTAS） |
| 过度修正 | 反思把本来正确的答案改错（多数团队从不测） |
| 幻觉雪球 | 自由文本反思递归美化早期错误 |
| 结构雪球/对齐税 | 强制结构化反思降低准确率（Qwen3 实证 50%→38%） |
| 进度幻影 | 自我打分"接受一切"，真实进展停滞（54 循环全称改进但 56% 无变化） |
| 外部验证 | 用工具/测试/检索验证（"这通过了测试"而非"我觉得对"） |
| Out-of-band 评估 | 成功信号在记录之外的客观评估（结构性需求） |
| 级联反思 | 便宜检查先过，不过才反思（省 50-80%） |
| PRM（过程奖励模型） | 步骤级打分（vs ORM 结果级，太稀疏 66.77%） |
| 三指标评测 | pre/post delta + 过度修正率 + 成本每提升 |

## 5. 2026 生态基准

> 📅 基准窗口：2026-08。版本与事实以各模块【参考来源】为准。

- **核心共识反转**：自我反思从"锦上添花"变成"不可信"——Kamoi（TACL 2024）实证自我修正使 GSM8K 数学成绩**下降**；2026 信息论论证：生成器与评判器共享相关错误模式时，自我评估证据力弱，迭代自省放大信心而非增加信息（"一致性陷阱"）。**可靠评估必须外部化**。
- **失败模式目录成型**：谄媚批评、过度修正（从不被测量）、幻觉雪球、结构雪球（Qwen3-8B HotpotQA：约束解码强制结构化反思使准确率 50%→38% 的对齐税）、无停止规则、同模型盲区、**进度幻影**（Zenodo 2026：自我评分门退化为"全接受"，56% 声称改进实际零变化，最好状态被侵蚀 19%——out-of-band 评估是结构性需求）。
- **变体谱系完整**：Reflexion（外部反馈+记忆）> Self-Refine（自我批评）；LATS（MCTS 树搜索+反思）、CRITIC（外部知识批评）、Self-RAG（反思 token）、GSAR（可验证声明限定批评）；三种部署模式：distilled（1x 成本）/frozen-model（2-5x）/hybrid+tools+search。
- **PRM 成为前沿**：过程奖励模型（步骤级）替代结果奖励（66.77% 太稀疏）——AgentPRM（Promise/Progress 双指标，NeurIPS 2025）、ThinkPRM、ToolPRMBench。
- **多智能体评判**：MARS（角色感知评判集成，反馈多样性 +76%，仅 5.2% token 成本）；ReTAS（演员-观察者不对称，ACL 2026，>20% 场景触发）；CoNL（元评估：批评质量=能否帮他人改进）。
- **成本画像**：内隐提示词反思 ~2x 推理（+2-13% 准确率）、外部评判器/工具 ~3-4x（+3-23%）、微调 distilled 最小推理成本（+10-30%）——**收益与成本都要报**。
- **工程共识**：独立评判器（不同模型家族）、外部验证优先、轮次上限 2-3（第 1 轮捕获 70-80% 改进，第 5 轮过优化）、级联反思（便宜检查先过省 50-80%）、最佳版本回滚。
- **与体系分工**：[四大核心组件](..%2F..%2FAgent%20四大核心组件%2F04-反思组件：自我评估与纠错.md) 04 篇讲反思组件的概览与 oracle 原则；本体系深潜"反思本体"（机制/评判器/失败/评测/成本）。

---

**下一模块**：[01-反思全景：自我修正的边界](01-反思全景：自我修正的边界.md)

## 参考来源

- [What is Reflection Tuning? Reflexion, Self-Refine, and 2026 Patterns（FutureAGI）](https://futureagi.com/blog/what-is-reflection-tuning-2026/)
- [Agent Self-Correction: From Reflexion to Process Reward Models（Zylos）](https://zylos.ai/en/research/2026-05-12-agent-self-correction-reflexion-to-prm/)
- [Self-Correcting Agents: Reflexion, CRITIC, and ReAct Loops Compared（CallSphere）](https://callsphere.ai/blog/self-correcting-agents-reflexion-critic-react-loops-compared-2026)
- [Evaluating LLM Self-Reflection Loops: The 3 Metrics That Matter (2026)（FutureAGI）](https://futureagi.com/blog/evaluating-llm-self-reflection-loops-2026/)
- [When Do Agent Loops Mistake Stagnation for Progress?（Zenodo 2026）](https://zenodo.org/records/21594736)
- [MARS: Multi-Agent Reflective Synergy（IEEE 2026）](https://ieeexplore.ieee.org/document/11557855)
- [Taming Actor-Observer Asymmetry in Agents via Dialectical Alignment（ACL 2026）](https://aclanthology.org/2026.acl-long.1104/)
- [The Reflection Pattern: How AI Agents Self-Correct（Toolhalla）](https://toolhalla.ai/blog/reflection-pattern-ai-agents-2026)
