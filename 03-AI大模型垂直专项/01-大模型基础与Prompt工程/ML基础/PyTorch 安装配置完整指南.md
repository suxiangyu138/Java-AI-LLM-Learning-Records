# PyTorch 安装配置完整指南

> **核心摘要**：本文提供 PyTorch 在 Windows、macOS、Linux 三系统下的完整安装指南，涵盖 Conda 和 Pip 两种安装方式、CUDA 环境配置、安装验证以及常见问题排查，适配从入门到生产环境的各种需求。

> **前置阅读**：[[Python基础]]

---

## 目录

1. [安装前的准备工作](#1-安装前的准备工作)
2. [Conda 安装方式（推荐，稳定适配 GPU）](#2-conda-安装方式)
3. [Pip 安装方式（快速验证，适合轻量项目）](#3-pip-安装方式)
4. [安装验证](#4-安装验证)
5. [常见问题解决](#5-常见问题解决)
6. [核心要点回顾](#6-核心要点回顾)
7. [参考资料](#7-参考资料)

---

## 1. 安装前的准备工作

### 1.1 检查 Python 环境

PyTorch 要求 Python 版本在 3.8 至 3.12 之间。在终端执行以下命令检查当前版本：

```bash
python --version
```

若版本不在要求范围内，建议升级或重新安装 Python，推荐使用 Python 3.10（兼容性最好）。

### 1.2 检查显卡与 CUDA 环境

若电脑配备 NVIDIA 独立显卡，建议安装 CUDA 以获得 GPU 加速，大幅提升模型训练速度。

**检查显卡型号**：

- **Windows**：右键"此电脑" -> "管理" -> "设备管理器" -> "显示适配器"
- **macOS**：苹果菜单 -> "关于本机" -> "显卡"（M1/M2 芯片的 Mac 不支持 CUDA，只能用 CPU 版本）

**安装 CUDA Toolkit**：

访问 NVIDIA CUDA 官网，推荐安装 CUDA 11.8 或 12.1（PyTorch 支持最稳定）。安装时选择自定义，确保勾选 CUDA 和 Visual Studio Integration（Windows 系统）。

**验证 CUDA 安装**：

```bash
nvcc --version
```

若输出 CUDA 版本号，说明安装成功。

### 1.3 选择虚拟环境工具

| 工具 | 适用场景 | 特点 |
|------|----------|------|
| **Conda** | 需要管理复杂依赖、GPU 环境 | 适合科研和多项目开发 |
| **venv** | Python 自带轻量级工具 | 适合快速验证和轻量项目 |

---

## 2. Conda 安装方式

### 2.1 安装 Anaconda/Miniconda

- **Anaconda**：包含大量科学计算库的发行版，适合新手
- **Miniconda**：精简版，仅包含 Conda 和 Python，占用空间更小

安装时 Windows 系统建议勾选"Add Anaconda to my PATH environment variable"。完成后终端输入 `conda --version` 验证安装。

### 2.2 创建并激活虚拟环境

```bash
# 创建虚拟环境，指定 Python 3.10
conda create -n pytorch_env python=3.10

# 激活虚拟环境（Windows）
conda activate pytorch_env

# 激活虚拟环境（Mac/Linux）
source activate pytorch_env
```

激活成功后，终端前缀会显示 `(pytorch_env)`。

### 2.3 配置国内镜像源

解决安装速度慢的问题，切换为清华镜像源：

```bash
conda config --add channels https://mirrors.tuna.tsinghua.edu.cn/anaconda/pkgs/free/
conda config --add channels https://mirrors.tuna.tsinghua.edu.cn/anaconda/pkgs/main/
conda config --add channels https://mirrors.tuna.tsinghua.edu.cn/anaconda/cloud/pytorch/
conda config --set show_channel_urls yes
```

### 2.4 安装 PyTorch

访问 PyTorch 官方安装页面，根据系统、CUDA 版本选择对应命令：

```bash
# GPU 版本（CUDA 11.8，Windows/Linux）
conda install pytorch torchvision torchaudio pytorch-cuda=11.8 -c pytorch -c nvidia

# CPU 版本（无 NVIDIA 显卡）
conda install pytorch torchvision torchaudio cpuonly -c pytorch
```

---

## 3. Pip 安装方式

### 3.1 创建并激活 venv 虚拟环境

```bash
# 创建虚拟环境
python -m venv pytorch_env

# 激活（Windows）
pytorch_env\Scripts\activate

# 激活（Mac/Linux）
source pytorch_env/bin/activate
```

### 3.2 配置 Pip 国内镜像源

```bash
# Windows
pip config set global.index-url https://pypi.tuna.tsinghua.edu.cn/simple
```

### 3.3 安装 PyTorch

```bash
# GPU 版本（CUDA 11.8）
pip3 install torch torchvision torchaudio --index-url https://download.pytorch.org/whl/cu118

# CPU 版本
pip3 install torch torchvision torchaudio --index-url https://download.pytorch.org/whl/cpu
```

---

## 4. 安装验证

### 4.1 基础验证

在终端进入 Python 交互环境，执行以下代码：

```python
import torch

# 验证 PyTorch 版本
print(torch.__version__)

# 验证 CUDA 可用性（GPU 版本才会输出 True）
print(torch.cuda.is_available())

# 验证 CUDA 设备数量
print(torch.cuda.device_count())

# 验证当前 CUDA 设备
print(torch.cuda.get_device_name(0) if torch.cuda.is_available() else "No GPU")
```

> **注意**：若 `torch.cuda.is_available()` 输出 `False`，检查是否正确安装了带 CUDA 的版本，或确认虚拟环境中安装的是 GPU 版本而非 CPU 版本。

### 4.2 运行简单模型验证

```python
# 创建一个随机张量并移到 GPU（如果可用）
x = torch.randn(2, 3).to('cuda' if torch.cuda.is_available() else 'cpu')
y = torch.randn(3, 4).to('cuda' if torch.cuda.is_available() else 'cpu')

# 矩阵乘法
z = x @ y
print(z)
```

---

## 5. 常见问题解决

### 5.1 CUDA 不可用

- 检查显卡驱动是否为最新版本
- 确保 CUDA 版本与 PyTorch 版本匹配（PyTorch 2.0+ 建议 CUDA 11.8 或 12.1）
- 验证虚拟环境中安装的是 GPU 版本而非 CPU 版本

### 5.2 安装速度慢

- 确保已配置国内镜像源（Conda 和 Pip 均需配置）
- 如 Conda 安装卡住，尝试添加 `-c conda-forge` 通道，或改用 Pip 方式安装

### 5.3 环境冲突

始终在虚拟环境中安装 PyTorch，避免在全局环境安装。如虚拟环境出现问题，直接删除后重新创建：

```bash
conda remove -n pytorch_env --all
```

---

## 6. 核心要点回顾

- 安装前需确认 Python 版本（3.8-3.12）和 CUDA 支持情况
- Conda 适合管理复杂依赖和 GPU 环境，是推荐方式
- Pip 适合快速验证和轻量项目
- 国内用户务必配置镜像源以加速下载
- 安装完成后务必通过 torch.cuda.is_available() 验证 GPU 加速是否生效
- 始终在虚拟环境中安装 PyTorch，避免依赖冲突

---

## 7. 参考资料

1. PyTorch 官方安装页面：https://pytorch.org/get-started/locally/
2. NVIDIA CUDA  Toolkit 官方下载页面
3. 清华镜像站 Anaconda 帮助文档
4. PyTorch 官方文档
