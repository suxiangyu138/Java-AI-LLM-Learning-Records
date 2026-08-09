# 05 Inspector 调试与测试

> MCP 官方调试工具实战：一条命令启动、三种传输连接、UI 五步调试流程、CLI 冒烟测试进 CI——把"能跑"变成"可验证"。

## 📚 目录

1. [Inspector 是什么](#1-inspector-是什么)
2. [快速启动与连接](#2-快速启动与连接)
3. [UI 五步调试流程](#3-ui-五步调试流程)
4. [CLI 模式与 CI 冒烟测试](#4-cli-模式与-ci-冒烟测试)
5. [导出配置到客户端](#5-导出配置到客户端)
6. [已知问题与避坑](#6-已知问题与避坑)

## 1. Inspector 是什么

```text
浏览器 UI（React，端口 6274）
   │
   ▼
Inspector Proxy（Node 代理，端口 6277）—— 扮演 MCP Client
   │
   ▼
你的 Server（stdio 子进程 / HTTP 远程）
```

- **定位**：MCP 的"Postman"——可视化浏览工具、手动调用、查看 JSON-RPC 请求/响应与耗时。
- **组成**：React 网页 UI（Client）+ Node 代理（协议桥接），无需克隆仓库。
- **能力**：tools/resources/prompts 三页签、输入 Schema 查看、手动执行、日志查看。
- **局限（重要）**：Inspector 看不到 Claude Code 等真实客户端对 Server 的调用——它只记录自己浏览器会话的流量；真实客户端的调用需要其他手段（06 篇）。

## 2. 快速启动与连接

```bash
# 一条命令启动（自动打开浏览器）
npx @modelcontextprotocol/inspector

# 启动后直接连接本地 stdio Server
npx @modelcontextprotocol/inspector uv -- run python server.py
```

**端口**：UI = 6274，Proxy = 6277。

**三种连接方式**：

| Transport Type | 场景 | 示例输入 |
|---|---|---|
| STDIO | 本地开发（推荐） | Command: `uv`，Args: `run python server.py` |
| SSE | 旧版远程（2026-07 已弃用） | `http://localhost:3000/sse` |
| Streamable HTTP | 新版远程（推荐） | `http://localhost:3000/mcp` |

**连接要点**：环境变量可以在这里配置（如 API Key）；连接后左侧出现 Tools/Resources/Prompts 三个页签。

## 3. UI 五步调试流程

| 步骤 | 操作 | 验证目标 |
|---|---|---|
| 1 | Tools 页签 → `tools/list` | 工具是否注册成功、Schema 是否完整 |
| 2 | 选中工具 → 填最小参数 → 调用 | happy path 通过 |
| 3 | 故意传错参数（类型错/缺必填） | 错误信息是否可读（而不是裸异常） |
| 4 | Resources / Prompts 页签浏览 | 资源与提示注册是否完整 |
| 5 | 导出配置 → 接入真实客户端 | 配置正确性（见第 5 节） |

**每次调用能看到的**：

- 完整 JSON-RPC 请求与响应（协议学习神器）
- 执行耗时（初步性能感知）
- Server stderr 日志（调试 print 输出都在这里）
- 错误详情（401/403/404/参数校验等）

**调试心法**：先在 Inspector 里把工具调到"输入输出都符合预期"，再接入 Claude Code——**不要直接拿真实客户端当调试器**（看不到协议细节，问题定位慢 10 倍）。

## 4. CLI 模式与 CI 冒烟测试

```bash
# 列出工具
npx @modelcontextprotocol/inspector --cli uv -- run python server.py --method tools/list

# 调用工具（可带参数）
npx @modelcontextprotocol/inspector --cli uv -- run python server.py \
  --method tools/call \
  --tool-name add \
  --tool-arg a=3 --tool-arg b=4
```

**CI 冒烟测试（推荐套路）**：

```yaml
# .github/workflows/mcp-smoke.yml（节选）
steps:
  - run: uv add fastmcp
  - run: npx @modelcontextprotocol/inspector --cli uv -- run python server.py \
         --method tools/call --tool-name add --tool-arg a=3 --tool-arg b=4
    # 期望输出含 "7"
```

**分级测试策略**：

| 层级 | 工具 | 时机 |
|---|---|---|
| 本地联调 | Inspector UI | 开发中 |
| 回归冒烟 | Inspector CLI | 每次提交/PR |
| 端到端 | Claude Code + 真实配置 | 上线前 |

## 5. 导出配置到客户端

Inspector 验证通过后，UI 可直接导出两种格式：

| 导出类型 | 内容 | 用途 |
|---|---|---|
| Server Entry | 单个 Server 配置片段 | 粘贴进客户端配置 |
| Servers File | 完整 mcp.json | 直接覆盖/合并进 Claude Code 配置 |

**stdio Server 的标准配置**（与 06 篇格式一致）：

```json
{
  "mcpServers": {
    "demo-server": {
      "command": "uv",
      "args": ["run", "python", "server.py"],
      "env": { "API_KEY": "your-key" }
    }
  }
}
```

> 💡 用导出代替手写，能消除 90% 的"连不上"问题（路径、参数、环境变量笔误）。

## 6. 已知问题与避坑

| 问题 | 现象 | 解决 |
|---|---|---|
| Windows 上 Proxy 启动失败 | `MCP error -32001: Request timed out`，UI 连不上 6277 | Inspector v0.21+ 已知问题：改用 HTTP/SSE 传输，或升级 Node 版本 |
| 空白页 | 浏览器缓存旧版 UI | 无痕窗口 / Ctrl+Shift+R |
| tools/list 为空 | 工具没注册或连错传输 | 检查 Server 入口是否执行了注册代码；确认传输类型 |
| 调用超时 | 长耗时工具 | 增大超时；工具内加进度通知 |
| 本地能跑、客户端连不上 | 配置不一致 | 用 Inspector 导出的配置替换手写配置 |
| 远程偶发 401 | Token 过期/头不一致 | 检查 Bearer Token 与自定义头名称 |

**安全配置红线**：Inspector Proxy 有本地进程执行能力——保持默认 token 认证、**只绑定 localhost（勿绑 0.0.0.0）**、配置 ALLOWED_ORIGINS 防 DNS Rebinding、不在公网裸跑。

> 🎯 **核心要点**：Inspector 把 MCP 调试从"黑盒"变成"白盒"——五步流程（list → happy path → 错参 → 资源提示 → 导出配置）是每个工具的固定验收套路；CLI 模式让"协议冒烟"能进 CI 防回归。记住一个心法：**先在 Inspector 调到完美，再接真实客户端**——调试成本能降一个数量级。

---

**参考来源**：
- [Explore and debug tools with MCP Inspector - Gcore Docs](https://docs.gcore.com/developer-tools/mcp-server/test-tools-with-mcp-inspector)
- [MCP Inspector 调试实战：本地开发、CLI 自动化与安全配置](https://learnagent.org/library/playbooks/mcp-inspector-guide/)

---

**下一模块**：[06-接入Claude Code与客户端配置](06-接入Claude Code与客户端配置.md) / **返回总览**：[00-阶段总览与学习路径](00-阶段总览与学习路径.md)
