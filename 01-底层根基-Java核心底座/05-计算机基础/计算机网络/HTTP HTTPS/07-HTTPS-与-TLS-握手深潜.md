# HTTPS 与 TLS 握手深潜

> HTTPS = HTTP + TLS：身份认证（证书链）、机密性（对称加密）、完整性（MAC）与防重放（序列号）四件事都由 TLS 完成。TLS 1.3 把握手从 2-RTT 压到 1-RTT（回访 0-RTT）；2026 年浏览器已默认启用后量子混合密钥交换（X25519MLKEM768）——加密正在量子化

---

## 📚 目录

1. [HTTPS 解决的四件事](#1-https-解决的四件事)
2. [TLS 1.3 握手：1-RTT 的秘密](#2-tls-13-握手1-rtt-的秘密)
3. [0-RTT：快但有重放风险](#3-0-rtt快但有重放风险)
4. [证书链与 PKI](#4-证书链与-pki)
5. [TLS 1.3 vs 1.2：变化清单](#5-tls-13-vs-12变化清单)
6. [会话恢复：减少重复握手](#6-会话恢复减少重复握手)
7. [2026：后量子混合密钥交换](#7-2026后量子混合密钥交换)
8. [HTTPS 配置最佳实践](#8-https-配置最佳实践)
9. [核心要点与思考题](#9-核心要点与思考题)

---

## 1. HTTPS 解决的四件事

```text
明文 HTTP 的三个问题：被偷看（机密性）、被篡改（完整性）、被冒充（身份）
TLS 的四个目标：
  ① 身份认证：客户端验证服务器证书（证书链 + 数字签名）
  ② 机密性：会话数据用对称加密（AES-GCM / ChaCha20）
  ③ 完整性：MAC（GMAC/Poly1305）防篡改
  ④ 防重放：序列号 + 窗口机制

密钥关系（TLS 体系）：
  非对称（RSA/ECDSA/ML-KEM）→ 只在握手期做密钥交换/签名（慢）
  对称（AES/ChaCha20）→ 会话数据加密（快）
  哈希（SHA-256/384）→ 派生密钥（HKDF）
```

> 🎯 **核心要点**：HTTPS 的性能焦虑（"加密很慢"）在现代硬件上早已过时——AES-NI 指令让对称加密近乎免费；**真正昂贵的是握手**（往返延迟），所以 TLS 1.3 与 0-RTT 的一切努力都在压缩握手。

## 2. TLS 1.3 握手：1-RTT 的秘密

### 2.1 完整握手流程（1-RTT）

```text
客户端                         服务器
ClientHello ─────────────────→   （携带 key_share：预生成的公钥 + 支持的套件）
              ←──────────────── ServerHello（选定套件 + 服务端公钥）
              ←──────────────── {EncryptedExtensions, Certificate, Finished}（加密的）
{Finished} ──────────────────→
← 双向发送应用数据 ──→（此后全部加密）
```

| 步骤 | 关键点 |
|------|--------|
| ClientHello | **携带密钥共享**（key_share 提前生成）——比 1.2 少一次往返的根源 |
| ServerHello | 选定套件（1.3 固定套件格式） |
| Certificate | 服务器证书链（加密发送） |
| Finished | 双方确认（握手中的消息都经 HKDF 派生密钥保护） |

```text
为什么比 TLS 1.2 快：
  1.2：密钥交换与证书认证串行 → 2 个往返（2-RTT）
  1.3：密钥交换预置（key_share）+ 证书加密后置 → 1 个往返（1-RTT）
```

### 2.2 首次连接的延迟账本

```text
HTTPS 首次访问延迟（RTT 数）：
  TCP 三次握手：1 RTT
  TLS 1.2：+2 RTT = 3 RTT
  TLS 1.3：+1 RTT = 2 RTT（HTTP/2 的 ALPN 已合并）

移动网络 100ms RTT → 差 100ms；跨洋 200ms → 差 200ms
```

## 3. 0-RTT：快但有重放风险

```text
0-RTT：上次握手成功后，客户端缓存 PSK（预共享密钥）
  回访请求：客户端第一个应用数据包就带 PSK + 加密的请求
  → 服务器收到即解密响应 → 零往返开始传输

安全代价：重放攻击
  攻击者截获 0-RTT 请求 → 反复重放（服务器无法区分新旧）
  限制：0-RTT 只能用于幂等请求（GET/查询）
  → RFC 8446 明确建议：0-RTT 数据只能用于无副作用操作

生产实践：
  开启 0-RTT 的站点（Cloudflare 默认）对 GET 资源安全
  登录/下单等写操作绝不走 0-RTT（服务端强制全握手）
```

## 4. 证书链与 PKI

### 4.1 信任链

```text
根证书（CA Root，预装在浏览器/系统信任库）
  └─ 中间证书（Intermediate CA）
       └─ 服务器证书（Leaf，example.com）

验证过程（客户端）：
  ① 用上一级公钥验证下一级签名（逐级）
  ② 最上级必须在信任库（Trust Store）
  ③ 校验域名（CN/SAN 匹配）、有效期、吊销状态（OCSP）
  任一环节失败 → 浏览器告警（证书错误）
```

### 4.2 证书内容

| 字段 | 说明 |
|------|------|
| 域名（SAN） | 覆盖的主机名（`*.example.com` 通配） |
| 公钥 | 服务器公钥（RSA/ECDSA） |
| 有效期 | 签发/到期（2026 后行业标准 90 天短寿命） |
| 签发者 | 上级 CA |
| 扩展 | 用途（服务器认证/客户端认证）等 |

### 4.3 证书生命周期管理（生产）

```text
□ 到期监控与自动续期（Let's Encrypt 90 天自动轮换；ACME 协议）
□ 私钥安全：权限最小化、不落仓库、定期轮换
□ 证书链完整：Leaf + 中间证书（缺失中间链 = 客户端验证失败）
□ 吊销处理：OCSP Stapling（服务器代查，减客户端往返）
```

## 5. TLS 1.3 vs 1.2：变化清单

| 维度 | TLS 1.2 | TLS 1.3 |
|------|:---:|:---:|
| 握手 | 2-RTT | **1-RTT**（0-RTT 可选） |
| 密码套件 | 40+ 组合（版本/密钥交换/加密/MAC/哈希自由拼） | **5 个固定套件**（TLS_AES_128_GCM_SHA256 等） |
| RSA 密钥交换 | 支持（无前向保密） | **移除**（强制 ECDHE/前向保密） |
| 对称算法 | CBC + MAC 组合（BEAST/Lucky13 漏洞史） | 仅 AEAD（AES-GCM/ChaCha20-Poly1305） |
| 会话恢复 | Session ID / Session Ticket | **PSK + Ticket** |
| 兼容 | 广泛 | 需兼容层（middlebox 兼容模式） |

```text
1.3 的设计哲学：默认安全（移除不安全选项）+ 简单（固定套件）
  → 配置错误面大幅缩小（1.2 时代"用了 RC4/3DES"的悲剧不再可能）
```

## 6. 会话恢复：减少重复握手

| 机制 | 原理 | 现状 |
|------|------|:---:|
| Session Ticket（1.3 主流） | 服务器签发加密票据（PSK），客户端缓存；回访直接 PSK 握手（1-RTT 或 0-RTT） | ✅ 标准 |
| Session ID（1.2 遗留） | 服务器缓存会话状态（有状态，集群需共享） | 兼容 |
| 客户端缓存 | 浏览器按域名缓存会话 | 默认 |

```text
工程意义：
  Ticket 是"无状态会话恢复"——服务器不存会话，票据自包含
  集群部署零负担（任何节点都能验票，除非换了票据密钥）
  → Nginx ssl_session_tickets on + ticket 密钥轮换
```

## 7. 2026：后量子混合密钥交换

### 7.1 背景与机制

```text
量子计算威胁：Shor 算法可破解 RSA/ECDH（一旦量子计算机够大，"先截获后解密"）
NIST 标准（2024）：ML-KEM（密钥封装，前身 Kyber）/ ML-DSA / SLH-DSA

混合方案（2026 浏览器默认）：
  X25519MLKEM768（RFC 9794，IANA 0x11EC）
  = 经典 X25519 + 量子 ML-KEM-768，两密钥 HKDF 合并
  → 只要一方安全，会话就安全（后量子安全 + 经典安全双保险）
```

### 7.2 采用状态（2026-08 检索）

| 维度 | 状态 |
|------|------|
| Chrome | Chrome 131+ 默认；**Chrome 134（2026-02）全球默认 ML-KEM-768** |
| Firefox | 132+（桌面）/145+（Android）默认；QUIC 需 135+ |
| Safari/Apple | Safari 26+、iOS 26、macOS Tahoe 26 默认 |
| 服务端 | Go 1.24+、OpenSSL 3.5+、**Java 27+**、Node 24.5+、Caddy 2.10+、Nginx（OpenSSL 3.5 编译） |
| 全球握手占比 | Cloudflare 遥测：**30%+** 的 TLS 1.3 握手已用 X25519MLKEM768 |
| 证书侧 | **尚未量子化**：证书仍 ECDSA/RSA 签名；无 ML-DSA 根证书；CA/B Forum 基线要求未合并 |

### 7.3 运维影响：ClientHello 变大

```text
ClientHello：~300-500B → 1400B+（含 1216B 的 key_share）
→ 跨 2 个 TCP 段 → 部分中间设备（只看首包的 SNI 路由/L4 LB/DPI）
  无法识别 → 连接失败（客户端回退 HelloRetryRequest 多一个 RTT）

排查：升级 LB/防火墙的 SNI 解析能力；抓包确认 hello 分段
```

> 🎯 **核心要点**：2026 年"后量子 TLS"已经默认生效于主流浏览器——**钥匙交换已量子化，证书签名尚未**。服务端应尽快支持 X25519MLKEM768（OpenSSL 3.5 编译），并检查中间设备对 1.4KB ClientHello 的兼容。

## 8. HTTPS 配置最佳实践

```nginx
# Nginx HTTPS 生产级配置（核心片段）
server {
    listen 443 ssl;
    http2 on;                                  # HTTP/2（ALPN）

    ssl_protocols TLSv1.2 TLSv1.3;             # 禁 TLS 1.0/1.1
    ssl_ciphers TLS_AES_128_GCM_SHA256:...;    # 1.3 固定套件优先
    ssl_certificate fullchain.pem;             # 证书链（含中间证书）
    ssl_certificate_key privkey.pem;
    ssl_session_cache shared:SSL:10m;          # 会话恢复缓存
    ssl_session_tickets on;
    ssl_stapling on;                           # OCSP Stapling
    ssl_stapling_verify on;
}
```

| 实践 | 说明 |
|------|------|
| 协议范围 | 只留 TLS 1.2 + 1.3（1.0/1.1 已全部禁用） |
| 证书 | 90 天短寿命 + ACME 自动续期；私钥最小权限 |
| 会话恢复 | Session Ticket + OCSP Stapling 开启 |
| 后量子 | OpenSSL 3.5+ 编译（自动获得 X25519MLKEM768） |
| 测试 | SSL Labs 评级 A+；`openssl s_client -tls1_3` 验证 |

## 9. 核心要点与思考题

> 🎯 **本章三句话总结**：
> 1. TLS 1.3 = 1-RTT 握手（key_share 预置 + 固定套件默认安全），0-RTT 只给幂等请求；
> 2. 信任靠证书链（根→中间→叶子），运维要点是短寿命自动续期 + OCSP Stapling + 会话 Ticket；
> 3. 2026 大事件：X25519MLKEM768 已成浏览器默认（Chrome 134 全球默认，握手占比 30%+），但证书仍经典签名——服务端跟上 OpenSSL 3.5，检查中间设备对 1.4KB ClientHello 的兼容。

**思考题**：

1. TLS 1.3 为什么能 1-RTT？（→ 2.1 key_share）
2. 0-RTT 为什么不能用于写操作？（→ 3 重放）
3. 证书验证失败会在哪一步？（→ 4.1 信任链）
4. 后量子化目前只完成了哪一半？（→ 7.2 密钥交换已量子、证书未）

---

**下一模块**：[08-HTTP 安全实践](08-HTTP 安全实践.md)｜**返回总览**：[00-HTTP与HTTPS知识体系总览](00-HTTP与HTTPS知识体系总览.md)
