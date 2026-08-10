# 06 - 与 Transformers/PEFT 集成

> 本体系第六课：bitsandbytes 的正确打开方式——藏在 Transformers/PEFT 背后工作——"你几乎不直接 import bitsandbytes——通过 HuggingFace 生态一行接入"

---

## 📚 目录

1. [集成的定位](#1-集成的定位)
2. [推理集成：加载即量化](#2-推理集成加载即量化)
3. [训练集成：PEFT 三件套](#3-训练集成peft-三件套)
4. [统一入口 BitsAndBytesConfig](#4-统一入口-bitsandbytesconfig)
5. [练习 5 题](#5-练习-5-题)
6. [本节验收](#6-本节验收)

---

## 1. 集成的定位

**bitsandbytes 的用法哲学：不直接调它——通过 HuggingFace 生态（Transformers/PEFT）**：

```text
为什么集成使用
├── Transformers（加载）：from_pretrained 里传 quantization_config——加载即量化
├── PEFT（微调）：LoRA 配置 + get_peft_model——QLoRA 的训练入口
├── 好处：不用懂 bitsandbytes API——配置写对就行
└── 现状：2026 年 99% 的 bitsandbytes 用户是"集成用户"（不直接 import）
    ——"bitsandbytes 是发动机，Transformers 是方向盘——你开的是车，不是发动机"
```

**集成心智**：**"集成的记忆点：'两个入口'——推理用 Transformers 的 from_pretrained、微调用 PEFT 的 get_peft_model"**——**"配置对象（BitsAndBytesConfig）写一次，两个入口通用——'一套配置，推理/微调两用'"**（05 篇的底座配置在 06 篇正式"官方化"）；**为什么设计成集成**——"量化涉及'模型加载的全流程'（每层替换/钩子）——库作者把它封装进 from_pretrained——**'你不写 bitsandbytes 代码，是设计使然——不是功能缺失'"**（"直接 import bitsandbytes 的场景：研究人员/高级调参——'了解即可'的你不必"）。

## 2. 推理集成：加载即量化

**推理集成 = from_pretrained + quantization_config（一行换量化）**：

```python
# 推理集成的完整姿势（8bit/4bit 同一套写法）
from transformers import BitsAndBytesConfig, AutoModelForCausalLM, AutoTokenizer
import torch

config = BitsAndBytesConfig(load_in_4bit=True,           # 换 8bit 只需改这里
                            bnb_4bit_quant_type="nf4",
                            bnb_4bit_compute_dtype=torch.bfloat16,
                            bnb_4bit_use_double_quant=True)

model = AutoModelForCausalLM.from_pretrained(
    "Qwen/Qwen2.5-7B",
    quantization_config=config,        # ← 唯一的量化入口
    device_map="auto",                 # 自动设备分配
)
tokenizer = AutoTokenizer.from_pretrained("Qwen/Qwen2.5-7B")

# 推理照常（和全精度模型一样的 API——量化在后台）
outputs = model.generate(**tokenizer("你好", return_tensors="pt").to("cuda"), max_new_tokens=50)
print(tokenizer.decode(outputs[0]))
```

**推理心智**：**"推理集成的三行：配置对象 + quantization_config + device_map——之后和普通模型一模一样"**——**"集成的最大价值：'API 无感'——量化前后代码只有配置不同——'模型的接口不变，变的只是省显存'"**（"从 FP16 换 4bit：改一行配置 + 重跑——**'切换量化档位 = 改配置的一行'"**）；**device_map="auto" 的作用**——"自动分配设备（GPU 优先，放不下拆 CPU）——**'量化 + 自动设备 = 最省心的加载组合'"**（"注意：4bit + auto 可能静默拆 CPU（07/08 篇的坑——显存将够时手动 device_map 指定"）；**验证三件套**——"显存（get_memory_footprint）+ 推理一句（能答）+ 速度（能接受）——**'加载成功 ≠ 能用——三件套验证是集成后的标准动作'"**。

**集成的常见使用模式**（三个高频变体）："**① 换档位**——同一份代码 8bit ↔ 4bit 切换：只改 config 的 `load_in_8bit/load_in_4bit`（一个开一个关）——'换档 = 改两行'；**② 换模型**——config 复用，只换模型名（`from_pretrained` 的参数）——'模型随便换，量化配置一套'；**③ 加载后微调**——同一模型对象接着走 PEFT 三件套（06 篇 3 节）——'加载即微调的前置'——**三个模式的共同点：config 一次创建、处处复用——'把配置写成一个变量，模型随便换'"**（"写代码的好习惯：`BNB_CONFIG = BitsAndBytesConfig(...)` 放文件顶部——**'配置集中管理，改一处全局生效'"**）。

## 3. 训练集成：PEFT 三件套

**训练集成 = PEFT 三件套（LoraConfig + get_peft_model + Trainer）**：

```python
# PEFT 三件套（QLoRA 微调的训练入口——05 篇流程的正式版）
from peft import LoraConfig, get_peft_model

# ① 配置适配器（要训练的部分）
lora_config = LoraConfig(
    r=16,                                # 秩：适配器的大小（显存调节旋钮）
    lora_alpha=32,                       # 缩放：r 的 2 倍是常用起点
    lora_dropout=0.05,
    target_modules=["q_proj", "k_proj", "v_proj", "o_proj"],  # 打 LoRA 的层
    task_type="CAUSAL_LM",
)

# ② 包装模型（底座冻结 + 适配器可训）
model = get_peft_model(model, lora_config)
print(f"可训练参数: {sum(p.numel() for p in model.parameters() if p.requires_grad) / 1e6:.1f}M")
# 7B 模型 r=16 → 约 80-160M（1-2%）——"只训这些"

# ③ Trainer 训练（05 篇——standard 流程）
```

**PEFT 心智**：**"PEFT 三件套的记忆：'配适配器（LoraConfig）→ 包模型（get_peft_model）→ 训练（Trainer）'"**——"**三件套 = QLoRA 的训练骨架——底座配置（06 篇 2 节）+ PEFT 三件套 = 完整 QLoRA"**（"两篇合起来就是 05 篇的完整代码——**'本体系 04+05+06 三篇 = 一个完整的 QLoRA 教程'"**）；**LoraConfig 的参数直觉**——"**r（秩）**：适配器大小（r=16 起步、显存紧用 r=8）；**target_modules**：打 LoRA 的层（注意力层是标配）；**lora_alpha**：缩放（r 的 2 倍常用）——**'参数不懂就抄默认——r=16/alpha=32/注意力层 是万能起点'"**；**可训练参数的验证**——"打印'可训练参数'（1-2% 左右）——**'验证 LoRA 生效的证据：可训练参数占比小'"**（"打印出来 100% = 适配器没包上（bug）——**'占比是 QLoRA 配置的第一体检项'"**）；**集成排障的快捷路径**——"加载报错 → 查版本（08 篇）；微调报错 → 查可训练参数占比 + 显存；量化没生效 → 查 `model.config.quantization_config`——**'三类问题三个第一动作——报错先归类再动手，别逐行猜'"**（"集成层的坑集中在'配置没传对'——**'第一动作永远是验证配置生效了没'"**）。

## 4. 统一入口 BitsAndBytesConfig

**BitsAndBytesConfig = 量化的统一配置对象（2026 唯一入口——版本纪律）**：

```python
# BitsAndBytesConfig 参数速查（常用六个）
from transformers import BitsAndBytesConfig
import torch

config = BitsAndBytesConfig(
    # 加载侧
    load_in_8bit=False,                      # 8bit 开关（与 4bit 二选一）
    load_in_4bit=True,                       # 4bit 开关（主角）
    # 4bit 侧（load_in_4bit=True 时生效）
    bnb_4bit_quant_type="nf4",               # nf4（推荐）/ fp4
    bnb_4bit_compute_dtype=torch.bfloat16,   # 计算精度（bf16 推荐——float16 会 NaN）
    bnb_4bit_use_double_quant=True,          # 双重量化（推荐开——白赚省显存）
    bnb_4bit_quant_storage=torch.uint8,      # 存储格式（FSDP 多卡时用 uint8）
)
```

**Config 心智**：**"六个参数的记忆分组：'开关（load_in_8/4bit）+ 4bit 细节（nf4/bf16/double_quant/storage）'"**——**"抄作业级别的标准配置：4bit + nf4 + bf16 + double_quant（04 篇四件套的官方版）"**（"四件套之外的两个补充：8bit 开关（换档用）、quant_storage（多卡 FSDP 用——08 篇）——**'平时四件套，多卡加 storage'"**）；**版本纪律的落地**——"≥0.43.0 后 `load_in_4bit=True` 直接传 from_pretrained 会报错——必须包进 BitsAndBytesConfig——**'看到'load_in_4bit'参数报错 = 版本太新，改用 Config 对象'"**（01 篇版本纪律的实例——"老教程代码的适配：`load_in_4bit=True` → `quantization_config=BitsAndBytesConfig(load_in_4bit=True)`"）；**Config 的排查价值**——"报错时先打印 config 看参数对不对——**'Config 是量化的'病历本'——排障从看它开始'"**（08 篇排障入口）。

**Config 的使用流**（从创建到验证的完整闭环）："**① 创建**——`BitsAndBytesConfig(...)` 一次性写好；**② 传递**——`from_pretrained(quantization_config=config)`；**③ 复用**——同一个 config 对象可以传给多个模型（换模型只改模型名）；**④ 验证**——`model.config.quantization_config` 打印确认参数生效（没打印出 = 没传对）——**'四个步骤的闭环：创建 → 传递 → 复用 → 验证——'Config 是一次创建、处处复用"**（"排查'量化没生效'的第一动作：打印 `model.config.quantization_config`——**'Config 在模型里留了'案底'——查它就知道传没传对'"**）。

## 5. 练习 5 题

1. 集成的两个入口？（Transformers 推理 / PEFT 微调）
2. 为什么"你几乎不直接 import bitsandbytes"？（设计使然）
3. 推理集成的三行？（config + quantization_config + device_map）
4. PEFT 三件套？（LoraConfig + get_peft_model + Trainer）
5. BitsAndBytesConfig 六参数分组？（开关 + 4bit 细节）

## 6. 本节验收

**验收动作**：① 用统一 Config 分别 8bit/4bit 加载同一模型（对比显存）；② PEFT 三件套包装模型并打印可训练参数占比；③ 写"老教程代码适配"笔记（load_in_4bit=True → Config）——**"两个入口 + Config 统一 = 集成能力"**——**练习纪律**：配置一律用 BitsAndBytesConfig——"2026 年唯一入口——版本纪律不商量"。

> 🎯 **核心要点**：集成的哲学（**发动机 bitsandbytes + 方向盘 Transformers——你开的是车不是发动机**）；**推理入口（from_pretrained + quantization_config + device_map——API 无感——换档 = 改一行配置）**；**训练入口（PEFT 三件套：LoraConfig + get_peft_model + Trainer——可训练参数占比是第一体检项）**；**统一入口 BitsAndBytesConfig（六参数：开关 + nf4/bf16/double_quant/storage——四件套标准 + 多卡加 storage——老代码报错先适配它）**——"一套配置，推理/微调两用——配置写对就赢"。

---

**上一模块**：[05-QLoRA微调.md](./05-QLoRA微调.md) / **下一模块**：[07-显存与速度权衡.md](./07-显存与速度权衡.md)
