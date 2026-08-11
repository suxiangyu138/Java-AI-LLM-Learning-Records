# 04-DNS 与域名问题排查
> "域名解析失败"是最高频的网络问题之一：解析流程、nslookup/dig 定位、缓存与 TTL、Java 侧 DNS 问题——从"Unknown host"到根因

## 📚 目录
1. [DNS 解析流程](#1-dns-解析流程)
2. [解析失败的定位](#2-解析失败的定位)
3. [缓存与 TTL 问题](#3-缓存与-ttl-问题)
4. [Java 应用 DNS 问题](#4-java-应用-dns-问题)
5. [容器 DNS 问题](#5-容器-dns-问题)
6. [高频场景排查表](#6-高频场景排查表)
7. [核心要点](#7-核心要点)
8. [参考来源](#8-参考来源)

## 1. DNS 解析流程

```text
应用请求域名
  → ① 本地 hosts 文件（/etc/hosts）
  → ② 本地缓存（nscd / systemd-resolved / JVM 缓存）
  → ③ 配置的 DNS 服务器（resolv.conf）
        → 递归查询（根 → TLD → 权威）
  → 返回 IP（带 TTL）
```

```bash
cat /etc/resolv.conf       # 当前 DNS 服务器配置
cat /etc/hosts             # 本地映射（优先于 DNS）
```

| 环节 | 常见问题 |
|------|---------|
| hosts 文件 | 错误映射/过时条目 |
| 本地缓存 | 缓存过期 IP（变更后仍访问旧地址） |
| DNS 服务器 | 服务器不可达/配置错误 |
| 递归解析 | 上游解析失败/超时 |
| TTL | 变更后缓存期内仍解析旧值 |

## 2. 解析失败的定位

```bash
# ① 基础查询
nslookup api.example.com
# Server: ...  ← 用的哪个 DNS
# Address: ... ← 解析结果

# ② 指定 DNS 对比（判断是否本地 DNS 问题）
nslookup api.example.com 8.8.8.8

# ③ 详细解析
dig api.example.com
# status: NOERROR  ← NOERROR 成功 / NXDOMAIN 域名不存在
# ANSWER SECTION  ← 结果 IP

# ④ 检查 hosts 干扰
cat /etc/hosts | grep api.example
```

| dig status | 含义 | 下一步 |
|------------|------|--------|
| NOERROR | 解析成功 | 看 ANSWER 是否为空（空 = 记录不存在） |
| NXDOMAIN | **域名不存在** | 域名拼写/记录未创建 |
| SERVFAIL | 上游解析失败 | DNS 服务器问题 |
| TIMEOUT | DNS 服务器无响应 | 网络/DNS 服务器故障 |

> 🎯 **核心对比法**：`nslookup 域名`（本地 DNS）vs `nslookup 域名 @8.8.8.8`（公网 DNS）——两者结果不同 = 本地 DNS 配置问题；都失败 = 域名记录本身问题。

## 3. 缓存与 TTL 问题

### 3.1 经典场景：域名指向变更后仍访问旧 IP

```text
CDN/负载均衡切换 IP → 期望立即生效
实际：各级缓存（本地/递归/浏览器/Java）仍返回旧 IP（TTL 未过期）
```

| 缓存层 | 清除方式 |
|--------|---------|
| 浏览器 | 清 DNS 缓存/无痕模式 |
| 本地 nscd | `systemctl restart nscd` 或 `nscd -i hosts` |
| systemd-resolved | `resolvectl flush-caches` |
| 应用（Java） | JVM 重启 / 调 `networkaddress.cache.ttl` |
| 递归 DNS | 等待 TTL 过期（无法强制） |

```bash
# 检查 TTL
dig api.example.com | grep -A1 'ANSWER SECTION'
# 输出中含 TTL 值（如 300 = 5 分钟）

# 测试新 IP 是否已生效（绕过 DNS 直接连）
curl -v --resolve api.example.com:443:新IP https://api.example.com
```

> 💡 **变更流程规范**：IP 变更前**先降低 TTL**（如 600 → 60），等缓存刷新后再切换——生产变更的标准姿势。

## 4. Java 应用 DNS 问题

### 4.1 JVM 默认 DNS 缓存

```text
JVM 默认缓存解析结果（安全策略决定）：
  networkaddress.cache.ttl（正向缓存，默认 30s，但 -1 = 永久缓存！）
  networkaddress.cache.negative.ttl（负缓存，默认 10s）

坑：某些环境配置为永久缓存 → 域名 IP 变更后 JVM 永远连旧 IP
```

```bash
# 启动参数控制
java -Dnetworkaddress.cache.ttl=30 -jar app.jar
# 或 JDK 安全配置
# $JAVA_HOME/conf/security/java.security
# networkaddress.cache.ttl=30
```

| JVM 参数 | 作用 |
|----------|------|
| `networkaddress.cache.ttl` | 正向缓存 TTL（秒；-1 永久、0 不缓存） |
| `networkaddress.cache.negative.ttl` | 负缓存 TTL（解析失败缓存） |

> ⚠️ **Java 经典坑**：域名 IP 变更后 Java 应用仍连旧地址——优先检查 `networkaddress.cache.ttl` 是否被配成永久缓存。

### 4.2 Java 侧排查

```java
// 代码内验证解析结果
System.out.println(InetAddress.getByName("api.example.com").getHostAddress());

// 测试连通
// 用 --resolve 或直接 IP + Host 头验证（curl）
```

```bash
# 运行时验证（jinfo 查看 JVM 参数）
jinfo <pid> | grep networkaddress
```

## 5. 容器 DNS 问题

### 5.1 Docker 场景

```text
容器 DNS：默认继承宿主机 resolv.conf（或 Docker 内嵌 DNS）
坑：容器内解析宿主机域名失败（内网域名只在宿主机解析）
```

| 场景 | 解决 |
|------|------|
| 容器解析宿主机服务 | `host.docker.internal`（Docker Desktop）或 `--add-host` |
| 自定义 DNS | `--dns 8.8.8.8` 启动参数 |
| 内网域名 | `/etc/hosts` 挂载或内网 DNS 配置 |

### 5.2 K8s 场景

```text
K8s 服务发现：Service 名 → DNS（CoreDNS）
坑：跨命名空间访问需 FQDN（service.namespace.svc.cluster.local）
```

```bash
# 容器内验证 DNS
kubectl exec pod -- nslookup my-service
kubectl exec pod -- cat /etc/resolv.conf    # 看 search 域
# 常见：同命名空间用短名、跨命名空间用 FQDN
```

> 💡 K8s DNS 排查详细见 [07-容器与微服务网络排查](07-容器与微服务网络排查.md)（含 CoreDNS 故障）。

## 6. 高频场景排查表

| 场景 | 现象 | 排查要点 |
|------|------|---------|
| 域名解析失败 | Unknown host | dig status（NXDOMAIN/SERVFAIL）+ 换 DNS 对比 |
| IP 变更不生效 | 仍连旧 IP | TTL + 各级缓存 + JVM networkaddress.cache.ttl |
| 时好时坏 | 偶发解析失败 | DNS 服务器负载/多 DNS 配置顺序 |
| 内网域名不通 | 公网能解析内网不能 | resolv.conf 配置/内网 DNS 可达性 |
| 容器解析失败 | 容器内 Unknown host | Docker DNS/宿主 hosts/K8s search 域 |
| Java 永远旧 IP | 变更后 Java 不刷新 | JVM DNS 缓存参数 |

## 7. 核心要点

> 🎯 **核心要点**：
> - 解析链路：hosts → 本地缓存 → DNS 服务器 → 递归；逐环排查；
> - 对比法：本地 DNS vs 公网 DNS（8.8.8.8）结果不同 = 本地问题；
> - dig status 四态：NOERROR/NXDOMAIN（记录不存在）/SERVFAIL/TIMEOUT；
> - TTL 纪律：IP 变更前先降 TTL；变更后等缓存刷新；
> - **Java 双坑**：networkaddress.cache.ttl 永久缓存、负缓存——启动参数显式控制；
> - 容器：Docker 用 --dns/--add-host，K8s 用 Service 名 + search 域。

## 8. 参考来源

- [RFC 1035（DNS）](https://www.rfc-editor.org/rfc/rfc1035)
- [dig 手册](https://man7.org/linux/man-pages/man1/dig.1.html)
- [JDK InetAddress 缓存文档](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/net/InetAddress.html)
- [CoreDNS 官方文档](https://coredns.io/)

---

**下一模块**：[05-HTTP层问题排查](05-HTTP层问题排查.md)　/　**返回总览**：[00-总览](00-网络问题排查总览.md)
