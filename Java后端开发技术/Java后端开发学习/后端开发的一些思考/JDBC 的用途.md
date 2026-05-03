03.13 16:41
JDBC 的用途
JDBC（Java Database Connectivity）是 Java 访问关系型数据库的标准 API，它提供了统一的接口，让 Java 程序可以连接 MySQL、Oracle、SQL Server 等不同数据库，实现数据库的增删改查（CRUD）操作，是 Java 后端开发中数据持久化的基础。
JDBC 基础使用方法（中文版 IntelliJ IDEA 操作）
前提准备
1. 安装并启动目标数据库（以 MySQL 8.0 为例），创建数据库  testdb  和表  user ：
sql
CREATE DATABASE IF NOT EXISTS testdb;
USE testdb;
CREATE TABLE IF NOT EXISTS user (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(20) NOT NULL,
    age INT
);
 
步骤 1：创建 Maven 项目并引入 MySQL 驱动依赖
1. 打开 IDEA → 文件 → 新建 → 项目 → 选择 Maven → 填写信息后完成创建。
2. 打开  pom.xml ，添加 MySQL 驱动依赖：
xml
<dependencies>
    <!-- MySQL 8.0 驱动 -->
    <dependency>
        <groupId>mysql</groupId>
        <artifactId>mysql-connector-java</artifactId>
        <version>8.0.33</version>
        <scope>runtime</scope>
    </dependency>
</dependencies>
 
3. 点击右侧 Maven → 刷新，下载依赖。
步骤 2：编写 JDBC 工具类（封装连接和关闭资源）
创建  com.example.jdbc.util.JDBCUtil.java ，封装数据库连接和资源释放逻辑，避免重复代码：
java
package com.example.jdbc.util;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
public class JDBCUtil {
    // 数据库连接信息（根据实际情况修改）
    private static final String URL = "jdbc:mysql://localhost:3306/testdb?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String USER = "root"; // 你的数据库用户名
    private static final String PASSWORD = "123456"; // 你的数据库密码
    // 注册驱动（MySQL 8.0 可省略，驱动会自动注册）
    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
    // 获取数据库连接
    public static Connection getConnection() {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return conn;
    }
    // 关闭资源（Statement + Connection）
    public static void close(Statement stmt, Connection conn) {
        if (stmt != null) {
            try {
                stmt.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (conn != null) {
            try {
                conn.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    // 关闭资源（ResultSet + Statement + Connection）
    public static void close(ResultSet rs, Statement stmt, Connection conn) {
        if (rs != null) {
            try {
                rs.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        close(stmt, conn);
    }
}
 
步骤 3：编写 CRUD 操作示例
创建  com.example.jdbc.JDBCDemo.java ，实现对  user  表的增删改查：
java
package com.example.jdbc;
import com.example.jdbc.util.JDBCUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
public class JDBCDemo {
    // 1. 新增用户
    public static void insertUser(String name, int age) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = JDBCUtil.getConnection();
            // 使用 PreparedStatement 防止 SQL 注入
            String sql = "INSERT INTO user(name, age) VALUES (?, ?)";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, name);
            pstmt.setInt(2, age);
            // 执行更新操作
            int rows = pstmt.executeUpdate();
            System.out.println("新增 " + rows + " 条数据");
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            JDBCUtil.close(pstmt, conn);
        }
    }
    // 2. 查询所有用户
    public static void queryAllUsers() {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = JDBCUtil.getConnection();
            String sql = "SELECT id, name, age FROM user";
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();
            // 遍历结果集
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                int age = rs.getInt("age");
                System.out.println("id: " + id + ", name: " + name + ", age: " + age);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            JDBCUtil.close(rs, pstmt, conn);
        }
    }
    public static void main(String[] args) {
        // 测试新增
        insertUser("张三", 20);
        // 测试查询
        queryAllUsers();
    }
}
 
步骤 4：运行测试
1. 确保 MySQL 服务已启动，且  user  表存在。
2. 运行  JDBCDemo  类的  main  方法，控制台会输出新增数据条数和查询到的用户信息。
核心关键点总结
1. 核心组件： DriverManager （管理驱动、获取连接）、 Connection （数据库连接）、 Statement/PreparedStatement （执行 SQL）、 ResultSet （存储查询结果）。
2. PreparedStatement 优势：相比  Statement ，可以防止 SQL 注入，且支持预编译，执行效率更高。
3. 资源释放：数据库连接是稀缺资源，使用后必须通过  close()  方法关闭，建议放在  finally  代码块中确保执行。
4. URL 格式：MySQL 8.0 的 URL 需要指定时区（ serverTimezone=UTC ），否则会报错。

