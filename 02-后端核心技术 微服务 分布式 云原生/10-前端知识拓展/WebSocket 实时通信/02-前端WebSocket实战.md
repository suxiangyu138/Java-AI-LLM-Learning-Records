# 02 - 前端 WebSocket 实战

> 定位：浏览器侧的实时通信——原生 API 五分钟能用，心跳重连才是工程分水岭——"四个事件 + 一个 send 是入门，连接生命周期管理（重连/去重/保活）是进阶"

---

## 📚 目录

1. [原生 WebSocket API](#1-原生-websocket-api)
2. [连接生命周期](#2-连接生命周期)
3. [心跳与重连封装](#3-心跳与重连封装)
4. [消息协议设计](#4-消息协议设计)
5. [STOMP 前端：@stomp/stompjs](#5-stomp-前端stompstompjs)
6. [与 Vue / React 集成](#6-与-vue--react-集成)
7. [前端常见坑](#7-前端常见坑)
8. [练习 5 题](#8-练习-5-题)

---

## 1. 原生 WebSocket API

```javascript
// 三行入门
const ws = new WebSocket('wss://api.example.com/ws/chat');   // wss 生产必用
ws.onopen    = () => ws.send(JSON.stringify({ type: 'hello' }));
ws.onmessage = (e) => console.log('收到:', JSON.parse(e.data));
ws.onclose   = () => console.log('连接关闭');
ws.onerror   = (e) => console.log('错误:', e);
```

**五个关键点**：其一，**构造即连接**——`new WebSocket(url)` 立即发起握手（**握手头无法自定义，token 只能放 query 或子协议里**，07 篇鉴权）；其二，**四个事件**——onopen/onmessage/onclose/onerror 是全部交互点（**onerror 之后必跟 onclose，错误处理写 onclose 就够了**）；其三，**send 只能发字符串或二进制**——结构化消息自己 JSON.stringify（第 4 节协议设计）；其四，**readyState 三态**——CONNECTING(0)/OPEN(1)/CLOSED(3)——**send 前检查 readyState===1**（CLOSING 态 send 会抛错）；其五，**收到即"新鲜"**——onmessage 触发时数据已在内存，**大消息注意内存**（服务端分片/压缩，01 篇帧格式）。

**二进制消息**：`ws.send(arrayBuffer)` / `ws.send(blob)` 发送二进制（01 篇 0x2 二进制帧）——**音频、图片、协议数据走二进制**（省 JSON 编码开销）；**接收端注意 e.data 类型**——文本帧是 string、二进制帧是 Blob（`await e.data.arrayBuffer()` 转回）——"**按 opcode 分类型处理，别假设都是字符串**"。

## 2. 连接生命周期

**一条连接的一生：构造（握手）→ OPEN（收发）→ 关闭（close() 或异常）**。前端要管理的四个阶段：

**连接中**：`readyState===0` 时用户点击发送——**要么排队，要么提示"连接中"**（别静默丢消息）；**打开中**：**心跳保活**（第 3 节）；**关闭时**：**区分"主动关"与"被动断"**——主动关（用户退出）`ws.close(1000)` 不重连；被动断（网络/服务端）**自动重连**；**关闭后**：**清定时器、标记状态**——"**每个阶段都有对应的状态机代码，连上/断开/重连都要有明确处理**"——这就是"连接管理"的含义（04 篇机制详讲）。

**连接管理的测试**：浏览器 DevTools 的 Network → WS 面板**可直接看到帧与心跳**；DevTools offline 模拟断网验证重连逻辑——"**连接管理是唯一需要'故意断网'来测试的代码**"。

## 3. 心跳与重连封装

```javascript
class ReconnectWS {
  constructor(url, { heartbeat = 30000, maxRetry = 5 } = {}) {
    this.url = url; this.maxRetry = maxRetry; this.retry = 0;
    this.connect();
  }
  connect() {
    this.ws = new WebSocket(this.url);
    this.ws.onopen = () => { this.retry = 0; this.startHeartbeat(); };
    this.ws.onmessage = (e) => {
      const msg = JSON.parse(e.data);
      if (msg.type === 'pong') { this.lastPong = Date.now(); return; }  // 心跳应答
      this.onmessage?.(msg);
    };
    this.ws.onclose = () => { this.stopHeartbeat(); this.scheduleReconnect(); };
  }
  startHeartbeat() {
    this.timer = setInterval(() => {                 // 每 30s 发一次 ping
      if (Date.now() - this.lastPong > 60000) {      // 60s 没收到 pong → 认为死连接
        this.ws.close();                              // 主动关闭 → 触发重连
        return;
      }
      this.ws.send(JSON.stringify({ type: 'ping' }));
    }, this.heartbeat);
  }
  scheduleReconnect() {
    if (this.retry >= this.maxRetry) { console.log('重连次数超限'); return; }
    const delay = Math.min(1000 * 2 ** this.retry++, 30000);   // 指数退避
    setTimeout(() => this.connect(), delay);
  }
}
```

**封装三件套**：**心跳**——定时发 ping、记录 pong 时间、超时主动 close（让死连接走重连流程，04 篇机制）；**指数退避**——1s/2s/4s/8s…封顶 30s（**别 1s 一次疯狂重连，也别退避到"用户等十分钟"**）；**重连次数上限**——超限提示用户手动刷新。**消息去重**：重连后可能重复收到消息——**消息带递增序号，前端按序号去重**（04 篇详讲）。**留痕**：心跳与重连的关键动作 console.info 记录——**调试与线上复盘都靠它**。

## 4. 消息协议设计

```javascript
// 统一消息信封（前后端约定，03 篇后端同构）
{ "type": "chat",     "data": { "from": "u1", "content": "hello" } }
{ "type": "pong",     "data": null }            // 心跳应答
{ "type": "notify",   "data": { "orderId": "A1024", "status": "PAID" } }
```

**协议设计三原则**：其一，**type 字段分派**——`switch (msg.type)` 分发到处理器（"**消息信封化：type 是路由键，data 是载荷**"）；其二，**服务端推送的消息与请求响应分离**——推送消息带 `id`（去重用，第 3 节）；其三，**错误消息有结构**——服务端推 `{type:"error", code, message}`，前端统一提示——**"协议先定信封，再定 type 字典——前后端各留一份，别让消息格式长成野草"**。

**序号与去重**（与 04 篇第 6 节配套的前端实现）：服务端消息带递增 `seq`——**前端维护 lastSeq：重复的 seq 直接跳过、出现 gap 时向服务端补拉**——"**重连后的重复与缺失，靠序号一个字段全解决**"。

## 5. STOMP 前端：@stomp/stompjs

```javascript
import { Client } from '@stomp/stompjs';

const client = new Client({
  brokerURL: 'wss://api.example.com/ws',        // 直连（现代浏览器）
  connectHeaders: { Authorization: 'Bearer ' + token },  // 握手头鉴权
  reconnectDelay: 5000,                          // 内置自动重连（指数退避）
  heartbeatIncoming: 10000, heartbeatOutgoing: 10000,    // 协议级心跳
  onConnect: () => {
    client.subscribe('/topic/public', msg => console.log('公告:', JSON.parse(msg.body)));
    client.subscribe('/user/queue/orders', msg => console.log('订单:', JSON.parse(msg.body)));
    client.publish({ destination: '/app/chat.send', body: JSON.stringify({ content: 'hi' }) });
  },
});
client.activate();
```

**STOMP 前端解决了裸 WS 的三件事**：**订阅语义**（/topic 广播、/user/queue 个人频道——路由由协议承担，不用自己写 type 分派）、**自动重连**（reconnectDelay 内置）、**心跳**（heartbeatIncoming/Outgoing 协议级）——"**裸 WS 要手写的东西，STOMP 前端库全内置**"。**注意**：connectHeaders 走的是 STOMP 握手（CONNECT 帧），**不是** HTTP Upgrade 头——网关/代理层的鉴权（07 篇）与 STOMP 层鉴权是两件事，别混淆。**SockJS 回退**：企业代理剥 Upgrade 头时（04 篇第 7 节），前端把 brokerURL 换成 `webSocketFactory: () => new SockJS('/ws')`——**自动降级长轮询**，业务代码不变。**订阅管理**：subscribe 返回带 id，**取消订阅用 `client.unsubscribe(id)`**（组件卸载时清理订阅，与关闭连接配套）。

## 6. 与 Vue / React 集成

```javascript
// Vue3：组合式封装（连接状态驱动 UI）
import { ref, onMounted, onUnmounted } from 'vue';
export function useWebSocket(url) {
  const status = ref('connecting');        // connecting/open/closed
  const messages = ref([]);
  let ws;
  onMounted(() => {
    ws = new ReconnectWS(url);
    ws.onmessage = (m) => messages.value.push(m);
    // 状态驱动：status 变化 → 顶部"连接中/已断开"横幅
  });
  onUnmounted(() => ws?.close());
  return { status, messages, send: (m) => ws?.send(JSON.stringify(m)) };
}
```

**框架集成三原则**：其一，**生命周期绑定**——`onMounted` 连接、`onUnmounted` 关闭（**不关 = 页面切走连接还挂着，内存泄漏 + 服务端僵尸连接**，08 篇）；其二，**状态进响应式**——连接状态驱动 UI（断开横幅/重连提示），**别把 WS 事件写在组件外裸奔**；其三，**pinia/vuex 集中管理**——多组件共用一条连接（**一个页面别开多条 WS**，连接是稀缺资源）。**React 同构**：useEffect 里 connect、cleanup 里 close，context 提供连接单例——**"框架不同，生命周期绑定与单例共享的原则相同"**；**单向推送场景**（通知/日志/LLM 流式）前端用 EventSource 更省——**"双向才上 WS，单向交给 SSE"**（05 篇选型）。

## 7. 前端常见坑

- **坑一**：生产用 `ws://`——**混合内容被浏览器拦截 + 明文；一律 wss://**；
- **坑二**：send 不检查 readyState——CLOSING 态 send 抛错；**send 前查 readyState===1**；
- **坑三**：重连用固定 1s 间隔——**服务端被打挂时客户端集体疯狂重连（重连风暴）；指数退避 + 抖动**；
- **坑四**：onerror 里做重连——**onerror 后必跟 onclose；重连逻辑只写 onclose**（否则双重连）；
- **坑五**：页面卸载不 close——**僵尸连接；onUnmounted/beforeDestroy 里关**；
- **坑六**：消息没有序号——**重连后重复消息重复处理；id + 去重**；
- **坑七**：大消息一次性 JSON.parse——**慢 + 内存；分片/流式处理或后端压缩**（01 篇 permessage-deflate）；
- **坑八**：多组件各自 new WebSocket——**同页多条连接浪费资源 + 消息分散；连接集中管理（pinia/context 单例）**（第 6 节）。

## 8. 练习 5 题

1. 原生 API 的四个事件与 readyState 三态？
2. 为什么"onerror 后必跟 onclose"？重连逻辑写哪？
3. 心跳封装三件套是什么？指数退避为什么带抖动？
4. 消息信封协议的三原则？
5. STOMP 前端解决了裸 WS 的哪三件事？

> 🎯 **核心要点**：前端实战 = **四个事件 + send 入门，生命周期管理进阶**——"心跳 + 指数退避重连 + 消息去重"是工程三件套；**STOMP 前端库（@stomp/stompjs）把订阅/重连/心跳全内置**——"裸 WS 三行能用，封装三件套才是工程分水岭"。

---

**下一模块**：[03-SpringBoot-WebSocket实战.md](03-SpringBoot-WebSocket实战.md) / **返回总览**：[00-WebSocket总览.md](00-WebSocket总览.md)
