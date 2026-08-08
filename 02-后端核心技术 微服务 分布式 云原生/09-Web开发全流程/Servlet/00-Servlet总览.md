# Servlet 总览
> JavaWeb 的基石：Servlet 规范演进、生命周期、核心 API、路径映射、配置三方式、异步模型与生产实践——理解了 Servlet，才真正理解 Spring MVC 的 DispatcherServlet 与一切 Java Web 框架的底层

## 📚 目录
1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [学习路线推荐](#3-学习路线推荐)
4. [Servlet 定位](#4-servlet-定位)
5. [核心概念速查](#5-核心概念速查)
6. [参考来源](#6-参考来源)

## 1. 知识体系导图

```text
Servlet 体系（9 篇，Servlet 6.1 / Jakarta EE 11 / Tomcat 11 基准，2026-08）
│
├─ 认知层 ─────────────────────────────
│   ├─ 00 总览（本文）
│   └─ 01 规范演进史与版本矩阵（2.3→6.1/6.2、javax→jakarta、容器生态）
│
├─ 原理层 ─────────────────────────────
│   ├─ 02 生命周期与线程模型（五阶段/单例多线程/线程安全）
│   ├─ 03 核心 API 体系详解（继承层次/Request/Response/Config/Context）
│   └─ 04 请求处理与路径映射（URL 拆解/四种映射/转发重定向/编码）
│
├─ 机制层 ─────────────────────────────
│   ├─ 05 配置方式演进与动态注册（web.xml/注解/SCI/Spring Boot 注册）
│   ├─ 06 异步处理与非阻塞 IO（AsyncContext/NIO 监听器/虚拟线程）
│   └─ 07 文件上传下载与 Part API（MultipartConfig/流式处理/安全）
│
└─ 实战层 ─────────────────────────────
│   └─ 08 生产实践与面试题（安全清单/性能/避坑/面试）
```

> 🎯 定位一句话：Servlet 是 **"Java Web 一切的起点"**——一个跑在容器里的 Java 类，处理 HTTP 请求、生成响应；JavaWeb 三大组件（Servlet / Filter / Listener）之首，Spring MVC / Spring Boot 的 DispatcherServlet 本质就是一个扩展版 Servlet。

## 2. 模块导航

| 序号 | 模块 | 核心内容 | 适合人群 |
|:---:|------|---------|---------|
| 00 | [总览（本文）](00-Servlet总览.md) | 导图、定位、速查 | 所有人 |
| 01 | [规范演进史与版本矩阵](01-规范演进史与版本矩阵.md) | 2.3→6.2 演进、javax→jakarta、容器映射 | 入门必读 |
| 02 | [生命周期与线程模型](02-生命周期与线程模型.md) | 五阶段、实例化时机、单例多线程、线程安全 | 入门必读 |
| 03 | [核心 API 体系详解](03-核心API体系详解.md) | 继承层次、Request/Response 全 API、四大域对象 | 重点 |
| 04 | [请求处理与路径映射](04-请求处理与路径映射.md) | URL 拆解、四种映射、转发 vs 重定向、编码陷阱 | 重点 |
| 05 | [配置方式演进与动态注册](05-配置方式演进与动态注册.md) | web.xml/注解/SCI/web-fragment、Spring Boot 注册 | 重点 |
| 06 | [异步处理与非阻塞 IO](06-异步处理与非阻塞IO.md) | AsyncContext、ReadListener/WriteListener、Tomcat 模型、虚拟线程 | 进阶 |
| 07 | [文件上传下载与 Part API](07-文件上传下载与PartAPI.md) | @MultipartConfig、Part、流式大文件、安全 | 进阶 |
| 08 | [生产实践与面试题](08-生产实践与面试题.md) | 安全清单、性能、与 Spring MVC 关系、避坑、面试 | 收尾 |

## 3. 学习路线推荐

| 路线 | 人群 | 路径 |
|------|------|------|
| 快速入门（1 天） | JavaWeb 初学者 | 00 → 01 → 02 → 03 → 04 |
| 应用进阶（2 天） | 要理解框架底层 | 05 → 06 → 07 |
| 面试冲刺（1 天） | 备战后端面试 | 00 → 02 → 04 → 05 → 06 → 08 |

> 💡 建议配合同目录兄弟体系交叉学习：**Filter 过滤器**（请求链路拦截）、**Listener 监听器**（容器事件）、**Cookie & Session**（会话）、**Tomcat**（容器实现）。本体系与 `JavaWeb/01-Servlet核心技术深度解析.md`（旧版入门单篇）内容互补：本体系是**规范级深挖**，旧文是**快速入门**。

## 4. Servlet 定位

```text
一次 HTTP 请求的完整旅程：
浏览器 → 容器(Tomcat) Connector 解析 HTTP → [Filter 链] → Servlet(DispatcherServlet)
      → Controller → Service → DAO → 响应原路返回
                               ↑
                  Servlet 是请求处理的"终点站"，
                  Spring MVC 只是给 Servlet 加了路由分发能力
```

| Servlet 能做的 | 说明 |
|--------------|------|
| 处理 HTTP 请求 | 接收任意 HTTP 方法的请求并生成响应 |
| 管理生命周期 | 容器负责创建、初始化、调用、销毁 |
| 作为一切框架的底座 | Spring MVC / Struts / JSP 最终都编译或映射为 Servlet |
| 参与 Web 标准 | 与 Filter/Listener/JSP/Cookie/Session 共同构成 JavaWeb 三件套 |
| 提供扩展机制 | 异步处理、非阻塞 IO、编程式注册 |

### 4.1 与兄弟体系的边界

| 体系 | 与 Servlet 的关系 |
|------|------------------|
| [Filter 过滤器](..%2FFilter%20过滤器%2F00-Filter过滤器总览.md) | 请求**进入 Servlet 前**的拦截关卡（洋葱外层） |
| [Listener 监听器](..%2FListener%20监听器%2F00-Listener监听器总览.md) | 监听容器/会话/请求的**事件**（生命周期观察者） |
| [Cookie & Session](..%2FCookie%20%26%20Session（会话技术）%2F00-会话技术总览.md) | Servlet 通过 HttpSession API 管理**会话状态** |
| [Tomcat](..%2FTomcat%2F00-Tomcat总览与核心概念.md) | Servlet 规范的标准**实现容器** |
| [Spring Boot](..%2F..%2F06-Spring全家桶%2FSpringBoot%2F00-SpringBoot总览与核心概念.md) | DispatcherServlet 是 Servlet 的**框架级封装** |

## 5. 核心概念速查

| 概念 | 一句话 |
|------|--------|
| Servlet | 运行在容器中的 Java 类，处理 HTTP 请求并生成响应 |
| Servlet 容器 | 管理 Servlet 生命周期的运行环境（Tomcat/Jetty/Undertow） |
| 生命周期 | 加载 → 实例化 → init(1次) → service(N次) → destroy(1次) |
| 单例多线程 | 一个 Servlet 实例被所有请求线程共享，成员变量不安全 |
| service() | 按 HTTP 方法分发到 doGet/doPost/doPut/doDelete 等 |
| HttpServletRequest | 请求封装：参数、头、属性、流 |
| HttpServletResponse | 响应封装：状态码、头、输出流 |
| ServletConfig | 单个 Servlet 的初始化参数（局部） |
| ServletContext | 整个应用的全局上下文（唯一） |
| 四大域对象 | PageContext / Request / Session / Application |
| 路径映射 | 精确 > 路径前缀 > 扩展名 > 缺省 `/` |
| 缺省 Servlet | 处理未匹配请求与静态资源 |
| 转发 vs 重定向 | Forward 服务器内部1次请求；Redirect 浏览器2次请求 |
| @WebServlet | 注解声明 Servlet（Servlet 3.0+） |
| ServletContainerInitializer | 编程式动态注册入口（Servlet 3.0+） |
| AsyncContext | 异步处理：释放容器线程（3.0） |
| ReadListener/WriteListener | 非阻塞 IO（3.1，适配 NIO） |
| Part API | 文件上传组件（3.0+） |
| HttpSessionIdListener | 会话 ID 变更监听（6.1 新增，防会话固定攻击） |
| jakarta.* | Servlet 5.0 起的命名空间（javax.* 已终结） |

## 6. 参考来源

- [Jakarta Servlet 规范主页（6.0 / 6.1）](https://jakarta.ee/specifications/servlet/)
- [Jakarta Servlet 6.1 API 文档](https://jakarta.ee/specifications/servlet/6.1/apidocs/)
- [Servlet 6.0 规范正文（GitHub，含 Issue 变更清单）](https://github.com/jakartaee/servlet/blob/6.0.x/spec/src/main/asciidoc/servlet-spec-body.adoc)
- [Apache Tomcat 官方文档（版本对照）](https://tomcat.apache.org/whichversion.html)
- [Tomcat 11 迁移指南](https://tomcat.apache.org/migration-11.0.html)
- [Spring Boot Servlet 相关文档](https://docs.spring.io/spring-boot/reference/web/servlet.html)

---

**下一模块**：[01-规范演进史与版本矩阵](01-规范演进史与版本矩阵.md)
