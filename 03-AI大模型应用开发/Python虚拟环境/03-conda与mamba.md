# 03 - conda 与 mamba

> **核心摘要**：conda 不是「Python 环境工具」——它是跨语言二进制包管理器，能管理 CUDA、GCC、GDAL 等系统级依赖，这正是数据科学/AI 场景选它的原因。本文覆盖 conda 定位、Miniconda 安装、环境管理、与 pip/uv 混用规则及 2026 年最佳实践。

> **前置阅读**：[[01-虚拟环境核心原理]]、[[02-venv与virtualenv]]

---

## 📚 目录

1. [conda 的正确定位](#1-conda-的正确定位)
2. [Miniconda vs Anaconda](#2-miniconda-vs-anaconda)
3. [conda 环境管理](#3-conda-环境管理)
4. [mamba：conda 的加速版](#4-mambaconda-的加速版)
5. [conda 与 pip/uv 混用规则](#5-conda-与-pipuv-混用规则)
6. [Windows 分层管理方案](#6-windows-分层管理方案)
7. [conda 的边界与避坑](#7-conda-的边界与避坑)
8. [核心要点](#8-核心要点)

---

## 1. conda 的正确定位

### 1.1 conda 是什么

> ⚠️ **最常见的误解**：把 conda 当「Python 版本管理器」。正确认知：conda 是**跨语言二进制包管理器**——管理 Python 的同时还能管理 C/C++ 库、CUDA、编译器、R 包等系统级依赖。

```
conda 管理范围
├── Python 解释器（版本切换）
├── C/C++ 库（GCC 编译器、OpenBLAS）
├── CUDA 工具链（GPU 深度学习）
├── 地理库（GDAL/PROJ）
├── R 语言包
└── 任意二进制依赖
→ 这正是 uv/venv 做不到的（它们只管理纯 Python 包）
```

### 1.2 何时必须用 conda

```text
必须 conda 的场景
├── ✅ CUDA 版 PyTorch/TensorFlow（GPU 环境）
├── ✅ GDAL/PROJ 等地理空间库（编译地狱）
├── ✅ 生信软件（Bioconductor 生态）
├── ✅ 需要编译的 C++ 项目依赖
└── ❌ 纯 Python 项目（uv 更快更好）
```

> 🎯 **2026 年定位**：**仅在需要非 Python 原生依赖时使用 conda**——纯 Python 项目的默认是 uv。

---

## 2. Miniconda vs Anaconda

### 2.1 对比

| 维度 | Anaconda | Miniconda |
|------|---------|-----------|
| 体积 | 3GB+（预装 1500+ 包） | **~100MB**（极简 + conda） |
| 安装时间 | 长 | 秒级 |
| 适合 | 初学者尝鲜（不推荐 2026） | **推荐（2026 主流）** |
| 自由度 | 高（预装可卸载） | 高（按需安装） |

> 💡 **2026 建议**：一律用 **Miniconda**——Anaconda 预装的大量包大多数项目用不上，且版本固定易冲突。

### 2.2 安装 Miniconda

```bash
# Windows：官网下载 exe 安装，勾选 "Add to PATH"（或手动配置）
# Linux/macOS
curl -L https://repo.anaconda.com/miniconda/Miniconda3-latest-Linux-x86_64.sh -o miniconda.sh
bash miniconda.sh

# 验证
conda --version
```

---

## 3. conda 环境管理

### 3.1 核心命令

```bash
# 创建环境（指定 Python 版本）
conda create -n ml310 python=3.10

# 激活 / 退出
conda activate ml310
conda deactivate

# 查看环境列表
conda env list

# 安装/卸载包
conda install numpy
conda remove numpy

# 导出/复现环境
conda env export > environment.yml
conda env create -f environment.yml

# 删除环境
conda env remove -n ml310
```

### 3.2 environment.yml

```yaml
# environment.yml
name: ml-project
channels:
  - conda-forge
  - defaults
dependencies:
  - python=3.10
  - pytorch=2.5.0
  - pytorch-cuda=12.4        # CUDA 版（conda 独有能力）
  - numpy
  - pandas
  - pip:                      # 混用：pip 安装的包列表
    - pydantic
    - fastapi
```

> 🎯 **environment.yml 是 conda 项目的复现基石**——包含通道（channels）、Python 版本、conda 包与 pip 包，必须提交 Git。

---

## 4. mamba：conda 的加速版

### 4.1 为什么需要 mamba

> ⚠️ **conda 的痛点**：依赖求解器慢（大型环境可能卡几分钟）——mamba 用 C++ 重写求解器，**快 10-30 倍**。

| 维度 | conda | mamba |
|------|-------|-------|
| 求解器 | Python 实现 | **C++ 实现** |
| 安装速度 | 慢（大型环境分钟级） | **快 10-30 倍** |
| 命令兼容 | 标准 | `mamba` 命令与 conda 兼容 |
| 2026 推荐 | 一般 | **数据科学主力** |

```bash
# 安装 mamba（Miniconda 内）
conda install -n base -c conda-forge mamba

# 使用（命令几乎一致）
mamba create -n ml python=3.10
mamba install pytorch pytorch-cuda -c pytorch -c nvidia
```

> 💡 **实践**：`conda` 与 `mamba` 命令可混用（同一环境体系），日常安装用 mamba、习惯性脚本用 conda。

---

## 5. conda 与 pip/uv 混用规则

### 5.1 混用的冲突根源

> ⚠️ **冲突根源**：conda 与 pip 各自维护 `site-packages` 元数据——两边装同名的不同版本包时，互相看不见对方，导致环境损坏。

### 5.2 黄金规则

```text
混用黄金规则：先 conda 后 pip，pip 包不装 conda 能装的
├── ① 顺序固定：先 conda install（底层/编译依赖），再 pip install（纯 Python）
├── ② 分工明确：编译依赖/CUDA → conda；纯 Python 包 → pip
├── ③ 避免重叠：conda 有的包不重复 pip 装
└── ④ 记录完整：environment.yml 中 pip 段记录 pip 包
```

### 5.3 2026 升级：conda + uv 组合

```text
数据科学环境的现代组合
├── 外层：conda 管理 Python 版本 + 原生依赖（CUDA 等）
├── 内层：uv 管理纯 Python 包（快 10-100 倍）
└── 工作流：
    conda create -n ml python=3.10   # 1. 环境（原生依赖）
    conda activate ml
    uv pip install -r requirements.txt  # 2. 纯 Python 包（uv 加速）
```

> 🎯 **2026 共识**：conda 管环境与底层二进制，**内部用 uv 装纯 Python 包**——兼具 conda 的原生能力与 uv 的速度。

---

## 6. Windows 分层管理方案

### 6.1 分层思路（Windows 推荐）

> 💡 Windows 平台实践共识：**Anaconda/Miniconda 统一管理所有 Python 版本（作为解释器仓库）→ 各版本内安装 uv/poetry 等工具 → 项目目录内用工具创建独立 .venv**。

```
Windows 分层管理架构
┌─────────────────────────────────────────┐
│ 第一层：conda（解释器仓库）               │
│   conda base 保持纯净                    │
│   conda create -n py313 python=3.13     │
└──────────────┬──────────────────────────┘
               │ conda activate py313
               ▼
┌─────────────────────────────────────────┐
│ 第二层：工具（装在版本环境内）             │
│   pip install uv / poetry               │
└──────────────┬──────────────────────────┘
               │ uv init / uv add
               ▼
┌─────────────────────────────────────────┐
│ 第三层：项目 .venv（工具创建）            │
│   项目A → .venv (Python 3.13 + 依赖)    │
│   项目B → .venv (Python 3.10 + 依赖)    │
└─────────────────────────────────────────┘
```

### 6.2 分层方案的优势

| 优势 | 说明 |
|------|------|
| **base 纯净** | conda base 不做项目，防污染 |
| **灵活组合** | 不同项目任意 Python 版本 + 工具链 |
| **并行共存** | conda 环境 / .venv 各自隔离 |
| **路径友好** | Windows 下避免 PATH 混乱 |

---

## 7. conda 的边界与避坑

### 7.1 避坑清单

| # | 坑 | 解法 |
|---|-----|------|
| 1 | **conda base 直接装项目包** | base 保持纯净，项目都建独立环境 |
| 2 | **conda 装全部包（含纯 Python）** | 纯 Python 包交给 uv/pip（更快） |
| 3 | **pip 后 conda 覆盖** | 遵守黄金规则：先 conda 后 pip |
| 4 | **混用 channels 冲突** | 锁定 channels（conda-forge 优先） |
| 5 | **慢（大型求解）** | 换 mamba |
| 6 | **Docker 里用 conda** | 见 05 篇：容器内不用 conda |

### 7.2 何时放弃 conda

```text
纯 Python 项目 → 不需要 conda（uv 足够）
├── 无 CUDA/GDAL/编译依赖
├── 团队不熟悉 conda 生态
└── 需要 Docker 镜像精简（conda 镜像巨大）
→ 判断：environment.yml 里没有原生依赖 → 迁 uv
```

---

## 8. 核心要点

> 🎯 **核心要点**：
> 1. conda 是跨语言二进制包管理器（CUDA/GCC/GDAL），不是普通 Python 工具——只在需要原生依赖时用
> 2. Miniconda（~100MB）替代 Anaconda（3GB+）；mamba 替代 conda 求解器（快 10-30 倍）
> 3. 混用黄金规则：**先 conda 后 pip**，纯 Python 包用 uv/pip；2026 组合 = conda 管环境 + uv 装包
> 4. Windows 分层方案：conda 管解释器 → 工具装环境内 → 项目 .venv；Docker 内不用 conda

---

**下一模块**：[04-uv与Poetry现代工具链](04-uv与Poetry现代工具链.md) | **返回总览**：[00-Python虚拟环境知识体系总览](00-Python虚拟环境知识体系总览.md)
