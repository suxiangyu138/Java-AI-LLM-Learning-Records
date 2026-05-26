# ReAct 模式与 Function Calling 实战

> **核心认知**：让 LLM 不只是"说话"，而是能"做事"——调用工具、执行代码、搜索信息。ReAct 和 Function Calling 是实现 Agent 自主行动的两大基础范式。
> **前置阅读**：`AI Agent核心知识点.md`

---

## 1. 两种范式的对比

| 维度 | ReAct | Function Calling |
|------|-------|-----------------|
| 推理方式 | Prompt 驱动（"Thought → Action → Observation"） | API 原生支持（Tool Use） |
| 判断逻辑 | LLM 自由文本推理 | 结构化 JSON 参数 |
| 灵活性 | 高（可任意组合工具） | 中等（受 API 格式约束） |
| 可靠性 | 中等（依赖 Prompt 质量） | 高（结构化输出） |
| 适用场景 | 需要自由思考的多步任务 | 标准的工具调用场景 |
| 代表框架 | LangChain Agent | OpenAI / DeepSeek API |

---

## 2. ReAct 模式详解

### 2.1 核心循环

```
Question: 今天北京天气怎么样？适合户外运动吗？

Thought: 我需要查询北京今天的天气。
Action: search_weather
Action Input: {"city": "北京", "date": "today"}
Observation: 北京今天晴，气温18-25℃，PM2.5=35

Thought: 天气晴朗，温度适宜，空气质量良好。
Action: Final Answer
Action Input: 北京今天天气很好！晴天，18-25℃，空气质量优，非常适合户外运动。
```

### 2.2 从零实现 ReAct Agent

```python
import re
import json
from typing import Callable, Any


class ReActAgent:
    """从零实现 ReAct Agent（不依赖 LangChain）"""

    REACT_PROMPT = """你是一个能使用工具的智能助手。请用以下格式逐步推理：

Question: 用户的问题
Thought: 我需要做什么？
Action: 工具名称（必须是 [{tool_names}] 之一）
Action Input: 工具的输入参数（JSON 格式）
Observation: 工具返回的结果
... (重复 Thought/Action/Action Input/Observation 直到得到最终答案)
Thought: 我有了足够的信息来回答
Final Answer: 最终答案

可用工具：
{tool_descriptions}

开始！
Question: {query}
{history}"""

    def __init__(self, llm_call: Callable[[str], str]):
        self.llm = llm_call
        self.tools: dict[str, Callable] = {}

    def register_tool(self, name: str, func: Callable, description: str):
        """注册工具"""
        self.tools[name] = {"func": func, "description": description}

    def run(self, query: str, max_steps: int = 10) -> str:
        history = ""
        for step in range(max_steps):
            # 1. 构建 Prompt
            prompt = self.REACT_PROMPT.format(
                tool_names=", ".join(self.tools.keys()),
                tool_descriptions=self._tool_descriptions(),
                query=query,
                history=history
            )

            # 2. LLM 推理
            response = self.llm(prompt)

            # 3. 解析输出
            action, action_input = self._parse_action(response)

            if action == "Final Answer":
                return action_input

            if action not in self.tools:
                history += f"\n{response}\nObservation: 未知工具 '{action}'，可用工具：{list(self.tools.keys())}"
                continue

            # 4. 执行工具
            try:
                params = json.loads(action_input) if action_input else {}
                observation = self.tools[action]["func"](**params)
            except Exception as e:
                observation = f"工具执行失败: {e}"

            history += f"\n{response}\nObservation: {observation}"

        return "达到最大推理步数，未能完成任务。"

    def _parse_action(self, response: str) -> tuple[str, str]:
        """解析 LLM 输出中的 Action 和 Action Input"""
        action_match = re.search(r"Action:\s*(.+)", response)
        input_match = re.search(r"Action Input:\s*(.+)", response)
        final_match = re.search(r"Final Answer:\s*(.+)", response, re.DOTALL)

        if final_match:
            return "Final Answer", final_match.group(1).strip()

        action = action_match.group(1).strip() if action_match else ""
        action_input = input_match.group(1).strip() if input_match else ""
        return action, action_input

    def _tool_descriptions(self) -> str:
        return "\n".join(
            f"- {name}: {info['description']}"
            for name, info in self.tools.items()
        )


# 使用示例
def search_weather(city: str, date: str = "today") -> str:
    """模拟天气查询"""
    # 实际项目中调用天气 API
    return f"{city}{date}天气：晴，18-25℃，适合户外运动"

def calculate(expression: str) -> str:
    """数学计算"""
    return str(eval(expression))  # 生产环境需安全沙箱

agent = ReActAgent(llm_call=llm_chat)
agent.register_tool("search_weather", search_weather, "查询城市天气，参数：city, date")
agent.register_tool("calculate", calculate, "数学计算，参数：expression")

result = agent.run("今天北京天气如何？适合户外运动吗？")
```

