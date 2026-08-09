# Reflexion 与自我修正范式

> 理论篇：第三代自我修正范式（Shinn 等，2023）——"做后复盘"的理论深潜。组件深潜见 [Reflection 反思模块](..%2F..%2FAgent%20子组件专项学习%2FReflection%20反思模块%2F00-Reflection反思模块总览.md)。

## 1. 核心机制：语言化自我批评

```text
Reflexion 循环：
  Actor 尝试 → Evaluator 评估（测试/外部反馈）
    → 失败 → Self-Reflection 分析（"我忽略了 X"）
    → 教训入情景记忆 → Actor 再尝试（避开已知错误）
```

| 要素 | 说明 |
|---|---|
| 无权重更新 | 批评写进记忆而非改参数（verbal RL） |
| 外部反馈优先 | 测试/工具结果（oracle） |
| 记忆传递 | 教训跨轮有效（情景记忆） |
| 奠基实证 | HumanEval **91%** vs 80% 基线 |

> 🎯 核心要点：**Reflexion 的贡献 = "自我改进不靠重训"**——语言化批评 + 记忆让 Agent 跨轮进步；2026 状态：**认知反转延续**——自我反思不可信（同模型同盲区），可靠评估必须外部化（Reflection 01 篇）。

## 2. 变体谱系

| 变体 | 反馈来源 | 特点 |
|---|---|---|
| Reflexion | 外部反馈（测试/工具） | 奠基（可靠） |
| Self-Refine | 模型自己的批评 | 同盲区（2026 不推荐单独用） |
| CRITIC | 外部知识批评（检索） | 知识验证 |
| LATS | MCTS 树搜索 + 反思 | 搜索+反思结合 |
| Self-RAG | 反思 token | 检索决策内建 |

> ⚠️ 2026 理论铁律：**"反思的可靠性 = 反馈来源的外部性"**——Reflexion（外部）> Self-Refine（自我）；"自我反思不能作为自己的评判器"（GSM8K 实证自我修正成绩下降）。

## 3. Oracle 前提：反思的许可证

| 有 oracle（测试/编译器/ground truth） | 无 oracle |
|---|---|
| ✅ 反思有效（Reflexion 主场） | ❌ 退化为自证偏见（"看起来不错"） |
| 代码/检索验证类任务 | 开放写作/创意 |
| 每轮有客观判据 | 无客观判据 |

> 💡 面试表述：**"反思收益 = oracle 质量 × 评判器独立性 × 修订执行力"**——三因子任一为零，反思都是白花钱（Reflection 01 篇）。

## 4. 成本模型

| 模式 | 推理开销 | 准确率提升 | 何时值 |
|---|---|---|---|
| 内隐提示词反思 | ~2x | +2-13% | 常规任务 |
| 外部评判器/工具 | ~3-4x | +3-23% | 高价值任务 |
| 微调蒸馏 | 最小 | +10-30% | 高频任务（Reflexion 蒸馏为权重） |

> ⚠️ 2026 理论共识：**"反思每轮是一次额外 LLM 调用——高价值/错误成本高的任务（医疗/金融/不可逆操作）才值得"**——低价值 QA 任务加反思是成本浪费；收益与成本都要报（Reflection 08 篇三指标）。

## 5. 轮次控制理论

| 规则 | 值 | 理由 |
|---|---|---|
| 复盘轮次 | 2-3 轮 | 第 1 轮捕获 70-80% 改进 |
| 精化轮次 | 3-5 轮 | 解空间探索 |
| 硬上限 | 计入预算 | 反思是预算内循环 |
| 最佳回滚 | 保留每轮最佳 | 防过度修正 |

> 🎯 一句话：**"反思是预算内的循环，不是无限的自省"**——过度修正（把对的改错）是反思特有失败；级联反思（便宜检查先过）省 50-80% 成本（Reflection 04 篇）。

## 6. 面试速记

| 问题 | 一句话答案 |
|---|---|
| Reflexion 是什么？ | 语言化自我批评 + 记忆——无权重更新（verbal RL） |
| 奠基实证？ | HumanEval 91% vs 80% |
| 与 Self-Refine 区别？ | 外部反馈 vs 自我批评（同盲区） |
| Oracle 前提？ | 有可验证标准才反思——无 oracle 是自证偏见 |
| 成本模型？ | 2x（内隐）~3-4x（外部）——高价值任务才值 |
| 何时不加反思？ | 低价值 QA/开放写作/无 oracle |
| 轮次控制？ | 2-3 复盘/3-5 精化 + 最佳回滚 |
| 三因子公式？ | oracle × 独立性 × 修订执行力 |
| 与重试区别？ | 重试原样再跑，反思先评再改 |
| 2026 认知？ | 自我反思不可信——评估外部化 |

---

**下一模块**：[05-推理范式家族：CoT-ToT-GoT](05-推理范式家族：CoT-ToT-GoT.md)　**返回总览**：[00-经典 Agent 范式理论总览](00-经典Agent范式理论总览.md)

## 参考来源

- [Reflexion: Language Agents with Verbal Reinforcement Learning（Shinn et al., 2023）](https://arxiv.org/abs/2303.11366)
- [ReAct, Plan-and-Execute, or Reflection?（dev.to）](https://dev.to/gabrielanhaia/react-plan-and-execute-or-reflection-the-three-agent-patterns-every-engineer-needs-in-2026-355p)
- [智能体经典范式深度解析（CSDN）](https://blog.csdn.net/2502_94273177/article/details/163042096)
- [AI Agent主流范式全解析（百度开发者）](https://developer.baidu.com/article/detail.html?id=7651022)
