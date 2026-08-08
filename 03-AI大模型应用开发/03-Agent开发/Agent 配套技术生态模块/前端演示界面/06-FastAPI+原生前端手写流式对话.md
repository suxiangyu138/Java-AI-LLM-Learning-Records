# FastAPI+原生前端手写流式对话

> 零框架、零依赖的 SSE 全链路手写：FastAPI StreamingResponse 透传模型事件流 + 浏览器 EventSource 渲染——搞懂这一篇，任何 UI 框架对你都是"换皮"。

## 1. 为什么值得手写一遍

| 框架方案 | 你理解的 | 你学不到的 |
|---|---|---|
| Streamlit/Gradio/Chainlit | 高层 API | 事件格式、缓冲、断线、取消 |
| 手写全链路（本文） | 协议与工程细节 | —— |

> 🎯 核心要点：**手写一次，三层架构就固化了**——此后用任何框架都是在"贴这层皮"。同时它是面试"聊天流式怎么实现"的标准答案骨架。

## 2. 系统结构

```text
浏览器（原生 JS）
  ├─ POST /api/chat          → 发起会话（传历史+新消息）
  ├─ GET  /api/chat/stream   → EventSource 收事件流
  └─ POST /api/chat/stop     → 停止生成

FastAPI 后端
  ├─ Agent 循环（调模型 API stream=True）
  ├─ 事件翻译层（模型 chunk → 统一事件格式）
  └─ 会话注册表（dict：sid → 生成任务句柄，供 stop 用）
```

## 3. 后端：事件翻译与流式转发

```python
from fastapi import FastAPI
from fastapi.responses import StreamingResponse
from openai import OpenAI

app = FastAPI()
SESSIONS = {}          # sid -> 取消句柄（演示用 dict，生产用 Redis）

client = OpenAI()      # 兼容 deepseek/qwen 等 OpenAI 协议厂商

def translate_to_events(sid: str, messages: list):
    """把模型 chunk 翻译成统一 SSE 事件（见 02 模块事件表）"""
    stream = client.chat.completions.create(
        model="deepseek-chat", messages=messages, stream=True)
    for chunk in stream:
        if chunk.choices and chunk.choices[0].delta:
            d = chunk.choices[0].delta
            if d.content:                          # 文本 token
                yield f"event: token\ndata: {json.dumps({'text': d.content})}\n\n"
            if d.tool_calls:                       # 工具调用片段（需累积）
                for tc in d.tool_calls:
                    SESSIONS.setdefault(f"tc_{sid}", {})[tc.index] = {
                        "name": tc.function.name or "",
                        "args": (SESSIONS.get(f"tc_{sid}", {}).get(tc.index, {})
                                 .get("args", "") + (tc.function.arguments or ""))}
    for i, tc in SESSIONS.pop(f"tc_{sid}", {}).items():  # 完整后统一发事件
        yield f"event: tool_call\ndata: {json.dumps(tc)}\n\n"
    yield "event: done\ndata: {}\n\n"

@app.post("/api/chat")
def chat(req: dict):
    sid = uuid4().hex
    SESSIONS[sid] = {"history": req.get("messages", [])}
    return {"sid": sid}

@app.get("/api/chat/stream")
def stream(sid: str):
    messages = SESSIONS[sid]["history"]
    return StreamingResponse(
        translate_to_events(sid, messages),
        media_type="text/event-stream",
        headers={"Cache-Control": "no-cache",
                 "X-Accel-Buffering": "no",
                 "Connection": "keep-alive"})
```

要点：

- **media_type 必须是 `text/event-stream`**，否则 EventSource 拒绝连接。
- **缓冲三件套**：`Cache-Control: no-cache`、`X-Accel-Buffering: no`、每次 yield 后服务端框架自动 flush——Nginx 再配 `proxy_buffering off`（见 [02 模块](02-流式传输基础：SSE与WebSocket.md)）。
- 工具调用参数是片段流，先按 index 累积，调用结束才发完整事件。

## 4. 前端：EventSource 消费与渲染

