# 05 多 Agent 协作模式

> 编排模式是 AutoGen 的灵魂：轮流/选择/分层三种 GroupChat 模式、MagenticOne 参考团队、人类审批介入——模式选对，协作质量翻倍。

## 📚 目录

1. [协作模式总览](#1-协作模式总览)
2. [轮流模式（RoundRobin）](#2-轮流模式roundrobin)
3. [选择模式（Selector）](#3-选择模式selector)
4. [分层协作](#4-分层协作)
5. [MagenticOne 参考团队](#5-magenticone-参考团队)
6. [人类审批介入](#6-人类审批介入)
7. [模式选型与面试](#7-模式选型与面试)
8. [协作模式的常见问题](#8-协作模式的常见问题)
9. [角色设计原则](#9-角色设计原则)

## 1. 协作模式总览

| 模式 | 机制 | 适用 |
|---|---|---|
| 轮流（RoundRobin） | 按顺序轮流发言 | 简单、可预测 |
| 选择（Selector） | Manager 选下一个发言者 | 灵活、角色分工明确 |
| 分层（Hierarchical） | 主管-下属结构 | 复杂任务拆解 |
| MagenticOne | 微软参考团队（编排+规划+验证） | 生产级多 Agent |

### 模式选择的核心问题

```
① 流程需要确定性吗？（轮流 > 选择）
② 角色之间有主次吗？（分层）
③ 需要验证与纠错吗？（MagenticOne）
```

## 2. 轮流模式（RoundRobin）

### 机制

```
Agent 列表按顺序轮流发言：
A → B → C → A → B → C ...
直到终止条件
```

### 代码

```python
from autogen_agentchat.teams import RoundRobinGroupChat

team = RoundRobinGroupChat(
    [planner, executor, reviewer],   # 三个角色
    max_turns=9,                     # 兜底
)
```

### 适用与局限

| 适用 | 局限 |
|---|---|
| 流水线式协作（规划→实现→评审） | 无法跳过（A 无事也要发言） |
| 简单确定性流程 | 发言顺序固定，不够灵活 |
| 学习/演示 | 大型复杂任务力不从心 |

## 3. 选择模式（Selector）

### 机制

```
SelectorGroupChat 的 Manager 用 LLM 决定"下一个谁发言"
→ 基于群聊历史选择最合适的 Agent
```

### 代码

```python
from autogen_agentchat.teams import SelectorGroupChat

team = SelectorGroupChat(
    [pm, engineer, tester],
    model_client=model,        # Manager 也用 LLM
    max_turns=12,
)
```

### 适用与局限

| 适用 | 局限 |
|---|---|
| 角色分工明确（谁该说话很自然） | 决策有 LLM 不确定性 |
| 复杂对话（需要"智能"地切换发言者） | 成本更高（Manager 也要 LLM 调用） |
| 任务路径不固定 | 可能来回扯皮（终止条件更要兜底） |

### 选择 vs 轮流（关键对比）

```
轮流 = 固定发言顺序（确定性、低成本、灵活性差）
选择 = LLM 决定发言者（灵活性高、成本高、不可预测）

选型：流水线流程用轮流，角色协商用选择
```

## 4. 分层协作

### 机制

```
主管 Agent 拆解任务 → 派发给下属 Agent
下属各自执行 → 结果汇总回主管
（对应 MAF 的层级工作流思想）
```

### 结构

```
ManagerAgent（主管）
  ├─ Agent A（下属 1：检索）
  ├─ Agent B（下属 2：分析）
  └─ Agent C（下属 3：写作）
```

### 实现思路

```python
# v0.4 中分层通常用 Swarm/MagenticOne 或自定义编排
# 关键：主管 Agent 的工具列表 = 调用下属的能力
manager = AssistantAgent(
    name="Manager",
    model_client=model,
    tools=[delegate_to_a, delegate_to_b],   # 派发工具
    system_message="你是主管：拆解任务并派发给下属，汇总结果。",
)
```

### 分层 vs 群聊

| 维度 | 群聊（平级） | 分层（主从） |
|---|---|---|
| 关系 | 平等讨论 | 主管分配 |
| 任务流 | 协商推进 | 拆解派发 |
| 控制 | 弱 | 强 |
| 适用 | 创意/评审 | 明确任务分解 |

## 5. MagenticOne 参考团队

### 是什么

微软提供的一组**开箱即用的多 Agent 团队**（参考实现），包含：

| 成员 | 角色 |
|---|---|
| Orchestrator | 编排：跟踪任务进度，决定下一步 |
| WebSurfer | 浏览网页/搜索 |
| FileSurfer | 文件系统操作 |
| Coder | 写代码 |
| ComputerTerminal | 执行终端命令 |
| UserProxy | 人类代理/审批 |

### 特点

```
① 开箱即用：一组 Agent 直接组装
② 编排内置：Orchestrator 管理进度
③ 生产参考：微软官方的"最佳实践模板"
④ MAF 中继续演进（MagenticOneGroupChat 是 GroupChat 替代）
```

### 适用

```
复杂任务（网页调研 + 代码 + 文件处理组合）
需要"团队"而非"几个 Agent"的场景
作为自定义团队的参考模板
```

## 6. 人类审批介入

### 介入时机

| 时机 | 方式 | 例子 |
|---|---|---|
| 关键决策 | UserProxy 请求输入 | "是否批准这笔订单？" |
| 高危操作 | 审批钩子 | 删除/写库前确认 |
| 代码执行 | 执行确认 | Docker 沙箱外执行前确认 |
| 任务偏离 | 手动打断 | 重定向任务方向 |

### 实现（UserProxy 请求输入）

```python
from autogen_agentchat.agents import UserProxyAgent

user_proxy = UserProxyAgent(
    name="UserProxy",
    input_func=lambda prompt: input(f"[需要您确认] {prompt}\n> "),
)

# 在团队中加入 user_proxy → 关键节点自动请求人工输入
```

### 审批设计原则

```
审批是"防线"不是"流程"：
高频低危操作自动执行（否则烦死人）
低频高危操作强制审批（这是安全底线）
```

## 7. 模式选型与面试

### 模式选型决策树

```
任务特征？
├─ 流水线式（规划→实现→评审）→ RoundRobin
├─ 角色协商式（谁说话不固定）→ SelectorGroupChat
├─ 任务分解式（主管派活）→ 分层 / MagenticOne
├─ 需要验证纠错 → MagenticOne（Orchestrator + 内置验证）
└─ 需要人类介入 → 任意模式 + UserProxyAgent
```

### 面试高频问法

| 问题 | 回答要点 |
|---|---|
| 三种群聊模式？ | 轮流/选择/分层——确定性 vs 灵活性 |
| RoundRobin vs Selector？ | 固定顺序 vs LLM 选发言者 |
| MagenticOne 是什么？ | 微软参考团队（Orchestrator/WebSurfer/Coder 等） |
| 人类怎么介入？ | UserProxyAgent 请求输入/审批钩子 |
| 多 Agent 的代价？ | token 多轮爆炸、不确定性、调试难——终止兜底必配 |

### 面试加分表达

> "模式选型看任务特征：流水线用轮流（确定、便宜）、角色协商用选择（灵活、贵）、任务分解用分层或 MagenticOne（主管派活）。所有模式都必须配 max_turns 和成本兜底——多 Agent 最怕的是'聊起来没完'。"

## 8. 协作模式的常见问题

| 问题 | 现象 | 对策 |
|---|---|---|
| 扯皮循环 | 两个 Agent 互相"你说得对但我认为..." | 加 reviewer 定论 / 终止条件收紧 |
| 角色重叠 | 多个 Agent 抢同一职责 | 系统提示明确边界（各管一段） |
| 信息丢失 | 长对话后 Agent 忘了前提 | 摘要/笔记 Agent（记忆管理） |
| 沉默 Agent | 某 Agent 从不发言 | 检查提示词/顺序（轮流模式必发言） |
| 成本失控 | 轮次多 token 爆炸 | 预算上限 + 简单问题直答 |
| 结果难验证 | 无人检查输出质量 | 加评审 Agent / 工具校验 |

### 问题定位思路

```
① 先看消息流：谁没说话/谁在重复（Studio 可视化）
② 再查角色：边界是否清晰（系统提示）
③ 后查流程：模式是否匹配任务（选型决策树）
④ 终查兜底：终止条件与预算是否生效
```

## 9. 角色设计原则

### 角色 = 系统提示 + 工具 + 权限

```
一个角色的完整定义：
角色（name + system_message 职责）
+ 工具（该角色能调什么）
+ 权限（审批/执行边界）
```

### 角色设计清单

| 原则 | 说明 |
|---|---|
| 职责单一 | 一个角色一件事（规划/实现/评审分离） |
| 边界清晰 | 提示词写清"你负责什么，不负责什么" |
| 工具匹配 | 只给角色需要的工具（最小权限） |
| 协作接口 | 写清"你产出什么给下一个角色" |
| 数量克制 | 3-5 个角色通常是甜区（再多成本爆炸） |

### 经典角色模板

```python
roles = [
    ("Planner", "拆解任务为步骤，输出步骤清单，不写代码"),
    ("Coder", "按步骤实现代码，调用工具验证"),
    ("Reviewer", "审查代码正确性/边界，有问题返回 Coder"),
]
# 三个角色 + 轮流模式 = 最小可用流水线
```

> 🎯 核心要点：四种协作模式（轮流/选择/分层/MagenticOne）——确定性与灵活性是选型主线；MagenticOne 是微软开箱即用团队；人类介入用 UserProxyAgent（审批是防线不是流程）；多 Agent 代价（token/不确定性）用终止兜底与预算控制对冲；角色设计五原则（职责单一/边界清晰/工具匹配/协作接口/数量克制），问题定位按"消息流→角色→流程→兜底"四步。

---

**下一模块**：[06-AutoGen-Studio与低代码开发](06-AutoGen-Studio与低代码开发.md) / **返回总览**：[00-AutoGen知识体系总览](00-AutoGen知识体系总览.md)
