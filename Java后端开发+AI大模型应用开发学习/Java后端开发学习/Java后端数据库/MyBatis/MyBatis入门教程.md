03.20 16:16
MyBatis入门教程
一、MyBatis简介
1.1 什么是MyBatis
MyBatis是一款基于Java的半自动化ORM（对象关系映射）持久层框架，前身是Apache的开源项目iBatis，2010年迁移至Google Code并更名为MyBatis，2013年又迁移至Github托管。它的核心作用是简化数据库操作，避免了传统JDBC编程中的硬编码和结果集解析繁琐问题，通过XML或注解将SQL语句与Java对象进行映射，实现数据的持久化操作。
与JDBC相比，MyBatis无需手动加载驱动、创建连接、处理结果集，同时保留了SQL的灵活性，允许开发者精细控制SQL语句，平衡了SQL控制权与开发效率，避免了全自动ORM框架的“黑盒”问题。
1.2 MyBatis核心特点
简单易学：体积小、无第三方依赖，只需引入相关jar包并完成简单配置即可使用，易于理解和上手。
灵活可控：SQL语句可独立存储在XML文件中，支持动态修改，无需重新编译代码，便于统一管理和优化。
解耦性强：实现SQL与Java代码的分离，将业务逻辑与数据访问逻辑拆分，提高系统可维护性和可测试性。
自动化映射：通过ResultMap自动将数据库查询结果集（ResultSet）转换为Java实体类（POJO）对象，减少手动解析工作。
支持动态SQL：基于OGNL表达式解析动态标签，可根据传入参数动态生成SQL语句，适配复杂查询场景。
集成便捷：支持与Spring、Spring Boot等框架无缝集成，同时内置连接池管理，避免频繁创建/销毁连接的性能损耗。
1.3 适用场景
MyBatis适合需要精细控制SQL、复杂报表查询、遗留系统改造的场景；不适合超简单CRUD操作、无SQL经验的团队或纯NoSQL架构的项目。
二、MyBatis环境搭建（IDEA+Maven）
2.1 环境准备
开发工具：IntelliJ IDEA
构建工具：Maven（管理依赖，简化配置）
数据库：MySQL（本文以MySQL 8.0为例）
JDK：1.8及以上版本
MyBatis版本：3.5.x（最新稳定版）
2.2 步骤1：创建Maven项目
打开IDEA，新建Maven项目，选择“Create New Project”，勾选“Create from archetype”，选择“maven-archetype-quickstart”（快速构建Java项目）。
填写GroupId（项目组织唯一标识）、ArtifactId（项目唯一标识）、Version（版本），点击Next，完成项目创建。
删除默认生成的src/main/java下的默认包和类，以及src/test/java下的测试类（后续按需创建）。
2.3 步骤2：导入依赖（pom.xml）
在pom.xml文件中添加MyBatis、MySQL驱动、日志（可选，用于调试）的依赖，依赖如下（版本可根据实际需求调整）：
&lt;dependencies&gt;
    <!-- MyBatis核心依赖 -->
    <dependency>
        <groupId>org.mybatis</groupId>
        <artifactId>mybatis</artifactId>
        <version>3.5.10&lt;/version&gt;
    &lt;/dependency&gt;
    <!-- MySQL驱动依赖（适配MySQL 8.0） -->
    <dependency>
        <groupId>mysql</groupId>
        <artifactId>mysql-connector-java</artifactId>
        <version>8.0.30</version>
    &lt;/dependency&gt;
    <!-- 日志依赖（SLF4J+Logback，用于打印SQL执行日志，可选） -->
    <dependency>
        <groupId>ch.qos.logback</groupId>
        <artifactId>logback-classic</artifactId>
        <version>1.4.5</version>
    </dependency>
    <!-- JUnit测试依赖（可选） -->
    <dependency>
        <groupId>junit</groupId>
        <artifactId>junit</artifactId>
        <version>4.13.2</version>
        <scope>test</scope>
    </dependency>
