# AI Agent 主流框架深度对比

> **核心摘要**：LangChain、LangGraph、AutoGen、CrewAI、Dify 是当前主流的五大 Agent 框架，分别定位于通用 Agent、状态图编排、多 Agent 对话、角色化协作和低代码平台。本文从定位、开发方式、编排模型、状态管理、多 Agent 支持、生产成熟度等维度进行全面对比，并提供选型决策树。

## 前置阅读

- [[AI Agent核心知识点]]
- [[LangGraph实战：状态图与多Agent工作流]]
- [[AI Agent Java后端实战]]

---

## 一、框架层级全景

```
Agent 框架分为三个层级：

层级 1：底层 LLM 调用
  - LLM API 直接调用（OpenAI / Claude / 本地模型）

层级 2：Agent 编排框架
  - LangChain Agent / LangGraph / AutoGen / CrewAI

层级 3：无代码 / 低代码平台
  - Dify / Coze / FastGPT / n8n
```

---

## 二、核心框架对比总表

| 维度 | LangChain Agent | LangGraph | AutoGen | CrewAI | Dify |
|---|---|---|---|---|---|
| **定位** | 通用 Agent 框架 | 状态图编排引擎 | 多 Agent 对话 | 角色化多 Agent | 低代码 AI 平台 |
| **开发方式** | Python 代码 | Python 代码 | Python 代码 | Python 代码 | 可视化拖拽 |
| **编排模型** | Chain + Tool | **有向图（Graph）** | 多 Agent 对话 | Crew（团队） | Flow 工作流 |
| **单 Agent** | 支持 | 支持 | 支持 | 不支持（最少 1 个） | 支持 |
| **多 Agent** | 不支持 | 支持 | **核心能力** | **核心能力** | 支持 |
| **状态管理** | 弱（Memory 类） | **强（StateGraph）** | 中等 | 弱 | 弱 |
| **人机协作** | 弱 | 支持中断+恢复 | 支持 | 支持 | 支持 |
| **流式响应** | 支持 | 支持 | 支持 | 支持 | 支持 |
| **Java 支持** | 社区版 | 无 | 无 | 无 | HTTP API |
| **学习曲线** | 中等 | 较陡 | 中等 | 低 | 极低 |
| **生产成熟度** | 高 | 高 | 中 | 中 | 非常高 |
| **适用场景** | 通用 | 复杂工作流 | 科研/实验 | 角色分工 | 快速业务落地 |

---

## 三、LangChain Agent

### 3.1 核心特点

```python
from langchain.agents import create_tool_calling_agent, AgentExecutor

agent = create_tool_calling_agent(llm, tools, prompt)
executor = AgentExecutor(agent=agent, tools=tools, max_iterations=10)
result = executor.invoke({"input": "今天的北京天气怎么样？"})
```

| 优缺点 | 说明 |
|---|---|
| **优点** | 生态最丰富（600+ 集成）、社区最大、Python + JS 双语言支持 |
| **缺点** | 过度抽象代码难调试、Agent 执行过程不透明、复杂分支/并行能力弱 |

---

## 四、LangGraph

### 4.1 核心概念

```python
from langgraph.graph import StateGraph, END
from typing import TypedDict

class AgentState(TypedDict):
    messages: list
    next_action: str
    result: str

def think_node(state):
    response = llm.invoke(state["messages"])
    return {"next_action": response.action}

def tool_node(state):
    result = execute_tool(state["messages"][-1]["tool_call"])
    return {"messages": [...], "result": result}

def should_continue(state):
    if state["next_action"] == "final_answer":
        return END
    return "tool_node"

workflow = StateGraph(AgentState)
workflow.add_node("think", think_node)
workflow.add_node("tool", tool_node)
workflow.add_edge("think", "tool")
workflow.add_conditional_edges("think", should_continue)
workflow.set_entry_point("think")
app = workflow.compile()
```

| 优缺点 | 说明 |
|---|---|
| **优点** | 状态流转完全可控、支持条件分支/循环/并行、每个节点可中断调试 |
| **缺点** | 学习曲线较陡、仅 Python 生态、多 Agent 需自行搭建 |

---

## 五、AutoGen（微软）

### 5.1 核心概念

```
AutoGen = 多个 Agent 之间进行对话协作

UserProxy Agent（代表用户，可执行代码）
    ↕ 对话
Assistant Agent（LLM，负责推理和计划）
    ↕ 对话
Executor Agent（执行特定任务）
```

