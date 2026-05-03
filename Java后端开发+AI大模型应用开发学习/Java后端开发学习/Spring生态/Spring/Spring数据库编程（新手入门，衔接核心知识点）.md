03.20 18:00
Spring数据库编程（新手入门，衔接核心知识点）
结合此前学习的Spring IOC、Bean装配、AOP核心知识，Spring数据库编程是Spring框架在实际开发中的核心应用——通过Spring提供的模板类和注解，简化传统JDBC的繁琐操作，实现数据库的CRUD（增删改查），同时结合AOP实现事务控制，解决数据库操作中的冗余代码、资源泄漏、事务不一致等问题。
补充衔接：Spring数据库编程的核心是「将数据库操作相关的Bean（如数据源、模板类）交给Spring容器管理」，完全遵循IOC思想；事务控制则基于AOP实现，无需侵入业务代码，与此前AOP的“横切逻辑解耦”理念一致。
本教程仍基于Spring 5.x（适配JDK 8+），延续User案例，从环境搭建到实操案例，逐步讲解Spring数据库编程的3种核心方式（JDBC Template、注解式、XML式），新手重点掌握前2种（实际开发常用）。
一、Spring数据库编程核心前提（必做）
Spring数据库编程依赖「数据源（DataSource）」和「相关依赖包」，数据源是Spring连接数据库的核心，负责管理数据库连接池（避免频繁创建/关闭连接，提升性能）；依赖包则提供数据库操作的核心API，需先完成这两步准备，才能进行后续实操。
1. 导入核心依赖（Maven配置）
需导入4类依赖：Spring核心依赖、Spring JDBC依赖、数据库驱动（以MySQL 8.x为例）、连接池依赖（HikariCP，Spring默认推荐，轻量高效），直接复制到pom.xml中，与此前的AOP、Bean依赖共存：
<!-- 1. Spring核心依赖（此前已导入，无需重复） -->
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-context</artifactId>
    <version>5.3.28</version>
</dependency>
<!-- 2. Spring JDBC 核心依赖（数据库编程核心） -->
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-jdbc</artifactId>
    <version>5.3.28</version>
</dependency>
<!-- 3. MySQL驱动（适配MySQL 8.x，若用其他数据库替换对应驱动） -->
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <version>8.0.36</version>
</dependency>
<!-- 4. 连接池（HikariCP，Spring默认推荐，替代传统C3P0） -->
<dependency>
    <groupId>com.zaxxer</groupId>
    <artifactId>HikariCP</artifactId>
    <version>4.0.3</version>
</dependency>
<!-- 可选：Spring事务依赖（后续事务控制用，已包含在spring-jdbc中，可省略） -->
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-tx</artifactId>
    <version>5.3.28</version>
