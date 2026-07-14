# LoRA 与 QLoRA：大模型参数高效微调实战

> **核心摘要**：全量微调 7B 模型需要约 56GB 显存，LoRA 只需要约 12GB，QLoRA 更只需约 6GB——让个人开发者也能微调大模型。本文涵盖 LoRA 原理、完整微调脚本、QLoRA 量化方案及部署导出。

> 前置阅读：[[大模型参数高效微调（PEFT）核心知识点]]

---

## 一、为什么需要 PEFT（参数高效微调）？

### 全量微调的问题

| 问题 | 影响 |
|------|------|
| 显存需求巨大 | 7B 模型全量微调需 60GB+ 显存 |
| 训练时间长 | 单卡训练数天 |
| 灾难性遗忘 | 容易丢失通用能力 |
| 部署成本高 | 每个下游任务一个完整模型副本 |

### PEFT 的思路

| 方案 | 更新参数量 |
|------|-----------|
| 全量微调 | 更新所有 70 亿参数 → 约 14GB 权重更新 |
| LoRA | 只更新 0.1% 参数 → 约 15MB 权重更新 |

## 二、LoRA 原理速通

### 2.1 一句话理解

```
原始： W_output = W_original @ x
LoRA： W_output = W_original @ x  +  (B @ A) @ x
                   ↑ 冻结不动           ↑ 只练这个（低秩矩阵）
```

LoRA 在原始权重旁加一条"旁路"（两个小矩阵 A × B），只训练旁路，原始权重冻结。训练完后旁路可以合并回原模型（推理无额外开销），也可以保持独立（方便切换不同任务的 adapter）。

### 2.2 关键参数

| 参数 | 含义 | 推荐值 |
|------|------|--------|
| `r` (rank) | 低秩矩阵的秩，越大表达能力越强 | 8-64（一般 8 或 16） |
| `lora_alpha` | 缩放系数 | 通常 = r 或 2×r |
| `target_modules` | 在哪些层插入 LoRA | Qwen 用 `["q_proj", "v_proj"]` |
| `lora_dropout` | Dropout 比例 | 0.05-0.1 |

## 三、LoRA 微调实战

### 3.1 环境准备

```bash
pip install transformers peft datasets accelerate bitsandbytes
pip install torch  # 建议用 conda 安装 CUDA 版本
```

### 3.2 数据集准备

```python
code_review_data = [
    {
        "instruction": "审查以下 Java 代码，找出潜在问题",
        "input": """
public void transfer(Account from, Account to, BigDecimal amount) {
    from.setBalance(from.getBalance().subtract(amount));
    to.setBalance(to.getBalance().add(amount));
}
""",
        "output": """
问题1：缺少事务管理，如果第二步失败会导致资金丢失
问题2：from.getBalance() 可能返回 null，需做空值判断
问题3：应使用 compareTo 而非 subtract 来判断余额是否足够
修复建议：添加 @Transactional 注解，使用 Optional 处理 null，在扣款前检查余额是否充足。
"""
    },
]

from datasets import Dataset
dataset = Dataset.from_list(code_review_data)
dataset = dataset.train_test_split(test_size=0.1)
```

### 3.3 核心训练代码

