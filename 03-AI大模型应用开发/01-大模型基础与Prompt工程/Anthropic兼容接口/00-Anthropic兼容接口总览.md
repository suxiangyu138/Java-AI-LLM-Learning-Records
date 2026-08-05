# Anthropic 兼容接口 知识体系
> 第三方服务商通过兼容 Anthropic Messages API 格式，实现与 Anthropic SDK 和生态工具的即插即用

## 📚 目录
1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [学习路线推荐](#3-学习路线推荐)
4. [核心概念速查](#4-核心概念速查)

---

## 1. 知识体系导图

```
Anthropic 兼容接口
│
├── 1. 协议基础
│   ├── Messages API 请求格式
│   ├── Messages API 响应格式
│   ├── Stream (SSE) 格式
│   └── Tool Use 格式
│
├── 2. 主流兼容服务商
│   ├── DeepSeek — deepseek-chat / deepseek-reasoner
│   ├── OpenAI — 兼容端点
│   ├── Google Gemini — 兼容转换层
│   ├── 国内厂商 — 硅基流动 / 阿里云百炼 / 智谱
│   └── 其他 — Together AI / Fireworks / Groq
│
├── 3. SDK 与工具生态
│   ├── Anthropic SDK (Python/TypeScript/Java)
│   ├── one-api / new-api 统一网关
│   ├── LobeChat / NextChat 等前端
│   └── LangChain4j / Spring AI 集成
│
├── 4. 协议差异对比
│   ├── Anthropic ↔ OpenAI 格式映射
│   ├── 关键字段差异 (role, system, cache)
│   └── 功能支持矩阵
│
└── 5. 最佳实践
    ├── 多 Provider 切换方案
    ├── 成本优化策略
    └── 故障转移与负载均衡
```

## 2. 模块导航

| 序号 | 模块 | 核心内容 | 适合人群 |
|:---:|------|---------|---------|
| 01 | 协议基础 | Messages API 请求/响应/Stream/Tool Use 格式详解 | 所有开发者 |
| 02 | 主流服务商 | DeepSeek / OpenAI / Gemini 等兼容端点的配置与差异 | API 调用者 |
| 03 | SDK 与工具生态 | 各语言 SDK、网关、前端工具的兼容使用 | 工程化实践者 |
| 04 | 协议差异对比 | Anthropic ↔ OpenAI 格式映射、功能矩阵 | 迁移/选型决策者 |
| 05 | 最佳实践 | 多 Provider 切换、成本优化、故障转移 | 运维/架构师 |

## 3. 学习路线推荐

### 🟢 入门路径（快速调用）
1. **协议基础** — 理解 Messages API 请求/响应结构
2. **主流服务商** — 选择一个服务商（推荐 DeepSeek）完成首次调用
3. **SDK 与工具生态** — 使用 Anthropic SDK 切换 Base URL

### 🟡 进阶路径（工程化）
1. 入门路径全部内容
2. **协议差异对比** — 理解不同协议间的映射关系
3. **SDK 与工具生态** — 部署 one-api 网关，统一管理多 Provider

### 🔴 高级路径（生产级架构）
1. 进阶路径全部内容
2. **最佳实践** — 实现多 Provider 故障转移与成本路由
3. **协议差异对比** — 深度理解缓存、Tool Use 等高级特性的协议差异

## 4. 核心概念速查

| 概念 | 说明 |
|------|------|
| **Messages API** | Anthropic 的 RESTful API 格式，以 `messages` 数组传递对话历史 |
| **兼容接口** | 第三方服务商实现与 Anthropic 相同的 API 格式，SDK 只需改 `base_url` |
| **Stream (SSE)** | Server-Sent Events 流式输出，Anthropic 使用 `event:` 类型区分消息片段 |
| **Tool Use** | 函数调用能力，Anthropic 通过 `tool_use` content block 实现 |
| **Base URL** | API 端点地址，切换兼容服务商的核心配置项 |
| **one-api** | 开源 API 网关，统一管理多 Provider 的密钥、路由、配额 |

---

**下一模块**：[01-Anthropic Messages API 协议基础](01-Anthropic-Messages-API协议基础.md) / **返回总览**：[00-Anthropic兼容接口总览](00-Anthropic兼容接口总览.md)
