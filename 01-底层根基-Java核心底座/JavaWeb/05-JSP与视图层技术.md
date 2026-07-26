# 05 - JSP 与视图层技术

> JSP 虽然已不是主流前端方案，但理解其编译原理、九大内置对象、EL 表达式和 JSTL 标签库，对于维护遗留系统、深入理解 Servlet 机制以及对比现代模板引擎（Thymeleaf、FreeMarker）至关重要。

---

## 目录

1. [JSP 概述与运行原理](#1-jsp-概述与运行原理)
2. [JSP 生命周期与编译过程](#2-jsp-生命周期与编译过程)
3. [JSP 语法全解](#3-jsp-语法全解)
4. [JSP 九大内置对象](#4-jsp-九大内置对象)
5. [JSP 三大指令](#5-jsp-三大指令)
6. [JSP 动作标签](#6-jsp-动作标签)
7. [EL 表达式](#7-el-表达式)
8. [JSTL 标签库](#8-jstl-标签库)
9. [JSP 与前端分离的演进](#9-jsp-与前端分离的演进)
10. [常见面试题](#10-常见面试题)

---

## 1. JSP 概述与运行原理

### 1.1 什么是 JSP

> JSP（JavaServer Pages）是嵌入 Java 代码的 HTML 页面，本质是**运行在服务端的 Servlet**。它简化了动态 HTML 的生成——用标签代替 `out.write("<html>....")`。

```
┌──────────────┐    编译     ┌──────────────┐    运行    ┌──────────┐
│  index.jsp    │ ────────> │ index_jsp.java │ ────────> │ Servlet  │
│ (HTML+Java)  │           │ (Servlet 源码)  │          │ (响应HTML)│
└──────────────┘           └──────────────┘          └──────────┘
```

### 1.2 JSP 与 Servlet 的职责分离

| 维度 | Servlet | JSP |
|------|---------|-----|
| **角色** | Controller（控制器） | View（视图） |
| **擅长** | 处理业务逻辑、控制流程 | 渲染 HTML 页面 |
| **代码** | 纯 Java 代码 | HTML 为主，Java 为辅 |
| **类比** | Spring 的 @Controller | Thymeleaf 模板 |

> 🎯 MVC 经典分工：Servlet 处理请求 → 调用 Service → 将数据放入 Request → Forward 到 JSP 渲染。

### 1.3 JSP 的内容组成

```jsp
<%-- JSP 页面由以下元素组成 --%>

<%-- 1. 静态内容：HTML、CSS、JS（直接输出） --%>
<h1>用户列表</h1>

<%-- 2. JSP 指令：页面配置 --%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<%-- 3. 脚本元素：嵌入 Java 代码 --%>
<% List<User> users = (List<User>) request.getAttribute("users"); %>

<%-- 4. 表达式：输出值 --%>
<p>当前用户数：<%= users.size() %></p>

<%-- 5. 声明：定义成员变量和方法 --%>
<%! private int visitCount = 0; %>

<%-- 6. 动作标签：JSP 内置行为 --%>
<jsp:include page="header.jsp"/>

<%-- 7. EL 表达式：简洁取值（替代脚本） --%>
<p>欢迎，${sessionScope.currentUser.username}</p>

<%-- 8. JSTL 标签：流程控制和格式化 --%>
<c:forEach items="${users}" var="user">
    <tr><td>${user.name}</td><td>${user.email}</td></tr>
</c:forEach>
```

---

## 2. JSP 生命周期与编译过程

### 2.1 五个阶段

```
┌────────┐   ┌────────┐   ┌────────┐   ┌────────┐   ┌────────┐
│  翻译   │──>│  编译   │──>│ 类加载  │──>│  初始化  │──>│  服务   │
│ Translation│ Compilation│Loading│ Initialization│ Service │
└────────┘   └────────┘   └────────┘   └────────┘   └────────┘
  .jsp→.java   .java→.class   ClassLoader  jspInit()   _jspService()
```

### 2.2 编译后的 Servlet 源码剖析

```java
// index.jsp → 翻译后 → index_jsp.java（简化版）
public final class index_jsp extends org.apache.jasper.runtime.HttpJspBase
    implements org.apache.jasper.runtime.JspSourceDependent {

    // 成员变量（来自 <%! %> 声明）
    private int visitCount = 0;

    // JSP 内置对象声明
    // ...

    // 初始化
    public void _jspInit() { }

    // ★ 核心方法：每次请求调用
    public void _jspService(HttpServletRequest request,
                            HttpServletResponse response)
            throws java.io.IOException, ServletException {

        // 1. 获取内置对象
        PageContext pageContext = _jspxFactory.getPageContext(...);
        HttpSession session = pageContext.getSession();
        ServletContext application = pageContext.getServletContext();
        ServletConfig config = pageContext.getServletConfig();
        JspWriter out = pageContext.getOut();

        // 2. 逐行输出 HTML
        out.write("<html>\r\n");
        out.write("<head>\r\n");
        out.write("<title>用户列表</title>\r\n");

        // 3. 执行 Java 脚本
        List<User> users = (List<User>) request.getAttribute("users");
        for (User user : users) {
            // 4. 表达式输出
            out.write("<tr><td>");
            out.print(user.getName());  // <%= user.getName() %>
            out.write("</td></tr>\r\n");
        }

        out.write("</html>\r\n");
    }

    public void _jspDestroy() { }
}
```

> 💡 关键发现：`<% %>` 中的代码直接放入 `_jspService()` 方法体，因此**脚本中的变量是局部变量**（线程安全）。`<%! %>` 中的变量变为**成员变量**（线程不安全！）。

### 2.3 为什么第一次访问 JSP 慢？

```
首次访问流程：
  .jsp → 翻译 → .java → 编译 → .class → 加载 → 执行
         ↑ 这步耗时约占 90%

优化方案：
1. 预编译：部署前将所有 .jsp 编译好
2. load-on-startup：启动时编译关键页面
3. Tomcat 配置 development="false"（生产模式，减少检查）
```

---

## 3. JSP 语法全解

### 3.1 脚本元素

```jsp
<%-- 1. 脚本段（Scriptlet）— 在 _jspService() 方法体内执行 --%>
<%
    String name = request.getParameter("name");  // 局部变量，线程安全
    List<String> list = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
        list.add("item" + i);
    }
%>

<%-- 2. 表达式（Expression）— 输出值到页面 --%>
<p>用户名：<%= name %></p>           <%-- 等价于 out.print(name) — 末尾不能有分号 --%>
<p>当前时间：<%= new java.util.Date() %></p>

<%-- 3. 声明（Declaration）— 成员变量和方法 --%>
<%!
    private int count = 0;           // ⚠️ 成员变量，线程不安全！

    public int add(int a, int b) {   // 自定义方法
        return a + b;
    }
%>

<%-- 4. 注释 — 客户端不可见 --%>
<%-- 这是 JSP 注释，不会出现在 HTML 源码中 --%>
<!-- 这是 HTML 注释，会出现在 HTML 源码中 -->
```

### 3.2 三种注释对比

```jsp
<%-- JSP 注释：不会发送到客户端 --%>
<!-- HTML 注释：会发送到客户端，浏览器中可见 -->
<%
    // Java 单行注释：在脚本中使用
    /* Java 多行注释：在脚本中使用 */
%>
```

> ⚠️ 不要在 JSP 注释中暴露敏感信息！虽然客户端看不到，但页面源码仍可被开发者查看。

---

## 4. JSP 九大内置对象

### 4.1 九大对象速查

| 序号 | 对象 | 类型 | 域范围 | 说明 |
|------|------|------|--------|------|
| 1 | **request** | HttpServletRequest | request | 请求对象 |
| 2 | **response** | HttpServletResponse | page | 响应对象 |
| 3 | **session** | HttpSession | session | 会话对象 |
| 4 | **application** | ServletContext | application | 应用上下文 |
| 5 | **out** | JspWriter | page | 输出流对象 |
| 6 | **pageContext** | PageContext | page | 页面上下文（可获取其他8个对象） |
| 7 | **config** | ServletConfig | page | Servlet 配置 |
| 8 | **page** | Object（this） | page | 当前 JSP 页面对象 |
| 9 | **exception** | Throwable | page | 异常对象（仅 errorPage） |

### 4.2 四大域对象的作用范围

```jsp
<%-- pageContext：当前页面有效 --%>
<% pageContext.setAttribute("key", "value"); %>
<%= pageContext.getAttribute("key") %>

<%-- request：一次请求内有效（含 forward）--%>
<% request.setAttribute("key", "value"); %>
<%-- forward 转发后仍可获取，redirect 后丢失 --%>

<%-- session：一次会话内有效 --%>
<% session.setAttribute("key", "value"); %>
<%-- 同一用户的不同请求间共享 --%>

<%-- application：整个应用生命周期有效 --%>
<% application.setAttribute("key", "value"); %>
<%-- 所有用户共享，谨慎使用 --%>
```

### 4.3 pageContext — 万能对象

```jsp
<%-- pageContext 可以获取所有其他 8 个内置对象 --%>
<%
    pageContext.getRequest();     // → request
    pageContext.getResponse();    // → response
    pageContext.getSession();     // → session
    pageContext.getServletContext();  // → application
    pageContext.getOut();         // → out
    pageContext.getServletConfig();   // → config
    pageContext.getPage();        // → page
    pageContext.getException();   // → exception

    // pageContext 还可以在任意域中查找属性
    Object val = pageContext.findAttribute("key");
    // 查找顺序：page → request → session → application（找到即停）
%>
```

---

## 5. JSP 三大指令

### 5.1 page 指令

```jsp
<%@ page
    language="java"                    <%-- 脚本语言，默认 Java --%>
    contentType="text/html;charset=UTF-8" <%-- 响应 MIME 和编码 --%>
    pageEncoding="UTF-8"              <%-- 页面文件编码 --%>
    import="java.util.*,com.example.*"<%-- 导入类（逗号分隔，唯一可多次出现的属性）--%>
    session="true"                    <%-- 是否自动创建 Session，默认 true --%>
    isELIgnored="false"               <%-- 是否忽略 EL 表达式，默认 false --%>
    errorPage="/error.jsp"            <%-- 当前页面出错时的跳转页 --%>
    isErrorPage="false"               <%-- 是否是错误页面（true 才能用 exception 对象）--%>
    buffer="8kb"                      <%-- out 缓冲区大小 --%>
    autoFlush="true"                  <%-- 缓冲区满时自动刷新 --%>
    isThreadSafe="true"               <%-- ⚠️ 已废弃！不要使用 false --%>
%>
```

### 5.2 include 指令 vs 动作

```jsp
<%-- 静态包含（指令）— 编译时合并 --%>
<%@ include file="header.jsp" %>
<%-- 相当于将被包含文件的源码复制粘贴到当前位置
     优点：性能好
     缺点：被包含文件修改后，所有包含它的页面都需重新编译
          不能传递参数，变量可能冲突 --%>

<%-- 动态包含（动作）— 运行时合并 --%>
<jsp:include page="header.jsp">
    <jsp:param name="title" value="首页"/>
</jsp:include>
<%-- 生成两个独立的 Servlet，运行时调用
     优点：被包含文件修改后自动生效，可传递参数
     缺点：性能略低于静态包含 --%>
```

| 维度 | `<%@ include %>` 静态 | `<jsp:include/>` 动态 |
|------|----------------------|----------------------|
| **合并时机** | 编译期 | 运行期 |
| **生成文件** | 1 个 .java/.class | N 个各自独立 |
| **变量共享** | 可以（同一个类） | 不可以 |
| **参数传递** | 不支持 | 支持 `<jsp:param>` |
| **性能** | 较好 | 略低（额外调用开销） |
| **变更影响** | 被包含页修改 → 全部重编译 | 各自独立，无需重编译 |

### 5.3 taglib 指令

```jsp
<%-- 引入标签库 --%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<%-- 自定义标签库 --%>
<%@ taglib prefix="my" uri="/WEB-INF/mytags.tld" %>
```

---

## 6. JSP 动作标签

```jsp
<%-- 1. jsp:include — 动态包含（运行时调用） --%>
<jsp:include page="/fragments/header.jsp">
    <jsp:param name="pageTitle" value="用户管理"/>
</jsp:include>

<%-- 2. jsp:forward — 请求转发（与 RequestDispatcher.forward() 相同） --%>
<jsp:forward page="/result.jsp">
    <jsp:param name="status" value="success"/>
</jsp:forward>
<%-- forward 之后的代码不会执行 --%>

<%-- 3. jsp:param — 传递参数（配合 include/forward 使用） --%>
<jsp:param name="username" value="张三"/>

<%-- 4. jsp:useBean — 获取或创建 JavaBean --%>
<jsp:useBean id="user" class="com.example.User" scope="session"/>
<%-- 等价于：User user = session.getAttribute("user");
            if (user == null) { user = new User(); session.setAttribute("user", user); }
--%>

<%-- 5. jsp:setProperty — 设置 JavaBean 属性 --%>
<jsp:setProperty name="user" property="username" value="张三"/>
<%-- 自动类型转换：String → int/double/boolean 等 --%>
<jsp:setProperty name="user" property="*"/>
<%-- 星号匹配：自动将同名请求参数注入 Bean 属性 --%>

<%-- 6. jsp:getProperty — 获取 JavaBean 属性 --%>
<jsp:getProperty name="user" property="username"/>
<%-- 注意：要求 Bean 有对应的 getter 方法 --%>
```

> ⚠️ `<jsp:useBean>` 和 `<jsp:setProperty>` 是 JSP 1.x 时代的 MVC 模式实现，现代已基本不推荐使用。了解即可。

---

## 7. EL 表达式

### 7.1 什么是 EL

> EL（Expression Language）是 JSP 2.0 引入的表达式语言，**替代** `<%= %>` 脚本表达式，让页面更简洁。

```jsp
<%-- 传统脚本 — 繁琐且有 NullPointerException 风险 --%>
<%= ((User) session.getAttribute("user")).getName() %>

<%-- EL 表达式 — 简洁且 null-safe --%>
${sessionScope.user.name}
<%-- 如果 user 为 null，输出空字符串而非抛异常 --%>
```

### 7.2 EL 取值规则

```jsp
<%-- EL 从四个域中按顺序查找属性 --%>
${username}
<%-- 查找顺序：pageScope → requestScope → sessionScope → applicationScope --%>
<%-- 等价于 pageContext.findAttribute("username") --%>

<%-- 显式指定域（推荐，避免歧义） --%>
${pageScope.username}       <%-- pageContext 域 --%>
${requestScope.username}    <%-- request 域 --%>
${sessionScope.user}        <%-- session 域 --%>
${applicationScope.config}  <%-- application 域 --%>

<%-- 访问对象属性（通过 getter） --%>
${user.name}             <%-- → user.getName() --%>
${user.address.city}     <%-- → user.getAddress().getCity() — 支持级联 --%>

<%-- 访问数组和集合 --%>
${list[0]}               <%-- List/数组按索引 --%>
${map["key"]}            <%-- Map 按键获取 --%>
${map.key}               <%-- 同上，key 为合法标识符时可用 --%>
```

### 7.3 EL 运算符

```jsp
<%-- 算术运算符 --%>
${10 + 5}    ${10 - 5}    ${10 * 5}    ${10 / 5}    ${10 % 3}

<%-- 比较运算符 --%>
${10 == 10}  ${10 eq 10}   <%-- 相等（推荐 eq 写法）--%>
${10 != 5}   ${10 ne 5}    <%-- 不等 --%>
${10 > 5}    ${10 gt 5}    <%-- 大于 --%>
${10 >= 5}   ${10 ge 5}    <%-- 大于等于 --%>
${5 < 10}    ${5 lt 10}    <%-- 小于 --%>
${5 <= 10}   ${5 le 10}    <%-- 小于等于 --%>

<%-- 逻辑运算符 --%>
${true && false}  ${true and false}  <%-- 与 --%>
${true || false}  ${true or false}   <%-- 或 --%>
${!true}          ${not true}        <%-- 非 --%>

<%-- 三元运算符 --%>
${user != null ? user.name : "匿名用户"}

<%-- empty 运算符 — 判空神器 --%>
${empty list}       <%-- null 或 空集合/空字符串 → true --%>
${not empty list}   <%-- 不为空 → true --%>
```

### 7.4 EL 隐含对象

```jsp
<%-- 与 JSP 内置对象对应的隐含对象 --%>
${pageContext.request.contextPath}  <%-- 获取上下文路径 /app --%>
${pageContext.request.remoteAddr}   <%-- 客户端 IP --%>
${pageContext.session.id}           <%-- Session ID --%>
${pageContext.session.maxInactiveInterval}

<%-- 请求参数 — 可替代 request.getParameter() --%>
${param.username}           <%-- 单个参数值 --%>
${paramValues.hobby[0]}     <%-- 多值参数 --%>

<%-- 请求头 — 可替代 request.getHeader() --%>
${header["User-Agent"]}     <%-- 含特殊字符用 [] --%>
${headerValues["Accept-Language"]} <%-- 多值请求头 --%>

<%-- Cookie — 直接获取 Cookie 值 --%>
${cookie.JSESSIONID.value}

<%-- 上下文初始化参数 --%>
${initParam.appName}        <%-- web.xml 中 context-param --%>
```

---

## 8. JSTL 标签库

### 8.1 JSTL 五大库

| 库 | URI | 前缀 | 功能 |
|----|-----|------|------|
| Core（核心） | `.../jstl/core` | `c` | 流程控制、循环、URL |
| Format（格式化） | `.../jstl/fmt` | `fmt` | 日期、数字、国际化 |
| SQL | `.../jstl/sql` | `sql` | ❌ 废弃（不应在 JSP 中操作 DB） |
| XML | `.../jstl/xml` | `x` | XML 解析 |
| Functions（函数） | `.../jstl/functions` | `fn` | 字符串处理 |

### 8.2 Core 标签库

```jsp
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%-- c:set — 设置变量 --%>
<c:set var="username" value="张三" scope="request"/>

<%-- c:out — 输出（自动转义 HTML，防 XSS）--%>
<c:out value="${user.bio}" default="暂无简介"/>
<%-- escapeXml="false" 可关闭转义（危险！）--%>

<%-- c:if — 条件判断（没有 else！）--%>
<c:if test="${not empty user}">
    <p>欢迎，${user.name}</p>
</c:if>

<%-- c:choose/when/otherwise — if-else if-else --%>
<c:choose>
    <c:when test="${score >= 90}">优秀</c:when>
    <c:when test="${score >= 80}">良好</c:when>
    <c:when test="${score >= 60}">及格</c:when>
    <c:otherwise>不及格</c:otherwise>
</c:choose>

<%-- c:forEach — 循环遍历 --%>
<c:forEach items="${users}" var="user" varStatus="status">
    <tr>
        <td>${status.index + 1}</td>   <%-- 0-based 索引 --%>
        <td>${status.count}</td>       <%-- 1-based 计数 --%>
        <td>${user.name}</td>
        <td>${user.email}</td>
        <td>${status.first ? '首行' : ''}</td>
        <td>${status.last ? '末行' : ''}</td>
    </tr>
</c:forEach>

<%-- c:forEach — 纯数字循环 --%>
<c:forEach begin="1" end="10" step="2" var="i">
    ${i}  <%-- 输出 1,3,5,7,9 --%>
</c:forEach>

<%-- c:forTokens — 字符串分割遍历 --%>
<c:forTokens items="Java,Python,Go" delims="," var="lang">
    <li>${lang}</li>
</c:forTokens>

<%-- c:url — 生成带上下文路径和 URL 重写的 URL --%>
<c:url value="/user/detail" var="userUrl">
    <c:param name="id" value="${user.id}"/>
</c:url>
<a href="${userUrl}">查看详情</a>
<%-- 自动添加 Context Path 和 jsessionid（如需要）--%>

<%-- c:redirect — 重定向 --%>
<c:redirect url="/login"/>
```

### 8.3 Format 标签库

```jsp
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<%-- 日期格式化 --%>
<fmt:formatDate value="${user.createTime}" pattern="yyyy-MM-dd HH:mm:ss"/>

<%-- 数字格式化 --%>
<fmt:formatNumber value="${price}" type="currency" currencyCode="CNY"/>
<%-- 输出：￥1,234.56 --%>

<fmt:formatNumber value="${ratio}" type="percent" maxFractionDigits="2"/>
<%-- 输出：85.50% --%>

<%-- 设置本地化 --%>
<fmt:setLocale value="zh_CN"/>
<fmt:setBundle basename="messages"/>
<fmt:message key="welcome.title"/>
```

### 8.4 Functions 标签库

```jsp
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

${fn:length(list)}               <%-- 集合大小/字符串长度 --%>
${fn:contains(str, "Java")}     <%-- 是否包含子串 --%>
${fn:startsWith(str, "pre")}    <%-- 是否以...开头 --%>
${fn:endsWith(str, "fix")}      <%-- 是否以...结尾 --%>
${fn:substring(str, 0, 5)}      <%-- 截取子串 --%>
${fn:split(str, ",")}           <%-- 分割为数组 --%>
${fn:join(array, "-")}          <%-- 数组用分隔符连接 --%>
${fn:toUpperCase(str)}          <%-- 转大写 --%>
${fn:toLowerCase(str)}          <%-- 转小写 --%>
${fn:trim(str)}                 <%-- 去除首尾空格 --%>
${fn:replace(str, "old", "new")} <%-- 替换 --%>
${fn:escapeXml(html)}           <%-- XML/HTML 转义 --%>
```

---

## 9. JSP 与前端分离的演进

### 9.1 JSP 的衰落原因

| 问题 | 说明 |
|------|------|
| **前后端耦合** | 前端开发者需要懂 JSP/Java，后端需要写 HTML |
| **难以调试** | 错误信息不友好，混合代码难以定位 |
| **性能瓶颈** | 服务端渲染消耗服务器资源 |
| **不利于 SPA** | Ajax + MVVM 框架（Vue/React）天然分离 |
| **无法移动端复用** | 移动端需要 JSON API，非 HTML |

### 9.2 演进路径

```
JSP / JSTL / EL
  │  (传统服务端渲染)
  ▼
Thymeleaf / FreeMarker / Velocity
  │  (更现代的模板引擎，Spring Boot 默认 Thymeleaf)
  │  (仍为服务端渲染，但语法更优雅)
  ▼
前后端分离
  │  (Vue / React + RESTful API / GraphQL)
  │  (服务端只返回 JSON，前端负责渲染)
  ▼
SSR / SSG (Next.js / Nuxt.js)
  │  (首屏性能优化 + SEO)
  ▼
全栈框架 (Next.js / Remix / Spring Boot + HTMX)
```

### 9.3 Thymeleaf 速览（JSP 的现代替代）

```html
<!-- Thymeleaf — Spring Boot 默认模板引擎 -->
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title th:text="${title}">默认标题</title>
    <!-- 可以直接在浏览器中预览，th:text 运行时替换 -->
</head>
<body>
    <h1 th:text="${title}">标题</h1>

    <!-- 循环 -->
    <tr th:each="user : ${users}">
        <td th:text="${userStat.count}">1</td>
        <td th:text="${user.name}">张三</td>
        <td th:text="${user.email}">zhangsan@example.com</td>
    </tr>

    <!-- 条件判断 -->
    <div th:if="${not #lists.isEmpty(users)}">
        <p th:text="'共' + ${users.size()} + '个用户'">共0个用户</p>
    </div>

    <!-- URL 构建 -->
    <a th:href="@{/user/{id}(id=${user.id})}">查看</a>
</body>
</html>
```

> 🎯 为什么 Spring Boot 选 Thymeleaf 而非 JSP？Thymeleaf 是纯 HTML（浏览器可预览），天然支持 SpringEL，不依赖 Servlet 容器即可渲染。

---

## 10. 常见面试题

### Q1：JSP 和 Servlet 的关系？

> JSP 本质是 Servlet——首次访问时 Tomcat 将 .jsp 翻译为 .java（继承 HttpJspBase）→ 编译为 .class → 运行。JSP 擅长视图渲染（HTML），Servlet 擅长流程控制（Java）。详见第1-2节。

### Q2：JSP 九大内置对象是什么？

> request、response、session、application、out、pageContext、config、page、exception。pageContext 是万能对象，可获取其他 8 个。详见第4节。

### Q3：`<%@ include %>` 和 `<jsp:include>` 的区别？

> 静态包含（指令）：编译时合并源码，生成一个类。动态包含（动作）：运行时调用，各自独立类。详见第5.2节。

### Q4：EL 表达式的查找顺序？如何避免歧义？

> pageScope → requestScope → sessionScope → applicationScope。推荐显式指定域：`${requestScope.user.name}`。详见第7.2节。

### Q5：JSP 为什么被前后端分离替代？

> 前后端耦合（开发效率低）、页面渲染占用服务器资源、不利于多端（移动端/App）复用、调试困难。详见第9节。
