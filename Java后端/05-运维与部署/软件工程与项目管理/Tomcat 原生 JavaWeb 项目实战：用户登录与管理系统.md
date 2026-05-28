Tomcat 原生 JavaWeb 项目实战：用户登录与管理系统
项目简介
基于 Tomcat 9 + Servlet + JSP + MySQL + JDBC 实现的用户管理系统，包含登录、用户列表、添加用户、删除用户等功能，完全贴合 Tomcat 原生开发流程，适合从零掌握 JavaWeb 与 Tomcat 部署。
技术栈
- 服务器：Tomcat 9
- 后端：Servlet、JSP、JDBC
- 数据库：MySQL 8.0
- 工具：Maven、Druid 连接池、Lombok
- 规范：MVC 架构、分层开发、Filter 过滤器
    项目结构（标准 Tomcat Web 项目）
    plaintext
    tomcat-demo/
    ├── src/
    │   └── main/
    │       ├── java/
    │       │   └── com/
    │       │       └── demo/
    │       │           ├── entity/          // 实体类
    │       │           ├── dao/             // 数据访问层
    │       │           │   └── impl/        // DAO 实现
    │       │           ├── service/         // 业务层
    │       │           │   └── impl/        // Service 实现
    │       │           ├── servlet/         // 控制器（Servlet）
    │       │           ├── filter/          // 过滤器
    │       │           └── util/            // 工具类（DB、加密）
    │       ├── resources/
    │       │   └── db.properties            // 数据库配置
    │       └── webapp/                      // Web 根目录（Tomcat 识别）
    │           ├── WEB-INF/
    │           │   ├── web.xml              // 部署描述文件
    │           │   └── lib/                 // 依赖 jar 包
    │           ├── login.jsp                // 登录页
    │           ├── index.jsp                // 首页
    │           └── user/
    │               └── list.jsp             // 用户列表页
    └── pom.xml                               // Maven 配置
 
 
1. 创建 Maven 项目（打包方式 war）
    xml
    <?xml version="1.0" encoding="UTF-8"?>
    <project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.demo</groupId>
    <artifactId>tomcat-demo</artifactId>
    <version>1.0-SNAPSHOT</version>
    <packaging>war</packaging>
    <dependencies>
        <!-- Servlet API -->
        <dependency>
            <groupId>javax.servlet</groupId>
            <artifactId>javax.servlet-api</artifactId>
            <version>4.0.1</version>
            <scope>provided</scope>
        </dependency>
        <!-- JSP API -->
        <dependency>
            <groupId>javax.servlet.jsp</groupId>
            <artifactId>javax.servlet.jsp-api</artifactId>
            <version>2.3.3</version>
            <scope>provided</scope>
        </dependency>
        <!-- JSTL 标签库 -->
        <dependency>
            <groupId>jstl</groupId>
            <artifactId>jstl</artifactId>
            <version>1.2</version>
        </dependency>
        <!-- MySQL 驱动 -->
        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
            <version>8.0.33</version>
        </dependency>
        <!-- Druid 连接池 -->
        <dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>druid</artifactId>
            <version>1.2.16</version>
        </dependency>
        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>1.18.30</version>
        </dependency>
    </dependencies>
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.8.1</version>
                <configuration>
                    <source>8</source>
                    <target>8</target>
                </configuration>
            </plugin>
        </plugins>
    </build>
    </project>
 
 
2. 数据库 SQL（MySQL）
    sql
    CREATE DATABASE IF NOT EXISTS tomcat_demo DEFAULT CHARSET utf8mb4;
    USE tomcat_demo;
    CREATE TABLE user (
    id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    nickname VARCHAR(50)
    );
    INSERT INTO user (username, password, nickname) 
    VALUES ('admin', '123456', '管理员');
 
 
3. 数据库配置（resources/db.properties）
    properties
    driverClassName=com.mysql.cj.jdbc.Driver
    url=jdbc:mysql://localhost:3306/tomcat_demo?useSSL=false&serverTimezone=Asia/Shanghai
    username=root
    password=root
    initialSize=5
    maxActive=10
 
 
