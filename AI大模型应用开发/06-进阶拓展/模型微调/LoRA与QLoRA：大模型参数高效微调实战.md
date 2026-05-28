# LoRA 与 QLoRA：大模型参数高效微调实战

> **所属阶段**：阶段四 — 大模型微调与部署
> **前置知识**：Python、Transformer 基础概念
> **核心目标**：用有限 GPU 资源微调大模型，适配垂直领域

---

## 1. 为什么需要 PEFT（参数高效微调）

### 全量微调的问题

| 问题 | 影响 |
|------|------|
| 显存需求巨大 | 7B 模型全量微调需 60GB+ 显存 |
| 训练时间长 | 单卡训练数天 |
| 灾难性遗忘 | 容易丢失通用能力 |
| 部署成本高 | 每个下游任务一个完整模型副本 |

### PEFT 的思路

```
全量微调：更新所有 70 亿参数 → 70 亿 × 2 bytes ≈ 14GB 权重更新
LoRA：    只更新 0.1% 参数     → 约 15MB 权重更新
```

---

## 2. LoRA 原理

### 2.1 核心思想

在大模型的 Attention 层旁路插入低秩矩阵，仅训练这些旁路矩阵：

```
原始权重:  W ∈ R^(d×d)  (冻结，不更新)
LoRA:      W' = W + ΔW = W + B·A
           其中 B ∈ R^(d×r), A ∈ R^(r×d), r << d

典型值: d=4096, r=8
参数量: 4096×8×2 = 65536 (仅为原权重的 0.4%)
```

### 2.2 关键参数

| 参数 | 说明 | 推荐值 |
|------|------|--------|
| `r` (rank) | 低秩矩阵秩 | 8-32 |
| `alpha` | 缩放因子 | 16-32 |
| `target_modules` | 应用 LoRA 的层 | q_proj, v_proj |
| `dropout` | 正则化 | 0.05-0.1 |

---

## 3. 实战：微调 Qwen 模型

### 3.1 环境准备

```bash
pip install transformers datasets peft accelerate bitsandbytes
pip install torch torchvision  # 根据 CUDA 版本选择
```

### 3.2 准备数据集

```python
# prepare_data.py
from datasets import Dataset

# Java 代码审查数据集格式
data = [
    {
        "instruction": "审查以下 Java 代码，找出潜在问题并给出修复建议",
        "input": """
public class UserService {
    private UserDao dao = new UserDao();
    public User getUser(String id) {
        return dao.findById(id);
    }
}
""",
        "output": """发现以下问题：
1. **硬编码依赖 (严重)**: UserDao 直接 new 创建，应使用依赖注入
   修复: @Autowired private UserDao dao;
2. **缺少空值检查 (中等)**: dao.findById() 可能返回 null
   修复: return Optional.ofNullable(dao.findById(id));
3. **缺少接口抽象 (建议)**: UserService 应有接口定义
   修复: 提取 IUserService 接口"""
    },
    # ... 更多样本
]

def format_instruction(example):
    return {
        "text": f"""<|im_start|>system
你是 Java 代码审查专家。<|im_end|>
<|im_start|>user
{example['instruction']}
{example['input']}<|im_end|>
<|im_start|>assistant
{example['output']}<|im_end|>"""
    }

dataset = Dataset.from_list(data).map(format_instruction)
```

### 3.3 微调脚本

