# 04 可观测性：日志、Trace 与 Metrics（LLMOps 三支柱）

> 定位：上线后"看得见"的基础——用 OpenTelemetry GenAI 语义约定把每次 Agent 循环变成可回放的 trace（2026-08 基准）

## 📚 目录

1. [Agent 可观测性与传统 APM 的区别](#1-agent-可观测性与传统-apm-的区别)
2. [OTel GenAI 语义约定：字段标准](#2-otel-genai-语义约定字段标准)
3. [埋点实战：Agent 循环插桩](#3-埋点实战agent-循环插桩)
4. [Trace 后端选型：Langfuse 与生态](#4-trace-后端选型langfuse-与生态)
5. [结构化日志规范](#5-结构化日志规范)
6. [Metrics 与采样策略](#6-metrics-与采样策略)
7. [成本与质量观测](#7-成本与质量观测)
8. [与 trace-first 排错的联动](#8-与-trace-first-排错的联动)
9. [落地检查单](#9-落地检查单)

## 1. Agent 可观测性与传统 APM 的区别

```text
传统 APM 关心：请求→响应 的延迟/错误/吞吐
Agent 关心：   一次请求内部的 决策链（模型调用 + 工具调用 + 中间状态）
               回答质量（离线 eval 分、在线反馈）
               成本（每轮 token、每用户花费）

结论：传统 APM 无法解析 prompt、token、思维链、工具调用——必须上 LLM 专用可观测
```

| 维度 | 传统 APM | LLM 可观测 |
|------|---------|-----------|
| 观测单元 | 请求 | trace（plan-act-observe-reflect 循环） |
| 关键字段 | URL/状态码/耗时 | prompt、model、temperature、prompt hash、工具参数、分轮 token |
| 质量信号 | 无 | eval 分数是一等公民 |
| 采样哲学 | 普通请求全采 | 长尾错误保留、普通流量采样 |
| 成本信号 | 无 | 每 trace 精确成本归因 |

## 2. OTel GenAI 语义约定：字段标准

> 2025 年稳定（1.28+，Datadog 已支持至 1.37）。**跨厂商、跨后端的 LLM trace 字段标准**——按它埋点，后端可自由切换（Langfuse/Phoenix/Tempo/Datadog）。

| 属性 | 含义 |
|------|------|
| `gen_ai.system` | 供应商（openai / anthropic / gemini / deepseek...） |
| `gen_ai.operation.name` | `chat` / `embeddings` |
| `gen_ai.request.model` / `gen_ai.response.model` | 请求模型 vs 实际响应模型（路由场景必看） |
| `gen_ai.usage.input_tokens` / `gen_ai.usage.output_tokens` | token 数（注意：1.28+ 用 `input_tokens`，不再是 `prompt_tokens`） |
| `gen_ai.response.finish_reasons` | 停止原因列表（stop / length / tool_use） |
| `gen_ai.tool.name` / `gen_ai.tool.call.id` | 工具调用标识 |
| temperature / top_p / max_tokens / stop_sequences | 请求参数（可选） |
| `user.id` / `session.id` | 用户与会话上下文 |

> 💡 三条附加规则：① span 带 `gen_ai.*` 属性即被后端渲染为"generation"（带模型/成本）；② 用 **OTel Baggage** 传播 trace 级属性（user id、session id、版本号、flag）保证全 span 可聚合；③ 实验性新字段用 `OTEL_SEMCONV_STABILITY_OPT_IN=gen_ai_latest_experimental` 开启。

## 3. 埋点实战：Agent 循环插桩

### 3.1 Span 层级设计

```text
Trace（一次用户请求）
├── Agent span（编排循环）        —— plan-act-observe-reflect
│   ├── LLM span（每次模型调用）  —— gen_ai.* 全字段
│   ├── Tool span（每次工具调用） —— 名称/参数/结果摘要/耗时
│   └── (嵌套) Agent span        —— 子 Agent 委派
└── Eval span（在线采样评分）     —— eval 分数、判定依据
```

### 3.2 必须采集的字段（Agent 专属）

| 字段 | 说明 | 用途 |
|------|------|------|
| prompt hash | 系统 prompt 的哈希 | trace-first 排错：秒判"是不是 prompt 变了" |
| 工具参数与结果摘要 | 全量结果截断 ~8KB（超过截断+摘要） | 回放每一步决策依据 |
| 轮次号 / 总轮数 | 循环进度 | 识别死循环（同一工具连续 8-47 次） |
| 版本快照 | (prompt_version, model_version, code_version) | 行为漂移归因 |
| 成本 | 每轮 token + 单价 → 会话累计 | 成本归因与告警（07/11 篇） |
| 错误标记 | 工具错误码、`retry: true/false` | 卡死循环分析 |

### 3.3 插桩代码模式

```python
with tracer.start_as_current_span("agent.run", attributes={
    "session.id": sid, "user.id": uid,
    "prompt.hash": prompt_hash, "agent.version": "v12"}) as run_span:
    for step in agent_loop(...):
        with tracer.start_as_current_span("llm.call", attributes=genai_attrs(model, messages)):
            response = await llm.chat(messages)
        with tracer.start_as_current_span("tool.call", attributes={"gen_ai.tool.name": name, "tool.args": redact(args)}):
            result = await tool.execute(args)
            run_span.add_event("tool.result", {"summary": truncate(result.summary)})
```

> ⚠️ **脱敏纪律**：prompt/工具参数/结果进 trace 前必须脱敏（手机号、身份证、密钥 mask）。trace 后端是敏感数据汇聚点，本身要按数据治理管控（08 篇）。

## 4. Trace 后端选型：Langfuse 与生态

### 4.1 Langfuse 定位（2026）

| 事实 | 说明 |
|------|------|
| 开源协议 | MIT（核心）——2026-01 被 ClickHouse 收购后**仍保持 MIT** |
| 特性 | trace + eval + prompt 管理 + 数据集一体，自托管可行，起步 $29/月 |
| OTLP 接入 | 任意 OTel 语言（Go/Java/C#/Ruby）可直发 `https://cloud.langfuse.com/api/public/otel`（HTTP/protobuf 或 JSON；**不支持 gRPC**；Basic Auth 用项目 key） |
| 非 Python/JS 栈 | 无需等官方 SDK——OTel 标准接入即可 |
| Collector | 可选，用于扇出/采样/网络隔离 |

> 💡 **混合架构**：OpenInference 插桩 → OTLP 到 Langfuse/Phoenix → 仓库汇（ClickHouse 等）是 2026 中位生产形态。与 Datadog（2026-07 起产品名"Agent Observability"）可同源双发，很多团队两个都跑。

### 4.2 工具对比

| 工具 | 定位 | 亮点 | 注意 |
|------|------|------|------|
| Langfuse | 开源可自托管 | OTLP 原生、eval/prompt 一体 | 自托管要维护 |
| LangSmith | LangChain 生态 | 与 LangGraph 无缝 | 绑定框架 |
| Arize Phoenix | 开源 | OpenInference 原生、NLI 打分 | 自托管成本 |
| Datadog Agent Observability | 商业 APM | 与现有监控打通 | 订阅贵 |
| Braintrust | eval 优先 SaaS | 实验对比统计显著 | trace 次要（06 篇） |

## 5. 结构化日志规范

```text
日志不是给 trace 兜底，而是给"无法进 trace 的上下文"兜底：
  1. 每次 Agent 循环输出一行结构化 JSON 事件（plan/act/observe/reflect）
  2. 关键决策写 decision 事件：原因 + 依据（可回放）
  3. 所有日志带 trace_id / session_id / version 三件套
  4. 敏感字段自动 mask（正则 + 白名单）
  5. 日志格式统一：Logback（Java）/ structlog（Python）
```

**推荐事件模型**：

```json
{"ts": "...", "trace_id": "t_123", "type": "tool_call",
 "agent": "refund_agent", "tool": "refund.apply",
 "args_summary": "order=#8823, amount=199.00",
 "result": "ok", "cost_usd": 0.003, "step": 4}
```

## 6. Metrics 与采样策略

### 6.1 核心指标集

| 指标 | 计算 | 告警 |
|------|------|------|
| TTFT（首 token 延迟） | 分位数 p50/p95/p99 | 缓存失效/模型故障时跳升 |
| 完整响应时间 | 分位数 | 拖尾（p99）比均值敏感 |
| 工具失败率 | 按工具分组 | 单一工具失败率 > 阈值 → 该工具降级 |
| 循环长度分布 | 轮数直方图 | 长尾 > max_steps 数突增 |
| 缓存命中率 | prompt/语义缓存 | 命中率下滑 → 成本上升预警 |
| token-per-success | 成功请求 token 成本 | 滚动均值突增 → 质量/循环异常 |
| eval 分数漂移 | 在线采样评分 | 持续 2-3pp 下滑 → 自动回滚信号 |

### 6.2 采样策略（与普通监控相反）

```text
普通请求：全量采样
LLM 请求：全量采成本 + 尾部采样（错误/慢/边界请求 100% 保留）
         普通成功请求按比例抽样（如 10%），省钱且够分析
         在线 eval 深判：约 5% 异步采样（06 篇）
```

## 7. 成本与质量观测

| 观测目标 | 手段 | 产出 |
|---------|------|------|
| 每用户/租户成本 | trace 归因 + 标签 | 账单爆炸按用户定位 |
| 每 Agent 成本 | 子 Agent span 归因 | 找出"吃钱"的子 Agent |
| 每模型成本 | `gen_ai.response.model` 聚合 | 路由策略效果评估（07 篇） |
| 质量 | 在线 eval 分数进 trace | 质量-成本-延迟三维对比 |
| 预算 | 滚动窗口聚合 + 告警 | 成本失控前止血（11 篇） |

## 8. 与 trace-first 排错的联动

```text
排错五步（详见调优模块 05 篇）：
  1. 拿到 trace_id → 打开完整 trace 回放
  2. 看 prompt hash 是否变化（变了 → 版本问题，查 10 篇）
  3. 看每一步 LLM 输出/工具结果（截断摘要已够定位）
  4. 看循环长度与错误标记（死循环 → 收紧 max_tool_calls）
  5. 看会话级指标（解析率/升级率/目标完成率）判断影响面

数据基础：prompt hash + 全量工具结果 + 会话回放——三者必须在埋点时就位
```

## 9. 落地检查单

```text
□ 已按 OTel GenAI 语义约定埋点（gen_ai.* 全字段）
□ trace 覆盖：模型调用 / 工具调用 / 子 Agent / 评估
□ prompt hash、版本快照、成本、错误标记已入 span
□ 敏感字段脱敏已生效（prompt/参数/结果）
□ 日志统一结构化并带 trace_id/session_id/version
□ 核心指标已建告警（TTFT/工具失败率/循环长度/缓存命中）
□ 采样策略已配（尾部保留 + 普通抽样）
□ 成本按用户/租户/Agent 可归因
□ 在线 eval 采样已接入 trace
```

---

## 【参考来源】

- [Langfuse: LLM Observability via OpenTelemetry（OTLP 端点与认证）](https://python-sdk-v3.docs-snapshot.langfuse.com/integrations/native/opentelemetry/)
- [Langfuse: Use Langfuse from Go, Java, C#, Ruby via OpenTelemetry](https://langfuse.com/resources/engineering/opentelemetry-languages)
- [Langfuse vs. Datadog for LLM Observability & Agent Tracing（2026-07）](https://langfuse.com/resources/engineering/langfuse-vs-datadog)
- [OTel 官方：GenAI Semantic Conventions（2025 稳定，1.28+）](https://opentelemetry.io/docs/specs/semconv/gen-ai/)
- [futureagi.com: What Is LLM Observability? A 2026 Architecture Guide（OpenInference/OTLP/采样）](https://futureagi.com/blog/what-is-llm-observability-2026/)
- [helmdeck ADR-013: OpenTelemetry GenAI Observability（多 Agent OTel 实践）](https://github.com/tosin2013/helmdeck/blob/main/docs/adrs/013-opentelemetry-genai-observability.md)
- trace-first 排错方法论：见 [Agent 调优模块 05 篇](../Agent%20调优、排错与安全模块（生产必备）/05-排错方法论.md)

---

**返回总览**：[00-总览：Agent 工程化与部署模块（从 demo 到可用应用）](00-总览：Agent%20工程化与部署模块（从%20demo%20到可用应用）.md)

**下一模块**：[05-测试体系：Mock、契约与端到端测试](05-测试体系：Mock、契约与端到端测试.md)
