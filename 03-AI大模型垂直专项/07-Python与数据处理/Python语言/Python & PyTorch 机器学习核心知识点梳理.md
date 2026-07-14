# Python & PyTorch 机器学习核心知识点梳理

> **核心摘要**：结合"毫米波雷达头部姿态估计"项目需求，整理从 Python 基础到 PyTorch 实战的核心知识点，按 Python 基础工具链 → 机器学习核心理论 → PyTorch 核心 → 项目实战的逻辑递进。

---

## 一、Python 机器学习基础工具链

### 1.1 核心数据处理库

| 库 | 用途 | 项目场景 |
|----|------|---------|
| **NumPy** | 数组运算、矩阵操作、傅里叶变换 | 雷达信号处理和特征提取的基础 |
| **Pandas** | 表格数据的清洗、特征工程 | 处理采集到的姿态标签数据集 |
| **SciPy** | 信号滤波（卡尔曼滤波、小波变换） | 雷达回波的预处理 |

### 1.2 可视化与评估工具

| 工具 | 用途 |
|------|------|
| **Matplotlib / Seaborn** | 绘制时频图、姿态角误差曲线 |
| **Scikit-learn** | 传统机器学习算法（SVM、随机森林）、数据划分、评估指标 |

## 二、机器学习核心理论（与项目强相关）

### 2.1 数据预处理

- **数据清洗**：处理雷达回波中的噪声、异常值
- **特征工程**：从雷达时频图、点云中提取手工特征（轮廓、微动频率等）
- **数据增强**：对雷达数据添加噪声、旋转，提升模型泛化能力
- **归一化/标准化**：确保输入特征的尺度统一，加速模型收敛

### 2.2 模型选型与评估

| 模型类型 | 适用场景 | 示例 |
|---------|---------|------|
| 传统模型 | 小样本姿态分类 | SVM、随机森林 |
| CNN | 处理时频图 | ResNet、VGG |
| PointNet | 处理点云 | PointNet 系列 |
| Transformer | 处理时序信号 | 时序 Transformer |

**评估指标**：MAE（平均绝对误差）、RMSE（均方根误差），用于衡量姿态角的估计精度。

## 三、PyTorch 核心知识点

### 3.1 张量与自动微分

```python
import torch

# 张量创建与运算
x = torch.tensor([[1, 2], [3, 4]], dtype=torch.float32)
y = torch.cat([x, x], dim=0)  # 拼接

# 自动微分
x = torch.randn(3, requires_grad=True)
y = x.pow(2).sum()
y.backward()  # 反向传播
print(x.grad)  # 梯度
```

### 3.2 模型构建与组件

```python
import torch.nn as nn

class HeadPoseModel(nn.Module):
    def __init__(self):
        super().__init__()
        self.conv1 = nn.Conv2d(1, 32, kernel_size=3)  # 处理时频图
        self.fc = nn.Linear(32 * 26 * 26, 3)           # 输出 Yaw/Pitch/Roll

    def forward(self, x):
        x = self.conv1(x)
        x = x.view(x.size(0), -1)
        return self.fc(x)
```

**常用层**：
- `nn.Conv2d`：处理时频图
- `nn.Linear`：全连接层
- `nn.LSTM`：处理时序信号

**损失函数**：
- `nn.MSELoss`：回归任务（适合姿态角预测）
- `nn.CrossEntropyLoss`：分类任务

### 3.3 训练与验证流程

```python
from torch.utils.data import Dataset, DataLoader

class RadarDataset(Dataset):
    def __init__(self, data_path):
        # 加载雷达数据
        pass
    def __len__(self):
        return len(self.data)
    def __getitem__(self, idx):
        return self.data[idx], self.label[idx]

# 训练循环
model = HeadPoseModel()
optimizer = torch.optim.Adam(model.parameters(), lr=1e-3)
criterion = nn.MSELoss()

for epoch in range(100):
    for batch in DataLoader(train_dataset, batch_size=32):
        x, y = batch
        pred = model(x)
        loss = criterion(pred, y)
        optimizer.zero_grad()
        loss.backward()
        optimizer.step()
```

### 3.4 进阶技术

- **迁移学习**：加载预训练 CNN 模型（如 ResNet），微调后用于雷达时频图的特征提取
- **模型量化**：`torch.ao.quantization` 实现模型量化，部署到边缘设备（如 Jetson Nano）
- **混合精度训练**：`torch.cuda.amp` 加速训练，减少显存占用

## 四、项目实战关键技能

1. **雷达数据与张量转换**：将原始雷达回波数据转换为 PyTorch 张量，构建适合模型输入的格式（如时频图、点云）
2. **点云处理**：用 Open3D 或 torch-points3d 库处理雷达点云，提取三维特征
3. **端到端姿态回归**：用 PyTorch 实现 PointNet 或 3D CNN 模型，直接从雷达数据回归出欧拉角（Yaw、Pitch、Roll）
4. **实时推理优化**：通过模型剪枝、量化提升推理速度，满足雷达系统的低延迟需求

## 核心要点回顾

- NumPy + Pandas + SciPy 构成 Python 数据科学生态基础
- PyTorch 的 Tensor 和 Autograd 是深度学习核心
- `nn.Module` 定义模型，`Dataset`/`DataLoader` 加载数据
- 迁移学习和模型量化是项目优化的关键手段
- 端到端姿态回归需要处理雷达数据到张量的转换

## 参考资料

1. PyTorch 官方文档 - Tensor 与 Autograd
2. NumPy 官方文档 - 数组运算指南
3. Scikit-learn 官方文档 - 模型评估指标
