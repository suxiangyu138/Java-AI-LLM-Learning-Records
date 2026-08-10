# 05 - QLoRA 微调

> 本体系第五课：QLoRA——bitsandbytes 的封神之作——"4bit 底座 + LoRA 适配器 = 单卡微调大模型——24GB 显卡微调 7B/13B 的标配"

---

## 📚 目录

1. [QLoRA 是什么](#1-qlora-是什么)
2. [一句话原理](#2-一句话原理)
3. [怎么用：完整流程](#3-怎么用完整流程)
4. [显存账本：省了多少](#4-显存账本省了多少)
5. [QLoRA 的边界](#5-qlora-的边界)
6. [练习 5 题](#6-练习-5-题)
7. [本节验收](#7-本节验收)

---

## 1. QLoRA 是什么

**QLoRA（Quantized LoRA）= 4bit 量化底座 + LoRA 适配器的微调方案**——2024 年提出、2026 年仍是单卡微调的事实标准：

```text
QLoRA 解决什么问题
├── 普通微调：7B 模型全量微调要 56GB+ 显存（推理 14GB × 4 左右）——消费卡没戏
├── LoRA（2019）：只训练小适配器（1% 参数）——省很多，但底座还是全精度
├── QLoRA（2024）：底座也量化成 4bit——LoRA 的省 × 量化的省 = 双省
└── 结果：24GB 显卡微调 7B（原需 56GB+）——"消费级显卡微调大模型的标配"
    ——"QLoRA = LoRA（只训适配器）× 量化（底座 4bit）——两个省显存方案相乘"
```

**定位心智**：**"QLoRA 的记忆锚点：'适配器全精度 + 底座 4bit 冻结'"**——"**底座（97%+ 参数）4bit 冻结不动，LoRA 适配器（1% 参数）全精度训练——'冻结的省显存 + 训练的质量'"**（"为什么能训练：只有适配器有梯度——量化底座不参与反向传播——**'bitsandbytes 是唯一'量化后还能训练'的方案——这是它的独门绝技'"**）；**QLoRA vs LoRA**——"LoRA：底座全精度（省得有限）；QLoRA：底座 4bit（省 3/4 还多）——**'QLoRA = LoRA 的省显存加强版——结果质量接近（损失 1-2%）'"**（"精度对比：QLoRA 微调 vs 全量微调——差距很小（指令跟随任务）——**'消费卡上 QLoRA 是'最接近全量微调'的方案'"**）；**2026 地位**——"QLoRA 仍是单卡微调主流（HuggingFace 对齐手册标配 bitsandbytes ≥0.46.1）——**'Lora 生态（peft）成熟 + 4bit 质量够 = QLoRA 统治消费级微调'"**（本体系 06 篇的 PEFT 集成）。

## 2. 一句话原理

**QLoRA 的原理——三行话讲完**：

```text
QLoRA 三句话原理
├── ① 底座 4bit：模型权重用 NF4 量化 + 冻结（省 3/4 显存——不参与训练）
├── ② 适配器训练：LoRA 小矩阵（原参数 1% 左右）全精度训练（可学习的部分）
└── ③ 前向反量化：计算时底座临时反量化回 bf16（用 4bit 存、用 bf16 算）
    ——"一句话：4bit 存（省显存）+ bf16 算（保质量）+ 适配器训（能训练）"
```

**原理心智**：**"QLoRA 的'存算分离'：存储用 4bit（省）、计算用 bf16（准）"**——"**前向时反量化回 bf16 计算——'4bit 是衣柜，bf16 是出门穿的衣服'"**（"理解'存算分离' = 理解 QLoRA 为什么又省又准"）；**三行话对应的工程**——"① 配置四件套（04 篇——底座 4bit）；② PEFT 的 LoRA 配置（r=16 等——适配器）；③ 前向自动处理（框架内部做——不用管）——**'你要写的只有 ①②——③ 是 bitsandbytes 的活'"**（06 篇完整代码）；**QLoRA 的延伸概念**——"**Q**LoRA 的 Q = Quantized（量化）——'Q 就是 bitsandbytes 的戏份'"（"LoRA 体系（peft）讲适配器，本体系讲'Q'——分工清晰"）。

## 3. 怎么用：完整流程

**QLoRA 微调完整流程（Transformers + PEFT——五步）**：

```python
# ① 4bit 底座配置（04 篇四件套）
from transformers import BitsAndBytesConfig, AutoModelForCausalLM, AutoTokenizer
import torch

bnb_config = BitsAndBytesConfig(
    load_in_4bit=True,
    bnb_4bit_quant_type="nf4",
    bnb_4bit_compute_dtype=torch.bfloat16,
    bnb_4bit_use_double_quant=True,
)

model = AutoModelForCausalLM.from_pretrained(
    "Qwen/Qwen2.5-7B", quantization_config=bnb_config, device_map="auto")
tokenizer = AutoTokenizer.from_pretrained("Qwen/Qwen2.5-7B")

# ② LoRA 适配器（peft——只训这部分）
from peft import LoraConfig, get_peft_model

lora_config = LoraConfig(
    r=16, lora_alpha=32,           # 适配器秩（r=16 是常用起点）
    lora_dropout=0.05,
    target_modules=["q_proj", "k_proj", "v_proj", "o_proj"],
)
model = get_peft_model(model, lora_config)     # 包上适配器（底座冻结）

# ③ 训练（Trainer——HuggingFace 标准——数据预处理略）
from transformers import Trainer, TrainingArguments

args = TrainingArguments(
    output_dir="qlora-out",
    per_device_train_batch_size=1,   # 显存紧张——batch 小
    gradient_accumulation_steps=8,   # 累积等效 batch=8
    learning_rate=2e-4, max_steps=1000,
    bf16=True,                       # 训练精度 bf16（配计算精度）
)
trainer = Trainer(model=model, args=args, train_dataset=dataset)
trainer.train()

# ④ 保存与合并（适配器小文件——底座不动）
model.save_pretrained("qlora-out/adapter")    # 只有适配器（几十 MB）
# 合并回底座（部署用）：model = model.merge_and_unload()——可选
```

**流程心智**：**"五步的骨架：4bit 底座 → LoRA 配置 → Trainer 训练 → 保存适配器"**——**"你要写的就是'配置 + 数据'——训练循环 Trainer 全包——'QLoRA 的门槛 = 配置的准确性，不是训练的理解'"**（"抄标准配置 + 调 batch 大小——**'QLoRA 微调 90% 是配置活，10% 是数据活'"**）；**训练细节三件套**——"**batch 小（显存紧——1）+ 梯度累积（等效大 batch）+ bf16 训练（配计算精度）**"——（"显存不够就把 batch 减半、累积加倍——**'batch 和累积的换算 = 微调显存的第一调节旋钮'"**）；**保存与合并**——"适配器小（几十 MB——方便存/分享）；合并（merge）回底座 = 部署用（推理更顺）——**'微调产出 = 适配器（轻）+ 可选合并（重）'"**（"HuggingFace 体系讲的'适配器即产品'——本体系确认了它的存储形态"）。

## 4. 显存账本：省了多少

**QLoRA 的显存账本——对比三种方案**：

```text
7B 模型微调显存对比（约数）
├── 全量微调：56GB+（权重 + 梯度 + 优化器状态）——消费卡没戏
├── LoRA（全精度底座）：28GB+（底座 14GB + 训练开销）——24GB 卡勉强/悬
└── QLoRA（4bit 底座）：约 8-12GB（底座 3.5GB + 适配器 + 训练开销）
    ——"QLoRA = 全量微调显存的 1/5 左右——24GB 卡微调 7B 舒适区"
```

**账本心智**：**"QLoRA 省显存的三个来源：底座 4bit（省 3/4）+ 只训适配器（省梯度）+ 适配器小（省优化器状态）"**——**"省的是'三份钱'：权重（4bit）、梯度（只适配器有）、优化器（只适配器有）——'全量微调的三份钱 QLoRA 只付一份'"**（02 篇"微调 ×3"的破解——"QLoRA 把'×3'打回'×1.5 左右'"）；**账本的实战判断**——"8GB 卡：微调 3B 以下模型（QLoRA）；12GB：7B（QLoRA 舒适）；24GB：13B——**'先算账再开跑——显存不够就把模型换小/适配器秩调小（r=8）'"**（"秩 r 也影响显存——r=16 → r=8 省一点——**'显存的最后调节旋钮：r'"**）；**账本的例外**——"长上下文/大批次会额外吃显存（KV 缓存/激活）——**'账本是下限，运行还要留余量——OOM 时先减 batch'"**（08 篇 OOM 排障）。

## 5. QLoRA 的边界

**QLoRA 的边界——知道什么情况不合适**：

```text
QLoRA 边界（三不适合）
├── ① 效果敏感任务：量化底座损失 1-2%——顶尖效果竞赛/关键业务要全量/高精度底座
│    —— "消费卡上 QLoRA 是'最接近全量'——不是'等于全量'"
├── ② 超大模型（70B+）：24GB 卡 QLoRA 70B 仍紧张（35GB 底座 + 训练开销）
│    —— 70B 微调要 48GB+ 或多卡
└── ③ 非 Transformer 架构/特殊算子：peft + bitsandbytes 支持面有边界
    ——"边界认知：QLoRA 是'消费级单卡微调'的答案——不是所有微调的答案"
```

**边界心智**：**"QLoRA 的定位总结：'消费级显卡微调的标准答案'——超出消费级（70B+/效果极限）就要升级方案"**——**"升级路径：QLoRA（24GB 单卡）→ 多卡 QLoRA/全量（48GB+ 多卡）→ 云端训练（A100/H100）——'方案跟着预算与效果要求走'"**；**QLoRA 与推理部署的关系**——"微调用 QLoRA → 部署用合并后的模型或重新量化（GPTQ）——**'微调与推理的量化方案可以不同——'微调产物重新量化再上线'"**（09 篇选型的微调侧结论——"训练用 NF4、推理用 GPTQ/AWQ/GGUF"）；**了解即可的边界观**——"记住'QLoRA = 消费级微调标配'——边界是'更大的模型/更高的效果要求'——**'边界之外的方案知道名字（多卡/全量/云端）就行'"**。

## 6. 练习 5 题

1. QLoRA 是什么？（4bit 底座 + LoRA 适配器）
2. 三句话原理？（4bit 存 + bf16 算 + 适配器训）
3. 五步流程？（底座 → LoRA → Trainer → 保存）
4. 7B 微调显存对比？（56GB vs 28GB vs 8-12GB）
5. QLoRA 的三边界？（效果敏感/超大模型/架构支持）

## 7. 本节验收

**验收动作**：① 用四件套 + LoRA 配好一个 QLoRA 训练配置（小模型/小数据）；② 跑 10 步训练（验证不 OOM）；③ 保存适配器并打印文件大小；④ 写显存账本对比表（三方案）——**"原理 + 流程 + 账本 + 边界 = QLoRA 能力"**——**练习纪律**：开跑前算账本——"QLoRA 的失败 80% 是显存不够——账本先行"。

> 🎯 **核心要点**：QLoRA = 4bit 底座 + LoRA 适配器（**LoRA 的省 × 量化的省 = 双省——24GB 微调 7B 标配**）；**三句话原理（4bit 存省显存 + bf16 算保质量 + 适配器训能训练——存算分离）**；**五步流程（四件套底座 → LoRA 配置 → Trainer → 保存适配器——batch 小 + 梯度累积 + bf16）**；**账本（全量 56GB vs QLoRA 8-12GB——省三份钱只付一份——r 是最后调节旋钮）**；边界（**消费级单卡是主场——70B+/效果极限升级多卡/云端**）——"QLoRA = 消费级显卡微调的标准答案——'最接近全量微调'的省钱方案"。

---

**上一模块**：[04-4bit量化.md](./04-4bit量化.md) / **下一模块**：[06-与Transformers-PEFT集成.md](./06-与Transformers-PEFT集成.md)
