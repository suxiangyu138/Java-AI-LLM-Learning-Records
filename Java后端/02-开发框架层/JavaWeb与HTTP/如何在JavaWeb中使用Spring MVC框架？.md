如何在JavaWeb中使用Spring MVC框架？

--------------------------------------------------------------------------------------------------------------------------------------
一、Spring MVC核心概念
1. 定义
    Spring MVC是Spring框架的Web模块，基于MVC（Model-View-Controller）设计模式，封装了原生Servlet，简化JavaWeb开发，是当前主流的JavaWeb开发框架。
2. MVC角色
    - Controller（控制器）：处理请求，调用业务逻辑，返回数据/视图（对应Spring MVC的@Controller/@RestController）
    - Model（模型）：封装数据（如POJO、Map、ModelAndView）
    - View（视图）：展示数据（如JSP、Thymeleaf、HTML）
3. 核心执行流程
    浏览器发送请求 → DispatcherServlet（前端控制器）接收 → HandlerMapping（处理器映射器）找到对应Controller方法 → HandlerAdapter（处理器适配器）执行方法 → Controller返回ModelAndView → ViewResolver（视图解析器）解析视图 → 渲染视图并返回响应

--------------------------------------------------------------------------------------------------------------------------------------
二、环境准备
1. 依赖配置（Maven为例）
```xml
<!-- Spring MVC核心依赖 -->
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-webmvc</artifactId>
    <version>5.3.30</version>
</dependency>
<!-- Servlet API（Tomcat 9+对应4.0+） -->
<dependency>
    <groupId>javax.servlet</groupId>
    <artifactId>javax.servlet-api</artifactId>
    <version>4.0.1</version>
    <scope>provided</scope>
</dependency>
<!-- JSP API（如需JSP视图） -->
<dependency>
    <groupId>javax.servlet.jsp</groupId>
    <artifactId>jsp-api</artifactId>
    <version>2.2</version>
    <scope>provided</scope>
</dependency>
<!-- JSON解析（返回JSON需添加） -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.15.2</version>
</dependency>
```
2. 部署描述符（web.xml）配置DispatcherServlet
```xml
<?xml version="1.0" encoding="UTF-8"?>
<web-app xmlns="http://xmlns.jcp.org/xml/ns/javaee"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/web-app_4_0.xsd"
         version="4.0">
    <!-- 配置Spring MVC前端控制器 -->
    <servlet>
        <servlet-name>dispatcherServlet</servlet-name>
        <servlet-class>org.springframework.web.servlet.DispatcherServlet</servlet-class>
        <!-- 指定Spring MVC配置文件路径 -->
        <init-param>
            <param-name>contextConfigLocation</param-name>
            <param-value>classpath:spring-mvc.xml</param-value>
        </init-param>
        <!-- 服务器启动时加载Servlet（优先级1） -->
        <load-on-startup>1</load-on-startup>
    </servlet>
    <!-- 映射所有请求到DispatcherServlet -->
    <servlet-mapping>
        <servlet-name>dispatcherServlet</servlet-name>
        <url-pattern>/</url-pattern>
    </servlet-mapping>
    <!-- 字符编码过滤器（解决中文乱码） -->
    <filter>
        <filter-name>characterEncodingFilter</filter-name>
        <filter-class>org.springframework.web.filter.CharacterEncodingFilter</filter-class>
        <init-param>
            <param-name>encoding</param-name>
            <param-value>UTF-8</param-value>
        </init-param>
        <init-param>
            <param-name>forceEncoding</param-name>
            <param-value>true</param-value>
        </init-param>
    </filter>
    <filter-mapping>
        <filter-name>characterEncodingFilter</filter-name>
        <url-pattern>/*</url-pattern>
    </filter-mapping>
</web-app>
```

--------------------------------------------------------------------------------------------------------------------------------------
三、Spring MVC核心配置（spring-mvc.xml）
```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:context="http://www.springframework.org/schema/context"
       xmlns:mvc="http://www.springframework.org/schema/mvc"
       xsi:schemaLocation="
        http://www.springframework.org/schema/beans
        http://www.springframework.org/schema/beans/spring-beans.xsd
        http://www.springframework.org/schema/context
        http://www.springframework.org/schema/context/spring-context.xsd
        http://www.springframework.org/schema/mvc
        http://www.springframework.org/schema/mvc/spring-mvc.xsd">
    <!-- 1. 扫描控制器包（@Controller/@Service等注解） -->
    <context:component-scan base-package="com.example.controller"/>
    <!-- 2. 开启MVC注解驱动（支持@RequestMapping、@ResponseBody等） -->
    <mvc:annotation-driven/>
    <!-- 3. 配置视图解析器（JSP为例） -->
    <bean class="org.springframework.web.servlet.view.InternalResourceViewResolver">
        <!-- 视图前缀 -->
        <property name="prefix" value="/WEB-INF/views/"/>
        <!-- 视图后缀 -->
        <property name="suffix" value=".jsp"/>
    </bean>
    <!-- 4. 静态资源放行（如CSS/JS/图片） -->
    <mvc:default-servlet-handler/>
</beans>
```

