# AI 编程工具全家桶 — 使用指南

> **安装日期**：2026-06-24 | **API**：DeepSeek

---

## 配置信息

| 项目 | 值 |
|------|-----|
| API Key | `[REDACTED_DEEPSEEK_API_KEY]` |
| 环境变量 | `DEEPSEEK_API_KEY` |
| Aider 模型 | `deepseek/deepseek-chat` |

---

## 目录

| 分类 | 工具 |
|------|------|
| AI IDE | Cursor / Windsurf / Claude Code |
| VS Code 扩展 | Copilot / Augment Code / 通义灵码 |
| Python 工具 | Aider / OpenAI Agents SDK / LangChain / LangGraph / CrewAI / AutoGen / Pydantic AI |
| Node.js 工具 | Mastra / Smithery / Repomix / Playwright MCP / Anthropic SDK |
| 效率工具 | uv / pipx |
| 云服务 | Devin |

---

## 一、AI IDE

### 1. Claude Code (v2.1.158) — Anthropic 官方终端 AI Agent

```bash
# 启动
claude
```

> ⚠️ **注意**：Claude Code 原生只支持 Anthropic API，不直接支持 DeepSeek。如需使用 DeepSeek，请用 Aider 或 LangChain。

### 2. Cursor — AI-first IDE

| 项目 | 值 |
|------|-----|
| 位置 | `D:\cursor\Cursor.exe` |
| 快捷键 | `Ctrl+K`（编辑）、`Ctrl+L`（Chat）、`Ctrl+I`（Composer/Agent） |

**配置 DeepSeek**：`Settings → Models → 添加 DeepSeek`

| 参数 | 值 |
|------|-----|
| Provider | OpenAI Compatible |
| Base URL | `https://api.deepseek.com` |
| API Key | `[REDACTED_DEEPSEEK_API_KEY]` |

### 3. Windsurf — Codeium 出品

| 项目 | 值 |
|------|-----|
| 位置 | `D:\Windsurf\Windsurf.exe` |
| 快捷键 | `Ctrl+I`（Cascade）、`Ctrl+K`（行内编辑） |

---

## 二、VS Code 扩展

### 4. GitHub Copilot — 代码补全 + Chat

| 快捷键 | 功能 |
|--------|------|
| `Ctrl+Shift+I` | Chat |
| `Tab` | 接受补全 |
| `Ctrl+→` | 逐词接受 |

> ℹ️ 需要 GitHub 账号 + 订阅。

### 5. Augment Code (v0.859.7) — 大上下文 AI

| 项目 | 值 |
|------|-----|
| 快捷键 | `Ctrl+Shift+A` |

### 6. 通义灵码 (v2.6.2) — 阿里云，中文生态

> ℹ️ 需要阿里云账号登录。

---

## 三、Python AI 工具（全部已配置 DeepSeek）

### 7. Aider (v0.86.2) — 终端 AI 结对编程

```bash
# 启动
aider

# 使用推理模型
aider --model deepseek/deepseek-reasoner
```

| 项目 | 值 |
|------|-----|
| 模型 | `deepseek/deepseek-chat`（已配置） |
| 配置文件 | `D:\AI-Tools\.aider.conf.yml` |
| 测试状态 | ✅ 已验证连通（费用 $0.00065/次） |

**常用命令**：

| 命令 | 说明 |
|------|------|
| `aider` | 启动 |
| `aider src/` | 添加文件 |
| `/add file.py` | 添加文件到上下文 |
| `/drop file.py` | 移除文件 |
| `/diff` | 查看改动 |
| `/undo` | 撤销 |
| `/commit` | 提交 |
| `/clear` | 清除上下文 |

### 8. OpenAI Agents SDK (v0.10.5)

```python
from openai import AsyncOpenAI
from agents import Agent, Runner
import agents

agents.set_default_openai_client(AsyncOpenAI(
    api_key="[REDACTED_DEEPSEEK_API_KEY]",
    base_url="https://api.deepseek.com"
))
```

### 9. LangChain (v1.3.11)

