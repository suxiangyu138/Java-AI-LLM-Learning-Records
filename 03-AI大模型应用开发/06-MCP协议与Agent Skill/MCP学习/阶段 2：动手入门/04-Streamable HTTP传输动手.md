# 04 Streamable HTTP 传输动手

> 把 Server 从"本地子进程"升级为"远程 HTTP 服务"：fastmcp 一行切换传输、curl 手测无状态接口、HTTP Client 编程调用——体验 2026-07-28 规范的无状态革命。

## 📚 目录

1. [为什么需要 HTTP 传输](#1-为什么需要-http-传输)
2. [Server 切换为 HTTP 模式](#2-server-切换为-http-模式)
3. [curl 手测：无状态接口](#3-curl-手测无状态接口)
4. [HTTP Client 编程调用](#4-http-client-编程调用)
5. [无状态的意义：从代码看规范](#5-无状态的意义从代码看规范)
6. [易错点与规范](#6-易错点与规范)

## 1. 为什么需要 HTTP 传输

| 维度 | stdio | Streamable HTTP |
|---|---|---|
| 范围 | 仅本机 | 任意可访问主机 |
| 部署 | 客户端启动子进程 | 独立服务（docker/k8s/serverless） |
| 状态 | 进程内 | **无状态**（2026-07-28 规范核心） |
| 水平扩展 | 不可 | 轻松（无粘性会话） |
| 客户端 | 本机工具 | 任意语言 HTTP 调用 |

**一句话**：stdio 是"调试与本地工具"模式，HTTP 是"生产与远程"模式——同一份工具代码，传输层一换即可。

## 2. Server 切换为 HTTP 模式

```python
# server.py —— 02 篇的 Server，仅改最后一行
from fastmcp import FastMCP
from datetime import datetime

mcp = FastMCP("demo-server")

@mcp.tool
def get_current_time(timezone: str = "UTC") -> str:
    """返回指定时区的当前时间（IANA 时区名）。"""
    return datetime.now().isoformat()

@mcp.tool
def add(a: int, b: int) -> int:
    """两数相加。"""
    return a + b

if __name__ == "__main__":
    # 关键一行：transport 从默认 stdio 切到 HTTP
    mcp.run(transport="http", host="127.0.0.1", port=8080)
```

```bash
uv run python server.py
# 输出示例：
# INFO:     Started server process
# Uvicorn running on http://127.0.0.1:8080/mcp
```

**发生了什么**：fastmcp 内置 uvicorn 起了一个 HTTP 服务，统一端点 `http://127.0.0.1:8080/mcp` 接收所有 MCP 请求。

> 💡 fastmcp 3.x 中 transport 参数名为 `"http"`（兼容旧写法 `"streamable-http"`）；老版本 2.x 写法是 `mcp.run(transport="streamable-http")`——教程混用很正常，认准 3.x 写法。

## 3. curl 手测：无状态接口

**tools/list（发现工具）**：

```bash
curl -s -X POST http://127.0.0.1:8080/mcp \
  -H "Content-Type: application/json" \
  -H "Accept: application/json, text/event-stream" \
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}'
```

**tools/call（调用工具）**：

```bash
curl -s -X POST http://127.0.0.1:8080/mcp \
  -H "Content-Type: application/json" \
  -H "Accept: application/json, text/event-stream" \
  -d '{"jsonrpc":"2.0","id":2,"method":"tools/call","params":{"name":"add","arguments":{"a":3,"b":4}}}'
```

**预期响应**（JSON 或 SSE 格式，取决于 Accept）：

```json
{"jsonrpc":"2.0","id":2,"result":{"content":[{"type":"text","text":"7"}]}}
```

**2026-07 规范的关键观察（对比 v1）**：

| 观察点 | v1（2025-11） | v2（2026-07-28） |
|---|---|---|
| 请求头 | Mcp-Session-Id 会话头 | **无会话头**，`Mcp-Method`/`Mcp-Name` 标准头（可选） |
| 握手 | 先 POST initialize 才能用 | **无需握手**，直接发请求 |
| 协商 | 会话级 | 每请求 `_meta` 携带版本/能力 |
| 连接方式 | 同一会话内多次 POST | **每次 POST 独立**（无状态） |

**你亲手验证了什么**：连续两次 curl 之间没有任何会话状态——这就是无状态。Server 崩了重启，客户端重发请求即可，无需重建会话。

## 4. HTTP Client 编程调用

```python
# http_client.py —— 官方 SDK 的 streamable_http_client
import asyncio
from mcp import ClientSession
from mcp.client.streamable_http import streamable_http_client

async def main():
    async with streamable_http_client(
        url="http://127.0.0.1:8080/mcp",
        headers={"Authorization": "Bearer <token>"},  # 生产必配，本阶段可空
    ) as (read, write):
        async with ClientSession(read, write) as session:
            tools = await session.list_tools()
            print("远程工具:", [t.name for t in tools.tools])

            result = await session.call_tool("get_current_time", {"timezone": "Asia/Shanghai"})
            print("远程调用结果:", result.content[0].text)

if __name__ == "__main__":
    asyncio.run(main())
```

```bash
uv run python http_client.py   # 需先启动 HTTP Server
```

**与 stdio Client 的差异**：连接方式从 `stdio_client` 换成 `streamable_http_client(url)`，其余 API 完全一样——**协议统一带来的便利：Client 代码只差一行传输层**。

## 5. 无状态的意义：从代码看规范

```text
传统（有状态）：
  Client ──握手──> Server（记住会话）
  Client ──请求──> Server（依赖会话）        ← 会话丢失=请求失败
  Server 重启 → 所有客户端会话失效

2026-07 无状态：
  Client ──请求──> Server（独立处理）        ← 每请求自包含
  Client ──请求──> Server
  Server 扩容/重启 → 客户端无感知
```

**工程收益**：

1. **水平扩展**：负载均衡随便加实例，无粘性会话。
2. **serverless 友好**：函数冷启动也无妨，请求级协商。
3. **代理简单**：网关/反代无需维护会话映射。
4. **运维统一**：标准 REST 语义，监控告警模板直接复用。

> ⚠️ 需要跨调用状态的业务怎么办？规范答案（SEP-2567）：**状态作为普通工具参数显式传递**——Server 签发句柄，客户端在后续调用中传入。会话状态从协议层搬到了业务层。

## 6. 易错点与规范

| 易错点 | 现象 | 正确做法 |
|---|---|---|
| 忘带 `Accept: application/json, text/event-stream` | 某些 SDK 返回非预期格式 | curl 手测必带（Server 可能回 SSE） |
| 绑定 `0.0.0.0` 无认证 | 内网裸奔 | 本阶段 `127.0.0.1`；生产加认证（阶段 3） |
| 端口占用 | Uvicorn 启动报错 | 换端口或 `lsof -i :8080` 排查 |
| 防火墙/容器网络 | 客户端连不上 | 检查监听地址与端口映射 |
| 用 SSE 旧端点 | 404 | 2026-07 规范已废弃 HTTP+SSE 双端点，统一 `/mcp` |
| v1 SDK 连 v2 端点 | 握手/协商异常 | 版本对齐：SDK v1 配 legacy 模式或升级（01 篇） |

**动手规范**：

1. 本阶段 HTTP 仅本机验证，**不要绑定 0.0.0.0 且不带认证**（安全细节阶段 3 讲）。
2. curl 是 HTTP Server 的第一测试工具——Client 连不上时先 curl 定位"是 Server 问题还是 Client 问题"。
3. 改代码后重启 Server 进程，观察 Uvicorn 日志确认新版本已加载。

> 🎯 **核心要点**：Streamable HTTP 动手的核心体验是"**无状态**"——同一份工具代码，传输从 stdio 换到 HTTP 只改一行；curl 两次独立请求验证了无会话；Client 代码同样只换传输层。2026-07-28 规范把 MCP 从"带会话的进程协议"变成"标准 REST 服务"——这是它 2026 年被企业广泛采用的工程基础。

---

**参考来源**：
- [MCP Server 开发完全教程：FastMCP 3.0 + TypeScript SDK 从零跑通](https://ofox.ai/zh/blog/mcp-server-development-chinese-complete-guide/)
- [MCP SDK Betas: What to Do Before July 28](https://byteiota.com/mcp-sdk-betas-migration-guide-2026-07-28/)

---

**下一模块**：[05-Inspector调试与测试](05-Inspector调试与测试.md) / **返回总览**：[00-阶段总览与学习路径](00-阶段总览与学习路径.md)
