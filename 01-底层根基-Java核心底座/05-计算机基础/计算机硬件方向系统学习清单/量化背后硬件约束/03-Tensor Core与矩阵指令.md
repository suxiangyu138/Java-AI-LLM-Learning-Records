# Tensor Core 与矩阵指令：量化加速的硬件执行单元
> 量化的收益最终要落到矩阵乘指令上：Tensor Core 吃哪种格式、在哪里反量化，决定了量化是"真加速"还是"只省内存"。

## 1. GEMM 是 LLM 的唯一主角

Transformer 的计算主体是**矩阵乘**：每个线性层把权重矩阵 W 与激活向量 x 相乘，attention 里是 Q、K、V 三组投影与分数矩阵。GPU 为此专门设计了矩阵乘执行单元——NVIDIA 叫 Tensor Core，AMD 叫 Matrix Core（MFMA 指令），Intel 叫 AMX，TPU 叫 MXU。**没有这些单元，量化精度再低也换不来吞吐**，因为通用 CUDA Core 做矩阵乘的效率只有 Tensor Core 的几分之一。

矩阵乘的本质是乘加累加（FMA）：每个输出元素是 K 次乘加之和。硬件上的关键设计是**累加器精度高于输入精度**——Tensor Core 一律用 FP32 或 INT32 累加，这样即使输入是 INT4/FP8，中间和的误差也不会累积爆掉。这也是"混合精度"的硬件前提。

### 1.1 一次矩阵乘如何在硬件上展开：tile 与指令

Tensor Core 的运算粒度是"一块小矩阵"，而不是整个大矩阵：

- **WMMA/mma.sync 指令**：NVIDIA 的矩阵乘指令按 tile 执行，典型如 `mma.sync.m16n8k16`（16×8 输出、K=16 的累加块）——一次指令完成 16×8×16 的乘加，约 2048 次 FMA
- **CUTLASS 的分层 tiling**：SM 级 tile（128×256）→ warp 级 tile（64×64）→ 指令级 tile（16×8），层层切分以匹配 SRAM 容量与寄存器带宽
- **TMA（Tensor Memory Accelerator）**：Blackwell/Hopper 的异步搬运单元，把数据从 HBM 预取到 SRAM 与矩阵乘重叠执行——搬运与计算流水线化，是量化后"少搬的字节"与"计算空闲"精确匹配的硬件前提

理解 tiling 的意义在于：量化后的权重是**压缩存储的**，必须以正确的 tile 形状解包。FP4 的打包方式（两个 4 位值拼 1 字节）决定了解包必须对齐 32 字节边界，这一约束直接写进了 Blackwell 的 CUTLASS schedule——这也是为什么 FP4 内核工程如此艰难、引擎间差距如此之大。

## 2. Tensor Core 精度演进：每个世代解锁一种格式

| 世代 | 微架构 | 新增量化精度 | 代表卡 |
|------|--------|-------------|--------|
| Volta | V100 | FP16 | — |
| Turing | TU10x | INT8（W8A8） | RTX 20 系列 |
| Ampere | GA10x | BF16、INT4 反量化路径 | A100/A10 |
| Hopper | GH100 | FP8（E4M3/E5M2） | H100/H200 |
| Blackwell | SM100 | FP4/NVFP4/MXFP4 | B200/B300/RTX 5090 |

规律很清晰：**大约每两代解锁一个更低精度**，且新精度向下兼容旧格式。INT8 从 Turing 到 Blackwell 都原生支持，这是 W8A8 方案"任何现代卡都能跑"的原因；FP8 需要 Hopper 起步；FP4 则是 Blackwell 独享——老卡上跑 FP4 只能软件回退。

### 2.1 从指令到密度数字：一次矩阵乘的吞吐怎么算出来

Tensor Core 的标称 TFLOPS 不是凭空来的，可按下式验算：

```text
FP16 稠密 TFLOPS ≈ 指令 FMA 数 × SM 数 × 每 SM 单元数 × 频率 × 2
```

以 Hopper 的 FP8 为例：FP8 矩阵乘的输入位宽是 FP16 的一半，数据在同样的数据通路里每次能装两倍的元素——**所以 FP8 密度是 FP16 的 2 倍，FP4 又翻倍成 4 倍**。这个"位宽减半 → 密度翻倍"的线性关系只在数据通路满载时成立：引擎必须真把 4 位数据喂满寄存器文件。若反量化先行（把 FP4 变回 FP16 再算），密度就退回 FP16 档——再次说明**执行端必须原生吃低精度格式**，否则标称密度只是装饰。

### 2.2 稀疏 Tensor Core：2:4 结构稀疏与量化的叠加

Ampere 起 Tensor Core 支持 2:4 结构稀疏：权重矩阵每 4 个元素最多保留 2 个非零，配合索引位图存储，**计算密度翻倍、存储减半**。它与量化的关系常被忽略：

- 稀疏与量化在存储上叠加：INT4 权重再 2:4 稀疏，等效每权重 0.25 字节
- 但稀疏精度损失与量化损失叠加，需要更精细的校准
- 稀疏的硬件收益同样只在新指令上有：稀疏 FP4 即 Blackwell 标称 18 PFLOPS 的来源（稠密 9 PFLOPS）

生产实践上，稀疏主要用于 MoE 模型的专家权重（本身有天然稀疏结构），稠密模型的稀疏化收益有限——这是"硬件能力存在"与"业务可获益"不对等的典型。

