# 工具发现：tool_search 与动态加载

> 发现 = 从工具目录中找出"当前任务需要哪几个工具"的能力。2026 最大变化：**声明从"启动时全量塞入"走向"运行时按需加载"**——OpenAI tool_search、Google 运行时 schema 校验、MCP 注册中心三路并进。核心动机：工具生态 >20 后，路由瓶颈不在文本生成，而在"找到对的工具"。

## 1. 为什么静态声明会失效

| 问题 | 数据/机制 |
|---|---|
| token 膨胀 | 50 工具 × 150 token = 7500 token，且每次请求全量重复 |
| 注意力弥散 | 60 个工具混在上下文里，模型"选择困难"——相关工具反而被淹没 |
| 准确率下降 | 2026 实测：Haiku 级模型单上下文 10-15 个工具即准确率边界 |
| 安全暴露面 | 无权工具也进上下文 → 被诱导调用（08 篇） |
| 更新成本 | 工具变更要重新部署全量定义 |

> 🎯 核心要点：静态声明不是"错"，是**规模问题**——工具 <20 时全量注入简单可靠；>20 时必须在"注册（全量保存）"与"声明（按需注入）"之间加一道**发现层**。

## 2. 发现层在架构中的位置

```text
注册表（全量：定义+元数据）
   ↓ 发现层（过滤/检索/按需加载）
声明（注入上下文的子集）
   ↓
模型（选择 + 调用）
   ↓
执行器
```

| 发现策略 | 做法 | 适用 |
|---|---|---|
| 静态过滤 | 按权限/域/会话状态预筛（02 篇） | 中小生态 |
| 模型驱动发现 | 模型先识别意图，再按需加载（tool_search） | 大生态 |
| 应用驱动发现 | 应用根据状态检索相关工具（client-executed） | 多租户/状态相关 |
| 混合 | 静态过滤 + 动态兜底 | 2026 生产主流 |

## 3. OpenAI tool_search 机制（GPT-5.4/5.5）

```python
# 声明侧：tools 数组加 tool_search + defer_loading
response = openai.responses.create(
    model="gpt-5.5",
    input="List open orders for customer CUST-12345.",
    tools=[crm_namespace, {"type": "tool_search"}],   # 命名空间 + 工具搜索
    parallel_tool_calls=False,
)
```

| 概念 | 说明 |
|---|---|
| `defer_loading: true` | 标记延迟加载：完整定义不进上下文，只暴露 name/描述 |
| namespace | 工具分组：请求开始时模型只见"组名 + 组描述"，组内函数按需加载 |
| tool_search_call | 模型决定"需要加载某组工具"时输出的搜索步骤记录 |
| tool_search_output | 搜索结果：`tools` 数组 = 本次加载的可调用子集 |
| hosted 模式 | 服务端在声明范围内搜并返回（`execution: "server"`，call_id 为 null） |
| client 模式 | 模型输出 tool_search_call 后停下，**应用执行搜索**并回传（`execution: "client"`，call_id 需回显） |

> 🎯 核心要点：tool_search 的本质是**把"选工具"变成一次结构化调用**——模型不再从 60 个定义里挑，而是"先搜到 5 个、再从 5 个里选"。这是两阶段选择（05 篇）的协议级实现。

### 3.1 命名空间设计规范

| 规范 | 理由 |
|---|---|
| 每组 ≤10 个函数 | token 效率与模型性能平衡点 |
| 组描述要概括性强 | 模型靠组名+描述决定是否加载该组 |
| 可混合 deferred 与非 deferred | 高频工具不延迟、低频工具进命名空间 |
| 复杂任务可一次加载多组 | 单个 tool_search_call 支持多 namespace/MCP server |
| 客户端模式已加载不必重载 | 跨轮次保留，从数组移除即禁用 |

## 4. client-executed 模式：应用控制的发现

| 适用场景 | 做法 |
|---|---|
| 工具可用性取决于租户/项目状态 | 应用知道该给什么工具，模型不知道 |
| 权限边界在应用侧 | 搜索时直接应用过滤规则 |
| 动态生成的工具 | 运行时才存在的工具集 |
| 审计要求 | 应用侧可记录每次搜索与加载 |

```text
client 模式流程：
  模型输出 tool_search_call（含搜索参数）
  → 应用执行检索（按 schema 参数 + 权限过滤）
  → 回传 tool_search_output（含 call_id 回显 + tools 数组）
  → 后续轮次模型调用已加载工具
```

> 💡 与 hosted 模式的选择：**hosted 适合"声明时就知候选"**（省一次往返）；**client 适合"工具集取决于应用状态"**（如多租户 SaaS）。生产上两者常混用。

## 5. Google 方案：Context-Aware Polymorphic Schema Validation

