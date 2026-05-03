03.21 13:36
Spring整合MyBatis详解（无缝衔接前文MyBatis基础）
一、整合概述
1.1 整合意义
前文我们已经掌握了MyBatis的核心用法（CRUD、关联映射、动态SQL、缓存），但MyBatis存在明显局限：需要手动创建SqlSessionFactory、SqlSession，事务管理需手动控制（openSession(true)），且无法利用Spring的IOC（依赖注入）、AOP（事务切面）等核心特性。
Spring整合MyBatis的核心目的，是让Spring接管MyBatis的核心组件（SqlSessionFactory、SqlSession、Mapper接口），通过Spring IOC实现组件的自动注入，通过Spring AOP实现声明式事务管理，简化开发，提升项目的可维护性和扩展性。
整合后优势：无需手动创建SqlSession，Mapper接口可直接注入使用；事务管理由Spring统一控制，无需手动提交/回滚；结合Spring的依赖注入，彻底解耦组件依赖。
1.2 整合核心思路
Spring整合MyBatis的核心是“将MyBatis的核心组件交给Spring管理”，核心步骤分为3步：
导入整合所需依赖（Spring核心依赖、MyBatis依赖、Spring-MyBatis整合依赖）；
配置Spring核心配置文件（applicationContext.xml），核心是配置数据源、SqlSessionFactory、Mapper扫描器；
编写Mapper接口、实体类，通过Spring注入Mapper接口，编写测试类验证整合效果。
说明：本文延续前文项目结构（com.example.pojo、com.example.mapper），复用User、Dept、Emp等实体类，确保与前文MyBatis基础内容无缝衔接，无需额外创建新实体。
二、整合环境准备（关键步骤）
2.1 导入依赖（pom.xml）
整合需要4类依赖：Spring核心依赖、MyBatis依赖、Spring-MyBatis整合依赖、数据库驱动依赖，直接复制到pom.xml即可，无需修改前文实体类和Mapper接口。
<!-- 1. Spring核心依赖 -->
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-context</artifactId>
    <version>5.3.20</version> <!-- 稳定版本，适配MyBatis -->
</dependency>
<!-- Spring JDBC依赖（用于事务管理和数据源配置） -->
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-jdbc</artifactId>
    <version>5.3.20</version>
</dependency>
<!-- Spring AOP依赖（用于声明式事务） -->
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-aop</artifactId>
    <version>5.3.20</version>
</dependency>
<dependency>
    <groupId>org.aspectj</groupId>
    <artifactId>aspectjweaver</artifactId>
    <version>1.9.9.1</version>
</dependency>
<!-- 2. MyBatis核心依赖（与前文一致） -->
<dependency>
    <groupId>org.mybatis</groupId>
    <artifactId>mybatis</artifactId>
    <version>3.5.9</version>
</dependency>
<!-- 3. Spring-MyBatis整合依赖（核心整合包） -->
<dependency>
    <groupId>org.mybatis</groupId>
    <artifactId>mybatis-spring</artifactId>
    <version>2.0.7</version> <!-- 适配MyBatis 3.5.x和Spring 5.x -->
</dependency>
<!-- 4. 数据库驱动（MySQL 8.0+） -->
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <version>8.0.30</version>
</dependency>
<!-- 可选：连接池依赖（替代MyBatis默认连接池，提升性能） -->
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>druid</artifactId>
    <version>1.2.16</version>
</dependency>
<!-- 测试依赖 -->
<dependency>
    <groupId>junit</groupId>
    <artifactId>junit</artifactId>
    <version>4.13.2</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-test</artifactId>
    <version>5.3.20</version>
    <scope>test</scope>
