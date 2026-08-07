# 03 授权模型：AuthorizationManager 与请求级授权

> 认证解决"你是谁"，授权解决"你能干什么"。7.0 的授权世界只有两个关键词：`authorizeHttpRequests`（请求级）与 `AuthorizationManager#authorize`（统一抽象）——本模块讲透授权模型、请求匹配新范式与 7.0 的静默行为变化

---

## 📚 目录

1. [授权模型演进：从 AccessDecisionManager 到 AuthorizationManager](#1-授权模型演进从-accessdecisionmanager-到-authorizationmanager)
2. [请求级授权：authorizeHttpRequests 全解](#2-请求级授权authorizehttprequests-全解)
3. [AuthorizationManager 接口与自定义授权器](#3-authorizationmanager-接口与自定义授权器)
4. [请求匹配：PathPatternRequestMatcher 新范式](#4-请求匹配pathpatternrequestmatcher-新范式)
5. [7.0 行为变化：dispatch 授权与 CSRF 默认值](#5-70-行为变化dispatch-授权与-csrf-默认值)
6. [规则引擎与灰度放行](#6-规则引擎与灰度放行)

---

## 1. 授权模型演进：从 AccessDecisionManager 到 AuthorizationManager

```text
6.x 时代（遗留）                          7.0 时代（唯一）
┌─────────────────────┐              ┌─────────────────────┐
│ AccessDecisionManager│  ──已删除──▶ │ AuthorizationManager │
│ AccessDecisionVoter  │              │   .authorize(...)   │
│ FilterSecurityInterceptor│          │   （5.5 起唯一入口）  │
└─────────────────────┘              └─────────────────────┘
```

| 演进点 | 旧（6.x） | 新（7.0） |
|--------|----------|----------|
| 授权入口 | `AuthorizationManager#check` | `#authorize`（check 已删除） |
| 投票机制 | AccessDecisionVoter 投票 | 授权器直接返回 AuthorizationDecision |
| 遗留模块 | 框架内置 | 移入可选模块 `spring-security-access` |
| 方法安全开关 | `@EnableGlobalMethodSecurity` | `@EnableMethodSecurity`（见 04 篇） |
| 请求匹配 | antMatchers/MvcRequestMatcher | requestMatchers + PathPattern |

> 🎯 **本质**：AuthorizationManager 是一个**策略接口**——`authorize(authentication, 目标对象) → AuthorizationDecision`。认证信息 + 目标（请求/方法/对象）进，决策出。授权逻辑 = 若干个 AuthorizationManager 的组合与编排。

## 2. 请求级授权：authorizeHttpRequests 全解

### 2.1 规则体系

```java
http.authorizeHttpRequests(auth -> auth
        // ① 路径放行（公开资源）
        .requestMatchers("/public/**", "/login", "/register", "/error").permitAll()

        // ② 角色/权限控制
        .requestMatchers("/admin/**").hasRole("ADMIN")             // ROLE_ADMIN
        .requestMatchers("/api/**").hasAuthority("api:read")       // 自定义权限

        // ③ 组合
        .requestMatchers("/ops/**").hasAnyRole("ADMIN", "OPS")     // 任一角色
        .requestMatchers("/mng/**").access("hasRole('ADMIN') and hasAuthority('mng:write')")

        // ④ 兜底：其余全部要求认证
        .anyRequest().authenticated());
```

| 方法 | 语义 |
|------|------|
| `permitAll()` | 放行（无需认证） |
| `authenticated()` | 已认证即可 |
| `anonymous()` / `rememberMe()` | 限定匿名/记住登录身份 |
| `hasRole/hasAnyRole` | 角色（自动加 `ROLE_` 前缀） |
| `hasAuthority/hasAnyAuthority` | 权限（精确匹配） |
| `access(SpEL)` | 表达式授权（7.0 对复杂表达式要求改写成 AuthorizationManager） |

### 2.2 匹配优先级

**先声明先匹配、匹配即决定**（与链式 if-else 等价）：

```text
.requestMatchers("/admin/**").hasRole("ADMIN")   ← 先声明，优先
.requestMatchers("/**").authenticated()          ← 后声明，兜底
```

> ⚠️ **顺序坑**：把 `/admin/**` 写在 `/**` 之后，admin 规则永远不生效。规则顺序 = 精确到宽松。

## 3. AuthorizationManager 接口与自定义授权器

### 3.1 接口契约（7.0）

```java
public interface AuthorizationManager<T> {
    AuthorizationDecision authorize(Supplier<Authentication> authentication, T object);
    // 可选：默认实现为"无决策则拒绝"
    default AuthorizationDecision check(Supplier<Authentication> auth, T object) {
        return authorize(auth, object);   // 兼容桥接（7.0 中 check 已删除）
    }
}
```

### 3.2 自定义授权器实战：IP 白名单 + 时间窗

```java
@Component
public class IpAndTimeAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    private final List<String> allowedIps = List.of("10.0.0.0/8", "192.168.1.0/24");

    @Override
    public AuthorizationDecision authorize(Supplier<Authentication> authentication,
                                           RequestAuthorizationContext context) {
        // 1. 校验 IP
        String ip = context.getRequest().getRemoteAddr();
        boolean ipOk = allowedIps.stream().anyMatch(cidr -> ipMatches(ip, cidr));

        // 2. 校验时间窗（仅工作日 9-18 点）
        LocalTime now = LocalTime.now();
        boolean timeOk = now.isAfter(LocalTime.of(9, 0)) && now.isBefore(LocalTime.of(18, 0));

        // 3. 决策
        return new AuthorizationDecision(ipOk && timeOk);
    }
}

// 使用：注入到规则
@Bean
SecurityFilterChain chain(HttpSecurity http, IpAndTimeAuthorizationManager ipManager) throws Exception {
    http.authorizeHttpRequests(auth -> auth
            .requestMatchers("/internal/**").access(ipManager)   // 自定义授权器
            .anyRequest().authenticated());
    return http.build();
}
```

### 3.3 7.0 删除了什么

- `AuthorizationManager#check` → 删除（用 `authorize`）；
- 复杂 SpEL（`access("...")`）→ 建议改写为 AuthorizationManager 实现（更易测试、更清晰）；
- 旧 `AccessDecisionManager` 生态 → 仅 `spring-security-access` 模块提供（旧项目迁移用）。

## 4. 请求匹配：PathPatternRequestMatcher 新范式

7.0 起请求匹配只剩 `PathPatternRequestMatcher`（基于 Spring 6 的 `PathPatternParser`）：

| 差异 | Ant 风格（旧） | PathPattern（新） |
|------|:---:|:---:|
| 性能 | 慢（逐段正则） | 快（预编译树匹配） |
| `**` 位置 | 任意位置 | **只能开头或结尾，且单段** |
| 尾斜杠 | `/api/u/` 匹配 `/api/u` | **不匹配**（严格） |
| 路径变量 | `{var}` | `{var}` + `{*var}`（结尾捕获） |

```java
// ✅ 合法
.requestMatchers("/api/**")                    // 结尾通配
.requestMatchers("/admin/{module}/**")         // 变量 + 结尾通配
.requestMatchers("/**/report")                 // 开头通配

// ❌ 非法（启动即报错或静默不匹配）
.requestMatchers("/api/**/admin")              // 中间通配
```

> ⚠️ **迁移期提醒**：6.5 可用 `PathPatternRequestMatcherBuilderFactoryBean` 发布匹配器提前演练；升级 7.0 前**为每条规则写测试**——PathPattern 的差异全是静默的。

## 5. 7.0 行为变化：dispatch 授权与 CSRF 默认值

### 5.1 默认授权覆盖 dispatch

7.0 的 `authorizeHttpRequests` **默认对 forward/error dispatch 也执行授权**：

```text
现象：认证过期 → 请求转发到 /error → /error 未放行 → 返回 401 而非 500
修复：
    .requestMatchers("/error").permitAll()
    或
    .dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.FORWARD).permitAll()
```

### 5.2 CSRF 默认覆盖 API（Boot 4）

```text
现象：升级 Boot 4 后无状态 REST 接口全部 403
根因：Boot 4 + Security 7 默认 CSRF 覆盖所有端点
修复（无状态 API）：http.csrf(csrf -> csrf.disable())
     或（有状态 SPA）：采用 05 篇的 csrf.spa() / 令牌方案
```

### 5.3 行为变化速查

| 行为 | 6.x | 7.0 |
|------|:---:|:---:|
| dispatch 授权 | 默认不参与 | **默认参与** |
| API CSRF | 默认关闭（Boot 3） | **默认开启**（Boot 4） |
| 请求匹配 | Ant | PathPattern |
| 中间通配 | 允许 | 禁止 |
| 尾斜杠 | 容忍 | 严格 |

## 6. 规则引擎与灰度放行

### 6.1 灰度：按比例/按标签放行新接口

```java
// 灰度授权器：按用户 ID 哈希进灰度桶
@Component
public class GrayReleaseAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    private final int grayPercent = 10;   // 灰度比例

    @Override
    public AuthorizationDecision authorize(Supplier<Authentication> auth, RequestAuthorizationContext ctx) {
        if (auth.get() == null || !auth.get().isAuthenticated()) {
            return new AuthorizationDecision(false);
        }
        String userId = auth.get().getName();
        int bucket = Math.abs(userId.hashCode()) % 100;
        return new AuthorizationDecision(bucket < grayPercent);
    }
}
```

### 6.2 授权规则的可观测

- 拒绝日志：`AuthorizationDeniedException` 捕获后记录 `ip + user + path + 决策原因`；
- 命中率监控：对关键规则埋点（放行/拒绝计数），灰度上线时观察拒绝曲线；
- 审计：敏感操作的授权决策落审计日志（合规需求）。

> 🎯 **核心要点**：7.0 的授权 = `authorizeHttpRequests` 声明规则 + `AuthorizationManager` 承载策略 + PathPattern 严格匹配。三大纪律：①规则从精确到宽松排列；②复杂逻辑写 AuthorizationManager 而非 SpEL 长表达式；③升级 7.0 先跑授权测试（dispatch 默认授权 + PathPattern 差异 + CSRF 默认值是三大静默杀手）。

---

**上一模块**：[02-认证机制与密码安全](02-认证机制与密码安全.md)　**下一模块**：[04-方法级安全：@EnableMethodSecurity与表达式](04-方法级安全：@EnableMethodSecurity与表达式.md)
