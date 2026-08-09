# OTel 语义约定与规范

> OTel = 2026 年 LLM 可观测的**默认传输层**（CNCF 厂商中立标准）：OTLP 协议 + 标准化 `gen_ai.*` 属性命名空间——解决 2024 前的厂商锁定问题：换后端从改代码变成改配置。本章给 gen_ai 语义约定全景、OpenInference 对照与迁移规范。

## 1. 为什么 OTel 成为默认

| 2024 前的问题 | OTel 解法 |
|---|---|
| 每家厂商私有 SDK + 私有属性 schema | 统一 gen_ai.* 命名空间 |
| 换后端 = 重写插桩 | 换后端 = 改配置（OTLP 输出） |
| 跨框架无法关联 | 同一属性 schema，跨 Agent 边界/框架/厂商可关联 |

> 🎯 核心要点：**OTel 是互操作契约，不是工具**——Phoenix/Langfuse/Datadog/Honeycomb 都在 OTLP 之上；插桩写一次，后端随便换。

## 2. gen_ai.* 属性地图

| 类别 | 属性 | 说明 |
|---|---|---|
| 操作 | `gen_ai.operation.name` | chat/embeddings/retrieval/execute_tool/create_agent/invoke_agent/generate_content/text_completion |
| 供应商/模型 | `gen_ai.provider.name`（替代废弃的 gen_ai.system） | provider + 请求/响应模型 |
| 用量 | `gen_ai.usage.input_tokens` / `output_tokens` / `cache_read.input_tokens` / `cache_creation.input_tokens` | 含缓存用量 |
| 成本 | `gen_ai.usage.cost` | **微美元**（microdollars）单位 |
| 延迟 | `gen_ai.client.operation.duration` / `server.time_to_first_token` | 客户端总时长 + 首 token |
| Agent | `gen_ai.agent.id/name/description/version` | Agent 身份与版本 |
| 会话 | `gen_ai.conversation.id` | 跨 trace 关联键 |

> 💡 单位约定是坑：成本用微美元（1 美元 = 1,000,000 微美元）、时长用秒（转 JSON 时 ms 要换算）——迁移期最常见的错误。

## 3. 稳定度与版本控制

| 项 | 说明 |
|---|---|
| 状态 | GenAI 语义约定仍在 **developing**（非稳定） |
| 控制 | `OTEL_SEMCONV_STABILITY_OPT_IN` 环境变量版本固定 |
| 供应商特定约定 | Anthropic/OpenAI/Bedrock/Azure AI Inference/MCP 各有专属约定 |
| 建议 | 插桩层加可插拔语义层——不硬编码单一约定（反模式） |

> ⚠️ 2026 反模式清单：仅用厂商 SDK 插桩；写死单一语义约定无插拔层；每个 LLM 调用手写 span——三个反模式对应锁定/脆弱/漏埋。

## 4. OpenInference：并行标准

| 项 | 说明 |
|---|---|
| 来源 | Arize 发起，Apache 2.0 |
| span kinds | CHAIN/LLM/RETRIEVER/TOOL/AGENT/EMBEDDING/RERANKER（**8 种**） |
| vs OTel | OTel 管传输与属性，OpenInference 管 span 分类——互补 |
| 对比 | Phoenix 8 种 span kind vs Langfuse 5 种——粒度差异 |

> 🎯 核心要点：OTel 与 OpenInference 是**双轨并行**——OTel 语义约定是传输契约，OpenInference 是 span 分类法；2026 平台大多同时支持。

## 5. 迁移与规范化：老 schema → gen_ai.*

| 步骤 | 做法 |
|---|---|
| 摄入期转换 | OpenLLMetry/Langfuse/OpenInference/Phoenix/Bedrock 旧 schema 摄入时规范化为 gen_ai.* |
| 属性映射 | `llm.token_count.prompt` → `gen_ai.usage.input_tokens` |
| 单位转换 | 延迟 ms → 秒；成本 → 微美元 |
| 验证 | 迁移后跑 span 完整度审计（§7） |

> 💡 迁移的价值：**历史数据可查询、跨厂商可比**——不迁移 = 每次换工具丢历史上下文。

## 6. OTel 的边界：它不做什么（Fiddler 2026）

