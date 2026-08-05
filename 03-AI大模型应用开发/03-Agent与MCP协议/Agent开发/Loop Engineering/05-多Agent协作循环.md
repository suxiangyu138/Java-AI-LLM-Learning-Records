# 05 - 多 Agent 协作循环

> 🎯 单 Agent 有天花板——上下文有限、视角单一。多 Agent 协作循环通过分工、辩论、投票，让多个 AI 像团队一样工作。本章覆盖四种核心协作模式和消息总线架构

---

## 目录

1. [四种协作模式](#1-四种协作模式)
2. [消息总线架构](#2-消息总线架构)
3. [协作循环设计模式](#3-协作循环设计模式)
4. [协作中的关键挑战](#4-协作中的关键挑战)

---

## 1. 四种协作模式

### 1.1 顺序接力 (Sequential)

```text
Agent₁ → Agent₂ → Agent₃ → Final

每个 Agent 完成自己的环节，输出传递给下一个

场景：代码开发流水线
  Architect Agent → 输出架构设计
  Developer Agent → 基于设计写代码
  Reviewer Agent → 审查代码质量
  Tester Agent   → 生成并运行测试
```

### 1.2 辩论模式 (Debate)

```text
Agent_A: "观点 X，因为证据 1、2、3"
Agent_B: "反对！证据 2 有漏洞，因为..."
Agent_A: "你说得对，修正为..."

Judge: "综合双方观点，结论是..."

→ 经过 N 轮辩论 → 更严谨的结论
```

```python
class DebateLoop:
    def __init__(self, agents: list, judge, rounds=3):
        self.agents = agents   # 正方 + 反方
        self.judge = judge     # 裁判
        self.rounds = rounds

    def debate(self, question: str) -> str:
        stances = [None] * len(self.agents)

        for r in range(self.rounds):
            for i, agent in enumerate(self.agents):
                # 每个 Agent 看到其他 Agent 的观点后回应
                others = [s for j, s in enumerate(stances) if j != i and s]
                stance = agent.respond(question, others)
                stances[i] = stance

        return self.judge.summarize(question, stances)
```

### 1.3 投票模式 (Voting)

```text
5 个 Agent 独立回答同一个问题
→ 取多数票答案 / 加权投票

场景：事实性校验
  "2024 年诺贝尔物理学奖得主是谁？"
  Agent₁: Hopfield & Hinton
  Agent₂: Hopfield & Hinton
  Agent₃: Hassabis & Jumper  ← 错误
  Agent₄: Hopfield & Hinton
  Agent₅: Hopfield & Hinton

  → 投票：Hopfield & Hinton (4/5) → 高置信度 ✅
```

### 1.4 层级委派 (Hierarchical)

```text
         Orchestrator（总指挥）
        /        |        \
   Agent₁     Agent₂     Agent₃
   (搜索)     (分析)     (写作)
      ↓          ↓          ↓
         Orchestrator 综合输出
```

```text
设计原则：
  → Orchestrator 不干活，只分配和综合
  → 每个子 Agent 有独立的上下文和工具
  → 子 Agent 之间不直接通信（通过 Orchestrator）
```

## 2. 消息总线架构

```text
                   ┌──────────────┐
                   │ Message Bus  │  (共享内存 / Redis / MCP)
                   └──┬──┬──┬──┬──┘
                      │  │  │  │
        ┌─────────────┘  │  │  └─────────────┐
        ▼                ▼  ▼                ▼
   ┌─────────┐    ┌─────────┐    ┌─────────────┐
   │ Agent A │    │ Agent B │    │  Observer   │
   │ (搜索)  │    │ (写代码) │    │ (事实核查)   │
   └─────────┘    └─────────┘    └─────────────┘
```

```python
class MessageBus:
    def __init__(self):
        self.subscribers: dict[str, list[Callable]] = {}
        self.history: list[dict] = []

    def subscribe(self, agent_id: str, topic: str, handler):
        self.subscribers.setdefault(topic, []).append((agent_id, handler))

    def publish(self, topic: str, message: dict):
        self.history.append({"topic": topic, **message})
        for agent_id, handler in self.subscribers.get(topic, []):
            handler(message)

    def broadcast(self, message: dict):
        """发给所有 Agent"""
        for topic in self.subscribers:
            for _, handler in self.subscribers[topic]:
                handler(message)
```

## 3. 协作循环设计模式

### 3.1 Planner-Executor

```text
最经典的协作循环：

Planner:
  "任务：写一篇 AI 技术博客"
  → 子任务 1: 研究最新趋势 (Agent_Search)
  → 子任务 2: 撰写草稿 (Agent_Writer)
  → 子任务 3: 审核事实 (Agent_Checker)
  → 子任务 4: 润色发布 (Agent_Editor)

循环控制：
  Planner 逐个分配 → 等待完成 → 检查 → 下一子任务
  子任务失败 → Planner 重新规划
```

### 3.2 Map-Reduce

```text
Map 阶段（并行）：
  100 篇论文 → 100 个 Agent 各自总结 1 篇

Reduce 阶段（串行）：
  100 篇总结 → Aggregate Agent → 综合研究报告

循环控制：
  Map 用并行循环（所有 Agent 同时工作）
  Reduce 用串行（聚合 Agent 逐步综合）
```

### 3.3 循环自检

```text
Writer Agent: 写完草稿
Checker Agent: 检查 → 发现 3 个问题
Writer Agent: 修正
Checker Agent: 再检查 → 全部通过
Editor Agent: 润色 → 发布

→ "写-检查-改" 的循环直到通过
```

## 4. 协作中的关键挑战

| 挑战 | 表现 | 对策 |
|------|------|------|
| **上下文不一致** | Agent A 说的 Agent B 没听到 | 共享 Message Bus |
| **无限对话** | A 和 B 一直辩论停不下来 | max_debate_rounds=5 |
| **搭便车** | 某个 Agent 偷懒，输出质量差 | 每个 Agent 的答案单独评分 |
| **信息偏差** | 所有 Agent 用同一个 LLM → 相同偏见 | 用不同模型做不同角色 |
| **Token 爆炸** | N 个 Agent × M 轮对话 | 只传递摘要，不传全文 |

```text
多 Agent 协作的核心工程难题：

成本控制：
  单 Agent 调用 = N tokens
  M 个 Agent 协作 = N × M × rounds tokens
  → 不是简单的加法，是指数增长！

解决：
  1. 用不同等级的模型（大模型做决策，小模型执行）
  2. 子 Agent 只返回摘要
  3. 并行执行而非串行等待
```

## 核心要点回顾

- 四种协作：顺序接力、辩论、投票、层级委派
- 消息总线 = 多 Agent 通信基础设施
- Planner-Executor = 最经典的协作循环（一个规划，多个执行）
- Map-Reduce = 适合大量独立子任务的并行处理
- 核心挑战：Token 爆炸（M 个 Agent × N 轮 = 乘数效应）
- 用不同模型做不同角色可以缓解同质化偏见

## 参考资料

1. Multi-Agent Debate (Du et al., 2023)
2. MetaGPT / AutoGen 开源框架
3. CAMEL: Communicative Agents (Li et al., 2023)
