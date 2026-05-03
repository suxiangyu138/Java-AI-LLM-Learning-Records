03.21 13:32
MyBatis注解配置详解（替代XML）
一、注解配置概述
1.1 什么是MyBatis注解配置
MyBatis提供两种配置方式：XML配置（前文重点讲解）和注解配置。注解配置是通过Java注解直接在Mapper接口上编写SQL语句和映射规则，无需创建SQL映射文件（XXXMapper.xml），简化配置流程，提升开发效率。
注解配置的核心是“将SQL语句与Mapper接口方法绑定”，适用于SQL逻辑简单、场景单一的项目；若SQL逻辑复杂（如多表关联、复杂动态SQL），仍推荐使用XML配置（更易维护、可读性更强）。
本文延续前文的项目结构（com.example包下的pojo、mapper），复用user、emp、dept等实体类和数据库表，逐步讲解注解配置的用法，实现与XML配置同等的功能（CRUD、关联映射、动态SQL）。
1.2 注解配置的优缺点
优点
简洁高效：无需创建XML映射文件，SQL与接口方法同屏显示，开发速度快；
配置简单：无需配置映射器（mappers标签）的resource路径，只需扫描Mapper接口包即可；
轻量灵活：适合简单SQL场景（如单表CRUD），代码结构更紧凑。
缺点
可读性差：复杂SQL（如多表关联、动态SQL）用注解编写时，代码冗长、格式混乱，难以维护；
功能有限：部分高级特性（如SQL片段复用、复杂resultMap映射）用注解实现繁琐，不如XML灵活；
调试不便：注解中的SQL错误，排查难度比XML配置更高。
1.3 核心注解分类
MyBatis的注解主要分为3大类，覆盖CRUD、关联映射、动态SQL等核心场景：
注解类型
具体注解
作用
基础CRUD注解
@Select、@Insert、@Update、@Delete
替代XML中的
映射配置注解
@Result、@Results、@ResultMap、@One、@Many
替代XML中的、、，实现实体映射和关联映射
动态SQL注解
@SelectProvider、@InsertProvider等Provider注解
替代XML中的动态标签，实现动态SQL拼接（需结合Java类编写SQL）
1.4 注解配置前置准备
使用注解配置前，需完成2个基础配置，确保MyBatis能扫描到注解的Mapper接口：
步骤1：修改mybatis-config.xml（核心配置）
无需配置<mapper resource="xxx.xml">，改为扫描Mapper接口所在的包，自动识别注解：
<?xml version="1.0" encoding="UTF-8" ?><!DOCTYPE configuration
        PUBLIC "-//mybatis.org//DTD Config 3.0//EN"
        "https://mybatis.org/dtd/mybatis-3-config.dtd">
<configuration>
    <!-- 环境配置（与前文一致，不变） -->
    <environments default="development"><environment id="development">
            <transactionManager type="JDBC"/>
            <dataSource type="POOLED">
               <property name="driver" value="com.mysql.cj.jdbc.Driver"/>
                <property name="url" value="jdbc:mysql://localhost:3306/mybatis_db?serverTimezone=UTC&useSSL=false"/>
                <property name="username" value="root"/>
                <property name="password" value="123456"/>
            </dataSource>
        </environment>
    </environments>
    <!-- 扫描Mapper接口包：注解配置的核心，替代XML映射文件的引入 --><mappers>
        <!-- 扫描com.example.mapper包下所有带注解的Mapper接口 -->
        <package name="com.example.mapper"/>
    </mappers>
