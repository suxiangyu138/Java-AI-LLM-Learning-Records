# 02 - Spring Boot WebSocket 实战

> 🎯 Spring Boot 集成 WebSocket 只需几个注解 — 从聊天室到消息推送，从单机到分布式方案

---

## 目录

1. [Spring Boot WebSocket 基础配置](#1-spring-boot-websocket-基础配置)
2. [聊天室实战](#2-聊天室实战)
3. [前端 WebSocket 代码](#3-前端-websocket-代码)
4. [分布式 WebSocket 方案](#4-分布式-websocket-方案)

---

## 1. Spring Boot WebSocket 基础配置

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```

```java
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new MyWebSocketHandler(), "/ws/chat")
            .setAllowedOrigins("*");      // 允许跨域
    }
}

public class MyWebSocketHandler extends TextWebSocketHandler {
    private static final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        System.out.println("新连接: " + session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // 广播给所有连接
        for (WebSocketSession s : sessions) {
            if (s.isOpen()) s.sendMessage(message);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
    }
}
```

### 方式2：STOMP 子协议（⭐ 推荐 — 类似 RabbitMQ 的 Topic）

```java
@Configuration
@EnableWebSocketMessageBroker
public class StompConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");   // 服务端推消息的前缀
        registry.setApplicationDestinationPrefixes("/app"); // 客户端发消息的前缀
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOrigins("*").withSockJS(); // 兼容老浏览器
    }
}
```

```java
@Controller
public class ChatController {

    @MessageMapping("/chat.send")              // 客户端发 → /app/chat.send
    @SendTo("/topic/public")                   // → 广播到 /topic/public
    public ChatMessage sendMessage(ChatMessage message) {
        return message;
    }

    @MessageMapping("/chat.private")
    public void sendPrivate(ChatMessage message, SimpMessageHeaderAccessor header) {
        String username = header.getUser().getName();
        messagingTemplate.convertAndSendToUser(
            message.getTo(), "/queue/private", message);  // 点对点
    }
}
```

---

## 2. 聊天室实战

```java
// ⭐ 监听连接/断开事件
@Component
public class WebSocketEventListener {

    @EventListener
    public void handleConnect(SessionConnectEvent event) {
        String username = event.getUser().getName();
        System.out.println(username + " 上线");
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        String username = event.getUser().getName();
        System.out.println(username + " 下线");
        // 广播下线通知
    }
}
```

```java
// ⭐ 服务端主动推送（非消息回复）
@Component
public class OrderNotifyService {
    private final SimpMessagingTemplate template;

    // 订单状态变更 → 推送给下单用户
    public void notifyOrderStatus(Long userId, Order order) {
        template.convertAndSendToUser(
            userId.toString(),
            "/queue/orders",        // → 用户私人频道
            order
        );
    }

    // 广播公告
    public void broadcast(String message) {
        template.convertAndSend("/topic/public", message);
    }
}
```

---

## 3. 前端 WebSocket 代码

```javascript
// 原生 WebSocket
const ws = new WebSocket('ws://localhost:8080/ws/chat');

ws.onopen  = () => console.log('连接成功');
ws.onmessage = (e) => console.log('收到:', JSON.parse(e.data));
ws.onclose = () => console.log('连接断开');
ws.send(JSON.stringify({ type: 'chat', content: 'Hello' }));

// ⭐ STOMP + SockJS（推荐 — 自动重连 + Topic 订阅）
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const client = new Client({
  brokerURL: 'ws://localhost:8080/ws',
  connectHeaders: { Authorization: 'Bearer ' + token },
  onConnect: () => {
    client.subscribe('/topic/public', msg => {
      console.log('公告:', JSON.parse(msg.body));
    });
    client.subscribe('/user/queue/orders', msg => {
      console.log('订单更新:', JSON.parse(msg.body));
    });
  }
});
client.activate();
```

---

## 4. 分布式 WebSocket 方案

```text
单机方案的问题：
  用户 A 连 Server-1，用户 B 连 Server-2
  → A 发消息给 B → Server-1 不知道 B 在哪

分布式方案：

方案1：Redis Pub/Sub（⭐ 最简单）
  Server-1 收到消息 → 发布到 Redis Channel "chat"
  → Server-2 订阅了 Redis Channel "chat" → 收到 → 推给 B

方案2：MQ（RocketMQ/Kafka）
  消息 → MQ Topic → 所有 Server 消费 → 在自己的连接池中查找目标用户

方案3：IP Hash 路由
  Nginx 根据 userId Hash → 同一用户永远连同一台 Server
  简单但有单点故障问题
```

```java
// Redis Pub/Sub 方案
@Component
public class RedisMessageListener implements MessageListener {

    private final SimpMessagingTemplate template;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        // 收到其他 Server 转发来的消息
        ChatMessage msg = JsonUtil.parse(message, ChatMessage.class);
        template.convertAndSendToUser(msg.getTo(), "/queue/private", msg);
    }
}
```

> 🎯 **WebSocket 三件套**：STOMP 子协议（比裸 WebSocket 更好用）、`/topic` 广播 + `/queue` 点对点、分布式用 Redis Pub/Sub 同步。Spring Boot 的 `SimpMessagingTemplate` 是对 WebSocket 的最佳封装。
