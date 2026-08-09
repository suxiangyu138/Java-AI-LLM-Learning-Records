# 函数调用 vs 工具 vs MCP vs Skills

> 辨析篇：工具生态四层概念——**叠加非替代**。深潜见 [Tools Skill 组件 02 篇](..%2F..%2FAgent组件%2F四大基础核心组件（最简定义）%2FTools%20%20Skill%20工具组件（手脚）%2F02-Tools-vs-Skills-vs-MCP.md)。

## 1. 四层定位

| 概念 | 层 | 解决什么 | 类比 |
|---|---|---|---|
| 函数调用（FC） | 协议 | 让模型"知道有哪些函数、能请求调用" | 拨号协议 |
| 工具（Tool） | 格式 | 统一工具形态（name+描述+schema） | 标准接口 |
| MCP | 基础设施 | 任何客户端连任何服务器 | **USB-C for AI** |
| Skills | 判断 | 何时/顺序/条件调哪些工具（含护栏） | 菜谱 |

> 🎯 核心要点：**2026 拆解——"FC 是基础能力、MCP 标准化工具基础设施、Skills 编码判断"**（阿里云）——四层是叠加关系，不是替代关系；面试别答"选哪个"，答"各管哪层"。

## 2. 两两辨析（面试混淆点）

| 概念对 | 判据 | 例子 |
|---|---|---|
| FC vs 工具 | FC 是"机制"（怎么调），工具是"对象"（调什么） | FC=协议；工具=get_weather 定义 |
| 工具 vs MCP | 工具是格式，MCP 是分发/发现 | 工具定义写一次，MCP 处处可用 |
| MCP vs Skills | MCP 给动作（能碰什么系统），Skills 给判断（怎么用） | "MCP 给手，Skills 给知识" |
| 工具 vs Skills | 单次动作 vs 顺序编排（跨步骤状态） | 工具=API 调用；Skill=退款流程 |

## 3. 关键区分：能力 vs 胜任力

| 维度 | 工具（Tool） | Skill |
|---|---|---|
| 扩展什么 | 能力（外部动作） | 胜任力（内部判断） |
| 判断在哪 | Agent 每轮推理 | SKILL.md 预写排序 |
| 状态 | 无状态 | 跨步骤跟踪 |
| 失败模式 | 单次调用错 | 顺序错跨步复合 |
| Token | 全 schema 每轮载入 | ~100 token 元数据 + 按需加载 |
| 治理 | 单调用权限 | 跨所有编排工具（执行层兜底） |

> 💡 金句：**"Tools/MCP/Apps 给新动作，Skills/Knowledge 塑推理与上下文"**——动作扩展能力，判断扩展胜任力（Auth0 2026）。

## 4. 五类能力全景（上下文）

| 能力 | 类型 | 说明 |
|---|---|---|
| 工具（Tool） | 动作 | 可调用函数（搜索/文件/API/代码执行） |
| MCP 服务器 | 动作 | 远程工具（统一协议） |
| Apps | 动作 | SaaS 集成（Gmail/Slack/Jira） |
| Skills | 判断 | SKILL.md——"如何思考" |
| Knowledge | 事实 | RAG 检索 |

> ⚠️ 2026 实证提醒：**生产 use both**——MCP+Skills 组合在安全关键任务 100% 成功率（基线 0%、纯 MCP 会"猜"工具组合）；正确性窄带但 MCP 贵 6x 慢 5x（Arize）——"正确性差不多，效率差 6 倍"。

## 5. 面试速记

| 问题 | 一句话答案 |
|---|---|
| 四层关系？ | 叠加非替代——FC 机制/工具对象/MCP 分发/Skills 判断 |
| FC vs 工具？ | 怎么调 vs 调什么 |
| MCP vs Skills？ | 给动作 vs 给判断（手 vs 知识） |
| 工具 vs Skill？ | 单次动作 vs 顺序编排 |
| 能力 vs 胜任力？ | 动作扩展能力，判断扩展胜任力 |
| Skill 失败模式？ | 顺序错跨步复合 |
| 渐进披露？ | 100 token 元数据 + 按需加载 |
| use both？ | MCP+Skills——Supabase 100% 安全成功率 |
| Arize 结论？ | 正确性差不多，MCP 贵 6x 慢 5x |
| 五类能力？ | 工具/MCP/Apps（动作）+ Skills/Knowledge（判断事实） |

---

**下一模块**：[03-ReAct vs Plan-and-Execute vs 反思](03-ReAct-vs-Plan-and-Execute-vs-反思.md)　**返回总览**：[00-区分概念总览](00-区分概念总览.md)

## 参考来源

- [AI Agent系列：Function Calling、MCP 和 Skills 的本质差异（阿里云开发者）](https://developer.aliyun.com/article/1713530)
- [What AI Tools, MCP Servers, and Skills Actually Do（Auth0）](https://auth0.com/blog/what-ai-tools-mcp-servers-and-skills-actually-do/)
- [Agent Skills vs MCP (2026)（Atlan）](https://atlan.com/know/ai-agent/ai-agent-skills/agent-skills-vs-mcp/)
- [一文读懂 Function Call、Tools、MCP、A2A、Multi-Agent 与 Skills（腾讯云）](https://cloud.tencent.com.cn/developer/article/2692501)
