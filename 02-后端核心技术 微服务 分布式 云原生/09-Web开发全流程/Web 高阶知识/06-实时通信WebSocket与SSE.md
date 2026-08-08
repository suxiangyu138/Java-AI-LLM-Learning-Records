# 实时通信：WebSocket 与 SSE
> 轮询→长轮询→WebSocket→SSE 的演进、握手与帧、心跳与断线重连、集群与粘滞会话——"服务端推送给浏览器"的完整选型

## 📚 目录
1. [实时通信方案演进](#1-实时通信方案演进)
2. [WebSocket 机制深入](#2-websocket-机制深入)
3. [SSE：被低估的推送方案](#3-sse被低估的推送方案)
4. [WebSocket vs SSE vs 长轮询](#4-websocket-vs-sse-vs-长轮询)
5. [生产要点：心跳/重连/集群](#5-生产要点心跳重连集群)
6. [Spring Boot 落地](#6-spring-boot-落地)

## 1. 实时通信方案演进

```text
 轮询（1990s）──> 长轮询（2005）──> WebSocket（2011）──> SSE 流行（2010s+）
 每 5 秒问一次     挂住请求等推送     全双工长连接       单向推送长连接
 大量空请求        连接反复建立       双向实时           简单自动重连
```

| 方案 | 实时性 | 成本 | 双向 | 现状 |
|------|:---:|------|:---:|------|
| 轮询 | 差（间隔决定） | 每间隔一请求 | ✅ | 简单状态同步可用 |
| 长轮询 | 中（等待即推送） | 连接反复建 | ✅ | 兼容旧基建的降级方案 |
| **WebSocket** | 即时 | 长连接常驻 | ✅ | 双向实时标准 |
| **SSE** | 即时 | 长连接常驻 | ❌ 单向 | 通知/流式标准 |

> 🎯 选型第一问：**是"通知"还是"对话"？** 只是服务端推消息（通知/进度/行情）→ SSE 够且更简单；需要双向交互（聊天/游戏/协作）→ WebSocket。80% 的业务推送场景用 SSE 就能解决。

## 2. WebSocket 机制深入

### 2.1 握手：HTTP 升级

```http
# 客户端 → 服务器（普通 HTTP 请求，带 Upgrade）
GET /ws/chat HTTP/1.1
Upgrade: websocket
Connection: Upgrade
Sec-WebSocket-Key: x3JJHMbDL1EzLkh9GBhXDw==     # 随机密钥
Sec-WebSocket-Version: 13

# 服务器 → 客户端（101 状态码确认升级）
HTTP/1.1 101 Switching Protocols
Upgrade: websocket
Connection: Upgrade
Sec-WebSocket-Accept: HSmrc0sMlYUkAGmm5OPpG2HaGWk=  # Key+魔数 SHA1 结果
```

> 💡 握手本质：**复用 HTTP 完成协商，然后协议切换为 WebSocket 帧**——这就是为什么 WebSocket 走 80/443 端口、能穿过大部分代理；`Sec-WebSocket-Accept` 是防跨站 WebSocket 劫持的校验。

### 2.2 帧与生命周期

```text
帧结构：FIN + opcode + payload
  opcode：0 续帧 / 1 文本 / 2 二进制 / 8 关闭 / 9 Ping / 10 Pong

连接生命周期：握手(101) → 双向帧通信 → 关闭帧 → TCP 断开
  · 服务端必须回 Ping（Pong），否则客户端判定死连接
  · 客户端/服务端任何一方可主动关闭
```

| 帧类型 | 用途 |
|--------|------|
| 文本/二进制 | 业务数据 |
| Ping/Pong | 心跳保活（应用层也常自建心跳） |
| Close | 优雅关闭（带状态码） |

### 2.3 与 HTTP 的本质差异

| 维度 | HTTP | WebSocket |
|------|:---:|:---:|
| 连接 | 请求-响应即断（Keep-Alive 复用） | **长连接常驻** |
| 方向 | 客户端发起 | **全双工**（服务端可主动推） |
| 协议 | 文本语义 | 帧流 |
| 状态 | 无状态 | 有状态（连接即会话） |

> ⚠️ **WebSocket 的代价是状态**：连接挂在某台服务器上 → 集群必须"粘滞会话"或"广播/订阅"（见 §5）。它是有状态的，与 HTTP 无状态理念相反。

## 3. SSE：被低估的推送方案

### 3.1 原理：一个不关闭的流式响应

```http
# 响应头：text/event-stream，连接持续不关闭
HTTP/1.1 200 OK
Content-Type: text/event-stream
Cache-Control: no-cache
Connection: keep-alive

# 数据格式（每事件两行）
data: {"progress": 50}

data: {"progress": 100}
```

```javascript
// 浏览器消费（EventSource 自动重连！）
const es = new EventSource('/api/sse/notify');
es.onmessage = (e) => console.log(JSON.parse(e.data));
es.onerror = () => console.log('断线，浏览器自动重连');   // ⚠️ 自动重连是内建能力
```

| SSE 特性 | 说明 |
|---------|------|
| 协议 | 纯 HTTP（无需升级，任何服务器/代理都兼容） |
| 自动重连 | **EventSource 内建**（WebSocket 要手写） |
| 事件类型 | `event:` 字段区分类型（`es.addEventListener('type')`） |
| 断点续传 | `Last-Event-ID` 头可续传错过的消息 |
| 方向 | 服务端 → 客户端（单向） |
| 浏览器支持 | 全部主流（IE 除外） |

> 🎯 SSE 的隐藏优势：**它是 HTTP**——走 80/443、兼容 Nginx/CDN/网关、不需要粘滞会话升级、自带重连、超时自动重连。ChatGPT/DeepSeek 的流式输出（打字机效果）就是 SSE。

### 3.2 流式输出（LLM 应用）

```java
// Spring Boot 4：SseEmitter 落地（或 ResponseBodyEmitter）
@GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter chatStream(@RequestParam String prompt) {
    SseEmitter emitter = new SseEmitter(60_000L);
    executor.execute(() -> {
        try {
            for (String chunk : llmService.stream(prompt)) {
                emitter.send(SseEmitter.event().data(chunk));  // 逐块推送
            }
            emitter.complete();
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
    });
    return emitter;   // Controller 立即返回，容器线程释放（异步，见 Servlet 体系）
}
```

> 💡 关联：SSE 本质是 Servlet 异步 + 流式响应（[Servlet 异步体系](../Servlet/06-异步处理与非阻塞IO.md)）的应用形态；Boot 里用 `SseEmitter` 一行接入，详见 [SpringBoot Web 异步返回](../SpringBoot%20Web/02-DispatcherServlet请求处理链路.md) §8。

## 4. WebSocket vs SSE vs 长轮询

| 维度 | WebSocket | SSE | 长轮询 |
|------|:---:|:---:|:---:|
| 方向 | 全双工 | 服务端→客户端 | 客户端→服务端（伪推送） |
| 协议 | 独立帧协议（HTTP 升级） | **纯 HTTP** | HTTP |
| 自动重连 | ❌ 手写 | ✅ 内建 | ❌ 手写 |
| 断点续传 | ❌ | ✅（Last-Event-ID） | ❌ |
| 代理/防火墙兼容 | 需支持 Upgrade | 天然兼容 | 天然兼容 |
| 负载均衡 | 需粘滞/广播 | 普通即可（重连会找新节点） | 普通 |
| 二进制 | ✅ | ❌（仅文本，base64） | ✅ |
| 浏览器兼容 | 全部主流 | 全部主流（IE 除外） | 全部 |
| 复杂度 | 高（状态/心跳/重连） | **低** | 中 |
| 典型场景 | 聊天/游戏/协作/实时编辑 | 通知/进度/行情/LLM 流式 | 兼容旧基建 |

> 🎯 **选型决策表**：
> - 服务端单向推送（通知/进度/行情/日志/LLM 流式）→ **SSE**（简单 + 自动重连 + HTTP 兼容）
> - 双向交互（聊天/游戏/白板/实时协作）→ **WebSocket**
> - 无法升级/老系统 → 长轮询兜底

## 5. 生产要点：心跳/重连/集群

### 5.1 心跳与断线

```text
心跳设计（WebSocket）：
  客户端每 30s 发 Ping（或业务心跳消息）
  服务端 60s 内未收到 → 判定死连接 → 清理会话
  客户端发完等 Pong 超时 → 主动重连（指数退避 + 随机抖动防雪崩）

SSE 天然优势：
  EventSource 断线自动重连；服务端可定期发注释行(: keep-alive)防代理超时断连
```

### 5.2 集群：粘滞会话 vs 广播

```text
单机：连接挂在进程内，直接推
集群（N 台）：
  · 连接分布在各节点（用户连的是节点 2，事件到达节点 1）
  · 方案 A 粘滞会话（sticky）：同一用户固定连同一节点
    └─ 节点挂 → 该节点所有用户连接断（容量不均）
  · 方案 B 广播/订阅（推荐）：Redis Pub/Sub / MQ 广播事件
    └─ 各节点订阅同一频道，谁持有连接谁推
```

```text
推荐架构（生产）：
  业务事件 → Redis Pub/Sub 广播 → 各节点 WebSocket/SSE 会话管理器 → 推给持连用户
  连接状态只存各节点内存 + 心跳检测；节点宕机由重连自动漂移
```

> ⚠️ 负载均衡配置：WebSocket 需要 Nginx 开启 `proxy_set_header Upgrade/Connection`（[Nginx WebSocket 章节](../Nginx/03-Nginx-核心配置.md)）；SSE 需要关掉代理缓冲（`proxy_buffering off`）否则推送不实时——两个都是高频生产坑。

## 6. Spring Boot 落地

| 方案 | Boot 支持 | 适用 |
|------|-----------|------|
| SSE | `SseEmitter`（MVC）/ WebFlux `Flux<ServerSentEvent>` | 推送首选 |
| WebSocket | Spring WebSocket（`WebSocketHandler`）/ STOMP（消息协议，含 @MessageMapping） | 双向交互 |
| 长轮询 | DeferredResult 手写 | 兼容兜底 |

```java
// WebSocket 落地（Spring WebSocket）
@Component
public class ChatHandler implements WebSocketHandler {
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        // 会话入注册表（ConcurrentHashMap<userId, session>）
    }
    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) {
        // 业务处理 + 推送给目标用户
    }
}

// 配置
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatHandler, "/ws/chat").setAllowedOrigins("*");
    }
}
```

> 🎯 综合建议：**新业务默认 SSE**（80% 场景够用且省心）；确需双向/高频实时再上 WebSocket + STOMP；别一上来就 WebSocket——连接状态、心跳、集群广播的运维复杂度是 SSE 的 3-5 倍。

---

**下一模块**：[07-Web安全攻防](07-Web安全攻防.md) / **返回总览**：[00-Web高阶知识总览](00-Web高阶知识总览.md)
