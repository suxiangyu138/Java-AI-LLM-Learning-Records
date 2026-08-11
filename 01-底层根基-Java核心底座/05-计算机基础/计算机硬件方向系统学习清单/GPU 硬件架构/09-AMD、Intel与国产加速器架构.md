# AMD、Intel 与国产加速器架构
> NVIDIA 之外的世界同样在快速演进——AMD 用芯粒化与 MX 格式追平 GB200，Intel 押注高带宽与 Xe 路线，华为昇腾与国产芯片在受限条件下走出自己的架构。

## 1. AMD：CDNA4 MI355X——芯粒化的极限实践

AMD Instinct 的架构主线是 CDNA（计算专用），MI355X（2026 年在产）是 CDNA4（gfx950）的代表作，三组关键设计：

- **8 XCD 芯粒 + 2 IO 芯粒**：每颗计算芯粒约 110mm²（TSMC N3P），32 个活跃 CU——主动从 MI300X 的 38 CU 降到 32（2 的幂），简化张量分块与工作负载切分；IO 芯粒从 4 颗并为 2 颗，减少芯粒间穿越的延迟与功耗
- **每 CU 矩阵引擎翻倍**：没有增加 CU 数，而是把每 CU 的 FP8 吞吐从 4,096 提到 8,192 FLOPS/时钟——"选择性共享"策略只在功耗可控的格式间共享算术硬件，换来同面积下 FP8 算力 1.9×（约 5 PFLOPS 稠密）
- **大 LDS 与 MX 格式**：LDS 从 64KB 扩到 160KB/CU（带宽翻倍 + L1 直连通路），配合 MXFP6/MXFP4 支持——FP6 达 18.45 PFLOPS、FP4 更高，比 FP8 世代快最多 4×；288GB HBM3E 8TB/s，Infinity Fabric 芯粒间 1,075GB/s

MI355X 的 MLPerf 成绩（Llama 2 70B 93K tok/s）与"追平 GB200、LoRA 微调反超 10%"的成绩说明：**芯粒化 + 大片上存储 + MX 低精度**足以在特定负载上与 NVIDIA 竞争，且同 OAM 接口可原位替换 MI300X。需要注意成绩的成立条件：MLPerf 的 93K tok/s 是 FP8/FP4 与推理框架（vLLM 2× 优化）共同作用的结果——同一张卡的裸算力必须搭配成熟软件栈才兑现，这也是 AMD 持续投 ROCm 的原因。

## 2. AMD：UDNA 与 MI400——统一架构转向

AMD 已宣布把 CDNA（计算）与 RDNA（图形）统一为 **UDNA（Unified DNA）**，计划中的 MI400 系列（2026 年，CDNA5 继承者）采用 TSMC N2、432GB HBM4——延续每年一代的节奏，目标直指机柜级（AMD Helios 方案）。UDNA 的工程意义：单一编译器后端（ROCm）同时服务计算与图形，降低软件生态的分裂成本——这是 AMD 对 CUDA 护城河最有威胁的软件层面回应。

## 3. Intel：Xe 与 Gaudi——两条战线

Intel 的加速路线分两条：**Gaudi 系列**（Habana 遗产）主打"HBM + 以太网 + 便宜"，Gaudi 3 的 24 核矩阵引擎 + 128GB HBM2e，2026 年市场份额有限但以成本与开放生态（无 NVLink 锁定）吸引特定客户；**Xe 系列**（Arc/数据中心 Xe）继承 GPU 资产，主打媒体、图形与轻量 AI。两条线将在 Falcon Shores 之后收敛为统一"AI 加速器"平台——Intel 的问题从来不是架构选项，而是软件栈（oneAPI）与良率。

## 4. 华为昇腾：达芬奇架构——受限生态下的全栈方案

昇腾的 **达芬奇（Da Vinci）架构**与 NVIDIA 同构但独立发展：核心计算单元 AI Core 由三部分组成——**Cube 单元**（矩阵乘，支持 FP16/INT8/INT4，等价 Tensor Core）、**Vector 单元**（逐元素运算）、**Scalar 单元**（标量与控制流）；配套 AI CPU 处理异构控制。存储层次为 HBM → L2 → 每 AI Core 的 L1 Buffer（等价共享内存）→ 寄存器。910B 为 64GB HBM ~1.2TB/s，910C 双芯粒 128GB 提升到 ~2TB/s 级别。软件侧 CANN 对标 CUDA，Triton 已通过 triton-ascend 后端支持——**架构模仿 + 生态独立**是国产路线的一致选择，三单元分工与 NVIDIA 的"矩阵/向量/控制"三分法几乎一一对应，印证了同一设计空间在并行演进中收敛。

## 5. 国产其他玩家与格局

