# 07 OAuth2 客户端：登录、授权码与 PKCE

> 让用户"用 GitHub/微信/企业账号登录"、让服务"代表用户调用别的服务"——OAuth2 客户端模块（oauth2Login + OAuth2AuthorizedClient）是第三方登录与服务间认证的标准通道，7.0 起 PKCE 默认强制、WebClient/RestTemplate 一等支持

---

## 📚 目录

1. [OAuth2 全景：角色、令牌与授权模式](#1-oauth2-全景角色令牌与授权模式)
2. [oauth2Login：第三方登录接入](#2-oauth2login第三方登录接入)
3. [授权码 + PKCE：7.0 默认流程](#3-授权码--pkce70-默认流程)
4. [OAuth2AuthorizedClient：代表用户调用 API](#4-oauth2authorizedclient代表用户调用-api)
5. [client-credentials：机器间认证](#5-client-credentials机器间认证)
6. [令牌管理：刷新、存储与失效](#6-令牌管理刷新存储与失效)
7. [安全边界与常见坑](#7-安全边界与常见坑)

---

## 1. OAuth2 全景：角色、令牌与授权模式

**四大角色：**

```text
资源所有者（用户）→ 授权 → 客户端（我们的应用）→ 拿令牌 → 访问 → 资源服务器（API）
                                    ↑
                          授权服务器（发令牌）
```

**授权模式（grant type）演进（OAuth 2.1）：**

| 授权模式 | 语义 | 状态 |
|---------|------|:---:|
| 授权码（authorization_code） | 用户授权 → 换授权码 → 换令牌 | ✅ 主推 |
| PKCE（授权码 + 挑战值） | 防授权码劫持 | ✅ **7.0 默认** |
| client_credentials | 客户端自己的身份（无用户） | ✅ 机器间 |
| refresh_token | 刷新令牌换新令牌 | ✅ |
| password | 直接交密码（客户端代持） | ❌ **7.0 已移除** |
| implicit | 隐式流程（URL 直发令牌） | ❌ OAuth 2.1 废弃 |

> 🎯 **要点**：OAuth 2.1 的精神是"收紧"——密码模式被移除（客户端不该碰用户密码），隐式流程被废弃（不安全），PKCE 成为默认。7.0 完全跟随：**授权码 + PKCE 是唯一面向用户的流程**。

## 2. oauth2Login：第三方登录接入

### 2.1 依赖与配置

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-client</artifactId>
</dependency>
```

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          github:
            client-id: xxx
            client-secret: xxx
            scope: read:user,user:email
          # 企业/自建授权服务器同样适用（见 09 篇）
          company:
            provider: company-issuer
            client-id: app-001
            client-secret: secret
            authorization-grant-type: authorization_code
            scope: openid,profile,email
        provider:
          company-issuer:
            issuer-uri: https://auth.example.com   # 自动发现授权端点（OIDC 元数据）
```

### 2.2 流程与行为

```text
用户访问受保护页 → 未认证 → 重定向到授权服务器
  → 用户登录并授权 → 授权码回调 → 客户端用授权码换令牌
  → OAuth2LoginAuthenticationFilter 建立本地会话 → 登录成功
```

| 组件 | 职责 |
|------|------|
| `OAuth2LoginAuthenticationFilter` | 处理授权码回调 |
| `OAuth2UserService` | 加载第三方用户信息（自定义 profile 映射） |
| `DefaultOAuth2User` | 默认用户模型（含 attributes） |
| 多注册方 | 一个应用同时接 GitHub + 企业账号（每个 registration 一套配置） |

```java
// 自定义第三方用户映射：拿 GitHub profile 建本地用户
@Bean
OAuth2UserService<OAuth2UserRequest, OAuth2User> oauth2UserService(UserMapper userMapper) {
    return new DefaultOAuth2UserService() {
        @Override
        public OAuth2User loadUser(OAuth2UserRequest req) throws OAuth2AuthenticationException {
            OAuth2User user = super.loadUser(req);
            // 按 user.getAttribute("login") 建/查本地用户，返回带权限的 OAuth2User
            return user;
        }
    };
}
```

## 3. 授权码 + PKCE：7.0 默认流程

### 3.1 PKCE 防什么

```text
风险：授权码在回调 URL 传递，可能被恶意应用截获（授权码劫持）
PKCE：客户端先算 code_verifier（随机串）→ 发 code_challenge（SHA256 摘要）
      换令牌时带 code_verifier → 授权服务器校验与 code_challenge 匹配
      → 截获授权码的人没有 code_verifier，换不到令牌
```

### 3.2 7.0 行为

- **`requireProofKey` 默认 true**——所有授权码流程（包括保密客户端）默认要求 PKCE；
- 6.x 里"只有公开客户端（SPA/原生）用 PKCE"的区分在 7.0 消失；
- 配置上**无需任何额外设置**（自动生成 verifier/challenge）：

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          company:
            client-id: app-001
            # 无需配 PKCE —— 7.0 自动启用（授权服务器端也默认开启）
```

> ⚠️ **兼容注意**：若对接的**第三方授权服务器**不支持 PKCE（老实现），7.0 默认强制会导致握手失败——需显式 `client-authentication-method` 或评估升级对方。自建授权服务器（09 篇）默认支持。

## 4. OAuth2AuthorizedClient：代表用户调用 API

登录只是第一步，更常见的是**代表已登录用户调用第三方/内部 API**：

```java
@Controller
public class OrderController {

    @GetMapping("/api/orders")
    public String orders(OAuth2AuthorizedClient authorizedClient) {
        // 注入已授权客户端（含 access_token）→ 发 API 请求
        String token = authorizedClient.getAccessToken().getTokenValue();
        // 用 token 调用资源服务器（手动 WebClient 或 RestTemplate）
        return callApi(token);
    }
}
```

### 4.1 WebClient 一等支持（7.0 新增）

```java
@Bean
WebClient webClient(OAuth2AuthorizedClientManager manager) {
    ServletOAuth2AuthorizedClientExchangeFilterFunction function =
            new ServletOAuth2AuthorizedClientExchangeFilterFunction(manager);
    function.setDefaultClientRegistrationId("company");
    return WebClient.builder().apply(function.oauth2Configuration()).build();
}

// 使用：自动携带令牌、自动刷新（7.0 原生支持）
public String getUserProfile() {
    return webClient.get()
            .uri("https://api.example.com/profile")
            .attributes(ServletOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId("company"))
            .retrieve().bodyToMono(String.class).block();
}
```

| 能力 | 说明 |
|------|------|
| `clientRegistrationId` | 指定用哪个注册方 |
| 令牌自动注入 | 请求自动带 Bearer 头 |
| 自动刷新 | 令牌过期自动用 refresh_token 刷新（需授权服务器支持） |
| RestTemplate 支持 | 同样有 `RestTemplate` 构建器（7.0 补齐） |

### 4.2 OAuth2AuthorizedClientManager

```java
// 手动获取/刷新已授权客户端
OAuth2AuthorizedClient client = manager.authorize(
        new OAuth2AuthorizeRequest(clientRegistrationId, principal));
```

> 💡 登录会话与授权客户端的生命周期绑定：用户登出时授权客户端也随之清空（`OAuth2AuthorizedClientService` 管理）。

## 5. client-credentials：机器间认证

服务 A 调用服务 B（无用户参与）——`client_credentials` 模式：

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          internal-api:
            client-id: service-a
            client-secret: secret-a
            authorization-grant-type: client_credentials
            scope: order:read
            provider: company-issuer
```

```java
// 获取令牌并调用
OAuth2AuthorizedClient client = manager.authorize(OAuth2AuthorizeRequest
        .withClientRegistrationId("internal-api").principal("service-a").build());
String token = client.getAccessToken().getTokenValue();
```

| 要点 | 说明 |
|------|------|
| 无用户语义 | principal 是服务名而非用户 |
| 令牌较短 | access_token 通常几分钟（客户端凭据不含用户信息） |
| 安全性 | **client-secret 不得出现在前端代码**（仅服务端） |
| 替代 | mTLS（双向 TLS）是更强机器认证，OAuth2 client_credentials + mTLS 组合最佳 |

## 6. 令牌管理：刷新、存储与失效

### 6.1 令牌生命周期

```text
authorization_code 流程：
  授权码（5-10 分钟）→ access_token（分钟级）+ refresh_token（天/月级）
  access_token 过期 → refresh_token 换新（无需用户重新授权）
```

### 6.2 Spring 侧存储

| 存储 | 场景 |
|------|------|
| `InMemoryOAuth2AuthorizedClientService`（默认） | 单实例 |
| `JdbcOAuth2AuthorizedClientService` | 多实例共享 |
| 自定义 + Redis | 高可用（数据表结构对应 Authorization Server 的 token 存储） |

### 6.3 刷新策略

```java
// 手动刷新（默认在调用时按需刷新）
if (client.getAccessToken().isExpired()) {
    OAuth2AuthorizedClient refreshed = manager.authorize(...);
    // 使用新令牌
}
```

> ⚠️ **刷新令牌安全**：refresh_token 相当于长期免密凭证——**必须服务端存储、加密、不可出服务端**。WebClient 自动刷新方便但要注意存储实现选型（多实例用 JDBC/Redis 而非内存）。

## 7. 安全边界与常见坑

| 坑 | 现象 | 处理 |
|----|------|------|
| client-secret 泄漏 | 令牌被他人换领 | 前端绝不出现；密钥管理（KMS/Vault） |
| 授权码劫持（无 PKCE 老协议） | 令牌被截获 | 7.0 默认 PKCE；对方不支持则评估风险 |
| 回调 URL 不校验 | 授权码发送到任意站点 | 注册精确 `redirect-uri` 白名单 |
| 令牌存储在内存 | 多实例互相踢登录 | 换 JDBC/Redis 存储 |
| scope 过宽 | 越权获取数据 | 最小权限原则（只申请需要的 scope） |
| 三方用户与本地用户未绑定 | 同一人多个账号 | 邮箱/唯一标识匹配并合并 |
| 对接老授权服务器失败 | 握手失败 | 检查 PKCE 支持与 `client-authentication-method` |

> 🎯 **核心要点**：OAuth2 客户端 = 登录（oauth2Login）+ 代表调用（OAuth2AuthorizedClient）两层能力。7.0 的三个关键词：**PKCE 默认强制**（防授权码劫持）、**密码模式移除**（OAuth 2.1 精神）、**WebClient/RestTemplate 一等支持**（服务间调用开箱即用）。安全红线：secret 只进服务端、redirect-uri 精确注册、scope 最小化、refresh_token 严加保管。

---

**上一模块**：[06-Session与会话安全](06-Session与会话安全.md)　**下一模块**：[08-OAuth2资源服务器：JWT与不透明令牌](08-OAuth2资源服务器：JWT与不透明令牌.md)
