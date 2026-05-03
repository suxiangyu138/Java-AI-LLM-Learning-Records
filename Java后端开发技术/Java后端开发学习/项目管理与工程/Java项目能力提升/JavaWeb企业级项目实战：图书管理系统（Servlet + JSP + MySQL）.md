03.31 02:07
JavaWeb企业级项目实战：图书管理系统（Servlet + JSP + MySQL）
项目简介
基于原生JavaWeb技术栈（Servlet + JSP + MySQL + JDBC）实现的图书管理系统，包含用户登录、图书管理、借阅管理、分类管理等核心功能，覆盖JavaWeb开发全流程，适合夯实基础。
技术栈
- 后端：Servlet、Filter、Listener、JDBC
- 前端：JSP、EL表达式、JSTL、HTML/CSS/JS
- 数据库：MySQL 8.0
- 工具：Maven、Tomcat 9、Lombok、Druid连接池
- 规范：MVC架构、分层开发、事务控制
项目结构
plaintext
book-management/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── book/
│   │   │           ├── filter/       // 过滤器（登录校验、编码）
│   │   │           ├── listener/     // 监听器
│   │   │           ├── servlet/      // 控制器
│   │   │           ├── service/      // 业务层
│   │   │           │   └── impl/     // 业务实现
│   │   │           ├── dao/          // 数据访问层
│   │   │           │   └── impl/     // DAO实现
│   │   │           ├── entity/       // 实体类
│   │   │           ├── util/         // 工具类（DB、MD5）
│   │   │           └── constant/     // 常量
│   │   ├── resources/
│   │   │   ├── db.properties         // 数据库配置
│   │   │   └── log4j.properties      // 日志配置
│   │   └── webapp/
│   │       ├── WEB-INF/
│   │       │   ├── web.xml            // 配置文件
│   │       │   └── lib/               // 依赖jar
│   │       ├── index.jsp              // 首页
│   │       ├── login.jsp              // 登录页
│   │       ├── book/                 // 图书相关页面
│   │       ├── borrow/               // 借阅相关页面
│   │       └── css/                  // 样式
└── pom.xml                            // Maven配置
 
完整代码实现
1. 数据库SQL脚本
sql
CREATE DATABASE IF NOT EXISTS book_management DEFAULT CHARSET utf8mb4;
USE book_management;
-- 用户表
CREATE TABLE user (
    id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    name VARCHAR(20) NOT NULL,
    role TINYINT NOT NULL DEFAULT 1 COMMENT '1-普通用户 2-管理员'
) COMMENT '用户表';
-- 图书分类表
CREATE TABLE category (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL UNIQUE
) COMMENT '图书分类表';
-- 图书表
CREATE TABLE book (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    author VARCHAR(50),
    category_id INT NOT NULL,
    stock INT NOT NULL DEFAULT 0,
    FOREIGN KEY (category_id) REFERENCES category(id)
) COMMENT '图书表';
-- 借阅表
CREATE TABLE borrow (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    book_id INT NOT NULL,
    borrow_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    return_time DATETIME,
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1-借阅中 2-已归还',
    FOREIGN KEY (user_id) REFERENCES user(id),
    FOREIGN KEY (book_id) REFERENCES book(id)
) COMMENT '借阅表';
-- 初始化管理员
INSERT INTO user (username, password, name, role) VALUES ('admin', '123456', '管理员', 2);
 
2. Maven配置（pom.xml）
xml
<dependencies>
    <!-- Servlet -->
    <dependency>
        <groupId>javax.servlet</groupId>
        <artifactId>javax.servlet-api</artifactId>
        <version>4.0.1</version>
        <scope>provided</scope>
    </dependency>
    <!-- JSP -->
    <dependency>
        <groupId>javax.servlet.jsp</groupId>
        <artifactId>javax.servlet.jsp-api</artifactId>
        <version>2.3.3</version>
        <scope>provided</scope>
    </dependency>
    <!-- JSTL -->
    <dependency>
        <groupId>jstl</groupId>
        <artifactId>jstl</artifactId>
        <version>1.2</version>
    </dependency>
    <!-- MySQL -->
    <dependency>
        <groupId>mysql</groupId>
        <artifactId>mysql-connector-java</artifactId>
        <version>8.0.33</version>
    </dependency>
    <!-- Druid -->
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
 
3. 数据库配置（db.properties）
properties
driverClassName=com.mysql.cj.jdbc.Driver
url=jdbc:mysql://localhost:3306/book_management?useSSL=false&serverTimezone=Asia/Shanghai
username=root
password=root
initialSize=5
maxActive=10
 
