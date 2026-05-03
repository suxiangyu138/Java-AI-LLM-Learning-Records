03.20 16:33
MyBatis 代码生成器（MBG）使用教程
MyBatis 代码生成器（MyBatis Generator，简称MBG）是MyBatis官方提供的一款自动生成工具，核心作用是根据数据库表结构，自动生成实体类（POJO）、Mapper接口、Mapper XML映射文件，无需手动编写基础CRUD代码，大幅提升开发效率，减少重复编码，同时保证代码规范统一，避免手动编写出错。
MBG支持多种使用方式（Maven插件、Java程序、命令行），其中Maven插件方式最便捷、最常用，适合IDEA+Maven开发环境。
本教程将全程采用Maven插件方式，延续前文的MySQL数据库、项目结构，保持上下文连贯，从环境搭建到代码生成、优化，完整讲解MBG的基本用法。
一、MyBatis Generator 核心作用
MBG可自动生成以下内容，覆盖基础CRUD操作，无需手动编写：
实体类（POJO）：与数据库表字段一一对应，自动生成getter、setter、toString、构造方法等；
Mapper接口：包含基础CRUD方法（selectByPrimaryKey、selectAll、insert、updateByPrimaryKey、deleteByPrimaryKey等）；
Mapper XML映射文件：包含对应Mapper接口方法的SQL语句（静态SQL，可后续扩展为动态SQL）；
可选生成：Example类（用于多条件查询，替代手动编写动态SQL的基础逻辑）。
注意：MBG生成的是基础代码，复杂SQL（如多表关联、复杂动态SQL）仍需手动优化和补充，不可完全依赖。
二、环境准备（延续前文项目）
使用MBG前，需确保基础环境已就绪，无需额外新增依赖（Maven插件方式会自动引入MBG核心依赖）：
开发环境：IDEA+Maven
数据库：MySQL（沿用前文mybatis_db数据库、user表）
项目结构：已创建com.example.pojo（实体类）、com.example.mapper（Mapper接口）、src/main/resources（配置文件）目录
核心依赖：已引入MyBatis、MySQL驱动（前文已配置，无需修改）
三、Maven插件方式配置（核心步骤）
Maven插件方式无需单独下载MBG，只需在pom.xml中配置MBG插件，编写配置文件，即可通过Maven命令生成代码，步骤清晰、操作便捷。
3.1 步骤1：在pom.xml中配置MBG插件
在pom.xml的<build>→<plugins>标签中，添加MBG插件（版本可按需调整，推荐使用最新稳定版）：
&lt;build&gt;
    &lt;plugins&gt;
        <!-- MyBatis Generator Maven插件 -->
        <plugin>
            <groupId>org.mybatis.generator</groupId>
            <artifactId>mybatis-generator-maven-plugin</artifactId>
            <version>1.4.2</version>
            &lt;configuration&gt;
                <!-- 配置文件路径（后续编写，放在src/main/resources目录下） -->
                <configurationFile>src/main/resources/generatorConfig.xml</configurationFile>
                <!-- 允许覆盖已生成的代码（开发阶段推荐开启，避免手动删除旧代码） -->
                <overwrite>true</overwrite>
                <!-- 生成日志打印（可选，便于查看生成过程） -->
                <verbose>true</verbose>
            </configuration&gt;
            &lt;dependencies&gt;
                <!-- MySQL驱动依赖（MBG需要连接数据库，读取表结构） -->
                <dependency>
                    <groupId>mysql</groupId>
                    <artifactId>mysql-connector-java</artifactId>
                    <version>8.0.30</version>
                </dependency>
               <!-- MyBatis核心依赖（与项目中MyBatis版本一致） -->
                <dependency>
                    <groupId>org.mybatis</groupId>
                    <artifactId>mybatis</artifactId>
                    <version>3.5.10</version>
                </dependency>
            </dependencies>
        </plugin>
    </plugins>
