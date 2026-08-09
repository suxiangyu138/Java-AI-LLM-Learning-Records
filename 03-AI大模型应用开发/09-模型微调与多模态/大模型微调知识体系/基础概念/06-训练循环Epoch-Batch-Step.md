# 训练循环：Epoch-Batch-Step

> 训练日志里最常见也最常被误解的三个单位：epoch、batch、step 是什么关系、公式怎么算、微调该训多少——读懂训练循环就读懂了一半训练日志

## 1. 三个概念与公式

训练循环由三个嵌套单位组成：

**Step（步）**：一次"前向 → 反向 → 优化器更新"的完整迭代，也叫 iteration。训练日志里最常见的时间刻度。

**Batch（批次）**：一个 step 里同时送入模型的样本组。batch size = 每步处理的样本数。batch 越大，梯度估计越稳，但显存占用越高。

**Epoch（轮）**：**完整过一遍训练集**。epoch 数 × 数据集大小 = 模型把每条数据"见"了几次——这是衡量训练量最直观的单位。

三者关系公式：

```text
每 epoch 步数 = ceil(样本数 / batch_size / gradient_accumulation_steps)
总步数 = 样本数 × epochs / batch_size / gradient_accumulation_steps
```

举例：10,000 条数据、batch 4、梯度累积 4、训 2 epochs → 每步实际吃 16 条 → 总步数 = 10000 × 2 / 4 / 4 = 1250 步。

## 2. 显存约束下的经典组合

**batch size**：单卡常见 4-8（`per_device_train_batch_size=4` 是大量微调脚本的默认）。batch 64 在单卡上几乎必然 OOM——因为显存主要被权重、梯度、优化器状态和激活值占据，而不是被 batch 撑满。

**梯度累积**（gradient accumulation）：显存装不下大 batch 时的标准解法——每吃一个 micro-batch 算一次梯度但不更新权重，**累计 gradient_accumulation_steps 个 micro-batch 后统一更新一次**，等效于放大了 batch size，得到更稳定的梯度估计。注意实现细节：loss 要除以累积步数再反传，否则等效学习率被放大。典型值 4，单卡微调几乎必配。

**有效 batch size** = per_device_batch × 卡数 × 梯度累积。2026 研究（ICLR 2026）对 batch 调度的新发现：简单任务 batch 可全程递增，困难任务先用小 batch、后期再切大 batch（"late switching"）——利用从小切大后 loss 快速追平的"catch-up 效应"省数据。企业微调落地仍以固定 batch + 监控为主。

## 3. 微调该训几个 epoch：1-3 惯例

SFT 的 epoch 数有非常稳定的经验共识：**1-3 个 epoch**。

- 数据量大（10k+）且质量高 → 1 epoch 足够（每条约见 1 次）。
- 数据量中等（1k-10k）→ 2-3 epoch。
- 数据量小（<1k）→ 也**不建议超过 3**：小数据 + 多 epoch = 把样本背下来，典型过拟合（见 09 篇）。

为什么不能多训：**超过 3 epoch 收益递减且过拟合风险陡增**——模型开始记忆训练样本（能背出原样回答）而不是学习泛化模式，验证 loss 与训练 loss 分道扬镳。

机制上，模型参数量远超样本信息量（7B 模型学 1 万条样本）时，权重空间足够"记住"每条样本的输入输出映射——多过几轮，训练 loss 可以降到接近 0，但学到的是"查表"而非"规律"。

1-3 epoch 的窗口意义在于：让"可泛化的规律"先于"逐条的记忆"被学到——越往后训练，记忆占比越高。Baseten 2026 研究（覆盖至 235B 模型）专门测量了"训多少 epoch 开始侵蚀通用指令遵循能力"，结论仍是早期停止越早越好。判断标准不是"训满 3"，而是**盯验证 loss**：验证 loss 连续 N 个评估点不降反升，就是该停的信号。

## 4. 学习率调度与训练步数的配合

调度器（scheduler）按 step 数运作，所以 warmup 步数与总步数强相关：

- warmup：总步数的 1-2%，或固定 100-200 步（6000 步的训练常用 200 步预热）。极短训练（几百步）可以只留 2-3 步 warmup。
- 调度形态：微调默认 **cosine**（平滑衰减）；短跑用 linear；极短（<200 步）用 constant_with_warmup。
- 梯度裁剪：max norm 1.0，配合 warmup 组成"保险丝"（见 05 篇）。

一个常见误解：很多人把总步数算错或忽略 warmup 步数占比——**总步数越短，warmup 占比越重要**，500 步的训练里 warmup 100 步和 200 步效果差异显著。

## 5. 训练日志的正确读法

一段典型训练日志长这样（概念示意）：

