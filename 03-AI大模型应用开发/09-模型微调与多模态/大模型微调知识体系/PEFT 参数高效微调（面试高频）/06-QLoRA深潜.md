# QLoRA 深潜

> 单卡微调的事实标准：4bit NF4 量化 + 双量化 + LoRA 适配器——原理三件套、为什么"4bit 存储 + bf16 计算"不损效果、单卡量级与稳定性

## 1. QLoRA 是什么

**QLoRA（Quantized LoRA）**：在 LoRA 之上增加一层——**底座权重 4bit 量化存储，LoRA 适配器保持原精度（bf16）训练**——解决"LoRA 虽省但权重还是占大头"的问题（`../训练关键超参 & 显存优化技术/06-显存账本与优化技术.md` 的账本：权重是最大项，量化省权重）。

三个组件（QLoRA 论文，2023）：

**NF4 量化（NormalFloat4）**：4bit 的"信息最优"格式——针对正态分布权重设计的分位数量化（比均匀 4bit 更贴合权重分布）——4bit 存储精度损失最小的方案。

**双量化（Double Quantization）**：量化参数的**量化常数再量化**——每参数再省 ~0.5bit（量化常数本身从 fp32 压到 8bit）——合计约 4.5bit/参数。

**Paged Optimizer（分页优化器）**：优化器状态用 CUDA 统一内存分页管理——显存溢出时自动换页到 CPU 而不是 OOM——长序列/大 batch 场景的稳定性保障。

## 2. 为什么"4bit 存储 + bf16 计算"不损效果

QLoRA 的关键设计：**量化只作用于存储，计算用反量化后的 bf16**——

- **存储精度**：权重以 4bit 存（省显存）；**计算精度**：前向/反向时反量化回 bf16 计算（保梯度质量）——"低精度存、高精度算"分离，训练质量接近 bf16 底座（`../训练关键超参 & 显存优化技术/07-量化微调技术.md` 01 节的稳定性三要点在此展开）。
- **适配器不受量化影响**：LoRA 的 A/B 以 bf16 全精度训练——微调要"学"的部分无精度损失；量化只影响"被冻结的底座"——**这正是 QLoRA 效果接近全量的机制解释**（`../训练关键超参 & 显存优化技术/07-量化微调技术.md` 01 节：训练期量化是"几乎免费的显存杠杆"）。

**实测效果**：QLoRA 效果接近 LoRA/全量（Baseten 2026：LoRA 恢复 98%，QLoRA 在其上再省显存）；训练速度比 LoRA 慢 **30-40%**（反量化开销）——**QLoRA 是"用速度换显存"**，显存够时 LoRA（bf16）更快。

**量化误差的"存储-计算"分配**：QLoRA 的误差来源分两层——**存储量化误差**（4bit 表示权重的精度损失，影响底座表达精度）与**反量化计算误差**（bf16 反量化后计算，误差可忽略）——**误差主体在存储层**；这正是 NF4（信息最优 4bit）与双量化（再省 0.5bit）存在的意义：把存储层的误差压到最小——**"量化误差在哪一层"是理解 QLoRA 设计的钥匙**（`../训练关键超参 & 显存优化技术/07-量化微调技术.md` 的量化损伤论述在此衔接）。

## 3. 单卡量级

QLoRA 的单卡能力（2026 实测参考）：

| 模型 | 显存 | 卡 |
|------|:---:|------|
| 7B | ~6GB | RTX 3080 级 |
| 13B | ~18GB | RTX 4090/5090 |
| 33B 级 | ~24-32GB | 40GB 级卡 |
| 65B | 单卡 48GB 可训 | A6000 级 |

**为什么量级这么小**：4bit 权重（0.5 字节/参数）——7B ≈ 3.5GB 权重 + 适配器状态 + 激活值——**QLoRA 让"单卡训大模型"从例外变成常规**（`../训练关键超参 & 显存优化技术/07-量化微调技术.md` 01 节的 ~4.5bit/参数账本）。

## 4. 稳定性三要点

QLoRA 训练稳定性的三个配置要点（`../训练关键超参 & 显存优化技术/07-量化微调技术.md` 01 节）：

**compute_dtype 用 bf16**：计算精度不被量化拖累——fp16 在 QLoRA 下更易溢出（NaN 排查先查这里，`../微调常见问题/04-训练问题loss不降NaN与发散.md` 02 节）。

**非量化层转 fp32**：LayerNorm、lm_head 输出保持 fp32——防止精度丢失（量化只作用于线性层权重）。

**FSDP 场景 bnb_4bit_quant_storage="bf16"**：4bit 层与常规层同形分片——否则 FSDP 分片失败（`../训练关键超参 & 显存优化技术/08-分布式训练与并行策略.md` 的 QLoRA+FSDP 组合前提）。

## 5. 2026 的 QLoRA 生态

