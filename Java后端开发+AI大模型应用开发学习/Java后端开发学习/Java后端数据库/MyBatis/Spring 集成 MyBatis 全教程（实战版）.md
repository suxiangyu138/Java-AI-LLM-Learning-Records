03.20 16:54
Spring 集成 MyBatis 全教程（实战版）
Spring 作为轻量级开源框架，负责管理Bean的生命周期和依赖注入；MyBatis 作为优秀的持久层框架，专注于SQL映射和数据库操作。两者集成后，可充分发挥Spring的依赖管理优势和MyBatis的灵活SQL特性，简化持久层开发，提升项目可维护性。本教程将详细讲解两种主流集成方式（XML配置、Java配置），覆盖从环境搭建到测试运行的全流程，同时补充关键注意事项和问题排查技巧。
一、集成核心前提与依赖准备
1.1 核心依赖
Spring 集成 MyBatis 需引入4类核心依赖（以Maven为例），分别是Spring核心依赖、Spring JDBC依赖、MyBatis核心依赖、Spring与MyBatis集成专用依赖（mybatis-spring），若使用Spring Boot，可直接引入整合 starter 简化配置。
方式1：传统Spring + MyBatis（非Spring Boot）
<!-- Spring 核心依赖 -->
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-context</artifactId>
    <version>5.3.28</version>
</dependency>
<!-- Spring JDBC 依赖（负责数据源管理、事务控制） -->
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-jdbc</artifactId>
    <version>5.3.28</version>
</dependency>
<!-- MyBatis 核心依赖 -->
<dependency>
    <groupId>org.mybatis</groupId>
    <artifactId>mybatis</artifactId>
    <version>3.5.16</version>
</dependency>
<!-- Spring 与 MyBatis 集成依赖（核心桥梁） -->
<dependency>
    <groupId>org.mybatis</groupId>
    <artifactId>mybatis-spring</artifactId>
    <version>2.1.6</version>
</dependency>
<!-- 数据库驱动（MySQL为例） -->
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <version>8.0.36</version>
    <scope>runtime</scope>
</dependency>
<!-- 连接池（可选，推荐使用Druid，提升性能） -->
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>druid</artifactId>
    <version>1.2.20</version>
</dependency>
方式2：Spring Boot + MyBatis（推荐，简化配置）
Spring Boot 提供了 mybatis-spring-boot-starter，可自动配置大部分组件，无需手动编写复杂XML：
<!-- Spring Boot 父工程（统一版本管理） -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>2.7.18</version>
    <relativePath/>
</parent>
<!-- MyBatis + Spring Boot 整合 starter -->
<dependency>
    <groupId>org.mybatis.spring.boot</groupId>
    <artifactId>mybatis-spring-boot-starter</artifactId>
    <version>2.3.2</version>
</dependency>
<!-- 数据库驱动 -->
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <scope>runtime</scope>
</dependency>
<!-- 连接池（Spring Boot 默认使用HikariCP，也可替换为Druid） -->
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>druid-spring-boot-starter</artifactId>
    <version>1.2.20</version>
</dependency>
1.2 核心集成原理
Spring 集成 MyBatis 的核心是通过 mybatis-spring 组件搭建桥梁，将 MyBatis 的核心对象（SqlSessionFactory、SqlSession、Mapper接口）交给 Spring 容器管理，实现依赖注入和生命周期托管，核心逻辑如下：
Spring 管理数据源（DataSource），为 MyBatis 提供数据库连接支持；
通过 SqlSessionFactoryBean（MyBatis-Spring 提供）创建 SqlSessionFactory（MyBatis 核心对象），并将数据源、MyBatis 配置等注入其中；
Spring 管理 SqlSession（通过 SqlSessionTemplate，线程安全，无需手动关闭）；
通过 MapperScannerConfigurer 自动扫描 Mapper 接口，将其注册为 Spring Bean，实现 Mapper 接口的依赖注入。
二、传统Spring + MyBatis 集成（XML配置方式）
适用于传统SSM项目，通过XML配置Spring和MyBatis的核心组件，步骤清晰，可灵活定制配置。
步骤1：配置数据源（DataSource）
在 Spring 配置文件（applicationContext.xml）中配置数据源，推荐使用Druid连接池（性能更优），也可使用Spring自带的BasicDataSource。
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
                           http://www.springframework.org/schema/beans/spring-beans.xsd">
    <!-- 1. 配置Druid数据源 -->
