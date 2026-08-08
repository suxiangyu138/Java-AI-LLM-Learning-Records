# 05-Spring Security 7 源码导读
> Security 7 的源码地图：SecurityFilterChain 构建、认证与授权两条链、7.0 新结构（Lambda DSL/Customizer/MFA/Passkey/授权服务器）

## 📚 目录
1. [Security 7 架构总览](#1-security-7-架构总览)
2. [过滤器链的构建与执行](#2-过滤器链的构建与执行)
3. [认证流程源码链路](#3-认证流程源码链路)
4. [授权流程源码链路](#4-授权流程源码链路)
5. [7.0 新结构与新能力源码](#5-70-新结构与新能力源码)
6. [源码阅读断点路径](#6-源码阅读断点路径)
7. [核心要点](#7-核心要点)
8. [参考来源](#8-参考来源)

## 1. Security 7 架构总览

```text
请求进入
  │
  ▼
DelegatingFilterProxy（Servlet 容器 → Spring 安全过滤器链的桥）
  │
  ▼
FilterChainProxy（核心门面，持有所有 SecurityFilterChain）
  │  └─ 按请求匹配 SecurityFilterChain（首条匹配生效）
  │        └─ 过滤器链（默认约 15+ 个 Filter）
  │              ├─ CsrfFilter / UsernamePasswordAuthenticationFilter
  │              ├─ BasicAuthenticationFilter / BearerTokenAuthenticationFilter
  │              └─ AuthorizationFilter（授权，基于 AuthorizationManager）
  ▼
业务 Controller
```

| 组件 | 职责 |
|------|------|
| `DelegatingFilterProxy` | Servlet 容器与 Spring 的桥（按 Bean 名转发） |
| `FilterChainProxy` | 安全过滤器链门面（一个 Servlet Filter） |
| `SecurityFilterChain` | 匹配规则 + 过滤器列表（7.x 唯一配置模型） |
| `SecurityFilterChainFilter` | 链执行器（7.0 引入，替代旧 FilterChainProxy 内部逻辑） |

## 2. 过滤器链的构建与执行

### 2.1 构建：SecurityConfigurer → SecurityFilterChain

```text
HttpSecurity（DSL 构建器）
 ├─ .authorizeHttpRequests(auth -> auth...)  7.x Lambda DSL（.and() 已删除）
 ├─ .formLogin(...) / .oauth2Login(...) ...
 └─ build() → SecurityFilterChain
       └─ 由 HttpSecurityConfiguration 提供的默认链 + Customizer<HttpSecurity> 增量修改
```

| 7.x 变化 | 说明 |
|----------|------|
| `.and()` 链式完全删除 | 必须用 Lambda DSL 闭包（编译期强制） |
| `WebSecurityConfigurerAdapter` 已删除 | `SecurityFilterChain` Bean 为标准 |
| `Customizer<HttpSecurity>` Bean | 只增改部分配置，保留 Boot 默认（表单登录/CSRF/会话/OAuth2 授权服务器默认） |
| 过滤器顺序诊断 | 检测到链内过滤器乱序时**告警**（不再静默） |
| `PathPatternRequestMatcher` | 唯一匹配策略（`AntPathRequestMatcher`/`MvcRequestMatcher` 已移除；路径必须绝对，`/admin` 不再匹配 `/admin/`） |

```java
// Security 7 标准写法
@Bean
Customizer<HttpSecurity> securityCustomizer() {
    return http -> http
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/public/**").permitAll()
                    .requestMatchers("/admin/**").hasRole("ADMIN")
                    .anyRequest().authenticated())
            .formLogin(login -> login.permitAll());
}
```

### 2.2 执行：一次请求的过滤器链

```text
FilterChainProxy.doFilter
 → SecurityFilterChainFilter（7.0）
 → 依次执行链上 Filter（责任链模式）
 → 关键过滤器职责：
    CsrfFilter（CSRF 校验）
    UsernamePasswordAuthenticationFilter（表单认证：UsernamePasswordAuthenticationToken）
    BasicAuthenticationFilter（HTTP Basic）
    BearerTokenAuthenticationFilter（JWT/OAuth2 Bearer）
    AuthorizationFilter（授权决策）
```

## 3. 认证流程源码链路

### 3.1 认证体系类图

```text
AuthenticationManager（认证入口）
 └─ ProviderManager（默认实现）
       └─ List<AuthenticationProvider>（按支持类型分发）
             ├─ DaoAuthenticationProvider（用户名密码，委托 UserDetailsService）
             ├─ JwtAuthenticationProvider（JWT）
             └─ OAuth2 各 Provider
```

### 3.2 表单认证完整链路

```text
UsernamePasswordAuthenticationFilter
 ① 提取 username/password → 构造 UsernamePasswordAuthenticationToken（未认证）
 ② authenticationManager.authenticate(token) → ProviderManager
 ③ DaoAuthenticationProvider.authenticate
      ├─ UserDetailsService.loadUserByUsername（查用户）
      ├─ PasswordEncoder.matches（比对密码）
      └─ 成功 → 返回已认证 Authentication（含 authorities）
 ④ SecurityContextHolder.getContext().setAuthentication(auth)（存入上下文）
 ⑤ 成功处理/失败处理（AuthenticationSuccessHandler/FailureHandler）
```

| 环节 | 关键类 | 面试锚点 |
|------|--------|---------|
| 过滤器 | `UsernamePasswordAuthenticationFilter` | 认证入口 |
| 管理器 | `ProviderManager` | 多 Provider 分发 |
| 用户来源 | `UserDetailsService` | 自定义用户体系挂点 |
| 密码比对 | `PasswordEncoder`（7.x 推荐 Argon2） | 编码器选型 |
| 上下文 | `SecurityContextHolder`（ThreadLocal 策略） | 请求内共享 |

## 4. 授权流程源码链路

### 4.1 授权体系（7.x 唯一模型）

```text
AuthorizationManager（授权决策接口，7.x 唯一授权模型）
 ├─ RequestMatcherDelegatingAuthorizationManager（URL 授权）
 │     └─ 由 authorizeHttpRequests 配置构建
 ├─ AllRequiredFactorsAuthorizationManager（MFA 多因子要求）
 └─ 方法级：@EnableMethodSecurity + @PreAuthorize
       └─ AuthorizationManagerBeforeMethodInterceptor（AOP 拦截）
```

| 7.x 行为变化 | 说明 |
|--------------|------|
| `authorizeHttpRequests` 模型 | 基于 `AuthorizationManager` + `AuthorizationFilter`（旧 `FilterSecurityInterceptor` 删除） |
| 派发类型也授权 | forwards/error 等 dispatch 也参与授权——**未认证错误页可能返回 401**，需显式放行对应 dispatcher |
| 方法安全 | `@EnableMethodSecurity` 为唯一入口（`@EnableGlobalMethodSecurity` 已删除） |

### 4.2 方法授权链路

```text
@EnableMethodSecurity
 → AuthorizationManagerBeforeMethodInterceptor（前置校验）
 → @PreAuthorize("hasRole('ADMIN')") → AuthorizationManager 求值
 → 不通过抛 AccessDeniedException → ExceptionTranslationFilter 转 403
```

## 5. 7.0 新结构与新能力源码

| 新能力 | 关键 API | 源码入口 |
|--------|---------|---------|
| MFA（多因子认证） | `@EnableMultiFactorAuthentication`、`FactorGrantedAuthority`、`AllRequiredFactorsAuthorizationManager` | 核心模块（曾要求 12 年的功能） |
| Passkey（无密码） | WebAuthn 支持 | 核心模块，约 4 行配置接入 |
| 授权服务器内建 | 核心包含 OAuth2 Authorization Server（不再依赖独立 artifact） | `spring-boot-starter-security` 即含 |
| PKCE 默认开启 | OAuth2 授权码流程默认要求 PKCE | 授权服务器核心 |
| Argon2 推荐 | `Argon2PasswordEncoder.defaultsForSpring7()` | PasswordEncoder 工厂 |
| 移除 OAuth2 Password Grant | 迁移授权码流程（OAuth 2.1） | 兼容层删除 |

> ⚠️ **7.x 迁移雷区**：① `.and()` 编译错误 → 改 Lambda DSL；② `antMatchers` 编译错误 → `requestMatchers`；③ 路径匹配语义变化（无尾斜杠容忍）；④ 授权模型变化导致 error dispatch 401——全部是**静默行为变化**，升级必须全量回归。

## 6. 源码阅读断点路径

| 场景 | 断点位置 | 观察点 |
|------|---------|--------|
| 认证流程 | `ProviderManager.authenticate` | provider 分发 |
| 密码比对 | `DaoAuthenticationProvider.authenticate` | UserDetails/PasswordEncoder |
| 授权决策 | `AuthorizationFilter` | manager 选择 |
| 过滤器顺序 | `SecurityFilterChainFilter` | 链内过滤器列表 |
| 自定义过滤器的位置 | `HttpSecurity.addFilterBefore/After` | 挂点 |

## 7. 核心要点

> 🎯 **核心要点**：
> - 一条主链：DelegatingFilterProxy → FilterChainProxy → SecurityFilterChain → 责任链执行；
> - 两条子链：认证（AuthenticationManager → Provider → 上下文）+ 授权（AuthorizationManager → 决策）；
> - 7.x 三大结构变化：Lambda DSL 强制、`Customizer<HttpSecurity>` 模块化、`PathPatternRequestMatcher` 唯一；
> - 7.x 新能力：MFA/Passkey/授权服务器内建/PKCE 默认——源码看核心模块即可；
> - 升级回归重点：静默行为变化（路径匹配、error dispatch 授权）。

## 8. 参考来源

- [Spring Security 5→6→7 迁移指南（含静默行为变化）](https://ankurm.com/spring-security-5-to-6-to-7-migration-guide/)
- [Spring Security 7：MFA、模块化配置与破坏性变更（dev.to）](https://dev.to/jamilxt/spring-security-7-mfa-modular-config-and-what-breaks-l1b)
- [Spring Boot 4 与 Spring Security 7（Zademy）](https://zademy.com/en/note/futuro-seguridad-spring-boot-spring-security/)
- [HttpSecurity customizer in Spring Boot 4](https://dimitri.codes/httpsecurity-customizer/)
- [Spring Security Reference](https://docs.spring.io/spring-security/reference/)

---

**下一模块**：[06-数据访问源码导读](06-数据访问源码导读.md)　/　**返回总览**：[00-总览](00-高级进阶与源码学习总览.md)
