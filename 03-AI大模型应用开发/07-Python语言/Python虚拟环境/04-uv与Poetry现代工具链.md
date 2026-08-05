# 04 - uv 与 Poetry 现代工具链

> **核心摘要**：2026 年 Python 环境管理格局已定：**uv 是新项目默认选择**（Rust 单二进制、快 10-100 倍、替代六种工具），Poetry 守住库发布与存量项目。本文拆解 uv 的三大提速来源、完整工作流、pyproject.toml 标准与锁文件机制。

> **前置阅读**：[[01-虚拟环境核心原理]]、[[02-venv与virtualenv]]

---

## 📚 目录

1. [2026 格局：为什么 uv 成为默认](#1-2026-格局为什么-uv-成为默认)
2. [uv 三大提速来源](#2-uv-三大提速来源)
3. [uv 完整工作流](#3-uv-完整工作流)
4. [pyproject.toml：PEP 621 标准](#4-pyprojecttomlpep-621-标准)
5. [锁文件：可复现的关键](#5-锁文件可复现的关键)
6. [Poetry：库发布的强者](#6-poetry库发布的强者)
7. [uv vs Poetry 选型](#7-uv-vs-poetry-选型)
8. [核心要点](#8-核心要点)

---

## 1. 2026 格局：为什么 uv 成为默认

### 1.1 uv 是什么

**uv**：Astral 团队（Ruff 的开发者）用 **Rust** 编写的单一二进制工具——**一个工具替代 pip、venv、virtualenv、pyenv、pip-tools、pipx 和 Poetry 的大部分功能**：

```text
uv 的六合一
├── pip 替代：uv pip install（快 10-100 倍）
├── venv 替代：uv venv / uv init（自动创建）
├── pyenv 替代：uv python install 3.13（自带解释器管理）
├── pip-tools 替代：uv lock（锁文件）
├── pipx 替代：uv tool install（全局 CLI 工具）
└── Poetry 替代（大部分）：uv add / uv sync / uv run
```

### 1.2 安装与验证

```bash
# Windows（PowerShell）
powershell -ExecutionPolicy ByPass -c "irm https://astral.sh/uv/install.ps1 | iex"

# Linux/macOS
curl -LsSf https://astral.sh/uv/install.sh | sh

# 验证
uv --version
```

> 🎯 **2026 共识**：新项目默认 uv——`uv init` → `uv add` → `uv sync` → `uv run` 四步闭环。

---

## 2. uv 三大提速来源

> 📊 实测：冷安装 50 个依赖 **2-5 秒**（pip 需 30-50 秒），快 **10-100 倍**。

| 提速来源 | 原理 | 效果 |
|---------|------|------|
| **① Rust 编译执行** | 无 Python 启动开销 | 命令启动毫秒级 |
| **② 全局内容寻址缓存** | 已下载的 wheel 跨项目复用 | 重复安装近零成本 |
| **③ 并行元数据拉取** | 并发解析依赖元数据 | 解析时间大幅缩短 |

```text
缓存复用示例
├── 项目 A 装过 requests
├── 项目 B 再装 → 命中全局缓存 → 秒级
└── 关键配置：uv cache dir（默认 ~/.cache/uv）
```

---

## 3. uv 完整工作流

### 3.1 新项目四步闭环

```bash
# ① 初始化项目（生成 pyproject.toml + .venv）
uv init my-project
cd my-project

# ② 添加依赖（自动解析 + 更新锁文件）
uv add requests fastapi

# ③ 开发依赖分组
uv add --dev pytest ruff

# ④ 同步环境 + 运行
uv sync
uv run python app.py
uv run pytest
```

### 3.2 常用命令速查

| 命令 | 作用 | 对应传统工具 |
|------|------|-------------|
| `uv init` | 初始化项目 | poetry new |
| `uv add <pkg>` | 添加依赖 | pip install + freeze |
| `uv remove <pkg>` | 移除依赖 | pip uninstall |
| `uv sync` | 同步环境（按锁文件） | pip install -r |
| `uv run <cmd>` | 免激活运行 | source activate |
| `uv lock` | 生成/更新锁文件 | pip-compile |
| `uv python install <ver>` | 安装 Python | pyenv install |
| `uv tool install <pkg>` | 全局 CLI 工具 | pipx install |
| `uv pip install` | pip 兼容模式 | pip |

### 3.3 Python 版本管理

```bash
# 项目指定 Python 版本（pyproject.toml）
uv python pin 3.13

# 安装指定版本（无需系统预装）
uv python install 3.12
uv python list       # 查看已管理版本

# 项目自动使用指定版本（无系统 Python 也能跑）
uv run python --version
```

> 💡 **uv python** 的价值：团队无需各自装 Python——一个 uv 命令解决解释器版本问题。

---

## 4. pyproject.toml：PEP 621 标准

### 4.1 标准配置文件

```toml
# pyproject.toml（uv 生成，PEP 621 标准）
[project]
name = "my-project"
version = "0.1.0"
description = "示例项目"
requires-python = ">=3.10"          # Python 版本约束
dependencies = [
    "fastapi>=0.110",
    "requests>=2.32",
]                                   # 运行依赖（PEP 621 标准表）

[project.optional-dependencies]
dev = ["pytest>=8.0", "ruff"]       # 开发依赖组

[dependency-groups]
dev = ["pytest>=8.0"]               # uv 的开发依赖组（uv 特有）

[build-system]
requires = ["hatchling"]
build-backend = "hatchling.build"
```

### 4.2 pyproject.toml 的意义

| 价值 | 说明 |
|------|------|
| **统一声明** | 依赖声明在工具间可迁移（uv/Poetry/pdm/hatch 都读 `[project]`） |
| **单一事实源** | 元数据 + 依赖 + 构建配置一个文件 |
| **标准演进** | PEP 621 是 PyPA 官方标准 |

> ⚠️ **2026 提醒**：新项目不用 pyproject.toml 会积累技术债——requirements.txt 时代正在结束。但**锁文件不可互用**（uv.lock vs poetry.lock 格式不同）。

---

## 5. 锁文件：可复现的关键

### 5.1 为什么锁文件是核心

```text
requirements.txt vs 锁文件
├── requirements.txt：版本列表（flask==3.0.3）
│   → 但传递依赖未锁定 → 环境间可能有差异
└── uv.lock：完整依赖树 + 精确版本 + 哈希校验
    → 100% 可复现（同锁文件 = 同环境）
```

### 5.2 uv.lock 特点

| 特点 | 说明 |
|------|------|
| **跨平台** | 同一锁文件支持 Windows/Linux/macOS |
| **哈希校验** | 每个包带哈希，防篡改 |
| **完整依赖树** | 传递依赖全部锁定 |
| **确定性构建** | `uv sync --frozen` 严格按锁文件 |

```bash
# 生产部署：严格按锁文件（不重新解析）
uv sync --frozen

# CI 中禁止锁文件变更时静默更新
uv sync --frozen --no-dev
```

> 🎯 **铁律**：**uv.lock 必须提交版本控制**——它是环境可复现的唯一保证。

---

## 6. Poetry：库发布的强者

### 6.1 Poetry 的定位（2026）

| 维度 | 说明 |
|------|------|
| 优势 | 成熟锁文件（poetry.lock）、完善 CLI、dev/test 依赖组、**内置 PyPI 发布** |
| 局限 | 冷装比 uv 慢约 10 倍、不管理 Python 解释器、大型依赖图解析曾有瓶颈 |
| 2026 原则 | **现有 Poetry 项目 CI 正常就不迁移**；发布 PyPI 库仍可用 |

### 6.2 完整工作流

```bash
# 创建
poetry new my-lib
cd my-lib
poetry add requests

# 激活/运行
poetry install
poetry run python -m my_lib

# 发布到 PyPI（库项目的强项）
poetry build
poetry publish
```

### 6.3 何时保持 Poetry

```text
保持 Poetry 的条件（满足任一即不动）
├── 现有项目 CI 正常（迁移风险 > 速度收益）
├── 需要成熟的 PyPI 发布流程
├── 团队已熟悉 Poetry 生态
└── 项目依赖复杂且 poetry.lock 已稳定
→ 原则：工具迁移的唯一理由应是"解决实际问题"，不是"追新"
```

---

## 7. uv vs Poetry 选型

| 维度 | uv | Poetry |
|------|:---:|:---:|
| 安装速度 | **快 10-100 倍** | 慢 |
| Python 管理 | ✅（uv python） | ❌ |
| 锁文件 | ✅（uv.lock 跨平台） | ✅（poetry.lock） |
| 开发/测试依赖组 | ✅ | ✅ |
| PyPI 发布 | ✅ | ✅✅（成熟） |
| 生态成熟度 | 新（增长最快） | 成熟 |
| 适用 | **新项目（2026 默认）** | 存量 + 库发布 |

```text
选型一句话
├── 新项目 → uv
├── 现有 Poetry 健康项目 → 保持
├── 发布 PyPI 库 → uv 或 Poetry 都行（Poetry 流程更成熟）
├── CI → uv sync --frozen（提速 30-90 秒）
└── 全 Python 工具 → uv tool install
```

> 🎯 **总结**：uv 是 2026 年的默认答案，Poetry 是存量与发布的稳妥答案——二者共享 pyproject.toml 标准，切换成本被标准化降到最低。

---

## 8. 核心要点

> 🎯 **核心要点**：
> 1. uv = Rust 六合一工具（pip/venv/pyenv/pip-tools/pipx/poetry 大部分功能），快 10-100 倍
> 2. 三大提速：Rust 执行 + 全局缓存 + 并行解析；新项目四步闭环（init/add/sync/run）
> 3. pyproject.toml（PEP 621）是统一声明标准；**uv.lock 提交版本控制**，`uv sync --frozen` 确定性构建
> 4. Poetry 守存量与 PyPI 发布；选型原则：新项目 uv、健康项目不动、锁文件不可互用

---

**下一模块**：[05-多工具协同与生产实践](05-多工具协同与生产实践.md) | **返回总览**：[00-Python虚拟环境知识体系总览](00-Python虚拟环境知识体系总览.md)
