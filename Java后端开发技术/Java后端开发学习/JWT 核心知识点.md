# JWT 核心知识点（Java 后端企业级实战版）

> **文档定位**：Java 后端企业级技术文档 | JWT（JSON Web Token）  
> **核心特点**：无状态、跨域友好、轻量化、自包含  
> **核心用途**：登录鉴权、单点登录（SSO）、跨域认证、接口防篡改

---

## 一、核心概念

### 1.1 JWT 是什么

JWT（JSON Web Token）是一种轻量级、无状态的身份验证与信息交换规范，用于在客户端与服务端之间安全传递用户身份信息。

### 1.2 JWT vs Session

| 维度 | Session | JWT |
|------|---------|-----|
| **存储位置** | 服务端存储 | **客户端存储** |
| **状态** | 有状态 | **无状态** |
| **扩展性** | 需 Session 共享（Redis） | 天然支持分布式 |
| **跨域** | 需要额外配置 | **天然支持跨域** |
| **适用场景** | 单体项目 | 前后端分离、微服务、分布式 |

---

## 二、底层原理

### 2.1 JWT 结构（三段 Base64Url，用 `.` 分隔）

```
Header.Payload.Signature
```

#### Header（头部）

```json
{
  "alg": "HS256",
  "typ": "JWT"
}
```

- `alg`：签名算法（HS256 / RS256）
- `typ`：令牌类型（JWT）

#### Payload（载荷）

| 类型 | 字段 | 说明 |
|------|------|------|
| 注册声明 | `iss` / `sub` / `exp` / `iat` / `nbf` / `aud` | 签发人/主题/过期/签发/生效/接收方 |
| 公共声明 | 自定义 | 用户名、角色、权限等 |
| 私有声明 | 自定义 | 业务约定字段 |

> Payload 仅 Base64Url **编码**，不加密，禁止存放密码、手机号等敏感信息。

#### Signature（签名）

```
签名 = HMACSHA256(Base64Url(Header) + "." + Base64Url(Payload), secret)
```

服务端用相同密钥重新计算签名，与客户端 Token 签名对比，一致则未被篡改。

### 2.2 工作流程

```
1. 用户登录 → 账号密码校验通过
2. 服务端生成 JWT → 返回给客户端
3. 客户端存储 Token（LocalStorage / Cookie）
4. 后续请求携带 Token：Authorization: Bearer <token>
5. 服务端解析、验签、校期 → 合法则放行
```

### 2.3 HS256 vs RS256

| 维度 | HS256（对称） | RS256（非对称） |
|------|-------------|----------------|
| 密钥 | 同一密钥签发+验证 | 私钥签发、公钥验证 |
| 优点 | 实现简单、速度快 | **安全性更高** |
| 缺点 | 密钥泄露则完全失控 | 性能略低 |
| 适用 | 单体服务 | **分布式、微服务**（推荐） |

---

## 三、代码实现

### 3.1 Java 常用工具库

- **jjwt**（`io.jsonwebtoken:jjwt`）：Java 最主流 JWT 工具包
- **com.auth0:java-jwt**：轻量易用的实现

### 3.2 Java 生成/验证 JWT 示例

```java
// 生成 JWT（HS256）
String jwt = Jwts.builder()
    .setSubject("10001")                       // 用户 ID
    .claim("username", "zhangsan")             // 自定义字段
    .setIssuedAt(new Date())                   // 签发时间
    .setExpiration(new Date(System.currentTimeMillis() + 3600000)) // 1小时过期
    .signWith(SignatureAlgorithm.HS256, secretKey)
    .compact();

// 验证 JWT
Claims claims = Jwts.parser()
    .setSigningKey(secretKey)
    .parseClaimsJws(jwt)
    .getBody();
String userId = claims.getSubject();
```

---

## 四、实战要点

| 要点 | 说明 |
|------|------|
| **AccessToken + RefreshToken** | AccessToken 短期（15min），RefreshToken 长期（7d），减少频繁登录 |
| **HTTPS 强制** | Token 在 HTTP 头明文传输，必须 HTTPS 防窃取 |
| **密钥管理** | 密钥复杂度高、定期轮换，生产环境用环境变量/KMS |
| **Payload 最小化** | 仅存用户 ID 和必要字段，敏感信息禁入 |

---

## 五、避坑总结

### 5.1 常见问题

| 问题 | 解决方案 |
|------|----------|
| **JWT 无法主动注销** | Redis 黑名单记录失效 Token；缩短过期 + RefreshToken |
| **Token 泄露** | HTTPS 传输 + 短过期时间 + 设备指纹绑定 |
| **续签困难** | RefreshToken 机制：AccessToken 过期后用 RefreshToken 换新 |
| **Payload 存敏感信息** | 仅存用户 ID，其余敏感数据从服务端查 |

### 5.2 核心面试题

1. **JWT vs Session** → JWT 无状态、分布式友好；Session 可控性强
2. **RefreshToken 作用** → AccessToken 短期鉴权，RefreshToken 长期刷新，避免频繁登录
3. **JWT 为什么安全** → 签名防篡改 + 过期时间限制 + HTTPS 传输
4. **Payload 为什么不能存敏感信息** → 仅 Base64 编码，可直接解码，无加密保护

---

## 六、企业级最佳实践

| 规范 | 说明 |
|------|------|
| **HTTPS 必须** | 防止 Token 在传输中被窃取 |
| **密钥高复杂度** | 定期轮换，使用 KMS 管理 |
| **合理过期时间** | AccessToken 15 分钟，RefreshToken 7 天 |
| **微服务用 RS256** | 非对称加密，私钥签发，公钥验证 |
| **Payload 最小化** | 仅 userId，禁止存放密码/手机号 |
| **Redis 黑名单** | 解决无法主动注销问题 |
