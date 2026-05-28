# LangChain Agent：ReAct 模式与 Function Calling

> **所属阶段**：阶段三 — Agent 智能体开发
> **前置知识**：Python、LLM API 调用、LangChain 基础
> **核心目标**：让 AI 自主调用工具、规划任务、迭代解决问题

---

## 1. Agent 核心概念

### 什么是 Agent

Agent（智能体）是一个能用 LLM 作为"大脑"，自主选择和使用工具来完成任务的系统。

```
传统 LLM：输入 → 输出（一次调用）
Agent：任务 → 思考 → 行动 → 观察 → 思考 → 行动 → ... → 完成
```

### Agent vs 普通 LLM

| 特性 | 普通 LLM | Agent |
|------|----------|-------|
| 知识来源 | 训练数据 | 训练数据 + 实时工具 |
| 行动能力 | 只能输出文本 | 可调用 API、执行代码、搜索 |
| 推理深度 | 单次推理 | 多步推理循环 |
| 适用场景 | 聊天、翻译 | 自动化、复杂问题解决 |

---

## 2. ReAct 模式（Reasoning + Acting）

### 2.1 核心思想

ReAct 将**推理**（Reasoning）和**行动**（Acting）交替进行：

```
Thought: 我需要查询今天天气
Action: search("北京今天天气")
Observation: 北京今天晴，18-28℃
Thought: 我知道天气了，可以给出穿衣建议
Answer: 建议穿短袖...
```

### 2.2 代码实现

```python
from langchain.agents import AgentExecutor, create_react_agent
from langchain.tools import tool
from langchain_openai import ChatOpenAI
from langchain_core.prompts import PromptTemplate

# 定义工具
@tool
def search_knowledge(query: str) -> str:
    """搜索 Java 技术知识库"""
    # 实际可接入搜索引擎或向量数据库
    kb = {
        "并发": "使用 synchronized、ReentrantLock、Atomic 类...",
        "集合": "ArrayList 非线程安全，Vector 线程安全..."
    }
    return kb.get(query, "未找到相关内容")

@tool
def execute_sql(sql: str) -> str:
    """执行 SQL 查询（模拟）"""
    # 生产环境需安全校验
    return f"[模拟] 执行: {sql}\n结果: 3 rows returned"

@tool
def read_file(path: str) -> str:
    """读取文件内容"""
    try:
        with open(path, "r") as f:
            return f.read()[:500]
    except Exception as e:
        return f"Error: {e}"

# 创建 Agent
llm = ChatOpenAI(model="deepseek-chat", api_key="sk-xxx",
                  base_url="https://api.deepseek.com")
tools = [search_knowledge, execute_sql, read_file]

prompt = PromptTemplate.from_template("""你是一个智能助手。使用以下工具完成任务：

{tools}

工具名: {tool_names}

使用格式：
Question: 用户问题
Thought: 我应该做什么
Action: 工具名
Action Input: 工具输入
Observation: 工具返回
... (可重复 Thought/Action/Observation)
Thought: 我现在知道答案了
Final Answer: 最终答案

{chat_history}
Question: {input}
{agent_scratchpad}""")

agent = create_react_agent(llm, tools, prompt)
executor = AgentExecutor(
    agent=agent, tools=tools,
    verbose=True, max_iterations=5,
    handle_parsing_errors=True
)

# 执行
result = executor.invoke({
    "input": "帮我查一下并发编程的最佳实践，然后写一个示例"
})
print(result["output"])
```

---

## 3. Function Calling（函数调用）

### 3.1 原理

Function Calling 是 LLM 原生支持的标准化工具调用协议，比 ReAct Prompt 更稳定。

