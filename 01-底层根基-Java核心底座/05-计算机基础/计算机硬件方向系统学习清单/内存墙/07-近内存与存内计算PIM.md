# 近内存与存内计算（PIM）：把计算搬向数据
> 既然数据搬不动，就让计算搬家：在 DRAM/HBM 内部或旁边放逻辑单元，就地完成矩阵运算——2026 年 PIM 从论文走向样品，但离生产还有一段路。

## 1. PIM 的动机：搬运成本大于计算成本

前几篇反复出现一个数字：一次 DRAM 访问的机会成本约 500 次乘法。这意味着对于低算术强度的操作（element-wise、规约、KV 更新、梯度累积），**把数据搬出内存再算，不如在内存里顺手算掉**。PIM（Processing-in-Memory）正是为此而生：

- **存内计算（CIM）**：逻辑直接做进 DRAM 存储阵列，行内运算——适合布尔/加法类操作，主流方向之一（如 SK 海力士 AiM）
- **近内存计算（NMP）**：在内存堆叠的逻辑基片（base die）或中介层放计算单元——兼顾灵活性与带宽，2026 年主流工程方向
- **3D 堆叠 PIM**：HBM 的逻辑基片升级为先进工艺节点后，天然具备放 ALU/矩阵单元的空间——HBM4 的逻辑基片正是为此铺路

共同点：**让数据少走几毫米**。DRAM 到计算单元的几厘米走线，在高频下就是数十纳秒与数倍功耗。

## 2. 产业代表：HBM-PIM 与 AiM 的数据

- **Samsung HBM-PIM**：在 HBM 堆栈内每 bank 放置计算单元（DRAM 工艺内的处理元素），声称较传统方案性能提升 2×+、能耗降低 70%+
- **SK Hynix AiM（Accelerator-in-Memory）**：存内计算路径，面向规约/搜索/矩阵操作，同样声称 2×+ 性能与 70%+ 能耗节省
- **现实**：两者 2026 年仍处于早期样品/研究阶段——性能提升依赖负载形状（低算术强度、带宽主导），通用性有限，编程模型仍封闭

关键判断：**PIM 的数字"2×+/70%"是带宽主导负载的上限收益，不是通用加速**。把矩阵乘（高算术强度）塞进 PIM 毫无意义——它本身就是计算受限。

## 3. 近内存反量化：与量化的天然结合

PIM 与量化体系（姊妹篇）有一个完美的结合点——**近内存反量化**（StreamDQ 等 2026 方案）：把 4-bit 权重的反量化单元做进 HBM 堆叠内部，每伪通道（pseudo-channel）一个反量化块，数据出内存前先恢复精度：

- 计算单元只收到已反量化的数据流，片上资源全部留给矩阵乘
- 反量化吞吐随 HBM 通道数扩展，不挤占 GPU 资源
- 位宽越低，近内存单元负担越轻——**量化是 PIM 的最佳搭档**：量化负责省流量，PIM 负责把省下来的流量就近算掉

这解释了为什么 HBM4 的逻辑基片升级为先进节点（TSMC 5nm）——它不只是接口控制器，更是未来近内存计算单元的家。

### 3.1 PIM 的边界：为什么矩阵乘不适合搬进内存

判断负载是否适合 PIM，用 03 篇的算术强度框架即可：

- **适合**：算术强度低（<1 FLOP/byte）的操作——element-wise、规约（sum/max）、KV 更新、近内存反量化——搬运成本大于计算成本，就地算掉最划算
- **不适合**：算术强度高（>10 FLOP/byte）的矩阵乘——计算本身是瓶颈，放进内存只是换了个地方算，且内存计算单元（位宽窄、频率低）比 Tensor Core 慢得多
- **折中**：部分归约（partial reduction）——把矩阵乘的中间归约在内存里做，主计算仍在加速器

这个框架也解释了厂商数字的适用面：HBM-PIM/AiM 声称的"2×+ 性能"都是在**低算术强度负载**上的结果，直接套用到大模型矩阵乘是典型的"数字误用"。

## 4. zHBM：把内存从"旁边"搬到"正上方"

