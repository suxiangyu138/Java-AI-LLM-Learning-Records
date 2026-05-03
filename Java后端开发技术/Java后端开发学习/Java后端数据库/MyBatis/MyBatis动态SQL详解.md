03.21 13:27
MyBatis动态SQL详解
一、动态SQL概述
1.1 什么是动态SQL
动态SQL是MyBatis的核心特性之一，它允许在SQL语句中根据传入的参数条件，动态拼接、调整SQL的结构（如条件判断、循环拼接、分支选择等），避免了手动拼接SQL带来的繁琐操作和SQL注入风险，适配多条件查询、动态新增/修改等常见业务场景。
简单来说，动态SQL就是“根据参数动态生成符合需求的SQL语句”，例如：多条件查询时，用户可能输入用户名、年龄、邮箱等任意组合的条件，动态SQL可自动判断哪些条件不为空，只拼接有效的查询条件。
动态SQL依赖MyBatis提供的标签实现，所有动态标签均嵌套在SQL映射文件的<select>、<insert>、<update>、<delete>标签内部，或嵌套在<sql>标签（SQL片段）中。
1.2 动态SQL的核心作用
简化多条件查询开发：无需手动判断参数是否为空，自动拼接有效条件；
避免SQL注入：动态标签会自动对参数进行转义处理（结合#{}），比手动拼接SQL更安全；
提升SQL复用性：可通过<sql>标签抽取公共SQL片段，减少重复代码；
适配灵活业务场景：支持动态新增（只插入非空字段）、动态修改（只更新非空字段）等场景。
1.3 核心动态SQL标签
MyBatis提供6个常用动态标签，覆盖大部分业务场景，重点掌握前5个：
标签
作用
适用场景

条件判断，满足条件则拼接标签内的SQL
多条件查询、动态新增/修改

自动处理WHERE关键字，避免拼接时出现多余的AND/OR
多条件查询（配合使用）

自动处理SET关键字，避免拼接时出现多余的逗号
动态修改（只更新非空字段）

循环遍历集合/数组，拼接SQL片段（如IN关键字、批量操作）
批量查询、批量新增/删除
/
抽取公共SQL片段，引用片段
重复SQL片段复用（如查询字段、表名）
//
分支选择，类似Java的switch-case，只执行一个满足条件的分支
多条件互斥的查询/操作
二、核心动态SQL标签实战（结合前文项目）
本文延续前文的项目结构（com.example包下的pojo、mapper），以user表、emp表为基础，结合实际业务场景，逐个讲解动态标签的用法，所有代码可直接复用前文项目。
2.1 <if>标签（基础条件判断）
场景说明
多条件查询用户：根据用户名（username）、年龄（age）、邮箱（email）查询用户，参数可能为空（如用户只输入用户名，不输入年龄），只拼接非空的条件。
代码实现（UserMapper.xml）
<mapper namespace="com.example.mapper.UserMapper">
    <!-- 1. 多条件查询用户：使用<if>标签 -->
    </mapper>
接口与测试
// UserMapper接口添加方法
public interface UserMapper {
    // 多条件查询用户
    List<User> findUserByCondition(User user);
}
// 测试方法
@Test
public void testFindUserByCondition() throws IOException {
    InputStream inputStream = Resources.getResourceAsStream("mybatis-config.xml");
    SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
        UserMapper userMapper = sqlSession.getMapper(UserMapper.class);
        // 模拟参数：只输入用户名，年龄和邮箱为空
        User condition = new User();
        condition.setUsername("zhangsan");
        // 执行查询，SQL会自动拼接：SELECT * FROM user WHERE 1=1 AND username LIKE CONCAT('%', 'zhangsan', '%')
        List<User> userList = userMapper.findUserByCondition(condition);
        userList.forEach(System.out::println);
    }
}
注意事项
WHERE 1=1的作用：当所有<if>条件都不满足时，SQL会变成“SELECT * FROM user WHERE”，出现语法错误；添加WHERE 1=1后，即使没有条件，SQL也会变成“SELECT * FROM user WHERE 1=1”，语法正确（后续可通过<where>标签替代）。
2.2 <where>标签（优化WHERE条件拼接）
场景说明
优化上文的多条件查询，使用<where>标签替代“WHERE 1=1”，自动处理多余的AND/OR关键字，简化SQL编写。
代码实现（UserMapper.xml）
<!-- 2. 优化多条件查询：使用<where>+<if> -->
核心特点
1. 当<where>标签内有满足条件的<if>时，自动添加WHERE关键字；
2. 自动剔除条件前多余的AND/OR（如第一个条件前有AND，会自动删除）；
3. 当所有<if>条件都不满足时，<where>标签会自动不生成，SQL变为“SELECT * FROM user”，语法正确。
2.3 <set>标签（动态修改）
场景说明
动态修改用户信息：只更新传入的非空字段（如用户只修改用户名，不修改年龄和邮箱，SQL只拼接用户名的更新语句）。
代码实现（UserMapper.xml）
<!-- 3. 动态修改用户：使用<set>+<if> -->
<update id="updateUserDynamic">
    UPDATE user
    <set>
        <if test="username != null and username != ''">
            username = #{username}, <!-- 逗号可保留，<set>会自动剔除多余逗号 -->
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
    WHERE id = #{id} <!-- 必须有WHERE条件，避免批量更新 -->