4. 数据库工具类（DBUtil.java）
    java
    package com.demo.util;
    import com.alibaba.druid.pool.DruidDataSourceFactory;
    import javax.sql.DataSource;
    import java.sql.Connection;
    import java.sql.PreparedStatement;
    import java.sql.ResultSet;
    import java.sql.SQLException;
    import java.util.Properties;
    public class DBUtil {
    private static DataSource dataSource;
    static {
        try {
            Properties prop = new Properties();
            prop.load(DBUtil.class.getClassLoader().getResourceAsStream("db.properties"));
            dataSource = DruidDataSourceFactory.createDataSource(prop);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
    public static void close(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
    public static void close(Connection conn, PreparedStatement pstmt) {
        if (pstmt != null) {
            try {
                pstmt.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        close(conn);
    }
    public static void close(Connection conn, PreparedStatement pstmt, ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        close(conn, pstmt);
    }
    }
 
 
5. 实体类（User.java）
    java
    package com.demo.entity;
    import lombok.Data;
    @Data
    public class User {
    private Integer id;
    private String username;
    private String password;
    private String nickname;
    }
 
 
6. DAO 层（UserDAO.java）
    java
    package com.demo.dao;
    import com.demo.entity.User;
    import java.util.List;
    public interface UserDAO {
    User findByUsername(String username);
    List<User> findAll();
    void add(User user);
    void deleteById(Integer id);
    }
 
 
7. DAO 实现（UserDAOImpl.java）
    java
    package com.demo.dao.impl;
    import com.demo.dao.UserDAO;
    import com.demo.entity.User;
    import com.demo.util.DBUtil;
    import java.sql.Connection;
    import java.sql.PreparedStatement;
    import java.sql.ResultSet;
    import java.util.ArrayList;
    import java.util.List;
    public class UserDAOImpl implements UserDAO {
    @Override
    public User findByUsername(String username) {
        String sql = "SELECT * FROM user WHERE username = ?";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, username);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setUsername(rs.getString("username"));
                user.setPassword(rs.getString("password"));
                user.setNickname(rs.getString("nickname"));
                return user;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            DBUtil.close(conn, pstmt, rs);
        }
        return null;
    }
    @Override
    public List<User> findAll() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM user";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setUsername(rs.getString("username"));
                user.setNickname(rs.getString("nickname"));
                list.add(user);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            DBUtil.close(conn, pstmt, rs);
        }
        return list;
    }
    @Override
    public void add(User user) {
        String sql = "INSERT INTO user (username, password, nickname) VALUES (?, ?, ?)";
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getPassword());
            pstmt.setString(3, user.getNickname());
            pstmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            DBUtil.close(conn, pstmt);
        }
    }
    @Override
    public void deleteById(Integer id) {
        String sql = "DELETE FROM user WHERE id = ?";
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            DBUtil.close(conn, pstmt);
        }
    }
    }
 
 
8. Service 层（UserService.java）
    java
    package com.demo.service;
    import com.demo.entity.User;
    import java.util.List;
    public interface UserService {
    User login(String username, String password);
    List<User> findAll();
    void add(User user);
    void deleteById(Integer id);
    }
 
 
9. Service 实现（UserServiceImpl.java）
    java
    package com.demo.service.impl;
    import com.demo.dao.UserDAO;
    import com.demo.dao.impl.UserDAOImpl;
    import com.demo.entity.User;
    import com.demo.service.UserService;
    import java.util.List;
    public class UserServiceImpl implements UserService {
    private UserDAO userDAO = new UserDAOImpl();
    @Override
    public User login(String username, String password) {
        User user = userDAO.findByUsername(username);
        if (user != null && user.getPassword().equals(password)) {
            return user;
        }
        return null;
    }
    @Override
    public List<User> findAll() {
        return userDAO.findAll();
    }
    @Override
    public void add(User user) {
        userDAO.add(user);
    }
    @Override
    public void deleteById(Integer id) {
        userDAO.deleteById(id);
    }
    }
 
 
10. Servlet（登录、用户列表、添加、删除）
    LoginServlet.java
    java
    package com.demo.servlet;
    import com.demo.entity.User;
    import com.demo.service.UserService;
    import com.demo.service.impl.UserServiceImpl;
    import javax.servlet.ServletException;
    import javax.servlet.annotation.WebServlet;
    import javax.servlet.http.HttpServlet;
    import javax.servlet.http.HttpServletRequest;
    import javax.servlet.http.HttpServletResponse;
    import java.io.IOException;
    @WebServlet("/login")
    public class LoginServlet extends HttpServlet {
    private UserService userService = new UserServiceImpl();
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        User user = userService.login(username, password);
        if (user != null) {
            request.getSession().setAttribute("user", user);
            response.sendRedirect("index.jsp");
        } else {
            request.setAttribute("msg", "用户名或密码错误");
            request.getRequestDispatcher("login.jsp").forward(request, response);
        }
    }
    }
 
UserListServlet.java
java
package com.demo.servlet;
import com.demo.entity.User;
import com.demo.service.UserService;
import com.demo.service.impl.UserServiceImpl;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
@WebServlet("/user/list")
public class UserListServlet extends HttpServlet {
    private UserService userService = new UserServiceImpl();
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        List<User> userList = userService.findAll();
        request.setAttribute("userList", userList);
        request.getRequestDispatcher("/user/list.jsp").forward(request, response);
    }
}
 
