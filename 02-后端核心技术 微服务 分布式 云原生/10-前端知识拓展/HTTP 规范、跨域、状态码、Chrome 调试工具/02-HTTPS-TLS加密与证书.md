# 02 - HTTPS / TLS 加密与证书

> 🎯 HTTP 是明文裸奔、HTTPS 加了 TLS 铠甲 — 理解 TLS 握手、证书链、双向认证，是后端安全的基础设施

---

## 目录

1. [TLS 握手过程](#1-tls-握手过程)
2. [证书链与 CA](#2-证书链与-ca)
3. [双向认证（mTLS）](#3-双向认证mtls)
4. [Java 后端证书配置](#4-java-后端证书配置)

---

## 1. TLS 握手过程

### TLS 1.2 握手（2-RTT）

```text
Client                                    Server
  │                                          │
  │── ClientHello ──────────────────────────→│  (支持的密码套件 + 随机数)
  │←─ ServerHello + Certificate ────────────│  (选密码套件 + 证书 + 随机数)
  │←─ ServerHelloDone ──────────────────────│
  │                                          │
  │── ClientKeyExchange + ChangeCipherSpec ──→│ (加密的 Pre-Master Secret)
  │── Finished ─────────────────────────────→│
  │←─ ChangeCipherSpec + Finished ──────────│
  │                                          │
  │═══════ Encrypted Data ═══════════════════│  ← 之后全部加密

时间：2 个 RTT（往返时间）
```

### TLS 1.3 握手（1-RTT / 0-RTT）

```text
TLS 1.3 改进：
  → 1-RTT：ClientHello 带密钥协商参数 → ServerHello 完成握手
  → 0-RTT：恢复会话时，第一个包就带加密数据
  → 删除不安全套件（RSA 密钥交换、SHA-1、CBC 模式）
```

---

## 2. 证书链与 CA

```text
证书链：

  Root CA（自签名，预置在操作系统/浏览器）
    │
    └── Intermediate CA（中间 CA，签发 Server 证书）
          │
          └── Server Certificate（你的网站证书）

验证过程：
  1. 浏览器收到 Server 证书
  2. 用 Intermediate CA 的公钥验证 Server 证书签名
  3. 用 Root CA 的公钥验证 Intermediate CA 证书签名
  4. Root CA 是信任锚点（预置信任）
```

| 概念 | 说明 |
|------|------|
| **CN** | Common Name — 域名 |
| **SAN** | Subject Alternative Name — 多域名支持 |
| **CRL** | Certificate Revocation List — 已吊销证书 |
| **OCSP** | Online Certificate Status Protocol — 实时查询证书状态 |
| **PFX/P12** | 含私钥的证书格式（Java Keystore） |
| **PEM** | Base64 文本格式 |

---

## 3. 双向认证（mTLS）

```text
普通 TLS（单向认证）：
  Client → 验证 Server 证书
  Server → 不验证 Client

mTLS（双向认证）：
  Client → 验证 Server 证书
  Server → 验证 Client 证书
  → 微服务间安全通信的标准方案（Istio/Consul Connect）
```

---

## 4. Java 后端证书配置

### Spring Boot HTTPS

```yaml
server:
  port: 8443
  ssl:
    key-store: classpath:keystore.p12
    key-store-password: changeit
    key-store-type: PKCS12
    key-alias: myserver
    enabled: true
```

### keytool 命令

```bash
# 生成自签名证书
keytool -genkeypair -alias myserver -keyalg RSA -keysize 2048 \
  -storetype PKCS12 -keystore keystore.p12 -validity 365 \
  -dname "CN=localhost, OU=Dev, O=Company, L=City, S=State, C=CN"

# 查看证书
keytool -list -v -keystore keystore.p12

# 导出证书
keytool -export -alias myserver -file server.crt -keystore keystore.p12
```

### RestTemplate 信任自签名证书

```java
@Bean
public RestTemplate restTemplate() throws Exception {
    SSLContext sslContext = SSLContextBuilder.create()
        .loadTrustMaterial(new File("truststore.p12"), "password".toCharArray())
        .build();
    HttpClient client = HttpClients.custom()
        .setSSLContext(sslContext)
        .build();
    return new RestTemplateBuilder()
        .requestFactory(() -> new HttpComponentsClientHttpRequestFactory(client))
        .build();
}
```

> 🎯 **生产铁律**：HTTPS 必配、TLS 1.2+ 仅用、HttpOnly+Secure Cookie、证书过期监控（<30天告警）。双向 mTLS 是零信任架构的基础。
