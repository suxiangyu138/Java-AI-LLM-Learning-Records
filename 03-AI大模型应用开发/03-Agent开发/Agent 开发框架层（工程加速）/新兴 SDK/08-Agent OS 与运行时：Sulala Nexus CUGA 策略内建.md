# 08 Agent OS 与运行时：Sulala / Nexus / CUGA / 策略内建

> 定位：运行时篇——"Agent 操作系统"叙事的代表项目与 2026 关键趋势"策略内建"：Sulala Agent OS、Nexus 三协议面、IBM CUGA（2026-08 基准）

## 📚 目录

1. [Agent OS 叙事：从口号到项目](#1-agent-os-叙事从口号到项目)
2. [Sulala Agent OS：极致轻量运行时](#2-sulala-agent-os极致轻量运行时)
3. [Nexus：三协议面的 Agent 基础设施](#3-nexus三协议面的-agent-基础设施)
4. [CUGA：策略内建的运行时](#4-cuga策略内建的运行时)
5. [策略内建：2026 关键趋势](#5-策略内建2026-关键趋势)
6. [Agent OS 评估视角](#6-agent-os-评估视角)
7. [核心要点](#7-核心要点)

## 1. Agent OS 叙事：从口号到项目

```text
"Agent OS" 的两种含义：
① 产品叙事：平台把自己包装成"Agent 的操作系统"
   （Coze 3.0 "AI 团队协作 OS"、AG2 "The Open-Source AgentOS"）
② 技术架构：Agent 运行时的操作系统化
   ——资源管理（进程/记忆/技能）、调度、权限、生命周期、互操作

2026 状态：技术架构层面的 Agent OS 处于探索期——
   Sulala（轻量运行时）、Nexus（协议面）、CUGA（策略内建）是代表
```

> 🎯 **判断**：Agent OS 是 2026 年最热的**叙事**之一——但"操作系统"隐喻的承诺（资源管理/调度/生态）远未兑现；评估看具体能力（运行时空不空、策略有没有、互操作通不通），不看名字。

## 2. Sulala Agent OS：极致轻量运行时

| 维度 | 事实 |
|------|------|
| 技术 | Bun 生态（TypeScript） |
| 体量 | 核心 ~100-150KB；含依赖 ~0.5-1MB（对比 LangChain 数十 MB 核心/100+MB 依赖） |
| 机制 | **图式协作微 Agent**（工作流中的微 Agent 协作，非单 Agent 循环） |
| 能力 | 可安装技能（skills）、工作流（workflows）、Web 仪表盘 |
| 安装 | `bun add -g @sulala-ai/agent-os` 或一行 curl/PowerShell |
| CLI | `sulala start`、`onboard` 等；仪表盘含 agents/graphs/skills/schedules/memory/settings |
| 定位 | "更接近 MicroGPT/SmolAgents，而非 LangChain"——无重型 Python 栈 |

> 💡 **体量对比的意义**：100KB vs 100MB 是"Agent OS 轻量派"的宣言——但轻量的代价是生态与生产配套需要自建。

## 3. Nexus：三协议面的 Agent 基础设施

| 维度 | 事实 |
|------|------|
| 定位 | AI 原生分布式文件系统 / Agent 基础设施 |
| 三协议面 | VFS（原生 REST）+ MCP Server（Agent↔工具）+ **A2A 端点**（Agent↔Agent） |
| A2A 实现 | Agent Card 端点 + JSON-RPC 路由 + Task 状态机 + SSE 流式；A2A 任务映射到内部 VFS 操作 |
| 意义 | "**跨框架互操作的缺失拼图**"——A2A 作为第三协议面补全 |

```text
Nexus 的 Agent OS 形态：
Agent 视角 = 文件系统（VFS）+ 工具（MCP）+ 同伴（A2A）
→ 一个"Agent 原生操作系统"的完整协议面样例
```

## 4. CUGA：策略内建的运行时

| 维度 | 事实 |
|------|------|
| 出品 | IBM Research（HuggingFace 博客，2026） |
| 定位 | 轻量 harness，20+ 可运行示例应用 |
| 策略系统 | **Intent Guard / Tool Approval / Tool Guide / Playbook / Output Formatter**——运行时强制护栏而非包装层 |
| 多 Agent | Supervisor 模式委派专家 Agent（本地或经 A2A 外部） |
| 技能 | SKILL.md 系 Agent Skills |

```python
# CUGA 概念：护栏是运行时策略，不是包装
policy = PolicyChain(
    IntentGuard(),           # 意图拦截
    ToolApproval(),          # 工具审批
    OutputFormatter()        # 输出约束
)
agent = Harness(model=..., policy=policy)
```

> 🎯 **CUGA 的意义**：治理从"外层包装"变为"运行时内建"——2026 年治理趋势的代表（多 Agent 模块的护栏、低代码平台的内置沙箱同向）。

## 5. 策略内建：2026 关键趋势

| 趋势 | 说明 | 代表 |
|------|------|------|
| 策略内建 | 护栏作为运行时组件而非包装层 | CUGA 策略系统 |
| 沙箱一等公民 | 执行隔离内建于框架 | Flue、Deer-Flow |
| 检查点内建 | 持久化默认开启 | LangGraph/CrewAI 2026 |
| 协议内建 | MCP/A2A 原生支持 | MAF/ADK/CrewAI |
| 技能内建 | SKILL.md 成为标准单元 | Flue、CUGA、Claude Code 生态 |

> 💡 **趋势内核**：2026 年"安全/持久化/互操作"从**开发者自建**变成**运行时默认**——新兴框架竞争点正是这些默认值。

## 6. Agent OS 评估视角

```text
评估 Agent OS 类项目五问：
① 运行时有什么？（调度/生命周期/权限——还是只有 CLI 包装）
② 策略在哪里？（内建还是包装层——内建才算数）
③ 互操作通吗？（MCP/A2A 原生还是 TODO）
④ 持久化？（检查点/记忆默认还是自建）
⑤ 生态证据？（生产部署 vs demo）

结论：2026 年 Agent OS = 探索期概念 + 部分可用的运行时组件——
      当"运行时"看待，不当"OS"承诺看待
```

## 7. 核心要点

> 🎯 **核心要点**：
> 1. "Agent OS" 双含义：产品叙事（Coze/AG2）vs 技术架构（Sulala/Nexus/CUGA）——评估看能力不看名字
> 2. Sulala：图式微 Agent 协作 + 极致轻量（核心 ~100-150KB）——轻量派宣言
> 3. Nexus：VFS + MCP + A2A 三协议面——"跨框架互操作缺失拼图"样例
> 4. CUGA：**策略内建**（Intent Guard/Tool Approval 等运行时组件）——2026 治理趋势代表
> 5. 2026 趋势内核：安全/持久化/互操作从开发者自建 → 运行时默认

---

**上一模块**：[07 协议新进展](07-协议新进展：A2A%20v1.0%20UCP%20OSSA%20三协议栈.md)　**下一模块**：[09 新兴框架评估与选型](09-新兴框架评估与选型.md)　**返回总览**：[00 总览](00-总览：新兴%20SDK%20知识体系.md)

## 【参考来源】

- [Sulala Agent OS (@sulala-ai/agent-os on npm)](https://www.npmjs.com/package/@sulala-ai/agent-os)
- [Sulala Agent OS security analysis (Socket)](https://socket.dev/npm/package/@sulala-ai/agent-os/overview/0.1.42)
- [Nexus: Implement Google A2A protocol endpoint (GitHub Issue)](https://github.com/nexi-lab/nexus/issues/1256)
- [Build real agentic apps using CUGA (HuggingFace/IBM Research)](https://huggingface.co/blog/ibm-research/cuga-apps)
- [AG2 — Framework (LLM Explorer)](https://llm-explorer.com/agent/ag2)
- [An Agent Is a Service: Where Agent Frameworks Are Going (go-micro)](https://go-micro.dev/blog/32)