</dependency>
2. 配置数据源（DataSource）
数据源是Spring连接数据库的入口，负责配置数据库URL、用户名、密码、连接池参数等，Spring支持多种配置方式，新手重点掌握「XML配置」和「注解配置」（与此前Bean装配方式一致）。
前提：提前创建MySQL数据库（示例：数据库名spring_demo）和用户表（user），SQL语句如下（直接执行）：
-- 创建数据库
CREATE DATABASE IF NOT EXISTS spring_demo CHARACTER SET utf8mb4;
-- 使用数据库
USE spring_demo;
-- 创建用户表
CREATE TABLE IF NOT EXISTS user (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    age INT NOT NULL,
    gender VARCHAR(10) NOT NULL
);
方式1：XML配置（衔接此前XML配置，新手易理解）
在applicationContext.xml中配置数据源（交给Spring容器管理，作为Bean），同时配置JDBC模板类（JdbcTemplate，Spring提供的数据库操作模板，简化JDBC）：
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:context="http://www.springframework.org/schema/context"
       xmlns:aop="http://www.springframework.org/schema/aop"
       xmlns:tx="http://www.springframework.org/schema/tx"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
                           http://www.springframework.org/schema/beans/spring-beans.xsd
                           http://www.springframework.org/schema/context
                           http://www.springframework.org/schema/context/spring-context.xsd
                           http://www.springframework.org/schema/aop
                           http://www.springframework.org/schema/aop/spring-aop.xsd
                           http://www.springframework.org/schema/tx
                           http://www.springframework.org/schema/tx/spring-tx.xsd"&gt;
    <!-- 1. 注解扫描：扫描Bean、切面类（此前已配置，新增Dao、Service层扫描） -->
    <context:component-scan base-package="com.example"/>
    <!-- 2. 配置数据源（HikariCP），Bean id固定为dataSource（推荐） -->
    <bean id="dataSource" class="com.zaxxer.hikari.HikariDataSource"&gt;
        <!-- 数据库驱动类（MySQL 8.x必填，5.x可省略） -->
        &lt;property name="driverClassName" value="com.mysql.cj.jdbc.Driver"/&gt;
        <!-- 数据库URL（spring_demo是数据库名，useSSL=false避免安全警告，serverTimezone设置时区） -->
        <property name="jdbcUrl" value="jdbc:mysql://localhost:3306/spring_demo?useSSL=false&serverTimezone=UTC&characterEncoding=utf8"/>
        <!-- 数据库用户名（替换为自己的MySQL用户名，默认root） -->
        &lt;property name="username" value="root"/&gt;
        <!-- 数据库密码（替换为自己的MySQL密码） -->
        <property name="password" value="123456"/&gt;
        <!-- 连接池参数（可选，默认即可，新手无需修改） -->
        &lt;property name="maximumPoolSize" value="10"/&gt; <!-- 最大连接数 -->
        <property name="minimumIdle" value="5"/&gt; <!-- 最小空闲连接 -->
    </bean>
    <!-- 3. 配置JDBC模板类（Spring提供，简化数据库操作），依赖数据源 -->
    <bean id="jdbcTemplate" class="org.springframework.jdbc.core.JdbcTemplate"&gt;
        <!-- 注入数据源（setter方法注入，与此前Bean装配一致） -->
        <property name="dataSource" ref="dataSource"/>
    </bean>
</beans>
方式2：注解配置（简化，后续Spring Boot常用）
用配置类替代XML配置，通过@Bean注解配置数据源和JdbcTemplate，与此前AOP的注解配置逻辑一致：
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import javax.sql.DataSource;
@Configuration // 标识为配置类，替代XML
@ComponentScan("com.example") // 注解扫描
public class SpringDbConfig {
    // 配置数据源，@Bean标识该方法返回的对象是Spring管理的Bean
    @Bean
    public DataSource dataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setJdbcUrl("jdbc:mysql://localhost:3306/spring_demo?useSSL=false&serverTimezone=UTC&characterEncoding=utf8");
        dataSource.setUsername("root"); // 替换为自己的用户名
        dataSource.setPassword("123456"); // 替换为自己的密码
        dataSource.setMaximumPoolSize(10);
        dataSource.setMinimumIdle(5);
        return dataSource;
    }
    // 配置JdbcTemplate，注入数据源（参数为dataSource Bean，Spring自动装配）
    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
