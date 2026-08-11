# 端侧 NPU 全景：手机与 AI PC 的算力地图
> 2026 年端侧 NPU 的战场从手机蔓延到 PC——Apple、高通、Intel、AMD 四家逐鹿，华为麒麟自成体系，Arm Ethos 覆盖中低端。本模块按阵营盘点架构与规格。

## 1. Apple Neural Engine：闭源生态的效率标杆

Apple 的 ANE 从 A11（2017）一路演进，架构不公开但性能稳定：**M5/A18 Pro 约 38 TOPS**，A17 Pro 约 35 TOPS @ 3.5W（约 10 TOPS/W 的能效标杆）。三个值得注意的设计选择：

- **专有精度口径**：Apple 的 TOPS 数字不是 INT8 口径，而是混合精度内部估计——与高通的 INT8 数字不可直接对比，这是 09 篇"口径陷阱"的活案例；坊间传闻 M6 世代将随 LPDDR6 把 ANE 拉到 60+ TOPS 并补齐公开口径，但截至 2026-08 仍以闭源估计为准
- **私有软件栈**：Core ML 是唯一入口，模型必须先转 Core ML 格式（编译器做算子映射与量化）——封闭换来了端到端质量，牺牲了生态开放性
- **混合架构**：Apple Intelligence 设备端决策 + Private Cloud Compute 云端兜底——路由决策在设备上完成，云端无状态、可验证，隐私承诺与 GDPR 合规构成产品卖点

## 2. Qualcomm Hexagon：80 TOPS 的性能冠军

高通 Hexagon NPU（与 Adreno GPU、Oryon CPU 共处 SoC）是 2026 年的性能领跑者：

- **Snapdragon X2 Elite / X2 Plus**：第六代 Hexagon（NPU6）**80 TOPS**，直指 $800 价位段 AI PC 甜点区；上一代 X Elite 为 45 TOPS
- **Snapdragon 8 Elite（手机）**：约 45 TOPS，搭配 Adreno 830
- **QHexRT 引擎**：纯 NPU 推理运行时（无 Python、无 CPU 回退），实测 230M 模型 prefill 达 12,540 tok/s（CPU 的 14.4×）、TTFT 恒定 36ms（CPU 的 1/16）——但 decode 反而输给 CPU（250 vs 172 tok/s），因为小模型 decode 是带宽受限、Oryon CPU 能拉到更多 DDR 带宽

Hexagon 的启示：**prefill 是算力游戏（NPU 碾压），decode 是带宽游戏（看谁拿得到内存带宽）**——80 TOPS 的卖点正确，但小模型 decode 并不买账；模型放大到 13B 后 decode 转向计算受限，NPU 的 80 TOPS 才真正兑现。

Hexagon 的架构沿革值得一提：它从 Hexagon DSP（数字信号处理器）演进而来——高通的 NPU 与 DSP 共享向量指令集遗产，这让它在音频与传感器 AI（DSP 传统地盘）上比对手更顺；2026 年 Hexagon 与 Adreno GPU 的分工是"小模型走 NPU、图像生成走 GPU"。这个"从 DSP 长出来"的谱系，与 Apple 从 GPU 团队长出的 ANE、Intel 从 CPU 指令集长出的 NPU，形成三种不同的演进路径——**端侧 NPU 的架构谱系决定其擅长场景**，选型时值得追溯。

## 3. Intel NPU 5 与 AMD XDNA2：AI PC 双雄

- **Intel**：Core Ultra Series 3（Panther Lake，Intel 18A 工艺 + 背面供电）集成 **NPU 5，50 TOPS**；CPU + GPU（Arc Xe）+ NPU 平台合计 170 TOPS，支持多模态任务并行（实时视频翻译、本地代码生成）不降频；上一代 Lunar Lake（Core Ultra 200H）为 48 TOPS
- **AMD**：XDNA2 架构，旗舰 Ryzen AI 9 HX 475 **60 TOPS**；XDNA 是 2D tile 阵列（AI Engine tile，VLIW 处理器 + 向量 MAC + L1/L2 SRAM + 显式 DMA），GEMM 实测效率达峰值 78.7%；Ryzen AI Max+ 395 对标 Apple M5 阵营

Intel 与 AMD 的共同点：**NPU 只是平台算力的三分之一**——CPU/GPU/NPU 三路协同（"异构调度"）才是 AI PC 的完整故事，单一 NPU 峰值不再是唯一卖点。

## 4. 华为麒麟与达芬奇：端侧算力先行者

麒麟 970（2017）是全球首款集成 NPU 的手机 SoC，达芬奇架构的 Cube 单元（矩阵）+ Vector + Scalar 三单元设计延续至今：**麒麟 9000 系列 NPU 约 15-30 TOPS 区间**（不同型号），配合自研 CANN 生态与鸿蒙 AI 框架。达芬奇架构与 `GPU 硬件架构/09` 的昇腾同源——**华为的端侧与云侧共用一套架构思想**，这是它与 Apple/高通最不同的地方：从芯片到框架到模型全栈自研。

## 5. Arm Ethos 与中低端市场

