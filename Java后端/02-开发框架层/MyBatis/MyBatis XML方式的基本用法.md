MyBatis XML方式的基本用法
MyBatis的XML方式是其核心用法之一，核心优势在于实现SQL语句与Java代码的分离，便于SQL统一管理、优化和维护，尤其适合SQL逻辑复杂的场景。
其核心组成包括「MyBatis核心配置XML」和「Mapper映射XML」，二者配合完成数据库的CRUD操作，以下从环境准备、核心配置、映射编写、实操演示到注意事项，完整讲解XML方式的基本用法。
一、前期准备（基础环境）
使用MyBatis XML方式前，需先完成基础环境搭建，确保依赖、数据库、实体类准备就绪（以IDEA+Maven+MySQL为例）。
1.1 导入核心依赖（pom.xml）
需引入MyBatis核心依赖、MySQL驱动依赖，可选引入日志依赖（便于调试SQL），依赖如下（版本可按需调整）：
<dependencies>
   <!-- MyBatis核心依赖 -->
    <dependency>
        <groupId>org.mybatis</groupId>
        <artifactId>mybatis</artifactId>
        <version>3.5.10</version>
    </dependency>
    <!-- MySQL驱动依赖（适配MySQL 8.0） -->
    <dependency>
        <groupId>mysql</groupId>
        <artifactId>mysql-connector-java</artifactId>
        <version>8.0.30</version&gt;
    &lt;/dependency&gt;
    <!-- 日志依赖（SLF4J+Logback，可选，用于打印SQL） -->
    <dependency>
        <groupId>ch.qos.logback</groupId>
        <artifactId>logback-classic</artifactId>
        <version>1.4.5</version&gt;
    &lt;/dependency&gt;
    <!-- JUnit测试依赖（可选） -->
    <dependency>
        <groupId>junit</groupId>
        <artifactId>junit</artifactId>
        <version>4.13.2</version>
        <scope>test</scope>
    </dependency>
</dependencies>
1.2 准备数据库与实体类
1. 创建测试数据库和表（以user表为例），SQL语句：
    CREATE DATABASE IF NOT EXISTS mybatis_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
    USE mybatis_db;
    CREATE TABLE IF NOT EXISTS user (
    id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(50) NOT NULL,
    age INT,
    email VARCHAR(100)
    );
    INSERT INTO user (username, password, age, email) VALUES
    ('zhangsan', '123456', 20, 'zhangsan@163.com'),
    ('lisi', '654321', 22, 'lisi@163.com');
2. 编写实体类（POJO），属性与数据库表字段对应，提供getter、setter和toString方法（包路径：com.example.pojo.User）：
    package com.example.pojo;
    public class User {
    private Integer id;
    private String username;
    private String password;
    private Integer age;
    private String email;
    // getter、setter方法（省略，自行补充）
    // toString方法（省略，自行补充）
    }
    二、核心XML配置（mybatis-config.xml）
    mybatis-config.xml是MyBatis的全局核心配置文件，用于配置数据库连接、全局参数、加载Mapper映射文件等，是XML方式的入口，需放在src/main/resources目录下，核心配置如下（标签顺序不可乱）：
    <?xml version="1.0" encoding="UTF-8" ?>
    <!DOCTYPE configuration
        PUBLIC "-//mybatis.org//DTD Config 3.0//EN"
        "https://mybatis.org/dtd/mybatis-3-config.dtd">
    <configuration&gt;
    <!-- 1. 加载外部数据库配置文件（可选，解耦硬编码） -->
    <properties resource="db.properties"&gt;&lt;/properties&gt;
    <!-- 2. 全局参数配置（可选，优化使用体验） -->
    &lt;settings&gt;
        <!-- 开启驼峰命名自动转换（数据库user_name → 实体类userName） -->
        <setting name="mapUnderscoreToCamelCase" value="true"/&gt;
        <!-- 开启日志，打印SQL执行过程（便于调试） -->
        <setting name="logImpl" value="SLF4J"/&gt;
    &lt;/settings&gt;
    <!-- 3. 实体类别名配置（可选，简化全类名书写） -->
    <typeAliases>
       <!-- 批量配置：为指定包下所有实体类生成别名（默认类名首字母小写） -->
        <package name="com.example.pojo"/&gt;
    &lt;/typeAliases&gt;
    <!-- 4. 数据库环境配置（核心，配置连接信息） -->
    <environments default="development"&gt;
        &lt;environment id="development"&gt;
            <!-- 事务管理器：JDBC（使用JDBC自带事务） -->
            <transactionManager type="JDBC"/&gt;
            <!-- 数据源：POOLED（MyBatis内置连接池，推荐） -->
            <dataSource type="POOLED">
                <property name="driver" value="${jdbc.driver}"/>
                <property name="url" value="${jdbc.url}"/>
                <property name="username" value="${jdbc.username}"/>
                <property name="password" value="${jdbc.password}"/>
            </dataSource>
        </environment&gt;
    &lt;/environments&gt;
    <!-- 5. 加载Mapper映射XML（核心，关联SQL与Java接口） -->
    &lt;mappers&gt;
        <!-- 方式1：单个映射文件加载（路径需与resources目录下的文件结构一致） -->
        <mapper resource="com/example/mapper/UserMapper.xml"/>
       <!-- 方式2：批量加载（推荐，扫描指定包下所有Mapper接口及对应XML） -->
        <package name="com.example.mapper"/>
    </mappers>
    </configuration>
    补充：外部数据库配置文件（db.properties）
    为避免核心配置文件硬编码数据库信息，新建db.properties（src/main/resources目录下），内容如下：