二、Spring数据库编程核心方式（3种，重点掌握前2种）
Spring数据库编程的核心是「通过JdbcTemplate或注解，简化JDBC操作」，无需手动加载驱动、创建连接、处理异常、关闭资源，Spring会自动完成这些操作，核心分为3种方式，结合实操案例讲解。
方式1：JdbcTemplate（最基础，新手入门首选）
JdbcTemplate是Spring提供的数据库操作模板类，封装了JDBC的所有繁琐操作，提供了CRUD相关的API，只需注入JdbcTemplate Bean，即可直接调用方法操作数据库，适合简单的数据库操作场景。
实操案例：基于JdbcTemplate实现User表CRUD
延续User案例，创建User实体类、Dao层（数据访问层），通过JdbcTemplate实现增删改查，全程遵循Bean装配逻辑（注解装配）。
步骤1：创建User实体类（对应数据库user表）
// com.example.entity.User.java
public class User {
    private Integer id;
    private String name;
    private Integer age;
    private String gender;
    // 无参构造（必需，Spring反射创建对象时需要）
    public User() {}
    // 有参构造（可选，方便创建对象）
    public User(String name, Integer age, String gender) {
        this.name = name;
        this.age = age;
        this.gender = gender;
    }
    // getter和setter方法（必需，Spring注入和查询结果封装需要）
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    // toString方法（方便打印查询结果）
    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", age=" + age +
                ", gender='" + gender + '\'' +
                '}';
    }
}
步骤2：创建Dao层（UserDao），注入JdbcTemplate
Dao层负责数据库操作，通过@Repository标识为Bean，注入JdbcTemplate，调用其API实现CRUD：
// com.example.dao.UserDao.java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository // 标识为Dao层Bean，交给Spring管理
public class UserDao {
    // 注入JdbcTemplate Bean（Spring自动装配，与此前@Autowired用法一致）
    @Autowired
    private JdbcTemplate jdbcTemplate;
    // 1. 新增用户（insert）
    public int addUser(User user) {
        // SQL语句：?是占位符，避免SQL注入（重点，不要拼接字符串）
        String sql = "INSERT INTO user(name, age, gender) VALUES(?, ?, ?)";
        // 调用update方法执行增删改操作，参数：SQL语句、占位符对应的值
        return jdbcTemplate.update(sql, user.getName(), user.getAge(), user.getGender());
    }
    // 2. 修改用户（update）
    public int updateUser(User user) {
        String sql = "UPDATE user SET name=?, age=?, gender=? WHERE id=?";
        return jdbcTemplate.update(sql, user.getName(), user.getAge(), user.getGender(), user.getId());
    }
    // 3. 删除用户（delete）
    public int deleteUser(Integer id) {
        String sql = "DELETE FROM user WHERE id=?";
        return jdbcTemplate.update(sql, id);
    }
    // 4. 根据ID查询单个用户（select）
    public User getUserById(Integer id) {
        String sql = "SELECT * FROM user WHERE id=?";
        // queryForObject：查询单个结果，BeanPropertyRowMapper自动将查询结果封装为User对象
        return jdbcTemplate.queryForObject(sql, new BeanPropertyRowMapper<>(User.class), id);
    }
    // 5. 查询所有用户（select）
    public List<User> getAllUser() {
        String sql = "SELECT * FROM user";
        // query：查询多个结果，封装为List<User>
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(User.class));
    }
}
步骤3：创建Service层（UserService），调用Dao层
Service层负责业务逻辑，通过@Service标识为Bean，注入UserDao，调用其方法（与此前Bean装配逻辑一致）：
// com.example.service.UserService.java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
@Service // 标识为Service层Bean
public class UserService {
    @Autowired
    private UserDao userDao;
    // 新增用户
    public int addUser(User user) {
        return userDao.addUser(user);
    }
    // 修改用户
    public int updateUser(User user) {
        return userDao.updateUser(user);
    }
    // 删除用户
    public int deleteUser(Integer id) {
        return userDao.deleteUser(id);
    }
    // 根据ID查询用户
    public User getUserById(Integer id) {
        return userDao.getUserById(id);
    }
    // 查询所有用户
    public List<User> getAllUser() {
        return userDao.getAllUser();
    }
}
步骤4：编写测试类，验证CRUD效果
// com.example.TestDb.java
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import java.util.List;
public class TestDb {
    public static void main(String[] args) {
        // 初始化Spring容器（加载XML配置，若用注解配置，替换为AnnotationConfigApplicationContext）
        ApplicationContext context = new ClassPathXmlApplicationContext("applicationContext.xml");
        // 获取UserService Bean（实际是代理对象，与AOP逻辑一致）
        UserService userService = context.getBean(UserService.class);
        // 1. 新增用户
        User user = new User("张三", 20, "男");
        int addCount = userService.addUser(user);
        System.out.println("新增用户数量：" + addCount);
        // 2. 查询所有用户
        List<User> userList = userService.getAllUser();
        System.out.println("所有用户：" + userList);
        // 3. 根据ID查询用户（假设新增的用户ID为1）
        User userById = userService.getUserById(1);
        System.out.println("根据ID查询的用户：" + userById);
        // 4. 修改用户（修改ID为1的用户）
        userById.setName("李四");
        userById.setAge(22);
        int updateCount = userService.updateUser(userById);
        System.out.println("修改用户数量：" + updateCount);
        // 5. 删除用户（删除ID为1的用户）
        int deleteCount = userService.deleteUser(1);
        System.out.println("删除用户数量：" + deleteCount);
    }
}
运行结果说明
运行main方法，控制台会依次输出新增、查询、修改、删除的结果，同时可在MySQL中查询user表，验证数据操作是否生效。核心注意：JdbcTemplate的update方法返回“影响的行数”，query方法自动封装查询结果为实体类，无需手动处理结果集。
方式2：注解式数据库编程（@Repository + @Autowired，实际开发主流）
注解式编程是在JdbcTemplate的基础上，进一步简化配置，核心是「通过注解标识Bean，自动装配依赖」，与此前Bean的注解装配、AOP注解逻辑完全一致，无需额外配置XML，仅需通过注解完成Dao层、Service层的Bean管理和依赖注入。
实操说明：上述JdbcTemplate案例，已经采用了注解式编程（@Repository、@Service、@Autowired），本质就是注解式数据库编程——Dao层、Service层通过注解标识为Bean，JdbcTemplate、UserDao通过@Autowired自动装配，无需手动配置XML的Bean标签，这是实际开发中最常用的方式。
补充优化：可通过@Value注解，将数据库连接参数（URL、用户名、密码）提取到配置文件中，避免硬编码（更规范），步骤如下：
// 1. 在src/main/resources下创建db.properties配置文件
jdbc.driver=com.mysql.cj.jdbc.Driver
jdbc.url=jdbc:mysql://localhost:3306/spring_demo?useSSL=false&serverTimezone=UTC&characterEncoding=utf8
jdbc.username=root
jdbc.password=123456
// 2. 在配置类中加载配置文件，通过@Value注入参数
@Configuration
@ComponentScan("com.example")
@PropertySource("classpath:db.properties") // 加载配置文件
public class SpringDbConfig {
    @Value("${jdbc.driver}")
    private String driverClassName;
    @Value("${jdbc.url}")
    private String jdbcUrl;
    @Value("${jdbc.username}")
    private String username;
    @Value("${jdbc.password}")
    private String password;
    @Bean
    public DataSource dataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName(driverClassName);
        dataSource.setJdbcUrl(jdbcUrl);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        return dataSource;
    }
    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
