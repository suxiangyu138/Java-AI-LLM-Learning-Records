# 03-JSP 现状与替代方案
> JSP 为什么退场、Thymeleaf 怎么用、前后端分离主流——"知道往哪走，比知道从哪来更重要"

## 📚 目录
1. [JSP 的现状](#1-jsp-的现状)
2. [JSP 退场的原因](#2-jsp-退场的原因)
3. [替代方案一：Thymeleaf（服务端模板）](#3-替代方案一thymeleaf服务端模板)
4. [替代方案二：前后端分离（主流）](#4-替代方案二前后端分离主流)
5. [存量 JSP 项目的处理](#5-存量-jsp-项目的处理)
6. [核心要点](#6-核心要点)
7. [参考来源](#7-参考来源)

## 1. JSP 的现状

```text
2026 现状：
  Spring Boot 官方默认模板：Thymeleaf（不是 JSP）
  JSP 仅存在于存量老项目（SSH/SSM 时代遗留）
  新项目选型：前后端分离为主、Thymeleaf 少量 SSR 场景

面试结论：JSP 不是新项目技术——是"历史知识"
```

| 场景 | JSP 使用 |
|------|:---:|
| 新项目 | ❌ 不用 |
| 存量维护 | ⚠️ 需看懂 |
| 面试 | ✅ 原理必问 |
| 老项目迁移 | ⚠️ 改造计划 |

## 2. JSP 退场的原因

| 原因 | 说明 |
|------|------|
| **前后端分离成为主流** | REST API + Vue/React，后端专注接口 |
| 逻辑混写 | 脚本段 `<% %>` 逻辑与页面混合，维护性差 |
| 前端技术发展 | 前端框架渲染能力远超服务端模板 |
| 团队分工 | 分离后前端/后端各司其职 |
| Spring 生态转向 | Boot 默认 Thymeleaf（比 JSP 更规范） |

> 🎯 **核心原因一句话**：**前后端分离让"服务端模板"整体退场**——JSP 只是"服务端模板时代"的代表，不是唯一原因。

## 3. 替代方案一：Thymeleaf（服务端模板）

```html
<!-- Thymeleaf：HTML 属性式语法（与 JSP 的脚本式完全不同）-->
<html xmlns:th="http://www.thymeleaf.org">
<body>
    <h1 th:text="${user.name}">默认值</h1>        <!-- 输出 -->
    <div th:if="${not #lists.isEmpty(users)}">有数据</div>

    <table>
        <tr th:each="u : ${users}">               <!-- 循环 -->
            <td th:text="${u.id}">1</td>
            <td th:text="${u.name}">name</td>
        </tr>
    </table>
</body>
</html>
```

```xml
<!-- Spring Boot 集成 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-thymeleaf</artifactId>
</dependency>
```

```yaml
spring:
  thymeleaf:
    cache: false        # 开发期关闭缓存（改完即生效）
```

| 对比 | JSP | Thymeleaf |
|------|-----|-----------|
| 语法 | 脚本（`<% %>`） | HTML 属性（`th:`） |
| 页面纯净 | 混 Java | 纯 HTML（浏览器可直接打开） |
| 前后端协作 | 需服务器渲染 | 静态原型可独立预览 |
| 逻辑控制 | 脚本/JSTL | th:if/th:each/th:text |

> 💡 **Thymeleaf 的核心优势**：**"天然 HTML"**——设计师可以单独打开页面预览，后端渲染时才执行 th: 属性；比 JSP 的"页面里塞 Java"干净得多。

## 4. 替代方案二：前后端分离（主流）

```text
前后端分离架构：
  前端（Vue/React）→ REST API → 后端（Spring Boot）→ 数据

渲染位置：浏览器（JS 渲染）
页面技术：前端框架组件
后端职责：纯 API（JSON 数据）
```

| 维度 | 服务端模板（JSP/Thymeleaf） | 前后端分离 |
|------|---------------------------|-----------|
| 渲染 | 服务器 | 浏览器 |
| 接口 | 页面请求 | REST API |
| 后端 | 渲染页面 + 数据 | 纯数据 API |
| 前端 | HTML 片段 | 完整应用（路由/状态） |
| 部署 | 一体 | 前后端独立部署 |
| 适用 | 简单 SSR/SEO | **中大型应用主流** |

> 🎯 **2026 选型判断**：**中大型应用前后端分离**（Vue/React + REST）；简单页面/SEO 需求用 Thymeleaf；**JSP 不再入新项目**——三条线清晰。

## 5. 存量 JSP 项目的处理

```text
存量 JSP 项目的策略：
  ① 能不动就不动（稳定优先，维护为主）
  ② 新功能尽量不新增 JSP（新页面用 API + 前端）
  ③ 渐进改造：核心页面逐步迁出（接口化）
  ④ 全面重构：评估成本后整体前后端分离

改造优先级：
  先"新功能接口化"→ 再"核心页面迁移"→ 最后"整体切换"
```

| 策略 | 适用 |
|------|------|
| 维护不动 | 稳定老系统（改动风险 > 收益） |
| 新功能 API 化 | 过渡期 |
| 渐进迁移 | 页面逐步替换 |
| 整体重构 | 重写成本可控 |

> 💡 **改造建议**：**别为"用新技术"而重写稳定系统**——JSP 老项目只要稳定，维护性优先级高于技术先进性；评估"重写风险"比"技术情怀"更重要。

## 6. 核心要点

> 🎯 **核心要点**：
> - 2026 现状：新项目不用 JSP（Thymeleaf 或前后端分离），JSP 是历史知识；
> - 退场核心原因：前后端分离 + 逻辑混写维护差；
> - Thymeleaf：HTML 属性式（th:text/th:if/th:each），页面纯净可独立预览；
> - 前后端分离是主流：后端纯 API，前端 Vue/React 渲染；
> - 存量 JSP：稳定优先、新功能接口化、渐进迁移（不为新技术重写稳定系统）；
> - 学习定位：原理懂、语法认识、会排错——面试与老项目维护够用。

## 7. 参考来源

- [Thymeleaf 官方文档](https://www.thymeleaf.org/)
- [Spring Boot Thymeleaf 文档](https://docs.spring.io/spring-boot/reference/web/servlet.html)
- [Jakarta Pages（JSP）规范](https://jakarta.ee/specifications/pages/)

---

**返回总览**：[00-总览](00-JSP了解即可总览.md)
