# 反思机制：从 Reflexion 到变体

> 反思的实现谱系：从 2023 的 Reflexion 到 2026 的 LATS/CRITIC/PRM。核心坐标轴是**反馈来源**——自我批评（Self-Refine）vs 外部反馈（Reflexion）vs 外部知识（CRITIC）——反馈越外部，反思越可靠。

## 1. Reflexion：奠基架构（verbal RL）

| 要素 | 说明 |
|---|---|
| 机制 | 语言化自我批评（不更新权重） |
| 三组件 | Actor + Evaluator + Self-Reflection |
| 记忆 | 批评入情景记忆，跨轮积累 |
| 效果 | 代码任务 pass@1 提升 10-20 个百分点 |
| 适用 | 有外部反馈的任务（测试/完成信号） |

```text
Reflexion 的可靠性来源：
  反馈是"外部环境"给的（测试跑不跑得过）
  而不是模型自己说的（"我觉得对"）
  —— 这是它比 Self-Refine 可靠的根本原因
```

## 2. Self-Refine vs Reflexion：反馈来源决定一切

| 维度 | Self-Refine | Reflexion |
|---|---|---|
| 批评来源 | 模型**自己的批评** | **外部环境反馈**（测试/信号） |
| 记忆 | 无（单轮） | 情景记忆（跨轮） |
| 可靠性 | 低（同模型盲区） | 高（外部证据） |
| 适用 | 表层改进 | 可验证任务 |

> 🎯 核心要点：**"谁给反馈"决定反思质量**——同一模型"自评自改"是循环论证；外部信号（测试结果/检索事实/执行输出）才是可信反馈。这是全体系的坐标轴。

## 3. 变体谱系（2026 全景）

| 变体 | 机制 | 反馈来源 | 适用 |
|---|---|---|---|
| Reflexion | 语言批评+记忆 | 外部（测试/信号） | 代码/决策 |
| Self-Refine | 自我批评 | 内部 | 表层改进 |
| CRITIC | 外部知识批评（搜索/DB/执行） | 外部知识 | 事实幻觉 |
| LATS | MCTS 树搜索+失败轨迹反思 | 环境+树探索 | 复杂决策 |
| Self-RAG | 反思 token（检索/相关/支持/有用） | 检索+自评 | RAG |
| GSAR | 多智能体可验证声明限定批评 | 声明可验证性 | 多智能体 |
| PRM 系 | 过程奖励（步骤级） | 步骤级打分 | 长任务（07 篇） |

## 4. CRITIC：外部知识批评（幻觉克星）

```text
CRITIC 循环：
  输出 → 外部工具验证（web 搜索/数据库/代码解释器）
  → 事实性批评 → 修订 → 再验证
  → 失败情形：事实源本身错 / 批评者与生成者同模型
```

| CRITIC 要点 | 说明 |
|---|---|
| 验证工具化 | "这通过了测试"替代"我觉得对" |
| 适用 | 知识密集任务（事实幻觉） |
| 局限 | 外部源错则批评错；同模型批评=盲区 |

## 5. LATS：反思 + 树搜索

| 要素 | 说明 |
|---|---|
| 机制 | MCTS 树搜索 + 反思失败轨迹 |
| 优势 | 多路径探索优于单轨迹反思 |
| 代价 | 计算成本远高于单轨迹 |
| 适用 | 高价值复杂决策 |

> 💡 LATS 的启示：**反思 + 探索的组合**——反思告诉"这条路径为什么错"，搜索负责"试更多路径"——适合"试错成本低、结果价值高"的场景。

## 6. Self-RAG：反思 token 化

| token | 功能 |
|---|---|
| [Retrieve] | 是否需要检索 |
| [IsRel] | 检索内容是否相关 |
| [IsSup] | 回答是否被支持 |
| [IsUse] | 回答是否有用 |

> 💡 Self-RAG 的工程价值：**把反思决策变成 token 流**（随生成一起输出），反思不再是一次独立 LLM 调用——反思内嵌化是降本方向（09 篇 distilled 同类思想）。