方式3：XML式数据库编程（传统方式，了解即可）
XML式编程是通过XML配置文件，手动配置Dao层Bean、Service层Bean，以及依赖注入，与此前Bean的手动装配逻辑一致，无需使用注解，适合老旧项目或不允许使用注解的场景，新手了解即可。
核心配置（在applicationContext.xml中添加）：
<!-- 配置UserDao Bean，注入JdbcTemplate -->
<bean id="userDao" class="com.example.dao.UserDao">
    <property name="jdbcTemplate" ref="jdbcTemplate"/>
</bean>
<!-- 配置UserService Bean，注入UserDao -->
<bean id="userService" class="com.example.service.UserService">
    <property name="userDao" ref="userDao"/>
</bean>
说明：该方式与JdbcTemplate案例的逻辑一致，仅将注解替换为XML配置，实操步骤相同，无需修改Java代码，仅需配置XML即可。
三、核心补充：Spring事务控制（基于AOP，必掌握）
数据库操作中，事务是核心需求（如“转账”操作，扣钱和加钱必须同时成功或同时失败），Spring的事务控制基于AOP实现，无需侵入业务代码，通过注解或XML配置即可完成，与此前AOP的横切逻辑解耦理念一致。
1. 事务控制核心注解（@Transactional）
在Service层的方法上添加@Transactional注解，即可实现事务控制，Spring会通过AOP，在方法执行前开启事务，执行成功提交事务，执行异常回滚事务。
@Service
public class UserService {
    @Autowired
    private UserDao userDao;
    // 给方法添加@Transactional，标识该方法需要事务控制
    @Transactional
    public void transfer() {
        // 模拟转账：用户1扣钱，用户2加钱（两个操作必须同时成功/失败）
        userDao.updateMoney(1, -100); // 用户1扣100
        int i = 1/0; // 模拟异常，事务应回滚，两个操作都不生效
        userDao.updateMoney(2, 100); // 用户2加100
    }
}
2. 开启事务注解支持（关键）
与AOP注解类似，事务注解需要开启支持，二选一即可：
XML配置：在applicationContext.xml中添加<tx:annotation-driven/>（需添加tx命名空间，此前XML配置已包含）。
注解配置：在配置类上添加@EnableTransactionManagement注解。
四、新手避坑重点（高频错误，提前规避）
数据库驱动版本不匹配：MySQL 8.x使用com.mysql.cj.jdbc.Driver，MySQL 5.x使用com.mysql.jdbc.Driver，否则会报“驱动类找不到”错误。
数据源配置错误：URL、用户名、密码写错，导致无法连接数据库（重点检查数据库名、端口号、密码是否正确）。
忘记注入依赖：JdbcTemplate未注入DataSource、UserDao未注入JdbcTemplate、UserService未注入UserDao，导致报NullPointerException。
SQL语句错误：占位符数量与参数数量不匹配、表名/字段名写错，导致SQL执行失败（重点检查SQL语句）。
事务不生效：未开启事务注解支持（@EnableTransactionManagement或<tx:annotation-driven/>），或@Transactional注解添加在Dao层（应添加在Service层方法上）。
连接池依赖缺失：未导入HikariCP依赖，导致数据源无法创建，报错“找不到HikariDataSource类”。
实体类缺少无参构造或getter/setter：导致JdbcTemplate无法封装查询结果，报“无法实例化实体类”错误。
五、实际应用场景与拓展
1. 常见应用场景
基础CRUD：通过JdbcTemplate实现简单的增删改查，适合小型项目。
事务控制：如转账、订单提交等场景，确保多个数据库操作原子性（同时成功或同时失败）。
批量操作：通过JdbcTemplate的batchUpdate方法，实现批量新增、批量修改，提升效率。
2. 进阶拓展（后续学习）
Spring JDBC进阶：使用NamedParameterJdbcTemplate，支持命名占位符（如:name），简化参数传递。
ORM框架集成：Spring可集成MyBatis、Hibernate等ORM框架，进一步简化数据库编程（MyBatis是目前企业开发主流）。
事务进阶：配置事务隔离级别、传播行为，解决复杂业务场景的事务问题。
六、实操总结（新手必练）
1. 核心流程：导入依赖 → 配置数据源 → 配置JdbcTemplate → 编写实体类 → Dao层注入JdbcTemplate实现CRUD → Service层调用Dao层 → 测试验证。
2. 关键衔接：Spring数据库编程完全遵循IOC（Bean管理）和AOP（事务控制）思想，与此前的Bean装配、AOP知识点紧密关联，核心是“将数据库相关Bean交给Spring管理，实现解耦”。
3. 新手练习：修改案例中的SQL语句，实现复杂查询（如按条件查询、分页查询）；测试事务控制，模拟异常场景，验证事务回滚效果；尝试注解配置数据源和JdbcTemplate，熟悉注解式编程。
掌握Spring数据库编程的基础后，后续学习MyBatis与Spring的集成会更加轻松，Spring数据库编程也是JavaWeb、微服务开发中不可或缺的核心技能。

