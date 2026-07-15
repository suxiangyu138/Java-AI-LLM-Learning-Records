# Spring MVC 零基础入门教程

> **定位**：Spring 框架的 Web 层框架，基于 MVC 模式。核心：`DispatcherServlet` 前端控制器 + `Controller` + 视图解析器。

---

## 目录

1. [环境搭建](#1-环境搭建)
2. [核心配置](#2-核心配置)
3. [第一个控制器](#3-第一个控制器)
4. [核心流程](#4-核心流程)

---

## 1. 环境搭建

### 依赖（pom.xml）

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-webmvc</artifactId>
        <version>5.3.20</version>
    </dependency>
    <dependency>
        <groupId>javax.servlet</groupId>
        <artifactId>javax.servlet-api</artifactId>
        <version>4.0.1</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

---

## 2. 核心配置

### web.xml（注册 DispatcherServlet）

```xml
<servlet>
    <servlet-name>dispatcherServlet</servlet-name>
    <servlet-class>org.springframework.web.servlet.DispatcherServlet</servlet-class>
    <init-param>
        <param-name>contextConfigLocation</param-name>
        <param-value>classpath:springmvc.xml</param-value>
    </init-param>
    <load-on-startup>1</load-on-startup>
</servlet>
<servlet-mapping>
    <servlet-name>dispatcherServlet</servlet-name>
    <url-pattern>/</url-pattern>
</servlet-mapping>
```

### springmvc.xml（三大核心配置）

```xml
<!-- 1. 注解扫描 -->
<context:component-scan base-package="com.controller"/>

<!-- 2. 注解驱动 -->
<mvc:annotation-driven/>

<!-- 3. 视图解析器 -->
<bean class="org.springframework.web.servlet.view.InternalResourceViewResolver">
    <property name="prefix" value="/WEB-INF/views/"/>
    <property name="suffix" value=".jsp"/>
</bean>
```

---

## 3. 第一个控制器

```java
@Controller
public class HelloController {
    @RequestMapping("/hello")
    public String hello(Model model) {
        model.addAttribute("msg", "Hello Spring MVC!");
        return "hello";  // → /WEB-INF/views/hello.jsp
    }
}
```

```jsp
<!-- /WEB-INF/views/hello.jsp -->
<h1>${msg}</h1>
```

> 访问 `http://localhost:8080/hello` → 显示 `Hello Spring MVC!`

---

## 4. 核心流程

```text
① 客户端请求 → DispatcherServlet 拦截
② → HandlerMapping 找到 Controller 方法
③ → Controller 处理 → Model 存数据 → 返回视图名
④ → ViewResolver 拼接视图路径（prefix + 视图名 + suffix）
⑤ → 渲染视图 → 返回给客户端
```