## 3. 反量化位置决定成败：SRAM 内 vs HBM 往返

权重以低精度存储，进入 Tensor Core 前必须恢复成计算精度（通常 FP16/BF16），这个恢复过程叫**反量化**。它在哪发生，直接决定量化收益是否成立：

- **正确做法**：HBM 只搬运紧凑的 INT8/INT4 权重 → 在片上 SRAM/寄存器内反量化 → 直接喂给 Tensor Core。BF16 权重全程不经过 HBM，带宽收益完整保留。
- **错误做法**：编译器看到 BF16 激活 × INT8 权重类型不匹配，把权重先反量化成 BF16 写回 HBM，再重新读入——**带宽成本一分没省，量化白做**。

XLA 等自动编译器常犯后者（JAX 生态实测踩坑），修复手段是手写 Pallas/CUTLASS 内核，把"读压缩权重→片内反量化→矩阵乘"熔成一个 kernel。这也是 2026 年所有高性能推理引擎（vLLM/SGLang/FlashInfer/TensorRT-LLM）都在卷 kernel 工程的原因：**同一个 Blackwell FP4 模型，不同引擎的实测吞吐能差 15%**。

## 4. 为什么 INT4 卡在 CUDA Core：指令级约束

A100/H100 没有 INT4 的矩阵指令，GPTQ/AWQ 的 W4A16 内核只能走这条路：CUDA Core 上把 INT4 权重解包、反量化成 FP16，再交给 Tensor Core 算 FP16 矩阵乘。结果是：

- 反量化本身占用 CUDA Core 吞吐，权重越界、组缩放越密，开销越大
- 矩阵乘主运算仍在 Tensor Core，但**数据搬运仍按 0.5 字节/权重**——内存收益完整，算力收益打折扣
- 因此 W4A16 在 Ampere/Hopper 上的定位是"省显存、降延迟"，不是"吃满算力"

Blackwell 把这条路堵死又打开：FP4 Tensor Core 只认 E2M1 格式（NVFP4/MXFP4），INT4 数据格式进不去；但**原生 FP4 指令的吞吐是 FP8 的两倍**——前提是量化方案改用 MX 格式体系（详见 02 篇），这正是 2026 年 MR-GPTQ、MicroMix 等研究集中解决的方向。

### 4.1 各平台矩阵指令对照：同一意图，不同接口

| 平台 | 指令族 | 4-bit 能力 | 量化接入方式 |
|------|--------|-----------|-------------|
| NVIDIA | `mma.sync` / WMMA | FP4（SM100 起） | CUTLASS/TensorRT-LLM |
| AMD | `v_mfma_scale_*` | FP4/FP6/FP8 混合（CDNA4 起） | ATOM/AITER，ROCm 7.x |
| Intel Xeon | AMX | 无 4-bit，INT8/BF16 | oneDNN/Neural Compressor |
| Google TPU | MXU | 无 4-bit，INT8/BF16 | XLA |
| Qualcomm/Apple NPU | 私有矩阵核 | INT8/INT16 为主 | QNN/CoreML 编译器 |

两个要点：**4-bit 矩阵指令是 2025-2026 的新战场，仅 NVIDIA/AMD 两条线**；其余平台（含 TPU）的矩阵指令仍停在 INT8/BF16——它们的"4-bit 部署"全部是软件反量化，收益只有内存侧。这也是 09 篇端侧部分的核心矛盾：NPU 算力再强，位宽支持不到 4-bit，量化就吃不到全量红利。

> 🎯 **核心要点**：Tensor Core 是量化收益的落点，约每两代解锁一个更低精度（Turing INT8 → Hopper FP8 → Blackwell FP4）；矩阵乘按 tile 执行（mma.sync/CUTLASS 分层），FP4 的打包格式把解包约束写进内核 schedule——这就是 FP4 内核工程难的根源；反量化必须在片上 SRAM 完成，任何"反量化回 HBM"的实现都会让量化白做；INT4 在旧卡上只能 CUDA Core 反量化，因此只省内存不涨吞吐；稀疏（2:4）可与量化叠加但主要用于 MoE；4-bit 原生指令只有 NVIDIA/AMD 两条线，TPU/CPU/NPU 仍是 INT8/BF16 上限。

---

**下一模块**：[04-NVIDIA GPU量化能力全谱](04-NVIDIA GPU量化能力全谱.md)　**返回总览**：[00-量化背后硬件约束知识体系总览](00-量化背后硬件约束知识体系总览.md)

## 参考来源

- [FP4 Quantization on Blackwell GPUs: Throughput, Cost — Spheron](https://www.spheron.network/blog/fp4-quantization-blackwell-gpu-cost/)
- [MicroMix: Efficient Mixed-Precision Quantization (ICLR 2026)](https://en.papernotes.org/ICLR2026/model_compression/micromix_efficient_mixed-precision_quantization_with_microscaling_formats_for_la/)
- [Accelerating LLM Inference: Fused INT8 Weight-Only Quantization in Pallas — Hugging Face](https://huggingface.co/blog/rishiraj/fused-int8-weight-only-quantization-in-pallas)
- [TFLOPS Gap: Why FP4 MoE Kernel Engineering Matters on Blackwell — Hugging Face](https://huggingface.co/blog/apsys/blackwell-nvfp4-comparison)
