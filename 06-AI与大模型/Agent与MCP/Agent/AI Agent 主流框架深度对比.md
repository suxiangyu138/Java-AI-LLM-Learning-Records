# AI Agent 主流框架深度对比（Java 后端 + AI 全栈实战版）

> **文档定位**：AI Agent 核心技术文档 | 框架选型指南
> **核心问题**：LangChain、LangGraph、AutoGen、CrewAI、Dify 到底选哪个？

---

## 一、框架全景图

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
| **单 Agent** | ✅ | ✅ | ✅ | ❌（最小 1 个） | ✅ |
| **多 Agent** | ❌ | ✅ | ✅ 核心能力 | ✅ 核心能力 | ✅ |
| **状态管理** | 弱（Memory 类） | **强（StateGraph）** | 中等 | 弱 | 弱 |
| **人机协作** | 弱 | ✅ 中断+恢复 | ✅ | ✅ | ✅ |
| **流式响应** | ✅ | ✅ | ✅ | ✅ | ✅ |
| **Java 支持** | 社区版 | 无 | 无 | 无 | HTTP API |
| **学习曲线** | 中等 | 较陡 | 中等 | 低 | 极低 |
| **生产成熟度** | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| **适用场景** | 通用 | 复杂工作流 | 科研/多 Agent 实验 | 角色分工作业 | 快速业务落地 |

---

## 三、LangChain Agent

### 3.1 核心特点

```python
# LangChain Agent = LLM + Tools + 循环
from langchain.agents import create_tool_calling_agent, AgentExecutor

agent = create_tool_calling_agent(llm, tools, prompt)
executor = AgentExecutor(agent=agent, tools=tools, max_iterations=10)

result = executor.invoke({"input": "今天的北京天气怎么样？"})
```

**优点**：
- 生态最丰富（600+ 集成）
- 社区最大，问题好查
- Python + JS 双语言支持

**缺点**：
- 过度抽象，代码难调试
- Agent 执行过程不透明
- 复杂分支/并行能力弱

---

## 四、LangGraph

### 4.1 核心概念

```python
from langgraph.graph import StateGraph, END
from typing import TypedDict

# 1. 定义状态
class AgentState(TypedDict):
    messages: list
    next_action: str
    result: str

# 2. 定义节点
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

# 3. 建图
workflow = StateGraph(AgentState)
workflow.add_node("think", think_node)
workflow.add_node("tool", tool_node)
workflow.add_edge("think", "tool")
workflow.add_conditional_edges("think", should_continue)
workflow.set_entry_point("think")

app = workflow.compile()
```

**优点**：
- 状态流转完全可控
- 支持条件分支、循环、并行
- 每个节点可中断调试
- 适合复杂工作流

**缺点**：
- 学习曲线较陡
- 仅 Python 生态
- 多 Agent 需自己搭

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

### 5.2 示例

```python
from autogen import AssistantAgent, UserProxyAgent

# 助手 Agent（出主意）
assistant = AssistantAgent(
    name="assistant",
    llm_config={"config_list": [{"model": "gpt-4"}]}
)

# 用户代理（能执行代码）
user_proxy = UserProxyAgent(
    name="user_proxy",
    code_execution_config={"work_dir": "coding", "use_docker": False}
)

# 发起对话，多轮自动协作
user_proxy.initiate_chat(
    assistant,
    message="帮我写一个 Python 爬虫爬取今日热榜"
)
```

**优点**：
- 多 Agent 对话协作天然
- 微软维护，与 Azure 集成好
- 支持代码执行沙箱

**缺点**：
- 学习曲线最陡
- 对话式设计不够灵活
- 中文文档少

---

## 六、CrewAI

### 6.1 核心概念

```python
from crewai import Agent, Task, Crew

# 定义角色 Agent
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

# 定义任务
research_task = Task(
    description="调研 2026 年 AI Agent 发展趋势",
    agent=researcher
)

write_task = Task(
    description="将调研结果写成 500 字报告",
    agent=writer
)

# 启动 Crew
crew = Crew(agents=[researcher, writer], tasks=[research_task, write_task])
result = crew.kickoff()
```

**优点**：
- 角色化设计直观易懂
- API 简洁，上手快
- 天然支持多 Agent

**缺点**：
- 框架较重，不可控
- 复杂工作流支持弱
- 生态较小

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

**优点**：
- 零代码/低代码
- 知识库 + 工作流内置
- 一键部署对外 API
- 生产就绪

**缺点**：
- 自定义逻辑受限
- 不适合复杂 Agent 逻辑
- 收费版功能限制

---

## 八、选择决策树

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
├── 多 Agent 角色协（研究员+写手+审核员）
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
<!-- Spring AI：Spring 官方的 AI 集成（含 Agent 支持） -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter</artifactId>
</dependency>
```

```java
// Spring AI 支持的 Agent 模式
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

## 十、面试核心要点

1. **LangChain vs LangGraph？** LangChain 是通用框架，LangGraph 是状态图编排引擎，适合复杂工作流
2. **CrewAI 核心概念？** Agent(角色) + Task(任务) + Crew(团队)，角色化多 Agent 协作
3. **AutoGen 独特在哪？** Agent 之间自由对话协作，微软出品，适合科研场景
4. **Dify 适合什么？** 低代码快速搭建、非技术人员使用、通过 API 被 Java 调用
5. **Java 后端怎么选？** Spring AI（原生）或 Dify HTTP API（低代码）

---

## 十一、极简总结

```
LangChain Agent = 万能瑞士军刀，生态最好，但单 Agent 为主
LangGraph = 状态图引擎，复杂工作流的终极方案
CrewAI = 跑团队（研究员+写手+审核），上手最快
AutoGen = 多 Agent 自由对话，适合实验和科研
Dify = 拖拽建 Agent，Java 通过 HTTP API 调用
Java 首选 = Spring AI + Dify / MCP 暴露微服务为工具
```
