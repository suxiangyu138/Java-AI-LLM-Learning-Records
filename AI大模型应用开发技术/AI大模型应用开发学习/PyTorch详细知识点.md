03.30 17:32
PyTorch详细知识点
一、PyTorch基础概述
1.1 什么是PyTorch
PyTorch是一个基于Python的科学计算库，核心用于机器学习和深度学习领域，由Meta（原Facebook）的AI研究团队于2016年发布，最初是Torch框架的Python重写版本，2018年发布1.0正式版，2022年发布1.12后逐步转向LTS（长期支持）版本策略。它兼顾研究灵活性与生产部署效率，既适合学术界快速开展实验，也能满足工业界的落地需求，是当前最流行的深度学习框架之一。
核心优势：支持动态计算图，调试直观；张量计算高效，支持CPU/GPU/TPU多硬件加速；API设计简洁，与Python原生语法高度契合，上手门槛低；生态系统完善，拥有丰富的官方库和第三方工具（如Hugging Face、OpenMMLab），覆盖计算机视觉、自然语言处理等多个领域。
定位：区别于TensorFlow 1.x的静态图模式，PyTorch以动态图为核心特性，同时支持TorchScript和ONNX导出，实现静态图部署，做到“科研与生产一体化”。
1.2 安装与环境配置
PyTorch支持Windows、Linux、macOS三大操作系统，推荐使用Anaconda进行环境管理，避免依赖冲突，常用安装步骤如下：
安装Anaconda环境管理工具；
创建独立Python环境：conda create -n pytorch_env python=3.12（Python版本建议3.8及以上）；
激活环境并安装PyTorch（以CUDA 12.6为例）：pip3 install torch torchvision torchaudio --index-url https://download.pytorch.org/whl/cu126；
验证安装：导入torch并查看版本，确认CUDA是否可用。
验证代码示例：
import torch
print(torch.__version__)  # 查看PyTorch版本
print(torch.cuda.is_available())  # 查看CUDA是否可用（True表示支持GPU加速）
二、核心组件之张量（Tensor）
2.1 张量的定义与本质
张量（Tensor）是PyTorch中所有计算的基础单元，可类比为“高维数组”或“矩阵”，能够表示0维（标量）、1维（向量）、2维（矩阵）、3维（图像：通道×高度×宽度）、4维（批量图像：批量×通道×高度×宽度）等任意维度的数据，是PyTorch处理数据的核心载体。
与NumPy数组的关系：两者高度兼容，可通过torch.from_numpy()将NumPy数组转为张量，通过.numpy()将张量转回NumPy数组，但张量支持GPU加速，而NumPy数组仅支持CPU计算。
2.2 张量的创建方法
2.2.1 基础创建方式
import torch
# 1. 创建标量（0维张量）
scalar = torch.tensor(5.0)
print(scalar.ndim)  # 输出：0（ndim查看维度）
# 2. 创建向量（1维张量）
vector = torch.tensor([1, 2, 3, 4])
print(vector.ndim, vector.shape)  # 输出：1 torch.Size([4])（shape查看形状）
# 3. 创建矩阵（2维张量）
matrix = torch.tensor(((1, 2), (3, 4)))
print(matrix.ndim, matrix.shape)  # 输出：2 torch.Size([2, 2])
# 4. 创建3维张量（例如：1张3通道图片，高度2、宽度2）
tensor_3d = torch.tensor([[[1, 2], [3, 4]], [[5, 6], [7, 8]], [[9, 10], [11, 12]]])
print(tensor_3d.ndim, tensor_3d.shape)  # 输出：3 torch.Size([3, 2, 2])
2.2.2 常用创建函数
# 1. 创建全0张量
zeros_tensor = torch.zeros((3, 2))  # 形状为(3,2)的全0张量
# 2. 创建全1张量
ones_tensor = torch.ones((2, 3, 4))  # 形状为(2,3,4)的全1张量
# 3. 创建随机正态分布张量（均值0，标准差1）
randn_tensor = torch.randn(2, 2)  # 2×2的随机正态分布张量
# 4. 创建均匀分布张量（范围[0,1)）
rand_tensor = torch.rand(3, 3)
# 5. 创建指定范围的整数张量（左闭右开）
arange_tensor = torch.arange(0, 10, 2)  # 从0到10，步长2，输出：[0,2,4,6,8]
# 6. 创建单位矩阵（对角线为1，其余为0）
eye_tensor = torch.eye(4)  # 4×4的单位矩阵
2.3 张量的核心属性
ndim：张量的维度（0/1/2/...）；
shape：张量的形状（各维度的元素个数），可通过torch.reshape()或.view()修改形状（注意：view仅适用于连续张量）；
dtype：张量的数据类型（常用：torch.float32、torch.float64、torch.int32、torch.int64），可通过torch.type()或.to(dtype)修改；
device：张量所在的设备（CPU/GPU），可通过.to(device)迁移设备（如tensor.to('cuda')迁移到GPU）；
requires_grad：是否追踪张量的操作以计算梯度（默认False，用于训练时开启自动微分）；
grad：存储张量的梯度值（仅当requires_grad=True且调用backward()后有效）。
2.4 张量的基本操作
2.4.1 算术运算
a = torch.tensor([1, 2, 3])
b = torch.tensor([4, 5, 6])
# 加法（三种方式）
c1 = a + b
c2 = torch.add(a, b)
a.add_(b)  # 带下划线的方法会修改张量本身（in-place操作）
# 减法、乘法、除法类似
d = a - b
e = a * b  # 元素级乘法（不是矩阵乘法）
f = a / b
# 矩阵乘法（两种方式）
mat1 = torch.tensor([[1, 2], [3, 4]])
mat2 = torch.tensor([[5, 6], [7, 8]])
mat_mul1 = torch.matmul(mat1, mat2)
mat_mul2 = mat1 @ mat2  # @符号等价于matmul
2.4.2 索引与切片
tensor = torch.tensor([[1, 2, 3], [4, 5, 6], [7, 8, 9]])
# 索引（取第2行第3列元素，索引从0开始）
print(tensor[1, 2])  # 输出：tensor(6)
# 切片（取前2行，所有列）
print(tensor[:2, :])  # 输出：tensor([[1,2,3],[4,5,6]])
# 高级索引（取第0行和第2行，第1列和第2列）
print(tensor[[0, 2], [1, 2]])  # 输出：tensor([2,9])
2.4.3 广播机制
当两个张量形状不同时，PyTorch会自动调整张量形状（满足广播条件）后进行运算，广播条件：两个张量的维度从右到左依次匹配，要么维度相同，要么其中一个维度为1。
a = torch.tensor([[1, 2, 3], [4, 5, 6]])  # 形状(2,3)
b = torch.tensor([10, 20, 30])  # 形状(3,)
c = a + b  # 自动广播b为(2,3)，结果形状(2,3)
print(c)  # 输出：tensor([[11,22,33],[14,25,36]])
2.4.4 设备迁移
# 查看当前设备
device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
# 创建CPU上的张量
cpu_tensor = torch.tensor([1, 2, 3])
print(cpu_tensor.device)  # 输出：cpu
# 迁移到GPU
gpu_tensor = cpu_tensor.to(device)
print(gpu_tensor.device)  # 输出：cuda:0（0表示第1块GPU）
# 迁移回CPU
cpu_tensor2 = gpu_tensor.cpu()
print(cpu_tensor2.device)  # 输出：cpu
三、核心组件之自动微分（Autograd）
3.1 自动微分的作用与原理
自动微分（Autograd）是PyTorch实现反向传播、训练模型的核心机制，能够自动计算张量操作的梯度，无需手动推导反向传播公式，极大简化了深度学习模型的训练流程。
核心原理：当张量的requires_grad=True时，PyTorch会追踪该张量的所有操作，构建一个“计算图”（记录操作顺序和依赖关系）；当调用.backward()方法时，会沿着计算图反向传播，自动计算所有相关可训练张量的梯度，并将梯度值存储在张量的.grad属性中。
计算图的特点：动态构建（每执行一行代码，构建一部分计算图），支持Python原生调试（如print、断点），且支持条件判断、循环等动态控制流。
3.2 核心使用步骤
# 1. 创建可训练张量（requires_grad=True）
x = torch.tensor(2.0, requires_grad=True)
# 2. 定义计算过程（构建计算图）
y = x ** 2 + 3 * x + 1  # y = x² + 3x + 1
# 3. 反向传播，计算梯度（dy/dx）
y.backward()  # 仅需调用backward()，无需手动推导
# 4. 查看梯度值（x的梯度为dy/dx = 2x + 3，x=2时，梯度为7）
print(x.grad)  # 输出：tensor(7.)
3.3 关键细节
requires_grad的默认值：False，即普通张量不追踪操作，无法计算梯度；只有设置为True，才能参与梯度计算（通常用于模型的参数）。
detach()方法：将张量从计算图中分离，分离后的张量requires_grad=False，不参与后续梯度计算，常用于固定部分参数（如迁移学习中的特征提取器）。
with torch.no_grad()：上下文管理器，在该上下文内，所有张量的操作都不会被追踪，常用于验证、测试阶段（无需计算梯度，节省内存）。
梯度清零：每次反向传播后，梯度会累积（.grad的值会叠加），因此在训练循环中，每次反向传播前需调用optimizer.zero_grad()清零梯度，避免梯度累积影响训练。
# 示例：梯度清零
x = torch.tensor(2.0, requires_grad=True)
y = x ** 2
y.backward()
print(x.grad)  # 输出：tensor(4.)
# 再次反向传播，梯度累积
y = x ** 2
y.backward()
print(x.grad)  # 输出：tensor(8.)（4+4）
# 梯度清零
x.grad.zero_()
y = x ** 2
y.backward()
print(x.grad)  # 输出：tensor(4.)（清零后重新计算）
四、核心组件之神经网络模块（torch.nn）
torch.nn是PyTorch用于构建神经网络的核心模块，提供了大量预定义的层、损失函数和容器，简化了神经网络的搭建过程，所有神经网络模型都需基于nn.Module类实现。
4.1 核心容器（nn.Module及其子类）
4.1.1 nn.Module（基类）
nn.Module是所有神经网络模型、层的基类，任何自定义模型或层都必须继承该类，核心特点：
必须实现forward()方法，定义前向传播逻辑（模型的核心计算流程）；
自动管理模型参数（通过parameters()或named_parameters()可获取所有可训练参数）；
支持嵌套定义（可在模型中包含其他nn.Module子类，如卷积层、全连接层）；
提供train()（训练模式）和eval()（评估模式）方法，用于切换模型状态（如Dropout、BN层在两种模式下行为不同）。
4.1.2 常用容器
nn.Sequential：序列容器，按顺序执行层操作，适合搭建结构简单、顺序固定的网络（无需手动实现forward()）；
nn.ModuleList：以列表形式保存子模块，可通过索引访问，适合需要动态调整层数量的场景；
nn.ModuleDict：以字典形式保存子模块，可通过键值对访问，适合层的命名管理；
nn.ParameterList/ParameterDict：分别以列表、字典形式保存参数，用于管理模型中的可训练参数。
# 示例1：nn.Sequential搭建简单网络
model = nn.Sequential(
    nn.Linear(784, 128),  # 全连接层
    nn.ReLU(),  # 激活函数
    nn.Linear(128, 10)  # 输出层
)
# 示例2：自定义模型（继承nn.Module）
class Net(nn.Module):
    def __init__(self):
        super(Net, self).__init__()  # 必须调用父类构造函数
        # 定义网络层
        self.conv1 = nn.Conv2d(1, 32, 3, 1)  # 卷积层
        self.conv2 = nn.Conv2d(32, 64, 3, 1)
        self.dropout1 = nn.Dropout(0.25)  # Dropout层
        self.dropout2 = nn.Dropout(0.5)
        self.fc1 = nn.Linear(9216, 128)  # 全连接层
        self.fc2 = nn.Linear(128, 10)
    # 前向传播逻辑
    def forward(self, x):
        x = self.conv1(x)
        x = torch.relu(x)  # 也可使用nn.ReLU()实例
        x = self.conv2(x)
        x = torch.relu(x)
        x = torch.max_pool2d(x, 2)  # 池化层
        x = self.dropout1(x)
        x = torch.flatten(x, 1)  # 展平（从第1维开始，保留批量维度）
        x = self.fc1(x)
        x = torch.relu(x)
        x = self.dropout2(x)
        x = self.fc2(x)
        return torch.log_softmax(x, dim=1)
