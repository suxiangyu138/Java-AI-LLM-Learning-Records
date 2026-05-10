03.20 16:26
MyBatis注解方式的基本用法
MyBatis注解方式是XML方式的补充，核心优势是无需编写Mapper XML映射文件，直接在Mapper接口方法上通过注解编写SQL语句，简化配置、提高开发效率，适合SQL逻辑简单、CRUD操作单一的场景。其核心是通过MyBatis提供的注解，将SQL语句与Mapper接口方法绑定，无需额外配置映射文件，以下从环境准备、核心注解、实操演示到注意事项，完整讲解注解方式的基本用法（延续前文数据库、实体类，保持上下文连贯）。
一、前期准备（与XML方式通用）
注解方式的基础环境与XML方式一致，只需确保依赖、数据库、实体类、核心配置文件准备就绪，无需额外新增依赖。
1.1 确认依赖（pom.xml）
沿用XML方式的核心依赖（MyBatis核心、MySQL驱动、日志、JUnit），无需新增注解相关依赖（MyBatis核心包已包含注解类）：
&lt;dependencies&gt;
    <!-- MyBatis核心依赖（包含注解相关类） -->
    <dependency>
        <groupId>org.mybatis</groupId>
        <artifactId>mybatis</artifactId>
        <version>3.5.10</version>
    </dependency>
<!-- MySQL驱动依赖 -->
    <dependency>
        <groupId>mysql</groupId>
        <artifactId>mysql-connector-java</artifactId>
        <version>8.0.30</version>
    &lt;/dependency&gt;
    <!-- 日志依赖（便于调试SQL） -->
    <dependency>
        <groupId>ch.qos.logback</groupId>
        <artifactId>logback-classic</artifactId>
        <version>1.4.5</version>
    </dependency>
    <!-- JUnit测试依赖 -->
    <dependency>
        <groupId>junit</groupId>
        <artifactId>junit</artifactId>
        <version>4.13.2</version>
        <scope>test</scope>
    </dependency>
</dependencies>
1.2 确认数据库与实体类
1. 数据库与表（沿用user表，与XML方式一致，无需修改）；
2. 实体类（com.example.pojo.User）：属性与数据库表字段对应，提供getter、setter和toString方法（与XML方式完全一致，无需修改）。
1.3 核心配置文件（mybatis-config.xml）
注解方式无需编写Mapper XML，但仍需核心配置文件，用于配置数据库环境、全局参数，关键区别是加载Mapper的方式（注解方式需扫描Mapper接口，无需加载XML文件），核心配置如下：
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE configuration
        PUBLIC "-//mybatis.org//DTD Config 3.0//EN"
        "https://mybatis.org/dtd/mybatis-3-config.dtd"&gt;
&lt;configuration&gt;
    <!-- 加载外部数据库配置文件 -->
    <properties resource="db.properties">&lt;/properties&gt;
    <!-- 全局参数配置（与XML方式一致） -->
    <settings>
        <setting name="mapUnderscoreToCamelCase" value="true"/>
        <setting name="logImpl" value="SLF4J"/&gt;
    &lt;/settings&gt;
    <!-- 实体类别名配置（简化书写） -->
    <typeAliases>
        <package name="com.example.pojo"/&gt;
    &lt;/typeAliases&gt;
    <!-- 数据库环境配置（与XML方式一致） -->
    <environments default="development">
        <environment id="development">
            <transactionManager type="JDBC"/>
            <dataSource type="POOLED">
                <property name="driver" value="${jdbc.driver}"/>
                <property name="url" value="${jdbc.url}"/>
                <property name="username" value="${jdbc.username}"/>
                <property name="password" value="${jdbc.password}"/>
            </dataSource>
        </environment>
    </environments>
    <!-- 核心区别：加载Mapper接口（注解方式） -->
    &lt;mappers&gt;
        <!-- 扫描指定包下所有Mapper接口（注解方式核心，无需加载XML） -->
        <package name="com.example.mapper"/>
    </mappers>
</configuration>
说明：db.properties文件与XML方式完全一致，无需修改。
二、MyBatis核心注解（重点）
MyBatis提供了一系列注解，用于替代XML中的SQL标签，核心注解对应XML中的select/update/insert/delete标签，无需编写XML，直接在Mapper接口方法上使用，常用核心注解如下：
2.1 核心CRUD注解
注解
作用
对应XML标签
核心说明
@Select
执行查询SQL

value属性填写SQL语句，可配合@ResultMap配置结果映射
@Insert
执行插入SQL

value属性填写SQL语句，支持获取自增主键
@Update
执行更新SQL

value属性填写SQL语句，返回受影响的行数
@Delete
执行删除SQL

