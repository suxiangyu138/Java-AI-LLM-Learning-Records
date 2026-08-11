# NVIDIA 微架构演进：Volta 到 Rubin
> 每代架构的演进史，就是"矩阵引擎扩张 + 数据通路拓宽 + 晶体管翻倍"的重复叙事——本模块以 SM 结构与 Tensor Core 为主线，梳理 2017-2026 的七代关键变化。

## 1. Volta（V100，2017）：Tensor Core 元年

Volta 是分水岭：首次在 SM 中放入 **Tensor Core**（4 个/SM），FP16 矩阵算力 125 TFLOPS——比同代 FP32 高 8 倍，宣告"GPU 为矩阵而生"。SM 结构：64 个 FP32 核 + 4 Tensor Core + 4 调度器 + 256KB 寄存器堆 + 独立 L1/共享内存（128KB 合计）。Volta 还引入**独立线程调度**（独立线程程序计数器），终结了"warp 内线程必须同时分支"的旧约束——发散处理更灵活，但代价是调度电路复杂度上升。

## 2. Turing（T20，2018）：INT8 与 RT Core

Turing 补上 INT8 推理的拼图（Tensor Core 支持 INT8，算力再翻倍），并加入 RT Core 光追单元。SM 调整为 64 FP32 + 64 INT32 的混合结构——**首次允许 FP32 与 INT32 指令同周期双派发**（此前 INT 运算占用 FP32 单元）。对 AI 的影响：INT8 量化推理在 Turing 上成熟，GGUF/QAT 生态由此起步。

## 3. Ampere（A100，2020）：双派发定型与稀疏

Ampere 的 SM 结构沿用至今：**64 个 FP32/INT32 混合单元，每周期双派发**（128 个"核心"标称即由此而来）；Tensor Core 支持 TF32/BF16/FP16/INT8，并引入 **2:4 结构化稀疏**（矩阵算力翻倍）与硬件稀疏索引。A100 的 40/80GB HBM2e、NVLink 3.0（600GB/s）确立了 AI 数据中心卡的形态。Ampere 也是**统一内存/虚拟化（MIG）**的起点——GPU 切片成为云上商品。

## 4. Hopper（H100，2022）：Transformer 专项

Hopper 是"为 Transformer 设计"的一代：Tensor Core 升级为 **wgmma 指令族**（warpgroup 级 64×N×K 大矩阵乘），引入 **TMA 异步搬运引擎**（全局内存↔共享内存无寄存器中转）、**分布式共享内存（DSMEM）**（SM 间集群共享）与 DPX 动态规划单元。SM 规模：128 FP32 + 4 调度器 + 4 Tensor Core + 50MB L2，H100 的 FP8 算力 3,958 TFLOPS（稀疏）——相比 A100 的 INT8，训练精度损失更小、吞吐更高，FP8 混合精度训练由此成为主流。

## 5. Blackwell（B200/B300，2024-2026）：tcgen05 与 NVFP4

Blackwell 的变革集中在三个方向：

- **tcgen05 指令族 + TMEM**：MMA 结果直写 256KB TMEM（B300 40MB 全片），累加器脱离寄存器堆——矩阵流水连续化，是 FP4 吞吐跃升的结构性原因
- **NVFP4 原生支持**：B300 的 FP4 推理 15 PFLOPS，比 B200 提升 55-67%；sm_103a 架构专门为 B300 的 tcgen05 扩展指令
- **双晶粒设计**：208B 晶体管、两 die 通过约 10TB/s 片内互连封装；TMA 增强（2SM Load、warp 特化）；SFU EX2 加速 softmax 的指数运算（10.7 TeraExp/s）

Blackwell 也是**机柜级系统**的分水岭：GB200 NVL72 把 72 卡 + 36 CPU 塞进单一 NVLink 域（130TB/s 聚合），NVLink 5.0 达 1.8TB/s——GPU 的采购单位从"卡"变成"机柜"。

Blackwell 的算力-带宽比也达到新高：FP4 15 PFLOPS 对 8TB/s，比值约 1,875 FLOP/byte——意味着连 Blackwell 自己也喂不饱自己的 FP4 峰值，推理依旧带宽受限。这催生了 Blackwell 上的"省字节"设计合力：NVFP4 本身省一半带宽、2:4 稀疏再省一半、TMA 布局压缩、注意力路径的 EX2 加速减少中间量——每一代的算力提升都在倒逼更激进的数据通路改造。这也解释了 NVIDIA 为何把 HBM4（22TB/s）列为 Rubin 的第一优先——带宽决定 FP4 时代的真实性能。

## 6. Rubin（VR200，2026）：HBM4 与 MoE 专项

Rubin 于 CES 2026 发布、GTC 2026 详述，Q1 2026 进入量产：**TSMC N3P、336B 晶体管、双晶粒（NV-HBI）、224 SM、896 Tensor Core、288GB HBM4 22TB/s**。针对 2026 年负载的三项微架构创新：

