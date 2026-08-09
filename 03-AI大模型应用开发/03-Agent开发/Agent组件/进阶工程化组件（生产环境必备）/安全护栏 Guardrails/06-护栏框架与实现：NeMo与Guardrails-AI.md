# 护栏框架与实现：NeMo 与 Guardrails AI

> 实现层 = 把护栏工程化：Guardrails AI 管"输出形状与安全"（验证器 + RAIL/Pydantic），NeMo Guardrails 管"对话流与话题边界"（Colang 五 rail）。2026 共识：**两者互补不替代，分层组合是推荐架构**。本章给双框架深潜与选型决策。

## 1. 双框架定位：不是替代品

| 维度 | Guardrails AI | NVIDIA NeMo Guardrails |
|---|---|---|
| 核心抽象 | Validators + 输出 schema | Colang rails / 对话流 |
| 关注点 | 输出验证、结构化输出 | 对话流控制、话题边界 |
| 配置方式 | Python + RAIL spec / Pydantic | Colang + YAML |
| 结构化输出保证 | 最佳（每次都是合法类型化 JSON） | 可能，非重点 |
| 话题/对话 rail | 有限 | 最佳 |
| 越狱/安全 rail | 靠验证器 | 内置 self-check + jailbreak rails |
| 事实核查 | 靠验证器 | 内置 rail |
| 验证器库 | Hub 70+（2026-04） | 内置 rail 类型 + 自定义 Colang |
| 生态倾向 | 框架无关（任意 LLM） | NVIDIA/NeMo 栈 |
| 最新版本 | v0.10.0（2026-04-03） | v0.21.0（2026-03-12） |
| 延迟 | 50-200ms（验证器链） | 100-500ms（大规则集） |
| 部署 | 库（Python 依赖） | 对话层/旁路服务 |
| 授权 | Apache 2.0 | Apache 2.0 |

> 🎯 核心要点：**Guardrails AI 保证"输出的形状与安全"，NeMo 规定"对话被允许做什么"**——一个管质量，一个管行为，分工清晰。

## 2. Guardrails AI：验证器框架

```python
# Guardrails AI 最小示例（示意）
from guardrails import Guard
from guardrails.hub import PIIRedaction, ToxicityDetection

guard = Guard().use_many(
    PIIRedaction(on_fail="fix"),        # 自动脱敏
    ToxicityDetection(on_fail="refrain")  # 拒绝
)

result = guard.validate(llm_response)   # 校验 + 补救
```

| 核心概念 | 说明 |
|---|---|
| Validator | 单个校验器（PII/毒性/幻觉/格式/偏见） |
| Hub | 社区验证器市场（70+，v0.10.0） |
| RAIL/Pydantic | 输出契约规范（类型化结构） |
| on_fail 三策略 | refrain（拒）/ fix（修）/ reask（重问） |
| 组合管线 | 链式校验：PII 脱敏 → 竞品屏蔽 → 语气强制 |

> ⚠️ 权衡：Python 原生（非 Python 栈要云产品或 sidecar）；验证器链 50-200ms；比 API 式方案需要更多集成代码。

## 3. NeMo Guardrails：Colang 对话流

```colang
# Colang 示意：话题边界 rail
define user ask about money transfer
  "转账到国外账户"
  "如何给境外汇款"

define flow
  user ask about money transfer
  bot refuse transfer outside policy   # 触发边界回应
```

| 核心概念 | 说明 |
|---|---|
| Colang | 可读可审计的对话流语言——非工程师也能 review 规则 |
| 五 rail | input/dialog/retrieval/execution/output 全覆盖 |
| Jailbreak rail | 内置越狱自检 |
| Fact-check rail | 事实核查（对知识源） |
| 部署 | 对话层/旁路服务；任意 LLM 提供商 |
| 局限 | Colang 学习曲线、自托管无托管 API、规则手动维护、非批量场景 |

> 💡 Colang 的价值：**规则可审计**——合规场景（on-prem 对话策略是合规要求）下，非工程师可审查"机器人被允许说什么"，这是 API 式方案给不了的。

## 4. 分层组合：2026 推荐架构

```text
生产推荐分层（2026 共识）：
  用户输入
    → NeMo 对话层：话题 rail（范围判定）+ jailbreak rail（轮次允许）
    → Colang flows 控制对话走向
    → LLM
    → Guardrails AI 输出层：Pydantic/RAIL schema 校验
    → 验证失败 → reask / fix 后放行
    → 用户
  分工：NeMo 规定"对话被允许做什么"；Guardrails AI 保证"输出了什么形状"
```

| 层 | 责任 | 失败语义 |
|---|---|---|
| NeMo（对话层） | 话题边界、越狱、流控制 | 拒绝轮次/话题外回应 |
| Guardrails AI（输出层） | schema、内容安全、自动修复 | refrain/fix/reask |

> 🎯 核心要点：**组合的价值在职责分离**——对话策略（业务规则）与输出质量（契约安全）由不同抽象管理，各自演进互不干扰；2026 主流生产栈还会配 Presidio（PII）或 Lakera（注入）等专家组件。