**QLoRA + DoRA**：2026 默认组合（`../PEFT 参数高效微调（面试高频）/03-LoRA变体家族.md` 02 节）——4bit 底座 + DoRA 适配器（表达力更接近全量）；**QLoRA + FSDP**：70B 级多卡微调标准组合；**NVFP4 的挑战**：NVIDIA FP4 量化（2026 新兴）让 MoE 大模型单卡训练成为可能——但 QLoRA NF4 仍是通用默认（`../训练关键超参 & 显存优化技术/07-量化微调技术.md` 02 节）。

**QLoRA 的适用边界**：**数据量小（<1k）时量化噪声占比大**——小数据优先 FP8/BF16（`../训练关键超参 & 显存优化技术/07-量化微调技术.md` 06 节）；结构化输出/工具调用任务对量化更敏感——量化后必跑格式专项评估。

**QLoRA 与 NF4 量化底座的前提**：4bit 底座需要 **bitsandbytes 库 + Ampere 及以上架构**（RTX 30 系+）——老卡（V100 及以下）不支持 bf16/4bit 训练（`../微调常见问题/09-框架与工具问题.md` 02 节的 bitsandbytes 兼容矩阵）；**检查硬件再承诺 QLoRA 方案**是环境搭建的第一课（`../完整微调工程流程/04-环境搭建与工具链选型.md` 04 节）——"QLoRA 单卡训 13B"的前提是"卡支持量化内核"。

**QLoRA 的 DPO 扩展**：2026 标准配方"QLoRA SFT → DPO"中，**DPO 阶段也可以用 QLoRA**（量化底座 + 偏好对训练，`../基础概念/07-微调方法全景图谱.md` 07 节）——显存预算一致（同一张卡跑完两阶段）；注意 **DPO 的参考模型（ref model）内存开销**：DPOTrainer 配 `ref_model=None` 省一半显存（`../完整微调工程流程/04-环境搭建与工具链选型.md` 02 节的组合模式）——**单卡跑完整条对齐流水线**是 QLoRA 时代的新常态。

**QLoRA 的输出质量保障**：量化底座 + 适配器的输出质量检查——**合并后用 float16 底座的服务质量与训练期一致**（训练期量化只影响训练，部署期合并回 float16 后量化痕迹消失——**"训练量化、部署全精度"是 QLoRA 路线的标准组合**，与"部署期再量化"（07 篇）是两回事——QLoRA 路线天然绕开了"train-serve 精度错配"问题（`../微调常见问题/07-效果不达预期问题.md` 02 节）。

## 6. QLoRA 面试必答要点

面试"QLoRA 原理"的满分结构：**存储与计算分离**（4bit 存、bf16 算——量化只影响冻结底座、适配器全精度）→ **NF4 是信息最优 4bit**（分位数量化贴合权重分布）→ **双量化再省 0.5bit/参数** → **Paged optimizer 防 OOM**。

**效果接近全量**（98% 实证 + 适配器不受量化影响的机制）→ **代价是慢 30-40%**（速度换显存）——六点齐备 = 原理 + 机制 + 实证 + 权衡全答。

> 🎯 **核心要点**：QLoRA = 4bit 量化底座 + LoRA 适配器；三组件——NF4（信息最优 4bit）、双量化（再省 ~0.5bit/参数，合计 ~4.5bit）、Paged optimizer（防 OOM）；效果不损的机制——存储 4bit/计算 bf16 分离、适配器全精度不受量化影响（98% 实证的机制解释）；代价是训练慢 30-40%（速度换显存）；单卡量级——7B ~6GB、13B ~18GB、65B 单卡 48GB；稳定性三要点（bf16 compute/非量化层 fp32/FSDP storage bf16）；2026 组合——QLoRA+DoRA 默认、QLoRA+FSDP 训 70B、NVFP4 是新兴挑战者；面试六点结构。

---

**参考来源**：

- [LLM Fine-Tuning 2026 Deep Dive（youngju.dev）](https://www.youngju.dev/blog/culture/2026-05-16-llm-fine-tuning-2026-lora-qlora-dora-galore-unsloth-axolotl-trl-peft-mlx-lm-deep-dive.en)
- [How Does LoRA Fine-Tuning Work? Adapters, QLoRA, DoRA（Mixpeek）](https://mixpeek.com/guides/fine-tuning-with-lora-adapters)
- [CTR-LoRA 引用的 QLoRA 论述（IEEE 2026）](https://ieeexplore.ieee.org/document/11463994/references)
- [Implementation: Huggingface Peft Prepare Model For Kbit Training](https://leeroopedia.com/index.php/?title=Implementation:Huggingface_Peft_Prepare_Model_For_Kbit_Training&oldid=13904)

---

**下一模块**：[07-PEFT显存与性能.md](./07-PEFT显存与性能.md) / **返回总览**：[00-PEFT参数高效微调总览](./00-PEFT参数高效微调总览.md)
