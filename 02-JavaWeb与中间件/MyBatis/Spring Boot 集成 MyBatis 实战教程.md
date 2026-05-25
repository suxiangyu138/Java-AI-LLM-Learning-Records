Spring Boot 集成 MyBatis 实战教程
Spring Boot 基于自动配置原理，大幅简化 Spring 与 MyBatis 的集成流程，无需编写复杂 XML 配置，仅通过依赖引入、简单配置和注解即可完成集成，是目前企业开发中最主流的持久层集成方式。本教程聚焦 Spring Boot 场景，从依赖准备、核心配置、代码编写到测试运行，全程贴合实际开发，同时补充避坑重点和常见问题排查，确保新手也能快速上手。
一、核心依赖准备（Maven）
Spring Boot 提供mybatis-spring-boot-starter 整合包，可自动配置 MyBatis 核心组件（SqlSessionFactory、SqlSession 等），无需手动引入过多依赖，核心依赖如下：
<!-- Spring Boot 父工程（统一版本管理，必加） -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>2.7.18</version> <!-- 稳定版本，兼容多数依赖 -->
    <relativePath/>
</parent>
<!-- 核心整合包：Spring Boot + MyBatis -->
<dependency>
    <groupId>org.mybatis.spring.boot</groupId>
    <artifactId>mybatis-spring-boot-starter</artifactId>
    <version>2.3.2</version> <!-- 与Spring Boot 2.7.x 兼容 -->
</dependency>
<!-- 数据库驱动（MySQL 8.0+，根据实际数据库替换） -->
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <scope>runtime</scope> <!-- 运行时依赖，无需编译 -->
</dependency>
<!-- 连接池（推荐Druid，性能优于默认HikariCP，可选但推荐） -->
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>druid-spring-boot-starter</artifactId>
    <version>1.2.20</version>
</dependency>
说明：若使用 Oracle、PostgreSQL 等其他数据库，替换对应的数据库驱动即可；若无需自定义连接池，可省略 Druid 依赖，Spring Boot 会默认使用 HikariCP 连接池。
二、核心配置（application.yml）
Spring Boot 集成 MyBatis 仅需在 application.yml（或 application.properties）中配置 数据源 和 MyBatis 相关参数，替代传统 XML 配置，简洁高效。

# 数据源配置（Druid连接池，若用默认HikariCP可简化）
spring:
  datasource:
    type: com.alibaba.druid.pool.DruidDataSource # 指定连接池类型
    driver-class-name: com.mysql.cj.jdbc.Driver # MySQL8.0+ 驱动类
    url: jdbc:mysql://localhost:3306/test_db?useSSL=false&serverTimezone=UTC&characterEncoding=utf8

    # 数据库地址、端口、数据库名根据实际修改，serverTimezone=UTC 解决时区问题
    username: root # 数据库用户名
    password: 123456 # 数据库密码

    # Druid连接池补充配置（可选，优化性能）
    druid:
      initial-size: 5 # 初始化连接数
      max-active: 20 # 最大活跃连接数
      min-idle: 5 # 最小空闲连接数
      max-wait: 60000 # 最大等待时间（毫秒）

