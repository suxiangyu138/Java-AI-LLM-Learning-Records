# 08-GRPO 与 RLVR

> 2026 年 LLM 后训练的默认组合是"GRPO 家族优化器 + 可验证奖励（RLVR）"。GRPO 用组内相对奖励替代 Critic，把 PPO 的四模型压到三模型；RLVR 用答案对错、单元测试、格式规则替代人类偏好，把奖励信号变得可验证、可扩展。这一篇讲清楚 GRPO 的目标函数、它的问题、DAPO/GSPO/Dr.GRPO 的修正，以及 RLVR 的完整训练管线。

---

## 目录

1. [从 PPO 到 GRPO](#1-从-ppo-到-grpo)
2. [GRPO 目标函数](#2-grpo-目标函数)
3. [组内优势估计](#3-组内优势估计)
4. [GRPO 与 PPO 的对比](#4-grpo-与-ppo-的对比)
5. [GRPO 的四个问题](#5-grpo-的四个问题)
6. [DAPO 四修](#6-dapo-四修)
7. [GSPO：序列级比率](#7-gspo序列级比率)
8. [Dr.GRPO 与其他变体](#8-drgrpo-与其他变体)
9. [RLVR：可验证奖励](#9-rlvr可验证奖励)
10. [RLVR 训练管线](#10-rlvr-训练管线)
11. [推理模型的 RL 实践](#11-推理模型的-rl-实践)
12. [2026 前沿方向](#12-2026-前沿方向)
13. [代码实现](#13-代码实现)
14. [面试问法](#14-面试问法)
15. [小结](#15-小结)

---

## 1. 从 PPO 到 GRPO

PPO 在 RLHF 中的成本结构：

```text
四个模型同时驻留：
  Actor（策略）
  Critic（价值）
  Reward Model（奖励）
  Reference（参考）

显存：70B 模型全参训练，四份权重 + 优化器状态 + 梯度
  → 需要数十张卡，成本极高
```

GRPO（Group Relative Policy Optimization，DeepSeekMath 2024）的核心改动：

```text
去掉 Critic
用"同一 prompt 的多个采样回答"的组内统计量作为基线
→ 优势 = 该回答的奖励 - 组内平均奖励（再标准化）
```

直觉：

```text
PPO：
  "这个回答比 Critic 估计的平均水平好多少？"

GRPO：
  "这个回答比同一道题的其他 G 个回答好多少？"

后者不需要训练价值网络
用采样换掉了 Critic
```

---

## 2. GRPO 目标函数

### 2.1 采样结构

对每个问题 $q$，从旧策略采样 $G$ 个回答：

$$\{o_1,o_2,\ldots,o_G\}\sim\pi_{\theta_{old}}(\cdot\mid q)$$

每个回答得到奖励 $r_i$（来自规则或奖励模型）。

### 2.2 优势估计

组内标准化：

$$\hat{A}_{i} = \frac{r_i - \mathrm{mean}(\{r_1,\ldots,r_G\})}{\mathrm{std}(\{r_1,\ldots,r_G\})}$$

同一回答内的所有 token 共享这个优势（token-level 变体会做更细的分配）。

### 2.3 目标函数

$$
\mathcal{J}_{GRPO}(\theta) = \mathbb{E}_{q,\,\{o_i\}\sim\pi_{old}}\left[
\frac{1}{G}\sum_{i=1}^{G}\frac{1}{|o_i|}\sum_{t=1}^{|o_i|}
\left\{
\min\left(\rho_{i,t}\hat{A}_i,\;\mathrm{clip}(\rho_{i,t},1-\epsilon,1+\epsilon)\hat{A}_i\right)
- \beta\,D_{KL}\left(\pi_\theta\,\|\,\pi_{ref}\right)
\right\}
\right]
$$

其中：

$$\rho_{i,t} = \frac{\pi_\theta(o_{i,t}\mid q,o_{i,<t})}{\pi_{\theta_{old}}(o_{i,t}\mid q,o_{i,<t})}$$

逐项对应 PPO：

| 项 | PPO | GRPO |
|----|-----|------|
| 优势 | GAE + Critic | 组内标准化 |
| 比率 | token 级 | token 级 |
| 裁剪 | 有 | 有 |
| KL | 有 | 有 |
| Critic | 需要 | 不需要 |

### 2.4 长度归一化的作用

$\frac{1}{|o_i|}\sum_t$ 是对回答长度取平均：

```text
防止长回答因为 token 多而获得更大权重
但长度归一化本身也引入了长度偏差（见第 5 节）
```

---

## 3. 组内优势估计

### 3.1 为什么组内平均是合理基线

对同一问题，组内平均奖励近似"当前策略在该问题上的期望表现"：

$$\mathbb{E}[r_i]\approx \mathrm{mean}(\{r_1,\ldots,r_G\})$$

于是：

$$\hat{A}_i \approx r_i - \mathbb{E}[r]$$

这正是优势的定义：比平均水平好多少。

### 3.2 与 Critic 的对比

| 维度 | Critic 基线 | 组内基线 |
|------|------------|---------|
| 估计对象 | $V(s_t)$，逐状态 | 整段回答的平均奖励 |
| 粒度 | token 级 | 序列级 |
| 成本 | 一个价值网络 | G 次采样 |
| 偏差 | 自举偏差 | 采样偏差（G 小则噪声大） |
| 方差 | 小 | 随 G 增大而减小 |
| 训练稳定性 | 依赖 Critic 质量 | 依赖组内多样性 |

### 3.3 组大小的权衡

```text
G 小（如 4）：
  基线噪声大，优势估计不稳
  采样成本低

G 大（如 16-64）：
  基线更准，优势更可靠
  采样成本线性增长

常用：8-16
训练时按显存与推理吞吐调整
```

---

## 4. GRPO 与 PPO 的对比

| 维度 | PPO | GRPO |
|------|-----|------|
| 模型数量 | 4（Actor/Critic/RM/Ref） | 3（Actor/RM/Ref） |
| 显存 | 高 | 低约 25% |
| 优势估计 | GAE + Critic | 组内标准化 |
| 采样 | 每个 prompt 少量 | 每个 prompt G 个 |
| 超参数 | clip、λ、value_coef 等 | clip、β、G |
| 实现复杂度 | 高 | 中 |
| 适合 | 通用 RL、连续控制 | LLM 后训练 |

```text
GRPO 不是全面超越 PPO：
  它用"更多采样"换"更少模型"
  在 LLM 场景下这笔交易划算（推理便宜、训练显存贵）
  在采样昂贵的场景（真实机器人）不一定划算
```

---

## 5. GRPO 的四个问题

### 5.1 零梯度组

```text
现象：
  同一 prompt 的 G 个回答全部正确或全部错误
  → 组内奖励方差为 0
  → 标准化时除零或优势全为 0
  → 这一组不产生梯度

后果：
  简单题与超难题贡献为零
  训练效率低（尤其后期，简单题全对）
```

### 5.2 长度偏差

```text
问题一：长回答的 token 级比率更多，梯度累积更大
问题二：长度归一化让"短而正确"与"长而正确"获得相近的优势
       但长回答的错误 token 更多，可能被过度惩罚
问题三：如果奖励与长度正相关（RM 偏好长回答），模型会越写越长
```

### 5.3 难度偏差

```text
std 归一化让"组内差异大"的题目获得更大梯度
  → 中等难度题（有对有错）权重最高
  → 简单题（全对）与难题（全错）被忽略

这本身是合理的（难易适中的题最有学习价值）
但会导致：
  模型在简单题上不再进步
  在超难题上永远学不到（因为从没对过）
```

### 5.4 重要性比率的不稳定

```text
token 级比率在长序列上容易累积偏差
MoE 模型中不同专家路由变化会放大比率波动
→ 训练不稳定、崩溃
```

---

## 6. DAPO 四修

DAPO（2025）针对 GRPO 的四个问题给出四项修正，是当前最常用的 GRPO 改进组合。

### 6.1 Clip-Higher

```text
原 GRPO：ε 对称，如 0.2
DAPO：解耦上下界
  ε_low = 0.2
  ε_high = 0.28

目的：
  提高"低概率 token 被提升"的空间
  增加探索，防止熵崩塌
```

### 6.2 动态采样（Dynamic Sampling）

```text
做法：
  过滤掉组内奖励全相同（全对/全错）的组
  重新采样，直到组内有区分度

解决：
  零梯度组问题
  提高训练效率
```

实现上要控制重采样次数，避免无限循环。

### 6.3 Token-level Policy Gradient Loss

```text
原 GRPO：先对每个回答的 token 求平均，再对组求平均
  1/G Σ_i (1/|o_i|) Σ_t

DAPO：先对所有 token 求和，再除以总 token 数
  1/Σ_i|o_i| Σ_i Σ_t

区别：
  原式让每个回答权重相同（不论长短）
  新式让每个 token 权重相同（长回答权重更大）

目的：
  避免长回答被"平均掉"
  让长序列中的每个 token 都得到有效梯度
```

### 6.4 Overlong Reward Shaping

```text
问题：
  超过最大长度的回答被截断
  截断的回答奖励可能错误（明明推理对了但没写完）
  → 噪声梯度

做法：
  对超长回答施加软惩罚
  长度接近上限时逐步降分，而不是直接判错
```

### 6.5 DAPO 的效果

论文报告（DAPO-32B）：

```text
AIME 2024 达到 50 分
比 DeepSeek-R1-Zero 用少约 50% 的训练步数
```

> **提示**：这些数字来自论文的特定设置，不能直接外推到其他模型与任务。工程上要自己复现基线。

---

## 7. GSPO：序列级比率

GSPO（Group Sequence Policy Optimization，2025，Qwen3 采用）把重要性比率从 token 级改为序列级。

### 7.1 定义

序列级比率：

$$s_i(\theta) = \left(\frac{\pi_\theta(o_i\mid q)}{\pi_{\theta_{old}}(o_i\mid q)}\right)^{1/|o_i|}$$

即长度归一化的几何平均。

### 7.2 目标

$$\mathcal{J}_{GSPO}(\theta) = \mathbb{E}\left[\frac{1}{G}\sum_{i=1}^{G}\min\left(s_i\hat{A}_i,\;\mathrm{clip}(s_i,1-\epsilon,1+\epsilon)\hat{A}_i\right)\right]$$

### 7.3 为什么序列级更稳

```text
token 级比率：
  每个 token 单独算比率
  长序列上多个比率的乘积容易爆炸或消失
  MoE 路由变化会让某些 token 比率剧烈波动

序列级比率：
  整段回答一个比率
  几何平均抑制极端值
  与"整段回答的奖励"粒度一致

效果：
  训练更稳定
  MoE 模型收益更明显
  长 CoT 场景方差更小
```

### 7.4 GSPO 的代价

```text
粒度变粗：
  无法区分回答内部"哪些 token 好、哪些 token 坏"
  对细粒度信用分配不友好

适用：
  整段回答质量主要由结果决定（数学、代码）
  不太适合需要 token 级精细控制的场景
```

---

## 8. Dr.GRPO 与其他变体

### 8.1 Dr.GRPO

去掉 std 归一化，只用组内均值：

$$\hat{A}_i = r_i - \mathrm{mean}(r)$$

理由：

```text
std 归一化会让"组内差异小"的题目获得过大权重
  → 奖励 hacking 与长度偏差被放大
去掉 std 后，优势尺度更稳定
```

### 8.2 其他 2025-2026 变体

| 变体 | 核心改动 | 目标 |
|------|---------|------|
| CISPO | 裁剪重要性采样权重而非比率 | 稳定长序列 |
| VAPO | 值函数辅助 + 长度自适应 | 提升长 CoT |
| RPT | 重复惩罚与 token 级重加权 | 抑制重复 |
| REAL | 把 RLVR 重构为分类问题 | 修正正负样本梯度错配 |
| SCOPE-RL | 奖励稠密化（过程奖励） | 提升样本效率 |
| GVPO | 过程可验证的优势塑形 | 交互式编码 Agent |

2026 年的研究方向可以概括为三条：

```text
① 奖励稠密化
   从"整段一个分"走向"过程每一步有信号"
   PRM、StepPO、SCOPE-RL

② 优化器稳定化
   GSPO 序列级比率、CISPO、Dr.GRPO
   解决 MoE、长 CoT、长度偏差

③ 训练范式重构
   REAL 把 RLVR 当分类问题
   GVPO 把优势塑形塞进过程验证
```

---

## 9. RLVR：可验证奖励

### 9.1 定义

RLVR（Reinforcement Learning with Verifiable Rewards）：用可程序化验证的信号作为奖励，而不是人类偏好或奖励模型。

```text
可验证的奖励来源：
  ① 数学：最终答案与标准答案比对
  ② 代码：单元测试通过率
  ③ 格式：正则/解析器校验
  ④ 工具调用：API 返回结果是否正确
  ⑤ 检索：引用是否支持结论
  ⑥ 约束：长度、语言、结构
```

### 9.2 为什么 RLVR 成为主流

| 维度 | RLHF | RLVR |
|------|------|------|
| 奖励来源 | 人类偏好 → RM | 规则/执行器 |
| 成本 | 高（标注 + RM 训练） | 低（程序验证） |
| 可扩展性 | 受标注限制 | 近无限 |
| 奖励准确性 | 有偏（标注者） | 精确（对可验证任务） |
| 覆盖范围 | 通用 | 仅可验证任务 |
| 奖励黑客 | 严重 | 较轻（但仍存在） |

### 9.3 RLVR 的边界

```text
适合：
  数学、代码、结构化输出、工具调用、逻辑推理
  有明确对错标准的任务

不适合：
  创意写作、开放式问答、主观质量
  没有可验证标准的任务

折中：
  混合奖励 = 规则奖励 + RM 奖励
  规则保底线，RM 管质量
```

---

## 10. RLVR 训练管线

以 DeepSeek-R1 系列为代表的四阶段管线：

```text
阶段一：冷启动 SFT
  用少量高质量长 CoT 数据做 SFT
  目的：给 RL 一个像样的起点，避免早期全是无效输出

阶段二：RLVR（GRPO）
  大规模可验证任务
  数学、代码、格式、工具
  目的：激发推理能力

阶段三：拒绝采样 + SFT
  用训练好的模型生成大量回答
  过滤掉错误答案，保留正确且格式好的
  做新一轮 SFT，把 RL 能力"固化"进模型

阶段四：全场景对齐
  通用对话、安全、有用性
  用偏好数据/DPO/RLHF 补齐
```

为什么需要拒绝采样 + SFT：

```text
RL 提升的是"采样到正确答案的概率"
但 RL 训练不稳定，且可能带来格式与语言混杂
用拒绝采样挑出高质量回答做 SFT：
  把 RL 的探索成果蒸馏回监督模型
  稳定输出格式
  降低推理时的方差
```

---

## 11. 推理模型的 RL 实践

### 11.1 奖励设计

```text
数学：
  答案精确匹配（需处理等价形式：分数、根号、单位）
  格式奖励（把答案放在 \boxed{} 里）
  语言一致性奖励（防止中英混杂）

代码：
  单元测试通过率
  编译通过
  运行时限制（超时、内存）
  隐藏测试防止过拟合

通用：
  长度惩罚（防止无限推理）
  重复惩罚
  格式校验
```

### 11.2 训练稳定性

```text
① 课程学习
   从简单题开始，逐步提高难度

② 动态采样
   过滤全对/全错的组

③ 熵监控
   熵过低说明探索不足，需要调温度或加熵奖励

④ KL 约束
   DeepSeek V3.2 在数学 RL 中采用 zero-KL
   → 说明对可验证任务，参考模型的约束可以放松
   → 但通用能力仍需 KL 保护

⑤ 长度控制
   防止"想得越久越好"的退化
```

### 11.3 评估

```text
① Pass@1：一次回答的正确率
② Pass@k：k 次采样中至少一次正确
③ 平均回答长度：监控是否失控
④ 熵/多样性：监控是否坍缩
⑤ 通用能力回测：防止"数学变强、通用变笨"
```

---

## 12. 2026 前沿方向

```text
① 过程奖励（PRM）
   对推理的每一步打分
   解决稀疏奖励与信用分配
   难点：过程标注成本高、容易奖励黑客

② 奖励稠密化
   SCOPE-RL 等工作用"自适应脚手架 + 质量感知过程 RL"
   论文报告在 Qwen3-8B 上提升 11.2 个百分点、减少 27.1% token

③ 分类视角重构
   REAL 把 RLVR 看作分类问题
   指出 GRPO/DAPO/GSPO 的正负样本梯度错配
   论文报告在 1.5B 上比 DAPO 提升 6.7% Pass@1

④ 优势塑形
   GVPO 在交互式编码 Agent 上用过程可验证信号塑形优势
   StepPO 做步骤级奖励

⑤ Agentic RL
   工具调用、多轮交互、环境反馈
   奖励来自任务完成度而非单轮回答

⑥ 多模态 RL
   视觉推理、GUI 操作
   奖励来自执行结果与环境状态
```

> **提示**：这些方向更新极快，论文数字只在特定设置下成立。工程选型时先复现基线，再评估新方法，不要直接照搬论文结论。

---

## 13. 代码实现

### 13.1 GRPO 损失

```python
import torch
import torch.nn as nn


def grpo_loss(policy_logps, old_logps, ref_logps, rewards,
              response_mask, beta=0.01, epsilon=0.2):
    group_size = rewards.shape[0]
    mean_reward = rewards.mean()
    std_reward = rewards.std() + 1e-8
    advantages = (rewards - mean_reward) / std_reward

    ratio = torch.exp(policy_logps - old_logps)

    token_advantages = advantages.unsqueeze(1).expand_as(ratio)

    unclipped = ratio * token_advantages
    clipped = torch.clamp(ratio, 1.0 - epsilon, 1.0 + epsilon) * token_advantages
    policy_loss = -torch.min(unclipped, clipped)

    kl = torch.exp(ref_logps - policy_logps) - (ref_logps - policy_logps) - 1.0
    per_token_loss = policy_loss + beta * kl

    mask = response_mask.float()
    loss = (per_token_loss * mask).sum() / mask.sum()

    with torch.no_grad():
        approx_kl = ((old_logps - policy_logps) * mask).sum() / mask.sum()
        clip_fraction = (((ratio - 1.0).abs() > epsilon).float() * mask).sum() / mask.sum()

    return loss, {
        "approx_kl": float(approx_kl.item()),
        "clip_fraction": float(clip_fraction.item()),
        "mean_reward": float(mean_reward.item()),
    }
```

### 13.2 组内优势（含零梯度组检测）

```python
def compute_group_advantages(rewards, eps=1e-8):
    rewards = torch.as_tensor(rewards, dtype=torch.float32)
    std = rewards.std()
    if std < eps:
        return None
    return (rewards - rewards.mean()) / (std + eps)
```

### 13.3 可验证奖励示例

```python
import re


def math_reward(response, ground_truth):
    match = re.search(r"\\boxed\{([^}]*)\}", response)
    if not match:
        return 0.0
    answer = match.group(1).strip()
    return 1.0 if normalize(answer) == normalize(ground_truth) else 0.0


def normalize(text):
    return text.replace(" ", "").replace(",", "").rstrip(".").lower()
```

---

## 14. 面试问法

**问：GRPO 和 PPO 的区别？**

详细版：GRPO 去掉了 Critic，用同一 prompt 的 G 个采样回答的组内奖励均值作为基线，标准化后得到优势。这样模型从四个减到三个，显存显著下降，超参数也少一个。代价是每个 prompt 要采样 G 个回答，采样成本上升，且组内全对/全错时没有梯度。

简短版：GRPO 用组内平均奖励替代 Critic，省掉价值网络，用采样换显存。

**问：GRPO 的优势怎么算？**

详细版：对每个问题采样 G 个回答，得到奖励 $r_1,\ldots,r_G$，优势是 $(r_i-\mathrm{mean})/\mathrm{std}$。它衡量该回答比同组其他回答好多少，同一回答内所有 token 共享这个优势。

简短版：组内标准化奖励，$(r_i-\text{均值})/\text{标准差}$。

**问：DAPO 改了什么？**

详细版：四项。Clip-Higher 解耦上下界提高探索；动态采样过滤零梯度组；Token-level 损失让长回答的每个 token 都有效；Overlong Reward Shaping 对超长回答软惩罚。四项共同解决 GRPO 的熵崩塌、零梯度、长度偏差与截断噪声。

简短版：Clip-Higher、动态采样、token 级损失、超长奖励塑形。

**问：GSPO 为什么用序列级比率？**

详细版：token 级比率在长序列上容易累积误差，MoE 路由变化会放大波动。GSPO 用整段回答的几何平均比率，粒度与序列级奖励一致，训练更稳定，长 CoT 与 MoE 场景收益明显。代价是无法做 token 级精细信用分配。

简短版：序列级几何平均比率，抑制长序列与 MoE 的比率波动。

**问：什么是 RLVR？**

详细版：用可程序化验证的奖励做 RL，比如数学答案比对、代码单测、格式校验。相比 RLHF，它不需要奖励模型、成本低、可扩展，适合有明确对错的任务。DeepSeek-R1 的推理能力就是靠 RLVR + GRPO 激发的。局限是只适用于可验证任务。

简短版：可验证奖励的 RL，用答案/单测/规则打分，替代人类偏好。

**问：GRPO 什么时候没有梯度？**

详细版：同一 prompt 的 G 个采样回答全部正确或全部错误时，组内奖励方差为 0，标准化后优势全为 0，整组不产生梯度。DAPO 的动态采样会过滤这类组并重新采样。

简短版：组内全对或全错，方差为 0，优势为 0。

---

## 15. 小结

| 问题 | 答案 |
|------|------|
| GRPO 的核心 | 组内平均替代 Critic |
| 优势公式 | $(r_i-\mathrm{mean})/\mathrm{std}$ |
| GRPO vs PPO | 三模型 vs 四模型，采样换显存 |
| 零梯度组 | 全对/全错，方差为 0 |
| DAPO 四修 | Clip-Higher、动态采样、token 级损失、超长塑形 |
| GSPO | 序列级几何平均比率 |
| Dr.GRPO | 去掉 std 归一化 |
| RLVR | 可验证奖励，规则/单测/答案比对 |
| RLVR 管线 | 冷启动 SFT → RLVR → 拒绝采样 → 全场景对齐 |
| 2026 趋势 | 过程奖励、奖励稠密化、优势塑形、Agentic RL |

**下一模块**：[09-工程实践与训练框架](09-工程实践与训练框架.md)　**返回总览**：[00-强化学习知识体系总览](00-强化学习知识体系总览.md)
