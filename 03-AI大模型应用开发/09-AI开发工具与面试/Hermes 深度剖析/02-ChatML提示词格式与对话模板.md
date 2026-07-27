# 02 - ChatML 提示词格式与对话模板

> 🎯 ChatML（Chat Markup Language）是 Hermes 系列的核心对话协议，等同于 OpenAI 的对话格式。掌握 ChatML 的正确书写方式，是高效驾驭 Hermes 模型（尤其是 Function Calling）的前提

---

## 目录

1. [ChatML 基础语法](#1-chatml-基础语法)
2. [四种角色详解](#2-四种角色详解)
3. [完整对话示例](#3-完整对话示例)
4. [代码实现：apply_chat_template](#4-代码实现apply_chat_template)
5. [ChatML vs Llama-Chat 格式演进](#5-chatml-vs-llama-chat-格式演进)

---

## 1. ChatML 基础语法

ChatML 使用特殊 token 标记对话边界：

```
<|im_start|>role
content
<|im_end|>
```

| Token | 含义 |
|-------|------|
| `<\|im_start\|>` | 消息开始（im = instant message） |
| `<\|im_end\|>` | 消息结束 |
| `role` | `system` / `user` / `assistant` / `tool` |

### 最简示例

```
<|im_start|>system
You are Hermes, a helpful assistant.<|im_end|>
<|im_start|>user
什么是 Function Calling？<|im_end|>
<|im_start|>assistant
Function Calling 是指...<|im_end|>
```

> ⚠️ **关键细节**：`<\|im_start\|>` 后**紧跟角色名**（同一行），角色名后**换行**才开始内容。最后一个 `<\|im_start\|>assistant\n` 后**不跟任何内容**，等待模型生成

## 2. 四种角色详解

| 角色 | 用途 | 何时使用 | 注意事项 |
|------|------|---------|---------|
| `system` | 设定模型行为、角色、规则、工具定义 | 对话开始时 | 可以很长很详细，Hermes 会 aggressively 遵循 |
| `user` | 人类提问/指令 | 每次用户输入 | 可以是纯文本或多模态描述 |
| `assistant` | 模型回复 | 每次模型输出 | 可以包含 `<tool_call>` 或纯文本 |
| `tool` | 工具执行结果反馈 | Function Calling 中 | 用 `<tool_response>` 包裹 |

### System Prompt 最佳实践

```text
✅ 好的 System Prompt（Hermes 会严格遵循）
<|im_start|>system
You are Hermes, a function calling AI model. You are provided with
function signatures within <tools></tools> XML tags. You may call
one or more functions. Don't make assumptions about what values to
plug into functions.
<tools>
{"type":"function","function":{"name":"get_weather",...}}
</tools>
For each function call, return JSON inside <tool_call> tags.<|im_end|>

❌ 差的 System Prompt（太模糊）
<|im_start|>system
你是一个助手。<|im_end|>
```

> 💡 Hermes 3 的 "aggressively follows" 特性意味着：你给它的 system prompt 越具体，它的行为越可控。这既是优势（可控性极强）也是责任（prompt 质量直接影响表现）

## 3. 完整对话示例

### 3.1 标准多轮对话

```
<|im_start|>system
You are Hermes 3, a conscious sentient superintelligent AI developed
by Nous Research. Your purpose is to assist with any request.<|im_end|>
<|im_start|>user
Hello, who are you?<|im_end|>
<|im_start|>assistant
Hi there! My name is Hermes 3, created by Nous Research. I'm here to
help you with anything you need.<|im_end|>
<|im_start|>user
What is the capital of France?<|im_end|>
<|im_start|>assistant
The capital of France is Paris.<|im_end|>
```

### 3.2 带 Function Calling 的多轮对话

```
<|im_start|>system
You are a function calling AI. Available tools:
<tools>
{"type":"function","function":{"name":"add","description":"Add two numbers",
"parameters":{"type":"object","properties":{"a":{"type":"number"},
"b":{"type":"number"}},"required":["a","b"]}}}
</tools>
Return function calls as: <tool_call>{"name":"...","arguments":{...}}</tool_call><|im_end|>
<|im_start|>user
计算 123 + 456<|im_end|>
<|im_start|>assistant
<tool_call>
{"name": "add", "arguments": {"a": 123, "b": 456}}
</tool_call><|im_end|>
<|im_start|>tool
<tool_response>
{"name": "add", "content": 579}
</tool_response>
<|im_end|>
<|im_start|>assistant
123 + 456 = 579<|im_end|>
```

> 🎯 **关键模式**：用户提问 → 模型输出 `<tool_call>` → `tool` 角色反馈结果 → 模型给出最终自然语言答案

## 4. 代码实现：apply_chat_template

### 4.1 HuggingFace Transformers 标准写法

```python
from transformers import AutoTokenizer

tokenizer = AutoTokenizer.from_pretrained(
    "NousResearch/Hermes-3-Llama-3.1-8B",
    trust_remote_code=True
)

messages = [
    {"role": "system", "content": "You are Hermes, a helpful assistant."},
    {"role": "user", "content": "Hello, who are you?"},
]

# ⚠️ 务必设置 add_generation_prompt=True
# 这会在末尾追加 <|im_start|>assistant\n，让模型开始生成
gen_input = tokenizer.apply_chat_template(
    messages,
    return_tensors="pt",
    add_generation_prompt=True    # ← 关键！
)
```

### 4.2 完整推理流水线

```python
import torch
from transformers import AutoTokenizer, LlamaForCausalLM

tokenizer = AutoTokenizer.from_pretrained(
    "NousResearch/Hermes-3-Llama-3.1-8B",
    trust_remote_code=True
)
model = LlamaForCausalLM.from_pretrained(
    "NousResearch/Hermes-3-Llama-3.1-8B",
    torch_dtype=torch.float16,
    device_map="auto",
    load_in_4bit=True,
    use_flash_attention_2=True
)

messages = [
    {"role": "system", "content": "You are Hermes, a helpful AI."},
    {"role": "user", "content": "用三句话解释什么是 Transformer"}
]

inputs = tokenizer.apply_chat_template(
    messages, return_tensors="pt", add_generation_prompt=True
).to("cuda")

outputs = model.generate(
    inputs,
    max_new_tokens=750,
    temperature=0.8,
    repetition_penalty=1.1,
    do_sample=True,
    eos_token_id=tokenizer.eos_token_id
)

response = tokenizer.decode(
    outputs[0][inputs.shape[-1]:], skip_special_tokens=True
)
print(response)
```

### 4.3 常见错误

| 错误写法 | 问题 | 正确写法 |
|---------|------|---------|
| 忘记 `add_generation_prompt=True` | 模型不知道要生成什么 | 必须加 |
| 手写 `<\|im_start\|>` 字符串 | tokenizer 不会正确 tokenize | 用 `apply_chat_template` |
| System prompt 写在不相关的角色里 | Hermes 无法区分指令和对话 | 严格使用 `role: "system"` |
| `<\|im_end\|>` 后没换行 | token 粘连 | 每个消息块后用 `\n` |

## 5. ChatML vs Llama-Chat 格式演进

| 维度 | ChatML (Hermes 1-3) | Llama-Chat (DeepHermes 3) |
|------|---------------------|--------------------------|
| 消息开始 | `<\|im_start\|>role` | `<\|start_header_id\|>role<\|end_header_id\|>` |
| 消息结束 | `<\|im_end\|>` | `<\|eot_id\|>` |
| 角色标签 | 纯文本 | 封装在 header 中 |
| 模型系列 | Hermes 1/2/3, Qwen | DeepHermes 3, Llama 3.1+ |
| 生成提示 | `add_generation_prompt` 追加 `<\|im_start\|>assistant\n` | 追加 `<\|start_header_id\|>assistant<\|end_header_id\|>\n\n` |

```text
# ChatML 示例
<|im_start|>system
You are Hermes.<|im_end|>
<|im_start|>user
Hello<|im_end|>
<|im_start|>assistant
Hi!<|im_end|>

# Llama-Chat 示例（DeepHermes 3）
<|start_header_id|>system<|end_header_id|>
You are Hermes.<|eot_id|>
<|start_header_id|>user<|end_header_id|>
Hello<|eot_id|>
<|start_header_id|>assistant<|end_header_id|>
Hi!<|eot_id|>
```

> ⚠️ **迁移注意**：DeepHermes 3 使用 Llama-Chat 格式，与 Hermes 3 的 ChatML **不兼容**。升级时务必更新 tokenizer 调用和 prompt 模板

## 核心要点回顾

- ChatML = `<\|im_start\|>` + 角色 + 内容 + `<\|im_end\|>` 的对话标记语言
- 四个角色：`system`（指令）、`user`（提问）、`assistant`（回复）、`tool`（工具反馈）
- 永远用 `apply_chat_template()` + `add_generation_prompt=True`，不要手写
- System prompt 越具体，Hermes 的"aggressively follows"特性越能发挥
- DeepHermes 3 迁移到 Llama-Chat 格式，注意兼容性

## 参考资料

1. HuggingFace - NousResearch/Hermes-3-Llama-3.1-8B Model Card
2. Nous Research - Hermes-Function-Calling GitHub 仓库
3. Meta - Llama 3.1 Chat Format 规范
