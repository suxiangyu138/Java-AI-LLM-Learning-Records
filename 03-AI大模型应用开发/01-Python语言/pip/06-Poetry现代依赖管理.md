# 06 - Poetry 现代依赖管理

> 🎯 Poetry 是 Python 依赖管理的现代化方案——一个 `pyproject.toml` 统一管理依赖+构建+发布，`poetry.lock` 精确锁定整个依赖树，告别 requirements.txt 的混乱

---

## 目录

1. [Poetry vs pip 对比](#1-poetry-vs-pip-对比)
2. [快速上手](#2-快速上手)
3. [pyproject.toml 详解](#3-pyprojecttoml-详解)
4. [与 AI 项目结合](#4-与-ai-项目结合)

---

## 1. Poetry vs pip 对比

| 功能 | pip + venv | Poetry |
|------|:---:|:---:|
| 依赖管理 | requirements.txt | pyproject.toml |
| 精确锁定 | 需 pip-tools 辅助 | ✅ poetry.lock 内置 |
| 虚拟环境 | 需手动创建/激活 | ✅ 自动管理 |
| 构建发布 | 需 setup.py/setup.cfg | ✅ 内置 `poetry build/publish` |
| 依赖解析 | 基础（易冲突） | ✅ SAT solver（更智能） |
| 脚本定义 | 无 | ✅ [tool.poetry.scripts] |
| AI 项目适配 | ✅ | ⚠️ PyTorch/CUDA 需额外配置 |

> 🎯 Poetry 适合纯 Python 项目和库的管理；重度 AI 项目（CUDA 依赖复杂）推荐 conda + pip 组合

## 2. 快速上手

```bash
# 安装
pip install poetry

# 初始化项目
poetry new my-project
cd my-project

# 或在已有项目中初始化
poetry init

# 安装依赖
poetry add requests numpy pandas          # 生产依赖
poetry add --group dev pytest black       # 开发依赖

# 安装所有依赖（从 poetry.lock）
poetry install

# 虚拟环境
poetry shell                                # 进入虚拟环境
poetry env info                             # 查看环境路径
poetry run python script.py                 # 不激活直接运行

# 锁定更新
poetry lock                                 # 更新 poetry.lock
poetry update                               # 更新所有依赖到最新

# 构建发布
poetry build                                # 生成 wheel + sdist
poetry publish                              # 发布到 PyPI
```

## 3. pyproject.toml 详解

```toml
[tool.poetry]
name = "my-ai-project"
version = "0.1.0"
description = "AI-powered document analyzer"
authors = ["Your Name <email@example.com>"]

[tool.poetry.dependencies]
python = "^3.11"                      # ^3.11 = >=3.11,<4.0
torch = {version = ">=2.4.0,<2.5.0", source = "pytorch"}
transformers = "^4.44"
datasets = "^2.20"
numpy = "<2.0"                        # 暂时不兼容 numpy 2

[tool.poetry.group.dev.dependencies]
pytest = "^8.0"
black = "^24.0"
ruff = "^0.5"

# PyTorch 需要特殊 source
[[tool.poetry.source]]
name = "pytorch"
url = "https://download.pytorch.org/whl/cu121"
priority = "explicit"

# 脚本入口
[tool.poetry.scripts]
train = "my_project.train:main"
serve = "my_project.api:main"

[build-system]
requires = ["poetry-core"]
build-backend = "poetry.core.masonry.api"
```

## 4. 与 AI 项目结合

### 4.1 PyTorch 在 Poetry 中的处理

```bash
# 问题：PyTorch 不在 PyPI，而在自己的索引
# 解决：用 source 配置

# 方式一：pip install 到 Poetry 环境
poetry run pip install torch --index-url https://download.pytorch.org/whl/cu121

# 方式二：poetry add 指定 source（推荐）
poetry source add pytorch https://download.pytorch.org/whl/cu121
poetry add --source pytorch torch torchvision

# 方式三：requirements.txt + Poetry 混合
# pyproject.toml 管理纯 Python 包
# requirements-torch.txt 管理 PyTorch/CUDA
```

### 4.2 Docker 中使用 Poetry

```dockerfile
FROM python:3.12-slim

WORKDIR /app
RUN pip install poetry

COPY pyproject.toml poetry.lock ./
RUN poetry config virtualenvs.create false \
    && poetry install --only main --no-interaction

COPY . .
CMD ["python", "main.py"]
```

## 核心要点回顾

- Poetry = pip + venv + pip-tools + setuptools 四合一的现代方案
- `pyproject.toml` 统一配置，`poetry.lock` 精确锁定
- PyTorch/CUDA 需单独配置 source（不在 PyPI）
- 重度 AI 项目（conda 环境管理 + pip 装包）仍是最稳方案
- Docker 中 Poetry 需 `config virtualenvs.create false`

## 参考资料

1. Poetry 官方文档 — python-poetry.org