4. 工具类（DBUtil.java）
java
import com.alibaba.druid.pool.DruidDataSourceFactory;
import javax.sql.DataSource;
import java.sql.Connection;
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
    public static void close(Connection conn, java.sql.PreparedStatement pstmt) {
        if (pstmt != null) {
            try {
                pstmt.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        close(conn);
    }
    public static void close(Connection conn, java.sql.PreparedStatement pstmt, java.sql.ResultSet rs) {
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
import lombok.Data;
@Data
public class User {
    private Integer id;
    private String username;
    private String password;
    private String name;
    private Integer role;
}
 
6. DAO层（UserDAO.java）
java
import com.book.entity.User;
public interface UserDAO {
    User findByUsername(String username);
}
 
7. DAO实现（UserDAOImpl.java）
java
import com.book.dao.UserDAO;
import com.book.entity.User;
import com.book.util.DBUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
                user.setName(rs.getString("name"));
                user.setRole(rs.getInt("role"));
                return user;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            DBUtil.close(conn, pstmt, rs);
        }
        return null;
    }
}
 
8. Service层（UserService.java）
java
import com.book.entity.User;
public interface UserService {
    User login(String username, String password);
}
 
9. Service实现（UserServiceImpl.java）
java
import com.book.dao.UserDAO;
import com.book.dao.impl.UserDAOImpl;
import com.book.entity.User;
import com.book.service.UserService;
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
}
 
10. 登录Servlet（LoginServlet.java）
java
import com.book.entity.User;
import com.book.service.UserService;
import com.book.service.impl.UserServiceImpl;
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
 
11. 登录过滤器（LoginFilter.java）
java
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
        if (uri.contains("login") || uri.contains("css")) {
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
 
12. 登录页面（login.jsp）
jsp
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>登录</title>
</head>
<body>
    <h2>图书管理系统登录</h2>
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
 
13. 首页（index.jsp）
jsp
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
<head>
    <title>首页</title>
</head>
<body>
    <h2>欢迎，${user.name}</h2>
    <a href="book/list">图书管理</a>
    <a href="borrow/list">借阅管理</a>
    <a href="logout">退出登录</a>
</body>
</html>
 
核心功能扩展（图书管理）
1. 图书实体（Book.java）
java
import lombok.Data;
@Data
public class Book {
    private Integer id;
    private String name;
    private String author;
    private Integer categoryId;
    private Integer stock;
}
 
2. 图书Servlet（BookListServlet.java）
java
import com.book.entity.Book;
import com.book.service.BookService;
import com.book.service.impl.BookServiceImpl;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
@WebServlet("/book/list")
public class BookListServlet extends HttpServlet {
    private BookService bookService = new BookServiceImpl();
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        List<Book> bookList = bookService.findAll();
        request.setAttribute("bookList", bookList);
        request.getRequestDispatcher("/book/list.jsp").forward(request, response);
    }
}
 
3. 图书列表页面（book/list.jsp）
jsp
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
<head>
    <title>图书列表</title>
</head>
<body>
    <h2>图书列表</h2>
    <a href="add.jsp">新增图书</a>
    <table border="1">
        <tr>
            <th>ID</th>
            <th>书名</th>
            <th>作者</th>
            <th>库存</th>
            <th>操作</th>
        </tr>
        <c:forEach items="${bookList}" var="book">
            <tr>
                <td>${book.id}</td>
                <td>${book.name}</td>
                <td>${book.author}</td>
                <td>${book.stock}</td>
                <td>
                    <a href="edit?id=${book.id}">编辑</a>
                    <a href="delete?id=${book.id}">删除</a>
                </td>
            </tr>
        </c:forEach>
    </table>
</body>
</html>
 
运行步骤
1. 执行SQL脚本创建数据库和表
2. 修改db.properties数据库连接信息
3. 配置Tomcat 9服务器
4. 启动项目，访问http://localhost:8080/login.jsp
5. 登录账号：admin/123456
核心知识点覆盖
1. MVC架构：Servlet（Controller）+ JSP（View）+ JavaBean（Model）
2. 分层开发：DAO、Service、Servlet分层解耦
3. 过滤器：登录校验、编码统一
4. 连接池：Druid实现数据库连接复用
5. JDBC：CRUD操作、事务控制
6. JSTL+EL：简化JSP页面数据展示
7. 会话管理：Session存储用户登录状态
扩展方向
1. 实现图书借阅/归还功能
2. 加入分页查询、模糊搜索
3. 实现用户注册、密码加密（MD5）
4. 加入权限控制（管理员/普通用户）
5. 集成文件上传（图书封面）
6. 实现数据统计、报表导出

