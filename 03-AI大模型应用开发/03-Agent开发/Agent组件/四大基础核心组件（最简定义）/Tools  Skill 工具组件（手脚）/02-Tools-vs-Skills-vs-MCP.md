# Tools vs Skills vs MCP

> 概念速记：三个概念的定位区分。深潜见 [Tool 09 篇](..%2F..%2F..%2FAgent%20子组件专项学习%2FTool%20工具开发与注册%2F09-框架与生态：从注解到MCP.md) 与 [MCP 知识体系](..%2F..%2F..%2F..%2F06-MCP协议与Agent%20Skill%2FMCP学习%2FMCP%2F00-MCP知识体系总览.md)。

## 三者定位

| 概念 | 层 | 作用 |
|---|---|---|
| Function Calling | 基础协议 | 让模型"知道有哪些函数、能请求调用"（2022 OpenAI） |
| Tools | 标准化格式 | name+描述+输入/输出 schema——统一工具形态 |
| MCP | 基础设施 | "USB-C for AI"——任何兼容客户端连任何服务器（Linux Foundation） |
| Skills | **判断层** | SKILL.md 指令包：何时/顺序/条件调哪些工具（含护栏） |

> 🎯 2026 拆解：**"FC 是基础能力、MCP 标准化工具基础设施、Skills 编码判断"**——三者是叠加不是替代（阿里云 2026）。

## Tools vs Skills：能力 vs 胜任力

| 维度 | Tool | Skill |
|---|---|---|
| 判断在哪 | Agent 每轮推理 | SKILL.md 预写排序 |
| 状态 | 无状态（每次独立） | 跨步骤跟踪进度 |
| 失败模式 | 单次调用错 | 顺序错误跨步复合 |
| Token | 全 schema 每轮载入 | ~100 token 元数据常驻、按需加载 |
| 可组合 | 否（Agent 自己组合） | 设计可组合 |
| 治理 | 单调用权限 | 跨所有编排工具（治理缺口） |

> ⚠️ Skill 的治理警示：**skill 编排的每个工具调用都继承 skill 的"无治理横跨"**——"未完成步骤 1-2 绝不批退款"这类护栏写在 SKILL.md，但执行层要兜底（Guardrails 04 篇 execution rail）。

## 交接模式（2026）

| 模式 | 机制 |
|---|---|
| 固定管线排序 | SKILL.md 指示调 A → 检查结果 → 决定是否调 B |
| 渐进披露 | skill 只加载任务需要的工具（防 90+ schema 每轮全载）——token 优化关键 |
| MCP 交付面 | skill 经 MCP/API 触达工具——MCP 标准化暴露与认证 |

> 💡 金句（Sentry 联创）：**"如果 skills 教你做饭，MCP 提供让你做到的厨具"**——Skills 教怎么工作，MCP 工具告诉能碰什么系统。

## 2026 实证

| 评测 | 结论 |
|---|---|
| Arize（MCP vs CLI skills） | 正确性窄带（0.826-0.834 差不多）；**MCP 贵 6x 慢 5x**（12 调用 vs 5）——"正确性差不多，效率差 6 倍" |
| Supabase（MCP+Skills） | 安全关键任务 **100% 成功率**（基线 0%、纯 MCP 会"猜"组合）——组合是生产答案 |

## 选型

| 场景 | 选择 |
|---|---|
| 本地文件/终端流程 | 先 Skills（规划/编码/调试行为） |
| 外部系统（API/DB/认证） | 先 MCP 工具 |
| 生产 Agent | **两者都用**：Skills 管稳定知识/工作流，MCP 管实时受治系统 |

> 🎯 一句话：**"生产默认 use both"**——没有单层替代另一层（LlamaIndex/Atlan 2026 共识）；警示：两层都不认证数据真实性（治理层在其下）。

## 面试速记

| 问题 | 一句话答案 |
|---|---|
| 四者定位？ | FC 协议/Tools 格式/MCP 基础设施/Skills 判断 |
| Tools vs Skills？ | 能力 vs 胜任力——动作 vs 用法 |
| Skill 失败模式？ | 顺序错误跨步复合 |
| 渐进披露？ | ~100 token 元数据 + 按需加载 |
| Skill 治理缺口？ | 跨所有编排工具——执行层兜底 |
| Arize 结论？ | 正确性差不多，MCP 贵 6x 慢 5x |
| Supabase 结论？ | MCP+Skills 100% 安全成功率 |
| 生产答案？ | use both——Skills 稳定知识 + MCP 实时系统 |
| 金句？ | Skills 教你做饭，MCP 给厨具 |
| 数据真实性？ | 两层都不认证——治理层在其下 |

---

**下一模块**：[03-实战速查：设计规范与误区](03-实战速查：设计规范与误区.md)　**返回总览**：[00-Tools & Skill 工具组件最简定义总览](00-ToolsSkill工具组件最简定义总览.md)

## 参考来源

- [Agent Skills vs MCP: Architecture and Decision Guide (2026)（Atlan）](https://atlan.com/know/ai-agent/ai-agent-skills/agent-skills-vs-mcp/)
- [Skills vs MCP tools for agents: when to use what（LlamaIndex）](https://www.llamaindex.ai/blog/skills-vs-mcp-tools-for-agents-when-to-use-what)
- [MCP vs. CLI Skills for agents: what our eval found（Arize）](https://arize.com/blog/mcp-vs-cli-skills-for-agents-what-our-eval-found-and-which-you-should-use/)
- [一文读懂 Function Call、Tools、MCP、A2A、Multi-Agent 与 Skills（腾讯云）](https://cloud.tencent.com.cn/developer/article/2692501)
- [What Are Agent Skills, and How Are They Different from Tools?（Data Science Dojo）](https://datasciencedojo.com/blog/what-are-agent-skills/)
