# TCP/UDP/IP 性能调优

> 单机网络吞吐的公式 = 窗口（BDP 匹配）× 卸载（offload）× 队列（qdisc）× 参数（sysctl）。先算清 BDP 才知道窗口够不够，再开 offload 让 CPU 干别的，最后用 sysctl 与队列把细节补齐——本章给出从计算到落地的完整清单

---

## 📚 目录

1. [性能公式：BDP 是一切的前提](#1-性能公式bdp-是一切的前提)
2. [接收窗口与发送缓冲调优](#2-接收窗口与发送缓冲调优)
3. [硬件卸载：TSO / GRO / 网卡 offload](#3-硬件卸载tso--gro--网卡-offload)
4. [队列与调度：fq / BBR 配套](#4-队列与调度fq--bbr-配套)
5. [sysctl 参数全表](#5-sysctl-参数全表)
6. [单机高吞吐调优清单](#6-单机高吞吐调优清单)
7. [UDP 性能要点](#7-udp-性能要点)
8. [核心要点与思考题](#8-核心要点与思考题)

---

## 1. 性能公式：BDP 是一切的前提

```text
BDP（带宽延迟积）= 带宽 × RTT —— 网络中"在途"的数据量

关键推论（TCP 吞吐上限）：
  吞吐 ≤ 窗口大小 / RTT
  → 窗口必须 ≥ BDP，否则带宽跑不满！

实例：
  10Gbps × 100ms（跨洋）→ BDP = 10G × 0.1s = 1Gbps·s = 125MB
  窗口 16 位最大值 64KB → 吞吐上限 = 64KB / 0.1s ≈ 5Mbps（严重浪费！）
  → 必须窗口缩放（shift 因子）把窗口提到 ≥ 125MB
  → 所以高 BDP 场景三件套：窗口缩放 + 自动缓冲 + 大内存

常见场景 BDP：
  1Gbps × 1ms（机房内）  → 125KB（默认窗口够）
  10Gbps × 50ms（城域）  → 62.5MB（需调优）
  1Gbps × 200ms（跨国）  → 25MB（需调优）
```

> 🎯 **核心要点**：调优第一步永远是**算 BDP**——算完就知道瓶颈在窗口还是链路。公式：`吞吐 ≤ min(窗口/RTT, 链路带宽, CPU/卸载能力)`。

## 2. 接收窗口与发送缓冲调优

```text
内核自动调优（现代 Linux 默认）：
  tcp_rmem = 4096  131072  6291456   （min 默认 初始  max）
  tcp_wmem = 4096  16384   4194304
  初始窗口按 RTT 自适应增长（不手动设死）

手动场景（追求确定性）：
  # 高 BDP 连接（10Gbps×100ms）：
  sysctl -w net.ipv4.tcp_rmem="4096 1048576 268435456"   # max 256MB
  sysctl -w net.ipv4.tcp_wmem="4096 1048576 268435456"
  # 注意：max 越大 → 每连接内存越大 → 内存预算（并发体系 06 章）

应用侧（Java）：
  socket.setReceiveBufferSize() —— 应用设定的值内核会 ×2 保留
  → 设 1MB 实际占 2MB（内核内部双缓冲）
```

## 3. 硬件卸载：TSO / GRO / 网卡 offload

| 卸载特性 | 作用 | 效果 |
|---------|------|------|
| **TSO/GSO** | 发送侧：大包交给网卡拆成 MSS 段 | 大幅减少 CPU 发包开销 |
| **GRO/LRO** | 接收侧：网卡把多个段合并成大包交内核 | 减少内核收包次数 |
| GSO/GRO（软件） | 无卸载网卡时的软件版本 | 兼容兜底 |
| 校验和卸载 | 网卡算 TCP/UDP 校验和 | 省 CPU |
| RSS（多队列） | 网卡按哈希分散到多 CPU | 多核并行收包 |
| XDP/eBPF | 收包最早点处理（DPDK 之前的轻量旁路） | 极高 PPS 场景 |

```bash
# 查看/管理卸载（ethtool）
ethtool -k eth0 | grep -E "tcp-segmentation|generic-segmentation|rx-gro"
# tcp-segmentation-offload: on     ← TSO
# generic-receive-offload: on      ← GRO
# 关闭（排障时验证卸载问题）：
ethtool -K eth0 tso off gro off
# RSS 队列数
ethtool -l eth0
```

> ⚠️ **排障注意**：卸载特性偶发 bug（大包校验错误）——现象是"低吞吐/随机错误/校验和失败"，先用 `ethtool -K tso off gro off` 验证再上报。

## 4. 队列与调度：fq / BBR 配套

```text
qdisc（队列纪律）：出方向包调度器
  pfifo_fast（默认，简单）
  fq（公平队列）：
    每流独立队列 + 按 pacing 平滑发送
    → BBR 的 pacing 依赖它（04 章）；也降低 bufferbloat
  cake：更现代的公平队列（VoIP/游戏友好，实验性应用）
  fq_codel：CoDel 主动队列管理（默认许多发行版）

启用：
  sysctl -w net.core.default_qdisc=fq     # BBR 配套
  # 或按接口
  tc qdisc replace dev eth0 root fq
```

## 5. sysctl 参数全表

| 参数 | 默认 | 建议 | 场景 |
|------|:---:|:---:|------|
| `net.ipv4.tcp_rmem/wmem` | 动态 | 按 BDP（第 2 节） | 高吞吐 |
| `net.ipv4.tcp_window_scaling` | 1 | 1（勿关） | 大窗口前提 |
| `net.ipv4.tcp_sack` | 1 | 1（勿关） | 丢包高效恢复 |
| `net.ipv4.tcp_timestamps` | 1 | 1 | RTT 精度/PAWS |
| `net.ipv4.tcp_congestion_control` | cubic | cubic 或 bbr | 算法选型（04 章） |
| `net.core.default_qdisc` | pfifo_fast | fq | BBR pacing |
| `net.core.rmem_max/wmem_max` | 212992 | 按需（如 16MB） | 应用大缓冲前提（socket 设置上限） |
| `net.ipv4.tcp_fastopen` | 1 | 3（客户端+服务端） | 快速打开（安全评估后） |
| `net.ipv4.tcp_slow_start_after_idle` | 1 | 0 | 空闲后免慢启动 |
| `net.ipv4.tcp_mtu_probing` | 0 | 1 | MSS 错配自愈（PMTUD 兜底） |
| `net.ipv4.tcp_ecn` | 2 | 1（协商启用） | 显式拥塞通知（可选） |
| `net.core.netdev_max_backlog` | 1000 | 5000+ | 收包队列积压 |

```bash
# 高吞吐基准配置（生产前逐项评估）
cat >> /etc/sysctl.conf <<'EOF'
net.core.rmem_max = 16777216
net.core.wmem_max = 16777216
net.ipv4.tcp_rmem = 4096 1048576 16777216
net.ipv4.tcp_wmem = 4096 1048576 16777216
net.ipv4.tcp_congestion_control = cubic
net.core.default_qdisc = fq
net.ipv4.tcp_slow_start_after_idle = 0
net.ipv4.tcp_mtu_probing = 1
EOF
sysctl -p
```

## 6. 单机高吞吐调优清单

```text
□ 1. 算 BDP → 决定窗口目标（第 1 节公式）
□ 2. 窗口链路：rmem/wmem max ≥ BDP；应用 setReceiveBufferSize 匹配
□ 3. 卸载：确认 TSO/GRO/RSS 开启；多队列分散到核
□ 4. 队列：fq（或 fq_codel）+ BBR（若压测证明收益）
□ 5. 内存预算：每连接缓冲 × 连接数（并发体系 06 章联动）
□ 6. 压测验证：iperf3 单流/多流对比
   iperf3 -c server -P 8 -t 30        # 8 并发流
   iperf3 -c server -w 4M -t 30       # 指定窗口
□ 7. 回归：卸载/队列变更后跑业务压测（吞吐 vs 时延双指标）
```

```bash
# iperf3 压测参考
# 服务端
iperf3 -s -p 5201
# 客户端（先 1 流看单流上限，再 8 流看总量）
iperf3 -c 10.0.0.2 -P 1 -t 30
iperf3 -c 10.0.0.2 -P 8 -t 30
# 结论判读：单流 ≈ 窗口/RTT；多流 ≈ 链路带宽
```

## 7. UDP 性能要点

| 要点 | 说明 |
|------|------|
| 无窗口/拥塞 → 吞吐 = 应用发送速率 | 由应用控制（自限速） |
| 收包瓶颈在**中断与拷贝** | 开 RSS 多队列、GRO、大 MTU（9000 jumbo） |
| 大报文 = 分片风险 | 保持 ≤ MTU；jumbo 帧内网可用 |
| 内存 | 每连接无状态 → 大缓冲按 socket 分配 |
| 丢包诊断 | `netstat -su` 看 UDP 收包错误/丢弃计数 |

## 8. 调优案例：两个真实场景

### 8.1 场景一：跨洋大文件传输（高 BDP）

```text
需求：10Gbps × 200ms（跨洋链路）传 10GB 文件

诊断：
  BDP = 10G × 0.2s = 2.5Gbps·s ≈ 300MB
  默认窗口（~2MB 自动）→ 理论吞吐上限 = 2MB/0.2s = 80Mbps ✗

调优：
  sysctl tcp_rmem/wmem max = 512MB（按 BDP 1.5-2 倍）
  应用：socket 缓冲设置匹配；启用 SACK/时间戳
  压测：iperf3 -w 256M 验证单流吞吐 → 应达 8-9Gbps

验证结果：单流从 80Mbps → 8.5Gbps（约 100 倍提升）
教训：跨洋传输的"慢"几乎总是窗口问题（BDP 未匹配）
```

### 8.2 场景二：移动端高并发短请求（低 BDP 高损耗）

```text
需求：移动 App API，每请求 ~50KB，并发 10 万

诊断：
  BDP 极小（RTT 50ms × 低带宽）→ 窗口不是瓶颈
  丢包率 1-5%（无线）→ 重传是主要开销

调优：
  算法：CUBIC → BBR（丢包不惩罚，04 章）
  队列：fq（BBR pacing 必需）
  应用：keep-alive 连接复用（省握手，并发体系 07 章）
  验证：对比 CUBIC/BBR 的 P99 时延与重传率

验证结果：P99 时延 -30%，重传率显著下降（弱网场景）
教训：低 BDP 场景瓶颈在"丢包恢复"而非窗口——算法选型优先
```

### 8.3 调优决策树

```text
网络性能问题 → 先测再调：
  ① 算 BDP：窗口够吗？→ 不够：窗口/缓冲（场景一）
  ② 看丢包率：高吗？→ 高：算法（BBR）+ SACK 验证
  ③ 看 CPU：卸载开吗？→ 未开：TSO/GRO/RSS
  ④ 看队列：fq 了吗？→ 未配：默认 qdisc 换 fq
  ⑤ 回归压测：吞吐/时延/CPU 三指标对比
```

## 9. 核心要点与思考题

> 🎯 **本章三句话总结**：
> 1. 一切调优从 BDP 出发：`吞吐 ≤ 窗口/RTT`，窗口必须 ≥ 带宽×时延；
> 2. 三件套落地：窗口（rmem/wmem + 缩放）、卸载（TSO/GRO/RSS）、队列（fq+BBR）；
> 3. UDP 的吞吐由应用自管，系统侧做好 RSS/GRO/大 MTU 即可。

**思考题**：

1. 为什么 10Gbps×100ms 必须窗口缩放？（→ 1 BDP 实例）
2. `net.core.rmem_max` 与应用 `setReceiveBufferSize` 的关系？（→ 2）
3. TSO 关了为什么吞吐暴跌？（→ 3 网卡拆段省 CPU）
4. BBR 与 fq 为什么必须配套？（→ 4 + 04 章）

---

**下一模块**：[09-面试高频考点与总结](09-面试高频考点与总结.md)｜**返回总览**：[00-TCP与UDP与IP知识体系总览](00-TCP与UDP与IP知识体系总览.md)
