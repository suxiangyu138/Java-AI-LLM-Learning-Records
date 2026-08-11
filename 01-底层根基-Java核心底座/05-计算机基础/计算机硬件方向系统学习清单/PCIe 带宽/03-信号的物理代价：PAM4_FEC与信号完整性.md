# 信号的物理代价：PAM4、FEC 与信号完整性
> 每三年速率翻倍不是白来的：Gen6 起改用 PAM4 四电平调制，眼高损失约 9.6 dB，FEC 补可靠性，Retimer 救距离——128 GT/s 时代铜缆逼近物理极限，2026 年 PCI-SIG 正式把"光学"写进 PCIe 演进路线。

## 1. 从 NRZ 到 PAM4：两个比特换一个符号

Gen1-5 全部使用 NRZ（不归零）调制：一个符号只有高低两个电平，每符号携带 1 比特。Gen6 把速率翻到 64 GT/s 时，两条路摆在面前——继续提高符号速率（信号会烂到无法恢复），或者**在同一符号速率下多塞比特**。后者就是 PAM4：四个电平（00/01/10/11），每符号携带 2 比特，符号速率只需 32 GT/s 就能达到 64 GT/s 的数据率。

数学上很划算，物理上代价沉重：四个电平被压在同一个信号摆幅里，相邻电平间距只有 NRZ 的三分之一。用眼图（eye diagram）的语言说，**PAM4 的眼高比 NRZ 损失约 9.6 dB**——而 9.6 dB 恰好是两倍电压摆幅的压缩量。眼高就是抗噪余量，余量缩小意味着：同样长度、同样质量的通道，PAM4 更容易误码。

这就是 Gen6/Gen7 必须引入三重补偿的原因：

- **FEC（前向纠错）**：Gen6 的 FLIT 内嵌 Reed-Solomon RS(242,236) 编码——242 字节中 236 字节数据 + 6 字节校验，可以纠正 flit 内任一符号错误。纠错在 PHY 层完成，对上层透明，代价是约 2 ns 的 FEC 时延
- **链路层重传降为兜底**：CRC 校验发现 FEC 纠不了的错误才走重传。RS(242,236) 的设计目标是让重传概率降到几乎为零——PCIe 6.0 的设计目标是把重传带宽损失压到 ~0.05%
- **信号质量预算收紧**：CXL 4.0 规格给链路定的通道损耗预算只有 -36 dB——这是工程上"多长 PCB 走线、多少个连接器"的硬约束

## 2. FLIT：时延、可靠性与效率的三方妥协

FLIT（Flow Control Unit，流量控制单元）是 Gen6 的架构核心，256 字节固定长度：242 字节数据 + 8 字节 CRC + 6 字节 FEC。它取代了 Gen1-5 的"TLP 流 + 独立 DLLP 流"结构，把链路层管理（流控、确认、FEC）全部收敛进固定单元。

FLIT 的三重收益：

- **编码开销归零**：不再有 128b/130b 的映射层，传输单元就是数据单元，2.5% 的编码损失消失
- **重传损失极小**：固定单元 + FEC，链路重传概率按 ~0.05% 设计，几乎不影响有效带宽
- **确定性时延**：固定长度意味着 PHY 层处理时间可预测，这对 AI 集群里的多 GPU 同步（all-reduce 等）至关重要

代价也要说清楚：**Gen6 相比 Gen5 的 PHY 时延增加不到 10 ns**（发射 + 接收，含 FEC 约 2 ns）——与以太网类 PAM4 动辄 100+ ns 的 FEC 时延相比，PCIe 因为时延敏感型负载（负载-存储语义）约束，把 FEC 设计得极轻。这也是 PCIe 与"网络思维"的本质分歧点：网络可以容忍毫秒级时延，PCIe 不行。

### 2.1 可靠性的完整链条

PCIe 的数据可靠性是端到端硬件保证的，不依赖软件重试：

```text
发送端：数据 → CRC 计算 → FEC 编码 → 物理发送
链路：   校验错误 → FEC 纠错（可纠正）→ 静默通过
接收端：FEC 不可纠 → CRC 不匹配 → 链路层重传（TLP 序号追踪）
```

这条链路的工程意义：**设备驱动和软件栈永远不需要"重发"逻辑**——这是 PCIe 与 RDMA 网络（要软件参与可靠性协商）的本质区别，也是它保持低时延的关键。代价是链路层要维持 TLP 序号与重传缓冲，复杂度内化在 PHY/数据链路层。

## 3. Retimer 与距离困境：128 GT/s 的铜缆能走多远

信号速率越高，通道损耗越凶——损耗以每单位长度 dB 计，随频率上升。128 GT/s 的奈奎斯特频率达到 32 GHz，PCB 走线每英寸损耗数 dB，两个连接器（插槽 + 金手指）再加几 dB，-36 dB 预算很快耗尽。

**Retimer**（中继器）是答案：它不是一个简单放大器（那样只会把噪声一起放大），而是完整的"接收 → CDR 重新时钟 → 重新发射"芯片，把信号在中间"重生"一次。每个 retimer 让链路多走一段距离，代价是约 6-10 ns 时延和若干瓦功耗。2026 年 Gen6 服务器几乎必然带 retimer——主板设计把 retimer 放在 CPU 与插槽之间，这就是"PCIe 6.0 Ready"主板比 Gen5 主板贵、功耗高的直接原因。

距离的现实约束（2026 基准）：

- **机箱内**（CPU ↔ 插槽，<0.5m）：Gen5 无 retimer 可行，Gen6 一般需要，Gen7 几乎必须
- **机箱间/机架间**（1-10m）：铜缆在 Gen7 已基本不可行——这就是 2026 年 PCI-SIG 把光学写进路线图的直接动机

