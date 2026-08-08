# SpringBoot Web 总览
> Spring Boot 的 Web 层专项体系：内嵌容器、DispatcherServlet 请求链路、MVC 定制、Filter/Interceptor、错误处理、静态资源、部署与 HTTPS——与 Servlet 体系同目录联动，专攻"Boot 的 Web 到底怎么跑起来"

## 📚 目录
1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [学习路线推荐](#3-学习路线推荐)
4. [SpringBoot Web 定位](#4-springboot-web-定位)
5. [核心概念速查](#5-核心概念速查)
6. [参考来源](#6-参考来源)

## 1. 知识体系导图

```text
SpringBoot Web 体系（8 篇，Boot 4.1 / Framework 7.0 / Tomcat 11 / Servlet 6.1 / Jackson 3 基准，2026-08）
│
├─ 认知层 ─────────────────────────────
│   ├─ 00 总览（本文）
│   └─ 01 内嵌容器与启动时序（WebServerFactory/启动链路/容器切换/server.* 配置）
│
├─ 机制层 ─────────────────────────────
│   ├─ 02 DispatcherServlet 请求处理链路（四大件/doDispatch/拦截器/异步返回）
│   ├─ 03 WebMvcConfigurer 定制全解（拦截器/CORS/消息转换器/参数解析）
│   └─ 04 Filter 与 Interceptor 注册与顺序（Boot 三路径/三层分工）
│
├─ 应用层 ─────────────────────────────
│   ├─ 05 错误处理机制深潜（/error/ErrorAttributes/异常分工）
│   └─ 06 静态资源与 SPA 路由（缓存/WebJars/history 路由/前后端分离）
│
└─ 实战层 ─────────────────────────────
│   ├─ 07 部署形态与 HTTPS 配置（Jar vs War/SSL/HTTP2/压缩/线程池）
│   └─ 08 生产实践与面试题（上传速查/故障排障/避坑/面试）
```

> 🎯 定位一句话：SpringBoot Web 是 **"Servlet 体系之上的框架化 Web 层"**——DispatcherServlet 是一个标准 Servlet（映射 `/`），Boot 只是把容器、配置、定制全部自动化的"零配置版本"。

## 2. 模块导航

| 序号 | 模块 | 核心内容 | 适合人群 |
|:---:|------|---------|---------|
| 00 | [总览（本文）](00-SpringBootWeb总览.md) | 导图、定位、速查 | 所有人 |
| 01 | [内嵌容器与启动时序](01-内嵌容器与启动时序.md) | WebServerFactory、启动链路、容器切换、server.* 全表 | 重点 |
| 02 | [DispatcherServlet 请求处理链路](02-DispatcherServlet请求处理链路.md) | 四大件、doDispatch 源码、拦截器链、异步返回类型 | 重点 |
| 03 | [WebMvcConfigurer 定制全解](03-WebMvcConfigurer定制全解.md) | 拦截器/CORS/静态资源/消息转换器/参数解析器 | 重点 |
| 04 | [Filter 与 Interceptor 注册与顺序](04-Filter与Interceptor注册与顺序.md) | Boot 三路径、OncePerRequestFilter、三层分工 | 进阶 |
| 05 | [错误处理机制深潜](05-错误处理机制深潜.md) | /error、ErrorAttributes、@Advice 分工、错误页 | 进阶 |
| 06 | [静态资源与 SPA 路由](06-静态资源与SPA路由.md) | 默认目录、缓存头、WebJars、history 路由 | 进阶 |
| 07 | [部署形态与 HTTPS 配置](07-部署形态与HTTPS配置.md) | Jar vs War、SSL/HTTP2、压缩、线程池调优 | 进阶 |
| 08 | [生产实践与面试题](08-生产实践与面试题.md) | 上传速查、故障排障、避坑、面试 | 收尾 |

## 3. 学习路线推荐

| 路线 | 人群 | 路径 |
|------|------|------|
| 快速入门（1 天） | Boot Web 使用者 | 00 → 01 → 02 → 03 |
| 应用进阶（2 天） | 要搞懂框架机制 | 04 → 05 → 06 |
| 面试冲刺（1 天） | 备战后端面试 | 00 → 02 → 03 → 05 → 08 |

> 💡 前置要求：本体系假设已了解 [Servlet](../Servlet/00-Servlet总览.md)、[Filter 过滤器](../Filter%20过滤器/00-Filter过滤器总览.md)、[Listener 监听器](../Listener%20监听器/00-Listener监听器总览.md)。**先学 Servlet 再看本体系，理解深度完全不同**。

## 4. SpringBoot Web 定位

### 4.1 与 06-Spring全家桶/SpringBoot 的分工

| 体系 | 覆盖范围 | 本体系不重复 |
|------|---------|-------------|
| `06-Spring全家桶/SpringBoot/` | 框架全貌：自动配置原理、配置体系、数据访问、测试、安全、可观测、微服务 | 基础 RESTful/参数绑定/校验/异常处理实践 |
| **本体系（SpringBoot Web）** | **Web 层专项**：内嵌容器、请求链路源码、MVC 定制、拦截体系、错误处理、静态资源、部署 | 框架级主题（数据/测试/安全） |

### 4.2 与 JavaWeb 体系的关系

```text
浏览器 → Tomcat 11（内嵌）→ Filter 链 → DispatcherServlet（一个标准 Servlet）
            ├── Servlet 规范   ←── 01 内嵌容器 / 04 Filter
            ├── MVC 机制       ←── 02 请求链路 / 03 定制
            ├── 错误与资源     ←── 05 错误处理 / 06 静态资源
            └── 部署形态       ←── 07 部署与 HTTPS
```

| 兄弟体系 | 与本体系的关系 |
|---------|---------------|
| [Servlet](../Servlet/00-Servlet总览.md) | DispatcherServlet 就是 Servlet，规范即底层 |
| [Filter 过滤器](../Filter%20过滤器/00-Filter过滤器总览.md) | Boot 中 Filter 注册的框架化三路径（04 深化） |
| [Listener 监听器](../Listener%20监听器/00-Listener监听器总览.md) | 容器事件监听在 Boot 的等价物（ApplicationListener） |
| [Cookie & Session](../Cookie%20%26%20Session（会话技术）/00-会话技术总览.md) | Session 配置在 Boot：`server.servlet.session.*` |

## 5. 核心概念速查

| 概念 | 一句话 |
|------|--------|
| DispatcherServlet | 一个标准 Servlet（映射 `/`），MVC 的前端控制器 |
| ServletWebServerFactory | 内嵌容器工厂（Tomcat/Jetty 的抽象） |
| TomcatStarter | 容器启动时回调，执行所有 ServletContextInitializer |
| HandlerMapping | URL → Controller 方法的映射器 |
| HandlerAdapter | 执行 Controller 方法 + 参数绑定 + 返回值解析 |
| HandlerExceptionResolver | 异常 → 响应（含 @ControllerAdvice） |
| ViewResolver | 逻辑视图名 → 视图渲染 |
| WebMvcConfigurer | MVC 定制的核心接口（拦截器/CORS/静态资源/转换器） |
| HandlerInterceptor | MVC 层拦截器（preHandle/postHandle/afterCompletion） |
| OncePerRequestFilter | 一次请求只执行一次的 Filter 基类 |
| BasicErrorController | `/error` 兜底控制器（白标页） |
| ErrorAttributes | 错误信息的结构化提供者 |
| PathPatternParser | Framework 7 默认路径匹配器（尾斜杠不再等价） |
| spring-boot-starter-webmvc | Boot 4 起 Web 启动器新名（旧名废弃） |
| Jackson 3 | 默认 JSON 库（tools.jackson 包，Boot 4 起） |
| spring.mvc.apiversion.* | Boot 4 内置 API 版本管理 |
| RestTestClient | Boot 4 测试新客户端（替代旧 MockMvc 自动配置） |

## 6. 参考来源

- [Spring Boot 官方文档（Web 部分）](https://docs.spring.io/spring-boot/reference/web/index.html)
- [Spring Boot 4.0 发布说明](https://github.com/spring-projects/spring-boot/releases/tag/v4.0.0)
- [Spring Boot 4.1 Release Highlights](https://spring.io/projects/release-highlights/)
- [Spring Boot 4.1 发布公告（2026-06-10）](https://github.com/spring-projects/spring-boot/releases/tag/v4.1.0)
- [Spring Boot 4 概览与迁移指南（中文社区）](https://garden.piemoe.com/diary/spring/springboot4-overview/)
- [Spring Framework 7 参考文档](https://docs.spring.io/spring-framework/reference/index.html)
- [Spring Boot 4.1 新特性（InfoQ）](https://www.infoq.com/news/2026/06/spring-boot-4-1/)

---

**下一模块**：[01-内嵌容器与启动时序](01-内嵌容器与启动时序.md)
