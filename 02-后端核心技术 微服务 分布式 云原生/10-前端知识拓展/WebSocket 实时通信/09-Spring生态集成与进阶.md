# 09 - Spring 生态集成与进阶

> 定位：从"会用注解"到"懂机制"——Spring Messaging 抽象、STOMP 帧协议深潜、消息类型化、与 Spring AI 的流式协同——"STOMP 不是魔法：CONNECT/SUBSCRIBE/SEND 帧 + 前缀路由，理解帧协议就理解了 Spring 的实时消息体系"

---

## 📚 目录

1. [Spring Messaging 抽象](#1-spring-messaging-抽象)
2. [STOMP 帧协议深潜](#2-stomp-帧协议深潜)
3. [消息路由与类型化](#3-消息路由与类型化)
4. [用户会话与个性化推送](#4-用户会话与个性化推送)
5. [与 Spring Security 的完整集成](#5-与-spring-security-的完整集成)
6. [与 Spring AI 的流式协同](#6-与-spring-ai-的流式协同)
7. [RSocket 与备选](#7-rsocket-与备选)
8. [五个常见坑](#8-五个常见坑)
9. [练习 5 题](#9-练习-5-题)

---

## 1. Spring Messaging 抽象

**Spring 的 WebSocket 能力建立在统一的 Messaging 抽象上**：**Message**（消息：headers + payload）、**MessageChannel**（发送管道）、**MessageHandler**（处理端点）、**MessageBroker**（路由中枢）——"**Spring 的实时消息是'管道 + 端点'架构，STOMP 只是传输形态**"。**客户端 → 服务端的路径**：`/app/xx` 消息进 **clientInboundChannel** → 分发给 @MessageMapping 方法 → 返回值经 **brokerChannel** → 广播到订阅者。**理解这层抽象的工程价值**：**加拦截器（ChannelInterceptor）可以做鉴权/日志/限流**（07 篇的 STOMP 层鉴权就挂在通道上）——"**通道是横切逻辑的挂点：鉴权、限流、审计都在通道拦截器里**"；**出站通道（clientOutboundChannel）拦截器可做消息审计**——"出站也留痕"。

## 2. STOMP 帧协议深潜

**STOMP（Simple Text Oriented Messaging Protocol）是文本帧协议**，四个核心帧：

```text
CONNECT    客户端 → 服务端：登录（含心跳参数与 token）
CONNECTED  服务端 → 客户端：确认（含 session id）
SUBSCRIBE  客户端 → 服务端：订阅目标（/topic/xx、/user/queue/xx）
SEND       客户端 → 服务端：发消息（目标 /app/xx）
MESSAGE    服务端 → 客户端：推送（对应订阅目标）
```

**三个机制细节**：其一，**目标地址（destination）是路由键**——服务端按前缀路由：**/app 进应用方法、/topic 与 /queue 进 broker**（03 篇三件套的协议面）——"**前缀即路由：进走 /app，出走 /topic /queue**"；其二，**/user 前缀是"个性化路由"**——服务端把 `/user/queue/x` 解析成"目标用户的实际会话"（`/user/{username}/queue/x`）——**convertAndSendToUser 就是干这个的**（03 篇）；其三，**ack 机制**——客户端可显式确认（`ack: client`）——**"需要可靠送达的消息用客户端 ack，普通推送不需要"**（04 篇可靠性的协议层实现）。

**帧格式示例**（SEND 帧的文本形态）：`SEND\ndestination:/app/chat.send\ncontent-type:application/json\n\n{"content":"hi"}\x00`——**STOMP 是纯文本协议，可读可调试**（wscat/命令行直接手敲帧与服务器对话，01 篇"协议可读"的实战价值）。

## 3. 消息路由与类型化

**Spring 的消息处理是"类型化"的**——@MessageMapping 方法的参数自动反序列化：

```java
@MessageMapping("/chat.send")
@SendTo("/topic/public")
public ChatMessage send(ChatMessage msg) {      // 自动 JSON → ChatMessage
    return msg;                                  // 返回值自动 → JSON 推送
}

// 带路径变量与头部
@MessageMapping("/room/{id}/join")
public void join(@DestinationVariable String id,
                 @Header("simpUser") User user) { ... }
```

**类型化的三个好处**：**编译期类型安全**（消息结构 = Java 类，不用手撕 JSON）；**校验在入口**（反序列化即校验——07 篇内容防护的落点，**用 Bean Validation 注解约束字段**）；**可读性**（消息结构有类型有文档）。**路由进阶**：**@SendToUser**（等价 convertAndSendToUser 的注解版——**返回值自动推给发送者本人**）、**返回 void + SimpMessagingTemplate 手动推**（灵活控制）、**@SubscribeMapping**（**订阅时立即回一条**——"刚订阅就收到当前状态"的场景，如进聊天室先拉历史）。**"类型化 + 注解路由"是 Spring 实时消息与裸 STOMP 的分水岭**。

## 4. 用户会话与个性化推送

**用户会话体系三件套**：**SimpUserRegistry**（在线用户注册表——**谁在线、连了几个会话**，06 篇连接注册表的 Spring 版）；**SimpMessagingTemplate.convertAndSendToUser**（按用户名推——**目标会话是"该用户的所有连接"**，多端登录一次推全部）；**SessionDisconnectEvent**（断开清理——**用户退出时从 registry 移除**，03 篇事件监听）。**个性化推送的工程姿势**：

```java
// 按用户推（订单/私信）
template.convertAndSendToUser(userId, "/queue/notify", payload);

// 按会话推（只推某个设备——多端登录只通知当前设备）
template.convertAndSendToSession(sessionId, "/queue/device", payload);

// 在线判断（SimpUserRegistry 查）
boolean online = userRegistry.getUser(userId) != null;
```

**注意**：**convertAndSendToUser 目标用户不在本实例 = 无感跳过**——**多实例要配合 06 篇的广播**（消息先广播到所有实例，每实例只推自己手上的连接）——"**SimpUserRegistry 是本机视图，全局视图靠连接注册表**"（06 篇第 5 节）。

## 5. 与 Spring Security 的完整集成

**完整集成三层**（07 篇的机制层）：**① 握手层**——`HandshakeInterceptor` 验 token 并注入用户（`attributes.put("user", principal)`）；**② 通道层**——`ChannelInterceptor` 在 STOMP CONNECT/SUBSCRIBE 帧上做授权（**@EnableWebSocketSecurity 的 MessageMatcher 规则**，07 篇第 5 节）；**③ 应用层**——@MessageMapping 方法里 `@AuthenticationPrincipal` 取当前用户（业务按用户处理）。**三层分工**：**握手验"能不能连"、通道验"能不能订阅"、应用层做"业务权限"**——"**三层防线各管一段：连接、订阅、操作**"。**Boot 4 注意**：@EnableWebSocketSecurity 取代旧绑定（07 篇）——**升级后三层配置都要重新验证**。

## 6. 与 Spring AI 的流式协同

**2026 年 Spring AI（`../../06-Spring全家桶/Spring AI/` 交叉）与实时通信的配合**：**SSE 管"模型说话"、WebSocket 管"双向协作"**（05 篇组合一）：

```java
// 姿势一：SSE 流式（模型输出 → 前端逐 token 显示）
@GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<String> chatStream(@RequestParam String q) {
    return chatClient.prompt(q).stream().content();   // Flux → SSE

// 姿势二：WebSocket 推"AI 事件"（任务进度/工具调用状态 → 实时通知）
template.convertAndSendToUser(userId, "/queue/ai", Map.of(
    "event", "tool_call", "tool", "search", "status", "RUNNING"));
```

**分工原则**：**token 文本流走 SSE**（浏览器 EventSource 原生消费、断点续传）；**结构化事件（工具调用/进度/状态）走 WebSocket**（双向 + 结构化推送）——"**AI 应用的实时双通道：SSE 传文字，WS 传事件**"。

## 7. RSocket 与备选

**RSocket**（Spring 支持的响应式协议）：**四模型**（request-response/request-stream/fire-and-forget/channel 双向流）、**二进制 + 背压**、**基于 TCP/WebSocket/HTTP**——"**RSocket 是'协议级'的响应式通信，比 WebSocket 重、比 REST 灵活**"。**2026 定位**：**Spring 生态里的专业件**——**需要背压的响应式数据流**（传感器/金融行情）用它；**通用实时通信（聊天/推送）WebSocket + STOMP 仍是默认**——"**RSocket 是特种部队，WebSocket 是常规军**"（Boot 4.0.5 还修了个 RSocket 与 WebSocket 端点重复的 bug，01 篇基线）。**备选全景**：Socket.IO（Node 生态）、SignalR（.NET 生态）——**Java/Spring 生态的答案是 WebSocket + STOMP**。

## 8. 五个常见坑

- **坑一**：以为 STOMP 是 Spring 私有协议——**它是开放协议（文本帧），Spring 只是实现之一**；前端 @stomp/stompjs 是通用客户端（02 篇）；
- **坑二**：convertAndSendToUser 在多实例下"用户明明在线却收不到"——**用户连在别的实例；先广播再本地推**（06 篇）；
- **坑三**：消息参数不类型化（Map 裸传）——**校验与可读性全丢；定义消息类 + Bean Validation**（第 3 节）；
- **坑四**：SimpUserRegistry 当全局在线表——**它是本机视图；全局用 Redis 注册表**（第 4 节）；
- **坑五**：SSE 与 WS 混用不分层——**token 流与事件流走错通道；按"文字走 SSE、事件走 WS"分工**（第 6 节）。

## 9. 练习 5 题

1. Spring Messaging 四件套？"通道是横切逻辑的挂点"指什么？
2. STOMP 五个核心帧？/user 前缀的个性化路由怎么工作？
3. 类型化消息的三个好处？@SubscribeMapping 的场景？
4. SimpUserRegistry 与连接注册表的分工？
5. RSocket 与 WebSocket 的定位？AI 双通道分工？

> 🎯 **核心要点**：Spring 生态 = **Messaging 抽象（通道拦截器挂鉴权/限流）+ STOMP 帧协议（/app 进 /topic /queue 出 /user 个性化）+ 类型化消息（反序列化即校验）**——"三层防线各管一段：握手验连接、通道验订阅、应用做权限"；**AI 时代：SSE 传文字、WS 传事件**。

---

**下一模块**：[10-生产实战与自测.md](10-生产实战与自测.md) / **返回总览**：[00-WebSocket总览.md](00-WebSocket总览.md)
