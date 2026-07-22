# AI Agent 智能体开发 面试宝典
> 基于课程大纲全面覆盖面试高频考点 -- Brain + Perception + Planning + Memory + Action 全架构解析，涵盖 LangChain / LangGraph / MCP / 多轮对话 / 记忆持久化

## 目录
1. [一、基础概念速答](#一基础概念速答18-24题)
2. [二、深度原理剖析](#二深度原理剖析10-14题)
3. [三、实战场景题](#三实战场景题8-10题)
4. [四、手写代码题](#四手写代码题6-8题)
5. [五、系统设计题](#五系统设计题3-5题)
6. [六、常见坑点与最佳实践](#六常见坑点与最佳实践)
7. [七、面试回答模板](#七面试回答模板top-5高频题的结构化回答模板)
8. [八、快速查漏补缺 Checklist](#八快速查漏补缺-checklist)

---

## 一、基础概念速答（18-24题）

### 1.1 什么是 AI Agent？与普通 LLM 有什么区别？
**AI Agent（智能体）** 是以大语言模型（LLM）为"大脑"，结合**规划（Planning）、记忆（Memory）、工具调用（Tools）、行动执行（Action）** 四大模块，能够自主感知环境、拆解任务并执行操作的智能程序。

| 维度 | 普通 LLM | AI Agent |
|------|---------|----------|
| 能力范围 | 仅文本生成与理解 | 自主规划、调用 API、读写文件、操作数据库 |
| 执行方式 | 单次问答 | Thought-Action-Observation 循环闭环 |
| 自主性 | 被动回答，依赖用户每一步指令 | 主动拆解任务，自主决策下一步 |
| 上下文 | 仅当前对话 | 拥有短期 + 长期记忆系统 |

> 🎯 一句话：LLM 是大脑，Agent 是完整的人（大脑 + 手脚 + 规划 + 记忆力）。

### 1.2 Agent 的五大核心组件是什么？
| 组件 | 英文 | 职责 |
|------|------|------|
| **大脑** | Brain / LLM | 推理、判断、意图识别、决策下一步 |
| **感知** | Perception | 接收多模态输入（文本、语音、图片、传感器） |
| **规划** | Planning | 将复杂任务拆解为多步子任务，如 Chain-of-Thought、ReAct |
| **记忆** | Memory | 工作记忆/短期/长期三层存储 |
| **行动** | Action | 调用工具、执行代码、发送 API 请求、操作文件系统 |

### 1.3 什么是 ReAct 模式？
ReAct = **Re**asoning + **Act**ion，是业界最通用的 Agent 运行范式。它在每一轮推理中交替执行以下步骤：

```
1. Thought（思考）  ：当前需要做什么？需要哪个工具？
2. Action（行动）    ：调用工具，传入参数
3. Observation（观察）：获取工具返回结果
4. 循环迭代          ：不够则继续 Thought → Action → Observation
5. Final Answer（最终答案）：整合结果返回给用户
```

> 💡 ReAct 最早由论文 "ReAct: Synergizing Reasoning and Acting in Language Models" 提出，LangChain 的 AgentExecutor 默认采用此模式。

### 1.4 LangChain Agent 的核心组件有哪些？
| 组件 | 说明 |
|------|------|
| **LLM** | 底层大模型，负责推理与生成 |
| **Prompt Template** | 提示词模板，指导 Agent 行为格式 |
| **Tools** | Agent 可调用的外部工具，由 `@tool` 装饰器注册 |
| **ToolKit** | 工具集合 |
| **Agent Executor** | Agent 执行器，负责 Thought-Action-Observation 循环 |
| **Output Parser** | 输出解析器，将 LLM 输出转为结构化数据 |
| **Memory** | 记忆系统，存储对话上下文 |

### 1.5 LLM / RAG / Agent 的区别是什么？
| 维度 | LLM | RAG | Agent |
|------|-----|-----|-------|
| 核心能力 | 语言理解+生成 | 检索+生成 | 规划+工具+记忆+循环 |
| 知识来源 | 训练数据 | 训练数据+外部知识库 | 训练数据+工具+记忆+实时信息 |
| 行动能力 | 无 | 无 | 调用 API、执行代码、操作文件 |
| 典型场景 | 聊天机器人 | 知识库问答 | 自动化流程、任务编排 |

```
LLM（基础大脑）
├── 加知识库 → RAG（专属问答系统）
└── 加规划 + 工具 + 记忆 → Agent（自主执行系统）
```

### 1.6 什么是 MCP 协议？
**MCP（Model Context Protocol）** 是由 Anthropic 推出的开源协议，旨在标准化大模型与外部工具/数据源的通信方式。它类似于"AI 世界的 USB 协议"—— 一个工具一次接入，所有 MCP 兼容的 Agent 都能使用。

| 角色 | 说明 | 类比 |
|------|------|------|
| **MCP Host** | 发起请求的 LLM 应用 | 电脑主机 |
| **MCP Client** | 与 Server 保持 1:1 连接 | USB 驱动程序 |
| **MCP Server** | 为 LLM 提供工具/资源 | 外部设备（键盘、鼠标） |

MCP 支持的传输协议：
- **stdio**：通过标准输入/输出进行本地通信，适合本地工具
- **SSE（Server-Sent Events）**：HTTP 流式传输，适合远程服务

### 1.7 什么是 LangGraph？它解决了什么痛点？
LangGraph 是 LangChain 团队推出的**有向图（DAG）工作流框架**，用于构建复杂的多步骤 Agent 流程。

**LangChain Agent 的局限：**
- 仅支持线性循环（Think → Act → Observe）
- 无法实现条件分支、并行执行
- 状态管理能力弱

**LangGraph 的解决方案：**
| 概念 | 说明 |
|------|------|
| **State** | 节点间传递的共享状态 |
| **Node** | 工作流中的一个处理步骤 |
| **Edge** | 节点间连接 |
| **Conditional Edge** | 条件路由，根据 State 值动态选择下一节点 |

### 1.8 `@tool` 装饰器的作用是什么？
`@tool` 是 LangChain 提供的装饰器，用于将 Python 函数注册为 Agent 可调用的工具。它自动从函数签名和文档字符串中提取参数 schema，生成工具描述。

```python
from langchain_core.tools import tool

@tool
def get_weather(location: str, unit: str = "celsius") -> str:
    """查询指定位置的天气信息"""
    # 调用天气 API
    return f"{location} 的天气是晴，温度 25{unit}"
```

> 💡 `args_schema` 参数可以用 Pydantic 模型精细控制输入参数。

### 1.9 什么是 PromptTemplate / ChatPromptTemplate / FewShotPromptTemplate？
| 类型 | 说明 | 适用场景 |
|------|------|----------|
| **PromptTemplate** | 基础字符串模板，`{variable}` 占位替换 | 简单文本生成 |
| **ChatPromptTemplate** | 对话消息模板，支持 System / Human / AI 角色 | 多轮对话 |
| **FewShotPromptTemplate** | 小样本模板，提供示例引导 LLM 输出格式 | 需要格式规范的场景 |

```python
from langchain_core.prompts import PromptTemplate, ChatPromptTemplate, FewShotPromptTemplate

# PromptTemplate
prompt = PromptTemplate.from_template("请用{topic}写一首诗")

# ChatPromptTemplate
chat_prompt = ChatPromptTemplate.from_messages([
    ("system", "你是{role}领域的专家"),
    ("human", "{question}")
])

# FewShotPromptTemplate
examples = [
    {"query": "苹果", "answer": "水果"},
    {"query": "Java", "answer": "编程语言"},
]
```

### 1.10 LCEL（LangChain Expression Language）是什么？
LCEL 是 LangChain 提供的声明式链式调用语法，用 `|` 管道符将组件串联成 Pipeline。

```python
# LCEL 管道式链式调用
chain = prompt | model | output_parser

# 与传统方式对比
# 传统：output_parser.parse(model.invoke(prompt.format(topic="AI")))
# LCEL：chain.invoke({"topic": "AI"})
```

**LCEL 的核心优势：** 支持流式输出、异步调用、并行执行、自动重试、中间结果追踪。

### 1.11 LangChain 的 Runnables 是什么？
Runnables 是 LangChain 的核心抽象接口，所有可执行的组件（Model、Prompt、Parser、Retriever）都实现了 `Runnable` 接口。

| 方法 | 说明 |
|------|------|
| `invoke()` | 同步调用 |
| `ainvoke()` | 异步调用 |
| `stream()` | 流式输出 |
| `batch()` | 批量调用 |
| `bind()` | 绑定额外参数（如 tools 绑定到模型） |

### 1.12 多轮对话中如何保持上下文？
LangChain 提供了多层机制维护对话上下文：

```python
from langchain.memory import ChatMessageHistory
from langchain_core.runnables.history import RunnableWithMessageHistory

# 1. ChatMessageHistory 存储历史消息
history = ChatMessageHistory()
history.add_user_message("你好")
history.add_ai_message("你好！有什么可以帮助你的？")

# 2. RunnableWithMessageHistory 包装链式调用
chain_with_history = RunnableWithMessageHistory(
    runnable=chain,
    get_session_history=lambda sid: ChatMessageHistory(),
    input_messages_key="question",
    history_messages_key="history"
)
```

### 1.13 Agent 的记忆系统分几层？
| 层级 | 存储位置 | 生命周期 | 容量 | 检索方式 |
|------|----------|----------|------|----------|
| **工作记忆** | LLM 上下文窗口 | 单次推理周期 | KB-MB | 直接注入 Prompt |
| **短期记忆** | 会话缓存（Redis/内存） | 单次会话 | 几十条消息 | 滑动窗口取最近 N 条 |
| **长期记忆** | 向量库 + 结构化 DB | 跨会话持久化 | GB-TB | 向量相似度检索 |

### 1.14 什么是 Function Calling？和 `@tool` 的关系？
Function Calling 是大模型（如 OpenAI、Qwen）的原生能力——模型输出结构化 JSON 格式的工具调用参数，由框架负责执行。`@tool` 装饰器是 LangChain 对 Function Calling 的高级封装，自动完成：

```
@tool 函数 → 提取函数名+参数描述+文档字符串 → 生成 Function Calling Schema 
→ 发送给 LLM → LLM 返回 JSON 调用参数 → 框架自动执行并返回结果
```

### 1.15 LangChain 的 Output Parser 有哪些？
| 解析器 | 说明 | 适用场景 |
|--------|------|----------|
| **StrOutputParser** | 直接输出字符串 | 通用文本生成 |
| **JsonOutputParser** | 解析 JSON 格式输出 | API 响应、结构化数据 |
| **PydanticOutputParser** | 输出解析为 Pydantic 模型 | 强类型数据结构 |
| **CommaSeparatedListOutputParser** | 逗号分隔列表 | 关键词列表 |
| **DatetimeOutputParser** | 日期时间格式 | 日期处理 |

### 1.16 Shell / PowerShell MCP 工具的作用是什么？
通过 subprocess / psutil / pyautogui 等库将操作系统终端能力封装为 MCP 工具，让 Agent 可以直接执行 Shell 命令、管理进程、操作文件系统。

```python
# 核心实现思路（安全模式：使用列表传参，避免 shell 注入）
import subprocess
from langchain_core.tools import tool

@tool
def run_shell_command(command: str, *args: str) -> str:
    """在本地终端执行 Shell 命令
    ⚠️ 生产环境建议使用 shlex.split() 解析参数 + 白名单限制"""
    import shlex
    cmd_list = shlex.split(command) + list(args)
    result = subprocess.run(cmd_list, capture_output=True, text=True)
    return result.stdout + result.stderr
```

### 1.17 Playwright MCP 能做什么？
Playwright MCP 将浏览器自动化能力封装为 MCP 工具，Agent 可以：
- 打开网页并截图
- 填写表单并提交
- 点击按钮、导航页面
- 提取页面内容
- 执行 JavaScript 脚本

### 1.18 GitHub MCP 提供了哪些能力？
GitHub MCP 工具集允许 Agent 直接操作 GitHub：
- 创建/管理 Repository
- 创建/审核 Pull Request
- 读取和搜索代码
- 管理 Issues 和 Projects
- 查看 CI/CD 状态

### 1.19 什么是 \`create_react_agent\`？
LangGraph 提供的快捷函数，快速创建一个基于 ReAct 模式的 Agent：

```python
from langgraph.prebuilt import create_react_agent

agent = create_react_agent(
    model=llm,
    tools=[get_weather, search, calculator],
    state_modifier="你是一个智能助手，请使用工具回答用户问题。"
)
```

### 1.20 什么是多 Agent 协作？有哪些模式？

| 模式 | 说明 | 适用场景 |
|------|------|----------|
| **Supervisor（主管）** | 一个 Agent 调度多个子 Agent | 复杂任务分配 |
| **Peer-to-Peer（对等）** | Agent 间自由通信协作 | 开放式讨论 |
| **Pipeline（流水线）** | Agent 按顺序接力执行 | 固定流程任务 |
| **Debate（辩论）** | 多 Agent 提出不同观点并评审 | 决策评审 |

---

## 二、深度原理剖析（10-14题）

### 2.1 ReAct 循环的完整数据流是怎样的？

```
用户输入: "北京明天会下雨吗？"

Round 1:
  Thought: 用户想知道北京的天气，我需要调用天气查询工具。
  Action: get_weather(location="北京", date="2026-07-23")
  Observation: {"temperature": 28, "condition": "多云", "humidity": 65%}
  
  Thought: 天气返回多云，没有提到下雨。我再查一下降水概率。
  Action: get_precipitation(location="北京", date="2026-07-23")
  Observation: {"precipitation_probability": "10%"}
  
Final Answer: 北京明天（7月23日）多云，降水概率仅10%，基本不会下雨。
```

**关键技术细节：**
- Agent 每次执行 Action 后，LLM 判断是否符合终止条件
- 最大迭代次数（`max_iterations`）防止死循环，默认 15 次
- 早期停止（`early_stopping_method`）支持 "generate" 或 "force"

### 2.2 LangGraph 的 StateGraph 如何管理状态？

```python
from typing import TypedDict, Annotated, Sequence
from langgraph.graph import StateGraph, END
from langgraph.graph.message import add_messages

# 1. 定义 State（消息使用 add_messages 合并策略）
class AgentState(TypedDict):
    messages: Annotated[list, add_messages]
    next_step: str
    tool_results: dict

# 2. 构建图
graph = StateGraph(AgentState)

# 3. 添加节点
graph.add_node("analyze", analyze_node)
graph.add_node("call_tool", call_tool_node)
graph.add_node("respond", respond_node)

# 4. 添加边
graph.set_entry_point("analyze")
graph.add_conditional_edges(
    "analyze",
    decide_next_step,  # 条件函数
    {"tool": "call_tool", "respond": "respond", END: END}
)
graph.add_edge("call_tool", "respond")
graph.add_edge("respond", END)

# 5. 编译
app = graph.compile()
```

**状态管理的关键机制：**
- `Annotated[list, add_messages]`：消息类型字段自动追加而不是覆盖
- `Conditional Edge`：根据当前 State 的值动态路由到不同节点
- `StateGraph` 每次调用接收 State，返回 State 更新

### 2.3 LangChain Agent 的工具调用底层如何实现？

```
用户 Query
    │
    ▼
Agent Executor
    │
    ├─→ 1. Prompt 组装（System Prompt + Tools Schema + 历史 + 用户输入）
    │
    ├─→ 2. LLM 推理
    │      ├─ 返回文本（直接输出）→ StrOutputParser
    │      └─ 返回工具调用（JSON）→ AgentAction
    │
    ├─→ 3. 解析 AgentAction（包含 tool 名称 + 参数）
    │
    ├─→ 4. 执行工具（调用注册的 Python 函数）
    │      └─ 返回 AgentFinish（任务完成）或继续循环
    │
    └─→ 5. 循环直到 AgentFinish 或 max_iterations
```

**核心接口：**
```python
from langchain.agents import AgentExecutor
from langchain.agents.format_scratchpad import format_to_openai_function_messages
from langchain.agents.output_parsers import OpenAIFunctionsAgentOutputParser
```

### 2.4 MCP 协议的核心通信机制是什么？

MCP 使用 **JSON-RPC 2.0** 作为通信协议格式，支持以下核心方法：

| 方法 | 方向 | 说明 |
|------|------|------|
| `tools/list` | Server → Client | 返回可用工具列表 |
| `tools/call` | Client → Server | 调用指定工具 |
| `resources/list` | Server → Client | 返回可用资源列表 |
| `resources/read` | Client → Server | 读取指定资源 |
| `prompts/list` | Server → Client | 返回提示模板列表 |
| `notifications/*` | 双向 | 状态变更通知 |

```python
# MCP 客户端调用示例
from langchain_mcp_adapters.client import MCPClient

async with MCPClient("path/to/mcp_server.py") as client:
    tools = await client.list_tools()
    result = await client.call_tool("get_weather", {"location": "北京"})
```

### 2.5 LangChain 的 Memory 如何与 Agent 集成？

```python
from langchain.memory import ConversationBufferMemory, RedisChatMessageHistory
from langchain.agents import AgentExecutor

# 基于 Redis 的持久化记忆
history = RedisChatMessageHistory(
    session_id="user_123",
    url="redis://localhost:6379/0"
)

memory = ConversationBufferMemory(
    chat_memory=history,
    memory_key="chat_history",
    return_messages=True
)

agent_executor = AgentExecutor(
    agent=agent,
    tools=tools,
    memory=memory,  # 注入记忆
    verbose=True
)
```

**记忆系统的核心原理：**
1. `save_context(inputs, outputs)` 自动保存每一轮对话
2. `load_memory_variables()` 在每次调用前注入历史到 Prompt
3. 窗口管理：`ConversationBufferWindowMemory(k=10)` 只保留最近 10 轮
4. 摘要压缩：`ConversationSummaryMemory` 用 LLM 压缩旧消息

### 2.6 RunnableWithMessageHistory 是如何工作的？

```python
from langchain_core.runnables.history import RunnableWithMessageHistory

# 1. 创建基础链
base_chain = prompt | llm | output_parser

# 2. 包装历史消息
chain_with_history = RunnableWithMessageHistory(
    runnable=base_chain,
    get_session_history=lambda session_id: ChatMessageHistory(),
    input_messages_key="input",
    history_messages_key="history"
)

# 3. 调用时传入 session_id
chain_with_history.invoke(
    {"input": "你好"},
    config={"configurable": {"session_id": "user_001"}}
)
```

**工作原理：** 每次调用时根据 `session_id` 获取对应的 `ChatMessageHistory` 实例，自动将历史消息拼接到 Prompt 中，返回后将新消息追加到历史中。

### 2.7 FileSaver 如何实现记忆持久化？

```python
import json
import os
from pathlib import Path
from typing import List
from langchain_core.chat_history import BaseChatMessageHistory
from langchain_core.messages import BaseMessage, HumanMessage, AIMessage

class FileChatMessageHistory(BaseChatMessageHistory):
    """基于文件的对话历史存储"""
    
    def __init__(self, file_path: str):
        self.file_path = Path(file_path)
        self.file_path.parent.mkdir(parents=True, exist_ok=True)
        if not self.file_path.exists():
            self.file_path.write_text("[]", encoding="utf-8")
    
    @property
    def messages(self) -> List[BaseMessage]:
        data = json.loads(self.file_path.read_text(encoding="utf-8"))
        return [self._deserialize(m) for m in data]
    
    def add_message(self, message: BaseMessage) -> None:
        messages = json.loads(self.file_path.read_text(encoding="utf-8"))
        messages.append(self._serialize(message))
        self.file_path.write_text(
            json.dumps(messages, ensure_ascii=False, indent=2),
            encoding="utf-8"
        )
    
    def clear(self) -> None:
        self.file_path.write_text("[]", encoding="utf-8")
    
    def _serialize(self, msg: BaseMessage) -> dict:
        return {"type": msg.type, "content": msg.content}
    
    def _deserialize(self, data: dict) -> BaseMessage:
        if data["type"] == "human":
            return HumanMessage(content=data["content"])
        return AIMessage(content=data["content"])
```

### 2.8 LCEL 的 Pipeline 内部是如何实现的？

LCEL 的 `|` 运算符本质上是将多个 Runnable 组合为一个 `RunnableSequence`：

```python
# LCEL 管道
chain = prompt | model | output_parser

# 等价于
from langchain_core.runnables import RunnableSequence
chain = RunnableSequence(first=prompt, middle=[model], last=output_parser)

# RunnableSequence 的 invoke 内部逻辑：
class RunnableSequence:
    def invoke(self, input, config=None):
        # 1. 输入传递给第一个组件
        result = self.first.invoke(input, config)
        # 2. 依次传递中间组件
        for step in self.middle:
            result = step.invoke(result, config)
        # 3. 最后一个组件处理
        result = self.last.invoke(result, config)
        return result
```

### 2.9 FewShotPromptTemplate 的示例格式化过程？

```python
from langchain_core.prompts import FewShotPromptTemplate, PromptTemplate

# 示例数据
examples = [
    {"input": "用乐观的语气写", "output": "今天真是美好的一天！"},
    {"input": "用正式的语气写", "output": "尊敬的客户，您好。"},
]

# 示例模板
example_prompt = PromptTemplate(
    input_variables=["input", "output"],
    template="输入: {input}\n输出: {output}"
)

# FewShot 模板
few_shot_prompt = FewShotPromptTemplate(
    examples=examples,
    example_prompt=example_prompt,
    prefix="请根据以下示例风格回答用户问题：",
    suffix="输入: {user_input}\n输出:",
    input_variables=["user_input"]
)

# 最终生成的 Prompt:
"""
请根据以下示例风格回答用户问题：

输入: 用乐观的语气写
输出: 今天真是美好的一天！

输入: 用正式的语气写
输出: 尊敬的客户，您好。

输入: 帮我写一句问候
输出:
"""
```

### 2.10 LangGraph 如何实现并行执行？

```python
from langgraph.graph import StateGraph
import asyncio

# 多个节点可以并发执行
graph.add_node("search_web", search_web_node)
graph.add_node("query_db", query_db_node)  
graph.add_node("call_api", call_api_node)
graph.add_node("aggregate", aggregate_node)

# 并行分支：这三个节点共享同一个入口
graph.add_edge("analyze", "search_web")
graph.add_edge("analyze", "query_db")
graph.add_edge("analyze", "call_api")

# 聚合点：等待所有并行分支完成
graph.add_edge("search_web", "aggregate")
graph.add_edge("query_db", "aggregate")
graph.add_edge("call_api", "aggregate")

# LangGraph 自动处理 fan-out / fan-in
```

---

## 三、实战场景题（8-10题）

### 3.1 如何让 Agent 同时搜索网页和查数据库再做决策？

```python
@tool
def search_web(query: str) -> str:
    """搜索互联网信息"""
    return f"搜索结果: {query} 相关资讯..."

@tool
def query_database(sql: str) -> str:
    """查询业务数据库"""
    return f"数据库结果: {sql} 查询返回 3 条记录"

tools = [search_web, query_database]

# LangGraph 并行执行
agent = create_react_agent(model=llm, tools=tools)
result = agent.invoke({
    "messages": [("human", "查询最新的行业新闻和本周订单数据")]
})
```

### 3.2 如何实现带记忆的多轮对话 Agent？

```python
from langchain.memory import RedisChatMessageHistory
from langchain.memory import ConversationBufferMemory
from langchain.agents import AgentExecutor, create_tool_calling_agent

# Redis 持久化
history = RedisChatMessageHistory(session_id="user_001")
memory = ConversationBufferMemory(
    chat_memory=history,
    memory_key="chat_history",
    return_messages=True
)

# 创建 Agent
agent = create_tool_calling_agent(llm, tools, prompt)
agent_executor = AgentExecutor(
    agent=agent, 
    tools=tools, 
    memory=memory,
    verbose=True
)

# 多轮交互
agent_executor.invoke({"input": "我叫张三"})
# Agent: 你好张三！有什么可以帮助你的？

agent_executor.invoke({"input": "我刚才说了什么？"})
# Agent: 你刚才说你叫张三。
```

### 3.3 如何用 MCP 将高德地图能力接入 Agent？

```python
# 1. 创建 MCP 客户端连接高德 MCP Server
from langchain_mcp_adapters.client import MCPClient

async with MCPClient("amap_mcp_server.py") as client:
    # 2. 获取工具列表
    tools = await client.list_tools()
    # 返回: geo_query, route_planning, location_search 等

    # 3. 创建 Agent 并使用 MCP 工具
    agent = create_react_agent(model=llm, tools=tools)
    
    result = agent.invoke({
        "messages": [("human", "从北京西站到天安门怎么坐地铁？")]
    })
```

### 3.4 如何用 LangChain Agent 自动编写企业官网？

```python
from langchain_community.tools import PythonREPLTool

# PythonREPLTool 可以执行 Python 代码
tools = [PythonREPLTool()]

agent = create_react_agent(
    model=llm,
    tools=tools,
    state_modifier="你是一个前端开发专家，使用 Python 生成 HTML/CSS/JS 企业官网代码。"
)

result = agent.invoke({
    "messages": [("human", "帮我生成一个科技公司官网首页，包含导航栏、Hero 区域、产品展示和页脚")]
})
```

### 3.5 如何通过 Cursor + GitHub MCP 做二次开发？

Cursor IDE 可以接入 MCP Server，让 AI 直接操作 GitHub：
1. 在 Cursor 的 MCP 配置中添加 GitHub MCP Server
2. AI 可以直接创建 PR、提交代码、管理 Issues
3. 结合高德 MCP，可实现旅行计划自动生成等小项目

### 3.6 如何用 LangGraph 实现 Supervisor 多 Agent 架构？

```python
# Supervisor 节点：决定哪个子 Agent 执行
def supervisor_node(state: AgentState) -> dict:
    messages = state["messages"]
    prompt = f"当前任务: {messages[-1].content}\n可用的专家: code_agent, search_agent, data_agent\n请选择最合适的专家。"
    response = llm.invoke(prompt)
    return {"next_agent": response.content.strip()}

# 各 Agent 节点
def code_agent_node(state: AgentState) -> dict:
    return {"messages": [code_agent.invoke(state["messages"])]}

def search_agent_node(state: AgentState) -> dict:
    return {"messages": [search_agent.invoke(state["messages"])]}

# 构建图
graph.add_node("supervisor", supervisor_node)
graph.add_node("code_agent", code_agent_node)
graph.add_node("search_agent", search_agent_node)

# 条件路由：Supervisor 决定下一步
graph.add_conditional_edges(
    "supervisor",
    lambda state: state["next_agent"],
    {"code_agent": "code_agent", "search_agent": "search_agent"}
)
```

### 3.7 Agent 在生成过程中 Token 过多超出上下文窗口怎么办？

**解决方案：**
1. **滑动窗口（Sliding Window）**：只保留最近 N 轮对话，超出部分丢弃
2. **摘要压缩（Summary Compression）**：用 LLM 压缩旧消息为摘要
3. **向量检索 + 长期记忆**：将历史消息存入向量库，仅检索相关片段
4. **显式 Token 计数**：使用 `tiktoken` 计算 Token 数，超出时自动截断

```python
from langchain.memory import ConversationSummaryBufferMemory

memory = ConversationSummaryBufferMemory(
    llm=llm,
    max_token_limit=2000,  # Token 上限
    memory_key="chat_history",
    return_messages=True
)
# 超过 2000 Token 时自动将旧消息压缩为摘要
```

### 3.8 如何确保 Agent 工具调用的安全性？

| 安全策略 | 说明 | 实现方式 |
|----------|------|----------|
| **输入校验** | 校验工具参数，防止注入攻击 | Pydantic 校验、白名单 |
| **权限控制** | 限制 Agent 可调用的工具范围 | 最小权限原则 |
| **沙箱执行** | 在隔离环境中执行代码 | Docker / subprocess 限制 |
| **人工确认** | 高风险操作需要人工审批 | `human-in-the-loop` |
| **操作审计** | 记录所有工具调用日志 | 完整审计链 |

---

## 四、手写代码题（6-8题）

### 4.1 手写一个基于 LangChain 的 ReAct Agent

```python
from langchain_openai import ChatOpenAI
from langchain_core.tools import tool
from langchain.agents import create_tool_calling_agent, AgentExecutor
from langchain_core.prompts import ChatPromptTemplate

# 1. 定义工具
@tool
def calculate(expression: str) -> str:
    """计算数学表达式"""
    try:
        return str(eval(expression))
    except Exception as e:
        return f"计算错误: {e}"

@tool
def get_current_time() -> str:
    """获取当前时间"""
    from datetime import datetime
    return datetime.now().strftime("%Y-%m-%d %H:%M:%S")

# 2. 初始化 LLM
llm = ChatOpenAI(model="qwen-plus", temperature=0)

# 3. 创建 Prompt
prompt = ChatPromptTemplate.from_messages([
    ("system", "你是一个智能助手，使用工具回答用户问题。"),
    ("placeholder", "{chat_history}"),
    ("human", "{input}"),
    ("placeholder", "{agent_scratchpad}"),
])

# 4. 创建 Agent
agent = create_tool_calling_agent(llm, [calculate, get_current_time], prompt)
agent_executor = AgentExecutor(
    agent=agent,
    tools=[calculate, get_current_time],
    verbose=True,
    max_iterations=10
)

# 5. 调用
result = agent_executor.invoke({"input": "计算 (15 + 27) * 3 等于多少？现在几点了？"})
print(result["output"])
```

### 4.2 手写 LangGraph 自定义 Agent 工作流

```python
from typing import TypedDict, Annotated, Literal
from langgraph.graph import StateGraph, END, START
from langgraph.graph.message import add_messages
from langchain_openai import ChatOpenAI
from langchain_core.messages import HumanMessage, AIMessage
from langchain_core.tools import tool

# 1. 定义状态
class AgentState(TypedDict):
    messages: Annotated[list, add_messages]
    next_step: str

# 2. 初始化模型
llm = ChatOpenAI(model="gpt-4o", temperature=0)

@tool
def search_web(query: str) -> str:
    """搜索网络信息"""
    return f"关于'{query}'的搜索结果..."

tools = [search_web]
llm_with_tools = llm.bind_tools(tools)

# 3. 节点函数
def call_model(state: AgentState) -> dict:
    """LLM 推理节点"""
    response = llm_with_tools.invoke(state["messages"])
    return {"messages": [response]}

def should_continue(state: AgentState) -> Literal["tools", END]:
    """条件判断：是否需要调用工具"""
    last_message = state["messages"][-1]
    if hasattr(last_message, "tool_calls") and last_message.tool_calls:
        return "tools"
    return END

def call_tool(state: AgentState) -> dict:
    """工具执行节点"""
    last_message = state["messages"][-1]
    tool_results = []
    for tc in last_message.tool_calls:
        if tc["name"] == "search_web":
            result = search_web.invoke(tc["args"])
            tool_results.append({
                "role": "tool",
                "content": result,
                "tool_call_id": tc["id"]
            })
    return {"messages": tool_results}

# 4. 构建图
workflow = StateGraph(AgentState)
workflow.add_node("agent", call_model)
workflow.add_node("tools", call_tool)

workflow.add_edge(START, "agent")
workflow.add_conditional_edges("agent", should_continue)
workflow.add_edge("tools", "agent")

app = workflow.compile()

# 5. 执行
result = app.invoke({
    "messages": [HumanMessage(content="搜索一下最新的 AI 新闻")]
})
```

### 4.3 手写一个带记忆持久化的多轮对话系统

```python
import json
from pathlib import Path
from langchain_openai import ChatOpenAI
from langchain_core.messages import HumanMessage, AIMessage
from langchain_core.prompts import ChatPromptTemplate, MessagesPlaceholder
from langchain_core.runnables.history import RunnableWithMessageHistory

# 1. 文件持久化历史
class FileHistory:
    def __init__(self, file_path: str):
        self.file_path = Path(file_path)
        self.file_path.parent.mkdir(parents=True, exist_ok=True)
        if not self.file_path.exists():
            self._save([])
    
    def _load(self):
        return json.loads(self.file_path.read_text(encoding="utf-8"))
    
    def _save(self, messages):
        self.file_path.write_text(
            json.dumps(messages, ensure_ascii=False, indent=2),
            encoding="utf-8"
        )
    
    def add_message(self, msg):
        messages = self._load()
        messages.append({"role": msg.type, "content": msg.content})
        self._save(messages)
    
    def get_messages(self):
        return self._load()

# 2. 创建链
prompt = ChatPromptTemplate.from_messages([
    ("system", "你是一个智能助手。"),
    MessagesPlaceholder(variable_name="history"),
    ("human", "{input}")
])

llm = ChatOpenAI(model="qwen-plus")
chain = prompt | llm

# 3. 包装历史
histories = {}
def get_session_history(session_id):
    if session_id not in histories:
        histories[session_id] = FileHistory(f"./history/{session_id}.json")
    return histories[session_id]

chain_with_history = RunnableWithMessageHistory(
    chain,
    get_session_history,
    input_messages_key="input",
    history_messages_key="history"
)

# 4. 调用
response = chain_with_history.invoke(
    {"input": "我叫小明"},
    config={"configurable": {"session_id": "user_001"}}
)
```

### 4.4 手写 MCP 本地工具 Server

```python
# mcp_server.py - 本地文件系统操作 MCP Server
import json
import os
import sys
from typing import Any

class MCPServer:
    """简易 MCP Server 实现（stdio 协议）"""
    
    def __init__(self):
        self.tools = {
            "read_file": {
                "name": "read_file",
                "description": "读取文件内容",
                "inputSchema": {
                    "type": "object",
                    "properties": {
                        "path": {"type": "string", "description": "文件路径"}
                    },
                    "required": ["path"]
                }
            },
            "list_dir": {
                "name": "list_dir",
                "description": "列出目录内容",
                "inputSchema": {
                    "type": "object",
                    "properties": {
                        "path": {"type": "string", "description": "目录路径"}
                    },
                    "required": ["path"]
                }
            }
        }
    
    def handle_request(self, request: dict) -> dict:
        method = request.get("method")
        params = request.get("params", {})
        
        if method == "tools/list":
            return {"tools": list(self.tools.values())}
        elif method == "tools/call":
            tool_name = params.get("name")
            args = params.get("arguments", {})
            return self.execute_tool(tool_name, args)
        return {"error": f"Unknown method: {method}"}
    
    def execute_tool(self, name: str, args: dict) -> Any:
        if name == "read_file":
            with open(args["path"], "r", encoding="utf-8") as f:
                return {"content": [{"type": "text", "text": f.read()}]}
        elif name == "list_dir":
            files = os.listdir(args["path"])
            return {"content": [{"type": "text", "text": "\n".join(files)}]}
        return {"error": f"Unknown tool: {name}"}
    
    def run(self):
        """通过 stdio 接收和响应 JSON-RPC 消息"""
        for line in sys.stdin:
            try:
                request = json.loads(line.strip())
                response = self.handle_request(request)
                response["id"] = request.get("id")
                sys.stdout.write(json.dumps(response) + "\n")
                sys.stdout.flush()
            except Exception as e:
                error_resp = {"error": str(e), "id": None}
                sys.stdout.write(json.dumps(error_resp) + "\n")
                sys.stdout.flush()

if __name__ == "__main__":
    server = MCPServer()
    server.run()
```

### 4.5 手写带流式输出的 Agent

```python
from langchain_openai import ChatOpenAI
from langchain_core.tools import tool
from langchain.agents import create_tool_calling_agent, AgentExecutor

@tool
def get_weather(city: str) -> str:
    """查询天气"""
    return f"{city} 25度，晴"

llm = ChatOpenAI(model="qwen-plus", temperature=0, streaming=True)
agent = create_tool_calling_agent(llm, [get_weather])
agent_executor = AgentExecutor(agent=agent, tools=[get_weather])

# 流式输出
for chunk in agent_executor.stream({"input": "北京的天气怎么样？"}):
    if "output" in chunk:
        print(chunk["output"], end="", flush=True)
    elif "actions" in chunk:
        print(f"\n[调用工具: {chunk['actions'][0].tool}]")
```

### 4.6 手写一个参数校验的 @tool 工具

```python
from typing import Optional
from langchain_core.tools import tool
from pydantic import BaseModel, Field

# 方式一：使用 args_schema
class CreateUserInput(BaseModel):
    """创建用户参数"""
    name: str = Field(description="用户名")
    age: int = Field(ge=0, le=150, description="年龄")
    email: Optional[str] = Field(default=None, description="邮箱")

@tool(args_schema=CreateUserInput)
def create_user(name: str, age: int, email: Optional[str] = None) -> str:
    """创建新用户"""
    return f"用户 {name}({age}岁) 创建成功"

# 方式二：函数签名自动推断
@tool
def update_user(user_id: int, name: str = None) -> str:
    """
    更新用户信息
    Args:
        user_id: 用户ID
        name: 新用户名（可选）
    """
    return f"用户 {user_id} 更新成功"
```

---

## 五、系统设计题（3-5题）

### 5.1 设计一个企业级智能客服 Agent 系统

**需求：** 7x24 在线客服，支持多轮对话、查询订单、退换货、接入后端 ERP 系统。

**架构设计：**
```
用户 (Web/App)
    │
    ▼
API Gateway
    │
    ├──→ 会话管理（Redis 缓存）
    │
    ├──→ Agent Core（LangGraph 工作流）
    │       ├── 意图识别节点（分类：查询/退换货/投诉）
    │       ├── 知识库检索节点（FAQ + 向量库）
    │       ├── 业务查询节点（调用订单 API）
    │       └── 人工转接节点（超阈值转人工）
    │
    ├──→ Tools 层
    │       ├── 订单查询 MCP Server
    │       ├── 退换货 API
    │       ├── 知识库 RAG
    │       └── 工单系统 API
    │
    └──→ 持久化层
            ├── Redis（会话记忆、缓存）
            ├── MongoDB（历史对话存储）
            └── 向量库（FAQ 知识库）
```

**关键设计要点：**
| 维度 | 方案 |
|------|------|
| 上下文管理 | Redis 存储短期记忆，MongoDB 持久化长期 |
| Token 控制 | ConversationSummaryBufferMemory 自动压缩 |
| 并发处理 | LangGraph 并行执行多个 Tools |
| 降级策略 | 高峰期关闭非核心工具，仅保留 FAQ |
| 人工兜底 | 3 次不满意转人工，置信度 < 0.6 转人工 |

### 5.2 设计一个多 Agent 协作的代码审查系统

**Agent 分工：**
| Agent 名称 | 职责 | Tools |
|-----------|------|-------|
| **Reviewer** | 代码审查主 Agent | 读取文件、分析代码 |
| **Security** | 安全检查 Agent | 扫描漏洞、检查密钥泄漏 |
| **Tester** | 测试 Agent | 运行单元测试、生成测试用例 |
| **Reporter** | 报告生成 Agent | 生成 Markdown 报告 |

**工作流程：**
```
PR 提交 → Reviewer 分析代码变更
           ├── 检测到数据库操作 → 调用 Security Agent
           ├── 检测到新功能 → 调用 Tester Agent
           └── 汇总结果 → Reporter 生成审查报告
```

### 5.3 设计 Agent 记忆系统架构

```
                    ┌─────────────────────┐
                    │      Agent Core      │
                    └──────┬──────────────┘
                           │
              ┌────────────┼────────────┐
              │            │            │
         ┌────▼───┐  ┌────▼───┐  ┌────▼───┐
         │ Working │  │ Short  │  │  Long  │
         │ Memory  │  │ Memory │  │ Memory │
         └────────┘  └────────┘  └────────┘
                         │             │
                    ┌────▼───┐    ┌────▼──────────┐
                    │ Redis  │    │  Vector DB     │
                    │        │    │  (Milvus/PG)   │
                    └────────┘    └────┬──────────┘
                                       │
                               ┌───────▼───────┐
                               │ Embedding API  │
                               └───────────────┘
```

**记忆检索策略：**
1. 先查 Working Memory（当前推理上下文）
2. 再查 Short-Term Memory（Redis 最近 N 条消息）
3. 最后查 Long-Term Memory（向量库语义检索）
4. 分层合并后注入 Prompt

---

## 六、常见坑点与最佳实践

| 坑点 | 问题描述 | 最佳实践 |
|------|----------|----------|
| **Tool 循环死锁** | Agent 反复调用同一工具无法结束 | 设置 `max_iterations=10` + `early_stopping_method="generate"` |
| **Token 爆炸** | 多轮对话后历史消息过多超出上下文窗口 | 使用 `ConversationSummaryBufferMemory` 自动压缩 |
| **工具参数错误** | LLM 生成的工具参数不符合预期格式 | 使用 Pydantic `args_schema` 做强类型校验 |
| **MCP 连接超时** | 远程 MCP Server 响应慢导致 Agent 超时 | 设置合理的 `timeout` + 重试机制 + 熔断降级 |
| **模型幻觉** | LLM 捏造工具返回结果而不是真正调用 | 强制约束：工具调用必须真实执行，不允许"假调用" |
| **上下文污染** | 不同用户的对话串号 | 严格使用 `session_id` 隔离，每个请求携带唯一 ID |
| **并行数据竞争** | 多 Agent 同时修改同一状态 | LangGraph State 使用 `Anacomated` 注解，add_messages 合并策略 |
| **Prompt 注入** | 用户输入中嵌入恶意指令改变 Agent 行为 | 用户输入与系统指令分离，使用提示词边界标记（如 `<user_input>`） |
| **性能瓶颈** | 每次请求都重新加载模型/工具 | 使用模型池化、工具懒加载、连接池复用 |
| **记忆不一致** | FileSaver 并发写入导致 JSON 损坏 | 加文件锁，或改用 Redis/MongoDB 等专业存储 |
| **流式中断** | 流式输出时工具调用与文本交替出错 | 确保 LLM 支持流式 Function Calling，使用最新 SDK |
| **跨平台路径** | Windows/macOS 路径分隔符不一致 | 使用 `pathlib.Path` 统一路径处理 |

---

## 七、面试回答模板（Top 5 高频题的结构化回答模板）

### 模板 1：请解释 AI Agent 的架构

> **回答思路：大脑 + 感知 + 规划 + 记忆 + 行动 五层架构。**

"AI Agent 的架构可以拆分为五个核心模块。第一是 **Brain（大脑）**，也就是大模型本身，负责推理和决策。第二是 **Perception（感知）**，接收多模态输入，比如文本、图片、语音。第三是 **Planning（规划）**，将复杂任务分解为子任务，典型实现是 ReAct 模式的 Thought-Action-Observation 循环。第四是 **Memory（记忆）**，分为工作记忆、短期记忆和长期记忆三层。第五是 **Action（行动）**，通过工具调用执行具体操作，比如调用 API、执行代码、操作数据库。

这五个模块的协作流程是：**感知输入 → 大脑推理 → 规划拆解 → 记忆检索 → 行动执行 → 观察结果 → 循环直到任务完成**。以 LangChain 为例，AgentExecutor 就封装了这个完整流程。"

### 模板 2：ReAct 模式和 Function Calling 的区别

> **回答思路：先定义各自概念，再对比关系。**

"ReAct 是一种**推理框架**，强调 Thought → Action → Observation 的循环迭代模式。而 Function Calling 是大模型的**原生能力**，让模型能够输出结构化的工具调用参数。

两者的关系是：ReAct 定义了 Agent 的行为范式，Function Calling 是实现 Action 步骤的具体技术手段。在 LangChain 中，AgentExecutor 采用 ReAct 模式驱动循环，而底层的工具调用依赖模型的 Function Calling 能力。

简单说：**ReAct 是战略层的循环范式，Function Calling 是战术层的工具调用实现。**"

### 模板 3：LangChain vs LangGraph 的选择

> **回答思路：适用场景不同，不是替代关系。**

"LangChain Agent 和 LangGraph 是**互补关系**而非替代关系。LangChain Agent 适合**简单的线性对话场景**，使用 AgentExecutor 即可快速实现 Thought-Action-Observation 循环。但它在以下场景有局限：无法处理条件分支、无法并行执行、状态管理能力弱。

LangGraph 则适合**复杂的多步骤工作流**，它用有向图建模流程，支持条件路由（Conditional Edge）、并行分支（Fan-out/Fan-in）、持久化状态。如果一个场景需要 '根据条件走不同流程' 或 '同时查询多个数据源'，LangGraph 是更好的选择。

我的实践是：**80% 的简单场景用 LangChain Agent 即可，20% 的复杂工作流用 LangGraph。**"

### 模板 4：如何实现 Agent 记忆持久化

> **回答思路：分层设计 + 具体技术选型。**

"Agent 记忆持久化我采用**三层架构**。第一层是**工作记忆**，直接利用 LLM 上下文窗口，不需要额外存储。第二层是**短期记忆**，使用 Redis 存储最近 N 轮对话，通过 `RedisChatMessageHistory` 实现，TTL 设置为 24 小时。第三层是**长期记忆**，使用 MongoDB 或向量库存储跨会话的用户画像和知识记忆。

关键技术点：
- **窗口管理**：`ConversationBufferWindowMemory(k=10)` 只保留最近 10 轮
- **摘要压缩**：超过 Token 上限时，用 LLM 将旧消息压缩为摘要
- **持久化方案**：小规模用 FileSaver（JSON 文件），中等规模用 Redis，大规模用 MongoDB
- **检索策略**：先查短期记忆，再查长期记忆（向量检索），分层合并"

### 模板 5：MCP 协议解决了什么问题

> **回答思路：标准化、解耦、生态互通。**

"MCP（Model Context Protocol）解决了大模型与外部工具集成的**碎片化问题**。在 MCP 出现之前，每个 Agent 框架都有自己的工具接入规范，工具开发者需要为不同框架分别适配。MCP 提供了一套**统一标准**，工具一次接入，所有兼容的 Agent 框架都能使用。

核心价值有三点：
1. **标准化**：统一的工具发现（`tools/list`）、调用（`tools/call`）和资源访问协议
2. **解耦**：工具开发者只需要实现 MCP Server，不需要关心上层 Agent 框架的细节
3. **生态互通**：LangChain、Cursor IDE、Claude Desktop 等都支持 MCP，生态正在形成"

---

## 八、快速查漏补缺 Checklist

### 基础概念
- [ ] 能清晰解释 AI Agent 的定义和五大核心组件（Brain + Perception + Planning + Memory + Action）
- [ ] 能区分 LLM / RAG / Agent 三者的能力层级
- [ ] 理解 ReAct 循环的完整流程（Thought → Action → Observation）
- [ ] 知道 Function Calling 与 ReAct 的关系
- [ ] 理解 MCP 协议的作用和核心方法（tools/list, tools/call）

### LangChain 框架
- [ ] 会用 `@tool` 装饰器注册工具，理解 `args_schema` 参数控制
- [ ] 掌握 PromptTemplate / ChatPromptTemplate / FewShotPromptTemplate 的用法和区别
- [ ] 掌握 StrOutputParser / JsonOutputParser / PydanticOutputParser 等 Output Parser
- [ ] 理解 LCEL 管道语法（`|`）和内部 RunnableSequence 实现
- [ ] 理解 Runnables 的核心方法：invoke / ainvoke / stream / batch

### 多轮对话与记忆
- [ ] 会用 ChatMessageHistory 管理历史上下文
- [ ] 会用 RunnableWithMessageHistory 构建带历史的多轮链
- [ ] 理解三层记忆架构：工作记忆 / 短期记忆 / 长期记忆
- [ ] 会用 RedisSaver 实现会话持久化
- [ ] 会用 MongoDB 实现持久化
- [ ] 能手写 FileSaver 实现文件级持久化

### LangGraph 工作流
- [ ] 理解 State / Node / Edge / Conditional Edge 核心概念
- [ ] 会用 StateGraph 构建自定义工作流
- [ ] 会用 `create_react_agent` 快速创建 Agent
- [ ] 理解条件路由和并行执行的图结构设计
- [ ] 理解 Supervisor 多 Agent 协作模式

### MCP 协议与工具集成
- [ ] 理解 MCP stdio 和 SSE 两种传输协议
- [ ] 会用 LangChain MCP Adapters 创建客户端
- [ ] 理解 Playwright MCP / GitHub MCP 的工具能力
- [ ] 能手写简单的 MCP Server（stdio 协议）
- [ ] 理解 Shell / PowerShell MCP 工具的实现方法

### 工程实践
- [ ] 知道如何避免 Tool 循环死锁
- [ ] 知道如何防止 Token 爆炸
- [ ] 理解 Agent 安全策略（输入校验、权限控制、沙箱、人工确认）
- [ ] 会处理流式输出场景
- [ ] 了解跨平台路径和编码问题

### 实战项目
- [ ] 完成过 Agent + MCP 高德地图位置服务
- [ ] 完成过 Agent + Playwright 浏览器自动化
- [ ] 完成过 Agent + GitHub MCP 二次开发
- [ ] 完成过多轮对话 + 记忆持久化完整项目
- [ ] 完成过 LangGraph 自定义工作流项目

---

> 🎯 **面试核心策略**：
> 1. **原理要深**：ReAct 循环的每一步都要讲清楚数据流
> 2. **代码要熟**：`@tool`、LCEL、LangGraph StateGraph 要能手写
> 3. **架构要全**：分层记忆、MCP 协议、多 Agent 协作要能画架构图
> 4. **场景要真**：用具体项目经验说话，比如高德 MCP 旅行计划、GitHub MCP 二次开发
> 5. **坑点要清**：Token 爆炸、死循环、Prompt 注入等实战问题要能应对