# 实例化模型
model = Net()
# 查看模型参数
print(list(model.parameters()))
4.2 常用网络层
4.2.1 卷积层（用于提取空间特征，主要用于图像任务）
nn.Conv1d：1D卷积，用于时序数据（如音频、文本序列）；
nn.Conv2d：2D卷积，用于图像数据（最常用），参数说明：nn.Conv2d(in_channels, out_channels, kernel_size, stride=1, padding=0)；
in_channels：输入通道数（如灰度图为1，RGB图为3）；
out_channels：输出通道数（卷积核的数量）；
kernel_size：卷积核大小（如3表示3×3卷积核）；
stride：步长（卷积核移动的步幅）；
padding：填充（在输入边缘填充0，避免特征图缩小）。
nn.Conv3d：3D卷积，用于视频、3D图像等数据；
nn.ConvTranspose2d：转置卷积（反卷积），用于上采样（如生成对抗网络中的生成器）。
4.2.2 池化层（用于降维、减少参数，防止过拟合）
nn.MaxPool2d：最大池化（取卷积窗口内的最大值，最常用）；
nn.AvgPool2d：平均池化（取卷积窗口内的平均值）；
核心作用：保留关键特征，减少特征图尺寸，降低计算量和过拟合风险。
4.2.3 全连接层（用于分类、回归，将提取的特征映射到输出空间）
nn.Linear(in_features, out_features)：参数说明：
in_features：输入特征数（前一层的输出维度）；
out_features：输出特征数（当前层的输出维度）。
4.2.4 激活函数（引入非线性，让模型能拟合复杂函数）
nn.ReLU()：最常用，ReLU(x) = max(0, x)，解决梯度消失问题，计算高效；
nn.Sigmoid()：输出范围[0,1]，用于二分类任务的输出层；
nn.Softmax(dim)：输出范围[0,1]，且所有输出之和为1，用于多分类任务的输出层（dim指定计算维度）；
nn.Tanh()：输出范围[-1,1]，常用于循环神经网络；
nn.LeakyReLU()：解决ReLU的“死亡ReLU”问题（给负区间一个小的斜率）。
4.2.5 正则化层（防止过拟合）
nn.Dropout(p)：随机丢弃p比例的神经元（训练时生效，评估时不丢弃），p通常取0.2~0.5；
nn.BatchNorm2d(num_features)：批量归一化，对每一批数据进行归一化（均值为0，方差为1），加速训练收敛，减少过拟合；
nn.LayerNorm(normalized_shape)：层归一化，对每一层的特征进行归一化，适用于序列数据。
4.2.6 循环层与Transformer层（用于序列数据）
nn.LSTM(input_size, hidden_size, num_layers)：长短期记忆网络，解决传统RNN的梯度消失问题，用于时序数据（如文本、音频）；
nn.GRU(input_size, hidden_size, num_layers)：门控循环单元，LSTM的简化版，计算效率更高；
nn.Transformer：基于自注意力机制的模型，用于自然语言处理（如BERT、GPT）等复杂序列任务。
4.3 损失函数（衡量模型预测值与真实值的差距，用于反向传播更新参数）
损失函数需根据任务类型选择，常用损失函数如下：
nn.MSELoss()：均方误差损失，用于回归任务（如房价预测、温度预测）；
nn.CrossEntropyLoss()：多分类交叉熵损失，内置Softmax函数，直接接收模型输出（未经过Softmax），用于多分类任务（如MNIST手写识别）；
nn.BCELoss()：二分类交叉熵损失，需配合Sigmoid激活函数使用，用于二分类任务；
nn.BCEWithLogitsLoss()：二分类交叉熵损失，内置Sigmoid函数，无需手动添加；
nn.NLLLoss()：负对数似然损失，需配合LogSoftmax使用，常用于多分类任务。
五、优化器（torch.optim）
5.1 优化器的作用
优化器的核心作用是根据损失函数的梯度，更新模型的可训练参数（如卷积层的权重、全连接层的偏置），最小化损失函数，使模型达到更好的预测效果。PyTorch的torch.optim模块提供了多种常用优化器，无需手动实现参数更新逻辑。
5.2 常用优化器
torch.optim.SGD(params, lr, momentum=0)：随机梯度下降，最基础的优化器，参数说明：
params：需要更新的模型参数（通常为model.parameters()）；
lr：学习率（核心参数，控制参数更新的步幅，过大易震荡，过小收敛慢）；
momentum：动量，用于加速收敛，减少震荡（通常取0.9）。
torch.optim.Adam(params, lr=0.001, betas=(0.9, 0.999))：自适应学习率优化器，最常用，结合了SGD和RMSprop的优点，自动调整每个参数的学习率，收敛速度快，稳定性好；
torch.optim.RMSprop(params, lr=0.01)：基于梯度平方的移动平均，解决SGD学习率单一的问题，适合处理非平稳目标；
torch.optim.Adagrad(params, lr=0.01)：自适应学习率，对稀疏数据友好，但学习率会逐渐减小，可能导致后期收敛变慢。
5.3 优化器的核心操作
# 1. 实例化优化器（以Adam为例）
optimizer = torch.optim.Adam(model.parameters(), lr=0.001)
# 2. 梯度清零（每次反向传播前必须执行）
optimizer.zero_grad()
# 3. 计算损失
loss = criterion(output, target)  # criterion为定义的损失函数
# 4. 反向传播，计算梯度
loss.backward()
# 5. 更新模型参数
optimizer.step()
# 6. 学习率调整（可选，用于后期微调学习率）
# 方式1：手动调整
optimizer.param_groups[0]['lr'] *= 0.1  # 学习率衰减为原来的1/10
# 方式2：使用学习率调度器（如StepLR）
scheduler = torch.optim.lr_scheduler.StepLR(optimizer, step_size=10, gamma=0.1)
# 每个step_size个epoch，学习率乘以gamma
scheduler.step()  # 通常在每个epoch结束后调用
六、数据加载与预处理（torch.utils.data）
在深度学习中，数据加载与预处理是关键步骤，PyTorch提供了torch.utils.data模块，简化了数据的读取、批量处理、打乱等操作，核心类为Dataset和DataLoader。
6.1 Dataset（数据集类）
Dataset是PyTorch中表示数据集的抽象类，自定义数据集需继承该类，并实现两个核心方法：
__getitem__(self, idx)：根据索引idx返回一个样本（输入数据+标签）；
__len__(self)：返回数据集的总样本数。
PyTorch还提供了多个内置Dataset（如MNIST、CIFAR-10），可直接调用，无需手动实现数据读取逻辑。
# 示例1：自定义数据集
import os
from PIL import Image
from torch.utils.data import Dataset
class MyDataset(Dataset):
    def __init__(self, img_dir, label_dir, transform=None):
        self.img_dir = img_dir  # 图像文件夹路径
        self.label_dir = label_dir  # 标签文件夹路径
        self.img_names = os.listdir(img_dir)  # 所有图像文件名
        self.transform = transform  # 数据预处理函数
    def __len__(self):
        return len(self.img_names)  # 数据集总样本数
    def __getitem__(self, idx):
        # 读取图像
        img_path = os.path.join(self.img_dir, self.img_names[idx])
        img = Image.open(img_path).convert('RGB')
        # 读取标签（假设标签为txt文件，内容为整数）
        label_path = os.path.join(self.label_dir, self.img_names[idx].replace('.jpg', '.txt'))
        with open(label_path, 'r') as f:
            label = int(f.read())
        # 数据预处理（如Resize、ToTensor等）
        if self.transform:
            img = self.transform(img)
        return img, label  # 返回（输入数据，标签）
