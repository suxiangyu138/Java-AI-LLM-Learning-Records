# 07 - PyTorch 基础：张量与自动微分

> 🎯 PyTorch 是 LLM 时代的"普通话" — 微调、推理、实验全用它。张量是数据容器，Autograd 是自动求导引擎，GPU 是加速器

---

## 目录

1. [PyTorch 生态定位](#1-pytorch-生态定位)
2. [张量 Tensor 全解](#2-张量-tensor-全解)
3. [自动微分 Autograd](#3-自动微分-autograd)
4. [GPU 加速与设备管理](#4-gpu-加速与设备管理)
5. [PyTorch vs TensorFlow](#5-pytorch-vs-tensorflow)

---

## 1. PyTorch 生态定位

```text
PyTorch 在 LLM 开发中的角色：

  → HuggingFace Transformers：基于 PyTorch 的模型库
  → LoRA/QLoRA 微调：全部基于 PyTorch
  → vLLM 推理：基于 PyTorch
  → 所有主流开源模型：LLaMA/Qwen/DeepSeek → PyTorch 权重

一句话：LLM 开发 = PyTorch。
```

---

## 2. 张量 Tensor 全解

### 2.1 张量 = 多维数组

```python
import torch
import numpy as np

# 标量 (0维)
t0 = torch.tensor(3.14)

# 向量 (1维)
t1 = torch.tensor([1.0, 2.0, 3.0])

# 矩阵 (2维) — 权重矩阵
t2 = torch.randn(768, 1024)  # 正态分布随机初始化

# 3维 — Batch 数据 (batch, seq_len, dim)
t3 = torch.randn(32, 128, 768)  # 32 条, 128 tokens, 768 维

# 4维 — 图像 (batch, channel, height, width)
t4 = torch.randn(16, 3, 224, 224)
```

### 2.2 核心操作

```python
# === 创建 ===
torch.zeros(3, 4)        # 全 0
torch.ones(3, 4)         # 全 1
torch.randn(3, 4)        # 标准正态分布
torch.arange(0, 10, 2)   # [0, 2, 4, 6, 8]

# === 运算 ===
a + b                     # 加法
a @ b                     # 矩阵乘法（推荐！）
torch.matmul(a, b)        # 等价
a * b                     # 逐元素乘法

# === 形状操作 ===
x.view(2, -1)             # 变形（-1 = 自动推断）
x.reshape(2, -1)          # 变形（可能复制）
x.unsqueeze(0)            # 增加维度 → (1, 3, 4)
x.squeeze()               # 删除大小为1的维度

# === 索引 ===
x[0]                      # 第一行
x[:, 1:3]                 # 所有行的第2-3列
x[x > 0]                  # 布尔索引

# === NumPy 互转 ===
x_np = x.numpy()          # Tensor → NumPy
x = torch.from_numpy(x_np) # NumPy → Tensor
```

### 2.3 数据类型

| dtype | 位数 | 用途 |
|-------|:---:|------|
| `torch.float32` | 32 | 默认、权重存储 |
| `torch.float16` | 16 | 推理加速 |
| `torch.bfloat16` | 16 | **LLM 训练标配** |
| `torch.int8` | 8 | 量化推理 |
| `torch.long` | 64 | 索引、标签 |

```python
# 类型转换
x_fp32 = torch.randn(1000)
x_fp16 = x_fp32.half()      # → float16
x_bf16 = x_fp32.bfloat16()  # → bfloat16
```

---

## 3. 自动微分 Autograd

### 3.1 计算图

```python
# autograd 自动追踪运算 → 构建计算图 → backward() 自动求导

x = torch.tensor([2.0, 3.0], requires_grad=True)
y = x.pow(2).sum()        # y = 2² + 3² = 13

y.backward()               # 自动计算 ∂y/∂x
print(x.grad)              # [4.0, 6.0]  (∂y/∂x = 2x)
```

### 3.2 关键操作

```python
# 不需要梯度时 → 关闭以省显存
with torch.no_grad():
    predictions = model(inputs)  # 推理时不需要梯度

# 清零梯度
optimizer.zero_grad()       # 或 model.zero_grad()

# 分离计算图
y_detached = y.detach()     # 从计算图中断开

# 冻结参数（微调时常用）
for param in model.parameters():
    param.requires_grad = False
```

### 3.3 梯度累积

```python
# 模拟大 batch（显存不够时的技巧）
accumulation_steps = 4

for step, batch in enumerate(dataloader):
    loss = model(batch) / accumulation_steps  # 缩放 loss
    loss.backward()  # 梯度累积（不清零！）
    
    if (step + 1) % accumulation_steps == 0:
        optimizer.step()     # 累积够了才更新
        optimizer.zero_grad() # 清零
```

---

## 4. GPU 加速与设备管理

```python
# 检测 GPU
print(f"CUDA 可用: {torch.cuda.is_available()}")
print(f"GPU 数量: {torch.cuda.device_count()}")
print(f"GPU 名称: {torch.cuda.get_device_name(0)}")

# 设备管理
device = torch.device("cuda" if torch.cuda.is_available() else "cpu")

# 张量移到 GPU
x = torch.randn(1000, 1000)
x_gpu = x.to(device)       # 或 x.cuda()

# 模型移到 GPU
model = model.to(device)

# 多 GPU (DataParallel — 简单但效率一般)
model = nn.DataParallel(model)

# 混合精度训练（省显存 + 加速）
scaler = torch.cuda.amp.GradScaler()
with torch.cuda.amp.autocast(dtype=torch.bfloat16):
    loss = model(batch)
scaler.scale(loss).backward()
scaler.step(optimizer)
scaler.update()
```

---

## 5. PyTorch vs TensorFlow

| 维度 | PyTorch | TensorFlow/Keras |
|------|---------|------------------|
| **设计哲学** | 动态图（Define-by-Run） | 静态图→动态图（TF 2.x） |
| **调试** | Python 原生调试 ✅ | 需要特殊工具 |
| **LLM 生态** | **统治地位** | 几乎没有 |
| **学术圈** | 90%+ 论文使用 | 罕见 |
| **工业界** | Meta/OpenAI/DeepMind | Google 内部 |
| **部署** | TorchScript/ONNX | TFLite/TF Serving |

```text
结论：LLM 开发选 PyTorch — HuggingFace、vLLM、LoRA 全部基于 PyTorch
      TensorFlow 只在与 Google 生态对接时才考虑
```

---

## 核心要点回顾

- Tensor = 多维数组，`@` 做矩阵乘法，`.to(device)` 移 GPU
- `requires_grad=True` → Autograd 追踪 → `backward()` 自动求导
- 推理时用 `torch.no_grad()` 关闭梯度计算省显存
- `bfloat16` 是 LLM 训练标配精度（FP32 的动态范围 + FP16 的速度）
- LLM 开发 = PyTorch，TensorFlow 在这个领域已被边缘化
