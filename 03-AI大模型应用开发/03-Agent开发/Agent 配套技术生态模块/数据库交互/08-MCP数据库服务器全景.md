# MCP 数据库服务器全景

> 数据库交互的"标准插座"：通过 MCP 协议把 SQL 能力暴露给任何 Agent 客户端。2026 年生态爆发——本模块对比七大服务器，讲透选型与安全默认值。

## 1. MCP 数据库服务器是什么

```text
Agent 客户端（Claude Desktop / Claude Code / Cursor / 自研 Agent）
    │  MCP 协议（stdio / streamable HTTP / SSE）
    ▼
数据库 MCP 服务器（独立进程）
    ├── 连接管理（连接池/多库/隧道）
    ├── 工具暴露（list_tables / describe / query / explain...）
    ├── 安全默认（只读模式/行数上限/超时/白名单）
    └── Schema 资源（db://schema 供客户端读取）
```

| 优势 | 说明 |
|---|---|
| 即插即用 | npx/uvx 一行启动，Claude Code 等客户端自动发现 |
| 工具标准化 | 各客户端用同一套工具签名 |
| 安全默认 | 主流服务器默认只读/限行/超时 |
| 生态复用 | 一个服务器服务所有 Agent 客户端 |

> 🎯 核心要点：MCP 数据库服务器的价值是**"一次封装，处处可用"**——数据库连接逻辑（含安全默认值）与 Agent 客户端解耦。

## 2. 七大服务器横向对比（2026-08 基准）

| 服务器 | 出品 | 支持库 | 默认权限 | 亮点 |
|---|---|---|---|---|
| **DBHub** | bytebase | PG/MySQL/SQL Server/MariaDB/SQLite | 可配只读 | 官方示例、默认 2 工具 ~1.4k tokens（省 13-14x）、TOML 多连接、SSH 隧道 |
| **mcp-multi-db** | 社区 | PG/MySQL/SQLite | **只读默认** | 双层强制（文本守卫+只读事务）、schema 感知 |
| **talk-sql** | 社区 | PG/Cockroach/MySQL/SQL Server/SQLite/DB2 | 全量（含 DDL） | 可建表/建外键/导出 ER 图（Mermaid） |
| **mcp-alchemy** | 社区 | SQLAlchemy 全家（9 库） | 可配 | 连接池优化、驱动自由 |
| **database-mcp** | 社区 | PG/MySQL/SQLite | 可配 | **执行回滚快照**、EXPLAIN ANALYZE、上下文压缩 60-95% |
| **any-db-mcp** | 社区 | PG/MySQL/SQLite/MSSQL | readonly/readwrite/full 三档 | 事务工具带 rollback、HTTP 传输+Bearer |
| **safedb-mcp** | 社区 | 多库 | 只读+守卫 | 表白名单、PII 脱敏、审计日志 |

## 3. 选型决策树

```text
要最省 token / 官方背书？→ DBHub（Claude Code 文档示例）
要"只读铁律"开箱即用？→ mcp-multi-db / safedb-mcp
要写操作（建表/ER 图）？→ talk-sql（务必配审批，见 07 篇）
要复杂库兼容？→ mcp-alchemy（SQLAlchemy 九库）
要执行回滚/审计快照？→ database-mcp / safedb-mcp
要 HTTP 远程 + 令牌？→ any-db-mcp / DBHub（SSH）
```

| 场景 | 推荐 |
|---|---|
| Claude Code 日常查库 | DBHub（官方示例，token 最省） |
| 自研 Agent 只读查询 | mcp-multi-db（只读默认+双层强制） |
| 数据建模/教学演示 | talk-sql（建表+ER 图） |
| 企业多库统一 | mcp-alchemy（SQLAlchemy 生态） |
| 高合规场景 | safedb-mcp（白名单+PII+审计） |

## 4. 配置示例：DBHub（官方推荐路线）

```toml
# dbhub.toml —— 多连接 + 只读模式
[[connections]]
name = "prod_ro"
url = "postgresql://agent_ro:xxx@10.0.0.5:5432/app"
read_only = true
max_rows = 200
query_timeout = "10s"

[[connections]]
name = "dev"
url = "mysql://dev:xxx@127.0.0.1:3306/app"
read_only = true
```

```bash
# 启动（npx 一行）
npx @bytebase/dbhub --config dbhub.toml
# Claude Code 配置 MCP 后自动获得 execute_sql / search_objects 两个工具
```

**token 效率对比**：DBHub 默认工具载荷 ~1.4k tokens，为同类方案的 1/13-1/14——省 token 即省钱（每条消息都带工具定义）。

## 5. 安全默认值清单（接入任何 DB MCP 前确认）

| 项 | 默认 | 建议 |
|---|---|---|
| 只读模式 | 部分默认 | **全部开启**（DB 角色只读是前提，见 03 篇） |
| 行数上限 | 100-1000 | 保持 ≤200 |
| 查询超时 | 有 | 10s |
| 多语句 | 多数拒绝 | 确认拒绝 |
| PII 脱敏 | safedb 有 | 敏感库必配 |
| 审计 | 部分有 | 生产必开 |
| 传输 | stdio 优先 | 远程用 HTTP+Bearer/SSH，勿裸 TCP |
| 连接串 | — | 环境变量注入，不进版本库 |

