# 04 推理实战与 serve 部署
> 从一行 pipeline 到生产服务：pipeline 家族、generate 参数、transformers serve 与 vLLM 分工

## 📚 目录
1. [pipeline：一行代码推理](#1-pipeline一行代码推理)
2. [generate 参数全解](#2-generate-参数全解)
3. [对话与 chat template](#3-对话与-chat-template)
4. [transformers serve：一条命令起服务](#4-transformers-serve一条命令起服务)
5. [v5 推理新能力：连续批处理与分页注意力](#5-v5-推理新能力连续批处理与分页注意力)
6. [与 vLLM 的分工](#6-与-vllm-的分工)
7. [推理性能基线](#7-推理性能基线)
8. [核心要点](#8-核心要点)

---

## 1. pipeline：一行代码推理

```python
from transformers import pipeline

# ① 文本生成（最常用）
pipe = pipeline("text-generation", model="Qwen/Qwen2.5-7B-Instruct")
out = pipe("写一首关于秋天的诗", max_new_tokens=100)
print(out[0]["generated_text"])

# ② 情感分类
cls = pipeline("sentiment-analysis", model="distilbert-base-multilingual-cased")
cls("这个产品太好用了！")   # [{'label': 'POSITIVE', 'score': 0.99}]

# ③ 其他任务
pipeline("summarization", model=...)       # 摘要
pipeline("translation", model=...)         # 翻译
pipeline("ner", model=...)                 # 实体识别
pipeline("question-answering", model=...)  # 问答
pipeline("image-classification", model=...)  # 图像分类
```

| pipeline 能力 | 说明 |
|--------------|------|
| 任务抽象 | `task + model_id` 即可，内部自动装三件套 |
| 硬件自动 | `device=0` 或 `device_map="auto"` |
| v5 新特性 | **原生异步推理**（async pipeline）、量化 MoE 一等支持 |
| 生产用途 | 快速验证/小流量/原型 |

> 🎯 **核心要点**：pipeline = "**任务的抽象**"——不用关心类名、输入格式、后处理。它是学习与原型的最快路径；生产高流量场景换 serve/vLLM（§4/§6）。

## 2. generate 参数全解

```python
outputs = model.generate(
    **inputs,
    max_new_tokens=512,              # 本次生成长度（区别于 max_length 含输入）
    temperature=0.7,                 # 采样温度（低=确定，高=发散）
    top_p=0.9,                       # 核采样：累积概率截断
    top_k=50,                        # 只从前 50 个候选采样
    repetition_penalty=1.1,          # 重复惩罚
    do_sample=True,                  # 采样模式（False=贪心）
    num_beams=1,                     # beam search（>1 质量高但慢）
    num_return_sequences=1,          # 返回几条
    eos_token_id=tokenizer.eos_token_id,   # 终止符
    pad_token_id=tokenizer.pad_token_id,   # 填充符（⚠️ 必须设）
)
text = tokenizer.decode(outputs[0], skip_special_tokens=True)
```

| 参数 | 作用 | 推荐值 |
|------|------|--------|
| `max_new_tokens` | 生成长度上限 | 按任务（对话 512-2048） |
| `temperature` | 随机性 | 0.1-0.3（事实）/ 0.7-0.9（创意） |
| `top_p` | 核采样 | 0.9 |
| `top_k` | 候选截断 | 50 或不用 |
| `repetition_penalty` | 防重复 | 1.0-1.2 |
| `do_sample` | 采样开关 | 创意任务 True |
| `num_beams` | 束搜索 | 质量优先 3-5（慢） |
| `use_cache=True` | KV 缓存加速 | 保持默认 True（v5 训练默认 False 与此无关） |

> ⚠️ **generate 两个必设**：`pad_token_id`（未设会警告/报错）与 `eos_token_id`（不设可能停不下来）。解码后 `skip_special_tokens=True` 去掉特殊 token（`<|im_end|>` 等）。

## 3. 对话与 chat template

```python
from transformers import AutoModelForCausalLM, AutoTokenizer

tokenizer = AutoTokenizer.from_pretrained("Qwen/Qwen2.5-7B-Instruct")
model = AutoModelForCausalLM.from_pretrained("Qwen/Qwen2.5-7B-Instruct",
                                             device_map="auto", torch_dtype="auto")

messages = [
    {"role": "system", "content": "你是专业的客服助手，回答简洁准确。"},
    {"role": "user", "content": "我的订单 20260801001 到哪了？"},
]

# ⭐ chat template：自动套用模型的对话格式（<|im_start|> 等特殊 token）
inputs = tokenizer.apply_chat_template(
    messages,
    tokenize=True,          # v5 返回 BatchEncoding，直接喂模型
    add_generation_prompt=True,   # 加上模型开始回答的提示
    return_tensors="pt",
).to(model.device)

outputs = model.generate(**inputs, max_new_tokens=512)
print(tokenizer.decode(outputs[0][inputs.shape[1]:], skip_special_tokens=True))
```

| chat template 要点 | 说明 |
|-------------------|------|
| 为什么必须用 | 每个模型的对话格式不同（Qwen `<|im_start|>`、LLaMA `<|begin_of_text|>`） |
| 换模型零改动 | template 存在模型仓库，自动加载 |
| 多轮对话 | messages 列表越长，前面轮次全带上 |
| v5 返回 BatchEncoding | 直接 `**inputs` 喂模型（02 章 §3 迁移坑） |
| `add_generation_prompt` | 让模型知道"该你回答了" |

> 🎯 **核心要点**：**手写对话模板 = 自找麻烦**。`apply_chat_template` 一行解决格式问题，换模型不换代码。多轮对话 = 把历史消息全放 messages 列表（上下文管理在调用侧，07 章 Datasets 有数据侧视角）。

## 4. transformers serve：一条命令起服务

**v5 新命令**：从 checkpoint 直接起 OpenAI 兼容的推理服务：

```bash
# 起服务（默认 8000 端口，OpenAI 兼容 API）
transformers serve --model Qwen/Qwen2.5-7B-Instruct \
    --device cuda:0 \
    --dtype bfloat16 \
    --port 8000
```

```python
# 客户端：OpenAI SDK 直接调（兼容 /v1/chat/completions）
from openai import OpenAI
client = OpenAI(base_url="http://localhost:8000/v1", api_key="unused")

resp = client.chat.completions.create(
    model="Qwen/Qwen2.5-7B-Instruct",
    messages=[{"role": "user", "content": "你好"}],
)
print(resp.choices[0].message.content)
```

| 场景 | 用 serve | 用 vLLM |
|------|:---:|:---:|
| 开发/原型 | ✅ | — |
| 内网小流量（<50 QPS） | ✅ | — |
| 生产高并发（100+ QPS） | — | ✅ |
| 多卡张量并行生产 | 支持 | ✅ 更成熟 |
| 不想装额外依赖 | ✅（内置） | — |

> 🎯 **核心要点**：`transformers serve` = "**HF 自带的 OpenAI 兼容服务器**"——开发、内网、小流量场景一条命令搞定，零额外依赖；生产大流量再上 vLLM（§6 对比）。2026 年"起个 LLM 服务"的最低门槛就是这条命令。

## 5. v5 推理新能力：连续批处理与分页注意力

```python
# v5 起 from_pretrained 支持（推理引擎级能力下放）：
model = AutoModelForCausalLM.from_pretrained(
    "Qwen/Qwen2.5-7B-Instruct",
    # Continuous batching：请求动态拼批，长请求不再阻塞短请求
    continuous_batching=True,
    # Paged attention：注意力 KV 缓存分页，长序列省显存
    attn_implementation="paged_attention",
    # 与 Flash Attention / 量化 / 融合叠加
)
```

| 能力 | 解决的问题 | 效果 |
|------|-----------|------|
| Continuous batching | 静态批处理下"长请求拖垮整批" | 吞吐提升数倍（服务场景） |
| Paged attention | KV 缓存碎片化浪费显存 | 长上下文/高并发省显存 |
| 动态权重加载 | 启动慢/重复转换 | 加载即优化（02 章 §4） |

> 💡 这些能力 vLLM 早已实现（2024-2025），v5 把它们收进 Transformers——**"低配版 vLLM"进标准库**。理解：连续批处理 = 服务端动态拼批；分页注意力 = KV 缓存按页分配（模型推理部署体系有原理深潜）。

## 6. 与 vLLM 的分工

| 维度 | transformers serve | vLLM |
|------|:---:|:---:|
| 定位 | 官方兜底 | 专业推理引擎 |
| 吞吐（同卡同模型） | 基准 | 高 3-10 倍（PagedAttention 成熟） |
| 并发 | 有限 | 连续批处理 + 抢占式调度成熟 |
| 量化支持 | bitsandbytes/TorchAO | GPTQ/AWQ/SQLite 多样 |
| 长上下文 | 支持 | 更强（分页 + 前缀缓存） |
| 依赖 | 零额外 | vllm 包 |
| 适用 | 开发/小流量/兜底 | 生产高吞吐 |

```text
选型决策树：
  流量 < 50 QPS / 原型 / 内网 → transformers serve（零依赖）
  流量 50-500 QPS / 生产 → vLLM（吞吐/并发）
  本地/边缘 → llama.cpp / Ollama
  混合 → serve 兜底 + vLLM 扩容（同一 OpenAI 协议，切换无感）
```

> 🎯 **核心要点**：**协议统一（OpenAI 兼容）让引擎切换无感**——serve/vLLM/llama.cpp 客户端代码完全一样，只是 `base_url` 不同。生产架构 = "协议在前，引擎在后"，随时可换。

## 7. 推理性能基线

| 指标 | 意义 | 7B 模型参考（单卡 A100） |
|------|------|:---:|
| 首 token 延迟（TTFT） | 用户等待感 | 100-500ms |
| 生成吞吐（token/s） | 每请求速度 | 30-80 token/s |
| 总吞吐（QPS） | 系统容量 | serve: 低 / vLLM: 高 |
| 显存占用 | 卡选择 | bf16 ≈ 14GB + KV 缓存 |

> 💡 量化与吞吐的关系：**4bit 量化显存省 70%，但吞吐可能略降**（反量化开销）——卡不够用才量化，卡够用 bf16 吞吐更好。压测方法论与调优见 09 章 + 模型推理部署体系。

## 8. 核心要点

| 序号 | 要点 |
|:---:|------|
| 1 | pipeline = 任务抽象：task + model_id 一行推理 |
| 2 | generate 参数：max_new_tokens/temperature/top_p 是核心三件 |
| 3 | pad_token_id 与 eos_token_id 必设；decode 用 skip_special_tokens |
| 4 | 对话必须 apply_chat_template（v5 返回 BatchEncoding） |
| 5 | transformers serve = 一条命令的 OpenAI 兼容服务（小流量兜底） |
| 6 | v5 推理新能力：continuous_batching + paged_attention 进标准库 |
| 7 | 生产高吞吐用 vLLM；协议统一让引擎切换无感 |
| 8 | 性能基线：TTFT 100-500ms / 30-80 token/s（7B 单卡） |

---

**下一模块**：[05-微调与 Trainer](05-微调与Trainer.md) / **返回总览**：[00-HuggingFace知识体系总览](00-HuggingFace知识体系总览.md)

## 参考来源

- [Transformers 官方：pipeline 指南](https://huggingface.co/docs/transformers/en/pipeline_tutorial)
- [Transformers 官方：generate 参数](https://huggingface.co/docs/transformers/en/main_classes/text_generation)
- [Transformers 官方：chat template](https://huggingface.co/docs/transformers/en/chat_templates)
- [Transformers v5 发布说明（serve/连续批处理/分页注意力）](https://github.com/huggingface/transformers/releases/tag/v5.0.0)
- [07-模型部署与工程化/模型推理与部署（vLLM 对比专题）](../../07-模型部署与工程化/模型推理与部署/00-模型推理与部署体系总览.md)
