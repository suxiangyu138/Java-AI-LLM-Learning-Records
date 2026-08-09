# 工具定义：JSON Schema 契约设计

> 工具定义 = 模型看得见的全部：**name + description + JSON Schema 参数契约**。2026 共识：定义是"产品"、实现是"细节"——描述含示例提升约 25% 选择准确率；缺参数描述导致幻觉参数；每多一个参数就多一分幻觉风险。本章给出一套可直接套用的契约设计规范。

## 1. 三要素：定义的结构

| 要素 | 职责 | 质量红线 |
|---|---|---|
| name | 唯一标识、供模型引用 | 动词开头；camelCase；绝不重名；不用 do_xxx 泛名 |
| description | 模型决策依据：何时用、怎么用、边界 | 3-4 句起步；含示例；说清与其他工具的区分 |
| parameters | 参数契约：类型、约束、每个参数的描述 | 全参数描述；flat；枚举/边界用约束表达 |

```json
{
  "type": "function",
  "function": {
    "name": "create_issue",
    "description": "在指定仓库创建 GitHub Issue。当用户要求记录 bug、需求或任务时使用；若只想查看已有 Issue 用 list_issues；若仓库不存在或名称含空格先调用 search_repository 确认。示例：create_issue(repo='myapp', title='登录页 500', labels=['bug'])。",
    "parameters": {
      "type": "object",
      "properties": {
        "repo": {
          "type": "string",
          "description": "目标仓库名，格式 owner/name，如 'acme/myapp'。"
        },
        "title": { "type": "string", "description": "Issue 标题，10-100 字符。" },
        "body": { "type": "string", "description": "Issue 正文，可选，默认空。" },
        "labels": {
          "type": "array",
          "items": { "type": "string", "enum": ["bug", "feature", "docs"] },
          "description": "标签列表，只能从枚举中选择，可选。"
        }
      },
      "required": ["repo", "title"],
      "additionalProperties": false
    }
  }
}
```

> 🎯 核心要点：模型做两个决定——**"该不该调"看 description，"参数怎么填"看 schema**。两处都要写到位，缺一个就是一半准确率。

## 2. 命名规范：工具名的工程标准

| 规则 | 反例 | 正例 |
|---|---|---|
| 动词开头 | file_handler | read_file / write_file |
| 动作-对象 | do_github | create_issue / list_repositories |
| camelCase | get_user_by_id | getUserById（OpenAI 生态） |
| 域前缀分组 | search | issues_search / issues_create |
| 绝对标识 | 传相对路径 | 绝对路径（Anthropic 实证消除整类 Agent 错误——poka-yoke 原则） |
| 禁用泛名 | exec、run、shell | 拆成 git_status、fs_read 等具体动词 |

> ⚠️ 2026 硬拒绝清单（agentpatterns 标准）：通用 shell/命令执行工具（必须拆成具体动词）；封闭值集无枚举；重复描述；无超时的工具——工具必须带执行上限。

## 3. description 黄金法则

| 法则 | 做法 | 收益 |
|---|---|---|
| 讲清"何时用" | 触发场景 + 反例（"不需要时别调"） | 减少无谓调用 |
| 讲清"何时不用" | 明确与其他工具的边界 | 减少选错工具 |
| 给示例 | 调用格式示例直接写进描述 | 准确率约 +25% |
| 给约束 | 输入格式要求、长度限制 | 减少幻觉参数 |
| 给前置条件 | 依赖其他工具先执行时写明 | 减少顺序错误 |
| 3-4 句起步 | 复杂工具描述要充足 | 复杂工具描述不足 = 随机选择 |

> 💡 描述自测三连：① 换个 LLM 只看描述能正确调用吗？② 两个工具描述对调，模型会选错吗？③ 描述里有没有至少一个具体例子？——任一答"否"，重写描述。

## 4. JSON Schema 设计规范

| 维度 | 规范 | 反模式 |
|---|---|---|
| 扁平化 | 顶层属性优先，少嵌套 | 深嵌套对象（token 高、解析易错） |
| 全参数描述 | 每个 property 都要 description | 只给类型不给描述（模型猜参数名） |
| 参数数量 | 最小化，够用即可 | 每多一个参数多一分幻觉 |
| 封闭值集 | 用 enum 表达 | 让模型自由发挥字符串 |
| 数值边界 | minimum/maximum | 无界数字 |
| 文本长度 | minLength/maxLength | 无限长字符串 |
| 结构 | required 明确 + additionalProperties:false | 松散对象、dict[str, Any]（那不是 schema） |
| 自由文本 | 设长度上限 | 无限自由文本（提示注入面，08 篇） |

> ⚠️ 反模式警示：`{"type": "object"}` 无 properties 的"空 schema"等于没约束——模型会自由发挥出任何键；`dict[str, Any]` 是给模型"自由写作许可证"。**Schema 的使命是收窄可能空间，不是记录参数名**。

## 5. strict 模式：契约的硬约束

| 项 | 说明 |
|---|---|
| 是什么 | OpenAI/Anthropic 的 `strict: true`：模型生成的参数保证符合 schema |
| 硬规则 | 所有 object 必须 `additionalProperties: false`；所有属性必须进 `required`（不允许 optional） |
| 收益 | 参数结构 100% 合规，省掉解析层兜底 |
| 代价 | optional 参数要改写成 required + null 联合，或带默认值重新设计 |
| 适用 | 生产环境、参数错误会导致下游失败的工具 |

