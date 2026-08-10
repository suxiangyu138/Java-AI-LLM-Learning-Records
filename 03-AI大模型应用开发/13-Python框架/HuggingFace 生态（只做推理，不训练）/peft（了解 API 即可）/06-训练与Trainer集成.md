# 06 - 训练与 Trainer 集成

> 定位：实际训练怎么组织——包装前后的差异、TrainingArguments 要点、数据格式、显存账本、保存与恢复——"PEFT 不发明训练循环，它只负责让 99% 的参数不参与训练——循环与数据仍是 transformers 的活"

---

## 📚 目录

1. [包装前后：模型差异在哪里](#1-包装前后模型差异在哪里)
2. [TrainingArguments 要点](#2-trainingarguments-要点)
3. [数据集与格式](#3-数据集与格式)
4. [显存账本](#4-显存账本)
5. [保存、恢复与推送](#5-保存恢复与推送)
6. [评估与迭代](#6-评估与迭代)
7. [完整训练模板](#7-完整训练模板)
8. [五个常见坑](#8-五个常见坑)
9. [练习 5 题](#9-练习-5-题)

---

## 1. 包装前后：模型差异在哪里

`get_peft_model` 包装后，**模型对象变了，训练体验不变**：

- **参数量**：`model.num_parameters()` 不变（还是底座的量），但 `peft_model.print_trainable_parameters()` 显示可训练占比 0.1%-2%——**优化器只挂这两项参数的状态**（Adam 的 m/v 按可训练参数算），这就是显存与内存大省的关键；
- **梯度**：冻结参数在前向时也参与计算图，但 `requires_grad=False` 让它们**不累积梯度、不进优化器**——省的是优化器状态与梯度存储，前向开销不变；
- **保存**：`save_pretrained` 只写 adapter（04 篇文件结构），**Trainer 的 save 系列回调全部兼容**（它只调用模型的 save_pretrained）。

**"包装即全部差异"**——Trainer、数据加载、评估、分布式（accelerate）都无感工作，**PEFT 与 transformers 训练体系是零摩擦集成**。

**这个设计的深意**：可训练参数可以随时加减（`model.disable_adapter()` 后继续训练就是"底座与 adapter 解耦"实验）——**训练脚本不用为 PEFT 写任何分支**："包装即差异"保证了生态兼容——评估（evaluate）、模型并行（accelerate）、日志回调全部原样工作。

## 2. TrainingArguments 要点

```python
TrainingArguments(
    output_dir="./lora-out",
    per_device_train_batch_size=4,     # LoRA 可训练参数少，batch 可以比全量大
    gradient_accumulation_steps=8,     # 等效 batch = per_device × grad_acc × 卡数
    learning_rate=2e-4,                # LoRA 常用 1e-4~5e-4（全量微调的 5e-5 太小）
    lr_scheduler_type="cosine",
    num_train_epochs=3,
    bf16=True,                         # 现代 GPU（H100/A100/4090）首选；老卡 fp16
    gradient_checkpointing=True,       # 显存紧必开（QLoRA 场景尤其）
    optim="adamw_torch",
    logging_steps=10, save_strategy="epoch",
    save_only_model=True,              # 只存模型不存优化器（adapter 场景推荐）
    eval_strategy="steps", eval_steps=50,
)
```

**三个 LoRA 特有注意点**：其一，**学习率比全量微调大一个量级**——可训练参数少、优化器状态小，2e-4 起是惯例；其二，**batch 可以放大**——梯度里只有 LoRA 分支占内存，per_device_batch 4-16 常见；其三，**save_only_model=True**——adapter 只有几 MB，恢复训练的概率低，不必存几百 MB 的优化器状态。

**其余值得动的旋钮**：`warmup_ratio=0.03~0.1`（前 3-10% 步学习率线性爬升，防起步震荡）、`weight_decay=0.01`（LoRA 分支的正则，防过拟合）、`max_grad_norm=1.0`（梯度裁剪，防 loss 尖峰）——**这三个是"训练不稳定"时最先开的药**；多卡场景用 `accelerate launch` 或 DeepSpeed——**adapter 参数少、通信开销小，LoRA 多卡收益接近线性**。

## 3. 数据集与格式

训练数据是**指令-回答配对**，格式由任务决定，常用两种：

- **chat 模板**：用 tokenizer 自带模板组装——`tokenizer.apply_chat_template([{"role": "user", "content": "..."}, ...], tokenize=True)`——**2026 年微调的默认姿势**（与推理时完全一致的数据形态）；
- **拼接格式**：手拼 `"### 指令\n...\n### 回答\n..."`——老教程常见，**不推荐**（与推理时的 chat 模板不一致，训练/推理数据分布漂移）。

**数据质量是效果上限**：LoRA 只是放大数据质量的手段——**几条高质量样本胜过几百条低质样本**（指令数据构建与清洗看 `../../../../09-模型微调与多模态/05-指令数据构建与清洗.md`）。训练前必做三件事：**去重、长度检查（max_length 截断）、人工抽查 10 条**。

**tokenize 细节三件套**：`padding_side`（生成模型用 right——chat 模板默认；**left padding 会污染生成**）、`max_length`（截断——超长样本是显存杀手，按数据分布设 512-2048）、`labels`（**CausalLM 训练自动把 labels 对齐到 input_ids**——被 padding/截断的部分不参与 loss；手写数据时确认 `ignore_index=-100` 的屏蔽行为）。

## 4. 显存账本

| 场景（7B 模型量级） | 底座驻留 | 优化器/梯度 | 合计 |
|------|---------|------------|------|
| 全量微调 | 14GB（fp16） | 28GB+ | 42GB+ |
| 纯 LoRA | 14GB | < 2GB | 16GB+ |
| QLoRA（4bit） | 4GB | < 2GB | 7GB+ |

**账本逻辑**：LoRA 省的是"优化器 + 梯度"（只算可训练参数），QLoRA 再省"底座驻留"（4bit 压缩）——**两级省法正交，叠加就是单卡微调大模型的事实标准**。训练中还吃显存的：激活值（`gradient_checkpointing` 以算换存）、日志与评估（`eval_strategy` 频率压低）。**实测方法：`torch.cuda.max_memory_allocated()` 训练后打印峰值**，账本数字按你的模型重新量。

**账本怎么算出来的**（知其所以然）：训练显存 ≈ **模型权重 + 梯度 + 优化器状态 + 激活值**——全量微调时梯度与优化器按全部参数算（每个参数约 6 字节：fp16 梯度 2B + Adam m/v 各 4B），**LoRA 只按可训练参数算这一项**（0.5% 参数 ≈ 忽略不计）；激活值与 batch、序列长度、层数相关——`gradient_checkpointing` 用"重算前向"换掉激活存储。**七亿模型上全量 vs LoRA 的差距，数学上就来自那"6 字节 × 参数总量"**——这就是"省显存"的来源。

## 5. 保存、恢复与推送

**保存**：`peft_model.save_pretrained("./lora-out")` 只存 adapter；`tokenizer.save_pretrained("./lora-out")` 存分词器——**部署目录 = adapter + tokenizer + 底座型号声明**（README 写明 base model 与底座版本）。

**恢复**：`PeftModel.from_pretrained(base_model, "./lora-out")` 后可直接继续训练（Trainer 传入包装模型即可）——**adapter 恢复训练是 PEFT 的天然优势**：几十 MB 加载即继续，无需全量 checkpoint。

**推送 Hub**：登录后 `peft_model.push_to_hub("your-name/your-lora")`——**一个 API 完成发布**；Hub 上的 adapter 目录与本地完全同构，`PeftModel.from_pretrained("your-name/your-lora")` 全球可加载（09 篇发布规范）。

## 6. 评估与迭代

LoRA 训练的传统评估坑：**训练 loss 低 ≠ 任务效果好**——指令微调必须做生成级评估：**抽样 20-50 条验证样本，人工对比"微调前 vs 微调后"的输出**（同一 prompt 跑底座与 adapter）。迭代节奏：**每轮跑一次抽样评估，效果停滞就回到 04 篇的旋钮（r/alpha/数据）**——"微调是循环：数据 → 训练 → 评估 → 调整"（评估与迭代完整方法论见微调体系 06 篇）。

## 7. 完整训练模板

```python
from transformers import (AutoModelForCausalLM, AutoTokenizer, Trainer, TrainingArguments)
from peft import LoraConfig, get_peft_model
from datasets import load_dataset

base_id = "Qwen/Qwen2.5-1.5B"
tokenizer = AutoTokenizer.from_pretrained(base_id, padding_side="right")
tokenizer.pad_token = tokenizer.eos_token

model = AutoModelForCausalLM.from_pretrained(base_id, torch_dtype="auto")
model = get_peft_model(model, LoraConfig(
    r=16, lora_alpha=32, target_modules=["q_proj","k_proj","v_proj","o_proj"],
    lora_dropout=0.1, finetuning_type="lora",
))

ds = load_dataset("databricks/databricks-dolly-15k", split="train[:1000]")
def fmt(x):
    msgs = [{"role":"user","content":x["instruction"]},
            {"role":"assistant","content":x["response"]}]
    return tokenizer.apply_chat_template(msgs, tokenize=True,
                                         truncation=True, max_length=1024)
ds = ds.map(lambda x: {"input_ids": fmt(x)}, remove_columns=ds.column_names)

trainer = Trainer(model=model, args=TrainingArguments(
    output_dir="./dolly-lora", per_device_train_batch_size=2,
    gradient_accumulation_steps=8, learning_rate=2e-4,
    num_train_epochs=2, bf16=True, gradient_checkpointing=True,
    save_strategy="epoch", save_only_model=True, logging_steps=10,
), train_dataset=ds)
trainer.train()
model.save_pretrained("./dolly-lora")
tokenizer.save_pretrained("./dolly-lora")
```

## 8. 五个常见坑

- **坑一**：lr 用全量微调的 5e-5——**LoRA 用 1e-4~5e-4**，太小 loss 掉得极慢；
- **坑二**：pad_token 缺失直接报错——**先 `tokenizer.pad_token = tokenizer.eos_token`**（chat 模板尤其）；
- **坑三**：训练/推理数据格式不一致（训练手拼、推理走 chat 模板）——**统一用 apply_chat_template**；
- **坑四**：save_only_model 忘了开，adapter 目录塞进几百 MB 优化器——**adapter 场景默认开**；
- **坑五**：评估只看 loss——**生成任务必须抽样人工对比输出**，loss 低不代表任务做对了。
- **坑六**：数据集 map 后忘记 remove_columns——原始列混进 batch，collator 报未知字段；**`remove_columns=ds.column_names` 一步清干净**。

## 9. 练习 5 题

1. 包装后模型哪些开销被省掉？为什么说"前向开销不变"？
2. LoRA 训练的学习率惯例？为什么比全量微调大？
3. 训练数据格式的 2026 默认姿势是什么？为什么别手拼？
4. 显存账本的三级结构（全量/LoRA/QLoRA）？各省了哪一块？
5. 恢复训练为什么天然方便？评估为什么不能只看 loss？

> 🎯 **核心要点**：PEFT 与 Trainer 是零摩擦集成——**包装即全部差异，循环仍是 transformers 的活**；LoRA 专属设置只有三处：**学习率 2e-4、batch 放大、save_only_model**；"数据质量决定上限，LoRA 决定下限"。

---

**下一模块**：[07-推理与适配器加载.md](07-推理与适配器加载.md) / **返回总览**：[00-PEFT总览.md](00-PEFT总览.md)
