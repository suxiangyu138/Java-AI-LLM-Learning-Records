03.13 17:03
Spring MVC 零基础入门教程

一、核心概念
Spring MVC 是 Spring 框架的Web 层框架，基于 MVC（Model-View-Controller）设计模式，用于简化 Java Web 应用开发，核心作用是接收前端请求、处理业务逻辑、返回响应数据。
二、环境准备
1. 确保已安装 JDK 8 及以上、中文版 IntelliJ IDEA、Maven（IDEA 自带可直接使用）
2. 新建 Maven 项目
- 打开 IDEA → 点击文件 → 新建 → 项目
- 选择 Maven → 取消勾选从模板创建 → 点击下一步
- 填写项目名称（如  SpringMvcDemo ）、存储路径 → 点击完成
三、添加依赖（pom.xml）
在 pom.xml 中添加 Spring MVC 核心依赖，Maven 会自动下载jar包：
xml
<dependencies>
    <!-- Spring MVC 核心依赖 -->
    <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-webmvc</artifactId>
        <version>5.3.20</version>
    </dependency>
    <!-- Servlet 依赖 -->
    <dependency>
        <groupId>javax.servlet</groupId>
        <artifactId>javax.servlet-api</artifactId>
        <version>4.0.1</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
 
添加后点击 IDEA 右上角的 刷新 按钮，导入依赖。
四、配置 Spring MVC 核心文件
1. 配置 web.xml（注册前端控制器）
在  src/main/webapp/WEB-INF  下新建  web.xml （若没有 webapp 文件夹，右键项目 → 添加框架支持 → 勾选 Web 应用程序）
xml
<?xml version="1.0" encoding="UTF-8"?>
<web-app xmlns="http://xmlns.jcp.org/xml/ns/javaee"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/web-app_4_0.xsd"
         version="4.0">
    <!-- 前端控制器：所有请求都先经过此Servlet -->
    <servlet>
        <servlet-name>dispatcherServlet</servlet-name>
        <servlet-class>org.springframework.web.servlet.DispatcherServlet</servlet-class>
        <!-- 加载 Spring MVC 配置文件 -->
        <init-param>
            <param-name>contextConfigLocation</param-name>
            <param-value>classpath:springmvc.xml</param-value>
        </init-param>
        <!-- 启动时加载 -->
        <load-on-startup>1</load-on-startup>
    </servlet>
    <servlet-mapping>
        <servlet-name>dispatcherServlet</servlet-name>
        <!-- 拦截所有请求 -->
        <url-pattern>/</url-pattern>
    </servlet-mapping>
</web-app>
 
2. 配置 springmvc.xml（核心配置）
在  src/main/resources  下新建  springmvc.xml 
xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:mvc="http://www.springframework.org/schema/mvc"
       xmlns:context="http://www.springframework.org/schema/context"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
       http://www.springframework.org/schema/beans/spring-beans.xsd
       http://www.springframework.org/schema/mvc
       http://www.springframework.org/schema/mvc/spring-mvc.xsd
       http://www.springframework.org/schema/context
       http://www.springframework.org/schema/context/spring-context.xsd">
    <!-- 1. 开启注解扫描：扫描控制器所在包 -->
    <context:component-scan base-package="com.controller"/>
    <!-- 2. 开启 Spring MVC 注解驱动 -->
    <mvc:annotation-driven/>
    <!-- 3. 配置视图解析器：解析 jsp 视图 -->
    <bean class="org.springframework.web.servlet.view.InternalResourceViewResolver">
        <property name="prefix" value="/WEB-INF/views/"/>
        <property name="suffix" value=".jsp"/>
    </bean>
</beans>
 
五、编写第一个控制器
1. 在  src/main/java  下新建包  com.controller 
2. 在包下新建  HelloController.java 
java
package com.controller;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
@Controller // 标记为控制器
public class HelloController {
    // 映射请求路径：http://localhost:8080/hello
    @RequestMapping("/hello")
    public String hello(Model model) {
        // 向视图传递数据
        model.addAttribute("msg", "Hello Spring MVC!");
        // 返回视图名称，视图解析器会拼接为 /WEB-INF/views/hello.jsp
        return "hello";
    }
}
 
六、编写视图页面
1. 在  WEB-INF  下新建文件夹  views 
2. 在  views  下新建  hello.jsp 
jsp
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>Spring MVC 入门</title>
</head>
<body>
    <h1>${msg}</h1>
</body>
</html>
 
七、配置 Tomcat 并运行
1. 点击 IDEA 右上角 添加配置 → 点击 + → 选择 Tomcat 服务器 → 本地
2. 配置 Tomcat 路径：点击 配置 → 选择本地 Tomcat 安装目录 → 点击 确定
3. 点击 部署 → 点击 + → 选择 工件 → 选择当前项目 → 点击 确定
4. 点击 运行 按钮，启动 Tomcat
5. 打开浏览器访问  http://localhost:8080/hello ，即可看到页面显示  Hello Spring MVC! 
八、核心流程总结
1. 前端发送请求 → 被 DispatcherServlet（前端控制器） 拦截
2. 前端控制器通过 HandlerMapping 找到对应的控制器方法
3. 控制器方法处理请求 → 将数据存入 Model → 返回视图名称
4. 前端控制器通过 ViewResolver（视图解析器） 解析视图路径
5. 渲染视图 → 将响应返回给浏览器
 

