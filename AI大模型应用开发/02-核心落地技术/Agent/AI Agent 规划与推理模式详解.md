# AI Agent 规划与推理模式详解（Java 后端 + AI 全栈实战版）

> **文档定位**：AI Agent 核心技术文档 | 推理与规划模式深度解析
> **前置阅读**：AI Agent核心知识点.md
> **核心问题**：Agent 如何思考？如何拆解任务？如何自我修正？

---

## 一、推理模式全景图

```
Agent 推理模式
├── 单步推理
│   ├── Direct（直接回答）
│   └── CoT（Chain of Thought，思维链）
├── 多步循环
│   ├── ReAct（Reasoning + Acting）
│   ├── ReWOO（Reason WithOut Observation）
│   └── RAISE（ReAct + 记忆增强）
├── 树/图搜索
│   ├── ToT（Tree of Thoughts）
│   ├── GoT（Graph of Thoughts）
│   └── A* Search
├── 计划驱动
│   ├── Plan-and-Execute（先计划再执行）
│   └── Plan-and-Solve（逐步求解）
└── 自我改进
    ├── Self-Reflection（自我反思）
    ├── Self-Critique（自我批评）
    └── Reflexion（反思 + 记忆）
```

---

## 二、CoT（思维链）

### 2.1 原理

让 LLM 在输出最终答案前**显式输出推理步骤**，提升复杂推理题的正确率。

```
❌ 直接输出："答案是 15"
✅ CoT 输出：
   1. 小明有 5 个苹果
   2. 小红给了他 3 个 → 5 + 3 = 8
   3. 他吃了 2 个 → 8 - 2 = 6
   4. 买了一袋 10 个 → 6 + 10 = 16
   5. 分给同学 1 个 → 16 - 1 = 15
   最终答案：15
```

### 2.2 CoT Prompt 模板

```
请一步步思考，先列出推理步骤，再给出最终答案。
问题：{question}
让我们一步步分析：
```

---

## 三、ReAct（Reasoning + Acting）—— 最主流模式

### 3.1 核心循环

```
     ┌──────────────────────────────────┐
     ↓                                  │
  Thought（思考）→ Action（行动）→ Observation（观察）
                         │
                    Final Answer（最终答案）
```

| 步骤 | 含义 | 示例 |
|---|---|---|
| **Thought** | LLM 内部推理，决定下一步 | "我需要查北京的天气" |
| **Action** | 发出工具调用 | `get_weather("北京")` |
| **Observation** | 接收工具返回值 | `{temp: 25°C, weather: "晴"}` |
| **循环** | 根据观察再次 Thought | "25 度晴天，适合出游" |
| **Final Answer** | 认为任务完成，输出 | "北京今天 25°C，晴天，适合出行" |

### 3.2 ReAct 完整示例

```
用户问题：今天的北京和上海哪个更适合户外活动？

Thought 1: 我需要知道两个城市的天气信息
Action 1: get_weather("北京", "2026-05-27")
Observation 1: {city: "北京", temp: 25, weather: "晴", aqi: 45}

Thought 2: 北京是晴天，AQI 也很好。还需要上海的信息
Action 2: get_weather("上海", "2026-05-27")
Observation 2: {city: "上海", temp: 28, weather: "雷阵雨", aqi: 85}

Thought 3: 北京晴天 25°C AQI 好，上海雷阵雨。显然北京更适合
Final Answer: 今天北京更适合户外活动。
原因：北京晴天 25°C AQI 优，上海有雷阵雨不建议户外。
```

### 3.3 ReAct Prompt 模板

```
你是一个具备工具调用能力的 AI 助手。请按以下格式回复：

Thought: 你的推理过程
Action: tool_name(tool_input)
Observation: 工具返回结果
... (可重复多次)
Thought: 最终推理
Final Answer: 给用户的最终回答
```

---

## 四、ToT（思维树）

### 4.1 原理

CoT 的问题是只有**一条推理链**，ToT 会**同时探索多条推理路径**：

```
                    问题
              /    |    |    \
           思路A  思路B 思路C 思路D
          /   \     |     \
       继续 舍弃   继续   继续
        ↓           ↓      ↓
       最终A       最终B  最终C → 选最优
```

### 4.2 实战步骤

```
1. 生成（Generate）：对于当前节点，生成 N 个候选"下一步"
2. 评估（Evaluate）：对每个候选打分
3. 扩展（Expand）：选择高分的继续探索
4. 回溯（Backtrack）：低分路径回退
5. 选择（Select）：所有路径中选最优
```

