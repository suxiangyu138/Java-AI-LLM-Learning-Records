# API 聚合网关

> One-API、New-API、LiteLLM Proxy——构建统一 LLM 接入层的三大方案。一个入口，管理所有模型渠道。

---

## 📚 目录

1. [为什么需要 API 聚合网关](#1-为什么需要-api-聚合网关)
2. [One-API：开源多模型管理平台](#2-one-api开源多模型管理平台)
3. [New-API：One-API 增强版](#3-new-apione-api-增强版)
4. [LiteLLM Proxy：Python 生态网关](#4-litellm-proxypython-生态网关)
5. [OpenRouter：商业模型路由服务](#5-openrouter商业模型路由服务)
6. [网关核心能力对比](#6-网关核心能力对比)
7. [负载均衡策略](#7-负载均衡策略)
8. [自建网关架构设计](#8-自建网关架构设计)

---

## 1. 为什么需要 API 聚合网关

### 1.1 多模型管理的痛点

```text
痛点 1：Key 管理混乱
  ├── GPT-4o → OpenAI API Key
  ├── DeepSeek → DeepSeek API Key
  ├── 智谱 → 智谱 API Key
  ├── 通义千问 → 阿里云 API Key
  └── 本地 Ollama → 无 Key
  → 每个开发者要记住 5+ 个 key，泄露风险高

痛点 2：计费分散
  ├── OpenAI 账单（美元）
  ├── DeepSeek 账单（人民币）
  └── 阿里云账单（人民币）
  → 无法统一查看团队总消耗

痛点 3：切换成本高
  ├── 改 base_url
  ├── 改 api_key
  └── 改 model name
  → 每次切换都要改代码重新部署
```

### 1.2 网关的核心价值

```
Before（无网关）:
┌──────────┐
│ 你的应用  │──→ GPT-4o (api.openai.com)
│          │──→ DeepSeek (api.deepseek.com)
│          │──→ 智谱 (open.bigmodel.cn)
│          │──→ Ollama (localhost:11434)
└──────────┘
   每个渠道独立管理 key、计费、限流


After（有网关）:
┌──────────┐      ┌─────────────────┐
│ 你的应用  │─────→│  API 聚合网关     │
│          │      │  localhost:3000  │
└──────────┘      │                 │
                  │ 渠道1: GPT-4o   │
                  │ 渠道2: DeepSeek  │
                  │ 渠道3: 智谱     │
                  │ 渠道4: Ollama   │
                  └─────────────────┘
   应用只需对接网关，key 统一管理
```

---

## 2. One-API：开源多模型管理平台

### 2.1 一句话定位

> One-API 是 Go 语言编写的开源 LLM API 管理平台，提供 Web 管理界面，支持负载均衡、用户管理、额度控制，是团队级 LLM 网关的首选。

### 2.2 部署（Docker）

```bash
docker run -d \
  --name one-api \
  -p 3000:3000 \
  -e TZ=Asia/Shanghai \
  -v /data/one-api:/data \
  justsong/one-api:latest
```

### 2.3 核心概念

```
渠道（Channel）
  └── 一个模型提供商的 API 配置（base_url + api_key）
  例：渠道 "OpenAI GPT-4o" → api.openai.com + sk-xxx

模型（Model）
  └── 对外暴露的模型名称
  例：gpt-4o → 映射到渠道 "OpenAI GPT-4o"

令牌（Token）
  └── 用户/应用使用的 API Key
  例：sk-app1-xxxx → 权限绑定到模型 gpt-4o, deepseek-chat

用户组（Group）
  └── 用户分组，按组设置额度
```

### 2.4 配置流程

```text
1. 添加渠道
   → 渠道名称：OpenAI GPT-4o
   → 类型：OpenAI
   → Base URL：https://api.openai.com
   → API Key：sk-xxxxxxxx

2. 渠道关联模型
   → 渠道 "OpenAI GPT-4o" 支持的模型：gpt-4o, gpt-4o-mini, text-embedding-3-small

3. 创建令牌
   → 令牌：sk-myapp-abc123
   → 绑定的模型：gpt-4o, gpt-4o-mini, deepseek-chat
   → 额度限制：$100/月

4. 应用使用
   → base_url: http://your-gateway:3000/v1
   → api_key: sk-myapp-abc123
   → 代码零改动！
```

### 2.5 高级特性

```text
✅ 负载均衡
   → 同一模型配置多个渠道，One-API 自动分发请求

✅ 自动重试
   → 渠道 A 失败 → 自动切换到渠道 B

✅ 速率限制
   → 按令牌、按用户组设置 RPM/TPM 上限

✅ 额度管理
   → 预充值模式，超额度自动拒绝

✅ 日志审计
   → 每次请求的模型、Token 消耗、费用一目了然

✅ 多租户
   → 一个 One-API 实例服务多个团队
```

### 2.6 多模型渠道配置示例

```yaml
# One-API 中的典型配置
渠道列表：
┌────────────────┬──────────┬──────────────────────────────┬─────────────┐
│ 渠道名称        │ 类型     │ Base URL                     │ 模型列表     │
├────────────────┼──────────┼──────────────────────────────┼─────────────┤
│ OpenAI 官方     │ OpenAI   │ https://api.openai.com       │ gpt-4o, ... │
│ DeepSeek 官方   │ 自定义   │ https://api.deepseek.com     │ deepseek-chat│
│ 智谱 GLM        │ 自定义   │ https://open.bigmodel.cn    │ glm-4-plus  │
│ 本地 vLLM       │ OpenAI   │ http://10.0.0.5:8000        │ qwen2.5-72b │
│ 本地 Ollama     │ OpenAI   │ http://localhost:11434       │ qwen2.5:7b  │
└────────────────┴──────────┴──────────────────────────────┴─────────────┘
```

---

## 3. New-API：One-API 增强版

### 3.1 一句话定位

> New-API 是 One-API 的增强 Fork，新增了**计费系统、用户充值、套餐管理**等功能，适合构建商业化的 API 平台。

### 3.2 核心增强

| 特性 | One-API | New-API |
|------|:---:|:---:|
| 渠道管理 | ✅ | ✅ |
| 负载均衡 | ✅ | ✅ |
| 令牌/用户管理 | ✅ | ✅ |
| **计费系统** | ❌ | ✅ 按量计费 |
| **用户充值** | ❌ | ✅ 在线充值 |
| **套餐管理** | ❌ | ✅ 月/季/年套餐 |
| **渠道价格设置** | ❌ | ✅ 设置利润率 |
| **用户仪表盘** | ❌ | ✅ 用量统计前端 |
| **分组倍率** | ⚠️ | ✅ 不同模型不同价格 |

### 3.3 适用场景

```text
One-API：
  → 团队内部使用，不需要计费
  → 研发团队统一 LLM 接入

New-API：
  → 对外提供 API 服务，需要收费
  → 企业内部多个部门按用量结算
  → 构建类似 OpenRouter 的代理服务
```

---

## 4. LiteLLM Proxy：Python 生态网关

### 4.1 一句话定位

> LiteLLM Proxy 是 Python 生态的 API 网关，优势在于模型覆盖最广（100+ 提供商）和配置灵活性最高。

### 4.2 配置方式

```yaml
# litellm_config.yaml
model_list:
  - model_name: gpt-4o
    litellm_params:
      model: openai/gpt-4o
      api_key: sk-openai-xxxxx

  - model_name: deepseek-chat
    litellm_params:
      model: deepseek/deepseek-chat
      api_key: sk-deepseek-xxxxx

  - model_name: local-qwen
    litellm_params:
      model: ollama/qwen2.5:7b
      api_base: http://localhost:11434

  - model_name: claude-3-opus
    litellm_params:
      model: bedrock/anthropic.claude-3-opus
      aws_region: us-east-1

litellm_settings:
  drop_params: true          # 自动忽略不支持的参数
  set_verbose: false
  request_timeout: 120

general_settings:
  master_key: sk-admin-xxxx  # 管理密钥
  database_url: postgresql://...
```

### 4.3 LiteLLM 的独特优势

```text
✅ 支持 100+ 模型提供商（远超 One-API）
   → AWS Bedrock, Azure OpenAI, Vertex AI, HuggingFace...
   → OpenAI, Anthropic, Cohere, Replicate, Together AI...

✅ drop_params: true
   → 自动去掉模型不支持的参数，避免报错
   → 例：GPT-4o 支持 seed，但 DeepSeek 不支持
   → LiteLLM 自动把 seed 从发给 DeepSeek 的请求中删除

✅ 模型降级 (Fallback)
   router:
     - model: gpt-4o
       fallbacks: [gpt-4o-mini, local-qwen]

✅ 多模型重试 (Retry)
   router:
     - model: gpt-4o
       num_retries: 3
       retry_policy:
         - strategy: "exponential_backoff"

✅ 成本追踪
   → 每次调用自动记录 token 消耗和费用
   → 支持 Postgres / Prometheus 存储
```

### 4.4 LiteLLM vs One-API

```text
选 LiteLLM 如果：
  ✅ Python 是你的主力技术栈
  ✅ 需要对接 AWS Bedrock / Azure / Vertex 等云平台
  ✅ 需要灵活的 Fallback / Retry 策略
  ✅ 需要 drop_params 自动适配

选 One-API 如果：
  ✅ 需要 Web 管理界面（非开发人员也能配置）
  ✅ 团队技术栈以 Java/Go 为主
  ✅ 需要多租户 + 用户体系
  ✅ 需要开箱即用的计费（New-API）
```

---

## 5. OpenRouter：商业模型路由服务

### 5.1 一句话定位

> OpenRouter 是商业化的 LLM 路由器，聚合了 200+ 模型，你只需要一个 API Key 就能调用所有模型。

### 5.2 使用方式

```bash
curl https://openrouter.ai/api/v1/chat/completions \
  -H "Authorization: Bearer $OPENROUTER_API_KEY" \
  -d '{
    "model": "openai/gpt-4o",
    "messages": [{"role": "user", "content": "Hello!"}]
  }'
```

### 5.3 适合场景

```text
✅ 快速原型 → 不需要自己部署网关
✅ 模型探索 → 一个 Key 试遍所有模型
✅ 个人项目 → 用量不大，不值得自建网关

❌ 不适合：
  → 对数据隐私有要求（请求经过第三方）
  → 大量调用（价格包含 OpenRouter 溢价）
  → 需要自定义路由策略
```

---

## 6. 网关核心能力对比

| 能力 | One-API | New-API | LiteLLM Proxy | OpenRouter |
|------|:---:|:---:|:---:|:---:|
| 开源 | ✅ MIT | ✅ MIT | ✅ MIT | ❌ |
| Web 管理界面 | ✅ | ✅ | ⚠️ 社区版 | ✅ SaaS |
| 多租户 | ✅ | ✅ | ⚠️ 通过 key 隔离 | ✅ |
| 负载均衡 | ✅ | ✅ | ✅ | ✅ |
| 自动重试 | ✅ | ✅ | ✅ | ✅ |
| 计费系统 | ❌ | ✅ | ⚠️ | ✅ |
| API 调用 | OpenAI 格式 | OpenAI 格式 | OpenAI 格式 | OpenAI 格式 |
| 模型数量 | ~20 类型 | ~20 类型 | 100+ | 200+ |
| 自部署 | ✅ | ✅ | ✅ | ❌ |
| 语言 | Go | Go | Python | — |

---

## 7. 负载均衡策略

### 7.1 常见策略

```text
策略 1：轮询（Round Robin）
  请求1 → 渠道A, 请求2 → 渠道B, 请求3 → 渠道A...

策略 2：加权轮询（Weighted）
  高性能渠道权重 5，普通渠道权重 1
  → 6 个请求中 5 个到高性能渠道

策略 3：最少连接（Least Connections）
  → 实时检测每个渠道的并发请求数，发给最少的那一个

策略 4：优先级 + 故障转移
  优先 → 本地 vLLM（低延迟、免费）
  降级 → DeepSeek API（中等成本）
  兜底 → GPT-4o API（最高质量、最高成本）
```

### 7.2 One-API 中的配置

```text
渠道设置：
┌──────────┬──────┬──────────┐
│ 渠道     │ 优先级│ 权重     │
├──────────┼──────┼──────────┤
│ vLLM-1   │  1   │  10      │ ← 主力，高权重
│ vLLM-2   │  1   │  10      │ ← 主力，高权重
│ DeepSeek │  2   │  5       │ ← 备用
│ GPT-4o   │  3   │  1       │ ← 兜底
└──────────┴──────┴──────────┘

执行逻辑：
  1. 优先使用优先级最高的渠道组（vLLM-1, vLLM-2）
  2. 同优先级内按权重分发
  3. 当 vLLM 渠道全部不可用 → 启用 DeepSeek
  4. 只有 DeepSeek 也挂了 → 最后用 GPT-4o
```

---

## 8. 自建网关架构设计

### 8.1 最小可行架构

```text
如果你只需要一个简单的转发网关（不需要 Web 界面）：

┌──────────────────────────────────────┐
│          Spring Cloud Gateway         │
│                                       │
│  /v1/**  → 根据 Header 路由           │
│    X-Model → gpt-4o → OpenAI 渠道     │
│    X-Model → deepseek → DeepSeek 渠道 │
│                                       │
│  内置：                               │
│    - 请求日志（ELK）                   │
│    - 限流（RateLimiter）              │
│    - 熔断（Resilience4j）            │
│    - API Key 管理（数据库）           │
└──────────────────────────────────────┘
```

### 8.2 Spring Cloud Gateway 示例

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: openai-route
          uri: https://api.openai.com
          predicates:
            - Path=/v1/**
            - Header=X-Model, gpt-.*
          filters:
            - AddRequestHeader=Authorization, Bearer ${OPENAI_KEY}

        - id: deepseek-route
          uri: https://api.deepseek.com
          predicates:
            - Path=/v1/**
            - Header=X-Model, deepseek.*
          filters:
            - AddRequestHeader=Authorization, Bearer ${DEEPSEEK_KEY}

        - id: local-vllm-route
          uri: http://localhost:8000
          predicates:
            - Path=/v1/**
            - Header=X-Model, qwen.*
```

### 8.3 完整架构

```text
┌────────────────────────────────────────────────┐
│                   客户端应用                     │
└──────────────────┬─────────────────────────────┘
                   │ OpenAI 格式请求
┌──────────────────▼─────────────────────────────┐
│              API Gateway (Nginx/Kong)           │
│              - SSL Termination                  │
│              - Rate Limiting                    │
│              - Request Logging                  │
└──────────────────┬─────────────────────────────┘
                   │
┌──────────────────▼─────────────────────────────┐
│            LLM Gateway (One-API/自研)           │
│   ┌─────────────────────────────────────┐     │
│   │  路由层                              │     │
│   │  ├── 模型 → 渠道映射                 │     │
│   │  ├── 负载均衡                        │     │
│   │  └── 故障转移                        │     │
│   ├─────────────────────────────────────┤     │
│   │  管理层                              │     │
│   │  ├── API Key 管理                    │     │
│   │  ├── 额度/计费                       │     │
│   │  └── 日志/审计                       │     │
│   └─────────────────────────────────────┘     │
└──────────────────┬─────────────────────────────┘
                   │
      ┌────────────┼────────────┐
      ▼            ▼            ▼
┌──────────┐ ┌──────────┐ ┌──────────┐
│ OpenAI   │ │ DeepSeek │ │ vLLM     │
│ API      │ │ API      │ │ 集群     │
└──────────┘ └──────────┘ └──────────┘
```

---

> 🎯 **核心要点**：团队超过 5 人或对接超过 3 个模型提供商时，就该引入网关。One-API（Go + Web UI）和 LiteLLM Proxy（Python + 灵活性）是两个最优解，选哪个取决于技术栈和是否需要 Web 管理界面。

---

**下一模块**：[05 - Java 生态集成实践](./05-Java生态集成实践.md)  
**返回总览**：[00 - OpenAI 兼容接口总览](./00-OpenAI兼容接口总览.md)