```python
from openai import OpenAI

client = OpenAI(api_key="sk-xxx", base_url="https://api.deepseek.com")

# 1. 定义工具 schema
tools = [{
    "type": "function",
    "function": {
        "name": "search_docs",
        "description": "搜索 Java 技术文档",
        "parameters": {
            "type": "object",
            "properties": {
                "query": {
                    "type": "string",
                    "description": "搜索关键词"
                },
                "topic": {
                    "type": "string",
                    "enum": ["spring", "mysql", "redis", "java"],
                    "description": "技术领域"
                }
            },
            "required": ["query"]
        }
    }
}]

# 2. 对话循环：LLM 决定何时调用工具
def run_agent(user_input):
    messages = [{"role": "user", "content": user_input}]

    for _ in range(5):  # 最多 5 轮
        response = client.chat.completions.create(
            model="deepseek-chat",
            messages=messages,
            tools=tools
        )

        msg = response.choices[0].message

        # 如果 LLM 要调用工具
        if msg.tool_calls:
            messages.append(msg)
            for tool_call in msg.tool_calls:
                func_name = tool_call.function.name
                args = json.loads(tool_call.function.arguments)

                # 执行工具
                result = execute_tool(func_name, args)

                # 将结果反馈给 LLM
                messages.append({
                    "role": "tool",
                    "tool_call_id": tool_call.id,
                    "content": result
                })
        else:
            # 最终回复
            return msg.content

    return "达到最大轮次，任务未完成"
```

### 3.2 LangChain Tool 定义

```python
from langchain_core.tools import tool
from pydantic import BaseModel, Field

# 方式一：装饰器
@tool
def search_stackoverflow(query: str) -> str:
    """搜索 StackOverflow 解决技术问题"""
    # 接入搜索 API
    return f"搜索结果: {query} 的解决方案..."

# 方式二：结构化输入
class CodeReviewInput(BaseModel):
    code: str = Field(description="要审查的 Java 代码")
    level: str = Field(default="strict", description="审查严格度: strict/relaxed")

@tool(args_schema=CodeReviewInput)
def review_code(code: str, level: str = "strict") -> str:
    """审查 Java 代码，返回改进建议"""
    return f"[{level}模式] 审查结果: 代码符合规范"

# 方式三：自定义工具类
from langchain_core.tools import BaseTool

class LogAnalyzer(BaseTool):
    name = "LogAnalyzer"
    description = "分析服务器日志，提取异常信息"

    def _run(self, log_content: str) -> str:
        import re
        errors = re.findall(r'ERROR.*', log_content)
        return f"发现 {len(errors)} 个错误:\n" + "\n".join(errors[:5])
```

---

## 4. Agent 类型选择

```python
from langchain.agents import create_openai_functions_agent
from langchain.agents import create_structured_chat_agent

# Function Calling Agent（推荐，最稳定）
agent = create_openai_functions_agent(llm, tools, prompt)

# ReAct Agent（兼容性好，适合不支持 function calling 的模型）
agent = create_react_agent(llm, tools, prompt)

# Structured Chat Agent（支持多参数工具）
agent = create_structured_chat_agent(llm, tools, prompt)
```

---

## 5. 实战：最简单的 Agent

```python
from langchain.agents import AgentExecutor, create_openai_functions_agent
from langchain_openai import ChatOpenAI
from langchain_core.prompts import ChatPromptTemplate, MessagesPlaceholder
from langchain_core.tools import tool

@tool
def calculator(expression: str) -> str:
    """计算数学表达式，如 '2+3*4'"""
    try:
        return str(eval(expression))
    except:
        return "计算错误"

@tool
def get_current_time() -> str:
    """获取当前时间"""
    from datetime import datetime
    return datetime.now().strftime("%Y-%m-%d %H:%M:%S")

llm = ChatOpenAI(model="deepseek-chat", api_key="sk-xxx",
                  base_url="https://api.deepseek.com")
tools = [calculator, get_current_time]

prompt = ChatPromptTemplate.from_messages([
    ("system", "你是有工具调用能力的助手。"),
    ("human", "{input}"),
    MessagesPlaceholder("agent_scratchpad")
])

agent = create_openai_functions_agent(llm, tools, prompt)
executor = AgentExecutor(agent=agent, tools=tools, verbose=True)

executor.invoke({"input": "现在几点了？然后算一下(100+200)*3"})
```

---

## 6. 安全与限制

| 关注点 | 实践 |
|--------|------|
| 工具权限 | 敏感操作（删库、执行命令）需人工确认 |
| 成本控制 | 设置 max_iterations 防止无限循环 |
| 输入校验 | 工具内部校验参数，防止注入 |
| 日志追踪 | 记录每一步 Thought/Action/Observation |
