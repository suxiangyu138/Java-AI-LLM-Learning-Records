03.31 13:19
Maven+Java后端企业级开发流程实战（用户管理模块）
一、实战目标
1. 熟练掌握Maven核心操作（项目初始化、依赖管理、打包、部署）；
2. 体验企业级Java后端开发完整流程（需求→架构设计→编码→测试→打包→部署）；
3. 整合SpringBoot、MyBatis-Plus、MySQL等常用企业级技术，巩固Maven依赖引入与版本控制。
说明：本项目模拟企业级“用户管理模块”，实现用户新增、查询、修改、删除（CRUD）核心功能，全程围绕Maven操作展开，所有步骤可直接上手实操。
二、环境准备（企业级标准配置）
2.1 基础环境
JDK：1.8（企业级后端主流版本，Maven对JDK版本有明确兼容要求，1.8最稳定）；
Maven：3.6.3（避免过高版本兼容问题，与SpringBoot、IDEA适配性最佳）；
开发工具：IntelliJ IDEA（企业级主流IDE，内置Maven插件，操作便捷）；
数据库：MySQL 8.0（主流关系型数据库，与MyBatis-Plus完美适配）；
工具：Navicat（数据库可视化工具，用于创建数据库、表）。
2.2 Maven环境配置（关键步骤）
核心：配置Maven本地仓库、阿里云镜像（解决中央仓库下载依赖慢的问题，企业开发必配）。
找到Maven安装目录下的 conf/settings.xml 文件；
配置本地仓库（自定义路径，避免C盘占用）： <localRepository>D:\maven\localRepository</localRepository>
配置阿里云镜像（替换默认中央仓库）：在 <mirrors> 标签内添加： <mirror> <id>aliyunmaven</id> <mirrorOf>central</mirrorOf> <name>阿里云公共仓库</name> <url>https://maven.aliyun.com/repository/public</url> </mirror>
IDEA中配置Maven：打开IDEA → File → Settings → Build, Execution, Deployment → Build Tools → Maven，选择本地Maven路径、settings.xml路径和本地仓库路径，点击Apply保存。
三、企业级开发流程实战（核心步骤）
步骤1：需求分析（企业开发第一步）
用户管理模块核心需求（简化企业实际需求，聚焦Maven操作）：
新增用户：接收用户名、密码、手机号，插入数据库；
查询用户：根据用户ID查询单个用户，查询所有用户列表；
修改用户：根据用户ID修改用户名、手机号；
删除用户：根据用户ID删除用户；
异常处理：用户ID不存在、手机号重复等基础异常。
步骤2：架构设计（企业级分层架构）
采用Java后端主流分层架构（Maven项目目录规范），目录结构如下（后续通过Maven自动生成+手动补充）：
com.example.usermanage
├── controller  // 控制层：接收前端请求，返回响应（对外接口）
├── service     // 业务层：处理核心业务逻辑
│   └── impl    // 业务层实现类
├── mapper      // 持久层：与数据库交互（MyBatis-Plus接口）
├── entity      // 实体层：对应数据库表，封装数据
├── exception   // 异常层：自定义异常、全局异常处理
└── Application // 启动类：SpringBoot项目入口
Maven核心作用：通过依赖管理，引入SpringBoot、MyBatis-Plus等框架，无需手动导入jar包，统一管理版本，避免jar包冲突（企业开发核心痛点）。
步骤3：Maven项目初始化（两种方式，企业常用）
方式1：IDEA创建Maven项目（推荐，便捷高效）；方式2：命令行创建（巩固Maven命令），两种方式选一种即可。
3.1 方式1：IDEA创建Maven项目（SpringBoot骨架，企业主流）
打开IDEA → New Project → 选择Spring Initializr（SpringBoot官方骨架，内置Maven支持）；
填写项目信息（企业级规范）：
Group：com.example（企业域名倒写，标识项目所属组织）；
Artifact：user-manage（项目名称，小写+连字符）；
Version：1.0.0（版本号，企业规范：主版本.次版本.修订版本）；
Package：com.example.usermanage（包名，与Group+Artifact对应）；
Java Version：8（与本地JDK一致）。
选择依赖（核心，Maven自动管理依赖）：
Spring Web：提供Web支持（接收HTTP请求）；
MyBatis-Plus Generator：代码生成器（自动生成entity、mapper、service，提高开发效率）；
MySQL Driver：MySQL数据库驱动（连接数据库）；
Lombok：简化实体类开发（无需写getter/setter）。
点击Finish，IDEA自动生成Maven项目，核心文件：
pom.xml：Maven核心配置文件（依赖管理、打包配置等）；
Application.java：SpringBoot启动类；
src/main/resources：资源目录（配置文件、静态资源）；
src/test：测试目录（单元测试，企业开发必写）。
3.2 方式2：命令行创建Maven项目（巩固Maven命令）
打开CMD，输入以下命令（对应上面的项目信息），执行后生成基础Maven项目，再导入IDEA：
mvn archetype:generate -DgroupId=com.example -DartifactId=user-manage -Dversion=1.0.0 -Dpackage=com.example.usermanage -DarchetypeArtifactId=maven-archetype-quickstart -DinteractiveMode=false
说明：命令执行后，需手动在pom.xml中添加SpringBoot、MyBatis-Plus等依赖（后续步骤统一补充）。
步骤4：Maven依赖管理（核心，重点巩固）
打开项目中的pom.xml文件，这是Maven项目的核心，所有依赖、打包、插件配置都在这里，企业开发中需重点关注依赖版本兼容、冲突解决。
4.1 核心依赖配置（复制到pom.xml的<dependencies>标签内）
<!-- SpringBoot Web依赖：提供Web支持 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<!-- MySQL驱动依赖：连接MySQL数据库 -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>
<!-- MyBatis-Plus依赖：简化持久层开发 -->
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-boot-starter</artifactId>
    <version>3.5.3.1</version>
