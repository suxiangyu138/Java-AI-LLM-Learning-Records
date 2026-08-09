# 08 Microsoft Agent Framework：迁移与未来

> AutoGen 的下一站：与 Semantic Kernel 合并为 MAF（2026-04 GA）——图工作流取代自由对话、@tool 取代 FunctionTool、checkpoint 一等公民；迁移对照表 + 学习建议是本篇核心。

## 📚 目录

1. [MAF 是什么](#1-maf-是什么)
2. [MAF 核心特性](#2-maf-核心特性)
3. [为什么合并：分工与整合](#3-为什么合并分工与整合)
4. [迁移对照表](#4-迁移对照表)
5. [迁移实战要点](#5-迁移实战要点)
6. [学习建议与面试](#6-学习建议与面试)
7. [MAF 常见误区](#7-maf-常见误区)
8. [MAF 生态与未来方向](#8-maf-生态与未来方向)

## 1. MAF 是什么

| 维度 | 说明 |
|---|---|
| 全称 | Microsoft Agent Framework |
| 构成 | AutoGen（编排）+ Semantic Kernel（企业集成）合并 |
| 发布时间 | 2025-10 公开预览 → **2026-04-03 GA** |
| 许可 | MIT |
| 前身规模 | 合并前两项目累计 75,000+ stars |
| 定位 | 微软官方推荐的 Agent 开发框架 |

一句话定义：

> MAF = AutoGen 的多智能体编排能力 + Semantic Kernel 的企业级集成能力，用**类型化有向图工作流**统一编排，面向生产级 Agent 应用。

## 2. MAF 核心特性

| 特性 | 说明 |
|---|---|
| 图工作流 | SequentialBuilder 等类型化有向图，显式建模顺序/并行/层级/并发 |
| DevUI 调试器 | 浏览器可视化：Agent 执行/消息流/工具调用 |
| 负责任 AI | 任务守卫、提示注入防护（spotlighting）、PII 检测 |
| 持久化 | Cosmos DB + Application Insights 开箱即用 |
| 人机审批 | 一等公民（内置审批工作流） |
| 6 家模型提供商 | Azure OpenAI/OpenAI/Anthropic/Bedrock/Gemini/Ollama 一行切换 |
| 原生协议 | MCP + A2A 支持 |
| 检查点 | 工作流状态保存/恢复（一等公民） |

### 与 AutoGen 的本质差异

```
AutoGen：自由对话编排（灵活、不可预测）
MAF：图工作流编排（显式、可预测、生产化）

MAF 的图工作流表明微软的选择：
生产环境要"可预测的流程"，不要"自由对话"
```

## 3. 为什么合并：分工与整合

### 两者的互补

| 框架 | 擅长 | 短板 |
|---|---|---|
| AutoGen | 多智能体编排、对话协作 | 企业集成弱（Azure/365/数据源） |
| Semantic Kernel | 企业集成、Azure AI 服务 | 多 Agent 编排弱 |

```
合并逻辑：
编排能力（AutoGen）+ 企业集成能力（SK）= 完整的企业 Agent 框架
一个框架搞定：编排 + 集成 + 可观测 + 负责任 AI
```

### 对生态的影响

| 影响 | 说明 |
|---|---|
| AutoGen | 维护模式（bug 修复，无新功能） |
| Semantic Kernel | 并入 MAF（同源演进） |
| 新项目 | 官方推荐直接用 MAF |
| 存量 AutoGen | 官方迁移指南，或继续维护 |

## 4. 迁移对照表

| 方面 | AutoGen | MAF |
|---|---|---|
| 编排模型 | 事件驱动 Team 容器 | 类型化有向图 Workflow（SequentialBuilder 等） |
| Agent 默认行为 | 单轮（手动 max_tool_iterations） | **默认多轮自动调用工具直至完成**（最大坑） |
| 工具定义 | FunctionTool 包装类 | **@tool 装饰器**（自动推断 JSON schema，支持类型提示） |
| 检查点 | 无原生 checkpoint/resume | 一等公民（保存/恢复工作流） |
| GroupChat | GroupChat + Manager | **MagenticOneGroupChat** 等替代 |
| 模型配置 | OpenAIChatCompletionClient | 统一模型客户端（6 家一行切换） |
| 调试 | Studio | DevUI 浏览器调试器 |

### 迁移中的三个坑（必看）

| 坑 | 说明 | 对策 |
|---|---|---|
| 默认行为差异 | AutoGen 单轮 → MAF 默认多轮 | 迁移后测试工具循环行为，必要时限制轮数 |
| 工具定义重写 | FunctionTool → @tool | 逐个重写工具注册 |
| 编排重做 | Team → Workflow 图 | 按任务画有向图（顺序/并行/条件） |

## 5. 迁移实战要点

### 迁移步骤

```
① 盘点存量：Agent 列表、工具、编排模式、终止条件
② 画工作流图：把对话式流程映射为有向图
③ 重写工具：FunctionTool → @tool
④ 重写编排：Team → SequentialBuilder/并行/条件
⑤ 加检查点：关键步骤保存状态
⑥ 验证：测试集回归（对话结果一致性）
```

### 工具迁移示例

```python
# AutoGen（v0.4）
inventory_tool = FunctionTool(
    query_inventory,
    description="查询商品库存",
)

# MAF
from autogen_agentchat.agents import AssistantAgent  # MAF 保留的高层 API
@tool
def query_inventory(product_id: str) -> str:
    """查询商品库存"""
    ...
```

### 编排迁移示例

```python
# AutoGen：Team（对话驱动）
team = RoundRobinGroupChat([planner, executor], max_turns=6)

# MAF：Workflow（图驱动）
from autogen_agentchat.workflows import SequentialBuilder
workflow = (
    SequentialBuilder()
    .step(planner)      # 明确顺序
    .step(executor)
    .build()
)
```

### 迁移决策（务实）

```
迁移不是必选项：
✅ 需要 MAF 新特性（检查点/DevUI/负责任 AI/企业集成）→ 迁移
✅ 重构窗口期（反正要大改）→ 顺势迁移
✅ 系统稳定运行且无新需求 → 继续维护（AutoGen 仍在修复 bug）
```

## 6. 学习建议与面试

### 学习建议（2026）

```
AutoGen 体系的正确用法：
① 理解思想：对话编排 → 这是理解 MAF/LangGraph 的基础
② 学习 MAF：新项目路线（本仓库可扩展 MAF 体系）
③ 对照 LangGraph：图式编排的另一种实现（同级目录体系）
④ 存量维护：本体系 01-07 篇作为维护手册
```

### 面试高频问法

| 问题 | 回答要点 |
|---|---|
| MAF 是什么？ | AutoGen + Semantic Kernel 合并；图工作流 + 企业集成 |
| 为什么合并？ | 编排（AutoGen）+ 集成（SK）互补，统一企业 Agent 框架 |
| 迁移最大坑？ | Agent 默认多轮工具循环（AutoGen 单轮）——行为差异 |
| 工具怎么迁移？ | FunctionTool → @tool 装饰器 |
| 编排怎么迁移？ | Team → 类型化有向图 Workflow |
| 新项目选什么？ | 微软栈 MAF；否则 LangGraph/OpenAI Agents SDK |

### 面试加分表达

> "MAF 的迁移本质是'从自由对话走向显式图'——微软用图工作流换生产可预测性。我迁移时会先画工作流图、重写工具为 @tool、重点回归 Agent 默认多轮行为这个差异。框架演进给我的判断是：思想（编排模式）比 API 长寿，API 会变，思想是理解新框架的钥匙。"

## 7. MAF 常见误区

| 误区 | 真相 |
|---|---|
| "MAF 只是改名的 AutoGen" | 架构变了：图工作流 + 企业集成 + 检查点，不只是改名 |
| "迁移是必须的" | 稳定系统可继续维护 AutoGen；迁移按业务需求触发 |
| "MAF 没有对话编排" | 保留 Agent 能力 + MagenticOneGroupChat，图是其"生产化外壳" |
| "新框架不成熟" | MAF 1.0 已 GA（2026-04），MIT，官方主推 |
| "学了 AutoGen 白学" | Agent/工具/终止/人审等概念全部迁移有效 |

## 8. MAF 生态与未来方向

### 生态构成

| 组件 | 说明 |
|---|---|
| 模型提供商 | 6 家一行切换（Azure/OpenAI/Anthropic/Bedrock/Gemini/Ollama） |
| 协议 | 原生 MCP（工具）+ A2A（Agent 间） |
| 存储 | Cosmos DB（持久化）+ Application Insights（遥测） |
| 工具 | DevUI 调试器 + 负责任的 AI 内置 |

### 与同级框架的关系（2026）

| 框架 | 关系 |
|---|---|
| LangGraph | 主要竞品（图式编排，生态更大） |
| OpenAI Agents SDK | 竞品（OpenAI 生态） |
| AG2 | 存量的延续（非竞品，不同受众） |

### 未来方向判断

```
MAF 的演进方向（趋势判断）：
① 企业化：Azure 集成加深（365/数据源）
② 标准化：MCP/A2A 协议成为 Agent 互操作标准
③ 生产化：可观测/评估/治理内建
④ 多模态：Agent 处理图/音/视（模型能力驱动）
```

### 学习路线（2026 推荐）

```
理解流派（AutoGen 体系：对话思想）→ 掌握主流（MAF/LangGraph 二选一深耕）
→ 关注协议（MCP/A2A）→ 实践（自己的 Agent 项目）
```

> 🎯 核心要点：MAF = AutoGen 编排 + SK 企业集成（2026-04 GA，MIT）；图工作流取代自由对话（生产要可预测）；迁移四步（盘点→画图→重写工具→重写编排）；三大坑（默认多轮行为/@tool/Workflow 重做）；迁移按需（稳定系统可继续维护）；MAF 误区五条校准认知；学习上"思想比 API 长寿"，2026 推荐路线是"理解流派 → 深耕 MAF/LangGraph → 掌握 MCP/A2A 协议"。

---

**返回总览**：[00-AutoGen知识体系总览](00-AutoGen知识体系总览.md)
