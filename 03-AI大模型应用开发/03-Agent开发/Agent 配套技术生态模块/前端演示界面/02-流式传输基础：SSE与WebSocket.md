# 流式传输基础：SSE 与 WebSocket

> 打字机效果的本质是"事件流"：后端每秒产 30-80 个事件，前端逐个渲染。选对传输协议、定好事件格式，界面体验与架构复杂度全由这两件事决定。

## 1. 四种传输方式对比

| 维度 | 轮询 | 长轮询 | SSE | WebSocket |
|---|---|---|---|---|
| 方向 | 单向（客户端拉） | 单向（挂起后推） | 单向（服务端推） | 全双工 |
| 协议 | HTTP | HTTP | HTTP（text/event-stream） | 独立握手升级 |
| 浏览器 API | fetch 循环 | fetch 循环 | `EventSource` 原生 | `WebSocket` 原生 |
| 自动重连 | 无 | 无 | **内置（Last-Event-ID）** | 需自己实现 |
| 连接成本/连接 | 高 | 中 | 低（14-64 KiB） | 高（100KB-1MB） |
| 代理友好度 | 好 | 好 | 好（需关缓冲） | 差（需 Upgrade 穿透） |
| 双向能力 | 可模拟 | 可模拟 | 无（需配合 POST） | 原生 |
| LLM 流式适用 | ❌ | ❌ | ✅ 默认选择 | 仅双向场景 |

> 🎯 核心要点：**"用户打字→服务端流式吐 token"是教科书级单向场景，SSE 是默认与正解**；不要没有双向需求就"升级"到 WebSocket——迁移成本真实存在（代理配置、重连、横向扩展都要重做）。

## 2. SSE 协议详解

### 2.1 事件格式

响应头：`Content-Type: text/event-stream`。每个事件由若干字段行 + 空行分隔：

```text
id: 42
event: token
data: {"text": "今天"}

data: {"text": "天气"}

```

| 字段 | 作用 | 必选 |
|---|---|---|
| `data:` | 事件载荷（可多行拼接，各 models 拼接方式不同） | ✅ |
| `id:` | 事件序号，重连时随 Last-Event-ID 上报 | 推荐 |
| `event:` | 事件类型，前端按类型分发渲染 | 推荐 |
| `retry:` | 重连间隔（毫秒） | 可选 |
| `: ping` | 注释行，纯心跳防代理超时 | 可选 |

### 2.2 浏览器消费

```js
const es = new EventSource('/api/chat/stream?sid=abc');
es.addEventListener('token', (e) => appendText(JSON.parse(e.data).text));
es.addEventListener('tool_call', (e) => renderToolCard(JSON.parse(e.data)));
es.addEventListener('done', () => es.close());
```

浏览器原生自动重连：断线后按 retry 间隔重连，并把最后一次收到的 `id` 作为 `Last-Event-ID` 请求头发送——服务端可据此续传。

## 3. 为什么 LLM 全家都用 SSE

| 服务 | 流式方式 | 说明 |
|---|---|---|
| OpenAI | SSE（`stream: true`） | choices[0].delta 增量 |
| Anthropic | SSE | content_block_delta / thinking_delta |
| Gemini | SSE（`streamGenerateContent`） | candidates[0].content.parts |
| 各家 SDK | 底层都是 SSE | OpenAI/Anthropic/Vercel AI SDK 默认 |

意味着：**无论前端用什么框架，后端对接模型商都是"收 SSE 事件、转发 SSE 事件"**，链路天然同构，不需要协议转换。

## 4. 何时该用 WebSocket

| 场景 | 为什么需要双向 | 例子 |
|---|---|---|
| 中途取消生成 | SSE 取消要"关闭连接或另发请求配对" | 生成 30 秒后用户点停止 |
| 工具审批（HITL） | 卡住等用户批准/拒绝 | "删除 47 个文件？" |
| 生成中转向 | 客户端指令中途打断推理 | "停，说短一点" |
| 语音 Agent | 持续双向音频流 | 实时语音对话 |
| 多 Agent 协调 UI | 客户端根据部分结果派发后续任务 | 子 Agent 进度看板 |

WebSocket 的成本：每层都要配（nginx `Upgrade` 头、ALB、Cloudflare、防火墙）、无内置重连（自研心跳+退避）、横向扩展要 Redis Pub/Sub 协调。

## 5. 混合模式（2026 生产推荐）

```text
读路径：SSE  —— 模型输出 / 状态事件 / 日志尾随
写路径：POST —— 取消请求 / 工具审批 / 转向指令（服务端把结果推回既有 SSE 流）
```

- 客户端 POST 写信号 → 服务端在对应会话的 SSE 流里下发响应事件。
- 一个双向需求都不丢，却保留 SSE 的全部基础设施简单性。
- Cloudflare Agents API 与多家托管 Agent 平台即此模式。

## 6. 事件协议设计：从裸 token 到类型化事件

