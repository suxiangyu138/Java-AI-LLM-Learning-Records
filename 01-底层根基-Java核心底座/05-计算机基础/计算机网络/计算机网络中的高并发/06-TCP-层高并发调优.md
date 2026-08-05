# TCP 层高并发调优

> 高并发的第一现场是内核：backlog 溢出丢连接、TIME_WAIT 吃光端口、Nagle 与延迟 ACK 叠加出 40ms 延迟、文件描述符上限卡住连接数——这些坑每个都在面试与生产里反复出现。本章给全参数与排查手段

---

## 📚 目录

1. [连接建立：backlog 与 SYN/accept 队列](#1-连接建立backlog-与-synaccept-队列)
2. [连接关闭：TIME_WAIT 与端口耗尽](#2-连接关闭time_wait-与端口耗尽)
3. [Nagle 与延迟 ACK：40ms 延迟之谜](#3-nagle-与延迟-ack40ms-延迟之谜)
4. [keepalive：连接保活的三种语义](#4-keepalive连接保活的三种语义)
5. [连接数上限：fd 与端口](#5-连接数上限fd-与端口)
6. [sysctl 调优参数速查表](#6-sysctl-调优参数速查表)
7. [排查手段](#7-排查手段)
8. [核心要点与思考题](#8-核心要点与思考题)

---

## 1. 连接建立：backlog 与 SYN/accept 队列

### 1.1 三次握手与两个队列

```text
客户端                      服务端
SYN ──────────────────────→  SYN 队列（半连接队列，未完成握手）
SYN+ACK ←───────────────────  （SYN_RECV 状态，等待 ACK）
ACK ──────────────────────→  移入 accept 队列（已建立连接）
                             accept() 取走 → 应用处理
```

| 队列 | 内容 | 满时的行为 |
|------|------|-----------|
| SYN 队列（半连接） | 收到 SYN 未完成握手的连接 | 内核丢弃 SYN（客户端重试） |
| accept 队列（全连接） | 握手完成等 accept() 的连接 | 内核丢弃 ACK（**连接建立失败**） |

### 1.2 backlog 参数链

```text
backlog 的取值链路（每层都可能截断）：
  ① 应用层：listen(fd, backlog) / Java ServerSocket(port, backlog) / Nginx listen 8080 backlog=511
  ② 内核上限：net.core.somaxconn（默认 4096，旧内核 128）
  ③ 实际取：min(应用 backlog, somaxconn)

accept 队列长度 = min(backlog, somaxconn)（+ 部分超额，Linux 2.6.25+ 允许 1.5 倍增长）
SYN 队列长度 ≈ max(backlog, 8) 受 tcp_max_syn_backlog（默认 1024）影响
```

```bash
# 调优
sysctl -w net.core.somaxconn=65535          # 内核 accept 队列上限
sysctl -w net.ipv4.tcp_max_syn_backlog=65535  # SYN 队列上限
# 应用侧：Java ServerSocket(8080, 1024)；Nginx listen 8080 backlog=4096;
```

> ⚠️ **症状识别**：`ss -lnt` 看 Recv-Q（accept 队列堆积数）接近上限 → 应用 accept 太慢；`netstat -s | grep -i "SYNs to LISTEN"` 看丢弃。**accept 队列满 ≠ 内核问题，是应用消费慢**。

## 2. 连接关闭：TIME_WAIT 与端口耗尽

### 2.1 TIME_WAIT 的来源

```text
主动关闭方在发送最后一个 ACK 后进入 TIME_WAIT，等待 2MSL（Linux 默认 60s）：
  目的①：保证对方重传的 FIN 能收到（防止旧连接干扰新连接）
  目的②：让网络中旧报文自然消亡

高并发短连接（HTTP/1.1 短连接）：
  每次请求 = 客户端主动关闭 → 客户端产生大量 TIME_WAIT
  连接数 = QPS × 60s 的存活量 → 客户端端口耗尽（约 3 万/5.5 万可用）
```

### 2.2 端口耗尽公式与对策

```text
客户端可用端口 = net.ipv4.ip_local_port_range（默认 32768-60999 → ~2.8 万）
短连接 QPS 上限 ≈ 端口数 / TIME_WAIT 时长 = 28000 / 60s ≈ 460 QPS（单连接）！

对策层次：
  ① 应用层（首选）：连接复用（keep-alive/连接池，07 章）→ 根治
  ② 内核参数：tcp_tw_reuse=1（客户端复用 TIME_WAIT 端口，仅对出站连接生效）
  ③ 调端口范围：ip_local_port_range=1024 65535
  ④ 服务端优化：tcp_fin_timeout 缩短（影响 TIME_WAIT 相关的 FIN 等待）
```

```bash
sysctl -w net.ipv4.ip_local_port_range="1024 65535"   # ~6.4 万端口
sysctl -w net.ipv4.tcp_tw_reuse=1                     # 出站 TIME_WAIT 端口复用
# ⚠️ tcp_tw_recycle 已废弃（内核 4.12 移除）——NAT 环境下会导致连接异常，勿用
```

> 🎯 **核心要点**：TIME_WAIT 是**正确性设计**（防旧报文串扰），不能"消灭"，只能"绕开"——连接复用是最佳解，`tcp_tw_reuse` 是次优解（有语义代价）。

## 3. Nagle 与延迟 ACK：40ms 延迟之谜

### 3.1 两个算法

| 算法 | 规则 | 目的 |
|------|------|------|
| **Nagle**（发送侧） | 有未确认小包时，小包合包发送（最多等一个 RTT） | 减少小包（慢网时代） |
| **延迟 ACK**（接收侧） | ACK 延迟最多 40ms，可捎带数据（Linux tcp_delack_min 等） | 减少 ACK 数量 |

### 3.2 叠加效应：40ms 停顿

```text
经典死锁式交互：
  客户端发送侧 Nagle：发了一个小包等 ACK 再发下一个（合包）
  服务端接收侧延迟 ACK：等 40ms 凑数据才回 ACK
  → 客户端小包被"卡"在 Nagle 等待里 → 每个交互固定 +40ms

触发条件：小包（< MSS）+ 未确认 + 交互式请求/响应
```

### 3.3 解法：TCP_NODELAY

```java
// Java：关闭 Nagle（高频请求/响应场景必须）
Socket s = new Socket(host, port);
s.setTcpNoDelay(true);          // 关闭 Nagle 算法（对应 TCP_NODELAY=1）
```

```text
什么时候关：交互式（请求-响应模式）——Web/API/RPC 全部适用
什么时候别关：批量大包传输（Nagle 合包有益，如文件传输）
服务端相关：net.ipv4.tcp_slow_start_after_idle=0（避免空闲后慢启动惩罚）
```

## 4. keepalive：连接保活的三种语义

| 层级 | 机制 | 默认 | 作用 |
|------|------|:---:|------|
| TCP keepalive（内核） | 空闲 2h 后每 75s 探测，9 次失败断开 | 2h | 检测对端崩溃（**默认太慢**，生产调短） |
| HTTP keep-alive（应用） | 连接复用（07 章） | 按服务器配置 | 减少握手（**高并发关键**） |
| 应用心跳（业务） | 自定义 ping/pong | 应用决定 | 业务保活 + 断线检测 |

```bash
# TCP keepalive 调优（服务端检测死连接）
sysctl -w net.ipv4.tcp_keepalive_time=600      # 空闲 10 分钟开始探测
sysctl -w net.ipv4.tcp_keepalive_intvl=30      # 探测间隔 30s
sysctl -w net.ipv4.tcp_keepalive_probes=3      # 3 次失败即断开
```

> 💡 **区别认知**：TCP keepalive 是内核检测"物理对端活着没"（网络层保活）；HTTP keep-alive 是"连接复用"（性能层优化）——两者名字像，作用完全不同。

## 5. 连接数上限：fd 与端口

### 5.1 三个上限

| 上限 | 默认 | 调优 | 症状 |
|------|:---:|------|------|
| 单进程 fd 数（ulimit -n） | 1024 | `ulimit -n 1000000` | accept 失败、EMFILE |
| 系统总 fd（fs.file-max） | 视内存 | `sysctl fs.file-max=2000000` | 全系统 EMFILE |
| 端口范围 | 32768-60999 | 见 2.2 | 客户端连不上 |

```bash
ulimit -n 1000000                          # shell 级（需在启动脚本里设置）
echo "fs.file-max = 2000000" >> /etc/sysctl.conf
# Java：服务端进程 ulimit 未调时，即使内核支持百万连接也会 EMFILE
```

### 5.2 连接数估算

```text
百万连接 × 每连接内核缓冲（收发各 ~16KB 起）：
  → 纯内核缓冲就可能 32GB+ 内存 —— 生产调小缓冲（tcp_rmem/wmem 下限）
  → 服务器内存预算要与连接规模挂钩（01 章成本模型）
```

## 6. sysctl 调优参数速查表

| 参数 | 默认 | 建议 | 场景 |
|------|:---:|:---:|------|
| `net.core.somaxconn` | 4096 | 65535 | accept 队列上限 |
| `net.ipv4.tcp_max_syn_backlog` | 1024 | 65535 | SYN 队列上限 |
| `net.ipv4.ip_local_port_range` | 32768-60999 | 1024-65535 | 客户端端口池 |
| `net.ipv4.tcp_tw_reuse` | 0 | 1 | 出站 TIME_WAIT 复用 |
| `net.ipv4.tcp_fin_timeout` | 60 | 30 | FIN 等待缩短 |
| `net.ipv4.tcp_keepalive_time` | 7200 | 600 | 保活探测启动 |
| `net.ipv4.tcp_slow_start_after_idle` | 1 | 0 | 空闲后免慢启动 |
| `net.ipv4.tcp_rmem/wmem` | 动态 | 按需调小下限 | 每连接内存预算 |
| `fs.file-max` | 视内存 | 2000000 | 系统 fd 上限 |
| `net.core.netdev_max_backlog` | 1000 | 5000+ | 网卡队列积压 |

```bash
# 一键应用（生产请先评估）
cat >> /etc/sysctl.conf <<'EOF'
net.core.somaxconn = 65535
net.ipv4.tcp_max_syn_backlog = 65535
net.ipv4.ip_local_port_range = 1024 65535
net.ipv4.tcp_tw_reuse = 1
net.ipv4.tcp_fin_timeout = 30
net.ipv4.tcp_keepalive_time = 600
net.ipv4.tcp_slow_start_after_idle = 0
fs.file-max = 2000000
EOF
sysctl -p
```

> ⚠️ **调优纪律**：参数不是"越大越好"——缓冲区大 = 每连接内存高（百万连接撑不住）；`tcp_tw_reuse` 有语义代价；改参数必须压测验证，且标注"为何改"（运维交接）。

## 7. 排查手段

| 症状 | 命令 | 看什么 |
|------|------|--------|
| 连接建立失败 | `netstat -s | grep -i "SYNs to LISTEN"` | SYN 丢弃计数 |
| accept 队列满 | `ss -lnt` | Recv-Q 堆积 |
| 端口耗尽 | `netstat -an | grep TIME_WAIT | wc -l` | TIME_WAIT 总量 |
| fd 耗尽 | `ls /proc/<pid>/fd | wc -l`；`dmesg | grep EMFILE` | 打开数 vs ulimit |
| 40ms 延迟 | 抓包看交互间隔 | 是否有 Nagle/延迟 ACK 叠加 |
| 连接异常断开 | `netstat -s` 的 RST 计数 | 非正常关闭比例 |

## 8. 核心要点与思考题

> 🎯 **本章三句话总结**：
> 1. 连接建立看双队列（backlog/somaxconn/SYN 队列），关闭看 TIME_WAIT——短连接下客户端端口池是吞吐天花板，根治靠连接复用；
> 2. Nagle×延迟 ACK = 40ms 幽灵延迟，交互式服务一律 `setTcpNoDelay(true)`；
> 3. 百万连接 = 内存预算 + fd 上限 + 内核参数三件套，改参数必须压测留痕。

**思考题**：

1. accept 队列满说明什么问题？（→ 1.2：应用消费慢，不是内核）
2. 为什么"消灭 TIME_WAIT"是错误想法？（→ 2.1 正确性设计）
3. `tcp_tw_recycle` 为什么被移除？（→ 2.2 注释）
4. TCP keepalive 与 HTTP keep-alive 是一回事吗？（→ 4）

---

**下一模块**：[07-应用层高并发：连接复用与多路复用](07-应用层高并发：连接复用与多路复用.md)｜**返回总览**：[00-计算机网络高并发知识体系总览](00-计算机网络高并发知识体系总览.md)
