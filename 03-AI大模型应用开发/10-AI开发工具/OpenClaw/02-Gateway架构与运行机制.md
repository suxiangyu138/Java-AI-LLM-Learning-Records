# 02 - Gateway 架构与运行机制

> 🎯 Gateway 是 OpenClaw 的"神经中枢" — 单一长驻进程，监听 127.0.0.1:18789，负责所有消息路由、身份认证、渠道管理、Cron 调度。理解 Gateway = 理解 OpenClaw 的一切

---

## 目录

1. [分层架构总览](#1-分层架构总览)
2. [Gateway 核心机制](#2-gateway-核心机制)
3. [三层认证体系](#3-三层认证体系)
4. [消息路由与会话隔离](#4-消息路由与会话隔离)
5. [Cron 与心跳引擎](#5-cron-与心跳引擎)
6. [Channel Plugin 渠道插件](#6-channel-plugin-渠道插件)

---

## 1. 分层架构总览

```text
OpenClaw 五层架构：

┌─────────────────────────────────────────────┐
│  渠道适配层                                   │  ← Channel Plugins
│  WhatsApp | Telegram | 飞书 | Slack | CLI... │
├─────────────────────────────────────────────┤
│  ★ Gateway 网关层（127.0.0.1:18789）        │  ← 神经中枢
│  消息路由 | 身份认证 | Session管理 | Cron引擎 │
├─────────────────────────────────────────────┤
│  核心业务层                                   │
│  Agent（ReAct循环）| Skills | Memory | Tools │
├─────────────────────────────────────────────┤
│  模型适配层                                   │
│  Claude | GPT | Gemini | DeepSeek | Ollama.. │
├─────────────────────────────────────────────┤
│  数据存储层                                   │
│  Session JSONL | MEMORY.md | Cron Jobs | Logs│
└─────────────────────────────────────────────┘
```

---

## 2. Gateway 核心机制

### 2.1 单端口多路复用

```text
Gateway 默认监听 127.0.0.1:18789（Web UI 面板 18790）

单端口同时承载三种通信：
├── WebSocket：实时指令收发、工具调用流、状态推送
├── HTTP：控制面板 REST API、Webhooks
└── API 兼容接口

通信帧格式（TypeBox JSON Schema）：
├── Request 帧：req_id + method + params
├── Response 帧：req_id + result/error
└── Event 帧：event_name + data（server push）

第一帧必须是 connect 消息（强制握手，声明角色）
副作用方法（send、agent）需携带幂等性键 → 短期去重缓存
```

### 2.2 角色声明

| 角色 | 能力 |
|------|------|
| **Operator** | 操作员/管理员：修改配置、启动 Agent、管理设备 |
| **Node** | 执行节点：如手机、另一台 Mac，声明"摄像头""屏幕""Shell"等能力 |

### 2.3 确定性路由

Gateway 记录每条入站消息的 SessionKey，保证**"消息从哪来、回哪去"** — WhatsApp 来的消息必定回复到 WhatsApp，不会串渠道。

---

## 3. 三层认证体系

```text
第 1 层：Gateway Token（共享密钥）
  → OPENCLAW_GATEWAY_TOKEN 环境变量
  → 所有客户端必须携带

第 2 层：Device Identity（设备身份）
  → 非对称加密签名：客户端私钥签名、Gateway 公钥验证
  → 绑定 platform + deviceFamily，防重放

第 3 层：Pairing Approval（人工审批）
  → 新设备连接时需 Operator 审批
  → openclaw devices list / approve / reject
  → 本地 loopback/tailnet 连接可自动批准
```

---

## 4. 消息路由与会话隔离

### 4.1 分层优先级绑定

```text
消息路由优先级（高→低）：
① Group/Topic 级别绑定
② DM（私聊）级别绑定
③ Channel 级别绑定
④ 全局默认 Agent
```

### 4.2 Session 隔离

```text
Session Key 格式：
  agent:<agentId>:<channel>:direct:<peerId>     → 私聊
  agent:<agentId>:<channel>:group:<groupId>     → 群聊
  cron:<jobId>                                   → 定时任务

默认 dmScope: "per-channel-peer"
  → 同一用户在不同渠道发消息 = 不同 Session
  → 支持 identity linking 跨平台身份链接

⚠️ 多用户 DM 必须开启 per-channel-peer
  → 默认 main 模式会共享 session 上下文 → 隐私泄露
```

---

## 5. Cron 与心跳引擎

```text
Cron 引擎特性：
├── 内置在 Gateway 进程中（不依赖外部调度器）
├── 两种运行模式：
│   ├── systemEvent → 注入主 Session 心跳（共享上下文）
│   └── agentTurn → 独立 isolated Session（lightContext 跳过 workspace）
├── 三种 schedule：at / every / cron
├── 确定性随机 stagger（最多 5 分钟）→ 避免流量尖峰
├── 持久化：~/.openclaw/cron/jobs.json → 重启自动恢复
└── Heartbeat 心跳：定期唤醒 AI 主动工作（如定期检查服务器状态）
```

---

## 6. Channel Plugin 渠道插件

```typescript
// ChannelPlugin 接口
interface ChannelPlugin {
    id: string;                 // 唯一标识
    capabilities: {             // 能力声明
        chatTypes: string[];    // direct / group / channel
        media: string[];        // image / video / audio / file
        polls: boolean;
        streaming: boolean;
    };
    config: Record<string, any>;
    outbound: (message: OutboundMessage) => Promise<void>;
}
```

**Gateway 按 capabilities 动态调整行为** — 不支持图片的渠道自动隐藏图片生成功能、不支持流式的渠道自动退化为普通文本。

---

> 🎯 **核心要点**：Gateway 的三个"一" — **一个端口（18789）跑所有通信、一个进程（守护进程）管所有调度、一个 SessionKey 保证消息不串渠道**。三层认证（Token + Device Identity + Pairing Approval）保证即使开放到公网也不会被未授权设备控制。

**下一模块**：[03-Agent实现原理与ReAct循环](03-Agent实现原理与ReAct循环.md) / **返回总览**：[00-总览](00-OpenClaw知识体系总览.md)
