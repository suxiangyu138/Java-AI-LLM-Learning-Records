# 06 - RLHF 与对齐技术

> 🎯 让 LLM"有用、诚实、无害"的核心是对齐技术——RLHF 让 ChatGPT 从续写工具变成助手，DPO 让对齐训练更简单。本章覆盖 RLHF/DPO/Constitutional AI 三大对齐方案

---

## 目录

1. [为什么需要对齐](#1-为什么需要对齐)
2. [RLHF 完整流程](#2-rlhf-完整流程)
3. [DPO：更简单的对齐](#3-dpo更简单的对齐)
4. [对齐技术的挑战](#4-对齐技术的挑战)

---

## 1. 为什么需要对齐

```text
预训练 + SFT 后的模型：
  ✅ 能对话了
  ❌ 仍可能输出有害内容
  ❌ 可能"胡说八道"（幻觉）
  ❌ 不知道"我不知道"
  ❌ 偏好不明确（冗长 vs 简洁、安全 vs 有用）

对齐 (Alignment) = 让模型的输出符合人类偏好
```

### 对齐的目标

```text
HHH 原则（Anthropic）：
├── Helpful（有用）：回答问题，完成任务
├── Honest（诚实）：不知道就说不知道，不编造
└── Harmless（无害）：不输出危险/违法/有害内容
```

## 2. RLHF 完整流程

```text
RLHF (Reinforcement Learning from Human Feedback)
ChatGPT 的"魔法"来源

Step 1: 收集偏好数据
  对同一 prompt，让模型生成多个回答
  → 人类标注员选择更好的那个
  → 构建 (prompt, chosen, rejected) 数据集

Step 2: 训练奖励模型 (RM)
  输入: prompt + response
  输出: 分数（这个回答有多好）
  目标: chosen 的分数 > rejected 的分数

Step 3: PPO 强化学习
  用奖励模型给模型输出打分
  → PPO 算法最大化分数
  → 同时加 KL 惩罚（不要偏离 SFT 太远）
```

### PPO 训练公式

```text
目标 = 最大化 [ E[RM(prompt, response)] - β·KL(π_RL || π_SFT) ]

奖励模型分数      偏离惩罚

β 越大 → 模型越保守（紧跟 SFT）
β 越小 → 模型越激进（最大化奖励）
```

## 3. DPO：更简单的对齐

### 3.1 为什么 DPO 替代 RLHF

```text
RLHF 的痛点：
  ❌ 需要单独训练奖励模型（费时费力）
  ❌ 需要 PPO 强化学习（不稳定、难调参）
  ❌ 需要 4 个模型同时在线（Policy + Reference + RM + Value）

DPO (Direct Preference Optimization)：
  ✅ 直接从偏好数据学习，不需要奖励模型
  ✅ 不需要强化学习（标准监督学习 loss）
  ✅ 数学上等价于 RLHF
  ✅ 更稳定、更快、更省算力
```

### 3.2 DPO 原理直觉

```text
DPO 的 Loss：
  最大化 chosen 相对于 rejected 的概率比

简单理解：DPO = 让模型"更喜欢"人类选中的回答
         而不是"训练奖励模型 → PPO优化"
```

### 3.3 代码示例

```python
from trl import DPOTrainer
from transformers import AutoModelForCausalLM

model = AutoModelForCausalLM.from_pretrained("mymodel-sft")

trainer = DPOTrainer(
    model=model,
    train_dataset=dpo_dataset,   # 包含 prompt/chosen/rejected
    tokenizer=tokenizer,
    args=DPOConfig(
        per_device_train_batch_size=4,
        learning_rate=5e-7,
        beta=0.1,                 # KL 惩罚系数
        max_length=1024,
    ),
)

trainer.train()
```

### 3.4 RLHF vs DPO 对比

| 维度 | RLHF (PPO) | DPO |
|------|:---:|:---:|
| 需要奖励模型 | ✅ 需要 | ❌ 不需要 |
| 训练稳定性 | 低（RL 难调） | 高 |
| 计算开销 | 高（4 个模型） | 中（1 个模型） |
| 数据效率 | 需要更多 | 更高效 |
| 最终效果 | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| 主流使用 | GPT-4, Claude 1-2 | Llama 3, Qwen 2.5 |

## 4. 对齐技术的挑战

```text
1. 过度对齐
   模型过于"安全"，拒绝合理请求
   "怎么开锁" → "我不能帮你..."（用户只是想修自家门锁）

2. 偏好多样
   不同文化/用户有不同的价值观
   "什么是好回答" → 没有统一标准

3. 奖励黑客 (Reward Hacking)
   模型学会骗奖励模型（给高分但不真正有用）
   例如：输出很长但没内容 → 奖励模型给的分数还挺高

4. 评估困难
   怎么客观衡量"对齐程度"？
   当前靠人工评估 + Arena 排行榜投票
```

## 核心要点回顾

- 对齐三目标：Helpful（有用）、Honest（诚实）、Harmless（无害）
- RLHF 三步：收集偏好 → 训练奖励模型 → PPO 优化
- DPO = 直接偏好优化：不需要奖励模型 + RL，更简单稳定
- DPO 已成为 2024-2025 开源模型的主流对齐方案
- 对齐悖论：过度对齐导致"安全但没用"

## 参考资料

1. InstructGPT / RLHF 论文 (Ouyang et al., 2022)
2. DPO 论文 (Rafailov et al., 2023)
3. Constitutional AI 论文 (Anthropic, 2022)