```html
<!DOCTYPE html>
<html><body>
<div id="chat"></div>
<input id="inp" placeholder="问点什么..." />
<button id="stop" hidden>停止</button>
<script>
const $chat = document.getElementById('chat');
const inp = document.getElementById('inp');

async function send() {
  const text = inp.value; inp.value = '';
  append('user', text);
  const bubble = append('assistant', '');

  // 1. 发起会话，拿到 sid
  const { sid } = await (await fetch('/api/chat', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ messages: [{ role: 'user', content: text }] }),
  })).json();

  // 2. EventSource 订阅事件流
  const es = new EventSource(`/api/chat/stream?sid=${sid}`);
  es.addEventListener('token', (e) => {
    bubble.innerHTML += JSON.parse(e.data).text;   // 见 5.2 的渲染优化
  });
  es.addEventListener('tool_call', (e) => {
    const t = JSON.parse(e.data);
    bubble.innerHTML += `<p class="tool">🔧 ${t.name}(${t.args})</p>`;
  });
  es.addEventListener('done', () => es.close());
  es.onerror = () => { /* 自动重连：浏览器原生行为 */ };
}

function append(role, content) {
  const div = document.createElement('div');
  div.className = role;
  div.innerHTML = content;
  $chat.appendChild(div);
  return div;
}
</script></body></html>
```

## 5. 三个必须处理的工程细节

### 5.1 停止生成

```js
// 前端：点停止 → POST 取消
fetch('/api/chat/stop', { method: 'POST', body: JSON.stringify({ sid }) });
es.close();   // 本地先断
```

```python
# 后端：把生成任务与取消句柄关联
import asyncio

def translate_to_events(sid, messages):
    task = asyncio.current_task()
    SESSIONS[sid]["task"] = task
    try:
        for chunk in stream_model(messages):
            if SESSIONS[sid].get("cancelled"):     # 停止标志
                yield "event: error\ndata: {\"code\": \"cancelled\"}\n\n"
                return
            ...
    finally:
        task = None

@app.post("/api/chat/stop")
def stop(req: dict):
    SESSIONS.get(req["sid"], {}).get("task") and \
        SESSIONS[req["sid"]].__setitem__("cancelled", True)
    return {"ok": True}
```

> 💡 更优做法：直接 abort 上游模型请求（OpenAI SDK 支持请求取消），否则模型还在烧钱生成。

### 5.2 token 渲染优化

| 问题 | 方案 |
|---|---|
| 每 token 改一次 innerHTML → 卡顿 | 累积缓冲：收集 token，`requestAnimationFrame` 每帧只渲染一次 |
| 回答含 Markdown/代码 | 用 marked + highlight.js 渲染；**长回复分段渲染**（每 ~1000 字渲染一段，避免整段重排） |
| 长对话性能 | 虚拟滚动或裁剪早于 N 条的消息（仅演示可全量保留） |

Markdown 渲染骨架（CDN 引入，无构建）：

```html
<script src="https://cdn.jsdelivr.net/npm/marked/marked.min.js"></script>
<script>
const PART = 1000;                       // 分段阈值
let buf = '', lastRender = 0;

function flush(bubble, force = false) {
  if (!force && buf.length < PART) return;   // 攒够一段才渲染
  bubble.innerHTML += marked.parse(buf);     // 分段解析，避免整段重排
  buf = ''; lastRender = performance.now();
}
es.addEventListener('token', (e) => {
  buf += JSON.parse(e.data).text;
  requestAnimationFrame(() => flush(bubble));   // 每帧最多一次
});
es.addEventListener('done', () => flush(bubble, true));
</script>
```

> ⚠️ 直接用 `innerHTML` 拼接时，若模型输出含 HTML 标签会注入执行——上线前对回答做 HTML 转义（`marked` 默认转义原始 HTML）或走白名单；外部 CDN 脚本必须带 `integrity`（SRI）与 `crossorigin="anonymous"`，防 CDN 被攻陷。

### 5.3 错误处理

- 连接成功后 HTTP 状态码已定型 → 错误必须在流内（`event: error`）表达。
- 前端收到 error 事件 → 停止打字机、展示错误卡片、保留已生成部分。
- 断线 → EventSource 自动重连（Last-Event-ID）；服务端按 id 续传（见 [02](02-流式传输基础：SSE与WebSocket.md)）。

## 5.4 消息历史与上下文管理

```python
# 会话注册表：演示用内存 dict；生产换 Redis（TTL + 跨实例共享）
# {sid: {"history": [...], "task": Task|None, "cancelled": bool}}

def append_message(sid: str, msg: dict):
    hist = SESSIONS[sid]["history"]
    hist.append(msg)
    hist = hist[-20:]                       # 窗口裁剪：长会话只留最近 N 条
    return hist
```

