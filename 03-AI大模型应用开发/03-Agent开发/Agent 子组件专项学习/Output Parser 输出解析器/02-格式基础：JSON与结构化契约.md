# 格式基础：JSON 与结构化契约

> 输出格式的选择是解析器的第一决策：JSON 是 2026 的事实标准，但"JSON"之外还有类型、枚举、嵌套、转义四个雷区。本章把格式基础讲透——格式选对，解析器成功一半。

## 1. 格式选型：为什么 JSON 是默认

| 格式 | 适用 | 缺点 |
|---|---|---|
| JSON | **默认（2026 事实标准）** | 无注释、无类型（靠 schema） |
| JSONL | 流式/批量记录 | 每行一个对象，不适合嵌套 |
| YAML | 配置类 | 缩进敏感，LLM 易错 |
| XML | 遗留系统 | 冗长 |
| Markdown 表格 | 人类可读的表格输出 | 结构弱 |
| 代码围栏（```json） | 对话式输出 | 需先剥围栏 |

> 🎯 核心要点：**默认 JSON + JSON Schema**——模型对 JSON 的生成能力最强，生态工具最全（strict/修复/部分解析都在 JSON 上）。非 JSON 只用于特殊场景。

## 2. JSON 解析的基础陷阱

| 陷阱 | 例子 | 解法 |
|---|---|---|
| 围栏包裹 | ```json {...} ``` | 解析前剥围栏（正则或标记） |
| 前后废话 | "好的，结果是：{...}" | 提取首个 `{` 到末个 `}` |
| 单引号 | {'a': 1} | 修复（02/04 篇） |
| 尾逗号 | {"a":1,} | 修复工具 |
| 注释 | // xxx | 修复工具 |
| Unicode 转义 | 😀 代理对 | 按对解码（05 篇陷阱） |

```python
# 剥围栏 + 提取 JSON 的最小实现
import json, re

def extract_json(text: str) -> dict:
    text = re.sub(r"```json|```", "", text).strip()      # 剥围栏
    start, end = text.find("{"), text.rfind("}")
    if start == -1 or end == -1:
        raise ParseError("无 JSON 对象")
    return json.loads(text[start:end+1])                  # 提取并解析
```

## 3. 类型转换：模型的"字符串癖"

| 模型常见输出 | 期望类型 | 处理 |
|---|---|---|
| `"42"`（数字字符串） | number | 解析后转换（保留原始值） |
| `"true"` / `"False"` | boolean | 大小写归一 |
| `"1,200.5"`（千分位） | number | 去分隔符转换 |
| `"2026年8月9日"` | date | 日期解析 |
| 空字符串 `""` | null | 归一为 null |

> 💡 原则：**解析器做"宽容输入、严格输出"**——输入容忍模型的各种字符串癖，输出永远给规范类型。这也是 stable-stream-core 等库内置类型强转的原因。

## 4. 枚举：约束输出的第一利器

| 做法 | 说明 |
|---|---|
| schema 中定义 enum | 模型只能选列表内的值（strict 下强制） |
| 枚举值语义化 | `status: ["paid", "pending", "cancelled"]` 而非任意串 |
| 枚举 + 描述 | 每个枚举值带说明（模型选得更准） |
| 未知值处理 | 落到 `unknown` 兜底而非崩溃 |

```json
{
  "status": {"type": "string", "enum": ["paid", "pending", "cancelled", "unknown"],
             "description": "paid=已支付, pending=待支付, cancelled=已取消, unknown=无法判定"}
}
```

> 🎯 核心要点：**枚举是"零成本的准确率提升"**——把自由文本变成选项，模型的选择空间被约束，准确率与可校验性同时上升（2026 评测实践第一条）。

## 5. 嵌套与复杂结构

| 结构 | 要点 |
|---|---|
| 嵌套对象 | 层级 ≤5（strict 限制）；语义分组（customer 内嵌 address） |
| 数组 | 顶层必须包 object（strict 限制）；数组元素用对象 |
| 可选字段 | strict 下用 null union（`["string","null"]`） |
| 联合类型 | 避免重叠 oneOf/anyOf（strict 要求确定性） |

```json
{
  "type": "object",
  "properties": {
    "customer": {"type": "object",
      "properties": {"name": {"type": "string"}, "email": {"type": ["string", "null"]}},
      "required": ["name", "email"], "additionalProperties": false},
    "items": {"type": "array", "items": {"type": "object",
      "properties": {"sku": {"type": "string"}, "qty": {"type": "integer"}},
      "required": ["sku", "qty"], "additionalProperties": false}}
  },
  "required": ["customer", "items"],
  "additionalProperties": false
}
```

## 6. 解析器的通用接口设计

| 接口 | 职责 | 实现 |
|---|---|---|
| `parse(text) -> dict` | 文本 → 对象（宽容输入） | 剥围栏 + JSON.parse + 修复 |
| `validate(obj) -> list[Error]` | 对象 → 错误列表（不抛异常） | schema 校验（08 篇） |
| `parse_or_repair(text) -> dict` | 失败自动修复重试 | 修复工具链（04 篇） |
| `to_typed(obj) -> Model` | 对象 → 类型化（Pydantic/Zod） | 框架层（06 篇） |

> 💡 接口设计的核心：**parse 与 validate 分离**——解析管"语法"，校验管"语义"，各自独立可测。混在一起的结果是"解析报错还是校验报错都分不清"。

## 7. 格式基础常见坑速查

| 坑 | 现象 | 解法 |
|---|---|---|
| 忘剥围栏 | JSON.parse 失败 | 提取函数内置剥围栏 |
| 数字字符串未转 | 类型错误进下游 | 类型转换层 |
| 枚举没兜底 | 新值崩管道 | unknown 兜底 |
| 顶层数组 | strict 拒绝 | 包一层 object |
| 可选字段写 required | strict 拒绝 | null union |
| 代理对乱码 | emoji 变乱码 | 按代理对解码 |
| 只解析不校验 | 静默错误 | validate 必接 |

## 8. 非 JSON 格式的专项处理

| 格式 | 解析要点 | 适用场景 |
|---|---|---|
| JSONL | 每行一个对象，逐行解析 | 批量抽取/日志流 |
| YAML | 缩进敏感——LLM 生成易错 | 配置类（少用） |
| XML | 标签配对校验 | 遗留系统对接 |
| Markdown 表格 | 按行分割 + 表头映射 | 人类可读的表格输出 |
| 代码块（```json） | 剥围栏 | 对话式输出 |

