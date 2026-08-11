# 特权架构与虚拟化：M/S/U 三层与 H 扩展
> 操作系统安全模型的地基是特权级：RISC-V 用 M/S/U 三层干净的硬件权限，让固件、内核、应用各居其位；2026 年 RVA23 把虚拟化（H 扩展）设为强制——"能跑 Linux、能开虚拟机"成了应用处理器的及格线。

## 1. 三层特权：M/S/U

RISC-V 定义三个特权级，硬件强制隔离：

- **M 模式（Machine，最高）**：运行固件与安全监控，可访问一切 CSR 与物理资源。事实标准固件是 **OpenSBI**（提供 SBI 服务，见第 3 节）；最小嵌入式系统可以只实现 M 模式
- **S 模式（Supervisor，中间）**：运行操作系统内核，管理虚拟内存（页表、satp CSR）、中断与异常分发。Linux 内核就跑在 S 模式
- **U 模式（User，最低）**：运行应用，一切敏感操作通过系统调用陷入内核（ecall 指令触发 trap）

三个规则构成安全模型：**低特权级不能访问高特权级的状态**（CSR 分权限）；**模式切换只能通过 trap 机制**（ecall/异常/中断进入更高级别，mret/sret 返回）；**页表权限与特权级叠加**（U 模式页表只能访问 U 权限页面）。Linux 的应用-内核隔离、MMU 的地址空间保护，全部由这三层规则支撑。

### 1.1 与 x86/ARM 的对照

| 概念 | x86 | ARM | RISC-V |
|------|-----|-----|--------|
| 特权级 | 4 级（ring0-3，实际只用 0/3） | EL0-EL3 | M/S/U（+VS/VU 虚拟化） |
| 固件/安全世界 | SMM/CPUID 固件 | EL3（TrustZone） | M 模式 + OpenSBI |
| 陷入指令 | syscall/int | svc | ecall |
| 返回指令 | iret | eret | mret/sret |
| 虚拟化 | VT-x/AMD-V（VMX 根/非根） | EL2（VHE 可合并） | H 扩展（HS/VS/VU） |

RISC-V 的"三层"比 x86 的四层更诚实（x86 的 ring1/2 几乎无人使用）、比 ARM 的四层更简（EL1/EL2 语义重叠，VHE 合并是补丁式设计）——**M/S/U 的对应关系一眼可辨，是三类架构里最好教的模型**，这也是它成为大学操作系统课程标配的原因。

## 2. CSR 与陷阱机制：内核的"控制面板"

特权架构的心脏是 CSR（Control and Status Register，控制状态寄存器），按权限分为 m*（仅 M 可访问）、s*（S 及以上）、u*（U 可访问只读）。核心成员：

- **mstatus/sstatus**：全局状态（中断使能、特权级切换）
- **mtvec/stvec**：陷阱向量基址（异常/中断跳转目标）
- **mepc/sepc**：陷阱返回地址；**mcause/scause**：陷阱原因
- **satp**：页表根地址（S 模式 MMU 开关，Linux 每进程切换）
- **mhartid**：当前硬件线程 ID（多核启动必读）

**ecall 陷入的完整流程**（系统调用的硬件底座）：U 模式执行 ecall → 硬件原子切换到 S 模式 → 保存 sepc/scause → 跳转 stvec 指向的内核入口 → 内核根据 scause 分发（系统调用/缺页/中断）→ 处理完成执行 sret → 恢复 U 模式。**这套流程与 x86 的 syscall 语义等价，但状态保存显式且完整**——RISC-V 的 CSR 数量少、命名规则统一（前缀即权限），读代码比 x86 的 MSR 迷宫友好得多。

## 3. SBI：固件与内核的标准化接口

RISC-V 的一个独特设计：**S 模式与 M 模式的接口被规范化为 SBI（Supervisor Binary Interface）**——内核不直接操作任何硬件细节，而是通过 ecall 调用 M 模式固件提供的服务（时间、控制台、重启、IPI 等）。OpenSBI 是事实标准的 M 模式固件实现。

SBI 的意义在启动流程里最清晰：

```text
上电 → M 模式固件（OpenSBI）初始化 → 跳到 S 模式内核入口
     → 内核运行中通过 ecall 请求 M 模式服务
     → 虚拟化场景：H 模式 Hypervisor 叠加（第 4 节）
```

这套分层带来的工程红利：**固件与内核解耦**——换启动固件（U-Boot/OpenSBI）不影响内核；**多核启动有标准流程**（主 hart 引导、从 hart 等 IPI）；**安全监控可以插入 M 模式**（CoVE 的 TSM 就住在 M 模式，07 篇）——x86 的固件-内核边界模糊（BIOS/UEFI/SMM 各自为政），RISC-V 用一个公开规范统一了它。

## 4. H 扩展：硬件虚拟化成为标配

H 扩展（Hypervisor）为虚拟化提供硬件加速，核心是两级地址翻译（G-stage + VS-stage）与新增模式：

- **HS 模式**（Host Supervisor）：跑 Hypervisor（KVM 等）
- **VS/VU 模式**（Virtual Supervisor/User）：跑客户机内核/应用
- **两级地址翻译**：客户机页表（VS 级）+ 宿主机物理页表（G 级）串联，TLB 直通硬件完成——**虚拟机地址翻译不再需要软件影子页表**，这是 KVM 高性能的硬件前提

