# Agent 核心缺陷面试总览

> 定位：「LLM AI Agent 相关理论」之「Agent 的核心缺陷」**面试必说层**——面试被问"Agent 有什么缺陷/局限/风险"时的标准答案体系：复合错误数学、幻觉、循环失控、上下文记忆缺陷、工具执行缺陷、安全缺陷、可观测性缺陷 + 应对框架。与 [幻觉、格式错误、安全、token 超限](..%2F..%2F幻觉、格式错误、安全、token%20超限%2F00-Agent四大失败模式知识体系总览.md)（失败模式深潜 11 篇）和 [Agent 调优、排错与安全模块](..%2F..%2FAgent%20调优、排错与安全模块（生产必备）%2F00-总览：Agent%20调优、排错与安全模块（生产必备）.md)（生产排错 11 篇）分工：本体系讲**缺陷的面试表述与数据弹药**。2026 一句话：**"Agent 的缺陷不是模型笨，是'把不可靠的组件串成系统'——错误会相乘，不是相加"**。

## 📚 目录

1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [缺陷地图一图速记](#3-缺陷地图一图速记)
4. [学习路线推荐](#4-学习路线推荐)
5. [2026 生态基准](#5-2026-生态基准)

## 1. 知识体系导图

```text
Agent 的核心缺陷（面试高频，必须会说）
├── 01 复合错误：缺陷的数学原理      0.95^n 相乘 / Lusser 定律 / 非确定性预算
├── 02 幻觉：自信地胡说             幻觉四类 / 循环放大 / LLM09 错误信息
├── 03 循环失控：死循环与成本黑洞     $47K 案例 / 三周采购循环 / 沉默失败
├── 04 上下文与记忆缺陷              context rot / 中途遗忘 / 记忆投毒
├── 05 工具与执行缺陷                payload 幻觉 38% / 误路由 / 执行失真
├── 06 安全缺陷：提示注入与权限滥用    OWASP Agentic Top 10 / 致命三要素
├── 07 评估与可观测性缺陷             沉默失败 / 21% 可见性 / 漂移 +0.227
├── 08 缺陷应对框架：防护与治理        校验门 / 断路器 / 确定性外壳 / Agentic SRE
└── 09 面试高频问答冲刺               20 题 + 答题范式 + 金句弹药库
```

## 2. 模块导航

| 序号 | 模块 | 核心内容 | 适合人群 |
|---|---|---|---|
| 01 | [复合错误：缺陷的数学原理](01-复合错误：缺陷的数学原理.md) | 0.95^n、Lusser 定律、非确定性预算、级联失败 | 全部（地基） |
| 02 | [幻觉：自信地胡说](02-幻觉：自信地胡说.md) | 幻觉四类、循环放大、法律后果（LLM09） | 全部（面试必背） |
| 03 | [循环失控：死循环与成本黑洞](03-循环失控：死循环与成本黑洞.md) | $47K/$47K 案例、三周采购循环、沉默失败 | 全部（面试必背） |
| 04 | [上下文与记忆缺陷](04-上下文与记忆缺陷.md) | context rot、lost-in-the-middle、记忆投毒 | 全部（面试必背） |
| 05 | [工具与执行缺陷](05-工具与执行缺陷.md) | payload 幻觉 38%、工具误路由、幻觉式恢复 | Agent 工程师 |
| 06 | [安全缺陷：提示注入与权限滥用](06-安全缺陷：提示注入与权限滥用.md) | OWASP Agentic Top 10、致命三要素、数据泄露 | 全部（面试必背） |
| 07 | [评估与可观测性缺陷](07-评估与可观测性缺陷.md) | 沉默失败、21% 可见性、漂移 +0.227、不可复现 | 全部 |
| 08 | [缺陷应对框架：防护与治理](08-缺陷应对框架：防护与治理.md) | 校验门、断路器 Agent、确定性外壳、Agentic SRE | 架构师 |
| 09 | [面试高频问答冲刺](09-面试高频问答冲刺.md) | 20 题 + 答题范式 + 金句弹药库 | 面试前 |

## 3. 缺陷地图一图速记

```text
Agent 缺陷的三层结构（面试背这条主线）：
  组件层：幻觉（LLM 本身）/ 工具不可靠（payload 幻觉）/ 记忆易污染
  系统层：复合错误（错误相乘）/ 循环失控 / 上下文退化（context rot）
  信任层：不可观测（沉默失败）/ 安全漏洞（提示注入）/ 难以评估

数学本质（一句话）：系统成功率 = 各步成功率之积
  95%/步 × 10 步 ≈ 60% 成功率——每加一步都在消耗"可靠性预算"
  2026 核心缺陷判断：问题不在模型质量，在"组合与架构"

安全本质（一句话）：Agent 带着凭据行动——模型风险 × 权限风险
  OWASP：Agent 继承 LLM 全部风险 + 新增自主性/工具/多 Agent 风险
```

## 4. 学习路线推荐

| 路线 | 路径 | 目标 |
|---|---|---|
| 面试速成（1 天） | 00 → 01 → 02/03 → 06 → 08 → 09 | 缺陷说得全、数据报得出 |
| 理论深潜（2 天） | 01-03 → 04-05 → 06-07 → 09 | 能讲数学、能讲机制 |
| 工程视角（3 天） | 全量 + [调优排错体系](..%2F..%2FAgent%20调优、排错与安全模块（生产必备）%2F00-总览：Agent%20调优、排错与安全模块（生产必备）.md) | 能落地防护 |

## 5. 2026 生态基准

> 📅 基准窗口：2026-08。版本与事实以各模块【参考来源】为准。

- **缺陷的数学共识**：多步 Agent 系统成功率 = 各步成功率之积（Lusser 定律）——95%/步 ×10 步 ≈ 60%、×20 步 ≈ 36%；98%/Agent ×10 Agent ≈ 81.7%；GPT-4o 同一零售任务跑 8 次：单次 ~65%、全部成功 <25%——可靠性像"可消耗资源"（非确定性预算）。**2026 共识：核心缺陷不在模型质量，在组合与架构**（MIT：95% 企业生成式 AI 试点失败；真实 agentic 工作流 63% 失败率）。
- **循环失控实证**：IBM 2025-10 CIO 手册把死循环与级联决策错误列为企业多 Agent 系统首要失败模式；制造采购 Agent 卡在死循环 **三周**；四 LangChain Agent 循环 11 天烧 **$47K**；89% 损坏的 API 集成无即时告警（沉默失败）。
- **幻觉升级为安全漏洞**：2026 LLM Top 10 把 Overreliance 重构为 **LLM09 Misinformation**——Air Canada 因聊天机器人错误退款政策败诉；律师引用 ChatGPT 编造的判例；攻击者注册幻觉名称的恶意包。
- **安全框架双轨**：OWASP 分设 **LLM Top 10**（推理层）与 **Top 10 for Agentic Applications 2026**（执行层，2025-12 发布）——分界线是"权限"：LLM 只出建议，Agent 带着凭据行动；Agentic Top 10 含 Goal Hijack/Tool Misuse/Identity & Privilege Abuse/Memory Poisoning 等 **ASI01-10**；**提示注入映射其中 6 类**，是"结构性未解问题"（命令与数据在同一 token 流、无可靠边界）。
- **杀伤性三要素（Simon Willison）**：Agent 同时具备"私有数据访问 + 接触不可信内容 + 对外通信"时，任何一次注入指令都能变成数据外传工具；2026 真实案例：GrafanaGhost 外传、Gemini 恶意日历邀请、能递归删文件/转 $5000 的 Forcepoint/Google 载荷。
- **可观测性赤字**：88% 企业过去 12 个月遭遇 Agent 安全事件；仅 21% 有运行时可见性；97% 安全负责人预期 12 个月内发生重大 Agent 事故；仅 6% 安全预算投给 agentic 风险。2026-02 研究：漂移系数 +0.227，失败与成功运行在 ~75% 完成前统计上不可区分——**失败只在可靠性预算耗尽后才可见**。
- **应对共识**：校验门（不传递未验证状态）、断路器 Agent（1B-3B 小模型监控死循环/停滞/"礼貌螺旋"）、确定性外壳（模型推理限制在代码驱动的阶段内，每 ~9 步重新接地，失真 -80%）、Agentic SRE（把 Agent 当非确定性微服务管，量化 SLO：成功率 <95% 告警/交接 >30s 红旗/工具调用保真度）、最小权限 + 每调用校验 + 人工确认门——**注入无法根治，目标是控制爆炸半径**。
- **与体系分工**：[幻觉、格式错误、安全、token 超限](..%2F..%2F幻觉、格式错误、安全、token%20超限%2F00-Agent四大失败模式知识体系总览.md) 讲四类失败模式的机制与治理深潜；[Agent 调优、排错与安全模块](..%2F..%2FAgent%20调优、排错与安全模块（生产必备）%2F00-总览：Agent%20调优、排错与安全模块（生产必备）.md) 讲生产排错与调优；本体系讲**面试必说的缺陷清单与数据弹药**。

---

**下一模块**：[01-复合错误：缺陷的数学原理](01-复合错误：缺陷的数学原理.md)

## 参考来源

- [The Hidden Cost of Agentic Failure（O'Reilly Radar）](https://www.oreilly.com/radar/the-hidden-cost-of-agentic-failure/)
- [Your AI Agent Is Silently Failing 80% of the Time (And You Have No Idea)（coasty.ai）](https://coasty.ai/blog/ai-agent-error-handling-recovery-2025-20260330)
- [The Non-Determinism Budget（tinyfish）](https://current.tinyfish.ai/issue/30/foundations/article/16997/the-non-determinism-budget)
- [When AI Agents Collide: Multi-Agent Orchestration Failure Playbook for 2026（NASSCOM）](https://community.nasscom.in/communities/ai/when-ai-agents-collide-multi-agent-orchestration-failure-playbook-2026)
- [OWASP Top 10 for Agentic Applications (2026)（diffray）](https://diffray.ai/blog/owasp-top-10-llm-applications/)
- [OWASP GenAI Exploit Round-up Report Q1 2026](https://genai.owasp.org/2026/04/14/owasp-genai-exploit-round-up-report-q1-2026/)
- [The Web Is Whispering to Your AI Agents — And They're Listening: Indirect Prompt Injection（lyrie.ai）](https://lyrie.ai/research/research/2026-05-04-21-deepdive-indirect-prompt-injection-wild-ipi-ai-agents-google-forcepoint-owasp-agentic)
- [Prompt Injection Remains Unsolved, OWASP Researcher Warns（Infosecurity）](https://www.infosecurity-magazine.com/news/infosec-europe-prompt-injection/)
- [From prompts to privileges: Securing the age of AI agents（TeamViewer）](https://www.teamviewer.com/cs/insights/from-prompts-to-privileges-securing-the-age-of-ai-agents/)
