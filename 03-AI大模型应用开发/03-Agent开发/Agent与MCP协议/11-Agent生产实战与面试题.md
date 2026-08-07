# 11 - Agent 生产实战与面试题

> 🎯 Agent 从 Demo 到生产的最后一公里 — 架构设计、成本优化、避坑指南、高频面试题全覆盖

---

## 目录

1. [生产架构设计](#1-生产架构设计)
2. [成本优化](#2-成本优化)
3. [Java 后端集成方案](#3-java-后端集成方案)
4. [生产避坑 TOP 8](#4-生产避坑-top-8)
5. [高频面试题](#5-高频面试题)

---

## 1. 生产架构设计

```text
生产级 Agent 系统：

┌──────────────────────────────────────────────────────┐
│  用户层：Chat UI / API / Slack Bot                     │
├──────────────────────────────────────────────────────┤
│  Agent 编排层：                                        │
│  ├── Router (意图识别→分发)                            │
│  ├── Agent Executor (LangGraph/AutoGen)               │
│  ├── Tool Registry (工具注册+权限)                     │
│  └── Memory Manager (短期+长期记忆)                    │
├──────────────────────────────────────────────────────┤
│  工具层：                                              │
│  ├── MCP Servers (标准协议工具)                        │
│  ├── Internal APIs (内部服务)                         │
│  └── External APIs (第三方)                           │
├──────────────────────────────────────────────────────┤
│  基础设施：                                            │
│  ├── Redis (缓存/限流)                                │
│  ├── PostgreSQL (状态持久化)                          │
│  ├── Milvus (长期记忆)                                │
│  └── LangSmith (可观测性)                             │
└──────────────────────────────────────────────────────┘
```

---

## 2. 成本优化

```text
Agent Token 消耗高的原因：
  → 每次 Thought → LLM 调用
  → 每次 Action → LLM 调用
  → N 步任务 ≈ 2N 次 LLM 调用

优化策略：
  ① 用小模型做简单任务（GPT-4o-mini 替代 GPT-4o）
  ② 用 ReWOO 减少 LLM 调用（从 2N 降到 2 次）
  ③ 缓存常见问题的 Answer（Redis）
  ④ 限制最大步数（默认 10-15 步）
  ⑤ 并行工具调用（减少等待时间）
```

---

## 3. Java 后端集成方案

```text
推荐架构：Python Agent 服务 + Java 后端 HTTP 调用

  ┌──────────┐   HTTP    ┌──────────────┐
  │ Java 后端  │ ───────→ │ Python Agent │
  │ (业务逻辑) │ ←─────── │ (LangGraph)  │
  └──────────┘           └──────────────┘

Java 端职责：
  → 用户认证/权限控制
  → 业务逻辑编排
  → 结果后处理/存储

Python 端职责：
  → Agent 推理循环（利用 LangChain/LangGraph 生态）
  → LLM 调用
  → 工具执行
```

```java
// Java 端调用 Agent 服务
@Service
public class AgentService {
    
    private final RestTemplate restTemplate;
    
    public AgentResponse execute(String goal, String userId) {
        // 权限检查
        UserPermissions perms = permissionService.get(userId);
        
        // 调用 Python Agent
        AgentRequest request = AgentRequest.builder()
            .goal(goal)
            .permissions(perms)
            .userId(userId)
            .build();
        
        return restTemplate.postForObject(
            "http://agent-service:8000/execute",
            request, AgentResponse.class
        );
    }
}
```

---

## 4. 生产避坑 TOP 8

| # | 坑 | 现象 | 解决 |
|---|------|------|------|
| 1 | **无最大步数限制** | Agent 无限循环 → 费用失控 | 设置 max_steps=10~15 |
| 2 | **工具未限权** | Prompt 注入 → 执行危险操作 | 权限分级+写操作确认 |
| 3 | **工具描述模糊** | LLM 选错工具或参数 | 写清楚"何时用、做什么" |
| 4 | **单一 LLM** | 所有步骤用 GPT-4o → 成本高 | 简单任务用小模型 |
| 5 | **无重试机制** | 工具临时失败 → Agent 卡住 | 指数退避重试 |
| 6 | **无超时保护** | 慢工具拖死整个流程 | 每个工具设超时(30s-60s) |
| 7 | **无视记忆管理** | 长对话 → 上下文爆炸 | 滑动窗口+摘要压缩 |
| 8 | **无降级策略** | Agent 失败 → 用户看到错误 | 降级到简单 LLM 回答 |

---

## 5. 高频面试题

### Q1: Agent 和传统 LLM 有什么区别？

```text
LLM：被动应答，一问一答
Agent：主动执行，给定目标→自主规划→调用工具→迭代完成

核心差异：Agent 有规划、工具使用、记忆、反思能力
```

### Q2: ReAct 的原理是什么？

```text
ReAct = Reasoning + Acting 交替循环

Thought → Action → Observation → Thought → ...
每一步基于上一步的观察结果决定下一步做什么
```

### Q3: Function Calling 和 MCP 的关系？

```text
Function Calling：LLM 调用工具的格式规范（OpenAI 提出）
MCP：工具和数据源的标准化共享协议（Anthropic 提出）

互补关系：MCP Server 内部用 Function Calling Schema 定义工具
```

### Q4: 多 Agent 协作有哪些模式？

```text
① 顺序流水线：A→B→C
② 层级管理：Manager 分配 → Expert 执行
③ 辩论模式：多方论证 → Judge 评判
④ 群集自组织：无中心协商
```

### Q5: Agent 怎么处理工具调用失败？

```text
① 解析错误信息 → 修正参数重试
② 同一工具失败 3 次 → 换备选工具
③ 所有工具不可用 → 降级到纯 LLM 回答
④ 关键步骤失败 → 请求人工介入
```

### 更多面试题

| # | 问题 | 关键词 |
|---|------|--------|
| 6 | Agent 记忆系统怎么设计？ | 工作/短期/长期记忆 |
| 7 | 怎么防止 Agent 死循环？ | max_steps、循环检测 |
| 8 | LangGraph 和 LangChain 的区别？ | 状态图 vs 线性Chain |
| 9 | Agent 怎么控制成本？ | 小模型、ReWOO、缓存 |
| 10 | MCP Server 怎么开发？ | Resources/Tools/Prompts |

---

## 6. 评估驱动开发（2026 生产方法论）

**Agent 生产化的核心转变：从 Demo 到评估驱动**：

```text
2026 共识："用 Trace 替代 Demo" ——
  评估与可观测性从"上线后的事"变成"开发流程的一部分"

评估驱动开发流程：
  ① 定义指标：ToolSelectionAccuracy / FunctionCallAccuracy / TaskCompletion
     （分层度量 —— 端到端指标会掩盖工具层缺陷）
  ② 模拟测试：Persona/ScenarioGenerator 生成 200-500 个虚拟用户
     对 staging 环境压测 → 指标阈值作为发布门禁
  ③ CI 集成：基于 Trace 的回归测试（每次改动跑）
  ④ 上线监控：网关 + OpenTelemetry 跨客户端统一追踪
  ⑤ 持续优化：工具描述优化器（ProTeGi/GEPA 等）自动改进
     → ToolSelectionAccuracy 可提升 10-25 个百分点
```

**生产 SLI 清单**（2026 推荐）：

| SLI | 说明 |
|-----|------|
| 工具选择准确率 | Agent 选对工具的比例（核心） |
| 函数调用准确率 | 参数/Schema 校验通过率 |
| 任务完成率 | 端到端成功率 |
| 工具延迟/错误率 | 每个工具的 P95 与错误 |
| Schema 校验成功率 | 参数合法性 |
| 资源新鲜度 | 知识/数据是否过期 |

> 🎯 **核心要点**：Agent 生产化的 2026 分水岭 = "**评估驱动**"——**分层指标（工具/函数/任务）+ 模拟压测门禁 + Trace 回归**三件套，把 Agent 从"感觉还行"变成"可度量可回归"（与 RAG 评估体系同源，见 02-RAG 深化 20 号文件）。

---

## 核心要点回顾

- 生产架构 = Agent编排 + 工具注册 + 记忆管理 + 可观测性
- 成本优化：小模型做简单任务、ReWOO减调用、缓存
- Java 集成：Python Agent 服务 + HTTP 调用
- 面试核心：LLM→Agent差异、ReAct、FC vs MCP、多Agent协作
