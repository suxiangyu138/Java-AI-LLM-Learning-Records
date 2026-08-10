# 03 - Spring Boot WebSocket 实战

> 定位：后端实时通信的 Spring 落地——"Spring Boot 集成 WebSocket 只需几个注解：原生 Handler 打底、STOMP 是推荐姿势（类似 RabbitMQ 的 Topic）、SimpMessagingTemplate 是推送封装"

---

## 📚 目录

1. [依赖与两种姿势](#1-依赖与两种姿势)
2. [姿势一：原生 WebSocketHandler](#2-姿势一原生-websockethandler)
3. [姿势二：STOMP 子协议（⭐ 推荐）](#3-姿势二stomp-子协议-推荐)
4. [聊天室实战：事件监听](#4-聊天室实战事件监听)
5. [服务端主动推送](#5-服务端主动推送)
6. [Boot 4 的变化](#6-boot-4-的变化)
7. [五个常见坑](#7-五个常见坑)
8. [练习 5 题](#8-练习-5-题)

---

## 1. 依赖与两种姿势

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```

**Spring 提供两种姿势**：**姿势一（原生 Handler）**——`@EnableWebSocket` + 实现 WebSocketHandler——**裸协议、自己管理会话**，适合"协议自己定、消息简单"的场景；**姿势二（STOMP 子协议，⭐ 推荐）**——`@EnableWebSocketMessageBroker` + `@MessageMapping` 注解——**框架帮你做路由/广播/点对点**（类似 RabbitMQ 的 Topic/Queue 语义）——"**原生是砖头，STOMP 是框架——业务开发默认 STOMP，只有特殊协议需求才裸写 Handler**"。**Boot 4 注意**：4.0.5 修复了"Jackson 相关配置缺失导致 WebSocket 启动失败"的问题——**升级到 4.0.5+ 或配好 ObjectMapper bean**（第 6 节）。**版本基线**：Boot 4 需要 **JDK 21+**；starter 坐标与 Boot 3 相同——老教程的配置类写法（@EnableWebSocketMessageBroker 等注解）基本兼容，**安全配置除外**（@EnableWebSocketSecurity 迁移，第 6 节）；**老项目 Boot 2 → Boot 4 升级**：先查 WebSocket 安全配置与消息转换器（Jackson/Gson），再跑 STOMP 连接回归（第 6 节检查单）。

## 2. 姿势一：原生 WebSocketHandler

```java
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new MyWebSocketHandler(), "/ws/chat")
            .setAllowedOrigins("*")        // 跨域（生产按域名白名单，07 篇）
            .addInterceptors(new TokenAuthInterceptor());   // 握手拦截器鉴权（07 篇）
    }
}

public class MyWebSocketHandler extends TextWebSocketHandler {
    private static final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);                       // 会话登记（06 篇连接注册表雏形）
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        for (WebSocketSession s : sessions) {        // 广播给所有连接
            if (s.isOpen()) s.sendMessage(message);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
    }
}
```

**四个要点**：其一，**sessions 集合是"连接注册表"**——多实例部署时它只是本机视图（06 篇分布式）；其二，**isOpen() 检查再 send**——连接已关还 send 会抛异常；其三，**addInterceptors 是握手鉴权钩子**（07 篇）；其四，**并发注意**——sessions 用并发集合（ConcurrentHashMap.newKeySet()）——"**原生 Handler 的全部管理（注册/广播/移除）都要自己写，这就是为什么不推荐**"。

**会话的四个常用操作**：`getAttributes()`（握手拦截器存的数据——用户信息）、`isOpen()`（发送前检查）、`close(CloseStatus)`（主动关闭——**踢人下线 = 找到 session 调 close(1008)**，07 篇）、`getRemoteAddress()`（来源 IP——日志/风控）——"**会话对象是连接的服务端身份证**"。

## 3. 姿势二：STOMP 子协议（⭐ 推荐）

```java
@Configuration
@EnableWebSocketMessageBroker
public class StompConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");     // 服务端推送的前缀
        registry.setApplicationDestinationPrefixes("/app");  // 客户端发消息的前缀
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOrigins("*")
                .withSockJS();          // SockJS 回退（老浏览器/代理兼容，04 篇）
    }
}

@Controller
public class ChatController {

    @MessageMapping("/chat.send")          // 客户端发 → /app/chat.send
    @SendTo("/topic/public")               // 广播到 /topic/public（所有订阅者）
    public ChatMessage sendMessage(ChatMessage message) { return message; }

    @MessageMapping("/chat.private")
    public void sendPrivate(ChatMessage message, SimpMessageHeaderAccessor header) {
        messagingTemplate.convertAndSendToUser(
            message.getTo(), "/queue/private", message);   // 点对点（用户专属频道）
    }
}
```

**STOMP 三件套认知**：**前缀分工**——`/topic` 广播（所有人）、`/queue` 点对点（个人频道，`/user/queue/xx` 自动带用户前缀）、`/app` 是"客户端发进来的门"——"**进走 /app，出走 /topic 与 /queue，双向路由一目了然**"；**@MessageMapping 注解即路由**——Spring 自动把 `/app/chat.send` 映射到方法；**convertAndSendToUser 是点对点推送的核心 API**（第 5 节详讲）。**线程模型**：@MessageMapping 方法默认在**消息线程池**执行——**耗时操作（调外部 API/查库）要 @Async 异步化，别阻塞消息通道**（阻塞 = 全体消息排队变慢，08 篇延迟排查）。

## 4. 聊天室实战：事件监听

```java
@Component
public class WebSocketEventListener {

    @EventListener
    public void handleConnect(SessionConnectEvent event) {
        String username = event.getUser().getName();   // 握手时存的用户（07 篇鉴权）
        messagingTemplate.convertAndSend("/topic/public", username + " 上线");
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        String username = event.getUser().getName();
        // 广播下线通知 + 从在线列表移除
    }
}
```

**事件监听是"连接状态 → 业务动作"的桥梁**：连接/断开事件 → 上线/下线广播、在线列表维护、会话清理——**"聊天室不只是收发消息，上下线事件是体验的一半"**。**注意**：SessionConnectEvent 在 STOMP CONNECT 成功后触发（鉴权也在 CONNECT 时做，07 篇）——**"先鉴权后广播"的顺序由 ChannelInterceptor 保证**（09 篇安全集成）。**在线列表维护**：`ConcurrentHashMap<username, session>` 随上下线事件增删，**列表变化再广播**——"**在线状态是聊天室的核心业务数据**"（06 篇连接注册表的雏形）。**SSE 的 Spring 支持**：通知类单向推送可用 SseEmitter/Flux 实现——"**聊天类双向用 STOMP，通知类单向用 SSE**"（05 篇选型）。**事件埋点**：SessionConnectedEvent/DisconnectEvent 也是**连接监控的埋点位置**（08 篇五指标的上线/下线计数）。

## 5. 服务端主动推送

```java
@Component
public class OrderNotifyService {
    private final SimpMessagingTemplate template;

    // 订单状态变更 → 推送给下单用户（业务代码里任意位置调用）
    public void notifyOrderStatus(Long userId, Order order) {
        template.convertAndSendToUser(
            userId.toString(),
            "/queue/orders",        // 前端订阅 /user/queue/orders（02 篇）
            order
        );
    }

    // 广播公告
    public void broadcast(String message) {
        template.convertAndSend("/topic/public", message);
    }
}
```

**SimpMessagingTemplate 是 Spring 对 WebSocket 的最佳封装**：**convertAndSendToUser（点对点）/ convertAndSend（广播）两个 API 覆盖 90% 推送需求**——而且**可以在任何地方调用**（Service/定时任务/消息监听器），不限于 @MessageMapping 方法——"**业务事件（订单状态变、库存告警）→ 调 template 推给相关用户**"是实时通知的标准姿势。**convertAndSendToUser 的原理**：目标变成 `/user/{userId}/queue/orders`，**用户前缀由会话关联的用户名解析**——**先鉴权（有用户名）才能点对点推**（07/09 篇）。**推送与业务解耦的设计**：业务 Service 只发**领域事件**（Spring 事件机制），推送监听器接收后调 template——"**业务不感知实时通道，实时通道不侵入业务**"——订单状态变 → 发事件 → 监听器推通知，链路清晰可测。

## 6. Boot 4 的变化

**2026-03-26 发布的 Spring Boot 4.0.5 修复了两个 WebSocket/STOMP 问题**：其一，**Jackson 在 classpath 但没有 ObjectMapper bean 时启动失败**（#49749）——升级 4.0.5+ 自动处理，否则显式提供 ObjectMapper；其二，**WebSocket 任务执行器只随 Jackson 自动配置**（#49753）——用 Gson 等其他 JSON 库的应用配置不完整——升级修复。**安全配置的变化**：**@EnableWebSocketSecurity 取代旧的 Spring Security 组件绑定模式**（STOMP 安全规则独立配置，07/09 篇）——**升级 Boot 4 时 WebSocket 安全配置要重新验证**（迁移检查单：启动正常/STOMP 连接/JSON 序列化/线程池生效/错误处理场景）。

## 7. 五个常见坑

- **坑一**：`setAllowedOrigins("*")` 上生产——**任意源可连；按域名白名单**（07 篇 Origin 校验）；
- **坑二**：Handler 里直接用非并发集合——**并发连接下 ConcurrentModificationException；用并发集合**；
- **坑三**：send 前不查 isOpen()——**已关连接 send 抛异常；检查 + 清理**；
- **坑四**：点对点推送前没鉴权——**convertAndSendToUser 需要用户名；先做握手/STOMP 鉴权**（07 篇）；
- **坑五**：Boot 4 升级后 WS 启动失败/配置失效——**先看 4.0.5 修复项与 @EnableWebSocketSecurity 迁移**（第 6 节）；
- **坑六**：@SendTo 返回实体导致序列化失败（循环引用/懒加载）——**推送用专用 DTO，别直接返回 JPA 实体；@JsonIgnore 兜底**（第 3 节类型化）；
- **坑七**：聊天消息不落库——**实时通道与历史数据是两张表：实时走 WS、历史走 REST 拉取**（04 篇补拉与 05 篇组合二）；推送消息也建议落库——补拉与审计都需要。

## 8. 练习 5 题

1. Spring 的两种 WebSocket 姿势？为什么推荐 STOMP？
2. 原生 Handler 的四要点？sessions 集合是什么？
3. STOMP 三件套：/topic、/queue、/app 的分工？
4. SimpMessagingTemplate 的两个核心 API？为什么可以任意位置调用？
5. Boot 4.0.5 修复了什么？@EnableWebSocketSecurity 是什么？

> 🎯 **核心要点**：SpringBoot 实战 = **"原生 Handler 打底、STOMP 推荐（/topic 广播 + /queue 点对点 + /app 入口）、SimpMessagingTemplate 是推送封装"**——"业务事件 → template 推给相关用户"是实时通知标准姿势；**Boot 4.0.5 修了两个 STOMP bug，安全配置迁到 @EnableWebSocketSecurity**。

---

**下一模块**：[04-心跳保活与断线重连.md](04-心跳保活与断线重连.md) / **返回总览**：[00-WebSocket总览.md](00-WebSocket总览.md)