# MyBatis 核心配置（关键，必配）
mybatis:
  mapper-locations: classpath:mapper/*.xml # Mapper.xml 文件存放路径
  type-aliases-package: com.example.entity # 实体类别名包，简化XML中resultType写法
  configuration:
    map-underscore-to-camel-case: true # 开启驼峰命名映射（数据库下划线→Java驼峰）
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl # 打印SQL日志（开发环境启用）

# 日志配置（可选，精细控制日志级别）
logging:
  level:
    com.example.mapper: debug # 只打印Mapper接口的SQL日志，避免冗余
关键配置说明：
mapper-locations：指定 Mapper.xml 文件的路径，若放在 resources/mapper 目录下，配置为 classpath:mapper/*.xml 即可批量扫描；
type-aliases-package：配置实体类所在包，后续在 Mapper.xml 中可直接使用实体类名（如 User），无需写全路径（com.example.entity.User）；
map-underscore-to-camel-case：开启后，数据库字段（如 user_name）会自动映射到 Java 实体类的驼峰字段（userName），无需手动配置 resultMap。
三、代码编写（实战步骤）
集成配置完成后，依次编写 实体类、Mapper接口、Mapper.xml、Service层，代码简洁且符合开发规范，全程无需手动配置 Bean。
3.1 编写实体类（Entity）
实体类与数据库表一一对应，开启驼峰映射后，字段名与数据库字段保持“下划线→驼峰”对应即可。
package com.example.entity;
/**
 * 实体类：与数据库user表对应
     */
    public class User {
    private Long id;       // 对应数据库id字段
    private String userName; // 对应数据库user_name字段（驼峰映射）
    private Integer age;   // 对应数据库age字段
    private String phone;  // 对应数据库phone字段
    // 必须提供无参构造、getter/setter方法，toString方法（便于调试）
    public User() {}
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", userName='" + userName + '\'' +
                ", age=" + age +
                ", phone='" + phone + '\'' +
                '}';
    }
    }
    3.2 编写 Mapper 接口
    Mapper 接口是 MyBatis 的核心，无需编写实现类，Spring Boot 会自动生成代理对象，通过注解或 XML 编写 SQL 映射。
    package com.example.mapper;
    import com.example.entity.User;
    import org.apache.ibatis.annotations.Param;
    import org.springframework.stereotype.Repository;
    import java.util.List;
    /**
 * Mapper接口：操作user表，@Repository 避免IDE报注入错误（可选）
     */
    @Repository
    public interface UserMapper {
    // 1. 根据ID查询用户（参数用@Param标注，便于XML中引用）
    User selectById(@Param("id") Long id);
    // 2. 查询所有用户
    List<User> selectAll();
    // 3. 新增用户（参数为实体类，XML中可直接引用实体类字段）
    int insert(User user);
    // 4. 修改用户（可选，根据需求添加）
    int update(User user);
    // 5. 删除用户（可选，根据需求添加）
    int deleteById(@Param("id") Long id);
    }
    3.3 编写 Mapper.xml 文件
    在 resources/mapper 目录下创建 UserMapper.xml，编写 SQL 映射，注意与 Mapper 接口的关联规则。
    <?xml version="1.0" encoding="UTF-8"?>
    <!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
    "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
    <!-- 关键：namespace必须与Mapper接口全路径完全一致 -->
    <mapper namespace="com.example.mapper.UserMapper">
    <!-- 新增用户：id与Mapper接口方法名一致，parameterType可省略（MyBatis自动识别） -->
    <insert id="insert">
        insert into user (user_name, age, phone)
        values (#{userName}, #{age}, #{phone})
    </insert>
    <!-- 根据ID查询：resultType指定返回实体类（因配置了别名，直接写User即可） -->
    <!-- 查询所有用户 -->
    <!-- 修改用户 -->
    <update id="update">
        update user
        set user_name = #{userName}, age = #{age}, phone = #{phone}
        where id = #{id}
    </update>
    <!-- 删除用户 -->
    <delete id="deleteById">
        delete from user where id = #{id}
    </delete>
    </mapper>
    关键规则：
    namespace 必须与 Mapper 接口的全路径（如 com.example.mapper.UserMapper）完全一致；
    SQL 标签（insert/select/update/delete）的 id 必须与 Mapper 接口的方法名完全一致；
    parameterType 可省略，MyBatis 会自动根据方法参数类型识别；
    resultType 若配置了别名包，可直接写实体类名，否则需写全路径。
    3.4 编写 Service 层（业务逻辑）
    Service 层依赖注入 Mapper 接口，编写业务逻辑，通过 @Service 注解标记为 Spring Bean。
    package com.example.service;
    import com.example.entity.User;
    import com.example.mapper.UserMapper;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.stereotype.Service;
    import java.util.List;
    /**
 * Service层：处理用户相关业务逻辑
     */
    @Service
    public class UserService {
    // 依赖注入 Mapper 接口（Spring Boot 自动生成代理对象，无需手动创建）
    @Autowired
    private UserMapper userMapper;
    // 根据ID查询用户
    public User getUserById(Long id) {
        // 直接调用Mapper接口方法，无需关注SqlSession等细节
        return userMapper.selectById(id);
    }
    // 查询所有用户
    public List<User> getAllUsers() {
        return userMapper.selectAll();
    }
    // 新增用户
    public int addUser(User user) {
        return userMapper.insert(user);
    }
    // 修改用户
    public int updateUser(User user) {
        return userMapper.update(user);
    }
    // 删除用户
    public int deleteUser(Long id) {
        return userMapper.deleteById(id);
    }
    }
    3.5 启动类配置（关键一步）
    在 Spring Boot 启动类上添加 @MapperScan 注解，扫描 Mapper 接口所在包，确保 Spring 能扫描到 Mapper 并生成代理对象。
    package com.example;
    import org.mybatis.spring.annotation.MapperScan;
    import org.springframework.boot.SpringApplication;
    import org.springframework.boot.autoconfigure.SpringBootApplication;
    /**
 * Spring Boot 启动类
     */
    @SpringBootApplication // 自动配置核心注解
    @MapperScan("com.example.mapper") // 扫描Mapper接口包，路径必须正确
    public class MyBatisSpringBootApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyBatisSpringBootApplication.class, args);
    }
    }
    说明：若未添加 @MapperScan 注解，可在每个 Mapper 接口上添加 @Mapper 注解（效果一致），但批量扫描更高效。
    四、测试集成效果
    使用 Spring Boot 内置的测试框架，编写测试用例，快速验证集成是否成功，无需手动启动项目。
    4.1 编写测试类
    package com.example.test;
    import com.example.entity.User;
    import com.example.service.UserService;
    import org.junit.jupiter.api.Test;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.boot.test.context.SpringBootTest;
    import java.util.List;
    /**
 * 测试类：@SpringBootTest 自动加载Spring容器，无需手动初始化
     */
    @SpringBootTest
    public class MyBatisIntegrationTest {
    @Autowired
    private UserService userService;
    // 测试：根据ID查询用户
    @Test
    public void testSelectById() {
        User user = userService.getUserById(1L);
        System.out.println("查询到的用户：" + user);
    }
    // 测试：查询所有用户
    @Test
    public void testSelectAll() {
        List<User> userList = userService.getAllUsers();
        System.out.println("所有用户：");
        userList.forEach(System.out::println);
    }
    // 测试：新增用户
    @Test
    public void testAddUser() {
        User newUser = new User();
        newUser.setUserName("张三");
        newUser.setAge(25);
        newUser.setPhone("13800138000");
        int count = userService.addUser(newUser);
        System.out.println("新增用户数量：" + count); // 新增成功返回1
    }
    }
    4.2 测试结果验证
    运行测试方法，控制台会打印 SQL 日志（因配置了 log-impl）和执行结果，若能正常查询、新增数据，说明集成成功。
    示例控制台输出：
    ==>  Preparing: select * from user where id = ? 
    ==> Parameters: 1(Long)
    <==    Columns: id, user_name, age, phone
    <==        Row: 1, 张三, 25, 13800138000
    <==      Total: 1
    查询到的用户：User{id=1, userName='张三', age=25, phone='13800138000'}
    五、关键注意事项（避坑重点）
    Mapper接口与Mapper.xml 关联必须一致：namespace 与 Mapper 全路径、SQL标签 id 与 Mapper 方法名，必须完全一致，否则会报「Invalid bound statement (not found)」错误（最常见坑）。
    数据源配置不能出错：MySQL8.0+ 驱动类必须是 com.mysql.cj.jdbc.Driver，URL 必须添加 serverTimezone=UTC，否则会出现时区异常或连接失败。
    @MapperScan 路径必须正确：注解配置的包路径必须包含所有 Mapper 接口，否则 Mapper 无法被扫描，注入时会报「No qualifying bean of type」错误。
    实体类必须有getter/setter方法：MyBatis 映射结果时需要通过 getter/setter 赋值，缺少会导致实体类字段为 null。
    SQL日志仅开发环境启用：生产环境需关闭 log-impl 配置，避免SQL日志泄露敏感信息，影响性能。
    参数传递注意：若 Mapper 方法有多个参数，需用 @Param 标注参数名，否则 XML 中无法正确引用；若参数是实体类，可直接引用实体类字段。
    六、常见问题排查
    问题1：Invalid bound statement (not found)
    最常见错误，原因及解决：
    原因1：Mapper.xml 的 namespace 与 Mapper 接口全路径不一致 → 修正 namespace；
    原因2：SQL标签 id 与 Mapper 接口方法名不一致 → 修正标签 id；
    原因3：mapper-locations 配置错误，Mapper.xml 未被加载 → 检查路径是否正确（如是否放在 resources/mapper 下）。
    问题2：No qualifying bean of type 'com.example.mapper.UserMapper' available
    原因：Mapper 未被 Spring 扫描到 → 解决：添加 @MapperScan("com.example.mapper") 注解，或在 Mapper 接口添加 @Mapper 注解。
    问题3：数据库连接失败（Communications link failure）
    原因及解决：
    MySQL 服务未启动 → 启动 MySQL 服务；
    URL、用户名、密码错误 → 修正 application.yml 中的数据源配置；
    驱动类错误（用了旧版本 com.mysql.jdbc.Driver）→ 替换为 com.mysql.cj.jdbc.Driver。
    七、总结
    Spring Boot 集成 MyBatis 核心流程可总结为 3 步：引入依赖 → 配置参数 → 编写代码，全程无需复杂 XML 配置，依赖 Spring Boot 自动配置能力，大幅提升开发效率。
    核心关键点：确保 Mapper 接口与 Mapper.xml 关联正确、数据源配置无误、Mapper 接口被 Spring 扫描到。掌握这些，就能快速完成集成，灵活实现数据库的增删改查操作，适配绝大多数企业开发场景。
