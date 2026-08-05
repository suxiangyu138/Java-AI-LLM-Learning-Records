# 07 - Python AI 项目工程化模板

> 🎯 从零搭建一个生产级 Python AI 项目的完整模板 — 技术栈选型、pyproject.toml 统一配置、目录结构、CI/CD、日志/监控/安全。面向 LLM API 调用 + RAG + Agent 场景

---

## 目录

1. [技术栈全览](#1-技术栈全览)
2. [pyproject.toml 配置模板](#2-pyprojecttoml-配置模板)
3. [目录结构](#3-目录结构)
4. [CI/CD 模板](#4-cicd-模板)
5. [Docker 模板](#5-docker-模板)
6. [日志与监控](#6-日志与监控)

---

## 1. 技术栈全览

```text
Python AI 项目 2026 标准技术栈：

包管理      → uv（替代 pip/Poetry/pip-tools 全部）
Lint/格式化 → Ruff（替代 flake8/black/isort，Rust 实现 10-100x 快）
类型检查    → Pyright（VS Code 默认）或 mypy
测试        → pytest + pytest-asyncio + DeepEval (LLM 评估)
API 框架    → FastAPI（异步+自动文档，LLM 服务首选）
LLM SDK     → openai / anthropic（都用 async 客户端）
Agent 框架  → LangGraph / CrewAI（按场景，见 01 章）
RAG         → LlamaIndex / LangChain RAG + pgvector/Qdrant
推理引擎    → vLLM（如需自托管模型）或直接调 API
可观测      → Phoenix / OpenTelemetry
CI/CD       → GitHub Actions + setup-uv
```

---

## 2. pyproject.toml 配置模板

```toml
[project]
name = "my-ai-app"
version = "0.1.0"
requires-python = ">=3.12"
dependencies = [
    "openai>=1.0",
    "anthropic>=0.40",
    "fastapi>=0.115",
    "uvicorn[standard]>=0.30",
    "pydantic>=2.0",
    "httpx>=0.27",
    "python-dotenv>=1.0",
]

[project.optional-dependencies]
dev = [
    "ruff>=0.8",
    "pyright>=1.1",
    "pytest>=8.0",
    "pytest-asyncio>=0.24",
    "deepeval>=1.0",
]

[tool.ruff]
line-length = 100
target-version = "py312"

[tool.ruff.lint]
select = ["E", "F", "I", "N", "W", "UP"]  # pycodestyle + pyflakes + isort + pep8

[tool.pytest.ini_options]
asyncio_mode = "auto"
testpaths = ["tests"]

[[tool.uv.index]]
name = "tsinghua"
url = "https://pypi.tuna.tsinghua.edu.cn/simple"
default = true
```

---

## 3. 目录结构

```text
my-ai-app/
├── pyproject.toml            # 唯一配置文件（包管理+lint+测试+类型）
├── uv.lock                   # 锁文件（提交到 Git）
├── .env.example              # 环境变量模板
├── .gitignore
├── src/
│   ├── __init__.py
│   ├── main.py               # FastAPI 入口
│   ├── config.py             # 配置加载（环境变量+pydantic model）
│   ├── models/               # Pydantic 数据模型
│   ├── clients/              # LLM 客户端封装（OpenAI/Claude/DeepSeek）
│   ├── agent/                # Agent 逻辑
│   │   ├── tools.py          # MCP 工具/Function Calling
│   │   └── graph.py          # LangGraph 有向图
│   ├── rag/                  # RAG 管道
│   │   ├── loader.py         # 文档加载
│   │   ├── splitter.py       # 文档分块
│   │   └── retriever.py      # 检索
│   └── utils/                # 工具函数
│       ├── async_utils.py    # semaphore/timeout 等
│       └── logging.py
├── tests/
│   ├── test_agent.py
│   ├── test_rag.py           # 含 RAGAS/DeepEval 评估
│   └── conftest.py           # fixtures
└── .github/workflows/
    └── ci.yml
```

---

## 4. CI/CD 模板

```yaml
name: CI
on: [push, pull_request]
jobs:
  quality:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: astral-sh/setup-uv@v3     # uv 官方 action
      - run: uv sync
      - run: uv run ruff check .         # Lint
      - run: uv run pyright src/         # 类型检查
      - run: uv run pytest               # 单元测试
      - run: uv run deepeval test run    # LLM 评估（CI 中可选，较耗时）
        env:
          OPENAI_API_KEY: ${{ secrets.OPENAI_API_KEY }}
```

---

## 5. Docker 模板

```dockerfile
FROM python:3.12-slim
WORKDIR /app

# 安装 uv
COPY --from=ghcr.io/astral-sh/uv:latest /uv /usr/local/bin/uv

COPY pyproject.toml uv.lock ./
RUN uv sync --frozen --no-dev

COPY src/ ./src/
CMD ["uv", "run", "uvicorn", "src.main:app", "--host", "0.0.0.0", "--port", "8000"]
```

---

## 6. 日志与监控

```python
# 结构化日志（JSON 格式，方便日志平台解析）
import structlog
logger = structlog.get_logger()
logger.info("llm_call", model="claude-sonnet-4-5", tokens=1500, latency_ms=2300)

# 关键指标埋点（用 phoenix 或手动记录）
#   每次 LLM 调用：模型、输入/输出 Token、延迟、是否缓存命中
#   每次 RAG 检索：检索片段数、检索延迟、是否命中
#   每次 Agent 任务：步数、工具调用次数、是否成功
```

---

> 🎯 **核心要点**：2026 年 Python AI 工程化标准 = **uv 管项目 + pyproject.toml 收敛全配置 + Ruff 管代码质量 + FastAPI 做服务 + structlog 做日志 + Phoenix 做追踪**。关键动作：锁文件提交 Git（可复现构建）、`uv sync --frozen` 保证 CI 一致性、评估指标纳入 CI（RAGAS/DeepEval 回归）。

**返回总览**：[00-Python生态知识体系总览](00-Python生态知识体系总览.md)