---

## 3. Function Calling（OpenAI 兼容协议）

### 3.1 工具定义与调用

```python
from openai import OpenAI

client = OpenAI(
    api_key="sk-xxx",
    base_url="https://api.deepseek.com/v1"
)

# 定义工具
tools = [
    {
        "type": "function",
        "function": {
            "name": "search_java_doc",
            "description": "搜索 Java 官方文档或 Spring 文档中的类/方法用法",
            "parameters": {
                "type": "object",
                "properties": {
                    "query": {
                        "type": "string",
                        "description": "搜索关键词，如 'Spring Boot @Transactional 用法'"
                    },
                    "version": {
                        "type": "string",
                        "description": "文档版本，如 'Spring Boot 3.2'",
                        "default": "latest"
                    }
                },
                "required": ["query"]
            }
        }
    },
    {
        "type": "function",
        "function": {
            "name": "execute_sql_analysis",
            "description": "分析 SQL 语句的性能，检查索引使用、全表扫描等问题",
            "parameters": {
                "type": "object",
                "properties": {
                    "sql": {
                        "type": "string",
                        "description": "需要分析的 SQL 语句"
                    },
                    "dialect": {
                        "type": "string",
                        "enum": ["mysql", "postgresql", "oracle"],
                        "description": "数据库方言"
                    }
                },
                "required": ["sql", "dialect"]
            }
        }
    }
]


def run_function_calling(prompt: str) -> str:
    """完整的 Function Calling 流程"""
    messages = [
        {"role": "system", "content": "你是一个 Java 后端技术助手，可以搜索文档和分析 SQL。"},
        {"role": "user", "content": prompt}
    ]

    # 第 1 步：LLM 决定是否调用工具
    response = client.chat.completions.create(
        model="deepseek-chat",
        messages=messages,
        tools=tools,
        tool_choice="auto"  # auto / none / required
    )

    assistant_msg = response.choices[0].message

    # 第 2 步：如果有工具调用，执行工具
    if assistant_msg.tool_calls:
        messages.append(assistant_msg)

        for tool_call in assistant_msg.tool_calls:
            func_name = tool_call.function.name
            func_args = json.loads(tool_call.function.arguments)

            # 执行工具
            if func_name == "search_java_doc":
                result = search_java_doc(**func_args)
            elif func_name == "execute_sql_analysis":
                result = analyze_sql(**func_args)
            else:
                result = "未知工具"

            # 将工具结果添加到消息
            messages.append({
                "role": "tool",
                "tool_call_id": tool_call.id,
                "content": result
            })

        # 第 3 步：LLM 根据工具结果生成最终回答
        final_response = client.chat.completions.create(
            model="deepseek-chat",
            messages=messages
        )
        return final_response.choices[0].message.content

    # 没有工具调用，直接返回
    return assistant_msg.content
```

### 3.2 工具执行器注册表

```python
class ToolRegistry:
    """工具注册表——管理所有可用工具"""

    def __init__(self):
        self._tools: dict[str, Callable] = {}
        self._schemas: list[dict] = []

    def register(
        self,
        name: str,
        func: Callable,
        description: str,
        parameters: dict,
        required: list[str] = None
    ):
        self._tools[name] = func
        self._schemas.append({
            "type": "function",
            "function": {
                "name": name,
                "description": description,
                "parameters": {
                    "type": "object",
                    "properties": parameters,
                    "required": required or list(parameters.keys())
                }
            }
        })

    def execute(self, tool_call) -> str:
        name = tool_call.function.name
        args = json.loads(tool_call.function.arguments)
        if name not in self._tools:
            return f"错误：工具 '{name}' 未注册"
        try:
            result = self._tools[name](**args)
            return str(result)
        except Exception as e:
            return f"工具执行异常: {e}"

    def get_schemas(self) -> list[dict]:
        return self._schemas


# 使用
registry = ToolRegistry()
registry.register(
    name="search_code",
    func=lambda query, lang: f"搜索结果：找到3个匹配的{lang}代码",
    description="在代码库中搜索代码片段",
    parameters={
        "query": {"type": "string", "description": "搜索关键词"},
        "lang": {"type": "string", "enum": ["java", "python", "sql"]}
    },
    required=["query"]
)
```

---

## 4. LangChain 版 Agent（快速上手）