</dependencies>
添加完成后，点击IDEA右侧“Maven”，点击“Reload All Maven Projects”，加载依赖包。
2.4 步骤3：创建数据库和表
打开MySQL，创建数据库（例如mybatis_db）和测试表（例如user），SQL语句如下：
-- 创建数据库
CREATE DATABASE IF NOT EXISTS mybatis_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- 使用数据库
USE mybatis_db;
-- 创建user表
CREATE TABLE IF NOT EXISTS user (
    id INT PRIMARY KEY AUTO_INCREMENT,  -- 主键自增
    username VARCHAR(50) NOT NULL,     -- 用户名
    password VARCHAR(50) NOT NULL,     -- 密码
    age INT,                           -- 年龄
    email VARCHAR(100)                 -- 邮箱
);
-- 插入测试数据
INSERT INTO user (username, password, age, email) VALUES
('zhangsan', '123456', 20, 'zhangsan@163.com'),
('lisi', '654321', 22, 'lisi@163.com');
2.5 步骤4：编写MyBatis核心配置文件（mybatis-config.xml）
核心配置文件用于配置数据库连接、MyBatis全局参数、加载映射文件等，是MyBatis的入口配置。
在src/main/resources目录下，新建文件mybatis-config.xml，文件内容如下（注意标签顺序，需遵循MyBatis DTD约束）：
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE configuration
        PUBLIC "-//mybatis.org//DTD Config 3.0//EN"
        "https://mybatis.org/dtd/mybatis-3-config.dtd">
<configuration&gt;
    <!-- 1. 加载外部属性文件（可选，用于分离数据库配置，解耦） -->
    <properties resource="db.properties"&gt;&lt;/properties&gt;
    <!-- 2. 全局配置参数（可选） -->
    &lt;settings&gt;
        <!-- 开启驼峰命名自动转换（数据库列user_name对应实体类属性userName） -->
        <setting name="mapUnderscoreToCamelCase" value="true"/&gt;
        <!-- 开启日志（使用SLF4J，便于调试SQL） -->
        <setting name="logImpl" value="SLF4J"/&gt;
        <!-- 开启二级缓存（默认开启，可省略） -->
        <setting name="cacheEnabled" value="true"/>
    </settings&gt;
    <!-- 3. 实体类别名配置（可选，简化全类名书写） -->
    &lt;typeAliases&gt;
        <!-- 方式1：单个实体类配置别名 -->
        <typeAlias type="com.example.pojo.User" alias="User"/>
        <!-- 方式2：批量配置（推荐，为指定包下所有实体类生成别名，默认别名是类名首字母小写） -->
        <package name="com.example.pojo"/&gt;
    &lt;/typeAliases&gt;
    <!-- 4. 环境配置（核心，配置数据库连接信息） -->
    <environments default="development"&gt;
        &lt;environment id="development"&gt;
            <!-- 事务管理器：JDBC（使用JDBC自带的事务管理） -->
            &lt;transactionManager type="JDBC"/&gt;
            <!-- 数据源：POOLED（MyBatis内置连接池，推荐使用） -->
            <dataSource type="POOLED">
                <property name="driver" value="${jdbc.driver}"/>
                <property name="url" value="${jdbc.url}"/>
                <property name="username" value="${jdbc.username}"/>
                <property name="password" value="${jdbc.password}"/>
            </dataSource>
        </environment&gt;
    &lt;/environments&gt;
    <!-- 5. 加载Mapper映射文件（核心，关联SQL语句和Java接口） -->
    <mappers>
        <!-- 方式1：单个映射文件配置 -->
        <mapper resource="com/example/mapper/UserMapper.xml"/&gt;
        <!-- 方式2：批量配置（推荐，扫描指定包下所有Mapper接口） -->
        <package name="com.example.mapper"/>
    </mappers>