<bean id="dataSource" class="com.alibaba.druid.pool.DruidDataSource">
<property name="driverClassName" value="com.mysql.cj.jdbc.Driver"/>
        <property name="url" value="jdbc:mysql://localhost:3306/test_db?useSSL=false&serverTimezone=UTC&characterEncoding=utf8"/>
       <property name="username" value="root"/>
        <property name="password" value="123456"/>
        <!-- 连接池补充配置（可选） -->
        <property name="initialSize" value="5"/>
        <property name="maxActive" value="20"/>
        <property name="minIdle" value="5"/>
    </bean>
</beans>
步骤2：配置 SqlSessionFactoryBean
SqlSessionFactoryBean 是 MyBatis-Spring 提供的核心Bean，用于创建 SqlSessionFactory，需注入数据源、MyBatis 配置文件路径、Mapper.xml 路径等。
<!-- 2. 配置 SqlSessionFactoryBean，创建 SqlSessionFactory -->
<bean id="sqlSessionFactory" class="org.mybatis.spring.SqlSessionFactoryBean">
    <!-- 注入数据源 -->
    <property name="dataSource" ref="dataSource"/>
    <!-- 配置 MyBatis 核心配置文件路径（可选，若有全局配置需指定） -->
    <property name="configLocation" value="classpath:mybatis-config.xml"/>
    <!-- 配置 Mapper.xml 文件路径（批量扫描） -->
    <property name="mapperLocations" value="classpath:mapper/*.xml"/>
    <!-- 可选：配置别名包，简化Mapper.xml中的实体类全路径 -->
    <property name="typeAliasesPackage" value="com.example.entity"/>
</bean>
步骤3：配置 Mapper 扫描器
通过 MapperScannerConfigurer 自动扫描指定包下的 Mapper 接口，将其注册为 Spring Bean，无需手动为每个 Mapper 接口创建实现类。
<!-- 3. 配置 Mapper 扫描器，自动扫描 Mapper 接口 -->
<bean class="org.mybatis.spring.mapper.MapperScannerConfigurer">
    <!-- 扫描的 Mapper 接口包路径（多个包用逗号分隔） -->
    <property name="basePackage" value="com.example.mapper"/>
    <!-- 关联 SqlSessionFactory（若容器中只有一个SqlSessionFactory，可省略） -->
    <property name="sqlSessionFactoryBeanName" value="sqlSessionFactory"/>
</bean>
步骤4：配置 MyBatis 全局配置（可选）
创建 mybatis-config.xml 文件，配置 MyBatis 全局参数（如日志、驼峰命名映射、插件等），若无需全局配置，可省略此步骤。
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE configuration PUBLIC "-//mybatis.org//DTD Config 3.0//EN"
"http://mybatis.org/dtd/mybatis-3-config.dtd">
<configuration>
    <!-- 日志配置（可选，打印SQL日志） -->
<settings>
        <setting name="logImpl" value="STDOUT_LOGGING"/>
       <!-- 开启驼峰命名映射（数据库字段下划线转Java实体类驼峰） -->
        <setting name="mapUnderscoreToCamelCase" value="true"/>
    </settings>
    <!-- 插件配置（如分页插件、数据脱敏插件） -->
<plugins>
        <!-- 示例：分页插件（PageHelper） -->
<plugin interceptor="com.github.pagehelper.PageInterceptor">
            <property name="helperDialect" value="mysql"/>
        </plugin>
    </plugins>