</update>
接口与测试
// UserMapper接口添加方法
public interface UserMapper {
    // 动态修改用户
    int updateUserDynamic(User user);
}
// 测试方法
@Test
public void testUpdateUserDynamic() throws IOException {
    InputStream inputStream = Resources.getResourceAsStream("mybatis-config.xml");
    SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
    try (SqlSession sqlSession = sqlSessionFactory.openSession(true)) {
        UserMapper userMapper = sqlSession.getMapper(UserMapper.class);
        // 模拟参数：只修改用户名和邮箱，密码、年龄为空
        User user = new User();
        user.setId(1);
        user.setUsername("lisi");
        user.setEmail("lisi@163.com");
        // 执行修改，SQL自动拼接：UPDATE user SET username = 'lisi', email = 'lisi@163.com' WHERE id = 1
        int rows = userMapper.updateUserDynamic(user);
        System.out.println("修改成功，受影响行数：" + rows);
    }
}
2.4 <foreach>标签（循环拼接）
场景说明
<foreach>标签用于循环遍历集合或数组，常见两个场景：
1. 批量查询：根据多个id查询用户（IN关键字）；
2. 批量新增：一次性插入多条用户数据。
场景1：批量查询（IN关键字）
<!-- 4. 批量查询用户：根据多个id查询（IN关键字） -->
场景2：批量新增
<!-- 5. 批量新增用户 -->
<insert id="addUserBatch" useGeneratedKeys="true" keyProperty="id">
    INSERT INTO user (username, password, age, email)
    VALUES
    <foreach collection="userList" item="user" separator=",">
        (#{user.username}, #{user.password}, #{user.age}, #{user.email})
    </foreach>
</insert>
foreach核心属性说明
collection：要遍历的集合/数组名称（与接口方法的参数名一致，若参数用@Param指定，需与@Param的值一致）；
item：遍历过程中，集合/数组的每个元素的别名（如遍历ids集合，item="id"，则可用#{id}获取元素）；
open：循环拼接的SQL片段开头（如IN查询需开头加"("）；
close：循环拼接的SQL片段结尾（如IN查询需结尾加")"）；
separator：每个元素之间的分隔符（如IN查询用","，批量新增用","）。
接口与测试
// UserMapper接口添加方法
public interface UserMapper {
    // 批量查询用户
    List<User> findUserByIds(@Param("ids") List<Integer> ids);
    // 批量新增用户
    int addUserBatch(@Param("userList") List<User> userList);
}
// 测试方法
@Test
public void testFindUserByIds() throws IOException {
    InputStream inputStream = Resources.getResourceAsStream("mybatis-config.xml");
    SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
        UserMapper userMapper = sqlSession.getMapper(UserMapper.class);
        // 模拟参数：查询id为1、2、3的用户
        List<Integer> ids = Arrays.asList(1, 2, 3);
        List<User> userList = userMapper.findUserByIds(ids);
        userList.forEach(System.out::println);
    }
}
@Test
public void testAddUserBatch() throws IOException {
    InputStream inputStream = Resources.getResourceAsStream("mybatis-config.xml");
    SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
    try (SqlSession sqlSession = sqlSessionFactory.openSession(true)) {
        UserMapper userMapper = sqlSession.getMapper(UserMapper.class);
        // 模拟批量新增数据
        List<User> userList = Arrays.asList(
            new User("wangwu", "123456", 23, "wangwu@163.com"),
            new User("zhaoliu", "654321", 25, "zhaoliu@163.com")
        );
        int rows = userMapper.addUserBatch(userList);
        System.out.println("批量新增成功，受影响行数：" + rows);
    }
}
2.5 <sql>/<include>标签（SQL片段复用）
场景说明
当多个SQL语句需要使用相同的片段（如查询字段、表名、条件）时，用<sql>标签抽取公共片段，再用<include>标签引用，减少重复代码，便于维护。
代码实现（UserMapper.xml）
<!-- 6. 抽取公共SQL片段：查询user表的所有字段 -->
<sql id="userColumns">
    id, username, password, age, email
