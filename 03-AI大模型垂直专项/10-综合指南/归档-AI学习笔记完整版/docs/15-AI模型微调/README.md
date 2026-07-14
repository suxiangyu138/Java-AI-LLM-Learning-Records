# 第15步：AI模型微调（Fine-tuning）

> **阶段目标：** 理解模型微调的核心概念，掌握PEFT/LoRA等参数高效微调技术的原理与实践  
> **预计学时：** 2-3周（每天3-4小时）  
> **前置要求：** Transformer架构 + HuggingFace使用 + 一定的GPU使用经验  

---

## 📚 目录

- [15.1 微调基础概念](#151-微调基础概念)
- [15.2 PEFT：参数高效微调](#152-peft参数高效微调)
- [15.3 LoRA：低秩适配](#153-lora低秩适配)
- [15.4 QLoRA：量化+LoRA](#154-qlora量化lora)
- [15.5 微调实战全流程](#155-微调实战全流程)
- [15.6 评估与迭代](#156-评估与迭代)
- [15.7 阶段练习](#157-阶段练习)

---

## 15.1 微调基础概念

### 15.1.1 什么时候需要微调？

```
✅ 应该微调的场景：
├── 领域术语：医疗、法律、金融等专业领域
├── 特定格式：要求固定的输出格式（如特定的JSON schema）
├── 风格模仿：让模型学会特定的写作风格
├── 小型化：让小模型学会大模型的能力（蒸馏）
└── 数据安全：不想把数据发给第三方API

❌ 不需要微调的场景：
├── 通用问答 ← Prompt Engineering就够了
├── 知识更新 ← RAG更合适
├── 事实性修改 ← 微调不是用来"纠正"错误的
└── 简单格式控制 ← Few-shot + JSON Mode就够了

⚠️ 微调改变的是"风格和格式"，不是"知识和事实"
```

### 15.1.2 微调方式对比

```
全量微调 (Full Fine-tuning):
  ┌────────────────────────────┐
  │  原模型 (7B参数, 14GB)     │  全部参数参与训练
  │  ████████████████████████  │  需要：7B × 4 bytes × 4 (optimizer states)
  │  ████████████████████████  │  ≈ 112GB+ GPU显存！
  │  ████████████████████████  │  不现实 ❌
  └────────────────────────────┘

PEFT (参数高效微调):
  ┌────────────────────────────┐
  │  原模型 (7B参数)  冻结！❄️ │  只训练极少参数
  │  ░░░░░░░░░░░░░░░░░░░░░░░░ │  
  │  ░░░░░░██░░░██░░░██░░░░░░ │ ← 只训练这几小块
  │  ░░░░░░░░░░░░░░░░░░░░░░░░ │  
  └────────────────────────────┘
  训练参数：0.1%-1% 的原模型
  显存需求：原来的 1/3 - 1/10
```

### 15.1.3 PEFT方法对比

```
┌─────────────┬──────────┬──────────┬──────────┬──────────┐
│   方法       │ 训练参数  │ 显存需求  │ 推理开销  │  效果    │
├─────────────┼──────────┼──────────┼──────────┼──────────┤
│ Full FT      │ 100%     │ ⭐⭐⭐⭐⭐ │ 无       │ ★★★★★   │
│ LoRA         │ 0.1-1%   │ ⭐⭐     │ 可合并   │ ★★★★    │
│ QLoRA        │ 0.1-1%   │ ⭐       │ 可合并   │ ★★★★    │
│ Adapter      │ 1-3%     │ ⭐⭐⭐   │ 有       │ ★★★     │
│ Prefix Tuning│ 0.01%    │ ⭐       │ 有       │ ★★★     │
│ Prompt Tuning│ 0.001%   │ ⭐       │ 有       │ ★★      │
└─────────────┴──────────┴──────────┴──────────┴──────────┘

推荐学习顺序：LoRA → QLoRA
推荐默认选择：QLoRA（性价比最高）
```

---

## 15.2 PEFT：参数高效微调

### 15.2.1 PEFT库使用

```python
from peft import (
    LoraConfig,
    get_peft_model,
    TaskType,
    PeftModel,
    prepare_model_for_kbit_training,
)
from transformers import (
    AutoModelForCausalLM,
    AutoTokenizer,
    TrainingArguments,
    Trainer,
    DataCollatorForLanguageModeling,
)
import torch

# ========== 1. 加载基础模型 ==========
model_name = "Qwen/Qwen2-0.5B"  # 小模型便于学习

model = AutoModelForCausalLM.from_pretrained(
    model_name,
    torch_dtype=torch.float16,   # 半精度节省显存
    device_map="auto",
)

tokenizer = AutoTokenizer.from_pretrained(model_name)

# 设置pad_token
if tokenizer.pad_token is None:
    tokenizer.pad_token = tokenizer.eos_token

# ========== 2. 配置LoRA ==========
lora_config = LoraConfig(
    r=8,                         # LoRA秩（最重要的超参数）
    lora_alpha=16,               # LoRA缩放因子
    target_modules=[             # 目标模块（需要根据模型架构选择）
        "q_proj", "k_proj", "v_proj", "o_proj",     # Attention层
        "gate_proj", "up_proj", "down_proj",         # FFN层
    ],
    lora_dropout=0.1,            # LoRA层的Dropout
    bias="none",                 # 不训练bias
    task_type=TaskType.CAUSAL_LM,  # 因果语言模型
)

# ========== 3. 应用LoRA ==========
model = get_peft_model(model, lora_config)

# 查看可训练参数
model.print_trainable_parameters()
# 输出: trainable params: 2.3M || all params: 500M || trainable%: 0.46%
```

### 15.2.2 LoRA参数详解

```python
"""
LoRA核心思想：

传统的全连接层：
  h = W @ x          (W ∈ R^{d×k})

LoRA的修改：
  h = W @ x + (B @ A) @ x / α * r
  
  其中: A ∈ R^{r×k}, B ∈ R^{d×r}
  r 是秩（rank），通常取 4-64

关键超参数：

1. r (秩):
   - r=4: 极低参数，效果一般
   - r=8: 推荐起始值，大多数场景够用
   - r=16: 复杂任务推荐
   - r=64: 接近全量微调效果
   原则：简单任务用低r，复杂任务用高r

2. lora_alpha (缩放因子):
   - 通常设为 r × 2
   - r=8, alpha=16 或 r=16, alpha=32
   - 越大 → LoRA影响越大 → 学习率应相应调小

3. target_modules (目标模块):
   不同架构的模块名不同！
   
   LLaMA/Qwen系:
   ["q_proj", "k_proj", "v_proj", "o_proj", 
    "gate_proj", "up_proj", "down_proj"]
   
   BERT系:
   ["query", "key", "value", "output"]
   
   GPT-2系:
   ["c_attn", "c_proj", "c_fc"]
   
   建议：对所有线性层应用LoRA → modules_to_save=None

4. lora_dropout:
   - 0.0: 无dropout（数据少时不推荐）
   - 0.05-0.1: 推荐范围
   - 0.2: 防过拟合，但可能降低效果
"""

# 推荐配置（不同场景）
LORA_CONFIGS = {
    "quick_test": {
        "r": 4, "lora_alpha": 8, "lora_dropout": 0.0,
    },
    "standard": {
        "r": 8, "lora_alpha": 16, "lora_dropout": 0.05,
    },
    "complex_task": {
        "r": 16, "lora_alpha": 32, "lora_dropout": 0.1,
    },
    "max_quality": {
        "r": 64, "lora_alpha": 128, "lora_dropout": 0.1,
    },
}
```

---

## 15.3 LoRA：低秩适配

### 15.3.1 深入理解LoRA

```python
"""
LoRA的数学直觉：

原模型权重 W ∈ R^{d×k} (冻结)
LoRA分解: ΔW = B @ A
  其中: A ∈ R^{r×k} (下投影), B ∈ R^{d×r} (上投影)
  r << min(d, k)  → 低秩

前向传播:
  h = W @ x + B @ (A @ x)
      ↑          ↑
   冻结部分    LoRA部分(训练)

为什么有效？
- 大模型的参数更新实际上存在于一个低秩子空间中
- 不需要改变所有权重，只需要在这个低秩子空间中调整
- 类似于：不需要重新训练整个大脑，只需要激活/调整几个神经通路

直观类比：
- 原模型 = 一个已建好的城市
- LoRA = 在城市中修建几条新的"高速路"
- 城市本身不变（原模型权重冻结）
- 但交通流量（输出）发生了显著变化
"""
```

### 15.3.2 LoRA实战训练

```python
from datasets import Dataset
from transformers import TrainingArguments, Trainer, DataCollatorForLanguageModeling
import torch

# ========== 准备训练数据 ==========
# 指令微调数据格式
training_data = [
    {
        "instruction": "将以下文本翻译成英文",
        "input": "你好，今天天气怎么样？",
        "output": "Hello, how's the weather today?"
    },
    # ... 更多数据
]

def format_instruction(example):
    """将数据格式化为模型输入"""
    prompt = f"""### 指令:
{example['instruction']}

### 输入:
{example['input']}

### 回答:
{example['output']}"""
    
    return {"text": prompt}

# 创建数据集
dataset = Dataset.from_list(training_data)
dataset = dataset.map(format_instruction)

# Tokenize
def tokenize_function(examples):
    result = tokenizer(
        examples['text'],
        truncation=True,
        padding='max_length',
        max_length=512,
    )
    # 对于因果语言模型，labels = input_ids
    result['labels'] = result['input_ids'].copy()
    return result

tokenized_dataset = dataset.map(tokenize_function, batched=True)

# ========== 训练配置 ==========
training_args = TrainingArguments(
    output_dir="./lora-finetuned-model",
    
    # 训练超参数
    num_train_epochs=3,           # 微调通常1-5个epoch就够了
    per_device_train_batch_size=4,  # 根据GPU显存调整
    gradient_accumulation_steps=4,  # 梯度累积（模拟更大的batch）
    
    # 优化器
    learning_rate=2e-4,           # LoRA的学习率通常比全量微调高
    lr_scheduler_type="cosine",   # 余弦退火
    warmup_ratio=0.03,            # warmup比例
    optim="adamw_8bit",          # 8bit优化器节省显存
    
    # 评估与保存
    evaluation_strategy="steps",
    eval_steps=100,
    save_strategy="steps",
    save_steps=200,
    save_total_limit=3,           # 只保留最近3个checkpoint
    
    # 日志
    logging_steps=10,
    report_to="tensorboard",
    
    # 其他
    bf16=True,                    # BF16训练（需要Ampere+ GPU）
    # fp16=True,                  # 如果没有BF16支持
    gradient_checkpointing=True,  # 节省显存（以速度为代价）
    dataloader_num_workers=4,
    remove_unused_columns=False,
)

# ========== 数据整理器 ==========
data_collator = DataCollatorForLanguageModeling(
    tokenizer=tokenizer,
    mlm=False,  # 不是MLM，是因果语言模型
)

# ========== 训练 ==========
trainer = Trainer(
    model=model,
    args=training_args,
    train_dataset=tokenized_dataset,
    data_collator=data_collator,
)

trainer.train()

# ========== 保存LoRA权重 ==========
model.save_pretrained("./lora-weights")
tokenizer.save_pretrained("./lora-weights")

print("✓ LoRA权重已保存！")
print(f"  权重文件大小: ~{2.3 * 8 * r / 1024:.1f} MB")  # 远小于原模型的几个GB
```

---

## 15.4 QLoRA：量化+LoRA

### 15.4.1 QLoRA原理

```python
"""
QLoRA = Quantization + LoRA

量化 (Quantization):
  FP16: 每个参数16位 → 7B模型 ≈ 14GB
  INT8: 每个参数8位  → 7B模型 ≈ 7GB
  INT4: 每个参数4位  → 7B模型 ≈ 3.5GB ← QLoRA使用这个

QLoRA的三大创新：
1. NF4 (4-bit NormalFloat): 更适合正态分布权重的量化格式
2. Double Quantization: 对量化常数也做量化，再省0.4 bits/参数
3. Paged Optimizers: 利用CPU内存作为GPU显存的"交换空间"

结果：
  原本需要48GB显存的65B模型微调 → QLoRA只需12GB！
  一块RTX 3090 (24GB) 就能微调70B模型
"""

import torch
from transformers import BitsAndBytesConfig

# ========== 4-bit量化配置 ==========
bnb_config = BitsAndBytesConfig(
    load_in_4bit=True,                    # 4bit量化
    bnb_4bit_quant_type="nf4",           # NF4量化类型
    bnb_4bit_compute_dtype=torch.bfloat16, # 计算时使用的精度
    bnb_4bit_use_double_quant=True,      # 双重量化
)

# ========== 加载量化模型 ==========
model = AutoModelForCausalLM.from_pretrained(
    model_name,
    quantization_config=bnb_config,
    device_map="auto",
    torch_dtype=torch.bfloat16,
    trust_remote_code=True,
)

# ========== 准备k-bit训练 ==========
model = prepare_model_for_kbit_training(model)

# ========== 应用LoRA（与普通LoRA完全一样）==========
lora_config = LoraConfig(
    r=8,
    lora_alpha=16,
    target_modules=["q_proj", "k_proj", "v_proj", "o_proj",
                    "gate_proj", "up_proj", "down_proj"],
    lora_dropout=0.05,
    bias="none",
    task_type=TaskType.CAUSAL_LM,
)

model = get_peft_model(model, lora_config)
model.print_trainable_parameters()

# 后续训练与普通LoRA完全相同！
```

### 15.4.2 加载和使用微调模型

```python
# ========== 方法1: 合并权重（推荐用于部署）==========
from peft import PeftModel

# 加载基础模型
base_model = AutoModelForCausalLM.from_pretrained(
    model_name,
    torch_dtype=torch.float16,
    device_map="auto",
)

# 加载LoRA权重
model = PeftModel.from_pretrained(base_model, "./lora-weights")

# 合并LoRA权重到基础模型（推荐！推理更快）
model = model.merge_and_unload()

# 保存为完整模型
model.save_pretrained("./merged-model")
tokenizer.save_pretrained("./merged-model")

# ========== 方法2: 动态加载（方便调试/切换）==========
# 不合并，推理时动态应用LoRA
model = PeftModel.from_pretrained(base_model, "./lora-weights")
model.eval()

# 可以轻松切换不同的LoRA adapter
# model.load_adapter("./lora-customer-service", "customer_service")
# model.load_adapter("./lora-code-review", "code_review")
# model.set_adapter("customer_service")

# ========== 推理 ==========
def generate_with_lora(prompt: str, max_new_tokens: int = 256) -> str:
    inputs = tokenizer(prompt, return_tensors="pt").to(model.device)
    
    with torch.no_grad():
        outputs = model.generate(
            **inputs,
            max_new_tokens=max_new_tokens,
            temperature=0.7,
            top_p=0.9,
            do_sample=True,
        )
    
    return tokenizer.decode(outputs[0], skip_special_tokens=True)
```

---

## 15.5 微调实战全流程

### 15.5.1 数据准备

```python
"""
微调数据三要素：质量 > 数量 > 多样性

1. 数据质量（最重要！）
   - 检查是否有错误答案
   - 输出格式是否一致
   - 是否存在矛盾的数据
   
2. 数据数量
   - 最少: 100条（验证可行性）
   - 推荐: 500-5000条（大多数场景）
   - 理想: 1万条以上（复杂任务）
   
3. 数据多样性
   - 覆盖各种输入情况
   - 包含边界case
   - 长文本和短文本混合

数据格式规范（Alpaca格式）：
{
  "instruction": "任务的指令描述",
  "input": "输入内容（可选）",
  "output": "期望的输出"
}
"""

class DataPreparator:
    """微调数据准备器"""
    
    def __init__(self, tokenizer, max_length: int = 512):
        self.tokenizer = tokenizer
        self.max_length = max_length
    
    def prepare(self, data: List[dict]) -> Dataset:
        """准备训练数据"""
        # 1. 格式化为Prompt
        formatted_texts = []
        for item in data:
            formatted = self._format_prompt(item)
            formatted_texts.append(formatted)
        
        # 2. Tokenize
        encodings = self.tokenizer(
            formatted_texts,
            truncation=True,
            padding='max_length',
            max_length=self.max_length,
            return_tensors='pt',
        )
        
        # 3. 构造labels（Causal LM: labels = input_ids）
        # 关键：只在"回答"部分计算loss！
        labels = encodings['input_ids'].clone()
        
        for i, text in enumerate(formatted_texts):
            # 找到"### 回答:"的位置，前面的token标记为-100（忽略loss）
            response_marker = "### 回答:"
            prompt_len = len(self.tokenizer.encode(
                text[:text.find(response_marker) + len(response_marker)]
            ))
            labels[i, :prompt_len] = -100
        
        return Dataset.from_dict({
            'input_ids': encodings['input_ids'],
            'attention_mask': encodings['attention_mask'],
            'labels': labels,
        })
    
    def _format_prompt(self, item: dict) -> str:
        """格式化Prompt模板"""
        prompt = f"""### 指令:
{item['instruction']}"""
        
        if item.get('input'):
            prompt += f"""

### 输入:
{item['input']}"""
        
        prompt += f"""

### 回答:
{item['output']}"""
        
        return prompt
    
    def validate_data(self, data: List[dict]) -> List[str]:
        """数据质量验证"""
        issues = []
        
        for i, item in enumerate(data):
            if not item.get('instruction'):
                issues.append(f"第{i}条: 缺少instruction")
            if not item.get('output'):
                issues.append(f"第{i}条: 缺少output")
            if len(item.get('output', '')) < 5:
                issues.append(f"第{i}条: output过短")
            
            # 检查一致性
            instruction_len = len(item.get('instruction', ''))
            if instruction_len > self.max_length * 3:
                issues.append(f"第{i}条: instruction过长({instruction_len}字符)")
        
        return issues
```

### 15.5.2 完整的训练脚本

```python
# train_lora.py — 完整的LoRA微调脚本
import argparse
import os
import torch
from transformers import (
    AutoModelForCausalLM, AutoTokenizer,
    TrainingArguments, Trainer,
    BitsAndBytesConfig,
)
from peft import LoraConfig, get_peft_model, prepare_model_for_kbit_training
from datasets import load_dataset

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--model", default="Qwen/Qwen2-0.5B")
    parser.add_argument("--data", required=True)
    parser.add_argument("--output", default="./output")
    parser.add_argument("--lora_r", type=int, default=8)
    parser.add_argument("--epochs", type=int, default=3)
    parser.add_argument("--batch_size", type=int, default=4)
    parser.add_argument("--learning_rate", type=float, default=2e-4)
    parser.add_argument("--use_4bit", action="store_true")
    args = parser.parse_args()
    
    # 加载模型
    if args.use_4bit:
        bnb_config = BitsAndBytesConfig(
            load_in_4bit=True,
            bnb_4bit_quant_type="nf4",
            bnb_4bit_compute_dtype=torch.bfloat16,
            bnb_4bit_use_double_quant=True,
        )
        model = AutoModelForCausalLM.from_pretrained(
            args.model,
            quantization_config=bnb_config,
            device_map="auto",
        )
        model = prepare_model_for_kbit_training(model)
    else:
        model = AutoModelForCausalLM.from_pretrained(
            args.model,
            torch_dtype=torch.float16,
            device_map="auto",
        )
    
    tokenizer = AutoTokenizer.from_pretrained(args.model)
    if tokenizer.pad_token is None:
        tokenizer.pad_token = tokenizer.eos_token
    
    # 配置LoRA
    lora_config = LoraConfig(
        r=args.lora_r,
        lora_alpha=args.lora_r * 2,
        target_modules=["q_proj", "k_proj", "v_proj", "o_proj",
                        "gate_proj", "up_proj", "down_proj"],
        lora_dropout=0.05,
        bias="none",
        task_type="CAUSAL_LM",
    )
    model = get_peft_model(model, lora_config)
    model.print_trainable_parameters()
    
    # 加载数据
    dataset = load_dataset("json", data_files=args.data, split="train")
    preparator = DataPreparator(tokenizer)
    train_data = preparator.prepare(dataset)
    
    # 训练
    training_args = TrainingArguments(
        output_dir=args.output,
        num_train_epochs=args.epochs,
        per_device_train_batch_size=args.batch_size,
        gradient_accumulation_steps=4,
        learning_rate=args.learning_rate,
        lr_scheduler_type="cosine",
        warmup_ratio=0.03,
        logging_steps=10,
        save_strategy="epoch",
        bf16=True,
        gradient_checkpointing=True,
        report_to="none",
    )
    
    trainer = Trainer(
        model=model,
        args=training_args,
        train_dataset=train_data,
    )
    
    trainer.train()
    
    # 保存
    model.save_pretrained(args.output)
    tokenizer.save_pretrained(args.output)
    
    print(f"✓ 训练完成！模型保存在 {args.output}")

if __name__ == "__main__":
    main()

# 运行:
# python train_lora.py --data training_data.json --use_4bit --epochs 3
```

---

## 15.6 评估与迭代

```python
class FineTuneEvaluator:
    """微调模型评估器"""
    
    def __init__(self, base_model, finetuned_model, tokenizer):
        self.base_model = base_model
        self.finetuned_model = finetuned_model
        self.tokenizer = tokenizer
    
    def compare_responses(self, test_cases: List[dict]) -> List[dict]:
        """对比微调前后的效果"""
        results = []
        
        for case in test_cases:
            prompt = self._format_prompt(case)
            
            # 基础模型输出
            base_output = self._generate(self.base_model, prompt)
            
            # 微调模型输出
            ft_output = self._generate(self.finetuned_model, prompt)
            
            results.append({
                "instruction": case["instruction"],
                "expected": case.get("output", ""),
                "base_model": base_output,
                "finetuned": ft_output,
            })
        
        return results
    
    def evaluate_format_compliance(self, test_samples: int = 100) -> float:
        """评估输出格式的合规率"""
        # 检查输出是否符合预期的JSON格式、长度等
        pass
    
    def evaluate_safety(self, adversarial_prompts: List[str]) -> dict:
        """安全性评估"""
        # 检查微调后是否有安全退化
        pass
    
    def _generate(self, model, prompt: str) -> str:
        inputs = self.tokenizer(prompt, return_tensors="pt").to(model.device)
        with torch.no_grad():
            outputs = model.generate(**inputs, max_new_tokens=256, 
                                     temperature=0.1, do_sample=False)
        return self.tokenizer.decode(outputs[0], skip_special_tokens=True)
```

---

## 15.7 阶段练习

### 练习1：LoRA微调小模型
在Qwen2-0.5B上用100条自定义数据完成一次LoRA微调。

### 练习2：数据质量实验
用"干净数据"和"脏数据"分别微调，对比效果差异，体会数据质量的重要性。

### 练习3：超参数搜索
在同一个数据集上，对比r=4/r=8/r=16三种配置的效果。

---

> **✅ 阶段完成检查清单：**
> - [ ] 理解微调与RAG/Prompt Engineering的适用场景
> - [ ] 掌握LoRA的原理和配置
> - [ ] 能用QLoRA在消费级GPU上微调7B+模型
> - [ ] 知道如何准备高质量的训练数据
> - [ ] 完成至少一次完整的微调→评估→迭代流程
> - [ ] 完成3个阶段练习
>
> **下一步：** [第16步：模型部署与评估](../16-模型部署与评估/README.md)