# 示例2：使用内置MNIST数据集
from torchvision import datasets, transforms
# 数据预处理
transform = transforms.Compose([
    transforms.ToTensor(),  # 转为Tensor（0~1）
    transforms.Normalize((0.1307,), (0.3081,))  # 归一化（均值，标准差）
])
# 加载训练集
train_dataset = datasets.MNIST(
    root='./data',  # 数据保存路径
    train=True,  # 训练集
    download=True,  # 自动下载（若本地没有）
    transform=transform  # 预处理
)
6.2 DataLoader（数据加载器）
DataLoader是基于Dataset的迭代器，用于批量加载数据，支持多进程加载、数据打乱、批量整理等功能，核心参数如下：
dataset：传入的Dataset对象；
batch_size：每批加载的样本数（常用32、64、128）；
shuffle：是否打乱数据（训练集设为True，测试集设为False）；
num_workers：多进程加载数据的进程数（Windows系统建议设为0，Linux/Mac可设为4、8等）；
drop_last：是否丢弃最后一批不足batch_size的样本（通常设为False）；
collate_fn：自定义批量整理函数，用于处理样本形状不一致的情况。
from torch.utils.data import DataLoader
# 加载训练集
train_loader = DataLoader(
    train_dataset,
    batch_size=64,
    shuffle=True,
    num_workers=0  # Windows系统设为0
)
# 迭代加载数据（训练循环中使用）
for batch_idx, (data, target) in enumerate(train_loader):
    # data：当前批次的输入数据（形状：[batch_size, 通道数, 高度, 宽度]）
    # target：当前批次的标签（形状：[batch_size]）
    # 模型训练逻辑...
    pass
