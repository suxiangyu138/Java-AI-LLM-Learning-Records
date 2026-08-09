# 护栏治理与合规：EU AI Act 与 OWASP

> 治理层 = 护栏的"董事会"：OWASP Agentic AI Top 10 是威胁基线、EU AI Act 是合规义务、企业四支柱是落地框架。2026 关键转变：**从内容过滤走向行为/意图安全**——传统护栏挡不住"意图被劫持"的攻击，治理必须覆盖身份、权限、监控、审计全链条。本章给治理与合规全景。

## 1. OWASP Agentic AI Top 10（2025-12 发布）

| 编号 | 风险 | 护栏对应 |
|---|---|---|
| AG01 | Agent Goal Hijack / Intent Breaking | 意图校验、行为监控 |
| AG02 | Tool Misuse & Exploitation | Execution rail（04 篇） |
| AG03 | Memory and Context Poisoning | 记忆写入护栏（07 篇） |
| AG04 | Identity and Privilege Abuse | 最小权限、JIT |
| AG05 | Agentic Supply Chain | 供应链校验（Tool 08 篇） |
| AG06 | Unexpected Code Execution（RCE） | 代码执行护栏 |
| AG07 | Insecure Inter-Agent Communication | 通信校验 |
| AG08 | Resource Overload | 预算护栏（Tool 06 篇） |
| AG09 | Cascading Hallucination Attacks | 输出护栏 + 引用强制 |
| AG10 | Misaligned/Deceptive Behaviors / Repudiation | 审计、可信日志 |

> 🎯 核心要点：**Top 10 里至少 6 项是 Agent 特有**（目标劫持/记忆投毒/Agent 间通信/级联幻觉）——LLM 时代的 OWASP LLM Top 10 管"单次对话"，Agentic Top 10 管"自主行动"。威胁基线升级了，护栏必须跟着升级。

## 2. 威胁案例：为什么内容护栏失守（2026 实证）

```text
邮件摘要 Agent 攻击链（Radware 场景）：
  1. 攻击者发一封"正常"邮件，内嵌隐藏间接注入
  2. Agent 读邮件 → 摘要生成（无违规内容）
  3. 隐藏指令让 Agent 执行授权动作（读更多邮件、调外部 API）
  4. Agent 修改自身记忆、外泄数据
  5. 全程无恶意提示、无策略违规、无违禁输出 —— 内容护栏 0 拦截
```

| 特征 | 说明 |
|---|---|
| 攻击看似合法 | 无单点违规——攻击在动作序列的"语义"里 |
| 意图被劫持 | Agent 的目标被替换，行为仍是"合法"工具调用 |
| 护栏盲区 | 内容检测抓不住"语法合法但语义可疑" |

> ⚠️ 2026 结论：**内容护栏 + 行为监控是互补对**——护栏拦已知（毒词/注入特征），行为监控抓未知（意图偏离/序列异常）。企业落地已出现 90+ 行为分析检测（Exabeam）与四支柱框架（Obsidian）。

## 3. 企业四支柱框架（Obsidian Security）

| 支柱 | 内容 | 案例 |
|---|---|---|
| Inventory（盘点） | 每个 Agent 的持续权威记录：OAuth 范围、MCP 连接、共享范围 | 某企业发现安全介入前已创建 2500 个 Agent |
| Identify（识别爆炸半径） | 映射真实权限（委派授权/继承凭据）；发现"毒性组合" | 孤儿 Agent + 组织级共享 + 管理员凭据 |
| Detect（检测） | 确定性运行时护栏：固定规则可预测执行 | 行为分析 + 护栏触发 |
| Enforce（执行） | 在动作点执行，不是设计时 | 动作级拦截、会话终止 |

> 🎯 核心要点：四支柱的递进逻辑——**先知道有什么（盘点），再知道危险在哪（爆炸半径），再发现异常（检测），最后在动作点拦截（执行）**。跳过盘点的护栏是盲人摸象。

## 4. 权限治理：零常设与 JIT

| 机制 | 做法 |
|---|---|
| 零常设权限（ZSP） | Agent 启动时无任何权限——权限按任务临时授予 |
| JIT 授权 | 任务级临时权限，用后自动回收 |
| 委托审计 | 委派授权与继承凭据全映射（识别"毒性组合"） |
| 角色最小化 | 只给任务所需工具/数据范围（Tool 08 篇最小权限） |
| 人工升级路径 | 护栏定义"何时必须停下问人" |

> 💡 与人类安全同构：**Agent 就是新的"员工"**——零信任框架（无默认信任 + 持续验证）直接迁移到 Agent 治理。

## 5. 合规：EU AI Act 与 NIST

| 义务 | 内容 | 时间线 |
|---|---|---|
| EU AI Act Article 50 | AI 交互透明度：告知用户在跟 AI 对话 | **2026-08-02 生效** |
| EU Digital Omnibus | 部分高风险义务推迟 | 推迟至 2027-2028，Article 50 不推迟 |
| 风险分级 | 按 EU AI Act 分级 Agent：minimal/limited/high/unacceptable | 持续 |
| NIST AI RMF | Govern/Map/Measure/Manage 四功能 + GenAI Profile（200+ 建议动作、12 风险域） | 参考框架 |
| 审计义务 | 每个 Agent 动作、Agent 间交接、system prompt 修改全部记录 | 高风险 Agent 必备 |

