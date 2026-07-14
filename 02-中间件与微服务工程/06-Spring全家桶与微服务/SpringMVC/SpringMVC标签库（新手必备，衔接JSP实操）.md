# Spring MVC 标签库（新手必备，衔接 JSP 实操）

> **文档定位**：Java 后端技术参考文档 | Spring MVC 标签库使用指南  
> **核心作用**：在 JSP 视图层简化数据渲染、表单提交、请求跳转等操作，无需编写 Java 脚本  
> **衔接知识**：Controller 传递 Model 数据 → JSP 通过标签库渲染 → 形成完整 MVC 链路  
> **技术栈**：Spring 5.x + JSP + JSTL

---

## 目录

- [一、标签库核心前提](#一标签库核心前提)
- [二、核心标签库（prefix="c"）](#二核心标签库prefixc)
- [三、表单标签库（prefix="form"）](#三表单标签库prefixform)
- [四、新手避坑重点](#四新手避坑重点)
- [五、实操总结](#五实操总结)

---

## 一、标签库核心前提

使用标签库前，必须在 JSP 页面头部导入标签库：

```jsp
<%-- 核心标签库（必导入）：数据渲染、循环、条件判断、跳转 --%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%-- 表单标签库（表单场景必导入）：自动绑定模型数据到表单元素 --%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
```

| 标签库 | prefix | 用途 |
|--------|--------|------|
| **核心标签库** | `c` | 数据渲染、循环遍历、条件判断、URL 跳转 |
| **表单标签库** | `form` | 自动绑定 Model 数据到表单元素 |

---

## 二、核心标签库（prefix="c"）

### 1. `<c:out>` — 输出模型数据

| 属性 | 说明 |
|------|------|
| `value` | 模型数据（必填），格式 `${模型key}` |
| `default` | 默认值（可选），数据为 null 时显示 |
| `escapeXml` | 是否转义 HTML 标签（默认 `true`） |

```jsp
<p>用户ID：<c:out value="${user.id}" default="无"/></p>
<p>用户姓名：<c:out value="${user.name}" default="无"/></p>

<!-- escapeXml=false 可以让 <br/> 生效 -->
<p><c:out value="${user.name}<br/>性别：${user.gender}" escapeXml="false"/></p>
```

### 2. `<c:forEach>` — 循环遍历集合

| 属性 | 说明 |
|------|------|
| `items` | 集合（必填），格式 `${集合key}` |
| `var` | 元素变量名（必填） |
| `varStatus` | 循环状态对象（可选），含 `index`（从 0）/ `count`（从 1） |

```jsp
<table border="1">
    <tr><th>序号</th><th>姓名</th><th>年龄</th><th>性别</th></tr>
    <c:forEach items="${userList}" var="u" varStatus="status">
        <tr>
            <td>${status.count}</td>       <!-- 序号，从 1 开始 -->
            <td>${u.name}</td>
            <td>${u.age}</td>
            <td>${u.gender}</td>
        </tr>
    </c:forEach>
</table>
```

### 3. `<c:if>` — 条件判断

| 属性 | 说明 |
|------|------|
| `test` | 判断条件（必填），EL 表达式返回 boolean |

```jsp
<c:if test="${u.gender == '男'}">
    <font color="blue">${u.gender}</font>
</c:if>
<c:if test="${u.gender == '女'}">
    <font color="pink">${u.gender}</font>
</c:if>
```

### 4. `<c:redirect>` — 请求重定向

```jsp
<!-- 重定向到 Controller 请求路径 -->
<c:redirect url="/user/list"/>

<!-- 也可重定向到 JSP 页面 -->
<%-- <c:redirect url="/userDetail.jsp"/> --%>
```

---

## 三、表单标签库（prefix="form"）

### `<form:form>` — 表单核心

| 属性 | 说明 |
|------|------|
| `modelAttribute` | 绑定的模型数据名称（必填），与 Controller 中 `model.addAttribute("key", value)` 的 key 一致 |
| `action` | 表单提交路径 |
| `method` | 请求方法（默认 POST） |

### 表单元素标签

| 标签 | 对应 HTML | 说明 |
|------|----------|------|
| `<form:input path="属性名">` | `<input type="text">` | 自动绑定模型属性值 |
| `<form:password path="属性名">` | `<input type="password">` | 密码输入框 |
| `<form:radiobutton path="属性名" value="值" label="文本">` | `<input type="radio">` | 单选框 |
| `<form:select path="属性名">` | `<select>` | 下拉选择框 |
| `<form:textarea path="属性名">` | `<textarea>` | 多行文本 |
| `<form:button>` | `<button>` | 提交按钮 |

### 实战：新增/修改用户表单

**Controller**：

```java
// 新增用户：传递空 User 对象
@GetMapping("/toAdd")
public String toAdd(Model model) {
    model.addAttribute("user", new User());
    return "addUser";
}

// 修改用户：传递已有 User 对象（表单自动回显数据）
@GetMapping("/toUpdate/{id}")
public String toUpdate(@PathVariable Integer id, Model model) {
    User user = new User("张三", 20, "男");
    user.setId(id);
    model.addAttribute("user", user);
    return "updateUser";
}
```

**JSP 表单页面（addUser.jsp）**：

```jsp
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<html>
<head><title>新增用户</title></head>
<body>
    <h1>新增用户</h1>
    <form:form modelAttribute="user" action="/user/add" method="post">
        <p>姓名：<form:input path="name" placeholder="请输入姓名"/></p>
        <p>年龄：<form:input path="age" placeholder="请输入年龄"/></p>
        <p>
            性别：
            <form:radiobutton path="gender" value="男" label="男"/>
            <form:radiobutton path="gender" value="女" label="女"/>
        </p>
        <p><form:button type="submit">新增用户</form:button></p>
    </form:form>
</body>
</html>
```

> **说明**：修改页面与新增页面一致，区别是 Controller 传递已有 User 对象，表单自动回显属性值。

---

## 四、新手避坑重点

| 坑 | 原因 | 解决 |
|----|------|------|
| **标签无法识别** | 未导入标签库 | JSP 头部添加 `<%@ taglib %>` |
| **模型数据不显示** | `value`/`path` 与 Model key 不一致 | 确保标签属性与 `model.addAttribute("key")` 一致 |
| **`<form:form>` 报错** | 未指定 `modelAttribute` 或模型数据不存在 | 新增时传空对象，修改时传已有对象 |
| **EL 表达式无效** | JSP 页面关闭了 EL | 添加 `<%@ page isELIgnored="false" %>` |
| **重定向 404** | url 未加 `/` 前缀 | 跳转 Controller 路径需加 `/` |
| **`<c:forEach>` 报错** | items 不是集合类型 | 确保传递的是 `List` 而非单个对象 |

---

## 五、实操总结

### 场景适配

| 场景 | 使用标签 |
|------|----------|
| **数据展示**（列表、详情） | `<c:out>` 单值输出、`<c:forEach>` 遍历集合、`<c:if>` 条件渲染 |
| **表单提交**（新增、修改） | `<form:form>` 绑定模型 + 表单元素标签 |
| **页面跳转** | `<c:redirect>` 重定向 |

### 完整链路

```
Controller（处理请求）→ Model（传递数据）→ JSP（标签库渲染）→ 浏览器（展示页面）
```

> **补充**：前后端分离开发中 JSP 已逐渐被 Vue/React 替代，但掌握标签库用法有助于理解"视图与数据绑定"的逻辑。
