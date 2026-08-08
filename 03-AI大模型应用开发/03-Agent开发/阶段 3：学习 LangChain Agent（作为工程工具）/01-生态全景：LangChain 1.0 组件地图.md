# 01 生态全景：LangChain 1.0 组件地图

> 定位：LangChain 不是"一个库"——五大组件的分工地图、安装姿势、与手写/原生代码的对应关系（2026-08 基准）

## 📚 目录

1. [五大组件地图](#1-五大组件地图)
2. [安装与版本窗口](#2-安装与版本窗口)
3. [与手写代码的对应表](#3-与手写代码的对应表)
4. [框架帮你做了什么（价值分析）](#4-框架帮你做了什么价值分析)
5. [框架的代价](#5-框架的代价)

## 1. 五大组件地图

```text
LangChain 生态（2026-08）
│
├── langchain-core    核心抽象：模型/消息/工具/Runnable/提示词（地基）
│
├── langchain         官方整合层 + 高层 Agent API（create_agent）
│   └── langchain-community / 供应商包（langchain-openai、langchain-deepseek 等）
│
├── LangGraph         状态机执行运行时（create_agent 的底层引擎）
│
├── LangSmith         可观测/调试/评估平台（官方）
│
└── LangServe         部署（把链/Agent 发布为 API，本阶段了解即可）
```

| 组件 | 定位 | 本阶段用法 |
|------|------|-----------|
| langchain-core | 抽象层 | 导入 `@tool`、消息类型、ChatPromptTemplate |
| langchain | 整合 + 高层 API | **`create_agent`** |
| 供应商包 | 模型接入 | `ChatOpenAI`（base_url 指向 DeepSeek 亦可） |
| LangGraph | 状态机引擎 | 框架自动使用；04 篇手动下沉 |
| LangSmith | 可观测 | 06 篇入门 |

> 🎯 **核心要点**：`create_agent` 来自 **langchain**（不是 langgraph.prebuilt）——这是 2026 版最重要的导入变化。老的 `AgentExecutor`（langchain.agents）已进维护期。

## 2. 安装与版本窗口

```bash
# 2026-08 推荐安装
pip install langchain langchain-openai langgraph
# langchain 1.0+（2025-10-22 GA）
# langgraph 1.0+（同日 GA）
```

| 版本事实 | 说明 |
|---------|------|
| LangChain 1.0 | 2025-10-22 GA；API 稳定承诺 |
| LangGraph 1.0 | 同步 GA；保持核心图 API（state/nodes/edges）不变 |
| create_react_agent | **LangGraph v1 已弃用** → 用 `create_agent` |
| AgentExecutor | **维护期至 2026-12**，官方禁止新代码使用 |
| 供应商包 | langchain-openai / langchain-deepseek / langchain-anthropic 等 |

> ⚠️ **教程陷阱**：网上大量教程（2024-2025）仍教 `AgentExecutor` + `create_react_agent`——2026 学它们等于学"维护期 API"。本阶段一律使用 `create_agent`。

## 3. 与手写代码的对应表

| 手写（阶段 1/2） | LangChain 等价物 | 框架帮你做了 |
|-----------------|-----------------|------------|
| 手写 tools 数组（JSON Schema） | `@tool` 装饰器 | 从函数签名/docstring 生成 schema |
| 手写工具注册表 + execute_tool | `ToolNode` | 注册、分发、异常转错误回传 |
| 手写 while 循环 | `create_agent`（内部 LangGraph） | 节点调度、条件路由、终止 |
| 手写消息追加铁律 | 框架内部 messages 管理 | 自动追加 assistant/tool 消息 |
| 手写 MAX_TURNS | `recursion_limit` / remaining_steps | 循环上限内置 |
| 手写 stream 拼装 | `agent.stream(stream_mode="values")` | 事件流封装 |
| 手写 checkpointer/记忆 | `checkpointer` 参数 | 持久化一行配置 |
| 手写 response_format | `response_format` 参数 | 与 Pydantic 集成 |

> 💡 **费曼练习**：看到框架任何"黑盒行为"，尝试回答"这对应我手写代码里的哪一段？"——对不上，说明你还没真正理解框架。

## 4. 框架帮你做了什么（价值分析）

| 能力 | 手写成本 | 框架成本 | 框架的价值点 |
|------|:---:|:---:|------------|
| 基础工具循环 | 60 行（阶段 1） | 3 行 | **小**（你已会写） |
| 记忆/持久化 | 中等（外部存储接入） | 1 参数 | **大** |
| 流式 + 可观测 | 中（事件拼装） | 1 方法 | **大** |
| 多 Agent 编排 | 高（阶段 5 内容） | StateGraph 声明式 | **大** |
| 跨模型/供应商 | 手写适配层 | 包即用 | **大** |
| 错误/边界处理 | 中 | 参数级 | **中** |

> 🎯 **核心要点**：框架价值的真实分布——**基础循环你自己 60 行就写了，价值不大；记忆/流式/可观测/多 Agent/跨供应商才是框架的护城河**。这也决定了选型：简单单工具 Agent 手写即可，生产能力密集的场景用框架。

## 5. 框架的代价

| 代价 | 说明 | 缓解 |
|------|------|------|
| 黑盒抽象 | 报错堆栈深、行为需理解 | 本阶段 04 篇学状态机 |
| 版本漂移 | 1.0 前 API 频繁变动 | 锁版本、看官方迁移指南 |
| 与供应商原生能力脱节 | 新能力（如 Responses API）框架适配滞后 | 需要时用原生（阶段 2 能力） |
| 依赖重 | 安装包多、升级联动 | 按需安装、虚拟环境隔离 |

> 💡 **2026 生产共识**：LangChain + LangGraph 组合使用——LangChain 提供整合与高层 API，LangGraph 提供状态机运行时；需要节点级控制时下沉 StateGraph（04 篇）。

---

**返回总览**：[00-阶段总览：LangChain Agent 学习](00-阶段总览：LangChain%20Agent%20学习.md) / **下一模块**：[02-@tool 工具定义](02-%40tool%20工具定义.md)
