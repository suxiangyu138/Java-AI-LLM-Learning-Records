# 06-PPO 与 TRPO

> 朴素策略梯度有一个致命问题：步长稍大，策略就会崩到无法恢复的区域。TRPO 用"信赖域"约束每次更新的幅度，PPO 用"裁剪"近似这个约束，把二阶优化变成一阶优化。PPO 因此成为 RLHF 时代的默认算法，也是理解 GRPO 的直接前置。

---

## 目录

1. [策略梯度的步长困境](#1-策略梯度的步长困境)
2. [信赖域思想](#2-信赖域思想)
3. [重要性采样](#3-重要性采样)
4. [TRPO 的目标与约束](#4-trpo-的目标与约束)
5. [PPO 的裁剪目标](#5-ppo-的裁剪目标)
6. [PPO 的完整损失](#6-ppo-的完整损失)
7. [PPO 训练流程](#7-ppo-训练流程)
8. [PPO 实现细节](#8-ppo-实现细节)
9. [PPO 在 RLHF 中的形态](#9-ppo-在-rlhf-中的形态)
10. [PPO 的局限](#10-ppo-的局限)
11. [代码实现](#11-代码实现)
12. [面试问法](#12-面试问法)
13. [小结](#13-小结)

---

## 1. 策略梯度的步长困境

策略梯度的更新式：

$$\theta \leftarrow \theta + \alpha\nabla_\theta J(\theta)$$

问题在于"步长多大合适"：

```text
步长太小：
  收敛慢，采样成本高

步长太大：
  策略分布剧变
  → 新策略采不到旧策略的高奖励动作
  → 性能崩溃，且很难恢复（策略梯度是局部线性近似，大步长下失效）
```

更本质的问题：

```text
策略梯度只保证"局部"方向正确
它假设更新后仍在同一个"信任区域"内
一旦跨出这个区域，梯度方向不再可信

需要一个机制：
  限制每次更新后新旧策略的差异
```

---

## 2. 信赖域思想

信赖域（trust region）的核心是：**在旧策略附近找一个可信范围，在这个范围内优化，而不是全局优化**。

```text
度量"两个策略有多不同"：
  KL 散度 D_KL(π_old || π_new)

约束优化：
  maximize  目标函数
  subject to E[D_KL(π_old, π_new)] ≤ δ

δ 小 → 每次只走一小步，稳定
δ 大 → 走得快，但可能崩
```

为什么用 KL 而不是参数距离：

```text
参数空间的距离不能反映策略行为
  两组参数差很多，策略可能几乎一样
  两组参数差一点，策略可能天差地别（softmax 饱和区）

KL 直接度量"动作分布"的差异
  → 与性能变化更相关
```

---

## 3. 重要性采样

TRPO/PPO 都用重要性采样，让"旧策略采的数据"能评估"新策略"：

$$\mathbb{E}_{a\sim\pi_{\theta'}}\left[f(a)\right] = \mathbb{E}_{a\sim\pi_\theta}\left[\frac{\pi_{\theta'}(a\mid s)}{\pi_\theta(a\mid s)}f(a)\right]$$

定义比率：

$$\rho_t(\theta) = \frac{\pi_\theta(a_t\mid s_t)}{\pi_{\theta_{old}}(a_t\mid s_t)}$$

```text
ρ = 1：新旧策略对该动作的概率相同
ρ > 1：新策略更倾向该动作
ρ < 1：新策略更不倾向该动作

重要性采样的方差随 ρ 偏离 1 而增大
→ 新旧策略差太远时，估计不可靠
→ 这也是需要信赖域/裁剪的原因
```

---

## 4. TRPO 的目标与约束

### 4.1 性能差异引理

对任意策略 $\pi'$：

$$\eta(\pi') = \eta(\pi) + \mathbb{E}_{\tau\sim\pi'}\left[\sum_{t=0}^{\infty}\gamma^t A^\pi(s_t,a_t)\right]$$

含义：新策略的性能 = 旧策略性能 + 新策略轨迹上的优势总和。

### 4.2 代理目标

直接优化上式需要在新策略上采样，循环依赖。于是用旧策略的状态分布近似：

$$L_\pi(\pi') = \eta(\pi) + \mathbb{E}_{s\sim d^\pi,\,a\sim\pi'}\left[A^\pi(s,a)\right]$$

这是 $\eta(\pi')$ 的一阶近似（状态分布用旧的，动作分布用新的）。

### 4.3 TRPO 的优化问题

$$\max_\theta\; \mathbb{E}_{s,a\sim\pi_{old}}\left[\frac{\pi_\theta(a\mid s)}{\pi_{old}(a\mid s)}A^{\pi_{old}}(s,a)\right]$$

$$\text{s.t.}\; \mathbb{E}_{s\sim\pi_{old}}\left[D_{KL}\left(\pi_{old}(\cdot\mid s)\,\|\,\pi_\theta(\cdot\mid s)\right)\right]\le\delta$$

### 4.4 求解

```text
TRPO 用二阶方法（共轭梯度 + 线搜索）求解：
  ① 算策略梯度 g
  ② 算 Fisher 信息矩阵 F（KL 的二阶近似）
  ③ 解 F x = g 得方向
  ④ 线搜索保证 KL 约束与性能提升

问题：
  实现复杂
  二阶计算昂贵
  与深度学习框架的自动微分配合麻烦
```

---

## 5. PPO 的裁剪目标

PPO 用一阶方法近似信赖域，有两种形式。

### 5.1 裁剪版本（Clipped Surrogate）

$$
L^{CLIP}(\theta) = \mathbb{E}_t\left[\min\left(\rho_t(\theta)A_t,\;\mathrm{clip}\left(\rho_t(\theta),1-\epsilon,1+\epsilon\right)A_t\right)\right]
$$

其中：

$$\rho_t(\theta) = \frac{\pi_\theta(a_t\mid s_t)}{\pi_{\theta_{old}}(a_t\mid s_t)}$$

### 5.2 逐项分析

分两种情况讨论 $\min$ 的行为：

**情况一：$A_t>0$（好动作）**

```text
希望提高该动作概率 → ρ 增大
但 ρ 超过 1+ε 后，clip 把目标固定在 (1+ε)A_t
→ 继续增大 ρ 不会带来更大收益
→ 梯度为 0，停止更新
```

**情况二：$A_t<0$（坏动作）**

```text
希望降低该动作概率 → ρ 减小
但 ρ 低于 1-ε 后，clip 把目标固定在 (1-ε)A_t
→ 继续减小 ρ 不会带来更大收益
→ 梯度为 0，停止更新
```

用一张表概括：

| 优势 | 比率 | 未裁剪目标 | 裁剪后 | 有效梯度 |
|------|------|-----------|--------|---------|
| $A>0$ | $\rho<1-\epsilon$ | 小 | 同左 | 有（提升） |
| $A>0$ | $1-\epsilon\le\rho\le1+\epsilon$ | 随 $\rho$ 增 | 同左 | 有 |
| $A>0$ | $\rho>1+\epsilon$ | 大 | 截断 | 无 |
| $A<0$ | $\rho>1+\epsilon$ | 更负 | 同左 | 有（降低） |
| $A<0$ | $1-\epsilon\le\rho\le1+\epsilon$ | 随 $\rho$ 增 | 同左 | 有 |
| $A<0$ | $\rho<1-\epsilon$ | 小 | 截断 | 无 |

核心思想：**只惩罚"朝有利方向走太远"的更新，不阻止"朝不利方向修正"的更新**。

### 5.3 KL 惩罚版本

另一种 PPO 形式把 KL 放进损失：

$$L^{KL}(\theta) = \mathbb{E}_t\left[\rho_t(\theta)A_t - \beta\,D_{KL}\left(\pi_{\theta_{old}}\,\|\,\pi_\theta\right)\right]$$

```text
β 自适应：
  如果 KL 太大 → 增大 β（收紧约束）
  如果 KL 太小 → 减小 β（放松约束）
  目标是把 KL 维持在一个目标值附近

PPO 原论文两种形式都支持，实践中 clip 版本更常用
```

---

## 6. PPO 的完整损失

实际 PPO 的损失由三部分组成：

$$L(\theta) = -L^{CLIP}(\theta) + c_1 L^{VF}(\theta) - c_2\,\mathbb{E}\left[\mathcal{H}(\pi_\theta(\cdot\mid s))\right]$$

| 项 | 含义 | 系数 |
|----|------|------|
| $L^{CLIP}$ | 策略损失（最大化） | 1 |
| $L^{VF}$ | 价值函数损失（最小化） | $c_1$，常 0.5 |
| 熵 | 探索奖励（最大化） | $c_2$，常 0.01 |

价值损失：

$$L^{VF}(\theta) = \mathbb{E}_t\left[\left(V_\theta(s_t) - V_t^{target}\right)^2\right]$$

有些实现用裁剪的价值损失，防止价值更新过猛：

$$L^{VF}_{clip} = \mathbb{E}\left[\max\left(\left(V_\theta - V^{target}\right)^2,\;\left(\mathrm{clip}(V_\theta,V_{old}-\epsilon,V_{old}+\epsilon)-V^{target}\right)^2\right)\right]$$

---

## 7. PPO 训练流程

```text
循环：
  ① 用当前策略 π_old 采样一批轨迹
     （多个环境并行，凑够 N 步或 M 条轨迹）
  ② 用 GAE 计算优势 A_t 与回报 V_target
  ③ 把数据切成 mini-batch
  ④ 对每个 mini-batch 做 K 个 epoch 的更新
       计算 ρ、clip 损失、价值损失、熵
       反向传播更新 θ
  ⑤ 丢弃这批数据，回到 ①

关键：
  K 个 epoch 复用同一批数据
  → 样本效率比朴素策略梯度高
  → 但 ρ 会越来越偏离 1，所以 K 不能太大（常 3-10）
```

---

## 8. PPO 实现细节

### 8.1 超参数基线

| 参数 | 常用值 | 说明 |
|------|--------|------|
| `clip_range` $\epsilon$ | 0.2 | 裁剪范围 |
| `gamma` | 0.99 | 折扣 |
| `lambda` (GAE) | 0.95 | 优势估计 |
| `learning_rate` | 3e-4 | Adam |
| `n_steps` | 2048 | 每次采样的步数 |
| `batch_size` | 64 | mini-batch |
| `n_epochs` | 10 | 每批数据更新轮数 |
| `value_coef` | 0.5 | 价值损失系数 |
| `entropy_coef` | 0.01 | 熵系数 |
| `max_grad_norm` | 0.5 | 梯度裁剪 |

### 8.2 优势归一化

```text
对 mini-batch 内的优势做标准化：
  A ← (A - mean(A)) / (std(A) + eps)

作用：
  稳定梯度尺度
  防止某些 batch 优势过大导致更新过猛

注意：
  不改变优势的相对大小，只改变尺度
```

### 8.3 常见实现坑

```text
① 忘记 detach 旧策略的 log_prob
   → 计算 ρ 时把旧策略也算进梯度图，梯度错误

② 优势没有归一化
   → 不同 batch 梯度尺度差异大，训练震荡

③ n_epochs 太大
   → ρ 偏离 1，裁剪失效，策略崩塌

④ 学习率过大
   → 与裁剪叠加后仍可能不稳

⑤ 价值网络与策略网络共享参数
   → 两个目标冲突，需谨慎

⑥ 环境终止处理错误
   → done 时没把 next_value 置 0，优势估计偏差
```

---

## 9. PPO 在 RLHF 中的形态

RLHF 的 PPO 与游戏 RL 的差异：

| 维度 | 游戏 PPO | RLHF PPO |
|------|---------|----------|
| 环境 | 游戏引擎 | 人类偏好/奖励模型 |
| 动作 | 手柄按键 | 词表 token |
| 奖励 | 每步或终局 | 整段回答一个分数 |
| 状态 | 游戏画面 | prompt + 已生成 token |
| 回合长度 | 几十到几千步 | 几十到几千 token |
| 额外约束 | 无 | KL 约束参考模型 |

RLHF 的目标：

$$\max_\theta\;\mathbb{E}_{x\sim\mathcal{D},\,y\sim\pi_\theta(\cdot\mid x)}\left[r_\phi(x,y) - \beta\,D_{KL}\left(\pi_\theta(\cdot\mid x)\,\|\,\pi_{ref}(\cdot\mid x)\right)\right]$$

四个模型同框：

```text
Actor（π_θ）：待训练的策略
Critic（V_φ）：估状态价值
Reward Model（r_φ）：给回答打分
Reference（π_ref）：KL 锚点，通常是 SFT 模型

显存压力：
  四个模型同时驻留 → 70B 模型需要多卡 + ZeRO/FSDP
  → 工程成本高，这是 GRPO 去掉 Critic 的动机之一
```

---

## 10. PPO 的局限

```text
① 需要 Critic
   价值网络与策略网络同规模时，显存翻倍
   价值估计不准会拖累策略

② 超参数敏感
   clip、学习率、epoch 数、GAE λ 都要调
   不同任务的最优值差异大

③ 样本效率仍有限
   On-policy，数据用几次就丢

④ 长序列信用分配难
   LLM 回答几千 token，只有句末奖励
   GAE 的 λ 要在偏差与方差之间折中

⑤ 奖励黑客
   KL 约束只能减缓，不能根治

⑥ 实现复杂
   四个模型、采样与训练交替、分布式同步
```

> **要点**：GRPO 的两大简化正是针对 PPO 的两个痛点：去掉 Critic（省显存、省价值估计），用组内奖励标准化代替优势估计（省 GAE 调参）。代价是采样组变大。

---

## 11. 代码实现

```python
import torch
import torch.nn as nn
import torch.optim as optim


class ActorCritic(nn.Module):

    def __init__(self, state_dim, action_dim, hidden_dim=128):
        super().__init__()
        self.shared = nn.Sequential(
            nn.Linear(state_dim, hidden_dim),
            nn.Tanh(),
        )
        self.actor = nn.Linear(hidden_dim, action_dim)
        self.critic = nn.Linear(hidden_dim, 1)

    def forward(self, state):
        features = self.shared(state)
        return self.actor(features), self.critic(features).squeeze(-1)


def ppo_update(model, optimizer, states, actions, old_log_probs, advantages,
               returns, clip_range=0.2, value_coef=0.5, entropy_coef=0.01,
               max_grad_norm=0.5):
    logits, values = model(states)
    dist = torch.distributions.Categorical(logits=logits)
    new_log_probs = dist.log_prob(actions)
    entropy = dist.entropy().mean()

    ratio = torch.exp(new_log_probs - old_log_probs)
    unclipped = ratio * advantages
    clipped = torch.clamp(ratio, 1.0 - clip_range, 1.0 + clip_range) * advantages
    policy_loss = -torch.min(unclipped, clipped).mean()

    value_loss = nn.functional.mse_loss(values, returns)

    loss = policy_loss + value_coef * value_loss - entropy_coef * entropy

    optimizer.zero_grad()
    loss.backward()
    nn.utils.clip_grad_norm_(model.parameters(), max_grad_norm)
    optimizer.step()

    with torch.no_grad():
        approx_kl = (old_log_probs - new_log_probs).mean().item()
        clip_fraction = ((ratio - 1.0).abs() > clip_range).float().mean().item()

    return {
        "loss": float(loss.item()),
        "policy_loss": float(policy_loss.item()),
        "value_loss": float(value_loss.item()),
        "entropy": float(entropy.item()),
        "approx_kl": approx_kl,
        "clip_fraction": clip_fraction,
    }
```

关键监控指标：

```text
approx_kl：
  新旧策略的近似 KL
  经验阈值：> 0.02 说明更新过大，应提前停止该批数据的 epoch

clip_fraction：
  被裁剪的样本比例
  过高（> 0.3）说明策略变化太大

entropy：
  持续下降说明策略趋于确定
  下降过快可能探索不足

value_loss：
  持续上升说明 Critic 跟不上策略
```

---

## 12. 面试问法

**问：PPO 为什么比朴素策略梯度稳定？**

详细版：朴素策略梯度只保证局部方向，步长过大会让策略跨出信任区域，采不到高奖励动作而崩溃。PPO 用重要性采样比率 $\rho$ 衡量新旧策略差异，通过裁剪把 $\rho$ 限制在 $[1-\epsilon,1+\epsilon]$，超出范围后梯度归零，相当于用一阶方法近似 TRPO 的 KL 信赖域约束。

简短版：PPO 用 clip 限制新旧策略的比率，防止单次更新过大导致策略崩塌。

**问：PPO 的 clip 是怎么起作用的？**

详细版：对优势为正的动作，$\rho$ 超过 $1+\epsilon$ 后裁剪把目标固定，梯度归零，阻止继续提高该动作概率；对优势为负的动作，$\rho$ 低于 $1-\epsilon$ 后同理。它只惩罚"朝有利方向走太远"，不阻止"朝不利方向修正"。

简短版：好动作涨太多就截断，坏动作降太多也截断，防止过度更新。

**问：PPO 和 TRPO 的区别？**

详细版：TRPO 显式约束 KL 并用二阶优化（Fisher 矩阵 + 共轭梯度）求解，理论严谨但实现复杂；PPO 用裁剪或 KL 惩罚把约束近似成一阶目标，实现简单、可与自动微分配合，实践效果接近。

简短版：TRPO 二阶、显式 KL 约束；PPO 一阶、裁剪近似，更简单更常用。

**问：PPO 里的重要性采样比率有什么用？**

详细版：让旧策略采样的数据能评估新策略，从而在同一批数据上做多个 epoch 的更新，提高样本效率。比率偏离 1 越远，估计方差越大，所以要用裁剪限制。

简短版：复用旧数据评估新策略，配合裁剪控制偏差。

---

## 13. 小结

| 问题 | 答案 |
|------|------|
| 为什么需要信赖域 | 策略梯度局部有效，大步长会崩 |
| TRPO 的约束 | 平均 KL ≤ δ |
| PPO 的近似 | 裁剪重要性采样比率 |
| 裁剪范围 | 通常 0.2 |
| PPO 损失三项 | 策略、价值、熵 |
| 训练流程 | 采样 → GAE → mini-batch 多 epoch |
| 关键监控 | approx_kl、clip_fraction、entropy、value_loss |
| RLHF 的 PPO | 四模型同框，加 KL 约束 |
| PPO 的痛点 | 需要 Critic、超参敏感、实现复杂 |

**下一模块**：[07-RLHF：人类反馈对齐](07-RLHF：人类反馈对齐.md)　**返回总览**：[00-强化学习知识体系总览](00-强化学习知识体系总览.md)
