# 05 - 工具调用与 MCP 集成

> **核心摘要**：工具是 Agent 与世界的接口——没有工具的 Agent 只会「说」，有工具的 Agent 才会「做」。本文拆解 Function Calling 原理、工具注册表设计、MCP 协议（v1→v2）、代码执行式工具与工具工程最佳实践。

> **前置阅读**：[[01-Agent核心概念与架构]]、[[03-Agent开发框架选型]]

---

## 📚 目录

1. [工具调用全景](#1-工具调用全景)
2. [Function Calling 原理](#2-function-calling-原理)
3. [工具注册表设计](#3-工具注册表设计)
4. [MCP：工具生态标准](#4-mcptool-生态标准)
5. [代码执行式工具](#5-代码执行式工具)
6. [工具工程最佳实践](#6-工具工程最佳实践)
7. [核心要点](#7-核心要点)

---

## 1. 工具调用全景

### 1.1 工具的分类

```text
Agent 工具分类
├── 查询类：读数据库/查 API/搜索（无副作用）
├── 写入类：写库/发消息/创建文件（有副作用）
├── 执行类：运行命令/执行代码（强副作用）
├── 感知类：网页浏览/文件读取/屏幕截图
└── 协作类：子 Agent 委派/人工确认
```

| 类别 | 副作用 | 权限要求 | 示例 |
|------|:---:|---------|------|
| 查询类 | 无 | 只读 | `query_order` |
| 写入类 | 有 | 写权限 + 审计 | `update_order` |
| 执行类 | 强 | 沙箱 + 白名单 | `run_code` |
| 感知类 | 无 | 访问范围 | `browse_url` |
| 协作类 | 中 | 审批 | `delegate` |

> ⚠️ **核心安全认知**：工具是 Agent 的「手脚」——权限过大等于把刀给 Agent 随便用。**每次工具调用都要问：这个 Agent 真的需要这个工具吗？**

---

## 2. Function Calling 原理

### 2.1 工作机制

**Function Calling（函数调用）**：模型输出**结构化调用指令**（而非执行本身），由程序解析并执行——模型「提议」，代码「执行」：

```text
Function Calling 流程
┌────────┐   ① 工具 schema 注册
│ 系统     │ ──────────────────→ 模型
│ (程序)   │
└────────┘
    ↑                    ↓ ② 模型输出结构化调用
    │                    {tool: "query_order", args: {order_id: "ORD-1"}}
    │ ④ 结果回传上下文
    └──────────────────── ③ 程序校验并执行工具
```

### 2.2 代码示例（OpenAI 风格）

```python
# 工具定义（schema）
tools = [{
    "type": "function",
    "function": {
        "name": "query_order",
        "description": "查询订单状态。用户询问订单时使用",
        "parameters": {
            "type": "object",
            "properties": {
                "order_id": {"type": "string", "description": "订单号"}
            },
            "required": ["order_id"],
        },
    },
}]

# 模型决策（输出调用指令而非执行）
response = client.chat.completions.create(
    model="gpt-4o",
    messages=[{"role": "user", "content": "查一下 ORD-1001"}],
    tools=tools,
)
# → response.tool_calls = [{name: "query_order", args: {order_id: "ORD-1001"}}]

# 程序执行（模型不直接执行！）
result = execute_tool(response.tool_calls)   # 你的代码执行
```

### 2.3 关键要点

| 要点 | 说明 |
|------|------|
| **描述质量决定调用率** | 工具 description 要写「何时用」（用户问 X 时） |
| **参数校验在程序侧** | 模型的参数可能错误，必须程序校验 |
| **异常返回给模型** | 工具报错要作为观察回传，让模型调整 |

> 🎯 **本质**：Function Calling = 模型负责「决策调用什么」，程序负责「执行并校验」——这是安全边界的关键设计。

---

## 3. 工具注册表设计

### 3.1 注册表结构

```python
# 工具注册表（白名单 + schema + 权限）
TOOL_REGISTRY = {
    "query_order": {
        "handler": query_order_impl,          # 执行函数
        "schema": {...},                       # 参数 schema
        "permission": "read",                  # 权限级别
        "requires_approval": False,            # 是否需审批
        "max_calls_per_task": 50,              # 调用上限
        "timeout": 10,                         # 超时
    },
    "delete_order": {
        "handler": delete_order_impl,
        "schema": {...},
        "permission": "write",
        "requires_approval": True,             # 高危操作审批
        "max_calls_per_task": 5,
        "timeout": 30,
    },
}

def execute_tool(call):
    """统一执行入口：校验 → 审批 → 执行 → 留痕"""
    tool = TOOL_REGISTRY[call["name"]]
    validate_args(call["args"], tool["schema"])      # 参数校验
    if tool["requires_approval"]:
        wait_human_approval(call)                     # 审批门
    log_tool_call(call)                               # 留痕
    return tool["handler"](**call["args"])            # 执行
```

### 3.2 注册表设计要点

| 设计 | 说明 |
|------|------|
| **统一入口** | 所有工具调用走同一执行函数（校验/审批/留痕） |
| **schema 校验** | 参数类型/必填/枚举校验（模型输出可能错） |
| **调用上限** | 每任务调用次数上限（防失控） |
| **超时控制** | 工具执行超时（防卡死） |
| **分级权限** | read/write/approval 三级 |

> 💡 **工具即产品**：每个工具都应获得公共 API 级待遇——清晰名称与描述、输入验证、结构化错误与契约。工具表面是 Agent 与世界的接口。

---

## 4. MCP：Tool 生态标准

### 4.1 什么是 MCP

**MCP（Model Context Protocol，模型上下文协议）**：连接 LLM/Agent 与外部工具、数据源的**行业标准协议**——2026 年已成 Agent 工具生态的事实标准。

```
MCP 架构
┌────────────┐      MCP 协议      ┌────────────┐
│ Agent/LLM   │ ←───────────────→ │ MCP 服务器  │
│ (Client)    │                   │ (Server)    │
│ 应用层      │   工具/资源/提示   │ 工具实现     │
└────────────┘                   └────────────┘
     ▲                                ▲
     │ 标准接口                        │ 可复用
     │                                │
  Agent 应用                     数据库/API/文件/外部系统
```

### 4.2 MCP v1.0 vs v2.0（2026）

| 能力 | v1.0 | v2.0 |
|------|:---:|:---:|
| 工具定义 | 简化 schema | **完整 JSON Schema** |
| 认证 | 基础 | **多因素认证 + 细粒度权限** |
| 上下文生命周期 | 无 | **完整管理** |
| 调用方式 | 同步串行 | **异步并发调用** |
| 多 Agent 支持 | 无 | **原生支持** |
| 多模态 | 无 | 实验性多模态工具调用 |

> 🎯 **MCP v2.0 的意义**：从「工具调用协议」升级为「Agent 生态协议」——认证、权限、并发、多 Agent 协作都成为协议级能力。

### 4.3 MCP 服务器开发骨架

```python
# 极简 MCP 服务器（Python）
from mcp.server import Server
from mcp.types import Tool

server = Server("order-tools")

@server.list_tools()
async def list_tools():
    return [Tool(
        name="query_order",
        description="查询订单状态（用户询问订单时）",
        inputSchema={
            "type": "object",
            "properties": {"order_id": {"type": "string"}},
            "required": ["order_id"],
        },
    )]

@server.call_tool()
async def call_tool(name: str, args: dict):
    if name == "query_order":
        return [{"type": "text", "text": str(query_order(args["order_id"]))}]
    raise ValueError(f"未知工具: {name}")
```

### 4.4 MCP 安全要点

```text
MCP 集成安全清单
├── ① 服务器认证：客户端/服务器双向认证
├── ② 权限声明：每个工具的最小权限
├── ③ 供应链：MCP 服务器源码审查（恶意服务器风险）
├── ④ 行为监测：异常调用模式告警
└── ⑤ 资产发现：登记 MCP 资产（AIBOM）
```

---

## 5. 代码执行式工具

### 5.1 写代码执行 vs 直接调用

> 💡 **2026 趋势**：Agent 写代码来执行操作（而非直接调用工具）——提升上下文效率 + 防止工具提示注入。

```text
两种工具执行方式对比
├── 直接调用：50 个工具描述全部塞进上下文
│   └── 缺点：上下文膨胀 + 工具描述可被注入
├── 代码执行：Agent 写 Python 代码，沙箱执行
│   ├── 优点：上下文只带代码执行工具（1 个）
│   ├── 优点：灵活组合（新操作无需注册新工具）
│   └── 要求：沙箱环境（代码可能危险！）
└── 2026 实践：数据分析/文件处理类任务优先代码执行
```

### 5.2 代码执行沙箱要求

```text
代码执行沙箱安全基线
├── 隔离：独立容器/VM，无宿主网络
├── 白名单：限制可用的库/API
├── 资源限制：CPU/内存/超时
├── 只读：默认无写权限，写入走审批
└── 审计：代码 + 执行结果留痕
```

---

## 6. 工具工程最佳实践

### 6.1 十项实践清单

| # | 实践 | 说明 |
|---|------|------|
| 1 | **描述即文档** | description 写「何时用 + 返回值格式」 |
| 2 | **schema 校验** | 模型输出参数必须程序校验 |
| 3 | **结构化错误** | 错误码 + 可读信息，回传模型继续尝试 |
| 4 | **幂等设计** | 重复调用无副作用（防重试造成重复操作） |
| 5 | **超时与上限** | 每个工具超时 + 每任务调用上限 |
| 6 | **分级权限** | read/write/approval 分级 |
| 7 | **调用留痕** | 每次调用记录（工具/参数/结果/耗时） |
| 8 | **统一入口** | 所有调用走同一执行函数 |
| 9 | **测试优先** | 工具与调用链有单元测试 |
| 10 | **MCP 标准** | 新工具优先按 MCP 协议实现（可复用） |

### 6.2 工具调用的观测

```text
工具调用观测字段
├── 工具名 + 参数（脱敏后）
├── 耗时与状态（成功/失败/超时）
├── 调用者（哪个 Agent/哪一步）
├── 结果摘要
└── 异常详情（失败时）
→ 观测数据 = 评估与排障的基础
```

---

## 7. 核心要点

> 🎯 **核心要点**：
> 1. 工具是 Agent 与世界的接口：模型「提议」、程序「执行」——校验与审批在程序侧
> 2. 工具注册表是安全核心：统一入口 + schema 校验 + 分级权限 + 调用上限 + 留痕
> 3. MCP v2.0 是生态标准：JSON Schema/多因素认证/异步并发/多 Agent 原生支持
> 4. 工具工程十项实践 + 代码执行式工具（沙箱）是 2026 年工具开发的主流形态

---

**下一模块**：[06-多Agent协作开发](06-多Agent协作开发.md) | **返回总览**：[00-Agent开发知识体系总览](00-Agent开发知识体系总览.md)
