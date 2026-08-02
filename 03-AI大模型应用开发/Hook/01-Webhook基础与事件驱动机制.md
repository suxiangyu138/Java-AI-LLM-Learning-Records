# 01 - Webhook 基础与事件驱动机制

> 🎯 Webhook = 反向 API — 系统在事件发生时主动推送数据，而非你主动去查。它是 AI 平台（Coze/Dify/n8n）与外部系统集成的核心通道。本章覆盖 Webhook 的工程全链路

---

## 目录

1. [Webhook 核心概念](#1-webhook-核心概念)
2. [与 API/Polling/WebSocket 的对比](#2-与-apipollingwebsocket-的对比)
3. [工作流程三步](#3-工作流程三步)
4. [安全四件套](#4-安全四件套)
5. [可靠性保障](#5-可靠性保障)
6. [本地调试与测试](#6-本地调试与测试)

---

## 1. Webhook 核心概念

```text
传统 API（你主动）：
  客户端 → GET /api/orders?id=123 → 服务器 → 返回订单

Webhook（系统主动）：
  系统事件发生 → 服务器 POST https://yourapp.com/webhook/order → 你的服务
```

| 术语 | 说明 |
|------|------|
| Callback URL | 你提供给第三方的 URL（数据发送目标） |
| Event | 触发 Webhook 的业务事件（如 `order.created`） |
| Payload | HTTP POST Body 中的 JSON 数据 |
| Secret / Signature | 共享密钥，用于验证请求来源真实性 |
| 200 OK | 你的服务必须快速返回的成功确认（否则平台会重试） |

---

## 2. 与 API / Polling / WebSocket 的对比

| 方式 | 方向 | 实时性 | 资源消耗 | 适用 |
|------|------|:---:|:---:|------|
| **API（主动拉）** | 客户端→服务器 | 按需 | 低 | 查询数据 |
| **Polling（轮询）** | 客户端→服务器 | 取决于间隔 | 高（空转浪费） | 简单状态监控 |
| **Webhook（被动推）** | 服务器→客户端 | **实时** | **低** | 事件通知 |
| **WebSocket** | 双向 | 实时 | 中（长连接） | 即时通讯 |

> 💡 Webhook 是"事件消费"的最佳模式 — 不用轮询、实时响应、资源效率最高。但要求你的服务**公网可达**。

---

## 3. 工作流程三步

```text
① 在第三方平台配置 Callback URL + 选择订阅的事件类型
   例：GitHub 上配置 https://yourserver.com/webhook/github

② 事件触发 → 平台向 Callback URL 发送 HTTP POST
   POST /webhook/github HTTP/1.1
   Content-Type: application/json
   X-Hub-Signature-256: sha256=abc123...
   {
     "action": "opened",
     "pull_request": { "title": "...", "user": {...} }
   }

③ 你的服务接收 → 验证签名 → 返回 200 OK → 异步处理业务逻辑
   ⚠️ 必须在 10-30 秒内返回 200（否则平台认为失败并重试）
```

---

## 4. 安全四件套

### 4.1 签名验证（防伪造）

```java
// HMAC-SHA256 验证（最常用方案）
public boolean verifySignature(String payload, String signatureHeader, String secret) {
    String expected = "sha256=" + HmacUtils.hmacSha256Hex(secret, payload);
    return MessageDigest.isEqual(expected.getBytes(), signatureHeader.getBytes());
    // ⚠️ 必须用时序安全的比较（防时序攻击），不要用 .equals()
}
```

### 4.2 IP 白名单（防来源伪造）

只接受已知平台的出口 IP（如 GitHub 的 `/meta` API 返回的 hooks IP 列表）。

### 4.3 防重放攻击

```java
// 检查请求时间戳（超过 5 分钟的请求直接丢弃）
if (Math.abs(System.currentTimeMillis() - timestamp) > 5 * 60 * 1000) {
    return ResponseEntity.status(400).build();
}
```

### 4.4 幂等性（防重复处理）

```java
// 基于事件 ID 去重
String eventId = request.getHeader("X-Event-Id");
if (processedEventCache.contains(eventId)) {
    return ResponseEntity.ok().build();  // 已处理过，不重复执行业务
}
processedEventCache.put(eventId);
```

---

## 5. 可靠性保障

| 策略 | 说明 |
|------|------|
| **快速返回 200** | 收到请求后先验签→返回 200→**再异步处理业务**（消息队列/线程池） |
| **指数退避重试** | 平台端：失败后 1s→2s→4s→8s...重试，通常最多 3-5 次 |
| **死信队列（DLQ）** | 多次重试仍失败→入 DLQ→告警→人工介入 |
| **顺序保证** | 依赖事件的时序时，用队列分区（同一订单的事件发同一分区） |

```text
标准架构：
Webhook 请求 → 签名验证 → 200 OK（<1s）
                        → 消息队列（Kafka/RabbitMQ）
                        → 消费者异步处理
                        → 失败 → 重试 → DLQ → 告警
```

---

## 6. 本地调试与测试

```bash
# ① ngrok：暴露本地服务到公网
ngrok http 8080
# 生成 https://abc123.ngrok.io → 配置为 Webhook URL

# ② 用 Postman 模拟 Webhook 请求
curl -X POST https://localhost:8080/webhook/test \
  -H "Content-Type: application/json" \
  -H "X-Signature: sha256=..." \
  -d '{"event":"test","data":{...}}'

# ③ 平台自带测试功能（GitHub/Dify 等都有"发送测试事件"按钮）
```

---

> 🎯 **核心要点**：Webhook 工程的"安全四件套+可靠性保障" = **签名验证（防伪造）+ IP 白名单 + 时间戳防重放 + 事件 ID 幂等** + **快速 200→异步处理→消息队列→DLQ 告警**。缺任何一个，生产环境都可能"丢事件或重复处理"。

**下一模块**：[02-AI平台Webhook实战](02-AI平台Webhook实战.md) / **返回总览**：[00-Hook总览](00-Hook知识体系总览.md)
