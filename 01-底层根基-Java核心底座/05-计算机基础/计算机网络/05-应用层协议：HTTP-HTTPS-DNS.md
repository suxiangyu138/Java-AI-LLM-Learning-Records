# 应用层协议：HTTP-HTTPS-DNS
> 核心定位：应用层是直接面向业务通信的层级，定义了应用程序之间的数据交换规则，是后端开发者日常接触最多的网络层级

## 目录
1. [应用层概述](#1-应用层概述)
2. [HTTP协议基础](#2-http协议基础)
3. [HTTP请求与响应](#3-http请求与响应)
4. [HTTP版本演进](#4-http版本演进)
5. [HTTPS与SSL-TLS](#5-https与ssl-tls)
6. [DNS域名系统](#6-dns域名系统)
7. [其他应用层协议](#7-其他应用层协议)
8. [Java后端关联](#8-java后端关联)

---

## 1. 应用层概述

### 1.1 应用层的定位

应用层是TCP/IP协议栈中最顶层，直接为用户的应用程序提供网络服务。

| 层级 | 功能 | 代表协议 |
|------|------|----------|
| 应用层 | 面向具体业务的数据交换 | HTTP, DNS, FTP, SMTP |
| 传输层 | 端到端的可靠/高效传输 | TCP, UDP |
| 网络层 | IP寻址与路由选择 | IP, ICMP |
| 网络接口层 | 物理介质信号传输 | 以太网, WiFi |

### 1.2 核心特征与模型

**核心特征**：直接面向用户、协议多样性、基于传输层（TCP/UDP）、持续演进。

**客户端-服务器模型**：客户端主动发起请求，服务器被动等待并提供服务。

```
客户端 ──── HTTP请求 ────→ 服务器
       ←─── HTTP响应 ────
```

---

## 2. HTTP协议基础

### 2.1 HTTP核心定义

HTTP（HyperText Transfer Protocol）是基于TCP/IP的应用层协议，用于客户端与服务器之间的超文本数据传输。

| 特性 | 说明 |
|------|------|
| 客户端-服务器模型 | 严格遵循请求-响应模式 |
| 无状态 | 每次请求独立，需Cookie/Session维持状态 |
| 基于TCP | HTTP/1.x/2基于TCP，HTTP/3基于QUIC(UDP) |
| 可扩展 | 通过请求头、状态码等机制灵活扩展 |

### 2.2 URI与URL

| 概念 | 核心作用 | 是否包含协议 | 示例 |
|------|----------|-------------|------|
| URI（统一资源标识符） | 标识资源，不关心访问方式 | 可选 | `urn:isbn:9787111641247` |
| URL（统一资源定位符） | 定位并访问资源 | 必须包含 | `https://www.baidu.com` |

URL完整结构：`scheme://host:port/path?query#fragment`

---

## 3. HTTP请求与响应

### 3.1 请求格式

HTTP请求由三部分组成：**请求行 + 请求头 + 请求体**。

```
GET /api/user?id=1 HTTP/1.1\r\n
Host: localhost:8080\r\n
Accept: application/json\r\n
\r\n
```

**请求行**：`请求方法 请求URI HTTP版本`

### 3.2 HTTP方法

| 方法 | 用途 | 请求体 | 幂等 | 安全 |
|------|------|--------|------|------|
| GET | 获取资源 | 无 | 是 | 是 |
| POST | 创建/提交资源 | 有 | 否 | 否 |
| PUT | 全量更新资源 | 有 | 是 | 否 |
| DELETE | 删除资源 | 可选 | 是 | 否 |
| PATCH | 部分更新资源 | 有 | 否 | 否 |
| HEAD | 获取响应头 | 无 | 是 | 是 |
| OPTIONS | 查询支持的HTTP方法 | 无 | 是 | 是 |

### 3.3 常见请求头

| 请求头 | 作用 | 示例 |
|--------|------|------|
| `Host` | 目标服务器域名（HTTP/1.1必填） | `Host: www.example.com` |
| `Content-Type` | 请求体数据格式 | `application/json` |
| `User-Agent` | 客户端标识 | `User-Agent: Java-http-client/11` |
| `Cookie` | 携带Cookie | `Cookie: sessionId=abc123` |
| `Authorization` | 身份验证凭据 | `Authorization: Bearer eyJhbGci...` |
| `Accept` | 可接受的响应格式 | `Accept: application/json` |
| `Cache-Control` | 缓存控制 | `Cache-Control: no-cache` |

`Content-Type` 常用值：`application/json`（RESTful API）、`application/x-www-form-urlencoded`（表单）、`multipart/form-data`（文件上传）。

### 3.4 响应格式

**响应行**：`HTTP版本 状态码 状态描述`（如 `HTTP/1.1 200 OK`）

### 3.5 状态码

| 类别 | 范围 | 含义 | 关键状态码 |
|------|------|------|-----------|
| 1xx 信息性 | 100-199 | 临时响应 | 101 Switching Protocols |
| 2xx 成功 | 200-299 | 请求成功 | **200 OK**, **201 Created**, 204 No Content |
| 3xx 重定向 | 300-399 | 需进一步操作 | 301 Moved Permanently, **302 Found**, **304 Not Modified** |
| 4xx 客户端错误 | 400-499 | 请求包含错误 | **400 Bad Request**, **401 Unauthorized**, 403 Forbidden, **404 Not Found**, **429 Too Many Requests** |
| 5xx 服务端错误 | 500-599 | 服务器处理失败 | **500 Internal Server Error**, 502 Bad Gateway, **503 Service Unavailable**, 504 Gateway Timeout |

### 3.6 常见响应头

| 响应头 | 作用 | 示例 |
|--------|------|------|
| `Content-Type` | 响应体数据格式 | `Content-Type: application/json` |
| `Content-Length` | 响应体长度（字节） | `Content-Length: 1024` |
| `Set-Cookie` | 设置Cookie | `Set-Cookie: sessionId=abc123; HttpOnly` |
| `Cache-Control` | 缓存策略 | `Cache-Control: max-age=3600` |
| `Location` | 重定向目标URL | `Location: https://new-site.com` |

---

## 4. HTTP版本演进

### 4.1 HTTP/0.9（1991）

仅GET方法，无请求头/响应头/状态码，仅支持HTML。

### 4.2 HTTP/1.0（1996）

引入请求头、响应头、状态码，支持多种文件类型。**短连接**：每次请求需新建立TCP连接。

### 4.3 HTTP/1.1（1999）

| 核心改进 | 说明 |
|----------|------|
| 持久连接 | 默认keep-alive，复用TCP连接 |
| 管道化 | 请求可连续发送，但响应仍按序返回（队头阻塞） |
| Host头 | 支持单服务器托管多个域名（虚拟主机） |
| 分块传输 | 支持 `Transfer-Encoding: chunked` |
| 缓存增强 | `Cache-Control`、`ETag` |

### 4.4 HTTP/2（2015）

| 核心改进 | 说明 |
|----------|------|
| 二进制分帧 | 文本→二进制，解析更高效 |
| 多路复用 | 同一连接并发处理多个请求，消除队头阻塞 |
| 头部压缩 | HPACK算法压缩冗余请求头 |
| 服务器推送 | 服务器可主动推送资源 |

> TCP层的队头阻塞仍然存在（因底层仍是TCP）。

### 4.5 HTTP/3（2022）

| 核心改进 | 说明 |
|----------|------|
| 基于QUIC | 基于UDP而非TCP，减少连接建立延迟 |
| 0-RTT握手 | 复用场景跳过握手直接发送数据 |
| 无队头阻塞 | QUIC在应用层处理丢包 |
| 连接迁移 | WiFi→移动网络切换不中断 |

### 4.6 版本对比

| 特性 | HTTP/1.1 | HTTP/2 | HTTP/3 |
|------|----------|--------|--------|
| 底层传输 | TCP | TCP | QUIC (UDP) |
| 数据格式 | 文本 | 二进制分帧 | 二进制 |
| 多路复用 | 否 | 是 | 是 |
| 头部压缩 | 否 | HPACK | QPACK |
| 服务器推送 | 否 | 是 | 是 |
| 队头阻塞 | 有 | 传输层（TCP） | 无 |
| 连接建立 | TCP三次握手 | TCP+TLS | 0-1 RTT |

---

## 5. HTTPS与SSL-TLS

### 5.1 HTTPS概述

HTTPS = HTTP + SSL/TLS，提供三大保障：

| 保障 | 说明 | 实现方式 |
|------|------|----------|
| **加密** | 防窃听 | 对称加密（数据）+ 非对称加密（密钥交换） |
| **完整性** | 防篡改 | 消息认证码（MAC） |
| **身份验证** | 防中间人攻击 | 数字证书（CA签名） |

### 5.2 HTTP vs HTTPS

| 维度 | HTTP | HTTPS |
|------|------|-------|
| 传输 | 明文 | 加密（SSL/TLS） |
| 默认端口 | 80 | 443 |
| 安全性 | 低 | 高 |
| 性能 | 略快 | 握手+加密开销（现代硬件影响很小） |
| 证书 | 不需要 | 需要CA签发证书 |

> 现代Web开发中HTTPS已成为标配。

### 5.3 TLS协议版本

| 版本 | 发布 | 状态 |
|------|------|------|
| SSL 2.0/3.0 | 1995/1996 | 已废弃/禁用 |
| TLS 1.0/1.1 | 1999/2006 | 逐步淘汰 |
| **TLS 1.2** | **2008** | **当前主流** |
| **TLS 1.3** | **2018** | **逐步普及** |

### 5.4 TLS 1.2握手流程

```
客户端                             服务器
  │                                   │
  │─── Client Hello ──────────────→   │
  │   (TLS版本、加密套件列表、随机数)   │
  │                                   │
  │←── Server Hello ──────────────────│
  │   (选定版本、加密套件、服务器随机数) │
  │←── Certificate ──────────────────│
  │   (数字证书，含公钥)                │
  │←── Server Hello Done ────────────│
  │                                   │
  │   客户端验证证书（CA链校验）          │
  │   生成预主密钥                      │
  │                                   │
  │─── Client Key Exchange ────────→   │
  │   (用服务器公钥加密预主密钥)         │
  │                                   │
  │   双方计算会话密钥                  │
  │   (客户端随机+服务端随机+预主密钥)    │
  │                                   │
  │─── Change Cipher Spec / Finished → │
  │←── Change Cipher Spec / Finished ─│
  │                                   │
  │   开始对称加密通信                   │
```

### 5.5 TLS 1.2 vs TLS 1.3

| 对比维度 | TLS 1.2 | TLS 1.3 |
|---------|---------|---------|
| 握手轮次 | 2-RTT | 1-RTT（首次）/ 0-RTT（复用） |
| 加密套件 | 多种组合（含不安全项） | 仅安全套件（AEAD） |
| 密钥交换 | RSA、DH、ECDH多种 | 仅(EC)DHE |
| 会话恢复 | Session ID / Ticket | PSK预共享密钥 |

### 5.6 证书链

```
根CA (Root CA)
  └── 中间CA (Intermediate CA)
       └── 服务器证书 (Server Certificate)
```

**验证流程**：有效期检查 → 域名匹配 → 中间CA签名验证 → 根CA签名验证（根CA证书内置于操作系统/浏览器成为信任锚）。

| 证书类型 | 验证级别 | 适用场景 |
|----------|---------|----------|
| DV（域名验证） | 低 | 个人网站 |
| OV（组织验证） | 中 | 企业官网 |
| EV（扩展验证） | 高 | 银行、金融 |

> 自签名证书仅用于开发测试环境，生产环境必须使用CA签发证书。

---

## 6. DNS域名系统

### 6.1 核心作用

DNS将人类易记的域名（`www.baidu.com`）转换为机器识别的IP地址（`110.242.68.66`），相当于互联网的"电话簿"。

### 6.2 域名层次结构

```
根域 (.)
  └── 顶级域 (.com .org .cn)
       └── 二级域 (example.com)
            └── 三级域 (www.example.com)
```

### 6.3 解析过程

```
浏览器输入 www.baidu.com
       │
   浏览器缓存（TTL控制）
       │
   操作系统缓存（hosts文件）
       │
   本地DNS服务器（递归查询）
       │
       ├──→ 根DNS服务器 → 返回 .com 顶级域服务器地址
       ├──→ .com 顶级域服务器 → 返回 baidu.com 权威服务器地址
       └──→ baidu.com 权威服务器 → 返回 IP: 110.242.68.66
       │
   返回IP → 浏览器发起HTTP请求
```

**关键概念**：

| 概念 | 说明 |
|------|------|
| 递归查询 | 本地DNS服务器代替客户端完成完整解析 |
| 迭代查询 | 本地DNS服务器逐个询问各级DNS服务器 |
| 权威服务器 | 存储域名实际记录的服务器 |
| TTL | 缓存有效期 |

### 6.4 DNS记录类型

| 类型 | 作用 | 示例 |
|------|------|------|
| **A** | 域名→IPv4 | `example.com. 3600 IN A 93.184.216.34` |
| **AAAA** | 域名→IPv6 | `example.com. 3600 IN AAAA 2606:2800:220::1` |
| **CNAME** | 域名→别名 | `www.example.com. 3600 IN CNAME example.com` |
| **MX** | 邮件服务器 | `example.com. 3600 IN MX 10 mail.example.com` |
| **NS** | 权威DNS服务器 | `example.com. 3600 IN NS ns1.example.com` |
| **TXT** | 文本信息 | `example.com. 3600 IN TXT "v=spf1 include:_spf.example.com"` |
| **PTR** | IP→域名（反向） | `34.216.184.93.in-addr.arpa. IN PTR example.com` |

### 6.5 DNS缓存层级

| 层级 | 位置 | 特点 |
|------|------|------|
| 浏览器缓存 | 浏览器 | 命中率最高 |
| OS缓存 | hosts文件+DNS Client | 可手动配置 |
| ISP缓存 | 本地DNS服务器 | 缓存最近查询 |
| 公共DNS | 如8.8.8.8 | 大容量缓存 |

### 6.6 DNS安全

DNS over HTTPS (DoH) 和 DNS over TLS (DoT) 加密DNS请求，防ISP劫持。DNSSEC通过数字签名验证DNS记录真实性。

---

## 7. 其他应用层协议

### 7.1 常见协议端口速查

| 协议 | 默认端口 | 传输层 | 作用 |
|------|---------|--------|------|
| HTTP | 80 | TCP | Web通信 |
| HTTPS | 443 | TCP | 加密Web通信 |
| FTP控制 | 21 | TCP | 文件传输控制 |
| FTP数据 | 20 | TCP | 文件传输数据 |
| SMTP | 25/587 | TCP | 发送邮件 |
| POP3 | 110 | TCP | 接收邮件(下载) |
| IMAP | 143 | TCP | 接收邮件(服务端保留) |
| SSH | 22 | TCP | 远程安全登录 |
| DHCP | 67/68 | UDP | 自动分配IP |
| DNS | 53 | UDP/TCP | 域名解析 |
| WebSocket | 80/443 | TCP | 全双工通信 |

### 7.2 邮件协议对比

| 协议 | 方向 | 特点 |
|------|------|------|
| SMTP | 发送 | 推协议，邮件提交到服务器 |
| POP3 | 接收 | 下载到本地阅读，服务端删除 |
| IMAP | 接收 | 在服务端管理邮件，多端同步 |

---

## 8. Java后端关联

### 8.1 Java HttpClient（Java 11+）

```java
// GET请求
HttpClient client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build();
HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create("https://api.example.com/user?id=1"))
        .header("Accept", "application/json")
        .GET()
        .build();
HttpResponse<String> response = client.send(request,
        HttpResponse.BodyHandlers.ofString());
System.out.println("状态码: " + response.statusCode());
System.out.println("响应体: " + response.body());
```

```java
// POST请求
HttpClient client = HttpClient.newHttpClient();
String json = "{\"username\":\"admin\"}";
HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create("https://api.example.com/login"))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(json))
        .build();
HttpResponse<String> response = client.send(request,
        HttpResponse.BodyHandlers.ofString());
```

### 8.2 HttpURLConnection

```java
URL url = new URL("https://api.example.com/user?id=1");
HttpURLConnection conn = (HttpURLConnection) url.openConnection();
conn.setRequestMethod("GET");
conn.setConnectTimeout(5000);
conn.setReadTimeout(5000);

if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
    try (BufferedReader br = new BufferedReader(
            new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
        System.out.println(br.lines().collect(Collectors.joining()));
    }
}
conn.disconnect();
```

### 8.3 Java HTTP框架对比

| 框架 | JDK | 特点 | 适用场景 |
|------|-----|------|----------|
| HttpURLConnection | 1.1+ | 内置无依赖 | 简单请求 |
| HttpClient | 11+ | 异步、连接池 | Java 11+项目推荐 |
| OkHttp | 第三方 | 高性能、拦截器 | Android/Java |
| Apache HttpClient | 第三方 | 精细连接池配置 | 企业项目 |
| Spring WebClient | Spring 5+ | 响应式非阻塞 | Spring WebFlux |

### 8.4 Java DNS与URI操作

```java
// DNS解析
InetAddress addr = InetAddress.getByName("www.baidu.com");
System.out.println("IP: " + addr.getHostAddress());

// 获取多个IP
InetAddress[] all = InetAddress.getAllByName("www.baidu.com");

// URL解析
URL url = new URL("https://www.example.com:8080/docs/api?name=java");
System.out.println("协议: " + url.getProtocol());
System.out.println("端口: " + url.getPort());

// URI相对路径解析
URI base = URI.create("https://www.example.com/docs/");
URI resolved = base.resolve("test.html");
System.out.println("解析后: " + resolved);  // /docs/test.html
```

---

> 应用层协议是后端开发者的"日常语言"——HTTP设计API、HTTPS保障安全、DNS做服务发现，三者是后端架构最常打交道的协议。
