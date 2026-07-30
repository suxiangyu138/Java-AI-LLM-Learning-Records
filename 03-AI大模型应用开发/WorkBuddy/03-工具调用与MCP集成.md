# 工具调用与 MCP 集成

> Function Call 让模型"能说也能做"，MCP 让工具连接标准化——从单点工具调用到开放协议生态

---

## 📚 目录

1. [Function Call 机制深入](#1-function-call-机制深入)
2. [MCP 协议详解](#2-mcp-协议详解)
3. [MCP Server 三种原语](#3-mcp-server-三种原语)
4. [MCP Server 开发实战](#4-mcp-server-开发实战)
5. [MCP Apps：工具返回 UI（2026）](#5-mcp-apps工具返回-ui2026)
6. [工具调用安全模型](#6-工具调用安全模型)
7. [最佳实践与避坑指南](#7-最佳实践与避坑指南)

---

## 1. Function Call 机制深入

### 1.1 工作原理

```text
┌─────────────────────────────────────────────────────┐
│                  Function Call 流程                    │
│                                                       │
│  1. 注册工具                                          │
│     定义 name / description / parameters (JSON Schema) │
│     ↓                                                 │
│  2. 模型决策                                          │
│     模型判断当前对话是否需要调用工具                      │
│     ↓ 需要                                           ↓ 不需要
│  3a. 生成调用请求     3b. 直接生成文本回复               │
│     {name, arguments}                                 │
│     ↓                                                 │
│  4. Agent 执行                                        │
│     校验参数 → 执行工具 → 获取结果                        │
│     ↓                                                 │
│  5. 结果注入上下文                                     │
│     将 tool_result 注入到 messages 中                    │
│     ↓                                                 │
│  6. 模型继续                                          │
│     基于工具结果生成最终回复（或再次调用工具）              │
└─────────────────────────────────────────────────────┘
```

### 1.2 工具定义规范

```json
{
  "name": "search_files",
  "description": "在项目目录中搜索包含指定关键词的文件，返回匹配的文件列表和行号",
  "parameters": {
    "type": "object",
    "properties": {
      "pattern": {
        "type": "string",
        "description": "要搜索的正则表达式模式"
      },
      "directory": {
        "type": "string",
        "description": "搜索的目录路径，默认为项目根目录"
      },
      "file_types": {
        "type": "array",
        "items": { "type": "string" },
        "description": "限定搜索的文件类型，如 ['.java', '.xml']"
      }
    },
    "required": ["pattern"]
  }
}
```

> 💡 **description 是给模型看的**——它决定了模型能否在合适的时机选择这个工具。描述要清晰说明"什么时候用"、"做什么"、"输入输出是什么"。

### 1.3 工具调用失败处理

| 失败类型 | 原因 | 处理策略 |
|---------|------|---------|
| **参数校验失败** | 模型生成的参数不符合 Schema | 返回错误信息，让模型修正后重试（最多 2 次） |
| **执行超时** | 工具执行时间过长 | 返回超时提示 + 部分结果，让模型决策 |
| **执行异常** | 工具内部错误 | 返回错误详情，降级到备选方案 |
| **权限不足** | 模型尝试调用未授权工具 | 返回"无权限"提示，不重试 |
| **幻觉调用** | 模型调用不存在的工具 | 返回"工具不存在"提示 |

---

## 2. MCP 协议详解

### 2.1 什么是 MCP

**Model Context Protocol（MCP）** 是 Anthropic 于 2024 年底发布的开放协议，旨在标准化 LLM 应用与外部数据源、工具之间的连接方式。

```text
MCP 的核心价值：
┌─────────────────────────────────────────┐
│          传统方式（N × M 集成）             │
│                                          │
│  每个 AI 应用 → 每个数据源都要写适配代码     │
│  App1 → DB1, App1 → API2, App1 → FS3    │
│  App2 → DB1, App2 → API2, App2 → FS3    │
│  App3 → DB1, App3 → API2, App3 → FS3    │
│  复杂度：N × M                             │
├─────────────────────────────────────────┤
│          MCP 方式（N + M 集成）              │
│                                          │
│  数据源暴露为 MCP Server                   │
│  所有 AI 应用通过统一协议连接                │
│  App1 ─┐                                 │
│  App2 ─┼─ MCP 协议 ─ MCP Servers ─ 数据源  │
│  App3 ─┘                                 │
│  复杂度：N + M                             │
└─────────────────────────────────────────┘
```

### 2.2 MCP 架构

```text
┌──────────────────┐
│   MCP Host       │  ← AI 应用（Claude Desktop、WorkBuddy、VS Code）
│   (主机)          │
└────────┬─────────┘
         │ MCP 协议（JSON-RPC over stdio / SSE / WebSocket）
         │
┌────────┴─────────┐
│   MCP Client     │  ← 协议客户端（内嵌在 Host 中）
│   (客户端)        │
└────────┬─────────┘
         │ 1:N 连接
         │
    ┌────┴────────────────┐
    │                     │
┌───┴──────────┐  ┌──────┴──────────┐
│ MCP Server A │  │ MCP Server B    │
│ (文件系统)    │  │ (数据库)         │
└──────────────┘  └─────────────────┘
```

### 2.3 传输方式

| 传输方式 | 适用场景 | 优点 | 缺点 |
|---------|---------|------|------|
| **stdio** | 本地进程通信 | 简单、安全、低延迟 | 仅限本机 |
| **SSE（HTTP）** | 远程服务 | 跨网络、可水平扩展 | 需要处理认证和网络安全 |
| **WebSocket** | 实时双向通信 | 全双工、低延迟 | 实现复杂度较高 |

---

## 3. MCP Server 三种原语

### 3.1 Resources（资源）

> **只读数据**暴露给模型，模型可以主动读取

```text
Resources 类型：
├── 文件内容：read_file(path) → 返回文件内容
├── 数据库记录：query_db(sql) → 返回查询结果（只读）
├── API 响应：get_api_status(url) → 返回状态信息
├── 系统信息：get_system_info() → CPU、内存、磁盘
└── 知识库文档：search_docs(query) → 返回相关文档
```

```json
// Resource 定义示例
{
  "uri": "file:///project/config/application.yml",
  "name": "应用配置文件",
  "description": "Spring Boot 应用的主配置文件",
  "mimeType": "text/yaml"
}
```

### 3.2 Tools（工具）

> **模型驱动的动作**，模型决定何时调用、传什么参数

```text
Tools 类型：
├── 创建/修改：create_file, update_record, send_message
├── 删除：delete_file, remove_record（需审批）
├── 执行：run_command, execute_query, trigger_workflow
├── 搜索：search_code, find_files, grep_content
└── 交互：ask_user, show_notification
```

```json
// Tool 定义示例
{
  "name": "create_issue",
  "description": "在 GitHub 仓库中创建一个 Issue",
  "inputSchema": {
    "type": "object",
    "properties": {
      "title": { "type": "string", "description": "Issue 标题" },
      "body": { "type": "string", "description": "Issue 内容（Markdown）" },
      "labels": { "type": "array", "items": { "type": "string" } }
    },
    "required": ["title"]
  }
}
```

### 3.3 Prompts（提示模板）

> **用户驱动的模板**，提供标准化的交互起点

```text
Prompts 类型：
├── 任务模板："帮我写一份 Q3 工作总结"
├── 角色模板："你是一个代码审查专家，请审查以下代码"
├── 格式模板："将以下内容整理为表格格式"
└── 流程模板："按照需求分析→设计→实现的步骤帮我..."
```

```json
// Prompt 定义示例
{
  "name": "code_review",
  "description": "标准化代码审查提示",
  "arguments": [
    {
      "name": "language",
      "description": "编程语言",
      "required": true
    },
    {
      "name": "focus_areas",
      "description": "关注领域：security, performance, readability",
      "required": false
    }
  ]
}
```

### 3.4 三种原语的对比

| 维度 | Resources | Tools | Prompts |
|------|-----------|-------|---------|
| **发起方** | 模型主动读取 | 模型决定调用 | 用户选择使用 |
| **数据流向** | Server → Model | Model → Server（执行）→ Model | Server → User |
| **是否可写** | 只读 | 可读写 | 只读 |
| **安全性** | 低风险 | 需审批机制 | 无风险 |
| **典型用途** | 文档查询、配置读取 | 文件操作、API 调用 | 模板化交互 |

---

## 4. MCP Server 开发实战

### 4.1 最简单的 MCP Server（Python）

```python
# weather_mcp_server.py
import json
import sys
from typing import Any

# MCP Server 通过 stdio 与 Host 通信
# 读取 stdin，写入 stdout

def handle_request(request: dict) -> dict:
    """处理 MCP 请求"""
    method = request.get("method")

    if method == "tools/list":
        return {
            "tools": [{
                "name": "get_weather",
                "description": "获取指定城市的天气信息",
                "inputSchema": {
                    "type": "object",
                    "properties": {
                        "city": {
                            "type": "string",
                            "description": "城市名称，如 Beijing"
                        }
                    },
                    "required": ["city"]
                }
            }]
        }

    elif method == "tools/call":
        tool_name = request["params"]["name"]
        args = request["params"]["arguments"]

        if tool_name == "get_weather":
            city = args["city"]
            # 实际项目中调用天气 API
            result = f"{city}：晴，22-30°C，湿度 45%"
            return {
                "content": [{"type": "text", "text": result}]
            }

    return {"error": "Unknown method"}

# 主循环：读取 JSON-RPC 请求，返回响应
if __name__ == "__main__":
    while True:
        line = sys.stdin.readline()
        if not line:
            break
        request = json.loads(line)
        response = handle_request(request)
        sys.stdout.write(json.dumps(response) + "\n")
        sys.stdout.flush()
```

### 4.2 MCP Server 配置（Claude Desktop）

```json
// claude_desktop_config.json
{
  "mcpServers": {
    "weather": {
      "command": "python",
      "args": ["weather_mcp_server.py"],
      "env": {
        "API_KEY": "your-api-key"
      }
    },
    "filesystem": {
      "command": "npx",
      "args": ["-y", "@anthropic/mcp-server-filesystem", "/allowed/path"]
    }
  }
}
```

### 4.3 MCP Server 开发清单

| 步骤 | 内容 | 关键点 |
|:---:|------|------|
| 1 | 选择传输方式 | stdio（本地）或 SSE（远程） |
| 2 | 定义 Tools/Resources/Prompts | 描述要精准，Schema 要完整 |
| 3 | 实现请求处理 | JSON-RPC 协议，处理 `tools/list`、`tools/call` |
| 4 | 错误处理 | 返回结构化错误，而非崩溃 |
| 5 | 测试验证 | 用 MCP Inspector 或 Claude Desktop 测试 |
| 6 | 文档与发布 | Readme、配置说明、安装指南 |

---

## 5. MCP Apps：工具返回 UI（2026）

### 5.1 概念

2026 年 MCP 协议扩展了 **MCP Apps**，允许 MCP Server 的工具返回**交互式 UI 组件**，而不仅仅是文本。

```text
传统 MCP Tool：
  用户：帮我创建一个 GitHub Issue
  Agent：调用 create_issue
  返回："✅ Issue #42 已创建"
  用户需要自己去 GitHub 确认

MCP Apps：
  用户：帮我创建一个 GitHub Issue
  Agent：调用 create_issue
  返回：一个内嵌的 Issue 预览卡片
        ┌─────────────────────────────┐
        │ Issue #42: Fix login bug    │
        │ Status: Open                │
        │ Assignee: 张三              │
        │ [查看详情] [编辑] [关闭]     │
        └─────────────────────────────┘
  用户可以直接在聊天界面操作
```

### 5.2 支持的 UI 组件

| 组件 | 适用场景 | 示例 |
|------|---------|------|
| **表单（Form）** | 需要用户填写信息 | 创建 Issue、提交审批 |
| **确认框（Confirm）** | 高危操作确认 | 删除文件、发送邮件 |
| **卡片（Card）** | 展示结构化信息 | 用户信息、Issue 详情 |
| **仪表盘（Dashboard）** | 多维度数据展示 | 项目进度、系统监控 |
| **选择器（Picker）** | 从选项中选取 | 选择分支、选择模板 |

---

## 6. 工具调用安全模型

### 6.1 安全层次

```text
┌──────────────────────────────────────┐
│ L4: 审计日志                          │
│ 所有工具调用记录 → 异常检测 → 告警      │
├──────────────────────────────────────┤
│ L3: 审批门（Approval Gate）            │
│ 高危操作 = 人工确认，低风险 = 自动通过   │
├──────────────────────────────────────┤
│ L2: 权限校验                          │
│ 细粒度权限：文件目录、网络域名、数据库表  │
├──────────────────────────────────────┤
│ L1: 工具 Schema 约束                   │
│ 参数类型校验、必填校验、格式校验          │
└──────────────────────────────────────┘
```

### 6.2 风险等级分类

| 风险等级 | 操作类型 | 审批策略 | 示例 |
|:---:|------|------|------|
| 🔴 **Critical** | 不可逆操作、安全敏感 | 必须人工确认 | `rm -rf`、`git push --force`、发送邮件 |
| 🟡 **Warning** | 可逆但影响大 | 首次确认后可批量 | 修改配置文件、创建大量文件 |
| 🟢 **Safe** | 只读或无影响 | 自动通过 | 读取文件、搜索代码、查询 API |

---

## 7. 最佳实践与避坑指南

### 7.1 工具设计原则

| 原则 | 说明 | 好例子 | 坏例子 |
|------|------|-------|-------|
| **单一职责** | 一个工具只做一件事 | `read_file(path)` | `process_data(type, data)` 同时读取和写入 |
| **幂等性** | 同一参数多次调用结果一致 | `get_user(id)` | `generate_random_id()` |
| **可重试** | 失败后可以安全重试 | `search(query)` | `delete_file(path)` |
| **有边界** | 明确输入范围和限制 | `search(query, max_results=10)` | `search(query)` 可能返回全部结果 |
| **可观测** | 记录调用日志 | 每次调用记日志 | 黑盒执行 |

### 7.2 MCP 常见陷阱

| 陷阱 | 问题 | 解决方案 |
|------|------|---------|
| **工具定义不稳定** | 改了工具名或参数，历史会话中的工具调用失败 | 工具定义版本化，旧名保留为别名 |
| **超时没有处理** | MCP Server 卡住，Host 无限等待 | 设置合理超时（30s-120s），超时返回优雅降级 |
| **大结果不截断** | 查询返回 10 万行数据，塞爆上下文 | 结果分页 + 截断提示："返回前 50 条，共 10000 条" |
| **加密凭证硬编码** | API Key 写在配置文件里 | 使用环境变量或 Secret Manager |
| **忽略 MCP Server 崩溃** | Server 挂了但 Host 不知道 | 心跳检测 + 自动重连 |

### 7.3 工具描述撰写指南

```text
好的工具描述 = 什么时候用 + 做什么 + 输入什么 + 输出什么

❌ 坏的描述：
  "搜索文件"

✅ 好的描述：
  "在指定目录中搜索包含正则匹配模式的文件内容。
   当需要查找某个函数定义、变量使用位置、或特定代码模式时使用。
   输入：pattern（正则表达式）、directory（可选，搜索目录）。
   输出：匹配的文件路径列表，每项包含文件路径和匹配行号。"
```

---

> 🎯 **核心要点**：Function Call 是模型的"手"，MCP 是"标准化接口"——前者让模型能做事，后者让连接外部系统变得统一可复用。安全模型必须内置在工具调用层，不能依赖模型自觉遵守权限边界。

---

**上一模块**：[02-Harness 工程与上下文管理](02-Harness工程与上下文管理.md) ｜ **下一模块**：[04-Skill 与 Plugin 能力体系](04-Skill与Plugin能力体系.md) ｜ **返回总览**：[00-WorkBuddy 总览](00-WorkBuddy总览.md)
