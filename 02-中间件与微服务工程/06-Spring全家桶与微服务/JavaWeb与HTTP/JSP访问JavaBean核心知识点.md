# JSP 访问 JavaBean 核心知识点

> **文档定位**：Java 后端技术参考文档 | JSP + JavaBean 数据封装  
> **核心作用**：JavaBean 是遵循特定规范的 Java 类，用于封装数据和业务逻辑，在 JSP 中分离数据和视图，减少 JSP 中的 Java 代码，符合 MVC 设计思想  
> **核心规范**：无参构造 + 私有属性 + getter/setter 方法

---

## 目录

- [一、JavaBean 核心定义](#一javabean-核心定义)
- [二、JSP 访问 JavaBean 的核心标签](#二jsp-访问-javabean-的核心标签)
- [三、JavaBean 的作用域](#三javabean-的作用域)
- [四、JSP 访问 JavaBean 的两种方式](#四jsp-访问-javabean-的两种方式)
- [五、核心示例（Bookstore 场景）](#五核心示例bookstore-场景)
- [六、常见问题与注意事项](#六常见问题与注意事项)
- [七、核心总结](#七核心总结)

---

## 一、JavaBean 核心定义

**核心规范**：

| 规范 | 说明 |
|------|------|
| **无参构造** | 必须提供（默认构造器即可） |
| **属性私有化** | `private` 修饰，提供对应的 getter/setter 方法 |
| **实现 Serializable** | 可选，用于序列化场景 |

```java
package com.example;

import java.io.Serializable;

public class User implements Serializable {
    private String username;
    private Integer age;

    // 无参构造（必须）
    public User() {}

    // getter/setter
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
}
```

---

## 二、JSP 访问 JavaBean 的核心标签

### 1. `<jsp:useBean>` — 创建/获取 JavaBean 对象

```jsp
<jsp:useBean id="对象名" class="全类名" scope="作用域"/>
```

| 属性 | 说明 |
|------|------|
| `id` | 对象引用名，后续调用使用 |
| `class` | 全限定类名（如 `com.example.User`） |
| `scope` | 作用域：`page` / `request` / `session` / `application`（默认 `page`） |

```jsp
<jsp:useBean id="user" class="com.example.User" scope="session"/>
```

### 2. `<jsp:setProperty>` — 设置属性值

| 语法 | 说明 |
|------|------|
| `property="属性名" value="属性值"` | 直接赋值 |
| `property="属性名" param="请求参数名"` | 从请求参数取值 |
| `property="*"` | 自动匹配所有同名请求参数 |

```jsp
<jsp:setProperty name="user" property="username" value="张三"/>
<jsp:setProperty name="user" property="password" param="pwd"/>
<jsp:setProperty name="user" property="*"/>
```

### 3. `<jsp:getProperty>` — 获取属性值

```jsp
用户名：<jsp:getProperty name="user" property="username"/>
```

---

## 三、JavaBean 的作用域

| 作用域 | 有效范围 |
|--------|----------|
| **page** | 仅当前 JSP 页面有效，页面跳转后失效 |
| **request** | 一次请求有效，转发有效，重定向失效 |
| **session** | 一次会话有效，浏览器关闭前均有效 |
| **application** | 整个应用运行期间有效，服务器重启后失效 |

```jsp
<!-- 作用域为 session，整个会话中可使用 -->
<jsp:useBean id="user" class="com.example.User" scope="session"/>
```

---

## 四、JSP 访问 JavaBean 的两种方式

### 方式 1：标签方式（推荐 ✅）

```jsp
<jsp:useBean id="user" class="com.example.User" scope="page"/>
<jsp:setProperty name="user" property="username" value="李四"/>
<jsp:setProperty name="user" property="age" value="20"/>

<h3>姓名：<jsp:getProperty name="user" property="username"/></h3>
<h3>年龄：<jsp:getProperty name="user" property="age"/></h3>
```

### 方式 2：脚本方式（不推荐 ❌）

```jsp
<%
    com.example.User user = new com.example.User();
    user.setUsername("王五");
    user.setAge(25);
    request.setAttribute("user", user);
%>
<h3>姓名：<%= ((com.example.User)request.getAttribute("user")).getUsername() %></h3>
```

---

## 五、核心示例（Bookstore 场景）

### Book JavaBean

```java
package com.example;

public class Book {
    private Integer id;
    private String name;
    private String author;
    private Double price;

    // 无参构造（必须）
    public Book() {}

    // getter/setter
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
}
```

### JSP 中使用

```jsp
<jsp:useBean id="book" class="com.example.Book" scope="request"/>

<jsp:setProperty name="book" property="name" value="JavaWeb从入门到精通"/>
<jsp:setProperty name="book" property="author" value="张三"/>
<jsp:setProperty name="book" property="price" value="69.9"/>

<div>
    <h2>书籍名称：<jsp:getProperty name="book" property="name"/></h2>
    <p>作者：<jsp:getProperty name="book" property="author"/></p>
    <p>价格：¥<jsp:getProperty name="book" property="price"/></p>
</div>
```

---

## 六、常见问题与注意事项

| 问题 | 原因 | 解决方案 |
|------|------|----------|
| 找不到 JavaBean 类 | class 路径错误 | 确保全类名正确，class 文件在 `WEB-INF/classes` |
| 无参构造缺失 | JavaBean 没有无参构造 | 必须提供无参构造方法 |
| 属性名与方法名不匹配 | getter/setter 不符合规范 | 属性 `username` → `getUsername()` / `setUsername()` |
| 作用域选择错误 | 数据无法在不同页面间共享 | 按需选择 page/request/session/application |

> **最佳实践**：JavaBean 仅封装数据，业务逻辑放在 Servlet/Service 层。

---

## 七、核心总结

| 要点 | 说明 |
|------|------|
| JavaBean | 遵循规范的 Java 类（无参构造 + 私有属性 + getter/setter） |
| 核心标签 | `<jsp:useBean>` + `<jsp:setProperty>` + `<jsp:getProperty>` |
| 作用域 | page → request → session → application |
| MVC 分工 | Servlet 处理业务 → JavaBean 封装数据 → JSP 展示数据 |
