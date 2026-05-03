03.21 14:07
SpringBoot数据访问（入门实战，避坑版）
数据访问是后端开发的核心需求，SpringBoot对主流数据访问方式（JDBC、MyBatis、JPA等）提供了完善的自动配置支持，无需手动编写复杂的配置文件，就能快速实现数据库的增删改查（CRUD）操作。
本文针对新手，重点讲解最常用的JDBC和MyBatis两种数据访问方式，全程实操、步骤清晰，解决整合过程中的常见问题，让你快速上手SpringBoot数据访问。
前置准备：
1. 已搭建SpringBoot基础项目（推荐2.7.x版本）；
2. 安装数据库（本文以MySQL 8.0为例）；
3. 准备数据库和数据表（提前创建，方便后续实战）。
提前操作：创建数据库springboot_db，创建数据表user，SQL语句如下（直接执行即可）：
-- 创建数据库
CREATE DATABASE IF NOT EXISTS springboot_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- 使用数据库
USE springboot_db;
-- 创建用户表
CREATE TABLE IF NOT EXISTS `user` (
  `id` INT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
  `name` VARCHAR(50) NOT NULL COMMENT '用户姓名',
  `age` INT NOT NULL COMMENT '用户年龄',
  `address` VARCHAR(100) COMMENT '用户地址',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) COMMENT '用户表';
一、SpringBoot整合JDBC（基础入门）
JDBC是Java访问数据库的基础方式，SpringBoot通过spring-boot-starter-jdbc起步依赖，自动配置JDBC相关组件（如DataSource、JdbcTemplate），简化JDBC的使用流程，适合简单的数据访问场景。
1.1 引入依赖（核心步骤）
在pom.xml中添加JDBC起步依赖和MySQL驱动依赖（MySQL 8.0对应驱动版本8.x）：
<!-- SpringBoot JDBC起步依赖：自动配置JDBC相关组件 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-jdbc</artifactId>
</dependency>
<!-- MySQL驱动依赖（MySQL 8.0版本） -->
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <version>8.0.30</version>
    <scope>runtime</scope>