</configuration>
步骤2：确保Mapper接口规范
Mapper接口需放在指定包下（如com.example.mapper），接口方法上添加对应注解，无需编写实现类（MyBatis动态代理自动生成），与XML配置的Mapper接口规范一致。
二、基础CRUD注解（核心用法）
基础CRUD注解是注解配置的核心，对应XML中的4个核心标签，用法简单，适合单表简单操作，以下以user表和UserMapper接口为例讲解。
2.1 @Select（查询）
替代XML中的<select>标签，用于编写查询SQL，支持参数传递、模糊查询等。
package com.example.mapper;
import com.example.pojo.User;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;
import java.util.List;
public interface UserMapper {
    // 1. 根据id查询用户（单个参数，用@Param指定参数名，与SQL中的#{}对应）
    @Select("SELECT * FROM user WHERE id = #{id}")
    User findById(@Param("id") Integer id);
    // 2. 查询所有用户
    @Select("SELECT * FROM user")
    List<User> findAll();
    // 3. 模糊查询（根据用户名）
    @Select("SELECT * FROM user WHERE username LIKE CONCAT('%', #{username}, '%')")
    List<User> findByUsername(@Param("username") String username);
}
2.2 @Insert（新增）
替代XML中的<insert>标签，用于编写新增SQL，支持获取主键自增的值（类似useGeneratedKeys和keyProperty）。
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
public interface UserMapper {
    // 新增用户，获取主键自增的值（keyProperty对应实体类的id属性）
    @Insert("INSERT INTO user (username, password, age, email) VALUES (#{username}, #{password}, #{age}, #{email})")
    @Options(useGeneratedKeys = true, keyProperty = "id") // 等价于XML中的useGeneratedKeys="true" keyProperty="id"
    int addUser(User user);
}
2.3 @Update（修改）
替代XML中的<update>标签，用于编写修改SQL，支持动态修改（简单场景）。
import org.apache.ibatis.annotations.Update;
public interface UserMapper {
    // 简单修改（修改所有字段）
    @Update("UPDATE user SET username = #{username}, password = #{password}, age = #{age}, email = #{email} WHERE id = #{id}")
    int updateUser(User user);
    // 简单动态修改（只修改用户名，参数为空时不执行，需手动判断）
    @Update("UPDATE user SET username = #{username} WHERE id = #{id}")
    int updateUsername(@Param("id") Integer id, @Param("username") String username);
}
2.4 @Delete（删除）
替代XML中的<delete>标签，用于编写删除SQL，支持单个参数或多个参数。
import org.apache.ibatis.annotations.Delete;
public interface UserMapper {
    // 根据id删除用户
    @Delete("DELETE FROM user WHERE id = #{id}")
    int deleteUser(@Param("id") Integer id);
    // 批量删除（结合foreach思想，手动拼接SQL，简单场景可用）
    @Delete("DELETE FROM user WHERE id IN (#{ids})")
    int deleteUserBatch(@Param("ids") String ids); // 传入格式如"1,2,3"的字符串
}
2.5 测试CRUD注解
测试方法与XML配置完全一致，无需修改，直接调用Mapper接口方法即可：
@Test
public void testAddUserByAnnotation() throws IOException {
    InputStream inputStream = Resources.getResourceAsStream("mybatis-config.xml");
    SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
    try (SqlSession sqlSession = sqlSessionFactory.openSession(true)) {
        UserMapper userMapper = sqlSession.getMapper(UserMapper.class);
        User user = new User("tianqi", "123123", 24, "tianqi@163.com");
        int rows = userMapper.addUser(user);
        System.out.println("新增成功，受影响行数：" + rows);
        System.out.println("新增用户id：" + user.getId()); // 主键自增生效
    }
}
三、映射配置注解（解决字段不匹配、关联映射）
当实体类属性与数据库表字段名不一致，或需要实现一对一、一对多关联映射时，需使用映射配置注解，替代XML中的<resultMap>标签。
3.1 @Result、@Results（字段映射）
场景说明
若User实体类的属性名（如userName）与数据库表字段名（username）不一致，直接查询会导致属性赋值失败，需通过@Results和@Result注解配置映射关系。
// 改造User实体类（属性名与字段名不一致）
public class User {
    private Integer id;
    private String userName; // 对应数据库字段username
    private String passWord; // 对应数据库字段password
    private Integer age;
    private String email;
    // getter/setter、toString方法省略
}
// Mapper接口配置字段映射
public interface UserMapper {
    // @Results：替代XML中的<resultMap>，id属性是映射标识（可复用）
    @Results(id = "userResultMap", value = {
        @Result(column = "id", property = "id", id = true), // id=true表示该字段是主键
        @Result(column = "username", property = "userName"), // 字段名username → 属性名userName
        @Result(column = "password", property = "passWord"), // 字段名password → 属性名passWord
        @Result(column = "age", property = "age"),
        @Result(column = "email", property = "email")
    })
    @Select("SELECT * FROM user WHERE id = #{id}")
    User findById(@Param("id") Integer id);
    // @ResultMap：复用已定义的映射（id为userResultMap），避免重复配置
    @ResultMap("userResultMap")
    @Select("SELECT * FROM user")
    List<User> findAll();
}
说明：@Result的id=true表示该字段是主键，MyBatis会优先处理主键映射；@ResultMap通过id引用已定义的@Results，实现映射复用。
3.2 @One（一对一关联映射）
替代XML中的<association>标签，用于实现一对一关联映射（如User与UserDetail），本质是“嵌套查询”。
import org.apache.ibatis.annotations.One;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.mapping.FetchType;
public interface UserMapper {
    // 一对一关联查询：查询用户及对应详情
    @Results(id = "userWithDetailMap", value = {
        @Result(column = "id", property = "id", id = true),
        @Result(column = "username", property = "userName"),
        @Result(column = "password", property = "passWord"),
        @Result(column = "age", property = "age"),
        @Result(column = "email", property = "email"),
        // @One：一对一关联，select指定关联查询的方法，fetchType指定加载方式
        @Result(
            column = "id", // 主表关联字段（user.id），作为参数传递给关联方法
            property = "userDetail", // 实体类中关联对象的属性名
            one = @One(
                select = "com.example.mapper.UserDetailMapper.findByUserId", // 关联查询的方法全路径
                fetchType = FetchType.EAGER // 立即加载（默认），FetchType.LAZY为延迟加载
            )
        )
    })
    @Select("SELECT * FROM user WHERE id = #{id}")
    User findUserWithDetail(@Param("id") Integer id);
}
// UserDetailMapper接口（关联查询方法）
public interface UserDetailMapper {
    @Select("SELECT * FROM user_detail WHERE user_id = #{userId}")
    UserDetail findByUserId(@Param("userId") Integer userId);
}
3.3 @Many（一对多关联映射）
替代XML中的<collection>标签，用于实现一对多关联映射（如Dept与Emp），同样支持立即加载和延迟加载。
import org.apache.ibatis.annotations.Many;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.mapping.FetchType;
public interface DeptMapper {
    // 一对多关联查询：查询部门及下属员工
    @Results(id = "deptWithEmpMap", value = {
        @Result(column = "id", property = "id", id = true),
        @Result(column = "dept_name", property = "deptName"),
        @Result(column = "dept_desc", property = "deptDesc"),
        // @Many：一对多关联，select指定关联查询的方法
        @Result(
            column = "id", // 主表关联字段（dept.id），传递给关联方法
            property = "empList", // 实体类中关联集合的属性名
            many = @Many(
                select = "com.example.mapper.EmpMapper.findByDeptId", // 关联查询方法
                fetchType = FetchType.LAZY // 延迟加载，提升性能
            )
        )
    })
    @Select("SELECT * FROM dept WHERE id = #{id}")
    Dept findDeptWithEmp(@Param("id") Integer id);
}
// EmpMapper接口（关联查询方法）
public interface EmpMapper {
    @Select("SELECT * FROM emp WHERE dept_id = #{deptId}")
    List<Emp> findByDeptId(@Param("deptId") Integer deptId);
}
四、动态SQL注解（Provider注解）
当SQL逻辑复杂（如多条件动态查询、动态修改）时，直接用@Select、@Update等注解编写SQL会非常繁琐，此时需使用Provider注解，通过Java类动态拼接SQL，替代XML中的动态标签。
常用Provider注解：@SelectProvider、@InsertProvider、@UpdateProvider、@DeleteProvider，核心是“指定一个Java类，由该类的方法返回拼接好的SQL字符串”。
4.1 场景：多条件动态查询用户
步骤1：创建SQL Provider类
创建专门用于拼接SQL的类，编写动态SQL拼接方法，方法返回String类型的SQL语句。
package com.example.provider;
import com.example.pojo.User;
import org.apache.ibatis.jdbc.SQL;
// SQL Provider类：专门用于拼接动态SQL
public class UserSqlProvider {
    // 动态拼接多条件查询SQL
    public String findUserByCondition(User user) {
        // 使用MyBatis提供的SQL类，简化SQL拼接，避免手动拼接的语法错误
        return new SQL() {{
            SELECT("*");
            FROM("user");
            // 动态添加条件，与XML中的<if>标签一致
            if (user.getUserName() != null && !user.getUserName().equals("")) {
                WHERE("username LIKE CONCAT('%', #{userName}, '%')");
            }
            if (user.getAge() != null) {
                WHERE("age = #{age}");
            }
            if (user.getEmail() != null && !user.getEmail().equals("")) {
                WHERE("email = #{email}");
            }
        }}.toString();
    }
}
步骤2：在Mapper接口中使用@SelectProvider
import com.example.provider.UserSqlProvider;
import org.apache.ibatis.annotations.SelectProvider;
public interface UserMapper {
    // @SelectProvider：指定Provider类和拼接SQL的方法
    @SelectProvider(type = UserSqlProvider.class, method = "findUserByCondition")
    List<User> findUserByCondition(User user);
}
步骤3：测试动态SQL注解
测试方法与XML动态SQL完全一致，传入不同参数，会自动拼接对应的SQL：
@Test
public void testFindUserByCondition() throws IOException {
    InputStream inputStream = Resources.getResourceAsStream("mybatis-config.xml");
    SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
        UserMapper userMapper = sqlSession.getMapper(UserMapper.class);
        // 模拟参数：只输入用户名
        User condition = new User();
        condition.setUserName("zhangsan");
        List<User> userList = userMapper.findUserByCondition(condition);
        userList.forEach(System.out::println);
    }
}
4.2 Provider注解核心说明
type：指定SQL Provider类的全类名（如UserSqlProvider.class）；
method：指定Provider类中用于拼接SQL的方法名（如findUserByCondition）；
拼接SQL时，推荐使用MyBatis提供的SQL类，可自动处理WHERE、AND等关键字，避免语法错误；
Provider方法的参数的与Mapper接口方法的参数一致（如均为User user），可直接在SQL拼接中使用参数的属性。
五、注解配置与XML配置对比及选型建议
5.1 核心对比
对比维度
注解配置
XML配置
配置复杂度
简单，无需创建XML文件
稍复杂，需创建映射文件，配置标签
SQL可读性
简单SQL清晰，复杂SQL冗长混乱
结构清晰，复杂SQL（动态、关联）易维护
高级特性支持
有限，复杂映射、SQL复用不便
全面，支持所有MyBatis特性
调试难度
较高，SQL嵌入注解中，错误排查不便
较低，SQL集中在XML中，便于调试
适用场景
简单项目、单表CRUD、SQL逻辑简单
复杂项目、多表关联、动态SQL、需维护大量SQL
5.2 选型建议
小型项目/工具类：优先使用注解配置，快速开发，减少配置文件；
中大型项目：优先使用XML配置，便于SQL维护、调试和团队协作；
混合使用：简单CRUD用注解，复杂SQL（关联、动态）用XML，灵活适配场景（MyBatis支持注解与XML混合配置）。
六、注解配置常见问题与注意事项
Mapper接口扫描失败原因：mybatis-config.xml中未配置<package name="xxx"/>，或包路径错误；解决：确保<mappers>标签中扫描的包路径与Mapper接口所在包一致（如com.example.mapper）。
实体类属性与字段名不匹配，赋值失败原因：未使用@Results和@Result配置映射关系，或映射关系配置错误；解决：通过@Results注解配置字段与属性的映射，确保column（字段名）与property（属性名）对应正确。
关联映射查询不到关联对象（为null）原因1：@One/@Many注解的select属性配置错误（方法全路径错误）；原因2：关联字段（column）配置错误，未将主表字段传递给关联方法；解决：核对select属性的方法全路径，确保column属性与主表关联字段一致。
动态SQL拼接错误原因：手动拼接SQL时遗漏关键字（如WHERE、AND），或参数引用错误；解决：使用MyBatis提供的SQL类拼接动态SQL，避免手动拼接，减少语法错误。
注解与XML配置冲突原因：同一Mapper接口的同一方法，同时使用了注解和XML配置；解决：同一方法只能使用一种配置方式，优先保留XML配置（复杂场景）。
七、注解配置总结
MyBatis注解配置是XML配置的补充，核心是通过注解简化简单场景的配置，关键要点如下：
基础CRUD：用@Select、@Insert、@Update、@Delete，简单高效，无需XML；
字段/关联映射：用@Results、@Result、@One、@Many，替代XML的<resultMap>；
动态SQL：用Provider注解（@SelectProvider等），结合Java类拼接复杂SQL；
选型原则：简单场景用注解，复杂场景用XML，可混合使用提升开发效率。
注解配置和XML配置的核心功能一致，都是实现Java方法与SQL的绑定，掌握两者的用法和选型，能灵活适配不同项目场景，提升MyBatis开发效率。

