# 06-PEFT 训练实战

> 定位：从环境搭建到断点续训的完整工程链路，覆盖 SFT/DPO/GRPO 三种训练范式、多 GPU 并行与高频报错排查。2026-08 基准：Python 3.10+ / torch 2.6+ / peft 0.19.1 / transformers 5.x / trl 0.15.2+。

## 环境基线

peft 0.19.x 要求 **Python 3.10+**（0.18 起弃 3.9，3.9 上 pip 会静默装回 0.17.x）；transformers 5.12.1+ 强制 peft ≥ 0.19.0，装新版 transformers 时务必核对 peft 被升级到 0.19 而非停留在 0.18（这是 2026 上半年最常见的安装事故）。QLoRA 场景再加 bitsandbytes 0.49.2；LoRA 合并（merge）需要 bf16 的 torch 版本支持。

```bash
pip install "peft>=0.19.0" "transformers>=5.12" "trl>=0.15.2" "bitsandbytes>=0.49"
```

## LoraConfig 全参数速查

| 参数 | 默认 | 说明 |
|---|---|---|
| r | 8 | 秩；SFT 建议 16 |
| lora_alpha | 8 | 与 r 钉比例（1.0 或 2.0） |
| target_modules | 必填 | 线性层名列表；不确定时用 `find_all_linear_names` 探测 |
| lora_dropout | 0 | 防遗忘正则，0.05-0.1 用于小数据 |
| bias | "none" | 保持 none |
| fan_in_fan_out | False | 只有 GPT-2 等权重转置的模型开 True |
| use_rslora | False | α/√r 缩放，高秩训练开 |
| loftq_config | None | LoftQConfig 对象，用小数据初始化 |
| modules_to_save | [] | 非 LoRA 层也训练（如 lm_head），存盘时一并保存 |

target_modules 填错名称会抛 `TargetModulesMissingException`（peft 会给出候选列表）；MoE 模型（Mistral 8x7B）注意只挂共享层，专家层挂载有专用约定。

## 端到端代码模板（SFT 全流程）

从数据到可部署适配器的完整骨架，缺一不可的五步：加载底座 → 挂 LoRA → 准备数据 → 训练 → 保存验证：

```python
from transformers import AutoModelForCausalLM, AutoTokenizer, TrainingArguments
from peft import LoraConfig, get_peft_model, prepare_model_for_kbit_training
from datasets import load_dataset
from trl import SFTTrainer

# 1. 底座：QLoRA 场景传 BitsAndBytesConfig 并调用 prepare_model_for_kbit_training（见 04 篇）
model = AutoModelForCausalLM.from_pretrained("Qwen/Qwen3-8B", torch_dtype="bfloat16")

# 2. 挂载：全部 7 个线性层
model = get_peft_model(model, LoraConfig(
    r=16, lora_alpha=16, lora_dropout=0.0, bias="none",
    target_modules=["q_proj","k_proj","v_proj","o_proj",
                    "gate_proj","up_proj","down_proj"],
))
model.print_trainable_parameters()   # 冒烟：确认 ~0.2% 级别

# 3. 数据：chat template 必须套，否则标签错位
def fmt(example):
    return {"text": tokenizer.apply_chat_template(example["messages"],
            tokenize=False, add_generation_prompt=False)}
ds = load_dataset("json", data_files="train.jsonl")["train"].map(fmt)

# 4. 训练
trainer = SFTTrainer(
    model=model, train_dataset=ds,
    args=TrainingArguments(
        output_dir="out", per_device_train_batch_size=1,
        gradient_accumulation_steps=8, learning_rate=2e-4,
        num_train_epochs=2, warmup_ratio=0.03,
        lr_scheduler_type="cosine", bf16=True,
        logging_steps=10, save_strategy="epoch",
    ),
)
trainer.train()

# 5. 保存与验证
trainer.model.save_pretrained("adapter-qwen3-8b-v1")
trainer.model.push_to_hub("user/qwen3-8b-lora")   # 可选
```

这个模板的每个"冒烟点"：第 2 步确认可训练参数占比（0.1%-0.5% 区间合理，超过 1% 说明误挂了 modules_to_save 或底座没冻结）；第 3 步打印 3-5 条 tokenize 后样本人工核对；第 4 步首步 loss 必须明显低于 log(V)（V 为词表大小，Qwen 约 10.6）——loss 不降先查数据，别调超参。