</dependency>
注意：1. MySQL 8.0驱动版本需与数据库版本匹配（8.0数据库对应驱动8.x）；2. scope设为runtime，避免编译时依赖冲突；3. 无需手动配置DataSource，SpringBoot会自动根据配置文件创建。
1.2 配置数据库连接（必配）
在application.properties（或yml）中配置数据库连接信息，核心是URL、用户名、密码，MySQL 8.0需添加时区配置，避免时区异常。
# 数据库连接配置（MySQL 8.0）
# 1. 数据库URL（serverTimezone=Asia/Shanghai 解决时区问题，useSSL=false 关闭SSL验证）
spring.datasource.url=jdbc:mysql://localhost:3306/springboot_db?serverTimezone=Asia/Shanghai&useSSL=false&characterEncoding=utf8
# 2. 数据库用户名（替换为自己的MySQL用户名，默认root）
spring.datasource.username=root
# 3. 数据库密码（替换为自己的MySQL密码）
spring.datasource.password=123456
# 4. 数据库驱动类（MySQL 8.0对应com.mysql.cj.jdbc.Driver，5.x对应com.mysql.jdbc.Driver）
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
# 可选：JDBC模板配置（优化查询性能）
spring.jdbc.template.query-timeout=3  # 查询超时时间（秒）
spring.jdbc.template.fetch-size=100  # 每次查询获取的行数
1.3 实战：使用JdbcTemplate实现CRUD
SpringBoot自动配置了JdbcTemplate，可直接注入使用，JdbcTemplate封装了JDBC的常用操作，无需手动处理连接、关闭资源，简化代码。
3.1 创建实体类（User）
package com.example.springboot.data.entity;
import java.util.Date;
// 与user表对应，字段名与表中字段一致
public class User {
    private Integer id;
    private String name;
    private Integer age;
    private String address;
    private Date createTime;
    // 无参构造（必须，否则查询结果无法封装）
    public User() {}
    // 有参构造（用于新增、修改）
    public User(String name, Integer age, String address) {
        this.name = name;
        this.age = age;
        this.address = address;
    }
    // 全参构造（可选）
    public User(Integer id, String name, Integer age, String address, Date createTime) {
        this.id = id;
        this.name = name;
        this.age = age;
        this.address = address;
        this.createTime = createTime;
    }
    // 必须提供getter和setter方法（否则无法封装数据）
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}
3.2 创建Service层（业务逻辑）
封装数据访问逻辑，注入JdbcTemplate，实现CRUD操作：
package com.example.springboot.data.service;
import com.example.springboot.data.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.List;
@Service // 标记为服务层组件，交给Spring管理
public class UserJdbcService {
    // 自动注入SpringBoot自动配置的JdbcTemplate
    @Autowired
    private JdbcTemplate jdbcTemplate;
    // 1. 新增用户
    public int addUser(User user) {
        String sql = "INSERT INTO user(name, age, address) VALUES(?, ?, ?)";
        // 占位符赋值，避免SQL注入
        return jdbcTemplate.update(sql, user.getName(), user.getAge(), user.getAddress());
    }
    // 2. 修改用户（根据ID）
    public int updateUser(User user) {
        String sql = "UPDATE user SET name=?, age=?, address=? WHERE id=?";
        return jdbcTemplate.update(sql, user.getName(), user.getAge(), user.getAddress(), user.getId());
    }
    // 3. 删除用户（根据ID）
    public int deleteUser(Integer id) {
        String sql = "DELETE FROM user WHERE id=?";
        return jdbcTemplate.update(sql, id);
    }
    // 4. 根据ID查询单个用户
    public User getUserById(Integer id) {
        String sql = "SELECT * FROM user WHERE id=?";
        // BeanPropertyRowMapper：自动将查询结果封装为User对象（字段名需一致）
        return jdbcTemplate.queryForObject(sql, new BeanPropertyRowMapper<>(User.class), id);
    }
    // 5. 查询所有用户
    public List<User> getAllUser() {
        String sql = "SELECT * FROM user";
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(User.class));
    }
}
3.3 测试（验证功能）
创建测试类，或直接在Controller中调用Service方法，验证CRUD功能（这里用SpringBoot自带的测试类演示）：
package com.example.springboot.data;
import com.example.springboot.data.entity.User;
import com.example.springboot.data.service.UserJdbcService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.List;
@SpringBootTest // 标记为SpringBoot测试类，自动加载Spring上下文
public class JdbcTest {
    @Autowired
    private UserJdbcService userService;
    // 测试新增用户
    @Test
    public void testAddUser() {
        User user = new User("张三", 20, "北京");
        int result = userService.addUser(user);
        System.out.println("新增用户影响行数：" + result); // 新增成功返回1
    }
    // 测试查询所有用户
    @Test
    public void testGetAllUser() {
        List<User> userList = userService.getAllUser();
        userList.forEach(user -> System.out.println(user.getName()));
    }
    // 测试修改、删除、根据ID查询（可自行测试）
}
二、SpringBoot整合MyBatis（实战首选）
MyBatis是一款优秀的持久层框架，支持自定义SQL、存储过程，灵活度高，是SpringBoot项目中最常用的数据访问方式。SpringBoot通过spring-boot-starter-mybatis起步依赖，实现MyBatis的自动配置，无需手动配置MyBatis核心文件（如mybatis-config.xml）。
2.1 引入依赖（核心步骤）
在pom.xml中添加MyBatis起步依赖、MySQL驱动依赖（若已引入JDBC依赖，可删除，避免冲突）：
<!-- SpringBoot MyBatis起步依赖 -->
<dependency>
    <groupId>org.mybatis.spring.boot</groupId>
    <artifactId>mybatis-spring-boot-starter</artifactId>
    <version>2.2.2</version>
</dependency>
<!-- MySQL驱动依赖（与JDBC整合时一致） -->
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <version>8.0.30</version>
    <scope>runtime</scope>
