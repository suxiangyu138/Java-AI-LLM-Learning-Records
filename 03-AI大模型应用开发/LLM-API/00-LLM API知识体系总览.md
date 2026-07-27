# 00 - LLM API 知识体系总览

> 🎯 LLM API 是大模型落地的"最后一公里"——掌握主流平台的 API 调用、流式输出、Function Calling、生产级封装，是将 AI 能力集成到任何应用中的必备技能

> 🎯 本系列共 **12 篇**，从协议基础到平台实战、从 GPT/Gemini/Claude/DeepSeek 到国产模型、从 Function Calling 到生产优化，覆盖 LLM API 开发全链路

---

## 1. 知识全景

```
LLM API 体系（12个文件）
│
├── 🏗️ 基础篇（01-03）
│   ├── 01-LLM API协议与核心概念.md        # 端点/认证/参数/响应/平台对比
│   ├── 02-大模型API调用实践.md            # OpenAI SDK/HTTP/重试/Key安全/Java集成
│   └── 03-多平台统一调用与流式输出.md      # DeepSeek/Qwen/GLM + SSE流式 + 多轮对话
│
├── 🔧 平台实战篇（04-08）
│   ├── 04-快速精通GPT.md                  # OpenAI: FC/Structured/Vision/Caching/Batch
│   ├── 05-快速精通Gemini.md               # Google: 多模态/1M上下文/搜索/代码执行
│   ├── 06-快速精通Claude API.md           # Anthropic: Tool Use/200K/Computer Use
│   ├── 07-快速精通DeepSeek API.md         # DeepSeek: V3/R1推理/FIM/极低成本
│   └── 08-国产大模型API全景.md            # Qwen/GLM/Moonshot/Kimi/豆包
│
├── 🚀 进阶篇（09-10）
│   ├── 09-Function Calling与Tool Use实战.md  # 跨平台FC统一封装/递归调用/Agent
│   └── 10-主流大模型API速查与选型指南.md      # 各平台速查表/场景选型/成本对比
│
└── 📌 冲刺篇（11）
    └── 11-面试高频考点与总结.md              # API面试题/选型答辩/最佳实践
```

## 2. 文件导航

| # | 文件 | 核心内容 | 级别 |
|---|------|----------|:---:|
| 00 | 知识体系总览 | 全景+路线 | — |
| 01 | API协议与核心概念 | 端点/认证/参数/响应 | ⭐⭐⭐⭐ |
| 02 | API调用实践 | SDK/HTTP/重试/Java | ⭐⭐⭐⭐⭐ |
| 03 | 多平台统一调用 | 流式SSE/多轮对话/错误处理 | ⭐⭐⭐⭐⭐ |
| 04 | 快速精通GPT | FC/Structured/Vision/Batch | ⭐⭐⭐⭐⭐ |
| 05 | 快速精通Gemini | 多模态/1M上下文/搜索 | ⭐⭐⭐⭐⭐ |
| 06 | 快速精通Claude | Tool Use/200K/Computer Use | ⭐⭐⭐⭐ |
| 07 | 快速精通DeepSeek | V3/R1/FIM/极低成本 | ⭐⭐⭐⭐ |
| 08 | 国产大模型API | Qwen/GLM/Moonshot/Kimi | ⭐⭐⭐⭐ |
| 09 | Function Calling实战 | 跨平台封装/递归/Agent | ⭐⭐⭐⭐⭐ |
| 10 | API速查与选型 | 速查表/场景选型/成本 | ⭐⭐⭐ |
| 11 | 面试高频考点 | API面试题/选型答辩 | ⭐⭐⭐⭐⭐ |

## 3. 学习路线

```text
🟢 上手（45min）：01-协议概念 → 02-调用实践 → 10-速查选型
🔵 平台（2h）：04-GPT → 05-Gemini → 06-Claude → 07-DeepSeek → 08-国产
🟣 进阶（45min）：03-流式输出 → 09-Function Calling
🔴 冲刺（30min）：11-面试考点
```

## 4. 平台速查

| 平台 | API 协议 | 特色能力 | 成本（1M tokens） |
|------|---------|---------|:---:|
| **OpenAI GPT** | OpenAI 原生 | FC + Vision + Batch | $2.5-15 |
| **Google Gemini** | OpenAI 兼容 / 原生 | 1M 上下文 + 多模态 + 搜索 | $0.5-10 |
| **Anthropic Claude** | Anthropic 原生 | 200K 上下文 + Tool Use | $3-15 |
| **DeepSeek** | OpenAI 兼容 | 推理模式 + FIM + 极低成本 | **$0.14-2.2** |
| **Qwen 通义千问** | OpenAI 兼容 | Apache 2.0 + 中文王者 | $0.5-4 |
| **GLM 智谱** | OpenAI 兼容 | 国产合规 + 多模态 | $0.5-5 |
| **Moonshot Kimi** | OpenAI 兼容 | 128K 上下文 + 中文 | $0.6-3 |
