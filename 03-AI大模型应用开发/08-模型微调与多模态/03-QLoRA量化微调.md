# 03 - QLoRA 量化微调

> 🎯 QLoRA = 4-bit 量化基座 + LoRA 微调 — 单张 RTX 4090 (24GB) 就能微调 LLaMA-70B！

## 核心技术栈

```text
QLoRA 的三个技术创新：

① NF4 (4-bit NormalFloat)
   专为正态分布权重设计的 4-bit 格式
   → 比标准 INT4 更适合 LLM 权重

② 双重量化 (Double Quantization)
   量化常数量化（省 ~0.4 bit/参数）
   → 65B 模型从 32GB 压到 15GB

③ 分页优化器 (Paged Optimizer)
   梯度峰值溢到 CPU 内存
   → 避免 OOM
```

## 完整代码

```python
import torch
from transformers import AutoModelForCausalLM, BitsAndBytesConfig
from peft import LoraConfig, get_peft_model, prepare_model_for_kbit_training

# ① 4-bit 量化配置
bnb_config = BitsAndBytesConfig(
    load_in_4bit=True,
    bnb_4bit_quant_type="nf4",            # NF4 量化
    bnb_4bit_compute_dtype=torch.bfloat16, # 计算用 BF16
    bnb_4bit_use_double_quant=True,        # 双重量化
)

# ② 加载量化模型
model = AutoModelForCausalLM.from_pretrained(
    "Qwen/Qwen2-72B",                     # 72B 模型！
    quantization_config=bnb_config,
    device_map="auto",
    torch_dtype=torch.bfloat16,
)
model = prepare_model_for_kbit_training(model)  # 准备 LoRA 训练

# ③ LoRA 配置（同上）
model = get_peft_model(model, LoraConfig(
    r=8, lora_alpha=16,
    target_modules=["q_proj","v_proj"],
    lora_dropout=0.05,
    task_type="CAUSAL_LM"
))

# ④ 训练（代码与 LoRA 完全相同！）
trainer = Trainer(
    model=model,
    args=TrainingArguments(
        output_dir="./qlora-output",
        per_device_train_batch_size=1,    # 72B 只能 batch=1
        gradient_accumulation_steps=16,   # 模拟 batch=16
        learning_rate=2e-4,
        fp16=True,  # QLoRA 用 FP16（不支持 BF16）
        optim="paged_adamw_8bit",         # 分页优化器
    ),
    train_dataset=dataset,
)
trainer.train()
```

## 显存需求对比

| 模型 | LoRA (FP16) | QLoRA (NF4) | 节省 |
|------|:---:|:---:|:---:|
| Qwen2-1.8B | 8GB | 3GB | 63% |
| Qwen2-7B | 16GB | 6GB | 63% |
| Qwen2-14B | 28GB | 10GB | 64% |
| Qwen2-72B | 140GB | 24GB | 83% |

## QLoRA vs LoRA 选型

```text
选 QLoRA：
  ✅ 显存 < 16GB
  ✅ 微调 13B+ 模型
  ✅ 快速实验（训练速度慢 20-30% 但能跑更大的模型）

选 LoRA (FP16)：
  ✅ 显存充足(>24GB)
  ✅ 追求训练速度
  ✅ 7B 以下小模型
```
