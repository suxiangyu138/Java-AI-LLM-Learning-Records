# 系统提示词全景：Agent 的宪法

> 系统提示词 = Agent 的"宪法"——每次轮次前运行，定义身份、能力、约束与输出格式。2026 设计共识：**六段解剖 + CARE 结构 + 约束层级**；165 个生产 system prompt 提炼 15 模式四层。本章立起全景：解剖、结构、约束层级与常见错误。

## 1. 系统提示词的定位

| 项 | 说明 |
|---|---|
| 宪法类比 | 定义 Agent 是谁、能做什么、不能做什么、怎么输出 |
| 运行时机 | 每次轮次前（非一次性）——持续生效 |
| 与 user prompt 区别 | system 是制度，user 是当次任务 |
| 与工具声明区别 | system 讲"怎么行为"，tools 讲"有什么能力"（07 篇） |

> 🎯 核心要点：**系统提示词是 Agent 唯一"稳定不变"的部分**——模型换了、工具变了、用户变了它都在；它的质量决定行为基线，漂移检测（06 篇）盯的就是它。

## 2. 六段解剖

| 段 | 内容 | 位置原则 |
|---|---|---|
| 身份/角色 | 谁（领域专家/助手） | **重要内容靠前**（模型权重偏早） |
| 能力 | 能做什么（工具/知识） | 靠前 |
| 约束 | 不能做什么（MUST NOT） | 靠前 |
| 输出格式 | 响应结构 | 中 |
| 示例 | 具体示范 | 中后 |
| 行为规则 | 多轮/边界处理 | 后 |

> ⚠️ 2026 实证：**模型对靠前内容权重更高**——"入职第一天向新人简报"式顺序（先角色后职责再约束再语气）；把输出格式放最前的模板浪费了高权重位置。

## 3. CARE 结构：稳定行为的模板

| 段 | 内容 | 例子 |
|---|---|---|
| Context | 背景与目标 | "你是电商客服 Agent，处理订单查询" |
| Ask | 明确要求 | "回答前先查订单状态" |
| Rules | 行为规则 | "绝不报价、绝不承诺时效" |
| Examples | 2-3 个示范 + 1 个拒绝示范 | 好的回答/恰当的拒绝各一 |

> 💡 CARE 的价值：**示例是"行为锚"**——2-3 个好示例 + 1 个恰当拒绝示例，比十条抽象规则更有效（与评估体系 few-shot 校准同源）。

## 4. 约束层级：冲突时的裁判

```text
约束优先级（冲突时）：
  安全约束（最高）
    > 系统提示词指令
    > 用户指令
    > 上下文示例
    > 模型默认行为（最低）
```

| 层级 | 例子 |
|---|---|
| 安全 | 绝不泄露凭据（最高，不可覆盖） |
| System | 角色/范围/格式 |
| User | 当次任务要求 |
| 示例 | few-shot 示范 |
| 默认 | 模型训练行为 |

> 🎯 核心要点：**约束层级必须显式写进系统提示词**——模型在冲突时按层级裁决；2026 指令层级研究（模型原生优先 system over user/tool 结果）与本层级互补（09 篇）。

## 5. 硬约束 vs 软约束

| 类型 | 标记 | 用途 | 例子 |
|---|---|---|---|
| 硬约束 | MUST / MUST NOT | 安全关键、不可违反 | "MUST NOT 提供医疗诊断" |
| 软约束 | SHOULD / PREFER | 风格偏好 | "PREFER 简洁回答" |

> ⚠️ 2026 实证细节：**新模型对柔和语言遵循更可靠**——过度使用 MUST/NEVER/DO NOT 反而降低遵循率；"每条规则只出现一次、不重复"（重复规则互相稀释）。硬约束的数量要少而精（安全关键才硬）。

## 6. 2026 模式库：15 模式四层（165 个生产提示词提炼）

| 层 | 模式 |
|---|---|
| 核心架构 | Persona/人格/工具/安全/记忆 |
| 交互控制 | 输出格式/对话流/搜索引用 |
| 工程支持 | 上下文管理/委派/注入防御 |
| 场景适配 | 语音/移动/编码 Agent |

> 💡 模式库的意义：**设计不是从零开始**——按层查模式（Persona 见 02 篇、上下文管理见 03 篇、注入防御见 09 篇）；"15 模式四层"是 2026 的设计索引。

## 7. 常见错误清单（2026）

| 错误 | 后果 | 解法 |
|---|---|---|
| >4000 token | 注意力稀释 | <2000，细节进参考文档（03 篇） |
| 矛盾指令 | 行为随机 | 冲突审计 + 约束层级 |
| 穷举边界 | token 浪费 | 讲原则 + 模糊场景给示例 |
| 无输出格式 | 解析不稳定 | 明确格式（Output Parser 联动） |
| 工具无错误处理 | 失败卡死 | 错误回退行为（Tool 06 篇） |
| 只测 happy path | 对抗场景崩 | 红队压测（09 篇） |
| 无版本化 | 不可回滚 | PromptOps（05/06 篇） |

> 🎯 核心要点：**七错误 = 设计的反面清单**——写完系统提示词对照七项自查；"无版本化"最隐蔽（上线时看不出，出问题时回不去）。

## 8. 面试速记

| 问题 | 一句话答案 |
|---|---|
| 系统提示词定位？ | Agent 宪法——每次轮次前运行，定义身份/能力/约束/格式 |
| 六段？ | 身份/能力/约束/输出格式/示例/行为规则 |
| 位置原则？ | 重要内容靠前——模型权重偏早 |
| CARE？ | Context/Ask/Rules/Examples——示例是行为锚 |
| 约束层级？ | 安全 > system > user > 示例 > 默认 |
| 硬软约束？ | MUST（安全关键）vs SHOULD（风格）——硬要少而精 |
| 新模型遵循？ | 柔和语言更可靠——过度 MUST 反降遵循 |
| 15 模式四层？ | 核心架构/交互控制/工程支持/场景适配 |
| 与 Prompt 工程体系分工？ | 02 层讲写作技巧，本体系讲 Agent 组件工程 |
| 常见错误？ | >4000 token/矛盾/穷举/无格式/无错误/只测 happy/无版本 |
| 宪法类比？ | 唯一稳定部分——漂移检测盯它 |
| 与 tools 分工？ | system 讲怎么行为，tools 讲有什么能力 |

---

**下一模块**：[02-角色设计：Persona 与身份](02-角色设计：Persona与身份.md)　**返回总览**：[00-Prompt 角色配置组件总览](00-Prompt角色配置组件总览.md)

## 参考来源

- [Design Advanced Prompting Strategies for Production AI Agents（Microsoft Learn）](https://learn.microsoft.com/zh-tw/training/modules/aaai-design-advanced-prompt-production-agents/)
- [system-prompt-design.md（oakoss/agent-skills）](https://github.com/oakoss/agent-skills/blob/0283bed313563d5677a0838f4bf921b03296cf6c/skills/expert-instruction/references/system-prompt-design.md)
- [Agent Persona: Definition, Metrics & Guide（FutureAGI）](https://futureagi.com/glossary/agent-persona/)
- [system-prompts.md（yo-steven/agents-exploration）](https://github.com/yo-steven/agents-exploration-20260523/blob/main/plugins/llm-application-dev/skills/prompt-engineering-patterns/references/system-prompts.md)
