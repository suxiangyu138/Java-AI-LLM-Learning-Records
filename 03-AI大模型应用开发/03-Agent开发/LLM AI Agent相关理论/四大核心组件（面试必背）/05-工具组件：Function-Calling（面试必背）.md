# 工具组件：Function-Calling（面试必背）

> 背诵篇：四大核心组件之「工具组件」——30 秒背诵卡 + 6 个面试高频题 + schema 铁律与目录治理。深化见 [Agent 四大核心组件 06 篇](..%2F..%2FAgent%20四大核心组件%2F06-工具组件：Function-Calling与工具注册.md)，速记见 [Tools & Skill 最简定义](..%2F..%2FAgent组件%2F四大基础核心组件（最简定义）%2FTools%20%20Skill%20工具组件（手脚）%2F00-ToolsSkill工具组件最简定义总览.md)。

## 1. 30 秒背诵卡

| 概念 | 背诵版一句话 |
|---|---|
| 工具组件 | Agent 的"手和脚"——通过 Function-Calling/MCP 连接外部世界（API/代码/数据库/文件/浏览器） |
| 机制 | 工具描述（JSON Schema）进上下文 → 模型输出结构化调用意图 → Harness 执行 → 结果回填 |
| schema 铁律 | 枚举优于自由串 / additionalProperties:false 收口 / 参数级描述 / 必填精简 |
| 描述铁律 | **工具描述是准确度第一大因素**——做什么/何时用/何时不用/边界/例子 |
| 目录治理 | 活动工具集 <20 个；工具检索（从 60 个里选 8 个相关）；分层两级选择 |
| 2026 协议 | MCP 成为工具连接事实标准（统一 JSON-RPC，即插即用） |
| 关键实证 | ~38% 生产 Agent 失败源于 payload 幻觉；多轮编排比单轮掉 30-60pp（BFCL V4） |
| 模型基线 | BFCL v3：GPT-5.x ~89% / Claude Opus 4.7 ~87%（无关性检测最强）/ Gemini 3 Pro ~85% |

## 2. 面试高频问题（背诵版）

**Q1. 工具组件在 Agent 里起什么作用？**
> 答：弥补 LLM 三大先天局限——① **只出文本不能行动**（工具执行真实操作）；② **知识截止与幻觉**（工具检索外部事实）；③ **无外部状态**（工具读写系统数据）。工具让"意图"变"行动"，是 Chatbot 与 Agent 的分水岭之一。机制闭环：schema 声明 → 模型选工具给参数 → 执行 → 观察回填 → 再决策（ReAct 的 Action/Observation 就是工具组件在循环里的位置）。

**Q2. 工具 schema 设计有哪些铁律？（面试必答 5 条）**
> 答：① **用枚举不用自由字符串**（`status: "approved"|"rejected"|"pending"` 优于 `status: string`）；② **数值加范围约束**（minimum/maximum，防 `amount: -50`）；③ **字段名无歧义**（`from_account_id`/`to_account_id` 比 `from`/`to` 少被互换）；④ **`additionalProperties: false` 收口**（模型不能加你没处理的字段）+ strict 校验；⑤ **每个参数一句话描述**（"ISO-8601 UTC" 优于 "datetime"）+ 参数按重要性排序（模型可能截断）。避坑：`oneOf` 联合类型谨慎用——弱模型常选错分支，优先单对象 + 可选属性；嵌套对象/对象数组弱模型解析错误率更高（成功率：扁平属性 > 嵌套 > 数组 > oneOf）。

**Q3. 工具描述（description）怎么写？**
> 答：**描述是准确度第一大因素**——它是模型判断"何时调、怎么填参"的唯一依据。规范：做什么（action-oriented 命名：`get_weather` 优于 `weather_api`）、何时用、**何时不用**（区分相似工具如知识库搜索 vs 网页搜索）、参数含义与格式、边界与副作用、**具体正反例**——反例（标注 ❌ WRONG 的常见错误模式）比只给正确格式更有效，因为模型从"别这样做"学得更好。工具描述要和提示词一样认真写（Anthropic：给模型足够 token 思考、格式贴近自然文本、poka-yoke 防错设计——如 SWE-bench 用绝对路径后零失误）。

