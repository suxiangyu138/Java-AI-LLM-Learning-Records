03.31 03:10
Java注解实战项目：自定义ORM框架（极简版MyBatis）
项目简介
通过自定义注解+反射实现一个轻量级ORM框架，支持自动建表、CRUD操作，完全基于注解驱动，深入理解注解原理与框架底层设计。
技术栈
- 核心：Java注解、反射、JDBC
- 数据库：MySQL 8.0
- 工具：Druid连接池、Lombok
- 规范：注解驱动、反射解析、ORM思想
项目结构
plaintext
annotation-orm/
├── src/main/java/com/orm
│   ├── annotation    // 自定义注解
│   ├── core          // 核心执行类（注解解析、SQL生成）
│   ├── entity        // 实体类（使用自定义注解）
│   ├── util          // 工具类（DB连接、反射）
│   └── test          // 测试类
└── resources
    └── db.properties // 数据库配置
 
完整代码实现
1. 自定义注解（核心）
@Table（表名注解）
java
import java.lang.annotation.*;
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Table {
    String value(); // 表名
}
 
@Id（主键注解）
java
import java.lang.annotation.*;
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Id {
    String value() default "id"; // 主键列名
}
 
@Column（字段注解）
java
import java.lang.annotation.*;
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Column {
    String value(); // 列名
}
 
2. 数据库配置（db.properties）
properties
driverClassName=com.mysql.cj.jdbc.Driver
url=jdbc:mysql://localhost:3306/annotation_orm?useSSL=false&serverTimezone=Asia/Shanghai
username=root
password=root
 
3. 数据库工具类（DBUtil.java）
java
import com.alibaba.druid.pool.DruidDataSourceFactory;
import javax.sql.DataSource;
import java.sql.Connection;
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
    public static Connection getConnection() {
        try {
            return dataSource.getConnection();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
 
4. 实体类（使用自定义注解）
User.java
java
import lombok.Data;
@Table("user")
@Data
public class User {
    @Id
    private Long id;
    @Column("username")
    private String username;
    @Column("password")
    private String password;
    @Column("age")
    private Integer age;
}
 
5. 核心ORM类（注解解析+SQL生成）
OrmCore.java
java
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
public class OrmCore {
    /**
     * 新增数据
     */
    public static <T> void insert(T entity) {
        Class<?> clazz = entity.getClass();
        // 解析@Table注解
        Table table = clazz.getAnnotation(Table.class);
        String tableName = table.value();
        // 解析字段
        Field[] fields = clazz.getDeclaredFields();
        List<String> columns = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        for (Field field : fields) {
            field.setAccessible(true);
            // 解析@Column注解
            Column column = field.getAnnotation(Column.class);
            if (column != null) {
                columns.add(column.value());
                try {
                    values.add(field.get(entity));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        // 生成SQL
        String sql = String.format("INSERT INTO %s (%s) VALUES (%s)",
                tableName,
                String.join(",", columns),
                "?,".repeat(values.size()).replaceAll(",$", ""));
        // 执行SQL
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < values.size(); i++) {
                pstmt.setObject(i + 1, values.get(i));
            }
            pstmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /**
     * 根据ID查询
     */
    public static <T> T selectById(Class<T> clazz, Long id) {
        // 解析@Table注解
        Table table = clazz.getAnnotation(Table.class);
        String tableName = table.value();
        // 解析@Id注解
        String idColumn = "id";
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            Id idAnno = field.getAnnotation(Id.class);
            if (idAnno != null) {
                idColumn = idAnno.value();
                break;
            }
        }
        // 生成SQL
        String sql = String.format("SELECT * FROM %s WHERE %s = ?", tableName, idColumn);
        // 执行查询并封装结果
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setObject(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                T entity = clazz.newInstance();
                for (Field field : fields) {
                    field.setAccessible(true);
                    Column column = field.getAnnotation(Column.class);
                    if (column != null) {
                        field.set(entity, rs.getObject(column.value()));
                    }
                }
                return entity;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    /**
     * 更新数据
     */
    public static <T> void update(T entity) {
        Class<?> clazz = entity.getClass();
        Table table = clazz.getAnnotation(Table.class);
        String tableName = table.value();
        Field[] fields = clazz.getDeclaredFields();
        List<String> setClauses = new ArrayList<>();
        Object idValue = null;
        for (Field field : fields) {
            field.setAccessible(true);
            // 处理主键
            Id idAnno = field.getAnnotation(Id.class);
            if (idAnno != null) {
                try {
                    idValue = field.get(entity);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                continue;
            }
            // 处理普通字段
            Column column = field.getAnnotation(Column.class);
            if (column != null) {
                try {
                    setClauses.add(column.value() + " = '" + field.get(entity) + "'");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        // 生成SQL
        String sql = String.format("UPDATE %s SET %s WHERE id = ?",
                tableName,
                String.join(",", setClauses));
        // 执行SQL
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setObject(1, idValue);
            pstmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /**
     * 删除数据
     */
    public static <T> void deleteById(Class<T> clazz, Long id) {
        Table table = clazz.getAnnotation(Table.class);
        String tableName = table.value();
        String sql = String.format("DELETE FROM %s WHERE id = ?", tableName);
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setObject(1, id);
            pstmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
 
6. 测试类（OrmTest.java）
java
public class OrmTest {
    public static void main(String[] args) {
        // 1. 新增用户
        User user = new User();
        user.setUsername("annotation");
        user.setPassword("123456");
        user.setAge(20);
        OrmCore.insert(user);
        // 2. 根据ID查询
        User queryUser = OrmCore.selectById(User.class, 1L);
        System.out.println("查询结果：" + queryUser);
        // 3. 更新用户
        queryUser.setAge(21);
        OrmCore.update(queryUser);
        // 4. 删除用户
        OrmCore.deleteById(User.class, 1L);
    }
}
 
7. 数据库SQL
sql
CREATE DATABASE IF NOT EXISTS annotation_orm DEFAULT CHARSET utf8mb4;
USE annotation_orm;
CREATE TABLE user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(100) NOT NULL,
    age INT
);
 
注解核心知识点
1. 元注解：@Target、@Retention、@Documented、@Inherited
2. 注解生命周期：
- SOURCE：源码阶段（如@Override）
- CLASS：编译阶段（默认）
- RUNTIME：运行时（反射解析）
3. 反射解析注解：
-  clazz.getAnnotation(Table.class) ：获取类注解
-  field.getAnnotation(Column.class) ：获取字段注解
4. 注解驱动开发：通过注解配置，减少硬编码
项目扩展方向
1. 实现自动建表（根据注解生成CREATE TABLE语句）
2. 支持复杂查询（WHERE、ORDER BY、LIMIT）
3. 加入事务控制
4. 实现结果集自动映射
5. 支持联表查询
6. 加入缓存机制
运行步骤
1. 执行SQL脚本创建库表
2. 配置db.properties数据库连接
3. 运行OrmTest测试类
4. 查看数据库数据变化
企业级应用场景
- Spring Boot：@SpringBootApplication、@RestController
- MyBatis：@Mapper、@Select、@Insert
- Spring MVC：@RequestMapping、@Autowired
- 自定义AOP：@Log、@Transactional
通过本项目，你将彻底掌握注解原理+反射解析，这是所有Java框架的底层核心！

