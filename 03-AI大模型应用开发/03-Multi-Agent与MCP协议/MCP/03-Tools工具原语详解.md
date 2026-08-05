# 03 - Tools 工具原语详解

> 🎯 Tools 是 MCP 的"行动层" — LLM 通过标准化接口执行搜索、查询、计算、部署等操作。与 OpenAI Function Calling Schema 兼容，但通过 MCP 实现跨平台复用

---

## 目录

1. [Tools 概念与架构](#1-tools-概念与架构)
2. [工具定义与 Schema](#2-工具定义与-schema)
3. [工具调用流程](#3-工具调用流程)
4. [错误处理与 ToolError](#4-错误处理与-toolerror)
5. [流式工具输出](#5-流式工具输出)
6. [Logging 日志系统](#6-logging-日志系统)

---

## 1. Tools 概念与架构

### 1.1 Tools API 总览

```text
┌──────────────────────────────────────────────────────────┐
│                    Tools API 完整列表                      │
├──────────────────────────────────────────────────────────┤
│                                                           │
│  核心操作：                                                │
│  ├── tools/list         列出所有可用工具及其 Schema        │
│  └── tools/call         调用指定工具                       │
│                                                           │
│  通知（Server → Client）：                                 │
│  ├── notifications/tools/list_changed  工具列表变更通知    │
│  │                                                         │
│  └── notifications/tools/progress      长任务进度通知      │
│      { progressToken, progress, total }                    │
│                                                           │
│  关联原语：                                                │
│  ├── logging/setLevel   设置日志级别                       │
│  └── notifications/message   日志消息通知                  │
│                                                           │
└──────────────────────────────────────────────────────────┘
```

### 1.2 Tools 在 MCP 架构中的位置

```text
  Client                                          Server
  ┌──────────┐    ① tools/list                    ┌──────────┐
  │          │ ──────────────────────────────────► │          │
  │  LLM     │    ② [Tool Schema List]            │  工具注册 │
  │  (决策)  │ ◄────────────────────────────────── │  中心     │
  │          │                                     │          │
  │          │    ③ tools/call {name, args}        │          │
  │          │ ──────────────────────────────────► │          │
  │          │    ④ [Tool Result]                  │          │
  │          │ ◄────────────────────────────────── │          │
  └──────────┘                                     └──────────┘
```

---

## 2. 工具定义与 Schema

### 2.1 完整 Tool 定义

```jsonc
// tools/list 响应示例
{
  "tools": [
    {
      "name": "get_weather",
      "description": "获取指定城市的实时天气信息。适用：用户询问天气、出行规划。不适用：历史天气查询",
      "inputSchema": {
        "type": "object",
        "properties": {
          "city": {
            "type": "string",
            "description": "城市名称，支持中文（如'北京'）和英文（如'Beijing'）"
          },
          "unit": {
            "type": "string",
            "enum": ["celsius", "fahrenheit"],
            "description": "温度单位：celsius=摄氏度, fahrenheit=华氏度",
            "default": "celsius"
          },
          "include_forecast": {
            "type": "boolean",
            "description": "是否包含未来3天预报",
            "default": false
          }
        },
        "required": ["city"],
        "additionalProperties": false
      },
      "annotations": {
        "title": "天气查询",
        "readOnlyHint": true,        // 提示 LLM：此工具只读，无副作用
        "destructiveHint": false,    // 非破坏性
        "idempotentHint": true,      // 提示 LLM：可安全重试
        "openWorldHint": true        // 提示 LLM：工具连接外部世界
      },
      "execution": {
        "progress": {
          "enabled": false           // 不支持进度通知
        },
        "timeout": 10000,           // 超时 10 秒（非标准字段，但常用）
        "retryable": true
      }
    },
    {
      "name": "deploy_service",
      "description": "部署服务到 Kubernetes 集群。⚠️ 会修改生产环境！使用前需确认",
      "inputSchema": {
        "type": "object",
        "properties": {
          "service_name": { "type": "string", "description": "服务名称" },
          "namespace": { "type": "string", "description": "K8s 命名空间", "default": "default" },
          "image_tag": { "type": "string", "description": "Docker 镜像标签" },
          "replicas": { "type": "integer", "description": "副本数", "minimum": 1, "maximum": 10 }
        },
        "required": ["service_name", "image_tag"]
      },
      "annotations": {
        "readOnlyHint": false,       // 会修改状态
        "destructiveHint": true,     // 可能破坏运行中的服务
        "idempotentHint": false,     // 非幂等（每次部署产生新版本）
        "openWorldHint": true
      },
      "execution": {
        "progress": {
          "enabled": true            // 支持进度通知！
        },
        "timeout": 300000            // 5 分钟超时
      }
    }
  ]
}
```

### 2.2 Tool Annotations（工具标注）

```text
Annotations 是给 LLM 的元数据提示，帮助 LLM 做出正确的调用决策：

readOnlyHint: true
  → LLM 知道：此工具不修改任何东西，可以放心调用，不需要用户确认

destructiveHint: true
  → LLM 知道：此工具有破坏性，需要用户确认后才能执行

idempotentHint: true
  → LLM 知道：失败后可以安全重试，不会产生重复副作用

openWorldHint: true
  → LLM 知道：工具访问外部世界，返回结果可能变化
```

### 2.3 输入 Schema 最佳实践

```text
Schema 设计 = LLM 填参质量

✅ 好 Schema 的特征：
  ① 每个属性有明确的 description（这是给 LLM 的提示！）
  ② 枚举值说明含义（不只看值名）
  ③ default 值减轻 LLM 决策负担
  ④ 约束清晰可验证（min/max/pattern/enum）
  ⑤ required 列表精确（不必要的不强制）

❌ 坏 Schema：
  { "type": "object", "properties": { "q": { "type": "string" } } }
  → LLM 完全不知道 q 是什么意思、怎么填
```

---

## 3. 工具调用流程

### 3.1 标准调用-响应流程

```jsonc
// === Client → Server: tools/call ===
{
  "jsonrpc": "2.0",
  "id": 5,
  "method": "tools/call",
  "params": {
    "name": "get_weather",
    "arguments": {
      "city": "北京",
      "unit": "celsius",
      "include_forecast": true
    }
  }
}

// === Server → Client: 成功响应 ===
{
  "jsonrpc": "2.0",
  "id": 5,
  "result": {
    "content": [
      {
        "type": "text",
        "text": "北京当前天气：晴，25°C，湿度45%，风力3级\n\n未来3天预报：\n- 7/30: 晴转多云 22-30°C\n- 7/31: 小雨 20-26°C\n- 8/1:  多云 21-28°C"
      }
    ],
    "isError": false
  }
}
```

### 3.2 多内容类型响应

```jsonc
// 工具可以返回多种类型的内容（文本 + JSON + 图片等）
{
  "result": {
    "content": [
      {
        "type": "text",
        "text": "生成了以下图表："
      },
      {
        "type": "image",
        "data": "iVBORw0KGgo...base64...",
        "mimeType": "image/png"
      },
      {
        "type": "resource",
        "resource": {
          "uri": "file:///output/report.pdf",
          "mimeType": "application/pdf",
          "text": "..."  // PDF 的文本提取
        }
      }
    ],
    "isError": false
  }
}
```

### 3.3 带进度的长任务调用

```jsonc
// === Client → Server: 带 progressToken 的调用 ===
{
  "jsonrpc": "2.0",
  "id": 10,
  "method": "tools/call",
  "params": {
    "name": "deploy_service",
    "arguments": { "service_name": "api-gateway", "image_tag": "v2.1.0" },
    "_meta": {
      "progressToken": "deploy-task-001"   // 用于关联进度通知
    }
  }
}

// === Server → Client: 进度通知（执行过程中多次） ===
{
  "jsonrpc": "2.0",
  "method": "notifications/progress",
  "params": {
    "progressToken": "deploy-task-001",
    "progress": 60,           // 当前进度
    "total": 100,             // 总进度（可选）
    "message": "正在滚动更新 Pod (2/3 完成)..."
  }
}

// === Server → Client: 最终响应 ===
{
  "jsonrpc": "2.0",
  "id": 10,
  "result": {
    "content": [
      { "type": "text", "text": "✅ 部署成功！api-gateway v2.1.0 已上线\n- 3/3 Pod 运行中\n- 健康检查通过\n- 访问地址: https://api.example.com" }
    ],
    "isError": false
  }
}
```

---

## 4. 错误处理与 ToolError

### 4.1 工具级错误返回

```jsonc
// 工具执行失败 — 在 result 中标记 isError: true（不是 JSON-RPC Error）
{
  "jsonrpc": "2.0",
  "id": 5,
  "result": {
    "content": [
      {
        "type": "text",
        "text": "❌ 城市 'Mars' 不在支持范围内。支持的城市：北京、上海、广州、深圳、..."
      }
    ],
    "isError": true
    // ⚠️ 这是"工具执行失败"，不是协议错误！
    // LLM 看到 isError: true 后可以调整参数重试
  }
}
```

### 4.2 JSON-RPC 级错误 vs 工具级错误

```text
┌─────────────────────────────────────────────────────────┐
│ 错误双层模型                                              │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  JSON-RPC Error（协议层）：                               │
│    原因：Method Not Found、Invalid Params、解析错误       │
│    表现：返回 error 字段，无 result 字段                  │
│    LLM 行为：无法处理，需要系统级修复                      │
│                                                          │
│  Tool Error（业务层）：                                   │
│    原因：参数合法但业务失败（城市不存在、权限不足）         │
│    表现：返回 result { isError: true, content: [...] }   │
│    LLM 行为：看到错误描述 → 调整策略重试                   │
│                                                          │
│  ┌─────────────────┬──────────────┬──────────────────┐   │
│  │                 │ JSON-RPC Err │   Tool Error     │   │
│  ├─────────────────┼──────────────┼──────────────────┤   │
│  │ 错误字段         │ error        │ result.isError   │   │
│  │ HTTP 状态码      │ 4xx/5xx      │ 200 OK           │   │
│  │ LLM 可恢复       │ ❌            │ ✅ (读错误描述)    │   │
│  │ 示例              │ 方法不存在    │ 城市不支持         │   │
│  └─────────────────┴──────────────┴──────────────────┘   │
└─────────────────────────────────────────────────────────┘
```

### 4.3 结构化错误内容

```jsonc
// 工具错误也可返回结构化信息，帮助 LLM 理解
{
  "result": {
    "content": [
      {
        "type": "text",
        "text": "验证失败：以下参数不合法"
      },
      {
        "type": "text",
        "text": "{\n  \"errors\": [\n    {\"field\": \"replicas\", \"message\": \"必须在 1-10 之间，当前值为 100\"},\n    {\"field\": \"image_tag\", \"message\": \"标签格式无效，应为 semver 格式\"}\n  ],\n  \"suggestion\": \"请修正上述参数后重试\"\n}"
      }
    ],
    "isError": true
  }
}
```

---

## 5. 流式工具输出

### 5.1 流式输出的使用场景

```text
哪些工具适合流式输出？

✅ 适合流式：
  • 长文本生成（摘要、报告）
  • 代码生成（逐行输出）
  • 日志实时追踪
  • 数据流处理

❌ 不适合流式：
  • 简单查询（一次返回即可）
  • 需要完整结果才能处理的操作
  • 原子性操作（要么全成功，要么全失败）
```

### 5.2 流式输出实现（Streamable HTTP）

```text
使用 Streamable HTTP 传输时，Tool 可返回 SSE 流：

Client: tools/call { name: "generate_report", arguments: {...} }
  │
  ▼
Server: (SSE Stream)
  event: message
  data: {"type":"text","text":"正在生成报告...\n"}
  
  event: message
  data: {"type":"text","text":"## 1. 概览\n\n本报告分析..."}
  
  event: message
  data: {"type":"text","text":"## 2. 详细数据\n\n| 指标 | 数值 |\n..."}
  
  event: message
  data: {"type":"text","text":"## 3. 结论\n\n综上所述..."}
  
  event: done
  data: {}
```

---

## 6. Logging 日志系统

### 6.1 日志级别设置

```jsonc
// Client → Server: 设置日志级别
{
  "jsonrpc": "2.0",
  "id": 30,
  "method": "logging/setLevel",
  "params": {
    "level": "debug"   // debug | info | notice | warning | error | critical | alert | emergency
  }
}

// Server → Client: {}  (确认)
```

### 6.2 日志消息通知

```jsonc
// Server → Client: 日志消息（Notification）
{
  "jsonrpc": "2.0",
  "method": "notifications/message",
  "params": {
    "level": "warning",
    "logger": "weather-api",
    "data": {
      "message": "天气 API 响应延迟超过 2 秒",
      "latency_ms": 2340,
      "endpoint": "/v2/forecast"
    }
  }
}
```

> 🎯 **核心要点**：Tools = JSON Schema 定义 + Annotations 语义标注 + 标准调用-响应 + isError 业务错误 + progressToken 进度通知。好的 Tool 定义 = LLM 能自主正确调用并处理错误

---

**上一模块**：[02 - Resources 资源原语详解](./02-Resources资源原语详解.md)  
**下一模块**：[04 - 传输层与通信机制](./04-传输层与通信机制.md)  
**返回总览**：[00 - MCP 知识体系总览](./00-MCP知识体系总览.md)