**适用场景**：写作、博弈、数学证明、策略规划

---

## 五、Plan-and-Execute（先计划再执行）

### 5.1 模式

```
ReAct 的问题：走一步想一步，思路可能不连贯
Plan-and-Execute：先全局规划，再逐步执行

阶段 1（Plan）：LLM 先制定完整计划
阶段 2（Execute）：按计划逐步执行，每步可调用工具
```

### 5.2 示例

```
用户：帮我调研市面上 3 款主流 Agent 框架并生成对比报告

Plan 阶段：
1. 搜索确定当前最主流的 3 款 Agent 框架
2. 对每款框架查询：核心架构、优点、缺点、适用场景
3. 整理信息生成对比表格
4. 输出 Markdown 格式报告

Execute 阶段：
Step 1: web_search("2026 年主流 Agent 框架") → 确定 LangGraph, CrewAI, AutoGen
Step 2: web_search("LangGraph 架构优缺点") → 收集信息
Step 3: web_search("CrewAI 架构优缺点") → 收集信息
Step 4: web_search("AutoGen 架构优缺点") → 收集信息
Step 5: 整合生成报告
```

---

## 六、Reflexion（反思机制）

### 6.1 核心思想

Agent 执行后**自我评估**，如果不满意就**带着教训重试**：

```
Attempt 1 → 失败 → 反思原因 → 记录教训
    ↓
Attempt 2 → 改进 → 可能仍失败 → 再次反思
    ↓
Attempt N → 成功 ✅
```

### 6.2 反思 Prompt 模板

```
你之前尝试解决问题时失败了。请反思：
1. 之前的方法有什么问题？
2. 为什么会失败？
3. 下次应该如何改进？

原始任务：{task}
尝试记录：{trajectory}
实际结果：{result vs expected}

反思：
```

---

## 七、模式选择指南

| 场景 | 推荐模式 | 原因 |
|---|---|---|
| 简单事实问答 | Direct | 无需推理链 |
| 数学/逻辑推理 | CoT | 逐步思考 |
| 需要外部工具 | **ReAct** | 思考-行动-观察循环 |
| 复杂规划+执行 | **Plan-and-Execute** | 全局观 |
| 多方案选择 | ToT | 探索多条路径 |
| 成功率要求高 | **Reflexion** | 失败后自我改进 |
| 多步非工具任务 | CoT + Self-Consistency | 多次推理投票 |

---

## 八、实战：伪代码实现 ReAct 循环

```python
def react_loop(user_query: str, tools: list, max_iterations: int = 10):
    messages = [{"role": "user", "content": user_query}]
    trajectory = []
    
    for i in range(max_iterations):
        # 1. LLM 生成 Thought + Action
        response = llm.chat(messages, tools=tools)
        
        if response.has_final_answer():
            return response.final_answer, trajectory
        
        # 2. 执行 Action
        tool_name = response.tool_name
        tool_args = response.tool_args
        observation = execute_tool(tool_name, tool_args)
        
        # 3. 记录轨迹
        trajectory.append({
            "thought": response.thought,
            "action": f"{tool_name}({tool_args})",
            "observation": observation
        })
        
        # 4. 将 Observation 加入上下文继续循环
        messages.append({"role": "assistant", "content": response.raw})
        messages.append({"role": "tool", "content": str(observation)})
    
    raise Exception("Agent 达到最大迭代次数仍未完成任务")
```

---

## 九、面试核心要点

1. **ReAct 的三个步骤？** Thought → Action → Observation，循环到给出 Final Answer
2. **CoT 和 ReAct 区别？** CoT 输出推理链但不调用工具，ReAct 在推理中嵌入工具调用
3. **Plan-and-Execute 比 ReAct 好在哪？** 先全局规划再执行，思路更连贯，减少无效步骤
4. **Reflexion 机制的核心？** 失败后反思原因生成教训(lesson)，下次带着教训重试
5. **ToT 和 CoT 的区别？** CoT 单链推理，ToT 多路径同时探索+评估+剪枝

---

## 十、极简总结

```
CoT = 显式推理步骤，不调工具，适合逻辑题
ReAct = Thought → Action → Observation 循环，主流 Agent 模式
ToT = 多路径探索 + 评估剪枝，适合写作/策略
Plan-and-Execute = 先做计划再执行，长任务更连贯
Reflexion = 失败后反思改进，提高最终成功率
选择 = 要调工具用 ReAct，长任务用 Plan-and-Execute，要准确用 Reflexion
```
