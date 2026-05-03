03.26 19:14
JDBC详细知识点剖析
一、JDBC核心概述
1.1 什么是JDBC
JDBC（Java Database Connectivity，Java数据库连接）是Java语言中用于连接和操作关系型数据库的标准API，本质是Sun公司（现Oracle）定义的一套接口规范，用于屏蔽不同数据库（MySQL、Oracle、SQL Server等）的底层实现差异，让Java程序可以通过统一的代码操作各种关系型数据库。
简单来说，JDBC就是Java程序与数据库之间的“桥梁”，它提供了一套统一的方法，让开发者无需关注不同数据库的底层细节，只需调用标准接口，就能完成对数据库的增删改查（CRUD）等操作。
1.2 JDBC的核心作用
建立Java程序与数据库之间的连接；
向数据库发送SQL语句，执行增删改查、存储过程调用等操作；
处理数据库返回的结果集（查询操作）；
处理数据库操作过程中出现的异常；
管理数据库连接资源，避免资源泄露。
1.3 JDBC的依赖条件
使用JDBC操作数据库，需要两个核心依赖：
JDK自带的JDBC核心接口（java.sql包、javax.sql包），无需额外导入，属于JDK内置API；
对应数据库的JDBC驱动（Driver），是数据库厂商提供的实现类，用于适配JDBC接口，不同数据库的驱动不同（如MySQL的mysql-connector-java、Oracle的ojdbc），需要手动导入依赖。
二、JDBC核心组件（接口与类）
JDBC的核心功能通过一套接口实现，这些接口由数据库驱动厂商提供具体实现，开发者只需面向接口编程即可。核心组件主要包括以下5个，也是JDBC操作的核心对象：
2.1 Driver（驱动接口）
Driver是所有数据库驱动类必须实现的接口，位于java.sql包下，其核心作用是建立与数据库的连接，并返回Connection对象。
关键细节：
不同数据库的驱动类不同，例如MySQL的驱动类是com.mysql.cj.jdbc.Driver（MySQL 8.0+），Oracle的驱动类是oracle.jdbc.driver.OracleDriver；
驱动类加载方式：通过Class.forName("驱动类全路径")加载驱动，加载后会自动注册到DriverManager中（JDK 1.6+可省略加载步骤，DriverManager会自动扫描classpath下的驱动）；
Driver接口中只有一个核心方法：connect(String url, Properties info)，用于建立连接并返回Connection对象。
2.2 DriverManager（驱动管理类）
DriverManager是JDBC的驱动管理工具类，位于java.sql包下，核心作用是管理所有注册的驱动，并提供静态方法获取数据库连接（getConnection()）。
核心方法：
static Connection getConnection(String url, String user, String password)：最常用，通过数据库URL、用户名、密码获取连接；
static Connection getConnection(String url, Properties info)：通过URL和包含用户名、密码的Properties对象获取连接；
static void registerDriver(Driver driver)：手动注册驱动（一般无需手动调用，加载驱动类时会自动注册）；
static void deregisterDriver(Driver driver)：注销驱动，释放资源。
注意：DriverManager是线程安全的，但获取的Connection对象是线程不安全的，不能多线程共享一个Connection。
2.3 Connection（连接对象）
Connection是Java程序与数据库之间的“连接会话”，位于java.sql包下，核心作用是创建执行SQL的对象（Statement、PreparedStatement），管理事务，以及关闭连接。
核心方法：
Statement createStatement()：创建普通Statement对象，用于执行静态SQL语句；
PreparedStatement prepareStatement(String sql)：创建预处理Statement对象，用于执行动态SQL（带占位符?），可防止SQL注入；
CallableStatement prepareCall(String sql)：创建CallableStatement对象，用于调用数据库中的存储过程；
void setAutoCommit(boolean autoCommit)：设置事务是否自动提交（默认true，即执行SQL后自动提交事务；设置为false时，需手动调用commit()提交）；
void commit()：提交事务（仅当autoCommit为false时有效）；
void rollback()：回滚事务（发生异常时调用，撤销未提交的操作）；
void close()：关闭连接，释放资源（必须手动关闭，否则会导致数据库连接泄露）；
boolean isClosed()：判断连接是否已关闭。
2.4 Statement（SQL执行对象）
Statement是用于执行静态SQL语句的对象，由Connection创建，位于java.sql包下，适用于SQL语句固定、无参数的场景。
核心方法：
int executeUpdate(String sql)：执行DML语句（INSERT、UPDATE、DELETE），返回受影响的行数；执行DDL语句（CREATE、ALTER、DROP），返回0；
ResultSet executeQuery(String sql)：执行SELECT语句，返回查询结果集（ResultSet）；
boolean execute(String sql)：可执行任意SQL语句（DML、DDL、SELECT），返回true表示执行的是查询语句（需通过getResultSet()获取结果集），返回false表示执行的是DML/DDL语句（需通过getUpdateCount()获取受影响行数）；
void close()：关闭Statement对象，释放资源（需在Connection关闭前关闭）。
缺点：Statement执行动态SQL时，需要拼接字符串，容易引发SQL注入攻击（例如：SELECT * FROM user WHERE username='"+name+"'，若name为'or 1=1 --，则SQL变为SELECT * FROM user WHERE username=''or 1=1 --'，会查询所有用户数据），因此实际开发中很少使用。
2.5 PreparedStatement（预处理SQL执行对象）
PreparedStatement是Statement的子接口，位于java.sql包下，是实际开发中最常用的SQL执行对象，适用于SQL语句固定、参数可变的场景，可解决SQL注入问题，且支持SQL预编译，提升执行效率。
核心特点：
SQL预编译：创建PreparedStatement时，会将SQL语句（带?占位符）发送给数据库预编译，后续执行时只需传入参数，无需重新编译，多次执行同一SQL时效率更高；
参数化查询：通过setXxx(int parameterIndex, Xxx value)方法设置占位符参数（parameterIndex从1开始），避免字符串拼接，防止SQL注入；
继承Statement的所有方法，且新增了参数设置相关方法。
核心方法（新增参数设置方法）：
void setInt(int parameterIndex, int x)：设置int类型参数；
void setString(int parameterIndex, String x)：设置String类型参数；
void setDouble(int parameterIndex, double x)：设置double类型参数；
void setDate(int parameterIndex, Date x)：设置Date类型参数（java.sql.Date，对应数据库的DATE类型）；
void clearParameters()：清除已设置的参数。
注意：PreparedStatement的SQL语句中，占位符?只能用于替换“值”，不能替换表名、列名等标识符（例如：SELECT * FROM ? 是错误的）。
2.6 ResultSet（结果集对象）
ResultSet是执行SELECT语句后返回的结果集对象，位于java.sql包下，核心作用是存储查询到的数据，并提供方法遍历和获取数据。
核心细节：
ResultSet的游标：初始时游标位于第一行数据之前（beforeFirst()），通过next()方法移动游标，next()返回true表示存在下一行数据，false表示游标已到末尾；
数据获取方法：getXxx(int columnIndex)（通过列索引获取，索引从1开始）、getXxx(String columnLabel)（通过列名获取，推荐使用，避免列索引变化导致错误）；
常用获取方法：getInt()、getString()、getDouble()、getDate()、getBoolean()等，需与数据库列的类型对应；
结果集关闭：需在Statement关闭前关闭，调用close()方法，释放资源；
ResultSet的类型：默认是“只能向前遍历、不可更新”，可通过Connection的createStatement(int type, int concurrency)方法设置结果集类型（如可滚动、可更新）。
三、JDBC核心操作流程（完整版）
JDBC操作数据库的核心流程固定，无论增删改查，都遵循“加载驱动→建立连接→创建SQL执行对象→执行SQL→处理结果→释放资源”的步骤，以下以MySQL 8.0+为例，详细说明每一步的实现。
3.1 前提准备（导入驱动依赖）
使用Maven项目时，在pom.xml中导入MySQL驱动依赖（版本可根据实际情况调整）：
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <version>8.0.36</version>
</dependency>
3.2 完整操作步骤（以查询为例）
import java.sql.*;
public class JdbcDemo {
    public static void main(String[] args) {
        // 1. 定义数据库连接信息（URL、用户名、密码）
        String url = "jdbc:mysql://localhost:3306/test_db?useSSL=false&serverTimezone=UTC&characterEncoding=utf8";
        String username = "root";
        String password = "123456";
        // 声明核心对象（提升作用域，方便finally中关闭）
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            // 2. 加载驱动（MySQL 8.0+可省略，DriverManager会自动扫描）
            Class.forName("com.mysql.cj.jdbc.Driver");
            // 3. 建立数据库连接（通过DriverManager获取Connection）
            conn = DriverManager.getConnection(url, username, password);
            // 4. 编写SQL语句（带占位符，查询id=1的用户）
            String sql = "SELECT id, username, age FROM user WHERE id = ?";
            // 5. 创建PreparedStatement对象（预处理SQL）
            pstmt = conn.prepareStatement(sql);
            // 6. 设置占位符参数（index从1开始，id为int类型）
            pstmt.setInt(1, 1);
            // 7. 执行SQL（查询用executeQuery()，返回ResultSet）
            rs = pstmt.executeQuery();
            // 8. 处理结果集（遍历游标，获取数据）
            while (rs.next()) {
                // 通过列名获取数据（推荐）
                int id = rs.getInt("id");
                String name = rs.getString("username");
                int age = rs.getInt("age");
                // 输出结果
                System.out.println("id: " + id + ", username: " + name + ", age: " + age);
            }
        } catch (ClassNotFoundException e) {
            // 驱动加载异常（如驱动类路径错误）
            e.printStackTrace();
        } catch (SQLException e) {
            // 数据库操作异常（如URL错误、用户名密码错误、SQL语法错误等）
            e.printStackTrace();
        } finally {
            // 9. 释放资源（顺序：ResultSet → PreparedStatement → Connection，逆序关闭）
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
3.3 关键细节说明
数据库URL格式：jdbc:数据库类型://主机地址:端口号/数据库名?参数1&参数2&... 示例（MySQL 8.0+）：jdbc:mysql://localhost:3306/test_db?useSSL=false&serverTimezone=UTC&characterEncoding=utf8 核心参数：useSSL（是否启用SSL连接，开发环境建议关闭）、serverTimezone（时区，必须设置，否则会报错）、characterEncoding（字符编码，避免中文乱码）。
资源释放：必须在finally中关闭资源，且顺序是“ResultSet → Statement/PreparedStatement → Connection”，因为Connection依赖于Statement，Statement依赖于ResultSet，逆序关闭可避免资源泄露。
异常处理：JDBC操作中会抛出SQLException（数据库相关异常）和ClassNotFoundException（驱动加载异常），必须捕获或抛出，不能忽略。
增删改操作：只需将步骤7的executeQuery()改为executeUpdate()，无需处理ResultSet，返回受影响的行数。例如： // 新增用户SQL String sql = "INSERT INTO user(username, age) VALUES(?, ?)"; pstmt = conn.prepareStatement(sql); pstmt.setString(1, "zhangsan"); pstmt.setInt(2, 20); int rows = pstmt.executeUpdate(); // 返回新增的行数 System.out.println("新增了" + rows + "行数据");
四、JDBC事务管理
4.1 事务的核心概念
事务（Transaction）是数据库操作的最小单元，是一组不可分割的SQL操作，要么全部执行成功，要么全部执行失败（回滚），用于保证数据的一致性。
事务的ACID特性（必须掌握）：
原子性（Atomicity）：事务中的所有操作要么全部成功，要么全部失败，没有中间状态；
一致性（Consistency）：事务执行前后，数据库的数据状态保持一致（例如：转账时，转出账户减少的金额等于转入账户增加的金额）；
隔离性（Isolation）：多个事务并发执行时，一个事务的操作不会影响另一个事务的执行（避免脏读、不可重复读、幻读）；
持久性（Durability）：事务执行成功后，数据会永久保存到数据库中，即使数据库崩溃，数据也不会丢失。
4.2 JDBC事务的实现方式
JDBC的事务管理通过Connection对象实现，默认情况下，事务是自动提交的（autoCommit=true），即每执行一条SQL语句，就自动提交一次事务。若需要手动管理事务（多SQL语句作为一个事务），需按以下步骤操作：
获取Connection对象后，调用setAutoCommit(false)，关闭自动提交；
执行一组SQL操作（如转账：转出账户减钱、转入账户加钱）；
所有SQL执行成功后，调用commit()提交事务；
若执行过程中出现异常，在catch块中调用rollback()回滚事务，撤销所有已执行的操作；
无论成功与否，最终都要释放资源。
事务管理示例（转账场景）：
try {
    // 1. 建立连接
    conn = DriverManager.getConnection(url, username, password);
    // 2. 关闭自动提交，开启手动事务
    conn.setAutoCommit(false);
    // 3. 执行SQL1：转出账户减100元
    String sql1 = "UPDATE account SET balance = balance - 100 WHERE id = ?";
    pstmt = conn.prepareStatement(sql1);
    pstmt.setInt(1, 1);
    pstmt.executeUpdate();
    // 模拟异常（测试回滚）
    // int i = 1 / 0;
    // 4. 执行SQL2：转入账户加100元
    String sql2 = "UPDATE account SET balance = balance + 100 WHERE id = ?";
    pstmt = conn.prepareStatement(sql2);
    pstmt.setInt(1, 2);
    pstmt.executeUpdate();
    // 5. 所有SQL执行成功，提交事务
    conn.commit();
    System.out.println("转账成功！");
} catch (SQLException e) {
    // 6. 出现异常，回滚事务
    if (conn != null) {
        try {
            conn.rollback();
            System.out.println("转账失败，已回滚！");
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
    e.printStackTrace();
} finally {
    // 释放资源（略）
}
4.3 事务隔离级别
事务隔离级别用于解决多事务并发执行时的问题（脏读、不可重复读、幻读），JDBC提供了5种隔离级别（定义在Connection接口中），由低到高依次为：
TRANSACTION_NONE：不支持事务（很少使用）；
TRANSACTION_READ_UNCOMMITTED（读未提交）：最低隔离级别，允许读取未提交的事务数据，会导致脏读；
TRANSACTION_READ_COMMITTED（读已提交）：允许读取已提交的事务数据，避免脏读，会导致不可重复读（Oracle默认隔离级别）；
TRANSACTION_REPEATABLE_READ（可重复读）：保证同一事务中多次读取同一数据结果一致，避免脏读、不可重复读，会导致幻读（MySQL默认隔离级别）；
TRANSACTION_SERIALIZABLE（串行化）：最高隔离级别，所有事务串行执行，避免所有并发问题，但性能极低（很少使用）。
设置隔离级别的方法：conn.setTransactionIsolation(int level)，例如：conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
五、JDBC常见问题与解决方案
5.1 SQL注入问题
问题描述：使用Statement拼接SQL语句时，恶意用户通过输入特殊字符（如'or 1=1 --）篡改SQL逻辑，导致数据泄露或破坏。
解决方案：使用PreparedStatement代替Statement，通过占位符?设置参数，避免字符串拼接，PreparedStatement会自动对参数进行转义，防止SQL注入。
5.2 数据库连接泄露问题
问题描述：Connection、Statement、ResultSet等资源未正确关闭，导致数据库连接被占用，无法释放，最终导致数据库连接池耗尽，程序无法正常访问数据库。
解决方案：
将资源关闭代码放在finally块中，确保无论是否出现异常，资源都能被关闭；
关闭资源时，按“ResultSet → Statement → Connection”的顺序关闭；
实际开发中，使用数据库连接池（如Druid、C3P0）管理连接，避免手动创建和关闭连接。
5.3 中文乱码问题
问题描述：Java程序向数据库插入或查询中文数据时，出现乱码（如？？？）。
解决方案：
数据库URL中添加characterEncoding=utf8参数；
确保数据库、数据表、列的字符集为utf8或utf8mb4；
Java程序的编码格式为UTF-8（IDE中设置）。
5.4 驱动加载失败问题
问题描述：抛出ClassNotFoundException，提示找不到驱动类。
解决方案：
检查驱动类全路径是否正确（MySQL 8.0+是com.mysql.cj.jdbc.Driver，5.x是com.mysql.jdbc.Driver）；
检查驱动依赖是否导入成功（Maven项目检查pom.xml，非Maven项目检查是否添加驱动jar包）；
确保驱动版本与数据库版本匹配（如MySQL 8.0+需使用8.0+的驱动）。
六、JDBC高级特性
6.1 数据库连接池
核心背景：手动创建和关闭Connection对象效率极低（创建连接需要与数据库建立TCP连接，耗时较长），且容易出现连接泄露。数据库连接池是预先创建一定数量的数据库连接，存放在连接池中，程序需要时从池中获取连接，使用完毕后归还连接，避免频繁创建和关闭连接，提升程序性能。
常用连接池：
Druid（阿里出品，性能最优，最常用）：支持监控、防SQL注入、连接泄露检测等功能；
C3P0：老牌连接池，稳定性好，但性能略差；
HikariCP：Spring Boot默认连接池，性能优秀，轻量级。
连接池核心参数：初始连接数、最大连接数、最小空闲连接数、最大等待时间（获取连接的超时时间）。
6.2 CallableStatement（调用存储过程）
CallableStatement用于调用数据库中的存储过程，由Connection的prepareCall()方法创建，支持输入参数、输出参数和返回值。
示例（调用存储过程）：
// 存储过程：根据用户id查询用户名（out参数）
String sql = "{call get_username(?, ?)}"; // ?分别是输入参数（id）、输出参数（username）
CallableStatement cstmt = conn.prepareCall(sql);
// 设置输入参数
cstmt.setInt(1, 1);
// 注册输出参数（参数索引、参数类型）
cstmt.registerOutParameter(2, Types.VARCHAR);
// 执行存储过程
cstmt.execute();
// 获取输出参数的值
String username = cstmt.getString(2);
System.out.println("用户名：" + username);
6.3 批处理（Batch Processing）
当需要执行大量相同或相似的SQL语句（如批量插入、批量更新）时，使用批处理可以减少Java程序与数据库的交互次数，提升执行效率。
核心方法（PreparedStatement）：
addBatch()：将SQL语句添加到批处理队列中；
executeBatch()：执行批处理队列中的所有SQL语句，返回一个int数组，数组中的每个元素表示对应SQL执行受影响的行数；
clearBatch()：清空批处理队列。
示例（批量插入）：
String sql = "INSERT INTO user(username, age) VALUES(?, ?)";
pstmt = conn.prepareStatement(sql);
// 批量添加10条数据
for (int i = 1; i <= 10; i++) {
    pstmt.setString(1, "user" + i);
    pstmt.setInt(2, 18 + i);
    pstmt.addBatch(); // 添加到批处理队列
}
// 执行批处理
int[] rows = pstmt.executeBatch();
System.out.println("批量插入了" + rows.length + "条数据");
七、JDBC总结
JDBC是Java操作关系型数据库的基础，核心是“接口规范+驱动实现”，开发者面向接口编程，屏蔽数据库差异。掌握JDBC的核心组件（Driver、DriverManager、Connection、PreparedStatement、ResultSet）和操作流程，是后续学习ORM框架（如MyBatis、Hibernate）的基础。
核心重点：
PreparedStatement的使用（防SQL注入、预编译）；
事务管理（ACID特性、手动提交与回滚）；
资源释放（避免连接泄露）；
常见问题的解决方案（SQL注入、中文乱码等）。
实际开发中，JDBC很少直接使用，通常会结合数据库连接池和ORM框架，简化开发流程，但JDBC的底层原理和核心知识点，是理解ORM框架和数据库交互的关键。

