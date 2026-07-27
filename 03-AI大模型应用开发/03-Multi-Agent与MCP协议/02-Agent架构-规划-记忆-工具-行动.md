# 02 - Agent 架构：规划、记忆、工具、行动

> 🎯 Agent 的四大组件中，规划是灵魂（决定做什么）、工具是手脚（怎么做）、记忆是经验（记住什么）、行动是执行（做到什么程度）

---

## 目录

1. [规划 Planning](#1-规划-planning)
2. [记忆 Memory](#2-记忆-memory)
3. [工具 Tools](#3-工具-tools)
4. [行动 Action](#4-行动-action)
5. [Agent 设计模式](#5-agent-设计模式)

---

## 1. 规划 Planning

### 1.1 规划的两种范式

```text
① 先规划后执行（Plan-then-Execute）：
   收到目标 → 制定完整计划 → 逐步执行
   优势：全局最优、可预览
   劣势：计划可能不切实际

② 边执行边规划（Plan-as-you-Go / ReAct）：
   思考 → 行动 → 观察 → 思考 → 行动 → ...
   优势：灵活、可根据反馈调整
   劣势：可能短视

现代 Agent 通常混合使用：先粗粒度规划，再细粒度 ReAct 执行
```

### 1.2 任务分解

```python
# LLM 将复杂任务分解为子任务
def decompose_task(goal, llm):
    prompt = f"""将以下目标分解为有序的子任务列表（每个子任务应独立且可验证）：
目标：{goal}

输出格式：
1. [子任务描述] → 所需工具：[工具名]
2. [子任务描述] → 所需工具：[工具名]
..."""
    return llm(prompt)

# 示例：
# 目标："分析公司上季度销售数据并生成报告"
# 分解结果：
# 1. 查询上季度销售数据 → 工具：QueryDB
# 2. 计算各产品线增长率 → 工具：Calculator
# 3. 生成可视化图表 → 工具：PlotChart
# 4. 撰写分析报告 → 工具：LLM(无外部工具)
```

---

## 2. 记忆 Memory

### 2.1 三种记忆类型

| 类型 | 存储内容 | 生命周期 | 实现方式 |
|------|----------|:---:|------|
| **工作记忆** | 当前任务的上下文 | 单次任务 | LLM 上下文窗口 |
| **短期记忆** | 当前会话的历史 | 单次会话 | 对话历史列表 |
| **长期记忆** | 跨会话的知识和偏好 | 永久 | 向量数据库 / RAG |

```python
class AgentMemory:
    def __init__(self, vector_store):
        self.working = []           # 当前任务上下文
        self.short_term = []        # 对话历史 (最近 N 轮)
        self.long_term = vector_store  # 持久化记忆
    
    def remember(self, key_info):
        """重要信息存入长期记忆"""
        self.long_term.add(key_info)
    
    def recall(self, query, top_k=5):
        """从长期记忆中检索相关信息"""
        return self.long_term.search(query, top_k)
```

### 2.2 记忆管理策略

```text
① 滑动窗口：只保留最近 N 轮对话（简单但丢失老信息）
② 摘要记忆：定期对历史对话做摘要 → 压缩存储
③ 向量记忆：将记忆 Embedding → 向量库 → 按相关性检索
④ 反射记忆：从经历中提取"经验教训" → 类似 Reflexion
```

---

## 3. 工具 Tools

### 3.1 工具的定义

```python
# OpenAI Function Calling 格式的工具定义
tools = [{
    "type": "function",
    "function": {
        "name": "search_knowledge_base",
        "description": "搜索企业内部知识库。用于查找公司政策、技术文档、产品说明。",
        "parameters": {
            "type": "object",
            "properties": {
                "query": {
                    "type": "string",
                    "description": "搜索关键词或自然语言查询"
                },
                "max_results": {
                    "type": "integer",
                    "description": "返回结果数量，默认5",
                    "default": 5
                }
            },
            "required": ["query"]
        }
    }
}]
```

### 3.2 工具设计原则

```text
① 单一职责：一个工具只做一件事
② 清晰描述：description 是 LLM 选择工具的唯一依据 → 关键！
③ 明确参数：类型、必填/可选、默认值
④ 返回结构化：JSON 格式、包含 success/error/data
⑤ 幂等安全：读操作可重试、写操作需确认
```

---

## 4. 行动 Action

### 4.1 行动-观察循环

```python
def agent_loop(goal, llm, tools, max_steps=15):
    """Agent 主循环：思考 → 行动 → 观察 → 思考 → ..."""
    context = [{"role": "system", "content": SYSTEM_PROMPT}]
    context.append({"role": "user", "content": f"目标：{goal}"})
    
    for step in range(max_steps):
        response = llm(context, tools=tools)
        
        if response.has_tool_calls():
            # 行动
            for tool_call in response.tool_calls:
                result = execute_tool(tool_call)
                context.append({
                    "role": "tool",
                    "content": str(result),
                    "tool_call_id": tool_call.id
                })
        else:
            # 最终回答
            return response.content
    
    return "达到最大步数限制"
```

---

## 5. Agent 设计模式

| 模式 | 结构 | 适用场景 |
|------|------|----------|
| **单Agent** | 一个 LLM + 工具集 | 简单任务、单一领域 |
| **流水线** | Agent A → Agent B → Agent C | 有明确先后顺序的任务 |
| **路由器** | 意图识别 → 分发到专家 Agent | 多领域客服 |
| **辩论** | 多个 Agent 各自论证 → 评判 | 需要多角度分析 |
| **层级** | 管理者 Agent + 执行者 Agent | 复杂项目 |

---

## 核心要点回顾

- 规划 = 任务分解（先规划后执行 / 边执行边规划）
- 记忆 = 工作(单任务) + 短期(单会话) + 长期(向量库)
- 工具 = 清晰描述是 LLM 正确选工具的保证
- 行动 = 思考→行动→观察→思考 的循环
