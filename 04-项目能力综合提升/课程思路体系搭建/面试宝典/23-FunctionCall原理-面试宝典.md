# Function Call 原理 面试宝典
> 基于课程大纲全面覆盖面试高频考点 -- Function Calling 原理、Schema 定义与实战应用

## 目录
1. [一、基础概念速答](#一基础概念速答)
2. [二、深度原理剖析](#二深度原理剖析)
3. [三、实战场景题](#三实战场景题)
4. [四、手写代码题](#四手写代码题)
5. [五、系统设计题](#五系统设计题)
6. [六、常见坑点与最佳实践](#六常见坑点与最佳实践)
7. [七、面试回答模板](#七面试回答模板)
8. [八、快速查漏补缺Checklist](#八快速查漏补缺checklist)

---

## 一、基础概念速答

> 高频面试题，每个问题要求在30秒内清晰回答核心要点。

### 1. 什么是 Function Calling（函数调用）？
Function Calling 是 LLM API 原生支持的一种能力，允许 LLM 在需要时输出结构化的函数调用请求（JSON），由开发者的代码实际执行该函数，再将结果回传给 LLM，最终 LLM 生成自然语言回复。**LLM 本身不执行任何代码，它只决定"何时调用、调用哪个函数、传入什么参数"**。

> 💡 **English**: Function Calling is an API-native capability that lets the LLM output structured JSON requests for function execution. The developer's code executes the function and returns results to the LLM for final response generation.

### 2. Function Calling 的核心流程是什么？
三步流程：
1. **定义工具**：开发者以 JSON Schema 格式描述函数的名称、参数和功能描述
2. **LLM 决策**：LLM 根据用户输入判断是否需要调用工具，若需要则返回 `tool_calls` 字段（包含函数名 + 参数 JSON）
3. **执行与回传**：开发者代码执行函数，将结果以 `role: "tool"` 消息回传给 LLM，LLM 据此生成最终回答

### 3. Function Calling 与 ReAct 模式的核心区别是什么？

| 维度 | Function Calling | ReAct |
|------|-----------------|-------|
| 推理方式 | API 原生支持，结构化 JSON 参数 | Prompt 驱动，Thought → Action → Observation 文本循环 |
| 可靠性 | 高，结构化输出不易出错 | 中等，依赖 Prompt 质量和解析器 |
| 灵活性 | 中等，受 API 格式约束 | 高，LLM 自由文本推理，可任意组合 |
| 并行调用 | 原生支持多 tool_calls 并行 | 需自行实现 |
| 适用场景 | 标准工具调用、生产环境 | 复杂多步推理、探索性任务 |
| 代表实现 | OpenAI / DeepSeek API | LangChain Agent |

> ⚠️ **面试重点**：不要将两者对立。最佳实践是用 Function Calling 做工具调用的"基础设施"，用 ReAct 提示词模式做复杂推理的"引导框架"。

### 4. 什么是 Tool / Function Schema？
Tool Schema 是以 JSON Schema 格式描述工具元数据的定义体，包含三个核心部分：`name`（函数名，snake_case）、`description`（详细功能描述，LLM 据此判断何时调用）、`parameters`（参数定义，包含类型、枚举值、描述、是否必需）。

```json
{
  "type": "function",
  "function": {
    "name": "get_weather",
    "description": "获取指定城市的实时天气信息，包含温度、湿度、天气状况",
    "parameters": {
      "type": "object",
      "properties": {
        "city": {
          "type": "string",
          "description": "城市名称，如'北京'、'上海'"
        },
        "unit": {
          "type": "string",
          "enum": ["celsius", "fahrenheit"],
          "description": "温度单位"
        }
      },
      "required": ["city"]
    }
  }
}
```

### 5. tool_choice 参数有哪些可选值？
- **`"auto"`**（默认）：LLM 自主决定是否调用工具
- **`"none"`**：禁止 LLM 调用任何工具，仅返回文本回复
- **`"required"`**：强制 LLM 调用至少一个工具（每次对话必须调用）
- **`{"type": "function", "function": {"name": "xxx"}}`**：强制 LLM 调用指定名称的工具

> 💡 在测试阶段可用 `"required"` 确保每次都能触发工具调用验证，生产环境推荐 `"auto"`。

### 6. Function Calling 中 LLM 生成的内容有哪些字段？
当 LLM 决定调用函数时，响应中会包含 `tool_calls` 数组，每个元素包含：
- `id`：工具调用唯一标识符，回传结果时必须用此 ID 关联
- `type`：固定为 `"function"`
- `function.name`：被调用的函数名
- `function.arguments`：JSON 字符串格式的参数

### 7. 什么是并行 Function Calling（Parallel Function Calling）？
OpenAI 等 LLM API 支持在一次响应中返回多个 `tool_calls`，这些调用之间无数据依赖，可以并行执行。例如"比较北京、上海、广州的天气"会同时触发 3 次 `get_weather` 调用。

```python
# LLM 返回的并行调用示例
response.choices[0].message.tool_calls = [
    ToolCall(id="call_1", function=Function(name="get_weather", arguments='{"city":"北京"}')),
    ToolCall(id="call_2", function=Function(name="get_weather", arguments='{"city":"上海"}')),
    ToolCall(id="call_3", function=Function(name="get_weather", arguments='{"city":"广州"}'))
]
```

### 8. Function Calling 在 LangChain 中如何创建 Agent？
LangChain 提供 `create_tool_calling_agent`（基于 OpenAI Function Calling 协议）和 `create_openai_functions_agent`（旧版）两种方式，通过 `@tool` 装饰器或 Pydantic 模型定义工具。

```python
from langchain.agents import create_tool_calling_agent, AgentExecutor
from langchain_openai import ChatOpenAI
from langchain.tools import tool

@tool
def calculator(expression: str) -> str:
    """计算数学表达式，如 '2+3*4'"""
    return str(eval(expression))

llm = ChatOpenAI(model="gpt-4", api_key="sk-xxx")
tools = [calculator]
agent = create_tool_calling_agent(llm, tools, prompt)
executor = AgentExecutor(agent=agent, tools=tools, verbose=True)
```

### 9. Function Calling 与 Plugin 的区别是什么？
Function Calling 是 API 层的工具调用协议（底层机制），Plugin 是 ChatGPT 平台层的插件生态（上层产品形态）。Plugin 内部实现也依赖 Function Calling 协议。在开发自有 Agent 时，直接使用 Function Calling API 更灵活可控。

### 10. Function Calling 的 Token 消耗如何计算？
工具定义本身（Tool Schema）会消耗 Prompt Token。Schema 越大、描述越长，每次请求消耗的 Token 越多。工具调用的返回结果也会计入 Token。优化策略：精简 description、避免过长的参数枚举值、对工具返回结果做截断/摘要。

> 💡 **策略**：将不常用的工具拆分为单独请求组，避免每次携带全部 Schema。

### 11. 如果 LLM 一直不调用工具，可能是什么原因？
- Tool Schema 的 `description` 不够清晰，LLM 无法判断使用场景
- 工具功能重叠，LLM 选择了错误的工具
- 使用 `tool_choice="none"` 或者模型不支持 Function Calling
- 模型版本过低（旧版 GPT-3.5-turbo 不支持 Function Calling）

### 12. 流式（Streaming）模式下如何处理 Function Calling？
流式模式下，LLM 会逐 chunk 输出。当检测到 `tool_calls` 字段出现时，需要缓存参数直到完整接收；工具执行后，再以非流式模式将结果回传给 LLM，继续流式输出最终回答。

```python
# 流式处理伪代码
stream = client.chat.completions.create(model="gpt-4", messages=msgs, tools=tools, stream=True)
for chunk in stream:
    if chunk.choices[0].delta.tool_calls:
        # 1. 缓存增量参数
        # 2. 等所有 chunk 接收完毕
        # 3. 执行工具
        # 4. 将结果回传给 LLM
    elif chunk.choices[0].delta.content:
        # 直接推流文本内容
        yield chunk.choices[0].delta.content
```

### 13. 什么是 Multi-turn Function Calling（多轮函数调用）？
多轮调用指 LLM 根据上一轮工具返回的结果，再次调用另一个工具的情形。例如"先查北京天气，如果下雨就推荐室内活动" → `get_weather("北京")` → 结果"雨" → `recommend_activity("北京", "室内")`。串行调用时，每一轮需要将上轮的 `assistant_msg` 和 `tool_msg` 都追加到 `messages` 列表中。

### 14. Function Calling 与 MCP（Model Context Protocol）的关系是什么？
MCP 是 Anthropic 提出的标准化工具协议层，定义了一套统一的工具发现、调用和传输规范。Function Calling 是底层的 API 调用格式，MCP 是上层的协议标准。在 MCP 架构中，工具的传输和调用仍使用 Function Calling 的 JSON Schema 格式。可以理解为：MCP = 标准化协议 + 传输层，Function Calling = 序列化格式 + API 层。

### 15. 如何用 Function Calling 实现数据库查询？
定义 SQL 查询工具，将用户自然语言转为 SQL 语句执行：

```json
{
  "type": "function",
  "function": {
    "name": "query_database",
    "description": "对 MySQL 数据库执行 SELECT 查询，返回查询结果",
    "parameters": {
      "type": "object",
      "properties": {
        "sql": { "type": "string", "description": "SQL SELECT 查询语句" }
      },
      "required": ["sql"]
    }
  }
}
```

> ⚠️ **安全注意**：必须限制仅允许 SELECT 操作，并通过数据库账号权限做好读写分离，防止 SQL 注入。

### 16. Function Calling 的 error recovery（错误恢复）策略有哪些？
工具调用可能失败（API 超时、参数错误、网络异常），常见恢复策略：
1. 将错误信息作为 `tool` 角色消息回传给 LLM，让 LLM 自行决定重试或换工具
2. 设置 `max_iterations` 防止无限重试循环
3. 对工具执行加超时保护（timeout），超时后返回友好的错误信息
4. 使用退避策略（exponential backoff）处理临时故障

```python
try:
    result = execute_tool(func_name, args)
    messages.append({"role": "tool", "tool_call_id": tc.id, "content": result})
except Exception as e:
    # 将异常信息返回给 LLM 决策
    error_msg = f"工具 {func_name} 执行失败: {str(e)}"
    messages.append({"role": "tool", "tool_call_id": tc.id, "content": error_msg})
```

### 17. Function Calling 在多 Agent 系统中如何协同？
多 Agent 系统中，Function Calling 作为 Agent 调用子工具的标准化协议：
- **Supervisor Agent** 通过 Function Calling 分配任务给 Specialist Agent
- **Specialist Agent** 通过 Function Calling 调用具体工具执行
- 各 Agent 的 Tool Schema 通过注册中心统一管理

### 18. Function Calling 中如何传递复杂的嵌套参数？
使用 JSON Schema 的嵌套对象和数组定义：

```json
{
  "name": "batch_send_email",
  "parameters": {
    "type": "object",
    "properties": {
      "recipients": {
        "type": "array",
        "items": {
          "type": "object",
          "properties": {
            "email": {"type": "string"},
            "name": {"type": "string"}
          },
          "required": ["email"]
        }
      },
      "template_id": {"type": "string"}
    },
    "required": ["recipients", "template_id"]
  }
}
```

---

## 二、深度原理剖析

> 面试中高区分度的问题，要求对 Function Calling 机制有深入理解。

### 1. Function Calling 的内部实现机制是什么？LLM 是如何"学会"调用工具的？

Function Calling 的本质是**指令微调（Instruction Tuning）**与**结构化生成（Structured Generation）**的结合：
- 模型在训练阶段使用了大量包含工具调用的对话数据（人工标注 + 合成数据），让模型学会"在需要外部信息时输出函数调用"的思维链
- 推理阶段，模型通过**约束解码（Constrained Decoding）**或**分类头（Classification Head）**确保输出的 `tool_calls` 符合 JSON Schema 格式
- 实际实现中，模型将工具定义编码到 Attention 上下文中，通过特殊 Token 标记函数调用的开始和结束

> 💡 **核心理解**：Function Calling 不是模型在"执行代码"，而是模型在"生成符合格式的结构化文本"，开发者负责将这段结构化文本"翻译"为真实函数执行。

### 2. Function Calling 与 ReAct 在 Prompt 层面本质上有何不同？

ReAct 在 Prompt 中嵌入工具使用格式示例（few-shot），LLM 通过文本续写的方式生成 Action。Function Calling 则在 API 层面以结构化字段 `tools` 传入，模型内部使用特殊的注意力机制处理工具定义。

```
ReAct Prompt (文本级):
  可用工具: get_weather(城市名)
  格式: Action: get_weather\nAction Input: {"city":"北京"}

Function Calling (API 级):
  tools=[{"type":"function","function":{"name":"get_weather","parameters":{...}}}]
```

**关键差异**：Function Calling 在模型训练时就嵌入了工具调用的特殊 Token 和注意力模式，因此比 ReAct 更稳定可靠。

### 3. 单函数调用与多函数调用的消息管理有何不同？

单函数调用时，消息序列为：
```
user → assistant(含 tool_calls) → tool → assistant(最终回复)
```

多函数调用（并行）时：
```
user → assistant(含 N 个 tool_calls) → tool_1/tool_2/.../tool_N → assistant(最终回复)
```

多函数调用（串行/多轮）时：
```
user → assistant(含 tool_call_1) → tool_1_result → assistant(含 tool_call_2) → tool_2_result → assistant(最终回复)
```

> 💡 **要点**：串行多轮调用中，每一轮 `assistant` 的消息和 `tool` 的消息都必须追加到 messages 列表，否则 LLM 会丢失上下文。

### 4. Tool Schema 的设计如何影响 LLM 的调用准确率？

Schema 设计的质量直接决定 LLM 能否准确调用工具：

| Schema 设计维度 | 好的实践 | 差的实践 |
|----------------|---------|---------|
| name | 动词_名词：get_weather、search_docs | 模糊：do_stuff、func1 |
| description | 详细说明功能、场景、示例 | "获取天气" |
| 参数 description | 说明格式、枚举值含义、默认值 | "城市名" |
| 参数约束 | 精确的类型 + enum + pattern | 全是 string 类型 |
| required | 只标注真正必需的参数 | 全部必填或全部可选 |

**实测经验**：详细的 description 能让 LLM 调用准确率从 ~60% 提升到 ~95%。

### 5. Function Calling 中的 JSON Schema 支持哪些数据类型？

支持 JSON Schema 标准的全部类型：
- `string` — 字符串（支持 `enum`、`pattern` 约束）
- `number` / `integer` — 数字
- `boolean` — 布尔值
- `array` — 数组（支持 `items` 定义元素类型）
- `object` — 嵌套对象
- `anyOf` / `oneOf` — 联合类型
- `nullable: true` — 允许 null 值

### 6. Token 消耗优化：如何减少 Function Calling 的 Tool Schema 开销？

```python
# 优化前：每个 description 过长
{
  "description": "获取指定城市的实时天气信息，包含温度、湿度、风速、风向、气压、紫外线指数、空气质量等详细数据，用户可以根据这些信息决定出行计划"
}

# 优化后：精简但有足够的区分度
{
  "description": "获取城市实时天气（温度/湿度/天气状况）"
}
```

**优化技巧总结**：
1. 全局工具定义只放高频工具，低频工具动态注入
2. description 控制在 50 字以内，但必须包含关键触发词
3. 参数表的 enum 值太多时，只放高频值并加 `description` 说明
4. 对工具返回结果做摘要截断（超过 ~2000 字符就 truncate）
5. 将工具按领域分组，不同轮次只传入相关组的 Schema

### 7. tool_choice=required 的内部工作原理是什么？

当设置 `tool_choice="required"` 时，模型在推理时被强制在第一个生成的 Token 就进入"函数调用模式"，跳过自由文本生成的路径。这通过在解码时修改 logit 分布——将非工具调用的 Token 概率置零或极大压低，确保模型一定会输出 `tool_calls`。

> 💡 **应用场景**：在需要强制工具调用的测试场景、工作流编排、工具链处理等场景下非常有用。

### 8. 为什么说 Function Calling 改变了 LLM 的应用架构范式？

传统 LLM 应用是"输入-输出"的封闭系统，LLM 无法获取外部实时数据。Function Calling 打破了这一限制，使 LLM 从"知识问答引擎"进化为"可行动的系统中枢"：

```
传统架构：用户 → LLM → 文本回答（知识截止于训练数据）
Function Calling 架构：用户 → LLM → 【调用工具/API/数据库】 → 基于实时数据回答
```

这意味着 LLM 可以实时查询天气、检索知识库、操作数据库、发送邮件、调用第三方 API——Function Calling 是 LLM 从"聊天机器人"进化到"Agent"的关键枢纽。

### 9. Function Calling 在开源模型中的支持情况如何？

| 模型 | 支持程度 | 备注 |
|------|---------|------|
| GPT-4 / GPT-4o | 原生支持 | OpenAI，最成熟的 FC 支持 |
| GPT-3.5-turbo | 原生支持 | 较早支持 FC 的模型 |
| DeepSeek V2/V3 | 原生支持 | 兼容 OpenAI API 格式 |
| Qwen 2.5 (Qwen series) | 原生支持 | 阿里千问系列，原生 FC |
| Llama 3.1+ | 有限支持 | 通过系统 Prompt 模拟 FC |
| GLM-4 | 原生支持 | 智谱清言 |
| Mistral Large | 原生支持 | Mistral AI |

### 10. Function Calling 的安全风险有哪些？

1. **Prompt Injection**：用户输入可能诱导 LLM 调用危险工具（如删除数据库）
2. **工具权限提升**：LLM 可能在非预期场景下调用了高权限工具
3. **数据泄露**：工具执行结果可能包含敏感数据被 LLM 回显
4. **无限循环**：工具返回结果可能诱导 LLM 再次调用同一工具造成 Token 浪费
5. **参数注入**：LLM 生成的参数可能包含恶意内容（如 SQL 注入）

**防御措施**：按权限分组工具、设置速率限制（Rate Limiter）、熔断机制（Circuit Breaker）、工具执行超时、设置最大迭代次数、敏感操作人工审批。

---

## 三、实战场景题

> 面试官给出具体场景，考察候选人能否将 Function Calling 应用到实际问题中。

### 场景 1：构建一个天气查询助手

**问题**：用户说"今天北京热吗，适合穿什么？"请描述 Function Calling 的完整调用流程。

**回答要点**：
1. 定义 `get_weather` 工具（参数：city、date）
2. 用户消息 → LLM（携带天气工具 Schema）
3. LLM 输出 `tool_calls`：`get_weather(city="北京")`
4. 后端调用天气 API，返回 `{"temp": 32, "condition": "晴", "humidity": 60}`
5. 结果回传给 LLM → LLM 生成："北京今天 32°C，气温较高，建议穿短袖短裤，注意防晒。"

```python
import json
from openai import OpenAI

client = OpenAI(api_key="sk-xxx")

tools = [{
    "type": "function",
    "function": {
        "name": "get_weather",
        "description": "获取指定城市的实时天气信息",
        "parameters": {
            "type": "object",
            "properties": {
                "city": {
                    "type": "string",
                    "description": "城市名称，如'北京'"
                }
            },
            "required": ["city"]
        }
    }
}]

def chat_with_weather(user_input):
    messages = [{"role": "user", "content": user_input}]
    
    response = client.chat.completions.create(
        model="gpt-4",
        messages=messages,
        tools=tools
    )
    
    msg = response.choices[0].message
    if msg.tool_calls:
        messages.append(msg)
        for tc in msg.tool_calls:
            args = json.loads(tc.function.arguments)
            result = f"{args['city']}天气：32°C，晴，湿度60%"
            messages.append({"role": "tool", "tool_call_id": tc.id, "content": result})
        
        final = client.chat.completions.create(model="gpt-4", messages=messages)
        return final.choices[0].message.content
    return msg.content
```

### 场景 2：Function Calling + MySQL 数据库集成

**问题**：如何用 Function Calling 让 LLM 查询数据库？需要注意什么？

**回答要点**：
1. 定义 `query_database` 工具，只允许 SELECT 操作
2. 创建独立的数据库只读账号，设置 statement timeout
3. 对 SQL 做安全性校验（关键词黑名单检查）
4. 对查询结果做截断（限制返回行数）

```python
import pymysql

def query_mysql(sql: str) -> str:
    """执行 MySQL SELECT 查询（只读）"""
    # 安全检查：仅允许 SELECT
    if not sql.strip().upper().startswith("SELECT"):
        return "错误：仅支持 SELECT 查询"
    
    conn = pymysql.connect(
        host="localhost", user="readonly_user",
        password="xxx", database="orders_db",
        connect_timeout=5, read_timeout=10
    )
    try:
        with conn.cursor() as cursor:
            cursor.execute(sql)
            rows = cursor.fetchmany(50)  # 限制最多 50 行
            columns = [desc[0] for desc in cursor.description]
            return json.dumps([dict(zip(columns, row)) for row in rows], ensure_ascii=False)
    finally:
        conn.close()

# 注册到 Tool Schema
database_tool = {
    "type": "function",
    "function": {
        "name": "query_mysql",
        "description": "查询 MySQL 数据库（只读），仅支持 SELECT 语句",
        "parameters": {
            "type": "object",
            "properties": {
                "sql": {"type": "string", "description": "SELECT SQL 查询语句"}
            },
            "required": ["sql"]
        }
    }
}
```

### 场景 3：用 Function Calling 实现自动发邮件

**问题**：用户说"给张三（zhangsan@example.com）发一封主题为'面试邀请'的邮件"，请设计实现方案。

```python
import smtplib
from email.mime.text import MIMEText

def send_email(recipient: str, subject: str, body: str) -> str:
    """发送邮件"""
    try:
        msg = MIMEText(body, "plain", "utf-8")
        msg["Subject"] = subject
        msg["To"] = recipient
        msg["From"] = "noreply@company.com"
        
        with smtplib.SMTP("smtp.company.com", 587) as server:
            server.starttls()
            server.login("noreply@company.com", "password")
            server.send_message(msg)
        return f"邮件已成功发送至 {recipient}"
    except Exception as e:
        return f"发送失败: {str(e)}"

email_tool = {
    "type": "function",
    "function": {
        "name": "send_email",
        "description": "发送电子邮件，收件人、主题、正文皆可由 LLM 生成",
        "parameters": {
            "type": "object",
            "properties": {
                "recipient": {"type": "string", "description": "收件人邮箱地址"},
                "subject": {"type": "string", "description": "邮件主题"},
                "body": {"type": "string", "description": "邮件正文内容"}
            },
            "required": ["recipient", "subject", "body"]
        }
    }
}
```

> ⚠️ **安全注意**：自动发邮件功能需增加人工确认步骤或白名单机制，防止被滥用为垃圾邮件发送器。

### 场景 4：Tools 和 Messages 的数据准备流程

**问题**：在一次包含 Function Calling 的对话中，如何正确组织和更新 messages 数组？

```python
def function_calling_loop(user_input: str, tools: list, max_rounds: int = 5) -> str:
    """完整的 Function Calling 对话循环"""
    messages = [
        {"role": "system", "content": "你是一个智能助手，可以使用工具来完成任务。"},
        {"role": "user", "content": user_input}
    ]
    
    for round_idx in range(max_rounds):
        # Step 1: 调用 LLM（携带工具定义）
        response = client.chat.completions.create(
            model="gpt-4",
            messages=messages,
            tools=tools,
            tool_choice="auto"
        )
        
        assistant_msg = response.choices[0].message
        
        # Step 2: 检查是否有工具调用
        if not assistant_msg.tool_calls:
            return assistant_msg.content
        
        # Step 3: 将 Assistant 消息追加到历史（必须！）
        messages.append(assistant_msg)
        
        # Step 4: 逐一执行工具并追加结果
        for tool_call in assistant_msg.tool_calls:
            func_name = tool_call.function.name
            func_args = json.loads(tool_call.function.arguments)
            result = tool_executor.execute(func_name, func_args)
            
            # 将工具执行结果作为 tool 角色追加
            messages.append({
                "role": "tool",
                "tool_call_id": tool_call.id,  # 关联到对应的调用 ID
                "name": func_name,
                "content": result
            })
        
        # Step 5: 继续循环，LLM 会基于工具结果生成回复或再次调用
    
    return "达到最大轮次限制，任务可能未完全完成。"
```

### 场景 5：多函数并行调用 + 结果融合

**问题**：用户问"查一下北京和上海的股票行情和天气"，需要同时调用多个工具，如何实现结果融合？

**回答要点**：
1. LLM 返回两个独立的 `tool_calls`：`get_stock("北京")`、`get_stock("上海")`、`get_weather("北京")`、`get_weather("上海")`
2. 后端用 `ThreadPoolExecutor` 或异步 IO 并行执行这 4 个函数
3. 所有结果回传给 LLM，LLM 融合生成："北京...上海..."
4. 关键：用 `tool_call_id` 关联结果，保证时序正确

### 场景 6：信息追加与多消息轮次

**问题**：如果第一轮 LLM 调用了工具，第二轮需要再次调用，消息格式如何维护？

每一轮对话必须包含完整的消息链：
```
Round 1:
  system + user → assistant(tool_call_1) → tool(result_1) → assistant(text)
Round 2 (追加):
  system + user + assistant(tool_call_1) + tool(result_1) + assistant(text)
  → assistant(tool_call_2) → tool(result_2) → assistant(final)
```

不能只传最后一轮的消息，否则 LLM 丢失了工具调用的完整上下文。

### 场景 7：Function Calling + 知识库 RAG 的结合

**问题**：如何用 Function Calling 实现 RAG（检索增强生成）？

```python
def search_knowledge_base(query: str, top_k: int = 3) -> str:
    """从向量知识库检索相关内容"""
    # 1. 将 query 转为 embedding
    # 2. 在向量数据库中搜索相似文档
    # 3. 返回 Top-K 文档
    pass

rag_tool = {
    "type": "function",
    "function": {
        "name": "search_knowledge_base",
        "description": "从企业内部知识库搜索相关文档，适用于产品文档、技术文档、FAQ 等",
        "parameters": {
            "type": "object",
            "properties": {
                "query": {"type": "string", "description": "搜索问题或关键词"},
                "top_k": {"type": "integer", "description": "返回文档数量（默认3）"}
            },
            "required": ["query"]
        }
    }
}
```

### 场景 8：Function Calling 在桌面自动化中的应用

**问题**：如何用 LLM + Function Calling 自动化日常办公操作？

定义一组办公工具集：
1. `create_calendar_event(title, time, attendees)` — 创建日程
2. `send_slack_message(channel, message)` — 发送 Slack 消息
3. `create_jira_ticket(summary, description, priority)` — 创建 Jira 工单
4. `read_file(path)` — 读取本地文件

用户说"把这份文档的要点总结发到团队群" → LLM 自动调用 `read_file` → 总结内容 → `send_slack_message`。

### 场景 9：消息信息追加的具体实现

当 LLM 第一轮调用工具获取信息后，第二轮需要追加更多上下文：

```python
# 追加系统指令，引导 LLM 继续使用工具
messages.append({
    "role": "system",
    "content": "基于上述数据，如果还需要补充信息，请继续使用相关工具。如果信息已足够，请直接回答用户。"
})

# 或者追加用户的新问题
messages.append({
    "role": "user",
    "content": "再帮我看一下明天的天气情况"
})

# 继续调用 ...
```

---

## 四、手写代码题

> 面试中要求现场手写或白板 coding 的题目。

### 1. 手写一个完整的 Function Calling 对话循环

```python
import json
from openai import OpenAI
from typing import Callable, Dict

class FunctionCallingAgent:
    """手写完整的 Function Calling Agent"""
    
    def __init__(self, api_key: str, model: str = "gpt-4"):
        self.client = OpenAI(api_key=api_key)
        self.model = model
        self.tools: list = []
        self.handlers: Dict[str, Callable] = {}
    
    def register_tool(self, name: str, handler: Callable, schema: dict):
        """注册工具"""
        self.tools.append({
            "type": "function",
            "function": schema
        })
        self.handlers[name] = handler
    
    def run(self, user_input: str, max_rounds: int = 5) -> str:
        """运行 Agent"""
        messages = [
            {"role": "system", "content": "你是一个智能助手，可以调用工具完成任务。"},
            {"role": "user", "content": user_input}
        ]
        
        for _ in range(max_rounds):
            response = self.client.chat.completions.create(
                model=self.model,
                messages=messages,
                tools=self.tools if self.tools else None,
                tool_choice="auto" if self.tools else None
            )
            
            msg = response.choices[0].message
            
            if not msg.tool_calls:
                return msg.content
            
            messages.append(msg)
            
            for tc in msg.tool_calls:
                if tc.function.name in self.handlers:
                    try:
                        args = json.loads(tc.function.arguments)
                        result = self.handlers[tc.function.name](**args)
                    except Exception as e:
                        result = f"执行错误: {str(e)}"
                else:
                    result = f"未知工具: {tc.function.name}"
                
                messages.append({
                    "role": "tool",
                    "tool_call_id": tc.id,
                    "content": str(result)
                })
        
        return "达最大轮次，任务未完成"


# 使用示例
agent = FunctionCallingAgent(api_key="sk-xxx")
agent.register_tool(
    name="get_weather",
    handler=lambda city: f"{city}天气：25°C，晴",
    schema={
        "name": "get_weather",
        "description": "查询天气",
        "parameters": {
            "type": "object",
            "properties": {
                "city": {"type": "string", "description": "城市名"}
            },
            "required": ["city"]
        }
    }
)
print(agent.run("北京今天天气怎么样？"))
```

### 2. 手写 ToolRegistry（工具注册表）

```python
class ToolRegistry:
    """工具注册表——统一管理工具的定义、注册和执行"""
    
    def __init__(self):
        self._tools: Dict[str, Callable] = {}
        self._schemas: list[dict] = []
    
    def register(self, name: str, func: Callable, 
                 description: str, parameters: dict,
                 required: list[str] = None):
        """注册工具"""
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
    
    def execute(self, name: str, args_json: str) -> str:
        """执行工具"""
        if name not in self._tools:
            return f"错误：工具 '{name}' 未注册"
        try:
            args = json.loads(args_json)
            result = self._tools[name](**args)
            return str(result)
        except Exception as e:
            return f"执行异常: {e}"
    
    def get_schemas(self) -> list[dict]:
        return self._schemas

# 使用示例
registry = ToolRegistry()
registry.register(
    name="calculator",
    func=lambda expression: str(eval(expression)),
    description="数学计算器，支持 +-*/ 运算",
    parameters={
        "expression": {
            "type": "string",
            "description": "数学表达式，如 '2+3*4'"
        }
    },
    required=["expression"]
)
```

### 3. 手写 JSON Schema 生成器：从 Python 函数自动生成 Tool Schema

```python
import inspect
import json
from typing import get_type_hints

def auto_schema(func: callable) -> dict:
    """从 Python 函数自动生成 OpenAI Tool Schema"""
    sig = inspect.signature(func)
    hints = get_type_hints(func)
    doc = inspect.getdoc(func) or ""
    
    properties = {}
    required = []
    
    for name, param in sig.parameters.items():
        # 从类型注解推断参数类型
        param_type = hints.get(name, str)
        type_map = {
            str: "string",
            int: "integer",
            float: "number",
            bool: "boolean",
            list: "array",
            dict: "object"
        }
        
        json_type = type_map.get(param_type, "string")
        properties[name] = {"type": json_type}
        
        # 从 docstring 的第一行提取描述
        if doc:
            first_line = doc.split("\n")[0]
            properties[name]["description"] = f"参数 {name}"
        
        # 无默认值的参数标记为 required
        if param.default is inspect.Parameter.empty:
            required.append(name)
    
    return {
        "type": "function",
        "function": {
            "name": func.__name__,
            "description": doc,
            "parameters": {
                "type": "object",
                "properties": properties,
                "required": required
            }
        }
    }

# 使用
def get_weather(city: str, unit: str = "celsius"):
    """获取城市天气信息"""
    pass

schema = auto_schema(get_weather)
print(json.dumps(schema, indent=2, ensure_ascii=False))
```

### 4. 手写并行 Function Calling 执行器

```python
from concurrent.futures import ThreadPoolExecutor, as_completed

def execute_parallel_tool_calls(tool_calls: list, tool_registry: ToolRegistry) -> list:
    """并行执行多个工具调用"""
    results = []
    with ThreadPoolExecutor(max_workers=5) as executor:
        future_map = {
            executor.submit(tool_registry.execute, tc.function.name, tc.function.arguments): tc
            for tc in tool_calls
        }
        for future in as_completed(future_map):
            tc = future_map[future]
            try:
                result = future.result(timeout=30)
            except Exception as e:
                result = f"执行超时或失败: {str(e)}"
            results.append({
                "tool_call_id": tc.id,
                "content": result
            })
    return results
```

### 5. 手写带错误重试的 Function Calling

```python
import time

def robust_function_calling(messages: list, tools: list, 
                            max_retries: int = 3) -> str:
    """带重试机制的 Function Calling 实现"""
    
    for attempt in range(max_retries):
        try:
            response = client.chat.completions.create(
                model="gpt-4",
                messages=messages,
                tools=tools,
                timeout=30  # 防止卡死
            )
            
            msg = response.choices[0].message
            
            if not msg.tool_calls:
                return msg.content
            
            # 执行工具
            messages.append(msg)
            all_success = True
            
            for tc in msg.tool_calls:
                try:
                    args = json.loads(tc.function.arguments)
                    result = execute_tool(tc.function.name, args)
                    messages.append({
                        "role": "tool",
                        "tool_call_id": tc.id,
                        "content": result
                    })
                except Exception as e:
                    all_success = False
                    messages.append({
                        "role": "tool",
                        "tool_call_id": tc.id,
                        "content": f"工具执行失败（第{attempt+1}次尝试）: {str(e)}"
                    })
            
            if all_success:
                # 所有工具执行成功，获取 LLM 最终回答
                final = client.chat.completions.create(
                    model="gpt-4", messages=messages
                )
                return final.choices[0].message.content
            
        except Exception as e:
            if attempt == max_retries - 1:
                return f"系统错误，已重试{max_retries}次: {str(e)}"
            time.sleep(2 ** attempt)  # 退避等待
    
    return "处理失败"
```

### 6. 手写 JSON Schema 参数校验器

```python
from jsonschema import validate, ValidationError

def validate_tool_call(tool_call: dict, tool_schema: dict) -> tuple[bool, str]:
    """校验 LLM 生成的 tool_call 参数是否符合 Schema"""
    try:
        parameters_schema = tool_schema["function"]["parameters"]
        args = json.loads(tool_call["arguments"])
        validate(instance=args, schema=parameters_schema)
        return True, "参数校验通过"
    except json.JSONDecodeError as e:
        return False, f"参数 JSON 格式错误: {e}"
    except ValidationError as e:
        return False, f"参数校验失败: {e.message}"

# 使用
tool_call = {"name": "get_weather", "arguments": '{"city": "北京"}'}
is_valid, msg = validate_tool_call(tool_call, tool_schema)
```

### 7. 手写一个支持 Function Calling 的 Spring AI Tool（Java）

```java
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class WeatherTools {

    @Tool(description = "获取指定城市的实时天气信息，包括温度、湿度和天气状况")
    public String getWeather(
        @ToolParam(description = "城市名称，如'北京'、'上海'") String city,
        @ToolParam(description = "温度单位：celsius(摄氏度) 或 fahrenheit(华氏度)", 
                   defaultValue = "celsius") String unit
    ) {
        double temp = unit.equals("fahrenheit") ? 89.6 : 32.0;
        return String.format("{\"city\":\"%s\",\"temp\":%.1f,\"condition\":\"晴\"}", city, temp);
    }
}
```

### 8. 手写 Tool Call 流式处理

```python
def stream_with_tools(messages: list, tools: list):
    """流式处理 Function Calling，支持工具调用"""
    stream = client.chat.completions.create(
        model="gpt-4",
        messages=messages,
        tools=tools,
        stream=True
    )
    
    collected_content = ""
    tool_calls_buffer = {}
    
    for chunk in stream:
        delta = chunk.choices[0].delta if chunk.choices else None
        if not delta:
            continue
        
        # 处理文本内容
        if delta.content:
            collected_content += delta.content
            yield ("content", delta.content)
        
        # 处理工具调用（流式模式返回增量数据）
        if delta.tool_calls:
            for tc_delta in delta.tool_calls:
                idx = tc_delta.index
                if idx not in tool_calls_buffer:
                    tool_calls_buffer[idx] = {
                        "id": tc_delta.id or "",
                        "function": {"name": "", "arguments": ""}
                    }
                if tc_delta.id:
                    tool_calls_buffer[idx]["id"] = tc_delta.id
                if tc_delta.function:
                    if tc_delta.function.name:
                        tool_calls_buffer[idx]["function"]["name"] = tc_delta.function.name
                    if tc_delta.function.arguments:
                        tool_calls_buffer[idx]["function"]["arguments"] += tc_delta.function.arguments
    
    # 如果检测到工具调用
    if tool_calls_buffer:
        tool_calls = [v for k, v in sorted(tool_calls_buffer.items())]
        yield ("tool_calls", tool_calls)
```

---

## 五、系统设计题

> 考察架构设计能力，如何在实际系统中落地 Function Calling。

### 1. 设计一个企业级 Function Calling 平台

**需求**：支持多个业务部门注册工具、权限分级管理、调用监控、高可用。

**架构设计**：

```
                    ┌─────────────────────┐
                    │    API Gateway       │
                    │  (鉴权 / 限流 / 路由) │
                    └────────┬────────────┘
                             │
              ┌──────────────┼──────────────┐
              ▼              ▼              ▼
      ┌──────────────┐ ┌──────────────┐ ┌──────────────┐
      │  Tool Registry│ │ LLM Service  │ │  Monitor     │
      │  (工具注册/   │ │ (FC 编排引擎) │ │ (日志/追踪/   │
      │   版本管理)   │ │              │ │  告警)       │
      └──────────────┘ └──────────────┘ └──────────────┘
              │              │
              ▼              ▼
      ┌──────────────┐ ┌──────────────┐
      │  Tool Executor│ │  Rate Limiter│
      │  (沙箱执行)   │ │  (熔断/限流) │
      └──────────────┘ └──────────────┘
              │
              ▼
      ┌──────────────┐ ┌──────────────┐ ┌──────────────┐
      │ 业务系统 API  │ │  数据库      │ │  第三方服务  │
      └──────────────┘ └──────────────┘ └──────────────┘
```

**核心组件**：
1. **Tool Registry**：工具 Schema 管理、版本管理、灰度发布
2. **LLM Service**：FC 编排引擎，管理多轮对话状态
3. **Tool Executor**：沙箱化的工具执行环境，支持超时/熔断
4. **Rate Limiter**：按工具、按用户做调用频率控制
5. **Monitor**：全链路追踪，每次 FC 调用都记录日志

### 2. 设计一个高可用的自动化邮件发送系统

**需求**：LLM 通过 Function Calling 自动生成并发送邮件，需保证可靠性和安全性。

**设计方案**：
1. **邮件生成层**：LLM + FC 生成邮件内容（收件人、主题、正文）
2. **审核层**：敏感邮件进入人工审核队列（awaiting_review）
3. **发送层**：邮件队列 + 重试机制（最多重试 3 次，退避间隔）
4. **安全层**：收件人白名单、发送频率限制、内容敏感词过滤

```python
# 邮件发送的状态机
states = {
    "draft": "草稿，等待 LLM 生成",
    "awaiting_review": "等待人工审核（高风险邮件）",
    "queued": "已进入发送队列",
    "sending": "正在发送",
    "sent": "已发送",
    "failed": "发送失败（可重试）",
    "blocked": "被安全策略拦截"
}
```

### 3. 设计一个多租户的 Function Calling 网关

**需求**：多个业务线共享同一套 LLM 服务，各自的工具互相隔离。

**核心设计**：
- **租户隔离**：每个租户有独立的 Tool Registry 和数据空间
- **工具分组**：`READ_ONLY` / `WRITE` / `DANGEROUS` / `SYSTEM` 四级权限
- **动态注入**：根据租户角色和上下文动态选择注入哪些工具 Schema
- **计费计量**：每个租户的 FC 调用次数、Token 消耗单独统计

### 4. 设计一个 Function Calling 的性能优化方案

**瓶颈分析**：
1. Tool Schema 过大 → Token 消耗高、LLM 决策慢
2. 工具执行延迟 → 端到端响应延迟受最慢工具影响
3. LLM 反复调用 → Token 和延迟成本线性增长

**优化策略**：

| 问题 | 方案 | 预估效果 |
|------|------|---------|
| Schema 太大 | 工具分组动态注入、精简 description | Token 减少 40-60% |
| 工具执行慢 | 并行执行、异步非阻塞、工具缓存 | 延迟降低 50-80% |
| LLM 反复调用 | 设置 max_iterations、合并相似工具 | 轮次减半 |
| 返回结果太大 | 结果截断、摘要、分页 | Token 减少 70% |
| 高并发场景 | 请求合并、本地缓存工具结果 | 吞吐量提升 3-5x |

### 5. 设计一个 Agent-First 的智能客服系统

**需求**：用户通过自然语言提问，系统能自动调用多个工具解决问题。

**架构**：
```
用户提问 → 意图识别 → 工具编排 → 并行执行 → 结果融合 → 回答生成
```

**工具链示例**：
1. `search_faq(query)` — 搜索常见问题库
2. `query_order(order_id)` — 查询订单状态
3. `create_ticket(issue, priority)` — 创建工单（解决不了时转人工）
4. `send_sms(phone, message)` — 发送短信通知

**多轮对话的状态管理**：每个会话维护一个 `messages` 列表和 `context` 字典（缓存已查到的订单信息等），避免重复查询。

---

## 六、常见坑点与最佳实践

### 常见坑点

| 坑点 | 现象 | 原因 | 解决方案 |
|------|------|------|---------|
| LLM 不调用工具 | 始终回复文本 | Tool description 描述不清晰，LLM 不知道何时用 | 优化 description，加入触发场景示例 |
| 参数格式错误 | LLM 传参类型不对 | JSON Schema 约束不够严格 | 使用 enum、pattern、type 精确约束 |
| Tool 调用死循环 | 同一工具被无限重复调用 | 工具返回结果诱导 LLM 再次调用 | 设置 `max_iterations`，检测重复调用 |
| 结果时序错乱 | 并行调用结果匹配不上 | tool_call_id 没有正确关联 | 每次回传必须带 `tool_call_id` |
| Token 爆炸 | 请求 Token 超限 | 工具 Schema + 历史消息链太长 | 精简 Schema，截断历史，结果摘要 |
| SQL 注入 | 用户通过 LLM 注入 SQL | 工具直接拼接用户输入执行 | 工具内部参数校验，只读账户隔离 |
| 上下文丢失 | 多轮对话中工具调用结果被遗忘 | 消息列表未正确追加 | 每一轮都追加完整消息链 |
| 流式中断 | 流式模式下工具调用处理异常 | 流式 chunk 中 tool_calls 是增量 | 缓存增量参数，等 finish_reason=stop |

### 最佳实践清单

**Schema 设计**：
1. 工具名使用 `snake_case`，动词在前（`get_weather`、`send_email`）
2. description 写清功能 + 触发场景 + 参数格式说明
3. 每个工具职责单一，避免"万能工具"
4. 精确约束参数类型和枚举值

**执行层**：
5. 工具执行加超时保护（建议 10-30s）
6. 非敏感操作自动执行，敏感操作加入工审批
7. 工具执行结果截断（超过 ~2000 字符做摘要）
8. 设置明确的 `max_iterations`（建议 5-10 轮）

**安全层**：
9. 数据库工具仅允许 SELECT + 只读账号
10. 文件操作工具限制访问路径
11. 邮件/短信工具加频率限制和白名单
12. 详尽的调用日志（记录每次 FC 的输入和输出）

**监控层**：
13. 监控每次 FC 调用的成功率、延迟、Token 消耗
14. 设置告警：连续 3 次工具失败 / Token 消耗异常激增
15. A/B 测试不同的 Schema 设计方案的效果

---

## 七、面试回答模板

> Top 5 高频问题的结构化回答模板，面试时可直接套用。

### 模板 1："请介绍一下 Function Calling 的原理"

> 按照"是什么→怎么用→为什么重要"的结构回答。

**回答**：
"Function Calling 是 LLM API 原生支持的一种工具调用协议。它的核心机制是：开发者在 API 请求中以 JSON Schema 格式定义工具，LLM 根据用户输入自主判断是否需要调用这些工具——如果需要，就返回结构化的 `tool_calls` 字段，包含函数名和参数 JSON；开发者拿到这个请求后自己执行函数，把结果返回给 LLM，LLM 据此生成最终回复。

关键点在于：**LLM 并不执行任何代码，它只决定'什么时候调用、调用哪个函数、传入什么参数'**。实际执行是开发者的代码完成的。

从架构角度看，Function Calling 的本质是 **指令微调 + 结构化生成的结合**。模型通过训练数据学会了在需要外部信息时输出函数调用，API 层通过约束解码确保输出符合 JSON Schema 格式。这使得 LLM 从纯文本问答系统进化为可以实时获取数据、操作业务系统的 Agent 中枢。"

### 模板 2："Function Calling 和 ReAct 有什么区别？"

> 从"机制层面 + 适用场景"两个维度对比。

**回答**：
"Function Calling 和 ReAct 是实现 Agent 工具调用的两种范式，核心区别在于实现层次。

Function Calling 是 **API 层面的原生支持**，LLM 直接返回结构化 JSON，不需要解析文本，可靠性更高。ReAct 是 **Prompt 层面的文本驱动**，通过 Thought → Action → Observation 的格式引导 LLM 逐步推理，更灵活但依赖 Prompt 质量。

选型上，我倾向于组合使用：Function Calling 做工具调用的基础设施，负责标准化的函数执行；ReAct 提示词模式做复杂推理的引导框架。比如在 LangChain 中，`create_tool_calling_agent` 就是两者结合的例子——用 Function Calling API 调用工具，用 ReAct 风格的 Prompt 引导推理步骤。"

### 模板 3："如果 LLM 不调用工具或调用了错误的工具，怎么排查？"

> 按照"检视→测试→优化→兜底"的思路回答。

**回答**：
"排查路径有四步。

第一步，检查 Tool Schema 的 **description**。LLM 靠描述判断何时使用工具，如果描述不清晰或工具间功能重叠，LLM 容易选错或干脆不用。我会检查每个工具的 description 是否包含了触发场景的关键词。

第二步，调整 **tool_choice**。测试阶段用 `tool_choice="required"` 强制每次调用工具来验证 Schema 是否正确，确认无误后再改为 `"auto"`。

第三步，**精简和拆分工具**。如果工具功能太复杂，拆分为多个细粒度的工具；如果工具太多让 LLM 困惑，按场景分组动态注入。

第四步，设置 **兜底策略**。如果 LLM 连续两次调用同一工具返回同样结果，说明陷入循环，强制终止并报错。如果工具全部失败，回退到纯文本回答。"

### 模板 4："如何设计一个安全的 Function Calling 系统？"

> 从"输入层→执行层→输出层"分层阐述。

**回答**：
"我会从三个层面设计安全机制。

**输入层**：对所有工具参数做 JSON Schema 校验，确保 LLM 生成的参数符合预期类型和格式；对数据库工具只允许 SELECT 操作，使用只读账号连接。

**执行层**：工具执行加超时保护（15-30 秒超时熔断）；敏感操作（发邮件、删除数据）进入人工审批队列；设置调用频率限制防止被滥用。

**输出层**：对工具结果做脱敏处理，防止敏感数据被 LLM 回显；设置 Token 上限，防止结果过大导致成本失控；全链路日志记录每次 FC 调用的入参、结果、耗时，便于审计和问题追踪。"

### 模板 5："你们的项目中 Function Calling 是怎样落地的？"

> 建议用 STAR 法则（Situation-Task-Action-Result），根据实际项目调整。

**回答（以邮件自动发送为例）**：
"在我们的智能客服项目中，我设计了一套基于 Function Calling 的自动邮件回复系统。

**Situation**：客服团队每天需要回复数千封用户邮件，重复性高、人力成本大。

**Task**：设计一个 LLM Agent，自动理解用户邮件内容并生成回复、调用邮件系统发送。

**Action**：
1. 定义了三个工具：`search_faq`（搜索知识库）、`generate_reply`（生成邮件回复内容）、`send_email`（发送邮件）
2. 设计了消息管理逻辑：每次 FC 调用后，`assistant` 和 `tool` 的消息都正确追加到 messages 列表
3. 实现了多轮串行调用：先搜索 FAQ → 生成回复 → 发送邮件
4. 加了安全控制：仅允许发送到白名单邮箱，每日发送上限 200 封

**Result**：邮件回复效率提升约 70%，人工审核覆盖 10% 的高风险邮件，零安全事故。"

---

## 八、快速查漏补缺 Checklist

> 面试前逐项自检，确认掌握程度。

### 基础概念

- [ ] 能用自己的话解释什么是 Function Calling（30 秒版）
- [ ] 能画出 FC 的完整流程图（用户 → LLM → 工具执行 → LLM → 用户）
- [ ] 理解 tool_choice 四种模式的区别（auto / none / required / 指定工具）
- [ ] 理解并行 FC 和串行 FC 的区别和适用场景
- [ ] 理解 FC 与 ReAct 的核心区别和各自优缺点
- [ ] 理解 streaming 模式下 FC 的处理方式

### Schema 设计

- [ ] 能手写完整的 JSON Schema（name / description / parameters / required）
- [ ] 知道如何定义嵌套参数（array of objects）
- [ ] 知道如何通过 enum、pattern 约束参数
- [ ] 理解 description 长度对 LLM 调用准确率的影响
- [ ] 能写出从 Python 函数自动生成 Schema 的代码

### 代码能力

- [ ] 能手写 FC 完整循环（含 messages 管理）
- [ ] 能实现 ToolRegistry 模式
- [ ] 能实现并发工具执行器
- [ ] 能实现带重试和错误处理的 FC 逻辑
- [ ] 能实现流式 FC 的分段处理
- [ ] 能在 Spring AI 中用 @Tool 注解定义工具

### 实战场景

- [ ] 能设计天气查询 FC 场景
- [ ] 能设计 LLM + MySQL 数据库查询场景
- [ ] 能设计 LLM 自动发邮件场景
- [ ] 能设计 FC + RAG 知识库检索场景
- [ ] 能设计多轮对话中的消息追加和信息融合

### 安全与优化

- [ ] 理解 FC 的安全风险（Prompt Injection / SQL 注入 / 权限提升）
- [ ] 知道如何做 Token 消耗优化（Schema 精简、结果截断）
- [ ] 知道如何做工具执行的熔断和限流
- [ ] 知道如何防止 FC 死循环
- [ ] 理解企业级 FC 平台的分层架构

### 对比知识

- [ ] FC vs ReAct：范式对比
- [ ] FC vs Plugin：层次对比
- [ ] FC vs MCP：协议对比
- [ ] FC vs Agent：整体与局部关系
- [ ] OpenAI FC vs LangChain Agent vs Spring AI @Tool：三套方案的对比

> 🎯 **面试核心策略**：用"定义 → 流程 → 痛点 → 解决方案"四步法回答所有 FC 相关问题。先清晰定义问题，再讲标准流程，接着指出实际落地中的坑，最后给出你的解决方案——这是面试官最想听到的"有深度、有实践"的回答。

---

*本面试宝典基于"大模型 Function Call 的原理及应用"课程大纲整理，覆盖 Function Calling 基础概念、单函数/多函数调用、MySQL 数据库集成、自动发邮件等核心知识点。*
