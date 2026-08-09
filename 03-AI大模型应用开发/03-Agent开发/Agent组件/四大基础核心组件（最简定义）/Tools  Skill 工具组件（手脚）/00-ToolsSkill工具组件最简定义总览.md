# Tools & Skill 工具组件最简定义总览

> 定位：四大基础核心组件之「工具（手脚）」的最简速记层——3 分钟看懂"Agent 怎么碰世界"；深化见 [Tool 工具开发与注册](..%2F..%2F..%2FAgent%20子组件专项学习%2FTool%20工具开发与注册%2F00-Tool工具开发与注册总览.md)（11 篇）与 [Function Calling](..%2F..%2F..%2F..%2F02-大模型基础与Prompt工程%2FFunction%20Calling%20函数调用【Agent%20基石】%2F00-FunctionCalling知识体系总览.md)。2026 一句话：**工具 = Agent 的手（动作能力），Skill = 手的用法（怎么做）——"MCP 给 Agent 手，Skills 给知识和可配置能力"**。

## 📚 目录

1. [速记导图](#1-速记导图)
2. [30 秒速查表](#2-30-秒速查表)
3. [学习路径](#3-学习路径)
4. [2026 关键事实](#4-2026-关键事实)

## 1. 速记导图

```text
Tools & Skill 工具组件（最简定义）
├── 01 工具组件是什么        定义 / 五类能力 / 决策-执行分离 / 与大脑分工
├── 02 Tools vs Skills vs MCP  能力-判断-基础设施 / 对比表 / 交接模式
└── 03 实战速查与误区         设计规范 / 鲁棒性 / 安全 / 评估 / 面试速记
```

## 2. 30 秒速查表

| 概念 | 一句话 |
|---|---|
| 工具（Tool） | Agent 的手——可调用的外部函数/服务（模型发请求、宿主执行） |
| 工具调用 | LLM 输出结构化请求（工具名+参数）→ 宿主校验执行 → 结果回注 |
| Skill | 手的用法——SKILL.md 指令包：何时/顺序/条件调哪些工具（含护栏） |
| MCP | 工具的"USB-C"——统一接口，任何兼容客户端连任何服务器 |
| 五类能力 | 工具（动作）/MCP（远程工具）/Apps（SaaS 集成）/Skills（判断）/Knowledge（事实） |
| 关键区分 | Tools/MCP/Apps 给新**动作**；Skills/Knowledge 塑**推理与上下文** |
| 决策-执行分离 | 模型只"建议"，宿主"执行"——安全边界在执行层 |
| 渐进披露 | Skill 元数据 ~100 token 常驻、完整指令按需加载（防 90+ schema 每轮全载） |

## 3. 学习路径

| 路径 | 目标 |
|---|---|
| 速记（10 分钟） | 本体系 01-03——概念入门 |
| 工具深化（1 天） | [Tool 工具开发与注册](..%2F..%2F..%2FAgent%20子组件专项学习%2FTool%20工具开发与注册%2F00-Tool工具开发与注册总览.md) 11 篇 |
| 协议深化（1 天） | [Function Calling](..%2F..%2F..%2F..%2F02-大模型基础与Prompt工程%2FFunction%20Calling%20函数调用【Agent%20基石】%2F00-FunctionCalling知识体系总览.md) + [MCP 知识体系](..%2F..%2F..%2F..%2F06-MCP协议与Agent%20Skill%2FMCP学习%2FMCP%2F00-MCP知识体系总览.md) |

## 4. 2026 关键事实

> 📅 基准窗口：2026-08。详见各篇【参考来源】。

- **能力分层定型**：Function Calling（基础协议）→ Tools（标准化格式）→ MCP（统一基础设施，Linux Foundation 托管）→ Skills（加判断）——**FC 是基础能力、MCP 标准化工具基础设施、Skills 编码"何时/顺序/条件"**（阿里云 2026 拆解）。
- **Skills 是 2026 新战场**：skill = recipe（比工具 schema 表达更多：顺序/分支/护栏——"未完成步骤 1-2 绝不批退款"）；**渐进披露**（~100 token 元数据常驻、完整指令按需加载）解决 token 膨胀；Supabase 评测：**MCP+Skills 组合在安全关键任务达 100% 成功率**（基线 0%、纯 MCP 会"猜"工具组合）。
- **效率实证**：Arize 评测——正确性窄带（0.826-0.834 差不多），但**MCP 贵 6x 慢 5x**（每任务 ~12 次调用 vs skills 5 次）——"正确性差不多，效率差 6 倍"。
- **设计规范**：工具 = 正式契约（清晰命名/1-3 句何时用/枚举/约束/每参数描述/additionalProperties:false）；**工具数 >30-50 选择准确率线性下降**——按组路由；原子不重叠（描述出现"或"= 两个工具）。
- **鲁棒性**：输入校验 + 输出验证 + 超时重试（幂等）+ 类型化错误返回（非裸堆栈）+ 顺序默认并行后置 + 断路器（三连败）+ 日志不可谈判。
- **评估**：ToolSelectionAccuracy/FunctionCallAccuracy/JSONValidation 三独立失败模式；BFCL v3 前沿 88-94%（GPT-5.x ~89%/Claude Opus 4.7 ~87%/Gemini 3 Pro ~85%）——**差距在 irrelevance 与 missing-tool**。
- **与体系分工**：[Tool 工具开发与注册](..%2F..%2F..%2FAgent%20子组件专项学习%2FTool%20工具开发与注册%2F00-Tool工具开发与注册总览.md) 深潜工具本体；本体系只做最简速记；[Multi-Agent](..%2F..%2F进阶工程化组件（生产环境必备）%2FMulti-Agent协作组件（多智能体）%2F00-Multi-Agent协作组件总览.md) 03 篇管 A2A 工具委派。

---

**下一模块**：[01-工具组件是什么](01-工具组件是什么.md)

## 参考来源

- [AI Agent系列：Function Calling、MCP 和 Skills 的本质差异（阿里云开发者）](https://developer.aliyun.com/article/1713530)
- [What AI Tools, MCP Servers, and Skills Actually Do（Auth0）](https://auth0.com/blog/what-ai-tools-mcp-servers-and-skills-actually-do/)
- [Agent Skills vs MCP: Architecture and Decision Guide (2026)（Atlan）](https://atlan.com/know/ai-agent/ai-agent-skills/agent-skills-vs-mcp/)
- [Skills vs MCP tools for agents: when to use what（LlamaIndex）](https://www.llamaindex.ai/blog/skills-vs-mcp-tools-for-agents-when-to-use-what)
- [MCP vs. CLI Skills for agents: what our eval found（Arize）](https://arize.com/blog/mcp-vs-cli-skills-for-agents-what-our-eval-found-and-which-you-should-use/)
- [What Is Tool Use in AI Agents（MLflow）](https://mlflow.org/articles/what-is-tool-use-in-ai-agents-a-technical-guide/)
- [Ultimate Guide: How AI Agents Use Tools — 2026（Skywork）](https://skywork.ai/blog/ai-agents-using-tools-ultimate-guide-2026/)
