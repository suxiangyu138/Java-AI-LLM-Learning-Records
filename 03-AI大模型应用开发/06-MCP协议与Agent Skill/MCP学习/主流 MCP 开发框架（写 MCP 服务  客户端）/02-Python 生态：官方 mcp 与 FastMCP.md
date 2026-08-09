# 02 Python 生态：官方 mcp 与 FastMCP

> Python 的 MCP 版图：官方 mcp SDK（1.x 稳定 / 2.0b1 MCPServer）与 FastMCP 3.x 社区框架——各自定位、代码对照、选型决策，以及 v2 迁移路径。

## 📚 目录

1. [Python 生态三件套](#1-python-生态三件套)
2. [官方 mcp SDK：1.x 稳定线](#2-官方-mcp-sdk1x-稳定线)
3. [官方 v2：MCPServer 与无状态重写](#3-官方-v2mcpserver-与无状态重写)
4. [FastMCP 3.x：社区事实标准](#4-fastmcp-3x社区事实标准)
5. [官方 vs FastMCP：怎么选](#5-官方-vs-fastmcp怎么选)
6. [v2 迁移路径](#6-v2-迁移路径)

## 1. Python 生态三件套

| 组件 | 定位 | 版本（2026-08） | 关系 |
|---|---|---|---|
| 官方 mcp SDK | 协议层（Tier 1） | 1.28 稳定 / 2.0.0b1 | 地基 |
| FastMCP | 高层框架（PrefectHQ） | 3.4.x | 构建于官方 SDK 之上 |
| mcp CLI（官方） | 脚手架/调试 | 随 mcp 包 | `uvx mcp` / Inspector |

**生态事实**：FastMCP 是 Python 生态"写 Server"的事实标准（10,000+ 公开 Server 中大量基于它）；官方 SDK 是"协议保证"的标准（合规、底层、可扩展）。**两者不是替代关系**——FastMCP 内部就是调官方 SDK，选 FastMCP 是选"开发效率"，选官方是选"控制粒度"。

## 2. 官方 mcp SDK：1.x 稳定线

```python
# 官方 SDK 1.x：Server（低层 API）
from mcp.server import Server
from mcp.server.stdio import stdio_server
import mcp.types as types

server = Server("demo")

@server.list_tools()
async def list_tools():
    return [types.Tool(name="add", description="两数相加",
                       inputSchema={"type": "object",
                                    "properties": {"a": {"type": "number"},
                                                   "b": {"type": "number"}},
                                    "required": ["a", "b"]})]

@server.call_tool()
async def call_tool(name: str, arguments: dict):
    return [types.TextContent(type="text", text=str(arguments["a"] + arguments["b"]))]

async def main():
    async with stdio_server() as (r, w):
        await server.run(r, w, server.create_initialization_options())
```

**1.x 官方 SDK 的特点**：装饰器式但偏底层（Schema 手写）、显式初始化、官方保证的协议合规（Tier 1）；**每个方法都对应协议概念**（list_tools/call_tool/list_resources…）——适合理解协议、写框架的人。

**1.28 版本要点**（2026）：Python 3.14 支持、WebSocket 传输弃用（从未入规范）、实验性 tasks API 弃用（SEP-1686 移出规范）——**1.x 的"减负"动作全部指向 2026-07-28 规范**。

## 3. 官方 v2：MCPServer 与无状态重写

```python
# mcp 2.0.0b1：MCPServer（FastMCP 类改名 + 无状态管线）
from mcp.server.mcpserver import MCPServer

mcp = MCPServer("demo")              # 原 FastMCP 类

@mcp.tool
def get_weather(city: str) -> str:
    """查询城市天气。"""
    return f"{city}: 晴，25°C"

# 无状态 HTTP 部署（serverless 友好）
mcp.run(transport="http", host="0.0.0.0", port=8080,
        stateless_http=True, json_response=True)
```

**v2 的核心变化**：

| 变化 | 说明 |
|---|---|
| 类改名 | `FastMCP` → `MCPServer`（名字即定位：服务器而非快速开发） |
| 无状态管线 | 会话式内部改为 dispatcher/runner 管线（2026-07-28 协议） |
| serverless 支持 | `stateless_http=True` + `json_response=True` |
| 迁移工具 | `mcp-migration` 扫描 v1→v2 迁移风险点 |
| 安装 | `pip install "mcp[cli]==2.0.0b1"` |

**v2 的定位变化**：官方 SDK 吸收了 FastMCP 式的高层 API（MCPServer 即原 FastMCP 类），**"官方 vs 社区"的 API 差距在 v2 大幅缩小**——官方高层 + FastMCP 的差异化只剩中间件/组合等扩展（下节）。

## 4. FastMCP 3.x：社区事实标准

```python
from fastmcp import FastMCP, Context, Depends, ToolResult
from fastmcp.middleware import Middleware

mcp = FastMCP("order-server")

@mcp.tool(tags={"write"})
def update_status(order_id: str, status: str) -> ToolResult:
    """更新订单状态（写操作）。"""
    return ToolResult(content="已更新", structured_content={"code": "OK"})

mcp.run(transport="http", host="0.0.0.0", port=8080)
```

**FastMCP 3.x 的差异化能力**（官方 SDK 没有或晚到的）：

| 能力 | 版本 | 价值 |
|---|---|---|
| 中间件系统 | 3.0 | 认证/限流/审计横切 |
| 组合与代理 | 3.0 | 多 Server 合并、包装上游 |
| OAuth 四策略 | 3.0-3.2 | JWT/远程/OAuthProxy/完整实现 |
| code mode | 3.1 | 沙箱 Python 执行，工具列表 token 降 99.9% |
| fastmcp-remote | 3.4.0 | stdio 客户端桥接远程 Server |
| ToolResult(is_error) | 3.4.0 | 结构化错误 |
| OpenTelemetry | 3.0 | 三支柱可观测 |
| OpenAPI 集成 | 3.x | REST 规格自动生成工具 |

**FastMCP 版本时间线**（2026）：3.0（2026-02-18，OAuth/OTel/组合）→ 3.1（code mode）→ 3.2.4（Keycloak 提供商）→ 3.4.0（fastmcp-remote、is_error）→ 3.4.1/3.4.2（安全与兼容修复：Starlette CVE-2026-48710、JWT 兼容）——**版本节奏证明它是活跃维护的社区框架**。

## 5. 官方 vs FastMCP：怎么选

| 维度 | 官方 mcp（1.x/2.x） | FastMCP 3.x |
|---|---|---|
| 协议合规 | Tier 1 官方保证 | 基于官方 SDK（跟随） |
| 开发效率 | 中（低层 API） | 高（装饰器/DI/中间件） |
| v2 支持 | 原生（2.0b1） | 官方 SDK v2 稳定后跟进 |
| 生态扩展 | 少（核心为主） | 多（中间件/组合/代理） |
| 学习价值 | 高（理解协议） | 中（抽象掩盖细节） |
| 适用 | 协议级/框架开发/教学 | 业务 Server/快速交付 |

**选型建议**：
- **写业务 Server（大多数场景）→ FastMCP**：开发效率与生产能力（OAuth/中间件）兼备，阶段 3/4 全程用它；
- **写协议基础设施/定制框架 → 官方 SDK**：控制粒度与合规保证；
- **v2 新项目 → 官方 2.0b1 起步**（MCPServer 已吸收高层 API，等稳定版）；
- **学习协议 → 官方 1.x 低层 API 写一遍**（阶段 2 的做法）再切 FastMCP。

## 6. v2 迁移路径

**迁移决策**：

| 现状 | 建议 |
|---|---|
| 新项目（2026-08 后） | 直接 2.0b1（等稳定版锁版） |
| 存量 FastMCP Server | 暂缓：FastMCP 3.x 继续可用（建在 1.x 上），等官方 v2 稳定 + FastMCP 跟进 |
| 存量官方 1.x Server | 用 `mcp-migration` 扫描 → 按 07 篇清单迁移 |

```bash
# 迁移工具使用
pip install "mcp[cli]==2.0.0b1"
mcp-migration scan .            # 扫描迁移风险点
mcp-migration diff              # 对比 v1/v2 API 差异
```

**迁移注意**：类改名（FastMCP→MCPServer）是机械替换；**真正的迁移工作是"无状态改造"**（删会话依赖、改句柄模式、补 resultType）——与阶段 3 的无状态迁移同一套方法论。

## 7. 官方 SDK 的完整能力表

官方 mcp 1.x 的能力清单（对照 FastMCP 找差异）：

| 能力 | 官方 1.x | FastMCP 3.x |
|---|---|---|
| 工具/资源/提示词 | 装饰器（低层，手写 Schema） | 装饰器（注解转 Schema） |
| Client | 官方 Client（低层） | 无独立 Client（官方提供） |
| 传输 | stdio/SSE/Streamable HTTP | stdio/HTTP（封装官方） |
| 认证 | 无内建（v2 规划） | OAuth 四策略内建 |
| 中间件 | 无 | 内建 |
| 组合/代理 | 无 | 内建 |
| 可观测 | 基础日志 | OTel 内建 |
| CLI/脚手架 | `uvx mcp` + Inspector | 无独立 CLI |

**读表结论**：官方 1.x 是"协议全集、功能最小"（适合框架开发/教学）；FastMCP 是"协议子集、功能最全"（适合业务）——**两者面向的开发者不同，不是竞争关系**。

**深入理解"FastMCP 建在官方 SDK 上"**：这意味着两件事——第一，用 FastMCP 写出的 Server 本质上是官方协议实现（Tier 1 的传输/序列化/协议处理都在底层），协议合规有兜底；第二，FastMCP 的版本节奏受官方 SDK 影响（官方 v2 稳定后 FastMCP 才跟进），**选 FastMCP 等于接受"协议新特性晚半拍"**。这个权衡对业务 Server 几乎无感（业务用不到协议最前沿特性），但对"必须 day-one 用新协议"的基础设施场景就是硬伤——**这也是"业务选社区、基础设施选官方"规律的技术根源**：社区框架的延迟是架构性的，不是维护问题。

## 8. 官方 v2 的完整变化清单

| 变化点 | v1 | v2（2.0.0b1） |
|---|---|---|
| 高层类名 | FastMCP | MCPServer |
| 内部管线 | 会话式 | dispatcher/runner（无状态） |
| 无状态 HTTP | 无 | stateless_http=True |
| JSON 响应 | 文本 | json_response=True |
| 传输 | stdio/SSE/Streamable | Streamable（SSE 弃用） |
| tasks API | 实验性 | 弃用（SEP-1686） |
| WebSocket 传输 | 实验性 | 弃用（从未入规范） |
| 迁移支持 | — | mcp-migration 工具 |

**v2 的哲学**：**"为无状态而生"**——所有 API 变化都指向 2026-07-28 规范的核心（无会话、serverless 友好）；迁移的机械部分（改名）由工具代劳，语义部分（无状态改造）靠阶段 3 方法论。

## 9. 面试速记与常见坑

**面试速记**：
1. FastMCP = PrefectHQ 社区框架（3.x），官方 = Tier 1 mcp SDK——分工不竞争。
2. v2 改名 MCPServer + dispatcher/runner 管线 = 无状态规范重写。
3. FastMCP 差异化：中间件/OAuth 四策略/code mode（token 降 99.9%）/fastmcp-remote。
4. 迁移工具：mcp-migration（Python）、codemod（TS）。
5. 业务 Server 选 FastMCP，协议基础设施选官方——与 Go 的选型规律同构。

**常见坑**：
- 把 FastMCP 当官方（文档/Issue 归属分不清）；
- v2 beta 上生产（等稳定版，锁版本）；
- 不知道 1.28 已弃用 WebSocket 传输（新代码别用）；
- 迁移只做改名不做无状态改造（正活被跳过）；
- 官方低层 API 手写 Schema 出错（用高层类或 Zod 类比物）。

## 10. 工具链：CLI 与 Inspector

Python 生态的配套工具（阶段 2 已用过，这里给全景）：

| 工具 | 用途 | 命令 |
|---|---|---|
| mcp CLI | 脚手架/调试 Server | `uvx mcp` / `python -m mcp` |
| MCP Inspector | 可视化调试 | `npx @modelcontextprotocol/inspector`（跨语言） |
| mcp-migration | v1→v2 迁移扫描 | `mcp-migration scan .` |
| FastMCP Cloud | 托管部署（PrefectHQ） | 云平台 |
| uv/uvx | 环境与依赖管理 | Python 生态标配（3.14 支持） |

**工具链要点**：Inspector 是跨语言的（连任意 Server）；mcp CLI 随官方包提供（脚手架 + 冒烟）；**"mcp-migration"是 Python 生态独有的迁移工具**（TS 用 codemod）——两个官方迁移工具是 v2 时代的第一批"标准件"。

**Python 生态的测试与调试实践**（阶段 3/4 经验的框架落地）：测试用官方 `mcp` 的 Client 类连接 FastMCP 实例（进程内测试，阶段 3 已用）；调试用 Inspector（`npx @modelcontextprotocol/inspector` 连任何 Server，含 Python 的）；性能热点用 `uvloop`/异步优化（FastMCP 基于 asyncio）；**依赖管理用 uv/uvx**（Python 3.14 时代的事实标准）——这些实践与框架无关，但在 Python 生态的完成度最高（AI 开发者基数大，工具链被锤炼得最狠）。**一个生态判断**：Python 的 MCP 工具链（Inspector 跨语言 + uv 管理 + pytest 生态）是"AI 开发者主力"地位的正反馈——工具越好用，开发者越多，生态越大，框架选择空间也越大。

## 11. 生态数据与趋势

| 数据点 | 数值/事实 |
|---|---|
| FastMCP 采用 | 10,000+ 公开 Server 中大量基于它 |
| 官方 SDK 下载 | 累计破 10 亿（与 TS 并列） |
| Python 3.14 | v1.28 起支持（2026） |
| tasks API | v2 弃用（SEP-1686，移出规范） |
| WebSocket 传输 | v1.28 弃用（从未入规范） |
| FastMCP 3.4.1 | 修复 Starlette CVE-2026-48710（安全跟进） |

**趋势判断**：① 官方 v2 的 MCPServer 吸收了 FastMCP 式 API——**官方与社区的高层 API 差距收敛**；② FastMCP 靠中间件/组合/OAuth 保持差异化（官方 v2 短时间不会全盘吸收）；③ **"官方高层 + 社区扩展"是 Python 生态 2026-2027 的最优组合**。

**Python 生态的三个"为什么"**（面试深水区）：① 为什么 FastMCP 比官方流行——"先到 + 开发者体验"：FastMCP 的装饰器/DI/中间件比官方 1.x 低层 API 早一年成熟，10,000+ 公开 Server 的采用形成正反馈；② 为什么官方 v2 吸收 FastMCP 式 API——"生态倒逼"：官方意识到高层 API 是社区标准，与其竞争不如吸收（MCPServer 即证据）；③ 为什么说 Python 是 AI 开发者的 MCP 主力——"生态协同"：MCP Server 的常见业务（数据处理/AI 工具）与 Python 的数据/AI 生态天然匹配，FastMCP 的 OAuth/中间件又补齐了生产化能力——**三个"为什么"讲完，Python 生态的现状就有了完整的因果链**：先到（历史）→ 倒逼（演进）→ 协同（结构），因果链比事实表更能扛住追问。

**Python 生态的常见问题快答**（面试补充）：① "FastMCP 的中间件和阶段 3 说的中间件一样吗？"——同哲学（请求管道横切），FastMCP 是框架级实现，阶段 3 讲的是工程模式——**概念一致、载体不同**；② "官方 v2 稳定后 FastMCP 会消失吗？"——不会，FastMCP 有差异化能力（中间件/组合/OAuth Proxy）且社区惯性大，更可能长期共存（与 Go 双轨同构）；③ "code mode 什么时候用？"——工具列表膨胀（>50 工具）或数据探索类 Server，沙箱 Python 执行代替逐个工具——**"工具爆炸治理"的终极方案**；④ "Python 3.14 对 MCP 的意义？"——官方 SDK v1.28 起支持（2026），异步性能与自由线程实验性改进，**生态跟进是 Python 生态活力的信号**；⑤ "学习 Python 生态的顺序？"——官方 1.x 低层（懂协议）→ FastMCP（效率）→ v2（未来），与 00 篇路线一致。**02 篇的完成标准**：能脱稿讲出"FastMCP 与官方的关系与分工"（一句）、v2 的三大变化（改名/无状态/迁移工具）、官方能力表与 FastMCP 差异化的五个点——三项齐备，Python 生态篇毕业。**02 篇的深度追问**：① "FastMCP 的中间件与官方 v2 的 dispatcher 什么关系？"——FastMCP 中间件建在官方 SDK 的请求处理之上，v2 的 dispatcher/runner 管线是官方对同一需求的底层实现——**高层与底层的同构**；② "官方 v2 为什么不直接合并 FastMCP？"——官方定位是协议层（保持中立），高层框架交给社区竞争（FastMCP 只是最流行的一个）——**官方不垄断应用层是生态健康的设计**；③ "code mode 有安全风险吗？"——沙箱执行（FastMCP 3.1 设计），但沙箱不是万能的——**危险操作仍需确认与最小权限**（阶段 3 五件套在 code mode 同样适用）。**02 篇与 08 篇的对照**：把 08 篇的 Python 天气实现（FastMCP 版）与官方 1.x 低层版对照写一遍，就是"高层 vs 低层"差异的第一手体验——**官方 1.x 版约 40 行（手写 Schema/显式初始化），FastMCP 版约 15 行**——这个行数差就是"选高层框架"的理由，而"手写 Schema 的 40 行"就是"理解协议"的学费（阶段 2 建议的路径）。

**Python 生态的实操路径建议**（本库阶段 2-4 读者的衔接）：阶段 2 用 FastMCP 跑通最小链路后，补一遍官方 1.x 低层 API（理解协议：list_tools/call_tool 的每个方法对应一个协议概念）；阶段 3/4 的生产级 Server 继续用 FastMCP（认证/中间件/测试都建立在 FastMCP 生态上）；2026 下半年官方 v2 稳定后，评估 MCPServer 是否满足需求（无状态部署/JSON 响应是加分项），不满意则留在 FastMCP 3.x（官方 v2 稳定前它持续可用）——**路径的本质：先懂协议（官方低层），再用效率（FastMCP），最后看迁移（v2 评估）**。

**Python 生态的面试表达**：被问"你用什么写 MCP"时，回答分三层——第一层讲事实（"业务 Server 用 FastMCP，协议层用官方 SDK"）；第二层讲理由（"FastMCP 建在官方 SDK 上，效率与合规兼得；官方 v2 的 MCPServer 是未来方向"）；第三层讲判断（"基础设施场景会切官方以保证 day-one 合规，业务场景留在 FastMCP 等 v2 稳定"）——**三层回答展示的是"选型思维"而非"工具熟练度"**，这正是框架知识体系区别于"会写代码"的价值所在。同样的三层结构适用于任何生态的面试问答：先事实、再理由、后判断，三层齐备的答案在任何框架问题面前都不会浅。

> 🎯 **核心要点**：Python 生态 = 官方 mcp（Tier 1 协议层，1.28 稳定 / 2.0b1 MCPServer 无状态重写）+ FastMCP 3.x（社区高层框架，中间件/OAuth/code mode 等差异化）+ 选型两分（业务 Server 用 FastMCP，协议基础设施用官方）+ 工具链（Inspector 跨语言 / mcp-migration 迁移）+ v2 迁移（改名机械、无状态改造是正活）。**记住一个事实：FastMCP 是写 Python MCP Server 的事实标准，官方 SDK 是协议的法定标准——两者不是竞争，是分工；官方 v2 让高层 API 收敛，差异化靠社区扩展**。

---

**下一模块**：[03-TypeScript 生态：官方 SDK 与 v2 拆包](03-TypeScript 生态：官方 SDK 与 v2 拆包.md) / **返回总览**：[00-主流MCP开发框架知识体系总览](00-主流MCP开发框架知识体系总览.md)
