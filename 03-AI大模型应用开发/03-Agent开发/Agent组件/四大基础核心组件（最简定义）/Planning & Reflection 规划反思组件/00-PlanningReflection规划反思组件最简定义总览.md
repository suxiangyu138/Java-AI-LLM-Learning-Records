# Planning & Reflection 规划反思组件最简定义总览

> 定位：四大基础核心组件之「规划与反思」的最简速记层——3 分钟看懂"想清楚再做 + 做后复盘"；深化见 [Planner 规划器](..%2F..%2F..%2FAgent%20子组件专项学习%2FPlanner%20规划器%2F00-Planner规划器总览.md) 与 [Reflection 反思模块](..%2F..%2F..%2FAgent%20子组件专项学习%2FReflection%20反思模块%2F00-Reflection反思模块总览.md)（各 11 篇）。2026 一句话：**规划 = 先想清楚再做（Plan-Execute）；反思 = 做后复盘改进（≠重试）；两者都只在"可验证"时可靠**。

## 📚 目录

1. [速记导图](#1-速记导图)
2. [30 秒速查表](#2-30-秒速查表)
3. [学习路径](#3-学习路径)
4. [2026 关键事实](#4-2026-关键事实)

## 1. 速记导图

```text
Planning & Reflection 规划反思组件（最简定义）
├── 01 规划组件是什么        ReAct vs Plan-Execute / 任务分解 / 与大脑分工
├── 02 反思组件是什么        反思 ≠ 重试 / Reflexion / oracle 前提 / 轮次控制
├── 03 实战速查与误区         规划失败模式 / 护栏 / 混合模式 / 面试速记
├── 04 规划模式深潜           模式谱系 / 计划表示 / 规划执行分离
├── 05 反思机制与轮次控制     机制谱系 / 评判器 / 轮次与成本
├── 06 生产实践与失败治理     失败防御 / 重规划 / 护栏清单
└── 07 面试冲刺               10 题 + 答题范式
```

## 2. 30 秒速查表

| 概念 | 一句话 |
|---|---|
| 规划（Planning） | 想清楚"分几步、按什么顺序、用什么工具"再动手 |
| ReAct 循环 | 边想边做（Thought→Action→Observation）——控制流未知时默认 |
| Plan-Execute | 先出完整计划再逐步执行——多步/依赖任务 |
| 任务分解 | 大目标 → 子任务 → 依赖关系（图式规划） |
| 反思（Reflection） | 做后复盘——评估输出、分析错因、改进（**≠ 重试**） |
| Reflexion | 无权重更新的语言化自我批评（HumanEval 91% vs 80% 基线） |
| Oracle 前提 | **只有可验证标准（测试/编译器）才加反思**——无 oracle 是自证偏见 |
| 轮次控制 | 反思 2-3 轮；迭代精化 3-5 轮——超限过优化 |
| 重规划 | 执行中失败 → 检测路径中断 → 修订计划 |
| Irrecoverable Drift | 主要规划失败模式：部分进展后偏离所有有效路径，难回溯 |

## 3. 学习路径

| 路径 | 目标 |
|---|---|
| 速记（10 分钟） | 本体系 01-03——概念入门 |
| 规划深化（1 天） | [Planner 规划器](..%2F..%2F..%2FAgent%20子组件专项学习%2FPlanner%20规划器%2F00-Planner规划器总览.md) 11 篇 |
| 反思深化（1 天） | [Reflection 反思模块](..%2F..%2F..%2FAgent%20子组件专项学习%2FReflection%20反思模块%2F00-Reflection反思模块总览.md) 11 篇 |
| 范式（半天） | [主流 Agent 范式 04-Plan-and-Execute](..%2F..%2F..%2F主流%20Agent%20范式%2F04-Plan-and-Execute范式.md) |

## 4. 2026 关键事实

> 📅 基准窗口：2026-08。详见各篇【参考来源】。

- **生产模式谱系**：prompt chaining → routing → parallelization → orchestrator-workers → evaluator-optimizer → 全自主 Agent——**从最简单开始，"穷尽确定性替代后才毕业到 Agent"**；混合模式是生产常态（简单任务 ReAct、复杂长任务 Plan-Execute、关键步骤后 Reflect）。
- **反思前提不变**：**"只有可验证 oracle 才加反思"**——无 oracle 的批评退化为自证偏见；Reflexion 91% vs 80% 基线证明语言化反馈有效（NeurIPS 2023 奠基结论 2026 仍适用）。
- **规划失败编目（PlanBench-XL，2026-06）**：327 任务/1665 工具——GPT-5.4 无阻塞 51.90% → 严重阻塞 **11.36%**；**静默失败最破坏性**（无错误信号的错误值 → value contamination 级联）；**Irrecoverable Drift 是主要失败模式**（GPT-4o >70% 失败）；工具选择瓶颈（过度依赖最近检索的工具而非最相关的）。
- **自适应规划**：AdaPlanBench（2026）隐藏约束逐步揭示——最好模型仅 67.75%；SPIRAL（IBM，AAAI 2026）Planner/Simulator/Critic 三 Agent 进 MCTS——DailyLifeAPIs **83.6%**（+16 点）。
- **护栏共识**：max_steps 步数预算 + 成本上限（一次无界查询可级联成数百次 LLM 调用 $5-50）+ 长任务 checkpoint + 高风险计划人工批准（与 [Orchestrator Runtime](..%2F..%2F进阶工程化组件（生产环境必备）%2FOrchestrator%20%20Runtime%20编排调度器%2F00-OrchestratorRuntime编排调度器总览.md) 预算联动）。
- **与体系分工**：[Planner 规划器](..%2F..%2F..%2FAgent%20子组件专项学习%2FPlanner%20规划器%2F00-Planner规划器总览.md) 深潜规划本体；[Reflection 反思模块](..%2F..%2F..%2FAgent%20子组件专项学习%2FReflection%20反思模块%2F00-Reflection反思模块总览.md) 深潜反思本体；本体系只做最简速记。

---

**下一模块**：[01-规划组件是什么](01-规划组件是什么.md)

## 参考来源

- [AI Agent 架构设计与实践：React、Plan-Exec、Reflect 与混合模式（腾讯云）](https://cloud.tencent.com.cn/developer/article/2655650)
- [构建生产级 AI Agent 系统的 4 大主流技术（阿里云开发者）](https://developer.aliyun.com/article/1717115)
- [Implement agent reflection and planning cycles（Microsoft Learn）](https://learn.microsoft.com/zh-cn/training/modules/aaai-design-agentic-loops-azure-ai-agent-service/4-implement-agent-reflection-planning-cycles)
- [PlanBench-XL: Evaluating Long-Horizon Planning（arXiv 2606.22388）](https://arxiv-org.ezproxy.obspm.fr/html/2606.22388v1)
- [AdaPlanBench: Evaluating Adaptive Planning（arXiv 2606.05622）](https://huggingface.co/papers/2606.05622)
- [SPIRAL: Symbolic LLM Planning via Grounded and Reflective Search（IBM Research，AAAI 2026）](https://research.ibm.com/publications/spiral-symbolic-llm-planning-via-grounded-and-reflective-search)
- [tepa-ai: Planner → Executor → Evaluator loop（GitHub）](https://github.com/frandi/tepa-ai)
