# 02 - Resources 资源原语详解

> 🎯 Resources 是 MCP 的"只读数据层" — 让 LLM 以标准化方式访问文件、数据库、API 等结构化数据。URI 模板 + 内容类型 + 订阅机制 = 完整的数据接入方案

---

## 目录

1. [Resources 概念与架构](#1-resources-概念与架构)
2. [资源 URI 与模板系统](#2-资源-uri-与模板系统)
3. [资源内容类型](#3-资源内容类型)
4. [资源分页与增量读取](#4-资源分页与增量读取)
5. [资源订阅与变更通知](#5-资源订阅与变更通知)
6. [资源层级与目录遍历](#6-资源层级与目录遍历)

---

## 1. Resources 概念与架构

### 1.1 什么是 Resource

```text
Resource = LLM 可访问的只读数据单元

和 Tool 的本质区别：
  Resource：给 LLM "看"的（上下文注入）
  Tool：    给 LLM "用"的（执行操作）

示例 Resources：
  file:///docs/api-reference.md        → 文件内容
  postgres://db/schema/users           → 数据库表结构
  api://github/issues?repo=my/project   → API 查询结果
```

### 1.2 Resources API 总览

```text
┌──────────────────────────────────────────────────────────┐
│                  Resources API 完整列表                    │
├──────────────────────────────────────────────────────────┤
│                                                           │
│  核心操作：                                                │
│  ├── resources/list         列出所有可用资源               │
│  ├── resources/read         读取资源内容                   │
│  └── resources/templates/list  列出资源URI模板            │
│                                                           │
│  订阅机制：                                                │
│  ├── resources/subscribe    订阅资源变更                   │
│  └── resources/unsubscribe  取消订阅                       │
│                                                           │
│  通知（Server → Client）：                                 │
│  ├── notifications/resources/list_changed   列表变更       │
│  ├── notifications/resources/updated        内容更新       │
│  └── notifications/resources/updated        (带内容推送)   │
│                                                           │
└──────────────────────────────────────────────────────────┘
```

---

## 2. 资源 URI 与模板系统

### 2.1 资源 URI 规范

```text
MCP 资源 URI 格式：{scheme}://{authority}/{path}

支持的 scheme：
  file://    — 文件系统资源
  postgres:// — 数据库资源
  api://     — 外部 API 资源
  app://     — 应用内部资源
  custom://  — 自定义资源

示例：
  file:///home/user/docs/README.md
  postgres://prod-db/schema/public/users
  api://github/repos/myorg/myrepo/issues
```

### 2.2 URI 模板（动态资源）

```jsonc
// resources/templates/list 响应示例
{
  "resourceTemplates": [
    {
      "uriTemplate": "file:///src/{path}",
      "name": "源代码文件",
      "description": "读取项目中的任意源代码文件，path 为相对于 src/ 的路径",
      "mimeType": "text/plain",
      "annotations": {
        "audience": ["user", "assistant"],
        "priority": 0.8
      }
    },
    {
      "uriTemplate": "postgres://db/schema/{table}",
      "name": "数据表结构",
      "description": "查看数据库表结构定义，table 为表名",
      "mimeType": "application/json",
      "annotations": {
        "audience": ["assistant"],
        "priority": 0.5
      }
    },
    {
      "uriTemplate": "api://weather/{city}",
      "name": "城市天气",
      "description": "获取指定城市的天气信息",
      "mimeType": "application/json"
    }
  ]
}
```

```text
URI 模板变量替换：

  模板：    file:///src/{path}
  变量：    { path: "com/example/Main.java" }
  结果：    file:///src/com/example/Main.java

  模板：    api://weather/{city}?unit={unit}
  变量：    { city: "Beijing", unit: "celsius" }
  结果：    api://weather/Beijing?unit=celsius

LLM 使用流程：
  ① 调用 resources/templates/list 获取所有模板
  ② 看到 uriTemplate: "file:///src/{path}"
  ③ 构造 URI: "file:///src/com/example/Main.java"
  ④ 调用 resources/read 读取内容
```

### 2.3 资源 Annotations（元数据标注）

```jsonc
{
  "uri": "file:///docs/secret-design.md",
  "name": "机密设计文档",
  "annotations": {
    // audience: 谁能看到此资源
    "audience": ["assistant"],           // 只看 LLM 层，不通过界面向用户展示
    // priority: 资源推荐优先级 0.0-1.0
    "priority": 0.3,                     // 低优先级，通常不被主动推荐
    // 资源最后修改时间
    "lastModified": "2025-07-28T10:00:00Z",
    // 资源大小（字节）
    "size": 1048576,
    // 内容摘要
    "description": "包含敏感架构决策，仅在涉及安全设计时引用"
  }
}
```

---

## 3. 资源内容类型

### 3.1 文本内容 (TextContent)

```jsonc
// resources/read 响应 — 文本类型
{
  "contents": [
    {
      "uri": "file:///src/Main.java",
      "mimeType": "text/x-java",
      "text": "public class Main {\n    public static void main(String[] args) {\n        System.out.println(\"Hello\");\n    }\n}"
    }
  ]
}
```

### 3.2 二进制内容 (BlobContent)

```jsonc
// resources/read 响应 — 二进制类型
{
  "contents": [
    {
      "uri": "file:///assets/logo.png",
      "mimeType": "image/png",
      "blob": "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=="
    }
  ]
}
```

### 3.3 多内容资源（复合返回）

```jsonc
// 一个资源可以返回多个 content（如：代码 + AST + 注释）
{
  "contents": [
    {
      "uri": "file:///src/Main.java",
      "mimeType": "text/x-java",
      "text": "public class Main { ... }"
    },
    {
      "uri": "file:///src/Main.java#ast",
      "mimeType": "application/json",
      "text": "{\"type\": \"CompilationUnit\", \"types\": [...]}"
    },
    {
      "uri": "file:///src/Main.java#docs",
      "mimeType": "text/markdown",
      "text": "# Main.java\n主入口类..."
    }
  ]
}
```

### 3.4 MIME Type 最佳实践

| 场景 | 推荐 MIME Type | 说明 |
|------|---------------|------|
| 普通文本 | `text/plain` | 未知文本默认值 |
| 源代码 | `text/x-{lang}` | `text/x-java`, `text/x-python` |
| Markdown | `text/markdown` | 文档类 |
| JSON 数据 | `application/json` | 结构化数据 |
| YAML 配置 | `text/yaml` 或 `application/x-yaml` | 配置文件 |
| 图片 | `image/png`, `image/jpeg` | 二进制 |
| PDF | `application/pdf` | 文档 |
| HTML | `text/html` | 网页 |

---

## 4. 资源分页与增量读取

### 4.1 分页读取（大文件/大结果集）

```jsonc
// Client 请求 — 分页
{
  "jsonrpc": "2.0",
  "id": 42,
  "method": "resources/read",
  "params": {
    "uri": "file:///logs/app.log",
    "cursor": "line=500",    // 分页游标：从第 500 行开始
    "limit": 200             // 每页 200 行
  }
}

// Server 响应 — 含下一页游标
{
  "jsonrpc": "2.0",
  "id": 42,
  "result": {
    "contents": [
      {
        "uri": "file:///logs/app.log",
        "mimeType": "text/plain",
        "text": "[2025-07-29 10:00:01] INFO ...\n...共 200 行..."
      }
    ],
    "nextCursor": "line=700"   // 下一页游标
    // 如果 nextCursor 不存在或为 null，表示已到末尾
  }
}
```

### 4.2 资源内容摘要（先预览再决定是否完整读取）

```jsonc
// resources/list 响应可包含摘要
{
  "resources": [
    {
      "uri": "file:///logs/app.log",
      "name": "应用日志",
      "mimeType": "text/plain",
      "description": "应用完整日志文件（15MB）",  // 摘要信息
      "annotations": {
        "size": 15728640,        // 15MB
        "lastModified": "2025-07-29T10:30:00Z",
        "priority": 0.5
      }
    }
  ]
}
// LLM 可以据此判断：是大文件 → 使用分页读取；是敏感数据 → 仅必要时读取
```

---

## 5. 资源订阅与变更通知

### 5.1 订阅机制

```text
场景：LLM 关注某个配置文件的变化
      当文件被外部修改时，Server 主动通知 Client

流程：
  ① Client → Server: resources/subscribe { uri: "file:///config/app.yaml" }
  ② Server → Client: {}  (确认订阅)
  ③ ... 外部修改 app.yaml ...
  ④ Server → Client: notifications/resources/updated { uri: "..." }
  ⑤ Client → Server: resources/read { uri: "file:///config/app.yaml" }
```

```jsonc
// 订阅请求
{
  "jsonrpc": "2.0",
  "id": 10,
  "method": "resources/subscribe",
  "params": {
    "uri": "file:///config/app.yaml"
  }
}

// 变更通知（Server 主动推送）
{
  "jsonrpc": "2.0",
  "method": "notifications/resources/updated",
  "params": {
    "uri": "file:///config/app.yaml"
    // 可选：直接附带新内容
    // "content": { ... }
  }
}
```

### 5.2 订阅生命周期

```text
订阅状态管理：
  ├── 连接断开 → 所有订阅自动清除
  ├── Client 取消：resources/unsubscribe { uri }
  ├── 资源被删除 → Server 自动取消 + 通知
  └── 可选：订阅过期时间 TTL
```

---

## 6. 资源层级与目录遍历

### 6.1 目录结构暴露

```jsonc
// resources/list 支持层级展示
{
  "resources": [
    // 内容资源：可直接读取
    {
      "uri": "file:///project/README.md",
      "name": "README.md",
      "mimeType": "text/markdown",
      "annotations": { "size": 2048 }
    },
    // 目录资源：标记为目录类型
    {
      "uri": "file:///project/src/",
      "name": "src/",
      "mimeType": "application/x-directory",
      "description": "源代码根目录"
    },
    {
      "uri": "file:///project/src/com/example/",
      "name": "com/example/",
      "mimeType": "application/x-directory"
    }
  ]
}
```

### 6.2 按目录读取子资源

```jsonc
// 读取目录下的子资源列表（类似 ls）
{
  "jsonrpc": "2.0",
  "id": 20,
  "method": "resources/read",
  "params": {
    "uri": "file:///project/src/"
  }
}
// 返回该目录下的直接子资源
```

> 🎯 **核心要点**：Resources = URI 标识 + 模板参数化 + 文本/二进制多格式 + 分页加载 + 变更订阅。它是 MCP 的数据基石，让 LLM 以统一方式"看到"任意数据源

---

**上一模块**：[01 - MCP 协议规范与生命周期](./01-MCP协议规范与生命周期.md)  
**下一模块**：[03 - Tools 工具原语详解](./03-Tools工具原语详解.md)  
**返回总览**：[00 - MCP 知识体系总览](./00-MCP知识体系总览.md)
