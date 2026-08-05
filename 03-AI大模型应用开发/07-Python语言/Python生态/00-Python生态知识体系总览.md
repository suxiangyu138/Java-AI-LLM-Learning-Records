# 00 - Python 生态知识体系总览

> 🎯 AI 时代的 Python 生态全景 — 不做语法教程（已有 `07-Python语言`/`Python高级语法`），专注 AI/LLM 工程化工具链：Agent 框架选型、模型推理引擎、项目管理（uv）、HuggingFace 生态、异步并发模式、评估体系

---

## 目录

1. [知识全景](#1-知识全景)
2. [文件导航](#2-文件导航)
3. [与现有 Python 体系的关系](#3-与现有-python-体系的关系)
4. [2026 Python AI 生态趋势](#4-2026-python-ai-生态趋势)

---

## 1. 知识全景

```
Python 生态体系（8个文件 — AI/LLM 工程化工具链）
│
├── 🤖 Agent 框架（01）
│   └── 01-AI-Agent开发框架对比.md      # LangChain/LangGraph/LlamaIndex/CrewAI/AutoGen等10+框架
│
├── 🚀 推理引擎（02）
│   └── 02-模型推理与服务引擎.md        # vLLM/TGI/Ollama/Triton/推理优化技术
│
├── 📦 项目管理（03）
│   └── 03-Python-AI项目管理工具链.md   # uv vs Poetry vs pip vs Conda + AI项目最佳实践
│
├── 🤗 HF 生态（04）
│   └── 04-HuggingFace生态.md           # Transformers/Datasets/Diffusers/Spaces/Hub
│
├── ⚡ 异步并发（05）
│   └── 05-Python异步编程与AI并发模式.md  # asyncio + LLM并发调用 + 流式处理
│
├── 📊 评估可观测（06）
│   └── 06-AI应用评估与可观测性.md       # RAGAS/DeepEval/LangSmith/OpenTelemetry
│
├── 🏗️ 工程模板（07）
│   └── 07-Python-AI项目工程化模板.md    # 从零搭AI项目：技术栈/目录结构/CI/CD
│
└── 📌 00-Python生态知识体系总览.md       # ← 本文件
```

---

## 2. 文件导航

| # | 文件 | 核心内容 | 级别 |
|---|------|----------|:---:|
| 00 | 生态总览 | 全景导航 + 趋势 + 分工 + 2026 生态趋势 | — |
| 01 | Agent 框架对比 | 10+ 框架横评 + 选型决策树 | ⭐⭐⭐ |
| 02 | 推理与服务引擎 | vLLM/TGI/Ollama 部署方案 + PagedAttention | ⭐⭐ |
| 03 | AI 项目管理工具链 | uv 10-100 倍加速/poetry/pip/Conda 选型 | ⭐⭐⭐ |
| 04 | HuggingFace 生态 | Transformers/Datasets/Spaces/Hub 工作流 | ⭐⭐ |
| 05 | 异步并发模式 | asyncio + LLM 并发 + 流式 + semaphore 限流 | ⭐⭐ |
| 06 | 评估与可观测性 | RAGAS/DeepEval/LangSmith/Phoenix 对比 | ⭐⭐ |
| 07 | 工程化模板 | pyproject.toml 收敛/Ruff/CI/CD/目录结构 | ⭐⭐ |

---

## 3. 与现有 Python 体系的关系

| 已有系统 | 做什么 | 本体系补充什么 |
|----------|--------|----------------|
| `07-Python语言` | Python 基础语法 | 不做基础 → 做 AI 工程化工具链 |
| `Python高级语法` | 高级特性 | 同上 |
| `FastAPI/` | Web 框架 | 05 章补充异步并发与 LLM 集成的 FastAPI 实践 |
| `pip/` | 包管理基础 | 03 章做 uv/Poetry/Conda 的 AI 项目选型对比 |
| `Transformer/` | Transformers 库 | 04 章做 HF 全生态（Datasets/Spaces/Diffusers/Hub） |

> 💡 **定位**：Python 基础语法和高级特性专题已覆盖"写 Python 代码"的能力；本体系补充"用 Python 做 AI 工程"的工具链与最佳实践。

---

## 4. 2026 Python AI 生态趋势

| 趋势 | 说明 |
|------|------|
| **uv 取代 pip/Poetry** | Rust 编写，10-100 倍加速，OpenAI 2026.03 收购 Astral（uv 作者），成为 AI 项目事实标准 |
| **MCP 协议标准化** | Anthropic 2024 年提出，2026 年几乎所有 Agent 框架已支持 |
| **"去 LangChain 化"** | 模型原生能力增强，开发者倾向原生 Function Calling + 轻量组合，框架"胶水"价值降低 |
| **Agent 框架收敛** | 微软合并 AutoGen+Semantic Kernel；OpenAI 推出 Agents SDK；Google 推出 ADK |
| **推理支出超训练支出** | vLLM/SGLang/TGI 等推理引擎成为 AI 基础设施层核心 |
| **LLM 评估体系建立** | RAGAS/DeepEval 生态成熟，从"感觉好不好"到"指标化评估" |

---

> 🎯 **核心要点**：2026 年用 Python 做 AI 工程的标准栈 = **uv 管项目 + Pydantic 管类型 + LangGraph/CrewAI 编排 Agent + vLLM 跑推理 + asyncio 处理并发 + RAGAS 做评估**。本体系每个模块都可独立作为选型参考。

**下一模块**：[01-AI-Agent开发框架对比](01-AI-Agent开发框架对比.md)