</sql><!-- 引用公共片段：查询所有用户 -->
<!-- 引用公共片段：多条件查询 -->
核心特点
1. <sql>标签的id属性是片段唯一标识，<include>通过refid引用对应id的片段；
2. 公共片段可嵌套<if>等动态标签，实现动态复用；
3. 适合抽取查询字段、固定条件、表名等重复出现的SQL片段。
2.6 <choose>/<when>/<otherwise>标签（分支选择）
场景说明
多条件互斥查询：例如查询用户时，优先根据id查询，若id为空则根据用户名查询，若用户名也为空则查询所有用户（类似Java的switch-case，只执行一个分支）。
代码实现（UserMapper.xml）
<!-- 7. 分支选择查询：choose+when+otherwise -->
核心特点
1. <choose>标签内只能有一个<when>标签被执行（满足条件的第一个<when>）；
2. 若所有<when>条件都不满足，执行<otherwise>标签内的内容（可选）；
3. 适合“多条件互斥”的场景，与<if>标签（多条件同时满足）区分开。
三、动态SQL常见问题与注意事项
参数判断错误原因：<if>标签的test属性中，参数判断逻辑错误（如判断字符串为空用“== null”，未判断空字符串）；解决：字符串判断需同时判断“!= null”和“!= ''”；数值类型（如age）只需判断“!= null”；集合判断需判断“!= null and !isEmpty()”。
SQL语法错误（多余逗号/AND）原因：未使用<where>/<set>标签，手动拼接时出现多余的AND/OR或逗号；解决：多条件查询用<where>替代“WHERE 1=1”，动态修改用<set>标签，自动处理多余符号。
foreach标签的collection属性错误原因：collection的值与接口方法的参数名不一致，或未用@Param指定集合参数名；解决：若接口方法参数是集合，且未用@Param，collection默认值为“list”（数组默认“array”）；建议用@Param明确指定参数名，避免出错。
批量操作失败原因：MySQL默认关闭批量操作支持，或SQL语句拼接错误；解决：在mybatis-config.xml的数据源配置中，添加允许批量操作的参数（<property name="allowMultiQueries" value="true"/>）。
SQL注入风险原因：动态SQL中使用${}拼接SQL（如排序字段），未做参数过滤；解决：优先使用#{}；若必须用${}（如排序字段），需手动过滤参数（如限制只能是“id”“username”等合法字段）。
四、动态SQL总结
动态SQL是MyBatis实战中最常用的特性，核心是“根据参数动态调整SQL结构”，关键要点如下：
核心标签：<if>（条件判断）、<where>（优化WHERE）、<set>（优化SET）、<foreach>（循环）、<sql>/<include>（复用）；
核心原则：优先使用<where>/<set>避免语法错误，用#{}避免SQL注入，用<foreach>处理批量操作；
实战技巧：抽取公共SQL片段提升复用性，根据业务场景选择合适的动态标签（多条件同时满足用<if>，互斥用<choose>）。
动态SQL可灵活适配各种复杂业务场景，结合前文的关联映射，可实现多表动态查询、动态关联等高级功能，是MyBatis进阶的核心基础。

