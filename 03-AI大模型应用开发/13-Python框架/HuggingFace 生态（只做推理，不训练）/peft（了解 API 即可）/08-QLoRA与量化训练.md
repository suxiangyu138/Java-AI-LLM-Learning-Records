# 08 - QLoRA 与量化训练

> 定位：单卡微调大模型的标准姿势——4bit 量化底座 + LoRA——"全量微调 70B 要 42GB+ 显存，QLoRA 把它压到 10GB 级别——2026 年单卡微调的事实标准，三件套 + 一份账本"

---

## 📚 目录

1. [QLoRA 是什么：三件套](#1-qlora-是什么三件套)
2. [bitsandbytes：4bit 引擎](#2-bitsandbytes4bit-引擎)
3. [完整配置示例](#3-完整配置示例)
4. [显存账本与实测方法](#4-显存账本与实测方法)
5. [训练质量与速度的代价](#5-训练质量与速度的代价)
6. [2026 状态：版本与生态](#6-2026-状态版本与生态)
7. [五个常见坑](#7-五个常见坑)
8. [练习 5 题](#8-练习-5-题)

---

## 1. QLoRA 是什么：三件套

**QLoRA（Quantized LoRA）= 4bit 量化底座 + LoRA 微调**，论文关键贡献不止是"4bit"，而是三件套：**4bit NF4 量化底座**（NormalFloat 4bit：按权重分布优化的信息论最优量化，比普通 int4 精度高）；**双重量化**（Double Quantization：把量化缩放因子本身再量化一次，再省约 0.4 比特/参数）；**分页优化器**（Paged Optimizer：优化器状态溢出时换页到 CPU 内存，防 OOM）。加上**梯度 checkpointing**（以算换存）与 **flash attention**，共同构成单卡微调大模型的完整配方。**量化细节是了解级，API 是会用级**：底座加载交给 transformers + bitsandbytes，LoRA 注入交给 PEFT——两者自动协作。

**NF4 与 FP4 的对比**（配置时二选一）：NF4（NormalFloat）**按权重分布做信息论最优量化**——对正态分布权重误差更小，**QLoRA 论文与社区实践都默认 NF4**；FP4 是均匀浮点量化，实现更简单但精度略差——**配置里写 `bnb_4bit_quant_type="nf4"` 就不用再想**。还有一个常被忽略的点：**4bit 只是"驻留"格式**——权重进计算时先反量化成 `bnb_4bit_compute_dtype` 指定的 bf16——**"存 4bit、算 bf16"是省显存不毁精度的关键机制**（量化原理本身看 `../../../../07-模型部署与工程化/模型推理与部署/05-模型量化推理实战.md`）。

## 2. bitsandbytes：4bit 引擎

4bit 底座由 **bitsandbytes** 提供（transformers 的 `BitsAndBytesConfig`）：

```python
from transformers import BitsAndBytesConfig

bnb_config = BitsAndBytesConfig(
    load_in_4bit=True,
    bnb_4bit_quant_type="nf4",        # nf4（推荐）或 fp4
    bnb_4bit_compute_dtype="bf16",    # 反量化后计算的精度：LoRA 分支保持 bf16
    bnb_4bit_use_double_quant=True,   # 双重量化
)

model = AutoModelForCausalLM.from_pretrained(
    "Qwen/Qwen2.5-7B", quantization_config=bnb_config, device_map="auto",
)
lora_config = LoraConfig(r=16, lora_alpha=32,
    target_modules=["q_proj","k_proj","v_proj","o_proj"])
peft_model = get_peft_model(model, lora_config)   # PEFT 自动识别量化底座并注入
```

**关键点**：`bnb_4bit_compute_dtype` 决定 LoRA 分支的训练精度（bf16）——**底座是 4bit 存储、bf16 计算，LoRA 分支全程 bf16**，这是"量化省显存但不毁训练精度"的机制核心；`device_map="auto"` 让 accelerate 自动分配层到 GPU/CPU。**PEFT 侧零额外配置**——`get_peft_model` 检测到量化底座后自动处理（注入层保持 bf16、冻结层保持量化）。

**逐行解读配置**：`load_in_4bit=True` 开启 4bit 加载（transformers 转发给 bitsandbytes）；`bnb_4bit_quant_type` 选量化算法（nf4）；`bnb_4bit_compute_dtype` 设计算精度（**与 TrainingArguments 的 bf16 一致**——不一致是常见坑）；`bnb_4bit_use_double_quant` 开双重量化（默认开，省约 0.4 比特/参数）；`device_map="auto"` 让 accelerate 分配层——**这套配置是主流底座 + 单卡的通吃模板**。

## 3. 完整配置示例

```python
from transformers import (AutoModelForCausalLM, AutoTokenizer, Trainer,
                          TrainingArguments, BitsAndBytesConfig)
from peft import LoraConfig, get_peft_model

base_id = "Qwen/Qwen2.5-7B"
tokenizer = AutoTokenizer.from_pretrained(base_id, padding_side="right")
tokenizer.pad_token = tokenizer.eos_token

model = AutoModelForCausalLM.from_pretrained(base_id,
    quantization_config=BitsAndBytesConfig(
        load_in_4bit=True, bnb_4bit_quant_type="nf4",
        bnb_4bit_compute_dtype="bf16", bnb_4bit_use_double_quant=True),
    device_map="auto",
)
model = get_peft_model(model, LoraConfig(
    r=16, lora_alpha=32, target_modules=["q_proj","k_proj","v_proj","o_proj"],
    lora_dropout=0.1,
))

trainer = Trainer(model=model, args=TrainingArguments(
    output_dir="./qlora-out", per_device_train_batch_size=1,
    gradient_accumulation_steps=16,    # batch=1 + 累积：量化底座显存有限
    learning_rate=2e-4, num_train_epochs=1,
    bf16=True,                         # 注意：compute_dtype=bf16 与之呼应
    gradient_checkpointing=True,       # 必开：以算换存
    save_strategy="epoch", save_only_model=True, logging_steps=10,
), train_dataset=ds)
trainer.train()
model.save_pretrained("./qlora-out")   # 同样只存 adapter
```

**与纯 LoRA 的差异只有三处**：加载时带 `quantization_config`、`device_map="auto"`、**batch 通常要更小**（量化底座反量化有额外显存占用）——其余 API 完全不变。

**还有一个实务细节**：量化底座 + 梯度 checkpointing 报兼容错误时，检查 `gradient_checkpointing_kwargs={"use_reentrant": False}`——新版本默认值变更后与量化层有已知兼容问题，**报错时先查这个参数**；8GB 显存的极限配置：4bit + batch=1 + checkpointing + CPU offload——**慢但能跑，是低配学习机的最后选项**。

## 4. 显存账本与实测方法

**账本（7B 量级示例，实际按模型重算）**：

| 配置 | 底座驻留 | 优化器+梯度 | 激活值 | 合计量级 |
|------|---------|------------|--------|---------|
| 全量微调 | 14GB（fp16） | 28GB | 高 | 42GB+ |
| 纯 LoRA | 14GB | <2GB | 中 | 16GB+ |
| QLoRA | ~4GB（4bit） | <2GB | 低（batch=1） | 7-10GB |

**70B 量级**：全量微调 42GB+（多卡）；纯 LoRA 20GB 级（fp16 底座 140GB 显然不行——纯 LoRA 大模型也要量化）；**QLoRA 10GB 级，单卡 24GB 可训 70B**（Galore-Plus 48GB 单卡 70B 是另一条路，05 篇）。

**实测方法**：训练循环后打印峰值——`trainer.callback_handler.callbacks` 里挂 `max_memory_allocated` 打印，或训练外 `torch.cuda.reset_peak_memory_stats()` + `torch.cuda.max_memory_allocated()`——**账本是测出来的，不是背出来的**。

**显卡选型参考**（量级认知，按实际模型验证）：24GB（4090）——QLoRA 训 7B-13B 舒适；40GB（A100）——QLoRA 训 13B-34B；48GB（RTX 6000 Ada）——QLoRA 训 70B 或 Galore-Plus 70B（05 篇）；80GB（H100/A100）——纯 LoRA 训 13B+ 甚至全量微调起点。**判断公式**：`底座参数量 × 驻留字节`（4bit 约 0.5GB/10 亿参数）+ `可训练参数 × 6 字节` + 激活值——**先算后跑，别让 OOM 告诉你**；不同框架版本会有 ±20% 浮动，实测为准。

## 5. 训练质量与速度的代价

**速度**：4bit 底座每次前向都要**反量化 → 计算 → 再量化**，训练速度慢 **30-40%**——预算与显存的交易；**batch=1 + 梯度累积**进一步拉慢 wall-clock，但等效 batch 不变。

**质量**：量化会丢失底座权重精度，**任务效果通常比纯 LoRA 略降**（低数据量任务差距更明显）；补偿手段：`init_lora_weights="loftq"`（用 4bit 量化误差初始化 LoRA 分支，补回部分精度）、`bnb_4bit_quant_type="nf4"`（优于 fp4）、数据质量优先。

**选型主线**：**显存够（24GB+ 训 7B）用纯 LoRA；显存紧/模型大（7B+ 或单卡）上 QLoRA**——"量化是显存换速度的不得已，不是免费的午餐"。

**QLoRA 微调后的必做实验**：同一任务分别用纯 LoRA 与 QLoRA 微调（同数据同超参），**对比验证集效果与训练耗时**——"量化到底损失多少"不该靠传闻，**一次对照实验是生产决策的依据**；若 QLoRA 明显劣化，先试 `init_lora_weights="loftq"`（04 篇）再考虑升级显存。

## 6. 2026 状态：版本与生态

- **bitsandbytes**：transformers v5 要求 **≥ 0.46.1**；4bit 加载是 transformers 的 `BitsAndBytesConfig`，PEFT 透明协作；
- **gptqmodel 取代 AutoGPTQ/AutoAWQ**：PEFT v0.19 全面弃用 AutoGPTQ/AutoAWQ（它们对 GPT-QModel 的适配被移除）——**4bit 家族里 NF4 走 bitsandbytes，GPTQ 量化走 gptqmodel 库**；老教程的 `from peft import prepare_model_for_kbit_training` 等旧 API 在新版已收敛（PEFT 自动处理量化底座，无需手动准备）；
- **多卡 QLoRA**：`device_map="auto"` + FSDP/DeepSpeed 仍兼容（adapter 参数少，通信开销可接受）——**单卡起步，多卡只是加速**；
- **GPTQ 底座的微调**（了解）：除 NF4 外，社区还有 GPTQ 量化底座（gptqmodel 库加载）——PEFT 同样支持在其上挂 LoRA（v0.19 起依赖 gptqmodel 而非已弃用的 AutoGPTQ/AutoAWQ）——**"量化底座的选择（NF4/GPTQ）影响加载与精度，不影响 LoRA 写法"**。

## 7. 五个常见坑

- **坑一**：没装 bitsandbytes 直接 ImportError——`pip install bitsandbytes`（Windows 新版已支持 CUDA）；
- **坑二**：`bnb_4bit_compute_dtype` 与 TrainingArguments 的精度不一致（如 compute_dtype=fp16 而训练 bf16）——**两处统一**；
- **坑三**：batch=8 起步直接 OOM——**量化底座先 batch=1 验证，再逐步放大**；
- **坑四**：QLoRA 训完的 adapter 加载到 fp16 底座——精度劣化/报错，**按训练时的量化配置加载**（07 篇一致性铁律）；
- **坑五**：CPU offload 场景（显存真的不够）——`device_map="auto"` 自动 offload，**训练极慢是正常现象，先缩小模型/数据再谈**。

## 8. 练习 5 题

1. QLoRA 三件套是哪三件？各解决什么问题？
2. bnb_4bit_compute_dtype 的作用？为什么 LoRA 分支能保持 bf16？
3. QLoRA 与纯 LoRA 的代码差异只有哪三处？
4. 7B/70B 量级的显存账本？峰值显存怎么实测？
5. QLoRA 的两个代价是什么？loftq 初始化解决什么？

> 🎯 **核心要点**：QLoRA = **4bit 量化底座（NF4 + 双重量化 + 分页优化器）+ LoRA + 梯度 checkpointing**——单卡微调大模型的事实标准；**"显存够用纯 LoRA，显存紧/模型大上 QLoRA——量化是显存换速度的不得已"**。

---

**下一模块**：[09-适配器管理与生产实践.md](09-适配器管理与生产实践.md) / **返回总览**：[00-PEFT总览.md](00-PEFT总览.md)