## 5. 托管 API 生态（不开源自托管时）

| 服务 | 厂商 | 能力 |
|---|---|---|
| Prompt Shields | Azure | 直接+间接注入（文档面） |
| moderation（omni） | OpenAI | 通用内容审核（免费额度） |
| Lakera Guard | Lakera | 商用注入防护专家 |
| Bedrock Guardrails | AWS | 托管护栏 |
| Model Armor | GCP | 托管护栏 |
| guardrails-api | Guardrails AI | 云版验证器服务 |

> 💡 选型维度：**数据合规（能否出内网）× 威胁覆盖（间接注入？）× 延迟预算 × 成本**——开源自托管保数据，托管 API 省运维，通常混用。

## 6. 框架选型决策

| 场景 | 选择 |
|---|---|
| 输出必须是合法结构化 JSON | Guardrails AI（RAIL/Pydantic 最佳） |
| 对话 Agent 要话题边界/流程控制 | NeMo Guardrails |
| 合规要求规则可审计 | NeMo（Colang 非工程师可 review） |
| RAG 答案忠实度校验 | Guardrails AI 验证器 |
| 全栈非 Python | 托管 API / 云产品 / sidecar |
| 多层纵深 | 两者组合（§4） |
| 预算诚实 | 数"模型支撑的检查数"，不是数框架 |

> ⚠️ 成本真相：两个框架都免费（Apache 2.0），**真金白银是模型支撑的检查**——事实核查/自检每轮多一次模型调用；正则/schema 验证基本免费。预算公式：模型检查数 × 每检查成本。

## 7. 常见误区

| 误区 | 真相 |
|---|---|
| "两个框架二选一" | 互补不替代——一个管对话行为，一个管输出质量 |
| "NeMo 太慢" | 100-500ms 是模型 rail 的代价——可只对高危轮启用 |
| "Guardrails AI 只适合 Python" | 非 Python 栈用云产品/sidecar，不是不能用 |
| "框架自带你的策略" | 框架执行公共分类法——退款规则/禁用话题要自己配 |
| "开源=免费" | 框架免费，模型判断按调用计费 |
| "Colang 是负担" | 可审计性在合规场景是刚需——成本换审计 |
| "托管 API 更省心就全托管" | 数据出内网即合规风险——先定数据边界再选 |

## 8. 面试速记

| 问题 | 一句话答案 |
|---|---|
| 双框架定位？ | Guardrails AI 管输出形状安全，NeMo 管对话流与话题边界——互补 |
| Guardrails AI 核心？ | Validators + RAIL/Pydantic + Hub 70+ 验证器 |
| on_fail 三策略？ | refrain（拒）/ fix（修）/ reask（重问） |
| NeMo 核心？ | Colang 对话流 + 五 rail + 内置 jailbreak/fact-check rail |
| Colang 的价值？ | 规则可审计——合规场景非工程师可 review |
| 分层组合怎么分？ | NeMo 管"对话被允许做什么"，Guardrails AI 保证"输出了什么形状" |
| 两框架延迟？ | 50-200ms vs 100-500ms（模型 rail 的代价） |
| 预算公式？ | 数模型支撑的检查数——框架免费，判断收费 |
| 托管 API 有哪些？ | Azure Prompt Shields / OpenAI moderation / Lakera / Bedrock / Model Armor |
| 版本现状？ | Guardrails AI v0.10.0（2026-04）/ NeMo v0.21.0（2026-03） |
| 合规选型？ | 规则可审计 → NeMo Colang；on-prem 对话策略是合规要求 |
| 与 05 篇分工？ | 05 选"眼睛"（分类器），本篇选"骨架"（框架）——先骨架后眼睛 |

---

**下一模块**：[07-护栏编排与 Agent 集成](07-护栏编排与Agent集成.md)　**返回总览**：[00-安全护栏 Guardrails 总览](00-安全护栏Guardrails总览.md)

## 参考来源

- [Guardrails AI vs NeMo Guardrails (2026)（Respan）](https://www.respan.ai/market-map/compare/guardrails-ai-vs-nemo-guardrails)
- [Guardrails AI vs NeMo Guardrails (2026): Which LLM Safety Framework?（GenAI.QA）](https://genai.qa/blog/guardrails-ai-vs-nemo-guardrails/)
- [Best AI Agent Guardrails Platforms in 2026（FutureAGI）](https://futureagi.com/blog/best-ai-agent-guardrails-platforms-2026/)
- [I put 6 LLM guardrail tools inline and measured what they cost me（dev.to）](https://dev.to/james_oconnor_dev/i-put-6-llm-guardrail-tools-inline-and-measured-what-they-cost-me-here-is-the-latency-vs-recall-433g)
- [Guardrails AI vs NeMo Guardrails — Output Validation Framework vs Conversational Flow Control（AICoolies）](https://aicoolies.com/comparisons/guardrails-ai-vs-nemo-guardrails)
