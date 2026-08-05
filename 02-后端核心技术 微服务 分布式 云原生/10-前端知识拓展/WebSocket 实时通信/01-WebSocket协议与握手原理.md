# 01 - WebSocket 协议与握手原理

> 🎯 HTTP 是"请求-响应"单行道，WebSocket 是"全双工"双向通道 — 理解从 HTTP Upgrade 握手到帧格式，是实时通信的基础

---

## 目录

1. [为什么需要 WebSocket](#1-为什么需要websocket)
2. [握手升级过程](#2-握手升级过程)
3. [与 HTTP 对比](#3-与-http-对比)
4. [WebSocket vs SSE vs 轮询](#4-websocket-vs-sse-vs-轮询)

---

## 1. 为什么需要 WebSocket

```text
HTTP 的局限：
  Client → Request → Server → Response → Client
  → 服务端不能主动推消息给客户端！

轮询（Polling）的无奈：
  前端每 3 秒发一次 GET /messages → 看有没有新消息
  → 99% 的请求返回"没新消息"（浪费）

WebSocket：
  Client ↔ WebSocket 连接 ↔ Server
  → 双向实时通信，服务端有新消息立即推
```

---

## 2. 握手升级过程

```text
WebSocket 握手（HTTP Upgrade）：

  浏览器                                     服务端
    │                                          │
    │── GET /chat HTTP/1.1 ──────────────────→│  ← 还是 HTTP 请求
    │   Upgrade: websocket                     │
    │   Connection: Upgrade                    │
    │   Sec-WebSocket-Key: dGhlIHNhbXBsZQ==    │
    │   Sec-WebSocket-Version: 13              │
    │                                          │
    │←─ HTTP/1.1 101 Switching Protocols ──────│  ← 切换协议！
    │   Upgrade: websocket                     │
    │   Connection: Upgrade                    │
    │   Sec-WebSocket-Accept: s3pPLMBi...      │
    │                                          │
    │══════ WebSocket 帧（二进制/文本） ════════│  ← 之后不再是 HTTP
```

```text
握手关键点：
  1. 基于 HTTP 协议升级（复用 80/443 端口）
  2. Sec-WebSocket-Key → 服务端用 SHA1 计算 Accept → 客户端验证
  3. 101 状态码 = "协议切换成功"
  4. 握手后同一 TCP 连接双向传输 WebSocket 帧
```

---

## 3. 与 HTTP 对比

| 维度 | HTTP | WebSocket |
|------|------|-----------|
| 通信模式 | 请求-响应（半双工） | 全双工 |
| 服务端推送 | ❌ 不能 | ✅ 可以 |
| 连接 | 短连接/长连接 | 长连接 |
| 头部开销 | 每次几百字节 | 2-14 字节 |
| 协议 | HTTP | ws:// / wss:// |
| 心跳 | — | PING/PONG 帧 |
| 适用 | REST API、文件传输 | 聊天/推送/实时数据 |

---

## 4. WebSocket vs SSE vs 轮询

| 方案 | 方向 | 协议 | 自动重连 | 适用场景 |
|------|:---:|------|:---:|------|
| **短轮询** | 双向（模拟） | HTTP | — | 极简需求 |
| **长轮询** | 双向（模拟） | HTTP | — | 兼容老浏览器 |
| **SSE** | 服务端→客户端 | HTTP | ✅ 内置 | 单向推送（股票/日志） |
| **WebSocket** | ⭐ 双向 | WebSocket | ❌ 需手写 | ⭐ 聊天/协作/游戏 |

```text
SSE（Server-Sent Events）：
  → 基于 HTTP，服务端 → 客户端单向推送
  → 浏览器原生支持 EventSource API
  → 自动重连、轻量级

SSE 示例：
  const source = new EventSource('/api/events');
  source.onmessage = (e) => console.log(e.data);
```

> 🎯 **选型**：双向实时通信 → WebSocket；服务端推送（如通知/日志） → SSE；简单状态查询 → 轮询。WebSocket 是唯一真正的双向方案。
