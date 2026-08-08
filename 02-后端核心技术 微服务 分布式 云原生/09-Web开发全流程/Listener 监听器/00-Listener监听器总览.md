# Listener 监听器总览
> JavaWeb 三大组件之一（Servlet/Filter/Listener）：监听应用/会话/请求的生命周期与属性变化——"容器里发生的事，监听器替你看着"

## 📚 目录
1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [学习路线推荐](#3-学习路线推荐)
4. [Listener 定位](#4-listener-定位)
5. [核心概念速查](#5-核心概念速查)
6. [参考来源](#6-参考来源)

## 1. 知识体系导图

```text
Listener 监听器体系（6 篇）
│
├─ 基础层 ─────────────────────────────
│   ├─ 00 总览（本文）
│   └─ 01 Listener 基础与八大分类（生命周期/属性/会话）
│
├─ 核心层 ─────────────────────────────
│   ├─ 02 ServletContextListener 与启动初始化（最常用）
│   └─ 03 Session 与 Request 监听器（在线统计/超时清理）
│
├─ 工程层 ─────────────────────────────
│   └─ 04 Spring Boot 注册 Listener（@WebListener/注册 Bean）
│
└─ 实战层 ─────────────────────────────
│   └─ 05 生产实践与面试题
```

> 🎯 定位一句话：**Listener = 容器事件的"观察者"**——应用启动/关闭、会话创建/销毁、属性增删改发生时，监听器自动收到通知执行回调。

## 2. 模块导航

| 序号 | 模块 | 核心内容 | 适合人群 |
|:---:|------|---------|---------|
| 00 | [总览（本文）](00-Listener监听器总览.md) | 导图、定位、速查 | 所有人 |
| 01 | [Listener 基础与八大分类](01-Listener基础与八大分类.md) | 事件模型、八大监听器、触发时机 | 入门必读 |
| 02 | [ServletContextListener 与启动初始化](02-ServletContextListener与启动初始化.md) | 启动/关闭回调、初始化任务、context 共享 | 重点 |
| 03 | [Session 与 Request 监听器](03-Session与Request监听器.md) | 会话创建/销毁、在线统计、属性监听 | 重点 |
| 04 | [Spring Boot 注册 Listener](04-SpringBoot注册Listener.md) | @WebListener、注册 Bean、与事件体系关系 | 进阶 |
| 05 | [生产实践与面试题](05-生产实践与面试题.md) | 应用场景、避坑、面试 | 收尾 |

## 3. 学习路线推荐

| 路线 | 人群 | 路径 |
|------|------|------|
| 快速了解（半天） | JavaWeb 基础 | 00 → 01 → 02 |
| 工程应用（1 天） | 要写初始化/统计 | 02 → 03 → 04 |
| 面试冲刺（半天） | 备战后端面试 | 00 → 01 → 02 → 04 → 05 |

## 4. Listener 定位

```text
JavaWeb 三大组件分工：
  Servlet：处理请求（Controller）
  Filter：拦截请求（关卡）
  Listener：监听事件（观察者）

Listener 的触发时机：
  应用：启动（contextInitialized）/关闭（contextDestroyed）
  会话：创建/销毁/属性变化
  请求：开始/结束/属性变化
```

| 组件 | 角色 | 时机 |
|------|------|------|
| Servlet | 请求处理 | 每次请求 |
| Filter | 请求拦截 | 每次请求前后 |
| **Listener** | **事件监听** | **生命周期/属性变化时** |

> 🎯 **Listener 的本质**：**观察者模式在 JavaWeb 的应用**——容器是"被观察者"，监听器是"观察者"，事件发生时自动回调。

## 5. 核心概念速查

| 概念 | 一句话 |
|------|--------|
| 监听器接口 | 实现特定接口（如 ServletContextListener） |
| 触发时机 | 容器事件发生时自动调用回调方法 |
| 八大监听器 | 生命周期类 3 + 属性类 3 + 会话绑定类 2 |
| ServletContextListener | 应用启动/关闭（**最常用**） |
| HttpSessionListener | 会话创建/销毁（在线统计） |
| ServletRequestListener | 请求开始/结束 |
| 属性监听器 | 属性增/删/改 |
| @WebListener | 注解声明（需扫描） |
| 观察者模式 | 事件驱动机制的本质 |
| vs 事件体系 | JavaWeb Listener vs Spring 事件（层级不同） |

## 6. 参考来源

- [Jakarta Servlet 规范（Listener）](https://jakarta.ee/specifications/servlet/)
- [Baeldung：Servlet Listeners](https://www.baeldung.com/java-servlet-listeners)

---

**下一模块**：[01-Listener基础与八大分类](01-Listener基础与八大分类.md)