**Q4. 工具目录怎么治理？工具太多怎么办？**
> 答：核心是**活动工具集要小**——OpenAI 建议 <20 个；>30-50 个模型选择精度线性下降（LongFuncEval 实证，128K 上下文也救不了）。手段：① **工具检索**：先按任务召回相关工具（8 个相关 > 60 个大多无关）；② **两级选择**：先选类别再选工具（像"先开应用再找功能"）；③ **标签过滤**：按权限/领域/前置条件在入 prompt 前过滤（STEP_TOOL_MAP 按流程步骤加载）；④ **合并操作**：create/update/close_ticket 合并为一个 `manage_ticket`（action 枚举）——更少更强大的工具减少选择歧义；⑤ 命名空间前缀（`github_list_prs`）防同名冲突。

**Q5. 2026 年工具调用最大的失败来源是什么？**
> 答：**payload 幻觉**——模型凭记忆重建深层嵌套 schema（~38% 生产 Agent 失败源于此）；以及**多轮工具编排**——BFCL V4（2026-04）显示多轮编排准确率比单轮掉 30-60 个百分点（最强 68%，最差 14.8%），瓶颈是顺序选工具而非模型智能。对策：① schema 扁平化（少嵌套）；② 代码抽象层/组合工具（复杂 7 步工作流 token 减少 9 倍、轮次 3.9→2.0——注意 MCP 命名空间分组不够，要真抽象层）；③ 结构化错误回传让模型自修复；④ 有依赖的工具串行、无依赖才并行（并行调用默认是新风险面）。

**Q6. 工具调用怎么度量可靠性？**
> 答：三个独立失败模式分开度量——**JSONValidation**（schema 符合性，防非法 JSON/结构错误）、**FunctionCallAccuracy**（0-1 综合质量：名称/结构/类型/语义）、**ToolSelectionAccuracy**（"JSON 对了但工具选错"——工具丰富环境必测）；加 ParameterValidation（执行前参数守卫）与 SchemaCompliance；按函数名切片看 schema 违规率（不看聚合）；全部指标挂同一 OTel span 统一聚类。选模型按能力子集选（并行/无关性检测/轨迹状态），不只看总分。

## 3. 避坑与易混点

| 坑 | 澄清 |
|---|---|
| "工具描述随便写写" | 描述是准确度第一大因素——值得与提示词同规格 |
| "工具越多越强" | >30-50 个选择精度线性下降——检索/分层/合并治理 |
| "oneOf 很灵活" | 弱模型常选错分支——优先单对象+可选属性 |
| "MCP 分组 = 优化" | 仅命名空间分组反而 +15% token（BFCL 实验）——要代码抽象层 |
| 工具 vs MCP vs Skills | 工具=能力声明；MCP=工具协议（怎么接）；Skills=可复用技能包（怎么做）——见 [区分概念 02](../区分概念（面试容易混淆）/02-函数调用-vs-工具-vs-MCP-vs-Skills.md) |
| "函数调用 = Agent" | Function-Calling 是协议层——没有循环决策的调用仍是工作流 |

## 4. 面试速记

| 问题 | 一句话答案 |
|---|---|
| 工具组件作用？ | 弥补 LLM 三局限（不能行动/知识截止/无外部状态）——手和脚 |
| schema 五铁律？ | 枚举/数值范围/字段无歧义/additionalProperties:false/参数描述 |
| 描述怎么写？ | 做什么+何时用+何时不用+正反例——描述是准确度第一大因素 |
| 工具治理？ | <20 个活动工具 + 检索 + 两级选择 + 合并操作 + 命名空间 |
| 最大失败来源？ | payload 幻觉（38%）与多轮编排掉点（30-60pp） |
| 三指标？ | JSONValidation / FunctionCallAccuracy / ToolSelectionAccuracy |

---

**下一模块**：[06-行动组件：执行与反馈（面试必背）](06-行动组件：执行与反馈（面试必背）.md)　**返回总览**：[00-四大核心组件面试总览](00-四大核心组件面试总览.md)

## 参考来源

- [Ultimate Guide: How AI Agents Use Tools — 2026（skywork.ai）](https://skywork.ai/blog/ai-agents-using-tools-ultimate-guide-2026/)
- [Function Calling Definition | FutureAGI Guide (2026)](https://futureagi.com/glossary/function-calling/)
- [AI Agent Tool Design: What Works and What Doesn't（MachineLearningMastery）](https://machinelearningmastery.com/ai-agent-tool-design-what-works-and-what-doesnt/)
- [The Roadmap to Mastering Tool Calling in AI Agents（MachineLearningMastery）](https://machinelearningmastery.com/the-roadmap-to-mastering-tool-calling-in-ai-agents/)
- [Function calling best practices（Together AI）](https://docs.together.ai/docs/inference/function-calling/best-practices)
- [Building effective agents（Anthropic）](https://www.anthropic.com/engineering/building-effective-agents)
