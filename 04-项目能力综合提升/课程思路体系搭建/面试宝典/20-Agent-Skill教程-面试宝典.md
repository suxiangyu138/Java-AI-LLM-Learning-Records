# Agent Skill 面试宝典
> 基于课程大纲全面覆盖面试高频考点 — Skill 机制、Multi-Agent、设计模式全解析

## 目录
1. [一、基础概念速答](#一基础概念速答18题)
2. [二、深度原理剖析](#二深度原理剖析12题)
3. [三、实战场景题](#三实战场景题10题)
4. [四、手写代码题](#四手写代码题8题)
5. [五、系统设计题](#五系统设计题5题)
6. [六、常见坑点与最佳实践](#六常见坑点与最佳实践)
7. [七、面试回答模板](#七面试回答模板top-5高频题的结构化回答模板)
8. [八、快速查漏补缺 Checklist](#八快速查漏补缺-checklist)

---

## 一、基础概念速答（18题）

### 1.1 什么是 AI Agent？
AI Agent（智能体）是一个能够**感知环境、自主规划、调用工具、执行动作并记忆历史**的大模型应用系统。它不同于单纯的 LLM 聊天——Agent 能够主动制定计划、使用工具、从错误中恢复。

> 🎯 Agent = LLM（推理核心）+ Planning（规划能力）+ Tools（工具调用）+ Memory（记忆系统）

### 1.2 Agent 的 Skill 机制是什么？
Skill（技能）机制是 Agent 的**能力模块化封装**方式。每个 Skill 包含一组相关的 Tool、Prompt 模板和状态处理逻辑。其核心价值在于 **Progressive Disclosure（渐进式暴露）**——Agent 根据当前任务动态加载和激活所需 Skill，而非一次性加载所有能力。

> 💡 类比：Skill 就像人的"专业技能"，Agent 是"项目经理"。项目经理不需要同时精通所有领域，而是在需要时调用对应专家（Skill）。

### 1.3 什么是 Progressive Disclosure（渐进式暴露）？
Progressive Disclosure 是一种**按需加载**设计原则。在 Agent 系统中，这意味着：
| 层级 | 暴露内容 | 触发条件 |
|------|---------|---------|
| 第 1 层 | 核心对话能力 | 始终可用 |
| 第 2 层 | Skill 名称和描述列表 | Agent 规划阶段 |
| 第 3 层 | Skill 内的具体 Tool 集合 | 选定 Skill 后 |
| 第 4 层 | Tool 的参数和文档 | 决定调用 Tool 时 |

> ⚠️ 不遵循 Progressive Disclosure 的 Agent 会在 Prompt 中塞入所有工具描述，导致 Token 浪费和推理质量下降。

### 1.4 Multi-Agent 和 Skills-Agent 的区别是什么？
| 对比维度 | Multi-Agent（多智能体） | Skills-Agent（单智能体+技能） |
|---------|------------------------|------------------------------|
| 架构模式 | 多个 Agent 相互协作 | 单个 Agent + 多个 Skill 模块 |
| 通信方式 | Agent 间通过消息传递 | Agent 内通过函数调用 |
| 状态共享 | 需要共享内存/消息总线 | 共享同一上下文窗口 |
| 复杂度 | 高（协调、冲突、死锁） | 低（单线程控制流） |
| 扩展性 | 水平扩展（加 Agent） | 垂直扩展（加 Skill） |
| 容错性 | 单个 Agent 故障影响有限 | 核心 Agent 故障则整体不可用 |
| 适用场景 | 跨域复杂任务、异构系统 | 单域多步骤、功能丰富场景 |

### 1.5 Multi-Agent 和 Skills-Agent 各在什么时候用？
**用 Multi-Agent 的场景：**
- 任务涉及多个**专业领域**（如金融 + 法律 + 技术）
- 需要**并行**处理互不依赖的子任务
- 各子任务使用**不同的模型或配置**
- 需要**隔离的上下文**（防止信息泄露）

**用 Skills-Agent 的场景：**
- 任务流程**线性或有限分支**
- 所有功能属于**同一领域**
- 追求**低延迟**（避免 Agent 间通信开销）
- 需要**简单的状态管理和调试**

> 💡 一个常见的错误是"什么场景都用 Multi-Agent"。实际上 80% 的场景用 Skills-Agent 更合适，只有真正需要跨域隔离时才上 Multi-Agent。

### 1.6 WorkFlow 和 Agent 的区别？
| 对比维度 | WorkFlow（工作流） | Agent（智能体） |
|---------|-------------------|----------------|
| 决策方式 | 预定义逻辑（if-else/状态机） | LLM 动态推理决策 |
| 灵活性 | 低，流程固定 | 高，可自主规划 |
| 确定性 | 高，结果可预测 | 低，每次可能不同 |
| 调试难度 | 低，按图索骥 | 高，需要 Log 分析 |
| 适用场景 | 确定性的业务流水线 | 复杂的开放式任务 |
| 典型实现 | LangGraph 条件边、State Machine | ReAct Loop、Plan-Execute |

> 🎯 WorkFlow 适合"已知怎么做"的任务，Agent 适合"需要探索怎么做"的任务。

### 1.7 什么是 ReAct 模式？
ReAct = **Re**asoning + **Act**ion（推理 + 行动）。Agent 交替进行"思考（Thought）→ 行动（Action）→ 观察（Observation）"，形成循环：

```
Thought: 用户想知道今日天气，我需要查询天气 API
Action: call weather_api(city="北京")
Observation: {"temp": 28, "condition": "晴"}
Thought: 天气是 28 度晴天，组织回复
Final Answer: 北京今天 28°C，天气晴朗！
```

> 💡 ReAct 是最主流的 Agent 设计模式，OpenAI Function Calling、LangChain Agent 均基于此。

### 1.8 什么是 REWOO 模式？
REWOO = **Re**asoning **W**ith**o**ut **O**bservation。与 ReAct 的区别在于：**将推理和行动分离为两个独立阶段**。
| 阶段 | 工作内容 |
|------|---------|
| Plan（规划阶段） | LLM 推理出完整的执行计划，生成所有工具调用指令 |
| Execute（执行阶段） | 无需 LLM 参与，仅执行器按计划顺序调用工具并收集结果 |

**优势：** 省去每次 Action 后等待 LLM 推理的延迟，节省 Token。
**劣势：** 执行过程中无法动态调整计划，灵活性低于 ReAct。

### 1.9 什么是 LLMComp（LLM Compiler）模式？
LLMComp 将 Agent 的控制流程视为**编译器优化**问题。核心思想：
- 将多步工具调用**合并编译**为并行执行计划
- LLM 生成一个多分支的**DAG（有向无环图）**，标识哪些步骤可并行
- 执行器并行执行可并行的工具调用，大幅降低延迟

> 🎯 LLMComp 适合工具调用密集的场景，如同时查询多个 API 获取信息。

### 1.10 什么是 Reflexion 模式？
Reflexion 是一种**自我反思 + 反馈迭代**的 Agent 模式：
1. Actor（执行者）：完成任务并产生输出
2. Evaluator（评估者）：评估输出质量、检查错误
3. Reflector（反思者）：将评估结果总结为经验，反馈给 Actor

```
Actor: 生成代码 → Evaluator: 发现语法错误 → Reflector: 记录错误类型 →
Actor: 修正代码 → Evaluator: 通过 → 输出结果
```

### 1.11 什么是 LAT（Language Agent Tree）模式？
LAT = **Language Agent Tree**。将 Agent 的决策过程建模为**树形搜索**：
- 每个节点是一个"思维 + 行动"步骤
- 多条推理路径同时探索
- 使用 BFS/DFS 在树中搜索最优路径
- 叶子节点是最终答案

> ⚠️ LAT 计算成本高，适合需要深度探索的复杂推理任务。

### 1.12 LCEL（LangChain Expression Language）是什么？
LCEL 是 LangChain 的**声明式链式语法**，用 `|` 管道符连接组件：

```python
# LCEL 语法示例
from langchain_core.output_parsers import StrOutputParser
from langchain_core.prompts import ChatPromptTemplate
from langchain_openai import ChatOpenAI

prompt = ChatPromptTemplate.from_template("请用{language}回答：{question}")
model = ChatOpenAI(model="gpt-4")

chain = prompt | model | StrOutputParser()
result = chain.invoke({"language": "中文", "question": "什么是 Agent？"})
```

**核心特性：** Runnable 协议、自动批处理、流式支持、并行执行、重试和回退。

### 1.13 Function Calling（函数调用）是什么？
Function Calling 是 LLM 的一种**结构化输出能力**。模型在生成回复时，可以输出一个结构化的函数调用请求，而非纯文本：

```json
{
  "name": "get_weather",
  "arguments": {
    "location": "Beijing",
    "unit": "celsius"
  }
}
```

**关键点：** 模型"调用"函数并非实际执行，而是描述"应该调用什么函数和参数"，执行由应用层完成。

### 1.14 Tool Overload（工具过载）问题是什么？
当 Agent 绑定 **超过 30 个工具（Tool）** 时出现的系统性退化：
| 问题 | 表现 |
|------|------|
| 选择困难 | LLM 在大量工具中难以选中正确的 |
| Token 暴涨 | 工具描述占据大量 Prompt 上下文 |
| 幻觉调用 | 模型编造不存在的函数名或参数 |
| 推理退化 | 长篇工具描述干扰核心推理能力 |

> 💡 解决 Tool Overload 的核心方法就是 **Skill 机制** — 将工具分组分层、渐进式暴露。

### 1.15 什么是 Coze（扣子）？
Coze 是字节跳动推出的 **AI Agent 开发平台**，提供可视化 Agent 搭建能力：
- 内置插件市场（搜索、阅读、绘图等）
- 知识库管理（上传文档、分段、Embedding）
- 工作流编排（可视化拖拽）
- 多 Agent 对话模式（Bot 间协作）
- 一键发布到飞书、微信、Web

### 1.16 Prompt Engineering 的核心方法有哪些？
| 方法 | 说明 | 适用场景 |
|------|------|---------|
| Soft Prompt（软提示） | 通过可学习的连续向量调优模型行为 | 模型微调、Few-shot 替代 |
| Few-shot（少样本） | 在 Prompt 中给出若干输入-输出示例 | 分类、抽取、格式化 |
| CoT（思维链 Chain-of-Thought） | 引导模型逐步推理，输出中间步骤 | 数学推理、逻辑题 |
| ToT（思维树 Tree-of-Thought） | 同时探索多条推理路径并评估 | 复杂规划、博弈推理 |

### 1.17 Agent 的系统提示词（System Prompt）设计原则？
1. **角色定义**：明确 identity（"你是一个客服助手"）
2. **能力边界**：说明能做什么、不能做什么
3. **工具声明**：列出可用工具和使用条件
4. **行为约束**：回复风格、安全规则
5. **输出格式**：结构化输出要求

### 1.18 Agent 的记忆系统有哪几种？
| 记忆类型 | 存储方式 | 生命周期 | 示例 |
|---------|---------|---------|------|
| 短期记忆 | 上下文窗口 | 单次对话 | Chat History |
| 长期记忆 | 向量数据库 | 跨会话 | 用户偏好、事实 |
| 工作记忆 | 临时变量 | 单次任务 | 中间计算结果 |
| 共享记忆 | 消息总线 | Agent 间可见 | Multi-Agent 通信 |

---

## 二、深度原理剖析（12题）

### 2.1 ReAct 模式的核心循环是如何实现的？
```python
# ReAct Loop 核心逻辑（伪代码）
state = {"input": user_query, "steps": []}

while not state.get("finished"):
    # 1. 推理步：LLM 生成 Thought + Action
    thought_action = llm.generate(
        prompt=build_react_prompt(state),
        tools=tool_schemas
    )
    # 2. 执行步：解析 Action 并执行工具
    if thought_action.type == "action":
        observation = execute_tool(
            thought_action.tool_name,
            thought_action.tool_args
        )
        state["steps"].append({
            "thought": thought_action.thought,
            "action": thought_action.action_str,
            "observation": observation
        })
    # 3. 结束条件：LLM 生成 Final Answer
    elif thought_action.type == "final":
        state["finished"] = True
        state["answer"] = thought_action.answer
```

> 🎯 ReAct 的核心优势在于"边做边想"，每一步都能基于上一步的观察调整推理。

### 2.2 Skill 机制如何解决 Tool Overload 问题？
**Key Insight：** 将 30+ 工具按领域分组为 Skill，Agent 先选 Skill 再选 Tool。

**分层决策流程：**
```
Level 1: 用户输入 → 判断需要哪个 Skill（如 "数据分析Skill"）
Level 2: 激活 Skill → 暴露该 Skill 下的工具列表（如 SQL 查询、图表生成）
Level 3: 选择具体 Tool → 填充参数 → 执行
```

```python
# Skill 注册机制示意
class SkillRegistry:
    def __init__(self):
        self.skills = {}
    
    def register_skill(self, name, description, tools):
        self.skills[name] = {
            "description": description,
            "tools": tools  # 每个工具包含 name, description, parameters
        }
    
    def select_skill(self, user_input):
        """Level 1: 选择 Skill"""
        prompt = f"用户需求：{user_input}\n可用 Skills：{self.list_skills()}"
        return llm_choose_skill(prompt)
    
    def select_tool(self, skill_name, task_context):
        """Level 2: 在 Skill 内选择 Tool"""
        skill = self.skills[skill_name]
        prompt = f"任务上下文：{task_context}\n可用 Tools：{skill['tools']}"
        return llm_choose_tool(prompt)
```

**效果对比：**
| 指标 | 无 Skill（30+ 工具平铺） | 有 Skill（分层分组） |
|------|------------------------|--------------------|
| Prompt Token 消耗 | ~8K（全部工具描述） | ~2K（仅当前 Skill 描述）|
| 工具选择准确率 | ~65% | ~92% |
| 平均推理延迟 | ~3s | ~1.2s |

### 2.3 Multi-Agent 的通信协议如何设计？
Multi-Agent 通信有三种模式：
| 模式 | 特点 | 适用场景 |
|------|------|---------|
| 点对点（Direct） | Agent A 直接发消息给 Agent B | 少量 Agent、固定路由 |
| 消息总线（Message Bus） | Agent 通过中心队列订阅/发布 | 中等规模、灵活路由 |
| 协调器（Orchestrator） | 中央 Orchestrator 调度所有子 Agent | 复杂任务、依赖管理 |

```python
# 消息总线模式示例
class MessageBus:
    def __init__(self):
        self.agents = {}
        self.queue = []
    
    def register(self, agent_name, agent_instance):
        self.agents[agent_name] = agent_instance
    
    def send(self, from_agent, to_agent, message):
        self.queue.append({
            "from": from_agent,
            "to": to_agent,
            "content": message
        })
    
    def deliver(self):
        while self.queue:
            msg = self.queue.pop(0)
            if msg["to"] in self.agents:
                self.agents[msg["to"]].receive(msg)
```

### 2.4 LangChain Agent 与 LangGraph Agent 的核心区别？
| 对比维度 | LangChain Agent | LangGraph Agent |
|---------|----------------|-----------------|
| 控制流 | ReAct Loop 硬编码 | 自定义 Graph 拓扑 |
| 状态管理 | AgentExecutor 内置状态 | 显式定义 State 对象 |
| 循环控制 | 固定 max_iterations | 自定义循环条件 |
| 分支逻辑 | 不支持 | 条件边、并行边 |
| 人机交互 | 有限 | 支持 Interrupt/Resume |
| 调试友好度 | 黑盒 | 每步可追溯、可视 |
| 适用版本 | LangChain < 0.3 | LangChain >= 0.3 推荐 |

> 💡 LangGraph 可以看作是 LangChain Agent 的"替代升级方案"。

### 2.5 AutoGen 和 CrewAI 的架构差异？
| 对比维度 | AutoGen（微软） | CrewAI |
|---------|----------------|--------|
| 核心模式 | 对话式多 Agent | 角色式多 Agent |
| Agent 定义 | AssistantAgent + UserProxyAgent | Agent + Task + Crew |
| 工具集成 | 通过 UserProxy 代理执行 | Agent 直接绑定 Tool |
| 人机参与 | 原生支持 Human-in-the-loop | 需额外配置 |
| 异步支持 | 原生 asyncio | 支持但有限 |
| 学习曲线 | 陡峭（概念较多） | 平滑（API 简洁） |
| 典型场景 | 代码生成、复杂推理 | 内容创作、自动化流水线 |

**CrewAI 示例：**
```python
from crewai import Agent, Task, Crew

researcher = Agent(
    role="研究员",
    goal="收集和分析行业数据",
    backstory="资深行业分析师",
    tools=[search_tool, web_scraper]
)

writer = Agent(
    role="写手",
    goal="撰写报告",
    backstory="专业科技记者",
    tools=[llm_writer]
)

research_task = Task(
    description="研究 AI Agent 行业趋势",
    agent=researcher
)

write_task = Task(
    description="基于研究结果撰写报告",
    agent=writer
)

crew = Crew(agents=[researcher, writer], tasks=[research_task, write_task])
crew.kickoff()
```

### 2.6 LCEL 的核心组件和运行机制？
LCEL 基于 **Runnable 协议（Runnable Protocol）**：
```python
# Runnable 协议核心方法
class Runnable:
    def invoke(self, input) -> output
    def batch(self, inputs) -> outputs
    def stream(self, input) -> Iterator[output]
    def astream(self, input) -> AsyncIterator[output]
```

**LCEL 管道操作符重载原理：**
```python
# `|` 运算符实际上创建了 RunnableSequence
chain = prompt | model | parser
# 等价于
from langchain_core.runnables import RunnableSequence
chain = RunnableSequence(first=prompt, last=RunnableSequence(first=model, last=parser))

# 自动并行
parallel_chain = {"summary": chain1, "keywords": chain2}
# RunnableParallel 自动并行执行两个子链
```

### 2.7 REWOO 模式的 Plan 阶段如何生成可执行计划？
```python
# REWOO Plan 生成
def plan_generation(task):
    """Phase 1: LLM 生成无依赖依赖的执行计划"""
    plan_prompt = f"""
    任务：{task}
    可用工具：{tools_descriptions}
    
    请生成一个执行计划，格式：
    Step 1: tool_name(param1="value1", param2="value2")
    Step 2: tool_name(param=Step1.result)
    ...
    
    注意：
    - 标记步骤间的数据依赖关系
    - 无依赖的步骤可以并行
    - 每步只调用一个工具
    """
    plan = llm.generate(plan_prompt)
    return parse_plan_to_dag(plan)

def execute_plan(plan_dag):
    """Phase 2: 按 DAG 并行/串行执行"""
    results = {}
    while plan_dag.has_unexecuted():
        # 获取所有依赖已满足的步骤
        ready_steps = plan_dag.get_ready_steps()
        # 并行执行
        for step in ready_steps:
            step.inputs = resolve_inputs(step, results)
            results[step.id] = execute_tool(step.tool, step.inputs)
        plan_dag.mark_executed(ready_steps)
    return results
```

### 2.8 Reflexion 模式的反馈循环阈值如何设计？
| 要素 | 设计建议 |
|------|---------|
| 最大迭代次数 | 3-5 次，防止无限循环 |
| 评估标准 | 精确指标（准确率、覆盖率）+ 模糊指标（LLM 评估）|
| 反馈粒度 | 错误类型分类（语法、逻辑、语义）|
| 停止条件 | 达到质量标准 or 达到最大次数 |
| 记忆持久化 | 将反馈经验存入外部 Memory 供下次复用 |

### 2.9 Prompt Engineering 中 Few-shot、CoT、ToT 如何选择？
| 方法 | 适合场景 | 不适合场景 | Token 开销 |
|------|---------|-----------|-----------|
| Few-shot | 分类、格式化、抽取 | 需要深度推理的任务 | 低-中 |
| CoT | 数学、逻辑、推理 | 简单问答、事实查询 | 中 |
| ToT | 复杂规划、博弈、搜索 | 实时交互、简单任务 | 高 |

> 💡 经验法则：简单任务用 Few-shot，复杂推理用 CoT，超复杂探索用 ToT。

### 2.10 Soft Prompt（软提示）与 Hard Prompt（硬提示）的区别？
| 对比维度 | Hard Prompt | Soft Prompt |
|---------|-------------|-------------|
| 表现形式 | 自然语言文本 | 连续向量/Embedding |
| 可学性 | 手动编写 | 梯度优化学习 |
| 可迁移性 | 跨模型不可用 | 需重新训练 |
| Token 开销 | 实际 Token 数 | 通常 10-20 个虚拟 Token |
| 效果上限 | 依赖编写技巧 | 可逼近微调效果 |
| 典型方法 | Few-shot, CoT | Prompt Tuning, P-Tuning |

### 2.11 Function Calling 的底层实现机制？
```
LLM Function Calling 底层流程：
1. Tool Schema 注入 → 将工具定义（JSON Schema）拼入 System Prompt
2. 模型生成 → 模型在生成文本时，在内部 Token 分布中"规划" 
   输出特殊 Token 序列标记函数调用开始
3. 结构化提取 → 从生成内容中提取 JSON 格式的函数名和参数
   （gpt-4 等模型在预训练阶段就训练了这种输出格式）
4. 终止判断 → 模型可能连续调用多个函数（Multi-turn Function Calling）

关键本质：Function Calling 不是"模型调用了函数"，而是
"模型输出了结构化的函数调用描述文本"。
```

### 2.12 Agent 平台的演进路线？
```
                     ┌──────────────────┐
                     │   Coze / Dify    │   ← 可视化、零代码
                     │  (Agent 平台)     │
                     ├──────────────────┤
                     │AutoGen / CrewAI  │   ← 多 Agent、框架级
                     │ (Agent 框架)      │
                     ├──────────────────┤
                     │LangGraph / 自定义  │  ← 图编排、精细化控制
                     │ (Graph 编排)      │
                     ├──────────────────┤
                     │    ReAct / Raw   │   ← 最底层、最灵活
                     │  (原始 Agent)     │
                     └──────────────────┘
越往下灵活度越高、可控性越强；越往上开发效率越高、门槛越低。
```

---

## 三、实战场景题（10题）

### 3.1 场景：客服系统集成 50+ 工具，Agent 决策准确率只有 60%
**问题分析：** 典型的 Tool Overload 问题。

**解决方案：**
1. 按业务域划分 Skill（订单、退款、物流、商品）
2. 实现 Progressive Disclosure：先判定业务域，再激活对应 Skill
3. 每个 Skill 内工具数量控制在 10 个以内
4. 结果：准确率从 60% 提升到 ~90%，Token 消耗降低 50%

### 3.2 场景：需要同时查询天气、股票、新闻三个不相关的 API
**推荐方案：** Skills-Agent + LLMComp 模式

**原因：**
- 三个 API 相互独立，不需跨 Agent 通信
- 可用 LLMComp 生成并行执行计划
- 单 Agent 即可低成本完成

```python
# 并行调用
from concurrent.futures import ThreadPoolExecutor

def parallel_execute(plan):
    with ThreadPoolExecutor(max_workers=3) as executor:
        futures = {
            executor.submit(call_api, step): step
            for step in plan.steps
        }
        results = {}
        for future in futures:
            step = futures[future]
            results[step.id] = future.result()
    return results
```

### 3.3 场景：用户说"帮我规划一次日本旅行，包括机票、酒店、行程"
**推荐方案：** Skills-Agent（单 Agent + 多个 Skill）

**流程：**
1. Agent 识别需要：航班查询 Skill + 酒店查询 Skill + 行程规划 Skill
2. 按依赖顺序激活：航班 Skill（确定日期）→ 酒店 Skill（确定地点）→ 行程 Skill（生成攻略）
3. 每步的结果传递给下一步作为上下文

**为什么不选 Multi-Agent：** 三个子任务高度依赖顺序结果，且属于同一领域（旅游），Single Agent 完全可处理。

### 3.4 场景：构建一个合同审查系统，需要法律 + 财务 + 技术三方评估
**推荐方案：** Multi-Agent（三个角色 Agent）

**原因：**
- 三个领域差异大（法律、财务、技术）
- 每个 Agent 需要独立的行业知识库和系统提示
- 需要多轮协商讨论（如"这个条款从法律上合规，但从财务上有风险"）

```python
# 三 Agent 合同审查协作
legal_agent = Agent(system_prompt="你是资深法律顾问...", tools=[law_db])
finance_agent = Agent(system_prompt="你是财务分析师...", tools=[finance_db])
tech_agent = Agent(system_prompt="你是技术架构师...", tools=[tech_specs])

orchestrator = Orchestrator(agents=[legal_agent, finance_agent, tech_agent])
result = orchestrator.run("审查这份 SaaS 服务合同")
```

### 3.5 场景：Agent 调用链路过长（10+ 步），频繁出错
**推荐方案：** 引入 Reflexion 模式

**具体做法：**
1. 每 3 步插入一个 Checkpoint，进行中间评估
2. 评估发现错误时，回滚到最近正确的 Checkpoint
3. 将错误类型和修正经验写入 Feedback Memory
4. 后续任务中，优先避开已知的失败路径

### 3.6 场景：需要 Agent 支持联网搜索 + 知识库查询 + 数据库操作
**推荐方案：** Skills-Agent + LCEL 链路编排

```python
skill_search = Skill("search", tools=[web_search, news_api])
skill_kb = Skill("knowledge_base", tools=[vector_db_query, doc_retriever])
skill_db = Skill("database", tools=[sql_executor, data_analyzer])

agent = SkillsAgent(
    skills=[skill_search, skill_kb, skill_db],
    llm=ChatOpenAI(model="gpt-4")
)
```

### 3.7 场景：Agent 总是选择同一个工具（Tool Bias）
**问题根因：** LLM 对 Prompt 中靠前或描述更详细的工具有偏好。

**解决方案：**
- 工具列表随机排序
- 平衡各工具描述的详细程度（字数和格式统一）
- 增加工具使用的竞争逻辑（如多个工具评分后选中最高分）

### 3.8 场景：Agent 输出不稳定，同一问题有时好有时差
**推荐方案：** 多方面的策略组合
| 维度 | 策略 |
|------|------|
| 输出格式 | 使用 Pydantic Output Parser 强制结构化 |
| 推理稳定性 | CoT + 少样本示例固定 |
| 解码参数 | 降低 temperature（0.1-0.3）|
| 回退机制 | 首次失败后重试（不同 temperature）|
| 验证层 | 输出后通过 Validator 校验再返回 |

### 3.9 场景：在 Coze 平台搭建一个多轮对话客服 Agent
**设计方案：**
1. 创建 Bot，设定角色（"电商客服助手"）
2. 配置知识库：上传商品手册、退换货政策
3. 添加插件：物流查询、订单查询
4. 设置工作流：用户意图识别 → 分流到不同处理流程
5. 发布到微信公众号

### 3.10 场景：Agent 需要处理包含敏感信息的数据
**设计方案：**
1. 使用 Skills-Agent 而非 Multi-Agent（减少数据暴露面）
2. 在 Tool 层实现数据脱敏（电话、身份证号打码）
3. 对话历史定期清理，不持久化到外部存储
4. 设置 System Prompt 约束："不要提及具体的用户身份信息"

---

## 四、手写代码题（8题）

### 4.1 实现一个 ReAct Agent 核心循环
```python
from typing import TypedDict, Optional
import json

class ReActStep(TypedDict):
    thought: str
    action: Optional[str]
    action_input: Optional[dict]
    observation: Optional[str]

def react_agent(user_query: str, tools: list, llm, max_steps: int = 10):
    steps = []
    system_prompt = f"""
    你是一个 AI Agent，可以使用以下工具：
    {json.dumps([t["schema"] for t in tools], ensure_ascii=False, indent=2)}
    
    请按照格式回复：
    Thought: 你的推理
    Action: 工具名称（如果需要调用工具）
    Action Input: {{"参数名": "参数值"}}
    Observation: 工具返回结果
    ...（重复 Thought/Action/Observation）...
    Final Answer: 最终答案
    """
    
    for i in range(max_steps):
        prompt = system_prompt + f"\n用户问题：{user_query}\n历史步骤：{steps}"
        response = llm.invoke(prompt)
        
        if "Final Answer:" in response:
            return response.split("Final Answer:")[-1].strip()
        
        # 解析 Action
        action = extract_action(response)
        if action:
            tool = next(t for t in tools if t["name"] == action["name"])
            observation = tool["func"](**action["args"])
            steps.append({
                "thought": extract_thought(response),
                "action": action["name"],
                "action_input": action["args"],
                "observation": observation
            })
    
    return "达到最大步数，任务未完成"
```

### 4.2 实现 Skills-Agent (Single Agent with Skill Registry)
```python
class Tool:
    def __init__(self, name: str, description: str, func):
        self.name = name
        self.description = description
        self.func = func
    
    def to_schema(self) -> dict:
        return {"name": self.name, "description": self.description}

class Skill:
    def __init__(self, name: str, description: str, tools: list[Tool]):
        self.name = name
        self.description = description
        self.tools = {t.name: t for t in tools}
    
    def describe(self) -> str:
        tools_desc = "\n".join([
            f"  - {t.name}: {t.description}" for t in self.tools.values()
        ])
        return f"### {self.name}\n{self.description}\n可用工具：\n{tools_desc}"

class SkillsAgent:
    def __init__(self, skills: list[Skill], llm):
        self.skills = {s.name: s for s in skills}
        self.llm = llm
    
    def invoke(self, user_input: str) -> str:
        # Level 1: Select Skill
        skill_prompt = (
            f"用户需求：{user_input}\n\n"
            f"可用技能列表：\n" +
            "\n".join([
                f"- {s.name}: {s.description}" 
                for s in self.skills.values()
            ]) +
            "\n请选择最匹配的一个技能名称："
        )
        chosen_skill_name = self.llm.invoke(skill_prompt).strip()
        skill = self.skills[chosen_skill_name]
        
        # Level 2: Execute within Skill
        exec_prompt = (
            f"用户需求：{user_input}\n"
            f"已选择技能：{skill.name}\n"
            f"可用工具：\n{skill.describe()}\n"
            "请决定是否调用工具或直接回答："
        )
        return self.llm.invoke(exec_prompt)
```

### 4.3 实现 Multi-Agent 协调器
```python
class Agent:
    def __init__(self, name: str, system_prompt: str, llm, tools: list = None):
        self.name = name
        self.system_prompt = system_prompt
        self.llm = llm
        self.tools = tools or []
    
    def run(self, task: str, context: list = None) -> str:
        full_context = context or []
        prompt = f"{self.system_prompt}\n任务：{task}\n上下文：{full_context}"
        return self.llm.invoke(prompt)

class Orchestrator:
    def __init__(self, agents: list[Agent]):
        self.agents = {a.name: a for a in agents}
    
    def run(self, task: str, pipeline: list[str]) -> dict:
        results = {}
        for agent_name in pipeline:
            agent = self.agents[agent_name]
            context = [f"{k} 的结果：{v}" for k, v in results.items()]
            result = agent.run(task, context)
            results[agent_name] = result
        return results

# 使用示例
planner = Agent("planner", "你是一个任务规划师，负责拆解任务...", llm)
coder = Agent("coder", "你是一个 Python 工程师...", llm, tools=[run_python])
reviewer = Agent("reviewer", "你是代码审查员...", llm)

orchestrator = Orchestrator([planner, coder, reviewer])
result = orchestrator.run("编写一个冒泡排序算法", ["planner", "coder", "reviewer"])
```

### 4.4 实现 Function Calling Schema 解析器
```python
import json
from typing import Any

class FunctionCallingParser:
    def parse(self, llm_response: str) -> dict | None:
        """从 LLM 回复中提取函数调用"""
        # 支持两种格式：JSON 格式和代码块格式
        try:
            return json.loads(llm_response)  # 纯 JSON
        except json.JSONDecodeError:
            pass
        
        # 代码块格式,如 ```json 块
        if "```json" in llm_response:
            json_str = llm_response.split("```json")[1].split("```")[0]
            return json.loads(json_str.strip())
        
        # Function Calling 格式 (tool_call 标记)
        if '"name"' in llm_response and '"arguments"' in llm_response:
            # 提取第一个完整的函数调用 JSON
            start = llm_response.find('{')
            end = llm_response.rfind('}') + 1
            return json.loads(llm_response[start:end])
        
        return None

# Tool Schema 定义示例
def create_tool_schema(name: str, description: str, parameters: dict) -> dict:
    return {
        "type": "function",
        "function": {
            "name": name,
            "description": description,
            "parameters": {
                "type": "object",
                "properties": parameters,
                "required": [k for k, v in parameters.items() 
                            if v.get("required", False)]
            }
        }
    }
```

### 4.5 实现一个简单的 LCEL 链
```python
from typing import Callable, Any

class Runnable:
    """简化版 Runnable 实现"""
    def __init__(self, func: Callable = None):
        self.func = func
    
    def invoke(self, input: Any) -> Any:
        if self.func:
            return self.func(input)
        return input
    
    def __or__(self, other: "Runnable") -> "RunnableSequence":
        return RunnableSequence(self, other)

class RunnableSequence(Runnable):
    def __init__(self, *steps: Runnable):
        self.steps = steps
    
    def invoke(self, input: Any) -> Any:
        result = input
        for step in self.steps:
            result = step.invoke(result)
        return result

class RunnableParallel(Runnable):
    def __init__(self, **branches: Runnable):
        self.branches = branches
    
    def invoke(self, input: Any) -> dict:
        return {
            name: branch.invoke(input)
            for name, branch in self.branches.items()
        }

# 使用
prompt = Runnable(lambda x: f"请回答：{x['question']}")
model = Runnable(lambda x: f"模型收到：{x}")
parser = Runnable(lambda x: x.upper())

chain = prompt | model | parser
result = chain.invoke({"question": "什么是 AI?"})
print(result)  # "模型收到：请回答：什么是 AI?"
```

### 4.6 实现 Tool Overload 检测器
```python
def detect_tool_overload(tools: list, llm_model: str = "gpt-4") -> dict:
    """检测工具集是否存在 Tool Overload 风险"""
    total_tools = len(tools)
    total_desc_tokens = sum(len(t["description"].split()) for t in tools)
    
    risk_score = 0
    warnings = []
    
    if total_tools > 20:
        risk_score += 30
        warnings.append(f"工具数量 {total_tools} > 20，建议分组为 Skill")
    
    if total_tools > 30:
        risk_score += 30
        warnings.append(f"工具数量 {total_tools} > 30，严重过载！必须分层")
    
    if total_desc_tokens > 2000:
        risk_score += 20
        warnings.append(f"工具描述总 Token 数约 {total_desc_tokens}，建议精简描述")
    
    if is_gpt4(lm_model) and total_desc_tokens > 4000:
        risk_score += 20
        warnings.append("GPT-4 尚未解决的 Tool Overload 风险建议使用 Skill 机制规避")
    
    return {
        "risk_score": risk_score,
        "level": "high" if risk_score >= 50 else "medium" if risk_score >= 20 else "low",
        "warnings": warnings,
        "suggestion": "建议使用 Skill 机制进行工具分组和渐进式暴露" if risk_score >= 20 else "当前配置正常"
    }
```

### 4.7 实现 Agent 的运行日志追踪
```python
import datetime

class AgentTracer:
    def __init__(self):
        self.logs = []
    
    def trace(self, agent_name: str, stage: str, input_data, output_data):
        entry = {
            "timestamp": datetime.datetime.now().isoformat(),
            "agent": agent_name,
            "stage": stage,
            "input": str(input_data)[:200],
            "output": str(output_data)[:200],
            "latency_ms": 0
        }
        self.logs.append(entry)
        return entry
    
    def visualize_last_run(self):
        for log in self.logs[-20:]:
            print(f"[{log['timestamp'][:19]}] {log['agent']} | {log['stage']}")
            print(f"  Input: {log['input'][:80]}...")
            print(f"  Output: {log['output'][:80]}...")
            print()
    
    def export_trace(self, filepath: str):
        import json
        with open(filepath, "w", encoding="utf-8") as f:
            json.dump(self.logs, f, ensure_ascii=False, indent=2)
```

### 4.8 实现 CoT（Chain-of-Thought）Prompt 构建器
```python
class CoTPromptBuilder:
    def __init__(self, task_type: str = "reasoning"):
        self.task_type = task_type
        self.examples = []
    
    def add_example(self, question: str, reasoning_steps: list[str], answer: str):
        self.examples.append({
            "question": question,
            "steps": reasoning_steps,
            "answer": answer
        })
    
    def build(self, question: str) -> str:
        prompt = "请逐步推理并给出最终答案。\n\n"
        
        for ex in self.examples:
            prompt += f"问题：{ex['question']}\n"
            for i, step in enumerate(ex["steps"], 1):
                prompt += f"步骤 {i}：{step}\n"
            prompt += f"答案：{ex['answer']}\n\n"
        
        prompt += f"问题：{question}\n步骤 1："
        return prompt

# 使用示例
builder = CoTPromptBuilder()
builder.add_example(
    "小明有 5 个苹果，给了小红 2 个，又买了 3 个，现在有几个？",
    ["初始有 5 个苹果", "给小红 2 个，剩下 5-2=3 个", "又买了 3 个，现在有 3+3=6 个"],
    "6 个"
)

prompt = builder.build("超市有 20 箱牛奶，卖了 8 箱，又进货 15 箱，现在有多少箱？")
# 输出包含 CoT 引导的 Prompt
```

---

## 五、系统设计题（5题）

### 5.1 设计一个企业级 Agent 平台
**需求：** 支持多部门、多 Agent、多工具管理

**架构设计：**
```
┌──────────────────────────────────────────┐
│            API Gateway / BFF              │
├──────────────────────────────────────────┤
│         Agent Orchestrator Layer          │
│  ┌────────┐ ┌────────┐ ┌────────┐       │
│  │ AgentA │ │ AgentB │ │ AgentC │ ...    │
│  └────────┘ └────────┘ └────────┘       │
├──────────────────────────────────────────┤
│            Skill Registry                 │
│  ┌────────┐ ┌────────┐ ┌────────┐       │
│  │ 客服Skill│ │ 订单Skill│ │ 数据Skill│ ...  │
│  └────────┘ └────────┘ └────────┘       │
├──────────────────────────────────────────┤
│         Tool Execution Engine             │
│  (Rate Limiter / Retry / Circuit Breaker) │
├──────────────────────────────────────────┤
│     Memory & Knowledge (RAG Layer)        │
│  ┌────────┐ ┌────────┐ ┌────────┐       │
│  │ 短期记忆 │ │ 长期记忆 │ │ 知识库  │       │
│  └────────┘ └────────┘ └────────┘       │
└──────────────────────────────────────────┘
```

**关键设计决策：**
- 启用 Progressive Disclosure，防止 Tool Overload
- 引入 Agent Tracer 实现全链路追踪
- 所有工具调用通过统一 Gateway（权限校验、限流、审计）

### 5.2 设计一个多 Agent 协作系统
**需求：** 实现 Research Agent（研究）+ Write Agent（写作）+ Review Agent（审查）

```
用户请求 → Planner(初评) → Research(收集资料) → Write(生成初稿)
                                      ↓
完成 ← Publish(发布) ← Review(修改审查) ← Feedback(收集反馈)
```

**通信设计：**
- Research Agent → Write Agent：结构化数据（Fact Sheets）
- Write Agent → Review Agent：文本 + Source 引用
- Review Agent → Write Agent：修改建议（Diff 格式）

**容错设计：**
- 每个 Agent 超时 30s
- Review 阶段循环最多 3 次
- 全部超时走人工 Fallback

### 5.3 设计一个面向电商的 Agent Skill 体系
| Skill 名称 | 包含工具 | 触发条件 |
|-----------|---------|---------|
| 订单 Skill | 查订单、改地址、取消订单、退货申请 | 用户提及订单号/订单相关 |
| 商品 Skill | 商品搜索、比价、查看详情、库存查询 | 用户想买东西/看商品 |
| 物流 Skill | 查物流、预估送达时间、修改配送方式 | 用户询问物流/配送 |
| 售后 Skill | 退款申请、投诉、评价管理 | 用户不满意/要退款 |
| 客服 Skill | 转人工、常见问题 FAQ、投诉建议 | 其他 Skill 无法处理 |

**设计原则：** 每个 Skill 的 Tool 数量不超过 8 个，超出的工具继续拆分。

### 5.4 设计一个 Function Calling 的 Fallback 机制
```python
class FunctionCallingWithFallback:
    def __init__(self, llm, tools: list):
        self.primary_llm = llm
        self.fallback_llm = llm  # 可以配置不同模型
        self.tools = tools
    
    def call_with_fallback(self, user_input: str) -> dict:
        # Attempt 1: 主模型调用
        try:
            result = self.primary_llm.invoke_with_tools(
                user_input, self.tools
            )
            if result.tool_call and self.validate_call(result.tool_call):
                return self.execute_and_verify(result.tool_call)
        except Exception as e:
            logger.warning(f"Primary call failed: {e}")
        
        # Attempt 2: 降级策略 - 仅用核心工具
        try:
            core_tools = self.tools[:5]  # 仅 5 个核心工具
            result = self.fallback_llm.invoke_with_tools(
                f"请仅使用以下工具：\n{core_tools}\n用户输入：{user_input}",
                core_tools
            )
            if result.tool_call:
                return self.execute_and_verify(result.tool_call)
        except Exception as e:
            logger.error(f"Fallback failed: {e}")
        
        # Attempt 3: 纯 LLM 回答（无工具）
        return {"type": "text", "content": self.primary_llm.invoke(user_input)}
    
    def validate_call(self, tool_call: dict) -> bool:
        """校验函数调用参数合法性"""
        tool_name = tool_call.get("name")
        args = tool_call.get("arguments", {})
        tool_schema = next(
            (t for t in self.tools if t["name"] == tool_name), 
            None
        )
        if not tool_schema:
            return False
        required = tool_schema.get("parameters", {}).get("required", [])
        return all(r in args for r in required)
```

### 5.5 设计一个 Agent 可观测性（Observability）系统
| 监控维度 | 指标 | 告警阈值 |
|---------|------|---------|
| 延迟 | P50/P95/P99 响应时间 | P95 > 5s 告警 |
| Token 消耗 | 每次对话 Token 数 | > 4K 提醒，> 8K 告警 |
| 工具调用 | 调用频率、成功率、错误率 | 成功率 < 90% 告警 |
| Agent 循环 | 单次任务 ReAct 步数 | 步数 > 10 告警 |
| 工具过载 | 单次 Prompt 中工具数 | > 20 告警 |
| 模型错误 | 无效 JSON、参数校验失败 | 频率 > 5% 告警 |

**实现手段：**
1. 每个 Agent 调用插入 Tracer Hook
2. 输出结构化日志（JSON 格式）
3. 聚合到 ELK / Grafana 可视化
4. 设置 Webhook 告警通知

---

## 六、常见坑点与最佳实践

| # | 坑点 | 问题表现 | 最佳实践 |
|---|------|---------|---------|
| 1 | Tool Overload | Agent 决策慢、选择错误工具 | 使用 Skill 机制分组，每层 Tool ≤ 10 个 |
| 2 | 无限循环 | ReAct Loop 不停调用工具 | 设置 max_steps（推荐 5-10），使用 Checkpoint |
| 3 | Hard Prompt 过长 | LLM 忽略中间内容 | 核心指令靠前、分段标识、控制总 Token |
| 4 | Multi-Agent 通信混乱 | Agent 间重复劳动、结果矛盾 | 设计清晰的消息协议，使用 Orchestrator 调度 |
| 5 | 忽视错误恢复 | 一步出错全链崩溃 | 实现 Retry + Fallback + 人机交互 |
| 6 | 工具返回值过大 | 上下文被撑满 | 工具返回摘要化，长文本写入外部存储 |
| 7 | 无状态约束 | Agent 同一错误反复出现 | 引入 Reflexion + Memory，记录失败经验 |
| 8 | 不分场景 Multi-Agent | 过度设计、延迟高、调试困难 | 80% 场景用 Skills-Agent，真正跨域再上 Multi-Agent |
| 9 | 忽略 Progressive Disclosure | Prompt 中无差别暴露所有能力 | 按层级暴露：Skill 名 → Tool 列表 → Tool 参数 |
| 10 | CoT 与 Tool Call 混用 | 推理步骤和工具调用互相干扰 | 明确区分推理阶段和工具调用阶段（参考 REWOO）|

> ⚠️ **最常犯的错误：** 一上来就上 Multi-Agent。99% 的初版 Agent 用 Skills-Agent + ReAct 就能解决问题，后续根据实际瓶颈逐步升级。

---

## 七、面试回答模板（Top 5 高频题的结构化回答模板）

### Q1：请解释 Agent 的 Skill 机制
**面试官意图：** 考察对 Agent 架构设计中模块化和可扩展性的理解。

**模板回答：**
"Agent 的 Skill 机制是一种能力模块化封装方式。每个 Skill 封装一组相关的工具、Prompt 模板和状态逻辑。它的核心价值在于 Progressive Disclosure（渐进式暴露）——Agent 不需要在每次推理时都面对全部工具列表，而是先选择 Skill，再在 Skill 内选择工具。

举个例子，如果一个电商助手有 50 个工具，平铺给 LLM 会导致 Tool Overload。但如果我们按领域划分为订单 Skill、物流 Skill、商品 Skill，Agent 先判定用户的意图属于哪个 Skill，再加载该 Skill 的 5-8 个工具，准确率可以从 60% 提升到 90% 以上。"

### Q2：Multi-Agent 和 Skills-Agent 怎么选？
**面试官意图：** 考察架构选型能力，避免过度设计。

**模板回答：**
"选择的关键是看任务的领域边界和依赖关系。
- 如果任务属于**单一领域**且步骤间有**线性依赖**，用 Skills-Agent 更高效、更简单。
- 如果任务涉及**多个专业领域**（如法律+财务+技术），需要**独立的上下文隔离**，或者需要**并行处理**，用 Multi-Agent。

我通常遵循"Skills-Agent 优先"原则——先用 Skills-Agent 实现，只有当遇到跨域上下文冲突、需要不同模型配置、或者 Agent 间需要多轮协商时，才拆分为 Multi-Agent。"

### Q3：ReAct、REWOO、LLMComp、Reflexion、LAT 的设计模式你怎么选？
**面试官意图：** 考察对主流 Agent 设计模式的理解和场景化决策能力。

**模板回答：**
"这五种模式可以看作一个从简单到复杂的频谱：
| 模式 | 适合场景 | 原因 |
|------|---------|------|
| ReAct | 通用场景 | 最灵活，边思考边行动 |
| REWOO | 确定性高、追求低延迟 | 推理和行动分离，节省 Token |
| LLMComp | 多工具可并行调用 | 将独立调用合并为 DAG 并行执行 |
| Reflexion | 需要质量迭代 | 自我反思 + 多轮修正 |
| LAT | 超复杂推理 | 树形探索多条路径再择优 |

我一般从 ReAct 开始，当遇到具体的性能瓶颈或质量问题时，再针对性地切换到更合适的模式。"

### Q4：LangGraph 和 LangChain Agent 有什么区别？
**面试官意图：** 考察对 LangChain 生态演进和技术选型的理解。

**模板回答：**
"LangGraph 实际上是 LangChain Agent 的下一代替代方案。LangChain Agent 使用硬编码的 ReAct Loop，控制流固定，扩展难度大。而 LangGraph 提供了完整的图编排能力，可以自定义状态、节点、条件边，支持循环和并行。

本质上，LangChain Agent 是一个 '黑盒'，LangGraph 是一个 '白盒'。在 LangChain 0.3+ 版本中，官方已推荐使用 LangGraph 替代 AgentExecutor。如果项目是新启动的，我建议直接用 LangGraph。"

### Q5：你怎么处理 Agent 的工具过载（Tool Overload）问题？
**面试官意图：** 考察实际工程经验和架构设计能力。

**模板回答：**
"Tool Overload 是 Agent 工程中最常见的问题。我的解决方案分三层：
1. **架构层**：使用 Skill 机制实现分层分组，渐进式暴露。
2. **工程层**：设置工具数量监控告警，当单 Agent 工具超 20 个时触发架构评审。
3. **模型层**：对工具描述进行标准化，统一格式和长度，减少 LLM 的选择偏差。

实践证明，这三层策略结合使用，可以将 Agent 的工具选择准确率从 65% 以下提升到 90% 以上。"

---

## 八、快速查漏补缺 Checklist

**基础概念（自测能 3 句话讲清楚）：**
- [ ] 什么是 AI Agent？Agent = LLM + Planning + Tools + Memory
- [ ] 什么是 Skill 机制？能力模块化 + 渐进式暴露
- [ ] 什么是 Progressive Disclosure？按需逐层暴露能力，避免信息过载
- [ ] Multi-Agent vs Skills-Agent 选型依据
- [ ] WorkFlow vs Agent 的本质区别
- [ ] ReAct 循环（Thought → Action → Observation）
- [ ] Function Calling 的本质（不是模型执行函数，是输出结构化描述）
- [ ] Tool Overload 问题和解决方案

**设计模式（能画出示意图）：**
- [ ] ReAct：推理 + 行动交替
- [ ] REWOO：规划 + 执行分离
- [ ] LLMComp：工具调用编译为 DAG
- [ ] Reflexion：Actor → Evaluator → Reflector 循环
- [ ] LAT：树形搜索多条路径

**框架对比（能列出优缺点）：**
- [ ] LangChain vs LangGraph
- [ ] AutoGen vs CrewAI
- [ ] LCEL 语法和使用

**Prompt Engineering：**
- [ ] Soft Prompt vs Hard Prompt
- [ ] Few-shot, CoT, ToT 的适用场景
- [ ] System Prompt 结构设计

**手写代码（能现场写出核心逻辑）：**
- [ ] ReAct Agent 核心循环
- [ ] Skills-Agent（Skill Registry）
- [ ] Multi-Agent Orchestrator
- [ ] Function Calling 解析器
- [ ] 简版 LCEL Runnable
- [ ] Tool Overload 检测器
- [ ] Agent Tracer
- [ ] CoT Prompt 构建器

**系统设计（能画出架构图）：**
- [ ] 企业级 Agent 平台设计
- [ ] 多 Agent 协作系统设计
- [ ] Agent Skill 体系设计（电商案例）
- [ ] Function Calling Fallback 机制
- [ ] Agent 可观测性系统

**常见坑点（能脱口而出）：**
- [ ] 不分场景上 Multi-Agent（过度设计）
- [ ] Tool Overload 不处理（50+ 工具平铺）
- [ ] 无错误恢复（一步出错全链崩溃）
- [ ] 无状态管理（Agent 没有记忆）
- [ ] 忽视 Progressive Disclosure

---

> 🎯 **面试冲刺提示：** Agent 相关面试的重点已经不是"知不知道概念"，而是"有没有在实际项目中踩过坑、如何解决"。建议结合自己的项目经验，准备 1-2 个具体的 Case Study。Skill 机制和 Multi-Agent 选型是最高频考点。
