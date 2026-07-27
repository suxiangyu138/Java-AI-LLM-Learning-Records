# 06 - 训练对齐循环：RLHF / DPO

> 🎯 训练对齐循环是 LLM 从"续写工具"变成"有用助手"的关键——RLHF 通过人类反馈的迭代循环逐步塑造模型行为，DPO 则以更简洁的方式实现同样目标

---

## 目录

1. [RLHF 三阶段循环](#1-rlhf-三阶段循环)
2. [DPO：更简洁的循环](#2-dpo更简洁的循环)
3. [迭代对齐：多轮 RLHF](#3-迭代对齐多轮-rlhf)
4. [RLHF vs DPO 循环对比](#4-rlhf-vs-dpo-循环对比)

---

## 1. RLHF 三阶段循环

```text
RLHF = 三阶段循环

Phase 1: SFT (监督微调)
  Pretrained Model → 人工写的高质量对话 → SFT Model

Phase 2: RM (训练奖励模型)
  对同一 Prompt 生成多个回答 → 人类标注偏好 → 训练打分模型

Phase 3: PPO (强化学习优化)
  SFT Model + RM → PPO 最大化奖励 → 对齐后的模型
                              ↑
                         KL 约束（不偏离太远）
```

### 1.1 PPO 的循环本质

```python
# PPO 循环 = 生成 → 打分 → 更新 → 重复
for epoch in range(num_epochs):
    for batch in preference_data:
        # 1. 当前模型生成回答
        responses = model.generate(batch.prompts)

        # 2. 奖励模型打分
        rewards = reward_model.score(batch.prompts, responses)

        # 3. 计算 KL 惩罚（与 SFT 模型的差异）
        kl_penalty = KL_divergence(model, sft_model, batch.prompts)

        # 4. PPO 更新
        loss = -(rewards - beta * kl_penalty).mean()
        optimizer.zero_grad()
        loss.backward()
        optimizer.step()
```

### 1.2 为什么 PPO 必须是循环

```text
不是一次性计算：

原因 1: 生成是串行的（自回归）
  必须逐 token 生成 → 天然是循环

原因 2: 模型在"在线"更新
  每轮优化的模型不同 → 生成的回答不同 → RM 的打分对象变了
  → 必须循环：生成 → 打分 → 更新 → 再生成 → ...

原因 3: RM 会过时
  当模型的行为模式发生显著变化后，旧 RM 的打分可能不准
  → 需要周期性地重新收集偏好数据 → 重新训练 RM
```

## 2. DPO：更简洁的循环

### 2.1 DPO 的简化

```text
RLHF 需要 4 个模型：Policy + Reference + RM + Value
DPO  只需要 1 个模型

RLHF:
  偏好数据 → 训练 RM → PPO 优化策略
  (两步循环)

DPO:
  偏好数据 → 直接优化策略
  (一步到位)
```

### 2.2 DPO 的 Loss

```python
def dpo_loss(model, ref_model, prompt, chosen, rejected, beta=0.1):
    # 计算 chosen 和 rejected 的对数概率比
    chosen_logp = model.log_prob(prompt, chosen)
    rejected_logp = model.log_prob(prompt, rejected)
    ref_chosen_logp = ref_model.log_prob(prompt, chosen)
    ref_rejected_logp = ref_model.log_prob(prompt, rejected)

    # DPO Loss：让 chosen 比 rejected 更"受模型喜欢"
    log_ratio = (chosen_logp - ref_chosen_logp) - (rejected_logp - ref_rejected_logp)
    loss = -F.logsigmoid(beta * log_ratio)

    return loss.mean()
```

### 2.3 DPO 的迭代循环

```text
虽然 DPO 单次训练更简单，但实践中仍然需要迭代循环：

Iteration 1: 初始偏好数据 → DPO → Model v1
  ↓ 部署，收集新数据
Iteration 2: Model v1 的输出 + 人类标注 → DPO → Model v2
  ↓
Iteration 3: ...

→ 每轮迭代，模型的行为模式在变化 → 需要新的偏好数据
```

## 3. 迭代对齐：多轮 RLHF

```text
真实的对齐不是一次 RLHF，而是多轮迭代：

Round 1: 基础对齐
  让模型学会"对话"和"遵循指令"

Round 2: 安全对齐
  减少有害输出、拒绝危险请求

Round 3: 质量对齐
  提升有用性、简洁性、格式遵循

Round 4: 细分对齐
  特定领域/语言的专项优化

每轮的关键：
  → 用上一轮的模型生成回答 → 标注 → 训练 → 下一轮
  → 偏好数据持续更新（模型变了，偏好也会变）
```

## 4. RLHF vs DPO 循环对比

| 维度 | RLHF (PPO) | DPO |
|------|:---:|:---:|
| 循环复杂度 | 三阶段 (SFT→RM→PPO) | 单阶段 |
| 所需模型数 | 4 个 | 1-2 个 |
| 在线生成 | ✅ 需要（PPO 交互） | ❌ 不需要（离线数据） |
| 训练稳定性 | 低（RL 难调） | 高 |
| 对偏好数据的利用 | 高效（RM 可以泛化） | 直接 |
| 迭代成本 | 高 | 低 |
| 最终效果 | 理论上限更高 | 实践中相近 |
| 谁在用 | GPT-4, Claude | Llama 3, Qwen 2.5 |

```text
选型建议：
├── 有大量计算资源 + 追求极致 → RLHF
├── 资源有限 + 快速迭代 → DPO
├── 两者结合 → DPO 做基础对齐 → RLHF 做精调
└── 2025 趋势：DPO 正在成为开源模型的主流选择
```

## 核心要点回顾

- RLHF 三阶段 = SFT(学对话) → RM(学打分) → PPO(学最大化奖励)
- PPO 必须是循环：生成→打分→更新→再生成（在线学习）
- DPO 简化：跳过 RM 和 PPO，直接从偏好数据优化
- 对齐是迭代循环——不是一次 RLHF 就完了，而是持续收集偏好→持续训练
- 2025: DPO 成为开源主流，但 RLHF 仍是闭源旗舰的首选

## 参考资料

1. InstructGPT / RLHF 论文 (Ouyang et al., 2022)
2. DPO 论文 (Rafailov et al., 2023)
3. Llama 3 对齐技术报告