</dependency>
2.2 核心配置（必配）
在application.properties中配置数据库连接信息（与JDBC一致），同时添加MyBatis专属配置（如 mapper文件路径、实体类别名）：
# 1. 数据库连接配置（与JDBC一致）
spring.datasource.url=jdbc:mysql://localhost:3306/springboot_db?serverTimezone=Asia/Shanghai&useSSL=false&characterEncoding=utf8
spring.datasource.username=root
spring.datasource.password=123456
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
# 2. MyBatis核心配置
# 2.1 mapper.xml文件路径（指定mapper文件存放位置，必配）
mybatis.mapper-locations=classpath:mapper/*.xml
# 2.2 实体类别名配置（简化mapper.xml中的resultType，可选但推荐）
mybatis.type-aliases-package=com.example.springboot.data.entity
# 2.3 开启驼峰命名映射（解决数据库字段下划线与实体类驼峰命名不匹配问题，必配）
mybatis.configuration.map-underscore-to-camel-case=true
关键说明：1. mapper-locations：指定MyBatis的mapper.xml文件路径（本文放在src/main/resources/mapper目录下）；2. type-aliases-package：指定实体类所在包，配置后mapper.xml中可直接写实体类名（如User），无需写全类名；3. map-underscore-to-camel-case：开启后，数据库字段（如create_time）会自动映射到实体类驼峰命名字段（createTime），避免手动配置映射关系。
2.3 实战：MyBatis实现CRUD（两种方式）
MyBatis支持两种开发方式：① 注解方式（简单SQL，无需写mapper.xml）；② XML方式（复杂SQL，推荐），本文两种方式都讲解，新手可根据需求选择。
注意：实体类仍使用前面创建的User类（无需修改）。
方式1：注解方式（简单SQL）
通过MyBatis的注解（如@Select、@Insert）直接编写SQL，无需创建mapper.xml文件，适合简单的CRUD操作。
package com.example.springboot.data.mapper;
import com.example.springboot.data.entity.User;
import org.apache.ibatis.annotations.*;
import java.util.List;
@Mapper // 标记为MyBatis的Mapper接口，SpringBoot会自动扫描
public interface UserMapper {
    // 1. 新增用户（@Insert注解编写SQL）
    @Insert("INSERT INTO user(name, age, address) VALUES(#{name}, #{age}, #{address})")
    int addUser(User user);
    // 2. 修改用户（@Update注解）
    @Update("UPDATE user SET name=#{name}, age=#{age}, address=#{address} WHERE id=#{id}")
    int updateUser(User user);
    // 3. 删除用户（@Delete注解）
    @Delete("DELETE FROM user WHERE id=#{id}")
    int deleteUser(Integer id);
    // 4. 根据ID查询（@Select注解）
    @Select("SELECT * FROM user WHERE id=#{id}")
    User getUserById(Integer id);
    // 5. 查询所有（@Select注解）
    @Select("SELECT * FROM user")
    List<User> getAllUser();
}
方式2：XML方式（复杂SQL，推荐）
适合复杂SQL（如多表关联、条件查询），需创建mapper.xml文件，编写SQL语句，步骤如下：
步骤1：创建Mapper接口
package com.example.springboot.data.mapper;
import com.example.springboot.data.entity.User;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
@Mapper
public interface UserXmlMapper {
    // 方法名与mapper.xml中的id一致，参数和返回值对应
    int addUser(User user);
    int updateUser(User user);
    int deleteUser(Integer id);
    User getUserById(Integer id);
    List<User> getAllUser();
}
步骤2：创建mapper.xml文件
在src/main/resources目录下创建mapper文件夹，新建UserXmlMapper.xml文件，编写SQL语句：
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<!-- namespace：对应Mapper接口的全类名，必须一致 -->
<mapper namespace="com.example.springboot.data.mapper.UserXmlMapper">
   <!-- 新增用户 -->
    <insert id="addUser" parameterType="User">
        INSERT INTO user(name, age, address) VALUES(#{name}, #{age}, #{address})
    &lt;/insert&gt;
    <!-- 修改用户 -->
    <update id="updateUser" parameterType="User">
        UPDATE user SET name=#{name}, age=#{age}, address=#{address} WHERE id=#{id}
    &lt;/update&gt;
    <!-- 删除用户 -->
    <delete id="deleteUser" parameterType="Integer">
        DELETE FROM user WHERE id=#{id}
    </delete&gt;
    <!-- 根据ID查询 -->
    <select id="getUserById" parameterType="Integer" resultType="User">
        SELECT * FROM user WHERE id=#{id}
    &lt;/select&gt;
    <!-- 查询所有 -->
    <select id="getAllUser" resultType="User">
        SELECT * FROM user
    </select>
</mapper>
注意：1. namespace必须与Mapper接口的全类名一致；2. 标签id必须与Mapper接口的方法名一致；3. parameterType是方法参数类型（配置了别名后可写实体类名）；4. resultType是查询结果返回类型。
步骤3：创建Service层（调用Mapper）
package com.example.springboot.data.service;
import com.example.springboot.data.entity.User;
import com.example.springboot.data.mapper.UserXmlMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
@Service
public class UserMybatisService {
    // 注入Mapper接口（SpringBoot会自动创建实现类）
    @Autowired
    private UserXmlMapper userXmlMapper;
    // 调用Mapper方法，实现CRUD
    public int addUser(User user) {
        return userXmlMapper.addUser(user);
    }
    public int updateUser(User user) {
        return userXmlMapper.updateUser(user);
    }
    public int deleteUser(Integer id) {
        return userXmlMapper.deleteUser(id);
    }
    public User getUserById(Integer id) {
        return userXmlMapper.getUserById(id);
    }
    public List<User> getAllUser() {
        return userXmlMapper.getAllUser();
    }
}
步骤4：测试功能
与JDBC测试方式一致，通过SpringBoot测试类调用Service方法，验证CRUD功能，此处不再赘述。
三、数据访问常见问题及解决方案（避坑重点）
问题1：数据库连接失败，报“Access denied for user 'root'@'localhost'” 解决方案：① 检查数据库用户名、密码是否正确（与application.properties配置一致）；② 检查MySQL服务是否启动；③ 检查数据库地址、端口是否正确（默认localhost:3306）。
问题2：MyBatis查询返回null，或字段值为null 解决方案：① 检查是否开启驼峰命名映射（mybatis.configuration.map-underscore-to-camel-case=true）；② 检查实体类字段名与数据库字段名是否一致（或通过驼峰映射匹配）；③ 检查Mapper接口方法名与mapper.xml中的id是否一致；④ 检查实体类是否提供getter方法。
问题3：MyBatis提示“Could not find resource mapper/UserXmlMapper.xml” 解决方案：① 检查mybatis.mapper-locations配置是否正确（如classpath:mapper/*.xml）；② 检查mapper.xml文件是否放在指定目录下（src/main/resources/mapper）；③ 检查IDEA中mapper.xml文件是否被识别为资源文件（右键文件→Mark Directory as→Resources Root）。
问题4：SQL注入风险 解决方案：① 避免使用字符串拼接SQL（如"SELECT * FROM user WHERE name='" + name + "'"）；② 使用MyBatis的占位符#{ }（自动防SQL注入），不要使用${ }（直接拼接，有注入风险）。
问题5：时区异常，报“serverTimezone is required” 解决方案：在数据库URL中添加serverTimezone=Asia/Shanghai（MySQL 8.0必须配置）。
四、进阶补充（新手后续学习）
MyBatis分页：整合PageHelper分页插件，实现分页查询（实战必备）；
MyBatis-Plus：基于MyBatis的增强工具，无需编写SQL，自动实现CRUD，简化开发；
事务管理：使用@Transactional注解，实现数据访问的事务控制（避免新增/修改失败导致数据不一致）；
多数据源：配置多个数据库连接，实现多数据库的数据访问；
复杂查询：学习MyBatis的动态SQL（if、where、foreach等），实现多条件查询。
新手提示：数据访问的核心是“依赖+配置+CRUD”，新手先掌握MyBatis的XML方式（实战最常用），重点注意配置文件和Mapper接口、XML文件的对应关系；遇到问题优先查看控制台日志，大部分错误都是配置错误或语法不规范导致的；多动手测试，熟悉CRUD流程，再逐步学习进阶功能。