</dependency>
<!-- MyBatis-Plus代码生成器依赖 -->
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-generator</artifactId>
    <version>3.5.3.1</version>
</dependency>
<!-- Lombok依赖：简化实体类 -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>
<!-- 单元测试依赖：企业开发必写测试用例 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
4.2 Maven依赖核心知识点（巩固重点）
groupId：组织标识，通常是企业域名倒写（如com.alibaba、com.example），用于区分不同组织的项目；
artifactId：项目标识，即项目名称，用于区分同一组织下的不同项目；
version：版本号，格式为“主版本.次版本.修订版本”，企业中通过版本控制迭代项目（如1.0.0→1.0.1）；
scope：依赖范围（核心），决定依赖在哪个阶段生效：
compile（默认）：编译、测试、运行阶段都生效（核心依赖，如SpringBoot Web）；
runtime：仅运行、测试阶段生效（如MySQL驱动，编译时无需依赖）；
test：仅测试阶段生效（如单元测试依赖）；
provided：编译、测试阶段生效，运行阶段由容器提供（如servlet-api，Tomcat已自带）。
依赖传递：Maven会自动导入依赖的依赖（如导入mybatis-plus-boot-starter，会自动导入mybatis-plus核心包、SpringBoot相关依赖），无需手动导入；
冲突解决：当多个依赖引入同一个jar包的不同版本时，Maven默认“就近原则”（距离当前pom.xml最近的版本生效），也可手动指定版本（通过<version>标签）。
4.3 插件配置（打包、编译用，企业必备）
在pom.xml中添加<build>标签，配置Maven插件（用于编译、打包成jar包，部署到服务器）：
<build>
    <plugins>
        <!-- SpringBoot打包插件：将项目打包成可运行的jar包 -->
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
            <configuration>
                <excludes>
                    <exclude>
                        <groupId>org.projectlombok</groupId>
                        <artifactId>lombok</artifactId>
                    </exclude>
                </excludes>
            </configuration>
        </plugin>
        <!-- 编译插件：指定JDK版本为1.8，避免编译报错 -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.8.1</version>
            <configuration>
                <source>8</source>
                <target>8</target>
                <encoding>UTF-8</encoding>
            </configuration>
        </plugin>
    </plugins>
