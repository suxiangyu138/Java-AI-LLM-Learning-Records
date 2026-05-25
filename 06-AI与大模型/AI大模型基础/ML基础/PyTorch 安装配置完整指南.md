PyTorch 安装配置完整指南
我会把每一步拆解得非常详细，带你从准备到验证全程走通，适配 Windows、Mac、Linux 系统，尤其适合你的毫米波雷达项目需求。
 一、 安装前的准备工作
1. 检查 Python 环境
     PyTorch 对 Python 版本有明确要求，建议使用 3.8~3.12 之间的版本。
     - 在终端（Windows 用命令提示符/Anaconda Prompt，Mac/Linux 用终端）输入：
    bash
  python --version
 
 如果输出的版本不在 3.8~3.12 区间，建议先升级或重新安装 Python。
- 如果你没有 Python 环境，推荐从 Python 官网 下载 3.10 版本（兼容性最好）。
    2. 检查显卡与 CUDA 环境
    如果你的电脑有 NVIDIA 独立显卡，建议安装 CUDA 以获得 GPU 加速，大幅提升模型训练速度。
- 检查显卡型号：
- Windows：右键“此电脑”→“管理”→“设备管理器”→“显示适配器”，查看显卡是否为 NVIDIA 系列。
- Mac：苹果菜单→“关于本机”→“显卡”，注意：M1/M2 芯片的 Mac 不支持 CUDA，只能用 CPU 版本。
- 安装 CUDA Toolkit：
    访问 NVIDIA CUDA 官网，推荐安装 CUDA 11.8 或 12.1（PyTorch 对这两个版本支持最稳定）。
    安装时选择“自定义”，确保勾选“CUDA”和“Visual Studio Integration”（Windows 系统）。
- 验证 CUDA 安装：
    在终端输入：
    bash
  nvcc --version
 
如果输出 CUDA 版本号，说明安装成功。
 3. 选择虚拟环境工具
     为了避免依赖冲突，强烈建议在虚拟环境中安装 PyTorch。常用的虚拟环境工具有两种：
     - Conda：适合需要管理复杂依赖、GPU 环境的场景，推荐科研和多项目开发。
    - venv：Python 自带的轻量级虚拟环境工具，适合快速验证和轻量项目。
 
二、 Conda 安装方式（推荐，稳定适配 GPU）
1. 安装 Anaconda/Miniconda
    - Anaconda 是包含大量科学计算库的发行版，适合新手；Miniconda 是精简版，仅包含 Conda 和 Python，占用空间更小。
    - 下载地址：
    - Anaconda：官网
    - Miniconda：官网
    - 安装时注意：
    - Windows：勾选“Add Anaconda to my PATH environment variable”（方便在终端直接调用）。
    - Mac/Linux：按提示完成安装，终端输入  conda --version  验证是否安装成功。
2. 创建并激活虚拟环境
    - 打开终端（Windows 用 Anaconda Prompt），创建名为  pytorch_env  的虚拟环境，指定 Python 3.10：
    bash
    conda create -n pytorch_env python=3.10
 
- 激活虚拟环境：
    bash

# Windows
conda activate pytorch_env

# Mac/Linux
source activate pytorch_env
 
激活成功后，终端前缀会显示  (pytorch_env) 。
3. 配置国内镜像源（解决安装速度慢的问题）
    默认的 Conda 源在国内速度较慢，建议切换为清华镜像源：
    bash
    conda config --add channels https://mirrors.tuna.tsinghua.edu.cn/anaconda/pkgs/free/
    conda config --add channels https://mirrors.tuna.tsinghua.edu.cn/anaconda/pkgs/main/
    conda config --add channels https://mirrors.tuna.tsinghua.edu.cn/anaconda/cloud/pytorch/
    conda config --set show_channel_urls yes
 
