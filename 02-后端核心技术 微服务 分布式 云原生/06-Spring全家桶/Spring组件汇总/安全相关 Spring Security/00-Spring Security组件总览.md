# 00 Spring Security 组件总览

> 组件卡片：Spring Security 是什么、版本现状、能做什么、与深度体系如何衔接——本系列是"组件汇总"速查型文档，深挖见 [Spring Security 与 OAuth2 知识体系](../../Spring Security与Spring Security OAuth2/00-Spring Security与OAuth2知识体系总览.md)

---

## 📚 目录

1. [组件一句话定位](#1-组件一句话定位)
2. [版本现状（2026-08）](#2-版本现状2026-08)
3. [能力地图](#3-能力地图)
4. [与深度体系的映射](#4-与深度体系的映射)
5. [快速上手 3 步](#5-快速上手-3-步)
6. [速查导航](#6-速查导航)

---

## 1. 组件一句话定位

**Spring Security 是 Spring 生态的认证与授权安全框架**——通过一条过滤器链（SecurityFilterChain）在请求进入业务之前完成认证（你是谁）、授权（你能干什么）与 Web 防护（CSRF/安全头等），并原生支持 OAuth2/OIDC 三件套（客户端、资源服务器、授权服务器）。

```text
核心心智模型：
  SecurityFilterChain（过滤器链）
    ├── 认证：AuthenticationManager → AuthenticationProvider → UserDetailsService
    ├── 授权：AuthorizationManager#authorize
    └── 防护：CsrfFilter / HeaderWriterFilter / ExceptionTranslationFilter
```

| 维度 | 说明 |
|------|------|
| 所属 | Spring 生态安全标准组件（Spring Framework 家族） |
| 版本线 | 7.x（2025-11 起，随 Spring Boot 4.0 同步） |
| 语言要求 | Java 17+（推荐 21） |
| 定位 | 应用安全：认证、授权、Web 防护、OAuth2/OIDC |

## 2. 版本现状（2026-08）

| 版本 | 发布时间 | 状态 | 支持截止 |
|------|:---:|:---:|:---:|
| **7.1.0** | 2026-06-09 | 最新 GA（含 opaque token introspection 修复） | OSS 至 2027-07-31 |
| 7.0.x（7.0.6） | 2025-11-17 起 | 广泛使用（Boot 4.0 配套） | OSS 至 2026-12-31 |
| 6.5.x（6.5.11） | 2025 | 维护线（Boot 3.5 配套） | OSS 至 2026-12-31 |
| 5.8.x | 2022-11 起 | 已 EOL | 2025-06 已终止 |

**7.x 关键版本事实：**

- 7.0.0 GA：2025-11-17（与 Spring Boot 4.0、Spring Framework 7.0 同步发布）；
- **授权服务器并入核心**（Spring Authorization Server 1.5.x 为最后独立代际，坐标 `spring-security-oauth2-authorization-server:7.0.0`）；
- 7.0 三大卖点：原生 MFA、模块化配置（Customizer Bean）、PKCE 默认；
- 2026-06 批次（7.0.6/7.1.0）：多项 CVE 修复（SAML 反序列化、XSS、开放重定向、X.509 冒用）。

> ⚠️ **选型提醒**：新项目直接 7.1.x；Boot 3 存量项目按"6.5 → 7.0"迁移路径走（清 deprecation → 升 Boot 4）。

## 3. 能力地图

| 能力域 | 能力点 | 对应注解/API |
|--------|--------|-------------|
| 认证 | 表单登录 / Basic / Remember-Me | `formLogin` / `httpBasic` / `rememberMe` |
| 认证 | 多因素认证（7.0 原生） | `@EnableMultiFactorAuthentication`、`FactorGrantedAuthority` |
| 认证 | 密码安全（Argon2 推荐） | `Argon2PasswordEncoder.defaultsForSpring7()` |
| 授权 | 请求级授权 | `authorizeHttpRequests` + `AuthorizationManager` |
| 授权 | 方法级授权 | `@EnableMethodSecurity` + `@PreAuthorize` |
| Web 防护 | CSRF / CORS / 安全头 | `csrf` / `cors` / `headers`（Boot 4 默认 API 开 CSRF） |
| OAuth2 客户端 | 第三方登录、代表调用 | `oauth2Login` / `OAuth2AuthorizedClient` |
| OAuth2 资源服务器 | JWT / 不透明令牌校验 | `oauth2ResourceServer` / `JwtDecoder` |
| OAuth2 授权服务器 | 自建发令牌服务（并入核心） | `OAuth2AuthorizationServerConfigurer` |

## 4. 与深度体系的映射

| 速查文档 | 深度体系模块 |
|---------|-------------|
| 01-模块清单 | [01-核心架构](../../Spring Security与Spring Security OAuth2/01-核心架构：SecurityFilterChain与认证授权流程.md) |
| 02-核心类与注解速查 | [02-认证机制与密码安全](../../Spring Security与Spring Security OAuth2/02-认证机制与密码安全.md)、[03-授权模型](../../Spring Security与Spring Security OAuth2/03-授权模型：AuthorizationManager与请求级授权.md) |
| 03-配置属性速查 | [01-与 Boot 自动配置协同](../../Spring Security与Spring Security OAuth2/01-核心架构：SecurityFilterChain与认证授权流程.md) |
| 04-集成地图与常见问题 | [05-Web防护](../../Spring Security与Spring Security OAuth2/05-Web防护：CSRF、CORS与安全头.md)、[10-生产实践与面试题](../../Spring Security与Spring Security OAuth2/10-生产实践与面试题.md) |

> 💡 本系列定位"查得快"，深度体系定位"学得透"——速查命中后，跳转深度体系看机制与实战。

## 5. 快速上手 3 步

```xml
<!-- ① 引入依赖（Boot 4 自动含授权服务器能力） -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

```java
// ② 基础配置（Lambda DSL，7.x 唯一合法写法）
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain chain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/public/**", "/login", "/error").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            .formLogin(Customizer.withDefaults())
            .logout(Customizer.withDefaults());
        return http.build();
    }

    // ③ 密码编码（Argon2 为 7.x 推荐默认）
    @Bean
    PasswordEncoder passwordEncoder() {
        return Argon2PasswordEncoder.defaultsForSpring7();
    }
}
```

> ⚠️ **上线前必查**：Boot 4 默认 CSRF 覆盖 API——无状态接口若返回 403，先看 [05-Web防护](../../Spring Security与Spring Security OAuth2/05-Web防护：CSRF、CORS与安全头.md) 再决定关不关。

## 6. 速查导航

| 文档 | 内容 |
|------|------|
| [01-模块清单](01-模块清单.md) | spring-security-* 各 artifact 职责 |
| [02-核心类与注解速查](02-核心类与注解速查.md) | 配置/认证/授权/OAuth2 关键类与注解 |
| [03-配置属性速查](03-配置属性速查.md) | spring.security.* 属性全表 |
| [04-集成地图与常见问题](04-集成地图与常见问题.md) | 与周边组件联动 + 高频坑速查 |

---

**下一模块**：[01-模块清单](01-模块清单.md)　**返回总览**：本页
