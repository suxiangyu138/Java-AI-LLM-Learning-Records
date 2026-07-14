# JDBC详细知识点剖析（标准GitHub Markdown）
## 一、JDBC核心概述
### 1.1 什么是JDBC
JDBC（Java Database Connectivity，Java数据库连接）是Oracle定义的**Java操作关系型数据库统一标准API**，属于一套接口规范。
作用：屏蔽 MySQL、Oracle、SQLServer 等数据库底层差异，一套代码适配所有关系型数据库；本质是Java程序和数据库之间的通信桥梁。

### 1.2 JDBC四大核心功能
1. 建立Java与数据库TCP连接
2. 发送执行CRUD、DDL、存储过程SQL
3. 接收、解析查询结果集 ResultSet
4. 处理数据库异常、管理连接资源

### 1.3 使用JDBC必备两大依赖
1. JDK内置核心包：`java.sql`、`javax.sql`，无需额外引入
2. 数据库厂商驱动Jar（实现JDBC接口）
    - MySQL8.0+：`mysql-connector-java`，驱动类`com.mysql.cj.jdbc.Driver`
    - MySQL5.x：驱动类`com.mysql.jdbc.Driver`
    - Oracle：`ojdbc`，驱动类`oracle.jdbc.driver.OracleDriver`

## 二、JDBC五大核心核心组件（接口/工具类）
### 2.1 Driver 驱动接口
所有数据库驱动必须实现该接口，负责建立数据库连接。
- 核心方法：`Connection connect(String url, Properties info)`
- 加载规则：JDK1.6+ SPI自动注册驱动，可省略`Class.forName()`手动加载

### 2.2 DriverManager 驱动管理工具类
统一管理所有注册驱动，对外提供静态方法获取连接。
常用静态方法：
```java
// 传入url、账号、密码获取连接
static Connection getConnection(String url, String user, String password)
// 使用配置对象传参
static Connection getConnection(String url, Properties props)
```
注意：`DriverManager`线程安全；但`Connection`非线程安全，禁止多线程共用同一个连接。

### 2.3 Connection 数据库连接会话
代表Java与数据库单次会话，是所有SQL执行对象的工厂，负责事务控制。
核心方法：
```java
// 创建静态SQL执行对象
Statement createStatement()
// 创建预编译对象（开发首选，防注入）
PreparedStatement prepareStatement(String sql)
// 调用存储过程
CallableStatement prepareCall(String sql)

// 事务控制
void setAutoCommit(boolean flag) // 关闭自动提交开启手动事务
void commit() // 提交事务
void rollback() // 事务回滚

void close() // 关闭连接释放资源
```

### 2.4 Statement 静态SQL执行对象
适用于无参数固定SQL，**存在SQL注入漏洞，项目禁止使用**。
核心方法：
```java
// 执行增删改/DDL，返回受影响行数
int executeUpdate(String sql)
// 执行查询，返回结果集
ResultSet executeQuery(String sql)
// 兼容所有SQL，返回boolean标识是否为查询
boolean execute(String sql)
```
漏洞原理：字符串拼接SQL，恶意参数篡改查询逻辑。

### 2.5 PreparedStatement 预编译执行对象（企业标准）
`Statement`子接口，开发唯一推荐使用，两大核心优势：
1. **防止SQL注入**：使用`?`占位符，底层自动转义特殊字符，禁止篡改SQL结构
2. **SQL预编译缓存**：数据库缓存编译模板，多次执行同结构SQL性能更高

参数赋值规则：占位符下标**从1开始**
```java
setInt(int index, int val)
setString(int index, String val)
setDate(int index, java.sql.Date date)
clearParameters() // 清空已赋值参数
```
限制：`?`仅能替换字段值，不能替换表名、字段名、关键字。

### 2.6 ResultSet 查询结果集
存储SELECT返回的数据表，游标初始在首行数据前。
1. 游标遍历：`boolean next()` 下移一行，有数据返回true
2. 获取数据两种方式（推荐列名，适配字段顺序变动）
    - `getInt("id")` 通过字段名
    - `getString(2)` 通过列索引（从1开始）
3. 必须手动`close()`释放，关闭顺序早于Statement、Connection

## 三、JDBC完整标准操作流程（MySQL8.0）
### 3.1 Maven驱动依赖
```xml
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <version>8.0.36</version>
</dependency>
```

### 3.2 MySQL标准URL参数说明
```
jdbc:mysql://localhost:3306/test_db?useSSL=false&serverTimezone=UTC&characterEncoding=utf8&allowMultiQueries=true
```
- `serverTimezone=UTC`：MySQL8强制配置，解决时区报错
- `characterEncoding=utf8`：统一编码，防止中文乱码
- `useSSL=false`：开发环境关闭SSL校验

### 3.3 完整查询示例（标准try-finally释放资源）
```java
import java.sql.*;

public class JdbcQueryDemo {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/test_db?serverTimezone=UTC";
        String user = "root";
        String pwd = "123456";

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            // 1. 获取连接
            conn = DriverManager.getConnection(url, user, pwd);
            // 2. 编写带占位符SQL
            String sql = "SELECT id,username,age FROM user WHERE id = ?";
            pstmt = conn.prepareStatement(sql);
            // 3. 填充占位符
            pstmt.setInt(1, 1);
            // 4. 执行查询
            rs = pstmt.executeQuery();
            // 5. 遍历结果集
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("username");
                int age = rs.getInt("age");
                System.out.printf("id:%d 姓名:%s 年龄:%d%n", id, name, age);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            // 6. 逆序释放资源 ResultSet -> PreparedStatement -> Connection
            try {
                if (rs != null) rs.close();
                if (pstmt != null) pstmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
```