</dependency>
2.2 项目结构（延续前文，无新增）
保持与前文一致的项目结构，无需新增包或类，核心结构如下：
com.example
├── pojo          // 实体类（User、Dept、Emp、UserDetail等，与前文一致）
├── mapper        // Mapper接口（UserMapper、DeptMapper等，与前文一致）
├── service       // 新增：业务层接口及实现（可选，规范开发）
│   ├── impl      // 业务层实现类
└── config        // 配置文件目录（Spring配置、MyBatis配置）
src/main/resources
├── applicationContext.xml  // Spring核心配置文件（整合核心）
├── mybatis-config.xml      // MyBatis核心配置（可选，简化配置）
└── db.properties           // 数据库连接信息（抽取配置，便于维护）
三、核心配置（applicationContext.xml）
Spring整合MyBatis的核心配置都在applicationContext.xml中，主要包括4个部分：加载数据库配置、配置数据源、配置SqlSessionFactory、配置Mapper扫描器，无需再手动编写MyBatis的SqlSession相关代码。
3.1 步骤1：加载数据库配置文件（db.properties）
先创建db.properties文件，抽取数据库连接信息，避免硬编码，便于后续修改：
# db.properties 数据库连接配置
jdbc.driver=com.mysql.cj.jdbc.Driver
jdbc.url=jdbc:mysql://localhost:3306/mybatis_db?serverTimezone=UTC&useSSL=false
jdbc.username=root
jdbc.password=123456
# 可选：连接池配置（Druid）
druid.initialSize=5
druid.maxActive=20
druid.minIdle=5
在applicationContext.xml中加载该配置文件：
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:context="http://www.springframework.org/schema/context"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
                           http://www.springframework.org/schema/beans/spring-beans.xsd
                           http://www.springframework.org/schema/context
                           http://www.springframework.org/schema/context/spring-context.xsd">
    <!-- 1. 加载db.properties配置文件 -->
   <context:property-placeholder location="classpath:db.properties"/>
</beans>
3.2 步骤2：配置数据源（DataSource）
数据源是连接数据库的核心，Spring提供了内置数据源，也可使用第三方连接池（如Druid），推荐使用Druid（性能更优），两种方式二选一即可。
方式1：Spring内置数据源（简单，无需额外依赖）
<!-- 配置Spring内置数据源（DriverManagerDataSource） -->
<bean id="dataSource" class="org.springframework.jdbc.datasource.DriverManagerDataSource">
    <property name="driverClassName" value="${jdbc.driver}"/>
    <property name="url" value="${jdbc.url}"/>
    <property name="username" value="${jdbc.username}"/>
<property name="password" value="${jdbc.password}"/>
</bean>
方式2：Druid连接池（推荐，性能更优）
<!-- 配置Druid连接池（需导入Druid依赖） -->
<bean id="dataSource" class="com.alibaba.druid.pool.DruidDataSource">
    <property name="driverClassName" value="${jdbc.driver}"/>
    <property name="url" value="${jdbc.url}"/>
    <property name="username" value="${jdbc.username}"/>
    <property name="password" value="${jdbc.password}"/>
    <!-- 连接池补充配置（可选） -->
<property name="initialSize" value="${druid.initialSize}"/>
<property name="maxActive" value="${druid.maxActive}"/>
    <property name="minIdle" value="${druid.minIdle}"/>
</bean>
3.3 步骤3：配置SqlSessionFactory（核心）
SqlSessionFactory是MyBatis的核心，整合后由Spring管理，无需手动创建，通过MyBatis-Spring提供的SqlSessionFactoryBean实现配置，核心依赖数据源和MyBatis配置。
<!-- 3. 配置SqlSessionFactory，由Spring管理 -->
<bean id="sqlSessionFactory" class="org.mybatis.spring.SqlSessionFactoryBean">
    <!-- 注入数据源（关联上文配置的dataSource） -->
    <property name="dataSource" ref="dataSource"/>
    <!-- 可选：配置MyBatis核心配置文件（mybatis-config.xml） -->
    <property name="configLocation" value="classpath:mybatis-config.xml"/>
    <!-- 可选：配置实体类别名（替代mybatis-config.xml中的typeAliases） -->
    <property name="typeAliasesPackage" value="com.example.pojo"/>
    <!-- 可选：配置Mapper.xml文件路径（若使用XML配置Mapper） -->
    <property name="mapperLocations" value="classpath:com/example/mapper/*.xml"/>
</bean>
关键说明：
configLocation：指定MyBatis核心配置文件（mybatis-config.xml），若MyBatis配置简单（如仅需别名），可省略该配置，直接在SqlSessionFactoryBean中配置；
typeAliasesPackage：自动给com.example.pojo包下的实体类起别名（如User类别名为user），与前文MyBatis的typeAliases配置一致；
mapperLocations：若使用XML配置Mapper（如UserMapper.xml），需指定XML文件路径，Spring会自动扫描加载；若使用注解配置Mapper（@Select等），可省略该配置。
3.4 步骤4：配置Mapper扫描器（核心，无需手动注入Mapper）
通过Mapper扫描器，Spring会自动扫描指定包下的Mapper接口，生成代理对象，并存入IOC容器，后续可直接通过@Autowired注入Mapper接口，无需手动创建SqlSession和Mapper代理对象。
<!-- 4. 配置Mapper扫描器，扫描com.example.mapper包下的所有Mapper接口 -->
<bean class="org.mybatis.spring.mapper.MapperScannerConfigurer"><!-- 指定要扫描的Mapper接口包 -->
    <property name="basePackage" value="com.example.mapper"/>
    <!-- 关联SqlSessionFactory（可省略，Spring会自动匹配） -->
    <property name="sqlSessionFactoryBeanName" value="sqlSessionFactory"/>
