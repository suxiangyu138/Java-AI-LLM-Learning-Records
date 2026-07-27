# 07 - 快速精通 DeepSeek API

> 🎯 DeepSeek 是当前性价比最高的开源/商业大模型——V3 通用推理、R1 深度推理、FIM 代码补全，三合一 API 统一入口。API 完全兼容 OpenAI 格式，迁移成本几乎为零，且价格仅为 GPT-4 的 1/50

> **前置阅读**：[[02-大模型API调用实践]]、[[04-快速精通GPT]]

---

## 目录

1. [DeepSeek 模型全景](#1-deepseek-模型全景)
2. [API 基础调用](#2-api-基础调用)
3. [推理模式 (R1) 使用](#3-推理模式-r1-使用)
4. [FIM 代码补全](#4-fim-代码补全)
5. [流式输出与成本对比](#5-流式输出与成本对比)

---

## 1. DeepSeek 模型全景

| 模型 | API ID | 定位 | 上下文 | 特色 |
|------|--------|------|:---:|------|
| **DeepSeek V3** | `deepseek-chat` | 通用对话 | 64K | 旗舰模型，日常使用首选 |
| **DeepSeek R1** | `deepseek-reasoner` | 深度推理 | 64K | CoT 推理链，数学/逻辑专用 |
| **DeepSeek Coder** | `deepseek-coder` | 代码生成 | 128K | FIM 补全 + 项目级理解 |

### 成本对比（令人震惊的性价比）

| 模型 | 输入 (1M tokens) | 输出 (1M tokens) | vs GPT-4o |
|------|:---:|:---:|:---:|
| DeepSeek V3 | **$0.27** | **$1.10** | ~1/50 |
| DeepSeek R1 | $0.55 | $2.19 | ~1/30 |
| GPT-4o | $2.50 | $10.00 | 基准 |
| Claude Sonnet 5 | $3.00 | $15.00 | 更贵 |

> 🎯 DeepSeek V3 输出 100 万 Token 只要 $1.10，相当于一本《三体》全集（约 90 万 tokens）的生成费用不到 8 元人民币

## 2. API 基础调用

### 2.1 OpenAI SDK 直接复用

```python
# DeepSeek API 完全兼容 OpenAI SDK，只需改 base_url
from openai import OpenAI

client = OpenAI(
    api_key="sk-your-deepseek-key",
    base_url="https://api.deepseek.com"
)

response = client.chat.completions.create(
    model="deepseek-chat",    # = V3
    messages=[
        {"role": "system", "content": "You are a helpful assistant"},
        {"role": "user", "content": "Explain MoE architecture"}
    ],
    temperature=0.7,
    max_tokens=1024
)
print(response.choices[0].message.content)
```

### 2.2 原生 HTTP 调用

```python
import requests

headers = {
    "Authorization": "Bearer sk-your-key",
    "Content-Type": "application/json"
}

data = {
    "model": "deepseek-chat",
    "messages": [{"role": "user", "content": "What is FIM?"}],
    "stream": False
}

resp = requests.post(
    "https://api.deepseek.com/v1/chat/completions",
    headers=headers, json=data
)
print(resp.json()["choices"][0]["message"]["content"])
```

### 2.3 Java Spring Boot 集成

```java
@Configuration
public class DeepSeekConfig {
    @Bean
    public RestClient deepseekClient(
        @Value("${deepseek.api-key}") String apiKey
    ) {
        return RestClient.builder()
            .baseUrl("https://api.deepseek.com")
            .defaultHeader("Authorization", "Bearer " + apiKey)
            .build();
    }
}

@Service
public class DeepSeekService {
    private final RestClient client;

    public String chat(String prompt) {
        var resp = client.post()
            .uri("/v1/chat/completions")
            .body(Map.of(
                "model", "deepseek-chat",
                "messages", List.of(Map.of("role", "user", "content", prompt))
            ))
            .retrieve()
            .body(new ParameterizedTypeReference<Map<String,Object>>() {});

        var choices = (List) resp.get("choices");
        var message = (Map) ((Map) choices.get(0)).get("message");
        return (String) message.get("content");
    }
}
```

## 3. 推理模式 (R1) 使用

### 3.1 切换到推理模型

```python
# 只需改 model 名称
response = client.chat.completions.create(
    model="deepseek-reasoner",   # ← R1 推理模型
    messages=[
        {"role": "user", "content": """
        A bat and a ball cost $1.10 in total.
        The bat costs $1.00 more than the ball.
        How much does the ball cost?
        """}
    ]
)

# R1 的响应结构有所不同
message = response.choices[0].message
# message.reasoning_content  ← 推理过程（CoT）
# message.content           ← 最终答案
```

### 3.2 R1 响应特征

```text
R1 响应 = reasoning_content + content

reasoning_content:  模型内部推理链（消耗 output tokens 但不展示给用户?）
content:            最终的简洁答案

Token 计费：
  输入 tokens  + 推理 tokens (reasoning_content) + 输出 tokens (content)
  R1 输出比 V3 贵约 2x，因为包含推理链
```

### 3.3 R1 vs V3 选择指南

| 场景 | 推荐 | 原因 |
|------|:---:|------|
| 日常对话 | V3 | 足够好，更便宜 |
| 数学推理 | **R1** | CoT 推理显著提升准确率 |
| 代码调试 | R1 | 定位 bug 更准 |
| 逻辑分析 | R1 | 多步推理能力 |
| 翻译+摘要 | V3 | 不需要深度推理 |
| 创意写作 | V3 | R1 的推理链打断行文 |

## 4. FIM 代码补全

### 4.1 Fill-in-the-Middle 格式

```python
# FIM (Fill-in-the-Middle) 专用端点
response = client.completions.create(
    model="deepseek-coder",
    prompt="def binary_search(arr, target):\n",
    suffix="\n    return -1",       # ← 后缀（函数结尾）
    max_tokens=256,
    temperature=0.2,
    stop=["\n\n"]
)

# 模型会补全 prompt 和 suffix 之间的代码
print(response.choices[0].text)
# → "    left, right = 0, len(arr) - 1\n    while left <= right:\n..."
```

### 4.2 FIM 适用场景

```text
FIM = Fill-in-the-Middle（中间填充）

典型场景：
├── IDE 代码补全：光标位置（prompt）+ 光标后代码（suffix）
├── 代码重构：选中片段 → 替换为优化版
└── 文档补全：段落中间插入内容
```

## 5. 流式输出与成本对比

### 5.1 流式调用

```python
stream = client.chat.completions.create(
    model="deepseek-chat",
    messages=[{"role": "user", "content": "Write a Python script"}],
    stream=True
)

for chunk in stream:
    if chunk.choices[0].delta.content:
        print(chunk.choices[0].delta.content, end="", flush=True)
```

### 5.2 全平台成本对比

| 模型 | 输入 $/1M | 输出 $/1M | 日调用 10 万次 (500 tokens/次) 月成本 |
|------|:---:|:---:|:---:|
| DeepSeek V3 | $0.27 | $1.10 | ~$21 |
| DeepSeek R1 | $0.55 | $2.19 | ~$42 |
| GPT-4o | $2.50 | $10.00 | ~$188 |
| Claude Sonnet | $3.00 | $15.00 | ~$270 |

> 🎯 从 GPT-4o 迁移到 DeepSeek V3，API 成本直接降至 1/10

## 核心要点回顾

- DeepSeek API 完全兼容 OpenAI SDK，只需改 `base_url`
- V3 (`deepseek-chat`) = 日常主力，R1 (`deepseek-reasoner`) = 深度推理
- R1 的 `reasoning_content` 包含推理链，需单独处理
- FIM 代码补全用 `prompt` + `suffix` 参数，IDE 场景专用
- 价格是最大优势：V3 比 GPT-4o 便宜 50 倍
- 缺点：并发限制较严格，高峰期可能排队

## 参考资料

1. DeepSeek API 官方文档 — platform.deepseek.com
2. DeepSeek V3/R1 技术报告
