# Claude Code MCP 集成实战

> **核心摘要**：通过 MCP（Model Context Protocol）扩展 Claude Code 的能力边界，集成数据库查询、IDE 交互、框架文档查询等外部工具。

---

## 1. MCP 在 Claude Code 中的角色

```
Claude Code
    ↓ MCP Protocol
MCP Server A    MCP Server B    MCP Server C
(数据库查询)    (IDE 交互)      (框架文档)
    ↓               ↓               ↓
 MySQL / PG     VS Code API    Context7 API
```

## 2. MCP Server 配置

### 2.1 两种传输方式

| 方式 | 配置 | 适用场景 |
|------|------|---------|
| **stdio** | 指定 command + args，Claude Code 启动为子进程 | 本地工具 |
| **HTTP** | 指定 url，HTTP + SSE 连接 | 远程服务 |

### 2.2 配置示例

```json
{
  "mcpServers": {
    "database": {
      "type": "stdio",
      "command": "python",
      "args": ["-m", "mcp_server_database"],
      "env": {
        "DB_HOST": "localhost",
        "DB_PORT": "5432"
      }
    },
    "context7": {
      "type": "http",
      "url": "https://context7.com/mcp",
      "headers": {
        "Authorization": "Bearer ${CONTEXT7_API_KEY}"
      }
    }
  }
}
```

## 3. MCP Server 示例

### 3.1 Python 数据库查询 Server

```python
from mcp.server import Server
from mcp.server.stdio import stdio_server
import psycopg2, json

app = Server("database-server")

@app.tool()
async def query_database(sql: str, params: list = None) -> str:
    """执行 SELECT 查询。只允许 SELECT 语句。"""
    if not sql.strip().upper().startswith("SELECT"):
        return "错误：仅允许 SELECT 查询"
    conn = psycopg2.connect("postgresql://localhost/mydb")
    cursor = conn.cursor()
    cursor.execute(sql, params or [])
    rows = cursor.fetchall()
    columns = [desc[0] for desc in cursor.description]
    return json.dumps([dict(zip(columns, row)) for row in rows])

if __name__ == "__main__":
    import asyncio
    asyncio.run(stdio_server(app))
```

### 3.2 Node.js 业务 API Server

```javascript
const { Server } = require('@modelcontextprotocol/sdk/server/index.js');
const { StdioServerTransport } = require('@modelcontextprotocol/sdk/server/stdio.js');

const server = new Server({ name: 'business-api-server', version: '1.0.0' },
  { capabilities: { tools: {} } });

server.setRequestHandler('tools/list', async () => ({
  tools: [
    { name: 'get_order_status', description: '查询订单状态',
      inputSchema: { type: 'object', properties: { order_id: { type: 'string' } }, required: ['order_id'] } }
  ]
}));

const transport = new StdioServerTransport();
server.connect(transport);
```

## 4. 常用 MCP Server 生态

| MCP Server | 功能 | 配置方式 |
|-----------|------|---------|
| **Context7** | 查询最新框架文档 | HTTP，需 API Key |
| **GitHub** | Issue/PR/Repo 操作 | HTTP，需 Token |
| **PostgreSQL** | 数据库查询 | stdio，本地连接 |
| **Slack** | 消息/频道操作 | HTTP，需 Token |
| **Puppeteer** | 浏览器自动化 | stdio，本地进程 |
| **Docker** | 容器管理 | stdio，本地进程 |

## 核心要点回顾

- MCP = Claude Code 的能力扩展协议
- 两种模式：stdio（本地子进程）+ HTTP+SSE（远程服务）
- 工具命名规则：`mcp__{server_name}__{tool_name}`
- MCP 与 Hook 的区别：MCP 是工具扩展（给新能力），Hook 是事件自动化（触发脚本）
- 配置在 settings.json 的 `mcpServers` 中定义

## 参考资料

1. Anthropic 官方文档 - MCP Protocol
2. Model Context Protocol 规范 - modelcontextprotocol.io
3. Context7 文档 - context7.com
