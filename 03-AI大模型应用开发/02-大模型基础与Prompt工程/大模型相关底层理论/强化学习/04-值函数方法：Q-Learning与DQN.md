# 04-值函数方法：Q-Learning 与 DQN

> 值函数方法不直接学策略，而是先学"每个动作值多少分"，再选分最高的动作。Q-Learning 是表格版的代表，DQN 用神经网络把它扩展到高维状态。理解 DQN 的两个稳定器（经验回放、目标网络）与两类偏差（最大化偏差、过估计），是理解后续所有深度 RL 的基础。

---

## 目录

1. [无模型控制问题](#1-无模型控制问题)
2. [SARSA：on-policy 控制](#2-sarsaon-policy-控制)
3. [Q-Learning：off-policy 控制](#3-q-learningoff-policy-控制)
4. [收敛条件与局限](#4-收敛条件与局限)
5. [最大化偏差与 Double Q-Learning](#5-最大化偏差与-double-q-learning)
6. [DQN：深度值函数](#6-dqn深度值函数)
7. [DQN 的两个稳定器](#7-dqn-的两个稳定器)
8. [DQN 变体家族](#8-dqn-变体家族)
9. [DQN 核心代码](#9-dqn-核心代码)
10. [为什么 LLM 不用 DQN](#10-为什么-llm-不用-dqn)
11. [小结](#11-小结)

---

## 1. 无模型控制问题

控制（control）的目标是找到最优策略，而不只是评估给定策略。

无模型控制的难点：

```text
没有 p(s',r|s,a)：
  无法枚举所有动作算期望
  → 只能用采样得到的 (s,a,r,s') 估计

GPI 框架仍然适用：
  用采样估计 Q → 对 Q 贪心改进策略 → 用新策略采样
```

把 GPI 落到 Q 函数上，就得到两类算法：

| 算法 | 行为策略 | 目标策略 | 类型 |
|------|---------|---------|------|
| SARSA | $\epsilon$-贪心 | $\epsilon$-贪心 | On-policy |
| Q-Learning | $\epsilon$-贪心 | 完全贪心 | Off-policy |

---

## 2. SARSA：on-policy 控制

SARSA 的名字来自它用到的五元组 $(S_t, A_t, R_{t+1}, S_{t+1}, A_{t+1})$。

更新式：

$$Q(S_t,A_t) \leftarrow Q(S_t,A_t) + \alpha\left[R_{t+1} + \gamma Q(S_{t+1},A_{t+1}) - Q(S_t,A_t)\right]$$

关键点：$A_{t+1}$ 是**实际按当前策略选出的动作**，不是贪心动作。

```text
On-policy 的含义：
  评估的策略 = 执行的策略
  所以 SARSA 学到的是"带探索的策略"的价值

后果：
  在有悬崖的环境里，SARSA 学到的策略更保守
  因为它把"随机探索掉下悬崖"的风险算进了价值
```

---

## 3. Q-Learning：off-policy 控制

Q-Learning 用"下一状态的最大 Q 值"作为目标：

$$Q(S_t,A_t) \leftarrow Q(S_t,A_t) + \alpha\left[R_{t+1} + \gamma\max_{a'}Q(S_{t+1},a') - Q(S_t,A_t)\right]$$

与 SARSA 的唯一差别是把 $Q(S_{t+1},A_{t+1})$ 换成了 $\max_{a'}Q(S_{t+1},a')$。

```text
Off-policy 的含义：
  行为策略（采样）：ε-贪心，保证探索
  目标策略（学习）：完全贪心，逼近最优

优势：
  可以用旧数据、探索数据学习最优策略
  样本效率高于 SARSA

代价：
  对函数逼近 + 离策略的组合不稳定（ deadly triad）
```

### 3.1 收敛条件

表格型 Q-Learning 收敛到 $Q^*$ 需要：

```text
① 每个 (s,a) 被无限次访问
② 步长满足 Robbins-Monro 条件
   Σα_t = ∞，Σα_t² < ∞
③ 奖励有界
④ γ < 1 或所有策略最终到达终止状态
```

---

## 4. 收敛条件与局限

表格型方法在状态多时不可行：

```text
状态数 = 10^6 → 维护 10^6 × |A| 的表
状态是连续值（图像、传感器）→ 表格根本存不下
```

于是需要函数逼近：

$$Q(s,a)\approx Q_\theta(s,a)$$

但函数逼近 + 自举 + 离策略三者同时出现时，会出现**致命三元组**（deadly triad）：

```text
① 函数逼近：用参数化模型代替表格
② 自举：用估计更新估计
③ 离策略：行为策略 ≠ 目标策略

三者同时满足 → 可能发散
DQN 的经验回放与目标网络，就是为缓解这个不稳定
```

---

## 5. 最大化偏差与 Double Q-Learning

### 5.1 问题

Q-Learning 的目标里有 $\max_{a'}Q(s',a')$。如果 Q 的估计有噪声：

$$\mathbb{E}\left[\max_a \hat{Q}(s,a)\right] \ge \max_a \mathbb{E}\left[\hat{Q}(s,a)\right]$$

取最大会系统性高估。这叫**最大化偏差**（maximization bias）。

```text
直觉：
  10 个动作的真实价值都是 0
  但估计各有噪声（有的 +0.1，有的 -0.1）
  max 会选中 +0.1 的那个
  → 估计的价值系统性偏高
```

### 5.2 Double Q-Learning

用两个独立的 Q 估计，一个选动作，另一个评估：

$$a^* = \arg\max_a Q_A(s',a)$$

$$Q_A(s,a) \leftarrow Q_A(s,a) + \alpha\left[r + \gamma Q_B(s',a^*) - Q_A(s,a)\right]$$

交换 A/B 角色交替更新。这样"选动作"和"估价值"用的不是同一个噪声源，偏差被抵消。

---

## 6. DQN：深度值函数

DQN（Deep Q-Network，2013/2015）用神经网络近似 Q 函数：

$$Q(s,a;\theta) \approx Q^*(s,a)$$

输入是原始状态（如 Atari 的 84×84×4 帧），输出是每个动作的 Q 值。

### 6.1 损失函数

$$L(\theta) = \mathbb{E}_{(s,a,r,s')\sim\mathcal{D}}\left[\left(r + \gamma\max_{a'}Q(s',a';\theta^-) - Q(s,a;\theta)\right)^2\right]$$

| 符号 | 含义 |
|------|------|
| $\theta$ | 在线网络参数（每步更新） |
| $\theta^-$ | 目标网络参数（周期性同步） |
| $\mathcal{D}$ | 经验回放缓冲区 |

### 6.2 与表格 Q-Learning 的对应

```text
表格：
  Q(s,a) 直接更新一个格子

DQN：
  用梯度下降让 Q(s,a;θ) 靠近目标
  目标里的 max Q(s',a';θ^-) 用目标网络算
  → 本质相同，只是"格子"变成了"网络输出"
```

---

## 7. DQN 的两个稳定器

### 7.1 经验回放（Experience Replay）

```text
做法：
  把 (s,a,r,s') 存入缓冲区（通常 10^5-10^6 条）
  训练时随机采样 mini-batch

解决两个问题：
  ① 样本相关性：连续帧高度相关，直接训练会震荡
     随机采样打破相关性，接近独立同分布
  ② 数据复用：一条经验可以被多次使用，提高样本效率
```

### 7.2 目标网络（Target Network）

```text
做法：
  维护两份参数 θ（在线）与 θ^-（目标）
  每 C 步把 θ 复制给 θ^-
  计算 TD 目标时用 θ^-

解决：
  如果目标和预测用同一个网络，每次更新都会移动目标
  → "追着自己的尾巴跑"，训练不稳定
  固定目标一段时间，让回归有稳定的靶子
```

### 7.3 两个稳定器的代价

```text
经验回放：
  只能用于 off-policy（数据来自旧策略）
  缓冲区占内存

目标网络：
  目标滞后，收敛变慢
  超参数 C（同步周期）敏感
```

---

## 8. DQN 变体家族

| 变体 | 解决的问题 | 核心改动 |
|------|-----------|---------|
| Double DQN | 过估计 | 在线网络选动作，目标网络估值 |
| Dueling DQN | 状态价值与动作优势分离 | 网络分两路输出 $V(s)$ 与 $A(s,a)$ |
| Prioritized Replay | 均匀采样浪费高 TD 误差样本 | 按 TD 误差优先级采样 |
| Noisy Nets | $\epsilon$-贪心探索不灵活 | 网络参数加噪声，学习探索程度 |
| Distributional DQN | 只学期望，丢失分布信息 | 学回报分布（C51、QR-DQN） |
| Rainbow | 集成以上所有 | 组合六个改进 |

Dueling 的分解：

$$Q(s,a) = V(s) + A(s,a) - \frac{1}{|\mathcal{A}|}\sum_{a'}A(s,a')$$

减去均值是为了解决 $V$ 与 $A$ 的不可识别性（同一个 Q 可以由多组 V/A 表示）。

---

## 9. DQN 核心代码

```python
import random
from collections import deque

import numpy as np
import torch
import torch.nn as nn
import torch.optim as optim


class QNetwork(nn.Module):

    def __init__(self, state_dim, action_dim, hidden_dim=128):
        super().__init__()
        self.net = nn.Sequential(
            nn.Linear(state_dim, hidden_dim),
            nn.ReLU(),
            nn.Linear(hidden_dim, hidden_dim),
            nn.ReLU(),
            nn.Linear(hidden_dim, action_dim),
        )

    def forward(self, state):
        return self.net(state)


class ReplayBuffer:

    def __init__(self, capacity=100000):
        self.buffer = deque(maxlen=capacity)

    def push(self, state, action, reward, next_state, done):
        self.buffer.append((state, action, reward, next_state, done))

    def sample(self, batch_size):
        batch = random.sample(self.buffer, batch_size)
        states, actions, rewards, next_states, dones = zip(*batch)
        return (
            np.array(states, dtype=np.float32),
            np.array(actions, dtype=np.int64),
            np.array(rewards, dtype=np.float32),
            np.array(next_states, dtype=np.float32),
            np.array(dones, dtype=np.float32),
        )

    def __len__(self):
        return len(self.buffer)


class DQNAgent:

    def __init__(self, state_dim, action_dim, gamma=0.99, lr=1e-3,
                 batch_size=64, target_update=1000, epsilon=0.1):
        self.action_dim = action_dim
        self.gamma = gamma
        self.batch_size = batch_size
        self.target_update = target_update
        self.epsilon = epsilon
        self.steps = 0

        self.online = QNetwork(state_dim, action_dim)
        self.target = QNetwork(state_dim, action_dim)
        self.target.load_state_dict(self.online.state_dict())
        self.optimizer = optim.Adam(self.online.parameters(), lr=lr)
        self.buffer = ReplayBuffer()

    def select_action(self, state):
        if random.random() < self.epsilon:
            return random.randrange(self.action_dim)
        with torch.no_grad():
            state_tensor = torch.as_tensor(state, dtype=torch.float32).unsqueeze(0)
            return int(self.online(state_tensor).argmax(dim=1).item())

    def store(self, state, action, reward, next_state, done):
        self.buffer.push(state, action, reward, next_state, done)

    def train_step(self):
        if len(self.buffer) < self.batch_size:
            return None

        states, actions, rewards, next_states, dones = self.buffer.sample(self.batch_size)
        states = torch.as_tensor(states)
        actions = torch.as_tensor(actions)
        rewards = torch.as_tensor(rewards)
        next_states = torch.as_tensor(next_states)
        dones = torch.as_tensor(dones)

        current_q = self.online(states).gather(1, actions.unsqueeze(1)).squeeze(1)

        with torch.no_grad():
            next_q = self.target(next_states).max(dim=1).values
            target_q = rewards + self.gamma * next_q * (1.0 - dones)

        loss = nn.functional.mse_loss(current_q, target_q)

        self.optimizer.zero_grad()
        loss.backward()
        nn.utils.clip_grad_norm_(self.online.parameters(), 10.0)
        self.optimizer.step()

        self.steps += 1
        if self.steps % self.target_update == 0:
            self.target.load_state_dict(self.online.state_dict())

        return float(loss.item())
```

Double DQN 的目标只需改两行：

```python
with torch.no_grad():
    next_actions = self.online(next_states).argmax(dim=1)
    next_q = self.target(next_states).gather(1, next_actions.unsqueeze(1)).squeeze(1)
    target_q = rewards + self.gamma * next_q * (1.0 - dones)
```

---

## 10. 为什么 LLM 不用 DQN

| 原因 | 说明 |
|------|------|
| 动作空间巨大 | 词表 5 万到 20 万，每个 token 都要输出一个 Q 值 |
| 序列决策 | 一句回答几十到几千步，DQN 的逐步自举极其昂贵 |
| 奖励稀疏 | 只有句末一个分数，中间步骤没有奖励 |
| 值函数难估 | "当前前缀的价值"高度依赖后续生成，难以准确估计 |
| 策略梯度更自然 | 直接对生成概率求梯度，天然适配语言模型 |

```text
DQN 的适用画像：
  离散、中小动作空间（十几个到几百个）
  状态可观测
  奖励可以逐步给（或可以进行 n-step 自举）

LLM 的画像：
  巨大离散动作空间
  部分可观测
  奖励稀疏（序列级）
  → 策略梯度 + 组基线（GRPO）更合适
```

不过 DQN 的思想仍然有用：GRPO 用"组内平均奖励"作为基线，本质是在没有 Critic 的情况下估计"这个回答比平均水平好多少"，与 DQN 用 $V(s)$ 做基线的目的相同。

---

## 11. 小结

| 问题 | 答案 |
|------|------|
| SARSA 与 Q-Learning 的区别 | SARSA 用实际下一动作（on-policy），Q-Learning 用 max（off-policy） |
| Q-Learning 收敛条件 | 全访问、步长条件、奖励有界 |
| 致命三元组 | 函数逼近 + 自举 + 离策略 → 可能发散 |
| 最大化偏差 | max 对噪声估计系统性高估 |
| Double Q 的解法 | 一个网络选动作，另一个估值 |
| DQN 两稳定器 | 经验回放、目标网络 |
| DQN 变体 | Double、Dueling、Prioritized、Noisy、Distributional、Rainbow |
| 为什么 LLM 不用 DQN | 动作空间巨大、奖励稀疏、序列太长 |

**下一模块**：[05-策略梯度与Actor-Critic](05-策略梯度与Actor-Critic.md)　**返回总览**：[00-强化学习知识体系总览](00-强化学习知识体系总览.md)
