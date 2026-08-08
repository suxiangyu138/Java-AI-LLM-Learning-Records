# 02-JSP 与 Servlet 的关系及 MVC
> 面试核心："JSP 本质是 Servlet"、翻译机制、MVC 模式演进——"理解 JSP 原理比会写 JSP 更重要"

## 📚 目录
1. [JSP 的本质：Servlet](#1-jsp-的本质servlet)
2. [翻译机制](#2-翻译机制)
3. [JSP 与 Servlet 对比](#3-jsp-与-servlet-对比)
4. [JavaWeb 的 MVC 演进](#4-javaweb-的-mvc-演进)
5. [面试必答](#5-面试必答)
6. [核心要点](#6-核心要点)
7. [参考来源](#7-参考来源)

## 1. JSP 的本质：Servlet

```text
面试第一问：JSP 是什么？
答案：JSP 本质是 Servlet——最终被编译成 Servlet 执行

工作目录（Tomcat）：
  work/Catalina/localhost/xxx/org/apache/jsp/
  index_jsp.java        ← JSP 翻译的 Java 源文件
  index_jsp.class       ← 编译后的类（继承 HttpJspBase → HttpServlet）
```

> 🎯 **一句话原理**：**JSP = 以页面形式书写的 Servlet**——容器把 JSP 翻译成 Servlet（`_jspService` 方法），再走 Servlet 生命周期。

## 2. 翻译机制

```text
JSP 执行流程：
① 第一次访问：JSP 文件 → 翻译为 .java（Servlet 源码）
② 编译：.java → .class
③ 实例化并执行：_jspService(request, response)
④ 后续访问：直接使用已编译的 Servlet（不再翻译）

首次访问慢的原因：翻译 + 编译开销
```

```java
// 翻译后的 Servlet 骨架（理解用）
public final class index_jsp extends HttpJspBase {

    public void _jspService(HttpServletRequest request,
                            HttpServletResponse response) {
        // JSP 的 HTML 输出 = out.write("...")
        out.write("<html>...");
        // JSP 的 Java 片段 = 原样放进方法
        // JSP 的 <%= expr %> = out.print(expr)
        // JSP 的 ${user.name} = 取值后 out.print
    }
}
```

| 翻译映射 | JSP 元素 | 生成代码 |
|----------|---------|---------|
| HTML | 静态内容 | `out.write("...")` |
| `<% 代码 %>` | 脚本段 | 原样嵌入方法 |
| `<%= 表达式 %>` | 表达式 | `out.print(expr)` |
| `<%! %>` | 声明 | 成为类的成员 |

> 💡 **了解翻译机制的价值**：① 理解"首次访问慢"；② 排错看翻译后的 Java（JSP 报错行号对应 .java）；③ 面试讲原理有深度。

## 3. JSP 与 Servlet 对比

| 维度 | Servlet | JSP |
|------|---------|-----|
| 定位 | Java 类（逻辑） | 页面（视图） |
| 写 HTML | 不方便（字符串拼接） | 天然支持 |
| 写 Java | 天然 | 脚本段（被诟病） |
| 本质 | - | **也是 Servlet** |
| 分工 | Controller | View |

```text
传统分工（MVC）：
  Servlet = Controller（接收请求、调逻辑、转发）
  JSP = View（渲染页面）
  JavaBean/Service = Model（业务）

Servlet 处理完：request.setAttribute("users", list);
             → request.getRequestDispatcher("list.jsp").forward(req, resp);
JSP 读取：${users} 渲染列表
```

> 🎯 **MVC 分工记忆**：**Servlet 管"干什么"（Controller）、JSP 管"长什么样"（View）、JavaBean 管"数据"（Model）**——三者各司其职。

## 4. JavaWeb 的 MVC 演进

```text
演进主线（JavaWeb 视图层）：
  JSP 脚本（<% %> 混逻辑）——最原始，维护差
    → JSP + JSTL/EL（标签替代脚本）——规范点
    → JSP + Servlet MVC（SSH/SSM 时代标准）
    → Thymeleaf（Spring Boot 官方模板）——现代 SSR
    → 前后端分离（REST + Vue/React）——主流

核心驱动：逻辑与视图的分离程度越来越高
```

| 时代 | 视图方案 | 特点 |
|------|---------|------|
| JSP 脚本时代 | `<% %>` 混写 | 维护灾难 |
| SSH/SSM 时代 | JSP + JSTL MVC | 规范化 |
| Boot 时代 | Thymeleaf | 服务端模板现代版 |
| 2026 主流 | 前后端分离 | 彻底解耦 |

> 🎯 **演进主线是面试亮点**：能讲出"从 JSP 脚本到前后端分离的演进逻辑 = 逻辑与视图分离"——比只会用 JSP 高一个层次。

## 5. 面试必答

```text
Q1：JSP 和 Servlet 的关系？
A：JSP 本质是 Servlet——容器把 JSP 翻译成 Servlet（继承 HttpJspBase）
   执行 _jspService 方法；首次访问翻译+编译较慢。

Q2：MVC 中 JSP 的角色？
A：View（视图层）——Servlet 是 Controller、JavaBean/Service 是 Model；
   Servlet 处理请求 → 转发 JSP 渲染页面。

Q3：JSP 为什么被淘汰？
A：① 前后端分离（REST API + 前端框架）成为主流
   ② JSP 脚本逻辑与页面混写维护性差
   ③ 前端技术发展（Vue/React 渲染能力远超服务端模板）

Q4：JSP 的九大内置对象？（加分）
A：request/response/session/application/pageContext/out/config/page/exception
```

## 6. 核心要点

> 🎯 **核心要点**：
> - **JSP 本质是 Servlet**（翻译机制：HTML→write、脚本→嵌入、表达式→print）；
> - 首次访问慢的原因 = 翻译+编译；
> - MVC 分工：Servlet=Controller、JSP=View、Bean=Model；
> - 演进主线：JSP 脚本 → JSTL → MVC → Thymeleaf → 前后端分离（逻辑与视图分离）；
> - 面试四问：关系/角色/淘汰原因/内置对象——答好即达标；
> - 了解即可定位：原理清楚，语法认识，不深写。

## 7. 参考来源

- [Jakarta Pages（JSP）规范](https://jakarta.ee/specifications/pages/)
- [Jakarta Servlet 规范](https://jakarta.ee/specifications/servlet/)
- [Baeldung：JSP 与 Servlet](https://www.baeldung.com/jsp)

---

**下一模块**：[03-JSP现状与替代方案](03-JSP现状与替代方案.md)　/　**返回总览**：[00-总览](00-JSP了解即可总览.md)