6.3 常用数据预处理（torchvision.transforms）
torchvision.transforms模块提供了大量常用的数据预处理方法，用于将原始数据（如图片）转换为适合模型训练的格式，常用操作如下：
transforms.Resize((h, w))：将图片缩放为指定尺寸；
transforms.CenterCrop(size)：从图片中心裁剪指定尺寸；
transforms.RandomCrop(size)：随机裁剪指定尺寸（数据增强）；
transforms.ToTensor()：将PIL图片或NumPy数组转为Tensor，同时将像素值从[0,255]归一化到[0,1]；
transforms.Normalize(mean, std)：对Tensor进行归一化（每个通道减去均值，除以标准差）；
transforms.RandomFlipHorizontal()：随机水平翻转（数据增强）；
transforms.Compose(transforms_list)：将多个预处理操作组合起来，按顺序执行。
七、PyTorch模型训练完整流程
结合上述知识点，以MNIST手写识别为例，梳理PyTorch模型训练的完整流程，涵盖数据加载、模型定义、损失函数、优化器、训练循环、模型保存与加载等环节。
7.1 完整代码示例
import torch
import torch.nn as nn
import torch.optim as optim
from torchvision import datasets, transforms
from torch.utils.data import DataLoader
# 1. 配置设备
device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
# 2. 数据加载与预处理
transform = transforms.Compose([
    transforms.ToTensor(),
    transforms.Normalize((0.1307,), (0.3081,))
])
# 加载训练集和测试集
train_dataset = datasets.MNIST('./data', train=True, download=True, transform=transform)
test_dataset = datasets.MNIST('./data', train=False, download=True, transform=transform)
# 数据加载器
train_loader = DataLoader(train_dataset, batch_size=64, shuffle=True, num_workers=0)
test_loader = DataLoader(test_dataset, batch_size=64, shuffle=False, num_workers=0)
# 3. 定义模型
class Net(nn.Module):
    def __init__(self):
        super(Net, self).__init__()
        self.conv1 = nn.Conv2d(1, 32, 3, 1)
        self.conv2 = nn.Conv2d(32, 64, 3, 1)
        self.dropout1 = nn.Dropout(0.25)
        self.dropout2 = nn.Dropout(0.5)
        self.fc1 = nn.Linear(9216, 128)
        self.fc2 = nn.Linear(128, 10)
    def forward(self, x):
        x = self.conv1(x)
        x = torch.relu(x)
        x = self.conv2(x)
        x = torch.relu(x)
        x = torch.max_pool2d(x, 2)
        x = self.dropout1(x)
        x = torch.flatten(x, 1)
        x = self.fc1(x)
        x = torch.relu(x)
        x = self.dropout2(x)
        x = self.fc2(x)
        return torch.log_softmax(x, dim=1)
