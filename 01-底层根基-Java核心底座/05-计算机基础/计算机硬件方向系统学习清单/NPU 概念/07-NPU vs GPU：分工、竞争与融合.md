# NPU vs GPU：分工、竞争与融合
> 2026 年最热闹的架构问题：NPU 会不会取代 GPU？本模块用实测数据拆开"分工"与"竞争"两条线——常驻低功耗归 NPU、重计算归 GPU，但边界正在移动。

## 1. 分工的物理基础：功耗曲线决定战场

NPU 与 GPU 的分工首先是功耗曲线的分工。GPU 的效率区间在几十瓦以上（RTX 5090 达 575W），NPU 的效率区间在 0.1-5W——**端侧设备没有 GPU 级功耗余量**，所以：

- **常驻任务归 NPU**：唤醒词检测 ~0.1W、系统级 AI（实时字幕、照片增强、预测文本）2-5W——这些任务要求"永远在线"，用 GPU 意味着电池 4 小时见底
- **爆发任务归 GPU**：大模型生成、图像生成、游戏 AI——50-400W 区间 GPU 的绝对吞吐无可替代
- **云端亦然**：数据中心里 GPU 负责训练与大规模推理，NPU/ASIC 负责吞吐优先的推理（TPU、Trainium）
- **批大小的分野**：GPU 在 batch=1 的在线推理上效率一般（每请求都要全量算一遍、闲置算力浪费），NPU 的静态流水线对单请求流更友好——这也是"交互式 AI 助手"类负载在 2026 年普遍倾向端侧 NPU 或云端批处理优化而非 GPU 直怼的原因

有意思的对照：**NVIDIA RTX 50 系列（含 5090）没有集成 NPU**——NVIDIA 的判断是"GPU 的 Tensor Core 足以覆盖端侧 AI"，而 Intel/AMD/高通/Apple 全部押注 NPU。两种路线在 2026 年同时成立：GPU 强在"一个硬件覆盖所有 AI 负载"，NPU 强在"同一个负载用十分之一的功耗"。

## 2. 实测对照：QHexRT 的数字说明什么

高通 QHexRT 在 Snapdragon 8 Elite Gen-2 上对比 NPU 与 CPU（llama.cpp）跑 230M 模型，数字极具教学意义：

- **Prefill**：12,540 tok/s vs CPU 871 tok/s——14.4×，纯算力碾压（NPU 的 MAC 阵列 vs CPU 的 SIMD）
- **TTFT**：36ms（512 token 内恒定）vs 588ms——1/16，响应体验的量级差异
- **Decode**：172 tok/s vs CPU 250 tok/s——**CPU 反胜**，因为小模型 decode 是带宽受限，Oryon CPU 从 DDR 拿到的有效带宽超过 NPU 的搬运引擎

结论：**prefill 拼算力（NPU 赢）、decode 拼带宽（看谁拿得到内存）**——大模型（13B+）decode 转向计算受限后 NPU 才赢回。这个对照是 2026 年端侧推理优化的方法论基础：按阶段选引擎、按模型大小选策略，而不是"全上 NPU"——这也是"混合调度"（prefill 走 NPU、decode 按需切 CPU/GPU）这类工程方案的诞生土壤。

## 3. 竞争的另一面：GPU 的 Tensor Core 就是 NPU

严格说，现代 GPU 已经内置"NPU"——Tensor Core 就是矩阵专用引擎，Hopper/Blackwell 的 TMA、TMEM 与 NPU 的 DMA/SRAM 同构（见 `GPU 硬件架构/06`）。差异在三个维度：

- **通用性**：GPU 的 Tensor Core 被包在完整 GPU 里——同一块硅片还能跑图形、通用计算、数据搬运；NPU 是"只有矩阵和逐元素"的裸引擎
- **调度方式**：GPU 保留动态调度（warp 每周期被选择），NPU 完全静态（编译器排好一切）——静态调度省硬件但灵活性低
- **精度路线**：GPU 追 FP8/FP4 浮点（训练/推理通吃），NPU 追 INT8/INT4 整数（推理最优）——浮点 vs 整数的分野是 2026 年两条路线的分岔点

## 4. 融合的方向：谁在往谁那边靠

2026 年两个方向的融合迹象明显：

- **NPU 向 GPU 靠**：Transformer 负载多算子化（矩阵、逐元素、softmax、归一化混合），纯脉动阵列吃不消——2026 年端侧 NPU 普遍引入向量引擎与灵活调度，NPU 架构越来越像"低功耗迷你 GPU"
- **GPU 向 NPU 靠**：Blackwell 的 tcgen05/TMEM、静态内核调度（Rubin 的 tile-level 调度）——GPU 正在吸收 NPU 的"静态数据流"思想，把搬运与计算按编译器排定（见 `GPU 硬件架构/08`）