## 三范式训练要点

**SFT（监督微调）**：LR 2e-4 起步，1-3 epoch，指令数据 500-10000 条。**DPO（偏好对齐）**：LR 5e-6 量级（比 SFT 低两个数量级，直接抄 SFT 的 LR 会发散），需要偏好对数据。**GRPO（强化学习）**：群组相对奖励，LR 更低（5e-6 上下），max_lora_rank 建议 64（Unsloth 实测），且训练中多次前向采样，显存峰值远高于 SFT。三者共用同一套 peft 挂载机制，区别全在数据格式与训练器：

```python
# GRPO 中 LoRA 已原生支持（trl 0.15+）
from trl import GRPOTrainer
trainer = GRPOTrainer(
    model="Qwen/Qwen3-8B",
    peft_config=lora_config,           # GRPO 内部自动 get_peft_model
    quantization_config=bnb_config,    # PR #6157 后可直接传
    args=grpo_args, train_dataset=ds, reward_funcs=[reward_fn],
)
```

## 训练监控指标解读

LoRA 训练的三个关键监控读数及含义：**首步 loss**——应略低于 log(V)（词表大小对数，Qwen3-8B 约 10.6）；显著高于说明数据格式错（标签错位）或 LR 过大，显著低于（比如 5 以下）说明数据过简单（同质样本占多数）。**loss 曲线形态**——快速下降后平台期正常；持续上升是 LR 过大或数据噪声；下降后回升是过拟合信号（此时评估通道会先于 loss 显现问题）。**可训练参数占比**——0.1%-0.5% 区间合理；打印出的 trainable params 超过 1% 说明误挂了 modules_to_save 或冻结失效，立即停止排查。三条读数配合前面的评估双通道，构成完整的"训练 + 验证"闭环；只看 loss 单指标是 2026 年微调事故的头号原因。

## 数据格式与评估循环

训练数据三形态按训练器选择：SFT 用 messages 结构（role/content 列表，经 apply_chat_template 转文本）；DPO 需要 chosen/rejected 成对样本（同一 prompt 的两个回答）；GRPO 只需 prompt + reward_funcs（奖励函数返回数值，可叠加规则奖励与模型奖励）。评估不能只看 loss：训练集 loss 降到 0.1 以下往往意味着过拟合。推荐双通道评估——**任务通道**（业务指标，如格式正确率、答案准确率）与**通用通道**（底座原有能力抽样：数学几题、推理几题、多轮对话几题），通用通道是灾难性遗忘的探针。适配器与底座解耦还有个红利：评估可以在"启用/禁用适配器"之间直接对比（model.disable_adapter()），一句代码量化适配器带来的真实增益与伤害。

## 保存、加载与多适配器

```python
model.save_pretrained("adapter-dir")              # 只存 A/B + config，~100-150MB
model = PeftModel.from_pretrained(base, "adapter-dir")   # 挂载
model.load_adapter("adapter2-dir", adapter_name="v2")    # 多适配器共存
model.set_adapter("v2")                                  # 切换
model.disable_adapter()                                  # 临时回到底座
model.base_model.model.some_layer.weight               # 访问原始权重
```

多适配器共存的边界：同一底座可挂任意数量适配器，但**同一时刻只有一个是激活的**；适配器间组合（weighted_sum、线性插值）可用 `add_weighted_adapter`——peft 0.19.0 起支持**负权重**（适配器相减），多任务消融实验的利器。

## DPO 与 GRPO 的数据与奖励设计

偏好对齐阶段的数据质量决定成败，两个训练器的数据差异要分清。**DPO** 需要 chosen/rejected 成对样本：同一 prompt 下的好回答与坏回答，两者必须同长度同格式（只差质量），否则模型学到的是"格式差异"而非"质量差异"——实测中"chosen 与 rejected 长度差超过 30% 时对齐效果显著下降"是常见坑。**GRPO** 不需要成对数据，只需 prompt + reward_funcs 奖励函数；奖励设计的三原则：规则奖励（格式、答案匹配）优先于模型奖励（可解释、无偏置）、多个奖励函数用加权而非级联（级联会掩盖次要维度）、奖励值域归一化（同一量纲才能加权）。GRPO 的训练开销是 SFT 的数倍（每步多次采样），LoRA 场景建议先用小规模数据验证奖励函数设计，再全量训练——奖励函数错了，训完才发现，成本翻倍。

## 断点续训与多卡

