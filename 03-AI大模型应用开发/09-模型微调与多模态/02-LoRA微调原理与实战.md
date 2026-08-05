# 02 - LoRA 微调原理与实战

> 🎯 LoRA = Low-Rank Adaptation — 只训练 0.1% 参数达到全量微调 95%+ 效果，是个人开发者与大模型之间的桥梁

## 核心原理

```text
LoRA 核心假设：微调时的权重更新 ΔW 是"低秩"的

  原始前向：h = W₀x （W₀ 冻结）
  LoRA 前向：h = W₀x + BAx

  W₀: (4096 × 4096) = 16.8M 参数 → 冻结
  B:  (4096 × r)    = 32K (r=8)
  A:  (r × 4096)    = 32K
  → 只训练 BA 的 64K 参数（原来的 0.4%！）
```

## 参数配置

```python
from peft import LoraConfig, get_peft_model

lora_config = LoraConfig(
    r=8,                          # 低秩维度（核心参数）
    lora_alpha=16,                # 缩放系数（通常 α=2r）
    target_modules=[              # 应用 LoRA 的模块
        "q_proj", "k_proj",      # 注意力 Q/K/V
        "v_proj", "o_proj",      # 输出投影
        "gate_proj", "up_proj",  # LLaMA FFN 门控
        "down_proj"               # LLaMA FFN 下投影
    ],
    lora_dropout=0.05,
    task_type="CAUSAL_LM"
)

model = get_peft_model(base_model, lora_config)
model.print_trainable_parameters()
# trainable: 8.4M || all: 7,078M || 0.12%
```

### r 的选择

| r | Adapter 大小 | 效果 | 适用 |
|:---:|------|:---:|------|
| 4 | ~2MB(7B) | ⭐⭐⭐ | 快速实验 |
| **8** | ~4MB | ⭐⭐⭐⭐ | **通用推荐** |
| 16 | ~8MB | ⭐⭐⭐⭐ | 追求效果 |
| 32 | ~16MB | ⭐⭐⭐⭐⭐ | 复杂任务 |
| 64+ | ~32MB | 接近全量 | 接近全量微调 |

## 完整训练代码

```python
from transformers import AutoModelForCausalLM, AutoTokenizer, Trainer, TrainingArguments
from peft import LoraConfig, get_peft_model
from datasets import load_dataset
import torch

# ① 加载基座模型
model = AutoModelForCausalLM.from_pretrained(
    "Qwen/Qwen2-7B",
    torch_dtype=torch.bfloat16,
    device_map="auto"
)
tokenizer = AutoTokenizer.from_pretrained("Qwen/Qwen2-7B")
tokenizer.pad_token = tokenizer.eos_token

# ② 配置 LoRA
model = get_peft_model(model, LoraConfig(
    r=8, lora_alpha=16, target_modules=["q_proj","v_proj"],
    lora_dropout=0.05, task_type="CAUSAL_LM"
))

# ③ 加载数据
dataset = load_dataset("json", data_files="train.jsonl")
def tokenize(examples):
    return tokenizer(examples["text"], truncation=True, max_length=512)
dataset = dataset.map(tokenize, batched=True)

# ④ 训练
trainer = Trainer(
    model=model,
    args=TrainingArguments(
        output_dir="./lora-output",
        num_train_epochs=3,
        per_device_train_batch_size=4,
        gradient_accumulation_steps=4,  # 模拟 batch=16
        learning_rate=2e-4,
        warmup_steps=100,
        logging_steps=10,
        save_strategy="epoch",
        bf16=True,                      # BF16 混合精度
    ),
    train_dataset=dataset["train"],
)
trainer.train()

# ⑤ 保存
model.save_pretrained("./qwen2-lora-adapter")
# 或合并到基座
merged = model.merge_and_unload()
merged.save_pretrained("./qwen2-merged")
```

## 关键超参

| 参数 | 推荐值 | 说明 |
|------|:---:|------|
| learning_rate | 2e-4 | LoRA 可以比全量大 10× |
| epochs | 1-3 | 小数据 3，大数据 1 |
| batch_size | 4-16 | 看显存，配合梯度累积 |
| warmup_steps | 100-500 | LoRA 可以短一些 |
| lr_scheduler | cosine | LLM 标配 |
