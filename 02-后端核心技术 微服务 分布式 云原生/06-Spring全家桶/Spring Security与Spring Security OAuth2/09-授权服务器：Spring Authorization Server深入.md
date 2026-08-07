# 09 授权服务器：Spring Authorization Server 深入

> 授权服务器是 OAuth2 的"发令牌权威"：独立发展 3 年后于 2025-11 并入 Spring Security 7.0 核心，配置模型从 @EnableAuthorizationServer 进化为 OAuth2AuthorizationServerConfigurer——本模块讲透自建授权服务器的完整链路

---

## 📚 目录

1. [并入核心：Spring Authorization Server 的 7.0 迁移](#1-并入核心spring-authorization-server-的-70-迁移)
2. [架构与配置体系：OAuth2AuthorizationServerConfigurer](#2-架构与配置体系oauth2authorizationserverconfigurer)
3. [客户端注册：RegisteredClient 与存储](#3-客户端注册registeredclient-与存储)
4. [授权码流程全链路解析](#4-授权码流程全链路解析)
5. [令牌管理：生成、存储与增强](#5-令牌管理生成存储与增强)
6. [同意授权：consent 机制](#6-同意授权consent-机制)
7. [生产化：安全、高可用与监控](#7-生产化安全高可用与监控)

---

## 1. 并入核心：Spring Authorization Server 的 7.0 迁移

### 1.1 时间线与现状（2026）

| 时间 | 事件 |
|------|------|
| 2020-11 | 从 Spring Security OAuth2 项目分裂为独立项目（社区接力） |
| 2022-11 | Spring Authorization Server 1.0 独立发布 |
| 2025-09-11 | 官方宣布**并入 Spring Security 7.0**（1.5.x 为最后独立代际） |
| 2025-11-17 | Spring Security 7.0 GA：授权服务器成为核心模块 |

### 1.2 迁移影响（官方承诺"最小化"）

| 迁移项 | 变化 |
|--------|------|
| Maven 坐标 | 不变：`org.springframework.security:spring-security-oauth2-authorization-server`，版本改 7.0.0 |
| 类名/包名 | 基本不变（少量包调整） |
| 配置方式 | `@EnableAuthorizationServer` → `OAuth2AuthorizationServerConfigurer` |
| 客户端存储 | `ClientDetailsService` → `RegisteredClientRepository` |
| 新增能力 | 随 Spring Security 7.0 同步获得：PKCE 默认、Dynamic Registration 默认、MFA、Password4j |

> 🎯 **面试点**：授权服务器并入核心意味着"自建 OAuth2 授权服务器"成为 Spring 生态官方一等能力——`spring-boot-starter-security` 即含全套授权服务器能力，独立依赖时代结束。

## 2. 架构与配置体系：OAuth2AuthorizationServerConfigurer

### 2.1 最小可运行授权服务器

```java
@Configuration
@EnableWebSecurity
public class AuthorizationServerConfig {

    @Bean
    @Order(1)   // 授权服务器端点优先级高于普通安全链
    public SecurityFilterChain authorizationServerChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfigurer configurer =
                new OAuth2AuthorizationServerConfigurer();
        http
            .securityMatcher(configurer.getEndpointsMatcher())  // 只接管授权端点
            .with(configurer, c -> c
                .authorizationEndpoint(authz -> authz.consentPage("/oauth2/consent"))
                .oidc(oidc -> oidc.clientRegistrationEndpoint(Customizer.withDefaults())))
            .authorizeHttpRequests(auth -> auth
                .anyRequest().authenticated());
        return http.build();
    }

    @Bean
    @Order(2)   // 其余请求走普通链
    public SecurityFilterChain defaultChain(HttpSecurity http) throws Exception {
        http.formLogin(Customizer.withDefaults()).authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
```

### 2.2 核心端点（内置）

| 端点 | 协议 | 职责 |
|------|:---:|------|
| `/oauth2/authorize` | 授权码流程 | 用户授权入口 |
| `/oauth2/token` | 令牌端点 | 换令牌（授权码/刷新/客户端凭据） |
| `/oauth2/introspect` | 内省 | 资源服务器校验不透明令牌 |
| `/oauth2/revoke` | 吊销 | 撤销令牌/刷新令牌 |
| `/oauth2/jwks` | JWK | 公钥分发（JWT 签名验证） |
| `/oauth2/register` | **动态注册** | 客户端自助注册（7.0 默认开启） |
| `/.well-known/openid-configuration` | OIDC | 发现元数据 |

### 2.3 模块化定制（7.0 新能力）

```java
// 局部定制：不重写整链，只改令牌端点行为
@Bean
Customizer<OAuth2AuthorizationServerConfigurer> authServerCustomizer() {
    return configurer -> configurer
            .tokenEndpoint(token -> token
                .accessTokenResponseHandler(customSuccessHandler())
                .errorResponseHandler(customErrorHandler()))
            .clientAuthentication(client -> client
                .authenticationProvider(customClientAuthProvider()));
}
```

## 3. 客户端注册：RegisteredClient 与存储

### 3.1 客户端模型

```java
@Bean
RegisteredClientRepository registeredClientRepository(JdbcTemplate jdbcTemplate) {
    return new JdbcRegisteredClientRepository(jdbcTemplate);
}

// 注册一个客户端（初始化脚本）
RegisteredClient webApp = RegisteredClient.withId(UUID.randomUUID().toString())
        .clientId("web-app")
        .clientSecret("{noop}secret")              // 生产用 PasswordEncoder 编码
        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
        .redirectUri("https://app.example.com/login/oauth2/code/web-app")
        .scope("order:read")
        .scope("order:write")
        .requireProofKey(true)                      // PKCE 强制（7.0 默认语义）
        .tokenSettings(TokenSettings.builder()
                .accessTokenTimeToLive(Duration.ofMinutes(30))
                .refreshTokenTimeToLive(Duration.ofDays(7))
                .build())
        .build();
```

### 3.2 存储选型

| 仓库 | 场景 |
|------|------|
| `InMemoryRegisteredClientRepository` | 演示/测试 |
| `JdbcRegisteredClientRepository` | 生产默认（标准表结构 `oauth2_registered_client`） |
| 自定义（Redis 等） | 高可用/特殊需求 |

> ⚠️ **7.0 迁移注意**：旧 `ClientDetailsService`（Spring Security OAuth2 时代）已废弃——统一用 `RegisteredClientRepository` 接口。

## 4. 授权码流程全链路解析

```text
① 客户端重定向用户到 /oauth2/authorize?client_id=web-app&redirect_uri=...&code_challenge=...
② 授权服务器校验 client_id/redirect_uri/scope → 用户未登录则先表单登录
③ 用户点击"同意"（consent 页）→ 生成一次性授权码（短有效期）
④ 回调客户端 redirect_uri?code=xxxx
⑤ 客户端 POST /oauth2/token（code + code_verifier + client 凭据）
⑥ 校验授权码（一次性、已绑定客户端/redirect_uri）→ 校验 PKCE（code_challenge == SHA256(verifier)）
⑦ 颁发 access_token + refresh_token → 清除授权码（防重放）
⑧ 资源服务器验签放行（08 篇链路）
```

**源码级关键对象：**

| 对象 | 职责 |
|------|------|
| `OAuth2AuthorizationCodeRequestAuthenticationProvider` | 处理授权请求、生成授权码 |
| `OAuth2AuthorizationCodeAuthenticationProvider` | 处理令牌请求、兑换授权码 |
| `OAuth2Authorization` | 授权记录的持久化模型（含授权码/令牌/元数据） |
| `OAuth2AuthorizationService` | 授权记录存取（Jdbc 实现为标准） |
| `OAuth2TokenGenerator` | 令牌生成（JWT/不透明） |

> 🎯 **要点**：授权码是一次性短效凭证，令牌换取时必须匹配**客户端身份 + redirect_uri + PKCE 挑战值**——任何一个环节不匹配即拒绝。授权码清除防重放、PKCE 防劫持，两层防护是 OAuth 2.1 的安全基石。

## 5. 令牌管理：生成、存储与增强

### 5.1 令牌类型配置

```yaml
# 方式一：JWT（默认，自签 RSA 密钥自动生成）
# 方式二：OAuth2AccessToken（不透明，需 introspection 端点）
```

```java
// JWT 自定义声明（业务字段进令牌）
@Bean
OAuth2TokenCustomizer<JwtEncodingContext> jwtCustomizer() {
    return context -> {
        if (context.getTokenType().equals(OAuth2TokenType.ACCESS_TOKEN)) {
            context.getClaims().claims(claims -> {
                claims.put("tenant", "company-a");
                claims.put("user_roles", loadRoles(context.getPrincipal().getName()));
            });
        }
    };
}
```

### 5.2 令牌存储与吊销

| 存储 | 说明 |
|------|------|
| JWT（无状态） | 本地验签，吊销靠黑名单（jti）或缩短 TTL |
| `OAuth2AuthorizationService`（Jdbc） | 授权码/刷新令牌/令牌元数据持久化（生产必须） |
| 刷新令牌 | 必须服务端存储（Jdbc），吊销刷新令牌 = 撤销整个会话 |

### 5.3 令牌增强链路

```text
OAuth2TokenGenerator（默认组合）：
  JwtGenerator（JWT 场景）→ JwtCustomizer 加 claim → 签名 → 返回
  组合模式：DelegatingOAuth2TokenGenerator 依次尝试各生成器
```

## 6. 同意授权：consent 机制

用户第一次用第三方应用登录时，需要显式"同意授权"（scope 清单）：

```java
// 同意页端点（自建页面）
@Controller
public class ConsentController {

    @GetMapping("/oauth2/consent")
    public String consent(OAuth2AuthorizationConsentAuthenticationToken auth, Model model) {
        // 展示客户端申请的 scope 列表
        Set<String> scopes = auth.getAuthorizationRequest().getScopes();
        model.addAttribute("scopes", scopes);
        return "consent";
    }
}
```

| 环节 | 机制 |
|------|------|
| 触发 | 首次授权（无 consent 记录时） |
| 存储 | `OAuth2AuthorizationConsentService`（Jdbc 表 `oauth2_authorization_consent`） |
| 二次授权 | 已有同意记录则跳过同意页（直发授权码） |
| scope 变更 | scope 变化需重新同意 |

> 💡 **生产要点**：同意页是授权服务器的"用户体验门面"——明确列出申请的 scope 与用途，降低用户顾虑；企业内网可配置"预授权"跳过同意页（`AuthorizationRequestValidator` 定制）。

## 7. 生产化：安全、高可用与监控

### 7.1 安全基线

| 项目 | 要求 |
|------|------|
| 传输 | 授权服务器**必须 HTTPS**（令牌/授权码在途） |
| 密钥 | JWT 签名密钥托管（Vault/KMS），轮换机制 |
| 客户端凭据 | 哈希存储（PasswordEncoder），Basic/POST 二选一 |
| 授权码 | 短 TTL + 一次性 + 绑定 redirect_uri |
| 刷新令牌 | 加密存储 + 吊销接口 + 轮换策略 |
| 审计 | 授权/令牌事件全量审计日志（合规） |

### 7.2 高可用

- 授权码/consent/令牌元数据走 `Jdbc` 存储（多实例共享）；
- 多实例无状态化：负载均衡 + 共享 DB；
- JWT 签发密钥集群共享（RSA 密钥对入库或 KMS 统一管理）。

### 7.3 监控

```java
// 关键指标（Micrometer 自动暴露）
authorization_server.token.issued     // 令牌签发数
authorization_server.token.error     // 令牌签发失败
authorization_server.authorization.* // 授权事件
```

> 🎯 **核心要点**：授权服务器 = 客户端注册（RegisteredClient）+ 授权码流程（Provider 链）+ 令牌生成（JWT/不透明）+ 同意授权（Consent）+ 持久化（Jdbc）。7.0 并入核心后配置模型统一为 `OAuth2AuthorizationServerConfigurer`，自建授权服务器的门槛与文档成本大幅下降。生产红线：HTTPS、密钥托管、Jdbc 存储、审计日志四件套缺一不可。

---

**上一模块**：[08-OAuth2资源服务器：JWT与不透明令牌](08-OAuth2资源服务器：JWT与不透明令牌.md)　**下一模块**：[10-生产实践与面试题](10-生产实践与面试题.md)