</configuration>
2.6 步骤5：编写数据库配置文件（db.properties，可选）
为了分离数据库配置，避免硬编码在mybatis-config.xml中，新建db.properties文件（src/main/resources目录下），内容如下：
# MySQL 8.0 驱动（注意与MySQL版本匹配）
jdbc.driver=com.mysql.cj.jdbc.Driver
# 数据库URL（mybatis_db是数据库名，serverTimezone指定时区，避免时区异常）
jdbc.url=jdbc:mysql://localhost:3306/mybatis_db?serverTimezone=UTC&useUnicode=true&characterEncoding=utf8
# 数据库用户名（替换为自己的MySQL用户名）
jdbc.username=root
# 数据库密码（替换为自己的MySQL密码）
jdbc.password=123456
三、MyBatis核心组件与工作流程
3.1 核心组件
SqlSessionFactoryBuilder（构建者）：用于解析mybatis-config.xml配置文件，创建SqlSessionFactory实例，采用建造者模式，创建完成后可销毁，无需长期保存。
SqlSessionFactory（工厂）：用于创建SqlSession实例，采用工厂模式，全局只需要一个实例（单例模式），负责管理数据库连接相关配置。
SqlSession（会话）：代表与数据库的一次交互，是MyBatis的核心接口，提供执行SQL、获取Mapper接口的方法，每个SqlSession对应一次数据库会话，用完需关闭（可通过try-with-resources自动关闭）。
Mapper接口（映射器）：一个接口，无需编写实现类，MyBatis通过动态代理生成其实现类，接口中的方法与Mapper.xml中的SQL语句一一对应，是开发者操作数据库的入口。
Executor（执行器）：SqlSession内部的SQL执行器，负责执行SQL语句，包含Simple、Reuse、Batch三种类型，分别对应简单执行、复用Statement、批量执行三种模式。
3.2 核心工作流程
加载配置：SqlSessionFactoryBuilder加载mybatis-config.xml配置文件，解析环境、数据源、Mapper映射等信息，同时加载外部属性文件和Mapper.xml映射文件。
创建SqlSessionFactory：通过SqlSessionFactoryBuilder的build()方法，创建SqlSessionFactory实例，初始化MyBatis核心配置。
创建SqlSession：通过SqlSessionFactory的openSession()方法，创建SqlSession实例，默认不自动提交事务（需手动调用commit()提交），可通过openSession(true)设置自动提交。
获取Mapper接口：通过SqlSession的getMapper()方法，获取Mapper接口的动态代理对象，代理对象会根据接口方法定位对应的SQL语句。
执行SQL：调用Mapper接口的方法，MyBatis解析SQL语句，进行参数映射，通过Executor执行SQL，获取结果集后进行结果映射，转换为Java实体类对象。
提交事务与关闭会话：执行完SQL后，若涉及增删改操作，需调用SqlSession的commit()方法提交事务，最后关闭SqlSession，释放资源（try-with-resources可自动关闭）。
四、MyBatis入门实战（CRUD操作）
本节通过实现User表的CRUD（增删改查）操作，演示MyBatis的基本使用，全程遵循“实体类→Mapper接口→Mapper.xml→测试”的流程。
4.1 步骤1：编写实体类（POJO）
在src/main/java下创建包com.example.pojo，新建User类，属性与数据库user表的字段对应，提供getter、setter方法和toString()方法：
package com.example.pojo;
/**
 * 实体类：与数据库user表对应
 */
