# NVIDIA架构演进
> 从 Volta 到 Blackwell 再到 Rubin：Tensor Core 革命、每代翻倍的算力与带宽、GB200 机柜级系统，以及 2026-2028 路线图全解。

---

## 📚 目录

1. [演进总览：十年五架构](#1-演进总览十年五架构)
2. [Hopper：Transformer 特化](#2-hoppertransformer-特化)
3. [Blackwell：双 die 与机柜系统](#3-blackwell双-die-与机柜系统)
4. [Blackwell Ultra（B300）](#4-blackwell-ultrab300)
5. [Rubin：2026 的真正飞跃](#5-rubin2026-的真正飞跃)
6. [路线图与产业格局](#6-路线图与产业格局)

---

## 1. 演进总览：十年五架构

| 架构 | 年份 | 标志 GPU | 关键创新 | 算力（FP8/FP4） |
|------|:---:|---------|---------|:---:|
| Volta | 2017 | V100 | **Tensor Core 诞生** | — |
| Turing | 2018 | RTX 20 | 光线追踪 + 推理加速 | — |
| Ampere | 2020 | A100 | 第三代 TC、稀疏加速 | 1.25 PF FP8 |
| Hopper | 2022 | H100 | Transformer 引擎、NVLink 900GB/s | 4 PF FP8 |
| **Blackwell** | 2024-25 | B200/GB200 | **双 die、FP4、NVLink 5** | 9 PF FP4 |
| Rubin | 2026 | VR NVL72 | HBM4、3nm、NVLink 6 | 50 PF FP4/GPU |
| Feynman | 2028 | — | 下一代 | — |

```
演进主线（2020s）：
  ① 算力每代翻倍以上（Tensor Core 扩展 + 数据格式降精度）
  ② 带宽每代翻倍（HBM3→3e→4）
  ③ 从"单卡"走向"机柜级系统"（NVL72 = 新计算单元）
  ④ 年更节奏（2024 起每年一代，取代 2 年一代）
```

---

## 2. Hopper：Transformer 特化

### 2.1 为什么说 Hopper 为 Transformer 而生

```
2017 Transformer 论文 → 2022 大模型爆发 → NVIDIA 把架构押在 Transformer
Hopper 的三板斧：
  ① Transformer 引擎：FP8/FP16 混合精度自动选择
  ② 更大 Tensor Core：每 SM 4 个，支持稀疏
  ③ NVLink 900GB/s：多卡训练带宽翻倍
```

### 2.2 H100 规格与地位

| 指标 | 值 |
|------|:---:|
| 晶体管 | 80B（TSMC 4N） |
| SM / CUDA 核 | 132 SM / 16896 |
| 显存 | 80GB HBM3，3.5 TB/s |
| FP16/FP8 | 989 TF / 4 PF（稀疏） |
| TDP | 700W |
| NVLink | 900 GB/s |

```
历史地位：ChatGPT 时代的第一代主力（DGX H100 = 8 卡）
2025 仍在售（中国特供 H20/H200 变体）——生命周期极长
```

---

## 3. Blackwell：双 die 与机柜系统

### 3.1 B200 规格（2024 发布、2025 放量）

| 指标 | 值 | vs H100 |
|------|:---:|:---:|
| 晶体管 | 208B（TSMC 4NP） | 2.6× |
| 架构 | **双 die**（chiplet 化） | — |
| 显存 | 192GB HBM3e，8 TB/s | 2.3× |
| FP4 | 9 PF（推理主力格式） | 新增 |
| 训练 | 2.25 PF FP8 | ~2× |
| TDP | 1000W | +43% |
| NVLink 5 | 1.8 TB/s | 2× |

### 3.2 GB200 NVL72：GPU 时代的"新服务器"

```
GB200 = 2 × B200 + 1 × Grace CPU（2.7kW 整机）
NVL72 机柜 = 36 × GB200 = 72 GPU + 36 Grace CPU

机柜级规格：
  总显存 384GB × 36 = 13.8TB，总带宽 8TB/s × 72 = 576 TB/s
  铜背板直连：任何 GPU 间 1.8TB/s 无交换机（NVLink 全互联）
  液冷：单柜功耗 ~120kW（风冷不可能）

为什么机柜化：大模型训练需要"一个逻辑 GPU"
  → 72 GPU 像一块 GPU 一样编程（NVLink 域）
  → 交换机从 3 层减到 0 层 —— 训练效率的本质提升
```

### 3.3 2025 的市场现实

```
收入：FY2025 达 $130.5B（+114% YoY）
供给：B200/GB200 售罄至 2026 年中；2025 年 GB200 出货超 100 万卡
瓶颈：CoWoS 封装、HBM3e 供应（海力士/美光涨 20%）、液冷机柜组装
命名插曲：黄仁勋承认"Blackwell 命名错了"——B200 是双 die，
  NVL72 实际是 NV144L（144 颗 die）
```

---

## 4. Blackwell Ultra（B300）

### 4.1 定位：内存升级的过渡代

| 指标 | B300 vs B200 |
|------|:---:|
| 显存 | **288GB HBM3e**（+50%） |
| 带宽 | 8 TB/s（不变——堆叠上限） |
| FP4 算力 | 15 PF（+67%） |
| 推理性能 | 2.5× H100 |

### 4.2 GB300 NVL72

```
72 GPU + 36 Grace，总内存 20TB，柜内带宽 576 TB/s
推理能力：1.5× GB200 NVL72；对比 Hopper DGX ~50×
发布时间：2025 底-2026 初
```

> 💡 观察：B300 的升级点全在"显存容量"（192→288GB）——因为 LLM 推理的瓶颈是"权重装不装得下、KV Cache 够不够"。过渡代=解决当下最痛的瓶颈。

---

## 5. Rubin：2026 的真正飞跃

### 5.1 规格（GTC 2025 公布，2026 更新）

| 指标 | Rubin（Vera Rubin） | vs B200 |
|------|:---:|:---:|
| 晶体管 | 336B（TSMC 3nm） | 1.6× |
| 算力 | **50 PF FP4** | 2.8× |
| 显存 | 288GB **HBM4**，**22 TB/s** | 2.75× |
| NVLink | 6 代，3.6 TB/s | 2× |
| CPU | **Vera**（88 核 ARMv9.2，替代 Grace） | 2× C2C |
| 训练效率 | 3.5× 提升 | — |
| 推理成本 | 每 token 成本 **-10×** | — |

### 5.2 Vera Rubin NVL72

```
72 Rubin GPU + 36 Vera CPU
  3.6 EF FP4 推理（5.6× GB200 NVL72）
  20.7TB HBM，1.6 PB/s 柜内带宽
  2.5 EF FP4 训练

新形态：NVL144（144 GPU）—— 单柜 1.2 EF FP8 训练
兼容性：可沿用 Blackwell 机柜（机柜组装时间 100 分钟 → 6 分钟）

新产品：Rubin CPX —— GDDR7 显存、专为长上下文 KV Cache 推理设计
  → 推理细分市场的首次独立产品线
```

### 5.3 为什么 Rubin 是"飞跃"而非"升级"

```
① HBM4 换代：带宽 8→22 TB/s（2.75×）—— 推理吞吐直接翻倍
② 3nm 工艺：晶体管密度 1.6×
③ Vera CPU 换代：88 核 ARM —— CPU 不再是配角
④ 每 token 成本 -10×：AI 经济学的地震
   （token 成本 = 推理总成本 / 吞吐 —— 带宽翻倍直接降价）
```

---

## 6. 路线图与产业格局

### 6.1 2026-2028 路线图

| 时间 | 架构 | 形态 | 关键点 |
|------|------|------|--------|
| 2026 H2 | **Rubin** | NVL72/NVL144 | HBM4、3nm、量产爬坡 |
| 2027 H2 | **Rubin Ultra** | NVL576（576 GPU） | HBM4e ~1TB/4GPU 封装、NVLink 7 |
| 2028 | **Feynman** | — | 下一代（HBM4e/5） |

```
Rubin Ultra NVL576（2027）：
  15 EF FP4 推理、5 EF FP8 训练（4× Rubin NVL144）
  单封装 4 die（百 PF 级 FP4）
  节奏验证：年度发布已成常态（Blackwell→Ultra→Rubin→Ultra→Feynman）
```

### 6.2 产业格局（2025-2026）

```
供应端：NVIDIA 一家独大（AI GPU 份额 >80%），TSMC CoWoS 产能被锁
竞争者：AMD MI350/MI400（见 06 模块）、Intel Gaudi（份额小）、
        云自研（TPU 等）只服务自家
需求端：云厂商 backlog >360 万卡；中国特供（H200/H20 重启生产）

风险：HBM4 产能、3nm 良率、液冷数据中心改造（电力与散热）
```

> 🎯 **核心要点**：NVIDIA 的护城河 = 硬件年更（架构/带宽/系统）+ 软件生态（CUDA/cuDNN）+ 系统能力（NVL 机柜）。Blackwell→Rubin 的本质是"把推理瓶颈（带宽）与训练瓶颈（互联）同时翻倍"——看懂带宽表，就看懂了 NVIDIA 的路线图逻辑。

---

## 7. 小结

| 架构 | 一句话印象 |
|------|-----------|
| Hopper | Transformer 特化，大模型时代主力 |
| Blackwell | 双 die + FP4 + NVLink 机柜化 |
| Blackwell Ultra | 显存 288GB 过渡代 |
| Rubin（2026） | HBM4 22TB/s、50 PF FP4、token 成本 -10× |
| 2027+ | Rubin Ultra NVL576、Feynman |

**下一模块**：[06-AMD与移动GPU](06-AMD与移动GPU.md)　**返回总览**：[00-GPU知识体系总览](00-GPU知识体系总览.md)
