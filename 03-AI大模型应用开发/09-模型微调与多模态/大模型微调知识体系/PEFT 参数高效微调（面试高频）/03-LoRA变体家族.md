# LoRA 变体家族

> 2026 变体全景：DoRA（幅度/方向分解，接近全量）、rsLoRA（α/√r 理论稳定）、AdaLoRA/PiSSA/LoRA+ 各解决什么问题——以及 2026 最重要的提醒：变体收益可能来自超参敏感而非方法本身

## 1. 变体地图：每个变体解决一个 LoRA 的短板

LoRA 的四个短板与对应的变体：

| LoRA 短板 | 变体 | 思路 |
|----------|------|------|
| 表达能力受低秩约束 | DoRA | 幅度/方向分解，方向部分用 LoRA |
| 大秩时激活/梯度坍缩 | rsLoRA | 缩放从 α/r 改为 α/√r |
| 秩均匀分配不合理 | AdaLoRA | 全局参数预算下自适应分配秩 |
| 收敛慢/初始化差 | PiSSA / LoRA+ | 主成分初始化 / A、B 非对称学习率 |

**变体的共性**：都不改变"低秩分解"的根基，只修改一个环节（分解形式/缩放/分配/初始化）——**面试答变体先答"解决 LoRA 的什么问题"**，这是变体知识的正确组织方式。

**变体生态的另一个观察**：变体数量很多（DyLoRA/GoRA/ElaLoRA/L1RA/CTR-LoRA……）但**生产使用的极少**——2026 年实际生产中 DoRA 与 rsLoRA 是主流（框架一行配置、效果验证充分），其余多停留在研究阶段——**选变体的第一原则：框架支持 + 验证充分**（研究论文里"效果好"≠生产可用，`../训练关键超参 & 显存优化技术/01-超参全景与调参方法论.md` 的"抄配置要复验"纪律同样适用于抄方法）。

## 2. DoRA：2026 事实默认的"近全量"变体

**DoRA（Weight-Decomposed LoRA）**：把每个权重分解为**幅度（magnitude）与方向（direction）**两个分量——LoRA 只作用于方向分量（m·(W₀+BA)/‖W₀+BA‖），幅度单独训练。

**为什么有效**：全量微调的更新里，幅度与方向的变化规律不同——单独建模幅度让表达空间更接近全量；实测**缩小了与全量微调的剩余差距**（低秩下稳定带来 1-4% 收益），显存与 LoRA 基本持平、计算略增。

**2026 组合默认**：**QLoRA + DoRA**（4bit 底座 + DoRA 适配器）——Axolotl/主流框架一行配置（use_dora=True）；**重要警告**（2026 审计论文"Learning Rate Matters"）：DoRA 在简单任务（如 GSM8K）上可能**退化**——幅度/方向分解在低复杂度场景引入优化不稳定——**DoRA 不是无脑升级，复杂任务收益、简单任务谨慎**。

## 3. rsLoRA：缩放的理论修正

**rsLoRA（Rank-Stabilized LoRA）**：经典 α/r 缩放是启发式的——r 增大时，前向激活与反向梯度随 r 增长而**坍缩/爆炸**（导致大秩不如预期）；理论推导的正确缩放是 **α/√r**——保持前向/反向量级 O(1)，**大秩（32-64）稳定生效，收益可递增**。

**实践含义**：复杂任务用大秩（r=32-64）时优先考虑 rsLoRA（或按其缩放原则调 alpha）；小秩（8-16）时 α/r 与 α/√r 差异小，经典配置即可——**rsLoRA 是"大秩场景"的稳定性补丁**（`../训练关键超参 & 显存优化技术/02-学习率与调度.md` 的缩放讨论在此落地）。

**rsLoRA 与 alpha==r 的关系**：2026 保守共识"alpha==r"与 rsLoRA 的"α/√r"在**小秩时几乎重合**（r=16 时 √r=4，α/r=1 vs α/√r=4——缩放值不同但量级相近，配合 lr 调节等效）；**大秩时分歧明显**（r=64：α/r=1 vs α/√r=8——经典配置的缩放被压得太小，这就是大秩"不生效"的机制）——**小秩用 alpha==r、大秩按 rsLoRA 原则**是两者统一的实操结论。

## 4. 自适应秩与初始化：AdaLoRA / PiSSA / LoRA+

**AdaLoRA**：把"每层/每矩阵用多大秩"从手调变成**自适应**——在全局参数预算下按重要性分配秩（重要矩阵多分、次要矩阵少分）——解决"均匀分配秩忽略层间差异"的问题；DyLoRA（训练一次任意切片秩）、GoRA（梯度敏感度预算）、ElaLoRA（剪枝-生长动态秩）是同类方向。

**PiSSA**：**初始化感知**——用权重矩阵的主成分（principal components）初始化 A、B（而不是随机初始化）——加速收敛；注意：主成分提取的是底座原始结构，语义上更接近"从底座出发的微调"。

