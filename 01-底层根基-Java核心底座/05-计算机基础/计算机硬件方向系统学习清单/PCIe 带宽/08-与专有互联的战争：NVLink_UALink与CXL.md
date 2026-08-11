# 与专有互联的战争：NVLink、UALink 与 CXL
> PCIe 的带宽再翻倍，也挡不住专有互联的碾压：NVLink 6.0 单 GPU 3.6 TB/s，是 PCIe Gen6 x16 的 14 倍——2026 年的格局是"PCIe 管宿主、NVLink/UALink 管加速器、CXL 管内存"，看懂分工才能选对互连。

## 1. NVLink：NVIDIA 的带宽碾压与边界

NVLink 是 NVIDIA 的 GPU 直连互连：绕过 CPU、直连显存空间、低时延（GPU-GPU 约 1 µs，PCIe 经 CPU 的路径 4-8 µs）。带宽演进：

| 世代 | 架构 | 单 GPU 双向带宽 | 说明 |
|:---:|:---:|:---:|------|
| 3.0 | Ampere（A100/RTX 3090） | 600 GB/s（DGX）/112.5 GB/s（消费） | RTX 3090 是最后带 NVLink 的消费卡 |
| 4.0 | Hopper（H100） | 900 GB/s | 18 条链路 |
| 5.0 | Blackwell（B200） | 1,800 GB/s | 工作站 RTX PRO 6000 也有 |
| 6.0 | Rubin（R100，2026 H2） | **3,600 GB/s** | 36 条链路；Vera Rubin NVL72 全互联 72 GPU、织网总带宽 260 TB/s |

对照 PCIe：Gen6 x16 双向 256 GB/s——**NVLink 6.0 是其 14 倍**。这就是张量并行（每层都做 all-reduce）必须走 NVLink 的原因：70B 模型每次推理约 80 次 all-reduce 往返，PCIe 路径在长上下文下直接把吞吐腰斩；而在 NVLink 4.0 上 70B 的 all-reduce 仅 ~300ms，PCIe Gen5 则要 1 秒以上。

NVLink 的边界同样清晰：**专有、昂贵、只服务 NVIDIA 生态**——消费级 GPU 自 RTX 40 系起全部移除 NVLink（RTX 3090 之后无 NVLink 桥），工作站级 RTX PRO 6000（约 $6000+）才有。两张消费卡的张量并行要么走 PCIe（自动降速），要么用管道并行（层切分，对带宽不敏感，Gen4/5 x8 都够）。**PCIe 在"低预算多卡推理"里反而不可替代**：llama.cpp 等框架 2026 年起自动检测 NVLink 与 PCIe 并分别适配。

## 2. UALink：开放阵营的反击

UALink（Ultra Accelerator Link）联盟成立于 2024-10，成员 AMD、Intel、Google、Meta、微软、AWS 等（**NVIDIA 缺席**），目标是做"开放版 NVLink"：

- **UALink 1.0（2025-04）**：200 GT/s 每 lane，x4 配置最高 800 GB/s，子微秒时延（100-150 ns），单 pod 最多 1024 个加速器（NVLink 的 576 GPU 上限的两倍）
- **UALink 2.0（2026-04-07，一次发布四项规范）**：引入 **In-Network Compute**（把 all-reduce 等集合通信下沉到交换器内执行，省带宽省时延）、链路弹性与折叠（Link Resiliency/Folding）、基于 gNMI/YANG/SAI/Redfish 的可管理性规范 1.0、与 UCIe 3.0 兼容的 Chiplet 规范 1.0
- **硬件路线**：AMD MI 系列与 Intel Gaudi 2026/2027 带 UALink；Astera Labs、Broadcom 出 UALink 交换器；Upscale AI 的 SkyHammer 是首个 UALink 原生交换 ASIC（2026 Q4 出样）

诚实的差距：UALink 2.0 的 800 GB/s 只有 NVLink 5.0（1.8 TB/s）的一半不到，且 2.0 发布时 1.0 硬件还没出货——但它的开放性与设备数上限（1024 vs 576）是生态层面的筹码。2026 年的现实是"NVLink 生态先行、UALink 蓄势"，对自建集群的决策影响有限，对采购 AMD/Intel 加速器的新集群则值得关注。

## 3. CXL：内存池化的开放通道

CXL 复用 PCIe 物理层（Gen5 起），是三类里与 PCIe 关系最近的：

