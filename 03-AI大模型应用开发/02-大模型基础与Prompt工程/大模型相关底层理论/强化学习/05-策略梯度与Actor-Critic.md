# 05-策略梯度与 Actor-Critic

> 值函数方法先学 Q 再选动作，适合动作空间小的场景。策略梯度直接对"生成动作的概率"求梯度，天然适配巨大离散空间（如词表）与连续控制。这一篇推导策略梯度定理、解释基线为什么能降方差、引出 Actor-Critic 与 GAE，是 PPO 与 GRPO 的共同前置。

---

## 目录

1. [为什么直接优化策略](#1-为什么直接优化策略)
2. [目标函数](#2-目标函数)
3. [策略梯度定理](#3-策略梯度定理)
4. [REINFORCE](#4-reinforce)
5. [基线降方差](#5-基线降方差)
6. [Actor-Critic](#6-actor-critic)
7. [优势函数与 GAE](#7-优势函数与-gae)
8. [熵正则](#8-熵正则)
9. [A2C 与 A3C](#9-a2c-与-a3c)
10. [代码实现](#10-代码实现)
11. [策略方法的取舍](#11-策略方法的取舍)
12. [小结](#12-小结)

---

## 1. 为什么直接优化策略

值函数方法需要 $\arg\max_a Q(s,a)$，在以下场景不可行：

```text
① 连续动作空间
   动作是实数向量（机器人关节角度、方向盘转角）
   → 无法枚举所有动作取 max

② 巨大离散空间
   词表 5 万到 20 万 token
   → 每个 token 都要估 Q 值，成本高

③ 需要随机策略
   石头剪刀布的纳什均衡是随机策略
   → 确定性策略会被针对
```

策略梯度直接参数化策略 $\pi_\theta(a\mid s)$，对期望回报求梯度，绕开了 $\arg\max$。

---

## 2. 目标函数

强化学习的目标是最大化期望回报：

$$J(\theta) = \mathbb{E}_{\tau\sim\pi_\theta}\left[R(\tau)\right]$$

其中轨迹 $\tau=(s_0,a_0,r_0,\ldots)$，$R(\tau)$ 可以是总回报或折扣回报。

写成对轨迹分布求和：

$$J(\theta) = \sum_{\tau}p_\theta(\tau)R(\tau)$$

轨迹概率：

$$p_\theta(\tau) = \rho_0(s_0)\prod_{t=0}^{T-1}\pi_\theta(a_t\mid s_t)P(s_{t+1}\mid s_t,a_t)$$

关键观察：**只有 $\pi_\theta$ 依赖 $\theta$**，初始分布与环境转移都不依赖。这是推导能成立的核心。

---

## 3. 策略梯度定理

### 3.1 推导

对目标函数求梯度：

$$
\nabla_\theta J(\theta) = \nabla_\theta\sum_\tau p_\theta(\tau)R(\tau)
= \sum_\tau \nabla_\theta p_\theta(\tau)R(\tau)
$$

用对数导数技巧（log-derivative trick）：

$$\nabla_\theta p_\theta(\tau) = p_\theta(\tau)\nabla_\theta\log p_\theta(\tau)$$

代入：

$$\nabla_\theta J(\theta) = \sum_\tau p_\theta(\tau)\nabla_\theta\log p_\theta(\tau)R(\tau)
= \mathbb{E}_{\tau\sim\pi_\theta}\left[\nabla_\theta\log p_\theta(\tau)R(\tau)\right]$$

展开 $\log p_\theta(\tau)$：

$$
\log p_\theta(\tau) = \log\rho_0(s_0) + \sum_{t=0}^{T-1}\left[\log\pi_\theta(a_t\mid s_t) + \log P(s_{t+1}\mid s_t,a_t)\right]
$$

对 $\theta$ 求梯度，环境项与初始分布项都消失：

$$\nabla_\theta\log p_\theta(\tau) = \sum_{t=0}^{T-1}\nabla_\theta\log\pi_\theta(a_t\mid s_t)$$

因此：

$$
\boxed{
\nabla_\theta J(\theta) = \mathbb{E}_{\tau\sim\pi_\theta}\left[\sum_{t=0}^{T-1}\nabla_\theta\log\pi_\theta(a_t\mid s_t)\,G_t\right]
}
$$

其中 $G_t$ 是从 $t$ 时刻起的回报。

### 3.2 因果性修正

$t$ 时刻的动作不影响 $t$ 之前的奖励，所以 $G_t$ 可以替换为"从 $t$ 开始的回报"：

$$\nabla_\theta J(\theta) = \mathbb{E}\left[\sum_{t}\nabla_\theta\log\pi_\theta(a_t\mid s_t)\sum_{k=t}^{T-1}\gamma^{k-t}r_k\right]$$

这一步不改变期望，但显著降低方差。

### 3.3 定理的三种等价形式

| 形式 | 表达式 | 用途 |
|------|--------|------|
| 轨迹形式 | $\mathbb{E}[\sum_t\nabla\log\pi_\theta(a_t\mid s_t)G_t]$ | 最直观 |
| 状态分布形式 | $\sum_s d^\pi(s)\sum_a\nabla\pi_\theta(a\mid s)Q^\pi(s,a)$ | 理论分析 |
| 优势形式 | $\mathbb{E}[\sum_t\nabla\log\pi_\theta(a_t\mid s_t)A^\pi(s_t,a_t)]$ | 实际算法 |

---

## 4. REINFORCE

REINFORCE 是最直接的蒙特卡洛策略梯度：

```text
采样一条完整轨迹
对每个 t 计算 G_t
θ ← θ + α Σ_t ∇log π_θ(a_t|s_t) G_t
```

算法流程：

```text
for episode in range(N):
    采样轨迹 τ ~ π_θ
    for t in range(T):
        G_t = Σ_{k=t}^{T-1} γ^{k-t} r_k
        θ ← θ + α ∇log π_θ(a_t|s_t) G_t
```

特点：

```text
优点：
  无偏
  不需要环境模型
  适用于任何可微的策略

缺点：
  方差极大（G_t 含整条轨迹的随机性）
  样本效率低（一条轨迹只用一次）
  必须等回合结束
```

---

## 5. 基线降方差

### 5.1 为什么可以减基线

对任意只依赖状态的函数 $b(s)$：

$$\mathbb{E}_{a\sim\pi_\theta}\left[\nabla_\theta\log\pi_\theta(a\mid s)b(s)\right] = b(s)\sum_a\nabla_\theta\pi_\theta(a\mid s) = b(s)\nabla_\theta\underbrace{\sum_a\pi_\theta(a\mid s)}_{=1} = 0$$

所以减去基线不改变梯度的期望：

$$\nabla_\theta J(\theta) = \mathbb{E}\left[\sum_t\nabla_\theta\log\pi_\theta(a_t\mid s_t)\left(G_t - b(s_t)\right)\right]$$

### 5.2 最优基线

使方差最小的基线是状态价值：

$$b^*(s) = V^\pi(s)$$

此时：

$$G_t - b(s_t) \approx Q^\pi(s_t,a_t) - V^\pi(s_t) = A^\pi(s_t,a_t)$$

即**优势函数**。

```text
直观：
  G_t 是"这个动作实际拿了多少"
  V(s_t) 是"平均能拿多少"
  两者相减 = "这个动作比平均好多少"
  → 只关心相对好坏，去掉状态本身的难度差异
```

### 5.3 方差的来源

```text
G_t 的方差来自：
  ① 环境随机性（转移、奖励）
  ② 策略随机性（采样动作）
  ③ 轨迹长度

减基线消除的是"状态难度"带来的共同项
  → 同一状态下，好动作与坏动作的差异被放大
  → 梯度信号更清晰
```

---

## 6. Actor-Critic

Actor-Critic 用两个网络：

```text
Actor（策略网络 π_θ）：
  输出动作分布
  按策略梯度更新

Critic（价值网络 V_φ）：
  估计状态价值
  作为基线，并计算优势
```

更新式：

$$\delta_t = r_t + \gamma V_\phi(s_{t+1}) - V_\phi(s_t)$$

$$\theta \leftarrow \theta + \alpha_\theta\,\delta_t\,\nabla_\theta\log\pi_\theta(a_t\mid s_t)$$

$$\phi \leftarrow \phi - \alpha_\phi\,\nabla_\phi\left(\delta_t\right)^2$$

```text
与 REINFORCE 的区别：
  REINFORCE 用完整回报 G_t
  Actor-Critic 用 TD 目标 r + γV(s')
  → 方差更小，可在线更新
  → 但引入 Critic 的偏差
```

偏差-方差的权衡：

| 方法 | 基线 | 偏差 | 方差 |
|------|------|------|------|
| REINFORCE | 无 | 无偏 | 大 |
| REINFORCE + baseline | $V(s)$ | 无偏（基线不引入偏差） | 中 |
| Actor-Critic（一步 TD） | $V(s)$ | 有偏（自举） | 小 |
| GAE | 多步加权 | 可调 | 可调 |

---

## 7. 优势函数与 GAE

### 7.1 TD 误差作为优势估计

一步 TD 误差：

$$\delta_t = r_t + \gamma V(s_{t+1}) - V(s_t)$$

它是优势 $A(s_t,a_t)$ 的有偏估计（偏差来自 $V$ 不准）。

### 7.2 n-step 优势

$$A_t^{(n)} = \sum_{l=0}^{n-1}\gamma^l r_{t+l} + \gamma^n V(s_{t+n}) - V(s_t)$$

### 7.3 GAE 推导

广义优势估计（GAE）对 n-step 优势做指数加权平均：

$$
\begin{aligned}
A_t^{GAE(\gamma,\lambda)}
&= (1-\lambda)\sum_{n=1}^{\infty}\lambda^{n-1}A_t^{(n)} \\
&= \sum_{l=0}^{\infty}(\gamma\lambda)^l\delta_{t+l}
\end{aligned}
$$

其中：

$$\delta_t = r_t + \gamma V(s_{t+1}) - V(s_t)$$

两个极端：

```text
λ=0：A_t = δ_t（一步 TD，偏差大、方差小）
λ=1：A_t = Σ γ^l r_{t+l} - V(s_t)（蒙特卡洛，无偏、方差大）
λ=0.95：常用折中
```

### 7.4 递推实现

GAE 可以从后往前递推，$O(T)$ 计算：

$$A_t = \delta_t + \gamma\lambda A_{t+1}$$

```python
def compute_gae(rewards, values, dones, gamma=0.99, lam=0.95):
    advantages = []
    gae = 0.0
    for t in reversed(range(len(rewards))):
        if t == len(rewards) - 1:
            next_value = 0.0
        else:
            next_value = values[t + 1]
        delta = rewards[t] + gamma * next_value * (1.0 - dones[t]) - values[t]
        gae = delta + gamma * lam * (1.0 - dones[t]) * gae
        advantages.insert(0, gae)
    returns = [adv + value for adv, value in zip(advantages, values)]
    return advantages, returns
```

> **要点**：PPO 用 GAE 估优势，GRPO 用组内标准化估优势。两者都是"优势估计"，只是前者依赖 Critic，后者依赖组采样。理解 GAE 的偏差-方差权衡，才能理解 GRPO 为什么愿意多花采样成本换掉 Critic。

---

## 8. 熵正则

为了鼓励探索、防止策略过早收敛到确定性动作，在目标里加策略熵：

$$J(\theta) = \mathbb{E}\left[R\right] + \beta\,\mathbb{E}\left[\mathcal{H}(\pi_\theta(\cdot\mid s))\right]$$

熵的定义：

$$\mathcal{H}(\pi(\cdot\mid s)) = -\sum_a\pi(a\mid s)\log\pi(a\mid s)$$

```text
熵高 → 动作分布均匀 → 探索强
熵低 → 分布集中 → 利用强

β 的作用：
  β 大 → 更随机、探索多
  β 小 → 更确定、利用多
  β 通常随训练衰减
```

LLM 场景：

```text
熵正则对应"生成多样性"
  RLHF/GRPO 里通常用 KL 约束而非熵正则
  原因：KL 约束的是"相对参考模型"，熵只约束"自身分布"
  → KL 能防跑偏，熵只防过确定
```

---

## 9. A2C 与 A3C

| 算法 | 并行方式 | 特点 |
|------|---------|------|
| A3C | 多线程异步 | 各线程用不同环境副本，异步更新全局参数 |
| A2C | 多环境同步 | 等所有环境采完再统一更新，实现更简单 |

```text
A3C 的问题：
  异步更新导致梯度陈旧（用旧参数算的梯度更新新参数）
  GPU 利用率低（多线程 Python 有 GIL）

A2C 的优势：
  同步批处理，适合 GPU
  梯度一致性更好
  → 现代实现多用 A2C 思路（向量化环境）
```

---

## 10. 代码实现

### 10.1 REINFORCE

```python
import torch
import torch.nn as nn
import torch.optim as optim


class PolicyNetwork(nn.Module):

    def __init__(self, state_dim, action_dim, hidden_dim=128):
        super().__init__()
        self.net = nn.Sequential(
            nn.Linear(state_dim, hidden_dim),
            nn.ReLU(),
            nn.Linear(hidden_dim, action_dim),
        )

    def forward(self, state):
        logits = self.net(state)
        return torch.distributions.Categorical(logits=logits)


def reinforce(env, episodes=1000, gamma=0.99, lr=1e-3):
    state_dim = env.observation_space.shape[0]
    action_dim = env.action_space.n
    policy = PolicyNetwork(state_dim, action_dim)
    optimizer = optim.Adam(policy.parameters(), lr=lr)

    for episode in range(episodes):
        log_probs = []
        rewards = []
        state = env.reset()

        done = False
        while not done:
            state_tensor = torch.as_tensor(state, dtype=torch.float32)
            dist = policy(state_tensor)
            action = dist.sample()
            log_probs.append(dist.log_prob(action))
            state, reward, done, _ = env.step(action.item())
            rewards.append(reward)

        returns = []
        running = 0.0
        for reward in reversed(rewards):
            running = reward + gamma * running
            returns.insert(0, running)

        returns = torch.as_tensor(returns, dtype=torch.float32)
        returns = (returns - returns.mean()) / (returns.std() + 1e-8)

        loss = -torch.stack([log_prob * ret for log_prob, ret in zip(log_probs, returns)]).sum()

        optimizer.zero_grad()
        loss.backward()
        optimizer.step()
```

### 10.2 Actor-Critic

```python
class ActorCritic(nn.Module):

    def __init__(self, state_dim, action_dim, hidden_dim=128):
        super().__init__()
        self.shared = nn.Sequential(
            nn.Linear(state_dim, hidden_dim),
            nn.ReLU(),
        )
        self.actor = nn.Linear(hidden_dim, action_dim)
        self.critic = nn.Linear(hidden_dim, 1)

    def forward(self, state):
        features = self.shared(state)
        return self.actor(features), self.critic(features).squeeze(-1)


def actor_critic_step(model, optimizer, state, action, reward, next_state, done,
                      gamma=0.99, value_coef=0.5, entropy_coef=0.01):
    logits, value = model(state)
    dist = torch.distributions.Categorical(logits=logits)

    with torch.no_grad():
        _, next_value = model(next_state)
        target = reward + gamma * next_value * (1.0 - done)
        advantage = target - value

    actor_loss = -(dist.log_prob(action) * advantage.detach()).mean()
    critic_loss = advantage.pow(2).mean()
    entropy = dist.entropy().mean()

    loss = actor_loss + value_coef * critic_loss - entropy_coef * entropy

    optimizer.zero_grad()
    loss.backward()
    optimizer.step()
    return float(loss.item())
```

---

## 11. 策略方法的取舍

| 维度 | 值函数方法 | 策略梯度 |
|------|-----------|---------|
| 动作空间 | 离散、小 | 连续、巨大离散 |
| 策略类型 | 确定性（或 $\epsilon$-贪心） | 天然随机 |
| 样本效率 | 高（可 off-policy 复用） | 低（on-policy） |
| 方差 | 低 | 高（需基线） |
| 收敛稳定性 | 中（易过估计） | 中（易崩塌） |
| 适用 | Atari、小规模控制 | 机器人、LLM |

```text
选择原则：
  动作空间小且离散 → DQN 系列
  动作连续或巨大 → 策略梯度
  需要随机策略 → 策略梯度
  样本昂贵 → 值函数或 off-policy 方法
```

---

## 12. 小结

| 问题 | 答案 |
|------|------|
| 策略梯度定理 | $\nabla J=\mathbb{E}[\sum_t\nabla\log\pi_\theta(a_t\mid s_t)G_t]$ |
| 对数导数技巧 | $\nabla p=p\nabla\log p$ |
| 基线为什么有效 | 期望为 0，不引入偏差，只降方差 |
| 最优基线 | 状态价值 $V(s)$ |
| 优势函数 | $Q-V$，动作相对平均的好坏 |
| Actor-Critic | Actor 学策略，Critic 做基线 |
| GAE | 多步 TD 误差的指数加权，$\lambda$ 调偏差方差 |
| 熵正则 | 鼓励探索，防策略过早确定 |
| LLM 的对应 | 策略=语言模型，优势=组内相对好坏 |

**下一模块**：[06-PPO与TRPO](06-PPO与TRPO.md)　**返回总览**：[00-强化学习知识体系总览](00-强化学习知识体系总览.md)
