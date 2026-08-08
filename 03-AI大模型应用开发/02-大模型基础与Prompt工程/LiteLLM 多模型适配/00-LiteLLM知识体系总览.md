# LiteLLM 多模型适配知识体系总览
> 100+ 模型一个接口：LiteLLM SDK 统一接入 + Proxy 网关工程化——多模型时代的适配层

## 📚 目录
1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [学习路线推荐](#3-学习路线推荐)
4. [体系定位与分工](#4-体系定位与分工)
5. [核心概念速查](#5-核心概念速查)
6. [参考来源](#6-参考来源)

## 1. 知识体系导图

```text
LiteLLM 多模型适配 ——11 篇（v1.94.0 基准，2026-08）
│
├─ 认知层 ─────────────────────────────
│   ├─ 00 总览（本文）
│   ├─ 01 定位与核心能力（SDK vs Proxy/统一抽象/2026 版本线）
│   └─ 02 SDK 编程接入（completion 统一接口/参数映射/流式）
│
├─ 能力层 ─────────────────────────────
│   ├─ 03 模型管理（model_list/别名/密钥管理/供应商）
│   ├─ 04 路由与负载均衡（Router/fallback/重试/plugins）
│   └─ 05 成本与上下文（token 记账/预算/缓存/压缩）
│
├─ 网关层 ─────────────────────────────
│   ├─ 06 Proxy 部署与配置（config.yaml/虚拟 key/鉴权）
│   └─ 07 Proxy 生产实践（Docker/数据库/监控/高可用）
│
├─ 生态层 ─────────────────────────────
│   ├─ 08 生态集成（OpenAI SDK/LangChain/FastAPI/Java）
│   └─ 09 生产避坑与选型（vs One-API/OpenRouter 决策树）
│
└─ 实战层 ─────────────────────────────
│   └─ 10 面试冲刺（20 自测/毕业检查单/面试题）
```

> 🎯 一句话定位：LiteLLM = "**100+ 家模型供应商，一个统一接口**"——SDK 层一行代码换模型，Proxy 层一个网关管所有 key/路由/预算/审计。2026 年基准 **v1.94.0**（2026-07-28），正在迁移 Rust 内核（15 倍吞吐、11 倍省内存）。

## 2. 模块导航

| 序号 | 模块 | 核心内容 | 适合人群 |
|:---:|------|---------|---------|
| 00 | [总览（本文）](00-LiteLLM知识体系总览.md) | 导图、定位、速查 | 所有人 |
| 01 | [定位与核心能力](01-定位与核心能力.md) | SDK vs Proxy/统一抽象/版本线 | 入门必读 |
| 02 | [SDK 编程接入](02-SDK编程接入.md) | completion/参数映射/流式/异步 | 入门必读 |
| 03 | [模型管理](03-模型管理.md) | model_list/别名/密钥/供应商 | 重点 |
| 04 | [路由与负载均衡](04-路由与负载均衡.md) | Router/fallback/重试/健康检查 | 重点 |
| 05 | [成本与上下文](05-成本与上下文.md) | token 记账/预算/缓存/压缩 | 重点 |
| 06 | [Proxy 部署与配置](06-Proxy部署与配置.md) | config.yaml/虚拟 key/鉴权/团队 | 重点 |
| 07 | [Proxy 生产实践](07-Proxy生产实践.md) | Docker/数据库/监控/高可用 | 进阶 |
| 08 | [生态集成](08-生态集成.md) | OpenAI SDK/LangChain/FastAPI/Java | 进阶 |
| 09 | [生产避坑与选型](09-生产避坑与选型.md) | 决策树/12 避坑/落地清单 | 进阶 |
| 10 | [面试冲刺](10-面试冲刺.md) | 20 自测/毕业检查单/面试题 | 收尾 |

## 3. 学习路线推荐

| 路线 | 人群 | 路径 |
|------|------|------|
| 快速上手（半天） | 会 OpenAI SDK | 01 → 02 → 03 → 08 |
| 完整学习（2 天） | 系统学习 | 00 → 01 → 02 → 03 → 04 → 05 → 06 → 07 → 08 → 09 → 10 |
| 网关工程（进阶） | 要管团队 key/预算 | 03 → 05 → 06 → 07 → 09 |
| 多模型生产 | 多供应商切换 | 02 → 04 → 05 → 09 |

> 💡 完成标志：**能用 SDK 一行切换三家模型 + 能部署 Proxy 网关（虚拟 key/预算/审计全配好）**——达标后多模型接入问题从此只写配置不写代码。

## 4. 体系定位与分工

```text
与 OpenAI兼容接口/04-API聚合网关.md 的分工：
  聚合网关 04 = 网关全景（One-API/New-API/LiteLLM/OpenRouter 横向对比 + 选型）
  本体系     = LiteLLM 专题深潜（SDK 编程 + Proxy 工程化全链路）

与 LLM-API 体系的分工：
  LLM-API = 各家 API 使用（Claude/DeepSeek/Gemini 各自怎么调）
  本体系   = 统一抽象层（一次编写，多平台运行）

与 Function Calling 体系的分工：
  FC 体系 = 工具调用协议与循环（08 章提到适配层）
  本体系   = 适配层的现成实现（LiteLLM 就是那个适配层）
```

> 🎯 认知起点：**多模型时代的工程问题不是"会调一家"而是"统一管所有"**——切换成本、key 管理、预算控制、故障切换。LiteLLM 把这些问题压缩成"一行代码 + 一个配置文件"。

## 5. 核心概念速查

| 概念 | 一句话 |
|------|--------|
| LiteLLM | 100+ 供应商的统一 LLM 接口库（v1.94.0，2026-07-28） |
| SDK | Python 库：`litellm.completion()` 统一调用 |
| Proxy | 网关服务：一个 OpenAI 兼容端点管所有模型 |
| model_list | 模型注册表（供应商/模型/别名/优先级） |
| model_name | 统一别名（你代码里用的名字） |
| Router | SDK 路由器：负载均衡/fallback/重试 |
| Fallback | 主模型失败自动切备模型 |
| 虚拟 Key | Proxy 分发的 key（限制预算/速率/模型） |
| Spend tracking | 每 key/每模型/每团队的成本记账 |
| 预算控制 | 团队/用户/key 级预算上限 |
| Prompt caching | 提示词缓存（降本） |
| Prompt compression | 上下文压缩（v1.83.14+） |
| Guardrails | 内容安全策略（Proxy 层） |
| 数据面/控制面 | 2026 组件化部署：请求处理与管理工作分离 |
| Rust 网关 | 2026 迁移中：15 倍吞吐、11 倍省内存 |
| Virtual Keys | 代理分发的受限 key |
| config.yaml | Proxy 全部配置（模型/路由/预算/鉴权） |
| SSO/SAML | 企业单点登录（Enterprise） |

## 6. 参考来源

- [LiteLLM 官方文档](https://docs.litellm.ai/)
- [LiteLLM Release Notes（v1.94.0）](https://docs.litellm.ai/release_notes)
- [LiteLLM Blog：Router Plugins](https://docs.litellm.ai/blog/router-plugins-on-the-proxy)
- [FutureAGI：What is LiteLLM 2026](https://futureagi.com/blog/what-is-litellm-2026/)
- [LiteLLM GitHub](https://github.com/BerriAI/litellm)
- [OpenAI兼容接口/04-API聚合网关.md](../OpenAI兼容接口/04-API聚合网关.md)（网关横向对比）
- [LLM-API 体系](../LLM-API/00-LLM%20API知识体系总览.md)（各家 API 使用）
- [Function Calling 体系](../Function%20Calling%20函数调用【Agent%20基石】/00-FunctionCalling知识体系总览.md)（适配层上层）
- [Python 异步 + FastAPI](../../01-Python语言/Python%20异步%20+%20FastAPI/00-Python异步与FastAPI知识体系总览.md)（服务端工程）

---

**下一模块**：[01-定位与核心能力](01-定位与核心能力.md)