value属性填写SQL语句，返回受影响的行数
2.2 辅助注解（常用）
@Param：给Mapper接口方法的参数命名，解决多参数传递时的参数匹配问题（与XML方式的@Param作用一致）。
@ResultMap：引用XML中定义的ResultMap（当实体类与数据库字段不匹配，且不想用XML时，可配合@Results、@Result手动配置映射）。
@Options：配置额外参数，如获取自增主键（useGeneratedKeys=true，keyProperty="实体类主键属性名"）。
三、注解方式实操演示（CRUD操作）
注解方式的核心是编写Mapper接口（无需实现类，无需XML文件），在接口方法上添加对应注解并编写SQL，然后通过SqlSession执行，步骤如下（工具类与XML方式一致，无需修改）。
3.1 编写Mapper接口（核心，注解编写SQL）
创建com.example.mapper.UserMapper接口，在方法上添加对应注解，编写SQL语句，替代XML映射文件：
package com.example.mapper;
import com.example.pojo.User;
import org.apache.ibatis.annotations.*;
import java.util.List;
/**
 * MyBatis注解方式：Mapper接口（无需XML文件）
 * 注解直接编写SQL，与方法绑定
 */
public interface UserMapper {
    // 1. 根据id查询单个用户（@Select注解）
    @Select("SELECT * FROM user WHERE id = #{id}")
    User selectUserById(@Param("id") Integer id);
    // 2. 查询所有用户（@Select注解）
    @Select("SELECT * FROM user")
    List<User> selectAllUser();
    // 3. 添加用户（@Insert注解），获取自增主键（@Options）
    @Insert("INSERT INTO user (username, password, age, email) VALUES (#{username}, #{password}, #{age}, #{email})")
    @Options(useGeneratedKeys = true, keyProperty = "id") // 获取自增主键，赋值给User的id属性
    int insertUser(User user);
    // 4. 修改用户（@Update注解）
    @Update("UPDATE user SET username = #{username}, password = #{password}, age = #{age}, email = #{email} WHERE id = #{id}")
    int updateUser(User user);
    // 5. 根据id删除用户（@Delete注解）
    @Delete("DELETE FROM user WHERE id = #{id}")
    int deleteUserById(@Param("id") Integer id);
}
关键说明：
SQL语句直接写在注解的value属性中，语法与XML中的SQL完全一致，可直接使用#{ }引用参数。
@Options注解用于获取自增主键，useGeneratedKeys=true表示开启自增主键获取，keyProperty="id"表示将自增主键赋值给User实体类的id属性。
多参数方法（如无实体类传入），需用@Param注解命名参数，例如：selectUserByUsernameAndAge(@Param("username") String username, @Param("age") Integer age)。
3.2 复用XML方式的工具类
无需修改MyBatisUtils工具类（com.example.utils.MyBatisUtils），该工具类用于获取SqlSession，与XML方式完全通用，直接复用即可。
3.3 编写测试类（执行CRUD）
测试类与XML方式完全一致，无需修改，通过SqlSession获取Mapper接口代理对象，调用接口方法即可执行SQL：
package com.example.test;
import com.example.mapper.UserMapper;
import com.example.pojo.User;
import com.example.utils.MyBatisUtils;
import org.apache.ibatis.session.SqlSession;
import org.junit.Test;
import java.util.List;
public class UserMapperAnnotationTest {
    // 测试查询单个用户
    @Test
    public void testSelectUserById() {
        try (SqlSession sqlSession = MyBatisUtils.getSqlSession()) {
            UserMapper userMapper = sqlSession.getMapper(UserMapper.class);
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
    // 测试添加用户（可获取自增主键）
    @Test
    public void testInsertUser() {
        try (SqlSession sqlSession = MyBatisUtils.getSqlSession()) {
            UserMapper userMapper = sqlSession.getMapper(UserMapper.class);
            User user = new User();
            user.setUsername("zhaoliu");
            user.setPassword("888888");
            user.setAge(23);
            user.setEmail("zhaoliu@163.com");
            int rows = userMapper.insertUser(user);
            System.out.println("添加成功，受影响行数：" + rows);
            System.out.println("添加用户的自增id：" + user.getId()); // 可获取自增主键
        }
    }
    // 测试修改用户
    @Test
    public void testUpdateUser() {
        try (SqlSession sqlSession = MyBatisUtils.getSqlSession()) {
            UserMapper userMapper = sqlSession.getMapper(UserMapper.class);
            User user = new User();
            user.setId(4); // 对应添加的zhaoliu的id
            user.setUsername("zhaoliu666");
            user.setPassword("999999");
            int rows = userMapper.updateUser(user);
            System.out.println("修改成功，受影响行数：" + rows);
        }
    }
    // 测试删除用户
    @Test
    public void testDeleteUserById() {
        try (SqlSession sqlSession = MyBatisUtils.getSqlSession()) {
            UserMapper userMapper = sqlSession.getMapper(UserMapper.class);
            int rows = userMapper.deleteUserById(4);
            System.out.println("删除成功，受影响行数：" + rows);
        }
    }
}
3.4 实操结果说明
运行测试方法，控制台会打印SQL执行日志，与XML方式效果一致，验证SQL正常执行。
添加用户时，@Options注解可成功获取自增主键，并赋值给User对象的id属性，可直接打印查看。
所有操作的逻辑与XML方式完全一致，区别仅在于SQL的编写位置（注解 vs XML文件）。
四、注解方式进阶（解决字段不匹配问题）
当实体类属性与数据库字段不匹配（如数据库字段为user_name，实体类属性为userName，未开启驼峰转换），或字段名完全不一致时，可通过@Results和@Result注解手动配置结果映射，替代XML中的ResultMap。
// 示例：实体类属性与数据库字段不匹配时，手动配置映射
@Select("SELECT id, username, password, age, email FROM user WHERE id = #{id}")
@Results({
    @Result(column = "id", property = "id"), // column：数据库字段名；property：实体类属性名
    @Result(column = "username", property = "username"),
    @Result(column = "password", property = "password"),
    @Result(column = "age", property = "userAge"), // 假设实体类属性为userAge，数据库字段为age
    @Result(column = "email", property = "userEmail") // 假设实体类属性为userEmail，数据库字段为email
})
User selectUserById(@Param("id") Integer id);
说明：若多个方法需要复用该映射关系，可给@Results添加id属性，然后用@ResultMap引用：
// 定义可复用的结果映射，id为userResultMap
@Results(id = "userResultMap", value = {
    @Result(column = "id", property = "id"),
    @Result(column = "username", property = "username"),
    @Result(column = "age", property = "userAge"),
    @Result(column = "email", property = "userEmail")
})
@Select("SELECT * FROM user WHERE id = #{id}")
User selectUserById(@Param("id") Integer id);
// 复用结果映射
@Select("SELECT * FROM user")
@ResultMap("userResultMap") // 引用上面定义的结果映射
List<User> selectAllUser();
五、注解方式注意事项（避坑重点）
注解方式仅适合SQL逻辑简单的场景（如基础CRUD），复杂SQL（动态SQL、多表关联）推荐使用XML方式，注解方式编写复杂SQL会降低可读性和可维护性。
核心配置文件中，<mappers>标签必须扫描Mapper接口所在包（<package name="com.example.mapper"/>），否则MyBatis无法识别注解。
注解中的SQL语句若包含特殊字符（如<、>、&），需使用转义字符（如< → &lt;），或用双引号包裹SQL，内部用单引号（避免XML转义冲突）。
多参数传递时，必须使用@Param注解给参数命名，否则MyBatis无法匹配参数（如selectUserByUsernameAndAge(@Param("username") String username, @Param("age") Integer age)）。
获取自增主键时，需添加@Options(useGeneratedKeys = true, keyProperty = "实体类主键属性名")，否则无法获取自增id。
注解方式与XML方式可混合使用：若部分方法SQL简单用注解，部分方法SQL复杂用XML，只需在核心配置文件中同时扫描Mapper接口和加载XML文件即可。
SQL语句中的表名、字段名需与数据库一致，避免拼写错误（注解方式无XML解析校验，拼写错误会运行时报错）。
六、注解方式与XML方式对比
对比维度
注解方式
XML方式
配置复杂度
简单，无需编写XML文件，直接在接口上注解
稍复杂，需编写核心配置和Mapper XML
SQL可读性
简单SQL清晰，复杂SQL杂乱
SQL集中管理，复杂SQL（动态SQL）可读性强
适用场景
基础CRUD、SQL逻辑简单的场景
复杂SQL、动态SQL、多表关联、需要统一管理SQL的场景
维护成本
简单SQL维护方便，复杂SQL维护困难
SQL集中维护，便于优化和修改，维护成本低
七、总结
MyBatis注解方式的核心是“Mapper接口+注解”，无需编写XML映射文件，简化了基础CRUD操作的配置，适合快速开发简单业务。其基本流程为：配置核心文件（扫描Mapper接口）→ 在Mapper接口方法上添加对应注解编写SQL → 通过SqlSession执行SQL。
实际开发中，建议根据SQL复杂度选择方式：简单CRUD用注解，复杂SQL用XML，也可混合使用，兼顾开发效率和可维护性。掌握上述注解用法，即可完成基础的数据库操作，后续可结合动态SQL注解（如@SelectProvider）进一步拓展。

