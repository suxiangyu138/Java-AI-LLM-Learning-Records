MyBatis 动态 SQL 详解（XML+注解方式）
动态SQL是MyBatis的核心特性之一，核心作用是根据传入的参数条件，动态生成不同的SQL语句，避免手动拼接SQL带来的冗余、错误和SQL注入风险。它适用于多条件查询、批量操作、动态更新等场景，既能简化代码，又能提升SQL的灵活性和可维护性。
MyBatis动态SQL支持XML和注解两种实现方式：XML方式通过MyBatis提供的动态标签编写，适合复杂SQL场景；注解方式通过Provider类或动态注解编写，适合简单至中等复杂度SQL场景。以下将详细讲解两种方式的核心用法、案例及注意事项，延续前文user表、实体类和基础配置，保持上下文连贯。
一、动态SQL核心原理
MyBatis动态SQL基于OGNL（对象图导航语言）表达式解析参数，根据参数的有无、值的大小等条件，动态拼接SQL语句。核心优势：
无需手动拼接SQL，减少语法错误和SQL注入风险；
根据参数动态适配SQL，适配多条件查询、动态更新等复杂场景；
XML方式和注解方式可灵活选择，适配不同SQL复杂度需求。
二、XML方式动态SQL（核心，推荐复杂场景）
XML方式是动态SQL的主流用法，通过MyBatis提供的一系列动态标签编写，标签可嵌套使用，语法简洁、可读性强，适合复杂SQL场景。核心动态标签如下，结合案例逐一讲解。
2.1 核心动态标签说明
标签
作用
核心说明

条件判断，满足条件则拼接标签内SQL
常用test属性指定判断条件（OGNL表达式），如test="username != null and username != ''"

替代SQL中的WHERE关键字，自动处理多余的AND/OR
若子标签拼接的SQL以AND/OR开头，会自动删除，避免语法错误

用于UPDATE语句，自动处理多余的逗号
若子标签拼接的SQL以逗号结尾，会自动删除，适配动态更新字段

循环遍历集合/数组，拼接SQL片段（如IN查询、批量插入）
常用属性：collection（集合/数组名）、item（循环项）、separator（分隔符）、open（开始拼接字符）、close（结束拼接字符）