```python
from autogen import AssistantAgent, UserProxyAgent

assistant = AssistantAgent(
    name="assistant",
    llm_config={"config_list": [{"model": "gpt-4"}]}
)
user_proxy = UserProxyAgent(
    name="user_proxy",
    code_execution_config={"work_dir": "coding", "use_docker": False}
)
user_proxy.initiate_chat(
    assistant,
    message="帮我写一个 Python 爬虫爬取今日热榜"
)
```

| 优缺点 | 说明 |
|---|---|
| **优点** | 多 Agent 对话协作天然、微软维护与 Azure 集成好、支持代码执行沙箱 |
| **缺点** | 学习曲线最陡、对话式设计不够灵活、中文文档少 |

---

## 六、CrewAI

### 6.1 核心概念

```python
from crewai import Agent, Task, Crew

researcher = Agent(
    role="研究员",
    goal="研究指定主题并收集信息",
    backstory="你是一个资深研究员，擅长互联网调研",
    tools=[web_search_tool],
    llm=llm
)

writer = Agent(
    role="写手",
    goal="将研究结果写成清晰的报告",
    backstory="你是专业的技术文档写手",
    llm=llm
)

research_task = Task(description="调研 2026 年 AI Agent 发展趋势", agent=researcher)
write_task = Task(description="将调研结果写成 500 字报告", agent=writer)

crew = Crew(agents=[researcher, writer], tasks=[research_task, write_task])
result = crew.kickoff()
```

| 优缺点 | 说明 |
|---|---|
| **优点** | 角色化设计直观易懂、API 简洁上手快、天然支持多 Agent |
| **缺点** | 框架较重不可控、复杂工作流支持弱、生态较小 |

---

## 七、Dify（低代码平台）

### 7.1 特点

```
Dify = 可视化 Agent 构建 + 知识库 + 工作流编排

适用：
  - 不需要写代码就能建 Agent
  - Java 后端通过 HTTP API 调用
  - 快速 MVP 验证
  - 非技术人员也能使用

不适用：
  - 需要复杂自定义逻辑
  - 需要嵌入 Java 代码中
```

| 优缺点 | 说明 |
|---|---|
| **优点** | 零代码/低代码、知识库+工作流内置、一键部署对外 API、生产就绪 |
| **缺点** | 自定义逻辑受限、不适合复杂 Agent 逻辑、收费版功能限制 |

---

## 八、选型决策树

```
你的需求是什么？
│
├── 需要快速出 Demo / 原型
│   └── 选 Dify（拖拽就完事）
│
├── Java 后端直接集成
│   ├── 简单 Agent → LangChain（有 Java 版）
│   └── 复杂 Agent → Dify HTTP API / Spring AI
│
├── 单 Agent + 简单工具调用
│   └── 选 LangChain Agent（生态最好）
│
├── 单 Agent + 复杂工作流（条件分支/循环/并行）
│   └── 选 LangGraph（状态图控制一切）
│
├── 多 Agent 角色协作（研究员+写手+审核员）
│   └── 选 CrewAI（上手最快）
│
├── 多 Agent 自由对话 + 科研实验
│   └── 选 AutoGen（最灵活）
│
└── 企业级 Java 微服务体系
    └── 选 Spring AI + Dify / MCP Server 暴露微服务
```

---

## 九、Java 生态补充

```xml
<!-- Spring AI：Spring 官方的 AI 集成 -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter</artifactId>
</dependency>
```

```java
@RestController
public class AgentController {
    @Autowired
    private ChatClient chatClient;

    @PostMapping("/agent/chat")
    public String chat(@RequestBody String query) {
        return chatClient.prompt()
            .system("你是一个客服助手，可以调用以下工具...")
            .tools(new SearchTool(), new OrderTool())
            .user(query)
            .call()
            .content();
    }
}
```

---

## 核心要点回顾

- LangChain Agent = 万能瑞士军刀，生态最好，但以单 Agent 为主
- LangGraph = 状态图引擎，复杂工作流的终极方案
- CrewAI = 跑团队（研究员+写手+审核），上手最快
- AutoGen = 多 Agent 自由对话，适合实验和科研
- Dify = 拖拽建 Agent，Java 通过 HTTP API 调用
- Java 首选 = Spring AI + Dify / MCP 暴露微服务为工具

---

## 参考资料

1. LangChain 官方文档. Agent 模块与架构
2. LangGraph 官方文档. StateGraph 与多 Agent 编排
3. Microsoft Research. AutoGen 框架文档
4. CrewAI 官方文档. 角色化 Agent 协作
5. Dify 官方文档. 低代码 AI 平台
6. Spring AI 官方文档. Java 生态集成指南
