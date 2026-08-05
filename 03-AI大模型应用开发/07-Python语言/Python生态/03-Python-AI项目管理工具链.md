# 03 - Python AI 项目管理工具链

> 🎯 **2026 年结论：uv 是 Python AI 项目的首选包管理器** — Rust 编写、10-100 倍加速、OpenAI 已收购其开发团队。一个工具替代 pip/pip-tools/pipx/poetry/pyenv/virtualenv/twine 共 7 个工具

---

## 目录

1. [uv vs Poetry vs pip vs Conda](#1-uv-vs-poetry-vs-pip-vs-conda)
2. [uv 详解：速度、用法、AI 项目实践](#2-uv-详解速度用法ai-项目实践)
3. [AI 项目工程化完整栈](#3-ai-项目工程化完整栈)
4. [中国源配置](#4-中国源配置)
5. [版本管理与 CI/CD](#5-版本管理与-cicd)

---

## 1. uv vs Poetry vs pip vs Conda

| 维度 | **uv** | Poetry | pip | Conda |
|------|--------|--------|-----|-------|
| 语言 | **Rust**（单二进制） | Python | Python | Python/C |
| 解析速度 | **10-100 倍** | 中等 | 慢 | 慢 |
| 锁文件 | ✅ uv.lock | ✅ poetry.lock | ❌（需 pip-tools） | ✅ environment.yml |
| Python 版本管理 | ✅ 内置 | ❌（需 pyenv） | ❌ | ✅ |
| 虚拟环境 | ✅ 自动 | ✅ 自动 | ❌（需手动 venv） | ✅ |
| 发布 | ✅ `uv publish` | ✅ | ❌（需 twine） | ❌ |
| 替代工具数 | **7 个** | 3-4 个 | 仅基础 | 多 |
| CUDA/非 PyPI | ❌（仅 PyPI） | ❌ | ❌ | **✅ 最佳** |
| 适用 | **新项目/AI 应用首选** | 库发布 | 遗留项目 | 深度学习/CUDA |

**实测数据（AI 依赖场景）：**
- Transformers 项目全部依赖：uv 冷 7.48s vs Poetry 47.91s（**快 6 倍**）；**热缓存 0.14s** vs Poetry 4.32s
- vLLM+LangChain+Streamlit（1200 依赖）：uv 冷 18s vs pip 3m20s；热 1.1s vs 1m05s
- CI 管道：从 pip+Poetry 的 8 分钟降至 **uv 2 分钟内**

---

## 2. uv 详解：速度、用法、AI 项目实践

### 2.1 快速上手

```bash
# 安装（macOS/Linux）
curl -LsSf https://astral.sh/uv/install.sh | sh

# 创建 AI 项目（一行）
uv init my-ai-app && cd my-ai-app
uv add openai anthropic langchain vllm
uv run main.py          # 自动创建/激活虚拟环境
```

### 2.2 关键命令

```bash
uv init                    # 初始化项目（生成 pyproject.toml）
uv add <package>           # 添加依赖（自动解析+lock+安装）
uv remove <package>        # 移除依赖
uv sync                    # 按 lock 文件同步环境（CI 必备）
uv run <script>            # 在项目环境中运行脚本
uv lock --upgrade-package  # 升级单个包
uv python install 3.12     # uv 管理 Python 版本
uv tool install <tool>     # 安装全局工具（替代 pipx）
```

### 2.3 AI 项目最佳实践

```text
纯 LLM 应用（调 API）：uv 一步到位
  uv add openai anthropic → 写代码 → uv run

深度学习/CUDA 项目：
  方案：Conda 创建环境（装 CUDA toolkit）→ uv pip install（装 PyTorch 等）
  趋势：PyTorch 2.6+ 逐步脱离 conda，vLLM 官方已默认推荐 uv
```

---

## 3. AI 项目工程化完整栈

```text
一个 pyproject.toml 收敛所有配置（无需 setup.py/setup.cfg/requirements.txt/...）：

[tool.uv]       → 包管理（替代 pip/Poetry）
[tool.ruff]     → Lint + 格式化（替代 flake8/black/isort）
[tool.pyright]  → 类型检查（替代 mypy，更快）
[tool.pytest]   → 测试配置

项目结构：
my-ai-app/
├── pyproject.toml        # 唯一配置文件
├── uv.lock               # 锁文件（CI 可复现）
├── src/
│   ├── agent/            # Agent 逻辑
│   ├── tools/            # MCP 工具
│   └── rag/              # RAG 管道
├── tests/
└── .github/workflows/    # CI/CD（setup-uv action）
```

---

## 4. 中国源配置

```toml
# pyproject.toml
[[tool.uv.index]]
name = "tsinghua"
url = "https://pypi.tuna.tsinghua.edu.cn/simple"
default = true           # 设为默认源

# 附加源（非 PyPI 包需要自建反代或走原站）
[[tool.uv.index]]
name = "pytorch"
url = "https://download.pytorch.org/whl/cu124"
explicit = true
```

**稳定镜像源：** 清华（最稳、推荐）> 阿里云 > 中科大 > 华为云。**豆瓣已不稳定不推荐。**

---

## 5. 版本管理与 CI/CD

```yaml
# .github/workflows/test.yml
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: astral-sh/setup-uv@v3     # 安装 uv
      - run: uv sync                     # 按 lock 文件装依赖
      - run: uv run ruff check .         # Lint
      - run: uv run pytest               # 测试
```

---

> 🎯 **核心要点**：2026 年 Python AI 项目的标准答案 = **uv 一把梭 + pyproject.toml 收敛全配置**。纯 LLM 应用直接用 uv；深度学习项目 Conda + uv 混合。uv 一个二进制替代 7 个传统工具，10-100 倍速度提升，OpenAI 已收购。

**下一模块**：[04-HuggingFace生态](04-HuggingFace生态.md) / **返回总览**：[00-生态总览](00-Python生态知识体系总览.md)
