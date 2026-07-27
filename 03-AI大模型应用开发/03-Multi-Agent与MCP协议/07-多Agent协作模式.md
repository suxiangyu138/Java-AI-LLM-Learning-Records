# 07 - 多 Agent 协作模式

> 🎯 单 Agent 有天花板 — 多 Agent 分工协作是解决复杂任务的必经之路。顺序、层级、辩论、群集四种模式覆盖全场景

---

## 目录

1. [为什么需要多 Agent](#1-为什么需要多-agent)
2. [四种协作模式](#2-四种协作模式)
3. [Agent 间通信](#3-agent-间通信)
4. [多 Agent 框架实践](#4-多-agent-框架实践)

---

## 1. 为什么需要多 Agent

```text
单 Agent 的局限：
  ❌ 上下文窗口有限（一个 Agent 装不下所有领域的专业知识）
  ❌ 单点故障（一个 Agent 出错 → 整个任务失败）
  ❌ 缺乏多视角（没有第二意见来审查）

多 Agent 的优势：
  ✅ 分工协作（各司其职 → 每个 Agent 更专业）
  ✅ 并行执行（多个 Agent 同时工作 → 更快）
  ✅ 交叉验证（一个 Agent 的输出由另一个审查）
  ✅ 鲁棒性（一个失败不影响全局）
```

---

## 2. 四种协作模式

### 2.1 模式对比

| 模式 | 结构 | 决策方式 | 适用场景 |
|------|------|----------|----------|
| **顺序流水线** | A→B→C | 固定顺序 | 有明确先后阶段 |
| **层级管理** | M→A,B,C | 管理者分配 | 复杂项目 |
| **辩论模式** | A vs B → Judge | 辩论+评判 | 需要多角度 |
| **群集自组织** | 无中心 | 自主协商 | 开放探索 |

### 2.2 顺序流水线

```text
示例：合同审查流水线

  合同 → Agent A(合规检查) → Agent B(风险分析) → Agent C(生成报告)

每个 Agent 专注自己的环节，输出是下一个的输入
```

### 2.3 层级管理

```python
class ManagerAgent:
    def delegate(self, task):
        # ① 分析任务 → 分解
        subtasks = self.planner.decompose(task)
        
        # ② 分配给专家 Agent
        results = {}
        for subtask in subtasks:
            expert = self.select_expert(subtask.type)
            results[subtask.id] = expert.execute(subtask)
        
        # ③ 汇总 → 审查
        return self.synthesize(results)

# 使用
manager = ManagerAgent()
manager.register_expert("code", CoderAgent())
manager.register_expert("review", ReviewerAgent())
manager.register_expert("test", TesterAgent())

result = manager.delegate("开发一个用户登录功能")
```

### 2.4 辩论模式

```text
适用：需要多角度分析 → 方案评估、风险分析

  问题："用 Redis 还是 Kafka 做消息队列？"
  
  Agent A：为 Redis 辩护 → 低延迟、简单、数据结构丰富
  Agent B：为 Kafka 辩护 → 持久化、高吞吐、可回溯
  
  Judge Agent：综合评估 → Redis 适合轻量场景，Kafka 适合数据管道
```

### 2.5 群集模式（AutoGen / CrewAI）

```python
# CrewAI 多 Agent 群集示例
from crewai import Agent, Task, Crew

# 定义专家 Agent
researcher = Agent(role="研究员", goal="深入调研技术方案")
analyst = Agent(role="分析师", goal="分析数据和趋势")
writer = Agent(role="撰稿人", goal="撰写专业报告")

# 定义任务
research_task = Task(description="调研 2026 年 Java 后端 AI 框架")
analysis_task = Task(description="分析各框架优劣")
write_task = Task(description="撰写技术选型报告")

# 组建团队
crew = Crew(agents=[researcher, analyst, writer],
            tasks=[research_task, analysis_task, write_task])
result = crew.kickoff()
```

---

## 3. Agent 间通信

```text
通信方式：

  ① 消息传递：Agent A → 结构化消息 → Agent B
  ② 共享黑板：所有 Agent 读写共享的"公告板"
  ③ 事件驱动：Agent A 发布事件 → 订阅者 Agent B 响应
  ④ 共享记忆：Agent 间通过共享的长期记忆交换信息

  推荐：消息传递（精确）+ 共享记忆（灵活）
```

---

## 4. 多 Agent 框架实践

| 框架 | 特点 | 适用 |
|------|------|------|
| **LangGraph** | 状态图、可控流程 | 自定义多 Agent 工作流 |
| **AutoGen (Microsoft)** | 对话式多 Agent | 研究/探索 |
| **CrewAI** | 角色化 Agent 团队 | 快速搭建 |
| **MetaGPT** | 软件公司 SOP 模拟 | 软件开发全流程 |

---

## 核心要点回顾

- 四种模式：流水线、层级、辩论、群集
- 层级模式最适合企业级复杂任务
- 多 Agent 通信 = 消息传递（精确）+ 共享记忆（灵活）
- LangGraph 最适合自定义多 Agent 流程
