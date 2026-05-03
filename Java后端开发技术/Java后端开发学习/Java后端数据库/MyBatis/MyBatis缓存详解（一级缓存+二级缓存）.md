03.21 13:34
MyBatis缓存详解（一级缓存+二级缓存）
一、缓存概述
1.1 什么是MyBatis缓存
MyBatis缓存是MyBatis为提升数据库查询性能设计的核心机制，本质是将频繁查询、且数据不常变化的结果，临时存储在内存中。当再次执行相同查询时，无需重复访问数据库，直接从内存中获取结果，减少数据库IO操作，提升查询效率。
类比理解：缓存就像手机相册的“最近照片”，频繁查看的照片会临时存在手机内存，无需每次都从相册文件中读取，节省时间和资源。
1.2 缓存的核心作用
提升查询性能：减少数据库连接和SQL执行次数，尤其适合频繁查询、数据稳定的场景（如查询字典数据、用户基础信息）；
降低数据库压力：避免高频次查询对数据库造成的负载，减少IO开销；
简化开发：MyBatis缓存默认开启（一级缓存），无需额外编码，开箱即用。
1.3 MyBatis缓存分类
MyBatis提供两级缓存，遵循“先查二级缓存、再查一级缓存、最后查数据库”的查询顺序，两级缓存协同工作，互不冲突：
缓存级别
核心特点
生命周期
默认状态
一级缓存（本地缓存）
SqlSession级别的缓存，仅当前会话有效，不可跨会话共享
与SqlSession一致（一次请求/一个方法），关闭会话则缓存失效
默认开启，无需配置
二级缓存（全局缓存）
SqlSessionFactory级别的缓存，可跨所有SqlSession共享
与应用程序一致，应用启动时创建，关闭时销毁
默认关闭，需手动配置开启
补充：MyBatis还支持集成第三方缓存（如Redis、Ehcache），替代默认的二级缓存，适用于分布式项目（后续进阶内容）。
二、一级缓存（重点，默认开启）
2.1 一级缓存原理
一级缓存是SqlSession级别的缓存，MyBatis在创建SqlSession时，会在内部维护一个HashMap（缓存容器），用于存储查询结果。
核心流程：
第一次执行查询（如findById(1)）：SqlSession会先检查一级缓存，缓存中无对应数据，执行SQL查询数据库，将结果存入一级缓存，再返回结果；
第二次执行相同查询（findById(1)）：SqlSession直接从一级缓存中获取结果，无需访问数据库；
当SqlSession执行新增、修改、删除操作（add/delete/update），或调用clearCache()方法、关闭SqlSession时，一级缓存会被清空（避免缓存数据与数据库数据不一致）。
2.2 实战案例（验证一级缓存）
复用前文UserMapper接口和User实体类，编写测试方法，直观看到一级缓存的效果：
@Test
public void testFirstLevelCache() throws IOException {
    InputStream inputStream = Resources.getResourceAsStream("mybatis-config.xml");
    SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
    // 同一个SqlSession，验证一级缓存
    try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
        UserMapper userMapper = sqlSession.getMapper(UserMapper.class);
        // 第一次查询：id=1，访问数据库，存入一级缓存
        User user1 = userMapper.findById(1);
        System.out.println("第一次查询结果：" + user1);
        // 第二次查询：相同id=1，从一级缓存获取，不访问数据库
        User user2 = userMapper.findById(1);
        System.out.println("第二次查询结果：" + user2);
        // 验证：两个对象是同一个（缓存复用）
        System.out.println("两次查询结果是否相同：" + (user1 == user2)); // 输出true
    }
}
2.3 一级缓存注意事项
一级缓存不可跨SqlSession：两个不同的SqlSession，即使执行相同查询，也会各自查询数据库（缓存不共享）；
增删改操作会清空一级缓存：执行addUser、updateUser等操作后，当前SqlSession的一级缓存会被清空，避免缓存数据过时；
无需手动配置：一级缓存默认开启，无法关闭，只能通过clearCache()方法手动清空。
三、二级缓存（全局缓存，需手动开启）
3.1 二级缓存开启条件（三步配置）
二级缓存默认关闭，需满足3个条件才能生效，缺一不可，配置过程结合前文mybatis-config.xml和Mapper接口，保持项目结构一致：
步骤1：开启全局二级缓存（mybatis-config.xml）
在核心配置文件中添加<settings>标签，开启全局二级缓存（全局开关）：
<configuration>
<!-- 其他配置（properties、environments等）不变 -->
    <!-- 开启全局二级缓存，必须配置 -->
    <settings>
        <setting name="cacheEnabled" value="true"/> <!-- value=true开启，false关闭 -->
    </settings>
    <!-- 扫描Mapper接口（与前文注解配置一致） -->
    <mappers>
        <package name="com.example.mapper"/>
    </mappers>
