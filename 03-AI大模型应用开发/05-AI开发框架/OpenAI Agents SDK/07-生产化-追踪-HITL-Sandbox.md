# 07 生产化：追踪、HITL 与 Sandbox

> 生产三件套：Tracing 内置追踪（OpenAI 仪表盘/可切 OTel）、HITL 人工审批（run 级别）、Sandbox Agents 容器化执行（2026-04 Beta）——外加 MCP 与多 Provider。

## 📚 目录

1. [生产化全景](#1-生产化全景)
2. [Tracing 可观测](#2-tracing-可观测)
3. [HITL 人工审批](#3-hitl-人工审批)
4. [Sandbox Agents](#4-sandbox-agents)
5. [MCP 与多 Provider](#5-mcp-与多-provider)
6. [生产清单与误区](#6-生产清单与误区)
7. [面试高频问法](#7-面试高频问法)
8. [与 RAG 阶段 5 的衔接](#8-与-rag-阶段-5-的衔接)
9. [常见误区](#9-常见误区)

## 1. 生产化全景

```
SDK 生产五件套：
① Tracing：内置追踪（默认 OpenAI 仪表盘）
② HITL：人工审批（run 级别）
③ Sandbox：容器化执行（2026-04 Beta）
④ MCP：工具生态复用
⑤ 多 Provider：LiteLLM 100+ 模型
```

| 件 | 解决什么 |
|---|---|
| Tracing | 出问题看得到（全链路） |
| HITL | 危险操作人审 |
| Sandbox | Agent 代码安全执行 |
| MCP | 工具生态 |
| 多 Provider | 不锁定 |

## 2. Tracing 可观测

### 内置追踪

```
每次 Runner.run() 自动生成 trace：
Agent 调用 / 工具调用 / Handoffs / Guardrails 全部记录
默认发往 OpenAI 仪表盘
```

### 切换后端

```python
# 自定义 trace processor（发到 Datadog/Langfuse/OTel）
from agents.tracing import set_trace_processors, OTelSpanProcessor

set_trace_processors([OTelSpanProcessor()])
```

### OTel 映射（OpenAIAgentsInstrumentor）

| Span 类型 | OTel 映射 |
|---|---|
| AgentSpanData | AGENT |
| HandoffSpanData | TOOL |
| GuardrailSpanData | CHAIN |

### 可观测内容

```
① 全链路：一次请求的完整调用链
② 工具：每个工具调用的耗时/参数
③ 交接：交接给谁/何时交接
④ 护栏：触发记录（独立过滤）
⑤ 成本：token 用量
（对应 RAG 阶段 5 的可观测思想，SDK 内建）
```

## 3. HITL 人工审批

### 机制

```
审批为 run 级别：
覆盖直接工具调用、handoffs、嵌套 Agent.as_tool() 运行
支持部分解决和恢复行为
```

### 使用场景

| 场景 | 审批 |
|---|---|
| 危险操作 | 删除/写库/发消息 |
| 花钱操作 | 下单/支付 |
| 外部动作 | 发送邮件/发布 |

### 与 Guardrails 的关系

```
Guardrails = 自动校验（程序判定）
HITL = 人工审批（人判定）
组合：自动拦低危，人审高危
```

### 设计原则（回顾 RAG 阶段 5）

```
审批是防线不是流程：
高频低危 → 自动（Guardrails）
低频高危 → 人审（HITL）
```

## 4. Sandbox Agents

### 是什么（2026-04-15 Beta）

```
容器化执行环境：
Agent 可读写文件、运行 shell、生成工件、跨运行持久化状态
```

### 结构

```
Sandbox Agent = 标准 Agent + Manifest + capabilities
capabilities：filesystem / shell / memory / skills
sandbox client：Docker 或本地 Unix
```

### 注意点

```
① 启动延迟 2-4 秒（容器启动）
② Docker 客户端需要 Docker socket 访问
③ Beta 阶段（生产评估后使用）
④ 安全价值：Agent 代码在容器内（对应"不可信代码沙箱"纪律）
```

## 5. MCP 与多 Provider

### MCP 支持

```
可消费任何 MCP 兼容服务器的工具：
文件系统 / GitHub / 数据库等生态
→ 工具扩展 = 挂 MCP 服务器（与本仓库 MCP 体系一致）
```

### 多 Provider（Provider 无关）

```
通过 LiteLLM 支持 100+ LLM：
不锁定 OpenAI 模型（DeepSeek/Claude/本地）
配置：LiteLLM provider 接入
```

### 上线检查（综合）

```
Agent 上线前的最终检查：
① 每个 Agent 有明确指令与工具边界
② 交接条件可验证（HandoffCorrectness）
③ 护栏双类型 + Tripwire 捕获
④ max_turns 全链路设置
⑤ Session 隔离与 TTL
⑥ 危险操作 HITL
⑦ 追踪接入（可回放故障）
⑧ 成本预算（模型分层 + 护栏门控）
```

### 组合实践

```
检索框架（LlamaIndex/Haystack）→ 包装为工具 → SDK Agent
MCP 服务器 → 工具 → SDK Agent
多模型 → LiteLLM → SDK
```

## 6. 生产清单与误区

### 生产检查清单

| 类别 | 检查项 | ✅ |
|---|---|---|
| 循环 | max_turns 必设 | ☐ |
| 安全 | 输入/输出 Guardrails + Tripwire | ☐ |
| 记忆 | Session 按用户隔离 | ☐ |
| 审批 | 危险操作 HITL | ☐ |
| 观测 | Tracing 接入（仪表盘/OTel） | ☐ |
| 成本 | 护栏门控 + 模型分层 | ☐ |
| 工具 | 白名单 + 幂等 | ☐ |
| 恢复 | RunState/会话恢复测试 | ☐ |

### 常见误区

| 误区 | 真相 |
|---|---|
| "托管 = 不用管安全" | 护栏/HITL/沙箱都要配 |
| "max_turns 可选" | 生产必设（死循环兜底） |
| "追踪只发 OpenAI" | 可切 Datadog/Langfuse/OTel |
| "只支持 OpenAI 模型" | LiteLLM 100+ Provider |
| "Sandbox 已成熟" | Beta（2026-04），评估后使用 |

## 7. 面试高频问法

| 问题 | 回答要点 |
|---|---|
| Tracing 默认发哪？ | OpenAI 仪表盘（可切 OTel 等） |
| HITL 覆盖什么？ | 工具/handoffs/嵌套 Agent（run 级别） |
| Sandbox Agents？ | 2026-04 Beta：容器化执行（文件/shell） |
| MCP 支持？ | 消费任意 MCP 服务器工具 |
| 多 Provider？ | LiteLLM 100+ LLM |
| 生产最重要的事？ | max_turns + Guardrails + HITL + 追踪 |

### 面试加分表达

> "SDK 生产化是'内建五件套'：Tracing 默认发 OpenAI 仪表盘、一行切 OTel；HITL 覆盖工具/handoffs/嵌套 Agent 的 run 级审批；Sandbox Agents（2026-04 Beta）把 Agent 执行容器化。我的生产清单第一行永远是 max_turns——托管循环没有上限就是无底洞，第二行是 Guardrails 双护栏。"

## 8. 与 RAG 阶段 5 的衔接

```
SDK 生产化 = 阶段 5 思想的 Agent 版：
监控（阶段 5 的 06）→ Tracing（内建）
安全（阶段 5 的 08）→ Guardrails + HITL
部署（阶段 5 的 01-04）→ 无状态服务 + Redis Session
评估（阶段 5）→ HandoffCorrectness + 业务指标

对应关系：
Prometheus 指标 → 追踪/成本统计
告警 → Tripwire 捕获 + 监控告警
密钥管理 → API Key 环境变量
```

## 9. 常见误区

| 误区 | 真相 |
|---|---|
| "托管 = 不用运维" | 追踪/HITL/Sandbox 都要配 |
| "追踪只能 OpenAI" | 可切 Datadog/Langfuse/OTel |
| "Sandbox 生产可用" | Beta（2026-04）——评估后使用 |
| "HITL 拖慢一切" | 只对高危操作设（低频） |
| "MCP 是可选" | 工具生态扩展的标配 |

> 🎯 核心要点：生产五件套（Tracing/HITL/Sandbox/MCP/多 Provider）；Tracing 默认仪表盘可切 OTel（span 映射 AGENT/TOOL/CHAIN）；HITL 是 run 级审批（自动护栏 + 人审高危）；Sandbox Agents 容器化执行（Beta，启动 2-4 秒）；MCP 扩工具、LiteLLM 多模型；生产清单八项（max_turns 第一）；与 RAG 阶段 5 的思想完全打通（SDK 内建版）。

---

**下一模块**：[08-对比选型与面试](08-对比选型与面试.md) / **返回总览**：[00-OpenAI-Agents-SDK知识体系总览](00-OpenAI-Agents-SDK知识体系总览.md)
