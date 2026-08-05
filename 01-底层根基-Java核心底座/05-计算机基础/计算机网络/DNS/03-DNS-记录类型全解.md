# DNS 记录类型全解

> 记录（RR，Resource Record）是 DNS 的数据单元：A/AAAA 定地址、CNAME 定别名、MX 定邮件、NS 定授权、TXT/SRV/CAA 承载元数据——每种记录有明确的语义与限制（CNAME 不能与 A 共存是最高频的坑）。会用 Zone 文件，才算真正理解记录

---

## 📚 目录

1. [记录的通用结构：五元组](#1-记录的通用结构五元组)
2. [核心记录：A / AAAA / CNAME / NS](#2-核心记录a--aaaa--cname--ns)
3. [服务记录：MX / SRV / PTR](#3-服务记录mx--srv--ptr)
4. [元数据记录：TXT / CAA / DNAME / DS](#4-元数据记录txt--caa--dname--ds)
5. [CNAME 的规则与陷阱](#5-cname-的规则与陷阱)
6. [Zone 文件实战](#6-zone-文件实战)
7. [TTL 的语义细节](#7-ttl-的语义细节)
8. [核心要点与思考题](#8-核心要点与思考题)

---

## 1. 记录的通用结构：五元组

```text
每个 DNS 记录（RR）由五元组构成：
  NAME（名称） TYPE（类型） CLASS（类，几乎都是 IN） TTL（存活秒数） RDATA（数据）

  www.example.com.  IN   A    300   93.184.215.14
  └──名称───┘      └类型┘└TTL┘  └──数据───┘
```

## 2. 核心记录：A / AAAA / CNAME / NS

| 记录 | 全称 | 数据 | 用途 |
|------|------|------|------|
| **A** | Address | IPv4 地址 | 域名 → IPv4（最基础） |
| **AAAA** | IPv6 Address | IPv6 地址 | 域名 → IPv6（IPv6 双栈必需） |
| **CNAME** | Canonical Name | 另一域名 | 别名（www → 主域名） |
| **NS** | Name Server | 权威服务器域名 | **声明该域归谁管**（授权） |

```text
A 记录的关键语义：一个域名可有多条 A（DNS 轮询）
  example.com. IN A 10.0.0.1
  example.com. IN A 10.0.0.2     ← 负载均衡最底层形态（07 章）

NS 记录的关键语义：
  写在父域（如 com 里的 example.com NS）= 委派授权
  写在子域自身（example.com 里的 NS）= 声明（需与父域一致）
  → NS 不一致 = 域名解析失败的经典原因
```

## 3. 服务记录：MX / SRV / PTR

### 3.1 MX（邮件路由）

```text
example.com. IN MX 10 mail.example.com.
                     └优先级┘  └目标┘
规则：
  优先级数字越小越优先（10 < 20）
  目标必须是域名（不能是 IP）
  收件方查询 MX → 取最高优先级（失败再降级）
生产要点：MX 目标要有 A/AAAA 记录；SPF 记录里要包含 MX 的 IP
```

### 3.2 SRV（服务定位）

```text
_service._proto.domain. IN SRV 优先级 权重 端口 目标
  _sip._tcp.example.com. IN SRV 10 60 5060 sip1.example.com.
                                └权重┘  └端口┘
用途：服务发现（SIP、LDAP、K8s 之外的 XMPP 等）
对比：A 记录找"主机"，SRV 找"服务+端口"
```

### 3.3 PTR（反向解析）

```text
4.3.2.1.in-addr.arpa. IN PTR host.example.com.
  （IPv4 反查域：in-addr.arpa；IPv6：ip6.arpa）

用途：
  邮件反垃圾：接收方查发件 IP 的 PTR，无 PTR/不匹配 → 判垃圾
  日志审计：把 IP 还原成主机名
```

## 4. 元数据记录：TXT / CAA / DNAME / DS

| 记录 | 数据 | 典型用途 |
|------|------|---------|
| **TXT** | 自由文本 | SPF/DKIM/DMARC 邮件验证、域名归属验证（CA 签发证书时） |
| **CAA** | 允许的 CA 列表 | 声明"只允许这些 CA 给我签发证书" |
| **DNAME** | 子树别名（整个域树重定向） | 域迁移（比 CNAME 强：覆盖所有子域） |
| **DS** | 子域 DNSSEC 摘要 | DNSSEC 委派链（06 章） |

```text
TXT 实战（邮件安全三件套）：
  SPF:   v=spf1 include:_spf.example.com ~all        （谁允许代发）
  DKIM:  v=DKIM1; k=rsa; p=MIGfMA...                  （签名公钥）
  DMARC: v=DMARC1; p=quarantine; rua=mailto:dmarc@example.com
  → 无 SPF/DKIM 的域名邮件被拒收率极高（2026 邮件生态标配）

CAA 实战：
  example.com. IN CAA 0 issue "digicert.com"       （只允许 DigiCert）
  example.com. IN CAA 0 issuewild "letsencrypt.org"（通配符证书）
  → 防止别人（攻击者/误操作）用其他 CA 给你的域名签发证书
```

## 5. CNAME 的规则与陷阱

```text
规则：
  ① CNAME 是"别名"：查询 CNAME → 跟随目标再查一次（A/AAAA）
  ② CNAME 记录不能再挂其他记录：
     www.example.com CNAME example.com
     www.example.com A 1.2.3.4     ❌ 非法！（同一名字 CNAME 与 A 互斥）
  ③ 域名根（裸域 apex，如 example.com）不能是 CNAME（RFC 无此限制，但
     多数权威实现禁止/解析器不跟随——所以裸域用 A 或 ALIAS/ANAME）

陷阱：
  - 把裸域设成 CNAME → 部分解析器解析失败（邮件/证书验证断裂）
  - CNAME 链过长（A→B→C）→ 解析慢、TTL 取最小
  - CDN 接入常见形态：裸域 A → CDN IP；www 域 CNAME → CDN 域名
```

> 🎯 **核心要点**：CNAME 的本质是"查询重定向"——它让"别名"与"真实主机"解耦（改真实目标不动别名），但代价是不能在同一名字上共存其他记录。

## 6. Zone 文件实战

```text
# example.com.zone（BIND 格式）
$TTL 300
@   IN SOA ns1.example.com. admin.example.com. (
        2026080501  ; serial（每次修改递增）
        3600        ; refresh（从服务器刷新间隔）
        600         ; retry
        1209600     ; expire
        300 )       ; negative TTL

    IN NS ns1.example.com.
    IN NS ns2.example.com.
    IN A  93.184.215.14          ; 裸域 A
    IN AAAA 2606:2800:220:1::1   ; 裸域 IPv6
    IN MX 10 mail.example.com.

www IN A 93.184.215.14           ; www.example.com（相对名，无结尾点）
api IN CNAME www.example.com.    ; CNAME 目标必须写 FQDN（带点）
mail IN A 93.184.215.15
```

```text
Zone 文件易错点：
  ① 相对名 vs 绝对名：不带点 = 相对当前域；带点 = 绝对（08 章）
  ② SOA 的 serial 每次修改必须递增（否则从服务器不同步）
  ③ NS 记录的名字必须是 FQDN（不能是 IP）
  ④ 修改后 rndc reload / named-checkzone 验证
```

## 7. TTL 的语义细节

```text
TTL 是"记录在缓存里的存活时间"（05 章详述策略）：
  Zone 级：$TTL 300（默认给所有记录）
  记录级：可单独覆盖（高优先级）
  负缓存 TTL：SOA 的 negative TTL 控制"查无此名"的缓存时长

示例语义：
  A 记录 TTL=60 → 递归器最多缓存 60s → 改 IP 后 1 分钟内在全球生效
  A 记录 TTL=86400 → 缓存 1 天 → 改 IP 后要等 1 天（发版事故源头！）
```

## 8. 记录配置的最佳实践清单

### 8.1 一套"健康域名"的完整记录

```text
以 example.com 为例（生产模板）：
  @    IN NS   ns1.dnsprovider.com.      ; 权威（托管商）
  @    IN NS   ns2.dnsprovider.com.
  @    IN A    93.184.215.14             ; 裸域
  @    IN AAAA 2606:2800:220:1::1        ; IPv6（双栈必配）
  @    IN MX   10 mail.example.com.      ; 邮件
  @    IN TXT  "v=spf1 include:_spf... ~all"   ; 防伪造
  @    IN TXT  "v=DMARC1; p=quarantine; ..."   ; 邮件策略
  @    IN CAA  0 issue "letsencrypt.org"        ; 证书授权
  www  IN A    93.184.215.14
  mail IN A    93.184.215.15
  _dmarc IN TXT "v=DMARC1; ..."                ; DMARC 专用名
```

### 8.2 检查清单（新域名上线/迁移时逐项核对）

```text
□ A/AAAA 齐全（IPv6 缺失 = 部分用户无法访问）
□ MX + SPF + DKIM + DMARC（邮件四件套，缺一拒收风险）
□ CAA（防他人签发证书——安全加分项）
□ NS 与父域委派一致（不一致 = 解析失败）
□ 子域 CNAME 无环（A→B→A 死循环）
□ TTL 合理（05 章速查表）
□ SOA serial 正确（改动后递增）
□ 反向 PTR（邮件服务器/日志审计必需）
```

### 8.3 变更安全习惯

```text
□ 改动前导出全量记录（回滚快照）
□ 改 CNAME 前验证目标存在（悬空 CNAME = 子域接管风险，01 章）
□ 删除记录前确认无引用（邮件/证书验证依赖）
□ 用 dig 三视角验证（08 章）而非"看着对"
```

## 9. 核心要点与思考题

> 🎯 **本章三句话总结**：
> 1. 记录五元组（名称/类型/类/TTL/数据）；A 定 IPv4、AAAA 定 IPv6、CNAME 定别名、MX 定邮件、NS 定授权；
> 2. CNAME 不能与 A 共存、裸域不能 CNAME、MX 目标必须是域名——三条高频坑；
> 3. TXT/CAA/DS 让 DNS 成为"安全与元数据载体"（SPF/DKIM/DMARC/证书签发验证都靠它）。

**思考题**：

1. 为什么裸域不能设 CNAME？（→ 5 规则③）
2. 邮件被拒收，先查哪三条记录？（→ 4 TXT 三件套）
3. Zone 文件里 serial 不递增会怎样？（→ 6 易错点②）
4. 一个域名两条 A 记录是负载均衡吗？（→ 2 + 07 章 DNS 轮询）

---

**下一模块**：[04-DNS 报文与协议细节](04-DNS 报文与协议细节.md)｜**返回总览**：[00-DNS知识体系总览](00-DNS知识体系总览.md)