2026 年 8 月 Samsung 在 FMS 发布的 zHBM 是 PIM 思想的激进版本：**把 HBM 垂直堆叠在 AI 加速器正上方**（而非旁边的中介层），数据走行距离从厘米级缩到微米级：

- 声称较 HBM5 性能提升 4-8×、能效提升 3×、内存密度提升 10×（混合铜键合）
- 散热是最大工程挑战：内存与加速器热耦合，需要新的散热架构
- 与"近内存"的区别：不增加计算单元，只消灭搬运距离——从源头降低访问成本

zHBM 与 PIM 可以叠加：内存越近，近内存计算单元的收益越大。但两者 2026 年都处于样品/纸面阶段，量产路径未明。

### 4.1 zHBM 与 PIM 的工程现实差距

从纸面规格到量产，zHBM/PIM 还有三重现实差距：

- **制造**：混合键合（copper-to-copper）良率爬坡慢，垂直堆叠的热机械应力需要新封装验证——HBM4 的 16 层堆叠已让良率承压，垂直堆叠的工程复杂度更高
- **编程模型**：PIM 单元的指令与数据流需要新编译器/运行时支持——"写进内存里算"的抽象至今没有统一标准，厂商各自为政（Samsung/SK Hynix 两套 SDK）
- **负载适配**：收益依赖负载的算术强度与数据布局，通用加速的期望落空——2026 年实际部署的 PIM 用例仍是"特定内核 + 特定场景"

务实判断：**PIM/zHBM 是 2027-2028 的期权，不是 2026 的选项**——现在可落地的近数据方案是"近内存反量化"（StreamDQ 类）与 CXL 池化，两者成熟度与 ROI 都更明确。跟踪 PIM 进展的观察点：HBM4 逻辑基片的算力密度、混合键合良率、是否有主流推理引擎（vLLM/TRT-LLM）接入 PIM 内核——三者齐备之前，投入产出比不值得押注。

## 5. 光子内存与更远的未来

Marvell/Celestial AI 等推动的光子内存走完全不同的路：用光互连做机架级内存池，让"内存墙"退化为"光速与交换机延迟"问题——2026 年定位是下一代（机架级）方案。与电互连的对比：

- 电：距离受限（<1m 内带宽才可观）、串扰随频率恶化
- 光：距离免疫（机架级 10-100m 带宽不衰减）、需要光电转换（成本与功耗）

光子不替代 HBM/PIM，而是把 CXL 池化的"机架级内存"扩展为"数据中心级内存"——同一面墙的又一种绕法。

> 🎯 **核心要点**：PIM 的动机是"搬运成本（约 500 次乘法）大于计算成本"，适用低算术强度负载；HBM-PIM/AiM 声称 2×+ 性能与 70%+ 省电，但 2026 年仍是样品、收益依赖负载形状；近内存反量化（StreamDQ）与量化体系天然互补——量化省流量、PIM 就近算掉；zHBM 把内存搬到加速器正上方消灭搬运距离（声称 4-8×）；光子内存解决机架级距离问题；PIM 的价值定位是"带宽受限负载的最后一公里优化"，不是通用加速。

---

**下一模块**：[08-CXL与内存池化](08-CXL与内存池化.md)　**返回总览**：[00-内存墙知识体系总览](00-内存墙知识体系总览.md)

## 参考来源

- [A Review of Near-Memory Computing Architectures — IEEE](https://ieeexplore.ieee.org/document/8491877/references)
- [Samsung Attacks the AI Memory Wall with 3D Packaging — HPCwire](https://www.hpcwire.com/bigdatawire/2026/08/10/samsung-attacks-the-ai-memory-wall-with-3d-packaging/)
- [StreamDQ: Near-Memory Weight DeQuantization in Custom HBM (2026)](https://arxiv-org.ezproxy.obspm.fr/html/2607.08993v1)
- [Scaling the Memory Wall: HBM, CXL, and the New GPU Playbook — Data Center Knowledge](https://www.datacenterknowledge.com/data-center-hardware/scaling-the-memory-wall-hbm-cxl-and-the-new-gpu-playbook)
- [Memory Becoming Chip Industry's Next Bottleneck — EE Times Asia](https://www.eetasia.com/memory-becoming-chip-industrys-next-bottleneck-amid-strong-ai-demand/)
