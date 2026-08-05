# 02 - AI 平台 Webhook 实战

> 🎯 Coze / Dify / n8n 三大平台 Webhook 集成实战 — 以 n8n 为编排中枢串联多平台、统一认证方案、触发器设计范式、LLM 节点黄金位置

---

## 目录

1. [三平台 Webhook 能力对比](#1-三平台-webhook-能力对比)
2. [n8n — 自动化编排中枢](#2-n8n--自动化编排中枢)
3. [Dify — API First AI 应用平台](#3-dify--api-first-ai-应用平台)
4. [Coze — 低代码 Agent 渠道集成](#4-coze--低代码-agent-渠道集成)
5. [典型集成架构](#5-典型集成架构)
6. [生产级设计范式](#6-生产级设计范式)

---

## 1. 三平台 Webhook 能力对比

| 维度 | n8n | Dify | Coze |
|------|-----|------|------|
| Webhook 入口 | **Webhook Trigger 节点**（生成唯一 URL） | 每个 Bot 天生是 API | 平台 Webhook（渠道 Callback URL） |
| 触发方式 | HTTP GET/POST + Cron + 消息+文件+DB | REST API / Chat API | 微信/飞书/豆包等平台回调 |
| 部署 | 云端 + **开源自托管** | 云端 + **开源自托管** | 仅云端 |
| 集成节点 | **400+** | 较少（API 驱动） | ~700 插件（封闭生态） |
| 代码扩展 | ✅ JS/Python 代码节点 | ✅ 自定义工具 API | ❌ 有限 |
| 自托管 TCO | 月任务>2000 时最低 | 中等 | 不可自托管 |

---

## 2. n8n — 自动化编排中枢

### 2.1 Webhook Trigger 节点

n8n 的 Webhook 是最灵活的触发器 — 创建一个 Webhook 节点即自动生成唯一 URL，外部系统调用该 URL 触发整个工作流。

```text
触发类型：
├── Webhook（HTTP GET/POST）
├── Cron 定时
├── 手动触发
├── 消息类：Telegram / Slack / 飞书 / 钉钉 / 企微
├── 文件系统监听
└── 数据库 / Kafka / MQTT
```

### 2.2 n8n 做编排中枢的典型模式

```text
Coze Bot 事件 → n8n Webhook 接收 → 调用 Dify API 推理 → 返回结果
GitHub PR 事件 → n8n → AI 代码审查（调 Claude API）→ 评论回 PR
飞书消息 → n8n → 解析意图 → Dify RAG 问答 → 返回飞书
```

---

## 3. Dify — API First AI 应用平台

### 3.1 每个 Bot 天生就是 API

```bash
# Dify 创建完 AI 应用后直接获取 API 端点
curl -X POST https://your-dify.com/v1/chat-messages \
  -H "Authorization: Bearer app-xxx" \
  -H "Content-Type: application/json" \
  -d '{"query": "什么是 Webhook？", "user": "user-123"}'

# 流式响应
curl -X POST https://your-dify.com/v1/chat-messages \
  -d '{..., "response_mode": "streaming"}'
```

### 3.2 多渠道接入

| 渠道 | 方式 |
|------|------|
| 微信 | dify-on-wechat 适配器 / LangBot |
| 飞书/钉钉 | 官方 Webhook + OAuth |
| Web 嵌入 | iframe / JS SDK |
| 自定义 | REST API（最灵活） |

---

## 4. Coze — 低代码 Agent 渠道集成

### 4.1 平台 Webhook 机制

Coze 通过各平台的 **Callback URL + Token 验证** 接入：
微信后台配置 Callback URL → Coze 接收消息 → LLM 处理 → 返回回复。

### 4.2 限制与注意

| 限制 | 说明 |
|------|------|
| 插件生态封闭 | 无 MCP 支持（当前）、调试能力有限 |
| 供应商锁定 | 工作流 JSON 难以迁移到其他平台 |
| 令牌消耗 | 比纯代码高 30%-50% |

---

## 5. 典型集成架构

### 5.1 n8n 做编排中枢

```text
┌──────────────────────────────────────────────┐
│                   n8n（编排中枢）               │
│                                              │
│  Webhook Trigger ─→ 签名验证 ─→ 条件路由       │
│      ↑                           ↓          │
│  外部事件               ├─→ Dify API（RAG）    │
│  (GitHub/飞书)          ├─→ Coze Bot（对话）    │
│                         ├─→ Claude API（推理）  │
│                         └─→ 数据库写入 + 通知    │
└──────────────────────────────────────────────┘
```

### 5.2 统一身份认证方案

三平台身份体系割裂 → **反向代理 + JWT 中继 + 声明映射** 三层解耦架构，将异构标识归一化为标准 bridge-jwt。

---

## 6. 生产级设计范式

### 6.1 LLM 节点黄金位置

| 位置 | 作用 | ROI |
|------|------|:---:|
| **前置** | 非结构化→结构化（邮件提取、字段识别） | ⭐⭐⭐ |
| **中置** | 推理决策（工单分类、意图识别）需置信度阈值 | ⭐⭐ |
| **后置** | 内容生成（润色、翻译） | ⭐ |

> 💡 核心原则：**LLM 做推理，规则做执行** — LLM 与执行之间必须加校验层。

### 6.2 错误处理三层纵深

```text
第 1 层：重试退避（指数退避：1s→2s→4s→8s...）
第 2 层：死信队列（DLQ + 告警）
第 3 层：人工审核（n8n Wait + Webhook 实现审批流）
```

### 6.3 选型建议

| 需求 | 推荐 |
|------|------|
| 快速构建 AI 应用 | Dify / Coze（开箱即用） |
| **企业级工作流/系统集成** | **n8n**（400+节点、自托管、月任务>2000 时 TCO 最低） |
| 数据隐私要求高 | Dify 或 n8n 开源自部署 |
| 纯对话 Agent | Coze（最简单） |

---

> 🎯 **核心要点**：AI 平台 Webhook 的黄金架构 = **n8n 做编排中枢 + Dify 负责 AI 推理 + Coze 负责 C 端渠道**。n8n 的 Webhook Trigger 可以串联任何有 API 的平台。生产级工作流设计规则：LLM 只做推理、规则做执行、人工做兜底。

**下一模块**：[03-LangChain Agent Hooks与Middleware](03-LangChain%20Agent%20Hooks与Middleware.md) / **返回总览**：[00-Hook总览](00-Hook知识体系总览.md)
