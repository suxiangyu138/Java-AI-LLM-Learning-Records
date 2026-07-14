# Maven Web 应用开发

## 一、Maven Web 项目结构

```
web-project/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/com/example/web/
│   │   │   ├── servlet/         # Servlet 控制层
│   │   │   ├── service/         # 业务层
│   │   │   └── entity/          # 实体层
│   │   └── webapp/              # Web 资源（Maven Web 项目专属）
│   │       ├── WEB-INF/
│   │       │   └── web.xml      # Web 部署描述符
│   │       └── *.jsp            # JSP 页面
│   └── test/java/               # 测试代码
└── target/                      # 构建产物
```

**关键差异**：
- `packaging` 必须为 `war`
- 必须有 `src/main/webapp/WEB-INF/` 目录

## 二、pom.xml 配置

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" ...>
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example.web</groupId>
    <artifactId>user-manage-web</artifactId>
    <version>1.0.0</version>
    <packaging>war</packaging>     <!-- Web 项目必须为 war -->

    <dependencies>
        <!-- Servlet API（provided：Tomcat 已提供） -->
        <dependency>
            <groupId>javax.servlet</groupId>
            <artifactId>javax.servlet-api</artifactId>
            <version>4.0.1</version>
            <scope>provided</scope>
        </dependency>

        <!-- JSP API（provided） -->
        <dependency>
            <groupId>javax.servlet.jsp</groupId>
            <artifactId>jsp-api</artifactId>
            <version>2.3.3</version>
            <scope>provided</scope>
        </dependency>

        <!-- JSTL 标签库 -->
        <dependency>
            <groupId>javax.servlet.jsp.jstl</groupId>
            <artifactId>jstl-api</artifactId>
            <version>1.2</version>
        </dependency>
        <dependency>
            <groupId>taglibs</groupId>
            <artifactId>standard</artifactId>
            <version>1.1.2</version>
        </dependency>

        <!-- 测试 -->
        <dependency>
            <groupId>junit</groupId>
            <artifactId>junit</artifactId>
            <version>4.12</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <finalName>user-manage-web</finalName>
        <plugins>
            <!-- 编译插件 -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.8.1</version>
                <configuration>
                    <source>1.8</source>
                    <target>1.8</target>
                    <encoding>UTF-8</encoding>
                </configuration>
            </plugin>
            <!-- Tomcat 插件（一键部署） -->
            <plugin>
                <groupId>org.apache.tomcat.maven</groupId>
                <artifactId>tomcat7-maven-plugin</artifactId>
                <version>2.2</version>
                <configuration>
                    <port>8080</port>
                    <path>/user-manage</path>
                    <uriEncoding>UTF-8</uriEncoding>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

## 三、核心代码

### 3.1 Servlet 控制层

```java
package com.example.web.servlet;

import com.example.web.entity.User;
import com.example.web.service.UserService;
import com.example.web.service.UserServiceImpl;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@WebServlet("/user")
public class UserServlet extends HttpServlet {

    private final UserService userService = new UserServiceImpl();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        List<User> userList = userService.findAllUsers();
        req.setAttribute("userList", userList);
        req.getRequestDispatcher("/userList.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setContentType("text/html;charset=UTF-8");

        String name = req.getParameter("name");
        Integer age = Integer.parseInt(req.getParameter("age"));
        User user = new User(null, name, age);
        userService.addUser(user);

        resp.sendRedirect(req.getContextPath() + "/user");
    }
}
```

### 3.2 JSP 页面

```jsp
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
<head>
    <title>用户管理</title>
</head>
<body>
    <h1>用户管理系统</h1>

    <!-- 新增表单 -->
    <form action="${pageContext.request.contextPath}/user" method="post">
        姓名：<input type="text" name="name" required><br>
        年龄：<input type="number" name="age" min="1" max="120" required><br>
        <input type="submit" value="新增用户">
    </form>

    <!-- 用户列表 -->
    <table border="1">
        <tr><th>ID</th><th>姓名</th><th>年龄</th></tr>
        <c:forEach items="${userList}" var="user">
            <tr>
                <td>${user.id}</td>
                <td>${user.name}</td>
                <td>${user.age}</td>
            </tr>
        </c:forEach>
    </table>
</body>
</html>
```

## 四、打包与部署

### 4.1 两种部署方式

| 方式 | 命令 | 说明 |
|------|------|------|
| **Maven 一键部署** | `mvn tomcat7:run` | 开发环境，热部署 |
| **手动部署** | `mvn clean package` → 复制 war 到 Tomcat `webapps/` | 生产环境 |

### 4.2 Maven 一键部署

```bash
# 启动 Tomcat + 部署
mvn tomcat7:run

# 访问
# http://localhost:8080/user-manage/user
```

### 4.3 手动部署

```bash
# 打包
mvn clean package

# 复制 war 包到 Tomcat
cp target/user-manage-web.war $TOMCAT_HOME/webapps/

# 启动 Tomcat
$TOMCAT_HOME/bin/startup.sh
```

## 五、Spring Boot Web 对比

| 特性 | 传统 Web（Servlet + JSP） | Spring Boot Web |
|------|--------------------------|-----------------|
| packaging | `war` | `jar` |
| Servlet 配置 | web.xml / `@WebServlet` | 自动配置 |
| 部署 | 需外部 Tomcat | 内嵌 Tomcat，`java -jar` |
| 依赖 scope | `provided` | `compile`（默认） |
| 前端 | JSP | Thymeleaf / 前后端分离 |

## 六、常见问题

| 问题 | 原因 | 解决方案 |
|------|------|---------|
| servlet-api 冲突 | scope 为 compile | 改为 `provided` |
| JSP 中文乱码 | 编码未统一 | 所有层配置 UTF-8 |
| war 包无 class 文件 | 源码目录未识别 | 设置 `src/main/java` 为 Sources Root |
| 404 Not Found | Servlet 路径错误 | 检查 `@WebServlet` 和访问 URL |
| JSTL 标签无法识别 | 依赖缺失或未引入 | 检查依赖 + 添加 `<%@ taglib %>` |
| 端口占用 | 8080 已被占用 | 修改 Tomcat 插件 port 配置 |
