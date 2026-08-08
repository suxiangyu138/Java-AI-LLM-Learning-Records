# 04 - 生成对抗网络 GAN

> 🎯 GAN = 生成器 + 判别器的博弈 — 这是深度学习在"创造"领域最惊艳的突破。理解 GAN 的训练博弈才能理解 Stable Diffusion 的前身

---

## 目录

1. [GAN 的核心思想](#1-gan-的核心思想)
2. [训练博弈](#2-训练博弈)
3. [经典变体](#3-经典变体)
4. [GAN 的挑战](#4-gan-的挑战)
5. [GAN 与扩散模型](#5-gan-与扩散模型)
6. [生成模型家族全景](#6-生成模型家族全景)

---

## 1. GAN 的核心思想

```text
GAN (Generative Adversarial Network) = 两个网络互相博弈

  生成器 G (Generator)：从随机噪声生成"假"数据
    → 目标：骗过判别器

  判别器 D (Discriminator)：判断数据是"真"还是"假"
    → 目标：识破生成器

  类比：伪造者 vs 警察
    伪造者（G）想造出以假乱真的假币
    警察（D）想区分真假币
    → 两者不断提升 → 最终假币以假乱真
```

---

## 2. 训练博弈

### 2.1 训练过程

```text
GAN 的训练是交替进行的：

  Step 1：训练判别器 D
    输入：真样本（标签=1）+ 假样本（标签=0，来自 G）
    目标：真样本判 1，假样本判 0
    → D 学会区分真假

  Step 2：训练生成器 G
    生成假样本 → 给 D 判断
    G 的目标：让 D 把假样本判为 1（真）
    → G 学会生成更逼真的样本

  Step 3：重复 → G 越来越强，D 越来越难区分
```

```python
# GAN 训练的核心伪代码
for epoch in range(epochs):
    # 训练判别器
    real_data = get_real_batch()
    fake_data = G(torch.randn(batch_size, latent_dim))
    
    D_loss = -(torch.log(D(real_data)) + torch.log(1 - D(fake_data))).mean()
    D_loss.backward()
    
    # 训练生成器
    fake_data = G(torch.randn(batch_size, latent_dim))
    G_loss = -torch.log(D(fake_data)).mean()
    G_loss.backward()
```

### 2.2 训练难点

```text
GAN 训练的不稳定性（著名难题）：

  ① 模式崩塌 (Mode Collapse)：
     G 只学会生成少数几种模式 → 输出重复
     → 如只生成"正面人脸"，不会其他角度

  ② 不收敛：
     G 和 D 的博弈不保证收敛到均衡点
     → Loss 震荡，可能越训越差

  ③ 梯度消失：
     D 太强 → G 的梯度趋零 → G 停止学习
```

---

## 3. 经典变体

| 变体 | 核心改进 | 用途 |
|------|----------|------|
| **DCGAN** | 用 CNN 替代全连接 + BatchNorm | 图像生成 baseline |
| **CGAN** | 加条件信息（标签/文本） | 可控生成 |
| **CycleGAN** | 无配对数据的风格迁移 | 照片→梵高风格 |
| **StyleGAN** | 逐层控制生成风格 | 超逼真人脸生成 |
| **Pix2Pix** | 有配对数据的图像翻译 | 草图→照片 |

---

## 4. GAN 的挑战

```text
GAN 的三大局限：

  ① 训练不稳定 — 需要大量调参技巧
  ② 模式崩塌 — 生成多样性不足
  ③ 难以评估 — 没有公认的生成质量指标

GAN 时代（2014-2020）的调参技巧：
  → 用 Wasserstein Loss (WGAN)
  → 梯度惩罚 (Gradient Penalty)
  → Spectral Normalization
  → 两时间尺度更新规则 (TTUR)
```

---

## 5. GAN 与扩散模型

```text
GAN → Diffusion：生成模型的范式转换

  GAN (2014-2020)：
    优势：生成速度快（一次前向）
    劣势：训练不稳定、模式崩塌

  扩散模型 (2020+)：
    优势：生成质量极高、训练稳定
    劣势：生成慢（多步去噪）

  Stable Diffusion / DALL-E 都是扩散模型 → GAN 在生成领域退居二线

  但 GAN 的思想（对抗博弈）影响了：
    → GAN 的判别器 → RLHF 的 Reward Model（都是"评判"角色）
    → 对抗训练 → 提升模型鲁棒性
```

---

## 6. 生成模型家族全景

```text
生成模型四大家族（理解 GAN 的位置）：

① 自回归模型（Autoregressive）：GPT 系列
   逐 token 生成，条件概率乘积
   优势：LLM 主流、质量高；劣势：逐 token 慢

② 扩散模型（Diffusion）：Stable Diffusion / DALL-E / Sora
   加噪-去噪学习，多步采样
   优势：图像质量 SOTA、训练稳定；劣势：采样慢（蒸馏加速中）

③ 变分自编码器（VAE）
   编码-解码 + 隐空间先验
   优势：快；劣势：图像模糊

④ GAN
   对抗博弈
   优势：快、锐利；劣势：训练不稳定、模式崩塌
```

| 维度 | GAN | 扩散模型 | 自回归（GPT） |
|------|:---:|:-------:|:------------:|
| 生成速度 | 快（单次前向） | 慢（多步采样） | 慢（逐 token） |
| 训练稳定性 | 差（对抗难收敛） | 稳定 | 稳定 |
| 模式覆盖 | 差（易崩塌） | 好 | 好 |
| 质量上限 | 中 | **SOTA（图像）** | **SOTA（文本）** |
| 适用领域 | 旧应用/实时 | 图像/视频 | 文本/多模态 |

**面试观点题**："GAN 过时了吗？"

```text
标准回答框架：
① 承认事实：图像/视频生成主流已转向扩散（SD/DALL-E/Sora）
② GAN 未死：实时生成、图像翻译（CycleGAN）、超分等场景仍有优势
③ 思想遗产：对抗训练（判别器 → RLHF 的 Reward Model）、鲁棒性研究
④ 趋势判断：扩散蒸馏（一步生成）正在蚕食 GAN 的"快"优势
```

---

## 7. GAN 训练稳定性技巧

```text
GAN 训练不稳定的三大根源与对策：

① 模式崩塌（Mode Collapse）
   生成器只学会生成少数模式（骗过 D 的"捷径"）
   → 对策：Mini-batch discrimination、Unrolled GAN、增加多样性惩罚

② 训练不平衡（G/D 速度不一）
   D 太强 → 梯度消失；G 太强 → 梯度爆炸
   → 对策：TTUR（两网络不同学习率）、标签平滑、频谱归一化

③ 收敛不稳定（loss 震荡）
   对抗目标本身是 min-max 博弈，无固定收敛点
   → 对策：WGAN 的 Wasserstein 距离替代 JS 散度（本质改良）

工程经验（2020 前 GAN 时代的共识）：
   → D 每步更新 5 次、G 每步 1 次（不平衡比例）
   → 使用 Adam 而非 SGD；不用 BatchNorm 用 LayerNorm
   → 从简单任务开始，逐步增加难度（课程学习）
```

> 💡 为什么值得了解：**GAN 的"对抗训练不稳定"经验 → 扩散模型与 RLHF 都吸收了"稳定训练"的设计教训**——理解 min-max 博弈的困难，才能理解对齐训练为什么需要 KL 惩罚与裁剪。

---

## 核心要点回顾

- GAN = G（生成假样本）+ D（判别真假）→ 对抗博弈 → 以假乱真
- 训练交替进行：训 D 区分真假 → 训 G 骗过 D → 重复
- 模式崩塌和训练不稳定是 GAN 的主要挑战
- 扩散模型（Stable Diffusion）已取代 GAN 成为生成模型主流
- GAN 的对抗思想 → 影响了 RLHF 的 Reward Model 设计
