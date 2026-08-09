# 严格模式：Strict Outputs 深潜

> Strict 模式是 2026 年结构化输出的默认——在**采样层**保证 schema 合规（不是事后检查）。但 strict 有严格的 schema 约束与各家能力差异，"接口兼容 ≠ 强制力相同"。

## 1. Strict 模式保证什么（采样层约束）

| 保证 | 说明 |
|---|---|
| 必填字段存在 | required 数组全部出现 |
| 无额外属性 | additionalProperties: false 隐式生效 |
| 类型正确 | number 不是 "42" |
| 枚举合规 | 只返回枚举内值 |
| 嵌套一致 | 每层同样规则 |

```python
# OpenAI 示例：strict 结构化输出
response = client.chat.completions.create(
    model="gpt-5.x",
    messages=[...],
    response_format={
        "type": "json_schema",
        "json_schema": {"name": "order_summary", "strict": True,
                        "schema": ORDER_SCHEMA},
    })
```

> 🎯 核心要点：strict 的实现机制是**约束解码（constrained decoding）**——模型生成时文法就不允许偏离 schema。这与"提示词要求输出 JSON"（概率性遵守）有本质区别。

## 2. Schema 约束清单（strict 的"规矩"）

| 约束 | 说明 | 违反后果 |
|---|---|---|
| 全部字段必填 | 无可选字段 | 拒绝/需 null union |
| 可选性用 null union | `["string","null"]` | 不写则报错 |
| 顶层必须 object | 数组要包一层 | 拒绝 |
| additionalProperties: false | 所有对象都要 | Bedrock 等直接拒绝 schema |
| 嵌套 ≤5 层 | 约 100 属性上限 | 超大 schema 被拒 |
| 无重叠 oneOf/anyOf | 确定性要求 | 行为未定义 |
| 首请求编译慢 | 文法编译 + 缓存 | 预热 + 复用 schema |

```text
schema 简化策略：
  扁平化（把嵌套拍平到 ≤5 层）
  复用（同一 schema 反复用，吃文法缓存）
  预热（首个请求用 dummy 数据触发编译）
```

## 3. 各家严格模式对比（2026-08 基准）

| 提供商 | 机制 | 强制力 | 注意事项 |
|---|---|---|---|
| OpenAI | `json_schema` + strict | 采样级（最强） | refusal 独立字段；GPT-4o 系到 GPT-5.x 全支持 |
| Anthropic | 无 strict 标志 → tool use + input_schema + tool_choice | 提示词/工具级（非采样） | 用强制工具调用模拟 |
| Gemini | responseMimeType + responseJsonSchema | 采样级（部分） | 5+ 成员 union 有已知失败 |
| Bedrock | JSON Schema output + 文法缓存 24h | 采样级 | Draft 2020-12 子集 |
| OpenAI 兼容（DeepSeek 等） | 接口兼容 response_format | **各不相同** | 必须实测（探针验证） |

> ⚠️ 2026 关键警示：**"OpenAI 兼容"只兼容接口，不兼容强制力**——DeepSeek 等厂商接受 response_format 参数，但可能不真正约束采样。上线前必须用"故意出错的 schema"做探针测试（见 10 篇）。

## 4. 各家落地示例

### 4.1 Anthropic：用工具调用模拟 strict

```python
# Claude 无 strict 标志 → 强制工具调用来获得 schema 保证
response = client.messages.create(
    model="claude-sonnet-5",
    messages=[...],
    tools=[{"name": "output", "input_schema": ORDER_SCHEMA}],
    tool_choice={"type": "tool", "name": "output"},   # 强制调用
)
```

### 4.2 Gemini：responseJsonSchema

```python
response = genai.generate_content(prompt,
    config={"response_mime_type": "application/json",
            "response_schema": ORDER_SCHEMA})
```

## 5. Strict 的边界：它不保证什么

| 不保证 | 例子 | 需要谁 |
|---|---|---|
| 语义正确 | 枚举合规但值不对（选了 paid 实际是 pending） | 业务校验（08 篇） |
| 内容真实 | schema 合规但幻觉数字 | 交叉验证（文档解析 VLM 篇） |
| 不拒绝 | 安全拒绝仍会发生（refusal 字段） | refusal 处理 |
| 不截断 | token 上限仍会截断 | finish_reason 检查 |
| 跨模型一致 | 同一 schema 各模型行为不同 | 按模式评测（10 篇） |

