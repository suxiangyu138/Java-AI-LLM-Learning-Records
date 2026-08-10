# 04 - LoraConfig 全参数详解

> 定位：本体系 API 了解的重心——LoraConfig 的完整参数面——"r/alpha/target_modules 是日常，finetuning_type 是 v0.18 的方法总开关，init_lora_weights/use_dora 等是进阶旋钮——全部参数一张表，选型心法一段话"

---

## 📚 目录

1. [参数全景分组](#1-参数全景分组)
2. [结构参数：r / alpha / dropout / bias](#2-结构参数r--alpha--dropout--bias)
3. [目标层参数：target_modules 匹配规则](#3-目标层参数target_modules-匹配规则)
4. [方法参数：finetuning_type 统一开关](#4-方法参数finetuning_type-统一开关)
5. [初始化与量化参数](#5-初始化与量化参数)
6. [进阶参数与版本差异](#6-进阶参数与版本差异)
7. [选型心法：参数怎么定](#7-选型心法参数怎么定)
8. [五个常见坑](#8-五个常见坑)
9. [练习 5 题](#9-练习-5-题)

---

## 1. 参数全景分组

LoraConfig 的全部参数可以分成五组，**记住分组就记住了 API**：

| 组 | 参数 | 一句话 |
|----|------|--------|
| 结构 | r / lora_alpha / lora_dropout / bias | adapter 的粗细/强度/正则/偏置策略 |
| 目标层 | target_modules / target_parameters | 往哪些层注入（字符串匹配/对象匹配） |
| 方法 | finetuning_type / use_dora / use_rslora | v0.18+ 统一方法开关与旧参数 |
| 初始化 | init_lora_weights / layers_to_transform | 分支初始权重策略/只注入部分层 |
| 量化协同 | use_dora 的量化适配、与 bitsandbytes 的组合 | QLoRA 场景的配套设置 |

日常只用前两组，第三组在 v0.18+ 变得重要（统一 API），后两组按需。

**一个全景示例**（含五组参数，注释即索引）：

```python
LoraConfig(
    r=16, lora_alpha=32, lora_dropout=0.1, bias="none",        # 结构组
    target_modules=["q_proj", "k_proj", "v_proj", "o_proj"],   # 目标层组
    finetuning_type="dora",                                    # 方法组（v0.18+）
    init_lora_weights="gaussian", layers_to_transform=None,    # 初始化组
    task_type="CAUSAL_LM",                                     # 任务类型
)
```

**task_type 的定位**：它不在五组里，但决定注入位置参考与 PeftModel 子类——分类/生成/序列标注各一个，**生成模型必须 CAUSAL_LM**。

## 2. 结构参数：r / alpha / dropout / bias

**r（秩数）**——LoRA 分支的宽度：A 是 r×d、B 是 d×r，参数量正比于 r。**r 决定"表达能力"与"容量"**：r=8 适合任务差异小的场景（指令微调够用），r=64 适合任务差异大的场景（代码/领域知识注入）。**关键认知：r 不是越大越好**——低秩假设认为模型更新本身是低秩的，r 过大反而过拟合；r>64 时建议 `use_rslora=True`（缩放改为 1/√r，防高秩发散，见第 4 节）。

**lora_alpha（缩放系数）**——注入强度：`W' = W + (α/r)·BA`，**α 与 r 的比值决定实际缩放**。经验法则 `alpha = 2×r`（r=8 → α=16）；**只调 alpha 不动 r = 只调强度不动容量**——训练后期想增强任务效果，先加 alpha。

**lora_dropout**——分支 dropout 概率，防过拟合；小数据 0.1，大数据 0.05 即可，**过大的 dropout 会显著削弱 adapter 效果**（0.3+ 罕见）。

**bias**——是否训练偏置参数：`none`（默认，不训）/ `all`（训全部偏置）/ `lora_only`（只训 LoRA 层的偏置）。**偏置参数极少（万级），训练与否对效果影响有限——保持默认 none**，除非做消融实验。

## 3. 目标层参数：target_modules 匹配规则

**target_modules 是注入位置的指定，也是最常见的出错点**。匹配规则：

- **字符串列表**（最常用）：`["q_proj", "v_proj"]`——层名包含这些子串的线性层全部注入；
- **单个字符串**：`"all-linear"`——**注入模型里所有 nn.Linear 层**（v0.7+ 支持，小模型无脑用）；
- **正则表达式**：`re.compile(r"mlp\.(gate|up)\.proj")`——按正则精确匹配层名；
- **对象匹配（v0.17+）**：`target_parameters=[nn.Parameter]`——**按参数对象类型匹配，专治 MoE 模型**（Llama-4 的专家权重是裸 nn.Parameter，target_modules 匹配不到，用 target_parameters 注入）。

**选型主线**：**生成模型（GPT/Qwen 系）惯例注入 q/k/v/o 四个投影**（注意力部分，效果与开销平衡点）；小模型/实验用 `"all-linear"`；**找不到层名时先 `model.named_modules()` 打印核对——别猜，猜必错**。

## 4. 方法参数：finetuning_type 统一开关

**v0.18.0 是 API 分水岭**：此前想用 DoRA 要换 `DoRAConfig`、用 rsLoRA 要传 `use_rslora=True`、用 LoRA-XS 要换 `LoRAXSConfig`——**v0.18 起全部收敛进 LoraConfig 的 `finetuning_type` 一个参数**：

```python
LoraConfig(
    r=16, lora_alpha=32,
    target_modules=["q_proj", "k_proj", "v_proj", "o_proj"],
    finetuning_type="dora",      # lora（默认）/ dora / rslora / lora_xs / slora / shira
)
```

| finetuning_type | 方法 | 一句话 | 场景 |
|:---:|------|--------|------|
| lora | 标准 LoRA | 事实基线 | 默认场景 |
| dora | DoRA | 幅度/方向解耦，0.8% 参数平齐全量微调精度 | 追求与全量持平的精度 |
| rslora | rsLoRA | 缩放 1/√r，高秩稳定 | r > 64 |
| lora_xs | LoRA-XS | SVD 初始化，再省 15-20% 参数 | 边缘设备/显存极限 |
| slora | S-LoRA | 稀疏掩码只更新关键参数，收敛快约 1.4x | 追求训练速度 |
| shira | SHiRA | 只训 1-2% 权重，天然稀疏 | 多 adapter 高频切换 |

**注意版本门槛**：`finetuning_type` 是 v0.18+ 的 API，老教程里的 `use_dora=True`、`DoRAConfig` 在 0.19 仍兼容但已过时——**"2026 写法：一个 LoraConfig + finetuning_type 走天下"**。

## 5. 初始化与量化参数

**init_lora_weights**——LoRA 分支的初始权重策略：`gaussian`（默认，A 高斯初始化、B 全零——保证训练开始时增量恒为零，即"从原模型出发"）；`pissa`（PiSSA 初始化：对 W 做 SVD，把主成分放进 A/B——收敛更快但改变初始行为）；`loftq`（用 4bit 量化误差初始化——QLoRA 场景推荐，把量化丢掉的精度补回一点）。**默认 gaussian 不动；QLoRA 微调时试 loftq 有惊喜**。

**pissa 与 loftq 的取舍**：pissa 适合"底座质量高、任务数据少"的场景（收敛快但初始行为被改变，与 gaussian 的结果可能不同——**做对比实验时保持统一初始化**）；loftq 只在 4bit 底座上有意义——**它用底座量化的误差信息初始化分支，等价于"补偿量化丢失的信息"**，QLoRA 场景值得一试；**默认 gaussian 永远是安全起跑线**。

**layers_to_transform**——只对部分层注入（如只注入前 12 层）：调试用，常规不设。

**量化协同参数**（QLoRA 场景，08 篇详讲）：PEFT 侧通常不用额外参数——4bit 底座由 `AutoModelForCausalLM.from_pretrained(..., load_in_4bit=True)` 加载，PEFT 自动识别底座已量化并挂载 LoRA；只需保证 **bitsandbytes 版本 ≥ 0.46.1**（transformers v5 要求）。

## 6. 进阶参数与版本差异

**v0.19 新增**：`tie_weights` 相关——target_modules 支持**权重绑定**（tied weights，如 LM head 与 embed 共享的场景）；**非 LoRA adapter 转 LoRA 的转换函数**（`convert_non_lora_to_lora` 一族）——**把旧的非 LoRA 方法产物转成 LoRA 格式**（v0.19 特性，转换后可享受 LoRA 的动态加载生态）；cartridges 支持（模块化加载器形态，了解即可）。

**版本差异速记**：0.18 引入 finetuning_type；0.17 引入 target_parameters；0.16 前是 use_dora/use_rslora 等旧参数时代——**看教程先看它写的是哪个版本，老教程的参数名对 0.19 不一定有效**。

**旧参数对照表**（老教程读者必看）：`use_dora=True` → `finetuning_type="dora"`；`use_rslora=True` → `finetuning_type="rslora"`；`LoRAXSConfig` → `finetuning_type="lora_xs"`——**v0.18 起新写法统一收敛，旧写法在 0.19 仍兼容但新代码别再用了**。

## 7. 选型心法：参数怎么定

**一句话模板：`r=16, alpha=32, dropout=0.1, target=q/k/v/o, finetuning_type 默认 lora` 起步，然后按信号调**：

- **过拟合**（训练 loss 低、验证 loss 高）→ 降 r（16→8）、降 alpha（32→16）、升 dropout；
- **欠拟合/任务效果不足** → 升 r（16→32）、升 alpha（32→64）、换 dora；
- **收敛太慢** → 升学习率（2e-4 → 5e-4）、试 slora；
- **显存顶不住** → 降 r、减小 batch、上 QLoRA（08 篇）。
- **改一个参数跑一次**——r 与 alpha 联动时记住公式 `W' = W + (α/r)·BA`，**"容量看 r，强度看 α/r"**。

## 8. 五个常见坑

- **坑一**：finetuning_type 参数在 0.17 及以下直接报错——**先确认 peft ≥ 0.18**（锁 0.19.1 就没事）。
- **坑二**：target_modules 拼错不报错、静默 0% 注入——**每次都看 print_trainable_parameters**。
- **坑三**：alpha 只给 4 或给 128 的极端值——注入强度失衡，效果差；**先按 2×r 起步**。
- **坑四**：r=128 不配 rslora——训练发散 loss 飙到 NaN；**高秩必配 use_rslora/finetuning_type="rslora"**。
- **坑五**：init_lora_weights="loftq" 在非量化模型上用——报错或无效；**loftq 是 QLoRA 的配套选项**。
- **坑六**：`task_type` 写错（分类模型写成 CAUSAL_LM）——不报错但注入位置与分类头行为不符，效果莫名差；**任务类型先核对再训练**。

## 9. 练习 5 题

1. LoraConfig 五组参数各是什么？日常只用哪两组？
2. r 与 alpha 各管什么？"容量看 r，强度看 α/r"怎么理解？
3. target_modules 的四种写法？MoE 模型为什么用 target_parameters？
4. finetuning_type 能选哪些值？各自一句话定位？
5. 过拟合/欠拟合/收敛慢/显存紧四类信号各调什么参数？

> 🎯 **核心要点**：LoraConfig = **五个参数组**（结构/目标层/方法/初始化/量化）；日常 `r=16, alpha=32, target=q/k/v/o`，方法切换靠 v0.18 的 `finetuning_type` 一个开关；**"容量看 r，强度看 α/r，高秩配 rslora，QLoRA 配 loftq"**——这就是全部选型心法。

---

**下一模块**：[05-方法家族与选型.md](05-方法家族与选型.md) / **返回总览**：[00-PEFT总览.md](00-PEFT总览.md)