</configuration>
步骤2：开启当前Mapper的二级缓存
在需要使用二级缓存的Mapper接口上，添加@CacheNamespace注解（注解配置方式），或在XML映射文件中添加<cache>标签（XML配置方式），二选一即可。
方式1：注解配置（对应前文注解配置方式）
package com.example.mapper;
import com.example.pojo.User;
import org.apache.ibatis.annotations.CacheNamespace;
import org.apache.ibatis.annotations.Select;
// 开启当前Mapper的二级缓存，注解添加在接口上
@CacheNamespace
public interface UserMapper {
    @Select("SELECT * FROM user WHERE id = #{id}")
    User findById(Integer id);
    // 其他方法（addUser、updateUser等）不变
}
方式2：XML配置（对应前文XML配置方式）
<!-- UserMapper.xml中添加<cache>标签，放在mapper标签内部最上方 -->
<mapper namespace="com.example.mapper.UserMapper">
    <!-- 开启当前Mapper的二级缓存 -->
    <cache/>
    <!-- 其他SQL标签（select、insert等）不变 -->
    </mapper>
步骤3：实体类实现序列化接口
二级缓存会将缓存数据序列化后存储（默认使用Java序列化），因此实体类必须实现Serializable接口，否则会报序列化异常：
package com.example.pojo;
import java.io.Serializable;
// 实现Serializable接口，支持二级缓存序列化存储
public class User implements Serializable {
    private Integer id;
    private String username;
    private String password;
    private Integer age;
    private String email;
    // 无参构造、getter/setter、toString方法不变
}
3.2 二级缓存原理
二级缓存是SqlSessionFactory级别的缓存，由SqlSessionFactory维护，所有通过该工厂创建的SqlSession，都能共享二级缓存中的数据。
核心流程：
SqlSession1执行查询（findById(1)）：先查二级缓存（无数据）→ 查一级缓存（无数据）→ 查数据库 → 结果存入一级缓存和二级缓存；
SqlSession1关闭（close()）：一级缓存数据会被同步到二级缓存中；
SqlSession2执行相同查询（findById(1)）：先查二级缓存（有数据）→ 直接获取结果，无需访问数据库；
当任意SqlSession执行增删改操作，会清空当前Mapper的二级缓存（保证缓存与数据库数据一致）。
3.3 实战案例（验证二级缓存）
@Test
public void testSecondLevelCache() throws IOException {
    InputStream inputStream = Resources.getResourceAsStream("mybatis-config.xml");
    SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
    // 第一个SqlSession
    try (SqlSession sqlSession1 = sqlSessionFactory.openSession()) {
        UserMapper mapper1 = sqlSession1.getMapper(UserMapper.class);
        User user1 = mapper1.findById(1);
        System.out.println("SqlSession1查询结果：" + user1);
        // 关闭SqlSession1，一级缓存数据同步到二级缓存
    }
    // 第二个SqlSession（不同会话）
    try (SqlSession sqlSession2 = sqlSessionFactory.openSession()) {
        UserMapper mapper2 = sqlSession2.getMapper(UserMapper.class);
        User user2 = mapper2.findById(1);
        System.out.println("SqlSession2查询结果：" + user2);
        // 验证：二级缓存共享，两个对象内容相同（可通过equals判断）
        System.out.println("两次查询结果是否相同：" + user1.equals(user2)); // 输出true
    }
}
四、缓存相关配置与高级用法
4.1 二级缓存的详细配置
无论是注解@CacheNamespace，还是XML<cache>标签，都支持自定义配置，优化缓存性能，以XML配置为例：
<cache 
    eviction="LRU" <!-- 缓存回收策略，默认LRU -->
    flushInterval="60000" <!-- 缓存刷新间隔（毫秒），60秒自动刷新一次 -->
    readOnly="false" <!-- 是否只读，false支持读写，true只读（性能更高） -->
    size="1024" <!-- 缓存最多存储的对象数量，默认1024 -->
