Bookstore应用访问数据库

---------------------------------------------------------------------------------------------------------------------------------------
一、Bookstore应用访问数据库的核心方式
Bookstore应用中数据库访问是核心环节，所有用户、书籍、购物车、订单数据的增删改查都依赖数据库操作，主流分为**原生JDBC**（入门）和**MyBatis/MyBatis-Plus**（企业级）两种方式，以下结合Bookstore场景详细说明。

### 方式1：原生JDBC（入门级，理解底层原理）
JDBC（Java Database Connectivity）是Java访问数据库的基础接口，直接操作数据库连接、SQL执行、结果集处理，适合新手理解底层流程。

#### 1. 核心步骤（以查询书籍为例）
```java
package com.example.bookstore.util;
import com.example.bookstore.entity.Book;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
// JDBC工具类：封装数据库连接和关闭
public class JDBCUtil {
    // 数据库连接信息
    private static final String URL = "jdbc:mysql://localhost:3306/bookstore?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASSWORD = "123456";
    // 加载驱动（MySQL 8.0+无需手动加载，驱动类名：com.mysql.cj.jdbc.Driver）
    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
    // 获取数据库连接
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
    // 关闭资源
    public static void close(Connection conn, PreparedStatement pstmt, ResultSet rs) {
        try {
            if (rs != null) rs.close();
            if (pstmt != null) pstmt.close();
            if (conn != null) conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
// 书籍数据访问层（DAO）：实现书籍查询
public class BookDAO {
    // 查询所有书籍
    public List<Book> findAllBooks() {
        List<Book> bookList = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        String sql = "SELECT id, name, author, price, stock, cover, description FROM book";
        try {
            // 1. 获取连接
            conn = JDBCUtil.getConnection();
            // 2. 预编译SQL
            pstmt = conn.prepareStatement(sql);
            // 3. 执行SQL，获取结果集
            rs = pstmt.executeQuery();
            // 4. 处理结果集
            while (rs.next()) {
                Book book = new Book();
                book.setId(rs.getInt("id"));
                book.setName(rs.getString("name"));
                book.setAuthor(rs.getString("author"));
                book.setPrice(rs.getDouble("price"));
                book.setStock(rs.getInt("stock"));
                book.setCover(rs.getString("cover"));
                book.setDescription(rs.getString("description"));
                bookList.add(book);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            // 5. 关闭资源
            JDBCUtil.close(conn, pstmt, rs);
        }
        return bookList;
    }
    // 新增书籍（示例：带参数的SQL）
    public void addBook(Book book) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        String sql = "INSERT INTO book(name, author, price, stock, cover, description) VALUES (?, ?, ?, ?, ?, ?)";
        try {
            conn = JDBCUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            // 设置参数（？占位符，索引从1开始）
            pstmt.setString(1, book.getName());
            pstmt.setString(2, book.getAuthor());
            pstmt.setDouble(3, book.getPrice());
            pstmt.setInt(4, book.getStock());
            pstmt.setString(5, book.getCover());
            pstmt.setString(6, book.getDescription());
            // 执行更新（增删改用executeUpdate，返回受影响行数）
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            JDBCUtil.close(conn, pstmt, null);
        }
    }
}
// 书籍实体类（JavaBean）
public class Book {
    private Integer id;
    private String name;
    private String author;
    private Double price;
    private Integer stock;
    private String cover;
    private String description;
    // 无参构造（必须）
    public Book() {}
    // getter和setter方法
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    public String getCover() { return cover; }
    public void setCover(String cover) { this.cover = cover; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
```