</bean>
<!-- 可选：开启Spring注解扫描（用于Service层、Controller层的@Autowired、@Service等注解） -->
<context:component-scan base-package="com.example.service"/>
3.5 可选：MyBatis核心配置（mybatis-config.xml）
若需配置MyBatis的全局属性（如缓存、日志），可创建mybatis-config.xml，简化applicationContext.xml的配置，内容如下（与前文MyBatis缓存配置一致）：
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE configuration
        PUBLIC "-//mybatis.org//DTD Config 3.0//EN"
        "https://mybatis.org/dtd/mybatis-3-config.dtd">
<configuration>
    <!-- 开启全局二级缓存（与前文一致） -->
<settings>
        <setting name="cacheEnabled" value="true"/>
    </settings>
    <!-- 实体类别名（可省略，若在applicationContext.xml中配置了typeAliasesPackage） -->
    <typeAliases>
        <package name="com.example.pojo"/>
    </typeAliases>
</configuration>
四、整合声明式事务管理（Spring AOP）
前文MyBatis的事务需手动控制（sqlSession.commit()），整合Spring后，通过Spring AOP实现声明式事务，无需手动处理事务，只需通过注解或XML配置，即可实现事务的自动提交、回滚。
4.1 配置事务管理器
在applicationContext.xml中添加事务管理器，关联数据源，用于管理事务：
<!-- 配置事务管理器（DataSourceTransactionManager） -->
<bean id="transactionManager" class="org.springframework.jdbc.datasource.DataSourceTransactionManager">
    <!-- 关联数据源，与上文配置的dataSource一致 -->
    <property name="dataSource" ref="dataSource"/>
</bean>
4.2 开启声明式事务（两种方式）
方式1：XML配置（传统方式）
<!-- 开启事务注解驱动（需导入AOP依赖） -->
<tx:annotation-driven transaction-manager="transactionManager"/>
<!-- 配置事务切面（可选，自定义事务规则） -->
<tx:advice id="txAdvice" transaction-manager="transactionManager">
    <tx:attributes>
        <!-- 所有以find开头的查询方法，只读事务 -->
        <tx:method name="find*" read-only="true"/>
        <!-- 所有以add、update、delete开头的方法，需要事务 -->
        <tx:method name="add*" propagation="REQUIRED"/>
        <tx:method name="update*" propagation="REQUIRED"/>
        <tx:method name="delete*" propagation="REQUIRED"/>
        <!-- 其他方法，默认事务规则 -->
        <tx:method name="*" propagation="REQUIRED"/>
    </tx:attributes>
</tx:advice>
<!-- AOP切面配置，将事务 advice 织入到Service层 -->
<aop:config>
    <aop:pointcut id="txPointcut" expression="execution(* com.example.service.impl.*.*(..))"/>
    <aop:advisor advice-ref="txAdvice" pointcut-ref="txPointcut"/>