# MySQL 8.0 驱动
jdbc.driver=com.mysql.cj.jdbc.Driver

# 数据库URL（mybatis_db为数据库名，serverTimezone解决时区异常）
jdbc.url=jdbc:mysql://localhost:3306/mybatis_db?serverTimezone=UTC&useUnicode=true&characterEncoding=utf8

# 数据库用户名（替换为自己的）
jdbc.username=root

# 数据库密码（替换为自己的）
jdbc.password=123456
三、Mapper映射XML（核心，编写SQL）
Mapper映射XML是XML方式的核心，用于编写具体的SQL语句，与Mapper接口一一对应，实现SQL与Java代码的分离。命名规范：与Mapper接口同名，路径与接口包结构一致（如接口com.example.mapper.UserMapper，XML路径src/main/resources/com/example/mapper/UserMapper.xml）。
3.1 Mapper XML核心结构
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "https://mybatis.org/dtd/mybatis-3-mapper.dtd"&gt;
<!-- namespace：必须关联对应的Mapper接口全类名，不可写错 -->
<mapper namespace="com.example.mapper.UserMapper"><!-- 这里编写具体的SQL标签（select/update/insert/delete） -->
</mapper>
3.2 常用SQL标签详解（CRUD操作）
结合user表，编写常用CRUD操作的XML映射，配合对应的Mapper接口（无实现类，MyBatis动态代理生成），每个SQL标签对应接口中的一个方法。
3.2.1 查询操作（select标签）
用于执行查询SQL，核心属性：id（与Mapper接口方法名一致）、resultType（查询结果返回类型，实体类或基本类型）、parameterType（传入参数类型，可省略）。
<!-- 1. 根据id查询单个用户 -->
<select id="selectUserById" resultType="user">
    SELECT * FROM user WHERE id = #{id}
&lt;/select&gt;
<!-- 2. 查询所有用户 -->
<select id="selectAllUser" resultType="user">
    SELECT * FROM user
