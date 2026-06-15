# 🔧 模型微调与 LoRA 实战

> 当 Prompt Engineering 和 RAG 都无法满足需求时——比如你需要模型学会一种特定的写作风格、掌握一套专业术语，或者遵循一种独特的输出格式——模型微调就是你的下一步。其中，**LoRA（Low-Rank Adaptation，低秩适应）** 是当前最高效的微调技术：它只训练极少量参数（通常不到总参数的 1%），就能让模型高效地适应新领域。

## 前置阅读

- [[大模型参数高效微调（PEFT）核心知识点]]
- [[LoRA与QLoRA：大模型参数高效微调实战]]
- [[快速学会「预训练模型」]]

## 目录

1. [什么时候需要微调](#1-什么时候需要微调)
2. [微调方法全景](#2-微调方法全景)
3. [LoRA 原理深度解析](#3-lora-原理深度解析)
4. [QLoRA：消费级 GPU 也能微调](#4-qlora消费级-gpu-也能微调)
5. [实战：微调 Qwen 7B](#5-实战微调-qwen-7b)
6. [微调数据准备指南](#6-微调数据准备指南)

---

## 1. 什么时候需要微调

### 1.1 决策矩阵

| 问题 | 优先尝试 | 如果不行 |
|------|---------|---------|
| 模型输出格式不对 | Prompt 约束 | 微调 |
| 模型不了解公司信息 | RAG | RAG+微调 |
| 模型语气不够专业 | Few-Shot Prompt | 微调 |
| 模型回答不够"像我们" | 优化 System Prompt | 微调 |
| 模型不理解行业黑话 | 术语表 + RAG | 微调 |
| 需要特定推理模式 | CoT Prompt | 微调 |

> **重点**：微调是"最后一招"，不是"第一选择"。Prompt + RAG 的成本是零，微调的成本是 GPU 时间 + 数据准备。先用前两者做到极致，再考虑微调。

### 1.2 微调的典型应用场景

| 场景 | 微调目标 | 数据需求 |
|------|---------|---------|
| **品牌风格对齐** | 让模型输出符合品牌调性 | 500-2000 条高质量对话 |
| **专业领域适配** | 掌握法律/医疗/金融等垂直领域知识 | 1000-5000 条领域数据 |
| **指令遵循增强** | 提升模型对特定指令格式的遵从度 | 500-3000 条指令-输出对 |
| **多语言优化** | 增强特定语言的表达能力 | 1000-10000 条多语言对照 |
| **任务特化** | 在分类/抽取/摘要等任务上达到 SOTA | 2000-20000 条任务数据 |

---

## 2. 微调方法全景

### 2.1 三类微调方法对比

```
全量微调（Full Fine-tuning）
  ├─ 可训练参数：100%（全部参数）
  ├─ 显存需求：16× 模型大小（含优化器状态）
  ├─ 训练速度：慢
  └─ 适用：资源充足、追求极致效果

参数高效微调（PEFT）
  ├─ LoRA：可训练参数 0.1%-1%
  │   ├─ 显存需求：~1.2× 模型大小
  │   ├─ 效果：接近全量微调
  │   └─ 推荐指数：⭐⭐⭐⭐⭐
  ├─ Adapter：在层间插入小型网络
  └─ Prefix Tuning：训练虚拟 token 前缀

QLoRA（量化 + LoRA）
  ├─ 可训练参数：0.1%-1%
  ├─ 显存需求：~0.3× 模型大小（模型以 INT4 加载）
  ├─ 效果：略低于 LoRA，但成本极低
  └─ 推荐指数：⭐⭐⭐⭐⭐（性价比之王）
```

### 2.2 硬件需求速查

| 模型规模 | 全量微调 | LoRA | QLoRA |
|----------|---------|------|-------|
| 1B | RTX 3060 12GB | RTX 3060 12GB | GTX 1060 6GB |
| 7B | 4×A100 80GB | RTX 3090 24GB | RTX 3060 12GB |
| 13B | 8×A100 80GB | A100 40GB | RTX 3090 24GB |
| 70B | 32×A100 80GB | 4×A100 80GB | A100 80GB |

---

## 3. LoRA 原理深度解析

### 3.1 核心思想

LoRA 基于一个关键观察：**模型在适应新任务时，权重的变化矩阵是"低秩"的**。换句话说，虽然权重矩阵很大（如 4096×4096），但它真正需要的"变化"可以用两个小矩阵的乘积来表示。

```
原始前向传播：
  h = W₀·x

LoRA 前向传播：
  h = W₀·x + (B·A)·x

其中：
  W₀：冻结的原始权重（不训练）
  A：d×r 矩阵（低秩分解的上行矩阵）
  B：r×d 矩阵（低秩分解的下行矩阵）
  r：秩（rank），通常设为 8-64

参数量对比：
  原始：d×d = 4096×4096 = 16,777,216 参数
  LoRA：d×r + r×d = 4096×8 + 8×4096 = 65,536 参数
  压缩比：256×（只训练了 0.39% 的参数）
```

### 3.2 可视化理解

```
    原始权重矩阵 W₀ (4096×4096)
    ┌─────────────────────┐
    │                     │  ← 冻结，不训练
    │     16M 参数        │
    │                     │
    └─────────────────────┘
              +
    B (4096×8) × A (8×4096)
    ┌────┐   ┌──────────┐
    │    │ × │          │  ← 只训练这两个小矩阵
    │    │   │  65K 参数 │     共 65K 参数
    └────┘   └──────────┘
```

### 3.3 LoRA 的关键参数

| 参数 | 含义 | 推荐值 | 调优建议 |
|------|------|--------|---------|
| **r（秩）** | 低秩分解的秩 | 8-16 | r 越大，表达能力越强，但参数量线性增长 |
| **α（缩放因子）** | 控制 LoRA 更新的幅度 | 16-32 | 通常设为 r 的 2 倍 |
| **target_modules** | 哪些层应用 LoRA | q_proj, v_proj | 至少覆盖 attention 的 Q 和 V 投影 |
| **dropout** | 正则化强度 | 0.05-0.1 | 数据量小时增大，数据量大时减小 |

---

## 4. QLoRA：消费级 GPU 也能微调

### 4.1 三重核心技术

QLoRA = **NF4 量化** + **双重量化** + **分页优化器**，让 7B 模型在 12GB 显存上微调成为可能。

| 技术 | 作用 |
|------|------|
| **NF4（NormalFloat4）** | 专门为正态分布权重设计的 4-bit 量化格式，比普通 INT4 损失更小 |
| **双重量化（Double Quantization）** | 对量化常数本身再做一次量化，额外节省 0.4 bit/参数 |
| **分页优化器（Paged Optimizer）** | 将优化器状态分页到 CPU 内存，避免 OOM |

### 4.2 QLoRA 配置模板

```python
from transformers import BitsAndBytesConfig
from peft import LoraConfig, prepare_model_for_kbit_training

# 量化配置
bnb_config = BitsAndBytesConfig(
    load_in_4bit=True,
    bnb_4bit_compute_dtype=torch.bfloat16,
    bnb_4bit_use_double_quant=True,
    bnb_4bit_quant_type="nf4"
)

# LoRA 配置
lora_config = LoraConfig(
    r=16,                    # 低秩
    lora_alpha=32,           # 缩放因子
    target_modules=[         # 目标模块
        "q_proj", "k_proj", "v_proj", "o_proj",
        "gate_proj", "up_proj", "down_proj"
    ],
    lora_dropout=0.05,
    bias="none",
    task_type="CAUSAL_LM"
)

model = prepare_model_for_kbit_training(model)
model = get_peft_model(model, lora_config)

# 可训练参数占比
model.print_trainable_parameters()
# 输出：trainable params: 41,943,040 || all params: 7,615,573,632
#       trainable%: 0.55%
```

---

## 5. 实战：微调 Qwen 7B

### 5.1 完整训练脚本

```python
import torch
from transformers import (
    AutoModelForCausalLM,
    AutoTokenizer,
    TrainingArguments,
    Trainer,
    BitsAndBytesConfig
)
from peft import LoraConfig, get_peft_model, prepare_model_for_kbit_training
from datasets import load_dataset

# ========== 1. 加载量化模型 ==========
model_name = "Qwen/Qwen2.5-7B-Instruct"

model = AutoModelForCausalLM.from_pretrained(
    model_name,
    quantization_config=BitsAndBytesConfig(
        load_in_4bit=True,
        bnb_4bit_compute_dtype=torch.bfloat16,
        bnb_4bit_use_double_quant=True,
        bnb_4bit_quant_type="nf4"
    ),
    device_map="auto",
    trust_remote_code=True
)

tokenizer = AutoTokenizer.from_pretrained(model_name, trust_remote_code=True)
tokenizer.pad_token = tokenizer.eos_token

# ========== 2. 准备 LoRA ==========
model = prepare_model_for_kbit_training(model)

lora_config = LoraConfig(
    r=16,
    lora_alpha=32,
    target_modules=["q_proj", "k_proj", "v_proj", "o_proj"],
    lora_dropout=0.05,
    bias="none",
    task_type="CAUSAL_LM"
)

model = get_peft_model(model, lora_config)
model.print_trainable_parameters()

# ========== 3. 加载数据 ==========
# 数据格式：{"instruction": "...", "input": "...", "output": "..."}
dataset = load_dataset("json", data_files="training_data.jsonl")

def format_prompt(example):
    """格式化为 ChatML 格式"""
    prompt = f"<|im_start|>system\n你是一个Java技术助手。<|im_end|>\n"
    prompt += f"<|im_start|>user\n{example['instruction']}\n{example['input']}<|im_end|>\n"
    prompt += f"<|im_start|>assistant\n{example['output']}<|im_end|>"
    return {"text": prompt}

dataset = dataset.map(format_prompt)

def tokenize(example):
    return tokenizer(
        example["text"],
        truncation=True,
        max_length=2048,
        padding="max_length"
    )

dataset = dataset.map(tokenize, batched=True)

# ========== 4. 训练 ==========
training_args = TrainingArguments(
    output_dir="./qwen-lora-java",
    num_train_epochs=3,
    per_device_train_batch_size=4,
    gradient_accumulation_steps=4,  # 有效 batch_size = 4×4 = 16
    learning_rate=2e-4,
    warmup_ratio=0.03,
    logging_steps=10,
    save_steps=200,
    fp16=True,
    optim="paged_adamw_8bit",      # QLoRA 推荐优化器
    report_to="none"
)

trainer = Trainer(
    model=model,
    args=training_args,
    train_dataset=dataset["train"],
    tokenizer=tokenizer
)

trainer.train()

# ========== 5. 保存 ==========
model.save_pretrained("./qwen-lora-java-final")
tokenizer.save_pretrained("./qwen-lora-java-final")
print("✅ 微调完成！")
```

### 5.2 微调后推理

```python
from peft import PeftModel

# 加载基础模型
base_model = AutoModelForCausalLM.from_pretrained(model_name, device_map="auto")

# 加载 LoRA 权重
model = PeftModel.from_pretrained(base_model, "./qwen-lora-java-final")

# 推理
inputs = tokenizer("用Java实现一个线程安全的LRU缓存", return_tensors="pt")
outputs = model.generate(**inputs, max_new_tokens=512)
print(tokenizer.decode(outputs[0], skip_special_tokens=True))
```

---

## 6. 微调数据准备指南

### 6.1 数据质量 > 数据数量

> **重点**：500 条高质量数据的效果远超 5000 条低质量数据。宁缺毋滥。

| 质量维度 | 检查要点 |
|----------|---------|
| **格式一致** | 所有样本遵循相同的格式模板 |
| **指令清晰** | 每条指令描述清楚，避免歧义 |
| **输出正确** | 目标输出经过人工验证 |
| **多样性** | 覆盖简单/中等/困难案例 |
| **无偏差** | 避免性别、种族、政治等敏感偏差 |

### 6.2 数据格式模板

```jsonl
{"instruction": "解释什么是线程安全", "input": "", "output": "线程安全是指..."}
{"instruction": "修复以下代码的并发问题", "input": "public class Counter { private int count = 0; ... }", "output": "修复方案：使用AtomicInteger..."}
{"instruction": "实现一个生产者消费者模式", "input": "使用BlockingQueue", "output": "```java\npublic class ProducerConsumer { ... }\n```"}
```

---

## 核心要点回顾

- **微调是最后一招**：先用 Prompt + RAG 做到极致，再考虑微调
- **LoRA 只训练 <1% 的参数**，效果却接近全量微调，是当前最主流的方案
- **QLoRA = INT4量化 + LoRA**，让 7B 模型在 12GB 消费级显卡上微调成为现实
- **数据质量 > 数据数量**：500 条精品 > 5000 条垃圾
- LoRA 关键参数：r=8-16, α=r×2, target_modules 至少包含 q_proj 和 v_proj
- 微调后 LoRA 权重是独立文件，不修改原始模型，可以随时切换

## 参考资料

1. [[大模型参数高效微调（PEFT）核心知识点]]
2. [[LoRA与QLoRA：大模型参数高效微调实战]]
3. [[LoRA与QLoRA参数高效微调实战]]
4. [[模型评估指标与实践]]
5. [[快速学会「预训练模型」]]