融合的终点可能是"同一套硬件架构 + 两种配置"：**云端用大 GPU/NPU 混合训练推理，端侧用集成的低功耗混合引擎**——架构之争最终由负载结构与功耗预算裁决，而非阵营。

融合的另一条线索在功耗管理：2026 年的 SoC 把 NPU/GPU/CPU 放进同一电源域做"异构功耗协同"——Windows 的电源调度器按负载类型动态开关 NPU/GPU 的时钟与电源域，让"常驻 AI"与"爆发 AI"共享同一热预算。这暗示架构融合的下一个战场在"功耗调度"而非"计算单元"：当引擎越来越像，决定胜负的是谁能把瓦数花在刀刃上。

## 5. 能效对比的完整账：口径对齐后的真相

把 NPU 与 GPU 的能效放在同一口径下：RTX 5090 的 INT8 稠密算力约 2,500 TOPS @ 575W ≈ 4.3 TOPS/W；A17 Pro NPU 35 TOPS @ 3.5W ≈ 10 TOPS/W（混合精度口径）；XDNA2 的 60 TOPS 在约 15W 功耗预算下 ≈ 4 TOPS/W（NPU 部分）。可见**峰值 TOPS/W 双方同量级（4-10）**——NPU 的真正优势不在"每瓦算力更高"，而在三处结构性差异：一是**负载比例**（NPU 把全部算力给 AI，GPU 要分给图形与通用计算）；二是**功耗下限**（GPU 无法在 5W 内运行，NPU 可以——这是"有没有"的区别而非"快不快"）；三是**空闲功耗**（常驻任务的 GPU 待机数十瓦，NPU 接近零）。所以"NPU 比 GPU 高效"的准确表述是：**0-5W 区间 NPU 是唯一选项；50W+ 区间 GPU 每瓦吞吐更高；5-50W 中间地带才是真实竞争区**——这正是边缘计算盒子（15-45W）选择最纠结的原因。工业检测、智慧零售的边缘设备常用 Jetson（GPU）或瑞芯微/算能（NPU）：同 30W 预算下，Jetson 的灵活性（跑任意模型）vs NPU 的吞吐（同模型快 2-3 倍）是典型取舍，2026 年的主流答案是混合：小模型 NPU 常驻 + 大模型按需 GPU。

## 6. 给工程师的决策框架

面对"用 NPU 还是 GPU"，2026 年的实用框架四问：

- **负载是否常驻**？永远在线 → NPU；按需爆发 → GPU
- **功耗预算多少**？5W 内 → NPU；50W+ → GPU；中间地带（边缘盒子 15-45W）→ 两者皆可，比 TOPS/W 与生态（Jetson 生态 vs NPU 工具链）
- **模型多大**？3B 以内端侧 → NPU；70B+ → 云端 GPU/TPU；中间（7-13B，2026 年底端侧可及）→ 看内存带宽
- **生态在哪**？CUDA 生态不可替代 → GPU；端侧系统集成 → NPU（软件栈绑定，见 08 篇）

四问之外的第五问是**成本结构**：端侧"免费算力"（NPU 已随 SoC 买下）vs 云侧"按 token 付费"——常驻功能用 NPU 边际成本为零，按需重负载才值得花云端费用。2026 年多数产品的成本模型正是"NPU 兜底 + 云端溢出"：本地能跑的不上云，本地跑不动的才付费。

> 🎯 **核心要点**：NPU 与 GPU 的分工由功耗曲线决定——常驻低功耗归 NPU、爆发重计算归 GPU，RTX 无 NPU 与端侧四强押注 NPU 两条路线并存；实测表明 prefill 拼算力、decode 拼带宽，按阶段选引擎才是正确姿势；Tensor Core 本质就是"GPU 里的 NPU"，2026 年两者双向融合——NPU 变通用、GPU 变静态，架构之争最终由负载与功耗裁决。

---

**下一模块**：[08-软件栈与编译部署](08-软件栈与编译部署.md)　**返回总览**：[00-NPU概念知识体系总览](00-NPU概念知识体系总览.md)

## 参考来源

- [QHexRT launch article: full-stack NPU inference（NPU vs CPU 实测）— HuggingFace](https://huggingface.co/runanywhere/lfm2_5_230m_HNPU/commit/cadbdbd37cc3065487a306fa8df14b86534484cf)
- [NPU vs GPU: What's the Difference and Which Wins for AI（2026）— Solidaitech](https://www.solidaitech.com/2026/05/npu-vs-gpu-explained-ai-chips-difference.html)
- [NPU Explained（功耗分工）— GuptaDeepak](https://guptadeepak.com/research/npu-neural-processing-unit-explained/)
- [On-Device AI Agents: 混合云-端架构（2026）— AgentMarketCap](https://agentmarketcap.ai/blog/2026/04/07/on-device-ai-agents-apple-intelligence-copilot-plus-hybrid-architecture)
