03.14 19:59
PyTorch 核心知识点全梳理
结合你的“毫米波雷达头部姿态估计”项目，这里从基础到实战梳理出你需要掌握的核心内容，按逻辑递进，可直接作为学习路线。
-------------------------------------------------------------------------------------------------
 一、 基础核心
1.  张量（Tensor)
- 核心概念：PyTorch的基础数据结构，类似NumPy数组，但支持GPU加速和自动微分。
- 常用操作：
python 
import torch
# 创建张量
x = torch.randn(3, 5)  # 随机正态分布
y = torch.zeros((2, 3), dtype=torch.float32)
# 张量运算
z = x @ y.t()  # 矩阵乘法 + 转置
# 设备迁移
x_cuda = x.to('cuda')
- 重点掌握：张量的形状变换（ reshape / view ）、索引切片、广播机制。
2.  自动微分（Autograd）
- 核心概念：PyTorch的自动求导引擎，支持动态计算图。
- 关键操作：
python  
x = torch.tensor([1.0, 2.0], requires_grad=True)
y = x ** 2 + 3 * x
y.backward(torch.tensor([1.0, 1.0]))  # 反向传播
print(x.grad)  # 输出梯度：tensor([5., 7.])
 
- 重点掌握： requires_grad 、 backward() 、 with torch.no_grad() （推理时禁用梯度）
-------------------------------------------------------------------------------------------------
二、 模型构建
1.   nn.Module  基类
- 所有自定义模型都需要继承这个类，通过  __init__  定义层， forward  定义前向传播。
- 示例：
python  
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
2.  常用网络层
- 卷积层： nn.Conv1d （一维时序）、 nn.Conv2d （二维时频图）、 nn.Conv3d （三维点云）。
- 池化层： nn.MaxPool2d 、 nn.AvgPool2d ，用于降维。
- 循环层： nn.LSTM 、 nn.GRU ，处理雷达时序回波信号。
- Transformer： nn.TransformerEncoder ，适合捕捉时序信号的长距离依赖。
3.  损失函数
- 回归任务（姿态角预测）： nn.MSELoss （均方误差）、 nn.L1Loss （MAE）。
- 分类任务（姿态分类）： nn.CrossEntropyLoss （多分类）、 nn.BCELoss （二分类）。
 ------------------------------------------------------------------------------------------------
三、 训练与验证
1.  数据加载
- 自定义 Dataset：继承  torch.utils.data.Dataset ，实现  __len__  和  __getitem__ ，加载雷达数据与姿态标签。
- DataLoader：批量加载数据，支持多进程和数据打乱。
python  
from torch.utils.data import DataLoader, Dataset
class RadarDataset(Dataset):
    def __len__(self):
        return len(self.data)
    def __getitem__(self, idx):
        return self.data[idx], self.labels[idx]
dataloader = DataLoader(RadarDataset(), batch_size=32, shuffle=True)
2.  训练循环
- 标准流程：
python  
model = SimpleNet(100, 3).to('cuda')  # 输入100维特征，输出3个姿态角
optimizer = torch.optim.Adam(model.parameters(), lr=1e-3)
criterion = nn.MSELoss()
for epoch in range(100):
    model.train()
    for data, labels in dataloader:
        data, labels = data.to('cuda'), labels.to('cuda')
        optimizer.zero_grad()  # 清空梯度
        outputs = model(data)
        loss = criterion(outputs, labels)
        loss.backward()  # 反向传播
        optimizer.step()  # 更新参数
    # 验证阶段
    model.eval()
    with torch.no_grad():
        val_loss = ...
3.  模型保存与加载
- 保存权重： torch.save(model.state_dict(), 'model.pth') 
- 加载权重：
python  
model = SimpleNet(100, 3)
model.load_state_dict(torch.load('model.pth'))
 ------------------------------------------------------------------------------------------------
四、 进阶优化（项目必备）
1.  迁移学习
- 加载预训练模型（如ResNet、PointNet），冻结部分层，微调适配雷达数据。
- 示例：
python  
from torchvision import models
backbone = models.resnet18(pretrained=True)
backbone.fc = nn.Linear(512, 3)  # 替换最后一层，输出3个姿态角
2.  模型量化与部署
- 量化：用  torch.ao.quantization  将模型转为INT8精度，减少计算量和显存占用。
- 边缘部署：用  TorchScript  或  ONNX  导出模型，部署到Jetson Nano等设备。
3.  混合精度训练
- 用  torch.cuda.amp  实现FP16混合精度训练，加速训练并减少显存占用.
 ------------------------------------------------------------------------------------------------
五、 项目实战适配
针对你的毫米波雷达头部姿态估计项目，重点掌握：
1. 雷达数据转张量：将原始回波、时频图、点云转为PyTorch张量。
2. 点云模型实现：用  torch-points3d  或自定义  PointNet  处理雷达点云。
3. 实时推理优化：模型剪枝、量化，确保在嵌入式设备上低延迟运行。
 
 

