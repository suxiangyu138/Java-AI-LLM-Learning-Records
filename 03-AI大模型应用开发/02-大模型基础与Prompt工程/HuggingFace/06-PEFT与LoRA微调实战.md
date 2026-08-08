# 06 PEFT 与 LoRA 微调实战
> 单卡微调大模型的标准姿势：LoRA/QLoRA 原理、PEFT 集成、训练与合并全流程

## 📚 目录
1. [为什么 LoRA 是 2026 微调默认](#1-为什么-lora-是-2026-微调默认)
2. [LoRA 原理与超参数](#2-lora-原理与超参数)
3. [QLoRA：4bit + LoRA 单卡微调](#3-qlora4bit--lora-单卡微调)
4. [PEFT 与 Trainer 集成](#4-peft-与-trainer-集成)
5. [训练完成：合并与导出](#5-训练完成合并与导出)
6. [实战案例：Qwen 客服模型微调](#6-实战案例qwen-客服模型微调)
7. [LoRA 调参与常见坑](#7-lora-调参与常见坑)
8. [核心要点](#8-核心要点)

---

## 1. 为什么 LoRA 是 2026 微调默认

**成本对比**（7B 模型，单卡 A100 40G）：

| 方式 | 可训练参数 | 显存 | 耗时（1 万样本） | 效果 |
|------|:---:|:---:|:---:|------|
| 全量微调 | 7B（100%） | 42GB+（需多卡） | 2-3 天 | 100% |
| **LoRA** | ~0.1%（数百万） | 20GB | 数小时 | 90-95% |
| **QLoRA** | ~0.1% | **10GB** | 数小时 | 85-90% |

**原理一句话**：冻结原模型，只训练低秩适配矩阵 `W' = W + BA`（B×A 乘积近似增量）：

```text
LoRA 数学本质：
  原权重 W（冻结，d×d）
  增量 ΔW ≈ B·A（B: d×r, A: r×d，r 通常 8-64）
  前向：h = (W + BA)x = Wx + BAx   ← 只多一次小矩阵乘

QLoRA = 原权重 4bit 量化 + LoRA 适配层（bf16）
  → 显存省 70% 的同时还能微调
```

> 🎯 **核心要点**：LoRA = "**冻结大模型，训练小补丁**"——效果接近全量、成本低两个量级、切换任务零成本（换 adapter 文件即可，模型本体不动）。**2026 年微调 7B+ 模型的默认选择，没有之一**。

## 2. LoRA 原理与超参数

```python
from peft import LoraConfig, get_peft_model

config = LoraConfig(
    task_type="CAUSAL_LM",
    r=16,                    # 秩：适配层的表达能力（8-64）
    lora_alpha=32,           # 缩放系数：alpha/r 决定更新强度
    lora_dropout=0.05,       # 防止过拟合
    target_modules=["q_proj", "k_proj", "v_proj", "o_proj",   # 挂哪些层
                    "gate_proj", "up_proj", "down_proj"],
    bias="none",             # 不训练 bias
)

model = get_peft_model(base_model, config)   # 冻结原权重 + 注入适配层
model.print_trainable_parameters()
# trainable params: 8,388,608 || all params: 7,500,000,000 || trainable%: 0.11
```

| 超参数 | 作用 | 推荐值 |
|--------|------|--------|
| `r` | 秩：表达能力 | 8（任务简单）/ 16（通用）/ 32-64（复杂任务） |
| `lora_alpha` | 更新强度 | alpha = r 或 2×r（经验） |
| `lora_dropout` | 防过拟合 | 0.05 |
| `target_modules` | 挂载层 | 注意力全部 + MLP（Qwen/LLaMA 标准集） |
| `task_type` | 任务类型 | CAUSAL_LM（对话）/ SEQ2SEQ / 其他 |

> 💡 `r` 不是越大越好：**r=64 与 r=8 效果差异通常 <2%，但参数量差 8 倍**。先 r=16 起步，效果不够再加大；target_modules 少挂 MLP 层可省显存（掉一点点效果）。

## 3. QLoRA：4bit + LoRA 单卡微调

```python
from transformers import AutoModelForCausalLM, BitsAndBytesConfig
from peft import LoraConfig, get_peft_model
import torch

# ① 4bit 量化底座（03 章 §4 配置）
bnb_config = BitsAndBytesConfig(
    load_in_4bit=True,
    bnb_4bit_quant_type="nf4",
    bnb_4bit_compute_dtype=torch.bfloat16,
    bnb_4bit_use_double_quant=True,
)

model = AutoModelForCausalLM.from_pretrained(
    "Qwen/Qwen2.5-7B-Instruct",
    quantization_config=bnb_config,
    device_map="auto",
    torch_dtype=torch.bfloat16,
)

# ② 挂 LoRA（4bit 模型上挂 bf16 适配层）
lora_config = LoraConfig(
    task_type="CAUSAL_LM", r=16, lora_alpha=32,
    lora_dropout=0.05,
    target_modules=["q_proj", "k_proj", "v_proj", "o_proj",
                    "gate_proj", "up_proj", "down_proj"],
)
model = get_peft_model(model, lora_config)

# ③ 训练（与 05 章 Trainer 完全相同，只是 lr 调高）
training_args = TrainingArguments(
    output_dir="./qlora-out",
    per_device_train_batch_size=2,     # 显存紧张就 1-2
    gradient_accumulation_steps=8,     # 等效 batch 16-32
    learning_rate=1e-4,                # ⭐ LoRA 用 1e-4 量级（比全量高）
    bf16=True,
    logging_steps=10,
    save_steps=500, save_total_limit=3,
    report_to="none",
)
trainer = Trainer(model=model, args=training_args,
                  train_dataset=train_ds, data_collator=collator)
trainer.train()
```

| QLoRA 要点 | 说明 |
|-----------|------|
| 显存 | 7B QLoRA ≈ 10GB（24G 卡轻松跑 13B） |
| 学习率 | **1e-4 起步**（适配层从头训，比全量 2e-5 高） |
| 计算精度 | bnb_4bit_compute_dtype=bf16（保质量） |
| 冻结 | get_peft_model 自动冻结原模型（只训 0.1%） |
| v5 兼容 | 量化一等公民，QLoRA 是标准路径 |

> 🎯 **核心要点**：QLoRA = **"10GB 显存微调 7B、24G 卡微调 13B"**——笔记本/单卡工作站就能干 2022 年需要多卡集群的活。学习率记住：**全量 2e-5、LoRA 1e-4**。

## 4. PEFT 与 Trainer 集成

```python
# ① 模型打包（get_peft_model）后，Trainer 用法零变化（05 章）
trainer = Trainer(
    model=model,                    # 已是 PEFT 模型
    args=training_args,
    train_dataset=train_ds,
    eval_dataset=eval_ds,
    data_collator=collator,
    compute_metrics=compute_metrics,
)
trainer.train()

# ② 训练后保存：默认只存适配器（小文件，~10-100MB）
model.save_pretrained("./lora-adapter")
# 结构：
# lora-adapter/
# ├── adapter_config.json      # LoRA 配置（r/alpha/挂载层）
# └── adapter_model.safetensors # 只含适配层权重（不含底座！）

# ③ 推理时加载：底座 + 适配器
from peft import PeftModel
base = AutoModelForCausalLM.from_pretrained("Qwen/Qwen2.5-7B-Instruct",
                                            device_map="auto", torch_dtype="auto")
model = PeftModel.from_pretrained(base, "./lora-adapter")
```

| PEFT 生态能力 | 说明 |
|--------------|------|
| 适配器热切换 | 同一底座挂不同 adapter = 不同能力（换任务零加载） |
| 存储极小 | 适配器 ~10-100MB vs 全量 14GB |
| 多适配器 | `PeftModel.from_pretrained(base, [adapter1, adapter2])` |
| 合并导出 | 需要时把适配器合进底座（§5） |
| 训练/推理同构 | 加载/保存/切换 API 一致 |

> 💡 **适配器即产品**：LoRA 产物就是一个小文件——分发/版本/AB 试验都围绕 adapter 做，底座模型人人共享一份。这是 PEFT 生态的商业模式（HF 上大量 adapter 共享）。

## 5. 训练完成：合并与导出

```python
# 场景一：只推 adapter（推荐，轻量）
model.push_to_hub("my-org/qwen-cs-adapter")

# 场景二：合并进底座，导出完整模型（部署无 peft 依赖 / 提效）
from peft import PeftModel
import torch

base = AutoModelForCausalLM.from_pretrained(
    "Qwen/Qwen2.5-7B-Instruct", torch_dtype=torch.bfloat16, device_map="cpu")
model = PeftModel.from_pretrained(base, "./lora-adapter")

merged = model.merge_and_unload()          # ⭐ 合并适配层 → 完整权重
merged.save_pretrained("./qwen-cs-full")   # 14GB 完整模型
tokenizer.save_pretrained("./qwen-cs-full")

# 合并后可直接 transformers serve 部署（04 章）
# transformers serve --model ./qwen-cs-full
```

| 合并场景 | 做法 | 理由 |
|---------|------|------|
| 分发/协作 | 只推 adapter | 小、可切换 |
| 部署推理 | 合并成完整模型 | 无 peft 依赖、推理略快 |
| 对比实验 | 各 adapter 独立 | 同一底座多版本 |

> ⚠️ 合并注意：**合并后权重变成"底座 + 增量"的完整 bf16 模型**（显存回到原大小）；合并过程在 CPU 做（省显存）；合并结果要重新跑一遍冒烟测试（推理质量确认）。

## 6. 实战案例：Qwen 客服模型微调

```python
# 全流程（浓缩 01-07 章）：客服问答 LoRA 微调
# 数据：10k 客服问答（05 章 §2 格式）

# 1. 环境（01 章）
# export HF_ENDPOINT=https://hf-mirror.com
# pip install transformers peft datasets accelerate bitsandbytes

# 2. 加载与 QLoRA（§3）
# 3. 训练（§3 + 05 章 Trainer，lr=1e-4, 3 epochs）
# 4. 评估（05 章 §7 compute_metrics + 抽样生成对比）
# 5. 合并与部署（§5）
#    transformers serve --model ./qwen-cs-full

# 6. 验收（对比微调前后）
before = base_model.generate("用户说：货还没到，很生气")   # 通用回答
after  = merged_model.generate("用户说：货还没到，很生气") # 客服语气 + 查单指引
```

**验收标准**：微调后模型在该领域回答的**语气/格式/专有名词**明显贴合训练数据；通用能力不应大幅退化（回测 MMLU 等基准，LoRA 通常退化 <1-2%）。

> 🎯 **核心要点**：完整实战闭环 = **数据 → QLoRA 训练 → 评估对比 → 合并部署**。别跳过"微调前 vs 微调后"的对比验收——**微调失败（数据问题）最常见信号就是"训完没变化"或"通用能力崩了"**。

## 7. LoRA 调参与常见坑

| # | 坑 | 现象 | 解法 |
|:---:|-----|------|------|
| 1 | 没冻结底座 | 显存爆炸 | get_peft_model 会冻结，检查 trainable%≈0.1% |
| 2 | 学习率太低 | 训了等于没训 | LoRA 用 1e-4 量级 |
| 3 | target_modules 不对 | 训练完没效果 | 按模型架构查标准集（Qwen/LLaMA 用注意力+MLP 全挂） |
| 4 | 显存 OOM | 训练崩 | batch=1 + 梯度累积 + gradient_checkpointing |
| 5 | adapter 加载报错 | 架构不匹配 | adapter 与底座必须同架构同版本 |
| 6 | 合并后效果变差 | 与未合并推理不一致 | 检查 dtype（合并用 bf16 对齐训练） |
| 7 | 过拟合（数据少） | 训练 loss 低但 eval 崩 | 加 dropout / 减 r / 增数据 |
| 8 | 灾难性遗忘 | 通用能力大降 | 混合通用数据（10-20%） |
| 9 | 中文没效果 | 分词器/模板错 | apply_chat_template 统一 |
| 10 | 多卡不均衡 | 某卡爆 | device_map="auto" + 检查 hf_device_map |

## 8. 核心要点

| 序号 | 要点 |
|:---:|------|
| 1 | LoRA = 冻结大模型 + 训练低秩补丁（W' = W + BA） |
| 2 | 成本：全量 2-3 天/42GB vs LoRA 数小时/20GB vs QLoRA 10GB |
| 3 | 超参起点：r=16、alpha=32、lr=1e-4（LoRA） |
| 4 | QLoRA = 4bit NF4 底座 + bf16 适配层，单卡微调标准姿势 |
| 5 | Trainer 集成零改动；adapter 小文件即产品 |
| 6 | 合并导出：merge_and_unload → 完整模型 → serve 部署 |
| 7 | 验收：微调前后对比 + 通用能力回测 |
| 8 | 十大坑：冻结检查/学习率/过拟合/灾难性遗忘 |

---

**下一模块**：[07-Datasets 与数据流水线](07-Datasets与数据流水线.md) / **返回总览**：[00-HuggingFace知识体系总览](00-HuggingFace知识体系总览.md)

## 参考来源

- [PEFT 官方文档](https://huggingface.co/docs/peft/index)
- [PEFT：LoRA 指南](https://huggingface.co/docs/peft/en/developer_guides/lora)
- [HF 官方：QLoRA 微调教程](https://huggingface.co/docs/transformers/en/peft)
- [HF 博客：LoRA 原理（Efficient Large Language Model Fine-tuning）](https://huggingface.co/blog/peft)
