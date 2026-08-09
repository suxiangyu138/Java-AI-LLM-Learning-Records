# Prompt 角色配置组件知识体系总览

> 定位：Agent 生产工程化组件之「提示词与角色配置」——Agent 的"宪法"：系统提示词设计、Persona 角色配置、PromptOps 版本化生命周期、自动优化、注入防御。2026 核心共识：**system prompt 是 Agent 的宪法（每次轮次前运行，定义身份/能力/约束/格式）**；Persona 是**评估契约不是品牌备注**；提示词按代码管理（版本化/门禁/回滚秒级）；**自我进化优化（SePO/GEPA）成为前沿**。总览做索引与速查，子主题各一篇。

## 📚 目录

1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [学习路线推荐](#3-学习路线推荐)
4. [核心概念速查](#4-核心概念速查)
5. [2026 生态基准](#5-2026-生态基准)

## 1. 知识体系导图

```text
Prompt 角色配置组件
├── 01 系统提示词全景：Agent 的宪法            六段解剖 / CARE / 约束层级
├── 02 角色设计：Persona 与身份                元素 / 人格漂移 / 角色评估
├── 03 渐进披露与紧凑设计                      100-token 元数据 / 瘦身技巧
├── 04 约束设计：硬约束与软约束                MUST-SHOULD / 冲突审计 / 升级路径
├── 05 Prompt 管理：版本化与注册中心           一等工件 / 不可变版本 / 完整上下文
├── 06 生命周期：部署-漂移-回滚                环境标记 / 按版本漂移 / 秒级回滚
├── 07 与工具-记忆-护栏-观测集成               system vs tools / 规则钉扎 / 结构隔离
├── 08 自动优化：从手动到自我进化              SePO / GEPA / 优化前置条件
├── 09 注入防御：角色即防线                    五攻击模式 / 分层防御 / 特权分离
└── 10 生产冲刺：落地清单与面试                12 避坑 / 面试题
```

## 2. 模块导航

| 序号 | 模块 | 核心内容 | 适合人群 |
|---|---|---|---|
| 01 | [系统提示词全景：Agent 的宪法](01-系统提示词全景：Agent的宪法.md) | 六段解剖、CARE、约束层级 | 全部（地基） |
| 02 | [角色设计：Persona 与身份](02-角色设计：Persona与身份.md) | 元素、人格漂移、角色评估 | Agent 工程师 |
| 03 | [渐进披露与紧凑设计](03-渐进披露与紧凑设计.md) | 元数据+Skill 文件、瘦身 | 落地开发者 |
| 04 | [约束设计：硬约束与软约束](04-约束设计：硬约束与软约束.md) | MUST/SHOULD、冲突审计 | 落地开发者 |
| 05 | [Prompt 管理：版本化与注册中心](05-Prompt管理：版本化与注册中心.md) | 一等工件、完整上下文版本 | 架构师 |
| 06 | [生命周期：部署-漂移-回滚](06-生命周期：部署-漂移-回滚.md) | 环境标记、按版本漂移、秒级回滚 | 架构师 |
| 07 | [与工具-记忆-护栏-观测集成](07-与工具-记忆-护栏-观测集成.md) | system vs tools、规则钉扎、结构隔离 | Agent 工程师 |
| 08 | [自动优化：从手动到自我进化](08-自动优化：从手动到自我进化.md) | SePO、GEPA、优化前置条件 | 前沿关注者 |
| 09 | [注入防御：角色即防线](09-注入防御：角色即防线.md) | 五攻击模式、分层防御、特权分离 | 架构师/安全 |
| 10 | [生产冲刺：落地清单与面试](10-生产冲刺：落地清单与面试.md) | 12 避坑、面试题、落地清单 | 面试/上线前 |

## 3. 学习路线推荐

| 路线 | 路径 | 目标 |
|---|---|---|
| 入门（1 天） | 01 → 02 → 04 → 10 | 能写第一个生产级系统提示词 |
| 进阶（1 周） | 01-03 → 05-06 → 07 → 10 | 能做 PromptOps 生命周期管理 |
| 高级（2 周） | 全量 + 08 → 09 | 能做自动优化与注入防御 |

## 4. 核心概念速查

| 概念 | 一句话定义 |
|---|---|
| 系统提示词 | Agent 的宪法——每次轮次前运行，定义身份/能力/约束/格式 |
| Persona | 配置的身份：角色/风格/知识边界/受众——**评估契约不是品牌备注** |
| 人格漂移 | 越界行为（品牌/幻觉/策略同时破）——>4 轮恶化，记忆压缩加剧 |
| CARE 结构 | Context/Ask/Rules/Examples 四段稳定行为模板 |
| 硬约束 vs 软约束 | MUST（安全关键）vs SHOULD（风格偏好） |
| 约束层级 | 安全 > system > user > 示例 > 默认行为 |
| 渐进披露 | ~100 token 元数据常驻 + Skill 文件按需加载（10K+ 知识） |
| Prompt as Code | 提示词按代码管理：Git 版本化/门禁/回滚 |
| 不可变版本 | 发布后不原地改——任何变更 = 新版本（带提交信息/作者/评估结果） |
| 完整执行上下文 | 版本化 prompt + 模型/参数 + 工具定义 + 检索配置（可复现） |
| 环境标记 | dev 新候选/staging 验证/prod 固定——从不浮动 latest |
| 按版本漂移 | 每版本滚动均值 rubric + trace 带 prompt.id/version/variant/template_hash |
| 秒级回滚 | 注册中心指针变更——非代码重部署（MTTR -70%） |
| SePO | 自我进化提示词 Agent——优化任务 Agent 也优化自己（+4.49 点） |
| GEPA | 遗传-Pareto 提示进化（ICLR 2026 Oral）——读执行轨迹懂"为何失败" |
| 五攻击模式 | 提取/覆盖/角色劫持/工具外泄/间接注入 |
| Prevent-Detect-Prove | 自动红队/运行时护栏/形式化验证三层防御架构 |
| Twin Agent | 特权分离：Explore 发紧凑提示给 Safe Agent——降攻击成功率 |

## 5. 2026 生态基准

> 📅 基准窗口：2026-08。版本与事实以各模块【参考来源】为准。

- **宪法式设计定型**：165 个生产 system prompt（OpenAI/Anthropic/Google/xAI 等）提炼 15 模式四层（核心架构/交互控制/工程支持/场景适配）；六段解剖（身份/能力/约束/输出格式/示例/行为规则）+ CARE 结构 + 约束层级（安全 > system > user > 示例 > 默认）——**重要内容放前面**（模型权重偏早）。
- **人格漂移成为主要失败模式**：2026 前沿模型（Claude Opus 4.7/GPT-5.x 级）比 2024-25 小模型产出**更长更自信的越界响应**；>4 轮恶化；**记忆压缩把约束"摘要掉"**——关键规则必须钉在不可压缩区；Persona 要按 span 打分、对抗性压测（愤怒用户/离题/越狱）。
- **紧凑设计规范**：渐进披露（~100 token 元数据 + Skill 文件按需加载，保留 10K+ token 知识）；<2000 token 纪律（>4000 是反例）；去历史版本标签/去重复规则/软化标记（新模型对柔和语言遵循更可靠）/动态数据走实时源。
- **PromptOps 成熟**：PELM 生命周期架构（IEEE 2026-06：设计/评估/优化/部署/监控/治理闭环）；提示词作为一等版本化工件（**版本化完整执行上下文**——prompt+模型+参数+工具+检索，否则不可复现）；环境标记 + Git 标签（production-2026-04-07）；评估门禁（回归/A-B/内置评估器）；按版本漂移检测（trace 必须带 prompt.version，template_hash 抓同版本内容漂移）；**回滚 = 注册中心指针变更秒级，MTTR -70%**。
- **自动优化进入自我进化**：SePO（2026-06）——提示词 Agent 优化任务 Agent 也优化自己，5 基准超 Manual-CoT **+4.49 点**、$37 预训练摊销；Hermes（DSPy+GEPA）：无 GPU 训练、$2-10/次、**强护栏（过全测试/语义保持/人审 PR）**；PromptEvo（StarCraft II 轨迹挖掘 +17.4%）；GEPA（ICLR 2026 Oral）反思式进化超越 RL——**优化工具只在有版本化对象时有用**（05 篇是前提）。
- **注入防御架构化**："prevent-detect-prove" 三层（自动红队/运行时护栏/形式化验证 TLA+/Dafny）；五攻击模式编目；**"提示词可被披露仍应工作——绝不嵌秘密"**；结构隔离（指令与数据不混字符串）是基础防线；Twin Agent 特权分离（Explore 发紧凑提示给 Safe Agent）2026 实证降攻击成功率。
- **与体系分工**：[02 层 Prompt 工程体系](..%2F..%2F..%2F..%2F02-大模型基础与Prompt工程%2FPrompt工程%2F00-Prompt工程知识体系总览.md) 讲提示词写作技巧；本体系讲"Agent 语境的角色配置组件"（宪法设计/Persona/PromptOps/优化/防御）；[Feedback & Evaluation](..%2FFeedback%20%26%20Evaluation%20评估模块%2F00-FeedbackEvaluation评估模块总览.md) 管评估门禁；[安全护栏 Guardrails](..%2F安全护栏%20Guardrails%2F00-安全护栏Guardrails总览.md) 管运行时拦截；[观测&可观测组件](..%2F观测%26可观测组件%2F00-观测可观测组件总览.md) 管按版本漂移检测。

---

**下一模块**：[01-系统提示词全景：Agent 的宪法](01-系统提示词全景：Agent的宪法.md)

## 参考来源

- [Design Advanced Prompting Strategies for Production AI Agents（Microsoft Learn）](https://learn.microsoft.com/zh-tw/training/modules/aaai-design-advanced-prompt-production-agents/)
- [Agent Persona: Definition, Metrics & Guide（FutureAGI）](https://futureagi.com/glossary/agent-persona/)
- [system-prompt-design.md（oakoss/agent-skills）](https://github.com/oakoss/agent-skills/blob/0283bed313563d5677a0838f4bf921b03296cf6c/skills/expert-instruction/references/system-prompt-design.md)
- [PromptOps: An End-to-End Architecture for Prompt Engineering Lifecycle Management（IEEE 2026）](https://ieeexplore.ieee.org/document/11609183)
- [How to Version & Rollback LLM Agent Prompts（Arthur AI）](https://www.arthur.ai/column/version-rollback-prompts-llm-agents)
- [Linking Prompt Management with Tracing in 2026（FutureAGI）](https://futureagi.com/blog/link-prompt-management-tracing-2026/)
- [SePO: Self-Evolving Prompt Agent for System Prompt Optimization（arXiv 2606.04465）](https://arxiv-org.ezproxy.obspm.fr/html/2606.04465v1)
- [hermes-agent-self-evolution（Nous Research）](https://github.com/NousResearch/hermes-agent-self-evolution)
- [Prompt Injection Detection: 5 Attack Patterns and the Defenses That Work (2026)（Respan）](https://www.respan.ai/articles/prompt-injection-detection)
- [Twin Agent: Context Residual Compression for Privilege Separated Agents（arXiv 2607.19595）](https://arxiv-org.ezproxy.obspm.fr/html/2607.19595v1)