</configuration>
步骤5：编写业务代码与测试
5.1 编写实体类（Entity）
package com.example.entity;
// 实体类，与数据库表对应（开启驼峰映射后，数据库字段user_name对应实体类userName）
public class User {
    private Long id;
    private String userName;
    private Integer age;
    private String phone;
    //  getter/setter 方法、toString方法
}
5.2 编写 Mapper 接口
package com.example.mapper;
import com.example.entity.User;
import org.apache.ibatis.annotations.Param;
import java.util.List;
// Mapper接口，无需实现类，MyBatis自动生成代理对象
public interface UserMapper {
    // 根据ID查询用户
    User selectById(@Param("id") Long id);
    // 查询所有用户
    List<User> selectAll();
    // 新增用户
    int insert(User user);
}
5.3 编写 Mapper.xml 文件
在 classpath:mapper 目录下创建 UserMapper.xml，编写SQL映射：
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
"http://mybatis.org/dtd/mybatis-3-mapper.dtd"><!-- namespace 必须与 Mapper 接口全路径一致 -->
<mapper namespace="com.example.mapper.UserMapper">
    <!-- 新增用户 -->
   <insert id="insert" parameterType="com.example.entity.User">
        insert into user (user_name, age, phone)
        values (#{userName}, #{age}, #{phone})
    </insert>
    <!-- 根据ID查询用户 -->
    <!-- 查询所有用户 -->
   </mapper>
5.4 编写 Service 层（依赖注入 Mapper）
package com.example.service;
import com.example.entity.User;
import com.example.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
@Service // 标记为Spring Service Bean
public class UserService {
    // 依赖注入 Mapper 接口（Spring自动生成代理对象）
    @Autowired
    private UserMapper userMapper;
    // 业务方法：根据ID查询用户
    public User getUserById(Long id) {
        return userMapper.selectById(id);
    }
    // 业务方法：查询所有用户
    public List<User> getAllUsers() {
        return userMapper.selectAll();
    }
    // 业务方法：新增用户
    public int addUser(User user) {
        return userMapper.insert(user);
    }
}
5.5 测试集成效果
package com.example.test;
import com.example.entity.User;
import com.example.service.UserService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
public class MyBatisSpringTest {
    public static void main(String[] args) {
        // 加载Spring配置文件，获取容器
        ApplicationContext context = new ClassPathXmlApplicationContext("applicationContext.xml");
        // 获取Service Bean
        UserService userService = context.getBean(UserService.class);
        // 测试查询
        User user = userService.getUserById(1L);
        System.out.println("查询用户：" + user);
        // 测试新增
        User newUser = new User();
        newUser.setUserName("李四");
        newUser.setAge(28);
        newUser.setPhone("13800138000");
        int count = userService.addUser(newUser);
        System.out.println("新增用户数量：" + count);
    }
}
三、Spring Boot + MyBatis 集成（Java配置方式，推荐）
Spring Boot 基于自动配置原理，大幅简化配置，无需编写复杂XML，只需通过配置文件和注解即可完成集成，是目前主流的集成方式。
步骤1：配置 application.yml（核心配置）
在 resources 目录下创建 application.yml 文件，配置数据源、MyBatis 相关参数，替代传统XML配置。
# 数据源配置（Druid连接池）
spring:
  datasource:
    type: com.alibaba.druid.pool.DruidDataSource
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/test_db?useSSL=false&serverTimezone=UTC&characterEncoding=utf8
    username: root
    password: 123456
    # Druid连接池补充配置
    druid:
      initial-size: 5
      max-active: 20
      min-idle: 5
# MyBatis 配置
mybatis:
  # Mapper.xml 文件路径
  mapper-locations: classpath:mapper/*.xml
  # 实体类别名包（简化Mapper.xml中的resultType）
  type-aliases-package: com.example.entity
  # 全局配置（对应mybatis-config.xml中的settings）
  configuration:
    # 开启驼峰命名映射
    map-underscore-to-camel-case: true
    # 打印SQL日志（开发环境启用，生产环境关闭）
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
# 可选：日志配置（更精细的日志控制）
logging:
  level:
    com.example.mapper: debug # 只打印Mapper接口的SQL日志
步骤2：编写核心注解配置
只需在Spring Boot启动类上添加 @MapperScan 注解，扫描Mapper接口，无需额外配置 SqlSessionFactory 和 MapperScannerConfigurer（Spring Boot 自动配置）。
package com.example;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
// Spring Boot 启动类
@SpringBootApplication
// 扫描 Mapper 接口包（与application.yml中配置一致，二选一即可）
@MapperScan("com.example.mapper")
public class MyBatisSpringBootApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyBatisSpringBootApplication.class, args);
    }
}
步骤3：编写业务代码（与传统方式一致）
实体类（Entity）、Mapper接口、Mapper.xml 文件、Service层代码与传统方式完全一致，无需修改。唯一区别是：Spring Boot 自动完成依赖注入，无需手动配置Spring Bean。
步骤4：测试集成效果（Spring Boot 测试）
使用 Spring Boot 内置的测试框架，编写测试用例，简化测试流程：
package com.example.test;
import com.example.entity.User;
import com.example.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.List;
// 标记为Spring Boot测试类，自动加载Spring容器
@SpringBootTest
public class MyBatisSpringBootTest {
    @Autowired
    private UserService userService;
    @Test
    public void testSelectById() {
        User user = userService.getUserById(1L);
        System.out.println("查询用户：" + user);
    }
    @Test
    public void testSelectAll() {
        List<User> userList = userService.getAllUsers();
        userList.forEach(System.out::println);
    }
}
四、关键注意事项（避坑重点）
Mapper接口与Mapper.xml 关联必须正确：Mapper.xml 的 namespace 必须与 Mapper 接口的全路径完全一致；SQL标签的 id 必须与 Mapper 接口的方法名完全一致，否则会报「Invalid bound statement (not found)」错误。
数据源配置必须正确：驱动类、URL、用户名、密码错误会导致数据库连接失败，MySQL8.0+ 驱动类为 com.mysql.cj.jdbc.Driver，URL需添加serverTimezone=UTC 参数。
Mapper扫描路径必须正确：@MapperScan 注解或 MapperScannerConfigurer 配置的包路径，必须包含所有 Mapper 接口，否则 Mapper 无法被Spring扫描到，注入时会报「No qualifying bean of type」错误。
驼峰命名映射配置：开启 map-underscore-to-camel-case: true 后，数据库字段（如 user_name）会自动映射到Java实体类的驼峰字段（userName），避免手动配置 resultMap，减少代码量。
SqlSession 线程安全：Spring 集成 MyBatis 后，通过 SqlSessionTemplate 管理 SqlSession，其内部使用 ThreadLocal 保证线程安全，无需手动关闭 SqlSession，避免线程安全问题。
插件配置注意：若使用分页插件（如PageHelper），需确保插件版本与 MyBatis、Spring 版本兼容，否则会出现拦截器失效、SQL执行异常等问题。
Spring Boot 自动配置失效排查：若出现 Mapper 注入失败，可检查是否添加了 mybatis-spring-boot-starter 依赖、是否配置了 mapper-locations、是否添加了 @MapperScan 注解。
五、常见问题排查
问题1：Invalid bound statement (not found)
原因：Mapper接口与Mapper.xml 关联错误，常见场景：
Mapper.xml 的 namespace 与 Mapper 接口全路径不一致；
SQL标签 id 与 Mapper 接口方法名不一致；
Mapper.xml 文件路径未配置，或路径配置错误，导致MyBatis无法加载。
解决：检查上述三点，确保关联正确，Spring Boot 中确认 mapper-locations 配置正确。
问题2：No qualifying bean of type 'com.example.mapper.UserMapper' available
原因：Mapper接口未被Spring扫描到，常见场景：
未添加 @MapperScan 注解（Spring Boot）或 MapperScannerConfigurer 配置（传统Spring）；
@MapperScan 注解的包路径错误，未包含 Mapper 接口；
Mapper接口未添加 @Mapper 注解（可选，若未使用 @MapperScan，可在每个Mapper接口添加 @Mapper 注解）。
解决：添加 @MapperScan 注解并配置正确的包路径，或在Mapper接口添加 @Mapper 注解。
问题3：数据库连接失败（Communications link failure）
原因：数据源配置错误，常见场景：
MySQL服务未启动；
URL中的IP、端口、数据库名错误；
用户名、密码错误；
MySQL8.0+ 未配置 serverTimezone 参数，或驱动类使用了旧版本（com.mysql.jdbc.Driver）。
解决：检查MySQL服务状态，修正URL、用户名、密码，使用正确的驱动类和URL参数。
六、总结
Spring 集成 MyBatis 有两种核心方式：
传统XML配置方式（适用于SSM项目，灵活可控）和Spring Boot Java配置方式（适用于Spring Boot项目，简化配置，推荐）。
两种方式的核心逻辑一致，都是通过 MyBatis-Spring 组件将 MyBatis 核心对象交给 Spring 管理，实现依赖注入和SQL操作的简化。
开发时需重点关注 Mapper 接口与 Mapper.xml 的关联、数据源配置、Mapper 扫描路径这三个核心点，避免出现常见错误。
集成完成后，即可通过 Spring 依赖注入 Mapper 接口，灵活编写SQL，大幅提升持久层开发效率和项目可维护性。