</select>
对应Mapper接口（com.example.mapper.UserMapper）方法：
package com.example.mapper;
import com.example.pojo.User;
import org.apache.ibatis.annotations.Param;
import java.util.List;
public interface UserMapper {
    // 与XML中select标签id一致
    User selectUserById(@Param("id") Integer id);
    List<User> selectAllUser();
}
3.2.2 添加操作（insert标签）
用于执行插入SQL，核心属性：id（与接口方法名一致）、parameterType（传入参数类型，实体类），可通过#{实体类属性名}获取参数值。
<insert id="insertUser" parameterType="user">
    INSERT INTO user (username, password, age, email)
    VALUES (#{username}, #{password}, #{age}, #{email})
</insert>
对应Mapper接口方法：
// 添加用户，返回受影响的行数
int insertUser(User user);
3.2.3 修改操作（update标签）
用于执行更新SQL，核心属性与insert一致，可根据需求修改指定字段。
<update id="updateUser" parameterType="user">
    UPDATE user
    SET username = #{username}, password = #{password}, age = #{age}, email = #{email}
    WHERE id = #{id}
</update>
对应Mapper接口方法：
// 修改用户，返回受影响的行数
int updateUser(User user);
3.2.4 删除操作（delete标签）
用于执行删除SQL，核心属性：id（与接口方法名一致）、parameterType（传入参数类型，基本类型或包装类）。
<delete id="deleteUserById" parameterType="java.lang.Integer">
    DELETE FROM user WHERE id = #{id}
</delete>
对应Mapper接口方法：
// 根据id删除用户，返回受影响的行数
int deleteUserById(@Param("id") Integer id);
3.3 核心标签属性说明
namespace：唯一关联Mapper接口，MyBatis通过该属性找到接口与XML的对应关系，必须是接口全类名，不可出错。
id：SQL标签的唯一标识，必须与Mapper接口中的方法名完全一致，否则无法匹配。
resultType：查询结果的返回类型，可写实体类全类名、别名（如配置了typeAliases），或基本数据类型（如Integer、String）。
parameterType：传入SQL的参数类型，可省略（MyBatis会自动推断），传入实体类时，直接写实体类别名或全类名。

#  }：参数占位符，MyBatis会自动进行参数预编译，避免SQL注入，可直接引用实体类属性名或@Param注解的参数名。
四、XML方式实操演示（执行SQL）
完成核心配置和Mapper映射后，通过SqlSession获取Mapper接口代理对象，调用接口方法执行SQL，步骤如下（可编写工具类简化操作）。
4.1 编写MyBatis工具类（简化SqlSession获取）
package com.example.utils;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import java.io.IOException;
import java.io.InputStream;
public class MyBatisUtils {
    // 全局唯一SqlSessionFactory（单例）
    private static SqlSessionFactory sqlSessionFactory;
    // 静态代码块，初始化SqlSessionFactory
    static {
        try {
            // 加载核心配置文件mybatis-config.xml
            String resource = "mybatis-config.xml";
            InputStream inputStream = Resources.getResourceAsStream(resource);
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    // 提供方法，获取SqlSession实例（自动提交事务）
    public static SqlSession getSqlSession() {
        return sqlSessionFactory.openSession(true);
    }
}
4.2 编写测试类（执行CRUD）
package com.example.test;
import com.example.mapper.UserMapper;
import com.example.pojo.User;
import com.example.utils.MyBatisUtils;
import org.apache.ibatis.session.SqlSession;
import org.junit.Test;
import java.util.List;
public class UserMapperTest {
    // 测试查询单个用户
    @Test
    public void testSelectUserById() {
        // 1. 获取SqlSession
        try (SqlSession sqlSession = MyBatisUtils.getSqlSession()) {
            // 2. 获取Mapper接口代理对象
            UserMapper userMapper = sqlSession.getMapper(UserMapper.class);
            // 3. 调用接口方法，执行SQL
            User user = userMapper.selectUserById(1);
            System.out.println(user);
        }
    }
    // 测试查询所有用户
    @Test
    public void testSelectAllUser() {
        try (SqlSession sqlSession = MyBatisUtils.getSqlSession()) {
            UserMapper userMapper = sqlSession.getMapper(UserMapper.class);
            List<User> userList = userMapper.selectAllUser();
            userList.forEach(System.out::println);
        }
    }
    // 测试添加用户
    @Test
    public void testInsertUser() {
        try (SqlSession sqlSession = MyBatisUtils.getSqlSession()) {
            UserMapper userMapper = sqlSession.getMapper(UserMapper.class);
            User user = new User();
            user.setUsername("wangwu");
            user.setPassword("111111");
            user.setAge(25);
            user.setEmail("wangwu@163.com");
            int rows = userMapper.insertUser(user);
            System.out.println("添加成功，受影响行数：" + rows);
        }
    }
    // 测试修改用户
    @Test
    public void testUpdateUser() {
        try (SqlSession sqlSession = MyBatisUtils.getSqlSession()) {
            UserMapper userMapper = sqlSession.getMapper(UserMapper.class);
            User user = new User();
            user.setId(3);
            user.setUsername("wangwu666");
            user.setPassword("666666");
            int rows = userMapper.updateUser(user);
            System.out.println("修改成功，受影响行数：" + rows);
        }
    }
    // 测试删除用户
    @Test
    public void testDeleteUserById() {
        try (SqlSession sqlSession = MyBatisUtils.getSqlSession()) {
            UserMapper userMapper = sqlSession.getMapper(UserMapper.class);
            int rows = userMapper.deleteUserById(3);
            System.out.println("删除成功，受影响行数：" + rows);
        }
    }
}
4.3 实操结果说明
运行测试方法，控制台会打印SQL执行日志（配置了SLF4J）和执行结果，验证SQL是否正常执行。
增删改操作需确保事务提交，工具类中openSession(true)已设置自动提交，无需手动调用commit()；若为openSession()，需手动提交。
查询操作无需提交事务，直接返回结果，MyBatis会自动将查询结果映射为实体类对象。
五、XML方式注意事项（关键避坑）
核心配置文件（mybatis-config.xml）标签有严格顺序，必须遵循：properties → settings → typeAliases → environments → mappers，顺序错误会导致解析失败。
Mapper XML的namespace必须与Mapper接口全类名完全一致，SQL标签的id必须与接口方法名完全一致，否则无法匹配。
Mapper XML的路径必须与Mapper接口的包结构一致（如接口在com.example.mapper，XML需在resources/com/example/mapper下），否则MyBatis无法加载。

#  }与${ }的区别：#{ }是预编译占位符，避免SQL注入，推荐使用；${ }是字符串拼接，有SQL注入风险，仅用于动态拼接表名、列名。
实体类属性与数据库字段不匹配时，可开启驼峰命名转换（mapUnderscoreToCamelCase），或通过resultMap手动配置映射关系。
SqlSession是线程不安全的，不可共享，每次操作数据库需获取新的SqlSession，用完及时关闭（try-with-resources可自动关闭）。
若SQL语句中包含特殊字符（如<、>、&），需使用XML转义字符（< → &lt;、> → &gt;、& → &amp;），或包裹在<![CDATA[ ]]>中。
六、总结
MyBatis XML方式的核心是「mybatis-config.xml全局配置」+「Mapper XML映射SQL」，通过二者配合，实现SQL与Java代码的分离，兼顾灵活性和可维护性。
其基本流程为：配置数据库环境→编写Mapper接口→在XML中编写对应SQL→通过SqlSession执行SQL。
掌握上述用法，即可完成大部分基础的数据库CRUD操作，后续可进一步学习动态SQL、resultMap等高级用法，适配更复杂的场景。
