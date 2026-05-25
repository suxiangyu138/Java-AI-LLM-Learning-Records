JDBC 安装配置 + 企业级项目实战（图书管理系统）
一、JDBC 环境安装与配置（从零开始）
1. 安装 JDK（1.8+）
    - 下载地址：https://www.oracle.com/java/technologies/downloads/
    - 配置环境变量：
    plaintext
    JAVA_HOME=D:\jdk1.8.0_301
    PATH=%JAVA_HOME%\bin
 
- 验证： java -version 
    2. 安装 MySQL（8.0+）
- 下载地址：https://dev.mysql.com/downloads/mysql/
- 安装时设置 root 密码（如：root）
- 验证： mysql -u root -p 
    3. 引入 MySQL 驱动（Maven）
    创建 Maven 项目，在  pom.xml  中添加依赖：
    xml
    <dependencies>
    <!-- MySQL 驱动 -->
    <dependency>
        <groupId>mysql</groupId>
        <artifactId>mysql-connector-java</artifactId>
        <version>8.0.33</version>
    </dependency>
    <!-- Druid 连接池（企业级） -->
    <dependency>
        <groupId>com.alibaba</groupId>
        <artifactId>druid</artifactId>
        <version>1.2.16</version>
    </dependency>
    <!-- Lombok 简化代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <version>1.18.30</version>
        <scope>provided</scope>
    </dependency>
    </dependencies>
 
4. JDBC 核心概念（必懂）
    - DriverManager：加载驱动，获取连接
    - Connection：数据库连接对象
    - Statement：执行 SQL 语句
    - PreparedStatement：预编译 SQL，防止 SQL 注入
    - ResultSet：查询结果集
 
二、企业级项目：图书管理系统
项目简介
基于 JDBC + Druid 连接池实现的企业级图书管理系统，支持：
- 用户登录/注册
- 图书增删改查
- 图书借阅/归还
- 事务控制
- 连接池管理
    项目结构
    plaintext
    jdbc-book-system/
    ├── src/main/java/com/book
    │   ├── entity/          # 实体类
    │   │   ├── User.java
    │   │   └── Book.java
    │   ├── dao/             # 数据访问层
    │   │   ├── UserDAO.java
    │   │   ├── BookDAO.java
    │   │   └── impl/        # 实现类
    │   ├── service/         # 业务层
    │   │   ├── UserService.java
    │   │   ├── BookService.java
    │   │   └── impl/        # 实现类
    │   ├── util/            # 工具类
    │   │   ├── DBUtil.java
    │   │   └── DruidUtil.java
    │   └── test/            # 测试类
    │       ├── UserTest.java
    │       └── BookTest.java
    └── resources/
    └── druid.properties # 连接池配置
 
 
三、数据库设计（MySQL）
1. 创建数据库
    sql
    CREATE DATABASE IF NOT EXISTS book_system DEFAULT CHARSET utf8mb4;
    USE book_system;
 
2. 创建用户表
    sql
    CREATE TABLE user (
    id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    name VARCHAR(20) NOT NULL
    );
 
3. 创建图书表
    sql
    CREATE TABLE book (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    author VARCHAR(50),
    stock INT NOT NULL DEFAULT 0
    );
 
4. 创建借阅表
    sql
    CREATE TABLE borrow (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    book_id INT NOT NULL,
    borrow_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    status TINYINT DEFAULT 1 COMMENT '1-借阅中 2-已归还',
    FOREIGN KEY (user_id) REFERENCES user(id),
    FOREIGN KEY (book_id) REFERENCES book(id)
    );
 
 
四、连接池配置（Druid）
druid.properties
properties
driverClassName=com.mysql.cj.jdbc.Driver
url=jdbc:mysql://localhost:3306/book_system?useSSL=false&serverTimezone=Asia/Shanghai
username=root
password=root
initialSize=5
maxActive=10
maxWait=3000
 
