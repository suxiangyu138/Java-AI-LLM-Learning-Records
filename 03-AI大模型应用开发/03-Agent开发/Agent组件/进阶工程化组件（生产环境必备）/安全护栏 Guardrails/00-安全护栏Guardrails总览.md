# 安全护栏 Guardrails 知识体系总览

> 定位：Agent 生产工程化组件之「安全护栏」——在输入、输出、检索、对话、执行五个拦截点上做运行时策略控制的组件。2026 核心共识：**护栏是确定性控件，用来约束概率系统**；传统内容过滤挡不住 Agent 化攻击（间接注入检测率仅 7-37%），护栏必须与行为监控、最小权限、审计组成纵深防御。总览做索引与速查，子主题各一篇。

## 📚 目录

1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [学习路线推荐](#3-学习路线推荐)
4. [核心概念速查](#4-核心概念速查)
5. [2026 生态基准](#5-2026-生态基准)

## 1. 知识体系导图

```text
安全护栏 Guardrails
├── 01 护栏全景：安全组件的定位与边界        输入vs输出 / 五 rail / 护栏≠安全全部
├── 02 输入护栏：预-LLM 防线                 PII 脱敏 / 注入越狱检测 / 延迟预算
├── 03 输出护栏：后-LLM 校验                 幻觉检测 / 内容安全 / refrain-fix-reask
├── 04 工具与执行护栏：动作层防线             tool call 校验 / ToolSafe / 参数验证
├── 05 护栏分类器选型：从通用到专用            Llama Guard 3 / ShieldGemma / 激活探针
├── 06 护栏框架与实现：NeMo 与 Guardrails AI  Colang / RAIL / 两框架分工
├── 07 护栏编排与 Agent 集成                 五 rail 编排 / tripwire / 护栏即反馈
├── 08 护栏评测：从基准到生产                 LODO / 绕过率-FPR-延迟 / 阈值校准
├── 09 护栏治理与合规：EU AI Act 与 OWASP     OWASP Agentic Top 10 / 四支柱 / JIT 权限
└── 10 生产实践与面试冲刺                     12 避坑 / 面试题
```

## 2. 模块导航

| 序号 | 模块 | 核心内容 | 适合人群 |
|---|---|---|---|
| 01 | [护栏全景：安全组件的定位与边界](01-护栏全景：安全组件的定位与边界.md) | 拦截点、五 rail、护栏 vs 威胁框架 | 全部（地基） |
| 02 | [输入护栏：预-LLM 防线](02-输入护栏：预-LLM防线.md) | PII 脱敏、注入检测、快速确定性 | 落地开发者 |
| 03 | [输出护栏：后-LLM 校验](03-输出护栏：后-LLM校验.md) | 幻觉检测、内容安全、三种修复 | 落地开发者 |
| 04 | [工具与执行护栏：动作层防线](04-工具与执行护栏：动作层防线.md) | tool call 校验、ToolSafe、执行拦截 | Agent 工程师 |
| 05 | [护栏分类器选型：从通用到专用](05-护栏分类器选型：从通用到专用.md) | 分类器对比、2026 盲区实证、激活探针 | 架构师 |
| 06 | [护栏框架与实现：NeMo 与 Guardrails AI](06-护栏框架与实现：NeMo与Guardrails-AI.md) | Colang、RAIL/Pydantic、分层组合 | Java/全栈 |
| 07 | [护栏编排与 Agent 集成](07-护栏编排与Agent集成.md) | 五 rail 编排、tripwire、护栏即反馈 | Agent 工程师 |
| 08 | [护栏评测：从基准到生产](08-护栏评测：从基准到生产.md) | LODO、指标三件套、阈值校准 | 评测关注者 |
| 09 | [护栏治理与合规：EU AI Act 与 OWASP](09-护栏治理与合规：EU-AI-Act与OWASP.md) | AG01-10、EU AI Act、企业四支柱 | 架构师/合规 |
| 10 | [生产实践与面试冲刺](10-生产实践与面试冲刺.md) | 12 避坑、落地清单、面试题 | 面试/上线前 |

## 3. 学习路线推荐

| 路线 | 路径 | 目标 |
|---|---|---|
| 入门（1 天） | 01 → 02 → 03 → 10 | 能搭最简输入输出护栏 |
| 进阶（1 周） | 01-04 → 05 → 06-07 → 10 | 能选型分类器、集成护栏框架 |
| 高级（2 周） | 全量 + 08 → 09 | 能评测护栏效果、对齐合规 |

## 4. 核心概念速查

| 概念 | 一句话定义 |
|---|---|
| 护栏（Guardrail） | 运行时策略控制：拦截不安全/不合规行为到达用户或下游系统的组件 |
| 输入护栏（Pre-LLM） | 模型前拦截：PII 脱敏、注入/越狱检测（热路径，须快而确定） |
| 输出护栏（Post-LLM） | 模型后校验：幻觉、毒性、格式、输出 PII（成本刻意） |
| 五 rail 分类 | input/dialog/retrieval/execution/output（NeMo Guardrails 提出） |
| Refrain / Fix / Reask | 校验失败三种补救：拒绝 / 自动修正 / 重问模型 |
| 护栏即反馈 | 检测到问题 → 反馈给模型修正 → 重检——把护栏变质量保障而非纯拦截 |
| Tripwire | 触发即中断执行的控制机制（OpenAI Agents SDK 术语） |
| Llama Guard 3 | Meta 开源多标签安全分类器（1B/8B/11B，8 语言，MLCommons 分类法） |
| ShieldGemma | Google 概率打分的分类器（按类别设阈值） |
| Aegis Guard | 上下文感知分类法（区分绝对不安全与上下文相关） |
| LODO 评估 | Leave-One-Dataset-Out：留出整数据集评估，防基准虚高（标准评估高估 8.4 点 AUC） |
| 间接注入盲区 | 2026 实证：Prompt Guard 2/Llama Guard 3 对间接注入与工具攻击检测率仅 7-37% |
| 激活探针 | 在 LLM 残差流隐藏态上训练线性探针，检测间接注入最高 99% |
| OWASP Agentic Top 10 | 2025-12 发布的 Agent 化威胁框架（AG01-AG10） |
| 行为安全 | 超越内容过滤：意图建模 + 运行时监控 + 意图偏离检测（2026 企业模型） |
| JIT 权限 | 零常设权限，任务级临时授权自动回收（同人类最小权限） |
| EU AI Act Article 50 | 2026-08-02 生效的 AI 交互透明度义务（告知用户在跟 AI 对话） |

## 5. 2026 生态基准

> 📅 基准窗口：2026-08。版本与事实以各模块【参考来源】为准。

- **护栏定位反转**：护栏是"确定性控件约束概率系统"（Obsidian 定义）；但 **2026 企业共识：传统护栏对自主 Agent 根本不够**——护栏为"提示"构建，Agent 靠"意图"操作，攻击发生在提示与提示之间（自提示、链式工具、记忆篡改、Agent 间交互）。内容过滤必须让位于行为/意图安全（09 篇）。
- **间接注入盲区实证**：Meta Llama Prompt Guard 2（86M）与 Llama Guard 3（8B）对间接注入与 Agent 工具攻击检测率仅 **7-37%**（聊天模板无 tool 角色支持、扁平文本丢失结构边界）；激活探针（残差流隐藏态线性探针）达 **99%**（05 篇）。
- **分类器谱系成熟**：Llama Guard 3（多标签 8 语言）/ ShieldGemma（概率分档）/ Aegis Guard（上下文感知）替代通用毒性过滤器（Perspective API 挡不住礼貌式越狱）；Purpose-built 分类器是 2026 标配。
- **框架双雄互补**：Guardrails AI v0.10.0（2026-04-03，Hub 70+ 验证器，RAIL/Pydantic，refrain/fix/reask）管"输出形状与安全"；NeMo Guardrails v0.21.0（2026-03-12，Colang 五 rail）管"对话流与话题边界"——分层组合是推荐架构。
- **评测方法论升级**：LODO 评估证明标准评估高估 8.4 点 AUC（单数据集差距 1-25%，28% SAE 特征是与数据集相关的捷径）；生产指标三件套 = 绕过率 + FPR + 延迟；教育导师基准（480 查询）：多层级流水线 46.34% 绕过/0% FPR/2.5ms vs NeMo 0% 绕过/16.22% FPR/1.3-1.5s——**没有免费的午餐，安全与误报延迟是三角权衡**。
- **工具护栏成为独立战场**：ToolSafe（ACL 2026）步骤级工具调用安全，有害工具调用减少 65%、良性任务完成提升 ~10%；OWASP Agentic Top 10 中 Tool Misuse 与 RCE 位列高危（04 篇）。
- **合规驱动落地**：EU AI Act Article 50 于 **2026-08-02 生效**（AI 交互透明度）；CSA 报告 65% 组织已遭遇 AI Agent 安全事件；OWASP Agentic AI Top 10（2025-12）成为威胁基线框架（09 篇）。
- **与体系分工**：[幻觉、格式错误、安全、token 超限 07-08 篇](..%2F..%2F..%2F幻觉、格式错误、安全、token%20超限%2F07-Agent安全：提示注入与OWASP%20Top%2010.md) 讲威胁框架（注入技术与 OWASP LLM Top 10）；[调优排错安全模块 07 篇](..%2F..%2F..%2FAgent%20调优、排错与安全模块（生产必备）%2F07-安全加固清单.md) 讲加固清单；[Tool 工具开发与注册 08 篇](..%2F..%2F..%2FAgent%20子组件专项学习%2FTool%20工具开发与注册%2F08-工具安全：权限-注入与治理.md) 讲工具侧安全；本体系深潜"护栏组件本体"（分类/实现/编排/评测/合规）。

---

**下一模块**：[01-护栏全景：安全组件的定位与边界](01-护栏全景：安全组件的定位与边界.md)

## 参考来源

- [AI Guardrails Explained: Safe, On-Policy Agents (2026)（Taskade）](https://www.taskade.com/blog/ai-guardrails)
- [LLM Guardrails (2026): Failure Taxonomy, Libraries Compared（MorphLLM）](https://www.morphllm.com/llm-guardrails)
- [AI Agent Guardrails: Pre-LLM & Post-LLM Best Practices（Arthur AI）](https://www.arthur.ai/blog/best-practices-for-building-agents-guardrails)
- [Guardrails AI vs NeMo Guardrails (2026)（Respan）](https://www.respan.ai/market-map/compare/guardrails-ai-vs-nemo-guardrails)
- [What Are Agentic Guardrails? Deterministic Controls for Probabilistic Systems（Obsidian Security）](https://www.obsidiansecurity.com/academy/what-are-agentic-guardrails-deterministic-controls-for-probabilistic-systems)
- [How to Mitigate Agentic AI Threats with Guardrails & Red Teaming（Alice）](https://alice.io/blog/owasp-agentic-ai-threats)
- [When Benchmarks Lie: Evaluating Malicious Prompt Classifiers Under True Distribution Shift（ICLR 2026）](https://iclr.cc/virtual/2026/10016274)
- [ToolSafe: Enhancing Tool Invocation Safety via Proactive Step-level Guardrail（ACL 2026 Findings）](https://aclanthology.org/2026.findings-acl.1850/)
