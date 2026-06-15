# 🤖 AI Agent 架构与 MCP 协议

> AI Agent 是大模型应用的高级形态——它不再被动回答问题，而是主动规划、调用工具、执行任务。如果说 Prompt Engineering 和 RAG 解决的是"让模型说对话"，Agent 解决的就是"让模型做对事"。2025-2026 年，MCP（Model Context Protocol）已快速成为 Agent 与外部工具交互的标准化协议，是 AI 应用开发者必须掌握的新兴技能。

## 前置阅读

- [[AI Agent核心知识点]]
- [[AI-Agent-快速吃透]]
- [[AI Agent 设计模式]]
- [[MCP协议深度解析]]

## 目录

1. [Agent 的本质：从对话到行动](#1-agent-的本质从对话到行动)
2. [Agent 核心架构](#2-agent-核心架构)
3. [ReAct 模式：思考与行动的循环](#3-react-模式思考与行动的循环)
4. [工具系统设计](#4-工具系统设计)
5. [MCP 协议：统一的工具接入标准](#5-mcp-协议统一的工具接入标准)
6. [多 Agent 协作模式](#6-多-agent-协作模式)

---

## 1. Agent 的本质：从对话到行动

### 1.1 LLM vs Agent 的本质区别

```
LLM：用户问 → 模型答 → 结束
Agent：用户说 → 模型理解意图 → 制定计划 → 调用工具 → 观察结果 → 
       调整计划 → 继续执行 → 完成任务 → 报告结果
```

| 维度 | 普通 LLM | AI Agent |
|------|---------|----------|
| 交互模式 | 一问一答 | 多轮自主循环 |
| 能力边界 | 只有文本生成 | 可以影响外部世界 |
| 任务颗粒度 | 单次原子任务 | 复杂多步骤任务 |
| 外部感知 | 无 | 通过工具感知环境 |
| 状态管理 | 无状态 | 有短期 + 长期记忆 |

### 1.2 Agent 的核心公式

```
Agent = LLM（大脑） + Planning（规划） + Memory（记忆） + Tools（工具） + Action（行动）
```

> **重点**：Agent 不是另一种模型，而是一种**架构模式**——用大模型作为核心推理引擎，外部系统补充其能力短板，形成从感知到执行的闭环。

---

## 2. Agent 核心架构

### 2.1 六大核心模块

```
                        ┌──────────────┐
                        │   用户输入    │
                        └──────┬───────┘
                               ↓
┌──────────────────────────────────────────────────┐
│                   AI Agent                        │
│                                                   │
│  ┌──────────┐  ┌──────────┐  ┌──────────────┐   │
│  │  感知    │→│  规划    │→│  工具调用    │   │
│  │Perception│  │Planning  │  │  Tool Use    │   │
│  └──────────┘  └──────────┘  └──────┬───────┘   │
│       ↑                              ↓           │
│  ┌──────────┐                  ┌──────────┐     │
│  │  记忆    │←────────────────│  行动    │     │
│  │ Memory   │                  │  Action  │     │
│  └──────────┘                  └──────────┘     │
│                                                   │
└──────────────────────────────────────────────────┘
                               ↓
                     ┌──────────────────┐
                     │   任务完成 / 报告  │
                     └──────────────────┘
```

| 模块 | 职责 | 技术实现 |
|------|------|---------|
| **感知** | 接收并理解多模态输入 | 文本解析、ASR、OCR、API 数据 |
| **规划** | 分解任务、决定调用顺序 | ReAct、Tree-of-Thought、任务分解 |
| **工具** | 定义可调用的外部能力 | Function Calling、MCP、HTTP API |
| **行动** | 执行工具调用并获取结果 | 函数调用 → 结果解析 → 下一轮推理 |
| **记忆** | 维护上下文和历史信息 | 短期：上下文窗口；长期：向量库/RAG |
| **学习** | 从反馈中持续改进 | 规则迭代、Prompt 优化、微调 |

### 2.2 一个典型的 Agent 执行循环

```python
class AgentLoop:
    """Agent 核心执行循环"""

    def __init__(self, llm, tools, max_iterations=10):
        self.llm = llm
        self.tools = tools  # {"search": func, "calculator": func, ...}
        self.max_iterations = max_iterations

    def run(self, user_request: str) -> str:
        memory = [{"role": "system", "content": self._system_prompt()}]
        memory.append({"role": "user", "content": user_request})

        for i in range(self.max_iterations):
            # 1. LLM 推理：决定下一步行动
            response = self.llm.chat(memory)

            # 2. 检查是否完成
            if "FINAL_ANSWER:" in response:
                return response.split("FINAL_ANSWER:")[1].strip()

            # 3. 解析工具调用
            tool_call = self._parse_tool_call(response)
            if not tool_call:
                memory.append({"role": "assistant", "content": response})
                continue

            # 4. 执行工具
            tool_result = self._execute_tool(tool_call)
            memory.append({
                "role": "system",
                "content": f"工具执行结果 [{tool_call['name']}]: {tool_result}"
            })

        return "任务超时，已达到最大迭代次数"
```

---

## 3. ReAct 模式：思考与行动的循环

### 3.1 ReAct = Reasoning + Acting

**ReAct** 是 Agent 领域最经典的模式，它将推理（Thought）和行动（Action）交替进行：

```
循环流程：
Thought → Action → Observation → Thought → Action → ... → Final Answer

具体示例（用户问："北京今天天气怎么样？"）：

Thought: 用户想知道北京的天气，我需要查询实时天气数据
Action: search_weather("北京")
Observation: 北京今天晴天，温度 18-28℃，空气质量良

Thought: 已经拿到天气数据，可以直接回答用户了
Final Answer: 北京今天天气晴朗，气温 18-28℃，空气质量良，适合户外活动。
```

### 3.2 Function Calling：现代 ReAct 的标准实现

大多数现代 LLM 都原生支持 **Function Calling**（函数调用），它比文本解析式的 ReAct 更可靠：

```python
# 定义工具（Function Schema）
tools = [
    {
        "type": "function",
        "function": {
            "name": "search_weather",
            "description": "查询指定城市的实时天气",
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
    }
]

# LLM 返回的不是文本，而是结构化的函数调用指令
# {
#   "tool_calls": [{
#     "function": {"name": "search_weather", "arguments": '{"city": "北京"}'}
#   }]
# }
```

---

## 4. 工具系统设计

### 4.1 工具的本质

工具就是 **Agent 可以调用的外部函数**。从 LLM 的视角看，每个工具就是一个"带描述的函数签名"。

### 4.2 工具设计原则

| 原则 | 说明 | 好例子 | 坏例子 |
|------|------|--------|--------|
| **单一职责** | 每个工具只做一件事 | `search_docs(query)` | `do_everything(request)` |
| **明确描述** | 描述清楚"做什么、什么时候用" | "当需要查询天气信息时使用" | "用来做事情" |
| **类型安全** | 参数类型和约束要明确 | `limit: int, 1-100` | `limit: any` |
| **幂等性** | 读操作尽量幂等 | GET 请求 | 每次查询都创建新资源 |
| **错误友好** | 返回清晰的错误信息 | `{"error": "城市不存在"}` | 抛出 500 异常 |

### 4.3 Java 后端工具集成模式

```java
// Spring Boot 中将微服务接口暴露为 Agent 工具

@RestController
@RequestMapping("/agent/tools")
public class AgentToolController {

    @PostMapping("/search_orders")
    public ToolResult searchOrders(@RequestBody SearchOrdersRequest request) {
        // Agent 调用此接口查询订单
        List<Order> orders = orderService.search(
            request.userId(),
            request.keyword(),
            request.limit()
        );

        return ToolResult.success(orders, "找到 " + orders.size() + " 条订单");
    }

    @PostMapping("/create_ticket")
    public ToolResult createTicket(@RequestBody CreateTicketRequest request) {
        // Agent 调用此接口创建工单
        Ticket ticket = ticketService.create(request.title(), request.priority());
        return ToolResult.success(ticket, "工单创建成功: #" + ticket.id());
    }
}
```

---

## 5. MCP 协议：统一的工具接入标准

### 5.1 为什么需要 MCP

以往每接入一个新的数据源或工具，都需要单独编码对接。**MCP（Model Context Protocol）** 定义了模型与外部工具/数据源之间的标准通信协议，实现"一次适配，处处可用"。

```
Before MCP：
  模型 → 自定义适配器 A → 工具 A
  模型 → 自定义适配器 B → 工具 B
  模型 → 自定义适配器 C → 工具 C
  （每个工具都要写一个适配器，N×M 复杂度）

After MCP：
  模型 → MCP Client → MCP Server A → 工具 A
                    → MCP Server B → 工具 B
                    → MCP Server C → 工具 C
  （统一协议，N+M 复杂度）
```

### 5.2 MCP 核心概念

| 概念 | 说明 |
|------|------|
| **MCP Server** | 提供工具/资源的服务端，封装具体实现 |
| **MCP Client** | 运行在模型侧的客户端，发现和调用 MCP Server |
| **Resources** | 暴露给模型的数据源（文件、数据库、API 等） |
| **Tools** | 模型可以调用的函数/操作 |
| **Prompts** | 预定义的 Prompt 模板 |

### 5.3 MCP 通信流程

```
模型说："帮我查一下最新的订单状态"
         │
         ▼
MCP Client 发现可用的 MCP Server
         │
         ├──→ MCP Server A (订单系统): tools/list → [search_orders, get_order_detail]
         │
         ▼
MCP Client 调用: search_orders(user_id="123", status="pending")
         │
         ▼
MCP Server A 返回: [订单 #456, 订单 #789]
         │
         ▼
模型说："你有 2 个待处理订单：#456（已发货）和 #789（待付款）"
```

---

## 6. 多 Agent 协作模式

### 6.1 从单 Agent 到多 Agent

复杂任务往往需要多种专业能力。与其让一个 Agent 包揽一切，不如让多个专业 Agent 协作：

| 模式 | 结构 | 适用场景 |
|------|------|---------|
| **顺序流水线** | A → B → C | 数据清洗 → 分析 → 报告 |
| **主从调度** | 主 Agent 分配任务给从 Agent | 复杂任务分解执行 |
| **对等协作** | 多个 Agent 平等讨论 | 方案评审、代码审查 |
| **辩论模式** | Agent 之间互相质疑 | 关键决策、安全检查 |

### 6.2 多 Agent 实战示例

```python
# 主 Agent：负责任务分解和调度
MASTER_PROMPT = """
你是一个项目协调者。将用户的任务分解为子任务，
并分配给以下专家 Agent：
- CodeAgent：负责代码编写
- ReviewAgent：负责代码审查
- TestAgent：负责测试用例生成
"""

# 用户说："帮我实现一个 LRU 缓存，并确保代码质量"
# Master Agent 将其分解为：
# 1. CodeAgent：实现 LRU 缓存类
# 2. ReviewAgent：审查代码质量和性能
# 3. TestAgent：生成单元测试
# 4. CodeAgent：根据审查意见优化
# 5. ReviewAgent：二次确认通过
```

---

## 核心要点回顾

- **Agent = LLM + 规划 + 记忆 + 工具 + 行动**，本质是让模型从"说"到"做"
- **ReAct** 是 Agent 的核心推理模式：Thought → Action → Observation 循环
- **Function Calling** 是现代 LLM 原生支持的标准化工具调用方式
- 工具设计四原则：单一职责、明确描述、类型安全、错误友好
- **MCP 协议**正在成为 Agent 与外部工具交互的行业标准，值得重点关注
- 多 Agent 协作是处理复杂任务的必然趋势

## 参考资料

1. [[AI Agent核心知识点]]
2. [[AI-Agent-快速吃透]]
3. [[AI-Agent-ReAct与FunctionCalling]]
4. [[AI Agent 设计模式]]
5. [[AI Agent 规划与推理模式详解]]
6. [[AI Agent 工具系统设计]]
7. [[AI Agent 多Agent协作模式]]
8. [[MCP协议深度解析]]
9. [[MCP（Model Context Protocol，模型上下文协议）]]
10. [[LangGraph实战：状态图与多Agent工作流]]