```python
from langchain.agents import AgentExecutor, create_tool_calling_agent
from langchain_openai import ChatOpenAI
from langchain.tools import tool
from langchain.prompts import ChatPromptTemplate


# 1. 定义工具（@tool 装饰器）
@tool
def search_spring_docs(query: str) -> str:
    """搜索 Spring 官方文档，参数 query 为搜索关键词"""
    # 实际搜索逻辑
    return f"Spring 文档中关于 '{query}' 的内容：..."

@tool
def query_database(sql: str) -> str:
    """执行 SQL 查询（只读），参数 sql 为 SELECT 语句"""
    # 实际查询逻辑
    return "[{'id': 1, 'name': 'user1'}]"

tools = [search_spring_docs, query_database]

# 2. 创建 Agent
llm = ChatOpenAI(
    model="deepseek-chat",
    api_key="sk-xxx",
    base_url="https://api.deepseek.com/v1"
)

prompt = ChatPromptTemplate.from_messages([
    ("system", "你是Java后端技术助手，可以使用工具来帮助用户解决问题。"),
    ("human", "{input}"),
    ("placeholder", "{agent_scratchpad}")  # Agent 推理历史占位符
])

agent = create_tool_calling_agent(llm, tools, prompt)
executor = AgentExecutor(
    agent=agent,
    tools=tools,
    verbose=True,        # 打印推理过程
    max_iterations=5,    # 最多5步推理
    handle_parsing_errors=True
)

# 3. 运行
result = executor.invoke({
    "input": "Spring Boot 3.2 中 @Transactional 的默认传播行为是什么？"
})
print(result["output"])
```

---

## 5. 实战项目：智能运维 Agent 骨架

```python
# devops_agent.py
import subprocess
import re


class DevOpsAgent:
    """智能运维助手——自动分析日志、定位问题、生成方案"""

    def __init__(self, llm_client):
        self.llm = llm_client
        self.tools = self._build_tools()

    def _build_tools(self) -> dict:
        return {
            "read_log": lambda path, lines=100:
                self._read_log_file(path, int(lines)),
            "search_error": lambda pattern, log_content:
                self._search_error_pattern(pattern, log_content),
            "check_system": lambda:
                self._system_health_check(),
            "search_solution": lambda error:
                self._search_solution_db(error),
        }

    def analyze(self, user_request: str) -> str:
        """根据用户请求自动完成运维分析"""
        # 1. Agent 解析用户意图
        # 2. 按需调用工具（读日志 → 提取错误 → 查方案）
        # 3. 生成分析报告
        pass

    def _read_log_file(self, path: str, lines: int = 100) -> str:
        """读取日志文件尾部"""
        with open(path, "r", encoding="utf-8") as f:
            all_lines = f.readlines()
            return "".join(all_lines[-lines:])

    def _search_error_pattern(self, pattern: str, log_content: str) -> str:
        """正则提取错误信息"""
        matches = re.findall(pattern, log_content, re.IGNORECASE)
        return "\n".join(matches[:20]) if matches else "未找到匹配项"

    def _system_health_check(self) -> str:
        """系统健康检查"""
        result = subprocess.run(["top", "-bn1"], capture_output=True, text=True)
        return result.stdout[:500]

    def _search_solution_db(self, error: str) -> str:
        """从内部知识库搜索解决方案"""
        # 实际项目中对接 RAG 知识库
        return f"关于 '{error}' 的已知解决方案：..."
```

---

## 6. Function Calling vs ReAct 选型

| 场景 | 推荐方案 | 原因 |
|------|----------|------|
| 标准 API 查询 | Function Calling | 结构化、可靠 |
| 多步推理探索 | ReAct | 灵活、可自纠正 |
| 并行调用多个工具 | Function Calling（多 tool_calls） | 原生支持 |
| 需要中间结果推理 | ReAct | Thought 步骤可视化 |
| 生产环境 | Function Calling | 更可控、易监控 |

**推荐策略**：用 Function Calling 做工具调用的"基础设施"，用 ReAct 提示词模式引导 LLM 做复杂的多步推理。

---

## 快速调试检查清单

- [ ] 工具描述是否足够清晰？（LLM 靠描述判断用哪个工具）
- [ ] `tool_choice` 是否设对？（`auto` 表示让 LLM 自己决定）
- [ ] Function Calling 返回 `tool_calls` 后是否把结果回传给了 LLM？
- [ ] 工具执行是否有超时保护？（执行太久的工具会卡住整个流程）
- [ ] 是否限制了 `max_iterations` / `max_steps`？（防止无限循环）