</build>
配置说明：
configurationFile：指定MBG的核心配置文件路径，后续需在该路径下创建generatorConfig.xml；
overwrite：true表示允许覆盖已生成的代码，开发阶段建议开启，避免重复生成导致文件冗余；
dependencies：MBG需要依赖MySQL驱动（连接数据库）和MyBatis核心包，版本需与项目一致。
3.2 步骤2：编写MBG核心配置文件（generatorConfig.xml）
核心配置文件用于指定数据库连接信息、生成代码的路径、需要生成的数据库表、生成规则等，是MBG生成代码的关键。在src/main/resources目录下，新建generatorConfig.xml文件，核心配置如下（可直接复制修改）：
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE generatorConfiguration
        PUBLIC "-//mybatis.org//DTD MyBatis Generator Configuration 1.0//EN"
        "https://mybatis.org/dtd/mybatis-generator-config_1_0.dtd">
<generatorConfiguration&gt;
    <!-- 1. 配置数据库连接信息（读取数据库表结构） -->
    <context id="DB2Tables" targetRuntime="MyBatis3"><!-- 关闭注释生成（可选，避免生成多余注释，保持代码简洁） -->
        <commentGenerator>
            <property name="suppressAllComments" value="true"/>
        </commentGenerator>
       <!-- 数据库连接配置（替换为自己的数据库信息） -->
        <jdbcConnection driverClass="com.mysql.cj.jdbc.Driver"
                        connectionURL="jdbc:mysql://localhost:3306/mybatis_db?serverTimezone=UTC&useUnicode=true&characterEncoding=utf8"
                        userId="root"
                        password="123456">
        </jdbcConnection&gt;
        <!-- 2. 配置Java类型解析（可选，默认即可） -->
        <javaTypeResolver>
            <property name="forceBigDecimals" value="false"/>
        </javaTypeResolver>
        <!-- 3. 配置实体类（POJO）生成规则 -->
        <javaModelGenerator targetPackage="com.example.pojo" targetProject="src/main/java"&gt;
            <!-- 是否在包名后添加子包（false表示不添加） -->
            <property name="enableSubPackages" value="false"/&gt;
            <!-- 清理字段前后的空格 -->
            <property name="trimStrings" value="true"/>
        </javaModelGenerator>
<!-- 4. 配置Mapper XML映射文件生成规则 -->
        <sqlMapGenerator targetPackage="com.example.mapper" targetProject="src/main/resources">
            <property name="enableSubPackages" value="false"/>
        &lt;/sqlMapGenerator&gt;
        <!-- 5. 配置Mapper接口生成规则 -->
        <javaClientGenerator type="XMLMAPPER" targetPackage="com.example.mapper" targetProject="src/main/java">
            <property name="enableSubPackages" value="false"/>
        </javaClientGenerator>
        <!-- 6. 配置需要生成代码的数据库表（核心） -->
        <!-- tableName：数据库表名；domainObjectName：生成的实体类名（可自定义） -->
        <table tableName="user" domainObjectName="User"&gt;
            <!-- 配置主键生成策略（MySQL自增主键） -->
            <generatedKey column="id" sqlStatement="MySQL" identity="true"/&gt;
            <!-- 可选：忽略数据库中的某些字段（不生成到实体类中） -->
            <!-- <ignoreColumn column="password"/&gt; -->
            <!-- 可选：指定数据库字段与实体类属性的映射（如字段名与属性名不一致时） -->
           <!-- <columnOverride column="user_name" property="userName"/&gt; -->
        &lt;/table&gt;
        <!-- 若有多个表，可继续添加<table&gt;标签 -->
        <!-- <table tableName="order" domainObjectName="Order"/> -->
    </context>
