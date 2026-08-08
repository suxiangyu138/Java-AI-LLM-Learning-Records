# 06 Claude Agent SDK：Claude Code 作为库

> 定位：完整 harness 范式的代表——把 Claude Code 的 agent loop、内置工具、上下文管理打包成 Python/TypeScript 库（v2.x，2026-08 基准）

## 📚 目录

1. [定位与四者区分](#1-定位与四者区分)
2. [双入口：query 与 ClaudeSDKClient](#2-双入口query-与-claudesdkclient)
3. [内置工具与自定义工具](#3-内置工具与自定义工具)
4. [权限六模式](#4-权限六模式)
5. [消息流与循环控制](#5-消息流与循环控制)
6. [上下文管理与自动压缩](#6-上下文管理与自动压缩)
7. [会话、hooks 与子 Agent](#7-会话hooks-与子-agent)
8. [2026 实践要点与陷阱](#8-2026-实践要点与陷阱)

## 1. 定位与四者区分

```text
Claude Agent SDK = Claude Code 打包为库（Python/TS）
  同款工具、同款 agent loop、同款上下文管理，程序化调用
  harness-only：循环与工具由 SDK 提供，部署与托管自己做

四者区分（官方）：
  Agent SDK     → 在自己进程里跑 agent loop（本模块）
  Claude Code CLI → 终端交互/一次性任务
  Client SDK    → 直接调 API，loop 自己写
  Managed Agents → 托管 REST API，Anthropic 跑 loop 与沙箱
```

> ⚠️ **与其他 Claude 产品的区别**：它**不是** API Tool Runner（`client.beta.messages.tool_runner`，那是 Client SDK 的 beta 辅助，loop 范围更小、无内置工具）；两者不要混用。其他语言没有官方 SDK——用 CLI 子进程 `-p --output-format json` 驱动同一 loop。

## 2. 双入口：query 与 ClaudeSDKClient

| 入口 | 会话 | 场景 |
|------|------|------|
| `query()` | 每次新会话（可 resume/fork） | 一次性任务、批处理 |
| `ClaudeSDKClient` | 复用同一会话 | 持续对话、支持 interrupt |

```python
import asyncio
from claude_agent_sdk import query, ClaudeAgentOptions, ResultMessage

async def main():
    async for message in query(
        prompt="修复 auth 模块的测试失败",
        options=ClaudeAgentOptions(
            allowed_tools=["Read", "Edit", "Bash", "Glob", "Grep"],
            setting_sources=["project"],      # 加载 CLAUDE.md/skills/hooks
            max_turns=30,                     # 防跑飞
            effort="high",
        ),
    ):
        if isinstance(message, ResultMessage):
            if message.subtype == "success":
                print(f"完成: {message.result}，成本 ${message.total_cost_usd:.4f}")
            elif message.subtype == "error_max_turns":
                print(f"超轮次，可 resume 会话 {message.session_id}")
    # 注意：query() 出错会 raise——循环要包 try（见官方示例）

asyncio.run(main())
```

> 💡 **API 形状说明**：2026 现行 API 是 `query()`/`ClaudeSDKClient`/`ClaudeAgentOptions`；早期资料中的 `Agent` 类、`AgentResult`、`SessionMemory` 属于旧/误导信息，不在此版本。

## 3. 内置工具与自定义工具

### 3.1 内置工具（Claude Code 同款）

| 类别 | 工具 |
|------|------|
| 文件 | `Read` / `Edit` / `Write` |
| 搜索 | `Glob` / `Grep` |
| 执行 | `Bash`（shell/git/脚本） |
| 网页 | `WebSearch` / `WebFetch` |
| 发现 | `ToolSearch`（按需加载工具 schema） |
| 编排 | `Agent`（子 Agent）/ `Skill` / `AskUserQuestion` / `TaskCreate` / `TaskUpdate` |

### 3.2 自定义工具与进程内 MCP

```python
from claude_agent_sdk import tool, create_sdk_mcp_server, ClaudeAgentOptions

@tool("add", "两个数相加", {"a": float, "b": float})
async def add(args):
    return {"content": [{"type": "text", "text": f"和: {args['a'] + args['b']}"}]}

server = create_sdk_mcp_server(name="calculator", version="2.0.0", tools=[add])
options = ClaudeAgentOptions(
    mcp_servers={"calc": server},
    allowed_tools=["mcp__calc__add"],
)
```

外部 MCP 服务器同样支持：`mcp_servers` 配置 + `allowed_tools=["mcp__server__tool"]`。工具可标注 `readOnlyHint` 支持并行执行（只读工具并发、改状态工具串行）。

## 4. 权限六模式

```text
三件套协同：
  allowed_tools     自动批准（列表内免提示；不限制，需 disallowed 阻断）
  disallowed_tools  硬阻断（支持 "Bash(rm *)" 作用域规则）
  permission_mode   总模式

六模式：
  default          未覆盖工具走 canUseTool 回调；无回调=拒绝
  acceptEdits      自动批准文件编辑与常见 FS 命令
  plan             探索只规划，文件编辑永不自动批准
  dontAsk          永不提示；规则外全部拒绝（无头 Agent 固定工具面）
  auto             模型分类器自动批准/拒绝（有护栏的自主模式）
  bypassPermissions 全部放行（CI/容器隔离环境专用；TS 需显式 allowDangerouslySkipPermissions）
```

> 🎯 **2026 建议**：交互应用用 `default` + 批准回调；自主开发机用 `acceptEdits`；`bypassPermissions` 仅限 CI/容器。

## 5. 消息流与循环控制

### 5.1 五种消息类型

| 类型 | 含义 |
|------|------|
| `SystemMessage` | 生命周期事件（init 会话元数据 / compact_boundary 压缩后 / informational / worker_shutting_down） |
| `AssistantMessage` | 每轮 Claude 响应（文本 + 工具调用块） |
| `UserMessage` | 每次工具执行后的结果回传 |
| `StreamEvent` | 流式原始事件（需 `include_partial_messages`） |
| `ResultMessage` | 循环结束：结果/用量/成本/session_id + `subtype` |

### 5.2 ResultMessage 终止状态

| subtype | 含义 | result 可用? |
|---------|------|:---:|
| `success` | 正常完成 | ✅ |
| `error_max_turns` | 达 max_turns | ❌ |
| `error_max_budget_usd` | 达成本上限 | ❌ |
| `error_during_execution` | 执行中错误 | ❌ |
| `error_max_structured_output_retries` | 结构化输出重试耗尽 | ❌ |

### 5.3 循环控制参数

```text
max_turns          工具轮次上限（只计工具轮）——生产默认要设
max_budget_usd     成本上限（覆盖子 Agent 开销）
effort             low/medium/high/xhigh/max 推理深度（xhigh 推荐用于编码/agentic）
model              不设则用 Claude Code 默认；可显式 pin
```

## 6. 上下文管理与自动压缩

```text
上下文窗口：会话内不重置——system + 工具定义 + 对话历史持续累积
  CLAUDE.md 与工具定义自动 prompt caching（前缀稳定）

自动压缩：上下文将满时自动摘要旧历史
  触发 → 发 compact_boundary 事件
  定制：CLAUDE.md 里写"摘要时保留什么"（compactor 会读）
        PreCompact hook 存档完整 transcript
  手动：prompt 传 "/compact"

上下文效率策略（官方）：
  子 Agent 隔离——子任务用子 Agent，主上下文只收最终报告
  工具精选——每个工具定义都占上下文；子 Agent 只给最小工具集
  MCP tool search——默认延迟加载 MCP schema（关闭时每个 server 全量加载）
  effort=low 例行任务
```

## 7. 会话、hooks 与子 Agent

### 7.1 会话

```text
ResultMessage.session_id → 后续 resume / fork
  resume: 恢复全部上下文（读过的文件、做过的分析）
  fork:   分支到不同方案，不改原会话
跨无状态容器/Serverless：session_store 适配器镜像 transcript 到自家后端
辅助：list_sessions() / get_session_messages() / rename_session() / tag_session()
```

### 7.2 Hooks（进程内执行，不占上下文）

| Hook | 触发点 | 用途 |
|------|--------|------|
| `PreToolUse` | 工具执行前 | 校验入参、阻断危险命令（可短路循环） |
| `PostToolUse` | 工具返回后 | 审计输出、触发副作用 |
| `UserPromptSubmit` | 提交 prompt 时 | 注入额外上下文 |
| `Stop` | Agent 完成时 | 校验结果、保存会话状态 |
| `SubagentStart/Stop` | 子 Agent 生灭 | 聚合并行任务结果 |
| `PreCompact` | 压缩前 | 存档完整 transcript |

### 7.3 子 Agent（AgentDefinition）

```python
options = ClaudeAgentOptions(
    agents={
        "code-reviewer": AgentDefinition(
            description="审查代码改动",
            prompt="你是代码审查员，报告 diff 中的问题。",
            tools=["Read", "Grep", "Glob"],
            model="sonnet",
        ),
    },
)
```

子 Agent 新开对话（不继承父对话历史），只回传最终报告——主上下文按摘要增长而非全量。

## 8. 2026 实践要点与陷阱

### 8.1 实践要点

```text
- 生产默认设 max_turns + max_budget_usd（开源式 prompt 会跑很久）
- effort 按任务调：例行 low，编码/agentic xhigh
- 权限组合：allowed_tools 白名单 + default/acceptEdits 模式
- hooks 做审批门与审计（工程化模块 08 篇落地）
- 流式输出 include_partial_messages 给用户实时进度
- 品牌合规：集成产品不能自称 "Claude Code"（官方 branding 指南）
```

### 8.2 陷阱速查

| 陷阱 | 说明 |
|------|------|
| query() 错误会 raise | 错误结果后抛异常是设计（单次模式）；要 try 包裹 |
| 绑 Claude 生态 | 模型/工具绑定 Anthropic；其他语言无官方 SDK |
| API 形状误信旧资料 | 现行是 query/ClaudeSDKClient/ClaudeAgentOptions |
| 上下文膨胀 | 长会话累积大——子 Agent 隔离 + 工具精选 + 自动压缩 |
| 本地进程承载 | harness 在自己进程跑——资源/超时/优雅停机要自己管 |
| 沙箱边界 | 内置工具在你的进程/文件系统执行——权限模式是唯一防线 |

---

## 【参考来源】

- [Claude Agent SDK Overview（官方）](https://code.claude.com/docs/en/agent-sdk/overview)
- [Claude Agent SDK: How the agent loop works（官方）](https://code.claude.com/docs/en/agent-sdk/agent-loop)
- [Claude Agent SDK: Python reference（官方）](https://code.claude.com/docs/en/agent-sdk/python)
- [Claude Agent SDK: TypeScript reference（官方）](https://code.claude.com/docs/en/agent-sdk/typescript)
- [Claude Agent SDK: Hooks（官方）](https://code.claude.com/docs/en/agent-sdk/hooks)
- [Claude Agent SDK: Permissions（官方）](https://code.claude.com/docs/en/agent-sdk/permissions)
- [Claude Agent SDK: Sessions（官方）](https://code.claude.com/docs/en/agent-sdk/sessions)

---

**返回总览**：[00-总览：单 Agent 框架知识体系](00-总览：单%20Agent%20框架知识体系.md)

**下一模块**：[07-Agent 循环通用架构：框架的底层共识](07-Agent%20循环通用架构：框架的底层共识.md)