| 事件类型 | 载荷示例 | 前端动作 |
|---|---|---|
| `token` | `{"text":"..."}` | 追加到当前消息 |
| `thinking` | `{"delta":"先搜索..."}` | 渲染思考块（折叠） |
| `tool_call` | `{"id":"t1","name":"search","args":{...}}` | 渲染工具卡片"执行中" |
| `tool_result` | `{"id":"t1","status":"ok","dur_ms":832}` | 工具卡片"成功+耗时" |
| `citation` | `{"url":"...","idx":3}` | 答案锚点引用 |
| `error` | `{"code":"rate_limit","msg":"..."}` | 流内错误提示 |
| `done` | `{"usage":{...}}` | 收尾、关闭 |

要点：

- **统一格式**：把各家模型 chunk（OpenAI delta vs Anthropic content_block_delta）翻译成一套稳定事件再下发，前端只认这一套。
- **带 id 与明确终止**：id 供 Last-Event-ID 续传；以显式 `done` 事件收尾，前端才能可靠清理状态。
- **工具调用是部分 JSON**：tool_call 参数以片段流式到达，不能对单个 chunk `JSON.parse`，要按调用 id 累积缓冲直到完整。
- **HTTP 状态码陷阱**：响应头发出后状态码已定型，错误必须在流内以 `error` 事件表达。

## 7. 缓冲问题：SSE 第一大坑

代理/网关默认缓冲响应，token 攒成一坨才到达——打字机效果变"10 秒静默+瞬间整段"。

| 层 | 配置 |
|---|---|
| Nginx | `proxy_buffering off;` + 上游头 `X-Accel-Buffering: no` |
| 应用 | `Cache-Control: no-cache, no-transform`，每次写后 flush |
| Cloudflare | 对 SSE 路径关闭 HTML 缓存（或走 Workers） |
| 空闲保活 | 每 15-30 秒发一行 `: ping`（ALB 默认 60s 空闲断开） |

## 8. 断线重连与横向扩展

| 问题 | 方案 |
|---|---|
| 断线续传 | 事件带序号；SSE 用 Last-Event-ID，服务端缓存部分生成态按请求 ID 续发 |
| 多实例会话归属 | 会话请求粘滞到固定实例（sticky session），或经 Redis Pub/Sub 广播 |
| 客户端失联 | 服务端超时清理生成任务（如 60s 无读则 abort 模型请求，避免烧钱） |
| 前端兜底 | 超过 3 秒无事件判定流卡死，提示"继续等待/停止"；自动重连 + 指数退避 |

> 💡 Vercel Edge/Serverless 支持 SSE 但不支持 WebSocket（需外挂 Ably/Pusher）；Cloudflare Workers 两者皆可（WebSocketPair + Durable Objects）。HTTP/3 对 SSE 有天然增益。

## 9. 最小可运行后端示例（FastAPI）

```python
from fastapi import FastAPI
from fastapi.responses import StreamingResponse

app = FastAPI()

def event_stream(sid: str):
    # 真实场景：这里接入 Agent 循环，把事件逐个 yield
    yield "event: token\ndata: {\"text\": \"你好\"}\n\n"
    yield "event: tool_call\ndata: {\"id\": \"t1\", \"name\": \"search\", \"args\": {}}\n\n"
    yield "event: done\ndata: {}\n\n"

@app.get("/api/chat/stream")
def stream(sid: str):
    return StreamingResponse(event_stream(sid), media_type="text/event-stream",
        headers={"Cache-Control": "no-cache", "X-Accel-Buffering": "no"})
```

（完整前端与停止生成见 [06 模块](06-FastAPI+原生前端手写流式对话.md)。）

---

**下一模块**：[03-Streamlit 快速演示](03-Streamlit快速演示.md)　**返回总览**：[00-前端演示界面总览](00-前端演示界面总览.md)

## 参考来源

- [WebSockets vs SSE for AI chat: how to choose for production（Ably）](https://ably.com/topic/ai-stack/websockets-vs-sse-ai-chat)
- [Streaming AI API with SSE and WebSockets in 2026: A Practical Latency Guide（CrazyRouter）](https://crazyrouter.com/en/blog/streaming-ai-api-sse-websockets-2026-latency-guide)
- [Designing Streaming APIs for LLM Applications: SSE, WebSockets, and HTTP Chunked Transfer（CallSphere）](https://callsphere.ai/blog/designing-streaming-apis-llm-applications-sse-websockets-chunked-transfer)
- [Agent Streaming Transport 2026: WebSockets vs SSE vs Long-Polling at 10K Sessions（Agent Market Cap）](https://agentmarketcap.ai/blog/2026/04/11/agent-streaming-transport-layer-websockets-sse-2026)
- [Is SSE breaking your AI chat experience? Here are your alternatives（Ably）](https://ably.com/topic/sse-alternatives-ai-chat)
