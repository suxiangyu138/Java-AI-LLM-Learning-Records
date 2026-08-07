# Spring Security 与 OAuth2 知识体系总览

> 从 SecurityFilterChain 认证授权链路、密码安全、CSRF 防护，到 OAuth2 客户端/资源服务器/授权服务器三件套——Spring Security 7.0（2025-11-17 GA，与 Spring Boot 4.0 同步）完成"授权服务器并入核心 + MFA 原生支持 + 模块化配置"三大代际升级，2026 年最新 7.0.6 / 7.1.0

---

## 📚 目录

1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [为什么必须系统学透 Spring Security](#3-为什么必须系统学透-spring-security)
4. [核心概念速查](#4-核心概念速查)
5. [与周边知识的关系](#5-与周边知识的关系)
6. [学习路线推荐](#6-学习路线推荐)
7. [快速自测 10 题](#7-快速自测-10-题)

---

## 1. 知识体系导图

```text
Spring Security 与 OAuth2 知识体系
│
├── 01 核心架构：SecurityFilterChain 与认证授权流程
│   ├── FilterChain 请求处理全链路与核心过滤器
│   ├── 认证（Authentication）与授权（Authorization）分离
│   ├── SecurityContext 与 SecurityContextHolder 线程模型
│   ├── 7.0 模块化配置：Customizer<HttpSecurity> Bean
│   └── 与 Spring Boot 4 自动配置的协同
│
├── 02 认证机制与密码安全
│   ├── 表单登录 / HTTP Basic / Remember-Me
│   ├── PasswordEncoder 演进：MD5 → BCrypt → Argon2（Password4j）
│   ├── 7.0 原生 MFA：@EnableMultiFactorAuthentication
│   ├── UserDetailsService 与自定义认证
│   └── 常见认证漏洞与防护
│
├── 03 授权模型：AuthorizationManager 与请求级授权
│   ├── authorizeHttpRequests 请求级授权
│   ├── AuthorizationManager 统一授权抽象
│   ├── PathPatternRequestMatcher 请求匹配新范式
│   ├── 7.0 行为变化：dispatch 默认授权、CSRF 默认开启
│   └── 自定义授权器与灰度放行
│
├── 04 方法级安全
│   ├── @EnableMethodSecurity 与 @PreAuthorize/@PostAuthorize
│   ├── SpEL 安全表达式与自定义表达式
│   ├── @Secured / @RolesAllowed 兼容
│   └── 事务与异步环境下的安全上下文传播
│
├── 05 Web 防护：CSRF、CORS 与安全头
│   ├── CSRF 原理与 Boot 4 默认开启的 403 事故
│   ├── 7.0 SPA 友好的 csrf.spa() DSL
│   ├── CORS 跨域配置与安全头（HSTS/X-Content-Type-Options）
│   └── 点击劫持 / XSS 防护
│
├── 06 Session 与会话安全
│   ├── 会话固定攻击与防御
│   ├── 并发会话控制与 SessionRegistry
│   ├── Remember-Me 令牌机制
│   └── 无状态 JWT 与 Session 的取舍
│
├── 07 OAuth2 客户端：登录、授权码与 PKCE
│   ├── oauth2Login 社交/企业登录
│   ├── 授权码 + PKCE（7.0 默认强制）
│   ├── OAuth2AuthorizedClient 与令牌管理
│   ├── WebClient / RestTemplate 服务间调用
│   └── client-credentials 机器间认证
│
├── 08 OAuth2 资源服务器：JWT 与不透明令牌
│   ├── JwtDecoder 与 jwk-set-uri 校验链
│   ├── 7.0 JWT typ 头默认校验
│   ├── 多租户 / 多签发方配置
│   ├── BearerTokenAuthenticationConverter
│   └── 不透明令牌与 TokenIntrospection
│
├── 09 授权服务器：Spring Authorization Server 深入
│   ├── 并入 Spring Security 7.0 核心（2025-11）
│   ├── OAuth2AuthorizationServerConfigurer 配置体系
│   ├── 授权码流程全链路源码级解析
│   ├── RegisteredClientRepository 与令牌存储
│   ├── Dynamic Registration（7.0 默认开启）
│   └── 客户端凭据 / 刷新令牌 / 令牌增强
│
└── 10 生产实践与面试题
    ├── 网关统一认证与微服务安全架构
    ├── 测试：SecurityMockMvc 与测试切片
    ├── 从 5.x/6.x 迁移到 7.0 的清单
    ├── 常见漏洞自查清单
    └── 面试高频 15 问
```

## 2. 模块导航

| 序号 | 模块 | 核心内容 | 适合人群 |
|:---:|------|----------|---------|
| 01 | 核心架构：SecurityFilterChain 与认证授权流程 | 过滤器链全链路、SecurityContext、模块化配置 | 入门必读 |
| 02 | 认证机制与密码安全 | 表单/Basic/Remember-Me、密码编码演进、原生 MFA | 入门必读 |
| 03 | 授权模型：AuthorizationManager | authorizeHttpRequests、请求匹配、7.0 行为变化 | 入门必读 |
| 04 | 方法级安全 | @PreAuthorize 家族、SpEL、上下文传播 | 高频核心 |
| 05 | Web 防护：CSRF、CORS 与安全头 | CSRF 默认开启的 403、SPA 方案、安全头 | 高频核心 |
| 06 | Session 与会话安全 | 会话固定、并发控制、Remember-Me、无状态取舍 | 进阶 |
| 07 | OAuth2 客户端 | 登录、授权码 + PKCE、令牌管理、服务间调用 | 进阶 |
| 08 | OAuth2 资源服务器 | JWT 校验、typ 校验、多租户、不透明令牌 | 进阶 |
| 09 | 授权服务器深入 | 并入核心、配置器、授权码全流程、令牌管理 | 高级 |
| 10 | 生产实践与面试题 | 网关安全架构、测试、迁移清单、面试题 | 冲刺 |

## 3. 为什么必须系统学透 Spring Security

**1）它是 Java 生态安全的事实标准。** 从单体应用的登录鉴权，到微服务网关统一认证、OAuth2 三件套，Spring Security 是绕不开的防线。面试"认证授权怎么做"的答案是它，生产事故"403/401 排查"的根因也在它。

**2）7.0 是一次影响深远的代际升级。** 与 Spring Boot 4.0 同步发布的 Spring Security 7.0（2025-11-17 GA）：授权服务器 12 年独立发展后**并入核心**、MFA 原生支持（被请求了 12 年的特性）、模块化配置、PKCE 默认强制、CSRF 默认覆盖 API——**6.x 升 7.0 不只是升级依赖，而是行为默认值全面收紧**，2026 年面试必问"升级后为什么全是 403"。

**3）安全是漏洞的高发区，也是面试的差异化地带。** CSRF、会话固定、开放重定向、JWT typ 校验——每一个都是 CVE 级别的知识点。系统学过的人能答"为什么",只背过 demo 的人只能说"怎么配"。

**4）OAuth2 是企业级认证的最终答案。** 单点登录、第三方登录、服务间认证全部落在 OAuth2/OIDC 上。Authorization Server 并入核心后，"自己搭授权服务器"的门槛大幅降低，成为 2026 年 Spring 生态的标志性能力。

> 🎯 **核心要点**：Spring Security 的底层逻辑是一条**过滤器链**：认证过滤器负责"你是谁"（Authentication），授权器负责"你能干什么"（Authorization），两者通过 SecurityContext 串联。OAuth2 是这套机制在企业级认证场景的协议化延伸。学它 = 学"安全如何成为框架能力而非业务代码"。

## 4. 核心概念速查

| 概念 | 一句话本质 | 关键类/注解 |
|------|-----------|------------|
| FilterChain | 请求经过的安全过滤器链 | `SecurityFilterChain` Bean |
| 认证 Authentication | 验证"你是谁"并建立身份 | `AuthenticationManager`/`Authentication` |
| 授权 Authorization | 判定"你能干什么" | `AuthorizationManager#authorize` |
| 安全上下文 | 当前线程的身份信息容器 | `SecurityContextHolder` |
| 密码编码 | 不可逆存储与校验 | `PasswordEncoder`（BCrypt/Argon2） |
| 方法安全 | 方法级细粒度授权 | `@PreAuthorize`/`@EnableMethodSecurity` |
| CSRF | 跨站请求伪造防护 | `CsrfFilter`（Boot 4 默认开启） |
| OAuth2 客户端 | 代表用户向授权服务器要令牌 | `oauth2Login`/`OAuth2AuthorizedClient` |
| 资源服务器 | 校验令牌并放行受保护资源 | `oauth2ResourceServer`/`JwtDecoder` |
| 授权服务器 | 发令牌的权威机构 | `OAuth2AuthorizationServerConfigurer` |
| PKCE | 无密客户端防授权码劫持 | 7.0 默认 `requireProofKey=true` |
| MFA | 多因素认证（7.0 原生） | `@EnableMultiFactorAuthentication` |

## 5. 与周边知识的关系

```text
                    ┌─────────────────────────────┐
                    │  Spring 全家桶（06-Spring全家桶） │
                    │  Boot 自动配置 / 事件 / AOP      │
                    └──────────────┬──────────────┘
                                   │ 提供基础设施（过滤器注册、属性绑定、代理）
                    ┌──────────────▼──────────────┐
                    │   Spring Security（本体系）    │
                    │  认证 → 授权 → 防护 → OAuth2   │
                    └──────┬───────────────┬───────┘
                           │               │
          ┌────────────────▼───┐   ┌───────▼────────────────┐
          │ 微服务网关安全架构    │   │ 分布式缓存/Session 会话   │
          │ （Gateway + Security）│   │ （Redis 会话共享等）      │
          └────────────────────┘   └────────────────────────┘
```

- **向上承接**：[Spring全家桶-07-Spring-Security认证与授权](../Spring全家桶/07-Spring-Security认证与授权.md) 有基础篇；本体系做 7.0 代际深度化与 OAuth2 全链路。
- **横向联动**：[SpringBoot-06-安全与认证](../SpringBoot/06-SpringBoot安全与认证.md) 讲 Boot 视角的自动配置；本体系讲 Security 内部机制。分布式锁/会话场景联动 [Spring Data Redis](../Spring Data Redis/00-Spring Data Redis知识体系总览.md)。
- **向下延伸**：JWT（jjwt/nimbus）、API 网关鉴权、OIDC 协议细节，均以本体系的 OAuth2 三件套为地基。

## 6. 学习路线推荐

**路线 A（初级 · 快速上手 3 天）**
01 核心架构 → 02 认证机制 → 03 授权模型 → 05 CSRF 防护。目标：能独立配置登录鉴权、理解 403/401、写对密码编码。

**路线 B（中级 · 生产工程师 1 周）**
路线 A + 04 方法级安全 → 06 Session → 07 OAuth2 客户端 → 08 资源服务器。目标：能实现方法级细粒度授权、OAuth2 登录与 JWT 资源保护。

**路线 C（高级 · 架构与面试冲刺）**
全套 01-10，重点 09（授权服务器）、10（网关架构 + 迁移 + 面试题）。目标：能自建授权服务器、设计微服务安全架构、回答 7.0 新特性与迁移面试题。

## 7. 快速自测 10 题

1. SecurityFilterChain 的执行顺序是什么？认证与授权在哪一环分界？
2. Boot 4 升级后接口突然全部 403，最可能的原因是什么？怎么解决？
3. BCrypt、Argon2、SCrypt 有什么区别？7.0 推荐默认是哪个？
4. `@EnableGlobalMethodSecurity` 为什么没了？@EnableMethodSecurity 默认开哪些注解？
5. MFA 在 Spring Security 7.0 里怎么开启？FactorGrantedAuthority 记录什么？
6. CSRF 攻击的原理是什么？`csrf.spa()` 解决了 SPA 的什么问题？
7. 授权码 + PKCE 流程里，PKCE 防的是什么攻击？
8. JWT 的 `typ` 头校验是 7.0 的新默认行为吗？不校验有什么风险？
9. Spring Authorization Server 现在还在单独维护吗？迁移到 7.0 要换什么？
10. Password grant（密码授权模式）为什么在 OAuth 2.1 里被移除？替代方案是什么？

> 💡 答不上的题，对应的模块序号就是你的学习优先级；答案全在本体系文档里。

---

**下一模块**：[01-核心架构：SecurityFilterChain与认证授权流程](01-核心架构：SecurityFilterChain与认证授权流程.md)　**返回总览**：本页