```python
import torch
from transformers import AutoTokenizer, AutoModelForCausalLM, TrainingArguments, Trainer, DataCollatorForSeq2Seq
from peft import LoraConfig, get_peft_model, TaskType

def train_lora():
    # ========== 1. 加载模型 ==========
    model_name = "Qwen/Qwen2-7B-Instruct"
    tokenizer = AutoTokenizer.from_pretrained(model_name, trust_remote_code=True, padding_side="right")
    if tokenizer.pad_token is None:
        tokenizer.pad_token = tokenizer.eos_token

    model = AutoModelForCausalLM.from_pretrained(
        model_name, torch_dtype=torch.float16, device_map="auto", trust_remote_code=True
    )

    # ========== 2. 配置 LoRA ==========
    lora_config = LoraConfig(
        task_type=TaskType.CAUSAL_LM,
        r=8, lora_alpha=16, lora_dropout=0.05,
        target_modules=["q_proj", "k_proj", "v_proj", "o_proj"],
        bias="none",
    )
    model = get_peft_model(model, lora_config)
    model.print_trainable_parameters()
    # 输出：trainable params: 4,194,304 || all params: 7,073,013,760 || trainable%: 0.0593

    # ========== 3. 数据预处理 ==========
    def format_prompt(example):
        return {
            "text": f"""<|im_start|>system
你是一个Java代码审查专家。<|im_end|>
<|im_start|>user
{example['instruction']}

```java
{example['input']}
```<|im_end|>
<|im_start|>assistant
{example['output']}<|im_end|>"""
        }

    def tokenize(example):
        result = tokenizer(example["text"], truncation=True, max_length=1024, padding=False)
        result["labels"] = result["input_ids"].copy()
        return result

    train_dataset = dataset["train"].map(format_prompt).map(tokenize, remove_columns=dataset["train"].column_names)
    eval_dataset = dataset["test"].map(format_prompt).map(tokenize, remove_columns=dataset["test"].column_names)

    # ========== 4. 训练配置 ==========
    training_args = TrainingArguments(
        output_dir="./lora-code-reviewer",
        num_train_epochs=3,
        per_device_train_batch_size=4,
        per_device_eval_batch_size=4,
        gradient_accumulation_steps=4,
        learning_rate=2e-4,
        warmup_ratio=0.1,
        logging_steps=10,
        eval_strategy="steps",
        eval_steps=50,
        save_strategy="steps",
        save_steps=50,
        load_best_model_at_end=True,
        fp16=True,
        report_to="none",
    )

    # ========== 5. 开始训练 ==========
    trainer = Trainer(
        model=model, args=training_args,
        train_dataset=train_dataset, eval_dataset=eval_dataset,
        data_collator=DataCollatorForSeq2Seq(tokenizer, padding=True),
    )
    trainer.train()

    # ========== 6. 保存 adapter ==========
    model.save_pretrained("./lora-adapter-code-reviewer")
    tokenizer.save_pretrained("./lora-adapter-code-reviewer")
    print("LoRA adapter 已保存！只有 ~16MB")
```

### 3.4 加载与推理

```python
from peft import PeftModel

base_model = AutoModelForCausalLM.from_pretrained(
    "Qwen/Qwen2-7B-Instruct", torch_dtype=torch.float16, device_map="auto"
)
model = PeftModel.from_pretrained(base_model, "./lora-adapter-code-reviewer")

prompt = """<|im_start|>system
你是一个Java代码审查专家。<|im_end|>
<|im_start|>user
审查以下Java代码：
```java
String sql = "SELECT * FROM users WHERE id = " + userId;
```<|im_end|>
<|im_start|>assistant
"""
inputs = tokenizer(prompt, return_tensors="pt").to("cuda")
outputs = model.generate(**inputs, max_new_tokens=256, temperature=0.7)
print(tokenizer.decode(outputs[0], skip_special_tokens=True))
```

### 3.5 合并 Adapter 到基础模型

```python
merged_model = model.merge_and_unload()
merged_model.save_pretrained("./merged-code-reviewer")
```

## 四、QLoRA —— 更低显存的方案

### 4.1 原理

QLoRA = LoRA + 4-bit 量化，将原始权重压缩到 4-bit，释放大量显存。LoRA 的旁路矩阵仍用 FP16 训练，精度损失极小。

### 4.2 代码（与 LoRA 几乎一样，只改模型加载方式）

```python
from transformers import BitsAndBytesConfig

bnb_config = BitsAndBytesConfig(
    load_in_4bit=True,
    bnb_4bit_quant_type="nf4",           # 量化类型（nf4 效果最好）
    bnb_4bit_compute_dtype=torch.float16,  # 计算精度
    bnb_4bit_use_double_quant=True,       # 双重量化（再省 0.4 bit）
)

model = AutoModelForCausalLM.from_pretrained(
    "Qwen/Qwen2-7B-Instruct",
    quantization_config=bnb_config,
    device_map="auto",
    trust_remote_code=True
)

# 其余代码和 LoRA 完全一样
lora_config = LoraConfig(r=8, lora_alpha=16, ...)
model = get_peft_model(model, lora_config)
# ... Trainer 训练 ...
```