多条件分支判断，类似Java的if-else if-else
仅执行第一个满足条件的，都不满足则执行
2.2 XML方式动态SQL案例（结合user表）
沿用前文的UserMapper.xml（com.example.mapper.UserMapper.xml），编写动态SQL案例，对应Mapper接口方法同步更新。
2.2.1 案例1：多条件查询（<if>+<where>）
需求：根据用户名、年龄查询用户，参数可为null（即不传入该条件则不拼接对应SQL）。
<!-- XML动态SQL：多条件查询 -->
<select id="selectUserByCondition" resultType="user">
    SELECT * FROM user
    <where>
        <!-- 若username不为null且不为空字符串，拼接该条件 -->
        <if test="username != null and username != ''">
            AND username LIKE CONCAT('%', #{username}, '%')
        </if>
        <!-- 若age不为null，拼接该条件 -->
        <if test="age != null">
            AND age = #{age}
        </if>
    </where>
</select>
对应Mapper接口方法：
// 多条件查询，参数可为null
List<User> selectUserByCondition(@Param("username") String username, @Param("age") Integer age);
说明：若仅传入username，SQL会拼接为「SELECT * FROM user WHERE username LIKE CONCAT('%', ?, '%')」；若两个参数都不传入，SQL会变为「SELECT * FROM user」（无WHERE关键字），避免语法错误。
2.2.2 案例2：动态更新（<if>+<set>）
需求：修改用户信息，传入哪些字段就更新哪些字段（参数可为null，不传入则不更新该字段）。
<!-- XML动态SQL：动态更新 -->
<update id="updateUserDynamic" parameterType="user">
    UPDATE user
    <set>
        <if test="username != null and username != ''">
            username = #{username},
        </if>
        <if test="password != null and password != ''">
            password = #{password},
        </if>
        <if test="age != null">
            age = #{age},
        </if>
        <if test="email != null and email != ''">
            email = #{email}
        </if>
    </set>
    WHERE id = #{id}
</update>
对应Mapper接口方法：
// 动态更新用户，传入哪些字段就更新哪些字段
int updateUserDynamic(User user);
说明：<set>标签会自动删除最后一个字段后的逗号，避免「UPDATE user SET username = ?, WHERE id = ?」的语法错误。
2.2.3 案例3：批量操作（<foreach>）
需求1：批量查询（根据多个id查询用户，IN语句）；需求2：批量插入用户。
<!-- 案例3.1：批量查询（IN语句） -->
<select id="selectUserByIds" resultType="user">
    SELECT * FROM user
    <where>
        id IN
        <foreach collection="ids" item="id" open="(" close=")" separator=",">

            #{id}
        </foreach>
    </where>
</select>
<!-- 案例3.2：批量插入 -->
<insert id="insertUserBatch" parameterType="list">
    INSERT INTO user (username, password, age, email)
    VALUES
    <foreach collection="list" item="user" separator=",">
        (#{user.username}, #{user.password}, #{user.age}, #{user.email})
    </foreach>
</insert>
对应Mapper接口方法：
// 批量查询：根据多个id查询
List<User> selectUserByIds(@Param("ids") List<Integer> ids);
// 批量插入：插入多个用户
int insertUserBatch(@Param("list") List<User> userList);
说明：<foreach>标签的collection属性需与接口参数名一致（如@Param("ids")，则collection="ids"）；item是循环中每个元素的别名，separator是元素之间的分隔符。
2.2.4 案例4：多分支判断（<choose><when><otherwise>）
需求：优先根据id查询，若id为null则根据用户名查询，若都为null则查询所有用户。
<select id="selectUserByChoose" resultType="user">
    SELECT * FROM user
    <where>
        <choose>
            <!-- 第一个满足条件的分支执行 -->
            <when test="id != null">
                AND id = #{id}
            </when>
            <when test="username != null and username != ''">
                AND username LIKE CONCAT('%', #{username}, '%')
            </when>
            <!-- 所有when都不满足时执行 -->
            <otherwise>
                AND 1=1 -- 占位，避免where后无条件（也可省略，此时SQL无where）
            </otherwise>
        </choose>
    </where>
</select>
对应Mapper接口方法：
// 多分支查询：优先id，其次用户名，否则查询所有
List<User> selectUserByChoose(@Param("id") Integer id, @Param("username") String username);
三、注解方式动态SQL（适合简单至中等复杂度）
注解方式实现动态SQL，主要有两种方式：@SelectProvider、@UpdateProvider等Provider注解（推荐）、动态SQL注解（@If、@Where等）（MyBatis 3.5+支持）。两种方式均无需编写XML文件，直接在Mapper接口中实现。
3.1 方式1：Provider注解（推荐，兼容性好）
核心原理：通过自定义Provider类，编写方法动态生成SQL语句，然后在Mapper接口方法上通过@SelectProvider、@UpdateProvider等注解引用该方法，适合中等复杂度动态SQL。
3.1.1 步骤1：编写Provider类
创建com.example.provider.UserProvider类，编写动态生成SQL的方法（方法返回String类型，即拼接好的SQL）：
package com.example.provider;
import com.example.pojo.User;
import org.apache.ibatis.jdbc.SQL;
import java.util.List;
/**
 * 动态SQL Provider类：用于生成动态SQL语句
     */
    public class UserProvider {
    // 1. 多条件查询（对应XML的<if>+<where>）
    public String selectUserByCondition(String username, Integer age) {
        // 使用MyBatis提供的SQL类，简化SQL拼接
        return new SQL() {{
            SELECT("*");
            FROM("user");
            if (username != null && !username.equals("")) {
                WHERE("username LIKE CONCAT('%', #{username}, '%')");
            }
            if (age != null) {
                WHERE("age = #{age}");
            }
        }}.toString();
    }
    // 2. 动态更新（对应XML的<if>+<set>）
    public String updateUserDynamic(User user) {
        return new SQL() {{
            UPDATE("user");
            if (user.getUsername() != null && !user.getUsername().equals("")) {
                SET("username = #{username}");
            }
            if (user.getPassword() != null && !user.getPassword().equals("")) {
                SET("password = #{password}");
            }
            if (user.getAge() != null) {
                SET("age = #{age}");
            }
            if (user.getEmail() != null && !user.getEmail().equals("")) {
                SET("email = #{email}");
            }
            WHERE("id = #{id}");
        }}.toString();
    }
    // 3. 批量查询（对应XML的<foreach>）
    public String selectUserByIds(List<Integer> ids) {
        return new SQL() {{
            SELECT("*");
            FROM("user");
            WHERE("id IN (" + String.join(",", ids.stream().map(String::valueOf).toArray(String[]::new)) + ")");
        }}.toString();
    }
    }
    3.1.2 步骤2：Mapper接口引用Provider类
    在UserMapper接口中，使用@SelectProvider、@UpdateProvider等注解，引用Provider类的对应方法：
    package com.example.mapper;
    import com.example.pojo.User;
    import com.example.provider.UserProvider;
    import org.apache.ibatis.annotations.*;
    import java.util.List;
    public interface UserMapper {
    // 多条件查询：引用Provider类的selectUserByCondition方法
    @SelectProvider(type = UserProvider.class, method = "selectUserByCondition")
    List<User> selectUserByCondition(@Param("username") String username, @Param("age") Integer age);
    // 动态更新：引用Provider类的updateUserDynamic方法
    @UpdateProvider(type = UserProvider.class, method = "updateUserDynamic")
    int updateUserDynamic(User user);
    // 批量查询：引用Provider类的selectUserByIds方法
    @SelectProvider(type = UserProvider.class, method = "selectUserByIds")
    List<User> selectUserByIds(@Param("ids") List<Integer> ids);
    }
    说明：type属性指定Provider类，method属性指定Provider类中生成SQL的方法名，参数需与Provider方法的参数一致。
    3.2 方式2：动态SQL注解（MyBatis 3.5+）
    MyBatis 3.5及以上版本支持@If、@Where、@Set、@Foreach等动态注解，直接在Mapper接口方法上使用，无需编写Provider类，语法与XML标签类似，适合简单动态SQL。
    import org.apache.ibatis.annotations.*;
    // 示例：动态SQL注解实现多条件查询
    @Select("SELECT * FROM user")
    @Where({
    @If(test = "username != null and username != ''", value = "AND username LIKE CONCAT('%', #{username}, '%')"),
    @If(test = "age != null", value = "AND age = #{age}")
    })
    List<User> selectUserByConditionAnnotation(@Param("username") String username, @Param("age") Integer age);
    // 示例：动态SQL注解实现动态更新
    @Update("UPDATE user")
    @Set({
    @If(test = "username != null and username != ''", value = "username = #{username},"),
    @If(test = "password != null and password != ''", value = "password = #{password},"),
    @If(test = "age != null", value = "age = #{age},"),
    @If(test = "email != null and email != ''", value = "email = #{email}")
    })
    @Where({
    @If(test = "id != null", value = "id = #{id}")
    })
    int updateUserDynamicAnnotation(User user);
    说明：动态注解的语法与XML标签对应，@Where、@Set会自动处理多余的AND/OR和逗号，用法与XML方式基本一致。
    四、动态SQL注意事项（避坑重点）
    OGNL表达式语法：判断条件需遵循OGNL规范，如判断字符串非空需写「test="username != null and username != ''"」，不可简写为「test="username"」（会判断字符串是否为"true"，而非非空）；判断集合非空需写「test="ids != null and ids.size() > 0"」。
    <where>和<set>标签的使用：避免手动拼接WHERE和SET关键字，否则容易出现多余的AND/OR或逗号，导致SQL语法错误。
    <foreach>标签的collection属性：若接口参数是List，且未加@Param注解，collection需写「list」；若参数是数组，collection需写「array」；若加了@Param注解，collection需与注解参数名一致。
    SQL注入风险：动态SQL虽能避免大部分注入，但需注意「${ }」的使用（仅用于拼接表名、列名），查询参数必须用「#{ }」，防止SQL注入。
    注解方式的局限性：复杂动态SQL（如多标签嵌套、复杂循环）推荐使用XML方式，注解方式（尤其是Provider类）会导致代码杂乱，可读性下降。
    参数传递：多参数传递时，必须使用@Param注解命名参数，否则OGNL表达式无法识别参数名，导致动态SQL拼接失败。
    特殊字符处理：若动态SQL中包含<、>、&等特殊字符，XML方式需使用转义字符（如< → &lt;），注解方式可使用双引号包裹SQL，内部用单引号。
    五、XML与注解方式动态SQL对比
    对比维度
    XML方式
    注解方式（Provider/动态注解）
    可读性
    标签清晰，嵌套逻辑直观，复杂SQL可读性强
    简单SQL清晰，复杂SQL（多嵌套）可读性差
    适用场景
    复杂动态SQL、多标签嵌套、批量操作等场景
    简单至中等复杂度动态SQL，快速开发场景
    维护成本
    SQL集中管理，便于修改和优化，维护成本低
    Provider类需单独维护，复杂SQL维护困难
    灵活性
    标签丰富，支持各种复杂逻辑，灵活性高
    受注解语法限制，复杂逻辑实现繁琐
    六、总结
    MyBatis动态SQL的核心是“根据参数动态拼接SQL”，通过XML标签或注解两种方式实现，核心价值是简化代码、避免SQL注入、提升灵活性。
    实际开发中，建议：
    复杂动态SQL（多条件嵌套、批量操作、复杂分支）：优先使用XML方式，标签清晰、维护方便；
    简单至中等复杂度动态SQL（基础多条件查询、动态更新）：可使用注解方式（Provider或动态注解），简化配置、提升开发效率；
    无论哪种方式，都需遵循OGNL表达式规范，合理使用<where>、<set>等标签，避免语法错误和SQL注入风险。
    掌握上述动态SQL用法，即可适配大部分实际开发中的复杂查询和操作场景，结合前文的XML和注解基础，可完整掌握MyBatis的核心使用。
