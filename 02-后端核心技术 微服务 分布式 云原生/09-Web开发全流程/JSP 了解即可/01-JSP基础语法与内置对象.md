# 01-JSP 基础语法与内置对象
> 脚本元素、指令、九大内置对象、JSTL/EL——"认识 JSP 的语法，看懂老项目"

## 📚 目录
1. [JSP 文件结构](#1-jsp-文件结构)
2. [脚本元素](#2-脚本元素)
3. [指令](#3-指令)
4. [九大内置对象](#4-九大内置对象)
5. [EL 表达式与 JSTL](#5-el-表达式与-jstl)
6. [核心要点](#6-核心要点)
7. [参考来源](#7-参考来源)

## 1. JSP 文件结构

```jsp
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head><title>用户列表</title></head>
<body>
    <!-- 页面 = HTML + Java 片段 -->
    <%-- 这是 JSP 注释（客户端不可见） --%>
    <h1>欢迎：<%= session.getAttribute("user") %></h1>
</body>
</html>
```

```text
JSP = HTML 模板 + Java 片段
  服务端渲染完成后 → 纯 HTML 发给浏览器
  浏览器看到的只是渲染结果
```

> 🎯 核心认知：**JSP 是"服务端模板"**——服务器把 Java 片段执行结果拼进 HTML，再发给浏览器（浏览器只看到 HTML）。

## 2. 脚本元素

| 元素 | 语法 | 用途 |
|------|------|------|
| 脚本段 | `<% Java代码 %>` | 执行逻辑（if/for/赋值） |
| 表达式 | `<%= 表达式 %>` | 输出（等价 out.print） |
| 声明 | `<%! 声明 %>` | 成员变量/方法 |

```jsp
<%-- 脚本段：逻辑 --%>
<%
    List<User> users = (List<User>) request.getAttribute("users");
%>

<%-- 表达式：输出 --%>
<% for (User u : users) { %>
    <tr>
        <td><%= u.getId() %></td>
        <td><%= u.getName() %></td>
    </tr>
<% } %>

<%-- 声明：成员 --%>
<%! private int counter = 0; %>
```

> ⚠️ **脚本段是 JSP 被诟病的原因**：业务逻辑混在页面里——维护性差；现代模板引擎（Thymeleaf）强制分离逻辑。

## 3. 指令

| 指令 | 作用 | 示例 |
|------|------|------|
| `<%@ page %>` | 页面属性 | 编码/错误页/session |
| `<%@ include %>` | 静态包含 | 公共头部/尾部 |
| `<%@ taglib %>` | 引入标签库 | JSTL |

```jsp
<%@ page contentType="text/html;charset=UTF-8" language="java"
         errorPage="error.jsp"            %>  <%-- 错误页 --%>
<%@ include file="header.jsp"              %>  <%-- 公共头部 --%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>  <%-- JSTL --%>
```

> 💡 了解即可的重点：**page 指令的编码与错误页**（老项目排错常看）；include 用于公共片段复用。

## 4. 九大内置对象

| 对象 | 类型 | 作用域 | 说明 |
|------|------|--------|------|
| `request` | HttpServletRequest | 请求 | 请求数据 |
| `response` | HttpServletResponse | 请求 | 响应 |
| `session` | HttpSession | 会话 | **登录态（重点，见会话体系）** |
| `application` | ServletContext | 全局 | 全局共享 |
| `pageContext` | PageContext | 页面 | 页面上下文 |
| `out` | JspWriter | 页面 | 输出 |
| `config` | ServletConfig | 页面 | 配置 |
| `page` | Object | 页面 | 当前页面 |
| `exception` | Throwable | 页面 | 异常（errorPage 才有） |

```jsp
<%-- 内置对象直接用（无需声明）--%>
<%= request.getParameter("id") %>          <%-- 请求参数 --%>
<%= session.getAttribute("user") %>        <%-- 会话数据 --%>
<%= application.getAttribute("counter") %> <%-- 全局数据 --%>
```

> 🎯 **重点内置对象**：`request`（请求数据）、`session`（登录态）、`application`（全局）——与 [Servlet](../Servlet/00-Servlet知识体系总览.md) 体系对应；**内置对象 = 自动注入的变量**。

## 5. EL 表达式与 JSTL

### 5.1 EL（Expression Language）

```jsp
<%-- EL：${...} 简化取值（替代脚本）--%>
${user.name}                    <%-- 等价 <%= user.getName() %> --%>
${users.size()}                 <%-- 集合大小 --%>
${empty users ? "空" : users.size()}   <%-- 条件 --%>

<%-- 常见隐式对象 --%>
${param.id}                     <%-- 请求参数 --%>
${sessionScope.user}            <%-- 会话属性 --%>
```

### 5.2 JSTL（标准标签库）

```jsp
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<%-- 条件 --%>
<c:if test="${not empty user}">
    欢迎 ${user}
</c:if>

<%-- 循环 --%>
<c:forEach items="${users}" var="u">
    <tr><td>${u.id}</td><td>${u.name}</td></tr>
</c:forEach>
```

| JSTL 标签 | 用途 |
|-----------|------|
| `<c:if>` | 条件 |
| `<c:forEach>` | 循环 |
| `<c:choose>/<c:when>` | 多分支 |
| `<c:out>` | 输出（转义） |

> 💡 **JSTL + EL 是"不写脚本"的 JSP 正确用法**：逻辑用标签、取值用 EL——老项目中看到这种风格说明是"规范的老项目"。

## 6. 核心要点

> 🎯 **核心要点**：
> - JSP = 服务端模板（HTML + Java 片段，渲染后发纯 HTML）；
> - 脚本三元素：`<% %>`（逻辑）、`<%= %>`（输出）、`<%! %>`（声明）；
> - 三大指令：page（编码/错误页）、include（公共片段）、taglib（JSTL）；
> - 九大内置对象：重点 request/session/application（与 Servlet 对应）；
> - **现代 JSP 正确用法 = JSTL + EL（不写脚本）**；
> - 了解即可：看得懂、会排错，不深写。

## 7. 参考来源

- [Jakarta Pages（JSP）规范](https://jakarta.ee/specifications/pages/)
- [Jakarta Standard Tag Library（JSTL）](https://jakarta.ee/specifications/tags/)
- [Oracle JSP 教程（历史）](https://docs.oracle.com/javaee/5/tutorial/doc/bnagx.html)

---

**下一模块**：[02-JSP与Servlet的关系及MVC](02-JSP与Servlet的关系及MVC.md)　/　**返回总览**：[00-总览](00-JSP了解即可总览.md)