#### 2. JDBC核心要点（Bookstore场景）
- **预编译SQL**：使用`PreparedStatement`替代`Statement`，防止SQL注入（如用户登录时的账号密码拼接）；
- **事务处理**：订单生成时需保证"扣减库存+生成订单"原子性，示例：
  ```java
  // 订单生成的事务处理
  public void createOrder(Order order, List<OrderItem> itemList) {
      Connection conn = null;
      try {
          conn = JDBCUtil.getConnection();
          conn.setAutoCommit(false); // 关闭自动提交
          // 1. 插入订单
          String orderSql = "INSERT INTO `order`(id, user_id, total_price, address, status, create_time) VALUES (?, ?, ?, ?, ?, ?)";
          PreparedStatement orderPstmt = conn.prepareStatement(orderSql);
          // 设置订单参数...
          orderPstmt.executeUpdate();
          // 2. 插入订单项
          String itemSql = "INSERT INTO order_item(order_id, book_id, quantity, price) VALUES (?, ?, ?, ?)";
          PreparedStatement itemPstmt = conn.prepareStatement(itemSql);
          for (OrderItem item : itemList) {
              // 设置订单项参数...
              itemPstmt.executeUpdate();
              // 3. 扣减书籍库存
              String stockSql = "UPDATE book SET stock = stock - ? WHERE id = ?";
              PreparedStatement stockPstmt = conn.prepareStatement(stockSql);
              stockPstmt.setInt(1, item.getQuantity());
              stockPstmt.setInt(2, item.getBookId());
              stockPstmt.executeUpdate();
          }
          conn.commit(); // 提交事务
      } catch (SQLException e) {
          try {
              if (conn != null) conn.rollback(); // 回滚事务
          } catch (SQLException ex) {
              ex.printStackTrace();
          }
          e.printStackTrace();
      } finally {
          JDBCUtil.close(conn, null, null);
      }
  }
  ```
- **分页查询**：书籍列表分页，使用MySQL的`LIMIT`关键字：
  ```java
  // 分页查询书籍（pageNum：页码，pageSize：每页条数）
  public List<Book> findBooksByPage(int pageNum, int pageSize) {
      List<Book> bookList = new ArrayList<>();
      Connection conn = null;
      PreparedStatement pstmt = null;
      ResultSet rs = null;
      // LIMIT 偏移量, 条数（偏移量 = (页码-1)*每页条数）
      String sql = "SELECT * FROM book LIMIT ?, ?";
      try {
          conn = JDBCUtil.getConnection();
          pstmt = conn.prepareStatement(sql);
          pstmt.setInt(1, (pageNum - 1) * pageSize);
          pstmt.setInt(2, pageSize);
          rs = pstmt.executeQuery();
          // 处理结果集...
      } catch (SQLException e) {
          e.printStackTrace();
      } finally {
          JDBCUtil.close(conn, pstmt, rs);
      }
      return bookList;
  }
  ```

### 方式2：MyBatis（企业级，简化JDBC）
MyBatis是半自动化ORM框架，替代JDBC的重复代码，通过XML/注解映射SQL和Java对象，是Bookstore进阶版的主流选择。

#### 1. 核心配置与使用（Spring Boot整合）

##### （1）依赖引入（pom.xml）
```xml
<!-- Spring Boot整合MyBatis -->
<dependency>
    <groupId>org.mybatis.spring.boot</groupId>
    <artifactId>mybatis-spring-boot-starter</artifactId>
    <version>2.3.0</version>
</dependency>
<!-- MySQL驱动 -->
<dependency>
    <groupId>com.mysql.cj</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <scope>runtime</scope>
</dependency>
```

##### （2）配置数据库连接（application.yml）
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/bookstore?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC
    username: root
    password: 123456
    driver-class-name: com.mysql.cj.jdbc.Driver
