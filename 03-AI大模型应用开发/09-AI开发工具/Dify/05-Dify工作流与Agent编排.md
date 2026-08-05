# Dify 工作流与 Agent 编排

> 🔀 从简单的 LLM 链到复杂的多分支工作流 —— 知识检索、代码执行、HTTP 请求、条件分支、迭代循环、变量传递、Agent 策略，Dify 工作流全解析

---

## 📚 目录

1. [工作流概述与核心概念](#1-工作流概述与核心概念)
2. [工作流节点详解](#2-工作流节点详解)
3. [变量系统与 Jinja2 模板](#3-变量系统与-jinja2-模板)
4. [Agent 策略与工具配置](#4-agent-策略与工具配置)
5. [工作流实战案例](#5-工作流实战案例)

---

## 1. 工作流概述与核心概念

### 1.1 工作流 vs 聊天助手

```text
聊天助手 (Chat App)：                    工作流 (Workflow)：
┌─────────────────────┐                ┌─────────────────────┐
│   用户输入           │                │    开始 (触发器)     │
│      ↓              │                │        ↓            │
│  系统提示词          │                │   [节点1] 知识检索   │
│      ↓              │                │        ↓            │
│  LLM 生成           │                │   [节点2] LLM 推理   │
│      ↓              │                │        ↓            │
│   返回结果           │                │   [节点3] IF-ELSE   │
└─────────────────────┘                │     ╱       ╲      │
                                     │    ↓         ↓     │
   → 单一 LLM 调用                   │ [LLM A]   [LLM B]   │
   → 线性流程                        │    ↓         ↓     │
   → 简单场景                        │   [节点5] 代码执行   │
                                     │        ↓            │
                                     │    结束 (输出)       │
                                     └─────────────────────┘
                                       → 多 LLM 调用
                                       → 分支/循环/并行
                                       → 复杂业务流程
```

### 1.2 工作流的四大特点

| 特点 | 说明 | 典型场景 |
|------|------|---------|
| **可视化** | 拖拽节点、连线编排，所见即所得 | 降低开发门槛 |
| **分支** | IF-ELSE 条件分支、多路选择 | 不同问题走不同处理链路 |
| **迭代** | 循环处理列表数据 | 批量分析文档、逐条处理 |
| **模板** | Jinja2 变量在节点间传递 | 数据在各步骤间流转 |

### 1.3 工作流创建流程

```text
首页 → 创建应用 → 工作流 → 命名

工作流编辑器界面：
┌─────────────────────────────────────────────┐
│  左侧：节点面板                              │
│  ├── 开始 (Start)                           │
│  ├── LLM                                    │
│  ├── 知识检索 (Knowledge Retrieval)          │
│  ├── 代码 (Code)                            │
│  ├── HTTP 请求 (HTTP Request)               │
│  ├── IF-ELSE 条件分支                       │
│  ├── 迭代 (Iteration)                       │
│  ├── 模板转换 (Template)                     │
│  ├── 变量聚合 (Variable Aggregator)          │
│  ├── 参数提取 (Parameter Extractor)          │
│  ├── 工具 (Tool)                             │
│  └── 结束 (End)                             │
├─────────────────────────────────────────────┤
│  中间：画布 (拖拽节点 + 连线)                 │
├─────────────────────────────────────────────┤
│  右侧：节点配置面板                           │
│  顶部：运行/发布/日志                         │
└─────────────────────────────────────────────┘
```

---

## 2. 工作流节点详解

### 2.1 开始节点 (Start)

```text
工作流的入口，定义输入变量：

输入字段：
├── 文本输入：{{input.query}} 或自定义名称
├── 段落输入：长文本
├── 下拉选项：预定义选项
├── 数字：数值型输入
└── 文件：支持图片、文档上传

这些变量在整个工作流中都能以 {{#start.变量名#}} 引用
```

### 2.2 LLM 节点

```text
LLM 节点 = 调用大模型的核心节点

配置项：
├── 模型选择：GPT-4o / DeepSeek-V4 / Qwen-Max...
├── 上下文：关联工作流中的系统提示词
├── 记忆：是否携带对话历史（聊天工作流）
├── 提示词：支持 Jinja2 模板
│   ├── SYSTEM：角色定义 + 行为规则
│   └── USER：当前任务 + 数据
├── 输出变量：
│   ├── {{#llm.text#}} — LLM 生成的文本
│   ├── {{#llm.usage#}} — Token 消耗
│   └── {{#llm.reasoning_content#}} — 推理过程 (DeepSeek-R1)

示例提示词：
SYSTEM: 你是一个数据分类专家，将用户输入分类到以下类别之一。
USER: 请分类以下文本：{{#start.query#}}
```

### 2.3 知识检索节点

```text
知识检索节点 = 从知识库中检索相关片段

配置项：
├── 关联知识库：选择一个或多个 Dataset
├── 检索模式：向量/全文/混合
├── Top-K：返回片段数 (3-5)
├── 分数阈值：0.5-0.7
├── Rerank：是否启用
├── 查询内容：{{#llm_query.text#}} 或 {{#start.query#}}
├── 输出变量：
│   ├── {{#knowledge.result#}} — 检索结果数组
│   └── 每个结果含：content, metadata, score
```

### 2.4 代码节点 (Code)

```text
代码节点 = 在沙箱中执行 Python/JavaScript 代码

支持语言：
├── Python 3 (默认)
└── JavaScript (Node.js)

内置 Python 库：
├── json, re, datetime, math, random
├── requests (HTTP 请求)
├── numpy, pandas (需在 .env 启用)
└── 不支持 pip install

输入变量：{{#variable_name#}}
输出变量：return {"key": value}

示例：
```

```python
# 代码节点：提取 JSON 中的关键字段
import json

def main(input_data: str) -> dict:
    """输入：上一个节点的输出 JSON 字符串"""
    data = json.loads(input_data)

    # 提取字段
    result = {
        "order_id": data.get("order_id"),
        "total_amount": data.get("total", 0),
        "item_count": len(data.get("items", [])),
        "is_high_value": data.get("total", 0) > 1000
    }
    return result
```

### 2.5 HTTP 请求节点

```text
HTTP 请求节点 = 调用外部 API

配置项：
├── 请求方法：GET / POST / PUT / DELETE / PATCH
├── URL：支持 Jinja2 变量 {{#var#}}
├── 请求头：JSON 键值对
├── 请求体：JSON Body
├── 认证：None / Basic / Bearer Token / API Key
├── 超时：默认 30s
├── 输出变量：
│   ├── {{#http.body#}} — 响应体
│   ├── {{#http.status_code#}} — 状态码
│   └── {{#http.headers#}} — 响应头

示例：
URL: https://api.yourservice.com/orders/{{#code.order_id#}}
Headers: Authorization: Bearer {{#api_token#}}
Body: {}
```

### 2.6 IF-ELSE 条件分支

```text
IF-ELSE 节点 = 根据条件走不同分支

支持的条件运算符：
├── = (等于)
├── != (不等于)
├── > (大于)
├── < (小于)
├── contains (包含)
├── not contains (不包含)
├── startswith (以...开头)
├── endswith (以...结尾)
├── is empty (为空)
├── is not empty (非空)

多条件组合：
├── AND → 所有条件都满足
└── OR  → 任一条件满足

条件引用变量：{{#code.is_high_value#}}
```

### 2.7 迭代节点 (Iteration)

```text
迭代节点 = 对列表逐条处理

配置项：
├── 输入列表：{{#variable#}}（必须是数组）
├── 并行/串行：默认串行（推荐）
├── 循环体：内部节点链
├── 迭代变量：{{#iteration.item#}} — 当前项
├── 输出变量：
│   └── {{#iteration.output#}} — 所有迭代输出合并的数组

示例：
输入：[{"name":"A"}, {"name":"B"}, {"name":"C"}]
循环内：LLM 分析 {{#iteration.item.name}}
输出：[分析A, 分析B, 分析C]
```

### 2.8 其他节点

| 节点 | 功能 | 使用场景 |
|------|------|---------|
| **模板转换** | Jinja2 模板处理文本 | 拼接多段数据、格式化输出 |
| **变量聚合** | 合并多个节点输出 | 聚合多个分支结果 |
| **参数提取** | LLM 提取结构化参数 | 从自然语言提取 JSON |
| **工具** | 调用预定义工具 | API 工具 / 代码工具 |
| **结束** | 定义最终输出 | 设置返回的变量 |

---

## 3. 变量系统与 Jinja2 模板

### 3.1 变量引用语法

```text
Dify 工作流中的变量引用：

{{#节点类型.节点名称.字段名#}}

示例：
├── {{#start.query#}}             → 开始节点的 query 字段
├── {{#llm.text#}}                → LLM 节点的生成文本
├── {{#knowledge.result#}}        → 知识检索节点的结果
├── {{#http.body#}}               → HTTP 节点的响应体
├── {{#code.result#}}             → 代码节点的返回值
├── {{#iteration.item#}}          → 迭代中的当前项
├── {{#iteration.output#}}        → 迭代结束的合并输出
├── {{#sys.query#}}               → 系统变量：用户当前输入
├── {{#sys.conversation_id#}}     → 系统变量：对话 ID
└── {{#sys.user_id#}}             → 系统变量：用户 ID
```

### 3.2 Jinja2 模板高级用法

```jinja2
{# === 条件判断 === #}
{% if code.score > 0.8 %}
这是高置信度结果。
{% elif code.score > 0.5 %}
这个结果需要进一步确认。
{% else %}
置信度过低，建议人工审核。
{% endif %}

{# === 循环 === #}
{% for item in knowledge.result %}
片段 {{loop.index}} (来源: {{item.metadata.source}}):
{{item.content}}
---
{% endfor %}

{# === 过滤器 === #}
{{llm.text | trim}}                     {# 去除首尾空白 #}
{{start.query | upper}}                  {# 转大写 #}
{{start.query | length}}                 {# 字符串长度 #}
{{json_data | tojson}}                   {# 转 JSON 字符串 #}

{# === 变量设置 === #}
{% set total = code.price * code.count %}
总价：{{total}} 元

{# === JSON 解析 === #}
{% set order = http.body | fromjson %}
订单号：{{order.id}}
金额：{{order.amount}}
```

---

## 4. Agent 策略与工具配置

### 4.1 Agent 模式

```text
Dify Agent = LLM + 工具调用能力

两种策略：

┌─────────────────────────────────────────────────────────────┐
│ 策略           │ 原理                │ 适用场景              │
├─────────────────────────────────────────────────────────────┤
│ ReAct          │ 思考→行动→观察→循环  │ 复杂推理、多步决策    │
│ Function Call  │ LLM 直接调用工具函数 │ 结构化任务、高效      │
└─────────────────────────────────────────────────────────────┘

ReAct 循环过程：
1. Thought: "我需要查天气数据"
2. Action: 调用天气 API 工具
3. Observation: 收到天气数据
4. Thought: "基于天气数据，给出建议"
5. Action: 生成最终回答
```

### 4.2 工具类型

| 工具类型 | 说明 | 配置示例 |
|---------|------|---------|
| **API 工具** | 调用外部 REST API | OpenAPI / Swagger 导入 |
| **代码工具** | 自定义 Python/JS 函数 | 数据处理脚本 |
| **知识库工具** | 搜索知识库 | 已有 Dataset |
| **插件** | Dify 插件生态 | 社区 + 自定义 |

### 4.3 创建 API 工具

```yaml
# OpenAPI (Swagger) 格式导入
openapi: 3.0.0
info:
  title: 订单查询 API
  version: 1.0.0
servers:
  - url: https://api.yourcompany.com/v1
paths:
  /orders/{orderId}:
    get:
      summary: 查询订单详情
      parameters:
        - name: orderId
          in: path
          required: true
          schema:
            type: string
      responses:
        '200':
          description: 订单信息
          content:
            application/json:
              schema:
                type: object
  /orders:
    post:
      summary: 创建订单
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              properties:
                product_id:
                  type: string
                quantity:
                  type: integer
```

### 4.4 Agent 配置要点

```text
Agent 设置中的关键配置：

├── 最大迭代次数：5-10
│   → Agent 最多进行几轮思考-行动循环
│   → 太小：任务未完成就停止
│   → 太大：可能无限循环或 Token 消耗过多
│
├── 工具调用策略：
│   ├── Auto：LLM 自己决定何时使用工具
│   ├── Sequential：按设定顺序依次调用
│   └── Manual：由工作流节点控制
│
└── 输出变量：
    ├── {{#agent.text#}} — 最终回答
    ├── {{#agent.steps#}} — 执行步骤历史
    └── {{#agent.usage#}} — Token 消耗
```

---

## 5. 工作流实战案例

### 5.1 案例一：智能客服分流

```text
场景：根据用户问题自动分类，走不同处理链路

开始 (用户输入)
    │
    ▼
LLM (分类意图)
 → 输出：{category: "订单查询", sub_category: "发货进度"}
    │
    ▼
IF-ELSE 条件分支
    │ category == "订单查询"
    ├──→ 知识检索 (订单相关 FAQ)
    │         │
    │         ▼
    │    LLM (回答订单问题 + 知识库)
    │
    │ category == "技术咨询"
    ├──→ HTTP 请求 (查询技术支持系统 API)
    │         │
    │         ▼
    │    模板转换 (格式化技术回答)
    │
    │ category == "投诉建议"
    └──→ LLM (安抚情绪 + 记录投诉)
              │
              ▼
         HTTP 请求 (写入工单系统)
              │
              ▼
    结束 (输出)
```

### 5.2 案例二：多文档分析 Agent

```text
场景：上传多篇文档，逐篇分析并汇总

开始 (接收多个文件)
    │
    ▼
代码 (解析文件列表)
 → 输出：[{name:"A.pdf",text:"..."},{name:"B.pdf",text:"..."}]
    │
    ▼
迭代节点 (逐篇处理)
    │
    └─→ LLM (分析 {{#iteration.item.name}})
         → 输出：摘要、关键词、情感倾向
    │
    ▼
变量聚合 (收集所有分析结果)
    │
    ▼
LLM (汇总分析)
 → 输出：整体报告
    │
    ▼
结束
```

### 5.3 案例三：API 编排 + 数据转换

```text
场景：调用多个 API，转换数据，格式化输出

开始 (用户输入产品名)
    │
    ▼
HTTP 请求 (搜索产品 API)
 → 返回：产品 ID 列表
    │
    ▼
迭代 (逐个查详情)
    │
    └─→ HTTP 请求 (产品详情 API)
         → 代码节点 (提取价格/库存/评分)
    │
    ▼
变量聚合 (合并所有产品信息)
    │
    ▼
LLM (对比推荐)
 → 输出：性价比最高产品推荐 + 理由
    │
    ▼
结束
```

---

**上一模块**：[04-Dify知识库与RAG实践](./04-Dify知识库与RAG实践.md) ｜ **下一模块**：[06-Dify API与外部集成](./06-Dify API与外部集成.md) ｜ **返回总览**：[00-Dify知识体系总览](./00-Dify知识体系总览.md)

---

*创建于：2026年7月*
