# 传输层：TCP与UDP
> 核心定位：传输层为应用层提供端到端的通信服务，核心解决进程寻址与可靠数据传输

## 目录
1. [传输层概述](#1-传输层概述)
2. [端口号机制](#2-端口号机制)
3. [UDP协议](#3-udp协议)
4. [TCP协议](#4-tcp协议)
5. [TCP三次握手与四次挥手](#5-tcp三次握手与四次挥手)
6. [TCP流量控制](#6-tcp流量控制)
7. [TCP拥塞控制](#7-tcp拥塞控制)
8. [TCP状态转换](#8-tcp状态转换)
9. [TCP与UDP对比总结](#9-tcp与udp对比总结)
10. [Java后端开发关联](#10-java后端开发关联)

---

## 1. 传输层概述

### 1.1 核心定位

传输层位于应用层与网络层之间，是"端到端通信"的核心枢纽。对上为应用进程提供通信服务，对下利用网络层的IP地址完成主机寻址，进一步通过**端口号**实现进程寻址。

```text
应用层         HTTP      SMTP      DNS      RTP
                ↓         ↓         ↓        ↓
传输层       ┌────── TCP ────┐  ┌─── UDP ───┐
                ↓         ↓         ↓        ↓
网络层       ┌─────────── IP ────────────────┐
                ↓         ↓         ↓        ↓
链路层       ┌──── Ethernet / WiFi / PPP ────┐
```

### 1.2 传输层核心功能

| 功能 | 说明 |
|------|------|
| **进程寻址** | 通过端口号标识同一主机上的不同应用进程 |
| **服务抽象** | 提供两种核心服务：面向连接的可靠服务（TCP）和无连接的高效服务（UDP） |
| **流量控制** | 控制发送速率，防止接收方缓冲区溢出 |
| **差错控制** | 通过校验和检测数据损坏，通过序号/确认/重传保证可靠交付 |
| **拥塞控制** | TCP特有的机制，在网络拥塞时降低发送速率，避免网络过载 |

### 1.3 传输层与网络层的分工

| 层面 | 寻址粒度 | 可靠性 | 核心设备 |
|------|---------|--------|---------|
| **网络层** | 主机到主机（IP地址） | 不可靠，尽力而为 | 路由器 |
| **传输层** | 进程到进程（端口号） | 可配置（TCP可靠/UDP不可靠） | 端系统（OS内核） |

> **核心理解**：网络层负责将数据送到目标主机，传输层负责将数据送到目标主机的目标进程。两层相互配合，上层不必关心下层如何转发。

---

## 2. 端口号机制

### 2.1 端口号范围

传输层使用16位端口号（0-65535）来标识应用进程。

| 分类 | 范围 | 说明 | 示例 |
|------|------|------|------|
| **Well-Known（知名端口）** | 0-1023 | 由IANA分配，固定对应特定服务 | HTTP=80, HTTPS=443 |
| **Registered（注册端口）** | 1024-49151 | 可由用户或应用程序注册使用 | MySQL=3306, Redis=6379 |
| **Dynamic/Private（动态/私有端口）** | 49152-65535 | 客户端临时端口，系统随机分配 | Java客户端Socket |

### 2.2 常见端口号速查表

| 端口 | 协议 | 服务 | 传输层协议 |
|------|------|------|-----------|
| 21 | FTP | 文件传输控制 | TCP |
| 22 | SSH | 安全远程登录 | TCP |
| 23 | Telnet | 远程登录（明文） | TCP |
| 25 | SMTP | 邮件发送 | TCP |
| 53 | DNS | 域名解析 | UDP（主）/TCP（辅） |
| 67/68 | DHCP | 动态IP分配 | UDP |
| 80 | HTTP | 超文本传输 | TCP |
| 110 | POP3 | 邮件接收 | TCP |
| 123 | NTP | 网络时间同步 | UDP |
| 143 | IMAP | 邮件访问 | TCP |
| 443 | HTTPS | 安全HTTP | TCP |
| 3306 | MySQL | 数据库 | TCP |
| 5432 | PostgreSQL | 数据库 | TCP |
| 6379 | Redis | 缓存 | TCP |
| 8080 | HTTP-Alt | HTTP备用端口 | TCP |
| 9092 | Kafka | 消息队列 | TCP |
| 27017 | MongoDB | 数据库 | TCP |

### 2.3 Socket地址

Socket地址 = IP地址 + 端口号，唯一标识一个通信端点。

```text
Socket地址 = IP:Port

示例：
服务器Socket：192.168.1.1:8080
客户端Socket：192.168.1.100:52001

一个TCP连接由四元组唯一标识：
（源IP, 源端口, 目的IP, 目的端口）
```

---

## 3. UDP协议

### 3.1 UDP核心特性

UDP（User Datagram Protocol，用户数据报协议）是传输层最简洁的协议。

| 特性 | 说明 |
|------|------|
| **无连接** | 通信前无需建立连接，直接发送数据，延迟极低 |
| **不可靠** | 不保证数据到达，不保证顺序，不保证不重复 |
| **面向数据报** | 以数据报为单位传输，有消息边界（应用层读取的单位与发送一致） |
| **头部开销小** | UDP首部仅8字节 |
| **无拥塞控制** | 发送速率不受网络拥塞限制（适合实时通信） |
| **支持广播和多播** | UDP天然支持一对多通信 |

### 3.2 UDP首部格式

UDP首部固定为8字节，极为简洁：

```text
   0               16                  31
   +----------------+------------------+
   |   源端口(16)    |   目的端口(16)    |
   +----------------+------------------+
   |   UDP长度(16)   |   校验和(16)     |
   +----------------+------------------+
   |           数据（可变长度）          |
   +-----------------------------------+
```

| 字段 | 长度 | 说明 |
|------|------|------|
| **源端口** | 2字节 | 发送方端口（可选，若不使用可置0） |
| **目的端口** | 2字节 | 接收方端口，必须指定 |
| **UDP长度** | 2字节 | UDP数据报总长度（首部+数据），最小8字节，最大65535字节 |
| **校验和** | 2字节 | 校验首部和数据的正确性（可选，0表示不校验） |

### 3.3 UDP应用场景

| 场景 | 原因 | 典型协议 |
|------|------|---------|
| **DNS查询** | 一次查询响应数据小，丢失重发代价低，无需TCP握手开销 | DNS（UDP 53） |
| **DHCP** | 设备获取IP时尚未建立TCP连接，需广播 | DHCP（UDP 67/68） |
| **视频/语音通话** | 实时性优先，丢包可容忍，不可接受延迟 | RTP、WebRTC |
| **在线游戏** | 低延迟要求高于可靠性，丢包可接受短暂卡顿 | 游戏自定义协议 |
| **日志采集** | 允许少量丢失，追求高吞吐低延迟 | Syslog |
| **广播/组播** | UDP支持一对多通信 | IGMP、组播视频流 |
| **IoT设备通信** | 资源受限设备，追求简单高效 | CoAP（基于UDP） |

### 3.4 UDP常见问题

| 问题 | 说明 | 解决方案 |
|------|------|----------|
| **数据丢失** | UDP无确认重传机制 | 应用层实现确认和重传 |
| **数据乱序** | UDP不保证数据报到达顺序 | 应用层添加序号，接收端排序 |
| **数据报大小限制** | 最大65507字节（受限于IP层） | 应用层拆分大数据 |
| **无流量控制** | 发送过快可能导致接收缓冲区溢出 | 应用层实现流量控制 |

---

## 4. TCP协议

### 4.1 TCP核心特性

TCP（Transmission Control Protocol，传输控制协议）是互联网最核心的传输层协议。

| 特性 | 说明 |
|------|------|
| **面向连接** | 通信前需通过三次握手建立连接，通信后通过四次挥手释放连接 |
| **全双工通信** | 双方可同时收发数据 |
| **可靠传输** | 通过序号、确认、重传、校验和等机制保证数据可靠交付 |
| **面向字节流** | 数据以字节流形式传输，无消息边界，应用层需自行分包 |
| **流量控制** | 通过滑动窗口机制控制发送速率，防止接收方溢出 |
| **拥塞控制** | 动态感知网络拥塞状态，调整发送速率 |
| **单播** | TCP仅支持一对一的单播通信，不支持广播和多播 |

### 4.2 TCP首部格式

TCP首部固定部分为20字节，选项部分最长40字节：

```text
   0                   4                   8                   12
   0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |         源端口(16)             |          目的端口(16)           |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |                      序号 Sequence Number (32)                 |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |                   确认号 Acknowledgment Number (32)             |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   | 首部长(4) |保留(6) |U|A|P|R|S|F|      窗口大小(16)             |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |          校验和(16)            |         紧急指针(16)           |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
   |                 选项（可选，最长40字节）                         |
   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
```

### 4.3 TCP首部字段详解

| 字段 | 长度 | 说明 |
|------|------|------|
| **源端口** | 2字节 | 发送方应用端口 |
| **目的端口** | 2字节 | 接收方应用端口 |
| **序号（Sequence Number）** | 4字节 | 本段数据第一个字节的序号，初始序号ISN随机生成 |
| **确认号（Acknowledgment Number）** | 4字节 | 期望收到的下一个字节序号，表示此序号之前的数据均已收到 |
| **首部长度（Data Offset）** | 4位 | TCP首部长度，以4字节为单位，典型值5（20字节） |
| **保留位** | 6位 | 保留未用，固定为0 |
| **标志位（Flags）** | 6位 | URG/ACK/PSH/RST/SYN/FIN |
| **窗口大小（Window Size）** | 2字节 | 接收方可用缓冲区大小，用于流量控制 |
| **校验和** | 2字节 | 校验TCP首部+数据+伪首部的正确性 |
| **紧急指针** | 2字节 | 指向紧急数据的最后一个字节（仅URG=1时有效） |
| **选项** | 0-40字节 | MSS、窗口缩放因子、SACK、时间戳等 |

### 4.4 TCP标志位详解

| 标志位 | 全称 | 说明 |
|-------|------|------|
| **SYN** | Synchronize | 建立连接时使用，同步序列号 |
| **ACK** | Acknowledgment | 确认字段有效，除初始SYN外所有报文都应设置 |
| **FIN** | Finish | 关闭连接时使用，表示发送方数据已发送完毕 |
| **RST** | Reset | 重置连接，通常表示异常或拒绝 |
| **PSH** | Push | 催促接收方立即将数据交付应用层 |
| **URG** | Urgent | 紧急指针字段有效 |

### 4.5 TCP可靠传输机制

| 机制 | 说明 |
|------|------|
| **校验和（Checksum）** | 检测数据在传输过程中是否损坏 |
| **序号与确认（Seq & Ack）** | 每字节编号，通过确认号告知接收情况 |
| **超时重传（Timeout Retransmission）** | 发送后开启定时器，超时未确认则重传 |
| **快速重传（Fast Retransmit）** | 收到3个重复ACK时立即重传，不等超时 |
| **累积确认（Cumulative ACK）** | ACK n 表示序号n-1及之前的数据全部收到 |

> **超时重传的核心**：RTO（Retransmission Timeout）基于RTT（Round Trip Time）动态计算，RTT通过Jacobson/Karn算法估算。RTO过小会导致不必要的重传，过大会降低重传效率。

---

## 5. TCP三次握手与四次挥手

### 5.1 三次握手（建立连接）

TCP三次握手是建立可靠连接的基础，确保双方的收发能力正常，并同步初始序列号（ISN）。

```text
Client                              Server
   |                                    |
   |-------- SYN (seq=x) -------------->|   Step 1: 客户端发送SYN，进入SYN_SENT
   |                                    |
   |<--- SYN+ACK (seq=y, ack=x+1) -----|   Step 2: 服务器回复SYN+ACK，进入SYN_RCVD
   |                                    |
   |-------- ACK (ack=y+1) ------------>|   Step 3: 客户端发送ACK，进入ESTABLISHED
   |                                    |   Step 3': 服务器收到ACK，进入ESTABLISHED
   |<========= 连接建立 ================>|
```

#### 每一步的详细说明

| 步骤 | 报文 | 发送方状态 | 接收方状态 | 说明 |
|------|------|-----------|-----------|------|
| 1 | SYN | CLOSED→SYN_SENT | LISTEN→SYN_RCVD | 客户端发送SYN，seq=x随机初始化 |
| 2 | SYN+ACK | SYN_SENT→ESTABLISHED | LISTEN→SYN_RCVD | 服务器确认客户端SYN（ack=x+1），并发送自己的SYN（seq=y） |
| 3 | ACK | ESTABLISHED | SYN_RCVD→ESTABLISHED | 客户端确认服务器的SYN（ack=y+1），连接正式建立 |

#### 为什么是三次而不是两次？

> **核心原因：防止"已失效的连接请求"到达服务器导致错误**
>
> 如果只用两次握手，场景如下：
> 1. 客户端发送SYN（seq=x）——因网络阻塞滞留
> 2. 客户端超时重发SYN（seq=z），成功建立连接并关闭
> 3. 滞留的旧SYN到达服务器，服务器误以为是新的连接请求，回复SYN+ACK
> 4. 客户端收到后，因为自己没有发起连接，回复RST——但服务器已经分配了资源
>
> 三次握手让服务器在收到ACK后才分配资源，避免上述问题。同时，三次握手也确保了双方都能确认对方的收发能力正常。

#### 三次握手的附带的参数协商

在三次握手过程中，通信双方会协商多个TCP参数：

| 参数 | 说明 | 协商时机 |
|------|------|---------|
| **MSS（Maximum Segment Size）** | 最大报文段大小，避免IP分片 | SYN中携带 |
| **窗口缩放因子（Window Scale）** | 将窗口大小从16位扩展到30位 | SYN中携带 |
| **SACK-Permitted** | 选择性确认，支持只重传丢失的部分 | SYN中携带 |

### 5.2 SYN Flood攻击

| 攻击类型 | 原理 | 影响 | 防御措施 |
|---------|------|------|---------|
| **SYN Flood** | 攻击者发送大量SYN包但不回复ACK，耗尽服务器的半连接队列 | 服务器无法接受正常连接 | SYN Cookie（不分配资源，直到收到ACK再建立连接） |

> **SYN Cookie原理**：服务器收到SYN后不立即分配半连接资源，而是根据源IP/端口、目的IP/端口和ISN计算一个cookie作为自己的初始序号；收到ACK时验证cookie的有效性，验证通过才分配资源建立连接。

### 5.3 四次挥手（释放连接）

TCP是全双工通信，双方需要独立关闭各自的数据传输方向，因此需要四次交互。

```text
主动关闭方                         被动关闭方
   |                                    |
   |-------- FIN (seq=u) -------------->|   Step 1: 主动方发送FIN，进入FIN_WAIT_1
   |                                    |
   |<------- ACK (ack=u+1) -------------|   Step 2: 被动方回复ACK，进入CLOSE_WAIT
   |    (主动方收到ACK进入FIN_WAIT_2)    |     （被动方还可继续发送数据）
   |                                    |
   |<------- FIN (seq=v) ---------------|   Step 3: 被动方发送FIN，进入LAST_ACK
   |    (主动方收到FIN进入TIME_WAIT)     |
   |-------- ACK (ack=v+1) ------------>|   Step 4: 主动方回复ACK，进入TIME_WAIT
   |    (被动方收到ACK进入CLOSED)        |     （等待2MSL后自动变为CLOSED）
```

#### 为什么是四次而不是三次？

> 因为全双工通信的关闭需要双方各自确认数据发送完毕：
>
> - 主动方发FIN：表示"我没有数据要发了"
> - 被动方回复ACK：表示"我知道你发完了"
> - 被动方发FIN：表示"我也没数据要发了"（可能需要等未完成的数据发送完毕）
> - 主动方回复ACK：表示"我知道你也发完了"
>
> 第二步和第三步之间可能有间隔（被动方还有数据待发送），所以无法合并。

#### TIME_WAIT详解

| 问题 | 回答 |
|------|------|
| **TIME_WAIT持续时间** | 2MSL（Maximum Segment Lifetime），通常为1-4分钟 |
| **为什么需要TIME_WAIT** | 1. 确保最后的ACK能被被动关闭方收到（若丢失，被动方会重发FIN）<br>2. 让旧连接的报文在网络中过期消失，避免与新连接混淆 |
| **TIME_WAIT过多的影响** | 占用端口资源，高并发短连接场景可能导致端口耗尽 |
| **优化方案** | 使用长连接复用（连接池）；设置`SO_REUSEADDR`；调整`tcp_tw_reuse`（Linux） |

#### CLOSE_WAIT状态过多

> **常见开发Bug**：服务器端`CLOSE_WAIT`过多，通常是应用层没有正确调用`close()`导致的。被动关闭方收到FIN后回复ACK进入CLOSE_WAIT，如果应用程序忘记关闭Socket，会一直停留在CLOSE_WAIT，造成资源泄漏。

---

## 6. TCP流量控制

### 6.1 滑动窗口机制

流量控制的目的是防止发送方发送数据过快，导致接收方缓冲区溢出。TCP通过**滑动窗口（Sliding Window）**机制实现流量控制。

```text
发送方已发送的数据：
+----------+----------+----------+----------+----------+
| 已确认   | 已发送   | 可发送   | 不可发送           |
|          | 未确认   |          |                    |
+----------+----------+----------+----------+----------+
           ^          ^          ^
           |          |          |
        已确认边界  已发送边界  窗口右边界
                     ←--窗口大小--→
```

| 概念 | 说明 |
|------|------|
| **rwnd（receiver window）** | 接收方通告的窗口大小，表示接收方还有多少缓冲区可用 |
| **发送窗口** | 发送方可连续发送的数据量，由接收方窗口（rwnd）和拥塞窗口（cwnd）共同决定 |
| **累计确认** | ACK n 表示序号n-1及之前的所有数据均已正确接收 |
| **零窗口** | rwnd=0时发送方停止发送，启动"持续计时器"等待接收方窗口更新 |

### 6.2 滑动窗口大小变化

```text
接收方缓冲区：
+------------------+--------------------+------------------+
|   已读取的数据    |   空闲缓冲区       |   未读取的数据   |
+------------------+--------------------+------------------+
                    ↑                   ↑
                    |                   |
                 rwnd（空闲容量）    已接收未读取
                   ↓ 发送方据此控制发送
```

> **发送窗口大小 = min(rwnd, cwnd)**
> 发送窗口不仅受接收方缓冲区的限制，还受到网络拥塞状态的影响。

### 6.3 Nagle算法与TCP_NODELAY

| 算法 | 说明 | 适用场景 |
|------|------|---------|
| **Nagle算法** | 将多个小数据包合并为一个大包再发送，减少网络中的小包数量 | 批量数据传输（吞吐量优先） |
| **TCP_NODELAY** | 禁用Nagle算法，数据到达后立即发送 | 实时交互场景（延迟优先） |

> **TCP_NODELAY使用建议**：
> - 低延迟应用（游戏、实时通信）：应开启TCP_NODELAY
> - 文件传输、大数据批量提交：关闭TCP_NODELAY（使用Nagle算法）效率更高
>
> Java中设置：`socket.setTcpNoDelay(true);`

### 6.4 糊涂窗口综合征

当接收方每次只腾出少量缓冲区（如1字节）就通告给发送方，会导致发送方每次都发送很小的数据段，浪费网络带宽。

| 解决方案 | 说明 |
|---------|------|
| **接收方：延迟确认** | 接收方不立即发送窗口更新，等积累足够空间再通告 |
| **发送方：Clark算法** | 窗口大小小于MSS时，不发送数据（仅可发送已积累到MSS的数据） |

---

## 7. TCP拥塞控制

### 7.1 拥塞控制概述

拥塞控制与流量控制的区别：

| 控制类型 | 控制对象 | 目标 |
|---------|---------|------|
| **流量控制** | 接收方缓冲区（rwnd） | 防止接收方被淹没 |
| **拥塞控制** | 网络中间设备（路由器） | 防止网络过载 |

TCP通过维护**拥塞窗口（cwnd，congestion window）**来感知和控制网络拥塞。

### 7.2 拥塞控制四阶段

```text
拥塞窗口cwnd
    ^
    |  慢启动            拥塞避免              拥塞发生
    |  (指数增长)         (线性增长)            (窗口减半)
    |
    |         /
    |       /  
    |     /  
    |   /
    | / ssthresh
    +-------------------------------------------> 时间
```

#### 慢启动（Slow Start）

| 特性 | 说明 |
|------|------|
| **增长方式** | 指数增长，每个RTT内cwnd翻倍 |
| **初始值** | 初始cwnd=10 MSS（RFC 6928，旧标准=1 MSS） |
| **结束条件** | cwnd >= ssthresh（慢启动阈值），或发生丢包 |

#### 拥塞避免（Congestion Avoidance）

| 特性 | 说明 |
|------|------|
| **增长方式** | 线性增长，每个RTT内cwnd增加1 MSS |
| **目的** | 缓慢探测网络容量，避免突然拥塞 |

#### 快速重传（Fast Retransmit）

> 发送方收到3个相同的重复ACK时，立即重传丢失的数据段，而不等待超时计时器

#### 快速恢复（Fast Recovery）

收到3个重复ACK后的处理：

```text
1. ssthresh = cwnd / 2（减半）
2. cwnd = ssthresh + 3（加上已收到的3个重复ACK对应的数据）
3. 进入拥塞避免阶段（线性增长）
```

#### 超时后的处理

```text
1. ssthresh = cwnd / 2
2. cwnd = 1（重新开始）
3. 进入慢启动阶段
```

### 7.3 拥塞控制算法对比

| 算法 | 特点 | Linux默认 | 适用场景 |
|------|------|----------|---------|
| **Reno** | 经典算法，快速重传+快速恢复 | 否 | 传统场景 |
| **CUBIC** | 基于三次函数的增长，高BDP下占优 | 是（Linux 2.6.19+） | 高带宽、长距离链路 |
| **BBR** | Google提出，基于带宽和RTT建模，不依赖丢包 | 可选（Linux 4.9+） | 高速网络、高延迟链路 |

> **BBR（Bottleneck Bandwidth and Round-trip propagation time）**：不同于传统基于丢包的拥塞控制，BBR通过测量瓶颈带宽和传播延迟，主动控制发送速率。在丢包率较高的网络（如无线网络、长肥网络）中表现显著优于CUBIC。

### 7.4 拥塞控制与Java后端

| 场景 | 影响 | 优化建议 |
|------|------|---------|
| **高延迟链路** | cwnd增长慢，带宽利用率低 | 调整初始cwnd；考虑使用BBR |
| **频繁丢包** | 传统算法误判为拥塞，降低发送速率 | 启用SACK；考虑BBR |
| **短连接** | 慢启动阶段占比较大，带宽利用率低 | 使用长连接（连接池） |

---

## 8. TCP状态转换

### 8.1 TCP状态机完整图

```text
                            ┌─────────────┐
                            │   CLOSED    │
                            └──────┬──────┘
                                   │ socket/bind/listen
                                   ▼
                            ┌─────────────┐
                    ┌───────│   LISTEN    │◄───────┐
                    │       └──────┬──────┘        │
                    │              │ 收到SYN        │
                    │              ▼                │
          ┌─────────┴─────┐  ┌─────────────┐       │
          │   SYN_SENT    │  │  SYN_RCVD   │       │
          │  (客户端发SYN) │  │ (服务器收SYN)│       │
          └──────┬───────┘  └──────┬──────┘       │
                 │ 收到SYN+ACK     │ 收到ACK        │
                 └────────┬───────┘                │
                          ▼                        │
                   ┌─────────────┐                 │
                   │ ESTABLISHED │                 │
                   └──────┬──────┘                 │
                          │                        │
               ┌──────────┴──────────┐             │
               │                     │             │
        主动关闭方              被动关闭方          │
               │                     │             │
               ▼                     ▼             │
        ┌─────────────┐      ┌─────────────┐      │
        │  FIN_WAIT_1 │      │  CLOSE_WAIT │      │
        └──────┬──────┘      └──────┬──────┘      │
               │ 收到ACK            │ 发FIN        │
               ▼                    ▼              │
        ┌─────────────┐      ┌─────────────┐      │
        │  FIN_WAIT_2 │      │   LAST_ACK  │      │
        └──────┬──────┘      └──────┬──────┘      │
               │ 收到FIN            │ 收到ACK      │
               ▼                    └──────────────┘
        ┌─────────────┐
        │  TIME_WAIT  │
        │  (等待2MSL) │
        └──────┬──────┘
               │ 超时
               ▼
            ┌─────────────┐
            │   CLOSED    │
            └─────────────┘
```

### 8.2 TCP状态详解

| 状态 | 角色 | 说明 |
|------|------|------|
| **CLOSED** | 双方 | 初始状态，Socket未创建或已关闭 |
| **LISTEN** | 服务器 | 等待客户端的连接请求 |
| **SYN_SENT** | 客户端 | 已发送SYN，等待服务器的SYN+ACK |
| **SYN_RCVD** | 服务器 | 已收到SYN并回复SYN+ACK，等待客户端的ACK |
| **ESTABLISHED** | 双方 | 连接已建立，可正常收发数据 |
| **FIN_WAIT_1** | 主动关闭方 | 已发送FIN，等待ACK |
| **FIN_WAIT_2** | 主动关闭方 | 已收到ACK，等待对方的FIN |
| **TIME_WAIT** | 主动关闭方 | 已收到FIN并回复ACK，等待2MSL后自动关闭 |
| **CLOSE_WAIT** | 被动关闭方 | 已收到FIN并回复ACK，等待应用层调用close() |
| **LAST_ACK** | 被动关闭方 | 应用层已调用close()，发送了FIN，等待对方ACK |

### 8.3 状态转换路径汇总

```text
服务器端完整路径：
CLOSED → LISTEN → SYN_RCVD → ESTABLISHED → CLOSE_WAIT → LAST_ACK → CLOSED

客户端完整路径：
CLOSED → SYN_SENT → ESTABLISHED → FIN_WAIT_1 → FIN_WAIT_2 → TIME_WAIT → CLOSED
```

### 8.4 Socket选项

| 选项 | 说明 | 使用场景 |
|------|------|---------|
| **SO_REUSEADDR** | 允许端口快速复用，即使TIME_WAIT状态也可绑定 | 服务器重启，避免bind失败 |
| **SO_KEEPALIVE** | 启用TCP保活探测，检测连接是否仍然有效 | 长连接场景，检测死连接 |
| **TCP_NODELAY** | 禁用Nagle算法，小数据包立即发送 | 低延迟交互场景 |
| **SO_RCVBUF/SO_SNDBUF** | 设置接收/发送缓冲区大小 | 大数据量传输场景 |
| **SO_LINGER** | 控制close()行为，设置等待未发送数据的时间 | 强制关闭连接 |

---

## 9. TCP与UDP对比总结

### 9.1 综合对比表

| 对比维度 | TCP | UDP |
|---------|-----|-----|
| **连接方式** | 面向连接（三次握手/四次挥手） | 无连接，直接发送 |
| **可靠性** | 可靠（确认、重传、排序、校验和） | 不可靠（尽力而为，不确认不重传） |
| **传输单位** | 字节流（无消息边界，需应用层分包） | 数据报（保留消息边界） |
| **首部开销** | 20~60字节 | 8字节 |
| **传输效率** | 较低（握手、确认、拥塞控制等开销） | 高（无额外控制机制） |
| **流量控制** | 支持（滑动窗口，rwnd） | 不支持 |
| **拥塞控制** | 支持（慢启动、拥塞避免等） | 不支持 |
| **双工通信** | 全双工 | 全双工 |
| **广播/多播** | 不支持（仅单播） | 支持 |
| **数据有序性** | 保证有序（序号机制） | 不保证有序 |
| **适用场景** | HTTP、RPC、数据库、文件传输 | DNS、DHCP、视频/语音、游戏 |
| **Java类** | `Socket` / `ServerSocket` | `DatagramSocket` / `DatagramPacket` |

### 9.2 选型决策树

```text
需要可靠传输？
  ├── 是 → 需要有序交付？
  │         ├── 是 → 使用 TCP
  │         └── 否 → 考虑使用 TCP（应用层处理序号）
  └── 否 → 实时性要求高？
            ├── 是 → 使用 UDP（应用层自行处理可靠性）
            └── 否 → 小数据量单次查询？
                      ├── 是 → UDP（如DNS/DHCP）
                      └── 否 → TCP
```

### 9.3 场景推荐

| 业务场景 | 推荐协议 | 原因 |
|---------|---------|------|
| HTTP/RESTful API | TCP | 可靠性优先 |
| RPC（gRPC/Dubbo） | TCP | 可靠+流式传输 |
| 数据库连接 | TCP | 不能丢数据 |
| 消息队列（Kafka/RabbitMQ） | TCP | 可靠投递 |
| DNS查询 | UDP（主） | 一次查询，低延迟 |
| 视频直播 | UDP | 实时性优先 |
| 在线游戏 | UDP | 低延迟+应用层可靠性 |
| 日志采集 | UDP | 高吞吐，允许少量丢失 |
| 服务注册发现 | UDP+TCP | 心跳UDP，同步TCP |

---

## 10. Java后端开发关联

### 10.1 Java Socket编程基础

#### TCP Socket服务器

```java
import java.io.*;
import java.net.*;

public class TcpServer {
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(8080);
        System.out.println("TCP服务器已启动，监听端口8080...");
        while (true) {
            try (Socket client = serverSocket.accept()) {
                BufferedReader br = new BufferedReader(
                    new InputStreamReader(client.getInputStream()));
                PrintWriter pw = new PrintWriter(
                    client.getOutputStream(), true);
                String request = br.readLine();
                System.out.println("收到: " + request);
                pw.println("响应: " + request);
            }
        }
    }
}
```

#### TCP Socket客户端

```java
import java.io.*;
import java.net.*;

public class TcpClient {
    public static void main(String[] args) throws IOException {
        // connect超时：5秒
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress("127.0.0.1", 8080), 5000);
        try {
            PrintWriter pw = new PrintWriter(
                socket.getOutputStream(), true);
            BufferedReader br = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));
            pw.println("Hello");
            String response = br.readLine();
            System.out.println("服务端响应: " + response);
        } finally {
            socket.close();
        }
    }
}
```

#### UDP Socket

```java
// UDP服务器
DatagramSocket udpServer = new DatagramSocket(9999);
byte[] buffer = new byte[65535];
DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
udpServer.receive(packet);  // 阻塞接收
String data = new String(packet.getData(), 0, packet.getLength());
System.out.println("收到UDP数据: " + data);

// UDP客户端
DatagramSocket udpClient = new DatagramSocket();
byte[] sendBuf = "Hello UDP".getBytes();
DatagramPacket sendPkt = new DatagramPacket(
    sendBuf, sendBuf.length,
    InetAddress.getByName("127.0.0.1"), 9999);
udpClient.send(sendPkt);
udpClient.close();
```

### 10.2 TCP参数调优

| 参数 | Java设置 | 说明 | 推荐值 |
|------|---------|------|--------|
| **连接超时** | `new Socket().connect(addr, timeout)` | connect等待时间 | 3000~5000ms |
| **SO_TIMEOUT** | `socket.setSoTimeout(timeout)` | read等待时间 | 5000~10000ms |
| **TCP_NODELAY** | `socket.setTcpNoDelay(true)` | 禁用Nagle算法 | 低延迟场景开启 |
| **SO_REUSEADDR** | `serverSocket.setReuseAddress(true)` | 端口复用 | 服务器重启使用 |
| **SO_KEEPALIVE** | `socket.setKeepAlive(true)` | 保活探测 | 长连接开启 |
| **SO_RCVBUF** | `socket.setReceiveBufferSize(size)` | 接收缓冲区 | 64KB~1MB |
| **SO_SNDBUF** | `socket.setSendBufferSize(size)` | 发送缓冲区 | 64KB~1MB |

### 10.3 连接池与长连接

```java
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

public class HttpClientPool {
    public static CloseableHttpClient createPooledClient() {
        PoolingHttpClientConnectionManager cm =
            new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(200);           // 最大连接数
        cm.setDefaultMaxPerRoute(50);  // 每个路由最大连接数

        return HttpClients.custom()
            .setConnectionManager(cm)
            .setConnectionTimeToLive(30, java.util.concurrent.TimeUnit.SECONDS)
            .evictIdleConnections(60, java.util.concurrent.TimeUnit.SECONDS)
            .build();
    }
}
```

### 10.4 Netty框架中的TCP优化

```java
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

public class NettyTcpServer {
    public static void main(String[] args) throws InterruptedException {
        NioEventLoopGroup boss = new NioEventLoopGroup(1);
        NioEventLoopGroup worker = new NioEventLoopGroup();
        try {
            new ServerBootstrap()
                .group(boss, worker)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 1024)        // 半连接队列大小
                .childOption(ChannelOption.TCP_NODELAY, true)  // 禁用Nagle
                .childOption(ChannelOption.SO_KEEPALIVE, true) // 保活探测
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline()
                            .addLast(new StringDecoder())
                            .addLast(new StringEncoder())
                            .addLast(new SimpleChannelInboundHandler<String>() {
                                @Override
                                protected void channelRead0(
                                        ChannelHandlerContext ctx, String msg) {
                                    System.out.println("收到: " + msg);
                                    ctx.writeAndFlush("服务端响应: " + msg);
                                }
                            });
                    }
                })
                .bind(8080).sync()
                .channel().closeFuture().sync();
        } finally {
            boss.shutdownGracefully();
            worker.shutdownGracefully();
        }
    }
}
```

### 10.5 生产环境常见问题

| 问题 | 原因 | 排查方法 | 解决方案 |
|------|------|---------|---------|
| **TCP连接超时** | 三次握手失败、网络延迟高、防火墙拦截 | ping/telnet确认连通性 | 调整connectTimeout；检查防火墙 |
| **大量TIME_WAIT** | 高并发短连接 | `netstat -an丨grep TIME_WAIT丨wc -l`（Linux） | 使用连接池；启用`SO_REUSEADDR` |
| **大量CLOSE_WAIT** | 应用层未正确关闭Socket | `netstat -an丨grep CLOSE_WAIT` | 检查代码中close()调用 |
| **TCP粘包/拆包** | 字节流无消息边界 | Wireshark抓包分析 | 自定义协议（分隔符/长度字段）；Netty内置处理器 |
| **端口耗尽** | 短连接过多，端口被TIME_WAIT占用 | `netstat -an丨grep ESTABLISHED丨wc -l` | 使用连接池；增大临时端口范围 |
| **TCP RST异常** | 连接被强制重置 | Wireshark抓包查看RST标志 | 检查应用层异常关闭、超时配置 |
| **UDP数据丢失** | 无确认重传、接收缓冲区溢出 | 抓包分析丢包率 | 应用层自实现确认重传；增大接收缓冲区 |

### 10.6 排查命令速查

```bash
# 查看所有TCP连接状态分布（Linux）
ss -s

# 查看端口监听情况
ss -tlnp

# 查看特定端口的连接状态
ss -tan state ESTABLISHED

# 查看TIME_WAIT连接数
ss -tan state TIME_WAIT | wc -l

# Windows等效命令
netstat -an | findstr "TIME_WAIT"
netstat -an | findstr "LISTEN"
```

> **⚠️ TIME_WAIT和端口耗尽警告**：
> 在高并发短连接场景（如HTTP/1.0、短连接HTTP客户端），主动关闭方会产生大量TIME_WAIT状态连接。单个端口在2MSL（约1~4分钟）内不可重用，临时端口范围有限（默认约28232个），极限QPS下可能耗尽端口。
>
> **优化方案**：
> 1. 使用长连接（连接池）复用TCP连接，避免频繁创建/关闭
> 2. 启用`SO_REUSEADDR`允许端口快速复用
> 3. Linux调整`net.ipv4.tcp_tw_reuse`和`net.ipv4.tcp_tw_recycle`参数
> 4. 增大临时端口范围：`net.ipv4.ip_local_port_range`

> **💡 TCP_NODELAY低延迟优化**：
> 默认Nagle算法会等待缓冲区积累到MSS或收到ACK后才发送数据，这在交互式应用中会造成明显的延迟。对于实时通信、游戏、高频交易等低延迟场景，应通过`setTcpNoDelay(true)`禁用Nagle算法，让每次write()立即发送。
>
> Java设置：`socket.setTcpNoDelay(true)` 或 Netty中 `ChannelOption.TCP_NODELAY, true`

> **🎯 传输层核心要义**：
> - TCP：面向连接、可靠、字节流——支付、核心业务接口必须使用TCP
> - UDP：无连接、快速、数据报——实时通信、日志收集、DNS查询选择UDP
> - 三次握手建立连接，四次挥手关闭连接，TIME_WAIT/CLOSE_WAIT是排查重点
> - 流量控制取决于接收方（rwnd），拥塞控制感知网络（cwnd）
> - Java后端核心优化：连接池 + 超时配置 + TCP_NODELAY + 缓冲区调整
