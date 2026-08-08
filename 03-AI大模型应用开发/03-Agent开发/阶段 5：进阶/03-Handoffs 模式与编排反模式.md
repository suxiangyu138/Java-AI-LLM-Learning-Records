# 03 Handoffs 模式与编排反模式

> 定位：无中央瓶颈的平级协作——Swarm 交接实现、上下文保留、循环预防；以及"过度监督"等四大反模式（2026-08 基准）

## 📚 目录

1. [Handoffs 模式本质](#1-handoffs-模式本质)
2. [实现：交接工具与 Command 转移](#2-实现交接工具与-command-转移)
3. [上下文保留策略](#3-上下文保留策略)
4. [循环预防](#4-循环预防)
5. [四大编排反模式](#5-四大编排反模式)
6. [多 Agent 通用最佳实践](#6-多-agent-通用最佳实践)

## 1. Handoffs 模式本质

Handoffs（Swarm 风格）= **Agent 之间平级转移控制权**：

```text
典型流程：请求 → Researcher → Writer → Editor → FINISH
                 ↑___________↑__________↑
                 每个 Agent 完成自己的部分后，把控制权交给下一个
```

| 对比 Supervisor | Handoffs |
|----------------|---------|
| 控制点 | 中央监督者 | 无中央（谁接手谁决定） |
| 路由决策 | 监督者统一 | 当前 Agent 自行决定 |
| 适用 | 结构化分工 | 开放式对话、动态路径 |
| 瓶颈 | 有（监督者是单点） | 无 |

> 🎯 **核心要点**：Supervisor 是"老板派活"，Handoffs 是"同事交接"。交接策略三选一：**显式**（声明下一个 Agent）/ **条件**（按完成标准）/ **循环**（交回修订）。

## 2. 实现：交接工具与 Command 转移

```python
"""handoffs.py — 交接模式实现（图节点 + 转移工具）"""
from langgraph.graph import StateGraph, START, END
from langgraph.types import Command
from typing import TypedDict, Annotated, operator


class HandoffState(TypedDict):
    messages: Annotated[list, operator.add]
    context: dict                      # 交接时携带的共享上下文
    next_agent: str                    # 下一个执行者


# 交接工具：Agent 通过"调工具"实现转交
def handoff_to(target: str):
    """生成一个交接工具：Agent 调用即转交"""
    def _handoff(context: dict) -> Command:
        return Command(
            update={"next_agent": target, "context": context},
            goto=target,               # ★ 转移控制权
        )
    _handoff.__name__ = f"handoff_to_{target}"
    _handoff.__doc__ = f"将任务移交给 {target}。当本阶段完成且需要{target}处理时调用。"
    return _handoff


def researcher_node(state):
    # ... 研究逻辑 ...
    return Command(update={"messages": [research_result]},
                   goto="writer")      # 或调用交接工具


def writer_node(state):
    # ... 写作逻辑 ...
    return Command(update={"messages": [draft]}, goto="editor")


g = StateGraph(HandoffState)
g.add_node("researcher", researcher_node)
g.add_node("writer", writer_node)
g.add_node("editor", editor_node)
g.add_edge(START, "researcher")
g.add_edge("editor", END)
```

| 交接实现要点 | 说明 |
|------------|------|
| Command goto | 状态更新 + 转移一体 |
| 交接工具 | Agent 可"主动"交（动态决策） |
| 共享上下文 | context 字段随交接传递（见第 3 节） |

## 3. 上下文保留策略

交接的**最大风险**：上下文断链（下一个 Agent 不知道前面发生了什么）。

| 策略 | 做法 | 适用 |
|------|------|------|
| 全量传递 | context 带完整消息 | 短链、小上下文 |
| 摘要传递 | 前 Agent 输出结构化摘要 | 长链（防膨胀） |
| 精选传递 | 只传关键字段（目标/产出/约束） | 生产推荐 |

```python
def pass_context(prev_state, essentials: dict) -> dict:
    """精选传递：只传下个 Agent 需要的关键信息"""
    return {
        "original_request": essentials["original_request"],   # 原始目标
        "intermediate_output": essentials["output"],          # 本阶段产出
        "constraints": essentials["constraints"],             # 全程约束
        "audit": prev_state["messages"][-5:],                 # 最近 5 条审计留痕
    }
```

> 💡 与阶段 4 上下文工程的联动：交接的"精选传递" = 上下文管理的"外部记忆"思想在多 Agent 的体现——**传必要，不传全部**。

## 4. 循环预防

多 Agent 的**循环灾难**：A 交 B、B 交 A、无限转圈。

```python
"""loop_guard.py — 多 Agent 循环预防"""
class LoopGuard:
    def __init__(self, max_handoffs=8):
        self.max_handoffs = max_handoffs
        self.handoff_history = []

    def check(self, target: str) -> tuple[bool, str]:
        # ① 总量上限
        if len(self.handoff_history) >= self.max_handoffs:
            return False, "交接次数超限"
        # ② 相同路径检测（A→B→A→B 模式）
        recent = self.handoff_history[-4:]
        if len(recent) == 4 and recent[0] == recent[2] and recent[1] == recent[3]:
            return False, "检测到 A→B→A→B 循环模式"
        # ③ 单 Agent 重复出现上限（防 A→A→A）
        if len([h for h in self.handoff_history[-3:] if h == target]) >= 3:
            return False, f"{target} 连续被调用过多次"
        return True, ""

    def record(self, target: str):
        self.handoff_history.append(target)

# 用法：每次 goto 前 check()，拒绝则转 END 或人工
```

| 预防层 | 机制 |
|--------|------|
| 总量上限 | 8 次（与轮数熔断联动） |
| 模式检测 | A→B→A→B 交替循环 |
| 单点过热 | 同一 Agent 连续多次 |
| 兜底 | 拒绝时转 END + 人类提示 |

> ⚠️ **多 Agent 的熔断要乘系数**：单 Agent 25 轮 × 5 个 Agent ≠ 125 轮预算——**整树共享一个预算**（阶段 4 熔断 + 01 篇成本陷阱）。

## 5. 四大编排反模式

| # | 反模式 | 表现 | 正确姿势 |
|:---:|--------|------|---------|
| 1 | **过度监督** | 线性流也套 Supervisor：User→S→A1→S→A2→S | 用 Handoffs 直连：User→A1→A2→FINISH |
| 2 | 复杂路由器 | 手写海量条件路由代码 | LLM 监督者分析并路由（让模型当路由器） |
| 3 | 无管理状态增长 | messages 无限累积 | 摘要/限制/精选传递 |
| 4 | 职责重叠 | 两个 Agent 都能干同一件事 | 职责不重叠 + 文档化边界 |

```text
过度监督识别法：如果任务只是"顺序做 2-3 件事"，没有分支决策——
              加监督者 = 多付一轮模型调用 + 多一个故障点
```

## 6. 多 Agent 通用最佳实践

| 实践 | 说明 |
|------|------|
| 职责清晰 | 不重叠能力 + 文档化每个 Agent 的目的 |
| 循环预防 | 迭代计数进状态 + 上限 + 模式检测（第 4 节） |
| 上下文纪律 | 大了就摘要、只传必要信息、结构化 context |
| 路由容错 | 无效路由优雅处理 + 默认安全回退 |
| 检查点 | compile(checkpointer=...) 支持恢复与 HITL |
| 可观测 | 记录每次路由决策（谁 → 谁 → 为什么） |
| 评估 | 路由准确率是独立评估项（阶段 4 五模式） |

> 🎯 **核心要点**：Handoffs 是"无中心的信任链"——每个 Agent 都信任上一个的交接。因此**上下文保留、循环预防、路由审计**三件事必须代码化。反模式记忆法：**不过度监督、不让模型写路由器、不让状态无限长、不让职责重叠**。

---

**返回总览**：[00-阶段总览：进阶](00-阶段总览：进阶.md) / **上一模块**：[02-Supervisor 模式工程](02-Supervisor%20模式工程.md) / **下一模块**：[04-多 Agent 状态与上下文工程](04-多%20Agent%20状态与上下文工程.md)