public class User {
    private Integer id;
    private String username;
    private String password;
    private Integer age;
    private String email;
    // getter和setter方法
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
    // toString()方法，便于打印结果
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
4.2 步骤2：编写Mapper接口
在src/main/java下创建包com.example.mapper，新建UserMapper接口，定义CRUD方法，方法名与Mapper.xml中的SQL语句id对应：
package com.example.mapper;
import com.example.pojo.User;
import org.apache.ibatis.annotations.Param;
import java.util.List;
/**
 * Mapper接口：操作user表的入口，无需编写实现类
 */
public interface UserMapper {
    // 1. 根据id查询用户
    User selectUserById(@Param("id") Integer id);
    // 2. 查询所有用户
    List<User> selectAllUser();
    // 3. 添加用户
    int insertUser(User user);
    // 4. 修改用户
    int updateUser(User user);
    // 5. 根据id删除用户
    int deleteUserById(@Param("id") Integer id);
}
说明：@Param注解用于给参数命名，当接口方法有多个参数时，必须添加该注解，便于Mapper.xml中引用参数。
4.3 步骤3：编写Mapper.xml映射文件
Mapper.xml用于编写SQL语句，与Mapper接口关联，实现SQL与Java代码的分离。在src/main/resources下创建目录com/example/mapper（与Mapper接口包结构一致），新建UserMapper.xml文件：
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "https://mybatis.org/dtd/mybatis-3-mapper.dtd"&gt;
<!-- namespace：关联对应的Mapper接口（全类名） -->
&lt;mapper namespace="com.example.mapper.UserMapper"&gt;
    <!-- 1. 根据id查询用户 -->
    <select id="selectUserById" resultType="com.example.pojo.User">
        SELECT * FROM user WHERE id = #{id}
    &lt;/select&gt;
    <!-- 2. 查询所有用户 -->
    <select id="selectAllUser" resultType="com.example.pojo.User">
        SELECT * FROM user
    &lt;/select&gt;
    <!-- 3. 添加用户 -->
    <insert id="insertUser" parameterType="com.example.pojo.User">
        INSERT INTO user (username, password, age, email)
        VALUES (#{username}, #{password}, #{age}, #{email})
    </insert>
    <!-- 4. 修改用户 -->
    <update id="updateUser" parameterType="com.example.pojo.User">
        UPDATE user
        SET username = #{username}, password = #{password}, age = #{age}, email = #{email}
        WHERE id = #{id}
    &lt;/update&gt;
    <!-- 5. 根据id删除用户 -->
    <delete id="deleteUserById" parameterType="java.lang.Integer">
        DELETE FROM user WHERE id = #{id}
    </delete>
</mapper>
关键标签说明：
namespace：必须是对应的Mapper接口的全类名，用于关联接口和映射文件。
select/update/insert/delete：对应SQL的查询/修改/添加/删除操作，id必须与Mapper接口中的方法名一致。
resultType：指定查询结果的返回类型（实体类全类名或别名），用于结果集映射。
parameterType：指定传入参数的类型（实体类全类名、基本数据类型或别名），可省略（MyBatis会自动推断）。
#{ }：用于引用参数值，MyBatis会自动进行参数类型转换，避免SQL注入。
4.4 步骤4：编写工具类（简化SqlSession获取，可选）
每次操作数据库都需要加载配置、创建SqlSessionFactory和SqlSession，可编写工具类MyBatisUtils，简化代码：
package com.example.utils;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import java.io.IOException;
import java.io.InputStream;
/**
 * MyBatis工具类：获取SqlSession实例
 */
public class MyBatisUtils {
    // 全局唯一的SqlSessionFactory实例（单例）
    private static SqlSessionFactory sqlSessionFactory;
    // 静态代码块：加载配置文件，初始化SqlSessionFactory
    static {
        try {
            // 1. 加载mybatis-config.xml配置文件
            String resource = "mybatis-config.xml";
            InputStream inputStream = Resources.getResourceAsStream(resource);
            // 2. 创建SqlSessionFactory实例
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    // 提供方法：获取SqlSession实例
    public static SqlSession getSqlSession() {
        // openSession(true)：自动提交事务；默认openSession()：不自动提交事务
        return sqlSessionFactory.openSession(true);
    }
}
4.5 步骤5：编写测试类
在src/test/java下创建包com.example.test，新建UserMapperTest类，测试CRUD操作（使用JUnit测试）：
package com.example.test;
import com.example.mapper.UserMapper;
import com.example.pojo.User;
import com.example.utils.MyBatisUtils;
import org.apache.ibatis.session.SqlSession;
import org.junit.Test;
import java.util.List;
public class UserMapperTest {
    // 测试：根据id查询用户
    @Test
    public void testSelectUserById() {
        // 1. 获取SqlSession实例
        try (SqlSession sqlSession = MyBatisUtils.getSqlSession()) {
            // 2. 获取Mapper接口代理对象
            UserMapper userMapper = sqlSession.getMapper(UserMapper.class);
            // 3. 调用接口方法，执行SQL
            User user = userMapper.selectUserById(1);
            // 4. 打印结果
            System.out.println(user);
        }
    }
    // 测试：查询所有用户
    @Test
    public void testSelectAllUser() {
        try (SqlSession sqlSession = MyBatisUtils.getSqlSession()) {
            UserMapper userMapper = sqlSession.getMapper(UserMapper.class);
            List<User> userList = userMapper.selectAllUser();
            // 遍历输出所有用户
            for (User user : userList) {
                System.out.println(user);
            }
        }
    }
    // 测试：添加用户
    @Test
    public void testInsertUser() {
        try (SqlSession sqlSession = MyBatisUtils.getSqlSession()) {
            UserMapper userMapper = sqlSession.getMapper(UserMapper.class);
            // 创建用户对象
            User user = new User();
            user.setUsername("wangwu");
            user.setPassword("111111");
            user.setAge(25);
            user.setEmail("wangwu@163.com");
            // 调用添加方法，返回受影响的行数
            int rows = userMapper.insertUser(user);
            System.out.println("添加成功，受影响行数：" + rows);
        }
    }
    // 测试：修改用户
    @Test
    public void testUpdateUser() {
        try (SqlSession sqlSession = MyBatisUtils.getSqlSession()) {
            UserMapper userMapper = sqlSession.getMapper(UserMapper.class);
            // 创建用户对象（设置要修改的id和新属性）
            User user = new User();
            user.setId(3);
            user.setUsername("wangwu666");
            user.setPassword("666666");
            user.setAge(26);
            user.setEmail("wangwu666@163.com");
            // 调用修改方法
            int rows = userMapper.updateUser(user);
            System.out.println("修改成功，受影响行数：" + rows);
        }
    }
    // 测试：删除用户
    @Test
    public void testDeleteUserById() {
        try (SqlSession sqlSession = MyBatisUtils.getSqlSession()) {
            UserMapper userMapper = sqlSession.getMapper(UserMapper.class);
            // 调用删除方法
            int rows = userMapper.deleteUserById(3);
            System.out.println("删除成功，受影响行数：" + rows);
        }
    }
}
4.6 测试结果说明
运行每个测试方法，控制台会打印SQL执行日志（因配置了SLF4J日志）和执行结果。
添加、修改、删除操作，若工具类中openSession(true)，则自动提交事务，无需手动调用commit()；若为openSession()，则需手动调用sqlSession.commit()提交事务，否则操作不会生效。
查询操作无需提交事务，执行后直接返回结果。
五、入门常见问题与注意事项
5.1 常见问题
问题1：找不到mybatis-config.xml或Mapper.xml文件？ 解决：检查文件路径是否正确（src/main/resources目录下），Maven项目中，resources目录下的文件会自动打包，若路径错误，需调整文件位置或修改mybatis-config.xml中的mapper加载路径。
问题2：Mapper接口与Mapper.xml关联失败？ 解决：确保Mapper.xml的namespace是Mapper接口的全类名，SQL标签的id与接口方法名一致，参数类型、返回类型匹配。
问题3：数据库连接失败？ 解决：检查db.properties中的驱动类、URL、用户名、密码是否正确，MySQL服务是否启动，URL中是否添加时区参数（serverTimezone=UTC）。
问题4：查询结果为null，但SQL语句在数据库中能正常执行？ 解决：检查实体类属性与数据库字段是否对应（大小写、字段名），可开启驼峰命名转换（mapUnderscoreToCamelCase），或通过ResultMap手动配置映射关系。
5.2 注意事项
MyBatis核心配置文件（mybatis-config.xml）的标签有严格的顺序，必须遵循：properties → settings → typeAliases → typeHandlers → objectFactory → objectWrapperFactory → reflectorFactory → plugins → environments → databaseIdProvider → mappers，顺序错误会导致解析失败。
SqlSession是线程不安全的，不能共享，每次操作数据库都应获取新的SqlSession实例，用完及时关闭（try-with-resources可自动关闭）。
Mapper接口不能有实现类，MyBatis通过动态代理生成实现类，接口方法不能重载（方法名相同、参数不同）。
#{ }与${ }的区别：#{ }会进行参数预编译，避免SQL注入，推荐使用；${ }直接拼接SQL语句，有SQL注入风险，仅用于动态拼接表名、列名等场景。
增删改操作会影响数据库数据，需确保事务提交（自动或手动），否则操作不会持久化到数据库。
六、后续学习方向
入门MyBatis后，可进一步学习以下内容，提升MyBatis使用能力：
动态SQL：深入学习if、where、foreach、choose等动态标签，适配复杂查询场景（如多条件查询、批量操作）。
ResultMap：手动配置结果映射，解决实体类属性与数据库字段不匹配、一对一/一对多关联查询问题。
MyBatis缓存：学习一级缓存（SqlSession级别）和二级缓存（SqlSessionFactory级别），优化查询性能，了解缓存失效场景和自定义缓存配置（如Redis缓存）。
MyBatis注解开发：无需编写Mapper.xml，直接在Mapper接口方法上使用@Select、@Insert等注解编写SQL，简化配置（适合简单SQL场景）。
MyBatis与Spring/Spring Boot集成：实际开发中，MyBatis常与Spring Boot集成，通过配置文件简化环境搭建，使用@MapperScan扫描Mapper接口，无需手动获取SqlSession。
MyBatis插件：学习自定义插件，扩展MyBatis功能（如分页插件、日志插件），了解MyBatis插件的执行原理。