</build>
步骤5：项目编码（结合Maven依赖，企业级开发）
基于Maven导入的依赖，按照分层架构编写代码，重点体验“依赖引入后直接使用框架功能”，无需手动导入jar包。
5.1 数据库准备
打开Navicat，创建数据库：user_manage_db（编码：utf8mb4）；
创建用户表（t_user），SQL语句如下： CREATE TABLE `t_user` ( `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '用户ID（主键）', `username` varchar(50) NOT NULL COMMENT '用户名', `password` varchar(100) NOT NULL COMMENT '密码（加密存储）', `phone` varchar(11) NOT NULL COMMENT '手机号', `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间', `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间', PRIMARY KEY (`id`), UNIQUE KEY `uk_phone` (`phone`) COMMENT '手机号唯一' ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';
5.2 配置文件（src/main/resources/application.yml）
配置数据库连接、MyBatis-Plus，依赖Maven导入的MySQL驱动、MyBatis-Plus依赖，直接生效：
spring:
  # 数据库配置
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/user_manage_db?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root  # 你的MySQL用户名
    password: 123456  # 你的MySQL密码
# MyBatis-Plus配置
mybatis-plus:
  mapper-locations: classpath:mapper/**/*.xml  # mapper.xml文件路径
  type-aliases-package: com.example.usermanage.entity  # 实体类包路径
  configuration:
    map-underscore-to-camel-case: true  # 下划线转驼峰（数据库字段_转实体类驼峰命名）
5.3 代码生成（使用MyBatis-Plus Generator，企业高效开发）
基于Maven导入的代码生成器依赖，编写生成器类，自动生成entity、mapper、service，减少重复编码：
在com.example.usermanage下创建generator包，新建Generator.java类；
编写生成器代码（复制即可，修改数据库配置、包路径）： package com.example.usermanage.generator; import com.baomidou.mybatisplus.generator.FastAutoGenerator; import com.baomidou.mybatisplus.generator.config.OutputFile; import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine; import java.util.Collections; public class Generator { public static void main(String[] args) { FastAutoGenerator.create("jdbc:mysql://localhost:3306/user_manage_db?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai", "root", "123456") // 全局配置 .globalConfig(builder -> { builder.author("maven-demo") // 作者 .outputDir(System.getProperty("user.dir") + "/src/main/java") // 输出路径（项目src/main/java） .disableOpenDir() // 生成后不打开文件夹 .commentDate("yyyy-MM-dd"); // 注释日期格式 }) // 包配置 .packageConfig(builder -> { builder.parent("com.example.usermanage") // 父包名 .moduleName("") // 模块名（无则留空） .entity("entity") // 实体类包名 .mapper("mapper") // mapper接口包名 .service("service") // service接口包名 .serviceImpl("service.impl") // service实现类包名 .controller("controller") // 控制层包名 .xml("mapper") // mapper.xml文件包名（src/main/resources/mapper） .pathInfo(Collections.singletonMap(OutputFile.xml, System.getProperty("user.dir") + "/src/main/resources/mapper")); }) // 策略配置（指定生成的表） .strategyConfig(builder -> { builder.addInclude("t_user") // 生成t_user表对应的代码 .entityBuilder() .enableLombok() // 启用Lombok，实体类自动生成getter/setter .enableTableFieldAnnotation() // 给实体类字段添加注释 .controllerBuilder() .enableRestStyle(); // 控制层启用REST风格（@RestController） }) .templateEngine(new FreemarkerTemplateEngine()) // 使用Freemarker模板引擎 .execute(); // 执行生成 } }
运行Generator.java，自动生成entity、mapper、service、controller的基础代码，无需手动编写。
5.4 完善核心代码（补充业务逻辑）
基于生成的代码，补充CRUD业务逻辑，体验Maven依赖带来的便捷（直接使用SpringBoot、MyBatis-Plus的API）。
（1）Service层完善（UserService.java）
package com.example.usermanage.service;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.usermanage.entity.TUser;
public interface UserService extends IService<TUser> {
    // 新增用户（自定义方法，补充业务逻辑）
    boolean addUser(TUser user);
    // 根据ID查询用户
    TUser getUserById(Long id);
    // 修改用户
    boolean updateUser(TUser user);
    // 删除用户
    boolean deleteUser(Long id);
}
（2）Service实现类完善（UserServiceImpl.java）
package com.example.usermanage.service.impl;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.usermanage.entity.TUser;
import com.example.usermanage.mapper.TUserMapper;
import com.example.usermanage.service.UserService;
import org.springframework.stereotype.Service;
@Service
public class UserServiceImpl extends ServiceImpl<TUserMapper, TUser> implements UserService {
    @Override
    public boolean addUser(TUser user) {
        // 简单业务逻辑：判断手机号是否已存在
        TUser existUser = lambdaQuery().eq(TUser::getPhone, user.getPhone()).one();
        if (existUser != null) {
            throw new RuntimeException("手机号已存在");
        }
        // 调用MyBatis-Plus自带的save方法（依赖MyBatis-Plus依赖）
        return save(user);
    }
    @Override
    public TUser getUserById(Long id) {
        // 调用MyBatis-Plus自带的getById方法
        return getById(id);
    }
    @Override
    public boolean updateUser(TUser user) {
        // 调用MyBatis-Plus自带的updateById方法
        return updateById(user);
    }
    @Override
    public boolean deleteUser(Long id) {
        // 调用MyBatis-Plus自带的removeById方法
        return removeById(id);
    }
}
（3）Controller层完善（UserController.java）
package com.example.usermanage.controller;
import com.example.usermanage.entity.TUser;
import com.example.usermanage.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
// REST风格控制层，依赖Spring Web依赖
@RestController
@RequestMapping("/user")
public class UserController {
    @Autowired
    private UserService userService;
    // 新增用户：POST请求
    @PostMapping("/add")
    public String addUser(@RequestBody TUser user) {
        try {
            boolean success = userService.addUser(user);
            return success ? "新增用户成功" : "新增用户失败";
        } catch (RuntimeException e) {
            return e.getMessage();
        }
    }
    // 根据ID查询用户：GET请求
    @GetMapping("/{id}")
    public TUser getUserById(@PathVariable Long id) {
        return userService.getUserById(id);
    }
    // 查询所有用户：GET请求
    @GetMapping("/list")
    public List<TUser> getAllUser() {
        return userService.list();
    }
    // 修改用户：PUT请求
    @PutMapping("/update")
    public String updateUser(@RequestBody TUser user) {
        boolean success = userService.updateUser(user);
        return success ? "修改用户成功" : "修改用户失败";
    }
    // 删除用户：DELETE请求
    @DeleteMapping("/{id}")
    public String deleteUser(@PathVariable Long id) {
        boolean success = userService.deleteUser(id);
        return success ? "删除用户成功" : "删除用户失败";
    }
}
（4）启动类添加注解（Application.java）
package com.example.usermanage;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
// 扫描mapper接口（MyBatis-Plus依赖提供的注解）
@MapperScan("com.example.usermanage.mapper")
@SpringBootApplication
public class UserManageApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserManageApplication.class, args);
    }
}
步骤6：Maven常用操作（企业开发高频）
通过IDEA的Maven插件或命令行，执行Maven操作，巩固Maven核心用法，所有操作均围绕pom.xml配置展开。
6.1 依赖更新（解决依赖缺失、版本冲突）
场景：新增依赖后，IDEA未自动导入，或依赖报错。
IDEA操作：点击右侧Maven → 找到项目 → 点击刷新按钮（Reload Project）；
命令行操作：进入项目根目录（pom.xml所在目录），输入命令：mvn clean install -U（-U表示强制更新依赖）。
6.2 编译项目（检查代码语法错误）
场景：编码完成后，编译项目，生成class文件。
IDEA操作：右侧Maven → Lifecycle → compile，双击执行；
命令行操作：mvn compile，编译后的class文件在target/classes目录下。
6.3 单元测试（企业开发必做，验证业务逻辑）
基于Maven导入的test依赖，编写单元测试，验证Service层逻辑。
在src/test/java/com/example/usermanage/service下，创建UserServiceTest.java；
编写测试代码： package com.example.usermanage.service; import com.example.usermanage.entity.TUser; import org.junit.jupiter.api.Test; import org.springframework.beans.factory.annotation.Autowired; import org.springframework.boot.test.context.SpringBootTest; import static org.junit.jupiter.api.Assertions.*; @SpringBootTest // SpringBoot测试注解，依赖test依赖 class UserServiceTest { @Autowired private UserService userService; // 测试新增用户 @Test void addUser() { TUser user = new TUser(); user.setUsername("maven_test"); user.setPassword("123456"); user.setPhone("13800138000"); boolean success = userService.addUser(user); assertTrue(success); // 断言：新增成功则测试通过 } // 测试查询用户 @Test void getUserById() { TUser user = userService.getUserById(1L); assertNotNull(user); // 断言：查询到用户则测试通过 } }
执行测试：
IDEA操作：右键点击测试类 → Run UserServiceTest；
命令行操作：mvn test，自动执行所有测试用例。
6.4 打包项目（部署到服务器，企业最终步骤）
将项目打包成可运行的jar包，依赖pom.xml中的SpringBoot打包插件。
IDEA操作：右侧Maven → Lifecycle → package，双击执行；
命令行操作：mvn clean package（clean：清理之前的编译、打包文件，避免冲突）；
打包结果：打包后的jar包在项目target目录下，名称为user-manage-1.0.0.jar（与pom.xml中的artifactId和version对应）。
6.5 部署运行（模拟企业服务器部署）
将target目录下的jar包复制到任意目录（如D:\deploy）；
打开CMD，进入jar包所在目录，输入命令运行： java -jar user-manage-1.0.0.jar
运行成功后，访问接口测试（使用Postman或浏览器）：
新增用户：POST http://localhost:8080/user/add，请求体：{"username":"test","password":"123456","phone":"13900139000"}；
查询用户：GET http://localhost:8080/user/1；
查询所有用户：GET http://localhost:8080/user/list。
步骤7：Maven高级操作（企业拓展）
7.1 依赖排除（解决冲突）
场景：当引入的依赖包含不需要的jar包（如引入A依赖，A依赖自动导入B依赖，但B依赖与其他依赖冲突），可通过<exclusions>排除：
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-boot-starter</artifactId>
    <version>3.5.3.1</version>
    <exclusions>
        <exclusion>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-jdbc</artifactId>
        </exclusion>
    </exclusions>
</dependency>
7.2 自定义Maven属性（统一版本管理）
场景：项目中多个依赖使用相同版本，可通过<properties>定义属性，统一修改，减少冗余：
<properties>
    <java.version>8</java.version>
    <mybatis-plus.version>3.5.3.1</mybatis-plus.version>
</properties>
<!-- 使用属性引用版本 -->
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-boot-starter</artifactId>
    <version>${mybatis-plus.version}</version>
</dependency>
四、实战总结（Maven核心巩固）
本实战项目模拟企业级Java后端开发完整流程，全程围绕Maven核心功能展开，重点掌握以下内容（企业开发必备）：
Maven核心作用：依赖管理（自动导入、版本控制、冲突解决）、项目构建（编译、测试、打包、部署）；
核心配置文件：pom.xml（依赖配置、插件配置、属性配置）；
高频操作：依赖更新、编译、测试、打包（命令行+IDEA两种方式）；
企业痛点解决：通过Maven避免jar包手动导入、版本冲突，提高开发效率；
拓展：依赖排除、属性统一管理，适配企业复杂项目需求。
后续可自行拓展功能（如密码加密、分页查询），继续巩固Maven依赖引入（如引入分页插件、加密工具依赖），深入体验企业级开发流程。

