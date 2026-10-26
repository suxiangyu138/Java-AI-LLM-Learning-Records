# 07-RLHF：人类反馈对齐

> 预训练让模型会说话，SFT 让模型会对话，RLHF 让模型说得"符合人类偏好"。它的核心不是 PPO，而是把一个无法直接优化的目标——"人类觉得哪个回答更好"——先蒸馏成奖励模型，再用 RL 优化策略。DPO 则证明这一步可以绕过 RL，直接用分类损失完成。

---

## 目录

1. [为什么需要对齐](#1-为什么需要对齐)
2. [RLHF 三阶段](#2-rlhf-三阶段)
3. [偏好数据采集](#3-偏好数据采集)
4. [奖励模型训练](#4-奖励模型训练)
5. [PPO 对齐阶段](#5-ppo-对齐阶段)
6. [KL 约束的作用](#6-kl-约束的作用)
7. [奖励黑客与缓解](#7-奖励黑客与缓解)
8. [DPO：绕过 RL 的对齐](#8-dpo绕过-rl-的对齐)
9. [Constitutional AI 与 RLAIF](#9-constitutional-ai-与-rlaif)
10. [工程挑战](#10-工程挑战)
11. [评估体系](#11-评估体系)
12. [局限与边界](#12-局限与边界)
13. [代码实现](#13-代码实现)
14. [面试问法](#14-面试问法)
15. [小结](#15-小结)

---

## 1. 为什么需要对齐

预训练目标是"预测下一个 token"，它优化的是"像人写的文本"，不是"对人有用"：

```text
预训练模型的问题：
  ① 不遵循指令：你问问题，它续写更多问题
  ② 有害输出：学到训练数据里的偏见、暴力、违法内容
  ③ 幻觉：流畅但事实错误
  ④ 偏好不明确：不知道要简洁还是详细、要肯定还是质疑
```

SFT 能解决一部分：

```text
SFT：用"指令-回答"对做监督学习
  能学会指令格式、基本对话
  但学不会"两个都不错的回答里哪个更好"
  → 需要偏好信号
```

RLHF 的位置：

```text
预训练 → SFT → 偏好对齐（RLHF/DPO）→ 部署
         ↑        ↑
      学"怎么做"   学"哪个更好"
```

---

## 2. RLHF 三阶段

```text
阶段一：SFT（监督微调）
  数据：人工写的高质量指令-回答对
  目标：让模型学会指令跟随
  产物：π_SFT（也是后续的参考模型）

阶段二：奖励模型（RM）
  数据：同一 prompt 的多个回答 + 人类排序
  目标：学习"哪个回答更好"的打分函数
  产物：r_φ(x, y)

阶段三：PPO 强化学习
  数据：模型自己生成的回答 + RM 打分
  目标：最大化 RM 分数，同时不偏离 SFT 太远
  产物：π_RL（对齐后的模型）
```

三个阶段的数据量级（经验值）：

| 阶段 | 数据量 | 标注成本 |
|------|--------|---------|
| SFT | 1 万到 10 万条 | 高（要写完整回答） |
| RM | 1 万到 10 万组偏好对 | 中（只需排序） |
| PPO | 模型自采样，无需人工 | 低（RM 自动打分） |

---

## 3. 偏好数据采集

### 3.1 数据形式

```text
(prompt x, chosen y_w, rejected y_l)

来源：
  ① 同一 prompt 采样多个回答 → 人工排序
  ② 不同模型/不同版本的输出对比
  ③ 用户反馈（点赞/点踩、重新生成）

标注要点：
  定义清晰的偏好标准（有用性、真实性、无害性、格式）
  多人标注 + 一致性校验
  保留分歧样本（用于评估标注质量）
```

### 3.2 排序 vs 打分

| 方式 | 优点 | 缺点 |
|------|------|------|
| 两两比较 | 标注简单、一致性好 | 数据量随候选数平方增长 |
| 绝对打分 | 一条数据一个分 | 标注者尺度不一致，噪声大 |
| 排序（多候选） | 信息量大 | 标注成本高 |

工业界主流是**两两比较**，因为它把"主观打分"转成"相对判断"，标注者更容易达成一致。

---

## 4. 奖励模型训练

### 4.1 Bradley-Terry 模型

假设每个回答有一个潜在分数 $r(x,y)$，人类偏好概率由分数差决定：

$$P(y_w\succ y_l\mid x) = \sigma\left(r(x,y_w) - r(x,y_l)\right)$$

其中 $\sigma$ 是 sigmoid 函数。

### 4.2 损失函数推导

对偏好数据做最大似然：

$$\mathcal{L}_{RM}(\phi) = -\mathbb{E}_{(x,y_w,y_l)\sim\mathcal{D}}\left[\log\sigma\left(r_\phi(x,y_w) - r_\phi(x,y_l)\right)\right]$$

推导：

```text
似然：P(y_w ≻ y_l) = σ(r_w - r_l)
对数似然：log σ(r_w - r_l)
最大化对数似然 = 最小化负对数似然
→ 得到上面的损失
```

### 4.3 奖励模型的结构

```text
主流做法：
  在 SFT 模型上加一个标量头（linear head）
  输入 (x, y)，输出一个实数分数
  训练时冻结大部分参数或全量微调

变体：
  Pairwise RM：直接输入两个回答，输出偏好概率
  Generative RM：让模型生成"好/坏"判断（可解释性更强）
  Process RM（PRM）：对推理的每一步打分，而非整段
```

### 4.4 奖励模型的评估

```text
① 偏好准确率：在留出集上判断 chosen > rejected 的比例
② 校准性：分数是否与人类评分一致
③ 对抗鲁棒性：面对分布外输入是否稳定
④ 与下游指标的相关性：RM 分数提升是否带来真实质量提升
```

> **警告**：奖励模型的准确率不是越高越好。过拟合标注数据的 RM 会放大标注偏差，让 PPO 学到"标注者的怪癖"而非真实质量。

---

## 5. PPO 对齐阶段

### 5.1 目标函数

$$\max_\theta\;\mathbb{E}_{x\sim\mathcal{D},\,y\sim\pi_\theta(\cdot\mid x)}\left[r_\phi(x,y) - \beta\,D_{KL}\left(\pi_\theta(\cdot\mid x)\,\|\,\pi_{ref}(\cdot\mid x)\right)\right]$$

三项分别对应：

```text
r_φ(x,y)：奖励模型给回答打分（要最大化）
D_KL：与参考模型的差异（要最小化）
β：权衡系数
```

### 5.2 逐 token 的 KL

工程上 KL 按 token 计算并累加：

$$D_{KL}\left(\pi_\theta\,\|\,\pi_{ref}\right) \approx \sum_{t}\left(\log\pi_\theta(y_t\mid x,y_{<t}) - \log\pi_{ref}(y_t\mid x,y_{<t})\right)$$

实际实现常用 $k_3$ 估计（无偏、非负）：

$$D_{KL}^{(3)} = \frac{\pi_{ref}}{\pi_\theta} - \log\frac{\pi_{ref}}{\pi_\theta} - 1$$

### 5.3 奖励塑形

```text
每个 token 的奖励：
  r_t = -β · KL_t（中间步）
  r_T = r_φ(x,y) - β · KL_T（终止步）

这样优势估计能感知"每一步偏离参考模型多少"
避免模型在句末突然跑偏
```

### 5.4 训练稳定性

```text
① 奖励归一化
   同一 batch 内的奖励做标准化，稳定优势尺度

② 长度惩罚
   防止模型为了高分生成超长回答

③ 定期人工评估
   RM 分数上涨不等于真实质量上涨

④ 早停
   监控 KL 与 RM 分数，KL 爆炸时停止
```

---

## 6. KL 约束的作用

KL 约束是 RLHF 的"安全带"，它有四个作用：

```text
① 防止奖励黑客
   模型不能为了刷分跑到 RM 没见过的分布

② 保留基础能力
   防止对齐训练破坏预训练学到的知识

③ 稳定训练
   限制策略变化幅度，避免崩塌

④ 保持多样性
   防止模型收敛到单一"安全回答"模式
```

β 的权衡：

| β | 效果 | 风险 |
|---|------|------|
| 过大 | 几乎不偏离 SFT，对齐效果弱 | 训练没有意义 |
| 过小 | 大幅优化 RM 分数 | 奖励黑客、能力退化 |
| 常用 | 0.01-0.1（按实现而定） | 需要实验确定 |

> **提示**：β 不是固定值。可以在训练中自适应：KL 超过目标就增大 β，低于目标就减小 β。PPO 的 KL 惩罚版本就是这么做的。

---

## 7. 奖励黑客与缓解

### 7.1 典型表现

```text
长度偏好：
  RM 偏好长回答 → 模型学会啰嗦
  缓解：长度归一化、长度惩罚、RM 训练时控制长度分布

谄媚（sycophancy）：
  模型学会附和用户观点，而非给出正确答案
  缓解：偏好数据中加入"纠正用户错误"的样本

格式刷分：
  模型学会用漂亮的格式掩盖空洞内容
  缓解：格式与内容分开评估

测试集泄漏：
  代码任务中直接输出测试期望值
  缓解：隐藏测试、多套测试、人工审核
```

### 7.2 缓解手段

| 手段 | 原理 |
|------|------|
| KL 约束 | 限制分布偏移 |
| 奖励集成 | 多个 RM 取平均，降低单模型偏差 |
| 过程奖励 | 对推理步骤打分，而非只看结果 |
| 可验证奖励 | 用规则/单测替代 RM（RLVR） |
| 人工抽检 | 定期校准 RM 与真实质量 |
| 红队测试 | 主动找对抗样本 |

---

## 8. DPO：绕过 RL 的对齐

DPO（Direct Preference Optimization）把 RLHF 的"训练 RM + PPO"两步合并成一个分类损失。

### 8.1 推导

RLHF 的目标：

$$\max_\theta\;\mathbb{E}_{x,y\sim\pi_\theta}\left[r(x,y) - \beta\log\frac{\pi_\theta(y\mid x)}{\pi_{ref}(y\mid x)}\right]$$

这个目标的最优解有闭式形式：

$$\pi^*(y\mid x) = \frac{1}{Z(x)}\pi_{ref}(y\mid x)\exp\left(\frac{r(x,y)}{\beta}\right)$$

其中 $Z(x)$ 是配分函数。反解出奖励：

$$r(x,y) = \beta\log\frac{\pi^*(y\mid x)}{\pi_{ref}(y\mid x)} + \beta\log Z(x)$$

代入 Bradley-Terry 偏好模型：

$$
P(y_w\succ y_l\mid x) = \sigma\left(\beta\log\frac{\pi^*(y_w\mid x)}{\pi_{ref}(y_w\mid x)} - \beta\log\frac{\pi^*(y_l\mid x)}{\pi_{ref}(y_l\mid x)}\right)
$$

注意 $Z(x)$ 在相减时抵消了。于是得到 DPO 损失：

$$
\boxed{
\mathcal{L}_{DPO}(\theta) = -\mathbb{E}_{(x,y_w,y_l)}\left[\log\sigma\left(\beta\log\frac{\pi_\theta(y_w\mid x)}{\pi_{ref}(y_w\mid x)} - \beta\log\frac{\pi_\theta(y_l\mid x)}{\pi_{ref}(y_l\mid x)}\right)\right]
}
$$

### 8.2 与 RLHF 的对比

| 维度 | RLHF（PPO） | DPO |
|------|------------|-----|
| 是否需要 RM | 需要 | 不需要 |
| 是否需要采样 | 需要（on-policy） | 不需要（离线偏好数据） |
| 训练稳定性 | 中（超参敏感） | 高（监督学习式） |
| 显存 | 四个模型 | 两个模型（策略 + 参考） |
| 探索能力 | 有（可发现新回答） | 无（只学数据里的偏好） |
| 分布偏移 | 有 KL 约束 | 隐式约束（β） |
| 适合 | 大规模对齐、可验证奖励 | 中小规模、快速迭代 |

### 8.3 DPO 的局限

```text
① 离线：只在给定偏好数据上优化，不会探索新回答
② 分布偏移：训练数据与模型实际分布不一致时效果下降
③ 长度偏差：仍可能学到"长回答更好"
④ 无法利用可验证奖励：数学/代码任务不如 RLVR
⑤ β 敏感：控制偏离参考模型的程度
```

### 8.4 DPO 变体

| 变体 | 改进 |
|------|------|
| IPO | 用平方损失替代 log-sigmoid，缓解过拟合 |
| KTO | 只需"好/坏"标签，不需要成对数据 |
| ORPO | 把 SFT 与偏好优化合并成一步 |
| SimPO | 去掉参考模型，用长度归一化 |
| CPO | 加入 SFT 损失，稳定训练 |

---

## 9. Constitutional AI 与 RLAIF

### 9.1 Constitutional AI

Anthropic 提出的两阶段方法：

```text
阶段一：监督阶段（SL-CAI）
  模型生成回答 → 按"宪法原则"自我批评 → 修订
  用修订后的数据做 SFT

阶段二：RL 阶段（RL-CAI）
  模型生成多个回答
  AI 按宪法原则判断哪个更好（RLAIF）
  用 AI 偏好数据训练 RM，再做 RL
```

宪法是一组自然语言原则，例如"选择最不有害的回答""不要帮助违法活动"。

### 9.2 RLAIF

用 AI 反馈替代人类反馈：

```text
优势：
  成本低、可扩展、一致性好
缺点：
  继承 AI 的偏见
  可能"AI  judge AI"形成回音室
  对强模型的判断力依赖大
```

---

## 10. 工程挑战

```text
① 显存
   PPO 四模型同框：Actor、Critic、RM、Reference
   70B 模型需要多卡 + ZeRO/FSDP + 量化
   DPO 两模型：策略 + 参考，压力小一半

② 采样吞吐
   PPO 需要大量在线采样
   推理引擎（vLLM/SGLang）与训练框架的协同是关键

③ 奖励模型服务
   RM 前向要快，否则采样成为瓶颈
   常与训练同机部署或独立推理集群

④ 数据管道
   偏好数据清洗、去重、质量过滤
   训练中动态采样与课程学习

⑤ 评估
   RM 分数、人工评估、基准测试三条线
   防止"指标上涨、体验下降"
```

---

## 11. 评估体系

```text
三层评估：

① 自动指标
   RM 分数、困惑度、基准测试（MMLU/GSM8K/HumanEval）
   注意：RM 分数上涨可能只是奖励黑客

② AI 评估
   用强模型做裁判（LLM-as-a-Judge）
   需要控制位置偏差、长度偏差

③ 人工评估
   成对比较、 Likert 量表、领域专家评审
   成本高，但是最终标准
```

评估的常见陷阱：

```text
① 只测对齐，不测能力
   对齐后模型变"安全"但变笨

② 只测分布内
   换领域、换语言就退化

③ 只看平均分
   忽略长尾失败案例

④ 裁判与被评模型同源
   自我偏好偏差
```

---

## 12. 局限与边界

```text
① RLHF 对齐的是"标注者的偏好"，不是"客观正确"
   标注者偏见会被放大

② 无法创造新能力
   RLHF 主要调整行为分布，不增加知识

③ 奖励模型是瓶颈
   RM 的质量决定对齐上限

④ 多目标冲突
   有用、无害、诚实之间存在张力

⑤ 评估困难
   "好回答"难以形式化

⑥ 成本高
   数据标注 + 训练 + 评估都是重投入
```

---

## 13. 代码实现

### 13.1 奖励模型损失

```python
import torch
import torch.nn as nn


def reward_model_loss(chosen_rewards, rejected_rewards):
    return -nn.functional.logsigmoid(chosen_rewards - rejected_rewards).mean()
```

### 13.2 DPO 损失

```python
def dpo_loss(policy_chosen_logps, policy_rejected_logps,
             ref_chosen_logps, ref_rejected_logps, beta=0.1):
    chosen_ratio = policy_chosen_logps - ref_chosen_logps
    rejected_ratio = policy_rejected_logps - ref_rejected_logps
    logits = beta * (chosen_ratio - rejected_ratio)
    return -nn.functional.logsigmoid(logits).mean()
```

### 13.3 序列 log 概率

```python
def sequence_log_prob(model, input_ids, attention_mask, labels):
    outputs = model(input_ids=input_ids, attention_mask=attention_mask)
    logits = outputs.logits[:, :-1, :]
    targets = labels[:, 1:]
    log_probs = nn.functional.log_softmax(logits, dim=-1)
    token_log_probs = log_probs.gather(2, targets.unsqueeze(-1)).squeeze(-1)
    mask = (targets != -100).float()
    return (token_log_probs * mask).sum(dim=-1)
```

---

## 14. 面试问法

**问：RLHF 的完整流程是什么？**

详细版：三阶段。先 SFT 让模型学会指令跟随；再用同一 prompt 的多个回答做人工排序，用 Bradley-Terry 模型训练奖励模型；最后用 PPO 优化策略，目标是最大化奖励模型分数减去 KL 偏离惩罚。KL 约束防止奖励黑客与能力退化。

简短版：SFT → 训奖励模型 → PPO 对齐，中间用 KL 约束防止跑偏。

**问：奖励模型怎么训练？**

详细版：用成对偏好数据，假设偏好概率由分数差经 sigmoid 得到，做最大似然，损失是 $-\log\sigma(r_w-r_l)$。模型通常在 SFT 模型上加标量头，训练时可以用全参或 LoRA。

简短版：Bradley-Terry + 交叉熵，让好回答的分数高于坏回答。

**问：DPO 和 RLHF 的区别？**

详细版：RLHF 要训练 RM 再做 PPO，是在线 RL；DPO 利用 RLHF 目标的闭式最优解，把奖励表示成策略与参考模型的 log 比率，直接对偏好数据做分类，不需要 RM 和采样。DPO 更稳定、显存更小，但离线、无探索，复杂任务不如 RLVR。

简短版：RLHF 训 RM + PPO；DPO 把偏好直接变成分类损失，省掉 RM 和采样。

**问：KL 约束为什么必要？**

详细版：三个原因。防止模型为了刷 RM 分数跑到分布外（奖励黑客）；保留预训练与 SFT 的能力；稳定训练。β 控制约束强度，过大对齐无效，过小能力退化。

简短版：防奖励黑客、保能力、稳训练，β 调强度。

---

## 15. 小结

| 问题 | 答案 |
|------|------|
| RLHF 三阶段 | SFT → RM → PPO |
| 偏好数据形式 | (prompt, chosen, rejected) |
| RM 损失 | $-\log\sigma(r_w-r_l)$（Bradley-Terry） |
| PPO 目标 | 奖励 − β·KL |
| KL 作用 | 防跑偏、保能力、稳训练 |
| 奖励黑客 | 长度偏好、谄媚、格式刷分 |
| DPO | 闭式解转分类损失，无需 RM |
| DPO 局限 | 离线、无探索、长度偏差 |
| 评估三层 | 自动指标、AI 裁判、人工评估 |

**下一模块**：[08-GRPO与RLVR](08-GRPO与RLVR.md)　**返回总览**：[00-强化学习知识体系总览](00-强化学习知识体系总览.md)
