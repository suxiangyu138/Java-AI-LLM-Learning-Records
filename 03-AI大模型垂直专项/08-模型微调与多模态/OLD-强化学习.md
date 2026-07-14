# 强化学习

> **优先级**：🟢 拓展选修 | **类型**：校内提升专业课 | **方向**：AI 能力拔高·前沿

## 课程定位

**提升点**：MDP建模与求解、基于价值/策略的RL算法、深度强化学习、RLHF、多臂老虎机

**适用人群**：想做智能交互机器人/自动驾驶/对话AI/决策系统、对Agent训练感兴趣

**学习目标**：理解MDP框架、能手推Q-Learning和Policy Gradient、理解RLHF在ChatGPT中的应用

---

# 第一章：强化学习基础
- 强化学习特点：无监督信号→通过奖惩学习；时序决策→当前动作影响未来
- MDP正式定义：(S, A, P, R, γ)——策略π是状态到动作概率分布的映射
- 回报：G_t = R_{t+1} + γR_{t+2} + γ²R_{t+3} + ...（γ∈[0,1]折扣未来奖励）
- 价值函数：V_π(s) = E_π[G_t|s_t=s]——从状态s出发，策略π的期望回报
- 动作价值函数：Q_π(s,a) = E_π[G_t|s_t=s, a_t=a]——额外指定了第一个动作
- 贝尔曼方程：V_π(s) = Σ_a π(a|s) Σ_{s',r} P(s',r|s,a)[r + γV_π(s')]

# 第二章：动态规划（已知模型）
- 策略评估：迭代求解 V_π——贝尔曼方程的迭代形式
- 策略改进：贪心选取 argmax Q_π(s,a)——策略改进定理保证不退化
- 策略迭代：评估→改进→评估→改进→收敛到最优——轮次多
- 价值迭代：跳过完整的策略评估——每次只迭代一次就改进——更高效

# 第三章：免模型学习（无环境模型）
- 蒙特卡洛方法（MC）：完整采样序列→用实际回报更新——需要 episode 结束
  - 首次访问 vs 每次访问：只计首次出现状态的回报 vs 所有出现都计数
- 时序差分（TD）：
  - TD(0)：V(s_t)←V(s_t)+α[R_{t+1}+γV(s_{t+1})-V(s_t)]——单步采样即更新
  - SARSA：On-policy——Q(s,a)←Q(s,a)+α[R+γQ(s',a')-Q(s,a)]——a' 由策略π选
  - Q-Learning：Off-policy——Q(s,a)←Q(s,a)+α[R+γmax_a'Q(s',a')-Q(s,a)]——不管行为策略直接取最大Q值
- MC vs TD：MC 无偏但高方差；TD 有偏（自举）但低方差

# 第四章：函数近似与DQN
- 价值函数近似：Q(s,a;θ)——用神经网络参数化 Q 函数
- DQN 三大创新：
  1. 经验回放(Experience Replay)：随机抽样历史经验——打破数据相关性
  2. 目标网络(Target Network)：Q-target 网络定期复制——稳定训练目标
  3. 奖励裁剪：奖励归一化到[-1,1]
- DQN 改进：Double DQN(解耦选择和评价)、Dueling DQN(分离V(s)和A(s,a))、Prioritized Replay(优先采样TD误差大的)

# 第五章：策略梯度方法
- Policy Gradient：直接参数化策略π(a|s;θ)→参数梯度∝E[∇logπ·Q]
- REINFORCE：用实际回报G_t替代Q→但方差大 →引入基线b(s)减方差
- Actor-Critic：Actor(策略网络)决定动作，Critic(价值网络)评估好坏——降低方差
- A2C/A3C：Advantage函数A=Q-V代替Q——多线程异步训练
- PPO：近端策略优化——限制更新幅度(Clip)——RLHF的核心算法

# 第六章：RLHF与大模型
- RLHF 三步流程：
  1. SFT：用人类标注的高质量回答监督微调
  2. Reward Model：人对回答排序→训练打分模型
  3. PPO：用RM的分数作为奖励→PPO优化语言模型
- KL 惩罚：防止RL过程中模型偏离预训练太远

# 第七章：探索与利用
- 多臂老虎机：ε-贪心→UCB→Thompson采样
- 高级探索：内在激励(好奇心/状态新颖性)、计数探索、随机网络蒸馏

---

## 推荐教材
- 《Reinforcement Learning: An Introduction》——Sutton & Barto（RL圣经）
- OpenAI Spinning Up 在线教程

## 实验建议
- 用 OpenAI Gym 训练 DQN 玩 CartPole、PPO 玩 LunarLander
