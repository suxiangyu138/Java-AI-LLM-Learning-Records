# Output Parser 输出解析器知识体系总览

> 定位：Agent 子组件专项之「输出解析器」——模型输出是"建议"不是"数据"：解析器负责把模型输出变成可靠的结构化契约。2026 年核心：strict 模式成默认、评测从"解析率"转向"语义质量"。总览做索引与速查，子主题各一篇。

## 📚 目录

1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [学习路线推荐](#3-学习路线推荐)
4. [核心概念速查](#4-核心概念速查)
5. [2026 生态基准](#5-2026-生态基准)

## 1. 知识体系导图

```text
Output Parser 输出解析器
├── 01 输出解析器全景                      契约三环节 / 失败代价 / 与协议层分工
├── 02 格式基础：JSON 与结构化契约          JSON 系列 / 类型转换 / 枚举 / 嵌套
├── 03 严格模式：Strict Outputs 深潜        strict 保证 / schema 约束 / 各家对比
├── 04 解析失败处理：修复与重试            失败模式目录 / 修复工具 / fallback 链
├── 05 流式输出解析                        流式组装 / 修复 vs 部分解析 / 增量解析器
├── 06 框架中的输出解析器                  LangChain / Spring AI / LangChain4j
├── 07 函数调用与结构化输出：工具参数解析   tool_calls 即契约 / 参数累积 / 强制调用
├── 08 校验与安全：解析后的防线            schema 校验 / 业务校验 / 输出注入防御
├── 09 输出契约设计实战                    契约模式 / 命名 / 版本化 / 跨层衔接
└── 10 生产实践与面试冲刺                  语义评测 / 12 避坑 / 面试题
```

## 2. 模块导航

| 序号 | 模块 | 核心内容 | 适合人群 |
|---|---|---|---|
| 01 | [输出解析器全景](01-输出解析器全景.md) | 解析-校验-修复三环节、失败代价 | 全部（地基） |
| 02 | [格式基础：JSON 与结构化契约](02-格式基础：JSON与结构化契约.md) | JSON 系列、类型转换、枚举、嵌套 | 全部（地基） |
| 03 | [严格模式：Strict Outputs 深潜](03-严格模式：Strict-Outputs深潜.md) | strict 保证、schema 约束、各家对比 | 落地开发者 |
| 04 | [解析失败处理：修复与重试](04-解析失败处理：修复与重试.md) | 失败模式目录、修复工具、fallback | 落地开发者 |
| 05 | [流式输出解析](05-流式输出解析.md) | 流式组装、修复 vs 部分解析、增量解析器 | Agent 工程师 |
| 06 | [框架中的输出解析器](06-框架中的输出解析器.md) | LangChain/Spring AI/LangChain4j 对比 | 落地开发者 |
| 07 | [函数调用与结构化输出](07-函数调用与结构化输出：工具参数解析.md) | tool_calls 即契约、参数累积、强制调用 | Agent 工程师 |
| 08 | [校验与安全：解析后的防线](08-校验与安全：解析后的防线.md) | schema/业务校验、refusal 处理、注入防御 | 安全关注者 |
| 09 | [输出契约设计实战](09-输出契约设计实战.md) | 契约模式、命名、版本化、跨层衔接 | 架构师 |
| 10 | [生产实践与面试冲刺](10-生产实践与面试冲刺.md) | 语义评测、12 避坑、面试题 | 面试/上线前 |

## 3. 学习路线推荐

| 路线 | 路径 | 目标 |
|---|---|---|
| 入门（1 天） | 01 → 02 → 03 → 10 | 理解结构化输出原理，能配置 strict 模式 |
| 进阶（1 周） | 01-03 → 04 → 05 → 06 → 10 | 能处理失败与流式，会选框架解析器 |
| 高级（2 周） | 全量 + 07 → 08 → 09 | 能设计契约体系与语义级评测 |

## 4. 核心概念速查

| 概念 | 一句话定义 |
|---|---|
| 输出解析器 | 把模型输出（文本）转成结构化数据并校验的组件 |
| 结构化输出（Structured Outputs） | 模型按 JSON Schema 生成（strict 模式在采样层约束） |
| Strict 模式 | `response_format: {"type":"json_schema","strict":true}`——2026 默认 |
| JSON 模式（legacy） | 只保证合法 JSON，不保证 schema——2026 视为过时 |
| 约束解码（Constrained Decoding） | 采样时按文法强制生成合法结构（strict 的实现机制） |
| 契约（Contract） | 输出结构的 schema 定义（record/枚举/嵌套） |
| 失败模式目录 | refused（拒绝）/partial（残缺）/truncated（截断）/degraded（降级） |
| 修复（Repair） | 把残缺 JSON 补成合法 JSON（jsonmend/repair-json-stream） |
| 部分解析（Partial Parse） | 增量解析出"不完整但可用"的对象（jsiphon/jsoncurrent） |
| 补丁协议 | 流式增量传输（add/append/insert/complete 四操作，jsoncurrent） |
| 语义评测 | 不只测"解析率"，测字段级语义正确率（2026 转向） |
| tool_calls 解析 | 函数调用参数本身就是结构化输出（按 index 累积） |
| 输出注入 | 模型输出中夹带的指令性内容（解析后需分域） |
| 模式间评测 | 按"模式"对比而非按"模型"对比（strict vs JSON vs tool use） |

## 5. 2026 生态基准

> 📅 基准窗口：2026-08。版本与事实以各模块【参考来源】为准。

- **Strict 模式成默认**：OpenAI strict（json_schema + strict:true）在采样层保证 schema 合规（必填字段/无额外属性/类型/枚举/嵌套）；legacy JSON mode 只保证语法合法，2026 视为过时；Amazon Bedrock 也推出结构化输出（Draft 2020-12 子集 + 24h 文法缓存）。
- **各家强制力不同**：Anthropic 无 strict 标志——用 tool use + `tool_choice` 强制 schema（提示词级非采样级）；Gemini 用 responseMimeType + responseJsonSchema（5+ 成员 union 有已知失败）；**OpenAI 兼容厂商（DeepSeek 等）接口兼容但强制力各异**——"兼容 ≠ 同能力"必须实测。
- **Schema 约束清单**：全部字段必填（可选性用 null union 表达）、顶层必须是 object、所有对象 additionalProperties:false、嵌套约 5 层/约 100 属性、无重叠 oneOf/anyOf、首请求文法编译较慢（预热 + 复用 schema）。
- **失败处理成熟**：失败模式目录（refused/partial/truncated/degraded）成为排查框架；修复工具链成熟（jsonmend 单遍流式修复、repair-json-stream 增量修复）；重试成本要审计（约束解码的重试被头榜解析率掩盖）。
- **流式解析两流派**：修复派（补齐残缺 JSON）vs 部分解析派（产出不完整但可用的对象——jsiphon 永不出错、jsoncurrent 补丁协议、stable-stream-core 零布局偏移）；真实陷阱：Unicode 代理对（emoji）、转义序列、流中途歧义。
- **评测范式转向**：从"JSON 解析率"转向"语义质量"（schema 合规 + 字段级语义打分）；按模式对比而非按模型对比（同模型 strict vs JSON vs tool use 差异显著）；镜像生产 schema 复杂度（5 字段测试 schema 掩盖 50 字段嵌套退化）；识别"静默失败格"（schema 合规但语义错）。
- **与体系分工**：协议层（tool_calls 格式/回传）在 [Function Calling 函数调用](..%2F..%2F..%2F02-大模型基础与Prompt工程%2FFunction%20Calling%20函数调用%E3%80%90Agent%20基石%E3%80%91%2F00-FunctionCalling知识体系总览.md) 体系；本体系讲"任何结构化输出的解析-校验-修复全流程"。

---

**下一模块**：[01-输出解析器全景](01-输出解析器全景.md)

## 参考来源

- [Evaluating LLM Structured Output Modes (2026)（FutureAGI）](https://futureagi.com/blog/evaluating-llm-structured-output-modes-2026/)
- [OpenAI Structured Outputs vs JSON Mode (2026 Guide)（Respan）](https://www.respan.ai/articles/openai-structured-outputs-vs-json-mode)
- [Structured Output & JSON Mode: Get Reliable AI Responses（CrazyRouter）](https://crazyrouter.com/en/blog/structured-output-json-mode-ai-api-guide-2026)
- [Structured outputs on Amazon Bedrock（AWS）](https://aws.amazon.com/cn/blogs/machine-learning/structured-outputs-on-amazon-bedrock-schema-compliant-ai-responses/)
- [How to use OpenAI structured outputs（Apidog）](https://apidog.com/blog/openai-structured-outputs/)
- [jsoncurrent（npm）](https://www.npmjs.com/package/jsoncurrent)
- [jsonmend（PyPI）](https://www.piwheels.org/project/jsonmend/json/)
- [jsiphon（npm）](https://www.npmjs.com/package/jsiphon)
