# 04 - Tools 工具与多智能体

> 🎯 GPT-5.6 的工具生态全景 — 10 种内置工具 + 编程式工具调用（新）+ 多智能体执行（Beta）。Responses API 让工具集成从"手动循环"变为"内置能力"

---

## 目录

1. [工具全景：10 种内置工具](#1-工具全景10-种内置工具)
2. [Web Search 与 File Search](#2-web-search-与-file-search)
3. [编程式工具调用（新能力）](#3-编程式工具调用新能力)
4. [Function Calling 自定义函数](#4-function-calling-自定义函数)
5. [多智能体执行架构](#5-多智能体执行架构)
6. [工具选择模式](#6-工具选择模式)

---

## 1. 工具全景：10 种内置工具

| 工具 | 类型 | 说明 |
|------|------|------|
| Web Search | `web_search` | 联网搜索 |
| File Search | `file_search` | 向量检索（vector_store） |
| Image Generation | `image_generation` | 文生图 |
| Code Interpreter | `code_interpreter` | Python 代码执行 |
| Hosted Shell | `hosted_shell` | 托管 Shell |
| Apply Patch | `apply_patch` | 补丁应用 |
| Skills | `skills` | 技能系统 |
| Computer Use | `computer_use` | 桌面操控 |
| MCP | `mcp` | 外部 MCP 服务器 |
| Tool Search | `tool_search` | 工具搜索 |
| **Function** | `function` | 自定义函数（JSON Schema） |

```python
# 工具组合使用
response = client.responses.create(
    model="gpt-5.6-terra",
    tools=[
        {"type": "web_search"},
        {"type": "file_search", "vector_store_ids": ["vs_123"]},
        {"type": "function", "name": "check_inventory", "parameters": {...}}
    ],
    input="查一下商品 X 的库存（调函数）和竞品价格（联网搜索）"
)
```

---

## 2. Web Search 与 File Search

```python
# ① Web Search（联网搜索）
response = client.responses.create(
    model="gpt-5.6-terra",
    tools=[{"type": "web_search"}],
    input="2026 年 Java 21 虚拟线程的最新生产实践"
)
# 输出中包含 citations（引用来源）
for item in response.output:
    if item.type == "web_search_call":
        print(item.citations)

# ② File Search（向量检索企业文档）
response = client.responses.create(
    model="gpt-5.6-terra",
    tools=[{
        "type": "file_search",
        "vector_store_ids": ["vs_123"],    # 先在 Vector Store 上传文档
        "max_num_results": 5
    }],
    input="公司的请假制度是什么？"
)
```

**File Search 流程：**
```text
① 创建 Vector Store（Files API 上传文档）
② 绑定 vector_store_ids 到工具
③ 提问 → 自动检索相关片段 → 生成回答
④ 输出包含 file_citation（来源文件）
```

---

## 3. 编程式工具调用（新能力）

**GPT-5.6 的核心新特性 — 模型编写 JavaScript 编排工具：**

```python
# 编程式工具调用：模型写 JS 在隔离 V8 运行时中执行
response = client.responses.create(
    model="gpt-5.6-terra",
    tools=[
        {"type": "code_interpreter"},
        {"type": "function", "name": "get_stock_price", ...},
        {"type": "function", "name": "get_exchange_rate", ...}
    ],
    input="""
    计算苹果公司股票（用美元）兑换成人民币的价值。
    先查股价，再查汇率，然后计算。
    """
)
```

```javascript
// 模型内部生成的编排代码（隔离 V8 沙箱，无网络访问）
// ① 调 get_stock_price("AAPL") → 拿到美元价格
// ② 调 get_exchange_rate("USD","CNY") → 拿到汇率
// ③ 计算 股价 × 汇率 → 输出结果
// 模型可以：条件分支、循环、数据加工 → 更复杂的工具编排
```

**编程式 vs 传统工具调用：**

| 对比 | 传统 | 编程式（新） |
|------|------|-------------|
| 编排逻辑 | 你的代码（每轮循环） | **模型写 JS 一次完成** |
| 多步依赖 | 多轮往返 | 单次执行 |
| 条件分支 | 每轮判断 | 代码内 if/else |
| 安全 | 你的沙箱 | **隔离 V8 + 无网络** |

---

## 4. Function Calling 自定义函数

```python
# 自定义函数（JSON Schema）
tools = [{
    "type": "function",
    "name": "get_weather",
    "description": "查询城市天气",
    "parameters": {
        "type": "object",
        "properties": {
            "city": {"type": "string", "description": "城市名"}
        },
        "required": ["city"]
    }
}]

response = client.responses.create(
    model="gpt-5.6-terra",
    tools=tools,
    input="北京天气怎么样？"
)

# Responses API 自动管理工具循环！
# 无需手动：执行工具 → 回传结果 → 再请求
# 只需在工具调用后追加 function_call_output
```

```python
# 执行工具后继续（Responses API 的方式）
response = client.responses.create(
    model="gpt-5.6-terra",
    previous_response_id=response.id,     # ★ 直接引用上一响应
    input=[{
        "type": "function_call_output",
        "call_id": call_id,               # 工具调用的 ID
        "output": "北京：晴 25°C"
    }]
)
```

---

## 5. 多智能体执行架构

```text
GPT-5.6 Sol 的 Ultra 模式多智能体架构：

主智能体（Ultra 协调者，4-16 个子智能体）
├── 任务拆解 → 分配给子智能体
├── 子智能体并行工作（独立上下文）
├── 交叉验证（减少幻觉）
├── 结果汇总 → 整合输出
└── 以更高算力换取更强结果和更快速度

适用场景：
├── 复杂分析（多维度并行）
├── 大型代码库任务
├── 研究报告生成
└── 安全审计（ExploitBench 73.5 验证）
```

---

## 6. 工具选择模式

| 模式 | 行为 | 场景 |
|------|------|------|
| `auto` | 模型自主决定 | 默认 |
| `required` | 必须至少调一次工具 | 数据获取任务 |
| `none` | 禁止工具 | 纯文本回复 |
| 指定工具 | 强制调用某工具 | 特定流程 |

```python
response = client.responses.create(
    model="gpt-5.6-terra",
    tools=tools,
    tool_choice="required",              # 强制工具调用
    input="总结这份订单数据"             # 模型必须先查数据再回答
)
```

---

> 🎯 **核心要点**：GPT-5.6 工具体系三大变化 — **① Responses API 内置工具循环（previous_response_id 简化多轮）② 编程式工具调用（模型写 JS 在 V8 沙箱编排，安全隔离）③ Ultra 多智能体（4-16 子 Agent 并行）**。10 种内置工具覆盖搜索/文件/代码/桌面/ MCP 全场景。

**下一模块**：[05-GPT-API开发实战](05-GPT-API开发实战.md) / **返回总览**：[00-总览](00-GPT-API知识体系总览.md)
