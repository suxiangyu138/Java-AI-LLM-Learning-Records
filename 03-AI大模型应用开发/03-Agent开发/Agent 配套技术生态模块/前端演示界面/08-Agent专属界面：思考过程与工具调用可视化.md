# Agent 专属界面：思考过程与工具调用可视化

> 聊天界面回答"答了什么"，Agent 界面还要回答"做了什么、为什么"——过程可视化 + 控制平面，是 2026 年 Agent 界面的分水岭。

## 1. 为什么聊天界面不够

| 用户痛点 | 场景 | 后果 |
|---|---|---|
| 答案不可信 | "这个结论哪来的？" | 无法核验 → 弃用 |
| 等待无感 | 长任务只有转圈 | 焦虑、中途流失 |
| 不可干预 | 发现方向错了 | 只能等完再重来 |
| 无法审计 | 出事故追责 | 没有过程记录 |

> 🎯 核心要点：**Agent 界面必须同时是"进度条 + 审计日志 + 遥控器"**——让用户看到工作、验证工作、打断工作。

## 2. 核心设计原则：渐进式披露

2026 年的共识答案（LukeW 等产品研究）：**默认只显示结论，过程细节一层点击之遥**。

```text
用户视角                    工程师视角
┌──────────────────┐    ┌──────────────────────┐
│ 最终答案 + 引用     │◄─1 次点击─│ 工具调用卡片（状态+耗时）│
│                  │    ├──────────────────────┤
│   ▲ 默认折叠       │    │ 思考过程（可滚动）       │
└──────────────────┘    │ 子 Agent 树            │
                        │ 检查点/分支             │
                        └──────────────────────┘
```

| 披露层级 | 显示内容 | 默认状态 |
|---|---|---|
| L0 结论 | 答案、引用、最终输出 | 默认显示 |
| L1 摘要 | 每步工具"名字+状态+耗时"一行 | 折叠可见 |
| L2 详情 | 工具参数/结果全文、思考过程 | 点击展开 |
| L3 控制 | 中断、审批、回放、分支 | 按需调用 |

反面教材：早期 Bench 迭代"全开"（工具设置、重跑、中断全摆出来）被证明过载；ChatDB 双栏"工作→结果右栏着陆后左栏折叠"、Intent 单行可展开——都是披露分级的成功案例。

## 3. 事件协议：AG-UI（跨框架标准）

AG-UI 把 Agent 过程事件标准化，走 SSE：

| 事件族 | 事件 | 载荷 |
|---|---|---|
| 推理 | `REASONING_START/UPDATE/END` | ThinkingBlock（思考块增量） |
| 工具 | `TOOL_CALL_START` | 工具 id/名称/参数 |
| 工具 | `TOOL_CALL_END` | 状态（success/error）、结果 |
| 状态 | `STATE_SNAPSHOT` | 全量状态快照 |
| 状态 | `STATE_DELTA` | 增量（RFC 6902 JSON Patch） |
| 消息 | `MESSAGE_START/UPDATE/END` | 消息正文 |

- 生态：Java/AgentScope、Flutter（agentivity_ag_ui）、Vercel 均有实现。
- 意义：前端组件库与后端引擎解耦——换引擎不动 UI，换 UI 不动引擎。

## 4. 过程可视化的组件词汇表

| 组件 | 形态 | 展示内容 |
|---|---|---|
| 工具调用卡片（Tool Call Card） | 状态图标（⏳/✅/❌）+ 名称 + 耗时 ms，展开见参数与结果 | 替代原始 JSON 的必备形态 |
| 思考块（Thinking Block） | 斜体/图标标记、可折叠，与答案区分渲染 | 推理过程（CoT） |
| 思考链（Thought Chain） | 完整"思考-行动-观察"时间线（Ant Design ThoughtChainList） | ReAct 循环全貌 |
| 任务列表（Task List） | 计划步骤逐项 running/done/waiting | Agent 规划执行进度 |
| 子 Agent 树（Subagent Tree） | 嵌套节点保留层级，选中才展开详情流 | 多 Agent 结构 |
| 引用（Citation） | 答案锚点 + 可点击来源列表 | RAG 溯源 |

