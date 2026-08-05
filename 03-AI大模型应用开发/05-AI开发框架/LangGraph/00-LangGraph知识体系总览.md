# LangGraph 知识体系总览

> LangGraph 是 LLM Agent 的"操作系统内核"——用有向图管理状态、循环、分支和人机协作，把 Agent 从线性脚本升级为可恢复的有状态工作流。

---

## 📚 目录

1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [学习路线推荐](#3-学习路线推荐)
4. [核心概念速查](#4-核心概念速查)
5. [LangGraph 在 LangChain 生态中的定位](#5-langgraph-在-langchain-生态中的定位)

---

## 1. 知识体系导图

```
LangGraph 知识体系
│
├── 基础篇 ─── 核心概念与图结构 ──────────────────────┐
│   ├── State（状态）：Agent 的"工作内存"              │
│   │   ├── TypedDict vs Pydantic vs Dataclass        │
│   │   ├── Reducer：Annotated[list, operator.add]    │
│   │   ├── State Schema 设计模式                      │
│   │   └── MessageGraph（消息专用简化版）             │
│   │                                                  │
│   ├── Node（节点）：计算单元                          │
│   │   ├── LLM Node（模型推理）                       │
│   │   ├── Tool Node（ToolExecutor）                  │
│   │   ├── Agent Node（ReAct / OpenAI Agent）         │
│   │   ├── Function Node（纯逻辑处理）                │
│   │   └── Subgraph Node（嵌套子图）                  │
│   │                                                  │
│   └── Edge（边）：控制流                             │
│       ├── Normal Edge（顺序执行）                    │
│       ├── Conditional Edge（条件分支）               │
│       ├── Parallel Edge（并行 fan-out）              │
│       └── END / START（特殊哨兵）                    │
│                                                      │
├── 进阶篇 ─── 控制流与状态持久化 ────────────────────┤
│   ├── 循环与 Agentic Loop                            │
│   │   ├── ReAct 循环（LLM ↔ Tool 直到完成任务）     │
│   │   ├── Supervisor 模式（主Agent 分配子任务）      │
│   │   └── Self-Reflection Loop（自我反思改进）       │
│   │                                                  │
│   ├── Checkpointing（检查点/持久化）                  │
│   │   ├── MemorySaver（开发测试用）                  │
│   │   ├── SqliteSaver（单机生产）                    │
│   │   ├── PostgresSaver（分布式生产）                │
│   │   ├── CheckpointAt（每次/每N步保存）             │
│   │   └── 断点恢复（从任意 Checkpoint 继续）         │
│   │                                                  │
│   └── Human-in-the-Loop（人机协作）                  │
│       ├── interrupt() / interrupt_before / after     │
│       ├── Command() 编辑状态 + 恢复执行               │
│       ├── 审批工作流（Approval Flow）                 │
│       └── 人工标注循环（RLHF 数据采集）              │
│                                                      │
├── 深入篇 ─── 流式、多Agent与生产部署 ───────────────┤
│   ├── Streaming（流式输出）                           │
│   │   ├── values 模式（每一步的完整状态）             │
│   │   ├── updates 模式（增量更新）                   │
│   │   ├── messages 模式（LLM token 流 + 状态）       │
│   │   ├── custom 模式（自定义数据流）                │
│   │   └── debug 模式（排查用）                       │
│   │                                                  │
│   ├── Multi-Agent 架构                                │
│   │   ├── Supervisor Agent                            │
│   │   ├── Hierarchical Agent（层级委派）              │
│   │   ├── Swarm / Handoff（动态交接）                 │
│   │   └── Map-Reduce 并行 Agent                       │
│   │                                                  │
│   └── LangGraph Platform                              │
│       ├── LangGraph Cloud（SaaS 托管）                │
│       ├── LangGraph Server（自建 API 服务）           │
│       ├── LangGraph Studio（可视化调试）              │
│       └── Assistant API（开箱即用 Agent）            │
│                                                      │
└── Java 篇 ─── LangGraph4j ──────────────────────────┤
    ├── langgraph4j 核心 API                            │
    ├── Spring Boot 集成                                │
    └── Java 实现 Agent 工作流                          │
```

---

## 2. 模块导航

| 序号 | 模块 | 核心内容 | 适合人群 |
|:---:|------|---------|---------|
| 01 | [核心概念与 State 管理](./01-核心概念与State管理.md) | State/Node/Edge 三要素、StateGraph 构建、Reducer 机制 | 所有人（必读） |
| 02 | [条件分支与循环控制流](./02-条件分支与循环控制流.md) | Conditional Edge、ReAct Loop、并行 fan-out、Subgraph | 所有人 |
| 03 | [持久化与 Human-in-the-Loop](./03-持久化与Human-in-the-Loop.md) | Checkpoint、断点恢复、interrupt、Command、审批流 | 进阶开发者 |
| 04 | [流式输出与调试](./04-流式输出与调试.md) | values/updates/messages/custom 四种模式、LangSmith 追踪 | 全栈开发者 |
| 05 | [多 Agent 协作架构](./05-多Agent协作架构.md) | Supervisor、层级委派、Swarm/Handoff、Map-Reduce | 架构师 |
| 06 | [Java 生态：LangGraph4j](./06-Java生态LangGraph4j.md) | langgraph4j API、Spring Boot 集成、完整 Java 示例 | Java 后端（必读） |
| 07 | [LangGraph Platform 与生产部署](./07-LangGraph-Platform与生产部署.md) | Cloud、Server、Studio、自建部署、生产 checklist | DevOps/架构师 |

---

## 3. 学习路线推荐

### 路线 A：Java 后端快速上手（2-3 天）

```
01-核心概念 → 06-Java生态LangGraph4j → 02-条件分支与循环 → 03-持久化
```

适合 Java 后端开发者，目标是理解 LangGraph 并能在 Spring Boot 中构建 Agent 工作流。

### 路线 B：Python Agent 开发者（4-5 天）

```
01 → 02 → 03 → 04（流式）→ 05（多Agent）→ 07（生产部署）
```

适合 Python 技术栈，目标是构建可上线的 Agent 系统。

### 路线 C：全栈 Agent 架构师（7-10 天，全部模块）

```
01 → 02 → 03 → 04 → 05 → 06 → 07
```

---

## 4. 核心概念速查

| 概念 | 说明 | 模块 |
|------|------|:---:|
| **State** | 图中所有节点共享的数据容器，决定图何时"完成" | 01 |
| **StateGraph** | LangGraph 的核心类，定义 State + Nodes + Edges | 01 |
| **Reducer** | 状态合并策略（`operator.add`、自定义函数） | 01 |
| **Node** | 输入 State → 输出 State 更新（partial）的计算单元 | 01 |
| **Conditional Edge** | 根据 State 内容动态选择下一个节点 | 02 |
| **Agentic Loop** | LLM ↔ Tool 循环，直到 finish_reason="stop" | 02 |
| **Checkpoint** | 每一步的状态快照（支持断点恢复、时间旅行） | 03 |
| **interrupt()** | 暂停执行，等待人工输入后再继续 | 03 |
| **Command()** | 人工编辑状态后，恢复暂停的图执行 | 03 |
| **MemorySaver** | 内存中的 Checkpointer（开发用，进程重启丢失） | 03 |
| **SqliteSaver** | SQLite 持久化 Checkpointer（单机生产） | 03 |
| **Stream Mode** | values / updates / messages / custom / debug | 04 |
| **Supervisor Agent** | 一个主 Agent 根据意图将任务分发给专业子 Agent | 05 |
| **Handoff** | Agent A 将对话上下文"交接"给 Agent B | 05 |
| **LangGraph Server** | 将图包装为 HTTP API + 内置 Checkpoint 管理 | 07 |
| **LangGraph4j** | LangGraph 的 Java 实现（社区维护） | 06 |

---

## 5. LangGraph 在 LangChain 生态中的定位

```
LangChain 生态三层架构：

┌─────────────────────────────────────────────┐
│            LangGraph Platform                │  ← 部署层
│  (Cloud / Server / Studio / Assistant API)  │
├─────────────────────────────────────────────┤
│              LangGraph                       │  ← 编排层（核心）
│  (StateGraph, 循环, Checkpoint, 多Agent)    │
├─────────────────────────────────────────────┤
│              LangChain                       │  ← 组件层
│  (LLM, Tool, Chain, RAG, Prompt Template)   │
└─────────────────────────────────────────────┘

关键区别：
  LangChain Chain  = 固定流水线（A → B → C）
  LangGraph         = 灵活有向图（A → B → C → 判断 → 回A）
```

```text
LangChain 解决"用什么组件"
LangGraph 解决"这些组件怎么编排"（任意控制流）
LangGraph Platform 解决"怎么部署成服务"
```

> 🎯 **核心要点**：LangGraph 补上了 LangChain 最大的短板——复杂控制流。没有 LangGraph 的 Agent 只能用线性 Chain 硬凑，有了 LangGraph 才能实现真正的"观察→思考→行动→观察→..."循环。

---

**下一模块**：[01 - 核心概念与 State 管理](./01-核心概念与State管理.md)
