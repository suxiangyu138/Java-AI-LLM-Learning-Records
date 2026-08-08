# 05 微调与 Trainer
> 让模型学会你的业务：训练全流程、TrainingArguments v5 变化、断点续训与评估

## 📚 目录
1. [微调全景：从预训练到领域模型](#1-微调全景从预训练到领域模型)
2. [训练数据准备](#2-训练数据准备)
3. [Trainer 完整训练流程](#3-trainer-完整训练流程)
4. [TrainingArguments 参数全解（v5 变化）](#4-trainingarguments-参数全解v5-变化)
5. [DataCollator 与批处理](#5-datacollator-与批处理)
6. [断点续训与检查点](#6-断点续训与检查点)
7. [评估与推送 Hub](#7-评估与推送-hub)
8. [常见坑](#8-常见坑)
9. [核心要点](#9-核心要点)

---

## 1. 微调全景：从预训练到领域模型

```text
训练金字塔（成本递增）：
  预训练（Pre-training）   千卡·月级  巨头专属
     ↓
  继续预训练（Domain）     卡·周级    行业基座（代码/医疗/法律）
     ↓
  指令微调（SFT）          卡·天级    ⭐ 本体系重点（让模型学会任务）
     ↓
  对齐（RLHF/DPO）        卡·天级    安全与偏好
     ↓
  参数高效微调（LoRA）     单卡·小时级 ⭐ 06 章（性价比之王）
```

| 微调方式 | 数据量 | 算力 | 效果 | 适用 |
|---------|:---:|:---:|------|------|
| 全量微调 | 10 万+ | 多卡多天 | 最强 | 领域底座 |
| **LoRA/QLoRA** | 1 万-10 万 | 单卡数小时 | 强（90%+） | ⭐ 大多数场景 |
| 提示工程/少样本 | 少量 | 无 | 中 | 快速验证 |
| RAG | 文档库 | 无 | 按需 | 知识型 |

> 🎯 **核心要点**：**2026 年的微调首选是 LoRA 而非全量**（06 章）——效果接近、成本低两个量级。全量微调只在"要换模型能力分布"（领域底座）时才有必要。本章 Trainer 全流程同样适用于 LoRA（06 章是叠加层）。

## 2. 训练数据准备

```python
# 指令微调数据格式：messages 结构（与 chat template 一致）
train_data = [
    {"messages": [
        {"role": "system", "content": "你是客服助手。"},
        {"role": "user", "content": "怎么申请退款？"},
        {"role": "assistant", "content": "请在订单页点击退款，选择原因即可。"},
    ]},
    {"messages": [...更多样本...]},
]

# 保存为 jsonl（每条一行）或 json 数组
import json
with open("train.jsonl", "w", encoding="utf-8") as f:
    for sample in train_data:
        f.write(json.dumps(sample, ensure_ascii=False) + "\n")
```

```python
# 用 Datasets 加载 + 转成训练格式（07 章详解）
from datasets import load_dataset
from transformers import AutoTokenizer

ds = load_dataset("json", data_files="train.jsonl")
tokenizer = AutoTokenizer.from_pretrained("Qwen/Qwen2.5-7B-Instruct")

def format_example(example):
    return {
        "text": tokenizer.apply_chat_template(
            example["messages"], tokenize=True, add_generation_prompt=False
        )
    }

train_ds = ds["train"].map(format_example)   # 07 章：map 批处理与缓存
```

| 数据质量要点 | 说明 |
|------------|------|
| 样本格式 | messages 三件套（system/user/assistant） |
| 数量 | LoRA 1 万+ / 全量 10 万+（质量 > 数量） |
| 去重清洗 | 07 章 dedup/filter |
| 多样性 | 任务类型/语言/难度均衡 |
| 长度分布 | 与目标推理长度一致 |
| 不要有注入 | 训练数据里的指令模式会被模型学到（05 章 FC 安全联动） |

> 💡 数据是微调的上限：**"模型学不会 = 数据没给够/给错"是 90% 微调失败的原因**。先小数据跑通流程，再扩数据。

## 3. Trainer 完整训练流程

```python
from transformers import (
    AutoModelForCausalLM, AutoTokenizer, Trainer, TrainingArguments,
    DataCollatorForLanguageModeling,
)
from datasets import load_dataset

# ① 模型与分词器
model = AutoModelForCausalLM.from_pretrained("Qwen/Qwen2.5-7B-Instruct")
tokenizer = AutoTokenizer.from_pretrained("Qwen/Qwen2.5-7B-Instruct")
tokenizer.pad_token = tokenizer.eos_token          # 常见缺省修复

# ② 训练参数（v5 写法）
args = TrainingArguments(
    output_dir="./qwen-sft",
    num_train_epochs=3,                # 或 max_steps=1000
    per_device_train_batch_size=4,
    gradient_accumulation_steps=8,     # 等效 batch = 4×8=32
    learning_rate=2e-5,
    lr_scheduler_type="cosine",
    warmup_steps=100,
    bf16=True,                         # 大模型训练精度
    logging_steps=10,
    save_steps=500,                    # 每 500 步存检查点
    eval_strategy="steps",             # v5 命名（v4 是 evaluation_strategy）
    eval_steps=500,
    load_best_model_at_end=True,
    report_to="none",                  # v5 默认已是 none，显式更稳
    gradient_checkpointing=True,       # 省显存（慢一点）
)

# ③ 数据整理器
data_collator = DataCollatorForLanguageModeling(
    tokenizer=tokenizer, mlm=False,    # CausalLM 用 mlm=False
)

# ④ Trainer
trainer = Trainer(
    model=model,
    args=args,
    train_dataset=train_ds,
    eval_dataset=eval_ds,
    data_collator=data_collator,
)

# ⑤ 开始
trainer.train()
```

> 🎯 **核心要点**：Trainer = "**训练循环的官方封装**"——前向/反向/梯度累积/日志/检查点/评估全内置，你只管数据与参数。**等效 batch size = per_device_batch × 梯度累积 × 卡数**，这是调参第一公式。

## 4. TrainingArguments 参数全解（v5 变化）

| 参数 | 作用 | 推荐值 |
|------|------|--------|
| `output_dir` | 输出目录（必填） | 独立目录 |
| `num_train_epochs` / `max_steps` | 训练量 | 3 epochs 或 1000-3000 steps |
| `per_device_train_batch_size` | 单卡批大小 | 显存决定（4-16） |
| `gradient_accumulation_steps` | 梯度累积 | 等效 batch 32-64 |
| `learning_rate` | 学习率 | 全量 2e-5 / LoRA 1e-4（06 章） |
| `lr_scheduler_type` | 调度器 | cosine（稳定） |
| `warmup_steps` | 预热 | 100-500 |
| `bf16` / `fp16` | 精度 | bf16（Ampere+） |
| `gradient_checkpointing` | 省显存（换速度） | 显存紧张时开 |
| `save_steps` / `save_total_limit` | 检查点频率/上限 | 500 步 / 3 份 |
| `eval_strategy` | 评估策略（**v5 命名**） | steps 或 epoch |
| `load_best_model_at_end` | 结束加载最优 | True |
| `report_to` | 日志平台 | **v5 默认 "none"**（显式设 wandb/tensorboard） |
| `push_to_hub` | 训练完推送 | 手动推更稳（§7） |

**v5 移除的参数**（迁移注意）：`fp16_backend`、`no_cuda`、`per_gpu_train_batch_size`、`push_to_hub_token`、`fsdp_min_num_params` 等——老教程里看到这些参数就要查迁移指南（02 章 §6）。

> ⚠️ 显存预算公式：**训练显存 ≈ 参数量 × (梯度 2 + 优化器 2 + 模型 2) 字节（fp16/bf16）**——7B 全量训练 ≈ 7×6=42GB+（单卡 48G 勉强），这就是为什么 7B+ 微调默认 LoRA（06 章）。

## 5. DataCollator 与批处理

```python
from transformers import DataCollatorForLanguageModeling, DataCollatorForSeq2Seq

# CausalLM（对话/生成）：mlm=False → 训练目标 = 预测下一个 token
collator_lm = DataCollatorForLanguageModeling(tokenizer=tokenizer, mlm=False)

# 关键行为：batch 内 padding 到最长（右侧填充 pad token）
# 训练时 mask 掉 pad 位置的 loss（自动）
```

| Collator | 用途 | 说明 |
|---------|------|------|
| `DataCollatorForLanguageModeling` | CausalLM | mlm=False 即"下一 token 预测" |
| `DataCollatorForSeq2Seq` | 翻译/摘要 | 显式 labels |
| `DataCollatorWithPadding` | 分类等 | 简单 padding |
| 自定义 | 特殊任务 | 继承实现 call |

> 💡 padding 策略：**训练用右填充 + pad token**（与模型一致）；`padding="max_length"` 固定长度可加速（省动态 reshape）但浪费显存；短样本过多时考虑 packing（多样本拼一条，v5 生态常用）。

## 6. 断点续训与检查点

```bash
# 中断后恢复（trainer.train() 内部检查 output_dir）
# 自动加载最新 checkpoint 继续
trainer.train(resume_from_checkpoint=True)
```

```python
# 检查点结构
# output_dir/
# ├── checkpoint-500/    ← 每 save_steps 存一份（模型+优化器+调度器状态）
# ├── checkpoint-1000/
# ├── checkpoint-1500/
# └── best_model/        ← load_best_model_at_end=True 时保存最优

# 推理时加载检查点
from transformers import AutoModelForCausalLM
model = AutoModelForCausalLM.from_pretrained("./qwen-sft/checkpoint-1500")
```

| 检查点管理 | 做法 |
|-----------|------|
| 频率 | save_steps=500 或每 epoch |
| 上限 | save_total_limit=3（防磁盘爆炸） |
| 最优模型 | load_best_model_at_end + metric_for_best_model |
| 续训 | resume_from_checkpoint=True |
| 显存 | 检查点 = 磁盘开销，不是显存开销 |

> ⚠️ **训练中途断点是最常见事故**：没开 save 或 save_total_limit 太大 → 磁盘满 → 训练崩溃。生产训练脚本：save_steps 必须设 + save_total_limit=3 + 磁盘监控（09 章）。

## 7. 评估与推送 Hub

```python
# 评估：用 evaluate 库（08 章是应用侧；训练侧内嵌）
from evaluate import load
from transformers import Trainer

# 自定义评估指标（Trainer 的 compute_metrics）
def compute_metrics(eval_pred):
    preds, labels = eval_pred
    # 生成类任务：逐条 decode 后对比（更真实但慢）
    return {"accuracy": ...}

trainer = Trainer(..., compute_metrics=compute_metrics)
trainer.evaluate()
```

```python
# 推送 Hub（01 章 §7）
# ① 训练后合并/整理（LoRA 见 06 章）
# ② 推送
model.push_to_hub("my-org/my-sft-model", token=...)   # 或命令行
tokenizer.push_to_hub("my-org/my-sft-model")
```

> 🎯 **核心要点**：**评估与训练同样重要**——`compute_metrics` 在训练中每个 eval 周期报告指标，比训练完再评估早发现过拟合/数据问题。推送 Hub 让微调产物可版本化、可复现、可协作（01 章 §7）。

## 8. 常见坑

| # | 坑 | 现象 | 解法 |
|:---:|-----|------|------|
| 1 | pad_token 缺失 | 训练警告/崩溃 | `tokenizer.pad_token = eos_token` |
| 2 | 显存 OOM | 训练中断 | 梯度累积 + gradient_checkpointing + LoRA |
| 3 | 学习率过大 | loss 发散 | 全量 2e-5 起步 |
| 4 | 检查点磁盘爆炸 | 训练崩 | save_total_limit=3 |
| 5 | 中文乱码数据 | 训练集编码错 | jsonl 强制 utf-8 |
| 6 | 数据长度超限 | 截断丢失 | 检查 max_length 与数据分布 |
| 7 | v4 参数 | 报错/警告 | 迁移清单（eval_strategy 等） |
| 8 | 训练 loss 降但效果差 | 评估缺失 | compute_metrics + 生成对比 |
| 9 | 忘记 resume | 白训几天 | resume_from_checkpoint=True |
| 10 | 过拟合 | eval 指标上升后下降 | early stopping / 数据增强 |

## 9. 核心要点

| 序号 | 要点 |
|:---:|------|
| 1 | 微调路线：LoRA（默认）→ 全量（领域底座）→ 预训练（巨头） |
| 2 | 数据是上限：messages 三件套 + 质量 > 数量 |
| 3 | Trainer 全流程：数据 → 参数 → collator → train → 评估 |
| 4 | 等效 batch = per_device × 累积 × 卡数 |
| 5 | v5 变化：eval_strategy 命名、report_to 默认 none、旧参数移除 |
| 6 | 训练显存 ≈ 6 字节/参数（bf16）→ 大模型默认 LoRA |
| 7 | 检查点：save_steps + save_total_limit=3 + resume |
| 8 | 评估内嵌 compute_metrics，产物推 Hub 版本化 |

---

**下一模块**：[06-PEFT 与 LoRA 微调实战](06-PEFT与LoRA微调实战.md) / **返回总览**：[00-HuggingFace知识体系总览](00-HuggingFace知识体系总览.md)

## 参考来源

- [Transformers 官方：Trainer 文档](https://huggingface.co/docs/transformers/en/main_classes/trainer)
- [Transformers 官方：TrainingArguments](https://huggingface.co/docs/transformers/en/main_classes/trainer#transformers.TrainingArguments)
- [Transformers v5 迁移指南（Trainer 参数变更）](https://huggingface.co/docs/transformers/v5.0.0/en/migration)
- [HF 官方：指令微调教程](https://huggingface.co/docs/transformers/en/tasks/language_modeling)
