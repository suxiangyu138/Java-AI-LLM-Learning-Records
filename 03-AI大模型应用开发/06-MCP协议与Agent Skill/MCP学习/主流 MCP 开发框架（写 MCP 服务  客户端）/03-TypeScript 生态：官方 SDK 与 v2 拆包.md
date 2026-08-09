# 03 TypeScript 生态：官方 SDK 与 v2 拆包

> TS 是 MCP 的参考实现生态：v1.29 稳定线的完整能力（Zod/传输/OAuth/Express 适配），v2 拆包（server + client 双包）与 codemod 迁移，Server 与 Client 双端实战。

## 📚 目录

1. [TS 生态定位](#1-ts-生态定位)
2. [v1.29：完整能力清单](#2-v129完整能力清单)
3. [Server 实战：McpServer + Zod](#3-server-实战mcpserver--zod)
4. [Client 实战与多传输](#4-client-实战与多传输)
5. [OAuth 2.1 与 Express 适配](#5-oauth-21-与-express-适配)
6. [v2 拆包与迁移](#6-v2-拆包与迁移)

## 1. TS 生态定位

**TS 是 MCP 的"参考实现"**——协议规范的行为定义以 TS SDK 为准，其他语言对照实现。2026 数据：v1.29 有 3.47 万+ 依赖项目、累计下载破 10 亿、Node 22+。

| 特性 | 说明 |
|---|---|
| 参考实现 | 协议行为的基准（其他 SDK 对照它） |
| 生态最大 | npm 生态 + 前端/全栈开发者基数 |
| 双端齐全 | Server 与 Client 都是 Tier 1 |
| v2 激进 | 拆包（server/client）是四 Tier 1 中变化最大的 |

**TS 生态的"两面"**：对前端/全栈开发者是主场（用 TypeScript 写 MCP 最自然）；对纯后端团队是"又多一个 Node 服务"（权衡见 07 篇决策树）。

## 2. v1.29：完整能力清单

| 能力 | 实现 |
|---|---|
| 传输 | stdio / HTTP+SSE / Streamable HTTP |
| Schema | Zod 自动转 JSON Schema（类型安全） |
| 框架 | McpServer（高层）/ Server（低层） |
| 客户端 | Client + 各传输客户端 |
| 认证 | OAuth 2.1 with PKCE（mcpAuthRouter） |
| 集成 | Express 适配器（服务器挂进现有 Express） |
| 工具链 | Inspector、CLI |

**v1.27.1 安全要点**（2026-02）：修复 URL 处理命令注入漏洞、补认证预注册一致性测试、SEP-1730 文档——**TS SDK 是安全漏洞的高发区也是最快修复区**（生态大=攻击面大），生产务必锁版本跟进。

## 3. Server 实战：McpServer + Zod

```typescript
// v1.29：McpServer（高层）
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

const transport = new StdioServerTransport();
await server.connect(transport);
```

**Zod 的价值**：类型定义即 Schema——`z.string().describe()` 同时产生 TS 类型、JSON Schema 与工具描述（阶段 3 的 Schema 规范在 TS 生态由 Zod 原生承载）；`outputSchema` 约束返回结构（比 Python 的返回注解更显式）。

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

**传输选择**：stdio（本机子进程）、Streamable HTTP（远程，2026 规范唯一远程传输）、HTTP+SSE（v1 保留、v2 弃用）——**新代码一律 Streamable HTTP**（阶段 3 结论一致）。

## 5. OAuth 2.1 与 Express 适配

```typescript
// OAuth：mcpAuthRouter 完成授权服务器职责（PKCE）
import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { mcpAuthRouter } from "@modelcontextprotocol/sdk/auth/mcpAuthRouter.js";

const server = new McpServer({ name: "secure-demo", version: "1.0" });
const authRouter = mcpAuthRouter({ serverInfo: { name: "secure-demo", version: "1.0" } });

// Express 集成：同一服务挂 MCP 端点 + 认证端点
app.use("/mcp", express.json(), authRouter);      // 认证 + MCP 端点
app.listen(3000);
```

**TS 生态的认证特点**：`mcpAuthRouter` 内建授权服务器流程（OAuth 2.1 + PKCE）——**TS SDK 是唯一把"完整授权服务器"做成开箱即用的 Tier 1 SDK**（Python 官方 v1 没有，FastMCP 是 OAuth Proxy 路线）；做"对外服务的 MCP"用 TS 生态认证最省事。

## 6. v2 拆包与迁移

**v2 的最大变化：包拆分**：

```text
v1：@modelcontextprotocol/sdk（一包装全部）
v2：@modelcontextprotocol/server（服务端）
    @modelcontextprotocol/client（客户端）
    @modelcontextprotocol/spec（规范类型：spec.types.2025-11-25.ts / 2026-07-28.ts）
```

**v2 的三大设计**：① 拆包（按端安装，依赖更小）；② Schema 校验一等公民（不再是可选）；③ 规范类型按版本追踪（`spec.types.2026-07-28.ts`——协议版本与代码强绑定）。

```bash
# v1 → v2 机械迁移（官方 codemod）
npx @modelcontextprotocol/codemod@beta v1-to-v2 .
```

**迁移节奏建议**：v2 目前 2.0.0-alpha.2（beta），**生产项目锁 v1.29 等 v2 稳定**（阶段 3 同款"等稳定再迁"）；新项目可 alpha 试用但别上生产——**TS 生态的迁移红利是 codemod 自动化的机械部分**，剩下的"无状态改造"（删会话、补 resultType）与其他语言一致（07 篇清单）。

## 7. 工具链与调试生态

TS 生态的配套工具（写 Server 的效率保障）：

| 工具 | 用途 | 使用方式 |
|---|---|---|
| MCP Inspector | 可视化调试（工具/资源/提示词） | `npx @modelcontextprotocol/inspector` |
| CLI（官方） | 脚手架/冒烟 | `npx @modelcontextprotocol/cli` |
| codemod | v1→v2 迁移 | `npx @modelcontextprotocol/codemod@beta v1-to-v2 .` |
| TypeScript 类型 | 协议类型即文档 | `spec.types.2026-07-28.ts`（v2） |
| npm 生态 | 复用一切 Node 库 | 与普通 TS 服务无异 |

**TS 生态的调试优势**：Inspector 原生支持（TS 是参考实现，工具链最全）+ npm 调试工具（tsx/ts-node/vitest）直接可用——**TS 写 MCP 的调试体验在六生态中最好**，这也是前端团队选它的隐形理由。

**TS 生态的工程细节**：类型安全贯穿全程——Zod Schema 在编译期约束参数类型、在运行期校验输入（双层防护）；`outputSchema` 让工具返回结构也受类型约束（Python 生态的返回注解没有这么严格）；**协议类型按版本追踪**（v2 的 `spec.types.2026-07-28.ts`）意味着"协议升级 = 类型变更"，编译器会在你升级依赖时暴露所有不兼容点——**类型系统把"迁移的语义风险"提前到编译期**，这是 TS 生态在 v2 迁移中相对 Python 的结构性优势。对团队的意义：TS 团队做 MCP 的"回归保障"有一半靠编译器，另一半靠测试；Python 团队则几乎全靠测试——**"类型即文档、编译即回归"是 TS 生态的最大工程红利**。

**TS 生态的面试表达**：被问"TS 生态的独特价值"时，三个点足够——① 参考实现（协议行为以它为准，遇到跨语言不一致以 TS 为基准排查）；② Zod 类型即 Schema（编译期+运行期双层校验，参数安全是语言级内建）；③ mcpAuthRouter（OAuth 2.1 + PKCE 开箱即用，是四 Tier 1 中认证最省事的）。**再加一个工程点**：v2 的 spec 包把协议版本做成类型文件（spec.types.2026-07-28.ts），升级协议时编译器直接报出所有不兼容调用——**"用类型系统管理协议版本"是 TS 生态对 MCP 工程最独特的贡献**，讲出这一点，面试深度立刻拉开。

**TS 生态的"为什么"**：① 为什么 TS 是参考实现——"历史 + 生态"：MCP 起源的 Anthropic 生态与前端生态同源，TS 自然成为第一个完整实现，参考实现地位由此固化；② 为什么 v2 拆包——"职责清晰"：一包装两端（server+client）在依赖体积与 API 面都臃肿，拆包让"服务端项目不装客户端代码"，同时 spec 独立成包让"协议版本"可独立追踪；③ 为什么 TS 认证最强——"生态需求"：TS 是 Web 生态语言，OAuth/PKCE 是 Web 基础设施的既成惯例，mcpAuthRouter 是"把 Web 的认证能力带进 MCP"的自然结果——**三个"为什么"与 Python 篇的因果链同构：历史 → 演进 → 结构，回答完因果链，任何追问都落在这三条线上**。

**TS 生态的常见问题快答**（面试补充）：① "TS 写 MCP 与写普通 Node 服务什么区别？"——多一层协议层（McpServer 注册/传输连接），其余（依赖/部署/调试）完全一致，**TS 团队接入 MCP 的成本最低**；② "v2 alpha 能用吗？"——试用可以，生产等稳定（约 2026-08 后）；③ "HTTP+SSE 还有必要学吗？"——v2 弃用，新代码不学；④ "Express 项目怎么接 MCP？"——适配器挂载（`app.use("/mcp", ...)`），认证/限流复用 Express 中间件——**"挂进现有 Web 服务"是 TS 生态最顺手的一体化形态**；⑤ "前端项目有必要自己写 Server 吗？"——取决于场景，前端团队写"浏览器/前端工具类 Server"（页面状态、设计资源）是自然延伸，写企业后端工具则交给后端团队（07 篇决策树：团队栈与端匹配）。

**TS 生态与阶段体系的衔接**：阶段 2/3/4 的 Python 代码（FastMCP）在 TS 生态的对应物——`@mcp.tool` 对应 `registerTool`（Zod 版 Schema）、lifespan/Depends 对应闭包/依赖注入（TS 生态无内建 DI，用模块级单例或构造函数注入）、中间件对应 Express 中间件或 SDK 中间件（v1.29 支持传输中间件）、ToolResult(is_error) 对应 outputSchema + 错误对象——**"Python 概念 → TS 形态"的映射表是 TS 团队从阶段体系迁移的最短路径**；反过来，TS 团队读完阶段体系再回看 03 篇，每个概念也都有落点——**两套生态的工程方法论完全一致，只有语法外衣不同**（08 篇五要素共性的实例验证）。**03 篇的完成标准**：能脱稿讲出 v1.29 的四大能力（Zod/传输/OAuth/Express）、v2 拆包的三包职责、TS 的三个"为什么"（参考实现/拆包/认证强）——三项齐备，TS 生态篇毕业。**03 篇与阶段体系的对照**：TS 团队用阶段 2 的"最小 Server"流程跑一遍 TS 版（McpServer + Zod + StdioServerTransport），再对比 08 篇的 TS 天气实现——**阶段体系教你"链路"，03 篇教你"生态"**：链路（环境→Server→Client→调试）与生态（Zod/OAuth/Express/v2）合起来，TS 团队的 MCP 能力就是"会跑链路 + 懂生态选择"的双层结构——**这与 Python 读者的"FastMCP 熟练 + 官方低层理解"是完全对称的能力模型**（08 篇五要素共性的又一验证）。**TS 生态的完成标志**：能独立完成"带认证的远程 Server"（McpServer + mcpAuthRouter + Streamable HTTP + Express 挂载）并说清每个组件的作用——**"完整链路 + 完整生态"双全，TS 生态篇才算真正毕业**。

## 8. 传输与部署细节

### 8.1 传输选型

三种传输的定位与选择（2026 规范下只有前两种是"正道"）：

| 传输 | 场景 | 部署形态 | 选择理由 |
|---|---|---|---|
| stdio | 本机子进程（Claude Code/编辑器） | `claude mcp add` 直接指启动命令 | 零网络面、零配置、安全面最小 |
| Streamable HTTP | 远程（**2026 唯一远程传输**） | Node 服务 / Docker / Serverless | 无状态规范的正统远程形态 |
| HTTP+SSE | 存量项目 | 迁移到 Streamable | v2 已弃用，新代码不碰 |

**选型原则**：**本机用 stdio、对外用 Streamable HTTP**——与阶段 3 的结论完全一致；HTTP+SSE 只在"必须兼容旧客户端"时保留（v2 弃用窗口内），新项目一律 Streamable。

### 8.2 部署形态详表

| 形态 | 配置要点 | 适用 |
|---|---|---|
| 本机进程 | `node server.js` 或 `npx tsx server.ts` | 开发/个人 |
| Docker 容器 | Node 22+ 镜像、非 root、健康检查 | 生产标准 |
| Serverless | 无状态规范红利：冷启动可接受、状态外置 | 低频/突发 |
| Express 挂载 | `app.use("/mcp", ...)` 适配器 | **与现有 Web 服务一体**（TS 独有） |

### 8.3 部署注意点

1. **Node 版本**：v1 要求 Node 22+——生产镜像锁 Node LTS（22/24 线）；
2. **健康检查**：MCP 端点探活（`tools/list` 冒烟）比进程存活检查更真实（阶段 3 的"就绪要真实"原则）；
3. **传输切换**：同一套 Server 代码，stdio 与 HTTP 只差 transport 类——**"传输即配置"跨框架成立**（08 篇对照）；
4. **Express 挂载的注意事项**：`express.json()` 中间件必须在 MCP 适配器之前（JSON body 解析是协议前提）；认证/限流/日志中间件与普通 API 路由共用一套——**"MCP 端点 = 又一个 Express 路由"的心智让 TS 部署零学习成本**；
5. **Serverless 的边界**：长连接/流式场景受平台限制（超时上限），普通工具调用无碍——**选 Serverless 前先看工具是否全部秒级返回**（阶段 3 同款判断）。

### 8.4 部署示例

```typescript
// Streamable HTTP 独立部署（Serverless 友好）
import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StreamableHTTPServerTransport } from
  "@modelcontextprotocol/sdk/server/streamableHttp.js";

const server = new McpServer({ name: "weather", version: "1.0.0" });
// 无状态规范：每个请求独立 transport，不维护会话
const transport = new StreamableHTTPServerTransport({
  sessionIdGenerator: undefined,     // 无会话（2026-07-28 规范）
});
await server.connect(transport);
// 由运行时（Express/Serverless 适配器）将请求交给 transport.handleRequest
```

**部署的完成标志**：`tools/list` 冒烟通过 + 健康检查就位 + 认证（Bearer/OAuth）生效——**与阶段 3 的部署检查单完全一致，TS 只是换了载体**。

## 9. 面试速记与常见坑

**面试速记**：
1. TS SDK 是参考实现——协议行为以它为准。
2. Zod = 类型即 Schema（TS 类型的 JSON Schema 化）。
3. v2 拆三包：server/client/spec；codemod 机械迁移。
4. mcpAuthRouter = 唯一开箱即用的完整授权服务器（OAuth 2.1 + PKCE）。
5. 安全：v1.27.1 修了命令注入——TS 生态漏洞修复快但必须锁版本跟进。

**常见坑**：
- v2 alpha 上生产（等稳定版）；
- 依赖版本不锁（安全修复追不上）；
- HTTP+SSE 新代码还在用（v2 弃用）；
- Zod 描述写太少（Schema 即工具接口，描述决定模型调用质量）；
- 忽略 Express 适配器（能复用的认证/限流生态浪费掉）。

## 10. 安全与版本管理

TS 生态的安全实践（攻击面大 = 纪律更严）：

| 实践 | 说明 |
|---|---|
| 锁版本 | package.json 精确版本 + lockfile 提交 |
| 漏洞跟进 | 官方 release notes 订阅（v1.27.1 修命令注入） |
| 依赖审计 | npm audit / OSV 扫描进 CI |
| 传输安全 | 远程走 TLS + Bearer（阶段 3 标准） |
| 输入校验 | Zod 校验优先（Schema 即防线） |

**安全要点**：TS 生态是"漏洞发现-修复"循环最快的（生态大=攻击面大），**生产项目必须锁版本 + 定期升级**——"升级恐惧"与"版本裸奔"之间取"定期小步升级"。**Zod 的校验安全价值**：Zod Schema 不只是"类型即文档"，它还是运行时校验器——工具参数在进入 handler 前经过 Zod 校验（类型/枚举/长度），**非法参数在业务逻辑之前就被拦截**，这与阶段 3 的"Schema 层校验"完全同构；v2 把"Schema 校验一等公民"写进设计（校验从可选变默认），进一步强化了"输入不可信"的安全基线——**TS 生态的 Schema 安全是语言级的内建优势**，值得其他生态借鉴。

## 11. 生态数据与趋势

| 数据点 | 数值/事实 |
|---|---|
| v1.29 依赖项目 | 3.47 万+ |
| 累计下载 | 破 10 亿 |
| Node 要求 | v1: 22+ |
| v2 状态 | 2.0.0-alpha.2（拆包 beta） |
| 安全 | v1.27.1 修 URL 命令注入（2026-02） |
| 认证 | mcpAuthRouter（OAuth 2.1 + PKCE）唯一开箱即用 |

**趋势判断**：① v2 拆包后**依赖更小、端更清晰**（服务端不用装客户端代码）；② spec 包让"协议版本"成为一等公民（`spec.types.2026-07-28.ts`）——**多协议版本并存是 TS 生态的设计哲学**；③ codemod 迁移工具的成熟度预示 v2 迁移在 TS 生态最顺滑。

**TS 生态的实操路径建议**：前端/全栈团队做 MCP 时，Server 端用 v1.29 的 McpServer + Zod（生产稳定），Client 端用同 SDK 的 Client（自研 Agent 时复用阶段 4 的编排器心智）；需要对外服务的认证时直接上 mcpAuthRouter（OAuth 2.1 + PKCE 开箱即用，省掉自建授权服务器的全部工作）；v2 稳定后跑 codemod 迁移（机械部分自动化），再按 07 篇七步清单做无状态改造。**TS 生态的独特价值是"全栈一体"**：同一个仓库里写前端、写 MCP Server、写 Agent 客户端，类型系统（Zod 到 API 层）贯穿全程——这正是参考实现生态对全栈团队的吸引力所在。

> 🎯 **核心要点**：TS 生态 = 参考实现（协议行为基准）+ v1.29 完整能力（Zod 类型即 Schema/Streamable HTTP/OAuth 2.1 PKCE/Express 适配）+ 工具链最全（Inspector/codemod 原生）+ v2 拆包（server/client/spec 三包 + codemod 迁移）+ 安全纪律（锁版本 + 定期升级 + 依赖审计）。**TS 是"认证能力最强、调试体验最好、v2 变化最大"的生态**——前端/全栈选它自然，后端团队按 07 篇决策树权衡；v2 的 spec 包设计（协议版本一等公民）值得所有生态借鉴。

---

**下一模块**：[04-Java 生态：MCP Java SDK 与 Spring AI](04-Java 生态：MCP Java SDK 与 Spring AI.md) / **返回总览**：[00-主流MCP开发框架知识体系总览](00-主流MCP开发框架知识体系总览.md)
