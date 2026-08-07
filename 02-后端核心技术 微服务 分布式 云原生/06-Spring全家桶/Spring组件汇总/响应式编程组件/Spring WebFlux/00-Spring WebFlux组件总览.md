# 00 Spring WebFlux 组件总览

> 组件卡片：Spring WebFlux 是什么、版本现状、能做什么、与深度体系如何衔接——本系列是"组件汇总"速查型文档；WebFlux 是 Spring 的响应式 Web 栈（SF 内置模块），暂无独立深度体系，深挖见官方参考文档

---

## 📚 目录

1. [组件一句话定位](#1-组件一句话定位)
2. [版本现状（2026-08）](#2-版本现状2026-08)
3. [能力地图](#3-能力地图)
4. [与深度体系的映射](#4-与深度体系的映射)
5. [快速上手 3 步](#5-快速上手-3-步)
6. [速查导航](#6-速查导航)
7. [学习路线推荐](#7-学习路线推荐)
8. [核心概念速查](#8-核心概念速查)

---

## 1. 组件一句话定位

**Spring WebFlux 是 Spring 生态的响应式 Web 栈**——基于 Reactor（Mono/Flux）与 Netty，以非阻塞事件驱动处理 HTTP/SSE/WebSocket，天然支持背压与流式响应，是 Spring MVC（Servlet 阻塞栈）的响应式对应物。

```text
核心心智模型：
  两种编程风格（同能力，任选）
    ├── 注解式：@RestController + Mono/Flux 返回值（像 MVC，但返回发布者）
    └── 函数式：RouterFunction + HandlerFunction（路由即代码）
  执行模型
    ├── Netty event loop（默认服务器，少量线程扛海量连接）
    ├── 非阻塞 IO + 背压（下游慢 → 上游停）
    └── 响应式链路全程无阻塞（JDBC 等阻塞调用是禁区）
  周边能力
    ├── WebClient（响应式 HTTP 客户端）
    ├── WebSocket / SSE（长连接流式）
    └── WebFilter / WebSession（横切）
```

> 🎯 **一句话**：WebFlux = "把 Controller 从『每个请求占一个线程』变成『少量线程异步处理海量请求』"——是否值得用，看你的技术栈是否全响应式（见版本现状）。

## 2. 版本现状（2026-08）

| 维度 | 现状 |
|------|------|
| 载体 | Spring Framework 内置模块 `spring-webflux`（随 SF **7.0.x** / Boot **4.0.x**，当前 4.0.7） |
| 底层 | Reactor 3.8.x + reactor-netty 2.x（默认 Netty 服务器） |
| Java 基线 | 17+（Boot 4 建议 21/25） |
| 虚拟线程 | SF 7 原生支持（`spring.threads.virtual.enabled` / `@VirtualThread`） |

**SF 7 的关键变化与定位重塑**：

- **虚拟线程改变选型格局**：SF 7 让 MVC + 虚拟线程也能低成本扛高并发——**"为了高并发而选 WebFlux"的理由不再成立**；WebFlux 的价值收窄到"技术栈全响应式"与"流式语义"场景（网关扇出、R2DBC 端到端、SSE 长连接）；
- **ReactiveAdapterRegistry**：响应式与命令式代码可无缝混合（新能力）；
- **Kotlin 协程结构化并发**：CoroutineScope 集成（suspend 函数 / Flow / coRouter DSL）；
- **WebSocket 简化**：非响应式 WebSocket handler 移除，仅保留响应式。

> 💡 选型定论（2026 面试标准答案）：**常规高并发业务 → MVC + 虚拟线程；全响应式技术栈 / 流式长连接 → WebFlux**——WebFlux 不是"更快"，而是"线程模型不同"。

## 3. 能力地图

| 能力域 | 能力 | 代表 API |
|--------|------|---------|
| 编程风格 | 注解式 / 函数式双模型 | @RestController + Mono/Flux、RouterFunction |
| 响应式 Web | 响应式 Controller | 返回 Mono<T>/Flux<T>/Mono<ResponseEntity<T>> |
| 客户端 | 响应式 HTTP 客户端 | WebClient（非阻塞、可重试、可流式） |
| 长连接 | SSE / WebSocket | Flux<ServerSentEvent>、WebSocketHandler |
| 横切 | 过滤器与会话 | WebFilter、WebSession、HandlerInterceptor 对应物 |
| 数据绑定 | 响应式校验与绑定 | @Valid + Mono（WebExchangeBindException） |
| 异常处理 | 响应式统一错误 | @ControllerAdvice（ResponseStatusException 等） |
| 数据访问 | 响应式 DAO | R2DBC / 响应式 Mongo 驱动（端到端非阻塞） |
| 测试 | 响应式测试 | WebTestClient（bindToServer/RouterFunction） |
| 互操作 | 响应式/命令式混合 | ReactiveAdapterRegistry（SF 7） |
| 协议 | HTTP/2、SSE、WebSocket | 基于 Netty |

## 4. 与深度体系的映射

| 本卡片模块 | 深度体系对应 | 衔接说明 |
|-----------|-------------|---------|
| 02 核心类 | [Project Reactor 卡片](../Project Reactor/00-Project Reactor组件总览.md)（同级） | WebFlux 的一切返回值都是 Mono/Flux——先懂 Reactor 再懂 WebFlux |
| 04 集成地图 | [Spring框架核心 深度体系](../../../Spring框架核心/00-Spring框架核心知识体系总览.md) | 容器/Bean 生命周期与 MVC 同源 |
| 04 集成地图 | [Spring Batch 深度体系-08 调度、运维与可观测性](../../../Spring Batch/08-调度、运维与可观测性.md) | 响应式触发批处理（Mono 包裹 JobOperator 调用） |

> 💡 WebFlux 生态位：**它是 SF 内置模块**（非独立项目）——与 MVC 共享大部分注解/异常/配置心智，差异集中在"线程模型与返回值类型"；本仓库暂无独立深度体系，深入以 [官方参考文档](https://docs.spring.io/spring-framework/reference/web/webflux.html) 为准。

## 5. 快速上手 3 步

```text
① 引入 spring-boot-starter-webflux（替代 web，内嵌 Netty）
② @RestController + Mono/Flux 返回值（或 RouterFunction）
③ WebTestClient 测试 / WebClient 调用
```

```java
// 响应式 Controller：最小示例
@RestController
class OrderController {
    private final ReactiveOrderRepository repo;   // R2DBC 仓储

    @GetMapping("/orders/{id}")
    Mono<Order> find(@PathVariable Long id) {
        return repo.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND)));
    }

    @GetMapping(value = "/orders/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    Flux<Order> stream() {
        return repo.findAll();                    // SSE 流式推送
    }
}
```

> 💡 运行即得：Netty 默认端口 8080；返回 Mono/Flux 由框架自动订阅并响应——**不要手动 subscribe/block**。

## 6. 速查导航

| 卡片 | 内容 | 何时翻 |
|------|------|--------|
| [01-模块清单](01-模块清单.md) | artifact 与 Starter 对照 | 加依赖、服务器选型 |
| [02-核心类与注解速查](02-核心类与注解速查.md) | Controller/路由/WebClient/SSE/WebSocket | 写响应式接口时随手查 |
| [03-配置属性速查](03-配置属性速查.md) | spring.webflux.* 属性字典 | 写 application.yml 时 |
| [04-集成地图与常见问题](04-集成地图与常见问题.md) | 生态集成 + 选型 + 高频坑 | 集成/排障 |

## 7. 学习路线推荐

```text
快速上手：00 → 01 → 05（三步）→ 02 核心类
完整响应式：00 → Reactor 卡片全卡 → 02 → 03 → 04（R2DBC 端到端）
面试冲刺：00 版本现状（虚拟线程选型）→ 04（WebFlux vs MVC 对比）
```

## 8. 核心概念速查

| 概念 | 一句话 |
|------|--------|
| Mono/Flux 返回值 | Controller 返回发布者，框架自动订阅 |
| RouterFunction | 函数式路由（代码式 URL 映射） |
| WebClient | 响应式 HTTP 客户端（非阻塞调用） |
| SSE / WebSocket | 流式推送 / 双向长连接 |
| WebFilter | 响应式过滤器（认证/日志） |
| 背压 | 下游慢 → 上游停（非阻塞的核心） |
| WebTestClient | 响应式测试客户端 |
| ReactiveAdapterRegistry | 响应式/命令式互操作（SF 7） |

---

> 🎯 **核心要点**：WebFlux = 注解式/函数式双风格 + Mono/Flux + Netty 非阻塞；SF 7 时代它的定位是"全响应式栈与流式场景"，高并发红利已被 MVC + 虚拟线程摊薄——**选型先问技术栈，再问场景**。

**下一模块**：[01-模块清单](01-模块清单.md)
