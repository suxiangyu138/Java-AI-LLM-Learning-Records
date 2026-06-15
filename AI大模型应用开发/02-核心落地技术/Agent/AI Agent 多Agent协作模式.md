# AI Agent 多 Agent 协作模式

> **核心摘要**：当单一 Agent 无法胜任复杂任务时，多 Agent 协作系统通过分工与协调来提升整体能力。本文详细阐述五种协作模式（顺序流水线、角色分工、对话讨论、层级管理、广播协作），并介绍 A2A 协议和常见陷阱的解决方案。

## 前置阅读

- [[AI Agent核心知识点]]
- [[AI Agent 设计模式]]
- [[LangGraph状态图与多Agent协作]]

---

## 一、协作模式全景

```
多 Agent 协作
├── 顺序流水线（Pipeline）
├── 角色分工（Role-based）
├── 对话讨论（Debate / Discussion）
├── 层级管理（Hierarchical）
├── 广播协作（Broadcast / Blackboard）
└── 混合模式（Hybrid）
```

---

## 二、顺序流水线模式

### 2.1 模式说明

```
输入 → [Agent A] → [Agent B] → [Agent C] → 输出

示例：文章生成流水线
用户主题 → 研究员（搜索信息）→ 写手（撰写初稿）→ 审核员（校对润色）→ 最终文章
```

### 2.2 代码实现

```python
def pipeline_agents(topic: str) -> str:
    research = researcher_agent.run(f"研究主题：{topic}")
    draft = writer_agent.run(f"根据以下研究写文章：\n{research}")
    final = reviewer_agent.run(f"请审核并润色以下文章：\n{draft}")
    return final
```

---

## 三、角色分工模式（CrewAI 风格）

### 3.1 模式说明

定义 N 个专业角色 Agent，各自负责擅长的领域：

```
用户需求
    ↓
┌──────────────────────────┐
│  协调 Agent（项目经理）    │
│  分配任务                 │
└──┬────────┬──────────┬───┘
   ↓        ↓          ↓
┌──────┐ ┌──────┐ ┌──────┐
│研究员 │ │工程师 │ │设计师 │
└──────┘ └──────┘ └──────┘
   ↓        ↓          ↓
┌──────────────────────────┐
│  合并输出（Final Report）  │
└──────────────────────────┘
```

### 3.2 代码实现

```python
class ResearchAgent(BaseAgent):
    role = "研究员"
    tools = [web_search, paper_search]

class EngineerAgent(BaseAgent):
    role = "软件工程师"
    tools = [code_executor, api_tester]

class ReviewerAgent(BaseAgent):
    role = "技术审核"
    tools = [fact_checker]

tasks = [
    {"agent": "研究员", "desc": "调研方案可行性"},
    {"agent": "工程师", "desc": "实现技术原型"},
    {"agent": "审核", "desc": "审核代码和方案"}
]
orchestrator.run(tasks)
```

---

## 四、对话讨论模式（Debate）

### 4.1 模式说明

多个 Agent 对同一问题发表观点，相互质疑，最终达成共识：

```
Agent A: "我认为方案 X 更好，因为..."
Agent B: "但方案 X 有个问题...，我建议方案 Y"
Agent A: "你说得对，但方案 Y 也有..."
Agent C: "我综合一下，X 的 A 部分 + Y 的 B 部分"
→ 综合方案（比单独 Agent 的质量高）
```

### 4.2 代码实现

```python
def debate_agents(question: str, agents: list, rounds: int = 3):
    opinions = []
    for round_num in range(rounds):
        for agent in agents:
            context = f"""
            问题：{question}
            已发表的意见：{opinions}
            轮次：{round_num + 1}/{rounds}
            请发表你的观点，可以赞同、反驳或补充之前意见。
            """
            opinion = agent.respond(context)
            opinions.append(f"{agent.name}: {opinion}")

    summary_prompt = f"请总结以下讨论并给出最终结论：\n{opinions}"
    return summarizer_agent.respond(summary_prompt)
```

---

## 五、层级管理模式

### 5.1 模式说明

```
            ┌──────────┐
            │ Manager   │  ← 顶层管理者：分配、协调、汇总
            │ Agent     │
            └─────┬────┘
       ┌─────────┼─────────┐
       ↓         ↓         ↓
  ┌─────────┐ ┌─────────┐ ┌─────────┐
  │Worker A │ │Worker B │ │Worker C │  ← 执行层
  └─────────┘ └─────────┘ └─────────┘
```