</generatorConfiguration>
关键配置说明（必须修改）：
jdbcConnection：替换为自己的MySQL驱动、数据库URL、用户名、密码（与前文db.properties一致）；
javaModelGenerator：targetPackage指定实体类生成的包路径（如com.example.pojo），targetProject指定生成到src/main/java目录；
sqlMapGenerator：targetPackage指定Mapper XML生成的包路径（如com.example.mapper），targetProject指定生成到src/main/resources目录；
javaClientGenerator：targetPackage指定Mapper接口生成的包路径（如com.example.mapper），type="XMLMAPPER"表示生成XML映射的Mapper接口；
table：tableName是数据库表名（必须与数据库中一致），domainObjectName是生成的实体类名（自定义，如user表生成User类）；generatedKey配置自增主键，适配MySQL自增策略。
四、执行代码生成（实操步骤）
完成pom.xml插件配置和generatorConfig.xml配置后，即可通过IDEA的Maven工具执行生成命令，步骤如下：
打开IDEA右侧的“Maven”面板，展开项目→Plugins→mybatis-generator；
找到mybatis-generator:generate命令，双击执行（或右键选择“Run Maven Build”）；
执行成功后，查看控制台日志，会提示“Generating XXX”，表示代码生成成功；
刷新项目，即可看到自动生成的文件：
src/main/java/com/example/pojo：生成User实体类；
src/main/java/com/example/mapper：生成UserMapper接口；
src/main/resources/com/example/mapper：生成UserMapper.xml映射文件。
4.1 生成文件说明
4.1.1 实体类（User.java）
自动生成与user表字段对应的属性、getter、setter、toString、无参/有参构造方法，示例如下（简化版）：
package com.example.pojo;
public class User {
    private Integer id;
    private String username;
    private String password;
    private Integer age;
    private String email;
    // 无参构造
    public User() {}
    // 有参构造（不含id，因为id是自增）
    public User(String username, String password, Integer age, String email) {
        this.username = username;
        this.password = password;
        this.age = age;
        this.email = email;
    }
    // getter、setter方法（自动生成，完整）
    // toString方法（自动生成）
}
4.1.2 Mapper接口（UserMapper.java）
自动生成基础CRUD方法，包含selectByPrimaryKey、insert、updateByPrimaryKey、deleteByPrimaryKey、selectAll等，示例如下：
package com.example.mapper;
import com.example.pojo.User;
import org.apache.ibatis.annotations.Param;
import java.util.List;
public interface UserMapper {
    // 根据主键查询
    User selectByPrimaryKey(Integer id);
    // 查询所有用户
    List<User> selectAll();
    // 插入用户（全字段）
    int insert(User record);
    // 插入用户（非空字段）
    int insertSelective(User record);
    // 根据主键更新（全字段）
    int updateByPrimaryKey(User record);
    // 根据主键更新（非空字段）
    int updateByPrimaryKeySelective(User record);
    // 根据主键删除
    int deleteByPrimaryKey(Integer id);
}
4.1.3 Mapper XML（UserMapper.xml）
自动生成对应Mapper接口方法的SQL语句（静态SQL），无需手动编写，示例如下（简化版）：
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "https://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.example.mapper.UserMapper">
    <resultMap id="BaseResultMap" type="com.example.pojo.User">
        <id column="id" property="id" jdbcType="INTEGER"/>
        <result column="username" property="username" jdbcType="VARCHAR"/>
        <result column="password" property="password" jdbcType="VARCHAR"/>
        <result column="age" property="age" jdbcType="INTEGER"/>
        <result column="email" property="email" jdbcType="VARCHAR"/>
    </resultMap>
    <select id="selectByPrimaryKey" resultMap="BaseResultMap" parameterType="java.lang.Integer">
        select id, username, password, age, email
        from user
        where id = #{id,jdbcType=INTEGER}
    </select>
    <select id="selectAll" resultMap="BaseResultMap">
        select id, username, password, age, email
        from user
    &lt;/select&gt;
    <!-- 插入、更新、删除SQL语句（自动生成，完整） -->