> 🎯 核心要点：strict 的取舍——**结构保证换灵活性**。参数少且稳定的工具无脑开 strict；参数多、大量 optional 的工具要评估改写成本（Output Parser 03 篇有 strict 深潜，本体系只取工具侧结论）。

## 6. 契约的 token 预算

| 数据点 | 值 |
|---|---|
| 单工具定义典型成本 | 100-200 token/个（含 schema） |
| 工具总数红线 | <20 建议；30 硬拒绝（agentpatterns） |
| 单上下文准确率边界 | 10-15 个（Haiku 级模型实测，MCP 2026 研究） |
| 定义占上下文比例 | 建议 <10%；>30% 必做动态发现 |

```text
定义瘦身三板斧：
  1. 描述精简去重复（示例只留最有区分度的一个）
  2. schema 扁平化 + 公共类型抽取（枚举/时间格式复用定义）
  3. 按需加载（namespace/tool_search，05 篇）
```

> ⚠️ 上下文不是免费的：20 个工具 × 150 token = 3000 token，占 128K 窗口的 2.3%，但占 8K 窗口的 37%——**小上下文模型必须动态发现**。

## 7. 定义与验证分离：2026 多层校验范式

| 层 | 工具 | 职责 |
|---|---|---|
| 定义层 | JSON Schema | 行业互转格式，各协议统一（OpenAI `parameters`/Anthropic `input_schema`/MCP `inputSchema`） |
| SDK 层 | Pydantic/Zod 等 | 代码内类型化契约，编译期+运行期双校验 |
| 网关层 | 网关校验 | 防御纵深：参数形状、大小、枚举二次校验 |
| 业务层 | 自定义校验 | 数据库查重、业务规则、权限——解析后的独立步骤 |

> 💡 三层校验不是冗余：SDK 校验结构、网关校验形状、业务校验语义——2026 标准（FutureAGI）建议"契约 = 类型化 schema + SDK 校验 + 网关校验 + 业务校验"，四层缺一不可。

## 8. 常见误区

| 误区 | 真相 |
|---|---|
| "描述 = 功能说明书" | 描述是**决策依据**，要写何时用/何时不用/示例 |
| "schema 越完整越好" | 参数数最小化——多一个参数多一分幻觉 |
| "strict 模式万能" | strict 只保证结构合规，不保证语义正确（要 07 篇业务校验） |
| "工具定义一次性写完" | 定义要随真实调用数据迭代——错误调用即契约改进信号 |
| "枚举限死了能力" | 枚举限死的是**幻觉空间**，模型更准而不是更笨 |
| "示例占 token 不划算" | 一个示例 ~25% 准确率提升，是 ROI 最高的 token 投资 |
| "描述重复没关系" | 重叠描述使模型无法区分工具——注册期就要去重（02 篇） |

## 9. 面试速记

| 问题 | 一句话答案 |
|---|---|
| 工具定义三要素？ | name + description + JSON Schema（模型只见这三样） |
| 描述为什么重要？ | 决定"该不该调/调哪个"；示例约 +25% 准确率 |
| 命名规范？ | 动词开头、动作-对象、camelCase、域前缀、绝对标识、禁泛名 |
| 2026 硬拒绝工具？ | 通用 shell 工具、无枚举封闭值集、重复描述、无超时 |
| schema 设计要点？ | 扁平、全参数描述、枚举、边界约束、additionalProperties:false |
| strict 模式代价？ | 全 required + additionalProperties:false——结构换灵活性 |
| 工具定义 token 预算？ | 100-200 token/个；<20 个；单上下文 10-15 准确率边界 |
| 空 schema 问题？ | 等于没约束，模型自由发挥——schema 使命是收窄可能空间 |
| 四层校验？ | 定义层(Schema) + SDK 层(Pydantic/Zod) + 网关层 + 业务层 |
| 描述写多长？ | 3-4 句起步，复杂工具更长；示例必含 |
| 描述自测三连？ | 换模型能用吗、对调会选错吗、有示例吗 |
| 定义要不要迭代？ | 要——错误调用即契约改进信号，定义是产品 |

---

**下一模块**：[04-工具声明与协议：从提示词到原生协议](04-工具声明与协议：从提示词到原生协议.md)　**返回总览**：[00-Tool 工具开发与注册总览](00-Tool工具开发与注册总览.md)

## 参考来源

- [Tool Calling Schema Standards（agentpatterns）](https://github.com/agentpatterns-ai/website/blob/main/standards/tool-calling-schema-standards.md)
- [Ultimate Guide: How AI Agents Use Tools — 2026（Skywork）](https://skywork.ai/blog/ai-agents-using-tools-ultimate-guide-2026/)
- [What is LLM Input/Output Validation? The 2026 Explainer（FutureAGI）](https://futureagi.com/blog/what-is-llm-input-output-validation-2026/)
- [agent-tool-builder SKILL.md（agent-skills-hub）](https://github.com/agent-skills-hub/agent-skills-hub/blob/main/skills/agent-tool-builder/SKILL.md)