**LoRA+**：**非对称学习率**——A、B 的更新动力学天然不平衡（A 的学习率应大于 B），用两档 lr 修正——思路简单、零额外成本，框架里一行配置。

**2026 主线提醒**：自适应秩（RaLoRA 类，ICLR 2026）用**熵估计逐层梯度内在维度（GID）**并通过对齐有效秩逼近全量——**"秩 vs GID 对齐"是 2026 LoRA 研究的主线**（02 篇第 4 节），AdaLoRA 是这条线的早期代表。

**RaLoRA 的机制亮点**：块对角分解把有效秩从 r 扩展到 n_l × r（**不增加参数**——r(d_in + d_out) 保持不变）——"参数不增、容量翻倍"的数学技巧；RaLoRA-Pro 再加层间预算重分配（按 loss 敏感度）——在 GLUE/GSM8K/HumanEval/MT-Bench 上持续逼近或打平全量——**2026 年"LoRA 的容量上限"正在被自适应秩方法突破**：面试被问"LoRA 的局限"时，补一句"2026 年自适应秩方法正在缩小这一差距"是时效性加分。

## 5. 2026 最重要的提醒：超参敏感审计

**"Learning Rate Matters: Vanilla LoRA May Suffice"（2026）**对 LoRA 变体做了系统审计：**固定超参下重评估四种变体（含 DoRA/PiSSA 类），发现报告中的收益很多来自超参敏感性而非方法本身**——在低内在维度任务（如 GSM8K）上，vanilla LoRA 的线性子空间已经足够，变体的"收益"其实是"恰好调到了更好的超参"。

**工程含义**：**选变体之前先确认基线调好**——同一份数据、同样的调参预算下，vanilla LoRA（r/alpha/lr 调好）可能不比 DoRA 差；变体对比实验要**同超参预算、多 seed 复现**（避免把超参差异当方法差异，`../训练关键超参 & 显存优化技术/01-超参全景与调参方法论.md` 03 节的搜索方法在此复用）——**先调好 vanilla，再谈变体**是 2026 的调参纪律。

**审计的积极面**：审计论文同时证明**vanilla LoRA 在低内在维度任务上已经足够**（线性子空间即可拟合）——这不是"变体没用"，是"vanilla 被低估"——**多数业务任务（格式/风格/行为校准）落在低内在维度区间**，vanilla LoRA 的 r=16 配置就是最优解（`../PEFT 参数高效微调（面试高频）/09-PEFT选型与边界.md` 的选型地图由此简化：先 vanilla，不达再变体）。

## 6. 变体选型速查

| 场景 | 选择 |
|------|------|
| 默认起步 | vanilla LoRA（r=16、alpha==r、lr 调好） |
| 复杂任务要接近全量 | QLoRA + DoRA（谨慎：简单任务可能退化） |
| 大秩（32-64）稳定 | rsLoRA（或按 α/√r 调 alpha） |
| 层间重要性差异大 | AdaLoRA（自适应秩） |
| 收敛慢 | PiSSA（主成分初始化）/ LoRA+（非对称 lr） |
| 不确定 | 先调好 vanilla 再对比（2026 审计结论） |

> 🎯 **核心要点**：变体 = 每个解决 LoRA 一个短板——DoRA（表达能力，幅度/方向分解，QLoRA+DoRA 是 2026 默认但简单任务谨慎）、rsLoRA（大秩稳定，α/√r 理论）、AdaLoRA（自适应秩）、PiSSA（主成分初始化）、LoRA+（非对称 lr）；2026 审计结论——变体收益可能来自超参敏感，先调好 vanilla 再谈变体；自适应秩 + GID 对齐是 2026 研究主线（RaLoRA 类）；选型速查按场景走。

---

**参考来源**：

- [CTR-LoRA 引用的 LoRA 变体全景（IEEE 2026）](https://ieeexplore.ieee.org/document/11463994/references)
- [Learning Rate Matters: Vanilla LoRA May Suffice for LLM Fine-tuning（2026）](https://scite.ai/reports/learning-rate-matters-vanilla-lora-G3WVvPNx)
- [Low-Rank Adapter Fine-Tuning（Emergent Mind）](https://www.emergentmind.com/topics/low-rank-adapter-fine-tuning)
- [Gradient Intrinsic Dimensionality Alignment（ICLR 2026）](https://en.papernotes.org/ICLR2026/model_compression/gradient_intrinsic_dimensionalityalignmentnarrowing_the_gap_between_low-rank_ad/)
- [How Does LoRA Fine-Tuning Work? Adapters, QLoRA, DoRA（Mixpeek）](https://mixpeek.com/guides/fine-tuning-with-lora-adapters)

---

**下一模块**：[04-LoRA配置工程.md](./04-LoRA配置工程.md) / **返回总览**：[00-PEFT参数高效微调总览](./00-PEFT参数高效微调总览.md)