- **单卡续训**：TrainingArguments 保持原样，`resume_from_checkpoint=True`，peft 自动恢复适配器与优化器状态。**禁止**中途改动 use_rslora/loftq/α，任何参数化变更都会让检查点不可续。
- **DeepSpeed**：与 LoRA 兼容良好，stage 2/3 均可，注意 offload 参数与 bnb 4-bit 层冲突（量化层不可 offload）。
- **FSDP**：LoRA 适配器自动被分片；QLoRA 需 `bnb_4bit_quant_storage="bfloat16"` 才能分片量化层（见 04 篇）。

## 训练失败的三个静默陷阱

不报错但结果错的三类事故：**chat template 未套**——原始 JSON 直接 tokenize 会让模型学到"角色的名字"而不是"角色的语气"，loss 正常下降但生成质量完全不对，这是 SFT 最常见的事故；**标签错位**——packing 场景下把跨样本边界合并进一个序列，或把 system 提示词也算进标签，同样静默；**eval 集污染**——评测样本与训练数据重叠或高度相似，分数虚高导致"上线即打脸"。三条防线：每批数据打印 5 条生成样本人工看、训练集与评测集做 overlap 检测、发布前用 hold-out 的"改写版"评测样本再验一次。LoRA 训练因为参数少、收敛快，这些错误的效果放大倍数比全参微调更高——数据管线值得投入比超参调优更多的时间。

## 训练资产的组织规范

多轮实验后项目目录会迅速失控，推荐的组织范式：**一个任务一个目录**，任务目录下按"配置指纹"命名实验子目录（如 `r16-a16-sft2e4`，指纹 = 关键超参缩写），每个实验子目录固定存放 adapter/、logs/、eval/ 三件套；训练配置（YAML 或 dataclass）与代码一并入 git，实验记录标注数据 hash 与底座版本。理由：LoRA 训练快、迭代频繁，没有配置指纹的目录两周后就是"不知道训了什么"的黑洞；而配置指纹 + 数据 hash 让"回到任意历史实验"成为可能——这比任何实验管理平台都可靠。适配器命名与模型仓库解耦（`task-lora-v1` 而非模型名），避免与底座权重混淆。

## 高频报错排查

| 报错 | 根因 | 解法 |
|---|---|---|
| TargetModulesMissingException | 模块名拼错 | 打印 model 结构或 find_all_linear_names |
| 加载后行为异常但无报错 | 底座与适配器版本/量化位宽不一致 | 核对 config.json 的 base_model_name 与 bnb 版本 |
| ValueError: Tried to convert bf16 to 8-bit | 混用位宽 | 统一 load_dtype 与量化存储类型 |
| OOM at step 1（QLoRA） | 页缓存未预热/批次过大 | 先小 batch 验证，开 gradient_checkpointing |
| 训练 loss 不动 | 数据格式错（chat template 未套） | 用 tokenizer.apply_chat_template 再 tokenize |

> 🎯 **核心要点**：工程链路的三条铁律——版本钉死（peft/transformers/bnb 三位一体）、参数化一旦开始不中途改（rslora/loftq/α 决定检查点可续性）、训练范式决定 LR 量级（SFT 2e-4、DPO/GRPO 5e-6）。跑通这条链路后，LoRA 训练只是"换数据换配置"的流水线。最后一道防线是训练资产的组织规范——配置指纹命名 + 数据 hash，让每个实验都可追溯可回滚。

---

**参考来源**：

- [PEFT v0.19.0 Release Notes](https://mirrors.yin199909.workers.dev/huggingface/peft/releases/tag/v0.19.0)
- [How to Configure Sparse-LoRA and DoRA With PEFT](https://getaibook.com/blog/how-to-configure-sparse-lora-and-dora-with-hugging-face-peft)
- [Add quantization_config trainer argument | trl PR #6157](https://github.com/huggingface/trl/pull/6157)
- [LoRA 微调超参数指南 | Unsloth](https://unsloth.ai/docs/zh/kai-shi-shi-yong/fine-tuning-llms-guide/lora-hyperparameters-guide)
- [transformers v5.12.1 发布解读](https://cloud.tencent.com.cn/developer/article/2697094)

**下一模块**：[07-合并与多 LoRA 推理服务](07-合并与多LoRA推理服务.md) / **返回总览**：[00-LoRA深度剖析总览](00-LoRA深度剖析总览.md)