```text
step 200/1250 | train loss 1.32 | eval loss 1.41 | lr 2.0e-4 | grad norm 0.87
step 400/1250 | train loss 1.05 | eval loss 1.28 | lr 1.9e-4 | grad norm 0.92
```

要看的四件事：**train loss**（降得平不平滑）；**eval loss**（与 train 的差是否在拉大——拉大 = 过拟合前兆）；**lr**（调度是否生效）；**grad norm**（异常飙升 = 该查数据/加裁剪）。

评估纪律：**不要等训练结束才评**——第一个 checkpoint 后（训练 10-20% 处）就要开始看 eval loss；eval_steps 设 100-500 步；训练器配置 `load_best_model_at_end=True` + `metric_for_best_model="eval_loss"` 自动保存最优 checkpoint。

配套的数据加载细节：**shuffle（打乱）** 默认开启，保证每个 epoch 的 batch 组合不同（否则模型按批次顺序产生伪规律）；随机种子固定保证可复现；数据集版本（清洗/增补）要记录到训练日志——**"能复现"是训练工程的第一纪律**，任何一次实验都要能回答"用了哪版数据、哪版代码、哪些超参"（checkpoint 与恢复细节见本章第 7 节）。

## 6. packing 与 padding：序列怎么装进 batch

batch 里的样本长度不一，直接拼装会有两个问题：**padding**（短样本补到最长样本的长度，空位用 pad token 填）浪费显存与算力——如果样本长度参差严重（如 100 token 到 4000 token 混装），padding 浪费可达 50% 以上；**truncation**（截断到 max_seq_length）丢弃长样本信息。

**packing（打包）** 是标准解法：把多条样本按 token 数拼接成等长序列，拼接处用 eos token 分隔、position_ids 正确重置（不同样本不能共享同一位置编码）。packing 把 padding 浪费降到接近零，训练吞吐可提升 1.5-3 倍——TRL 的 `ConstantLengthDataset` 等工具开箱即用，长文本场景强烈建议开启。

选择要点：短文本任务（平均几百 token）padding 浪费可控，packing 收益一般；长文本或长度分布极不均匀的任务（客服日志、长文档），packing 是吞吐优化第一优先项。

## 7. checkpoint 与训练恢复

长训练必须做好 checkpoint 管理，三件事：

**保存频率**：`save_steps` 通常 200-1000 步一次（或按 epoch），太疏会错过最优窗口，太密浪费磁盘（7B LoRA 每个 checkpoint 也要 GB 级）。保留策略 `save_total_limit` 控制在 3 个左右，配 `load_best_model_at_end` 自动保留最优。

**断点续训**：训练中途断电/超时/节点故障是常态，`resume_from_checkpoint` 从最近 checkpoint 恢复，配合 warmup 计数（restart 后不要重新预热或只留极短预热）——注意恢复时 scheduler 要从保存的 step 继续，而不是从头算。

**训练一致性**：分布式训练（多卡）时随机种子要固定（seed），否则断点恢复后数据顺序漂移、复现性差；每次实验记录完整配置（模型版本、数据版本、超参、日志路径），这是评估与排障（09 篇）的前提。

> 🎯 **核心要点**：step = 一次参数更新，batch = 每步样本数，epoch = 完整过一遍数据；总步数 = 样本 × epoch / batch / 累积；单卡 batch 4-8 + 梯度累积 4 是标配；SFT 训 1-3 epoch、看验证 loss 决定停点；warmup 占 1-2% 步数、cosine 调度默认——"epoch 越多越好"是入门第一大误区。

---

**参考来源**：

- [Epochs: 1-3 typical for SFT（The Neural Base）](https://theneuralbase.com/fine-tuning-fundamentals/learn/beginner/epochs-1-3-typical-for-sft/)
- [Fast Catch-up, Late Switching: Optimal Batch Size Scheduling（ICLR 2026）](https://mlanthology.org/iclr/2026/wang2026iclr-fast/)
- [Post-Training Science for Supervised Fine-Tuning（Baseten, 2026-06）](https://www.baseten.co/research/post-training-science-for-supervised-fine-tuning/)
- [LoRA Fine-tuning Hyperparameters Guide（Unsloth）](https://unsloth.ai/docs/get-started/fine-tuning-llms-guide/lora-hyperparameters-guide)
- [Evaluation during training: loss curve interpretation（The Neural Base）](https://theneuralbase.com/fine-tuning-fundamentals/learn/beginner/evaluation-during-training-loss-curve-interpretation/)

---

**下一模块**：[07-微调方法全景图谱.md](./07-微调方法全景图谱.md) / **返回总览**：[00-基础概念知识体系总览](./00-基础概念知识体系总览.md)