RVA23 把 H 扩展设为**强制**——2026 年起，任何 RVA23 合规应用处理器必须支持硬件虚拟化。配套的虚拟化基础设施也在 2026 年就绪：KVM 的 RISC-V 后端已上游、RVA23 服务器平台规范标准化了 PCIe/IOMMU（设备直通与 DMA 隔离）、CoVE 机密虚拟机（07 篇）直接构建在 H 扩展之上——**"能开虚拟机"从高级特性变成及格线**，这是 RISC-V 从嵌入式正式迈入数据中心的关键一步。

## 5. 中断与时钟：操作系统的节拍器

特权架构的另一半是中断与时钟：**M 模式中断**（mstatus.MIE 使能、mie 寄存器选来源）处理固件级事件（定时器、软件中断、外部中断）；**S 模式中断**（sstatus.SIE 等）处理内核关心的事件（时钟中断、设备中断、IPI）。RISC-V 的中断控制器经历了"CLINT/PLIC（旧）→ APLIC/IMSIC（新，RVA23 强制）"的演进：CLINT 提供定时器与 IPI，PLIC 分发外部设备中断；2026 年的 RVA23 强制 IMSIC（按 hart 私有的 MSI 式中断控制器）——**虚拟化与机密计算（CoVE 要求 MSI-only）都依赖 IMSIC**，这是中断体系升级的根本原因。

时钟中断的完整流程（Linux 的心跳）：定时器中断 → S 模式跳转 stvec → 内核识别 scause=时钟中断 → 更新 jiffies/tick → 调度器检查时间片 → sret 返回——**操作系统的"时间感"完全建立在这条硬件路径上**，与 x86 的 APIC/时钟中断语义等价，但 CSR 路径显式清晰（第 2 节）。

### 5.1 启动流程全貌：一条链串起全部知识点

RISC-V 服务器上电后的完整旅程：**复位 → 所有 hart 从 M 模式启动（执行 OpenSBI）→ 主 hart 初始化平台（内存、PCIe、中断控制器）→ OpenSBI 跳转 S 模式内核入口 → 内核初始化（页表、调度器、驱动）→ 从 hart 经 IPI 被唤醒 → 进入用户态启动第一个进程（init）→ 运行中，系统调用（ecall）与中断（trap）在 M/S/U 之间穿梭**。每个环节都有对应章节：OpenSBI 是第 3 节、内核在 S 模式是第 1 节、ecall 是第 2 节、IPI 是本节——**一条启动链，把本体系全串起来**。对照 x86：BIOS/UEFI → 实模式 → 保护模式 → 长模式的层层转换充满历史包袱；RISC-V 的 M→S→U 单向前进，每一步都在规范里明确定义——**启动链的整洁度，是架构整洁度的第一份体检报告**。

### 5.2 虚拟化的实战形态：KVM 的 RISC-V 路径

RISC-V 上跑虚拟机的现实路径：**KVM（内核虚拟机）**——Linux 的 RISC-V KVM 后端已上游多年，配合 H 扩展的两级地址翻译提供接近原生的性能；**QEMU** 的用户态模拟与系统模拟全支持 RISC-V（含多核、SMP、中断控制器模拟）——开发环境里"虚拟机跑虚拟机"完全可行。2026 年的新增量：RVA23 强制 H 扩展后，所有新平台默认带虚拟化，**"服务器能开 KVM"从可选项变默认项**；CoVE 机密虚拟机（07 篇）又让"虚拟机"从"隔离"升级到"保密"——RISC-V 的虚拟化叙事与 x86 的演进节奏完全对齐：先是 KVM 生态就绪，再是机密计算补齐，2026-2027 年进入"与 x86 同台"的窗口。对运维工程师：虚拟机迁移（P2V/V2V）在 RISC-V 平台与 x86 流程一致，但镜像与驱动需按架构重新准备——**迁移成本主要在镜像层，不在虚拟化层**。

一个容易忽略的 CSR 类别：**性能计数器（mcycle/minstret 等）**——RISC-V 把"跑了多少周期、执行了多少指令"也规范化了，perf 工具直接读它们做性能分析；对比 x86 的专用 PMU 模型，RISC-V 的计数器更简单也更开放（用户态可读，门槛低）。

> 🎯 **核心要点**：RISC-V 用 M/S/U 三层硬件权限实现固件-内核-应用隔离，ecall/mret 的 trap 机制与 x86/ARM 等价但更规整；CSR 命名即权限（m*/s*），系统调用路径显式可读；SBI 把固件-内核边界规范化为标准接口，OpenSBI 成为事实标准；H 扩展的两级地址翻译让 KVM 高性能虚拟化成为可能，RVA23 强制 H 扩展意味着 2026 年"能开虚拟机"是应用处理器的及格线——这是 RISC-V 进入数据中心的真正入场券。

---

**下一模块**：[06-向量与AI扩展](06-向量与AI扩展：RVV_1.0与Matrix.md)　**返回总览**：[00-RISC-V基础知识体系总览](00-RISC-V基础知识体系总览.md)

## 参考来源

- [RISC-V 安全：特权级、PMP 与机密计算 — Luca Berton](https://lucaberton.com/blog/risc-v-security-privilege-pmp-confidential-computing/)
- [RVA23 服务器平台规范 1.0（UEFI/ACPI/PCIe 标准化）— IndexBox](https://www.indexbox.io/blog/risc-v-ecosystem-strong-targets-enterprise-data-centers-with-rva23-standard/)
- [RISC-V 特权架构文档（Smstateen/Ssstateen 1.0）— RISC-V 官方](https://docs.riscv.org/reference/isa/v20260120/priv/smstateen.html)
