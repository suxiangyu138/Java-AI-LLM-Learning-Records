# 现代前端方案：AI SDK 与 React 生态

> 产品级 Agent 界面的 TypeScript 事实标准（Vercel AI SDK 7，2026）：`useChat` 一行接入流式对话、AI SDK 7 可直连 Agent、组件库免造轮子——适合有前端能力的团队把 Agent 界面做成真正的产品。

## 1. 定位与适用边界

| 维度 | AI SDK + React | 说明 |
|---|---|---|
| 团队要求 | 需前端能力 | TypeScript/React（Next.js 最佳） |
| 定制自由度 | 最高 | 界面即代码，任意布局 |
| 生态 | 最活跃 | AI SDK + 组件库 + 服务端框架 |
| 上手门槛 | 中 | 环境/路由/打包一次到位，约 100 行可跑 |
| 支持框架 | React/Vue/Svelte/Solid | 官方适配齐全 |

> 🎯 核心要点：**Python 框架解决"演示"，AI SDK 解决"产品"**——要品牌化、复杂交互、HITL 审批、深度定制时，这是唯一正确的方向；其余场景先用 [03](03-Streamlit快速演示.md)/[05](05-Chainlit对话界面.md) 顶住。

## 2. AI SDK 7 核心能力（2026-08 基准）

| 能力 | 说明 |
|---|---|
| `useChat` | React Hook：消息数组 + 发送/停止/重连，流式对话的标准骨架 |
| `DirectChatTransport` | **直连 Agent**：UI 代码直接持有 Agent 对象调用，无需 HTTP 服务（v7 新） |
| 工具审批流 | 工具调用需用户确认时 UI 内自动弹出审批（approval），支持回放 |
| 实时能力 | browser-to-provider WebSocket 实时会话（realtime 预览） |
| 生命周期回调 | 响应时间、首 token 时间、tokens/秒等性能统计 |
| DevTools | 本地开发工具（localhost:4983）可视化调试 |
| `streamText` | 服务端流式生成核心，`toDataStreamResponse()` 转前端协议 |

## 3. 标准模式：Next.js App Router + useChat

```tsx
// app/api/chat/route.ts —— 服务端路由（引擎层挂 Agent）
import { streamText, toDataStreamResponse } from 'ai';
import { openai } from '@ai-sdk/openai';

export async function POST(req: Request) {
  const { messages } = await req.json();
  const result = streamText({
    model: openai('gpt-5.x'),
    messages,
    tools: { web_search: { execute: async () => search(messages) } },  // Agent 工具
    stopWhen: { delay: 1000 },   // 工具等待间隙
  });
  return result.toDataStreamResponse();
}
```

```tsx
// app/page.tsx —— 渲染层
'use client';
import { useChat } from '@ai-sdk/react';

export default function Chat() {
  const { messages, input, handleInputChange, handleSubmit, stop, isLoading } =
    useChat({ api: '/api/chat' });

  return (
    <div>
      {messages.map((m) => (
        <MessageBubble key={m.id} message={m} />
      ))}
      <form onSubmit={handleSubmit}>
        <input value={input} onChange={handleInputChange} disabled={isLoading} />
        <button type="button" onClick={stop} hidden={!isLoading}>停止</button>
      </form>
    </div>
  );
}
```

流程：前端 `useChat` → POST `/api/chat` → `streamText` 驱动 Agent → `toDataStreamResponse`（AI 数据流协议）→ `useChat` 增量渲染。约 100 行完成 [06 模块](06-FastAPI+原生前端手写流式对话.md) 全部手写工作量。

> 💡 本仓库后端以 Java 为主：AI SDK 是 TS 方案；Java 侧等价物是 [Spring AI 的 ChatClient + SSE](..%2F..%2F..%2F02-大模型基础与Prompt工程%2FFunction%20Calling%20函数调用%E3%80%90Agent%20基石%E3%80%91%2F00-FunctionCalling知识体系总览.md)（协议仍是同一套 SSE 事件设计，前端可共用）。

## 4. Agent 组件库：不用从零画卡片

