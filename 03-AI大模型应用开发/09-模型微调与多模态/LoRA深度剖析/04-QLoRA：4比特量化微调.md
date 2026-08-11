# 04-QLoRA：4比特量化微调

> 定位：把 LoRA 的显存优势推到消费级显卡的最后一公里——底座量化到 4-bit，优化器状态只留给低秩适配器。QLoRA 是 2026 年个人开发者微调 7B 级模型的事实标准路径。

## 显存账本：省在哪里

7B 模型 fp16 全精度权重 14GB，仅权重就逼近 RTX 4090 的 24GB 预算；加上梯度（通常 4bit 分页）与 Adam 优化器状态（每个可训练参数 12 字节），LoRA 全精度也紧张。QLoRA 的拆解账目：

| 组成部分 | 全精度 LoRA | QLoRA |
|---|---|---|
| 底座权重 | 14GB (fp16) | ~4.5GB (4-bit NF4) |
| LoRA 适配器（r=16） | 可训练 ~0.2%，状态可忽略 | 同左 |
| 梯度 | 全模型 or 部分 | 4-bit 分页 |
| 7B 总量 | ~18-20GB | ~6-8GB（4090 轻松） |

省下的量级来自三件套：**NF4 量化底座**（14GB→4.5GB）、**双重量化**（再省 ~0.37bit/参数）、**分页优化器**（梯度与优化器状态溢出时换页到 CPU）。

## 量化精度的数值细节

NF4 的"近最优"不是宣传语。4-bit 只有 16 个离散取值，均匀量化（如 int4）把这 16 个值均匀铺开，而预训练权重的分布近似高斯——大量权重集中在 0 附近，均匀量化会在密集区浪费精度、稀疏区浪费取值。NF4 的做法：先对权重分布做分位归一化（把真实分位数映射到均匀区间），再等距量化，等价于让量化误差在分布上均匀分布，这是信息论意义上 4-bit 能到的最优精度。与之对照的 **FP4**（bnb_4bit_quant_type="fp4"）直接对指数/尾数按浮点规则截断，实现更简单但精度略逊，实测在多数任务上 NF4 全面占优。选择口诀：默认 NF4，只有遇到数值溢出类问题才尝试 FP4。量化粒度上，bnb 按 64 个权重一组共享一个量化常数（group size），组越小精度越高但量化常数开销越大——QLoRA 论文测过 32/64/128 三档，64 是质量与开销的平衡点。

## 分页优化器的机制细节

分页优化器是 QLoRA 里最"反直觉"的组件：它不提升精度、不降低计算量，纯粹为"让训练在显存不够时继续跑"而生。机制上，它把优化器状态（Adam 的 m、v 矩）放进以 4KB 页为单位的管理器，GPU 显存不足时按页换出到 CPU 内存，需要时换回——与操作系统虚拟内存的缺页换入同理（实现利用 NVIDIA 的统一内存 API）。关键行为差异：**换页有确定性开销**，训练速度会随换页频率波动；显存勉强够用时开分页反而因频繁换页变慢。工程判断标准：显存充足（余量 >10%）不开分页；显存紧张（贴着 OOM 线）开分页保命；显存极缺（8GB 卡跑 7B）分页 + 降 batch 双管齐下。注意分页只作用于优化器状态，不作用于激活值——激活值仍然必须常驻 GPU，batch 与序列长度仍是硬上限。

## 三个核心技术

**NF4（NormalFloat4）量化**。4-bit 量化精度低，常规均匀量化在权重分布两端的精度浪费严重。NF4 的做法是：对权重做分位归一化（quantile normalization），使量化区间匹配高斯分布的形状——信息论上接近 4-bit 的最优精度。相比早期 int4 方案，NF4 在同等显存下精度显著更高，是 QLoRA 论文的核心贡献之一。底座权重前向时实时反量化为 bf16 参与计算（反量化在 kernel 内完成，不在显存中展开）。

