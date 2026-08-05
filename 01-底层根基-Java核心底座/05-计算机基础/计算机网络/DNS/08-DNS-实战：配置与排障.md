# DNS 实战：配置与排障

> 实战三件套：dig 是"X 光机"（查真相）、Zone 文件是"配置面"（管数据）、排障决策树是"地图"（从症状到根因）。加上 K8s 里绕不开的 CoreDNS——本章是 DNS 工程能力的最后一块拼图

---

## 📚 目录

1. [dig 实战：DNS 的瑞士军刀](#1-dig-实战dns-的瑞士军刀)
2. [dig 输出全解读](#2-dig-输出全解读)
3. [Zone 文件与 BIND 配置实战](#3-zone-文件与-bind-配置实战)
4. [K8s 与 CoreDNS](#4-k8s-与-coredns)
5. [内网 DNS 设计](#5-内网-dns-设计)
6. [常见故障与排障决策树](#6-常见故障与排障决策树)
7. [核心要点与思考题](#7-核心要点与思考题)

---

## 1. dig 实战：DNS 的瑞士军刀

```bash
# 基础三连
dig example.com                    # 查 A（默认递归器）
dig example.com AAAA               # 查 IPv6
dig example.com MX                 # 查邮件

# 指定查询源（排障关键）
dig @8.8.8.8 example.com A         # 公共递归器视角
dig @ns1.example.com example.com A # 权威服务器视角（真相）
dig @1.1.1.1 example.com A +short  # 只输出答案

# 进阶参数
dig example.com ANY +noall +answer # 只看答案段
dig example.com A +trace           # 模拟完整迭代（根→TLD→权威）
dig example.com A +ttlid           # 显示 TTL 剩余
dig -x 93.184.215.14               # 反向解析（PTR）
dig +dnssec example.com A          # 查看 DNSSEC 记录
dig example.com A +tcp             # 强制 TCP 查询
```

```bash
# 排障常用组合
dig @ns1.example.com www.example.com A +noall +answer +comments
# 看：flags 里的 aa（权威）、ttl（剩余）、answer（真实记录）
```

## 2. dig 输出全解读

```text
; <<>> DiG 9.18 <<>> example.com A
;; global options: +cmd
;; Got answer:
;; ->>HEADER<<- opcode: QUERY, status: NOERROR, id: 5678
;; flags: qr rd ra; QUERY: 1, ANSWER: 1, AUTHORITY: 0, ADDITIONAL: 1
                              ↑
              权威应答时这里会出现 aa（02/04 章）

;; OPT PSEUDOSECTION:
; EDNS: version: 0, flags:; udp: 1232        ← EDNS 协商（04 章）

;; QUESTION SECTION:
;example.com.           IN      A              ← 问了什么

;; ANSWER SECTION:
example.com.    300     IN      A       93.184.215.14   ← 答案+TTL 剩余

;; Query time: 12 msec                        ← 查询耗时
;; SERVER: 127.0.0.53#53(127.0.0.53)          ← 谁答的（递归器）
;; WHEN: Wed Aug 05 10:00:00 CST 2026
```

| 字段 | 排障含义 |
|------|---------|
| status | NOERROR/NXDOMAIN/SERVFAIL/REFUSED（04 章） |
| flags aa | 是否权威应答 |
| ttl | 缓存剩余（= 变更还需等多久，05 章） |
| SERVER | 应答者（验证"到底谁在答"） |
| Query time | 该跳的 RTT（慢在哪一跳） |

## 3. Zone 文件与 BIND 配置实战

```text
# /etc/bind/named.conf.local（BIND 声明区域）
zone "example.com" {
    type master;                        # 主权威
    file "/etc/bind/zones/example.com.zone";
};
```

```text
# example.com.zone（完整实例，03 章详解过结构）
$TTL 300
@   IN SOA ns1.example.com. admin.example.com. (
        2026080501 3600 600 1209600 300 )

    IN NS ns1.example.com.
    IN NS ns2.example.com.
    IN A  93.184.215.14
    IN MX 10 mail.example.com.

www     IN A  93.184.215.14
api     IN CNAME www.example.com.
mail    IN A  93.184.215.15
```

```bash
# 部署与验证
named-checkconf                      # 语法检查
named-checkzone example.com /etc/bind/zones/example.com.zone
systemctl reload named               # 或 rndc reload
dig @127.0.0.1 example.com A +noall +answer   # 本地验证
```

> ⚠️ **最高频配置错误**：相对名 vs 绝对名——`www IN A 1.2.3.4` 是 `www.example.com`，但 `api IN CNAME www.example.com.`（目标必须 FQDN 带点）；SOA 里两个名字也必须是 FQDN。

## 4. K8s 与 CoreDNS

```text
K8s 内 DNS 架构：
  Pod 的 resolv.conf：nameserver 指向 CoreDNS Service IP（kube-dns）
  CoreDNS = 集群内递归器（+权威于集群域）

CoreDNS 解析三类域名：
  ① 集群域：<service>.<namespace>.svc.cluster.local → Service IP（权威）
  ② 外部域名：转发到上游（/etc/resolv.conf 或显式 upstream）→ 递归
  ③ 特殊：cluster.local（集群）、in-addr.arpa（反向）

常见问题：
  Pod 解析慢 → CoreDNS 副本数不足（建议 ≥ 2）或上游慢
  Service 解析失败 → Service 未创建/namespace 不对（FQDN 查询最稳）
  外部解析异常 → 上游 DNS 配置（Corefile 的 forward）
  大集群压力 → CoreDNS 每 Pod 缓存（cache 插件）与并发调优
```

```text
# Corefile 片段（CoreDNS 配置）
.:53 {
    errors
    health
    kubernetes cluster.local in-addr.arpa ip6.arpa {
        pods insecure
        fallthrough in-addr.arpa ip6.arpa
    }
    forward . 10.0.0.1 8.8.8.8        # 外部转发上游
    cache 30                            # 缓存 30s
    loop                               # 环路检测
    reload
}
```

## 5. 内网 DNS 设计

| 设计点 | 建议 |
|--------|------|
| 递归器 | 内网自建（如 dnsmasq/Unbound）+ 上游公共解析器 |
| 权威 | 内网域（如 corp.example.com）独立权威，与公网隔离 |
| 缓存 | 内网递归器缓存 30-300s（平衡一致性与性能） |
| 高可用 | 双递归器（客户端 resolv.conf 配 2 个 nameserver） |
| 加密 | 内网解析器间 DoT；客户端有条件上 DoH |
| 监控 | 查询量、SERVFAIL 率、解析时延、缓存命中率 |

## 6. 常见故障与排障决策树

```text
症状：域名解析失败/慢/不一致
│
├─ dig @公共递归（8.8.8.8）？
│   ├─ NXDOMAIN → 域名真的不存在/记录未添加（查权威托管）
│   ├─ SERVFAIL → 权威不可达/NS 配置错/DNSSEC 错误
│   ├─ 无响应 → 网络问题（UDP 53 被拦）
│   └─ 正常 → 问题在本地（见下）
│
├─ dig @权威（ns1.域名）？
│   ├─ 记录与预期不符 → 权威配置问题（Zone 文件/同步）
│   └─ 一致 → 问题在缓存链（见下）
│
└─ 本地/缓存视角
    ├─ 改记录不生效 → TTL 未过期（05 章）
    ├─ 部分用户旧 IP → 缓存渐进（等待/清缓存）
    ├─ 被劫持（结果与权威不同） → 中间设备污染（06 章，换 DoH 验证）
    └─ 内网异常 → CoreDNS/内网递归器（第 4 节）
```

| 症状 | 高频根因 |
|------|---------|
| 解析超时 | UDP 53 被防火墙拦 / 上游递归慢 / EDNS 被中间设备丢 |
| SERVFAIL | NS 委派不一致 / 权威挂 / DNSSEC 链断裂 |
| 改记录不生效 | TTL 未过 / 记录改错 zone / 缓存未清 |
| 结果不一致 | GSLB 正常差异 / 缓存不一致 / 被污染 |
| 内网 Pod 解析慢 | CoreDNS 副本少 / 上游慢 / conntrack 满 |
| 邮件被拒 | PTR/SPF/DKIM 缺失（03 章） |

## 7. 核心要点与思考题

> 🎯 **本章三句话总结**：
> 1. dig 排障三视角：@公共递归（用户所见）、@权威（真相）、+trace（链路）——aa 标志与 TTL 剩余是判断关键；
> 2. Zone 文件易错：相对名/绝对名、serial 递增、NS 必须 FQDN；改完 named-checkzone 验证；
> 3. K8s 解析靠 CoreDNS（集群域权威 + 外部转发 + 缓存），排障先分"集群域/外部域"再动手。

**思考题**：

1. 怎么区分"权威真相"与"缓存所见"？（→ 1/2 指定查询源）
2. `api IN CNAME www.example.com.` 里哪个是易错点？（→ 3 绝对名）
3. Pod 解析外部域名失败，先查哪？（→ 4 forward 配置）
4. 改记录后"部分用户还是旧 IP"是故障吗？（→ 6 缓存渐进）

---

**下一模块**：[09-面试高频考点与总结](09-面试高频考点与总结.md)｜**返回总览**：[00-DNS知识体系总览](00-DNS知识体系总览.md)
