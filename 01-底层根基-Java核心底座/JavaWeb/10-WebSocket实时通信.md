# 10 - WebSocket 实时通信

> HTTP 是"请求-响应"的单行道，WebSocket 是"全双工"的双向通道——消息推送、实时聊天、AI 流式输出、协同编辑，这些场景 HTTP 做不到的事，WebSocket 做到了。

---

## 目录

1. [WebSocket 协议原理](#1-websocket-协议原理)
2. [Java WebSocket API](#2-java-websocket-api)
3. [SSE vs WebSocket vs 轮询](#3-sse-vs-websocket-vs-轮询)
4. [AI 流式输出实战](#4-ai-流式输出实战)

---

## 1. WebSocket 协议原理

```text
HTTP 升级到 WebSocket：

客户端请求（Upgrade）：
  GET /chat HTTP/1.1
  Host: example.com
  Upgrade: websocket
  Connection: Upgrade
  Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==

服务器响应（101 Switching Protocols）：
  HTTP/1.1 101 Switching Protocols
  Upgrade: websocket
  Connection: Upgrade
  Sec-WebSocket-Accept: s3pPLMBiTxaQ9kYGzzhZRbK+xOo=

→ TCP 连接保持，双方随时可以发送消息（不再需要 HTTP 请求/响应格式）
```

| 特性 | HTTP | WebSocket |
|------|:---:|:---:|
| 通信模式 | 请求-响应（单向） | 全双工（双向） |
| 连接 | 短连接（Keep-Alive 除外） | **长连接** |
| 服务器推送 | 不支持（需要轮询） | ✅ 原生支持 |
| 协议开销 | 每请求 ~800 bytes | 每消息 ~2 bytes |
| 适用 | CRUD API / 静态资源 | 实时推送 / 聊天 |

## 2. Java WebSocket API

```java
// 服务端 Endpoint
@ServerEndpoint("/chat/{userId}")
public class ChatEndpoint {
    private static final ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session, @PathParam("userId") String userId) {
        sessions.put(userId, session);
        session.getAsyncRemote().sendText("欢迎 " + userId);
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        // 广播给所有连接的用户
        sessions.values().forEach(s ->
            s.getAsyncRemote().sendText(message));
    }

    @OnClose
    public void onClose(Session session, @PathParam("userId") String userId) {
        sessions.remove(userId);
    }

    @OnError
    public void onError(Session session, Throwable error) {
        error.printStackTrace();
    }
}
```

```java
// Spring Boot WebSocket 配置
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new ChatHandler(), "/ws/chat")
                .setAllowedOrigins("*");
    }
}
```

## 3. SSE vs WebSocket vs 轮询

| 维度 | 短轮询 | 长轮询 | SSE | WebSocket |
|------|:---:|:---:|:---:|:---:|
| 方向 | 客户端→服务器 | 客户端→服务器 | **服务器→客户端** | **双向** |
| 协议 | HTTP | HTTP | HTTP | **WS/WSS** |
| 浏览器支持 | ✅ 全部 | ✅ 全部 | ✅ 大部分 | ✅ IE10+ |
| 自动重连 | ❌ | ❌ | ✅ 内置 | ❌ 手动 |
| AI 流式输出 | ❌ | ❌ | ✅ 最佳 | ✅ 也可 |

```text
AI 开发的选型建议：
├── 流式文本输出（ChatGPT 风格） → SSE ✅（最简单）
├── 双向实时对话 → WebSocket ✅
├── 简单的状态查询（每隔 5 秒查一次） → 轮询够了
└── 文件上传进度条 → SSE ✅
```

## 4. AI 流式输出实战

```java
// SSE 实现 AI 流式回答（Servlet）
@WebServlet("/ai/chat/stream")
public class ChatStreamServlet extends HttpServlet {
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        resp.setContentType("text/event-stream");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-cache");
        resp.setHeader("Connection", "keep-alive");

        PrintWriter writer = resp.getWriter();
        String prompt = req.getReader().readLine();

        // 模拟 AI 逐 token 生成
        String answer = aiModel.generate(prompt);
        for (String token : answer.split(" ")) {
            writer.write("data: " + token + "\n\n");
            writer.flush();          // ← 立即发送，不等缓冲区满
            Thread.sleep(50);        // 模拟生成延迟
        }
        writer.write("data: [DONE]\n\n");
        writer.close();
    }
}
```

```javascript
// 前端 SSE 消费（原生 Fetch API）
const eventSource = new EventSource('/ai/chat/stream');

eventSource.onmessage = (event) => {
    if (event.data === '[DONE]') {
        eventSource.close();
        return;
    }
    document.getElementById('output').innerText += event.data;
};

eventSource.onerror = () => {
    eventSource.close();  // 自动重连可能无限循环
};
```

## 核心要点回顾

- WebSocket = HTTP Upgrade → 全双工长连接
- `@ServerEndpoint` 是 Java WebSocket 的标准 API
- SSE > WebSocket（AI 流式输出首选，更简单）
- SSE 自动重连，WebSocket 需手动实现
- `writer.flush()` 是流式输出的关键（立即发送）

## 参考资料

1. JSR 356: Java WebSocket API
2. SSE 规范 — HTML Standard
3. Spring WebSocket 文档