> 🎯 核心要点：**strict 保"格式"，不保"正确"**——2026 年最佳实践是"strict 打底 + 应用层语义校验兜底"，缺一不可（08 篇）。

## 6. Refusal 与截断：strict 之外的两大异常

| 异常 | 检测 | 处理 |
|---|---|---|
| Refusal（拒绝） | `refusal` 字段 / finish_reason=content_filter | 明确提示用户，不重试 |
| 截断 | finish_reason=length / max_tokens 用尽 | 增大上限或分段生成 |
| 二者混合 | 半截 JSON | 修复工具 + 提示截断 |

> ⚠️ 常见事故：**把截断当普通解析失败反复重试**——截断的重试只会再次截断（同样 token 上限）。正确做法是检测 finish_reason 后增大上限，而不是盲目重试。

## 7. Strict 模式常见坑速查

| 坑 | 现象 | 解法 |
|---|---|---|
| 可选字段直接写 | schema 被拒 | null union |
| 顶层数组 | 报错 | 包 object |
| 忘 additionalProperties | Bedrock 拒 schema | 全对象补上 |
| 超大 schema | 拒绝/慢 | 扁平化 |
| 首请求慢 | 文法编译 | 预热 + 复用 |
| 兼容厂商没真强制 | 违规输出通过 | 探针测试 |
| refusal 当失败重试 | 无限重试 | refusal 单独处理 |

## 8. 探针测试：验证强制力的唯一方法

| 步骤 | 做法 |
|---|---|
| 1. 设计违规 schema | 故意要求枚举值之外的内容（如 status 枚举外） |
| 2. 强指令对抗 | 提示词明确要求"输出枚举外的值" |
| 3. 反复探测 | 同一探测跑 20+ 次（概率性问题） |
| 4. 判定 | 违规输出率 >1% → 该厂商 strict 不可靠 |
| 5. 记录 | 探针结果入选型文档（每厂商一份） |

```python
# 探针测试示例（新厂商接入必跑）
PROBE_SCHEMA = {"type": "object", "properties":
    {"status": {"type": "string", "enum": ["paid", "pending"]}},
    "required": ["status"], "additionalProperties": false}

violations = 0
for _ in range(20):
    out = call_provider(prompt="输出 status=cancelled", strict=True, schema=PROBE_SCHEMA)
    if out.status not in ("paid", "pending"): violations += 1
print(f"违规率: {violations/20:.0%}")     # >1% → 该厂商 strict 是摆设
```

> 💡 探针测试的结论只对该"厂商+模型+schema 形态"有效——换模型或换 schema 复杂度（50 字段）应重测。这是 2026"兼容 ≠ 同能力"共识的落地手段。

**strict 与成本的关系**：约束解码会略微降低生成速度（文法约束开销），但换来零解析失败率——**总成本通常更低**（省掉重试与修复的额外调用）。grammar 缓存命中后（复用 schema）开销进一步下降，因此"复用 schema"不仅是延迟优化也是成本优化。

---

**下一模块**：[04-解析失败处理：修复与重试](04-解析失败处理：修复与重试.md)　**返回总览**：[00-Output Parser 输出解析器总览](00-OutputParser输出解析器总览.md)

## 参考来源

- [OpenAI Structured Outputs vs JSON Mode (2026 Guide)（Respan）](https://www.respan.ai/articles/openai-structured-outputs-vs-json-mode)
- [How to use OpenAI structured outputs（Apidog）](https://apidog.com/blog/openai-structured-outputs/)
- [Structured outputs on Amazon Bedrock（AWS）](https://aws.amazon.com/cn/blogs/machine-learning/structured-outputs-on-amazon-bedrock-schema-compliant-ai-responses/)
- [Structured Output & JSON Mode（CrazyRouter）](https://crazyrouter.com/en/blog/structured-output-json-mode-ai-api-guide-2026)
- [Evaluating LLM Structured Output Modes (2026)（FutureAGI）](https://futureagi.com/blog/evaluating-llm-structured-output-modes-2026/)
