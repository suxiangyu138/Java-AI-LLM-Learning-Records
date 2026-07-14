# AI-Agent-快速吃透

> **核心摘要**：AI Agent = 以大模型为大脑、具备自主思考与工具调用能力的智能体，与普通 LLM 和 RAG 形成三层递进关系。本文用极简方式梳理 Agent 的核心架构、运行闭环、分类方式与落地场景，并提供了基于 LangChain + Ollama 的最简可运行代码。

## 前置阅读

- [[AI Agent核心知识点]]
- [[ReAct模式与Function-Calling实战]]

---

## 一、AI Agent 核心定义

### 1.1 什么是 AI Agent

**AI Agent** 是以大模型（LLM）为大脑，具备自主思考、任务拆解、工具调用、循环执行、记忆规划的智能体。

| 类型 | 能力 | 局限性 |
|---|---|---|
| 普通 LLM | 语言理解与生成 | 被动回答，无行动能力 |
| RAG | 知识库问答 | 局限于检索，无规划与工具 |
| **Agent** | 自主规划 + 工具调用 + 循环执行 | 需要强推理模型支撑 |

### 1.2 一句话理解

> LLM 是脑子，Agent 是完整的人（脑子 + 手脚 + 规划 + 执行力）。

---

## 二、五大核心组件

| 组件 | 职责 |
|---|---|
| **LLM 大脑** | 推理、判断、思考、决策下一步动作 |
| **规划能力（Planner）** | 将复杂任务拆解为多步子任务 |
| **工具调用（Tools）** | 联网搜索、查数据库、操作向量库、运行代码、调用第三方 API |
| **记忆系统（Memory）** | 记住历史对话、执行步骤、用户偏好和上下文 |
| **执行器与反思机制** | 执行工具、出错复盘、自我修正、迭代答案 |

---

## 三、Agent 标准运行闭环

经典 **ReAct 架构** 是业界通用的 Agent 运行模式：

```
1. Thought（思考）：分析需要做什么、需要什么工具
2. Action（行动）：调用对应工具、传入参数
3. Observation（观察）：获取工具返回结果
4. 循环迭代：不够则继续思考 + 调用工具，直到任务完成
5. Final Answer（输出）：整合全部结果，给出最终回答
```

> **重点**：Agent 的核心特征在于循环执行，而非单次问答。

---

## 四、LLM / RAG / Agent 三者层级关系

```
LLM（基础大脑）
├── 加知识库 → RAG（专属问答）
└── 加规划 + 工具 + 记忆 → Agent（自主干活）
```

| 维度 | LLM | RAG | Agent |
|---|---|---|---|
| 核心能力 | 语言理解 + 生成 | 检索 + 生成 | 规划 + 工具 + 记忆 + 循环 |
| 行动能力 | 无 | 无 | 有（调用 API、执行代码等） |
| 知识来源 | 训练数据 | 训练数据 + 外部知识库 | 训练数据 + 工具 + 记忆 |

---

## 五、主流 Agent 分类

| 类型 | 特点 | 适用场景 |
|---|---|---|
| **工具型 Agent** | 调用外部工具：搜索、数据库、API、代码 | 办公自动化、数据查询、资料搜集 |
| **对话智能 Agent** | 多轮长期记忆、角色扮演、个性化 | 个人助理、客服 |
| **任务型 Agent** | 自动完成复杂长流程 | 学习规划、文档整理、项目拆解 |
| **多智能体（Multi-Agent）** | 多个 Agent 分工协作 | 企业级复杂系统 |

---

## 六、落地场景

- 自动查资料、整理学习笔记
- 连接 MySQL / Redis / ES 查询业务数据
- 结合 Milvus 做 RAG + 工具混合问答
- 自动写代码、跑脚本、排查错误
- 定时任务、流程自动化、个人助理
- 后端系统内置智能客服、运维助手

---

## 七、限制与短板

| 问题 | 说明 |
|---|---|
| 长链推理不稳定 | 复杂逻辑容易断、思考跑偏 |
| 工具参数易出错 | 需要约束和校验 |
| 工具滥用 | 重复调用，需要限流 + 规则 |
| 依赖模型能力 | 越聪明的模型，Agent 越强 |

---

## 八、最简可运行代码（LangChain + Ollama）

```python
from langchain_openai import ChatOpenAI
from langchain.agents import create_react_agent, AgentExecutor
from langchain_core.tools import tool
from langchain_core.prompts import PromptTemplate

# 1. 接入本地 Ollama 大模型
llm = ChatOpenAI(
    base_url="http://localhost:11434/v1",
    api_key="ollama",
    model="qwen"
)

# 2. 自定义工具
@tool
def search_info(query: str) -> str:
    """用于查询计算机技术知识点"""
    return f"【工具查询结果】{query} 的核心原理已整理完成"

# 3. 绑定工具
tools = [search_info]

# 4. 初始化 Agent
prompt = PromptTemplate.from_template("""
Answer the following question as best you can.
You have access to the following tools:
{tools}
Use the following format:
Question: the input question
Thought: reasoning
Action: tool name
Action Input: tool parameter
Observation: tool result
...
Final Answer: final answer
Question: {input}
""")
agent = create_react_agent(llm, tools, prompt)
agent_executor = AgentExecutor(agent=agent, tools=tools, verbose=True)

# 5. 执行自主任务
result = agent_executor.invoke({"input": "详细解释 RabbitMQ"})
print(result["output"])
```

---

## 九、技术栈组合建议

```
本地大模型 Ollama
├── RAG 知识库：LangChain + Milvus 向量库
└── AI Agent：工具调用 + 任务规划 + 自动化
```

| 场景 | 推荐方案 |
|---|---|
| 日常复习 | RAG 挂载课程笔记 |
| 开发提效 | Agent 写代码、查文档、排错 |
| 项目落地 | RAG + Agent 混合架构（工业主流） |

---

## 核心要点回顾

- LLM = 只会说；Agent = 会思考、会干活
- Agent 三大核心：规划 + 工具 + 记忆
- 运行模式：思考 → 行动 → 观察 → 循环
- RAG 是查资料，Agent 是做事情
- 开发框架首选 LangChain

---

## 参考资料

1. LangChain 官方文档. Agent 快速入门指南
2. Ollama 官方文档. 本地大模型部署与使用
3. LangChain 社区. ReAct Agent 实现示例
