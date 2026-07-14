# 附录 A：开发环境与工具链

> **目标**：搭建高效的 Python 开发环境，让工具为你服务
> **策略**：VSCode 为主 IDE，venv/conda 管环境，Jupyter 做探索

---

## 📌 章节定位

工具链是生产力的基础。对习惯了 IntelliJ IDEA + Maven/Gradle 的 Java 开发者来说，Python 的工具生态更轻量但同样丰富。本章帮你用最短时间搭出顺手的环境。

---

## 🎯 核心章节

### 1. Python 安装与版本管理

```bash
# 方案 A：官方安装（推荐入门）
# 1. 访问 python.org/downloads 下载安装包
# 2. 安装时勾选 "Add Python to PATH"
# 3. 验证
python --version          # Python 3.11.x
pip --version             # pip 23.x

# 方案 B：pyenv（推荐多版本切换，类似 Java 的 SDKMAN）
# Windows: pyenv-win
# macOS: brew install pyenv
# Linux: curl https://pyenv.run | bash

pyenv install 3.11.9      # 安装指定版本
pyenv global 3.11.9        # 设置全局默认
pyenv local 3.12.3         # 当前目录使用 3.12

# 方案 C：conda（数据科学/AI 方向推荐）
# conda 不仅可以管理 Python 版本，还能管理 CUDA、cuDNN 等非 Python 依赖
# 推荐安装 Miniconda（轻量版）或 Miniforge（社区维护）
conda create -n ai python=3.11
conda activate ai
conda install pytorch torchvision torchaudio pytorch-cuda=12.1 -c pytorch -c nvidia
```

### 2. VSCode 配置（⭐ 推荐 IDE）

#### 必装扩展

| 扩展 | 用途 |
|------|------|
| **Python** (ms-python.python) | 语法高亮、智能提示、调试、测试 |
| **Pylance** (ms-python.vscode-pylance) | 类型检查、自动导入、参数提示 |
| **Jupyter** (ms-toolsai.jupyter) | Notebook 支持 |
| **Ruff** (charliermarsh.ruff) | ⭐ 极速 Linting + Formatting |
| **Even Better TOML** (tamasfe.even-better-toml) | pyproject.toml 语法支持 |

#### settings.json 推荐配置

```json
{
    // Python 语言服务器
    "python.languageServer": "Pylance",
    "python.analysis.typeCheckingMode": "basic",
    "python.analysis.autoImportCompletions": true,

    // 格式化 —— 保存时自动用 Ruff 格式化
    "[python]": {
        "editor.defaultFormatter": "charliermarsh.ruff",
        "editor.formatOnSave": true,
        "editor.codeActionsOnSave": {
            "source.fixAll.ruff": "explicit",
            "source.organizeImports.ruff": "explicit"
        }
    },

    // 终端默认使用虚拟环境
    "python.terminal.activateEnvironment": true,

    // 测试框架
    "python.testing.pytestEnabled": true,
    "python.testing.unittestEnabled": false,
    "python.testing.cwd": "${workspaceFolder}",

    // Notebook
    "jupyter.askForKernelRestart": false
}
```

#### launch.json 调试配置

```json
{
    "version": "0.2.0",
    "configurations": [
        {
            "name": "Python: 当前文件",
            "type": "debugpy",
            "request": "launch",
            "program": "${file}",
            "console": "integratedTerminal",
            "env": {
                "PYTHONPATH": "${workspaceFolder}/src"
            }
        },
        {
            "name": "FastAPI 调试",
            "type": "debugpy",
            "request": "launch",
            "module": "uvicorn",
            "args": [
                "main:app",
                "--reload",
                "--host", "0.0.0.0",
                "--port", "8000"
            ],
            "jinja": true,
            "justMyCode": true
        },
        {
            "name": "当前文件（带参数）",
            "type": "debugpy",
            "request": "launch",
            "program": "${file}",
            "args": ["--verbose", "--output", "./out"],
            "console": "integratedTerminal"
        }
    ]
}
```

### 3. 代码质量工具

```bash
# --- Ruff（⭐ 推荐，替代 flake8 + isort + black）---
# 极速（Rust 实现），零配置开箱即用
pip install ruff

ruff check .                          # 检查代码问题
ruff check --fix .                    # 自动修复
ruff format .                         # 格式化代码
ruff format --check .                 # 只检查不修改（CI 中使用）

# --- pyproject.toml 中的 Ruff 配置 ---
# [tool.ruff]
# line-length = 100
# target-version = "py311"
#
# [tool.ruff.lint]
# select = ["E", "F", "I", "N", "W", "UP"]  # 启用的规则集
# ignore = ["E501"]                          # 忽略行长度
#
# [tool.ruff.lint.isort]
# known-first-party = ["my_package"]
#
# [tool.ruff.format]
# quote-style = "double"

# --- MyPy（静态类型检查）---
pip install mypy
mypy src/                             # 检查类型注解
mypy --strict src/                    # 严格模式

# --- pytest（⭐ 测试框架）---
pip install pytest pytest-cov

# 运行测试
pytest                                # 自动发现 test_*.py
pytest -v                             # 详细输出
pytest -x                             # 遇到第一个失败就停止
pytest -k "test_api"                  # 按名称过滤
pytest --cov=src --cov-report=html    # 生成覆盖率报告

# conftest.py 示例（共享 fixtures）
# import pytest
#
# @pytest.fixture
# def sample_data():
#     return {"name": "test", "value": 42}
#
# @pytest.fixture(scope="session")
# def db_connection():
#     conn = create_connection()
#     yield conn
#     conn.close()
```