| 组件库 | 出品 | 覆盖 |
|---|---|---|
| `@ant-design/agentic-ui` | Ant Design | ThoughtChainList（思考链）、ToolUseBar（工具调用条）、AIBubble、TaskList、多 Agent 视图 |
| `agentkit-ui` | agentkit | 工具徽章、流状态检测（3s 静默）、会话恢复 |
| `@bigduu/lotus` | 社区 | 实时待办列表、每帧批量渲染（性能优化参考） |
| HF `chat-ui` | Hugging Face | 完整聊天界面（Open WebUI 同类定位的组件版） |
| `agentivity_ag_ui` | AG-UI 官方系 | AG-UI 协议原生消费组件（Flutter/JS 多端） |

选型建议：`@ant-design/agentic-ui` 功能最全（中文文档友好），`agentkit-ui` 轻量。组件模式详解见 [08 模块](08-Agent专属界面：思考过程与工具调用可视化.md)。

## 5. LangChain 前端（TS）：类型化流订阅

LangChain JS 前端按 **channels / namespaces** 订阅类型化事件，而非解析文本流：

```ts
const stream = useStream({
  endpoint: '/api/langgraph',
  channels: ['messages', 'tools', 'values'],   // 只订阅需要的通道
  namespace: ['agent', 'web_search_subagent'], // 精确到子图
});
```

| 概念 | 作用 |
|---|---|
| Channels | 流里有什么：messages（消息）、values（图状态）、tools（工具生命周期）、checkpoints（分支） |
| Namespaces | 事件发生在哪：根图 vs 嵌套子图 → 子 Agent 独立渲染 |
| Typed content blocks | 推理/工具参数/多模态以结构化块到达，而非拼接字符串 |
| `useStream`/`injectStream` | React/Vue/Svelte/Angular 各框架的官方 Hook |

## 6. Vue / Svelte 方案

| 框架 | 适配 | 说明 |
|---|---|---|
| Vue 3 | `ai/vue`（useChat 同构） | 选项/组合式 API 均可 |
| Svelte | `ai/svelte` | 轻量，适合小团队 |
| Angular | LangChain `injectStream` | 企业存量栈 |

接口与 React 版一致（messages/input/handleSubmit），学习成本趋近于零。

## 7. 何时升级到 AI SDK 方案

| 信号 | 说明 |
|---|---|
| 需要品牌化 | 主题、布局、动效完全自主 |
| 需要 HITL | 工具审批、人工确认流（AI SDK 原生支持） |
| 需要深度定制过程可视化 | 组件库 + 自有事件协议 |
| 多端（Web/移动/桌面） | React 生态全覆盖 |
| 团队有前端 | 有专人才值得持续投入 |

> 🎯 核心要点：AI SDK 方案的护城河不在"能聊天"（Python 框架也行），而在**工具审批、Agent 直连、类型化事件、性能统计**这些产品化能力——它把 [08 模块](08-Agent专属界面：思考过程与工具调用可视化.md) 的复杂可视化变成了组件与 Hook 的组装。

---

**下一模块**：[08-Agent 专属界面：思考过程与工具调用可视化](08-Agent专属界面：思考过程与工具调用可视化.md)　**返回总览**：[00-前端演示界面总览](00-前端演示界面总览.md)

## 参考来源

- [AI SDK 7 is now available（Vercel）](https://vercel.com/changelog/ai-sdk-7)
- [Claude Agent SDK vs Vercel AI SDK 6 — which to pick in 2026（dev.to）](https://dev.to/muhammad_moeed/claude-agent-sdk-vs-vercel-ai-sdk-6-which-to-pick-in-2026-2jj)
- [From Token Streams to Agent Streams（LangChain Blog）](https://www.langchain.com/blog/token-streams-to-agent-streams)
- [LangChain JS Frontend Overview（官方文档）](https://docs.langchain.com/oss/javascript/langchain/frontend/overview)
- [@ant-design/agentic-ui（npm）](https://www.npmjs.com/package/@ant-design/agentic-ui)
- [agentkit-ui（npm）](https://www.npmjs.com/package/agentkit-ui)
- [Next.js + Vercel AI SDK Chat With Streaming（OSS AI Hub）](https://ossaihub.com/code/nextjs-ai-sdk-chat-streaming/)
