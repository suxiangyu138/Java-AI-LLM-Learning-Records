# 04 - 全量微调与 SFT 实战

> 🎯 全量微调 = 效果上限 — 需要 8×A100 和足够数据，但能做到最好的领域适配效果

## SFT 流程

```text
SFT (Supervised Fine-Tuning) = 用高质量指令数据训练 Base Model

  流程：
    ① 准备指令数据集（instruction-input-output 格式）
    ② 加载 Base Model（非 Chat Model！）
    ③ 全量参数训练（或 LoRA）
    ④ 输出 Chat/Instruct Model
```

## 全量微调 vs 其他方法

| 维度 | 全量微调 | LoRA | Prompt |
|------|:---:|:---:|:---:|
| 数据需求 | 1K-100K | 100-10K | 0-10 |
| GPU 需求 | 8×A100 (7B) | 1×4090 (7B) | 无 |
| 训练时间 | 数小时~天 | 数十分钟~小时 | 0 |
| 效果上限 | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ |
| 灾难性遗忘 | 高风险 | 低风险(冻结) | 无 |

## DeepSpeed ZeRO 分布式训练

```python
# DeepSpeed ZeRO-3 配置 → 分片模型参数到多 GPU
# ds_config.json
{
  "zero_optimization": {
    "stage": 3,                    # ZeRO-3: 参数+梯度+优化器全分片
    "offload_optimizer": {"device": "cpu"},  # 优化器状态溢到 CPU
    "offload_param": {"device": "cpu"}       # 参数溢到 CPU
  },
  "bf16": {"enabled": true},
  "train_batch_size": 32,
  "gradient_accumulation_steps": 8
}
```

```bash
# 启动分布式训练
deepspeed --num_gpus=8 train.py --deepspeed ds_config.json
```

## 全量微调超参

```python
training_args = TrainingArguments(
    output_dir="./sft-output",
    num_train_epochs=3,
    per_device_train_batch_size=4,
    gradient_accumulation_steps=8,   # total_batch = 4×8×8GPU = 256
    learning_rate=2e-5,              # 全量比 LoRA 小 10×！
    warmup_ratio=0.03,               # 3% step 用于 warmup
    lr_scheduler_type="cosine",
    bf16=True,
    save_strategy="steps",
    save_steps=500,
    logging_steps=10,
    gradient_checkpointing=True,      # 省显存
    deepspeed="ds_config.json",
)
```

## 关键注意事项

| 注意 | 说明 |
|------|------|
| **小学习率** | 全量 2e-5 vs LoRA 2e-4，差 10 倍 |
| **Warmup** | 全量至少 3% step，避免初期震荡 |
| **Gradient Checkpointing** | 省 30-40% 显存，代价是 20% 速度 |
| **灾难性遗忘** | 混入 5-10% 通用数据、epoch=1~3 |
| **验证集** | 留 10% 数据做验证，监控过拟合 |
