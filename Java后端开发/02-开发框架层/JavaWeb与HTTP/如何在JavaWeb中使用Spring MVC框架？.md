# 如何在 JavaWeb 中使用 Spring MVC 框架

> **文档定位**：Java 后端技术参考文档 | Spring MVC 框架完整使用指南  
> **核心定义**：Spring MVC 是 Spring 框架的 Web 模块，基于 MVC 设计模式，封装了原生 Servlet  
> **核心入口**：`DispatcherServlet`（前端控制器）  
> **版本说明**：含传统 XML 配置版 + Spring Boot 简化版

---

## 目录

- [一、Spring MVC 核心概念](#一spring-mvc-核心概念)
- [二、环境准备](#二环境准备)
- [三、Spring MVC 核心配置](#三spring-mvc-核心配置)
- [四、核心开发步骤](#四核心开发步骤)
- [五、核心功能使用](#五核心功能使用)
- [六、Spring Boot 整合 Spring MVC](#六spring-boot-整合-spring-mvc)
- [七、总结](#七总结)

---

## 一、Spring MVC 核心概念

### MVC 角色

| 角色 | 职责 | Spring MVC 对应 |
|------|------|----------------|
| **Controller** | 处理请求，调用业务逻辑 | `@Controller` / `@RestController` |
| **Model** | 封装数据 | POJO、`Map`、`ModelAndView` |
| **View** | 展示数据 | JSP、Thymeleaf、HTML |

### 核心执行流程

```
浏览器请求 → DispatcherServlet（前端控制器）
→ HandlerMapping 找到对应 Controller 方法
→ HandlerAdapter 执行方法
→ Controller 返回 ModelAndView
→ ViewResolver 解析视图
→ 渲染并返回响应
```

---

## 二、环境准备

### Maven 依赖

```xml
<!-- Spring MVC 核心 -->
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-webmvc</artifactId>
    <version>5.3.30</version>
</dependency>

<!-- Servlet API -->
<dependency>
    <groupId>javax.servlet</groupId>
    <artifactId>javax.servlet-api</artifactId>
    <version>4.0.1</version>
    <scope>provided</scope>
</dependency>

<!-- JSON 解析 -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.15.2</version>
</dependency>
```

### web.xml 配置 DispatcherServlet

```xml
<web-app xmlns="http://xmlns.jcp.org/xml/ns/javaee" version="4.0">

    <!-- Spring MVC 前端控制器 -->
    <servlet>
        <servlet-name>dispatcherServlet</servlet-name>
        <servlet-class>org.springframework.web.servlet.DispatcherServlet</servlet-class>
        <init-param>
            <param-name>contextConfigLocation</param-name>
            <param-value>classpath:spring-mvc.xml</param-value>
        </init-param>
        <load-on-startup>1</load-on-startup>
    </servlet>
    <servlet-mapping>
        <servlet-name>dispatcherServlet</servlet-name>
        <url-pattern>/</url-pattern>
    </servlet-mapping>

    <!-- 字符编码过滤器 -->
    <filter>
        <filter-name>encodingFilter</filter-name>
        <filter-class>org.springframework.web.filter.CharacterEncodingFilter</filter-class>
        <init-param>
            <param-name>encoding</param-name>
            <param-value>UTF-8</param-value>
        </init-param>
    </filter>
    <filter-mapping>
        <filter-name>encodingFilter</filter-name>
        <url-pattern>/*</url-pattern>
    </filter-mapping>
</web-app>
```

---

## 三、Spring MVC 核心配置（spring-mvc.xml）

```xml
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:context="http://www.springframework.org/schema/context"
       xmlns:mvc="http://www.springframework.org/schema/mvc">

    <!-- 1. 扫描控制器包 -->
    <context:component-scan base-package="com.example.controller"/>

    <!-- 2. 开启 MVC 注解驱动 -->
    <mvc:annotation-driven/>

    <!-- 3. 视图解析器（JSP） -->
    <bean class="org.springframework.web.servlet.view.InternalResourceViewResolver">
        <property name="prefix" value="/WEB-INF/views/"/>
        <property name="suffix" value=".jsp"/>
    </bean>

    <!-- 4. 静态资源放行 -->
    <mvc:default-servlet-handler/>
</beans>
```

---

## 四、核心开发步骤

### 1. 实体类（Model）

```java
package com.example.entity;

public class User {
    private Long id;
    private String username;
    private Integer age;

    public User() {}
    public User(Long id, String username, Integer age) {
        this.id = id;
        this.username = username;
        this.age = age;
    }
    // getter/setter...
}
```

### 2. 控制器 — 返回视图（传统 MVC）

```java
import com.example.entity.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import java.util.List;

@Controller
@RequestMapping("/user")
public class UserController {

    @GetMapping("/list")
    public String getUserList(Model model) {
        List<User> userList = List.of(
            new User(1L, "张三", 20),
            new User(2L, "李四", 25)
        );
        model.addAttribute("userList", userList);
        // 视图解析器拼接：/WEB-INF/views/user/list.jsp
        return "user/list";
    }
}
```

### 3. 控制器 — 返回 JSON（RESTful 风格）

```java
import com.example.entity.User;
import org.springframework.web.bind.annotation.*;
import java.util.List;

// @RestController = @Controller + @ResponseBody
@RestController
@RequestMapping("/api/user")
public class UserApiController {

    @GetMapping("/all")
    public List<User> getAllUsers() {
        return List.of(
            new User(1L, "张三", 20),
            new User(2L, "李四", 25)
        );
    }

    @GetMapping("/{id}")
    public User getUserById(@PathVariable Long id) {
        return new User(id, "张三", 20);
    }
}
```

### 4. JSP 视图

```jsp
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
<head><title>用户列表</title></head>
<body>
    <h1>用户列表</h1>
    <table border="1">
        <tr><th>ID</th><th>用户名</th><th>年龄</th></tr>
        <c:forEach items="${userList}" var="user">
            <tr>
                <td>${user.id}</td>
                <td>${user.username}</td>
                <td>${user.age}</td>
            </tr>
        </c:forEach>
    </table>
</body>
</html>
```

---

## 五、核心功能使用

### 1. 请求参数绑定

| 方式 | 代码 | 请求示例 |
|------|------|----------|
| **基本类型** | `queryUser(String username, Integer age)` | `/query?username=张三&age=20` |
| **对象封装** | `saveUser(User user)` | `/save?username=张三&age=20` |
| **JSON 请求体** | `createUser(@RequestBody User user)` | `POST` body: `{"username":"张三"}` |
| **数组参数** | `batchQuery(Long[] ids)` | `/batch?ids=1&ids=2&ids=3` |
| **路径参数** | `getUserById(@PathVariable Long id)` | `/api/user/1` |

### 2. 文件上传

#### 配置（spring-mvc.xml）

```xml
<bean id="multipartResolver" class="org.springframework.web.multipart.commons.CommonsMultipartResolver">
    <property name="maxUploadSize" value="10485760"/>  <!-- 10MB -->
    <property name="defaultEncoding" value="UTF-8"/>
</bean>
```

#### Maven 依赖

```xml
<dependency>
    <groupId>commons-fileupload</groupId>
    <artifactId>commons-fileupload</artifactId>
    <version>1.4</version>
</dependency>
```

#### 上传控制器

```java
@RestController
@RequestMapping("/upload")
public class UploadController {

    @PostMapping("/file")
    public String uploadFile(@RequestParam("file") MultipartFile file, 
                             HttpServletRequest request) throws IOException {
        if (file.isEmpty()) return "文件为空";

        String fileName = file.getOriginalFilename();
        String uploadPath = request.getServletContext().getRealPath("/uploads/");
        File pathFile = new File(uploadPath);
        if (!pathFile.exists()) pathFile.mkdirs();

        file.transferTo(new File(uploadPath + fileName));
        return "上传成功：" + fileName;
    }
}
```

### 3. 全局异常处理

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public Map<String, Object> handleRuntimeException(RuntimeException e) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 500);
        result.put("msg", "系统异常：" + e.getMessage());
        result.put("data", null);
        return result;
    }

    @ExceptionHandler(BusinessException.class)
    public Map<String, Object> handleBusinessException(BusinessException e) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", e.getCode());
        result.put("msg", e.getMessage());
        return result;
    }
}
```

### 4. 拦截器

```java
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, 
                             HttpServletResponse response, Object handler) throws Exception {
        Object user = request.getSession().getAttribute("user");
        if (user == null) {
            response.sendRedirect("/login.jsp");
            return false;  // 拦截
        }
        return true;  // 放行
    }
}
```

```xml
<!-- spring-mvc.xml 配置拦截器 -->
<mvc:interceptors>
    <mvc:interceptor>
        <mvc:mapping path="/**"/>
        <mvc:exclude-mapping path="/login.jsp"/>
        <mvc:exclude-mapping path="/api/login"/>
        <bean class="com.example.interceptor.LoginInterceptor"/>
    </mvc:interceptor>
</mvc:interceptors>
```

---

## 六、Spring Boot 整合 Spring MVC

> Spring Boot 整合后可省略 `web.xml` 和 `spring-mvc.xml`，通过自动配置快速开发。

### 依赖

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>2.7.15</version>
</parent>

<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
</dependencies>
```

### 启动类

```java
@SpringBootApplication
public class SpringMvcDemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpringMvcDemoApplication.class, args);
    }
}
```

### 控制器（无需额外配置）

```java
@RestController
public class HelloController {
    @GetMapping("/hello")
    public String hello() {
        return "Hello Spring MVC + Spring Boot!";
    }
}
```

### application.properties

```properties
server.port=8080
server.servlet.context-path=/demo
spring.mvc.view.prefix=/WEB-INF/views/
spring.mvc.view.suffix=.jsp
```

---

## 七、总结

| 维度 | 核心要点 |
|------|----------|
| **核心入口** | `DispatcherServlet`（前端控制器） |
| **核心注解** | `@Controller` / `@RestController`、`@RequestMapping` / `@GetMapping` / `@PostMapping` |
| **开发步骤** | 配置 DispatcherServlet → 编写 MVC 配置文件 → 开发 Controller → 编写视图 / 返回 JSON |
| **参数绑定** | 基本类型、对象封装、`@RequestBody`（JSON）、`@PathVariable`（路径参数） |
| **进阶功能** | 文件上传、全局异常处理、拦截器 |
| **主流方式** | Spring Boot + `spring-boot-starter-web`（零 XML 配置） |