--------------------------------------------------------------------------------------------------------------------------------------
四、核心开发步骤
1. 编写实体类（Model）
```java
package com.example.entity;
// 用户实体类（Model）
public class User {
    private Long id;
    private String username;
    private Integer age;
    // 无参构造、有参构造、getter/setter、toString
    public User() {}
    public User(Long id, String username, Integer age) {
        this.id = id;
        this.username = username;
        this.age = age;
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", age=" + age +
                '}';
    }
}
```
2. 编写控制器（Controller）
    ① 返回视图（传统MVC）
```java
package com.example.controller;
import com.example.entity.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import java.util.ArrayList;
import java.util.List;
@Controller
@RequestMapping("/user") // 类级请求映射
public class UserController {
    // 处理/user/list请求，返回用户列表视图
    @GetMapping("/list")
    public String getUserList(Model model) {
        // 模拟从数据库查询数据
        List<User> userList = new ArrayList<>();
        userList.add(new User(1L, "张三", 20));
        userList.add(new User(2L, "李四", 25));
        userList.add(new User(3L, "王五", 30));
        // 将数据存入Model（传递到视图）
        model.addAttribute("userList", userList);
        // 返回视图名（视图解析器拼接为/WEB-INF/views/user/list.jsp）
        return "user/list";
    }
}
```
② 返回JSON（RESTful风格）
```java
package com.example.controller;
import com.example.entity.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.ArrayList;
import java.util.List;
// @RestController = @Controller + @ResponseBody（所有方法返回JSON）
@RestController
@RequestMapping("/api/user")
public class UserApiController {
    // 获取所有用户（返回JSON数组）
    @GetMapping("/all")
    public List<User> getAllUsers() {
        List<User> userList = new ArrayList<>();
        userList.add(new User(1L, "张三", 20));
        userList.add(new User(2L, "李四", 25));
        return userList;
    }
    // 根据ID获取用户（路径参数）
    @GetMapping("/{id}")
    public User getUserById(@PathVariable Long id) {
        return new User(id, "张三", 20);
    }
}
```
3. 编写视图（JSP示例，/WEB-INF/views/user/list.jsp）
```jsp
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
<head>
    <title>用户列表</title>
</head>
<body>
    <h1>用户列表</h1>
    <table border="1" cellpadding="5" cellspacing="0">
        <tr>
            <th>ID</th>
            <th>用户名</th>
            <th>年龄</th>
        </tr>
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

--------------------------------------------------------------------------------------------------------------------------------------
五、核心功能使用
1. 请求参数绑定
    ① 基本类型/字符串参数
```java
// 请求示例：/api/user/query?username=张三&age=20
@GetMapping("/query")
public User queryUser(String username, Integer age) {
    return new User(1L, username, age);
}
```
② 对象参数（自动封装）
```java
// 请求示例：/api/user/save?username=张三&age=20
@PostMapping("/save")
public User saveUser(User user) {
    user.setId(1L);
    return user;
}
```
③ 请求体参数（JSON）
```java
// 请求体：{"username":"张三","age":20}
@PostMapping("/create")
public User createUser(@RequestBody User user) {
    user.setId(1L);
    return user;
}
```
④ 数组/集合参数
```java
// 请求示例：/api/user/batch?ids=1&ids=2&ids=3
@GetMapping("/batch")
public List<User> batchQuery(Long[] ids) {
    List<User> userList = new ArrayList<>();
    for (Long id : ids) {
        userList.add(new User(id, "用户" + id, 20));
    }
    return userList;
}
```
2. 文件上传
    ① 配置文件上传解析器（spring-mvc.xml）
```xml
<!-- 文件上传解析器（id必须为multipartResolver） -->
<bean id="multipartResolver" class="org.springframework.web.multipart.commons.CommonsMultipartResolver">
    <!-- 最大上传文件大小（10MB） -->
    <property name="maxUploadSize" value="10485760"/>
    <!-- 编码 -->
    <property name="defaultEncoding" value="UTF-8"/>
</bean>
```
② 添加文件上传依赖（Maven）
```xml
<dependency>
    <groupId>commons-fileupload</groupId>
    <artifactId>commons-fileupload</artifactId>
    <version>1.4</version>
</dependency>
<dependency>
    <groupId>commons-io</groupId>
    <artifactId>commons-io</artifactId>
    <version>2.11.0</version>
