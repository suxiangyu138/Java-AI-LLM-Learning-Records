MyBatis入门教程
一、MyBatis简介
1.1 什么是MyBatis
MyBatis是一款优秀的Java持久层框架，前身是Apache的开源项目iBatis，2010年迁移到Google Code后更名为MyBatis，2013年11月迁移至GitHub托管。它的核心作用是简化JDBC开发，通过XML配置或注解的方式，将Java方法与SQL语句关联，实现Java对象（POJO）与数据库表数据的映射（ORM轻量级实现），避免了原生JDBC中加载驱动、创建连接、手动处理结果集等繁琐的模板代码，同时保留开发者对SQL的完全控制能力。
简单来说，MyBatis就是“帮我们写JDBC代码”的工具，让开发者无需关注底层数据库连接细节，专注于SQL逻辑和业务实现。
1.2 MyBatis的核心特点
简单易学：核心体积小，无第三方依赖，只需引入相关jar包并配置少量文件，即可快速上手，通过文档和源代码能轻松掌握其设计思路与实现方式。
灵活可控：不强制约束应用程序和数据库的现有设计，SQL语句可手动编写，便于统一管理和性能优化，能满足各类数据库操作需求。
解耦性强：通过DAO层将业务逻辑与数据访问逻辑分离，SQL语句与Java代码分离，提升代码的可维护性和可测试性。
强大的映射能力：支持对象与数据库表的ORM字段映射，可自动将查询结果转换为Java对象（POJO），也支持复杂的关联映射（一对一、一对多等）。
支持动态SQL：通过OGNL表达式实现动态SQL拼接，解决了JDBC中手动拼接SQL的繁琐与风险，简化多条件查询等场景的开发。
易集成：可与Spring、Spring Boot等主流框架无缝集成，同时支持分页插件（PageHelper）、代码生成器等工具，提升开发效率。
1.3 MyBatis的应用场景
MyBatis轻量灵活、SQL可控性强，适合以下场景：
需精细优化SQL性能的系统（如电商订单查询、金融交易统计）；
中小型Java项目或微服务（如用户中心、商品管理服务）；
数据库表结构稳定，但SQL逻辑频繁变化的场景（如报表系统、数据分析后台）；
需调用存储过程或复杂SQL语句的企业级系统（如ERP、CRM）。
二、入门准备（环境搭建）
2.1 环境要求
JDK：1.8及以上（推荐1.8）；
数据库：MySQL 5.7及以上（本文以MySQL为例）；
构建工具：Maven（推荐，简化依赖管理）；
开发工具：IDEA（或Eclipse）；
MyBatis版本：最新稳定版（本文以3.5.7为例，2021年4月发布的稳定版）。
2.2 Maven依赖配置
在Maven项目的pom.xml文件中，引入MyBatis核心依赖和MySQL驱动依赖，无需手动下载jar包，Maven会自动导入：
<!-- MyBatis核心依赖 -->
<dependency>
    <groupId>org.mybatis</groupId>
    <artifactId>mybatis</artifactId>
    <version>3.5.7</version>
</dependency>
<!-- MySQL驱动依赖（适配MySQL 8.0+，若为5.x版本，驱动类需调整） -->
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <version>8.0.28</version>
</dependency>
2.3 核心配置文件（mybatis-config.xml）
MyBatis的核心配置文件用于配置数据库连接信息、全局参数、映射器等，默认命名为mybatis-config.xml，放置在src/main/resources目录下，核心配置如下（可直接复制修改数据库信息）：
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE configuration
        PUBLIC "-//mybatis.org//DTD Config 3.0//EN"
        "https://mybatis.org/dtd/mybatis-3-config.dtd">
<configuration>
    <!-- 环境配置：可配置多个环境（开发、测试、生产），默认使用development环境 -->
    <environments default="development">
        <environment id="development">
            <!-- 事务管理器：MyBatis提供两种事务管理器，JDBC（依赖数据库事务）和MANAGED（交给容器管理） -->
            <transactionManager type="JDBC"/>
            <!-- 数据源：配置数据库连接信息，POOLED表示使用连接池（推荐，提升性能） -->
            <dataSource type="POOLED"><property name="driver" value="com.mysql.cj.jdbc.Driver"/> <!-- MySQL 8.0+驱动类 -->
                <property name="url" value="jdbc:mysql://localhost:3306/mybatis_db?serverTimezone=UTC&amp;useSSL=false"/> <!-- 数据库URL，替换为自己的数据库名 -->