### 4. Jupyter Notebook 工作流

Jupyter 是 AI/数据科学方向的交互式编程环境，适合数据探索和快速实验。

```bash
# 安装（VSCode 中可直接使用，无需安装 Jupyter Lab）
pip install jupyter ipykernel

# 在 VSCode 中使用：
# 1. 创建 .ipynb 文件
# 2. 右上角选择 Python 解释器（.venv）
# 3. 按 Shift+Enter 执行单个 Cell

# 将虚拟环境注册为 Jupyter Kernel
python -m ipykernel install --user --name=myenv --display-name="Python (myenv)"
```

```python
# Notebook 中的实用魔法命令
%timeit sum(range(1000))                         # 计时
%time func()                                     # 单次计时
%matplotlib inline                                # 图表内嵌显示
%load_ext autoreload                              # 自动重载模块
%autoreload 2

# 快速查看 DataFrame
# df.head()        前 5 行
# df.describe()    统计摘要
# df.info()        列信息

# 转换 Notebook → Python 脚本
# jupyter nbconvert --to script notebook.ipynb
```

### 5. Git 配置（Python 项目）

```gitignore
# .gitignore —— Python 项目标准模板

# 虚拟环境
.venv/
venv/
env/
ENV/

# Python 缓存
__pycache__/
*.py[cod]
*$py.class
*.egg-info/
dist/
build/
*.egg

# Jupyter
.ipynb_checkpoints/
*.ipynb_checkpoints

# 环境变量（包含密钥）
.env
.env.local

# IDE
.vscode/settings.json    # 保留 launch.json 和 extensions.json
.idea/

# 模型文件和大文件
*.pth
*.bin
*.safetensors
models/
checkpoints/

# 操作系统
.DS_Store
Thumbs.db

# 项目特定
chroma_db/
*.log
```

### 6. CI/CD 配置示例

```yaml
# .github/workflows/python-ci.yml
name: Python CI

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

jobs:
  test:
    runs-on: ubuntu-latest
    strategy:
      matrix:
        python-version: ["3.10", "3.11", "3.12"]

    steps:
      - uses: actions/checkout@v4

      - name: Setup Python
        uses: actions/setup-python@v5
        with:
          python-version: ${{ matrix.python-version }}
          cache: "pip"

      - name: Install dependencies
        run: |
          pip install --upgrade pip
          pip install ruff pytest pytest-cov
          pip install -e ".[dev]"

      - name: Lint
        run: ruff check .

      - name: Type check
        run: mypy src/ --ignore-missing-imports

      - name: Test
        run: pytest --cov=src --cov-report=xml

      - name: Upload coverage
        uses: codecov/codecov-action@v3
        with:
          file: ./coverage.xml
```

---

## 🔧 工具速查表

| 需求 | 工具 | 替代方案 |
|------|------|---------|
| 虚拟环境 | `venv`（内置） | `conda`, `poetry`, `pipenv` |
| 包安装 | `pip` | `conda install`, `poetry add` |
| 代码格式化 | `ruff format` | `black` |
| 代码检查 | `ruff check` | `flake8`, `pylint` |
| 类型检查 | `mypy` | `pyright`, `pyre` |
| 测试 | `pytest` | `unittest`, `nose2` |
| 覆盖率 | `pytest-cov` | `coverage.py` |
| 依赖锁定 | `pip freeze` | `pip-tools`, `poetry.lock` |
| 构建打包 | `setuptools` + `build` | `poetry build`, `hatch` |
| 发布 | `twine` | `poetry publish` |
| Notebook | Jupyter (VSCode 内置) | JupyterLab, Google Colab |
| 调试 | VSCode debugger | `pdb`, `ipdb`, `pudb` |
| 性能分析 | `cProfile` + `snakeviz` | `py-spy`, `scalene` |
| API 文档 | FastAPI 自动生成 | Flask + apispec |

---

> **上一阶段** ← [05-项目实战与 Java 联动](05-项目实战与Java联动.md)
> **下一阶段** → [07-资源索引与速查](07-资源索引与速查.md)