```python
from langchain.chat_models import init_chat_model

model = init_chat_model(
    "deepseek-chat",
    openai_api_key="[REDACTED_DEEPSEEK_API_KEY]",
    openai_api_base="https://api.deepseek.com"
)
```

### 10. LangGraph (v1.2.6) — Agent 工作流编排

与 LangChain 共用模型，配置同上。

### 11. CrewAI (v0.95.0) — 多 Agent 协作

```python
import os
os.environ["OPENAI_API_KEY"] = "[REDACTED_DEEPSEEK_API_KEY]"
os.environ["OPENAI_API_BASE"] = "https://api.deepseek.com"

# CrewAI 通过 litellm 支持 DeepSeek
# 在 Agent 中指定: llm="deepseek/deepseek-chat"
```

### 12. AutoGen (v0.7.5) — 微软多 Agent 框架

```python
import autogen_agentchat
import autogen_core

from autogen_ext.models.openai import OpenAIChatCompletionClient

client = OpenAIChatCompletionClient(
    model="deepseek-chat",
    api_key="[REDACTED_DEEPSEEK_API_KEY]",
    base_url="https://api.deepseek.com"
)
```

### 13. Pydantic AI (v1.22.0) — 结构化 Agent

```python
from pydantic_ai import Agent

agent = Agent(
    "openai:deepseek-chat",
    base_url="https://api.deepseek.com",
    api_key="[REDACTED_DEEPSEEK_API_KEY]"
)
```

---

## 四、Node.js 全局工具

### 14. Mastra (v1.15.0) — TS Agent 框架

```bash
npx mastra create my-app
```

### 15. Smithery CLI (v4.11.1) — MCP 生态

```bash
npx @smithery/cli search 关键词    # 搜索
npx @smithery/cli install 名称    # 安装
```

### 16. Repomix (v1.15.0) — 代码库打包

```bash
cd 项目 && npx repomix
# 输出: repomix-output.txt
```

### 17. Playwright MCP (v0.0.76) — 浏览器自动化

已在 Claude Code 中可用。

### 18. Anthropic SDK — Claude API (TypeScript)

---

## 五、效率工具

### 19. uv (v0.11.23) — 超快 Python 包管理器

```bash
uv pip install 包名    # 替代 pip
uv venv                # 创建虚拟环境
uv add 包名            # 添加依赖
```

### 20. pipx (v1.14.1) — Python 工具隔离

---

## 六、云服务

### 21. Devin — 全自主 AI 工程师

- 地址：[https://devin.ai](https://devin.ai)

---

## DeepSeek 配置汇总

| 项目 | 值 |
|------|-----|
| 环境变量 | `DEEPSEEK_API_KEY=[REDACTED_DEEPSEEK_API_KEY]` |
| Base URL | `https://api.deepseek.com` |
| 可用模型 | `deepseek-chat`（通用）/ `deepseek-reasoner`（推理） |

**已配置工具状态**：

| 工具 | 状态 | 说明 |
|------|------|------|
| Aider | ✅ 直接可用 | — |
| LangChain | ✅ 代码配置 | — |
| CrewAI | ✅ 代码配置 | — |
| AutoGen | ✅ 代码配置 | — |
| Pydantic AI | ✅ 代码配置 | — |
| OpenAI SDK | ✅ 代码配置 | — |
| Claude Code | ⚠️ 原生不支持 | 用 Aider 替代 |
| Cursor | ⚠️ 需手动配置 | Settings 中添加 |

---

## 推荐用法

| 场景 | 推荐工具 |
|------|----------|
| 快速开发 | Cursor + DeepSeek（代码补全+Chat） |
| 结对编程 | Aider（终端里最爽） |
| 复杂任务 | Claude Code（用 Anthropic 原生） |
| 构建 Agent | Pydantic AI（简单）/ LangChain+LangGraph（复杂） |
| 多 Agent | CrewAI（角色分工）/ AutoGen（微软生态） |
| 代码分析 | Repomix 打包 → 喂给 Aider |
| 浏览器操作 | Playwright MCP |

---

> **文档版本**：v1.1 | **生成日期**：2026-06-24 | **API**：DeepSeek