<property name="username" value="root"/> <!-- 数据库用户名 -->
               <property name="password" value="123456"/> <!-- 数据库密码 -->
            </dataSource>
        </environment>
    </environments>
    <!-- 映射器配置：告诉MyBatis去哪里找SQL映射文件 -->
    <mappers>
        <!-- 方式1：单个映射文件引入（推荐，清晰可控） -->
        <mapper resource="com/example/mapper/UserMapper.xml"/>
       <!-- 方式2：包扫描（批量引入，需保证Mapper接口与XML文件同名同路径） -->
        <!-- <package name="com.example.mapper"/> -->
    </mappers>
</configuration>
说明：配置文件的结构有固定顺序，依次为properties、settings、typeAliases、typeHandlers、objectFactory、plugins、environments、databaseIdProvider、mappers，不可随意打乱顺序。
三、MyBatis核心组件与工作原理
3.1 核心组件
MyBatis的核心组件有3个，掌握其生命周期和作用，可避免内存泄漏，提升框架运行效率：
SqlSessionFactoryBuilder（构建者）：用于构建SqlSessionFactory实例，生命周期短暂，创建完SqlSessionFactory后即可销毁，通常在main方法或工具类中临时创建。
SqlSessionFactory（工厂）：MyBatis的核心，用于创建SqlSession实例，生命周期与应用程序一致，整个应用中只需创建一个实例（线程安全），负责管理数据库连接池。
SqlSession（会话）：用于执行具体的SQL操作，生命周期为一次请求或一个方法，线程不安全，使用完后必须关闭（可通过try-with-resources自动关闭），相当于JDBC中的Connection。
3.2 核心工作流程
MyBatis的工作流程可分为4个步骤，清晰易懂：
加载配置并初始化：加载mybatis-config.xml配置文件和SQL映射文件，将配置信息解析为MappedStatement对象（包含SQL语句、参数映射、结果映射等），存储在内存中。
接收调用请求：开发者通过SqlSession调用API，传入SQL的ID和参数对象，请求被传递给下层处理。
处理操作请求：根据SQL的ID找到对应的MappedStatement，解析参数得到最终执行的SQL，获取数据库连接执行SQL，将结果按映射规则转换为Java对象，最后释放连接。
返回处理结果：将转换后的Java对象返回给开发者，完成一次数据库操作。
四、入门案例（实现CRUD操作）
本案例以“用户表（user）”为例，实现基本的增删改查（CRUD）操作，完整演示MyBatis的使用流程。
4.1 步骤1：创建数据库表
在MySQL中创建mybatis_db数据库（与配置文件中URL的数据库名一致），并创建user表：
CREATE DATABASE IF NOT EXISTS mybatis_db;
USE mybatis_db;
CREATE TABLE IF NOT EXISTS user (
    id INT PRIMARY KEY AUTO_INCREMENT, -- 主键自增
    username VARCHAR(50) NOT NULL, -- 用户名
    password VARCHAR(50) NOT NULL, -- 密码
    age INT, -- 年龄
    email VARCHAR(100) -- 邮箱
);
4.2 步骤2：创建POJO实体类
创建与user表对应的Java实体类（POJO），属性名与表字段名保持一致（若不一致，可通过映射配置解决），放在com.example.pojo包下：
package com.example.pojo;
// 与user表对应的实体类
public class User {
    private Integer id;
    private String username;
    private String password;
    private Integer age;
    private String email;
    // 无参构造（MyBatis反射需要）
    public User() {}
    // 有参构造（可选，用于快速创建对象）
    public User(String username, String password, Integer age, String email) {
        this.username = username;
        this.password = password;
        this.age = age;
        this.email = email;
    }
    // getter和setter方法（必须，MyBatis通过setter注入数据）
    public Integer getId() {
        return id;
    }
    public void setId(Integer id) {
        this.id = id;
    }
    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public Integer getAge() {
        return age;
    }
    public void setAge(Integer age) {
        this.age = age;
    }
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    // toString方法（可选，用于打印对象信息）
    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", age=" + age +
                ", email='" + email + '\'' +
                '}';
    }
}
4.3 步骤3：创建Mapper接口
创建Mapper接口（相当于DAO接口），定义CRUD方法，放在com.example.mapper包下，MyBatis会自动生成接口的实现类（动态代理），无需手动编写：
package com.example.mapper;
import com.example.pojo.User;
import org.apache.ibatis.annotations.Param;
import java.util.List;
public interface UserMapper {
    // 1.查询所有用户
    List<User> findAll();
    // 2.根据id查询用户
    User findById(@Param("id") Integer id); // @Param用于指定参数名，与XML中#{}对应
    // 3.新增用户
    int addUser(User user);
    // 4.修改用户
    int updateUser(User user);
    // 5.删除用户
    int deleteUser(@Param("id") Integer id);
}
4.4 步骤4：创建SQL映射文件（UserMapper.xml）
SQL映射文件用于编写SQL语句，与Mapper接口对应，放在src/main/resources/com/example/mapper目录下（与接口包路径一致），命名为UserMapper.xml：
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "https://mybatis.org/dtd/mybatis-3-mapper.dtd">
<!-- namespace：必须与Mapper接口的全类名一致，建立接口与XML的关联 -->
<mapper namespace="com.example.mapper.UserMapper">
    <!-- 1.查询所有用户：id与接口方法名一致，resultType指定查询结果映射的实体类全类名 -->
    <!-- 2.根据id查询用户：#{}用于接收参数，相当于JDBC的?，避免SQL注入 -->
    <!-- 3.新增用户：useGeneratedKeys="true" keyProperty="id" 用于获取主键自增的值，赋值给User对象的id属性 -->
    <insert id="addUser" useGeneratedKeys="true" keyProperty="id">
        INSERT INTO user (username, password, age, email)
        VALUES (#{username}, #{password}, #{age}, #{email})
    </insert>
    <!-- 4.修改用户：根据id修改，参数为User对象，#{}直接获取对象的属性值 --><update id="updateUser">
        UPDATE user
        SET username = #{username}, password = #{password}, age = #{age}, email = #{email}
        WHERE id = #{id}
    </update>
    <!-- 5.删除用户：根据id删除 -->
    <delete id="deleteUser">
        DELETE FROM user WHERE id = #{id}
    </delete>
</mapper>
说明：SQL映射文件中的id必须与Mapper接口的方法名完全一致，否则MyBatis无法关联方法与SQL语句；#{}是预编译占位符，会自动对参数转义，避免SQL注入，优于JDBC的Statement。
4.5 步骤5：编写测试类
创建测试类，测试CRUD操作，放在src/test/java/com/example/test包下，使用try-with-resources自动关闭SqlSession：
package com.example.test;
import com.example.mapper.UserMapper;
import com.example.pojo.User;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.Test;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
public class MyBatisTest {
    // 测试查询所有用户
    @Test
    public void testFindAll() throws IOException {
        // 1.加载核心配置文件
        String resource = "mybatis-config.xml";
        InputStream inputStream = Resources.getResourceAsStream(resource);
        // 2.创建SqlSessionFactory
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        // 3.创建SqlSession（true表示自动提交事务，默认false需手动commit）
        try (SqlSession sqlSession = sqlSessionFactory.openSession(true)) {
            // 4.获取Mapper接口的代理对象
            UserMapper userMapper = sqlSession.getMapper(UserMapper.class);
            // 5.调用接口方法执行SQL
            List<User> userList = userMapper.findAll();
            // 6.遍历结果
            for (User user : userList) {
                System.out.println(user);
            }
        }
    }
    // 测试根据id查询用户
    @Test
    public void testFindById() throws IOException {
        InputStream inputStream = Resources.getResourceAsStream("mybatis-config.xml");
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
            UserMapper userMapper = sqlSession.getMapper(UserMapper.class);
            User user = userMapper.findById(1); // 假设数据库中有id=1的用户
            System.out.println(user);
        }
    }
    // 测试新增用户
    @Test
    public void testAddUser() throws IOException {
        InputStream inputStream = Resources.getResourceAsStream("mybatis-config.xml");
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        // 自动提交事务（新增、修改、删除需提交事务）
        try (SqlSession sqlSession = sqlSessionFactory.openSession(true)) {
            UserMapper userMapper = sqlSession.getMapper(UserMapper.class);
            // 创建用户对象
            User user = new User("zhangsan", "123456", 20, "zhangsan@163.com");
            // 调用新增方法，返回受影响的行数
            int rows = userMapper.addUser(user);
            System.out.println("新增成功，受影响行数：" + rows);
            System.out.println("新增用户的id：" + user.getId()); // 主键自增后的值
        }
    }
    // 测试修改用户
    @Test
    public void testUpdateUser() throws IOException {
        InputStream inputStream = Resources.getResourceAsStream("mybatis-config.xml");
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        try (SqlSession sqlSession = sqlSessionFactory.openSession(true)) {
            UserMapper userMapper = sqlSession.getMapper(UserMapper.class);
            // 查询要修改的用户
            User user = userMapper.findById(1);
            // 修改用户信息
            user.setUsername("lisi");
            user.setAge(22);
            // 调用修改方法
            int rows = userMapper.updateUser(user);
            System.out.println("修改成功，受影响行数：" + rows);
        }
    }
    // 测试删除用户
    @Test
    public void testDeleteUser() throws IOException {
        InputStream inputStream = Resources.getResourceAsStream("mybatis-config.xml");
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        try (SqlSession sqlSession = sqlSessionFactory.openSession(true)) {
            UserMapper userMapper = sqlSession.getMapper(UserMapper.class);
            // 调用删除方法
            int rows = userMapper.deleteUser(1);
            System.out.println("删除成功，受影响行数：" + rows);
        }
    }
}
4.6 测试注意事项
确保数据库连接信息（用户名、密码、数据库名）与实际环境一致，否则会报连接失败错误；
新增、修改、删除操作必须提交事务（openSession(true)自动提交，或手动调用sqlSession.commit()）；
Mapper接口与XML映射文件的namespace、方法名、参数名必须一致，否则会报“找不到方法”或“参数不匹配”错误；
若实体类属性与表字段名不一致，可通过<resultMap>标签配置映射关系（后续进阶内容）。
五、入门常见问题与解决方法
问题1：配置文件找不到原因：配置文件路径错误，或未放在resources目录下；解决：确保mybatis-config.xml在src/main/resources目录下，加载配置文件时路径正确（如示例中的"mybatis-config.xml"）。
问题2：SQL映射文件未被加载原因：mybatis-config.xml中mappers标签未配置映射文件，或路径错误；解决：检查mappers标签配置，确保resource路径与XML文件的实际路径一致（注意路径用/分隔，而非.）。
问题3：SQL注入风险原因：使用${}占位符（直接拼接SQL），而非#{}；解决：优先使用#{}，若需拼接SQL（如排序字段），需手动过滤参数，避免恶意输入。
问题4：主键自增后无法获取id原因：insert标签未配置useGeneratedKeys和keyProperty；解决：在insert标签中添加useGeneratedKeys="true" keyProperty="id"（id为实体类主键属性名）。
六、入门总结与进阶方向
6.1 入门总结
MyBatis入门核心是“3个核心组件+1个配置文件+1个映射文件”：
核心组件：SqlSessionFactoryBuilder（构建工厂）→ SqlSessionFactory（创建会话）→ SqlSession（执行SQL）；
核心配置文件：mybatis-config.xml（配置数据库、全局参数、映射器）；
SQL映射文件：XXXMapper.xml（编写SQL，关联Mapper接口）。
入门关键是掌握环境搭建、CRUD的基本写法，理解MyBatis的工作流程，重点区分#{}与${}的区别，避免常见错误。
6.2 进阶方向
掌握入门内容后，可进一步学习MyBatis进阶知识，满足复杂项目需求：
动态SQL（if、where、foreach等标签的使用）；
resultMap高级映射（解决属性与字段名不一致、关联查询等问题）；
缓存机制（一级缓存、二级缓存的配置与使用）；
MyBatis与Spring、Spring Boot集成（实战常用）；
分页插件（PageHelper）、代码生成器（MyBatis Generator）的使用；
注解开发（无需XML映射文件，直接在接口方法上写SQL）。