DruidUtil.java
java
import com.alibaba.druid.pool.DruidDataSourceFactory;
import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Properties;
public class DruidUtil {
    private static DataSource dataSource;
    static {
        try {
            Properties prop = new Properties();
            prop.load(DruidUtil.class.getClassLoader().getResourceAsStream("druid.properties"));
            dataSource = DruidDataSourceFactory.createDataSource(prop);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // 获取连接
    public static Connection getConnection() {
        try {
            return dataSource.getConnection();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    // 关闭连接
    public static void close(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    public static void close(Connection conn, java.sql.PreparedStatement pstmt) {
        if (pstmt != null) {
            try {
                pstmt.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        close(conn);
    }
    public static void close(Connection conn, java.sql.PreparedStatement pstmt, java.sql.ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        close(conn, pstmt);
    }
}
 
 
五、实体类
User.java
java
import lombok.Data;
@Data
public class User {
    private Integer id;
    private String username;
    private String password;
    private String name;
}
 
Book.java
java
import lombok.Data;
@Data
public class Book {
    private Integer id;
    private String name;
    private String author;
    private Integer stock;
}
 
 
六、DAO 层（数据访问）
UserDAO.java
java
import com.book.entity.User;
public interface UserDAO {
    // 注册
    void register(User user);
    // 登录
    User login(String username, String password);
}
 
UserDAOImpl.java
java
import com.book.dao.UserDAO;
import com.book.entity.User;
import com.book.util.DruidUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
public class UserDAOImpl implements UserDAO {
    @Override
    public void register(User user) {
        String sql = "INSERT INTO user (username, password, name) VALUES (?, ?, ?)";
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = DruidUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getPassword());
            pstmt.setString(3, user.getName());
            pstmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            DruidUtil.close(conn, pstmt);
        }
    }
    @Override
    public User login(String username, String password) {
        String sql = "SELECT * FROM user WHERE username = ? AND password = ?";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DruidUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setUsername(rs.getString("username"));
                user.setPassword(rs.getString("password"));
                user.setName(rs.getString("name"));
                return user;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            DruidUtil.close(conn, pstmt, rs);
        }
        return null;
    }
}
 
BookDAO.java
java
import com.book.entity.Book;
import java.util.List;
public interface BookDAO {
    // 添加图书
    void add(Book book);
    // 删除图书
    void delete(Integer id);
    // 修改图书
    void update(Book book);
    // 查询所有图书
    List<Book> findAll();
    // 根据 ID 查询
    Book findById(Integer id);
    // 扣减库存
    void reduceStock(Integer id);
}
 
BookDAOImpl.java
java
import com.book.dao.BookDAO;
import com.book.entity.Book;
import com.book.util.DruidUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
public class BookDAOImpl implements BookDAO {
    @Override
    public void add(Book book) {
        String sql = "INSERT INTO book (name, author, stock) VALUES (?, ?, ?)";
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = DruidUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, book.getName());
            pstmt.setString(2, book.getAuthor());
            pstmt.setInt(3, book.getStock());
            pstmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            DruidUtil.close(conn, pstmt);
        }
    }
    @Override
    public void delete(Integer id) {
        String sql = "DELETE FROM book WHERE id = ?";
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = DruidUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            DruidUtil.close(conn, pstmt);
        }
    }
    @Override
    public void update(Book book) {
        String sql = "UPDATE book SET name = ?, author = ?, stock = ? WHERE id = ?";
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = DruidUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, book.getName());
            pstmt.setString(2, book.getAuthor());
            pstmt.setInt(3, book.getStock());
            pstmt.setInt(4, book.getId());
            pstmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            DruidUtil.close(conn, pstmt);
        }
    }
    @Override
    public List<Book> findAll() {
        List<Book> list = new ArrayList<>();
        String sql = "SELECT * FROM book";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DruidUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                Book book = new Book();
                book.setId(rs.getInt("id"));
                book.setName(rs.getString("name"));
                book.setAuthor(rs.getString("author"));
                book.setStock(rs.getInt("stock"));
                list.add(book);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            DruidUtil.close(conn, pstmt, rs);
        }
        return list;
    }
    @Override
    public Book findById(Integer id) {
        String sql = "SELECT * FROM book WHERE id = ?";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DruidUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, id);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                Book book = new Book();
                book.setId(rs.getInt("id"));
                book.setName(rs.getString("name"));
                book.setAuthor(rs.getString("author"));
                book.setStock(rs.getInt("stock"));
                return book;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            DruidUtil.close(conn, pstmt, rs);
        }
        return null;
    }
    @Override
    public void reduceStock(Integer id) {
        String sql = "UPDATE book SET stock = stock - 1 WHERE id = ? AND stock > 0";
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = DruidUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            DruidUtil.close(conn, pstmt);
        }
    }
}
 
 
七、Service 层（业务逻辑）
UserService.java
java
import com.book.entity.User;
public interface UserService {
    void register(User user);
    User login(String username, String password);
}
 
