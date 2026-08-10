# 08 - 工具调用与 Agent

> 本体系第八课：Agent 的 1.x 姿势——@tool 工具、create_agent、LangGraph 底层——"Agent 是应用的'大脑'——create_agent 是 1.x 的入口——工具是它的'手脚'"

---

## 📚 目录

1. [Agent 的定位](#1-agent-的定位)
2. [@tool：定义工具](#2-tool定义工具)
3. [create_agent：1.x 姿势](#3-create_agent1x-姿势)
4. [LangGraph 底层与边界](#4-langgraph-底层与边界)
5. [练习 5 题](#5-练习-5-题)
6. [本节验收](#6-本节验收)

---

## 1. Agent 的定位

**Agent = 能"自己决定怎么完成任务"的应用（模型 + 工具 + 循环）**：

```text
Agent 的定位
├── 本质：模型自主决策（不是一次调用——是多步循环）
├── 组成：模型（大脑）+ 工具（手脚）+ 循环（思考-行动-观察）
├── 场景：需要多步/查资料/调工具的任务（客服/数据分析/操作执行）
├── 1.x 姿势：create_agent（LangGraph 底层——01 篇三新之一）
└── 对比 Chain：Chain 是"固定流程"、Agent 是"自主决策"
    ——"Chain 是'流水线'（走固定路线），Agent 是'探险家'（自己找路）"
```

**定位心智**：**"Agent 的记忆：'大脑 + 手脚 + 循环——自主决策的应用'"**——"**循环的本质：模型思考（要调工具？）→ 行动（调工具）→ 观察（看结果）→ 再思考（继续/结束）——'思考-行动-观察'循环直到任务完成（`主流 Agent 范式` 体系的 ReAct 模式）'"**（"1.x 的重要变化：create_agent 是新入口（底层自动生成 LangGraph 状态机——流式/持久化/可观测开箱即用）——**'AgentExecutor 维护至 2026-12——新代码用 create_agent（01 篇）'"**）；**Chain vs Agent 的选型**——"流程固定（三步走完）→ Chain；流程不定（要看情况）→ Agent——**'固定用 Chain、灵活用 Agent（先 Chain 后 Agent——能 Chain 就不 Agent）'"**（"Agent 的成本：多步循环 = 多次模型调用（贵）+ 可能失控（要护栏——09 篇）——**'Agent 是'能者多劳'，也是'贵者多劳'"**）。

## 2. @tool：定义工具

**@tool = 定义工具的最简姿势（函数 + 装饰器）**：

```python
from langchain_core.tools import tool

# @tool：函数变工具（函数名 = 工具名、docstring = 工具描述）
@tool
def get_weather(city: str) -> str:
    """查询指定城市的当前天气。参数：city（城市名）。"""
    # 这里是真实实现（查天气 API/数据库）
    return f"{city}：晴，25°C"

@tool
def calculate(expression: str) -> str:
    """计算数学表达式（如 "2+3*4"）。"""
    return str(eval(expression))          # 示例——生产要安全实现

# 工具的要点（Function Calling 体系的规范）
# ① 参数类型注解（str/int——模型据此生成参数）
# ② docstring 描述（模型据此决定"何时用"——03 篇 bind_tools 的决策依据）
# ③ 实现要安全（工具是"执行"的——生产要校验/鉴权）
```

**工具心智**：**"工具的记忆：'@tool 装饰器——函数变工具——docstring 是模型的决策依据'"**——"**为什么 docstring 重要：模型看不到你的代码——它靠 docstring 决定'要不要用这个工具、传什么参数'——'工具描述 = 模型的使用说明书（写清'做什么/何时用'）'"**（"参数的设计：参数类型（str/int）让模型生成正确参数；参数少而清晰（模型好决策）——**'工具设计 = 让模型'好用'（Function Calling 体系的三原则）'"**）；**工具的实现责任**——"@tool 只是'包装'——真实逻辑（查库/调 API/执行）是你写的——**'工具的执行是真实的（要安全——校验输入/鉴权——生产工具不能裸奔）'"**（"了解即可：工具的高级形态（多参数/异步/复杂返回）用时查——**'先会'函数 + 装饰器 + 描述'的最简姿势'"**）。

## 3. create_agent：1.x 姿势

**create_agent = 1.x 的 Agent 入口（一行创建）**：

```python
from langchain.agents import create_agent
from langchain_openai import ChatOpenAI

# 创建 Agent（模型 + 工具 + 系统提示词）
agent = create_agent(
    model=ChatOpenAI(model="gpt-4o-mini"),
    tools=[get_weather, calculate],          # 工具列表（@tool 定义的）
    system_prompt="你是天气助手，能用工具查天气和算数。",
)

# 调用（Agent 自动循环：思考 → 调工具 → 观察 → 继续）
result = agent.invoke({"messages": [("user", "北京今天多少度？")]})
print(result["messages"][-1].content)        # Agent 的回答
# 内部过程：模型决定调 get_weather → 执行 → 看结果 → 回答
# 全程自动（create_agent 内置循环——LangGraph 状态机）
```

**create_agent 心智**：**"create_agent 的记忆：'模型 + 工具 + 提示词——一行创建——循环自动'"**——"**为什么 1.x 推荐：底层自动生成 LangGraph 状态机（流式/持久化/可观测开箱即用）——'对比 AgentExecutor（维护模式）——create_agent 是'生产级'的新姿势'"**（"内部流程（了解即可）：create_agent 跑的是 ReAct 循环（思考-行动-观察——`主流 Agent 范式` 体系）——**'知道'循环自动'就够——细节是框架的活'"**）；**Agent 的输入输出**——"输入：消息列表（messages）；输出：消息列表（最后一跳是回答）——**'Agent 的接口也是消息（与 ChatModel 一致——统一 Runnable 的落地）'"**（"Agent 的状态：可以用 Checkpointer（07 篇——带记忆的 Agent——thread_id 会话）——**'Agent + 记忆 = 完整的多轮助手'"**）。

## 4. LangGraph 底层与边界

**LangGraph = Agent 的编排引擎（create_agent 的底层）**：

```text
LangGraph 底层
├── 本质：图编排引擎（节点 + 边——状态机）
├── create_agent 的底层：create_agent 生成一个 LangGraph 状态机
│    （节点：模型调用/工具执行；边：条件跳转——循环/结束）
├── 自建图的场景：复杂流程（多 Agent 协作/人工审批/复杂分支）
└── 生态位置：编排层（01 篇四件套——LangGraph 是复杂编排的杀手锏）
    ——"create_agent 是'现成的图'、LangGraph 是'自己画图'"
```

**LangGraph 心智**：**"LangGraph 的记忆：'编排引擎——create_agent 是现成图、LangGraph 是自己画'"**——"**什么时候自己画：create_agent 不够时（多 Agent 协作、人工审批环节、复杂条件流）——'简单 Agent 用 create_agent、复杂流程自建图（LangGraph 深入是进阶体系）'"**（"本体系的边界：知道'LangGraph 存在 + 是 create_agent 底层'——深入（画图/多 Agent）是 LangGraph 体系的事——**'本课给'地图'，进阶给'路'"**）；**Agent 的边界与护栏**——"Agent 的失控风险：循环不结束/乱调工具/输出有害——**'生产 Agent 要护栏（工具白名单/步骤上限/人工审批——09 篇 LangSmith + 安全）'"**（"了解即可：知道'Agent 强但有风险'——**'护栏是生产 Agent 的标配（Function Calling 体系的安全章节）'"**）。

**Agent 的调试与观测**："**① 看轨迹**——LangSmith 追踪（每个思考/行动/观察步骤可见——09 篇）；**② 试工具**——先把工具单独调通（@tool 函数单独测试）再接 Agent；**③ 限制步骤**——循环次数上限（防死循环——护栏之一）——**'三个姿势 = Agent 的'不翻车'三板斧'"**（"调试的递进：工具单测 → 最小 Agent（一个工具）→ 多工具 → 生产（+护栏）——**'从简到繁的调试路径（02 篇小链先测的 Agent 版）'"**）。

## 5. 练习 5 题

1. Agent 是什么？（大脑 + 手脚 + 循环——自主决策）
2. @tool 的要点？（docstring 是决策依据——参数注解——实现要安全）
3. create_agent 的姿势？（模型 + 工具 + 提示词——一行创建）
4. Chain vs Agent？（固定用 Chain、灵活用 Agent）
5. LangGraph 是什么？（编排引擎——create_agent 的底层）

## 6. 本节验收

**验收动作**：① 用 @tool 定义两个工具（天气/计算）；② 用 create_agent 创建 Agent 并调用（观察自动循环）；③ 让 Agent 完成一个多步任务（查天气 + 计算温差）；④ 写"Agent 选型"笔记（Chain vs Agent）——**"工具 + create_agent + 边界 = Agent 能力"**——**练习纪律**：Agent 前先想"能不能用 Chain"——"固定用 Chain、灵活用 Agent"。

> 🎯 **核心要点**：Agent = 自主决策（**大脑 + 手脚 + 循环——思考-行动-观察——固定用 Chain 灵活用 Agent——Agent 贵要护栏**）；**@tool（函数变工具——docstring 是模型决策依据——实现要安全）**；**create_agent（1.x 入口——模型 + 工具 + 提示词——循环自动——LangGraph 状态机开箱即用——AgentExecutor 维护至 2026-12）**；**LangGraph（编排引擎——create_agent 是现成图复杂流程自己画——本课给地图进阶给路）**——"Agent 是应用的'大脑'——create_agent 是 1.x 的入口——工具是它的'手脚'"。

---

**上一模块**：[07-记忆与对话.md](./07-记忆与对话.md) / **下一模块**：[09-集成生态.md](./09-集成生态.md)
