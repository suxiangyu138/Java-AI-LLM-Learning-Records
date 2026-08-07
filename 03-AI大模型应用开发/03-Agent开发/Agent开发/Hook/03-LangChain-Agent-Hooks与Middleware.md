# 03 - LangChain Agent Hooks 与 Middleware

> 🎯 LangChain 1.x 用 AgentMiddleware（Hooks）替代旧版回调机制 — 在模型调用和工具执行的前后插入拦截逻辑。与 Spring AI Advisor、Web 中间件同源：洋葱模型 + 顺序控制 + 状态共享

---

## 目录

1. [Middleware 核心概念](#1-middleware-核心概念)
2. [Hook 类型与执行时机](#2-hook-类型与执行时机)
3. [两种实现方式](#3-两种实现方式)
4. [执行顺序：洋葱模型](#4-执行顺序洋葱模型)
5. [内置 Middleware 清单](#5-内置-middleware-清单)
6. [状态管理与最佳实践](#6-状态管理与最佳实践)

---

## 1. Middleware 核心概念

LangChain 1.x 将 Agent Middleware 定义为**在 Agent 执行循环内的一层模块化拦截层** — 可在模型调用和工具执行前后插入自定义逻辑。

```python
# 通过 middleware 参数注入
from langchain.agents import create_agent

agent = create_agent(
    model="claude-sonnet-4-5",
    tools=[...],
    middleware=[
        SummarizationMiddleware(),
        HumanInTheLoopMiddleware(),
        MyCustomMiddleware()
    ]
)
```

> ⚠️ 这些 Hook 不是旧版的 Callbacks（事件回调）— 它们可以**修改状态、中断执行、控制流程**，而非仅仅是观察。

---

## 2. Hook 类型与执行时机

| Hook 类型 | Hook 名称 | 执行时机 | 典型用途 |
|-----------|-----------|----------|----------|
| **Node 式** | `before_agent` | Agent 启动前（每次调用一次） | 初始化、权限检查 |
| | `before_model` | 每次 LLM 调用前 | 上下文注入、Prompt 增强 |
| | `after_model` | 每次 LLM 响应后、工具执行前 | 安全护栏、人工审批 |
| | `after_agent` | Agent 完成后 | 日志、结果校验 |
| **Wrap 式** | `wrap_model_call` | 包裹每次 LLM 调用 | 重试、缓存、短路 |
| | `wrap_tool_call` | 包裹每次工具调用 | 重试、结果校验 |
| **请求修改** | `modify_model_request` | 模型调用前立即执行 | 修改**当前请求**（不改变永久状态） |

---

## 3. 两种实现方式

### 3.1 装饰器式（快速原型）

```python
from langchain.agents.middleware import before_model, after_model

@before_model
def inject_context(state, runtime):
    """在每次 LLM 调用前注入系统上下文"""
    return {"system_message": "你是一个友好的助手，回答简洁。"}

@after_model
def safety_guard(state, runtime):
    """检查 LLM 响应是否包含敏感内容"""
    last_msg = state.messages[-1]
    if "password" in last_msg.content.lower():
        return {"jump_to": "end"}  # 中断执行
```

### 3.2 类式（生产级，可组合复用）

```python
from langchain.agents.middleware import AgentMiddleware, hook_config

class RetryMiddleware(AgentMiddleware):
    def __init__(self, max_retries=3):
        self.max_retries = max_retries

    @hook_config
    def wrap_model_call(self, handler, request):
        """带指数退避的重试逻辑"""
        for attempt in range(self.max_retries):
            try:
                return handler(request)
            except Exception as e:
                if attempt == self.max_retries - 1:
                    raise
                time.sleep(2 ** attempt)
```

---

## 4. 执行顺序：洋葱模型

```text
用户输入
→ before_model(M1)  → before_model(M2)          # 正序进入
→ modify_request(M1) → modify_request(M2)
→ ★ LLM 调用 ★                                  # 模型被调用
→ after_model(M2)   → after_model(M1)           # 逆序返回
→ 继续（工具执行或结束）
```

| 规则 | 说明 |
|------|------|
| `before_*` | 按 Middleware **添加顺序**执行 |
| `after_*` | **逆序**执行（后添加的先看到响应） |
| `wrap_*` | 嵌套包裹实际调用（handler 可被调用 0/1/多次） |
| `modify_model_request` | 在 before_model 之后、LLM 调用之前执行，只修改当前请求 |

**与 Web 服务器中间件的范式完全一致** — 请求"穿入"before 链，模型被调用，响应"穿出"after 链。

---

## 5. 内置 Middleware 清单

| Middleware | 功能 |
|------------|------|
| `SummarizationMiddleware` | 上下文窗口管理：自动摘要历史消息，防止超限 |
| `HumanInTheLoopMiddleware` | 人工审批：LLM 响应需人工确认后才继续 |
| `ModelCallLimitMiddleware` | 成本控制：限制 LLM 调用次数 |
| `ToolCallLimitMiddleware` | 安全控制：限制工具调用次数（防死循环） |
| `ModelFallbackMiddleware` | 模型回退：主模型失败时切换备用模型 |
| `PIIMiddleware` | 敏感信息脱敏：自动检测并脱敏身份证号/手机号/邮箱等 |
| `TodoListMiddleware` | 任务规划：强制 Agent 先列计划再执行 |
| `ToolRetryMiddleware` | 工具调用失败时自动重试 |

---

## 6. 状态管理与最佳实践

### 6.1 AgentState

Middleware 操作 `AgentState` 对象 — 消息、工具选择、元数据、跳转标志：
- Node 式钩子返回 `dict` → 合并进 AgentState
- Wrap 式钩子通过 `ExtendedModelResponse` + `Command(update=...)` 注入状态更新

```python
@after_model
def route_to_human(state, runtime):
    if state.confidence < 0.7:
        return {
            "jump_to": "human_review",
            "pending_question": state.messages[-1].content
        }
    return None  # 继续正常流程
```

### 6.2 生产最佳实践

| 实践 | 说明 |
|------|------|
| **保持聚焦** | 一个 Middleware 只做一件事（安全/成本/日志分开） |
| **优雅降级** | 钩子异常不应崩溃 Agent（用 try-catch 兜底） |
| **注意顺序** | 安全钩子放最前面、日志放最后（先拦截再执行再记录） |
| **可观测性** | 钩子中记录耗时/Token/工具调用次数 |

---

> 🎯 **核心要点**：LangChain Middleware 的三个关键认知 — ① **洋葱模型**（before 正序 → LLM → after 逆序）② **wrap vs node**（包裹式可控调用次数，节点式单向执行）③ **状态驱动**（返回 dict 可直接改变 Agent 行为，包括 `jump_to` 中断）。生产 Agent 必配：Summarization + HumanInTheLoop + ModelCallLimit。

**下一模块**：[04-Spring AI Advisor拦截器机制](04-Spring%20AI%20Advisor拦截器机制.md) / **返回总览**：[00-Hook总览](00-Hook知识体系总览.md)
