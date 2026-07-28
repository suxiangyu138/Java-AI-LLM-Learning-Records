# SDK 与工具生态
> 利用 Anthropic 兼容接口，可以在不修改业务代码的前提下切换底层模型供应商

## 📚 目录
1. [Anthropic SDK 使用](#1-anthropic-sdk-使用)
2. [统一 API 网关](#2-统一-api-网关)
3. [前端工具集成](#3-前端工具集成)
4. [Java 生态集成](#4-java-生态集成)

---

## 1. Anthropic SDK 使用

### 1.1 Python SDK

**安装**：

```bash
pip install anthropic
```

**基本使用**：

```python
from anthropic import Anthropic

# 切换 base_url 即可指向兼容服务
client = Anthropic(
    base_url="https://api.deepseek.com/beta",   # ← 替换为兼容服务端点
    api_key="sk-xxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
)

message = client.messages.create(
    model="deepseek-chat",
    max_tokens=1024,
    temperature=0.7,
    system="You are a helpful assistant.",
    messages=[
        {"role": "user", "content": "Hello!"}
    ]
)

print(message.content[0].text)
```

**流式输出**：

```python
stream = client.messages.create(
    model="deepseek-chat",
    max_tokens=1024,
    stream=True,
    messages=[{"role": "user", "content": "Write a short poem."}]
)

for event in stream:
    if event.type == "content_block_delta" and event.delta.type == "text_delta":
        print(event.delta.text, end="", flush=True)
```

### 1.2 TypeScript / JavaScript SDK

**安装**：

```bash
npm install @anthropic-ai/sdk
```

**基本使用**：

```typescript
import Anthropic from '@anthropic-ai/sdk';

const client = new Anthropic({
  baseURL: 'https://api.deepseek.com/beta',  // ← 替换为兼容服务端点
  apiKey: 'sk-xxxxxxxxxxxxxxxxxxxxxxxxxxxxx',
});

const response = await client.messages.create({
  model: 'deepseek-chat',
  max_tokens: 1024,
  messages: [{ role: 'user', content: 'Hello!' }],
});

console.log(response.content[0].text);
```

### 1.3 Java SDK

**安装** (Maven)：

```xml
<dependency>
    <groupId>com.anthropic</groupId>
    <artifactId>anthropic-java</artifactId>
    <version>0.8.0</version>
</dependency>
```

**基本使用**：

```java
import com.anthropic.Anthropic;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageParam;
import com.anthropic.models.messages.ContentBlock;

Anthropic client = Anthropic.builder()
    .apiKey("sk-xxxxxxxxxxxxxxxxx")
    .baseUrl("https://api.deepseek.com/beta")  // ← 替换
    .build();

Message message = client.messages().create(MessageCreateParams.builder()
    .model("deepseek-chat")
    .maxTokens(1024)
    .addUserMessage("Hello!")
    .build());

System.out.println(message.content().get(0).text());
```

### 1.4 SDK 配置切换模板

```python
import os

ANTHROPIC_BASE_URLS = {
    "anthropic": "https://api.anthropic.com",
    "deepseek": "https://api.deepseek.com/beta",
    "siliconflow": "https://api.siliconflow.cn/v1",
    "custom": os.getenv("CUSTOM_BASE_URL", ""),
}

def get_client(provider: str = "anthropic") -> Anthropic:
    api_key = os.getenv(f"{provider.upper()}_API_KEY")
    base_url = ANTHROPIC_BASE_URLS.get(provider)
    return Anthropic(base_url=base_url, api_key=api_key)
```

> 💡 **核心技巧**：所有兼容服务只需要修改 `base_url` 和 `api_key`，业务代码无需调整。

---

## 2. 统一 API 网关

### 2.1 one-api

| 属性 | 说明 |
|------|------|
| 项目地址 | [songquanpeng/one-api](https://github.com/songquanpeng/one-api) |
| 语言 | Go |
| 核心功能 | 多 Provider 管理、密钥分发、配额控制、日志审计 |

**支持的格式**：

| 输入格式 | 输出格式 | 转换能力 |
|---------|---------|:--------:|
| OpenAI | OpenAI | ✅ |
| OpenAI | Anthropic | ✅ (渠道设置 → 返回格式选择) |
| Anthropic | Anthropic | ✅ |
| Anthropic | OpenAI | ✅ |

**部署方式**：

```bash
docker run --name one-api -d \
  -p 3000:3000 \
  -v /data/one-api:/data \
  -e SQL_DSN="one-api.db" \
  justsong/one-api
```

### 2.2 new-api

| 属性 | 说明 |
|------|------|
| 项目地址 | [Calcium-Ion/new-api](https://github.com/Calcium-Ion/new-api) |
| 说明 | one-api 的增强分支，新增更多 Provider 支持 |
| 亮点 | 支持更多国内模型（百度、字节等）的格式转换 |

### 2.3 LiteLLM

| 属性 | 说明 |
|------|------|
| 项目地址 | [BerriAI/litellm](https://github.com/BerriAI/litellm) |
| 语言 | Python |
| 核心功能 | 100+ LLM 的统一调用接口，支持 Anthropic 格式输入 |

### 2.4 网关推荐配置

**场景一：多模型统一入口**

```
用户 → Anthropic SDK → one-api (统一网关)
                         ├── DeepSeek (直接兼容)
                         ├── OpenAI (格式转换)
                         ├── 硅基流动 (直接兼容)
                         └── 阿里云百炼 (格式转换)
```

**场景二：故障转移**

```yaml
# one-api 渠道配置示意
渠道1: DeepSeek (主)   权重: 5
渠道2: 硅基流动 (备)    权重: 3
渠道3: Anthropic (备)  权重: 2
```

---

## 3. 前端工具集成

### 3.1 LobeChat

| 属性 | 说明 |
|------|------|
| 项目 | [lobehub/lobe-chat](https://github.com/lobehub/lobe-chat) |
| 配置方式 | 自定义 OpenAI 兼容端点 |
| Anthropic 兼容 | 通过自定义模型提供商配置使用 Anthropic 格式 |

**配置示例**（环境变量）：

```env
# 使用 DeepSeek 的 Anthropic 兼容接口
NEXT_PUBLIC_DEEPSEEK_BASE_URL=https://api.deepseek.com/beta
DEEPSEEK_API_KEY=sk-xxx
```

### 3.2 NextChat (ChatGPT-Next-Web)

| 属性 | 说明 |
|------|------|
| 项目 | [ChatGPTNextWeb/NextChat](https://github.com/ChatGPTNextWeb/NextChat) |
| Anthropic 兼容 | 通过自定义接口配置使用 |

### 3.3 Open WebUI

| 属性 | 说明 |
|------|------|
| 项目 | [open-webui/open-webui](https://github.com/open-webui/open-webui) |
| OpenAI 兼容 | 原生支持 OpenAI 格式 |
| Anthropic 兼容 | 需通过 one-api 网关转换 |

### 3.4 与 Anthropic 原生对比

| 前端工具 | 原生 Anthropic 支持 | 兼容接口支持 | 推荐方式 |
|---------|:------------------:|:-----------:|---------|
| LobeChat | ✅ | ✅ | 直接配置兼容端点 |
| NextChat | ✅ | ✅ | 直接配置兼容端点 |
| Open WebUI | ❌ | 🔄 | one-api 转换 |
| Claude Code (CLI) | ✅ | 有限 | 配置 `/model` 切换 |

---

## 4. Java 生态集成

### 4.1 Spring AI

Spring AI 提供了统一的 AI 客户端抽象，通过切换 `ChatClient` 实现：

```java
@Bean
public ChatClient anthropicChatClient() {
    return ChatClient.builder(anthropicChatModel()).build();
}

private AnthropicChatModel anthropicChatModel() {
    return new AnthropicChatModel(anthropicApi());
}

private AnthropicApi anthropicApi() {
    return new AnthropicApi("https://api.deepseek.com/beta", "sk-xxx");
}
```

### 4.2 LangChain4j

```java
AnthropicChatModel model = AnthropicChatModel.builder()
    .baseUrl("https://api.deepseek.com/beta")
    .apiKey("sk-xxx")
    .modelName("deepseek-chat")
    .maxTokens(1024)
    .build();

String response = model.generate("Hello!");
```

> 💡 LangChain4j 的 `AnthropicChatModel` 直接基于 Anthropic Messages API 实现，切换 base_url 即可连接兼容服务。

### 4.3 Spring Boot 配置示例

```yaml
# application.yml
anthropic:
  base-url: https://api.deepseek.com/beta
  api-key: ${DEEPSEEK_API_KEY}
  model: deepseek-chat
  max-tokens: 1024

# 切换为 Anthropic 原生
# anthropic:
#   base-url: https://api.anthropic.com
#   api-key: ${ANTHROPIC_API_KEY}
#   model: claude-sonnet-5-20251001
```

> 🎯 **核心要点**：Anthropic 兼容接口的最大价值在于 SDK 层面的无缝切换——只需修改 `base_url` 配置即可切换底层模型。对于生产环境，推荐使用 one-api 网关统一管理多 Provider 的密钥、路由和配额。

---

**下一模块**：[04-协议差异对比](04-协议差异对比.md) / [**返回总览**](00-Anthropic兼容接口总览.md)