```python
# finetune.py
import torch
from transformers import (
    AutoModelForCausalLM,
    AutoTokenizer,
    TrainingArguments,
    Trainer,
    BitsAndBytesConfig,
)
from peft import LoraConfig, get_peft_model, prepare_model_for_kbit_training

# 1. 量化配置（QLoRA — 4bit 加载模型）
bnb_config = BitsAndBytesConfig(
    load_in_4bit=True,
    bnb_4bit_quant_type="nf4",
    bnb_4bit_compute_dtype=torch.bfloat16,
    bnb_4bit_use_double_quant=True,
)

# 2. 加载模型
model_name = "Qwen/Qwen2.5-7B-Instruct"
model = AutoModelForCausalLM.from_pretrained(
    model_name,
    quantization_config=bnb_config,
    device_map="auto",
    trust_remote_code=True,
)
tokenizer = AutoTokenizer.from_pretrained(model_name, trust_remote_code=True)
tokenizer.pad_token = tokenizer.eos_token

# 3. 准备 k-bit 训练
model = prepare_model_for_kbit_training(model)

# 4. LoRA 配置
lora_config = LoraConfig(
    r=16,                    # 低秩
    lora_alpha=32,           # 缩放
    target_modules=["q_proj", "k_proj", "v_proj", "o_proj"],  # Qwen 的 attention 层
    lora_dropout=0.05,
    bias="none",
    task_type="CAUSAL_LM",
)

model = get_peft_model(model, lora_config)
model.print_trainable_parameters()
# 输出: trainable params: 13,631,488 || all params: 7,628,591,104 || trainable%: 0.18%

# 5. 训练参数
training_args = TrainingArguments(
    output_dir="./qwen-java-reviewer",
    per_device_train_batch_size=2,
    gradient_accumulation_steps=4,   # 等效 batch_size = 2*4 = 8
    num_train_epochs=3,
    learning_rate=2e-4,
    fp16=True,
    logging_steps=10,
    save_steps=100,
    save_total_limit=2,
    warmup_ratio=0.03,
    lr_scheduler_type="cosine",
    remove_unused_columns=False,
)

# 6. 训练
trainer = Trainer(
    model=model,
    args=training_args,
    train_dataset=dataset,
    tokenizer=tokenizer,
    data_collator=lambda data: {
        "input_ids": torch.stack([d["input_ids"] for d in data]),
        "attention_mask": torch.stack([d["attention_mask"] for d in data]),
        "labels": torch.stack([d["input_ids"] for d in data]),
    }
)

trainer.train()

# 7. 保存
model.save_pretrained("./java-code-reviewer-lora")
tokenizer.save_pretrained("./java-code-reviewer-lora")
```

---

## 4. 模型推理

```python
# inference.py
from peft import PeftModel
from transformers import AutoModelForCausalLM, AutoTokenizer

base_model = "Qwen/Qwen2.5-7B-Instruct"
lora_weights = "./java-code-reviewer-lora"

model = AutoModelForCausalLM.from_pretrained(base_model, device_map="auto")
model = PeftModel.from_pretrained(model, lora_weights)
tokenizer = AutoTokenizer.from_pretrained(base_model)

def review_code(code):
    prompt = f"""<|im_start|>system
你是 Java 代码审查专家。<|im_end|>
<|im_start|>user
审查以下代码，找出潜在问题并给出修复建议：
{code}<|im_end|>
<|im_start|>assistant
"""
    inputs = tokenizer(prompt, return_tensors="pt").to(model.device)
    outputs = model.generate(
        **inputs, max_new_tokens=512, temperature=0.3, do_sample=True
    )
    return tokenizer.decode(outputs[0], skip_special_tokens=True)

# 测试
code = """
public void process(List<String> items) {
    for (int i = 0; i < items.size(); i++) {
        System.out.println(items.get(i));
    }
}
"""
print(review_code(code))
```

---

## 5. 合并与导出（Ollama 部署）

```python
# merge_and_export.py
from peft import PeftModel
from transformers import AutoModelForCausalLM, AutoTokenizer
import torch

# 合并 LoRA 权重到基础模型
base_model = AutoModelForCausalLM.from_pretrained(
    "Qwen/Qwen2.5-7B-Instruct",
    torch_dtype=torch.float16,
    device_map="auto"
)
model = PeftModel.from_pretrained(base_model, "./java-code-reviewer-lora")
merged_model = model.merge_and_unload()

# 保存合并后的模型
merged_model.save_pretrained("./java-code-reviewer-merged")
tokenizer = AutoTokenizer.from_pretrained("Qwen/Qwen2.5-7B-Instruct")
tokenizer.save_pretrained("./java-code-reviewer-merged")
```

```bash
# 创建 Ollama Modelfile
cat > Modelfile << 'EOF'
FROM ./java-code-reviewer-merged
TEMPLATE """{{ if .System }}<|im_start|>system
{{ .System }}<|im_end|>
{{ end }}<|im_start|>user
{{ .Prompt }}<|im_end|>
<|im_start|>assistant
"""
SYSTEM "你是 Java 代码审查专家，擅长发现代码问题和提出改进建议。"
EOF

# 导入 Ollama
ollama create java-reviewer -f Modelfile
```

---

## 6. 关键实践要点

| 要点 | 说明 |
|------|------|
| 高质量数据 | 数据质量 > 数据数量，100 条高质量样本优于 10000 条噪声 |
| 格式一致性 | 指令模板必须与基础模型训练格式一致 |
| 学习率 | LoRA 通常用 1e-4 到 5e-4，比全量微调高 |
| 过拟合 | 小数据集容易过拟合，监控 validation loss，增加 dropout |
| 显存不足 | 降低 batch_size，增加 gradient_accumulation_steps |
