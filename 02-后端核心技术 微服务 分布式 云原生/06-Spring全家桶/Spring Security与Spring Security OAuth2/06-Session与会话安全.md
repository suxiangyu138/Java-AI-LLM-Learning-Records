# 06 Session 与会话安全

> 有状态认证的核心战场：会话固定攻击与 7.0 的默认防御、并发会话控制、Remember-Me 令牌机制，以及无状态 JWT 与 Session 的架构取舍

---

## 📚 目录

1. [会话的本质与安全威胁面](#1-会话的本质与安全威胁面)
2. [会话固定攻击与防御](#2-会话固定攻击与防御)
3. [并发会话控制](#3-并发会话控制)
4. [Remember-Me 令牌机制](#4-remember-me-令牌机制)
5. [会话管理配置全解](#5-会话管理配置全解)
6. [无状态 JWT vs Session：架构取舍](#6-无状态-jwt-vs-session架构取舍)

---

## 1. 会话的本质与安全威胁面

**会话（Session）**：服务器为已认证用户维护的上下文状态，通过 Cookie（`JSESSIONID`）关联。

```text
认证成功 → 服务器创建 Session（内存/Redis）→ 返回 JSESSIONID Cookie
后续请求 → 带 Cookie → SecurityContextHolderFilter 从 Session 恢复身份
```

| 威胁 | 攻击方式 | 防御 |
|------|---------|------|
| 会话固定 | 预置 Session ID 钓鱼 | 认证成功后更换 ID（7.0 默认） |
| 会话劫持 | 窃取 Cookie（XSS/中间人） | Secure Cookie + HTTPS + 绑定校验 |
| 会话失效不彻底 | 登出后 ID 仍可用 | 登出销毁 Session + 清 Cookie |
| 并发滥用 | 多端同时登录 | 并发会话控制 |

> 🎯 **要点**：会话安全的核心 = "认证前后会话身份要变"（防固定）+ "传输要加密"（防窃取）+ "登出要彻底"（防复活）。

## 2. 会话固定攻击与防御

### 2.1 攻击模型

```text
1. 攻击者自己登录站点 → 拿到合法 Session ID（如 SID=abc123）
2. 诱导受害者用该 ID 访问站点（URL 携带 / 预置 Cookie）
3. 受害者登录 → 若服务器不换 Session ID，认证后的会话仍是 SID=abc123
4. 攻击者用 SID=abc123 直接冒充受害者
```

### 2.2 防御（7.0 默认已开）

| 机制 | 行为 | Spring 默认 |
|------|------|:---:|
| `changeSessionId` | 认证成功后**更换 Session ID**（保留会话数据） | ✅ 默认 |
| `migrateSession` | 换 ID 并迁移数据（旧） | 被 changeSessionId 取代 |
| `newSession` | 认证后建全新会话 | 可选 |
| `none` | 不处理（危险） | 不推荐 |

```java
http.sessionManagement(sm -> sm
        .sessionFixation(fix -> fix.changeSessionId())   // 默认即此，显式写出更清晰
        .maximumSessions(1));                            // 并发控制（见下节）
```

> 💡 **验证方法**：登录前后抓 Cookie 对比 `JSESSIONID` 是否变化——变了说明防固定生效。

## 3. 并发会话控制

### 3.1 需求与配置

```java
@Bean
public SecurityFilterChain chain(HttpSecurity http, SessionRegistry sessionRegistry) throws Exception {
    http.sessionManagement(sm -> sm
            .maximumSessions(1)                       // 每用户最多 1 个会话
            .maxSessionsPreventsLogin(true)           // true:新登录被拒; false:旧会话被踢（默认）
            .expiredUrl("/login?expired")             // 会话过期跳转
            .sessionRegistry(sessionRegistry));       // 会话注册表（见 3.2）
    return http.build();
}
```

| 选项 | 行为 |
|------|------|
| `maximumSessions(n)` | 每用户最多 n 个会话 |
| `maxSessionsPreventsLogin=true` | 超出时**拒绝新登录** |
| `maxSessionsPreventsLogin=false` | 超出时**踢掉最旧会话**（默认） |
| `expiredUrl` | 会话被踢后访问的跳转 |

### 3.2 SessionRegistry：会话的"户口本"

```java
@Bean
public SessionRegistry sessionRegistry() {
    return new SessionRegistryImpl();
}

// 业务侧使用：踢人下线、在线用户统计
@Resource
private SessionRegistry sessionRegistry;

public void kickUser(String username) {
    sessionRegistry.getAllPrincipals().stream()
        .filter(p -> ((UserDetails) p).getUsername().equals(username))
        .flatMap(p -> sessionRegistry.getAllSessions(p, false).stream())
        .forEach(session -> session.expireNow());     // 强制下线
}
```

> ⚠️ **集群注意**：默认 `SessionRegistryImpl` 是**本地内存**——多实例部署时踢人只对本实例生效。需要 Redis 共享的会话注册表（Spring Session 或自研）。

## 4. Remember-Me 令牌机制

**Remember-Me**：登录时勾选"记住我"，关闭浏览器后仍保持登录（通过持久化令牌 Cookie）。

### 4.1 两种实现

| 实现 | 机制 | 安全性 | 场景 |
|------|------|:---:|------|
| `TokenBasedRememberMeServices` | Cookie 内嵌加密令牌（用户名+过期时间+密钥签名） | 中（泄露即冒用） | 简单场景 |
| `PersistentTokenBasedRememberMeServices` | DB 存随机 token 哈希，Cookie 只存序列 | 高（服务端可撤销） | 生产推荐 |

### 4.2 配置

```java
// 简单版（内存密钥）
http.rememberMe(rm -> rm.key("my-secret-key").tokenValiditySeconds(7 * 24 * 3600));

// 生产版（持久化）
@Bean
PersistentTokenRepository persistentTokenRepository(DataSource ds) {
    return new JdbcTokenRepositoryImpl() {{ setDataSource(ds); setCreateTableOnStartup(false); }};
}

http.rememberMe(rm -> rm
        .rememberMeParameter("remember-me")            // 登录表单勾选参数
        .tokenRepository(persistentTokenRepository)    // 持久化仓库
        .tokenValiditySeconds(7 * 24 * 3600));         // 7 天
```

### 4.3 Remember-Me 与 CSRF/安全边界

- Remember-Me 会话**不等于完整登录**（`rememberMe()` 授权规则可单独对待敏感操作）；
- Remember-Me 令牌泄露 = 长期冒用风险——生产用 PersistentToken 并支持"踢出所有记住会话"；
- 7.0 的令牌格式随 Jackson 3 变化：**旧 6.x 的 Remember-Me Cookie 升级后可能失效**（需重新登录）——迁移注意。

## 5. 会话管理配置全解

```java
http.sessionManagement(sm -> sm
        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)  // 默认
        .sessionFixation(fix -> fix.changeSessionId())
        .invalidSessionUrl("/login?invalid")          // 失效会话跳转
        .maximumSessions(2));
```

| `SessionCreationPolicy` | 语义 | 适用 |
|------------------------|------|------|
| `IF_REQUIRED`（默认） | 需要时创建 | 常规 Web |
| `ALWAYS` | 总是创建 | 兼容旧逻辑 |
| `NEVER` | 不主动创建（已有则用） | 半无状态 |
| `STATELESS` | 完全不使用 Session | **无状态 API** |

> ⚠️ **STATELESS + 表单登录**：无状态策略下 SecurityContext 不持久化——认证信息仅存活于当次请求，表单登录毫无意义（每次请求都要重新认证）。**STATELESS 必须配 JWT/Bearer 认证**（08 篇），这是最常见的配置错配。

## 6. 无状态 JWT vs Session：架构取舍

| 维度 | Session（有状态） | JWT（无状态） |
|------|:---:|:---:|
| 服务端存储 | 需要（内存/Redis） | 不需要（令牌自含） |
| 水平扩展 | 需共享 Session（Spring Session + Redis） | 天然多实例无状态 |
| 登出/踢人 | 即时（删 Session） | **难**（令牌在手即有效，需黑名单） |
| 令牌泄露 | 作用域有限（可撤销） | 泄露即冒用至过期 |
| 数据量 | 服务端可控 | 令牌体积随 claim 膨胀（HTTP 头大小） |
| 典型场景 | 传统 Web、管理后台 | 微服务 API、移动端 |

**微服务时代的常见答案：**

```text
网关层：统一认证 → 校验/签发 JWT（无状态放行内部服务）
内部服务：信任网关（内网互信）或各自校验 JWT
会话状态需要共享时：Spring Session + Redis（见 Spring Data Redis 体系）
登出即时性要求高：JWT + 黑名单（Redis 存 jti）或缩短有效期 + 刷新令牌
```

> 🎯 **核心要点**：会话安全四件事——防固定（changeSessionId 默认开）、防劫持（Secure Cookie + HTTPS）、控并发（maximumSessions + SessionRegistry）、可登出（销毁彻底）。架构层面：Session 重"可控"（可踢人可撤销），JWT 重"无状态"（易扩展难撤销）——**没有银弹，按"登出即时性 vs 扩展性"的优先级选择**；微服务网关 + JWT + Redis 黑名单是 2026 年主流组合。

---

**上一模块**：[05-Web防护：CSRF、CORS与安全头](05-Web防护：CSRF、CORS与安全头.md)　**下一模块**：[07-OAuth2客户端：登录、授权码与PKCE](07-OAuth2客户端：登录、授权码与PKCE.md)