### 3.4 增删改操作区别
增删改调用 `int executeUpdate()`，返回受影响行数，无ResultSet：
```java
String insertSql = "INSERT INTO user(username,age) VALUES(?,?)";
pstmt = conn.prepareStatement(insertSql);
pstmt.setString(1, "张三");
pstmt.setInt(2, 22);
int affectRows = pstmt.executeUpdate();
System.out.println("新增行数：" + affectRows);
```

## 四、JDBC事务管理
### 4.1 事务四大ACID特性
1. **原子性 Atomic**：一组SQL全部成功/全部回滚，无中间状态
2. **一致性 Consistency**：事务前后业务数据逻辑一致（转账总额不变）
3. **隔离性 Isolation**：并发事务互不干扰，解决脏读、不可重复读、幻读
4. **持久性 Durability**：事务提交后数据永久落地，宕机不丢失

### 4.2 JDBC手动事务标准模板（转账案例）
JDBC默认`autoCommit=true`，单条SQL自动提交；多SQL事务需关闭自动提交。
```java
try {
    conn = DriverManager.getConnection(url, user, pwd);
    // 关闭自动提交，开启手动事务
    conn.setAutoCommit(false);

    // 转出100
    String sql1 = "UPDATE account SET balance = balance - 100 WHERE id = ?";
    pstmt = conn.prepareStatement(sql1);
    pstmt.setInt(1, 1);
    pstmt.executeUpdate();

    // 转入100
    String sql2 = "UPDATE account SET balance = balance + 100 WHERE id = ?";
    pstmt = conn.prepareStatement(sql2);
    pstmt.setInt(1, 2);
    pstmt.executeUpdate();

    // 全部执行无异常，提交事务
    conn.commit();
    System.out.println("转账成功");
} catch (SQLException e) {
    // 异常触发事务回滚
    if (conn != null) {
        conn.rollback();
        System.out.println("转账失败，已回滚");
    }
    e.printStackTrace();
} finally {
    // 释放资源
}
```

### 4.3 五大事务隔离级别（Connection常量）
由低到高，隔离越强并发性能越差：
1. `TRANSACTION_READ_UNCOMMITTED` 读未提交：存在脏读、不可重复读、幻读
2. `TRANSACTION_READ_COMMITTED` 读已提交：Oracle默认，消除脏读
3. `TRANSACTION_REPEATABLE_READ` 可重复读：MySQL InnoDB默认，消除脏读、不可重复读
4. `TRANSACTION_SERIALIZABLE` 串行化：全部并发问题消除，性能极低
5. `TRANSACTION_NONE` 不支持事务

设置方式：
```java
conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
```

## 五、JDBC高级特性
### 5.1 批处理 Batch（大批量插入/更新）
减少Java与数据库网络交互，提升批量操作性能
```java
String sql = "INSERT INTO user(username,age) VALUES(?,?)";
pstmt = conn.prepareStatement(sql);
for (int i = 1; i <= 100; i++) {
    pstmt.setString(1, "user" + i);
    pstmt.setInt(2, 18);
    pstmt.addBatch(); // 添加至批处理队列
}
int[] resArr = pstmt.executeBatch(); // 批量执行
```

### 5.2 CallableStatement 调用存储过程
```java
// {call 存储过程名(入参?, 出参?)}
String procSql = "{call get_user_name(?, ?)}";
CallableStatement cstmt = conn.prepareCall(procSql);
cstmt.setInt(1, 1);
// 注册输出参数类型
cstmt.registerOutParameter(2, Types.VARCHAR);
cstmt.execute();
// 获取输出值
String name = cstmt.getString(2);
```

### 5.3 数据库连接池（生产必备）
#### 痛点
原生JDBC频繁创建/关闭Connection，TCP握手开销大，极易出现连接泄露。
#### 主流连接池
1. HikariCP：SpringBoot默认，轻量高性能
2. Druid（阿里）：国内首选，内置监控、防注入、泄露检测
3. C3P0：老牌稳定，性能偏弱

核心参数：初始连接数、最大连接数、最小空闲连接、获取连接超时时间

## 六、高频问题与解决方案
### 6.1 SQL注入
- 原因：Statement字符串拼接SQL
- 解决：全程使用PreparedStatement占位符`?`

### 6.2 数据库连接泄露
- 原因：ResultSet、PreparedStatement、Connection未关闭
- 规范：所有资源在finally逆序关闭；生产环境使用连接池自动回收

### 6.3 中文乱码
1. URL添加`characterEncoding=utf8`
2. 数据库、数据表、字段统一字符集`utf8mb4`
3. IDE、项目文件编码UTF-8

### 6.4 驱动类找不到 ClassNotFoundException
1. MySQL8驱动类`com.mysql.cj.jdbc.Driver`，5.x无`cj`包
2. Maven依赖未导入、Jar包缺失
3. 驱动版本与MySQL服务版本不匹配

## 七、面试核心背诵总结
1. JDBC是接口规范，厂商驱动提供实现，统一操作各类数据库；
2. 五大核心对象：Driver、DriverManager、Connection、PreparedStatement、ResultSet；
3. PreparedStatement优势：预编译缓存、彻底防止SQL注入；禁止使用Statement；
4. 资源关闭顺序：ResultSet → PreparedStatement → Connection，必须写在finally；
5. 事务ACID、四大隔离级别，MySQL默认可重复读；
6. 原生JDBC性能差，企业开发统一使用连接池（Druid/HikariCP）；
7. JDBC是MyBatis、JPA等ORM框架底层基础，所有持久层框架底层均封装JDBC。