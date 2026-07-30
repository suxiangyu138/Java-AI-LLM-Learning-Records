# 08 - MCP 高级特性与生态

> 🎯 掌握 Sampling、Elicitation、Roots、Multi-Server 编排等高级特性，以及 MCP 的完整生态版图

---

## 目录

1. [Sampling — Server 请求 LLM 生成](#1-sampling--server-请求-llm-生成)
2. [Elicitation — 人机交互式信息获取](#2-elicitation--人机交互式信息获取)
3. [Roots — 文件系统根目录管理](#3-roots--文件系统根目录管理)
4. [Multi-Server 编排](#4-multi-server-编排)
5. [MCP 生态系统](#5-mcp-生态系统)
6. [MCP 未来演进方向](#6-mcp-未来演进方向)

---

## 1. Sampling — Server 请求 LLM 生成

### 1.1 什么是 Sampling

```text
Sampling = Server → Client 方向的能力
即：MCP Server 主动请求 Host 侧的 LLM 生成内容

  普通流程（Client 主导）：
    Client → Server: tools/call
    Server → Client: result

  Sampling 流程（Server 主导）：
    Client → Server: tools/call
    Server → Client: sampling/createMessage（请 LLM 帮我生成...）
    Client → Server: 生成的文本
    Server → Client: tools/call result（结合 LLM 生成的结果）

使用场景：
  • Agent 委派子任务："Server 执行过程中需要 LLM 辅助决策"
  • 内容增强：Server 获取原始数据 → 请求 LLM 润色 → 返回
  • 多步推理：Server 执行分析 → 请求 LLM 推理 → 继续分析
```

### 1.2 Sampling 流程

```text
  Client (Host + LLM)                         Server (Tool Provider)
       │                                             │
       │ ① tools/call { name: "analyze_report" }      │
       │─────────────────────────────────────────────►│
       │                                             │
       │                      ② Server 需要 LLM 帮助   │
       │                    sampling/createMessage     │
       │◄─────────────────────────────────────────────│
       │  {                                          │
       │    "messages": [{                            │
       │      "role": "user",                         │
       │      "content": "分析以下数据并给出建议..."     │
       │    }],                                       │
       │    "maxTokens": 1000                        │
       │  }                                          │
       │                                             │
       │ ③ Client 调用 LLM 生成                       │
       │ ④ 返回生成结果                                │
       │─────────────────────────────────────────────►│
       │  { "content": "根据数据分析，建议..." }        │
       │                                             │
       │ ⑤ Server 结合 LLM 输出，继续处理              │
       │◄─────────────────────────────────────────────│
       │  { "result": "完整分析报告..." }               │
```

### 1.3 Sampling 代码示例

```python
"""
MCP Server 使用 Sampling 请求 LLM 辅助
"""
from fastmcp import FastMCP
from fastmcp.sampling import SamplingRequest
from fastmcp.exceptions import ToolError

mcp = FastMCP(name="report-server")


@mcp.tool(name="generate_report")
async def generate_report(topic: str) -> str:
    """生成深度分析报告 — 利用 Sampling 调用 Host LLM"""

    # Step 1: 获取原始数据
    raw_data = await data_service.fetch_data(topic)

    # Step 2: 请求 Host LLM 分析数据
    sampling_result = await mcp.request_sampling(
        SamplingRequest(
            messages=[
                {
                    "role": "user",
                    "content": f"""请分析以下 {topic} 相关的数据，提取 3-5 个关键趋势：

原始数据：
{raw_data}

请以 JSON 格式返回：
{{
  "trends": [
    {{ "name": "趋势名称", "data": "相关数据", "significance": "重要性" }}
  ],
  "summary": "一句话总结"
}}"""
                }
            ],
            max_tokens=2000,
            temperature=0.3,       # 低温度 = 更确定性的分析
        )
    )

    # Step 3: 解析 LLM 输出，格式化报告
    import json
    analysis = json.loads(sampling_result.content)

    # Step 4: 生成最终报告
    report = f"""# {topic} 深度分析报告

## 关键趋势

"""
    for i, trend in enumerate(analysis["trends"], 1):
        report += f"### {i}. {trend['name']}\n"
        report += f"**数据**：{trend['data']}\n"
        report += f"**重要性**：{trend['significance']}\n\n"

    report += f"## 总结\n{analysis['summary']}"

    return report
```

### 1.4 Sampling 的安全考虑

```text
Sampling 的安全挑战：

  ⚠️ Token 消耗：Server 可以消耗 Host 的 LLM Token
    → 防御：maxTokens 限制 + 速率限制

  ⚠️ Prompt 注入：恶意 Server 利用 Sampling 绕过安全策略
    → 防御：Client 审查 Sampling 请求的 messages

  ⚠️ 递归风险：Sampling 结果可能触发新的 Tool 调用
    → 防御：限制 Sampling 深度（默认 max 1 层）

  ⚠️ 隐私泄露：Server 将用户数据发给 Host LLM
    → 防御：数据分类 + Host 侧敏感数据拦截
```

---

## 2. Elicitation — 人机交互式信息获取

### 2.1 什么是 Elicitation

```text
Elicitation（2025-06 Spec）= Server 主动向用户请求信息

与 Sampling 的区别：
  Sampling：Server → LLM（让 AI 帮忙生成）
  Elicitation：Server → User（让人来提供信息）

使用场景：
  • 表单填写：Server 需要用户提供额外的结构化信息
  • 确认对话框：高风险操作前需要用户明确确认
  • 选择器：让用户从选项中选择
```

### 2.2 Elicitation 交互模式

```jsonc
// Server → Client: elicitation/request
{
  "jsonrpc": "2.0",
  "id": 100,
  "method": "elicitation/request",
  "params": {
    "message": "请选择要部署的环境：",
    "mode": "form",           // form | confirm | select
    "schema": {
      "type": "object",
      "properties": {
        "environment": {
          "type": "string",
          "enum": ["staging", "production"],
          "description": "目标环境"
        },
        "version": {
          "type": "string",
          "description": "部署版本号"
        },
        "skip_tests": {
          "type": "boolean",
          "description": "跳过测试（⚠️ 不推荐）",
          "default": false
        }
      },
      "required": ["environment", "version"]
    }
  }
}

// Client → Server: 用户的表单响应
{
  "jsonrpc": "2.0",
  "id": 100,
  "result": {
    "action": "submit",       // submit | cancel
    "formData": {
      "environment": "production",
      "version": "v2.1.0",
      "skip_tests": false
    }
  }
}
```

### 2.3 Elicitation 模式对比

| 模式 | UI 表现 | 适用场景 | 示例 |
|------|---------|----------|------|
| **form** | 表单填写 | 需要多个字段的结构化输入 | 部署配置、创建资源 |
| **confirm** | 确认按钮 | 二元选择（是/否） | 删除确认、发布确认 |
| **select** | 下拉/单选 | 从预定义选项中选择 | 选择环境、选择策略 |

---

## 3. Roots — 文件系统根目录管理

### 3.1 什么是 Roots

```text
Roots = Client 向 Server 声明的文件系统根目录

问题：MCP Server 需要访问文件系统，但不知道哪些目录是允许的
解决：Client 通过 roots/list 告诉 Server 可以访问哪些目录

  类比：chroot — 限制进程的文件系统可见范围

使用场景：
  • Claude Desktop 打开项目文件夹 → 将项目根目录声明为 Root
  • IDE 打开工作区 → 将工作区目录声明为 Roots
  • 多项目场景 → 声明多个 Roots
```

### 3.2 Roots 交互

```jsonc
// === Client → Server: roots/list 响应（Server 主动查询） ===
{
  "roots": [
    {
      "uri": "file:///Users/alex/projects/myapp",
      "name": "myapp",
      "description": "主项目目录"
    },
    {
      "uri": "file:///Users/alex/projects/shared-lib",
      "name": "shared-lib",
      "description": "共享库项目"
    }
  ]
}

// === Client → Server: Roots 变更通知 ===
{
  "jsonrpc": "2.0",
  "method": "notifications/roots/list_changed"
  // Server 收到后重新请求 roots/list
}
```

### 3.3 Server 端使用 Roots

```python
@mcp.tool(name="search_code")
async def search_code(query: str):
    """在允许的目录范围内搜索代码"""

    # 获取 Client 声明的 Roots
    roots = await mcp.request_roots()

    results = []
    for root in roots:
        root_path = root.uri.replace("file://", "")
        # 只在允许的目录内搜索
        for file in Path(root_path).rglob("*.py"):
            if query.lower() in file.read_text().lower():
                results.append(str(file.relative_to(root_path)))

    return "\n".join(results) if results else "未找到匹配结果"
```

---

## 4. Multi-Server 编排

### 4.1 编排模式

```text
┌──────────────────────────────────────────────────────────────┐
│               Multi-Server 编排三大模式                       │
├──────────────────────────────────────────────────────────────┤
│                                                               │
│  模式一：聚合模式 (Aggregation)                                │
│    Client 同时连接多个独立 Server                              │
│    LLM 自动选择合适的 Server 的工具                            │
│                                                               │
│    ┌────────┐  ┌────────┐  ┌────────┐                       │
│    │Weather │  │GitHub  │  │Database│                       │
│    │Server  │  │Server  │  │Server  │                       │
│    └───┬────┘  └───┬────┘  └───┬────┘                       │
│        │           │           │                              │
│        └───────────┼───────────┘                              │
│                    │                                          │
│              ┌─────┴─────┐                                   │
│              │   Client  │ → LLM 自动路由                     │
│              └───────────┘                                   │
│                                                               │
│  模式二：链式模式 (Chaining)                                   │
│    一个 Server 的 Tool 调用结果作为另一个 Server 的输入         │
│                                                               │
│    [GitHub] → git_diff → [Reviewer] → findings → [Slack]     │
│                                                               │
│  模式三：网关模式 (Gateway)                                    │
│    一个 MCP Gateway 代理多个后端 Server                        │
│    Client 只连接 Gateway，Gateway 路由到实际 Server            │
│                                                               │
│    ┌────────┐                                                │
│    │ Client │──→ ┌───────────┐ ──→ Weather Server            │
│    └────────┘    │   MCP     │ ──→ GitHub Server             │
│                  │  Gateway  │ ──→ Database Server            │
│                  └───────────┘ ──→ Custom Server              │
└──────────────────────────────────────────────────────────────┘
```

### 4.2 MCP Gateway 实现

```python
"""
MCP Gateway — 统一代理多个 MCP Server
功能：工具聚合、认证统一、限流统一、审计统一
"""
from fastapi import FastAPI, Request, Depends
from fastapi.responses import StreamingResponse
import httpx

app = FastAPI(title="MCP Gateway")

# 后端 Server 注册表
BACKENDS = {
    "weather":  {"url": "http://weather-server:8080/mcp",  "auth": True},
    "github":   {"url": "http://github-server:8080/mcp",   "auth": True},
    "database": {"url": "http://database-server:8080/mcp", "auth": True},
}


@app.post("/mcp")
async def mcp_proxy(request: Request, user = Depends(authenticate)):
    """MCP 请求代理"""
    body = await request.json()
    method = body.get("method")

    # ===== 特殊方法：tools/list — 聚合所有 Server 的工具 =====
    if method == "tools/list":
        all_tools = []
        async with httpx.AsyncClient() as client:
            for name, backend in BACKENDS.items():
                # 并行请求各 Server
                resp = await client.post(
                    backend["url"],
                    json={"jsonrpc": "2.0", "id": 1, "method": "tools/list"}
                )
                data = resp.json()
                for tool in data.get("result", {}).get("tools", []):
                    # 加前缀避免冲突
                    tool["name"] = f"{name}__{tool['name']}"
                    tool["description"] = f"{tool['description']}\n[来源: {name}]"
                    all_tools.append(tool)

        return {"jsonrpc": "2.0", "id": body["id"], "result": {"tools": all_tools}}

    # ===== tools/call — 路由到正确的 Server =====
    if method == "tools/call":
        tool_name = body["params"]["name"]
        # 从工具名中提取 Server 名称
        server_name, original_name = tool_name.split("__", 1)

        backend = BACKENDS.get(server_name)
        if not backend:
            return jsonrpc_error(body["id"], -32601, f"未知 Server: {server_name}")

        # 转发调用
        body["params"]["name"] = original_name

        async with httpx.AsyncClient() as client:
            resp = await client.post(
                backend["url"],
                json=body,
                timeout=60
            )
            return resp.json()

    # ===== 其他方法：转发到默认 Server =====
    async with httpx.AsyncClient() as client:
        resp = await client.post(
            BACKENDS["weather"]["url"],
            json=body
        )
        return resp.json()
```

---

## 5. MCP 生态系统

### 5.1 生态版图

```text
┌──────────────────────────────────────────────────────────────┐
│                     MCP 生态全景                              │
├──────────────────────────────────────────────────────────────┤
│                                                               │
│  📦 官方 Server（@modelcontextprotocol/server-*）              │
│  ├── server-filesystem    文件系统操作                        │
│  ├── server-github        GitHub API 集成                     │
│  ├── server-postgres      数据库查询                          │
│  ├── server-slack         Slack 消息                          │
│  ├── server-brave-search  互联网搜索                          │
│  ├── server-google-maps   地图服务                            │
│  ├── server-memory        持久化记忆                          │
│  └── server-puppeteer     浏览器自动化                        │
│                                                               │
│  🛠️ 开发工具                                                  │
│  ├── MCP Inspector        调试/可视化测试                     │
│  ├── MCP CLI              命令行管理工具                      │
│  └── MCP Registry         社区 Server 注册中心                │
│                                                               │
│  🏢 平台集成                                                  │
│  ├── Claude Desktop       原生 MCP 支持（Anthropic）          │
│  ├── Claude Code CLI      原生 MCP 支持                       │
│  ├── VS Code / Cursor     通过插件支持                        │
│  ├── JetBrains IDEs       通过插件支持                        │
│  ├── LangChain            社区适配                            │
│  ├── Spring AI            内置 MCP Client/Server             │
│  └── Dify / Coze          低代码平台集成                      │
│                                                               │
│  🌐 市场与社区                                                │
│  ├── mcp.so               MCP Server 搜索引擎                 │
│  ├── github.com/modelcontextprotocol/servers  官方仓库        │
│  ├── smithery.ai          MCP Server 托管平台                 │
│  └── open-mcp.org         社区驱动的开放市场                  │
│                                                               │
└──────────────────────────────────────────────────────────────┘
```

### 5.2 MCP 相关 SDK

| 语言 | SDK | 特点 |
|------|-----|------|
| **TypeScript** | `@modelcontextprotocol/sdk` | 官方 SDK、最完整 |
| **Python** | `mcp` + `fastmcp` | 官方 SDK + 高级封装 |
| **Java** | `spring-ai-mcp` | Spring 生态集成 |
| **Kotlin** | `io.modelcontextprotocol:kotlin-sdk` | 官方 Kotlin SDK |
| **Go** | 社区实现 | `mark3labs/mcp-go` |
| **Rust** | 社区实现 | `phatblat/mcp-rs` |
| **C#/.NET** | `ModelContextProtocol` NuGet | 官方 .NET SDK |

---

## 6. MCP 未来演进方向

### 6.1 协议路线图

```text
2025 已发布：
  ✅ Streamable HTTP 传输
  ✅ OAuth 2.0 认证标准
  ✅ Tool Annotations (readOnlyHint/destructiveHint/idempotentHint)
  ✅ Elicitation 人机交互
  ✅ JSON-RPC Batching

2025 进行中：
  🔄 Tasks API — 长时间运行任务的标准化管理
  🔄 Dynamic Tool Registration — 运行时注册/注销工具
  🔄 Server-to-Server — MCP Server 之间直接通信
  🔄 Namespace — 官方命名空间管理，防工具名冲突
  🔄 WebTransport — 基于 QUIC 的新传输方式

未来方向：
  🔮 Marketplace API — 标准化的 Server 发现与安装
  🔮 Federation — 跨组织 MCP Server 信任联邦
  🔮 Agent-as-MCP-Server — Agent 本身作为 MCP Server 暴露
  🔮 Edge MCP — 边缘计算 + MCP 集成
```

### 6.2 MCP vs A2A（Google）

```text
MCP (Anthropic) vs A2A (Google Agent-to-Agent)：

┌────────────────┬──────────────────┬──────────────────┐
│ 维度            │ MCP               │ A2A (Google)      │
├────────────────┼──────────────────┼──────────────────┤
│ 定位            │ Tool/Data 协议    │ Agent 协作协议     │
│ 方向            │ Client → Server  │ Agent ↔ Agent    │
│ 核心概念         │ Resources/Tools/ │ Task/Message/    │
│                │ Prompts           │ Artifact          │
│ 传输            │ stdio/HTTP/SSE    │ HTTP/gRPC        │
│ 提出者          │ Anthropic 2024   │ Google 2025      │
│ 互补关系         │                    │                  │
│ MCP             │ Agent 使用 MCP 连接到工具和数据         │
│ A2A             │ 多个 Agent 通过 A2A 相互协作           │
│ 组合            │ Agent A (MCP→Tools) ←A2A→ Agent B      │
└────────────────┴──────────────────┴──────────────────┘

不是竞争关系，而是互补：
  MCP = Agent 的"手"（执行工具、获取数据）
  A2A = Agent 的"嘴"（与其他 Agent 沟通协调）
```

> 🎯 **核心要点**：Sampling 让 Server 反向调用 LLM、Elicitation 让人机交互标准化、Roots 限制文件访问边界、Gateway 统一多 Server 入口。MCP 生态已覆盖主流语言和平台，正从工具协议向 Agent 基础设施演进

---

**上一模块**：[07 - MCP 认证与安全](./07-MCP认证与安全.md)  
**返回总览**：[00 - MCP 知识体系总览](./00-MCP知识体系总览.md)
