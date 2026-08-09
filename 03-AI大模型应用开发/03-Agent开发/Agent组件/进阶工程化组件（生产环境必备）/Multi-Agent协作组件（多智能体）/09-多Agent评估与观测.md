# 多 Agent 评估与观测

> 多 Agent 评估 = 回答"协作整体好不好"——单步评估在级联面前失明（08 篇 12% 传播错误），**轨迹级评估是检测信号**；观测层面需要**跨框架分布式追踪**（W3C Trace Context）。2026 基准：ACIArena（1356 用例）与 Agentic SLO 成为标准。本章给多 Agent 评估与观测完整设计。

## 1. 为什么单步评估在多 Agent 失明

| 单步评估的盲区 | 多 Agent 现实 |
|---|---|
| 每步各自打分 | 级联错误每步"合理"（08 篇典型链） |
| 无跨 Agent 依赖 | 错误经信任交接传播（12%） |
| 无拓扑维度 | 路由错→下游全错 |
| 无交互模式 | 争论循环/礼貌螺旋不可见 |

> 🎯 核心要点：**检测信号 = 轨迹通过率与单步通过率的缺口**——轨迹级评分（TrajectoryScore/GoalProgress/StepEfficiency）暴露传播模式；"单步 95%、轨迹 60%"的缺口就是级联证据。

## 2. ACIArena：级联注入统一评估（ACL 2026）

| 项 | 说明 |
|---|---|
| 定位 | Agent Cascading Injection 鲁棒性统一评估框架 |
| 攻击面 | 外部输入/Agent 画像/Agent 间消息 |
| 目标 | 指令劫持/任务破坏/信息外泄 |
| 规模 | 六种 MAS 实现 + **1356 测试用例** |

| 关键发现 | 结论 |
|---|---|
| 拓扑评估不足 | 仅靠拓扑（结构）评估鲁棒性不够 |
| 角色设计 | 鲁棒 MAS 需刻意角色设计与受控交互模式 |
| 防御不可迁移 | 简化环境开发的防御常不迁移到真实环境 |
| 窄防御反噬 | 狭窄范围的防御可能引入新漏洞 |

> ⚠️ ACIArena 的教训：**"简化环境测的防御到生产就失灵"**——多 Agent 评估必须在真实交互形态下测；防御设计要防"补一个洞开一个窗"。

## 3. Agentic SLO：多 Agent 的量化契约

| SLO | 阈值 | 说明 |
|---|---|---|
| 成功率 | 含人工审计 | 任务级成功率（非单步） |
| 交接延迟 | **30 秒** | 每次交接耗时上限 |
| 工具调用保真 | 高 | 工具调用正确性（评估 03 篇联动） |
| 级联率 | 趋 0 | 传播错误占比（§1 缺口信号） |

> 🎯 核心要点：**SLO 是"量化到可告警"**——交接延迟 30 秒、成功率含人工审计、工具保真；SLO 进入告警体系（观测 09 篇质量告警联动），不是纸面承诺。

## 4. 分布式追踪：跨框架可观测

| 项 | 说明 |
|---|---|
| 协议 | **W3C Trace Context（traceparent 头）** 跨 Agent 传播 |
| 场景 | LangGraph → CrewAI → PydanticAI 跨框架调试 |
| 存储 | OTel 兼容 collector（观测 03 篇） |
| 关联 | conversation.id + traceparent 双键（观测 02 篇） |

> 💡 跨框架追踪的工程现实：**每个框架的 span 语义不同（观测 03 篇 OpenInference 分类）**——统一 collector 摄入 + traceparent 传播，才能从"每框架各自看"变成"一条轨迹看全链"。

## 5. 断路器监控：循环的语言签名

| 模式 | 语言签名 | 监控 |
|---|---|---|
| Agent tennis | 分歧来回无进展 | 轻量模型持续看 |
| 停滞 | 重复相同输出 | 重复 span 检测（观测 02 篇） |
| 礼貌螺旋 | 过度客气互让 | 语言模式 |
| 九日循环 | 自引用无终止 | 轮次上限 + 断路器（02 篇） |