寒武纪思元（MLU）采用"云边端"三级架构，主打推理卡；海光 DCU 走类 GPGPU 路线（兼容 CUDA 语法）；摩尔线程、壁仞等新势力各有芯粒化方案。2026 年国产加速器的共性：**受制于先进工艺与 HBM 供应，普遍在 7nm 级别、带宽 2-4TB/s 区间**，靠"大显存、多芯粒、软件适配层"在受限生态中争取可用性——架构思想与 NVIDIA 同步（矩阵引擎、芯粒、低精度），但代际差距体现在工艺与互连上。

国产芯片的共性挑战在供应链而非架构：HBM 出口管制下，国产卡多用 LPDDR5 或国产 HBM 替代，带宽普遍落在 1-3TB/s——直接锁死推理吞吐的上限（Roofline 带宽受限区）。因此国产路线把优化重心放在"同带宽下榨吞吐"：更大的片上缓存、更激进的内核融合、量化优先的算子库。寒武纪的推理卡在成本敏感场景有价格优势，海光 DCU 靠兼容 CUDA 语法降低迁移成本——2026 年国产加速器的现实定位是"可用"而非"最优"，但生态适配速度在加快。

## 6. 跨阵营架构映射与软件生态

三家的硬件其实高度同构——NVIDIA SM ↔ AMD CU ↔ 昇腾 AI Core；Tensor Core ↔ 矩阵单元 ↔ Cube 单元；共享内存 ↔ LDS ↔ L1 Buffer；CUDA ↔ HIP ↔ CANN——背后是同一个设计空间的不同取舍。真正拉开差距的是软件栈：

- **CUDA**：十五年累积，vLLM/FlashAttention/Triton 全部首发 NVIDIA；2026 年 CUDA 12.x 对 Blackwell 的 tcgen05、TMA 特化支持是独占优势
- **ROCm/HIP**：语法级兼容 CUDA（HIP 可自动移植 CUDA 代码），2026 年在 vLLM 的 MI355X 优化中已追到可用程度；短板在调试工具与部分算子的性能悬崖
- **CANN**：昇腾全栈（算子库 + 图引擎闭环），Triton 后端在途；开发体验与调试链路仍是国产最大短板
- **oneAPI**：Intel 的统一抽象，2026 年存在感继续走低

架构细节上还有一处常被忽略的分野：AMD 的 RDNA 图形架构带 Infinity Cache（大容量片上 L3），而 CDNA 计算架构放弃大 L3，把面积直接给 HBM 接口与 LDS——"缓存是图形的事，计算要的是带宽"这一取舍，与 NVIDIA 计算卡（Blackwell 的 L2 仅约百 MB 级）如出一辙。Intel Gaudi 则走"HBM 管够 + 以太网互联"的差异化：128GB HBM 在同价位显存最大，配合低价与开放生态在推理成本敏感场景占一席。昇腾 910C 双芯粒把 HBM 翻倍到 128GB 级，训练/推理全栈自研（昇腾 + CANN + MindSpore），在国产大模型适配中被广泛采用。

选型结论：**硬件架构决定上限，软件生态决定下限**——同等硬件下，生态成熟的卡实际吞吐可能高出 30-50%；国产卡的真实价值场景是"数据不出境 + 供应链自主"，而非性能对标。

> 🎯 **核心要点**：AMD 用"8 芯粒 + 每 CU 矩阵翻倍 + 160KB LDS + MXFP4/6"在 2026 年追平 GB200，UDNA 统一架构转向与 MI400（N2/432GB HBM4）已排上日程；Intel 双线作战等待收敛；华为昇腾的达芬奇架构（Cube/Vector/Scalar 三单元）与国产各家共同特征是"架构同构、生态独立、工艺受限"；跨阵营看，硬件同构、软件分化——生态成熟度直接决定相同架构下的真实性能，读三家的核心是读"同样的矩阵引擎思想，在各自约束下长出的不同形态"。

---

**下一模块**：[10-性能模型、生产实践与选型](10-性能模型、生产实践与选型.md)　**返回总览**：[00-GPU硬件架构知识体系总览](00-GPU硬件架构知识体系总览.md)

## 参考来源

- [Inside the AMD Instinct MI355X（ISSCC 2026）— Tom's Hardware](https://www.tomshardware.com/tech-industry/semiconductors/inside-the-instinct-mi355x)
- [AMD MI355X CDNA4 占用率与架构 — AMD 开发者社区](https://devcommunity.amd.com/t/occupancy-math-on-the-amd-mi355x-gpu-cdna4-a-from-first-principles-guide/689)
- [AMD MI350 系列产品页 — AMD](https://www.amd.com/zh-tw/products/accelerators/instinct/mi350.html)
- [GPU 架构附录：昇腾达芬奇架构 — GitHub Triton 教程](https://github.com/Allen-C-Guan/Pytorch-Inductor-Tutorial/blob/main/Triton%20Introduction/triton_compiler_view_tutorial/appendix_c_gpu_architecture.md)