**双重量化**。量化本身需要存量化常数（每 64 个权重一组存一个标量），这个常数再量化一次（从 fp32 压到 fp8 存储），训练时再反量化回原值。看似绕一圈，实际省掉的是常数的 fp32 存储——对 7B 模型约 0.37 bit/参数的额外节省，训练内存下降约 0.7GB，精度损失可忽略。

**分页优化器（paged optimizers）**。利用 NVIDIA 统一内存技术，当 GPU 显存不足时，优化器状态（Adam 的 m、v 及梯度）自动以 4KB 页为单位换入换出 CPU 内存，像操作系统缺页一样避免 OOM。代价是换页时有性能抖动——这是 QLoRA 显存账的最后一块拼图，让它能在 8GB 显卡上训练 7B。

## 2026 工具链版本线

| 组件 | 2026-08 基准版本 | 说明 |
|---|---|---|
| bitsandbytes | 0.49.2（2026-02-16） | 4-bit 训练事实标准；0.46.1 为最低要求 |
| peft | 0.19.1（2026-04-16） | transformers 5.12.1+ 要求 ≥0.19.0 |
| transformers | 5.x（5.13 已验证 QLoRA 路径） | 4.56.2 为 TRL 新参数最低支持 |
| trl | SFTTrainer quantization_config（PR #6157，2026-06-24 合并） | 免手动预加载模型 |

**版本纪律**：bitsandbytes 与 torch 强绑定，跨版本升级必须重测；QLoRA 检查点建议在目录内记录 bnb 版本元数据——版本不匹配会静默加载失败或权重不兼容（2026 教程共识）。

## 最小 QLoRA 代码（2026 推荐形态）

```python
from transformers import BitsAndBytesConfig
from peft import LoraConfig
from trl import SFTTrainer

bnb_config = BitsAndBytesConfig(
    load_in_4bit=True,
    bnb_4bit_quant_type="nf4",        # 或 "fp4"
    bnb_4bit_use_double_quant=True,   # 双重量化
    bnb_4bit_quant_storage="bfloat16" # FSDP 分片量化层的关键
)
lora_config = LoraConfig(r=16, lora_alpha=16, target_modules=...,)

trainer = SFTTrainer(
    model="meta-llama/Llama-3.2-8B",
    peft_config=lora_config,
    quantization_config=bnb_config,   # TRL 0.15.2+ 直接传入，免 model_init_kwargs
    train_dataset=dataset,
    args=training_args,
)
```

## 量化与 LoRA 的交互细节

量化底座与 LoRA 适配器在训练时的精度关系常被误解：适配器本身始终以训练精度（bf16/fp16）存储与更新，量化只影响底座权重的存储与前向计算——所以"QLoRA 是 4-bit 训练"的说法不准确，更准确的是"4-bit 底座 + 全精度适配器"的组合训练。这个区分带来两个推论：一是适配器权重质量不直接受量化位宽影响（影响来自反量化计算的前向误差），二是训练后可以把适配器与**任意精度的底座**重新组合（例如 QLoRA 训练、fp16 底座部署）——前提是底座相同，这是 LoRA 与量化解耦带来的部署红利。另一个易错点：LoRA 梯度只回传至适配器，但前向经过量化层时仍要执行反量化与反量化权重的前向，所以梯度检查点（gradient checkpointing）对量化层同样有效，QLoRA 场景务必开启。

## FSDP + QLoRA：30B-70B 的分布式路径

传统上 4-bit 层无法被 FSDP 分片（量化权重存 uint8）。**bnb_4bit_quant_storage="bfloat16"** 让量化权重以 bf16 形态参与分片、按需反量化，从而支持多卡分布式 QLoRA 微调 30B-70B 模型。2026 年 Alignment Handbook 流程已标准化：单卡 4090 跑 7B QLoRA，多卡 FSDP 跑 70B QLoRA。

## 速度与质量的权衡数据

