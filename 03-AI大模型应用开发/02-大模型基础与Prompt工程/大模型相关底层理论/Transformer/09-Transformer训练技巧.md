# 09 - Transformer 训练技巧

> 🎯 训练 Transformer 不是"把数据丢进去就行"。学习率 Warmup、混合精度、梯度累积、Label Smoothing——这些技巧是高效训练和不崩的保障

---

## 目录

1. [学习率调度](#1-学习率调度)
2. [优化器选择](#2-优化器选择)
3. [正则化技术](#3-正则化技术)
4. [混合精度训练](#4-混合精度训练)
5. [训练配置模板](#5-训练配置模板)

---

## 1. 学习率调度

### 1.1 Warmup + Cosine Decay

```python
# Transformer 训练的标准 LR 调度
from torch.optim.lr_scheduler import CosineAnnealingLR, LinearLR, SequentialLR

optimizer = torch.optim.AdamW(model.parameters(), lr=3e-4)

# Warmup: 前 N 步线性增长
warmup = LinearLR(optimizer, start_factor=0.01, total_iters=1000)

# 主阶段: Cosine 衰减
cosine = CosineAnnealingLR(optimizer, T_max=100000)

scheduler = SequentialLR(optimizer, schedulers=[warmup], milestones=[1000])
```

```text
为什么需要 Warmup？
  训练初期模型参数随机 → 梯度方向不准 → 大学习率容易炸
  Warmup = 从小学习率开始，逐步增大 → 先"找到方向"再"加速"

推荐的 Warmup 步数：
  小模型 (<1B)：500-2000 步
  大模型 (>7B)：2000-5000 步
```

### 1.2 学习率大小

| 模型规模 | 推荐 LR | 说明 |
|------|:---:|------|
| <100M | 1e-3 ~ 5e-4 | 小模型可以大步 |
| 100M-1B | 3e-4 ~ 1e-4 | 标准范围 |
| 1B-10B | 1e-4 ~ 5e-5 | 大模型要保守 |
| >10B | 3e-5 ~ 1e-5 | 超大规模需极小 LR |

## 2. 优化器选择

```text
AdamW = Adam + 解耦的 Weight Decay
  → Transformer 训练的标配

关键参数：
  lr=3e-4           # 学习率
  betas=(0.9, 0.95) # 动量（第二个 beta 比通常的 0.999 小）
  weight_decay=0.1   # 正则化
  eps=1e-8

为什么 betas[1]=0.95 而不是 0.999？
  → 大模型训练时间短（数据量固定，epoch 少）
  → 0.999 太慢，0.95 能更快适应最新梯度
```

## 3. 正则化技术

### 3.1 Dropout

```text
Dropout = 随机丢弃神经元（训练时）
  → 防止过拟合
  → 标准值：0.1

Attention Dropout: 随机丢弃 attention weight
Residual Dropout: 丢弃残差连接

现代 LLM 的趋势：
  → 数据量非常大 + 只训 1-2 epoch → 几乎不过拟合
  → Dropout 值越来越小（0.0-0.05）
  → Llama 3 不用 Dropout！
```

### 3.2 Label Smoothing

```text
Label Smoothing：不让模型"太自信"

标准 CE Loss：目标 [0, 0, 1, 0, 0]（One-Hot）
Label Smoothing：目标 [0.01, 0.01, 0.96, 0.01, 0.01]

ε = 0.1（smoothing 系数）
好处：
  → 防止过拟合
  → 提高泛化能力
  → 模型输出更 calibrated（自信度和准确率更匹配）
```

## 4. 混合精度训练

```python
# 混合精度训练 — 大部分用 FP16，关键部分用 FP32
scaler = torch.cuda.amp.GradScaler()

for batch in dataloader:
    optimizer.zero_grad()

    with torch.cuda.amp.autocast():     # 自动混合精度
        loss = model(batch)

    scaler.scale(loss).backward()       # 梯度缩放（防止 FP16 下溢）
    scaler.step(optimizer)
    scaler.update()
```

```text
混合精度 = FP16 + FP32

为什么混合？
  FP16 的优势：
    ✅ 显存减半
    ✅ 速度提升 2-3x（Tensor Core）
  
  FP16 的问题：
    ❌ 精度不足 → 梯度下溢（太小变成 0）
    ❌ 需要 Loss Scaling 补偿

解决方案：大部分计算用 FP16，权重+优化器用 FP32
  → BF16 更好（范围同 FP32），A100+ 推荐
```

## 5. 训练配置模板

```python
# LLM 训练的标准配置模板
training_config = {
    # 模型
    "model": "llama-8b",

    # 优化器
    "optimizer": "AdamW",
    "lr": 3e-4,
    "betas": (0.9, 0.95),
    "weight_decay": 0.1,
    "eps": 1e-8,

    # 学习率调度
    "warmup_steps": 2000,
    "lr_schedule": "cosine",

    # 训练
    "max_steps": 100000,
    "batch_size": 128,               # 全局 batch（含梯度累积）
    "micro_batch_size": 4,           # 单 GPU batch
    "gradient_accumulation_steps": 8, # 累积 8 步 = 4×8×GPU数

    # 精度
    "dtype": "bfloat16",             # A100+ / H100
    "mixed_precision": True,

    # 正则化
    "dropout": 0.0,                  # 现代 LLM 不用
    "label_smoothing": 0.0,

    # 序列
    "max_seq_len": 4096,
}
```

## 核心要点回顾

- AdamW = Transformer 标配优化器，betas=(0.9, 0.95)
- Warmup + Cosine Decay = 训练稳定性的保障
- 混合精度 (FP16/BF16) = 显存减半 + 2-3x 加速
- 现代 LLM 不用或极少用 Dropout（数据量大，不过拟合）
- 梯度累积 = 小 GPU 跑大 batch（4×8步 = batch 32）

## 参考资料

1. AdamW 论文 (Loshchilov & Hutter, 2019)
2. Mixed Precision Training (Micikevicius et al., 2018)
