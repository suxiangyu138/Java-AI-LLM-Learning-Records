# 02 - 虚拟环境：venv 与 conda

> 🎯 "在我电脑上能跑"的罪魁祸首就是环境问题。虚拟环境让你每个项目拥有独立的 Python 和依赖，彻底告别版本冲突

---

## 目录

1. [为什么需要虚拟环境](#1-为什么需要虚拟环境)
2. [venv 标准方案](#2-venv-标准方案)
3. [conda AI 开发首选](#3-conda-ai-开发首选)
4. [最佳实践与选择](#4-最佳实践与选择)

---

## 1. 为什么需要虚拟环境

```text
没有虚拟环境：
  项目A 需要 numpy==1.24  ──┐
  项目B 需要 numpy==2.0   ──┤ → 💥 冲突！
  全局 Python 只能装一个版本  ──┘

有了虚拟环境：
  项目A → venv_A → numpy==1.24 ✅
  项目B → venv_B → numpy==2.0  ✅
  互不干扰
```

## 2. venv 标准方案

### 2.1 基础操作

```bash
# 创建
python -m venv .venv           # 推荐 .venv 作为目录名

# 激活
source .venv/bin/activate       # Linux/macOS
.venv\Scripts\activate          # Windows
.venv\Scripts\Activate.ps1      # Windows PowerShell

# 确认
which python                    # → .../.venv/bin/python
pip list                        # 全新环境，几乎为空

# 退出
deactivate

# 删除（直接删目录）
rm -rf .venv
```

### 2.2 项目模板

```bash
# 创建项目的标准步骤
mkdir my-ai-project && cd my-ai-project
git init

# 创建虚拟环境
python -m venv .venv
source .venv/bin/activate

# 安装依赖
pip install torch transformers datasets

# 导出依赖
pip freeze > requirements.txt

# .gitignore
echo ".venv/" >> .gitignore
```

## 3. conda AI 开发首选

### 3.1 为什么 AI 开发者多用 Conda

| 维度 | venv | conda |
|------|:---:|:---:|
| Python 版本管理 | ❌ | ✅ 一键切换 |
| C/C++ 库（CUDA/MKL） | ❌ 需手动 | ✅ 内置 |
| 非 Python 依赖 | ❌ | ✅ (ffmpeg/gcc) |
| 下载速度 | 从 PyPI | 从 conda-forge（更快） |
| 磁盘占用 | 小 (~20MB) | 大 (~500MB+) |
| 推荐场景 | 纯 Python 项目 | **AI / 科学计算** |

### 3.2 基础操作

```bash
# 创建环境（指定 Python 版本）
conda create -n ai-project python=3.11

# 激活/退出
conda activate ai-project
conda deactivate

# 安装包
conda install numpy pandas matplotlib
conda install -c conda-forge jupyterlab

# 导出环境
conda env export > environment.yml     # 跨平台完整环境
conda env export --no-builds > environment.yml  # 去掉系统特定信息

# 从 yml 重建
conda env create -f environment.yml

# 列出所有环境
conda env list

# 删除环境
conda env remove -n ai-project
```

### 3.3 environment.yml 模板

```yaml
name: ai-project
channels:
  - pytorch
  - conda-forge
  - defaults
dependencies:
  - python=3.11
  - pytorch=2.4.0
  - torchvision
  - torchaudio
  - pytorch-cuda=12.1           # CUDA 版本锁定
  - numpy
  - pandas
  - pip
  - pip:
    - transformers==4.44.0
    - datasets==2.20.0
    - accelerate==0.33.0
```

## 4. 最佳实践与选择

```text
你的场景？
├── 🐍 纯 Python 后端（FastAPI/Django）
│   └── venv 或 Poetry ✅
│
├── 🤖 AI/ML 项目（PyTorch/CUDA）
│   └── conda ✅（CUDA 管理是刚需）
│
├── 📦 需要发布到 PyPI 的库
│   └── Poetry ✅（一键构建+发布）
│
├── 🐳 Docker 部署
│   └── venv ✅（轻量，Docker 内不需要 conda）
│
└── 👥 团队协作（跨平台）
    └── conda ✅（environment.yml 锁定完整环境）
```

> 🎯 **我的建议**：AI 项目用 conda 做环境管理 + pip 安装部分包（切到 conda-forge 和 PyTorch 官方 channel），纯 Python 项目用 Poetry。

## 核心要点回顾

- 永远创建虚拟环境！永远创建虚拟环境！
- venv = Python 内置，轻量够用；conda = AI 开发首选（CUDA 支持）
- `.venv/` 加入 `.gitignore`，不要提交虚拟环境
- `pip freeze` ≠ 精确锁定，生产环境用 pip-tools 或 Poetry
- conda environment.yml 可锁定 CUDA 版本，这是 AI 项目的关键

## 参考资料

1. venv 官方文档 — docs.python.org
2. Conda 官方文档 — docs.conda.io