## 4. 2026 里程碑：光学 Retimer 与铜缆延长

2026 年 PCIe 物理层有两条并行路线，都指向"带宽的最后一公里"：

**光学（Optical Aware Retimer ECN）**：2026 年 PCI-SIG DevCon（台湾）发布的规范修订，核心是"电气-光学-电气（E-O-E）"模型——标准 PCIe 电信号进入 retimer，转换为光信号传输，远端再恢复成标准电接口。宿主和端点看到的都是标准 PCIe PHY，中间的"光纤种类、调制方式、连接器"全部实现自由。PCI-SIG 刻意不标准化光学细节，保留厂商在时延、功耗、成本上的差异化空间——这是 PCIe 历史上第一次官方允许光学链路，目标是 AI 数据中心机架到机架的连接（GPU 机架功耗已超 100kW，Stargate 项目规划 1.2GW，铜缆在功耗与距离上双双触顶）。

**铜缆延长（Linear Equalizer）**：2026 年 3 月，MACOM 发布业界首批 PCIe 7.0 线性均衡器（MAEQ-39964/39966），用被动均衡技术延长铜缆距离——128 GT/s 下的 Active Copper Cable（有源铜缆）方案，支持 2.5 NRZ 到 128 Gbps PAM4，兼容 Gen7/Gen6 和 CXL。与 retimer 相比，线性均衡器功耗更低、时延更小（不做 CDR），适合对时延极敏感的短距离场景。

两条路线的关系不是二选一：**铜缆延长保住机箱内与机箱间的低成本，光学解决机架间与功耗问题**。2026 年 8 月的实测工具已就绪（Teledyne LeCroy Summit M616 协议分析仪宣布 PCIe 7.0 Ready，Anritsu MP1900A 支持 128 GT/s PAM4 注入抖动噪声测试），但光学规范本身（连接器、端面标准）尚未完全定稿，厂商仍在自建测试环境。

## 5. 信号完整性工程：损耗、抖动与板材

128 GT/s 的工程现实藏在三组数字里：

- **损耗预算**：-36 dB（CXL 4.0 的链路预算）大致是"主板走线（10-15 cm）+ 两个连接器 + 插槽金手指"的组合。走线越长、板材等级越低，预算耗尽越快——Gen7 主板的 PCB 普遍升级到低损耗材料（MegaTron6 类），成本直接体现在主板定价；机架间的 -36 dB 在铜缆上完全不够用，这就是光学路线（第 4 节）的物理必然
- **抖动与串扰**：速率翻倍让每个符号时长减半，同样的绝对抖动占符号比例翻倍；PAM4 的四电平间距又压缩三分之一的抗噪余量——两项叠加就是 9.6 dB 眼高损失（第 1 节）。Gen6/7 的信号设计从"走线就行"变成"SI 仿真驱动"：板材叠层、过孔残桩、连接器选型全部要仿真验证
- **Retimer 的代价明细**：每个 retimer 重定时 +6-10 ns 时延、约 2-3W 功耗、需要独立时钟域管理——Gen6 服务器主板普遍 2-4 颗，这是"Gen6 主板更贵"的物理账单，也是延迟敏感负载（同步屏障）要审视 retimer 链路的理由

工程启示：**信号完整性问题在量产阶段不可修复**——只能靠预设计与测试（眼图、误码率、抖动注入）把关。这就是 PCIe 7.0 协议分析仪（Teledyne LeCroy Summit M616）在 2026 年 8 月就绪的意义：硬件未到，测试先到，厂商把验证窗口前置，避免"硅片到手才发现信号不通"。

> 🎯 **核心要点**：Gen6 起的带宽翻倍建立在物理层的大手术上——PAM4 用 9.6 dB 眼高换 2× 比特密度，FEC（RS(242,236)，~2ns）与链路重传（~0.05% 损失）补可靠性，FLIT 让编码开销归零；代价是 -36 dB 损耗预算、retimer 成为 Gen6+ 标配（+6-10ns 时延）；128 GT/s 时代铜缆在机架间触顶，2026 年光学 Retimer ECN（E-O-E 模型）正式写入规范，铜缆延长与光学并存到 Gen8。

---

**下一模块**：[04-标称带宽 vs 实际带宽](04-标称带宽vs实际带宽：TLP_MPS与DMA开销.md)　**返回总览**：[00-PCIe带宽知识体系总览](00-PCIe带宽知识体系总览.md)

## 参考来源

- [PCIe 7.0：光互连与 Retimer 架构 — EE Times 台湾](https://www.eettaiwan.com/20260224nt11-pcie-advances-into-optical-interconnects-with-a-retimer-based-architecture/)
- [MACOM 首发 PCIe 7.0 线性均衡器 — Nasdaq/MACOM](https://www.nasdaq.com/press-release/macom-introduces-industrys-first-pcier-70-linear-equalizers-extending-reach-copper)
- [CXL 4.0 规格网络研讨会 Q&A（128 GT/s、-36dB、retimer）— Compute Express Link](https://computeexpresslink.org/blog/introducing-the-cxl-4-0-specification-webinar-qa-recap-4386/)
- [PCIe 6.0 性能与功耗效率 — SemiEngineering](https://semiengineering.com/delivering-breakthrough-performance-and-power-efficiency-with-pcie-6/)
- [PCIe 7.0 设备测试方案（Anritsu）— Anritsu](https://www.anritsu.com/zh-cn/test-measurement/solutions/high-speed-digital-system/peripheral-component-interconnect-express-pcie/pcie7)