mybatis:
  # 映射文件位置
  mapper-locations: classpath:mapper/*.xml
  # 实体类别名包
  type-aliases-package: com.example.bookstore.entity
  configuration:
    # 开启驼峰命名自动转换（如数据库字段book_name → 实体类bookName）
    map-underscore-to-camel-case: true
```

##### （3）Mapper接口（替代DAO类）
```java
package com.example.bookstore.mapper;
import com.example.bookstore.entity.Book;
import org.apache.ibatis.annotations.*;
import java.util.List;
// 书籍Mapper接口（MyBatis自动生成实现类）
@Mapper // 标记为MyBatis映射接口
public interface BookMapper {
    // 注解方式：查询所有书籍
    @Select("SELECT * FROM book")
    List<Book> findAll();
    // 注解方式：新增书籍
    @Insert("INSERT INTO book(name, author, price, stock, cover, description) VALUES (#{name}, #{author}, #{price}, #{stock}, #{cover}, #{description})")
    @Options(useGeneratedKeys = true, keyProperty = "id") // 自动返回主键
    void addBook(Book book);
    // XML方式：分页查询（推荐复杂SQL用XML）
    List<Book> findByPage(@Param("pageNum") int pageNum, @Param("pageSize") int pageSize);
    // 扣减库存（订单场景）
    @Update("UPDATE book SET stock = stock - #{num} WHERE id = #{bookId} AND stock >= #{num}")
    int reduceStock(@Param("bookId") Integer bookId, @Param("num") Integer num);
}
```

##### （4）XML映射文件（resources/mapper/BookMapper.xml）
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<!-- namespace对应Mapper接口全类名 -->
<mapper namespace="com.example.bookstore.mapper.BookMapper">
    <!-- 分页查询书籍 -->
    <select id="findByPage" resultType="Book">
        SELECT * FROM book LIMIT #{pageNum}, #{pageSize}
    </select>
</mapper>
```

##### （5）Service层调用（事务控制）
```java
package com.example.bookstore.service;
import com.example.bookstore.entity.Book;
import com.example.bookstore.mapper.BookMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.annotation.Resource;
import java.util.List;
@Service
public class BookService {
    @Resource
    private BookMapper bookMapper;
    // 查询所有书籍
    public List<Book> findAllBooks() {
        return bookMapper.findAll();
    }
    // 新增书籍
    public void addBook(Book book) {
        bookMapper.addBook(book);
    }
    // 分页查询
    public List<Book> findBooksByPage(int pageNum, int pageSize) {
        // 计算偏移量
        int offset = (pageNum - 1) * pageSize;
        return bookMapper.findByPage(offset, pageSize);
    }
    // 扣减库存（事务注解：保证原子性）
    @Transactional(rollbackFor = Exception.class)
    public boolean reduceStock(Integer bookId, Integer num) {
        int rows = bookMapper.reduceStock(bookId, num);
        return rows > 0; // 受影响行数>0表示扣减成功
    }
}
```

#### 2. MyBatis-Plus（进阶：简化CRUD）
MyBatis-Plus是MyBatis的增强工具，无需编写基础CRUD SQL，适合快速开发Bookstore应用：
```java
package com.example.bookstore.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.bookstore.entity.Book;
import org.apache.ibatis.annotations.Mapper;
// 继承BaseMapper，自动拥有CRUD方法
@Mapper
public interface BookMapper extends BaseMapper<Book> {
    // 仅需编写自定义SQL（如分页、复杂查询），基础CRUD无需编写
}
// Service层使用
@Service
public class BookService {
    @Resource
    private BookMapper bookMapper;
    // 分页查询（使用MyBatis-Plus分页插件）
    public IPage<Book> findBooksByPage(int pageNum, int pageSize) {
        Page<Book> page = new Page<>(pageNum, pageSize);
        return bookMapper.selectPage(page, null); // null表示无查询条件
    }
    // 新增书籍
    public void addBook(Book book) {
        bookMapper.insert(book); // 内置insert方法
    }
}
```

---------------------------------------------------------------------------------------------------------------------------------------
二、Bookstore数据库访问的核心注意事项
1. **连接池使用**
   - 原生JDBC手动管理连接效率低，企业中必用连接池（如HikariCP，Spring Boot默认），配置：
     ```yaml
     spring:
       datasource:
         hikari:
           maximum-pool-size: 10 # 最大连接数
           minimum-idle: 5 # 最小空闲连接
           connection-timeout: 30000 # 连接超时时间
     ```
2. **SQL注入防护**
   - 禁止拼接SQL字符串（如`"SELECT * FROM user WHERE username = '" + username + "'"`）；
   - 必须使用`PreparedStatement`（JDBC）或MyBatis的`#{}`占位符（而非`${}`）。
3. **事务一致性**
   - 订单生成、库存扣减等场景必须加事务，确保"要么全成，要么全败"；
   - Spring Boot中用`@Transactional`注解，指定`rollbackFor = Exception.class`（默认仅回滚运行时异常）。
4. **性能优化**
   - 分页查询：避免`SELECT *`，只查需要的字段；
   - 索引设计：用户表`username`、订单表`user_id`、购物车表`user_id+book_id`加索引；
   - 缓存：热门书籍数据存入Redis，减少数据库查询。
5. **异常处理**
   - 捕获数据库异常（如SQLSyntaxErrorException、DuplicateKeyException）；
   - 给用户友好提示（如"用户名已存在"、"库存不足"），而非直接抛出异常。

---------------------------------------------------------------------------------------------------------------------------------------

### 总结
1. Bookstore应用访问数据库有两种核心方式：原生JDBC（入门，理解底层）、MyBatis/MyBatis-Plus（企业级，简化开发）；
2. 核心场景（订单生成、库存扣减）需保证事务一致性，分页查询需使用`LIMIT`关键字或MyBatis分页插件；
3. 关键注意事项：使用连接池、防止SQL注入、加事务控制、优化查询性能；
4. 企业中优先选择MyBatis/MyBatis-Plus，配合Spring Boot的`@Transactional`实现高效、安全的数据库操作。
