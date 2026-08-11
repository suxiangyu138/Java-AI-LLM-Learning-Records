# 05-HTTP 层问题排查
> TCP 通了但 HTTP 失败/超时/慢：超时配置、代理与网关、TLS 证书、负载均衡——应用层问题的定位方法

## 📚 目录
1. [HTTP 问题分层](#1-http-问题分层)
2. [超时问题：connect/read/总时长](#2-超时问题connectread总时长)
3. [重试与幂等](#3-重试与幂等)
4. [代理与网关介入](#4-代理与网关介入)
5. [TLS/HTTPS 问题](#5-tlshttps-问题)
6. [负载均衡场景](#6-负载均衡场景)
7. [curl -v 全链路解读](#7-curl--v-全链路解读)
8. [核心要点](#8-核心要点)
9. [参考来源](#9-参考来源)

## 1. HTTP 问题分层

```text
HTTP 请求的生命周期：
DNS → TCP 连接 → TLS 握手 → 发送请求 → 等待响应 → 接收响应

各阶段失败 = 不同层问题：
  卡在 TCP 连接 ← 传输层（见 03）
  卡在 TLS 握手 ← 证书/协议
  卡在等待响应 ← 服务端处理（超时配置）
  收到错误状态码 ← 应用层业务
```

| 现象 | 阶段 | 排查工具 |
|------|------|---------|
| 连接超时 | TCP 连接 | telnet/ss（见 03） |
| TLS 握手失败 | TLS | openssl s_client |
| 请求超时（读超时） | 等待响应 | 服务端日志/耗时指标 |
| 5xx/4xx | 应用层 | 应用日志/状态码统计 |

## 2. 超时问题：connect/read/总时长

### 2.1 三种超时

| 超时 | 含义 | Java 配置示例 |
|------|------|--------------|
| connectTimeout | 建立 TCP 连接的最大等待 | `HttpClient.connectTimeout(5s)` |
| readTimeout | 发送请求后等待响应的最大时间 | `HttpClient.responseTimeout(10s)` |
| 总时长 | 整个请求（含重试）上限 | 客户端/网关超时链 |

```text
超时链原则（网关/负载均衡 > 应用 > 下游）：
  Nginx proxy_read_timeout 60s
  Spring Boot（服务端）server.tomcat.connection-timeout
  应用 HTTP 客户端 readTimeout 10s（调用下游）
  数据库连接池 connectionTimeout 3s

⚠️ 反例：客户端 readTimeout 5s，下游正常处理要 8s → 客户端超时重试 → 双重请求
```

> 🎯 **超时链设计**：**调用方超时 ≥ 被调方处理时间上限**；网关超时 ≥ 应用超时——层层递减会导致"明明在处理却报超时"。

### 2.2 慢请求定位（耗时分解）

```bash
curl -w 'DNS:%{time_namelookup} 连接:%{time_connect} TLS:%{time_appconnect} 首字节:%{time_starttransfer} 总:%{time_total}\n' url
```

| 耗时分布 | 定位 |
|----------|------|
| DNS 大 | DNS 慢（见 04） |
| 连接大 | 网络/服务端队列 |
| TLS 大 | 握手开销（会话复用/加密强度） |
| 首字节大 | **服务端处理慢**（应用/DB/GC） |

## 3. 重试与幂等

### 3.1 重试的坑

```text
超时自动重试 → 非幂等接口（下单/支付）重复执行 → 业务事故
```

| 场景 | 重试策略 |
|------|---------|
| GET/查询 | 可安全重试 |
| POST 下单 | 必须幂等键（Idempotency-Key）或禁止自动重试 |
| 超时后重试 | 只重试"明确未执行"（连接失败）——响应超时**不确定**是否执行 |
| 重试次数 | 有限次（1-2 次）+ 退避 |

> ⚠️ **超时重试的本质风险**：readTimeout 超时 ≠ 请求没到——服务端可能已处理成功，重试 = 重复执行。**写操作必须幂等**（业务幂等键）。

## 4. 代理与网关介入

### 4.1 问题特征

```text
请求经过 Nginx/网关/代理时：
  连接池超时回收 → 上游 504/502
  缓冲设置 → 大响应被截断
  头修改 → 下游拿不到原始头
  keepalive 配置 → 连接复用异常
```

### 4.2 排查

```bash
# 绕过代理直连（对比法）
curl -v http://10.0.0.5:8080/health          # 直连
curl -v -x http://nginx:80 http://10.0.0.5:8080/health   # 走代理
# 直连通、代理不通 → 问题在代理配置

# 看代理日志
tail -f /var/log/nginx/access.log
tail -f /var/log/nginx/error.log             # 502/504 现场
```

| 网关现象 | 常见根因 |
|----------|---------|
| 502 Bad Gateway | 上游未启动/连接失败 |
| 504 Gateway Timeout | 上游处理超时（proxy_read_timeout） |
| 499（Nginx） | 客户端断开（应用响应慢） |
| 大响应截断 | proxy_buffering/缓冲区过小 |

> 💡 Nginx 499 是"客户端等不及先走了"的信号——配合应用日志看处理耗时，通常是应用慢而非网络。

## 5. TLS/HTTPS 问题

```bash
# TLS 握手诊断
openssl s_client -connect api.example.com:443 -servername api.example.com
# 输出关键：
#   CONNECTED / TLS handshake 完成
#   subject/issuer（证书信息）
#   Verify return code: 0 (ok) / 20 (unable to get issuer)

# 证书有效期/链
openssl s_client -connect api.example.com:443 | grep -E 'subject|issuer|Verify'

# 证书过期检查
echo | openssl s_client -connect api.example.com:443 2>/dev/null \
  | openssl x509 -noout -dates
```

| 现象 | 根因 |
|------|------|
| 证书过期 | 证书未续期（监控证书有效期告警） |
| 证书链不完整 | 缺少中间证书（Let's Encrypt 常见） |
| 域名不匹配 | 证书 SAN 不含访问域名 |
| TLS 版本不兼容 | 服务端只支持 TLS1.2，客户端强制 1.3 |
| 自签名 | 客户端未信任 CA（`--verify=no` 仅测试） |

> 🎯 **证书监控**：证书 7 天内过期是标配告警（见 [05-告警体系与SLO](../日志监控指标/05-告警体系与SLO.md)）——过期是"定时炸弹"，必须提前告警。

## 6. 负载均衡场景

```text
多实例/多节点场景的偶发失败：
  某个实例不健康 → LB 仍转发（健康检查延迟）
  会话保持（sticky）→ 单实例过载
  连接池在 LB 层超时 → 上游重试双倍压力
```

| 排查 | 命令 |
|------|------|
| 各实例健康 | 逐个访问实例 IP + Host 头 |
| LB 后端列表 | Nginx upstream / K8s endpoints |
| 会话分布 | 检查 sticky cookie/来源 IP |
| 健康检查配置 | LB 健康检查间隔/失败阈值 |

```bash
# 逐实例测试（绕过 LB）
curl -v -H "Host: api.example.com" http://10.0.0.5:8080/health
curl -v -H "Host: api.example.com" http://10.0.0.6:8080/health
# 某实例失败 → 该实例问题（从 LB 摘除排查）
```

> 💡 **逐实例对比法**：LB 偶发失败时，直接访问每个后端实例——**一个坏实例会让整体"偶发失败"**。

## 7. curl -v 全链路解读

```bash
curl -v https://api.example.com/orders
```

```text
* Trying 10.0.0.5:443...                   ① TCP 连接（卡住 = 传输层）
* Connected to api.example.com (10.0.0.5)  ② TCP 成功
* TLS handshake completed                  ③ TLS 成功
* Server certificate: CN=api.example.com   ④ 证书检查（Verify return code: 0）
> GET /orders HTTP/1.1                     ⑤ 请求发出
> Host: api.example.com
< HTTP/1.1 200 OK                          ⑥ 收到响应
< Content-Type: application/json
```

| 卡在步骤 | 定位 | 下一步 |
|----------|------|--------|
| ① Trying 卡住 | 网络/端口 | telnet + ss（03） |
| ③ TLS 卡住/失败 | 证书/协议 | openssl s_client（05） |
| ⑤ 后无响应 | 服务端处理 | 服务端日志/耗时指标 |
| ⑥ 状态码异常 | 应用层 | 应用日志/业务排查 |

## 8. 核心要点

> 🎯 **核心要点**：
> - HTTP 生命周期五阶段：DNS → TCP → TLS → 请求 → 响应——每阶段失败对应不同层；
> - 超时三兄弟：connect（建连）/ read（等响应）/ 总时长；**超时链必须层层递减或对齐**；
> - 超时重试的最大风险：请求可能已执行——**写操作必须幂等**；
> - 代理介入排查：直连 vs 走代理对比法；Nginx 502/504/499 各有指向；
> - TLS 四坑：过期/链不完整/域名不匹配/版本不兼容；证书监控是标配告警；
> - LB 偶发失败：逐实例对比法定位坏实例；
> - curl -v 五阶段解读 = HTTP 排障的完整骨架。

## 9. 参考来源

- [RFC 9110（HTTP Semantics）](https://www.rfc-editor.org/rfc/rfc9110)
- [curl 官方文档](https://curl.se/docs/manpage.html)
- [Nginx 官方文档（proxy 超时/缓冲）](https://nginx.org/en/docs/http/ngx_http_proxy_module.html)
- [openssl s_client 手册](https://docs.openssl.org/master/man1/openssl-s_client/)

---

**下一模块**：[06-Java应用网络问题排查](06-Java应用网络问题排查.md)　/　**返回总览**：[00-总览](00-网络问题排查总览.md)
