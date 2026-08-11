# NVIDIA GPU 量化能力全谱：Turing 到 Blackwell
> 用 compute capability 一表看懂每代 NVIDIA GPU 原生支持的量化精度，以及 2026 年 Blackwell FP4 的实测性能真相。

## 1. 以 compute capability 为坐标的量化能力地图

NVIDIA 用 compute capability（计算能力）标识微架构代际，量化精度是它最直观的分界线：

| 计算能力 | 微架构 | 代表卡 | 原生量化支持 | 4-bit 能力 |
|:---:|------|------|------------|-----------|
| 7.5 | Turing | RTX 20/T4 | INT8 W8A8 | GPTQ/AWQ W4A16（软件） |
| 8.0 | Ampere | A100/A10 | INT8、BF16 | W4A16（软件） |
| 8.6/8.9 | Ampere/Ada | RTX 30/40 | INT8、BF16 | W4A16（软件） |
| 9.0 | Hopper | H100/H200 | FP8 E4M3/E5M2 | W4A8 FP8 权重（部分） |
| 10.0 | Blackwell | B200/B300/RTX 5090 | FP8、NVFP4/MXFP4 | 原生 FP4 Tensor Core |

要点：**INT4 从未有过原生 Tensor Core 指令**（7.5 到 9.0 全为软件路径）；FP8 从 Hopper 起步且向后不兼容 Ampere；FP4 是 Blackwell 独占。判断一块卡能跑什么量化，先看 compute capability，再看指令集，别信框架层的"支持"字样——vLLM 能在 H100 上加载 NVFP4 检查点，但那是软件回退，无加速。

### 1.1 消费级与工作站卡的分层

数据中心卡之外，消费级/工作站市场同样按代际划分量化能力：

- **RTX 20（Turing）**：INT8 原生，4-bit 软件路径——存量推理卡的代表
- **RTX 30（Ampere）**：INT8/BF16，性价比最高的小模型推理卡
- **RTX 40（Ada）**：同上，带宽提升明显（GDDR6X）
- **RTX 50（Blackwell 消费版）**：**FP4 Tensor Core 下放消费级**，RTX 5090 的 FP4 密度约为同代 FP8 的 2 倍——本地跑 70B INT4/MXFP4 级模型成为可能

消费级与数据中心的差异不在指令集，而在**显存带宽与容量**：RTX 5090 约 1.8 TB/s、32 GB 显存，相比 B200 的 8 TB/s/192 GB 差一个量级——FP4 能力相同，但带宽墙先到。本地部署选型时，显存容量（装得下）与带宽（跑得快）比 FP4 标称密度更关键。

## 2. Blackwell 的 FP4 硬件规格

FP4 是 Blackwell 的核心卖点，密度数字如下（均为稀疏/稠密并列）：

- **B200**：FP4 稠密约 9 PFLOPS，稀疏约 18 PFLOPS；192 GB HBM3e
- **B300（Blackwell Ultra）**：FP4 稠密约 14-15 PFLOPS（比 B200 高 55-67%）；288 GB HBM3e
- **GB300 NVL72 整机柜**：FP4 稠密 1.08 ExaFLOPS、稀疏 1.44 ExaFLOPS
- **RTX 5090**：消费级 Blackwell，同样具备 FP4 Tensor Core
- **DGX B300（8 卡）**：FP4 总计 144 PFLOPS，单机柜级部署单元

FP4 密度约为 FP8 的 2 倍、FP16 的 4 倍——这是 Blackwell 卖"每 token 成本减半"叙事的硬件基础。

### 2.3 H200 的定位：Hopper 最后的带宽王

H200 常被忽略，但它是量化部署的重要选项：与 H100 相同的 FP8 指令集，但 HBM3e 把带宽从 3.35 提到 4.8 TB/s、容量提到 141 GB。对带宽受限的量化推理而言，**H200 的"等效带宽"高于同指令集的 H100，实际吞吐提升约 40%**——它证明了一个量化部署的通用规律：带宽与容量常常比算力更值钱，同代际里优先加带宽而非加 FLOPs。

### 2.1 NVFP4 的格式细节：不是简单的 4 位浮点

NVFP4 与通用 FP4 的差别在缩放机制上，值得单独拆开：

- 权重按 **每 16 个元素一个 E8M0 缩放** 存储（block scale），激活按每 32 元素共享
- 权重用 16 元素块缩放，比 MXFP4 的 32 元素块更细——异常值控制更好，代价是 scale 存储开销略高
- 检查点层面与 MXFP4 基本互换（同是 E2M1 载荷），但 NVIDIA 的 block scale 布局有自己的打包顺序，跨框架迁移需要重新打包

这意味着"FP4"不是一个值，而是一套"载荷格式 + 缩放布局 + 打包顺序"的协议。**两个 FP4 检查点看起来一样，跑起来可能完全不兼容**——部署前先确认引擎与检查点的缩放约定一致（NVFP4 vs MXFP4 vs 自定义 FP4）。

### 2.2 标称数字的口径陷阱：dense 与 sparse 要分清

B200 "FP4 18 PFLOPS"与"FP4 9 PFLOPS"两个数字并存，差别是稀疏（sparse，2:4 结构稀疏）与稠密（dense）——**所有宣传材料默认报稀疏值**，因为数字更大。对照方法：任何厂商标称数字先问三个问题——是不是稀疏？是不是 FP4 还是 FP8？是不是峰值而非可持续（sustained）？

实际部署参考：B200 的 FP4 可持续吞吐（实测 MoE 推理）约 1000-1260 TFLOPS，仅为标称稠密的 11-14%。规划容量时用实测值，标称值只用于"代数比较"（谁比谁快 2 倍这种量级判断）。

