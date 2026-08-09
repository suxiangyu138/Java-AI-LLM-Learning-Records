# 03 TypeScript 生态：官方 SDK 与 v2 拆包

> TS 是 MCP 的参考实现生态：v1.29 稳定线的完整能力（Zod 类型即 Schema、三传输、OAuth 2.1 + PKCE、Express 适配）、v2 拆包（server/client/spec 三包）与 codemod 迁移、工具链与部署细节、安全与版本纪律——从定位到实战的完整一册。

## 📚 目录

1. [TS 生态定位](#1-ts-生态定位)
2. [v1.29：完整能力清单](#2-v129完整能力清单)
3. [Server 实战：McpServer + Zod](#3-server-实战mcpserver--zod)
4. [Client 实战与多传输](#4-client-实战与多传输)
5. [OAuth 2.1 与 Express 适配](#5-oauth-21-与-express-适配)
6. [v2 拆包与迁移](#6-v2-拆包与迁移)
7. [工具链与调试生态](#7-工具链与调试生态)
8. [传输与部署细节](#8-传输与部署细节)
9. [安全与版本管理](#9-安全与版本管理)
10. [生态数据与趋势](#10-生态数据与趋势)
11. [面试速记与常见坑](#11-面试速记与常见坑)

## 1. TS 生态定位

**TS 是 MCP 的"参考实现"**——协议规范的行为定义以 TS SDK 为准，其他语言的 SDK 对照它实现；跨语言行为不一致时，以 TS 为基准排查。这个地位的成因是历史与生态的双重作用：MCP 起源的 Anthropic 生态与前端生态同源，TS 自然成为第一个完整实现，参考实现地位由此固化。

| 维度 | 现状（2026-08） |
|---|---|
| 角色 | 参考实现（协议行为基准） |
| 版本 | v1.29 稳定线；v2（拆包）alpha |
| 生态 | 3.47 万+ 依赖项目；累计下载破 10 亿 |
| 运行时 | Node 22+（v1 要求） |
| 双端 | Server 与 Client 均为 Tier 1 |

**TS 生态的"两面"**：对前端/全栈团队是主场（同一仓库写前端、MCP Server 与 Agent 客户端，类型系统贯穿全程）；对纯后端团队是"又多一个 Node 服务"——是否值得，按 07 篇决策树的"团队栈"一问答。

## 2. v1.29：完整能力清单

| 能力 | 实现 |
|---|---|
| 传输 | stdio / HTTP+SSE（弃用中） / Streamable HTTP |
| Schema | Zod 自动转 JSON Schema（类型安全） |
| 高层框架 | McpServer（registerTool）；低层 Server 类备选 |
| 客户端 | Client + 各传输客户端类 |
| 认证 | OAuth 2.1 with PKCE（mcpAuthRouter） |
| 集成 | Express 适配器（MCP 端点挂进现有 Web 服务） |
| 工具链 | Inspector / CLI / codemod |

**v1.27.1 安全要点**（2026-02）：修复 URL 处理命令注入漏洞、补认证预注册一致性测试——**TS 生态是漏洞发现-修复循环最快的**（生态大 = 攻击面大），生产必须锁版本并定期跟进。

**能力清单的读法**：这张表不是"功能罗列"，而是"参考实现的完整度证明"——其他语言 SDK 对照这张表找差距（例如"有没有开箱即用认证""有没有 Web 框架适配器"）；**"参考实现"意味着 TS 的每个能力都是协议行为的标本**：遇到跨语言行为不一致（如传输头格式、错误码语义），以 TS SDK 的实际行为为准排查，这是 TS 生态排障的独特方法论。

## 3. Server 实战：McpServer + Zod

```typescript
// v1.29：McpServer（高层）+ Zod（类型即 Schema）
import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { z } from "zod";

const server = new McpServer({ name: "demo", version: "1.0.0" });

server.registerTool("get_weather", {
  description: "查询城市天气。何时用：用户问天气时。",
  inputSchema: {
    city: z.string().describe("城市名"),
    days: z.number().int().min(1).max(7).optional().describe("预报天数"),
  },
  outputSchema: z.object({ city: z.string(), temp: z.number() }),
}, async ({ city, days }) => ({ city, temp: 25 }));

await server.connect(new StdioServerTransport());
```

**Zod 的双层价值**：① 编译期——`z.string().describe()` 同时产生 TS 类型与工具 Schema（阶段 3 的 Schema 规范在 TS 生态由语言承载）；② 运行期——Zod 是运行时校验器，**非法参数在进入 handler 之前被拦截**（类型/枚举/长度），与阶段 3 的"Schema 层校验"完全同构。`outputSchema` 进一步约束返回结构（Python 生态的返回注解没有这么严格）。

## 4. Client 实战与多传输

```typescript
// v1.29：Client（Streamable HTTP）
import { Client } from "@modelcontextprotocol/sdk/client/index.js";
import { StreamableHTTPClientTransport } from
  "@modelcontextprotocol/sdk/client/streamableHttp.js";

const transport = new StreamableHTTPClientTransport(
  new URL("http://localhost:8080/mcp"),
  { authProvider: mcpAuthProvider }   // OAuth 流程（第 5 节）
);
const client = new Client({ name: "my-agent", version: "1.0" });
await client.connect(transport);

const tools = await client.listTools();
const result = await client.callTool({ name: "get_weather",
                                       arguments: { city: "北京" } });
```

**传输选择**：stdio（本机子进程）、Streamable HTTP（远程，2026 规范唯一远程传输）、HTTP+SSE（v2 弃用，新代码不碰）——**本机用 stdio、对外用 Streamable HTTP**，与阶段 3 结论一致。客户端代码可复用阶段 4 的编排器心智（list/call + 传输 + 认证是全部核心）。

## 5. OAuth 2.1 与 Express 适配

```typescript
// OAuth：mcpAuthRouter 完成授权服务器职责（PKCE）
import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { mcpAuthRouter } from "@modelcontextprotocol/sdk/auth/mcpAuthRouter.js";
import express from "express";

const server = new McpServer({ name: "secure-demo", version: "1.0" });
const authRouter = mcpAuthRouter({ serverInfo: { name: "secure-demo", version: "1.0" } });

const app = express();
app.use(express.json());                     // JSON 解析必须先于 MCP 适配器
app.use("/mcp", authRouter);                 // 认证 + MCP 端点一体
app.listen(3000);
```

**TS 生态的认证特点**：`mcpAuthRouter` 内建完整授权服务器流程（OAuth 2.1 + PKCE）——**它是四个 Tier 1 SDK 中唯一"开箱即用完整授权服务器"的实现**（Python 官方 v1 没有、FastMCP 走 OAuth Proxy 路线）；做"对外服务的 MCP"，TS 生态认证最省事。认证能力源自生态需求：TS 是 Web 生态语言，OAuth/PKCE 是 Web 基础设施的既成惯例，mcpAuthRouter 是"把 Web 认证能力带进 MCP"的自然结果。

**认证的工程含义**：有了 mcpAuthRouter，"对外服务的 MCP Server"在 TS 生态的认证工作从"自建授权服务器"降为"配置 + 挂载"——登录页、同意页、令牌签发、JWKS 端点全部内建（阶段 3 认证篇的六 SEP 义务由框架代劳一部分）；**对比参照**：Java 生态要靠 Spring Security 组合，Python 官方 v1 要自行封装——**"认证开箱即用"是选型时容易被忽略、但对"对外服务"场景权重极高的一项差异**（08 篇认证对照表的数据来源）。

## 6. v2 拆包与迁移

**v2 的最大变化：包拆分**：

```text
v1：@modelcontextprotocol/sdk（一包装全部）
v2：@modelcontextprotocol/server（服务端）
    @modelcontextprotocol/client（客户端）
    @modelcontextprotocol/spec（规范类型：spec.types.2025-11-25.ts / 2026-07-28.ts）
```

**v2 的三大设计**：① 拆包——按端安装、依赖更小（服务端项目不装客户端代码）；② Schema 校验一等公民——从可选变默认，强化"输入不可信"安全基线；③ **规范类型按版本追踪**——`spec.types.2026-07-28.ts` 让"协议升级 = 类型变更"，编译器在升级依赖时直接报出所有不兼容调用，**把迁移的语义风险提前到编译期**（相对 Python 的结构性优势）。

```bash
# v1 → v2 机械迁移（官方 codemod）
npx @modelcontextprotocol/codemod@beta v1-to-v2 .
```

**迁移节奏**：v2 目前 2.0.0-alpha.2（beta），生产锁 v1.29 等稳定（约 2026-08 后）；新项目可 alpha 试用。codemod 只代劳机械部分（改包名/导入），**"无状态改造"（删会话依赖、补 resultType）是正活**——按 07 篇七步清单走。

**迁移的时机判断**（与 07 篇一致）：新项目直接 v2 起步（无历史包袱）；生产项目按部署需求评估——要上云/Serverless 的优先迁（无状态红利），纯本机个人工具可留在 v1（12 个月弃用窗口 + 双代兼容兜底）；**"等 codemod 成熟再迁"是合理等待，但"等所有工具都支持 v2 再迁"是拖延**——v2 是 TS 生态的主线，早迁早受益。

## 7. 工具链与调试生态

| 工具 | 用途 | 使用方式 |
|---|---|---|
| MCP Inspector | 可视化调试（工具/资源/提示词） | `npx @modelcontextprotocol/inspector`（跨语言） |
| CLI（官方） | 脚手架/冒烟 | `npx @modelcontextprotocol/cli` |
| codemod | v1→v2 迁移 | `npx @modelcontextprotocol/codemod@beta v1-to-v2 .` |
| spec 类型 | 协议类型即文档 | `spec.types.2026-07-28.ts`（v2） |
| npm 生态 | 复用一切 Node 库 | 与普通 TS 服务无异 |

**调试优势**：Inspector 原生支持（参考实现的工具链最全）+ npm 调试工具（tsx/ts-node/vitest）直接可用——**TS 写 MCP 的调试体验在六生态中最好**，这是前端团队选它的隐形理由；配合 vitest 的进程内测试，TS 生态的"调试 + 测试"组合与阶段 3 的 Inspector + pytest 双件套完全对应；**工具链的完备是生态成熟度的直接投影**——工具好用的生态，开发者才愿意长期投入。

**类型系统的工程红利**："类型即文档、编译即回归"——TS 团队的回归保障一半靠编译器、一半靠测试（Python 团队几乎全靠测试）；`outputSchema` 让工具返回结构也受类型约束。**"用类型系统管理协议版本"是 TS 生态对 MCP 工程最独特的贡献**（v2 spec 包）。

## 8. 传输与部署细节

### 8.1 传输选型

| 传输 | 场景 | 部署形态 | 选择理由 |
|---|---|---|---|
| stdio | 本机子进程（Claude Code/编辑器） | `claude mcp add` 直接指启动命令 | 零网络面、零配置、安全面最小 |
| Streamable HTTP | 远程（**2026 唯一远程传输**） | Node 服务 / Docker / Serverless | 无状态规范的正统远程形态 |
| HTTP+SSE | 存量项目 | 迁移到 Streamable | v2 已弃用，新代码不碰 |

### 8.2 部署形态

| 形态 | 配置要点 | 适用 |
|---|---|---|
| 本机进程 | `node server.js` 或 `npx tsx server.ts` | 开发/个人 |
| Docker 容器 | Node 22+ 镜像、非 root、健康检查 | 生产标准 |
| Serverless | 无状态规范红利：冷启动可接受、状态外置 | 低频/突发 |
| Express 挂载 | `app.use("/mcp", ...)` 适配器 | **与现有 Web 服务一体**（TS 独有） |

### 8.3 部署注意点

1. **Node 版本**：v1 要求 Node 22+，生产镜像锁 Node LTS（22/24 线）；
2. **健康检查**：用 MCP 端点冒烟（`tools/list`）比进程存活检查更真实——阶段 3 的"就绪要真实"原则；
3. **传输切换**：同一套 Server 代码，stdio 与 HTTP 只差 transport 类——"传输即配置"跨框架成立（08 篇对照）；
4. **Express 挂载**：`express.json()` 必须位于 MCP 适配器之前（JSON body 解析是协议前提）；认证/限流/日志与普通 API 路由共用一套——**"MCP 端点 = 又一个 Express 路由"的心智让 TS 部署零学习成本**；
5. **Serverless 边界**：长连接/流式场景受平台超时限制，普通工具调用无碍——选 Serverless 前先确认工具全部秒级返回。

### 8.4 无状态部署示例

```typescript
// Streamable HTTP 独立部署（无状态规范：不维护会话）
import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StreamableHTTPServerTransport } from
  "@modelcontextprotocol/sdk/server/streamableHttp.js";

const server = new McpServer({ name: "weather", version: "1.0.0" });
const transport = new StreamableHTTPServerTransport({
  sessionIdGenerator: undefined,     // 无会话（2026-07-28 规范）
});
await server.connect(transport);
// 由运行时（Express/Serverless 适配器）将请求交给 transport.handleRequest
```

**部署的完成标志**：`tools/list` 冒烟通过 + 健康检查就位 + 认证（Bearer/OAuth）生效——与阶段 3 的部署检查单完全一致，TS 只是换了载体。

## 9. 安全与版本管理

| 实践 | 说明 |
|---|---|
| 锁版本 | package.json 精确版本 + lockfile 提交 |
| 漏洞跟进 | 官方 release notes 订阅（v1.27.1 修命令注入） |
| 依赖审计 | npm audit / OSV 扫描进 CI |
| 传输安全 | 远程走 TLS + Bearer（阶段 3 标准） |
| 输入校验 | Zod 校验优先（Schema 即防线） |

**安全要点**：TS 生态"漏洞发现-修复"循环最快，**生产必须锁版本 + 定期小步升级**——"升级恐惧"与"版本裸奔"之间取"定期小步"。Zod 的双层校验（编译期类型 + 运行期参数）让非法输入在业务逻辑前被拦截，**Schema 安全是语言级内建优势**。

## 10. 生态数据与趋势

| 数据点 | 数值/事实 |
|---|---|
| v1.29 依赖项目 | 3.47 万+ |
| 累计下载 | 破 10 亿 |
| Node 要求 | v1: 22+ |
| v2 状态 | 2.0.0-alpha.2（拆包 beta） |
| 安全 | v1.27.1 修 URL 命令注入（2026-02） |
| 认证 | mcpAuthRouter（OAuth 2.1 + PKCE）唯一开箱即用 |

**趋势判断**：① v2 拆包后依赖更小、端更清晰；② spec 包让"协议版本"成为一等公民——**多协议版本并存是 TS 生态的设计哲学**；③ codemod 迁移工具的成熟度预示 v2 迁移在 TS 生态最顺滑；④ 2026-07-28 规范下，Streamable HTTP 无会话部署（sessionIdGenerator: undefined）成为标准形态。

**生态健康度的三个信号**（判断 TS 生态值不值得长期跟）：① 官方发布节奏——v1.27→v1.29 在 2026 上半年密集迭代，活跃度是生态生命力的直接指标；② 安全响应——命令注入漏洞在 2026-02 修复，漏洞发现-修复闭环快是"有人管"的证据；③ 迁移工具投入——官方为 v2 专门做了 codemod，说明"官方认真对待升级体验"——**三个信号齐备，TS 生态的长期性无需怀疑**，这也是"TS/Python 双雄"格局在框架层的支撑。

## 11. 面试速记与常见坑

**面试速记**：
1. TS SDK 是参考实现——协议行为以它为准，跨语言不一致以 TS 为基准排查。
2. Zod = 类型即 Schema：编译期类型 + 运行期校验双层防护（阶段 3 Schema 层校验的语言级内建）。
3. v2 拆三包：server/client/spec；codemod 机械迁移，无状态改造是正活。
4. mcpAuthRouter = 唯一开箱即用的完整授权服务器（OAuth 2.1 + PKCE）。
5. 类型系统管理协议版本（spec.types.2026-07-28.ts）——升级协议时编译器暴露所有不兼容调用。
6. "挂进现有 Express"是 TS 生态独有的一体化能力（认证/限流/日志复用）。

**常见坑**：
- v2 alpha 上生产（等稳定版，锁版本）；
- 依赖版本不锁（安全修复追不上）；
- HTTP+SSE 新代码还在用（v2 弃用）；
- Zod 描述写太少（Schema 即工具接口，描述决定模型调用质量——阶段 3 规范）；
- 忽略 Express 适配器（能复用的认证/限流生态浪费掉）；
- `express.json()` 放在 MCP 适配器之后（协议解析失败）。

**与阶段体系的衔接**：阶段 2/3/4 的 Python 概念在 TS 生态的对应物——`@mcp.tool` → `registerTool`（Zod 版 Schema）；lifespan/Depends → 闭包/构造函数注入（TS 无内建 DI，用模块级单例）；中间件 → Express 中间件或 SDK 传输中间件；ToolResult(is_error) → outputSchema + 错误对象。**"Python 概念 → TS 形态"的映射表是迁移最短路径**——两套生态的工程方法论完全一致，只有语法外衣不同（08 篇五要素共性的实例验证）。

**完成标志**：能独立完成"带认证的远程 Server"闭环（McpServer + mcpAuthRouter + Streamable HTTP + Express 挂载）并说清每个组件的作用；能讲出 v1.29 四大能力、v2 三包职责、TS 的三个"为什么"（参考实现/拆包/认证强）——"完整链路 + 完整生态"双全，TS 生态篇毕业。

> 🎯 **核心要点**：TS 生态 = 参考实现（协议行为基准）+ v1.29 完整能力（Zod 类型即 Schema/三传输/OAuth 2.1 PKCE/Express 适配）+ 工具链最全（Inspector/codemod 原生）+ v2 拆包（server/client/spec 三包，类型系统管理协议版本）+ 安全纪律（锁版本 + 定期升级 + 依赖审计）。**TS 是"认证能力最强、调试体验最好、v2 变化最大"的生态**——前端/全栈选它自然，后端团队按 07 篇决策树权衡；记住三个"为什么"（参考实现源于历史生态、拆包源于职责清晰、认证强源于 Web 生态需求），因果链比事实表更能扛住追问。

---

**下一模块**：[04-Java 生态：MCP Java SDK 与 Spring AI](04-Java 生态：MCP Java SDK 与 Spring AI.md) / **返回总览**：[00-主流MCP开发框架知识体系总览](00-主流MCP开发框架知识体系总览.md)
