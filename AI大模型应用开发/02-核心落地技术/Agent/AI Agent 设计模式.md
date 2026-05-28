# AI Agent 设计模式（实战版）

> **文档定位**：AI Agent 核心技术 | 设计模式与架构最佳实践
> **核心问题**：Agent 开发中有哪些可复用的设计模式？每种模式解决什么问题？

---

## 一、Agent 设计模式全景

```
Agent 设计模式
├── 结构模式（如何组织 Agent）
│   ├── Single Agent（单 Agent）
│   ├── Router Agent（路由分发）
│   ├── Orchestrator-Worker（编排-执行）
│   └── Peer-to-Peer（对等协作）
├── 行为模式（如何决策执行）
│   ├── ReAct Loop（思考-行动循环）
│   ├── Plan-and-Execute（先计划再执行）
│   ├── Reflection（反思优化）
│   └── Chain-of-Thought（思维链）
├── 交互模式（如何与用户/环境交互）
│   ├── Human-in-the-Loop（人机协同）
│   ├── Streaming（流式交互）
│   └── Ambiguity Clarification（歧义澄清）
└── 优化模式（如何提升质量）
    ├── Self-Consistency（自一致性投票）
    ├── Mixture-of-Agents（多 Agent 混合）
    └── Fallback Chain（降级链）
```

---

## 二、Router Agent（路由分发模式）

### 2.1 模式

```
用户输入 → Router Agent → 判断意图 → 分发到专业 Agent
                                  ├→ 客服 Agent
                                  ├→ 技术支持 Agent
                                  └→ 销售 Agent
```

### 2.2 实现

```python
class RouterAgent:
    """根据用户意图路由到对应的专业 Agent"""
    
    def __init__(self):
        self.specialists = {
            "客服": CustomerServiceAgent(),
            "技术支持": TechSupportAgent(),
            "销售": SalesAgent(),
        }
    
    def route(self, user_query: str) -> str:
        # 1. 意图识别
        intent = llm.chat(f"""
        判断以下用户问题的意图，只输出一个词：
        客服 | 技术支持 | 销售
        
        用户问题：{user_query}
        """).strip()
        
        # 2. 路由
        agent = self.specialists.get(intent)
        if agent:
            return agent.handle(user_query)
        
        # 3. 兜底
        return self.specialists["客服"].handle(user_query)
```

**适用场景**：多渠道客服系统、企业内部服务台

---

## 三、Orchestrator-Worker（编排-执行模式）

### 3.1 模式

```
用户复杂任务
       ↓
Orchestrator → 分解成子任务
       ↓
   ┌───┼───┐
   ↓   ↓   ↓
Worker Worker Worker  → 并行/顺序执行
   ↓   ↓   ↓
   └───┼───┘
       ↓
Orchestrator → 汇总结果
```

### 3.2 实现

```python
class OrchestratorAgent:
    def execute(self, task: str) -> dict:
        # 1. 分解
        subtasks = self.plan(task)
        # [
        #   {"agent": "researcher", "task": "调研竞品"},
        #   {"agent": "analyst", "task": "分析数据"},
        #   {"agent": "writer", "task": "生成报告"}
        # ]
        
        # 2. 并行/顺序执行
        results = {}
        for sub in subtasks:
            agent = self.get_agent(sub["agent"])
            results[sub["agent"]] = agent.execute(sub["task"])
        
        # 3. 汇总
        return self.synthesize(results)

class WorkerAgent:
    def __init__(self, role: str, tools: list):
        self.role = role
        self.tools = tools
    
    def execute(self, task: str) -> str:
        # Worker 遵循 ReAct 模式执行子任务
        return agent_loop.run(task, tools=self.tools)
```

**适用场景**：复杂研究报告生成、多步骤自动化

---

## 四、Human-in-the-Loop（人机协同模式）

### 4.1 模式

```
Agent 推进任务 → 遇到需要确认的点 → 暂停询问用户
                                      ↓
                                  用户反馈
                                      ↓
                              Agent 继续执行
```

### 4.2 实现

```python
class HumanInTheLoopAgent:
    def execute(self, task: str) -> str:
        # 1. 生成计划
        plan = self.make_plan(task)
        
        # 2. 展示计划，请求用户确认
        print(f"我计划执行以下步骤：")
        for i, step in enumerate(plan, 1):
            print(f"  {i}. {step}")
        
        approval = input("是否继续？(y/n): ")
        if approval.lower() != 'y':
            return "任务已取消"
        
        # 3. 执行，在关键步骤暂停
        for step in plan:
            if step.is_critical:
                print(f"即将执行：{step.description}")
                confirm = input("确认执行？(y/n): ")
                if confirm.lower() != 'y':
                    continue
            
            result = step.execute()
            print(f"✅ {step.description} → {result}")
        
        return "任务完成"
```

**适用场景**：删除操作、资金操作、高风险决策

---

## 五、Fallback Chain（降级链模式）

### 5.1 模式

```
请求 → 方案 A（最优）
         ↓ 失败
       方案 B（次优）
         ↓ 失败
       方案 C（兜底）
```

### 5.2 实现

```python
class FallbackAgent:
    def __init__(self):
        self.chain = [
            ("GPT-4o", gpt4o_client),
            ("Claude Sonnet", claude_client),
            ("DeepSeek", deepseek_client),
            ("本地模型", local_model),
        ]
    
    def call(self, messages: list) -> str:
        last_error = None
        
        for model_name, client in self.chain:
            try:
                return client.chat(messages)
            except Exception as e:
                last_error = e
                print(f"⚠️ {model_name} 失败: {e}，降级到下一个")
                continue
        
        raise Exception(f"所有模型调用失败: {last_error}")
```

**适用场景**：LLM API 高可用、工具调用容错

---

## 六、Self-Consistency（自一致性投票）

```python
def self_consistency_answer(question: str, n: int = 5) -> str:
    """对同一问题跑 N 次，投票选最一致的答案"""
    answers = []
    for _ in range(n):
        # temperature 调高增加多样性
        answer = llm.chat(question, temperature=0.7)
        answers.append(answer)
    
    # 投票选出现次数最多的
    from collections import Counter
    return Counter(answers).most_common(1)[0][0]
```

**适用场景**：需要高准确性的推理、数学证明

---

## 七、模式选型指南

| 场景 | 模式 |
|---|---|
| 简单问答/单工具调用 | **Single Agent + ReAct** |
| 多意图多专业 | **Router Agent** |
| 复杂多步骤任务 | **Orchestrator-Worker** |
| 高风险操作 | **Human-in-the-Loop** |
| 需要高可靠性 | **Fallback Chain + Self-Consistency** |
| 质量敏感（写作/策略） | **Reflection + Mixture-of-Agents** |

---

## 八、面试核心要点

1. **Router Agent 干什么？** 根据用户意图分发到对应专业 Agent
2. **Orchestrator 和 Worker 分工？** Orchestrator 拆解分配，Worker 执行
3. **Human-in-the-Loop 什么时候用？** 高风险操作、决策边界模糊
4. **Fallback Chain 解决什么问题？** LLM API 故障时的降级容错
5. **Self-Consistency 原理？** 多次推理投票，减少单次偏差

---

## 九、极简总结

```
Router = 分发到对的 Agent
Orchestrator-Worker = 拆解 → 分配 → 执行 → 汇总
Human-in-the-Loop = 关键步骤暂停让用户拍板
Fallback = A 不行用 B，B 不行用 C
Self-Consistency = 多投几次票，取最一致的答案
```
