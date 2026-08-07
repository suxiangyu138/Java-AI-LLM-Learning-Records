# SpringMVC 知识体系总览

> 从 DispatcherServlet 请求全链路、注解驱动开发、参数绑定与校验，到消息转换（Jackson 3）、异常处理（ProblemDetail）、异步与 SSE——Spring MVC 7.0（Spring Framework 7 / Boot 4.0，2025-11）完成 Jackson 3 全面迁移与虚拟线程默认化，2026 年最新 7.0.x

---

## 📚 目录

1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [为什么必须系统学透 SpringMVC](#3-为什么必须系统学透-springmvc)
4. [核心概念速查](#4-核心概念速查)
5. [与周边知识的关系](#5-与周边知识的关系)
6. [学习路线推荐](#6-学习路线推荐)
7. [快速自测 10 题](#7-快速自测-10-题)

---

## 1. 知识体系导图

```text
SpringMVC 知识体系
│
├── 01 DispatcherServlet 请求处理全链路
│   ├── 前端控制器模式与请求生命周期
│   ├── HandlerMapping：请求→处理器
│   ├── HandlerAdapter：处理器→参数→返回值
│   ├── HandlerExceptionResolver 与 ViewResolver
│   └── 与 Boot 4 自动配置的装配
│
├── 02 注解驱动开发：@Controller 与请求映射
│   ├── @RequestMapping 家族与组合注解
│   ├── 路径变量 / 请求参数 / 请求头绑定
│   ├── produces / consumes：内容协商前置
│   ├── @RequestBody / @ResponseBody 语义
│   └── 请求映射的匹配规则与冲突
│
├── 03 参数绑定与数据校验
│   ├── 参数绑定：简单类型 / 对象 / 集合 / 嵌套
│   ├── 类型转换体系：Converter 与 Formatter
│   ├── @Valid / @Validated 与分组校验
│   ├── 自定义校验注解与校验失败响应
│   └── MethodArgumentResolver 扩展点
│
├── 04 响应与消息转换：HttpMessageConverter 与 Jackson 3
│   ├── 消息转换器链与内容协商
│   ├── Jackson 3 迁移：tools.jackson 与类名重命名
│   ├── @JsonView / FilterProvider 的 hint 机制
│   ├── 日期格式、null 处理与配置定制
│   └── XML / Protobuf 等其它格式转换器
│
├── 05 异常处理：@ExceptionHandler 与 ProblemDetail
│   ├── HandlerExceptionResolver 解析链
│   ├── @RestControllerAdvice 全局异常处理
│   ├── RFC 7807 ProblemDetail 标准错误体
│   ├── 异常处理优先级与局部覆盖
│   └── 业务异常体系设计
│
├── 06 拦截器与过滤器
│   ├── Filter vs HandlerInterceptor 的本质区别
│   ├── 拦截器注册、顺序与三个回调时机
│   ├── 登录/鉴权/审计拦截器实战
│   ├── CORS 与响应头处理
│   └── 异步场景下拦截器的注意点
│
├── 07 异步处理：虚拟线程、SSE 与流式响应
│   ├── Boot 4 虚拟线程默认化与 MVC
│   ├── SSE：Server-Sent Events 实时推送
│   ├── DeferredResult / StreamingResponseBody
│   ├── 异步与拦截器/异常处理的兼容
│   └── 流式接口的生产注意（超时/背压）
│
├── 08 文件上传下载与内容协商
│   ├── MultipartFile 上传全流程
│   ├── 大文件、多文件与进度
│   ├── 下载：Content-Disposition 与断点续传
│   ├── 静态资源映射与缓存
│   └── 内容协商策略配置
│
└── 09 生产实践与面试题
    ├── REST API 设计规范与版本化
    ├── 性能调优与故障排查
    ├── Boot 3→4 迁移清单（Jackson 3 等）
    ├── 面试高频 15 问
    └── 版本特性速查（2026）
```

## 2. 模块导航

| 序号 | 模块 | 核心内容 | 适合人群 |
|:---:|------|----------|---------|
| 01 | DispatcherServlet 请求处理全链路 | 前端控制器、HandlerMapping/Adapter、解析链 | 入门必读 |
| 02 | 注解驱动开发 | @RequestMapping 家族、参数/头绑定、匹配规则 | 入门必读 |
| 03 | 参数绑定与数据校验 | 类型转换、@Valid 分组校验、自定义校验 | 高频核心 |
| 04 | 响应与消息转换 | HttpMessageConverter、Jackson 3 迁移、内容协商 | 高频核心 |
| 05 | 异常处理 | 解析链、@RestControllerAdvice、ProblemDetail | 高频核心 |
| 06 | 拦截器与过滤器 | Filter vs 拦截器、注册顺序、实战 | 进阶 |
| 07 | 异步处理 | 虚拟线程、SSE、流式响应 | 进阶 |
| 08 | 文件上传下载与内容协商 | Multipart、下载、静态资源 | 进阶 |
| 09 | 生产实践与面试题 | REST 规范、性能调优、迁移清单、面试题 | 冲刺 |

## 3. 为什么必须系统学透 SpringMVC

**1）它是 Java Web 后端的事实标准入口。** 所有请求先经过 SpringMVC——参数怎么绑定、校验怎么失败、异常怎么返回、消息怎么转换，决定了接口的"长相"与质量。面试问"接口怎么做"，答的就是这套体系。

**2）Spring Framework 7 的 Jackson 3 迁移是一次全局性变化。** `MappingJackson2HttpMessageConverter` → `JacksonJsonHttpMessageConverter`、包名 `com.fasterxml.jackson` → `tools.jackson`、SmartHttpMessageConverter + hints 取代旧包装器——升级 Boot 4 后接口返回结构、错误处理、配置方式都受影响。

**3）虚拟线程默认化改变并发心智。** Boot 4 在 Java 21+ 默认虚拟线程处理请求（39% 吞吐提升的实测案例），同步阻塞不再是线程池灾难——但 ThreadLocal 泄漏与 pinning 成为新陷阱。

**4）从"能返回 JSON"到"企业级接口"的差距在细节。** ProblemDetail 标准错误体、分组校验、内容协商、SSE 实时推送、优雅异常体系——这些是 2 年经验与 5 年经验的分水岭。

> 🎯 **核心要点**：SpringMVC 的底层是一条"请求处理管道"：DispatcherServlet 收请求 → HandlerMapping 找处理器 → HandlerAdapter 绑定参数调方法 → 消息转换器写响应 → 异常处理器兜底。学它 = 学"请求从 URL 到 JSON 的每一步发生了什么"。

## 4. 核心概念速查

| 概念 | 一句话本质 | 关键类/注解 |
|------|-----------|------------|
| 前端控制器 | 所有请求的统一入口与分发中心 | `DispatcherServlet` |
| HandlerMapping | 请求 URL → 处理器方法的映射 | `RequestMappingHandlerMapping` |
| HandlerAdapter | 处理器执行器（参数绑定+返回值处理） | `RequestMappingHandlerAdapter` |
| 控制器 | 业务处理单元 | `@Controller`/`@RestController` |
| 请求映射 | URL/方法/头/内容类型的匹配声明 | `@RequestMapping`/`@GetMapping` |
| 消息转换器 | Java 对象 ↔ HTTP 报文体 | `HttpMessageConverter`/`JacksonJsonHttpMessageConverter` |
| 异常解析器 | 异常 → 响应结果的翻译 | `HandlerExceptionResolver`/`@RestControllerAdvice` |
| ProblemDetail | RFC 7807 标准错误体 | `ProblemDetail` |
| 拦截器 | 处理器前后回调（比 Filter 晚） | `HandlerInterceptor` |
| 内容协商 | 按 Accept 头决定响应格式 | `ContentNegotiationManager` |
| SSE | 服务端单向实时推送 | `SseEmitter` |
| 虚拟线程 | Boot 4 默认请求处理线程模型 | `spring.threads.virtual.enabled` |

## 5. 与周边知识的关系

```text
                    ┌─────────────────────────────┐
                    │  Spring 全家桶（06-Spring全家桶） │
                    │  IoC / AOP / 事务 / 事件         │
                    └──────────────┬──────────────┘
                                   │ 控制器=Bean；事务/AOP 切在服务层
                    ┌──────────────▼──────────────┐
                    │       SpringMVC（本体系）       │
                    │  请求链路 / 绑定 / 转换 / 异常   │
                    └──────┬───────────────┬───────┘
                           │               │
          ┌────────────────▼───┐   ┌───────▼────────────────┐
          │ 安全：Spring Security│   │ 数据：Spring Data / Task│
          │ （过滤器/拦截器协同）  │   │ （校验、异步、SSE 联动） │
          └────────────────────┘   └────────────────────────┘
```

- **向上承接**：[Spring全家桶-04-Spring-MVC-Web层框架](../Spring全家桶/04-Spring-MVC-Web层框架.md) 有基础篇；[Spring生态深度剖析-05-DispatcherServlet请求处理全链路](../Spring生态深度剖析/05-DispatcherServlet请求处理全链路.md) 有源码级解析；本体系做 7.0 框架级系统化。
- **横向联动**：[Spring Security](../Spring Security与Spring Security OAuth2/00-Spring Security与OAuth2知识体系总览.md) 的过滤器链在 DispatcherServlet 之前；[Spring Task](../Spring Task/00-Spring Task知识体系总览.md) 的异步与 MVC 异步共用线程模型。
- **向下延伸**：REST 客户端（RestClient/RestTemplate）、API 网关转发，均以 MVC 的请求/响应语义为地基。

## 6. 学习路线推荐

**路线 A（初级 · 快速上手 3 天）**
01 请求链路 → 02 注解开发 → 03 参数绑定校验 → 04 消息转换。目标：能独立写出规范接口、理解参数与 JSON 的来龙去脉。

**路线 B（中级 · 生产工程师 1 周）**
路线 A + 05 异常处理 → 06 拦截器 → 08 文件与协商。目标：能搭企业级 REST 规范（统一错误体、鉴权拦截、上传下载）。

**路线 C（高级 · 架构与面试冲刺）**
全套 01-09，重点 04（Jackson 3 迁移）、07（虚拟线程与 SSE）、09（面试题与迁移清单）。目标：能讲透消息转换与异步机制、主导 Boot 3→4 迁移。

## 7. 快速自测 10 题

1. DispatcherServlet 处理一个请求经过哪些核心组件？顺序是什么？
2. @RestController 与 @Controller + @ResponseBody 等价吗？
3. @Valid 与 @Validated 的区别？分组校验怎么做？
4. Jackson 3 的消息转换器类名是什么？包名变成了什么？
5. 全局异常处理和局部 @ExceptionHandler 同时存在时谁生效？
6. ProblemDetail 是什么规范？怎么返回标准错误体？
7. Filter 与 HandlerInterceptor 的执行时机有什么区别？
8. SSE 与 WebSocket 的适用边界是什么？
9. Boot 4 虚拟线程下，ThreadLocal 有什么新风险？
10. 上传大文件要注意什么？下载断点续传怎么支持？

> 💡 答不上的题，对应的模块序号就是你的学习优先级；答案全在本体系文档里。

---

**下一模块**：[01-DispatcherServlet请求处理全链路](01-DispatcherServlet请求处理全链路.md)　**返回总览**：本页
