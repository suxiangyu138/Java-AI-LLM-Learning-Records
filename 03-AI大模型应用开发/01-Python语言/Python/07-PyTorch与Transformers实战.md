# 07 - PyTorch 与 Transformers 实战

> 🎯 Java 后端不需要成为 PyTorch 专家，但必须会：加载模型、跑推理、微调 LoRA。这三件事就够了

## 1. HuggingFace Pipeline（零代码）

```python
from transformers import pipeline

# 一行跑任何任务
generator = pipeline("text-generation", model="Qwen/Qwen2-1.5B")
print(generator("解释 Java 多态：", max_length=100))

classifier = pipeline("sentiment-analysis", model="uer/roberta-base-finetuned-jd-binary-chinese")
print(classifier("这个产品质量很好"))

ner = pipeline("ner", model="dslim/bert-base-NER")
print(ner("Elon Musk founded SpaceX in California"))
```

## 2. 加载模型推理

```python
from transformers import AutoModelForCausalLM, AutoTokenizer

model = AutoModelForCausalLM.from_pretrained(
    "Qwen/Qwen2-7B-Instruct",
    device_map="auto",          # 自动放到 GPU
    torch_dtype="auto"          # 自动选精度
)
tokenizer = AutoTokenizer.from_pretrained("Qwen/Qwen2-7B-Instruct")

inputs = tokenizer("解释 Java 多态", return_tensors="pt").to(model.device)
outputs = model.generate(**inputs, max_new_tokens=256)
print(tokenizer.decode(outputs[0], skip_special_tokens=True))
```

## 3. LoRA 微调

```python
from peft import LoraConfig, get_peft_model

# 配置 LoRA
lora_config = LoraConfig(
    r=8, lora_alpha=16,
    target_modules=["q_proj", "v_proj"],  # 只训练注意力层
    lora_dropout=0.05,
    task_type="CAUSAL_LM"
)

model = get_peft_model(model, lora_config)
model.print_trainable_parameters()
# trainable params: 8,388,608 || all: 7,078,658,048 || 0.12%

# 训练（标准 PyTorch 训练循环）
from transformers import Trainer, TrainingArguments

trainer = Trainer(
    model=model,
    args=TrainingArguments(
        output_dir="./output",
        num_train_epochs=3,
        per_device_train_batch_size=4,
        learning_rate=2e-4,
    ),
    train_dataset=dataset,
)
trainer.train()
model.save_pretrained("./qwen2-lora-finetuned")
```

## 4. 常用操作速查

```python
import torch

# 张量操作
x = torch.randn(1, 768)        # 创建张量
x_gpu = x.to("cuda")            # 移到 GPU

# 推理加速
with torch.no_grad():           # 不计算梯度（省显存+加速）
    output = model(input_ids)

# 混合精度
with torch.cuda.amp.autocast(dtype=torch.bfloat16):
    output = model(input_ids)
```
