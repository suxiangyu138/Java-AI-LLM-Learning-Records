# LLM 大脑最简定义总览

> 定位：四大基础核心组件之「LLM 大脑」的最简速记层——3 分钟看懂"推理决策模块"是什么；深化见 [Agent 四大核心组件 02 篇](..%2F..%2F..%2FAgent%20四大核心组件%2F02-LLM大脑：推理决策核心.md) 与 [主流 Agent 范式](..%2F..%2F..%2F主流%20Agent%20范式%2F00-主流Agent范式知识体系总览.md)。2026 一句话：**LLM 大脑 = 推理模型（thinking tokens）+ 决策循环（Thought-Action-Observation）**。

## 📚 目录

1. [速记导图](#1-速记导图)
2. [30 秒速查表](#2-30-秒速查表)
3. [学习路径](#3-学习路径)
4. [2026 关键事实](#4-2026-关键事实)

## 1. 速记导图

```text
LLM 大脑（推理决策模块）
├── 01 LLM大脑是什么        定位 / 与 Agent 关系 / 五组件位置
├── 02 推理与决策机制        ReAct 循环 / 推理模型 / thinking tokens
├── 03 实战速查与误区         选型 / 成本旋钮 / 三误区 / 面试速记
├── 04 推理模型家族与选型     家族谱系 / 选型决策 / 成本模型
├── 05 决策机制与工具调用     决策三形态 / tool_calls / 决策四维
├── 06 生产实践与调优         路由 / 思考旋钮 / 监控指标 / 调优清单
└── 07 面试冲刺               10 题 + 答题范式
```

## 2. 30 秒速查表

| 概念 | 一句话 |
|---|---|
| LLM 大脑 | Agent 的推理决策核心——"想什么、怎么想、做什么决定" |
| 决策循环 | Thought（想）→ Action（做）→ Observation（看）→ 循环（ReAct 模式） |
| Thinking tokens | 推理模型的内部思考（隐藏但计费）——推理质量的新计算轴 |
| 推理模型 | 先内部思考再回答的模型（o 系列/Claude thinking/DeepSeek-R1） |
| 推理 vs 普通 | 多步可验证任务用推理模型；查找/摘要用快速模型（路由） |
| 大脑 vs 循环 | 推理模型提高"每步决策质量"，不消除 Agent 循环 |
| 成本旋钮 | thinking 是"旋钮"（budget_tokens/reasoning_effort）——按任务难度调 |

## 3. 学习路径

| 路径 | 目标 |
|---|---|
| 速记（10 分钟） | 本体系 01-03——概念入门 |
| 深化（1 天） | [Agent 四大核心组件 02 篇](..%2F..%2F..%2FAgent%20四大核心组件%2F02-LLM大脑：推理决策核心.md)——完整解剖 |
| 范式（半天） | [主流 Agent 范式 03-ReAct](..%2F..%2F..%2F主流%20Agent%20范式%2F03-ReAct范式.md)——循环深潜 |

## 4. 2026 关键事实

> 📅 基准窗口：2026-08。详见各篇【参考来源】。

- **Thinking tokens 是新计算轴**：推理模型先内部思考再回答（5-60 秒）；**收益递减**（no→medium 陡、high→max 平，Snell 计算最优）——按任务难度路由是成本控制正解。
- **推理模型 = Agent 的规划大脑**：内部计算先行探索/回溯再输出计划——"GPS 后台算完全程再给一条指令"；但 **thinking 不传给 Agent**（服务端，不可日志调试）。
- **推理模型不消除循环**：只提高每步质量——多步任务仍要 Thought-Action-Observation 循环（ReAct 模式仍是现代 Agent 的工作方式）。
- **阵容 2026**：OpenAI o 系列/GPT-5 thinking（实时路由，省 50-80% 输出 token）、Claude budget_tokens、Gemini thinkingBudget、DeepSeek-R1（开源）、Trinity-Large-Thinking（398B MoE，PinchBench 91.9% 排 #2，比 Opus-4.6 便宜 96%）、MAI-Thinking-1（**无内置持久记忆——生产 Agent 要外部记忆层**）。
- **控制是前沿**：ACTS 把推理转向建模为 MDP（控制器观 reasoning trace 发转向指令）；SR²AM 三系统（反应/模拟推理/自调节——何时规划）。

---

**下一模块**：[01-LLM 大脑是什么](01-LLM大脑是什么.md)

## 参考来源

- [AI Reasoning Models Explained: Test-Time Compute (2026)（Taskade）](https://www.taskade.com/blog/reasoning-models)
- [Reasoning model as agent planner（The Neural Base）](https://theneuralbase.com/reasoning-models/learn/intermediate/reasoning-model-as-agent-planner/)
- [Arcee AI Releases Trinity-Large-Thinking（NYU Shanghai）](http://rits.shanghai.nyu.edu/ai/arcee-ai-releases-trinity-large-thinking-a-400b-open-reasoning-agent/)
- [MAI-Thinking-1 + Mem0: Add Long-Term Memory（Mem0）](https://mem0.ai/blog/how-mai-thinking-1-works)
- [Introducing Nemotron 3 Super（NVIDIA）](https://developer.nvidia.com/blog/introducing-nemotron-3-super-an-open-hybrid-mamba-transformer-moe-for-agentic-reasoning/)
- [SR²AM: Efficient Agentic Reasoning Through Self-Regulated Simulative Planning（GitHub）](https://github.com/sailing-lab/sr2am)
- [Agentic Chain-of-Thought Steering for Efficient and Controllable LLM Reasoning（HuggingFace）](https://huggingface.co/papers/2606.03965)
