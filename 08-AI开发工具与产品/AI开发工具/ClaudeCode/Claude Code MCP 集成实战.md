# Claude Code MCP 集成实战（实战版）

> **文档定位**：Claude Code CLI MCP 集成指南
> **核心问题**：如何通过 MCP 扩展 Claude Code 的能力？集成数据库、IDE、外部 API？

---

## 一、MCP 在 Claude Code 中的角色

Claude Code 通过 MCP（Model Context Protocol）接入外部工具和数据源：

```
Claude Code
    ↓ MCP Protocol
MCP Server A    MCP Server B    MCP Server C
(数据库查询)    (IDE 交互)      (框架文档)
    ↓               ↓               ↓
 MySQL / PG     VS Code API    Context7 API
```

---

## 二、MCP Server 配置

### 2.1 在 settings.json 中配置

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
    },
    "ide": {
      "type": "stdio",
      "command": "node",
      "args": [".mcp/ide-server.js"]
    }
  }
}
```

### 2.2 两种传输方式

| 方式 | 配置 | 适用场景 |
|---|---|---|
| **stdio** | 指定 command + args，Claude Code 启动为子进程 | 本地工具 |
| **HTTP** | 指定 url，HTTP + SSE 连接 | 远程服务 |

---

## 三、MCP Server 示例

### 3.1 Python MCP Server（数据库查询）

```python
# mcp_server_database.py
from mcp.server import Server
from mcp.server.stdio import stdio_server
import psycopg2

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

### 3.2 Node.js MCP Server（业务 API）

```javascript
// mcp_api_server.js
const { Server } = require('@modelcontextprotocol/sdk/server/index.js');
const { StdioServerTransport } = require('@modelcontextprotocol/sdk/server/stdio.js');

const server = new Server({
  name: 'business-api-server',
  version: '1.0.0'
}, {
  capabilities: { tools: {} }
});

server.setRequestHandler('tools/list', async () => ({
  tools: [
    {
      name: 'get_order_status',
      description: '查询订单状态',
      inputSchema: {
        type: 'object',
        properties: {
          order_id: { type: 'string', description: '订单ID' }
        },
        required: ['order_id']
      }
    },
    {
      name: 'get_user_info',
      description: '查询用户信息',
      inputSchema: {
        type: 'object',
        properties: {
          user_id: { type: 'string' }
        },
        required: ['user_id']
      }
    }
  ]
}));

server.setRequestHandler('tools/call', async (request) => {
  const { name, arguments: args } = request.params;
  
  if (name === 'get_order_status') {
    const order = await fetchOrderFromAPI(args.order_id);
    return { content: [{ type: 'text', text: JSON.stringify(order) }] };
  }
  
  if (name === 'get_user_info') {
    const user = await fetchUserFromDB(args.user_id);
    return { content: [{ type: 'text', text: JSON.stringify(user) }] };
  }
  
  throw new Error(`Unknown tool: ${name}`);
});

const transport = new StdioServerTransport();
server.connect(transport);
```

---

## 四、MCP 工具在 Claude Code 中的使用

配置好 MCP Server 后，工具自动以 `mcp__{server_name}__{tool_name}` 的形式出现：

```
Claude Code 中可以直接调用：
  mcp__database__query_database(sql="SELECT * FROM users WHERE id = ?", params=[1])
  mcp__business_api__get_order_status(order_id="ORD-2024-001")
  mcp__context7__query-docs(libraryId="/spring-projects/spring-boot", query="如何配置数据源")
```

---

## 五、常见 MCP Server 生态

| MCP Server | 功能 | 配置 |
|---|---|---|
| **Context7** | 查询最新框架文档 | HTTP，需 API Key |
| **GitHub** | Issue/PR/Repo 操作 | HTTP，需 Token |
| **Filesystem** | 安全文件操作 | stdio，本地进程 |
| **PostgreSQL** | 数据库查询 | stdio，本地连接 |
| **Slack** | 消息/频道操作 | HTTP，需 Token |
| **Puppeteer** | 浏览器自动化 | stdio，本地进程 |
| **Docker** | 容器管理 | stdio，本地进程 |

---

## 六、MCP 配置最佳实践

```json
{
  "mcpServers": {
    "_comment": "生产环境 MCP Server 配置",
    
    "context7": {
      "type": "http",
      "url": "https://mcp.context7.com/mcp",
      "headers": {
        "CONTEXT7_API_KEY": "${CONTEXT7_API_KEY}"
      }
    },
    
    "postgres": {
      "type": "stdio",
      "command": "npx",
      "args": ["-y", "@anthropic/mcp-server-postgres", "${DATABASE_URL}"]
    },
    
    "github": {
      "type": "http",
      "url": "https://api.github.com/mcp",
      "headers": {
        "Authorization": "Bearer ${GITHUB_TOKEN}",
        "Accept": "application/vnd.github+json"
      }
    }
  }
}
```

---

## 七、核心要点

1. **MCP 在 Claude Code 中的定位？** 通过标准协议扩展 Claude Code 的能力边界
2. **两种传输方式？** stdio（本地子进程）和 HTTP+SSE（远程服务）
3. **工具命名规则？** `mcp__{server_name}__{tool_name}`
4. **如何配置环境变量？** settings.json 的 mcpServers 中可配置 env 和 headers
5. **MCP 和 Hook 有什么区别？** MCP 是工具扩展（给 Claude Code 新能力），Hook 是事件自动化（在已有能力上触发脚本）

---

## 八、极简总结

```
MCP = Claude Code 的能力扩展协议
两种模式 = stdio(本地) + HTTP(远程)
配置 = settings.json → mcpServers → {server_name: {type, command/url}}
工具名 = mcp__服务器名__工具名
生态 = Context7(文档)、GitHub(仓库)、PostgreSQL(数据库)、Slack(消息)
```
