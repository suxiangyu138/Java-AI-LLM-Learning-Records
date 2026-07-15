# Bookstore 应用访问数据库

> **定位**：Bookstore 应用中数据库访问是核心环节（用户/书籍/购物车/订单 CRUD）。分为原生 JDBC（理解底层）和 MyBatis/MyBatis-Plus（企业级）两种方式。

---

## 目录

1. [原生 JDBC 方式](#1-原生-jdbc-方式)
2. [MyBatis 方式](#2-mybatis-方式)
3. [MyBatis-Plus 方式](#3-mybatis-plus-方式)
4. [核心注意事项](#4-核心注意事项)

---

## 1. 原生 JDBC 方式

### 1.1 五步流程

| 步骤 | 操作 | 关键 API |
|:----:|------|----------|
| 1 | 获取连接 | `DriverManager.getConnection(url, user, pwd)` |
| 2 | 预编译 SQL | `conn.prepareStatement(sql)` |
| 3 | 执行查询 | `executeQuery()` / `executeUpdate()` |
| 4 | 处理结果集 | `rs.next()` + `rs.getXxx()` |
| 5 | 关闭资源 | `rs → pstmt → conn`（try-with-resources） |

### 1.2 JDBC 工具类

```java
public class JDBCUtil {
    private static final String URL = "jdbc:mysql://localhost:3306/bookstore?serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASSWORD = "123456";

    static { Class.forName("com.mysql.cj.jdbc.Driver"); }  // MySQL 8+ 可省略

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static void close(Connection conn, PreparedStatement pstmt, ResultSet rs) {
        try { if (rs != null) rs.close(); } catch (SQLException e) {}
        try { if (pstmt != null) pstmt.close(); } catch (SQLException e) {}
        try { if (conn != null) conn.close(); } catch (SQLException e) {}
    }
}
```

### 1.3 DAO 示例

```java
// 查询所有书籍
public List<Book> findAllBooks() {
    List<Book> list = new ArrayList<>();
    String sql = "SELECT id, name, author, price, stock FROM book";
    try (Connection conn = JDBCUtil.getConnection();
         PreparedStatement pstmt = conn.prepareStatement(sql);
         ResultSet rs = pstmt.executeQuery()) {
        while (rs.next()) {
            Book book = new Book();
            book.setId(rs.getInt("id"));
            book.setName(rs.getString("name"));
            list.add(book);
        }
    }
    return list;
}
```

### 1.4 事务处理（订单场景）

```java
public void createOrder(Order order, List<OrderItem> items) {
    Connection conn = JDBCUtil.getConnection();
    try {
        conn.setAutoCommit(false);          // 关闭自动提交
        // ① 插入订单
        // ② 插入订单项
        // ③ 扣减库存
        conn.commit();                       // 提交
    } catch (SQLException e) {
        conn.rollback();                     // 回滚
    } finally {
        JDBCUtil.close(conn, null, null);
    }
}
```

### 1.5 分页查询

```java
String sql = "SELECT * FROM book LIMIT ?, ?";
pstmt.setInt(1, (pageNum - 1) * pageSize);  // 偏移量
pstmt.setInt(2, pageSize);
```

---

## 2. MyBatis 方式

### 2.1 依赖

```xml
<dependency>
    <groupId>org.mybatis.spring.boot</groupId>
    <artifactId>mybatis-spring-boot-starter</artifactId>
    <version>2.3.0</version>
</dependency>
```

### 2.2 配置

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/bookstore?serverTimezone=UTC
    username: root
    password: 123456
mybatis:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: com.example.bookstore.entity
  configuration:
    map-underscore-to-camel-case: true
```

### 2.3 Mapper 接口

```java
@Mapper
public interface BookMapper {
    @Select("SELECT * FROM book")
    List<Book> findAll();

    @Insert("INSERT INTO book(name, author, price, stock) VALUES (#{name}, #{author}, #{price}, #{stock})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void addBook(Book book);

    @Update("UPDATE book SET stock = stock - #{num} WHERE id = #{bookId} AND stock >= #{num}")
    int reduceStock(@Param("bookId") Integer bookId, @Param("num") Integer num);

    List<Book> findByPage(@Param("pageNum") int pageNum, @Param("pageSize") int pageSize);
}
```

### 2.4 XML 映射

```xml
<mapper namespace="com.example.bookstore.mapper.BookMapper">
    <select id="findByPage" resultType="Book">
        SELECT * FROM book LIMIT #{pageNum}, #{pageSize}
    </select>
</mapper>
```

### 2.5 Service 层

```java
@Service
public class BookService {
    @Resource
    private BookMapper bookMapper;

    @Transactional(rollbackFor = Exception.class)
    public boolean reduceStock(Integer bookId, Integer num) {
        return bookMapper.reduceStock(bookId, num) > 0;
    }
}
```

---

## 3. MyBatis-Plus 方式

```java
@Mapper
public interface BookMapper extends BaseMapper<Book> {
    // 基础 CRUD 无需编写，仅写自定义 SQL
}

@Service
public class BookService {
    @Resource
    private BookMapper bookMapper;

    public IPage<Book> findBooksByPage(int pageNum, int pageSize) {
        return bookMapper.selectPage(new Page<>(pageNum, pageSize), null);
    }

    public void addBook(Book book) {
        bookMapper.insert(book);  // 内置 insert
    }
}
```

---

## 4. 核心注意事项

| 原则 | 说明 |
|------|------|
| **连接池** | Spring Boot 默认 HikariCP，配置 `maximum-pool-size` |
| **防 SQL 注入** | JDBC 用 `PreparedStatement`，MyBatis 用 `#{}`（禁用 `${}`） |
| **事务一致性** | `@Transactional(rollbackFor = Exception.class)` |
| **分页查询** | `LIMIT offset, size`，避免 `SELECT *` |
| **索引** | `username` 唯一索引、`user_id` 普通索引、`user_id+book_id` 联合索引 |
| **缓存** | 热门数据 Redis 缓存，减少 DB 查询 |
| **异常处理** | 捕获 `SQLException`/`DuplicateKeyException`，给用户友好提示 |

---

> 🎯 **总结**：Bookstore 数据库访问 = JDBC（底层原理）→ MyBatis（XML+注解映射）→ MyBatis-Plus（零 SQL CRUD）。核心关注点：连接池 + 防注入 + 事务 + 分页 + 索引。