**换算口诀**：同一卡上，稀疏 = 稠密 × 2，FP8 = FP16 × 2，FP4 = FP8 × 2——四个数字其实只有一个信息，其余全是倍数关系。记住这个换算，就不会被宣传页的多个 PFLOPS 数字绕晕。

## 3. 实测真相：峰值 vs 可达吞吐差距巨大

峰值密度是纸面数字，实测才见真章。Hugging Face 团队在 B200 上跑 GPT-OSS-20B（32 专家 MoE，batch 4096）的 FP4 实测：

- SGLang FP4：约 993-1262 TFLOPS（较 BF16 快 4.12 倍）
- FlashInfer FP4：约 1132 TFLOPS（快 4.69 倍）
- vLLM FP4：约 968 TFLOPS

对比 B200 标称的 9000 PFLOPS 级 FP4 峰值，实测只有 11-14%——差距来自 MoE 内核调度、TMA 搬运、CUTLASS schedule 等工程细节。**不同引擎之间能有 145 TFLOPS 的落差**，这说明 FP4 时代内核工程能力比硬件选型更能拉开差距。

## 4. 老卡上的 FP4：省内存不加速

NVFP4 检查点可以在 H100/A100 上加载（vLLM 支持），Marlin 系列内核也能跑 FP4 GEMM，但**没有 FP4 Tensor Core 就等于没有加速**：省了显存（4× 压缩），吞吐和 FP16 相同甚至略慢（反量化开销）。典型场景是把大模型塞进显存不够的卡：例如 70B 模型 FP16 需 140 GB，INT4 只需 ~35 GB，A100 80GB 单卡就能装下——**容量换吞吐，这是旧卡上 4-bit 量化的真实定位**。

### 3.1 模型实测：B200 与 B300 的胜负因模型而异

2026 年第三方基准（InferenceX/SemiAnalysis，FP4 精度）显示，B300 对 B200 的优势不是单调的：

- **Qwen 3.5 397B-A17B**：B200 反而胜出（90 tok/s/user 档位，B200 约 9634 vs GB300 约 6025 tok/s/GPU，且每 token 便宜约 50%）——小专家数模型吃不满 B300 的带宽
- **DeepSeek V4 Pro 1.6T**：GB300 全面占优（68 tok/s/user 档位，9759 vs 3177 tok/s/GPU）——超大 MoE 的专家并行吃满带宽
- **MiniMax M2.5/M2.7**：GB300 高 5-12%

结论：**FP4 峰值密度高不等于每 token 快**——MoE 的专家调度、KV 广播、all-to-all 通信决定了实际吞吐。选 B300 还是 B200 要按目标模型实测，不能按标称 PFLOPS 排序。这与 05 篇"AMD 与 NVIDIA 的胜负同样依赖模型"形成一致的方法论：**模型 × 引擎 × 硬件三维实测**。

## 5. 推理引擎与硬件的绑定关系

- **TensorRT-LLM**：Blackwell 上 FP4 吞吐的推荐路径，FP4 引擎**必须在真实 Blackwell 硬件上构建**（SM100 目标，不能从 H100/A100 交叉编译）
- **vLLM**：FP8/INT8/MXFP4/NVFP4 全支持；MoE 模型 FP4 需设置 `VLLM_USE_FLASHINFER_MOE_FP4=1`
- **SGLang/FlashInfer**：MoE FP4 内核工程领先，DeepSeek 规模（256 专家）下优势明显

### 5.1 引擎与硬件的三重绑定规则

- **架构绑定**：FP4 引擎的 SASS 指令面向 SM100 生成，跨架构（Hopper/Ampere）无法运行——引擎构建与目标卡必须同代
- **量化器绑定**：同一检查点用不同量化器（TensorRT Model Optimizer vs llm-compressor vs 自研）产出的 scale 布局不同，引擎对量化元数据（block size、对称性）有强假设
- **版本绑定**：TensorRT-LLM 的 FP4 支持（0.15+）仍标 beta，内核与打包格式随版本演进，检查点与引擎版本需配套记录

生产上建议把"量化器版本 + 引擎版本 + 硬件型号 + scale 布局"写进模型制品元数据——FP4 时代的部署事故有一半来自这三者错配。

> 🎯 **核心要点**：量化精度跟着 compute capability 走：INT8 全世代可用、FP8 需 Hopper、FP4 仅 Blackwell；B200 标称 9-18 PFLOPS FP4，但实测内核工程好的引擎也只能到 ~1200 TFLOPS，引擎间差 15%——FP4 时代的竞争力在 kernel 而不在纸面密度；老卡加载 FP4 只省显存，本质是容量换吞吐。

---

**下一模块**：[05-AMD Intel 国产加速卡的量化支持](05-AMD Intel 国产加速卡的量化支持.md)　**返回总览**：[00-量化背后硬件约束知识体系总览](00-量化背后硬件约束知识体系总览.md)

## 参考来源

- [TFLOPS Gap: Why FP4 MoE Kernel Engineering Matters on Blackwell — Hugging Face](https://huggingface.co/blog/apsys/blackwell-nvfp4-comparison)
- [NVIDIA Blackwell GPU Comparison: B200, B300, GB200, GB300 — AxeCompute](https://axecompute.com/nvidia-blackwell-gpu-comparison/)
- [FP4 Quantization on Blackwell GPUs — Spheron](https://www.spheron.network/blog/fp4-quantization-blackwell-gpu-cost/)
- [NVIDIA B300 vs B200 for AI Inference (2026) — Spheron](https://www.spheron.network/blog/b300-vs-b200-inference-cost-per-token/)
- [vLLM Quantization Supported Hardware](https://docs.vllm.ai/en/v0.10.0/features/quantization/supported_hardware.html)