QLoRA 不是"白拿显存"。代价有三层：**反量化计算开销**——每次前向要把 4-bit 权重反量化为 bf16 参与运算，训练吞吐比同配置 fp16 LoRA 慢约 10-30%（具体取决于 kernel 优化与 GPU 架构）；**质量损失**——量化噪声在复杂推理任务上可观测，2026 年的共识是"窄任务无感、推理任务有感的量级"；**调试成本**——量化层参与的反向传播不透明，出问题更难排查。收益同样量化：显存降约 3.2 倍（7B：14GB→4.5GB），让"24GB 卡训 13B、16GB 卡训 7B、8GB 卡训 7B（配合分页）"成为可能。工程决策的锚点：**显存是硬约束时用 QLoRA，否则优先 fp16 LoRA**——质量与速度两个维度 fp16 都不吃亏。真正的"既要又要"方案是 2026 年成熟的分阶段策略：QLoRA 做探索（数据/超参验证），定稿后用 fp16 LoRA 或全参微调做最终训练。

## 典型配置组合速查

| 目标 | 组合 | 显存示例（7B） |
|---|---|---|
| 8GB 卡能训 | NF4 + 双重量化 + 分页优化器 + r=16 | ~6-7GB |
| 质量优先 | NF4 + 双重量化，不用分页 | ~7-8GB |
| 13B 模型 | NF4 + FSDP（quant_storage=bf16） | 多卡分摊 |
| 速度优先 | FP4 或直接 fp16 LoRA | 视显存 |

## 代价与坑

- **精度代价**：量化底座 + 反量化计算有轻微质量损失，复杂任务上差异明显时退回 fp16 LoRA。
- **算子兼容**：4-bit 层与部分自定义算子、torch.compile 组合受限；GPU 架构有最低要求（RTX 30 系后 CUDA 生态顺畅，ROCm 支持随版本差异）。
- **merge 后**：合并出的全精度权重在内存中展开，推理部署时可再重新量化，训练产物不受影响。
- **禁止 NF4 与 int8 混用**：同一模型不同层混用两种量化在 bnb 中行为未定义。

## 何时不该用 QLoRA

三条反例：**显存充足**（A100/H100 用户）——fp16 LoRA 全流程更简单、更快、无量化噪声，QLoRA 只增加复杂度；**需要与推理侧对齐**——如果部署时模型以 fp16/bf16 服务（多数生产场景），训练用 QLoRA 会导致"训练分布 ≠ 部署分布"的微小错位，量化噪声在长上下文生成中可能累积；**数值敏感任务**（科学计算、代码）——量化舍入误差在严格数值任务上不可忽略。QLoRA 的黄金定位始终是消费级显卡上的"能训起来"，而不是"训得最好"。

> 🎯 **核心要点**：QLoRA = LoRA 低秩训练 × NF4 近最优 4-bit 量化 × 双重量化压缩常数 × 分页优化器兜底显存。2026 年的配齐版本是 bnb 0.49.2 + peft 0.19.x + transformers 5.x；FSDP 分布式大模型微调的钥匙是 bnb_4bit_quant_storage 参数。

---

**参考来源**：

- [bitsandbytes: LLM Quantization for PyTorch | DEV.co](https://dev.co/ai/frameworks/bitsandbytes)
- [Train LLM LoRA Models with QLoRA on GPU Cloud (2026 Guide) | Massed Compute](https://massedcompute.com/train-llm-lora-qlora-gpu-cloud/)
- [Add quantization_config trainer argument | trl PR #6157](https://github.com/huggingface/trl/pull/6157)
- [Versioning QLoRA checkpoints | The Neural Base](https://theneuralbase.com/bitsandbytes/learn/intermediate/versioning-qlora-checkpoints/)
- [Huggingface Alignment Handbook BitsAndBytes CUDA](https://leeroopedia.com/index.php?title=Environment:Huggingface_Alignment_handbook_BitsAndBytes_CUDA&oldid=27722)

**下一模块**：[05-变体谱系与选型](05-变体谱系与选型.md) / **返回总览**：[00-LoRA深度剖析总览](00-LoRA深度剖析总览.md)
