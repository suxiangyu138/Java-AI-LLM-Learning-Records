# Spring Boot 整合 Thymeleaf（入门实战，避坑版）

> **文档定位**：Java 后端技术参考文档 | Spring Boot + Thymeleaf 模板引擎实战  
> **核心定义**：Thymeleaf 是一款现代化的 Java 模板引擎，Spring Boot 官方推荐，替代传统 JSP  
> **核心优势**：无需编译可直接预览 HTML、支持 Spring MVC Model 数据绑定、与 HTML 无缝融合  
> **前置条件**：Spring Boot 2.7.x + Spring Web

---

## 目录

- [一、引入依赖](#一引入依赖)
- [二、核心配置](#二核心配置)
- [三、基础语法](#三基础语法)
- [四、实战案例](#四实战案例)
- [五、常见问题及解决方案](#五常见问题及解决方案)
- [六、进阶补充](#六进阶补充)

---

## 一、引入依赖

```xml
<!-- Spring Boot Web（若已有则跳过） -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<!-- Thymeleaf 起步依赖 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-thymeleaf</artifactId>
</dependency>
```

> **注意**：无需指定版本，Spring Boot 父依赖统一管理。

---

## 二、核心配置

### application.properties

```properties
# 模板文件前缀（默认 classpath:/templates/）
spring.thymeleaf.prefix=classpath:/templates/
# 模板文件后缀（默认 .html）
spring.thymeleaf.suffix=.html
# 编码（必须配置，避免中文乱码）
spring.thymeleaf.encoding=UTF-8
# 模板模式
spring.thymeleaf.mode=HTML5
# 缓存：开发环境 false，生产环境 true
spring.thymeleaf.cache=false
# 响应头 Content-Type
spring.thymeleaf.servlet.content-type=text/html;charset=UTF-8
```

### application.yml（推荐）

```yaml
spring:
  thymeleaf:
    prefix: classpath:/templates/
    suffix: .html
    encoding: UTF-8
    mode: HTML5
    cache: false
    servlet:
      content-type: text/html;charset=UTF-8
```

### 关键配置说明

| 配置项 | 说明 | 避坑 |
|--------|------|------|
| **模板路径** | 默认 `src/main/resources/templates`，所有 `.html` 模板必须放这里 | 自定义路径需与配置一致 |
| **缓存** | 开发环境必须 `false`，否则修改模板后需重启才能生效 | 生产环境设为 `true` 提升性能 |
| **编码** | 必须配置 `UTF-8`，否则中文乱码 | 即使 HTML 文件是 UTF-8 也无效 |

---

## 三、基础语法

### 核心标签速查

| 标签 | 作用 | 示例 |
|------|------|------|
| `th:text` | 渲染文本（转义特殊字符） | `<p th:text="${name}">默认</p>` |
| `th:utext` | 渲染文本（不转义，渲染 HTML 片段） | `<div th:utext="${htmlContent}">` |
| `th:value` | 给表单元素赋值 | `<input th:value="${user.name}">` |
| `th:each` | 循环遍历集合 | `<li th:each="user : ${userList}" th:text="${user.name}">` |
| `th:if` | 条件判断 | `<p th:if="${age > 18}">成年</p>` |
| `th:href` | 设置超链接 | `<a th:href="@{/user/detail(id=${userId})}">详情</a>` |

### 语法规则

| 规则 | 说明 |
|------|------|
| 标签前缀 | 所有 Thymeleaf 标签以 `th:` 开头 |
| 数据绑定 | `${...}` 取值，对应 Model 中的 key |
| 路径跳转 | `@{...}` 构建 URL，参数拼接用 `(id=${val})` |
| 模板格式 | 必须是 `.html` 格式 |

---

## 四、实战案例

### 步骤 1：实体类

```java
package com.example.springboot.thymeleaf.entity;

public class User {
    private Integer id;
    private String name;
    private Integer age;
    private String address;

    // 必须提供无参构造、getter/setter
    public User() {}
    public User(Integer id, String name, Integer age, String address) {
        this.id = id;
        this.name = name;
        this.age = age;
        this.address = address;
    }
    // getter/setter...
}
```

### 步骤 2：Controller

```java
package com.example.springboot.thymeleaf.controller;

import com.example.springboot.thymeleaf.entity.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.ArrayList;
import java.util.List;

@Controller  // 注意：不是 @RestController
public class UserController {

    // 用户列表页
    @GetMapping("/user/list")
    public String userList(Model model) {
        List<User> userList = new ArrayList<>();
        userList.add(new User(1, "张三", 20, "北京"));
        userList.add(new User(2, "李四", 22, "上海"));
        userList.add(new User(3, "王五", 19, "广州"));

        model.addAttribute("userList", userList);
        model.addAttribute("title", "用户列表");
        return "user/list";  // 对应 templates/user/list.html
    }

    // 用户详情页
    @GetMapping("/user/detail")
    public String userDetail(Model model, Integer id) {
        User user = new User(id, "张三", 20, "北京");
        model.addAttribute("user", user);
        return "user/detail";  // 对应 templates/user/detail.html
    }
}
```

### 步骤 3：模板页面

**列表页面（templates/user/list.html）**：

```html
<!DOCTYPE html>
<html lang="zh-CN" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title th:text="${title}">用户列表</title>
</head>
<body>
    <h1 th:text="${title}">用户列表</h1>
    <table border="1" width="80%">
        <tr>
            <th>ID</th><th>姓名</th><th>年龄</th><th>地址</th><th>操作</th>
        </tr>
        <tr th:each="user : ${userList}">
            <td th:text="${user.id}">1</td>
            <td th:text="${user.name}">张三</td>
            <td th:text="${user.age}">20</td>
            <td th:text="${user.address}">北京</td>
            <td>
                <a th:href="@{/user/detail(id=${user.id})}">查看详情</a>
            </td>
        </tr>
    </table>
</body>
</html>
```

**详情页面（templates/user/detail.html）**：

```html
<!DOCTYPE html>
<html lang="zh-CN" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>用户详情</title>
</head>
<body>
    <h1>用户详情</h1>
    <p>ID：<span th:text="${user.id}">1</span></p>
    <p>姓名：<span th:text="${user.name}">张三</span></p>
    <p>年龄：<span th:text="${user.age}">20</span></p>
    <p>地址：<span th:text="${user.address}">北京</span></p>
    <a th:href="@{/user/list}">返回列表</a>
</body>
</html>
```

### 步骤 4：测试

| 步骤 | 操作 |
|------|------|
| 1 | 启动 Spring Boot 项目 |
| 2 | 访问 `http://localhost:8081/user/list` |
| 3 | 查看用户列表，点击"查看详情"跳转详情页 |
| 4 | 修改模板文件，刷新浏览器即可看到效果（缓存已关闭） |

---

## 五、常见问题及解决方案

| 问题 | 原因 | 解决方案 |
|------|------|----------|
| **标签不生效，显示默认文本** | 未添加 `xmlns:th`；Controller 用了 `@RestController`；Model key 与 `${}` 不一致 | 检查三处：xmlns、@Controller、key 大小写 |
| **中文乱码** | 未配置 `encoding=UTF-8`；HTML 文件编码不是 UTF-8 | 配置 encoding + IDEA 设置文件编码为 UTF-8 |
| **修改模板后不生效** | 缓存未关闭 | 检查 `spring.thymeleaf.cache=false` |
| **404 Not Found** | 模板路径错误 | 确认 return 值与文件路径对应、后缀为 `.html` |
| **循环/条件不生效** | 集合数据未存入 Model；语法错误；实体类无 getter | 检查 Model 数据、语法空格、实体类 getter |

---

## 六、进阶补充

| 方向 | 说明 |
|------|------|
| **模板布局** | `th:fragment` / `th:insert` / `th:replace` 实现页面复用（公共头部/底部） |
| **国际化** | 整合 Spring Boot 国际化 + Thymeleaf `#{}` 语法 |
| **模板片段** | 抽取公共页面部分（如导航栏），在其他页面引用 |
| **高级语法** | 三元运算、字符串拼接、日期格式化等 |

> **核心流程**：依赖 → 配置 → 语法 → 实战，先掌握基础，再逐步进阶。
