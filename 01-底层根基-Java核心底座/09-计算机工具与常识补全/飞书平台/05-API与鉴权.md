# 05 - API 与鉴权

> 定位：飞书能力的"程序入口"——"token 是通行证（tenant 应用身份 / user 用户身份）、API 是动作（发消息/读写表格）、事件订阅是耳朵（长连接/Webhook）——官方 Java SDK 是 2026 年的标准姿势"

---

## 📚 目录

1. [API 全景](#1-api-全景)
2. [双 token 体系](#2-双-token-体系)
3. [发送消息：第一个 API](#3-发送消息第一个-api)
4. [事件订阅：长连接 vs Webhook](#4-事件订阅长连接-vs-webhook)
5. [回调安全：验签与加密](#5-回调安全验签与加密)
6. [官方 SDK：Java 姿势](#6-官方-sdkjava-姿势)
7. [错误处理与重试](#7-错误处理与重试)
8. [五个常见坑](#8-五个常见坑)
9. [练习 5 题](#9-练习-5-题)

---

## 1. API 全景

**飞书开放 API 覆盖六件套的全部能力**——按域分组：**im**（消息/群/机器人）、**bitable**（多维表格）、**docx**（文档）、**calendar**（日历）、**approval**（审批）、**contact**（通讯录）、**drive**（云盘）。**API 的调用模型**：**HTTPS + Bearer token**——**每个请求带 access_token**（第 2 节）——"**API 是动作，token 是通行证**"。**API 的三大纪律**：**权限先行**（每个 API 对应一个 scope——**03 篇权限申请**）；**频控存在**（**每个应用有 QPS 限制**——09 篇限流）；**版本演进**（API 有版本（v1/v2）——**升级看官方文档**）。**2026 状态**：**API 体系成熟**（事件订阅延迟 <800ms、SDK 全语言）——"**飞书 API 是程序猿把飞书接进系统的'标准接口'**"。**调用通用模式**：`POST /open-apis/{域}/{版本}/{资源}` + `Authorization: Bearer {token}` + 对应 scope 权限——"**一个模式走遍六件套：会发消息，就会读写表格与文档**"。

## 2. 双 token 体系

**飞书有两种 access_token，场景完全不同**：

| Token | 身份 | 获取 | 适用 |
|-------|------|------|------|
| tenant_access_token | 应用身份（机器人） | App ID + Secret 换 | 机器调用（发消息/读表格） |
| user_access_token | 用户身份（代表用户） | App + 用户授权换 | 代表用户操作（创建日程/提交审批） |

**tenant_access_token 的获取**（**程序猿 90% 场景用它**）：

```bash
POST https://open.feishu.cn/open-apis/auth/v3/tenant_access_token/internal
{ "app_id": "cli_xxx", "app_secret": "xxx" }
# 返回 { "tenant_access_token": "t-xxx", "expire": 7200 }
```

**token 的三个要点**：**有效期 2 小时**（expire=7200——**过期要重新获取**）；**缓存复用**（**别每次请求都换 token**——缓存到快过期再换，09 篇）；**user token 要用户授权**（**OAuth 授权流程**——代表用户操作才有"人"的上下文）——"**tenant 是机器人办事，user 是替人办事——90% 的通知场景 tenant 就够**"。

## 3. 发送消息：第一个 API

**发送消息是"第一个 API"**——**调用模型**：

```bash
POST https://open.feishu.cn/open-apis/im/v1/messages?receive_id_type=chat_id
Authorization: Bearer {tenant_access_token}
{ "receive_id": "oc_xxx",       # 群/用户 ID（04 篇）
  "msg_type": "interactive",    # 消息类型（04 篇）
  "content": "{...卡片 JSON...}" }
```

**发消息的三个参数**：**receive_id**（接收方：chat_id 群/open_id 用户——**群 ID 在群设置里拿**）；**msg_type**（text/post/interactive——**04 篇**）；**content**（消息内容 JSON）。**程序猿的第一条消息**：**从群里拿 chat_id → 用 tenant token 发一条"hello"**——"**会发消息 = 会调飞书 API 了：后面的表格/文档 API 都是同一个模式（token + 参数）**"。**注意**：**卡片内容 JSON 要转义**（content 是字符串——**引号转义是新手第一坑**）。

## 4. 事件订阅：长连接 vs Webhook

**事件订阅让飞书"主动告诉你"发生了什么**（收到消息/文档被编辑/审批提交）——**两种订阅方式**（03 篇配置）：

| 方式 | 原理 | 优点 | 缺点 |
|------|------|------|------|
| 长连接（WebSocket） | 飞书连到你的服务（SDK 建连） | ⭐ 无需公网/延迟低/免验签 | 需 SDK 支持 |
| HTTP Webhook | 飞书 POST 到你的公网地址 | 无 SDK 也行 | 需公网/验签/防伪造 |

**2026 推荐**：**长连接是默认姿势**（**无需公网 IP/域名**——内网开发友好、延迟更低，官方 SDK 内置支持）；**Webhook 留给"必须 HTTP 回调"的场景**（已有网关/无 SDK 环境）。**订阅流程**：**开发者后台配置事件 → 发布版本 → 收到事件**。**事件的结构**：**event_id**（唯一标识——**去重用**）、**event**（事件类型与数据）、**timestamp**——"**长连接是'飞书主动找你'，Webhook 是'飞书敲门你开门'——2026 年首选长连接**"。**事件类型速查**：`im.message.receive_v1`（收到消息）、`card.action.trigger`（卡片点击）、`approval.instance.*`（审批事件）、`bitable.app.table.record.created`（表格新记录）——"**按需订阅，别全量订阅**"（订阅越少，处理越简单）。

## 5. 回调安全：验签与加密

**事件回调的安全三件套**（00 篇基线）：**verification_token**（**验证"事件真的来自飞书"**——回调里带 token，校验匹配）；**encrypt_key**（**事件载荷加密**——配置加密后事件体是密文，用 encrypt_key 解密）；**HMAC-SHA256 签名**（**Webhook 模式验签**——防篡改）。**为什么要验签**：**不验签的回调 = 任何人伪造事件**（攻击者 POST 一个假事件 → 你的服务执行假逻辑——**"不验签的回调是敞开的后门"**，00 篇误区四）。**验签的三个实践**：**长连接模式 SDK 自动处理**（**官方 SDK 内置验签解密——首选长连接的理由 +1**）；**Webhook 模式手动验签**（校验 verification_token/解密 encrypt_key）；**验签失败直接拒绝**（**别"先处理再验"**）——"**回调安全是'信任边界'：验签通过才相信，验签失败就拒绝**"。

## 6. 官方 SDK：Java 姿势

**官方 SDK（larksuite/oapi-sdk-java）是 2026 年 Java 开发的标准姿势**——**核心用法**：

```java
// 初始化客户端（App ID/Secret 从环境变量读）
Client client = new Client.Builder("appId", "appSecret")
        .logLevel(Level.INFO)
        .build();

// 发送消息（内置 token 管理与重试）
SendMessageReq req = new SendMessageReq()
        .setReceiveIdType("chat_id")
        .setReceiveId("oc_xxx")
        .setMsgType("interactive")
        .setContent(cardJson);
client.im().message().create(req);

// 事件监听（长连接模式）
EventDispatcher dispatcher = EventDispatcher.newBuilder("verificationToken", "encryptKey")
        .onP2ImMessageReceiveV1(event -> { /* 收到消息 */ })
        .onP2CardActionTrigger(event -> { /* 卡片点击 */ })
        .build();
```

**SDK 的三个价值**：**token 管理内置**（自动获取/缓存/刷新）；**长连接内置**（EventDispatcher 建连收事件）；**类型安全**（请求/响应强类型）——"**用 SDK 是'开车'，裸调 API 是'走路'——2026 年 Java 开发一律 SDK**"。

## 7. 错误处理与重试

**API 调用的错误处理**（通用模式）：**错误码判断**（飞书返回 `code` 字段——**0 成功，非 0 按码查文档**；HTTP 层还有 4xx/5xx）；**token 过期**（**99991663 等**——重新获取 token 重试）；**频控**（**达到 QPS 上限**——退避重试，09 篇限流）。**重试的三个纪律**：**幂等操作可重试**（发消息/读数据——**重复发送可容忍**）；**指数退避**（1s/2s/4s——**别暴力重试**）；**重试上限**（3 次——**超限记日志人工介入**）——"**错误处理三件事：识别错误码、处理 token 过期、按纪律重试**"（与外部 API 对接纪律一致——L1 体系的 06 篇对接纪律同源：`../软件架构能力体系培养/L1：初级开发｜看懂架构，落地模块/06-读懂接口与契约.md`）。

## 8. 五个常见坑

- **坑一**：每次请求都换 token——**频控白费 + 慢；缓存 token 到快过期**（第 2 节）；
- **坑二**：卡片 content 不转义——**JSON 解析失败；引号转义**（第 3 节）；
- **坑三**：Webhook 模式不验签——**伪造事件后门；verification_token/签名校验**（第 5 节）；
- **坑四**：裸调 API 不用 SDK——**token/重试/长连接全手写；官方 SDK**（第 6 节）；
- **坑五**：事件重复不处理——**重复执行副作用；event_id 去重**（第 4 节）。

## 9. 练习 5 题

1. API 按域怎么分？调用模型与三大纪律？
2. 双 token 的对比与获取？tenant token 的三要点？
3. 发送消息的三个参数？"会发消息 = 会调飞书 API"？
4. 长连接 vs Webhook 的对比？为什么 2026 首选长连接？
5. 回调安全三件套？错误处理三件事？

> 🎯 **核心要点**：API 与鉴权 = **"tenant token 办事（缓存复用）、user token 替人办事、事件订阅长连接首选、回调必验签"**——"**官方 SDK 是 2026 标准姿势（token/长连接/重试内置）**"；"会发消息 = 会调飞书 API"。

---

**下一模块**：[06-多维表格Bitable.md](06-多维表格Bitable.md) / **返回总览**：[00-飞书平台总览.md](00-飞书平台总览.md)
