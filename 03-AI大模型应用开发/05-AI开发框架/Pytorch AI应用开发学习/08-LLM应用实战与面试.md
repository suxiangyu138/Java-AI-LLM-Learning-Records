# 08 LLM 应用实战与面试

> 收官篇：PyTorch 在 LLM 时代的实战位置（transformers 加载/微调/显存）、AI 工程师面试速记、与仓库其他体系的闭环——从框架到应用的最后一公里。

## 📚 目录

1. [PyTorch 在 LLM 时代的定位](#1-pytorch-在-llm-时代的定位)
2. [transformers 加载与推理](#2-transformers-加载与推理)
3. [LLM 微调实战（LoRA）](#3-llm-微调实战lora)
4. [LLM 显存与精度实践](#4-llm-显存与精度实践)
5. [实战项目：图像分类完整流程](#5-实战项目图像分类完整流程)
6. [面试速记](#6-面试速记)
7. [学习路径与仓库闭环](#7-学习路径与仓库闭环)

## 1. PyTorch 在 LLM 时代的定位

```
LLM 全栈中 PyTorch 的位置：
底座：PyTorch（动态图 + autograd + GPU）
框架层：transformers（HF 生态，基于 PyTorch）
引擎层：vLLM/TensorRT-LLM（推理专用，PyTorch 模型可导入）
微调层：PEFT/LoRA（基于 PyTorch）
```

| 层 | 工具 | 与 PyTorch 关系 |
|---|---|---|
| 模型 | transformers | 基于 PyTorch |
| 微调 | PEFT（LoRA） | PyTorch 训练循环 |
| 推理 | vLLM | 兼容 PyTorch 权重 |
| 部署 | safetensors | 权重格式 |

> 结论：**LLM 生态建立在 PyTorch 之上**——学 PyTorch 是 LLM 工程的地基（与 05-AI开发框架 其他体系分工：框架体系讲编排，本体系讲模型底座）。

## 2. transformers 加载与推理

### 加载模型

```python
from transformers import AutoModelForCausalLM, AutoTokenizer

# 加载（LLM 用 causal LM）
model = AutoModelForCausalLM.from_pretrained(
    "Qwen/Qwen2.5-7B-Instruct",
    torch_dtype=torch.float16,        # 半精度（显存减半）
    device_map="auto",                # 自动分配设备
)
tokenizer = AutoTokenizer.from_pretrained("Qwen/Qwen2.5-7B-Instruct")
```

### 推理

```python
messages = [{"role": "user", "content": "解释什么是 RAG"}]
inputs = tokenizer.apply_chat_template(
    messages, add_generation_prompt=True, return_tensors="pt",
).to(model.device)

outputs = model.generate(
    inputs,
    max_new_tokens=200,
    temperature=0.7,
    do_sample=True,
)
print(tokenizer.decode(outputs[0], skip_special_tokens=True))
```

### 关键点

| 点 | 说明 |
|---|---|
| torch_dtype | FP16 标配（显存减半） |
| device_map | 多卡自动分片 |
| apply_chat_template | 对话格式（HF 标准） |
| generate 参数 | max_new_tokens/temperature |

## 3. LLM 微调实战（LoRA）

### 为什么 LoRA

```
全量微调 7B：显存 126GB+（不可行）
LoRA：冻结底座 + 训练小适配器 → 显存约 20-30GB（单卡可训）
（详见 `03-AI.../深度学习/` 与 HF 体系）
```

### PEFT 微调

```python
from peft import LoraConfig, get_peft_model
from transformers import TrainingArguments, Trainer

# 1. LoRA 配置
lora_config = LoraConfig(
    r=16,                    # 秩（小=省显存）
    lora_alpha=32,
    target_modules=["q_proj", "v_proj"],   # 只训注意力投影
    lora_dropout=0.05,
)

# 2. 包装模型
model = get_peft_model(base_model, lora_config)
model.print_trainable_parameters()   # 可训练参数 <1%

# 3. Trainer 训练（PyTorch 训练循环的封装）
training_args = TrainingArguments(
    output_dir="./lora-out",
    per_device_train_batch_size=2,
    learning_rate=2e-4,
    num_train_epochs=3,
    fp16=True,               # AMP（06 篇）
    logging_steps=10,
)
trainer = Trainer(
    model=model,
    args=training_args,
    train_dataset=dataset,
)
trainer.train()
```

### 微调要点

| 要点 | 说明 |
|---|---|
| 可训练参数 | <1%（冻结底座） |
| fp16 | AMP 应用（06 篇） |
| 适配器保存 | 只存 LoRA 权重（几 MB） |
| 底座不动 | 推理时可热插拔适配器 |

## 4. LLM 显存与精度实践

### 显存公式（回顾 06 篇）

```
推理：参数 × 2 字节（FP16）→ 7B ≈ 14GB
训练：参数 × 18 字节 → 7B ≈ 126GB（LoRA 降到 ~25GB）
量化：4bit → 参数 × 0.5 字节（7B ≈ 4GB）
```

### 精度选择

| 精度 | 显存 | 适用 |
|---|---|---|
| FP32 | 4B/参 | 小模型/训练主权重 |
| FP16/BF16 | 2B/参 | **LLM 标配** |
| INT8 | 1B/参 | 推理压缩 |
| INT4 | 0.5B/参 | 单卡跑大模型 |

### 实践建议

```
推理：FP16 起步；显存不够再量化（4bit QLoRA 风格）
训练：BF16（数值稳定优于 FP16，现代 GPU 支持）
微调：LoRA + BF16 单卡标准
```

## 5. 实战项目：图像分类完整流程

```python
"""端到端：数据→模型→训练→保存→部署（全体系串联）"""
import torch
import torch.nn as nn
from torch.utils.data import DataLoader
from torchvision import datasets, transforms

# ① 数据（03 篇）
transform = transforms.Compose([
    transforms.ToTensor(),
    transforms.Normalize((0.5,), (0.5,)),
])
train_set = datasets.MNIST("./data", train=True, download=True, transform=transform)
train_loader = DataLoader(train_set, batch_size=64, shuffle=True, num_workers=2)

# ② 模型（04 篇）
model = nn.Sequential(
    nn.Flatten(),
    nn.Linear(784, 128), nn.ReLU(),
    nn.Linear(128, 10),
)
model = torch.compile(model)          # ⑤ 编译（05 篇）

# ③ 训练（05 篇）
device = "cuda" if torch.cuda.is_available() else "cpu"
model.to(device)
opt = torch.optim.AdamW(model.parameters(), lr=1e-3)
criterion = nn.CrossEntropyLoss()

for epoch in range(5):
    for x, y in train_loader:
        x, y = x.to(device), y.to(device)
        opt.zero_grad()
        loss = criterion(model(x), y)
        loss.backward()
        opt.step()
    print(f"epoch {epoch+1}: loss={loss.item():.4f}")

# ④ 保存（07 篇，安全格式）
torch.save(model.state_dict(), "mnist_model.pt", weights_only=True)

# ⑤ 推理（07 篇）
model.eval()
with torch.no_grad():
    pred = model(test_x.to(device)).argmax(1)
```

### 项目验收清单

| 项 | 检查 |
|---|---|
| 数据管道 | MNIST 加载正确、批处理正常 |
| 训练收敛 | loss 下降、acc 上升 |
| GPU 加速 | 用上了 cuda（如有） |
| 保存加载 | weights_only 安全加载 |
| 推理正确 | 预测结果合理 |

## 6. 面试速记

### 核心结论（背诵版）

```
1. PyTorch = 张量 + autograd + 动态图（上手最易）
2. 2.x 核心：torch.compile（Dynamo 追踪→图优化→Inductor 生成）
3. 训练四步：zero_grad/forward/backward/step
4. 优化器：小模型 SGD/通用 Adam/LLM AdamW（warmup+余弦）
5. AMP：FP16 计算 + FP32 主权重（显存减半 + 1.5-3 倍速）
6. DDP 取代 DP（torchrun + DistributedSampler + 主卡保存）
7. 安全：safetensors 或 weights_only=True（torch.load 有 RCE 风险）
8. 部署：compile/AOT（免冷启动）/ONNX（跨框架）/vLLM（LLM）
9. 显存公式：推理 2B/参、训练 18B/参（LoRA 降到 ~25GB for 7B）
10. LLM 生态基于 PyTorch（transformers/PEFT/vLLM 都是它的上层）
```

### 面试高频问题

| 问题 | 回答要点 |
|---|---|
| 为什么 PyTorch 流行？ | 动态图 + autograd + HF 生态 |
| torch.compile 原理？ | 三阶段（追踪/优化/代码生成） |
| 训练不收敛排查？ | loss 曲线/学习率/数据 |
| AMP 怎么做？ | autocast + GradScaler |
| DDP 与 DP？ | 梯度同步 vs 主卡汇总 |
| torch.load 安全？ | weights_only=True（RCE 风险） |
| 7B 模型怎么训？ | LoRA + BF16 + 梯度累积 |
| 部署选型？ | 形态决定导出（compile/AOT/ONNX/vLLM） |

### 项目故事模板

> "我做过 MNIST 分类的完整流程：自定义 Dataset + DataLoader（num_workers 并行）、nn.Module 模型 + torch.compile 编译、AdamW + 余弦调度训练到 98%+ 准确率、safetensors 保存、FastAPI 服务化推理。后面用同样的技能栈做 LLM 微调：LoRA 冻结底座只训 <1% 参数，BF16 单卡跑通 7B 模型。"

## 7. 学习路径与仓库闭环

### 前置与延伸

```
前置：Python 基础（03-AI.../01-Python语言/）
理论：深度学习体系（03-AI.../深度学习/）
推理：模型推理与部署体系（03-AI.../模型推理与部署/）
HF 生态：HuggingFace 体系（03-AI.../02-大模型基础与Prompt工程/HuggingFace/）
应用：Agent/RAG 框架体系（05-AI开发框架/ 其他体系）
```

### 学习路线（完整）

```
① 本体系 01-05（入门实操：张量→训练）—— 3 天
② 06-07（GPU/部署）—— 2 天
③ 08 实战项目（MNIST 全流程）—— 1 天
④ 延伸：HF 体系（LLM 加载微调）—— 2 天
⑤ 延伸：推理部署体系（vLLM/量化）—— 2 天
```

### 最终闭环

```
PyTorch 实操（本体系）
  → transformers/PEFT（LLM 微调）
  → vLLM（推理部署）
  → Agent/RAG 框架（应用编排）
= AI 应用开发全栈能力
```

> 🎯 收官总结：PyTorch 是 AI 工程的底座——LLM 生态（transformers/PEFT/vLLM）全部建立其上；本体系覆盖"张量→训练→部署"全链路，与深度学习理论体系（是什么）、推理部署体系（怎么快）、框架体系（怎么编排）互补；安全铁律（weights_only/safetensors）贯穿始终；学完做一遍 MNIST 全流程 + LoRA 微调即毕业。

---

**返回总览**：[00-Pytorch学习体系总览](00-Pytorch学习体系总览.md)