4. 安装 PyTorch
    - 访问 PyTorch 官方安装页面，根据你的系统、CUDA 版本选择对应的命令。
    - 示例命令（Windows/Linux，CUDA 11.8）：
    bash
    conda install pytorch torchvision torchaudio pytorch-cuda=11.8 -c pytorch -c nvidia
    - 如果你的电脑没有 NVIDIA 显卡，安装 CPU 版本：
    bash
    conda install pytorch torchvision torchaudio cpuonly -c pytorch
 
- 等待安装完成，过程中会自动下载并安装所有依赖。
 
三、 Pip 安装方式（快速验证，适合轻量项目）
1. 创建并激活 venv 虚拟环境
    - 在终端中，进入你想要存放虚拟环境的目录，执行：
    bash
    python -m venv pytorch_env
 
- 激活虚拟环境：
    bash

# Windows
pytorch_env\Scripts\activate

# Mac/Linux
source pytorch_env/bin/activate
 
2. 配置 Pip 国内镜像源
    为了加速下载，把 Pip 源切换为清华镜像：
    bash

# Windows
pip config set global.index-url https://pypi.tuna.tsinghua.edu.cn/simple

# Mac/Linux
mkdir -p ~/.pip && echo "[global]
index-url = https://pypi.tuna.tsinghua.edu.cn/simple" > ~/.pip/pip.conf
 
3. 安装 PyTorch
    - 访问 PyTorch 官方页面，复制对应的 Pip 命令。
    - 示例命令（CUDA 11.8）：
    bash
    pip3 install torch torchvision torchaudio --index-url https://download.pytorch.org/whl/cu118
 
- CPU 版本命令：
    bash
    pip3 install torch torchvision torchaudio --index-url https://download.pytorch.org/whl/cpu
 
 
四、 安装验证
1. 基础验证
    在终端中进入 Python 交互环境：
    bash
    python
 
然后执行以下代码：
python
import torch

# 验证 PyTorch 版本
print(torch.__version__)

# 验证 CUDA 可用性（GPU 版本才会输出 True）
print(torch.cuda.is_available())

# 验证 CUDA 设备数量
print(torch.cuda.device_count())

# 验证当前 CUDA 设备
print(torch.cuda.get_device_name(0) if torch.cuda.is_available() else "No GPU")
 
- 如果  torch.cuda.is_available()  输出  True ，说明 GPU 加速已正常启用。
- 如果输出  False ，检查是否安装了正确的 CUDA 版本，或者是否在虚拟环境中安装了 CPU 版本。
    2. 运行简单模型验证
    执行以下代码，测试一个简单的张量运算：
    python

# 创建一个随机张量并移到 GPU（如果可用）
x = torch.randn(2, 3).to('cuda' if torch.cuda.is_available() else 'cpu')
y = torch.randn(3, 4).to('cuda' if torch.cuda.is_available() else 'cpu')

# 矩阵乘法
z = x @ y
print(z)
 
如果能正常输出结果，说明 PyTorch 已完全可用。
 
五、 常见问题解决
1. CUDA 不可用
    - 检查显卡驱动是否为最新版本：访问 NVIDIA 驱动官网 下载并安装对应驱动。
    - 确保 CUDA 版本与 PyTorch 版本匹配：例如 PyTorch 2.0 以上版本建议使用 CUDA 11.8 或 12.1。
    - 验证虚拟环境：确保在激活的虚拟环境中安装了带 CUDA 的 PyTorch 版本，而非 CPU 版本。
2. 安装速度慢
    - 确保已配置国内镜像源（Conda 和 Pip 都要配置）。
    - 如果 Conda 安装卡住，尝试添加  -c conda-forge  通道，或用 Pip 方式重新安装。
3. 环境冲突
    - 不要在全局环境中安装 PyTorch，始终使用虚拟环境隔离依赖。
    - 如果虚拟环境出现问题，直接删除后重新创建：
    bash
    conda remove -n pytorch_env --all
 
 
