# 07 - CUDA 与 PyTorch 安装指南

> 🎯 CUDA 是 AI 开发的"地基"。PyTorch 的 CUDA 版本装错，GPU 就废了。本章提供完整的 CUDA/PyTorch 版本矩阵和安装决策树

---

## 目录

1. [CUDA 基础概念](#1-cuda-基础概念)
2. [PyTorch 版本矩阵](#2-pytorch-版本矩阵)
3. [安装决策树](#3-安装决策树)
4. [Docker 中的 CUDA](#4-docker-中的-cuda)

---

## 1. CUDA 基础概念

```text
三个版本要区分：

1. NVIDIA 驱动版本
   nvidia-smi → 右上角 "CUDA Version: 12.5"
   → 这是驱动支持的 MAX CUDA 版本，向下兼容

2. CUDA Toolkit 版本
   nvcc --version → "release 12.1"
   → 你安装的 CUDA 工具包版本

3. PyTorch CUDA 版本
   torch.version.cuda → "12.1"
   → PyTorch 预编译时用的 CUDA 版本

关系：驱动版本 >= Toolkit 版本 >= PyTorch CUDA 版本
```

## 2. PyTorch 版本矩阵

| PyTorch | CUDA | cuDNN | Python | 推荐 |
|------|:---:|:---:|:---:|:---:|
| 2.5.x | 12.4 | 9.x | 3.9-3.12 | 🆕 最新 |
| **2.4.0** | **12.1** / 12.4 | 9.x | 3.9-3.12 | ✅ 当前推荐 |
| 2.3.0 | 11.8 / 12.1 | 8.9 | 3.8-3.12 | ✅ 稳定 |
| 2.2.0 | 11.8 / 12.1 | 8.9 | 3.8-3.11 | 兼容旧驱动 |
| 2.0.0 | 11.7 / 11.8 | 8.7 | 3.8-3.11 | 旧项目 |

### 检查当前环境

```python
import torch

print(f"PyTorch: {torch.__version__}")
print(f"CUDA available: {torch.cuda.is_available()}")
print(f"CUDA version: {torch.version.cuda}")
print(f"cuDNN version: {torch.backends.cudnn.version()}")
print(f"GPU count: {torch.cuda.device_count()}")
print(f"GPU name: {torch.cuda.get_device_name(0)}")
```

```bash
# 命令行检查
nvidia-smi                    # 驱动 + GPU 型号 + 温度 + 显存
nvcc --version                # CUDA Toolkit 版本
nvidia-smi topo -m            # GPU 拓扑（多卡时重要）
```

## 3. 安装决策树

```text
你的情况？
│
├── 🆕 新项目，2080Ti 以上 GPU
│   └── PyTorch 2.4 + CUDA 12.1
│       conda install pytorch torchvision pytorch-cuda=12.1 -c pytorch -c nvidia
│       或 pip install torch --index-url https://download.pytorch.org/whl/cu121
│
├── 🏢 服务器，A100/H100
│   └── PyTorch 2.4+ + CUDA 12.4 (支持新架构)
│       pip install torch --index-url https://download.pytorch.org/whl/cu124
│
├── 🕰️ 旧 GPU（GTX 10系列 / 驱动 < 525）
│   ├── nvidia-smi → CUDA Version: 11.x?
│   │   └── pip install torch --index-url https://download.pytorch.org/whl/cu118
│   └── nvidia-smi → CUDA Version: 12.x?
│       └── pip install torch --index-url https://download.pytorch.org/whl/cu121
│
├── 💻 无 NVIDIA GPU（Mac / AMD / Intel）
│   └── pip install torch  (CPU 版自动)
│       或 conda install pytorch cpuonly -c pytorch
│
└── 🤖 想用 Apple Silicon GPU (M1/M2/M3)
    └── pip install torch (自动支持 MPS 后端)
        device = torch.device("mps")
```

### Conda 一键安装（推荐）

```bash
# 最稳路线：conda 管理 CUDA 依赖
conda create -n torch-env python=3.11
conda activate torch-env

# 安装 PyTorch + CUDA 全家桶（conda 自动匹配 CUDA 版本）
conda install pytorch torchvision torchaudio pytorch-cuda=12.1 \
  -c pytorch -c nvidia

# 验证
python -c "import torch; print(torch.cuda.is_available())"
```

## 4. Docker 中的 CUDA

```dockerfile
# 使用 NVIDIA 官方 CUDA 基础镜像
FROM nvidia/cuda:12.1.0-runtime-ubuntu22.04

# 安装 Python + pip
RUN apt-get update && apt-get install -y python3.11 python3-pip

# 安装 PyTorch（CUDA 版本一致）
RUN pip install torch torchvision --index-url https://download.pytorch.org/whl/cu121

# 运行时：
# docker run --gpus all my-image
```

## 核心要点回顾

- 驱动 >= Toolkit >= PyTorch CUDA 版本（向下兼容）
- `nvidia-smi` → 看驱动支持的 MAX CUDA 版本
- Conda 装 PyTorch = 最省心（自动管理 CUDA/cuDNN 依赖）
- Docker 用 `nvidia/cuda` 基础镜像，版本必须匹配 PyTorch CUDA
- 装完第一件事：`torch.cuda.is_available()` 验证

## 参考资料

1. PyTorch 安装页面 — pytorch.org/get-started
2. NVIDIA CUDA 文档 — docs.nvidia.com/cuda