/>
核心配置说明：
eviction（回收策略）：当缓存满时，删除缓存的规则，常用LRU（最近最少使用，推荐）、FIFO（先进先出）；
flushInterval：缓存自动刷新时间，避免缓存数据长期不更新；
readOnly：true时，缓存返回对象的引用，性能高但不安全；false时，返回对象副本，安全但性能稍低（推荐false）。
4.2 禁止单个方法使用缓存
若某个方法不需要使用缓存（如实时查询数据），可单独禁止，通过注解或XML配置实现：
// 注解方式：在方法上添加@CacheEvict或@Options，禁止缓存
public interface UserMapper {
    // 禁止当前方法使用缓存（查询结果不存入缓存，也不读取缓存）
    @Options(useCache = false)
    @Select("SELECT * FROM user WHERE id = #{id}")
    User findByIdNoCache(Integer id);
    // 清空当前Mapper的二级缓存（新增/修改/删除方法建议添加）
    @CacheEvict(allEntries = true)
    @Insert("INSERT INTO user (username, password) VALUES (#{username}, #{password})")
    int addUser(User user);
}
4.3 第三方缓存集成（进阶）
MyBatis默认的二级缓存是内存缓存，仅适用于单应用，分布式项目（多服务器）无法共享缓存，需集成第三方缓存（如Redis）。
核心步骤（简要）：
导入Redis相关依赖（pom.xml）；
创建Redis缓存实现类，实现MyBatis的Cache接口；
在Mapper接口上配置@CacheNamespace(implementation = RedisCache.class)，指定使用Redis缓存。
（详细集成步骤可后续进阶学习，此处重点掌握默认二级缓存用法）
五、缓存常见问题与解决方案
问题1：二级缓存不生效原因：未开启全局缓存（cacheEnabled=false）、未给Mapper配置缓存、实体类未实现Serializable接口；解决：核对三步开启条件，确保每一步配置正确，尤其注意实体类序列化。
问题2：缓存数据与数据库数据不一致原因：执行增删改操作后，未清空缓存，导致缓存数据过时；解决：在增删改方法上添加@CacheEvict(allEntries = true)，执行操作后自动清空缓存。
问题3：分布式项目缓存无法共享原因：默认二级缓存是本地内存缓存，多服务器无法共享；解决：集成第三方缓存（Redis、Ehcache），实现分布式缓存共享。
问题4：一级缓存导致数据重复原因：同一个SqlSession中，多次查询相同数据，返回同一个对象引用，修改对象会影响缓存；解决：查询后使用对象副本（如BeanUtils.copyProperties），避免直接修改查询结果对象。
六、缓存总结
MyBatis缓存的核心是“减少数据库访问，提升性能”，关键掌握以下3点：
一级缓存：SqlSession级别，默认开启，无需配置，注意增删改会清空缓存；
二级缓存：SqlSessionFactory级别，需手动开启，核心是实体类序列化+Mapper缓存配置；
缓存选型：单应用用默认二级缓存，分布式项目集成Redis等第三方缓存。
缓存是MyBatis性能优化的关键，合理使用缓存能大幅提升项目响应速度，同时需规避缓存不一致、缓存失效等问题，结合实际业务场景配置缓存策略。

