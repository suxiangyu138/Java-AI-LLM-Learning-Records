# Filter 过滤器总览
> Servlet 请求链路的第一道关卡：Filter 生命周期、责任链、应用场景、Spring Boot 注册、与 Interceptor/AOP 的分工

## 📚 目录
1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [学习路线推荐](#3-学习路线推荐)
4. [Filter 定位](#4-filter-定位)
5. [核心概念速查](#5-核心概念速查)
6. [参考来源](#6-参考来源)

## 1. 知识体系导图

```text
Filter 过滤器体系（7 篇，Servlet 6.x 基准）
│
├─ 基础层 ─────────────────────────────
│   ├─ 00 总览（本文）
│   ├─ 01 Filter 基础与生命周期（init/doFilter/destroy）
│   └─ 02 FilterChain 责任链（链式执行/顺序/包装）
│
├─ 应用层 ─────────────────────────────
│   ├─ 03 常用 Filter 场景（登录/日志/编码/CORS/限流/XSS）
│   └─ 04 Spring Boot 注册 Filter（三方式/顺序/OncePerRequestFilter）
│
├─ 辨析层 ─────────────────────────────
│   └─ 05 Filter vs Interceptor vs AOP（分工与选型）
│
└─ 实战层 ─────────────────────────────
│   └─ 06 生产实践与面试题
```

> 🎯 定位一句话：Filter 是**"Servlet 请求链路的统一拦截关卡"**——请求进业务前与响应出业务后的公共逻辑都在这做。

## 2. 模块导航

| 序号 | 模块 | 核心内容 | 适合人群 |
|:---:|------|---------|---------|
| 00 | [总览（本文）](00-Filter过滤器总览.md) | 导图、定位、速查 | 所有人 |
| 01 | [Filter 基础与生命周期](01-Filter基础与生命周期.md) | 接口、三方法、生命周期、执行时机 | 入门必读 |
| 02 | [FilterChain 责任链](02-FilterChain责任链.md) | 链式执行、顺序、请求包装 | 重点 |
| 03 | [常用 Filter 场景](03-常用Filter场景.md) | 登录/日志/编码/CORS/限流/XSS | 重点 |
| 04 | [Spring Boot 注册 Filter](04-SpringBoot注册Filter.md) | 三方式、顺序控制、OncePerRequestFilter | 重点 |
| 05 | [Filter vs Interceptor vs AOP](05-Filter-vs-Interceptor-vs-AOP.md) | 三层拦截分工与选型 | 进阶 |
| 06 | [生产实践与面试题](06-生产实践与面试题.md) | 避坑、最佳实践、面试 | 收尾 |

## 3. 学习路线推荐

| 路线 | 人群 | 路径 |
|------|------|------|
| 快速入门（半天） | JavaWeb 基础 | 00 → 01 → 02 → 04 |
| 应用进阶（1 天） | 要写公共逻辑 | 03 → 04 → 05 |
| 面试冲刺（半天） | 备战后端面试 | 00 → 01 → 02 → 05 → 06 |

## 4. Filter 定位

```text
请求处理链路中的位置：
客户端 → Servlet 容器 → [Filter 链] → Servlet/Controller → 业务 → 响应
                          │ 公共逻辑（前置/后置）│
```

| Filter 能做的 | 说明 |
|--------------|------|
| 请求前置 | 登录校验、日志、编码、CORS |
| 请求拦截 | 限流、黑名单、XSS 过滤 |
| 响应后置 | 统一响应头、压缩、日志耗时 |
| 请求包装 | 修改请求参数/读取 Body（包装类） |

> 🎯 **Filter 的本质**：**"横切"请求处理**——与业务无关的公共逻辑（鉴权/日志/编码）统一在 Filter 做，业务代码保持干净。

## 5. 核心概念速查

| 概念 | 一句话 |
|------|--------|
| Filter 接口 | 三个方法：init/doFilter/destroy |
| FilterChain | 过滤器链（链式调用） |
| doFilter | 核心方法（拦截 + 放行 chain.doFilter） |
| 生命周期 | 容器启动 init、每次请求 doFilter、关闭 destroy |
| 责任链 | 多个 Filter 按顺序执行 |
| @WebFilter | 注解声明（需 @ServletComponentScan） |
| FilterRegistrationBean | Boot 注册 + 顺序控制 |
| OncePerRequestFilter | 保证一次请求只执行一次（转发场景） |
| 过滤器顺序 | @Order 或 registrationBean.setOrder |
| vs Interceptor | Filter 在容器层（Servlet）、Interceptor 在 MVC 层 |
| vs AOP | 拦截目标层级不同 |

## 6. 参考来源

- [Jakarta Servlet 规范（Filter）](https://jakarta.ee/specifications/servlet/)
- [Spring Boot Filter 文档](https://docs.spring.io/spring-boot/reference/web/servlet.html)
- [Baeldung：Spring Boot Filters](https://www.baeldung.com/spring-boot-add-filter)

---

**下一模块**：[01-Filter基础与生命周期](01-Filter基础与生命周期.md)