> 💡 实现捷径：别自研，用 `@ant-design/agentic-ui`（ToolUseBar/ThoughtChainList）、`agentkit-ui`（工具徽章）或 AG-UI 系组件——它们即本节组件表的现成实现。

## 5. 渲染实现要点

| 问题 | 方案 |
|---|---|
| 每 token 重渲染全树 | token 缓冲 + `requestAnimationFrame` 每帧一次（Lotus 模式） |
| 状态分散导致重渲染 | 流式数据用细粒度原子状态（Jotai），业务状态另用 store（Zustand） |
| 长流卡顿 | 消息虚拟化；工具结果默认截断展示（如前 2000 字符） |
| Markdown/代码 | 流式 Markdown 分段渲染 + 代码高亮；Mermaid 图 zoom/pan |
| 消息类型化 | `TextContent \| ThinkingContent \| ToolCallContent \| ToolResultContent` 联合类型 + `displayMode: 'compact'|'detailed'|'live'` 切换 |

## 6. 控制平面：UI 不只是渲染器

| 能力 | 说明 | 价值 |
|---|---|---|
| 中断（Interrupt） | 工具执行中点停止，从断点恢复 | 防止跑偏烧钱 |
| 审批（HITL） | 危险操作弹确认（"删除 47 个文件？"） | 安全边界 |
| 检查点（Checkpoint） | 流状态可保存/恢复 | 跨设备续跑 |
| 分支/时间旅行 | 从历史某点 fork 新路径 | 探索多方案 |
| 回放 | 已结束运行逐步回看 | 审计、教学 |

协议侧：LangChain channels 里 `checkpoints` 通道 + 生命周期事件；AI SDK 7 的 `needsApproval` + 审批回放。实现侧：UI 发出控制指令走"写路径"（POST），与 [02 模块](02-流式传输基础：SSE与WebSocket.md) 的混合模式一致。

## 7. 安全与治理红线

| 风险 | 缓解 |
|---|---|
| 思考过程泄露内部提示词 | 默认折叠仍可能被读 → 敏感场景提供"仅显示工具摘要"模式 |
| 工具参数/结果含敏感数据 | 脱敏后再进 UI（日志/密钥/内部路径） |
| UI 暴露控制能力 | 审批与中断接口必须鉴权，防外部滥用 |
| 长流内存 | 只渲染窗口内消息，服务端持久化全量（见 [10 生产实践](10-生产实践与面试冲刺.md)） |

## 8. 不同框架的落地对照

| 框架 | 过程可视化实现 | 控制平面 |
|---|---|---|
| Chainlit | Step 树（tool 类型自动图标+状态） | on_stop 钩子 |
| Streamlit | st.status 折叠区 | 1.59 submit_mode="stop" |
| AI SDK | useChat 消息类型 + 工具审批流 | needsApproval / 回放 |
| LangChain JS | channels/namespaces 类型化订阅 | checkpoints 通道 |
| 手写（06 模块） | 自己按事件画卡片 | 自己实现 POST 控制 |

> 🎯 核心要点：过程可视化的胜负手不在"画得多花哨"，而在**协议类型化（AG-UI/Channels）+ 披露分层（渐进式）+ 控制闭环（中断/审批/回放）**——三者齐备，Agent 界面才从"聊天记录"升级为"工作台"。

---

**下一模块**：[09-低代码平台与成品 UI：Dify 与 Open WebUI](09-低代码平台与成品UI：Dify与Open-WebUI.md)　**返回总览**：[00-前端演示界面总览](00-前端演示界面总览.md)

## 参考来源

- [Showing the Work of Agents in UI（LukeW）](https://lukew.com/ff/entry.asp?2142=)
- [From Token Streams to Agent Streams（LangChain Blog）](https://www.langchain.com/blog/token-streams-to-agent-streams)
- [AG-UI Protocol（AgentScope Java Docs）](https://java.agentscope.io/v2/en/integration/protocol/agui.html)
- [Frontend: EventLog component for agent reasoning visibility（GitHub Issue）](https://github.com/datorresb/ai-lingo/issues/14)
- [@ant-design/agentic-ui（npm）](https://www.npmjs.com/package/@ant-design/agentic-ui)
- [@bigduu/lotus（npm）](https://www.npmjs.com/package/@bigduu/lotus)
