# 08 - 国产大模型 API 全景

> 🎯 国产大模型已形成完整梯队——通义千问 (Qwen)、智谱 GLM、Kimi (Moonshot)、豆包 (字节)、文心一言 (百度) 等。它们共同的特点：全部兼容 OpenAI API 格式、中文能力突出、价格极具竞争力

> **前置阅读**：[[02-大模型API调用实践]]、[[10-主流大模型API速查与选型指南]]

---

## 目录

1. [国产模型全景对比](#1-国产模型全景对比)
2. [通义千问 Qwen API](#2-通义千问-qwen-api)
3. [智谱 GLM API](#3-智谱-glm-api)
4. [Moonshot Kimi API](#4-moonshot-kimi-api)
5. [其他平台速览](#5-其他平台速览)
6. [选型指南](#6-选型指南)

---

## 1. 国产模型全景对比

| 平台 | 公司 | 代表模型 | 上下文 | 特色 | 价格 (输出 1M) |
|------|------|---------|:---:|------|:---:|
| **通义千问** | 阿里 | Qwen-Max/Qwen-Plus | 128K | Apache 2.0 开源 | ¥8-60 |
| **智谱 GLM** | 智谱 AI | GLM-4-Plus/Flash/Air | 128K | 国产合规+多模态 | ¥4-50 |
| **Kimi** | Moonshot | moonshot-v1-128k | **128K** | 超长上下文+文件 | ¥12-60 |
| **豆包** | 字节跳动 | Doubao-Pro/Lite | 128K | Function Calling | ¥0.8-8 |
| **文心一言** | 百度 | ERNIE-4.0-Turbo | 128K | 百度生态集成 | ¥12-120 |
| **DeepSeek** | DeepSeek | V3/R1 | 64K | 极致性价比 | ¥0.42-1.1 |
| **百川** | 百川智能 | Baichuan4 | 128K | 中文+联网搜索 | ¥3-30 |

> 💡 所有国产大模型**全部兼容 OpenAI SDK**，切换到国产模型只需改 `base_url` + `api_key` + `model`

## 2. 通义千问 Qwen API

### 2.1 快速调用

```python
from openai import OpenAI

client = OpenAI(
    api_key="sk-your-dashscope-key",
    base_url="https://dashscope.aliyuncs.com/compatible-mode/v1"
)

response = client.chat.completions.create(
    model="qwen-max",
    messages=[
        {"role": "system", "content": "你是一个Java后端专家"},
        {"role": "user", "content": "解释MySQL索引优化原理"}
    ]
)
```

### 2.2 模型选择

| 模型 | 定位 | 价格 (输出 1K tokens) | 场景 |
|------|------|:---:|------|
| `qwen-max` | 旗舰 | ¥0.06 | 复杂推理/代码 |
| `qwen-plus` | 均衡 | ¥0.02 | 日常使用推荐 |
| `qwen-turbo` | 快速 | ¥0.008 | 简单任务/高并发 |
| `qwen-long` | 超长上下文 | ¥0.02 | 文档分析/知识库 |

### 2.3 本地 Ollama 部署

```bash
# Qwen 有全尺寸开源版本，可本地部署
ollama pull qwen2.5:7b
ollama pull qwen2.5:32b
ollama pull qwen2.5:72b

# API 调用 (OpenAI 兼容)
curl http://localhost:11434/v1/chat/completions \
  -d '{"model":"qwen2.5:7b","messages":[{"role":"user","content":"你好"}]}'
```

> 🎯 Qwen 系列是 Apache 2.0 许可证最宽松的国产模型，可商用可修改

## 3. 智谱 GLM API

### 3.1 基础调用

```python
from openai import OpenAI

client = OpenAI(
    api_key="your-zhipu-api-key",
    base_url="https://open.bigmodel.cn/api/paas/v4"
)

response = client.chat.completions.create(
    model="glm-4-plus",
    messages=[
        {"role": "user", "content": "分析这段代码的时间复杂度"}
    ]
)
```

### 3.2 模型矩阵

| 模型 | 特色 |
|------|------|
| `glm-4-plus` | 旗舰，多模态+FC |
| `glm-4-flash` | 免费！轻量快速 |
| `glm-4-air` | 极致性价比 |
| `glm-4-long` | 128K 超长上下文 |
| `glm-4v-plus` | 多模态视觉 |

> 💡 GLM-4-Flash **完全免费**，适合学习和原型的零成本方案

## 4. Moonshot Kimi API

### 4.1 核心特点

```text
Kimi 的差异化：
├── 128K 超长上下文 → 一次上传整个项目的代码 
├── 文件上传原生支持 → PDF/Word/PPT/TXT/图片
├── 联网搜索 → 实时信息检索
└── 中文理解深度 → 国内用户反馈最佳之一
```

### 4.2 文件上传 + 问答

```python
from openai import OpenAI

client = OpenAI(
    api_key="sk-your-moonshot-key",
    base_url="https://api.moonshot.cn/v1"
)

# 1. 上传文件
file = client.files.create(
    file=open("project_spec.pdf", "rb"),
    purpose="file-extract"
)

# 2. 基于文件提问
response = client.chat.completions.create(
    model="moonshot-v1-128k",
    messages=[
        {"role": "system", "content": "file_ids: [" + file.id + "]"},
        {"role": "user", "content": "总结这份需求文档的核心功能点"}
    ]
)
```

### 4.3 Kimi 特有功能

```python
# 联网搜索（Kimi 的特色能力）
response = client.chat.completions.create(
    model="moonshot-v1-128k",
    messages=[{
        "role": "user",
        "content": "2026年Java生态最新趋势是什么？"
    }],
    extra_body={"search": True}   # ← 启用联网搜索
)
```

## 5. 其他平台速览

### 5.1 豆包（字节跳动）

```python
client = OpenAI(
    api_key="your-volcengine-key",
    base_url="https://ark.cn-beijing.volces.com/api/v3"
)
# 模型: doubao-pro-32k, doubao-lite-32k
# 特色: 字节生态集成（飞书/抖音）+ Function Calling
```

### 5.2 文心一言（百度）

```python
# 百度有自己的 SDK，但也提供 OpenAI 兼容接口
import requests

resp = requests.post(
    "https://aip.baidubce.com/rpc/2.0/ai_custom/v1/wenxinworkshop/chat/completions_pro",
    params={"access_token": get_access_token()},
    json={
        "messages": [{"role": "user", "content": "你好"}]
    }
)
# 注意：文心一言需要先获取 access_token（OAuth 2.0）
```

### 5.3 统一适配器模式

```python
class LLMUnifiedClient:
    """国产大模型统一客户端——一套代码切换所有平台"""

    PROVIDERS = {
        "deepseek": {
            "base_url": "https://api.deepseek.com",
            "default_model": "deepseek-chat"
        },
        "qwen": {
            "base_url": "https://dashscope.aliyuncs.com/compatible-mode/v1",
            "default_model": "qwen-plus"
        },
        "glm": {
            "base_url": "https://open.bigmodel.cn/api/paas/v4",
            "default_model": "glm-4-flash"
        },
        "moonshot": {
            "base_url": "https://api.moonshot.cn/v1",
            "default_model": "moonshot-v1-128k"
        }
    }

    def __init__(self, provider, api_key):
        config = self.PROVIDERS[provider]
        self.client = OpenAI(
            api_key=api_key,
            base_url=config["base_url"]
        )
        self.model = config["default_model"]

    def chat(self, prompt, model=None):
        return self.client.chat.completions.create(
            model=model or self.model,
            messages=[{"role": "user", "content": prompt}]
        )
```

## 6. 选型指南

```text
选择国产大模型 → 你的首要需求是什么？
│
├── 💰 成本最低
│   ├── 开发/学习 → GLM-4-Flash（免费）
│   ├── 生产/大量调用 → DeepSeek V3（极致便宜）
│   └── 本地部署 → Qwen 2.5 GGUF（零 API 费用）
│
├── 🇨🇳 中文最好
│   ├── 日常对话 → Qwen-Max 或 Kimi
│   ├── 企业合规 → GLM-4（国产审核合规）
│   └── 创意写作 → Kimi（中文写作口碑最佳）
│
├── 📄 超长文档
│   └── Kimi (128K + 文件上传原生支持)
│
├── 🔓 需要开源
│   └── Qwen 2.5（Apache 2.0，全尺寸开源）
│
├── 🔧 需要 Function Calling
│   ├── Qwen-Plus/Max
│   ├── GLM-4-Plus
│   └── 豆包 Pro
│
└── 🏢 百度生态
    └── 文心一言（深度绑定百度云/搜索）
```

## 核心要点回顾

- 所有国产模型 100% 兼容 OpenAI SDK，只需改 3 行代码
- Qwen = 开源友好 (Apache 2.0) + 全尺寸部署
- GLM-4-Flash = 完全免费，学习原型首选
- Kimi = 128K 超长上下文 + 文件上传 + 联网搜索
- DeepSeek = 性价比之王（但非国产合规审核体系）
- 统一适配器模式：一套代码 + 字典切换 = 所有平台

## 参考资料

1. 阿里云 DashScope API 文档 — help.aliyun.com
2. 智谱 AI 开放平台 — open.bigmodel.cn
3. Moonshot AI 开发者文档 — platform.moonshot.cn
4. DeepSeek 开放平台 — platform.deepseek.com