| 项 | 说明 |
|---|---|
| 出处 | Google ADK + Gemini 3 Flash（2026 提出） |
| 核心 | schema 不预载提示词，存注册中心（如 Cloud Storage），带字段定义、映射规则、校验钩子 |
| 流程 | 短 discovery prompt 识别意图 → 只加载相关描述符 → 逐字段程序化校验后放行 |
| 收益 | 减 token；防"注意力弥散"（跨 schema 混字段）；注册中心热更新免重部署；多 Agent 交接确定性校验 |
| 意义 | "注册中心 = 一等基础设施"的工程化方案（与 OpenAI 命名空间异曲同工） |

> ⚠️ 2026 趋势判断：OpenAI（tool_search）与 Google（运行时校验）殊途同归——**模型先意图识别、再按需拉取、加载后校验**。框架层跟进只是时间问题（Semantic Kernel 等 2026 已出现相关讨论）。

## 6. 数量治理：上限、两阶段与监控

| 治理项 | 规范 |
|---|---|
| 单 Agent 工具数 | <20 建议；30 硬拒绝（超限拆子 Agent 委派） |
| 单上下文注入数 | 10-15（Haiku 级模型准确率边界） |
| 两阶段选择 | 先分类/域过滤（~8 个），再具体选择 |
| 超限处置 | namespace 化 + tool_search；或拆子 Agent（按域分） |
| 监控指标 | 发现命中率、加载-调用比（加载了不用 = 浪费）、选择准确率 |

> 🎯 核心要点：数量治理的目标不是"减少工具"，而是**让模型每次只面对可分辨的候选集**——宁可 50 个工具分 5 个命名空间，也不要 50 个平铺在上下文里。

## 7. 发现与注册、声明的三权分工

| 层 | 职责 | 2026 定位 |
|---|---|---|
| 注册（Registry） | 全量保存定义+元数据+校验 | 单一事实来源（02 篇） |
| 发现（Discovery） | 从全量中检索"当前需要的" | 动态化：模型驱动/应用驱动 |
| 声明（Declaration） | 把选中的定义注入上下文 | 精细化：只给能用的 |

> 💡 三者最易混淆：**注册是"有没有"、发现是"该给谁看"、声明是"真的给他看了"**。架构演进的方向是三者越来越解耦。

## 8. 常见误区

| 误区 | 真相 |
|---|---|
| "工具多就全量注入，模型自己会挑" | 60 个平铺 = 注意力弥散，实测准确率下降 |
| "tool_search 是玩具特性" | GPT-5.4/5.5 生产特性，hosted/client 双模式 |
| "发现层只有 OpenAI 有" | Google 运行时校验、MCP 注册中心同向演进 |
| "namespace 只是组织代码" | namespace 是 token 优化 + 准确率优化手段 |
| "client 模式更简单" | client 模式要自己写检索+过滤，适合状态相关场景，hosted 更省事 |
| "发现解决了所有数量问题" | 数量 >30 仍应拆 Agent——发现不是万能药 |
| "过滤和发现是两套东西" | 静态过滤是发现的朴素形态，动态发现是过滤的协议化 |

## 9. 面试速记

| 问题 | 一句话答案 |
|---|---|
| 为什么需要动态发现？ | 工具 >20 后 token 膨胀 + 注意力弥散 + 准确率下降 |
| tool_search 是什么？ | OpenAI Responses API 运行时工具发现：模型先搜再调用 |
| defer_loading 作用？ | 定义不进上下文，只暴露 name/描述，需要时加载 |
| namespace 规范？ | 每组 ≤10 函数、描述概括性强、可混合延迟与非延迟 |
| hosted vs client 模式？ | 服务端搜（省往返）vs 应用搜（状态相关/权限在应用侧） |
| tool_search_call/output？ | 模型发起加载的记录 + 加载后的可调用子集 |
| Google 方案？ | schema 存注册中心，意图识别后按需拉取 + 逐字段校验 |
| 数量红线？ | <20 建议、30 硬拒绝、单上下文 10-15 准确率边界 |
| 两阶段选择？ | 先分类过滤（~8 个）再具体选择——高于全量选择 |
| 注册/发现/声明关系？ | 有没有 / 该给谁看 / 真的给他看了——三者解耦 |
| 超限怎么处理？ | namespace + tool_search；或按域拆子 Agent 委派 |
| 监控什么？ | 发现命中率、加载-调用比、选择准确率 |

---

**下一模块**：[06-工具执行：执行器与调用循环](06-工具执行：执行器与调用循环.md)　**返回总览**：[00-Tool 工具开发与注册总览](00-Tool工具开发与注册总览.md)

## 参考来源

- [Use tool search with the Azure OpenAI Responses API（Microsoft Learn）](https://learn.microsoft.com/zh-cn/azure/foundry/openai/how-to/tool-search)
- [Google outlines runtime schema checks for AI agents（IT Brief）](https://itbrief.in/story/google-outlines-runtime-schema-checks-for-ai-agents)
- [Support for tool_search in AzureChatCompletion（Semantic Kernel Discussion）](https://github.com/microsoft/semantic-kernel/discussions/13684)
- [OpenAI e tool use agentic: o que mudou em 2026（DIO）](https://www.dio.me/en/articles/openai-e-tool-use-agentic-o-que-mudou-em-2026-80c2c8b58e07)
