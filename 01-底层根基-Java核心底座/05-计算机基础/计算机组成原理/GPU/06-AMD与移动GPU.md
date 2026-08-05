# AMD与移动GPU
> AMD 的 CDNA 数据卡与 RDNA 消费级双线、Infinity Cache 的差异化设计、MI350/MI400 追赶路线图，以及集成 GPU（iGPU）的移动生态。

---

## 📚 目录

1. [AMD 的双线战略：CDNA 与 RDNA](#1-amd-的双线战略cdna-与-rdna)
2. [Infinity Cache：AMD 的差异化设计](#2-infinity-cacheamd-的差异化设计)
3. [CDNA4 MI350 系列](#3-cdna4-mi350-系列)
4. [MI400：2026 的对标反击](#4-mi4002026-的对标反击)
5. [RDNA 与消费级 GPU](#5-rdna-与消费级-gpu)
6. [集成 GPU：移动生态的战场](#6-集成-gpu移动生态的战场)

---

## 1. AMD 的双线战略：CDNA 与 RDNA

| 产品线 | 架构 | 定位 | 代表 | 特点 |
|--------|------|------|------|------|
| **CDNA** | 计算专用 | 数据中心/AI | MI300、MI350、MI400 | 无图形、纯计算、矩阵优先 |
| **RDNA** | 图形/计算兼顾 | 消费级/RX 显卡 | RX 7000/9000 | 游戏 + 通用计算 |

```
分离原因：数据卡与游戏卡的需求完全不同
  数据中心：矩阵算力、显存带宽、多卡互联、可靠性
  游戏：光栅化、光追、帧率、每瓦图形性能

2025 状态：AI 时代 AMD 重心明显转向 CDNA（MI 系列是财报主角）
```

---

## 2. Infinity Cache：AMD 的差异化设计

### 2.1 什么是 Infinity Cache

```
GPU 片上大容量 L3 级缓存（不是 HBM，是 SRAM/高速缓存）：
  RX 7900 XTX：96MB
  MI300X：256MB
  MI350：256MB

作用：拦截对显存的访问 → 等效带宽放大
  96MB 缓存 + 1TB/s 片上带宽 → 游戏场景等效 3-4 倍显存带宽
```

### 2.2 为什么 AMD 走"大缓存"路线

```
① 显存带宽差距：AMD 的 GDDR/HBM 带宽常低于同代 NVIDIA
   → 用缓存补带宽（带宽墙的缓存化解法）
② 能效：缓存命中功耗 << 显存访问功耗
③ 成本：片上缓存比加 HBM 便宜

局限：
  - 大缓存是"赌局部性"——流式负载（大量一次性数据）命中率低
  - 深度学习训练/推理多为流式 → Infinity Cache 对 AI 负载收益有限
  - 数据卡场景 256MB 相对 288GB 工作集微不足道
```

> 💡 设计哲学对照：NVIDIA 押"更大带宽"（HBM 8-22TB/s），AMD 押"更大缓存 + 中等带宽"——两条路在游戏场景 AMD 有效（命中率高），在 AI 负载 NVIDIA 的带宽路线占优。理解这个差异，就理解了 MI350 为何与 B200 打"显存容量 + 性价比"牌。

### 2.3 技术细节

```
Infinity Cache 挂在 Infinity Fabric 上：
  L2 → Infinity Cache → HBM
  256MB 分片 + 一致性管理（GPU 内）

代价：芯片面积（256MB SRAM ≈ 数百 mm² 等效）、设计复杂度
```

---

## 3. CDNA4 MI350 系列

### 3.1 发布与定位（2025-06-12 "Advancing AI 2025"）

```
MI350X / MI355X：AMD 对 Blackwell B200 的直接回应
口号：4× AI 算力、35× 推理提升（vs MI300）、
      每 token 成本比 B200 低 ~40%
```

### 3.2 规格表

| 指标 | MI350X / MI355X |
|------|:---:|
| 制程 | TSMC 3nm（N3P）计算芯粒 + 6nm I/O |
| 晶体管 | ~185B（10 个 chiplet） |
| 计算单元 | 256 CU / 16384 核（8 个 XCD） |
| L2 / Infinity Cache | 32MB / **256MB** |
| 显存 | 288GB HBM3E（12-Hi），8 TB/s |
| FP4/FP6 | ~18.5-20 PF（推理主打） |
| FP8 | ~10 PF |
| FP16 | ~2.3-2.5 PF |
| 功耗 | 1000W（风冷）/ 1400W（液冷） |
| 互联 | 4 代 Infinity Fabric 1075 GB/s |
| 支持模型 | 单分区跑 520B 参数 |

### 3.3 市场策略与实测表现

```
差异化卖点：
  ① 显存 288GB = B200 的 192GB × 1.5（大模型更有余量）
  ② FP6 推理：比 FP8 快 2×、比 FP4 稳 —— 精度/吞吐的甜点
  ③ 每 token 成本 -40%（显存便宜 + 缓存策略）

对标实测（厂商口径，需谨慎）：Llama 3.1 405B / DeepSeek R1
  FP6 推理 2.2× B200；部分模型吞吐持平 GB200

生态：ROCm 支持 PyTorch 全栈；合作 OpenAI（MI400 联合开发）
```

> ⚠️ 厂商自测数据需第三方验证——MI350 的"35×"等数字是 vs MI300 的跨代累计，实际购买决策看第三方推理基准（MLPerf、tokens/s/$）。

---

## 4. MI400：2026 的对标反击

### 4.1 规格预告（2025 Hot Chips / 发布计划）

| 指标 | MI400（2026） | vs MI355X |
|------|:---:|:---:|
| 制程 | TSMC 2nm | — |
| 架构 | CDNA Next（传闻统一 UDNA） | — |
| 芯粒 | 8 个计算芯粒（XCD）+ 独立多媒体 I/O die | — |
| 显存 | **432GB HBM4，19.6 TB/s** | 1.5× / 2.5× |
| 算力 | 40 PF FP4 / 20 PF FP8 | ~2× |
| 互联 | UALink：72 GPU 统一域 | 新标准 |
| 平台 | Helios 机柜（72 GPU，260 TB/s） | — |

### 4.2 UALink：AMD 牵头的互联标准

```
UALink（Ultra Accelerator Link）：AMD + 博通 + 微软等开放标准
  对标 NVIDIA NVLink —— 打破 NVLink 生态锁定
  Helios 机柜：72 MI400，260 TB/s 机柜内带宽，~2.9 EF FP4

意义：多厂商互联标准 = 对 NVIDIA "全家桶"的一次反击
  但生态与 NVLink 的成熟度差距仍需数年
```

### 4.3 平台配套（2026 全家桶）

```
MI400 + Zen 6 "Venice" EPYC（256 核）+ Pensando "Vulcano" NIC（800G）
  对标 NVIDIA 的 Rubin + Vera + NVLink 全家桶
  2030 目标：机柜能效较 2024 提升 20×
```

> 🎯 观察：AMD 的追赶策略 = "带宽/容量略超 + 价格便宜 + 开放标准"——但在软件生态（cuDNN 级库）与系统成熟度上仍落后 1-2 年。2026 的 MI400 vs Rubin 是 AMD 十年最重要的正面对决。

---

## 5. RDNA 与消费级 GPU

### 5.1 RDNA4（RX 9000 系列，2025）

| 特性 | 说明 |
|------|------|
| 定位 | 中高端为主（放弃旗舰，主攻性价比） |
| 架构改进 | 光追增强、AI 加速（WMMA） |
| 显存 | 16-32GB GDDR6/7 |
| 对手 | RTX 5070/5080 档位 |
| 卖点 | 显存大、功耗好、FSR 生态 |

### 5.2 消费级 vs 数据卡的架构分岔

```
RDNA：保留传统图形管线 + 计算单元（游戏与 AI 两用）
CDNA：砍掉图形管线，纯计算（面积全给矩阵/向量）

趋势（2025）：消费卡也开始强调 AI（FSR 4 的 AI 上采样、NPU 集成）
  但数据卡才是 AMD 的战略重心（MI 系列利润率远超 RX）
```

---

## 6. 集成 GPU：移动生态的战场

### 6.1 iGPU 的三种路线

| 方案 | 代表 | 特点 |
|------|------|------|
| 核显（低端） | Intel UHD / AMD Radeon 核显 | 办公/影音 |
| 强核显 | **AMD Radeon 890M**、Intel Arc Xe2 | 1080p 中画质游戏 |
| SoC 级 GPU | Apple M 系、Snapdragon X | 统一内存、高效 |

### 6.2 Apple M 系 GPU 的独特设计

```
① 统一内存架构（UMA）：CPU/GPU 共享同一物理内存
   → 免拷贝（CPU 写 GPU 直接读）—— 传统独显的最大开销消失
   → 代价：带宽共享（M4 带宽 120GB/s vs 独显 500+GB/s）

② 可扩展：M4 Max 双 die 合体（GPU 核心数翻倍）
③ 效率路线：不冲峰值，追求每瓦（笔记本续航 20h+）

教训：UMA 适合端侧 AI（模型常驻、带宽够用）；
      训练/大推理仍需独显的带宽与容量
```

### 6.3 iGPU 的 AI 角色（2025-2026）

```
端侧 AI 落地的"免费算力"：
  Windows AI PC 的 NPU + iGPU 组合（Strix Point 等）
  本地小模型推理（Phi-3/Llama 3.2 1-8B）iGPU 完全可跑
  Apple 的 M 系是 Mac 本地 LLM 的默认引擎（Ollama 等）

注意：iGPU 共享系统内存 → 带宽是天花板
  8B 模型每秒 ~10-20 token（Mac 级）—— 可用但别指望推理服务器
```

> 🎯 **核心要点**：AMD = 双线作战（CDNA 追 NVIDIA、RDNA 守消费）、Infinity Cache 差异化、UALink 破生态；移动端 = UMA 效率路线 vs 独显带宽路线。2026 的看点集中在 MI400 vs Rubin 的正面碰撞。

---

## 7. 小结

| 问题 | 答案 |
|------|------|
| CDNA vs RDNA？ | 数据卡纯计算 vs 消费卡图形+计算 |
| Infinity Cache 是什么？ | 片上 256MB L3，等效带宽放大（游戏有效，AI 有限） |
| MI350 的卖点？ | 288GB 显存、FP4/FP6、每 token 成本 -40% |
| MI400 什么时候？ | 2026，432GB HBM4、19.6TB/s、UALink 72 GPU |
| UMA 是什么？ | 统一内存（Apple），免拷贝但带宽共享 |

**下一模块**：[07-AI时代的GPU](07-AI时代的GPU.md)　**返回总览**：[00-GPU知识体系总览](00-GPU知识体系总览.md)
