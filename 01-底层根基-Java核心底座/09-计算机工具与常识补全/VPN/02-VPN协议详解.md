# VPN 协议详解

> 📡 WireGuard 的极简、OpenVPN 的稳健、IPsec 的标准、L2TP 的兼容 —— 彻底搞懂每个协议的基因

---

## 📚 目录

1. [WireGuard —— 现代 VPN 的标杆](#1-wireguard--现代-vpn-的标杆)
2. [OpenVPN —— 久经考验的通用方案](#2-openvpn--久经考验的通用方案)
3. [IPsec/IKEv2 —— 企业级标准](#3-ipsecikev2--企业级标准)
4. [L2TP/IPsec —— 移动端兼容利器](#4-l2tpipsec--移动端兼容利器)
5. [PPTP —— 已死的协议](#5-pptp--已死的协议)
6. [协议全景对比](#6-协议全景对比)
7. [协议选型决策树](#7-协议选型决策树)

---

## 1. WireGuard —— 现代 VPN 的标杆

### 1.1 一句话定位

> WireGuard 是 Jason A. Donenfeld 在 2018 年创建的新一代 VPN 协议，2020 年进入 Linux 5.6 内核主线。仅 **4000 行代码**（OpenVPN 约 10 万行），无算法协商，无复杂配置，代表了 VPN 协议的未来。

### 1.2 核心设计哲学

```
传统 VPN 的设计哲学：              WireGuard 的设计哲学：
┌─────────────────────┐          ┌─────────────────────┐
│ 协商加密算法          │          │ 固定一套加密套件      │
│ 协商认证方式          │          │ 密钥即身份            │
│ 复杂的连接状态机       │          │ 无连接 (UDP)         │
│ 成千上万行配置         │          │ 一个 IP + 一个密钥对  │
│ 需要 CA 证书链        │          │ 仅交换公钥            │
│ 连上≠通信（可能卡住）  │          │ 连上=通信             │
└─────────────────────┘          └─────────────────────┘
```

### 1.3 密码学套件（内置，不可协商）

| 组件 | 算法 | 作用 |
|------|------|------|
| **对称加密** | ChaCha20 (256-bit) | 加密实际数据 |
| **认证** | Poly1305 | 消息认证码，防篡改 |
| **密钥交换** | Curve25519 (ECDH) | 安全协商对称密钥 |
| **哈希** | BLAKE2s | 哈希表查找、密钥派生 |
| **身份** | 32 字节公钥 (base64) | 节点唯一标识 |

> ⚠️ **没有算法协商**意味着不存在降级攻击。如果对端不支持 ChaCha20-Poly1305，连接直接失败 —— 没有中间状态。

### 1.4 工作模式

```text
WireGuard 是无状态的无连接协议

传统 VPN (如 OpenVPN):                  WireGuard:
  SYN →                                UDP 直接发包
  ← SYN-ACK                            无握手
  → ACK                                公钥不匹配直接丢弃
  → TLS 握手 (多轮往返)
  → 认证
  → 密钥协商
  → 开始传输
  
延迟：数百毫秒                          延迟：微秒级
```

### 1.5 配置示例（极简）

服务端：
```ini
# /etc/wireguard/wg0.conf
[Interface]
PrivateKey = <服务端私钥>
Address = 10.0.0.1/24
ListenPort = 51820

[Peer]
# 客户端 A
PublicKey = <客户端A公钥>
AllowedIPs = 10.0.0.2/32
```

客户端：
```ini
# /etc/wireguard/wg0.conf
[Interface]
PrivateKey = <客户端私钥>
Address = 10.0.0.2/24
DNS = 1.1.1.1

[Peer]
PublicKey = <服务端公钥>
Endpoint = <服务器IP>:51820
AllowedIPs = 0.0.0.0/0    # 路由全部流量走 VPN
PersistentKeepalive = 25  # NAT 穿透保活
```

### 1.6 WireGuard 的优势与局限

| ✅ 优势 | ❌ 局限 |
|---------|--------|
| 代码极少，审计容易 | 不适用于需要用户名/密码认证的场景 |
| 内核态运行，性能极高 | 不支持动态地址分配（DHCP 式） |
| 配置极其简单 | 没有内置的吊销机制 |
| 漫游支持（IP 切换无感） | 两层 NAT 下可能困难 |
| 已进入 Linux 内核主线 | 商业 VPN 支持仍在增长中 |
| 移动端功耗极低 | 没有混淆层（容易被 DPI 识别） |

---

## 2. OpenVPN —— 久经考验的通用方案

### 2.1 一句话定位

> OpenVPN 是 2001 年创建的开源 VPN 方案，使用 TLS/SSL 进行密钥交换，支持极其灵活的配置，是**兼容性最广、社区最成熟**的 VPN 方案。

### 2.2 架构

```
┌──────────────────────────────────────────┐
│              OpenVPN 架构                 │
│                                           │
│  ┌─────────┐          ┌─────────┐        │
│  │ 控制通道  │   TLS   │ 控制通道  │        │
│  │ (TCP)   │←────────→│ (TCP)    │        │
│  │  认证    │          │  认证     │        │
│  │  密钥交换│          │  密钥交换  │        │
│  └─────────┘          └─────────┘        │
│                                           │
│  ┌─────────┐          ┌─────────┐        │
│  │ 数据通道  │  加密    │ 数据通道  │        │
│  │ (UDP/TCP)│←────────→│ (UDP/TCP)│        │
│  │  传输数据 │          │  传输数据  │        │
│  └─────────┘          └─────────┘        │
│                                           │
│  控制通道认证 + 密钥交换 → 数据通道加密    │
└──────────────────────────────────────────┘
```

### 2.3 传输协议选择

| 选项 | 协议 | 优点 | 缺点 | 推荐 |
|------|------|------|------|:---:|
| **udp** | UDP | 性能好，无 TCP-over-TCP 问题 | 某些网络封 UDP | ✅ 首选 |
| **tcp** | TCP | 穿透防火墙容易 | TCP-over-TCP 重传冲突，性能差 | 备选 |
| **tcp + TLS** | TCP/443 | 伪装成 HTTPS 流量 | 双重加密开销大 | 特殊场景 |

> ⚠️ **TCP-over-TCP 问题**：当 VPN 数据通道是 TCP，内部传输的数据也是 TCP 时，两层 TCP 的重传机制会互相干扰，导致性能急剧下降。始终优先用 UDP。

### 2.4 认证方式

```text
OpenVPN 的三种认证模式：

1. 预共享密钥 (Static Key)
   → 最简单，双向用同一个密钥
   → 不支持前向安全性
   
2. TLS + 证书 (PKI)
   → 最安全，需要建 CA
   → 支持客户端证书吊销 (CRL)
   → 适合大规模部署
   
3. 用户名/密码 + 证书
   → 双重认证
   → 证书管设备，密码管人
   → 企业常用
```

### 2.5 典型服务器配置

```text
# server.conf (简化版)
port 1194
proto udp
dev tun

# 证书
ca ca.crt
cert server.crt
key server.key
dh dh.pem

# 网络
server 10.8.0.0 255.255.255.0
push "redirect-gateway def1"      # 推送默认路由
push "dhcp-option DNS 1.1.1.1"   # 推送 DNS

# 安全
cipher AES-256-GCM
auth SHA256
tls-version-min 1.2

# 性能
keepalive 10 120
max-clients 100

# 持久化
persist-key
persist-tun
```

### 2.6 优势与局限

| ✅ 优势 | ❌ 局限 |
|---------|--------|
| 极其稳定，久经考验 | 配置较复杂（CA、证书、dh参数等） |
| 支持 TCP/UDP 双模式 | 代码量大（约10万行），审计困难 |
| 灵活的认证方式 | 性能不如 WireGuard |
| 丰富的插件生态 | TCP 模式下的 TCP-over-TCP 问题 |
| 几乎所有平台都有客户端 | 移动端耗电较 WireGuard 高 |

---

## 3. IPsec/IKEv2 —— 企业级标准

### 3.1 一句话定位

> IPsec 是 IETF 制定的网络层安全标准，不是单一协议而是一个**协议族**。IKEv2 是密钥交换协议的最新版本，和 IPsec 配合使用。操作系统原生支持，无需安装客户端。

### 3.2 协议族组成

```
IPsec 协议族
│
├── IKE (Internet Key Exchange)  ← 密钥协商
│   ├── IKEv1 (旧，仍广泛使用)
│   └── IKEv2 (新，更快更安全)
│
├── AH (Authentication Header)   ← 认证，不加密
├── ESP (Encapsulating Security Payload) ← 认证 + 加密
│
├── 加密算法：AES-GCM, AES-CBC, ChaCha20-Poly1305
├── 认证算法：HMAC-SHA256, HMAC-SHA384
├── DH 组：Group 14 (2048), 15 (3072), 16 (4096), 19 (ECP256), 20 (ECP384)
│
└── 工作模式
    ├── Transport Mode (只加密载荷)
    └── Tunnel Mode (加密整个 IP 包)
```

### 3.3 IKEv2 的优势特性

| 特性 | 说明 |
|------|------|
| **MOBIKE** | 网络切换时无缝重连（WiFi ↔ 4G） |
| **快速重连** | 断开后可快速恢复，无需完整握手 |
| **Dead Peer Detection** | 快速检测对端是否在线 |
| **EAP 认证** | 支持用户名/密码、证书等多种方式 |
| **NAT 穿透** | 内置 NAT-T，UDP 封装 ESP |

### 3.4 使用 strongSwan 配置示例

```text
# /etc/ipsec.conf
config setup
    charondebug="ike 2, knl 2, cfg 2"

conn ikev2-vpn
    auto=add
    compress=no
    type=tunnel
    keyexchange=ikev2
    
    # 本端 (服务器)
    left=%any
    leftid=@vpn.example.com
    leftcert=server-cert.pem
    leftsendcert=always
    leftsubnet=0.0.0.0/0
    
    # 对端 (客户端)
    right=%any
    rightid=%any
    rightauth=eap-mschapv2
    rightsourceip=10.10.10.0/24
    rightdns=8.8.8.8,1.1.1.1
    
    # 加密
    ike=aes256-sha256-modp2048!
    esp=aes256-sha256!
```

### 3.5 优势与局限

| ✅ 优势 | ❌ 局限 |
|---------|--------|
| 操作系统原生支持 (iOS/macOS/Windows/Android) | 配置复杂度极高 |
| IKEv2 + MOBIKE 移动端体验极好 | 防火墙 NAT 穿越困难（需 NAT-T） |
| 企业级成熟度，大厂首选 | IKEv1 有已知安全缺陷 |
| 性能优秀（硬件加速 AES） | 证书管理复杂 |
| strongSwan/libreswan 开源实现成熟 | 调试困难 |

---

## 4. L2TP/IPsec —— 移动端兼容利器

### 4.1 一句话定位

> L2TP 是 Layer 2 Tunneling Protocol（二层隧道协议），本身**不提供加密**，必须和 IPsec 配合使用。几乎所有操作系统都内置支持，是最兼容的方案之一。

### 4.2 为什么 L2TP 需要 IPsec

```text
单独 L2TP：                              L2TP/IPsec：
┌──────────────┐                    ┌──────────────────────┐
│ [L2TP 头]    │                    │ [IPsec 加密层]        │
│ [PPP 头]     │  ← 明文，不安全      │  ┌──────────────┐   │
│ [原始数据]    │                    │  │ [L2TP 头]     │   │
└──────────────┘                    │  │ [PPP 头]      │   │
                                    │  │ [原始数据]     │   │
                                    │  └──────────────┘   │
                                    └──────────────────────┘
```

### 4.3 双封装开销

```
原始数据包：
└── TCP/UDP payload

封装层 1 - PPP：
└── [PPP 头] + TCP/UDP payload

封装层 2 - L2TP：
└── [L2TP 头] + [PPP 头] + TCP/UDP payload

封装层 3 - IPsec (ESP Tunnel)：
└── [外层 IP 头] + [ESP 头] + 加密([L2TP 头] + [PPP 头] + payload) + [ESP 尾]

总开销：~40-60 字节  ← 有效 MTU 需相应减小
```

### 4.4 优势与局限

| ✅ 优势 | ❌ 局限 |
|---------|--------|
| 所有主流 OS 内置支持 | 双重封装，开销大 |
| 配置相对简单 | 性能最差（协议栈最深） |
| 通过 UDP 4500 端口，NAT 穿透好 | 安全性依赖 IPsec 配置 |
| 支持 EAP 用户名/密码认证 | UDP 500/4500 在某些网络被封 |

---

## 5. PPTP —— 已死的协议

### 5.1 一句话定位

> PPTP (Point-to-Point Tunneling Protocol) 由 Microsoft 在 1999 年推广。加密算法 MPPE 和认证协议 MS-CHAPv2 已完全被攻破，**不要在任何场景使用**。

### 5.2 已知安全漏洞

| 漏洞 | 影响 | 披露时间 |
|------|------|---------|
| **MS-CHAPv2 破解** | 可离线暴力破解用户密码 | 2012 (CloudCracker) |
| **MPPE RC4 弱加密** | 可实时解密流量 | 持续存在 |
| **无数据完整性校验** | 可篡改传输中的数据 | 设计缺陷 |

> 🎯 **一句话总结**：PPTP 的安全性相当于"网线插在公共场所，上面贴了个「请勿偷看」的标签"。

---

## 6. 协议全景对比

| 维度 | WireGuard | OpenVPN | IPsec/IKEv2 | L2TP/IPsec | PPTP |
|------|-----------|---------|-------------|------------|------|
| **代码量** | ~4,000 行 | ~100,000 行 | ~400,000 行 (strongSwan) | depends | ~10,000 行 |
| **加密算法** | ChaCha20-Poly1305 | AES-256-GCM (可配置) | AES-256-GCM (可配置) | IPsec 控制 | MPPE-128 (弱) |
| **密钥交换** | Noise IK (Curve25519) | TLS/ECDH | IKEv2 | IKEv1/v2 | MS-CHAPv2 |
| **前向安全性** | ✅ | ✅ (ECDH) | ✅ | ⚠️ | ❌ |
| **传输协议** | UDP | UDP/TCP | UDP (ESP/AH) | UDP | TCP (GRE) |
| **内核支持** | Linux 5.6+ | 用户态 | 内核态 | 内核态 | 内核态 |
| **吞吐量** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐ |
| **移动端功耗** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐ |
| **漫游支持** | ✅ 原生 | ⚠️ 需重连 | ✅ MOBIKE | ❌ | ❌ |
| **NAT 穿透** | ✅ (需 keepalive) | ✅ | ⚠️ (需 NAT-T) | ✅ | ⚠️ (需特殊处理) |
| **配置复杂度** | ⭐ (极简) | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐ |
| **审计难度** | 低 | 高 | 极高 | 高 | 中 |
| **OS 支持** | All | All | All | All | All |
| **成熟度** | 2018+ | 2001+ | 1995+ | 1999+ | 1995 |
| **推荐度** | ✅✅✅ | ✅✅ | ✅✅ | ⚠️ | ❌ |

### 性能基准参考

```text
吞吐量测试 (单核 2.5GHz CPU, 256-bit 加密, 1Gbps 链接)：

WireGuard:   900+ Mbps  ──────────────────
IPsec:       800+ Mbps  ────────────────
OpenVPN:     400+ Mbps  ────────
L2TP/IPsec:  200+ Mbps  ────
PPTP:        150+ Mbps  ───
```

> 💡 WireGuard 的高性能主要来自：1) 内核态运行 2) ChaCha20 软实现很快 3) 无状态无连接设计 4) 代码极简无额外开销。

---

## 7. 协议选型决策树

```text
                      开始选型
                         │
                    ┌────▼────┐
                    │ 你的场景？ │
                    └────┬────┘
                         │
          ┌──────────────┼──────────────┐
          ▼              ▼              ▼
     个人/小团队       企业环境         最大兼容性
          │              │              │
    ┌─────▼─────┐   ┌────▼────┐   ┌────▼────┐
    │ 追求极简？ │   │ 合规要求？│   │ 老旧设备？│
    └─────┬─────┘   └────┬────┘   └────┬────┘
      Y   │   N      Y   │   N      Y   │   N
    ┌─▼──┐┌─▼───┐  ┌─▼──┐┌─▼───┐  ┌─▼──┐┌─▼───┐
    │WG  ││OpenVPN││IPsec││OpenVPN││L2TP││OpenVPN│
    │    ││(UDP) ││IKEv2││(证书)││/IPsec││(TCP)│
    └────┘└──────┘└─────┘└──────┘└─────┘└──────┘
```

### 推荐总结

| 场景 | 首选 | 理由 |
|------|:---:|------|
| **个人翻墙/隐私** | WireGuard | 飞快、省电、极简 |
| **自建 VPN 服务器** | WireGuard | 五分钟配好，性能拉满 |
| **公司远程办公** | IPsec/IKEv2 | 企业成熟度、客户端零安装 |
| **多平台兼容** | OpenVPN (UDP) | 哪都能跑 |
| **受限网络环境** | OpenVPN (TCP/443) | 伪装 HTTPS，最难被封 |
| **混合云互联** | IPsec | 云厂商原生支持 |
| **IoT/嵌入式** | WireGuard | 代码极小，功耗极低 |
| **移动端首选** | WireGuard / IKEv2 | MOBIKE (IKEv2) vs 低功耗 (WireGuard) |

---

**下一模块**：[03-自建VPN实战](./03-自建VPN实战.md) | **返回总览**：[00-VPN知识体系总览](./00-VPN知识体系总览.md)
