03.17 23:50
Java数据库相关核心知识点
Java数据库相关核心知识点
Java数据库编程是Java后端开发的核心模块之一，核心是通过Java语言与数据库建立连接，实现数据的CRUD、事务管理、性能优化等操作，适配企业级系统的数据存储与交互需求。其核心依赖JDBC规范，结合ORM框架、连接池等技术，兼顾开发效率、数据安全性与系统性能，广泛应用于各类Java后端项目（如电商、政务、管理系统等）。
一、Java数据库编程基础（必懂）
1.1 核心概念（Java专属关联）
Java数据库编程的基础的是理解数据库与Java语言的交互逻辑，核心概念需结合Java技术栈记忆，重点如下：
数据库选型（Java后端常用）：
关系型数据库（主流）：MySQL（中小企业首选，开源免费、轻量）、Oracle（大型企业、高并发场景，收费）、SQL Server（微软生态，适配.NET+Java混合架构），核心适配Java后端的事务、复杂查询场景。
非关系型数据库（补充）：Redis（缓存首选，搭配Java的Jedis、Redisson客户端）、MongoDB（文档存储，适配非结构化数据，Java客户端MongoTemplate）、Elasticsearch（全文检索，Java客户端HighLevelRestClient）。
JDBC（Java Database Connectivity）：Java访问数据库的标准接口，定义了Java与数据库交互的规范，由数据库厂商提供具体实现（驱动），是所有Java数据库操作的底层基础，无需关注不同数据库的底层差异，实现跨数据库兼容。
数据库驱动：JDBC规范的具体实现类，Java程序通过驱动连接数据库。例如MySQL的驱动（mysql-connector-java）、Oracle的驱动（ojdbc），需在项目中引入对应依赖（Maven/Gradle）。
ORM框架（Java后端必备）：对象关系映射，将Java实体类（Entity）与数据库表一一映射，无需手写复杂SQL，简化开发。Java主流ORM框架：MyBatis（半自动化，灵活可控，最常用）、Hibernate（全自动化，封装度高）、Spring Data JPA（基于Hibernate，简化CRUD）。
数据库连接池（Java性能优化核心）：管理数据库连接的容器，避免频繁创建/关闭连接导致的资源开销，提升并发性能。Java主流连接池：HikariCP（SpringBoot默认，性能最优）、Druid（阿里出品，功能强大，支持监控）、C3P0（老牌连接池，兼容性好但性能一般）。
1.2 Java数据库编程核心依赖
Java项目操作数据库，需引入对应依赖，核心依赖如下（以Maven为例）：
JDBC驱动依赖（MySQL）： <dependency> <groupId>mysql</groupId> <artifactId>mysql-connector-java</artifactId> <version>8.0.36</version> <scope>runtime</scope> </dependency>
MyBatis依赖（主流ORM）： <dependency> <groupId>org.mybatis</groupId> <artifactId>mybatis</artifactId> <version>3.5.13&lt;/version&gt; &lt;/dependency&gt; <!-- Spring整合MyBatis（实际开发常用） --> <dependency> <groupId>org.mybatis.spring.boot</groupId> <artifactId>mybatis-spring-boot-starter</artifactId> <version>3.0.3</version> </dependency>
连接池依赖（HikariCP，SpringBoot默认无需手动引入）： <dependency> <groupId>com.zaxxer</groupId> <artifactId>HikariCP</artifactId> <version>5.0.1</version> </dependency>
二、Java数据库核心操作（JDBC+MyBatis）
2.1 JDBC原生操作（底层原理，必懂）
JDBC是Java操作数据库的底层规范，所有ORM框架、连接池都基于JDBC封装，核心步骤分为5步，需掌握其原理（实际开发中不直接使用原生JDBC，避免代码冗余）：
加载数据库驱动：Java8及以上版本无需手动加载，驱动会通过SPI机制自动加载（核心类：DriverManager）。 // 手动加载驱动（旧版本，Java8+可省略） Class.forName("com.mysql.cj.jdbc.Driver");
建立数据库连接：通过DriverManager获取Connection对象，传入数据库URL、用户名、密码（核心：连接的核心载体）。 String url = "jdbc:mysql://localhost:3306/test_db?useSSL=false&serverTimezone=UTC"; String username = "root"; String password = "123456"; Connection connection = DriverManager.getConnection(url, username, password);
创建执行SQL的对象：通过Connection创建Statement或PreparedStatement（推荐使用PreparedStatement，避免SQL注入）。 // PreparedStatement（参数化查询，安全） String sql = "SELECT * FROM user WHERE id = ?"; PreparedStatement pstmt = connection.prepareStatement(sql);
执行SQL并处理结果：执行查询（executeQuery()）返回ResultSet结果集，执行增删改（executeUpdate()）返回影响行数，处理结果集封装为Java对象。 // 绑定参数（索引从1开始） pstmt.setInt(1, 1); // 执行查询 ResultSet rs = pstmt.executeQuery(); // 处理结果集 List<User> userList = new ArrayList<>(); while (rs.next()) { User user = new User(); user.setId(rs.getInt("id")); user.setName(rs.getString("name")); userList.add(user); }
释放资源：按“ResultSet → Statement/PreparedStatement → Connection”的顺序关闭资源，避免资源泄露（推荐使用try-with-resources语法，自动关闭）。 // try-with-resources自动关闭资源（Java7+） try (Connection conn = DriverManager.getConnection(url, username, password); PreparedStatement pstmt = conn.prepareStatement(sql); ResultSet rs = pstmt.executeQuery()) { // 执行SQL、处理结果集 } catch (SQLException e) { e.printStackTrace(); }
原生JDBC注意事项：频繁创建/关闭Connection会导致性能损耗；SQL拼接易引发SQL注入；代码冗余，需手动封装重复逻辑，因此实际开发中优先使用ORM框架。
2.2 MyBatis核心操作（实际开发首选）
MyBatis是Java后端最常用的ORM框架，半自动化封装JDBC，兼顾灵活性与开发效率，核心分为“配置 → 映射 → 调用”三步：
2.2.1 MyBatis核心配置
核心配置文件（mybatis-config.xml）：配置数据库连接信息、别名、映射器（Mapper）、插件等。 <configuration> <!-- 配置环境（数据库连接信息） --> <environments default="development"> <environment id="development"> <transactionManager type="JDBC"/> <dataSource type="POOLED"> <property name="driver" value="com.mysql.cj.jdbc.Driver"/> <property name="url" value="jdbc:mysql://localhost:3306/test_db?useSSL=false&serverTimezone=UTC"/> <property name="username" value="root"/> <property name="password" value="123456"/> </dataSource> </environment> </environments> <!-- 配置映射器（关联Mapper.xml） --> <mappers> <mapper resource="mapper/UserMapper.xml"/> </mappers> </configuration>
SpringBoot整合MyBatis（简化配置）：无需编写mybatis-config.xml，直接在application.yml中配置，自动扫描Mapper接口和映射文件。 spring: datasource: driver-class-name: com.mysql.cj.jdbc.Driver url: jdbc:mysql://localhost:3306/test_db?useSSL=false&serverTimezone=UTC username: root password: 123456 # 配置连接池（HikariCP） hikari: maximum-pool-size: 10 # 最大连接数 minimum-idle: 5 # 最小空闲连接 connection-timeout: 3000 # 连接超时时间（ms） mybatis: mapper-locations: classpath:mapper/*.xml # 映射文件路径 type-aliases-package: com.example.demo.entity # 实体类别名包 configuration: map-underscore-to-camel-case: true # 下划线转驼峰（如user_name → userName）
2.2.2 核心组件与操作
Mapper接口：定义数据库操作方法，无需实现类，MyBatis自动生成代理实现，方法名与Mapper.xml中的SQL id对应。 // UserMapper接口 public interface UserMapper { // 根据id查询用户 User selectById(@Param("id") Integer id); // 新增用户 int insert(User user); // 更新用户 int update(User user); // 删除用户 int deleteById(@Param("id") Integer id); }
Mapper.xml映射文件：编写SQL语句，与Mapper接口方法关联，支持动态SQL（if、foreach、where等），解决复杂查询场景。 <?xml version="1.0" encoding="UTF-8" ?> <!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd"> <mapper namespace="com.example.demo.mapper.UserMapper"&gt; <!-- 结果集映射（实体类与表字段对应） --> <resultMap id="userMap" type="com.example.demo.entity.User"> <id column="id" property="id"/> <result column="name" property="name"/> <result column="age" property="age"/&gt; &lt;/resultMap&gt; <!-- 查询：根据id查询 --> <select id="selectById" resultMap="userMap"> SELECT id, name, age FROM user WHERE id = #{id} </select> <!-- 新增：动态SQL（非空字段才插入） --> <insert id="insert" parameterType="com.example.demo.entity.User"> INSERT INTO user (name, age) VALUES (#{name}, #{age}) </insert> <!-- 更新：动态SQL（非空字段才更新） --> <update id="update" parameterType="com.example.demo.entity.User"> UPDATE user <set> <if test="name != null">name = #{name},</if> <if test="age != null">age = #{age}</if> </set> WHERE id = #{id} &lt;/update&gt; <!-- 删除：根据id删除 --> <delete id="deleteById"> DELETE FROM user WHERE id = #{id} </delete> </mapper>
调用Mapper接口：通过SqlSession（MyBatis核心会话对象）获取Mapper接口代理对象，调用方法执行SQL；SpringBoot中可通过@Autowired直接注入Mapper接口使用。 // SpringBoot中直接注入使用 @Service public class UserService { @Autowired private UserMapper userMapper; // 查询用户 public User getUserById(Integer id) { return userMapper.selectById(id); } // 新增用户 public boolean addUser(User user) { return userMapper.insert(user) > 0; } }
三、Java数据库进阶技术（企业级开发必备）
3.1 事务管理（Java+数据库协同）
事务是保证数据一致性的核心，Java中事务管理分为“编程式事务”和“声明式事务”，结合数据库的ACID特性，解决并发场景下的数据安全问题。
JDBC编程式事务：通过Connection对象手动控制事务，核心方法：setAutoCommit(false)（关闭自动提交）、commit()（提交事务）、rollback()（回滚事务）。 try (Connection conn = DriverManager.getConnection(url, username, password)) { conn.setAutoCommit(false); // 关闭自动提交，开启事务 // 执行多个SQL操作（如转账：扣减A账户，增加B账户） String sql1 = "UPDATE account SET balance = balance - 100 WHERE id = 1"; String sql2 = "UPDATE account SET balance = balance + 100 WHERE id = 2"; PreparedStatement pstmt1 = conn.prepareStatement(sql1); PreparedStatement pstmt2 = conn.prepareStatement(sql2); pstmt1.executeUpdate(); pstmt2.executeUpdate(); conn.commit(); // 所有操作成功，提交事务 } catch (SQLException e) { conn.rollback(); // 出现异常，回滚事务 e.printStackTrace(); }
Spring声明式事务（实际开发首选）：通过@Transactional注解实现，无需手动控制commit/rollback，Spring自动管理事务，简化开发。 @Service public class AccountService { @Autowired private AccountMapper accountMapper; // 声明式事务：添加注解即可，默认遇到异常回滚 @Transactional(rollbackFor = Exception.class) // 所有异常都回滚 public void transfer(Integer fromId, Integer toId, Integer money) { // 扣减转出账户 accountMapper.deductBalance(fromId, money); // 模拟异常（测试回滚） // int i = 1 / 0; // 增加转入账户 accountMapper.addBalance(toId, money); } }
事务隔离级别（Java配置）：Spring中可通过@Transactional(isolation = Isolation.XXX)配置，对应数据库的隔离级别，解决脏读、不可重复读、幻读问题：
Isolation.READ_UNCOMMITTED（读未提交）：最低级别，允许读取未提交数据，存在脏读。
Isolation.READ_COMMITTED（读已提交）：默认级别（MySQL默认），只能读取已提交数据，避免脏读。
Isolation.REPEATABLE_READ（可重复读）：MySQL默认级别，保证同一事务内多次读取结果一致，避免不可重复读。
Isolation.SERIALIZABLE（串行化）：最高级别，完全避免并发问题，但性能极低，适合数据一致性要求极高的场景。
3.2 连接池详解（Java性能优化核心）
Java中数据库连接是稀缺资源，频繁创建/关闭连接会严重影响性能，连接池通过“复用连接”解决该问题，核心原理：初始化一定数量的连接，存放在连接池中，程序需要时从池中获取，使用完毕后归还，避免频繁创建/关闭。
主流连接池对比（Java后端）： 连接池特点适用场景HikariCPSpringBoot默认，性能最优，轻量、无冗余，连接获取速度快所有Java后端项目，尤其是高并发场景Druid阿里出品，功能强大，支持监控、防SQL注入、统计分析，扩展性好中大型项目，需要监控和安全防护的场景C3P0老牌连接池，兼容性好，稳定性强，但性能一般老旧项目，兼容性要求高的场景
连接池核心配置（HikariCP）： spring: datasource: hikari: maximum-pool-size: 10 # 最大连接数（根据CPU核心数和并发量配置，一般为CPU核心数*2+1） minimum-idle: 5 # 最小空闲连接，保证有可用连接 connection-timeout: 3000 # 连接超时时间（ms），超过时间未获取连接则报错 idle-timeout: 600000 # 空闲连接超时时间（ms），超过时间未使用则回收 max-lifetime: 1800000 # 连接最大生命周期（ms），避免连接老化
3.3 性能优化（Java数据库编程重点）
Java后端数据库性能优化，需从“SQL优化、索引优化、代码优化”三个维度入手，结合Java技术栈特点，核心优化点如下：
SQL优化（MyBatis场景）：
避免全表扫描：给查询条件字段添加索引（如where、order by、join on字段）。
简化复杂查询：避免嵌套过深的子查询，用join替代子查询；避免select *，只查询需要的字段（减少数据传输）。
使用动态SQL：MyBatis的if、foreach等标签，避免拼接无效SQL（如where后无条件）。
批量操作：使用MyBatis的foreach标签实现批量插入、更新，减少SQL执行次数（避免循环调用单条插入）。 <!-- 批量插入 --> <insert id="batchInsert" parameterType="java.util.List"> INSERT INTO user (name, age) VALUES <foreach collection="list" item="item" separator=","> (#{item.name}, #{item.age}) </foreach> </insert>
索引优化（Java开发关注）：
合理设计索引：主键索引（必加）、唯一索引（用于唯一约束，如手机号）、复合索引（多字段查询，如where name and age）。
避免过度索引：索引会增加插入、更新、删除的开销，一张表索引不超过5个。
MyBatis中使用覆盖索引：查询字段全部包含在索引中，避免回表查询（提升查询效率）。
代码优化：
使用连接池：避免频繁创建/关闭连接，配置合理的连接池参数。
缓存优化：查询频繁、修改少的数据，用Redis缓存（如字典数据），减少数据库查询压力。
避免长事务：长事务会占用连接，导致连接池耗尽，拆分长事务为多个短事务。
分页查询：大数据量查询时，使用limit分页，避免一次性查询所有数据（导致内存溢出）。
3.4 分布式场景拓展（进阶）
中大型Java后端项目多为分布式架构，数据库需适配分布式场景，核心技术如下：
分库分表：应对数据量大、查询缓慢的问题，通过中间件实现（ShardingSphere、MyCat），分为水平分片（按行拆分，如按用户ID分表）、垂直分库（按业务拆分，如用户库、订单库）。
分布式事务：解决分布式架构中多数据库操作的数据一致性问题，主流方案：2PC（两阶段提交）、TCC（补偿事务）、SAGA模式、本地消息表，Java中可通过Seata框架简化实现。
读写分离：主库负责写入（insert、update、delete），从库负责读取（select），通过中间件实现读写路由（如ShardingSphere），提升查询性能，避免主库压力过大。
四、Java数据库编程常见问题与避坑指南
常见问题
产生原因
解决方案
SQL注入
MyBatis中使用${}拼接SQL，未使用#{}参数化查询；原生JDBC拼接SQL字符串
MyBatis优先使用#{}，避免${}；必须使用${}时（如动态表名），过滤用户输入；原生JDBC使用PreparedStatement
连接泄露
原生JDBC未关闭Connection、Statement、ResultSet；连接池配置不合理，连接未正常归还
使用try-with-resources自动关闭资源；检查连接池配置，确保连接使用后归还；监控连接池状态
事务不回滚
@Transactional注解配置错误（如未指定rollbackFor）；异常被try-catch捕获，未抛出；非public方法添加@Transactional
配置rollbackFor = Exception.class；异常抛出不捕获；@Transactional只作用于public方法
查询性能差
未加索引、SQL逻辑不合理、全表扫描；未使用分页、缓存；连接池参数不合理
添加合适索引；优化SQL；使用分页和Redis缓存；调整连接池参数
MyBatis映射异常
实体类字段与表字段不匹配；Mapper接口方法名与Mapper.xml中SQL id不对应；namespace错误
开启下划线转驼峰；检查字段映射和方法名；确保namespace与Mapper接口全路径一致
五、总结
Java数据库编程的核心逻辑是“通过JDBC规范实现Java与数据库的交互，结合ORM框架、连接池、事务管理等技术，实现高效、安全、可扩展的数据操作”。基础层面需掌握JDBC原理、MyBatis使用；进阶层面需掌握事务管理、连接池优化、分布式场景适配；实践层面需规避SQL注入、连接泄露等常见问题，结合业务场景选择合适的数据库和优化策略。
对于Java后端开发者而言，数据库编程是必备技能，需重点掌握MyBatis、事务管理和性能优化，同时了解分布式数据库相关技术，适配中大型项目的开发需求。