Arm 的 Ethos-U（微控制器级，数十 GOPS 级）与 Ethos-N（SoC 集成，数 TOPS 级）覆盖手机中低端与 IoT：没有自研 NPU 的厂商（联发科用自研 APU、展锐等用 Ethos）借此获得入门级 AI 能力。中低端市场的逻辑是"够用即可"：人脸解锁、降噪、轻量翻译不需要 40 TOPS——**40 TOPS 是 AI PC 的门槛，不是所有设备的需求**。

## 6. 手机 NPU 的细分战场

手机是 NPU 的原生市场，2026 年的配置分层：**旗舰**（骁龙 8 Elite 45 TOPS、A18 Pro 38 TOPS、天玑 9400 系列）承担端侧 LLM 与多模态；**中端**（骁龙 7 系、天玑 8 系，10-25 TOPS）承担翻译、图像增强；**入门**（Arm Ethos-U 级，<5 TOPS）只做唤醒词与传感器融合。手机 NPU 的三个特有挑战：一是**热约束**——无风扇，连续推理 5-10 分钟后降频，持续吞吐远低于峰值（这是 09 篇"持续 vs 瞬时"陷阱的手机版）；二是**与 ISP 的协同**——拍照 AI 处理链（多帧合成、夜景降噪）每年消耗的算力超过 LLM，影像是手机 NPU 的第一大负载；三是**合规驱动**——GDPR 之后厂商把更多处理留设备端，端侧算力需求被法律推着走。联发科天玑 9400 用自研 APU 与骁龙打对称战，麒麟 9010 在鸿蒙生态内闭环——**手机 NPU 的竞争已从"TOPS 之战"转向"场景体验之战"**：谁能把 LLM、影像、音频的端侧体验做顺，谁就赢下用户。

还有一个反直觉的结构性事实：2026 年大量轻薄本没有独显，NPU 是唯一的 AI 硬件——Intel/AMD 集成 GPU 的功耗预算有限，NPU 承担全部 AI 推理。这是 NPU 出货量（年数十亿颗）远超独立 GPU（数千万颗）的结构性原因之一：**AI 硬件的最大市场在集成端侧，不在数据中心**。

## 7. 端侧 NPU 的共性规律

纵览四家，端侧 NPU 三条共性：一是**全部集成进 SoC**（统一内存、共享电源域），独立 NPU 芯片在端侧不存在；二是**功耗预算 1-5W**（常驻任务 0.1-2W），能效比峰值算力更重要；三是**软件栈绑定**（Core ML/Qualcomm AI Engine/OpenVINO/ONNX Runtime），NPU 的真实性能高度依赖运行时适配——同一模型在四家 NPU 上的表现差异可能超过硬件规格差异。

第三条共性还有一层隐藏含义：**常驻模型的管理**。2026 年的系统级 AI 需要同时驻留语音（唤醒词 + STT）、视觉（人脸/场景）、语言（小 LLM）多个模型，NPU 驱动要支持多模型分区与优先级调度——Windows 的 NPU 分区调度与高通的 multi-model 支持就是为此而生。选型时"能同时驻留几个模型"比"单个模型多快"更贴近真实体验。另一个容易被忽略的维度是**驱动质量**：NPU 的性能一半由 vendor driver 决定（调度、热管理、多模型分区），同一 SoC 在驱动迭代前后的端侧 AI 体验差异可达 2 倍——选型报告应把"驱动成熟度"列为独立评估项。

> 🎯 **核心要点**：2026 年端侧 NPU 四强格局——Apple 38 TOPS 闭源标杆（专有口径不可比）、高通 80 TOPS 性能冠军（prefill 碾压但 decode 看带宽）、Intel 50/AMD 60 TOPS 三路协同平台、华为达芬奇全栈自研；共性规律是 SoC 集成、1-5W 预算、软件栈绑定——端侧 NPU 的选型永远是"芯片 × 软件栈 × 功耗"三维决策，TOPS 只是入场券。

---

**下一模块**：[06-云侧 NPU 与 TPU](06-云侧NPU与TPU.md)　**返回总览**：[00-NPU概念知识体系总览](00-NPU概念知识体系总览.md)

## 参考来源

- [80-TOPS NPUs Land: On-Device AI Agents Get Real in 2026 — dev.to](https://dev.to/rishi_kora/80-tops-npus-land-on-device-ai-agents-get-real-in-2026-50gk)
- [QHexRT launch article: full-stack NPU inference — HuggingFace](https://huggingface.co/runanywhere/lfm2_5_230m_HNPU/commit/cadbdbd37cc3065487a306fa8df14b86534484cf)
- [The Silicon Sovereignty: CES 2026（Panther Lake/18A）— Wedbush](https://investor.wedbush.com/wedbush/article/tokenring-2026-1-12-the-silicon-sovereignty-ces-2026-marks-the-death-of-the-novelty-ai-and-the-birth-of-the-agentic-pc)
- [NPU vs GPU: What's the Difference（2026）— Solidaitech](https://www.solidaitech.com/2026/05/npu-vs-gpu-explained-ai-chips-difference.html)
- [On-Device AI Agents: Apple Intelligence 混合架构 — AgentMarketCap](https://agentmarketcap.ai/blog/2026/04/07/on-device-ai-agents-apple-intelligence-copilot-plus-hybrid-architecture)