# 实例化模型并迁移到设备
model = Net().to(device)
# 4. 定义损失函数和优化器
criterion = nn.NLLLoss()  # 损失函数
optimizer = optim.Adadelta(model.parameters(), lr=1.0)  # 优化器
# 5. 训练循环
epochs = 10  # 训练轮数
for epoch in range(epochs):
    model.train()  # 切换到训练模式
    train_loss = 0.0  # 记录训练损失
    for batch_idx, (data, target) in enumerate(train_loader):
        # 数据迁移到设备
        data, target = data.to(device), target.to(device)
        # 梯度清零
        optimizer.zero_grad()
        # 前向传播
        output = model(data)
        # 计算损失
        loss = criterion(output, target)
        # 反向传播
        loss.backward()
        # 更新参数
        optimizer.step()
        # 累计训练损失
        train_loss += loss.item() * data.size(0)
    # 计算每轮平均损失
    train_loss = train_loss / len(train_loader.dataset)
    print(f'Epoch [{epoch+1}/{epochs}], Train Loss: {train_loss:.4f}')
# 6. 模型评估
model.eval()  # 切换到评估模式
test_loss = 0.0
correct = 0  # 记录正确预测的样本数
with torch.no_grad():  # 禁用梯度计算
    for data, target in test_loader:
        data, target = data.to(device), target.to(device)
        output = model(data)
        test_loss += criterion(output, target).item() * data.size(0)
        # 计算正确预测数（取输出概率最大的类别）
        pred = output.argmax(dim=1, keepdim=True)
        correct += pred.eq(target.view_as(pred)).sum().item()
