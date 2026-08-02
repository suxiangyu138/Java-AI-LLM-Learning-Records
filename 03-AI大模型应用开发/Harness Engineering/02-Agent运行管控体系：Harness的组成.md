# 02 - Agent 运行管控体系：Harness 的组成

> **核心摘要**：Harness 不是单个组件，而是一套完整的运行管控体系。本文拆解 Harness 的七层架构与九大工程组件，并结合 Claude Code / Anthropic SDK / OpenAI Agents SDK / LangGraph 等主流实现给出对照，最后给出一个最小 Harness 的落地骨架。

> **前置阅读**：[[01-Harness Engineering核心概念与演进]]

---

## 📚 目录

1. [Harness 七层架构](#1-harness-七层架构)
2. [九大工程组件](#2-九大工程组件)
3. [主流实现的 Harness 对照](#3-主流实现的-harness-对照)
4. [Harness 生命周期](#4-harness-生命周期)
5. [最小 Harness 落地骨架](#5-最小-harness-落地骨架)
6. [Harness 演进路线](#6-harness-演进路线)
7. [核心要点](#7-核心要点)

---

## 1. Harness 七层架构

```
┌─────────────────────────────────────────────┐
│ ⑦ 反馈层：错误汇总、规则迭代                  │ ← 让 Harness 自己进化
├─────────────────────────────────────────────┤
│ ⑥ 约束层：高危拦截、人工审核、自动回滚         │ ← 守住最后底线
├─────────────────────────────────────────────┤
│ ⑤ 评估层：客观验收标准、任务完成判定           │ ← 杜绝自评自判
├─────────────────────────────────────────────┤
│ ④ 观测层：行为留痕、回放、溯源                 │ ← 出事了查得清
├─────────────────────────────────────────────┤
│ ③ 状态层：任务进度、操作日志、历史决策          │ ← 记住走到哪
├─────────────────────────────────────────────┤
│ ② 工具层：工具白名单、调用权限、参数校验        │ ← 能干什么
├─────────────────────────────────────────────┤
│ ① 指令层：任务目标、执行边界、优先级            │ ← 要干什么
└─────────────────────────────────────────────┘
```

### 1.1 各层职责与关键设计

| 层 | 职责 | 关键设计问题 | 典型实现 |
|---|------|------------|---------|
| ① 指令层 | 定义任务与边界 | 目标是否可验证？边界是否明确？ | 系统提示词 + 任务契约 |
| ② 工具层 | 管控能力范围 | 工具白名单？参数 schema 校验？ | MCP 服务器、函数注册表 |
| ③ 状态层 | 维护执行状态 | 状态外部化？可恢复？ | Redis/数据库会话存储 |
| ④ 观测层 | 记录行为轨迹 | 每次调用/修改/决策是否留痕？ | 追踪系统（trace_id） |
| ⑤ 评估层 | 判定任务完成 | 验收标准是否客观？能否自动执行？ | AI Evals、测试断言 |
| ⑥ 约束层 | 拦截高危操作 | 哪些操作需人工审批？失败如何回滚？ | 权限策略、审批门、快照 |
| ⑦ 反馈层 | 迭代改进规则 | 失败模式能否回灌规则库？ | 错误日志 → 规则更新 |

> 🎯 **七层的核心逻辑**：上面四层（指令/工具/状态）让 Agent「做对事」，下面三层（观测/评估/约束）让系统「管得住」，反馈层让体系「能进化」。

---

## 2. 九大工程组件

跨 Claude Code、Anthropic SDK、OpenAI Agents SDK、LangGraph 等实现提炼的九大组件清单：

| # | 组件 | 作用 | 核心考量 |
|---|------|------|---------|
| 1 | **While-loop 引擎** | 驱动 Agent 循环（思考→行动→观察） | 循环上限、终止条件、超时干预 |
| 2 | **上下文管理** | 注入/裁剪/压缩上下文 | 防上下文腐烂、成本控制 |
| 3 | **工具注册表** | 注册可用工具及 schema | 白名单、参数校验、冲突处理 |
| 4 | **子智能体管理** | 委派子任务、回收结果 | 权限隔离、结果验证 |
| 5 | **内置技能（Skills）** | 沉淀可复用能力包 | 版本管理、权限边界 |
| 6 | **会话持久化** | 外部化状态、断点恢复 | 序列化、跨会话连续性 |
| 7 | **动态提示组装** | 按任务拼装提示模板 | 与上下文管理协同 |
| 8 | **生命周期钩子** | 在关键节点注入自定义逻辑 | 钩子事件：开始/结束/失败/审批 |
| 9 | **权限执行** | 工具/文件/网络访问控制 | 最小权限、审批流、审计 |

### 2.1 九大组件的依赖关系

```text
        ⑦ 动态提示组装
            │
① While-loop 引擎（核心循环）
   ├── ③ 工具注册表 ←── ② 上下文管理
   ├── ④ 子智能体管理
   ├── ⑤ 内置技能
   ├── ⑥ 会话持久化
   └── ⑨ 权限执行 ←── ⑧ 生命周期钩子
```

> 💡 自检：你的 Agent 项目缺了哪几件？**大多数失败项目都缺 ⑥⑧⑨**——没有持久化（崩溃全丢）、没有钩子（无法插入审批）、没有权限执行（工具全开）。

---

## 3. 主流实现的 Harness 对照

| 组件 | Claude Code | Anthropic Agent SDK | OpenAI Agents SDK | LangGraph |
|------|:---:|:---:|:---:|:---:|
| While-loop 引擎 | ✅ 内置 | ✅ `while_loop` | ✅ Runner | ✅ 图节点 |
| 上下文管理 | ✅ 自动 + CLAUDE.md | ✅ 手动注入 | ✅ 手动 | ✅ 状态传播 |
| 工具注册表 | ✅ MCP/工具 | ✅ 函数 | ✅ function tools | ✅ 节点工具 |
| 子智能体管理 | ✅ Task/Agent 工具 | ✅ 子 Agent | ✅ Handoffs | ✅ 子图 |
| 内置技能 | ✅ Agent Skills | 部分 | ❌ | ❌ |
| 会话持久化 | ✅ 续聊 | ⚠️ 手动 | ⚠️ 手动 | ✅ Checkpointer |
| 动态提示组装 | ✅ 系统提示 + 记忆 | ✅ 手动 | ✅ 手动 | ✅ 节点 |
| 生命周期钩子 | ✅ Hooks | ⚠️ 手动 | ⚠️ 中间件 | ✅ 节点回调 |
| 权限执行 | ✅ 权限模式 | ⚠️ 自行实现 | ⚠️ 自行实现 | ⚠️ 自行实现 |

> 🎯 结论：**Claude Code 是目前九组件最完整的生产级 Harness 参考实现**；开源框架（LangGraph 等）提供底座，权限执行、治理等组件需要团队自建——这正是 Harness 工程团队存在的意义。

---

## 4. Harness 生命周期

### 4.1 生命周期阶段

```text
Agent DLC（Agent 开发生命周期）
┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐
│ 构建     │→│ 测试     │→│ 存储     │→│ 部署     │→│ 运行     │→│ 治理     │
│ Build   │ │ Test    │ │ Store   │ │ Deploy  │ │ Run     │ │ Govern  │
└─────────┘  └─────────┘  └─────────┘  └─────────┘  └─────────┘  └─────────┘
  版本管理      Evals      资产目录      灰度发布     观测+约束    审计+回收
  CI 构建     红队测试     注册登记     审批门      告警+回滚    影子Agent发现
```

| 阶段 | 关键动作 | Harness 组件 |
|------|---------|-------------|
| **构建** | 版本管理、CI 构建、提示词即代码 | 动态提示组装、技能 |
| **测试** | AI Evals、红队、回归 | 评估层 |
| **存储** | AI 资产目录登记、AIBOM | 资产目录 |
| **部署** | 灰度、审批门、回滚预案 | 约束层 |
| **运行** | 观测、告警、权限执行、审计追踪 | 观测层 + 约束层 |
| **治理** | 影子 Agent 发现、回收、合规审计 | 治理体系 |

### 4.2 影子 Agent 问题

> ⚠️ **影子 Agent（Shadow Agents）**：未经 IT 治理自行上线的 Agent——IDC 调研显示约 **64%** 的企业已在生产环境发现未授权智能体运行于关键业务流程。

```text
影子 Agent 防控三招
├── ① 发现：AI 资产目录自动扫描/注册，识别未知 Agent
├── ② 登记：所有者绑定 + 标准评分卡检查
└── ③ 治理：未登记 Agent 默认拒绝高权限操作
```

---

## 5. 最小 Harness 落地骨架

一个可运行的 Harness 骨架（以 Python + LangGraph 为例，覆盖关键组件）：

```python
# minimal_harness.py — 最小 Harness 骨架
import json
from langgraph.graph import StateGraph, END

class Harness:
    """最小运行管控体系：权限执行 + 观测 + 评估 + 约束"""

    def __init__(self, model, tools, policy):
        self.model = model            # 模型
        self.tools = self._whitelist(tools, policy.allowed_tools)  # 工具白名单
        self.policy = policy          # 策略：审批规则/高危操作/预算
        self.trace = []               # 观测层：行为留痕

    def _whitelist(self, tools, allowed):
        return {name: t for name, t in tools.items() if name in allowed}

    def run(self, task: str) -> dict:
        """while-loop 引擎：带最大循环次数与终止条件"""
        state = {"task": task, "done": False, "history": []}
        for step in range(self.policy.max_steps):          # 循环上限
            action = self.model.decide(state)              # 模型决策
            self._check_permission(action)                 # 权限执行（约束层）
            self._check_approval(action)                   # 审批门（约束层）
            result = self._execute(action)                 # 执行工具
            self.trace.append({"step": step, "action": action, "result": result})
            state["history"].append(result)
            if self.evaluate(state):                        # 评估层：客观验收
                state["done"] = True
                break
        else:
            raise HarnessTimeout(f"超过最大步数 {self.policy.max_steps}")
        return {"result": state, "trace": self.trace}       # 可审计输出

    def evaluate(self, state) -> bool:
        """评估层：用独立验收标准判定完成，而非模型自评"""
        return self.policy.verifier(state)

    def _check_permission(self, action):
        if action.tool not in self.policy.allowed_tools:
            raise PermissionError(f"工具 {action.tool} 未在白名单中")
        for arg in action.args:
            if arg.get("sensitive", False) and not self.policy.human_approved(action):
                raise ApprovalRequired(action)

    def _execute(self, action):
        # 观测层：每次执行前先留痕，失败时可回放
        self.trace.append(("pre-execute", action.tool, json.dumps(action.args)))
        return self.tools[action.tool](**action.args)
```

> 💡 骨架要点：**循环上限（防死循环）、工具白名单（防越权）、审批门（防高危）、客观评估（防自评）、完整 trace（防不可审计）**——这就是 Harness 的全部灵魂。

---

## 6. Harness 演进路线

```text
Harness 成熟度四级
├── L1 实验级：模型 + 裸工具调用，无权限无观测（Demo 阶段）
├── L2 工程级：+ 工具白名单 + 基础日志 + 循环上限（可内部使用）
├── L3 生产级：+ AI Evals + 审批门 + 审计追踪 + 回滚（可上线）
└── L4 治理级：+ 资产目录 + AIBOM + 红队常态化 + 策略即代码（可规模化）
```

| 成熟度 | 核心标志 | 企业适配阶段 |
|--------|---------|-------------|
| L1 | 能跑 | POC 验证 |
| L2 | 可控 | 内部试点 |
| L3 | 可信 | 单业务线生产 |
| L4 | 可规模化 | 全企业推广 |

> 🎯 演进原则：**不要一步到位 L4**——从 L2 起步，用真实业务数据驱动升级；跳级前进是 Harness 项目最常见的失败模式。

---

## 7. 核心要点

> 🎯 **核心要点**：
> 1. Harness 七层架构：指令/工具/状态（做对事）+ 观测/评估/约束（管得住）+ 反馈（能进化）
> 2. 九大组件缺一不可：大多数失败项目缺会话持久化、生命周期钩子、权限执行
> 3. Claude Code 是九组件最完整的生产级参考实现；开源框架需自建权限与治理
> 4. Agent DLC 全生命周期治理 + 影子 Agent 防控（64% 企业中招）；成熟度四级演进，L2 起步、数据驱动升级

---

**下一模块**：[03-Harness安全纵深防御](03-Harness安全纵深防御.md) | **返回总览**：[00-Harness Engineering知识体系总览](00-Harness%20Engineering知识体系总览.md)
