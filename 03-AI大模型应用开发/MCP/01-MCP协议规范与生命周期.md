# 01 - MCP 协议规范与生命周期

> 🎯 MCP 基于 JSON-RPC 2.0，定义了 Client-Server 间的完整通信流程。理解协议规范和生命周期，是掌握 MCP 的基石

---

## 目录

1. [MCP 协议总览](#1-mcp-协议总览)
2. [JSON-RPC 2.0 消息格式](#2-json-rpc-20-消息格式)
3. [协议生命周期](#3-协议生命周期)
4. [Capabilities 能力协商](#4-capabilities-能力协商)
5. [协议版本与演进](#5-协议版本与演进)

---

## 1. MCP 协议总览

### 1.1 协议分层架构

```text
┌──────────────────────────────────────────────────────────┐
│                     MCP 协议栈                             │
├──────────────────────────────────────────────────────────┤
│  Layer 4: 业务原语层                                       │
│  ├── Resources: resources/list, resources/read, ...       │
│  ├── Tools:     tools/list, tools/call, ...               │
│  ├── Prompts:   prompts/list, prompts/get, ...            │
│  └── Logging:   logging/setLevel, notifications/message   │
├──────────────────────────────────────────────────────────┤
│  Layer 3: 生命周期层                                       │
│  ├── initialize        — 能力协商 + 协议版本确认           │
│  ├── initialized       — 通知 Client 初始化完成            │
│  ├── ping/pong         — 心跳保活                          │
│  └── Cancellation      — 请求取消                          │
├──────────────────────────────────────────────────────────┤
│  Layer 2: JSON-RPC 2.0 消息层                             │
│  ├── Request           — { jsonrpc, id, method, params }  │
│  ├── Response          — { jsonrpc, id, result }          │
│  ├── Error             — { jsonrpc, id, error }           │
│  └── Notification      — { jsonrpc, method, params }      │
├──────────────────────────────────────────────────────────┤
│  Layer 1: 传输层                                           │
│  ├── stdio             — 本地进程间通信                    │
│  ├── HTTP + SSE        — 远程 HTTP 通信                   │
│  └── Streamable HTTP   — 流式 HTTP (2025 spec)            │
└──────────────────────────────────────────────────────────┘
```

### 1.2 Client-Server 角色

```text
┌──────────────────┐                    ┌──────────────────┐
│   MCP Client     │                    │   MCP Server     │
│   (Host 端)      │◄──── JSON-RPC ────►│   (工具/数据方)   │
├──────────────────┤                    ├──────────────────┤
│ 发起请求          │                    │ 响应请求           │
│ 消费 Resources   │                    │ 提供 Resources    │
│ 调用 Tools       │                    │ 注册 Tools        │
│ 获取 Prompts     │                    │ 定义 Prompts      │
│ 发送通知          │                    │ 发送通知           │
│ 管理连接生命周期   │                    │ 暴露 Capabilities  │
└──────────────────┘                    └──────────────────┘

注意：MCP 协议支持双向通信 —
  Server → Client：Sampling（让 LLM 生成内容）、Logging通知
  Client → Server：Tools调用、Resources读取、Prompts获取
```

---

## 2. JSON-RPC 2.0 消息格式

### 2.1 四种消息类型

```jsonc
// ① Request（请求 — 需要响应）
{
  "jsonrpc": "2.0",
  "id": 1,                           // 唯一标识（数字/字符串）
  "method": "tools/call",            // 方法名
  "params": {                        // 参数对象
    "name": "get_weather",
    "arguments": { "city": "北京" }
  }
}

// ② Response（成功响应）
{
  "jsonrpc": "2.0",
  "id": 1,                           // 对应请求的 id
  "result": {                        // 返回值
    "content": [
      { "type": "text", "text": "北京 晴 25°C" }
    ]
  }
}

// ③ Error（错误响应）
{
  "jsonrpc": "2.0",
  "id": 1,
  "error": {
    "code": -32602,                  // 错误码
    "message": "Invalid params",     // 错误描述
    "data": {                        // 附加信息（可选）
      "details": "city is required"
    }
  }
}

// ④ Notification（通知 — 无需响应，id 省略）
{
  "jsonrpc": "2.0",
  "method": "notifications/initialized"
  // 无 id 字段 = 通知，Server 不应回复
}
```

### 2.2 标准错误码

| 错误码 | 含义 | 使用场景 |
|:---:|------|----------|
| **-32700** | Parse Error | JSON 解析失败 |
| **-32600** | Invalid Request | 不是有效的 JSON-RPC 请求 |
| **-32601** | Method Not Found | 方法不存在 |
| **-32602** | Invalid Params | 参数无效 |
| **-32603** | Internal Error | 内部错误 |
| **-32000** | Server Not Initialized | Server 未初始化 |
| **-32001** | Unknown Capability | 请求了未声明的 Capability |
| **-32002~-32099** | 预留 | MCP 实现自定义错误 |

### 2.3 请求-响应匹配

```text
Client 必须支持乱序响应：
  Client 发送:  id=1, id=2, id=3
  Server 返回:  id=3, id=1, id=2    ← 顺序可以不同

Client 通过 id 匹配：
  id 可以是 number 或 string
  Server 必须在响应中返回相同的 id
  Notification 无 id → 不需要响应
```

---

## 3. 协议生命周期

### 3.1 完整生命周期

```text
┌────────────────────────────────────────────────────────────┐
│                    MCP 连接生命周期                          │
├────────────────────────────────────────────────────────────┤
│                                                             │
│  ① 传输层建立                                               │
│     stdio: 启动子进程 | HTTP: TCP连接 → SSE通道              │
│     │                                                       │
│     ▼                                                       │
│  ② 初始化阶段 (initialize)                                  │
│     Client → Server: initialize 请求                        │
│       { protocolVersion, capabilities, clientInfo }        │
│     Server → Client: initialize 响应                        │
│       { protocolVersion, capabilities, serverInfo }        │
│     │                                                       │
│     ▼                                                       │
│  ③ 就绪通知 (initialized)                                   │
│     Client → Server: notifications/initialized             │
│     │                                                       │
│     ▼                                                       │
│  ④ 正常操作阶段                                             │
│     • Tools: tools/list, tools/call                        │
│     • Resources: resources/list, resources/read            │
│     • Prompts: prompts/list, prompts/get                   │
│     • Logging: logging/setLevel, 日志通知                  │
│     • Ping: ping → pong (心跳)                              │
│     │                                                       │
│     ▼                                                       │
│  ⑤ 终止阶段                                                 │
│     • Client 主动关闭连接                                    │
│     • Server 主动关闭（先发通知）                             │
│     • 传输层异常断开（需重连逻辑）                             │
│                                                             │
└────────────────────────────────────────────────────────────┘
```

### 3.2 Initialize 阶段详解

```jsonc
// === Client → Server: initialize 请求 ===
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "initialize",
  "params": {
    "protocolVersion": "2024-11-05",   // 客户端支持的协议版本
    "capabilities": {                   // 客户端能力声明
      "roots": {
        "listChanged": true             // 支持 Roots 列表变更通知
      },
      "sampling": {}                    // 支持 Server→Client 的 LLM 采样请求
    },
    "clientInfo": {
      "name": "claude-code",
      "version": "1.0.0"
    }
  }
}

// === Server → Client: initialize 响应 ===
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": {
    "protocolVersion": "2024-11-05",   // 协商后的协议版本
    "capabilities": {                   // 服务端能力声明
      "tools": {
        "listChanged": true             // 支持工具列表动态变更通知
      },
      "resources": {
        "subscribe": true,              // 支持资源变更订阅
        "listChanged": true
      },
      "prompts": {
        "listChanged": true
      },
      "logging": {}                     // 支持日志级别设置
    },
    "serverInfo": {
      "name": "weather-mcp-server",
      "version": "2.1.0"
    },
    "instructions": "此 Server 提供天气查询、预报和预警功能。使用 get_weather 查询实时天气。"  // 可选：给 LLM 的使用说明
  }
}

// === Client → Server: initialized 通知 ===
{
  "jsonrpc": "2.0",
  "method": "notifications/initialized"
  // 此后才能进行 Tools/Resources/Prompts 操作
}
```

### 3.3 心跳保活

```jsonc
// === Client → Server ===
{ "jsonrpc": "2.0", "id": 99, "method": "ping" }

// === Server → Client ===
{ "jsonrpc": "2.0", "id": 99, "result": {} }
```

---

## 4. Capabilities 能力协商

### 4.1 Capabilities 全景

```text
MCP Capabilities 全览（2025 Spec）

Client Capabilities（Client → Server 声明）：
├── roots                    # Client 提供 Roots（文件/目录的根）
│   └── listChanged          #   支持 Roots 变化通知
├── sampling                 # Client 支持 Server 请求 LLM 采样
└── experimental             # 实验性能力

Server Capabilities（Server → Client 声明）：
├── tools                    # Server 提供工具
│   └── listChanged          #   工具列表支持动态变更通知
├── resources                # Server 提供资源
│   ├── subscribe            #   支持资源变更订阅
│   └── listChanged          #   资源列表动态变更通知
├── prompts                  # Server 提供提示模板
│   └── listChanged          #   提示列表动态变更通知
├── logging                  # Server 支持日志级别设置
├── completions              # Server 支持自动补全
└── experimental             # 实验性能力
```

### 4.2 能力协商流程

```text
能力协商 = 取交集

范例：
  Client 声明: { sampling: {}, roots: {} }
  Server 声明: { tools: {}, resources: {}, logging: {} }

  协商后可用：
    Client → Server: tools/call ✅, resources/read ✅
    Server → Client: sampling/createMessage ✅
    
  不可用（因为对方没声明）：
    prompts/get ❌ (Client 知道 Server 不支持)

原则：
  ① 双方都声明的能力才可使用
  ② 使用未声明能力 → Error code -32001
  ③ 实验性能力需双方都声明 experimental: {}
```

### 4.3 动态能力变更

```jsonc
// Server 通知 Client：工具列表变了
{
  "jsonrpc": "2.0",
  "method": "notifications/tools/list_changed"
  // Client 收到后应重新调用 tools/list
}

// Server 通知 Client：资源列表变了
{
  "jsonrpc": "2.0",
  "method": "notifications/resources/list_changed"
}
```

---

## 5. 协议版本与演进

### 5.1 版本历史

| 版本 | 时间 | 关键变化 |
|------|------|----------|
| **2024-11-05** | 2024.11 | MCP 正式发布：三大原语、stdio、HTTP+SSE |
| **2025-03-26** | 2025.03 | Streamable HTTP、OAuth 2.0、JSON-RPC Batching |
| **2025-06-18** | 2025.06 | Elicitation、Tool Annotations、增强的 Tasks |
| **2025-07+** | 2025.07 | 协议趋于稳定，生态工具链爆发 |

### 5.2 向后兼容原则

```text
MCP 协议兼容策略：

① 协议版本协商：Client 提议版本 → Server 确认或建议升级
② 新字段必须可选：旧 Client 忽略未知字段，不报错
③ 能力协商避免误用：没声明的能力不会意外被调用
④ 废弃字段保留 6 个月：标注 @deprecated，保留实现
```

> 🎯 **核心要点**：MCP 本质上是 JSON-RPC 2.0 + 初始化协商 + 三大原语（Resources/Tools/Prompts）+ 两种传输方式。理解 Initialize 流程和 Capabilities 协商是写对 MCP 代码的前提

---

**下一模块**：[02 - Resources 资源原语详解](./02-Resources资源原语详解.md)  
**返回总览**：[00 - MCP 知识体系总览](./00-MCP知识体系总览.md)