</dependency>
```
③ 编写上传控制器
```java
@RestController
@RequestMapping("/upload")
public class UploadController {
    @PostMapping("/file")
    public String uploadFile(@RequestParam("file") MultipartFile file, HttpServletRequest request) throws IOException {
        // 判断文件是否为空
        if (file.isEmpty()) {
            return "文件为空";
        }
        // 获取文件名
        String fileName = file.getOriginalFilename();
        // 获取文件存储路径（Tomcat的uploads目录）
        String uploadPath = request.getServletContext().getRealPath("/uploads/");
        File pathFile = new File(uploadPath);
        if (!pathFile.exists()) {
            pathFile.mkdirs(); // 创建目录
        }
        // 保存文件
        File destFile = new File(uploadPath + fileName);
        file.transferTo(destFile);
        return "上传成功：" + fileName;
    }
}
```
3. 异常处理
    ① 全局异常处理器
```java
package com.example.controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.HashMap;
import java.util.Map;
// 全局异常处理（适配所有@RestController）
@RestControllerAdvice
public class GlobalExceptionHandler {
    // 处理运行时异常
    @ExceptionHandler(RuntimeException.class)
    public Map<String, Object> handleRuntimeException(RuntimeException e) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 500);
        result.put("msg", "系统异常：" + e.getMessage());
        result.put("data", null);
        return result;
    }
    // 处理自定义异常
    @ExceptionHandler(BusinessException.class)
    public Map<String, Object> handleBusinessException(BusinessException e) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", e.getCode());
        result.put("msg", e.getMessage());
        result.put("data", null);
        return result;
    }
}
// 自定义业务异常
class BusinessException extends RuntimeException {
    private Integer code;
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
    public Integer getCode() {
        return code;
    }
}
```
4. 拦截器
    ① 自定义拦截器
```java
package com.example.interceptor;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
public class LoginInterceptor implements HandlerInterceptor {
    // 预处理（请求到达Controller前执行）
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 判断用户是否登录（从Session获取用户）
        Object user = request.getSession().getAttribute("user");
        if (user == null) {
            // 未登录，重定向到登录页
            response.sendRedirect("/login.jsp");
            return false; // 拦截请求
        }
        return true; // 放行请求
    }
    // 后处理（Controller执行后，视图渲染前）
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        HandlerInterceptor.super.postHandle(request, response, handler, modelAndView);
    }
    // 完成处理（视图渲染后）
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        HandlerInterceptor.super.afterCompletion(request, response, handler, ex);
    }
}
```
② 配置拦截器（spring-mvc.xml）
```xml
<!-- 配置拦截器 -->
<mvc:interceptors>
    <mvc:interceptor>
        <!-- 拦截所有请求 -->
        <mvc:mapping path="/**"/>
        <!-- 排除登录页和登录接口 -->
        <mvc:exclude-mapping path="/login.jsp"/>
        <mvc:exclude-mapping path="/api/login"/>
        <!-- 自定义拦截器 -->
        <bean class="com.example.interceptor.LoginInterceptor"/>
    </mvc:interceptor>
</mvc:interceptors>
```

--------------------------------------------------------------------------------------------------------------------------------------
六、Spring Boot整合Spring MVC（简化版）
1. 依赖（pom.xml）
```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>2.7.15</version>
    <relativePath/>
</parent>
<dependencies>
    <!-- Spring Boot Web（包含Spring MVC、Tomcat、JSON解析） -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <!-- 热部署（可选） -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-devtools</artifactId>
        <scope>runtime</scope>
        <optional>true</optional>
    </dependency>
</dependencies>
```
2. 启动类
```java
package com.example;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@SpringBootApplication // 自动配置+组件扫描+开启Spring Boot
public class SpringMvcDemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpringMvcDemoApplication.class, args);
    }
}
```
3. 控制器（无需web.xml和spring-mvc.xml）
```java
package com.example.controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController
public class HelloController {
    @GetMapping("/hello")
    public String hello() {
        return "Hello Spring MVC + Spring Boot!";
    }
}
```
4. 配置文件（application.properties）
```properties
# 服务器端口
server.port=8080
# 上下文路径
server.servlet.context-path=/demo
# 视图前缀（如需JSP）
spring.mvc.view.prefix=/WEB-INF/views/
# 视图后缀
spring.mvc.view.suffix=.jsp
```

--------------------------------------------------------------------------------------------------------------------------------------
总结
1. Spring MVC核心是DispatcherServlet（前端控制器），通过注解简化Servlet开发，核心注解包括@Controller/@RestController、@RequestMapping/@GetMapping/@PostMapping
2. 基础使用步骤：配置DispatcherServlet → 编写Spring MVC配置文件 → 开发Controller → 编写视图/返回JSON
3. Spring Boot整合Spring MVC可省略xml配置，通过自动配置快速开发，是当前主流方式，核心依赖为spring-boot-starter-web