UserServiceImpl.java
java
import com.book.dao.UserDAO;
import com.book.dao.impl.UserDAOImpl;
import com.book.entity.User;
import com.book.service.UserService;
public class UserServiceImpl implements UserService {
    private UserDAO userDAO = new UserDAOImpl();
    @Override
    public void register(User user) {
        userDAO.register(user);
    }
    @Override
    public User login(String username, String password) {
        return userDAO.login(username, password);
    }
}
 
BookService.java
java
import com.book.entity.Book;
import java.util.List;
public interface BookService {
    void add(Book book);
    void delete(Integer id);
    void update(Book book);
    List<Book> findAll();
    Book findById(Integer id);
    // 借阅图书（事务）
    void borrow(Integer userId, Integer bookId);
}
 
BookServiceImpl.java
java
import com.book.dao.BookDAO;
import com.book.dao.impl.BookDAOImpl;
import com.book.entity.Book;
import com.book.service.BookService;
import com.book.util.DruidUtil;
import java.sql.Connection;
import java.util.List;
public class BookServiceImpl implements BookService {
    private BookDAO bookDAO = new BookDAOImpl();
    @Override
    public void add(Book book) {
        bookDAO.add(book);
    }
    @Override
    public void delete(Integer id) {
        bookDAO.delete(id);
    }
    @Override
    public void update(Book book) {
        bookDAO.update(book);
    }
    @Override
    public List<Book> findAll() {
        return bookDAO.findAll();
    }
    @Override
    public Book findById(Integer id) {
        return bookDAO.findById(id);
    }
    @Override
    public void borrow(Integer userId, Integer bookId) {
        Connection conn = null;
        try {
            conn = DruidUtil.getConnection();
            // 开启事务
            conn.setAutoCommit(false);
            // 扣减库存
            bookDAO.reduceStock(bookId);
            // 记录借阅
            String sql = "INSERT INTO borrow (user_id, book_id) VALUES (?, ?)";
            var pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, userId);
            pstmt.setInt(2, bookId);
            pstmt.executeUpdate();
            // 提交事务
            conn.commit();
        } catch (Exception e) {
            // 回滚事务
            try {
                conn.rollback();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
        } finally {
            DruidUtil.close(conn);
        }
    }
}
 
 
八、测试类
UserTest.java
java
import com.book.entity.User;
import com.book.service.UserService;
import com.book.service.impl.UserServiceImpl;
public class UserTest {
    public static void main(String[] args) {
        UserService userService = new UserServiceImpl();
        // 注册
        User user = new User();
        user.setUsername("test");
        user.setPassword("123456");
        user.setName("测试用户");
        userService.register(user);
        // 登录
        User loginUser = userService.login("test", "123456");
        System.out.println(loginUser);
    }
}
 
BookTest.java
java
import com.book.entity.Book;
import com.book.service.BookService;
import com.book.service.impl.BookServiceImpl;
import java.util.List;
public class BookTest {
    public static void main(String[] args) {
        BookService bookService = new BookServiceImpl();
        // 添加图书
        Book book = new Book();
        book.setName("Java 编程思想");
        book.setAuthor("Bruce Eckel");
        book.setStock(10);
        bookService.add(book);
        // 查询所有图书
        List<Book> books = bookService.findAll();
        System.out.println(books);
        // 借阅图书
        bookService.borrow(1, 1);
    }
}
 
 
九、运行项目
1. 执行 SQL 脚本创建数据库和表
2. 配置  druid.properties  数据库连接信息
3. 运行  UserTest.java  测试用户注册/登录
4. 运行  BookTest.java  测试图书管理功能
 
十、JDBC 核心知识点总结
1. 连接池：Druid 实现连接复用，提升性能
2. 预编译 SQL： PreparedStatement  防止 SQL 注入
3. 事务控制： conn.setAutoCommit(false)  保证数据一致性
4. 资源关闭：确保连接、语句、结果集正确关闭
5. 分层开发：DAO、Service 分离，代码可维护
 
十一、企业级扩展方向
1. 加入分页查询
2. 实现图书归还功能
3. 加入密码加密（MD5）
4. 集成日志框架（log4j）
5. 实现 GUI 界面（Swing）
6. 部署到 Tomcat 作为 Web 项目
    本项目基于 JDBC 原生开发，代码规范、可扩展，是 Java 后端入门必备项目！
