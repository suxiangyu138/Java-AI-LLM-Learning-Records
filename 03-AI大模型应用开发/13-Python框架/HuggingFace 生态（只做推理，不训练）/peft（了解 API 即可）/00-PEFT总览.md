# 00 - PEFT 总览

> 定位：参数高效微调（PEFT）事实标准库——"了解 API 即可"——冻结底座 + 注入低秩适配器，只训 1% 参数达到全量微调效果；2026 年微调过的模型大多以 adapter（几十 MB）形式分发，**加载/合并/切换 adapter 是推理侧绕不开的日常——PEFT 是训练库，更是 adapter 的加载器**

---

## 📚 目录

1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [与主体系的分工](#3-与主体系的分工)
4. [学习路线推荐](#4-学习路线推荐)
5. [核心概念速查](#5-核心概念速查)
6. [常见误区](#6-常见误区)
7. [一周学习计划示例](#7-一周学习计划示例)
8. [快速自测 10 题](#8-快速自测-10-题)
9. [参考来源](#9-参考来源)

---

## 1. 知识体系导图

```text
PEFT（本体系 11 篇——了解 API 即可）
├── 定位层：01 PEFT 是什么（参数高效微调/2026 基线/与 transformers 关系）
│          02 安装与快速开始（LoraConfig→get_peft_model 最小闭环）
├── 核心层：03 核心概念与数据流（冻结/合并/加载/多 adapter）
│          04 LoraConfig 全参数详解（API 了解的重心）
│          05 方法家族与选型（LoRA/QLoRA/DoRA/rsLoRA/…）
├── 应用层：06 训练与 Trainer 集成（显存账本/保存加载）
│          07 推理与适配器加载（三姿势/vLLM 动态加载）
│          08 QLoRA 与量化训练（单卡微调标准姿势）
│          09 适配器管理与生产实践（合并部署/加权融合）
└── 验收层：10 生产实战与自测（加载→微调→合并全链路 + 20 题）
```

## 2. 模块导航

| 篇 | 模块 | 核心内容 | 核心产出 |
|:---:|------|---------|---------|
| 00 | 总览 | 导图/分工/路线/速查/误区 | 学习计划 |
| 01 | PEFT 是什么 | 参数高效微调/2026 基线/方法全景 | 认知 |
| 02 | 安装与快速开始 | LoraConfig/最小训练脚本/闭环 | 会微调 |
| 03 | 核心概念与数据流 | 冻结/合并/加载/多 adapter | 懂模型 |
| 04 | LoraConfig 全参数 | r/alpha/target_modules/finetuning_type | 会配参 |
| 05 | 方法家族与选型 | LoRA 系全家 + 选型决策 | 会选型 |
| 06 | 训练与 Trainer 集成 | 训练管线/显存账本/保存加载 | 会训练 |
| 07 | 推理与适配器加载 | 三种推理姿势/vLLM 动态加载 | 会推理 |
| 08 | QLoRA 与量化训练 | 4bit/单卡微调/显存账本 | 会省显存 |
| 09 | 适配器管理 | 合并部署/加权融合/发布 Hub | 会生产 |
| 10 | 实战与自测 | 加载→微调→合并全链路 + 20 题 | 毕业产出 |

## 3. 与主体系的分工

**与 HuggingFace 主体系（`../../../../02-大模型基础与Prompt工程/HuggingFace/00-HuggingFace知识体系总览.md`）的分工**：主体系 08 篇"PEFT 与 LoRA 微调实战"讲微调的一站式姿势（Trainer 全流程），**本体系专讲 PEFT 库本身的 API**——"了解 API 即可"= 所有配置项、方法家族、加载合并姿势吃透，不深挖源码。

**与同级 transformers（`../transformers/00-transformers总览.md`，本目录另一套）的分工**：transformers 管模型加载与推理（`from_pretrained` 一条龙），PEFT 管适配器（训练 + 加载/合并）——**transformers v5 内置 PeftAdapterMixin 集成层，加载 adapter 是两库协作：底座走 transformers，lora 层走 PEFT**（07 篇）。

**与模型微调体系（`../../../../09-模型微调与多模态/02-LoRA微调原理与实战.md`）的分工**：那个体系讲"要不要微调、微调什么、数据怎么做"的**决策层**，本体系讲"用 PEFT 怎么写"的**API 层**——**"决策看微调体系，落地看 PEFT 库"**。

**与 强烈推荐 六套（`../../强烈推荐（做项目必用，简历加分）/`）的分工**：LangChain/LlamaIndex/milvus/unstructured 管"微调之后的推理链路"（RAG 底座），pyyaml/loguru 是微调管线的"配置 + 日志"两个地基件——**"推理链用六件套，微调链用 PEFT——同属 13-Python框架，一条产业链"**。

**2026-08 基线**：PEFT **v0.19.1**（2026-04-16 发布，需 Python ≥ 3.10）；v0.19.0（2026-04-14，新增 BD-LoRA/LoRA-GA/PVeRA/PSOFT 与"非 LoRA 转 LoRA"转换、cartridges、LoRA 支持 Transformer Engine）；v0.18.1（2026-01-09，transformers v5 兼容修复）；v0.18.0（2025-11-13，弃 Python 3.9，方法大爆发：DoRA/rsLoRA/LoRA-XS/S-LoRA/Galore-Plus/RoAd/ALoRA/Arrow/WaveFT/DeLoRA，LoraConfig 新增统一 `finetuning_type` 参数）；**transformers v5 与 PEFT < 0.18.0 不兼容**，v5.12.1（2026-06-16）把下限提到 0.19.0，后续 PR #46442 提到 0.19.1 并移除 transformers 内重复的转换代码——**"新项目锁 `peft==0.19.1`"**。

## 4. 学习路线推荐

**路线一：标准路线（3-5 天）**——01 → 10 逐篇 + 每篇跑代码——**毕业标准：独立完成"从 Hub 加载 LoRA adapter 推理 → QLoRA 微调小模型 → 合并部署"全链路**。

**路线二：推理优先路线（1-2 天）**——01 → 02 → 03 → 07 → 09（重点在"加载/合并/切换 adapter"）——适合本目录"只做推理"定位：**"99% 的日常是加载别人训好的 adapter，只有 1% 是自己训——先学加载，再学训练"**。

**路线三：训练实操路线**——手头有微调任务时按需查篇：01 → 02 → 04 → 06 → 08（直接抄最小脚本起步，参数与坑再查）。

## 5. 核心概念速查

| 概念 | 一句话 | 对应篇 |
|------|--------|:---:|
| PEFT | 参数高效微调库：只训小部分参数（LoRA/DoRA/…） | 01 |
| Adapter | 微调产物：几十 MB 的增量权重（不是新模型） | 01 |
| LoraConfig | LoRA 的配置对象（r/alpha/target_modules…） | 04 |
| get_peft_model | 把底座模型包装成 PEFT 模型（冻结 + 注入） | 02 |
| r / alpha | 低秩秩数与缩放系数（LoRA 两个核心超参） | 04 |
| target_modules | 注入 LoRA 的目标层（q_proj/v_proj…） | 04 |
| finetuning_type | v0.18+ 统一方法开关（lora/dora/rslora…） | 04 |
| merge_and_unload | 合并 adapter 进底座并卸载 PEFT 包装 | 03/07 |
| PeftModel.from_pretrained | 加载他人训练好的 adapter | 07 |
| QLoRA | 4bit 量化底座 + LoRA（单卡微调标准） | 08 |
| set_adapter | 多 adapter 切换（同一底座多任务） | 03/09 |
| adapter 一致性 | adapter 必须匹配底座型号/版本（铁律） | 07 |

## 6. 常见误区

**误区一：PEFT 是训练库，推理用不上**——2026 年微调过的模型**大多以 adapter 形式分发**，`PeftModel.from_pretrained` 就是加载器——**"推理侧的 adapter 加载/合并/切换全是 PEFT 的活"**（07 篇）。

**误区二：微调 = 全量训练**——全量微调 70B 要 42GB+ 显存，QLoRA 10GB 级别——**"2026 年单卡微调的标准答案是 PEFT，不是全量"**（08 篇）。

**误区三：r 越大越好**——LoRA 的 r 不是越大越强，**8-64 是主流区间，r>64 用 rsLoRA**（缩放 1/√r 防发散）（04/05 篇）。

**误区四：老教程姿势过时**——`use_rslora`、`DoRAConfig` 等独立写法已被 **v0.18 的 `finetuning_type` 统一参数取代**；transformers v5 配 PEFT < 0.18.0 直接报错（02/04 篇）。

**误区五：adapter 随便换个底座就能用**——**adapter 与底座型号、量化方式强绑定**，Llama-3-8B 的 adapter 装到 Llama-4-8B 上直接 KeyError/尺寸不匹配（07 篇）。

**误区六：训练完必须合并才能部署**——合并（merge_and_unload）是最简部署姿势，但 **vLLM 支持 LoRA 动态加载不合并**，多 adapter 场景反而更优（09 篇）。

**误区七：QLoRA 就是"更省显存的 LoRA"**——QLoRA 不只是 4bit，还有**双重量化 + 分页优化器 + 梯度 checkpointing** 三件套；训练慢 30-40% 是量化代价（08 篇）。

## 7. 一周学习计划示例

| 天 | 内容 | 动手任务 |
|:---:|------|---------|
| 1 | 01 + 02 | 装 `peft==0.19.1`，跑通最小 LoRA 训练脚本 |
| 2 | 03 + 04 | 改 r/alpha/target_modules，看 trainable 参数变化 |
| 3 | 05 | 对比 LoRA/DoRA/rsLoRA 三种 finetuning_type |
| 4 | 06 | 用 Trainer 训一个分类/生成任务，保存 adapter |
| 5 | 07 | 从 Hub 加载一个 LoRA adapter 推理，试 merge/unload |
| 6 | 08 | QLoRA 微调（显存对比实验） |
| 7 | 10 自测 + 面试 | 全链路实战，跑 20 题 |

## 8. 快速自测 10 题

1. PEFT 解决什么问题？"参数高效"省在哪三个维度？
2. adapter 与全量 checkpoint 的本质区别？为什么说"模型以 adapter 分发"？
3. LoraConfig 的 r/alpha/target_modules 各自是什么？选型经验值？
4. get_peft_model 做了什么？如何看可训练参数占比？
5. merge_and_unload / unload / 不合并推理三种姿势的取舍？
6. v0.18 的 finetuning_type 是什么？统一了什么 API？
7. 为什么 transformers v5 要求 PEFT ≥ 0.18.0？PeftAdapterMixin 是什么？
8. QLoRA 的三件套是什么？4bit 的代价是什么？
9. adapter 与底座匹配的"一致性铁律"具体指什么？
10. 2026-08 的 PEFT 版本基线？v0.19.x 有哪些新东西？

## 9. 参考来源

- [HuggingFace PEFT GitHub 官方仓库（Release 记录 v0.19.0/v0.19.1）](https://github.com/huggingface/peft/releases)
- [PEFT 官方文档（API 参考/概念指南）](https://huggingface.co/docs/peft)
- [PEFT PyPI 页面（0.19.1 版本信息）](https://pypi.org/project/peft/)
- [Transformers v5.12.1 Release（PEFT 下限 0.19.0）](https://github.com/huggingface/transformers/releases/tag/v5.12.1)
- [Transformers PR #46442（下限提到 0.19.1 并移除重复转换代码）](https://github.com/huggingface/transformers/pull/46442)
- [PEFT 官方博客（QLoRA/S-LoRA/DoRA 方法介绍）](https://huggingface.co/blog)

---

**下一模块**：[01-PEFT是什么.md](01-PEFT是什么.md)