- **CXL 2.0/3.0（Gen5 物理层）**：内存扩展与池化，Type 2 设备（加速器 + 一致内存）走 CXL.io/CXL.cache，Type 3 纯内存设备走 CXL.mem
- **CXL 3.1（Gen6）**：2026 服务器平台支持，单链路带宽同 Gen6 x16（128 GB/s 单向）
- **CXL 4.0（2025-11，基于 Gen7）**：128 GT/s、x2 原生宽度、端口捆绑、4 级 retimer 支持多机架池化；控制器 2026 末、多机架部署 2027
- **规模信号**：Marvell Structera S 30260（2026-03）260-lane CXL 3.0 交换器、聚合 4 TB/s，为 8-GPU 服务器池化 16TB 内存（推理吞吐 4.8×、TTFT -82.7%）；ODCC-UALink 测试服务、CXL 合规插拔测试都在 2026 铺开

CXL 与 NVLink 的分工是 2026 年最重要的架构认知：**NVLink/UALink 管"加速器之间搬数据"（低时延、超带宽、小容量），CXL 管"扩大可寻址内存"（容量优先、带宽次之、开放标准）**——NVIDIA 自己都让 CXL 站台 KV Cache 外扩（CMX），因为 KV Cache 要的是容量，不是 GPU 之间的超低时延。

一个常被忽视的细节：CXL 的三种类型对应三种语义——CXL.io（I/O，等同 PCIe）、CXL.cache（缓存一致性）、CXL.mem（内存扩展）；Type 2 设备（GPU 等加速器）同时用三种，Type 3（内存扩展器）只用 CXL.mem。带宽规划的差别在于：**CXL.mem 流量走"内存语义"，没有 TLP 式请求-完成配对开销，大块效率比普通 PCIe 流量更高**——这也是 CXL 内存设备能压榨出接近链路上限的原因。

## 4. 2026 互连全景：分工、对比与选型

| 互连 | 定位 | 带宽（2026 顶配） | 时延 | 开放度 | 适用 |
|------|------|:---:|:---:|:---:|------|
| PCIe | 通用宿主互联（一切外设） | Gen7 x16 单向 256 GB/s | ~ns-µs | 开放 | 主机↔设备、存储、网络 |
| NVLink | NVIDIA GPU scale-up | 6.0 单 GPU 3.6 TB/s | ~1 µs | 专有 | NVIDIA 集群张量并行 |
| UALink | 开放加速器 scale-up | 2.0 x4 800 GB/s | 100-150 ns | 开放 | AMD/Intel 多卡互连 |
| CXL | 内存扩展与池化 | 4.0 128 GT/s | ~µs | 开放 | KV Cache 卸载、池化内存 |
| 以太网/UEC | 机架间/跨集群 | 800G-1.6T/链路 | µs-ms | 开放 | 数据并行、跨节点 |

选型决策树（2026）：

- **单机多 NVIDIA GPU 做训练/长上下文推理** → NVLink 优先，PCIe 只做宿主数据管道
- **AMD/Intel 加速器多卡** → UALink 1.0/2.0（2026-2027 硬件）+ PCIe/CXL 宿主
- **KV Cache 内存不够、要扩容量** → CXL 3.1/4.0 池化（Gen6/7 物理层），不是 NVLink 的战场
- **消费级/低预算双卡推理** → 只有 PCIe：层切分管道并行（带宽不敏感）+ Gen5 x8/x16 足够
- **一切"主机与外设"的通信** → 永远 PCIe——它是唯一保证兼容性的底座，专有互联都是它的增量

## 5. 十五年演进与生态经济学：NVLink 1.0 → 6.0

NVLink 的带宽史本身就是 GPU 集群的算力史：1.0（2016，P100，160 GB/s）首次让 GPU 绕过 CPU 直连；2.0（2017，V100，300 GB/s）伴随 DGX-1 定义"一台 AI 训练机"的形态；3.0（Ampere）600 GB/s；4.0（Hopper）900 GB/s；5.0（Blackwell）1,800 GB/s；6.0（Rubin，2026 H2）3,600 GB/s——**每两年翻倍，比 PCIe 的三年周期还快**，因为 NVLink 只为 GPU 一个用途服务：不需要向后兼容任何旧设备、不需要支撑存储与网络，物理层可以更激进（3.6 TB/s 的链路已经不是铜线能单独完成的了）。

生态经济学的逻辑同样清晰：NVLink 把"多卡性能"变成 NVIDIA 的护城河——卡买得越多，NVLink 价值越大，换阵营的迁移成本越高；UALink 的 1024 设备上限与开放标准，正是对准这条护城河开的火。2026 年的现实是：**训练生态（PyTorch/NCCL）深度优化 NVLink，开放阵营的软件栈（AMD RCCL 等）落后一代**——互联之争的不只是带宽，还有软件生态，这是采购 AMD/Intel 加速器时必须算进成本的"生态税"。

### 5.1 机架之间的流量不归 PCIe

