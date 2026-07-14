# PyTorch 核心知识点全梳理

> **核心摘要**：本文从 PyTorch 的基础核心（张量、自动微分）到模型构建、训练验证、进阶优化（迁移学习、量化、混合精度训练）进行系统梳理，帮助开发者快速掌握 PyTorch 的核心知识体系与工程实践能力。

> **前置阅读**：[[PyTorch 安装配置完整指南]]、[[ML-深度学习与机器学习]]

---

## 目录

1. [基础核心](#1-基础核心)
2. [模型构建](#2-模型构建)
3. [训练与验证](#3-训练与验证)
4. [进阶优化](#4-进阶优化)
5. [核心要点回顾](#5-核心要点回顾)
6. [参考资料](#6-参考资料)

---

## 1. 基础核心

### 1.1 张量（Tensor）

**张量**是 PyTorch 的基础数据结构，类似 NumPy 数组，但支持 GPU 加速和自动微分。

```python
import torch

# 创建张量
x = torch.randn(3, 5)  # 随机正态分布
y = torch.zeros((2, 3), dtype=torch.float32)

# 张量运算
z = x @ y.t()  # 矩阵乘法 + 转置

# 设备迁移
x_cuda = x.to('cuda')
```

> **重点**：需重点掌握张量的形状变换（`reshape`/`view`）、索引切片、广播机制。

### 1.2 自动微分（Autograd）

PyTorch 的自动求导引擎，支持**动态计算图**。

```python
x = torch.tensor([1.0, 2.0], requires_grad=True)
y = x ** 2 + 3 * x
y.backward(torch.tensor([1.0, 1.0]))  # 反向传播
print(x.grad)  # 输出梯度：tensor([5., 7.])
```

> **重点**：需掌握 `requires_grad`、`backward()`、`with torch.no_grad()`（推理时禁用梯度）的用法。

---

## 2. 模型构建

### 2.1 nn.Module 基类

所有自定义模型需继承 `nn.Module`，通过 `__init__` 定义层，`forward` 定义前向传播。

```python
import torch.nn as nn

class SimpleNet(nn.Module):
    def __init__(self, input_dim, output_dim):
        super().__init__()
        self.fc1 = nn.Linear(input_dim, 128)
        self.relu = nn.ReLU()
        self.fc2 = nn.Linear(128, output_dim)

    def forward(self, x):
        x = self.fc1(x)
        x = self.relu(x)
        return self.fc2(x)
```

### 2.2 常用网络层

| 层类型 | 说明 | 常用 API |
|--------|------|----------|
| 卷积层 | 提取空间特征 | nn.Conv1d / Conv2d / Conv3d |
| 池化层 | 降维，保留关键特征 | nn.MaxPool2d / AvgPool2d |
| 循环层 | 处理时序数据 | nn.LSTM / nn.GRU |
| Transformer | 捕捉长距离依赖 | nn.TransformerEncoder |

### 2.3 损失函数

| 任务类型 | 损失函数 |
|----------|----------|
| 回归（数值预测） | nn.MSELoss（均方误差）、nn.L1Loss（MAE） |
| 分类 | nn.CrossEntropyLoss（多分类）、nn.BCELoss（二分类） |

---

## 3. 训练与验证

### 3.1 数据加载

自定义 Dataset 需继承 `torch.utils.data.Dataset`，实现 `__len__` 和 `__getitem__` 方法。使用 DataLoader 进行批量加载。

```python
from torch.utils.data import DataLoader, Dataset

class RadarDataset(Dataset):
    def __len__(self):
        return len(self.data)

    def __getitem__(self, idx):
        return self.data[idx], self.labels[idx]

dataloader = DataLoader(RadarDataset(), batch_size=32, shuffle=True)
```

### 3.2 训练循环

```python
model = SimpleNet(100, 3).to('cuda')
optimizer = torch.optim.Adam(model.parameters(), lr=1e-3)
criterion = nn.MSELoss()

for epoch in range(100):
    model.train()
    for data, labels in dataloader:
        data, labels = data.to('cuda'), labels.to('cuda')
        optimizer.zero_grad()
        outputs = model(data)
        loss = criterion(outputs, labels)
        loss.backward()
        optimizer.step()

    # 验证阶段
    model.eval()
    with torch.no_grad():
        val_loss = ...
```

### 3.3 模型保存与加载

```python
# 保存权重
torch.save(model.state_dict(), 'model.pth')

# 加载权重
model = SimpleNet(100, 3)
model.load_state_dict(torch.load('model.pth'))
```

---

## 4. 进阶优化

### 4.1 迁移学习

加载预训练模型，冻结部分层，微调适配新任务：

```python
from torchvision import models

backbone = models.resnet18(pretrained=True)
backbone.fc = nn.Linear(512, 3)  # 替换最后一层
```

### 4.2 模型量化与部署

- **量化**：使用 `torch.ao.quantization` 将模型转为 INT8 精度，减少计算量和显存占用
- **边缘部署**：使用 TorchScript 或 ONNX 导出模型，部署到嵌入式设备

### 4.3 混合精度训练

使用 `torch.cuda.amp` 实现 FP16 混合精度训练，加速训练并减少显存占用。

---

## 5. 核心要点回顾

- 张量是 PyTorch 的基础数据结构，支持 GPU 加速和自动微分
- Autograd 实现动态计算图的自动求导，是训练机制的核心
- 模型构建继承 nn.Module，实现 __init__ 和 forward 方法
- 训练循环包含前向传播、损失计算、反向传播和参数更新四个步骤
- 迁移学习、混合精度训练、模型量化是生产级项目必备的进阶技能
- 模型保存推荐使用 state_dict 格式，灵活且兼容性好

---

## 6. 参考资料

1. PyTorch 官方文档：https://pytorch.org/docs/stable/
2. PyTorch 官方教程：https://pytorch.org/tutorials/
3. nn.Module 官方 API 文档
4. torch.cuda.amp 混合精度训练文档
5. TorchScript 和 ONNX 导出文档