</mapper>
五、代码生成后的优化（可选但推荐）
MBG生成的是基础代码，实际开发中可根据需求进行优化，提升代码规范性和实用性：
实体类优化：给实体类添加注解（如Lombok的@Data注解，简化getter/setter编写），删除多余的有参构造，添加注释；
Mapper接口优化：删除不需要的方法（如insertSelective、updateByPrimaryKeySelective，若用不到），添加自定义方法（如多条件查询）；
Mapper XML优化：将静态SQL扩展为动态SQL（如动态更新、多条件查询），添加自定义SQL语句；
Example类优化：若生成了Example类（多条件查询工具类），可根据需求简化，避免冗余代码（若不需要，可在generatorConfig.xml中关闭生成）。
关闭Example类生成：在generatorConfig.xml的<table>标签中添加配置：
<table tableName="user" domainObjectName="User">
    <generatedKey column="id" sqlStatement="MySQL" identity="true"/&gt;
    <!-- 关闭Example类生成 -->
    <generatedKey column="id" sqlStatement="MySQL" identity="true"/>
    <ignoreColumn column="password"/>
    <columnOverride column="user_name" property="userName"/>
    <!-- 关键配置：关闭Example类 -->
    <generatedKey column="id" sqlStatement="MySQL" identity="true"/>
    <enableCountByExample>false</enableCountByExample>
    <enableUpdateByExample>false</enableUpdateByExample>
    <enableDeleteByExample>false</enableDeleteByExample>
    <enableSelectByExample>false</enableSelectByExample>
    <selectByExampleQueryId>false</selectByExampleQueryId>
</table>
六、常见问题与注意事项（避坑重点）
问题1：执行生成命令后，无任何文件生成，控制台无报错？ 解决：检查generatorConfig.xml的路径是否正确（pom.xml中configurationFile配置是否与实际路径一致），检查数据库连接信息是否正确（用户名、密码、数据库名），检查table标签的tableName是否与数据库表名一致（区分大小写）。
问题2：生成代码后，Mapper接口与XML映射不匹配？ 解决：检查generatorConfig.xml中javaClientGenerator和sqlMapGenerator的targetPackage是否正确，确保Mapper接口和XML的namespace一致，接口方法名与XML中SQL标签id一致。
问题3：生成的实体类没有getter/setter方法？ 解决：检查MBG插件版本是否兼容，推荐使用1.4.2及以上版本，若仍有问题，手动添加getter/setter（或使用Lombok注解简化）。
问题4：数据库表有自增主键，但生成的insert方法没有忽略id字段？ 解决：在<table>标签中添加<generatedKey column="id" sqlStatement="MySQL" identity="true"/>，明确配置自增主键策略。
注意事项1：生成代码前，建议备份已有代码（开启overwrite=true会覆盖原有文件），避免误删自定义代码。
注意事项2：MBG仅生成基础CRUD代码，复杂SQL（多表关联、动态SQL）需手动编写，不可过度依赖。
注意事项3：数据库表结构修改后，需重新执行生成命令，更新实体类、Mapper接口和XML文件（开启overwrite=true）。
注意事项4：生成的XML文件中，SQL语句的字段名需与数据库一致，若实体类属性与数据库字段不匹配，需在generatorConfig.xml中配置<columnOverride>，或开启驼峰命名转换。
注意事项5：避免生成多余代码（如Example类），根据实际需求在配置文件中关闭，保持项目简洁。
七、总结
MyBatis代码生成器（MBG）的核心价值是“自动生成基础CRUD代码”，通过Maven插件方式配置简单、操作便捷，可大幅提升开发效率，减少重复编码。
其核心流程为：配置Maven插件→编写核心配置文件→执行生成命令→优化生成代码。
实际开发中，MBG是MyBatis入门和高效开发的必备工具，尤其适合多表、基础CRUD场景。
掌握其配置和使用方法，可将更多精力放在复杂业务逻辑和SQL优化上，提升开发效率和代码质量。

