# 09 - MCP Server 开发实战

> 🎯 MCP (Model Context Protocol) 是 2025 年最重要的 AI 协议——让 LLM 通过标准化接口调用外部工具和数据源。Node.js/TypeScript 是开发 MCP Server 的首选语言，本章提供从零到可用的完整教程

---

## 目录

1. [MCP 协议核心概念](#1-mcp-协议核心概念)
2. [第一个 MCP Server](#2-第一个-mcp-server)
3. [工具、资源、提示模板](#3-工具资源提示模板)
4. [实战：数据库查询 MCP Server](#4-实战数据库查询-mcp-server)

---

## 1. MCP 协议核心概念

```text
MCP = Model Context Protocol（Anthropic 提出，已成为行业标准）

架构：
┌──────────┐      MCP Protocol      ┌──────────────┐
│  Client  │ ←────────────────────→ │  MCP Server  │
│ (Claude) │    JSON-RPC over       │  (你的代码)   │
│          │    stdio / HTTP        │              │
└──────────┘                        └──────────────┘

Server 提供的三种能力：
├── Tools（工具）    → "查天气"、"发邮件" → 类似 Function Calling
├── Resources（资源） → 文件内容、数据库记录 → 数据源
└── Prompts（模板）   → 预定义的 Prompt 模板 → 快速调用
```

## 2. 第一个 MCP Server

```bash
npm install @modelcontextprotocol/sdk zod
npm install -D typescript @types/node
```

```typescript
// src/index.ts
import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { z } from "zod";

// 1. 创建 Server
const server = new McpServer({
    name: "my-first-mcp",
    version: "1.0.0"
});

// 2. 注册工具
server.tool(
    "get_weather",                         // 工具名
    "获取指定城市的天气",                    // 描述
    {                                      // 参数 Schema (Zod)
        city: z.string().describe("城市名，如 Beijing"),
        unit: z.enum(["celsius", "fahrenheit"]).default("celsius")
    },
    async ({ city, unit }) => {
        // 真实的工具逻辑
        const temp = await fetchWeather(city, unit);
        return {
            content: [{
                type: "text",
                text: `${city}: ${temp}°${unit === "celsius" ? "C" : "F"}`
            }]
        };
    }
);

// 3. 启动（stdio 传输）
const transport = new StdioServerTransport();
await server.connect(transport);
```

```json
// claude_desktop_config.json — 或 .mcp.json
{
  "mcpServers": {
    "my-first-mcp": {
      "command": "node",
      "args": ["dist/index.js"]
    }
  }
}
```

## 3. 工具、资源、提示模板

### 工具 (Tool)

```typescript
// 带枚举和复杂类型的工具
server.tool(
    "search_docs",
    "搜索内部文档",
    {
        query: z.string(),
        limit: z.number().min(1).max(50).default(10),
        category: z.enum(["api", "guide", "faq"]).optional(),
    },
    async ({ query, limit, category }) => {
        const results = await searchDocs(query, limit, category);
        return {
            content: results.map(r => ({
                type: "text",
                text: `📄 ${r.title}\n${r.snippet}`
            }))
        };
    }
);
```

### 资源 (Resource)

```typescript
// 暴露文件/数据给 LLM
server.resource(
    "config",                    // 资源 URI
    "config://app",              // 友好名
    async (uri) => ({
        contents: [{
            uri: uri.href,
            text: JSON.stringify(appConfig, null, 2),
            mimeType: "application/json"
        }]
    })
);
```

### 提示模板 (Prompt)

```typescript
// 预定义的 Prompt，方便用户快速调用
server.prompt(
    "code-review",
    "代码审查提示模板",
    { language: z.string().default("TypeScript") },
    ({ language }) => ({
        messages: [{
            role: "user",
            content: {
                type: "text",
                text: `请审查以下 ${language} 代码，找出潜在问题并用表格输出`
            }
        }]
    })
);
```

## 4. 实战：数据库查询 MCP Server

```typescript
import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { z } from "zod";
import mysql from "mysql2/promise";

const pool = mysql.createPool({
    host: "localhost",
    user: "root",
    database: "analytics"
});

const server = new McpServer({
    name: "analytics-mcp",
    version: "1.0.0"
});

// 工具 1：执行 SQL 查询（只读）
server.tool(
    "query_analytics",
    "查询分析数据（只读 SELECT）",
    {
        metric: z.enum(["dau", "revenue", "conversion"]),
        date_range: z.enum(["7d", "30d", "90d"]),
    },
    async ({ metric, date_range }) => {
        const sql = `
            SELECT date, value
            FROM metrics
            WHERE metric = ? AND date >= DATE_SUB(NOW(), INTERVAL ?)
            ORDER BY date
        `;
        const days = parseInt(date_range);
        const [rows] = await pool.query(sql, [metric, days]);

        return {
            content: [{
                type: "text",
                text: JSON.stringify(rows, null, 2)
            }]
        };
    }
);

const transport = new StdioServerTransport();
await server.connect(transport);
```

## 核心要点回顾

- MCP = JSON-RPC + stdio/HTTP，Server 提供 Tool/Resource/Prompt
- 工具定义 = Zod Schema（自动生成 JSON Schema 给 LLM）
- `StdioServerTransport` = 本地 MCP，`StreamableHTTPServerTransport` = 远程 MCP
- Claude Code / Cursor / Continue 都可以加载 MCP Server
- Node.js/TypeScript 是 MCP 开发首选（SDK 最成熟）

## 参考资料

1. MCP 官方文档 — modelcontextprotocol.io
2. MCP TypeScript SDK — github.com/modelcontextprotocol/typescript-sdk
