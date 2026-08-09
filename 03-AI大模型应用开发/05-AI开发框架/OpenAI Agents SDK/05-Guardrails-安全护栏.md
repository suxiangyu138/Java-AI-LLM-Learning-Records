# 05 Guardrails：安全护栏

> SDK 的原生安全防线：输入护栏（模型调用前拦截，省钱）、输出护栏（产出后校验）、Tripwire 立即中止——"护栏是第二道防线，不能替代 Prompt 安全设计"。

## 📚 目录

1. [护栏的设计定位](#1-护栏的设计定位)
2. [输入护栏](#2-输入护栏)
3. [输出护栏](#3-输出护栏)
4. [Tripwire 机制](#4-tripwire-机制)
5. [成本优化](#5-成本优化)
6. [护栏最佳实践](#6-护栏最佳实践)
7. [面试高频问法](#7-面试高频问法)
8. [Guardrails 完整示例](#8-guardrails-完整示例)
9. [常见误区](#9-常见误区)

## 1. 护栏的设计定位

### 为什么需要护栏

```
Agent 是自主的：模型可能
① 处理不该处理的内容（违规/敏感）
② 输出不符合要求的内容
护栏 = 程序化的安全底线（不依赖模型自觉）
```

### 核心原则（官方明确）

```
护栏是第二道防线，不能替代 Prompt 安全设计
第一道：instructions（指令约束）
第二道：Guardrails（程序校验）
```

### 两种类型

| 类型 | 时机 | 作用 |
|---|---|---|
| 输入护栏 | 模型调用前 | 拦截违规输入（省钱） |
| 输出护栏 | 最终输出后 | 校验产出合规 |

## 2. 输入护栏

### 机制

```
输入 guardrail 在模型调用前运行：
验证通过 → 正常调用模型
验证失败 → 阻止/替换/直接返回

默认与模型调用并行（不增加延迟）
也可 run_in_parallel=False（阻塞式，先验再调）
```

### 用法

```python
from agents import Agent, Runner, input_guardrail
from pydantic import BaseModel

class SafetyOutput(BaseModel):
    is_ok: bool
    reasoning: str

@input_guardrail
async def safety_check(ctx, agent, input):
    """输入安全校验：违规内容拦截"""
    result = await Runner.run(
        safety_evaluator,          # 校验 Agent（便宜模型）
        f"判断输入是否安全：{input}",
        output_type=SafetyOutput,
    )
    if not result.final_output.is_ok:
        return GuardrailResult(output=result, tripwire_triggered=True)
    return GuardrailResult(output=result, tripwire_triggered=False)

agent = Agent(
    name="Assistant",
    instructions="...",
    input_guardrails=[safety_check],
)
```

### 输入护栏的价值

```
① 安全：违规输入不进模型
② 省钱：拦截请求不调用主模型
③ 降噪：垃圾请求提前滤掉
```

## 3. 输出护栏

### 机制

```
在 agent 产出最终输出后运行：
验证输出是否符合要求（格式/内容/敏感信息）
失败 → 可阻止输出/tripwire
```

### 用法

```python
@output_guardrail
async def output_check(ctx, agent, output):
    """输出校验：PII 检测等"""
    result = await Runner.run(
        pii_evaluator,
        f"检查输出是否含敏感信息：{output}",
        output_type=SafetyOutput,
    )
    return GuardrailResult(output=result, tripwire_triggered=not result.final_output.is_ok)
```

### 输出护栏场景

| 场景 | 校验 |
|---|---|
| 合规 | 不得包含违规内容 |
| PII | 不含个人信息 |
| 格式 | 符合输出 schema |
| 质量 | 长度/完整性 |

## 4. Tripwire 机制

### 是什么

```
tripwire_triggered=True → 立即停止处理并抛出 GuardrailTripwireTriggered 异常
```

### 触发行为

```
① 输入护栏触发：不调用模型，直接中止
② 输出护栏触发：中止，不返回输出
③ 异常可捕获：调用方感知并降级处理
```

### 使用建议

```
捕获并降级：
try:
    result = Runner.run_sync(agent, query)
except GuardrailTripwireTriggered:
    return "该请求无法处理"      # 安全降级
```

### 与拒绝策略的关系

```
Tripwire = 程序级强制中止（硬防线）
对应"拒答机制"（RAG 阶段 2/3）的 Agent 版
```

## 5. 成本优化

### 省钱门控模式（官方推荐）

```
用便宜模型（如 gpt-4o-mini）做输入 guardrail：
拦截违规/无效请求 → 不调用主模型
可节省 60-80% token（大量请求被提前拦截）
```

### 成本对比

| 场景 | 无护栏 | 输入护栏 |
|---|---|---|
| 正常请求 | 主模型调用 | 便宜校验 + 主模型 |
| 违规请求 | 主模型调用（浪费） | 便宜校验（拦截） |
| 比例：违规占 60% | 100% 主模型 | 40% 主模型 |

### 成本控制注意

```
① 护栏模型选便宜的（校验任务简单）
② 护栏本身也有成本——拦截率低时不划算
③ 评估拦截率与成本（护栏的 ROI）
```

## 6. 护栏最佳实践

### 设计清单

| 实践 | 说明 |
|---|---|
| 第一道防线是 Prompt | 指令约束先行，护栏兜底 |
| 输入输出都设 | 双向防线 |
| Tripwire 必配 | 硬中止 + 捕获降级 |
| 便宜模型做门控 | 拦截类任务不贵 |
| 独立评估 | 护栏漏拦率/误拦率要测 |
| 日志 | GuardrailSpanData 独立可观测 |

### 护栏评估

```
指标：
漏拦率（违规没拦住）→ 必须低
误拦率（正常被拦）→ 影响体验
平衡：按业务容忍度调阈值
```

### 常见误区

| 误区 | 真相 |
|---|---|
| "护栏替代 Prompt" | 第二道防线，不能替代 |
| "只设输入" | 输出也要（PII/格式） |
| "护栏没成本" | 校验模型有成本（选便宜） |
| "Tripwire 影响体验" | 是安全底线（降级处理） |
| "越多越好" | 每道护栏有成本与误拦——评估后保留 |

## 7. 面试高频问法

| 问题 | 回答要点 |
|---|---|
| Guardrails 是什么？ | 输入/输出校验器（原生安全防线） |
| 输入 vs 输出护栏？ | 模型前拦截（省钱）vs 产出后校验 |
| Tripwire？ | 触发立即中止 + 抛异常 |
| 为什么是第二道防线？ | Prompt 安全设计是第一条 |
| 怎么省钱？ | 便宜模型门控（省 60-80% token） |
| 怎么评估护栏？ | 漏拦率/误拦率 |

### 面试加分表达

> "Guardrails 是 SDK 的原生安全防线：输入护栏在模型调用前拦截（默认并行不增延迟），输出护栏校验产出，Tripwire 触发立即中止。官方定位是第二道防线——Prompt 安全设计第一，程序校验兜底。成本上我用便宜模型做门控，违规请求拦截率高的场景能省 60-80% token。"

## 8. Guardrails 完整示例

```python
"""双护栏：输入安全门控 + 输出 PII 校验"""
from agents import Agent, Runner, input_guardrail, output_guardrail
from agents.guardrails import GuardrailResult
from pydantic import BaseModel

class CheckOutput(BaseModel):
    is_ok: bool
    reasoning: str = ""

# 校验 Agent（便宜模型门控）
checker = Agent(
    name="Checker",
    instructions="判断输入是否安全合规，输出结构化结论。",
    model="gpt-4o-mini",        # 便宜模型（成本关键）
)

@input_guardrail
async def input_safety(ctx, agent, input):
    """输入护栏：违规拦截（省钱）"""
    result = await Runner.run(
        checker, f"输入是否安全？\n{input}", output_type=CheckOutput,
    )
    return GuardrailResult(
        output=result,
        tripwire_triggered=not result.final_output.is_ok,
    )

@output_guardrail
async def output_pii(ctx, agent, output):
    """输出护栏：PII 检测（合规）"""
    result = await Runner.run(
        checker, f"输出是否含个人信息？\n{output}", output_type=CheckOutput,
    )
    return GuardrailResult(
        output=result,
        tripwire_triggered=not result.final_output.is_ok,
    )

# Agent 挂双护栏
agent = Agent(
    name="Assistant",
    instructions="...",
    input_guardrails=[input_safety],
    output_guardrails=[output_pii],
)
```

### 示例解读

```
双防线结构：
输入护栏（模型前）→ 主模型 → 输出护栏（产出后）
Tripwire 触发 → 捕获降级（安全底线）
便宜模型做校验（拦截类任务不贵）
```

## 9. 常见误区

| 误区 | 真相 |
|---|---|
| "护栏替代 Prompt" | 第二道防线（指令约束第一） |
| "只设输入够" | 输出也要（PII/格式） |
| "护栏无成本" | 校验模型有成本（选便宜） |
| "Tripwire 是可选" | 是安全底线（配合捕获降级） |
| "护栏越多越安全" | 每道有成本与误拦——评估后保留 |

> 🎯 核心要点：Guardrails 输入/输出双类型（模型前拦截省钱/产出后校验合规）；Tripwire 硬中止 + 捕获降级；定位是第二道防线（Prompt 设计优先）；便宜模型门控省 60-80% token（拦截率高时）；评估看漏拦率/误拦率；GuardrailSpanData 独立可观测；双护栏完整示例（输入安全 + 输出 PII）。

---

**下一模块**：[06-Sessions-状态管理](06-Sessions-状态管理.md) / **返回总览**：[00-OpenAI-Agents-SDK知识体系总览](00-OpenAI-Agents-SDK知识体系总览.md)
