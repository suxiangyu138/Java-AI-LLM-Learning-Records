# 03-TCP 连接问题排查
> 连接超时、拒绝、重置（RST）、TIME_WAIT、CLOSE_WAIT、SYN 队列溢出——TCP 六类高频问题的现象、根因与解决

## 📚 目录
1. [TCP 状态机速览](#1-tcp-状态机速览)
2. [连接超时（Timeout）](#2-连接超时timeout)
3. [连接拒绝（Refused）](#3-连接拒绝refused)
4. [连接重置（RST）](#4-连接重置rst)
5. [TIME_WAIT 堆积](#5-time_wait-堆积)
6. [CLOSE_WAIT 堆积（泄漏信号）](#6-close_wait-堆积泄漏信号)
7. [SYN 队列与 Accept 队列溢出](#7-syn-队列与-accept-队列溢出)
8. [抓包分析实战](#8-抓包分析实战)
9. [核心要点](#9-核心要点)
10. [参考来源](#10-参考来源)

## 1. TCP 状态机速览

```text
客户端                          服务端
  │  SYN        ──────────────▶   LISTEN
  │                              SYN_RECV（半连接队列）
  │  SYN+ACK    ◀──────────────
  │  ACK        ──────────────▶   ESTABLISHED（Accept 队列）
  │  ...数据...  ◀─────────────▶
  │  FIN        ──────────────▶   CLOSE_WAIT
  │  ACK        ◀──────────────
  │  FIN        ◀──────────────
  │  ACK        ──────────────▶
  │  TIME_WAIT（2MSL）
```

| 状态 | 含义 | 问题信号 |
|------|------|---------|
| SYN_SENT | 客户端等待 SYN-ACK | 网络/防火墙/服务端队列满 |
| SYN_RECV | 服务端等待 ACK（半连接） | **SYN 队列溢出** |
| ESTABLISHED | 正常通信 | 数量/增长趋势 |
| CLOSE_WAIT | 对端已关，应用未关 | **应用泄漏** |
| TIME_WAIT | 主动关闭等待 2MSL | 大量堆积（连接频繁创建） |

## 2. 连接超时（Timeout）

### 2.1 现象与定位

```text
现象：客户端报 Connection timed out（或 Java SocketTimeoutException: connect timed out）
```

```bash
# 定位四步
ping -c 3 ip                     # ① 网络层通吗？
traceroute -n ip                 # ② 路径通吗？
telnet ip port                   # ③ 端口通吗？（也超时 → 不是应用问题）
ss -tlnp | grep port             # ④ 服务端监听了吗？
```

### 2.2 根因矩阵

| 根因 | 特征 | 解决 |
|------|------|------|
| 网络不通 | ping 不通 | 路由/物理链路 |
| 防火墙 drop | ping 通、telnet 超时 | 防火墙放行（`firewall-cmd --add-port`） |
| SYN 队列满 | 服务端 `ss -tn state syn-recv` 大量 | 调大 backlog（见第 7 节） |
| 服务端负载过高 | CPU 高/GC 频繁 | 扩容/优化 |
| 服务端内核参数 | `net.ipv4.tcp_syn_retries` 等 | 内核调优（谨慎） |

> 💡 **超时 vs 拒绝的分水岭**：超时 = "包被丢了"（防火墙 drop/网络/队列满）；拒绝 = "包被回了 RST"（端口没监听/防火墙 reject）——telnet 的两种输出直接区分。

## 3. 连接拒绝（Refused）

```text
现象：Connection refused（Java：ConnectException）
```

```bash
ss -tlnp | grep 8080        # 端口监听了吗？
# 没输出 → 应用没启动 / 绑定错地址
# 有输出 → 看 Listen 地址（0.0.0.0 vs 127.0.0.1）
```

| 根因 | 检查 | 解决 |
|------|------|------|
| 应用没启动 | `ss -tlnp` 无监听 | 启动应用 |
| 绑定 127.0.0.1 | Listen 地址是 127.0.0.1 | 改绑 0.0.0.0 |
| 端口被占用换端口 | `ss -tlnp` 看占用进程 | 换端口/停占用 |
| 防火墙 reject | `iptables -L` / `firewall-cmd --list-all` | 防火墙放行 |
| 容器端口未映射 | Docker 场景（见 [07](07-容器与微服务网络排查.md)） | `-p` 映射 |

> ⚠️ **经典坑**：Spring Boot 配了 `server.address=127.0.0.1`——容器/远程访问全部 refused，而"本地明明能连"。绑定地址是拒绝类问题的首要检查项。

## 4. 连接重置（RST）

### 4.1 常见触发场景

| 场景 | 说明 |
|------|------|
| 端口未监听 | 对端回 RST（快速失败而非超时） |
| 防火墙 reject | 显式拒绝回 RST |
| 连接被中断 | 对端进程崩溃/连接超时被关 |
| 协议错误 | 向 HTTP 端口发非 HTTP 流量 |
| 负载均衡/网关重置 | 上游超时主动 RST |
| 连接池超时回收 | 池中空闲连接被服务端关闭后复用 |

### 4.2 定位

```bash
# 统计 RST
netstat -s | grep -i reset
ss -s

# 抓包看 RST 方向与时机
tcpdump -nn port 8080 'tcp[tcpflags] & tcp-rst != 0'
# 关键：RST 是客户端发的还是服务端发的？发生在哪个阶段？
```

```text
抓包判断：
  连接建立后立刻 RST ← 协议错误/端口不对
  空闲一段时间后 RST ← 服务端/中间设备空闲超时（连接池复用触发）
  请求处理中 RST ← 上游超时/应用崩溃
```

> 💡 Java 场景：**连接池中的空闲连接被服务端（或防火墙/负载均衡）超时回收**，复用时报 RST——解决：连接池配验证（`testOnBorrow`/`validationQuery`）+ 空闲超时小于对端。

## 5. TIME_WAIT 堆积

### 5.1 为什么会堆积

```text
主动关闭方（通常是客户端/短连接发起方）关闭连接后进入 TIME_WAIT，等待 2MSL（约 60s）
高并发短连接场景 → 大量 TIME_WAIT

危害：端口耗尽（客户端）/"Cannot assign requested address"
```

```bash
ss -tn state time-wait | wc -l     # 数量
ss -s                              # timewait 统计
```

### 5.2 解决方向

| 方案 | 说明 | 优先级 |
|------|------|:---:|
| **连接复用（连接池）** | 减少连接创建/关闭频率 | ✅ 首选（Java 场景天然适用） |
| 调大本地端口范围 | `net.ipv4.ip_local_port_range` | ✅ 快速缓解 |
| 开启 timewait reuse | `net.ipv4.tcp_tw_reuse=1`（仅客户端出站） | ⚠️ 谨慎 |
| timewait 回收 | `tcp_tw_recycle` **已废弃且危险**（NAT 场景丢包） | ❌ 禁用 |

> ⚠️ **内核参数纪律**：`tcp_tw_reuse` 只影响客户端出站连接（配合时间戳）；`tcp_tw_recycle` 在 NAT 环境会造成随机丢包——**现代内核不建议开启 recycle**。优先解法永远是连接池复用。

## 6. CLOSE_WAIT 堆积（泄漏信号）

```text
现象：ss -tn state close-wait 数量持续增长
原理：对端已发 FIN，应用未调用 close（连接泄漏）
后果：句柄/线程耗尽 → 无法接受新连接
```

```bash
ss -tn state close-wait | wc -l          # 确认堆积
ss -tnp state close-wait                 # 看哪个进程
jstack <pid> | grep -A 5 'wait'          # 应用线程状态（Java 视角）
```

| Java 根因 | 场景 |
|-----------|------|
| HTTP 客户端未关闭响应体 | 读取响应后未 close/释放 |
| 连接池泄漏 | 借出连接未归还 |
| 异常路径未释放 | try 缺 finally 关闭 |
| 响应体未消费完 | 未读完整响应就 close（或相反） |

```java
// ✅ 正确姿势：try-with-resources 保证关闭
try (InputStream is = response.getEntity().getContent()) {
    // 读取
} catch (IOException e) {
    log.error("请求失败", e);
}
```

> 🎯 **CLOSE_WAIT 是"应用层泄漏"的网络表现**：不是网络问题，是代码问题——数量持续增长必须查代码（连接/流未关闭），重启只能缓解。

## 7. SYN 队列与 Accept 队列溢出

### 7.1 队列机制

```text
服务端两层队列：
  SYN 队列（半连接）：握手未完成（默认 1024）
  Accept 队列（全连接）：握手完成等待应用 accept（backlog 参数）

溢出表现：客户端 connect 超时 / 偶发失败 / "connection reset by peer"
```

```bash
# 看溢出（内核计数）
netstat -s | grep -iE 'overflow|drop'
# SYN_RECV 堆积
ss -tn state syn-recv | wc -l

# backlog 配置（Java）
# Tomcat: server.tomcat.accept-count=100
# Netty: .option(ChannelOption.SO_BACKLOG, 1024)
```

### 7.2 解决

| 手段 | 说明 |
|------|------|
| 调大应用 backlog | Tomcat accept-count / Netty SO_BACKLOG |
| 调大内核队列 | `net.core.somaxconn`、`net.ipv4.tcp_max_syn_backlog` |
| 根本优化 | 缩短握手时间（Fast Open 谨慎）、扩容实例 |

> 💡 队列溢出 = "服务端忙不过来"的信号：先看应用处理能力（线程池/连接池），再调队列参数——**调队列是缓解，优化处理是根治**。

## 8. 抓包分析实战

```bash
# 案例：偶发连接失败
# ① 抓包
tcpdump -nn port 8080 -w /tmp/cap.pcap -c 5000

# ② 分析握手失败（只有 SYN 无 SYN-ACK）
tshark -r /tmp/cap.pcap -Y "tcp.flags.syn==1 && tcp.flags.ack==0"

# ③ 分析 RST
tshark -r /tmp/cap.pcap -Y "tcp.flags.reset==1"

# ④ 连接耗时分布
tshark -r /tmp/cap.pcap -q -z conv,tcp | sort -k 6 -rn | head
```

| 抓包结论 | 下一步 |
|----------|--------|
| SYN 无响应 | 防火墙/网络/队列（查服务端） |
| 立即 RST | 端口未监听/防火墙 reject |
| 握手成功但业务失败 | 应用层（看日志/超时配置） |
| 偶发丢包 | 网络质量（mtr 持续观察） |

## 9. 核心要点

> 🎯 **核心要点**：
> - 六类问题定位：超时（包被丢）、拒绝（RST 回应）、RST（中断/超时回收）、TIME_WAIT（短连接多）、CLOSE_WAIT（**代码泄漏**）、队列溢出（服务端忙）；
> - 超时 vs 拒绝：telnet 输出直接区分（drop vs reject）；
> - TIME_WAIT：连接池复用是首选解；`tcp_tw_recycle` 禁用；
> - CLOSE_WAIT：是代码问题不是网络问题——try-with-resources 防泄漏；
> - 队列溢出：调 backlog 是缓解，优化处理能力是根治；
> - 抓包看三点：SYN 是否被回、RST 方向与时机、握手是否完成。

## 10. 参考来源

- [RFC 9293（TCP）](https://www.rfc-editor.org/rfc/rfc9293)
- [TCP 状态机（tcpipguide）](http://www.tcpipguide.com/free/t_TCPOperationalOverviewandtheTCPFiniteStateMachineF-2.htm)
- [ss 手册](https://man7.org/linux/man-pages/man8/ss.8.html)
- [内核网络参数文档（tcp-tw-reuse 等）](https://docs.kernel.org/networking/ip-sysctl.html)

---

**下一模块**：[04-DNS与域名问题排查](04-DNS与域名问题排查.md)　/　**返回总览**：[00-总览](00-网络问题排查总览.md)
