# Firecracker 微虚拟机：硬件虚拟化隔离

> 2026 年不可信代码执行的业界默认：每沙箱一个独立内核，攻破它需打穿 hypervisor，而非 syscall 实现。AWS Lambda 每秒 15 万亿次调用的底座。

## 1. 为什么是"业界默认"

| 维度 | Firecracker | 对比说明 |
|---|---|---|
| 隔离机制 | KVM 硬件虚拟化，每沙箱独立内核 | **无共享内核攻击面**——容器/gVisor 的核心弱点被根除 |
| 启动耗时 | **~125ms**（E2B 报 ~150ms） | 接近容器、远快于全 VM（秒级） |
| 内存开销 | **~5MB/沙箱** | 传统 VM 数百 MB——密度高到可做多租户 |
| 攻破成本 | 需 exploit hypervisor | 远高于容器（内核漏洞）与 gVisor（Sentry） |
| 支持方 | AWS Lambda（**15 万亿次/月**）、E2B、Fly.io、Northflank、Vercel Sandbox | 生态成熟、久经考验 |

> 💡 与 gVisor 的关键差异：Firecracker 需 `/dev/kvm`（裸机或嵌套虚拟化）；gVisor 无此依赖——这是 ChatGPT 在 K8s-on-VM 上选 gVisor 的务实原因（见 [03-容器与 gVisor](03-容器与gVisor.md)）。

## 2. 架构要点

| 组件/特性 | 说明 |
|---|---|
| 实现语言 | **Rust 编写的 VMM**——内存安全语言降低 VMM 自身漏洞率 |
| 极简设备模型 | 只暴露虚拟化所需最小设备集（无 BIOS、无 PCI 枚举等传统 VM 负担）→ 攻击面小 + 启动快 |
| 控制面 | API socket（`/boot-source`、`/drives`、`/actions`）创建/启动/停止，无磁盘镜像管理 |
| 快照 | 内存 + 磁盘状态冻结保存，之后秒级恢复 |

```bash
# Firecracker 控制面概念流程（伪代码）
PUT /boot-source      # 指定 kernel 镜像
PUT /drives           # 挂载 rootfs 卷
PUT /actions InstanceStart     # 启动
PUT /snapshot/create  # 冻结快照（内存+磁盘）
PUT /actions InstanceRestore  # 秒级恢复
```

## 3. 快照/恢复的价值

| 场景 | 快照收益 |
|---|---|
| 长任务续跑 | 超时会话从快照续跑，省算力不重算 |
| 调试 | 复现故障现场（内存+磁盘完整状态） |
| 冷启动优化 | 预创建快照池，恢复远快于重建 |
| 环境模板 | 同一环境秒级克隆（Daytona 暂停/恢复也是此模式） |

## 4. 2026 新物种：AWS Lambda MicroVMs

- **2026-06-22 发布**：托管 Firecracker 微VM 成为一等云原语——"给每个 Agent 会话一个专属微VM"
- 规格：单沙箱最多 **16 vCPU / 32GB**；最长 **8 小时**运行
- 快照式**近即时恢复**；无需自管基础设施（内核镜像/rootfs/网络插件全托管）
- 定位：把"自建 Firecracker 集群"的运维复杂度外包给云——自建与托管的分水岭

## 5. 托管 vs 自建（Firecracker 路径）

| 维度 | 自建 Firecracker 集群 | 托管（E2B / Vercel / Lambda MicroVMs） |
|---|---|---|
| 隔离能力 | 相同（Firecracker） | 相同 |
| 运维 | 内核镜像/rootfs/快照/网络全自管 | 全托管 |
| 成本 | 低（开源） | 按量计费 |
| GPU | 可加（自建直通） | 无 GPU（E2B/Vercel 均无） |
| 适用 | 企业合规/数据不出内网 | 快速起步 |

## 6. 局限与权衡

| 局限 | 说明 |
|---|---|
| 需 `/dev/kvm` | 裸金属或嵌套虚拟化；K8s-on-VM（无 KVM）不可用 → 此时选 gVisor |
| 运维复杂 | 自建需管内核镜像、rootfs、快照、网络插件 |
| 不支持 GPU | Firecracker 微VM 无 GPU 直通（E2B/Vercel 无 GPU；要 GPU 选 Modal/Daytona） |
| 会话时长受限 | 依赖托管方策略：E2B Pro 24h/Base 1h；Vercel Pro 24h；Lambda MicroVMs 8h |

## 面试速记

1. Firecracker = KVM 微VM：125ms 启动、5MB 内存、Rust VMM、极简设备模型——不可信代码的业界默认。
2. 攻破微VM 需 exploit hypervisor，远难于容器/gVisor。
3. 快照 = 内存+磁盘冻结，秒级恢复：长任务续跑、冷启动优化、环境克隆。
4. 需 /dev/kvm 是主要部署门槛（K8s-on-VM 场景选 gVisor）；无 GPU 直通。
5. AWS Lambda MicroVMs（2026-06-22）：托管微VM 云原语，16 vCPU/32GB、8h、快照恢复。

---

**下一模块**：[05-WASM 与语言级沙箱](05-WASM与语言级沙箱.md) ｜ **返回总览**：[00-代码执行沙盒总览](00-代码执行沙盒总览.md)

## 参考来源

- [Best platforms for untrusted code execution in 2026（Northflank）](https://northflank.com/blog/best-platforms-for-untrusted-code-execution)
- [How to sandbox AI agents in 2026（dev.to）](https://dev.to/manveerchawla/how-to-sandbox-ai-agents-in-2026-firecracker-gvisor-runtimes-isolation-strategies-14pk)
- [AWS Lambda MicroVMs: Sandbox Agent Code Without the Infra（AgentConn）](https://agentconn.com/blog/sandbox-agent-code-aws-lambda-firecracker-microvms-2026/)
- [Best microVM Sandboxes for AI Code Execution in 2026（Modal）](https://modal.com/resources/best-microvm-sandboxes-ai-code-execution)
- [Best Platforms for High-Concurrency Sandbox Environments（blaxel）](https://blaxel.ai/blog/best-platforms-high-concurrency-sandbox-environments)
