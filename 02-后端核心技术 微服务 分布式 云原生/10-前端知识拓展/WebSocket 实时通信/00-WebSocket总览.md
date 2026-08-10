# 00 - WebSocket 总览

> 定位：全双工实时通信的事实标准——"HTTP 是'请求-响应'单行道，WebSocket 是'全双工'双向通道——2026 年 RFC 6455 仍是唯一生产就绪的双向实时协议（浏览器支持 99.84%），AI 时代双向协作靠它、单向流式靠 SSE"

---

## 📚 目录

1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [与主体系的分工](#3-与主体系的分工)
4. [学习路线推荐](#4-学习路线推荐)
5. [核心概念速查](#5-核心概念速查)
6. [常见误区](#6-常见误区)
7. [一周学习计划示例](#7-一周学习计划示例)
8. [快速自测 10 题](#8-快速自测-10-题)
9. [参考来源](#9-参考来源)

---

## 1. 知识体系导图

```text
WebSocket 实时通信（本体系 11 篇——10-前端知识拓展）
├── 认知层：01 协议与握手原理（RFC 6455/握手/帧格式）
│          02 前端 WebSocket 实战（浏览器 API/STOMP 前端）
├── 后端层：03 SpringBoot 实战（原生 Handler/STOMP/聊天室）
│          09 Spring 生态集成（Messaging 抽象/安全集成）
├── 工程层：04 心跳保活与断线重连（PING/PONG/指数退避）
│          05 方案对比与选型（轮询/SSE/WS/WebTransport）
│          06 分布式与集群方案（Redis Pub/Sub/MQ/网关）
│          07 安全与鉴权（wss/Origin/token/CSWSH）
│          08 生产实践与性能（连接管理/监控/容量）
└── 验收层：10 生产实战与自测（聊天室+推送+集群 + 20 题）
```

## 2. 模块导航

| 篇 | 模块 | 核心内容 | 核心产出 |
|:---:|------|---------|---------|
| 00 | 总览 | 导图/分工/路线/速查/误区 | 学习计划 |
| 01 | 协议与握手 | RFC 6455/握手/帧格式/子协议 | 懂协议 |
| 02 | 前端实战 | 浏览器 API/心跳重连封装/STOMP | 会前端 |
| 03 | SpringBoot 实战 | Handler/STOMP/聊天室/推送 | 会后端 |
| 04 | 心跳与重连 | PING/PONG/指数退避/去重 | 会保活 |
| 05 | 方案对比选型 | 轮询/SSE/WS/WebTransport | 会选型 |
| 06 | 分布式集群 | Redis Pub/Sub/MQ/网关转发 | 会集群 |
| 07 | 安全鉴权 | wss/Origin/token/CSWSH 防护 | 会加固 |
| 08 | 生产实践 | 连接管理/监控/容量/排障 | 会生产 |
| 09 | Spring 生态进阶 | Messaging 抽象/STOMP 深潜/安全集成 | 会进阶 |
| 10 | 实战与自测 | 三合一项目 + 20 题 | 毕业产出 |

## 3. 与主体系的分工

**与 Web 高阶知识（`../../09-Web开发全流程/Web 高阶知识/06-实时通信WebSocket与SSE.md`）的分工**：那个体系从**浏览器与 HTTP 视角**讲实时通信（演进/握手/帧/对比），**本体系从"前后端工程实现"视角讲全链路**——协议原理 + 前端 API + SpringBoot 后端 + 分布式与生产——"**概念看 Web 高阶知识，工程落地看本体系**"。

**与 SpringBoot Web（`../../09-Web开发全流程/SpringBoot Web/00-...`）、Spring Cloud Gateway 的分工**：WebSocket 不是 REST 的替代而是补充——**REST 管"查询与变更"，WebSocket 管"推送与协作"**；网关的 WebSocket 转发（Upgrade 支持）在 06 篇详讲。

**与 Nginx（`../../07-工程运维基础/运维/Nginx 基础/03-反向代理配置.md`）的分工**：Nginx 反代 WebSocket 的**三要素**（Upgrade/Connection 头转发 + 超时配置）是生产部署的必经环节（06/08 篇）。

**与同级前端体系的分工**：JavaScript（`../JavaScript/00-JavaScript总览与核心概念.md`）管语法基础，Node.JS（`../Node.JS/00-Node.js知识体系总览.md`）管 Node 侧服务，**本体系管"实时通信这个具体场景"**；Token（`../Token/00-Token总览.md`）的 JWT 是 WebSocket 鉴权的常用载体（07 篇）。

**2026-08 基线**：**RFC 6455（2011-12）仍是唯一生产就绪的双向实时标准**——浏览器支持 99.84%、服务器/代理/CDN 全兼容（聊天/协作/游戏/通知的事实标准）；扩展标准：RFC 7692（permessage-deflate 压缩）、RFC 8441（HTTP/2 扩展 CONNECT，实验）、RFC 9220（HTTP/3/QUIC，早期实验）；**WebTransport（QUIC 原生）是未来方向但未生产就绪**（浏览器 ~75% 仅 Chrome/Edge、服务器实现极少——预计 2027 早期采用、2028 主流）；**AI 时代的分工已确立**：**双向协作（人 + Agent 同文档编辑）用 WebSocket，单向 LLM token 流式用 SSE**（OpenAI/Anthropic/Gemini 全系 SSE）；**Spring Boot 4.0.5（2026-03-26）修复了两个 WebSocket/STOMP bug**（Jackson 无 ObjectMapper bean 启动失败、任务执行器仅随 Jackson 自动配置），**@EnableWebSocketSecurity 取代旧安全组件模式**——"WebSocket 二十年稳定，2026 年依然是实时通信的默认答案"。

## 4. 学习路线推荐

**路线一：标准路线（3-5 天）**——01 → 10 逐篇 + 每篇跑代码——**毕业标准：独立完成"聊天室 + 实时通知 + 分布式同步"三合一项目**。

**路线二：后端 Java 路线（2-3 天）**——01 → 03 → 06 → 07 → 09——适合 Spring 开发者最快落地——**"协议看一眼 01，其余精力全在 SpringBoot 三件套（Handler/STOMP/SimpMessagingTemplate）"**。

**路线三：前端视角路线**——02 → 04 → 05 精读——前端工程师把"连接管理"吃透——**"浏览器 API 三行能用，心跳重连才是工程分水岭"**。

## 5. 核心概念速查

| 概念 | 一句话 | 对应篇 |
|------|--------|:---:|
| 全双工 | 同一连接双向同时传输（HTTP 只能一来一回） | 01 |
| 握手 | HTTP Upgrade → 101 Switching Protocols | 01 |
| Sec-WebSocket-Key | 握手密钥：Key → SHA1 → Accept 验证 | 01 |
| 帧 | 传输单元：文本/二进制/PING/PONG/关闭 | 01 |
| STOMP | WebSocket 之上的消息子协议（Topic/Queue 语义） | 03 |
| SimpMessagingTemplate | Spring 推送封装（convertAndSendToUser） | 03 |
| PING/PONG | 心跳帧：保活与探测死连接 | 04 |
| SSE | 单向服务端推送（EventSource/LLM 流式主场） | 05 |
| WebTransport | QUIC 原生未来协议（2026 未生产就绪） | 05 |
| Redis Pub/Sub | 集群同步最简单方案 | 06 |
| CSWSH | 跨站点 WebSocket 劫持（Origin 校验防） | 07 |
| wss:// | TLS 加密的 WebSocket（生产必用） | 07 |

## 6. 常见误区

**误区一：WebSocket 是 HTTP 的替代**——它是 HTTP 的**升级补充**：握手本身就是 HTTP 请求（Upgrade），REST 管查询变更、WS 管推送协作——"**两个协议，一个端口，各司其职**"（01 篇）。

**误区二：连上就能一直用，不用管**——**连接会死**：网络切换、空闲超时、服务器重启——**心跳 + 重连是工程必需品不是可选优化**（04 篇）。

**误区三：推送一律用 WebSocket**——**单向推送（通知/日志/LLM 流式）SSE 更合适**：内置自动重连、轻量、代理友好——"**双向用 WS，单向用 SSE**"是 2026 的默认答案（05 篇）。

**误区四：分布式=每台服务器各连各的**——**用户连 Server-1，消息在 Server-2 进不来**——必须做连接注册表 + 广播同步（Redis Pub/Sub/MQ）（06 篇）。

**误区五：鉴权在连接后做**——**握手阶段就要鉴权**：连上了再踢 = 浪费 + 攻击面；token 在握手头/query 里验（07 篇）。

**误区六：生产用 ws:// 就行**——**wss://（TLS）是生产标配**：浏览器混合内容限制 + 加密——"HTTP 用 https，WS 就用 wss"（07 篇）。

**误区七：连接数随便开**——**每个连接占内存（KB 级）与线程**——100 万连接是大事；容量规划与连接数限制是生产纪律（08 篇）。

## 7. 一周学习计划示例

| 天 | 内容 | 动手任务 |
|:---:|------|---------|
| 1 | 01 | 用 wscat/浏览器连一个公共 WS 服务看握手 |
| 2 | 02 | 原生 API 写聊天页面 + 心跳重连封装 |
| 3 | 03 | SpringBoot STOMP 聊天室（前后端联调） |
| 4 | 04 + 05 | 断网重连实验；SSE vs WS 双实现对比 |
| 5 | 06 | 双实例 + Redis Pub/Sub 验证广播 |
| 6 | 07 + 08 | token 鉴权 + 连接监控指标 |
| 7 | 10 自测 + 面试 | 三合一项目 + 20 题 |

## 8. 快速自测 10 题

1. WebSocket 与 HTTP 的本质区别？握手返回什么状态码？
2. 握手的关键头有哪些？Sec-WebSocket-Accept 怎么算的？
3. 帧的类型有哪些？掩码机制解决什么问题？
4. 前端 WebSocket 的四个事件？心跳重连怎么设计？
5. STOMP 是什么？/topic 与 /queue 的区别？
6. SSE 与 WebSocket 的选型？2026 AI 时代的分工？
7. 分布式场景怎么让消息找到用户？三种方案？
8. WebSocket 鉴权的三个时机？CSWSH 攻击怎么防？
9. 一个连接占多少资源？100 万连接意味着什么？
10. 2026 年 WebSocket 的标准状态？WebTransport 能用了吗？

## 9. 参考来源

- [RFC 6455：The WebSocket Protocol（2011-12 事实标准）](https://datatracker.ietf.org/doc/html/rfc6455)
- [MDN WebSocket API 文档（浏览器侧完整参考）](https://developer.mozilla.org/en-US/docs/Web/API/WebSocket)
- [WebSocket vs WebTransport 对比（2026 采用时间线）](https://websocket.org/comparisons/webtransport/)
- [SSE vs WebSockets vs Long Polling 2026 开发者指南](https://alldevtoolshub.com/blog/server-sent-events-vs-websockets-vs-long-polling/)
- [Spring Boot 4.0.5 Release（WebSocket/STOMP 修复 #49749/#49753）](https://java.libhunt.com/spring-boot-changelog/4.0.5)
- [Spring Messaging 官方文档（STOMP/SimpMessagingTemplate）](https://docs.spring.io/spring-framework/reference/web/websocket.html)

---

**下一模块**：[01-WebSocket协议与握手原理.md](01-WebSocket协议与握手原理.md)
