# 03 快速上手：安装与第一个多 Agent 应用

> 从零跑通 AutoGen v0.4：三层架构的安装方式、双 Agent 对话的完整代码全解、运行验证——先让两个 Agent 聊起来，再理解每一行。

## 📚 目录

1. [v0.4 三层架构与安装](#1-v04-三层架构与安装)
2. [环境准备](#2-环境准备)
3. [第一个双 Agent 应用](#3-第一个双-agent-应用)
4. [代码逐行讲解](#4-代码逐行讲解)
5. [运行验证](#5-运行验证)
6. [常见报错排查](#6-常见报错排查)
7. [面试高频问法](#7-面试高频问法)
8. [扩展练习与进阶路径](#8-扩展练习与进阶路径)

## 1. v0.4 三层架构与安装

### 三层架构（2025 重构）

| 层 | 包名 | 职责 |
|---|---|---|
| 核心层 | autogen-core | 事件驱动原语（RoutedAgent/消息/订阅） |
| 高层 API | autogen-agentchat | AssistantAgent/GroupChat/initiate_chat |
| 扩展层 | autogen-ext | OpenAI/MCP/gRPC/代码执行器等 |

### 安装命令（2026 典型）

```bash
pip install -U "autogen-agentchat" "autogen-ext[openai]"
```

### 安装选择

| 需求 | 包 |
|---|---|
| 基本多 Agent | autogen-agentchat |
| OpenAI 模型 | autogen-ext[openai] |
| 代码执行 | autogen-ext[code-executor] |
| MCP 支持 | autogen-ext[mcp] |
| 全部扩展 | autogen-ext[all] |

> ⚠️ 注意：v0.4 起安装的是 `autogen-agentchat`（不是旧的 `pyautogen`）——v0.2 时代 `pip install pyautogen`，两者 API 不兼容（07 篇详解差异）。

## 2. 环境准备

### 配置模型（OpenAI 兼容）

```python
# 环境变量（或配置文件）
# OpenAI：OPENAI_API_KEY
# DeepSeek：DEEPSEEK_API_KEY + base_url 指向 https://api.deepseek.com
# 本地（Ollama）：OLLAMA_API_KEY（任意）+ base_url 指向本地
```

### 模型配置对象

```python
from autogen_agentchat.agents import AssistantAgent
from autogen_ext.models.openai import OpenAIChatCompletionClient

# OpenAI 兼容客户端（支持 DeepSeek 等）
model_client = OpenAIChatCompletionClient(
    model="deepseek-chat",
    api_key="sk-xxx",                     # 或从环境变量读
    base_url="https://api.deepseek.com",  # DeepSeek 端点
)
```

## 3. 第一个双 Agent 应用

```python
"""AutoGen v0.4 最小示例：两个 Agent 对话"""
import asyncio
from autogen_agentchat.agents import AssistantAgent
from autogen_agentchat.teams import RoundRobinGroupChat
from autogen_agentchat.ui import Console
from autogen_ext.models.openai import OpenAIChatCompletionClient

async def main():
    # 1. 模型客户端
    model = OpenAIChatCompletionClient(
        model="deepseek-chat",
        base_url="https://api.deepseek.com",
        api_key="YOUR_KEY",
    )

    # 2. 两个 Agent（不同角色）
    planner = AssistantAgent(
        name="Planner",
        model_client=model,
        system_message="你是规划者：把任务拆解成清晰的步骤，简洁回复。",
    )
    executor = AssistantAgent(
        name="Executor",
        model_client=model,
        system_message="你是执行者：根据规划步骤给出具体实现，简洁回复。",
    )

    # 3. 群聊团队（轮流发言）
    team = RoundRobinGroupChat([planner, executor], max_turns=6)

    # 4. 启动对话
    await Console(team.run_stream(task="写一个 Python 函数计算斐波那契数列"))

if __name__ == "__main__":
    asyncio.run(main())
```

## 4. 代码逐行讲解

| 段 | 作用 | 关键点 |
|---|---|---|
| OpenAIChatCompletionClient | 模型客户端 | OpenAI 兼容协议（DeepSeek/Ollama 通用） |
| AssistantAgent × 2 | 两个角色 Agent | name 唯一；system_message 定义角色 |
| RoundRobinGroupChat | 团队（轮流发言） | max_turns=6 是**终止兜底** |
| run_stream | 异步运行 | 流式输出；await 必需（事件驱动） |
| Console | 终端可视化 | 打印消息流 |

### 与 v0.2 的对照（老代码迁移者看）

| v0.2 | v0.4 |
|---|---|
| `autogen.ConversableAgent` | `autogen_agentchat.agents.AssistantAgent` |
| `initiate_chat()`（同步） | `team.run_stream()`（异步） |
| `GroupChatManager` | `RoundRobinGroupChat` 等团队 |
| `pip install pyautogen` | `pip install autogen-agentchat` |

> ⚠️ v0.4 是破坏性重构：v0.2 代码不能直接升级，需要重写——这也是 AG2 分叉存在的根本原因（07 篇）。

## 5. 运行验证

### 预期输出

```text
---------- user ----------
写一个 Python 函数计算斐波那契数列
---------- Planner ----------
规划步骤：
1. 定义 fib(n) 递归或迭代实现
2. 边界处理（n<=0）
3. 测试验证
---------- Executor ----------
实现如下：
def fib(n): ...
---------- Planner ----------
步骤完整，Executer 的实现正确。
[对话结束：max_turns=6 达到或任务完成]
```

### 验证清单

| 项 | 检查 |
|---|---|
| 安装 | `pip show autogen-agentchat` 有版本 |
| 模型连接 | 无 401/超时错误 |
| 对话推进 | 两个 Agent 交替发言 |
| 终止生效 | max_turns 后停止（不无限聊） |
| 角色区分 | Planner 拆解、Executor 实现（system_message 生效） |

## 6. 常见报错排查

| 报错 | 原因 | 修复 |
|---|---|---|
| ModuleNotFoundError: autogen | 装了 v0.2 的 pyautogen | `pip install -U "autogen-agentchat"` |
| API key not found | 环境变量未设 | export OPENAI_API_KEY 或配置 |
| 401/403 | Key 无效/端点错 | 检查 base_url（DeepSeek 必须指 deepseek.com） |
| RuntimeError: Event loop closed | asyncio 使用不当 | 用 `asyncio.run(main())` 顶层入口 |
| 对话不结束 | 无终止兜底 | max_turns 必配 |
| 工具调用报错 | 工具定义/序列化问题 | 检查 FunctionTool 参数（04 篇） |

### 排错三问

```
① 装对了吗？（v0.4 的 autogen-agentchat 而非 pyautogen）
② 模型通了吗？（先单独调模型客户端）
③ 终止了吗？（max_turns 兜底有没有）
```

## 7. 面试高频问法

| 问题 | 回答要点 |
|---|---|
| v0.4 怎么安装？ | autogen-agentchat + autogen-ext[openai]（不是 pyautogen） |
| 最小多 Agent 应用？ | 两个 AssistantAgent + RoundRobinGroupChat + run_stream |
| 为什么异步？ | v0.4 事件驱动架构，异步是基础 |
| max_turns 作用？ | 终止兜底（对话开放的防烧钱机制） |
| 怎么换 DeepSeek？ | OpenAI 兼容客户端换 base_url 即可 |
| v0.2 代码能升级吗？ | 不能直接升——破坏性重构，需重写（AG2 的原因） |

### 面试加分表达

> "跑通 AutoGen 的关键是三件事：装对包（autogen-agentchat 而非 pyautogen）、配好 OpenAI 兼容客户端（换 DeepSeek 只改 base_url）、永远配 max_turns 兜底。v0.4 是事件驱动架构，所以全程异步——这既是性能优势，也是迁移成本（v0.2 代码必须重写）。"

## 8. 扩展练习与进阶路径

### 五个扩展练习（按顺序）

| 练习 | 内容 | 掌握 |
|---|---|---|
| 1 | 给 Executor 加一个工具（04 篇） | 工具闭环 |
| 2 | 换 SelectorGroupChat（05 篇） | 模式差异 |
| 3 | 加入 UserProxyAgent 请求输入 | 人类介入 |
| 4 | 用 Studio 拖出同样的团队（06 篇） | 低代码对照 |
| 5 | 跑通后导出、加日志与成本统计 | 工程化意识 |

### 进阶路径

```
① 单团队跑通 → 多团队组合（分层编排）
② 本地模型（Ollama）接入 → 成本可控
③ MCP 工具接入（autogen-ext[mcp]）→ 生态扩展
④ 读 MAF 迁移指南（08 篇）→ 新项目路线
```

### 常见练习失败

| 失败 | 原因 | 对策 |
|---|---|---|
| 对话空转 | 角色提示词太泛 | 系统提示写具体职责 |
| 工具不触发 | description 不清 | 写明"何时用" |
| 无限对话 | 无终止 | max_turns 兜底 |
| 结果不对 | 没给验证环节 | 加 reviewer Agent |

> 🎯 核心要点：v0.4 三层架构（core/agentchat/ext）；安装用 autogen-agentchat；最小应用 = 两个 AssistantAgent + RoundRobinGroupChat + run_stream；max_turns 是终止兜底；OpenAI 兼容客户端让 DeepSeek/本地模型一行切换；v0.2 代码不能直接升级（破坏性重构）；五个扩展练习按顺序做，失败定位用四张表。

---

**下一模块**：[04-工具调用与代码执行](04-工具调用与代码执行.md) / **返回总览**：[00-AutoGen知识体系总览](00-AutoGen知识体系总览.md)