## 7. 三种部署模式（2026 分类）

| 模式 | 实现 | 推理成本 | 适用 |
|---|---|---|---|
| Distilled | 训练（草稿,批评,修订）三元组 → 单遍出精修 | **1x** | 场景固定 |
| Frozen-model | 提示词循环（Self-Refine/Reflexion） | 2-5x | 封闭 API/窄任务 |
| Hybrid+tools+search | 图编排+工具+树搜索（LangGraph） | 高 | 生产 Agent |

> 🎯 核心要点：部署模式的选择 = **"反思能否被蒸馏"**——场景固定且反思模式稳定 → distilled（1x 成本）；否则 frozen-model（2-5x）或 hybrid。别一上来就上最贵的混合。

## 8. 变体选型速查

| 需求 | 选型 |
|---|---|
| 代码任务（有测试） | Reflexion（外部反馈主场） |
| 事实幻觉治理 | CRITIC（外部知识） |
| 高价值复杂决策 | LATS（树搜索） |
| RAG 质量 | Self-RAG（token 化） |
| 成本敏感 | distilled 或级联（09 篇） |
| 多智能体 | GSAR（声明可验证） |
| 长任务步骤级 | PRM（07 篇） |

## 9. 常见坑速查

| 坑 | 现象 | 解法 |
|---|---|---|
| 同模型自评自改 | 循环论证 | 独立评判器/外部反馈 |
| 批评不落地 | 批评了没改 | 强制修订步骤 |
| 反思无记忆 | 每轮从零 | 情景记忆（Reflexion） |
| 变体混用 | 机制打架 | 按反馈来源选型 |

## 10. 面试速记

| 问题 | 一句话答案 |
|---|---|
| Reflexion 核心？ | 语言化自我批评 + 外部反馈 + 情景记忆（不更新权重） |
| Self-Refine vs Reflexion？ | 自我批评 vs 外部反馈——反馈来源决定可靠性 |
| CRITIC 是什么？ | 外部知识批评（搜索/DB/执行）专治事实幻觉 |
| LATS 是什么？ | MCTS 树搜索 + 失败轨迹反思（多路径优于单轨迹，贵） |
| 三种部署模式？ | Distilled（1x）/ Frozen（2-5x）/ Hybrid+tools（最贵） |
| 变体选型一句话？ | 按"反馈来源"选：测试→Reflexion、知识→CRITIC、探索→LATS |
| Self-RAG 的价值？ | 反思 token 化（[Retrieve]/[IsRel]/[IsSup]/[IsUse]）——反思内嵌降本 |
| 反思演进主线？ | 反馈越来越外部/细粒度/结构化：Reflexion→CRITIC→LATS→PRM→多智能体 |
| Reflexion 适用域？ | 代码生成（测试反馈）+10-20pp、决策、多步推理——有外部反馈的任务 |
| 树搜索无预算 | 成本爆炸 | LATS 限量 |
| distilled 场景漂移 | 精修退化 | 场景固定才蒸馏 |

---

**下一模块**：[03-评判器设计：谁来评估](03-评判器设计：谁来评估.md)　**返回总览**：[00-Reflection 反思模块总览](00-Reflection反思模块总览.md)

## 参考来源

- [What is Reflection Tuning?（FutureAGI）](https://futureagi.com/blog/what-is-reflection-tuning-2026/)
- [Agent Self-Correction: From Reflexion to Process Reward Models（Zylos）](https://zylos.ai/en/research/2026-05-12-agent-self-correction-reflexion-to-prm/)
- [Self-Correcting Agents: Reflexion, CRITIC, and ReAct Loops Compared（CallSphere）](https://callsphere.ai/blog/self-correcting-agents-reflexion-critic-react-loops-compared-2026)
- [The Reflection Pattern（Toolhalla）](https://toolhalla.ai/blog/reflection-pattern-ai-agents-2026)
