# 01 核心架构：SecurityFilterChain 与认证授权流程

> 一切 Spring Security 行为的起点是一条过滤器链：请求怎么进来、认证在哪一环发生、授权在哪一环判定、SecurityContext 如何跨层传播——本模块把 7.0 的核心架构与模块化配置一次讲透

---

## 📚 目录

1. [架构总览：三层职责](#1-架构总览三层职责)
2. [SecurityFilterChain：过滤器链全解析](#2-securityfilterchain过滤器链全解析)
3. [认证与授权：两个阶段的分界](#3-认证与授权两个阶段的分界)
4. [SecurityContext 与线程模型](#4-securitycontext-与线程模型)
5. [7.0 模块化配置：Customizer 化整为零](#5-70-模块化配置customizer-化整为零)
6. [与 Spring Boot 4 自动配置的协同](#6-与-spring-boot-4-自动配置的协同)
7. [多过滤器链与匹配规则](#7-多过滤器链与匹配规则)
8. [常见故障排查](#8-常见故障排查)

---

## 1. 架构总览：三层职责

```text
请求进入
  │
  ▼
┌─────────────────────────────────────────────────────┐
│ ① 过滤器链层：SecurityFilterChain（一组有序过滤器）      │
│    认证过滤器 → 授权过滤器 → 异常处理 → 放行到业务        │
└──────────────────────┬──────────────────────────────┘
                       │ 认证成功 → 写入
┌──────────────────────▼──────────────────────────────┐
│ ② 认证/授权模型层：Authentication / Authorization      │
│    AuthenticationManager（认证）                       │
│    AuthorizationManager#authorize（授权，7.0 唯一入口） │
└──────────────────────┬──────────────────────────────┘
                       │ 读取
┌──────────────────────▼──────────────────────────────┐
│ ③ 上下文层：SecurityContextHolder（线程绑定身份信息）    │
│    SecurityContext → Authentication → Principal       │
└─────────────────────────────────────────────────────┘
```

| 层 | 职责 | 7.0 关键变化 |
|----|------|-------------|
| 过滤器链 | 按顺序执行安全逻辑 | 模块化配置：`Customizer<HttpSecurity>` Bean |
| 认证/授权模型 | 验证身份、判定权限 | `AuthorizationManager#check` 删除，只剩 `authorize` |
| 上下文 | 跨过滤器共享身份 | 无变化（线程模型稳定） |

> 🎯 **核心要点**：Spring Security 不是"在业务代码里做安全检查"，而是**用过滤器在请求进入业务之前完成认证与授权**。理解这条链 = 理解一切"为什么这里 401""为什么这里放行"。

## 2. SecurityFilterChain：过滤器链全解析

### 2.1 最小配置与链的构建

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // 7.0 写法：Lambda DSL 是唯一合法写法（and() 已删除）
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/public/**", "/login", "/error").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login").defaultSuccessUrl("/home"))
            .logout(logout -> logout.logoutSuccessUrl("/login"));
        return http.build();
    }
}
```

### 2.2 核心过滤器（按默认顺序）

```text
Order  ┌────────────────────────────────────────────────────────┐
-100   │  DisableEncodeUrlFilter / WebAsyncManagerIntegration     │
 -7    │  SecurityContextHolderFilter      ① 读取/建立安全上下文    │
 -6    │  HeaderWriterFilter               ② 安全响应头            │
 -4    │  CsrfFilter                       ③ CSRF 校验（Boot4 默认开）│
 -3    │  LogoutFilter                     ④ 登出                  │
  0    │  UsernamePasswordAuthenticationFilter ⑤ 表单认证           │
  1    │  DefaultLoginPageGeneratingFilter  ⑥ 默认登录页           │
  3    │  BasicAuthenticationFilter       ⑦ HTTP Basic 认证        │
  6    │  RequestCacheAwareFilter          ⑧ 登录前请求缓存         │
 10    │  AnonymousAuthenticationFilter   ⑨ 匿名身份（未认证兜底）   │
 11    │  SessionManagementFilter         ⑩ 会话管理/并发控制       │
 12    │  ExceptionTranslationFilter      ⑪ 异常→401/403/重定向    │
 13    │  AuthorizationFilter             ⑫ 授权（最后一环）        │
       └────────────────────────────────────────────────────────┘
```

| 过滤器 | 职责 | 故障特征 |
|--------|------|---------|
| `SecurityContextHolderFilter` | 从 Session 恢复身份（无状态场景直接新建） | 身份丢失 |
| `CsrfFilter` | 校验 CSRF 令牌（Boot 4 默认覆盖 API） | **莫名 403** |
| `UsernamePasswordAuthenticationFilter` | 拦截 `/login` POST 做表单认证 | 401/重定向登录页 |
| `ExceptionTranslationFilter` | 捕获认证/授权异常并翻译 | 401 vs 403 的分界 |
| `AuthorizationFilter` | 最后判定：放行 or 403 | 403 |

> ⚠️ **7.0 注意**：认证过滤器（第 ⑤⑦ 环）负责"建立身份"，授权过滤器（第 ⑫ 环）负责"判定权限"——**认证异常 → 401（或重定向登录），授权异常 → 403**。排障第一步就是分清是认证失败还是授权失败。

## 3. 认证与授权：两个阶段的分界

```text
阶段一 认证（Authentication）：请求带凭据 → 验证 → 建立 Authentication
    ├── 表单：用户名/密码 → DaoAuthenticationProvider → UserDetailsService
    ├── Basic：Authorization: Basic base64(user:pass)
    ├── JWT：Bearer token → JwtAuthenticationProvider
    └── 结果：SecurityContextHolder 写入 Authentication（含权限集合）

阶段二 授权（Authorization）：凭 Authentication 的权限判定
    ├── 请求级：AuthorizationFilter → AuthorizationManager
    └── 方法级：@PreAuthorize 等 AOP 切面（见 04 篇）
```

**7.0 的授权抽象（唯一入口）：**

```java
public interface AuthorizationManager<T> {
    AuthorizationDecision check(Supplier<Authentication> authentication, T object); // 5.5 弃用
    AuthorizationDecision authorize(Supplier<Authentication> authentication, T object); // 7.0 唯一
}
```

- 5.5 起 `authorize()` 取代 `check()`，7.0 彻底删除 `check()`——**自定义授权器必须实现 `authorize()`**；
- 旧 `AccessDecisionManager`/`AccessDecisionVoter` 迁入可选模块 `spring-security-access`（仅旧代码需要）。

## 4. SecurityContext 与线程模型

### 4.1 存取模型

```java
// 读：当前线程的认证信息
Authentication auth = SecurityContextHolder.getContext().getAuthentication();
String name = auth.getName();
Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();

// 写（一般由框架完成，业务侧尽量别手写）
SecurityContextHolder.getContext().setAuthentication(auth);
```

| 策略 | 说明 | 适用 |
|------|------|------|
| `MODE_THREADLOCAL`（默认） | 每线程独立上下文 | 绝大多数场景 |
| `MODE_INHERITABLETHREADLOCAL` | 子线程继承父线程 | 需传递身份的线程池 |
| `MODE_GLOBAL` | 全 JVM 共享一份 | 单用户单线程应用 |

### 4.2 异步/线程池的上下文传播（高频坑）

```java
// ❌ 坑：新线程拿不到 SecurityContext（ThreadLocal 不跨线程）
new Thread(() -> {
    Authentication a = SecurityContextHolder.getContext().getAuthentication(); // null！
}).start();

// ✅ 方案一：手动传递
Authentication auth = SecurityContextHolder.getContext().getAuthentication();
new Thread(() -> {
    SecurityContextHolder.getContext().setAuthentication(auth);
    ...
}).start();

// ✅ 方案二：DelegatingSecurityContextExecutor 包装线程池
Executor executor = new DelegatingSecurityContextExecutor(Executors.newFixedThreadPool(4));
executor.execute(() -> { /* 上下文自动携带 */ });
```

> ⚠️ **注意**：`@Async`、响应式、虚拟线程场景各有专门的上下文传播机制（`@Async` 用 `SecurityContextHolder` 配合 `DelegatingSecurityContextAsyncTaskExecutor`）。**"异步就丢身份"是微服务里的高频 bug**，面试常考。

## 5. 7.0 模块化配置：Customizer 化整为零

**7.0 标志性新特性**：不再"一坨配置重写整个链"，而是提供**只改局部的 Customizer Bean**，Boot 默认的登录/CSRF/Session/OAuth2 配置原样保留：

```java
// 只补充"哪些路径放行"，其余默认配置不动
@Bean
Customizer<HttpSecurity> apiPublicPaths() {
    return http -> http.authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/public/**", "/actuator/health").permitAll());
}

// 针对 OAuth2 授权服务器的局部定制
@Bean
Customizer<OAuth2AuthorizationServerConfigurer> authServerCustomizer() {
    return configurer -> configurer
            .tokenEndpoint(token -> token.accessTokenResponseHandler(myHandler()));
}
```

| 特性 | 价值 | 与旧写法对比 |
|------|------|-------------|
| 模块化 Customizer Bean | 多模块各自贡献一段安全配置 | 旧版一个 Config 类垄断整链 |
| 继承 Boot 默认 | 改局部不动全局，升级兼容好 | 旧版默认配置易被误覆盖 |
| 按领域拆分 | 网关/管理台/OAuth2 各自独立 | 巨型 SecurityConfig 消亡 |

> 🎯 **面试点**：模块化配置是 7.0 的三大卖点之一（另两个是 MFA 与授权服务器并入核心）。答"如何多人协作配置安全规则"——给答案 `Customizer<HttpSecurity>` Bean。

## 6. 与 Spring Boot 4 自动配置的协同

Spring Boot 4 的 `SecurityAutoConfiguration` 行为：

| 自动配置 | 行为 | 与 7.0 联动 |
|---------|------|------------|
| 默认用户 | 无自定义 UserDetailsService 时生成随机密码用户 | `user` + 启动日志随机密码 |
| 默认 CSRF | **API 端点也开启 CSRF** | 升级后 403 的元凶 |
| 默认认证 | 表单登录 + Basic | 未配置即生效 |
| Jackson 3 | 安全模块序列化默认走 `tools.jackson` | Session/Remember-Me 令牌格式变化 |
| 条件装配 | 按类路径自动挂载 OAuth2 Client/Resource Server/Authorization Server | 依赖即开启 |

```yaml
# Boot 4 常用安全配置
spring:
  security:
    user:
      name: admin
      password: "{noop}123456"      # {noop} 明文标记，仅演示
    filter:
      order: -100                    # 过滤器链注册顺序
```

> ⚠️ **升级事故 Top1**：Boot 4 + Security 7 下 CSRF 默认覆盖 API，无状态 REST 接口全部 403——**要么显式 `csrf(csrf -> csrf.disable())`（无状态 API），要么携带 CSRF 令牌**（详见 05 篇）。

## 7. 多过滤器链与匹配规则

### 7.1 多链场景：不同路径不同安全策略

```java
@Bean
@Order(1)   // 第一个匹配的链生效
public SecurityFilterChain apiChain(HttpSecurity http) throws Exception {
    http
        .securityMatcher("/api/**")          // 本链只处理 /api/**
        .csrf(csrf -> csrf.disable())        // 无状态 API 关 CSRF
        .authorizeHttpRequests(auth -> auth.anyRequest().authenticated());
    return http.build();
}

@Bean
@Order(2)
public SecurityFilterChain uiChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
        .formLogin(Customizer.withDefaults());
    return http.build();
}
```

### 7.2 请求匹配：PathPatternRequestMatcher 时代

**7.0 起 `AntPathRequestMatcher`/`MvcRequestMatcher` 全部删除**，只剩 `PathPatternRequestMatcher`（基于 Spring `PathPatternParser`，更快更严格）：

| 写法 | 7.0 状态 | 示例 |
|------|:---:|------|
| `requestMatchers("/admin/**")` | ✅ | 尾部通配合法 |
| `requestMatchers("/api/**/admin")` | ❌ | **中间通配非法**（`**` 只能出现在开头或结尾） |
| `requestMatchers("/login")` | ✅ | 精确匹配 |
| `antMatchers(...)` | ❌ 已删除 | 迁移为 `requestMatchers(...)` |

> ⚠️ **静默差异**：PathPattern 更严格——尾斜杠不再匹配（`/api/users` ≠ `/api/users/`）、绝对路径需不含上下文路径。迁移时**先写授权测试再上线**，这些差异全是静默的。

## 8. 常见故障排查

| 现象 | 根因 | 处理 |
|------|------|------|
| 升级 Boot 4 后全部 403 | CSRF 默认覆盖 API | 无状态 API 关 CSRF 或带令牌（05 篇） |
| 接口 401 跳登录页 | 认证失败被重定向 | 无状态 API 配 `authenticationEntryPoint` 返回 401 JSON |
| 异步线程里身份为 null | ThreadLocal 不跨线程 | DelegatingSecurityContextExecutor |
| `/error` 页突然 401 | 7.0 dispatch 默认参与授权 | `dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()` |
| 中间通配报错 | PathPattern 更严格 | 改写为开头/结尾 `**` |
| 配置不生效 | 自定义 Bean 覆盖了默认链 | 检查 `@Order` 与链匹配优先级 |

> 🎯 **核心要点**：FilterChain 是理解 Spring Security 的"坐标系"——请求从左到右穿过过滤器，认证在中间环节建立身份（SecurityContext），授权在最后一环判定放行，异常翻译器决定 401 还是 403。7.0 的三大变化（模块化配置、authorize 唯一化、PathPattern 匹配）全部挂在这条链上。

---

**上一模块**：[00-Spring Security与OAuth2知识体系总览](00-Spring Security与OAuth2知识体系总览.md)　**下一模块**：[02-认证机制与密码安全](02-认证机制与密码安全.md)
