# JSP 了解即可总览
> JSP（Java Server Pages）：传统 JavaWeb 页面技术——**定位"了解即可"**：理解原理与历史，现代项目用前后端分离/模板引擎替代

## 📚 目录
1. [知识体系导图](#1-知识体系导图)
2. [模块导航](#2-模块导航)
3. [JSP 的定位（为什么了解即可）](#3-jsp-的定位为什么了解即可)
4. [核心概念速查](#4-核心概念速查)
5. [参考来源](#5-参考来源)

## 1. 知识体系导图

```text
JSP 知识体系（4 篇，了解即可定位）
│
├─ 00 总览（本文）
├─ 01 JSP 基础语法与内置对象（脚本/指令/九大内置对象）
├─ 02 JSP 与 Servlet 的关系及 MVC（原理/翻译机制/模式演进）
└─ 03 JSP 现状与替代方案（Thymeleaf/前后端分离）
```

> 🎯 学习目标：**能看懂老项目、理解 JavaWeb 演进、面试答得上概念**——不需要深入写 JSP。

## 2. 模块导航

| 序号 | 模块 | 核心内容 | 深度 |
|:---:|------|---------|:---:|
| 00 | [总览（本文）](00-JSP了解即可总览.md) | 定位、速查 | - |
| 01 | [JSP 基础语法与内置对象](01-JSP基础语法与内置对象.md) | 脚本元素、指令、九大内置对象 | ⭐ |
| 02 | [JSP 与 Servlet 的关系及 MVC](02-JSP与Servlet的关系及MVC.md) | 翻译机制、Servlet 本质、MVC 演进 | ⭐⭐ |
| 03 | [JSP 现状与替代方案](03-JSP现状与替代方案.md) | 为何被取代、Thymeleaf、前后端分离 | ⭐⭐ |

## 3. JSP 的定位（为什么了解即可）

```text
JSP 的历史角色：
  1999 年推出：在 HTML 中写 Java（服务端动态页面）
  JavaWeb 早期主流（SSH/SSM 时代的视图层）
  2015+ 前后端分离兴起 → JSP 快速退场

现代替代：
  SSR 模板引擎：Thymeleaf（Spring Boot 官方支持）
  前后端分离：REST API + Vue/React（主流）
  JSP 只存在于存量老项目
```

| 维度 | JSP | Thymeleaf | 前后端分离 |
|------|-----|-----------|-----------|
| 渲染位置 | 服务器 | 服务器 | 浏览器（JS） |
| 页面技术 | Java 脚本 + HTML | HTML + 属性 | 框架（Vue/React） |
| 后端耦合 | 高 | 中 | 低（API） |
| 2026 现状 | 存量维护 | 少量 SSR 场景 | **主流** |

> 🎯 **学习策略**：**原理懂、语法认识、不深写**——面试答得出"JSP 本质是 Servlet""为什么被淘汰"，老项目能看懂即可。

## 4. 核心概念速查

| 概念 | 一句话 |
|------|--------|
| JSP | 在 HTML 中写 Java 的服务端页面技术 |
| 本质 | JSP 最终翻译为 Servlet |
| 脚本元素 | `<% %>`（代码）、`<%= %>`（输出）、`<%! %>`（声明） |
| 指令 | `<%@ page %>`、`<%@ include %>`、`<%@ taglib %>` |
| 内置对象 | request/response/session/application 等 9 个 |
| JSTL | JSP 标准标签库（替代脚本的标签） |
| EL | 表达式语言 `${...}`（取值） |
| MVC | JSP=View、Servlet=Controller、JavaBean=Model |
| 淘汰原因 | 前后端分离 + 维护性差 + 脚本逻辑混乱 |

## 5. 参考来源

- [Jakarta Pages（JSP）规范](https://jakarta.ee/specifications/pages/)
- [Oracle JSP 教程（历史）](https://docs.oracle.com/javaee/5/tutorial/doc/bnagx.html)
- [Thymeleaf 官方文档](https://www.thymeleaf.org/)

---

**下一模块**：[01-JSP基础语法与内置对象](01-JSP基础语法与内置对象.md)