- **K 维吞吐翻倍**：GEMM 的 K 迭代从 4 次减为 2 次——共享内存读回减半，矩阵搬运成本骤降
- **MoE 加速**：TMA 描述符管理优化 + 专家权重调度，配合 Counted Writes（设备发起的 NVLink 通信）降低同步延迟——MoE 训练/推理的 all-to-all 通信是 2026 最大的系统瓶颈
- **长上下文注意力**：中间激活以 2:4 稀疏加载 + 自适应压缩（第三代 Transformer Engine）；softmax 的 FP32 吞吐翻倍（BF16/FP16 为 GB300 的 2 倍）

配套的 Vera CPU（88 核 Arm、1.5TB LPDDR5X 经 C2C 1.8TB/s 与 GPU 统一寻址）与 NVLink 6（3.6TB/s）把 NVL72 机柜推到 260TB/s、3.6 EFLOPS FP4。Rubin Ultra（NVL576、1TB HBM4e）定于 2027——**每代晶体管 +60%、带宽 +2.8×、FP4 算力 +5× 的节奏仍在继续**。

七代的代际表还可以读出另一条规律：SM 结构从 Volta 到 Rubin 基本稳定（128 核 + 4 调度器 + 4 Tensor Core 的框架不变），变化集中在 Tensor Core 指令族、数据通路与精度支持——架构的"芯"二十年不变，"引擎"每代换新。对开发者而言这意味着：**CUDA 技能十年不过时，但必须每年跟进新的指令与工具链**（tcgen05、TMA、CUDA 12.x），旧内核换新卡不一定自动提速。

## 7. 路线图、护城河与风险

看完代际演进，值得停下来问三个问题。

**为什么 NVIDIA 能两年一代**：架构迭代的燃料是"工艺 × 面积 × 功耗"三者的同步扩张——208B→336B 晶体管、160→224 SM、HBM3e→HBM4。每一代都是成熟的增量工程（SM 结构、Tensor Core 指令族、互连协议向后兼容），而不是推倒重来——向后兼容让 CUDA 生态零迁移成本（Blackwell Ultra 的 sm_103a 编译目标是个例外插曲，需要显式指定，但幅度极小）。

**护城河到底在哪**：不是单卡算力，而是"硬件-软件协同"的节奏优势——tcgen05/TMA/2SM 这类机制发布时，cuBLAS、cuDNN、vLLM、FlashAttention 同步跟进；竞争对手的硬件差距在缩小，但软件生态的适配差距仍以年计。Rubin 的"第三代 Transformer Engine + MoE 专项"再次证明：NVIDIA 在让芯片为"上个月才出现的模型结构"做专项优化——这种软硬协同的飞轮是规格表上看不到的壁垒。

**风险与变数**：一是功耗逼近物理极限——2.3kW/卡、230kW/机柜，性能/瓦可能反超绝对性能成为第一指标，Rubin 宣称的"10× 每单位能量吞吐"正是对此的回应；二是 HBM 供应——全球 HBM 产能被提前预订一空，VR200 NVL72 单机柜的 HBM4 内存就占约 $2M 成本，供应链而非设计成为瓶颈；三是竞争——MI355X 已追平 GB200，UDNA 生态若成熟，NVLink 锁定效应将被削弱。路线图终点：Rubin Ultra（2027，NVL576/1TB HBM4e）与后续的 Feynman——"10×/2 年"的宣称节奏能否维持，取决于上述风险是否可控。

一个值得注意的稳定性：从 2006 年的 G80 到 2026 年的 Rubin，SIMT + 调度器 + 分层存储的基本盘二十年未变——因为"并发换延迟"在物理上依然是最优解。变化只发生在边缘：指令集（mma→tcgen05）、数据通路（TMA/TMEM）、精度（FP8/FP4）。理解"底盘不变、引擎升级"的结构，比追逐每一代规格更能预测未来：**下一个十年 SIMT 依然会是主架构，变的还是引擎**。

> 🎯 **核心要点**：七年七代的主线始终是"矩阵引擎扩张 × 数据通路拓宽"——Volta 发明 Tensor Core，Turing 补 INT8，Ampere 定型双派发与稀疏，Hopper 引入 TMA 与 wgmma，Blackwell 用 TMEM 与 NVFP4 解放矩阵流水，Rubin 用 HBM4 与 MoE 专项把机柜推到 260TB/s；看懂每代的 SM 结构与指令族变化，比背规格更能预测下一代架构的方向；而护城河在软硬协同的节奏，风险在功耗、HBM 供应与竞争三方。

---

**下一模块**：[09-AMD、Intel 与国产加速器架构](09-AMD、Intel与国产加速器架构.md)　**返回总览**：[00-GPU硬件架构知识体系总览](00-GPU硬件架构知识体系总览.md)

## 参考来源

- [NVIDIA B300（Blackwell Ultra）架构与性能 — server-parts.eu](https://www.server-parts.eu/post/nvidia-b300-gpu-blackwell-ultra-architecture)
- [NVIDIA Vera Rubin 架构首曝 — TechWeb](https://m.techweb.com.cn/article/2026-07-23/2977665.shtml)
- [Vera Rubin NVL72 规格拆解 — HashrateIndex](https://hashrateindex.com/blog/nvidia-vera-rubin-nvl72-specs-breakdown/)
- [Blackwell GPU Optimizations — DeepWiki](https://deepwiki.com/cfregly/ai-performance-engineering/3.2-blackwell-gpu-optimizations)