分清"谁在什么距离上用谁"：NVLink/UALink 管**机箱内加速器**（<1m，TB/s 级），PCIe/CXL 管**加速器与宿主**（<0.5m，128-256 GB/s 级），InfiniBand/以太网（含 2026 年量产验证的 Ultra Ethernet）管**跨节点集群**（数米到数公里，800G 级）。三层各自独立演进、互不替代——所以一台 AI 服务器里同时存在三种互连：GPU 走 NVLink、卡与宿主走 PCIe/CXL、节点之间走 IB/UEC。**把跨节点瓶颈归罪于 PCIe，是把三层混为一谈的典型错误**。

### 5.2 InfiniBand、UEC 与以太网：跨节点战场的变局

跨节点的 scale-out 互连是第四股势力：InfiniBand（NVIDIA 主导，2026 年 NDR 400G、XDR 800G 在 HPC 与 AI 集群占据统治地位）与以太网（UEC，Ultra Ethernet，2026 年进入量产验证，目标是让以太网获得 RDMA 级性能）。与 PCIe 的关系：**PCIe 是节点内部的"最后一公里"**——跨节点流量在节点内部仍要过 PCIe（NIC ↔ 内存/GPU），所以节点内 PCIe 带宽不足会直接拖垮跨节点性能（1.6T NIC 必须 Gen6 x16 的理由）。选型判断：NVIDIA 集群里 IB 是生态标配；开放阵营 2026-2027 用 UEC 追赶，成本比 IB 低一个量级——但"PCIe 底座"两者都绕不开，这就是 PCIe 的地位：**它是所有互连的公共地基**。

## 6. 给决策者的三句话

把互连全景压缩成三条决策箴言：**第一，PCIe 是默认答案，专有互联是加分项**——只要场景没说清"必须 GPU 间 TB/s 级通信"，PCIe（+CXL）就是最优解，成本、兼容、人才都最省；**第二，带宽差距要换算成业务数字再决策**——NVLink 比 PCIe 快 14 倍，但如果业务是 4 卡以内推理（层切分 + KV Cache 分层），PCIe 方案的真实差距只有 10-20%，而成本差距是数量级；**第三，生态锁定是最贵的成本**——NVLink 的迁移成本、UALink 的软件成熟度、CXL 的开放红利，都要写进五年 TCO，而不是只看带宽表——带宽是技术指标，TCO 是商业决策，二者分开算才不容易被厂商话术带偏。三个判断做完，互连选型就完成了 80%——剩下的 20% 是等硬件上市、等软件适配。

**第四，版本号的领先不等于可用的领先**——UALink 2.0 发布于 1.0 硬件尚未出货之时、Gen7 规范先于硬件两年落地，规格纸面竞争总快于产品落地；采购以"能下单、能验收"为唯一标准，路线图只用来预判下一轮升级窗口——纸面领先与可用之间的差距，正是 2026 年互连市场最常踩的坑。

> 🎯 **核心要点**：2026 年互连格局是分工制——PCIe 管宿主与外设的通用通道（Gen7 x16 单向 256 GB/s），NVLink 管 NVIDIA GPU 直连（6.0 达 3.6 TB/s，14× PCIe Gen6），UALink 是开放阵营的反击（2.0 引入 In-Network Compute，但落后 NVLink 一代带宽），CXL 管内存池化（4.0 基于 Gen7、多机架）；选型口诀：**训练看 NVLink/UALink，容量看 CXL，兼容性永远靠 PCIe**。

---

**下一模块**：[09-软件与调试视角](09-软件与调试视角：测量_诊断与优化.md)　**返回总览**：[00-PCIe带宽知识体系总览](00-PCIe带宽知识体系总览.md)

## 参考来源

- [NVLink 官方规格（第 6 代 3.6 TB/s）— NVIDIA](https://www.nvidia.com/en-au/data-center/nvlink/)
- [UALink 2.0 四项新规范（In-Network Compute 等）— IT之家](https://www.ithome.com/0/936/785.htm)
- [UALink vs NVLink vs Ultra Ethernet：2026 scale-up 互连战争 — Agent Market Cap](https://agentmarketcap.ai/blog/2026/04/16/ualink-nvlink-ultra-ethernet-scale-up-interconnect-war-agent-inference)
- [CXL 4.0 带宽翻倍、多机架池化 — 至顶网](https://server.zhiding.cn/server/2025/1125/3174334.shtml)
- [双 GPU 本地 AI：NVLink vs PCIe 实际 tok/s（2026）— dev.to](https://dev.to/jovan_chan_9500711396d4e6/dual-gpu-for-local-ai-in-2026-nvlink-vs-pcie-bandwidth-and-real-toks-numbers-3h8a)