# 计算测试集平均损失和准确率
test_loss = test_loss / len(test_loader.dataset)
test_acc = correct / len(test_loader.dataset)
print(f'Test Loss: {test_loss:.4f}, Test Accuracy: {test_acc:.4f}')
# 7. 模型保存
# 方式1：保存模型参数（推荐，占用空间小）
torch.save(model.state_dict(), 'mnist_model.pth')
# 方式2：保存整个模型（占用空间大，不推荐）
# torch.save(model, 'mnist_model_full.pth')
# 8. 模型加载
# 方式1：加载模型参数（需先实例化模型）
model_load = Net().to(device)
model_load.load_state_dict(torch.load('mnist_model.pth'))
# 方式2：加载整个模型
# model_load = torch.load('mnist_model_full.pth').to(device)
7.2 关键注意点
训练模式与评估模式：model.train()会启用Dropout、BN层的训练行为；model.eval()会禁用Dropout，固定BN层的均值和方差，确保评估结果准确。
梯度管理：评估阶段需使用with torch.no_grad()禁用梯度计算，节省内存，避免不必要的计算。
模型保存与加载：推荐使用torch.save(model.state_dict(), path)保存参数，加载时需先实例化模型，再调用load_state_dict()，兼容性更好。
设备一致性：数据、模型必须在同一设备（CPU/GPU）上，否则会报错，需通过.to(device)确保设备一致。
八、高级特性与工程化部署
8.1 混合精度训练
使用torch.cuda.amp模块开启自动混合精度训练，可在不损失模型精度的前提下，减少显存占用，提升训练速度，适用于大数据量、大模型训练。
from torch.cuda.amp import GradScaler, autocast
# 初始化混合精度训练的缩放器
scaler = GradScaler()
# 训练循环中修改
for batch_idx, (data, target) in enumerate(train_loader):
    data, target = data.to(device), target.to(device)
    optimizer.zero_grad()
    # 开启混合精度
    with autocast():
        output = model(data)
        loss = criterion(output, target)
    # 反向传播（缩放损失，避免梯度下溢）
    scaler.scale(loss).backward()
    # 更新参数（先 unscales 梯度）
    scaler.step(optimizer)
    # 更新缩放器
    scaler.update()
