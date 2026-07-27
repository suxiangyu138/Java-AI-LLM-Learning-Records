# 00 - pip 与 Python 包管理 知识体系总览

> 🎯 pip 是 Python 生态的包管理器，也是 AI 开发中"环境问题"的头号来源——CUDA 版本冲突、依赖地狱、虚拟环境混乱，本系列帮你系统掌控 Python 依赖管理

> 🎯 共 **12 篇**，从 pip 基础到 Poetry 现代方案、从虚拟环境到 Docker 依赖、从 CUDA 陷阱到企业私服

---

## 1. 知识全景

```
pip 与包管理体系（12个文件）
│
├── 🏗️ 基础篇（01-03）
│   ├── 01-pip基础命令全解.md            # install/uninstall/list/show/freeze
│   ├── 02-虚拟环境：venv与conda.md       # venv/conda/poetry shell/最佳实践
│   └── 03-依赖锁定与版本管理.md          # requirements.txt/pip-tools/pip freeze
│
├── 🔧 进阶篇（04-06）
│   ├── 04-pip install原理与缓存.md       # 解析→下载→构建→安装 全流程
│   ├── 05-常见安装问题排查.md            # gcc/网络/权限/CUDA/平台兼容
│   └── 06-Poetry现代依赖管理.md          # pyproject.toml/lock/脚本/发布
│
├── 🚀 AI专项篇（07-09）
│   ├── 07-CUDA与PyTorch安装指南.md       # CUDA/cuDNN/PyTorch版本矩阵
│   ├── 08-AI项目依赖管理最佳实践.md       # transformers/diffusers/LLM项目模板
│   └── 09-Docker中的Python依赖管理.md     # Dockerfile最佳实践/多阶段构建
│
├── 📋 工程篇（10）
│   └── 10-私服与企业级包管理.md           # 阿里云镜像/pip.conf/DevPI
│
└── 📌 冲刺篇（11）
    └── 11-面试高频考点与总结.md           # 依赖冲突/虚拟环境/CI/CD pip
```

## 2. 文件导航

| # | 文件 | 核心内容 | 级别 |
|---|------|----------|:---:|
| 00 | 知识体系总览 | 全景+路线 | — |
| 01 | pip基础命令 | install/uninstall/freeze | ⭐⭐⭐⭐⭐ |
| 02 | 虚拟环境 | venv/conda/Poetry | ⭐⭐⭐⭐⭐ |
| 03 | 依赖锁定 | requirements/pip-tools | ⭐⭐⭐⭐ |
| 04 | install原理 | wheel/sdist/cache | ⭐⭐⭐ |
| 05 | 问题排查 | gcc/网络/权限/CUDA | ⭐⭐⭐⭐ |
| 06 | Poetry | pyproject.toml/lock | ⭐⭐⭐⭐ |
| 07 | CUDA安装 | CUDA/cuDNN/PyTorch | ⭐⭐⭐⭐⭐ |
| 08 | AI项目实践 | transformers/diffusers | ⭐⭐⭐⭐ |
| 09 | Docker依赖 | Dockerfile/多阶段 | ⭐⭐⭐ |
| 10 | 私服 | 镜像源/pip.conf | ⭐⭐⭐ |
| 11 | 面试考点 | 依赖冲突/虚拟环境 | ⭐⭐⭐⭐ |

## 3. 学习路线

```text
🟢 上手（20min）：01-基础命令 → 02-虚拟环境
🔵 理解（45min）：03-依赖锁定 → 04-install原理 → 05-问题排查
🟣 进阶（45min）：06-Poetry → 07-CUDA → 08-AI项目实践
🟡 工程（30min）：09-Docker → 10-私服
🔴 冲刺（15min）：11-面试
```

## 4. 核心概念速查

| 术语 | 含义 |
|------|------|
| **PyPI** | Python Package Index，Python 官方包仓库 |
| **wheel (.whl)** | 预编译的包格式，安装最快 |
| **sdist (.tar.gz)** | 源码分发格式，需要本地编译 |
| **venv** | Python 内置虚拟环境，轻量级 |
| **conda** | Anaconda 的包+环境管理器 |
| **Poetry** | 现代化的 Python 依赖+项目管理工具 |
| **CUDA** | NVIDIA GPU 计算平台，AI 依赖的基础 |
