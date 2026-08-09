# 04 Java 生态：MCP Java SDK 与 Spring AI

> 本库主线读者的主场：官方 MCP Java SDK（三层架构、版本线）与 Spring AI 的 MCP 全家桶（starter 矩阵、@Tool 注解、同步/异步）——从依赖坐标到可运行 Server 的完整路径。

## 📚 目录

1. [Java 生态定位](#1-java-生态定位)
2. [MCP Java SDK：三层架构](#2-mcp-java-sdk三层架构)
3. [版本线与 2026 进展](#3-版本线与-2026-进展)
4. [Spring AI MCP：starter 矩阵](#4-spring-ai-mcpstarter-矩阵)
5. [注解式开发：@Tool 与能力注解](#5-注解式开发tool-与能力注解)
6. [实战：最小 Java Server](#6-实战最小-java-server)
7. [Java 生态选型](#7-java-生态选型)

## 1. Java 生态定位

**Java 是 MCP 生态的"企业后端"担当**：Tier 2（官方维护、与 Spring AI 团队合作），生态重心在 Spring——**写 MCP 的 Java 开发者几乎必然遇到 Spring AI**。与阶段 4 的结论呼应：Java 后端做 AI 应用，MCP 是工具层的标准通道。

| 维度 | 现状 |
|---|---|
| Tier | 2（官方维护，合规性次一级于 Tier 1） |
| SDK | io.modelcontextprotocol.sdk（mcp-core/mcp-json-jackson3/…） |
| 高层框架 | Spring AI（2.0.0-M3+ 内建 MCP 支持） |
| 定位 | 企业后端、Spring 生态无缝集成 |
| 2026 状态 | 1.1.0 GA（2026-03）；2025-11-25 规范支持进行中 |

## 2. MCP Java SDK：三层架构

```
┌─────────────────────────────────────────┐
│ Client/Server 层                          │
│   McpClient（同步/异步）                    │
│   McpSyncServer / McpAsyncServer          │
├─────────────────────────────────────────┤
│ Session 层                                │
│   McpSession / McpClientSession /         │
│   McpServerSession（通信模式与连接状态）     │
├─────────────────────────────────────────┤
│ Transport 层                              │
│   McpTransport（JSON-RPC 序列化/反序列化）   │
│   STDIO / HTTP+SSE / Streamable-HTTP      │
└─────────────────────────────────────────┘
```

**三层设计要点**：顶层是开发者 API（Sync/Async 双模式），中层管理会话，底层可插拔传输；**JSON 序列化用 Jackson（抽象在 mcp-core，默认 mcp-json-jackson3）**；公开 API 用响应式流（Project Reactor），同步门面供阻塞场景——**"响应式内核 + 同步门面"是 Java SDK 的工程特色**（与 WebFlux 同哲学）。

**能力覆盖**：工具/资源（URI 模板）/提示词/补全/日志/进度/Ping——**七大能力全部实现**（与 Spring AI 文档一致）。

## 3. 版本线与 2026 进展

| 版本 | 时间 | 内容 |
|---|---|---|
| 1.0.0 GA | 2026-02 | 首个稳定版（语义化版本线开始） |
| 1.1.0 GA | 2026-03-14 | 支持 2025-06-18 规范；版本协商覆盖 2024-11-05/2025-03-26/2025-06-18 |
| 进行中 | 2026-03 后 | 2025-11-25 规范支持 |
| Spring AI 2.0.0-M3 | 2026-03-17 | MCP 注解迁入核心、传输实现迁入 Spring AI、Jackson 3 |

**2026-03-17 的破坏性变更**（Spring AI 2.0.0-M3，迁移注意）：
1. MCP 注解包：`org.springaicommunity.mcp` → `org.springframework.ai.mcp.annotation`（社区注解项目并入核心）；
2. Spring 专用传输实现：从 MCP Java SDK（io.modelcontextprotocol.sdk）迁入 Spring AI（org.springframework.ai.mcp）；
3. Jackson 2 → Jackson 3 迁移。

**含义**：MCP 在 Spring 世界"官方化"——**注解与传输成为 Spring AI 核心的一部分**，社区项目（spring-ai-community/mcp-annotations）使命完成。

## 4. Spring AI MCP：starter 矩阵

**客户端 starters**：

| Starter | 传输支持 | 场景 |
|---|---|---|
| spring-ai-starter-mcp-client | STDIO / Servlet Streamable-HTTP / Stateless Streamable-HTTP / SSE | 标准客户端 |
| spring-ai-starter-mcp-client-webflux | WebFlux Streamable-HTTP / Stateless / SSE | 响应式客户端 |

**服务端 starters**：

| Starter | 配置 | 传输 |
|---|---|---|
| spring-ai-starter-mcp-server | `spring.ai.mcp.server.stdio=true` | STDIO |
| spring-ai-starter-mcp-server-webmvc | `spring.ai.mcp.server.protocol=SSE` | SSE（WebMVC） |
| 同上 | `...protocol=STREAMABLE` | Streamable-HTTP |
| 同上 | `...protocol=STATELESS` | **无状态**（2026-07-28 规范） |
| spring-ai-starter-mcp-server-webflux | `...protocol=SSE/STREAMABLE/STATELESS` | 响应式三模式 |

**关键配置项**：`spring.ai.mcp.server.type=SYNC/ASYNC`（同步/异步服务器）；`spring.ai.mcp.client.*`（客户端连接配置）。**STATELESS 模式直接支持 2026-07-28 无状态规范**——Spring 生态对新规范的跟进速度是 Java 选型的加分项。

## 5. 注解式开发：@Tool 与能力注解

```java
// 注解式 MCP Server：方法即工具（Spring AI 2.x）
@Service
public class WeatherTool {
    @Tool(description = "查询城市天气。何时用：用户问天气时。")
    public String getWeather(@ToolParam(description = "城市名") String city) {
        return city + ": 晴，25°C";
    }
}
```

**能力注解全家桶**（Spring AI MCP）：`@Tool`（工具）、`@Resource`（资源）、`@Prompt`（提示词）、`@Completion`（补全）、`@LoggingHandler`、`@ProgressHandler`、`@SamplingHandler`、`@ElicitationHandler`、`@ListChangedHandler`——**注解覆盖 MCP 全部能力**，且 Bean 扫描自动注册（`ToolCallbackProvider` 装配）。

**注解式的价值**：与 FastMCP 装饰器同哲学（阶段 3 的 Schema 规范在 Java 由注解+参数描述承载）——**Java 后端写 MCP 的成本降到"写一个 Service 方法"**。

## 6. 实战：最小 Java Server

```xml
<!-- pom.xml 依赖坐标 -->
<dependency>
  <groupId>org.springframework.ai</groupId>
  <artifactId>spring-ai-starter-mcp-server</artifactId>
  <version>1.1.3</version>   <!-- 或 2.0.0-M3+ -->
</dependency>
```

```yaml
# application.yml：Streamable HTTP 服务端
spring:
  ai:
    mcp:
      server:
        protocol: STREAMABLE
        type: ASYNC
```

```java
@SpringBootApplication
public class McpServerApp {
    public static void main(String[] args) {
        SpringApplication.run(McpServerApp.class, args);
    }
}

@Configuration
public class McpConfig {
    @Bean
    ToolCallbackProvider toolCallbacks(List<WeatherTool> tools) {
        return MethodToolCallbackProvider.builder().toolObjects(tools).build();
    }
}
```

**客户端侧**（阶段 4 的编排器对应物）：`spring-ai-starter-mcp-client` + `servers-configuration` JSON（Claude Desktop 格式）——启动即连接并管理 Server 生命周期（stdio）；Streamable HTTP 客户端走 `McpClient`。

## 7. Java 生态选型

| 需求 | 选型 | 理由 |
|---|---|---|
| Spring 项目写 Server | Spring AI starter | 注解式 + 自动装配 + 配置即传输 |
| 非 Spring 纯 SDK | MCP Java SDK 直接 | 三层架构 + Sync/Async |
| 协议级定制 | Java SDK 底层（Transport 层） | 可插拔传输/序列化 |
| 学习协议 | Java SDK 低层写一遍 | 对照阶段 2 的 Python 路径 |
| 无状态规范 | STATELESS 模式（starter） | 2026-07-28 day-one 级支持 |

## 8. 客户端实战详解

Spring AI 客户端的两种形态（对应阶段 4 编排器在 Java 的落法）：

```yaml
# 形态一：stdio 客户端（启动时拉起 Server 子进程）
spring:
  ai:
    mcp:
      client:
        servers-configuration: classpath:servers-config.json
# servers-config.json 用 Claude Desktop 格式声明 Server 命令

# 形态二：Streamable HTTP 客户端（连远程 Server）
spring:
  ai:
    mcp:
      client:
        connections:
          - server: weather-server
            url: http://localhost:8080/mcp
            transport: streamable-http
```

```java
// 客户端使用：注入工具回调 → 模型自动调用
@Bean
ToolCallbackProvider weatherTools(McpClient mcpClient) {
    return new ToolCallbackProvider(mcpClient.getTools());   // 工具注入 AI 对话
}
```

**Java 客户端的价值**：与阶段 4 的自研编排器相比，Spring AI 客户端把"连 Server → 取工具 → 注入模型"变成配置项+Bean——**Java 后端做 Agent 的工具层时，Spring AI MCP 客户端是最高效的路径**（与阶段 4 的 Python 编排器互为参照）。

**Java 生态的企业级考量**：企业后端选 Java 做 MCP 的理由往往不是框架本身，而是**既有基础设施的复用**——Spring Security（认证）、Actuator（监控）、配置中心（环境管理）、灰度发布（流量控制）全部直接作用于 MCP Server，这是 Python/TS 生态需要自行拼装的能力；对应的代价是**启动与资源开销**（JVM 启动秒级、内存百 MB 级）——"每工具一进程"的轻量部署形态在 Java 不成立（那是 Go 的场景）。**企业级选型的一句话**：Java 的 MCP 不是"又一种写 Server 的方式"，而是"Spring 应用暴露能力给 AI 的方式"——把 MCP 当 Spring 生态的一个新端点类型理解，而不是一个新框架理解，才不会选错。

**Java 生态的面试表达**：被问"Java 怎么做 MCP"时，回答的完整链条——① 协议层：官方 MCP Java SDK（三层架构：Client/Server→Session→Transport，响应式内核+同步门面）；② 框架层：Spring AI starter（注解式 @Tool 全能力、配置化传输）；③ 规范跟进：STATELESS 模式直接支持 2026-07-28 无状态规范；④ 工程集成：Spring Security（认证）/Actuator（监控）/Boot（部署）全复用；⑤ 版本现状：Java SDK 1.1.0 GA（2026-03），Spring AI 2.0.0-M3 完成 MCP 官方化迁移。**五个链条点每个都能接追问**（三层架构怎么分层/@Tool 怎么注册/STATELESS 怎么配/与 1.x 的迁移）——面试官顺着任何一点往下问，你都站在"读过 04 篇"的完整知识上。这也是本库主线读者最该吃透的一篇。

**Java 生态的"为什么"**：① 为什么 Java 是 Tier 2 而不是 Tier 1——"定级时机的产物"：Tier 定级发生在 2026-07-28 规范定稿期，Java SDK 当时刚 1.1.0（2026-03），合规跟进尚未到 day-one 条件；**Tier 2 不代表能力差**——Spring AI 的 STATELESS 模式证明 Java 生态的实际跟进速度不落后；② 为什么 Java 的 MCP 重心在 Spring AI 而非 SDK——"框架生态的引力"：Java 企业开发的一切都经 Spring 消化（Web/MVC/Cloud 皆如此），MCP 被 Spring 化是同一规律的第三次重演；③ 为什么 Java 适合企业 MCP——"基础设施复用"：Security（认证）、Actuator（监控）、Boot（部署）三大件直接作用于 MCP Server，这是其他生态需要自行拼装的能力——**三个"为什么"讲完，Java 生态的"企业后端担当"定位就完整了：定级时机（历史）、Spring 引力（演进）、基础设施复用（结构）**。

**Java 生态的常见问题快答**（面试补充）：① "写 MCP Server 一定要用 Spring AI 吗？"——非 Spring 项目用 Java SDK 直接写（三层架构 API 自足），Spring 项目用 starter（注解+配置最省）；② "STATELESS 模式与 Streamable 什么关系？"——STATELESS 是 2026-07-28 无状态规范的传输模式（无握手无会话），Streamable 是传统远程传输——**新项目直接 STATELESS**；③ "Java SDK 的响应式是必须的吗？"——公开 API 是响应式（Reactor），同步门面（McpSyncServer）覆盖阻塞场景，**普通业务用同步门面即可**；④ "Spring AI 2.0 与 1.x 怎么选？"——新项目 2.0.0-M3+（注解入核心、Jackson 3），存量 1.x 按包路径迁移；⑤ "Java 写 MCP 的性能够吗？"——业务 Server 足够（下游系统才是瓶颈），高吞吐数据面才考虑 Rust/Go——**"Java 慢"是刻板印象，MCP 场景的性能差异可忽略**。

**Java 生态的实操路径**（本库 Java 后端主线的完整落地）：① 学协议——用 Java SDK 低层写一遍最小 Server（对照阶段 2 的 Python 路径，理解 list_tools/call_tool 的 Java 形态）；② 写业务——Spring AI starter（@Tool 注解 + STATELESS 配置），生产级清单（认证/只读默认/测试）按阶段 3 方法论落地；③ 做客户端——spring-ai-starter-mcp-client 接入（配置化注入工具到模型对话，对应阶段 4 编排器）；④ 进生产——Spring Security（认证）+ Actuator（监控）+ Boot（部署），全链路复用既有基础设施——**四条路径每一步都有既有知识打底（阶段 1-4 + 本体系），Java 后端做 MCP 没有"从零学"的环节，只有"换外衣"的环节**。**04 篇的完成标准**：能脱稿讲出 Java SDK 三层架构、Spring AI starter 矩阵（server/client × 传输）、@Tool 注解的价值、STATELESS 配置、2026-03-17 三大变更——五项齐备，Java 生态篇毕业（本库主线读者必达）。**04 篇与主体系 Spring AI 的关系**：本知识库 `02-.../06-Spring全家桶/Spring AI` 体系（11 篇）有 Spring AI 的完整模块（含 MCP 集成），04 篇是"MCP 视角"的浓缩——**MCP 在 Java 的深度实践（配置细节/排错/生产案例）查 Spring AI 体系，框架定位与选型逻辑查 04 篇**——两套体系的分工：Spring AI 体系讲"Spring AI 怎么用"，04 篇讲"MCP 框架在 Java 生态怎么选"。**04 篇的完成标志**：能独立完成"Spring AI 的 STATELESS Server + @Tool 工具 + 客户端接入"的最小闭环（加依赖→注解→配置→启动→客户端调用），并说清每一步对应 MCP 的哪个协议概念——**"会配 + 懂理"双全，Java 生态篇才算真正毕业**（本库主线读者的必修闭环）。**关于 Tier 2 的最终态度**：Java 的 Tier 2 身份不影响任何实际使用——STATELESS 模式的 day-one 支持、Spring 生态的集成深度、1.1.0 GA 的稳定版本线，三项加起来让 Java 的 MCP 体验不输 Tier 1；**Tier 是官方的合规承诺分级，不是语言的能力排名**——本库 Java 主线读者不必为 Tier 纠结，按 04 篇路径走即可。

## 9. 配置速查表

| 配置 | 值 | 作用 |
|---|---|---|
| spring.ai.mcp.server.protocol | STREAMABLE / SSE / STATELESS | 服务端传输模式 |
| spring.ai.mcp.server.type | SYNC / ASYNC | 同步/异步服务器 |
| spring.ai.mcp.server.stdio | true | STDIO 模式 |
| spring.ai.mcp.server.capabilities.* | true（默认全开） | 能力开关（工具/资源/提示词…） |
| spring.ai.mcp.client.connections | 列表 | 客户端连接配置 |
| spring.ai.mcp.client.servers-configuration | classpath 路径 | 本地 Server 拉起配置 |

**配置哲学**：**Spring AI 把 MCP 的"传输/能力/模式"全部配置化**——同一套代码换协议=改一行配置（STREAMABLE→STATELESS），这是"配置即架构"的 Spring 风格。

## 10. 面试速记与常见坑

**面试速记**：
1. Java SDK 三层：Client/Server → Session → Transport（响应式内核+同步门面）。
2. Spring AI starter 矩阵：server/client × webmvc/webflux × 四传输。
3. @Tool 注解 = 方法即工具（Bean 扫描自动注册）。
4. STATELESS 模式 = 2026-07-28 无状态规范支持（Spring 跟进快）。
5. 2026-03-17 迁移：注解包迁入核心（org.springframework.ai.mcp.annotation）+ Jackson 3。
6. Java 最短路径：加 starter → @Tool → 配 protocol=STATELESS → 部署。

**常见坑**：
- WebFlux starter 与 Tomcat 冲突（用 spring-boot-starter + Netty，官方示例踩过）；
- 版本混用（1.x 与 2.0.0-M3 的包路径不同：org.springaicommunity.mcp → org.springframework.ai.mcp）；
- 忘记注册 ToolCallbackProvider（注解了但没装配）；
- 同步/异步模式选错（阻塞代码配 ASYNC 会卡事件循环）；
- 只知注解不知 SDK（需要协议级定制时无从下手）。

## 11. 与 Spring AI 1.x 的对比

| 维度 | Spring AI 1.x | Spring AI 2.0.0-M3+ |
|---|---|---|
| MCP 注解包 | org.springaicommunity.mcp（社区） | org.springframework.ai.mcp.annotation（核心） |
| 传输实现 | MCP Java SDK 内 | Spring AI 内 |
| Jackson | 2 | 3 |
| MCP 支持 | 基础 | 全面（STATELESS 模式） |
| 迁移成本 | — | 包路径替换 + 依赖调整 |

**对比启示**：**Spring AI 2.x 是 Java 生态的 v2**（与协议 v2 同步的框架官方化）——**新项目直接 2.x 起步**（M3+，等 GA 锁版）；1.x 存量项目按包路径迁移（机械替换为主）。**注解能力的完整价值**：@Tool 之外的能力注解（@Resource/@Prompt/@Completion/@ProgressHandler 等）让 Spring 开发者用"声明式"覆盖 MCP 全部七大能力——**注解覆盖协议能力 = 框架把协议复杂度全部吸收**；这也意味着"注解不会用"不等于"协议不会"——需要协议级理解时回到 Java SDK 低层类（04 篇三层架构），注解是效率面，SDK 是控制面，两者互补不互斥。

## 12. 生态数据与趋势

| 数据点 | 数值/事实 |
|---|---|
| Java SDK 版本 | 1.0.0 GA（2026-02）→ 1.1.0（2026-03） |
| Spring AI 版本 | 1.0.4/1.1.3 稳定 + 2.0.0-M3 |
| 规范支持 | 2025-06-18（1.1.0）；2025-11-25 进行中；STATELESS 已可用 |
| 注解能力 | @Tool/@Resource/@Prompt/… 全能力 |
| 与 Spring 生态 | Security/Actuator/Boot 全复用 |

**趋势判断**：① Java 生态的 MCP 主战场在 Spring AI（SDK 是底层备选）——**"Spring 化"是 Java 生态一切技术的事实归宿**；② STATELESS 模式的 day-one 支持让 Java 在无状态规范上不落后 Tier 1；③ 企业后端（本库主线）的 MCP 路径已非常清晰：**starter + 注解 + 配置，协议细节交给框架**。

**Java 生态与本库主线的衔接**（重要）：本库是 Java 后端 + AI 学习体系，MCP 在 Java 的落地路径应当贯通——阶段 2/3/4 用 Python 学协议与工程（FastMCP 教学效率最高），生产级 Java 项目用 Spring AI（@Tool 注解 + STATELESS 配置），两者通过"协议概念"无缝平移：Python 的 `@mcp.tool` 对应 Java 的 `@Tool`，Python 的 lifespan/Depends 对应 Java 的 Bean 生命周期/依赖注入，Python 的中间件对应 Spring 的拦截器——**阶段 3/4 学到的生产级清单（认证/只读默认/MRTR/测试）在 Java 生态同样适用，只是承载框架不同**。面试"你用 Java 写过 MCP 吗"的答案：协议与工程方法在 Python 已验证，Java 侧 Spring AI 提供同等能力的配置化实现——这是双语言路线的最佳叙事。

## 13. 面试速记补充（Java 生态）

1. Java SDK 三层架构 + 响应式内核/同步门面 = 与 WebFlux 同哲学。
2. Spring AI starter 矩阵：server/client × webmvc/webflux × 4 传输。
3. @Tool 注解 = 方法即工具；ToolCallbackProvider 装配。
4. STATELESS 配置 = 无状态规范支持（`protocol: STATELESS`）。
5. 2026-03-17 三大变更：注解入核心、传输入 Spring AI、Jackson 3。
6. Java 最短路径四步：starter → @Tool → 配置 → 部署；SDK 低层留后路。

> 🎯 **核心要点**：Java 生态 = 官方 Java SDK（Tier 2，三层架构：Client/Server→Session→Transport，响应式内核+同步门面）+ Spring AI 全家桶（starter 矩阵覆盖 STDIO/SSE/Streamable/Stateless 全传输、@Tool 注解全能力、客户端配置化注入）+ 版本线（1.1.0 GA、Spring AI 2.0.0-M3 官方化迁移、STATELESS day-one）。**Java 后端写 MCP 的最短路径：加 starter → @Tool 注解 → 配 protocol=STATELESS → 部署**——Spring 生态把 MCP 从"协议工程"降为"配置项"，而 Java SDK 低层类保留协议级定制的后路。

---

**下一模块**：[05-Go 与 C#：Tier 1 双雄](05-Go 与 C#：Tier 1 双雄.md) / **返回总览**：[00-主流MCP开发框架知识体系总览](00-主流MCP开发框架知识体系总览.md)
