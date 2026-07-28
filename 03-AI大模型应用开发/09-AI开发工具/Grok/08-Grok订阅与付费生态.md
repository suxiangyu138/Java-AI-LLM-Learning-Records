# 08 - Grok 订阅与付费生态（2026.07）

> 🎯 2026 年 Grok 的付费生态经历了最大变化——SpaceXAI 整合了 Cursor、Grok 4.5 以 $2/$6 的颠覆性定价入局、SuperGrok 与 Cursor Pro 权益打通。理解每档方案的差异是性价比选型的关键

---

## 📚 目录

1. [订阅方案全景（2026.07）](#1-订阅方案全景202607)
2. [X 平台免费版](#2-x-平台免费版)
3. [X Premium+](#3-x-premium)
4. [SuperGrok 独立订阅](#4-supergrok-独立订阅)
5. [xAI API 按量付费](#5-xai-api-按量付费)
6. [Cursor Pro — 新增入口](#6-cursor-pro--新增入口)
7. [方案选型指南](#7-方案选型指南)

---

## 1. 订阅方案全景（2026.07）

```text
Grok 付费生态（2026.07 最新）
│
├── 🆓 X 免费版 — $0/月
│   ├── 每 2h 10 次基础 Grok 查询
│   └── 适合：轻度体验
│
├── ⭐ X Premium+ — $16/月
│   ├── Grok 4.5 完整能力 + X 蓝 V
│   ├── DeepSearch + Aurora + Thinking Mode
│   └── 适合：X 活跃用户 + Grok 深度用户
│
├── 🚀 SuperGrok — $30/月
│   ├── 无限查询 + Early Access
│   ├── 独立于 X 的账号体系 + 跨平台同步
│   └── 适合：高频专业用户
│
├── 💻 Cursor Pro — $20/月
│   ├── 含 Grok 4.5 Agent 调用额度
│   ├── IDE 内原生编程体验 ← 2026 新增入口！
│   └── 适合：开发者首选
│
└── 💰 xAI API — 按量付费
    ├── grok-4.5: $2 in / $6 out per 1M ← Token 效率 4.2×
    ├── grok-3-mini: $0.30 in / $4 out
    └── 适合：代码集成 / 高频批量调用
```

---

## 2. X 平台免费版

| 维度 | 限额 |
|------|------|
| Grok 查询 | 每 2 小时 10 次 |
| 图片分析 | ✅ 计次 |
| DeepSearch | ❌ |
| Aurora 图像生成 | ❌（或极有限） |

---

## 3. X Premium+

| 功能 | 免费版 | Premium+ ($16/月) |
|------|:---:|:---:|
| Grok 模型 | 基础 | **Grok 4.5 完整** |
| 查询频率 | 10次/2h | 50次/2h |
| DeepSearch | ❌ | ✅ |
| Thinking Mode | ❌ | ✅ |
| Aurora 图像生成 | ❌ | ✅ |
| X 蓝 V 认证 | ❌ | ✅ |
| 广告减半 | ❌ | ✅ |

> 💡 $16/月获得 Grok 4.5 完整能力仍然极具竞争力——对标 ChatGPT Plus ($20) 和 Claude Pro ($20)。

---

## 4. SuperGrok 独立订阅

| 维度 | Premium+ | SuperGrok ($30/月) |
|------|:---:|:---:|
| Grok 查询 | 50次/2h | 更高（接近无限） |
| 绑定 X | ✅ 必须 | ❌ 独立 |
| Early Access | ❌ | ✅ |
| 专属支持 | ❌ | ✅ |
| Cursor 集成 | ❌ | ⚠️ 部分权益 |

---

## 5. xAI API 按量付费

| 模型 | 输入/1M | 输出/1M | 缓存 | 上下文 |
|------|:---:|:---:|:---:|:---:|
| **grok-4.5** | **$2** | **$6** | $0.50 | 500K |
| grok-3 | $3 | $15 | $0.75 | 1M |
| grok-3-mini | $0.30 | $4 | $0.075 | 1M |

### Grok 4.5 Token 效率对成本的影响

```text
同样的 Bug 修复任务：

Opus 4.8 API：输出 ~67K tokens → $1.68
Grok 4.5 API：输出 ~16K tokens → $0.10

即使 Grok 4.5 的"单价"不是最低的，
但因为 Token 效率 4.2×，实际成本只有对手的 6%。
```

---

## 6. Cursor Pro — 新增入口

2026 年 7 月，Grok 4.5 原生集成到 Cursor 中：

| 功能 | Cursor Pro ($20/月) |
|------|:---:|
| Grok 4.5 补全 | ✅ |
| Grok 4.5 Agent | ✅ 含额度 |
| Grok 4.5 Chat | ✅ 侧边栏 |
| IDE 集成 | ✅ 原生（不需要 API Key） |
| 适合 | 开发者首选入口 |

> 💡 **推荐**：Java 开发者用 Cursor Pro ($20/月) 作为 Grok 4.5 的主力入口，IDE 内原生体验 + Agent 编程能力。

---

## 7. 方案选型指南

| 角色 | 推荐方案 | 月费 |
|------|------|:---:|
| 👨‍💻 **开发者** | Cursor Pro + DeepSeek API (省钱) | $20-25 |
| 💼 **职场人 + X 用户** | X Premium+ | $16 |
| 🔬 **研究员** | SuperGrok | $30 |
| 🏢 **企业团队** | Cursor Business + xAI API | 按需 |
| 🆓 **学生/学习者** | X 免费版 + DeepSeek 免费 | $0 |

> 🎯 **核心要点**：2026 年 Grok 付费生态的最大变化——Cursor Pro ($20/月) 成为开发者的 Grok 4.5 最佳入口。Token 效率优势意味着即使用 API 按量付费，高频使用场景下的成本也远低于竞品。

---

**上一模块**：[07-Grok图像生成与Aurora详解](07-Grok图像生成与Aurora详解.md) | **下一模块**：[09-Grok-Java后端集成实战](09-Grok-Java后端集成实战.md) | **返回总览**：[00-Grok知识体系总览](00-Grok知识体系总览.md)