| 边界 | 说明 | 谁补 |
|---|---|---|
| 无输出评估 | 不判忠实度/幻觉 | 评估层（06 篇） |
| 无安全打分 | 不判毒性/PII | 护栏（Guardrails 体系） |
| 无质量判断 | 不判相关性/连贯性 | LLM-judge |
| 被动 | 不能拦截/脱敏/阻断 | 网关/护栏 |
| 成本有限 | 只记 token，不归因浪费 | 成本层（05 篇） |

> ⚠️ 2026 金句：**"OTel 记录发生了什么，不评估好不好"**——Telemetry 与 Evaluation 是两个层（06 篇）。只接 OTel 的团队="有行车记录仪没有交警"。

## 7. Span 完整度：仪表盘诚实的前提

| 项 | 说明 |
|---|---|
| 规则 | **完整度 <95% 时仪表盘撒谎** |
| 检查 | 审计缺失属性、统计每类型 span 属性齐全率 |
| 收益 | 补属性往往是最快收益——比换平台便宜 |
| 频率 | 持续审计——新代码路径会悄悄丢 span |

> 🎯 核心要点：先完整度后分析——**在完整度不足的 trace 上做任何结论都是猜测**。生产可观测的体检第一步：完整度报表。

## 8. 常见误区

| 误区 | 真相 |
|---|---|
| "OTel 是某家公司的" | CNCF 厂商中立标准——换后端改配置 |
| "gen_ai 约定已稳定" | developing 状态——OTEL_SEMCONV_STABILITY_OPT_IN 固定 |
| "成本单位随意" | 微美元是约定——单位错=账目错 |
| "OTel 管评估" | OTel 记录不评估——评估是上层（06 篇） |
| "OpenInference 与 OTel 竞争" | 互补：传输 vs 分类 |
| "迁移可以不做" | 不迁移=换工具丢历史+无法跨厂商比对 |
| "完整度差不多就行" | <95% 仪表盘撒谎——补属性最快收益 |
| "厂商 SDK 够了" | 锁定——插桩层要可插拔 |

## 9. 面试速记

| 问题 | 一句话答案 |
|---|---|
| OTel 解决什么？ | 厂商锁定——统一 gen_ai.* 语义，换后端改配置 |
| 成本单位？ | 微美元（microdollars） |
| 操作名有哪些？ | chat/embeddings/retrieval/execute_tool/create_agent/invoke_agent 等 |
| agent 属性？ | id/name/description/version |
| 稳定度？ | developing——STABILITY_OPT_IN 固定版本 |
| OpenInference？ | span 分类法 8 kinds（CHAIN/LLM/TOOL/AGENT 等）——与 OTel 互补 |
| 迁移映射例子？ | llm.token_count.prompt → gen_ai.usage.input_tokens |
| OTel 不做什么？ | 不评估/不判安全/不拦截/不归因浪费 |
| 金句？ | OTel 记录发生了什么，不评估好不好 |
| span 完整度规则？ | <95% 仪表盘撒谎——补属性最快收益 |
| 反模式？ | 仅厂商 SDK/写死约定/手写 span |
| 与 06 篇关系？ | Telemetry 与 Evaluation 是两个层——OTel 之下，评估之上 |

---

**下一模块**：[04-指标：四类指标体系](04-指标：四类指标体系.md)　**返回总览**：[00-观测&可观测组件总览](00-观测可观测组件总览.md)

## 参考来源

- [LLM App Observability with OpenTelemetry: The 2026 Setup（FutureAGI）](https://futureagi.com/blog/llm-app-observability-otel-2026/)
- [What Is OpenTelemetry for LLMs?（FutureAGI）](https://futureagi.com/glossary/open-telemetry/)
- [OpenTelemetry for AI Observability: What It Covers and Where It Stops（Fiddler AI）](https://www.fiddler.ai/blog/opentelemetry-ai-observability-guide)
- [opentelemetry-agent-observability.md（agentpatterns）](https://github.com/agentpatterns-ai/website/blob/main/standards/opentelemetry-agent-observability.md)
- [Integrate AI Agent Observability through OpenTelemetry OBI（Alibaba Cloud）](https://www.alibabacloud.com/help/en/cms/cloudmonitor-2-0/integrate-ai-agent-observability-through-opentelemetry-obi)