**显存对比**：

| 方案 | Qwen2-7B 显存 |
|------|---------------|
| 全量微调 | ~56 GB |
| LoRA (FP16) | ~14 GB |
| QLoRA (4bit) | ~6 GB |
| QLoRA (4bit + 1.5B 模型) | ~3 GB |

## 五、实战技巧

### 5.1 数据质量优先于数据量

> **重点**：200 条高质量数据 > 2000 条低质量数据。高质量数据特征：格式统一（都用 ChatML 格式）、指令明确、输出质量高（人工审核过的标准答案）、覆盖边缘情况。

### 5.2 训练过程监控

```python
training_args = TrainingArguments(
    report_to="tensorboard",
    logging_dir="./logs",
)
# 启动：tensorboard --logdir ./logs
# 关注：loss 下降趋势、eval_loss 是否反弹（过拟合）
```

### 5.3 避免常见陷阱

| 陷阱 | 现象 | 解法 |
|------|------|------|
| 过拟合 | eval_loss 上升而 train_loss 下降 | 加 Dropout / 减少 epoch / 增大数据 |
| 灾难性遗忘 | 微调后不会聊天了 | 训练数据中混入 5-10% 通用对话数据 |
| Loss 不下降 | Loss 横盘 | 降低 learning rate / 检查数据格式 |
| OOM | CUDA Out of Memory | 减小 batch_size / 增大 gradient_accumulation_steps / 用 QLoRA |

### 5.4 快速练手流程

```bash
# 1. 先用小模型在 CPU 上跑通流程（验证代码正确）
model_name = "Qwen/Qwen2-0.5B"  # 0.5B 参数，CPU 也能跑
# 2. 用 100 条数据训练 1 epoch（验证 loss 能下降）
# 3. 确认流程正确后，换 7B 模型 + 完整数据 + GPU 训练
```

## 六、模型格式转换

```python
# HuggingFace 模型 → GGUF（Ollama 格式）
# python convert_hf_to_gguf.py ./merged-model --outtype q4_k_m --outfile model.gguf

# 创建 Ollama Modelfile
modelfile = """
FROM ./model.gguf
TEMPLATE \"\"\"<|im_start|>system
{{ .System }}<|im_end|>
<|im_start|>user
{{ .Prompt }}<|im_end|>
<|im_start|>assistant
\"\"\"
SYSTEM \"你是一个Java代码审查专家。\"
"""
# ollama create java-reviewer -f Modelfile
# ollama run java-reviewer
```

## 核心要点回顾

- LoRA 在 Attention 层插入低秩矩阵 A×B，训练旁路冻结原权重，参数量仅为 0.1%
- QLoRA = LoRA + 4-bit 量化，显存需求进一步降低 50%+
- 数据质量 > 数据数量，格式一致性直接影响训练效果
- LoRA 学习率通常用 1e-4 到 5e-4，比全量微调高
- 合并 Adapter 后可导出为 GGUF 格式通过 Ollama 部署

## 快速调试检查清单

- [ ] CUDA 是否可用？`torch.cuda.is_available()` 返回 True？
- [ ] `target_modules` 是否匹配你的模型架构？
- [ ] 数据集格式是否一致？检查几条样本的 tokenize 结果
- [ ] `pad_token` 是否设置？未设置会导致 padding 错误
- [ ] 显存不够时是否先尝试了 `batch_size=1` + `gradient_accumulation_steps=8`？
- [ ] 微调后测试过通用对话能力是否退化？

## 参考资料

1. Hugging Face PEFT 文档 - LoRA 与 QLoRA 实现
2. Hu et al. "LoRA: Low-Rank Adaptation of Large Language Models" - ICLR 2022
3. Dettmers et al. "QLoRA: Efficient Finetuning of Quantized Language Models" - NeurIPS 2023
4. Ollama 官方文档 - Modelfile 格式说明