| 问题 | 方案 |
|---|---|
| 多用户并发 | sid 每次会话独立生成（uuid），注册表按 sid 隔离 |
| 会话过期 | 注册表加 last_active，空闲 N 分钟清理（避免内存泄漏） |
| 跨实例共享 | dict → Redis（`SESSIONS` 换成 Redis hash，TTL 自动过期） |
| 长会话超窗 | 裁剪窗口或做摘要压缩（见[上下文管理工程](../../幻觉、格式错误、安全、token%20超限/06-上下文管理工程.md)） |
| 重启不丢 | 会话快照落库（SQLite/Redis），服务重启后按 sid 恢复 |

## 5.5 一次 Agent 对话的完整事件序列

```text
浏览器                FastAPI               模型 API
  │ POST /api/chat      │                     │
  │←───────{sid}────────│                     │
  │ GET /stream?sid=    │                     │
  │←──event: tool_call──│←──tool_calls delta──│   (累积完整后发)
  │←──event: token──────│←──content delta─────│   ×N
  │←──event: citation───│←──引用元数据─────────│
  │←──event: done───────│←──stream 结束───────│
  │ 停止按钮            │                     │
  │ POST /api/chat/stop │─abort 上游请求──────→│   (停止烧钱)
  │←──event: error(cancelled)─┐               │
  └ 展示"已停止"并保留已生成部分┘               │
```

对照这张时序图，可以验证 [02 模块](02-流式传输基础：SSE与WebSocket.md) 的事件设计是否齐全：类型化（token/tool_call/citation/error/done）、带 id、显式终止、取消闭环。

## 5.6 SSE 调试三板斧

| 症状 | 排查 | 工具/命令 |
|---|---|---|
| 前端收到整段而非逐字 | 代理缓冲 | `curl -N http://localhost:8000/api/chat/stream?sid=x` 直连后端，逐 token 出现则问题在中间层 |
| 长时间无事件后断开 | 空闲超时 | 抓包看是否缺 `: ping` 心跳；调大代理 read timeout |
| 断线后无法续传 | 缺 id/Last-Event-ID | 事件带递增 id；服务端按 id 缓存最近 N 条事件 |
| 事件到了但前端不显示 | 事件名不匹配 | 浏览器 Network → EventStream 标签直接看事件帧格式 |

> 💡 Chrome DevTools 的 Network 面板对 SSE 有专门的 EventStream 视图，逐帧可见——手写方案调试的第一利器。

## 6. 与框架方案的等价关系

| 框架 | 对应手写中的哪部分 |
|---|---|
| Streamlit `st.write_stream` | 5.2 的渲染优化内置 |
| Chainlit `cl.Step` | tool_call/tool_result 事件的前端展示层 |
| Vercel AI SDK `useChat` | EventSource 消费 + 状态管理的封装（[07 模块](07-现代前端方案：AI-SDK与React生态.md)） |
| AG-UI | 事件协议标准化（[08 模块](08-Agent专属界面：思考过程与工具调用可视化.md)） |

## 7. 何时选手写方案

| 场景 | 结论 |
|---|---|
| 学习/面试/后端主导的小工具 | ✅ 手写，零依赖可运维 |
| 已有 FastAPI 服务，前端需求简单 | ✅ 手写一个 /api/chat + 单页 |
| 需要复杂交互（过程可视化/审批） | 换 Chainlit 或 AI SDK，别手搓 |

> 🎯 核心要点：手写方案的全部价值在于**协议掌握与最小依赖**——把 [02](02-流式传输基础：SSE与WebSocket.md) 的事件协议吃透后，前端渲染就是"按类型画不同卡片"的体力活，届时换任何框架都是降维。

---

**下一模块**：[07-现代前端方案：AI SDK 与 React 生态](07-现代前端方案：AI-SDK与React生态.md)　**返回总览**：[00-前端演示界面总览](00-前端演示界面总览.md)

## 参考来源

- [Streaming AI API with SSE and WebSockets in 2026: A Practical Latency Guide（CrazyRouter）](https://crazyrouter.com/en/blog/streaming-ai-api-sse-websockets-2026-latency-guide)
- [Designing Streaming APIs for LLM Applications（CallSphere）](https://callsphere.ai/blog/designing-streaming-apis-llm-applications-sse-websockets-chunked-transfer)
- [Next.js + Vercel AI SDK Chat With Streaming（OSS AI Hub）](https://ossaihub.com/code/nextjs-ai-sdk-chat-streaming/)
- [WebSockets vs SSE for AI chat（Ably）](https://ably.com/topic/ai-stack/websockets-vs-sse-ai-chat)
