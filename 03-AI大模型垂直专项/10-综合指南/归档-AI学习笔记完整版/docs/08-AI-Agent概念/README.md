# 第8步：AI Agent（智能体）概念

> **阶段目标：** 理解AI Agent的核心概念和架构模式，掌握感知-规划-执行循环，能够构建简单的自主Agent  
> **预计学时：** 2-3周（每天3-4小时）  
> **前置要求：** Prompt Engineering + API调用基础  

---

## 📚 目录

- [8.1 AI Agent是什么](#81-ai-agent是什么)
- [8.2 Agent核心架构](#82-agent核心架构)
- [8.3 感知-规划-执行循环](#83-感知-规划-执行循环)
- [8.4 工具调用基础](#84-工具调用基础)
- [8.5 记忆系统](#85-记忆系统)
- [8.6 构建你的第一个Agent](#86-构建你的第一个agent)
- [8.7 阶段练习](#87-阶段练习)
- [8.8 常见问题](#88-常见问题)

---

## 8.1 AI Agent是什么

### 8.1.1 Agent vs 普通LLM调用

```
普通LLM调用：
  用户提问 → LLM → 回答（一次完成，无外部交互）

AI Agent：
  用户提问 → LLM（思考）→ 行动（调用工具）→ 观察结果
              ↑                                     ↓
              └──────── 循环直到任务完成 ←───────────┘

关键区别：
┌──────────────┬─────────────────┬─────────────────────┐
│   维度        │   普通LLM       │   AI Agent          │
├──────────────┼─────────────────┼─────────────────────┤
│ 交互次数      │ 1次             │ 多次循环            │
│ 外部工具      │ 不能用          │ 可以调用任何工具     │
│ 自主性        │ 被动响应        │ 主动规划和执行      │
│ 长期记忆      │ 无              │ 可以有              │
│ 适用场景      │ 简单问答        │ 复杂多步骤任务      │
└──────────────┴─────────────────┴─────────────────────┘
```

### 8.1.2 Agent思维模型

```
Agent = LLM + 工具 + 记忆 + 规划能力

类比：Agent就像一个聪明的实习生

实习生（Agent）：
├── 大脑（LLM）：理解任务，做决策
├── 工具（Tools）：搜索、计算、写代码、查数据库
├── 笔记本（Memory）：记录之前做过什么、学到了什么
├── 任务清单（Planning）：分解任务，一步步执行
└── 反馈循环（Loop）：做一步→看结果→调整→继续
```

---

## 8.2 Agent核心架构

### 8.2.1 架构总览

```
                    ┌─────────────────────────────┐
                    │        AI Agent              │
                    │                              │
    用户任务 ──────▶│  ┌───────────────────────┐  │
                    │  │     Planner (规划器)    │  │
                    │  │  分解任务→生成计划      │  │
                    │  └───────────┬───────────┘  │
                    │              ↓               │
                    │  ┌───────────────────────┐  │
                    │  │   Executor (执行器)    │  │
                    │  │  选择工具→执行动作     │──┼──▶ 工具1
                    │  └───────────┬───────────┘  │     工具2
                    │              ↓               │     工具3
                    │  ┌───────────────────────┐  │
                    │  │   Evaluator (评估器)   │  │
                    │  │  判断是否完成/需继续   │  │
                    │  └───────────┬───────────┘  │
                    │              ↓               │
                    │      ┌──────┴──────┐        │
                    │      ↓             ↓        │
                    │   完成 ✓       继续循环      │
                    │      ↓             │        │
                    ├──────┼─────────────┘        │
                    │  Memory (记忆系统)          │
                    │  ├── 短期记忆 (对话上下文)   │
                    │  └── 长期记忆 (持久化存储)   │
                    └─────────────────────────────┘
```

### 8.2.2 核心组件定义

```python
from abc import ABC, abstractmethod
from typing import List, Dict, Any, Optional
from dataclasses import dataclass, field
from enum import Enum
import json

class AgentState(Enum):
    IDLE = "idle"
    THINKING = "thinking"
    ACTING = "acting"
    OBSERVING = "observing"
    FINISHED = "finished"
    ERROR = "error"

@dataclass
class Tool:
    """工具定义"""
    name: str
    description: str
    parameters: Dict[str, Any]  # JSON Schema格式
    function: callable
    
    def to_openai_format(self) -> dict:
        """转为OpenAI Function Calling格式"""
        return {
            "type": "function",
            "function": {
                "name": self.name,
                "description": self.description,
                "parameters": {
                    "type": "object",
                    "properties": self.parameters,
                    "required": list(self.parameters.keys()),
                }
            }
        }

@dataclass
class AgentStep:
    """Agent执行的一个步骤"""
    step_id: int
    thought: str          # 思考过程
    action: Optional[str] # 执行的工具名
    action_input: Optional[Dict]  # 工具参数
    observation: Optional[str]    # 工具返回结果
    status: AgentState = AgentState.IDLE

@dataclass
class Memory:
    """Agent记忆"""
    short_term: List[Dict[str, str]] = field(default_factory=list)  # 对话历史
    long_term: Dict[str, Any] = field(default_factory=dict)         # 键值存储
    steps: List[AgentStep] = field(default_factory=list)            # 执行步骤
```

---

## 8.3 感知-规划-执行循环

### 8.3.1 核心循环

```python
class AgentLoop:
    """
    Agent主循环：感知 → 规划 → 执行 → 观察 → (重复)
    
    这是所有AI Agent的核心运行模式
    """
    
    def __init__(self, llm_client, tools: List[Tool], 
                 max_steps: int = 10):
        self.llm = llm_client
        self.tools = {t.name: t for t in tools}
        self.max_steps = max_steps
        self.memory = Memory()
    
    def run(self, task: str) -> str:
        """执行一个任务"""
        print(f"\n🎯 任务: {task}\n")
        
        # Step 0: 初始化
        system_prompt = self._build_system_prompt()
        self.memory.short_term.append(
            {"role": "system", "content": system_prompt}
        )
        self.memory.short_term.append(
            {"role": "user", "content": task}
        )
        
        # 主循环
        for step_num in range(self.max_steps):
            print(f"\n--- Step {step_num + 1} ---")
            
            # 1. 感知 + 思考 (调用LLM)
            response = self.llm.chat.completions.create(
                model="gpt-4o",
                messages=self.memory.short_term,
                tools=[t.to_openai_format() for t in self.tools.values()],
                tool_choice="auto",
            )
            
            message = response.choices[0].message
            
            # 2a. 如果需要调用工具 → 执行
            if message.tool_calls:
                for tool_call in message.tool_calls:
                    tool_name = tool_call.function.name
                    tool_args = json.loads(tool_call.function.arguments)
                    
                    print(f"  🔧 调用工具: {tool_name}({tool_args})")
                    
                    # 执行工具
                    result = self._execute_tool(tool_name, tool_args)
                    
                    print(f"  📊 结果: {str(result)[:100]}...")
                    
                    # 记录步骤
                    self.memory.steps.append(AgentStep(
                        step_id=step_num,
                        thought=f"调用{tool_name}",
                        action=tool_name,
                        action_input=tool_args,
                        observation=str(result),
                        status=AgentState.ACTING,
                    ))
                    
                    # 将工具结果加入对话
                    self.memory.short_term.append(message.model_dump())
                    self.memory.short_term.append({
                        "role": "tool",
                        "tool_call_id": tool_call.id,
                        "content": str(result),
                    })
            
            # 2b. 如果没有工具调用 → 任务完成
            else:
                final_answer = message.content
                print(f"  ✅ 最终回答: {final_answer}")
                
                self.memory.steps.append(AgentStep(
                    step_id=step_num,
                    thought="任务完成",
                    status=AgentState.FINISHED,
                ))
                
                return final_answer
        
        return "任务达到最大步数限制，未完成。"
    
    def _build_system_prompt(self) -> str:
        """构建系统提示"""
        tool_descriptions = "\n".join([
            f"- {t.name}: {t.description}" 
            for t in self.tools.values()
        ])
        
        return f"""
你是一个AI Agent，能够使用工具来完成用户的任务。

# 可用工具
{tool_descriptions}

# 工作流程
1. 分析用户任务，确定需要哪些步骤
2. 选择合适的工具执行每个步骤
3. 根据工具返回的结果决定下一步
4. 所有步骤完成后，给出最终答案

# 重要规则
- 每次只调用一个工具
- 工具返回的信息是真实的，优先使用
- 如果工具调用失败，尝试其他方法
- 当信息充足时，直接给答案，不要多调用工具
"""
    
    def _execute_tool(self, tool_name: str, args: dict) -> Any:
        """执行工具并返回结果"""
        if tool_name not in self.tools:
            return f"错误: 未知工具 {tool_name}"
        
        try:
            tool = self.tools[tool_name]
            return tool.function(**args)
        except Exception as e:
            return f"工具执行错误: {e}"
```

### 8.3.2 一个完整的示例

```python
# ========== 定义工具 ==========
def search_database(query: str) -> str:
    """搜索内部知识库"""
    # 模拟数据库
    db = {
        "产品价格": "旗舰版2999元，标准版1999元，入门版999元",
        "退货政策": "购买后7天内无条件退货，30天内可换货",
        "客服时间": "周一至周五 9:00-18:00，周末10:00-16:00",
    }
    for key, value in db.items():
        if query in key:
            return value
    return f"未找到关于'{query}'的信息"

def send_email(to: str, subject: str, body: str) -> str:
    """发送邮件（模拟）"""
    return f"邮件已发送至 {to}，主题：{subject}"

tools = [
    Tool("search_database", "搜索内部知识库获取产品信息", 
         {"query": {"type": "string", "description": "搜索关键词"}},
         search_database),
    Tool("send_email", "发送邮件给指定收件人",
         {"to": {"type": "string"}, "subject": {"type": "string"}, 
          "body": {"type": "string"}},
         send_email),
]

# ========== 运行Agent ==========
agent = AgentLoop(client, tools, max_steps=5)
result = agent.run("用户张三想退货，他3天前买的旗舰版。请帮我查下退货政策，然后发邮件告诉他退货流程。")

# Agent会：
# Step 1: 思考 → 需要查退货政策
# Step 2: 调用 search_database("退货政策")
# Step 3: 观察结果 → 7天无条件退货，可以退
# Step 4: 调用 send_email("zhangsan@email.com", "退货流程", ...)
# Step 5: 观察结果 → 邮件发送成功
# Step 6: 给用户最终回答
```

---

## 8.4 工具调用基础

### 8.4.1 Function Calling深入

```python
# ========== OpenAI Function Calling ==========
# Agent能够使用工具的核心机制

# 定义工具schema
tools_schema = [
    {
        "type": "function",
        "function": {
            "name": "get_weather",
            "description": "获取指定城市的当前天气",
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
]

# 调用
response = client.chat.completions.create(
    model="gpt-4o",
    messages=[{"role": "user", "content": "北京今天天气怎么样？"}],
    tools=tools_schema,
    tool_choice="auto",  # 让模型决定是否调用工具
)

# 解析工具调用
message = response.choices[0].message
if message.tool_calls:
    for tool_call in message.tool_calls:
        print(f"工具: {tool_call.function.name}")
        print(f"参数: {json.loads(tool_call.function.arguments)}")
else:
    print(f"直接回答: {message.content}")
```

### 8.4.2 工具设计原则

```python
"""
工具设计最佳实践：

1. 单一职责
   ✅ 好: search_products, get_product_detail, create_order
   ❌ 差: do_everything

2. 清晰的描述
   ✅ 好: "根据产品名称或关键词搜索产品库，返回匹配的产品列表和价格"
   ❌ 差: "搜索东西"

3. 明确的参数
   ✅ 好: {"query": {"type": "string", "description": "产品名称或关键词"}}
   ❌ 差: {"data": {"type": "object", "description": "数据"}}

4. 返回结构化结果
   ✅ 好: {"status": "success", "products": [...], "total": 10}
   ❌ 差: "找到了10个产品：产品A，产品B..."

5. 处理错误优雅
   不要在工具内部抛未处理的异常，返回错误信息让Agent决定怎么办
"""
```

---

## 8.5 记忆系统

### 8.5.1 记忆层次

```python
"""
Agent记忆的三种类型：

1. 工作记忆 (Working Memory)
   = 当前对话的上下文
   存储在 messages 数组中
   Token限制 → 需要管理

2. 短期记忆 (Short-term Memory)
   = 当前会话中记住的关键信息
   如：用户姓名、偏好、当前任务状态
   存储为键值对

3. 长期记忆 (Long-term Memory)
   = 跨会话的信息
   如：用户历史行为、知识库内容
   存储在向量数据库或传统数据库
   第9步RAG就是长期记忆的实现方式
"""
```

### 8.5.2 实现记忆管理

```python
class MemoryManager:
    """Agent记忆管理器"""
    
    def __init__(self, max_short_term: int = 50):
        self.short_term: Dict[str, Any] = {}
        self.max_short_term = max_short_term
    
    def remember(self, key: str, value: Any):
        """记录一条信息"""
        self.short_term[key] = value
        
        # LRU淘汰
        if len(self.short_term) > self.max_short_term:
            oldest = next(iter(self.short_term))
            del self.short_term[oldest]
    
    def recall(self, key: str) -> Optional[Any]:
        """回忆一条信息"""
        return self.short_term.get(key)
    
    def summarize_context(self) -> str:
        """将当前记忆总结为文本（注入到Prompt中）"""
        if not self.short_term:
            return "（暂无上下文记忆）"
        
        items = []
        for key, value in self.short_term.items():
            items.append(f"- {key}: {value}")
        return "当前已知信息：\n" + "\n".join(items)
    
    def compress_history(self, messages: List[Dict], 
                         keep_last: int = 10) -> List[Dict]:
        """
        压缩对话历史 — 处理Token超限问题
        
        策略：保留最近N条消息，更早的用LLM总结成一段摘要
        """
        if len(messages) <= keep_last:
            return messages
        
        # 待总结的旧消息
        old_messages = messages[:-keep_last]
        recent_messages = messages[-keep_last:]
        
        # 用LLM总结旧消息
        summary = self._summarize(old_messages)
        
        # 构造新的消息列表
        compressed = [
            {"role": "system", "content": f"之前的对话摘要：{summary}"}
        ] + recent_messages
        
        return compressed
    
    def _summarize(self, messages: List[Dict]) -> str:
        """用LLM总结对话"""
        full_text = "\n".join([
            f"[{m['role']}]: {m.get('content', '')}" 
            for m in messages if m.get('content')
        ])
        
        # 调用LLM生成摘要（实现省略）
        return f"对话摘要（{len(messages)}条消息）..."
```

---

## 8.6 构建你的第一个Agent

### 8.6.1 旅行规划Agent

```python
class TravelAgent:
    """旅行规划Agent — 集成多个工具的完整示例"""
    
    def __init__(self, llm_client):
        self.llm = llm_client
        self.tools = self._register_tools()
        self.loop = AgentLoop(llm_client, self.tools, max_steps=8)
    
    def _register_tools(self) -> List[Tool]:
        """注册所有工具"""
        return [
            Tool(
                "search_flights", 
                "搜索航班信息，返回航班号和价格",
                {
                    "from_city": {"type": "string", "description": "出发城市"},
                    "to_city": {"type": "string", "description": "目的城市"},
                    "date": {"type": "string", "description": "日期，格式YYYY-MM-DD"},
                },
                self._search_flights,
            ),
            Tool(
                "search_hotels",
                "搜索酒店信息",
                {
                    "city": {"type": "string", "description": "城市"},
                    "check_in": {"type": "string"},
                    "check_out": {"type": "string"},
                    "budget": {"type": "number", "description": "预算上限(元)"},
                },
                self._search_hotels,
            ),
            Tool(
                "get_attractions",
                "获取城市的热门景点推荐",
                {
                    "city": {"type": "string", "description": "城市名"},
                },
                self._get_attractions,
            ),
            Tool(
                "create_itinerary",
                "生成最终旅行行程表",
                {
                    "flights": {"type": "string", "description": "航班信息"},
                    "hotels": {"type": "string", "description": "酒店信息"},
                    "attractions": {"type": "string", "description": "景点信息"},
                    "days": {"type": "integer", "description": "旅行天数"},
                },
                self._create_itinerary,
            ),
        ]
    
    def plan_trip(self, request: str) -> str:
        """规划一次旅行"""
        enhanced_request = f"""
        请帮用户规划一次旅行。

        用户请求：{request}

        请按以下步骤执行：
        1. 先确认出发地、目的地、日期、预算等关键信息
        2. 搜索航班选项
        3. 搜索酒店选项
        4. 搜索目的地景点
        5. 生成完整的旅行行程表

        如果用户信息不完整，先向用户询问再继续。
        """
        return self.loop.run(enhanced_request)
    
    def _search_flights(self, from_city: str, to_city: str, date: str) -> str:
        """模拟航班搜索"""
        return json.dumps({
            "flights": [
                {"flight": "CA1234", "time": "08:00-10:30", "price": 1280},
                {"flight": "MU5678", "time": "14:00-16:30", "price": 980},
                {"flight": "CZ9012", "time": "19:00-21:30", "price": 1560},
            ]
        }, ensure_ascii=False)
    
    # ... 其他工具实现省略
```

---

## 8.7 阶段练习

### 练习1：构建一个计算器Agent
给Agent配备四则运算工具，让它能回答需要多步计算的数学问题。

### 练习2：多工具编排
设计一个Agent，至少使用3个不同的工具，完成一个端到端的任务。

### 练习3：记忆系统
给Agent加上短期记忆，让它能记住对话中的用户偏好。

---

## 8.8 常见问题

### Q1: Agent和RAG有什么区别？

| | Agent | RAG |
|---|---|---|
| 核心目标 | 自主执行任务 | 增强知识检索 |
| 工作方式 | 多步推理+行动 | 检索+生成 |
| 工具调用 | 核心能力 | 可选的检索工具 |
| 自主性 | 高 | 低 |
| 关系 | Agent可以用RAG作为工具 | RAG是Agent的一个组件 |

### Q2: Agent会不会失控？

**答：** 通过以下措施限制：
1. max_steps 限制最大步数
2. 工具白名单
3. 输出审查
4. 人工确认关键操作

### Q3: 什么时候用Agent，什么时候用简单LLM调用？

**答：**
- 简单问答/翻译/总结 → 普通LLM调用
- 需要多步操作/外部信息/工具调用 → Agent

---

> **✅ 阶段完成检查清单：**
> - [ ] 理解Agent的核心循环（感知→规划→执行→观察）
> - [ ] 掌握了Function Calling/Tool Use机制
> - [ ] 能创建一个使用2个以上工具的Agent
> - [ ] 知道如何管理Agent的对话记忆
> - [ ] 完成3个阶段练习
>
> **下一步：** [第9步：RAG基础](../09-RAG基础/README.md)