> ⚠️ 关键提示：**护栏是落实合规的手段，但没有任何工具自动"合规"**——合规是流程+证据+审计的组合，护栏提供的是可审计的执行痕迹。

## 6. 治理落地路线图

```text
治理落地六步：
  1. 盘点：Agent 清单（身份/OAuth/工具/数据范围）
  2. 分级：按 EU AI Act 风险分类（决定防护规格）
  3. 收缩：删除多余权限，应用 JIT/零常设
  4. 护栏：按风险级部署五 rail（低危轻量、高危全栈）
  5. 监控：行为分析 + 意图偏离检测 + 告警
  6. 审计：全量留痕 + 红队演练 + 逃逸闭环（08 篇）
```

> 🎯 核心要点：治理不是"装更多护栏"，而是**先收缩权限、再分级部署、再持续监控**——顺序错了（先装护栏再管权限）等于给敞开的门加锁。

## 7. 与相邻体系的边界

| 体系 | 分工 |
|---|---|
| 幻觉/安全 07-08 篇（OWASP LLM Top 10） | LLM Top 10 管单次对话威胁；本体系管 Agent 化威胁与护栏组件 |
| 调优排错安全 07 篇（加固清单） | 清单是"查什么"；本体系是"护栏怎么设计部署" |
| Tool 工具开发与注册 08 篇 | 工具侧安全（权限/注入）；本体系 04 篇执行护栏是其运行时拦截面 |
| Harness Engineering | Agent=Model+Harness 范式；护栏是 Harness 的安全组件之一 |

## 8. 常见误区

| 误区 | 真相 |
|---|---|
| "内容护栏够用" | 意图劫持攻击无单点违规——内容护栏 0 拦截 |
| "Top 10 是老框架翻新" | 6+ 项是 Agent 特有（目标劫持/记忆投毒/级联幻觉） |
| "权限先宽后收" | 先收缩再部署——顺序错了等于给敞门加锁 |
| "JIT 太麻烦" | 与人类零信任同构——成本换爆炸半径缩小 |
| "EU AI Act 还没生效不急" | Article 50 已 2026-08-02 生效——透明度义务现在就要做 |
| "护栏=合规" | 护栏是手段，合规是流程+证据+审计的组合 |
| "盘点一次就够" | Agent 会自发增长（2500 个案例）——盘点要持续 |

## 9. 面试速记

| 问题 | 一句话答案 |
|---|---|
| OWASP Agentic Top 10？ | 2025-12 发布，AG01-10，含目标劫持/记忆投毒等 Agent 特有风险 |
| 内容护栏为什么失守？ | 意图劫持攻击"语法合法语义可疑"——无单点违规 |
| 四支柱？ | 盘点/识别爆炸半径/检测/动作点执行 |
| 零常设权限？ | Agent 启动无权限，任务级 JIT 临时授予自动回收 |
| EU AI Act Article 50？ | AI 交互透明度，2026-08-02 生效 |
| 治理落地顺序？ | 盘点→分级→收缩→护栏→监控→审计 |
| 毒性组合是什么？ | 孤儿 Agent + 组织级共享 + 管理员凭据的组合 |
| NIST AI RMF？ | Govern/Map/Measure/Manage + GenAI Profile（200+ 动作） |
| 审计义务？ | 动作/Agent 交接/system prompt 修改全记录（高风险必备） |
| 护栏与合规关系？ | 护栏提供可审计痕迹，不自动合规 |
| 与 OWASP LLM Top 10 分工？ | LLM Top 10 管对话威胁，Agentic Top 10 管自主行动威胁 |
| 2500 个 Agent 案例说明什么？ | 盘点必须持续——Agent 会自发增长 |

---

**下一模块**：[10-生产实践与面试冲刺](10-生产实践与面试冲刺.md)　**返回总览**：[00-安全护栏 Guardrails 总览](00-安全护栏Guardrails总览.md)

## 参考来源

- [How to Mitigate Agentic AI Threats with Guardrails & Red Teaming（Alice）](https://alice.io/blog/owasp-agentic-ai-threats)
- [OWASP Agentic AI Threats & Mitigations Explained（Alice）](https://alice.io/blog/owasp-agentic-ai-threats?utm_source=Spectrum+Labs+website+-+blog%2C+homepage%2C+cta&utm_campaign=Masterclass+%7C+The+Wild+West+of+Audio+Renaissance)
- [OWASP Agentic AI Security Guide（GitHub）](https://github.com/Ajeesh25353646/OWASP-Agentic-AI-Security-Guide)
- [Why AI Guardrails Are Not Enough for Autonomous Agents（Radware）](https://www.radware.com/blog/why-ai-guardrails-are-not-enough-for-autonomous-agents/)
- [What Are Agentic Guardrails? Deterministic Controls for Probabilistic Systems（Obsidian Security）](https://www.obsidiansecurity.com/academy/what-are-agentic-guardrails-deterministic-controls-for-probabilistic-systems)
- [What's New in New-Scale July 2026: AI Agents Need More Than Guardrails（Exabeam）](https://www.exabeam.com/blog/company-news/whats-new-in-new-scale-july-2026-ai-agents-need-more-than-guardrails/)
- [The OWASP Top 10 for Agentic AI Security Explained（Alice）](https://alice.io/blog/owasp-agentic-top-ten)