```python
# Markdown 表格 → JSON（人读机器用的桥梁）
def md_table_to_json(md: str) -> list[dict]:
    lines = [l.strip() for l in md.splitlines() if l.strip().startswith("|")]
    if len(lines) < 2: return []
    headers = [h.strip() for h in lines[0].strip("|").split("|")]
    return [dict(zip(headers, [c.strip() for c in l.strip("|").split("|")]))
            for l in lines[2:]]
```

> 💡 记忆口诀：**"能 JSON 就 JSON"**——非 JSON 格式只用于"人类可读优先"的场景，且都应在进入流程前转成 JSON。

## 9. 解析器的错误类型体系

| 错误类型 | 抛出时机 | 处理方 |
|---|---|---|
| ParseError | 语法解析失败 | 修复链（04 篇） |
| ValidationError | schema/语义校验失败 | 错误回传（08 篇） |
| RefusalError | 模型安全拒绝 | 提示用户 |
| TruncationError | finish_reason=length | 增上限/分段 |
| RetryExhausted | 重试超限 | 错误契约 |

```python
# 错误层级设计：可编程捕获 + 可区分处理
try:
    data = parse_or_repair(text)
except TruncationError:
    return retry_with_bigger_limit(text)      # 截断 → 增上限
except RefusalError:
    return {"status": "refused"}              # 拒绝 → 不重试
except RetryExhausted:
    return error_contract("parse_failed")     # 全部失败 → 错误契约
```

> 💡 错误类型体系的工程价值：**调用方按错误类型分流处理**（截断增上限、拒绝提示、解析失败重试），而不是对一切错误做同一件事——这就是 04 篇"失败分类先行"的类型化实现。

## 10. JSON Schema 关键字速查（解析器核心工具）

| 关键字 | 作用 | 解析器用途 |
|---|---|---|
| type | 类型约束 | 类型校验 |
| required | 必填 | 缺字段检测 |
| enum | 枚举 | 值域校验 |
| properties | 属性定义 | 嵌套校验 |
| additionalProperties | 额外属性控制 | 防脏字段 |
| anyOf/oneOf | 联合类型 | 复杂结构 |
| items | 数组元素 | 数组校验 |
| format | 格式（email/date） | 格式校验 |
| pattern | 正则 | 格式校验 |
| minimum/maximum | 数值范围 | 值域校验 |

> 💡 掌握这 10 个关键字即掌握 JSON Schema 90% 用法——**解析器的 schema 校验就是"按这些关键字逐项核对"**。注意区分：JSON Schema 是"描述结构"的规范（03 篇 strict 用它），校验是"核对输出"的动作（08 篇）——同一份 schema 两头用，一份契约两处消费，这正是 09 篇"契约即文档"的由来。

## 11. 类型转换函数清单（模型的字符串癖对策）

| 函数 | 输入→输出 | 处理 |
|---|---|---|
| to_int | "42"/"1,200" → 42/1200 | 去分隔符 |
| to_float | "12.50" → 12.5 | 去货币符号 |
| to_bool | "true"/"False"/"yes" → True/False | 大小写归一 |
| to_date | "2026年8月9日"/"08-09" → ISO | 多种格式 |
| to_null | "" / "null" / "-" → None | 空值归一 |
| to_enum | 模糊值 → 最近枚举 | 映射兜底 |

> 💡 转换原则：**转换保留原始值用于审计**（转前转后都留痕）；无法确定转换时落 null/unknown 而非猜——"猜错"比"留空"更危险。转换规则应随契约文件管理（09 篇），模型描述中写明单位与格式可大幅减少转换需求——"描述写清，转换少做"。转换是解析器与业务层的交界：解析器只做"无损转换"（数字字符串→数字），"有损推断"（模糊值猜枚举）必须留给业务层决策。这条交界线的意义：解析器保持"纯函数"性质（同输入同输出、可测），业务推断留在业务层（可评审、可回滚）。面试速记：格式基础三问——"用什么格式（JSON 默认）、模型哪些癖好要转（数字字符串）、结构怎么约束（schema 关键字）"——三问答全即覆盖本章。

---

**下一模块**：[03-严格模式：Strict Outputs 深潜](03-严格模式：Strict-Outputs深潜.md)　**返回总览**：[00-Output Parser 输出解析器总览](00-OutputParser输出解析器总览.md)

## 参考来源

- [How to use OpenAI structured outputs（Apidog）](https://apidog.com/blog/openai-structured-outputs/)
- [Structured Output & JSON Mode（CrazyRouter）](https://crazyrouter.com/en/blog/structured-output-json-mode-ai-api-guide-2026)
- [Evaluating LLM Structured Output Modes (2026)（FutureAGI）](https://futureagi.com/blog/evaluating-llm-structured-output-modes-2026/)
- [stable-stream-core（npm）](https://socket.dev/npm/package/@vjvkrm/stable-stream-core)