> ⚠️ 关键提醒：**MCP 服务器的"只读模式"只是应用层守卫**（03 篇已讲可绕过）——真正防线仍是数据库账号只读。别因为服务器自带只读开关就放松 DB 角色。

## 6. 接入自研 Agent（自定义 MCP 客户端）

```python
# 自研 Agent 通过 MCP SDK 调用（Python 示例）
from mcp import ClientSession, StdioServerParameters
from mcp.client.stdio import stdio_client

server = StdioServerParameters(command="npx", args=["@bytebase/dbhub", "--config", "dbhub.toml"])
async with stdio_client(server) as (read, write):
    async with ClientSession(read, write) as session:
        await session.initialize()
        tools = await session.list_tools()          # 自动发现 execute_sql 等
        result = await session.call_tool("execute_sql",
            {"connection": "prod_ro", "sql": "SELECT COUNT(*) FROM orders"})
```

与 [04 模块](04-工具封装实战：连接查询与返回.md) 的自研工具链关系：**两者等价**——自研工具适合深度定制（口径注入/审批闭环），MCP 服务器适合快速接入与生态复用；生产可混合：查询走 MCP，写操作走自研审批工具。

## 7. 生态趋势（2026）

| 趋势 | 说明 |
|---|---|
| 官方化 | DBHub 进入 Claude Code 官方文档，数据库厂商自建 MCP 成标配 |
| 安全内建 | 只读默认/白名单/PII/审计从"可选"变"标配" |
| token 优化 | 工具载荷、结果压缩成选型硬指标（60-95% 压缩出现） |
| 协议升级 | 2025-06-18 MCP 规范：版本协商、结构化工具输出 |
| 一库一服务器 | 大厂数据平台（Snowflake/Oracle）把 MCP 做成产品能力 |

> 🎯 核心要点：MCP 数据库服务器的竞争焦点已经从"能不能查"转向**"安全默认、token 效率、审计完备"**——选型看这三项，而不是看支持库列表。

## 8. 接入后的常见故障排查

| 症状 | 原因 | 排查 |
|---|---|---|
| 客户端连不上服务器 | stdio 路径/Node 版本问题 | DBHub 要求 Node ≥22.5；先 `npx @bytebase/dbhub` 独立跑通 |
| 查询超时 | 无 LIMIT 全表扫描 | 检查 max_rows/query_timeout 配置；先 EXPLAIN |
| 工具调用报"连接不存在" | TOML 连接名拼错/未加载 | 核对 dbhub.toml `[[connections]]` name 与调用参数 |
| 只读模式仍能写 | 服务器配置只读但 DB 账号有写权 | 数据库角色只读才是边界（03 篇），服务器开关只是减负 |
| 上下文爆炸 | 工具载荷大/结果不截断 | 换 DBHub（1.4k tokens）；配 max_rows 与压缩模式 |
| 多库串数据 | 一个服务器配了多个库 | 按库拆分服务器实例或严格区分连接名 + 白名单 |
| SSH 隧道不稳 | 隧道配置 | 检查隧道存活/跳板机白名单；或走 HTTP+Bearer |

## 9. 自研 vs MCP：终局建议

| 维度 | 自研工具链 | MCP 服务器 |
|---|---|---|
| 接入速度 | 慢（写代码） | 快（npx/uvx 一行） |
| 定制深度 | 深（口径/审批/幂等全可控） | 浅（受服务器能力约束） |
| 审计集成 | 原生（写进自己的日志体系） | 依赖服务器能力（safedb 等） |
| 生态复用 | 只服务自己的 Agent | 服务所有 MCP 客户端 |
| 2026 趋势 | 封装成 MCP Server 暴露 | 逐渐成为接入默认形态 |

**结论**：查询场景优先 MCP（DBHub 等成熟服务器），复杂审批写场景自研工具并最终封装为 MCP Server——两条路在"标准插座"上汇合。

---

**下一模块**：[09-Java 集成：LangChain4j 与 Spring AI](09-Java集成：LangChain4j与SpringAI.md)　**返回总览**：[00-数据库交互总览](00-数据库交互总览.md)

## 参考来源

- [DBHub：Minimal database MCP server（bytebase）](https://github.com/bytebase/dbhub)
- [mcp-multi-db：Read-only MCP server（GitHub）](https://github.com/mahAnuj/mcp-multi-db)
- [talk-sql（npm）](https://www.npmjs.com/package/talk-sql)
- [mcp-alchemy（GitHub）](https://github.com/runekaagaard/mcp-alchemy)
- [database-mcp（GitHub）](https://github.com/cocaxcode/database-mcp)
- [any-db-mcp（npm）](https://www.npmjs.com/package/@sakura0v0/any-db-mcp)
- [safedb-mcp（GitHub）](https://github.com/narekmalk/safedb-mcp)