UserAddServlet.java
java
package com.demo.servlet;
import com.demo.entity.User;
import com.demo.service.UserService;
import com.demo.service.impl.UserServiceImpl;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
@WebServlet("/user/add")
public class UserAddServlet extends HttpServlet {
    private UserService userService = new UserServiceImpl();
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        String nickname = request.getParameter("nickname");
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setNickname(nickname);
        userService.add(user);
        response.sendRedirect("/user/list");
    }
}
 
UserDeleteServlet.java
java
package com.demo.servlet;
import com.demo.service.UserService;
import com.demo.service.impl.UserServiceImpl;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
@WebServlet("/user/delete")
public class UserDeleteServlet extends HttpServlet {
    private UserService userService = new UserServiceImpl();
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Integer id = Integer.parseInt(request.getParameter("id"));
        userService.deleteById(id);
        response.sendRedirect("/user/list");
    }
}
 
 
11. 登录过滤器（LoginFilter.java）
    java
    package com.demo.filter;
    import javax.servlet.*;
    import javax.servlet.annotation.WebFilter;
    import javax.servlet.http.HttpServletRequest;
    import javax.servlet.http.HttpServletResponse;
    import java.io.IOException;
    @WebFilter("/*")
    public class LoginFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        String uri = req.getRequestURI();
        if (uri.contains("login") || uri.contains("login.jsp")) {
            chain.doFilter(request, response);
            return;
        }
        Object user = req.getSession().getAttribute("user");
        if (user == null) {
            resp.sendRedirect("login.jsp");
            return;
        }
        chain.doFilter(request, response);
    }
    }
 
 
12. JSP 页面
    login.jsp
    jsp
    <%@ page contentType="text/html;charset=UTF-8" language="java" %>
    <html>
    <head>
    <title>登录</title>
    </head>
    <body>
    <h2>用户登录</h2>
    <form action="login" method="post">
        <div>
            <label>用户名：</label>
            <input type="text" name="username" required>
        </div>
        <div>
            <label>密码：</label>
            <input type="password" name="password" required>
        </div>
        <div>
            <button type="submit">登录</button>
        </div>
        <div style="color:red">${msg}</div>
    </form>
    </body>
    </html>
 
index.jsp
jsp
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>首页</title>
</head>
<body>
    <h2>欢迎，${user.nickname}</h2>
    <a href="/user/list">用户管理</a>
    <a href="login.jsp">退出登录</a>
</body>
</html>
 
user/list.jsp
jsp
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
<head>
    <title>用户列表</title>
</head>
<body>
    <h2>用户列表</h2>
    <form action="/user/add" method="post">
        <input type="text" name="username" placeholder="用户名" required>
        <input type="password" name="password" placeholder="密码" required>
        <input type="text" name="nickname" placeholder="昵称" required>
        <button type="submit">添加</button>
    </form>
    <table border="1">
        <tr>
            <th>ID</th>
            <th>用户名</th>
            <th>昵称</th>
            <th>操作</th>
        </tr>
        <c:forEach items="${userList}" var="user">
            <tr>
                <td>${user.id}</td>
                <td>${user.username}</td>
                <td>${user.nickname}</td>
                <td>
                    <a href="/user/delete?id=${user.id}">删除</a>
                </td>
            </tr>
        </c:forEach>
    </table>
</body>
</html>
 
 
13. web.xml（WEB-INF/web.xml）
    xml
    <?xml version="1.0" encoding="UTF-8"?>
    <web-app xmlns="http://xmlns.jcp.org/xml/ns/javaee"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/web-app_4_0.xsd"
         version="4.0">
    <welcome-file-list>
        <welcome-file>login.jsp</welcome-file>
    </welcome-file-list>
    </web-app>
 
 
14. 部署到 Tomcat
    1. 执行  mvn package  生成  tomcat-demo.war 
    2. 将 war 包放入  Tomcat/webapps 
    3. 启动 Tomcat（bin/startup.bat）
    4. 访问： http://localhost:8080/tomcat-demo/login.jsp 
 
15. 测试账号
    - 用户名： admin 
    - 密码： 123456 
 
项目核心 Tomcat 知识点
- Servlet 生命周期：init → service → destroy
- JSP 本质：最终编译为 Servlet
- Filter 过滤器：统一登录校验、编码处理
- Session 会话：用户登录状态保持
- Web 应用部署：war 包 + web.xml
- MVC 架构：Servlet（C）+ JSP（V）+ JavaBean（M）
 
扩展方向
1. 实现用户修改密码
2. 加入分页查询
3. 实现权限管理（管理员/普通用户）
4. 加入文件上传（头像）
5. 集成日志框架（log4j）
6. 实现 AJAX 异步请求