### 5.2 代码实现

```python
class ManagerAgent:
    def delegate(self, task: str) -> dict:
        subtasks = self.plan(task)
        assignments = {}
        for sub in subtasks:
            best_worker = self.select_best_worker(sub)
            assignments[best_worker] = sub
        results = {}
        for worker, sub in assignments.items():
            results[worker] = worker.execute(sub)
        return self.summarize(results)

    def select_best_worker(self, task):
        if "搜索" in task or "调研" in task:
            return researcher
        elif "代码" in task or "开发" in task:
            return engineer
        elif "审核" in task or "检查" in task:
            return reviewer
```

---

## 六、广播协作模式（Blackboard）

```
所有 Agent 共享一个"黑板"（共享上下文），谁发现问题谁处理：

        ┌──────────────────┐
        │   Blackboard      │
        │   ┌──────────────┐│
        │   │ Task Queue    ││
        │   │ Shared State  ││
        │   └──────────────┘│
        └───┬───┬───┬───────┘
       ↑    ↑   ↑    ↑
    Agent1 Agent2 Agent3 Agent4
```

```python
class Blackboard:
    def __init__(self):
        self.tasks = []
        self.state = {}
        self.results = []

    def post_task(self, task: dict):
        self.tasks.append(task)

    def claim_task(self, agent_capabilities: list) -> dict | None:
        for task in self.tasks:
            if task["type"] in agent_capabilities:
                self.tasks.remove(task)
                return task
        return None

    def submit_result(self, result: dict):
        self.results.append(result)
        self.state.update(result["state_changes"])
```

---

## 七、A2A 协议（Agent-to-Agent Protocol）

Google 提出的多 Agent 通信标准：

```
A2A 核心概念：
- Agent Card：每个 Agent 公布自己的能力（类似名片）
- Task：标准化的任务描述格式
- Message：Agent 间通信格式
```

```json
{
  "name": "WeatherAgent",
  "description": "提供天气查询服务",
  "url": "http://weather-agent:8081",
  "capabilities": {
    "streaming": true,
    "pushNotifications": false
  },
  "skills": [
    {
      "id": "get_weather",
      "name": "天气查询",
      "description": "查询指定城市天气",
      "inputSchema": {
        "type": "object",
        "properties": {
          "city": {"type": "string"}
        }
      }
    }
  ]
}
```

---

## 八、模式选择指南

| 场景 | 推荐模式 | 原因 |
|---|---|---|
| 内容生成流水线 | 顺序流水线 | 简单可靠 |
| 软件开发协作 | 角色分工 | 不同角色做不同事 |
| 策略决策 | 对话讨论 | 多角度思考 |
| 客服 + 技术支持 | 层级管理 | 复杂问题升级 |
| 代码审查 | 对话讨论 | 多种视角找问题 |
| 多工具协作 | 层级模式 | Manager 统一调度 |

---

## 九、常见陷阱与解决

| 问题 | 表现 | 解决方案 |
|---|---|---|
| **无限循环** | 两个 Agent 反复讨论不停止 | 设置最大轮次 + 强制终止条件 |
| **信息过载** | 上下文越来越长，推理变慢 | 定期压缩历史为摘要 |
| **责任推诿** | Agent 互相说"这不是我的事" | Manager Agent 强制分配 |
| **幻觉放大** | Agent A 编造内容，Agent B 基于它继续编 | 每个 Agent 做事实核查 |

---

## 核心要点回顾

- 五种协作模式：流水线、角色分工、对话讨论、层级管理、广播
- A2A 协议：Google 提出的多 Agent 通信标准，通过 Agent Card 公开能力
- Manager Agent：任务复杂需要动态拆解和分配时使用
- 最大风险：无限循环 + 幻觉放大
- CrewAI 采用角色分工式，AutoGen 采用对话式

---

## 参考资料

1. Google Research. Agent-to-Agent Protocol 规范文档
2. CrewAI 官方文档. 多 Agent 角色协作架构
3. Microsoft Research. AutoGen 多 Agent 对话框架
4. LangGraph 官方文档. 多 Agent 工作流编排
