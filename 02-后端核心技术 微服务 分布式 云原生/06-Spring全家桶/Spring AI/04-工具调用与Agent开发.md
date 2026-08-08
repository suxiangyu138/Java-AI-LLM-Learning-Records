# 04-工具调用与 Agent 开发
> 2.0 重构核心：@Tool 本地工具、ToolCallback 统一抽象、ToolCallingAdvisor 统一循环、递归链、按需工具发现

## 📚 目录
1. [工具调用闭环原理](#1-工具调用闭环原理)
2. [@Tool：声明本地工具](#2-tool声明本地工具)
3. [ToolCallback：本地与 MCP 的统一抽象](#3-toolcallback本地与-mcp-的统一抽象)
4. [ToolCallingAdvisor：统一循环](#4-toolcallingadvisor统一循环)
5. [递归链：循环的引擎](#5-递归链循环的引擎)
6. [按需工具发现：ToolSearchToolCallingAdvisor](#6-按需工具发现toolsearchtoolcallingadvisor)
7. [从工具调用到 Agent](#7-从工具调用到-agent)
8. [2.0 移除项对照](#8-20-移除项对照)
9. [核心要点](#9-核心要点)
10. [参考来源](#10-参考来源)

## 1. 工具调用闭环原理

```text
用户提问
  │
  ▼
ChatClient ──1. 工具 schema 注入 prompt──▶ LLM
  ▲                                      │
  │                                    2. 模型返回工具调用请求
  │                                      ▼
  │                           ToolCallingAdvisor 判定
  │                              ├─ 无工具请求 → 返回最终回答
  │                              └─ 有工具请求
  │                                     │
  │                                   3. 执行 ToolCallback
  │                                     │（本地方法 / MCP 远程）
  │                                     ▼
  └──────── 4. 结果回传 prompt ──▶ 进入下一轮循环（递归）
```

| 环节 | 2.0 归属 | 1.x 的旧做法 |
|------|---------|--------------|
| 工具 schema 生成 | ChatClient 组装时 | 各模型内部 |
| 循环判定 | `ToolCallingAdvisor` | 各 ChatModel 私循环 |
| 执行分发 | `DefaultToolCallingManager` | 同上 |
| 递归重入 | `CallAdvisorChain.copy(...)` | 不支持 |

> 🎯 **核心要点**：工具调用闭环本质是"**对话循环**"——模型要工具 → 执行 → 结果入对话 → 再问模型。2.0 把这个循环从各模型内部**上移**到 Advisor 链，成为可组合、可观测、可扩展的一等公民。

## 2. @Tool：声明本地工具

```java
@Component
public class OrderTools {

    @Tool(description = "根据订单号查询订单状态")
    public String getOrderStatus(@ToolParam(description = "订单号") String orderId) {
        // 调用业务服务查库
        return orderService.queryStatus(orderId);
    }
}
```

| 注解要素 | 作用 |
|----------|------|
| `@Tool(description=...)` | 工具名（默认方法名）+ 描述（schema 质量直接影响模型选工具） |
| `@ToolParam(description=...)` | 参数描述，参与 schema 生成 |
| 返回值 | 简单类型/JSON 字符串均可，模型友好格式最佳 |
| 注册 | 组件扫描即可，配合 `.tools(...)` 显式挂载 |

> ⚠️ **schema 质量决定调用率**：描述模糊时模型不敢调或乱调。写法标准："动词 + 宾语 + 边界"（"根据订单号查询订单**当前**状态，订单号由用户提供"）。

## 3. ToolCallback：本地与 MCP 的统一抽象

```text
ToolCallback（统一接口：getToolDefinition() + call()）
 ├── 本地：@Tool 方法 → 自动包装（框架扫描生成）
 └── 远程：MCP 工具 → 自动注册（@McpTool / mcp-servers.json）
```

```java
// 本地与 MCP 工具混用
chatClient.prompt()
        .user("查一下订单 123 的状态，并看看天气")
        .tools(orderTools, mcpToolCallback)   // 参数可以是 ToolCallback 对象/集合/Bean 名
        .call();
```

| 混用规则 | 说明 |
|----------|------|
| 类型统一 | 本地 `@Tool` 与 MCP 工具都是 `ToolCallback` |
| 名字空间 | MCP 工具默认带服务器前缀（`DefaultMcpToolNamePrefixGenerator`）防冲突 |
| 过滤 | `McpToolFilter` Bean 可限制外部工具可见性 |

## 4. ToolCallingAdvisor：统一循环

### 4.1 行为特征

| 特征 | 说明 |
|------|------|
| 自动注册 | 只要 prompt 带了工具，`DefaultChatClient` 自动挂载 |
| 循环条件 | `ToolExecutionEligibilityChecker` 判定是否还需工具 |
| `returnDirect` | 工具结果**直接返回调用方**，不再回 LLM（内置"工具即答案"模式） |
| token 累计 | 多轮循环的用量合并到最终 `ChatResponse` |
| 单例不变量 | 一条链只允许一个工具 Advisor（`ToolAdvisor` 标记接口），防双循环 |

### 4.2 扩展钩子（自定义子类）

| 钩子 | 时机 |
|------|------|
| `doInitializeLoop` / `doInitializeLoopStream` | 循环开始前（仅一次） |
| `doBeforeCall` / `doBeforeStream` | 每轮迭代前 |
| `doAfterCall` / `doAfterStream` | 每轮迭代后 |
| `doFinalizeLoop` / `doFinalizeLoopStream` | 循环结束后（仅一次） |
| `doGetNextInstructionsForToolCall` | 工具请求 → 下一轮指令组装 |

自定义实现注册：`ToolCallingAdvisor.Builder<?>` Bean（`@ConditionalOnMissingBean` 保护）。

## 5. 递归链：循环的引擎

```java
// 伪代码：ToolCallingAdvisor 内部
CallAdvisorChain chain = context.getChain();
ChatResponse response = chain.next(context);            // 调模型

while (executionEligibilityChecker.isEligible(response)) {
    // 执行工具
    ToolExecutionResult result = toolCallingManager.executeToolCalls(response);
    // 关键：copy(this) 构建"只含本 Advisor 之后"的子链，递归重入
    response = chain.copy(this).next(
            context.withUserMessage(buildToolResultMessage(result)));
}
```

| 递归机制 | 效果 |
|----------|------|
| `CallAdvisorChain.copy(after)` | 生成只含 `after` 及后续顾问的子链 |
| 顺序保持 | 递归轮次仍经过后续 Advisor（日志/校验可观察每一轮） |
| 不重复 | 之前的 Advisor 不重复执行（记忆不重复注入） |

> 💡 同一机制同时驱动：工具循环、结构化输出校验重试、评估重试——"**递归 Advisor**"是 2.0 的模式总纲。

## 6. 按需工具发现：ToolSearchToolCallingAdvisor

当工具数量大到无法全部塞进 prompt（数百个）时：

```text
传统：全部工具 schema 一次性注入 → prompt 膨胀、模型选择困难、成本高
ToolSearch：首轮只注入"元信息"→ 模型请求 → 按需检索并注入发现的工具
```

| 组件 | 说明 |
|------|------|
| `ToolSearchToolCallingAdvisor` | 在 `doInitializeLoop` 索引工具集，`doBeforeCall` 注入已发现工具 |
| `ToolIndex` 接口 | 三种实现：**向量库**（语义检索）、**Lucene**（文本索引）、**正则**（简单匹配） |
| 场景 | 上百工具的企业 Agent、跨 MCP 服务器的工具市场 |

## 7. 从工具调用到 Agent

| 模式 | 结构 | 适用 |
|------|------|------|
| 单轮工具调用 | 一次 prompt + 若干工具，最多几轮循环 | 查询类（订单状态、天气） |
| 多轮 Agent | 循环 + 记忆 + 规划（用户可见的中间步骤） | 复杂任务拆解 |
| 多 Agent | 每 Agent 独立 ChatClient + 工具集，互相调用（消息/HTTP/MCP） | 领域隔离、审批流 |

工程建议：

| 原则 | 说明 |
|------|------|
| 工具是 Agent 的"能力原子" | 每个工具单一职责、可独立测试 |
| 循环要有护栏 | 最大轮数 + 超时 + 预算，防模型失控空转 |
| 可观测 | SimpleLoggerAdvisor 记录每轮工具调用 |
| 权限收敛 | 工具按最小权限实现（只读查询 vs 写操作分离） |

## 8. 2.0 移除项对照

| 已移除 | 替代方案 |
|--------|---------|
| `ChatClient.prompt().toolNames(...)` | `.tools(ToolCallback)` 显式注册 |
| `SpringBeanToolCallbackResolver` | 显式 ToolCallback Bean |
| `ChatOptions.toolNames` / `toolBeanDefinitionNames` | `.tools(...)` |
| `internalToolExecutionEnabled` | 工具循环由 ToolCallingAdvisor 托管 |
| 各模型内部工具执行循环 | 统一循环（模型不再承担） |
| `ToolCallAdvisor` 名称 | `ToolCallingAdvisor`（旧名保留为废弃子类） |

> ⚠️ **迁移硬伤预警**：任何依赖 `toolNames()` 的 1.x 代码在 2.0 编译期即失败——工具改造是 1.x→2.0 迁移的唯一硬性阻断点（详见 [01](01-模块清单与版本矩阵.md) 迁移路径第 ③ 步）。

## 9. 核心要点

> 🎯 **核心要点**：
> - 闭环 = 注入 schema → 模型要工具 → 执行 → 结果回传 → 递归；
> - `@Tool` 声明本地工具，`ToolCallback` 统一本地与 MCP；描述质量决定调用质量；
> - `ToolCallingAdvisor` 自动注册，`returnDirect`、token 累计、扩展钩子四件套；
> - 递归链（`copy`）是工具循环/校验重试/评估循环的共同引擎；
> - 数百工具场景用 `ToolSearchToolCallingAdvisor` 按需披露；Agent 循环加护栏。

## 10. 参考来源

- [Tool Calling in Spring AI 2.0（官方博客）](https://spring.io/blog/2026/06/15/spring-ai-composable-tool-calling)
- [Spring AI Reference：Tool Calling](https://docs.spring.io/spring-ai/reference/api/tools.html)
- [Spring AI Reference：ToolCallingAdvisor](https://docs.spring.io/spring-ai/reference/2.0-SNAPSHOT/api/tools/tool-calling-advisor.html)
- [Spring AI Reference：Recursive Advisors](https://docs.spring.io/spring-ai/reference/2.0-SNAPSHOT/api/advisors-recursive.html)
- [Spring AI 2.0.0 GA 发布博客](https://spring.io/blog/2026/06/12/spring-ai-2-0-0-GA-available-now)

---

**下一模块**：[05-快速开始实战](05-快速开始实战.md)　/　**返回总览**：[00-总览](00-Spring%20AI总览.md)