8.2 模型量化与轻量化
通过torch.quantization模块对模型进行量化（将float32转为int8），压缩模型体积，提升推理速度，适用于边缘设备（如手机、嵌入式设备）部署。
8.3 多GPU训练与分布式训练
当单GPU显存不足时，可使用多GPU训练，PyTorch提供nn.DataParallel（简单多GPU）和torch.distributed（分布式训练）两种方式，其中torch.distributed适用于多机多GPU场景，效率更高。
8.4 模型部署
PyTorch支持多种部署方式，满足不同场景需求：
TorchScript：将PyTorch模型转换为TorchScript格式，可用于C++部署，提升推理速度；
ONNX：将模型导出为ONNX格式，实现跨框架部署（如PyTorch模型导出后，可在TensorFlow、Caffe中使用）；
PyTorch Mobile：将模型导出为移动端可执行格式，用于Android、iOS应用开发；
TorchServe：用于企业级模型部署，支持模型管理、推理服务化。
九、常见问题与解决方案
CUDA内存不足：减小batch_size；使用混合精度训练；使用梯度累积；删除无用张量（如del tensor）；清理显存（torch.cuda.empty_cache()）。
梯度消失/梯度爆炸：使用ReLU等激活函数；使用BN层；调整学习率；使用残差连接（ResNet）；权重初始化（如Xavier初始化）。
模型过拟合：增加数据增强；使用Dropout、BN层；增加训练数据量；使用正则化（L1/L2正则）。
数据加载速度慢：增大num_workers（Linux/Mac）；使用数据预加载；将数据转为Tensor格式保存，减少每次加载时的转换耗时。
PyTorch与TensorFlow选择：研究场景推荐PyTorch（动态图调试方便）；生产环境若需成熟部署工具链，可考虑TensorFlow。