> 🎯 核心要点：**断路器 = 低成本小模型实时盯循环**——大模型做任务，小模型做"哨兵"（语言签名检测）；这是 08 篇"运行时可干预"的观测侧配套（检测 + 干预双环）。

## 6. 多 Agent 评估工程规范

| 环节 | 做法 |
|---|---|
| 轨迹采样 | 全轨迹保留（级联只在轨迹可见）——tail-based（观测 08 篇） |
| 级联检测 | 轨迹 vs 单步通过率缺口监控 |
| 场景设计 | 注入传播/交接误判/竞争场景（ACIArena 方法论） |
| 角色评估 | 每个 Agent 角色职责完成度（不只整体） |
| 审计重建 | 事故从统一 trace 平面重建（五缺陷之审计碎片的对策） |

> 💡 与评估体系的分工：评估体系（Feedback & Evaluation）管单 Agent 评估工程；本体系 09 管多 Agent 特有的"级联检测 + 跨 Agent 归因"——两者是单点与系统的关系。

## 7. 常见误区

| 误区 | 真相 |
|---|---|
| "单步 eval 管多 Agent" | 12% 传播错误单步不标记——轨迹级 |
| "拓扑评估够" | ACIArena：拓扑不足——角色与交互模式 |
| "简化环境测防御" | 防御不迁移——真实交互形态下测 |
| "SLO 是纸面" | 进告警——30 秒交接阈值可监控 |
| "每框架各看各的" | traceparent 统一轨迹——跨框架看全链 |
| "循环是大模型的事" | 小模型哨兵盯语言签名——断路器 |
| "整体通过就放心" | 角色级评估——每 Agent 职责完成度 |

## 8. 面试速记

| 问题 | 一句话答案 |
|---|---|
| 级联检测信号？ | 轨迹通过率 vs 单步通过率缺口 |
| ACIArena？ | ACL 2026 级联注入统一评估——1356 用例 |
| ACIArena 发现？ | 拓扑评估不足——需角色设计与受控交互 |
| Agentic SLO？ | 成功率（人工审计）+ 交接延迟 30s + 工具保真 |
| 跨框架追踪？ | W3C traceparent 传播 + OTel collector |
| 断路器？ | 轻量模型盯 agent tennis/停滞/礼貌螺旋 |
| 九日循环防御？ | 轮次上限 + 断路器 |
| 轨迹采样？ | 全轨迹保留（tail-based）——级联只在轨迹可见 |
| 角色评估？ | 每 Agent 职责完成度——不只整体 |
| 审计重建？ | 统一 trace 平面——防审计碎片 |
| 与评估体系分工？ | 评估体系管单 Agent，本体系管级联与归因 |
| 与 08 篇配套？ | 检测（本体系）+ 干预（08 运行时可干预） |

---

**下一模块**：[10-生产冲刺：落地清单与面试](10-生产冲刺：落地清单与面试.md)　**返回总览**：[00-Multi-Agent 协作组件总览](00-Multi-Agent协作组件总览.md)

## 参考来源

- [ACIArena: Toward Unified Evaluation for Agent Cascading Injection（ACL 2026）](https://aclanthology.org/2026.acl-long.457/)
- [The Internet of Agentic AI: Communication, Coordination, and Collective Intelligence at Scale（arXiv 2606.12835）](https://arxiv-org.ezproxy.obspm.fr/html/2606.12835v1)
- [When AI Agents Collide: Orchestration Failure Playbook（NASSCOM）](https://community.nasscom.in/communities/ai/when-ai-agents-collide-multi-agent-orchestration-failure-playbook-2026)
- [5 Architectural Flaws in Agentic AI SOC Platforms（D3 Security）](https://d3security.com/resources/5-architectural-flaws-agentic-ai-soc/)