</aop:config>
方式2：注解配置（推荐，简洁）
只需开启事务注解驱动，然后在Service层方法上添加@Transactional注解，即可实现事务管理：
<!-- 1. 开启事务注解驱动（applicationContext.xml中添加） --><tx:annotation-driven transaction-manager="transactionManager"/>
在Service层实现类或方法上添加@Transactional注解：
package com.example.service.impl;
import com.example.mapper.UserMapper;
import com.example.pojo.User;
import com.example.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service // 交给Spring管理
public class UserServiceImpl implements UserService {
    // 自动注入Mapper接口（Spring扫描Mapper后存入IOC容器）
    @Autowired
    private UserMapper userMapper;
    // 声明式事务：查询方法设为只读，提升性能
    @Transactional(readOnly = true)
    @Override
    public User findById(Integer id) {
        return userMapper.findById(id);
    }
    // 声明式事务：新增方法，默认开启事务，异常自动回滚
    @Transactional
    @Override
    public int addUser(User user) {
        return userMapper.addUser(user);
    }
}
五、实战测试（验证整合效果）
结合前文的UserMapper接口，编写Service层和测试类，验证Spring整合MyBatis的效果（Mapper注入、事务管理）。
5.1 步骤1：编写Service接口及实现类
// UserService接口（com.example.service）
package com.example.service;
import com.example.pojo.User;
import java.util.List;
public interface UserService {
    User findById(Integer id);
    int addUser(User user);
    List<User> findAll();
}
// UserServiceImpl实现类（com.example.service.impl）
package com.example.service.impl;
import com.example.mapper.UserMapper;
import com.example.pojo.User;
import com.example.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
@Service
public class UserServiceImpl implements UserService {
    @Autowired // 自动注入UserMapper（Spring扫描后存入IOC）
    private UserMapper userMapper;
    @Override
    @Transactional(readOnly = true)
    public User findById(Integer id) {
        return userMapper.findById(id);
    }
    @Override
    @Transactional
    public int addUser(User user) {
        return userMapper.addUser(user);
    }
    @Override
    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userMapper.findAll();
    }
}
5.2 步骤2：编写测试类
使用Spring Test注解，无需手动加载Spring配置文件，直接注入Service接口，测试整合效果：
package com.example.test;
import com.example.pojo.User;
import com.example.service.UserService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import java.util.List;
// 让测试类交给Spring管理
@RunWith(SpringJUnit4ClassRunner.class)
// 加载Spring核心配置文件
@ContextConfiguration(locations = "classpath:applicationContext.xml")
public class SpringMyBatisTest {
    // 注入Service接口（Spring自动创建实现类代理对象）
    @Autowired
    private UserService userService;
    // 测试查询（验证Mapper注入和缓存）
    @Test
    public void testFindById() {
        User user = userService.findById(1);
        System.out.println("查询结果：" + user);
        // 再次查询，验证二级缓存（若开启，不会访问数据库）
        User user2 = userService.findById(1);
        System.out.println("再次查询结果：" + user2);
    }
    // 测试新增（验证事务管理）
    @Test
    public void testAddUser() {
        User user = new User("springmybatis", "123456", 26, "spring@163.com");
        int rows = userService.addUser(user);
        System.out.println("新增成功，受影响行数：" + rows);
    }
    // 测试查询所有
    @Test
    public void testFindAll() {
        List<User> userList = userService.findAll();
        userList.forEach(System.out::println);
    }
}
5.3 测试结果说明
查询测试：两次查询相同id，若开启二级缓存，第二次不会访问数据库，验证缓存生效；
新增测试：若新增过程中抛出异常（如字段错误），事务会自动回滚，数据库不会新增数据，验证事务管理生效；
Mapper注入：无需手动创建SqlSession和Mapper代理，Spring自动注入UserMapper，简化开发。
六、整合常见问题与注意事项
Mapper接口无法注入（@Autowired报错）原因：未配置Mapper扫描器，或扫描包路径错误；Mapper接口未添加@Mapper注解（注解配置方式）；解决：核对MapperScannerConfigurer的basePackage路径，确保扫描com.example.mapper；注解配置的Mapper接口需添加@Mapper注解。
事务不生效（新增/修改失败后不回滚）原因：未开启事务注解驱动（tx:annotation-driven）；@Transactional注解添加在接口上（需添加在实现类方法上）；事务管理器未关联数据源；解决：检查applicationContext.xml中的事务配置，确保@Transactional添加在Service实现类的方法上。
二级缓存不生效（整合后缓存失效）原因：未开启全局缓存（cacheEnabled=true）；实体类未实现Serializable接口；Mapper未配置缓存注解；解决：核对MyBatis缓存配置，确保实体类实现Serializable接口，Mapper接口添加@CacheNamespace注解。
数据源配置错误（连接数据库失败）原因：db.properties中的url、用户名、密码错误；驱动类名错误（MySQL 8.0+驱动为com.mysql.cj.jdbc.Driver）；解决：核对数据库连接信息，确保驱动类与MySQL版本匹配，url添加serverTimezone=UTC参数。
XML配置与注解配置冲突原因：同一Mapper接口同时使用XML配置（XXXMapper.xml）和注解配置（@Select）；解决：同一方法只能使用一种配置方式，推荐XML配置复杂SQL，注解配置简单SQL。
七、整合总结
Spring整合MyBatis的核心是“Spring接管MyBatis的核心组件”，关键记住3个核心配置和1个事务配置：
配置数据源：交给Spring管理，推荐使用Druid连接池；
配置SqlSessionFactory：关联数据源和MyBatis配置，由Spring创建和管理；
配置Mapper扫描器：自动扫描Mapper接口，生成代理对象存入IOC容器；
配置声明式事务：通过Spring AOP实现，无需手动控制事务提交/回滚。
整合后，开发流程简化为：编写实体类 → 编写Mapper接口（XML/注解） → 编写Service层（注入Mapper） → 测试，无需关注SqlSession、事务控制等底层细节，专注业务逻辑开发。
后续可结合Spring MVC，实现“Spring MVC + Spring + MyBatis”三层架构（SSM），完成完整的Web项目开发。

